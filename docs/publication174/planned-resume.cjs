'use strict';

const assert=require('node:assert/strict');
const fs=require('node:fs/promises');
const path=require('node:path');
const {launch}=require('../../workspace-desktop/test/calibration-integration.cjs');
const {verify}=require('../../scripts/Study-Build095.cjs');

const model=process.env.FRAYNIX_TEST_RAMP_MODEL||'gpt-oss:20b';
const csv='angle_deg,friction,gravity_mps2,mass_kg,duration_s\n27,0.08,9.81,1,0.71\n31,0.04,9.81,1,0.71\n';
const goal='Plan a local ramp task and pause after reading: Which prepared setup covers more ground on the same clock?';

async function main(){
 const {source}=verify(process.cwd());
 const project=await fs.mkdtemp(path.resolve('build','ramp174-planned-resume-'));
 let app,page;
 const api=(route,args)=>app.evaluate((_,x)=>global.workspaceTest.api(x.route,x.args??undefined),{route,args:args??null});
 const until=async(check)=>{
  const limit=Date.now()+180000;
  while(Date.now()<limit){const v=await check();if(v)return v;await new Promise(r=>setTimeout(r,150));}
  throw Error('Timed out waiting for the model-routed checkpoint');
 };
 async function start(disable){
  if(disable)process.env.FRAYNIX_WORKSPACE_NO_MODELS='1';else delete process.env.FRAYNIX_WORKSPACE_NO_MODELS;
  Object.assign(process.env,{FRAYNIX_WORKSPACE_PERSISTENT:'0',FRAYNIX_WORKSPACE_PLAN_CPU_ONLY:'1',
    FRAYNIX_WORKSPACE_JAVA_HEAP_MIB:'768',FRAYNIX_WORKSPACE_JAVA_PROCESSORS:'2'});
  ({app,page}=await launch(project));
 }
 try{
  await start(false);
  await page.waitForFunction(name=>[...document.querySelector('#model').options].some(o=>o.value===name),model,{timeout:65000});
  await page.locator('#model').selectOption(model);
  await page.locator('#ramp-input-open').click();
  await page.locator('#ramp-csv').setInputFiles({name:'resume-pair.csv',mimeType:'text/csv',buffer:Buffer.from(csv)});
  await page.waitForFunction(()=>document.querySelector('#ramp-file-note').textContent.startsWith('CSV checked'));
  await page.locator('#ramp-input-save').click();
  await page.locator('#ramp-input-dialog').waitFor({state:'hidden'});
  await page.locator('#goal').fill(goal);await page.locator('#run').click();
  const {id}=await until(async()=>(await api('/api/agency043/state',{})).tasks.find(t=>t.goal===goal));
  const before=await until(async()=>{
   const t=await api('/api/agency043/read',{id});
   return t.status==='interrupted'&&t;
  });
  assert.match(before.reason,/resume to start the world experiment/i);
  assert.equal(before.timing052.modelCalls,1);
  const record=await api('/api/ramp-mission/read',{id});
  assert.equal(record.procedure,'local-ramp-comparison-v1');
  assert.deepEqual(record.stages.map(row=>row.name),['browser']);
  assert(await app.evaluate(()=>global.workspaceTest.crashEngine()));
  await new Promise(r=>setTimeout(r,220));
  await app.close().catch(()=>{});app=null;

  await start(true);
  assert.deepEqual((await api('/api/state')).models,[]);
  assert.deepEqual(await api('/api/ramp-mission/read',{id}),record);
  await page.locator('#mission044-open').click();await page.locator('#mission044-task').selectOption(id);
  await page.locator('#mission044-resume').click();
  const after=await until(async()=>{
   const t=await api('/api/agency043/read',{id});
   if(t.resumes>0&&t.status==='interrupted')throw Error(t.reason);
   return t.status==='completed'&&t;
  });
  assert.equal(after.resumes,1);
  assert.equal(after.timing052.modelCalls,1);
  assert.equal(after.modelDigest,before.modelDigest);
  const verified=await api('/api/ramp-mission/verify',{id});
  assert.equal(verified.procedure,record.procedure);
  assert.deepEqual(verified.stages[0],record.stages[0]);
  assert.equal(verified.canvasReceipt.checks.length,9);
  const result={passed:true,source,model,modelCallsBefore:before.timing052.modelCalls,
    modelCallsAfter:after.timing052.modelCalls,browserReceipt:record.stages[0],outcome:verified.audit};
  await fs.writeFile('build/ramp174-planned-resume.json',JSON.stringify(result,null,2)+'\n');
  console.log(JSON.stringify(result));
 }finally{if(app)await app.close().catch(()=>{});}
}

main().catch(error=>{console.error(error);process.exitCode=1;});
