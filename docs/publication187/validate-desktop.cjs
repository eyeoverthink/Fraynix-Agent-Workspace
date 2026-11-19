'use strict';
const assert = require('node:assert/strict');
const {createHash} = require('node:crypto');
const fs = require('node:fs/promises');
const path = require('node:path');
const {launch} = require('../../workspace-desktop/test/calibration-integration.cjs');

const pendingCount=125;
const hash=bytes=>createHash('sha256').update(bytes).digest('hex');

function artifactId(reviewId) {
    const bytes=createHash('md5').update('request-workbench:'+reviewId).digest();
    bytes[6]=(bytes[6]&0x0f)|0x30;
    bytes[8]=(bytes[8]&0x3f)|0x80;
    return bytes.toString('hex').replace(/^(........)(....)(....)(....)(............)$/,'$1-$2-$3-$4-$5');
}

async function main() {
    const output=path.resolve(process.argv[2]||'build/edition187-live');
    const priorProject=process.argv[3]&&path.resolve(process.argv[3]);
    const project=path.join(output,'project');
    const workbench=path.join(project,'request-workbench');
    const faults=path.join(output,'faults');
    await fs.mkdir(output);
    await fs.mkdir(project);
    await fs.mkdir(workbench);
    await fs.mkdir(faults);
    const result={captures:[],failures:[],priorEditions:[],copiedVerifiedReviews:0};
    if(priorProject) {
        const previous=path.join(priorProject,'request-workbench');
        for(const name of await fs.readdir(previous)) {
            if(!name.endsWith('.task.json')) continue;
            const raw=await fs.readFile(path.join(previous,name));
            const task=JSON.parse(raw).data;
            if(task.status==='review'&&task.frayFirstChecks?.length===5&&!task.frayChecks&&!task.report) {
                await fs.writeFile(path.join(workbench,name),raw,{flag:'wx'});
                result.copiedVerifiedReviews++;
            }
        }
    }
    Object.assign(process.env,{FRAYNIX_WORKSPACE_NO_MODELS:'1',FRAYNIX_WORKSPACE_PERSISTENT:'0',
        FRAYNIX_WORKSPACE_JAVA_HEAP_MIB:'768',FRAYNIX_WORKSPACE_JAVA_PROCESSORS:'2'});
    let {app,page}=await launch(project);
    const api=(route,args)=>app.evaluate((_,[route,args])=>global.workspaceTest.api(route,args),[route,args]);
    const reviewFile=id=>path.join(workbench,id+'.task.json');
    const readReview=async id=>JSON.parse(await fs.readFile(reviewFile(id),'utf8')).data;
    const artifact=(id)=>api('/api/artifact/read',{id});
    const verify=(id)=>api('/api/requests/verify',{id});
    const state=()=>api('/api/state');
    async function ready() {
        await app.evaluate(({BrowserWindow})=>BrowserWindow.getAllWindows().find(w=>w.isVisible()).setSize(1480,1030));
        assert.equal((await state()).models.length,0);
    }
    const capture=async name=>{
        await page.evaluate(()=>new Promise(resolve=>requestAnimationFrame(()=>requestAnimationFrame(resolve))));
        const bytes=Buffer.from(await app.evaluate(()=>global.workspaceTest.capture()),'base64');
        await fs.writeFile(path.join(output,name),bytes);
        result.captures.push({name,sha256:hash(bytes)});
    };
    const openWorkbench=()=>page.locator('#requests-open').click();
    async function closeWorkbench() {
        await page.locator('#requests-close').click();
        await page.locator('#requests-dialog').waitFor({state:'hidden'});
    }
    async function chooser(size) {
        const start=performance.now();
        await page.locator('#requests-recover-chain').click();
        await page.waitForFunction(n=>{
            const select=document.querySelector('#requests-pending-chain');
            return !document.querySelector('#requests-pending').hidden&&select.options.length===n+1;
        },size);
        return Math.round(performance.now()-start);
    }
    async function choose(id,size) {
        await openWorkbench();
        await chooser(size);
        await page.locator('#requests-pending-chain').selectOption(id);
        await page.locator('#requests-load-chain').click();
        await page.locator('#requests-pending').waitFor({state:'hidden'});
        assert.equal(await page.evaluate(()=>localStorage.getItem('fraynix-request-review')),id);
    }
    async function stage(i) {
        const quantity=((i+101)/16).toFixed(4);
        const request=i%2 ? 'Convert '+quantity+' C to F, then F to R.'
            : 'Convert '+quantity+' inches to mm, then mm to cm.';
        await page.locator('#goal').fill(request);
        await page.locator('#run').click();
        await page.getByRole('button',{name:'Verify both conversions',exact:true}).waitFor({state:'visible'});
        const id=await page.evaluate(()=>localStorage.getItem('fraynix-request-review'));
        await page.locator('#requests-pause-chain').check();
        await page.locator('#requests-confirm').click();
        await page.getByRole('button',{name:'Resume verified handoff',exact:true}).waitFor({state:'visible'});
        const record=await readReview(id);
        assert(record.frayFirstChecks.length===5&&!record.frayChecks&&!record.report);
        await closeWorkbench();
        return id;
    }
    async function waitForWriteFailure(id,message) {
        await page.getByRole('button',{name:'Resume verified handoff',exact:true}).click();
        await page.getByRole('button',{name:'Finish verified save',exact:true}).waitFor({state:'visible'});
        assert.match(await page.locator('#requests-status').textContent(),message);
        const stored=await readReview(id);
        assert(stored.status==='review'&&!stored.report&&stored.frayChecks.length===5&&stored.frayFirstChecks.length===5);
        assert.match(stored.fraySaveSha256,/^[a-f0-9]{64}$/);
        await assert.rejects(verify(id));
        return stored;
    }
    async function finalize(id,stored) {
        await app.evaluate(({webContents})=>{
            global.retryPaths=[];
            const canvas=webContents.fromId(global.workspaceTest.surfaces().canvas);
            global.trackRetryNavigation=(_,url)=>global.retryPaths.push(new URL(url).pathname);
            canvas.on('did-navigate',global.trackRetryNavigation);
        });
        await page.getByRole('button',{name:'Finish verified save',exact:true}).click();
        await page.locator('#requests-dialog').waitFor({state:'hidden'});
        const intermediate=stored.frayFirstChecks[3].actual.text.trim();
        const message='Both Fray steps passed. The second used the verified '+intermediate+'.';
        await page.locator('#messages').getByText(message,{exact:true}).waitFor({state:'visible'});
        await page.waitForFunction(()=>document.querySelector('#test-badge').textContent==='TESTS PASSED');
        const verified=await verify(id);
        const stableId=artifactId(id);
        assert.equal(verified.artifact.id,stableId);
        assert.equal(verified.artifact.version,1);
        const saved=await artifact(stableId);
        const proof=JSON.parse(saved.contents['verification.json']);
        assert.deepEqual(proof.firstChecks,stored.frayFirstChecks);
        assert.deepEqual(proof.secondChecks,stored.frayChecks);
        assert(saved.test?.passed&&saved.test.checks.length===10);
        const navigations=await app.evaluate(({webContents})=>{
            const canvas=webContents.fromId(global.workspaceTest.surfaces().canvas);
            canvas.off('did-navigate',global.trackRetryNavigation);
            return global.retryPaths;
        });
        assert.deepEqual(navigations,['/artifact/'+stableId+'/1/index.html']);
        return {verified,saved,proof};
    }
    try {
        await ready();
        const initial=(await state()).artifacts.length;
        const staged=[];
        for(let i=result.copiedVerifiedReviews;i<pendingCount;i++) {
            staged.push(await stage(i));
            if((i+1)%25===0) console.log(JSON.stringify({paused:i+1}));
        }
        const afterStage=await api('/api/requests/pending-chains',{});
        assert.equal(afterStage.chains.length,pendingCount);
        for(let i=0;i<3;i++) {
            const extra=await api('/api/requests/chat',{request:'Convert '+(60+i)+' C to F, then F to R.'});
            assert.equal(extra.task.status,'review');
            assert(!extra.task.firstVerified);
        }
        await assert.rejects(api('/api/requests/chat',{request:'Convert 100 C to F, then F to R.'}),/128/);
        result.reviewFilesAtCapacity=(await fs.readdir(workbench)).filter(name=>name.endsWith('.task.json')).length;
        assert.equal(result.reviewFilesAtCapacity,128);
        const snapshots=new Map();
        for(const name of await fs.readdir(workbench)) {
            if(name.endsWith('.task.json')) snapshots.set(name,hash(await fs.readFile(path.join(workbench,name))));
        }
        const samples=[];
        for(let i=0;i<7;i++) {
            await openWorkbench();
            samples.push(await chooser(pendingCount));
            if(i===6) await capture('near-capacity-chooser.png');
            await closeWorkbench();
        }
        const sorted=[...samples].sort((a,b)=>a-b);
        result.chooser={pending:pendingCount,samples,medianMs:sorted[3],maxMs:sorted.at(-1)};
        const targetA=staged[10], targetB=staged[11];
        assert(targetA&&targetB);

        const idA=artifactId(targetA);
        const blockA=path.join(project,'artifacts',idA);
        await fs.writeFile(blockA,'Test obstruction for the artifact directory',{flag:'wx'});
        const checkpoint=path.join(project,'workspace.fworld');
        const before=hash(await fs.readFile(checkpoint));
        await choose(targetA,pendingCount);
        const a=await waitForWriteFailure(targetA,/Could not write the artifact files or register the save/);
        assert.equal((await state()).artifacts.length,initial);
        assert.equal(hash(await fs.readFile(checkpoint)),before);
        await capture('artifact-write-failure.png');
        const pid=app.process().pid;
        await app.close();
        await fs.rename(blockA,path.join(faults,'artifact-destination.txt'));
        ({app,page}=await launch(project));
        await ready();
        assert.notEqual(app.process().pid,pid);
        assert.deepEqual(await readReview(targetA),a);
        await choose(targetA,pendingCount);
        const finishedA=await finalize(targetA,a);
        assert.equal((await state()).artifacts.length,initial+1);
        result.failures.push({kind:'artifact-directory',registeredBeforeRetry:false,restarted:true,id:targetA,
            artifact:finishedA.verified.artifact,retainedChecks:[5,5],previewReplayed:false,result:finishedA.proof.result});

        const idB=artifactId(targetB);
        const receipts=path.join(project,'receipts');
        const parked=path.join(project,'receipts-before-fault');
        assert((await fs.lstat(receipts)).isDirectory());
        await fs.rename(receipts,parked);
        await fs.writeFile(receipts,'Test obstruction for the browser receipt directory',{flag:'wx'});
        let b;
        let untested;
        try {
            await choose(targetB,pendingCount-1);
            b=await waitForWriteFailure(targetB,/Could not write the browser receipt/);
            untested=await artifact(idB);
            assert.equal(untested.version,1);
            assert(!untested.test);
            assert.equal((await state()).artifacts.length,initial+2);
            await capture('receipt-write-failure.png');
        } finally {
            await fs.rename(receipts,path.join(faults,'receipt-destination.txt'));
            await fs.rename(parked,receipts);
        }
        const finishedB=await finalize(targetB,b);
        assert.equal(finishedB.saved.created,untested.created);
        assert.deepEqual(finishedB.saved.contents,untested.contents);
        assert.equal((await state()).artifacts.length,initial+2);
        assert.deepEqual(await verify(targetA),finishedA.verified);
        assert.equal((await api('/api/requests/pending-chains',{})).chains.length,123);
        for(const [name,old] of snapshots) {
            if(name===targetA+'.task.json'||name===targetB+'.task.json') continue;
            assert.equal(hash(await fs.readFile(path.join(workbench,name))),old,name);
        }
        result.failures.push({kind:'receipt-directory',registeredBeforeRetry:true,restarted:false,id:targetB,
            artifact:finishedB.verified.artifact,retainedChecks:[5,5],previewReplayed:false,
            sameRegisteredVersion:true,result:finishedB.proof.result});
        await fs.writeFile(path.join(output,'browser-receipt.json'),JSON.stringify(finishedB.saved.test,null,2)+'\n');
        result.completed={added:2,pending:123,unchangedReviews:snapshots.size-2};
        if(priorProject) {
            await app.close();
            const priorCopy=path.join(output,'compatibility');
            await fs.mkdir(priorCopy);
            for(const name of ['workspace.fworld','workspace.fworld.previous','artifacts','receipts','request-workbench'])
                await fs.cp(path.join(priorProject,name),path.join(priorCopy,name),{recursive:true});
            ({app,page}=await launch(priorCopy));
            await ready();
            for(const edition of [181,182,183,184,185,186]) {
                const prior=JSON.parse(await fs.readFile('docs/readmes/'+edition+'/verification.json','utf8'));
                const refs=edition===185?prior.completed.map(row=>[row.id,row.verified]):[[prior.reviewId,prior.verified]];
                for(const [id,proof] of refs) assert.deepEqual(await verify(id),proof);
                result.priorEditions.push(edition);
            }
        }
        result.passed=true;
        await fs.writeFile(path.join(output,'result.json'),JSON.stringify(result,null,2)+'\n');
        console.log(JSON.stringify({passed:true,chooser:result.chooser,capacity:result.reviewFilesAtCapacity,
            completed:result.completed,prior:result.priorEditions}));
    } catch(error) {
        await fs.writeFile(path.join(output,'failure.txt'),error.stack+'\n'+await page.locator('body').innerText()).catch(()=>{});
        throw error;
    } finally {
        await app.close().catch(()=>{});
    }
}

main().catch(error=>{console.error(error);process.exitCode=1;});
