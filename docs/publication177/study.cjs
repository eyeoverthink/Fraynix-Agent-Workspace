'use strict';

const assert = require('node:assert/strict');
const { createHash } = require('node:crypto');
const fs = require('node:fs/promises');
const path = require('node:path');
const { launch } = require('../../workspace-desktop/test/calibration-integration.cjs');
const { verify } = require('../../scripts/Study-Build095.cjs');

const output = path.resolve('build', process.argv[2] || 'chain177');
const project = path.join(output, 'project');
const requestText = "Review a local chain: Compare both ramp setups, then analyze the farther setup's motion.";
const header = 'angle_deg,friction,gravity_mps2,mass_kg,duration_s\n';
const a = '21,0.25,9.81,1,0.8\n', b = '35,0.07,9.81,1.25,0.8\n';
const digest = data => createHash('sha256').update(data).digest('hex');
const owned = (task, kind) => {
  const bytes = createHash('md5').update('ramp-trace-chain:' + kind + ':' + task).digest();
  bytes[6] = (bytes[6] & 15) | 48;
  bytes[8] = (bytes[8] & 63) | 128;
  return bytes.toString('hex').replace(/^(........)(....)(....)(....)(............)$/, '$1-$2-$3-$4-$5');
};

async function main() {
  const locked = verify(process.cwd());
  await fs.mkdir(output);
  await fs.mkdir(project);
  Object.assign(process.env, { FRAYNIX_WORKSPACE_NO_MODELS: '1', FRAYNIX_WORKSPACE_PERSISTENT: '0',
    FRAYNIX_WORKSPACE_JAVA_HEAP_MIB: '768', FRAYNIX_WORKSPACE_JAVA_PROCESSORS: '2' });
  let app, page;
  const result = { passed: false, source: locked.source, captures: [], errors: [] };
  const save = () => fs.writeFile(path.join(output, 'result.json'), JSON.stringify(result, null, 2) + '\n');
  const api = (route, args = {}) => app.evaluate((_, x) => global.workspaceTest.api(x.route, x.args), { route, args });
  const request = (operation, command, args) => page.evaluate(x => window.workspace.request(x), { operation, command, args });
  const task = id => api('/api/agency043/read', { id });
  const ramp = id => api('/api/ramp-mission/verify', { id });
  const verifyChat = id => api('/api/agency043/verify', { id });
  const delay = ms => new Promise(resolve => setTimeout(resolve, ms));

  async function until(fn, label) {
    const end = Date.now() + 60000;
    while (Date.now() < end) {
      const value = await fn();
      if (value) return value;
      await delay(120);
    }
    throw Error('Timed out waiting for ' + label);
  }

  async function open() {
    ({ app, page } = await launch(project));
    page.on('pageerror', error => result.errors.push(error.message));
    assert((await app.evaluate(() => global.workspaceTest.network())).enabled);
    assert.deepEqual((await app.evaluate(() => global.workspaceTest.api('/api/state'))).models, []);
    await app.evaluate(({ BrowserWindow }) => BrowserWindow.getAllWindows().find(w => w.isVisible()).setSize(1550, 1040));
  }

  async function close() {
    if (!app) return;
    await app.close().catch(() => {});
    app = null;
  }

  function manual(operation, command, args) {
    return until(async () => {
      try { return await request(operation, command, args); }
      catch (e) {
        if (!/Wait for the current operation|Resume or Stop the evidence workflow/.test(e.message)) throw e;
        return null;
      }
    }, 'desktop ownership released for ' + command);
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
    result.captures.push({ name, sha256: digest(png) });
    await save();
  }

  async function inspectStatus(id, label) {
    let previous;
    return until(async () => {
      const state = await task(id);
      if (state.reason !== previous) console.log(label + ' · ' + state.reason);
      previous = state.reason;
      return ['interrupted', 'completed'].includes(state.status) && state;
    }, label);
  }

  async function review(pause, label) {
    const goal = requestText + (pause ? ' Pause after comparison.' : '');
    assert.equal(await page.locator('#goal').inputValue(), goal);
    const existing = new Set((await api('/api/agency043/state')).tasks.map(row => row.id));
    await until(() => page.locator('#run').isEnabled(), 'available dispatch');
    await page.locator('#run').click();
    const row = await until(async () => (await api('/api/agency043/state')).tasks.find(
      item => !existing.has(item.id) && item.goal === goal), label + ' admission');
    const held = await inspectStatus(row.id, label + ' initial review');
    assert.equal(held.status, 'interrupted', held.reason);
    assert(!held.rampTraceChainApproved && !held.context044.ownedRampMission);
    await assert.rejects(() => api('/api/ramp-mission/read', { id: row.id }));
    await refuseSecond(row.id);
    await assert.rejects(() => api('/api/agency043/control', { id: row.id, action: 'resume' }), /confirm the reviewed chain/i);
    await show(row.id);
    await until(() => page.locator('#mission044-confirm').isEnabled(), 'review confirmation ready');
    if (label === 'restart') await capture('chain-review.png', row.id);
    await page.locator('#mission044-confirm').click();
    await until(async () => (await task(row.id)).rampTraceChainApproved, 'confirmed chain');
    return row.id;
  }

  async function refuseSecond(id) {
    await assert.rejects(() => api('/api/workflows/read', { id: owned(id, 'analysis') }));
    await assert.rejects(() => api('/api/measurement/read', { id: owned(id, 'trace'), version: 1 }));
  }

  async function setGoal(csv, pause) {
    await manual('rampInput', 'save', { csv });
    await page.locator('#goal').fill(requestText + (pause ? ' Pause after comparison.' : ''));
  }

  async function verifyCompleted(id, winner, sourceCsv, started) {
    const done = await inspectStatus(id, 'linked outcome');
    assert.equal(done.status, 'completed', done.reason);
    assert.equal(done.outcome, 'verified');
    assert.equal(done.timing052.modelCalls, 0);
    const first = await ramp(id);
    const meta = done.completion044;
    const linked = meta.chainHandoff, workflow = meta.javaWorkflow;
    const dataset = await api('/api/measurement/read', { id: linked.datasetId, version: 1 });
    const analysis = await api('/api/goals/read', { id: workflow.stages[0].goalId });
    const artifact = await api('/api/artifact/read', { id: linked.report.id });
    assert.equal(artifact.contents['input.csv'], sourceCsv);
    assert.equal(first.audit.farther, winner);
    assert.equal(first.canvasReceipt.checks.length, 9);
    assert.equal(linked.winner, winner);
    assert.equal(linked.datasetId, owned(id, 'trace'));
    assert.equal(workflow.id, owned(id, 'analysis'));
    assert.equal(dataset.sourceKind, 'synthetic');
    assert.equal(dataset.csv, linked.traceCsv);
    assert.equal(digest(dataset.csv), linked.traceSha256);
    const paired = JSON.parse(artifact.contents['experiment.json']);
    const winningSamples = paired.original[winner].samples;
    assert.deepEqual(winningSamples, paired.replay[winner].samples);
    const rows = dataset.csv.trim().split('\n').slice(1).map(row => row.split(',').map(Number));
    assert.deepEqual(rows.map(row => row.map(x => x || 0)), winningSamples.map(row => [row.timeS || 0, row.distanceM || 0]));
    const travel = rows.slice(1).reduce((sum, row, index) => sum + Math.abs(row[1] - rows[index][1]), 0);
    assert(Math.abs(analysis.metrics.totalTravelM - travel) < 1e-10);
    assert.equal(rows.length, linked.traceRows);
    const reviewIndex = done.history.findIndex(row => row.kind === 'chain-confirmed');
    const gateIndex = done.history.findIndex(row => row.kind === 'chain-handoff-verified');
    assert(reviewIndex >= 0 && gateIndex > reviewIndex);
    assert(Date.parse(done.history[gateIndex].at) <= Date.parse(workflow.createdAt));
    const proof = await verifyChat(id);
    assert(proof.passed && proof.ramp && proof.workflow && proof.handoffSha256);
    await manual('agency043', 'mission-open', { id });
    return { done, workflow, dataset, proof, travel, rows, elapsedMs: Math.round(performance.now() - started) };
  }

  try {
    await open();
    const rival = await manual('data', 'import', { title: 'Unrelated selected dataset', filename: 'unrelated.csv',
      csv: 'time,distance\n0,0\n0.5,90\n', sourceKind: 'synthetic', sourceNote: 'Unrelated fixture',
      missingPolicy: 'reject', mapping: { timeColumn: 'time', timeUnit: 's', distanceColumn: 'distance', distanceUnit: 'm' } });
    await page.locator('#ramp-input-open').click();
    await page.locator('#ramp-csv').setInputFiles({ name: 'paired.csv', mimeType: 'text/csv', buffer: Buffer.from(header + a + b) });
    await page.waitForFunction(() => document.querySelector('#ramp-file-note').textContent.startsWith('CSV checked'));
    await page.locator('#ramp-result').selectOption('chain');
    await page.locator('#ramp-pause').check();
    await page.locator('#ramp-input-save').click();
    await page.locator('#ramp-input-dialog').waitFor({ state: 'hidden' });
    const id = await review(true, 'restart');
    const boundary = await inspectStatus(id, 'verified first-step checkpoint');
    assert.match(boundary.reason, /comparison verified/i);
    assert(boundary.context044.chainHandoff && !boundary.context044.ownedWorkflowId);
    await refuseSecond(id);
    await manual('agency043', 'mission-open', { id });
    await capture('chain-first-verified.png', id);
    const original = await ramp(id);
    result.boundary = { task: boundary, comparison: original };
    await save();
    assert(await app.evaluate(() => global.workspaceTest.crashEngine()));
    await delay(300);
    await close();
    await open();
    assert.deepEqual(await ramp(id), original);
    assert.deepEqual((await task(id)).context044.chainHandoff, boundary.context044.chainHandoff);
    await refuseSecond(id);
    await show(id);
    await until(() => page.locator('#mission044-resume').isEnabled(), 'resume after completed first stage');
    const resume = performance.now();
    await page.locator('#mission044-resume').click();
    await until(async () => (await task(id)).resumes === 1, 'explicit resume');
    result.recovered = await verifyCompleted(id, 'second', header + a + b, resume);
    assert.notEqual(rival.datasetId, result.recovered.dataset.datasetId);
    assert.deepEqual(await ramp(id), original);
    await capture('chain-summary.png', id);
    await manual('agency043', 'mission-open-comparison', { id });
    assert.match(await app.evaluate(() => global.workspaceTest.canvasRead('document.querySelector("h1").innerText')), /verified ramp comparison/i);

    await setGoal(header + b + a, false);
    const started = performance.now();
    const auto = await review(false, 'automatic');
    result.automatic = await verifyCompleted(auto, 'first', header + b + a, started);
    assert.equal(result.automatic.dataset.csv, result.recovered.dataset.csv);

    await setGoal(header + a + b, true);
    const changed = await review(true, 'changed report');
    const checked = await inspectStatus(changed, 'checked before revision');
    await refuseSecond(changed);
    const reportId = checked.context044.chainHandoff.report.id;
    const savedReport = await api('/api/artifact/read', { id: reportId });
    await api('/api/artifact/write', { id: reportId, expectedVersion: 1, title: 'Altered verification fixture',
      files: { ...savedReport.contents, 'index.html': savedReport.contents['index.html'] + '\n<!-- deliberate test revision -->' },
      sources: savedReport.sources });
    await show(changed);
    await until(() => page.locator('#mission044-resume').isEnabled(), 'resume revised case');
    await page.locator('#mission044-resume').click();
    await until(async () => (await task(changed)).resumes === 1, 'retry of revised comparison');
    result.refused = await inspectStatus(changed, 'changed verified report');
    assert.match(result.refused.reason, /saved ramp report changed/i);
    await refuseSecond(changed);
    await capture('chain-refused.png', changed);

    await setGoal(header + a + a, false);
    const tie = await review(false, 'tie');
    result.tie = await inspectStatus(tie, 'tie refused');
    assert.match(result.tie.reason, /comparison is a tie/i);
    assert.equal((await ramp(tie)).audit.farther, 'tie');
    await refuseSecond(tie);

    const history = await api('/api/agency043/history');
    assert(history.records.some(row => row.id === id && row.eligible));
    await manual('agency043', 'archive', { planSha256: history.planSha256, ids: [id] });
    assert((await task(id)).archive112);
    await close();
    await open();
    const cold = await verifyChat(id);
    assert(cold.passed && cold.ramp && cold.workflow && cold.handoffSha256 === result.recovered.proof.handoffSha256);
    await manual('agency043', 'mission-open', { id });
    result.archived = cold;
    assert.deepEqual(result.errors, []);
    result.passed = true;
    console.log(JSON.stringify({ passed: true, resumedRows: result.recovered.rows.length, travel: result.recovered.travel,
      automatic: result.automatic.done.outcome, changedRefused: result.refused.reason, tie: result.tie.reason, archived: cold.passed }));
  } finally {
    await close();
    await save();
    verify(process.cwd());
  }
}

main().catch(error => { console.error(error); process.exitCode = 1; });
