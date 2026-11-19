'use strict';
const assert = require('node:assert/strict');
const {createHash} = require('node:crypto');
const fs = require('node:fs/promises');
const path = require('node:path');
const {launch} = require('../../workspace-desktop/test/calibration-integration.cjs');

const hash = bytes => createHash('sha256').update(bytes).digest('hex');
const openRouting = 'Route the last verified classification.';
const resumeRouting = 'Resume classification routing.';
const headings = 'id,reading,probe_c,inches,note,route_source_row';
const originalRows = [
    'dup,4,37.25,11,"A, north",old-temp',
    'dup,12,90,37.25,same ID,old-length',
    'dup,4,37.25,11,"A, north",old-temp',
    'l-2,16,88,2.5,"First line\nsecond ""quote"" α",old-line',
    't-2,9.9999,-40,9,Plain,old-tail',
];
const sample = [headings,...originalRows].join('\n');
const ranges = 'min_inclusive,max_exclusive,label\n0,10,temperature\n10,20,length';
const mapping = 'label,lesson\ntemperature,temperature-export\nlength,length-export';
const reportFields = ['source_row','classification','lesson','input_column','input_value','input_unit','output_value','output_unit'];
const classifications = ['temperature','length','temperature','length','temperature'];
const outputs = ['99.05','946.15','99.05','63.5','-40'];
const expectedReport = [
    headings+','+reportFields.map(field=>'route_2_'+field).join(','),
    ...originalRows.map((row,i)=>{
        const temperature=classifications[i]==='temperature';
        return row+','+[i+2,classifications[i],temperature?'temperature-export':'length-export',
            temperature?'probe_c':'inches',i===3?'2.5':i===4?'-40':'37.25',
            temperature?'°C':'in',outputs[i],temperature?'°F':'mm'].join(',');
    }),
].join('\n')+'\n';
async function main() {
    const output = path.resolve(process.argv[2] || 'build/edition197-live');
    const project = path.join(output,'project');
    const prior = process.argv[3] ? path.resolve(process.argv[3]) : null;
    await fs.mkdir(output);
    await fs.mkdir(project);
    if (prior) {
        for (const name of ['workspace.fworld','workspace.fworld.previous','artifacts','receipts','request-workbench'])
            await fs.cp(path.join(prior,name),path.join(project,name),{recursive:true});
    }
    Object.assign(process.env,{FRAYNIX_WORKSPACE_NO_MODELS:'1',FRAYNIX_WORKSPACE_PERSISTENT:'0',
        FRAYNIX_WORKSPACE_JAVA_HEAP_MIB:'768',FRAYNIX_WORKSPACE_JAVA_PROCESSORS:'2'});
    let {app,page} = await launch(project);
    const screenshots = [];
    const errors = [];
    const flags = {timeout:45000};
    const api = (route,args) => app.evaluate((_,[route,args])=>global.workspaceTest.api(route,args),[route,args]);
    const read = id => api('/api/requests/read',{id});
    const verify = id => api('/api/requests/verify',{id});
    const call = (command,args) => page.evaluate(([command,args])=>
        window.workspace.request({operation:'requestWorkbench',command,args}),[command,args]);
    const current = () => page.evaluate(()=>localStorage.getItem('fraynix-request-review'));
    const resultText = () => app.evaluate(()=>global.workspaceTest.canvasRead('document.querySelector("#result").textContent'));

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
    async function chat(message) {
        await page.locator('#goal').fill(message);
        await page.locator('#run').click();
    }
    async function start(message) {
        await chat(message);
        await page.locator('#requests-file').waitFor({state:'visible',...flags});
        return current();
    }
    async function attach(name,text) {
        const buffer = Buffer.from(text);
        await page.locator('#requests-file').setInputFiles({name,mimeType:'text/csv',buffer});
        const digest = hash(buffer);
        await page.waitForFunction(value=>document.querySelector('#requests-source').textContent.includes(value),digest,flags);
        assert.equal((await read(await current())).source.sha256,digest);
    }
    async function build(id,expectedTitle,expected,checkCount=16) {
        await page.locator('#requests-confirm').click();
        await page.waitForFunction(title=>document.querySelector('#artifact-title').textContent===title
            && document.querySelector('#requests-status').textContent.startsWith('Built an offline tool.'),expectedTitle,flags);
        const proof = await verify(id);
        assert(proof.passed && proof.output==='index.html');
        assert.equal(await resultText(),expected);
        const artifact = await api('/api/artifact/read',{id:proof.artifact.id});
        const evidence = JSON.parse(artifact.contents['verification.json']);
        assert.equal(hash(artifact.contents['index.html']),evidence.htmlSha256);
        assert.equal(artifact.test.checks.length,checkCount);
        assert(artifact.test.passed && evidence.cases.every(test=>test.java===test.browser));
        return {proof,evidence};
    }
    async function capture(name) {
        await page.evaluate(()=>new Promise(resolve=>requestAnimationFrame(()=>requestAnimationFrame(resolve))));
        const png = Buffer.from(await app.evaluate(()=>global.workspaceTest.capture()),'base64');
        await fs.writeFile(path.join(output,name),png);
        screenshots.push({name,sha256:hash(png)});
    }
    async function verifyLegacy() {
        if (!prior) return null;
        const old = JSON.parse(await fs.readFile('docs/readmes/196/verification.json'));
        const id = old.done.id;
        assert.deepEqual(await route('read',{id}),old.done);
        const report = await route('export',{id});
        assert.equal(report.rows,3);
        assert.equal(report.source.reviewId,old.done.source.reviewId);
        assert.equal(hash(report.csv),report.sha256);
        return {id,sha256:report.sha256,rows:report.rows};
    }

    const route = (operation,args) => api('/api/requests/routing-'+operation,args);
    const routingId = () => page.evaluate(()=>localStorage.getItem('fraynix-classification-routing-plan'));
    const encoded = text => Buffer.from(text).toString('base64');
    const artifactCount = async () => (await api('/api/state')).artifacts.length;
    async function teachTool(command,header,values,outputHeader,expected,title,name,checkCount=16) {
        const id = await start(command);
        await attach('teaching.csv',[header,...values].join('\n'));
        const converted = outputHeader
            ? [header+','+outputHeader,...values.map((v,i)=>v+','+expected[i])].join('\n') : expected.join('\n');
        await build(id,title,converted,checkCount);
        await chat('Teach data-to-tool lesson '+name);
        await page.getByText('Saved lesson '+name+'.',{exact:false}).waitFor({state:'visible',...flags});
    }
    async function attachMapping(text) {
        await page.locator('#routing-file').setInputFiles({name:'mapping.csv',mimeType:'text/csv',buffer:Buffer.from(text)});
        await page.waitForFunction(()=>document.querySelector('#routing-status').textContent.startsWith('Review the choices'),{},flags);
    }
    async function rejectChangedReceipt(ref,check) {
        const file = path.join(project,'receipts',ref.id+'-1.json');
        const original = await fs.readFile(file);
        try {
            const changed = JSON.parse(original);
            changed.checks[1].actual.text='Changed evidence';
            await fs.writeFile(file,JSON.stringify(changed));
            await assert.rejects(check,/original artifact test receipt changed/);
        } finally { await fs.writeFile(file,original); }
        return original;
    }
    async function openPlan(message) {
        await chat(message);
        await page.locator('#classification-routing').waitFor({state:'visible',...flags});
    }

    async function exportFiles() {
        const files = await fs.readdir(path.join(project,'exports')).catch(error=>{
            if(error.code==='ENOENT')return [];
            throw error;
        });
        return files.filter(name=>name.endsWith('.csv'));
    }

    async function refusePartial(id) {
        assert(await page.locator('#routing-export').isHidden());
        await assert.rejects(route('export',{id}),/Complete and verify every routed procedure/);
        await assert.rejects(call('export-routing',{id}),/Complete and verify every routed procedure/);
        assert.deepEqual(await exportFiles(),[]);
    }

    try {
        await ready();
        const legacy = await verifyLegacy();
        await teachTool('Build a data-to-tool converter from Celsius to Fahrenheit using column probe_c.','probe_c',
            ['0','10'],'fahrenheit',['32','50'],'CSV table · Celsius to Fahrenheit','temperature-export');
        await teachTool('Build a data-to-tool converter from inches to millimetres.','inches',
            ['1','10'],null,['25.4 mm','254 mm'],'CSV · Inches to millimetres','length-export',10);

        const classifier = await start('Build a data-to-tool classifier using column reading.');
        await page.locator('#requests-rules-file').setInputFiles({name:'ranges.csv',mimeType:'text/csv',buffer:Buffer.from(ranges)});
        await page.waitForFunction(digest=>document.querySelector('#requests-rules-source').textContent.includes(digest),hash(ranges),flags);
        await attach('samples.csv',sample);
        const classified = await build(classifier,'CSV table · Range classification',
            [headings+',classification',...originalRows.map((row,i)=>row+','+classifications[i])].join('\n'),18);
        const countBefore = await artifactCount();
        await openPlan(openRouting);
        const id = await routingId();
        await refusePartial(id);
        await attachMapping(mapping);
        const review = await route('read',{id});
        assert.deepEqual(review.rows.map(row=>row.row),[2,3,4,5,6]);
        await refusePartial(id);
        await page.locator('#routing-pause').check();
        await page.locator('#routing-run').click();
        await page.getByText('temperature-export · Verified',{exact:true}).waitFor({state:'visible',...flags});
        const paused = await route('read',{id});
        assert.equal(paused.status,'approved');
        assert.equal(paused.steps[0].result.result,'probe_c,fahrenheit\n37.25,99.05\n37.25,99.05\n-40,-40');
        assert(!paused.steps[1].result);
        await refusePartial(id);

        await restart();
        await openPlan(resumeRouting);
        assert.deepEqual(await route('read',{id}),paused);
        await refusePartial(id);
        await capture('partial-plan-after-restart.png');
        await page.locator('#routing-close').click();
        const {child} = await route('next',{id});
        const handoff = await call('build-fray',{id:child.id,reviewSha256:child.reviewSha256});
        assert(handoff.passed);
        const secondReceipt = path.join(project,'receipts',handoff.artifact.id+'-1.json');
        const retainedReceipt = await fs.readFile(secondReceipt);

        await restart();
        await openPlan(resumeRouting);
        await refusePartial(id);
        assert.deepEqual(await route('read',{id}),paused);
        await page.locator('#routing-run').click();
        await page.getByText('Every scheduled procedure has a verified result.',{exact:true}).waitFor({state:'visible',...flags});
        const complete = await route('read',{id});
        assert.equal(complete.steps[1].result.result,'946.15 mm\n63.5 mm');
        assert.equal(complete.steps[1].result.childId,child.id);
        assert.deepEqual(await fs.readFile(secondReceipt),retainedReceipt);
        assert.equal(await artifactCount(),countBefore+2);
        const report = await route('export',{id});
        assert.equal(report.csv,expectedReport);
        assert.equal(report.rows,5);
        assert.equal(report.sha256,hash(expectedReport));
        assert.deepEqual(report.children.map(ref=>ref.artifact),complete.steps.map(step=>step.result.artifact));
        await rejectChangedReceipt(classified.proof.artifact,()=>route('export',{id}));
        await rejectChangedReceipt(complete.steps[1].result.artifact,()=>call('export-routing',{id}));
        assert.deepEqual(await exportFiles(),[]);
        await capture('verified-plan-ready-to-export.png');

        await page.locator('#routing-export').click();
        await page.getByText('Saved 5 verified rows to ',{exact:false}).waitFor({state:'visible',...flags});
        const exported = await exportFiles();
        assert.equal(exported.length,1);
        const csvPath = path.join(project,'exports',exported[0]);
        const csv = await fs.readFile(csvPath);
        assert.equal(csv.toString('utf8'),expectedReport);
        const receipt = JSON.parse(await fs.readFile(csvPath+'.receipt.json'));
        assert.equal(receipt.sha256,report.sha256);
        assert.equal(receipt.source.reportSha256,report.sha256);
        assert.equal(receipt.source.decisionSha256,complete.decisionSha256);
        assert.deepEqual(receipt.source.children,report.children);
        assert.equal(receipt.source.rows,5);
        await capture('verified-csv-saved.png');

        await restart();
        assert.deepEqual(await route('export',{id}),report);
        assert.deepEqual(await fs.readFile(csvPath),csv);
        assert.deepEqual(await exportFiles(),exported);
        assert.deepEqual(await verifyLegacy(),legacy);
        assert.deepEqual(errors,[]);
        const result = {passed:true,complete,legacy,screenshots,export:{sha256:report.sha256,rows:report.rows,
            bytes:receipt.bytes,source:receipt.source},checks:{restartCount:3,unapprovedExportRefused:true,
                partialAfterRestartRefused:true,verifiedButUnacceptedChildRefused:true,mixedUnitsPreserved:true,
                exactDuplicateRowsPreserved:true,originalOrderPreserved:true,headerCollisionPreserved:true,
                classifierReceiptChangeRefused:true,childReceiptChangeRefused:true,oneCsvSaved:true,repeatBytesStable:true}};
        await fs.writeFile(path.join(output,'result.json'),JSON.stringify(result,null,2)+'\n');
        await fs.writeFile(path.join(output,'verified-routed-results.csv'),csv);
        console.log(JSON.stringify({passed:true,rows:report.rows,sha256:report.sha256,legacy:legacy?.rows,checks:result.checks}));
    } catch (error) {
        await fs.writeFile(path.join(output,'failure.txt'),error.stack+'\n'+await page.locator('body').innerText()).catch(()=>{});
        throw error;
    } finally { await app.close().catch(()=>{}); }
}

main().catch(error=>{console.error(error);process.exitCode=1;});
