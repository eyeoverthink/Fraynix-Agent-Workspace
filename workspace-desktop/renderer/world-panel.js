const $=id=>document.getElementById(id);
export class WorldPanel {
 constructor({call,sceneCall,lab,notice,layout,activate,requireSaved}){
  Object.assign(this,{call,sceneCall,lab,notice,layout,activate,requireSaved});this.pending=false;this.version=0;this.job=null;this.runId=null;
  const safe=f=>async()=>{try{await f();}catch(e){notice(e.message);$('world-status').textContent=e.message;}};
  $('world-open').onclick=safe(async()=>{$('world-dialog').showModal();layout();await this.refresh();});$('world-close').onclick=()=>{$('world-dialog').close();layout();};$('world-dialog').addEventListener('close',layout);
  $('world-train').onclick=safe(async()=>{await call('start',{kind:'world-model-train',autoPromote:true});this.job=null;await this.refresh();});
  $('world-cancel-training').onclick=safe(async()=>{if(this.jobId)await call('cancel',{id:this.jobId});});
  $('world-rollback').onclick=safe(async()=>{await call('world-rollback',{expectedVersion:this.version});await this.refresh();});
  $('world-prepare').onclick=safe(async()=>{
   requireSaved();if(!lab.scene)throw Error('Open and save a default-geometry 3D Lab scene first');const s=lab.scene;
   const prepared=await call('world-prepare',{id:s.id,expectedVersion:s.version,targetM:Number($('world-target').value)});this.runId=prepared.id;
   const preview=await call('world-preview',{runId:prepared.id,expectedTick:prepared.current.tick,observationSha256:prepared.worldProgress.observationSha256});this.preview(preview);$('world-dialog').close();activate('scene');$('scene-settings').open=false;layout();
  });
  $('world-imagine').onclick=safe(async()=>{const s=await sceneCall('snapshot',{});this.preview(await call('world-preview',{runId:s.id,expectedTick:s.current.tick,observationSha256:s.worldProgress.observationSha256}));});
  $('world-run').onclick=safe(async()=>{await sceneCall('control',{action:'resume'});});
  for(const [id,dv]of [['world-kick-forward',.4],['world-kick-back',-.4]])$(id).onclick=safe(async()=>{await call('world-kick',{runId:this.runId,operationId:crypto.randomUUID(),deltaVelocityMps:dv});});
  $('world-stop').onclick=safe(async()=>{await call('world-cancel',{runId:this.runId});});
  lab.worldProgress=value=>this.show(value);setInterval(()=>{if($('world-dialog').open&&!this.pending)this.refresh().catch(e=>notice(e.message));},500);
 }
 async refresh(){if(this.pending)return;this.pending=true;try{
  const s=await this.call('state'),p=s.activeWorldModel;this.version=p.version;
  $('world-model-status').textContent=p.available?`Native model v${p.version} · 5 learned members · 105 coefficients · recorded-data gate ${p.realityAccepted?'accepted':'rejected'}`:'No accepted native model yet';
  $('world-train').disabled=s.busy;$('world-prepare').disabled=s.busy||!p.available;$('world-rollback').disabled=s.busy||p.version<1;$('world-cancel-training').disabled=!s.busy;
  const latest=s.jobs.filter(j=>j.kind==='world-model-train').at(-1);if(!latest)return;this.jobId=latest.id;$('world-status').textContent=`${latest.status} · ${latest.phase} · ${latest.progress}/${latest.total}`;$('world-status').dataset.status=latest.status;
  if(!['queued','running'].includes(latest.status)&&this.job?.id!==latest.id){this.job=await this.call('read',{id:latest.id});this.report(this.job);}
 }finally{this.pending=false;}}
 report(j){const e=j.worldPredictionEvaluation,p=j.worldPlanningEvaluation,r=j.realityEvaluation;if(!e||!r){$('world-summary').textContent=j.reason||'No completed model result';return;}
  $('world-summary').textContent=`Native ${j.accepted?'accepted':'rejected'} · one-step position RMSE ${(1000*e.positionRmseM).toFixed(2)} mm · velocity ${e.velocityRmseMps.toFixed(3)} m/s · 8-step position RMSE ${(100*e.rolloutPositionRmseM).toFixed(2)} cm · empirical joint band coverage ${(100*e.oneStepBandCoverage).toFixed(1)}%`;
  $('world-planning-summary').textContent=`Planning score ${p.meanScore.toFixed(4)} vs fixed feedback ${p.feedback017MeanScore.toFixed(4)} (lower is better) · model actions ≥${(100*p.minModelActionFraction).toFixed(0)}% · worst endpoint ${(p.maxEndpointErrorM*100).toFixed(2)} cm. Native acceptance is separate from real-recording acceptance.`;
  $('world-reality').className=r.accepted?'accepted':'rejected';$('world-reality').textContent=`Recorded reality: ${r.accepted?'accepted':'REJECTED'} · free forecast RMSE ${(r.freeRmseM*100).toFixed(2)} cm · max ${(r.freeMaxErrorM*100).toFixed(2)} cm / tolerance ${r.toleranceM*100} cm. One-step RMSE ${(100*r.oneStepRmseM).toFixed(2)} cm uses prior observed frames; it is a different test.`;
  $('world-source').textContent=`${r.sourceKind}. ${r.trainingRows} training rows; ${r.sourceValidationRows} recording rows, first ${r.conditioningRows} condition the forecast, ${r.forecastRows} are scored. Previously observed regression case. No physical action authority.`;
  $('world-record').textContent=JSON.stringify({id:j.id,source:j.worldProtocol,nativeAccepted:j.accepted,native:{...e,predictions:undefined},planning:p,reality:{...r,rows:undefined}},null,2);this.chart(r.rows);
 }
 chart(rows){const canvas=$('world-reality-chart'),ctx=canvas.getContext('2d');canvas.width=900;canvas.height=280;ctx.fillStyle='#0c191e';ctx.fillRect(0,0,900,280);if(!rows?.length)return;const left=55,top=20,width=820,height=210,maxT=Math.max(...rows.map(r=>r.timeS)),maxY=Math.max(...rows.flatMap(r=>[r.observedM,r.passiveFreeForecastM,r.nativeTransferM]))*1.08;
  ctx.strokeStyle='#37545e';ctx.beginPath();ctx.moveTo(left,top);ctx.lineTo(left,top+height);ctx.lineTo(left+width,top+height);ctx.stroke();ctx.fillStyle='#c1dadd';ctx.font='12px Segoe UI';ctx.fillText('Displacement m',left,12);ctx.fillText('Source time s',730,267);
  for(const [key,color]of [['observedM','#f3c271'],['passiveFreeForecastM','#77dcc0'],['nativeTransferM','#7db8ff']]){ctx.strokeStyle=color;ctx.lineWidth=2;ctx.beginPath();rows.forEach((r,i)=>{const x=left+r.timeS/maxT*width,y=top+height-r[key]/maxY*height;i?ctx.lineTo(x,y):ctx.moveTo(x,y);});ctx.stroke();}
  ctx.fillStyle='#f3c271';ctx.fillText('Recorded observations',60,251);ctx.fillStyle='#77dcc0';ctx.fillText('Passive free forecast',260,251);ctx.fillStyle='#7db8ff';ctx.fillText('Native-model transfer (assumptions)',470,251);
 }
 preview(plan){this.lastPreview=plan;this.lab.forecast(plan);const box=$('world-preview-details');box.replaceChildren();const note=document.createElement('p');note.textContent=`Source tick ${plan.sourceTick} · selected first action ${plan.action.toFixed(2)} m/s · preview has no physical effect.`;box.append(note);const table=document.createElement('table'),head=table.insertRow();for(const name of ['Constant action m/s','Distance at 0.4 s m','Velocity m/s','Supported']){const cell=document.createElement('th');cell.textContent=name;head.append(cell);}for(const row of plan.constantActionAlternatives||[]){const tr=table.insertRow();for(const text of [row.action.toFixed(2),row.supported?row.afterEightSteps.x.toFixed(3):'—',row.supported?row.afterEightSteps.v.toFixed(3):'—',row.supported?'Yes':row.reason])tr.insertCell().textContent=text;}box.append(table);const caption=document.createElement('small');caption.textContent='Rows repeat one constant action. The planner compares 49 two-block sequences and executes only their first action when Run is pressed.';box.append(caption);}
 show(value){const active=value.scope==='world-model';$('world-live').hidden=!active;if(!active){this.lab.forecast(null);return;}this.runId=value.id;const p=value.worldProgress,actual=value.current,plan=p?.lastPlan||value.worldPreview;
  $('world-live-status').dataset.status=value.status;$('world-live-status').textContent=`${value.status} · native model v${value.controller.version} · tick ${actual.tick}/360 · actual distance ${actual.distanceM.toFixed(3)} m · ${p?.plans||0} plans / ${p?.fallbacks||0} feedback fallbacks`+(value.worldEvaluation?` · goal ${value.worldEvaluation.passed?'accepted':'not accepted'} · endpoint ${(100*value.worldEvaluation.endpointErrorM).toFixed(2)} cm`:'');
  const innovation=p?.lastInnovation;$('world-innovation').textContent=innovation?`Prediction check at tick ${innovation.tick}: Δposition ${(innovation.positionResidualM*1000).toFixed(2)} mm · Δvelocity ${innovation.velocityResidualMps.toFixed(3)} m/s · ${innovation.surprise?'outside empirical band; may include external disturbance':'inside empirical band'}`:'Preview is an imagined projected trajectory. No physical action occurs until Run.';
  for(const id of ['world-kick-forward','world-kick-back','world-stop'])$(id).disabled=!['running','paused'].includes(value.status);$('world-run').disabled=value.status!=='paused';$('world-imagine').disabled=value.status!=='paused';const stale=plan?.previewOnly&&plan.observationSha256!==p.observationSha256;if(stale)$('world-innovation').textContent='Preview expired because native state changed. Imagine again; Run always replans from actual state.';this.lab.forecast(stale?null:plan||null);
 }
}
