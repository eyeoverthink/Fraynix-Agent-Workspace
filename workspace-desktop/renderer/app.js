import {EnvironmentPanel} from './environment.js';
import {Procedures127} from './procedures127.js';
import {Inputs100Panel} from './inputs100.js';
import {Workspace099} from './workspace099.js';
import {BrowserChecks126} from './browser-checks126.js';
import {Construction051} from './construction051.js';
import {Workshop050} from './workshop050.js';
import {Agency043Panel} from './agency043.js';
import {Agent042Panel} from './agent042.js';
import {Small161Panel} from './small161.js';
import {Accuracy160Panel} from './accuracy160.js';
import {Recorded162Panel} from './recorded162.js';
import {Calibrated159Panel} from './calibrated159.js';
import {Alignment041Panel} from './alignment041.js';
import {SharedSpatial040} from './spatial-runtime040.js';
import {KnowledgePanel} from './knowledge-panel.js';
import {TabletopPanel} from './tabletop-panel.js';
import {MetricPanel} from './metric-panel.js';
import {Labels132Panel} from './labels132-panel.js';
import {Association134Panel} from './association134-panel.js';
import {Contact133Panel} from './contact133-panel.js';
import {ObservationPanel} from './observation-panel.js';
import {PersistentWorldPanel} from './persistent-world-panel.js';
import {PosePanel} from './pose-panel.js';
import {ContactStudyPanel} from './contact-study-panel.js';
import {ForceLearningPanel} from './force-learning-panel.js';
import {MapPanel} from './map-panel.js';
import {ProceduralPanel} from './procedural-panel.js';
import {WorkflowPanel} from './workflow-panel.js';
import {CreationPanel} from './creation-panel.js';
import {TrackingPanel} from './tracking-panel.js';
import {PerceptionPanel} from './perception-panel.js';
import {TileEditor} from './media-tile-editor.js';
import {MediaWorkshop} from './media-workshop.js';
import {AvatarPanel,isAvatarRequest} from './avatar-panel.js';
import {isMotionMediaRequest, MotionMediaPanel} from './motion-media.js';
import {TimelinePanel} from './timeline-editor.js';
import {isPlacementRequest} from './spatial-placement.js';
import {isRecipientApproachRequest} from './recipient-edit.js';
import {SpatialPanel,isSpatialPropRequest} from './spatial-panel.js';
import {isSpatialCarryRequest} from './spatial-carry.js';
import {isSpatialHeightRequest} from './spatial-height.js';
import {DataPanel} from './data-panel.js';
import {GoalsPanel} from './goals-panel.js';
import {WorldPanel} from './world-panel.js';
import {BrainGame} from './brain-game.js';
import {SceneLab} from './scene-lab.js';
import {MeasurementPanel} from './measurement-panel.js';
import {CalibrationPanel} from './calibration-panel.js';
import {ObserverPanel} from './observer-panel.js';
import {BodyPanel} from './body-panel.js';
import {RequestWorkbenchPanel,mayRequestFrayTool} from './request-workbench.js';
import {ChartFollowup} from './chart-followup.js';
import {SceneFiles} from './scene-files.js';
import {exportSceneVideo} from './scene-video.js';
import {WorkspaceShell} from './workspace-shell.js';
import {CAPABILITIES,matchWorkspaceIntent,workspaceRequestText} from './workspace-capabilities.mjs';
const $=id=>document.getElementById(id),host=window.workspace;
let state,artifact,editingFile='',libraryTab='artifacts',sourceMode=false,dirty=false,running=false,creationBusy=false,events=[];
let sceneMode=false;
let shell,submittingWorkspace=false;
function localWorkflowRequest(text) {
 return /^(?:(?:plan|route) a local workflow|run local workflow (?:ramp|analysis|creation)):[ \t]*\S.{0,1199}$/i.test(text.trim());
}
function quickWorkflowRequest(text) {
 return /^route a local workflow:[ \t]*\S.{0,1199}$/i.test(text.trim());
}
function offlineMissionRequest(text) {
 return /^Diagnose and repair Blender candidate [0-9a-f-]{36}, then playtest and save the evidence[.!]?(?: Pause after repair[.!]?)?$/i.test(text.trim()) ||
  /^Use Blender repair lesson [a-z0-9][a-z0-9-]{0,39} on candidate [0-9a-f-]{36}, then playtest and save the evidence[.!]?$/i.test(text.trim()) ||
  /^review a local chain:/i.test(text.trim()) || /^run local workflow (?:ramp|analysis):[ \t]*\S.{0,1199}$/i.test(text.trim()) || /^(?:run the local ramp experiment|compare the local ramp setups)(?: and pause after reading)?[.!]?$/i.test(text.trim()) || /^(?:playtest|verify) relay (?:ridge|switchyard) at (?:all three paces|(?:normal|steady|careful) pace)[.!]?$/i.test(text.trim());
}
function plannedRampRequest(text) {
 return /^(?:plan the local ramp experiment(?: and pause after reading)?[.!]?|plan a local ramp task(?: and pause after reading)?:[ 	]*\S.{0,1199})$/i.test(text.trim());
}
function hasChatRoute() {
 return Boolean(state && $('goal').value.trim() && !submittingWorkspace);
}
function updateChainRecommendation() {
 const goal=$('goal').value.trim();
 const localModel=state?.models.some(model=>model.name===$('model').value);
 $('chain-recommend').disabled=!localModel||running||creationBusy||dirty||lab.dirty||!goal||goal.length>1200
  ||!!state?.goals?.some(task=>['running','waiting-host'].includes(task.status));
}
const lab=new SceneLab({call:(command,args)=>request('scene',{command,args}),notice,activate:()=>setCanvasMode('scene'),dirtyChanged:()=>{if(state)$('run').disabled=running||creationBusy||dirty||lab.dirty||!hasChatRoute();updateChainRecommendation();}});
const measurements=new MeasurementPanel({call:(command,args)=>request('measurement',{command,args}),scene:()=>lab.scene,requireSaved,notice,preview:()=>setCanvasMode('preview')});
const calibrations=new CalibrationPanel({call:(command,args)=>request('calibration',{command,args}),scene:()=>lab.scene,requireSaved,notice,reopen:id=>lab.select(id),preview:()=>setCanvasMode('preview')});
const observers=new ObserverPanel({call:(command,args)=>request('observer',{command,args}),lab,requireSaved,notice});
const bodies=new BodyPanel({call:(command,args)=>request('body',{command,args}),lab,requireSaved,notice});
const files=new SceneFiles({call:(command,args)=>request('transfer',{command,args}),lab,requireSaved,notice});
const worldPanel=new WorldPanel({call:(command,args={})=>request('brain',{command,args}),sceneCall:(command,args={})=>request('scene',{command,args}),lab,notice,layout:updateLayout,activate:setCanvasMode,requireSaved});
window.nativeObserver={video:run=>exportSceneVideo(lab,run),openScene:scene=>lab.open(scene),viewport:()=>lab.observerViewport(),show:packet=>{setCanvasMode('scene');observers.show(packet);bodies.observe(packet);return {id:packet.id};},capture:packet=>{setCanvasMode('scene');observers.show(packet);return lab.captureObserver(packet);}};
lab.afterOpen=async()=>{observers.sceneChanged();bodies.packet=null;bodies.controls();files.controls();if($('measurement-panel').open)await measurements.refresh();if($('calibration-panel').open)await calibrations.refresh();};
function notice(text){$('notice').textContent=text;}
async function request(operation,data={}){try{return await host.request({operation,...data});}catch(error){notice(error.message);throw error;}}
function requireSaved(){if(dirty||lab.dirty)throw Error('Save or revert your source or scene changes first');}
const agent042Panel=new Agent042Panel(request);
window.agency043View=new Agency043Panel(request);
const brainGame=new BrainGame({call:(operation,command,args={})=>request(operation,{command,args}),notice,activate:setCanvasMode,layout:updateLayout,lab,requireSaved});
const dataPanel=new DataPanel({call:(command,args)=>request('data',{command,args}),notice,requireSaved,layout:updateLayout});
const goalsPanel=new GoalsPanel({getMapSources:()=>mapPanel.sources,onCompileMap:(id,layers)=>mapPanel.compile(id,layers),getCreationSources:()=>creationPanel.sources(),getDatasets:()=>state?.datasets||[],call:(command,args)=>request('javaGoal',{command,args}),notice,requireSaved,openReport:async id=>{requireSaved();setCanvasMode('preview');await request('tool',{tool:'canvas.open',args:{id}});}});
const mediaWorkshop=new MediaWorkshop(request);
const tileEditor=new TileEditor(request);
window.tileEvidence=Object.freeze({
  levelId:()=>tileEditor.levelId||'',
  live:options=>tileEditor.liveEvidence(options),
  render:options=>tileEditor.renderEvidence(options),
  replay:options=>tileEditor.replay(options),
  settle:()=>tileEditor.pendingRun
});
let mediaMode='tiles';
function showMedia(mode) { mediaMode=mode; setCanvasMode('media'); }
const avatars=new AvatarPanel(request,async sprite=>{
 showMedia('tiles');
 await tileEditor.pending;
 await tileEditor.addSprite(sprite);
});
const timelineEditor=new TimelinePanel(request,()=>showMedia('tiles'));
const motionMedia=new MotionMediaPanel(request,()=>showMedia('tiles'),async id=>{
 showMedia('timeline');
 await timelineEditor.pending;
 await timelineEditor.attach(id);
});
$('media-legacy-mode').onclick=()=>showMedia('sprite');
$('media-tiles-mode').onclick=()=>showMedia('tiles');
$('media-motion-mode').onclick=()=>showMedia('motion');
$('media-timeline-mode').onclick=()=>showMedia('timeline');
$('media-avatar-mode').onclick=()=>showMedia('avatar');
$('media-tab').onclick=()=>setCanvasMode('media');
const spatial=new SpatialPanel({call:(command,args={})=>request('spatial',{command,args}),pushCall:(command,args={})=>request('push',{command,args}),goalCall:(command,args={})=>request('javaGoal',{command,args}),notice,activate:setCanvasMode,goal:g=>goalsPanel.accept(g),announce:text=>message(text),layout:updateLayout});
const environment=new EnvironmentPanel({request,activate:setCanvasMode});
window.environmentEvidence=options=>environment.measure(options);
const workshop050=new Workshop050({call:(command,args={})=>request('workshop050',{command,args}),goal:g=>goalsPanel.accept(g),requireSaved});
const construction051=new Construction051({call:(command,args={})=>request('construction051',{command,args}),goal:g=>goalsPanel.accept(g),requireSaved,notice});
const forcePanel=new ForceLearningPanel({call:(command,args={})=>request('forceLearning',{command,args}),spatial,notice,goal:g=>goalsPanel.accept(g),openReport:async id=>{setCanvasMode('preview');await request('tool',{tool:'canvas.open',args:{id}});}});
const contactPanel=new ContactStudyPanel({call:(command,args={})=>request('contactStudy',{command,args}),spatial,notice,goal:g=>goalsPanel.accept(g),openReport:async id=>{setCanvasMode('preview');await request('tool',{tool:'canvas.open',args:{id}});}});
const posePanel=new PosePanel({call:(command,args={})=>request('pose',{command,args}),studyCall:(command,args={})=>request('poseStudy',{command,args}),goalCall:(command,args={})=>request('javaGoal',{command,args}),spatial,notice,goal:g=>goalsPanel.accept(g),openReport:async id=>{setCanvasMode('preview');await request('tool',{tool:'canvas.open',args:{id}});}});
const persistentPanel=new PersistentWorldPanel({
  call:(command,args={})=>request('persistentWorld',{command,args}),
  spatial,notice,goal:g=>goalsPanel.accept(g),
  openReport:async id=>{requireSaved();setCanvasMode('preview');await request('tool',{tool:'canvas.open',args:{id}});},
  requireFirstSkillReady:()=>{
    requireSaved();
    if(running||creationBusy||state?.goals?.some(g=>['running','waiting-host'].includes(g.status))){
      throw Error('Finish or stop the active task before running the practice skill');
    }
  },
  connectFirstSkill:(goal,steps)=>workflowPanel.openGraph(null,{
    worldId:goal.worldBinding.worldId,skill:goal.savedSkill,steps
  })
});
const observationPanel=new ObservationPanel({call:(command,args={})=>request('observations',{command,args}),persistent:persistentPanel,notice});
const labels132Panel=new Labels132Panel({call:(command,args={})=>request('labels132',{command,args}),observation:observationPanel,notice});window.labels132View=labels132Panel;
const contact133Panel=new Contact133Panel({labels:labels132Panel,call:(command,args={})=>request('contact133',{command,args}),notice});window.contact133View=contact133Panel;
const association134Panel=new Association134Panel({labels:labels132Panel,after:contact133Panel.root,call:(command,args={})=>request('association134',{command,args}),notice});window.association134View=association134Panel;
window.observationView=observationPanel;
window.metricView=new MetricPanel({call:(command,args={})=>request('metrics',{command,args}),observation:observationPanel,notice});
window.tabletopView=new TabletopPanel({call:(command,args={})=>request('tabletop',{command,args}),metricCall:(command,args={})=>request('metrics',{command,args}),persistent:persistentPanel,metric:window.metricView,notice});
window.knowledgeView=new KnowledgePanel({call:(command,args={})=>request('knowledge',{command,args}),tabletop:window.tabletopView,notice,activate:mode=>{window.alignment041?.close();window.sharedSpatial040?.close();setCanvasMode(mode);}});
window.sharedSpatial040=new SharedSpatial040({call:(command,args={})=>request('runtime040',{command,args}),spatial,persistent:persistentPanel,tabletop:window.tabletopView,notice,activate:mode=>{window.alignment041?.close();window.sharedSpatial040?.close();setCanvasMode(mode);}});
window.alignment041=new Alignment041Panel({call:(command,args={})=>request('alignment041',{command,args}),runtime:(command,args={})=>request('runtime040',{command,args}),spatial,media:window.tabletopView,notice,activate:setCanvasMode});
window.recorded162View=new Recorded162Panel({call:(command,args={})=>request('recorded162',{command,args}),small:(command,args={})=>request('small161',{command,args}),alignment:(command,args={})=>request('alignment041',{command,args}),notice});
window.calibrated159View=new Calibrated159Panel({call:(command,args={})=>request('calibrated159',{command,args}),world:(command,args={})=>request('persistentWorld',{command,args}),alignment:(command,args={})=>request('alignment041',{command,args}),notice});
window.accuracy160View=new Accuracy160Panel({call:(command,args={})=>request('accuracy160',{command,args}),alignment:(command,args={})=>request('alignment041',{command,args}),notice,activate:setCanvasMode,spatial});
window.small161View=new Small161Panel({call:(command,args={})=>request('small161',{command,args}),notice,activate:setCanvasMode,spatial});
const perceptionPanel=new PerceptionPanel({call:(command,args={})=>request('perception',{command,args}),goalCall:(command,args={})=>request('javaGoal',{command,args}),notice,activate:setCanvasMode,goal:g=>goalsPanel.accept(g)});
const trackingPanel=new TrackingPanel({call:(command,args={})=>request('tracking',{command,args}),notice,requireSaved,openPerception:()=>perceptionPanel.open()});
const creationPanel=new CreationPanel({layout:updateLayout,call:(command,args,model)=>request('creation',{command,args,model}),getState:()=>state,getArtifact:()=>artifact,getModel:()=>$('model').value,requireSaved,notice,onGoal:g=>goalsPanel.accept(g)});
const chartFollowup=new ChartFollowup({request,layout:updateLayout,announce:message,requireSaved});
$('tile-model-3d').onclick=()=>spatial.action(()=>spatial.openLibraryViews(tileEditor.active));
const requestsPanel=new RequestWorkbenchPanel({request,getModel:()=>$('model').value,requireSaved,layout:updateLayout,
 openCreation:brief=>creationPanel.open(null,brief),announce:message});

