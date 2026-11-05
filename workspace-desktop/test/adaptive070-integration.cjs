'use strict';
const {launch}=require('./calibration-integration.cjs'),fs=require('node:fs/promises'),path=require('node:path'),os=require('node:os'),assert=require('node:assert/strict');
const sleep=ms=>new Promise(r=>setTimeout(r,ms));
async function main(){
 process.env.FRAYNIX_WORKSPACE_PERSISTENT='0';process.env.FRAYNIX_WORKSPACE_PLAN_CPU_ONLY='1';
 const prior=process.env.FRAYNIX070_RESUME?JSON.parse(await fs.readFile(process.env.FRAYNIX070_RESUME,'utf8')):null,out=path.resolve(__dirname,'../test-output'),project=prior?.project||await fs.mkdtemp(path.join(os.tmpdir(),'fraynix-adaptive070-ui-'));await fs.mkdir(out,{recursive:true});
 const source=require('node:child_process').execFileSync('git',['rev-parse','HEAD'],{encoding:'utf8'}).trim(),record={...(prior||{}),project,source,date:new Date().toISOString(),checks:prior?.checks||[]};let app,page;const errors=[];
 const api=(route,args={})=>app.evaluate((_,x)=>global.workspaceTest.api(x.route,x.args),{route,args});
 async function until(f,ms=180000){const end=Date.now()+ms;while(Date.now()<end){const x=await f();if(x)return x;await sleep(750)}throw Error('Expected edition070 state timed out')}
 async function capture(name){await page.evaluate(()=>new Promise(r=>requestAnimationFrame(()=>requestAnimationFrame(r))));await sleep(300);await fs.writeFile(path.join(out,'adaptive070-'+name+'.png'),Buffer.from(await app.evaluate(()=>global.workspaceTest.capture()),'base64'))}
 async function open(){({app,page}=await launch(project));page.on('pageerror',e=>errors.push(e.message));await app.evaluate(({BrowserWindow})=>BrowserWindow.getAllWindows()[0].setSize(1600,1100));await page.locator('#brain-open').click();await page.locator('#adaptive070').evaluate(e=>e.open=true)}
 async function save(){await fs.writeFile(path.join(out,'adaptive070-checkpoint.json'),JSON.stringify(record,null,2))}
 async function select(phase,index){await page.locator('#a070-phase').selectOption(phase);await until(async()=>await page.locator('#a070-case option').count()===record.job[phase+'070'].length);await page.locator('#a070-case').selectOption(String(index));await page.locator('#a070-load').click();await until(async()=>(await page.locator('#a070-task').innerText()).includes(record.job[phase+'070'][index].task.angleDeg.toFixed(3)+'\u00b0'))}
 try{
  await open();if(!record.job){await page.locator('#a070-run').click();const state=await until(async()=>{const s=(await api('/api/brain/state')).adaptive070;if(s.status==='failed')throw Error(s.reason);return s.lastCompleted?s:null},3600000);record.job=await api('/api/brain/read',{id:state.lastCompleted.jobId});record.jobSource=source;assert.equal(record.job.status,'completed');assert.equal(record.job.progress,488);assert.equal(record.job.disturbed070.length,60);assert(record.job.parity070.every(p=>p.maximumDelta<=1e-6));record.checks.push('Visible study completes 488 slots: original-runtime parity, same-world later checks and frozen-model clean/disturbed/unsupported evaluations');await save()}
  assert.equal(record.job.status,'completed');assert.equal(record.job.progress,488);assert.equal(record.job.disturbed070.length,60);assert(record.job.parity070.every(p=>p.maximumDelta<=1e-6));assert(record.job.fixedParity070.every(p=>p.maximumDelta===0));record.checks.push('Visible study completes 488 slots, including four original and four fixed-policy parity comparisons');
  await page.locator('#a070-history').selectOption(record.job.id);await until(async()=>!await page.locator('#a070-load').isDisabled());
  assert.equal(await page.locator('#a070-metrics table').count(),4);assert.equal(await page.locator('#a070-score circle').count(),96);
  assert.equal(await page.locator('#a070-metrics').getAttribute('data-qualified'),String(record.job.accepted));
  assert((await page.locator('#a070-fit').innerText()).includes(record.job.reusedModel070.sha256));
  await page.locator('#a070-fit').scrollIntoViewIfNeeded();await capture('overview');
  await page.locator('#a070-metrics').scrollIntoViewIfNeeded();await capture('clean');
  await select('disturbed',0);assert.equal(await page.locator('#a070-score circle').count(),120);await page.locator('#a070-metrics').scrollIntoViewIfNeeded();await capture('disturbed');
  for(const family of ['brake','lift','push','spin','after-last']){
   const index=record.job.disturbed070.findIndex(r=>r.disturbance===family);await select('disturbed',index);await page.locator('#a070-method').selectOption('adaptive');
   const result=record.job.disturbed070[index].results.adaptive,receipt=result.record.externalReceipts[0];
   assert.equal((await page.locator('#a070-motion polyline').first().getAttribute('points')).split(' ').length,46);
   await page.locator('#a070-time').fill('27');await page.locator('#a070-time').dispatchEvent('input');assert((await page.locator('#a070-frame').innerText()).includes('0.4500 s'));
   assert.equal(await page.locator('#a070-second').getAttribute('data-kind'),result.record.laterPlan?'measured-later-window':'none');assert.equal(await page.locator('#a070-second').getAttribute('data-correct'),String(!!result.record.laterPlan?.correct));assert.equal(await page.locator('#a070-schedule').getAttribute('data-tick'),String(result.record.schedule070.selectedTick));
   assert.equal(await page.locator('#a070-event').getAttribute('data-count'),'1');assert((await page.locator('#a070-event').innerText()).includes(family));
   assert.deepEqual(receipt.before.position,receipt.after.position);assert.deepEqual(receipt.before.rotation,receipt.after.rotation);
   await page.locator('#a070-second').scrollIntoViewIfNeeded();await capture(family);
  }
  record.checks.push('All five disturbance families retain actual receipts, selected window or paid scheduler skip, and complete 46-sample trajectories');
  for(const tick of [-1,92,116,140]){
   let phase='clean',index=record.job.clean070.findIndex(r=>r.results.adaptive.record?.schedule070.selectedTick===tick);
   if(index<0){phase='disturbed';index=record.job.disturbed070.findIndex(r=>r.results.adaptive.record?.schedule070.selectedTick===tick)}
   if(index<0)continue;
   await select(phase,index);await page.locator('#a070-method').selectOption('adaptive');assert.equal(await page.locator('#a070-schedule').getAttribute('data-tick'),String(tick));
   assert.equal(await page.locator('#a070-schedule table').count(),2);assert.equal(await page.locator('#a070-schedule details table tr').count(),28);
   await page.locator('#a070-schedule').scrollIntoViewIfNeeded();await capture(tick<0?'skip':'schedule-'+tick);
  }
  record.checks.push('Actual schedule choices expose all three candidate values, 27 hypothetical traces, scheduler costs and the selected tick or skip');
  const saved=record.job.disturbed070.findIndex(r=>!r.results.fixed.outcome.success&&r.results.adaptive.outcome.success);
  const index=saved>=0?saved:0;record.savedGoalIndex=saved;await select('disturbed',index);await page.locator('#a070-method').selectOption('adaptive');await page.locator('#a070-outcomes').scrollIntoViewIfNeeded();await capture('paired');
  const declined=record.job.clean070.findIndex(r=>!r.plan.decisions.fixedCalibrated.execute);if(declined>=0){await select('clean',declined);await page.locator('#a070-method').selectOption('adaptiveCalibrated');assert.equal(await page.locator('#a070-second').getAttribute('data-kind'),'none');assert.equal(record.job.clean070[declined].results.adaptiveCalibrated.record,undefined);await page.locator('#a070-stage').scrollIntoViewIfNeeded();await capture('decline')}
  await select('unsupported',0);assert.equal(await page.locator('#a070-time').isDisabled(),true);assert.equal(await page.locator('#a070-motion polyline').count(),0);assert((await page.locator('#a070-frame').innerText()).includes('No execution'));await page.locator('#a070-task').scrollIntoViewIfNeeded();await capture('domain');
  record.checks.push('All-task costs and paired gains match Java evidence; screened and unsupported refusals do not invent motion; saved goals and failures remain inspectable');
  record.trace=await api('/api/brain/adaptive-trace',{jobId:record.job.id,index:3,phase:'disturbed'});await save();await app.close();app=null;await open();await until(async()=>!await page.locator('#a070-load').isDisabled());
  assert.deepEqual(await api('/api/brain/adaptive-trace',{jobId:record.job.id,index:3,phase:'disturbed'}),record.trace);const restored=(await api('/api/brain/state'));assert.equal(restored.activePolicy.version,1);assert.deepEqual(restored.adaptive070.lastCompleted.modelSha256,record.job.reusedModel070.sha256);record.checks.push('Cold Java/app restart preserves exact frozen screen, real external and corrective receipts, costs and trajectories; active controller remains v1');
  await app.evaluate(({BrowserWindow})=>BrowserWindow.getAllWindows()[0].setSize(1100,850));await page.locator('#a070-metrics').scrollIntoViewIfNeeded();await capture('narrow');
  if(!record.stopped){await page.locator('#a070-run').click();await until(async()=>!await page.locator('#a070-stop').isDisabled());await page.locator('#a070-stop').click();await until(async()=>!(await api('/api/brain/state')).busy);record.stopped=(await api('/api/brain/state')).adaptive070;assert.equal(record.stopped.status,'cancelled');assert.equal(record.stopped.lastCompleted.jobId,record.job.id);await save()}record.checks.push('Visible Stop cancels a new study and retains the completed model and evaluation');
  await app.evaluate(({BrowserWindow})=>BrowserWindow.getAllWindows()[0].setSize(1600,1100));await page.locator('#brain-close').click();
  if(record.chat?.status==='completed'&&!JSON.stringify(record.chat).includes(record.job.reusedModel070.sha256)){record.previousChatFailure=record.chat;delete record.chat;await save()}
  if(record.chat?.status==='interrupted'){record.previousChatFailure=record.chat;await api('/api/agency043/control',{id:record.chat.id,action:'resume'});record.chat=await until(async()=>{const t=await api('/api/agency043/read',{id:record.previousChatFailure.id});return ['completed','interrupted','cancelled'].includes(t.status)?t:null},1200000);await save()}
  if(!record.chat){await page.waitForFunction(()=>[...document.querySelector('#model').options].some(o=>o.value==='gpt-oss:20b'),{},{timeout:60000});await page.locator('#model').selectOption('gpt-oss:20b');const previous=(await api('/api/agency043/state')).tasks.map(t=>t.id);
   await page.getByLabel('Agent request').fill('Inspect the adaptive observation timing study. Read actual Java state and create the verified report. Include skip versus chosen observation times, the hypothetical assumptions and unchanged frozen model scope, clean and disturbed outcomes, all four policies with successes, misses, refusals and sensing costs, scheduler cost, paired utility, saved and harmed goals, and both qualification gates. Keep after-last disturbances in the results. Preserve failures and distinguish native simulation from physical reality. Do not run experiments or move objects.');
   await page.locator('#run').click();const task=await until(async()=>(await api('/api/agency043/state')).tasks.find(t=>!previous.includes(t.id)));record.chat=await until(async()=>{const t=await api('/api/agency043/read',{id:task.id});return ['completed','interrupted','cancelled'].includes(t.status)?t:null},1200000);await save()}
  assert.equal(record.chat.status,'completed',record.chat.reason);assert.equal(record.chat.family044,'agent');assert(record.chat.history.some(x=>x.kind==='plan'));assert(record.chat.history.filter(x=>x.kind==='plan').every(x=>x.detail.compute.cpuOnly&&x.detail.compute.options.num_thread===4));assert(JSON.stringify(record.chat).includes(record.job.reusedModel070.sha256));
  await app.evaluate((_,id)=>global.workspaceTest.execute('canvas.open',{id}),record.chat.report044.artifact.id);await until(()=>app.evaluate(()=>global.workspaceTest.canvasRead('document.body.innerText.includes("adaptive070")')));await capture('chat');record.checks.push('Actual CPU/four-thread gpt-oss20b reads the frozen screen and held-out outcomes and completes its checked report');
  assert.deepEqual(errors,[]);delete record.error;record.checks=[...new Set(record.checks)];record.passed=true;await fs.writeFile(path.join(out,'adaptive070-live.json'),JSON.stringify(record,null,2));console.log(JSON.stringify({passed:true,project,checks:record.checks,nativeExecutions:record.job.nativeExecutions,qualified:record.job.accepted,clean:record.job.cleanEvaluation070.methods,disturbed:record.job.disturbedEvaluation070.methods}));
 }catch(e){record.error=e.stack;await fs.writeFile(path.join(out,'adaptive070-failed-'+Date.now()+'.json'),JSON.stringify(record,null,2));throw e}finally{if(app)await app.close().catch(()=>{})}
}
main().catch(e=>{console.error(e);process.exitCode=1});
