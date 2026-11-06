'use strict';
// Retain the independent original-byte, arithmetic and commit checks from 080.
const fs=require('node:fs'),path=require('node:path'),assert=require('node:assert/strict');
const {inspectProcedure,inspectCommit,inspectCost}=require('./Inspect-Procedures080.cjs');
function aggregate(record){
 const rows=record.rows.filter(r=>r.split==='unseen');assert.equal(rows.length,8);
 const sum=(xs,fn)=>xs.reduce((a,x)=>a+fn(x),0),methods={};
 for(const mode of ['reference','parallel']){
  const xs=rows.map(r=>r.results[mode]);methods[mode]={successes:xs.filter(x=>x.status==='completed').length,
   completeMillis:sum(xs,x=>x.goal.taskCost080.totalMillis),callerMillis:sum(xs,x=>x.wallMillis),
   estimationMillis:sum(xs,x=>x.scan.loadMillis),hashMillis:sum(xs,x=>x.scan.hashMillis),loads:sum(xs,x=>x.scan.loads),
   verifiedBytes:sum(xs,x=>x.scan.verifiedBytes),ticks:sum(xs,x=>x.job.arm.chargedTicks),stages:sum(xs,x=>x.job.arm.attempts.length),
   memoryLookups:xs.filter(x=>x.job.decision.useMemory).length};
 }
 for(const row of rows){const a=row.results.reference,b=row.results.parallel;
  assert.deepEqual(a.job.decision,b.job.decision);assert.equal(a.status,b.status);assert.equal(a.job.arm.chargedTicks,b.job.arm.chargedTicks);
  assert.deepEqual(a.job.arm.programs[a.job.arm.selected].actualFrame,b.job.arm.programs[b.job.arm.selected].actualFrame);
  assert.equal(a.scan.loads,b.scan.loads);assert.equal(a.scan.verifiedBytes,b.scan.verifiedBytes);assert.equal(a.scan.failures+b.scan.failures,0);
 }
 const micro={};for(const mode of ['reference','parallel']){const xs=record.scanBenchmark.filter(x=>x.iteration>=0&&x.mode===mode);assert.equal(xs.length,6);micro[mode]={loads:sum(xs,x=>x.loads),verifiedBytes:sum(xs,x=>x.verifiedBytes),loadMillis:sum(xs,x=>x.loadMillis),hashMillis:sum(xs,x=>x.hashMillis),callerMillis:sum(xs,x=>x.callerMillis)};}
 assert.equal(micro.reference.verifiedBytes,micro.parallel.verifiedBytes);
 const evidenceBytes=Object.values(record.model.evidence).flat().reduce((n,e)=>n+e.bytes,0);
 for(const row of rows)for(const mode of ['reference','parallel']){const x=row.results[mode].scan;assert.equal(x.mode,mode);assert.equal(x.readers,mode==='parallel'?2:1);assert(x.loads>0);assert.equal(x.verifiedBytes,evidenceBytes*x.loads);assert(x.hashMillis>=0&&x.loadMillis>=x.hashMillis);}
 const a=methods.reference,b=methods.parallel;
 return {methods,micro,matchingDecisionsAndNativeWork:true,estimationImproved:b.estimationMillis<a.estimationMillis,
  estimationReductionPercent:100*(1-b.estimationMillis/a.estimationMillis),microReductionPercent:100*(1-micro.parallel.loadMillis/micro.reference.loadMillis),
  completeTaskFaster:b.completeMillis<a.completeMillis,completeReductionPercent:100*(1-b.completeMillis/a.completeMillis),
  trainingMillis:sum(record.training,x=>x.goal.taskCost080.totalMillis),calibrationMillis:sum(record.calibration.flatMap(x=>Object.values(x.results)),x=>x.goal.taskCost080.totalMillis)+record.calibrationFitMillis,
  scope:'Eight controlled pairs on one machine, six regression cases and two prospective variants; no retuning, no general speed guarantee. Final timing-record write/caller transport outside Java timing; caller measured separately.'};
}
if(require.main===module)try{
 const folder=path.resolve(process.argv[2]),record=JSON.parse(fs.readFileSync(path.join(folder,'result.json'),'utf8')),root=path.join(record.project,'world-procedures-079');assert(record.passed);
 const items=[...record.training,...record.calibration.flatMap(r=>Object.values(r.results)),...record.rows.filter(r=>r.split==='unseen').flatMap(r=>Object.values(r.results))];
 const checked=items.map(item=>{const x=JSON.parse(fs.readFileSync(path.join(folder,item.originalFile),'utf8'));const result=inspectProcedure(x,root);inspectCommit(x,root,item.goal);inspectCost(x,root,item.goal);return result;});
 console.log(JSON.stringify({passed:true,checked,summary:aggregate(record)},null,2));
}catch(e){console.error(e);process.exitCode=1;}
module.exports={aggregate};
