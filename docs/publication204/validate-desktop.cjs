
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

const name = 'field-audit';
const pin = 'fraynix-comparison-explorer-lesson-'+name;
const keyColumn = 'record ID';
const teach = 'Teach comparison explorer lesson '+name+'.';
const use = 'Use comparison explorer lesson '+name+'.';
const recheck = 'Recheck comparison explorer lesson '+name+'.';
const useOK = 'Comparison explorer lesson '+name+' verified against Java. 24 browser checks passed.';
const freshOK = 'Comparison explorer lesson '+name+' verified against Java. 24 fresh browser checks passed.';
const summarize = 'Summarize the last verified matched comparison CSV.';
const compare = 'Compare the last two verified routed CSVs by key "record ID".';
const build = 'Build an explorer from the verified comparison totals.';
const ranges = 'min_inclusive,max_exclusive,label\n0,10,temperature\n10,20,length';
const hash = bytes=>createHash('sha256').update(bytes).digest('hex');
const header = 'record ID,reading,probe_c,inches';
const changedHeader = 'reading,inches,record ID,probe_c';
const cases = {
    'temperature-only': {
        earlier:[header,'rise,4,0,1','fall,4,10,1','gone,4,-40,1','double,4,2,1','double,4,3,1'],
        later:[changedHeader,'4,1,fall,5','4,1,rise,5','4,1,double,2','4,1,new,16'],
        lengths:false, groups:5, unit:'°F', counts:[5,5,1,1],
        totals:[{unit:'°F',matched:2,added:1,missing:1,duplicate:1,netDelta:'0',absoluteDelta:'18'}],
    },
    'mixed-unmatched': {
        earlier:[header,'small,4,0,1','move,4,8,1','hinge,12,0,2','hinge,12,0,2.5','retired,12,0,7'],
        later:[changedHeader,'12,3,hinge,0','12,1.25,move,0','12,2,fresh,0','4,1,small,0.0001'],
        lengths:true, groups:6, unit:'mm', counts:[6,4,1,0],
        totals:[{unit:'°F',matched:1,added:0,missing:1,duplicate:0,netDelta:'0.00018',absoluteDelta:'0.00018'},
            {unit:'mm',matched:0,added:2,missing:1,duplicate:1,netDelta:null,absoluteDelta:null}],
    },
};

async function offline(browse,folder) {
    const independent = JSON.parse(execFileSync('python',['docs/publication203/verify-views.py',
        path.join(folder,'matched-comparison.csv'),path.join(folder,'verification.json')]));
    assert(independent.passed && independent.views.length===15);
    const page = await browse.newPage({viewport:{width:390,height:820},offline:true,acceptDownloads:true});
    try {
        await page.goto(pathToFileURL(path.join(folder,'comparison-explorer.html')).href);
        await page.locator('#visible-rows tr').last().waitFor();
        assert(await page.locator('body').evaluate(()=>document.documentElement.scrollWidth<=window.innerWidth));
        for (const view of independent.views) {
            await page.locator('button[data-unit="'+view.unit+'"]').click();
            await page.locator('button[data-status="'+view.status+'"]').click();
            const rows = await page.locator('#visible-rows tr').evaluateAll(elements=>elements.map(row=>{
                const cells = [...row.cells].map(cell=>cell.innerText);
                cells[0] = JSON.parse(cells[0]);
                return cells;
            }));
            assert.deepEqual(rows,view.rows);
            assert.deepEqual(await page.locator('#visible-totals div').allInnerTexts(),view.lines);
            assert.equal(await page.locator('#visible-count').innerText(),view.groups+(view.groups===1?' group':' groups'));
        }
        await page.locator('#reset-filters').click();
        assert.equal(await page.locator('#visible-rows tr').count(),independent.views[0].groups);
        for (const filename of ['matched-comparison.csv','import-receipt.json','summary-proof.json']) {
            const [download] = await Promise.all([page.waitForEvent('download'),page.locator('a[href="'+filename+'"]').click()]);
            assert.deepEqual(await fs.readFile(await download.path()),await fs.readFile(path.join(folder,filename)));
        }
        return independent.views.length;
    } finally { await page.close(); }
}

