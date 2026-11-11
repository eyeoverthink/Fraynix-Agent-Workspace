import {Useful072} from './useful072.js';
import {Uncertain071} from './uncertain071.js';
import {Adaptive070} from './adaptive070.js';
import {Later069} from './later069.js';
import {Calibration068} from './calibration068.js';
import {Temporal067} from './temporal067.js';
import {Recovery066} from './recovery066.js';
import {Decision065} from './decision065.js';
import {Contact064} from './contact064.js';
import {Stopping063} from './stopping063.js';
import {Residual062} from './residual062.js';
import {Cortex061} from './cortex061.js';
import {EvidenceBench049} from './evidence049.js';
const $=id=>document.getElementById(id);
export class BrainGame {
 async openIntegrated(){this.requireSaved();this.activate('game');if(!this.running){const state=await this.call('gameRuntime','state');this.show(await this.call('gameRuntime',state.running?'frame':'open'));}}
 constructor({call,notice,activate,layout,lab,requireSaved}){Object.assign(this,{call,notice,activate,layout,lab,requireSaved});this.running=false;this.busy=false;this.polling=false;
  const safe=f=>async()=>{try{await f();}catch(e){this.notice(e.message);$('brain-status').textContent=e.message;}};
  this.addSkillMemory();
  this.addLevelBuilder();
  this.cortex061=new Cortex061(call,notice);
  this.residual062=new Residual062(call,notice);
  this.stopping063=new Stopping063(call,notice);
  this.contact064=new Contact064(call,notice);this.decision065=new Decision065(call,notice);this.recovery066=new Recovery066(call,notice);this.temporal067=new Temporal067(call,notice);this.calibration068=new Calibration068(call,notice);this.later069=new Later069(call,notice);this.adaptive070=new Adaptive070(call,notice);this.uncertain071=new Uncertain071(call,notice);this.useful072=new Useful072(call,notice);
  this.evidenceBench=new EvidenceBench049(call);
  $('brain-open').onclick=safe(async()=>{
   $('brain-dialog').showModal();this.layout();await this.refresh();
   if($('skill-memory-panel').open)await this.skillMemoryAction('state');
  });
  $('brain-close').onclick=()=>{$('brain-dialog').close();this.layout();};$('brain-dialog').addEventListener('close',this.layout);
  $('game-tab').onclick=safe(()=>this.openIntegrated());
  for(const id of ['left','right','jump','step','play','pause','ai','reset'])$('game-'+id).onclick=safe(async()=>{const args=id==='left'||id==='right'||id==='jump'||id==='step'?{action:'step',ticks:30,horizontal:id==='left'?-1:id==='right'?1:0,jump:id==='jump'}:id==='ai'?{action:'ai',targetX:Number($('game-target').value)}:{action:id};this.show(await this.call('gameRuntime','command',args));});
  $('game-close').onclick=safe(async()=>{await this.call('gameRuntime','close');this.running=false;$('game-status').textContent='Runtime closed. Reopen with Start game.';});
  $('game-open').onclick=safe(async()=>this.show(await this.call('gameRuntime','open')));
  for(const kind of ['pulse','gpu-check','train','feedback-train'])$('brain-'+kind).onclick=safe(async()=>{const job=await this.call('brain','start',{kind,autoPromote:$('brain-auto').checked});this.jobId=job.id;await this.refresh();});
  this.lab.feedbackProgress=value=>this.feedback(value);
  $('brain-feedback-rollback').onclick=safe(async()=>{await this.call('brain','feedback-rollback',{expectedVersion:this.feedbackVersion});await this.refresh();});
  $('brain-feedback-run').onclick=safe(async()=>{this.requireSaved();if(!this.lab.scene)throw Error('Open a saved 3D Lab scene first');const s=this.lab.scene;await this.call('brain','feedback-start',{id:s.id,expectedVersion:s.version,targetM:Number($('brain-target').value)});$('brain-dialog').close();this.activate('scene');$('scene-settings').open=false;this.layout();});
  for(const [id,dv]of [['feedback-kick-forward',.4],['feedback-kick-back',-.4]])$(id).onclick=safe(async()=>{await this.call('brain','feedback-kick',{runId:this.feedbackRunId,operationId:crypto.randomUUID(),deltaVelocityMps:dv});});
  $('feedback-stop').onclick=safe(async()=>{await this.call('brain','feedback-cancel',{runId:this.feedbackRunId});});
  $('brain-cancel').onclick=safe(async()=>{if(this.jobId)await this.call('brain','cancel',{id:this.jobId});});
  $('brain-rollback').onclick=safe(async()=>{await this.call('brain','rollback',{expectedVersion:this.version});await this.refresh();});
  $('brain-run').onclick=safe(async()=>{this.requireSaved();if(!this.lab.scene)throw Error('Open and save a 3D Lab scene first');const s=this.lab.scene;await this.call('brain','run',{id:s.id,expectedVersion:s.version,targetM:Number($('brain-target').value),gpu:$('brain-gpu').checked});$('brain-dialog').close();this.activate('scene');this.layout();this.notice('Actual policy run completed; inspect its measured endpoint and retained run.');});
  $('memory-save').onclick=safe(async()=>{const receipt=await this.call('memory','remember',{text:$('memory-text').value});$('memory-result').textContent=JSON.stringify(receipt,null,2);});
  $('memory-search').onclick=safe(async()=>{$('memory-result').textContent=JSON.stringify(await this.call('memory','context',{query:$('memory-text').value,maxChars:2400}),null,2);});
  setInterval(()=>this.poll().catch(e=>this.notice(e.message)),150);
 }
 addLevelBuilder(){
  const box=document.createElement('details');box.id='game-level-builder';
  box.innerHTML=`<summary>Create a level from the original components</summary>
   <p>Java reuses the original player, terrain and power-up logic. Saved candidates are separate from your authored level. Construction checks do not certify playability.</p>
   <div class="runtime-controls"><button id="game-level-analyze">Analyze original</button><label>Layout seed <input id="game-level-seed" type="number" min="0" max="9999" value="0"></label><label>Length (tiles) <input id="game-level-length" type="number" min="48" max="72" value="56"></label><label>Gap (tiles) <input id="game-level-gap" type="number" min="1" max="2" value="2"></label><button id="game-level-create">Create separate level</button></div>
   <div class="runtime-controls"><select id="game-level-choice" aria-label="Saved level candidate"></select><button id="game-level-refresh">Refresh saved levels</button><button id="game-level-open" disabled>Open selected level</button></div><p id="game-level-status" role="status">No new level created. Image reconstruction is a later phase.</p>`;
  $('game-view').prepend(box);
  const action=async command=>{if(this.levelBusy)return;this.levelBusy=true;for(const b of box.querySelectorAll('button'))b.disabled=true;
   try{
    let result;
    if(command==='create-level')result=await this.call('gameRuntime',command,{seed:Number($('game-level-seed').value),lengthTiles:Number($('game-level-length').value),gapTiles:Number($('game-level-gap').value)});
    else if(command==='open-level'){
     const id=$('game-level-choice').value,candidate=this.levelCandidates.find(x=>x.id===id);if(!candidate?.checked)throw Error('Select a checked saved level');
     this.show(await this.call('gameRuntime',command,{id}));$('game-target').value=String(candidate.finishX+1);$('game-level-status').textContent='Opened saved candidate '+id+'. Use controls or AI play to test it. Finish contact is reported separately.';return;
    }else if(command==='analyze-levels'){
     result=await this.call('gameRuntime',command,{});$('game-level-status').textContent=`Original: ${result.objects} objects, ${result.components} components, ${Object.keys(result.componentTypes).length} types. Root level.txt: ${result.rootExampleObjects} example objects. Source ${result.sourceSha256.slice(0,12)}.`;return;
    }
    const list=await this.call('gameRuntime','levels',{});this.levelCandidates=list.candidates;const selected=result?.id||$('game-level-choice').value;
    $('game-level-choice').replaceChildren(...list.candidates.map(c=>new Option(c.checked?`Seed ${c.parameters.seed} · ${c.objects} objects · ${c.id.slice(0,8)}`:`Needs attention · ${c.error}`,c.id)));
    if(list.candidates.some(c=>c.id===selected))$('game-level-choice').value=selected;
    $('game-level-status').textContent=result?`Saved ${result.objects} objects as ${result.id}. Source, asset and construction checks passed. Playtest still required.`:`${list.candidates.length} saved candidates. Each was freshly checked; invalid originals remain listed.`;
   }catch(e){$('game-level-status').textContent=e.message;this.notice(e.message);}
   finally{this.levelBusy=false;for(const b of box.querySelectorAll('button'))b.disabled=false;$('game-level-open').disabled=!this.levelCandidates?.some(c=>c.id===$('game-level-choice').value&&c.checked);}
  };
  for(const [id,command] of [['analyze','analyze-levels'],['create','create-level'],['refresh','levels'],['open','open-level']])$('game-level-'+id).onclick=()=>action(command);
  $('game-level-choice').onchange=()=>{$('game-level-open').disabled=!this.levelCandidates?.some(c=>c.id===$('game-level-choice').value&&c.checked);};
 }
 addSkillMemory(){
  const panel=document.createElement('details');
  panel.id='skill-memory-panel';
  panel.innerHTML=`<summary>Skill recall · compare three methods</summary>
   <p>Teach a phrase for a saved world skill. Compare suggestions for another phrase, then inspect the skill in Verified skills.</p>
   <p id="skill-memory-state" role="status">Open to load saved world skills.</p>
   <div class="skill-memory-fields">
    <label>Verified world skill<select id="skill-memory-choice"></select></label>
    <label>Teaching phrase or recall query
     <input id="skill-memory-phrase" maxlength="240" placeholder="Arrange the objects beside the marker">
    </label>
   </div>
   <div class="runtime-controls">
    <button id="skill-memory-teach" disabled>Teach this phrase</button>
    <button id="skill-memory-recall" disabled>Compare recall</button>
   </div>
   <div id="skill-memory-result" aria-live="polite"></div>`;
  $('brain-state').after(panel);
  panel.ontoggle=()=>{if(panel.open)this.skillMemoryAction('state');};
  $('skill-memory-teach').onclick=()=>this.skillMemoryAction('teach');
  $('skill-memory-recall').onclick=()=>this.skillMemoryAction('recall');
 }
 async skillMemoryAction(command){
  if(this.skillMemoryBusy)return;
  this.skillMemoryBusy=true;
  const teach=$('skill-memory-teach'),recall=$('skill-memory-recall'),choice=$('skill-memory-choice');
  teach.disabled=recall.disabled=true;
  const result=$('skill-memory-result');
  result.replaceChildren();
  try{
   const args=command==='state'?{}:{phrase:$('skill-memory-phrase').value};
   if(command==='teach')args.skill=this.skillMemoryChoices[Number(choice.value)];
   const response=await this.call('skillMemory',command,args);
   if(command==='state'){
    this.skillMemoryChoices=response.choices;
    choice.replaceChildren(...response.choices.map((skill,index)=>new Option(skill.name+' · v'+skill.version,String(index))));
   }
   this.skillMemoryAvailable=response.available;
   $('skill-memory-state').textContent=response.available
    ? `${response.examples} taught phrase${response.examples===1?'':'s'} · ${response.skillCount} remembered skill${response.skillCount===1?'':'s'} · checkpoint ${response.sha256.slice(0,12)}`
    : response.reason;
   if(command==='teach')result.textContent=response.changed?'Phrase saved in Java skill memory.':'This phrase was already taught for that skill.';
   if(command==='recall'){
    const rows=document.createElement('ul');
    for(const [key,label] of [['exact','Exact phrase'],['lexical','Keyword overlap'],['hdc','Packed HDC']]){
     const match=response[key],row=document.createElement('li');
     row.textContent=match.status==='suggestion'
      ? `${label} · ${match.skill.name} v${match.skill.version} · score ${match.score.toFixed(3)}`
      : `${label} · No suggestion${match.reason?' · '+match.reason:''}`;
     rows.append(row);
    }
    result.append(rows);
   }
  }catch(error){
   result.textContent=error.message;
   if(command==='state')this.skillMemoryAvailable=false;
  }finally{
   this.skillMemoryBusy=false;
   recall.disabled=!this.skillMemoryAvailable;
   teach.disabled=recall.disabled||!this.skillMemoryChoices?.length;
  }
 }
 show(frame){this.running=true;$('game-frame').src='data:image/png;base64,'+frame.pngBase64;const s=frame.state,p=s.player;this.lastFrame=frame;$('game-status').textContent=`Original Java renderer · tick ${s.tick} · ${s.objects} objects\nPlayer X ${p.x.toFixed(3)} / Y ${p.y.toFixed(3)} · ${p.dead?'dead':p.won?'goal reached':p.onGround?'on ground':'airborne'}\nCapture ${Number(frame.captureMs).toFixed(1)} ms · bridge ${Number(frame.bridgeMs).toFixed(1)} ms · hidden runtime · no editor panels`;}
 async refresh(){const s=await this.call('brain','state');this.cortex061.refresh(s);this.residual062.refresh(s);this.stopping063.refresh(s);this.contact064.refresh(s);this.decision065.refresh(s);this.recovery066.refresh(s);this.temporal067.refresh(s);this.calibration068.refresh(s);this.later069.refresh(s);this.adaptive070.refresh(s);this.uncertain071.refresh(s);this.useful072.refresh(s);this.busy=s.busy;this.version=s.activePolicy.version;this.feedbackVersion=s.activeFeedback.version;$('brain-feedback-rollback').disabled=s.busy||this.feedbackVersion<2;$('brain-feedback-run').disabled=s.busy;const latest=s.jobs.at(-1);if(latest)this.jobId=latest.id;
  $('brain-state').textContent=`Java coordinator · policy v${this.version} (${s.activePolicy.kind}) · feedback v${this.feedbackVersion} (${s.activeFeedback.kind}) · ${s.journalEntries} journal entries · shared cortex ${s.cortex.ready?s.cortex.neurons+' neurons / '+s.cortex.synapses+' synapses / tick '+s.cortex.ticks:'ready to load on pulse'}`;
  for(const kind of ['pulse','gpu-check','train','feedback-train'])$('brain-'+kind).disabled=s.busy;$('brain-cancel').disabled=!s.busy;$('brain-rollback').disabled=s.busy||this.version<2;$('brain-run').disabled=s.busy;
  if(latest){$('brain-status').textContent=`${latest.kind}: ${latest.status} · ${latest.phase} · ${latest.total?latest.progress+'/'+latest.total:''}`;$('brain-status').dataset.status=latest.status;const stamp=[latest.id,latest.status,latest.phase,latest.progress].join('/');const j=['contact-uncertainty','decision-value','bounded-recovery','temporal-recovery','calibrated-recovery','later-recovery','adaptive-observation'].includes(latest.kind)?latest:this.jobStamp===stamp?this.lastJob:await this.call('brain','read',{id:latest.id});this.jobStamp=stamp;this.lastJob=j;const e=j.evaluation,f=j.feedbackEvaluation;$('brain-result').textContent=f?`${f.accepted?'Feedback accepted':'Feedback rejected'} · score ${f.meanScores['analytical-feedback'].toFixed(4)} → ${f.meanScores.candidate.toFixed(4)} vs hand-designed feedback · worst endpoint ${(100*f.maxEndpointErrorM).toFixed(2)} cm · terminal speed ≤ ${f.maxTerminalSpeedMps.toFixed(3)} m/s · ${j.feedbackPromotion?'activated v'+j.feedbackPromotion.version:'candidate retained'}`:e?`${e.accepted?'Validation accepted':'Validation rejected'} · mean error ${e.baselineMeanErrorM.toFixed(3)} → ${e.trainedMeanErrorM.toFixed(3)} m · ${(e.relativeMeanGain*100).toFixed(1)}% improvement · worst ${e.maxTrainedErrorM.toFixed(3)} m · ${j.promotion?'activated as policy v'+j.promotion.version:'candidate retained without activation'}`:j.kind==='gpu-check'&&j.cases?`${j.device}\nCPU/GPU parity passed for ${j.cases.length} workloads. Timings include transfer overhead; open evidence for measured costs.`:j.kind==='pulse'&&j.ticks?`Cortex tick ${j.ticks} saved · ${j.neurons} neurons · ${j.synapses} synapses. Diagnostic computation; physical learning is measured separately.`:'';const compact={...j};for(const key of ['training','validation','trainingDefinitions','validationDefinitions','search','feedbackTrials','worldTraining','worldCalibration','worldTestEpisodes','worldPlanningTrials','diagnostics063','calibration063','reserved063','diagnostics064','calibration064','reserved064','stress064'])delete compact[key];$('brain-evidence').textContent=JSON.stringify(compact,null,2);}
 }
 feedback(value){const active=value.scope==='feedback-policy';$('feedback-live').hidden=!active;if(!active)return;this.feedbackRunId=value.id;const cur=value.current,p=value.feedbackProgress,m=value.feedbackEvaluation,v=cur.velocity,a=value.definition.parameters.angleDeg*Math.PI/180,speed=v[0]*Math.cos(a)-v[1]*Math.sin(a);$('feedback-live-status').dataset.status=value.status;$('feedback-live-status').textContent=`${value.status} · policy v${value.controller.version} · tick ${cur.tick}/360 · ${p?.actions||0} actual impulses · position ${cur.distanceM.toFixed(3)} / target ${value.controller.targetM.toFixed(3)} m · velocity ${speed.toFixed(3)} m/s · ${cur.contacts} contacts`+(m?` · endpoint error ${(m.endpointErrorM*100).toFixed(2)} cm · ${m.passed?'goal accepted':'goal not accepted'}`:'');for(const id of ['feedback-kick-forward','feedback-kick-back','feedback-stop'])$(id).disabled=!['running','paused'].includes(value.status);}
 async poll(){if(this.polling)return;this.polling=true;try{if(this.running&&!$('game-view').hidden)this.show(await this.call('gameRuntime','frame'));if($('brain-dialog').open)await this.refresh();}finally{this.polling=false;}}
}
