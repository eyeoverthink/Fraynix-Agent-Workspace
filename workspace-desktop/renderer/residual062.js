const $=id=>document.getElementById(id);
const mm=value=>(value*1000).toFixed(3)+' mm';
export class Residual062 {
 constructor(call,notice){
  Object.assign(this,{call,notice});const panel=document.createElement('details');panel.id='residual062';
  panel.innerHTML=`<summary>Physics first · learn the remaining error</summary>
   <p>Keep analytical physics. Learn a small correction, choose the method on separate cases, then test 16 new conditions. Predictions remain advisory.</p>
   <div class="runtime-controls"><button id="r062-train">Learn residuals &amp; compare</button><button id="r062-stop" disabled>Stop experiment</button></div>
   <label>Saved residual experiment <select id="r062-history"></select></label>
   <p id="r062-status" role="status">No residual experiment yet.</p><progress id="r062-progress" max="60" value="0" aria-label="Residual native trials"></progress>
   <div id="r062-result" aria-live="polite"></div>
   <details id="r062-predict-panel"><summary>Compare a new prediction</summary><p>Default ramp · 1 kg · gravity 9.81 m/s² · 0.75 seconds</p>
    <div class="runtime-controls"><label>Angle ° <input id="r062-angle" type="number" min="8" max="32" value="20"></label>
    <label>Friction balance <input id="r062-ratio" type="number" min="0.5" max="1.3" step="0.05" value="1"></label>
    <label>Push m/s <input id="r062-push" type="number" min="0" max="0.4" step="0.05" value="0.2"></label></div>
    <p>Balance is friction ÷ tan(angle): below 1 accelerates; above 1 slows the block.</p>
    <button id="r062-predict" disabled>Compare predictions</button><p id="r062-prediction" role="status"></p>
   </details>
   <details><summary>Original evidence</summary><button id="r062-evidence" disabled>Read retained records</button><pre id="r062-original" style="max-height:260px;overflow:auto;white-space:pre-wrap"></pre></details>`;
  $('brain-state').after(panel);const safe=fn=>async()=>{try{await fn();}catch(e){notice(e.message);$('r062-prediction').textContent=e.message;}};
  $('r062-train').onclick=safe(async()=>{const job=await call('brain','start',{kind:'physics-residuals',autoPromote:false});this.id=job.id;this.cached=null;});
  $('r062-stop').onclick=safe(()=>call('brain','cancel',{id:this.activeId}));
  $('r062-history').onchange=()=>{this.id=$('r062-history').value;this.cached=null;this.shown=null;$('r062-prediction').textContent='';};
  $('r062-predict').onclick=safe(async()=>{const r=await call('brain','residual-predict',{jobId:this.id,definition:{angleDeg:Number($('r062-angle').value),frictionRatio:Number($('r062-ratio').value),pushMps:Number($('r062-push').value),massKg:1,gravityMps2:9.81}});
   $('r062-prediction').textContent=`${r.qualified?'Qualified advisory':'Unqualified candidate'} · ${r.selectedMethod}: ${r.selectedPredictionM.toFixed(5)} m · Analytical: ${r.predictionM.analytical.toFixed(5)} m · correction ${mm(r.selectedPredictionM-r.predictionM.analytical)}. No action executed.`;
  });
  $('r062-evidence').onclick=safe(async()=>{$('r062-original').textContent=JSON.stringify(await call('brain','read',{id:this.id}),null,2);});
 }
 async refresh(state){if(this.refreshing)return;this.refreshing=true;try{
  const summary=state.residual062;if(!summary)return;const jobs=state.jobs.filter(j=>j.kind==='physics-residuals');this.activeId=jobs.find(j=>['running','queued'].includes(j.status))?.id;
  if(!this.id)this.id=summary.lastCompleted?.jobId||summary.jobId;const stamp=JSON.stringify(jobs.map(j=>[j.id,j.status]));
  if(stamp!==this.historyStamp){this.historyStamp=stamp;$('r062-history').replaceChildren(...jobs.map((j,i)=>new Option(`Experiment ${i+1} · ${j.status}`,j.id)));}$('r062-history').value=this.id||'';
  const short=jobs.find(j=>j.id===this.id);let full=null;
  if(short?.status==='completed'){
   if(this.cached?.id!==this.id){const id=this.id,j=await this.call('brain','read',{id});if(this.id!==id)return;this.cached=j;}
   full=this.cached;if(this.shown!==full.id){this.render(full);this.shown=full.id;}
  }else{this.shown=null;$('r062-result').replaceChildren();}
  $('r062-train').disabled=state.busy;$('r062-stop').disabled=!state.busy||!this.activeId;$('r062-predict').disabled=state.busy||!full;$('r062-evidence').disabled=!short;
  $('r062-progress').value=short?.progress||0;$('r062-status').textContent=short?`${short.status} · ${short.progress||0} / 60 · ${short.phase||short.reason||''}`:'No residual experiment yet.';
 }catch(e){this.notice(e.message);}finally{this.refreshing=false;}}
 render(job){const target=$('r062-result');target.replaceChildren();const decision=job.decision062,e=job.evaluation062;target.dataset.qualified=String(e.qualified);target.dataset.selected=decision.method;
  const title=document.createElement('p');title.textContent=`Selected on separate cases: ${decision.method}. ${e.qualified?'Qualified on the new reserved cases.':'The reserved qualification gate did not pass.'} Controllers unchanged.`;target.append(title);
  const table=document.createElement('table');table.style.width='100%';const head=document.createElement('tr');for(const label of ['Method','Selection MAE','Reserved MAE','Reserved worst']){const th=document.createElement('th');th.textContent=label;head.append(th);}table.append(head);
  for(const method of ['analytical','constant','linear','cortex']){const tr=document.createElement('tr');for(const v of [method,mm(decision.selectionMetrics.meanAbsoluteErrorM[method]),mm(e.meanAbsoluteErrorM[method]),mm(e.maxErrorM[method])]){const td=document.createElement('td');td.textContent=v;tr.append(td);}if(method===decision.method)tr.style.color='#8ae3cf';table.append(tr);}target.append(table);
  const p=document.createElement('p');p.textContent=`16 withheld conditions · accelerating error ${mm(e.regimeMeanErrorM[decision.method].accelerating)} · slowing error ${mm(e.regimeMeanErrorM[decision.method].slowing)} · correction limited to ±50 mm. Simulation evidence; no LLM weight update.`;target.append(p);
  const ns='http://www.w3.org/2000/svg',svg=document.createElementNS(ns,'svg');svg.id='r062-chart';svg.setAttribute('viewBox','0 0 820 160');svg.setAttribute('aria-label','Endpoint errors for each reserved case');svg.style.width='100%';
  const errors=job.reserved062.map(r=>({base:Math.abs(r.record.final.distanceM-r.predictionBeforeObservation.analytical),fit:Math.abs(r.record.final.distanceM-r.predictionBeforeObservation[decision.method])}));const max=Math.max(.001,...errors.flatMap(r=>[r.base,r.fit]));
  function rect(x,y,height,color){const e=document.createElementNS(ns,'rect');for(const[k,v]of Object.entries({x,y,width:14,height,fill:color}))e.setAttribute(k,v);svg.append(e);}
  errors.forEach((r,i)=>{rect(15+i*50,130-r.base/max*110,r.base/max*110,'#eabd71');rect(31+i*50,130-r.fit/max*110,r.fit/max*110,'#83d8c5');const t=document.createElementNS(ns,'text');t.setAttribute('x',18+i*50);t.setAttribute('y',152);t.setAttribute('fill','#dcebe7');t.textContent=String(i+1);svg.append(t);});target.append(svg);
  const legend=document.createElement('p');legend.textContent=`Recorded case errors · amber: analytical · teal: selected method · chart maximum ${mm(max)}. Checkpoint ${job.modelSha256062.slice(0,16)}.`;target.append(legend);
 }
}
