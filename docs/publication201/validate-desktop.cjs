
'use strict';
const assert = require('node:assert/strict');
const {createHash} = require('node:crypto');
const {execFileSync} = require('node:child_process');
const fs = require('node:fs/promises');
const path = require('node:path');
const {NativeExports} = require('../../workspace-desktop/exports.cjs');
const {launch} = require('../../workspace-desktop/test/calibration-integration.cjs');

const hash = bytes => createHash('sha256').update(bytes).digest('hex');
const build = 'Compare the last two verified routed CSVs by key "record ID".';
const request = 'Export the matched routed CSV comparison.';
const kind = 'verified-routed-comparison';
const keyColumn = 'record ID';

async function main() {
    const output = path.resolve(process.argv[2] || 'build/edition201-live');
    const prior = path.resolve(process.argv[3] || 'build/edition200-first/project');
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
    async function waitMessage(text,exact=true) {
        await page.getByText(text,{exact}).last().waitFor({state:'visible',...flags});
    }
    async function capture(name) {
        await page.evaluate(()=>new Promise(resolve=>requestAnimationFrame(()=>requestAnimationFrame(resolve))));
        const png = Buffer.from(await app.evaluate(()=>global.workspaceTest.capture()),'base64');
        await fs.writeFile(path.join(output,name),png);
        screenshots.push({name,sha256:hash(png)});
    }
    async function nativeExports() {
        const manifest = await fs.readFile(path.join(project,'exports','manifest.jsonl'),'utf8');
        return manifest.trim().split('\n').map(line=>JSON.parse(line)).filter(entry=>entry.kind===kind);
    }
    async function waitExport(count) {
        const deadline = Date.now() + flags.timeout;
        do {
            const records = await nativeExports();
            if (records.length >= count) return records[count - 1];
            await new Promise(resolve=>setTimeout(resolve,100));
        } while (Date.now() < deadline);
        throw Error('Timed out waiting for verified comparison export '+count);
    }
    async function comparisonFiles() {
        return (await fs.readdir(path.join(project,'exports'))).filter(name=>name.endsWith('.csv')).sort();
    }
    async function readExport(receipt) {
        const csv = await fs.readFile(receipt.path);
        assert.equal(hash(csv),receipt.sha256);
        return {exportId:receipt.id,sha256:receipt.sha256,source:receipt.source,base64:csv.toString('base64')};
    }
    async function validateExport(receipt,report,proofPath) {
        assert.equal(receipt.kind,kind);
        assert.deepEqual(JSON.parse(await fs.readFile(receipt.path+'.receipt.json','utf8')),receipt);
        const bytes = await fs.readFile(receipt.path);
        assert.equal(hash(bytes),receipt.sha256);
        assert.equal(bytes.length,receipt.bytes);
        assert.equal(receipt.source.comparisonId,report.id);
        assert.deepEqual(receipt.source.artifact,report.artifact);
        assert.equal(receipt.source.keyColumn,keyColumn);
        assert.equal(receipt.source.browserChecks,9);
        assert.match(receipt.source.browserEvidenceSha256,/^[0-9a-f]{64}$/);
        assert.deepEqual(receipt.source.earlier,{exportId:report.earlierExportId,sha256:report.earlierCsvSha256});
        assert.deepEqual(receipt.source.later,{exportId:report.laterExportId,sha256:report.csvSha256});
        assert.deepEqual(JSON.parse(execFileSync('python',['docs/publication201/verify-export.py',receipt.path,proofPath])),
            {passed:true,groups:10,sha256:receipt.sha256});
        return bytes;
    }
    function publicReceipt(receipt) {
        return {...receipt,path:path.basename(receipt.path)};
    }

    try {
        await ready();
        const prev = JSON.parse(await fs.readFile('docs/readmes/200/verification.json','utf8'));
        const historical = prev.saved;
        const legacyPair = {earlier:historical.earlierExportId,later:historical.laterExportId};
        const old = await call('routing-dashboard',{verifyOnly:true,compare:true,pair:legacyPair,keyColumn});
        assert.deepEqual(old.artifact,historical.artifact);
        assert(old.rechecked && old.browserChecks===9);
        await chat(request);
        await waitMessage('Build a matched routed CSV comparison first');
        assert.equal((await nativeExports()).length,0);

        const earlier = await call('export-routing',{id:prev.proof.earlier.routing.planId});
        const later = await call('export-routing',{id:prev.proof.later.routing.planId});
        const baseline = await readExport(earlier);
        const file = await readExport(later);
        const pair = {earlier:earlier.id,later:later.id};
        const sources = {file,baseline,keyColumn};
        const before = await artifactCount();
        await chat(build);
        await waitMessage('Matched comparison verified against Java. 9 browser checks passed.');
        const report = await api('/api/requests/routing-dashboard-prepare',sources);
        assert.equal(report.earlierExportId,pair.earlier);
        assert.equal(report.laterExportId,pair.later);
        const artifact = await api('/api/artifact/read',{id:report.artifact.id});
        const evidence = JSON.parse(artifact.contents['verification.json']);
        assert.deepEqual(evidence.outcomes,prev.proof.outcomes);
        const proofPath = path.join(output,'source-comparison-proof.json');
        await fs.writeFile(proofPath,artifact.contents['verification.json']);
        const browserReceipt = path.join(project,'receipts',report.artifact.id+'-1.json');
        const untouched = await fs.readFile(browserReceipt);

        await chat(request);
        const first = await waitExport(1);
        await waitMessage('Saved 10 verified comparison groups to '+first.path);
        const bytes = await validateExport(first,report,proofPath);
        assert.equal((await nativeExports()).length,1);
        assert.equal(await artifactCount(),before+1);
        await capture('comparison-csv-export.png');

        const intervening = await call('export-routing',{id:earlier.source.planId});
        assert.notEqual(intervening.id,earlier.id);
        await restart();
        await chat(request);
        const second = await waitExport(2);
        await waitMessage('Saved 10 verified comparison groups to '+second.path);
        assert.notEqual(first.id,second.id);
        assert.deepEqual(first.source,second.source);
        assert.deepEqual(await validateExport(second,report,proofPath),bytes);
        assert.equal((await nativeExports()).length,2);
        assert.equal(await artifactCount(),before+1);
        assert.deepEqual(await fs.readFile(browserReceipt),untouched);
        await capture('comparison-csv-export-restarted.png');

        const baseFinish = {id:report.id,...sources,htmlSha256:report.htmlSha256,exportCsv:true};
        await assert.rejects(api('/api/requests/routing-dashboard-finish',baseFinish),/Recheck a saved matched comparison/);
        const forged = JSON.parse(untouched);
        const browserTable = forged.checks.find(check=>check.selector==='#matched-rows').actual;
        const changed = browserTable.text.replace('38.1','38.2');
        assert.notEqual(browserTable.text,changed);
        browserTable.text = changed;
        await assert.rejects(api('/api/requests/routing-dashboard-finish',{...baseFinish,checks:forged.checks}),/Browser result differed/);

        const verified = await api('/api/requests/routing-dashboard-finish',
            {...baseFinish,checks:JSON.parse(untouched).checks});
        const filesBeforeRefusal = await comparisonFiles();
        const native = new NativeExports(project);
        await assert.rejects(native.matchedComparison({...verified,csvExport:{...verified.csvExport,csv:'tampered\n'}}),
            /verified comparison CSV bytes changed/);

        const receiptPath = earlier.path+'.receipt.json';
        const originalNativeReceipt = await fs.readFile(receiptPath);
        const originalSource = await fs.readFile(earlier.path);
        try {
            const replacement = Buffer.from(originalSource.toString('utf8').replace('α-1','α-2'));
            assert.notDeepEqual(replacement,originalSource);
            const changedReceipt = JSON.parse(originalNativeReceipt);
            changedReceipt.bytes = replacement.length;
            changedReceipt.sha256 = hash(replacement);
            changedReceipt.source.reportSha256 = changedReceipt.sha256;
            await fs.writeFile(earlier.path,replacement);
            await fs.writeFile(receiptPath,JSON.stringify(changedReceipt));
            await assert.rejects(call('routing-dashboard',{compare:true,verifyOnly:true,pair,keyColumn,exportCsv:true}),
                /disagrees with the verified routing plan/);
        } finally {
            await fs.writeFile(earlier.path,originalSource);
            await fs.writeFile(receiptPath,originalNativeReceipt);
        }
        try {
            await fs.writeFile(browserReceipt,JSON.stringify(forged));
            await assert.rejects(call('routing-dashboard',{compare:true,verifyOnly:true,pair,keyColumn,exportCsv:true}),
                /original artifact test receipt changed/i);
        } finally { await fs.writeFile(browserReceipt,untouched); }

        assert.deepEqual(await comparisonFiles(),filesBeforeRefusal);
        assert.equal((await nativeExports()).length,2);
        assert.deepEqual(errors,[]);
        await fs.writeFile(path.join(output,'matched-comparison.csv'),bytes);
        const result = {passed:true,report,first:publicReceipt(first),second:publicReceipt(second),screenshots,checks:{
            priorComparisonRechecked:true,exportWithoutReportRefused:true,allRowsAndStatusesMatch:true,
            identicalAcrossRestart:true,pinnedDespiteNewSourceExport:true,originalReceiptPreserved:true,
            missingBrowserChecksRefused:true,changedBrowserRowsRefused:true,changedTransferRefused:true,
            rewrittenInputRefused:true,modifiedSavedReceiptRefused:true,noCsvWrittenOnRefusal:true}};
        await fs.writeFile(path.join(output,'result.json'),JSON.stringify(result,null,2)+'\n');
        console.log(JSON.stringify({passed:true,groups:second.source.groups,sha256:second.sha256,checks:result.checks}));
    } catch (error) {
        await fs.writeFile(path.join(output,'failure.txt'),error.stack+'\n'+await page.locator('body').innerText()).catch(()=>{});
        throw error;
    } finally { await app.close().catch(()=>{}); }
}

main().catch(error=>{console.error(error);process.exitCode=1;});
