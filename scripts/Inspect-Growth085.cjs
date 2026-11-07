'use strict';
const fs=require('node:fs'),path=require('node:path'),assert=require('node:assert/strict'),crypto=require('node:crypto');
const {inspectProcedure,inspectCommit,inspectCost,estimate084}=require('./Inspect-Procedures085.cjs');
const {canonical}=require('./Inspect-Spatial040.cjs');
const read=p=>JSON.parse(fs.readFileSync(p,'utf8')),sha=b=>crypto.createHash('sha256').update(b).digest('hex'),sum=(xs,f)=>xs.reduce((n,x)=>n+f(x),0);
function inspect(folder){
 const r=read(path.join(folder,'result.json'));assert(r.passed);assert.equal(r.training.length,1);assert.equal(r.calibration.length,6);assert.equal(r.rows.length,18);
 const fitRows=r.calibration.flatMap(x=>Object.values(x.results)),all=[...r.training,...fitRows,...r.rows],checked=[],boots=new Set();
 for(const row of all){
  const root=path.join(row.project,'world-procedures-079'),original=read(path.join(folder,row.originalFile));
  checked.push(inspectProcedure(original,root));inspectCommit(original,root,row.goal);inspectCost(original,root,row.goal);
  assert.equal(row.status,'completed');assert(row.retain);assert.equal(row.goal.taskCost080.workload.retain,true);assert.equal(row.goal.taskCost080.workload.skill,!!row.skillName);
  assert.equal(row.before.profile.cpus,4);assert.equal(row.before.configuredMaxHeapBytes,768*1024*1024);assert.equal(row.beforeOS.affinity,row.afterOS.affinity);assert.equal(BigInt(row.beforeOS.affinity).toString(2).split('1').length-1,4);
  assert(!boots.has(row.ownerBootId));boots.add(row.ownerBootId);assert.equal(row.bankAfter.count,row.bankBefore.count+1);
  const memory=read(path.join(root,row.goalId,'memory.json'));assert.deepEqual(memory,row.retainedReceipt);assert.equal(memory.sha256,sha(canonical(memory.data)));assert.equal(memory.data.id,row.goalId);assert.equal(memory.data.commitHash,row.observed.expectedHash);assert.equal(memory.data.jobSha256,sha(canonical(row.job)));assert.equal(memory.data.code,row.job.code);
  if(row.policy!=='simple'){assert.equal(row.job.decision.candidate.id,r.receiptId);assert.equal(row.job.decision.requestedReceipt085,r.receiptId);assert.equal(row.job.decision.candidate.warm,false);}
  if(row.skillName){assert.equal(row.savedProcedure.sourceGoalId,row.goalId);assert.equal(row.savedProcedure.id,row.goal.savedSkill.id);assert.deepEqual(row.savedProcedure.defaults.targets,row.job.args.targets);if(row.policy!=='simple')assert.equal(row.savedProcedure.defaults.procedureReceipt085,r.receiptId);}
  console.error('VERIFIED',row.id,row.policy,'bank',row.bankBefore.count,'->',row.bankAfter.count);
 }
 let preparedPrevious=r.training[0].bankAfter.sha256;assert.equal(r.training[0].bankAfter.count,1);
 for(let i=0;i<fitRows.length;i++){const row=fitRows[i];assert.equal(row.bankBefore.sha256,preparedPrevious);assert.equal(row.bankBefore.count,i+1);assert.equal(row.bankAfter.count,i+2);preparedPrevious=row.bankAfter.sha256;}
 const preparationBank=fs.readFileSync(path.join(r.project,'world-procedures-079/bank.json'));assert.equal(sha(preparationBank),preparedPrevious);assert.equal(preparedPrevious,r.frozenBankSha);const preparedEntries=JSON.parse(preparationBank).data.entries;assert.equal(preparedEntries.length,13);for(const row of [...r.training,...fitRows])assert(preparedEntries.some(x=>x.id===row.goalId));
 const calibrationGoals=new Set(fitRows.map(x=>x.goalId));assert.equal(calibrationGoals.size,12);const repeats=new Map();
 for(const p of r.model.pairs){const row=fitRows.find(x=>x.goalId===p.memoryId);assert(row);assert.equal(p.memoryBankEntries,row.bankBefore.count);repeats.set(p.taskSha256,(repeats.get(p.taskSha256)||0)+1);assert.equal(p.receiptId,r.receiptId);assert.deepEqual(p.workload,{retain:true,skill:true});}
 assert.deepEqual([...repeats.values()],[3,3]);assert.equal(r.model.version,'growing-bank-complete-task-cost-085-v1');assert(r.fitMillis>=r.model.setup085.fitMillis);
 const totals={},sequences={};
 for(const policy of ['simple','memory','calibrated']){
  const rows=r.rows.filter(x=>x.policy===policy);assert.equal(rows.length,6);let previous=r.frozenBankSha;
  for(let i=0;i<rows.length;i++){const x=rows[i];assert.equal(x.caseIndex,i);assert(!calibrationGoals.has(x.goalId));assert.equal(x.bankBefore.sha256,previous);previous=x.bankAfter.sha256;assert.equal(x.bankBefore.count,13+i);assert.equal(x.bankAfter.count,14+i);assert.equal(x.persistence.hash,x.observed.expectedHash);assert.deepEqual(x.persistence.skill,x.goal.savedSkill);assert.equal(sha(fs.readFileSync(path.join(x.project,'world-procedures-079/cost-models080',r.model.id+'.json'))),r.modelSha);}
  const final=fs.readFileSync(path.join(r.evaluationProjects[policy],'world-procedures-079/bank.json'));assert.equal(sha(final),previous);assert.equal(sha(final),r.finalBanks[policy].sha256);assert.equal(r.finalBanks[policy].entries.length,19);assert(r.finalBanks[policy].entries.some(x=>x.id===r.receiptId));for(const x of rows)assert(r.finalBanks[policy].entries.some(b=>b.id===x.goalId));
  totals[policy]={completed:rows.length,completeMillis:sum(rows,x=>x.goal.taskCost080.totalMillis),callerMillis:sum(rows,x=>x.callerMillis),ticks:sum(rows,x=>x.job.arm.chargedTicks),stages:sum(rows,x=>x.job.arm.attempts.length),checkMillis:sum(rows,x=>x.scan.loadMillis),verifiedBytes:sum(rows,x=>x.scan.verifiedBytes),retained:rows.length,bankStart:13,bankEnd:19};
  sequences[policy]=rows.map(x=>({id:x.id,goalId:x.goalId,bankBefore:x.bankBefore.count,bankAfter:x.bankAfter.count,completeMillis:x.goal.taskCost080.totalMillis,callerMillis:x.callerMillis,startupMillis:r.boots.find(b=>b.label===x.bootLabel).startupMillis,calibrationCheckingMillis:x.scan.loadMillis}));
 }
 const cases=[];for(let i=0;i<6;i++){
  const xs=Object.fromEntries(r.rows.filter(x=>x.caseIndex===i).map(x=>[x.policy,x]));const task=read(path.join(xs.simple.project,'world-procedures-079',xs.simple.goalId,'cost080.json')).data.task;
  for(const x of Object.values(xs))assert.deepEqual(read(path.join(x.project,'world-procedures-079',x.goalId,'cost080.json')).data.task,task);
  const e=xs.calibrated.job.decision.cost080,old=estimate084({...r.model,version:'repeated-complete-task-cost-084-v1'},xs.calibrated.job.decision.candidate,xs.calibrated.job.spec,xs.calibrated.job.args.workload080),saving=xs.simple.goal.taskCost080.totalMillis-xs.memory.goal.taskCost080.totalMillis;
  assert(e.available);assert(e.bankGrowthMarginMillis>0);cases.push({id:xs.simple.id,bankEntries:e.bankEntries,estimate:e,baseline084:old,observedSavingMillis:saving,insideEmpiricalInterval:saving>=e.lowerSavingMillis&&saving<=e.upperSavingMillis,baselineInside:saving>=old.lowerSavingMillis&&saving<=old.upperSavingMillis,selectedMemory:xs.calibrated.job.decision.useMemory,calibratedSavingMillis:xs.simple.goal.taskCost080.totalMillis-xs.calibrated.goal.taskCost080.totalMillis});
 }
 assert(r.rows.some(x=>x.viaChatToolBridge));assert.equal(r.uiChoice.receipt,r.receiptId);
 const setup={learningMillis:sum(r.training,x=>x.goal.taskCost080.totalMillis),calibrationTaskMillis:sum(fitRows,x=>x.goal.taskCost080.totalMillis),fitCallerMillis:r.fitMillis,fitInternalMillis:r.model.setup085.fitMillis,startupMillis:sum(r.boots,x=>x.startupMillis),reopenMillis:sum(r.rows,x=>x.persistence.millis)};
 assert(Math.abs(setup.calibrationTaskMillis-r.model.setup085.calibrationTaskMillis)<.001);
 const investment=setup.calibrationTaskMillis+setup.fitCallerMillis,executionSaving=totals.simple.completeMillis-totals.calibrated.completeMillis,net=executionSaving-investment;
 const payback={horizon:6,investmentMillis:investment,executionSavingMillis:executionSaving,netAfterCalibrationMillis:net,repaid:net>=0,netIncludingLearningMillis:net-setup.learningMillis,scope:'Measured six-task evaluation totals plus actual calibration tasks and fitting caller time. Learning/startup/reopen shown separately; excludes any assigned value of useful calibration tasks. No unseen longer-horizon repayment claim.'};
 const startupFor=row=>{const matches=r.boots.filter(b=>b.label===row.bootLabel);assert.equal(matches.length,1);assert(Number.isFinite(matches[0].startupMillis)&&matches[0].startupMillis>=0);return matches[0].startupMillis;};
 const fitBoots=r.boots.filter(b=>/^\d+-fit$/.test(b.label));assert.equal(fitBoots.length,1);
 const learningStartupMillis=sum(r.training,startupFor),calibrationStartupMillis=sum(fitRows,startupFor)+fitBoots[0].startupMillis;
 const deployment={learningWithStartupMillis:setup.learningMillis+learningStartupMillis,calibrationWithStartupMillis:investment+calibrationStartupMillis,learningStartupMillis,calibrationStartupMillis,controls:{},scope:'Accounting over actual fresh-owner launches and measured tasks. Charges seed learning to memory/calibrated, calibration to calibrated. Validation-only reopen/export/copy time excluded and separately reported. Prepared matched runs are not additional empty-project executions.'};
 for(const policy of ['simple','memory','calibrated']){const startupMillis=sum(r.rows.filter(x=>x.policy===policy),startupFor),setupMillis=(policy==='calibrated'?deployment.calibrationWithStartupMillis:0)+(policy!=='simple'?deployment.learningWithStartupMillis:0);deployment.controls[policy]={startupMillis,setupMillis,totalMillis:setupMillis+startupMillis+totals[policy].completeMillis};}
 deployment.netSavingMillis=deployment.controls.simple.totalMillis-deployment.controls.calibrated.totalMillis;deployment.repaid=deployment.netSavingMillis>=0;
 return{passed:true,checked,totals,sequences,cases,setup,payback,deployment,coverage:r.coverage,studyElapsedMillis:Date.parse(r.finishedAt)-Date.parse(r.startedAt),scope:'One growing-bank sequence per control, six predeclared unseen scenes, real retention and skill saving, cold owner per goal. Descriptive single-machine timings; no probability, causal bank-cost or model-weight-training claim.'};
}
if(require.main===module)try{console.log(JSON.stringify(inspect(path.resolve(process.argv[2]||'build/ui085-study')),null,2));}catch(e){console.error(e);process.exitCode=1;}
module.exports={inspect};
