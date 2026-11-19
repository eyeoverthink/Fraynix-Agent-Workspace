'use strict';
const assert=require('node:assert/strict');
const {execFile}=require('node:child_process');
const {createHash}=require('node:crypto');
const fs=require('node:fs/promises');
const path=require('node:path');
const {promisify}=require('node:util');
const {importGameLayout}=require('../../workspace-desktop/blender-exchange.cjs');
const {launch}=require('../../workspace-desktop/test/calibration-integration.cjs');

const fileHash=bytes=>createHash('sha256').update(bytes).digest('hex');

async function main(){
    const output=path.resolve(process.argv[2]||'build/edition190-live');
    await fs.mkdir(output);
    const project=path.join(output,'project');
    await fs.mkdir(project);
    Object.assign(process.env,{FRAYNIX_WORKSPACE_NO_MODELS:'1',FRAYNIX_WORKSPACE_PERSISTENT:'0',
        FRAYNIX_WORKSPACE_JAVA_HEAP_MIB:'768',FRAYNIX_WORKSPACE_JAVA_PROCESSORS:'2'});
    const flags={timeout:60000};
    let {app,page}=await launch(project);
    const errors=[];
    page.on('pageerror',error=>errors.push(error.message));
    const api=(route,args={})=>app.evaluate((_,[route,args])=>global.workspaceTest.api(route,args),[route,args]);
    const capture=async name=>{
        await page.evaluate(()=>new Promise(resolve=>requestAnimationFrame(()=>requestAnimationFrame(resolve))));
        const buffer=Buffer.from(await app.evaluate(()=>global.workspaceTest.capture()),'base64');
        await fs.writeFile(path.join(output,name),buffer);
        return {name,sha256:fileHash(buffer)};
    };
    const status=()=>page.locator('#game-blender-status').textContent();
    const statusContains=(text)=>page.waitForFunction(text=>document.querySelector('#game-blender-status').textContent.includes(text),text,flags);
    const pickOpen=file=>app.evaluate(({dialog},file)=>{
        dialog.showOpenDialog=async()=>({canceled:false,filePaths:[file]});
    },file);
    const pickSave=file=>app.evaluate(({dialog},file)=>{
        dialog.showSaveDialog=async()=>({canceled:false,filePath:file});
    },file);
    const evidence=id=>api('/api/game/blender-verify',{id});
    const list=async()=>(await api('/api/game/blender-list')).candidates;
    const hit=(entities,id)=>entities.find(row=>row.id===id);

    try{
        await app.evaluate(({BrowserWindow})=>BrowserWindow.getAllWindows().find(w=>w.isVisible()).setSize(1500,1030));
        await page.locator('#game-tab').click();
        await page.waitForFunction(()=>document.querySelector('#game-frame').naturalWidth===960,{},flags);
        await page.locator('#game-relay').click();
        await page.waitForFunction(()=>document.querySelector('#notice').textContent.includes('Relay Garden:'),{},flags);
        const original=await api('/api/game/frame');
        const originalLevel=path.resolve('assets/original/relay-garden/level.json');
        const originalHash=fileHash(await fs.readFile(originalLevel));
        const exported=path.join(output,'original-course.blend');
        await pickSave(exported);
        await page.locator('#game-export-blender').click();
        await page.waitForFunction(()=>document.querySelector('#notice').textContent.includes('Saved the Game Lab layout'),{},flags);
        await promisify(execFile)(process.env.FRAYNIX_BLENDER||'blender',[
            '--background','--factory-startup','--disable-autoexec',exported,'--python-exit-code','20',
            '--python',path.join(__dirname,'edit-course.py'),'--',output],
            {windowsHide:true,timeout:60000});
        await page.locator('#game-blender-candidates summary').click();
        await statusContains('Import an edited Relay layout');
        for(const [name,message] of [['moved-player.blend','only existing ground tiles'],['off-grid-course.blend','quarter-unit grid']]){
            await pickOpen(path.join(output,name));
            await page.locator('#game-blender-import').click();
            await statusContains(message);
            assert.equal((await list()).length,0);
        }
        const edited=path.join(output,'edited-course.blend');
        const input=await importGameLayout(edited);
        const wrong=JSON.parse(input.snapshot);
        wrong.frame.source.tick=1;
        await assert.rejects(api('/api/game/blender-import',{...input,snapshot:JSON.stringify(wrong)}),/checksum differs/);
        await pickOpen(edited);
        await page.locator('#game-blender-import').click();
        await statusContains('Ready to playtest');
        const [candidate]=await list();
        assert.equal(candidate.courseId,'relay-garden-v1');
        assert.equal(candidate.blendSha256,fileHash(await fs.readFile(edited)));
        const move=candidate.moves[0];
        assert.equal(candidate.moves.length,1);
        assert.deepEqual(move.from,[4.375,.125]);
        assert.deepEqual(move.to,[4.625,.375]);
        await assert.rejects(evidence(candidate.id),/must pass/);
        const screenshots=[await capture('blender-edit-imported.png')];

        await page.locator('#game-blender-open').click();
        await statusContains('Opened edited terrain');
        let frame=await api('/api/game/frame');
        assert.equal(frame.levelId,candidate.id);
        assert.equal(frame.state.contentId,candidate.courseId);
        assert.equal(frame.state.tick,0);
        assert.notEqual(frame.state.sessionId,original.state.sessionId);
        await api('/api/runtime040/bind',{kind:'game'});
        let snapshot=await api('/api/runtime040/read');
        assert.deepEqual(hit(snapshot.entities,move.sourceId).position,[...move.to,0]);
        await page.locator('#game-right').click();
        await page.waitForFunction(()=>document.querySelector('#game-status').textContent.includes('tick 30'),{},flags);
        frame=await api('/api/game/frame');
        assert(frame.state.player.x>original.state.player.x);
        screenshots.push(await capture('blender-candidate-playing.png'));
        const interactiveSession=frame.state.sessionId;

        await page.locator('#game-blender-start').click();
        await statusContains('Both AI playtests reached the beacon');
        const proof=await evidence(candidate.id);
        assert(proof.proof.passed&&proof.proof.trials===2);
        const trials=proof.attempts.at(-1).trials;
        assert.notEqual(trials[0].session,trials[1].session);
        for(const trial of trials){
            assert.equal(trial.trace[0].tick,0);
            assert(trial.trace.at(-1).player.won);
            assert(!trial.trace.at(-1).player.dead);
            assert.notEqual(trial.session,interactiveSession);
        }
        const interactive=await api('/api/game/frame');
        assert.equal(interactive.state.sessionId,interactiveSession);
        assert.equal(interactive.state.tick,30);
        await page.locator('#game-blender-verify').click();
        await statusContains('Checked 2 wins');
        screenshots.push(await capture('blender-candidate-verified.png'));
        const folder=path.join(project,'blender-game-candidates',candidate.id);
        const picture=await fs.readFile(path.join(folder,proof.proof.attemptId+'-trial-2.png'));
        assert.equal(fileHash(picture),trials[1].imageSha256);
        await fs.writeFile(path.join(output,'native-beacon.png'),picture);

        const definitionPath=path.join(folder,'level.json');
        const definition=await fs.readFile(definitionPath);
        try{
            await fs.appendFile(definitionPath,' ');
            await assert.rejects(evidence(candidate.id),/saved Blender level differs/);
        } finally { await fs.writeFile(definitionPath,definition); }
        const picturePath=path.join(folder,proof.proof.attemptId+'-trial-2.png');
        try{
            await fs.appendFile(picturePath,' ');
            await assert.rejects(evidence(candidate.id),/finish screenshot changed/);
        } finally { await fs.writeFile(picturePath,picture); }
        assert.deepEqual(await evidence(candidate.id),proof);
        await page.locator('#game-blender-evidence').click();
        await statusContains('Playtest evidence saved');
        const file=(await status()).split('saved to ')[1];
        assert.deepEqual(JSON.parse(await fs.readFile(file,'utf8')),proof);
        await app.close();
        ({app,page}=await launch(project));
        assert.deepEqual(await evidence(candidate.id),proof);
        assert.equal(fileHash(await fs.readFile(originalLevel)),originalHash);
        await api('/api/game/open-relay');
        await api('/api/runtime040/bind',{kind:'game'});
        snapshot=await api('/api/runtime040/read');
        assert.deepEqual(hit(snapshot.entities,move.sourceId).position,[...move.from,0]);
        const rejectedSource=await importGameLayout(path.join(output,'wide-gap-course.blend'));
        const difficult=await api('/api/game/blender-import',rejectedSource);
        assert.equal(difficult.moves.length,16);
        await api('/api/game/blender-start',{id:difficult.id});
        let outcome;
        for(let turn=0;turn<80;turn++){
            outcome=await api('/api/game/blender-state',{id:difficult.id});
            if(outcome.status!=='running')break;
            await new Promise(resolve=>setTimeout(resolve,300));
        }
        assert.equal(outcome?.status,'failed',JSON.stringify(outcome));
        await assert.rejects(evidence(difficult.id),/must pass/);
        const negative=JSON.parse(await fs.readFile(path.join(project,'blender-game-candidates',difficult.id,'playtest.json'),'utf8')).data;
        const failed=negative.attempts.at(-1).trials[0];
        assert.equal(failed.status,'failed');
        assert(!failed.trace.at(-1).player.won);
        assert(failed.trace.at(-1).player.dead || failed.trace.at(-1).tick===1728);
        assert.deepEqual(await evidence(candidate.id),proof);
        assert.deepEqual(errors,[]);
        const result={passed:true,candidate,proof:proof.proof,originalHash,
            failedTrial:{moves:difficult.moves.length,tick:failed.trace.at(-1).tick,
                dead:failed.trace.at(-1).player.dead,verified:false},
            checks:{protectedPlayer:true,quarterGrid:true,snapshotTamper:true,levelTamper:true,imageTamper:true,
                separateRuntime:true,restarted:true,originalPreserved:true},screenshots};
        await fs.writeFile(path.join(output,'playtest-evidence.json'),JSON.stringify(proof,null,2)+'\n');
        await fs.writeFile(path.join(output,'result.json'),JSON.stringify(result,null,2)+'\n');
        console.log(JSON.stringify(result));
    } catch(error){
        console.error('Candidate status',await status().catch(()=>null));
        const candidates=await list().catch(()=>[]);
        for(const candidate of candidates)console.error(await api('/api/game/blender-state',{id:candidate.id}).catch(()=>null));
        throw error;
    } finally {await app.close().catch(()=>{});}
}

main().catch(error=>{console.error(error);process.exitCode=1;});
