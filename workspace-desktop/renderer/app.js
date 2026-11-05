import {Construction051} from './construction051.js';
import {Workshop050} from './workshop050.js';
import {Agency043Panel} from './agency043.js';
import {Agent042Panel} from './agent042.js';
import {Alignment041Panel} from './alignment041.js';
import {SharedSpatial040} from './spatial-runtime040.js';
import {KnowledgePanel} from './knowledge-panel.js';
import {TabletopPanel} from './tabletop-panel.js';
import {MetricPanel} from './metric-panel.js';
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
import {SpatialPanel} from './spatial-panel.js';
import {DataPanel} from './data-panel.js';
import {GoalsPanel} from './goals-panel.js';
import {WorldPanel} from './world-panel.js';
import {BrainGame} from './brain-game.js';
import {SceneLab} from './scene-lab.js';
import {MeasurementPanel} from './measurement-panel.js';
import {CalibrationPanel} from './calibration-panel.js';
import {ObserverPanel} from './observer-panel.js';
import {BodyPanel} from './body-panel.js';
import {SceneFiles} from './scene-files.js';
import {exportSceneVideo} from './scene-video.js';
const $=id=>document.getElementById(id),host=window.workspace;
let state,artifact,editingFile='',libraryTab='artifacts',sourceMode=false,dirty=false,running=false,creationBusy=false,events=[];
let sceneMode=false;
const lab=new SceneLab({call:(command,args)=>request('scene',{command,args}),notice,activate:()=>setCanvasMode('scene'),dirtyChanged:()=>{if(state)$('run').disabled=running||creationBusy||dirty||lab.dirty||!state.models.length;}});
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
const spatial=new SpatialPanel({call:(command,args={})=>request('spatial',{command,args}),pushCall:(command,args={})=>request('push',{command,args}),goalCall:(command,args={})=>request('javaGoal',{command,args}),notice,activate:setCanvasMode,goal:g=>goalsPanel.accept(g)});
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
window.observationView=observationPanel;
window.metricView=new MetricPanel({call:(command,args={})=>request('metrics',{command,args}),observation:observationPanel,notice});
window.tabletopView=new TabletopPanel({call:(command,args={})=>request('tabletop',{command,args}),metricCall:(command,args={})=>request('metrics',{command,args}),persistent:persistentPanel,metric:window.metricView,notice});
window.knowledgeView=new KnowledgePanel({call:(command,args={})=>request('knowledge',{command,args}),tabletop:window.tabletopView,notice,activate:mode=>{window.alignment041?.close();window.sharedSpatial040?.close();setCanvasMode(mode);}});
window.sharedSpatial040=new SharedSpatial040({call:(command,args={})=>request('runtime040',{command,args}),spatial,persistent:persistentPanel,tabletop:window.tabletopView,notice,activate:mode=>{window.alignment041?.close();window.sharedSpatial040?.close();setCanvasMode(mode);}});
window.alignment041=new Alignment041Panel({call:(command,args={})=>request('alignment041',{command,args}),runtime:(command,args={})=>request('runtime040',{command,args}),spatial,media:window.tabletopView,notice,activate:setCanvasMode});
const perceptionPanel=new PerceptionPanel({call:(command,args={})=>request('perception',{command,args}),goalCall:(command,args={})=>request('javaGoal',{command,args}),notice,activate:setCanvasMode,goal:g=>goalsPanel.accept(g)});
const trackingPanel=new TrackingPanel({call:(command,args={})=>request('tracking',{command,args}),notice,requireSaved,openPerception:()=>perceptionPanel.open()});
const creationPanel=new CreationPanel({layout:updateLayout,call:(command,args,model)=>request('creation',{command,args,model}),getState:()=>state,getArtifact:()=>artifact,getModel:()=>$('model').value,requireSaved,notice,onGoal:g=>goalsPanel.accept(g)});
const workflowPanel=new WorkflowPanel({layout:updateLayout,call:(command,args)=>request('workflow',{command,args}),getState:()=>state,getModel:()=>$('model').value,requireSaved,notice,onGoal:w=>goalsPanel.accept(w),openFirstSkill:()=>persistentPanel.showFirstSkill()});
const proceduralPanel=new ProceduralPanel({layout:updateLayout,call:(command,args)=>request('javaGoal',{command,args}),requireSaved,notice,onGoal:g=>goalsPanel.accept(g)});
const mapPanel=new MapPanel({controlCall:args=>host.request({operation:'maps',command:'control',args}),call:(command,args={})=>request('maps',{command,args}),goalCall:(command,args={})=>request('javaGoal',{command,args}),notice,activate:setCanvasMode,goal:g=>goalsPanel.accept(g),requireSaved});
window.nativeMap={snapshot:()=>({mapId:mapPanel.map?.id,avatar:mapPanel.avatar?.position.toArray(),trace:mapPanel.history.length}),open:id=>mapPanel.open().then(()=>id?mapPanel.selectWorld(id):undefined)};
function setCanvasMode(mode){mapPanel.visible=mode==='maps';$('maps-view').hidden=!mapPanel.visible;document.body.classList.toggle('maps-active',mapPanel.visible);perceptionPanel.visible=mode==='perception';$('perception-view').hidden=!perceptionPanel.visible;document.body.classList.toggle('perception-active',perceptionPanel.visible);spatial.visible=mode==='spatial';$('spatial-view').hidden=!spatial.visible;document.body.classList.toggle('spatial-active',spatial.visible);$('game-view').hidden=mode!=='game';sceneMode=mode==='scene';sourceMode=mode==='source';lab.visible=sceneMode;$('scene-lab').hidden=!sceneMode;$('source-editor').hidden=!sourceMode;$('canvas-surface').hidden=sourceMode||sceneMode||mode==='game'||mode==='spatial'||mode==='perception'||mode==='maps';document.body.classList.toggle('scene-active',sceneMode);for(const name of ['preview','source','scene','spatial','perception','maps','game'])$(name+'-tab').classList.toggle('selected',name===mode);for(const id of ['test-badge','test-canvas','save-skill'])$(id).hidden=sceneMode||mode==='game'||mode==='spatial'||mode==='perception'||mode==='maps';$('canvas-kind').textContent=sceneMode?'3D · native physics':'HTML · CSS · JavaScript';if(sceneMode){libraryTab='scenes';for(const name of ['artifacts','skills','learning','scenes'])$(name+'-tab').classList.toggle('selected',name==='scenes');renderLibrary();$('artifact-title').textContent=lab.scene?.title||'3D mechanics lab';$('artifact-version').textContent=lab.scene?`Scene v${lab.scene.version} · SI units · 240 Hz physics`:'Open a scene';$('object-detail').textContent=lab.scene?.id||'Persistent scene entities';lab.resize();}else if(mode==='perception'){$('artifact-title').textContent='Browser perception';$('artifact-version').textContent='Actual pixels · qualified labels · temporary track IDs';$('canvas-kind').textContent='Browser · observe / teach / trace';}else if(mode==='maps'){$('artifact-title').textContent='Playable reference world';$('artifact-version').textContent='Original image/grid · native controls · measured feedback';$('canvas-kind').textContent='3D physics · explicit map layer';mapPanel.resize();}else if(mode==='spatial'){$('artifact-title').textContent='Spatial workspace';$('artifact-version').textContent='Declared-scale source · native forecasts and manipulation';$('canvas-kind').textContent='3D · imported box scenes';spatial.resize();}else if(mode==='game'){$('artifact-title').textContent='Integrated Java game';$('artifact-version').textContent='Original renderer · bounded runtime controls';$('canvas-kind').textContent='Game · XY simulation';}else if(artifact)showArtifact(artifact);updateLayout();}
function message(text,role='assistant'){const item=document.createElement('div');item.className='message '+role;item.textContent=text;$('messages').append(item);item.scrollIntoView({block:'end'});}
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
    const hasWorldSkill=items.some(item=>item.kind==='persistent-world-arrange-v1');
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
    button.onclick=()=>safe(async()=>{requireSaved();if(libraryTab==='learning')showLearningRecord(await request('learningRead',{type:item.recordType,id:item.id}),item.recordType);else if(libraryTab==='scenes')await lab.select(item.id);else{setCanvasMode('preview');if(libraryTab==='artifacts')await request('tool',{tool:'canvas.open',args:{id:item.id}});else if(item.kind==='typed-skill-graph-v1'){await window.openSkillGraph046(item);updateLayout();}else if(['gripper-construction-051-v1','construction-workshop-050-v1','persistent-world-arrange-v1','world-reach-report-v1','motion-data-report-v1','spatial-manipulation-v1','spatial-push-v1','spatial-arrange-v1','browser-perception-v1','creation-web-v1','evidence-workflow-v1','procedural-space-v1','map-world-v1'].includes(item.kind)){goalsPanel.showProcedure(item);updateLayout();}else await request('tool',{tool:'skill.run',args:{name:item.name}});}});$('library-items').append(button);}
    if(!items.length){const empty=document.createElement('p');empty.className='muted';empty.textContent=libraryTab==='learning'?'Task episodes, candidates and measured replay results appear here.':libraryTab==='scenes'?'Open 3D Lab to create a real ramp scene.':libraryTab==='skills'?'Passing recipes appear here after you save them.':'Create your first artifact.';$('library-items').append(empty);}
}
function renderState(value){state=value;const workflow=value.workflows?.find(w=>!['completed','cancelled'].includes(w.status))||value.workflows?.at(-1),last=value.goals?.at(-1),selectedGoal=workflow&&(!last||last.workflowId===workflow.id||!['completed','cancelled'].includes(workflow.status)||workflow.updatedAt>=last.updatedAt)?workflow:last;if(selectedGoal){goalsPanel.accept(selectedGoal);creationBusy=(selectedGoal.workflow&&!['completed','cancelled'].includes(selectedGoal.status))||(selectedGoal.procedure==='creation-web-v1'&&['running','waiting-host'].includes(selectedGoal.status));$('creation-open').disabled=creationBusy||running;}$('engine-state').textContent=value.discovering?'Discovering models':'Java engine online · '+value.models.length+' local models';const selected=$('model').value;const options=value.models.map(m=>m.name);if([...$('model').options].map(o=>o.value).join()!==options.join()){$('model').replaceChildren(...options.map(name=>{const o=document.createElement('option');o.value=name;o.textContent=name;return o;}));if(options.includes(selected))$('model').value=selected;else if(options.includes(localStorage.getItem('selectedModel')))$('model').value=localStorage.getItem('selectedModel');else if(options.includes('gpt-oss:20b'))$('model').value='gpt-oss:20b';else if(options.includes('eyeoverthink/FRAYNIX-PROTOCOL:3b'))$('model').value='eyeoverthink/FRAYNIX-PROTOCOL:3b';}$('run').disabled=running||creationBusy||dirty||lab.dirty||!value.models.length||value.goals?.some(g=>['running','waiting-host'].includes(g.status));$('controller-version').textContent='Controller v'+(value.learning?.activePolicy.version||1);$('episode-count').textContent=(value.learning?.episodeCount||0)+' remembered '+((value.learning?.episodeCount||0)===1?'episode':'episodes');$('learning-rollback').disabled=running||(value.learning?.activePolicy.version||1)===1;renderLibrary();}
function showArtifact(value){artifact=value;dirty=false;$('artifact-title').textContent=value.title;$('artifact-version').textContent='v'+value.version+' · '+value.files.length+' files · saved';$('object-detail').textContent=value.id+' · artifact component';const passed=value.test?.passed;$('test-badge').className='badge'+(value.test?(passed?' pass':' fail'):'');$('test-badge').textContent=value.test?(passed?'TESTS PASSED':'TESTS FAILED'):'UNTESTED';$('save-skill').disabled=!passed||running;$('test-canvas').disabled=running||!value.test?.steps?.length;$('source-file').replaceChildren(...value.files.map(name=>{const o=document.createElement('option');o.textContent=name;o.value=name;return o;}));editingFile=$('source-file').value;$('source').value=value.contents?.[editingFile]||'';renderLibrary();}
let layoutPending=false;
function updateLayout(){if(layoutPending)return;layoutPending=true;requestAnimationFrame(()=>{layoutPending=false;const bounds={};for(const kind of ['browser','canvas']){const r=$(kind+'-surface').getBoundingClientRect();bounds[kind]={x:r.x,y:r.y,width:r.width,height:r.height,visible:r.width>0&&r.height>0&&(kind!=='canvas'||!sourceMode)&&!document.querySelector('dialog[open]')};}host.request({operation:'layout',bounds}).catch(()=>{});});}
new ResizeObserver(updateLayout).observe(document.querySelector('main'));for(const kind of ['browser','canvas'])new ResizeObserver(updateLayout).observe($(kind+'-surface'));
function safe(action){Promise.resolve().then(action).catch(e=>notice(e.message));}
$('model').onchange=()=>localStorage.setItem('selectedModel',$('model').value);
$('navigate').onclick=()=>safe(async()=>{await request('tool',{tool:'browser.navigate',args:{url:$('address').value}});});
$('address').addEventListener('keydown',e=>{if(e.key==='Enter')$('navigate').click();});
$('back').onclick=()=>safe(()=>request('back'));$('forward').onclick=()=>safe(()=>request('forward'));
$('new-artifact').onclick=()=>safe(()=>{requireSaved();setCanvasMode('preview');return request('new');});
$('proof').onclick=()=>safe(async()=>{requireSaved();$('proof').disabled=true;notice('Running the real browser → artifact → test → skill proof');try{await request('proof');notice('Guided proof passed: browser, runnable app, tests and skill saved');}finally{$('proof').disabled=false;}});
$('game').onclick=()=>safe(async()=>{await brainGame.openIntegrated();notice('Original Java game runtime is integrated here; no separate editor window');});
$('run').onclick=()=>safe(async()=>{requireSaved();const goal=$('goal').value.trim();if(!goal)return;message(goal,'user');await request('agent',{goal,model:$('model').value});});
$('goal').addEventListener('keydown',e=>{if(e.ctrlKey&&e.key==='Enter')$('run').click();});
$('stop').onclick=()=>safe(()=>request('stop'));
$('observe-browser').onclick=()=>safe(async()=>{const result=await request('tool',{tool:'browser.observe',args:{}});$('observation').textContent=JSON.stringify(result,null,2);$('inspection').showModal();updateLayout();});
$('close-inspection').onclick=()=>{$('inspection').close();updateLayout();};$('inspection').addEventListener('close',updateLayout);
for(const tab of ['artifacts','skills','learning','scenes'])$(tab+'-tab').onclick=()=>{libraryTab=tab;for(const name of ['artifacts','skills','learning','scenes'])$(name+'-tab').classList.toggle('selected',name===tab);renderLibrary();};
for(const [id,operation]of [['learning-proof','learningProof'],['learning-evaluate','learningEvaluate']])$(id).onclick=()=>safe(async()=>{requireSaved();notice('Recording outcomes and evaluating a bounded controller');const result=await request(operation);notice(result.unchanged?'No new candidate is needed for the current policy':result.accepted?'Controller improved and checkpointed after independent replay':'Candidate rejected; active controller retained');});
$('learning-rollback').onclick=()=>safe(async()=>{requireSaved();await request('learningRollback');notice('Previous controller restored; evidence history retained');});
$('close-learning').onclick=()=>{$('learning-inspection').close();updateLayout();};$('learning-inspection').addEventListener('close',updateLayout);
for(const tab of ['preview','source'])$(tab+'-tab').onclick=()=>safe(()=>{requireSaved();setCanvasMode(tab);});
$('scene-tab').onclick=()=>safe(async()=>{requireSaved();if(lab.scene)setCanvasMode('scene');else if(state.scenes?.length)await lab.select(state.scenes[0].id);else await lab.create();});
$('source-file').onchange=()=>{if(dirty){$('source-file').value=editingFile;notice('Save or revert source before switching files');return;}editingFile=$('source-file').value;$('source').value=artifact?.contents?.[editingFile]||'';};
$('source').oninput=()=>{dirty=true;$('artifact-version').textContent='Unsaved source changes';$('run').disabled=true;};
$('revert-source').onclick=()=>{dirty=false;$('source').value=artifact?.contents?.[$('source-file').value]||'';$('artifact-version').textContent='v'+artifact.version+' · saved';$('run').disabled=running||!state.models.length;};
$('save-source').onclick=()=>safe(async()=>{if(!artifact)return;await request('sourceSave',{args:{id:artifact.id,expectedVersion:artifact.version,title:artifact.title,files:{[$('source-file').value]:$('source').value}}});});
$('test-canvas').onclick=()=>safe(async()=>{requireSaved();const result=await request('tool',{tool:'canvas.test',args:{steps:artifact.test.steps}});notice(result.passed?'Saved assertions passed':'Test failed. Inspect the timeline or ask the agent to repair it.');});
$('save-skill').onclick=()=>safe(async()=>{requireSaved();const name=artifact.title.toLowerCase().replace(/[^a-z0-9]+/g,'-').replace(/^-|-$/g,'').slice(0,50)||'web-artifact';await request('tool',{tool:'skill.save',args:{name:name.length<3?'web-'+name:name,description:artifact.title+' — verified interactive web artifact'}});notice('Verified recipe saved in the project skill library');});
for(const kind of ['browser','canvas'])$('expand-'+kind).onclick=()=>{const className=kind+'-focus';const active=document.body.classList.contains(className);document.body.classList.remove('browser-focus','canvas-focus');if(!active)document.body.classList.add(className);$('expand-browser').textContent=!active&&kind==='browser'?'Restore':'Expand';$('expand-canvas').textContent=!active&&kind==='canvas'?'Restore':'Expand';updateLayout();};
host.onEvent(({type,value})=>{
  if(type==='state')document.querySelector('.edition').textContent=value.network?.enabled?'OFFLINE / GUARDED':'LOCAL / 01';
  if(type==='run')for(const element of document.querySelectorAll('#scene-lab input,#scene-lab button,#scene-lab select'))element.disabled=value.active;
  if(type==='state')renderState(value);
  else if(type==='scene')safe(()=>lab.open(value));
  else if(type==='experiment')lab.accept(value);
  else if(type==='artifact')showArtifact(value);
  else if(type==='view'&&['preview','scene'].includes(value))setCanvasMode(value);
  else if(type==='javaGoal'){goalsPanel.accept(value);forcePanel.accept(value);contactPanel.accept(value);posePanel.accept(value);persistentPanel.accept(value);mapPanel.onGoal(value);spatial.onPushGoal(value);creationBusy=(value.workflow&&!['completed','cancelled'].includes(value.status))||(value.procedure==='creation-web-v1'&&['running','waiting-host'].includes(value.status));$('creation-open').disabled=creationBusy||running;$('run').disabled=creationBusy||running||dirty||lab.dirty||!state?.models.length;}
  else if(type==='dataset'){dataPanel.selected=value;dataPanel.binding();}
  else if(type==='knowledgeResearch'&&window.knowledgeView.root.open)window.knowledgeView.refresh().catch(e=>notice(e.message));
  else if(type==='agency043')window.agency043View.accept(value);
  else if(type==='activity')timeline(value);
  else if(type==='answer')message(value);
  else if(type==='error')notice(value);
  else if(type==='surface'){if(value.kind==='browser'){if(document.activeElement!==$('address'))$('address').value=value.url;$('browser-title').textContent=value.title||value.url;}}
  else if(type==='run'){running=value.active;$('stop').disabled=!running;$('run').disabled=running||creationBusy||dirty||lab.dirty||!state?.models.length;$('goal').disabled=running;$('run-state').textContent=running?(value.phase||(value.turn?'Planning step '+value.turn+' / 16':'Starting task')):'Ready when you are';for(const id of ['new-artifact','navigate','back','forward','proof','test-canvas','save-source','source-file','revert-source','observe-browser','learning-proof','learning-evaluate','learning-rollback'])$(id).disabled=running;$('learning-rollback').disabled=running||(state?.learning?.activePolicy.version||1)===1;$('source').readOnly=running;$('test-canvas').disabled=running||!artifact?.test?.steps?.length;$('save-skill').disabled=running||!artifact?.test?.passed;renderLibrary();}
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
safe(async()=>{const initial=await request('state');renderState(initial);if(initial.currentArtifact)showArtifact(initial.currentArtifact);for(const entry of initial.log||[])timeline(entry);if(initial.origin)$('address').value=initial.origin+'/welcome';notice('Connected. Your Studio baseline and personal state are separate.');updateLayout();});
setInterval(()=>{if(!running)host.request({operation:'state'}).then(renderState).catch(e=>notice(e.message));},12000);
