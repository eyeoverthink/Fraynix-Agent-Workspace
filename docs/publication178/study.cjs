'use strict';

const assert = require('node:assert/strict');
const {createHash} = require('node:crypto');
const fs = require('node:fs/promises');
const path = require('node:path');
const {launch} = require('../../workspace-desktop/test/calibration-integration.cjs');
const {verify} = require('../../scripts/Study-Build095.cjs');

const output = path.resolve('build', process.argv[2] || 'relay178-live');
const project = path.join(output, 'project');
const hash = value => createHash('sha256').update(value).digest('hex');
function dashboardId(task) {
  const bytes = createHash('md5').update('verified-relay-clearance-v1:' + task).digest();
  bytes[6] = (bytes[6] & 15) | 48;
  bytes[8] = (bytes[8] & 63) | 128;
  return bytes.toString('hex').replace(/^(........)(....)(....)(....)(............)$/, '$1-$2-$3-$4-$5');
}

async function main() {
  const locked = verify(process.cwd());
  await fs.mkdir(output);
  await fs.mkdir(project);
  Object.assign(process.env, {FRAYNIX_WORKSPACE_NO_MODELS:'1', FRAYNIX_WORKSPACE_PERSISTENT:'0',
    FRAYNIX_WORKSPACE_JAVA_HEAP_MIB:'768', FRAYNIX_WORKSPACE_JAVA_PROCESSORS:'2'});
  let app, page;
  const result = {passed:false, source:locked.source, captures:[], errors:[]};
  const save = () => fs.writeFile(path.join(output,'result.json'), JSON.stringify(result,null,2)+'\n');
  const api = (route,args={}) => app.evaluate((_,req)=>global.workspaceTest.api(req.route,req.args),{route,args});
  const renderer = (operation,command,args) => page.evaluate(x=>window.workspace.request(x),{operation,command,args});
  const task = id => api('/api/agency043/read',{id});
  const upstream = id => api('/api/game-tasks115/verify',{id});
  const evidence = id => api('/api/agency043/verify',{id});
  const delay = ms => new Promise(resolve=>setTimeout(resolve,ms));

  async function until(check,label) {
    const end = Date.now()+180000;
    while(Date.now()<end){
      const found=await check();
      if(found)return found;
      await delay(180);
    }
    throw Error('Timed out waiting for '+label);
  }
  async function open() {
    ({app,page}=await launch(project));
    page.on('pageerror',error=>result.errors.push(error.message));
    assert((await app.evaluate(()=>global.workspaceTest.network())).enabled);
    assert.deepEqual((await api('/api/state')).models,[]);
    await app.evaluate(({BrowserWindow})=>BrowserWindow.getAllWindows().find(w=>w.isVisible()).setSize(1480,1020));
  }
  async function close() {
    if(app)await app.close().catch(()=>{});
    app=null;
  }
  function terminal(id,label) {
    let previous;
    return until(async()=>{
      const g=await task(id);
      if(previous!==g.reason&&(!g.reason.includes('tick ')||previous?.split(' · ')[0]!==g.reason.split(' · ')[0]))
        console.log(label+' · '+g.reason);
      previous=g.reason;
      return ['interrupted','completed'].includes(g.status)&&g;
    },label);
  }
  async function mission(id) {
    if(!(await page.locator('#mission044').isVisible()))await page.locator('#mission044-open').click();
    await page.locator('#mission044-task').selectOption(id);
    await page.waitForFunction(id=>document.querySelector('#mission044').dataset.taskId===id,id);
  }
  async function capture(name) {
    await page.evaluate(()=>new Promise(resolve=>requestAnimationFrame(()=>requestAnimationFrame(resolve))));
    const bytes=Buffer.from(await app.evaluate(()=>global.workspaceTest.capture()),'base64');
    await fs.writeFile(path.join(output,name),bytes);
    result.captures.push({name,sha256:hash(bytes)});
    await save();
  }
  function missingDashboard(id) {
    return assert.rejects(()=>api('/api/artifact/read',{id:dashboardId(id)}),/artifact|entity/i);
  }
  async function review(course,pause,label) {
    const goal='Review a local chain: Playtest Relay '+course+', then build a clearance dashboard.'+(pause?' Pause after playtest.':'');
    const prior=new Set((await api('/api/agency043/state')).tasks.map(g=>g.id));
    await page.locator('#relay-review-open').click();
    await page.locator('#relay-review-course').selectOption(course);
    await page.locator('#relay-review-pause').setChecked(pause);
    await page.locator('#relay-review-prepare').click();
    assert.equal(await page.locator('#goal').inputValue(),goal);
    await until(()=>page.locator('#run').isEnabled(),'start');
    await page.locator('#run').click();
    const row=await until(async()=>(await api('/api/agency043/state')).tasks.find(g=>!prior.has(g.id)&&g.goal===goal),label);
    const held=await terminal(row.id,label+' review');
    assert.equal(held.status,'interrupted',held.reason);
    assert.equal(held.relayClearanceChain.course,'Relay '+course);
    assert(!held.relayClearanceChainApproved);
    assert(!held.context044.ownedGameTask115);
    await assert.rejects(()=>api('/api/game-tasks115/read',{id:row.id}));
    await missingDashboard(row.id);
    await assert.rejects(()=>api('/api/agency043/control',{id:row.id,action:'resume'}),/confirm the reviewed chain/i);
    await mission(row.id);
    await until(()=>page.locator('#mission044-confirm').isEnabled(),'confirm');
    assert.match(await page.locator('#mission044-facts').innerText(),/Playtest and replay three paces/);
    if(label==='ridge')await capture('relay-review.png');
    await page.locator('#mission044-confirm').click();
    await until(async()=>(await task(row.id)).relayClearanceChainApproved,'review approved');
    return row.id;
  }
  async function resume(id) {
    await mission(id);
    await until(()=>page.locator('#mission044-resume').isEnabled(),'resume');
    await page.locator('#mission044-resume').click();
    await until(async()=>(await task(id)).resumes===1,'explicit resume observed');
  }
  async function check(id) {
    const g=await terminal(id,'dashboard');
    assert.equal(g.status,'completed',g.reason);
    assert.equal(g.outcome,'verified');
    assert.equal(g.timing052.modelCalls,0);
    const verified=await upstream(id);
    assert.equal(verified.audit.independentReplays,3);
    assert.equal(verified.trials.length,6);
    assert.equal(new Set(verified.trials.map(trial=>trial.sessionId)).size,6);
    const handoff=g.context044.chainHandoff;
    assert.equal(handoff.report.id,verified.report.id);
    const dashboard=g.completion044.relayDashboard;
    assert.equal(dashboard.id,dashboardId(id));
    const artifact=await api('/api/artifact/read',{id:dashboard.id});
    assert.deepEqual(JSON.parse(artifact.contents['playtest-source.json']),handoff);
    assert.match(artifact.contents['crossings.csv'],/^pace,rail,entry_game_units_per_second,clearance_game_units,finish_tick\n/);
    const rows=artifact.contents['crossings.csv'].trim().split('\n').slice(1).map(line=>{
      const [pace,rail,speed,clearance,finish]=line.split(',');
      return {pace,rail:JSON.parse(rail),speed:Number(speed),clearance:Number(clearance),finish:Number(finish)};
    });
    const expected=verified.audit.runs.flatMap(run=>run.crossings.map(c=>({pace:run.pace,rail:c.name,
      speed:c.entrySpeed,clearance:c.measuredClearance,finish:run.finishTick})));
    assert.deepEqual(rows,expected);
    assert.equal(Math.min(...rows.map(row=>row.clearance)),verified.audit.minimumClearance);
    assert(artifact.test.passed);
    assert(artifact.test.checks.every(row=>row.passed));
    for(const pace of ['normal','steady','careful','all'])
      assert(artifact.test.steps.some(step=>step.action==='click'&&step.selector.includes("'"+pace+"'")));
    const gate=g.history.findIndex(e=>e.kind==='chain-handoff-verified');
    const shown=g.history.findIndex(e=>e.kind==='surface-action'&&e.detail.tool==='relayDashboard.check');
    assert(gate>g.history.findIndex(e=>e.kind==='chain-confirmed')&&shown>gate);
    const proof=await evidence(id);
    assert(proof.passed&&proof.relay&&proof.relay.handoffSha256);
    return {id, verified, dashboard, chromium:artifact.test, proof, rows};
  }
  async function revise(id) {
    const a=await api('/api/artifact/read',{id});
    await api('/api/artifact/write',{id,expectedVersion:a.version,title:'Deliberate revision in the isolated test project',
      sources:a.sources,files:{...a.contents,'index.html':a.contents['index.html']+'\n<!-- changed after verification -->'}});
  }

  try{
    await open();
    const ridge=await review('Ridge',true,'ridge');
    const pause=await terminal(ridge,'ridge checkpoint');
    assert.match(pause.reason,/Playtest verified/);
    assert(!pause.context044.relayDashboard);
    await missingDashboard(ridge);
    const before=await upstream(ridge);
    await mission(ridge);
    await until(()=>page.locator('#mission044-result').isEnabled(),'open first result');
    await page.locator('#mission044-result').click();
    await until(async()=>await app.evaluate(()=>global.workspaceTest.canvasRead('document.querySelector("#outcome")?.textContent')),'original canvas');
    await capture('relay-first-verified.png');
    result.boundary={handoff:pause.context044.chainHandoff,report:before.report,trials:before.trials};
    assert(await app.evaluate(()=>global.workspaceTest.crashEngine()));
    await delay(300);
    await close();
    await open();
    await missingDashboard(ridge);
    assert.deepEqual(await upstream(ridge),before);
    await resume(ridge);
    result.recovered=await check(ridge);
    assert.deepEqual(result.recovered.verified,before);
    await mission(ridge);
    await until(()=>page.locator('#mission044-result').isEnabled(),'completed dashboard available');
    await page.locator('#mission044-result').click();
    await until(async()=>await app.evaluate(()=>global.workspaceTest.canvasRead('document.querySelector("#minimum")?.textContent')),'dashboard canvas');
    await capture('relay-dashboard.png');

    const switchyard=await review('Switchyard',false,'switchyard');
    result.automatic=await check(switchyard);
    assert.notEqual(result.recovered.verified.source.manifestSha256,result.automatic.verified.source.manifestSha256);
    await revise(result.automatic.dashboard.id);
    await assert.rejects(()=>evidence(switchyard),/dashboard changed/i);
    result.revisedDashboardRefused=true;

    const changed=await review('Switchyard',true,'revised playtest');
    const checkpoint=await terminal(changed,'revision checkpoint');
    assert(checkpoint.context044.chainHandoff);
    await missingDashboard(changed);
    await revise(checkpoint.context044.chainHandoff.report.id);
    await resume(changed);
    const refused=await terminal(changed,'changed source refused');
    assert.match(refused.reason,/saved Relay report changed/i);
    await missingDashboard(changed);
    result.revisedPlaytestRefused=refused.reason;
    await mission(changed);
    await capture('relay-refused.png');

    const history=await api('/api/agency043/history');
    assert(history.records.some(row=>row.id===ridge&&row.eligible));
    await renderer('agency043','archive',{planSha256:history.planSha256,ids:[ridge]});
    await close();
    await open();
    const opened=await renderer('history113','open',{id:ridge,kind:'chat'});
    assert.equal(opened.artifact.id,result.recovered.dashboard.id);
    assert.deepEqual(opened.proof.relay,result.recovered.proof.relay);
    result.archive={passed:true, report:opened.artifact.id, proof:opened.proof.relay};
    assert.deepEqual(result.errors,[]);
    result.passed=true;
    console.log(JSON.stringify({passed:true, ridge:result.recovered.rows.length, switchyard:result.automatic.rows.length,
      ridgeChecks:result.recovered.chromium.checks.length,switchyardChecks:result.automatic.chromium.checks.length,
      revisedPlaytestRefused:result.revisedPlaytestRefused,archive:result.archive.passed}));
  }finally{
    await close();
    await save();
    verify(process.cwd());
  }
}
main().catch(error=>{console.error(error);process.exitCode=1;});
