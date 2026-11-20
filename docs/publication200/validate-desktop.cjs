
'use strict';
const assert = require('node:assert/strict');
const {createHash} = require('node:crypto');
const fs = require('node:fs/promises');
const path = require('node:path');
const {pathToFileURL} = require('node:url');
const {chromium} = require('../../workspace-desktop/node_modules/playwright');
const {launch} = require('../../workspace-desktop/test/calibration-integration.cjs');

const hash = bytes => createHash('sha256').update(bytes).digest('hex');
const keyColumn = 'record ID';
const earlierSource = [
    'record ID,reading,probe_c,inches',
    'α-1,4,10,0', 'rail,12,0,3', 'gone,4,-40,0', 'tank,12,0,1.5', 'tank,12,0,1.75',
    'switch,4,0,0', '"quote ""A"", β",4,-40,0', 'rail,4,5,0', 'late-dup,4,3,0',
].join('\n');
const laterSource = [
    'reading,inches,record ID,probe_c',
    '12,2.5,rail,0', '12,0.25,fresh,0', '4,0,α-1,11.25', '12,2,tank,0',
    '12,8,switch,0', '4,0,"quote ""A"", β",-40', '4,0,late-dup,4', '4,0,rail,7.75', '4,0,late-dup,5',
].join('\n');
const expected = [
    ['α-1','°F','Matched',[[2,'50']],[[4,'52.25']],'2.25'],
    ['rail','mm','Matched',[[3,'76.2']],[[2,'63.5']],'-12.7'],
    ['gone','°F','Missing',[[4,'-40']],[],null],
    ['tank','mm','Duplicate',[[5,'38.1'],[6,'44.45']],[[5,'50.8']],null],
    ['switch','°F','Missing',[[7,'32']],[],null],
    ['quote "A", β','°F','Matched',[[8,'-40']],[[7,'-40']],'0'],
    ['rail','°F','Matched',[[9,'41']],[[9,'45.95']],'4.95'],
    ['late-dup','°F','Duplicate',[[10,'37.4']],[[8,'39.2'],[10,'41']],null],
    ['fresh','mm','Added',[],[[3,'6.35']],null],
    ['switch','mm','Added',[],[[6,'203.2']],null],
];
const summary = 'Matched 4 · Added 2 · Missing 2 · Duplicate 2';
const build = 'Compare the last two verified routed CSVs by key "record ID".';
const recheck = 'Recheck the matched routed CSV comparison.';
const ranges = 'min_inclusive,max_exclusive,label\n0,10,temperature\n10,20,length';
const mapping = 'label,lesson\ntemperature,temperature-export\nlength,length-export';

