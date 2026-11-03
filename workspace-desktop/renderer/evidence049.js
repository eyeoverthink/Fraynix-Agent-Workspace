const $=id=>document.getElementById(id);
const labels={
 'measured-crossing-supported':'Beam supports a measured crossing',
 'prediction-near-witness':'Beam near a predicted crossing · position remains predicted',
 'witness-only':'Beam event without a matching camera crossing',
 'ambiguous':'Several crossings or beam transitions overlap · no unique match',
 'shared-clock':'Shared declared clock · independent timing not established',
 'gap-before-block':'Gap before blocked state · transition was not observed'
};
export class EvidenceBench049{
 constructor(call){
  this.call=call;
  const panel=document.createElement('details');panel.id='evidence049-panel';
  panel.innerHTML=`<summary>Evidence bench · replay observations</summary>
   <p>Import a recorded stream to compare camera measurements, predictions and beam events on one timeline.</p>
   <div class="evidence049-fields">
    <label>Observation file · JSON, up to 1 MiB<input id="evidence049-file" type="file" accept=".json,application/json"></label>
    <label>Retained recording<select id="evidence049-choice"><option value="">Choose a recording</option></select></label>
   </div>
   <p id="evidence049-status" role="status">Open to load recordings. No device capture starts here.</p>
   <div class="runtime-controls"><button id="evidence049-play" disabled>Play replay</button><button id="evidence049-export" disabled>Export recording</button></div>
   <label>Replay time<input id="evidence049-seek" type="range" min="0" max="1" step="1" value="0" disabled></label>
   <p id="evidence049-time" aria-live="off"></p>
   <svg id="evidence049-chart" viewBox="0 0 760 240" role="img" aria-label="Camera X positions in metres over session time. Measured samples are circles; predictions are hollow squares; beam transitions are vertical bands."></svg>
   <p class="evidence049-legend">● Measured &nbsp; □ Predicted &nbsp; │ Beam event &nbsp; · Gaps are not joined</p>
   <div id="evidence049-current"></div><div id="evidence049-results"></div>`;
  $('skill-memory-panel').after(panel);
  panel.ontoggle=()=>{if(panel.open)this.run(()=>this.refresh());else this.pause();};
  $('brain-dialog').addEventListener('close',()=>this.pause());
  document.addEventListener('visibilitychange',()=>{if(document.hidden)this.pause();});
  $('evidence049-file').onchange=()=>this.run(async()=>{
   this.pause();const file=$('evidence049-file').files[0];if(!file)return;
   if(file.size>1048576)throw Error('Choose a JSON recording up to 1 MiB.');
   const result=await this.call('evidence049','import',{text:await file.text()});
   await this.refresh(result.id);this.show(result);$('evidence049-file').value='';
  });
  $('evidence049-choice').onchange=()=>this.run(async()=>{
   this.clear();const id=$('evidence049-choice').value;if(!id)return;
   this.show(await this.call('evidence049','read',{id}));
  });
  $('evidence049-export').onclick=()=>this.run(async()=>{
   const result=await this.call('evidence049','save-export',{id:this.data.id});
   $('evidence049-status').textContent=`Recording exported to ${result.path}`;
  });
  $('evidence049-seek').oninput=()=>{this.pause();this.render(Number($('evidence049-seek').value));};
  $('evidence049-play').onclick=()=>{
   if(this.playing){this.pause();return;}
   const s=$('evidence049-seek'),start=Number(s.value)>=this.data.endMs?this.data.startMs:Number(s.value),began=performance.now();
   this.playing=true;$('evidence049-play').textContent='Pause replay';
   let painted=-Infinity;
   const tick=()=>{if(!this.playing)return;const now=performance.now(),t=Math.min(this.data.endMs,start+now-began);
    if(now-painted>=100||t>=this.data.endMs){this.render(t);painted=now;}
    if(t>=this.data.endMs)this.pause();else this.raf=requestAnimationFrame(tick);};tick();
  };
 }
 async run(work){
  if(this.busy)return;this.busy=true;
  $('evidence049-file').disabled=$('evidence049-choice').disabled=$('evidence049-export').disabled=true;
  try{await work();}catch(e){this.clear();$('evidence049-status').textContent=e.message;}
  finally{this.busy=false;$('evidence049-file').disabled=$('evidence049-choice').disabled=false;$('evidence049-export').disabled=!this.data;}
 }
 pause(){this.playing=false;cancelAnimationFrame(this.raf);$('evidence049-play').textContent='Play replay';}
 clear(){this.pause();this.data=null;for(const key of ['chart','current','results','time'])$('evidence049-'+key).replaceChildren();for(const key of ['seek','play','export'])$('evidence049-'+key).disabled=true;}
 async refresh(select){
  const result=await this.call('evidence049','list',{}),choice=$('evidence049-choice'),previous=select||choice.value;
  choice.replaceChildren(new Option('Choose a recording',''),...result.recordings.map(r=>{
   const o=new Option(`${r.title}${r.available?'':' · needs attention'}`,r.id);o.disabled=!r.available;return o;
  }));
  if([...choice.options].some(o=>o.value===previous&&!o.disabled))choice.value=previous;
  $('evidence049-status').textContent=result.recordings.length?`${result.recordings.length} retained recording(s).`:'No recordings yet. Import an observation JSON file.';
 }
 show(data){
  this.pause();this.data=data;
  $('evidence049-status').textContent=`${data.provenance==='synthetic'?'LABELLED SYNTHETIC RECORDING':'IMPORTED RECORDING · source claims not independently verified'} · ${data.title} · ${data.id.slice(0,12)}`;
  const s=$('evidence049-seek');s.min=data.startMs;s.max=data.endMs;s.value=data.startMs;s.disabled=false;
  $('evidence049-play').disabled=data.endMs<=data.startMs;$('evidence049-export').disabled=false;
  const result=$('evidence049-results');result.replaceChildren();
  const heading=document.createElement('h4');heading.textContent='Whole-recording analysis';result.append(heading);
  const p=document.createElement('p'),c=data.counts;
  p.textContent=`${c.measured} measured · ${c.predicted} predicted · ${c.missing} missing · ${c.droppedSequenceNumbers} skipped sequence numbers · ${data.gaps.length} source gaps. ${c.reacquisitionTransitions} return${c.reacquisitionTransitions===1?'':'s'} to measured state; identity is not independently verified.`;result.append(p);
  if(c.initialBlockedBaselines){const baseline=document.createElement('p');baseline.textContent=`${c.initialBlockedBaselines} sensor(s) started blocked. These are baselines, without observed crossing times.`;result.append(baseline);}
  const list=document.createElement('ul');
  for(const a of data.associations){const li=document.createElement('li');li.textContent=`${labels[a.status]} · ${a.beamId} · ${(a.fromMs/1000).toFixed(3)}–${(a.toMs/1000).toFixed(3)} s`;list.append(li);}result.append(list);
  for(const d of data.displacement){const p=document.createElement('p');p.textContent=`${d.cameraId} measured endpoint displacement ${d.distanceM.toFixed(4)} m; supplied bound ±${d.declaredUncertaintyM.toFixed(4)} m.`;result.append(p);}
  const timing=document.createElement('details'),summary=document.createElement('summary');summary.textContent='Source clocks and gaps';timing.append(summary);
  for(const clock of data.clocks){const p=document.createElement('p');p.textContent=`${clock.sourceId} · offset ${clock.offsetMs.toFixed(3)} ms · rate ${clock.ratePpm.toFixed(3)} ppm · declared timing bound ±${clock.uncertaintyMs.toFixed(3)} ms`;timing.append(p);}
  for(const gap of data.gaps){const p=document.createElement('p');p.textContent=`${gap.sourceId} gap ${(gap.fromMs/1000).toFixed(3)}–${(gap.toMs/1000).toFixed(3)} s · ${gap.missingSequenceCount} skipped sequence numbers`;timing.append(p);}
  result.append(timing);
  const note=document.createElement('p');note.textContent=data.scope;result.append(note);this.render(data.startMs);
 }
 render(time){
  const d=this.data;if(!d)return;$('evidence049-seek').value=time;
  $('evidence049-time').textContent=`${(time/1000).toFixed(3)} / ${(d.endMs/1000).toFixed(3)} s · recorded replay`;
  const chart=$('evidence049-chart');chart.replaceChildren();
  const ns='http://www.w3.org/2000/svg',add=(tag,attrs,text)=>{const e=document.createElementNS(ns,tag);for(const[k,v]of Object.entries(attrs))e.setAttribute(k,String(v));if(text)e.textContent=text;chart.append(e);return e;};
  const points=d.events.filter(e=>e.kind==='camera'&&e.state!=='missing');
  let lo=Math.min(0,...points.map(e=>e.xM-e.uncertaintyM)),hi=Math.max(.01,...points.map(e=>e.xM+e.uncertaintyM));
  const span=hi-lo||.01;lo-=span*.1;hi+=span*.1;
  const x=t=>65+(t-d.startMs)/Math.max(1,d.endMs-d.startMs)*670,y=v=>205-(v-lo)/(hi-lo)*180;
  add('line',{x1:65,y1:205,x2:735,y2:205,stroke:'#678888'});
  for(let i=0;i<=4;i++){const v=lo+(hi-lo)*i/4;add('line',{x1:65,y1:y(v),x2:735,y2:y(v),stroke:'#2c454b'});add('text',{x:2,y:y(v)+4,fill:'#c9dfe0','font-size':12},v.toFixed(3)+' m');}
  for(const a of d.associations){if(a.toMs>time)continue;add('rect',{x:x(Math.max(d.startMs,a.fromMs)),y:22,width:Math.max(2,x(a.toMs)-x(Math.max(d.startMs,a.fromMs))),height:183,fill:'#c697fb',opacity:.25});}
  for(const e of points){if(e.sessionMs>time)continue;const cx=x(e.sessionMs),cy=y(e.xM),color=e.state==='measured'?'#79e0bb':'#ffc879';
   add('line',{x1:cx,y1:y(e.xM-e.uncertaintyM),x2:cx,y2:y(e.xM+e.uncertaintyM),stroke:color});
   if(e.state==='measured')add('circle',{cx,cy,r:4,fill:color});else add('rect',{x:cx-4,y:cy-4,width:8,height:8,fill:'none',stroke:color,'stroke-width':2});
  }
  add('line',{x1:x(time),y1:20,x2:x(time),y2:210,stroke:'#fff','stroke-dasharray':'4 3'});
  add('text',{x:65,y:232,fill:'#c9dfe0','font-size':12},(d.startMs/1000).toFixed(2)+' s');
  add('text',{x:680,y:232,fill:'#c9dfe0','font-size':12},(d.endMs/1000).toFixed(2)+' s');
  const latest=new Map();for(const e of d.events)if(e.sessionMs<=time)latest.set(e.sourceId,e);
  $('evidence049-current').textContent=[...latest.values()].map(e=>`${e.sourceId}: last ${e.state} at ${(e.sessionMs/1000).toFixed(3)} s${e.xM!==undefined?`, X ${e.xM.toFixed(4)} m`:''} · age ${Math.round(time-e.sessionMs)} ms`).join(' | ');
 }
}
