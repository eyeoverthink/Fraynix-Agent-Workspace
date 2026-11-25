import {RecipientEditPanel} from './recipient-edit.js';
import {RecipientMissionPanel} from './recipient-mission.js';
import {PlacementExchangePanel} from './placement-exchange.js';
import {SpatialPlacementPanel} from './spatial-placement.js';
import {SpatialLibraryViews} from './spatial-library-views.js';
import {SpatialMeshEditPanel} from './spatial-mesh-edit.js';
import {SpatialAnimationPanel} from './spatial-animation.js';
import {SpatialHeightPanel} from './spatial-height.js';
import {SpatialCarryPanel} from './spatial-carry.js';
import {ImageAssemblyPanel} from './image-assembly.js';
import * as THREE from '../node_modules/three/build/three.module.js';
const $=id=>document.getElementById(id);
export const isSpatialPropRequest = text =>
 /^(?:(?:can|could|would) you )?(?:please )?copy (?:and transform )?the selected prop(?: and transform it)?[.!?]?$/i.test(text.trim());

function visualGeometry(encoded){
 const binary=atob(encoded), bytes=Uint8Array.from(binary,letter=>letter.charCodeAt(0));
 const data=new DataView(bytes.buffer);
 const count=data.getUint32(0,true)*3,indexCount=data.getUint32(4,true);
 const vertices=new Float32Array(count), triangles=new Uint16Array(indexCount);
 for(let i=0;i<count;i++)vertices[i]=data.getFloat32(8+4*i,true);
 for(let i=0;i<indexCount;i++)triangles[i]=data.getUint16(8+4*count+2*i,true);
 const geometry=new THREE.BufferGeometry();
 geometry.setAttribute('position',new THREE.BufferAttribute(vertices,3));
 geometry.setIndex(new THREE.BufferAttribute(triangles,1));
 geometry.computeVertexNormals();
 return geometry;
}

