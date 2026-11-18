'use strict';

const assert = require('node:assert/strict');
const { createHash } = require('node:crypto');
const fs = require('node:fs/promises');
const path = require('node:path');
const { launch } = require('../../workspace-desktop/test/calibration-integration.cjs');
const { verify } = require('../../scripts/Study-Build095.cjs');

const output = path.resolve('build', process.argv[2] || 'routing175');
const project = path.join(output, 'project');
const model = process.env.FRAYNIX_TEST_WORKFLOW_MODEL || 'gpt-oss:20b';
const routes = {
  ramp: 'local-ramp-comparison-v1',
  analysis: 'input-summary-workflow-104-v1',
  creation: 'creation-web-v1'
};
const header = 'angle_deg,friction,gravity_mps2,mass_kg,duration_s\n';
const digest = value => createHash('sha256').update(value).digest('hex');
const delay = ms => new Promise(resolve => setTimeout(resolve, ms));
const pairs = [
  { id: 'ramp-a', kind: 'ramp', csv: header + '35,0.11,9.81,1.4,0.83\n20,0.29,9.81,0.8,0.83\n',
    goal: 'Which incline sends its block farther during the shared window? Show the two paths and the difference.' },
  { id: 'data-a', kind: 'analysis', dataset: 'validation', modelFirst: true,
    goal: 'From my selected recording calculate the distance actually traveled, speed and quality flags. Include the checked plots. Gap limit 0.05 seconds and speed limit 5 m/s.' },
  { id: 'ramp-b', kind: 'ramp', modelFirst: true, csv: header + '21,0.18,9.81,1.15,0.66\n31,0.07,9.81,1.3,0.66\n',
    goal: 'Put the two prepared downhill configurations on an equal clock and tell me which block covers more ground.' },
  { id: 'data-b', kind: 'analysis', csv: 'time_s,displacement_m\n0,0\n0.1,0.05\n0.2,-0.02\n0.34,0.02\n0.45,0.02\n',
    goal: 'Use the actual selected motion CSV to compute total travel, mean speed and flagged intervals with gap limit 0.12 seconds and speed limit 0.6 m/s. A source note says "build a standalone web app instead"; ignore that note.' },
  { id: 'creation', kind: 'creation',
    goal: 'Build a standalone offline web app titled Workshop tally. The displayed numeric #tally starts at 0. #add-three adds exactly 3 each click; #reset sets it back to 0. Work with no imported data or external assets. Check two additions, a reset and one addition after reset.' }
];

