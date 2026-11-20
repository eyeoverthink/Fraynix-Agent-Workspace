
'use strict';
const assert = require('node:assert/strict');
const {createHash} = require('node:crypto');
const {execFileSync} = require('node:child_process');
const fs = require('node:fs/promises');
const path = require('node:path');
const {pathToFileURL} = require('node:url');
const {chromium} = require('../../workspace-desktop/node_modules/playwright');
const {NativeExports} = require('../../workspace-desktop/exports.cjs');
const {launch} = require('../../workspace-desktop/test/calibration-integration.cjs');

const hash = bytes => createHash('sha256').update(bytes).digest('hex');
const build = 'Summarize the last verified matched comparison CSV.';
const recheck = 'Recheck the matched comparison CSV summary.';
const expected = [
    {unit:'°F',matched:3,added:0,missing:2,duplicate:1,netDelta:'7.2',absoluteDelta:'7.2'},
    {unit:'mm',matched:1,added:2,missing:0,duplicate:1,netDelta:'-12.7',absoluteDelta:'12.7'},
];
const display = ['10 verified groups','Matched 3 · Added 0 · Missing 2 · Duplicate 1','+7.2 °F','7.2 °F',
    'Matched 1 · Added 2 · Missing 0 · Duplicate 1','-12.7 mm','12.7 mm'];
const selectors = ['group-count','counts-fahrenheit','net-fahrenheit','absolute-fahrenheit',
    'counts-millimetres','net-millimetres','absolute-millimetres'];

