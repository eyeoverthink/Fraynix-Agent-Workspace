'use strict';

const assert = require('node:assert/strict');
const {createHash} = require('node:crypto');
const fs = require('node:fs/promises');
const os = require('node:os');
const path = require('node:path');
const {launch} = require('./calibration-integration.cjs');
const {NativeExports} = require('../exports.cjs');

const ranges = 'min_inclusive,max_exclusive,label\n0,10,temperature\n10,20,length';
const earlier = [
    'record ID,reading,probe_c,inches', 'rise <probe>,4,0,1', 'fall,4,10,1', 'steady,4,5,1',
    'length gain,12,0,1', 'length fall,12,0,3', 'gone,12,0,8', 'double,4,2,1', 'double,4,3,1',
];
const later = [
    'reading,inches,record ID,probe_c', '4,1,fall,5', '4,1,rise <probe>,10', '4,1,steady,5',
    '12,2,length gain,0', '12,2.5,length fall,0', '4,1,double,2', '12,2,fresh,0',
];
// Hand-derived changes and bar widths. Each unit's largest magnitude spans its own half-axis.
const expected = [
    ['°F','fall','-9 °F','loss',5000], ['°F','rise <probe>','+18 °F','gain',10000],
    ['°F','steady','0 °F','steady',0], ['mm','length fall','-12.7 mm','loss',5000],
    ['mm','length gain','+25.4 mm','gain',10000],
];
const sourcePin = 'fraynix-last-comparison-story';
const keyColumn = 'record ID';
const hash = bytes => createHash('sha256').update(bytes).digest('hex');
const options = {timeout:60_000};

