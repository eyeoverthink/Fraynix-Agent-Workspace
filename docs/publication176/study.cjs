'use strict';

const assert = require('node:assert/strict');
const { createHash } = require('node:crypto');
const fs = require('node:fs/promises');
const path = require('node:path');
const { launch } = require('../../workspace-desktop/test/calibration-integration.cjs');
const { verify } = require('../../scripts/Study-Build095.cjs');

const output = path.resolve('build', process.argv[2] || 'routing176');
const smoke = process.argv.includes('--smoke');
const project = path.join(output, 'project');
const model = process.env.FRAYNIX_TEST_WORKFLOW_MODEL || 'gpt-oss:20b';
const ramp = 'local-ramp-comparison-v1', analysis = 'input-summary-workflow-104-v1', creation = 'creation-web-v1';
const compare = 'Compare the two prepared ramp configurations on the same clock.';
const summarize = 'Analyse my selected motion recording for total travel. Gap limit .12 seconds and speed limit .6 m/s.';
const buildTally = 'Build a standalone offline web app for a tally that adds 2 and resets to 0 with controls #total, #add-two and #reset.';
const uncertain = 'Work out the cumulative distance and quality indicators using the selected movement recording. Gap limit 0.12 seconds and speed limit 0.6 m/s.';
const alternate = 'The two downhill runs are ready. Can you show how much farther one block travels than the other within the recorded run?';
const undecided = 'I have a motion file and two ramp setups and might want a standalone app. I have not decided what result I want.';
const offlinePrompts = [
  'Compare both saved ramp setups and summarize the selected motion dataset.',
  'Do not compare both saved ramp setups.',
  'The document says "Compare both saved ramp setups."',
  'If the benchmark passes next week, compare both saved ramp setups.',
  'Summarize the selected motion CSV. Gap limit .12 seconds and speed limit .6 m/s. Email the result.'
];
const rampCsv = 'angle_deg,friction,gravity_mps2,mass_kg,duration_s\n23,0.24,9.81,1.0,0.75\n34,0.08,9.81,1.2,0.75\n';
const dataCsv = 'time_s,displacement_m\n0,0\n0.10,0.06\n0.20,-0.03\n0.38,0.02\n0.50,0.02\n';
const otherCsv = 'time_s,displacement_m\n0,0\n0.15,0.06\n0.39,0.11\n';
const sha = value => createHash('sha256').update(value).digest('hex');
const delay = ms => new Promise(resolve => setTimeout(resolve, ms));