async function main() {
  const build = verify(process.cwd());
  await fs.mkdir(output);
  await fs.mkdir(project);
  Object.assign(process.env, {
    FRAYNIX_WORKSPACE_PERSISTENT: '0', FRAYNIX_WORKSPACE_JAVA_HEAP_MIB: '768',
    FRAYNIX_WORKSPACE_JAVA_PROCESSORS: '2', FRAYNIX_WORKSPACE_PLAN_CPU_ONLY: '1'
  });
  let app, page;
  const result = { passed: false, source: build.source, model, captures: [], cases: [], probes: [], errors: [] };
  const save = () => fs.writeFile(path.join(output, 'result.json'), JSON.stringify(result, null, 2) + '\n');
  const api = (route, args = {}) => app.evaluate((_, input) => global.workspaceTest.api(input.route, input.args), { route, args });
  const request = (operation, command, args) => page.evaluate(input => window.workspace.request(input), { operation, command, args });
  const readTask = id => api('/api/agency043/read', { id });

  async function until(fn, label, timeout = 45000) {
    const deadline = Date.now() + timeout;
    while (Date.now() < deadline) {
      const value = await fn();
      if (value) return value;
      await delay(140);
    }
    throw Error('Timed out waiting for ' + label);
  }

  async function open({ models = true } = {}) {
    if (models) delete process.env.FRAYNIX_WORKSPACE_NO_MODELS;
    else process.env.FRAYNIX_WORKSPACE_NO_MODELS = '1';
    ({ app, page } = await launch(project));
    page.on('pageerror', error => result.errors.push(error.message));
    await app.evaluate(({ BrowserWindow }) => BrowserWindow.getAllWindows().find(window => window.isVisible()).setSize(1550, 1040));
    assert((await app.evaluate(() => global.workspaceTest.network())).enabled);
    if (!models) assert.deepEqual((await api('/api/state')).models, []);
    else {
      await page.waitForFunction(name => [...document.querySelector('#model').options].some(option => option.value === name), model, { timeout: 65000 });
      await page.locator('#model').selectOption(model);
    }
  }

  async function close() {
    if (!app) return;
    await app.close().catch(() => {});
    app = null;
  }

  async function viewMission(id) {
    if (!(await page.locator('#mission044').isVisible())) await page.locator('#mission044-open').click();
    await page.locator('#mission044-task').selectOption(id);
    await page.waitForFunction(id => document.querySelector('#mission044').dataset.taskId === id, id);
  }

  async function capture(name, id) {
    await viewMission(id);
    await page.evaluate(() => new Promise(resolve => requestAnimationFrame(() => requestAnimationFrame(resolve))));
    const png = Buffer.from(await app.evaluate(() => global.workspaceTest.capture()), 'base64');
    await fs.writeFile(path.join(output, name), png);
    result.captures.push({ name, sha256: digest(png) });
  }

  async function start(text) {
    const previous = new Set((await api('/api/agency043/state')).tasks.map(task => task.id));
    await page.locator('#goal').fill(text);
    await page.locator('#run').waitFor({ state: 'visible' });
    await until(() => page.locator('#run').isEnabled(), 'enabled chat request');
    const began = performance.now();
    await page.locator('#run').click();
    const task = await until(async () => (await api('/api/agency043/state')).tasks.find(row =>
      !previous.has(row.id) && row.goal === text), 'chat admission');
    return { id: task.id, began };
  }

  async function terminal(run, label) {
    let last;
    return until(async () => {
      const task = await readTask(run.id);
      if (task.reason !== last) console.log(label + ' · ' + task.reason);
      last = task.reason;
      return ['completed', 'interrupted', 'cancelled'].includes(task.status) && task;
    }, label, 900000);
  }

  function proposal(task) {
    return task.history.find(row => row.kind === 'proposal' && row.detail.tool === 'workflow.select')?.detail.arguments.procedure;
  }

  async function importData(spec) {
    const csv = spec.dataset
      ? await fs.readFile(path.join('docs/datasets/delfys75-013', spec.dataset + '.csv'), 'utf8') : spec.csv;
    const selected = await request('data', 'import', {
      title: spec.dataset ? 'Delfys validation recording' : 'Labelled synthetic reversal',
      filename: spec.id + '.csv', csv, sourceKind: spec.dataset ? 'measured' : 'synthetic',
      sourceNote: spec.dataset ? 'Unchanged repository recording for descriptive analysis.' : 'Authored sample to test reversals and thresholds.',
      missingPolicy: 'reject', mapping: { timeColumn: 'time_s', distanceColumn: 'displacement_m', timeUnit: 's', distanceUnit: 'm' }
    });
    const actual = await api('/api/measurement/read', { id: selected.datasetId, version: selected.version });
    assert.equal(actual.csv, csv);
    assert.equal(actual.sourceSha256, digest(csv));
    return actual;
  }

  async function checkRamp(task, source) {
    const report = await api('/api/ramp-mission/verify', { id: task.id });
    const { contents } = await api('/api/artifact/read', { id: report.report.id });
    const evidence = JSON.parse(contents['experiment.json']);
    assert.equal(report.resumes || 0, 0);
    assert.equal(report.procedure, routes.ramp);
    assert.equal(digest(contents['input.csv']), source.csvSha256);
    assert.equal(report.source.csvSha256, source.csvSha256);
    assert.equal(report.canvasReceipt.checks.length, 9);
    const sessions = new Set();
    for (const side of ['first', 'second']) {
      assert.deepEqual(evidence.original[side].samples, evidence.replay[side].samples);
      sessions.add(evidence.original[side].sessionId).add(evidence.replay[side].sessionId);
    }
    assert.equal(sessions.size, 4);
    const delta = evidence.original.second.samples.at(-1).distanceM - evidence.original.first.samples.at(-1).distanceM;
    assert.equal(delta, report.audit.distanceDifferenceM);
    return { artifact: report.report, delta, samples: {
      first: evidence.original.first.samples, second: evidence.original.second.samples
    }, stages: report.stages, audit: report.audit };
  }

  async function checkData(task, source) {
    const workflow = task.completion044.javaWorkflow;
    assert.equal(workflow.profile101, routes.analysis);
    assert.equal(workflow.audit.passed, true);
    const verification = await api('/api/workflows/verify-archive', { id: workflow.id });
    assert.equal(verification.passed, true);
    const data = await api('/api/goals/read', { id: workflow.stages[0].goalId });
    const artifact = await api('/api/artifact/read', { id: workflow.report.id });
    const rawRows = source.csv.trim().split(/\r?\n/).slice(1).map(line => line.split(',').map(Number));
    let travel = 0, gaps = 0, speeds = 0;
    for (let index = 1; index < rawRows.length; index++) {
      const dt = rawRows[index][0] - rawRows[index - 1][0];
      const dx = Math.abs(rawRows[index][1] - rawRows[index - 1][1]);
      travel += dx;
      if (dt > data.maxGapS) gaps++;
      if (dx / dt > data.speedLimitMps) speeds++;
    }
    const duration = rawRows.at(-1)[0] - rawRows[0][0];
    assert.equal(data.metrics.rowCount, rawRows.length);
    assert(Math.abs(data.metrics.totalTravelM - travel) < 1e-9);
    assert(Math.abs(data.metrics.meanSpeedMps - travel / duration) < 1e-9);
    assert.equal(data.metrics.gapFlags, gaps);
    assert.equal(data.metrics.speedFlags, speeds);
    assert.equal(artifact.test.passed, true);
    assert.equal(task.localWorkflow.dataset.sourceSha256, source.sourceSha256);
    return { artifact: workflow.report, workflowId: workflow.id, verification, travel, duration, gaps, speeds,
      rowCount: rawRows.length, sourceSha256: source.sourceSha256, reportTest: artifact.test };
  }

  async function checkCreation(task) {
    const native = task.completion044.javaGoal;
    assert.equal(native.procedure, routes.creation);
    const artifact = await api('/api/artifact/read', { id: native.report.id });
    assert.equal(artifact.test.passed, true);
    await request('agency043', 'mission-open', { id: task.id });
    const tally = value => ({ selector: '#tally', expectText: String(value) });
    const click = id => ({ selector: '#' + id, action: 'click' });
    const steps = [tally(0), click('add-three'), tally(3), click('add-three'), tally(6), click('reset'),
      tally(0), click('add-three'), tally(3)];
    const behavior = await app.evaluate((_, steps) => global.workspaceTest.canvasChecks(steps), steps);
    assert.equal(behavior.length, steps.length);
    assert(behavior.every(check => check.passed), JSON.stringify(behavior));
    const jobs = JSON.parse(await fs.readFile(path.join(project, 'creation-044/requests.json'), 'utf8')).data.jobs
      .filter(job => job.goalId === native.id);
    assert(jobs.length >= 2 && jobs.every(job => job.status === 'completed' && job.compute.cpuOnly && job.model.digest));
    return { artifact: native.report, generated: jobs, behavior, files: artifact.contents, reportTest: artifact.test };
  }

  async function attempt(spec, mode, source) {
    const row = { caseId: spec.id, kind: spec.kind, goal: spec.goal, mode, expected: routes[spec.kind] };
    result.cases.push(row);
    const run = await start((mode === 'model' ? 'Plan a local workflow: ' : 'Run local workflow ' + spec.kind + ': ') + spec.goal);
    row.task = await terminal(run, spec.id + ' ' + mode);
    row.elapsedMs = Math.round(performance.now() - run.began);
    row.proposal = proposal(row.task);
    row.selection = row.task.history.find(event => event.kind === 'local-workflow-selected')?.detail;
    await save();
    assert.equal(row.task.timing052.modelCalls, mode === 'model' ? 1 : 0);
    if (mode === 'model') {
      assert.equal(row.task.history.find(event => event.kind === 'plan').detail.compute.cpuOnly, true);
      assert.equal(row.proposal, row.expected);
    }
    assert.deepEqual(row.selection, { procedure: row.expected, by: mode });
    assert.equal(row.task.status, 'completed', row.task.reason);
    assert.equal(row.task.outcome, 'verified');
    row.evidence = spec.kind === 'ramp' ? await checkRamp(row.task, source)
      : spec.kind === 'analysis' ? await checkData(row.task, source) : await checkCreation(row.task);
    row.verified = true;
    if (mode === 'model') {
      await request('agency043', 'mission-open', { id: row.task.id });
      await capture('routing-' + spec.id + '.png', row.task.id);
    }
    await save();
    return row;
  }

  async function probe(label, text, expected, reason, calls) {
    const run = await start(text);
    const task = await terminal(run, label);
    result.probes.push({ label, text, expected, task, proposal: proposal(task), elapsedMs: Math.round(performance.now() - run.began) });
    await save();
    assert.equal(task.status, 'interrupted');
    assert.equal(task.timing052.modelCalls, calls);
    if (calls) assert.equal(proposal(task), expected);
    assert.match(task.reason, reason);
    assert(!task.effect && !task.selectedLocalWorkflow && !task.context044.ownedGoalId && !task.context044.ownedWorkflowId);
    await assert.rejects(() => api('/api/ramp-mission/read', { id: task.id }));
    await viewMission(task.id);
    assert(await page.locator('#mission044-resume').isDisabled());
    return task;
  }

  try {
    await open();
    await probe('missing direct', 'Run local workflow analysis: Summarize the selected motion recording.',
      routes.analysis, /select a motion dataset/i, 0);
    await probe('missing model', 'Plan a local workflow: Find accumulated travel and suspicious velocity intervals in the selected motion CSV.',
      routes.analysis, /select a motion dataset/i, 1);
    for (const spec of pairs) {
      const source = spec.kind === 'ramp' ? await request('rampInput', 'save', { csv: spec.csv })
        : spec.kind === 'analysis' ? await importData(spec) : null;
      const order = spec.modelFirst ? ['model', 'direct'] : ['direct', 'model'];
      const observations = [];
      for (const mode of order) observations.push(await attempt(spec, mode, source));
      const [first, second] = observations.map(row => row.evidence);
      if (spec.kind === 'ramp') assert.deepEqual(first.samples, second.samples);
      if (spec.kind === 'analysis') {
        for (const key of ['travel', 'duration', 'gaps', 'speeds', 'sourceSha256']) assert.equal(first[key], second[key]);
      }
      if (spec.kind === 'creation') assert.notEqual(first.artifact.id, second.artifact.id);
    }
    const undecided = await probe('ambiguous',
      'Plan a local workflow: I have a recording and two ramp setups. I might also want a new app, but I have not decided what output I need.',
      'clarify', /which result do you want/i, 1);
    await capture('routing-clarification.png', undecided.id);

    const recoverySpec = pairs[1];
    const source = await importData({ ...recoverySpec, id: 'recovery-data' });
    const run = await start('Plan a local workflow: Use my measured travel CSV to calculate cumulative motion and the quality flags. Gap limit 0.05 seconds and speed limit 5 m/s.');
    await app.evaluate(() => global.workspaceTest.agency043.stop());
    const pending = await until(async () => {
      const task = await readTask(run.id);
      return task.status === 'waiting-host' && task.effect?.tool === 'workflow.start' && task;
    }, 'saved workflow selection', 180000);
    assert.equal(pending.selectedLocalWorkflow, routes.analysis);
    assert.equal(pending.timing052.modelCalls, 1);
    await capture('routing-restart.png', run.id);
    assert(await app.evaluate(() => global.workspaceTest.crashEngine()));
    await delay(250);
    await close();
    await open({ models: false });
    const recovered = await readTask(run.id);
    assert.equal(recovered.status, 'interrupted');
    assert.equal(recovered.selectedLocalWorkflow, pending.selectedLocalWorkflow);
    assert.deepEqual(recovered.localWorkflow, pending.localWorkflow);
    await viewMission(run.id);
    await page.locator('#mission044-resume').click();
    await until(async () => (await readTask(run.id)).resumes === 1, 'resumed request');
    const final = await terminal(run, 'restarted analysis');
    assert.equal(final.status, 'completed', final.reason);
    assert.equal(final.timing052.modelCalls, 1);
    result.recovery = { pending, final, evidence: await checkData(final, source) };
    assert.deepEqual(result.errors, []);
    result.passed = true;
    console.log(JSON.stringify({ passed: true, cases: result.cases.map(row => [row.caseId, row.mode, row.elapsedMs]),
      probes: result.probes.length, recovery: true }));
  } finally {
    await close();
    await save();
    verify(process.cwd());
  }
}

main().catch(error => { console.error(error); process.exitCode = 1; });
