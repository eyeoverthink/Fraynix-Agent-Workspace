'use strict';
const sleep=ms=>new Promise(r=>setTimeout(r,ms));
const terminal=g=>['completed','cancelled','interrupted'].includes(g.status);
function goalSummary(g){if(g.procedure==='creation-web-v1')return `Java creation ${g.status}: ${g.reason}${g.metrics?` ${g.metrics.drafts} draft(s), ${g.metrics.checks} behavior steps, artifact v${g.metrics.artifactVersion}.`:""}${g.savedSkill?` Saved ${g.savedSkill.name} v${g.savedSkill.version}.`:""}`;if(g.procedure==='browser-perception-v1'){const m=g.metrics;return `Java browser perception ${g.status}: ${g.reason}${m?` ${m.frames} actual frames; ${(m.trackedFraction*100).toFixed(1)}% tracked; ${m.lostObservations} lost, ${m.reacquisitions} reacquired; ${m.appearanceUpdates} gated appearance updates. Labels retain their source authority.`:''}${g.savedSkill?` Saved ${g.savedSkill.name} v${g.savedSkill.version}.`:''}${g.report?` Report ${g.report.id} v${g.report.version}.`:''}`;}if(g.procedure==='spatial-manipulation-v1'){const m=g.metrics;return `Java spatial goal ${g.status}: ${g.reason}${m?` Route ${m.route}; horizontal error ${m.horizontalErrorM.toFixed(6)} m; forecast RMSE ${m.forecastRmseM.toFixed(6)} m; one actual grab/release. Declared-parameter native simulation.`:''}${g.savedSkill?` Saved ${g.savedSkill.name} v${g.savedSkill.version}.`:''}${g.report?` Report ${g.report.id} v${g.report.version}.`:''}`;}if(g.procedure==='motion-data-report-v1'){const m=g.metrics;return `Java data goal ${g.status}: ${g.reason}${m?` ${m.rowCount} mapped rows; total travel ${m.totalTravelM.toFixed(6)} m; mean speed ${m.meanSpeedMps.toFixed(6)} m/s. Quality ${m.qualityPassed?'within declared thresholds':`flags: ${m.gapFlags} gaps, ${m.speedFlags} speeds, ${m.skippedSourceRows} skipped source rows`}.`:''}${g.savedSkill?` Saved ${g.savedSkill.name} v${g.savedSkill.version}.`:''}${g.report?` Report ${g.report.id} v${g.report.version}.`:''}`;}return `Java goal ${g.status}: ${g.reason}${g.metrics?` Endpoint error ${g.metrics.endpointErrorM.toFixed(6)} m; terminal speed ${g.metrics.terminalSpeedMps.toFixed(6)} m/s; settling error ${g.metrics.settlingMaxErrorM.toFixed(6)} m; model actions ${(100*g.metrics.modelActionFraction).toFixed(1)}%.`:''}${g.savedSkill?` Saved ${g.savedSkill.name} v${g.savedSkill.version}.`:''}${g.report?` Report ${g.report.id} v${g.report.version}.`:''}`;}

