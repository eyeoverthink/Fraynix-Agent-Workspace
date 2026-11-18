'use strict';

const assert=require('node:assert/strict');
const {createHash}=require('node:crypto');
const fs=require('node:fs/promises');
const path=require('node:path');
const {launch}=require('../../workspace-desktop/test/calibration-integration.cjs');
const {verify}=require('../../scripts/Study-Build095.cjs');

const output=path.resolve('build',process.argv[2]||'ramp174');
const project=path.join(output,'project');
const modelName=process.env.FRAYNIX_TEST_RAMP_MODEL||'llama3.2:latest';
const single='local-ramp-experiment-v1', pair='local-ramp-comparison-v1';
const header='angle_deg,friction,gravity_mps2,mass_kg,duration_s\r\n';
const first='38,0.04,9.81,1.2,0.85\r\n',second='18,0.31,9.81,1.2,0.85\r\n';
const csv='\uFEFF'+header+first+second, reverse=header+second+first;
const hash=value=>createHash('sha256').update(value).digest('hex');
const nap=ms=>new Promise(resolve=>setTimeout(resolve,ms));
// Evaluation wording belongs to the study only. The model gets its tool descriptions and each request.
const heldOut=[
 {text:'Show the path and final distance of the first setup.',wanted:single},
 {text:'Give me the downhill trajectory for setup A on its own.',wanted:single},
 {text:'Which setup travels farther over the saved duration?',wanted:pair},
 {text:'How much more or less ground does one row cover relative to the other?',wanted:pair},
 {text:'I have these ramp settings, but I have not decided what result I want.',wanted:'clarify'},
 {text:'I need a comparison of both setups over their shared time.',wanted:'clarify',missing:true}
];

