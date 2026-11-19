'use strict';

const assert = require('node:assert/strict');
const {createHash} = require('node:crypto');
const fs = require('node:fs/promises');
const path = require('node:path');
const {launch} = require('../../workspace-desktop/test/calibration-integration.cjs');
const {verify} = require('../../scripts/Study-Build095.cjs');

const output = path.resolve('build',process.argv[2]||'recommend179-live');
const project = path.join(output,'project');
const model = 'gpt-oss:20b';
const header = 'angle_deg,friction,gravity_mps2,mass_kg,duration_s\n';
const rampA = header+'36,0.08,9.81,1.2,0.85\n21,0.24,9.81,0.9,0.85\n';
const rampB = header+'23,0.21,9.81,1,0.76\n33,0.1,9.81,1.2,0.76\n';
// Order, expected labels and missing-input cases are fixed before the first model call.
const fixtures = [
  {id:'ramp', want:'ramp', text:'Set both slopes going for the same time. Which block gets farther? I want the winning block’s motion broken down.', input:'ramp', action:'complete'},
  {id:'ridge', want:'relay', text:'Take our robot around Relay Ridge at each pace. Recheck the run and give me a dashboard of how much room it had over the rails.', input:'ramp', action:'recover'},
  {id:'ambiguous', want:'clarify', text:'I’m deciding between seeing which incline wins and checking the robot’s clearance on Relay Switchyard. I haven’t picked one yet.', input:'ramp', action:'question'},
  {id:'switchyard', want:'relay', text:'On Relay Switchyard, try all the driving speeds and show the height margin over each rail. Repeat the runs to make sure the measurements hold.', action:'decline'},
  {id:'missing-ramp', want:'ramp', text:'Find out which of my two inclines lets its block travel farther, then give me a motion breakdown for that one.', action:'question'},
  {id:'out-of-menu', want:'clarify', text:'Use my existing spreadsheet of shop orders and make an inventory tally. There is no game run or simulation to do.', input:'ramp', action:'question'},
  {id:'missing-course', want:'relay', text:'Have the robot play the Relay course at each pace, rerun it to check and show me clearances. I haven’t picked which circuit.', action:'question'},
  {id:'revised-ramp', want:'ramp', text:'Put the two prepared downhill configurations on one clock, then turn the longer slide’s samples into a motion summary.', input:'ramp', action:'revise'}
];
const digest = bytes => createHash('sha256').update(bytes).digest('hex');
const delay = ms => new Promise(resolve=>setTimeout(resolve,ms));

