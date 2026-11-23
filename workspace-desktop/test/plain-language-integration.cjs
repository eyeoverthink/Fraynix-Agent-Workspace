'use strict';

const assert = require('node:assert/strict');
const {createHash} = require('node:crypto');
const fs = require('node:fs/promises');
const os = require('node:os');
const path = require('node:path');
const {launch} = require('./calibration-integration.cjs');

const rampRequest = 'Which of these two ramps will the block go farther down?';
const dataRequest = 'Hey, can you tell me how far the object travelled in the selected motion recording?';
const reviewRequest = 'Read the selected motion recording and give me a checkable report on its travel and speed.';
const creationRequest = 'Could you whip up a little offline browser app that counts clicks and resets?';
const hash = contents => createHash('sha256').update(contents).digest('hex');
const recordTitle = 'Delfys observed motion';
const model = process.env.FRAYNIX_TEST_WORKFLOW_MODEL || 'gpt-oss:20b';

async function until(check, label, timeout = 60000) {
    const deadline = Date.now() + timeout;
    while (Date.now() < deadline) {
        const value = await check();
        if (value) return value;
        await new Promise(resolve => setTimeout(resolve,120));
    }
    throw Error('Timed out waiting for ' + label);
}

async function main() {
    const output = path.resolve(process.argv[2] || 'build/plain-language');
    const useModel = process.argv.includes('--real-model');
    await fs.mkdir(output,{recursive:true});
    const project = await fs.mkdtemp(path.join(os.tmpdir(),'Fraynix plain speech '));
    const args = process.env.FRAYNIX_WORKSPACE_CI_CHROMIUM === '1' && process.platform === 'linux' ? ['--no-sandbox'] : [];
    process.env.FRAYNIX_WORKSPACE_PERSISTENT = '0';
    const report = {passed:false,platform:process.platform,arch:process.arch,model:useModel?model:null,screenshots:[],routes:{},errors:[]};
    let app, page;

    async function open(models = false) {
        if (models) delete process.env.FRAYNIX_WORKSPACE_NO_MODELS;
        else process.env.FRAYNIX_WORKSPACE_NO_MODELS = '1';
        ({app,page} = await launch(project,args));
        page.on('pageerror',error => report.errors.push(error.message));
        await app.evaluate(({BrowserWindow}) => BrowserWindow.getAllWindows().find(window => window.isVisible()).setSize(1520,990));
        if (models) await page.locator('#model').selectOption(model,{timeout:60000});
    }

    async function close() {
        if(app) await app.close();
        app = null;
    }

    const api = (route,args = {}) => app.evaluate((_,input) => global.workspaceTest.api(input.route,input.args),{route,args});
    const request = (operation,command,args) => page.evaluate(input => window.workspace.request(input),{operation,command,args});
    const manual = (operation, command, args) => until(async () => {
        try { return await request(operation,command,args); }
        catch(error) {
            if(!error.message.includes('Wait for the current operation')) throw error;
            return null;
        }
    }, operation + ' ' + command);
    const read = id => api('/api/agency043/read',{id});
    const tasks = async () => (await api('/api/agency043/state')).tasks;
    const idle = () => until(async () => !(await app.evaluate(() => global.workspaceTest.agency043.busy())), 'idle desktop');

    async function send(text) {
        await idle();
        await page.locator('#goal').fill(text);
        await page.locator('#run').click();
    }

    async function ask(text) {
        const old = new Set((await tasks()).map(task => task.id));
        await send(text);
        const added = await until(async () => (await tasks()).find(task => !old.has(task.id) && task.goal === text),text);
        return added.id;
    }

    const terminal = (id, timeout) => until(async () => {
        const task = await read(id);
        return ['completed','interrupted','cancelled'].includes(task.status) && task;
    },id,timeout);

    async function mission(id) {
        if(!(await page.locator('#mission044').isVisible())) await page.locator('#mission044-open').click();
        await page.locator('#mission044-task').selectOption(id);
        await page.waitForFunction(value => document.querySelector('#mission044').dataset.taskId === value,id);
    }

    async function capture(name,id) {
        if(id) await mission(id);
        await page.evaluate(() => new Promise(resolve => requestAnimationFrame(() => requestAnimationFrame(resolve))));
        const bytes = Buffer.from(await app.evaluate(() => global.workspaceTest.capture()),'base64');
        assert(bytes.length > 10000);
        await fs.writeFile(path.join(output,name + '.png'),bytes);
        report.screenshots.push(name + '.png');
    }

    async function checkMotion(task, csv) {
        assert.equal(task.status,'completed',task.reason);
        const result = task.completion044.javaWorkflow;
        assert.equal(result.profile101,'input-summary-workflow-104-v1');
        assert.equal(task.localWorkflow.dataset.sourceSha256,hash(csv));
        assert.equal((await api('/api/workflows/verify-archive',{id:result.id})).passed,true);
        const data = await api('/api/goals/read',{id:result.stages[0].goalId});
        const rows = csv.trim().split(/\r?\n/).slice(1).map(line => line.split(',').map(Number));
        const timeS = rows.at(-1)[0] - rows[0][0];
        let travelM = 0;
        for(let index=1; index<rows.length; index++) travelM += Math.abs(rows[index][1] - rows[index-1][1]);
        assert.equal(data.metrics.rowCount,rows.length);
        assert(Math.abs(data.metrics.totalTravelM - travelM) < 1e-9);
        assert(Math.abs(data.metrics.meanSpeedMps - travelM / timeS) < 1e-9);
        assert(task.summary.includes(travelM.toFixed(3) + ' m of travel'), task.summary);
        assert(task.summary.includes((travelM / timeS).toFixed(3) + ' m/s'), task.summary);
        return {travelM,timeS,meanSpeedMps:travelM/timeS,samples:rows.length,sourceSha256:hash(csv),
            modelCalls:task.timing052.modelCalls,resumes:task.resumes};
    }

    try {
        await open();
        const initial = (await tasks()).length;
        await send('Compare these two ramps, then turn the CSV into JSON.');
        await page.locator('#messages').getByText('What should I do?',{exact:false}).waitFor({timeout:15000});
        assert.equal((await tasks()).length,initial);
        const missing = await terminal(await ask(dataRequest));
        assert.equal(missing.status,'interrupted');
        assert.match(missing.reason,/select a motion dataset/i);
        assert(!missing.selectedLocalWorkflow && !missing.effect);
        const missingRamp = await terminal(await ask(rampRequest));
        assert.equal(missingRamp.status,'interrupted');
        assert.match(missingRamp.reason,/open a saved ramp input/i);
        assert(!missingRamp.selectedLocalWorkflow && !missingRamp.effect);
        const missingModel = await terminal(await ask(creationRequest));
        assert.equal(missingModel.status,'interrupted');
        assert.match(missingModel.reason,/installed local model/i);
        assert(!missingModel.selectedLocalWorkflow && !missingModel.effect);
        report.refusals = ['mixed request','missing recording','missing ramp input','missing local model'];

        const setup = 'angle_deg,friction,gravity_mps2,mass_kg,duration_s\n35,0.11,9.81,1.4,0.83\n20,0.29,9.81,0.8,0.83\n';
        const source = await manual('rampInput','save',{csv:setup});
        const rampId = await ask(rampRequest);
        const ramp = await terminal(rampId);
        assert.equal(ramp.status,'completed',ramp.reason);
        assert.equal(ramp.timing052.modelCalls,0);
        const comparison = await api('/api/ramp-mission/verify',{id:rampId});
        const rampReport = await api('/api/artifact/read',{id:comparison.report.id});
        assert.equal(comparison.procedure,'local-ramp-comparison-v1');
        assert.equal(comparison.source.csvSha256,source.csvSha256);
        assert.equal(hash(rampReport.contents['input.csv']),source.csvSha256);
        assert.equal(comparison.canvasReceipt.checks.length,9);
        const experiment = JSON.parse(rampReport.contents['experiment.json']);
        const distances = {};
        for(const side of ['first','second']){
            assert.deepEqual(experiment.original[side].samples,experiment.replay[side].samples);
            assert.notEqual(experiment.original[side].sessionId,experiment.replay[side].sessionId);
            distances[side] = experiment.original[side].samples.at(-1).distanceM;
        }
        assert.equal(comparison.audit.distanceDifferenceM,distances.second-distances.first);
        report.routes.ramps = {request:rampRequest,distancesM:distances,sourceSha256:source.csvSha256,modelCalls:0,
            browserChecks:comparison.canvasReceipt.checks.length};
        await capture('ramp-comparison',rampId);

        const csv = await fs.readFile(path.join('docs','datasets','delfys75-013','validation.csv'),'utf8');
        const dataset = await manual('data','import',{title:recordTitle,filename:'delfys-validation.csv',csv,
            sourceKind:'measured',sourceNote:'Existing Delft validation motion recording.',
            missingPolicy:'reject',mapping:{timeColumn:'time_s',distanceColumn:'displacement_m',timeUnit:'s',distanceUnit:'m'}});
        const dataId = await ask(dataRequest);
        report.routes.recording = {request:dataRequest,...await checkMotion(await terminal(dataId),csv)};
        await capture('recorded-motion',dataId);

        const fileRequest = 'Turn this CSV into JSON, please.';
        const file = Buffer.from('item,reading\n"bolt, long",001.20\nwasher,\n','utf8');
        await send(fileRequest);
        await page.locator('#requests-file').waitFor({state:'visible'});
        await page.locator('#requests-file').setInputFiles({name:'parts.csv',mimeType:'text/csv',buffer:file});
        await until(() => page.locator('#requests-confirm').isEnabled(),'reviewed file');
        await capture('file-conversion-review');
        await page.locator('#requests-confirm').click();
        await page.locator('#requests-file').waitFor({state:'hidden'});
        const fileId = (await fs.readdir(path.join(project,'request-workbench')))
            .find(name => /^[a-f0-9-]+[.]task[.]json$/.test(name)).replace(/[.]task[.]json$/,'');
        const fileProof = await api('/api/requests/verify',{id:fileId});
        assert.equal(fileProof.passed,true);
        const outputFile = await api('/api/artifact/read',{id:fileProof.artifact.id});
        const outputJson = outputFile.contents[fileProof.output];
        assert.deepEqual(JSON.parse(outputJson),{columns:['item','reading'],rows:[['bolt, long','001.20'],['washer','']]});
        assert.deepEqual(outputFile.sources,['sha256:' + hash(file)]);
        report.routes.file = {request:fileRequest,sourceSha256:hash(file),exportSha256:hash(outputJson),rows:2};
        assert.equal((await api('/api/agency043/verify',{id:dataId})).passed,true);

        if(useModel) {
            await close();
            await open(true);
            await manual('data','select',{id:dataset.datasetId,version:dataset.version});
            const modelId = await ask(reviewRequest);
            const review = await terminal(modelId,240000);
            assert.equal(review.status,'interrupted',review.reason);
            assert.equal(review.localWorkflowReview,'input-summary-workflow-104-v1');
            assert.equal(review.localWorkflow.dataset.sourceSha256,hash(csv));
            assert.equal(review.timing052.modelCalls,1);
            assert(!review.selectedLocalWorkflow && !review.effect);
            assert.match(review.modelDigest, /^(?:sha256:)?[a-f0-9]{64}$/);
            await capture('local-model-review',modelId);
            await close();
            await open();
            const reopened = await read(modelId);
            assert.equal(reopened.localWorkflowReview,review.localWorkflowReview);
            await mission(modelId);
            await page.locator('#mission044-confirm').click();
            await until(async () => (await read(modelId)).history.some(event =>
                event.kind==='local-workflow-selected' && event.detail.by==='confirmed'),'confirmed route');
            const verified = await terminal(modelId);
            report.routes.review = {request:reviewRequest,digest:review.modelDigest,...await checkMotion(verified,csv)};
            assert.equal(verified.timing052.modelCalls,1);

            await close();
            await open(true);
            const counterId = await ask(creationRequest);
            const counter = await terminal(counterId,480000);
            assert.equal(counter.status,'completed',counter.reason);
            const goal = counter.completion044.javaGoal;
            assert.equal(goal.procedure,'creation-web-v1');
            assert.match(counter.summary,/two increments, a reset, and another increment/);
            const saved = await api('/api/artifact/read',{id:goal.report.id});
            assert.equal(saved.test.passed,true);
            const checks = [
                {selector:'#count',expectText:'0'},{selector:'#add',action:'click'},{selector:'#count',expectText:'1'},
                {selector:'#add',action:'click'},{selector:'#count',expectText:'2'},
                {selector:'#reset',action:'click'},{selector:'#count',expectText:'0'},
                {selector:'#add',action:'click'},{selector:'#count',expectText:'1'}
            ];
            assert.equal(saved.test.steps.length,checks.length);
            const results = await app.evaluate((_,checks) => global.workspaceTest.canvasChecks(checks),checks);
            assert.equal(results.length,checks.length);
            assert(results.every(result => result.passed),JSON.stringify(results));
            const jobs = JSON.parse(await fs.readFile(path.join(project,'creation-044','requests.json'),'utf8')).data.jobs
                .filter(job => job.goalId === goal.id);
            assert(jobs.length && jobs.every(job => job.status==='completed' && job.model.digest));
            report.routes.counter = {request:creationRequest,independentChecks:results.length,modelJobs:jobs.length,digest:jobs.at(-1).model.digest};
            await capture('created-counter',counterId);
        } else {
            await close();
            await open();
            assert.equal((await api('/api/agency043/verify',{id:dataId})).passed,true);
            assert.deepEqual(await checkMotion(await read(dataId),csv),{
                travelM:report.routes.recording.travelM,timeS:report.routes.recording.timeS,
                meanSpeedMps:report.routes.recording.meanSpeedMps,samples:report.routes.recording.samples,
                sourceSha256:report.routes.recording.sourceSha256,modelCalls:0,resumes:0
            });
        }
        assert.deepEqual(report.errors,[]);
        report.passed=true;
        console.log(JSON.stringify(report));
    } finally {
        await close();
        await fs.writeFile(path.join(output,'summary.json'),JSON.stringify(report,null,2)+'\n');
    }
}

main().catch(error => {console.error(error);process.exitCode=1;});
