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
 async openIntegrated(){this.requireSaved();this.activate('game');this.watching=true;if(!this.running){const state=await this.call('gameRuntime','state');if(state.running){this.show(await this.call('gameRuntime','frame'));return;}const tasks=await this.call('gameTasks115','list',{});if(tasks.tasks.some(t=>t.status==='running')){$('game-frame').hidden=true;$('game-status').textContent='Waiting for the active level task to open its runtime…';return;}try{this.show(await this.call('gameRuntime','open'));}catch(error){if(!String(error.message).includes('The active Java task owns execution'))throw error;$('game-status').textContent='The active task owns the game; waiting for its next frame…';this.notice($('game-status').textContent);}}}
 constructor({call,notice,activate,layout,lab,requireSaved}){Object.assign(this,{call,notice,activate,layout,lab,requireSaved});this.running=false;this.watching=false;this.busy=false;this.polling=false;
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
  $('game-close').onclick=safe(async()=>{await this.call('gameRuntime','close');this.running=false;this.watching=false;$('game-status').textContent='Runtime closed. Reopen with Start game.';});
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
   <div class="runtime-controls"><label>Challenge <select id="game-challenge-choice"><option value="descending">Descending enemy approach</option><option value="late-turtle">Late turtle approach</option><option value="supported-edge">Supported edge recovery</option><option value="return">Return to a cleared pickup</option><option value="reversal">Enemy reverses at a wall</option><option value="wait">Wait for moving threat</option><option value="turtle">Turtle and pickup</option><option value="corner">Corner recovery</option><option value="reconsider">Moving threat and pickup</option><option value="enemy">Enemy and steps</option><option value="clearance">Cramped power-up</option><option value="gap">Pickup above a gap</option></select></label><label>Pickup approach <select id="game-pickup-strategy"><option value="full">Wait and return when safe</option><option value="no-wait">Allow returns; skip waiting</option><option value="no-return">Allow waiting; skip remembered returns</option><option value="direct">Skip optional waits and returns</option></select></label><button id="game-challenge-chat">Prepare challenge request</button></div><p>Challenge tasks compare simple control with the AI. These explicit risk-aware trials must reach the finish; risky pickups can be deferred for a fresh observation or declined with a recorded reason. Bounded waiting can let a moving threat clear. Nearby revisits require observed ground and braking room, with a fixed time limit. Enemy reversals are observed directly; a return attempt is only useful if the item is actually collected. Descending enemy-contact forecasts can choose a supported approach; stationary edge support can admit a bounded retreat. Contact recovery uses measured movement and normal inputs. AI Play also uses the verified recovery controller; old task recipes retain their explicit comparison policy. Choose the same approach across repeated requests to compare cost and outcomes. Disabling optional waits or returns keeps all safety checks and safe forward pickup collection. The default remains unchanged. Failed courses are retained without terrain repair.</p>
   <div class="runtime-controls"><label>Reference PNG <input type="file" id="game-reference-file" accept="image/png"></label><button id="game-reference-import">Analyze reference</button><button id="game-reference-chat" disabled>Prepare reconstruction request</button></div><canvas id="game-reference-preview" hidden aria-label="Reference image with Java component matches"></canvas><p id="game-reference-status">Known original sprites, full two-gap course, 8–32 even pixels per tile. Hidden block behavior is declared; unsupported content is refused.</p><div class="runtime-controls"><select id="game-level-choice" aria-label="Saved level candidate"></select><button id="game-level-refresh">Refresh saved levels</button><button id="game-level-open" disabled>Open selected level</button></div><p id="game-level-status" role="status">Try chat: Create a playable Mario level; construct, playtest and repair it. Optional: seed=0 gapTiles=10 repairLimit=2. Java retains every attempt and resumes interrupted tasks explicitly.</p>`;
  const history=document.createElement('details');history.id='game-task-history';history.innerHTML='<summary>Level task history · attempts and checks</summary><div class="runtime-controls"><button id="game-task-refresh">Refresh tasks</button><select id="game-task-choice" aria-label="Level task"></select><button id="game-task-verify">Recheck evidence</button><button id="game-task-open">Open verified result</button></div><p id="game-task-status" role="status">Start a level request in chat. Progress and Resume are in Mission.</p><pre id="game-task-detail"></pre>';
  $('game-view').prepend(history);
  const taskView=task=>{if(!task)return;$('game-task-status').textContent=task.status+' · '+task.reason;$('game-task-detail').textContent=JSON.stringify({request:task.request,stage:task.stage,repairs:task.repairs,trials:task.trials.map(t=>({control:t.control,status:t.status,ticks:t.final?.tick,inputChunks:t.inputChunks,sessionId:t.sessionId,controller:t.final?.controller})),audit:task.audit},null,2);$('game-task-verify').disabled=task.status!=='completed';$('game-task-open').disabled=task.status!=='completed';};
  $('game-task-refresh').onclick=async()=>{try{const data=await this.call('gameTasks115','list',{});data.tasks.sort((a,b)=>Date.parse(a.startedAt)-Date.parse(b.startedAt)||a.id.localeCompare(b.id));this.gameTasks115=data.tasks;const chosen=$('game-task-choice').value;$('game-task-choice').replaceChildren(...data.tasks.map(t=>new Option(t.status+' · '+t.request.slice(0,65),t.id)));if(data.tasks.some(t=>t.id===chosen))$('game-task-choice').value=chosen;else if(data.tasks.length)$('game-task-choice').value=data.tasks.at(-1).id;taskView(data.tasks.find(t=>t.id===$('game-task-choice').value));}catch(e){$('game-task-status').textContent=e.message;}};
  $('game-task-choice').onchange=()=>taskView(this.gameTasks115?.find(t=>t.id===$('game-task-choice').value));
  for(const action of ['verify','open'])$('game-task-'+action).onclick=async()=>{try{const task=await this.call('gameTasks115','verify',{id:$('game-task-choice').value});taskView(task);$('game-task-status').textContent='Original sources, candidate and trial evidence rechecked.';if(action==='open'){this.show(await this.call('gameRuntime','open-level',{id:task.audit.candidateId}));history.open=false;box.open=false;}}catch(e){$('game-task-status').textContent=e.message;}};
  history.ontoggle=()=>{if(history.open)$('game-task-refresh').click();};
  $('game-view').prepend(box);
  const action=async command=>{if(this.levelBusy)return;this.levelBusy=true;for(const b of box.querySelectorAll('button'))b.disabled=true;
   try{
    let result;
    if(command==='create-level')result=await this.call('gameRuntime',command,{seed:Number($('game-level-seed').value),lengthTiles:Number($('game-level-length').value),gapTiles:Number($('game-level-gap').value)});
    else if(command==='open-level'){
     const id=$('game-level-choice').value,candidate=this.levelCandidates.find(x=>x.id===id);if(!candidate?.checked)throw Error('Select a checked saved level');
     this.show(await this.call('gameRuntime',command,{id}));$('game-target').value=String(candidate.finishX+1);box.open=false;$('game-level-status').textContent='Opened saved candidate '+id+'. Use controls or AI play to test it. Finish contact is reported separately.';return;
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
  $('game-reference-import').onclick=async()=>{this.reference115=null;$('game-reference-chat').disabled=true;$('game-reference-preview').hidden=true;try{const file=$('game-reference-file').files[0];if(!file||file.size>3000000)throw Error('Choose a PNG up to 3 MB');$('game-reference-import').disabled=true;const bytes=new Uint8Array(await file.arrayBuffer());let binary='';for(let i=0;i<bytes.length;i+=8192)binary+=String.fromCharCode(...bytes.subarray(i,i+8192));const ref=await this.call('gameRuntime','import-reference',{pngBase64:btoa(binary)});this.reference115=ref;if(ref.accepted){const image=new Image();image.src='data:image/png;base64,'+btoa(binary);await image.decode();const canvas=$('game-reference-preview'),ctx=canvas.getContext('2d'),grid=ref.analysis.grid;const top=Math.max(0,grid.worldBottomY-grid.tilePx*11);canvas.width=ref.analysis.parameters.lengthTiles*grid.tilePx;canvas.height=Math.min(image.height-top,grid.tilePx*11);ctx.drawImage(image,grid.originX,top,canvas.width,canvas.height,0,0,canvas.width,canvas.height);ctx.lineWidth=1;for(const d of ref.analysis.detections){ctx.strokeStyle=d.role==='player'?'#00ffff':d.role==='question block'?'#ffcf4a':d.role==='finish'?'#ffffff':'#62cead';ctx.strokeRect(d.x-grid.originX+.5,d.y-top+.5,d.sizePx-1,d.sizePx-1);}canvas.hidden=false;}$('game-reference-status').textContent=ref.accepted?`Matched ${ref.analysis.detections.length} components at ${ref.analysis.grid.tilePx} pixels per tile. Source image retained. Playtest is still required. Question-block behavior uses the original Powerup rule.`:'Reference refused: '+ref.reason;$('game-reference-chat').disabled=!ref.accepted;}catch(e){$('game-reference-status').textContent=e.message;}finally{$('game-reference-import').disabled=false;}};
  $('game-challenge-chat').onclick=()=>{const choices={descending:'seed=109 lengthTiles=72 gapTiles=2 secondGapTiles=2 pickupTile=40 goombaTile=42','late-turtle':'seed=113 lengthTiles=72 gapTiles=2 secondGapTiles=2 pickupTile=40 turtleTile=44','supported-edge':'seed=157 lengthTiles=72 gapTiles=3 secondGapTiles=2 pickupTile=11 turtleTile=16 obstacleTile=14 obstacleHeight=2',return:'seed=127 lengthTiles=72 gapTiles=2 secondGapTiles=2 pickupTile=42 goombaTile=46 obstacleTile=60 obstacleHeight=1',reversal:'seed=110 lengthTiles=64 gapTiles=2 secondGapTiles=2 pickupTile=10 goombaTile=15 obstacleTile=13 obstacleHeight=1',wait:'seed=41 lengthTiles=72 gapTiles=3 secondGapTiles=4 goombaTile=15 turtleTile=28 obstacleTile=25 obstacleHeight=2',turtle:'seed=53 lengthTiles=64 gapTiles=2 secondGapTiles=2 turtleTile=15 obstacleTile=26 obstacleHeight=2',corner:'seed=8 lengthTiles=72 gapTiles=4 secondGapTiles=2 goombaTile=26 obstacleTile=24 obstacleHeight=2',reconsider:'seed=23 lengthTiles=64 gapTiles=2 secondGapTiles=3 goombaTile=14 turtleTile=27 obstacleTile=24 obstacleHeight=2',enemy:'goombaTile=26 obstacleTile=24 obstacleHeight=2',clearance:'pickupTile=12 obstacleTile=12 obstacleHeight=2',gap:'pickupTile=17 goombaTile=27 obstacleTile=24 obstacleHeight=2'};$('goal').value='Create a playable Mario level '+choices[$('game-challenge-choice').value]+' repairLimit=0 pickupStrategy='+$('game-pickup-strategy').value+'; compare controls and report skipped pickups.';$('goal').focus();box.open=false;this.notice('Challenge request prepared. Start task to build and playtest it.');};
  $('game-reference-chat').onclick=()=>{if(!this.reference115?.accepted)return;$('goal').value=`Reconstruct a playable Mario level from reference=${this.reference115.id}; playtest and repair with repairLimit=2.`;$('goal').focus();box.open=false;this.notice('Reference request prepared. Start task to construct and test it.');};
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
 show(frame){this.running=true;$('game-frame').hidden=false;$('game-frame').src='data:image/png;base64,'+frame.pngBase64;const s=frame.state,p=s.player;this.lastFrame=frame;$('game-status').textContent=`Original Java renderer · tick ${s.tick} · ${s.objects} objects\nPlayer X ${p.x.toFixed(3)} / Y ${p.y.toFixed(3)} · ${p.dead?'dead':p.won?'goal reached':p.onGround?'on ground':'airborne'}\nCapture ${Number(frame.captureMs).toFixed(1)} ms · bridge ${Number(frame.bridgeMs).toFixed(1)} ms · hidden runtime · no editor panels${s.controller?.pickupSkips?'\nSkipped '+s.controller.pickupSkips+' optional pickup(s): '+s.controller.pickupDecisions.filter(d=>d.action!=='deferred for fresh observation').at(-1)?.reason:''}${s.controller?.pickupDeferrals?'\nDeferred '+s.controller.pickupDeferrals+' pickup checks for fresh observation':''}${(s.controller?.opportunity120?.waitStarts||s.controller?.opportunity120?.revisitStarts)?'\nPickup '+s.controller.opportunity120.phase.toLowerCase()+' · waited '+(s.controller.opportunity120.waitTicks/60).toFixed(2)+' s · '+s.controller.opportunity120.waitClears+' confirmed clear · '+s.controller.opportunity120.waitAborts+' aborted · '+s.controller.opportunity120.revisitStarts+' return attempts':''}${s.controller?.opportunity120?.motion121?'\nObserved enemy reversals: '+s.controller.opportunity120.motion121.reversals+' · uncertain forecasts refused: '+s.controller.opportunity120.motion121.forecastRefusals:''}${s.controller?.enemyLanding122?'\nContact forecast: '+s.controller.enemyLanding122.overrides+' corrections · supported-edge escapes: '+s.controller.enemyLanding122.edgeStarts:''}${s.controller?.contactRecovery119?.attempts?'\nContact recovery '+s.controller.contactRecovery119.phase.toLowerCase()+' · '+s.controller.contactRecovery119.attempts+'/3 attempts':''}`;}
 async refresh(){const s=await this.call('brain','state');this.cortex061.refresh(s);this.residual062.refresh(s);this.stopping063.refresh(s);this.contact064.refresh(s);this.decision065.refresh(s);this.recovery066.refresh(s);this.temporal067.refresh(s);this.calibration068.refresh(s);this.later069.refresh(s);this.adaptive070.refresh(s);this.uncertain071.refresh(s);this.useful072.refresh(s);this.busy=s.busy;this.version=s.activePolicy.version;this.feedbackVersion=s.activeFeedback.version;$('brain-feedback-rollback').disabled=s.busy||this.feedbackVersion<2;$('brain-feedback-run').disabled=s.busy;const latest=s.jobs.at(-1);if(latest)this.jobId=latest.id;
  $('brain-state').textContent=`Java coordinator · policy v${this.version} (${s.activePolicy.kind}) · feedback v${this.feedbackVersion} (${s.activeFeedback.kind}) · ${s.journalEntries} journal entries · shared cortex ${s.cortex.ready?s.cortex.neurons+' neurons / '+s.cortex.synapses+' synapses / tick '+s.cortex.ticks:'ready to load on pulse'}`;
  for(const kind of ['pulse','gpu-check','train','feedback-train'])$('brain-'+kind).disabled=s.busy;$('brain-cancel').disabled=!s.busy;$('brain-rollback').disabled=s.busy||this.version<2;$('brain-run').disabled=s.busy;
  if(latest){$('brain-status').textContent=`${latest.kind}: ${latest.status} · ${latest.phase} · ${latest.total?latest.progress+'/'+latest.total:''}`;$('brain-status').dataset.status=latest.status;const stamp=[latest.id,latest.status,latest.phase,latest.progress].join('/');const j=['contact-uncertainty','decision-value','bounded-recovery','temporal-recovery','calibrated-recovery','later-recovery','adaptive-observation'].includes(latest.kind)?latest:this.jobStamp===stamp?this.lastJob:await this.call('brain','read',{id:latest.id});this.jobStamp=stamp;this.lastJob=j;const e=j.evaluation,f=j.feedbackEvaluation;$('brain-result').textContent=f?`${f.accepted?'Feedback accepted':'Feedback rejected'} · score ${f.meanScores['analytical-feedback'].toFixed(4)} → ${f.meanScores.candidate.toFixed(4)} vs hand-designed feedback · worst endpoint ${(100*f.maxEndpointErrorM).toFixed(2)} cm · terminal speed ≤ ${f.maxTerminalSpeedMps.toFixed(3)} m/s · ${j.feedbackPromotion?'activated v'+j.feedbackPromotion.version:'candidate retained'}`:e?`${e.accepted?'Validation accepted':'Validation rejected'} · mean error ${e.baselineMeanErrorM.toFixed(3)} → ${e.trainedMeanErrorM.toFixed(3)} m · ${(e.relativeMeanGain*100).toFixed(1)}% improvement · worst ${e.maxTrainedErrorM.toFixed(3)} m · ${j.promotion?'activated as policy v'+j.promotion.version:'candidate retained without activation'}`:j.kind==='gpu-check'&&j.cases?`${j.device}\nCPU/GPU parity passed for ${j.cases.length} workloads. Timings include transfer overhead; open evidence for measured costs.`:j.kind==='pulse'&&j.ticks?`Cortex tick ${j.ticks} saved · ${j.neurons} neurons · ${j.synapses} synapses. Diagnostic computation; physical learning is measured separately.`:'';const compact={...j};for(const key of ['training','validation','trainingDefinitions','validationDefinitions','search','feedbackTrials','worldTraining','worldCalibration','worldTestEpisodes','worldPlanningTrials','diagnostics063','calibration063','reserved063','diagnostics064','calibration064','reserved064','stress064'])delete compact[key];$('brain-evidence').textContent=JSON.stringify(compact,null,2);}
 }
 feedback(value){const active=value.scope==='feedback-policy';$('feedback-live').hidden=!active;if(!active)return;this.feedbackRunId=value.id;const cur=value.current,p=value.feedbackProgress,m=value.feedbackEvaluation,v=cur.velocity,a=value.definition.parameters.angleDeg*Math.PI/180,speed=v[0]*Math.cos(a)-v[1]*Math.sin(a);$('feedback-live-status').dataset.status=value.status;$('feedback-live-status').textContent=`${value.status} · policy v${value.controller.version} · tick ${cur.tick}/360 · ${p?.actions||0} actual impulses · position ${cur.distanceM.toFixed(3)} / target ${value.controller.targetM.toFixed(3)} m · velocity ${speed.toFixed(3)} m/s · ${cur.contacts} contacts`+(m?` · endpoint error ${(m.endpointErrorM*100).toFixed(2)} cm · ${m.passed?'goal accepted':'goal not accepted'}`:'');for(const id of ['feedback-kick-forward','feedback-kick-back','feedback-stop'])$(id).disabled=!['running','paused'].includes(value.status);}
 async poll(){if(this.polling)return;this.polling=true;try{if(this.watching&&!this.running&&!$('game-view').hidden){const state=await this.call('gameRuntime','state');this.running=state.running;}if(this.running&&!$('game-view').hidden){try{this.show(await this.call('gameRuntime','frame'));}catch(error){if(!String(error.message).includes('Open embedded Game first'))throw error;this.running=false;$('game-status').textContent='Waiting for the runtime’s next frame…';this.notice($('game-status').textContent);}}if($('brain-dialog').open)await this.refresh();}finally{this.polling=false;}}
}
