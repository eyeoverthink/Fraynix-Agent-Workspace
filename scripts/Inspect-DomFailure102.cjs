'use strict';
const fs=require('node:fs'),path=require('node:path'),assert=require('node:assert/strict'),crypto=require('node:crypto');
const {taskCosts}=require('./WorkflowCost102.cjs');
const root=path.resolve(__dirname,'..');
const read=p=>JSON.parse(fs.readFileSync(path.join(root,p),'utf8'));
const sha=s=>crypto.createHash('sha256').update(s).digest('hex');
function inspect(r){
 assert.equal(r.outcome,'failed-exhausted');assert.equal(r.tasks.length,1);assert.equal(r.savedProcedureCount,0);
 const t=r.tasks[0],w=t.workflow,[data,created]=t.children,[source,artifact]=t.artifacts;
 assert.equal(t.task.status,'interrupted');assert.equal(w.status,'interrupted');assert.equal(t.task.resumes,2);
 assert.equal(created.draftNumber,3);assert(!w.savedSkill&&!w.pendingSkill101);assert.equal(w.auditHistory.length,0);
 assert.equal(t.end,Date.parse(r.finishedAt));assert.equal(data.status,'completed');
 assert.equal(w.stages[0].goalId,data.id);assert.equal(w.stages[1].goalId,created.id);
 const csv=fs.readFileSync(path.join(root,'docs/datasets/delfys75-013/validation.csv'),'utf8');
 assert.equal(t.source.csv,csv);assert.equal(t.source.sourceSha256,sha(csv));assert.equal(source.contents['original.csv'],csv);
 assert.equal(artifact.contents['inputs/source.csv'],source.contents['normalized.csv']);
 const lines=csv.trim().split(/\r?\n/).map(l=>l.split(',')),headers=lines.shift();
 assert.equal(t.source.mapping.timeColumn,'time_s');assert.equal(t.source.mapping.distanceColumn,'displacement_m');
 const expected=lines.map((l,i)=>[i+2,Number(l[headers.indexOf('time_s')]),Number(l[headers.indexOf('displacement_m')])]);
 const rows=source.contents['normalized.csv'].trim().split(/\r?\n/).slice(1).map(l=>l.split(',').map(Number));
 assert.equal(rows.length,49);assert.deepEqual(rows,expected);
 assert.equal(created.draftHistory.length,3);
 for(const [i,d] of created.draftHistory.entries()){
  assert.equal(d.draft,i+1);assert.equal(d.passed,false);
  const error=d.checks.find(c=>c.error==='Browser JavaScript errors');assert(error&&!error.passed);
  assert(error.errors.some(s=>s.includes(i===0?'SyntaxError':'Cannot set properties of null')));
  for(const name of ['samples','duration','travel','peak','gaps','speeds']){
   const c=d.checks.find(c=>c.selector==='#summary-'+name);assert(c&&!c.passed);assert.equal(c.actual.text,'');
  }
 }
 assert.equal(artifact.test.passed,false);assert.deepEqual(artifact.test.checks,created.draftHistory[2].checks);
 const journal=read('build/summary102-attempt2/project/creation-044/requests.json').data.jobs.filter(j=>j.goalId===created.id);
 assert.deepEqual(t.generation,journal);assert.equal(journal.length,5);
 for(const j of journal)if(j.status==='completed'){
  assert.equal(j.model.digest,r.model.digest);assert.equal(j.compute.options.num_gpu,0);assert.equal(j.compute.options.num_thread,4);
  if(j.effect.generation.draft===3)assert.equal(j.result.files['index.html'],artifact.contents['index.html']);
 }
 const originals=[['summary102-attempt2-failure','result.json'],['summary102-attempt2-budget2','result-resumed.json']];
 assert.equal(t.generationStops102.length,2);
 for(const [folder,file] of originals){
  const old=read('build/'+folder+'/requests.json').data.jobs.find(j=>j.status==='running');assert(old);
  const stop=t.generationStops102.find(s=>s.jobId===old.id),failure=read('build/'+folder+'/'+file);
  assert(stop);assert.equal(stop.stopAt,Date.parse(failure.finishedAt));assert.equal(failure.tasks[0].id,t.id);
  assert.deepEqual(journal.find(j=>j.id===old.id).timings102,old.timings102);
 }
 assert.equal(t.pauseStart,Date.parse(read('build/summary102-attempt2-failure/result.json').finishedAt));
 assert.equal(t.pauseEnd,read('build/summary102-attempt2-budget2/result-resumed.json').tasks[0].pauseEnd);
 assert.equal(t.recoveryPauses102.length,1);
 assert.equal(t.recoveryPauses102[0].start,Date.parse(read('build/summary102-attempt2-budget2/result-resumed.json').finishedAt));
 const cost=taskCosts(t,r.hostSpans);assert.deepEqual(cost,r.cost);assert.equal(cost.failedBehaviorDrafts,3);assert.equal(cost.failedAudits,0);
 return{passed:true,result:{input:'summary49-dom-failure',outcome:'failed-exhausted',rows:49,cost,completeMillis:t.end-t.start,drafts:3,modelGenerationJobs:journal.length,sourceSha256:t.source.sourceSha256},scope:'Second exhausted summary: actual syntax and repeated missing-element failures, three committed drafts, two explicit resumes, five generation jobs, no saved skill. Failure inspection is not workflow success.'};
}
function refusals(r){
 return [['invented success',x=>x.tasks[0].workflow.status='completed'],['invented skill',x=>x.savedProcedureCount=1],['hidden generation',x=>x.tasks[0].generation.pop()],['erased unfinished call',x=>x.tasks[0].generationStops102.pop()],['invented passing draft',x=>x.tasks[0].children[1].draftHistory[2].passed=true],['replaced final code',x=>x.tasks[0].artifacts[1].contents['index.html']='fixed'],['shortened cost',x=>x.tasks[0].end-=1000]].map(([name,mutate])=>{const copy=structuredClone(r);mutate(copy);assert.throws(()=>inspect(copy),name);return{name,refused:true};});
}
if(require.main===module){const r=read('build/summary102-attempt2/result-exhausted.json');console.log(JSON.stringify({...inspect(r),refusals:refusals(r)},null,2));}
module.exports={inspect,refusals};