export class SpatialPanel {
 constructor({call,pushCall,goalCall,notice,activate,goal,announce,layout}){
  Object.assign(this,{call,pushCall,goalCall,notice,activate,goal,announce,layout});this.visible=false;this.meshes=new Map();this.trace=[];
  const section=document.createElement('section');section.id='spatial-view';section.hidden=true;
  section.innerHTML=`<div class="spatial-top"><div><strong>Spatial workspace</strong><small>Declared-scale boxes · native physics · persistent identities</small></div><button id="spatial-example">Load labelled test scene</button><label class="spatial-file">Import JSON<input id="spatial-file" type="file" accept=".json,application/json" aria-label="Import spatial scene JSON"></label><button id="spatial-import-blender">Import Blender</button></div>
   <div class="spatial-bind"><label>Imported scene<select id="spatial-scenes" aria-label="Imported spatial scene"></select></label><label>Object<select id="spatial-objects" aria-label="Spatial object"></select></label><button id="spatial-frame">Frame scene</button><button id="spatial-top-down" aria-pressed="false">Top down</button><button id="spatial-export">Export scene</button><button id="spatial-export-blender">Export Blender</button><button id="placement-resume">Placement review</button><button id="placement-exchange">Inspect collision evidence</button><button id="recipient-edit" hidden>Review local edit</button><button id="recipient-mission">Collider mission</button><button id="spatial-observe">Check visible objects</button></div>
   <details class="spatial-detail" id="spatial-exchange"><summary>Scene exchange and image blueprints</summary>
    <div class="spatial-task">
     <label class="spatial-file">Import Fraynix GLB<input id="spatial-glb" type="file" accept=".glb" aria-label="Import Fraynix GLB"></label>
     <button id="spatial-export-glb">Export GLB</button>
     <label><input id="spatial-colliders" type="checkbox"> Show prop colliders</label>
     <button id="spatial-prop-open">Copy selected prop</button>
    </div><div class="spatial-task">
     <label>Labelled image from Maps<select id="spatial-blueprint"></select></label>
     <label>One tile measures · m<input id="spatial-blueprint-scale" type="number" min="0.6" max="1.2" step="0.01" value="1"></label>
     <button id="spatial-blueprint-build">Build 3D copy</button>
    </div><p class="spatial-note">Import a labelled PNG in Maps first. The original layout supplies the floor plan. The collider walls are 1.1 m high.</p></details>
   <details class="spatial-detail" id="spatial-browser-views"><summary>Model from registered images</summary>
    <p class="spatial-note">Use equal-sized orthographic images at the same scale and center. Pick the left side, and optionally a top view with the front edge down. The held-out camera is halfway between front and side.</p>
    <div class="spatial-task"><button id="browser-pick-front">Pick front</button><button id="browser-pick-side">Pick side</button>
     <button id="browser-pick-top">Pick top</button><button id="browser-pick-heldOut">Pick held-out 45°</button><button id="browser-top-clear">Remove top</button><button id="browser-views-clear">Clear picks</button></div>
    <div class="spatial-task"><label>Local image role<select id="registered-png-role">
      <option value="front">Front</option><option value="side">Left side</option><option value="top">Top</option><option value="heldOut">Held-out 45°</option>
     </select></label><label class="spatial-file">Load local PNG<input id="registered-png-file" type="file" accept="image/png,.png" aria-label="Load local PNG"></label></div>
    <form id="browser-views-form" class="spatial-task"><label>Front width · m<input id="browser-views-width" type="number" min=".1" max="5" step="any" value=".5" required></label>
     <label>Held-out elevation<select id="browser-views-elevation"><option value="0">Level</option><option value="60">60° above</option></select></label><button id="browser-views-build">Fit and verify model</button><button id="browser-views-place">Fit and review placement</button>
     <button type="button" id="browser-views-resume">Resume browser placement</button><button type="button" id="browser-views-export">Export checked GLB</button></form>
    <p id="browser-views-status" class="spatial-note" role="status">Pick a graphic for each view.</p>
   </details>
   <details class="spatial-detail" id="image-assembly"><summary>Build a scene from images</summary>
    <p class="spatial-note">Fit each prop under Model from registered images first. Choose a marked layout or locate the props by their silhouettes in two registered views.</p>
    <form>
     <div class="spatial-task"><label>Scene name<input name="title" value="Image scene" maxlength="90" required></label>
      <label>Position source<select name="mode"><option value="markers">Color markers</option><option value="views">Unmarked registered views</option></select></label>
      <label>Layout width · m<input name="widthM" type="number" min=".5" max="20" step="any" value="4" required></label>
      <label class="spatial-file"><span id="image-assembly-layout-label">Layout PNG</span><input name="layout" type="file" accept="image/png,.png" required></label></div>
     <div id="image-assembly-witness-fields" hidden>
      <p class="spatial-note">Use an opaque top-down image with separated silhouettes on a plain background. The second camera is 45° toward the left from the front and 60° above the floor. Use the same canvas, center and metres per pixel for both views. Keep each prop axis-aligned.</p>
      <label class="spatial-file">Second-angle PNG<input name="witness" type="file" accept="image/png,.png"></label>
     </div>
     <div class="spatial-assembly-props"></div>
     <div class="spatial-task"><button id="image-assembly-add" type="button">Add prop</button>
      <button id="image-assembly-build" class="primary">Build verified 3D scene</button></div>
    </form>
    <div class="spatial-task"><button id="image-assembly-replay" hidden>Replay floor contact checks</button></div>
    <p class="spatial-note" role="status" id="image-assembly-status"></p>
   </details>
   <details class="spatial-detail" id="spatial-browser-trace"><summary>Trace a browser graphic</summary>
    <form id="spatial-trace-form" class="spatial-task">
     <label>Graphic selector<input id="spatial-trace-selector" placeholder="#product-image" maxlength="180" required></label>
     <label>Width · m<input id="spatial-trace-width" type="number" min=".1" max="5" step="any" value=".6" required></label>
     <label>Thickness · m<input id="spatial-trace-depth" type="number" min=".05" max="2" step="any" value=".1" required></label>
     <button id="spatial-trace-build">Capture and model</button>
    </form><div class="spatial-task"><button id="spatial-trace-export">Export traced GLB</button></div><p id="spatial-trace-status" class="spatial-note" role="status">Choose a fully visible image, SVG or canvas with a plain contrasting background.</p></details>
   <details class="spatial-detail" id="spatial-reconstruct"><summary>Reconstruct editable geometry from images</summary>
    <div class="spatial-task">
     <button id="spatial-reconstruct-example">Reconstruct Blender example</button>
     <button id="spatial-reconstruct-depth-example">Recover hidden recess</button>
     <button id="spatial-reconstruct-opposed-example">Recover opposite recesses</button>
     <button id="spatial-reconstruct-side-example">Recover all three recesses</button>
     <label class="spatial-file">Import reconstruction package<input id="spatial-reconstruct-file" type="file" accept=".json,application/json" aria-label="Import reconstruction package"></label>
     <button id="spatial-reconstruct-export" disabled>Export reconstructed GLB</button>
    </div><p id="spatial-reconstruct-result" class="spatial-note" role="status">Use three registered PNG silhouettes. Add first-hit depth to recover recessed surfaces.</p>
   </details>
   <p id="spatial-source" class="spatial-note">Load the labelled test scene or import a supported box-scene JSON. No captured-room calibration is implied.</p>
   <div id="spatial-stage" aria-label="Spatial native viewport"></div><div id="spatial-live" role="status">Select an imported scene.</div>
   <div class="spatial-task"><label>Relative move X · m<input id="spatial-dx" type="number" value="4" min="-6" max="6" step="0.1"></label><label>Z · m<input id="spatial-dz" type="number" value="0" min="-6" max="6" step="0.1"></label><label>Save verified procedure as<input id="spatial-skill" value="spatial-crate-route" maxlength="56"></label><button id="spatial-preview">Forecast only</button><button id="spatial-run" class="primary">Grab, move &amp; release</button></div>
   <details id="push-panel" class="spatial-detail"><summary>Push an object through native contacts</summary><p>Choose a crate and a separate pusher. The planner checks where the pusher can stand; only the pusher receives force. Supported flat-floor box scenes use integer-metre targets.</p><div class="spatial-task"><label>Labelled test scene<select id="push-variant"><option value="corner">Obstacle turn</option><option value="open">Clear floor</option><option value="high-friction">High friction / recovery</option><option value="blocked">Blocked route</option><option value="unreachable-standing">Unreachable pushing pose</option></select></label><button id="push-example">Load pushing scene</button><label>Pusher body<select id="push-pusher"></select></label></div><div class="spatial-task"><label>Push relative X · m<input id="push-dx" type="number" step="1" min="-4" max="4" value="3"></label><label>Z · m<input id="push-dz" type="number" step="1" min="-4" max="4" value="0"></label><label>Gentle pushing limit · N<input id="push-initial-force" type="number" min="2" max="60" value="8"></label><label>Maximum force · N<input id="push-max-force" type="number" min="2" max="60" value="32"></label><label>Save verified skill as<input id="push-skill" value="contact-crate-route" maxlength="56"></label><button id="push-preview">Plan only</button><button id="push-run" class="primary">Push &amp; verify</button></div><div class="spatial-task"><button id="push-pause">Pause &amp; save both bodies</button><button id="push-resume">Resume saved push</button><button id="push-export">Export full push run</button></div><p id="push-live">No pushing task running. Maximum force also applies during approach and recovery.</p><pre id="push-evidence"></pre></details>
   <p id="spatial-plan" class="spatial-note">The agent tests direct, over and lateral routes using isolated native worlds. Orange is the selected forecast; blue is actual motion. New tasks start from the immutable imported state.</p><details class="spatial-detail"><summary>Identity, source and visibility evidence</summary><pre id="spatial-evidence"></pre></details>`;
  $('game-view').before(section);this.placement=new SpatialPlacementPanel(this);this.exchange=new PlacementExchangePanel(this);this.recipient=new RecipientEditPanel(this,visualGeometry);this.recipientMission=new RecipientMissionPanel(this);this.libraryViews=new SpatialLibraryViews(this);this.timeline=new SpatialAnimationPanel(this);this.meshEdit=new SpatialMeshEditPanel(this,visualGeometry);this.carry=new SpatialCarryPanel(this);this.height=new SpatialHeightPanel(this);const tab=document.createElement('button');tab.id='spatial-tab';tab.textContent='Spatial';$('scene-tab').after(tab);tab.onclick=()=>this.action(()=>this.open());
  $('placement-exchange').onclick=()=>this.action(()=>this.exchange.open());
  this.imageAssembly=new ImageAssemblyPanel(this);
  $('recipient-edit').onclick=()=>this.action(()=>this.recipient.open());
  $('recipient-mission').onclick=()=>this.action(()=>this.recipientMission.open());
  $('placement-resume').onclick=()=>this.action(()=>this.placement.openLatest());
  $('spatial-example').onclick=()=>this.action(async()=>this.import((await this.call('example')).text));
  $('spatial-file').onchange=()=>this.action(async()=>{const f=$('spatial-file').files[0];if(!f)return;if(f.size>131072)throw Error('Scene JSON is limited to 128 KiB');await this.import(await f.text());});
  const copyDialog=document.createElement('dialog');
  copyDialog.id='spatial-prop-dialog';
  copyDialog.setAttribute('aria-labelledby','spatial-prop-title');
  copyDialog.innerHTML='<div class="panel-head"><h2 id="spatial-prop-title">Copy and transform a prop</h2><button id="spatial-prop-close">Close</button></div>'
   +'<form id="spatial-prop-form"><label>Name <input id="spatial-prop-label" maxlength="80" required value="Prop copy"></label>'
   +'<div class="spatial-task"><label>X offset · m <input id="spatial-prop-x" type="number" required value="1.8" step="0.01"></label>'
   +'<label>Y offset · m <input id="spatial-prop-y" type="number" required value="0" step="0.01"></label>'
   +'<label>Z offset · m <input id="spatial-prop-z" type="number" required value="-0.9" step="0.01"></label>'
   +'<label>Yaw · degrees <input id="spatial-prop-yaw" type="number" min="-180" max="180" required value="35" step="1"></label></div>'
   +'<button id="spatial-prop-review">Review copy</button></form><p id="spatial-prop-status" role="status"></p>'
   +'<button id="spatial-prop-approve" class="primary" disabled>Approve and verify</button>';
  document.body.append(copyDialog);
  copyDialog.addEventListener("close",layout);
  $('spatial-prop-close').onclick=()=>copyDialog.close();
  $('spatial-prop-open').onclick=()=>this.action(()=>this.openProp());
  $('spatial-colliders').onchange=()=>this.paintColliders();
  $('spatial-prop-form').oninput=()=>this.clearPropReview();
  $('spatial-prop-form').onsubmit=event=>{event.preventDefault();this.action(()=>this.reviewProp());};
  $('spatial-prop-approve').onclick=()=>this.action(()=>this.approveProp());
  $('spatial-exchange').ontoggle=()=>{if($('spatial-exchange').open)this.action(()=>this.refreshBlueprints());};
  $('spatial-blueprint').onchange=()=>this.controls();
  $('spatial-blueprint-build').onclick=()=>this.action(async()=>{
   const scene=await this.call('blueprint',{referenceId:$('spatial-blueprint').value,cellSizeM:Number($('spatial-blueprint-scale').value)});
   await this.open(scene.id);this.notice('Built an editable 3D scene from the original labelled image.');
  });
  $('spatial-glb').onchange=()=>this.action(async()=>{
   const control=$('spatial-glb'), file=control.files[0];if(!file)return;control.value='';
   if(file.size>524288)throw Error('Choose a Fraynix scene GLB up to 512 KiB.');
   const bytes=new Uint8Array(await file.arrayBuffer());let binary='';
   for(let i=0;i<bytes.length;i+=8192)binary+=String.fromCharCode(...bytes.subarray(i,i+8192));
   const scene=await this.call('import-glb',{base64:btoa(binary)});
   await this.open(scene.id);this.notice('Verified GLB imported as a separate editable scene.');
  });
  this.browserViews={};
  const viewRoles=['front','side','top','heldOut'];
  const viewLabels={front:'front',side:'side',top:'top',heldOut:'held-out 45°'};
  const recordView=(role,capture)=>{
   this.browserViews[role]=capture;
   $('browser-pick-'+role).textContent='Replace '+viewLabels[role];
   const count=['front','side','heldOut'].filter(key=>this.browserViews[key]).length;
   $('browser-views-status').textContent=(capture.fileName?'Loaded ':'Captured ')+viewLabels[role]+' · '+count+' of 3 required views.';
  };
  $('registered-png-file').onchange=()=>{
   const field=$('registered-png-file'),file=field.files[0],role=$('registered-png-role').value;
   field.value='';
   if(!file)return;
   this.action(async()=>{
    if(file.size>204800)throw Error('Each registered PNG must be at most 200 KiB.');
    const bytes=new Uint8Array(await file.arrayBuffer());let binary='';
    for(let i=0;i<bytes.length;i+=8192)binary+=String.fromCharCode(...bytes.subarray(i,i+8192));
    recordView(role,{fileName:file.name,png:btoa(binary)});
   });
  };
  for(const role of viewRoles)$('browser-pick-'+role).onclick=()=>this.action(async()=>{
   const status=$('browser-views-status');
   status.textContent='Click the '+viewLabels[role]+' graphic in the browser. Escape cancels.';
   try{
    recordView(role,await this.call('pick-browser-view'));
   }catch(error){status.textContent=error.message;}
  });
  $('browser-top-clear').onclick=()=>{
   delete this.browserViews.top;
   $('browser-pick-top').textContent='Pick top';
   $('browser-views-status').textContent='Top view removed from the next fit.';
   this.controls();
  };
  $('browser-views-clear').onclick=()=>{
   this.browserViews={};
   for(const role of viewRoles)$('browser-pick-'+role).textContent='Pick '+viewLabels[role];
   $('browser-views-status').textContent='Pick a graphic for each view.';
   this.controls();
  };
  $('browser-views-form').onsubmit=event=>{
   event.preventDefault();
   const forPlacement=event.submitter?.id==='browser-views-place';
   this.action(async()=>{
    const status=$('browser-views-status');
    status.textContent='Fitting registered views, then checking the held-out view…';
    try {
     const result=await this.call('trace-browser-views',{
      views:this.browserViews,widthM:$('browser-views-width').valueAsNumber,
      heldOutElevationDeg:Number($('browser-views-elevation').value),forPlacement
     });
     if(forPlacement)await this.openBrowserPlacement(result);
     else await this.open(result.id);
    }catch(error){status.textContent=error.message;}
   });
  };
  $('browser-views-resume').onclick=()=>this.action(async()=>{
   const status=$('browser-views-status');
   try{
    const saved=await this.call('browser-placement-latest');
    if(!saved.source){status.textContent='Fit and verify a browser model for placement first.';return;}
    await this.openBrowserPlacement(saved);
   }catch(error){status.textContent=error.message;}
  });
  $('browser-views-export').onclick=()=>this.action(()=>this.exportGlb());
  $('spatial-trace-form').onsubmit=event=>{
   event.preventDefault();
   this.action(async()=>{
    const status=$('spatial-trace-status');
    status.textContent='Capturing the selected browser graphic…';
    try {
     const scene=await this.call('trace-browser',{selector:$('spatial-trace-selector').value.trim(),
      widthM:$('spatial-trace-width').valueAsNumber,thicknessM:$('spatial-trace-depth').valueAsNumber});
     await this.open(scene.id);
    }catch(error){status.textContent=error.message;}
   });
  };
  $('spatial-trace-export').onclick=()=>this.action(()=>this.exportGlb());
  $('spatial-reconstruct-export').onclick=()=>this.action(()=>this.exportGlb());
  $('spatial-reconstruct-side-example').onclick=()=>this.action(()=>this.reconstruct('reconstruct-side-example'));
  $('spatial-reconstruct-opposed-example').onclick=()=>this.action(()=>this.reconstruct('reconstruct-opposed-example'));
  $('spatial-reconstruct-depth-example').onclick=()=>this.action(()=>this.reconstruct('reconstruct-depth-example'));
  $('spatial-reconstruct-example').onclick=()=>this.action(()=>this.reconstruct('reconstruct-example'));
  $('spatial-reconstruct-file').onchange=()=>this.action(async()=>{
   const input=$('spatial-reconstruct-file'),file=input.files[0];input.value='';
   if(!file)return;
   if(file.size>1200000)throw Error('Choose a silhouette set up to 1.2 MB.');
   const bytes=new Uint8Array(await file.arrayBuffer());let binary='';
   for(let i=0;i<bytes.length;i+=8192)binary+=String.fromCharCode(...bytes.subarray(i,i+8192));
   await this.reconstruct('reconstruct',{base64:btoa(binary)});
  });
  $('spatial-export-glb').onclick=()=>this.action(()=>this.exportGlb());
  $('spatial-import-blender').onclick=()=>this.action(async()=>{
   const imported=await this.call('import-blender');
   if(imported){await this.open(imported.id);this.notice('Blender scene imported. Check declared scale and source in Spatial.');}
  });
  $('spatial-export-blender').onclick=()=>this.action(async()=>{
   if(!this.scene||this.worldMode)throw Error('Choose an imported source scene');
   const result=await this.call('export-blender',{id:this.scene.id});
   if(result)this.notice('Editable Blender scene saved: '+result.path);
  });
  $('spatial-scenes').onchange=()=>this.action(()=>this.select($('spatial-scenes').value));$('spatial-objects').onchange=()=>this.action(()=>this.selectObject($('spatial-objects').value));
  $('push-example').onclick=()=>this.action(async()=>{await this.import((await this.pushCall('example',{variant:$('push-variant').value})).text);this.pushMode=true;$('push-panel').open=true;await this.pushRoles();});$('push-pusher').onchange=()=>this.action(()=>this.pushRoles($('push-pusher').value));$('push-preview').onclick=()=>this.action(async()=>{const before=await this.pushCall('snapshot');const {useContactController,useActiveProfile,...options}=this.pushOptions();const plan=await this.pushCall('preview',{...this.pushBinding(),...options});if(JSON.stringify(before)!==JSON.stringify(await this.pushCall('snapshot')))throw Error('Pushing plan changed native state');this.showPushPlan(plan);});$('push-run').onclick=()=>this.action(async()=>{const g=await this.pushCall('start',{...this.pushOptions(),saveSkillName:$('push-skill').value.trim()});this.pushMode=true;this.pushGoal=g;this.goal(g);});$('push-pause').onclick=()=>this.action(()=>this.pushCall('pause'));$('push-resume').onclick=()=>this.action(async()=>{if(!this.pushGoal||this.pushGoal.status!=='interrupted')throw Error('Wait for the saved task to pause, then Resume');this.goal(await this.goalCall('goal.resume',{id:this.pushGoal.id}));});$('push-export').onclick=()=>this.action(async()=>{if(!this.pushRunId)throw Error('Select a settled pushing attempt');const r=await this.pushCall('export-run',{id:this.pushRunId});this.notice('Complete native pushing evidence saved: '+r.path);});
  $('spatial-frame').onclick=()=>this.frame();
  $('spatial-top-down').onclick=()=>{
   this.topDown=!this.topDown;
   $('spatial-top-down').setAttribute('aria-pressed',String(this.topDown));
   this.resize();this.controls();
  };
 $('spatial-preview').onclick=()=>this.action(async()=>{this.requireObject();const before=await this.call('snapshot');this.plan=await this.call('preview',{...this.binding(),displacementM:this.delta()});const after=await this.call('snapshot');if(JSON.stringify(before)!==JSON.stringify(after))throw Error('Forecast changed displayed state');this.forecast(this.plan);$('spatial-plan').textContent=`Forecast only · ${this.plan.route} route · ${this.plan.trials.length} finite native trials · source unchanged. ${this.plan.trials.map(t=>`${t.route}: ${t.passed?'feasible':'blocked / rejected'}`).join(' · ')}`;});
  $('spatial-run').onclick=()=>this.action(async()=>{this.requireObject();const g=await this.call('start',{displacementM:this.delta(),saveSkillName:$('spatial-skill').value.trim()});this.goal(g);});
  $('spatial-export').onclick=()=>this.action(async()=>{if(!this.scene)throw Error('Select a scene');const r=await this.call('export',{kind:'scene',id:this.scene.id});this.notice(`Scene JSON saved: ${r.path}`);$('spatial-evidence').textContent=JSON.stringify(r,null,2);});
  $('spatial-observe').onclick=()=>this.action(async()=>{if(!this.scene)throw Error('Select a scene');if(this.timeline.samples||this.meshEdit.preview)throw Error('Exit the edit preview to check native visibility.');if(!this.renderer)throw Error('The 3D preview is unavailable on this display.');const dir=new THREE.Vector3();this.camera.getWorldDirection(dir);const pose={position:this.camera.position.toArray(),yawDeg:Math.atan2(dir.x,-dir.z)*180/Math.PI,pitchDeg:Math.asin(dir.y)*180/Math.PI,fovDeg:this.camera.fov};const r=await (this.worldMode&&this.worldObserve?(_command,args)=>this.worldObserve(args):this.poseMode&&this.poseCall?this.poseCall:this.pushMode?this.pushCall:this.call)('observe',{sceneId:this.scene.id,pose,viewport:{width:Math.max(100,this.renderer.domElement.clientWidth),height:Math.max(100,this.renderer.domElement.clientHeight)}});$('spatial-evidence').textContent=JSON.stringify(r,null,2);$('spatial-plan').textContent=`${r.visibleObjects.length} visible of ${r.knownObjects.length} known objects. Occluded identities remain known; visibility uses rays through native box geometry.`;});
  setInterval(()=>{if(this.visible&&!this.polling)this.poll().catch(e=>this.notice(e.message));},100);new ResizeObserver(()=>this.resize()).observe($('spatial-stage'));
 }
 async action(fn){this.pending=(this.pending||Promise.resolve()).catch(()=>{}).then(async()=>{this.busy=true;this.controls();try{await fn();}catch(e){this.notice(e.message);}finally{this.busy=false;this.controls();}});return this.pending;}
 openProp(){
  if(this.worldMode||!this.object?.visualMesh||!this.object.massKg)throw Error('Select a dynamic detailed prop in Spatial first.');
  this.clearPropReview();
  $('spatial-prop-label').value=this.object.label.slice(0,70)+' copy';
  if(!$('spatial-prop-dialog').open)$('spatial-prop-dialog').showModal();
  this.layout();
 }
 clearPropReview(){
  this.propReview=null;
  $('spatial-prop-status').textContent='';
  $('spatial-prop-approve').disabled=true;
 }
 async reviewProp(){
  const input={label:$('spatial-prop-label').value,yawDeg:Number($('spatial-prop-yaw').value),
   offsetM:['x','y','z'].map(axis=>Number($('spatial-prop-'+axis).value))};
  try{
   this.propReview=await this.call('prop-review',input);
   const review=this.propReview.review;
   $('spatial-prop-status').textContent='New pose '+review.targetM.map(n=>n.toFixed(2)).join(', ')+' m · yaw '
    +review.yawDeg+'° · two isolated '+review.settleTicks+'-tick contact checks';
   $('spatial-prop-approve').disabled=false;
  }catch(error){$('spatial-prop-status').textContent=error.message;}
 }
 async approveProp(){
  if(!this.propReview)return;
  $('spatial-prop-approve').disabled=true;
  $('spatial-prop-status').textContent='Checking contacts in two fresh native worlds.';
  try {
   const scene=await this.call('prop-copy',this.propReview);
   $('spatial-prop-dialog').close();
   await this.open(scene.id);
   await this.selectObject(scene.propCopy.verify.objectId);
   this.announce(this.propSummary(scene.propCopy.verify));
  }catch(error){
   $('spatial-prop-status').textContent=error.message;
   $('spatial-prop-approve').disabled=false;
  }
 }
 propSummary(result){
  return 'Prop copy verified · '+result.visualTriangles+' visual triangles · '+result.coreContacts+' collider contacts'
   +(result.visualOverhangTested?' · '+result.visualOnlyContacts+' contacts at the visual overhang':'')+' · supported by '+result.support;
 }

