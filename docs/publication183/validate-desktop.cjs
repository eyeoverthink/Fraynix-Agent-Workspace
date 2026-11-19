'use strict';
const assert = require('node:assert/strict');
const {createHash} = require('node:crypto');
const fs = require('node:fs/promises');
const path = require('node:path');
const {pathToFileURL} = require('node:url');
const {launch} = require('../../workspace-desktop/test/calibration-integration.cjs');

const request = 'Convert 12.3456 inches to millimetres, then millimetres to centimetres.';
const hash = bytes => createHash('sha256').update(bytes).digest('hex');
const click = selector => ({selector,action:'click'});
const display = (selector,expectText) => ({selector,expectText});
const disabled = {selector:'#to-cm:disabled',expectCount:1};
const waiting = 'Waiting for conversion.';

async function main() {
    const output = path.resolve(process.argv[2] || 'build/edition183-live');
    const project = path.join(output,'project');
    await fs.mkdir(project,{recursive:true});
    const prior = [];
    if (process.argv[3]) {
        for (const name of ['workspace.fworld','workspace.fworld.previous','artifacts','receipts','request-workbench']) {
            await fs.cp(path.join(process.argv[3],name),path.join(project,name),{recursive:true});
        }
        for (const edition of [181,182]) {
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
    const fill = value => ({selector:'#inches',action:'fill',value,expectValue:value});
    try {
        await ready();
        evidence.priorEditions=await checkPrior();
        const count=(await api('/api/state')).artifacts.length;
        for (const [end,expected] of [['Celsius to Fahrenheit','\u00b0C'],['inches to millimetres','in']]) {
            const wrong='Convert 12.3456 inches to millimetres, then '+end+'.';
            await submit(wrong);
            const refusal='Cannot hand off mm to a step that expects '+expected+'.';
            await page.locator('#messages').getByText(refusal,{exact:true}).waitFor({state:'visible'});
            assert(!(await page.locator('#requests-dialog').isVisible()));
            assert.equal(await page.evaluate(()=>localStorage.getItem('fraynix-request-review')),null);
            evidence.refusals.push({request:wrong,reason:refusal});
        }
        assert.equal((await api('/api/state')).artifacts.length,count);
        await capture('rejected-handoffs.png');
        await submit(request);
        await page.locator('#requests-confirm').waitFor({state:'visible'});
        assert.match(await page.locator('#requests-source').textContent(),/12\.3456 in.*in to mm.*mm to cm/);
        const id=await page.evaluate(()=>localStorage.getItem('fraynix-request-review'));
        const review=await api('/api/requests/read',{id});
        assert.equal(review.choice.action,'build-length-chain');
        assert.equal(review.status,'review');
        await assert.rejects(api('/api/requests/verify',{id}));
        await capture('chat-review.png');
        await page.locator('#requests-confirm').click();
        await page.waitForFunction(()=>document.querySelector('#artifact-title').textContent==='Inches to centimetres in two steps',
            null,{timeout:45000});
        await page.locator('#messages').getByText('Both Fray steps passed.',{exact:false}).waitFor({state:'visible'});
        const verified=await api('/api/requests/verify',{id});
        const artifact=await api('/api/artifact/read',{id:verified.artifact.id});
        assert.equal(artifact.test.checks.length,10);
        assert(artifact.test.passed);
        const proof=JSON.parse(artifact.contents['verification.json']);
        assert.deepEqual(proof.intermediate,{unit:'mm',value:'313.57824'});
        assert.equal(proof.result,'31.357824 cm');
        assert.equal(hash(artifact.contents['index.html']),proof.htmlSha256);
        assert.equal(proof.firstChecks.length,5);
        assert.equal(proof.secondChecks.length,5);
        assert.equal(artifact.contents['fray-source.txt'],'inches * 25.4\nmillimetres / 10\n');
        await fs.writeFile(path.join(output,'browser-receipt.json'),JSON.stringify(artifact.test,null,2)+'\n');
        const reportView=await runBrowser(artifact.test.steps);
        assert.equal(reportView.at(-1).actual.text,'31.357824 cm');
        await capture('verified-chain.png');

        for (const [input,first,last] of [
            ['0','0 mm','0 cm'],['-3.25','-82.55 mm','-8.255 cm'],['0.0001','0.00254 mm','0.000254 cm'],
            ['-0.0001','-0.00254 mm','-0.000254 cm'],['999999.9999','25399999.99746 mm','2539999.999746 cm'],
            ['-1000000','-25400000 mm','-2540000 cm']
        ]) {
            const checks=await runBrowser([fill(input),disabled,display('#centimetres',waiting),
                click('#to-mm'),display('#millimetres',first),click('#to-cm'),display('#handoff',first),display('#centimetres',last)]);
            evidence.holdouts.push({input,millimetres:checks[4].actual.text,centimetres:checks.at(-1).actual.text});
        }
        const stale=await runBrowser([fill('7.125'),disabled,display('#millimetres',waiting),display('#centimetres',waiting),
            {selector:'#handoff[hidden]',expectCount:1},fill('1000000.0001'),click('#to-mm'),disabled,
            display('#millimetres','Enter a decimal within the stated range.')]);
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
            '(()=>{const input=document.querySelector("#inches");input.value="7.125";input.dispatchEvent(new Event("input"));' +
            'document.querySelector("#to-mm").click();document.querySelector("#to-cm").click();' +
            'return {protocol:location.protocol,result:document.querySelector("#centimetres").textContent}})()'));
        assert.deepEqual(localFile,{protocol:'file:',result:'18.0975 cm'});
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
            holdouts:evidence.holdouts.length,prior:evidence.priorEditions,exported:localFile.result}));
    } catch(error) {
        await fs.writeFile(path.join(output,'failure.txt'),error.stack+'\n'+await page.locator('body').innerText()).catch(()=>{});
        throw error;
    } finally {
        await app.close();
    }
}

main().catch(error=>{console.error(error);process.exitCode=1;});
