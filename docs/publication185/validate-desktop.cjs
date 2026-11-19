'use strict';
const assert = require('node:assert/strict');
const {execFile} = require('node:child_process');
const {once} = require('node:events');
const {createHash} = require('node:crypto');
const fs = require('node:fs/promises');
const path = require('node:path');
const {promisify} = require('node:util');
const {launch} = require('../../workspace-desktop/test/calibration-integration.cjs');

const requests = [
    {name:'near-zero', text:'Convert -17.7778 C to F, then F to R.', input:'-17.7778',
        intermediate:{value:'-0.00004',unit:'\u00b0F'}, result:'459.66996 \u00b0R'},
    {name:'length', text:'Convert 12.3456 inches to millimetres, then millimetres to centimetres.',
        input:'12.3456', intermediate:{value:'313.57824',unit:'mm'}, result:'31.357824 cm'},
    {name:'body-temperature', text:'Convert 37.25 C to F, then F to R.', input:'37.25',
        intermediate:{value:'99.05',unit:'\u00b0F'}, result:'558.72 \u00b0R'}
];
const hash = bytes => createHash('sha256').update(bytes).digest('hex');

async function main() {
    const output = path.resolve(process.argv[2] || 'build/edition185-live');
    const project = path.join(output,'project');
    await fs.mkdir(output);
    await fs.mkdir(project);
    const prior = [];
    if (process.argv[3]) {
        for (const name of ['workspace.fworld','workspace.fworld.previous','artifacts','receipts','request-workbench']) {
            await fs.cp(path.join(process.argv[3],name),path.join(project,name),{recursive:true});
        }
        for (const edition of [181,182,183,184]) {
            const record=JSON.parse(await fs.readFile('docs/readmes/'+edition+'/verification.json','utf8'));
            prior.push({edition,id:record.reviewId,proof:record.verified});
        }
    }
    Object.assign(process.env,{FRAYNIX_WORKSPACE_NO_MODELS:'1',FRAYNIX_WORKSPACE_PERSISTENT:'0',
        FRAYNIX_WORKSPACE_JAVA_HEAP_MIB:'768',FRAYNIX_WORKSPACE_JAVA_PROCESSORS:'2'});
    let {app,page}=await launch(project);
    const result={captures:[],pending:[],completed:[],rejectedHandoffs:[]};
    const api=(route,args)=>app.evaluate((_,[route,args])=>global.workspaceTest.api(route,args),[route,args]);
    const state=()=>page.evaluate(()=>window.workspace.request({operation:'state'}));
    async function ready() {
        await app.evaluate(({BrowserWindow})=>BrowserWindow.getAllWindows().find(w=>w.isVisible()).setSize(1480,1030));
        assert.equal((await api('/api/state')).models.length,0);
    }
    async function checkPrior() {
        for(const row of prior) assert.deepEqual(await api('/api/requests/verify',{id:row.id}),row.proof);
    }
    async function capture(name) {
        await page.evaluate(()=>new Promise(resolve=>requestAnimationFrame(()=>requestAnimationFrame(resolve))));
        const bytes=Buffer.from(await app.evaluate(()=>global.workspaceTest.capture()),'base64');
        await fs.writeFile(path.join(output,name),bytes);
        result.captures.push({name,sha256:hash(bytes)});
    }
    const readReview=id=>fs.readFile(path.join(project,'request-workbench',id+'.task.json'),'utf8');
    try {
        await ready();
        await checkPrior();
        const initialArtifacts=(await api('/api/state')).artifacts.length;
        const staged=new Map();
        for (const row of requests) {
            await page.locator('#goal').fill(row.text);
            await page.locator('#run').click();
            await page.getByRole('button',{name:'Verify both conversions',exact:true}).waitFor({state:'visible'});
            const id=await page.evaluate(()=>localStorage.getItem('fraynix-request-review'));
            await page.locator('#requests-pause-chain').check();
            await page.locator('#requests-confirm').click();
            await page.getByRole('button',{name:'Resume verified handoff',exact:true}).waitFor({state:'visible'});
            const review=await api('/api/requests/read',{id});
            assert.equal(review.choice.chain.input.value,row.input);
            assert.deepEqual(review.intermediate,row.intermediate);
            const record=await readReview(id);
            const task=JSON.parse(record).data;
            assert(task.frayFirstChecks.length===5&&!task.frayChecks&&!task.report);
            staged.set(row.name,{...row,id,record,review});
            result.pending.push({name:row.name,id,input:review.choice.chain.input,intermediate:row.intermediate,
                firstReceiptSha256:hash(JSON.stringify(task.frayFirstChecks))});
            await page.locator('#requests-close').click();
            await page.locator('#requests-dialog').waitFor({state:'hidden'});
        }
        const fresh=await api('/api/requests/chat',{request:'Convert 0 C to F, then F to R.'});
        assert(fresh.task&&!fresh.task.firstVerified);
        assert.equal((await api('/api/state')).artifacts.length,initialArtifacts);
        const child=app.process();
        const stopped=once(child,'exit');
        assert(Number.isSafeInteger(child.pid)&&child.pid>0&&child.exitCode===null);
        const firstPid=child.pid;
        await promisify(execFile)('taskkill.exe',['/PID',String(firstPid),'/T','/F'],{windowsHide:true});
        await stopped;
        ({app,page}=await launch(project));
        await ready();
        assert.notEqual(firstPid,app.process().pid);
        result.restart={forced:true,newDesktopProcess:true};
        await checkPrior();
        for (const row of staged.values()) assert.equal(await readReview(row.id),row.record);
        await page.evaluate(()=>localStorage.removeItem('fraynix-request-review'));
        const pending = [...staged.values()];
        const before=performance.now();
        await page.locator('#requests-open').click();
        await page.locator('#requests-recover-chain').click();
        await page.locator('#requests-pending').waitFor({state:'visible'});
        result.findThreeMs=Math.round(performance.now()-before);
        const ids=await page.locator('#requests-pending-chain option').evaluateAll(options=>options.map(o=>o.value).filter(Boolean));
        assert.deepEqual(ids,pending.map(row=>row.id).reverse());
        assert(await page.locator('#requests-load-chain').isDisabled());
        assert(!(await page.locator('#requests-confirm').isVisible()));
        await capture('three-pending.png');
        await page.locator('#requests-close').click();
        await page.locator('#requests-dialog').waitFor({state:'hidden'});

        for(const [target,other] of [['length','near-zero'],['near-zero','body-temperature']]) {
            const selected=staged.get(target), alternate=staged.get(other);
            const binding={id:selected.id,reviewSha256:selected.review.reviewSha256};
            const prep=await api('/api/requests/fray-prepare',binding);
            await assert.rejects(api('/api/requests/fray-chain-finish',{...binding,htmlSha256:prep.htmlSha256,
                handoff:alternate.intermediate,checks:[]}),/verified handoff requires/);
            assert.equal(await readReview(selected.id),selected.record);
            result.rejectedHandoffs.push({review:target,attempted:other,unit:alternate.intermediate.unit});
        }

        const order=['length','near-zero','body-temperature'];
        for(const name of order) {
            const row=staged.get(name);
            await page.locator('#requests-open').click();
            await page.locator('#requests-recover-chain').click();
            if (staged.size>1) {
                await page.locator('#requests-pending').waitFor({state:'visible'});
                await page.locator('#requests-pending-chain').selectOption(row.id);
                await page.locator('#requests-load-chain').click();
            }
            await page.getByRole('button',{name:'Resume verified handoff',exact:true}).waitFor({state:'visible'});
            assert.equal(await page.evaluate(()=>localStorage.getItem('fraynix-request-review')),row.id);
            await page.locator('#requests-confirm').click();
            await page.locator('#requests-dialog').waitFor({state:'hidden'});
            await page.waitForFunction(id=>Array.from(document.querySelectorAll('#messages .message')).some(
                el=>el.textContent.includes('Both Fray steps passed. The second used the verified '+id+'.')),
                row.intermediate.value+' '+row.intermediate.unit);
            const verified=await api('/api/requests/verify',{id:row.id});
            const artifact=await api('/api/artifact/read',{id:verified.artifact.id});
            const proof=JSON.parse(artifact.contents['verification.json']);
            assert(artifact.test.passed && artifact.test.checks.length===10);
            assert.deepEqual(proof.intermediate,row.intermediate);
            assert.equal(proof.result,row.result);
            assert.deepEqual(proof.firstChecks,JSON.parse(row.record).data.frayFirstChecks);
            const timed=(await state()).log.find(event=>event.kind==='request-recovery'&&event.data.reviewId===row.id);
            assert(timed?.data.rebuildMs>0&&timed.data.secondStepMs>0);
            result.completed.push({name,id:row.id,verified,result:proof.result,browserChecks:artifact.test.checks.length,
                rebuildMs:timed.data.rebuildMs,secondStepMs:timed.data.secondStepMs});
            staged.delete(name);
            for (const other of staged.values()) assert.equal(await readReview(other.id),other.record);
            await assert.rejects(api('/api/requests/recover-chain',{id:row.id}),/no longer has an interrupted handoff/);
            assert.equal((await api('/api/requests/pending-chains',{})).chains.length,staged.size);
            if (name==='length') {
                const checks=await app.evaluate((_,steps)=>global.workspaceTest.canvasChecks(steps),artifact.test.steps);
                assert(checks.every(check=>check.passed));
                await fs.writeFile(path.join(output,'browser-receipt.json'),JSON.stringify(artifact.test,null,2)+'\n');
                await capture('selected-length.png');
            }
        }
        assert.equal((await api('/api/state')).artifacts.length,initialArtifacts+3);
        await page.locator('#requests-open').click();
        await page.locator('#requests-recover-chain').click();
        await page.getByText('No interrupted conversion has a saved handoff.',{exact:true}).waitFor({state:'visible'});
        await capture('all-resolved.png');
        await checkPrior();
        Object.assign(result,{passed:true,priorEditions:prior.map(row=>row.edition),unapprovedExcluded:true,
            siblingReceiptsUnchanged:true});
        await fs.writeFile(path.join(output,'result.json'),JSON.stringify(result,null,2)+'\n');
        console.log(JSON.stringify({passed:true,pending:3,order,prior:result.priorEditions,rejections:2,
            rebuildMs:result.completed.map(row=>row.rebuildMs),secondStepMs:result.completed.map(row=>row.secondStepMs)}));
    } catch(error) {
        await fs.writeFile(path.join(output,'failure.txt'),error.stack+'\n'+await page.locator('body').innerText()).catch(()=>{});
        throw error;
    } finally {
        await app.close().catch(()=>{});
    }
}

main().catch(error=>{console.error(error);process.exitCode=1;});
