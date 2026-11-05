'use strict';
const{randomUUID}=require('node:crypto');
function asksAlignment041(goal,selected=false){const explicit=/\b(?:mat.to.world|measured (?:mat |plane )?alignment|registered (?:mat|surface)|alignment (?:report|trial|checks)|manual (?:movement|occlusion) trial)\b/i.test(goal),current=selected&&/\b(?:lighter|cutting mat|tabletop|tape measure)\b/i.test(goal)&&/\b(?:where|what|how|status|follow|track|predict|compare)\b/i.test(goal)&&!(/\b(?:038|older|earlier|legacy|old|research|search|google|web|browser|internet|images?|pictures?|pages?)\b/i.test(goal));return (explicit||(current&&!/\b(?:older|earlier|legacy|old)[-_ ]*\d/i.test(goal)))&&!(/\b(?:build|create)\b.*\b(?:website|html)\b/i.test(goal));}
function createAlignment041({api,execute,openArtifact,emit}){
 async function release(job){if(job?.alignmentHash)await api('/api/alignment041/release',{snapshotHash:job.alignmentHash}).catch(()=>{});}
 async function action(tool,args={},job={}){
  if(Object.keys(args).length)throw Error('Alignment tools use the actual selected registration/trial; no supplied measurements, identities or success flags');
  if(tool==='alignment.read'){await release(job);job.alignmentFacts=await api('/api/alignment041/capture',{pin:true});job.alignmentHash=job.alignmentFacts.snapshotHash;return job.alignmentFacts;}
  if(tool==='alignment.report'){if(!job.alignmentHash)throw Error('Read and pin actual alignment evidence first');const{artifact,receipt}=await api('/api/alignment041/report',{snapshotHash:job.alignmentHash});emit('view','preview');await openArtifact(artifact);const test=await execute('canvas.test',{steps:[{selector:'#alignment-hash',expectText:receipt.snapshotHash},{selector:'#alignment-id',expectText:receipt.registrationId},{selector:'#alignment-trial',expectText:receipt.trialId},{selector:'#alignment-landmarks tr',expectCount:receipt.landmarks},{selector:'.views svg',expectCount:4}]});if(!test.passed)throw Error('Actual alignment report checks failed');job.alignmentReport={id:artifact.id,version:artifact.version,passed:true};return job.alignmentReport;}
  throw Error('Unknown measured alignment tool');
 }
 return{action,release};
}
module.exports={createAlignment041,asksAlignment041};