async function main(){
 const build=verify(process.cwd());
 await fs.mkdir(output);await fs.mkdir(project);
 Object.assign(process.env,{FRAYNIX_WORKSPACE_PERSISTENT:'0',FRAYNIX_WORKSPACE_JAVA_HEAP_MIB:'768',
  FRAYNIX_WORKSPACE_JAVA_PROCESSORS:'2',FRAYNIX_WORKSPACE_PLAN_CPU_ONLY:'1'});
 let app,page;
 const result={passed:false,source:build.source,model:modelName,captures:[],errors:[],cases:[]};
 const api=(route,args)=>app.evaluate((_,x)=>global.workspaceTest.api(x.route,x.args??undefined),{route,args:args??null});
 const request=(operation,command,args)=>page.evaluate(x=>window.workspace.request(x),{operation,command,args});
 const parent=id=>api('/api/agency043/read',{id});
 const child=id=>api('/api/ramp-mission/read',{id});
 const proof=id=>api('/api/ramp-mission/verify',{id});
 const save=()=>fs.writeFile(path.join(output,'result.json'),JSON.stringify(result,null,2)+'\n');

 async function until(read,label,wait=45000){
  const stop=Date.now()+wait;
  while(Date.now()<stop){const value=await read();if(value)return value;await nap(95);}
  throw Error('Timed out waiting for '+label);
 }
 async function start(noModels=true){
  if(noModels)process.env.FRAYNIX_WORKSPACE_NO_MODELS='1';else delete process.env.FRAYNIX_WORKSPACE_NO_MODELS;
  ({app,page}=await launch(project));
  page.on('pageerror',e=>result.errors.push(e.message));
  await app.evaluate(({BrowserWindow})=>BrowserWindow.getAllWindows().find(w=>w.isVisible()).setSize(1550,1030));
  assert((await app.evaluate(()=>global.workspaceTest.network())).enabled);
  if(noModels)assert.deepEqual((await api('/api/state')).models,[]);
 }
 async function capture(name){
  await page.evaluate(()=>new Promise(resolve=>requestAnimationFrame(()=>requestAnimationFrame(resolve))));
  const png=Buffer.from(await app.evaluate(()=>global.workspaceTest.capture()),'base64');
  await fs.writeFile(path.join(output,name),png);result.captures.push({name,sha256:hash(png)});
 }
 async function openSource(source){
  const url=(await app.evaluate(()=>global.workspaceTest.origin()))+source.path;
  await page.locator('#address').fill(url);await page.locator('#navigate').click();
  await until(()=>app.evaluate(({webContents},url)=>webContents.fromId(global.workspaceTest.surfaces().browser).getURL()===url,url),'source navigation');
 }
 async function createSource(text,{pause=false,compare=false,captureInput=false}={}){
  await page.locator('#ramp-input-open').click();
  const dialog=page.locator('#ramp-input-dialog');
  await dialog.waitFor();
  await page.locator('#ramp-csv').setInputFiles({name:'ramp-setups.csv',mimeType:'text/csv',buffer:Buffer.from(text)});
  await page.waitForFunction(()=>document.querySelector('#ramp-file-note').textContent.startsWith('CSV checked'));
  await page.locator('#ramp-pause').setChecked(pause);
  await page.locator('#ramp-result').selectOption(compare?'compare':'single');
  if(captureInput)await capture('paired-ramp-input.png');
  await page.locator('#ramp-input-save').click();
  await dialog.waitFor({state:'hidden'});
  return api('/api/ramp-mission/bind',{path:new URL(await page.locator('#address').inputValue()).pathname});
 }
 async function submit(goal){
  if(goal)await page.locator('#goal').fill(goal);
  const prior=new Set((await api('/api/agency043/state',{})).tasks.map(t=>t.id));
  assert(await page.locator('#run').isEnabled());
  const began=Date.now();await page.locator('#run').click();
  const task=await until(async()=>(await api('/api/agency043/state',{})).tasks.find(row=>!prior.has(row.id)),'admission');
  return {id:task.id,began};
 }
 const terminal=run=>until(async()=>{
  const task=await parent(run.id);
  return ['completed','interrupted','cancelled'].includes(task.status)&&task;
 },'verified outcome or refusal',180000);
 async function showMission(id){
  await page.locator('#mission044-open').click();
  await page.locator('#mission044-task').selectOption(id);
  await page.waitForFunction(id=>document.querySelector('#mission044').dataset.taskId===id,id);
 }
 async function finished(run,kind,calls){
  const task=await terminal(run),elapsedMs=Date.now()-run.began;
  assert.equal(task.status,'completed',task.reason);
  assert.equal(task.outcome,'verified');
  assert.equal(task.timing052.modelCalls,calls);
  const mission=await proof(run.id);
  assert.equal(mission.procedure,kind);
  assert.equal(mission.contractSha256,result.registry.procedures.find(row=>row.id===kind).sha256);
  assert.deepEqual(mission.stages.map(row=>row.name),['browser','experiment','replay','artifact','canvas']);
  const report=await api('/api/artifact/read',{id:mission.report.id});
  const evidence=JSON.parse(report.contents['experiment.json']);
  assert.deepEqual(mission.audit,evidence.audit);
  assert.equal(hash(report.contents['input.csv']),mission.source.csvSha256);
  if(kind===pair){
   assert.equal(mission.canvasReceipt.checks.length,9);
   const sides=['first','second'],ids=new Set();
   for(const records of [evidence.original,evidence.replay])
    for(const side of sides)ids.add(records[side].sessionId);
   assert.equal(ids.size,4);
   for(const side of sides){
    assert.deepEqual(evidence.original[side].samples,evidence.replay[side].samples);
    assert.equal(evidence.original[side].samples.at(-1).tick,mission.audit.ticks);
   }
   const delta=evidence.original.second.samples.at(-1).distanceM-evidence.original.first.samples.at(-1).distanceM;
   assert.equal(mission.audit.distanceDifferenceM,delta);
   assert(report.contents['samples.csv'].startsWith('tick,time_s,first_distance_m,second_distance_m,second_minus_first_m'));
  }else{
   assert.equal(mission.canvasReceipt.checks.length,10);
   assert(Array.isArray(evidence.original.samples));
  }
  return {task,mission,evidence,elapsedMs};
 }

 try{
  await start();
  result.registry=await api('/api/ramp-mission/catalog',{});
  assert.deepEqual(result.registry.procedures.map(row=>row.id),[single,pair]);
  assert.notEqual(result.registry.procedures[0].sha256,result.registry.procedures[1].sha256);
  const source=await createSource(csv,{pause:true,compare:true,captureInput:true});
  assert.equal(source.csvSha256,hash(csv));
  assert.equal(source.secondValues.angleDeg,18);
  const recovering=await submit();
  assert.equal((await terminal(recovering)).status,'interrupted');
  assert.deepEqual((await child(recovering.id)).stages.map(row=>row.name),['browser']);
  await showMission(recovering.id);
  await app.evaluate(()=>global.workspaceTest.agency043.stop());
  await page.locator('#mission044-resume').click();
  await until(async()=>{
   const t=await parent(recovering.id);
   return t.resumes===1&&t.status==='waiting-host'&&t.effect?.tool==='localRamp.check';
  },'unclaimed comparison canvas checkpoint');
  result.beforeStop=await child(recovering.id);
  assert.equal(result.beforeStop.status,'ready-report');
  await page.evaluate(()=>window.agency043View.mission.refresh());
  await page.waitForFunction(()=>document.querySelector('#mission044-facts').textContent.includes('Saved report'));
  await capture('paired-ramp-checkpoint.png');
  assert(await app.evaluate(()=>global.workspaceTest.crashEngine()));
  await nap(220);await app.close().catch(()=>{});app=null;
  await start();
  assert.deepEqual(await child(recovering.id),result.beforeStop);
  await showMission(recovering.id);
  await page.locator('#mission044-resume').click();
  await until(async()=>(await parent(recovering.id)).resumes===2,'second admission');
  result.recovered=await finished(recovering,pair,0);
  assert.deepEqual(result.recovered.mission.stages.slice(0,4),result.beforeStop.stages);
  assert.equal(result.recovered.mission.audit.farther,'first');
  await openSource(source);
  await capture('paired-ramp-verified.png');

  result.direct=await finished(await submit('Compare the local ramp setups.'),pair,0);
  const flipped=await createSource(reverse,{compare:true});
  result.reversed=await finished(await submit(),pair,0);
  assert.equal(result.reversed.mission.audit.farther,'second');
  assert.equal(result.reversed.mission.audit.distanceDifferenceM,-result.direct.mission.audit.distanceDifferenceM);

  await app.close();app=null;
  await start(false);
  await page.waitForFunction(name=>[...document.querySelector('#model').options].some(o=>o.value===name),modelName,{timeout:65000});
  await page.locator('#model').selectOption(modelName);
  await openSource(flipped);
  for(const spec of heldOut){
   if(spec.missing)await createSource(header+first);
   const run=await submit('Plan a local ramp task: '+spec.text);
   const task=await terminal(run);
   const offered=task.history.find(row=>row.kind==='proposal')?.detail?.arguments?.procedure;
   const row={...spec,offered,task,elapsedMs:Date.now()-run.began};
   result.cases.push(row);
   await save();
   assert.equal(task.timing052.modelCalls,1);
   if(spec.wanted==='clarify'){
    assert.equal(task.status,'interrupted',task.reason);
    await assert.rejects(child(run.id));
    row.refusedBeforePhysics=true;
    await showMission(run.id);
    if(spec.missing)await capture('paired-ramp-missing-input.png');
   }else{
    row.completed=await finished(run,spec.wanted,1);
    if(spec.wanted===pair){
     assert.deepEqual(row.completed.evidence.original.second.samples,result.reversed.evidence.original.second.samples);
     assert.equal(row.completed.mission.audit.distanceDifferenceM,result.reversed.mission.audit.distanceDifferenceM);
    }else{
     assert.deepEqual(row.completed.evidence.original.samples,result.reversed.evidence.original.first.samples);
    }
   }
   assert.equal(offered,spec.wanted,JSON.stringify({request:spec.text,offered,reason:task.reason}));
   await save();
  }
  const modelCompare=result.cases.find(row=>row.wanted===pair).completed;
  const opened=await request('agency043','mission-open',{id:modelCompare.task.id});
  assert.equal(opened.id,modelCompare.mission.report.id);
  await showMission(modelCompare.task.id);
  await capture('paired-ramp-model-routing.png');

  assert.deepEqual(result.errors,[]);
  verify(process.cwd());
  result.passed=true;
  console.log(JSON.stringify({passed:true,correct:result.cases.length,sampleCount:result.reversed.mission.audit.samplesPerSetup,
   first:result.reversed.mission.audit.setups.first.distanceM,second:result.reversed.mission.audit.setups.second.distanceM,
   directMs:result.reversed.elapsedMs,plannedMs:result.cases.filter(c=>c.wanted===pair).map(c=>c.elapsedMs)}));
 }finally{
  if(app)await app.close().catch(()=>{});
  await save();
 }
}

main().catch(error=>{console.error(error);process.exitCode=1;});
