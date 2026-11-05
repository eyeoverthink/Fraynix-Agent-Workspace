const $=id=>document.getElementById(id);
const option=(value,text)=>{const o=document.createElement('option');o.value=value;o.textContent=text;return o;};
async function unpackRun(envelope){
 if(envelope.format!=='fraynix.native-world-run-gzip-v1'||envelope.rawBytes>32000000)throw Error('Unsupported original replay envelope');
 const compressed=Uint8Array.from(atob(envelope.gzipBase64),c=>c.charCodeAt(0));
 const bytes=new Uint8Array(await new Response(new Blob([compressed]).stream().pipeThrough(new DecompressionStream('gzip'))).arrayBuffer());
 const sha=Array.from(new Uint8Array(await crypto.subtle.digest('SHA-256',bytes)),n=>n.toString(16).padStart(2,'0')).join('');
 if(bytes.length!==envelope.rawBytes||sha!==envelope.rawSha256)throw Error('Original replay bytes or hash changed');
 return JSON.parse(new TextDecoder().decode(bytes));
}
/** Recorded native frames only. Drawing never writes poses back into the world. */
export class Rehearsal073Panel {
 constructor({call,root,notice,selectWorld}){
  Object.assign(this,{call,notice,selectWorld});this.playing=false;this.runs=[];
  const panel=document.createElement('details');panel.id='rehearsal073';panel.className='spatial-detail';panel.innerHTML=`<summary>Experiment memory · rebuild, vary &amp; verify</summary>
   <p>Recall a verified experiment. Reproduce its starting state, change one component, compare three bounded alternatives, then verify a fresh run in a separate persistent world.</p>
   <div class="persistent-toolbar"><label>Completed arrangement<select id="re073-goals"></select></label><button id="re073-remember">Remember verified experiment</button><button id="re073-refresh">Refresh memories</button></div>
   <div class="persistent-toolbar"><label>Verified memory<select id="re073-episodes"></select></label><label>Object role<select id="re073-role"></select></label><label>Component<select id="re073-property"><option value="friction">Friction · coefficient</option><option value="massKg">Payload mass · kg</option></select></label><label>New value<input id="re073-value" type="number" step="0.1" value="0.2" min="0" max="2"></label></div>
   <p id="re073-source"></p><div class="persistent-toolbar"><label><input id="re073-review" type="checkbox"> Review alternatives before moving</label><button id="re073-start" class="primary">Rehearse through chat</button></div>
   <p class="subtle">Starts at rest with the original controller. Friction0–2; dynamic mass within the existing planner's0.2–4kg range. Moving/contact checkpoints need a future restore contract. Stop and Resume use the existing Java goal controls.</p>
   <label>Retained rehearsal<select id="re073-history"></select></label><div class="persistent-toolbar"><button id="re073-load">Load linked replay</button><button id="re073-world">Open variant world</button><button id="re073-export">Export episode &amp; original traces</button></div>
   <p id="re073-status" role="status">No experiment selected.</p><div id="re073-results"></div>
   <div id="re073-replay" hidden><div class="persistent-toolbar"><label>Recorded run<select id="re073-runs"></select></label><button id="re073-play">Play recorded motion</button><label>Frame<input id="re073-frame" type="range" min="0" max="0" value="0"></label></div>
    <p id="re073-frame-label"></p><div style="display:grid;grid-template-columns:repeat(auto-fit,minmax(240px,1fr));gap:12px"><canvas id="re073-top" width="600" height="320" style="width:100%;background:#111827" aria-label="Recorded top X Z projection"></canvas><canvas id="re073-front" width="600" height="320" style="width:100%;background:#111827" aria-label="Recorded front X Y projection"></canvas></div>
    <p>Solid boxes: selected original native frame. Dashed amber: selected forecast at the same tick when viewing actual execution. Metres,240physics ticks/s; recorded replay, not a new action.</p><pre id="re073-positions" style="max-height:160px;overflow:auto"></pre></div>`;
  root.append(panel);this.root=panel;
  const action=fn=>async()=>{try{await fn();}catch(e){$('re073-status').textContent=e.message;this.notice(e.message);}};
  $('re073-refresh').onclick=action(()=>this.refresh());
  $('re073-remember').onclick=action(async()=>{const e=await call('rehearsal-remember',{goalId:$('re073-goals').value});await this.refresh();$('re073-episodes').value=e.id;this.roles();$('re073-status').textContent='Verified original retained · '+e.id;});
  $('re073-episodes').onchange=()=>this.roles();$('re073-role').onchange=$('re073-property').onchange=()=>this.component();
  $('re073-start').onclick=action(async()=>{const a={episodeId:$('re073-episodes').value,sourceId:$('re073-role').value,property:$('re073-property').value,value:Number($('re073-value').value),review:$('re073-review').checked};if(!a.episodeId)throw Error('Remember and select an eligible experiment first');const t=await call('rehearsal-start',a);this.task=t.id;$('re073-status').textContent='Java chat mission queued · '+t.id;});
  $('re073-load').onclick=action(()=>this.load());$('re073-world').onclick=action(async()=>{const g=this.goals.find(g=>g.id===$('re073-history').value);if(!g)throw Error('Select a rehearsal first');await selectWorld(g.worldBinding.worldId);});
  $('re073-export').onclick=action(async()=>{const r=await call('rehearsal-export',{goalId:$('re073-history').value});notice('Original episode and traces saved: '+r.path);});
  $('re073-runs').onchange=()=>{this.playing=false;$('re073-frame').value=0;this.draw();};$('re073-frame').oninput=()=>this.draw();
  $('re073-play').onclick=()=>{this.playing=!this.playing;$('re073-play').textContent=this.playing?'Pause replay':'Play recorded motion';if(this.playing&&Number($('re073-frame').value)>=Number($('re073-frame').max))$('re073-frame').value=0;};
  setInterval(()=>{if(!this.root.open||!this.playing)return;const f=$('re073-frame');if(Number(f.value)>=Number(f.max)){this.playing=false;$('re073-play').textContent='Play recorded motion';return;}f.value=Number(f.value)+1;this.draw();},50);
  panel.ontoggle=()=>{if(panel.open)action(()=>this.refresh())();else this.playing=false;};
  setInterval(()=>{if(panel.open&&this.task)this.poll().catch(e=>notice(e.message));},1500);
 }
 async poll(){if(this.polling)return;this.polling=true;try{const t=await this.call('counter-status',{id:this.task});$('re073-status').textContent=t.status+' · '+(t.reason||'');if(['completed','interrupted','cancelled'].includes(t.status)){this.task=null;await this.refresh();}}finally{this.polling=false;}}
 async refresh(){
  const memory=await this.call('rehearsal-list'),registry=await this.call('rehearsal-goals');this.episodes=memory.episodes.filter(e=>e.eligible);this.goals=registry.goals;
  const preserve=(id,items)=>{const old=$(id).value;$(id).replaceChildren(...items);if(items.some(o=>o.value===old))$(id).value=old;else if(items.length)$(id).value=items.at(-1).value;};
  preserve('re073-goals',this.goals.filter(g=>g.procedure==='persistent-world-arrange-v1'&&g.status==='completed'&&!g.construction051).map(g=>option(g.id,g.request.slice(0,75)+' · '+g.id.slice(0,8))));
  preserve('re073-episodes',this.episodes.map(e=>option(e.id,(e.lineage?e.lineage.change.sourceId+' '+e.lineage.change.property+'='+e.lineage.change.after:'Verified original')+' · '+e.id.slice(0,8))));
  preserve('re073-history',this.goals.filter(g=>g.rehearsal073).map(g=>option(g.id,g.rehearsal073.change.sourceId+' '+g.rehearsal073.change.property+'='+g.rehearsal073.change.after+' · '+g.status)));
  this.roles();$('re073-start').disabled=!this.episodes.length;$('re073-remember').disabled=!$('re073-goals').value;
  if(memory.episodes.some(e=>!e.eligible))$('re073-status').textContent='Some retained memories are incompatible; original files kept. '+memory.episodes.filter(e=>!e.eligible).map(e=>e.reason).join('; ');
 }
 roles(){const e=this.episodes?.find(e=>e.id===$('re073-episodes').value);$('re073-role').replaceChildren(...(e?.roles||[]).map(r=>option(r.sourceId,r.sourceId+' · '+r.label)));this.component();}
 component(){const e=this.episodes?.find(e=>e.id===$('re073-episodes').value),r=e?.roles.find(r=>r.sourceId===$('re073-role').value),p=$('re073-property').value;$('re073-value').max=p==='friction'?2:20;$('re073-source').textContent=r?'Original '+p+'='+r[p]+' · parent '+e.id+' · '+e.ticks+' native ticks. '+(p==='massKg'&&r.massKg===0?'Static body: choose a dynamic role.':''):'';}
 async load(){
  const id=$('re073-history').value;if(!id)throw Error('Run a rehearsal first');const data=await this.call('rehearsal-replay',{goalId:id});this.data=data;this.playing=false;
  const alternatives=[];for(const item of data.alternatives.originalAttempts){const r=await unpackRun(item.run);alternatives.push({label:item.candidate+' · '+r.status,run:r});}
  this.runs=[{label:'Parent · verified original',run:data.parent.run},...data.rebuilds.map((r,i)=>({label:'Rebuild '+(i+1)+' · '+r.status,run:r})),...alternatives,...(data.actual?[{label:'Actual · '+data.actual.status,run:data.actual}]:[])].filter(x=>x.run.samples?.length);
  $('re073-runs').replaceChildren(...this.runs.map((r,i)=>option(String(i),r.label)));$('re073-runs').value=String(Math.max(0,this.runs.length-1));$('re073-frame').value=0;$('re073-replay').hidden=!this.runs.length;
  const g=data.goal,m=g.rehearsal073,c=g.counterfactual045;this.forecast=this.runs.find(x=>x.run.id===c.comparison?.forecastRunId)?.run;
  $('re073-status').textContent=g.status+' · '+g.reason;
  $('re073-results').replaceChildren(...[
   'Parent '+m.parentEpisodeId+' → '+(m.childEpisode?.id||'no verified child yet'),
   'Changed '+m.change.sourceId+' '+m.change.property+': '+m.change.before+' → '+m.change.after,
   m.rebuild?'Reconstruction '+(m.rebuild.passed?'PASS':'FAIL')+' · coverage '+m.rebuild.sampleCoverage+' · maximum position residual '+m.rebuild.maxPositionResidualM+'m':'Reconstruction pending or interrupted',
   c.comparison?'Forecast vs actual '+(c.comparison.passed?'PASS':'FAIL')+' · coverage '+c.comparison.sampleCoverage+' · maximum position residual '+c.comparison.maxPositionResidualM+'m':'No verified actual comparison yet',
   'Alternatives: '+c.candidates?.map(x=>x.id+' '+x.status).join(' · '),
   'Retrieval '+m.retrieval.millis.toFixed(1)+'ms · rebuild '+m.rebuildAttempts.map(a=>(a.observedWallMillis??'?')+'ms').join(' + ')+' · alternative ticks '+(m.costs?.totalTicks??'pending')+' · actual ticks '+(data.actual?.final?.tick??'pending'),
   'World '+g.worldBinding.worldId+' · '+(g.worldCommit?'verified revision '+g.worldCommit.revision:'head not advanced')
  ].map(text=>{const p=document.createElement('p');p.textContent=text;return p;}));this.draw();
 }
 draw(){
  const run=this.runs[Number($('re073-runs').value)]?.run;if(!run)return;const frames=run.samples;$('re073-frame').max=frames.length-1;const f=frames[Math.min(frames.length-1,Number($('re073-frame').value))];
  $('re073-frame-label').textContent='Run '+run.id+' · tick '+f.tick+' · '+(f.tick/240).toFixed(3)+'s · source '+run.definition.id;
  $('re073-positions').textContent=JSON.stringify(f.bodies.map(b=>({id:b.id,positionM:b.position,rotation:b.rotation})),null,2);
  const forecast=run.id===this.data?.actual?.id?this.forecast?.samples.find(x=>x.tick===f.tick):null;
  for(const [id,axes,label] of [['re073-top',[0,2],'Top · X / Z · metres'],['re073-front',[0,1],'Front · X / Y · metres']]){
   const canvas=$(id),ctx=canvas.getContext('2d');ctx.clearRect(0,0,600,320);ctx.fillStyle='#cbd5e1';ctx.font='14px sans-serif';ctx.fillText(label,12,22);
   this.boundsCache??=new Map();if(!this.boundsCache.has(run.id)){
    const box=[[Infinity,-Infinity],[Infinity,-Infinity],[Infinity,-Infinity]],include=(p,size)=>{const radius=Math.hypot(...size)/2;for(let k=0;k<3;k++){box[k][0]=Math.min(box[k][0],p[k]-radius);box[k][1]=Math.max(box[k][1],p[k]+radius);}};
    for(const o of run.definition.objects)include(o.position,o.size);
    for(const frame of run.samples)for(const b of frame.bodies)include(b.position,run.definition.objects.find(o=>o.id===b.id).size);
    this.boundsCache.set(run.id,box);
   }
   const bounds=axes.map(k=>this.boundsCache.get(run.id)[k]),scale=Math.min(530/(bounds[0][1]-bounds[0][0]),245/(bounds[1][1]-bounds[1][0]))*.94;
   const mid=bounds.map(b=>(b[0]+b[1])/2),project=p=>[300+(p[axes[0]]-mid[0])*scale,170-(p[axes[1]]-mid[1])*scale];
   ctx.strokeStyle='#273449';ctx.lineWidth=1;for(let n=Math.ceil(bounds[0][0]);n<=Math.floor(bounds[0][1]);n++){const x=300+(n-mid[0])*scale;ctx.beginPath();ctx.moveTo(x,45);ctx.lineTo(x,295);ctx.stroke();ctx.fillText(String(n),x-3,313);}
   const paint=(sample,ghost)=>{for(const o of run.definition.objects){const b=sample.bodies.find(b=>b.id===o.id)||o;const q=b.rotation,pts=[];for(let i=0;i<8;i++){const v=[(i&1?.5:-.5)*o.size[0],(i&2?.5:-.5)*o.size[1],(i&4?.5:-.5)*o.size[2]],u=q.slice(0,3),cross=(a,b)=>[a[1]*b[2]-a[2]*b[1],a[2]*b[0]-a[0]*b[2],a[0]*b[1]-a[1]*b[0]],t=cross(u,v).map(n=>2*n),w=cross(u,t);pts.push(project(v.map((n,k)=>n+q[3]*t[k]+w[k]+b.position[k])));}
     ctx.strokeStyle=ghost?'#fbbf24':o.massKg>0?'#67e8f9':'#64748b';ctx.setLineDash(ghost?[5,4]:[]);ctx.lineWidth=ghost?1:2;for(let i=0;i<8;i++)for(const bit of [1,2,4])if(!(i&bit)){ctx.beginPath();ctx.moveTo(...pts[i]);ctx.lineTo(...pts[i|bit]);ctx.stroke();}if(!ghost&&o.massKg>0){const p=project(b.position);ctx.fillStyle='#e2e8f0';ctx.fillText(o.sourceId,p[0]+5,p[1]-8);}}ctx.setLineDash([]);};
   if(forecast)paint(forecast,true);paint(f,false);canvas.dataset.runId=run.id;canvas.dataset.tick=f.tick;
  }
 }
}
