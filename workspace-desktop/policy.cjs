'use strict';
const { randomUUID } = require('node:crypto');
const catalog = [
  ...['start','read','cancel','resume','apply','rollback','report'].map(action=>['calibration.'+action,({start:'Start a durable one-parameter native Bullet search. Training-only selection then separate validation. Does not edit authored scenes.',read:'Read real job progress, selection and validation metrics. Use this to wait for settlement.',cancel:'Cancel an active native calibration.',resume:'Explicitly resume a cancelled or interrupted calibration using frozen inputs.',apply:'Apply only a completed accepted current fit; returns durable scene receipt.',rollback:'Restore the prior parameter only without intervening authored changes.',report:'Open and test a real host-rendered calibration report with original CSV and exact native evidence.'})[action],action==='start'?'{trainingSceneId,trainingSceneVersion,validationSceneId,validationSceneVersion,trainingDatasetId,trainingDatasetVersion,validationDatasetId,validationDatasetVersion,parameter:friction|gravityMps2,minimum,maximum,seconds,toleranceM,minImprovementM}':'{id}']),
  ['dataset.import','Import actual UTF-8 CSV with explicit column mapping, units, source classification and notes. Does not infer units or execute cells.','{title,filename,csv,sourceKind:"measured"|"synthetic"|"unverified",sourceNote,missingPolicy:"reject"|"skip",mapping:{timeColumn,distanceColumn,timeUnit:"s"|"ms",distanceUnit:"m"|"cm"|"mm"}}'],
  ['dataset.read','Read exact measurement version, source digest, mapping and up to 12 sample rows.','{id,version}'],
  ['experiment.compare','Compare every dataset row to a completed native run using interpolation, no extrapolation. Both bindings must match current saved versions. Receipt includes actual residuals and declared tolerance.','{datasetId,datasetVersion,runId,expectedSceneVersion,toleranceM}'],
  ['comparison.report','Create and open a standalone local report, preserving original CSV and comparison JSON. Host verifies actual report DOM; numerical agreement is separate.','{id:comparisonId}'],
  ['scene.create','Create a persistent actual 3D ramp scene with SI parameters, stable entity IDs and version.','{title,parameters?:{angleDeg,lengthM,widthM,gravityMps2,massKg,friction,offsetZ}}'],
  ['scene.read','Read the authored scene and its real 3D entities.','{id}'],
  ['scene.update','Revise bounded authored parameters after reset, with version conflict protection.','{id,expectedVersion,title,parameters}'],
  ['experiment.run','Run actual native Bullet physics (0.05 to 10 simulated seconds), store results and independent frictionless-incline reference evaluation. Use experiment.reset before another run.','{id,expectedVersion,seconds}'],
  ['experiment.reset','Stop/reset the current physical experiment, preserving recorded runs and authored scene. Required before edits or another run.','{}'],
  ['browser.navigate','Navigate the visible real browser to an http/https URL.','{url}'],
  ['browser.observe','Read bounded page text and interactive controls, with observation ID.','{}'],
  ['browser.click','Click a control from a fresh observation.','{observation,ref}'],
  ['browser.type','Fill a text control from a fresh observation.','{observation,ref,text}'],
  ['canvas.write','Create or revise a real runnable web artifact. index.html required. Existing artifact requires expectedVersion. Opens live preview.','{title,files:{"index.html":"HTML source",...},sources?:[URL],id?:UUID,expectedVersion?:number}'],
  ['canvas.read','Read an existing artifact and current revision.','{id}'],
  ['canvas.observe','Inspect the visible running artifact.','{}'],
  ['canvas.test','Interact with the current artifact and check exact observable results. At least one assertion. Saves real receipt.','{steps:[{selector,action?:"click"|"fill",value?:string,expectText?:string,expectValue?:string,expectCount?:number}]}'],
  ['artifacts.list','List saved artifacts and versions.','{}'],
  ['skills.list','List verified reusable web-artifact recipes.','{}'],
  ['skill.save','Save the current artifact as a reusable recipe only after tests pass.','{name:"lowercase-name",description}'],
  ['skill.run','Recreate and test an existing verified recipe as a new artifact.','{name}'],
  ['memory.search','Search existing project memory through the Fraynix kernel.','{query}'],
  ['finish','Report completion, grounded in actual tool results. Created artifacts must have passed tests.','{summary}']
].map(([name,description,args])=>({name,description,args}));
function webURL(value) {
  const url = new URL(value);
  if (!['http:','https:'].includes(url.protocol) || url.username || url.password) throw Error('Use an HTTP or HTTPS URL without embedded credentials');
  return url.href;
}
function command(raw) {
  if (!raw || typeof raw!=='object' || Array.isArray(raw) || !catalog.some(t=>t.name===raw.tool)) throw Error('Unknown tool');
  if (!raw.args || typeof raw.args!=='object' || Array.isArray(raw.args)) throw Error('Tool args must be an object');
  if (Object.keys(raw).some(k=>!['tool','args','reason'].includes(k))) throw Error('Only tool, args and reason are allowed');
  return raw;
}
class ObservationLease {
  constructor(){this.revision=0;this.id='';}
  invalidate(){this.revision++;this.id='';}
  create(){this.id=randomUUID();return this.id;}
  require(id){if(!this.id || id!==this.id)throw Error('Stale observation. Observe this surface again before acting.');}
}
const text={type:'string'},empty={type:'object',properties:{},additionalProperties:false};
function object(properties,required=Object.keys(properties)){return {type:'object',properties,required,additionalProperties:false};}
const argumentSchemas={
 'calibration.start':object({trainingSceneId:text,trainingSceneVersion:{type:'integer',minimum:1},validationSceneId:text,validationSceneVersion:{type:'integer',minimum:1},trainingDatasetId:text,trainingDatasetVersion:{type:'integer',minimum:1},validationDatasetId:text,validationDatasetVersion:{type:'integer',minimum:1},parameter:{type:'string',enum:['friction','gravityMps2']},minimum:{type:'number'},maximum:{type:'number'},seconds:{type:'number',minimum:.05,maximum:10},toleranceM:{type:'number',minimum:0,maximum:10},minImprovementM:{type:'number',exclusiveMinimum:0,maximum:10}}),
 'calibration.read':object({id:text}),'calibration.cancel':object({id:text}),'calibration.resume':object({id:text}),'calibration.apply':object({id:text}),'calibration.rollback':object({id:text}),'calibration.report':object({id:text}),
 'dataset.import':object({title:text,filename:text,csv:{type:'string',maxLength:262144},sourceKind:{type:'string',enum:['measured','synthetic','unverified']},sourceNote:text,missingPolicy:{type:'string',enum:['reject','skip']},mapping:object({timeColumn:text,distanceColumn:text,timeUnit:{type:'string',enum:['s','ms']},distanceUnit:{type:'string',enum:['m','cm','mm']}})}),
 'dataset.read':object({id:text,version:{type:'integer',minimum:1}}),
 'experiment.compare':object({datasetId:text,datasetVersion:{type:'integer',minimum:1},runId:text,expectedSceneVersion:{type:'integer',minimum:1},toleranceM:{type:'number',minimum:0,maximum:10}}),
 'comparison.report':object({id:text}),
 'scene.create':object({title:text,parameters:{type:'object',properties:Object.fromEntries(['angleDeg','lengthM','widthM','gravityMps2','massKg','friction','offsetZ'].map(k=>[k,{type:'number'}])),additionalProperties:false}},['title']),
 'scene.read':object({id:text}),
 'scene.update':object({id:text,expectedVersion:{type:'integer',minimum:1},title:text,parameters:{type:'object',properties:Object.fromEntries(['angleDeg','lengthM','widthM','gravityMps2','massKg','friction','offsetZ'].map(k=>[k,{type:'number'}])),additionalProperties:false}}),
 'experiment.run':object({id:text,expectedVersion:{type:'integer',minimum:1},seconds:{type:'number',minimum:.05,maximum:10}}),
 'experiment.reset':empty,
 'browser.navigate':object({url:text}), 'browser.observe':empty,
 'browser.click':object({observation:text,ref:text}), 'browser.type':object({observation:text,ref:text,text}),
 'canvas.write':object({title:{type:'string',minLength:1},files:{type:'object',properties:{'index.html':{type:'string',minLength:30}},required:['index.html'],additionalProperties:text},sources:{type:'array',items:text},id:text,expectedVersion:{type:'integer',minimum:1}},['title','files']),
 'canvas.read':object({id:text}),'canvas.observe':empty,
 'canvas.test':object({steps:{type:'array',minItems:1,maxItems:24,items:{...object({selector:text,action:{type:'string',enum:['click','fill']},value:text,expectText:text,expectValue:text,expectCount:{type:'integer',minimum:0}},['selector']),anyOf:[{required:['expectText']},{required:['expectValue']},{required:['expectCount']}]}}}),
 'artifacts.list':empty,'skills.list':empty,
 'skill.save':object({name:{type:'string',pattern:'^[a-z][a-z0-9-]{2,55}$'},description:text}),
 'skill.run':object({name:text}),'memory.search':object({query:text}),'finish':object({summary:text})
};
function calibrationApplyRequested(goal){return /\bapply\b/i.test(goal)&&!/\b(do not|don't|never|without|no)\s+(?:\w+\s+){0,3}apply\b/i.test(goal);}
function calibrationStage(context,goal){
 const job=context.calibrationEvidence;
 if(!job)return {allowed:['scene.read','dataset.read','calibration.start'],stage:'Start exactly one calibration using the requested saved bindings and criteria.'};
 const prefix='Current job '+job.id+' is '+job.status+'. ';
 if(['queued','running'].includes(job.status))return {allowed:['calibration.read','calibration.cancel'],stage:prefix+'Read this job until settled; never start another.'};
 if(['cancelled','interrupted'].includes(job.status))return {allowed:['calibration.resume'],stage:prefix+'Explicit resume continues the same frozen job.'};
 if(job.status!=='completed')throw Error(prefix+(job.reason||'Native calibration failed'));
 if(calibrationApplyRequested(goal)&&job.evaluation.accepted&&!job.application?.applyReceipt)return {allowed:['calibration.apply'],stage:prefix+'The user requested application. Apply this accepted fit using its id; current bindings are checked by the host.'};
 if(context.calibrationReportId!==job.id)return {allowed:['calibration.report'],stage:prefix+'Open this completed job report, including a failed acceptance result; do not retune against validation.'};
 return {allowed:['finish'],stage:prefix+'A tested report and any required application receipt exist. Finish with actual selection, acceptance, training and validation metrics.'};
}
function planTools(allowed){return catalog.filter(t=>allowed.includes(t.name)).map(t=>({type:'function',function:{name:t.name.replace('.','_'),description:t.description,parameters:argumentSchemas[t.name]}}));}
function taskMode(goal){const needsScene=/\b(3d|physics|ramp|trajectory|incline|downhill)\b/i.test(goal)||(/\b(compare|comparison)\b/i.test(goal)&&/\b(measurements?|observed|simulated)\b/i.test(goal));const needsCalibration=needsScene&&/\b(calibrat\w*|fit|fitting)\b/i.test(goal);return {...(needsCalibration?{needsCalibration:true}:{}),needsScene,needsComparison:!needsCalibration&&needsScene&&/\b(measurements?|observations?|dataset|csv|residual|comparison)\b/i.test(goal)};}
module.exports={catalog,webURL,command,ObservationLease,planTools,taskMode,calibrationApplyRequested,calibrationStage};
