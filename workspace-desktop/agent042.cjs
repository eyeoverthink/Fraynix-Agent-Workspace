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
 return{action};
}
module.exports={createAgent042,asksAgent042};
