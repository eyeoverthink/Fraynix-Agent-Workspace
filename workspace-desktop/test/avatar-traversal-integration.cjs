'use strict';

const assert=require('node:assert/strict');
const {createHash}=require('node:crypto');
const fs=require('node:fs/promises');
const os=require('node:os');
const path=require('node:path');
const {pathToFileURL}=require('node:url');
const {launch}=require('./calibration-integration.cjs');

const sha=bytes=>createHash('sha256').update(bytes).digest('hex');

async function main(){
    process.env.FRAYNIX_UNIFIED_TEST='1';
    process.env.FRAYNIX_WORKSPACE_NO_MODELS='1';
    const flags=process.platform==='linux'?['--no-sandbox','--disable-dev-shm-usage']:[];
    const portrait=path.resolve(process.argv[2]||'docs/readmes/265/sample-portrait.png');
    const out=path.resolve(process.argv[3]||'build/avatar-traversal');
    const root=await fs.mkdtemp(path.join(os.tmpdir(),'Fraynix traversal '));
    const author=path.join(root,'author'), recipient=path.join(root,'recipient');
    await Promise.all([author,recipient,out].map(folder=>fs.mkdir(folder,{recursive:true})));
    let app,page;
    const errors=[];
    async function open(project=author){
        ({app,page}=await launch(project,flags));
        page.on('pageerror',error=>errors.push(error.message));
        await page.locator('#workspace-home h1').waitFor();
    }
    async function close(){if(app){await app.close();app=null;}}
    const api=(route,args={})=>app.evaluate((_,input)=>global.workspaceTest.api(input.route,input.args),{route,args});
    const read=()=>api('/api/avatar-level/read');
    const ask=async text=>{await page.locator('#goal').fill(text);await page.locator('#run').click();};
    const status=text=>page.locator('#avatar-level-status').filter({hasText:text}).waitFor({timeout:55000});
    const said=text=>page.locator('#messages .assistant').filter({hasText:text}).last().waitFor({timeout:55000});
    const save=(name,bytes)=>fs.writeFile(path.join(out,name),bytes);
    const shot=async name=>{
        await page.evaluate(()=>new Promise(resolve=>requestAnimationFrame(()=>requestAnimationFrame(resolve))));
        await save(name,Buffer.from(await app.evaluate(()=>global.workspaceTest.capture()),'base64'));
    };
    const noExport=id=>assert.rejects(fs.stat(path.join(author,'exports','avatar-level',id,'playable.html')),{code:'ENOENT'});

    async function files(prefix,mission){
        const result={};
        for(const [kind,record] of Object.entries(mission.files)){
            const data=await fs.readFile(record.path);
            assert.equal(sha(data),record.sha256);
            const name=prefix+'-'+path.basename(record.path);
            await save(name,data);
            result[kind]={name,sha256:record.sha256};
        }
        return result;
    }

    async function standalone(file,run){
        const url=pathToFileURL(file).href;
        await app.evaluate(async({BrowserWindow},url)=>{
            const window=new BrowserWindow({width:900,height:650,webPreferences:{
                partition:'traversal-proof',sandbox:true,nodeIntegration:false,contextIsolation:true
            }});
            window.webContents.session.webRequest.onBeforeRequest((req,done)=>done({cancel:!(/^(file|data):/.test(req.url))}));
            await window.loadURL(url);
        },url);
        const game=app.windows().find(view=>view.url()===url);
        assert(game);
        await game.waitForFunction(()=>typeof globalThis.fraynixAvatarReplay==='function');
        const inputs=run.rows.slice(1).map(row=>[row[18],row[16]]);
        const replay=await game.evaluate(inputs=>globalThis.fraynixAvatarReplay(inputs),inputs);
        assert.deepEqual(replay.rows,run.rows);
        const player=game.locator('canvas');
        await player.focus();
        await game.keyboard.down('ArrowRight');
        try{
            for(const row of run.rows.filter(row=>row[16])){
                await game.waitForFunction(x=>Number(document.querySelector('canvas').dataset.playerX)>=x-5,row[1]);
                await game.keyboard.press('Space');
            }
            await game.waitForFunction(()=>document.querySelector('canvas').dataset.won==='true');
        }finally{await game.keyboard.up('ArrowRight');}
        assert.equal(Number(await player.getAttribute('data-resets')),0);
        assert.equal(Number(await player.getAttribute('data-score')),run.score);
        return true;
    }

    try{
        await open();
        await ask('Open Avatar Studio.');
        await page.locator('#avatar-studio:visible #avatar-image:enabled').setInputFiles(portrait);
        await page.locator('#avatar-status').filter({hasText:'Image loaded'}).waitFor();
        await page.locator('#avatar-name').fill('Bank Runner');
        const brief='Build a playable platformer from this image with one wide gap, one tall hazard and four pickups, called "Bank Rescue". Test it, pause before repair and export it offline.';
        await ask(brief);
        await status('Failed playtest diagnosed');
        const failed=await read();
        assert.equal(failed.diagnosis.kind,'fell-in-gap');
        assert.equal(failed.diagnosis.before.width,216);
        assert(failed.diagnosis.takeoffTick>0 && failed.diagnosis.takeoffTick<failed.diagnosis.fallTick);
        assert(failed.diagnosis.resetTick>failed.diagnosis.fallTick);
        await noExport(failed.id);
        await page.locator('#avatar-level-evidence summary').click();
        await shot('failed-gap.png');
        await close();

        await open();
        await ask('Resume my avatar level.');
        await said('Bank Rescue is ready.');
        const repaired=await read();
        assert.equal(repaired.id,failed.id);
        assert.equal(repaired.stage,'exported');
        assert.deepEqual(repaired.repairs.map(step=>step.diagnosis.kind),['fell-in-gap','hit-hazard']);
        assert.equal(repaired.repairs[0].after.width,96);
        assert.equal(repaired.repairs[1].diagnosis.before.size,120);
        assert(repaired.repairs[1].diagnosis.takeoffTick>0);
        assert.equal(repaired.repairs[1].after.size,24);
        assert.equal(repaired.evidence.crossings.gaps.length,1);
        assert.equal(repaired.evidence.crossings.hazards.length,1);
        assert.equal(repaired.evidence.score,900);
        assert.equal(repaired.evidence.frameComparison.maxColorDelta,0);
        const outputs=await files('bank-rescue',repaired);
        const winning=JSON.parse(await fs.readFile(repaired.files.playtest.path,'utf8'));
        const history=JSON.parse(await fs.readFile(repaired.files.repairs.path,'utf8'));
        assert.equal(history.history[0].failedPlaytest.id,failed.diagnosis.runId);
        assert(history.history[0].failedPlaytest.rows.some(row=>row[17].some(event=>event[0]==='reset'&&!event[1])));
        assert(history.history[1].failedPlaytest.rows.some(row=>row[17].some(event=>event[0]==='reset'&&event[1]==='hazard-1')));
        const layout=await api('/api/media-tiles/inspect',{id:repaired.levelId});
        const gap=layout.layout.traversal.gaps[0];
        assert(!layout.layout.objects.some(item=>item.effect==='ground'&&item.x>=gap.x&&item.x<gap.x+gap.width));
        await page.locator('#avatar-level-evidence summary').click();
        await shot('verified-crossings.png');
        await page.locator('#avatar-level-review').click();
        await page.locator('#tile-status').filter({hasText:'Opened the verified level'}).waitFor();
        await page.locator('#tile-canvas').evaluate(el=>el.scrollIntoView({block:'center'}));
        await shot('bank-rescue.png');
        await close();

        await open(recipient);
        await ask('Open Tile Workshop.');
        await page.locator('#tile-editor:visible #tile-import-level:enabled').setInputFiles(path.join(out,outputs.level.name));
        await page.locator('#tile-status').filter({hasText:'Imported checked level'}).waitFor();
        assert.equal((await api('/api/media-tiles/open')).id,repaired.levelId);
        assert.equal((await api('/api/media-tiles/export',{id:repaired.levelId,format:'html'})).sha256,outputs.html.sha256);
        const keyboard=await standalone(path.join(out,outputs.html.name),winning);
        await close();

        await open();
        await ask('Resume my avatar level.');
        await said('Bank Rescue is ready.');
        assert.deepEqual((await read()).files,repaired.files);
        await ask('Make a playable platformer from my image with two gaps, one spike hazard and six pickups called "Twin Banks". Playtest and export offline.');
        await said('Twin Banks is ready.');
        const fresh=await read();
        assert.equal(fresh.evidence.crossings.gaps.length,2);
        assert.equal(fresh.evidence.crossings.hazards.length,1);
        assert.equal(fresh.evidence.score,1100);
        assert.equal(fresh.repairs.length,0);
        assert.equal(fresh.evidence.frameComparison.changedPixels,0);
        const visual=fresh.evidence.frameComparison;
        assert(visual.referenceTick===3&&visual.observedTick>=3);
        const freshFiles=await files('twin-banks',fresh);

        await ask('Build a playable level from my photo with an oversized gap, keep the layout fixed, and test and export it offline.');
        await status('Needs review. Export is blocked');
        const locked=await read();
        assert.equal(locked.diagnosis.kind,'fell-in-gap');
        assert.equal(locked.repairs.length,0);
        await noExport(locked.id);
        await assert.rejects(api('/api/avatar-level/finish',{id:locked.id}),/Complete the playtest/);
        await close();
        await open();
        await ask('Resume my avatar level.');
        await status('Needs review. Export is blocked');
        assert.equal((await read()).id,locked.id);
        await noExport(locked.id);
        assert.deepEqual(errors,[]);
        await save('jump-repair-evidence.json',JSON.stringify({restartBeforeRepairs:true,exactExportsAfterRestart:true,
            cleanImportExact:true,offlineKeyboardWin:keyboard,lockedRefusalAfterRestart:true,
            repaired:{...repaired.evidence,files:outputs},fresh:{...fresh.evidence,files:freshFiles},locked:locked.diagnosis},null,2)+'\n');
        console.log('Measured and repaired a failed gap jump and hazard hit, verified a fresh course, and refused a locked gap.');
    }catch(error){
        for(const id of ['avatar-level-status','avatar-status','notice'])
            console.error(id,await page?.locator('#'+id).textContent().catch(()=>null));
        console.error('Latest mission',await read().catch(()=>null),errors);
        throw error;
    }finally{
        await close();
        const resolved=await fs.realpath(root);
        if(path.dirname(resolved)===await fs.realpath(os.tmpdir())&&/^Fraynix traversal [a-z0-9]+$/i.test(path.basename(resolved)))
            await fs.rm(resolved,{recursive:true,force:true,maxRetries:3}).catch(()=>{});
    }
}
main().catch(error=>{console.error(error);process.exitCode=1;});
