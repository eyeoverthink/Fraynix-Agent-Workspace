'use strict';
const assert=require('node:assert/strict');
const keys=['reads','bytes','canonicalChecks','hits','failures','readMillis'];
function work(segments){
  const result=Object.fromEntries(keys.map(k=>[k,0]));
  for(const {before,after} of segments)for(const k of keys){
    const n=after[k]-before[k];assert(Number.isFinite(n)&&n>=0,'Counter reset within segment: '+k);result[k]+=n;
  }
  // A failed canonical check can also increment failures; these are not disjoint.
  if(result.failures===0)assert.equal(result.reads,result.canonicalChecks+result.hits);
  return result;
}
function taskWork(row){
  const segments=[...row.segments];
  // Reopening and exporting the interrupted prefix can read originals before
  // the second timed segment. Counter snapshots retain that work as well.
  if(row.restartBoot)segments.push({before:Object.fromEntries(keys.map(k=>[k,0])),after:row.segments[1].before});
  return work(segments);
}
function recoveredClock(cost,job){
  assert.equal(cost.eligible,false);assert.equal(cost.totalMillis,0);
  assert(!cost.clock089);assert.equal(cost.executionMillis,job.activeMillis);
  assert.equal(job.resumes,1);assert(cost.reason.includes('excluded from calibration'));
}
function pair(reference,reuse){
  assert.equal(reference.bank,reuse.bank);assert.equal(reference.lifecycle,reuse.lifecycle);
  assert.deepEqual(reference.goal.taskCost080.task,reuse.goal.taskCost080.task);
  assert.equal(reference.job.arm.selected,reuse.job.arm.selected);
  // Restart timing changes the interruption tick. Compare completed native motion,
  // and report charged/observed work separately instead of forcing equality.
  if(reference.lifecycle==='uninterrupted'){
    assert.equal(reference.job.arm.chargedTicks,reuse.job.arm.chargedTicks);
    assert.equal(reference.job.arm.observedTicks,reuse.job.arm.observedTicks);
  }
  for(const row of [reference,reuse]){
    assert.equal(row.goal.taskCost080.eligible,row.lifecycle==='uninterrupted');
    assert(row.callerMillis>0);assert.equal(row.goal.worldCommit.revision,1);
  }
  return {bank:reference.bank,lifecycle:reference.lifecycle,selected:reuse.job.arm.selected,referenceMillis:reference.callerMillis,reuseMillis:reuse.callerMillis,savingMillis:reference.callerMillis-reuse.callerMillis,referenceWork:taskWork(reference),reuseWork:taskWork(reuse),referenceTicks:reference.job.arm.chargedTicks,reuseTicks:reuse.job.arm.chargedTicks};
}
module.exports={work,taskWork,pair,recoveredClock};