const workflowPanel=new WorkflowPanel({layout:updateLayout,call:(command,args)=>request('workflow',{command,args}),getState:()=>state,getModel:()=>$('model').value,requireSaved,notice,onGoal:w=>goalsPanel.accept(w),openFirstSkill:()=>persistentPanel.showFirstSkill()});
const proceduralPanel=new ProceduralPanel({layout:updateLayout,call:(command,args)=>request('javaGoal',{command,args}),requireSaved,notice,onGoal:g=>goalsPanel.accept(g)});
const mapPanel=new MapPanel({controlCall:args=>host.request({operation:'maps',command:'control',args}),call:(command,args={})=>request('maps',{command,args}),goalCall:(command,args={})=>request('javaGoal',{command,args}),notice,activate:setCanvasMode,goal:g=>goalsPanel.accept(g),requireSaved});
window.nativeMap={snapshot:()=>({mapId:mapPanel.map?.id,avatar:mapPanel.avatar?.position.toArray(),trace:mapPanel.history.length}),open:id=>mapPanel.open().then(()=>id?mapPanel.selectWorld(id):undefined)};
function setCanvasMode(mode) {
 const activeMedia=mode==='media';
 environment.show(mode==='environment');
 mediaWorkshop.show(activeMedia&&mediaMode==='sprite');
 avatars.show(activeMedia&&mediaMode==='avatar');
 tileEditor.show(activeMedia&&mediaMode==='tiles');
 motionMedia.show(activeMedia&&mediaMode==='motion');
 timelineEditor.show(activeMedia&&mediaMode==='timeline');
 for(const [name,panel] of [['maps',mapPanel],['perception',perceptionPanel],['spatial',spatial]]) {
  panel.visible=mode===name;
  $(name+'-view').hidden=!panel.visible;
  document.body.classList.toggle(name+'-active',panel.visible);
 }
 $('game-view').hidden=mode!=='game';
 sceneMode=mode==='scene';
 sourceMode=mode==='source';
 lab.visible=sceneMode;
 $('scene-lab').hidden=!sceneMode;
 $('source-editor').hidden=!sourceMode;
 $('canvas-surface').hidden=mode!=='preview';
 document.body.classList.toggle('scene-active',sceneMode);
 for(const name of ['preview','source','scene','spatial','environment','perception','maps','game','media'])
  $(name+'-tab').classList.toggle('selected',name===mode);
 for(const id of ['test-badge','test-canvas','save-skill'])
  $(id).hidden=!['preview','source'].includes(mode);
 const titles={
  home:['Workspace','',''],
  environment:['Environment','Room models, floor plans and recordings','GLB · geometry and evidence'],
  perception:['Browser perception','Observe and track visible objects','Browser · observe / teach / trace'],
  maps:['Playable reference world','Image source and measured feedback','3D physics · map layers'],
  spatial:['Spatial workspace','Models, placement and measured geometry','3D · scene evidence'],
  game:['Integrated Java game','Original renderer and runtime controls','Game · XY simulation'],
  media:['Media Workshop',
   {avatar:'Talking portraits and game sprites',timeline:'Picture, overlay and audio tracks',motion:'Native recording and verified media',sprite:'Single-sprite level',tiles:'Sprite library and tile editor'}[mediaMode],
   mediaMode==='avatar'?'PNG · sprite sheet · AVI': ['timeline','motion'].includes(mediaMode)?'Video · WAV · PNG · timeline':'2D · browser game']
 };
 if(sceneMode) {
  libraryTab='scenes';
  for(const name of ['artifacts','skills','learning','scenes'])$(name+'-tab').classList.toggle('selected',name==='scenes');
  renderLibrary();
  $('object-detail').textContent=lab.scene?.id||'Persistent scene entities';
  titles.scene=[lab.scene?.title||'3D mechanics lab',lab.scene?'Scene v'+lab.scene.version+' · SI units · 240 Hz physics':'Open a scene','3D · native physics'];
 }
 if(titles[mode]) {
  [$('artifact-title').textContent,$('artifact-version').textContent,$('canvas-kind').textContent]=titles[mode];
 } else {
  if(artifact)showArtifact(artifact);
  $('canvas-kind').textContent='HTML · CSS · JavaScript';
 }
 shell?.stageChanged(mode);
 if(sceneMode)lab.resize();
 if(mode==='maps')mapPanel.resize();
 if(mode==='spatial')spatial.resize();
 updateLayout();
}
function message(text,role='assistant'){window.workbench099.message(text,role);}
function timeline(entry){events.push(entry);if(events.length>150)events.shift();const row=document.createElement('div');row.className='event '+entry.kind;const time=document.createElement('time');time.textContent=new Date(entry.at).toLocaleTimeString([],{hour12:false});const kind=document.createElement('span');kind.className='kind';kind.textContent=entry.kind;const text=document.createElement('span');text.className='text';text.textContent=entry.text;row.append(time,kind,text);if(entry.data&&Object.keys(entry.data).length){const details=document.createElement('details'),summary=document.createElement('summary'),pre=document.createElement('pre');summary.textContent='Details';pre.textContent=JSON.stringify(entry.data,null,2);details.append(summary,pre);row.append(details);}$('events').append(row);while($('events').children.length>150)$('events').firstChild.remove();$('events').scrollTop=$('events').scrollHeight;$('timeline-count').textContent=events.length+' events';}
function learningItems(){const learning=state.learning||{};return ['evaluation','candidate','episode','promotion'].flatMap(type=>(learning[type==='episode'?'episodes':type+'s']||[]).map(item=>({...item,recordType:type})));}
function showLearningRecord(value,type){
  $('learning-record').textContent=JSON.stringify(value,null,2);$('learning-cases').replaceChildren();
  $('learning-summary').textContent=type==='evaluation'?`${value.accepted?'Promoted':'Rejected'} · ${value.baselinePassed}/8 → ${value.candidatePassed}/8 fixtures passed · rejected actions and extra reads ${value.baselineFailures} → ${value.candidateFailures}`:type==='episode'?`${value.label} · ${value.status} · ${value.steps.length} recorded actions · controller v${value.policy.version}`:value.label||'Learning checkpoint';
  if(value.results){const table=document.createElement('table');const head=table.insertRow();for(const text of ['Independent fixture','Baseline','Candidate']){const th=document.createElement('th');th.textContent=text;head.append(th);}for(const result of value.results){const row=table.insertRow();for(const text of [result.id,result.baseline.passed?'Pass':'Fail',result.candidate.passed?'Pass':'Fail'])row.insertCell().textContent=text;}$('learning-cases').append(table);}
  $('learning-inspection').showModal();updateLayout();
}
function renderLibrary(){window.agency043View?.recipes(state?.skills||[]);
  if(!state)return;$('library-items').replaceChildren();
  const items=libraryTab==='learning'?learningItems():libraryTab==='scenes'?(state.scenes||[]):libraryTab==='artifacts'?state.artifacts:[...state.skills,...(state.procedures||[])];
  persistentPanel.updateFirstSkill(state.goals?.find(goal=>goal.id===persistentPanel.firstSkillGoalId));
  if(libraryTab==='skills'){
    const hasWorldSkill=items.some(item=>['persistent-world-arrange-v1','selective-world-procedure-079-v1'].includes(item.kind));
    const entry=document.createElement('button');
    entry.id='first-world-library';
    entry.className='quiet';
    entry.textContent=hasWorldSkill?'Connect verified world skills':'Create first world skill';
    entry.onclick=()=>safe(async()=>{
      requireSaved();
      if(hasWorldSkill)await workflowPanel.openGraph();
      else persistentPanel.showFirstSkill();
    });
    $('library-items').append(entry);
    if(persistentPanel.firstSkillGoal?.savedSkill){
      const practice=document.createElement('button');
      practice.id='first-world-resume-guide';
      practice.className='quiet';
      practice.textContent='Continue practice world';
      practice.onclick=()=>safe(()=>{requireSaved();persistentPanel.showFirstSkill();});
      $('library-items').append(practice);
    }
  }
  for(const item of items){const button=document.createElement('button');button.className='object'+(item.id===(libraryTab==='scenes'?lab.scene?.id:artifact?.id)?' selected':'');const label=document.createElement('span');label.textContent=item.label||item.title||item.name;const version=document.createElement('small');version.textContent=libraryTab==='learning'?(item.status||item.recordType):'v'+item.version;button.append(label,version);button.disabled=running;
    button.onclick=()=>safe(async()=>{requireSaved();if(libraryTab==='learning')showLearningRecord(await request('learningRead',{type:item.recordType,id:item.id}),item.recordType);else if(libraryTab==='scenes')await lab.select(item.id);else{setCanvasMode('preview');if(libraryTab==='artifacts')await request('tool',{tool:'canvas.open',args:{id:item.id}});else if(item.kind==='typed-skill-graph-v1'){await window.openSkillGraph046(item);updateLayout();}else if(['selective-world-procedure-079-v1','gripper-construction-051-v1','construction-workshop-050-v1','persistent-world-arrange-v1','world-reach-report-v1','motion-data-report-v1','spatial-manipulation-v1','spatial-push-v1','spatial-arrange-v1','browser-perception-v1','creation-web-v1','evidence-workflow-v1','procedural-space-v1','map-world-v1'].includes(item.kind)){goalsPanel.showProcedure(item);updateLayout();}else await request('tool',{tool:'skill.run',args:{name:item.name}});}});$('library-items').append(button);}
    if(!items.length){const empty=document.createElement('p');empty.className='muted';empty.textContent=libraryTab==='learning'?'Task episodes, candidates and measured replay results appear here.':libraryTab==='scenes'?'Open 3D Lab to create a real ramp scene.':libraryTab==='skills'?'Passing recipes appear here after you save them.':'Create your first artifact.';$('library-items').append(empty);}
}
function renderState(value){state=value;window.workbench099?.state(value);const workflow=value.workflows?.find(w=>!['completed','cancelled'].includes(w.status))||value.workflows?.at(-1),last=value.goals?.at(-1),selectedGoal=workflow&&(!last||last.workflowId===workflow.id||!['completed','cancelled'].includes(workflow.status)||workflow.updatedAt>=last.updatedAt)?workflow:last;if(selectedGoal){goalsPanel.accept(selectedGoal);creationBusy=(selectedGoal.workflow&&!['completed','cancelled'].includes(selectedGoal.status))||(selectedGoal.procedure==='creation-web-v1'&&['running','waiting-host'].includes(selectedGoal.status));$('creation-open').disabled=creationBusy||running;}$('engine-state').textContent=value.discovering?'Discovering models':'Java engine online · '+value.models.length+' local models';const selected=$('model').value;const options=value.models.map(m=>m.name);if([...$('model').options].map(o=>o.value).join()!==options.join()){$('model').replaceChildren(...options.map(name=>{const o=document.createElement('option');o.value=name;o.textContent=name;return o;}));if(options.includes(selected))$('model').value=selected;else if(options.includes(localStorage.getItem('selectedModel')))$('model').value=localStorage.getItem('selectedModel');else if(options.includes('gpt-oss:20b'))$('model').value='gpt-oss:20b';else if(options.includes('eyeoverthink/FRAYNIX-PROTOCOL:3b'))$('model').value='eyeoverthink/FRAYNIX-PROTOCOL:3b';}$('run').disabled=running||creationBusy||dirty||lab.dirty||!hasChatRoute()||value.goals?.some(g=>['running','waiting-host'].includes(g.status));$('controller-version').textContent='Task controller v'+(value.learning?.activePolicy.version||1);$('episode-count').textContent=(value.learning?.episodeCount||0)+' remembered '+((value.learning?.episodeCount||0)===1?'episode':'episodes');$('learning-rollback').disabled=running||(value.learning?.activePolicy.version||1)===1;updateChainRecommendation();renderLibrary();}
function showArtifact(value){artifact=value;dirty=false;$('artifact-title').textContent=value.title;$('artifact-version').textContent='v'+value.version+' · '+value.files.length+' files · saved';$('object-detail').textContent=value.id+' · artifact component';const passed=value.test?.passed;$('test-badge').className='badge'+(value.test?(passed?' pass':' fail'):'');$('test-badge').textContent=value.test?(passed?'TESTS PASSED':'TESTS FAILED'):'UNTESTED';$('save-skill').disabled=!passed||running;$('test-canvas').disabled=running||!value.test?.steps?.length;$('source-file').replaceChildren(...value.files.map(name=>{const o=document.createElement('option');o.textContent=name;o.value=name;return o;}));editingFile=$('source-file').value;$('source').value=value.contents?.[editingFile]||'';updateChainRecommendation();renderLibrary();}
let layoutPending=false;
function updateLayout(){if(layoutPending)return;layoutPending=true;requestAnimationFrame(()=>{layoutPending=false;const bounds={};for(const kind of ['browser','canvas']){const r=$(kind+'-surface').getBoundingClientRect();bounds[kind]={x:r.x,y:r.y,width:r.width,height:r.height,visible:r.width>0&&r.height>0&&(kind!=='canvas'||!sourceMode)&&!document.querySelector('dialog[open]')};}host.request({operation:'layout',bounds}).catch(()=>{});});}
document.addEventListener('scroll',updateLayout,{capture:true,passive:true});
new ResizeObserver(updateLayout).observe(document.querySelector('main'));for(const kind of ['browser','canvas'])new ResizeObserver(updateLayout).observe($(kind+'-surface'));
function safe(action){Promise.resolve().then(action).catch(e=>notice(e.message));}
$('model').onchange=()=>{localStorage.setItem('selectedModel',$('model').value);updateChainRecommendation();};
$('navigate').onclick=()=>safe(async()=>{await request('tool',{tool:'browser.navigate',args:{url:$('address').value}});});
$('address').addEventListener('keydown',e=>{if(e.key==='Enter')$('navigate').click();});
$('back').onclick=()=>safe(()=>request('back'));$('forward').onclick=()=>safe(()=>request('forward'));
$('new-artifact').onclick=()=>safe(()=>{requireSaved();setCanvasMode('preview');return request('new');});
$('proof').onclick=()=>safe(async()=>{requireSaved();$('proof').disabled=true;notice('Running the real browser → artifact → test → skill proof');try{await request('proof');notice('Guided proof passed: browser, runnable app, tests and skill saved');}finally{$('proof').disabled=false;}});
const relayReview=$('relay-review-dialog');
$('relay-review-open').onclick=()=>safe(async()=>{
 requireSaved();
 relayReview.showModal();
 updateLayout();
});
relayReview.addEventListener('close',()=>updateLayout());
$('relay-review-prepare').onclick=()=>{
 const course=$('relay-review-course').value;
 const pause=$('relay-review-pause').checked?' Pause after playtest.':'';
 $('goal').value='Review a local chain: Playtest Relay '+course+', then build a clearance dashboard.'+pause;
 $('goal').dispatchEvent(new Event('input'));
 relayReview.close();
 $('goal').focus();
};
$('game').onclick=()=>safe(async()=>{await brainGame.openIntegrated();notice('Original Java game runtime is integrated here; no separate editor window');});
$('chain-recommend').onclick=()=>safe(async()=>{
 requireSaved();
 const goal=$('goal').value.trim();
 const model=$('model').value;
 if(!goal||!state?.models.some(row=>row.name===model))return;
 message(goal,'user');
 await request('agent',{goal,model,chainRecommendation:true});
});
let colliderQuestion = null;
let routingCollider = false;
function showColliderQuestion(route, restoring = false) {
 colliderQuestion = route.decision === 'clarify' ? route : null;
 const last = window.workbench099.transcript.at(-1);
 if (colliderQuestion && (!restoring || last?.role !== 'assistant' || last.text !== colliderQuestion.question))
  message(colliderQuestion.question);
}
async function routeColliderChat(goal, model) {
 const explicit = /\bcolliders?\b|\bcollision box\b/i.test(goal);
 const selected = !spatial.worldMode && spatial.scene?.recipientBranch ? spatial.scene.id : '';
 if (goal.length>300 || (!colliderQuestion && !selected && !explicit)) return false;
 if (!model) {
  if (!colliderQuestion) return false;
  message('Select a local model to answer the saved collider question.');
  return true;
 }
 routingCollider = true;
 $('run').disabled = true;
 $('run-state').textContent = 'The local model is reading the collider request…';
 try {
  const input = {request:goal,model,sceneId:selected};
  if (colliderQuestion) input.clarificationId = colliderQuestion.id;
  const route = await spatial.call('recipient-route',input);
  showColliderQuestion(route);
  if (route.decision === 'compare') {
   setCanvasMode('spatial');
   await spatial.action(async()=>{
    await spatial.open(route.target.sceneId);
    await spatial.recipient.open();
   });
   message('I compared the saved collider proposal. The mission keeps the model decision and measured results for your review.');
   return true;
  }
  if (route.decision === 'clarify') return true;
  if (explicit) {
   message('The collider mission can compare a pending proposal. Use the review controls for edits, approval, and export.');
   return true;
  }
  return false;
 } catch (error) {
  message('I could not complete the collider comparison. '+error.message.replace(/^Error invoking remote method '[^']+': (?:Error: )?/,''));
  return true;
 } finally {
  routingCollider = false;
  $('run').disabled = running||creationBusy||dirty||lab.dirty||!hasChatRoute()
   ||Boolean(state?.goals?.some(goal=>['running','waiting-host'].includes(goal.status)));
  $('run-state').textContent = 'Ready when you are';
 }
}
async function answerTileQuestion(goal) {
 const names=/\b(tile (workshop|level|editor)|nexus|playtest)\b/i;
 const change=/\b(?:without|omit|remove) (?:the )?jump at tick (\d+)\b/i.exec(goal);
 if (names.test(goal)&&/^compare\b/i.test(goal)&&change) {
   try {
     showMedia('tiles');
     const result=await request('tile-evidence',{tool:'tile.replay',args:{jumpTick:Number(change[1])}});
     tileEditor.showComparison(result);
     message('The unchanged inputs matched the saved playtest. Without the jump at tick '+result.jumpTick+
       ', the score changed from '+result.source.score+' to '+result.variant.score+
       (result.firstOutcomeDifference<0?'. The observed playtest did not change.':
        ' and the first outcome difference was at tick '+result.firstOutcomeDifference+'.'));
   } catch (error) {message('I could not compare the tile playtest. '+error.message.replace(/^Error invoking remote method '[^']+': (?:Error: )?/,''));}
   return true;
 }
 if (!names.test(goal)||!/^(show|read|inspect|report|export|what|which|how)\b/i.test(goal)
     ||!/\b(ticks?|pixels?|image|score|layers?|objects?|playtest)\b/i.test(goal)) return false;
 showMedia('tiles');
 const tick=/\btick\s+(\d+)/i.exec(goal);
 const pixel=/\bpixels?\s+(?:at\s*)?\(?\s*(\d+)\s*,\s*(\d+)/i.exec(goal);
 const invoke=(tool,args)=>request('tile-evidence',{tool,args});
 try {
   if (/\bpixels?\b/i.test(goal)) {
     if (!tick||!pixel) {message('Give me a tick and pixel coordinates, for example: Show Nexus pixel (154, 275) at tick 0.');return true;}
     const result=await invoke('tile.pixels',{tick:Number(tick[1]),x:Number(pixel[1]),y:Number(pixel[2])});
     message('Tile playtest tick '+result.tick+' · pixel ('+result.x+', '+result.y+') · RGBA '+result.rgba.join(', ')+'.');
   } else if (/\bimage\b/i.test(goal)) {
     if (!tick) {message('Which tick should I render from the tile playtest?');return true;}
     const result=await invoke('tile.image',{tick:Number(tick[1])});
     message('Saved the '+result.width+' × '+result.height+' PNG for tick '+result.tick+' at '+result.file.path);
   } else if (/\b(layers?|objects?)\b/i.test(goal)) {
     const result=await invoke('tile.level',{limit:1});
     const layers=result.settings.planes||[];
     message(result.settings.title+' has '+result.total+' objects. '+(layers.length?layers.map(layer=>
       layer.name+' at Z '+layer.z+(layer.visible?'':' (hidden)')).join(' · '):'It uses the automatic layers.'));
   } else {
     const result=await invoke('tile.ticks',{start:tick?Number(tick[1]):0,limit:1});
     if (tick) {
       const row=result.rows[0];
       if (!row) message('There is no tick '+tick[1]+' in this playtest.');
       else message('Tick '+row[0]+' · position ('+row[1].toFixed(2)+', '+row[2].toFixed(2)+') px · score '+row[15]+
         '. '+(row[17].map(event=>event[0]+(event[1]?' '+event[1]:'')).join(' · ')||'No events at this tick.'));
     } else message('The tile playtest is at tick '+result.lastTick+' at '+result.hz+' Hz, with a score of '+result.score+'.');
   }
 } catch (error) {message('I could not read the tile evidence. '+error.message.replace(/^Error invoking remote method '[^']+': (?:Error: )?/,''));}
 return true;
}
async function openWorkspaceCapability(id) {
 if(id==='home'){setCanvasMode('home');return;}
 if(id==='help'){shell.showTools();return;}
 if(['browser','project','project-skills'].includes(id)){
  if(id==='project')$('artifacts-tab').click();
  if(id==='project-skills')$('skills-tab').click();
  shell.showSide(id==='project-skills'?'project':id);
  return;
 }
 if(id==='browser-read'){
  shell.showSide('browser');
  const page=await request('tool',{tool:'browser.observe',args:{}});
  const title=page.title||'Current page';
  const visible=(page.text||'').replace(/\s+/g,' ').trim();
  message(title+'\n'+page.url+'\n'+(visible?visible.slice(0,450)+(visible.length>450?'…':''):'The page has no visible text.'));
  shell.updateContext();
  return;
 }
 if(id==='activity'){
  if(document.querySelector('.timeline').classList.contains('activity-collapsed099'))$('activity-toggle099').click();
  return;
 }
 if(['environment','floor-plan','height-layers','desk-recording'].includes(id)){
  setCanvasMode('environment');
  await environment.pending;
  const panel={'floor-plan':environment.floorPlan,'height-layers':environment.heightLayers,'desk-recording':environment.desk}[id];
  if(panel)revealWorkspacePanel(panel.root);
  shell.updateContext();
  return;
 }
 if(id==='motion-media'){showMedia('motion');await motionMedia.pending;return;}
 if(id==='tile-workshop'){showMedia('tiles');return;}
 if(id==='media-timeline'){showMedia('timeline');return;}
 if(id==='sprite-editor'){showMedia('sprite');return;}
 if(id==='avatar-studio'){showMedia('avatar');await avatars.pending;return;}
 if(id==='preview'||id==='source'){setCanvasMode(id);return;}
 if(id==='game'){ $('game-tab').click(); return; }
 if(id==='knowledge'){await window.knowledgeView.open();return;}
 const target=CAPABILITIES.find(row=>row.id===id)?.target;
 if(target)document.querySelector(target)?.click();
}

function revealWorkspacePanel(details) {
 details.open=true;
 const stage=details.parentElement;
 stage.scrollTop+=details.getBoundingClientRect().top-stage.getBoundingClientRect().top-12;
}

function workspaceContext(mode) {
 if(mode==='browser')return 'Browser · '+$('browser-title').textContent;
 if(mode==='environment')return environment.catalog?'Room model · '+environment.catalog.name:'Environment · import a room GLB';
 if(mode==='project')return 'Project files and saved results';
 if(mode==='media'&&mediaMode==='motion')return motionMedia.source.value?'Motion recording · '+motionMedia.source.selectedOptions[0]?.textContent:'Motion media · select a recording';
 if(mode==='scene'&&lab.scene)return 'Physics scene · '+lab.scene.title;
 if(mode==='spatial'&&spatial.scene)return 'Spatial scene · '+spatial.scene.title;
 if(mode==='preview'||mode==='source')return artifact?'Artifact · '+artifact.title:'Project source';
 if(mode==='home')return state?.models?.length?'Local model available':'Direct tools available offline';
 return $('artifact-title').textContent||'Workspace ready';
}

function askForRoom(resume) {
 message('Choose a room model to continue this request.');
 shell.needInput({title:'Room model needed',description:'Use a local GLB for the current request.',
  choose:{label:'Choose room GLB',action:()=>$('environment-file').click()},
  canContinue:()=>Boolean(environment.catalog),resume});
}

async function measureWorkspaceLayers() {
 await openWorkspaceCapability('height-layers');
 if(!environment.catalog){askForRoom(measureWorkspaceLayers);return;}
 const result=await environment.heightLayers.measure();
 revealWorkspacePanel(environment.heightLayers.root);
 shell.setStageStatus('Measured');
 message('I measured '+result.bands.length+' height bands. '+result.solidBandCells+' filled cells agree across their cuts and '+
  result.varyingBandCells+' vary. The band diagrams and audit are open.');
}

async function makeWorkspaceFloorPlan(goal) {
 await openWorkspaceCapability('floor-plan');
 if(!environment.catalog){askForRoom(()=>makeWorkspaceFloorPlan(goal));return;}
 const height=goal.match(/(-?\d+(?:\.\d+)?)\s*(?:m\b|metres\b|meters\b)/i);
 const section=await environment.floorPlan.section(height?Number(height[1]):undefined);
 revealWorkspacePanel(environment.floorPlan.root);
 shell.setStageStatus('Measured');
 message('I made a horizontal floor plan at '+section.heightM+' m. It has '+section.occupiedCells+
  ' filled cells and '+section.openEdges+' open edges. The diagram and export controls are open.');
}

async function resumeDeskRecording() {
 await openWorkspaceCapability('desk-recording');
 await environment.desk.restore();
 if(!environment.desk.mission&&!environment.catalog){askForRoom(resumeDeskRecording);return;}
 if(!environment.desk.mission&&(!environment.desk.file||!$('desk-setup').value.trim())) {
  message('Choose the recording and its mission setup to continue.');
  shell.needInput({title:'Recording sources needed',description:'Choose an existing recording and its landmark and availability setup.',
   choose:{label:'Choose existing footage',action:()=>$('desk-footage').click()},
   canContinue:()=>Boolean(environment.desk.file&&$('desk-setup').value.trim()),resume:resumeDeskRecording});
  return;
 }
 const mission=await environment.desk.finish();
 shell.setStageStatus(mission.anchorFailure?'Anchor needs review':'Draft ready',mission.anchorFailure?'needs-input':'');
 message(mission.anchorFailure?
  'The preview and local calendar draft are open. The camera anchor still needs review.':
  'The desk recording, checked preview and local calendar draft are open.');
}

async function buildAvatarFromChat() {
 const result=await avatars.run(()=>avatars.build());
 if(result){
  shell.setStageStatus('Saved');
  message('Avatar portrait, transparent character and walk sheet are ready.');
 }else message(avatars.find('status').textContent);
}

async function buildWorkspaceMotionMedia() {
 await openWorkspaceCapability('motion-media');
 if(!motionMedia.source.value)await motionMedia.action(()=>motionMedia.refresh());
 if(!motionMedia.source.value) {
  const hasChoices=motionMedia.source.options.length>1;
  message(hasChoices?'Choose the motion recording to use. This request is ready to continue.':'This request needs a completed motion recording.');
  shell.needInput({title:'Motion recording needed',description:hasChoices?'Select one of the saved runs in the workspace.':'Use 3D Lab to save a motion run, then refresh recordings.',
   choose:{label:hasChoices?'Choose recording':'Open 3D Lab',action:async()=>{
    if(!hasChoices){await openWorkspaceCapability('scene');return;}
    await openWorkspaceCapability('motion-media');
    motionMedia.source.focus();
   }},
   canContinue:()=>!hasChoices||Boolean(motionMedia.source.value),resume:buildWorkspaceMotionMedia});
  return;
 }
 const output=await motionMedia.action(()=>motionMedia.build());
 if(output)shell.setStageStatus('Verified');
 message(output?'The verified video, audio, poster and timeline are open for review and export.':motionMedia.status.textContent);
}

$('run').onclick=()=>safe(async()=>{
 if(submittingWorkspace)return;
 submittingWorkspace=true;
 $('run').disabled=true;
 try{await submitWorkspaceChat();}
 finally{
  submittingWorkspace=false;
  $('run').disabled=running||creationBusy||dirty||lab.dirty||!hasChatRoute()||
    Boolean(state?.goals?.some(goal=>['running','waiting-host'].includes(goal.status)));
 }
});
async function submitWorkspaceChat() {
 await colliderRecovery;
 if(routingCollider)return;
 requireSaved();
 const goal=$('goal').value.trim();
 if(!goal)return;
 message(goal,'user');
 if(!shell.expert){
  $('goal').value='';
  $('goal').dispatchEvent(new Event('input'));
 }
 shell.clearInput();
 rampFromChat=false;
 const phrasing=workspaceRequestText(goal);
 const capability=matchWorkspaceIntent(phrasing);
 if(capability) {
  try{await openWorkspaceCapability(capability);}
  catch(error){message('Could not open that workspace action. '+error.message);}
  return;
 }
 if (/^(?:plan|run|resume|finish|continue)\b.*\bdesk recording\b/i.test(phrasing)) {
  try{await resumeDeskRecording();}
  catch(error){message('Desk recording: '+error.message);}
  return;
 }
 if (/^(?:measure|audit|compare)\s+(?:the\s+)?(?:environment\s+)?height (?:layers|bands)\b/i.test(phrasing)) {
  try{await measureWorkspaceLayers();}
  catch(error){message('I could not measure the height layers. '+error.message);}
  return;
 }
 if (/^(?:make|draw|show|create|extract|convert|turn)\b.*\b(?:floor[ -]?plan|horizontal (?:section|slice))\b/i.test(phrasing)
     && !/\b(?:into|to)\s+(?:a\s+)?3d\b/i.test(goal)) {
  try{await makeWorkspaceFloorPlan(goal);}
  catch(error){message('I could not make the floor plan. '+error.message);}
  return;
 }
 if (/^(inspect|read|measure|describe|show)\b.*\b(imported environment|environment object|environment objects|environment)\b/i.test(phrasing)
     ||/^what(?:'s| is) in (?:this|the) (?:room|environment)[?.!]?$/i.test(phrasing)) {
  try {
   await openWorkspaceCapability('environment');
   if(!environment.catalog) {
    askForRoom(async()=>{
     await openWorkspaceCapability('environment');
     const verified=await request('environment-evidence',{tool:'environment.read',args:{limit:1}});
     message(verified.environment.name+' is open with '+verified.total+' verified object records.');
    });
    return;
   }
   const result=await request('environment-evidence',{tool:'environment.read',args:{limit:1}});
   const node=result.details;
   message(node?node.name+' is at '+node.position.map(n=>n.toFixed(3)).join(', ')+' m.'+
    (node.dimensions?' Bounds size '+node.dimensions.map(n=>n.toFixed(3)).join(' × ')+' m.':'')+
    (node.marker?' Interaction marker: '+node.marker+'.':''):
    result.environment.name+' has '+result.total+' objects, '+result.environment.triangleCount+
    ' triangles, '+result.environment.imageCount+' embedded images and '+result.environment.markerCount+' interaction markers.');
  } catch (error) {message('I could not read the environment. '+error.message);}
  return;
 }
 if(isAvatarRequest(phrasing)){
  await openWorkspaceCapability('avatar-studio');
  if(!avatars.hasSource){
   message('Choose a portrait image or a frame from a clip. You can adjust the head crop and mark the mouth before building.');
   shell.needInput({title:'Portrait source needed',description:'Choose a still image or use the video frame picker in Avatar Studio.',
    choose:{label:'Choose portrait image',action:()=>avatars.chooseImage()},
    canContinue:()=>avatars.hasSource,resume:async()=>{
     await openWorkspaceCapability('avatar-studio');
     await buildAvatarFromChat();
    }});
   avatars.onSource=()=>shell.syncFollowup();
  }else{
   await buildAvatarFromChat();
  }
  return;
 }
 if(await answerTileQuestion(phrasing))return;
 if (isMotionMediaRequest(phrasing)) {
  await buildWorkspaceMotionMedia();
  return;
 }
 const selectedModel=state?.models.some(m=>m.name===$('model').value)?$('model').value:'';
 if(isRecipientApproachRequest(goal)&&!selectedModel&&!colliderQuestion){await spatial.action(()=>spatial.recipient.compareFromChat(goal));return;}
 if(isSpatialHeightRequest(goal)){await spatial.height.openFromChat(goal);return;}
 if(isSpatialCarryRequest(goal)){await spatial.carry.review(true);return;}
 if(isPlacementRequest(goal)){await spatial.action(()=>spatial.placement.openFromChat(goal));return;}
 if(isSpatialPropRequest(goal)){spatial.openProp();return;}
 if(await chartFollowup.start(goal))return;
 if(mayRequestFrayTool(goal)&&await requestsPanel.offerFromChat(goal))return;
 const local=offlineMissionRequest(goal);
 if(!local&&!plannedRampRequest(goal)&&!quickWorkflowRequest(goal)&&await routeColliderChat(goal,selectedModel))return;
 const submission={goal,model:local?'':selectedModel,offerRampInput:!shell.expert,plainOnly:!selectedModel&&!local&&!quickWorkflowRequest(goal),
   planWorld099:document.querySelector('#plan-world099').checked,
   ...(selectedModel&&!local&&!plannedRampRequest(goal)&&!localWorkflowRequest(goal)&&window.browserChecks126?.contract()
     ?{browserContract126:window.browserChecks126.contract()}:{})};
 await submitWorkspaceAgent(submission);
}
$('goal').addEventListener('input',updateChainRecommendation);
$('goal').addEventListener('input',()=>{
 $('run').disabled=running||creationBusy||dirty||lab.dirty||!hasChatRoute()
  ||Boolean(state?.goals?.some(goal=>['running','waiting-host'].includes(goal.status)));
});
$('goal').addEventListener('keydown',e=>{if((e.ctrlKey||e.metaKey)&&e.key==='Enter'){e.preventDefault();$('run').click();}});
$('stop').onclick=()=>safe(()=>request('stop'));
const rampDialog=$('ramp-input-dialog');
const rampDefaults={angleDeg:25,friction:0.08,gravityMps2:9.81,massKg:1,seconds:1};
const rampFields=Object.keys(rampDefaults);
const rampFileHelp='Required columns are angle_deg, friction, gravity_mps2, mass_kg and duration_s.';
let rampRevision, rampCsv, rampImport=0;
let rampFromChat=false,rampReadyFromChat=false;
async function submitWorkspaceAgent(submission) {
 try {
  const result=await request('agent',submission);
  if(!result.needsRampInput)return result;
 } catch(error) {
  if(shell.expert)throw error;
  message(error.message.replace(/^Error invoking remote method '[^']+': (?:Error: )?/,''));
  return;
 }
 rampFromChat=true;
 rampReadyFromChat=false;
 message('Choose the ramp setup to use for this request.');
 shell.needInput({title:'Ramp setup needed',description:'Save or select a ramp revision, then continue the same request.',
  choose:{label:'Choose ramp setup',action:()=>$('ramp-input-open').click()},
  canContinue:()=>rampReadyFromChat,
  resume:()=>{rampFromChat=false;return submitWorkspaceAgent(submission);}});
 $('ramp-input-open').click();
}

function showSecondRamp(second) {
 for(const value of ['compare','chain'])$('ramp-result').querySelector('option[value="'+value+'"]').disabled=!second;
 if(!second)$('ramp-result').value='single';
 updateRampResultOptions();
 $('ramp-second-facts').hidden=!second;
 $('ramp-second-facts').textContent=second?'Second setup · '+second.angleDeg+'° · friction '+second.friction+
  ' · '+second.gravityMps2+' m/s² · '+second.massKg+' kg · '+second.seconds+' s':'';
}
function showRampFields(values, readonly) {
 for(const field of rampFields){
  const input=$('ramp-'+field);
  input.value=values[field];input.readOnly=readonly;
 }
}
function resetRampFile(){
 rampImport++;
 rampCsv=null;
 $('ramp-csv').value='';
 $('ramp-file-note').textContent=rampRevision?.csvSha256?'Saved CSV revision. Import a replacement or use this page.':rampFileHelp;
 $('ramp-new-form').hidden=!rampRevision?.csvSha256;
 $('ramp-input-error').textContent='';
 showRampFields(rampRevision?.values||rampDefaults,Boolean(rampRevision?.csvSha256));
 showSecondRamp(rampRevision?.secondValues);
 $('ramp-input-save').disabled=Boolean(rampRevision?.csvSha256);
 $('ramp-use-current').disabled=!rampRevision;
}
function updateRampResultOptions(){
 const chain=$('ramp-result').value==='chain';
 $('ramp-plan-label').hidden=chain;
 $('ramp-pause-label').textContent=chain?'Pause after the verified comparison':'Pause after reading the table';
}
$('ramp-result').onchange=updateRampResultOptions;
function prepareRampGoal(){
 if(rampFromChat&&shell.pending){
  rampReadyFromChat=Boolean(rampRevision);
  rampDialog.close();
  shell.syncFollowup();
  $('workspace-next-continue')?.focus();
  return;
 }
 const pause=$('ramp-pause').checked?' and pause after reading':'';
 const compare=$('ramp-result').value==='compare';
 const plan=$('ramp-plan').checked;
 if($('ramp-result').value==='chain') $('goal').value="Review a local chain: Compare both ramp setups, then analyze the farther setup's motion."+($('ramp-pause').checked?' Pause after comparison.':'');
 else if(compare) $('goal').value=plan?'Plan a local ramp task'+pause+': Which setup travels farther on the shared clock?'
  :'Compare the local ramp setups'+pause+'.';
 else $('goal').value=(plan?'Plan':'Run')+' the local ramp experiment'+pause+'.';
 $('goal').dispatchEvent(new Event('input'));
 rampDialog.close();
 $('goal').focus();
}
$('ramp-input-open').onclick=()=>safe(async()=>{
 requireSaved();
 rampRevision=await request('rampInput',{command:'read'});
 resetRampFile();
 $('ramp-plan').disabled=!state?.models.length;
 if($('ramp-plan').disabled)$('ramp-plan').checked=false;
 rampDialog.showModal();
 updateLayout();
});
$('ramp-input-close').onclick=()=>rampDialog.close();
$('ramp-use-current').onclick=prepareRampGoal;
$('ramp-new-form').onclick=()=>{rampRevision=null;resetRampFile();};
rampDialog.addEventListener('close',()=>{rampImport++;updateLayout();});
$('ramp-catalog').addEventListener('toggle',()=>safe(async()=>{
 if(!$('ramp-catalog').open)return;
 const {stages}=await request('rampInput',{command:'catalog'});
 const labels={browser:'Read the browser table',experiment:'Run native physics',replay:'Compare a fresh replay',
  artifact:'Build the report',canvas:'Check the report in Chromium'};
 $('ramp-catalog-steps').replaceChildren(...stages.map(stage=>{
  const row=document.createElement('li');row.textContent=labels[stage.id];return row;
 }));
 updateLayout();
}));
$('ramp-csv').onchange=async()=>{
 const file=$('ramp-csv').files[0];
 if(!file){resetRampFile();return;}
 const version=++rampImport;
 rampCsv=null;
 $('ramp-input-error').textContent='';
 $('ramp-input-save').disabled=true;
 try{
  if(file.size>8192)throw Error('Choose a CSV under 8 KiB');
  const csv=new TextDecoder('utf-8',{fatal:true,ignoreBOM:true}).decode(await file.arrayBuffer());
  const parsed=await request('rampInput',{command:'preview-csv',args:{csv}});
  if(version!==rampImport)return;
  rampCsv=csv;
  showRampFields(parsed,true);
  showSecondRamp(parsed.secondValues);
  $('ramp-file-note').textContent='CSV checked. Its original bytes will stay with the experiment.';
  $('ramp-new-form').hidden=false;
  $('ramp-input-save').disabled=false;
 }catch(error){
  if(version!==rampImport)return;
  $('ramp-input-error').textContent=error.message;
  $('ramp-new-form').hidden=false;
 }
};
$('ramp-input-form').onsubmit=async event=>{
 event.preventDefault();
 const save=$('ramp-input-save');
 save.disabled=true;
 try{
  const values=Object.fromEntries(rampFields.map(field=>[field,$('ramp-'+field).valueAsNumber]));
  const data=rampCsv===null?{values}:{csv:rampCsv};
  rampRevision=await request('rampInput',{command:'save',args:{...data,...(rampRevision?{id:rampRevision.id,expectedVersion:rampRevision.version}:{})}});
  prepareRampGoal();
 }catch(error){$('ramp-input-error').textContent=error.message;}
 finally{save.disabled=false;}
};
$('observe-browser').onclick=()=>safe(async()=>{const result=await request('tool',{tool:'browser.observe',args:{}});$('observation').textContent=JSON.stringify(result,null,2);$('inspection').showModal();updateLayout();});
$('close-inspection').onclick=()=>{$('inspection').close();updateLayout();};$('inspection').addEventListener('close',updateLayout);
for(const tab of ['artifacts','skills','learning','scenes'])$(tab+'-tab').onclick=()=>{libraryTab=tab;for(const name of ['artifacts','skills','learning','scenes'])$(name+'-tab').classList.toggle('selected',name===tab);renderLibrary();};
for(const [id,operation]of [['learning-proof','learningProof'],['learning-evaluate','learningEvaluate']])$(id).onclick=()=>safe(async()=>{requireSaved();notice('Recording outcomes and evaluating a bounded controller');const result=await request(operation);notice(result.unchanged?'No new candidate is needed for the current policy':result.accepted?'Controller improved and checkpointed after independent replay':'Candidate rejected; active controller retained');});
$('learning-rollback').onclick=()=>safe(async()=>{requireSaved();await request('learningRollback');notice('Previous controller restored; evidence history retained');});
$('close-learning').onclick=()=>{$('learning-inspection').close();updateLayout();};$('learning-inspection').addEventListener('close',updateLayout);
for(const tab of ['preview','source'])$(tab+'-tab').onclick=()=>safe(()=>{requireSaved();setCanvasMode(tab);});
$('scene-tab').onclick=()=>safe(async()=>{requireSaved();if(lab.scene)setCanvasMode('scene');else if(state.scenes?.length)await lab.select(state.scenes[0].id);else await lab.create();});
$('source-file').onchange=()=>{if(dirty){$('source-file').value=editingFile;notice('Save or revert source before switching files');return;}editingFile=$('source-file').value;$('source').value=artifact?.contents?.[editingFile]||'';};
$('source').oninput=()=>{dirty=true;$('artifact-version').textContent='Unsaved source changes';$('run').disabled=true;updateChainRecommendation();};
$('revert-source').onclick=()=>{dirty=false;$('source').value=artifact?.contents?.[$('source-file').value]||'';$('artifact-version').textContent='v'+artifact.version+' · saved';$('run').disabled=running||!hasChatRoute();updateChainRecommendation();};
$('save-source').onclick=()=>safe(async()=>{if(!artifact)return;await request('sourceSave',{args:{id:artifact.id,expectedVersion:artifact.version,title:artifact.title,files:{[$('source-file').value]:$('source').value}}});});
$('test-canvas').onclick=()=>safe(async()=>{requireSaved();const result=await request('tool',{tool:'canvas.test',args:{steps:artifact.test.steps}});notice(result.passed?'Saved assertions passed':'Test failed. Inspect the timeline or ask the agent to repair it.');});
$('save-skill').onclick=()=>safe(async()=>{requireSaved();const name=artifact.title.toLowerCase().replace(/[^a-z0-9]+/g,'-').replace(/^-|-$/g,'').slice(0,50)||'web-artifact';await request('tool',{tool:'skill.save',args:{name:name.length<3?'web-'+name:name,description:artifact.title+' — verified interactive web artifact'}});notice('Verified recipe saved in the project skill library');});
for(const kind of ['browser','canvas'])$('expand-'+kind).onclick=()=>{const className=kind+'-focus';const active=document.body.classList.contains(className);document.body.classList.remove('browser-focus','canvas-focus');if(!active)document.body.classList.add(className);$('expand-browser').textContent=!active&&kind==='browser'?'Restore':'Expand';$('expand-canvas').textContent=!active&&kind==='canvas'?'Restore':'Expand';updateLayout();};
$('expand-browser').addEventListener('click',()=>{
 if(shell&&!shell.expert)shell.showSide(shell.side==='browser'?'':'browser');
});
window.workbench099=new Workspace099(request,updateLayout);
window.procedures127=new Procedures127(request,updateLayout);
window.browserChecks126=new BrowserChecks126();
window.inputs100=new Inputs100Panel(request,source=>creationPanel.open(source));
const interfaceMode=await request('workspace-interface');
shell=new WorkspaceShell({open:openWorkspaceCapability,relayout:updateLayout,request,requireSaved,
 context:workspaceContext,interfaceMode});
environment.onCatalog=()=>shell.updateContext();
motionMedia.source.addEventListener('change',()=>shell.updateContext());
motionMedia.onSources=()=>shell.updateContext();
$('desk-footage').addEventListener('change',()=>shell.syncFollowup());
$('desk-setup').addEventListener('input',()=>shell.syncFollowup());
$('desk-setup-file').addEventListener('change',()=>setTimeout(()=>shell.syncFollowup(),200));
shell.start();
host.onEvent(({type,value})=>{
  window.inputs100.event(type,value);
  if(type==='state'){document.querySelector('.edition').textContent=value.network?.enabled?'OFFLINE / GUARDED':'LOCAL / 01';}
  if(type==='run')for(const element of document.querySelectorAll('#scene-lab input,#scene-lab button,#scene-lab select'))element.disabled=value.active;
  if(type==='state'){renderState(value);shell.updateContext();}
  else if(type==='scene')safe(()=>lab.open(value));
  else if(type==='experiment')lab.accept(value);
  else if(type==='artifact')showArtifact(value);
  else if(type==='view'&&['preview','scene'].includes(value))setCanvasMode(value);
  else if(type==='javaGoal'){goalsPanel.accept(value);forcePanel.accept(value);contactPanel.accept(value);posePanel.accept(value);persistentPanel.accept(value);mapPanel.onGoal(value);spatial.onPushGoal(value);creationBusy=(value.workflow&&!['completed','cancelled'].includes(value.status))||(value.procedure==='creation-web-v1'&&['running','waiting-host'].includes(value.status));$('creation-open').disabled=creationBusy||running;$('run').disabled=creationBusy||running||dirty||lab.dirty||!hasChatRoute();}
  else if(type==='dataset'){dataPanel.selected=value;dataPanel.binding();}
  else if(type==='knowledgeResearch'&&window.knowledgeView.root.open)window.knowledgeView.refresh().catch(e=>notice(e.message));
  else if(type==='agency043')window.agency043View.accept(value);
  else if(type==='activity')timeline(value);
  else if(type==='answer')message(value);
  else if(type==='error')notice(value);
  else if(type==='surface'){if(value.kind==='browser'){if(document.activeElement!==$('address'))$('address').value=value.url;$('browser-title').textContent=value.title||value.url;shell.updateContext();}}
  else if(type==='run'){running=value.active;$('stop').disabled=!running;$('run').disabled=running||creationBusy||dirty||lab.dirty||!hasChatRoute();$('goal').disabled=running;$('run-state').textContent=running?(value.phase||(value.turn?'Planning step '+value.turn+' / 16':'Starting task')):'Ready when you are';for(const id of ['new-artifact','navigate','back','forward','proof','test-canvas','save-source','source-file','revert-source','observe-browser','ramp-input-open','relay-review-open','learning-proof','learning-evaluate','learning-rollback'])$(id).disabled=running;$('learning-rollback').disabled=running||(state?.learning?.activePolicy.version||1)===1;$('source').readOnly=running;$('test-canvas').disabled=running||!artifact?.test?.steps?.length;$('save-skill').disabled=running||!artifact?.test?.passed;updateChainRecommendation();renderLibrary();}
  if(type==='run')$('creation-open').disabled=value.active||creationBusy;
  if(type==='run'){spatial.agentBusy=value.active;spatial.controls();}
  if(type==='perceptionFrame')perceptionPanel.accept(value).catch(e=>notice(e.message));
  if(type==='run'){perceptionPanel.agentBusy=value.active;perceptionPanel.controls();}
  if(type==='run'){trackingPanel.agentBusy=value.active;trackingPanel.controls();}
  if(type==='nativeVideo')files.video(value);
  if(type==='nativeExport')files.exported(value);
  if(type==='observerCapture')observers.captured(value);
  if(type==='run'){bodies.controls();files.controls();observers.controls();calibrations.controls();if(!value.active&&$('calibration-panel').open)safe(()=>calibrations.refresh());}
});
goalsPanel.dialog.addEventListener('close',updateLayout);
const colliderRecovery = spatial.call('recipient-route-latest')
 .then(route=>showColliderQuestion(route,true)).catch(error=>notice(error.message));
safe(async()=>{const initial=await request('state');renderState(initial);if(initial.currentArtifact)showArtifact(initial.currentArtifact);if(shell.stage==='home')setCanvasMode('home');for(const entry of initial.log||[])timeline(entry);if(initial.origin)$('address').value=initial.origin+'/welcome';notice('Connected. Your Studio baseline and personal state are separate.');updateLayout();await chartFollowup.refresh();});
setInterval(()=>{if(!running)host.request({operation:'state'}).then(renderState).catch(e=>notice(e.message));},12000);