 async refreshBlueprints(){
  const control=$('spatial-blueprint'), prior=control.value;
  const rows=await this.call('blueprint-list');
  control.replaceChildren(...rows.map(row=>new Option(row.title,row.id)));
  if(rows.some(row=>row.id===prior))control.value=prior;
  if(!rows.length)control.add(new Option('Import a labelled PNG in Maps',''));
 }
 controls(){$('spatial-observe').disabled=!this.renderer||Boolean(this.topDown||this.timeline.samples||this.meshEdit.preview);
  $('spatial-observe').title=(this.timeline.samples||this.meshEdit.preview)?'Exit the edit preview to check native visibility.':this.topDown?'Use the perspective camera to check visible objects.':'';
  const moving=['prepared','executing','running','paused'].includes(this.snapshot?.status);
  const editingBlocked=this.worldMode||this.poseMode||this.evaluationBusy||this.agentBusy||moving;
  this.imageAssembly?.controls(editingBlocked);
  const recipient=Boolean(this.scene?.recipientBranch)&&!this.worldMode;
  $('recipient-edit').hidden=!recipient;
  $('recipient-edit').disabled=this.busy||editingBlocked;
  this.placement.controls();this.exchange.controls();this.recipient.controls();
  this.libraryViews.controls(editingBlocked);
  this.timeline.controls(editingBlocked||recipient);
  this.meshEdit.controls(editingBlocked||recipient,this.busy);
  $('spatial-trace-build').disabled=this.busy||editingBlocked;
  $('registered-png-file').disabled=this.busy||editingBlocked;
  for(const role of ['front','side','top','heldOut'])$('browser-pick-'+role).disabled=this.busy||editingBlocked;
  $('browser-views-clear').disabled=this.busy||!Object.keys(this.browserViews).length;
  $('browser-top-clear').disabled=this.busy||!this.browserViews.top;
  $('browser-views-build').disabled=this.busy||editingBlocked||!this.browserViews.front||!this.browserViews.side||!this.browserViews.heldOut;
  $('browser-views-place').disabled=$('browser-views-build').disabled;
  $('browser-views-resume').disabled=this.busy||editingBlocked;
  $('browser-views-export').disabled=this.busy||editingBlocked||this.scene?.reconstruction?.policy!=='browser-registered-orthographic';
  $('spatial-trace-export').disabled=this.busy||editingBlocked||this.scene?.reconstruction?.policy!=='browser-silhouette-slab';
  $('spatial-export-glb').disabled=this.busy||!this.scene||this.worldMode||Boolean(this.scene.translationAnimation);
  $('spatial-export-glb').title=this.scene?.translationAnimation?'Use Export Blender to preserve your timeline.':'';
  $('spatial-reconstruct-export').disabled=this.busy||!this.scene?.reconstruction||this.scene.reconstruction.policy.startsWith('browser-')||this.worldMode;
  const exchangeBusy=this.busy||this.agentBusy||this.evaluationBusy||['prepared','executing','running','paused'].includes(this.snapshot?.status);
  $('spatial-prop-open').disabled=exchangeBusy||recipient||!!this.worldMode||!this.object?.visualMesh||!this.object.massKg;
  $('spatial-glb').disabled=exchangeBusy;$('spatial-blueprint-build').disabled=exchangeBusy||!$('spatial-blueprint').value;
  $('spatial-export-blender').disabled=this.busy||!this.scene||this.worldMode;const pushHeld=this.pushMode&&(this.pushGoal&&['running','waiting-host'].includes(this.pushGoal.status)||['running','paused'].includes(this.snapshot?.status)),held=this.evaluationBusy||pushHeld||['prepared','executing'].includes(this.snapshot?.status);for(const id of ['spatial-example','spatial-file','spatial-import-blender','spatial-scenes','spatial-objects','spatial-preview','spatial-run','spatial-dx','spatial-dz','spatial-skill'])$(id).disabled=this.busy||this.agentBusy||held||(['spatial-preview','spatial-run'].includes(id)&&!this.object?.massKg);for(const id of ['push-example','push-variant','push-pusher','push-dx','push-dz','push-initial-force','push-max-force','push-skill','push-preview','push-run'])$(id).disabled=this.busy||this.agentBusy||held||(['push-preview','push-run'].includes(id)&&(!this.object?.massKg||!$('push-pusher').value));if($('push-use-profile')){$('push-use-profile').disabled=this.busy||this.agentBusy||held;$('push-initial-force').disabled=this.busy||this.agentBusy||held||($('push-use-profile').checked||$('push-use-contact')?.checked);}if($('push-use-contact')){$('push-use-contact').disabled=this.busy||this.agentBusy||held;if($('push-use-contact').checked){$('push-initial-force').disabled=true;$('push-max-force').disabled=true;}}if(this.worldMode)for(const id of ['spatial-preview','spatial-run','spatial-dx','spatial-dz','spatial-skill'])$(id).disabled=true;$('push-export').disabled=this.busy||!this.pushRunId||!['completed','failed','cancelled'].includes(this.snapshot?.status);$('push-pause').disabled=this.busy||!this.pushMode||this.snapshot?.status!=='running';$('push-resume').disabled=this.busy||this.pushGoal?.status!=='interrupted'||!['paused','interrupted'].includes(this.snapshot?.status);}
 binding(){return {sceneId:this.scene.id,objectId:this.object.id};}
 delta(){return [Number($('spatial-dx').value),0,Number($('spatial-dz').value)];}
 requireObject(){if(!this.scene||!this.object?.massKg)throw Error('Select a dynamic imported object first');}
 async import(text){const s=await this.call('import',{text});await this.open(s.id);this.notice('Imported source and declared scale verified. No independent room calibration.');return s;}
 async exportGlb(){
  const result=await this.call('export-glb',{id:this.scene.id});
  this.notice('Checked GLB saved to '+result.path);$('spatial-evidence').textContent=JSON.stringify(result,null,2);
 }
 async reconstruct(command,args){
  const status=$('spatial-reconstruct-result');
  status.textContent='Reconstructing an editable surface from the silhouettes…';
  try{
   const scene=await this.call(command,args);
   await this.open(scene.id);
  }catch(error){
   status.textContent=error.message;
   throw error;
  }
 }
 showReconstruction(proof){
  this.libraryViews.show(proof);
  if(proof?.viewSource==='sprite-library')proof=null;
  if(proof?.policy==='browser-registered-orthographic'){
   $('browser-views-status').textContent='Side estimates '+Math.round(proof.estimatedDepthM*1000)+' mm depth · '+
    (proof.heldOutIou*100).toFixed(1)+'% held-out silhouette overlap.'+
    (proof.topIou==null?'':' Top view included.');
   proof=null;
  }
  if(proof?.policy==='browser-silhouette-slab'){
   $('spatial-trace-status').textContent=(proof.foregroundIou*100).toFixed(1)+'% silhouette overlap · '+proof.grid+'-cell grid · declared thickness '+proof.declaredThicknessM+' m.';
   proof=null;
  }else $('spatial-trace-status').textContent='Choose a fully visible image, SVG or canvas with a plain contrasting background.';
  const status=$('spatial-reconstruct-result');
  if(!proof){
   status.textContent='Use three registered PNG silhouettes. Add first-hit depth to recover recessed surfaces.';
   return;
  }
  const views=proof.depth?.views;
  const carved=proof.depth?' · '+proof.depth.removedVoxels+' cells carved by depth'+(views?' from '+views.length+' views':''):'';
  status.textContent=proof.triangles+' editable triangles · '+proof.voxels+' reconstructed cells'+carved+' · separate enclosing collider. Export GLB to edit in Blender.';
 }
 async openLibraryViews(front){
  await this.open();
  await this.libraryViews.open(front);
 }
 async openBrowserPlacement(saved){
  await this.open(saved.source.sceneId);
  if(this.object?.id!==saved.objectId)await this.selectObject(saved.objectId);
  await this.placement.openForBrowser(saved);
 }
 async open(id){if(!id&&this.worldMode&&this.worldReopen){this.activate('spatial');await this.worldReopen();this.resize();return;}this.worldMode=false;this.poseMode=false;this.removePoseTargets();this.evaluationMode=false;this.activate('spatial');this.ensureRenderer();const list=await this.call('list');$('spatial-scenes').replaceChildren(...list.scenes.map(s=>{const o=document.createElement('option');o.value=s.id;o.textContent=Number.isInteger(s.recipientRevision)?s.title+' · Local revision '+s.recipientRevision:s.title;return o;}));const selected=id||this.scene?.id||list.scenes.at(-1)?.id;if(selected)await this.select(selected,list);if($('spatial-exchange').open)await this.refreshBlueprints();if($('image-assembly').open)await this.imageAssembly.refresh();this.resize();this.controls();}
 async openWorld(scene){this.meshEdit.bind(null,null);this.timeline.bind(null,null);this.carry.clear();this.showReconstruction();this.activate('spatial');this.ensureRenderer();this.scene=scene;this.worldMode=true;this.poseMode=true;this.pushMode=false;this.evaluationMode=false;this.plan=null;this.snapshot=null;this.removePoseTargets();this.forecast(null);this.trace=[];this.drawTrace();this.paint(scene.objects);$('spatial-objects').replaceChildren(...scene.objects.map(o=>{const item=document.createElement('option');item.value=o.id;item.textContent=o.label+' · '+(o.massKg>0?'dynamic':'static');return item;}));this.object=scene.objects.find(o=>o.massKg>0);$('spatial-objects').value=this.object.id;this.paintSelection();this.resize();this.controls();}
 async select(id,list){this.meshEdit.bind(null,null);this.timeline.bind(null,null);this.carry.clear();this.worldMode=false;this.pushMode=false;this.pushGoal=null;this.pushRunId=null;this.removeLine(this.pusherTraceLine);this.pusherTraceLine=null;this.pusherTrace=[];this.scene=await this.call('read',{id});$('spatial-scenes').value=id;this.plan=null;this.forecast(null);this.trace=[];this.drawTrace();this.snapshot=null;this.accept({sceneId:id,status:'authored',current:{tick:0,objects:this.scene.objects,gripping:false}});$('spatial-objects').replaceChildren(...this.scene.objects.map(o=>{const v=document.createElement('option');v.value=o.id;v.textContent=`${o.label} · ${o.massKg>0?'dynamic':'static'}`;return v;}));const recipientProp=this.scene.objects.find(o=>o.sourceId===this.scene.recipientBranch?.propSourceId);await this.selectObject((recipientProp||this.scene.objects.find(o=>o.massKg>0)).id);const s=this.scene;this.showReconstruction(s.reconstruction);$('spatial-source').textContent=s.imageAssembly?'Image scene · '+s.imageAssembly.measured.length+' checked props · declared layout scale':s.reconstruction?.viewSource==='sprite-library'?'Library model · linked original views · held-out view checked':s.reconstruction?.policy==='browser-registered-orthographic'?'Browser model · estimated depth · held-out view checked':s.reconstruction?.policy==='browser-silhouette-slab'?'Browser silhouette · one captured view · declared scale and thickness':s.reconstruction?`Reconstructed visual mesh · ${s.reconstruction.depth?'silhouettes and depth':'silhouettes'} verified · ${s.reconstruction.cellM*1000} mm grid · declared scale`:`${s.source.kind.toUpperCase()} · ${s.sourceUnits} → m · ${s.scaleCheck.convertedLengthM} m reference (${s.scaleCheck.basis}) · ${s.source.physicalProperties} properties · not independently calibrated`;$('spatial-evidence').textContent=JSON.stringify({sceneId:s.id,sourceSha256:s.sourceSha256,source:s.source,scaleCheck:s.scaleCheck,propCopy:s.propCopy,reconstruction:s.reconstruction,meshEdit:s.meshEdit,imageAssembly:s.imageAssembly},null,2);if(s.propCopy)$('spatial-plan').textContent=this.propSummary(s.propCopy.verify);this.frame();const records=list||await this.call('list');const last=records.runs.filter(r=>r.sceneId===id&&r.status==='completed').at(-1);if(last){const r=await this.call('run',{id:last.id});this.accept({id:r.id,sceneId:id,status:r.status,current:r.actual.final,verification:r.verification});this.forecast(r.plan);this.trace=r.actual.samples.map(s=>s.position);this.drawTrace();}const pushState=await this.pushCall('scene-state',{sceneId:id});if(pushState.current){this.pushMode=true;this.acceptPush(pushState);}await this.pushRoles();this.controls();}
 async selectObject(id){if(this.object?.id!==id)this.carry.clear();this.object=this.scene.objects.find(o=>o.id===id);this.timeline.bind(this.scene,this.object?.massKg?this.object:null);this.meshEdit.bind(this.scene,this.object);$('spatial-objects').value=id;await this.call('select',this.binding());this.paintSelection();await this.pushRoles();this.controls();}
 async poll(){if(this.worldMode||this.poseMode)return;if(this.evaluationMode&&this.evaluationBusy)return;this.polling=true;try{const s=await (this.pushMode?this.pushCall:this.call)('snapshot');if(s.sceneId===this.scene?.id){if(this.pushMode)this.acceptPush(s);else this.accept(s);}}finally{this.polling=false;}}
 accept(s){if(!s.current)return;if(['prepared','executing'].includes(s.status)){this.meshEdit.clear();this.timeline.stop();}const prior=this.snapshot;this.snapshot=s;if(s.id&&s.id!==prior?.id){this.trace=[];this.drawTrace();this.call('run',{id:s.id}).then(r=>{if(this.snapshot?.id===r.id){this.forecast(r.plan);$('spatial-plan').textContent=`Native forecast ${r.plan.route} route · ${r.plan.trials.length} candidate worlds · orange prediction / blue actual. Source ${r.definition.source.kind}, declared parameters.`;}}).catch(e=>this.notice(e.message));}this.paint(s.current.objects);if(s.current.tick!==prior?.current?.tick&&this.object){const o=s.current.objects.find(o=>o.id===this.object.id);if(o&&s.status==='executing'){this.trace.push(o.position);if(this.trace.length>1000)this.trace.shift();this.drawTrace();}}
  $('spatial-live').textContent=`${s.status} · tick ${s.current.tick} · ${(s.current.tick/240).toFixed(2)} s · ${s.current.gripping?'native constraint holding object':'released / no constraint'}${s.verification?` · ${s.verification.passed?'verified':'failed gates'} · error ${s.verification.horizontalErrorM.toFixed(6)} m`:''}`;$('spatial-live').dataset.status=s.status;this.controls();
 }
 pushOptions(){return {...($('push-use-contact')?.checked?{useContactController:true}:{}),...($('push-use-profile')?.checked?{useActiveProfile:true}:{}),displacementM:[Number($('push-dx').value),0,Number($('push-dz').value)],initialForceN:Number($('push-initial-force').value),maxForceN:Number($('push-max-force').value)};}
 pushBinding(){this.requireObject();const pusherId=$('push-pusher').value;if(!pusherId)throw Error('Choose a separate dynamic pusher');return {sceneId:this.scene.id,objectId:this.object.id,pusherId};}
 async pushRoles(preferred){if(!this.scene)return;const select=$('push-pusher'),current=preferred||select.value;const candidates=this.scene.objects.filter(o=>o.massKg>0&&o.id!==this.object?.id);select.replaceChildren(...candidates.map(o=>{const e=document.createElement('option');e.value=o.id;e.textContent=o.label;return e;}));if(candidates.some(o=>o.id===current))select.value=current;if(this.object?.massKg&&select.value)await this.pushCall('select',this.pushBinding());}
 showPushPlan(plan,preview=true){this.pushPlan=plan;this.removeLine(this.forecastLine);this.forecastLine=plan.passed?this.line([this.scene.objects.find(o=>o.id===plan.intent.objectId).position,...plan.actions.filter(a=>a.kind==='push').map(a=>a.target)],0xf5ae52):null;$('push-evidence').textContent=JSON.stringify(plan,null,2);if(preview)$('push-live').textContent=plan.passed?'Standing-pose plan verified · '+plan.pushes.length+' pushes · '+plan.expanded+' configurations checked · no native actuation':'Plan rejected · '+plan.reason;}
 onPushGoal(g){if(g?.procedure!=='spatial-push-v1')return;const fresh=g.id!==this.pushSeenGoalId;this.pushSeenGoalId=g.id;if(g.pushBinding?.sceneId===this.scene?.id){this.pushGoal=g;this.pushMode=true;}else if(fresh&&g.pushBinding&&['running','interrupted'].includes(g.status)&&!this.pushOpening){this.pushOpening=true;this.open(g.pushBinding.sceneId).then(async()=>{this.pushGoal=g;this.pushMode=true;await this.selectObject(g.objectId);$('push-pusher').value=g.pusherId;await this.pushRoles(g.pusherId);$('push-panel').open=true;}).catch(e=>this.notice(e.message)).finally(()=>{this.pushOpening=false;});}this.controls();}
 acceptPush(s){if(!s.current)return;if(['running','paused'].includes(s.status)){this.meshEdit.clear();this.timeline.stop();}const prior=this.snapshot;this.snapshot=s;this.pushRunId=s.id;this.paint(s.current.objects);if(s.id!==prior?.id){this.trace=[];this.pusherTrace=[];this.pushCall('run',{id:s.id}).then(r=>{if(this.pushRunId===r.id)this.showPushPlan(r.plan,false);}).catch(e=>this.notice(e.message));}const sample=s.sample,c=sample?.controller;if(sample&&sample.tick!==prior?.sample?.tick){const crate=s.current.objects.find(o=>o.id===this.object?.id),pusher=s.current.objects.find(o=>o.id===$('push-pusher').value);if(crate)this.trace.push(crate.position);if(pusher)this.pusherTrace.push(pusher.position);this.drawTrace();this.removeLine(this.pusherTraceLine);this.pusherTraceLine=this.pusherTrace.length>1?this.line(this.pusherTrace,0xd298e8):null;}$('spatial-live').textContent='Native push '+s.status+' · '+(sample?.timeS||0).toFixed(2)+' s · action '+((c?.action||0)+1)+' · '+(c?.recoveryMode||c?.phase||'preparing')+' · measured contacts '+(sample?.contacts||0);$('spatial-live').dataset.status=s.status;$('push-live').textContent=(s.reason||'Original source and force budget retained')+' · recoveries '+(c?.recoveries||0)+' · explicit native resumes '+(s.resumes||0)+(s.checkpoint?' · committed tick '+s.checkpoint.tick:'');$('push-evidence').textContent=JSON.stringify({controller:c,nativeContact:sample?.nativeContact,force:sample?.force,checkpoint:s.checkpoint,verification:s.verification},null,2);this.controls();}
 showCarryFrame(id,sample,trace){
  if(this.carryTrace!==trace){
   this.removeLine(this.carryLine);this.carryLine=null;
   if(this.carryGhost){
    this.world.remove(this.carryGhost);
    this.carryGhost.material.dispose();
    this.carryGhost=null;
   }
   this.carryTrace=trace;
  }
  if(!sample)return;
  const source=this.meshes.get(id);
  if(!source)return;
  if(!this.carryGhost){
   this.carryGhost=new THREE.Mesh(source.geometry,new THREE.MeshBasicMaterial({
    color:0x64ddd2,transparent:true,opacity:.72,depthWrite:false
   }));
   this.world.add(this.carryGhost);
   this.carryLine=this.line(trace.map(frame=>frame.positionM),0x64ddd2);
  }
  this.carryGhost.position.fromArray(sample.positionM);
  this.carryGhost.quaternion.fromArray(sample.rotation);
 }
 removePoseTargets(){if(this.poseTargetGroup){this.poseTargetGroup.traverse(o=>{o.geometry?.dispose();o.material?.dispose();});this.world?.remove(this.poseTargetGroup);this.poseTargetGroup=null;}}
 showPoseTargets(targets,objects=this.scene?.objects||[]){this.ensureRenderer();this.removePoseTargets();this.poseTargetGroup=new THREE.Group();for(const t of targets){const o=objects.find(o=>o.id===t.objectId);if(!o)continue;const mesh=new THREE.Mesh(new THREE.BoxGeometry(...o.size),new THREE.MeshBasicMaterial({color:0xf0c178,wireframe:true,transparent:true,opacity:.65}));mesh.position.fromArray(t.positionM);mesh.quaternion.fromArray(t.rotation);this.poseTargetGroup.add(mesh);const axes=new THREE.AxesHelper(.6);axes.position.copy(mesh.position);axes.quaternion.copy(mesh.quaternion);this.poseTargetGroup.add(axes);}this.world.add(this.poseTargetGroup);}
 showPose(snapshot){this.meshEdit.clear();this.timeline.stop();if(!this.poseMode)this.carry.clear();this.ensureRenderer();this.poseMode=true;this.paint(snapshot.current.objects);for(const o of snapshot.current.objects){const mesh=this.meshes.get(o.id);if(o.massKg>0&&mesh)mesh.material.color.setHex(o.sourceId==='A'?0x8cdbbc:o.sourceId==='B'?0x8fb7e9:o.sourceId==='C'?0xd1a0e5:0xa1abad);}this.showPoseTargets(snapshot.targets||[],snapshot.current.objects);$('spatial-live').textContent='Native pose '+snapshot.status+' · '+snapshot.sample.timeS.toFixed(2)+'simulated s · held '+(snapshot.current.grippingId||'none')+' · '+snapshot.sample.bodies.length+' actual dynamic bodies';this.resize();}
 showEvaluation(live){this.meshEdit.clear();this.timeline.stop();if(!this.evaluationMode)this.carry.clear();this.ensureRenderer();this.evaluationMode=true;const snap=live.snapshot;this.paint(snap.current.objects);$('spatial-live').textContent='Native experience · '+live.currentTrial.caseId+' / '+live.currentTrial.arm+' · '+(snap.sample?.timeS||0).toFixed(2)+' s · actual contacts '+(snap.sample?.contacts||0);$('spatial-plan').textContent='Actual private evaluation world; authored scene selection stays separate. Source '+live.source.sourceSha256;this.resize();}
 ensureRenderer(){if(this.renderer||this.rendererUnavailable)return;this.world=new THREE.Scene();this.world.background=new THREE.Color('#11232c');this.camera=new THREE.PerspectiveCamera(45,1,.05,100);this.planCamera=new THREE.OrthographicCamera(-10,10,10,-10,.05,100);this.planCamera.up.set(0,0,-1);this.planCamera.position.set(0,45,0);this.planCamera.lookAt(0,0,0);try{
   this.renderer=new THREE.WebGLRenderer({antialias:true,preserveDrawingBuffer:true});
  }catch(error){
   this.rendererUnavailable=true;
   const status=document.createElement('p');
   status.className='spatial-note';
   status.setAttribute('role','status');
   status.textContent='The 3D preview is unavailable on this display. Spatial scenes, measurements and exports are available.';
   $('spatial-stage').append(status);
   console.warn(error.message);
   return;
  }this.renderer.setPixelRatio(Math.min(devicePixelRatio,2));this.renderer.domElement.setAttribute('aria-label','Rendered native spatial scene');$('spatial-stage').append(this.renderer.domElement);this.world.add(new THREE.HemisphereLight(0xe0fff4,0x263e5d,2));const light=new THREE.DirectionalLight(0xffffff,3);light.position.set(-3,8,5);this.world.add(light);this.world.add(new THREE.GridHelper(16,32,0x5a817f,0x29414b));this.world.add(new THREE.AxesHelper(1.5));this.frame();let drag=null,moved=false;const stage=$('spatial-stage');stage.onpointerdown=e=>{drag=[e.clientX,e.clientY];moved=false;stage.setPointerCapture(e.pointerId);};stage.onpointermove=e=>{if(!drag)return;const dx=e.clientX-drag[0],dy=e.clientY-drag[1];if(Math.abs(dx)+Math.abs(dy)>2)moved=true;if(this.topDown)return;this.yaw-=dx*.008;this.pitch=Math.max(.12,Math.min(1.35,this.pitch+dy*.008));drag=[e.clientX,e.clientY];};stage.onpointerup=e=>{drag=null;if(moved)return;const r=stage.getBoundingClientRect(),ray=new THREE.Raycaster();ray.setFromCamera(new THREE.Vector2((e.clientX-r.left)/r.width*2-1,-(e.clientY-r.top)/r.height*2+1),(this.topDown?this.planCamera:this.camera));const hit=ray.intersectObjects([...this.meshes.values()])[0];if(hit&&!this.meshEdit.pick(hit))this.action(()=>this.selectObject(hit.object.userData.id));};stage.addEventListener('wheel',e=>{e.preventDefault();if(this.topDown){this.planZoom=Math.max(.25,Math.min(5,this.planZoom*Math.exp(e.deltaY*.001)));this.resize();}else this.radius=Math.max(this.focusedProp ? .6 : 3,Math.min(30,this.radius+e.deltaY*.01));},{passive:false});const draw=()=>{requestAnimationFrame(draw);if(!this.visible||this.linkedMode)return;const focus=this.focus||[0,.5,0];this.camera.position.set(focus[0]+this.radius*Math.cos(this.pitch)*Math.sin(this.yaw),focus[1]+this.radius*Math.sin(this.pitch),focus[2]+this.radius*Math.cos(this.pitch)*Math.cos(this.yaw));this.camera.lookAt(...focus);this.renderer.render(this.world,this.topDown?this.planCamera:this.camera);};draw();}
 framedVisual(){
  if(this.scene?.imageAssembly)return null;
  return this.scene?.objects.find(object=>object.id===this.object?.id&&object.visualMesh);
 }
 frame(){
  this.yaw=.55;this.pitch=.55;this.radius=9;this.planZoom=1;this.focus=[0,.5,0];
  const subject=this.framedVisual();
  this.focusedProp=Boolean(subject);
  if(subject){this.focus=this.timeline.point(subject);this.radius=Math.max(1.15,Math.max(...subject.size)*2.5);}
  if(this.scene?.imageAssembly){
   const size=this.scene.objects[0].size;
   this.radius=Math.max(2.6,Math.hypot(size[0],size[2])*1.35);
  }
  this.resize();
 }
 resize(){if(!this.renderer||!this.visible)return;const r=$('spatial-stage').getBoundingClientRect();if(r.width<1||r.height<1)return;this.renderer.setSize(r.width,r.height,false);this.camera.aspect=r.width/r.height;this.camera.updateProjectionMatrix();
  const subject=this.framedVisual();
  let half=3;
  if(subject){
   const [x,y,z]=this.timeline.point(subject);
   this.planCamera.position.set(x,y+45,z);
   this.planCamera.lookAt(x,y,z);
   half=Math.max(.25,Math.hypot(subject.size[0],subject.size[2])*.75);
  }else{
   this.planCamera.position.set(0,45,0);
   this.planCamera.lookAt(0,0,0);
   for(const o of this.scene?.objects||[])half=Math.max(half,Math.abs(o.position[0])+Math.hypot(...o.size)/2,Math.abs(o.position[2])+Math.hypot(...o.size)/2);
  }
  const vertical=half*1.08*(this.planZoom||1)*Math.max(1,1/this.camera.aspect),horizontal=vertical*this.camera.aspect;
  Object.assign(this.planCamera,{left:-horizontal,right:horizontal,top:vertical,bottom:-vertical});
  this.planCamera.updateProjectionMatrix();
 }
 paint(objects){
  if(!this.renderer)return;
  objects=this.timeline.pose(objects);
  for(const o of objects){
   let mesh=this.meshes.get(o.id);
   if(!mesh){
    const asset=this.scene?.visualMeshes?.[o.visualMesh];
    const geometry=asset?visualGeometry(asset):new THREE.BoxGeometry(...o.size);
    mesh=new THREE.Mesh(geometry,new THREE.MeshStandardMaterial({
     color:asset?0xf0b176:o.sourceId==='pusher'?0x75a9e8:o.massKg>0?0x80d6b3:o.sourceId==='floor'?0x263f4c:0x8a98b1,
     roughness:.7,flatShading:Boolean(asset&&this.scene?.reconstruction),transparent:o.sourceId==='floor',opacity:o.sourceId==='floor'?.65:1
    }));
    mesh.userData.id=o.id;
    if(asset){
     const wire=new THREE.Mesh(new THREE.BoxGeometry(...o.size),
      new THREE.MeshBasicMaterial({color:0x9dffda,wireframe:true,depthTest:false,transparent:true,opacity:.72}));
     wire.userData.id=o.id;
     mesh.userData.collider=wire;
     mesh.add(wire);
    }
    this.meshes.set(o.id,mesh);
    this.world.add(mesh);
   }
   mesh.position.fromArray(o.position);
   mesh.quaternion.fromArray(o.rotation);
  }
  for(const [id,mesh] of this.meshes)if(!objects.some(o=>o.id===id)){
   this.world.remove(mesh);
   mesh.traverse(child=>{child.geometry?.dispose();child.material?.dispose();});
   this.meshes.delete(id);
  }
  this.paintColliders();
  this.paintSelection();
 }
 paintColliders(){
  for(const mesh of this.meshes.values())if(mesh.userData.collider)mesh.userData.collider.visible=$('spatial-colliders').checked;
 }
 paintSelection(){for(const [id,m] of this.meshes)m.material.emissive.setHex(id===this.object?.id?0x154a31:0);}
 line(points,color){const geometry=new THREE.BufferGeometry().setFromPoints(points.map(p=>new THREE.Vector3(...p)));const line=new THREE.Line(geometry,new THREE.LineBasicMaterial({color}));this.world.add(line);return line;}
 removeLine(line){if(line){this.world.remove(line);line.geometry.dispose();line.material.dispose();}}
 forecast(plan){if(!this.world)return;this.removeLine(this.forecastLine);this.forecastLine=plan?this.line(plan.forecast.samples.map(s=>s.position),0xf5ae52):null;}
 drawTrace(){if(!this.world)return;this.removeLine(this.traceLine);this.traceLine=this.trace.length>1?this.line(this.trace,0x71c9ff):null;}
}
