'use strict';
const assert = require('node:assert/strict');
const {spawn} = require('node:child_process');
const {once} = require('node:events');
const {setTimeout:delay} = require('node:timers/promises');
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

async function acquireReview(file) {
    const java=process.env.JAVA_HOME?path.join(process.env.JAVA_HOME,'bin','java.exe'):'java';
    const lease=spawn(java,[path.resolve(__dirname,'ReviewWriteLease.java'),file],{windowsHide:true});
    let error='';
    lease.stderr.on('data',chunk=>{error+=chunk;});
    try {
        const ready=await Promise.race([once(lease.stdout,'data').then(([chunk])=>String(chunk).trim()),
            once(lease,'exit').then(()=>{throw Error(error||'The review lease exited');}),
            delay(15000,null,{ref:false})]);
        assert.equal(ready,'locked',error);
        return lease;
    } catch (failure) {
        lease.stdin.end();
        throw failure;
    }
}

async function main() {
    const output=path.resolve(process.argv[2]||'build/edition188-live');
    const priorProject=process.argv[3]&&path.resolve(process.argv[3]);
    const project=path.join(output,'project');
    const workbench=path.join(project,'request-workbench');
    await fs.mkdir(output);
    await fs.mkdir(project);
    await fs.mkdir(workbench);
    const result={captures:[],priorEdition187Proofs:0,copiedVerifiedReviews:0};
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
        for(let i=result.copiedVerifiedReviews;i<pendingCount;i++) staged.push(await stage(i));
        const target=staged.at(-1);
        assert(target);
        const stableId=artifactId(target);
        const before=(await readReview(target)).frayFirstChecks;

        assert.equal((await api('/api/requests/pending-chains',{})).chains.length,pendingCount);
        for(let i=0;i<3;i++) {
            const extra=await api('/api/requests/chat',{request:'Convert '+(60+i)+' C to F, then F to R.'});
            assert.equal(extra.task.status,'review');
            assert(!extra.task.firstVerified);
        }
        await assert.rejects(api('/api/requests/chat',{request:'Convert 100 C to F, then F to R.'}),/128/);
        const snapshots=new Map();
        for(const name of await fs.readdir(workbench)) {
            if(name.endsWith('.task.json')) snapshots.set(name,hash(await fs.readFile(path.join(workbench,name))));
        }
        assert.equal(snapshots.size,128);

        // First leave valid browser evidence on disk through the path qualified in Edition 187.
        const obstruction=path.join(project,'artifacts',stableId);
        await fs.writeFile(obstruction,'Block the artifact directory',{flag:'wx'});
        await choose(target,pendingCount);
        const retained=await waitForWriteFailure(target,/Could not write the artifact files or register the save/);
        assert.deepEqual(retained.frayFirstChecks,before);
        await fs.rename(obstruction,path.join(output,'artifact-obstruction.txt'));

        const review=reviewFile(target);
        const receiptPath=path.join(project,'receipts',stableId+'-1.json');
        const reviewBytes=await fs.readFile(review);
        const lease=await acquireReview(review);
        try {
            await page.getByRole('button',{name:'Finish verified save',exact:true}).click();
            await page.getByRole('button',{name:'Finish verified save',exact:true}).waitFor({state:'visible'});
            assert.match(await page.locator('#requests-status').textContent(),/Could not finish the review/);
            assert.match(await page.locator('#requests-source').textContent(),/Both steps verified/);
            assert.deepEqual(await fs.readFile(review),reviewBytes);
            await assert.rejects(verify(target));
            await capture('final-review-write-blocked.png');
        } finally {
            const released=once(lease,'exit');
            lease.stdin.end();
            assert.equal((await released)[0],0);
        }
        const pending=(await fs.readdir(workbench)).filter(name=>name.startsWith(target+'.task.json.')&&name.endsWith('.pending'));
        assert.equal(pending.length,1);
        const uncommitted=JSON.parse(await fs.readFile(path.join(workbench,pending[0]),'utf8')).data;
        assert.equal(uncommitted.status,'verified');
        assert.equal(uncommitted.report.id,stableId);
        assert.equal((await state()).artifacts.length,initial+1);
        const alreadySaved=await artifact(stableId);
        const originalReceipt=await fs.readFile(receiptPath);
        assert.equal(alreadySaved.version,1);
        assert.equal(alreadySaved.test.checks.length,10);
        assert(alreadySaved.test.passed);

        const pid=app.process().pid;
        await app.close();
        ({app,page}=await launch(project));
        await ready();
        assert.notEqual(app.process().pid,pid);
        assert.deepEqual(await fs.readFile(review),reviewBytes);
        await assert.rejects(verify(target));
        await choose(target,pendingCount);
        await capture('ready-after-restart.png');

        // A mismatched existing receipt must block recovery instead of being overwritten.
        const changed=JSON.parse(originalReceipt);
        changed.checks[9].actual.text='Different browser result';
        const changedBytes=Buffer.from(JSON.stringify(changed));
        await fs.writeFile(receiptPath,changedBytes);
        await page.getByRole('button',{name:'Finish verified save',exact:true}).click();
        await page.getByRole('button',{name:'Finish verified save',exact:true}).waitFor({state:'visible'});
        assert.match(await page.locator('#requests-status').textContent(),/original artifact test receipt changed/);
        assert.deepEqual(await fs.readFile(receiptPath),changedBytes);
        assert.deepEqual(await fs.readFile(review),reviewBytes);
        await fs.writeFile(receiptPath,originalReceipt);

        const finished=await finalize(target,retained);
        assert.equal((await state()).artifacts.length,initial+1);
        assert.equal(finished.saved.created,alreadySaved.created);
        assert.deepEqual(finished.saved.contents,alreadySaved.contents);
        assert.deepEqual(await fs.readFile(receiptPath),originalReceipt);
        assert.deepEqual(finished.verified.artifact,uncommitted.report);
        const replay = await app.evaluate((_, steps) => global.workspaceTest.canvasChecks(steps), finished.saved.test.steps);
        assert.deepEqual(replay, finished.saved.test.checks);
        await capture('verified-artifact.png');

        // The completed proof must read the receipt even after the artifact view cached it.
        await fs.writeFile(receiptPath,changedBytes);
        await assert.rejects(verify(target),/original artifact test receipt changed/);
        assert.deepEqual(await fs.readFile(receiptPath),changedBytes);
        const removed=path.join(output,'held-receipt.json');
        await fs.rename(receiptPath,removed);
        await assert.rejects(verify(target),/original artifact test receipt is missing/);
        await assert.rejects(fs.stat(receiptPath),{code:'ENOENT'});
        await fs.writeFile(receiptPath,originalReceipt,{flag:'wx'});
        assert.deepEqual(await verify(target),finished.verified);
        assert.deepEqual(await fs.readFile(receiptPath),originalReceipt);

        assert.equal((await api('/api/requests/pending-chains',{})).chains.length,pendingCount-1);
        for(const [name,old] of snapshots) {
            if(name===target+'.task.json') continue;
            assert.equal(hash(await fs.readFile(path.join(workbench,name))),old,name);
        }
        result.reviewFilesAtCapacity=snapshots.size;
        result.pendingBefore=pendingCount;
        result.pendingAfter=pendingCount-1;
        result.unchangedReviews=snapshots.size-1;
        result.recovery={restarted:true,finalReviewBlocked:true,receiptAlreadySaved:true,retainedChecks:[5,5],
            addedArtifacts:1,originalReceiptSha256:hash(originalReceipt),receiptBytesPreserved:true,
            previewReplayed:false,id:target,verified:finished.verified,result:finished.proof.result};
        result.refusals={changedReceiptDuringRecovery:true,changedReceiptAfterCompletion:true,
            missingReceiptAfterCompletion:true,restoredProofMatches:true};
        await fs.writeFile(path.join(output,'browser-receipt.json'),originalReceipt);

        if(priorProject) {
            await app.close();
            const previous=path.join(output,'compatibility');
            await fs.mkdir(previous);
            for(const name of ['workspace.fworld','workspace.fworld.previous','artifacts','receipts','request-workbench'])
                await fs.cp(path.join(priorProject,name),path.join(previous,name),{recursive:true});
            ({app,page}=await launch(previous));
            await ready();
            const publication=JSON.parse(await fs.readFile('docs/readmes/187/verification.json','utf8'));
            for(const row of publication.failures) {
                const verified=await verify(row.id);
                assert(verified.passed);
                assert.deepEqual(verified.artifact,row.artifact);
                result.priorEdition187Proofs++;
            }
        }
        result.passed=true;
        await fs.writeFile(path.join(output,'result.json'),JSON.stringify(result,null,2)+'\n');
        console.log(JSON.stringify({passed:true,recovery:result.recovery,refusals:result.refusals,
            pendingAfter:result.pendingAfter,unchangedReviews:result.unchangedReviews}));
    } catch(error) {
        await fs.writeFile(path.join(output,'failure.txt'),error.stack+'\n'+await page.locator('body').innerText()).catch(()=>{});
        throw error;
    } finally {
        await app.close().catch(()=>{});
    }
}

main().catch(error=>{console.error(error);process.exitCode=1;});
