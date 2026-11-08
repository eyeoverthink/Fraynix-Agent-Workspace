'use strict';
// Negative checks use copied metadata. Native records are never rewritten here.
const fs=require('node:fs'),path=require('node:path'),assert=require('node:assert/strict'),{verifyRow}=require('./Inspect-Mixed095.cjs'),{account,paired,aggregate}=require('./Costs095.cjs');
const repo=path.resolve(__dirname,'..'),out=path.join(repo,'build/ui095-study'),r=JSON.parse(fs.readFileSync(path.join(out,'result.json'),'utf8')),copy=x=>JSON.parse(JSON.stringify(x)),checks=[];
function refuse(name,fn){assert.throws(fn,undefined,name);checks.push(name);}
const sample=r.rows.find(x=>x.workload.skill&&x.restarts.length&&x.mode==='memory');assert(sample);
for(const [name,mutate]of [
 ['different external clock owner',x=>x.clockOwner='other'],
 ['missing saving phase evidence',x=>x.savedProcedure.sourceGoalId='other'],
 ['changed saved skill targets',x=>x.savedProcedure.defaults.targets[0].positionM[0]+=1],
 ['changed saved skill code',x=>x.savedProcedure.code='other'],
 ['changed cold world hash',x=>x.persistence.world.expectedHash='other'],
 ['unavailable recovered clock claimed eligible',x=>x.goal.taskCost080.eligible=true],
 ['fabricated recovered total',x=>x.goal.taskCost080.totalMillis=1],
 ['mismatched declared source',x=>x.sourceSpecSha256='other'],
 ['changed original world commit',x=>x.goal.worldCommit.hash='other'],
 ['wrong process parent',x=>x.restarts[0].process.parentPid+=1],
 ['changed persisted checkpoint digest',x=>x.restarts[0].persisted[0].sha256='other']
]){const x=copy(sample);mutate(x);refuse(name,()=>verifyRow(x,r,out));}
for(const [name,mutate]of [
 ['overlapping phase',x=>x.phases[2].start=x.phases[1].start],
 ['omitted shutdown',x=>x.phases.splice(5,1)],
 ['restart counted outside task',x=>x.restarts[0].end=x.interval.end+1],
 ['false caller duration',x=>x.callerMillis+=100]
]){const x=copy(sample);mutate(x);refuse(name,()=>account(x));}
const first=r.summary.pairs[0],rows=r.rows.filter(x=>x.block===first.block&&x.condition===first.condition),simple=rows.find(x=>x.mode==='simple'),memory=rows.find(x=>x.mode==='memory');
for(const [name,mutate]of [['different workload',x=>x.workload.skill=!x.workload.skill],['different source',x=>x.sourceSpecSha256='other'],['different CPU profile',x=>x.profile.cpus=4],['changed order',x=>x.order.reverse()],['different paired clock owner',x=>x.clockOwner='other']]){const x=copy(memory);mutate(x);refuse(name,()=>paired(simple,x));}
refuse('incomplete opposite-order replication',()=>aggregate(r.rows.filter(x=>x.block===0),r.preparationMillis));
refuse('duplicate evaluated arm',()=>aggregate([...r.rows,r.rows[0]],r.preparationMillis));
assert.deepEqual(aggregate(r.rows,r.preparationMillis),r.summary);
const result={passed:true,refusals:checks.length,checks,source:require('node:child_process').execFileSync('git',['rev-parse','HEAD'],{cwd:repo,encoding:'utf8'}).trim(),scope:'Copied metadata mutations only. Original records remain untouched; independent verification is recomputed before changed metadata can pass.'};fs.writeFileSync(path.join(repo,'build/mutations095.json'),JSON.stringify(result,null,2)+'\n');console.log(JSON.stringify(result));
