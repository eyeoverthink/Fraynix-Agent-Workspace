'use strict';
const assert = require('node:assert/strict');
const {createHash} = require('node:crypto');
const fs = require('node:fs/promises');
const path = require('node:path');
const {launch} = require('../../workspace-desktop/test/calibration-integration.cjs');

const hash = bytes => createHash('sha256').update(bytes).digest('hex');
const openRouting = 'Route the last verified classification.';
const resumeRouting = 'Resume classification routing.';
const sample = 'id,reading,probe_c,distance_in\nt-1,4,37.25,11\nl-1,12,90,2.5\nt-2,9.9999,-40,9';
const ranges = 'min_inclusive,max_exclusive,label\n0,10,temperature\n10,20,length';
const mapping = 'label,lesson\ntemperature,temperature-check\nlength,length-check';
async function main() {
    const output = path.resolve(process.argv[2] || 'build/edition196-live');
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
        const started = performance.now();
        const buffer = Buffer.from(text);
        await page.locator('#requests-file').setInputFiles({name,mimeType:'text/csv',buffer});
        const digest = hash(buffer);
        await page.waitForFunction(value=>document.querySelector('#requests-source').textContent.includes(value),digest,flags);
        assert.equal((await read(await current())).source.sha256,digest);
        return Math.round(performance.now()-started);
    }
    async function build(id,expectedTitle,expected,attachMs,checkCount=16) {
        const started = performance.now();
        await page.locator('#requests-confirm').click();
        await page.waitForFunction(title=>document.querySelector('#artifact-title').textContent===title
            && document.querySelector('#requests-status').textContent.startsWith('Built an offline tool.'),expectedTitle,flags);
        const proof = await verify(id);
        const buildVerifyMs = Math.round(performance.now()-started);
        assert(proof.passed && proof.output==='index.html');
        assert.equal(await resultText(),expected);
        const artifact = await api('/api/artifact/read',{id:proof.artifact.id});
        const evidence = JSON.parse(artifact.contents['verification.json']);
        assert.equal(hash(artifact.contents['index.html']),evidence.htmlSha256);
        assert.equal(artifact.test.checks.length,checkCount);
        assert(artifact.test.passed && evidence.cases.every(test=>test.java===test.browser));
        return {proof,evidence,receipt:artifact.test,html:artifact.contents['index.html'],
            timing:{rows:evidence.inputCount,columns:evidence.inputHeaders.length,attachMs,buildVerifyMs}};
    }
    async function capture(name) {
        await page.evaluate(()=>new Promise(resolve=>requestAnimationFrame(()=>requestAnimationFrame(resolve))));
        const png = Buffer.from(await app.evaluate(()=>global.workspaceTest.capture()),'base64');
        await fs.writeFile(path.join(output,name),png);
        screenshots.push({name,sha256:hash(png)});
    }
    async function verifyLegacy() {
        if (!prior) return null;
        const old = JSON.parse(await fs.readFile('docs/readmes/195/verification.json'));
        const results = [];
        for (const [id,expected] of [[old.teacherId,old.teacher],[old.learnerId,old.fresh]]) {
            const proof = await verify(id);
            const artifact = await api('/api/artifact/read',{id:proof.artifact.id});
            assert.deepEqual(JSON.parse(artifact.contents['verification.json']),expected);
            assert.equal(hash(artifact.contents['index.html']),expected.htmlSha256);
            assert.equal(artifact.test.checks.length,20);
            results.push({id,sha256:proof.artifact.sha256});
        }
        return results;
    }

    const route = (operation,args) => api('/api/requests/routing-'+operation,args);
    const routingId = () => page.evaluate(()=>localStorage.getItem('fraynix-classification-routing-plan'));
    const encoded = text => Buffer.from(text).toString('base64');
    const routingStatus = () => page.locator('#routing-status');
    const artifactCount = async () => (await api('/api/state')).artifacts.length;
    async function planCount() {
        return (await fs.readdir(path.join(project,'request-workbench'))).filter(name=>name.endsWith('.task.json')).length;
    }
    async function teachTool(command,header,values,outputHeader,expected,title,name) {
        const id = await start(command);
        const ms = await attach('teaching.csv',[header,...values].join('\n'));
        await build(id,title,[header+','+outputHeader,...values.map((v,i)=>v+','+expected[i])].join('\n'),ms);
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

    try {
        await ready();
        const legacy = await verifyLegacy();
        await teachTool('Build a data-to-tool converter from Celsius to Fahrenheit using column probe_c.','probe_c',
            ['0','10'],'fahrenheit',['32','50'],'CSV table · Celsius to Fahrenheit','temperature-check');
        await teachTool('Build a data-to-tool converter from inches to millimetres using column distance_in.','distance_in',
            ['1','10'],'millimetres',['25.4','254'],'CSV table · Inches to millimetres','length-check');

        const classifier = await start('Build a data-to-tool classifier using column reading.');
        await assert.rejects(route('start',{sourceId:classifier}),/verified classifier/);
        await page.locator('#requests-rules-file').setInputFiles({name:'ranges.csv',mimeType:'text/csv',buffer:Buffer.from(ranges)});
        await page.waitForFunction(digest=>document.querySelector('#requests-rules-source').textContent.includes(digest),hash(ranges),flags);
        const sampleMs = await attach('samples.csv',sample);
        const classified = await build(classifier,'CSV table · Range classification',
            'id,reading,probe_c,distance_in,classification\nt-1,4,37.25,11,temperature\nl-1,12,90,2.5,length\nt-2,9.9999,-40,9,temperature',sampleMs,18);
        const artifactBefore = await artifactCount();
        const reviewsBefore = await planCount();
        await openPlan(openRouting);
        const id = await routingId();
        assert.equal((await route('read',{id})).source.reviewId,classifier);
        await page.locator('#routing-file').setInputFiles({name:'incomplete.csv',mimeType:'text/csv',
            buffer:Buffer.from('label,lesson\ntemperature,temperature-check')});
        await page.waitForFunction(()=>document.querySelector('#routing-status').textContent.includes('Missing a procedure mapping for length'),{},flags);
        assert.equal((await route('read',{id})).status,'awaiting-map');
        await assert.rejects(route('attach',{id,base64:encoded(mapping.replace('length-check','unverified-lesson'))}),/No data-to-tool lesson named/);
        await assert.rejects(route('next',{id}),/Confirm the current mapping/);
        assert.equal(await artifactCount(),artifactBefore);
        assert.equal(await planCount(),reviewsBefore);

        await attachMapping(mapping+'\n');
        const old = await route('read',{id});
        const choices = [{label:'temperature',lesson:'temperature-check',inputColumn:'probe_c',min:'0',max:'10'},
            {label:'length',lesson:'length-check',inputColumn:'distance_in',min:'10',max:'20'},
            {label:'temperature',lesson:'temperature-check',inputColumn:'probe_c',min:'0',max:'10'}];
        assert.deepEqual(old.rows.map(({label,lesson,inputColumn,min,max})=>({label,lesson,inputColumn,min,max})),choices);
        await attachMapping(mapping);
        const reviewed = await route('read',{id});
        assert.notEqual(reviewed.decisionSha256,old.decisionSha256);
        await assert.rejects(route('confirm',{id,decisionSha256:old.decisionSha256}),/current classification mapping/);
        await assert.rejects(route('next',{id}),/Confirm the current mapping/);
        assert.equal(await planCount(),reviewsBefore);
        await capture('routing-preview-and-reasons.png');

        await page.locator('#routing-pause').check();
        await page.locator('#routing-run').click();
        await page.getByText('temperature-check · Verified',{exact:true}).waitFor({state:'visible',...flags});
        await page.getByText('length-check · Pending',{exact:true}).waitFor({state:'visible',...flags});
        const paused = await route('read',{id});
        assert.equal(paused.status,'approved');
        assert.equal(paused.steps[0].result.result,'probe_c,fahrenheit\n37.25,99.05\n-40,-40');
        assert(!paused.steps[1].result);
        await assert.rejects(route('attach',{id,base64:encoded(mapping)}),/keeps its original mapping/);
        await capture('routing-one-result-saved.png');
        const firstReceipt = await rejectChangedReceipt(paused.steps[0].result.artifact,()=>route('read',{id}));
        await rejectChangedReceipt(classified.proof.artifact,()=>route('next',{id}));

        const {child} = await route('next',{id});
        assert.equal(child.status,'review');
        await assert.rejects(route('accept',{id,childId:child.id}),/still needs its browser checks/);
        await restart();
        const scheduled = await route('next',{id});
        assert.equal(scheduled.child.id,child.id);
        assert.deepEqual(scheduled.plan,paused);
        const handoff = await call('build-fray',{id:child.id,reviewSha256:child.reviewSha256});
        assert(handoff.passed);
        const secondReceiptFile = path.join(project,'receipts',handoff.artifact.id+'-1.json');
        const secondReceipt = await fs.readFile(secondReceiptFile);
        assert.deepEqual(await route('read',{id}),paused);

        await restart();
        await openPlan(resumeRouting);
        await page.locator('#routing-run').click();
        await page.getByText('Every scheduled procedure has a verified result.',{exact:true}).waitFor({state:'visible',...flags});
        const done = await route('read',{id});
        assert.deepEqual(done.steps[0],paused.steps[0]);
        assert.equal(done.steps[1].result.childId,child.id);
        assert.deepEqual(done.steps[1].result.artifact,handoff.artifact);
        assert.equal(done.steps[1].result.result,'distance_in,millimetres\n2.5,63.5');
        assert.deepEqual(await fs.readFile(secondReceiptFile),secondReceipt);
        assert.deepEqual(await fs.readFile(path.join(project,'receipts',paused.steps[0].result.artifact.id+'-1.json')),firstReceipt);
        assert.equal(await artifactCount(),artifactBefore+2);
        assert.equal(await planCount(),reviewsBefore+2);
        assert.deepEqual(await verifyLegacy(),legacy);
        await capture('routing-resumed-and-verified.png');
        await page.getByRole('button',{name:'Open length-check'}).click();
        await page.locator('#classification-routing').waitFor({state:'hidden',...flags});
        assert.equal(await resultText(),done.steps[1].result.result);
        assert.deepEqual(errors,[]);

        const result = {passed:true,source:classified.proof,reviewed,paused,done,legacy,screenshots,
            checks:{restartCount:2,unverifiedClassifierRefused:true,missingMappingRefused:true,unknownLessonRefused:true,
                noExecutionBeforeApproval:true,staleApprovalRefused:true,sourceReceiptChangeRefused:true,childReceiptChangeRefused:true,
                resumedSamePendingChild:true,reconciledAlreadyVerifiedChild:true,originalReceiptsPreserved:true,exactlyTwoNewArtifacts:true}};
        await fs.writeFile(path.join(output,'result.json'),JSON.stringify(result,null,2)+'\n');
        await fs.writeFile(path.join(output,'example-mapping.csv'),mapping+'\n');
        console.log(JSON.stringify({passed:true,choices:done.rows.length,steps:done.steps.map(s=>s.name),legacy:legacy?.length,checks:result.checks}));
    } catch (error) {
        await fs.writeFile(path.join(output,'failure.txt'),error.stack+'\n'+await page.locator('body').innerText()).catch(()=>{});
        throw error;
    } finally { await app.close().catch(()=>{}); }
}

main().catch(error=>{console.error(error);process.exitCode=1;});
