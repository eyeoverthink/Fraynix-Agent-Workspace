
'use strict';

const assert = require('node:assert/strict');
const fs = require('node:fs/promises');
const path = require('node:path');
const crypto = require('node:crypto');
const {launch} = require('../../workspace-desktop/test/calibration-integration.cjs');
const {verify} = require('../../scripts/Study-Build095.cjs');

const root = process.cwd();
const output = path.resolve('build', process.argv[2] || 'game169');
const courses = [
  {id: 'relay-circuit-v1', title: 'Relay Circuit', rail: 'Live rail'},
  {id: 'relay-terraces-v1', title: 'Relay Terraces', rail: 'High rail'}
];

async function main() {
  const build = verify(root);
  await fs.mkdir(output);
  const project = path.join(output, 'project');
  await fs.mkdir(project);
  Object.assign(process.env, {FRAYNIX_WORKSPACE_PERSISTENT: '0',
    FRAYNIX_WORKSPACE_JAVA_HEAP_MIB: '768', FRAYNIX_WORKSPACE_JAVA_PROCESSORS: '2'});
  const result = {passed: false, source: build.source, date: new Date().toISOString(),
    courses: [], captures: [], errors: []};
  let app, page;
  async function start() {
    ({app, page} = await launch(project));
    page.on('pageerror', error => result.errors.push(error.message));
    await app.evaluate(({BrowserWindow}) => {
      BrowserWindow.getAllWindows().find(window => window.isVisible()).setSize(1540, 1020);
    });
  }

  const call = (route, args = {}) => app.evaluate((_, request) =>
    global.workspaceTest.api(request.route, request.args), {route, args});
  const frame = async () => (await call('/api/game/frame')).state;
  const command = async args => (await call('/api/game/command', args)).state;
  const terminal = s => s.player.won || s.player.dead;
  const status = () => page.locator('#game-course-progress');
  const compact = state => ({tick: state.tick, player: state.player, progress: state.courseProgress,
    rails: state.railNavigation, policy: state.controller.policy});

  async function open(course) {
    await page.locator('#game-course-choice').selectOption(course.id);
    await page.locator('#game-play-course').click();
    await page.waitForFunction(title =>
      document.querySelector('#game-course-progress')?.textContent.startsWith(title), course.title);
    const state = await frame();
    assert.equal(state.contentId, course.id);
    assert.equal(state.railNavigation, undefined);
    return state;
  }

  async function reset() {
    await page.locator('#game-reset').click();
    await page.waitForFunction(() => document.querySelector('#game-status').textContent.includes('tick 0 ·'));
    const state = await frame();
    assert.equal(state.railNavigation, undefined);
    assert.equal(state.courseProgress.pickups.length, 0);
    return state;
  }

  async function capture(name) {
    await page.evaluate(() => new Promise(resolve => requestAnimationFrame(() => requestAnimationFrame(resolve))));
    const png = Buffer.from(await app.evaluate(() => global.workspaceTest.capture()), 'base64');
    await fs.writeFile(path.join(output, name), png);
    result.captures.push({name, sha256: crypto.createHash('sha256').update(png).digest('hex')});
  }

  async function waitTick(tick) {
    await page.waitForFunction(t => document.querySelector('#game-status').textContent.includes('tick ' + t + ' ·'), tick);
  }

  function verifyFinish(state, course) {
    assert.equal(state.player.won, true, course.title + ' actual beacon contact');
    assert.equal(state.player.dead, false);
    assert.equal(state.courseProgress.hazard, undefined);
    assert.equal(state.controller.policy, 'contact122');
    const nav = state.railNavigation;
    assert.equal(nav.crossings.length, 1);
    assert.equal(nav.launches, 1);
    assert.equal(nav.rejectedMeasurements, 0);
    assert.equal(nav.crossings[0].id, nav.rails[0].id);
    assert.equal(nav.crossings[0].name, course.rail);
    assert(nav.crossings[0].measuredClearance > .035);
    assert(nav.crossings[0].overlapSamples >= 5);
  }

  try {
    await start();
    await page.locator('#game-tab').click();
    await waitTick(0);
    assert.equal((await call('/api/game/frame')).levelId, 'original');
    assert.equal((await frame()).railNavigation, undefined);
    await page.locator('#game-relay').click();
    await page.waitForFunction(() => document.querySelector('#game-target').value === '14.625');
    assert.equal((await call('/api/game/frame')).levelId, 'relay-garden-v1');
    const garden = await command({action: 'ai-step', ticks: 6, targetX: 14.625});
    assert.equal(garden.railNavigation, undefined);
    assert.equal(garden.controller.policy, 'contact122');
    result.legacyDefaults = true;

    for (const course of courses) {
      let current = await open(course);
      const record = {id: course.id, initial: compact(current), trace: []};
      result.courses.push(record);
      const stem = course.id.replace('-v1', '');
      const targetX = Number(await page.locator('#game-target').inputValue());
      let photographed = false;
      while (!terminal(current) && current.tick < 650) {
        current = await command({action: 'ai-step', ticks: 6, targetX});
        record.trace.push(compact(current));
        const rail = current.railNavigation.rails[0].bounds;
        if (!photographed && current.railNavigation.state.startsWith('Jumping over')
            && current.player.x >= rail.left && current.player.x <= rail.right
            && current.player.y > rail.top + .15) {
          await waitTick(current.tick);
          assert.match(await status().textContent(), /AI jumping over/);
          await capture(stem + '-ai-crossing.png');
          photographed = true;
        }
      }
      assert(photographed, course.title + ' capture above observed rail');
      verifyFinish(current, course);
      record.bounded = compact(current);
      await reset();

      await page.locator('#game-ai').click();
      const deadline = Date.now() + 30000;
      do {
        await page.waitForTimeout(100);
        current = await frame();
      } while (!terminal(current) && Date.now() < deadline);
      verifyFinish(current, course);
      record.visibleAi = compact(current);
      await page.locator('#game-pause').click();
      await page.waitForFunction(() => document.querySelector('#game-course-progress').textContent.includes('Beacon reached'));
      assert.match(await status().textContent(), /AI cleared/);
      await capture(stem + '-ai-finish.png');
    }

    await app.evaluate(({BrowserWindow}) => {
      BrowserWindow.getAllWindows().find(window => window.isVisible()).setSize(1100, 1000);
    });
    assert.equal(await page.locator('#game-ai').isVisible(), true);
    assert.match(await status().textContent(), /AI cleared High rail/);
    await capture('relay-ai-compact.png');
    await app.close();
    app = null;
    await start();
    await page.locator('#game-tab').click();
    await waitTick(0);
    const fresh = await open(courses[1]);
    assert.equal(fresh.player.id, result.courses[1].initial.player.id);
    assert.equal(fresh.tick, 0);
    await call('/api/game/close');
    assert.deepEqual(result.errors, []);
    verify(root);
    result.coldRestart = true;
    result.passed = true;
  } finally {
    if (app) await app.close().catch(() => {});
    await fs.writeFile(path.join(output, 'result.json'), JSON.stringify(result, null, 2) + '\n');
  }
  console.log(JSON.stringify({passed: result.passed, courses: result.courses.map(c => ({
    id: c.id, boundedTicks: c.bounded.tick, visibleObservedTicks: c.visibleAi.tick,
    crossing: c.visibleAi.rails.crossings[0], cells: c.visibleAi.progress.pickups.length
  }))}));
}

main().catch(error => {
  console.error(error);
  process.exitCode = 1;
});
