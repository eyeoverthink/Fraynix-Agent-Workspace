'use strict';
const {measureResponsive}=require('./responsive.cjs');
const sleep=ms=>new Promise(r=>setTimeout(r,ms));
const unsettled=w=>w&&!['completed','cancelled'].includes(w.status);
function requestedWorkflow(goal,procedures){
 if(!/\breuse\b/i.test(goal))return null;const matched=procedures.filter(p=>p.kind==='evidence-workflow-v1'&&new RegExp('\\b'+p.name+'\\b','i').test(goal));const names=[...new Set(matched.map(p=>p.name))];if(names.length!==1)throw Error('Name one retained evidence workflow exactly, or choose it in Skills');const version=String(goal).match(/\bv(?:ersion\s*)?(\d+)\b/i),chosen=matched.filter(p=>!version||p.version===Number(version[1])).sort((a,b)=>b.version-a.version)[0];if(!chosen)throw Error('Requested workflow version is not retained');return {name:chosen.name,version:chosen.version};
}
function workflowSummary(w){return `Evidence workflow ${w.status}: ${w.reason}${w.metrics?` Observation quality: ${w.metrics.qualityPassed?'within declared thresholds':'flags retained'}. ${w.metrics.observationRows} original observations; ${w.metrics.evidenceRows} verified evidence rows; maximum native chart error ${w.metrics.maxPlotError.toFixed(6)} SVG units.`:''}${w.savedSkill?` Saved ${w.savedSkill.name} v${w.savedSkill.version}.`:''} Observations and native simulation remain separate; no LLM weights were trained.`;}
function createWorkflows({api,canvas,openArtifact,emit,refresh,activity,getDataset,getScene,getGoals,onAudit}){
 let latest=null,timer,busy=false,stopped=false;
 async function update(w){latest=w;emit('javaGoal',w);return w;}
 async function audit(effect){
  const a=await api('/api/artifact/read',{id:effect.report.id});if(a.version!==effect.report.version)throw Error('Workflow app version changed');emit('view','preview');await openArtifact(a);const ready=await canvas.test([{selector:'#row-count',expectText:String(effect.rowCount)}]);if(ready.some(c=>!c.passed))throw Error('Evidence app did not initialize its source before independent audit');
  const observed=await canvas.isolated(`(()=>{const rows=[...document.querySelectorAll('#data-table tbody tr')];if(rows.length>1200)throw Error('Audit row budget exceeded');return {rows:rows.map(r=>{const cells=[...r.querySelectorAll('td')];if(cells.length>8)throw Error('Audit column budget exceeded');return cells.map(c=>String(c.textContent).slice(0,500))}),plots:[...document.querySelectorAll('svg polyline[data-series]')].slice(0,8).map(e=>{const s=e.closest('svg'),v=s.viewBox.baseVal,r=e.getBoundingClientRect(),css=getComputedStyle(e);return {series:e.getAttribute('data-series'),visible:r.width>0&&r.height>0&&css.display!=='none'&&css.visibility!=='hidden'&&Number(css.opacity)>0&&css.stroke!=='none'&&Number(css.strokeOpacity)>0&&parseFloat(css.strokeWidth)>0,viewBox:[v.x,v.y,v.width,v.height],points:Array.from({length:Math.min(e.points.numberOfItems,1200)},(_,i)=>{const p=e.points.getItem(i);return [p.x,p.y]})}})}})()`);
  return {id:a.id,version:a.version,...observed,...(effect.responsive?{layout:await measureResponsive(canvas,canvas.view.webContents.getURL(),effect.rowCount)}:{})};
 }
 async function poll(){if(stopped||busy)return;busy=true;try{
  const all=(await api('/api/workflows/list',{})).workflows,w=all.find(unsettled)||all.at(-1);if(w&&(w.id!==latest?.id||w.updatedAt!==latest?.updatedAt||JSON.stringify(w.currentChild)!==JSON.stringify(latest?.currentChild))){await update(w);if(w.status==='completed'){activity('workflow',workflowSummary(w));await refresh();}}
  if(w?.status!=='running'||w.step!=='audit')return;const effect=await api('/api/workflows/claim',{});if(!effect.id)return;let result,error;try{result=await audit(effect);}catch(e){error=e.message;}
  const current=await api('/api/workflows/read',{id:effect.workflowId});if(current.status!=='running')return;const next=await api('/api/workflows/ack',{workflowId:effect.workflowId,id:effect.id,nonce:effect.nonce,...(error?{error}:{result})});await update(next);await onAudit?.(next);activity('workflow-audit',next.audit?.passed?'Every source cell and native plot point verified':next.reason,{workflowId:next.id,audit:next.audit||next.auditHistory?.at(-1)});
 }catch(e){if(!stopped)activity('workflow-error',e.message);}finally{busy=false;}}
 function bindings(args){const source=getDataset();if(!args.datasetId){if(!source)throw Error('Select observation data in Data tasks first');args={...args,datasetId:source.datasetId,datasetVersion:source.version};}return args;}
 async function execute(command,args={},context){let w;
  if(command==='workflow.start'){args=bindings({...args});const scene=getScene();if(scene&&!args.sceneId&&!args.parameters)args={...args,sceneId:scene.id,expectedVersion:scene.version};w=await api('/api/workflows/start',args);}
  else if(command==='workflow.reuse'){w=await api('/api/workflows/reuse',bindings({...args}));}
  else{const id=args.id||context?.ownedWorkflowId||latest?.id;if(!id)throw Error('No workflow selected');if(command==='workflow.cancel'){const current=await api('/api/workflows/read',{id});if(current.currentChild?.id)await getGoals()?.cancelGeneration(current.currentChild.id);w=await api('/api/workflows/cancel',{id});}else if(command==='workflow.resume')w=await api('/api/workflows/resume',{id});else if(command==='workflow.read'){const until=Date.now()+4500;do{w=await api('/api/workflows/read',{id});if(w.status!=='running'||context?.cancelled)break;await sleep(200);}while(Date.now()<until);}else throw Error('Unknown workflow command');}
  if(context)context.ownedWorkflowId=w.id;await update(w);if(command!=='workflow.read')await refresh();return w;
 }
 return {execute,latest:()=>latest,reserved:()=>unsettled(latest),start(){stopped=false;clearInterval(timer);timer=setInterval(poll,350);poll();},stop(){stopped=true;clearInterval(timer);},poll,audit};
}
module.exports={createWorkflows,workflowSummary,requestedWorkflow};
