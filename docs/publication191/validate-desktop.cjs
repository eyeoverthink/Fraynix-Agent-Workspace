'use strict';
const assert=require('node:assert/strict');
const {createHash}=require('node:crypto');
const fs=require('node:fs/promises');
const path=require('node:path');
const {setTimeout:delay}=require('node:timers/promises');
const {launch}=require('../../workspace-desktop/test/calibration-integration.cjs');

const hash=bytes=>createHash('sha256').update(bytes).digest('hex');

async function main(){
    const output=path.resolve(process.argv[2]||'build/edition191-live');
    const fixture=path.resolve(process.argv[3]||'docs/readmes/191/wide-gap-course.blend');
    await fs.mkdir(output);
    const project=path.join(output,'project');
    await fs.mkdir(project);
    Object.assign(process.env,{FRAYNIX_WORKSPACE_NO_MODELS:'1',FRAYNIX_WORKSPACE_PERSISTENT:'0',
        FRAYNIX_WORKSPACE_JAVA_HEAP_MIB:'768',FRAYNIX_WORKSPACE_JAVA_PROCESSORS:'2'});
    let {app,page}=await launch(project);
    const flags={timeout:70000};
    const errors=[];
    const api=(route,args={})=>app.evaluate((_,[route,args])=>global.workspaceTest.api(route,args),[route,args]);
    const task=id=>api('/api/agency043/read',{id});
    const child=id=>api('/api/game-tasks115/read',{id});
    const verify=id=>api('/api/game-tasks115/verify',{id});
    const candidates=async()=>(await api('/api/game/blender-list')).candidates;
    const startText=async text=>{
        await page.locator('#goal').fill(text);
        await page.locator('#run').click();
        return until(async()=>(await api('/api/agency043/state')).tasks.find(row=>row.goal===text));
    };
    async function until(check,maximum=90000){
        const deadline=performance.now()+maximum;
        let result;
        do{
            result=await check();
            if(result)return result;
            await delay(250);
        }while(performance.now()<deadline);
        throw Error('Timed out waiting for the Java task');
    }
    async function finish(id){
        const finished=await until(async()=>{
            const row=await task(id);
            if(row.status==='interrupted'||row.status==='cancelled')throw Error(row.reason);
            return row.status==='completed'?row:null;
        });
        assert.equal(finished.outcome,'verified');
        assert.equal(finished.turn,0);
        return finished;
    }
    const capture=async name=>{
        await page.evaluate(()=>new Promise(resolve=>requestAnimationFrame(()=>requestAnimationFrame(resolve))));
        const buffer=Buffer.from(await app.evaluate(()=>global.workspaceTest.capture()),'base64');
        await fs.writeFile(path.join(output,name),buffer);
        return {name,sha256:hash(buffer)};
    };
    const resize=()=>app.evaluate(({BrowserWindow})=>BrowserWindow.getAllWindows().find(w=>w.isVisible()).setSize(1500,1030));
    const pickOpen=file=>app.evaluate(({dialog},file)=>{
        dialog.showOpenDialog=async()=>({canceled:false,filePaths:[file]});
    },file);
    async function mission(id){
        if(!await page.locator('#mission044').isVisible())await page.locator('#mission044-open').click();
        await page.locator('#mission044-task').selectOption(id);
        await page.waitForFunction(id=>document.querySelector('#mission044').dataset.taskId===id,id,flags);
    }

    try{
        page.on('pageerror',error=>errors.push(error.message));
        await resize();
        await page.locator('#game-tab').click();
        await page.waitForFunction(()=>document.querySelector('#game-frame').naturalWidth===960,{},flags);
        await page.locator('#game-blender-candidates summary').click();
        await page.waitForFunction(()=>document.querySelector('#game-blender-status').textContent.includes('Import an edited Relay layout'),{},flags);
        await pickOpen(fixture);
        await page.locator('#game-blender-import').click();
        await page.waitForFunction(()=>document.querySelector('#game-blender-status').textContent.includes('Ready to playtest'),{},flags);
        const [parent]=await candidates();
        assert.equal(parent.moves.length,16);
        assert.equal((await api('/api/game/blender-state',{id:parent.id})).status,'ready');
        const parentFolder=path.join(project,'blender-game-candidates',parent.id);
        const original=hash(await fs.readFile(path.join(parentFolder,'level.json')));
        await page.locator('#game-blender-repair').click();
        const request=await page.locator('#goal').inputValue();
        assert(request.includes(parent.id));
        const pausedRequest=request+' Pause after repair.';
        const first=await startText(pausedRequest);
        const pause=await until(async()=>{
            const row=await task(first.id);
            return row.status==='interrupted'?row:null;
        });
        assert.match(pause.reason,/Repair saved\. Resume/);
        assert.equal(pause.resumes,0);
        const retained=await child(first.id);
        assert.equal(retained.stage,'playtest');
        assert.equal(retained.diagnosis.edits.length,8);
        assert.equal(retained.diagnosis.originalSpan,2.5);
        assert.equal(retained.diagnosis.remainingSpan,.5);
        assert.equal(retained.diagnosis.lastTick,1728);
        assert.equal((await api('/api/game/blender-state',{id:retained.candidateId})).status,'ready');
        await mission(first.id);
        const screenshots=[await capture('mission-paused-after-repair.png')];
        const pid=app.process().pid;
        await app.close();
        ({app,page}=await launch(project));
        page.on('pageerror',error=>errors.push(error.message));
        await resize();
        assert.notEqual(app.process().pid,pid);
        assert.deepEqual(await child(first.id),retained);
        await mission(first.id);
        await page.locator('#mission044-resume').click();
        const done=await finish(first.id);
        assert.equal(done.resumes,1);
        const checked=await verify(first.id);
        assert(checked.display.passed&&checked.display.checks.length===3);
        assert.equal(checked.candidateId,retained.candidateId);
        assert.equal(checked.audit.playtest.trials,2);
        assert.deepEqual(checked.audit.diagnosis,retained.diagnosis);
        assert.equal((await candidates()).length,2);
        await page.waitForFunction(()=>document.querySelector('#mission044').dataset.status==='completed',{},flags);
        await page.waitForFunction(()=>document.querySelector('#mission044-facts').textContent.includes('Ground tiles moved: 8')
            &&document.querySelector('#mission044-facts').textContent.includes('Verified playtests: 2'),{},flags);
        screenshots.push(await capture('mission-native-repair-verified.png'));
        await page.locator('#mission044-result').click();
        await page.waitForFunction(()=>document.querySelector('#test-badge').textContent==='TESTS PASSED',{},flags);
        screenshots.push(await capture('blender-repair-evidence.png'));

        const artifact=await api('/api/artifact/read',{id:checked.report.id});
        assert(artifact.test.passed&&artifact.test.checks.length===3);
        const evidence=JSON.parse(artifact.contents['repair-evidence.json']);
        assert.deepEqual(evidence.audit,checked.audit);
        const fail=evidence.failed.attempt;
        const failing=fail.trials.at(-1);
        assert.equal(failing.trace.at(-1).tick,1728);
        assert(!failing.trace.at(-1).player.won);
        const baselinePicture=path.join(parentFolder,fail.id+'-trial-'+fail.trials.length+'.png');
        const bytes=await fs.readFile(baselinePicture);
        try{
            await fs.appendFile(baselinePicture,' ');
            await assert.rejects(verify(first.id),/finish screenshot changed/);
        }finally{await fs.writeFile(baselinePicture,bytes);}
        assert.deepEqual(await verify(first.id),checked);
        const receiptFile=path.join(project,'receipts',checked.report.id+'-1.json');
        const receipt=await fs.readFile(receiptFile);
        const modified=JSON.parse(receipt);
        modified.checks[0].actual.text='Altered heading';
        try{
            await fs.writeFile(receiptFile,JSON.stringify(modified));
            await assert.rejects(verify(first.id),/artifact test receipt changed|report checks changed/);
        }finally{await fs.writeFile(receiptFile,receipt);}
        assert.deepEqual(await verify(first.id),checked);

        const automatic=await startText(request);
        const auto=await finish(automatic.id);
        assert.equal(auto.resumes,0);
        const checkedAuto=await verify(automatic.id);
        assert.equal(checkedAuto.audit.failureSha256,checked.audit.failureSha256);
        assert.equal(checkedAuto.audit.playtest.traceSha256,checked.audit.playtest.traceSha256);
        assert.notEqual(checkedAuto.candidateId,checked.candidateId);
        assert.equal((await candidates()).length,3);
        assert.equal(hash(await fs.readFile(path.join(parentFolder,'level.json'))),original);
        assert.deepEqual(errors,[]);
        const result={passed:true,blenderSha256:parent.blendSha256,
            audit:checked.audit,report:checked.report,automatic:{id:automatic.id,candidateId:checkedAuto.candidateId,resumes:auto.resumes},
            recovery:{id:first.id,candidateId:checked.candidateId,resumes:done.resumes,taskRetained:true},
            checks:{sourceScreenshotTamperRefused:true,browserReceiptTamperRefused:true,parentBytesPreserved:true,
                browserChecks:artifact.test.checks.length},
            screenshots};
        await fs.writeFile(path.join(output,'repair-evidence.json'),artifact.contents['repair-evidence.json']);
        await fs.writeFile(path.join(output,'result.json'),JSON.stringify(result,null,2)+'\n');
        console.log(JSON.stringify(result));
    }catch(error){
        const state=await api('/api/agency043/state').catch(()=>null);
        if(state)for(const row of state.tasks)console.error(JSON.stringify({goal:row.goal,status:row.status,reason:row.reason,child:await child(row.id).catch(()=>null)}));
        console.error(await page.locator('#notice').textContent().catch(()=>null));
        throw error;
    }finally{await app.close().catch(()=>{});}
}

main().catch(error=>{console.error(error);process.exitCode=1;});
