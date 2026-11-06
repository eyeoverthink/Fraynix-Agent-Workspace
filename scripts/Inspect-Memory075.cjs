'use strict';
const fs=require('node:fs'),path=require('node:path'),assert=require('node:assert/strict');
const {unpack,metrics}=require('./Inspect-Counterfactual045.cjs');
const families=['existing-controller-order','reversed-target-order','half-force'];
const methods=['search','simple','memory','cached'];
const distance=(a,b)=>Math.hypot(...a.map((v,k)=>v-b[k]));
function angle(a,b){const[x,y,z,w]=a,[aa,bb,c,d]=[-b[0],-b[1],-b[2],b[3]];return Math.atan2(Math.hypot(w*aa+x*d+y*c-z*bb,w*bb-x*c+y*d+z*aa,w*c+x*bb-y*aa+z*d),Math.abs(w*d-x*aa-y*bb-z*c))*360/Math.PI;}
function compare(a,b){assert.notEqual(a.id,b.id);assert.deepEqual(a.definition,b.definition);assert.deepEqual(a.plan,b.plan);const ticks=new Map(a.samples.map(s=>[s.tick,s]));let matched=0,position=0,rotation=0;for(const s of b.samples){const p=ticks.get(s.tick);if(!p)continue;matched++;for(const body of s.bodies){const prior=p.bodies.find(x=>x.id===body.id);assert(prior);position=Math.max(position,distance(body.position,prior.position));rotation=Math.max(rotation,angle(body.rotation,prior.rotation));}}return{coverage:matched/Math.max(a.samples.length,b.samples.length),position,rotation};}
function close(a,b,t=1e-8){assert(Number.isFinite(a)&&Number.isFinite(b)&&Math.abs(a-b)<=t,`${a} differs from ${b}`);}
function ranking(bank,spec){return bank.filter(b=>spec.targets.every(t=>b.episode.run.definition.objects.some(o=>o.sourceId===t.sourceId)&&b.episode.run.plan.intent.targets.some(o=>o.sourceId===t.sourceId))).map(b=>{
 const old=b.episode.run.definition,targets=b.episode.run.plan.intent.targets;let sum=0,count=0;
 for(const t of spec.targets){const a=old.objects.find(o=>o.sourceId===t.sourceId),v=spec.source.objects.find(o=>o.sourceId===t.sourceId),goal=targets.find(x=>x.sourceId===t.sourceId);assert(a&&v&&goal);sum+=Math.abs(a.massKg-v.massKg)/4+Math.abs(a.friction-v.friction)/2;count+=2;for(let k=0;k<3;k++){sum+=Math.abs(a.size[k]-v.size[k]);count++;}sum+=Math.abs(distance(a.position,goal.positionM)-distance(v.position,t.positionM))/10;count++;}
 const oldRoles=new Set(old.objects.map(o=>o.sourceId));let unmatched=0;for(const o of spec.source.objects)if(!oldRoles.delete(o.sourceId))unmatched++;
 return{id:b.episode.id,distance:sum/count+.25*(unmatched+oldRoles.size),preferred:b.preferred};
}).sort((a,b)=>a.distance-b.distance||a.id.localeCompare(b.id));}
function inspectCase(x,bank,study){
 assert.equal(x.format,'memory-study-case-075-v1');const r=x.row,all=new Map();
 for(const item of x.originals){const run=unpack(item.run);assert.equal(run.id,item.attempt.id);assert.equal(run.goalId,x.studyId);assert(!all.has(run.id));all.set(run.id,{run,item});}
 const out={id:r.id,split:r.split,methods:{}};
 for(const method of methods){
  const a=r.results[method];assert(a);let charged=0,observed=0;const verified=new Map(),forecastFamilies=[];let rebuilt;
  let preferred=families[0];if(a.retrieval){assert(['memory','cached'].includes(method));const rank=ranking(bank,r.spec);assert.deepEqual(a.retrieval.ranked.map(o=>({id:o.id,distance:o.distance,preferred:o.preferred})),rank);assert.equal(a.retrieval.selected.id,rank[0].id);preferred=rank[0].preferred;}
  const order=['memory','cached'].includes(method)?[preferred,...families.filter(f=>f!==preferred)]:families;
  if(a.candidateOrder)assert.deepEqual(a.candidateOrder,order);
  if(a.retrievalMissing){assert(['memory','cached'].includes(method));assert.equal(ranking(bank,r.spec).length,0);assert(a.fallback);}
  if(a.certificate){
   assert.equal(method,'cached');assert(study);const c=a.certificate;assert.equal(c.studyId,x.studyId);assert.equal(c.version,'owned-at-rest-reconstruction-075-v1');
   const proof=x.certificateProofs.find(p=>p.certificate.originAttemptId===c.originAttemptId);assert(proof);assert.deepEqual(proof.certificate,c);rebuilt=unpack(proof.run);assert.equal(rebuilt.id,c.originAttemptId);assert.equal(rebuilt.goalId,x.studyId);assert(metrics(rebuilt).passed);
   const origin=study.rows.flatMap(r=>r.results.cached?.attempts??[]).find(t=>t.id===c.originAttemptId);assert(origin);assert.equal(origin.stage,'rebuild');assert.equal(origin.family,'original-memory');assert.equal(origin.status,'completed');assert(origin.metrics.passed);assert.equal(origin.sha256,c.runSha256);assert.equal(origin.chargedTicks,rebuilt.final.tick);assert.equal(origin.observedTicks,rebuilt.final.tick);
   assert(Date.parse(c.createdAt)>=Date.parse(rebuilt.createdAt));assert(Date.parse(rebuilt.createdAt)>=Date.parse(origin.plannedAt));
   const original=bank.find(b=>b.episode.id===a.retrieval.selected.id);assert(original);assert.equal(c.nativeIdentity,original.episode.nativeIdentity);const cmp=compare(original.episode.run,rebuilt);assert(cmp.coverage>=.95&&cmp.position<=.00001&&cmp.rotation<=.001);close(cmp.position,c.comparison.maxPositionResidualM);close(cmp.coverage,c.comparison.sampleCoverage,0);
   if(a.cache.status==='hit'){assert.deepEqual(a.cache.certificate,c);assert(Date.parse(a.cache.checkedAt)>=Date.parse(c.createdAt));}else assert(a.attempts.some(t=>t.id===c.originAttemptId),'Missing charged certificate creation');
  }
  for(const attempt of a.attempts){
   if(!attempt.sha256){assert.equal(attempt.status,'not-started');assert.equal(attempt.chargedTicks,0);continue;}
   const original=all.get(attempt.id);assert(original);assert.equal(original.item.method,method);assert.deepEqual(original.item.attempt,attempt);const run=original.run;
   assert(Date.parse(run.createdAt)>=Date.parse(attempt.plannedAt));assert(charged+14400+(attempt.stage==='actual'?0:14400)<=57600);
   const ticks=run.final?.tick??run.samples.at(-1)?.tick??0;assert.equal(attempt.observedTicks,ticks);assert.equal(attempt.chargedTicks,attempt.unobservedTailCharged?14400:ticks);charged+=attempt.chargedTicks;observed+=ticks;
   if(run.final&&ticks>0){const m=metrics(run);assert.equal(m.passed,attempt.metrics.passed);close(m.nativeTicks,attempt.metrics.nativeTicks,0);close(m.effortNs,attempt.metrics.effortNs,1e-5);verified.set(run.id,m);}
   if(attempt.stage==='rebuild'){
    assert(['memory','cached'].includes(method));const memory=bank.find(b=>b.episode.id===a.retrieval.selected.id);assert(memory);assert.deepEqual(run.definition,memory.episode.run.definition);assert.deepEqual(run.plan,memory.episode.run.plan);
    if(verified.get(run.id)?.passed){const c=compare(memory.episode.run,run);assert(c.coverage>=.95&&c.position<=.00001&&c.rotation<=.001);close(c.coverage,a.reconstruction.sampleCoverage,0);close(c.position,a.reconstruction.maxPositionResidualM);rebuilt=run;}
   }else{
    assert(['forecast','actual'].includes(attempt.stage));assert.deepEqual(JSON.parse(run.definition.worldFrame.originalScene.sourceText),r.spec.source);
    const expectedTargets=attempt.family===families[1]?[...r.spec.targets].reverse():r.spec.targets;
    assert.deepEqual(run.plan.intent.targets,expectedTargets);assert.equal(run.plan.intent.forceLimitN,attempt.family===families[2]?Math.max(2,r.spec.forceLimitN*.5):r.spec.forceLimitN);assert.equal(run.plan.intent.torqueLimitNm,r.spec.torqueLimitNm);
    if(['memory','cached'].includes(method)&&a.retrieval)assert(rebuilt,'A new-scene trial needs a preceding verified original rebuild');
    if(attempt.stage==='forecast'&&!forecastFamilies.includes(attempt.family))forecastFamilies.push(attempt.family);
   }
  }
  close(charged,a.chargedTicks,0);close(observed,a.observedTicks,0);assert(charged<=57600);
  if(r.split==='unsupported'){assert.equal(a.status,'refused');assert.equal(a.attempts.length,0);assert.equal(a.success,false);}
  const permitted=order.filter(id=>a.candidates?.find(c=>c.id===id)?.plan.passed&&a.candidates.find(c=>c.id===id).status!=='duplicate');assert.deepEqual(forecastFamilies,permitted.slice(0,forecastFamilies.length));
  const eligible=a.attempts.filter(t=>t.stage==='forecast'&&verified.get(t.id)?.passed).sort((u,v)=>verified.get(u.id).nativeTicks-verified.get(v.id).nativeTicks||verified.get(u.id).effortNs-verified.get(v.id).effortNs||families.indexOf(u.family)-families.indexOf(v.family));
  if(method!=='search')assert(eligible.length<=1,'First-success control continued after success');
  if(a.selected){assert(eligible.length);assert.equal(a.selected,eligible[0].family);assert.equal(a.forecastRunId,eligible[0].id);}
  let success=false;if(a.actualRunId){const actual=all.get(a.actualRunId).run,forecast=all.get(a.forecastRunId).run;const c=compare(forecast,actual);success=verified.get(actual.id)?.passed&&c.coverage>=.95&&c.position<=.01&&c.rotation<=.5;close(c.position,a.comparison.maxPositionResidualM);close(c.coverage,a.comparison.sampleCoverage,0);}
  assert.equal(a.success,Boolean(success));out.methods[method]={success:Boolean(success),chargedTicks:charged,observedTicks:observed,runs:a.attempts.length,utility:(success?1:0)-.2*charged/57600,wallMillis:a.activeWallMillis};
 }
 assert.equal(all.size,Object.values(r.results).reduce((n,a)=>n+a.attempts.filter(t=>t.sha256).length,0));return out;
}
function aggregate(rows,bank){
 const supported=rows.filter(r=>r.split==='unseen'),methodsOut={};for(const method of methods){methodsOut[method]={successes:0,ticks:0,observedTicks:0,runs:0,wallMillis:0,utility:0};for(const r of supported){const a=r.methods[method],m=methodsOut[method];m.successes+=Number(a.success);m.ticks+=a.chargedTicks;m.observedTicks+=a.observedTicks;m.runs+=a.runs;m.wallMillis+=a.wallMillis;m.utility+=a.utility;}methodsOut[method].meanTicks=methodsOut[method].ticks/supported.length;methodsOut[method].meanUtility=methodsOut[method].utility/supported.length;}
 const paired={};let qualified=supported.length===12;for(const other of ['search','simple']){const m=methodsOut.cached,o=methodsOut[other];paired[other]={saved:supported.filter(r=>r.methods.cached.success&&!r.methods[other].success).length,harmed:supported.filter(r=>!r.methods.cached.success&&r.methods[other].success).length,meanTickDelta:m.meanTicks-o.meanTicks,meanUtilityDelta:m.meanUtility-o.meanUtility};qualified&&=m.successes>=o.successes&&m.ticks<=.9*o.ticks&&m.utility>o.utility;}
 let construction=0;for(const b of bank){const e=b.episode;construction+=e.run.final.tick;if(e.lineage){construction+=e.lineage.costs.totalTicks;construction+=e.lineage.rebuildAttempts.reduce((n,a)=>n+(a.comparison?.ticks??14400),0);}}
 const cold=methodsOut.memory,cached=methodsOut.cached;const cacheQualified=supported.length===12&&cached.successes>=cold.successes&&cached.ticks<=.9*cold.ticks&&cached.utility>cold.utility;
 return{cacheQualified,supported:supported.length,unsupported:rows.length-supported.length,methods:methodsOut,paired,qualified,bankConstructionTicksLowerBound:construction,memoryAmortizedTicksPerTaskLowerBound:(methodsOut.cached.ticks+construction)/supported.length,scope:'Independent native trace arithmetic, frozen new-scene components/targets/authority, complete attempt costs, retrieval ordering and paired outcomes. Same simulator, not an independent physics solver or physical-world test.'};
}
module.exports={inspectCase,aggregate};
if(require.main===module){try{
 const folder=path.resolve(process.argv[2]),bankFolder=path.resolve(process.argv[3]),study=JSON.parse(fs.readFileSync(path.join(folder,'study.json'),'utf8'));
 const bank=study.bank.map(b=>JSON.parse(fs.readFileSync(path.join(bankFolder,b.id+'.json'),'utf8')).data);
 const rows=study.rows.map(r=>inspectCase(JSON.parse(fs.readFileSync(path.join(folder,r.id+'-originals.json'),'utf8')),bank,study));const summary=aggregate(rows,bank);
 assert.equal(summary.qualified,study.evaluation.qualified);assert.equal(summary.cacheQualified,study.evaluation.cacheQualified);for(const method of methods)for(const key of ['successes','ticks','observedTicks','runs','wallMillis','utility'])close(summary.methods[method][key],study.evaluation.methods[method][key],1e-7);
 close(summary.bankConstructionTicksLowerBound,study.bankConstruction.attributableTicks,0);console.log(JSON.stringify({passed:true,rows,summary},null,2));
}catch(e){console.error(e);process.exitCode=1;}}
