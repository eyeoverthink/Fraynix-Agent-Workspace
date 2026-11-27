'use strict';

const assert = require('node:assert/strict');
const {createHash} = require('node:crypto');
const fs = require('node:fs/promises');
const os = require('node:os');
const path = require('node:path');
const {pathToFileURL} = require('node:url');
const {launch} = require('./calibration-integration.cjs');

const sha=bytes=>createHash('sha256').update(bytes).digest('hex');

async function main(){
    process.env.FRAYNIX_UNIFIED_TEST='1';
    process.env.FRAYNIX_WORKSPACE_NO_MODELS='1';
    const flags=process.platform==='linux'?['--no-sandbox','--disable-dev-shm-usage']:[];
    const picture=path.resolve(process.argv[2]||'docs/readmes/265/sample-portrait.png');
    const out=path.resolve(process.argv[3]||'build/avatar-level-mission');
    const root=await fs.mkdtemp(path.join(os.tmpdir(),'Fraynix avatar mission '));
    const author=path.join(root,'author'), recipient=path.join(root,'recipient');
    await Promise.all([out,author,recipient].map(location=>fs.mkdir(location,{recursive:true})));
    let app,page;
    const errors=[];
    async function open(project=author){
        ({app,page}=await launch(project,flags));
        page.on('pageerror',error=>errors.push(error.message));
        await page.locator('#workspace-home h1').waitFor();
    }
    async function close(){
        if(app){await app.close();app=null;}
    }
    const ask=async text=>{await page.locator('#goal').fill(text);await page.locator('#run').click();};
    const api=(route,args={})=>app.evaluate((_,input)=>global.workspaceTest.api(input.route,input.args),{route,args});
    const read=id=>api('/api/avatar-level/read',id?{id}:{});
    const stage=text=>page.locator('#avatar-level-status').filter({hasText:text}).waitFor();
    const save=(name,bytes)=>fs.writeFile(path.join(out,name),bytes);
    const capture=async name=>{
        await page.evaluate(()=>new Promise(resolve=>requestAnimationFrame(()=>requestAnimationFrame(resolve))));
        await save(name,Buffer.from(await app.evaluate(()=>global.workspaceTest.capture()),'base64'));
    };
    const imageInput=()=>page.locator('#avatar-studio:visible #avatar-image:enabled');

    async function checkedFiles(mission,prefix){
        const files={};
        for(const [kind,receipt] of Object.entries(mission.files)){
            const bytes=await fs.readFile(receipt.path);
            assert.equal(sha(bytes),receipt.sha256);
            assert.equal(bytes.length,receipt.bytes);
            const name=prefix+'-'+path.basename(receipt.path);
            await save(name,bytes);
            files[kind]={name,sha256:receipt.sha256};
        }
        return files;
    }

    async function standalone(file,mission,run){
        const url=pathToFileURL(file).href;
        await app.evaluate(async({BrowserWindow},url)=>{
            const window=new BrowserWindow({width:940,height:650,webPreferences:{
                partition:'avatar-level-integration',sandbox:true,nodeIntegration:false,contextIsolation:true
            }});
            window.webContents.session.webRequest.onBeforeRequest((req,done)=>
                done({cancel:!(/^(file|data):/.test(req.url))}));
            await window.loadURL(url);
        },url);
        const portable=app.windows().find(w=>w.url()===url);
        assert(portable);
        await portable.waitForFunction(()=>typeof globalThis.fraynixAvatarReplay==='function');
        const inputs=run.rows.slice(1).map(row=>[row[18],row[16]]);
        const witness=await portable.evaluate(inputs=>globalThis.fraynixAvatarReplay(inputs),inputs);
        assert.deepEqual(witness.rows,run.rows);
        assert.equal(witness.reason,'won');

        await portable.locator('canvas').focus();
        await portable.keyboard.down('ArrowRight');
        try{
            const jumps=run.rows.filter(row=>row[16]).map(row=>row[1]);
            for(const x of jumps){
                await portable.waitForFunction(x=>Number(document.querySelector('canvas').dataset.playerX)>=x-4,x);
                await portable.keyboard.press('Space');
            }
            await portable.waitForFunction(()=>document.querySelector('canvas').dataset.won==='true');
        }finally{await portable.keyboard.up('ArrowRight');}
        assert.equal(Number(await portable.locator('canvas').getAttribute('data-score')),mission.evidence.score);
        return {playedWithKeyboard:true,replayedExactRows:true};
    }

    try{
        await open();
        const prompt='Turn this imported photo into a character, build a playable level with hurdles called “Studio Circuit”, then playtest it and pause before export.';
        await ask(prompt);
        await stage('Waiting for the portrait');
        await page.locator('#workspace-next-continue:disabled').waitFor();
        const waiting=await read();
        assert.equal(waiting.request,prompt);
        assert.equal(waiting.course,'jump');
        await assert.rejects(api('/api/avatar-level/finish',{id:waiting.id}),/Complete the playtest/);
        await close();

        await open();
        await ask('Resume my avatar level.');
        await stage('Waiting for the portrait');
        await imageInput().setInputFiles(picture);
        await page.locator('#avatar-source:visible').waitFor();
        await page.locator('#avatar-name').fill('Studio Runner');
        await capture('portrait-input.png');
        await page.locator('#workspace-next-continue:enabled').click();
        await stage('Playtest passed. Export is ready');
        const proof=await read();
        assert.equal(proof.id,waiting.id);
        assert.equal(proof.stage,'verified');
        assert.equal(proof.title,'Studio Circuit');
        assert.equal(proof.evidence.hurdles,2);
        assert.equal(proof.evidence.jumps,2);
        assert.equal(proof.evidence.pickups,3);
        assert.equal(proof.evidence.score,800);
        assert(proof.evidence.straightWalkBlocked);
        assert.equal((await api('/api/media-tiles/open')).id,undefined);
        await assert.rejects(fs.stat(path.join(author,'exports','avatar-level',waiting.id,'playable.html')),{code:'ENOENT'});
        await page.locator('#avatar-level-evidence summary').click();
        await capture('verified-mission.png');

        const savedAvatar=path.join(author,'media','avatars',proof.avatarId+'.json');
        const bytes=await fs.readFile(savedAvatar);
        await close();
        await fs.appendFile(savedAvatar,' ');
        await open();
        await ask('Resume my avatar level.');
        await stage('saved avatar source could not be verified');
        assert.equal((await read()).stage,'verified');
        await fs.writeFile(savedAvatar,bytes);
        await page.locator('#avatar-level-resume:enabled').click();
        await stage('Offline game and editable level saved');
        const exported=await read();
        assert.equal(exported.id,waiting.id);
        assert.deepEqual(exported.evidence,proof.evidence);
        assert.equal(exported.files.html.sha256,proof.evidence.htmlSha256);
        const jumpFiles=await checkedFiles(exported,'studio-circuit');
        const replay=JSON.parse(await fs.readFile(exported.files.playtest.path,'utf8'));

        await page.locator('#avatar-level-review').click();
        await page.locator('#tile-status').filter({hasText:'Opened the verified level'}).waitFor();
        assert.equal((await api('/api/media-tiles/open')).id,exported.levelId);
        await page.locator('#tile-play:enabled').click();
        await page.waitForFunction(()=>document.querySelector('#tile-canvas').dataset.hero==='Studio Runner');
        await page.locator('#tile-canvas').evaluate(el=>el.scrollIntoView({block:'center'}));
        await capture('level-in-workspace.png');
        await page.locator('#tile-stop').click();
        await close();

        await open(recipient);
        await ask('Open Tile Workshop.');
        await page.locator('#tile-editor:visible #tile-import-level:enabled').setInputFiles(path.join(out,jumpFiles.level.name));
        await page.locator('#tile-status').filter({hasText:'Imported checked level'}).waitFor();
        assert.equal((await api('/api/media-tiles/open')).id,exported.levelId);
        assert.equal((await api('/api/media-tiles/export',{id:exported.levelId,format:'html'})).sha256,jumpFiles.html.sha256);
        const offline=await standalone(path.join(out,jumpFiles.html.name),exported,replay);
        await close();

        await open();
        await ask('Open Avatar Studio.');
        const fresh=path.join(root,'another-portrait.png');
        const altered=await page.evaluate(async input=>{
            const photo=new Image();photo.src='data:image/png;base64,'+input;
            await photo.decode();
            const canvas=document.createElement('canvas');canvas.width=photo.width;canvas.height=photo.height;
            const g=canvas.getContext('2d');
            g.drawImage(photo,0,0);
            g.fillStyle='#17327e';g.fillRect(8,8,24,16);
            return canvas.toDataURL().split(',')[1];
        },(await fs.readFile(picture)).toString('base64'));
        await fs.writeFile(fresh,Buffer.from(altered,'base64'));
        await imageInput().setInputFiles(fresh);
        await page.locator('#avatar-status').filter({hasText:'Image loaded'}).waitFor();
        await page.locator('#avatar-name').fill('Relay Runner');
        await ask('Make a playable level from this picture with no jumps called "Quick Relay" and export it offline.');
        await page.locator('#messages .assistant').filter({hasText:'Quick Relay is ready.'}).last().waitFor();
        await stage('Offline game and editable level saved');
        const second=await read();
        assert.notEqual(second.source.imageSha256,proof.source.imageSha256);
        assert.equal(second.stage,'exported');
        assert.equal(second.title,'Quick Relay');
        assert.equal(second.course,'relay');
        assert.equal(second.evidence.jumps,0);
        assert.equal(second.evidence.score,800);
        await checkedFiles(second,'quick-relay');
        assert.deepEqual(errors,[]);
        await save('mission-evidence.json',JSON.stringify({pausedBeforeImport:true,recordRetainedAcrossRestart:true,
            blockedChangedPortrait:true,resumedWithoutNewPrompt:true,cleanProjectExportExact:true,offline,
            jump:{...exported.evidence,files:jumpFiles},
            relay:{...second.evidence,imageSourceDistinct:true}},null,2)+'\n');
        console.log('Avatar level verified from chat, through two restarts, with two courses and offline keyboard play.');
    }catch(error){
        for(const selector of ['#avatar-level-status','#avatar-status','#engine-state','#notice'])
            console.error(selector,await page?.locator(selector).textContent().catch(()=>null));
        console.error('Renderer errors',errors);
        throw error;
    }finally{
        await close();
        const resolved=await fs.realpath(root);
        if(path.dirname(resolved)===await fs.realpath(os.tmpdir())&&/^Fraynix avatar mission [a-z0-9]+$/i.test(path.basename(resolved)))
            await fs.rm(resolved,{recursive:true,force:true,maxRetries:3}).catch(()=>{});
    }
}
main().catch(error=>{console.error(error);process.exitCode=1;});
