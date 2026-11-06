'use strict';
const fs=require('node:fs'),path=require('node:path'),assert=require('node:assert/strict');
const {inspectProcedure,inspectCommit,inspectCost}=require('./Inspect-Procedures080.cjs');
const read=p=>JSON.parse(fs.readFileSync(p,'utf8')),sum=(xs,f)=>xs.reduce((n,x)=>n+f(x),0);
function inspect(folder){
 const r=read(path.join(folder,'result.json'));assert(r.passed);const all=[...r.training,...r.warmups,...r.calibration.flatMap(x=>Object.values(x.results)),...r.rows],checked=[];
 for(const row of all){
  const original=read(path.join(folder,row.originalFile)),root=path.join(row.project,'world-procedures-079');
  checked.push(inspectProcedure(original,root));inspectCommit(original,root,row.goal);inspectCost(original,root,row.goal);
  assert.equal(row.status,'completed');assert.equal(row.before.profile.cpus,4);assert.equal(row.before.configuredMaxHeapBytes,768*1024*1024);assert.equal(row.beforeOS.affinity,row.afterOS.affinity);assert.equal(BigInt(row.beforeOS.affinity).toString(2).split('1').length-1,4);
  assert.equal(row.goal.taskCost080.workload.retain,row.retain);assert.equal(row.goal.taskCost080.workload.skill,!!row.skillName);
 }
 if(!r.group)return{passed:true,mode:r.mode,checked,learningMillis:sum(r.training,x=>x.goal.taskCost080.totalMillis)};
 for(const row of [...r.calibration.flatMap(x=>Object.values(x.results)),...r.rows]){
  if(r.group.warm)assert(r.warmups.some(x=>x.ownerBootId===row.ownerBootId&&x.before.pid===row.before.pid&&x.status==='completed'),'Warm condition requires a real same-owner completed warm-up');
  else assert.equal(all.filter(x=>x.ownerBootId===row.ownerBootId).length,1,'Cold condition has exactly one goal in this owner');
  if(row.policy!=='simple'){assert.equal(row.job.decision.candidate.warm,r.group.warm);assert.equal(row.job.decision.candidate.id,r.receiptId);}
 }
 for(const row of r.rows){assert(row.initialCloneVerified);assert.equal(row.bankBefore.sha256,r.frozenBankSha);assert.equal(row.persistence.hash,row.observed.expectedHash);if(r.group.skill)assert.deepEqual(row.persistence.skill,row.goal.savedSkill);}
 const cases=[];
 for(const id of [...new Set(r.rows.map(x=>x.id))]){
  const xs=Object.fromEntries(r.rows.filter(x=>x.id===id).map(x=>[x.policy,x]));assert.equal(Object.keys(xs).length,3);
  const costs=Object.fromEntries(Object.entries(xs).map(([k,v])=>[k,v.goal.taskCost080.totalMillis]));
  const estimate=xs.calibrated.job.decision.cost080;assert(estimate.available);const observedSaving=costs.simple-costs.memory;
  const task=read(path.join(xs.simple.project,'world-procedures-079',xs.simple.goalId,'cost080.json')).data.task;
  for(const x of Object.values(xs))assert.deepEqual(read(path.join(x.project,'world-procedures-079',x.goalId,'cost080.json')).data.task,task);
  cases.push({id,costs,estimate,observedSavingMillis:observedSaving,savingErrorMillis:estimate.savingMillis-observedSaving,simpleErrorMillis:estimate.simpleMillis-costs.simple,memoryErrorMillis:estimate.memoryMillis-costs.memory,insideEmpiricalInterval:observedSaving>=estimate.lowerSavingMillis&&observedSaving<=estimate.upperSavingMillis,selectedMemory:xs.calibrated.job.decision.useMemory,calibratedSavingMillis:costs.simple-costs.calibrated,calibrationOverheadMillis:xs.calibrated.scan.loadMillis,bankCounts:Object.fromEntries(Object.entries(xs).map(([k,x])=>[k,{before:x.bankBefore.count,after:x.bankAfter.count}]))});
 }
 const totals={};for(const mode of ['simple','memory','calibrated']){const rows=r.rows.filter(x=>x.policy===mode);totals[mode]={completed:rows.length,completeMillis:sum(rows,x=>x.goal.taskCost080.totalMillis),callerMillis:sum(rows,x=>x.callerMillis),ticks:sum(rows,x=>x.job.arm.chargedTicks),stages:sum(rows,x=>x.job.arm.attempts.length),checkMillis:sum(rows,x=>x.scan.loadMillis),verifiedBytes:sum(rows,x=>x.scan.verifiedBytes),retained:rows.filter(x=>x.bankAfter.count>x.bankBefore.count).length};}
 return{passed:true,group:r.group,checked,cases,totals,calibrationMillis:sum(r.calibration.flatMap(x=>Object.values(x.results)),x=>x.goal.taskCost080.totalMillis)+r.fitMillis,warmupMillis:sum(r.warmups,x=>x.goal.taskCost080.totalMillis),startupMillis:sum(r.boots,x=>x.startupMillis),persistenceMillis:sum(r.rows,x=>x.persistence.millis),coverage:r.coverage,scope:'Two held-out cases per group, descriptive measured comparisons. Original byte/native/cost/commit checks retained. Empirical intervals are not statistical confidence guarantees. Startup/warm-up are separate from uninterrupted goal time; calibration order and bank growth are retained.'};
}
if(require.main===module)try{console.log(JSON.stringify({passed:true,groups:process.argv.slice(2).map(x=>inspect(path.resolve(x)))},null,2));}catch(e){console.error(e);process.exitCode=1;}
module.exports={inspect};
