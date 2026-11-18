'use strict';

const assert = require('node:assert/strict');
const {createHash} = require('node:crypto');
const fs = require('node:fs/promises');
const path = require('node:path');
const {launch} = require('../../workspace-desktop/test/calibration-integration.cjs');
const {verify} = require('../../scripts/Study-Build095.cjs');

const output = path.resolve('build', process.argv[2] || 'game171');
const project = path.join(output, 'project');
const pause = ms => new Promise(resolve => setTimeout(resolve, ms));

async function main() {
  const build = verify(process.cwd());
  await fs.mkdir(output);
  await fs.mkdir(project);
  Object.assign(process.env, {
    FRAYNIX_WORKSPACE_NO_MODELS: '1', FRAYNIX_WORKSPACE_PERSISTENT: '0',
    FRAYNIX_WORKSPACE_JAVA_HEAP_MIB: '768', FRAYNIX_WORKSPACE_JAVA_PROCESSORS: '2'
  });

  const result = {passed: false, source: build.source, checks: [], captures: [], errors: []};
  let app, page;
  const api = (route, args = {}) => app.evaluate((_, req) => global.workspaceTest.api(req.route, req.args), {route, args});
  const request = (operation, command, args) => page.evaluate(req => window.workspace.request(req), {operation, command, args});
  const parent = id => api('/api/agency043/read', {id});
  const child = id => api('/api/game-tasks115/read', {id});
  const delay = ms => new Promise(resolve => setTimeout(resolve, ms));
  const proof = id => api('/api/game-tasks115/verify', {id});
  const wonTrials = row => row.trials.filter(t => t.status === 'accepted');

  async function eventually(callback, timeout = 150000) {
    const until = Date.now() + timeout;
    while (Date.now() < until) {
      const value = await callback();
      if (value) return value;
      await delay(150);
    }
    throw Error('Timed out waiting for a verified checkpoint');
  }

  async function start() {
    ({app, page} = await launch(project));
    page.on('pageerror', error => result.errors.push(error.message));
    await app.evaluate(({BrowserWindow}) => {
      BrowserWindow.getAllWindows().find(window => window.isVisible()).setSize(1540, 1020);
    });
    assert.deepEqual((await api('/api/state')).models, []);
    const network = await app.evaluate(() => global.workspaceTest.network());
    assert.equal(network.enabled, true);
    result.network = network;
    await page.waitForFunction(() => document.querySelector('#run')?.disabled);
    assert.equal(await page.locator('#model option').count(), 0);
  }

  async function capture(name) {
    await page.evaluate(() => new Promise(resolve => requestAnimationFrame(() => requestAnimationFrame(resolve))));
    const png = Buffer.from(await app.evaluate(() => global.workspaceTest.capture()), 'base64');
    await fs.writeFile(path.join(output, name), png);
    result.captures.push({name, sha256: createHash('sha256').update(png).digest('hex')});
  }

  async function submit() {
    const existing = new Set((await api('/api/agency043/state')).tasks.map(t => t.id));
    assert.equal(await page.locator('#run').isEnabled(), true);
    await page.locator('#run').click();
    return eventually(async () => (await api('/api/agency043/state')).tasks.find(t => !existing.has(t.id)));
  }

  async function showMission(id) {
    await page.locator('#mission044-open').click();
    await page.locator('#mission044-task').selectOption(id);
  }

  async function completion(id) {
    const task = await eventually(async () => {
      const current = await parent(id);
      if (['interrupted', 'cancelled'].includes(current.status)) throw Error(current.reason);
      return current.status === 'completed' && current;
    });
    assert.equal(task.family044, 'gamelevel');
    assert.equal(task.timing052.modelCalls, 0);
    const checked = await proof(id);
    assert.equal(checked.audit.passed, true);
    assert.deepEqual(task.completion044.javaGameTask115.audit, checked.audit);
    return {parent: task, checked};
  }

  try {
    await start();
    await page.locator('#goal').fill('Explain how to playtest Relay Ridge at all three paces.');
    assert.equal(await page.locator('#run').isDisabled(), true);
    await page.locator('#game-tab').click();
    await page.locator('#game-course-choice').selectOption('relay-ridge-v1');
    await page.locator('#game-play-course').click();
    await page.locator('#game-relay-playtest').click();
    assert.equal(await page.locator('#goal').inputValue(), 'Playtest Relay Ridge at all three paces.');
    await capture('relay-mission-start.png');

    const began = Date.now();
    const {id} = await submit();
    result.beforeCrash = await eventually(async () => {
      const task = await parent(id);
      if (task.status === 'interrupted') throw Error(task.reason);
      if (!task.context044?.ownedGameTask115) return false;
      const run = await child(id);
      return wonTrials(run).length === 2 && run.trials.at(-1).status === 'running'
        && run.trials.at(-1).inputChunks >= 2 && run;
    });
    assert(await app.evaluate(() => global.workspaceTest.crashEngine()));
    await pause(550);
    await app.close().catch(() => {});
    app = null;

    await start();
    result.afterRestart = await child(id);
    assert.equal(result.afterRestart.status, 'interrupted');
    assert.deepEqual(wonTrials(result.afterRestart), wonTrials(result.beforeCrash));
    assert.equal(result.afterRestart.trials.at(-1).status, 'interrupted');
    await assert.rejects(proof(id), /incomplete/i);
    await showMission(id);
    await page.waitForFunction(() => !document.querySelector('#mission044-resume').disabled);
    await capture('relay-mission-resume.png');
    await page.locator('#mission044-resume').click();

    result.ridge = await completion(id);
    result.ridgeElapsedMs = Date.now() - began;
    const {audit, trials} = result.ridge.checked;
    assert.equal(audit.crossings, 9);
    assert.equal(audit.independentReplays, 3);
    assert.deepEqual(wonTrials(result.ridge.checked).slice(0, 2), wonTrials(result.beforeCrash));
    assert.equal(new Set(trials.map(row => row.sessionId)).size, 7);
    await page.waitForFunction(() => !document.querySelector('#mission044-result').disabled);
    await capture('relay-mission-completed.png');
    await page.locator('#mission044-result').click();
    await eventually(async () => app.evaluate(() => global.workspaceTest.canvasRead(
      "document.querySelector('#outcome')?.textContent || ''"
    )).then(text => String(text).includes('9 rail crossings')).catch(() => false));
    const report = await api('/api/artifact/read', {id: result.ridge.checked.report.id});
    assert.deepEqual(JSON.parse(report.contents['playtest.json']), audit);
    assert.equal(await app.evaluate(() => global.workspaceTest.canvasRead(
      "document.querySelectorAll('tbody tr').length"
    )), 9);
    await capture('relay-mission-report.png');

    await app.close();
    app = null;
    await start();
    assert.deepEqual(await proof(id), result.ridge.checked);
    const restored = await request('agency043', 'mission-open', {id});
    assert.equal(restored.id, result.ridge.checked.report.id);
    await page.locator('#goal').fill('Verify Relay Switchyard at careful pace.');
    const second = await submit();
    result.switchyard = await completion(second.id);
    assert.equal(result.switchyard.checked.audit.crossings, 2);
    assert.equal(result.switchyard.checked.audit.independentReplays, 1);

    assert.deepEqual(result.errors, []);
    verify(process.cwd());
    result.checks.push('Empty model catalog and application browser offline mode',
      'Visible Game Lab control prepares a bounded chat mission',
      'Abrupt Java process stop preserves the completed trial pair',
      'Resume repeats the unfinished trial while keeping the original witnesses',
      'Three fresh Ridge replays and one fresh Switchyard replay match the actual motion',
      'The visible report has nine Ridge measurements and survives a cold restart');
    result.passed = true;
    console.log(JSON.stringify({passed: true, ridgeCrossings: audit.crossings, minimum: audit.minimumClearance,
      ridgeElapsedMs: result.ridgeElapsedMs, switchyardCrossings: result.switchyard.checked.audit.crossings}));
  } finally {
    if (app) await app.close().catch(() => {});
    await fs.writeFile(path.join(output, 'result.json'), JSON.stringify(result, null, 2) + '\n');
  }
}

main().catch(error => { console.error(error); process.exitCode = 1; });
