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
    const avatar=path.resolve(process.argv[2]||'build/avatar265-public/editable-avatar.avatar.json');
    const output=path.resolve(process.argv[3]||'build/avatar-player');
    const root=await fs.mkdtemp(path.join(os.tmpdir(),'Fraynix avatar player '));
    const author=path.join(root,'author'), recipient=path.join(root,'recipient');
    await Promise.all([output,author,recipient].map(location=>fs.mkdir(location,{recursive:true})));
    let app,page;
    const errors=[];
    async function open(project){
        ({app,page}=await launch(project,flags));
        page.on('pageerror',error=>errors.push(error.message));
        await page.locator('#workspace-home h1').waitFor();
    }
    const api=(route,args={})=>app.evaluate((_,input)=>global.workspaceTest.api(input.route,input.args),{route,args});
    const tiles=(name,args)=>api('/api/media-tiles/'+name,args);
    const tool=(name,args)=>app.evaluate((_,input)=>global.workspaceTest.execute(input.name,input.args),{name,args});
    const status=text=>page.locator('#tile-status').filter({hasText:text}).waitFor();
    const capture=async name=>fs.writeFile(path.join(output,name),Buffer.from(await app.evaluate(()=>global.workspaceTest.capture()),'base64'));
    const ask=async text=>{await page.locator('#goal').fill(text);await page.locator('#run').click();};
    const write=async(name,bytes)=>fs.writeFile(path.join(output,name),bytes);
    const replay=(level,inputs,legacy=false)=>page.evaluate(
        input=>window.tileEvidence.replay(input),{level,inputs,legacy});
    const name=JSON.parse(await fs.readFile(avatar,'utf8')).name;
    async function readyForPlay(){
        await page.locator('#tile-play:enabled').waitFor();
        await page.locator('#tile-play').click();
    }

    async function play(view,selector,screenshot=false){
        const canvas=view.locator(selector);
        await canvas.focus();
        await view.waitForFunction(({selector,name})=>{
            const stage=document.querySelector(selector);
            if(stage?.dataset.hero!==name || stage.dataset.playerFrame!=='idle')return false;
            const pixels=stage.getContext('2d').getImageData(48,280,24,32).data;
            let blue=0, pale=0;
            for(let i=0;i<pixels.length;i+=4){
                if(pixels[i]===92 && pixels[i+1]===155 && pixels[i+2]===195)blue++;
                if(pixels[i]===255 && pixels[i+1]===227 && pixels[i+2]===165)pale++;
            }
            return blue>=3&&pale===0;
        },{selector,name});
        if(screenshot){
            await canvas.evaluate(el=>el.scrollIntoView({block:'center'}));
            await capture('avatar-player.png');
        }
        await view.evaluate(selector=>{
            const stage=document.querySelector(selector);
            window.sampledPlayerFrames=new Set();
            let stopped=false;
            const observe=new MutationObserver(()=>{
                const state=stage.dataset;
                if(Number(state.playerX)<630 && Number(state.playerX)>100 && state.playerFrame!=='idle')
                    window.sampledPlayerFrames.add(state.playerFrame);
                if(state.won==='true'&&!stopped){stopped=true;observe.disconnect();}
            });
            observe.observe(stage,{attributes:true});
        },selector);
        await view.keyboard.down('ArrowRight');
        try{
            await view.waitForFunction(selector=>document.querySelector(selector)?.dataset.won==='true',selector);
        }finally{await view.keyboard.up('ArrowRight');}
        const frames=await view.evaluate(()=>[...window.sampledPlayerFrames].sort());
        assert.deepEqual(frames,['0','1','2','3']);
        const state=await canvas.evaluate(el=>({...el.dataset}));
        assert.equal(state.collected,'1');
        assert.equal(state.score,'600');
        assert.equal(state.hero,name);
        return {frames,score:Number(state.score),collected:Number(state.collected),won:true};
    }

    try{
        await open(author);
        const png=await page.evaluate(()=>{
            const canvas=document.createElement('canvas');
            canvas.width=24;canvas.height=24;
            const c=canvas.getContext('2d');
            c.fillStyle='#ffe282';
            c.fillRect(2,2,20,20);
            c.fillStyle='#d97344';
            c.fillRect(9,5,5,14);
            return canvas.toDataURL().split(',')[1];
        });
        const pickup=await tiles('add',{source:{filename:'Relay pickup.png',png}});
        const original=await tiles('save',{layout:{title:'Avatar Relay',objects:[
            {id:'token',assetId:pickup.id,kind:'sprite',layer:'pickup',x:160,y:288,size:24}
        ]}});
        await ask('Open Avatar Studio.');
        await page.locator('#avatar-project:enabled').setInputFiles(avatar);
        await page.locator('#avatar-player:enabled').waitFor();
        await ask('Use my avatar as the player.');
        await status('Save the level to play.');
        await page.locator('#tile-save').click();
        await status('Saved level. Sprite sources');
        const level=await tiles('open');
        assert.equal(level.layout.player.name,name);
        assert.deepEqual(level.layout.objects,original.layout.objects);
        const {idleAssetId,walkAssetId}=level.layout.player;
        assert.deepEqual([level.sprites[idleAssetId].width,level.sprites[idleAssetId].height],[96,112]);
        assert.deepEqual([level.sprites[walkAssetId].width,level.sprites[walkAssetId].height],[384,112]);
        const savedAvatar=await api('/api/avatar/open');
        assert.equal(level.layout.player.avatarId,savedAvatar.id);
        assert.equal(sha(Buffer.from(level.sprites[idleAssetId].png,'base64')),savedAvatar.hashes.spriteSha256);
        assert.equal(sha(Buffer.from(level.sprites[walkAssetId].png,'base64')),savedAvatar.hashes.walkSha256);
        await assert.rejects(tiles('save',{layout:{...level.layout,player:{...level.layout.player,walkAssetId:pickup.id}}}),
            /checked idle and walk/);
        await assert.rejects(tiles('save',{layout:{...level.layout,player:{...level.layout.player,avatarId:'f'.repeat(64)}}}),
            /checked idle and walk/);

        await readyForPlay();
        const inApp=await play(page,'#tile-canvas',true);
        await page.locator('#tile-stop').click();
        await page.waitForFunction(()=>document.querySelector('#tile-run-summary')?.textContent.includes('600'));
        const run=await tiles('export-run');
        assert.equal(run.levelId,level.id);
        const inputs=run.rows.slice(1).map(row=>[row[18],row[16]]);
        const exact=await replay(level,inputs);
        assert.deepEqual(exact.rows,run.rows);
        assert.deepEqual((await replay(original,inputs,true)).rows,run.rows);
        const motionTicks=run.rows.filter(row=>row[3]>10&&row[1]>120&&row[1]<250);
        assert(motionTicks.length>20);
        const mid=motionTicks[15][0];
        const images=[];
        for(const tick of [0,mid]){
            const result=await tool('tile.image',{runId:run.id,tick});
            const bytes=await fs.readFile(result.file.path);
            images.push(result.file.sha256);
            await write(tick===0?'idle-replay.png':'walk-replay.png',bytes);
        }
        assert.notEqual(...images);
        const originalRenderer=path.join(__dirname,'../renderer/media-tile-play-legacy.js');
        const rendererSha256=sha(await fs.readFile(originalRenderer));
        assert.equal(rendererSha256,'ba901b0fb8c990a83cfeadabac3fb5a8ba93e79007d85cd9847e388c14c27ab3');
        const legacy=await tiles('save-run',{levelId:original.id,reason:exact.reason,rows:exact.rows,rendererSha256});
        const older=await tool('tile.image',{runId:legacy.id,tick:0});
        assert.match(older.file.sha256,/^[0-9a-f]{64}$/);
        const unknown=await tiles('save-run',{levelId:original.id,reason:exact.reason,rows:exact.rows,rendererSha256:'9'.repeat(64)});
        await assert.rejects(tool('tile.image',{runId:unknown.id,tick:0}),/recorded renderer version/);

        const files={};
        for(const format of ['json','html']){
            const artifact=await tiles('export',{id:level.id,format});
            const bytes=Buffer.from(artifact.base64,'base64');
            assert.equal(sha(bytes),artifact.sha256);
            const filename='avatar-relay.'+format;
            await write(filename,bytes);
            files[format]={filename,sha256:artifact.sha256};
        }
        await app.close();app=null;

        await open(author);
        await ask('Open Tile Workshop.');
        await status('Reopened checked level');
        assert.equal((await tiles('open')).id,level.id);
        assert.equal((await tiles('export',{id:level.id,format:'html'})).sha256,files.html.sha256);
        await readyForPlay();
        const afterRestart=await play(page,'#tile-canvas');
        await page.locator('#tile-stop').click();
        await page.locator('#tile-clear-player').click();
        await status('Original player restored');
        assert.equal((await tiles('open')).id,level.id);
        await app.close();app=null;

        await open(recipient);
        await ask('Open Tile Workshop.');
        await page.locator('#tile-import-level:enabled').setInputFiles(path.join(output,files.json.filename));
        await status('Imported checked level');
        assert.equal((await tiles('open')).id,level.id);
        assert.equal((await tiles('export',{id:level.id,format:'html'})).sha256,files.html.sha256);
        const url=pathToFileURL(path.join(output,files.html.filename)).href;
        await app.evaluate(async({BrowserWindow},url)=>{
            const window=new BrowserWindow({width:950,height:610,webPreferences:{
                partition:'avatar-player-offline',sandbox:true,nodeIntegration:false,contextIsolation:true
            }});
            window.webContents.session.webRequest.onBeforeRequest((req,done)=>done({cancel:!/^(file|data):/.test(req.url)}));
            await window.loadURL(url);
        },url);
        const portable=app.windows().find(window=>window.url()===url);
        assert(portable);
        const standalone=await play(portable,'canvas');
        assert.deepEqual(errors,[]);
        const proof={levelId:level.id,inApp,afterRestart,standalone,exactRecordedPhysics:true,
            legacyImageReplay:true,unknownRendererRefused:true,cleanImportExact:true,files};
        await write('avatar-player-evidence.json',JSON.stringify(proof,null,2)+'\n');
        console.log('Avatar played, collected the pickup and reached the exit in-app, after restart and in offline HTML.');
    }catch(error){
        for(const selector of ['#tile-status','#avatar-status','#tile-run-summary','#engine-state'])
            console.error(selector,await page?.locator(selector).textContent().catch(()=>null));
        throw error;
    }finally{
        if(app)await app.close().catch(()=>{});
        const resolved=await fs.realpath(root);
        if(path.dirname(resolved)===await fs.realpath(os.tmpdir())&&/^Fraynix avatar player [a-z0-9]+$/i.test(path.basename(resolved)))
            await fs.rm(resolved,{recursive:true,force:true,maxRetries:3}).catch(()=>{});
    }
}
main().catch(error=>{console.error(error);process.exitCode=1;});
