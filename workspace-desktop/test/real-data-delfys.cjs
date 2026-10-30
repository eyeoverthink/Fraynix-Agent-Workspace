'use strict';
// Acceptance with externally collected video-derived observations, never a synthetic fallback.
const fs=require('node:fs/promises'),path=require('node:path'),os=require('node:os'),assert=require('node:assert/strict');
const {launch}=require('./calibration-integration.cjs');
const {createHash}=require('node:crypto');
async function main(){
 const input=path.resolve(process.env.FRAYNIX_REAL_DATA_INPUT||path.join(__dirname,'../../docs/datasets/delfys75-013'));
 const output=path.resolve(__dirname,'../test-output');await fs.mkdir(output,{recursive:true});
 const protocol=JSON.parse(await fs.readFile(path.join(input,'spatial-calibration-frozen.json'),'utf8'));
 const provenance=JSON.parse(await fs.readFile(path.join(input,'provenance.json'),'utf8'));
 assert.equal(provenance.dataset,'jaswar/physical-parameter-prediction');assert.equal(provenance.version,11);
 for(const [name,hash]of Object.entries(provenance.outputs))assert.equal(createHash('sha256').update(await fs.readFile(path.join(input,name))).digest('hex'),hash,'Changed published source or mapping: '+name);
 const project=await fs.mkdtemp(path.join(os.tmpdir(),'fraynix-real-data-'));const {app,page}=await launch(project);const checks=[],errors=[];
 const api=(route,args)=>app.evaluate((_,x)=>global.workspaceTest.api(x.route,x.args),{route,args});
 const execute=(tool,args)=>app.evaluate((_,x)=>global.workspaceTest.execute(x.tool,x.args),{tool,args});
 const capture=async name=>{await page.evaluate(()=>new Promise(r=>requestAnimationFrame(()=>requestAnimationFrame(r))));await fs.writeFile(path.join(output,name),Buffer.from(await app.evaluate(()=>global.workspaceTest.capture()),'base64'));};
 try{
  page.on('pageerror',e=>errors.push(e.message));
  checks.push('Published CSV and frozen spatial-protocol SHA-256 digests match the pinned provenance before import');
  const trainingScene=await api('/api/scene/save',{title:'Delfys75 · 20° training surrogate',parameters:{angleDeg:20,gravityMps2:9.81,friction:0}});
  const validationScene=await api('/api/scene/save',{title:'Delfys75 · 20° validation surrogate',parameters:{angleDeg:20,gravityMps2:9.81,friction:0}});
  const datasets={};
  for(const role of ['training','validation']){
   const csv=await fs.readFile(path.join(input,role+'.csv'),'utf8');
   const sourcePrefix='output_selected/sliding_block/low/'+(role==='training'?'01':'02')+'/';
   const sourceHashes=provenance.sources.filter(s=>s.path.startsWith(sourcePrefix)).map(s=>path.basename(s.path)+' SHA256 '+s.sha256).join('; ');
   datasets[role]=await api('/api/measurement/import',{title:'Delfys75 low/'+(role==='training'?'01':'02')+' · real video-derived',filename:role+'.csv',csv,sourceKind:'measured',sourceNote:'Real 60 fps video and author-provided SAM2 masks from jaswar/physical-parameter-prediction version 11. Centroid-derived displacement; training-only 0.726 m endpoint scale; shared projection. Nominal 20° (paper ±1°). No fitted time/velocity offset. Image scale/perspective and release conditions uncertain. Native 12 m ramp / 0.5 m cube / 1 kg are surrogate assumptions, not measured apparatus. '+sourceHashes+'. Spatial protocol SHA256 '+provenance.outputs['spatial-calibration-frozen.json'],missingPolicy:'reject',mapping:{timeColumn:'time_s',distanceColumn:'displacement_m',timeUnit:'s',distanceUnit:'m'}});
  }
  assert.equal(datasets.training.rows.length,52);assert.equal(datasets.validation.rows.length,49);
  checks.push('Real 60 fps source videos yield 52 training and 49 validation mask-centroid observations, with exact CSVs retained and measured classification explicitly qualified');
  await execute('scene.read',{id:trainingScene.id});await page.locator('#calibration-panel > summary').click();
  await page.locator('#calibration-training').selectOption(datasets.training.datasetId);await page.locator('#calibration-validation').selectOption(datasets.validation.datasetId);await page.locator('#calibration-validation-scene').selectOption(validationScene.id);
  await page.locator('#calibration-minimum').fill('0');await page.locator('#calibration-maximum').fill('1');await page.locator('#calibration-seconds').fill('1');await page.locator('#calibration-toleranceM').fill('0.015');await page.locator('#calibration-minImprovementM').fill('0.001');
  await page.locator('#calibration-start').click();await page.waitForFunction(()=>document.querySelector('#calibration-status').dataset.status==='completed',{},{timeout:60000});
  let state=await api('/api/state');assert.equal(state.calibrations.length,1);const job=await api('/api/calibration/read',{id:state.calibrations[0].id});
  assert.equal(job.status,'completed');for(const [key,value]of Object.entries({minimum:0,maximum:1,seconds:1,toleranceM:.015,minImprovementM:.001}))assert.equal(job.definition[key],value);
  assert.equal(job.definition.training.datasetId,datasets.training.datasetId);assert.equal(job.definition.validation.datasetId,datasets.validation.datasetId);
  checks.push('Visible controls execute the frozen 0..1 friction search, 1 s native runs, 0.015 m tolerance and 0.001 m minimum gain without tuning to the validation outcome');
  const nativeRuns=[];
  for(const trial of job.trials){
   const run=await api('/api/experiment/read',{id:trial.runId});nativeRuns.push(run);assert.equal(run.final.tick,240);let sum=0;
   for(const p of datasets.training.rows){
    let k=run.samples.findIndex(s=>s.timeS>=p.timeS);assert(k>=0);const b=run.samples[k],a=run.samples[Math.max(0,k-1)];const simulated=a.distanceM+(b.timeS===a.timeS?0:(p.timeS-a.timeS)/(b.timeS-a.timeS)*(b.distanceM-a.distanceM));sum+=(simulated-p.distanceM)**2;
   }
   assert(Math.abs(trial.metrics.rmseM-Math.sqrt(sum/datasets.training.rows.length))<1e-12);
  }
  checks.push('Every training candidate is a retained 240-tick native run; RMSE is independently recomputed from native samples and the real observation timestamps');
  assert(job.selection.frozenAt<=job.validationStartedAt);
  const e=job.evaluation;assert.equal(e.accepted,e.trainingImproved&&e.validationWithinTolerance&&e.validationNoRegression);
  if(!e.accepted){assert.equal(await page.locator('#calibration-apply').isDisabled(),true);await assert.rejects(api('/api/calibration/apply',{id:job.id}),/accepted/);}
  checks.push('The actual verdict equals the declared gates; a rejection disables Apply and is not rewritten into success');
  for(const scene of [trainingScene,validationScene])assert.equal((await api('/api/scene/read',{id:scene.id})).version,1);
  checks.push('Research leaves both authored surrogate scenes at version 1; no automatic parameter promotion');
  await page.locator('#calibration-result').scrollIntoViewIfNeeded();await capture('real-data-lab.png');
  await page.locator('#calibration-report').click();await page.waitForFunction(()=>document.querySelector('#test-badge').textContent==='TESTS PASSED'&&document.querySelector('#scene-lab').hidden);
  state=await api('/api/state');const report=await api('/api/artifact/read',{id:state.artifacts.at(-1).id});assert(report.test.passed);assert.equal(report.contents['training.csv'],datasets.training.csv);assert.equal(report.contents['validation.csv'],datasets.validation.csv);
  checks.push('Actual HTML/SVG/CSV/JSON report opens in the existing canvas and passes three DOM assertions with the original observation CSVs');await capture('real-data-report.png');
  assert.deepEqual(errors,[]);checks.push('No renderer JavaScript errors during real-data import, native fit or report');
  const result={date:new Date().toISOString(),passed:true,meaningOfPassed:'The import/execution/report workflow and independent arithmetic checks passed; the numerical fit verdict is recorded separately below.',checks,protocol,job,report:{id:report.id,test:report.test},sourceKinds:{training:datasets.training.sourceKind,validation:datasets.validation.sourceKind},nativeRuns,scientificStatus:'Exploratory surrogate-model comparison, not an independently calibrated apparatus or a certified material friction measurement. Masks are estimated from real recordings; image-plane scale and release assumptions are disclosed.',modelWeightsTrained:false};
  await fs.writeFile(path.join(output,'real-data-delfys.json'),JSON.stringify(result,null,2));console.log(JSON.stringify({workflowPassed:true,fitAccepted:e.accepted,selection:job.selection.value,trainingRmseM:job.fittedTraining.rmseM,validationRmseM:job.validationFitted.rmseM,validationMaxAbsErrorM:job.validationFitted.maxAbsErrorM,checks},null,2));
 }catch(error){await fs.writeFile(path.join(output,'real-data-failure.txt'),await page.locator('body').innerText());throw error;}finally{await app.close();}
}
main().catch(error=>{console.error(error);process.exitCode=1;});
