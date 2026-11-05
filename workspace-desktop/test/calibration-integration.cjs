'use strict';
const {_electron}=require('playwright'),fs=require('node:fs/promises'),path=require('node:path'),os=require('node:os'),assert=require('node:assert/strict');
async function launch(project,extraArgs=[]){
 const env={...process.env,FRAYNIX_WORKSPACE_PROJECT:project,FRAYNIX_WORKSPACE_TEST:'1',FRAYNIX_WORKSPACE_OFFLINE:'1'};delete env.ELECTRON_RUN_AS_NODE;
 const app=await _electron.launch({args:[...extraArgs,path.resolve(__dirname,'..','main.cjs')],env,timeout:60000});for(const stream of [app.process().stdout,app.process().stderr])stream.on('data',data=>require('node:fs').appendFileSync(path.join(project,'test-desktop-process.log'),data));const page=await app.firstWindow();
 await app.evaluate(async()=>{for(let i=0;i<200&&!global.workspaceTest;i++)await new Promise(r=>setTimeout(r,100));if(!global.workspaceTest)throw Error('Workspace not ready');});return {app,page};
}
async function fixtures(api){
 const trainingScene=await api('/api/scene/save',{title:'Training · 25 degrees',parameters:{angleDeg:25,friction:0}});
 const validationScene=await api('/api/scene/save',{title:'Validation · 35 degrees',parameters:{angleDeg:35,friction:0}});
 async function observations(angle,friction,title){
  const truth=await api('/api/scene/save',{title:'Synthetic source '+title,parameters:{angleDeg:angle,friction}});
  const run=await api('/api/experiment/run',{id:truth.id,expectedVersion:1,seconds:.75});await api('/api/experiment/control',{action:'reset'});
  const csv='time,distance\n'+run.samples.filter(p=>p.tick%24===0).map(p=>p.timeS+','+p.distanceM).join('\n')+'\n';
  const data=await api('/api/measurement/import',{title,filename:'synthetic-'+angle+'.csv',csv,sourceKind:'synthetic',sourceNote:'Separate native Bullet source run '+run.id+' at '+angle+' degrees, friction '+friction+'. Not instrument data.',missingPolicy:'reject',mapping:{timeColumn:'time',distanceColumn:'distance',timeUnit:'s',distanceUnit:'m'}});
  return {data,sourceRunId:run.id};
 }
 const training=await observations(25,.28,'Training observations'),validation=await observations(35,.28,'Validation observations'),rejected=await observations(35,.65,'Different validation conditions');
 return {trainingScene,validationScene,training,validation,rejected};
}
async function main(){
 const project=await fs.mkdtemp(path.join(os.tmpdir(),'fraynix-calibration-'));let {app,page}=await launch(project);const output=path.resolve(__dirname,'..','test-output'),checks=[],errors=[];await fs.mkdir(output,{recursive:true});
 const api=(route,args)=>app.evaluate((_,x)=>global.workspaceTest.api(x.route,x.args),{route,args});
 const execute=(tool,args)=>app.evaluate((_,x)=>global.workspaceTest.execute(x.tool,x.args),{tool,args});
 const capture=async name=>{await page.evaluate(()=>new Promise(r=>requestAnimationFrame(()=>requestAnimationFrame(r))));await fs.writeFile(path.join(output,name),Buffer.from(await app.evaluate(()=>global.workspaceTest.capture()),'base64'));};
 try{
  page.on('pageerror',e=>errors.push(e.message));assert.equal(await page.locator('.layout').evaluate(e=>getComputedStyle(e).display),'grid');
  const f=await fixtures(api);checks.push('Synthetic sources come from separate actual native worlds at 25 and 35 degrees; no analytic or random fitness substitutes');
  await execute('scene.read',{id:f.trainingScene.id});await page.locator('#calibration-panel > summary').click();
  await page.locator('#calibration-training').selectOption(f.training.data.datasetId);await page.locator('#calibration-validation').selectOption(f.validation.data.datasetId);await page.locator('#calibration-validation-scene').selectOption(f.validationScene.id);
  await page.locator('#calibration-maximum').fill('0.8');await page.locator('#calibration-minImprovementM').fill('0.001');
  await page.locator('#calibration-start').click();await page.waitForFunction(()=>document.querySelector('#calibration-status').dataset.status==='completed',{},{timeout:60000});
  let state=await api('/api/state'),job=await api('/api/calibration/read',{id:state.calibrations.at(-1).id});
  assert.equal(job.evaluation.accepted,true);assert(Math.abs(job.selection.value-.28)<.004);assert(job.completedNativeOperations<=20);assert.equal(job.definition.training.datasetId,f.training.data.datasetId);assert.equal(job.definition.validation.datasetId,f.validation.data.datasetId);
  assert.equal((await api('/api/scene/read',{id:f.trainingScene.id})).version,1);assert.equal((await api('/api/scene/read',{id:f.validationScene.id})).version,1);
  checks.push('Visible Fit & validate control executes native search, training-only selection and separate validation while both authored scenes remain v1');
  for(const trial of job.trials){const run=await api('/api/experiment/read',{id:trial.runId});assert.equal(run.scope,'calibration');assert.equal(run.final.tick,180);let squares=0;
   for(const p of f.training.data.rows){const sample=run.samples.find(s=>Math.abs(s.timeS-p.timeS)<1e-10);assert(sample);squares+=(sample.distanceM-p.distanceM)**2;}
   assert(Math.abs(trial.metrics.rmseM-Math.sqrt(squares/f.training.data.rows.length))<1e-12);
  }
  checks.push('Every candidate retains 180 actual native ticks and independently recomputed training RMSE');
  await page.locator('#calibration-result').scrollIntoViewIfNeeded();await capture('calibration-lab.png');
  await page.locator('#calibration-apply').click();await page.waitForFunction(()=>document.querySelector('#calibration-result').textContent.includes('applied.'));
  const applied=await api('/api/calibration/read',{id:job.id});assert.equal((await api('/api/scene/read',{id:f.trainingScene.id})).version,2);assert.equal((await api('/api/scene/read',{id:f.validationScene.id})).version,1);
  assert.deepEqual((await api('/api/scene/read',{id:f.trainingScene.id})).entityIds,f.trainingScene.entityIds);checks.push('Visible Apply changes only the training parameter in scene v2, preserves object IDs and records an atomic receipt');
  await page.locator('#calibration-report').click();await page.waitForFunction(()=>document.querySelector('#test-badge').textContent==='TESTS PASSED'&&document.querySelector('#scene-lab').hidden);
  state=await api('/api/state');const report=await api('/api/artifact/read',{id:state.artifacts.at(-1).id});assert.equal(report.test.checks.length,3);assert.equal(report.contents['training.csv'],f.training.data.csv);assert.equal(report.contents['validation.csv'],f.validation.data.csv);assert.equal(JSON.parse(report.contents['calibration.json']).application.applyReceipt.id,applied.application.applyReceipt.id);assert(!report.contents['index.html'].includes('<script'));
  checks.push('Visible report opens the real canvas with two host-rendered curves, exact CSVs and native JSON evidence; three DOM assertions pass');await capture('calibration-report.png');
  await app.close();({app,page}=await launch(project));page.on('pageerror',e=>errors.push(e.message));
  await execute('scene.read',{id:f.trainingScene.id});await page.locator('#calibration-panel > summary').click();await page.locator('#calibration-rollback').waitFor({state:'visible'});await page.locator('#calibration-rollback').click();
  await page.waitForFunction(()=>document.querySelector('#calibration-result').textContent.includes('rolled-back.'));
  assert.equal((await api('/api/scene/read',{id:f.trainingScene.id})).version,3);assert.equal((await api('/api/scene/read',{id:f.trainingScene.id})).parameters.friction,0);
  checks.push('Restart restores accepted fit and application receipt; visible rollback restores the prior parameter as scene v3 without erasing history');
  await page.locator('#calibration-validation').selectOption(f.rejected.data.datasetId);await page.locator('#calibration-start').click();
  await page.waitForFunction(id=>document.querySelector('#calibration-status').dataset.status==='completed'&&document.querySelector('#calibration-history').value!==id,job.id,{timeout:60000});
  state=await api('/api/state');const bad=await api('/api/calibration/read',{id:state.calibrations.at(-1).id});assert.equal(bad.evaluation.accepted,false);assert.equal(bad.selection.value,job.selection.value);assert.equal(await page.locator('#calibration-apply').isDisabled(),true);await assert.rejects(api('/api/calibration/apply',{id:bad.id}),/accepted/);
  checks.push('Changing only validation conditions preserves the training winner, produces a genuine failed gate and disables/rejects apply');await page.locator('#calibration-result').scrollIntoViewIfNeeded();await capture('calibration-rejected.png');
  assert.deepEqual(errors,[]);checks.push('No renderer JavaScript errors across fit, report, apply, restart, rollback and rejected validation');
  await fs.writeFile(path.join(output,'calibration-integration.json'),JSON.stringify({date:new Date().toISOString(),passed:true,checks,accepted:job,application:applied.application,rejected:bad,report:{id:report.id,test:report.test},scope:'Actual isolated Electron/Java/Bullet workflow using explicitly synthetic sources. This is physical parameter calibration, not language-model training.'},null,2));console.log(JSON.stringify({passed:true,checks},null,2));
 }catch(e){await fs.writeFile(path.join(output,'calibration-failure.txt'),await page.locator('body').innerText());throw e;}finally{await app.close();}
}
module.exports={launch,fixtures};if(require.main===module)main().catch(e=>{console.error(e);process.exitCode=1;});
