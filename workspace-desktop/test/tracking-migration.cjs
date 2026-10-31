'use strict';
const {launch}=require('./calibration-integration.cjs'),fs=require('node:fs/promises'),path=require('node:path'),os=require('node:os'),assert=require('node:assert/strict');
const sleep=ms=>new Promise(r=>setTimeout(r,ms));
async function main(){
 const output=path.resolve(__dirname,'../test-output'),control=JSON.parse(await fs.readFile(path.join(output,'tracking-controls-integration.json'),'utf8'));
 const legacyRoot=process.env.FRAYNIX_LEGACY_023_CHECKOUT||path.resolve(__dirname,'../../../fraynix-workspace-qa023-release');
 const oldLaunch=require(path.join(legacyRoot,'workspace-desktop/test/calibration-integration.cjs')).launch;
 const project=await fs.mkdtemp(path.join(os.tmpdir(),'fraynix-tracking-migration024-')),checks=[];
 let {app,page}=await oldLaunch(project);const api=(route,args={})=>app.evaluate((_,x)=>global.workspaceTest.api(x.route,x.args),{route,args:route==='/api/state'?undefined:args});
 try{
  await page.locator('#perception-tab').click();await page.locator('#tracking-lab > summary').click();
  await page.waitForFunction(()=>!document.querySelector('#tracking-files').disabled);
  await page.locator('#tracking-files').setInputFiles(['development','aspect'].map(n=>path.join(control.project,'controls',n+'.ftrack.json')));
  await page.waitForFunction(()=>document.querySelectorAll('#tracking-datasets input').length===2&&document.querySelector('#tracking-files').value==='');
  const oldState=await api('/api/tracking/state');assert.equal(oldState.comparisonCandidate,undefined);assert(oldState.profiles.some(p=>p.id==='adaptive-v2'));assert(!oldState.profiles.some(p=>p.id==='flow-v3'));
  await page.locator('#tracking-auto').uncheck();await page.locator('#tracking-evaluate').click();let job;
  for(let i=0;i<600;i++){job=(await api('/api/tracking/state')).jobs.at(-1);if(job?.status==='completed')break;await sleep(50);}
  assert.equal(job.status,'completed');assert.equal(job.candidateProfile,undefined);assert(job.gate.passed);await page.waitForFunction(()=>document.querySelector('#perception-view').hidden);
  const original=await api('/api/tracking/read',{id:job.id});await app.close();({app,page}=await launch(project));
  await page.locator('#perception-tab').click();await page.locator('#tracking-lab > summary').click();await page.waitForFunction(()=>document.querySelector('#tracking-view-profile').value==='adaptive-v2');
  const migrated=await api('/api/tracking/read',{id:job.id});assert.deepEqual(migrated,original);assert.equal((await api('/api/tracking/state')).comparisonCandidate,'flow-v3');
  checks.push('A genuine edition023 process creates an old-schema comparison; edition024 preserves the complete record and defaults its inspector to the original adaptive-v2 profile');
  await page.waitForFunction(()=>!document.querySelector('#tracking-report').disabled);await page.locator('#tracking-report').click();await page.waitForFunction(()=>document.querySelector('#perception-view').hidden);
  let report;for(let i=0;i<300;i++){const artifacts=await api('/api/state');report=await api('/api/artifact/read',{id:artifacts.artifacts.at(-1).id});if(report.test?.passed)break;await sleep(50);}assert(report.test?.passed);assert(report.contents['trace-0-adaptive-v2.json']);assert.equal(JSON.parse(report.contents['protocol.json']).edition,'023');
  checks.push('Reopened legacy report retains its original protocol and traces while passing actual canvas assertions under the new app');
  await page.locator('#perception-tab').click();await page.waitForFunction(()=>!document.querySelector('#tracking-promote').disabled);await page.locator('#tracking-promote').click();await page.waitForFunction(()=>document.querySelector('#tracking-active').textContent.includes('adaptive-v2'));
  await page.locator('#tracking-rollback').click();await page.waitForFunction(()=>document.querySelector('#tracking-active').textContent.includes('rgb-v1'));assert.equal((await api('/api/tracking/state')).generation,2);
  checks.push('A passing legacy job still activates its original installed profile and rolls back through the actual buttons, without adopting flow-v3 or changing its scores');
  const result={passed:true,date:new Date().toISOString(),project,legacyRoot,checks,legacyJobId:job.id,originalGate:original.gate,scope:'Fresh explicitly synthetic migration project, created by the installed published023 runtime. Only the two generated source packages are reused; no personal app database or model weights are copied.'};
  await fs.writeFile(path.join(output,'tracking-migration.json'),JSON.stringify(result,null,2));console.log(JSON.stringify({passed:true,checks:checks.length,legacyJobId:job.id}));
 }finally{await app.close().catch(()=>{});}
}
if(require.main===module)main().catch(e=>{console.error(e);process.exitCode=1;});module.exports={main};
