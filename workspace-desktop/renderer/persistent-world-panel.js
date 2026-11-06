import {Procedure077Panel} from './procedures077.js';
import {Procedure076Panel} from './procedures076.js';
import {Memory075Panel} from './memory075.js';
import {Memory074Panel} from './memory074.js';
import {Rehearsal073Panel} from './rehearsal073.js';
const $=id=>document.getElementById(id);
const FIRST_SKILL_CASE='dev035-three-body-chain';
const FIRST_SKILL_NAME='first-world-skill';

/** A view/controller for Java-owned revisions. Mesh updates never become physical state. */
export class PersistentWorldPanel {
 constructor({call,spatial,notice,goal,openReport,requireFirstSkillReady,connectFirstSkill}){
  Object.assign(this,{call,spatial,notice,goal,openReport,requireFirstSkillReady,connectFirstSkill});
  this.firstSkillGoalId=localStorage.getItem('first-world-goal');
  const root=document.createElement('details');root.id='persistent-world-panel';root.className='spatial-detail persistent-world';root.innerHTML=`<summary>One evolving world · continue across goals</summary>
   <p>Keep every body’s position, heading and velocities between requests. Java commits a new revision only after native motion and the original report pass. Physics pauses between goals.</p>
   <section id="first-world-guide" class="first-world-guide" hidden aria-labelledby="first-world-title">
    <h3 id="first-world-title">Your first world skill</h3>
    <p>Arrange objects in a separate, labelled simulation. Java checks where they finish and tests the report before saving a reusable skill.</p>
    <p id="first-world-status" role="status">Ready to create a practice world.</p>
    <div class="persistent-toolbar">
     <button id="first-world-start" class="primary">Run and verify practice skill</button>
     <button id="first-world-report" hidden>Open verified report</button>
     <button id="first-world-connect" hidden>Connect this skill</button>
    </div>
   </section>
   <div class="persistent-toolbar"><label>Saved world<select id="persistent-worlds"></select></label><button id="persistent-create">Create world from selected scene</button></div>
   <details><summary>Labelled test sequences</summary><label>Sequence<select id="persistent-case"></select></label><button id="persistent-load">Create labelled test world</button><label>Next request<select id="persistent-step"></select></label><button id="persistent-use-step">Use these targets</button></details>
   <p id="persistent-status" role="status">Create or select a persistent world.</p><div id="persistent-body-summary"></div>
   <div class="pose-target-table"><table><thead><tr><th>Role</th><th>Target X · m</th><th>Target Z · m</th><th>Yaw · degrees</th></tr></thead><tbody id="persistent-target-rows"></tbody></table></div>
   <div class="persistent-toolbar"><button id="persistent-confirm">Observe &amp; confirm targets</button><label>Save new skill as<input id="persistent-skill" placeholder="Optional skill name"></label><button id="persistent-run" class="primary">Continue world &amp; verify</button><label><input id="cf045-review" type="checkbox"> Review before moving</label><button id="cf045-run" class="primary">Compare alternatives &amp; run</button></div>
   <details id="cf045-panel"><summary>Alternative plans and forecast checks</summary><p id="cf045-panel-status">Confirm a world and target poses, then compare native alternatives before execution.</p><div class="persistent-target-table"><table id="cf045-panel-table"><thead><tr><th>Alternative</th><th>Outcome</th><th>Simulated s</th><th>Effort N.s</th><th>Protected drift m</th></tr></thead><tbody></tbody></table></div><p id="cf045-panel-result"></p><button id="cf045-export" disabled>Export alternatives and original traces</button><small>Unchanged native controller; original/reverse object order and half-force use under the confirmed ceiling. Forecasts do not move the saved world. Source changes refuse execution.</small></details>
   <p id="persistent-confirm-state">Targets need a current world observation.</p>
   <details class="persistent-editor"><summary>Target JSON, force limits &amp; native evidence</summary><label>Force limit · N<input id="persistent-force" type="number" min="2" max="60" value="60"></label><label>Torque limit · N·m<input id="persistent-torque" type="number" min="0" max="4" value="4"></label><label>Targets JSON<textarea id="persistent-targets" rows="8" spellcheck="false"></textarea></label><button id="persistent-apply">Apply target JSON</button><pre id="persistent-evidence"></pre></details>
   <div class="persistent-toolbar"><label>Verified history<select id="persistent-revisions"></select></label><button id="persistent-restore">Restore selected verified revision</button><button id="persistent-export">Export world &amp; all attempts</button></div>
   <p class="subtle">Restore is an explicit state revision, not physical movement. Failed or interrupted attempts keep ownership until resumed or explicitly abandoned and restored. Original history stays available.</p>`;
  $('pose-panel').before(root);this.root=root;
  this.procedures077=new Procedure077Panel({call:this.call,root,notice:this.notice});
  this.procedures076=new Procedure076Panel({call:this.call,root,notice:this.notice});
  this.memory075=new Memory075Panel({call:this.call,root,notice:this.notice});
  this.memory074=new Memory074Panel({call:this.call,root,notice:this.notice});
  this.rehearsal073=new Rehearsal073Panel({call:this.call,root,notice:this.notice,selectWorld:id=>this.select(id)});
  $('first-world-start').onclick=async()=>{
   $('first-world-start').disabled=true;
   try{await this.action(()=>this.startFirstSkill());}
   finally{$('first-world-start').disabled=false;}
  };
  $('first-world-report').onclick=()=>this.action(()=>this.openReport(this.firstSkillGoal.report.id));
  $('first-world-connect').onclick=()=>this.action(async()=>{
   const suite=this.suite||await this.call('suite');
   const practice=suite.cases.find(item=>item.id===FIRST_SKILL_CASE);
   await this.connectFirstSkill(this.firstSkillGoal,practice.steps.slice(1,3));
  });
  $('persistent-worlds').onchange=()=>this.action(()=>this.select($('persistent-worlds').value));
  $('persistent-create').onclick=()=>this.action(async()=>{if(!this.spatial.scene)throw Error('Select an imported source first');const w=await this.call('create',{sceneId:this.spatial.scene.id,title:(this.spatial.scene.title+' · evolving world').slice(0,120)});this.targets=null;await this.select(w.id);});
  $('persistent-load').onclick=()=>this.action(()=>this.loadLabelled($('persistent-case').value));
  $('persistent-case').onchange=()=>{this.case=this.suite.cases.find(c=>c.id===$('persistent-case').value);this.steps();};
  $('persistent-use-step').onclick=()=>this.action(async()=>{const c=this.case||this.suite.cases.find(c=>c.id===$('persistent-case').value),step=c.steps[Number($('persistent-step').value)];this.targets=structuredClone(step.targets);$('persistent-force').value=step.forceLimitN;$('persistent-torque').value=step.torqueLimitNm;this.renderTargets();this.invalidate();await this.confirm();});
  $('persistent-confirm').onclick=()=>this.action(()=>this.confirm());$('persistent-apply').onclick=()=>this.action(async()=>{this.targets=JSON.parse($('persistent-targets').value);this.renderTargets();this.invalidate();await this.confirm();});
  $('persistent-force').oninput=$('persistent-torque').oninput=()=>this.invalidate();
  $('persistent-run').onclick=()=>this.action(async()=>{if(!this.bound)throw Error('Observe and confirm these targets first');const g=await this.call('start',{saveSkillName:$('persistent-skill').value.trim()});this.accept(g);this.goal(g);});
  $('cf045-run').onclick=()=>this.action(async()=>{if(!this.bound)throw Error('Observe the current world and confirm these targets first');const task=await this.call('counter-start',{saveSkillName:$('persistent-skill').value.trim(),reviewCounterfactual045:$('cf045-review').checked});this.counterTaskId=task.id;$('cf045-panel').open=true;this.notice('Java queued the source-bound counterfactual mission.');});
  $('cf045-export').onclick=()=>this.action(async()=>{const id=this.g?.counterfactual045?.studyId;if(!id)throw Error('No actual counterfactual study selected');const saved=await this.call('counter-export',{id});this.notice('Original alternatives and traces saved: '+saved.path);});
  $('persistent-restore').onclick=()=>this.action(async()=>{if(!this.observed)throw Error('Select a world');const rev=await this.call('restore',{worldId:this.id,expectedRevision:this.observed.expectedRevision,expectedHash:this.observed.expectedHash,targetRevision:Number($('persistent-revisions').value)});this.invalidate();await this.refresh();this.notice('Explicit state restoration recorded as revision '+rev.revision+'; no physical movement was claimed.');});
  $('persistent-export').onclick=()=>this.action(async()=>{const result=await this.call('export',{worldId:this.id});this.notice('Complete persistent world and original native attempts saved: '+result.path);});
  root.ontoggle=()=>{if(root.open)this.action(()=>this.refresh());};
  spatial.worldReopen=()=>this.refresh();spatial.worldObserve=async args=>this.call('visibility',{worldId:this.id,...args});
  setInterval(()=>{if(!this.busy&&(this.spatial.visible||this.g&&['running','waiting-host'].includes(this.g.status)))this.refresh().catch(e=>this.notice(e.message));},1000);
  this.refresh().catch(e=>this.notice(e.message));this.controls();
 }
 showFirstSkill(){
  this.spatial.activate('spatial');
  this.root.open=true;
  const guide=$('first-world-guide');
  guide.hidden=false;
  guide.scrollIntoView({block:'nearest'});
  const next=this.firstSkillGoal?.savedSkill?$('first-world-connect'):$('first-world-start');
  if(!next.hidden&&!next.disabled)next.focus({preventScroll:true});
 }
 async loadLabelled(caseId,title=caseId){
  const suite=this.suite||await this.call('suite');
  const sequence=suite.cases.find(item=>item.id===caseId);
  if(!sequence)throw Error('Labelled world sequence is unavailable');
  const scene=await this.spatial.import(JSON.stringify(sequence.source));
  const world=await this.call('create',{sceneId:scene.id,title});
  const step=sequence.steps[0];
  this.targets=structuredClone(step.targets);
  $('persistent-force').value=step.forceLimitN;
  $('persistent-torque').value=step.torqueLimitNm;
  this.case=sequence;
  this.steps();
  await this.select(world.id);
  await this.confirm();
 }
 async startFirstSkill(){
  this.requireFirstSkillReady();
  if(this.firstSkillGoalId&&this.firstSkillGoal?.status!=='cancelled')return;
  this.preparingFirstSkill=true;
  $('first-world-status').textContent='Preparing the labelled practice world…';
  try{
   await this.loadLabelled(FIRST_SKILL_CASE,'Practice world · labelled simulation');
   this.requireFirstSkillReady();
   const goal=await this.call('start',{
    saveSkillName:FIRST_SKILL_NAME,expectedWorldId:this.bound.worldId,expectedHash:this.bound.expectedHash
   });
   this.firstSkillGoalId=goal.id;
   localStorage.setItem('first-world-goal',goal.id);
   this.preparingFirstSkill=false;
   this.accept(goal);
   this.goal(goal);
  }catch(error){
   $('first-world-status').textContent=error.message;
   throw error;
  }finally{
   this.preparingFirstSkill=false;
  }
 }
 updateFirstSkill(goal){
  if(this.preparingFirstSkill||!goal||goal.id!==this.firstSkillGoalId)return;
  this.firstSkillGoal=goal;
  const verified=goal.status==='completed'&&goal.worldCommit&&goal.savedSkill&&goal.report?.id;
  $('first-world-status').textContent=verified
   ? `Verified revision ${goal.worldCommit.revision} · saved ${goal.savedSkill.name} v${goal.savedSkill.version}.`
   : goal.status==='cancelled'?'Practice stopped. You can start again in a separate practice world.'
   : goal.reason||'Java is running and checking the practice skill.';
  $('first-world-start').hidden=goal.status!=='cancelled';
  $('first-world-start').textContent='Run and verify practice skill';
  $('first-world-report').hidden=!verified;
  $('first-world-connect').hidden=!verified;
 }
 steps(){const c=this.case||this.suite?.cases.find(c=>c.id===$('persistent-case').value);$('persistent-step').replaceChildren(...(c?.steps||[]).map((s,i)=>{const o=document.createElement('option');o.value=i;o.textContent='Goal '+(i+1)+' · '+s.expected;return o;}));}
 async action(fn){if(this.busy)return;this.busy=true;this.controls();try{await fn();}catch(e){this.notice(e.message);}finally{this.busy=false;this.controls();}}
 invalidate(){this.bound=null;this.plan=null;$('persistent-confirm-state').textContent='Targets need a fresh current-revision confirmation.';this.controls();}
 async select(id){if(!id)return;this.id=id;localStorage.setItem('persistent-world',id);this.invalidate();const observed=await this.call('observe',{worldId:id});const scene=observed.frame.originalScene;if(this.spatial.scene?.id!==scene.id)await this.spatial.openWorld(scene);this.spatial.worldMode=true;this.spatial.poseMode=true;this.spatial.pushMode=false;if(!this.targets)this.targets=observed.frame.bodies.slice(0,2).map(b=>{const o=scene.objects.find(o=>o.id===b.id);return {sourceId:o.sourceId,positionM:[b.position[0]+.5,o.size[1]/2,b.position[2]],yawDeg:0};});this.renderTargets();await this.refresh();this.spatial.frame();}
 renderTargets(){const rows=$('persistent-target-rows');rows.replaceChildren();for(const target of this.targets||[]){const tr=document.createElement('tr'),name=document.createElement('td');name.textContent=target.sourceId;tr.append(name);for(const [key,index]of [['x',0],['z',2],['yaw',null]]){const td=document.createElement('td'),input=document.createElement('input');input.type='number';input.step=key==='yaw'?'1':'.05';input.value=key==='yaw'?target.yawDeg:target.positionM[index];input.setAttribute('aria-label','World '+target.sourceId+' target '+key);input.oninput=()=>{if(key==='yaw')target.yawDeg=Number(input.value);else target.positionM[index]=Number(input.value);$('persistent-targets').value=JSON.stringify(this.targets,null,2);this.invalidate();};td.append(input);tr.append(td);}rows.append(tr);}$('persistent-targets').value=JSON.stringify(this.targets||[],null,2);}
 async confirm(){if(!this.id||!this.targets)throw Error('Select a world and targets');const current=await this.call('observe',{worldId:this.id});const args={worldId:this.id,expectedRevision:current.expectedRevision,expectedHash:current.expectedHash,targets:structuredClone(this.targets),forceLimitN:Number($('persistent-force').value),torqueLimitNm:Number($('persistent-torque').value)};const plan=await this.call('preview',args);if(!plan.passed)throw Error(plan.reason);this.bound=await this.call('select',args);this.plan=plan;this.observed=current;if(this.spatial.scene?.id!==current.frame.originalScene.id)await this.spatial.open(current.frame.originalScene.id);this.spatial.worldMode=true;this.spatial.poseMode=true;this.spatial.pushMode=false;this.spatial.activate('spatial');this.draw(current);$('persistent-confirm-state').textContent='Confirmed revision '+args.expectedRevision+' · '+args.expectedHash.slice(0,12)+' · '+this.targets.length+' roles · '+args.forceLimitN+'N / '+args.torqueLimitNm+'N·m';this.controls();}
 draw(observed){if(!this.spatial.worldMode)return;const frame=observed.frame;let snap=observed.nativeObservation;if(!snap?.sample){const objects=structuredClone(frame.originalScene.objects);for(const body of frame.bodies)Object.assign(objects.find(o=>o.id===body.id),body);snap={status:'verified',sample:{timeS:frame.elapsedTicks/240,bodies:frame.bodies},current:{objects,grippingId:''},targets:this.plan?.orderedTargets||[]};}this.spatial.showPose(snap);this.spatial.worldMode=true;$('spatial-live').textContent='Persistent world · revision '+observed.expectedRevision+' · '+(observed.pending?'uncommitted '+(snap.status||'owned')+' attempt':'verified · paused between goals')+' · '+(observed.nativeObservation?((frame.elapsedTicks+(snap.sample?.tick||0))/240).toFixed(2):(frame.elapsedTicks/240).toFixed(2))+' simulated s';$('spatial-plan').textContent='Persistent targets continue from the current verified world revision. Authored-source movement tools are inactive while this world is selected.';this.spatial.controls();$('spatial-source').textContent='PERSISTENT WORLD · '+observed.title+' · stable native body identities · declared synthetic/labelled properties';}
 accept(g){if(g?.construction051){this.anyGoal=g;this.controls();return;}this.updateFirstSkill(g);this.anyGoal=g;this.counterfactual(g?.counterfactual045);if(g?.procedure!=='persistent-world-arrange-v1'){if(g&&['running','waiting-host'].includes(g.status)&&this.spatial.worldMode){this.spatial.worldMode=false;this.spatial.poseMode=false;this.spatial.removePoseTargets();}this.controls();return;}this.g=g;if(g.worldBinding?.worldId!==this.id&&!this.opening){this.opening=true;this.select(g.worldBinding.worldId).catch(e=>this.notice(e.message)).finally(()=>{this.opening=false;});}this.controls();}
 counterfactual(c){const rows=$('cf045-panel-table').tBodies[0];rows.replaceChildren();if(!c){$('cf045-panel-status').textContent='Confirm a world and target poses to compare native alternatives.';$('cf045-panel-result').textContent='';return;}for(const v of c.candidates||[]){const tr=document.createElement('tr'),m=v.metrics||{};for(const x of [v.id,v.status,m.timeS===undefined?'--':m.timeS.toFixed(3),m.effortNs===undefined?'--':m.effortNs.toFixed(4),m.maxBystanderDriftM===undefined?'--':m.maxBystanderDriftM.toFixed(6)]){const td=document.createElement('td');td.textContent=x;tr.append(td);}rows.append(tr);}$('cf045-panel-status').textContent='Java '+c.phase+' - source '+c.sourceFrameSha256.slice(0,16)+(c.selectedCandidate?' - selected '+c.selectedCandidate:'');$('cf045-panel-result').textContent=c.comparison?'Actual forecast residual '+c.comparison.maxPositionResidualM.toFixed(8)+'m / '+c.comparison.maxAngleResidualDeg.toFixed(8)+'deg; matched trace coverage '+c.comparison.sampleCoverage:'The saved world remains unchanged until verified selection and current-head ownership.';}
 controls(){const held=!!this.counterTaskId||this.anyGoal&&['running','waiting-host'].includes(this.anyGoal.status);for(const id of ['persistent-worlds','persistent-create','persistent-load','persistent-case','persistent-use-step','persistent-step','persistent-confirm','persistent-apply','persistent-force','persistent-torque','persistent-targets','persistent-skill','persistent-revisions'])$(id).disabled=this.busy||held;for(const input of $('persistent-target-rows').querySelectorAll('input'))input.disabled=this.busy||held;$('persistent-run').disabled=this.busy||held||!this.bound||Boolean(this.observed?.pending);$('cf045-run').disabled=$('persistent-run').disabled;$('cf045-review').disabled=this.busy||held;$('cf045-export').disabled=this.busy||held||!(this.g?.counterfactual045?.comparison||this.g?.counterfactual045?.awaitingReview);$('persistent-restore').disabled=this.busy||held||!this.id;$('persistent-export').disabled=this.busy||held||!this.id;}
 async refresh(){if(this.refreshing)return;this.refreshing=true;try{if(this.counterTaskId){const task=await this.call('counter-status',{id:this.counterTaskId});if(['completed','cancelled','interrupted'].includes(task.status)){this.counterTaskId=null;if(task.status!=='completed')this.notice(task.reason);}}if(!this.suite){this.suite=await this.call('suite');$('persistent-case').replaceChildren(...this.suite.cases.map(c=>{const o=document.createElement('option');o.value=c.id;o.textContent=c.id+' · '+c.split;return o;}));this.steps();}const list=await this.call('list');$('persistent-worlds').replaceChildren(...list.worlds.map(w=>{const o=document.createElement('option');o.value=w.worldId;o.textContent=w.title+' · revision '+w.expectedRevision+(w.pending?' · owned':'');return o;}));if(!this.id){const saved=localStorage.getItem('persistent-world');if(list.worlds.some(w=>w.worldId===saved))this.id=saved;else this.id=list.worlds.at(-1)?.worldId;if(this.id){this.refreshing=false;await this.select(this.id);return;}}if(!this.id)return;$('persistent-worlds').value=this.id;const observed=await this.call('observe',{worldId:this.id});if(this.bound&&this.bound.expectedHash!==observed.expectedHash)this.invalidate();this.observed=observed;const world=await this.call('read',{worldId:this.id});const chosen=$('persistent-revisions').value;$('persistent-revisions').replaceChildren(...world.revisions.map(r=>{const o=document.createElement('option');o.value=r.revision;o.textContent='Revision '+r.revision+' · '+r.kind+' · '+(r.frame.elapsedTicks/240).toFixed(2)+' simulated s';return o;}));$('persistent-revisions').value=world.revisions.some(r=>String(r.revision)===chosen)?chosen:observed.expectedRevision;$('persistent-restore').textContent=observed.pending?'Abandon attempt & restore selected revision':'Restore selected verified revision';$('persistent-status').textContent=observed.title+' · verified revision '+observed.expectedRevision+' · '+observed.expectedHash.slice(0,12)+(observed.pending?' · pending owned attempt; Resume or explicitly restore':' · ready for the next request');$('persistent-body-summary').textContent=observed.frame.bodies.length+' stable dynamic bodies · '+(observed.frame.elapsedTicks/240).toFixed(2)+' committed simulated seconds';$('persistent-evidence').textContent=JSON.stringify({binding:{worldId:this.id,revision:observed.expectedRevision,hash:observed.expectedHash},pending:observed.pending,bodies:observed.frame.bodies,native:observed.nativeObservation?.sample?{tick:observed.nativeObservation.sample.tick,bodies:observed.nativeObservation.sample.bodies,force:observed.nativeObservation.sample.force,torque:observed.nativeObservation.sample.torque}:null},null,2);this.draw(observed);this.controls();}finally{this.refreshing=false;}}
}
