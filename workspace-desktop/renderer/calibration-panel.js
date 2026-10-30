const el=id=>document.getElementById('calibration-'+id);
const metric=n=>Number(n).toFixed(6);
export class CalibrationPanel {
  constructor({call,scene,requireSaved,notice,reopen,preview}){
    Object.assign(this,{call,scene,requireSaved,notice,reopen,preview});this.job=null;this.busy=false;this.scenes=[];this.datasets=[];
    el('panel').addEventListener('toggle',()=>{if(el('panel').open)this.action(()=>this.refresh());});
    el('parameter').onchange=()=>{const gravity=el('parameter').value==='gravityMps2';el('minimum').value=gravity?'5':'0';el('maximum').value=gravity?'15':'1';};
    el('history').onchange=()=>this.action(async()=>{if(el('history').value)this.show(await this.call('read',{id:el('history').value}));});
    el('start').onclick=()=>this.action(async()=>{
      this.requireSaved();const s=this.scene(),v=this.scenes.find(x=>x.id===el('validation-scene').value),t=this.datasets.find(x=>x.id===el('training').value),d=this.datasets.find(x=>x.id===el('validation').value);
      if(!s||!v||!t||!d)throw Error('Open a training scene and select two imported datasets and a validation scene');
      const args={trainingSceneId:s.id,trainingSceneVersion:s.version,validationSceneId:v.id,validationSceneVersion:v.version,trainingDatasetId:t.id,trainingDatasetVersion:t.version,validationDatasetId:d.id,validationDatasetVersion:d.version,parameter:el('parameter').value,...Object.fromEntries(['minimum','maximum','seconds','toleranceM','minImprovementM'].map(key=>{const raw=el(key).value;if(!raw.trim()||!Number.isFinite(Number(raw)))throw Error('Supply a finite '+key);return[key,Number(raw)];}))};
      const job=await this.call('start',args);this.show(job);await this.refresh(job.id);
    });
    for(const command of ['cancel','resume','apply','rollback','report'])el(command).onclick=()=>this.action(async()=>{
      if(!this.job)throw Error('Select a calibration job');if(['apply','rollback','report'].includes(command))this.requireSaved();
      if(command==='report'){this.preview();await this.call(command,{id:this.job.id});this.notice('Calibration report opened and tested; original datasets and native receipts are retained');}
      else{const result=await this.call(command,{id:this.job.id});this.show(result);if(['apply','rollback'].includes(command)){const id=result.definition.trainingScene.id;if(this.scene()?.id===id)await this.reopen(id);await this.refresh(result.id);}}
    });
    setInterval(()=>{if(el('panel').open&&this.job&&['queued','running'].includes(this.job.status)&&!this.busy)this.action(async()=>this.show(await this.call('read',{id:this.job.id})));},300);
  }
  async action(fn){if(this.busy)return;this.busy=true;this.controls();try{await fn();}catch(e){el('status').textContent=e.message;this.notice(e.message);}finally{this.busy=false;this.controls();}}
  options(id,items,label,selected){const select=el(id);select.replaceChildren();for(const value of items){const option=document.createElement('option');option.value=value.id;option.textContent=label(value);select.append(option);}if(!items.length){const option=document.createElement('option');option.value='';option.textContent='None saved';select.append(option);}else if(items.some(x=>x.id===selected))select.value=selected;}
  async refresh(id=''){
    const data=await this.call('list',{});this.scenes=data.scenes;this.datasets=data.datasets;
    for(const role of ['training','validation'])this.options(role,data.datasets,d=>d.title+' · v'+d.version+' · '+d.sourceKind,el(role).value);
    if(el('training').value===el('validation').value&&data.datasets.length>1)el('validation').selectedIndex=1;
    this.options('validation-scene',data.scenes,s=>s.title+' · v'+s.version,el('validation-scene').value||data.scenes.find(s=>s.id!==this.scene()?.id)?.id);
    this.options('history',data.jobs.toReversed(),j=>j.parameter+' · '+j.status+('accepted'in j?(j.accepted?' · accepted':' · rejected'):'')+' · '+j.id.slice(0,8),id||this.job?.id||el('history').value);
    el('training-scene').textContent=this.scene()?'Training: '+this.scene().title+' · scene v'+this.scene().version:'Open the training scene in 3D Lab';
    if(el('history').value)this.show(await this.call('read',{id:el('history').value}));this.controls();
  }
  controls(){
    const agent=!document.getElementById('stop').disabled,active=this.job&&['queued','running'].includes(this.job.status),locked=this.busy||agent;
    for(const field of el('panel').querySelectorAll('input,select,button'))field.disabled=locked;
    el('start').disabled=locked||active||!this.scene()||this.datasets.length<2;
    el('cancel').disabled=locked||!active;el('resume').disabled=locked||!this.job||!['cancelled','interrupted'].includes(this.job.status);
    el('apply').disabled=locked||!this.job?.canApply;el('rollback').disabled=locked||!this.job?.canRollback;el('report').disabled=locked||this.job?.status!=='completed';
    el('panel').setAttribute('aria-busy',String(this.busy));
  }
  show(job){
    this.job=job;el('history').value=job.id;const d=job.definition,e=job.evaluation;
    const option=[...el('history').options].find(o=>o.value===job.id);if(option)option.textContent=d.parameter+' · '+job.status+(e?(e.accepted?' · accepted':' · rejected'):'')+' · '+job.id.slice(0,8);
    el('progress').max=d.limits.nativeRuns;el('progress').value=job.completedNativeOperations;
    el('status').dataset.status=job.status;el('status').textContent=job.status+' · '+job.phase+' · '+job.completedNativeOperations+'/'+d.limits.nativeRuns+' maximum native operations · attempt '+job.attempts+(job.reason?' · '+job.reason:'');
    el('result').hidden=!e;
    if(e){el('result').textContent=(e.accepted?'Accepted':'Rejected')+' · '+d.parameter+' '+metric(job.selection.value)+' · training RMSE '+metric(job.baselineTraining.rmseM)+' → '+metric(job.fittedTraining.rmseM)+' m · validation RMSE '+metric(job.validationBaseline.rmseM)+' → '+metric(job.validationFitted.rmseM)+' m · max validation residual '+metric(job.validationFitted.maxAbsErrorM)+' / tolerance '+metric(d.toleranceM)+' m. '+(job.application?.status||'Not applied')+'. '+(job.bindingsCurrent?'Bindings current.':'Historical bindings; apply unavailable.')+' Prior validation uses: '+d.validationPriorUseCount+'.';el('result').dataset.accepted=String(e.accepted);}
    el('trials').replaceChildren();for(const trial of job.trials){const row=document.createElement('tr');for(const value of [metric(trial.value),metric(trial.metrics.rmseM),trial.runId]){const cell=document.createElement('td');cell.textContent=value;row.append(cell);}el('trials').append(row);}
    this.controls();
  }
}