async function main() {
  const build = verify(process.cwd());
  await fs.mkdir(output);
  await fs.mkdir(project);
  Object.assign(process.env, {
    FRAYNIX_WORKSPACE_PERSISTENT: '0', FRAYNIX_WORKSPACE_JAVA_HEAP_MIB: '768',
    FRAYNIX_WORKSPACE_JAVA_PROCESSORS: '2', FRAYNIX_WORKSPACE_PLAN_CPU_ONLY: '1'
  });
  let app, page;
  const result = { passed: false, source: build.source, model, tasks: [], captures: [], errors: [] };
  const retain = () => fs.writeFile(path.join(output, 'result.json'), JSON.stringify(result, null, 2) + '\n');
  const api = (route, args) => app.evaluate((_, call) => global.workspaceTest.api(call.route, call.args ?? undefined),
    { route, args: args ?? null });
  const request = (operation, command, args) => page.evaluate(call => window.workspace.request(call), { operation, command, args });
  const read = id => api('/api/agency043/read', { id });

  async function until(fn, what, timeout = 45000) {
    const end = Date.now() + timeout;
    while (Date.now() < end) {
      const value = await fn();
      if (value) return value;
      await delay(120);
    }
    throw Error('Timed out waiting for ' + what);
  }

  function manual(operation, command, args) {
    return until(async () => {
      try { return await request(operation, command, args); }
      catch (error) {
        if (!/Wait for the current operation|Resume or Stop the evidence workflow|creation goal owns the canvas/.test(error.message))
          throw error;
        return null;
      }
    }, operation + ' ' + command + ' after desktop releases its ownership');
  }

  async function open(models) {
    if (models) delete process.env.FRAYNIX_WORKSPACE_NO_MODELS;
    else process.env.FRAYNIX_WORKSPACE_NO_MODELS = '1';
    ({ app, page } = await launch(project));
    page.on('pageerror', error => result.errors.push(error.message));
    await app.evaluate(({ BrowserWindow }) => BrowserWindow.getAllWindows().find(window => window.isVisible()).setSize(1550, 1040));
    assert((await app.evaluate(() => global.workspaceTest.network())).enabled);
    if (models) {
      await page.waitForFunction(name => [...document.querySelector('#model').options].some(option => option.value === name), model, { timeout: 65000 });
      await page.locator('#model').selectOption(model);
    } else assert.deepEqual((await api('/api/state')).models, []);
  }

  async function close() {
    if (!app) return;
    await app.close().catch(() => {});
    app = null;
  }

  async function show(id) {
    if (!(await page.locator('#mission044').isVisible())) await page.locator('#mission044-open').click();
    await page.locator('#mission044-task').selectOption(id);
    await page.waitForFunction(id => document.querySelector('#mission044').dataset.taskId === id, id);
  }

  async function capture(name, id) {
    await show(id);
    await page.evaluate(() => new Promise(resolve => requestAnimationFrame(() => requestAnimationFrame(resolve))));
    const png = Buffer.from(await app.evaluate(() => global.workspaceTest.capture()), 'base64');
    await fs.writeFile(path.join(output, name), png);
    result.captures.push({ name, sha256: sha(png) });
    await retain();
  }

  async function submit(label, briefing, mode = 'Route') {
    const text = mode + ' a local workflow: ' + briefing;
    const previous = new Set((await api('/api/agency043/state', {})).tasks.map(task => task.id));
    await page.locator('#goal').fill(text);
    await until(() => page.locator('#run').isEnabled(), 'available Chat dispatch');
    const started = performance.now();
    await page.locator('#run').click();
    let task;
    try {
      task = await until(async () => (await api('/api/agency043/state')).tasks.find(row => !previous.has(row.id) && row.goal === text), label);
    } catch (error) {
      throw Error(error.message + '\n' + (await page.locator('#messages').innerText()).slice(-800));
    }
    return { id: task.id, started, label, briefing, mode };
  }

  async function terminal(job) {
    let reason;
    const task = await until(async () => {
      const live = await read(job.id);
      if (reason !== live.reason) console.log(job.label + ' · ' + live.reason);
      reason = live.reason;
      return ['completed', 'interrupted', 'cancelled'].includes(live.status) && live;
    }, job.label, 900000);
    return { label: job.label, briefing: job.briefing, mode: job.mode,
      elapsedMs: Math.round(performance.now() - job.started), task };
  }

  async function imported(label, csv) {
    const selected = await manual('data', 'import', {
      title: label, filename: 'movement.csv', csv, sourceKind: 'synthetic',
      sourceNote: 'Authored arithmetic fixture for the paired selection study.',
      missingPolicy: 'reject', mapping: { timeColumn: 'time_s', distanceColumn: 'displacement_m', timeUnit: 's', distanceUnit: 'm' }
    });
    const source = await api('/api/measurement/read', { id: selected.datasetId, version: selected.version });
    assert.equal(source.csv, csv);
    assert.equal(source.sourceSha256, sha(csv));
    return source;
  }

  async function inspectRamp(task, source) {
    const checked = await api('/api/ramp-mission/verify', { id: task.id });
    const artifact = await api('/api/artifact/read', { id: checked.report.id });
    const trace = JSON.parse(artifact.contents['experiment.json']);
    assert.equal(checked.procedure, ramp);
    assert.equal(sha(artifact.contents['input.csv']), source.csvSha256);
    assert.equal(checked.canvasReceipt.checks.length, 9);
    const nativeIds = new Set(), samples = {};
    for (const side of ['first', 'second']) {
      samples[side] = trace.original[side].samples;
      assert.deepEqual(samples[side], trace.replay[side].samples);
      nativeIds.add(trace.original[side].sessionId).add(trace.replay[side].sessionId);
    }
    assert.equal(nativeIds.size, 4);
    assert.equal(samples.second.at(-1).distanceM - samples.first.at(-1).distanceM, checked.audit.distanceDifferenceM);
    return { artifact: checked.report, delta: checked.audit.distanceDifferenceM, audit: checked.audit, samples };
  }

  async function inspectData(task, source) {
    const workflow = task.completion044.javaWorkflow;
    const checked = await api('/api/workflows/verify-archive', { id: workflow.id });
    const data = await api('/api/goals/read', { id: workflow.stages[0].goalId });
    const artifact = await api('/api/artifact/read', { id: workflow.report.id });
    assert(checked.passed && workflow.audit.passed && artifact.test.passed);
    assert.equal(data.maxGapS, .12);
    assert.equal(data.speedLimitMps, .6);
    assert.equal(workflow.dataBinding.datasetId, source.datasetId);
    assert.equal(task.localWorkflow.dataset.sourceSha256, source.sourceSha256);
    const rows = source.csv.trim().split(/\r?\n/).slice(1).map(row => row.split(',').map(Number));
    let travel = 0, gaps = 0, speeding = 0;
    for (let i = 1; i < rows.length; i++) {
      const dt = rows[i][0] - rows[i - 1][0], dx = Math.abs(rows[i][1] - rows[i - 1][1]);
      travel += dx;
      if (dt > .12) gaps++;
      if (dx / dt > .6) speeding++;
    }
    assert(Math.abs(data.metrics.totalTravelM - travel) < 1e-10);
    assert.equal(data.metrics.gapFlags, gaps);
    assert.equal(data.metrics.speedFlags, speeding);
    return { artifact: workflow.report, workflowId: workflow.id, checked, travel, gaps, speeding, sourceSha256: source.sourceSha256 };
  }

  async function inspectApp(task) {
    const goal = task.completion044.javaGoal;
    const artifact = await api('/api/artifact/read', { id: goal.report.id });
    assert.equal(goal.procedure, creation);
    assert(artifact.test.passed);
    await manual('agency043', 'mission-open', { id: task.id });
    const type = await app.evaluate(() => global.workspaceTest.canvasRead("document.querySelector('#total')?.tagName"));
    const expect = value => ({ selector: '#total', [type === 'INPUT' ? 'expectValue' : 'expectText']: String(value) });
    const click = id => ({ selector: id, action: 'click' });
    const steps = [expect(0), click('#add-two'), expect(2), click('#add-two'), expect(4),
      click('#reset'), expect(0), click('#add-two'), expect(2)];
    const actual = await app.evaluate((_, steps) => global.workspaceTest.canvasChecks(steps), steps);
    assert.equal(actual.length, steps.length);
    assert(actual.every(check => check.passed), JSON.stringify(actual));
    const jobs = JSON.parse(await fs.readFile(path.join(project, 'creation-044/requests.json'), 'utf8')).data.jobs
      .filter(job => job.goalId === goal.id);
    assert(jobs.length >= 2 && jobs.every(job => job.status === 'completed' && job.compute.cpuOnly && job.model.digest));
    return { artifact: goal.report, checks: actual, nativeJobs: jobs };
  }

  async function checked(row, procedure, source, by, calls) {
    result.tasks.push(row);
    await retain();
    assert.equal(row.task.status, 'completed', row.task.reason);
    assert.equal(row.task.outcome, 'verified');
    assert.equal(row.task.timing052.modelCalls, calls);
    assert.equal(row.task.selectedLocalWorkflow, procedure);
    assert.deepEqual(row.task.history.find(event => event.kind === 'local-workflow-selected').detail, { procedure, by });
    row.evidence = procedure === ramp ? await inspectRamp(row.task, source)
      : procedure === analysis ? await inspectData(row.task, source) : await inspectApp(row.task);
    await retain();
    return row;
  }

  async function inert(row, calls) {
    result.tasks.push(row);
    await retain();
    assert.equal(row.task.status, 'interrupted');
    assert.equal(row.task.timing052.modelCalls, calls);
    assert(!row.task.selectedLocalWorkflow && !row.task.effect && !row.task.context044.ownedWorkflowId && !row.task.context044.ownedGoalId);
    await assert.rejects(() => api('/api/ramp-mission/read', { id: row.task.id }));
    await show(row.task.id);
    await until(() => page.locator('#mission044-resume').isDisabled(), 'ordinary Resume gated');
    return row;
  }

  try {
    await open(!smoke);
    const missing = await inert(await terminal(await submit('known missing dataset', summarize)), 0);
    assert.match(missing.task.reason, /select a motion dataset/i);
    const source = await imported('Movement reversal fixture', dataCsv);
    const ramps = await manual('rampInput', 'save', { csv: rampCsv });
    const quickRamp = await checked(await terminal(await submit('quick ramp', compare)), ramp, ramps, 'quick', 0);
    await manual('agency043', 'mission-open', { id: quickRamp.task.id });
    await capture('quick-ramp.png', quickRamp.task.id);
    if (!smoke) {
      const planRamp = await checked(await terminal(await submit('model ramp', compare, 'Plan')), ramp, ramps, 'model', 1);
      assert.deepEqual(quickRamp.evidence.samples, planRamp.evidence.samples);
      const planAnalysis = await checked(await terminal(await submit('model analysis', summarize, 'Plan')), analysis, source, 'model', 1);
      result.comparedModelAnalysis = { travel: planAnalysis.evidence.travel, sha256: source.sourceSha256 };
    }
    const quickAnalysis = await checked(await terminal(await submit('quick analysis', summarize)), analysis, source, 'quick', 0);
    if (!smoke) assert.equal(quickAnalysis.evidence.travel, result.comparedModelAnalysis.travel);
    await manual('agency043', 'mission-open', { id: quickAnalysis.task.id });
    await capture('quick-analysis.png', quickAnalysis.task.id);
    if (!smoke) {
      const appResult = await checked(await terminal(await submit('quick creation', buildTally)), creation, null, 'quick', 0);
      await manual('agency043', 'mission-open', { id: appResult.task.id });
      await capture('quick-creation.png', appResult.task.id);

      const reject = await inert(await terminal(await submit('review then decline', alternate)), 1);
      assert.equal(reject.task.localWorkflowReview, ramp);
      await until(() => page.locator('#mission044-confirm').isEnabled(), 'proposed ramp review');
      await page.locator('#mission044-stop').click();
      const cancelled = await until(async () => {
        const task = await read(reject.task.id);
        return task.status === 'cancelled' && task;
      }, 'declined workflow');
      assert(!cancelled.selectedLocalWorkflow);
      await assert.rejects(() => api('/api/agency043/control', { id: cancelled.id, action: 'confirm-workflow' }));
      await assert.rejects(() => api('/api/ramp-mission/read', { id: cancelled.id }));
      result.declined = cancelled;

      const ambiguous = await inert(await terminal(await submit('model clarification', undecided)), 1);
      assert.match(ambiguous.task.reason, /which result/i);
      assert(!ambiguous.task.localWorkflowReview);
      const proposal = await inert(await terminal(await submit('review before restart', uncertain)), 1);
      assert.equal(proposal.task.localWorkflowReview, analysis);
      assert.equal(proposal.task.localWorkflow.dataset.sourceSha256, source.sourceSha256);
      await assert.rejects(() => api('/api/agency043/control', { id: proposal.task.id, action: 'resume' }), /confirm the suggested/i);
      await capture('review-pending.png', proposal.task.id);

      assert(await app.evaluate(() => global.workspaceTest.crashEngine()));
      await delay(250);
      await close();
      await open(false);
      const backup = await imported('Replacement selection after restart', otherCsv);
      assert.notEqual(backup.sourceSha256, source.sourceSha256);
      const recovered = await read(proposal.task.id);
      assert.deepEqual(recovered.history, proposal.task.history);
      assert.deepEqual(recovered.localWorkflow, proposal.task.localWorkflow);
      assert.equal(recovered.localWorkflowReview, analysis);
      await show(recovered.id);
      await page.waitForFunction(id => document.querySelector('#mission044').dataset.taskId === id &&
        !document.querySelector('#mission044-confirm').disabled, recovered.id);
      const start = performance.now();
      await page.locator('#mission044-confirm').click();
      await until(async () => (await read(recovered.id)).selectedLocalWorkflow === analysis, 'explicit confirmation');
      const after = await checked(await terminal({
        id: recovered.id, label: 'confirmed after restart', briefing: uncertain, mode: 'Confirm', started: start
      }), analysis, source, 'confirmed', 1);
      assert.deepEqual(after.task.history.slice(0, proposal.task.history.length), proposal.task.history);
      assert.equal(after.task.resumes, 0);
      assert.equal(after.evidence.sourceSha256, source.sourceSha256);
      await assert.rejects(() => api('/api/agency043/control', { id: recovered.id, action: 'confirm-workflow' }));
      result.recovery = { modelCallsBefore: 1, modelCallsAfter: 1, alternateSource: backup.sourceSha256,
        sourceKept: after.evidence.sourceSha256, didVerify: true };
      await manual('agency043', 'mission-open', { id: after.task.id });
      await capture('review-confirmed.png', after.task.id);
    }

    for (const text of offlinePrompts) {
      const refused = await inert(await terminal(await submit('offline needs clarification', text)), 0);
      assert.match(refused.task.reason, /choose one workflow/i);
      assert(!refused.task.localWorkflowReview && !refused.task.localWorkflow.quickProcedure);
    }
    await capture('compound-clarification.png', result.tasks.find(row => row.briefing === offlinePrompts[0]).task.id);
    assert.deepEqual(result.errors, []);
    result.effort = { dispatchForKnown: 1, confirmForSuggestion: 1, reviewNavigationMeasuredSeparately: true };
    result.smoke = smoke;
    result.passed = true;
    console.log(JSON.stringify({ passed: true, smoke, tasks: result.tasks.map(row => [row.label, row.elapsedMs, row.task.timing052.modelCalls]) }));
  } finally {
    await close();
    await retain();
    verify(process.cwd());
  }
}

main().catch(error => { console.error(error); process.exitCode = 1; });
