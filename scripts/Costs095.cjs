'use strict';
// Accounting for actual externally observed intervals. Never motion authority.
const assert=require('node:assert/strict');
const phases=['copy','startup','setup','goal','audit','shutdown','reopen','close'];
const close=(a,b)=>assert(Number.isFinite(a)&&Number.isFinite(b)&&Math.abs(a-b)<1e-5,'Interval arithmetic differs');
function duration(span){assert(Number.isFinite(span.start)&&Number.isFinite(span.end)&&span.start>=0&&span.end>=span.start,'Invalid monotonic interval');return span.end-span.start;}
function account(row){
 const total=duration(row.interval);assert(row.clockOwner);assert.deepEqual(row.phases.map(p=>p.name),phases);
 let end=row.interval.start,sum=0;const costs={};
 for(const p of row.phases){assert(p.start>=end&&p.end<=row.interval.end,'Overlapping or out-of-owner interval');costs[p.name]=duration(p);sum+=costs[p.name];end=p.end;}
 close(row.callerMillis,costs.goal);const goal=row.phases.find(p=>p.name==='goal');let last=goal.start,restartMillis=0;
 for(const p of row.restarts){assert(p.start>=last&&p.end<=goal.end,'Restart outside goal or overlapping');restartMillis+=duration(p);last=p.end;}
 assert(row.goal.status==='completed'||row.goal.status==='interrupted'||row.goal.status==='cancelled');
 assert.equal(row.goal.resumes,row.restarts.length);
 if(row.goal.status==='completed'){
  const c=row.goal.taskCost080;assert(c);assert.equal(c.eligible,!row.restarts.length);
  if(row.restarts.length){assert.equal(c.totalMillis,0);assert(!c.clock089);}else assert(c.totalMillis>0&&c.totalMillis<=row.callerMillis);
 }
 return {totalMillis:total,phases:costs,unattributedMillis:total-sum,restartMillis,taskMillis:costs.goal,completed:row.goal.status==='completed'};
}
function paired(simple,memory){
 assert.equal(simple.mode,'simple');assert.equal(memory.mode,'memory');
 for(const k of ['condition','block','sourceSpecSha256','preparedSha256'])assert.deepEqual(simple[k],memory[k],'Mismatched '+k);
 assert.deepEqual(simple.workload,memory.workload);assert.deepEqual(simple.profile,memory.profile);
 assert.deepEqual(simple.order,memory.order);assert.deepEqual([...simple.order].sort(),['memory','simple']);
 assert(simple.interval.end<=memory.interval.start||memory.interval.end<=simple.interval.start,'Controls overlapped');
 assert.equal(simple.order[0],simple.interval.start<memory.interval.start?'simple':'memory');
 const a=account(simple),b=account(memory),comparable=a.completed&&b.completed;
 return {condition:simple.condition,block:simple.block,order:simple.order,comparable,simple:a,memory:b,taskSavingMillis:comparable?a.taskMillis-b.taskMillis:null,totalSavingMillis:comparable?a.totalMillis-b.totalMillis:null};
}
function aggregate(rows,preparationMillis){
 assert(Number.isFinite(preparationMillis)&&preparationMillis>=0);const groups=new Map();
 for(const row of rows){const k=row.block+'/'+row.condition,x=groups.get(k)||{};assert(!x[row.mode],'Duplicate arm');x[row.mode]=row;groups.set(k,x);}
 const pairs=[...groups.values()].map(x=>{assert(x.simple&&x.memory,'Missing paired arm');return paired(x.simple,x.memory);});
 const conditions=[...new Set(rows.map(r=>r.condition))];for(const c of conditions){const ps=pairs.filter(p=>p.condition===c);assert.equal(ps.length,2,'Both orders required');assert.deepEqual(ps.map(p=>p.order[0]).sort(),['memory','simple']);}
 const complete=pairs.every(p=>p.comparable),sum=k=>pairs.reduce((n,p)=>n+(p[k]||0),0);
 return {pairs,completed:rows.filter(r=>r.goal.status==='completed').length,goals:rows.length,allComparable:complete,preparationMillis,taskSavingMillis:complete?sum('taskSavingMillis'):null,totalSavingMillis:complete?sum('totalSavingMillis'):null,firstBatchNetMillis:complete?sum('totalSavingMillis')-preparationMillis:null,scope:'Two counterbalanced orders, cold copies, external surviving clock. Nested restart costs are included once. Failed outcomes cannot claim speed benefit.'};
}
module.exports={phases,duration,account,paired,aggregate};
