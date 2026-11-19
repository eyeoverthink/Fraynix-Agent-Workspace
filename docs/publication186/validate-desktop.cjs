'use strict';
const assert = require('node:assert/strict');
const {execFile,spawn} = require('node:child_process');
const {once} = require('node:events');
const {createHash} = require('node:crypto');
const fs = require('node:fs/promises');
const path = require('node:path');
const {promisify} = require('node:util');
const {setTimeout:delay} = require('node:timers/promises');
const {launch} = require('../../workspace-desktop/test/calibration-integration.cjs');

const hash = bytes => createHash('sha256').update(bytes).digest('hex');
const expectedNames = ['fray-source.txt','index.html','verification.json'];
const levels = [3,24,60];

function artifactId(reviewId) {
    const bytes = createHash('md5').update('request-workbench:'+reviewId).digest();
    bytes[6]=(bytes[6]&0x0f)|0x30;
    bytes[8]=(bytes[8]&0x3f)|0x80;
    return bytes.toString('hex').replace(/^(........)(....)(....)(....)(............)$/,'$1-$2-$3-$4-$5');
}

async function acquireCheckpoint(file) {
    const java=process.env.JAVA_HOME?path.join(process.env.JAVA_HOME,'bin','java.exe'):'java';
    const lease=spawn(java,[path.resolve(__dirname,'CheckpointLease.java'),file],{windowsHide:true});
    let error='';
    lease.stderr.on('data',chunk=>{error+=chunk;});
    try {
        const ready=await Promise.race([once(lease.stdout,'data').then(([chunk])=>String(chunk).trim()),
            once(lease,'exit').then(()=>{throw Error(error||'The checkpoint lease exited');}),
            delay(15000,null,{ref:false})]);
        assert.equal(ready,'locked',error);
        return lease;
    } catch (failure) {
        lease.stdin.end();
        throw failure;
    }
}

async function eventually(action, ms=20000) {
    const until=performance.now()+ms;
    for (;;) {
        try { return await action(); }
        catch(error) { if(performance.now()>=until) throw error; }
        await delay(30);
    }
}

