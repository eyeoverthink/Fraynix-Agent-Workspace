'use strict';
const sleep=ms=>new Promise(r=>setTimeout(r,ms));
const terminal=g=>['completed','cancelled','interrupted'].includes(g.status);
function goalSummary(g){if(g.procedure==='motion-data-report-v1'){const m=g.metrics;return `Java data goal ${g.status}: ${g.reason}${m?` ${m.rowCount} mapped rows; total travel ${m.totalTravelM.toFixed(6)} m; mean speed ${m.meanSpeedMps.toFixed(6)} m/s. Quality ${m.qualityPassed?'within declared thresholds':`flags: ${m.gapFlags} gaps, ${m.speedFlags} speeds, ${m.skippedSourceRows} skipped source rows`}.`:''}${g.savedSkill?` Saved ${g.savedSkill.name} v${g.savedSkill.version}.`:''}${g.report?` Report ${g.report.id} v${g.report.version}.`:''}`;}return `Java goal ${g.status}: ${g.reason}${g.metrics?` Endpoint error ${g.metrics.endpointErrorM.toFixed(6)} m; terminal speed ${g.metrics.terminalSpeedMps.toFixed(6)} m/s; settling error ${g.metrics.settlingMaxErrorM.toFixed(6)} m; model actions ${(100*g.metrics.modelActionFraction).toFixed(1)}%.`:''}${g.savedSkill?` Saved ${g.savedSkill.name} v${g.savedSkill.version}.`:''}${g.report?` Report ${g.report.id} v${g.report.version}.`:''}`;}

/** Desktop effects are registered host code; the model never receives the claim/ack capability. */
function requestedPhysics(request){
 const patterns={angleDeg:/\b([+\-]?\d+(?:\.\d+)?)\s*(?:degrees?|°)/i,friction:/\bfriction\s*(?:of|=|:)?\s*([+\-]?\d+(?:\.\d+)?)/i,targetM:/\btarget\s*(?:of|=|:)?\s*([+\-]?\d+(?:\.\d+)?)/i,disturbanceMps:/([+\-]?\d+(?:\.\d+)?)\s*m\/s\s+disturbance/i};
 const result=Object.fromEntries(Object.entries(patterns).flatMap(([key,pattern])=>{const m=String(request).match(pattern);return m?[[key,Number(m[1])]]:[];}));
 if(/\b(?:include|attach)\b[^.!?\n]{0,120}\bbrowser\b/i.test(request))result.browserContext=true;
 const name=String(request).match(/\b(?:procedure|skill)\s+(?:as|named)\s+([a-z][a-z0-9-]{2,55})\b/i);if(name)result.saveSkillName=name[1];return result;
}
function requestedData(request){const expected={},patterns={maxGapS:/\bgap(?:\s+(?:threshold|limit))?\s*(?:of|=|:)?\s*([+\-]?\d+(?:\.\d+)?)/i,speedLimitMps:/\bspeed(?:\s+(?:threshold|limit))?\s*(?:of|=|:)?\s*([+\-]?\d+(?:\.\d+)?)/i};for(const [key,re] of Object.entries(patterns)){const m=String(request).match(re);if(m)expected[key]=Number(m[1]);}const p=requestedPhysics(request);if(p.saveSkillName)expected.saveSkillName=p.saveSkillName;return expected;}
function createGoals({api,browser,canvas,openArtifact,emit,refresh,activity,getScene,getDataset,onScene,onTest}){
 let timer,busy=false,latest=null,shownScene='',stopped=false,lastPoll=0;
 async function update(g){latest=g;emit('javaGoal',g);if(g.sceneId&&g.sceneId!==shownScene){shownScene=g.sceneId;await onScene(await api('/api/scene/read',{id:g.sceneId}));}return g;}
 async function poll(){
  if(busy||stopped||((!latest||terminal(latest))&&Date.now()-lastPoll<1200))return;lastPoll=Date.now();busy=true;
  try{
   const all=await api('/api/goals/list',{}),active=all.goals.find(g=>['running','waiting-host'].includes(g.status));
   const next=active||all.goals.at(-1);if(next){const changed=latest?.id!==next.id||latest?.updatedAt!==next.updatedAt;if(changed){await update(next);if(terminal(next)){activity('goal',goalSummary(next));await refresh();}}}
   if(active?.status!=='waiting-host')return;const effect=await api('/api/goals/claim',{});if(!effect.id)return;
   let result,error;
   try{
    if(effect.kind==='browser.observe')result=await browser.observe();
    else if(effect.kind==='canvas.verify-report'){
     const a=await api('/api/artifact/read',{id:effect.report.id});if(a.version!==effect.report.version)throw Error('Pinned report version changed');
     emit('view','preview');await openArtifact(a);const checks=await canvas.test(effect.steps);
     result=await api('/api/artifact/test',{id:a.id,version:a.version,steps:effect.steps,checks});
     if(!result.passed)throw Error('Actual report assertions failed');await onTest?.(effect,result);await openArtifact(await api('/api/artifact/read',{id:a.id}));
    }else throw Error('Unknown registered host effect');
   }catch(e){error=e.message;}
   await update(await api('/api/goals/ack',{goalId:effect.goalId,id:effect.id,nonce:effect.nonce,...(error?{error}:{result})}));
  }catch(e){if(!stopped)activity('goal-error',e.message);}finally{busy=false;}
 }
 async function execute(tool,args={},context){
  let g;args={...args};const parameters={...(args.parameters||{})};for(const key of ['angleDeg','friction','gravityMps2','massKg'])if(key in args){parameters[key]=args[key];delete args[key];}if(Object.keys(parameters).length)args.parameters=parameters;
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
   if(context?.goal&&skill){
    const expected=skill.kind==='motion-data-report-v1'?requestedData(context.goal):requestedPhysics(context.goal);
    const chosen={...(skill.defaults||{}),...(skill.defaults?.parameters||{}),...(args.parameters||{}),...args,browserContext:args.browserContext??false,saveSkillName:args.saveSkillName??''};
    for(const [key,value] of Object.entries(expected))if(chosen[key]!==value)throw Error(`The request specifies ${key}=${value}; send that exact value to procedure.run. No goal started.`);
   }
   if(skill?.kind==='motion-data-report-v1'&&!args.datasetId){const selected=getDataset();if(!selected)throw Error('Choose a source dataset in Data tasks before reusing this procedure');args={datasetId:selected.datasetId,datasetVersion:selected.version,...args};}
   g=await api('/api/procedures/run',args);
  }
  else if(!g){
   const id=context?.ownedGoalId||args.id||latest?.id;if(!id)throw Error('No Java goal selected');
   if(tool==='goal.read'){
    const until=Date.now()+6500;
    do{g=await api('/api/goals/read',{id});if(terminal(g)||context?.cancelled)break;await sleep(250);}while(Date.now()<until);
   }else if(tool==='goal.resume'||tool==='goal.cancel')g=await api('/api/goals/'+tool.split('.')[1],{id});else throw Error('Unknown goal action');
  }
  if(context){context.ownedGoalId=g.id;context.goalEvidence=g;}
  await update(g);if(tool!=='goal.read')await refresh();return g;
 }
 return {execute,start(){stopped=false;clearInterval(timer);timer=setInterval(poll,250);poll();},stop(){stopped=true;clearInterval(timer);},poll,latest:()=>latest};
}
module.exports={createGoals,goalSummary,requestedPhysics,requestedData};
