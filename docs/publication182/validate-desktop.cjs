'use strict';
const assert = require('node:assert/strict');
const {createHash} = require('node:crypto');
const fs = require('node:fs/promises');
const path = require('node:path');
const {pathToFileURL} = require('node:url');
const {launch} = require('../../workspace-desktop/test/calibration-integration.cjs');

const hash = bytes => createHash('sha256').update(bytes).digest('hex');
const request = 'Build me a browser tool to convert Celsius to Fahrenheit.';

async function main() {
    const output = path.resolve(process.argv[2] || 'build/edition182-live');
    await fs.mkdir(output);
    const project = path.join(output, 'project');
    await fs.mkdir(project);
    const prior = process.argv[3] ? JSON.parse(await fs.readFile('docs/readmes/181/verification.json','utf8')) : null;
    if (prior) {
        for (const name of ['workspace.fworld','workspace.fworld.previous','artifacts','receipts','request-workbench']) {
            await fs.cp(path.join(process.argv[3],name),path.join(project,name),{recursive:true});
        }
    }
    Object.assign(process.env, {FRAYNIX_WORKSPACE_NO_MODELS:'1', FRAYNIX_WORKSPACE_PERSISTENT:'0',
        FRAYNIX_WORKSPACE_JAVA_HEAP_MIB:'768', FRAYNIX_WORKSPACE_JAVA_PROCESSORS:'2'});
    let {app, page} = await launch(project);
    const api = (route, args) => app.evaluate((_, [route,args]) => global.workspaceTest.api(route,args),[route,args]);
    const call = (command,args) => page.evaluate(([command,args]) =>
        window.workspace.request({operation:'requestWorkbench',command,args}),[command,args]);
    const original = {request, captures:[]};
    const verifyPrior = async () => {
        if (!prior) return null;
        const proof = await api('/api/requests/verify',{id:prior.reviewId});
        assert.deepEqual(proof,prior.verified);
        return {reviewId:prior.reviewId,artifact:proof.artifact,matched:true};
    };
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
            {selector:'#celsius',action:'fill',value:input,expectValue:input},
            {selector:'#result',expectText:expected}
        ]);
        assert(checks.every(check => check.passed), JSON.stringify(checks));
        return checks[1].actual.text.trim();
    }
    try {
        await ready();
        const previousEdition = await verifyPrior();
        await page.locator('#goal').fill(request);
        await page.locator('#run').click();
        await page.locator('#requests-dialog').waitFor({state:'visible'});
        await page.locator('#requests-confirm').waitFor({state:'visible'});
        const id = await page.evaluate(() => localStorage.getItem('fraynix-request-review'));
        const review = await api('/api/requests/read',{id});
        assert.equal(review.status,'review');
        assert.equal(review.choice.action,'build-temperature-converter');
        assert.equal(review.choice.by,'java');
        await assert.rejects(api('/api/requests/verify',{id}));
        await capture('chat-review.png');
        await page.locator('#requests-confirm').click();
        await page.waitForFunction(() => document.querySelector('#artifact-title').textContent === 'Celsius to Fahrenheit',
            null,{timeout:45000});
        const verified = await api('/api/requests/verify',{id});
        assert(verified.passed);
        assert.equal(await canvasText(),'68 \u00b0F');
        await fillCanvas('37.25','99.05 \u00b0F');
        await capture('browser-converter.png');
        const holdouts = [
            ['12.125', '53.825 \u00b0F'], ['-17.7777', '0.00014 \u00b0F'],
            ['-11.125', '11.975 \u00b0F'], ['41.7', '107.06 \u00b0F'],
            ['-1000000', '-1799968 \u00b0F'], ['1000000', '1800032 \u00b0F'],
            ['1e3', 'Enter a decimal within the stated range.'],
            ['+', 'Enter a decimal within the stated range.']
        ];
        const observations = [];
        for (const [input, expected] of holdouts) {
            observations.push({input,expected,observed:await fillCanvas(input,expected)});
        }
        const artifact = await api('/api/artifact/read',{id:verified.artifact.id});
        const proof = JSON.parse(artifact.contents['verification.json']);
        assert.equal(proof.browserCases.length,12);
        assert(proof.browserCases.every(row => row.java===row.browser));
        assert.equal(hash(artifact.contents['index.html']),proof.htmlSha256);
        assert.equal(artifact.contents['fray-source.txt'],'celsius * 9 / 5 + 32\n');
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
            '(()=>{const input=document.querySelector("#celsius");input.value="12.125";input.dispatchEvent(new Event("input"));' +
            'return {protocol:location.protocol,result:document.querySelector("#result").textContent}})()'));
        assert.deepEqual(localFile,{protocol:'file:',result:'53.825 \u00b0F'});
        await exportedWindow.evaluate(win => win.destroy());
        await call('learn',{id});
        await app.close();
        ({app,page} = await launch(project));
        await ready();
        assert.deepEqual(await verifyPrior(),previousEdition);
        await page.locator('#requests-open').click();
        await page.locator('#requests-reopen').click();
        await page.locator('#requests-result').waitFor({state:'visible'});
        assert.match(await page.locator('#requests-status').textContent(),/matched the Fray reference/);
        await page.locator('#requests-result').click();
        await page.locator('#requests-dialog').waitFor({state:'hidden'});
        assert.equal(await canvasText(),'68 \u00b0F');
        const reopened = await api('/api/requests/verify',{id});
        assert.deepEqual(reopened,verified);
        await page.locator('#goal').fill(request);
        await page.locator('#run').click();
        await page.locator('#requests-dialog').waitFor({state:'visible'});
        assert.match(await page.locator('#requests-choice').textContent(),/remembered wording/);
        await page.locator('#requests-close').click();
        Object.assign(original,{passed:true,reviewId:id,verified,proof,originalBrowserSteps:artifact.test.checks.length,
            liveInput:{celsius:'37.25',fahrenheit:'99.05'},exportedFile:localFile,reopened:true,remembered:true,
            prior:previousEdition,holdouts:observations});
        await fs.writeFile(path.join(output,'result.json'),JSON.stringify(original,null,2)+'\n');
        console.log(JSON.stringify({passed:true,cases:proof.browserCases.length,browserSteps:artifact.test.checks.length,
            restarted:original.reopened,holdouts:holdouts.length,prior:previousEdition?.matched,exportedFile:localFile.result}));
    } catch (error) {
        await fs.writeFile(path.join(output,'failure.txt'),error.stack+'\n'+await page.locator('body').innerText()).catch(()=>{});
        throw error;
    } finally { await app.close(); }
}
main().catch(error=>{console.error(error);process.exitCode=1;});