async function main() {
    const output=path.resolve(process.argv[2]||'build/edition186-live');
    const project=path.join(output,'project');
    await fs.mkdir(output);
    await fs.mkdir(project);
    const prior=[];
    if(process.argv[3]) {
        for(const name of ['workspace.fworld','workspace.fworld.previous','artifacts','receipts','request-workbench'])
            await fs.cp(path.join(process.argv[3],name),path.join(project,name),{recursive:true});
        for(const edition of [181,182,183,184,185]) {
            const record=JSON.parse(await fs.readFile('docs/readmes/'+edition+'/verification.json','utf8'));
            const refs=edition===185?record.completed.map(item=>({id:item.id,proof:item.verified}))
                : [{id:record.reviewId,proof:record.verified}];
            prior.push({edition,refs});
        }
    }
    Object.assign(process.env,{FRAYNIX_WORKSPACE_NO_MODELS:'1',FRAYNIX_WORKSPACE_PERSISTENT:'0',
        FRAYNIX_WORKSPACE_JAVA_HEAP_MIB:'768',FRAYNIX_WORKSPACE_JAVA_PROCESSORS:'2'});
    let {app,page}=await launch(project);
    let lease=null;
    const result={captures:[],chooser:[]};
    const api=(route,args)=>app.evaluate((_,[route,args])=>global.workspaceTest.api(route,args),[route,args]);
    const reviewFile=id=>path.join(project,'request-workbench',id+'.task.json');
    const readReview=async id=>JSON.parse(await fs.readFile(reviewFile(id),'utf8')).data;
    const capture=async name=>{
        await page.evaluate(()=>new Promise(resolve=>requestAnimationFrame(()=>requestAnimationFrame(resolve))));
        const bytes=Buffer.from(await app.evaluate(()=>global.workspaceTest.capture()),'base64');
        await fs.writeFile(path.join(output,name),bytes);
        result.captures.push({name,sha256:hash(bytes)});
    };
    async function ready() {
        await app.evaluate(({BrowserWindow})=>BrowserWindow.getAllWindows().find(w=>w.isVisible()).setSize(1480,1030));
        assert.equal((await api('/api/state')).models.length,0);
    }
    async function checkPrior() {
        for(const {refs} of prior)
            for(const ref of refs) assert.deepEqual(await api('/api/requests/verify',{id:ref.id}),ref.proof);
    }
    const openWorkbench=()=>page.locator('#requests-open').click();
    async function closeWorkbench() {
        await page.locator('#requests-close').click();
        await page.locator('#requests-dialog').waitFor({state:'hidden'});
    }
    async function chooser(size) {
        const started=performance.now();
        await page.locator('#requests-recover-chain').click();
        await page.waitForFunction(n=>{
            const select=document.querySelector('#requests-pending-chain');
            return !document.querySelector('#requests-pending').hidden&&select.options.length===n+1;
        },size);
        return Math.round(performance.now()-started);
    }
    async function select(id) {
        await page.locator('#requests-pending-chain').selectOption(id);
        await page.locator('#requests-load-chain').click();
        await page.locator('#requests-pending').waitFor({state:'hidden'});
        assert.equal(await page.evaluate(()=>localStorage.getItem('fraynix-request-review')),id);
    }
    try {
        await ready();
        await checkPrior();
        const initial=(await api('/api/state')).artifacts.length;
        const ids=[];
        for(let i=0;i<levels.at(-1);i++) {
            const quantity=((i+1)/16).toFixed(4);
            const request=i%2
                ? 'Convert '+quantity+' C to F, then F to R.'
                : 'Convert '+quantity+' inches to mm, then mm to cm.';
            await page.locator('#goal').fill(request);
            await page.locator('#run').click();
            await page.getByRole('button',{name:'Verify both conversions',exact:true}).waitFor({state:'visible'});
            const id=await page.evaluate(()=>localStorage.getItem('fraynix-request-review'));
            await page.locator('#requests-pause-chain').check();
            await page.locator('#requests-confirm').click();
            await page.getByRole('button',{name:'Resume verified handoff',exact:true}).waitFor({state:'visible'});
            const review=await readReview(id);
            assert(review.frayFirstChecks.length===5&&!review.frayChecks&&!review.report);
            ids.push(id);
            await closeWorkbench();
            if(levels.includes(ids.length)) {
                const samples=[];
                for(let sample=0;sample<7;sample++) {
                    await openWorkbench();
                    samples.push(await chooser(ids.length));
                    assert(await page.locator('#requests-load-chain').isDisabled());
                    if(ids.length===levels.at(-1)&&sample===6) await capture('large-chooser.png');
                    await closeWorkbench();
                }
                const sorted=[...samples].sort((a,b)=>a-b);
                result.chooser.push({pending:ids.length,samples,medianMs:sorted[3],maxMs:sorted.at(-1)});
                console.log(JSON.stringify(result.chooser.at(-1)));
            }
        }
        assert.equal(initial,(await api('/api/state')).artifacts.length);
        const id=ids[28];
        const other=ids[29];
        const otherBytes=await fs.readFile(reviewFile(other));
        const stableId=artifactId(id);
        const artifactDir=path.join(project,'artifacts',stableId,'versions','1');
        const checkpoint=path.join(project,'workspace.fworld');
        const checkpointBefore=hash(await fs.readFile(checkpoint));

        await openWorkbench();
        await chooser(60);
        await select(id);
        lease=await acquireCheckpoint(checkpoint+'.lock');
        await page.getByRole('button',{name:'Resume verified handoff',exact:true}).click();

        const interrupted=await eventually(async()=>{
            const task=await readReview(id);
            assert(task.frayFirstChecks.length===5&&task.frayChecks.length===5&&!task.report);
            assert.match(task.fraySaveSha256,/^[a-f0-9]{64}$/);
            const files=await fs.readdir(artifactDir);
            assert.deepEqual(files.sort(),expectedNames);
            const proof=JSON.parse(await fs.readFile(path.join(artifactDir,'verification.json'),'utf8'));
            assert.equal(proof.result,'4.60375 cm');
            assert.deepEqual(proof.secondChecks,task.frayChecks);
            assert.equal(hash(await fs.readFile(checkpoint)),checkpointBefore);
            return {task,files,proof};
        });
        const child=app.process();
        const stopped=once(child,'exit');
        assert(Number.isSafeInteger(child.pid)&&child.pid>0&&child.exitCode===null);
        const firstPid=child.pid;
        await promisify(execFile)('taskkill.exe',['/PID',String(firstPid),'/T','/F'],{windowsHide:true});
        await stopped;
        const unblocked=once(lease,'exit');
        lease.stdin.end();
        await unblocked;
        lease=null;
        ({app,page}=await launch(project));
        await ready();
        assert.notEqual(app.process().pid,firstPid);
        assert.equal(initial,(await api('/api/state')).artifacts.length);
        assert.deepEqual(await readReview(id),interrupted.task);
        await assert.rejects(api('/api/requests/verify',{id}));
        await page.evaluate(()=>localStorage.removeItem('fraynix-request-review'));

        await openWorkbench();
        await chooser(60);
        await select(id);
        await page.getByRole('button',{name:'Finish verified save',exact:true}).waitFor({state:'visible'});
        await capture('interrupted-save.png');
        await app.evaluate(({webContents})=>{
            global.recoveryNavigations=[];
            const canvas=webContents.fromId(global.workspaceTest.surfaces().canvas);
            canvas.on('did-navigate',(_,url)=>global.recoveryNavigations.push(new URL(url).pathname));
        });
        await page.locator('#requests-confirm').click();
        await page.locator('#messages').getByText('Both Fray steps passed. The second used the verified 46.0375 mm.',{exact:true})
            .waitFor({state:'visible'});
        const verified=await api('/api/requests/verify',{id});
        assert.equal(verified.artifact.id,stableId);
        assert.equal(verified.artifact.version,1);
        assert.equal(initial+1,(await api('/api/state')).artifacts.length);
        const artifact=await api('/api/artifact/read',{id:stableId});
        const proof=JSON.parse(artifact.contents['verification.json']);
        assert.deepEqual(proof,interrupted.proof);
        assert(artifact.test.passed&&artifact.test.checks.length===10);
        assert.deepEqual(await fs.readFile(reviewFile(other)),otherBytes);
        assert.equal((await api('/api/requests/pending-chains',{})).chains.length,59);
        assert.deepEqual(await app.evaluate(()=>global.recoveryNavigations),['/artifact/'+stableId+'/1/index.html']);
        const preserved=(await fs.readdir(path.join(project,'recovery'))).filter(name=>name.startsWith(stableId+'-v1-'));
        assert.equal(preserved.length,1);
        assert.deepEqual(JSON.parse(await fs.readFile(path.join(project,'recovery',preserved[0],'verification.json'),'utf8')),
            interrupted.proof);

        const checks=await app.evaluate((_,steps)=>global.workspaceTest.canvasChecks(steps),artifact.test.steps);
        assert(checks.every(check=>check.passed));
        await capture('recovered-artifact.png');
        await fs.writeFile(path.join(output,'browser-receipt.json'),JSON.stringify(artifact.test,null,2)+'\n');

        await checkPrior();
        Object.assign(result,{passed:true,reviewId:id,verified,proof,priorEditions:prior.map(row=>row.edition),
            interruption:{forced:true,registeredBeforeRestart:false,retainedChecks:[5,5],
                incompleteFiles:interrupted.files,orphanPreserved:true,previewReplayed:false,
                artifactsAdded:1,pendingAfterSave:59,siblingUnchanged:true}});
        await fs.writeFile(path.join(output,'result.json'),JSON.stringify(result,null,2)+'\n');
        console.log(JSON.stringify({passed:true,chooser:result.chooser,interruption:result.interruption}));
    } catch(error) {
        await fs.writeFile(path.join(output,'failure.txt'),error.stack+'\n'+await page.locator('body').innerText()).catch(()=>{});
        throw error;
    } finally {
        if(lease) lease.stdin.end();
        await app.close().catch(()=>{});
    }
}

main().catch(error=>{console.error(error);process.exitCode=1;});
