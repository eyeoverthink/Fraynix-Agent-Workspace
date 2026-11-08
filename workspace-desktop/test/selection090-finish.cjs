'use strict';
// Read-only completion of the original study after its summary/export comparison failed.
// This never reruns, substitutes or changes any timed goal.
const {launch}=require('./calibration-integration.cjs'),fs=require('node:fs/promises'),path=require('node:path'),assert=require('node:assert/strict'),cp=require('node:child_process');
(async()=>{
 const repo=path.resolve(__dirname,'../..'),out=path.join(repo,'build/ui090-study'),record=JSON.parse(await fs.readFile(path.join(out,'checkpoint.json'),'utf8'));
 assert.equal(record.training.length,1);assert.equal(record.calibration.length,6);assert.equal(record.rows.length,18);assert.equal(record.finalVerification.length,18);assert(record.changedRecentCostRefusal);assert(record.changedTraceRefusal);
 const oldLog=await fs.readFile(path.join(repo,'build/study090.log'),'utf8');assert(oldLog.includes('AssertionError'),'Retain the original failed invocation');
 require('../../scripts/Study-Build090.cjs').verify(repo);
 process.env.FRAYNIX_WORKSPACE_PERSISTENT='0';process.env.FRAYNIX_WORKSPACE_PLAN_CPU_ONLY='1';process.env.FRAYNIX_WORKSPACE_JAVA_HEAP_MIB='512';process.env.FRAYNIX_WORKSPACE_JAVA_PROCESSORS='2';
 const at=performance.now(),{app,page}=await launch(record.project),errors=[],checks=[];page.on('pageerror',e=>errors.push(e.message));
 const api=(route,args={})=>app.evaluate((_,x)=>global.workspaceTest.api(x.route,x.args),{route,args});
 try{
  await page.waitForFunction(()=>window.agency043View);const startupMillis=performance.now()-at,res=await api('/api/resources082/state');
  const pinned=JSON.parse(cp.execFileSync('pwsh',['-NoProfile','-File',path.join(repo,'scripts/Measure-Resources082.ps1'),'-Action','pin','-TargetPid',String(res.pid),'-Processors','2','-Checkout',repo],{encoding:'utf8',windowsHide:true}));
  for(const id of [record.finalReview.covered,record.finalReview.uncovered]){
   const row=record.rows.find(r=>r.goalId===id),original=JSON.parse(await fs.readFile(path.join(out,row.originalFile),'utf8')),exported=await api('/api/procedures079/export',{id});
   assert.deepEqual(exported.job,original.job);assert.deepEqual(exported.cost080,original.cost080);
   const verified=await api('/api/procedures090/verify',{id}),world=await api('/api/world/observe',{worldId:row.binding.worldId});assert(verified.passed);assert.equal(world.expectedHash,row.observed.expectedHash);assert.equal(world.expectedRevision,row.observed.expectedRevision);checks.push({id,passed:true,worldHash:world.expectedHash});
  }
  const id=record.finalReview.uncovered;await app.evaluate((_,id)=>global.workspaceTest.goals.execute('goal.read',{id}),id);await page.waitForFunction(id=>document.querySelector('#java-goal').dataset.goalId===id&&document.querySelector('#java-goal-status').textContent==='completed',id);await page.locator('#java-goal-report').click();
  for(const w of [1600,1100]){await app.evaluate(({BrowserWindow},w)=>BrowserWindow.getAllWindows().find(x=>x.isVisible()).setSize(w,1100),w);await page.waitForTimeout(400);assert(await page.evaluate(()=>document.documentElement.scrollWidth<=innerWidth));await fs.writeFile(path.join(out,w===1600?'cold-restart-report.png':'cold-restart-report-1100.png'),Buffer.from(await app.evaluate(()=>global.workspaceTest.capture()),'base64'));}
  assert.deepEqual(errors,[]);record.completionRecovery={source:cp.execFileSync('git',['rev-parse','HEAD'],{cwd:repo,encoding:'utf8'}).trim(),reason:'The original cold check compared a complete exported cost with the abbreviated UI summary. Original timed outcomes unchanged; complete exports now compared with their original saved exports, then full goal audits and world hashes rechecked.',startupMillis,pid:res.pid,pinned,checks,errors,callerMillis:performance.now()-at};
  record.passed=true;record.finishedAt=new Date().toISOString();
 }finally{await app.close();}
 require('../../scripts/Study-Build090.cjs').verify(repo);await fs.writeFile(path.join(out,'result.json'),JSON.stringify(record,null,2));console.log('PASS read-only cold completion; all timed originals preserved');
})().catch(e=>{console.error(e);process.exitCode=1;});
