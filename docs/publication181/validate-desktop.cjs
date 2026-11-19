'use strict';
const assert = require('node:assert/strict');
const {createHash} = require('node:crypto');
const fs = require('node:fs/promises');
const path = require('node:path');
const {pathToFileURL} = require('node:url');
const {launch} = require('../../workspace-desktop/test/calibration-integration.cjs');

const hash = bytes => createHash('sha256').update(bytes).digest('hex');
const request = 'Make me a little converter for inches to millimetres and give me a browser version.';

async function main() {
    const output = path.resolve(process.argv[2] || 'build/edition181-live');
    await fs.mkdir(output);
    const project = path.join(output, 'project');
    await fs.mkdir(project);
    Object.assign(process.env, {FRAYNIX_WORKSPACE_NO_MODELS:'1', FRAYNIX_WORKSPACE_PERSISTENT:'0',
        FRAYNIX_WORKSPACE_JAVA_HEAP_MIB:'768', FRAYNIX_WORKSPACE_JAVA_PROCESSORS:'2'});
    let {app, page} = await launch(project);
    const api = (route, args) => app.evaluate((_, [route,args]) => global.workspaceTest.api(route,args),[route,args]);
    const call = (command,args) => page.evaluate(([command,args]) =>
        window.workspace.request({operation:'requestWorkbench',command,args}),[command,args]);
    const original = {request, captures:[]};
    const capture = async name => {
        await page.evaluate(() => new Promise(resolve => requestAnimationFrame(() => requestAnimationFrame(resolve))));
        const bytes = Buffer.from(await app.evaluate(() => global.workspaceTest.capture()), 'base64');
        await fs.writeFile(path.join(output,name),bytes);
        original.captures.push({name,sha256:hash(bytes)});
    };
    const canvasText = () => app.evaluate(() => global.workspaceTest.canvasRead('document.querySelector("#result").textContent'));
    const ready = async () => {
        await app.evaluate(({BrowserWindow}) => BrowserWindow.getAllWindows().find(w => w.isVisible()).setSize(1480,1030));
        await page.locator('#engine-state').waitFor({state:'visible'});
        assert.equal((await api('/api/state')).models.length,0);
    };
    async function fillCanvas(input, expected) {
        const checks = await app.evaluate((_, steps) => global.workspaceTest.canvasChecks(steps),[
            {selector:'#inches',action:'fill',value:input,expectValue:input},
            {selector:'#result',expectText:expected}
        ]);
        assert(checks.every(check => check.passed), JSON.stringify(checks));
    }
    try {
        await ready();
        await page.locator('#goal').fill(request);
        await page.locator('#run').click();
        await page.locator('#requests-dialog').waitFor({state:'visible'});
        await page.locator('#requests-confirm').waitFor({state:'visible'});
        const id = await page.evaluate(() => localStorage.getItem('fraynix-request-review'));
        const review = await api('/api/requests/read',{id});
        assert.equal(review.status,'review');
        assert.equal(review.choice.action,'build-inch-converter');
        assert.equal(review.choice.by,'java');
        await assert.rejects(api('/api/requests/verify',{id}));
        await capture('chat-review.png');
        await page.locator('#requests-confirm').click();
        await page.waitForFunction(() => document.querySelector('#artifact-title').textContent === 'Inches to millimetres',
            null,{timeout:45000});
        const verified = await api('/api/requests/verify',{id});
        assert(verified.passed);
        assert.equal(await canvasText(),'25.4 mm');
        await fillCanvas('12','304.8 mm');
        await capture('browser-converter.png');
        const artifact = await api('/api/artifact/read',{id:verified.artifact.id});
        const proof = JSON.parse(artifact.contents['verification.json']);
        assert.equal(proof.browserCases.length,12);
        assert(proof.browserCases.every(row => row.java===row.browser));
        assert.equal(hash(artifact.contents['index.html']),proof.htmlSha256);
        assert.equal(artifact.contents['fray-source.txt'],'inches * 25.4\n');
        assert.equal(artifact.test.checks.length,23);
        assert(artifact.test.passed);
        await fs.writeFile(path.join(output,'fray-browser-receipt.json'),JSON.stringify(artifact.test,null,2)+'\n');
        const exported = await call('export',{id});
        assert.equal(exported.sha256,proof.htmlSha256);
        assert.equal(hash(await fs.readFile(exported.path)),proof.htmlSha256);

        const exportedWindow = await app.evaluateHandle(async ({BrowserWindow}, url) => {
            const check = new BrowserWindow({show:false,webPreferences:{sandbox:true,contextIsolation:true,nodeIntegration:false}});
            await check.loadURL(url);
            return check;
        },pathToFileURL(exported.path).href);
        const localFile = await exportedWindow.evaluate(win => win.webContents.executeJavaScript(
            '(()=>{const input=document.querySelector("#inches");input.value="0.375";input.dispatchEvent(new Event("input"));' +
            'return {protocol:location.protocol,result:document.querySelector("#result").textContent}})()'));
        assert.deepEqual(localFile,{protocol:'file:',result:'9.525 mm'});
        await exportedWindow.evaluate(win => win.destroy());
        await call('learn',{id});
        await app.close();
        ({app,page} = await launch(project));
        await ready();
        await page.locator('#requests-open').click();
        await page.locator('#requests-reopen').click();
        await page.locator('#requests-result').waitFor({state:'visible'});
        assert.match(await page.locator('#requests-status').textContent(),/matched the Fray reference/);
        await page.locator('#requests-result').click();
        await page.locator('#requests-dialog').waitFor({state:'hidden'});
        assert.equal(await canvasText(),'25.4 mm');
        const reopened = await api('/api/requests/verify',{id});
        assert.deepEqual(reopened,verified);
        await page.locator('#goal').fill(request);
        await page.locator('#run').click();
        await page.locator('#requests-dialog').waitFor({state:'visible'});
        assert.match(await page.locator('#requests-choice').textContent(),/remembered wording/);
        await page.locator('#requests-close').click();
        Object.assign(original,{passed:true,reviewId:id,verified,proof,originalBrowserSteps:artifact.test.checks.length,
            liveInput:{inches:'12',millimetres:'304.8'},exportedFile:localFile,reopened:true,remembered:true});
        await fs.writeFile(path.join(output,'result.json'),JSON.stringify(original,null,2)+'\n');
        console.log(JSON.stringify({passed:true,cases:proof.browserCases.length,browserSteps:artifact.test.checks.length,
            restarted:original.reopened,exportedFile:localFile.result}));
    } catch (error) {
        await fs.writeFile(path.join(output,'failure.txt'),error.stack+'\n'+await page.locator('body').innerText()).catch(()=>{});
        throw error;
    } finally { await app.close(); }
}
main().catch(error=>{console.error(error);process.exitCode=1;});
