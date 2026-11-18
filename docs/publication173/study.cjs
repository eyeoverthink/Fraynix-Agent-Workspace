'use strict';

const assert = require('node:assert/strict');
const {createHash} = require('node:crypto');
const fs = require('node:fs/promises');
const path = require('node:path');
const {launch} = require('../../workspace-desktop/test/calibration-integration.cjs');
const {verify} = require('../../scripts/Study-Build095.cjs');

const output = path.resolve('build', process.argv[2] || 'ramp173');
const project = path.join(output,'project');
const nap = ms => new Promise(resolve=>setTimeout(resolve,ms));
const hash = value=>createHash('sha256').update(value).digest('hex');
const modelName=process.env.FRAYNIX_TEST_RAMP_MODEL||'llama3.2:latest';
const csv='\uFEFFduration_s,friction,mass_kg,gravity_mps2,angle_deg\r\n0.83,0.025,1.45,10.9,35\r\n';

async function main(){
 const build=verify(process.cwd());
 await fs.mkdir(output);
 await fs.mkdir(project);
 Object.assign(process.env,{FRAYNIX_WORKSPACE_PERSISTENT:'0',FRAYNIX_WORKSPACE_JAVA_HEAP_MIB:'768',
   FRAYNIX_WORKSPACE_JAVA_PROCESSORS:'2',FRAYNIX_WORKSPACE_PLAN_CPU_ONLY:'1'});
 const result={passed:false,source:build.source,captures:[],errors:[]};
 let app,page;
 const api=(route,args)=>app.evaluate((_,req)=>global.workspaceTest.api(req.route,req.args??undefined),{route,args:args??null});
 const request=(operation,command,args)=>page.evaluate(input=>window.workspace.request(input),{operation,command,args});
 const parent=id=>api('/api/agency043/read',{id});
 const child=id=>api('/api/ramp-mission/read',{id});
 const proof=id=>api('/api/ramp-mission/verify',{id});
 async function until(read,label,timeout=45000){
  const end=Date.now()+timeout;
  while(Date.now()<end){const value=await read();if(value)return value;await nap(100);}
  throw Error('Timed out waiting for '+label);
 }
 async function start(noModels=true){
  if(noModels) process.env.FRAYNIX_WORKSPACE_NO_MODELS='1';
  else delete process.env.FRAYNIX_WORKSPACE_NO_MODELS;
  ({app,page}=await launch(project));
  page.on('pageerror',error=>result.errors.push(error.message));
  await app.evaluate(({BrowserWindow})=>BrowserWindow.getAllWindows().find(w=>w.isVisible()).setSize(1540,1020));
  assert((await app.evaluate(()=>global.workspaceTest.network())).enabled);
  if(noModels)assert.deepEqual((await api('/api/state')).models,[]);
  await page.locator('#ramp-input-open').waitFor();
 }
 async function capture(name){
  await page.evaluate(()=>new Promise(resolve=>requestAnimationFrame(()=>requestAnimationFrame(resolve))));
  const png=Buffer.from(await app.evaluate(()=>global.workspaceTest.capture()),'base64');
  await fs.writeFile(path.join(output,name),png);
  result.captures.push({name,sha256:hash(png)});
 }
 async function openInput(){
  await page.locator('#ramp-input-open').click();
  await page.locator('#ramp-input-dialog').waitFor();
 }
 async function saveInput(){
  await page.locator('#ramp-input-save').click();
  await page.locator('#ramp-input-dialog').waitFor({state:'hidden'});
  return api('/api/ramp-mission/bind',{path:new URL(await page.locator('#address').inputValue()).pathname});
 }
 async function openSource(source){
  const origin=await app.evaluate(()=>global.workspaceTest.origin());
  const url=origin+source.path;
  await page.locator('#address').fill(url);
  await page.locator('#navigate').click();
  await until(()=>app.evaluate((_,target)=>{
   const {browser}=global.workspaceTest.surfaces();
   return require('electron').webContents.fromId(browser).getURL()===target;
  },url),'local input page navigation');
 }
 async function submit(){
  const existed=new Set((await api('/api/agency043/state',{})).tasks.map(t=>t.id));
  await page.locator('#run').waitFor();
  assert(await page.locator('#run').isEnabled());
  const began=Date.now();
  await page.locator('#run').click();
  const created=await until(async()=>(await api('/api/agency043/state',{})).tasks.find(t=>!existed.has(t.id)),'new Java task');
  return {id:created.id,began};
 }
 async function terminal(id,expected,timeout=45000){
  return until(async()=>{
   const p=await parent(id);
   if(p.status===expected)return p;
   if(['completed','interrupted','cancelled'].includes(p.status))throw Error(p.reason);
  },expected,timeout);
 }
 async function showMission(id){
  await page.locator('#mission044-open').click();
  await page.locator('#mission044-task').selectOption(id);
  await page.waitForFunction(id=>document.querySelector('#mission044').dataset.taskId===id,id);
 }
 async function complete(run,calls,timeout){
  const p=await terminal(run.id,'completed',timeout);
  const elapsed=Date.now()-run.began;
  const mission=await proof(run.id);
  assert.equal(p.outcome,'verified');
  assert.equal(p.timing052.modelCalls,calls);
  assert.equal(mission.contractSha256,result.catalog.sha256);
  assert.deepEqual(mission.stages.map(row=>row.name),result.catalog.stages.map(row=>row.id));
  assert.equal(mission.canvasReceipt.checks.length,mission.inputFormat==='csv-v1'?10:9);
  const report=await api('/api/artifact/read',{id:mission.report.id});
  const evidence=JSON.parse(report.contents['experiment.json']);
  if(mission.inputFormat==='csv-v1'){
   assert.equal(report.contents['input.csv'],csv);
   assert.equal(mission.source.csvSha256,hash(csv));
   assert(report.sources.includes('ramp-csv:'+hash(csv)));
  }
  return {parent:p,mission,evidence,elapsedMs:elapsed};
 }

 try{
  await start();
  result.catalog=await api('/api/ramp-mission/catalog',{});
  assert.deepEqual(result.catalog.inputs,['form-v1','csv-v1']);
  assert.equal(result.catalog.stages.length,5);
  for(let i=1;i<result.catalog.stages.length;i++)
   assert.equal(result.catalog.stages[i-1].output,result.catalog.stages[i].input);
  await openInput();
  assert(await page.locator('#ramp-plan').isDisabled());
  await page.locator('#ramp-seconds').fill('.67');
  const form=await saveInput();
  result.form=await complete(await submit(),0);
  assert.equal(result.form.mission.inputFormat,'form-v1');

  await openInput();
  const picker=page.locator('#ramp-csv');
  await picker.setInputFiles({name:'invalid.csv',mimeType:'text/csv',buffer:Buffer.from(csv.replace('0.83','=1+2'))});
  await page.waitForFunction(()=>document.querySelector('#ramp-input-error').textContent.includes('decimal value'));
  assert(await page.locator('#ramp-input-save').isDisabled());
  result.invalidCsv=await page.locator('#ramp-input-error').textContent();
  await picker.setInputFiles({name:'ramp-input.csv',mimeType:'text/csv',buffer:Buffer.from(csv)});
  await page.waitForFunction(()=>document.querySelector('#ramp-file-note').textContent.startsWith('CSV checked'));
  assert.equal(await page.locator('#ramp-angleDeg').inputValue(),'35');
  assert.equal(await page.locator('#ramp-seconds').inputValue(),'0.83');
  await page.locator('#ramp-pause').check();
  await page.locator('#ramp-catalog > summary').click();
  await page.waitForFunction(()=>document.querySelectorAll('#ramp-catalog-steps li').length===5);
  await capture('ramp-csv-import.png');
  const source=await saveInput();
  assert.equal(source.version,form.version+1);
  assert.equal(source.id,form.id);
  assert.equal(source.csvSha256,hash(csv));

  const recovering=await submit();
  await terminal(recovering.id,'interrupted');
  await showMission(recovering.id);
  const observation=await child(recovering.id);
  assert.deepEqual(observation.stages.map(row=>row.name),['browser']);
  await app.evaluate(()=>global.workspaceTest.agency043.stop());
  await page.locator('#mission044-resume').click();
  await until(async()=>{
   const p=await parent(recovering.id);
   return p.resumes===1&&p.status==='waiting-host'&&p.effect?.tool==='localRamp.check';
  },'checkpoint immediately before real canvas verification');
  result.beforeStop=await child(recovering.id);
  assert.equal(result.beforeStop.status,'ready-report');
  await capture('ramp-csv-checkpoint.png');
  assert(await app.evaluate(()=>global.workspaceTest.crashEngine()));
  await nap(220);
  await app.close().catch(()=>{});
  app=null;

  await start();
  assert.deepEqual(await child(recovering.id),result.beforeStop);
  await showMission(recovering.id);
  await page.locator('#mission044-resume').click();
  await until(async()=>(await parent(recovering.id)).resumes===2,'recovered task admission');
  result.recovered=await complete(recovering,0);
  assert.deepEqual(result.recovered.mission.stages.slice(0,4),result.beforeStop.stages);
  assert.equal(result.recovered.mission.replay.sessionId,result.beforeStop.replay.sessionId);
  await page.waitForFunction(()=>document.querySelector('#test-badge').textContent==='TESTS PASSED');
  await capture('ramp-csv-verified.png');

  await openSource(source);
  await openInput();
  await page.locator('#ramp-pause').uncheck();
  await page.locator('#ramp-use-current').click();
  result.direct=await complete(await submit(),0);
  assert.deepEqual(result.direct.mission.source,source);
  await app.close();app=null;

  await start(false);
  await page.waitForFunction(name=>[...document.querySelector('#model').options].some(option=>option.value===name),
    modelName,{timeout:65000});
  await page.locator('#model').selectOption(modelName);
  await openSource(source);
  await openInput();
  await page.locator('#ramp-pause').uncheck();
  await page.locator('#ramp-plan').check();
  await page.locator('#ramp-use-current').click();
  assert.equal(await page.locator('#goal').inputValue(),'Plan the local ramp experiment.');
  result.planned=await complete(await submit(),1,180000);
  assert.deepEqual(result.direct.mission.choice,result.planned.mission.choice);
  assert.deepEqual(result.direct.mission.source,result.planned.mission.source);
  for(const key of ['original','replay'])
   assert.deepEqual(result.direct.evidence[key].samples,result.planned.evidence[key].samples);
  const plans=result.planned.parent.history.filter(row=>row.kind==='procedure-selected');
  assert.equal(plans.length,1);
  assert.equal(plans[0].detail.contractSha256,result.catalog.sha256);
  assert(result.planned.parent.modelDigest);
  await showMission(result.planned.parent.id);
  await capture('ramp-planner-verified.png');

  const opened=await request('agency043','mission-open',{id:recovering.id});
  assert.equal(opened.id,result.recovered.mission.report.id);
  assert.deepEqual(result.errors,[]);
  verify(process.cwd());
  result.passed=true;
  console.log(JSON.stringify({passed:true,format:result.direct.mission.inputFormat,distance:result.direct.mission.audit.distanceM,
    samples:result.direct.mission.audit.sampleCount,model:modelName,directMs:result.direct.elapsedMs,
    plannedMs:result.planned.elapsedMs,modelMs:result.planned.parent.timing052.modelMillis}));
 }finally{
  if(app)await app.close().catch(()=>{});
  await fs.writeFile(path.join(output,'result.json'),JSON.stringify(result,null,2)+'\n');
 }
}

main().catch(error=>{console.error(error);process.exitCode=1;});
