
'use strict';

const assert = require('node:assert/strict');
const fs = require('node:fs/promises');
const path = require('node:path');
const crypto = require('node:crypto');
const {launch} = require('../../workspace-desktop/test/calibration-integration.cjs');
const {verify} = require('../../scripts/Study-Build095.cjs');

const root = process.cwd();
const output = path.resolve('build', process.argv[2] || 'game170');
const courses = [
  {id: 'relay-switchyard-v1', title: 'Relay Switchyard', names: ['West rail', 'East rail']},
  {id: 'relay-ridge-v1', title: 'Relay Ridge', names: ['First ridge rail', 'Second ridge rail', 'Lower rail']}
];
const paces = ['normal', 'steady', 'careful'];

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
  const compact = state => ({tick: state.tick, sessionId: state.sessionId, player: state.player,
    progress: state.courseProgress, rails: state.railNavigation, policy: state.controller.policy});
  const paceControl = () => page.getByLabel('AI rail approach pace');

  async function waitTick(tick) {
    await page.waitForFunction(t => document.querySelector('#game-status').textContent.includes('tick ' + t + ' ·'), tick);
  }

  async function open(course) {
    await page.locator('#game-course-choice').selectOption(course.id);
    await page.locator('#game-play-course').click();
    await page.waitForFunction(title => {
      const progress = document.querySelector('#game-course-progress');
      return progress && !progress.parentElement.hidden && progress.textContent.startsWith(title)
        && document.querySelector('#game-status').textContent.includes('tick 0 ·');
    }, course.title);
    const state = await frame();
    assert.equal(state.contentId, course.id);
    assert.equal(state.railNavigation, undefined);
    return state;
  }

  async function capture(name) {
    await page.evaluate(() => new Promise(resolve => requestAnimationFrame(() => requestAnimationFrame(resolve))));
    const png = Buffer.from(await app.evaluate(() => global.workspaceTest.capture()), 'base64');
    await fs.writeFile(path.join(output, name), png);
    result.captures.push({name, sha256: crypto.createHash('sha256').update(png).digest('hex')});
  }

  function verifyFinish(state, course, pace) {
    assert.equal(state.player.won, true, course.title + ' actual beacon contact at ' + pace);
    assert.equal(state.player.dead, false);
    assert.equal(state.courseProgress.hazard, undefined);
    assert.equal(state.controller.policy, 'contact122');
    const nav = state.railNavigation;
    assert.equal(nav.pace, pace);
    assert.deepEqual(nav.crossings.map(row => row.name), course.names);
    assert.deepEqual(nav.crossings.map(row => row.id), nav.rails.map(row => row.id));
    assert.equal(nav.launches, 2);
    assert.equal(nav.rejectedMeasurements, 0);
    for (const row of nav.crossings) {
      assert(row.measuredClearance > .035);
      assert(row.overlapSamples >= 5);
      assert(row.launchSpeed > .6 && row.entrySpeed > 0);
    }
    if (course === courses[1]) assert.equal(nav.crossings[0].launchTick, nav.crossings[1].launchTick);
  }

  try {
    await start();
    await page.locator('#game-tab').click();
    await waitTick(0);
    assert.equal((await call('/api/game/frame')).levelId, 'original');
    const circuit = await open({id: 'relay-circuit-v1', title: 'Relay Circuit'});
    assert.equal(circuit.railPaceAvailable, undefined);
    assert.equal(await paceControl().isVisible(), false);
    result.legacyDefaults = true;

    for (const course of courses) {
      const record = {id: course.id, runs: []};
      result.courses.push(record);
      for (const pace of paces) {
        // Reopen the course to give each pace an independent starting process and memory.
        if (record.runs.length) {
          await page.locator('#game-close').click();
          await page.waitForFunction(() => document.querySelector('#game-course-progress').parentElement.hidden);
        }
        const initial = await open(course);
        assert.equal(initial.tick, 0);
        assert.equal(await paceControl().isVisible(), true);
        if (!record.runs.length) assert.equal(await paceControl().inputValue(), 'normal');
        await paceControl().selectOption(pace);
        await page.locator('#game-ai').click();
        let current;
        const deadline = Date.now() + 35000;
        do {
          await page.waitForTimeout(100);
          current = await frame();
        } while (!terminal(current) && Date.now() < deadline);
        verifyFinish(current, course, pace);
        record.runs.push({pace, initial: compact(initial), final: compact(current)});
        await page.locator('#game-pause').click();
        await page.waitForFunction(() => document.querySelector('#game-course-progress').textContent.includes('Beacon reached'));
        assert.match(await status().textContent(), new RegExp('Rails ' + course.names.length + '/' + course.names.length));
        if (pace === 'normal' || pace === 'careful') {
          await capture(course.id.replace('-v1', '') + '-' + pace + '-finish.png');
        }
      }
      for (let rail = 0; rail < course.names.length; rail++) {
        const [normal, steady, careful] = record.runs.map(run => run.final.rails.crossings[rail]);
        assert(normal.launchSpeed - steady.launchSpeed > .4);
        assert(steady.launchSpeed - careful.launchSpeed > .2);
        assert(normal.entrySpeed - steady.entrySpeed > .1);
        assert(steady.entrySpeed - careful.entrySpeed > .07);
      }
    }

    await page.locator('#game-reset').click();
    await waitTick(0);
    assert.equal((await frame()).railNavigation, undefined);
    const targetX = Number(await page.locator('#game-target').inputValue());
    let current, inPair = false;
    for (let attempt = 0; attempt < 135; attempt++) {
      current = await command({action: 'ai-step', ticks: 2, targetX, railPace: 'careful'});
      const nav = current.railNavigation;
      const first = nav.rails[0].bounds;
      inPair = nav.state === 'Jumping over 2 rails' && current.player.x >= first.left
        && current.player.x <= first.right && current.player.y > first.top + .15;
      if (inPair) break;
    }
    assert(inPair, 'Capture the real paired flight above the first fixture');
    await waitTick(current.tick);
    assert.match(await status().textContent(), /AI jumping over 2 rails/);
    result.pairedCapture = compact(current);
    await capture('relay-ridge-paired-jump.png');
    await app.evaluate(({BrowserWindow}) => {
      BrowserWindow.getAllWindows().find(window => window.isVisible()).setSize(1100, 1000);
    });
    assert.equal(await paceControl().isVisible(), true);
    await capture('relay-ridge-compact.png');

    await app.close();
    app = null;
    await start();
    await page.locator('#game-tab').click();
    await waitTick(0);
    const fresh = await open(courses[1]);
    assert.equal(fresh.player.id, result.courses[1].runs[0].initial.player.id);
    assert.equal(await paceControl().inputValue(), 'normal');
    await call('/api/game/close');
    assert.deepEqual(result.errors, []);
    verify(root);
    result.coldRestart = true;
    result.passed = true;
  } finally {
    if (app) await app.close().catch(() => {});
    await fs.writeFile(path.join(output, 'result.json'), JSON.stringify(result, null, 2) + '\n');
  }

  console.log(JSON.stringify({passed: result.passed, runs: result.courses.flatMap(course =>
    course.runs.map(run => ({id: course.id, pace: run.pace, tick: run.final.tick,
      crossings: run.final.rails.crossings.map(row => [row.name, row.launchSpeed, row.entrySpeed, row.measuredClearance])})))}));
}

main().catch(error => {
  console.error(error);
  process.exitCode = 1;
});
