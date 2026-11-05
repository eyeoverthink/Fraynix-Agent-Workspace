'use strict';
const {randomUUID}=require('node:crypto');
const sleep=ms=>new Promise(r=>setTimeout(r,ms));
function comparisonSummary(j){const g=j.gate;return `Tracking comparison ${j.status}: ${j.reason}${g?` Evaluation mean IoU ${g.baselineMeanIou.toFixed(4)} → ${g.candidateMeanIou.toFixed(4)}; ${g.scoredEvaluationFrames} prediction frames; candidate ${g.passed?'passed':'rejected'} under the fixed gates.`:''} These are human-guided box measurements on declared sequences, not general recognition accuracy.`;}
function createTracking({api,openArtifact,execute,origin,activity,emit}){
 let selected=[];
 async function select(ids){if(!Array.isArray(ids)||ids.length>4)throw Error('Select up to4 tracking sequences');for(const id of ids)await api('/api/tracking/dataset',{id});selected=[...new Set(ids)];return {selected};}
 async function importPack(pack){if(!pack||pack.schema!=='fraynix-tracking-sequence-v1'||!Array.isArray(pack.frames)||JSON.stringify(pack).length>34000000)throw Error('Choose a tracking sequence package up to34 MB');const {frames,...metadata}=pack;let id;try{({id}=await api('/api/tracking/import-begin',{...metadata,frameCount:frames.length}));for(let ordinal=0;ordinal<frames.length;ordinal++)await api('/api/tracking/import-frame',{id,ordinal,frame:frames[ordinal]});return await api('/api/tracking/import-commit',{id});}catch(e){if(id)await api('/api/tracking/import-cancel',{id}).catch(()=>{});throw e;}}
 async function action(tool,args={},context){
  if(tool==='tracking.read'){const state=await api('/api/tracking/state',{});return {...state,selected:selected.map(id=>state.datasets.find(d=>d.id===id)).filter(Boolean),scope:'Source roles/license are declared. Later references are used only by the Java scorer, not the predictor. Promotion cannot bypass fixed numeric and negative-control gates.'};}
  if(tool==='tracking.start'){if(Object.keys(args).some(k=>k!=='autoPromote'))throw Error('Only autoPromote is a model option; the host binds selected datasets and Java owns fixed gates');if(context?.trackingJobId)throw Error('This chat already owns a comparison; wait for it instead of starting duplicates');const job=await api('/api/tracking/start',{datasetIds:selected,autoPromote:args.autoPromote??true});if(context)context.trackingJobId=job.id;activity('tracking','Started a real frozen-profile comparison',{id:job.id,selected});return job;}
  if(tool==='tracking.rollback'){const state=await api('/api/tracking/rollback',{});if(context)context.trackingRollback=state.activeProfile==='rgb-v1';activity('tracking','Baseline tracker active',{generation:state.generation});return {activeProfile:state.activeProfile,generation:state.generation};}
  const id=context?.trackingJobId||args.id;if(!id)throw Error('Choose an actual tracking comparison');
  if(tool==='tracking.cancel')return api('/api/tracking/cancel',{id});
  if(tool==='tracking.wait'){let j;const until=Date.now()+6500;do{j=await api('/api/tracking/read',{id});if(j.status!=='running'||context?.cancelled)break;await sleep(150);}while(Date.now()<until);if(context)context.trackingEvidence=j;return {id:j.id,status:j.status,reason:j.reason,completedFrames:j.completedFrames,totalFrames:j.totalFrames,gate:j.gate,activation:j.activation};}
  if(tool==='tracking.report'){const j=await api('/api/tracking/read',{id});if(j.status!=='completed')throw Error('Wait for a completed comparison before reporting');const {artifact,checks}=await api('/api/tracking/report',{id});emit?.('view','preview');await openArtifact(artifact);const receipt=await execute('canvas.test',{steps:checks});if(!receipt.passed)throw Error('Actual comparison report assertions failed');const result={comparisonId:id,id:receipt.id,version:receipt.version,passed:true,checks:receipt.checks,gate:j.gate};if(context)context.trackingReport=result;return result;}
  throw Error('Unknown registered tracking tool');
 }
 return {select,importPack,action,selected:()=>[...selected]};
}
module.exports={createTracking,comparisonSummary};
