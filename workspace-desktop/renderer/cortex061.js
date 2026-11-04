const $=id=>document.getElementById(id);
export class Cortex061 {
 constructor(call,notice){
  Object.assign(this,{call,notice});const panel=document.createElement('details');panel.id='cortex061';
  panel.innerHTML=`<summary>FraynixGPT · learn from world outcomes</summary>
  <p>Compare a Java cortex with simpler predictions using 24 training and 12 withheld native motion trials. Predictions cannot authorize movement.</p>
  <div class="runtime-controls"><button id="c061-train">Learn &amp; compare</button><button id="c061-stop" disabled>Stop experiment</button></div>
  <p id="c061-status" role="status">No outcome experiment yet.</p><progress id="c061-progress" max="36" value="0" aria-label="Verified native trials"></progress>
  <div id="c061-result" aria-live="polite"></div>
  <details id="c061-predict-panel"><summary>Try a prediction · default ramp</summary><p>1 kg block · gravity 9.81 m/s² · 0.75 seconds</p>
  <label>Angle ° <input id="c061-angle" type="number" min="10" max="40" value="25"></label>
  <label>Friction <input id="c061-friction" type="number" min="0" max="0.3" step="0.01" value="0.1"></label>
  <label>Push m/s <input id="c061-push" type="number" min="0" max="1" step="0.05" value="0.5"></label>
  <button id="c061-predict" disabled>Compare predictions</button><p id="c061-prediction" role="status"></p></details>
  <details><summary>Original experiment evidence</summary><button id="c061-evidence" disabled>Read retained evidence</button><pre id="c061-original" style="max-height:260px;overflow:auto;white-space:pre-wrap"></pre></details>`;
  $('brain-state').after(panel);
  const act=f=>async()=>{try{await f();}catch(e){notice(e.message);$('c061-status').textContent=e.message;}};
  $('c061-train').onclick=act(async()=>{const j=await call('brain','start',{kind:'fraynix-outcomes',autoPromote:false});this.id=j.id;});
  $('c061-stop').onclick=act(()=>call('brain','cancel',{id:this.id}));
  $('c061-predict').onclick=act(async()=>{const r=await call('brain','outcome-predict',{jobId:this.id,definition:{angleDeg:Number($('c061-angle').value),friction:Number($('c061-friction').value),pushMps:Number($('c061-push').value),gravityMps2:9.81,massKg:1}});
   $('c061-prediction').textContent=`${r.qualified?'Qualified advisory':'Unqualified candidate'} · Cortex ${r.predictionM.cortex.toFixed(4)} m · Linear ${r.predictionM.linear.toFixed(4)} m · Analytical ${r.predictionM.analytical.toFixed(4)} m. No action executed.`;
  });
  $('c061-evidence').onclick=act(async()=>{$('c061-original').textContent=JSON.stringify(await call('brain','read',{id:this.id}),null,2);});
 }
 refresh(s){const o=s.fraynixGPT061;if(!o)return;if(o.jobId)this.id=o.jobId;
  $('c061-train').disabled=s.busy;$('c061-stop').disabled=!s.busy||!['running','queued'].includes(o.status);
  $('c061-predict').disabled=s.busy||!o.available;$('c061-evidence').disabled=!o.jobId;
  $('c061-status').textContent=o.jobId?`${o.status} · ${o.progress} / 36 native trials · ${o.reason||'Predictions and observations are saved separately.'}`:'No outcome experiment yet.';
  $('c061-progress').value=o.progress||0;
  if(o.modelSha256&&o.modelSha256!==this.shown){this.shown=o.modelSha256;const e=o.evaluation,result=$('c061-result');result.replaceChildren();
   const title=document.createElement('p');title.textContent=e.qualified?'Candidate passed the declared comparison. Advisory use only.':'Candidate did not beat every simpler baseline. Retained for inspection; controllers unchanged.';result.append(title);
   const table=document.createElement('table');table.setAttribute('aria-label','Withheld endpoint error');
   for(const [name,value]of Object.entries(e.meanAbsoluteErrorM)){const tr=document.createElement('tr'),th=document.createElement('th'),td=document.createElement('td');th.textContent=name;td.textContent=`${(value*1000).toFixed(3)} mm mean absolute error`;tr.append(th,td);table.append(tr);}result.append(table);
   const p=document.createElement('p');p.textContent=`12 withheld conditions · worst cortex error ${(e.maxErrorM*1000).toFixed(3)} mm · checkpoint ${o.modelSha256.slice(0,16)}. Simulation evidence; language-model weights unchanged.`;result.append(p);
  }
 }
}