async function main() {
    const output = path.resolve(process.argv[2] || 'build/edition204-live');
    const prior = path.resolve(process.argv[3] || 'build/edition203-final/project');
    const project = path.join(output,'project');
    await fs.mkdir(output);
    await fs.mkdir(project);
    for (const entry of ['workspace.fworld','workspace.fworld.previous','artifacts','receipts','request-workbench','exports'])
        await fs.cp(path.join(prior,entry),path.join(project,entry),{recursive:true});
    Object.assign(process.env,{FRAYNIX_WORKSPACE_NO_MODELS:'1',FRAYNIX_WORKSPACE_PERSISTENT:'0',
        FRAYNIX_WORKSPACE_JAVA_HEAP_MIB:'768',FRAYNIX_WORKSPACE_JAVA_PROCESSORS:'2'});

    let {app,page} = await launch(project);
    const flags = {timeout:45000};
    const errors = [];
    const screenshots = [];
    const exports = new NativeExports(project);
    const api = (route,args)=>app.evaluate((_,[route,args])=>global.workspaceTest.api(route,args),[route,args]);
    const call = (command,args)=>page.evaluate(([command,args])=>
        window.workspace.request({operation:'requestWorkbench',command,args}),[command,args]);
    const artifactCount = async()=>(await api('/api/state')).artifacts.length;

    async function ready() {
        page.on('pageerror',error=>errors.push(error.message));
        await app.evaluate(({BrowserWindow})=>BrowserWindow.getAllWindows().find(w=>w.isVisible()).setSize(1520,1050));
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
    async function waitMessage(reply,exact=true) {
        await page.getByText(reply,{exact}).last().waitFor({state:'visible',...flags});
    }
    async function capture(filename) {
        await page.evaluate(()=>new Promise(resolve=>requestAnimationFrame(()=>requestAnimationFrame(resolve))));
        const png = Buffer.from(await app.evaluate(()=>global.workspaceTest.capture()),'base64');
        await fs.writeFile(path.join(output,filename),png);
        screenshots.push({name:filename,sha256:hash(png)});
    }
    function testReceipt(report) {
        return path.join(project,'receipts',report.artifact.id+'-1.json');
    }

    async function buildRoutingPlan(label,rows,lengths) {
        const old = await page.evaluate(()=>localStorage.getItem('fraynix-request-review'));
        await send('Build a data-to-tool classifier using column reading.');
        await page.locator('#requests-file').waitFor({state:'visible',...flags});
        await page.waitForFunction(prior=>{
            const id=localStorage.getItem('fraynix-request-review');
            return id && id!==prior;
        },old,flags);
        const id = await page.evaluate(()=>localStorage.getItem('fraynix-request-review'));
        await page.locator('#requests-rules-file').setInputFiles({
            name:'ranges.csv',mimeType:'text/csv',buffer:Buffer.from(ranges)});
        await page.waitForFunction(sha=>document.querySelector('#requests-rules-source').textContent.includes(sha),hash(ranges),flags);
        const text = rows.join('\n');
        await page.locator('#requests-file').setInputFiles({name:label+'.csv',mimeType:'text/csv',buffer:Buffer.from(text)});
        await page.waitForFunction(sha=>document.querySelector('#requests-source').textContent.includes(sha),hash(text),flags);
        await page.locator('#requests-confirm').click();
        await page.waitForFunction(()=>document.querySelector('#requests-status').textContent.startsWith('Built an offline tool.'),{},flags);
        assert((await api('/api/requests/verify',{id})).passed);
        const pending = await call('routing-start',{sourceId:id});
        const mapping = 'label,lesson\ntemperature,temperature-export'+(lengths?'\nlength,length-export':'');
        const review = await call('routing-attach',{id:pending.id,base64:Buffer.from(mapping).toString('base64')});
        assert.equal(review.status,'review');
        const completed = await call('run-routing',{id:pending.id,decisionSha256:review.decisionSha256});
        assert.equal(completed.status,'verified');
        return call('export-routing',{id:completed.id});
    }

    async function freshComparison(label,fixture) {
        const earlier = await buildRoutingPlan(label+'-before',fixture.earlier,fixture.lengths);
        const later = await buildRoutingPlan(label+'-after',fixture.later,fixture.lengths);
        const pair = {earlier:earlier.id,later:later.id};
        await send(compare);
        await page.waitForFunction(id=>JSON.parse(localStorage.getItem('fraynix-last-key-comparison')||'{}').later===id,later.id,flags);
        return call('routing-dashboard',{verifyOnly:true,compare:true,pair,keyColumn,exportCsv:true});
    }

    async function boundSources(exportId) {
        const comparisonImport = await exports.comparisonCsv(exportId);
        const {earlier,later} = comparisonImport.receipt.source;
        const [baseline,file] = await Promise.all([exports.routedCsv(earlier.exportId),exports.routedCsv(later.exportId)]);
        return {comparisonImport,baseline,file,keyColumn};
    }

    async function useCase(label,receipt,lesson) {
        const fixture = cases[label];
        const sources = await boundSources(receipt.id);
        const before = await artifactCount();
        await send(use);
        await page.waitForFunction(([key,id])=>localStorage.getItem(key)===id,[pin,receipt.id],flags);
        await waitMessage(useOK);
        assert.equal(await artifactCount(),before+2);
        const summary = await api('/api/requests/routing-dashboard-prepare',{...sources,verifyOnly:true});
        const report = await api('/api/requests/routing-dashboard-prepare',{...sources,explorer:true,explorerLesson:name});
        const artifact = await api('/api/artifact/read',{id:report.artifact.id});
        const proof = JSON.parse(artifact.contents['verification.json']);
        assert.equal(report.comparisonExportId,receipt.id);
        assert.deepEqual(proof.lesson,lesson);
        assert.deepEqual(proof.summary.artifact,summary.artifact);
        assert.deepEqual(proof.import,sources.comparisonImport.receipt);
        assert.deepEqual(proof.views.map(view=>view.groups),fixture.counts);
        assert.deepEqual(proof.views[0].totals,fixture.totals);
        assert.equal(proof.views[1].unit,fixture.unit);
        assert.equal(report.browserChecks,24);
        assert.equal(await app.evaluate(()=>global.workspaceTest.canvasRead('document.querySelector("#visible-count").innerText')),fixture.groups+' groups');

        const folder = path.join(output,label);
        await fs.mkdir(folder);
        for (const [filename,bytes] of Object.entries(artifact.contents))
            await fs.writeFile(path.join(folder,filename==='index.html'?'comparison-explorer.html':filename),bytes);
        await capture(label+'.png');
        return {report,proof,browserReceipt:artifact.test,summary};
    }

    async function refuseRewrittenSource(id) {
        const fp = path.join(project,'exports',id+'.csv');
        const sp = fp+'.receipt.json';
        const mp = path.join(project,'exports','manifest.jsonl');
        const [bytes,receiptText,manifestText] = await Promise.all([fs.readFile(fp),fs.readFile(sp,'utf8'),fs.readFile(mp,'utf8')]);
        const edited = Buffer.from(bytes.toString('utf8').replace('0.00018','0.00019'));
        assert.notDeepEqual(edited,bytes);
        const receipt = JSON.parse(receiptText);
        receipt.sha256=hash(edited);
        receipt.bytes=edited.length;
        try {
            await fs.writeFile(fp,edited);
            await fs.writeFile(sp,JSON.stringify(receipt));
            const manifest = manifestText.trim().split('\n').map(line=>JSON.parse(line));
            await fs.writeFile(mp,manifest.map(entry=>JSON.stringify(entry.id===id?receipt:entry)).join('\n')+'\n');
            await exports.comparisonCsv(id);
            await assert.rejects(call('run-comparison-explorer-lesson',{name,exportId:id,verifyOnly:true}),/disagrees with its verified source/);
        } finally {
            await fs.writeFile(fp,bytes);
            await fs.writeFile(sp,receiptText);
            await fs.writeFile(mp,manifestText);
        }
    }

    try {
        await ready();
        const published = JSON.parse(await fs.readFile('docs/readmes/203/verification.json','utf8'));
        const restored = await call('routing-summary',{exportId:published.report.comparisonExportId,explorer:true,verifyOnly:true});
        assert.deepEqual(restored.artifact,published.report.artifact);
        assert(restored.rechecked);
        await assert.rejects(call('run-comparison-explorer-lesson',{verifyOnly:true,exportId:published.report.comparisonExportId}),
            /Choose a comparison explorer lesson name/);
        await send(teach);
        await waitMessage('Build a verified comparison explorer first');
        await send(summarize);
        await waitMessage('Comparison CSV totals verified against Java. 13 browser checks passed.');
        await send(build);
        await waitMessage('Comparison explorer verified against Java. 24 browser checks passed.');
        const teacherId = await page.evaluate(()=>localStorage.getItem('fraynix-last-comparison-explorer'));
        const teacherSources = await boundSources(teacherId);
        const teacher = await api('/api/requests/routing-dashboard-prepare',{...teacherSources,explorer:true});
        const teacherReceipt = teacherSources.comparisonImport.receipt;
        const lessonPath = path.join(project,'request-workbench','comparison-explorer-lessons',name+'.json');

        await send(teach);
        await waitMessage('Saved comparison explorer lesson '+name+'.');
        const lessonBytes = await fs.readFile(lessonPath);
        const lesson = JSON.parse(lessonBytes).data;
        assert.equal(lesson.name,name);
        assert.equal(lesson.version,1);
        assert.deepEqual(lesson.teacherArtifact,teacher.artifact);
        assert.equal(lesson.teacherCsvSha256,teacherReceipt.sha256);
        const pair = {earlier:teacherReceipt.source.earlier.exportId,later:teacherReceipt.source.later.exportId};
        const copy = await call('routing-dashboard',{verifyOnly:true,compare:true,pair,keyColumn,exportCsv:true});
        assert.notEqual(copy.id,teacherId);
        assert.equal(copy.sha256,teacherReceipt.sha256);
        const beforeRefusals = await artifactCount();
        await send(use);
        await waitMessage('Use a different comparison CSV from the teaching example',false);
        assert.equal(await artifactCount(),beforeRefusals);
        assert.deepEqual(await fs.readFile(lessonPath),lessonBytes);

        await restart();
        const a = await freshComparison('temperature-only',cases['temperature-only']);
        assert.notEqual(a.sha256,lesson.teacherCsvSha256);
        const single = await useCase('temperature-only',a,lesson);
        const b = await freshComparison('mixed-unmatched',cases['mixed-unmatched']);
        assert.notEqual(a.sha256,b.sha256);
        assert.notEqual(b.sha256,lesson.teacherCsvSha256);

        const oldTeacherReceipt = await fs.readFile(testReceipt(teacher));
        const beforeDamaged = await artifactCount();
        try {
            const forged = JSON.parse(oldTeacherReceipt);
            forged.checks[0].actual.text='Edited teaching evidence';
            await fs.writeFile(testReceipt(teacher),JSON.stringify(forged));
            await assert.rejects(call('run-comparison-explorer-lesson',{name}),/original artifact test receipt changed/i);
            assert.equal(await artifactCount(),beforeDamaged);
        } finally { await fs.writeFile(testReceipt(teacher),oldTeacherReceipt); }

        const mixed = await useCase('mixed-unmatched',b,lesson);
        const oldMixedReceipt = await fs.readFile(testReceipt(mixed.report));
        await call('routing-dashboard',{verifyOnly:true,compare:true,pair,keyColumn,exportCsv:true});
        assert.notEqual((await exports.comparisonCsv()).receipt.id,b.id);
        await restart();
        await send(recheck);
        await waitMessage(freshOK);
        assert.equal(await app.evaluate(()=>global.workspaceTest.canvasRead('document.querySelector("#source-id").innerText')),'Export '+b.id);
        assert.deepEqual(await fs.readFile(testReceipt(mixed.report)),oldMixedReceipt);
        const beforeChanged = await artifactCount();
        await refuseRewrittenSource(b.id);
        assert.equal(await artifactCount(),beforeChanged);
        assert.deepEqual(await fs.readFile(lessonPath),lessonBytes);
        assert.deepEqual(await fs.readFile(testReceipt(mixed.report)),oldMixedReceipt);
        assert.deepEqual(errors,[]);

        const browser = await chromium.launch({channel:'msedge'});
        let offlineViews = 0;
        try {
            for (const label of Object.keys(cases)) offlineViews += await offline(browser,path.join(output,label));
        } finally { await browser.close(); }
        assert.equal(offlineViews,30);
        const result = {passed:true,teacher,lesson,runs:{'temperature-only':single,'mixed-unmatched':mixed},offlineViews,screenshots,checks:{
            priorExplorerRechecked:true,lessonSavedFromIndependentTeacher:true,copyOfTeacherRefused:true,
            lessonAvailableAfterRestart:true,twoFreshInputs:true,adaptedBrowserChecks:true,
            zeroNetDistinguishedFromNoMatches:true,changedTeacherRefusedBeforeSaving:true,
            rewrittenCsvRefused:true,recheckedOriginalAfterNewExport:true,originalReceiptsPreserved:true,
            allOfflineViewsAgree:true,offlineDownloadsExact:true}};
        await fs.writeFile(path.join(output,'result.json'),JSON.stringify(result,null,2)+'\n');
        console.log(JSON.stringify({passed:true,name,offlineViews,checks:result.checks}));
    } catch (error) {
        await fs.writeFile(path.join(output,'failure.txt'),error.stack+'\n'+await page.locator('body').innerText()).catch(()=>{});
        throw error;
    } finally { await app.close().catch(()=>{}); }
}
main().catch(error=>{console.error(error);process.exitCode=1;});
