
'use strict';
const assert = require('node:assert/strict');
const cp = require('node:child_process');
const fs = require('node:fs/promises');
const path = require('node:path');
const crypto = require('node:crypto');
const {launch} = require('../../workspace-desktop/test/calibration-integration.cjs');
const {verify} = require('../../scripts/Study-Build095.cjs');

const root = process.cwd();
const out = path.resolve('build', process.argv[2] || 'game168');
const courses = [
  {id: 'relay-circuit-v1', title: 'Relay Circuit', jumps: [3.5, 5.57],
    names: ['Copper cell', 'Mint cell'], rail: 'Live rail'},
  {id: 'relay-terraces-v1', title: 'Relay Terraces', jumps: [2.95, 5.50, 8.20],
    names: ['Lower cell', 'Midway cell', 'Return cell'], rail: 'High rail'}
];
const sha = bytes => crypto.createHash('sha256').update(bytes).digest('hex');

async function main() {
  const build = verify(root);
  await fs.mkdir(out);
  const project = path.join(out, 'project');
  await fs.mkdir(project);
  Object.assign(process.env, {FRAYNIX_WORKSPACE_PERSISTENT: '0',
    FRAYNIX_WORKSPACE_JAVA_HEAP_MIB: '768', FRAYNIX_WORKSPACE_JAVA_PROCESSORS: '2'});
  const result = {passed: false, createdAt: new Date().toISOString(), source: build.source,
    courses: [], captures: [], errors: []};
  let app, page;
  async function start() {
    ({app, page} = await launch(project));
    page.on('pageerror', error => result.errors.push(error.message));
    await app.evaluate(({BrowserWindow}) => {
      BrowserWindow.getAllWindows().find(window => window.isVisible()).setSize(1540, 1020);
    });
  }
  const api = (route, args = {}) => app.evaluate((_, request) =>
    global.workspaceTest.api(request.route, request.args), {route, args});
  const state = async () => (await api('/api/game/frame')).state;
  const controls = () => page.locator('#game-course-progress');
  const status = () => page.locator('#game-status');
  async function open(course) {
    await page.locator('#game-course-choice').selectOption(course.id);
    await page.locator('#game-play-course').click();
    await page.waitForFunction(title =>
      document.querySelector('#game-course-progress')?.textContent.startsWith(title), course.title);
    await page.getByLabel('Movement step size').selectOption('6');
    return state();
  }
  async function reset() {
    await page.locator('#game-reset').click();
    await page.waitForFunction(() => document.querySelector('#game-status').textContent.includes('tick 0 ·'));
    return state();
  }
  async function move(current, jump) {
    const button = jump ? page.getByRole('button', {name: 'Jump right', exact: true}) : page.locator('#game-right');
    await button.click();
    await page.waitForFunction(tick => document.querySelector('#game-status').textContent.includes('tick ' + tick + ' ·'),
      current.tick + (jump ? 28 : 6));
    return state();
  }
  async function capture(name) {
    await page.evaluate(() => new Promise(resolve => requestAnimationFrame(() => requestAnimationFrame(resolve))));
    const pixels = Buffer.from(await app.evaluate(() => global.workspaceTest.capture()), 'base64');
    await fs.writeFile(path.join(out, name), pixels);
    result.captures.push({name, sha256: sha(pixels)});
  }
  const compact = value => ({tick: value.tick, player: value.player, progress: value.courseProgress});
  try {
    await start();
    await page.locator('#game-tab').click();
    await page.waitForFunction(() => document.querySelector('#game-status').textContent.includes('tick 0 ·'));
    assert.equal((await api('/api/game/frame')).levelId, 'original');
    assert.equal(await controls().isVisible(), false);
    result.defaultOriginal = true;

    for (const course of courses) {
      let current = await open(course);
      const record = {id: course.id, initial: compact(current), trace: []};
      result.courses.push(record);
      assert.equal(current.contentId, course.id);
      assert.equal(current.courseProgress.totalPickups, course.names.length);
      await assert.rejects(api('/api/game/open-course', {id: '../anything'}), /Unknown original course/);
      await assert.rejects(api('/api/game/open-course', {id: course.id, path: '../anything'}), /field/i);
      assert.equal((await state()).sessionId, current.sessionId);
      let nextJump = 0;
      while (!current.player.won && !current.player.dead && current.tick < 760) {
        const jump = nextJump < course.jumps.length && current.player.x >= course.jumps[nextJump];
        if (jump) nextJump++;
        current = await move(current, jump);
        record.trace.push({...compact(current), jump});
        if (jump && nextJump === 1) await capture(course.id.replace('-v1', '-jump.png'));
      }
      record.finish = compact(current);
      assert.equal(current.player.won, true, course.title + ' finish contact');
      assert.deepEqual(current.courseProgress.pickups.map(event => event.name), course.names);
      assert.match(await status().textContent(), /goal reached/);
      assert.match(await controls().textContent(), /Beacon reached/);
      assert.match(await controls().textContent(), new RegExp('Cells ' + course.names.length + '/' + course.names.length));
      await capture(course.id.replace('-v1', '-finish.png'));
      current = await reset();
      assert.equal(current.courseProgress.pickups.length, 0);
      assert.equal(current.player.x, record.initial.player.x);

      // The existing AI is an independent comparison. Report its actual terminal result.
      await page.locator('#game-ai').click();
      const deadline = Date.now() + 18000;
      do {
        await page.waitForTimeout(100);
        current = await state();
      } while (!current.player.won && !current.player.dead && Date.now() < deadline);
      await page.locator('#game-pause').click();
      record.defaultAI = compact(current);
      record.policy = current.controller.policy;

      if (course.id === 'relay-circuit-v1') {
        current = await reset();
        while (!current.player.dead && current.tick < 240) current = await move(current, false);
        record.unjumped = compact(current);
        assert.equal(current.player.dead, true);
        assert.equal(current.courseProgress.hazard.name, course.rail);
        assert.match(await controls().textContent(), /Hit Live rail/);
        await capture('relay-circuit-hit.png');
      }
    }
    await app.evaluate(({BrowserWindow}) => {
      BrowserWindow.getAllWindows().find(window => window.isVisible()).setSize(1100, 1000);
    });
    await reset();
    await capture('relay-terraces-compact.png');
    assert.equal(await page.getByRole('button', {name: 'Jump right', exact: true}).isVisible(), true);
    assert.equal(await page.locator('#game-play-course').isVisible(), true);

    await app.close();
    app = null;
    await start();
    await page.locator('#game-tab').click();
    await page.waitForFunction(() => document.querySelector('#game-status').textContent.includes('tick 0 ·'));
    const reopened = await open(courses[1]);
    assert.equal(reopened.player.id, result.courses[1].initial.player.id);
    assert.equal(reopened.courseProgress.pickups.length, 0);
    assert.equal(reopened.tick, 0);
    await api('/api/game/close');
    assert.deepEqual(result.errors, []);
    verify(root);
    result.coldRestart = true;
    result.passed = true;
  } finally {
    if (app) await app.close().catch(() => {});
    await fs.writeFile(path.join(out, 'result.json'), JSON.stringify(result, null, 2) + '\n');
  }
  console.log(JSON.stringify({passed: result.passed, courses: result.courses.map(record =>
    ({id: record.id, ticks: record.finish.tick, defaultAI: record.defaultAI.player}))}));
}

main().catch(error => {
  console.error(error);
  process.exitCode = 1;
});
