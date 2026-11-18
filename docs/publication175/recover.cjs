'use strict';

const assert = require('node:assert/strict');
const fs = require('node:fs/promises');
const path = require('node:path');
const { createHash } = require('node:crypto');
const { launch } = require('../../workspace-desktop/test/calibration-integration.cjs');
const { verify } = require('../../scripts/Study-Build095.cjs');

async function main() {
  const output = path.resolve('build', process.argv[2] || 'routing175-release');
  const project = path.join(output, 'project');
  const filename = path.join(output, 'result.json');
  const raw = await fs.readFile(filename);
  const result = JSON.parse(raw);
  assert(!result.passed && result.cases.length === 10 && result.cases.every(row => row.verified));
  assert(result.captures.some(shot => shot.name === 'routing-restart.png'));
  assert.equal(verify(process.cwd()).source, result.source);
  const checkpoint = JSON.parse(await fs.readFile(path.join(project, 'agency-043/tasks.json'), 'utf8')).data.tasks.at(-1);
  assert.equal(checkpoint.status, 'interrupted');
  assert.equal(checkpoint.resumes, 0);
  assert.equal(checkpoint.selectedLocalWorkflow, 'input-summary-workflow-104-v1');
  assert.equal(checkpoint.effect.tool, 'workflow.start');
  assert.equal(checkpoint.effect.args.datasetId, checkpoint.localWorkflow.dataset.id);
  assert(!checkpoint.context044.ownedWorkflowId);
  await fs.writeFile(path.join(output, 'result-before-recovery.json'), raw, { flag: 'wx' });
  Object.assign(process.env, {
    FRAYNIX_WORKSPACE_NO_MODELS: '1', FRAYNIX_WORKSPACE_PERSISTENT: '0',
    FRAYNIX_WORKSPACE_JAVA_HEAP_MIB: '768', FRAYNIX_WORKSPACE_JAVA_PROCESSORS: '2'
  });
  const { app, page } = await launch(project);
  const api = (route, args) => app.evaluate((_, input) => global.workspaceTest.api(input.route, input.args), { route, args });
  const errors = [];
  page.on('pageerror', error => errors.push(error.message));
  try {
    assert.deepEqual((await app.evaluate(() => global.workspaceTest.api('/api/state'))).models, []);
    const pending = await api('/api/agency043/read', { id: checkpoint.id });
    assert.equal(pending.status, 'interrupted');
    assert.deepEqual(pending.history, checkpoint.history);
    assert.deepEqual(pending.localWorkflow, checkpoint.localWorkflow);
    assert.deepEqual(pending.effect.args, checkpoint.effect.args);
    assert.equal(pending.timing052.modelCalls, 1);
    await page.locator('#mission044-open').click();
    await page.locator('#mission044-task').selectOption(pending.id);
    await page.waitForFunction(id => document.querySelector('#mission044').dataset.taskId === id &&
      !document.querySelector('#mission044-resume').disabled, pending.id);
    await page.locator('#mission044-resume').click();
    const deadline = Date.now() + 70000;
    let final;
    while (Date.now() < deadline) {
      final = await api('/api/agency043/read', { id: pending.id });
      if (final.resumes && ['completed', 'interrupted', 'cancelled'].includes(final.status)) break;
      await new Promise(resolve => setTimeout(resolve, 160));
    }
    assert.equal(final.status, 'completed', final.reason);
    assert.equal(final.outcome, 'verified');
    assert.equal(final.resumes, 1);
    assert.equal(final.timing052.modelCalls, 1);
    assert.deepEqual(final.history.slice(0, pending.history.length), pending.history);
    assert.deepEqual(final.localWorkflow, pending.localWorkflow);
    assert.equal(final.selectedLocalWorkflow, pending.selectedLocalWorkflow);
    const { id, version, sourceSha256 } = pending.localWorkflow.dataset;
    const original = await api('/api/measurement/read', { id, version });
    const sourceHash = createHash('sha256').update(original.csv).digest('hex');
    assert.equal(sourceHash, sourceSha256);
    const workflow = final.completion044.javaWorkflow;
    assert.equal(workflow.dataBinding.datasetId, id);
    assert.equal(workflow.dataBinding.datasetVersion, version);
    const evidence = await api('/api/workflows/verify-archive', { id: workflow.id });
    assert(evidence.passed && workflow.audit.passed);
    assert.deepEqual(errors, []);
    result.recovery = { pending, final, evidence, restoredAfterMissingStateMethod: true };
    result.passed = true;
    await fs.writeFile(filename, JSON.stringify(result, null, 2) + '\n');
    console.log(JSON.stringify({ passed: true, originalRoute: final.selectedLocalWorkflow,
      modelCallsBefore: pending.timing052.modelCalls, modelCallsAfter: final.timing052.modelCalls,
      verifiedSource: sourceSha256, hostModels: 0 }));
  } finally {
    await app.close();
    verify(process.cwd());
  }
}

main().catch(error => { console.error(error); process.exitCode = 1; });
