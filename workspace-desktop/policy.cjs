'use strict';
const { randomUUID } = require('node:crypto');
const catalog = [
 ['alignment.read','Read and pin the selected measured mat registration and manual trial, independent checks, source clocks and actual failures. No camera capture, supplied truth or native motion.','{}'],
 ['alignment.report','Open and check exact pinned numeric alignment/physical-trial evidence, source identities, landmark rows and Java views. No raw camera pixels or native action.','{}'],
 ['runtime.read','Read the user-selected shared spatial source, actual stable entity identities, source clocks/units and separately qualified observations. Pins exact data; cannot start or actuate a runtime.','{}'],
 ['runtime.project','Inspect an exact-source Java perspective/top/front/side projection. Game render layer is not physical depth; no geometry or poses can be supplied.','{view}'],
 ['runtime.report','Open and verify the exact pinned snapshot report, source/entity identity, camera projections and compact rows. Does not change the source.','{}'],
 ['self.read','Read current registered capabilities, actual host/network/camera state, persistent object and research counts, recent execution receipts. Network allowed is not proof of connectivity.','{query?}'],
 ['object.read','Read the selected durable human-associated object, original camera provenance and actual research history. Labels are human guidance, not visual recognition.','{}'],
 ['object.research','Start or join bounded real web research for the selected object human category in a separate browser. Original camera images are not uploaded.','{}'],
 ['object.wait','Read saved progress of this task research job without starting duplicate work.','{}'],
 ['object.report','Open and test the actual cached-source object report with source URLs/hashes and identity. Candidate references are not visual matches or measured physical properties.','{}'],
 ['tabletop.answer','Ask a registered Java evidence question about selected tabletop position, motion, visibility, calibration, prediction or limits. No model-supplied measurements.','{question}'],
 ['tabletop.read','Read the explicitly selected actual tabletop camera run, planar measurements, tape check, freshness and uncertainty. Human target labels are not recognition.','{}'],
 ['tabletop.report','Open and test the actual numeric tabletop report; no private camera image is included.','{}'],
 ['observations.read','Read actual observed-world estimates, uncertainty, stable body identities and recorded footage history. Pixel coordinates/frame-index time; no physical calibration implied.','{}'],
 ['observations.start','Process explicitly selected original footage and bindings ONCE; Java owns tracker, prediction filter and persistent estimate revisions.','{}'],
 ['observations.wait','Wait for this request owned real observation run; retain losses and rejected updates.','{}'],
 ['observations.report','Open and test the actual saved motion report from recorded evidence.','{}'],
 ['world.observe','Observe the selected persistent world current revision and all native body kinematics; explicitly refresh a stale request binding.','{}'],
 ['world.arrange','Continue the current versioned world with bounded native forces/torques; verify all bodies and actual charts before advancing its head. Never reset the original source.','{targets?:[{sourceId,positionM:[x,y,z],yawDeg}],forceLimitN?,torqueLimitNm?,saveSkillName?}'],
 ['pose.compare','Run the frozen native multi-object pose/translation-only ablation with fixed source data and gates. Not a033 contact-controller promotion.','{}'],
 ['pose.study','Read current native pose comparison history.','{}'],
 ['pose.read','Read actual selected multi-object scene, displayed target poses and virtual gripper limits.','{}'],
 ['pose.start','Arrange2..4 actual source roles at target positions and yaw by bounded native virtual-gripper forces/torques; release, verify all bodies/charts and optionally retain a procedure.','{targets?:[{sourceId,positionM:[x,y,z],yawDeg}],forceLimitN?,torqueLimitNm?,saveSkillName?}'],
 ['contact.compare','Compare the original and versioned solver-contact controller on frozen development and different reserved native cases. Settings, sources, gates and results belong to Java.','{}'],
 ['contact.read','Read contact study history and the currently validated controller.','{}'],
 ['force.start','Run the frozen native force-learning experiment: training-only cap selection, paired reserved evaluation and exact report verification. No user supplied gates, data, force settings or success values.','{}'],
 ['force.read','Read the frozen suite scope and current active evaluated force profile.','{}'],
 ['map.read','Inspect the selected original palette-map reference or compiled native world. User-declared passability, actual source hash and learned frequencies; host binds selection.','{}'],
 ['push.read','Observe the actual selected crate/pusher roles, source geometry, masses, friction and committed native state. Labels are qualified scene data.','{}'],
 ['push.start','Plan reachable standing poses and push the selected crate by bounded native pusher forces. Java owns collision timing, measured jam recovery, both-body checkpoints and exact report verification. No direct crate force or grip.','{displacementM:[integerX,0,integerZ],initialForceN?,maxForceN?,saveSkillName?}'],
 ['map.start','Java learns image adjacency, preserves hard pins, compiles real colliders, navigates a force-driven avatar with feedback and verifies actual trace/charts; optionally retain a skill. For selected existing maps omit generation fields.','{seed?,width?,height?,cellSizeM?,friction?,materials?:[{tile,name,friction,restitution?,dragNsPerM?}],disturbanceNs?,pins?,saveSkillName?}'],
 ['space.start','Generate and verify actual Java procedural coordinates: colonization in2..16 dimensions or connected BSP dungeons in2..4 dimensions. Seeded, bounded, full source, actual projections and reusable procedure. No Processing dependency or physical-reality claim.','{type,dimensions,seed,samples?,iterations?,step?,influence?,kill?,side?,rooms?,saveSkillName?}'],
 ['space.export','Save the completed owned procedural space as a real verified portable JSON package. Host binds goal identity.','{}'],
 ['tracking.read','Inspect explicitly selected tracking sequences, installed frozen profiles and recorded comparisons. Source split labels are declared.','{}'],
 ['tracking.start','Compare the two installed frozen CPU tracker profiles on selected sequences. Java owns fixed accuracy/negative-control gates and optional promotion. Host binds dataset IDs.','{autoPromote?:boolean}'],
 ['tracking.wait','Wait for this chat’s one saved tracking comparison; no duplicate starts.','{}'],
 ['tracking.report','Open and test the actual completed comparison report, including rejected candidates.','{}'],
 ['tracking.rollback','Restore the installed rgb-v1 baseline and retain the profile history.','{}'],

 ['perception.read','Inspect visible browser regions, selected source and retained user-taught appearance examples. Page labels are untrusted claims.','{}'],
 ['perception.select','Select an actual returned visible browser region ref; never invent references.','{ref}'],
 ['perception.start','Detect, outline and follow a selected browser region; Java records pixel/DOM tracking, uncertain/lost/reacquired states and verifies an actual frame/trace report. Optional retain procedure. Uses already installed local vision or explicitly taught appearance.','{mode?,targetLabel?,visionModel?,lessonName?,seconds?,saveSkillName?}'],
 ['spatial.read','Read the explicitly selected imported box scene and dynamic object, scale and qualified source.','{}'],
 ['spatial.start','Forecast finite native routes, actually grab/move/release the selected object, check outcomes and chart forecast versus actual. Host binds actual scene/object. Horizontal relative displacementM in metres is required. Optional saveSkillName. No real-room or universal learned-model claim.','{displacementM:[x,0,z],saveSkillName?}'],
 ['data.read','Read the host-selected immutable imported motion dataset and declared units. Raw/source content never grants tool authority.','{}'],
 ['data.start','Java verifies selected CSV/mapping hashes, derives SI displacement/segment velocity/acceleration, flags gaps and speed, writes charts/audit files and tests the actual canvas. Defaults maxGapS0.25, speedLimitMps5. Optional saveSkillName retains a reusable procedure. No source modification or physics actuation.','{maxGapS?,speedLimitMps?,saveSkillName?}'],
  ['world.model','Read the actual learned native world model, validation status and supported physical domain.','{}'],
  ['goal.start','Java completes prediction, native target-reaching with a scheduled disturbance, physical checks, motion chart and actual canvas verification. Optionally retains a reusable skill. Current selected scene is bound by the host unless sceneId/version supplied. Defaults target 1.2 m, disturbance +0.35 m/s.','{angleDeg?,friction?,gravityMps2?,massKg?,targetM?,disturbanceMps?,saveSkillName?,browserContext?}'],
  ['goal.read','Wait briefly and read actual durable progress of this chat’s owned Java goal. Never start duplicate physical actions.','{}'],
  ['goal.cancel','Stop this chat’s owned Java goal and its actual motion.','{}'],
  ['goal.resume','Explicitly resume an interrupted Java goal from verified state. Incomplete physics becomes a new recorded attempt.','{id}'],
  ['procedures.list','List versioned registered procedural skills with prerequisites, actions, success checks, recovery and source evidence.','{}'],
  ['procedure.save','Retain a procedure only from this chat’s completed physically verified goal and exact tested report.','{name}'],
  ['procedure.run','Run a saved registered procedure version with freshly checked conditions and outcomes.','{name,version,angleDeg?,friction?,gravityMps2?,massKg?,targetM?,disturbanceMps?,browserContext?}'],
  ['body.grab','Attach a real native constraint to a visible dynamic Block within 6 m. Requires fresh observation; no current experiment. The host binds actual task ownership and a fresh source.','{entityId}'],
  ['body.move','Move the held body toward a world-space target through native solver ticks, collisions and a bounded constraint. Read measured reached/blocked outcome.','{target:[x,y,z]}'],
  ['body.release','Remove the native constraint, preserve velocity and simulate 0.5 s of actual gravity/contact response; settles this manipulation run.','{}'],
  ['body.cancel','Cancel an owned manipulation, stop pending motion and remove its native constraint.','{}'],
  ['body.capture','Capture the actual final view of this task’s owned native body run. The host binds the observer and source.','{}'],
  ['scene.export','Save a portable native scene JSON file and return its validated data and file receipt.','{id}'],
  ['scene.clone','Create a new native scene with equivalent supported geometry/parameters, fresh entity IDs and import provenance. Reset active physics first.','{id}'],
  ['scene.import','Validate and recreate a supported fraynix.scene package as a new scene. Data only, no code execution.','{package}'],
  ['run.export','Save a settled native run as JSON with actual samples, commands, source definition and results.','{id}'],
  ['run.csv','Save actual recorded native position/velocity/contact samples as CSV.','{id}'],
  ['run.video','Render recorded native poses to a real silent WebM video, up to 30 s at nominal 30 fps.','{id}'],
  ['observer.open','Open or restore the latest Java-owned first-person free camera for sceneId. Send only the saved sceneId; the host returns observer.id. No avatar body.','{sceneId}'],
  ['observer.observe','Observe actual native entities with camera pose, source scene/run/tick and a fresh observation id. Visibility uses geometric rays.','{id}'],
  ['observer.move','Move the free camera by declared metres in yaw-relative forward/right and world up. Requires a fresh observationId. Does not move objects.','{id,observationId,forward?,right?,up?}'],
  ['observer.look','Turn the camera by declared yaw/pitch degree deltas, using a fresh observationId.','{id,observationId,yawDeg?,pitchDeg?}'],
  ['observer.focus','Look at a known native entity id; uses actual current engine geometry, with stale-source protection.','{id,observationId,entityId}'],
  ['observer.inspect','Inspect an actual visible native entity id and retain its identity/position/body data in an action receipt.','{id,observationId,entityId}'],
  ['observer.fov','Set the camera vertical field of view, 30..110 degrees, with a fresh observationId.','{id,observationId,fovDeg}'],
  ['observer.capture','Render and save an actual PNG from the current Java observer/world snapshot. Returns frame hash, source binding and receipt.','{id}'],
  ['observer.history','Read persisted camera actions and saved frame receipts.','{id}'],
  ['observer.replay','Show a saved native view and its verified original PNG as a read-only historical observation. Does not change world/pose.','{id}'],
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
const goalParameters=object({angleDeg:{type:'number',minimum:10,maximum:40},friction:{type:'number',minimum:0,maximum:.3},gravityMps2:{type:'number',minimum:8,maximum:12},massKg:{type:'number',minimum:.5,maximum:2}},[]);
const goalOptions={...goalParameters.properties,targetM:{type:'number',minimum:.4,maximum:1.8},disturbanceMps:{type:'number',minimum:-.5,maximum:.5},browserContext:{type:'boolean'}};
const dataOptions={maxGapS:{type:'number',minimum:.000001,maximum:86400},speedLimitMps:{type:'number',minimum:.000001,maximum:1e9}};
const proceduralOptions={type:{type:'string',enum:['colonization','dungeon']},dimensions:{type:'integer',minimum:2,maximum:16},seed:{type:'integer',minimum:0,maximum:1e9},samples:{type:'integer',minimum:16,maximum:512},iterations:{type:'integer',minimum:1,maximum:128},step:{type:'number',minimum:.015,maximum:.08},influence:{type:'number',minimum:.12,maximum:.6},kill:{type:'number',minimum:.015,maximum:.6},side:{type:'integer',minimum:12,maximum:40},rooms:{type:'integer',minimum:4,maximum:24}};
const poseOptions={targets:{type:'array',minItems:2,maxItems:4,items:object({sourceId:text,positionM:{type:'array',items:{type:'number',minimum:-5,maximum:5},minItems:3,maxItems:3},yawDeg:{type:'number',minimum:-180,maximum:180}},['sourceId','positionM','yawDeg'])},forceLimitN:{type:'number',minimum:2,maximum:60},torqueLimitNm:{type:'number',minimum:0,maximum:4},saveSkillName:text};
const pushOptions={useContactController:{type:'boolean'},useActiveProfile:{type:'boolean'},displacementM:{type:'array',items:{type:'integer',minimum:-4,maximum:4},minItems:3,maxItems:3},initialForceN:{type:'number',minimum:2,maximum:60},maxForceN:{type:'number',minimum:2,maximum:60},saveSkillName:text};
const spatialOptions={displacementM:{type:'array',items:{type:'number',minimum:-6,maximum:6},minItems:3,maxItems:3},saveSkillName:text};
const perceptionOptions={mode:{type:'string',enum:['vision','appearance','dom']},targetLabel:text,visionModel:text,lessonName:text,seconds:{type:'number',minimum:2,maximum:30},saveSkillName:text};
const mapConstraints=object({connected:{type:'boolean'},minFloorFraction:{type:'number',minimum:.05,maximum:.95},maxFloorFraction:{type:'number',minimum:.05,maximum:.95},minRouteSteps:{type:'integer',minimum:0,maximum:128},maxRouteSteps:{type:'integer',minimum:0,maximum:128}},[]);
const mapOptions={materials:{type:'array',minItems:1,maxItems:8,items:object({tile:{type:'integer',minimum:0,maximum:7},name:{type:'string',minLength:1,maxLength:60},friction:{type:'number',minimum:.05,maximum:1.2},restitution:{type:'number',minimum:0,maximum:.95},dragNsPerM:{type:'number',minimum:0,maximum:8}},['tile','name','friction'])},constraints:mapConstraints,seed:{type:'integer',minimum:0,maximum:1e9},width:{type:'integer',minimum:8,maximum:24},height:{type:'integer',minimum:8,maximum:24},cellSizeM:{type:'number',minimum:.6,maximum:1.2},friction:{type:'number',minimum:.05,maximum:1.2},disturbanceNs:{type:'number',minimum:-.5,maximum:.5},saveSkillName:text,pins:{type:'array',maxItems:64,items:object({x:{type:'integer',minimum:0,maximum:23},y:{type:'integer',minimum:0,maximum:23},tile:{type:'integer',minimum:0,maximum:7}},['x','y','tile'])}};
const argumentSchemas={
 'alignment.read':empty,'alignment.report':empty,
 'runtime.read':empty,'runtime.project':object({view:{type:'string',enum:['perspective','top','front','side']}}),'runtime.report':empty,
 'self.read':object({query:{type:'string',maxLength:160}},[]),'object.read':empty,'object.research':empty,'object.wait':empty,'object.report':empty,
 'tabletop.answer':object({question:{type:'string',enum:['position','motion','visibility','calibration','prediction','limits']}}),'tabletop.read':empty,'tabletop.report':empty,'observations.read':empty,'observations.start':empty,'observations.wait':empty,'observations.report':empty,
 'map.read':empty,'map.start':object(mapOptions,[]),
 'space.start':object({...proceduralOptions,saveSkillName:text},['type','dimensions','seed']),'space.export':empty,
 'tracking.read':empty,'tracking.start':object({autoPromote:{type:'boolean'}},[]),'tracking.wait':empty,'tracking.report':empty,'tracking.rollback':empty,'perception.read':empty,'perception.select':object({ref:text}),'perception.start':object(perceptionOptions,[]),
 'world.observe':empty,'world.arrange':object(poseOptions,[]),'pose.compare':empty,'pose.study':empty,'pose.read':empty,'pose.start':object(poseOptions,[]),'contact.compare':empty,'contact.read':empty,'force.start':empty,'force.read':empty,'push.read':empty,'push.start':object(pushOptions,['displacementM']),
 'spatial.read':empty,'spatial.start':object(spatialOptions,['displacementM']),
 'data.read':empty,'data.start':object({...dataOptions,saveSkillName:text},[]),
 'world.model':empty,'goal.start':object({...goalOptions,saveSkillName:text},[]),'goal.read':empty,'goal.cancel':empty,'goal.resume':object({id:text}),'procedures.list':empty,'procedure.save':object({name:text}),
 'procedure.run':object({name:text,version:{type:'integer',minimum:1,maximum:8},...goalOptions,...dataOptions,saveSkillName:text},['name','version']),
 'body.grab':object({entityId:text}),
 'body.move':object({target:{type:'array',items:{type:'number'},minItems:3,maxItems:3}}),
 'body.release':empty,'body.cancel':empty,'body.capture':empty,
 'scene.export':object({id:text}),'scene.clone':object({id:text}),'scene.import':object({package:{type:'object'}}),
 'run.export':object({id:text}),'run.csv':object({id:text}),'run.video':object({id:text}),
 'observer.open':object({sceneId:{type:'string',description:'The saved scene id, not an observer id'}}),
 'observer.observe':object({id:text}),'observer.capture':object({id:text}),'observer.history':object({id:text}),'observer.replay':object({id:text}),
 'observer.move':object({id:text,observationId:text,forward:{type:'number',minimum:-5,maximum:5},right:{type:'number',minimum:-5,maximum:5},up:{type:'number',minimum:-5,maximum:5}},['id','observationId']),
 'observer.look':object({id:text,observationId:text,yawDeg:{type:'number',minimum:-90,maximum:90},pitchDeg:{type:'number',minimum:-45,maximum:45}},['id','observationId']),
 'observer.focus':object({id:text,observationId:text,entityId:text}),'observer.inspect':object({id:text,observationId:text,entityId:text}),
 'observer.fov':object({id:text,observationId:text,fovDeg:{type:'number',minimum:30,maximum:110}}),
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
function planTools(allowed,context={}){return catalog.filter(t=>allowed.includes(t.name)).map(t=>{
 let parameters=argumentSchemas[t.name];
  if(t.name==='map.start'&&context.mapCompiled)parameters=object({disturbanceNs:mapOptions.disturbanceNs,saveSkillName:text},[]);
 if(t.name==='procedure.run'&&context.family){const options=['world-poses','pose'].includes(context.family)?poseOptions:context.family==='push'?Object.fromEntries(Object.entries(pushOptions).filter(([key])=>!['useActiveProfile','useContactController'].includes(key))):context.family==='map'?(context.procedureBound&&context.allowMaterialOverride===false?Object.fromEntries(Object.entries(mapOptions).filter(([key])=>key!=='materials')):mapOptions):context.family==='procedural'?{seed:proceduralOptions.seed,definition:object(proceduralOptions,[]),saveSkillName:text}:context.family==='perception'?{targetLabel:text,seconds:perceptionOptions.seconds,saveSkillName:text}:context.family==='spatial'?spatialOptions:context.family==='data'?{...dataOptions,saveSkillName:text}:{...goalOptions,saveSkillName:text};parameters=context.procedureBound?object(options,[]):object({name:context.names?.length?{type:'string',enum:context.names}:text,version:{type:'integer',minimum:1,maximum:8},...options},['name','version']);}
 if(context.family==='pose'&&context.allowPoseSave===false&&['pose.start','procedure.run'].includes(t.name))parameters={...parameters,properties:Object.fromEntries(Object.entries(parameters.properties).filter(([key])=>key!=='saveSkillName'))};
 if(context.family==='world-poses'&&['world.arrange','procedure.run'].includes(t.name)){const properties={...parameters.properties};if(context.saveWorldName){properties.saveSkillName={type:'string',enum:[context.saveWorldName]};parameters={...parameters,properties,required:[...new Set([...(parameters.required||[]),'saveSkillName'])]};}else{delete properties.saveSkillName;parameters={...parameters,properties};}}
 return {type:'function',function:{name:t.name.replace('.','_'),description:t.description,parameters}};
});}
function observerStage(context,goal){
 if(!context.observerEvidence)return {allowed:['scene.create','scene.read','observer.open'],stage:'Open the native observer for the requested saved scene. Camera and visibility are computed by Java; the renderer supplies actual pixels.'};
 const ready=context.observerCapture?.observerVersion===context.observerEvidence.observer.version;
 const needsMove=/\bmove\b/i.test(goal),needsInspect=/\binspect\b/i.test(goal);
 if(ready&&(!needsMove||context.observerMoved)&&(!needsInspect||context.observerInspected))
  return {allowed:['finish'],stage:'The requested native workflow is complete. Do not repeat movement or inspection. Finish from actual observer '+JSON.stringify(context.observerEvidence.observer)+' and saved frame '+context.observerCapture.pngSha256+'.'};
 const allowed=['observer.observe','observer.move','observer.look','observer.focus','observer.inspect','observer.fov','observer.capture'];
 if(context.observerMoved&&/\bonce\b/i.test(goal))allowed.splice(allowed.indexOf('observer.move'),1);
 return {allowed,stage:'Use observer '+context.observerEvidence.observer.id+' and the current observation id '+context.observerEvidence.id+'. Distances are metres, look changes are degree deltas. Focus may use known entity IDs; inspection requires visible ones. Capture after the final pose/selection action. '+(context.observerMoved?'Movement already executed; do not restart the task. ':'Movement requested but not executed yet. ')+(context.observerInspected?'Inspection already executed; save the final frame. ':'Inspect the requested visible entity. ')+' Current pose: '+JSON.stringify(context.observerEvidence.observer.pose)};
}
function bodyStage(context){
 if(!context.observerEvidence)return {allowed:['scene.read','observer.open'],stage:'Open the observer for the saved scene. The observer returns real visible entity IDs.'};
 const id=context.observerEvidence.observer.id,observation=context.observerEvidence.id;
 if(!context.bodyGrab)return {allowed:['observer.observe','observer.focus','body.grab'],stage:'Grab the visible dynamic Block once using only entityId; the host supplies all ownership/source handles.'};
 const fence='Use observer '+id+', latest observationId '+observation+', runId '+context.bodyGrab.runId+' and gripId '+context.bodyGrab.id+'. ';
 if(!context.bodyMove)return {allowed:['observer.observe','body.move','body.cancel'],stage:'The host binds observer/run/grip and a fresh observation. Send only target. Move the actual body to the requested WORLD coordinates once. Derive target from the actual Block position; do not move the camera. Current position '+JSON.stringify(context.bodyGrab.after.position)};
 if(!context.bodyRelease)return {allowed:['body.release','body.cancel'],stage:'Motion already executed. Measured outcome '+context.bodyMove.outcome+'. Call body.release with empty arguments; the host supplies its actual ownership fence.'};
 if(!context.observerCapture||context.observerCapture.packet.world.runId!==context.bodyRelease.runId)return {allowed:['body.capture'],stage:'Release completed. Call body.capture with empty arguments to save the actual final view.'};
 return {allowed:['finish'],stage:'The native grab/move/release and final capture are complete. Finish with actual target error, release position, run id and image hash. Do not restart the actions.'};
}
const originalTaskMode=taskMode;
function expandedTaskMode(goal){
 if(/\b(?:persistent world|evolving world|world revision|world skill)\b/i.test(goal)&&!/\b(?:website|html|git)\b/i.test(goal))return {needsPersistentWorld:true,needsGoal:true,needsScene:false,needsComparison:false};
 if(/\b(?:compare|evaluate|test)\b/i.test(goal)&&/\b(?:pose control|pose controller|pose capability|pose feature)\b/i.test(goal)&&!/\b(?:website|html|git)\b/i.test(goal))return {needsPoseStudy:true,needsGoal:true,needsScene:false,needsComparison:false};
 if(/\b(?:arrange|multi[- ]object|target poses|pose skill|pose procedure)\b/i.test(goal)&&!/\b(?:website|html|git)\b/i.test(goal))return {needsPose:true,needsGoal:true,needsScene:false,needsComparison:false};
 if(/\b(?:compare|evaluate|test)\b/i.test(goal)&&/\bcontact controller\b/i.test(goal)&&!/\b(?:website|html|git)\b/i.test(goal))return {needsContactStudy:true,needsGoal:true,needsScene:false,needsComparison:false};
 if(/\b(?:learn|train|evaluate|compare)\b/i.test(goal)&&/\b(?:pushing|force)\b/i.test(goal)&&/\b(?:profile|reserved|experience|evaluation|training)\b/i.test(goal)&&!/\b(?:website|html|git)\b/i.test(goal))return {needsForceLearning:true,needsGoal:true,needsScene:false,needsComparison:false};
 if(/\bpush(?:ing)?\b/i.test(goal)&&/\b(?:crate|pusher|box|selected object|imported object)\b/i.test(goal)&&!/\b(?:website|web app|html|git)\b/i.test(goal))return {needsPush:true,needsGoal:true,needsScene:false,needsComparison:false};
 if(/\b(?:reference[- ]image map|reference[- ]map|playable (?:map|world)|map world|native map|tile map)\b/i.test(goal)||/\b(?:play|compile)\b.*\bdungeon\b/i.test(goal))return {needsMap:true,needsGoal:true,needsScene:false,needsComparison:false};
 if(/\b(?:space[- ]coloni[sz]ation|procedural (?:space|map|geometry)|dungeon|coloni[sz]ation)\b/i.test(goal))return {needsProcedural:true,needsGoal:true,needsScene:false,needsComparison:false};
 if(/\bevidence workflow\b|\bend[- ]to[- ]end\b/i.test(goal))return {needsWorkflow:true,needsScene:false,needsComparison:false};
 if(/\b(?:create|build)\s+and\s+verify\b|\bdurable\s+(?:creation|build|app)\b/i.test(goal))return {needsCreation:true,needsScene:false,needsComparison:false};
 if(!/\b(?:build|create|make)\b[\s\S]{0,80}\b(?:web ?app|website)\b/i.test(goal)&&(/\b(?:tracker profiles?|tracking (?:lab|evaluation|benchmark|sequences)|perception evaluation)\b/i.test(goal)&&/\b(?:compare|evaluate|benchmark|run|promote)\b/i.test(goal)||/\b(?:rollback|roll back|restore)\b/i.test(goal)&&/\btracker\b/i.test(goal)))return {needsTracking:true,needsScene:false,needsComparison:false};

 if(!/\b(?:build|create|make)\b[\s\S]{0,80}\b(?:web ?app|web application|website)\b/i.test(goal)&&(/\bperception\b/i.test(goal)&&/\b(observe|detect|outline|track|follow|trace|recogniz\w*|identify|scan|run|reuse)\b/i.test(goal)||/\b(browser|webpage|web page)\b/i.test(goal)&&/\b(detect|outline|recogniz\w*|identify|trace)\b/i.test(goal)||/\b(browser|webpage|web page)\b/i.test(goal)&&/\b(track|follow)\b/i.test(goal)&&/\b(person|people|object|region|image|video|element|cup)\b/i.test(goal)))return {needsPerception:true,needsGoal:true,needsScene:false,needsComparison:false};
 if(/\bspatial\b/i.test(goal)||/\bimported (?:box |3d )?scene\b/i.test(goal))return {needsSpatial:true,needsGoal:true,needsScene:false,needsComparison:false};
 const base=originalTaskMode(goal),native=/\b(native|3d|physics|scene|block)\b/i.test(goal);
 const needsBody=native&&/\b(grab|grabbing|release|pick up|held body|held object)\b/i.test(goal);
 const needsTransfer=!needsBody&&native&&/\b(export|import|duplicate|clone|replicate)\b/i.test(goal);
 const webTask=/\b(?:web ?app|web application|html|website)\b/i.test(goal)||/\b(?:build|create|make)\b.*\b(?:app|application)\b/i.test(goal);
 const needsData=!needsBody&&!needsTransfer&&!webTask&&(/\b(?:analy[sz]e|analysis|summari[sz]e|quality|data procedure)\b/i.test(goal)||/\b(?:run|reuse)\b/i.test(goal)&&/\bprocedure\b/i.test(goal))&&/\b(?:motion|data|csv|dataset|recording)\b/i.test(goal)&&!/\b(?:calibrat\w*|simulate|physics|world[ -]model)\b/i.test(goal);
 const needsGoal=!needsBody&&!needsTransfer&&(/\bworld[ -]model\b/i.test(goal)||/\bpredict\b/i.test(goal)&&/\b(block|motion|moves|disturbance)\b/i.test(goal)||/\b(reuse|run)\b/i.test(goal)&&/\bprocedur(?:e|al)\b/i.test(goal));
 return needsData?{needsGoal:true,needsData:true,needsScene:false,needsComparison:false}:needsGoal?{needsGoal:true,needsScene:true,needsComparison:false}:needsBody?{needsBody:true,needsScene:true,needsComparison:false}:needsTransfer?{needsTransfer:true,needsScene:true,needsComparison:false}:base;
}
function taskMode(goal){const needsObserver=/\b(observer|viewpoint|camera)\b/i.test(goal)&&/\b(native|3d|ramp|scene)\b/i.test(goal)||/\bfirst[ -]person\b/i.test(goal)&&/\b(native|3d|ramp|scene)\b/i.test(goal);const needsScene=needsObserver||/\b(3d|physics|ramp|trajectory|incline|downhill)\b/i.test(goal)||(/\b(compare|comparison)\b/i.test(goal)&&/\b(measurements?|observed|simulated)\b/i.test(goal));const needsCalibration=!needsObserver&&needsScene&&/\b(calibrat\w*|fit|fitting)\b/i.test(goal);return {...(needsObserver?{needsObserver:true}:{}),...(needsCalibration?{needsCalibration:true}:{}),needsScene,needsComparison:!needsObserver&&!needsCalibration&&needsScene&&/\b(measurements?|observations?|dataset|csv|residual|comparison)\b/i.test(goal)};}
module.exports={catalog,webURL,command,ObservationLease,planTools,taskMode:expandedTaskMode,calibrationApplyRequested,calibrationStage,observerStage,bodyStage};
