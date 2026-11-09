'use strict';
// External measurements describe cost; they never authorize motion or train a fit.
const assert=require('node:assert/strict');
const {duration}=require('./Costs095.cjs');
const near=(a,b)=>assert(Number.isFinite(a)&&Number.isFinite(b)&&Math.abs(a-b)<1e-5,'Cost arithmetic differs');
function intervals(owner,names){
 const total=duration(owner.interval);assert(owner.clockOwner);assert.deepEqual(owner.phases.map(p=>p.name),names);
 let end=owner.interval.start,sum=0;const costs={};
 for(const p of owner.phases){assert(p.start>=end&&p.end<=owner.interval.end,'Overlapping or escaped phase');costs[p.name]=duration(p);end=p.end;sum+=costs[p.name];}
 return {totalMillis:total,phases:costs,unattributedMillis:total-sum};
}
function task(row){
 const a=intervals(row,['setup','goal','audit']),g=row.phases[1];near(row.callerMillis,a.phases.goal);
 let end=g.start,restartMillis=0;
 for(const s of row.restarts){assert(s.start>=end&&s.end<=g.end,'Restart outside goal or overlapping');restartMillis+=duration(s);end=s.end;}
 assert(['completed','interrupted','cancelled'].includes(row.goal.status));assert.equal(row.goal.resumes,row.restarts.length);
 const completed=row.goal.status==='completed';if(completed){const c=row.goal.taskCost080;assert(c);assert.equal(c.eligible,row.restarts.length===0);if(row.restarts.length){assert.equal(c.totalMillis,0);assert(!c.clock089);}else assert(c.totalMillis>0&&c.totalMillis<=row.callerMillis);}
 return {...a,taskMillis:a.phases.goal,restartMillis,completed};
}
function continuity(rows,initial){
 let world=initial,bank=null,skills=null;
 for(let i=0;i<rows.length;i++){
  const r=rows[i];assert.equal(r.index,i);assert.deepEqual(r.binding,{worldId:world.worldId,expectedRevision:world.expectedRevision,expectedHash:world.expectedHash});assert.deepEqual(r.before.frame,world.frame);
  assert.equal(r.before.worldId,world.worldId);assert.equal(r.before.expectedHash,world.expectedHash);assert.equal(r.before.expectedRevision,world.expectedRevision);
  if(bank)assert.deepEqual(r.bankBefore,bank);if(skills)assert.deepEqual(r.skillsBefore,skills);
  const ok=task(r).completed;
  if(ok){assert.equal(r.observed.expectedRevision,world.expectedRevision+1);assert.equal(r.goal.worldCommit.hash,r.observed.expectedHash);assert.equal(r.bankAfter.length,r.bankBefore.length+(r.workload.retain?1:0));assert.equal(r.skillsAfter.length,r.skillsBefore.length+(r.workload.skill?1:0));for(const b of r.bankBefore)assert.deepEqual(r.bankAfter.find(x=>x.id===b.id),b);for(const s of r.skillsBefore)assert.deepEqual(r.skillsAfter.find(x=>x.id===s.id),s);}
  else{assert.equal(i,rows.length-1,'Cannot continue after refusal by silently resetting');assert.equal(r.observed.expectedRevision,world.expectedRevision);assert.equal(r.observed.expectedHash,world.expectedHash);assert.deepEqual(r.bankAfter,r.bankBefore);assert.deepEqual(r.skillsAfter,r.skillsBefore);assert(!r.goal.savedSkill);}
  world=r.observed;bank=r.bankAfter;skills=r.skillsAfter;
 }
 return {world,bank,skills};
}
function stream(s,expected){
 const names=['copy','startup','world',...s.rows.map(r=>'task/'+r.index),'shutdown','persistence','skill','close'];const a=intervals(s,names);
 assert(s.rows.length<=expected);const state=continuity(s.rows,s.initialWorld);
 for(const r of s.rows){assert.equal(r.clockOwner,s.clockOwner);const p=s.phases.find(x=>x.name==='task/'+r.index);assert(r.interval.start>=p.start&&r.interval.end<=p.end);assert.equal(r.mode,s.mode);}
 const completed=s.rows.length===expected&&s.rows.every(r=>r.goal.status==='completed');assert.equal(s.completed,completed);
 if(s.rows.length<expected)assert(s.rows.at(-1)?.goal.status!=='completed');
 assert.deepEqual(s.persistence.bank,state.bank);assert.deepEqual(s.persistence.skills,state.skills);assert.equal(s.persistence.world.expectedHash,state.world.expectedHash);assert.equal(s.persistence.world.expectedRevision,state.world.expectedRevision);
 return {...a,completed,goals:s.rows.length,successfulGoals:s.rows.filter(r=>r.goal.status==='completed').length,taskMillis:s.rows.reduce((n,r)=>n+task(r).taskMillis,0),bankEntries:state.bank.length,revision:state.world.expectedRevision,nativeTicks:s.rows.reduce((n,r)=>n+r.job.arm.chargedTicks,0)};
}
function summarize(r){
 assert(Number.isFinite(r.preparationMillis)&&r.preparationMillis>0,'Measured preparation cost required');assert.equal(r.streams.length,4);const pairs=[];let previous=-1;
 for(const s of r.streams){assert.equal(s.clockOwner,r.clock.id);assert(s.interval.start>=previous,'Streams overlapped');previous=s.interval.end;assert.equal(s.preparedSha256,r.preparedSha256);}
 for(let block=0;block<2;block++){
  const xs=r.streams.filter(s=>s.block===block);assert.equal(xs.length,2);assert.deepEqual(xs.map(s=>s.mode),block?['memory','simple']:['simple','memory']);const simple=xs.find(s=>s.mode==='simple'),memory=xs.find(s=>s.mode==='memory');assert.deepEqual(simple.profile,memory.profile);assert.deepEqual(simple.initialWorld.frame,memory.initialWorld.frame,'Initial copied world must match');
  const a=stream(simple,r.protocol.steps.length),b=stream(memory,r.protocol.steps.length),comparable=a.completed&&b.completed&&simple.demonstration?.goal.status==='completed'&&memory.demonstration?.goal.status==='completed';
  pairs.push({block,simple:a,memory:b,comparable,taskSavingMillis:comparable?a.taskMillis-b.taskMillis:null,streamSavingMillis:comparable?a.totalMillis-b.totalMillis:null});
 }
 const comparable=pairs.every(p=>p.comparable),sum=k=>pairs.reduce((n,p)=>n+p[k],0);return {pairs,comparable,taskSavingMillis:comparable?sum('taskSavingMillis'):null,streamSavingMillis:comparable?sum('streamSavingMillis'):null,preparationMillis:r.preparationMillis,netAfterPreparationMillis:comparable?sum('streamSavingMillis')-r.preparationMillis:null,scope:'Continuing-world descriptive comparison; later source states, bank contents and cache state may differ. No calibration or bank-size causal claim.'};
}
function inside(owner,phaseName,child){
 assert.equal(child.clockOwner,owner.clockOwner,'Child clock owner changed');const phase=owner.phases.find(p=>p.name===phaseName);assert(phase,'Missing owning phase');duration(child.interval);assert(child.interval.start>=phase.start&&child.interval.end<=phase.end,'Child interval escaped owning phase');
}
module.exports={intervals,task,continuity,stream,summarize,inside};