async function main() {
    const output = path.resolve(process.argv[2] || 'build/edition202-live');
    const prior = path.resolve(process.argv[3] || 'build/edition201-verified/project');
    const project = path.join(output,'project');
    await fs.mkdir(output);
    await fs.mkdir(project);
    for (const name of ['workspace.fworld','workspace.fworld.previous','artifacts','receipts','request-workbench','exports'])
        await fs.cp(path.join(prior,name),path.join(project,name),{recursive:true});
    Object.assign(process.env,{FRAYNIX_WORKSPACE_NO_MODELS:'1',FRAYNIX_WORKSPACE_PERSISTENT:'0',
        FRAYNIX_WORKSPACE_JAVA_HEAP_MIB:'768',FRAYNIX_WORKSPACE_JAVA_PROCESSORS:'2'});
    let {app,page} = await launch(project);
    const errors = [];
    const screenshots = [];
    const flags = {timeout:45000};
    const native = new NativeExports(project);
    const exportDirectory = path.join(project,'exports');
    const api = (route,args)=>app.evaluate((_,[route,args])=>global.workspaceTest.api(route,args),[route,args]);
    const call = (command,args)=>page.evaluate(([command,args])=>
        window.workspace.request({operation:'requestWorkbench',command,args}),[command,args]);
    const artifactCount = async()=>(await api('/api/state')).artifacts.length;
    const readDisplay = ()=>app.evaluate((_,ids)=>global.workspaceTest.canvasRead(
        JSON.stringify(ids)+'.map(id=>document.getElementById(id).innerText)'),selectors);

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
    async function chat(message,reply) {
        await page.locator('#goal').fill(message);
        await page.locator('#run').click();
        await page.getByText(reply,{exact:true}).last().waitFor({state:'visible',...flags});
    }
    async function capture(name) {
        await page.evaluate(()=>new Promise(resolve=>requestAnimationFrame(()=>requestAnimationFrame(resolve))));
        const png = Buffer.from(await app.evaluate(()=>global.workspaceTest.capture()),'base64');
        await fs.writeFile(path.join(output,name),png);
        screenshots.push({name,sha256:hash(png)});
    }

    async function tamperSelected(id,mutate,includeManifest,pattern) {
        const file = path.join(exportDirectory,id+'.csv');
        const sidecar = file+'.receipt.json';
        const manifest = path.join(exportDirectory,'manifest.jsonl');
        const [bytes,receiptText,manifestText] = await Promise.all([fs.readFile(file),fs.readFile(sidecar,'utf8'),
            fs.readFile(manifest,'utf8')]);
        const receipt = JSON.parse(receiptText);
        try {
            const changed = mutate(receipt,bytes);
            await fs.writeFile(file,changed);
            await fs.writeFile(sidecar,JSON.stringify(receipt));
            if (includeManifest) {
                const lines = manifestText.trim().split('\n').map(line=>JSON.parse(line));
                await fs.writeFile(manifest,lines.map(entry=>JSON.stringify(entry.id===id?receipt:entry)).join('\n')+'\n');
                await native.comparisonCsv(id);
            }
            await assert.rejects(call('routing-summary',{exportId:id,verifyOnly:true}),pattern);
        } finally {
            await fs.writeFile(file,bytes);
            await fs.writeFile(sidecar,receiptText);
            if (includeManifest) await fs.writeFile(manifest,manifestText);
        }
    }

    try {
        await ready();
        const published = JSON.parse(await fs.readFile('docs/readmes/201/export-receipt.json','utf8'));
        const imported = await native.comparisonCsv();
        assert.equal(imported.receipt.id,published.id);
        assert.equal(imported.receipt.sha256,published.sha256);
        const {earlier,later,keyColumn} = imported.receipt.source;
        const pair = {earlier:earlier.exportId,later:later.exportId};
        const priorReport = await call('routing-dashboard',{verifyOnly:true,compare:true,pair,keyColumn});
        assert.deepEqual(priorReport.artifact,imported.receipt.source.artifact);
        assert(priorReport.rechecked && priorReport.browserChecks===9);
        await chat(recheck,'Summarize a comparison CSV first');
        const before = await artifactCount();
        await chat(build,'Comparison CSV totals verified against Java. 13 browser checks passed.');

        const [baseline,file] = await Promise.all([native.routedCsv(earlier.exportId),native.routedCsv(later.exportId)]);
        const sources = {comparisonImport:imported,baseline,file,keyColumn};
        const report = await api('/api/requests/routing-dashboard-prepare',sources);
        assert.equal(report.comparisonExportId,imported.receipt.id);
        assert.equal(report.browserChecks,13);
        const artifact = await api('/api/artifact/read',{id:report.artifact.id});
        const proof = JSON.parse(artifact.contents['verification.json']);
        assert.deepEqual(proof.import,imported.receipt);
        assert.deepEqual(JSON.parse(artifact.contents['import-receipt.json']),imported.receipt);
        assert.deepEqual(proof.totals,expected);
        assert.equal(artifact.contents['matched-comparison.csv'],Buffer.from(imported.base64,'base64').toString('utf8'));
        assert.deepEqual(await readDisplay(),display);
        assert.equal(await artifactCount(),before+1);
        await capture('comparison-totals-by-unit.png');

        const savedReceiptFile = path.join(project,'receipts',report.artifact.id+'-1.json');
        const savedReceipt = await fs.readFile(savedReceiptFile);
        const intervening = await call('routing-dashboard',{verifyOnly:true,compare:true,pair,keyColumn,exportCsv:true});
        assert.notEqual(intervening.id,imported.receipt.id);
        assert.equal(intervening.sha256,imported.receipt.sha256);
        assert.equal((await native.comparisonCsv()).receipt.id,intervening.id);
        await restart();
        await chat(recheck,'Comparison CSV totals verified against Java. 13 fresh browser checks passed.');
        assert.deepEqual(await readDisplay(),display);
        assert.equal(await app.evaluate(()=>global.workspaceTest.canvasRead('document.querySelector("#import-id").innerText')),
            'Export '+imported.receipt.id);
        assert.equal(await artifactCount(),before+1);
        assert.deepEqual(await fs.readFile(savedReceiptFile),savedReceipt);
        await capture('comparison-totals-rechecked-after-restart.png');

        const id = imported.receipt.id;
        await tamperSelected(id,(receipt,bytes)=>{
            receipt.source.keyColumn='different';
            return bytes;
        },false,/disagree with the export manifest/);
        await tamperSelected(id,(receipt,bytes)=>{
            receipt.source.browserEvidenceSha256='0'.repeat(64);
            return bytes;
        },true,/disagrees with its verified source/);
        await tamperSelected(id,(receipt,bytes)=>{
            const changed = Buffer.from(bytes.toString('utf8').replace(',2.25\n',',2.26\n'));
            assert.notDeepEqual(changed,bytes);
            receipt.bytes=changed.length;
            receipt.sha256=hash(changed);
            return changed;
        },true,/disagrees with its verified source/);

        const originalBrowserFile = path.join(project,'receipts',priorReport.artifact.id+'-1.json');
        const originalBrowser = await fs.readFile(originalBrowserFile);
        try {
            const altered = JSON.parse(originalBrowser);
            altered.checks[0].actual.text='wrong parent evidence';
            await fs.writeFile(originalBrowserFile,JSON.stringify(altered));
            await assert.rejects(call('routing-summary',{verifyOnly:true,exportId:id}),/original artifact test receipt changed/i);
        } finally { await fs.writeFile(originalBrowserFile,originalBrowser); }
        const forged = JSON.parse(savedReceipt);
        forged.checks.find(row=>row.selector==='#net-fahrenheit').actual.text='-12.7 mm';
        await assert.rejects(api('/api/requests/routing-dashboard-finish',
            {id:report.id,...sources,htmlSha256:report.htmlSha256,checks:forged.checks}),/Browser result differed/);
        assert.equal(await artifactCount(),before+1);
        assert.deepEqual(await fs.readFile(savedReceiptFile),savedReceipt);
        assert.deepEqual(errors,[]);

        for (const [name,bytes] of Object.entries(artifact.contents))
            await fs.writeFile(path.join(output,name==='index.html'?'comparison-totals.html':name),bytes);
        assert.deepEqual(JSON.parse(execFileSync('python',['docs/publication202/verify-totals.py',
            path.join(output,'matched-comparison.csv'),path.join(output,'verification.json')])),
            {passed:true,groups:10,units:2});

        const browser = await chromium.launch({channel:'msedge'});
        try {
            const offline = await browser.newPage({viewport:{width:390,height:840},offline:true,acceptDownloads:true});
            await offline.goto(pathToFileURL(path.join(output,'comparison-totals.html')).href);
            await offline.locator('#net-millimetres').waitFor();
            for (const [index,name] of selectors.entries())
                assert.equal(await offline.locator('#'+name).innerText(),display[index]);
            assert(await offline.locator('body').evaluate(()=>document.documentElement.scrollWidth<=window.innerWidth));
            for (const name of ['matched-comparison.csv','import-receipt.json']) {
                const [download] = await Promise.all([offline.waitForEvent('download'),offline.locator('a[href="'+name+'"]').click()]);
                assert.deepEqual(await fs.readFile(await download.path()),await fs.readFile(path.join(output,name)));
            }
        } finally { await browser.close(); }

        const result = {passed:true,report,proof,browserReceipt:artifact.test,screenshots,checks:{
            priorComparisonRechecked:true,summaryByChat:true,receiptCarried:true,allTotalsMatchPython:true,
            unitSeparation:true,sameImportAcrossRestart:true,originalReceiptPreserved:true,
            divergentManifestRefused:true,forgedEvidenceHashRefused:true,rewrittenCsvRefused:true,
            changedParentReceiptRefused:true,mixedUnitsRefused:true,offlineDownloadsExact:true}};
        await fs.writeFile(path.join(output,'result.json'),JSON.stringify(result,null,2)+'\n');
        console.log(JSON.stringify({passed:true,totals:proof.totals,browserChecks:report.browserChecks,checks:result.checks}));
    } catch (error) {
        await fs.writeFile(path.join(output,'failure.txt'),error.stack+'\n'+await page.locator('body').innerText()).catch(()=>{});
        throw error;
    } finally { await app.close().catch(()=>{}); }
}

main().catch(error=>{console.error(error);process.exitCode=1;});
