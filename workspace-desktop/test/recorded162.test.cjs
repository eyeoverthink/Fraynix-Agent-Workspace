'use strict';
const test=require('node:test'),assert=require('node:assert/strict'),{createWorkflows,workflowSummary}=require('../workflows.cjs');
test('recorded object start and reuse keep explicit bindings without acquiring a dataset',async()=>{
 const calls=[],w=createWorkflows({api:async(route,args)=>{calls.push({route,args});return{id:'owned',status:'running'};},emit:()=>{},refresh:async()=>{},getDataset:()=>{throw Error('Unexpected dataset binding');},getScene:()=>{throw Error('Unexpected ramp binding');}});
 const source={profile162:'recorded-small-object-162-v1',trialId:'fresh-trial',ordinal:25,worldId:'small-world',expectedRevision:2,expectedHash:'original'};
 await w.execute('workflow.start',source);assert.deepEqual(calls[0].args,source);await w.execute('workflow.reuse',{name:'retained',version:1,trialId:'different-trial',ordinal:1});assert.equal(calls[1].args.trialId,'different-trial');assert.equal(calls[1].args.datasetId,undefined);
 assert.match(workflowSummary({profile162:source.profile162,status:'completed',reason:'Verified'}),/simulated proxy only/);
});
test('recorded workflow audits both actual artifact surfaces and returns observed cells',async()=>{
 const opened=[],checks=[],read={cells162:['observed source'],scope162:'observed scope',visible162:true};
 const w=createWorkflows({api:async(route,args)=>route.endsWith('/test')?(checks.push(args),{passed:true}):{id:args.id,version:1},canvas:{test:async()=>[{passed:true}],isolated:async code=>code.includes('cells162')?read:code.includes('return true')?true:{actual:'DOM fixture'}},openArtifact:async a=>opened.push(a.id),emit:()=>{}});
 const out=await w.audit({kind:'workflow.recorded-small162',report:{id:'parent',version:1},nativeReport:{id:'native',version:1},samples:3});
 assert.deepEqual(opened,['native','parent']);assert.deepEqual(checks.map(c=>c.id),['native','parent']);assert.deepEqual(out.cells162,['observed source']);assert.equal(out.passed,undefined);assert(out.nativeMotion&&out.motion);
});
