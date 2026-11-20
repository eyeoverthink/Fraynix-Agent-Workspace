
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

const hash = bytes=>createHash('sha256').update(bytes).digest('hex');
const build = 'Build an explorer from the verified comparison totals.';
const recheck = 'Recheck the verified comparison explorer.';
const summarize = 'Summarize the last verified matched comparison CSV.';
const summaryOK = 'Comparison CSV totals verified against Java. 13 browser checks passed.';
const firstOK = 'Comparison explorer verified against Java. 24 browser checks passed.';
const rechecked = 'Comparison explorer verified against Java. 24 fresh browser checks passed.';

function idFrom(value) {
    const bytes = createHash('md5').update(value).digest();
    bytes[6] = bytes[6] & 15 | 48;
    bytes[8] = bytes[8] & 63 | 128;
    const hex = bytes.toString('hex');
    return [hex.slice(0,8),hex.slice(8,12),hex.slice(12,16),hex.slice(16,20),hex.slice(20)].join('-');
}

async function main() {
    const output = path.resolve(process.argv[2] || 'build/edition203-live');
    const prior = path.resolve(process.argv[3] || 'build/edition202-release/project');
    const project = path.join(output,'project');
    await fs.mkdir(output);
    await fs.mkdir(project);
    for (const name of ['workspace.fworld','workspace.fworld.previous','artifacts','receipts','request-workbench','exports'])
        await fs.cp(path.join(prior,name),path.join(project,name),{recursive:true});
    Object.assign(process.env,{FRAYNIX_WORKSPACE_NO_MODELS:'1',FRAYNIX_WORKSPACE_PERSISTENT:'0',
        FRAYNIX_WORKSPACE_JAVA_HEAP_MIB:'768',FRAYNIX_WORKSPACE_JAVA_PROCESSORS:'2'});
    let {app,page} = await launch(project);
    const flags = {timeout:45000};
    const errors = [];
    const screenshots = [];
    const native = new NativeExports(project);
    const api = (route,args)=>app.evaluate((_,[route,args])=>global.workspaceTest.api(route,args),[route,args]);
    const call = (command,args)=>page.evaluate(([command,args])=>
        window.workspace.request({operation:'requestWorkbench',command,args}),[command,args]);
    const artifactCount = async()=>(await api('/api/state')).artifacts.length;

    async function ready() {
        page.on('pageerror',error=>errors.push(error.message));
        await app.evaluate(({BrowserWindow})=>BrowserWindow.getAllWindows().find(w=>w.isVisible()).setSize(1560,1050));
        assert.equal((await api('/api/state')).models.length,0);
    }
    async function restart() {
        const pid = app.process().pid;
        await app.close();
        ({app,page} = await launch(project));
        assert.notEqual(app.process().pid,pid);
        await ready();
    }
    async function send(message) {
        await page.locator('#goal').fill(message);
        await page.locator('#run').click();
    }
    async function chat(message,reply) {
        await send(message);
        await page.getByText(reply,{exact:true}).last().waitFor({state:'visible',...flags});
    }
    async function capture(name) {
        await page.evaluate(()=>new Promise(resolve=>requestAnimationFrame(()=>requestAnimationFrame(resolve))));
        const png = Buffer.from(await app.evaluate(()=>global.workspaceTest.capture()),'base64');
        await fs.writeFile(path.join(output,name),png);
        screenshots.push({name,sha256:hash(png)});
    }
    const readCount = ()=>app.evaluate(()=>global.workspaceTest.canvasRead('document.querySelector("#visible-count").innerText'));
    const readSource = ()=>app.evaluate(()=>global.workspaceTest.canvasRead('document.querySelector("#source-id").innerText'));

    try {
        await ready();
        const published = JSON.parse(await fs.readFile('docs/readmes/202/verification.json','utf8'));
        const previous = await call('routing-summary',{verifyOnly:true,exportId:published.report.comparisonExportId});
        assert.deepEqual(previous.artifact,published.report.artifact);
        assert(previous.rechecked && previous.browserChecks===13);
        await chat(build,'Summarize a comparison CSV first');
        await chat(recheck,'Build a verified comparison explorer first');

        const imported = await native.comparisonCsv();
        const id = imported.receipt.id;
        assert.notEqual(id,published.report.comparisonExportId);
        const {earlier,later,keyColumn} = imported.receipt.source;
        const [baseline,file] = await Promise.all([native.routedCsv(earlier.exportId),native.routedCsv(later.exportId)]);
        const summarySources = {comparisonImport:imported,baseline,file,keyColumn};
        const explorerSources = {...summarySources,explorer:true};
        const pair = {earlier:earlier.exportId,later:later.exportId};
        await assert.rejects(call('routing-summary',{exportId:id,explorer:true}),/Summarize the comparison CSV before building/);
        await chat(summarize,summaryOK);
        const summary = await api('/api/requests/routing-dashboard-prepare',summarySources);
        const parent = await api('/api/artifact/read',{id:summary.artifact.id});
        assert.equal(summary.comparisonExportId,id);
        const before = await artifactCount();

        const taskId = idFrom('routed-comparison-explorer:'+id);
        const expectedArtifact = idFrom('routed-dashboard:'+taskId);
        const blocked = path.join(project,'artifacts',expectedArtifact);
        const taskFile = path.join(project,'request-workbench','routed-dashboards',taskId+'.json');
        let staged;
        await fs.writeFile(blocked,'Fixture blocks only the new artifact directory',{flag:'wx'});
        try {
            await send(build);
            await page.waitForFunction(value=>document.querySelector('#notice').textContent.includes(value),expectedArtifact,flags);
            staged = JSON.parse(await fs.readFile(taskFile,'utf8')).data;
            assert.equal(staged.checks.length,24);
            assert(!staged.artifact);
            assert.equal(await artifactCount(),before);
            assert((await api('/api/requests/routing-dashboard-prepare',explorerSources)).savePending);
        } finally { await fs.unlink(blocked); }

        await restart();
        await chat(build,firstOK);
        const report = await api('/api/requests/routing-dashboard-prepare',explorerSources);
        assert.equal(report.id,taskId);
        assert.equal(report.artifact.id,expectedArtifact);
        assert.equal(report.comparisonExportId,id);
        assert.equal(report.browserChecks,24);
        assert.equal(await artifactCount(),before+1);
        assert.equal(await readCount(),'10 groups');
        const artifact = await api('/api/artifact/read',{id:report.artifact.id});
        const proof = JSON.parse(artifact.contents['verification.json']);
        assert.deepEqual(artifact.test.checks,staged.checks);
        assert.deepEqual(proof.summary.artifact,summary.artifact);
        assert.equal(proof.summary.proofSha256,hash(artifact.contents['summary-proof.json']));
        assert.deepEqual(proof.import,imported.receipt);
        assert.equal(artifact.contents['matched-comparison.csv'],parent.contents['matched-comparison.csv']);
        assert.equal(artifact.contents['import-receipt.json'],parent.contents['import-receipt.json']);
        assert.equal(artifact.contents['summary-proof.json'],parent.contents['verification.json']);
        assert.deepEqual(proof.views[0].totals,JSON.parse(parent.contents['verification.json']).totals);
        assert.deepEqual(proof.views.map(view=>view.groups),[10,4,1,0]);

        const filtered = await app.evaluate(()=>global.workspaceTest.canvasChecks([
            {selector:'#unit-mm',action:'click',expectCount:1},
            {selector:'#status-duplicate',action:'click',expectCount:1},
            {selector:'#visible-count',expectText:'1 group'},
        ]));
        assert(filtered.every(check=>check.passed));
        assert.equal(await readCount(),'1 group');
        await capture('comparison-explorer-duplicates.png');

        const receiptFile = path.join(project,'receipts',report.artifact.id+'-1.json');
        const original = await fs.readFile(receiptFile);
        const laterExport = await call('routing-dashboard',{verifyOnly:true,compare:true,pair,keyColumn,exportCsv:true});
        assert.notEqual(laterExport.id,id);
        await send(summarize);
        await page.waitForFunction(id=>localStorage.getItem('fraynix-last-comparison-summary')===id,laterExport.id,flags);
        assert.equal(await artifactCount(),before+2);
        await restart();
        await chat(recheck,rechecked);
        assert.equal(await readSource(),'Export '+id);
        assert.equal(await readCount(),'10 groups');
        assert.equal(await artifactCount(),before+2);
        assert.deepEqual(await fs.readFile(receiptFile),original);
        await capture('comparison-explorer-rechecked-after-restart.png');

        const summaryReceiptFile = path.join(project,'receipts',summary.artifact.id+'-1.json');
        const originalSummary = await fs.readFile(summaryReceiptFile);
        try {
            const changed = JSON.parse(originalSummary);
            changed.checks[0].actual.text='Altered summary evidence';
            await fs.writeFile(summaryReceiptFile,JSON.stringify(changed));
            await assert.rejects(call('routing-summary',{exportId:id,verifyOnly:true,explorer:true}),/original artifact test receipt changed/i);
        } finally { await fs.writeFile(summaryReceiptFile,originalSummary); }

        const forged = JSON.parse(original);
        const filteredRows = forged.checks.filter(check=>check.selector==='#visible-rows')[2];
        assert.match(filteredRows.actual.text, /44\.45/);
        filteredRows.actual.text=filteredRows.actual.text.replace('44.45','44.46');
        await assert.rejects(api('/api/requests/routing-dashboard-finish',
            {id:report.id,...explorerSources,htmlSha256:report.htmlSha256,checks:forged.checks}),/Browser result differed/);

        const exportedPath = path.join(project,'exports',id+'.csv');
        const sourceBytes = await fs.readFile(exportedPath);
        try {
            await fs.writeFile(exportedPath,Buffer.concat([sourceBytes,Buffer.from('\n')]));
            await assert.rejects(call('routing-summary',{exportId:id,verifyOnly:true,explorer:true}),/disagree with the export manifest/);
        } finally { await fs.writeFile(exportedPath,sourceBytes); }

        assert.deepEqual(await fs.readFile(receiptFile),original);
        assert.equal(await artifactCount(),before+2);
        assert.deepEqual(errors,[]);
        for (const [name,bytes] of Object.entries(artifact.contents))
            await fs.writeFile(path.join(output,name==='index.html'?'comparison-explorer.html':name),bytes);
        const independently = JSON.parse(execFileSync('python',['docs/publication203/verify-views.py',
            path.join(output,'matched-comparison.csv'),path.join(output,'verification.json')]));
        assert(independently.passed && independently.views.length===15);

        const browser = await chromium.launch({channel:'msedge'});
        try {
            const offline = await browser.newPage({viewport:{width:390,height:840},offline:true,acceptDownloads:true});
            await offline.goto(pathToFileURL(path.join(output,'comparison-explorer.html')).href);
            await offline.locator('#visible-rows tr').last().waitFor();
            assert(await offline.locator('body').evaluate(()=>document.documentElement.scrollWidth<=window.innerWidth));
            for (const view of independently.views) {
                await offline.locator('button[data-unit="'+view.unit+'"]').click();
                await offline.locator('button[data-status="'+view.status+'"]').click();
                const rows = await offline.locator('#visible-rows tr').evaluateAll(elements=>elements.map(row=>{
                    const cells = [...row.cells].map(cell=>cell.innerText);
                    cells[0] = JSON.parse(cells[0]);
                    return cells;
                }));
                assert.deepEqual(rows,view.rows);
                assert.deepEqual(await offline.locator('#visible-totals div').allInnerTexts(),view.lines);
                assert.equal(await offline.locator('#visible-count').innerText(),view.groups+(view.groups===1?' group':' groups'));
            }
            await offline.locator('#reset-filters').click();
            assert.equal(await offline.locator('#visible-count').innerText(),'10 groups');
            for (const name of ['matched-comparison.csv','import-receipt.json','summary-proof.json']) {
                const [download] = await Promise.all([offline.waitForEvent('download'),offline.locator('a[href="'+name+'"]').click()]);
                assert.deepEqual(await fs.readFile(await download.path()),await fs.readFile(path.join(output,name)));
            }
        } finally { await browser.close(); }

        const result = {passed:true,report,proof,browserReceipt:artifact.test,screenshots,offlineViews:15,checks:{
            priorSummaryRechecked:true,missingSummaryRefused:true,interactiveToolFromChat:true,
            blockedSaveRecoveredAfterRestart:true,originalStagedChecksPreserved:true,sourceAndParentBound:true,
            parentChangedAfterBuildButOriginalRechecked:true,changedParentReceiptRefused:true,changedCsvRefused:true,
            wrongFilteredRowsRefused:true,allFiltersAgreeWithIndependentDecimalCheck:true,offlineDownloadsExact:true}};
        await fs.writeFile(path.join(output,'result.json'),JSON.stringify(result,null,2)+'\n');
        console.log(JSON.stringify({passed:true,browserChecks:report.browserChecks,offlineViews:result.offlineViews,checks:result.checks}));
    } catch (error) {
        await fs.writeFile(path.join(output,'failure.txt'),error.stack+'\n'+await page.locator('body').innerText()).catch(()=>{});
        throw error;
    } finally { await app.close().catch(()=>{}); }
}

main().catch(error=>{console.error(error);process.exitCode=1;});
