'use strict';
const{randomUUID}=require('node:crypto');
function asksRuntime040(goal){return /\b(?:linked (?:views|spatial)|shared (?:spatial|runtime)|orthographic|spatial runtime)\b/i.test(goal)&&!(/\b(?:build|create)\b.*\b(?:website|html)\b/i.test(goal));}
function createRuntime040({api,execute,openArtifact,emit}){
 async function release(job){if(job?.runtimeHash)await api('/api/runtime040/release',{snapshotHash:job.runtimeHash}).catch(()=>{});}
 async function action(tool,args={},job={}){
  if(tool==='runtime.read'){if(Object.keys(args).length)throw Error('Read uses the actual user-selected runtime, no supplied identities or transforms');if(job.runtimeHash)await release(job);const frame=await api('/api/runtime040/read',{});await api('/api/runtime040/pin',{snapshotHash:frame.snapshotHash});job.runtimeHash=frame.snapshotHash;job.runtimeFacts=await api('/api/runtime040/summary',{snapshotHash:frame.snapshotHash});return job.runtimeFacts;}
  if(!job.runtimeHash)throw Error('Read and pin a source snapshot first');
  if(tool==='runtime.project'){if(Object.keys(args).some(k=>k!=='view')||!['perspective','top','front','side'].includes(args.view))throw Error('Choose a registered camera view');const result=await api('/api/runtime040/view',{snapshotHash:job.runtimeHash,camera:{view:args.view,width:800,height:450}});job.runtimeView={view:args.view,polygons:result.polygons.length,centers:result.centers.filter(c=>c.id===job.runtimeFacts.selectedId),snapshotHash:result.snapshotHash};return job.runtimeView;}
  if(tool==='runtime.report'){if(Object.keys(args).length)throw Error('Report uses this task exact pinned source');const{artifact,receipt}=await api('/api/runtime040/report',{snapshotHash:job.runtimeHash});emit('view','preview');await openArtifact(artifact);const tested=await execute('canvas.test',{steps:[{selector:'#runtime-snapshot',expectText:receipt.snapshotHash},{selector:'#runtime-selected',expectText:receipt.selectedId},{selector:'.views svg',expectCount:receipt.views},{selector:'#runtime-entities tr',expectCount:receipt.rows}]});if(!tested.passed)throw Error('Actual linked-view report assertions failed');job.runtimeReport={id:artifact.id,version:artifact.version,passed:true};return job.runtimeReport;}
  throw Error('Unknown shared runtime tool');
 }
 return{action,release};
}
module.exports={createRuntime040,asksRuntime040};
