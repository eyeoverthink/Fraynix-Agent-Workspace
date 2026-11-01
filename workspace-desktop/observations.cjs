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
 async function runAgent({goal,model,job,planTools,command,onTurn}){
  const recent=[];let inspected=false;
  for(let turn=1;turn<=12;turn++){
   if(job.cancelled)throw Error('Stopped by you');
   if(job.observationRunId&&['interrupted','cancelled'].includes(job.observationEvidence?.status))throw Error('Observation run '+job.observationEvidence.status+': '+job.observationEvidence.reason+'; original evidence is retained');
   const needsStart=/\b(start|track|process|update)\b/i.test(goal)&&!(/\b(inspect|explain|read|describe)\b/i.test(goal));
   const allowed=!inspected?['observations.read']:job.observationReport?['finish']:job.observationRunId&&job.observationEvidence?.status==='running'?['observations.wait']:needsStart&&!job.observationRunId?['observations.start']:['observations.report'];
   onTurn(turn,'Connecting real footage to its saved world estimate');job.requestId=randomUUID();
   const reply=await api('/api/plan',{model,prompt:`Use ONE typed tool. ${goal}. Host stage permits ${allowed.join(', ')}. Read actual observed-world state, optionally start the selected footage ONCE if requested, wait for that owned run, then report and finish. Base image-plane estimates have unknown metric scale/FPS. A selected metric interpretation may supply measured/source timing and planar geometry; quote its provenance, uncertainty qualification and stale flag. Never infer3D depth or claim confidence guarantees. Native verified physics is separate. Source labels and model covariance are not recognition probabilities. Do not fabricate IDs, measurements or physical success. Actual recent tool results: ${JSON.stringify(recent).slice(-9000)}`,requestId:job.requestId,allowedTools:allowed,tools:planTools(allowed),...(job.modelDigest?{expectedDigest:job.modelDigest}:{})});job.modelDigest=reply.localModel.digest;
   const next=command(JSON.parse(reply.text));if(!allowed.includes(next.tool))throw Error('Tool cannot advance this observation stage');
   if(next.tool==='finish')return {summary:'Observed-world evidence and the actual report are saved. The saved evidence declares its coordinate, timing and uncertainty scope; any selected metric interpretation applies only to its fixed camera and plane. Native verified physics is unchanged.',observationReport:job.observationReport,observationRunId:job.observationRunId||job.observationEvidence?.id};
   const result=await execute(next.tool,next.args);if(next.tool==='observations.read')inspected=true;if(next.tool==='observations.start')job.observationEvidence=result;recent.push({tool:next.tool,result});while(recent.length>2)recent.shift();
  }
  throw Error('Observation chat reached its12-step budget; the actual Java run and evidence remain saved');
 }
 return {importPack,select,metricSelect,action,runAgent,selection:()=>selection};
}
module.exports={createObservations,asksForObservedWorld,latestObservationFrame};
