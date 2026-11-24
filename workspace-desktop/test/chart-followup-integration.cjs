'use strict';

const assert = require('node:assert/strict');
const {createHash} = require('node:crypto');
const fs = require('node:fs/promises');
const os = require('node:os');
const path = require('node:path');
const {launch} = require('./calibration-integration.cjs');

const originalMotion = 'How far did it travel in the selected motion recording?';
const originalRamp = 'Which of these two ramps will the block go farther down?';
const chartRequest = 'Make a chart of that.';
const hash = value => createHash('sha256').update(value).digest('hex');

async function until(check, label, timeout = 60000) {
    const deadline = Date.now() + timeout;
    while (Date.now() < deadline) {
        const value = await check();
        if (value) return value;
        await new Promise(resolve => setTimeout(resolve,100));
    }
    throw Error('Timed out waiting for ' + label);
}

async function main() {
    const output = path.resolve(process.argv[2] || 'build/chart-followup');
    await fs.mkdir(output,{recursive:true});
    const project = await fs.mkdtemp(path.join(os.tmpdir(),'Fraynix chart followup '));
    const flags = process.env.FRAYNIX_WORKSPACE_CI_CHROMIUM === '1' && process.platform === 'linux' ? ['--no-sandbox'] : [];
    process.env.FRAYNIX_WORKSPACE_NO_MODELS = '1';
    process.env.FRAYNIX_WORKSPACE_PERSISTENT = '0';
    const evidence = {passed:false,platform:process.platform,arch:process.arch,screenshots:[],errors:[]};
    const csv = await fs.readFile(path.join('docs','datasets','delfys75-013','validation.csv'),'utf8');
    let app, page;

    async function open({compact = false} = {}) {
        ({app,page} = await launch(project,flags));
        page.on('pageerror',error => evidence.errors.push(error.message));
        await app.evaluate(({BrowserWindow},compact) => {
            const window = BrowserWindow.getAllWindows().find(window => window.isVisible());
            window.setSize(compact ? 1100 : 1500, compact ? 760 : 920);
        },compact);
        if (compact) await page.waitForFunction(() => window.innerHeight <= 850);
    }

    async function close() {
        if (app) await app.close();
        app = null;
    }

    const api = (route, args) => app.evaluate((_,input) => global.workspaceTest.api(input.route,input.args),{route,args});
    const read = id => api('/api/agency043/read',{id});
    const chart = (command, args = {}) => api('/api/reference-chart/' + command,args);
    const chartFacts = state => ({id:state.id,decisionSha256:state.decisionSha256});
    const artifacts = async () => (await api('/api/state')).artifacts;
    const idle = () => until(async () => !(await app.evaluate(() => global.workspaceTest.agency043.busy())), 'idle desktop');
    const fromHost = (operation,command,args) => until(async () => {
        try { return await page.evaluate(input => window.workspace.request(input), {operation,command,args}); }
        catch (error) {
            if (!error.message.includes('Wait for the current operation')) throw error;
            return null;
        }
    }, operation + ' ' + command);

    async function capture(name) {
        await page.evaluate(() => new Promise(resolve => requestAnimationFrame(() => requestAnimationFrame(resolve))));
        const png = Buffer.from(await app.evaluate(() => global.workspaceTest.capture()),'base64');
        assert(png.length > 10000);
        await fs.writeFile(path.join(output,name + '.png'),png);
        evidence.screenshots.push(name + '.png');
    }

    async function send(text) {
        await idle();
        await page.locator('#goal').fill(text);
        await page.locator('#run').click();
    }

    async function complete(text) {
        const before = new Set((await api('/api/agency043/state',{})).tasks.map(task => task.id));
        await send(text);
        const next = await until(async () => (await api('/api/agency043/state',{})).tasks.find(task =>
            !before.has(task.id) && task.goal === text && ['completed','interrupted','cancelled'].includes(task.status)),text);
        assert.equal(next.status,'completed',next.reason);
        assert.equal(next.outcome,'verified');
        await idle();
        return read(next.id);
    }

    async function importMotion(title, value, kind) {
        return fromHost('data','import',{
            title,filename:title.toLowerCase().replaceAll(' ','-')+'.csv',csv:value,sourceKind:kind,
            sourceNote:'Edition 211 reference-resolution test input.',missingPolicy:'reject',
            mapping:{timeColumn:'time_s',distanceColumn:'displacement_m',timeUnit:'s',distanceUnit:'m'}
        });
    }

    async function waitChart(previous) {
        return until(async () => {
            const state = await chart('state');
            return state.id !== previous && state.status === 'verified' && state;
        },'saved chart');
    }

    async function browserDocument() {
        return app.evaluate(() => global.workspaceTest.canvasRead("(() => {const svg=document.getElementById('chart-svg');return svg && {title:document.querySelector('h1').textContent,origin:document.getElementById('chart-origin').textContent,lines:[...svg.querySelectorAll('polyline')].map(line=>line.getAttribute('points')),visible:svg.getBoundingClientRect().height>0};})()"));
    }

    async function chartDocument(state, title) {
        const verified = await chart('prepare',chartFacts(state));
        assert.equal(verified.browserChecks,6);
        const artifact = await api('/api/artifact/read',{id:verified.artifact.id});
        assert.equal(hash(artifact.contents['index.html']),verified.htmlSha256);
        assert.equal(artifact.title,title);
        assert.equal(artifact.test.passed,true);
        const source = JSON.parse(artifact.contents['source.json']);
        assert.equal(source.binding.taskId,state.selected);
        assert.equal(source.binding.requestId,state.id);
        assert.equal(artifact.sources[0],'sha256:' + source.binding.sourceSha256);
        assert.equal(artifact.sources[1],'sha256:' + source.binding.evidenceSha256);
        const visible = await until(async () => {
            const doc = await browserDocument();
            return doc?.title===title && doc.visible && doc;
        },title);
        assert.deepEqual(visible.lines,source.projection.lines);
        return {id:artifact.id,version:artifact.version,source,origin:visible.origin,checks:verified.browserChecks};
    }

    function verifyProjection(source) {
        const traces = source.series.map(series => series.samples);
        const points = traces.flat();
        const times = points.map(row => row.timeS);
        const distances = points.map(row => row.distanceM);
        const minT = Math.min(...times), maxT = Math.max(...times);
        const minY = Math.min(0,...distances), maxY = Math.max(0,...distances);
        const valueRange = maxY === minY ? .001 : maxY-minY;
        const plotted = source.projection.lines.map(line => line.split(' ').map(pair => pair.split(',').map(Number)));
        for (const [index,samples] of traces.entries()) {
            assert.equal(samples.length,plotted[index].length);
            for (const [row,sample] of samples.entries()) {
                const [x,y] = plotted[index][row];
                assert(Math.abs((x-92)/616 - (sample.timeS-minT)/(maxT-minT)) < 0.000001);
                assert(Math.abs((265-y)/224 - (sample.distanceM-minY)/valueRange) < 0.000003);
            }
        }
        return traces.map(trace=>trace.length);
    }

    try {
        await open();
        await send(chartRequest);
        await page.locator('#messages').getByText('Finish a verified motion summary',{exact:false}).waitFor();
        assert.equal((await chart('state')).status,'none');
        evidence.noSourceRefused = true;

        await importMotion('Delfys observed motion',csv,'measured');
        const motion = await complete(originalMotion);
        await send(chartRequest);
        const single = await waitChart();
        const recorded = await chartDocument(single,'Recorded motion chart');
        assert.equal(single.choices.length,1);
        assert.equal(single.selected,motion.id);
        assert.equal(recorded.origin,'Recorded motion');
        assert.equal(recorded.source.binding.sourceSha256,hash(csv));
        const original = csv.trim().split(/\r?\n/).slice(1).map(line => line.split(',').slice(0,2).map(Number));
        assert.deepEqual(recorded.source.series[0].samples.map(row => [row.timeS,row.distanceM]),original);
        evidence.motion = {samples:verifyProjection(recorded.source),sha256:hash(csv),browserChecks:recorded.checks};
        await capture('recorded-chart');

        const catalog = await api('/api/agency043/history',{});
        const archived = await api('/api/agency043/archive',{planSha256:catalog.planSha256,ids:[motion.id]});
        assert(archived.archivedNow.includes(motion.id));
        const reopenedArchive = await chart('prepare',chartFacts(single));
        assert.equal(reopenedArchive.artifact.id,recorded.id);
        evidence.archivedSourceReverified = true;

        const rampCsv = 'angle_deg,friction,gravity_mps2,mass_kg,duration_s\n35,0.11,9.81,1.4,0.83\n20,0.29,9.81,0.8,0.83\n';
        await fromHost('rampInput','save',{csv:rampCsv});
        const ramp = await complete(originalRamp);
        const countBeforeQuestion = (await artifacts()).length;
        await send('Could you make me a chart of that?');
        await page.locator('#chart-request-dialog').waitFor({state:'visible'});
        await page.getByRole('heading',{name:'Which verified result should I chart?'}).waitFor();
        const pending = await chart('state');
        assert.equal(pending.status,'choose');
        assert.equal(pending.selected,'');
        assert.deepEqual(pending.choices.map(row=>row.taskId),[ramp.id,motion.id]);
        assert.equal((await artifacts()).length,countBeforeQuestion);
        assert(await page.locator('#chart-request-submit').isDisabled());
        await assert.rejects(chart('prepare',chartFacts(pending)),/Choose which verified result/);
        await capture('needs-source-choice');
        await page.locator('#chart-request-close').click();

        await importMotion('A later result','time_s,displacement_m\n0,0\n0.1,-0.04\n0.2,0\n','synthetic');
        const later = await complete(originalMotion);
        await close();
        await open({compact:true});

        await page.locator('#chart-request-open').click();
        await page.locator('#chart-request-dialog').waitFor({state:'visible'});
        assert.deepEqual(await chart('state'),pending);
        await assert.rejects(chart('choose',{...chartFacts(pending),taskId:later.id}),/not offered/);
        await page.locator('#chart-request-source').selectOption(ramp.id);
        await capture('choice-after-restart');
        // Persist the chosen reference before starting any rendering, then restart again.
        const chosen = await fromHost('chartFollowup','choose',{...chartFacts(pending),taskId:ramp.id});
        assert.equal(chosen.status,'ready');
        await close();
        await open();

        await page.locator('#chart-request-open').click();
        await page.locator('#chart-request-dialog').waitFor({state:'visible'});
        assert.deepEqual(await chart('state'),chosen);
        assert.equal(await page.locator('#chart-request-source').inputValue(),ramp.id);
        assert(await page.locator('#chart-request-source').isDisabled());
        await assert.rejects(chart('choose',{...chartFacts(chosen),taskId:motion.id}),/already bound/);
        const prep = await chart('prepare',chartFacts(chosen));
        const artifactCount = (await artifacts()).length;
        await assert.rejects(chart('finish',{...chartFacts(chosen),htmlSha256:prep.htmlSha256,checks:[],plot:{}}),
            /browser chart differs/);
        assert.equal((await artifacts()).length,artifactCount);
        await page.locator('#chart-request-submit').click();
        const built = await waitChart(single.id);
        const simulated = await chartDocument(built,'Simulated ramp comparison chart');
        assert.equal(simulated.origin,'Simulated data');
        assert.equal(simulated.source.binding.sourceSha256,hash(rampCsv));
        const parent = await api('/api/artifact/read',{id:ramp.completion044.rampMission.report.id});
        const experiment = JSON.parse(parent.contents['experiment.json']);
        assert.deepEqual(simulated.source.series.map(series=>series.samples),
            [experiment.original.first.samples,experiment.original.second.samples]);
        evidence.ramp = {samples:verifyProjection(simulated.source),sha256:hash(rampCsv),browserChecks:simulated.checks};
        evidence.recovery = {choicesReopened:pending.choices.length,compactWindowReopened:true,unofferedSourceRefused:true,selectedSourceReopened:true,
            plottedEvidenceRequired:true,noExtraArtifactBeforeBrowserProof:true};
        await capture('simulated-chart');

        await close();
        await open();
        await page.locator('#chart-request-open').click();
        await page.locator('#chart-request-submit').click();
        assert.deepEqual(await chartDocument(built,'Simulated ramp comparison chart'),simulated);
        assert.equal((await artifacts()).length,artifactCount+1);
        assert((await page.locator('#messages').innerText()).includes('rechecked the original source'));
        evidence.recovery.reopenedSameArtifact = true;

        // Corrupt only the isolated fixture's saved original report. No other project is touched.
        await api('/api/artifact/write',{id:parent.id,expectedVersion:parent.version,title:parent.title,
            files:{'index.html':'<!doctype html><title>Changed original report</title>'}});
        await page.locator('#chart-request-open').click();
        await page.locator('#chart-request-submit').click();
        await until(async () => (await page.locator('#chart-request-status').textContent()).includes('The chosen source could not be reverified'),
            'changed source refusal');
        assert.equal((await chart('state')).selected,ramp.id);
        assert.equal((await artifacts()).length,artifactCount+1);
        evidence.changedSourceRefused = true;
        await capture('changed-source-refusal');
        assert.deepEqual(evidence.errors,[]);
        evidence.passed = true;
        console.log(JSON.stringify(evidence));
    } finally {
        await close();
        await fs.writeFile(path.join(output,'summary.json'),JSON.stringify(evidence,null,2)+'\n');
    }
}

main().catch(error => {console.error(error);process.exitCode=1;});
