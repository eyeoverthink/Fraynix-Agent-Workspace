'use strict';
const fs=require('node:fs'),path=require('node:path'),assert=require('node:assert/strict'),crypto=require('node:crypto');
const {inspectProcedure,inspectCommit,inspectCost}=require('./Inspect-Procedures093.cjs'),{canonical}=require('./Inspect-Spatial040.cjs'),{unpack,metrics}=require('./Inspect-Counterfactual045.cjs');
const hash=x=>crypto.createHash('sha256').update(canonical(x)).digest('hex'),read=p=>JSON.parse(fs.readFileSync(p,'utf8'));
const repo=path.resolve(__dirname,'..'),out=path.join(repo,'build/ui093-study'),r=read(path.join(out,'result.json')),root=path.join(r.project,'world-procedures-079');assert(r.passed);assert.equal(r.rows.length,6);
function owned(p){const e=read(p);assert.equal(hash(e.data),e.sha256);return e.data;}
const checks=[];
for(const row of r.rows){
 const x=read(path.join(out,row.originalFile)),j=x.job;assert.deepEqual(j,row.job);assert.deepEqual(owned(path.join(root,j.id,'goal.json')),j);inspectCommit(x,root,row.goal);
 const runs=new Map(x.originals.map(p=>{const z=unpack(p);return[z.id,z];}));
 for(const p of row.prefixes){const prefix=read(path.join(out,p.file));assert.equal(p.goal.status,'interrupted');assert(prefix.job.arm.attempts.some(a=>a.stage===p.stage&&a.status==='interrupted'));for(const packed of prefix.originals){const prior=unpack(packed),current=runs.get(prior.id);assert.deepEqual(current.samples.slice(0,prior.samples.length),prior.samples);if(prior.status==='completed')assert.deepEqual(current,prior);}}
 const extra=j.arm.attempts.filter(a=>a.stage.startsWith('reforecast/'));let result;
 if(row.kind==='reforecast-cancel'){
  assert.equal(row.goal.status,'cancelled');assert(!row.goal.worldCommit);assert.deepEqual(x.cost080,{});assert.equal(row.observed.expectedRevision,0);assert.equal(row.observed.pending.goalId,j.id);assert.equal(extra.length,1);assert.equal(extra[0].status,'cancelled');assert(!j.arm.attempts.some(a=>a.stage==='actual/1'));
  for(const a of j.arm.attempts){const n=runs.get(a.id);assert.equal(hash(n),a.sha256);assert.equal(hash(n.definition),a.sourceSha256);assert.equal(hash(n.plan),a.planSha256);assert.equal(n.status,a.status);assert.equal(a.chargedTicks,a.unknownTail?14400:n.final?.tick??n.samples.at(-1)?.tick??0);if(n.final)assert.equal(metrics(n).passed,a.metrics.passed);}result={success:false,ticks:j.arm.chargedTicks,stages:j.arm.attempts.length};
 }else{
  result=inspectProcedure(x,root);assert(result.success);assert(row.fullVerify.passed);assert.equal(row.goal.worldCommit.revision,1);assert.equal(row.goal.resumes,row.prefixes.length);assert.equal(extra.length,row.prefixes.length?1:0);
  if(!row.prefixes.length)inspectCost(x,root,row.goal);else{
   const c=x.cost080;assert.deepEqual(c,owned(path.join(root,j.id,'cost080.json')));const summary={...c};delete summary.goal;delete summary.spec;delete summary.task;assert.deepEqual(summary,row.goal.taskCost080);assert.equal(c.jobSha256,hash(j));assert.equal(c.code,j.code);assert.equal(c.id,j.id);assert.equal(c.completed,true);assert.equal(c.eligible,false);assert.equal(c.totalMillis,0);assert(!c.clock089);assert.equal(c.executionMillis,j.activeMillis);assert.equal(j.resumes,row.prefixes.length);assert.deepEqual(c.goal.worldCommit,row.goal.worldCommit);assert(c.goal.poseView.passed);assert.deepEqual(c.spec,j.spec);assert(Math.abs(c.nativeStageMillis-j.arm.attempts.reduce((n,a)=>n+a.wallMillis,0))<1e-6);
   assert(extra[0].metrics.passed);assert.equal(runs.get(extra[0].id).resumes||0,row.kind==='reforecast-restart'?1:0);assert.equal(extra[0].unknownTail||false,row.kind==='reforecast-restart');
  }
 }
 assert(row.prefixPreserved);checks.push({kind:row.kind,id:j.id,success:result.success,ticks:result.ticks,stages:result.stages,callerMillis:row.callerMillis,restartMillis:row.restartMillis,reforecasts:extra.length,forecastChargedTicks:extra.reduce((n,a)=>n+a.chargedTicks,0),forecastObservedTicks:extra.reduce((n,a)=>n+a.observedTicks,0),forecastWallMillis:extra.reduce((n,a)=>n+a.wallMillis,0),resumes:j.resumes,costEligible:x.cost080?.eligible||false});console.log('PASS093 original source, motion, provenance and commit',row.kind);
}
assert.equal(r.boots.length,11);for(const b of r.boots){assert.equal(b.resources.profile.cpus,2);assert.equal(b.resources.configuredMaxHeapBytes,512*1024*1024);assert.equal(BigInt(b.pinned.affinity).toString(2).split('1').length-1,2);}assert.equal(r.captures.length,6);assert.deepEqual(r.errors,[]);
const result={passed:true,source:r.source,verifiedAt:new Date().toISOString(),checks,completed:checks.filter(x=>x.success).length,cancelled:1,boots:r.boots.length,scope:'Fresh labelled synthetic native experiments on two actual CPUs/512MiB; one baseline, two early recovery conditions, second interruption and cancellation. Graceful isolated-app shutdown, not power failure. Complete caller times include restarts; resumed monotonic totals unavailable and excluded from calibration. Independent arithmetic is not a second physics solver. No physical accuracy or LLM-training claim.'};fs.writeFileSync(path.join(repo,'build/analysis093.json'),JSON.stringify(result,null,2)+'\n');console.log(JSON.stringify(result,null,2));
