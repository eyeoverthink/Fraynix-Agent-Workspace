'use strict';
const fs=require('node:fs'),path=require('node:path'),assert=require('node:assert/strict');
const {inspectProcedure,inspectCommit,inspectCost}=require('./Inspect-Procedures085.cjs');
const repo=path.resolve(__dirname,'..'),folder=path.join(repo,'build/ui086-study'),r=JSON.parse(fs.readFileSync(path.join(folder,'result.json'),'utf8'));
assert(r.passed);assert.equal(r.rows.length,4);assert.equal(r.decision.fitted,false);assert.equal(r.refusal.receipt.status,'verification-refused');
const groups=new Map();let historical=0,maxTask=0;
for(const p of r.model.pairs){if(!groups.has(p.taskSha256))groups.set(p.taskSha256,[]);groups.get(p.taskSha256).push(p);historical+=p.simpleMillis+p.memoryMillis;maxTask=Math.max(maxTask,p.simpleMillis,p.memoryMillis);}
const mean=xs=>xs.reduce((a,b)=>a+b,0)/xs.length,range=xs=>Math.max(...xs)-Math.min(...xs);
const minimum=Math.min(...[...groups.values()].map(g=>mean(g.map(p=>p.simpleMillis-p.memoryMillis))));
const spread=Math.max(500,...[...groups.values()].map(g=>range(g.map(p=>p.simpleMillis))+range(g.map(p=>p.memoryMillis))));
const a=r.decision.admission,b=a.budget,expected={historicalComparisonMillis:historical,minimumGroupSavingMillis:minimum,variabilityAllowanceMillis:spread,lowerSavingMillis:minimum-spread,netPerTaskMillis:minimum-spread-b.checkBudgetMillis,futureInvestmentMillis:b.fitBudgetMillis+b.additionalRuns*maxTask};
expected.forecastBenefitMillis=b.horizon*expected.netPerTaskMillis;expected.requiredBenefitMillis=expected.futureInvestmentMillis*1.25;expected.forecastNetMillis=expected.forecastBenefitMillis-expected.futureInvestmentMillis;
for(const[k,v]of Object.entries(expected))assert(Math.abs(a[k]-v)<1e-7,k);
assert.equal(a.admit,expected.netPerTaskMillis>0&&expected.forecastBenefitMillis>expected.requiredBenefitMillis);
const checked=[],methods={};
for(const row of r.rows){const x=JSON.parse(fs.readFileSync(path.join(folder,row.originalFile),'utf8')),root=path.join(row.project,'world-procedures-079');
 const check=inspectProcedure(x,root);assert(check.success);inspectCommit(x,root,row.goal);inspectCost(x,root,row.goal);checked.push(check);
 assert.equal(row.bankAfter.count,row.bankBefore.count+1);assert.equal(row.persistence.hash,row.observed.expectedHash);assert.deepEqual(row.persistence.skill,row.goal.savedSkill);assert.equal(row.beforeOS.affinity,row.afterOS.affinity);assert.equal(row.before.profile.cpus,4);assert.equal(row.before.configuredMaxHeapBytes,768*1024*1024);
 const m=methods[row.method]||={goals:0,successes:0,completeMillis:0,callerMillis:0,ticks:0,modelCheckMillis:0,modelBytes:0};m.goals++;m.successes+=row.status==='completed';m.completeMillis+=row.goal.taskCost080.totalMillis;m.callerMillis+=row.callerMillis;m.ticks+=row.job.arm.chargedTicks;m.modelCheckMillis+=row.scan.loadMillis;m.modelBytes+=row.scan.verifiedBytes;
}
for(let i=0;i<2;i++){const rows=r.rows.filter(x=>x.caseIndex===i);assert.equal(rows.length,2);assert.deepEqual(rows[0].goal.taskCost080.task,rows[1].goal.taskCost080.task);}
methods.gated.setupCallerMillis=r.gatedCallerMillis;methods.baseline.setupCallerMillis=r.fitMillis;
for(const m of Object.values(methods))m.setupAndTasksMillis=m.setupCallerMillis+m.callerMillis;
const result={passed:true,checked,admission:expected,preflight:r.previews.map(p=>({millis:p.preflightMillis,callerMillis:p.callerMillis,summaryBytes:p.summaryBytes,originalTraceBytes:p.originalTraceBytes})),methods,savedFitMillis:r.fitMillis-r.gatedCallerMillis,netCallerSavingMillis:methods.baseline.setupAndTasksMillis-methods.gated.setupAndTasksMillis,diagnosticPreviewCallerMillis:r.previews.reduce((s,p)=>s+p.callerMillis,0),netIncludingRepeatedPreviewMillis:methods.baseline.setupAndTasksMillis-methods.gated.setupAndTasksMillis-r.previews.reduce((s,p)=>s+p.callerMillis,0),receiptRestart:true,savedSkillsReopened:4,scope:'Four new actual native goals on two repeated085 scenes; same original085 comparison evidence, unchanged compiled execution classes. Descriptive single-machine costs; setup is newly measured fitting only, historical comparison collection is sunk and not claimed as avoided. Test-control unconditional fitting is unavailable in the normal runtime. No new model inference or physical observation claim.'};
fs.writeFileSync(path.join(repo,'build/analysis086.json'),JSON.stringify(result,null,2)+'\n');console.log(JSON.stringify(result,null,2));