async function main() {
    const output = path.resolve(process.argv[2] || 'build/change-story');
    await fs.mkdir(output,{recursive:true});
    const project = await fs.mkdtemp(path.join(os.tmpdir(),'Fraynix change story '));
    process.env.FRAYNIX_WORKSPACE_NO_MODELS = '1';
    process.env.FRAYNIX_WORKSPACE_PERSISTENT = '0';
    const args = process.env.FRAYNIX_WORKSPACE_CI_CHROMIUM === '1' && process.platform === 'linux' ? ['--no-sandbox'] : [];
    const exports = new NativeExports(project);
    let {app,page} = await launch(project,args);
    const checks = [];
    const errors = [];
    const api = (route,input) => app.evaluate((_,[route,input])=>global.workspaceTest.api(route,input),[route,input]);
    const call = (command,args) => page.evaluate(([command,args])=>
        window.workspace.request({operation:'requestWorkbench',command,args}),[command,args]);
    const canvas = code => app.evaluate((_,code)=>global.workspaceTest.canvasRead(code),code);

    async function send(message) {
        await page.locator('#goal').fill(message);
        await page.locator('#run').click();
    }

    async function reply(text) {
        await page.getByText(text,{exact:false}).last().waitFor({state:'visible',...options});
    }

    async function draft(command) {
        const old = await page.evaluate(()=>localStorage.getItem('fraynix-request-review'));
        await send(command);
        await page.locator('#requests-file').waitFor({state:'visible',...options});
        await page.waitForFunction(previous=>{
            const current = localStorage.getItem('fraynix-request-review');
            return current && current !== previous;
        },old,options);
        return page.evaluate(()=>localStorage.getItem('fraynix-request-review'));
    }

    async function attach(selector,report,name,text) {
        await page.locator(selector).setInputFiles({name,mimeType:'text/csv',buffer:Buffer.from(text)});
        await page.waitForFunction(([selector,digest])=>document.querySelector(selector).textContent.includes(digest),
            [report,hash(text)],options);
    }

    async function confirm(id,rows) {
        await attach('#requests-file','#requests-source','readings.csv',rows.join('\n'));
        await page.locator('#requests-confirm').click();
        await page.waitForFunction(()=>document.querySelector('#requests-status').textContent.startsWith('Built an offline tool.'),{},options);
        assert((await api('/api/requests/verify',{id})).passed);
    }

    async function teach(command,name,rows) {
        const id = await draft(command);
        await confirm(id,rows);
        await send('Teach data-to-tool lesson '+name);
        await reply('Saved lesson '+name+'.');
    }

    async function route(rows) {
        const id = await draft('Build a data-to-tool classifier using column reading.');
        await attach('#requests-rules-file','#requests-rules-source','ranges.csv',ranges);
        await confirm(id,rows);
        const draftPlan = await call('routing-start',{sourceId:id});
        const mapping = Buffer.from('label,lesson\ntemperature,temperature-story\nlength,length-story').toString('base64');
        const review = await call('routing-attach',{id:draftPlan.id,base64:mapping});
        const done = await call('run-routing',{id:draftPlan.id,decisionSha256:review.decisionSha256});
        assert.equal(done.status,'verified');
        return call('export-routing',{id:done.id});
    }

    async function inspectFile(file,artifact) {
        const target = path.join(project,'offline-download.csv');
        const opened = app.waitForEvent('window');
        const windowId = await app.evaluate(({BrowserWindow},[file,target])=>{
            const window = new BrowserWindow({width:530,height:920,webPreferences:{
                partition:'change-story-offline',sandbox:true,contextIsolation:true,nodeIntegration:false}});
            const session = window.webContents.session;
            session.webRequest.onBeforeRequest({urls:['http://*/*','https://*/*']},
                (_,callback)=>callback({cancel:true}));
            session.on('will-download',(_,item)=>{
                item.setSavePath(target);
                item.once('done',(_,state)=>{window.storyDownloadState=state;});
            });
            window.loadFile(file);
            return window.id;
        },[file,target]);
        const browser = await opened;
        try {
            await browser.locator('.change-bar').first().waitFor(options);
            assert(browser.url().startsWith('file:'));
            assert.equal(await browser.locator('#visible-count').innerText(),'8 groups');
            assert(await browser.evaluate(()=>document.documentElement.scrollWidth<=innerWidth));
            async function marks() {
                return browser.locator('.change-bar').evaluateAll(buttons=>buttons.map(button=>{
                    const track = button.querySelector('.change-track').getBoundingClientRect();
                    const mark = button.querySelector('.change-mark').getBoundingClientRect();
                    const centre = track.left + track.width/2;
                    const {direction,width} = button.dataset;
                    const expectedWidth = direction==='steady' ? 6 : track.width*Number(width)/20000;
                    const origin = direction==='loss' ? mark.right : direction==='steady' ? mark.left+mark.width/2 : mark.left;
                    if (Math.abs(mark.width-expectedWidth)>.6 || Math.abs(origin-centre)>.6)
                        throw Error('The painted bar does not match its direction or scale');
                    return [button.closest('.change-lane').dataset.unit,JSON.parse(button.firstElementChild.textContent),
                        button.querySelector('.change-delta').textContent,direction,Number(width)];
                }));
            }
            const actual = await marks();
            const sort = rows=>rows.sort((a,b)=>a[1].localeCompare(b[1]));
            assert.deepEqual(sort(actual),sort([...expected]));
            assert.deepEqual(await browser.locator('.change-axis').allInnerTexts(),['-18 °F\n0\n+18 °F','-25.4 mm\n0\n+25.4 mm']);
            assert.equal(await browser.locator('#change-map probe').count(),0);
            const loss = browser.getByRole('button',{name:/"fall" -9 °F/});
            await loss.focus();
            await browser.keyboard.press('Enter');
            assert.equal(await loss.getAttribute('aria-pressed'),'true');
            assert.equal(await browser.locator('#change-selection-readings').innerText(),'Earlier #3: 50 | Later #2: 41');
            await browser.screenshot({path:path.join(output,'offline-story.png'),fullPage:true});
            await browser.locator('#unit-mm').click();
            assert.deepEqual(sort(await marks()),sort(expected.filter(row=>row[0]==='mm')));
            assert.equal(await browser.locator('.change-axis').innerText(),'-25.4 mm\n0\n+25.4 mm');
            await browser.locator('#status-missing').click();
            assert.equal(await browser.locator('#visible-count').innerText(),'1 group');
            assert.equal(await browser.locator('#change-empty').innerText(),'No matched changes in this view.');
            assert.equal(await browser.locator('.change-bar').count(),0);
            await browser.locator('#reset-filters').click();
            assert.equal(await browser.locator('button.change-bar[aria-pressed=true]').count(),0);
            assert.equal(await browser.locator('#change-selection-title').innerText(),'Select a bar to inspect both source readings.');
            await browser.getByRole('link',{name:'Comparison CSV'}).evaluate(link=>link.click());
            const download = () => app.evaluate(({BrowserWindow},id)=>BrowserWindow.fromId(id)?.storyDownloadState,windowId);
            let state;
            for (let attempt=0;attempt<80;attempt++) {
                state = await download();
                if (state) break;
                await new Promise(resolve=>setTimeout(resolve,100));
            }
            assert.equal(state,'completed');
            assert.equal(await fs.readFile(target,'utf8'),artifact.contents['matched-comparison.csv']);
        } finally {
            await app.evaluate(({BrowserWindow},id)=>BrowserWindow.fromId(id)?.destroy(),windowId);
        }
    }

    try {
        page.on('pageerror',error=>errors.push(error.message));
        assert.equal((await api('/api/state')).models.length,0);
        await teach('Build a data-to-tool converter from Celsius to Fahrenheit using column probe_c.',
            'temperature-story',['probe_c','0','10']);
        await teach('Build a data-to-tool converter from inches to millimetres.',
            'length-story',['inches','1','10']);
        const a = await route(earlier);
        const b = await route(later);
        await send('Compare the last two verified routed CSVs by key "record ID".');
        await reply('Matched comparison verified against Java.');
        const pair = {earlier:a.id,later:b.id};
        const exportComparison = () => call('routing-dashboard',{compare:true,verifyOnly:true,pair,keyColumn,exportCsv:true});
        const original = await exportComparison();
        await send('Summarize the last verified matched comparison CSV.');
        await reply('Comparison CSV totals verified against Java.');
        await send('Build an explorer from the verified comparison totals.');
        await reply('Comparison explorer verified against Java.');
        const legacy = await call('routing-summary',{verifyOnly:true,explorer:true,exportId:original.id});
        await send('Build a change story from the verified comparison totals.');
        await reply('Change story verified against Java. 24 browser checks passed.');
        assert.equal(await page.evaluate(key=>localStorage.getItem(key),sourcePin),original.id);
        const proof = await call('routing-summary',{story:true,exportId:original.id});
        const artifact = await api('/api/artifact/read',{id:proof.artifact.id});
        const evidence = JSON.parse(artifact.contents['verification.json']);
        assert.equal(evidence.presentation,'change-story-v1');
        assert.equal(evidence.import.sha256,original.sha256);
        assert.equal(proof.browserChecks,24);
        assert.equal(artifact.test.checks.length,24);
        assert.notEqual(legacy.artifact.id,proof.artifact.id);
        assert.equal(await canvas('document.querySelector("#visible-count").innerText'),'8 groups');
        const shot = await app.evaluate(()=>global.workspaceTest.capture());
        await fs.writeFile(path.join(output,'desktop-story.png'),Buffer.from(shot,'base64'));
        checks.push('Chat builds a separate, Java-verified artifact after the comparison and summary');

        const newer = await exportComparison();
        assert.notEqual(newer.id,original.id);
        await app.close();
        ({app,page} = await launch(project,args));
        page.on('pageerror',error=>errors.push(error.message));
        await send('Recheck the verified change story.');
        await reply('Change story verified against Java. 24 fresh browser checks passed.');
        assert.equal(await canvas('document.querySelector("#source-id").innerText'),'Export '+original.id);
        const readback = await call('routing-summary',{verifyOnly:true,story:true,exportId:original.id});
        assert.deepEqual(readback.artifact,proof.artifact);
        assert.deepEqual((await api('/api/artifact/read',{id:proof.artifact.id})).test,artifact.test);
        const legacyReadback = await call('routing-summary',{verifyOnly:true,explorer:true,exportId:original.id});
        assert.deepEqual(legacyReadback.artifact,legacy.artifact);
        checks.push('Restart rechecks the pinned original beside an independent explorer and a newer export');

        await send('Export the verified change story.');
        await reply('Saved verified change story to');
        const manifest = path.join(project,'exports','manifest.jsonl');
        const entries = (await fs.readFile(manifest,'utf8')).trim().split('\n').map(line=>JSON.parse(line));
        const saved = entries.findLast(entry=>entry.kind==='comparison-change-story');
        assert.equal(saved.sha256,proof.htmlSha256);
        assert.equal(saved.source.comparisonExportId,original.id);
        assert.equal(await fs.readFile(saved.path,'utf8'),artifact.contents['index.html']);
        const exported = path.join(output,'change-story.html');
        await fs.copyFile(saved.path,exported);
        for (const name of ['matched-comparison.csv','import-receipt.json','summary-proof.json','verification.json'])
            await fs.writeFile(path.join(output,name),artifact.contents[name]);
        const standalone = path.join(project,'standalone');
        await fs.mkdir(standalone);
        const singleFile = path.join(standalone,'change-story.html');
        await fs.copyFile(exported,singleFile);
        await inspectFile(singleFile,artifact);
        checks.push('Standalone HTML works offline with the correct painted bars, filter changes, keyboard selection and exact CSV download');

        const source = path.join(project,'exports',original.id+'.csv');
        await fs.appendFile(source,'changed');
        await assert.rejects(call('routing-summary',{verifyOnly:true,story:true,exportId:original.id,exportStory:true}),/disagree|changed/i);
        assert.equal(await fs.readFile(manifest,'utf8'),entries.map(entry=>JSON.stringify(entry)).join('\n')+'\n');
        checks.push('Changed comparison bytes are refused before sharing the story');
        assert.deepEqual(errors,[]);
        const result = {passed:true,platform:process.platform,architecture:process.arch,proof,changes:expected,checks};
        await fs.writeFile(path.join(output,'result.json'),JSON.stringify(result,null,2)+'\n');
        console.log(JSON.stringify({passed:true,platform:result.platform,architecture:result.architecture,checks}));
    } finally {
        await app.close().catch(()=>{});
        await fs.copyFile(path.join(project,'test-desktop-process.log'),path.join(output,'desktop.log')).catch(()=>{});
    }
}

main().catch(error=>{console.error(error);process.exitCode=1;});
