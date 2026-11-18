'use strict';

const assert = require('node:assert/strict');
const {createHash} = require('node:crypto');
const fs = require('node:fs/promises');
const path = require('node:path');
const {launch} = require('../../workspace-desktop/test/calibration-integration.cjs');
const {verify} = require('../../scripts/Study-Build095.cjs');

const output = path.resolve('build', process.argv[2] || 'ramp172');
const project = path.join(output, 'project');
const wait = ms => new Promise(resolve => setTimeout(resolve, ms));

async function main() {
  const build = verify(process.cwd());
  await fs.mkdir(output);
  await fs.mkdir(project);
  Object.assign(process.env, {FRAYNIX_WORKSPACE_NO_MODELS:'1', FRAYNIX_WORKSPACE_PERSISTENT:'0',
    FRAYNIX_WORKSPACE_JAVA_HEAP_MIB:'768', FRAYNIX_WORKSPACE_JAVA_PROCESSORS:'2'});
  const record = {passed:false, source:build.source, captures:[], errors:[]};
  let app, page;
  const api = (route, args) => app.evaluate((_, req) => global.workspaceTest.api(req.route, req.args ?? undefined), {route,args:args ?? null});
  const parent = id => api('/api/agency043/read', {id});
  const child = id => api('/api/ramp-mission/read', {id});
  const proof = id => api('/api/ramp-mission/verify', {id});
  const canvas = code => app.evaluate((_, js)=>global.workspaceTest.canvasRead(js), code);
  const request = (operation, command, args) => page.evaluate(x=>window.workspace.request(x), {operation,command,args});
  const origin = () => app.evaluate(()=>global.workspaceTest.origin());

  async function until(read, description, timeout=35000) {
    const end = Date.now()+timeout;
    while(Date.now()<end) {
      const value=await read();
      if(value)return value;
      await wait(100);
    }
    throw Error('Timed out: '+description);
  }
  async function start() {
    ({app,page}=await launch(project));
    page.on('pageerror', error=>record.errors.push(error.message));
    await app.evaluate(({BrowserWindow})=>BrowserWindow.getAllWindows().find(w=>w.isVisible()).setSize(1540,1020));
    assert.deepEqual((await api('/api/state')).models,[]);
    assert.deepEqual((await api('/api/agency043/models',{})).models,[]);
    assert((await app.evaluate(()=>global.workspaceTest.network())).enabled);
    await page.locator('#ramp-input-open').waitFor();
  }
  async function capture(name) {
    await page.evaluate(()=>new Promise(resolve=>requestAnimationFrame(()=>requestAnimationFrame(resolve))));
    const png=Buffer.from(await app.evaluate(()=>global.workspaceTest.capture()),'base64');
    await fs.writeFile(path.join(output,name),png);
    record.captures.push({name,sha256:createHash('sha256').update(png).digest('hex')});
  }
  async function saveInput(values, pause) {
    await page.locator('#ramp-input-open').click();
    const dialog=page.locator('#ramp-input-dialog');
    await dialog.waitFor();
    for(const [key,value]of Object.entries(values))await page.locator('#ramp-'+key).fill(String(value));
    await page.locator('#ramp-pause').setChecked(pause);
    await page.locator('#ramp-input-save').click();
    await dialog.waitFor({state:'hidden'});
    assert.equal(await page.locator('#goal').inputValue(),
      pause?'Run the local ramp experiment and pause after reading.':'Run the local ramp experiment.');
    const url=new URL(await page.locator('#address').inputValue());
    const source=await api('/api/ramp-mission/bind',{path:url.pathname});
    for(const [key,value]of Object.entries(values))assert.equal(source.values[key],value);
    return source;
  }
  async function viewSource(source) {
    await page.locator('#address').fill(await origin()+source.path);
    await page.locator('#navigate').click();
    await until(async()=>page.locator('#browser-title').textContent().then(t=>t.includes('Ramp experiment input')),'source page title');
  }
  async function submit() {
    const existing=new Set((await api('/api/agency043/state',{})).tasks.map(t=>t.id));
    await page.locator('#run').click();
    return until(async()=>(await api('/api/agency043/state',{})).tasks.find(t=>!existing.has(t.id)),'new chat task');
  }
  async function terminal(id,status) {
    return until(async()=>{
      const task=await parent(id);
      if(task.status===status)return task;
      if(['interrupted','cancelled','completed'].includes(task.status))throw Error(task.reason);
    }, status);
  }
  async function mission(id) {
    await page.locator('#mission044-open').click();
    await page.locator('#mission044-task').selectOption(id);
    await page.waitForFunction(actual=>document.querySelector('#mission044').dataset.taskId===actual,id);
  }
  async function completed(id) {
    const p=await terminal(id,'completed');
    assert.equal(p.family044,'ramp-mission');
    assert.equal(p.outcome,'verified');
    assert.equal(p.timing052.modelCalls,0);
    const verified=await proof(id);
    assert.equal(verified.canvasReceipt.checks.length,9);
    assert.deepEqual(verified.stages.map(s=>s.name),['browser','experiment','replay','artifact','canvas']);
    assert.equal(verified.audit.passed,true);
    assert.notEqual(verified.original.sessionId,verified.replay.sessionId);
    assert.deepEqual(p.completion044.rampMission.audit,verified.audit);
    const saved=await api('/api/artifact/read',{id:verified.report.id});
    assert.equal(saved.test.passed,true);
    const evidence=JSON.parse(saved.contents['experiment.json']);
    assert.deepEqual(evidence.audit,verified.audit);
    assert.deepEqual(evidence.observation,verified.observation);
    assert(evidence.original.samples.at(-1).tick>0);
    assert.equal(evidence.original.samples.at(-1).distanceM,verified.audit.distanceM);
    return {parent:p,verified,evidence};
  }
  try {
    await start();
    const v1=await saveInput({angleDeg:25,friction:.08,seconds:1},true);
    record.sourceOne=v1;
    await capture('ramp-input.png');
    const one=await submit();
    await terminal(one.id,'interrupted');
    record.beforeStop=await child(one.id);
    assert.equal(record.beforeStop.stage,'experiment');
    assert.deepEqual(record.beforeStop.source,v1);
    assert.deepEqual(record.beforeStop.stages.map(x=>x.name),['browser']);
    await assert.rejects(proof(one.id),/incomplete/i);
    await mission(one.id);
    await capture('ramp-checkpoint.png');
    assert(await app.evaluate(()=>global.workspaceTest.crashEngine()));
    await wait(200);
    await app.close().catch(()=>{});
    app=null;

    await start();
    assert.deepEqual(await child(one.id),record.beforeStop);
    await mission(one.id);
    // Leave the real Java work running yet keep the report's host effect unclaimed.
    await app.evaluate(()=>global.workspaceTest.agency043.stop());
    await page.locator('#mission044-resume').click();
    await until(async()=>{
      const state=await parent(one.id);
      return state.resumes===1 && state.status==='waiting-host' && state.effect?.tool==='localRamp.check';
    },'native replay retained before the canvas checks');
    record.beforeReportStop=await child(one.id);
    assert.equal(record.beforeReportStop.status,'ready-report');
    assert.deepEqual(record.beforeReportStop.stages.map(x=>x.name),['browser','experiment','replay','artifact']);
    assert(await app.evaluate(()=>global.workspaceTest.crashEngine()));
    await wait(200);
    await app.close().catch(()=>{});
    app=null;
    await start();
    assert.deepEqual(await child(one.id),record.beforeReportStop);
    await mission(one.id);
    await page.locator('#mission044-resume').click();
    await until(async()=>(await parent(one.id)).resumes===2,'second explicit resume admitted');
    record.one=await completed(one.id);
    assert.deepEqual(record.one.verified.stages.slice(0,4),record.beforeReportStop.stages);
    assert.equal(record.one.verified.original.sessionId,record.beforeReportStop.original.sessionId);
    assert.equal(record.one.verified.replay.sessionId,record.beforeReportStop.replay.sessionId);
    assert.deepEqual(record.one.verified.stages[0],record.beforeStop.stages[0]);
    await page.waitForFunction(()=>document.querySelector('#test-badge').textContent==='TESTS PASSED');
    assert.equal(await canvas("document.querySelector('#revision').textContent"),'Input revision 1');
    assert.equal(await canvas("document.querySelector('#replay-toggle').textContent"),'Show replay');
    await viewSource(v1);
    await capture('ramp-verified.png');

    const v2=await saveInput({angleDeg:39,friction:.04,seconds:1.2},false);
    assert.equal(v2.version,2);
    assert.equal(v2.id,v1.id);
    assert.notEqual(v2.pageSha256,v1.pageSha256);
    const two=await submit();
    record.two=await completed(two.id);
    assert.equal(record.two.verified.source.version,2);
    assert.notEqual(record.two.verified.report.id,record.one.verified.report.id);
    assert(record.two.verified.audit.distanceM-record.one.verified.audit.distanceM>.5);
    await capture('ramp-changed-input.png');

    const v3=await saveInput({angleDeg:30,seconds:.8},false);
    await app.evaluate(({webContents})=>{
      const {browser}=global.workspaceTest.surfaces();
      return webContents.fromId(browser).executeJavaScript("document.querySelector('[data-field=angleDeg] td').textContent='7'; true");
    });
    const forged=await submit();
    record.forged=await terminal(forged.id,'interrupted');
    assert.match(record.forged.reason,/visible browser table differs/i);
    assert.equal((await child(forged.id)).stages.length,0);
    assert.equal(record.forged.timing052.modelCalls,0);

    const v4=await saveInput({angleDeg:20,seconds:.75},true);
    assert.equal(v4.version,v3.version+1);
    const obsolete=await submit();
    await terminal(obsolete.id,'interrupted');
    const v5=await saveInput({angleDeg:28},false);
    assert.equal(v5.version,v4.version+1);
    await mission(obsolete.id);
    await page.locator('#mission044-resume').click();
    record.stale=await until(async()=>{
      const state=await parent(obsolete.id);
      return state.resumes===1 && state.status==='interrupted' && state;
    },'stale source refused');
    assert.match(record.stale.reason,/revision changed/i);
    assert.equal(record.stale.timing052.modelCalls,0);
    assert.equal((await child(obsolete.id)).stages.length,1);
    await assert.rejects(proof(obsolete.id),/incomplete/i);

    await app.close();
    app=null;
    await start();
    assert.deepEqual(await proof(one.id),record.one.verified);
    await mission(one.id);
    const opened=await request('agency043','mission-open',{id:one.id});
    assert.equal(opened.id,record.one.verified.report.id);
    assert.equal(await canvas("document.querySelector('#revision').textContent"),'Input revision 1');
    await capture('ramp-reopened.png');
    const history=await api('/api/agency043/history',{});
    assert(history.records.find(row=>row.id===one.id).eligible);
    const archived=await api('/api/agency043/archive',{ids:[one.id],planSha256:history.planSha256});
    assert.deepEqual(archived.archivedNow,[one.id]);
    record.archived=await api('/api/agency043/verify',{id:one.id});
    assert.equal(record.archived.passed,true);
    const archiveResult=await request('history113','open',{id:one.id,kind:'chat'});
    assert.equal(archiveResult.artifact.id,opened.id);
    assert.deepEqual(record.errors,[]);
    verify(process.cwd());
    record.passed=true;
    console.log(JSON.stringify({passed:true, first:record.one.verified.audit, second:record.two.verified.audit,
      forged:record.forged.reason, stale:record.stale.reason}));
  } finally {
    if(app)await app.close().catch(()=>{});
    await fs.writeFile(path.join(output,'result.json'),JSON.stringify(record,null,2)+'\n');
  }
}

main().catch(error=>{console.error(error);process.exitCode=1;});
