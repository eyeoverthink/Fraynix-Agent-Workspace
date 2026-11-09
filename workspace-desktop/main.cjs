'use strict';
const {app,BrowserWindow,ipcMain,Menu,shell}=require('electron');
const {spawn}=require('node:child_process');
const fs=require('node:fs');const fsp=require('node:fs/promises');const path=require('node:path');const {randomBytes,randomUUID}=require('node:crypto');const readline=require('node:readline');
const {Surface}=require('./surfaces.cjs');const {catalog,command,planTools}=require('./policy.cjs');
const {failureClass,performControlled,observeProgress,trainController}=require('./controller.cjs');
const {replay,rehearse}=require('./controller-replay.cjs');
const {NetworkPolicy}=require('./network-policy.cjs');
const {verifyRendererSources}=require('./source-integrity.cjs');
const {connectRuntime042}=require('./runtime-connection042.cjs');
const {budget:javaBudget082,checkEffective:checkJavaBudget082}=require('./java-budget082.cjs');
const {Inputs100}=require('./inputs100.cjs');
const {createAgency043}=require('./agency043.cjs');const {AsyncLocalStorage}=require('node:async_hooks');const agencyScope043=new AsyncLocalStorage();let agency043;let pendingAgency043=0,agencyLaunchGeneration043=0;
const {createAgent042}=require('./agent042.cjs');let agent042,runtimeConnection042;
const {NativeExports}=require('./exports.cjs');
const {bodyReport}=require('./body-result.cjs');
const {createGoals}=require('./goals.cjs');
const {createCreation}=require('./creation.cjs');
const {createWorkflows}=require('./workflows.cjs');
const {BrowserPerception}=require('./browser-perception.cjs');
const {createTracking}=require('./tracking.cjs');
const {createObservations}=require('./observations.cjs');
const {createTabletop}=require('./tabletop.cjs');
const {ResearchWorker}=require('./research-worker.cjs');
const {createKnowledge}=require('./knowledge.cjs');
const {createRuntime040}=require('./spatial-runtime040.cjs');let runtime040;
const {createAlignment041}=require('./alignment041.cjs');let alignment041;
let knowledgeBridge,researchWorker,selfRegistered=false;const selfHostSession=randomUUID();
let tabletopBridge;
let observationBridge;
let currentPose,currentPersistentWorld;
let goalBridge,perceptionBridge,trackingBridge,creationBridge,workflowBridge;
const networkPolicy=new NetworkPolicy(process.env.FRAYNIX_WORKSPACE_OFFLINE==='1');
if(networkPolicy.enabled){app.commandLine.appendSwitch('disable-background-networking');app.commandLine.appendSwitch('disable-quic');}
const repo=path.resolve(__dirname,'..');
const project=path.resolve(process.env.FRAYNIX_WORKSPACE_PROJECT||path.join(app.getPath('home'),'.fraynix-agent-workspace','projects','default'));
const nativeExports=new NativeExports(project);
const inputs100=new Inputs100(api,path.join(project,'input-selection100.json'));
fs.mkdirSync(path.join(project,'desktop-profile'),{recursive:true});
app.setPath('userData',path.join(project,'desktop-profile'));app.setName('Fraynix Agent Workspace');
let engine,origin,token,window,browser,canvas,currentArtifact,state,run=null,gameProcess,closing=false,manualBusy=false;
let captureContext=null,learningJob=null;
let currentScene=null;
let currentDataset=null;
let currentSpatial=null;let currentPushPusher=null;
const pushSelection=()=>currentSpatial&&currentPushPusher?{...currentSpatial,pusherId:currentPushPusher}:null;
let currentMapSelection=null;
let currentObserverId=null;
let surfacesVisible={browser:true,canvas:true};const log=[];
function emit(type,value){if(window&&!window.isDestroyed())window.webContents.send('workspace:event',{type,value});}
function activity(kind,text,data={}){const value={at:new Date().toISOString(),kind,text,data};log.push(value);if(log.length>150)log.shift();emit('activity',value);return value;}
async function updateSelfHost(){
 if(!selfRegistered)return;
 let cameraActive=false;if(window&&!window.isDestroyed())try{cameraActive=await Promise.race([window.webContents.executeJavaScript('Boolean(window.tabletopView?.stream||window.metricView?.stream)'),new Promise(resolve=>setTimeout(()=>resolve(false),500))]);}catch{}
 const spatialState=await api('/api/runtime040/state',{}),alignmentState=await api('/api/alignment041/state',{});
 await api('/api/self/host',{alignmentRegistrationId:alignmentState.selectedRegistrationId||'',alignmentTrialId:alignmentState.selectedTrialId||'',spatialSource:spatialState.binding||{},spatialSelectedId:spatialState.selectedId||'',sessionId:selfHostSession,offline:networkPolicy.enabled,browserReady:!!browser&&!browser.view.webContents.isDestroyed()&&!browser.pending,canvasReady:!!canvas&&!canvas.view.webContents.isDestroyed()&&!canvas.pending,cameraActive,selectedObjectId:knowledgeBridge?.selected()?.id||'',agentActive:!!run||!!pendingAgency043||!!agency043?.busy()});
}
async function api(route,input){
  const agencyAuthority043=agencyScope043.getStore();
  const response=await fetch(origin+route,{method:input===undefined?'GET':'POST',headers:{Authorization:`Bearer ${token}`,'Content-Type':'application/json','X-Fraynix-Interface':selfHostSession,...(agencyAuthority043?{'X-Fraynix-Task':agencyAuthority043.taskId,'X-Fraynix-Effect':agencyAuthority043.nonce}:{})},body:input===undefined?undefined:JSON.stringify(input)});
  const data=await response.json();if(!response.ok)throw Error(data.error||`Engine HTTP ${response.status}`);return data;
}
async function refresh(){state={...await api('/api/state'),network:networkPolicy.status()};emit('state',state);return state;}
async function engineStart(){
  if(process.env.FRAYNIX_WORKSPACE_PERSISTENT==='1'||(!process.env.FRAYNIX_WORKSPACE_TEST&&process.env.FRAYNIX_WORKSPACE_PERSISTENT!=='0')){runtimeConnection042=await connectRuntime042({repo,project,sessionId:selfHostSession,onState:value=>emit('agent042',value),onError:e=>activity('error',e.message)});origin=runtimeConnection042.origin;token=runtimeConnection042.token;return;}
  await fsp.mkdir(project,{recursive:true});token=randomBytes(32).toString('hex');
  const java=process.env.JAVA_HOME?path.join(process.env.JAVA_HOME,'bin','java.exe'):'java';
  const lib=path.join(repo,'workspace-engine','build','install','workspace-engine','lib','*');
  const engineLog=fs.openSync(path.join(project,'engine.log'),'a');
  const budget082=javaBudget082();
  engine=spawn(java,[...budget082.args,'-cp',lib,'fraymus.workspace.WorkspaceServer'],{cwd:repo,windowsHide:true,env:{...process.env,FRAYNIX_WORKSPACE_PROJECT:project,FRAYNIX_WORKSPACE_TOKEN:token},stdio:['pipe','pipe',engineLog]});
  engine.on('error',e=>activity('error',e.message));
  engine.on('exit',code=>{if(!closing)activity('error',`Java engine stopped (${code})`);});
  await new Promise((resolve,reject)=>{
    const deadline=setTimeout(()=>reject(Error('Java engine did not start within 30 seconds. Build workspace-engine first.')),30000);
    const lines=readline.createInterface({input:engine.stdout});lines.on('line',line=>{fs.appendFileSync(path.join(project,'engine.log'),line+'\n');try{const event=JSON.parse(line);if(event.event==='workspace-ready'){origin=`http://127.0.0.1:${event.port}`;clearTimeout(deadline);resolve();}}catch{}});
    engine.once('error',reject);engine.once('exit',code=>{clearTimeout(deadline);reject(Error(`Engine exited before startup (${code})`));});
  });
  if(budget082.explicit)checkJavaBudget082(budget082,await api('/api/resources082/state',{}));
}
async function openArtifact(artifact){currentArtifact=artifact;if(run)run.created=true;emit('artifact',artifact);await refresh();await canvas.navigate(`${origin}/artifact/${artifact.id}/${artifact.version}/index.html`);return {id:artifact.id,title:artifact.title,version:artifact.version,files:artifact.files,url:canvas.view.webContents.getURL()};}
function calibrationView(job){
  const result=Object.fromEntries(['id','status','phase','reason','completedNativeOperations','selection','evaluation','application','bindingsCurrent','canApply','canRollback'].filter(k=>k in job).map(k=>[k,structuredClone(job[k])]));
  for(const key of ['baselineTraining','fittedTraining','validationBaseline','validationFitted'])if(job[key]){result[key]=structuredClone(job[key]);delete result[key].points;}
  result.definition=structuredClone(job.definition);
  for(const key of ['trainingScene','validationScene']){delete result.definition[key].objects;delete result.definition[key].entityIds;}
  result.trialCount=job.trials.length;
  return result;
}
async function observerSurface(method,value){
  if(!window||window.isDestroyed())throw Error('The native view is unavailable');
  for(let i=0;i<30;i++){if(await window.webContents.executeJavaScript('Boolean(window.nativeObserver)'))break;await new Promise(resolve=>setTimeout(resolve,50));}
  return window.webContents.executeJavaScript('window.nativeObserver['+JSON.stringify(method)+']('+JSON.stringify(value??null)+')',true);
}
async function observeNative(id){
  const viewport=await observerSurface('viewport');const packet=await api('/api/observer/observe',{id,...viewport});await observerSurface('show',packet);return packet;
}
function observerSummary(packet){
  const {id,observer,viewport,visibleEntities,world,worldSha256,origin,readOnly,visibilityMethod}=packet;
  return {id,observer,viewport,visibleEntities,worldSha256,origin,readOnly,visibilityMethod,world:{sceneId:world.sceneId,sceneVersion:world.sceneVersion,authoredVersion:world.authoredVersion,runId:world.runId,tick:world.tick,timeS:world.timeS,source:world.source,status:world.status,grip:world.grip,actuatorRevision:world.actuatorRevision,knownEntities:world.objects.map(o=>({id:o.id,label:o.label,position:o.position}))}};
}
async function executeRaw(tool,args={}){
  if(['browser.navigate','browser.click','browser.type'].includes(tool)){const active=(await api('/api/goals/list',{})).goals.find(g=>['running','waiting-host'].includes(g.status));if(active?.procedure==='browser-perception-v1')throw Error('The Java perception goal owns the browser. Stop it before navigating or acting.');}
  if((run?.needsObserver||run?.needsBody)&&tool.startsWith('observer.')&&run.ownedObserverId){if(tool==='observer.open')throw Error('This task already owns an observer; continue its actions');if(args.id!==run.ownedObserverId)throw Error('Continue the observer owned by this task');}
  if(run?.needsCalibration&&tool.startsWith('calibration.')&&run.ownedCalibrationId){
    if(tool==='calibration.start')throw Error('This task already owns a calibration; continue its actual lifecycle');
    if(args.id!==run.ownedCalibrationId)throw Error('Continue the calibration owned by this task');
  }
  let result;
  switch(tool){
    case 'screen.read':case 'screen.report':result=await agency043.action(tool,args);break;
    case 'agent.report':if((await api('/api/goals/list',{})).goals.some(g=>['running','waiting-host'].includes(g.status))||(await api('/api/workflows/list',{})).workflows.some(g=>['running','waiting-host','queued'].includes(g.status)))throw Error('Finish or stop the active goal before opening another canvas report');
    case 'agent.read':result=await agent042.action(tool,args,captureContext||{});break;
    case 'alignment.read':case 'alignment.report':result=await alignment041.action(tool,args,captureContext||{});break;
    case 'runtime.read':case 'runtime.project':case 'runtime.report':result=await runtime040.action(tool,args,captureContext||{});break;
    case 'self.read':case 'object.read':case 'object.research':case 'object.wait':case 'object.report':await updateSelfHost();result=await knowledgeBridge.action(tool,args,captureContext||{});break;
    case 'tabletop.read':case 'tabletop.answer':case 'tabletop.report':result=await tabletopBridge.action(tool,args,captureContext);break;
    case 'observations.read':case 'observations.start':case 'observations.wait':case 'observations.report':result=await observationBridge.action(tool,args,captureContext);break;
    case 'tracking.read':case 'tracking.start':case 'tracking.wait':case 'tracking.report':case 'tracking.rollback':result=await trackingBridge.action(tool,args,captureContext);break;
    case 'experiment.rehearse':case 'construction.start':case 'creation.start':case 'world.observe':case 'world.arrange':case 'pose.compare':case 'pose.study':case 'pose.read':case 'pose.start':case 'contact.compare':case 'contact.read':case 'force.read':case 'force.start':case 'push.read':case 'push.start':case 'map.read':case 'map.start':case 'space.start':case 'space.export':case 'perception.read':case 'perception.select':case 'perception.start':case 'spatial.read':case 'spatial.start':case 'data.read':case 'data.start':case 'world.model':case 'goal.start':case 'goal.read':case 'goal.cancel':case 'goal.resume':case 'procedures.list':case 'procedure.save':case 'procedure.run':result=await goalBridge.execute(tool,args,captureContext);break;
    case 'workflow.start':case 'workflow.reuse':result=await workflowBridge.execute(tool,args,captureContext);break;
    case 'body.capture':{if(!run?.needsBody||!run.ownedObserverId)throw Error('Body capture requires an owned native body task');result=await executeRaw('observer.capture',{id:run.ownedObserverId});break;}
    case 'body.grab':case 'body.move':case 'body.release':case 'body.cancel':{
      if(run?.needsBody){
        if(!run.ownedObserverId)throw Error('Open the task observer first');
        const allowed=tool==='body.grab'?['entityId']:tool==='body.move'?['target']:[];
        if(Object.keys(args).some(k=>!allowed.includes(k)))throw Error('Send only semantic body fields; the host owns observer/run/grip identifiers');
        const packet=await observeNative(run.ownedObserverId);args={...args,id:run.ownedObserverId};
        if(tool!=='body.cancel')args.observationId=packet.id;
        if(tool!=='body.grab'){args.runId=run.bodyGrab.runId;if(tool!=='body.cancel')args.gripId=run.bodyGrab.id;}
      }
      let receipt=await api('/api/body/action',{...args,action:tool.split('.')[1],operationId:randomUUID()});
      if(run)run.ownedBodyRunId=receipt.runId;
      const deadline=Date.now()+12000;
      while(receipt.status==='pending'){
        if(Date.now()>deadline)throw Error('Native motion did not settle within its time budget; cancel the owned run');
        await new Promise(r=>setTimeout(r,60));receipt=await api('/api/body/operation',{operationId:receipt.id});
      }
      emit('experiment',await api('/api/scene/snapshot',{}));
      if(receipt.status!=='completed')throw Error('Body operation '+receipt.status+': '+(receipt.error||'native operation did not complete'));
      const packet=await observeNative(args.id);result={...observerSummary(packet),receipt};
      if(run){run.observerEvidence=observerSummary(packet);if(tool==='body.grab')run.bodyGrab=receipt;if(tool==='body.move')run.bodyMove=receipt;if(tool==='body.release')run.bodyRelease=receipt;}break;
    }
    case 'scene.export':{const file=await api('/api/scene/export',args);result={package:file,file:await nativeExports.scene(file)};if(run)run.transferEvidence=result.file;emit('nativeExport',result.file);break;}
    case 'scene.import':case 'scene.clone':{
      const input=tool==='scene.clone'?{package:await api('/api/scene/export',args)}:args;
      result=await api('/api/scene/import',input);currentScene=result.scene;emit('scene',currentScene);if(run)run.transferEvidence=result.receipt;await refresh();break;
    }
    case 'run.export':case 'run.csv':case 'run.video':{
      const file=await api('/api/experiment/export',args);
      if(tool==='run.video'){const rendered=await observerSurface('video',file.run);result=await nativeExports.video(file,rendered);emit('nativeVideo',{base64:rendered.base64,mime:rendered.mime});}else result=tool==='run.export'?await nativeExports.run(file):await nativeExports.csv(file);
      if(run)run.transferEvidence=result;emit('nativeExport',result);break;
    }
    case 'observer.open':{
      const observer=await api('/api/observer/open',args);currentObserverId=observer.id;currentScene=await api('/api/scene/read',{id:observer.sceneId});
      await observerSurface('openScene',currentScene);const packet=await observeNative(observer.id);result=observerSummary(packet);if(run){run.observerEvidence=result;run.ownedObserverId=observer.id;}await refresh();break;
    }
    case 'observer.observe':{
      const packet=await observeNative(args.id);currentObserverId=args.id;result=observerSummary(packet);if(run)run.observerEvidence=result;break;
    }
    case 'observer.move':case 'observer.look':case 'observer.focus':case 'observer.inspect':case 'observer.fov':{
      const applied=await api('/api/observer/action',{...args,action:tool.split('.')[1],operationId:randomUUID()});
      const packet=await observeNative(args.id);result={...observerSummary(packet),receipt:applied.receipt};
      if(run){run.observerEvidence=observerSummary(packet);if(tool==='observer.move')run.observerMoved=true;if(tool==='observer.inspect')run.observerInspected=true;}break;
    }
    case 'observer.capture':{
      const packet=await observeNative(args.id);const pixels=await observerSurface('capture',packet);
      if(pixels.observationId!==packet.id||pixels.worldSha256!==packet.worldSha256)throw Error('Native renderer returned a different source');
      const capture=await api('/api/observer/capture',{...pixels,operationId:randomUUID()});emit('observerCapture',{capture,pngBase64:pixels.pngBase64});
      result={id:capture.id,observerId:capture.observerId,observerVersion:capture.observerVersion,pngSha256:capture.pngSha256,bytes:capture.bytes,packet:observerSummary(packet)};
      if(run){run.observerEvidence=observerSummary(packet);run.observerCapture=result;}break;
    }
    case 'observer.history':result=await api('/api/observer/history',args);break;
    case 'observer.replay':{
      const replay=await api('/api/observer/replay',args);currentScene=await api('/api/scene/read',{id:replay.packet.world.sceneId});await observerSurface('openScene',currentScene);await observerSurface('show',replay.packet);emit('observerCapture',replay);
      result={captureId:replay.capture.id,pngSha256:replay.capture.pngSha256,packet:observerSummary(replay.packet)};break;
    }
    case 'calibration.start':{
      const job=await api('/api/calibration/start',args);if(run){run.ownedCalibrationId=job.id;run.calibrationEvidence=job;}result=calibrationView(job);await refresh();break;
    }
    case 'calibration.read':{
      let job=await api('/api/calibration/read',args);
      if(['running','queued'].includes(job.status)){await new Promise(resolve=>setTimeout(resolve,800));job=await api('/api/calibration/read',args);}
      if(run)run.calibrationEvidence=job;result=calibrationView(job);break;
    }
    case 'calibration.cancel':case 'calibration.resume':case 'calibration.apply':case 'calibration.rollback':{
      const action=tool.split('.')[1];const job=await api('/api/calibration/'+action,args);
      if(run){run.calibrationEvidence=job;if(action==='resume')run.ownedCalibrationId=job.id;}
      if(['apply','rollback'].includes(action)){currentScene=await api('/api/scene/read',{id:job.definition.trainingScene.id});emit('scene',currentScene);}
      result=calibrationView(job);await refresh();break;
    }
    case 'calibration.report':{
      const job=await api('/api/calibration/read',args);await openArtifact(await api('/api/calibration/report',args));emit('view','preview');
      const receipt=await execute('canvas.test',{steps:[{selector:'#verdict',expectText:job.evaluation.accepted?'Accepted by declared gates':'Rejected by declared gates'},{selector:'svg',expectCount:2},{selector:'tbody tr',expectCount:job.trials.length}]});
      if(run){run.calibrationEvidence=job;run.calibrationReportId=job.id;}result={id:currentArtifact.id,version:currentArtifact.version,calibrationId:job.id,reportTest:receipt};break;
    }
    case 'dataset.import':{const d=await api('/api/measurement/import',args);result={...d};delete result.csv;delete result.rows;await refresh();break;}
    case 'dataset.read':{const d=await api('/api/measurement/read',args);result={...d,rowCount:d.rows.length,rows:d.rows.slice(0,12)};delete result.csv;break;}
    case 'experiment.compare':{result=await api('/api/measurement/compare',args);if(run)run.comparisonEvidence={id:result.id,runId:result.runId,sceneId:result.sceneId,sceneVersion:result.sceneVersion,passed:result.passed};delete result.points;delete result.definition;await refresh();break;}
    case 'comparison.report':{
      const comparison=await api('/api/measurement/comparison',args);await openArtifact(await api('/api/measurement/report',args));emit('view','preview');
      const receipt=await execute('canvas.test',{steps:[{selector:'#comparison-result',expectText:comparison.passed?'Within declared tolerance':'Outside declared tolerance'},{selector:'#comparison-rows tr',expectCount:comparison.count},{selector:'#source-kind',expectText:comparison.sourceKind}]});
      if(run)run.reportComparisonId=comparison.id;result={id:currentArtifact.id,version:currentArtifact.version,comparisonId:comparison.id,reportTest:receipt};break;
    }
    case 'scene.create':result=await api('/api/scene/save',args);currentScene=result;emit('scene',result);await refresh();break;
    case 'scene.read':result=await api('/api/scene/read',args);currentScene=result;emit('scene',result);break;
    case 'scene.update':result=await api('/api/scene/save',args);currentScene=result;if(run)run.sceneEvidence=null;emit('scene',result);await refresh();break;
    case 'experiment.run':{
      const receipt=await api('/api/experiment/run',args);result={id:receipt.id,sceneId:receipt.sceneId,sceneVersion:receipt.sceneVersion,status:receipt.status,backend:receipt.backend,dt:receipt.dt,final:receipt.final,evaluation:receipt.evaluation};
      if(run)run.sceneEvidence=result;emit('experiment',await api('/api/scene/snapshot',{}));break;
    }
    case 'experiment.reset':result=await api('/api/experiment/control',{action:'reset'});if(run)run.sceneEvidence=null;emit('experiment',result);break;
    case 'browser.navigate':result=await browser.navigate(args.url);break;
    case 'browser.observe':result=await browser.observe();break;
    case 'browser.click':result=await browser.act(args,'click');break;
    case 'browser.type':result=await browser.act(args,'type');break;
    case 'canvas.write':result=await openArtifact(await api('/api/artifact/write',args));break;
    case 'canvas.read':result=await api('/api/artifact/read',args);break;
    case 'canvas.open':result=await openArtifact(await api('/api/artifact/read',args));break;
    case 'canvas.observe':if(!currentArtifact)throw Error('Create or open an artifact first');result=await canvas.observe();break;
    case 'canvas.test':{
      if(!currentArtifact)throw Error('Open an artifact first');
      await canvas.navigate(`${origin}/artifact/${currentArtifact.id}/${currentArtifact.version}/index.html`);
      const checks=await canvas.test(args.steps,agencyScope043.getStore()?agency043.onTestStep:undefined);
      result=await api('/api/artifact/test',{id:currentArtifact.id,version:currentArtifact.version,steps:args.steps,checks});currentArtifact.test=result;emit('artifact',currentArtifact);break;
    }
    case 'artifacts.list':result=(await refresh()).artifacts;break;
    case 'skills.list':result=(await refresh()).skills;break;
    case 'skill.save':if(!currentArtifact)throw Error('Open an artifact first');result=await api('/api/skill/save',{...args,id:currentArtifact.id});await refresh();break;
    case 'skill.run':{
      const skill=await api('/api/skill/read',args);await execute('canvas.write',{title:skill.title+' · '+new Date().toLocaleTimeString(),files:skill.files,sources:skill.sources});
      result=await execute('canvas.test',{steps:skill.tests});break;
    }
    case 'memory.search':result=await api('/api/host/skill',{name:'memory.search',args});break;
    default:throw Error(`Unknown executable tool: ${tool}`);
  }
  activity('tool',tool,{args:tool==='canvas.write'?{title:args.title,files:Object.keys(args.files)}:tool==='dataset.import'?{title:args.title,filename:args.filename,sourceKind:args.sourceKind}:args,result:JSON.stringify(result).slice(0,1400)});
  const recordedArgs=tool==='canvas.write'?{...args,files:Object.keys(args.files)}:tool==='dataset.import'?{title:args.title,filename:args.filename,sourceKind:args.sourceKind,sourceSha256:result.sourceSha256}:args;
  const recordedResult=tool==='canvas.read'?{id:result.id,title:result.title,version:result.version,files:result.files,test:result.test}:result;
  await api('/api/event',{kind:'tool_completed',data:{tool,args:recordedArgs,result:recordedResult}});
  return result;
}
async function episodeStep(context,step){if(context?.episodeStarted)await api('/api/learning/step',{id:context.id,step});}
async function execute(tool,args={}){
  const context=captureContext,started=Date.now();let result;
  try{result=await executeRaw(tool,args);}
  catch(error){if(selfRegistered)await api('/api/self/receipt',{tool,ok:false,reason:error.message}).catch(()=>{});await episodeStep(context,{tool,ok:false,failure:failureClass(error),elapsedMs:Date.now()-started});throw error;}
  const repeated=observeProgress(context,tool,result);
  const step={tool,ok:true,failure:tool==='canvas.test'&&!result.passed?'test-failed':repeated&&context?.needsArtifact&&!context.created?'repeated-observation':'none',elapsedMs:Date.now()-started};
  if(result?.observation){step.observationId=result.observation;step.controlCount=result.controls?.length||0;}
  if(tool==='canvas.write'||tool==='canvas.read'){step.artifactId=result.id;step.artifactVersion=result.version;}
  if(tool==='canvas.test'){step.artifactId=currentArtifact.id;step.artifactVersion=currentArtifact.version;step.testPassed=result.passed;}
  if(context?.transition&&context.transition!=='none')step.transition=context.transition;
  if(selfRegistered)await api('/api/self/receipt',{tool,ok:true,reason:'Actual host execution returned'}).catch(()=>{});
  await episodeStep(context,step);return result;
}
// Retained manual controller rehearsal/test seam. Chat decisions are owned by Java.
async function controlledExecute(next,policy){
 const result=await performControlled(next,policy,browser.latest,async(tool,args,transition)=>{if(captureContext)captureContext.transition=transition;return execute(tool,args);});
 if(result.decision.transition!=='none')activity('learning','Learned controller requested a fresh observation',{policyVersion:policy.version,transition:result.decision.transition});
 return result;
}
async function learnFromEpisodes({proof=false,automatic=false}={}){
  if(run||learningJob)throw Error('Finish the current task before evaluating learning');
  const controller=new AbortController();learningJob=controller;
  const checkStop=()=>{if(controller.signal.aborted)throw Error('Learning cancelled');};
  let proofEpisode,candidateId;
  try{
    if(proof){
      emit('run',{active:true,phase:'Recording a real controller failure'});
      proofEpisode=randomUUID();await api('/api/learning/begin',{id:proofEpisode,taskKind:'controller-rehearsal',goal:'Independent stale control rehearsal'});
      const failed=await rehearse(controller.signal);checkStop();
      if(failed.passed||failureClass(failed.error)!=='stale-observation')throw Error('Rehearsal did not produce the expected actual stale-reference rejection');
      await api('/api/learning/step',{id:proofEpisode,step:{tool:'browser.click',ok:false,failure:'stale-observation'}});
      await api('/api/learning/end',{id:proofEpisode,status:'failed'});proofEpisode=null;
      activity('learning','Recorded a real rejected stale browser action in a separate fixture');
    }
    const snapshot=(await refresh()).learning;
    if(!snapshot.episodes.some(e=>e.status!=='running'&&e.steps.some(s=>['stale-observation','repeated-observation'].includes(s.failure))))return {unchanged:true};
    emit('run',{active:true,phase:'Learning from recorded outcomes'});
    const summaries=snapshot.episodes.map(e=>({id:e.id,status:e.status,steps:e.steps.filter(s=>['stale-observation','repeated-observation'].includes(s.failure)).slice(-16).map(s=>({tool:s.tool,failure:s.failure}))}));
    const learned=await trainController({activePolicy:snapshot.activePolicy,controllerMemory:snapshot.controllerMemory,episodes:summaries},{signal:controller.signal});checkStop();
    if(!learned.proposal){if(!automatic)activity('learning','Current controller already covers the recorded failures');return {unchanged:true};}
    if(snapshot.candidates.some(c=>c.parentGeneration===snapshot.generation&&c.policy.refreshStale===learned.proposal.policy.refreshStale&&c.policy.observationLimit===learned.proposal.policy.observationLimit&&['promoted','rejected'].includes(c.status))){if(!automatic)activity('learning','This candidate was already evaluated for the active parent; no duplicate replay');return {unchanged:true};}
    const candidate=await api('/api/learning/propose',{parentVersion:snapshot.activePolicy.version,parentGeneration:snapshot.generation,...learned.proposal});
    candidateId=candidate.id;
    activity('learning',`Candidate controller v${candidate.policy.version} proposed from ${candidate.episodes.length} recorded episode(s)`,{candidateId:candidate.id,policy:candidate.policy,prototypeUpdates:candidate.controllerMemory?.totalUpdates});await refresh();checkStop();
    const report=await replay(snapshot.activePolicy,candidate.policy,{signal:controller.signal,onProgress:id=>{emit('run',{active:true,phase:'Independent replay: '+id});activity('replay',id);}});checkStop();
    const result=await api('/api/learning/evaluate',{candidateId:candidate.id,...report});
    candidateId=null;
    activity('learning',result.accepted?`Promoted controller v${candidate.policy.version}: ${result.baselinePassed}/8 â†’ ${result.candidatePassed}/8 replay cases passed`:'Candidate rejected: no verified gain or a regression',{evaluationId:result.id,baselineFailures:result.baselineFailures,candidateFailures:result.candidateFailures,regression:result.regression});
    return result;
  }catch(error){if(controller.signal.aborted)throw Error('Learning cancelled');throw error;}
  finally{
    if(candidateId)await api('/api/learning/cancel',{id:candidateId,status:controller.signal.aborted?'cancelled':'failed'}).catch(()=>{});
    if(proofEpisode)await api('/api/learning/end',{id:proofEpisode,status:controller.signal.aborted?'cancelled':'failed'}).catch(()=>{});
    learningJob=null;emit('run',{active:false});if(!closing)await refresh();
  }
}
// Presentation of Java-owned facts only. Never selects a tool, goal stage or skill.
async function mission044(id){
 const g=await api('/api/agency043/read',{id}),c=g.context044||{},task={};
 for(const k of ['id','goal','model','mode','family044','status','reason','turn','failures','resumes','generation','createdAt','finishedAt','outcome','savedRecipe','usedRecipe','summary','timing052','diagnostics052'])if(g[k]!==undefined)task[k]=g[k];
 task.frames=g.frames?.length||0;task.history=(g.history||[]).slice(-12).map(r=>({kind:r.kind,at:r.at,sha256:r.sha256,tool:r.detail?.tool,reason:r.detail?.reason,verified:r.detail?.verified}));
 const sources=Object.entries(g.bindings044||{}).map(([kind,b])=>({kind,...(Array.isArray(b)?{ids:b}:{...Object.fromEntries(['id','datasetId','sceneId','worldId','objectId','pusherId','mapId','referenceId','sourceArtifactId','sourceVersion','sourceFile','version','expectedVersion','expectedRevision','expectedHash','sourceSha256','title','sourceKind'].filter(k=>b?.[k]!==undefined).map(k=>[k,b[k]]))})}));
 if(g.initialArtifact)sources.push({kind:'initial artifact',...g.initialArtifact});
 let child=null;if(c.ownedGoalId)child=await api('/api/goals/read',{id:c.ownedGoalId});else if(c.ownedWorkflowId)child=await api('/api/workflows/read',{id:c.ownedWorkflowId});
 const result=g.report044?.artifact||g.completion044?.javaGoal?.report||g.completion044?.javaWorkflow?.report||g.report||g.artifact||child?.report||null;
 return{task,sources,evidence:g.completion044||{},child:child?Object.fromEntries(['id','status','step','reason','procedure','metrics','counterfactual045','savedSkill','draftNumber','resumes','attempt','report','currentChild','graph','graphSha256','stages','diagnostics','worldBinding'].filter(k=>child[k]!==undefined).map(k=>[k,child[k]])):null,result,skill:g.savedRecipe||g.usedRecipe||g.completion044?.javaGoal?.savedSkill||g.completion044?.javaWorkflow?.savedSkill||child?.savedSkill||null,scope:'Actual retained Java task and source selections. A past result is not a new camera observation or proof of improved model weights.'};
}
function creationSelection(){return inputs100.binding();}
function bindings044(){const b={scene:currentScene,dataset:currentDataset,spatial:currentSpatial,pose:currentPose,world:currentPersistentWorld,push:pushSelection(),mapping:currentMapSelection,creation:creationSelection(),knowledge:knowledgeBridge?.selected(),tabletop:tabletopBridge?.selected(),observations:observationBridge?.selection(),metric:observationBridge?.metricSelection?.(),tracking:trackingBridge?.selected()};return Object.fromEntries(Object.entries(b).filter(([,v])=>v!=null));}
async function startAgent(goal,model,extra={}){
 if(run||learningJob||manualBusy||pendingAgency043)throw Error('Finish or stop the current operation first');if(typeof goal!=='string'||!goal.trim()||goal.length>8000)throw Error('Enter a request up to8000characters');
 const generation=agencyLaunchGeneration043;pendingAgency043++;emit('run',{active:true,goal,model,phase:'Java is admitting and routing the request'});
 try{await inputs100.beforeStart();const settings=await window.webContents.executeJavaScript('window.agency043View?.settings()||({visionModel:""})');if(generation!==agencyLaunchGeneration043)return{stopped:true};await updateSelfHost();if(generation!==agencyLaunchGeneration043)return{stopped:true};return await agency043.begin({goal,model,visionModel:settings.visionModel||'',artifactId:currentArtifact?.id||'',bindings:bindings044(),...extra});}finally{pendingAgency043--;if(!agency043.busy())emit('run',{active:false});}
}
function startCreationAgent(goal,model,options={}){return startAgent(goal,model,{family:'creation',options});}
function startWorkflowAgent(goal,model,options={}){return startAgent(goal,model,{family:'workflow',options});}
async function stopAgent(){agencyLaunchGeneration043++;if(pendingAgency043||agency043?.busy()){await agency043.cancel();emit('run',{active:false});return{stopped:true};}if(learningJob){learningJob.abort();activity('stopped','Learning evaluation cancelled; unfinished candidates remain unpromoted');}if(!run)return;run.cancelled=true;await knowledgeBridge?.cancelOwned(run);browser.cancelPending();canvas.cancelPending();const active=run.requestId;if(active)await api('/api/cancel',{requestId:active});if(run.ownedBodyRunId)await api('/api/body/cancel',{id:run.ownedObserverId,runId:run.ownedBodyRunId});if(run.ownedCalibrationId)await api('/api/calibration/cancel',{id:run.ownedCalibrationId});if(run.ownedWorkflowId)await workflowBridge.execute('workflow.cancel',{id:run.ownedWorkflowId});if(run.ownedGoalId)await goalBridge.cancelGeneration(run.ownedGoalId);if(run.ownedGoalId)await api('/api/goals/cancel',{id:run.ownedGoalId});if(run.observationRunId)await api('/api/observations/stop',{id:run.observationRunId}).catch(()=>{});if(run.trackingJobId)await api('/api/tracking/cancel',{id:run.trackingJobId});activity('stopped','Takeover requested; no further agent actions will run');}
const starter=`<!doctype html><html><head><meta charset="utf-8"><meta name="viewport" content="width=device-width,initial-scale=1"><style>body{background:#edf6f2;color:#173a35;font:18px system-ui;display:grid;place-items:center;min-height:90vh}main{background:white;padding:36px;border:1px solid #b9d1c7;border-radius:20px;max-width:540px}h1{font-size:36px}output{font-size:64px;display:block;margin:20px 0}button{font:inherit;padding:12px 18px;border:0;border-radius:10px;background:#165e4d;color:white;margin-right:8px}p{line-height:1.5}</style></head><body><main><p>FRAYNIX / LIVE CREATION CANVAS</p><h1>Your first working artifact</h1><p>This is an actual interactive application. Change its source, ask the agent to improve it, or test its behavior.</p><output id="count">0</output><button id="add">Add one</button><button id="reset">Reset</button><script>let count=0;document.querySelector('#add').onclick=()=>document.querySelector('#count').textContent=String(++count);document.querySelector('#reset').onclick=()=>{count=0;document.querySelector('#count').textContent='0'};</script></main></body></html>`;
async function guidedProofRaw(){
  if(run)throw Error('Stop the agent before running the guided proof');
  await execute('browser.navigate',{url:origin+'/welcome'});let observation=await execute('browser.observe');
  const input=observation.controls.find(c=>c.label.startsWith('Experiment name'));
  observation=await execute('browser.type',{observation:observation.observation,ref:input.ref,text:'Verified workspace'});
  const button=observation.controls.find(c=>c.label==='Record experiment');
  const changed=await execute('browser.click',{observation:observation.observation,ref:button.ref});
  if(!changed.text.includes('Recorded: Verified workspace'))throw Error('The live browser did not reflect the action');
  await execute('canvas.write',{title:'Verified counter',files:{'index.html':starter},sources:[origin+'/welcome']});
  const proof=await execute('canvas.test',{steps:[{selector:'#count',expectText:'0'},{selector:'#add',action:'click'},{selector:'#count',expectText:'1'},{selector:'#reset',action:'click'},{selector:'#count',expectText:'0'}]});
  if(!proof.passed)throw Error('Canvas proof failed');
  await execute('skill.save',{name:'interactive-counter',description:'Create and verify a counter with Add and Reset controls.'});
  activity('complete','Browser interaction, runnable artifact, five checks and reusable skill verified.');return proof;
}
async function guidedProof(){
  const context={id:randomUUID(),episodeStarted:false,observed:0,needsArtifact:false};
  await api('/api/learning/begin',{id:context.id,taskKind:'artifact-task',goal:'Guided browser/canvas proof'});context.episodeStarted=true;captureContext=context;let status='failed';
  try{const result=await guidedProofRaw();status='verified';return result;}
  finally{captureContext=null;await api('/api/learning/end',{id:context.id,status});await refresh();}
}
function validSender(event){if(!window||event.sender!==window.webContents||event.senderFrame!==window.webContents.mainFrame)throw Error('This surface has no workspace capability');}
async function launchGame(){
  if(gameProcess&&gameProcess.exitCode===null)return {launched:true,pid:gameProcess.pid,alreadyRunning:true};
  const gameLib=path.join(repo,'build','install','Fraynix-Studio','lib','*');
  if(!fs.existsSync(path.dirname(gameLib)))throw Error('Build the preserved Game Lab with gradlew installDist');
  const java=process.env.JAVA_HOME?path.join(process.env.JAVA_HOME,'bin','java.exe'):'java';
  const logPath=path.join(project,'game-lab.log'),gameLog=fs.openSync(logPath,'a');
  const args=['--enable-native-access=ALL-UNNAMED',`-Dfraynix.studio.workspace=${path.join(project,'game-lab')}`,'-Dfraynix.windowTitle=Fraynix Workspace | Game Lab'];
  if(process.env.FRAYNIX_WORKSPACE_TEST)args.push(`-Dfraynix.captureOnStartup=${path.join(project,'game-lab-start.png')}`);
  args.push('-cp',gameLib,'Main','--mario');
  gameProcess=spawn(java,args,{cwd:path.join(repo,'build','install','Fraynix-Studio'),windowsHide:true,stdio:['ignore',gameLog,gameLog]});
  await new Promise((resolve,reject)=>{gameProcess.once('error',reject);setTimeout(()=>gameProcess.exitCode===null?resolve():reject(Error('Game Lab failed to start; inspect '+logPath)),1200);});
  activity('game','Game Lab started in separate project storage',{pid:gameProcess.pid});return {launched:true,pid:gameProcess.pid,log:logPath};
}
function layout(input){for(const kind of ['browser','canvas']){const rect=input[kind];if(!rect)continue;const [w,h]=window.getContentSize();const x=Math.max(0,Math.min(w,Math.round(rect.x))),y=Math.max(0,Math.min(h,Math.round(rect.y)));const width=Math.max(1,Math.min(w-x,Math.round(rect.width))),height=Math.max(1,Math.min(h-y,Math.round(rect.height)));const surface=kind==='browser'?browser:canvas;surface.view.setBounds(kind==='canvas'&&rect.visible===false?{x:0,y:0,width:640,height:480}:{x,y,width,height});surface.view.setVisible(rect.visible!==false);surfacesVisible[kind]=rect.visible!==false;}}
// Faithful application export: the shell and child WebContents have separate compositor surfaces.
async function captureComposite(){
  const shell=await window.capturePage(),size=window.getContentSize();
  const layers=[];for(const kind of ['browser','canvas'])if(surfacesVisible[kind]){const surface=kind==='browser'?browser:canvas;layers.push({bounds:surface.view.getBounds(),image:(await surface.view.webContents.capturePage()).toDataURL()});}
  const input={shell:shell.toDataURL(),layers,sx:shell.getSize().width/size[0],sy:shell.getSize().height/size[1]};
  return window.webContents.executeJavaScript(`(async()=>{const data=${JSON.stringify(input)};const load=src=>new Promise((resolve,reject)=>{const i=new Image();i.onload=()=>resolve(i);i.onerror=reject;i.src=src});const base=await load(data.shell),c=document.createElement('canvas');c.width=base.naturalWidth;c.height=base.naturalHeight;const ctx=c.getContext('2d');ctx.drawImage(base,0,0);for(const layer of data.layers){const image=await load(layer.image),r=layer.bounds;ctx.drawImage(image,r.x*data.sx,r.y*data.sy,r.width*data.sx,r.height*data.sy);}return c.toDataURL('image/png').split(',')[1];})()`);
}
ipcMain.handle('workspace:request',async(event,input)=>{
  validSender(event);
  // A view switch may still be finishing when the user submits the prepared chat.
  // Wait for that operation, then apply the same ownership checks before admission.
  if(input.operation==='agent'&&manualBusy&&!learningJob){
    const deadline=Date.now()+20000;
    while(manualBusy&&!learningJob&&Date.now()<deadline)await new Promise(r=>setTimeout(r,100));
  }
  if((['brain','gameRuntime','memory'].includes(input.operation))&&!['state','read','frame','context','cancel'].includes(input.command)&&(run||learningJob||manualBusy))throw Error('Wait for the current operation, or take over from the agent first');
  const manual=(input.operation==='inputs100'&&input.command!=='state')||(input.operation==='agency043'&&['report','mission-open'].includes(input.command))||(input.operation==='agent042'&&['report','stop'].includes(input.command))||(input.operation==='alignment041'&&!['state','trial','capture','view','image','frame','stop','finish'].includes(input.command))||(input.operation==='runtime040'&&!['state','read','view','raster','summary'].includes(input.command))||(input.operation==='knowledge'&&['bind','report','open-source'].includes(input.command))||(input.operation==='tabletop'&&!['state','read','image','understanding','frame','stop'].includes(input.command))||(input.operation==='metrics'&&!['state','view'].includes(input.command))||(input.operation==='observations'&&!['state','read','frame','native'].includes(input.command))||(input.operation==='persistentWorld'&&!['list','read','observe','visibility','run','suite','counter-start','counter-status','rehearsal-goals','rehearsal-start','rehearsal-list','rehearsal-read','rehearsal-replay'].includes(input.command))||(input.operation==='pose'&&!['snapshot','run','observe'].includes(input.command))||(input.operation==='poseStudy'&&!['state','suite','read','run','live','goal'].includes(input.command))||(input.operation==='contactStudy'&&!['state','suite','read','run','live','goal'].includes(input.command))||(input.operation==='forceLearning'&&!['state','suite','read','run','live','goal'].includes(input.command))||(input.operation==='tracking'&&!['state','read','dataset','frame'].includes(input.command))||(input.operation==='perception'&&!['catalog','state','models','read','frame-preview'].includes(input.command))||(input.operation==='maps'&&!['select','snapshot','reference','read','list','dungeons','example','reference-cases','reference-example','inspect-image','run'].includes(input.command))||(input.operation==='spatial'&&!['snapshot','read','list','example','observe','run'].includes(input.command))||(input.operation==='data'&&!['list','inspect'].includes(input.command))||(input.operation==='javaGoal'&&!['goal.read','procedures.list'].includes(input.command))||(input.operation==='body'&&input.command!=='cancel')||input.operation==='transfer'||['tool','new','sourceSave','proof','back','forward','learningProof','learningEvaluate','learningRollback'].includes(input.operation)||(input.operation==='scene'&&!['snapshot','read','runs'].includes(input.command))||(input.operation==='measurement'&&!['read','list','comparison'].includes(input.command))||(input.operation==='calibration'&&!['read','list'].includes(input.command))||(input.operation==='observer'&&!['observe','history'].includes(input.command));
  if(manual&&workflowBridge?.reserved()&&!(input.operation==='javaGoal'&&['workflow.cancel','workflow.resume'].includes(input.command)))throw Error('Resume or Stop the evidence workflow before changing its workspace');
  if(manual&&goalBridge?.latest()?.procedure==='creation-web-v1'&&['running','waiting-host'].includes(goalBridge.latest().status)&&!(input.operation==='javaGoal'&&input.command==='goal.cancel'))throw Error('The creation goal owns the canvas; stop it before editing or starting another operation');
  if(manual&&(run||learningJob||manualBusy||pendingAgency043||agency043?.busy()))throw Error('Wait for the current operation, or take over from the agent first');
  if(input.operation==='agent'&&(manualBusy||learningJob))throw Error('Wait for the current manual operation');
  if(manual)manualBusy=true;
  try {
  switch(input.operation){
    case 'inputs100':{
      await inputs100.ready();
      if(input.command==='select')await inputs100.select(input.args);
      else if(input.command==='clear')await inputs100.clear();
      else if(input.command!=='state')throw Error('Unknown task input operation');
      const value={...inputs100.snapshot(),dataset:currentDataset,artifact:currentArtifact?{id:currentArtifact.id,version:currentArtifact.version,files:currentArtifact.files.filter(f=>f.endsWith('.csv'))}:null};
      if(input.command!=='state')emit('inputs100',value);return value;
    }
    case 'agency043':{if(input.command==='mission')return mission044(input.args.id);if(input.command==='mission-open'){const m=await mission044(input.args.id);if(!m.result)throw Error('This task has no saved canvas result');const a=await api('/api/artifact/read',{id:m.result.id});if(a.version!==m.result.version)throw Error('The saved result has a later revision; this task retains its original version reference');const opened=await openArtifact(a);emit('view','preview');return opened;}if(input.command==='start'){if(run||learningJob||manualBusy)throw Error('Finish the current operation first');return agency043.begin({...input.args,artifactId:currentArtifact?.id||''});}if(input.command==='report')return agency043.report(input.args.id);if(!['state','read','models','image','control'].includes(input.command))throw Error('Unknown Java task operation');const value=await api('/api/agency043/'+input.command,input.args||{});if(input.command==='control')await agency043.poll();return value;}
    case 'agent042':{if(input.command==='state')return api('/api/agent042/state',{});if(input.command==='control')return api('/api/agent042/control',input.args);if(input.command==='report')return execute('agent.report',{});if(input.command==='stop'){if(!runtimeConnection042)throw Error('This test session owns an ephemeral Java process');const stopped=await runtimeConnection042.stop();setTimeout(()=>app.quit(),100);return stopped;}throw Error('Unknown continuing agent operation');}
    case 'workflow':{if(input.command==='catalog')return api('/api/workflows/catalog',{});if(input.command==='start')return startWorkflowAgent(input.args.request,input.args.modelName,input.args);throw Error('Unknown workflow operation');}
    case 'creation':{if(input.command==='preview')return (await inputs100.resolve(input.args)).preview;if(input.command==='start')return startCreationAgent(input.args.request,input.model,{definition:input.args.definition,source:input.args.source});throw Error('Unknown creation operation');}
    case 'tracking':{
      if(input.command==='select')return trackingBridge.select(input.args.datasetIds);
      if(input.command==='import')return trackingBridge.importPack(input.args.pack);
      if(input.command==='open'){await api('/api/tracking/dataset',{id:input.args.id});await execute('browser.navigate',{url:origin+'/tracking-page/'+input.args.id});return perceptionBridge.catalog();}
      if(input.command==='report')return trackingBridge.action('tracking.report',input.args||{});
      if(input.command==='export'){const r=await api('/api/tracking/read',{id:input.args.id});return nativeExports.save('tracking-comparison','json',JSON.stringify(r,null,2)+'\n',{id:r.id});}
      if(input.command==='start')return trackingBridge.action('tracking.start',input.args||{});
      if(!['state','read','dataset','frame','promote','rollback','cancel'].includes(input.command))throw Error('Unknown tracking lab operation');return api('/api/tracking/'+input.command,input.args||{});
    }
    case 'perception':{
      if(input.command==='catalog')return perceptionBridge.catalog();
      if(input.command==='select')return perceptionBridge.select(input.args.ref);
      if(input.command==='inspect')return perceptionBridge.capture();
      if(input.command==='start')return goalBridge.execute('perception.start',input.args||{});
      if(input.command==='demo'){await execute('browser.navigate',{url:origin+'/perception-assets/demo.html'});return perceptionBridge.catalog();}
      if(input.command==='frame-preview'){const r=await api('/api/perception/frame-preview',{id:input.args.id});return {...r.frame,preview:'data:image/png;base64,'+r.png};}
      if(input.command==='export'){const r=await api('/api/perception/read',{id:input.args.id});return nativeExports.save('browser-perception','json',JSON.stringify(r,null,2)+'\n',{runId:r.id,initialFrameId:r.initialFrameId});}
      if(input.command==='clear'){await perceptionBridge.clear();return true;}
      if(!['state','models','read','teach'].includes(input.command))throw Error('Unknown perception command');return api('/api/perception/'+input.command,input.args||{});
    }
    case 'tabletop':{
      if(input.command==='select')return tabletopBridge.select(input.args.id);
      if(input.command==='report'){await tabletopBridge.select(input.args.id);return tabletopBridge.action('tabletop.report',{});}
      if(input.command==='export'){const result=await api('/api/tabletop/export',input.args);return nativeExports.save('private-tabletop','json',JSON.stringify(result),{runId:input.args.id});}
      if(!['state','check','start','frame','read','image','understanding','stop','finish','resume','analyse'].includes(input.command))throw Error('Unknown tabletop operation');return api('/api/tabletop/'+input.command,input.args||{});
    }
    case 'metrics':{
      if(input.command==='select')return observationBridge.metricSelect(input.args);
      if(input.command==='report'){const result=await api('/api/metrics/report',input.args);emit('view','preview');await openArtifact(result.artifact);return result.artifact;}
      if(input.command==='export'){const result=await api('/api/metrics/export',input.args);return nativeExports.save('metric-world','json',JSON.stringify(result),{runId:input.args.runId});}
      if(!['state','clock','geometry','view','fit','evaluate','source-frame'].includes(input.command))throw Error('Unknown metric operation');return api('/api/metrics/'+input.command,input.args||{});
    }
    case 'observations':{
      if(input.command==='import')return observationBridge.importPack(input.args.pack);
      if(input.command==='select')return observationBridge.select(input.args);
      if(input.command==='native')return api('/api/world/observe',input.args||{});
      if(input.command==='start'){const selected=observationBridge.selection();if(!selected)throw Error('Confirm the saved world and footage first');return api('/api/observations/start',selected);}
      if(input.command==='report'){const selected=observationBridge.selection();if(!selected)throw Error('Confirm the world and footage first');return observationBridge.action('observations.report',{}, {observationSelection:selected,observationEvidence:{id:input.args.id}});}
      if(input.command==='export'){const result=await api('/api/observations/export',input.args);return nativeExports.save('observed-world','json',JSON.stringify(result),{runId:input.args.id});}
      if(!['state','read','frame','stop','resume'].includes(input.command))throw Error('Unknown observation operation');return api('/api/observations/'+input.command,input.args||{});
    }
    case 'persistentWorld':{
      if(input.command==='upgrade')return api('/api/construction051/upgrade',input.args);
      if(input.command==='procedures077-export'){const data=await api('/api/procedures077/export',input.args);return nativeExports.save('verified-procedures-077','json',JSON.stringify(data),{studyId:input.args.id,caseId:input.args.caseId});}
      if(input.command==='procedures087-history')return api('/api/procedures087/history',input.args||{});
      if(['procedures086-preview','procedures086-review','procedures086-history'].includes(input.command))return api('/api/procedures086/'+input.command.slice(14),input.args||{});
      if(['procedures080-calibration','procedures080-pairs','procedures080-fit'].includes(input.command))return api('/api/procedures080/'+input.command.slice(14),input.args||{});
      if(input.command==='procedures079-export'){const data=await api('/api/procedures079/export',input.args);return nativeExports.save('world-procedure-079','json',JSON.stringify(data),{goalId:input.args.id});}
      if(input.command==='procedures079-memories'||input.command==='procedures079-protocol')return api('/api/procedures079/'+input.command.slice(14),{});
      if(input.command==='procedures078-export'){const data=await api('/api/procedures078/export',input.args);return nativeExports.save('verified-procedures-078','json',JSON.stringify(data),{studyId:input.args.id,caseId:input.args.caseId});}
      if(input.command?.startsWith('procedures077-')){const op=input.command.slice(14);if(!['protocol','list','start','read','resume','stop','trace','audit'].includes(op))throw Error('Unknown procedure study operation');return api('/api/procedures077/'+op,input.args);}
      if(input.command?.startsWith('procedures078-')){const op=input.command.slice(14);if(!['protocol','list','start','read','resume','stop','trace','audit'].includes(op))throw Error('Unknown procedure study operation');return api('/api/procedures078/'+op,input.args);}
      if(input.command==='procedures076-export'){const data=await api('/api/procedures076/export',input.args);return nativeExports.save('verified-procedures-076','json',JSON.stringify(data),{studyId:input.args.id,caseId:input.args.caseId});}
      if(input.command?.startsWith('procedures076-')){const op=input.command.slice(14);if(!['protocol','list','start','read','resume','stop','trace','audit'].includes(op))throw Error('Unknown procedure study operation');return api('/api/procedures076/'+op,input.args);}
      if(input.command==='memory075-export'){const data=await api('/api/memory075/export',input.args);return nativeExports.save('certified-memory-comparison','json',JSON.stringify(data),{studyId:input.args.id,caseId:input.args.caseId});}
      if(input.command?.startsWith('memory075-')){const op=input.command.slice(10);if(!['protocol','list','start','read','resume','recheck','stop','trace','audit'].includes(op))throw Error('Unknown memory comparison operation');return api('/api/memory075/'+op,input.args||{});}
      if(input.command==='memory074-export'){const data=await api('/api/memory074/export',input.args);return nativeExports.save('memory-comparison','json',JSON.stringify(data),{studyId:input.args.id,caseId:input.args.caseId});}
      if(input.command?.startsWith('memory074-')){const op=input.command.slice(10);if(!['protocol','list','start','read','resume','stop','trace','audit'].includes(op))throw Error('Unknown memory comparison operation');return api('/api/memory074/'+op,input.args||{});}

      if(input.command==='rehearsal-goals')return api('/api/goals/list',{});
      if(input.command==='rehearsal-start'){const a=input.args||{};if(!/^[A-Za-z0-9_.-]{1,80}$/.test(a.sourceId)||!['friction','massKg'].includes(a.property)||!Number.isFinite(a.value)||a.episodeId&&!/^[0-9a-f-]{36}$/.test(a.episodeId))throw Error('Choose a source role and one numeric component change');return startAgent('Rehearse experiment '+(a.episodeId||'')+', change '+a.sourceId+' '+a.property+' to '+a.value+'. '+(a.review?'Review before executing.':'Execute and verify the variant world.'),await window.webContents.executeJavaScript('document.querySelector("#model").value'));}
      if(['rehearsal-list','rehearsal-read','rehearsal-remember','rehearsal-replay'].includes(input.command))return api('/api/rehearsal073/'+input.command.slice(10),input.args||{});
      if(input.command==='rehearsal-export'){const data=await api('/api/rehearsal073/replay',input.args);return nativeExports.save('experiment-memory','json',JSON.stringify(data),{goalId:input.args.goalId});}
      if(input.command==='counter-start'){const name=input.args?.saveSkillName||'';if(name&&!/^[A-Za-z0-9][A-Za-z0-9_-]{1,63}$/.test(name))throw Error('Use a simple saved-skill name');return startAgent('Compare alternative plans in this persistent world, execute the best verified candidate and check predicted versus actual motion.'+(input.args?.reviewCounterfactual045?' Review before executing.':'')+(name?' Save the skill as '+name+'.':''),await window.webContents.executeJavaScript('document.querySelector("#model").value'));}
      if(input.command==='counter-status')return api('/api/agency043/read',{id:input.args.id});

      if(input.command==='counter-export'){const data=await api('/api/world/counter045/export',input.args);return nativeExports.save('counterfactual-world','json',JSON.stringify(data),{studyId:input.args.id});}

      if(input.command==='select'){const args=input.args||{};const {selectiveMemory079,memoryMode079,procedureReceipt085,...preview}=args;const plan=await api('/api/world/preview',preview);if(!plan.passed)throw Error(plan.reason);currentPersistentWorld={...args};return currentPersistentWorld;}
      if(input.command==='start'){
        const {expectedWorldId,expectedHash,...options}=input.args||{};
        if((expectedWorldId!==undefined||expectedHash!==undefined)&&
          (expectedWorldId!==currentPersistentWorld?.worldId||expectedHash!==currentPersistentWorld?.expectedHash)){
          throw Error('The selected practice world changed. Confirm the practice targets again.');
        }
        return goalBridge.execute('world.arrange',options);
      }
      if(input.command==='export'){const output=await api('/api/world/export',input.args||{});return nativeExports.save('persistent-world','json',output.text,{worldId:input.args.worldId});}
      if(input.command==='export-run'){const output=await api('/api/world/export-run',input.args||{});return nativeExports.save('persistent-world-run','json',output.text,{id:input.args.id});}
      if(!['create','list','read','observe','visibility','preview','restore','run','suite'].includes(input.command))throw Error('Unknown persistent world operation');return api('/api/world/'+input.command,input.args||{});
    }
    case 'skillMemory': {
      if(!['state','teach','recall'].includes(input.command))throw Error('Unknown skill memory command');
      return api('/api/brain/skill-memory-'+input.command,input.args||{});
    }
    case 'construction051': {
      const args=input.args||{};
      if(input.command==='ready')return {ready:!manualBusy};
      if(input.command==='select'){const observed=await api('/api/world/observe',{worldId:args.worldId});currentPersistentWorld={worldId:observed.worldId,expectedRevision:observed.expectedRevision,expectedHash:observed.expectedHash};return observed;}
      if(input.command==='start')return goalBridge.execute('construction.start',args);
      if(input.command==='envelope-export'){const data=await api('/api/construction056/load-export',args);return nativeExports.save('measured-load-redesign','json',JSON.stringify(data),{id:args.id});}
      if(input.command==='redesign-export'){const data=await api('/api/construction058/payload-export',args);return nativeExports.payloadStudy(data,{id:args.id,edition:data.originals?.manifest?.edition});}
      if(input.command==='payload-export'){const data=await api('/api/construction057/payload-export',args);return nativeExports.payloadStudy(data,{id:args.id});}
      if(input.command==='load-export'){const data=await api('/api/construction055/load-export',args);return nativeExports.save('load-experiments','json',JSON.stringify(data,null,2),{id:args.id});}
      if(input.command==='design-export'){const data=await api('/api/world/counter045/export',args);return nativeExports.save('assembly-design','json',JSON.stringify(data),{studyId:args.id});}
      if(input.command==='report'){const g=await api('/api/goals/read',args);if((g.status!=='completed'&&!(g.payloadDesign058&&g.counterfactual045View?.passed))||!g.design053||!g.report)throw Error('Choose a completed verified assembly design');const a=await api('/api/artifact/read',{id:g.report.id});if(a.version!==g.report.version)throw Error('Saved design report changed; this goal retains its original checked version');emit('view','preview');return openArtifact(a);}
      if(input.command==='export'){const output=await api('/api/world/export',args);return nativeExports.save('assembled-world','json',output.text,{worldId:args.worldId});}
      const routes={configurations:'/api/construction054/configurations',restore:'/api/world/restore',create:'/api/construction051/create',upgrade:'/api/construction051/upgrade',list:'/api/world/list',observe:'/api/world/observe',read:'/api/goals/read',goals:'/api/goals/list',stop:'/api/goals/cancel',resume:'/api/goals/resume'};
      if(!routes[input.command])throw Error('Unknown construction operation');return api(routes[input.command],args);
    }
    case 'workshop050': {
      if(input.command==='export'){const data=await api('/api/workshop050/trial',input.args||{});return nativeExports.save('construction-trial','json',JSON.stringify(data,null,2),{id:data.id});}
      const routes={start:'/api/workshop050/start',trial:'/api/workshop050/trial',read:'/api/goals/read',list:'/api/goals/list',stop:'/api/goals/cancel',resume:'/api/goals/resume',reuse:'/api/procedures/run'};
      if(!routes[input.command])throw Error('Unknown construction operation');return api(routes[input.command],input.args||{});
    }
    case 'evidence049': {
      if(input.command==='save-export'){
        const data=await api('/api/evidence049/export',input.args||{});
        return nativeExports.save('observation-evidence','json',data.text,{id:data.id});
      }
      if(!['list','import','read','export'].includes(input.command))throw Error('Unknown observation evidence operation');
      return api('/api/evidence049/'+input.command,input.args||{});
    }
    case 'pose':{
      if(input.command==='select'){const args=input.args||{},scene=await api('/api/spatial/read',{id:args.sceneId});await api('/api/pose/preview',args);currentPose={sceneId:scene.id,targets:args.targets,forceLimitN:args.forceLimitN??60,torqueLimitNm:args.torqueLimitNm??4};return currentPose;}
      if(input.command==='start')return goalBridge.execute('pose.start',input.args||{});
      if(input.command==='export'){const r=await api('/api/pose/export',input.args);return nativeExports.save('native-pose-run','json',r.text,{id:input.args.id});}
      if(!['preview','snapshot','run','pause','observe'].includes(input.command))throw Error('Unknown native pose operation');return api('/api/pose/'+input.command,input.args||{});
    }
    case 'poseStudy':{
      if(input.command==='goal')return api('/api/goals/read',input.args||{});
      if(input.command==='start')return goalBridge.execute('pose.compare',{});
      if(input.command==='export'){const r=await api('/api/pose-study/export',input.args);return nativeExports.poseComparison(r.text,{goalId:input.args.goalId});}
      if(!['state','suite','read','run','live'].includes(input.command))throw Error('Unknown pose comparison operation');return api('/api/pose-study/'+input.command,input.args||{});
    }
    case 'contactStudy':{
      if(input.command==='goal')return api('/api/goals/read',input.args||{});
      if(input.command==='start')return goalBridge.execute('contact.compare',{});
      if(input.command==='export'){const r=await api('/api/contact-study/export',input.args);return nativeExports.save('native-contact-experience','json',r.text,{goalId:input.args.goalId});}
      if(!['state','suite','read','run','live','promote','rollback'].includes(input.command))throw Error('Unknown native contact-study operation');return api('/api/contact-study/'+input.command,input.args||{});
    }
    case 'forceLearning':{
      if(input.command==='goal')return api('/api/goals/read',input.args||{});
      if(input.command==='start')return goalBridge.execute('force.start',{});
      if(input.command==='export'){const r=await api('/api/push-learning/export',input.args);return nativeExports.save('native-force-experience','json',r.text,{goalId:input.args.goalId});}
      if(!['state','suite','read','run','live','promote','rollback'].includes(input.command))throw Error('Unknown native force-learning operation');return api('/api/push-learning/'+input.command,input.args||{});
    }
    case 'push':{
      if(input.command==='select'){const args=input.args||{},scene=await api('/api/spatial/read',{id:args.sceneId}),a=scene.objects.find(o=>o.id===args.objectId&&o.massKg>0),b=scene.objects.find(o=>o.id===args.pusherId&&o.massKg>0);if(!a||!b||a.id===b.id)throw Error('Select distinct actual crate/pusher roles');currentSpatial={sceneId:scene.id,objectId:a.id};currentPushPusher=b.id;return pushSelection();}
      if(input.command==='start')return goalBridge.execute('push.start',input.args||{});
      if(input.command==='export-run'){const r=await api('/api/push/export-run',{id:input.args.id});return nativeExports.save('native-push-run','json',r.text,{id:input.args.id});}
      if(!['example','list','snapshot','scene-state','run','pause','preview','observe'].includes(input.command))throw Error('Unknown pushing operation');return api('/api/push/'+input.command,input.args||{});
    }
    case 'maps':{
      if(input.command==='select'){const args=input.args||{};if(Boolean(args.referenceId)===Boolean(args.mapId))throw Error('Select one map source');if(args.referenceId)await api('/api/maps/reference',{id:args.referenceId});else await api('/api/maps/read',{id:args.mapId});currentMapSelection=args.referenceId?{referenceId:args.referenceId}:{mapId:args.mapId};return currentMapSelection;}
      if(input.command==='export-run'){const result=await api('/api/maps/export-run',{id:input.args.id});return nativeExports.save('native-map-run','json',result.text,{id:input.args.id});}
      if(input.command==='export'){const result=await api('/api/maps/export',{id:input.args.id});return nativeExports.save('playable-map','json',result.text,{id:input.args.id});}
      if(!['example','reference-cases','reference-example','dungeons','inspect-image','import-image','reference','list','read','run','snapshot','manual','control','stop','pause','resume','from-procedural','import-package'].includes(input.command))throw Error('Unknown map operation');return api('/api/maps/'+input.command,input.args||{});
    }
    case 'spatial':{
      if(input.command==='start')return goalBridge.execute('spatial.start',input.args||{});
      if(input.command==='select'){const s=await api('/api/spatial/read',{id:input.args.sceneId});const o=s.objects.find(o=>o.id===input.args.objectId);if(!o)throw Error('Select an actual spatial object');currentSpatial=o.massKg>0?{sceneId:s.id,objectId:o.id}:null;currentPushPusher=currentSpatial?s.objects.find(b=>b.massKg>0&&b.id!==o.id)?.id||null:null;return currentSpatial;}
      if(input.command==='export'){const kind=input.args.kind;let bytes;if(kind==='scene'){const s=await api('/api/spatial/read',{id:input.args.id});bytes=s.sourceText;}else if(kind==='run')bytes=JSON.stringify(await api('/api/spatial/run',{id:input.args.id}),null,2)+'\n';else throw Error('Choose scene or run export');return nativeExports.save('spatial-'+kind,'json',bytes,{id:input.args.id,kind});}
      if(!['example','import','read','list','snapshot','preview','observe','run'].includes(input.command))throw Error('Unknown spatial command');const result=await api('/api/spatial/'+input.command,input.args||{});if(input.command==='import'){currentSpatial=null;await refresh();}return result;
    }
    case 'data':{
      if(input.command==='start')return goalBridge.execute('data.start',input.args||{});
      if(input.command==='list')return {datasets:(await refresh()).datasets,selected:currentDataset};
      if(input.command==='inspect')return api('/api/measurement/inspect',input.args||{});
      if(!['import','select'].includes(input.command))throw Error('Unknown data operation');
      const d=await api(input.command==='import'?'/api/measurement/import':'/api/measurement/read',input.args||{});currentDataset={datasetId:d.datasetId,version:d.version,title:d.title,sourceKind:d.sourceKind,rowCount:d.rows.length,sourceSha256:d.sourceSha256};emit('dataset',currentDataset);await refresh();return currentDataset;
    }
    case 'javaGoal':{if(input.command.startsWith('workflow.'))return workflowBridge.execute(input.command,input.args||{});if(!['map.start','map.read','space.start','space.import','space.export','goal.start','goal.read','goal.cancel','goal.resume','procedures.list','procedure.save','procedure.run'].includes(input.command))throw Error('Unknown Java goal command');return goalBridge.execute(input.command,input.args||{});}
    case 'alignment041':{
      const args=input.args||{};
      if(input.command==='export'){const data=await api('/api/alignment041/export',args);return nativeExports.save('measured-mat-alignment','json',JSON.stringify(data,null,2),{snapshotHash:args.snapshotHash});}
      if(input.command==='report')return alignment041.action('alignment.report',{}, {alignmentHash:args.snapshotHash});
      if(!['state','register','select','trial','start','frame','mark','finish','stop','evaluate','image','capture','view'].includes(input.command))throw Error('Unknown measured alignment command');return api('/api/alignment041/'+input.command,args);
    }
    case 'runtime040':{
      const args=input.args||{};
      if(input.command==='game-open'){await api('/api/game/open',{});return api('/api/runtime040/bind',{kind:'game'});}
      if(input.command==='game-step'){const selected=await api('/api/runtime040/state',{});if(selected.binding?.kind!=='game')throw Error('Link the open game first');await api('/api/game/command',args);return api('/api/runtime040/read',{});}
      if(input.command==='example'){const suite=await api('/api/world/suite',{}),c=suite.cases.find(c=>c.id==='dev035-three-body-chain');const scene=await api('/api/spatial/import',{text:JSON.stringify(c.source)});const world=await api('/api/world/create',{sceneId:scene.id,title:'Labelled040 shared-view world'});return api('/api/runtime040/bind',{kind:'world',worldId:world.id});}
      if(input.command==='export'){const data=await api('/api/runtime040/export',args);return nativeExports.save('shared-spatial-snapshot','json',JSON.stringify(data,null,2),{snapshotHash:args.snapshotHash});}
      if(input.command==='report')return runtime040.action('runtime.report',{}, {runtimeHash:args.snapshotHash});
      if(!['state','bind','read','view','pick','select','config','raster','summary'].includes(input.command))throw Error('Unknown shared runtime command');return api('/api/runtime040/'+input.command,args);
    }
    case 'knowledge':{
      const args=input.args||{};switch(input.command){
       case 'self':await updateSelfHost();return api('/api/self/read',args);
       case 'state':return api('/api/knowledge/state',{});
       case 'bind':{const value=await api('/api/knowledge/bind',args);await knowledgeBridge.select(value.id);return value;}
       case 'select':return knowledgeBridge.select(args.id);
       case 'object':return api('/api/knowledge/object',args);
       case 'job':return api('/api/knowledge/job',args);
       case 'research':return api('/api/knowledge/start',args);
       case 'cancel':{const value=await api('/api/knowledge/cancel',args);researchWorker.abort(args.id);return value;}
       case 'resume':return api('/api/knowledge/resume',args);
       case 'report':{const job=await api('/api/knowledge/job',{id:args.id});return knowledgeBridge.action('object.report',{}, {knowledgeSelection:{id:job.objectId},researchJobId:job.id});}
       case 'open-source':{const source=await api('/api/knowledge/source',args);return browser.navigate(source.finalUrl);}
       default:throw Error('Unknown knowledge operation');
      }
    }
    case 'brain':{if(!['useful-trace','uncertain-trace','later-trace','adaptive-trace','calibration-trace','recovery-trace','temporal-trace','decision-plan','decision-trace','contact-predict','contact-trace','stopping-predict','stopping-trace','residual-predict','outcome-predict','state','start','read','cancel','rollback','run','feedback-start','feedback-kick','feedback-cancel','feedback-rollback','world-prepare','world-preview','world-kick','world-cancel','world-rollback'].includes(input.command))throw Error('Unknown brain command');const value=await api('/api/brain/'+input.command,input.args||{});if(['run','feedback-start','feedback-kick','feedback-cancel','world-prepare','world-kick','world-cancel'].includes(input.command)){emit('experiment',await api('/api/scene/snapshot',{}));activity('brain',{'run':'Java controller completed a native impulse run','feedback-start':'Live feedback run started','feedback-kick':'Native disturbance applied','feedback-cancel':'Feedback run cancelled','world-prepare':'World-model run prepared without physical actuation','world-kick':'Native world disturbance applied','world-cancel':'World-model run cancelled'}[input.command],{runId:value.id||input.args?.runId,tick:value.tick,deltaVelocityMps:value.deltaVelocityMps,controller:value.controller,goalEvaluation:value.goalEvaluation});}return value;}
    case 'gameRuntime':{if(!['open','frame','command','state','close'].includes(input.command))throw Error('Unknown game command');return api('/api/game/'+input.command,input.args||{});}
    case 'memory':{if(!['remember','context'].includes(input.command))throw Error('Unknown memory command');return api('/api/memory/'+input.command,input.args||{});}
    case 'body':{if(!['grab','move','release','cancel'].includes(input.command))throw Error('Unknown body command');return execute('body.'+input.command,input.args||{});}
    case 'transfer':{
      if(input.command==='reveal'){if(!nativeExports.latest)throw Error('Export a file first');shell.showItemInFolder(nativeExports.latest.path);return nativeExports.latest;}
      const routes={scene:'scene.export',clone:'scene.clone',import:'scene.import',run:'run.export',csv:'run.csv',video:'run.video'};
      if(!routes[input.command])throw Error('Unknown transfer command');return execute(routes[input.command],input.args||{});
    }
    case 'observer':{
      const args=input.args||{};
      if(input.command==='observe')return observerSummary(await observeNative(args.id));
      if(input.command==='history')return api('/api/observer/history',args);
      if(input.command==='action')return execute('observer.'+args.action,Object.fromEntries(Object.entries(args).filter(([key])=>key!=='action')));
      if(!['open','capture','replay'].includes(input.command))throw Error('Unknown observer command');
      return execute('observer.'+input.command,args);
    }
    case 'calibration':{
      if(!['start','read','list','cancel','resume','apply','rollback','report'].includes(input.command))throw Error('Unknown calibration command');
      if(input.command==='report')return execute('calibration.report',input.args||{});
      const result=await api('/api/calibration/'+input.command,input.args||{});
      if(['apply','rollback'].includes(input.command)){currentScene=await api('/api/scene/read',{id:result.definition.trainingScene.id});emit('scene',currentScene);}
      if(!['read','list'].includes(input.command)){activity('calibration',input.command,{id:result.id,status:result.status});await refresh();}return result;
    }
    case 'measurement':{
      const routes={inspect:'/api/measurement/inspect',import:'/api/measurement/import',read:'/api/measurement/read',compare:'/api/measurement/compare',comparison:'/api/measurement/comparison',repeat:'/api/measurement/repeat',list:'/api/measurement/list'};
      if(input.command==='report')return execute('comparison.report',input.args||{});
      if(!routes[input.command])throw Error('Unknown measurement command');const result=await api(routes[input.command],input.args||{});
      if(['import','compare','repeat'].includes(input.command)){activity('measurement',input.command,{id:result.id,version:result.version,sourceKind:result.sourceKind,passed:result.passed});await refresh();}if(input.command==='repeat')emit('experiment',await api('/api/scene/snapshot',{}));return result;
    }
    case 'scene':{
      const routes={save:'/api/scene/save',read:'/api/scene/read',snapshot:'/api/scene/snapshot',control:'/api/experiment/control',run:'/api/experiment/run',replay:'/api/experiment/replay',record:'/api/experiment/read',runs:'/api/experiment/list'};
      if(!routes[input.command])throw Error('Unknown scene command');const result=await api(routes[input.command],input.args||{});
      if(input.command==='save'){currentScene=result;emit('scene',result);await refresh();}
      if(input.command==='read')currentScene=result;
      if(!['snapshot','read','runs','record'].includes(input.command))activity('scene',input.command,{id:result.id,status:result.status,evaluation:result.evaluation});return result;
    }
    case 'state':return {...await refresh(),currentArtifact,origin,log,run:run?{active:true,goal:run.goal}:null};
    case 'tool':if(run)throw Error('Take over before using manual tools');return execute(input.tool,input.args);
    case 'agent':startAgent(input.goal,input.model,input.planWorld099===undefined?{}:{planWorld099:input.planWorld099}).catch(error=>{activity('error',error.message);emit('answer',error.message);});return {started:true};
    case 'stop':await stopAgent();return {stopped:true};
    case 'proof':return guidedProof();
    case 'learningProof':return learnFromEpisodes({proof:true});
    case 'learningEvaluate':return learnFromEpisodes();
    case 'learningRead':return api('/api/learning/read',{type:input.type,id:input.id});
    case 'learningRollback':{const result=await api('/api/learning/rollback',{});activity('learning','Previous controller restored',{activePolicy:result.activePolicy});await refresh();return result;}
    case 'new':return execute('canvas.write',{title:'Untitled web artifact',files:{'index.html':starter}});
    case 'sourceSave':return execute('canvas.write',input.args);
    case 'layout':layout(input.bounds);return true;
    case 'back':if(browser.view.webContents.navigationHistory.canGoBack())browser.view.webContents.navigationHistory.goBack();return true;
    case 'forward':if(browser.view.webContents.navigationHistory.canGoForward())browser.view.webContents.navigationHistory.goForward();return true;
    case 'game':return launchGame();
    default:throw Error('Unknown workspace operation');
  }
  } finally {if(manual)manualBusy=false;}
});
if(!app.requestSingleInstanceLock())app.quit();
else app.whenReady().then(async()=>{
  try{
    verifyRendererSources(path.join(__dirname,'renderer'));await engineStart();Menu.setApplicationMenu(null);
    window=new BrowserWindow({width:1600,height:1040,minWidth:1100,minHeight:760,title:'Fraynix Agent Workspace',backgroundColor:'#10191c',webPreferences:{preload:path.join(__dirname,'preload.cjs'),nodeIntegration:false,contextIsolation:true,sandbox:true}});
    browser=new Surface('browser',window,(type,value)=>emit(type,value),{networkPolicy});canvas=new Surface('canvas',window,(type,value)=>emit(type,value),{networkPolicy});perceptionBridge=new BrowserPerception({browser,api,emit});trackingBridge=createTracking({api,openArtifact,execute,origin,activity,emit});observationBridge=createObservations({api,openArtifact,execute,activity,emit});tabletopBridge=createTabletop({api,openArtifact,execute,emit});researchWorker=new ResearchWorker({api,networkPolicy,activity,emit,withAuthority:(a,fn)=>a?agencyScope043.run(a,fn):fn()});knowledgeBridge=createKnowledge({api,execute,openArtifact,emit,activity,worker:researchWorker});runtime040=createRuntime040({api,execute,openArtifact,emit});alignment041=createAlignment041({api,execute,openArtifact,emit});window.on('close',()=>researchWorker.stop());
    // Only the trusted application top frame may request video. Other surfaces remain denied.
    const trustedMedia=(contents,permission,details={})=>contents===window.webContents&&permission==='media'&&details.isMainFrame!==false&&contents.getURL()===require('node:url').pathToFileURL(path.join(__dirname,'renderer','index.html')).href&&(!details.mediaTypes||details.mediaTypes.length===1&&details.mediaTypes[0]==='video')&&(!details.mediaType||details.mediaType==='video');
    window.webContents.session.setPermissionRequestHandler((contents,permission,callback,details)=>callback(trustedMedia(contents,permission,details)));
    window.webContents.session.setPermissionCheckHandler((contents,permission,origin,details)=>trustedMedia(contents,permission,details));
    window.webContents.setWindowOpenHandler(()=>({action:'deny'}));window.webContents.on('will-navigate',e=>e.preventDefault());
    agent042=createAgent042({api,execute,openArtifact,emit});agency043=createAgency043({api,execute,openArtifact,refresh,isAdmitting:()=>pendingAgency043>0,toolNames:catalog.map(t=>t.name),withContext:async(job,fn)=>{const prior=run,previous=captureContext;run=job;captureContext=job;try{return await fn();}finally{run=prior;captureContext=previous;}},getArtifact:()=>currentArtifact,getSurface:kind=>kind==='browser'?browser:canvas,showPreview:async()=>{emit('view','preview');await new Promise(r=>setTimeout(r,80));},emit,activity,sessionId:selfHostSession,planTools,captureWorkspace:async()=>{if(await window.webContents.executeJavaScript('Boolean(window.tabletopView?.stream||window.metricView?.stream||document.querySelector("dialog[open]"))'))throw Error('Close camera/footage dialogs before capturing the app interface');emit('view','preview');await new Promise(r=>setTimeout(r,120));const png=await captureComposite(),im=require('electron').nativeImage.createFromBuffer(Buffer.from(png,'base64')),size=im.getSize();return{png,viewport:[size.width,size.height],url:window.webContents.getURL(),title:window.getTitle()};},withAuthority:(value,fn)=>agencyScope043.run(value,fn)});
    await window.loadFile(path.join(__dirname,'renderer','index.html'));
    await api('/api/self/register',{sourceSha256:require('node:crypto').createHash('sha256').update(fs.readFileSync(path.join(__dirname,'policy.cjs'))).digest('hex'),capabilities:catalog.map(({name,description,args})=>({name,description,args}))});selfRegistered=true;const knowledgeState=await api('/api/knowledge/state',{});if(knowledgeState.selectedObjectId)await knowledgeBridge.select(knowledgeState.selectedObjectId);await updateSelfHost();researchWorker.start();const hostStateTimer044=setInterval(()=>updateSelfHost().catch(()=>{}),5000);window.on('closed',()=>clearInterval(hostStateTimer044));
    await browser.navigate(origin+'/welcome');await refresh();
    const artifacts=state.artifacts;
    if(artifacts.length){try{await openArtifact(await api('/api/artifact/read',{id:artifacts[artifacts.length-1].id}));}catch(error){activity('error','Saved artifact could not open; the workspace remains available. Select another artifact or revise its source.',{reason:error.message});}}
    else await execute('canvas.write',{title:'Welcome canvas',files:{'index.html':starter}});
    workflowBridge=createWorkflows({api,withAuthority:(a,fn)=>a?agencyScope043.run(a,fn):fn(),canvas,openArtifact,emit,refresh,activity,getDataset:()=>currentDataset,getScene:()=>currentScene,getGoals:()=>goalBridge,onAudit:async w=>{if(captureContext?.ownedWorkflowId===w.id)await episodeStep(captureContext,{tool:'workflow.audit',ok:!!w.audit?.passed,failure:w.audit?.passed?'none':'test-failed',elapsedMs:0,artifactId:w.report.id,artifactVersion:w.report.version});}});
    creationBridge=createCreation({api,activity});goalBridge=createGoals({api,withAuthority:(a,fn)=>a?agencyScope043.run(a,fn):fn(),browser,canvas,openArtifact,emit,refresh,activity,getScene:()=>currentScene,getDataset:()=>currentDataset,getSpatial:()=>currentSpatial,getPose:()=>currentPose,getWorld:()=>currentPersistentWorld,getPush:pushSelection,getMapping:()=>currentMapSelection,perception:perceptionBridge,creation:creationBridge,workflow:workflowBridge,getCreationSource:()=>creationSelection(),onScene:async scene=>{currentScene=scene;emit('scene',scene);emit('view','scene');},onTest:async(effect,receipt)=>{if(captureContext?.ownedGoalId===effect.goalId||effect.workflowId&&captureContext?.ownedWorkflowId===effect.workflowId)await episodeStep(captureContext,{tool:'canvas.test',ok:true,failure:receipt.passed?'none':'test-failed',elapsedMs:0,artifactId:receipt.id,artifactVersion:receipt.version,testPassed:receipt.passed});}});goalBridge.start();workflowBridge.start();await agency043.start(require('node:crypto').createHash('sha256').update(fs.readFileSync(path.join(__dirname,'policy.cjs'))).digest('hex'));
    if(process.env.FRAYNIX_WORKSPACE_CAPTURE){setTimeout(async()=>{await fsp.writeFile(process.env.FRAYNIX_WORKSPACE_CAPTURE,Buffer.from(await captureComposite(),'base64'));},1600);}
    if(process.env.FRAYNIX_WORKSPACE_TEST)global.workspaceTest={sessionId043:selfHostSession,agency043,activityTrace:()=>log,crashEngine:()=>engine?.kill(),canvasChecks:steps=>canvas.test(steps),canvasRead:code=>canvas.isolated(code),execute,controlledExecute,guidedProof,api,capture:captureComposite,launchGame,stopTestGame:()=>gameProcess?.kill(),origin:()=>origin,surfaces:()=>({browser:browser.view.webContents.id,canvas:canvas.view.webContents.id}),network:()=>networkPolicy.status(),knowledge:knowledgeBridge,runtime040,alignment041,research:researchWorker,stopAgent,startAgent,learnFromEpisodes,replay,project,goals:goalBridge,workflows:workflowBridge};
  }catch(error){console.error(error);app.quit();}
});
app.on('window-all-closed',()=>app.quit());
app.on('before-quit',event=>{if(closing)return;event.preventDefault();closing=true;agency043?.stop();researchWorker?.stop();workflowBridge?.stop();goalBridge?.stop();learningJob?.abort();if(run)run.cancelled=true;browser?.close();canvas?.close();Promise.race([runtimeConnection042?runtimeConnection042.detach().catch(()=>{}):api('/api/game/close',{}).catch(()=>{}),new Promise(r=>setTimeout(r,5000))]).finally(()=>{if(!runtimeConnection042)engine?.kill();app.quit();});});