async function main() {
  const lock = verify(process.cwd());
  await fs.mkdir(output);
  await fs.mkdir(project);
  Object.assign(process.env,{FRAYNIX_WORKSPACE_PERSISTENT:'0',FRAYNIX_WORKSPACE_JAVA_HEAP_MIB:'768',
    FRAYNIX_WORKSPACE_JAVA_PROCESSORS:'2'});
  const result={passed:false, source:lock.source, model, fixtures, trials:[], captures:[], errors:[]};
  let app, page;
  const save=()=>fs.writeFile(path.join(output,'result.json'),JSON.stringify(result,null,2)+'\n');
  const api=(route,args={})=>app.evaluate((_,x)=>global.workspaceTest.api(x.route,x.args),{route,args});
  const state=()=>api('/api/agency043/state');
  const task=id=>api('/api/agency043/read',{id});
  const control=(id,action)=>api('/api/agency043/control',{id,action});
  const ui=(operation,command,args)=>page.evaluate(x=>window.workspace.request(x),{operation,command,args});

  async function until(check,label) {
    for(let end=Date.now()+185000;Date.now()<end;){
      const value=await check();
      if(value)return value;
      await delay(170);
    }
    throw Error('Timed out waiting for '+label);
  }
  async function open(withModel=true) {
    if(withModel)delete process.env.FRAYNIX_WORKSPACE_NO_MODELS;
    else process.env.FRAYNIX_WORKSPACE_NO_MODELS='1';
    ({app,page}=await launch(project));
    page.on('pageerror',error=>result.errors.push(error.message));
    assert((await app.evaluate(()=>global.workspaceTest.network())).enabled);
    await app.evaluate(({BrowserWindow})=>BrowserWindow.getAllWindows().find(w=>w.isVisible()).setSize(1490,1030));
    if(withModel){
      await until(()=>page.locator('#model option').evaluateAll(items=>items.some(x=>x.value==='gpt-oss:20b')),model);
      await page.locator('#model').selectOption(model);
    }else assert.deepEqual((await app.evaluate(()=>global.workspaceTest.api('/api/state'))).models,[]);
  }
  async function close() {
    if(app)await app.close().catch(()=>{});
    app=null;
  }
  async function idle() {
    await until(async()=>!(await app.evaluate(()=>global.workspaceTest.agency043.busy())),'idle desktop');
  }
  async function mission(id) {
    if(!(await page.locator('#mission044').isVisible()))await page.locator('#mission044-open').click();
    await page.locator('#mission044-task').selectOption(id);
    await page.waitForFunction(id=>document.querySelector('#mission044').dataset.taskId===id,id);
  }
  async function capture(name,id) {
    await mission(id);
    await page.evaluate(()=>new Promise(resolve=>requestAnimationFrame(()=>requestAnimationFrame(resolve))));
    const png=Buffer.from(await app.evaluate(()=>global.workspaceTest.capture()),'base64');
    await fs.writeFile(path.join(output,name),png);
    result.captures.push({name,sha256:digest(png)});
  }
  function terminal(id,condition) {
    return until(async()=>{
      const g=await task(id);
      return ['interrupted','completed','cancelled'].includes(g.status)&&condition(g)&&g;
    },id+' terminal');
  }
  async function chooseSource(fixture) {
    if(fixture.input==='ramp')return ui('rampInput','save',{csv:fixture.id==='revised-ramp'?rampB:rampA});
    const welcome=(await app.evaluate(()=>global.workspaceTest.origin()))+'/welcome';
    await page.evaluate(url=>window.workspace.request({operation:'tool',tool:'browser.navigate',args:{url}}),welcome);
    return null;
  }
  async function noWork(id) {
    await assert.rejects(()=>api('/api/ramp-mission/read',{id}));
    await assert.rejects(()=>api('/api/game-tasks115/read',{id}));
    const c=(await task(id)).context044;
    for(const key of ['chainHandoff','ownedRampMission','ownedGameTask115','ownedWorkflowId','relayDashboard'])
      assert(!c[key],key+' started before approval');
  }
  async function recommend(fixture) {
    await idle();
    const source=await chooseSource(fixture);
    const before=new Set((await state()).tasks.map(g=>g.id));
    await page.locator('#goal').fill(fixture.text);
    const button=page.locator('#chain-recommend');
    await until(()=>button.isEnabled(),'recommendation action');
    await button.click();
    const row=await until(async()=> (await state()).tasks.find(g=>!before.has(g.id)&&g.goal===fixture.text),fixture.id+' admission');
    await assert.rejects(()=>control(row.id,'confirm-chain'));
    const held=await terminal(row.id,g=>g.timing052.modelCalls>0);
    await idle();
    await noWork(row.id);
    const calls=held.history.filter(e=>e.kind==='model-call');
    assert.equal(calls.length,1);
    const call=calls[0].detail;
    assert(call.returned&&call.providerEvidence.eval_count>0,JSON.stringify(call));
    const plan=held.history.find(e=>e.kind==='plan')?.detail;
    assert.deepEqual(plan.allowed,['chain.recommend']);
    assert.equal(plan.model.model,model);
    const selected=held.recommendedChain;
    if(selected)assert.equal(selected.modelDigest,plan.model.digest);
    const actual=selected?.choice||'unparsed';
    const trial={fixture, id:row.id, actual, matched:actual===fixture.want, modelCall:call, plan,
      decision:selected, firstStatus:held.status, firstReason:held.reason, approvalRequired:true};
    if(held.rampSource&&source)assert.equal(held.rampSource.version,source.version);
    if(held.rampTraceChain||held.relayClearanceChain)await assert.rejects(()=>control(row.id,'resume'),/confirm the reviewed chain/i);
    else if(held.chainRecommendationQuestion)await assert.rejects(()=>control(row.id,'resume'),/new recommendation|choose one outcome/i);
    result.trials.push(trial);
    await save();
    console.log(fixture.id+' · '+actual+' · '+Math.round(call.durationMs)+' ms · '+held.reason);
    return {held,source,trial};
  }
  async function approve(id) {
    await mission(id);
    const button=page.locator('#mission044-confirm');
    await until(()=>button.isEnabled(),'review approval');
    await button.click();
    await until(async()=>{
      const g=await task(id);
      return g.rampTraceChainApproved||g.relayClearanceChainApproved;
    },'explicit Java approval');
  }
  async function checkFinished(trial,source) {
    const g=await terminal(trial.id,g=>g.rampTraceChainApproved||g.relayClearanceChainApproved);
    assert.equal(g.status,'completed',g.reason);
    assert.equal(g.outcome,'verified');
    assert.equal(g.timing052.modelCalls,1);
    const approved=g.history.findIndex(e=>e.kind==='chain-confirmed');
    assert(approved>g.history.findIndex(e=>e.kind==='chain-recommended'));
    assert(g.history.findIndex(e=>e.kind==='chain-handoff-verified')>approved);
    const proof=await api('/api/agency043/verify',{id:trial.id});
    assert(proof.passed);
    if(trial.actual==='ramp'){
      assert(proof.ramp&&proof.workflow&&proof.handoffSha256);
      assert.equal(g.completion044.rampMission.source.id,source.id);
      assert.equal(g.completion044.rampMission.source.version,source.version);
      assert.equal(g.completion044.rampMission.source.csvSha256,source.csvSha256);
    }else{
      assert(proof.relay?.receiptSha256);
      assert.equal(g.completion044.javaGameTask115.audit.crossings,9);
    }
    trial.completion=g.completion044;
    trial.proof=proof;
    await idle();
    await ui('agency043','mission-open',{id:trial.id});
    await capture(trial.actual+'-verified.png',trial.id);
  }

  try{
    await open();
    for(const fixture of fixtures){
      const {held,source,trial}=await recommend(fixture);
      if(!trial.matched){
        await control(trial.id,'cancel');
        trial.disposition='model mismatch; no approval';
        continue;
      }
      if(fixture.action==='question'){
        assert(held.chainRecommendationQuestion,held.reason);
        assert(!held.rampTraceChain&&!held.relayClearanceChain);
        await assert.rejects(()=>control(trial.id,'confirm-chain'));
        trial.disposition='clarified without execution';
        if(fixture.id==='ambiguous')await capture('ambiguous.png',trial.id);
      }else if(fixture.action==='decline'){
        await mission(trial.id);
        await until(()=>page.locator('#mission044-stop').isEnabled(),'decline available');
        await page.locator('#mission044-stop').click();
        await until(async()=>(await task(trial.id)).status==='cancelled','declined');
        await noWork(trial.id);
        trial.disposition='declined without execution';
      }else if(fixture.action==='revise'){
        const changed=await ui('rampInput','save',{id:source.id,expectedVersion:source.version,csv:rampA});
        assert.equal(changed.id,source.id);
        assert.equal(changed.version,source.version+1);
        await approve(trial.id);
        const refused=await terminal(trial.id,g=>g.rampTraceChainApproved);
        assert.match(refused.reason,/changed|revision/i);
        assert(!refused.context044.ownedWorkflowId&&!refused.context044.chainHandoff);
        trial.disposition='revised source refused';
        trial.refusal=refused.reason;
      }else{
        await capture(trial.actual+'-review.png',trial.id);
        if(fixture.action==='recover'){
          assert(await app.evaluate(()=>global.workspaceTest.crashEngine()));
          await delay(300);
          await close();
          await open(false);
          const restored=await task(trial.id);
          assert.deepEqual(restored.recommendedChain,held.recommendedChain);
          assert.deepEqual(restored.relaySource,held.relaySource);
          await noWork(trial.id);
          await assert.rejects(()=>control(trial.id,'resume'),/confirm the reviewed chain/i);
          trial.modelDisabledRestart=true;
        }else{
          const other=await ui('rampInput','save',{csv:rampB});
          assert.notEqual(other.id,source.id);
        }
        await approve(trial.id);
        await checkFinished(trial,source);
        trial.disposition='confirmed and verified';
        if(fixture.action==='recover'){
          await close();
          await open();
        }
      }
      await save();
    }
    assert.deepEqual(result.errors,[]);
    assert.equal(result.trials.length,fixtures.length);
    assert(result.trials.find(t=>t.fixture.id==='ramp')?.proof?.passed);
    assert(result.trials.find(t=>t.fixture.id==='ridge')?.proof?.passed);
    result.matched=result.trials.filter(row=>row.matched).length;
    result.passed=true;
    console.log(JSON.stringify({passed:true, matched:result.matched,total:result.trials.length}));
  }finally{
    await close();
    await save();
    verify(process.cwd());
  }
}
main().catch(error=>{console.error(error);process.exitCode=1;});
