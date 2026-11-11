const $=id=>document.getElementById(id);
export class WorkflowPanel {
 constructor({call,getState,getModel,requireSaved,notice,layout,onGoal,openFirstSkill}){
  this.openFirstSkill=openFirstSkill;
  Object.assign(this,{call,getState,getModel,requireSaved,notice,layout,onGoal});const open=document.createElement('button');open.id='workflow-open';open.className='quiet';open.textContent='Run a complete evidence task  -> ';document.querySelector('.creation-dialog h2').after(open);open.onclick=()=>{$('creation-dialog').close();this.open();};
  const dialog=document.createElement('dialog');dialog.id='workflow-dialog';dialog.innerHTML=`<div class="creation-dialog"><button id="workflow-close" class="quiet">Close</button><p class="eyebrow">ANALYZE  ->  SIMULATE  ->  CREATE  ->  VERIFY</p><h2>One task, verified stages</h2><p>Analyze selected observations, complete a separate native physics goal, then create an interactive explorer. Java keeps progress and independently checks every displayed data cell, native chart point and readable table layout at two screen sizes.</p><label>Observation source<select id="workflow-dataset"></select></label><label>Title<input id="workflow-title" value="Observation & prediction explorer" maxlength="140"></label><label>Request<textarea id="workflow-request" rows="3" maxlength="4000">Analyze these observations, predict and reach a target despite a disturbance, then create an interactive explorer of the actual results. Keep data quality and source limitations visible.</textarea></label><label>Keep successful workflow as<input id="workflow-skill" value="evidence-explorer" maxlength="56"></label><details><summary>Data criteria & separate simulation conditions</summary><label>Maximum observation gap  |  seconds<input id="workflow-gap" type="number" value="0.25" step="any"></label><label>Observation speed limit  |  m/s<input id="workflow-speed" type="number" value="5" step="any"></label><label>Simulation slope  |  degrees<input id="workflow-angle" type="number" value="25" min="10" max="40"></label><label>Simulation friction<input id="workflow-friction" type="number" value="0" min="0" max="0.3" step="0.01"></label><label>Simulation target  |  metres<input id="workflow-target" type="number" value="1.2" min="0.4" max="1.8" step="0.1"></label><label>Velocity disturbance  |  m/s<input id="workflow-disturbance" type="number" value="0.35" min="-0.5" max="0.5" step="0.05"></label></details><p id="workflow-source-note"></p><small>Observations and simulation remain separate. Quality flags stay visible. This does not calibrate a physical experiment or train LLM weights.</small><button id="workflow-build" class="primary">Run complete task</button></div>`;document.body.append(dialog);this.dialog=dialog;dialog.addEventListener('close',layout);$('workflow-close').onclick=()=>dialog.close();$('workflow-build').onclick=()=>this.build();$('workflow-dataset').onchange=()=>this.sourceInfo();this.graphControls();
 }
 graphControls(){
  this.historyControls110();
  const button=document.createElement('button');button.id='graph046-open';button.textContent='Connect verified skills  -> ';document.querySelector('#persistent-world-panel > .persistent-toolbar').append(button);button.onclick=()=>this.openGraph();
  const d=document.createElement('dialog');d.id='graph046-dialog';d.innerHTML=`<div class="creation-dialog"><button id="graph046-close" class="quiet">Close</button><p class="eyebrow">VERIFIED PROCEDURES  ->  ONE EVOLVING WORLD</p><h2>Connect what already works</h2><p>Java runs each exact saved procedure, checks its original results and passes its committed world to the next. The final comparison is checked in the canvas.</p><label>World to continue<select id="graph046-world"></select></label><label>First verified procedure<select id="graph046-first"></select></label><label>Then verified procedure<select id="graph046-second"></select></label><p id="graph046-binding" role="status"></p><label>Keep successful graph as<input id="graph046-name" placeholder="Optional graph skill name" maxlength="56"></label><label><input id="graph046-review" type="checkbox"> Pause for my review between procedures</label><details><summary>Targets, typed links &amp; authority</summary><p>Metres in native3D coordinates. Force and torque cannot exceed each saved procedure's limits. Imported text cannot add handlers.</p><textarea id="graph046-json" rows="16" spellcheck="false" aria-label="Typed skill graph JSON"></textarea></details><p id="graph046-message">Save at least one successful world procedure before connecting skills. Reusing a skill always performs fresh physical and report checks.</p><button id="graph046-run" class="primary">Run connected task</button></div>`;document.body.append(d);this.graphDialog=d;$('graph046-close').onclick=()=>d.close();d.addEventListener('close',this.layout);$('graph046-first').onchange=$('graph046-second').onchange=()=>this.graphDefinition();$('graph046-world').onchange=()=>this.graphWorld();$('graph046-run').onclick=()=>this.runGraph();window.openSkillGraph046=skill=>this.openGraph(skill);
  const firstSkill=document.createElement('button');
  firstSkill.id='graph047-first-skill';
  firstSkill.className='quiet';
  firstSkill.textContent='Create first world skill';
  $('graph046-message').after(firstSkill);
  firstSkill.onclick=()=>{
   d.close();
   this.openFirstSkill();
  };
  $('graph046-world').onchange=async()=>{
   try{
    await this.graphWorld();
    if(this.graphPractice)this.graphDefinition();
    else $('graph046-run').disabled=!this.graphBinding||(!this.graphRetained&&!this.graphSkills.length);
   }catch(error){this.notice(error.message);}
  };
 }
 historyControls110(){
  const open=document.createElement('button');open.id='history110-open';open.textContent='History';open.title='Inspect and archive completed workflow history';document.querySelector('.header-actions').append(open);
  const dialog=document.createElement('dialog');dialog.id='history110-dialog';dialog.innerHTML=`<div class="creation-dialog"><button id="history110-close" class="quiet">Close</button><p class="eyebrow">PRESERVE  ->  VERIFY  ->  CONTINUE</p><h2>Task history</h2><section aria-labelledby="history112-heading"><h3 id="history112-heading">Completed chat tasks</h3><p id="history112-counts"></p><p>Keep the full conversation, action journal and evidence while freeing completed-task slots. The active chat limit stays 24.</p><button id="history112-refresh">Refresh chat preview</button><button id="history112-archive" class="primary" disabled>Verify &amp; archive completed chat tasks</button><p id="history112-status" role="status"></p><label>Retained chat task<select id="history112-record"></select></label><button id="history112-inspect">Inspect original chat</button><button id="history112-verify">Recheck chat evidence</button><details><summary>Original task and evidence</summary><pre id="history112-detail"></pre></details><small>Interrupted, stopped and unsupported tasks stay available. Archiving preserves the original verified or unverified outcome; it does not repeat actions.</small></section><hr><h3>Workflow history</h3><p>Move completed data workflows and their verified child goals out of active scheduling. Their original records stay intact and are checked again whenever a procedure needs them.</p><p id="history110-counts"></p><p>Archive workflows and child goals separately after their evidence checks. Datasets, reports and saved skills stay in place. The active-goal limit remains 64.</p><button id="history110-refresh">Refresh preview</button><button id="history110-archive" class="primary" disabled>Verify &amp; archive completed data workflows</button><p id="history110-status" role="status"></p><label>Retained workflow<select id="history110-record"></select></label><button id="history110-inspect">Inspect original</button><button id="history110-verify">Recheck original evidence</button><pre id="history110-detail"></pre><hr><h3 id="history111-heading">Completed child goals</h3><p id="history111-counts"></p><button id="history111-archive" class="primary" disabled>Verify &amp; archive completed child goals</button><p id="history111-status" role="status"></p><label>Retained child goal<select id="history111-record"></select></label><button id="history111-inspect">Inspect child original</button><button id="history111-verify">Recheck child evidence</button><pre id="history111-detail"></pre><small>Active, interrupted, cancelled, unpublished and other workflow families stay in active history. Missing or changed originals refuse verification. No archive import or evidence deletion.</small></div>`;
  document.body.append(dialog);dialog.addEventListener('close',this.layout);const request=(command,args={})=>window.workspace.request({operation:'workflow',command,args});
  const refresh=async()=>{this.history110=await request('history');const h=this.history110;$('history110-counts').textContent=`${h.activeWorkflows} / 20 active workflows  |  ${h.activeSupervisors} / 16 supervisors  |  ${h.archived} archived  |  ${h.goals} / 64 active child goals`;$('history110-record').replaceChildren(...h.records.map(r=>new Option(`${r.archived?'Archived':r.status}  |  ${r.title}  |  ${r.id.slice(0,8)}`,r.id)));$('history110-archive').disabled=!h.eligible;$('history110-status').textContent=`${h.eligible} completed data records can be checked for archival.`;await children();await chats();this.layout();};
  const chatRequest=(command,args={})=>window.workspace.request({operation:'agency043',command,args});
  const chats=async()=>{const h=await chatRequest('history');this.chats112=h;$('history112-counts').textContent=`${h.active} / 24 active chat tasks | ${h.archived} archived chat tasks`;$('history112-record').replaceChildren(...h.records.map(r=>new Option(`${r.archived?'Archived':r.status} | ${r.outcome||'unfinished'} | ${r.goal.slice(0,90)}`,r.id)));$('history112-archive').disabled=!h.eligible;$('history112-status').textContent=`${h.eligible} completed tasks available for evidence checks.`;};
  const chatSafe=fn=>async()=>{try{await fn();}catch(error){$('history112-status').textContent=error.message;this.notice(error.message);}};
  $('history112-refresh').onclick=chatSafe(chats);
  $('history112-archive').onclick=chatSafe(async()=>{const h=this.chats112;if(!h)throw Error('Refresh chat preview first');$('history112-archive').disabled=true;$('history112-status').textContent='Checking original task journals, outcomes and linked evidence...';const r=await chatRequest('archive',{planSha256:h.planSha256,ids:h.records.filter(r=>r.eligible).map(r=>r.id)});await chats();$('history112-status').textContent=`Archived ${r.archivedNow.length} completed chat tasks. Full original records retained; the active limit remains 24.`;});
  $('history112-inspect').onclick=chatSafe(async()=>{const id=$('history112-record').value;if(!id)throw Error('Choose a retained chat task');$('history112-detail').textContent=JSON.stringify(await chatRequest('read',{id}),null,2);$('history112-detail').parentElement.open=true;this.layout();});
  $('history112-verify').onclick=chatSafe(async()=>{const id=$('history112-record').value;if(!id)throw Error('Choose a retained chat task');const r=await chatRequest('verify',{id});$('history112-status').textContent=`Original evidence rechecked in ${(Number(r.verificationNanos)/1e6).toFixed(1)} ms. Original outcome: ${r.outcome}.`;$('history112-detail').textContent=JSON.stringify(r,null,2);});
  const children=async()=>{const h=await request('child-history');this.children111=h;$('history111-counts').textContent=`${h.active} / 64 active child goals  |  ${h.archived} archived child goals`;$('history111-record').replaceChildren(...h.records.map(r=>new Option(`${r.archived?'Archived':r.status}  |  ${r.stage||'standalone'}  |  ${r.id.slice(0,8)}`,r.id)));$('history111-archive').disabled=!h.eligible;$('history111-status').textContent=`${h.eligible} completed children can be checked for archival. Other goals stay active.`;};
  const safe=fn=>async()=>{try{await fn();}catch(error){$('history110-status').textContent=error.message;this.notice(error.message);}};
  open.onclick=safe(async()=>{this.requireSaved();dialog.showModal();await refresh();});$('history110-close').onclick=()=>dialog.close();$('history110-refresh').onclick=safe(refresh);
  $('history110-archive').onclick=safe(async()=>{const h=this.history110;if(!h)throw Error('Refresh the preview first');$('history110-archive').disabled=true;$('history110-status').textContent='Checking original sources, outcomes and saved procedures...';const receipt=await request('archive',{planSha256:h.planSha256,ids:h.records.filter(r=>r.eligible).map(r=>r.id)});await refresh();$('history110-status').textContent=`Archived ${receipt.archivedNow.length} exact original records. Their evidence remains available for fresh checks.`;});
  $('history111-archive').onclick=safe(async()=>{const h=this.children111;if(!h)throw Error('Refresh preview first');$('history111-archive').disabled=true;$('history111-status').textContent='Checking completed parents, original sources, child outcomes and publication...';const receipt=await request('archive-children',{planSha256:h.planSha256,ids:h.records.filter(r=>r.eligible).map(r=>r.id)});await refresh();$('history111-status').textContent=`Archived ${receipt.archivedNow.length} exact child-goal records. The active limit remains 64.`;});
  $('history111-inspect').onclick=safe(async()=>{const id=$('history111-record').value;if(!id)throw Error('Choose a child goal');const g=await request('child-read',{id});$('history111-detail').textContent=JSON.stringify({id:g.id,parent:g.workflowId,stage:g.workflowStage,status:g.status,archive:g.archive111||null,source:g.dataBinding||g.sourceBinding,report:g.report,metrics:g.metrics},null,2);this.layout();});
  $('history111-verify').onclick=safe(async()=>{const id=$('history111-record').value;if(!id)throw Error('Choose a child goal');const proof=await request('verify-child',{id});$('history111-status').textContent=`Original child, parent and evidence verified in ${(Number(proof.verificationNanos)/1e6).toFixed(1)} ms. This is a recheck.`;$('history111-detail').textContent=JSON.stringify(proof,null,2);});
  $('history110-inspect').onclick=safe(async()=>{const id=$('history110-record').value;if(!id)throw Error('Choose a record');const w=await request('read',{id});$('history110-detail').textContent=JSON.stringify({id:w.id,title:w.title,status:w.status,archive:w.archive110||null,source:w.dataBinding,report:w.report,savedSkill:w.savedSkill,stages:w.stages?.map(s=>({name:s.name,status:s.status,workflowId:s.workflowId,goalId:s.goalId})),reason:w.reason},null,2);this.layout();});
  $('history110-verify').onclick=safe(async()=>{const id=$('history110-record').value;if(!id)throw Error('Choose a record');const proof=await request('verify-archive',{id});$('history110-status').textContent=`Original sources, outcomes and publication verified in ${(Number(proof.verificationNanos)/1e6).toFixed(1)} ms. This is a recheck, not a new observation.`;$('history110-detail').textContent=JSON.stringify(proof,null,2);});
 }
 async openGraph(retained=null,practice=null){
  try{
   this.requireSaved();
   this.graphRetained=retained;
   this.graphPractice=practice;
   this.graphBinding=null;
   this.graphSources=(await window.workspace.request({operation:'persistentWorld',command:'list'})).worlds.filter(world=>!world.pending);
   const catalog=await this.call('catalog',{});
   this.graphSkills=catalog.procedures.filter(procedure=>procedure.skill);
   const worlds=$('graph046-world');
   worlds.replaceChildren(...this.graphSources.map(world=>new Option(world.title+'  |  revision '+world.expectedRevision,world.worldId)));
   const wanted=practice?.worldId||$('persistent-worlds').value;
   worlds.value=this.graphSources.some(world=>world.worldId===wanted)?wanted:this.graphSources.at(-1)?.worldId||'';
   if(practice&&worlds.value!==practice.worldId)throw Error('The practice world has an unfinished attempt. Resolve it before connecting skills.');
   for(const id of ['graph046-first','graph046-second']){
    $(id).replaceChildren(...this.graphSkills.map((procedure,index)=>new Option(procedure.skill.name+'  |  v'+procedure.skill.version,String(index))));
    $(id).closest('label').hidden=Boolean(retained);
   }
   if(this.graphSkills.length>1)$('graph046-second').value='1';
   if(practice){
    const index=this.graphSkills.findIndex(procedure=>procedure.skill.name===practice.skill.name&&procedure.skill.version===practice.skill.version);
    if(index<0)throw Error('The exact practice skill version is unavailable');
    $('graph046-first').value=$('graph046-second').value=String(index);
   }
   $('graph046-json').readOnly=Boolean(retained);
   $('graph046-name').value='';
   $('graph046-review').checked=Boolean(retained?.defaults?.reviewEachNode);
   await this.graphWorld();
   this.graphDefinition();
   this.graphDialog.showModal();
   this.layout();
  }catch(error){this.notice(error.message);}
 }
 graphDefinition(){
  const practice=this.graphPractice;
  let usingPractice=false;
  const nodes=['first','second'].map((id,index)=>{
   const procedure=this.graphSkills[Number($('graph046-'+id).value)];
   if(!procedure)return null;
   const {skill,defaults}=procedure;
   const step=practice?.worldId===$('graph046-world').value&&practice.skill.name===skill.name&&practice.skill.version===skill.version
    ? practice.steps[index]:null;
   usingPractice||=Boolean(step);
   const {targets,forceLimitN,torqueLimitNm}=step||defaults;
   return {id,kind:'procedure',skill,source:index?'first':'world',unit:'m',frame:'native3d',targets,forceLimitN,torqueLimitNm};
  });
  const graph=this.graphRetained?.graph||{schema:'fraynix.skill-graph-046-v1',nodes:[...nodes.filter(Boolean),{id:'compare',kind:'compare',sources:['first','second']}]};
  $('graph046-json').value=JSON.stringify(graph,null,2);
  $('graph046-run').disabled=!this.graphBinding||(!this.graphRetained&&!nodes.every(Boolean));
  $('graph047-first-skill').hidden=Boolean(this.graphSkills.length||this.graphRetained);
  $('graph046-message').textContent=this.graphRetained
   ? 'Reuse exact graph '+this.graphRetained.name+' v'+this.graphRetained.version+' with a freshly confirmed source. Every retained skill and original receipt is checked again.'
   : usingPractice?'Use the saved skill with two new practice targets. Java verifies each result and compares them.'
   : this.graphSkills.length?'Two verified procedures and one real comparison. Edit advanced targets only within the saved authority.'
   : 'Create a verified practice skill to unlock the composer.';
 }
 async graphWorld(){
  const id=$('graph046-world').value;
  this.graphBinding=null;
  $('graph046-run').disabled=true;
  if(!id){$('graph046-binding').textContent='Create a persistent world first.';return;}
  $('graph046-binding').textContent='Checking the current world revision...';
  const observation=await window.workspace.request({operation:'persistentWorld',command:'observe',args:{worldId:id}});
  if($('graph046-world').value!==id)return;
  if(observation.pending)throw Error('Resolve the unfinished world attempt first');
  this.graphBinding={worldId:id,expectedRevision:observation.expectedRevision,expectedHash:observation.expectedHash,unit:'m',frame:'native3d'};
  $('graph046-binding').textContent='Confirmed revision '+observation.expectedRevision+'  |  head '+observation.expectedHash.slice(0,16)+'  |  metres / native3D. A changed head refuses execution.';
 }
 async runGraph(){const button=$('graph046-run');button.disabled=true;try{this.requireSaved();if(!this.graphBinding)throw Error('Choose and observe a world');const graph=JSON.parse($('graph046-json').value),request=this.graphRetained?'Reuse saved graph '+this.graphRetained.name+' v'+this.graphRetained.version:'Execute this connected verified procedure graph and compare its actual committed outcomes';if(this.graphRetained){const n=graph.nodes.find(n=>n.kind==='procedure');await window.workspace.request({operation:'persistentWorld',command:'select',args:{worldId:this.graphBinding.worldId,expectedRevision:this.graphBinding.expectedRevision,expectedHash:this.graphBinding.expectedHash,targets:n.targets,forceLimitN:n.forceLimitN,torqueLimitNm:n.torqueLimitNm}});}
  this.graphDialog.close();await this.call('start',{graph,worldBinding:this.graphBinding,title:'Connected verified procedures',request,modelName:this.getModel(),saveSkillName:$('graph046-name').value.trim(),reviewEachNode:$('graph046-review').checked});
 }catch(e){this.notice(e.message);}finally{button.disabled=false;}}
 open(){try{this.requireSaved();this.sources=this.getState()?.datasets||[];const select=$('workflow-dataset');select.replaceChildren();for(const s of this.sources){const o=document.createElement('option');o.value=s.id;o.textContent=`${s.title}  |  v${s.version}`;select.append(o);}select.value=this.sources.at(-1)?.id||'';$('workflow-build').disabled=!this.sources.length;this.sourceInfo();this.dialog.showModal();this.layout();}catch(e){this.notice(e.message);}}
 sourceInfo(){const s=this.sources.find(s=>s.id===$('workflow-dataset').value);$('workflow-source-note').textContent=s?'The selected version and original source hashes will be frozen before execution.':'Import observations in Data tasks first. Existing data and scenes are preserved.';}
 async build(){try{this.requireSaved();const s=this.sources.find(s=>s.id===$('workflow-dataset').value);if(!s)throw Error('Choose observation data first');const n=id=>Number($(id).value),args={datasetId:s.id,datasetVersion:s.version,title:$('workflow-title').value.trim(),request:$('workflow-request').value.trim(),saveSkillName:$('workflow-skill').value.trim(),modelName:this.getModel(),responsive:true,maxGapS:n('workflow-gap'),speedLimitMps:n('workflow-speed'),parameters:{angleDeg:n('workflow-angle'),friction:n('workflow-friction')},targetM:n('workflow-target'),disturbanceMps:n('workflow-disturbance')};this.dialog.close();const w=await this.call('start',args);if(w)this.onGoal(w);}catch(e){this.notice(e.message);}}
}
