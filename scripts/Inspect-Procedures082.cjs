'use strict';
const fs=require('node:fs'),path=require('node:path'),assert=require('node:assert/strict'),crypto=require('node:crypto');
const {inspectProcedure,inspectCommit,inspectCost}=require('./Inspect-Procedures080.cjs');
const sha=b=>crypto.createHash('sha256').update(b).digest('hex'),read=p=>JSON.parse(fs.readFileSync(p,'utf8')),sum=(xs,f)=>xs.reduce((n,x)=>n+f(x),0);
function inspect(folder){
 const r=read(path.join(folder,'result.json'));assert(r.passed);const checked=[];
 for(const item of [...r.training,...r.calibration.flatMap(x=>Object.values(x.results)),...r.arms.flatMap(x=>x.rows)]){
  const original=read(path.join(folder,item.originalFile)),root=path.join(item.project,'world-procedures-079');
  const proof=inspectProcedure(original,root);inspectCommit(original,root,item.goal);inspectCost(original,root,item.goal);checked.push(proof);
  if(item.status==='completed'){assert.deepEqual(item.before.profile,item.goal.taskCost080.profile);assert.equal(item.before.profile.cpus,r.budget.processors);assert.equal(item.before.configuredMaxHeapBytes,r.budget.heapMiB*1024*1024);assert.equal(item.beforeOS.affinity,item.afterOS.affinity);assert.equal(BigInt(item.beforeOS.affinity).toString(2).split('1').length-1,r.budget.processors);}
 }
 const micro={};for(const mode of ['reference','parallel']){const xs=r.micro.filter(x=>x.iteration>=0&&x.mode===mode);assert.equal(xs.length,6);micro[mode]={loadMillis:sum(xs,x=>x.scan.loadMillis),callerMillis:sum(xs,x=>x.callerMillis),cpuMillis:sum(xs,x=>(x.after.processCpuNanos-x.before.processCpuNanos)/1e6),bytes:sum(xs,x=>x.scan.verifiedBytes),loads:sum(xs,x=>x.scan.loads)};}
 assert.equal(micro.reference.bytes,micro.parallel.bytes);
 const arms={};for(const arm of r.arms){
  const rows=arm.rows,completed=rows.filter(x=>x.status==='completed');let last=r.initial;
  for(const x of rows){assert.equal(x.binding.expectedHash,last.expectedHash);assert.equal(x.binding.expectedRevision,last.expectedRevision);last=x.observed;}
  assert.equal(arm.final.expectedHash,last.expectedHash);const prefix=arm.mode+'-',boots=r.boots.filter(x=>x.label.startsWith(prefix));
  const osSamples=boots.flatMap(x=>fs.readFileSync(path.join(folder,x.osFile),'utf8').trim().split(/\r?\n/).filter(Boolean).map(JSON.parse));assert(osSamples.length>0);for(const sample of osSamples)assert.equal(BigInt(sample.affinity).toString(2).split('1').length-1,r.budget.processors);
  const jvm=rows.flatMap(x=>[x.before,...x.samples,x.after]);
  arms[arm.mode]={attempted:rows.length,completed:completed.length,completeMillis:sum(completed,x=>x.goal.taskCost080.totalMillis),callerMillis:sum(rows,x=>x.callerMillis),cpuMillis:sum(rows,x=>(x.after.processCpuNanos-x.before.processCpuNanos)/1e6),loadMillis:sum(rows,x=>x.scan.loadMillis),verifiedBytes:sum(rows,x=>x.scan.verifiedBytes),loads:sum(rows,x=>x.scan.loads),ticks:sum(rows,x=>x.job.arm.chargedTicks),stages:sum(rows,x=>x.job.arm.attempts.length),lookups:rows.filter(x=>x.job.decision.useMemory).length,estimated:rows.filter(x=>x.job.decision.cost080.available).length,executionEnvelopeMillis:arm.executionEnvelopeMillis,restartMillis:sum(arm.restarts,x=>x.millis),exportMillis:sum(rows,x=>x.exportMillis),persistenceMillis:arm.persistenceMillis,peakSampledHeapBytes:Math.max(...jvm.map(x=>x.heapUsedBytes)),peakSampledWorkingSetBytes:Math.max(...osSamples.map(x=>x.workingSetBytes)),peakWorkingSetBytes:Math.max(...osSamples.map(x=>x.peakWorkingSetBytes)),peakSampledPrivateBytes:Math.max(...osSamples.map(x=>x.privateBytes)),osSampleCount:osSamples.length,gcMillis:sum(rows,x=>sum(x.after.gc,y=>Math.max(0,y.millis))-sum(x.before.gc,y=>Math.max(0,y.millis))),finalRevision:arm.final.expectedRevision,savedSkill:arm.savedSkill||null};
 }
 const left=r.arms.find(x=>x.mode==='reference'),right=r.arms.find(x=>x.mode==='parallel'),pairs=[];
 for(let i=0;i<Math.min(left.rows.length,right.rows.length);i++){
  const a=left.rows[i],b=right.rows[i];assert.equal(a.id,b.id);assert.equal(a.status,b.status);assert.deepEqual(a.job.initialFrame,b.job.initialFrame);assert.deepEqual(a.job.decision,b.job.decision);assert.equal(a.job.arm.chargedTicks,b.job.arm.chargedTicks);assert.equal(a.scan.loads,b.scan.loads);assert.equal(a.scan.verifiedBytes,b.scan.verifiedBytes);
  if(a.status==='completed')assert.deepEqual(a.job.arm.programs[a.job.arm.selected].actualFrame,b.job.arm.programs[b.job.arm.selected].actualFrame);
  pairs.push({id:a.id,status:a.status,referenceMillis:a.goal.taskCost080?.totalMillis??null,parallelMillis:b.goal.taskCost080?.totalMillis??null,matchingOutcome:true});
 }
 const a=arms.reference,b=arms.parallel;
 return {passed:true,budget:r.budget,checked,arms,micro,pairs,trainingMillis:sum(r.training,x=>x.goal.taskCost080.totalMillis),calibrationMillis:r.fitMillis+sum(r.calibration.flatMap(x=>Object.values(x.results)),x=>x.goal.taskCost080.totalMillis),completeReductionPercent:100*(1-b.completeMillis/a.completeMillis),loaderReductionPercent:100*(1-b.loadMillis/a.loadMillis),microReductionPercent:100*(1-micro.parallel.loadMillis/micro.reference.loadMillis),scope:'One matched six-goal sequence pair per budget on one machine. Cold restart after goal three, final retention, same original verification. Heap caps do not cap total RAM. Windows sampled peaks and process peak working set are distinguished; no confidence-interval or general-speed claim.'};
}
if(require.main===module)try{const folders=process.argv.slice(2).map(x=>path.resolve(x)),profiles=folders.map(inspect);console.log(JSON.stringify({passed:true,profiles},null,2));}catch(e){console.error(e);process.exitCode=1;}
module.exports={inspect};
