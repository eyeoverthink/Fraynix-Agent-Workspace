'use strict';
const fs=require('node:fs'),path=require('node:path'),assert=require('node:assert/strict'),crypto=require('node:crypto');
const {inspectProcedure,inspectCommit,inspectCost}=require('./Inspect-Procedures098.cjs');
const {canonical}=require('./Inspect-Spatial040.cjs');
const hash=x=>crypto.createHash('sha256').update(canonical(x)).digest('hex');
const repo=path.resolve(__dirname,'..'),out=path.resolve(repo,process.argv[2]||'build/ui099-chat-attempt2'),read=p=>JSON.parse(fs.readFileSync(p,'utf8'));
const record=read(path.join(out,'result.json'));assert(record.passed);assert.equal(record.tasks.length,4);
const first=record.tasks.find(t=>t.kind==='world'),original=read(path.join(out,'world-originals.json')),root=path.join(out,'project/world-procedures-079');
const world=inspectProcedure(original,root);assert(world.success);inspectCommit(original,root,first.goal);inspectCost(original,root,first.goal);
const refusals=[];
for(const [name,change]of [['changed target',x=>x.job.spec.targets[0].positionM[0]+=.3],['changed charged work',x=>x.job.arm.chargedTicks++],['missing actual original',x=>x.originals.pop()]]){const altered=structuredClone(original);change(altered);assert.throws(()=>inspectProcedure(altered,root));refusals.push(name);}
const forged=structuredClone(first.goal);forged.worldCommit.hash='0'.repeat(64);assert.throws(()=>inspectCommit(original,root,forged));refusals.push('forged world commit');
const ledger=read(path.join(out,'project/agency-043/tasks.json'));assert.equal(hash(ledger.data),ledger.sha256);
const tasks=record.tasks.map(row=>{const task=ledger.data.tasks.find(t=>t.id===row.task.id);assert(task);assert.equal(task.status,'completed');assert.equal(task.outcome,'verified');assert.equal(task.goal,record.prompts[row.kind]);assert.equal(row.goal.id,row.task.completion044.javaGoal.id);assert.equal(row.artifact.id,row.goal.report.id);assert(row.artifact.test.passed);assert(row.callerMillis>0);return{kind:row.kind,taskId:task.id,goalId:row.goal.id,callerMillis:row.callerMillis,routerCalls:row.task.timing052.modelCalls,routerMillis:row.task.timing052.modelMillis,failedRouterCalls:row.task.timing052.failedModelCalls,generationCalls:row.generation?.length||0,modelDigest:row.task.modelDigest||row.generation?.[0].model.digest,savedSkill:row.goal.savedSkill?.name||null,reusedSkill:row.goal.skill||null};});
assert(tasks.every(t=>t.routerCalls+t.generationCalls>0));assert.equal(new Set(tasks.map(t=>t.modelDigest)).size,1);
const result={passed:true,source:record.source,at:new Date().toISOString(),world,refusals,tasks,scope:'Fresh local-model and native originals. Full inherited procedure source, forecast, actual motion, metrics and single-commit inspection retained. Four altered evidence cases refused. Data arithmetic and actual creation controls checked in the app harness. Caller times include inference and checks; no performance comparison or generalization claim.'};
fs.writeFileSync(path.join(repo,'build/analysis099.json'),JSON.stringify(result,null,2)+'\n');console.log(JSON.stringify(result));
