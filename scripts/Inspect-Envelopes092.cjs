'use strict';
// Reuses the established independent native arithmetic, then verifies new
// cross-boot accounting without making interrupted costs eligible for fitting.
const fs=require('node:fs'),path=require('node:path'),crypto=require('node:crypto'),assert=require('node:assert/strict');
const {inspectProcedure,inspectCommit,inspectCost}=require('./Inspect-Procedures090.cjs'),{canonical}=require('./Inspect-Spatial040.cjs'),{unpack}=require('./Inspect-Counterfactual045.cjs'),{work,taskWork,pair}=require('./Work092.cjs');
const hash=x=>crypto.createHash('sha256').update(canonical(x)).digest('hex'),read=p=>JSON.parse(fs.readFileSync(p,'utf8'));
const repo=path.resolve(__dirname,'..'),out=path.join(repo,'build/ui092-study'),r=read(path.join(out,'result.json')),root=path.join(r.project,'world-procedures-079');assert(r.passed);assert.equal(r.rows.length,20);
function owned(file){const e=read(file);assert.equal(e.format,'java-agent-042-v1');assert.equal(hash(e.data),e.sha256);return e.data;}
function resumedCost(x,row){
 const c=x.cost080,j=x.job;assert.deepEqual(c,owned(path.join(root,j.id,'cost080.json')));assert.deepEqual(c,row.goal.taskCost080);
 assert.equal(c.id,j.id);assert.equal(c.code,j.code);assert.equal(c.jobSha256,hash(j));assert.equal(c.mode,j.mode);assert.equal(c.usedMemory,j.decision.useMemory);assert.equal(c.preferred,j.decision.preferred);assert.equal(c.receiptId,j.decision.candidate.id);assert.equal(c.warm,j.decision.candidate.warm);
 assert.deepEqual(c.goal.worldCommit,row.goal.worldCommit);assert(c.goal.poseView.passed);assert.equal(c.completed,true);assert.equal(c.eligible,false);assert(!c.clock089);assert.equal(j.resumes,1);assert.equal(row.goal.resumes,1);
 assert(c.totalMillis>0&&c.totalMillis<row.callerMillis);assert(c.executionMillis<=c.totalMillis);assert.deepEqual(c.spec,j.spec);
 assert(Math.abs(c.nativeStageMillis-j.arm.attempts.reduce((n,a)=>n+a.wallMillis,0))<1e-6);
 const prefix=read(path.join(out,row.prefixFile)),runs=new Map(x.originals.map(p=>{const n=unpack(p);return[n.id,n];}));
 for(const p of prefix.originals){const prior=unpack(p);assert.deepEqual(runs.get(prior.id).samples.slice(0,prior.samples.length),prior.samples);}
 for(const a of prefix.job.arm.attempts)assert(j.arm.attempts.some(b=>b.id===a.id&&b.stage===a.stage));
 assert(j.arm.attempts.some(a=>a.unknownTail&&a.chargedTicks===14400));assert.equal(row.interrupted.status,'interrupted');assert(row.restartBoot.initial.entries<=row.restartBoot.initial.canonicalChecks);assert(row.prefixPreserved);assert.equal(row.segments.length,2);assert(row.restartMillis>=row.restartBoot.callerMillis);
 return c;
}
const checks=[];for(const row of r.rows){
 const x=read(path.join(out,row.originalFile));assert.deepEqual(x.job,row.job);const p=inspectProcedure(x,root);assert(p.success);inspectCommit(x,root,row.goal);
 const c=row.lifecycle==='restarted'?resumedCost(x,row):inspectCost(x,root,row.goal);assert(c.totalMillis<=row.callerMillis);assert.equal(row.resource.profile.cpus,2);assert.equal(row.resource.configuredMaxHeapBytes,512*1024*1024);assert.equal(row.beforeOS.affinity,row.afterOS.affinity);assert.equal(row.bankBefore.length,row.retain?row.bank-1:row.bank);
 assert(row.fullVerify.passed);assert.equal(row.fullVerify.id,row.goalId);const w=taskWork(row);assert.equal(w.failures,0);assert(w.bytes>0);if(row.mode==='reuse')assert(w.hits>0);else{const execution=work(row.segments);assert.equal(execution.hits,0);assert.equal(execution.canonicalChecks,execution.reads);}
 checks.push({goalId:row.goalId,id:row.id,bank:row.bank,lifecycle:row.lifecycle,mode:row.mode,ticks:p.ticks,work:w,callerMillis:row.callerMillis,costEligible:c.eligible,partialOrCompleteCostMillis:c.totalMillis,restartMillis:row.restartMillis||0});console.log('PASS original native/source/outcome/commit',row.bank,row.lifecycle,row.mode);
}
const pairs=[];for(const bank of r.protocol.banks)for(const lifecycle of ['uninterrupted','restarted']){const rows=r.rows.filter(x=>x.bank===bank&&x.lifecycle===lifecycle);assert.equal(rows.length,2);pairs.push({...pair(rows.find(x=>x.mode==='reference'),rows.find(x=>x.mode==='reuse')),order:rows.map(x=>x.mode)});}
const audits=r.audits.map(a=>({...a,work:work([a])}));assert.equal(audits.length,60);for(const a of audits){assert.equal(a.work.failures,0);assert.deepEqual(a.result,r.rows.find(x=>x.goalId===a.id).fullVerify);if(a.mode==='reference')assert.equal(a.work.canonicalChecks,a.work.reads);assert(a.after.entries<=64);}
const auditPasses=['reference','cold-table','second-pass'].map(pass=>{const rows=audits.filter(x=>x.pass===pass);return {pass,goals:rows.length,callerMillis:rows.reduce((n,x)=>n+x.callerMillis,0),...Object.fromEntries(['reads','bytes','canonicalChecks','hits','readMillis'].map(k=>[k,rows.reduce((n,x)=>n+x.work[k],0)])),lastEntries:rows.at(-1).after.entries};});
const envelopes=new Set();for(const row of r.rows)for(const a of row.job.arm.attempts)if(a.sha256)envelopes.add(crypto.createHash('sha256').update(fs.readFileSync(path.join(root,row.goalId,'attempts',a.id,'original.json'))).digest('hex'));assert(envelopes.size>64);assert.equal(auditPasses[2].lastEntries,64);
assert.equal(r.checks.length,1);assert.equal(r.captures.length,4);assert.deepEqual(r.errors,[]);
const totals=Object.fromEntries(['reference','reuse'].map(mode=>[mode,{callerMillis:pairs.reduce((n,p)=>n+p[mode+'Millis'],0),...Object.fromEntries(['reads','bytes','canonicalChecks','hits','readMillis'].map(k=>[k,pairs.reduce((n,p)=>n+p[mode+'Work'][k],0)]))}]));
const preparation=r.rows.filter(x=>x.retain).reduce((n,x)=>n+x.callerMillis,0),allBoots=r.boots.reduce((n,x)=>n+x.callerMillis,0),restartBoots=r.rows.reduce((n,x)=>n+(x.restartBoot?.callerMillis||0),0);
const result={passed:true,verifiedAt:new Date().toISOString(),source:r.source,goals:20,measuredGoals:12,checks,pairs,totals,auditPasses,distinctOriginalEnvelopes:envelopes.size,preparationMillis:preparation,allBootMillis:allBoots,initialBootMillisOutsideTasks:allBoots-restartBoots,restartBootMillisAlreadyIncludedInTaskTimes:restartBoots,exportMillis:r.rows.reduce((n,x)=>n+x.exportMillis,0),postTaskVerifyMillis:r.rows.reduce((n,x)=>n+x.fullVerifyMillis,0),scope:'Same compiled production runtime. Fresh synthetic executions of one existing scene family, eight similar preparation variants, alternating mode order. Six paired comparisons, not population confidence. Bank growth covaries with project history. Restart caller time includes close/startup/prefix export/resume and OS pinning; post-resume cost080 remains calibration-ineligible. Every original read/outcome check remains active. Independent arithmetic is not a second physics solver.'};
fs.writeFileSync(path.join(repo,'build/analysis092.json'),JSON.stringify(result,null,2)+'\n');console.log(JSON.stringify({passed:true,pairs: pairs.map(x=>({bank:x.bank,lifecycle:x.lifecycle,savingMillis:x.savingMillis})),totals,auditPasses},null,2));
