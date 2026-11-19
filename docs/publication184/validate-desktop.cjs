'use strict';
const {execFile} = require('node:child_process');
const {once} = require('node:events');
const {promisify} = require('node:util');
const assert = require('node:assert/strict');
const {createHash} = require('node:crypto');
const fs = require('node:fs/promises');
const path = require('node:path');
const {pathToFileURL} = require('node:url');
const {launch} = require('../../workspace-desktop/test/calibration-integration.cjs');

const request = 'Convert -17.7778\u2103 to \u2109, then \u00b0F to degrees Rankine.';
const hash = bytes => createHash('sha256').update(bytes).digest('hex');
const click = selector => ({selector,action:'click'});
const display = (selector,expectText) => ({selector,expectText});
const disabled = {selector:'#to-r:disabled',expectCount:1};
const waiting = 'Waiting for conversion.';

async function main() {
    const output = path.resolve(process.argv[2] || 'build/edition184-live');
    const project = path.join(output,'project');
    await fs.mkdir(project,{recursive:true});
    const prior = [];
    if (process.argv[3]) {
        for (const name of ['workspace.fworld','workspace.fworld.previous','artifacts','receipts','request-workbench']) {
            await fs.cp(path.join(process.argv[3],name),path.join(project,name),{recursive:true});
        }
        for (const edition of [181,182,183]) {
            const record=JSON.parse(await fs.readFile('docs/readmes/'+edition+'/verification.json','utf8'));
            prior.push({edition,id:record.reviewId,proof:record.verified});
        }
    }
    Object.assign(process.env,{FRAYNIX_WORKSPACE_NO_MODELS:'1',FRAYNIX_WORKSPACE_PERSISTENT:'0',
        FRAYNIX_WORKSPACE_JAVA_HEAP_MIB:'768',FRAYNIX_WORKSPACE_JAVA_PROCESSORS:'2'});
    let {app,page}=await launch(project);
    const evidence={request,captures:[],refusals:[],holdouts:[]};
    const api = (route,args) => app.evaluate((_,[route,args])=>global.workspaceTest.api(route,args),[route,args]);
    const call = (command,args) => page.evaluate(([command,args])=>
        window.workspace.request({operation:'requestWorkbench',command,args}),[command,args]);
    async function ready() {
        await app.evaluate(({BrowserWindow})=>BrowserWindow.getAllWindows().find(w=>w.isVisible()).setSize(1480,1030));
        assert.equal((await api('/api/state')).models.length,0);
    }
    async function checkPrior() {
        for (const row of prior) assert.deepEqual(await api('/api/requests/verify',{id:row.id}),row.proof);
        return prior.map(row=>row.edition);
    }
    async function submit(text) {
        await page.locator('#goal').fill(text);
        await page.locator('#run').click();
    }
    async function capture(name) {
        await page.evaluate(()=>new Promise(resolve=>requestAnimationFrame(()=>requestAnimationFrame(resolve))));
        const bytes=Buffer.from(await app.evaluate(()=>global.workspaceTest.capture()),'base64');
        await fs.writeFile(path.join(output,name),bytes);
        evidence.captures.push({name,sha256:hash(bytes)});
    }
    async function runBrowser(steps) {
        const checks=await app.evaluate((_,steps)=>global.workspaceTest.canvasChecks(steps),steps);
        assert(checks.every(check=>check.passed),JSON.stringify(checks));
        return checks;
    }
    const fill = value => ({selector:'#celsius',action:'fill',value,expectValue:value});
    try {
        await ready();
        evidence.priorEditions=await checkPrior();
        const count=(await api('/api/state')).artifacts.length;
        for (const [end,expected] of [['millimetres to centimetres','mm'],['kelvins to Rankine','K']]) {
            const wrong='Convert -17.7778 C to F, then '+end+'.';
            await submit(wrong);
            const refusal='Cannot hand off \u00b0F to a step that expects '+expected+'.';
            await page.locator('#messages').getByText(refusal,{exact:true}).waitFor({state:'visible'});
            assert(!(await page.locator('#requests-dialog').isVisible()));
            assert.equal(await page.evaluate(()=>localStorage.getItem('fraynix-request-review')),null);
            evidence.refusals.push({request:wrong,reason:refusal});
        }
        assert.equal((await api('/api/state')).artifacts.length,count);
        await capture('alias-refusals.png');
        const spellings=[
            'Convert -17.7778 Celsius to Fahrenheit, then Fahrenheit to Rankine.',
            'Please convert -17.7778 centigrade to degrees Fahrenheit, then deg F to deg R.'
        ];
        const aliases=[];
        for (const plain of spellings) {
            const suggestion=await api('/api/requests/chat',{request:plain});
            assert(suggestion.matched && suggestion.task);
            aliases.push({request:plain,chain:suggestion.task.choice.chain});
        }
        evidence.aliases=aliases;
        await submit(request);
        await page.locator('#requests-confirm').waitFor({state:'visible'});
        assert.match(await page.locator('#requests-source').textContent(),/-17\.7778 \u00b0C.*\u00b0C to \u00b0F.*\u00b0F to \u00b0R/);
        const id=await page.evaluate(()=>localStorage.getItem('fraynix-request-review'));
        const review=await api('/api/requests/read',{id});
        assert.equal(review.choice.action,'build-length-chain');
        assert.equal(review.status,'review');
        await assert.rejects(api('/api/requests/verify',{id}));
        for (const row of aliases) assert.deepEqual(row.chain,review.choice.chain);
        await page.locator('#requests-pause-chain').check();
        await page.locator('#requests-confirm').click();
        await page.getByRole('button',{name:'Resume verified handoff',exact:true}).waitFor({state:'visible'});
        const paused=await api('/api/requests/read',{id});
        assert.equal(paused.status,'review');
        assert.equal(paused.reviewSha256,review.reviewSha256);
        assert(paused.firstVerified);
        assert.deepEqual(paused.intermediate,{value:'-0.00004',unit:'\u00b0F'});
        const stateFile=path.join(project,'request-workbench',id+'.task.json');
        const persisted=JSON.parse(await fs.readFile(stateFile,'utf8'));
        assert.equal(persisted.data.frayFirstChecks.length,5);
        assert(!persisted.data.frayChecks && !persisted.data.report);
        await assert.rejects(api('/api/requests/verify',{id}));
        assert.equal((await api('/api/state')).artifacts.length,count);

        const child=app.process();
        const stopped=once(child,'exit');
        assert(Number.isSafeInteger(child.pid) && child.pid>0 && child.exitCode===null);
        const firstPid=child.pid;
        await promisify(execFile)('taskkill.exe',['/PID',String(firstPid),'/T','/F'],{windowsHide:true});
        const [code,signal]=await stopped;
        ({app,page}=await launch(project));
        await ready();
        const secondPid=app.process().pid;
        assert.notEqual(secondPid,firstPid);
        assert.deepEqual(await checkPrior(),evidence.priorEditions);
        assert.deepEqual(JSON.parse(await fs.readFile(stateFile,'utf8')),persisted);
        await assert.rejects(api('/api/requests/verify',{id}));
        await page.evaluate(()=>localStorage.removeItem('fraynix-request-review'));
        await page.locator('#requests-open').click();
        await page.locator('#requests-recover-chain').click();
        await page.getByRole('button',{name:'Resume verified handoff',exact:true}).waitFor({state:'visible'});
        assert.equal(await page.evaluate(()=>localStorage.getItem('fraynix-request-review')),id);
        assert.match(await page.locator('#requests-status').textContent(),/Verified -0\.00004 \u00b0F/);
        await page.locator('#requests-pause-label').waitFor({state:'hidden'});
        await capture('saved-handoff.png');
        await page.locator('#requests-confirm').click();
        evidence.restart={forced:true,firstPid,secondPid,exitCode:code,signal,
            sameReview:true,firstReceiptUnchanged:true,secondPendingAtRestart:true};
        await page.waitForFunction(()=>document.querySelector('#artifact-title').textContent==='Celsius to Rankine in two steps',
            null,{timeout:45000});
        await page.locator('#messages').getByText('Both Fray steps passed.',{exact:false}).waitFor({state:'visible'});
        const verified=await api('/api/requests/verify',{id});
        const artifact=await api('/api/artifact/read',{id:verified.artifact.id});
        assert.equal(artifact.test.checks.length,10);
        assert(artifact.test.passed);
        const proof=JSON.parse(artifact.contents['verification.json']);
        assert.deepEqual(proof.intermediate,{unit:'\u00b0F',value:'-0.00004'});
        assert.equal(proof.result,'459.66996 \u00b0R');
        assert.equal(hash(artifact.contents['index.html']),proof.htmlSha256);
        assert.deepEqual(proof.firstChecks,persisted.data.frayFirstChecks);
        assert.equal(proof.secondChecks.length,5);
        assert.equal(artifact.contents['fray-source.txt'],'celsius * 9 / 5 + 32\nfahrenheit + 459.67\n');
        await fs.writeFile(path.join(output,'browser-receipt.json'),JSON.stringify(artifact.test,null,2)+'\n');
        const reportView=await runBrowser(artifact.test.steps);
        assert.equal(reportView.at(-1).actual.text,'459.66996 \u00b0R');
        await capture('verified-temperature-chain.png');

        for (const [input,first,last] of [
            ['-273.15','-459.67 \u00b0F','0 \u00b0R'],['-40','-40 \u00b0F','419.67 \u00b0R'],
            ['0','32 \u00b0F','491.67 \u00b0R'],['37.25','99.05 \u00b0F','558.72 \u00b0R'],
            ['100','212 \u00b0F','671.67 \u00b0R'],['0.0001','32.00018 \u00b0F','491.67018 \u00b0R']
        ]) {
            const checks=await runBrowser([fill(input),disabled,display('#rankine',waiting),
                click('#to-f'),display('#fahrenheit',first),click('#to-r'),display('#handoff',first),display('#rankine',last)]);
            evidence.holdouts.push({input,fahrenheit:checks[4].actual.text,rankine:checks.at(-1).actual.text});
        }
        const stale=await runBrowser([fill('37.25'),disabled,display('#fahrenheit',waiting),display('#rankine',waiting),
            {selector:'#handoff[hidden]',expectCount:1},fill('-273.1501'),click('#to-f'),disabled,
            display('#fahrenheit','Enter a decimal within the stated range.')]);
        evidence.staleInputRefused=stale.every(check=>check.passed);

        const exported=await call('export',{id});
        assert.equal(exported.sha256,proof.htmlSha256);
        assert.equal(hash(await fs.readFile(exported.path)),proof.htmlSha256);
        const offline=await app.evaluateHandle(async ({BrowserWindow},url)=>{
            const test=new BrowserWindow({show:false,webPreferences:{sandbox:true,contextIsolation:true,nodeIntegration:false}});
            await test.loadURL(url);
            return test;
        },pathToFileURL(exported.path).href);
        const localFile=await offline.evaluate(win=>win.webContents.executeJavaScript(
            '(()=>{const input=document.querySelector("#celsius");input.value="37.25";input.dispatchEvent(new Event("input"));' +
            'document.querySelector("#to-f").click();document.querySelector("#to-r").click();' +
            'return {protocol:location.protocol,result:document.querySelector("#rankine").textContent}})()'));
        assert.deepEqual(localFile,{protocol:'file:',result:'558.72 \u00b0R'});
        await offline.evaluate(win=>win.destroy());
        await call('learn',{id});
        await app.close();
        ({app,page}=await launch(project));
        await ready();
        assert.deepEqual(await checkPrior(),evidence.priorEditions);
        await page.locator('#requests-open').click();
        await page.locator('#requests-reopen').click();
        await page.locator('#requests-result').waitFor({state:'visible'});
        await page.locator('#requests-result').click();
        await page.locator('#requests-dialog').waitFor({state:'hidden'});
        assert.deepEqual(await api('/api/requests/verify',{id}),verified);
        await submit(request);
        await page.locator('#requests-dialog').waitFor({state:'visible'});
        assert.match(await page.locator('#requests-choice').textContent(),/remembered wording/);
        await page.locator('#requests-close').click();
        Object.assign(evidence,{passed:true,proof,verified,reviewId:id,reopened:true,remembered:true,exportedFile:localFile});
        await fs.writeFile(path.join(output,'result.json'),JSON.stringify(evidence,null,2)+'\n');
        console.log(JSON.stringify({passed:true,stages:[5,5],wrongUnits:evidence.refusals.length,
            holdouts:evidence.holdouts.length,prior:evidence.priorEditions,forcedRestart:evidence.restart.forced,exported:localFile.result}));
    } catch(error) {
        await fs.writeFile(path.join(output,'failure.txt'),error.stack+'\n'+await page.locator('body').innerText()).catch(()=>{});
        throw error;
    } finally {
        await app.close();
    }
}

main().catch(error=>{console.error(error);process.exitCode=1;});
