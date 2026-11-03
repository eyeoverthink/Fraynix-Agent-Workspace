'use strict';
const {launch}=require('./calibration-integration.cjs'),fs=require('node:fs/promises'),path=require('node:path'),os=require('node:os'),assert=require('node:assert/strict');
const sleep=ms=>new Promise(r=>setTimeout(r,ms));
async function main(){
 process.env.FRAYNIX_WORKSPACE_PERSISTENT='0';process.env.FRAYNIX_WORKSPACE_PLAN_CPU_ONLY='1';
 const project=await fs.mkdtemp(path.join(os.tmpdir(),'fraynix-chat052-')),out=path.resolve(__dirname,'../test-output');await fs.mkdir(out,{recursive:true});const checks=[],errors=[];let app,page;const live=true;
 async function open(){({app,page}=await launch(project));page.on('pageerror',e=>errors.push(e.message));await page.locator('#spatial-tab').click();await page.locator('#construction051').evaluate(e=>e.open=true);await page.waitForFunction(()=>!document.querySelector('#c051-create').disabled);}
 const api=(route,args={})=>app.evaluate((_,v)=>global.workspaceTest.api(v.route,v.args),{route,args});
 async function until(fn,ms=120000){const end=Date.now()+ms;while(Date.now()<end){const value=await fn();if(value)return value;await sleep(150);}throw Error('Actual construction state did not arrive');}
 async function capture(name){await page.locator('#spatial-tab').click();await page.locator('#construction051').evaluate(e=>e.open=true);await page.locator('#c051-stage').evaluate(e=>e.scrollIntoView({block:'center'}));await page.evaluate(()=>new Promise(r=>requestAnimationFrame(()=>requestAnimationFrame(r))));await fs.writeFile(path.join(out,name),Buffer.from(await app.evaluate(()=>global.workspaceTest.capture()),'base64'));}
 async function latest(worldId){return (await api('/api/goals/list')).goals.filter(g=>g.construction051&&g.worldBinding?.worldId===worldId).at(-1);}
 async function finished(worldId,prior){return until(async()=>{const g=await latest(worldId);if(!g||g.id===prior)return null;if(['interrupted','failed','cancelled'].includes(g.status))throw Error(JSON.stringify(g));return g.status==='completed'?g:null;});}
 function checkChat(g){
  assert.equal(g.outcome,'verified');assert.equal(g.timing052.modelCalls,1);assert.equal(g.timing052.failedModelCalls,0);assert.equal(g.timing052.terminalChecksWithoutModel,1);
  assert(g.timing052.modelMillis>0);assert(g.summary.length<1000);assert(!g.summary.includes('Actual metrics:'));
  assert(g.history.some(r=>r.kind==='admitted'));assert(!g.history.some(r=>r.kind==='proposal'&&r.detail.tool==='finish'));
  const plans=g.history.filter(r=>r.kind==='plan');assert.equal(plans.length,1);assert(plans.every(r=>r.detail.compute.cpuOnly&&r.detail.compute.options.num_gpu===0&&r.detail.compute.options.num_thread===4&&r.detail.model.digest===g.modelDigest));
  assert(g.completion044.javaGoal.metrics.nativeTicks>0);assert(g.completion044.javaGoal.poseView.passed);
 }
 const report={project,live,checks,source:require('node:child_process').execFileSync('git',['rev-parse','HEAD'],{encoding:'utf8'}).trim()};
 try{
  await open();await page.locator('#c051-create').click();const id=await until(async()=>await page.locator('#c051-world').inputValue());report.worldId=id;const before=await api('/api/world/observe',{worldId:id});report.before=before;
  await page.locator('#c051-build').click();await until(async()=>{const s=await api('/api/world/observe',{worldId:id});return s.nativeObservation?.sample?.tick>24;});await capture('chat052-live.png');
  const first=await finished(id);report.first=first;assert.equal(first.worldCommit.revision,1);assert.equal(first.metrics.grabs,3);assert.equal(first.metrics.releases,3);assert.equal(first.constructionVerification051.supportContacts,2);assert(first.poseView.passed);assert(first.savedSkill);await capture('chat052-built.png');checks.push('Actual UI runs three native gripper placements, verifies support contacts and original report, commits revision 1 and saves a construction skill');
  const current=await api('/api/world/observe',{worldId:id});assert(current.construction051.passed);await assert.rejects(api('/api/construction051/start',{worldId:id,expectedRevision:0,expectedHash:before.expectedHash}),/Stale/);
  await app.close();app=null;await open();assert.deepEqual((await api('/api/world/observe',{worldId:id})).frame,current.frame);await until(async()=>await page.locator('#c051-world').inputValue()===id);checks.push('Fresh Java process restores the exact complete constructed frame and stable body IDs; stale observation refused');
  await page.locator('#c051-x').fill('0.45');await page.locator('#c051-z').fill('0.3');await page.locator('#c051-skill').fill('bridge-rebuilt');await page.locator('#c051-build').click();const second=await finished(id,first.id);report.second=second;assert.equal(second.worldCommit.revision,2);assert.equal(second.metrics.grabs,4);assert(second.constructionVerification051.passed);const run=await api('/api/world/run',{id:second.runId});assert.deepEqual(run.definition.worldFrame,current.frame);await capture('chat052-rebuilt.png');checks.push('Second goal stages the beam, relocates supports and rebuilds the same bodies from the exact prior frame, earning revision 2 with four verified releases');
  await page.locator('#skills-tab').click();await page.locator('#library-items button').filter({hasText:'bridge-assembly'}).first().click();await page.waitForFunction(()=>document.querySelector('#construction051').open&&!document.querySelector('#c051-reuse').disabled);checks.push('Shared verified-skill library opens the retained construction procedure in the same app');
  if(live){
   await page.waitForFunction(()=>[...document.querySelector('#model').options].some(o=>o.value==='gpt-oss:20b'));await page.locator('#model').selectOption('gpt-oss:20b');await sleep(800);await page.locator('#c051-x').fill('-0.35');await page.locator('#c051-z').fill('0.1');await page.locator('#c051-reuse').click();
   const task=await until(async()=>(await api('/api/agency043/state')).tasks.at(-1));assert.equal(task.family044,'construction');assert.match(task.goal,/Reuse the procedure bridge-assembly version 1/);
   const final=await until(async()=>{const t=await api('/api/agency043/read',{id:task.id});if(['interrupted','cancelled'].includes(t.status))throw Error(t.reason);return t.status==='completed'?t:null;},900000);report.liveReuse=final;checkChat(final);assert.equal(final.completion044.javaGoal.worldCommit.revision,3);assert(final.completion044.javaGoal.constructionVerification051.passed);await capture('chat052-chat.png');checks.push('Actual installed gpt-oss:20b chat selects the retained exact construction procedure and earns a new revision in the same world');
   await until(async()=>!(await page.locator('#run').isDisabled()));await page.locator('#c051-x').fill('0.2');await page.locator('#c051-z').fill('-0.2');await page.locator('#c051-skill').fill('chat-built-bridge');await page.locator('#c051-chat').click();const fresh=await until(async()=>(await api('/api/agency043/state')).tasks.findLast(t=>t.id!==task.id&&t.family044==='construction'));const built=await until(async()=>{const t=await api('/api/agency043/read',{id:fresh.id});if(['interrupted','cancelled'].includes(t.status))throw Error(t.reason);return t.status==='completed'?t:null;},900000);report.liveStart=built;checkChat(built);assert.equal(built.completion044.javaGoal.worldCommit.revision,4);assert.equal(built.completion044.javaGoal.savedSkill.name,'chat-built-bridge');assert.equal(built.completion044.javaGoal.construction051.centerX,.2);assert.equal(built.completion044.javaGoal.construction051.centerZ,-.2);await capture('chat052-chat-built.png');checks.push('A second actual local-model request uses construction.start with exact requested coordinates and saves a newly verified skill at revision 4');
  }
  await page.locator('#mission044-open').click();
  await page.waitForFunction(()=>document.querySelector('#mission044').dataset.status==='completed');
  assert.match(await page.locator('#mission044-facts').innerText(),/Model requests: 1/);
  assert.match(await page.locator('#mission052-outcome').innerText(),/World revision 4 saved/);
  await page.locator('#mission052-details').evaluate(e=>e.open=true);
  assert.match(await page.locator('#mission052-evidence').innerText(),/constructionVerification051/);
  await fs.writeFile(path.join(out,'chat052-evidence.png'),Buffer.from(await app.evaluate(()=>global.workspaceTest.capture()),'base64'));
  await page.locator('#mission052-details').evaluate(e=>e.open=false);
  await fs.writeFile(path.join(out,'chat052-mission.png'),Buffer.from(await app.evaluate(()=>global.workspaceTest.capture()),'base64'));
  checks.push('Mission shows measured model time, concise verified result and expandable original completion evidence');
  await page.locator('#c051-export').click();
  await until(async()=>(await page.locator('#notice').innerText()).includes('Complete assembled world'));
  const world=await api('/api/world/read',{worldId:id});report.world=world;
  const exported=await api('/api/world/export',{worldId:id});
  await fs.writeFile(path.join(out,live?'chat052-world-live.json':'chat052-world.json'),exported.text);
  const packed=JSON.parse(exported.text);
  const raw=require('node:zlib').gunzipSync(Buffer.from(packed.gzipBase64,'base64'));
  const bundle=JSON.parse(raw);
  assert.equal(raw.length,packed.rawBytes);
  assert.equal(require('node:crypto').createHash('sha256').update(raw).digest('hex'),packed.rawSha256);
  assert.equal(bundle.runs.length,live?4:2);assert.deepEqual(bundle.world,world);
  for(const attempt of bundle.runs){assert.equal(attempt.status,'completed');assert(attempt.samples.length>100);assert(attempt.events.length>=6);}
  report.exportedRawSha256=packed.rawSha256;report.exportedRawBytes=packed.rawBytes;
  await app.evaluate(({BrowserWindow})=>BrowserWindow.getAllWindows()[0].setSize(1100,920));await capture('chat052-narrow.png');assert(await page.locator('#construction051').evaluate(e=>e.scrollWidth<=e.clientWidth+2));
  await page.locator('#mission044-activity').click();
  const retained=await api('/api/world/observe',{worldId:id});await page.locator('#c051-skill').fill('');await page.locator('#c051-build').click();await until(async()=>{const w=await api('/api/world/observe',{worldId:id});return w.pending&&w.nativeObservation?.sample?.tick>24;});await page.locator('#c051-stop').click();await until(async()=>(await latest(id)).status==='cancelled');await page.waitForFunction(()=>!document.querySelector('#c051-restore').hidden);await page.locator('#c051-restore').click();await until(async()=>!(await api('/api/world/observe',{worldId:id})).pending);assert.deepEqual((await api('/api/world/observe',{worldId:id})).frame,retained.frame);checks.push('Stop cannot earn a construction; explicit restore preserves the previous committed frame and removes pending ownership. Export and narrow layout work');
  await until(async()=>!(await page.locator('#run').isDisabled()));
  const counterGoal='Build an HTML counter with working Add and Reset controls, IDs #add and #reset, and numeric text #count initially 0. Use a readable teal design with no external assets. Test the actual controls.';
  await page.locator('#goal').fill(counterGoal);await page.locator('#run').click();
  const counter=await until(async()=>(await api('/api/agency043/state')).tasks.find(t=>t.goal===counterGoal));
  const counted=await until(async()=>{const t=await api('/api/agency043/read',{id:counter.id});if(['interrupted','cancelled'].includes(t.status))throw Error(t.reason);return t.status==='completed'?t:null;},900000);
  assert.equal(counted.mode,'workbench');assert.equal(counted.outcome,'verified');assert.equal(counted.timing052.terminalChecksWithoutModel,1);assert(!counted.history.some(r=>r.kind==='proposal'&&r.detail.tool==='finish'));
  const actual=await api('/api/artifact/read',{id:counted.artifact.id});assert(actual.test.passed);assert.deepEqual(actual.test.steps.map(s=>s.selector),['#count','#add','#count','#reset','#count']);report.liveCounter=counted;
  await page.locator('#mission044-open').click();await page.waitForFunction(id=>document.querySelector('#mission044').dataset.taskId===id,counter.id);await page.locator('#mission044-result').click();
  await page.waitForFunction(()=>document.querySelector('#preview-tab').classList.contains('selected'));await page.evaluate(()=>new Promise(r=>requestAnimationFrame(()=>requestAnimationFrame(r))));
  await fs.writeFile(path.join(out,'chat052-counter.png'),Buffer.from(await app.evaluate(()=>global.workspaceTest.capture()),'base64'));
  await app.close();app=null;await open();const restoredTask=await api('/api/agency043/read',{id:counter.id});assert.deepEqual(restoredTask.timing052,counted.timing052);assert.equal(restoredTask.summary,counted.summary);assert.deepEqual((await api('/api/world/observe',{worldId:id})).frame,retained.frame);
  checks.push('A third actual local-model request creates an HTML counter, passes the five original Add/Reset assertions and finishes in Java; task timing, summary and world restore exactly after another cold restart');
  assert.deepEqual(errors,[]);report.passed=true;report.date=new Date().toISOString();report.errors=errors;await fs.writeFile(path.join(out,live?'chat052-live.json':'chat052-integration.json'),JSON.stringify(report,null,2));console.log(JSON.stringify({passed:true,checks,project}));
 }catch(e){report.error=e.stack;report.passed=false;report.errors=errors;if(page){await capture('chat052-failure.png').catch(()=>{});report.messages=await page.locator('#messages').innerText().catch(()=> '');report.notice=await page.locator('#notice').innerText().catch(()=> '');}await fs.writeFile(path.join(out,'chat052-failed-'+Date.now()+'.json'),JSON.stringify(report,null,2));console.error('Retained project',project);throw e;}finally{if(app)await app.close();}
}
main().catch(e=>{console.error(e);process.exitCode=1;});