/** Desktop effects are registered host code; the model never receives the claim/ack capability. */
function requestedPhysics(request){
 const patterns={angleDeg:/\b([+\-]?\d+(?:\.\d+)?)\s*(?:degrees?|°)/i,friction:/\bfriction\s*(?:of|=|:)?\s*([+\-]?\d+(?:\.\d+)?)/i,targetM:/\btarget\s*(?:of|=|:)?\s*([+\-]?\d+(?:\.\d+)?)/i,disturbanceMps:/([+\-]?\d+(?:\.\d+)?)\s*m\/s\s+disturbance/i};
 const result=Object.fromEntries(Object.entries(patterns).flatMap(([key,pattern])=>{const m=String(request).match(pattern);return m?[[key,Number(m[1])]]:[];}));
 if(/\b(?:include|attach)\b[^.!?\n]{0,120}\bbrowser\b/i.test(request))result.browserContext=true;
 const name=String(request).match(/\b(?:procedure|skill|workflow)\s+(?:as|named)\s+([a-z][a-z0-9-]{2,55})\b/i);if(name)result.saveSkillName=name[1];return result;
}
function requestedData(request){const expected={},patterns={maxGapS:/\bgap(?:\s+(?:threshold|limit))?\s*(?:of|=|:)?\s*([+\-]?\d+(?:\.\d+)?)/i,speedLimitMps:/\bspeed(?:\s+(?:threshold|limit))?\s*(?:of|=|:)?\s*([+\-]?\d+(?:\.\d+)?)/i};for(const [key,re] of Object.entries(patterns)){const m=String(request).match(re);if(m)expected[key]=Number(m[1]);}const p=requestedPhysics(request);if(p.saveSkillName)expected.saveSkillName=p.saveSkillName;return expected;}
function requestedSpatial(request){const out={};const explicit=String(request).match(/\bdisplacement(?:M)?\s*(?:of|=|:)?\s*\[\s*([+\-]?\d+(?:\.\d+)?)\s*,\s*([+\-]?\d+(?:\.\d+)?)\s*,\s*([+\-]?\d+(?:\.\d+)?)\s*\]/i);if(explicit)out.displacementM=explicit.slice(1).map(Number);const name=requestedPhysics(request).saveSkillName;if(name)out.saveSkillName=name;return out;}
function preserveSpatial(request,args,defaults={}){for(const [key,value] of Object.entries(requestedSpatial(request)))if(JSON.stringify(args[key]??defaults[key])!==JSON.stringify(value))throw Error('Preserve the explicitly requested spatial '+key+'; no goal started');}
function createGoals({api,browser,canvas,openArtifact,emit,refresh,activity,getScene,getDataset,getSpatial,perception,creation,workflow,getCreationSource,onScene,onTest}){
 let timer,busy=false,latest=null,shownScene='',stopped=false,lastPoll=0;
 async function update(g){latest=g;emit(g.workflowId?'workflowChild':'javaGoal',g);if(g.sceneId&&g.sceneId!==shownScene){shownScene=g.sceneId;await onScene(await api('/api/scene/read',{id:g.sceneId}));}return g;}
 async function poll(){
  if(busy||stopped||((!latest||terminal(latest))&&Date.now()-lastPoll<1200))return;lastPoll=Date.now();busy=true;
  try{
   const all=await api('/api/goals/list',{}),active=all.goals.find(g=>['running','waiting-host'].includes(g.status));
   const next=active||all.goals.at(-1);if(next){const changed=latest?.id!==next.id||latest?.updatedAt!==next.updatedAt;if(changed){await update(next);if(terminal(next)){activity('goal',goalSummary(next));await refresh();}}}
   if(active?.status!=='waiting-host')return;const effect=await api('/api/goals/claim',{});if(!effect.id)return;
   let result,error;
   try{
    if(effect.kind==='creation.plan'||effect.kind==='creation.generate')result=await creation.generate(effect);
    else if(effect.kind==='creation.test'){const a=await api('/api/artifact/read',{id:effect.report.id});if(a.version!==effect.report.version)throw Error('Created version changed');emit('view','preview');await openArtifact(a);const checks=await canvas.test(effect.steps);const proof=await api('/api/artifact/test',{id:a.id,version:a.version,steps:effect.steps,checks});result={id:a.id,version:a.version};await onTest?.(effect,proof);await openArtifact(await api('/api/artifact/read',{id:a.id}));activity('creation-test',proof.passed?'Declared behavior passed':'Declared behavior failed; Java retains the failures',{goalId:effect.goalId,version:a.version,checks:proof.checks});}
    else if(effect.kind==='browser.perception.sample')result=await perception.sample(effect);
    else if(effect.kind==='browser.observe')result=await browser.observe();
    else if(effect.kind==='canvas.verify-report'){
     const a=await api('/api/artifact/read',{id:effect.report.id});if(a.version!==effect.report.version)throw Error('Pinned report version changed');
     emit('view','preview');await openArtifact(a);const checks=await canvas.test(effect.steps);
     result=await api('/api/artifact/test',{id:a.id,version:a.version,steps:effect.steps,checks});
     if(!result.passed)throw Error('Actual report assertions failed');await onTest?.(effect,result);await openArtifact(await api('/api/artifact/read',{id:a.id}));
    }else throw Error('Unknown registered host effect');
   }catch(e){error=e.message;}
   if(['creation.plan','creation.generate','creation.test'].includes(effect.kind)){const current=await api('/api/goals/read',{id:effect.goalId});if(!['running','waiting-host'].includes(current.status))return;}
   await update(await api('/api/goals/ack',{goalId:effect.goalId,id:effect.id,nonce:effect.nonce,...(error?{error}:{result})}));
  }catch(e){if(!stopped)activity('goal-error',e.message);}finally{busy=false;}
 }
 async function execute(tool,args={},context){
  let g;args={...args};const parameters={...(args.parameters||{})};for(const key of ['angleDeg','friction','gravityMps2','massKg'])if(key in args){parameters[key]=args[key];delete args[key];}if(Object.keys(parameters).length)args.parameters=parameters;
  if(tool==='creation.start')g=await api('/api/goals/creation-start',args);
  if(tool==='perception.read')return {...await perception.catalog(),lessons:(await api('/api/perception/state',{})).lessons.map(l=>({name:l.name,version:l.version,label:l.label})),scope:'DOM text is page-stated. Vision boxes are estimates. Taught appearance is not named-person identity.'};
  if(tool==='perception.select')return perception.select(args.ref);
  if(tool==='perception.start'){if(context?.goal){const name=requestedPhysics(context.goal).saveSkillName;if(name&&args.saveSkillName!==name)throw Error('Preserve the requested skill name');const seconds=String(context.goal).match(/\bfor\s+(\d+(?:\.\d+)?)\s+seconds?\b/i);if(seconds&&(args.seconds??6)!==Number(seconds[1]))throw Error('Preserve the requested tracking duration');args.request=context.goal;}if('lessonName' in args){const name=args.lessonName;delete args.lessonName;if(name){const lessons=(await api('/api/perception/state',{})).lessons.filter(l=>l.name===name).sort((a,b)=>b.version-a.version);if(!lessons.length)throw Error('Unknown taught appearance name');args.lessonId=lessons[0].id;}}const frame=await perception.capture();g=await api('/api/goals/perception-start',{...args,frameId:frame.id});}
  if(tool==='spatial.read'){const selected=getSpatial();if(!selected)throw Error('Import a scene and select a dynamic object in Spatial first');const s=await api('/api/spatial/read',{id:selected.sceneId});return {binding:selected,title:s.title,source:s.source,scaleCheck:s.scaleCheck,objects:s.objects,modelKind:'Native rigid-body forward simulation, not universal learned dynamics'};}
  if(tool==='spatial.start'){const selected=getSpatial();if(!selected)throw Error('Select an imported dynamic object in Spatial first');if(context?.goal){preserveSpatial(context.goal,args);args.request=context.goal;}for(const key of ['sceneId','objectId'])if(args[key]!==undefined&&args[key]!==selected[key])throw Error('Spatial selection binding changed');g=await api('/api/goals/spatial-start',{...args,...selected});}
  if(tool==='data.read'){const selected=getDataset();if(!selected)throw Error('Open Data tasks and import or select a dataset first');const d=await api('/api/measurement/read',{id:selected.datasetId,version:selected.version});return {binding:{datasetId:d.datasetId,version:d.version},title:d.title,rows:d.rows.length,sourceKind:d.sourceKind,sourceSha256:d.sourceSha256,mapping:d.mapping,skippedRows:d.skippedRows,scope:'Source labels are user supplied; no physics or hardware validation',sample:d.rows.slice(0,3)};}
  if(tool==='data.start'){const selected=getDataset();if(!selected)throw Error('Choose an imported dataset in Data tasks first');if(context?.goal){const expected=requestedData(context.goal);const chosen={maxGapS:args.maxGapS??.25,speedLimitMps:args.speedLimitMps??5,saveSkillName:args.saveSkillName??''};for(const [key,value] of Object.entries(expected))if(chosen[key]!==value)throw Error(`The request specifies ${key}=${value}; send that exact value to data.start. No goal started.`);args.request=context.goal;}g=await api('/api/goals/data-start',{datasetId:selected.datasetId,datasetVersion:selected.version,...args});}
  else if(tool==='world.model'){const s=await api('/api/brain/state',{});return {model:s.activeWorldModel,jobs:s.jobs.filter(j=>j.kind==='world-model-train'),domain:{angleDeg:[10,40],friction:[0,.3],gravityMps2:[8,12],massKg:[.5,2],targetM:[.4,1.8],geometry:'12 × 3 m ramp, Z=0'},scope:'Native simulation; recorded real-data action use is not authorized'};}
  if(tool==='procedures.list')return (await api('/api/goals/list',{})).procedures;
  if(tool==='procedure.save'){const id=context?.ownedGoalId||args.goalId;if(!id)throw Error('Choose a completed Java goal');return api('/api/procedures/save',{goalId:id,name:args.name});}
  else if(tool==='goal.start'){
   const source=getScene();if(context?.goal){const chosen={...(source?.parameters||{angleDeg:25,friction:0}),...(args.parameters||{}),targetM:args.targetM??1.2,disturbanceMps:args.disturbanceMps??.35,browserContext:args.browserContext??false,saveSkillName:args.saveSkillName??''};for(const [key,value] of Object.entries(requestedPhysics(context.goal)))if(chosen[key]!==value)throw Error(`The request specifies ${key}=${value}; send that exact value to goal.start. No goal has started.`);args.request=context.goal;}g=await api('/api/goals/start',{...(source&&!args.sceneId?{sceneId:source.id,expectedVersion:source.version}:{}),...args});
  }else if(tool==='procedure.run'){
   if(context?.procedureBinding){for(const key of ['name','version'])if(args[key]!==undefined&&args[key]!==context.procedureBinding[key])throw Error('The selected procedure binding changed');args={name:context.procedureBinding.name,version:context.procedureBinding.version,...args};}
   const skills=(await api('/api/goals/list',{})).procedures,skill=skills.find(p=>p.name===args.name&&p.version===args.version);
   if(skill?.kind==='evidence-workflow-v1')return workflow.execute('workflow.reuse',args,context);
   if(context?.goal&&skill){
    const expected=skill.kind==='motion-data-report-v1'?requestedData(context.goal):requestedPhysics(context.goal);
    const chosen={...(skill.defaults||{}),...(skill.defaults?.parameters||{}),...(args.parameters||{}),...args,browserContext:args.browserContext??false,saveSkillName:args.saveSkillName??''};
    for(const [key,value] of Object.entries(expected))if(chosen[key]!==value)throw Error(`The request specifies ${key}=${value}; send that exact value to procedure.run. No goal started.`);
   }
   if(skill?.kind==='creation-web-v1'&&!args.sourceArtifactId&&skill.sourceRequirements.required){const selected=getCreationSource?.();if(!selected)throw Error('Choose a CSV source for this creation procedure');args={...args,...selected};}
   if(skill?.kind==='browser-perception-v1'){if(skill.defaults.mode==='appearance'&&args.targetLabel&&args.targetLabel!==skill.defaults.targetLabel)throw Error('This saved taught-appearance procedure retains its category; select another taught procedure for a different category.');if(context?.goal&&Object.keys(args).some(k=>!['name','version','targetLabel','seconds','saveSkillName'].includes(k)))throw Error('procedure.run uses its saved detector policy. Send only targetLabel, seconds and optional saveSkillName; no lessonName or mode.');if(context?.goal){const seconds=String(context.goal).match(/\bfor\s+(\d+(?:\.\d+)?)\s+seconds?\b/i);if(seconds&&(args.seconds??skill.defaults.seconds)!==Number(seconds[1]))throw Error('Preserve the requested tracking duration');}if('lessonName' in args){const name=args.lessonName;delete args.lessonName;if(name){const lessons=(await api('/api/perception/state',{})).lessons.filter(l=>l.name===name).sort((a,b)=>b.version-a.version);if(!lessons.length)throw Error('Unknown taught appearance name');args.lessonId=lessons[0].id;}}const frame=await perception.capture();args={...args,frameId:frame.id};}
   if(skill?.kind==='spatial-manipulation-v1'){const selected=getSpatial();if(!selected)throw Error('Select an imported spatial scene and dynamic object first');if(context?.goal)preserveSpatial(context.goal,args,skill.defaults);for(const key of ['sceneId','objectId'])if(args[key]!==undefined&&args[key]!==selected[key])throw Error('Spatial selection binding changed');args={...args,...selected};}
   if(skill?.kind==='motion-data-report-v1'&&!args.datasetId){const selected=getDataset();if(!selected)throw Error('Choose a source dataset in Data tasks before reusing this procedure');args={datasetId:selected.datasetId,datasetVersion:selected.version,...args};}
   g=await api('/api/procedures/run',args);
  }
  else if(!g){
   const id=context?.ownedGoalId||args.id||latest?.id;if(!id)throw Error('No Java goal selected');
   if(tool==='goal.read'){
    const until=Date.now()+6500;
    do{g=await api('/api/goals/read',{id});if(terminal(g)||context?.cancelled)break;await sleep(250);}while(Date.now()<until);
   }else if(tool==='goal.resume'&&(await api('/api/goals/read',{id})).procedure==='browser-perception-v1'){const prior=await api('/api/goals/read',{id});const r=prior.runId?await api('/api/perception/read',{id:prior.runId}):null;if(r?.status==='completed'&&r.metrics.passed)g=await api('/api/goals/resume',{id});else{const f=await perception.capture();g=await api('/api/goals/perception-resume',{id,frameId:f.id});}}else if(tool==='goal.resume'||tool==='goal.cancel'){if(tool==='goal.cancel')await creation?.cancel(id);g=await api('/api/goals/'+tool.split('.')[1],{id});}else throw Error('Unknown goal action');
  }
  if(context){context.ownedGoalId=g.id;context.goalEvidence=g;}
  await update(g);if(tool!=='goal.read')await refresh();return g;
 }
 return {execute,start(){stopped=false;clearInterval(timer);timer=setInterval(poll,250);poll();},stop(){stopped=true;clearInterval(timer);creation?.stop();},cancelGeneration:id=>creation?.cancel(id),poll,latest:()=>latest};
}
module.exports={createGoals,goalSummary,requestedPhysics,requestedData,requestedSpatial};