async function main() {
    const output = path.resolve(process.argv[2] || 'build/edition200-live');
    const prior = path.resolve(process.argv[3] || 'build/edition199-first/project');
    const project = path.join(output,'project');
    await fs.mkdir(output);
    await fs.mkdir(project);
    for (const name of ['workspace.fworld','workspace.fworld.previous','artifacts','receipts','request-workbench','exports'])
        await fs.cp(path.join(prior,name),path.join(project,name),{recursive:true});
    Object.assign(process.env,{FRAYNIX_WORKSPACE_NO_MODELS:'1',FRAYNIX_WORKSPACE_PERSISTENT:'0',
        FRAYNIX_WORKSPACE_JAVA_HEAP_MIB:'768',FRAYNIX_WORKSPACE_JAVA_PROCESSORS:'2'});
    let {app,page} = await launch(project);
    const screenshots = [];
    const errors = [];
    const flags = {timeout:45000};
    const api = (route,args) => app.evaluate((_,[route,args])=>global.workspaceTest.api(route,args),[route,args]);
    const call = (command,args) => page.evaluate(([command,args])=>
        window.workspace.request({operation:'requestWorkbench',command,args}),[command,args]);
    const artifactCount = async () => (await api('/api/state')).artifacts.length;

    async function ready() {
        page.on('pageerror',error=>errors.push(error.message));
        await app.evaluate(({BrowserWindow})=>BrowserWindow.getAllWindows().find(w=>w.isVisible()).setSize(1480,1030));
        assert.equal((await api('/api/state')).models.length,0);
    }
    async function restart() {
        const pid = app.process().pid;
        await app.close();
        ({app,page} = await launch(project));
        assert.notEqual(app.process().pid,pid);
        await ready();
    }
    async function chat(text) {
        await page.locator('#goal').fill(text);
        await page.locator('#run').click();
    }
    async function waitMessage(text) {
        await page.getByText(text,{exact:true}).last().waitFor({state:'visible',...flags});
    }
    async function capture(name) {
        await page.evaluate(()=>new Promise(resolve=>requestAnimationFrame(()=>requestAnimationFrame(resolve))));
        const png = Buffer.from(await app.evaluate(()=>global.workspaceTest.capture()),'base64');
        await fs.writeFile(path.join(output,name),png);
        screenshots.push({name,sha256:hash(png)});
    }
    async function readExport(receipt) {
        const csv = await fs.readFile(receipt.path);
        assert.equal(hash(csv),receipt.sha256);
        return {exportId:receipt.id,sha256:receipt.sha256,source:receipt.source,base64:csv.toString('base64')};
    }

    async function buildRoutingPlan(name,text) {
        await chat('Build a data-to-tool classifier using column reading.');
        await page.locator('#requests-file').waitFor({state:'visible',...flags});
        const id = await page.evaluate(()=>localStorage.getItem('fraynix-request-review'));
        await page.locator('#requests-rules-file').setInputFiles({
            name:'ranges.csv',mimeType:'text/csv',buffer:Buffer.from(ranges)});
        await page.waitForFunction(digest=>document.querySelector('#requests-rules-source').textContent.includes(digest),hash(ranges),flags);
        await page.locator('#requests-file').setInputFiles({
            name:name+'.csv',mimeType:'text/csv',buffer:Buffer.from(text)});
        await page.waitForFunction(digest=>document.querySelector('#requests-source').textContent.includes(digest),hash(text),flags);
        await page.locator('#requests-confirm').click();
        await page.waitForFunction(()=>document.querySelector('#requests-status').textContent.startsWith('Built an offline tool.'),{},flags);
        const classification = await api('/api/requests/verify',{id});
        assert(classification.passed);
        const pending = await call('routing-start',{sourceId:id});
        const review = await call('routing-attach',{id:pending.id,base64:Buffer.from(mapping).toString('base64')});
        assert.equal(review.status,'review');
        const completed = await call('run-routing',{id:pending.id,decisionSha256:review.decisionSha256});
        assert.equal(completed.status,'verified');
        return call('export-routing',{id:completed.id});
    }

    async function tamperExport(receipt,verify) {
        const file = receipt.path;
        const meta = file+'.receipt.json';
        const bytes = await fs.readFile(file);
        const originalReceipt = await fs.readFile(meta);
        try {
            const changed = Buffer.from(bytes.toString('utf8').replace('α-1','α-2'));
            assert.notDeepEqual(changed,bytes);
            const rewritten = JSON.parse(originalReceipt);
            rewritten.bytes = changed.length;
            rewritten.sha256 = hash(changed);
            rewritten.source.reportSha256 = rewritten.sha256;
            await fs.writeFile(file,changed);
            await fs.writeFile(meta,JSON.stringify(rewritten));
            await assert.rejects(verify,/disagrees with the verified routing plan/);
        } finally {
            await fs.writeFile(file,bytes);
            await fs.writeFile(meta,originalReceipt);
        }
    }

    const readRows = () => app.evaluate(()=>global.workspaceTest.canvasRead('document.querySelector("#matched-rows").innerText'));
    const readSummary = () => app.evaluate(()=>global.workspaceTest.canvasRead('document.querySelector("#row-counts").innerText'));
    const readyReport = (fresh=false)=>waitMessage('Matched comparison verified against Java. 9 '+(fresh?'fresh ':'')+'browser checks passed.');
    const newPair = (earlier,later)=>({earlier:earlier.id,later:later.id});

    try {
        await ready();
        const previous = JSON.parse(await fs.readFile('docs/readmes/199/verification.json')).saved;
        const legacyPair = {earlier:previous.earlierExportId,later:previous.laterExportId};
        const legacy = await call('routing-dashboard',{verifyOnly:true,compare:true,pair:legacyPair});
        assert.equal(legacy.browserChecks,12);
        assert.deepEqual(legacy.artifact,previous.artifact);
        await chat(recheck);
        await waitMessage('Build a matched routed CSV comparison first');

        const earlier = await buildRoutingPlan('earlier',earlierSource);
        const later = await buildRoutingPlan('later',laterSource);
        const baseline = await readExport(earlier);
        const file = await readExport(later);
        const pair = newPair(earlier,later);
        const sources = {file,baseline,keyColumn};
        const before = await artifactCount();

        await assert.rejects(call('routing-dashboard',{compare:true,keyColumn:'route_source_row'}),/unique original key column/);
        await chat(build);
        await readyReport();
        const saved = await api('/api/requests/routing-dashboard-prepare',sources);
        assert.equal(saved.keyColumn,keyColumn);
        assert.equal(saved.earlierExportId,earlier.id);
        assert.equal(saved.laterExportId,later.id);
        const artifact = await api('/api/artifact/read',{id:saved.artifact.id});
        const proof = JSON.parse(artifact.contents['verification.json']);
        assert.equal(artifact.contents['earlier-routed-results.csv'],Buffer.from(baseline.base64,'base64').toString('utf8'));
        assert.equal(artifact.contents['later-routed-results.csv'],Buffer.from(file.base64,'base64').toString('utf8'));
        assert.deepEqual(proof.counts,{Matched:4,Added:2,Missing:2,Duplicate:2});
        const rows = values=>values.map(row=>[row.row,row.value]);
        assert.deepEqual(proof.outcomes.map(row=>[row.key,row.unit,row.status,rows(row.earlier),rows(row.later),row.delta]),expected);
        assert.equal(await readSummary(),summary);
        const shown = await readRows();
        assert.equal(shown.trim().split('\n').length,expected.length);
        assert.equal(await artifactCount(),before+1);
        await capture('matched-key-and-unit.png');

        const receiptFile = path.join(project,'receipts',saved.artifact.id+'-1.json');
        const originalReceipt = await fs.readFile(receiptFile);
        const intervening = await call('export-routing',{id:earlier.source.planId});
        assert.notEqual(intervening.id,earlier.id);
        await restart();
        await chat(recheck);
        await readyReport(true);
        assert.equal(await readRows(),shown);
        assert.equal(await readSummary(),summary);
        assert.deepEqual(await fs.readFile(receiptFile),originalReceipt);
        assert.equal(await artifactCount(),before+1);
        await capture('matched-rechecked-after-restart.png');

        const verify = () => call('routing-dashboard',{compare:true,verifyOnly:true,pair,keyColumn});
        await tamperExport(earlier,verify);
        await tamperExport(later,verify);
        const prepared = await api('/api/requests/routing-dashboard-prepare',{...sources,verifyOnly:true});
        const changed = JSON.parse(originalReceipt);
        const browserTable = changed.checks.find(row=>row.selector==='#matched-rows').actual;
        const wrong = browserTable.text.replace('38.1','38.2');
        assert.notEqual(wrong,browserTable.text);
        browserTable.text = wrong;
        const finish = {id:saved.id,...sources,htmlSha256:prepared.htmlSha256,checks:changed.checks};
        await assert.rejects(api('/api/requests/routing-dashboard-finish',finish),/Browser result differed/);
        await assert.rejects(api('/api/requests/routing-dashboard-finish',{...finish,keyColumn:'reading'}),/comparison key changed/);
        assert.deepEqual(await api('/api/requests/routing-dashboard-prepare',sources),saved);
        assert.deepEqual(errors,[]);

        for (const [name,bytes] of Object.entries(artifact.contents))
            await fs.writeFile(path.join(output,name==='index.html'?'key-comparison.html':name),bytes);
        const browser = await chromium.launch({channel:'msedge'});
        try {
            const offline = await browser.newPage({viewport:{width:390,height:840},offline:true,acceptDownloads:true});
            await offline.goto(pathToFileURL(path.join(output,'key-comparison.html')).href);
            await offline.locator('#matched-rows tr').last().waitFor();
            assert.equal(await offline.locator('#row-counts').innerText(),summary);
            assert.equal(await offline.locator('#matched-rows').innerText(),shown);
            assert(await offline.locator('body').evaluate(()=>document.documentElement.scrollWidth<=window.innerWidth));
            for (const side of ['earlier','later']) {
                const [download] = await Promise.all([offline.waitForEvent('download'),offline.locator('#source-'+side).click()]);
                const filename = side+'-routed-results.csv';
                assert.equal(download.suggestedFilename(),filename);
                assert.deepEqual(await fs.readFile(await download.path()),await fs.readFile(path.join(output,filename)));
            }
        } finally { await browser.close(); }

        const result = {passed:true,saved,proof,browserReceipt:artifact.test,screenshots,checks:{
            legacyRechecked:true,twoNewRoutingPlans:true,originalColumnsReordered:true,quotedAndUnicodeKeys:true,
            addedMissingDuplicateChecked:true,unitChangesStaySeparate:true,pinnedAcrossRestart:true,
            originalReceiptPreserved:true,oneComparisonArtifact:true,earlierTamperRefused:true,laterTamperRefused:true,
            wrongKeyRefused:true,changedBrowserRowsRefused:true,offlineDownloadsExact:true}};
        await fs.writeFile(path.join(output,'result.json'),JSON.stringify(result,null,2)+'\n');
        console.log(JSON.stringify({passed:true,counts:proof.counts,browserChecks:saved.browserChecks,checks:result.checks}));
    } catch (error) {
        await fs.writeFile(path.join(output,'failure.txt'),error.stack+'\n'+await page.locator('body').innerText()).catch(()=>{});
        throw error;
    } finally { await app.close().catch(()=>{}); }
}

main().catch(error=>{console.error(error);process.exitCode=1;});
