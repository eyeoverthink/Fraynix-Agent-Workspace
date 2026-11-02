'use strict';
const {randomUUID}=require('node:crypto');
const sleep=ms=>new Promise(r=>setTimeout(r,ms));
function asksForObservedWorld(goal){const creation=/\b(build|create|design|generate)\b/i.test(goal)&&/\b(html|website|dashboard|viewer)\b/i.test(goal);return !creation&&/\b(observed world|world estimate|footage|observation run)\b/i.test(goal);}
function latestObservationFrame(run){const frame=run?.trace?.at(-1);return frame?{runId:run.id,datasetId:run.datasetId,nativeBinding:run.nativeBinding,ordinal:frame.ordinal,sourceFrame:frame.sourceFrame,objects:frame.objects.map(o=>({bodyId:o.bodyId,trackingStatus:o.tracker.status,...o.estimate}))}:null;}
function createObservations({api,openArtifact,execute,emit,activity}){
 let selection=null,metricSelection=null;
 async function metricSelect(args){const view=await api('/api/metrics/view',args);metricSelection=structuredClone(args);return view;}
 async function importPack(pack){
  if(pack?.schema==='fraynix-observation-frames-037-v1'){const{seconds,cameraKey,timingSource,...legacy}=pack;if(!Array.isArray(seconds)||seconds.length!==pack.frames?.length||seconds.some((v,i)=>!Number.isFinite(v)||v<0||v>1000000||i&&(v<=seconds[i-1]||v-seconds[i-1]>5))||!/^[a-zA-Z0-9_-]{1,80}$/.test(cameraKey||'')||typeof timingSource!=='string'||!timingSource.trim())throw Error('Declare valid source seconds and a fixed camera key before import');legacy.schema='fraynix-observation-frames-036-v1';const d=await importPack(legacy);await api('/api/metrics/clock',{datasetId:d.id,seconds,cameraKey,source:timingSource});return d;}
  if(!pack||pack.schema!=='fraynix-observation-frames-036-v1'||!Array.isArray(pack.frames)||JSON.stringify(pack).length>46000000)throw Error('Choose an observation frame package up to46MB');
  const{frames,...metadata}=pack;let id;
  try{({id}=await api('/api/observations/import-begin',{...metadata,frameCount:frames.length}));for(let ordinal=0;ordinal<frames.length;ordinal++)await api('/api/observations/import-frame',{id,ordinal,frame:frames[ordinal]});return await api('/api/observations/import-commit',{id});}
  catch(e){if(id)await api('/api/observations/import-cancel',{id}).catch(()=>{});throw e;}
 }
 async function select(args){const state=await api('/api/observations/state',{worldId:args.worldId});selection={...args,...state.nativeBinding,expectedObservationRevision:state.estimate.revision};return structuredClone(selection);}
 async function action(tool,args={},context){
  if(Object.keys(args).length)throw Error('Observation tools accept no model supplied IDs, thresholds or measurements; use the visible selected world and footage');
  const chosen=context?.observationSelection||selection;if(!chosen)throw Error('Select a saved world, footage and stable body bindings in Spatial first');
  if(context&&!context.observationSelection)context.observationSelection=structuredClone(chosen);
  if(tool==='observations.read'){
   let metric=null;if(metricSelection){try{metric=await api('/api/metrics/view',metricSelection);if(metric.nativeBinding.worldId!==chosen.worldId)metric=null;}catch(e){metric={unavailable:e.message};}}
   const state=await api('/api/observations/state',{worldId:chosen.worldId});const active=state.runs.filter(r=>r.nativeBinding.worldId===chosen.worldId).at(-1);
   if(context){context.observationEvidence=active;if(metric?.runId===active?.id)context.metricSelection=structuredClone(metricSelection);}
   const lastFrame=active?latestObservationFrame(await api('/api/observations/read',{id:active.id})):null;
   return {metric:metric?{...metric,rows:metric.rows?.slice(-8),clock:metric.clock?{...metric.clock,seconds:undefined}:undefined}:null,nativeBinding:state.nativeBinding,stale:state.stale,estimate:{...state.estimate,history:undefined},lastFrame,selected:{...chosen},run:active,scope:state.scope+' Retained tracks are last accepted measurements; lastFrame separately exposes missing/rejected predictions and their growing model uncertainty. Recorded footage is not a live camera observation.'};
  }
  if(tool==='observations.start'){
   if(context?.observationRunId)throw Error('This request already owns an observation run');
   const r=await api('/api/observations/start',chosen);if(context)context.observationRunId=r.id;activity('observations','Tracking real source pixels in the selected saved world',{id:r.id,worldId:chosen.worldId});return r;
  }
  const id=context?.observationRunId||context?.observationEvidence?.id;if(!id)throw Error('Read the actual selected world observation history first');
  if(tool==='observations.wait'){let r;const end=Date.now()+6500;do{r=await api('/api/observations/read',{id});if(r.status!=='running'||context?.cancelled)break;await sleep(100);}while(Date.now()<end);if(context)context.observationEvidence=r;return {id:r.id,status:r.status,reason:r.reason,completedFrames:r.completedFrames,totalFrames:r.totalFrames,last:r.trace.at(-1)};}
  if(tool==='observations.report'){
   const r=await api('/api/observations/read',{id});if(!r.completedFrames)throw Error('No actual observations to chart yet');
   if(context?.metricSelection?.runId===id){const view=await api('/api/metrics/view',context.metricSelection);const {artifact}=await api('/api/metrics/report',context.metricSelection);emit('view','preview');await openArtifact(artifact);const receipt=await execute('canvas.test',{steps:[{selector:'#rows tr',expectCount:view.rows.length},{selector:'#metric option',expectCount:4},{selector:'#identity',expectText:'World '+view.nativeBinding.worldId+' · native revision '+view.nativeBinding.expectedRevision+' · observation '+id+(view.nativeStale?' · STALE native binding':'')}]});if(!receipt.passed)throw Error('Actual metric report failed');context.observationReport={id:artifact.id,version:artifact.version,passed:true,metric:true};return {id:artifact.id,passed:true,runId:id,metric:true,scope:view.scope,uncertaintyMeaning:view.uncertaintyMeaning};}
   const {artifact}=await api('/api/observations/report',{id});emit('view','preview');await openArtifact(artifact);
   const receipt=await execute('canvas.test',{steps:[{selector:'#rows tr',expectCount:r.trace.reduce((n,f)=>n+f.objects.length,0)},{selector:'#identity',expectText:'World '+r.nativeBinding.worldId+' · native revision '+r.nativeBinding.expectedRevision+' · observation run '+r.id+' · '+r.status},{selector:'#metric option',expectCount:5}]});
   if(!receipt.passed)throw Error('Actual observation chart did not pass');if(context)context.observationReport={id:artifact.id,version:artifact.version,passed:true};return {id:artifact.id,passed:true,runId:id};
  }
  throw Error('Unknown observation tool');
 }
 return {importPack,select,metricSelect,action,selection:()=>selection,metricSelection:()=>metricSelection};
}
module.exports={createObservations,asksForObservedWorld,latestObservationFrame};
