'use strict';
const fs = require('node:fs/promises');
const path = require('node:path');
const assert = require('node:assert/strict');
const crypto = require('node:crypto');
const root = process.cwd();
const output = path.join(root, 'build/compatibility166');
const original = path.join(root, 'build/study165-on-b/project');
const copy = path.join(output, 'project');
const sha = bytes => crypto.createHash('sha256').update(bytes).digest('hex');

async function inventory(directory) {
  const files = {};
  async function walk(current) {
    for (const item of await fs.readdir(current, { withFileTypes: true })) {
      assert(!item.isSymbolicLink());
      const target = path.join(current, item.name);
      if (item.isDirectory()) await walk(target);
      else files[path.relative(directory, target).replaceAll('\\', '/')] = sha(await fs.readFile(target));
    }
  }
  await walk(directory);
  return files;
}

(async () => {
  require('../../scripts/Study-Build095.cjs').verify(root);
  const old = JSON.parse(await fs.readFile('build/study165-on-b/result.json'));
  assert(old.passed && old.tasks.length === 10);
  await fs.mkdir(output);
  const before = await inventory(original);
  await fs.cp(original, copy, { recursive: true, force: false, errorOnExist: true });
  Object.assign(process.env, {
    FRAYNIX_WORKSPACE_PERSISTENT: '0', FRAYNIX_WORKSPACE_JAVA_HEAP_MIB: '768',
    FRAYNIX_WORKSPACE_JAVA_PROCESSORS: '2', FRAYNIX_WORKSPACE_RECORDED_REPLAY: 'memo',
    FRAYNIX_WORKSPACE_COSTS164: 'on', FRAYNIX_WORKSPACE_EVIDENCE_REUSE: 'on'
  });
  const { launch } = require('../../workspace-desktop/test/calibration-integration.cjs');
  const result = { passed: false, sourceEdition: 165, checks: [], errors: [] };
  let app;
  try {
    ({ app } = await launch(copy));
    const api = (route, args) => app.evaluate((_, x) => global.workspaceTest.api(x.route, x.args), { route, args });
    const start = performance.now();
    for (const task of old.tasks) {
      const verified = await api('/api/workflows/verify-archive', { id: task.workflow.id });
      assert(verified.passed);
      const workflow = await api('/api/workflows/read', { id: task.workflow.id });
      assert.deepEqual(workflow, task.workflow);
      result.checks.push({ id: task.workflow.id, kind: task.kind, passed: true });
    }
    assert.equal((await api('/api/small161/state', {})).jobs.length, 8);
    const world = await api('/api/small161/world', { id: old.finalWorld.id });
    assert.deepEqual(world, old.finalWorld);
    result.verificationMs = performance.now() - start;
    result.worldUnchanged = true;
    result.originalFilesChecked = Object.keys(before).length;
    assert.deepEqual(await inventory(original), before);
    result.originalUnchanged = true;
    result.passed = true;
  } catch (error) {
    result.failure = error.stack;
    throw error;
  } finally {
    await fs.writeFile(path.join(output, 'result.json'), JSON.stringify(result, null, 2));
    if (app) await app.close();
  }
  console.log(JSON.stringify({ passed: true, oldWorkflows: result.checks.length }));
})().catch(error => { console.error(error); process.exitCode = 1; });
