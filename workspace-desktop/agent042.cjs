'use strict';
const {randomUUID}=require('node:crypto');
function asksAgent042(goal){return /\b(?:heartbeat|witness|continuing (?:java |agent |runtime)|agent continuity|agent identity|background (?:brain|agent|replay)|retained active time)\b/i.test(goal)&&!/\b(?:create|build)\b.*\b(?:website|game|html)\b/i.test(goal);}
function createAgent042({api,execute,openArtifact,emit}){
 async function action(tool,args={},job={}){
  if(Object.keys(args).length)throw Error('Agent evidence tools accept no supplied facts, identity, clocks or success flags');
  if(tool==='agent.read'){job.agent042Facts=await api('/api/agent042/state',{});job.agent042Audit=await api('/api/agent042/audit',{});return job.agent042Facts;}
  if(tool==='agent.report'){const {artifact,receipt}=await api('/api/agent042/report',{});emit('view','preview');await openArtifact(artifact);const check=await execute('canvas.test',{steps:[{selector:'#agent042-hash',expectText:receipt.snapshotSha256},{selector:'#agent042-id',expectText:receipt.agentId},{selector:'#agent042-boot',expectText:receipt.bootId}]});if(!check.passed||!receipt.witness.verified)throw Error('Actual continuing agent report verification failed');job.agent042Report={id:artifact.id,version:artifact.version,passed:true,receipt};return job.agent042Report;}
  throw Error('Unknown continuing agent tool');
 }
 async function agent({goal,model,job,planTools,command,onTurn}){
  const recent=[];for(let turn=1;turn<=5;turn++){
   if(job.cancelled)throw Error('Stopped by you');const allowed=!job.agent042Facts?['agent.read']:!job.agent042Report?['agent.report']:['finish'];onTurn(turn,'Inspecting continuing Java agent evidence');job.requestId=randomUUID();const reply=await api('/api/plan',{model,requestId:job.requestId,...(job.modelDigest?{expectedDigest:job.modelDigest}:{}),allowedTools:allowed,tools:planTools(allowed),prompt:`Use one typed tool for stage ${allowed[0]}. User: ${goal}. Read actual Java identity, clocks, shared neural state, witness audit and source-qualified memory; open its checked saved report; finish. No clock, identity, observations or task success may be supplied by the model. Replay is retained evidence, not fresh physical truth or improved skill. No subjective emotion or LLM weight training established. Actual receipts: ${JSON.stringify(recent).slice(-11000)}`});job.modelDigest=reply.localModel.digest;await api('/api/event',{kind:'agent042_plan',data:{episodeId:job.id,stage:allowed[0],model:reply.localModel,compute:reply.compute,context:reply.selfModelSha256,runtimeContext:reply.continuingAgent,runtimeContextSha256:reply.continuingAgentSha256}});const next=command(JSON.parse(reply.text));if(!allowed.includes(next.tool))throw Error('Tool outside continuing agent evidence stage');if(next.tool==='finish'){const f=job.agent042Facts;return{summary:`Java agent ${f.agentId}; boot ${f.bootId}. Status ${f.status}; ${f.clock.logicalTicks} heartbeat ticks; ${(f.clock.retainedActiveMs/1000).toFixed(1)} seconds retained active time at the dated read. ${f.experienceCount} retained source experiences, ${f.replayedExperiences} replayed. Witness integrity verified; saved report opened and checked. Runtime continuity and sensory plasticity are operational evidence; useful policy improvement still requires separate outcome evaluation.`,agent042Facts:f,agent042Report:job.agent042Report};}recent.push({tool:next.tool,result:await execute(next.tool,next.args)});
  }throw Error('Continuing agent planning budget reached');
 }
 return{action,agent};
}
module.exports={createAgent042,asksAgent042};
