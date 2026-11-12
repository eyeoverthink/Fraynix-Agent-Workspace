'use strict';
const workbenchNames=['browser.navigate','browser.observe','browser.click','browser.type','canvas.write','canvas.read','canvas.observe','canvas.test','artifacts.list','skills.list','skill.save','skill.run','memory.search','finish'];
function createAgency043({api,execute,openArtifact,getArtifact,getSurface,showPreview,emit,activity,sessionId,withAuthority,planTools,captureWorkspace,refresh=async()=>{},isAdmitting=()=>false,toolNames=workbenchNames,withContext=async(_job,fn)=>fn()}){
 let stopped=false,polling=false,executing=false,canceling=false,pendingStarts=0,startGeneration=0,stateGeneration=0,timer,sequence=0,activeId='',lastRunning=false,lastCompleted='',latestState={tasks:[]};
 const start=performance.now();
 async function capture(kind,phase,details={}){
  const c=currentClaim;if(!c)throw Error('Screenshot needs an active Java surface claim');if(kind==='canvas')await showPreview();if(kind==='workspace'){const data=await captureWorkspace();const source={kind,sessionId,sequence:++sequence,elapsedMs:performance.now()-start,url:data.url,title:data.title,viewport:data.viewport,artifactId:getArtifact()?.id||'',artifactVersion:getArtifact()?.version||0,...details};const frame=await api('/api/agency043/frame',{taskId:c.taskId,effectId:c.id,nonce:c.nonce,sessionId,phase,source,png:data.png});return frame.id;}const surface=getSurface(kind),wc=surface.view.webContents;if(wc.isDestroyed()||surface.pending)throw Error('Wait for the actual surface to finish loading before capture');
  const image=await wc.capturePage(),size=image.getSize(),artifact=kind==='canvas'?getArtifact():null;
  const source={kind,sessionId,sequence:++sequence,elapsedMs:performance.now()-start,url:wc.getURL(),title:wc.getTitle().slice(0,240),viewport:[size.width,size.height],artifactId:artifact?.id||'',artifactVersion:artifact?.version||0,...details};
  const frame=await api('/api/agency043/frame',{taskId:c.taskId,effectId:c.id,nonce:c.nonce,sessionId,phase,source,png:image.toPNG().toString('base64')});return frame.id;
 }
 let currentClaim=null,during=[];
 async function onTestStep(check,total){if(!currentClaim)return;if(check.expected?.action||check.index===0||check.index===total-1)during.push(await capture('canvas','during',{testIndex:check.index}));}
 async function runEffect(c){
  if(c.kind==='domain'){
   const context=structuredClone(c.extra.context);let result,error;
   try{await withAuthority({taskId:c.taskId,nonce:c.nonce},async()=>{result=await withContext(context,()=>execute(c.tool,c.args));});}catch(e){error=e.message;}
   try{await api('/api/agency043/ack',{taskId:c.taskId,effectId:c.id,nonce:c.nonce,sessionId,context,result:result&&typeof result==='object'&&!Array.isArray(result)?result:{value:result},...(error?{error}:{})});}catch(e){await api('/api/agency043/abort',{taskId:c.taskId,effectId:c.id,nonce:c.nonce,sessionId,error:e.message}).catch(()=>{});activity('error',e.message);}return;
  }
  currentClaim=c;during=[];const kind=c.extra?.captureFocus||(c.tool.startsWith('browser.')?'browser':'canvas');let beforeId,afterId,result,error;
  try{await withAuthority({taskId:c.taskId,nonce:c.nonce},async()=>{
   if(c.extra?.artifact&&c.kind==='tool'&&c.tool.startsWith('canvas.')&&(getArtifact()?.id!==c.extra.artifact.id||getArtifact()?.version!==c.extra.artifact.version))await openArtifact(await api('/api/artifact/read',{id:c.extra.artifact.id}));
   beforeId=await capture(kind,'before');
   try{if(['open','recipe','reverify','report'].includes(c.kind)){
     const a=await api('/api/artifact/read',{id:c.extra.artifact.id});if(a.version!==c.extra.artifact.version)throw Error('Java-selected artifact version changed');await showPreview();await openArtifact(a);
     result=c.kind==='open'?{id:a.id,version:a.version}:await execute('canvas.test',{steps:c.extra.steps||c.args.steps});
    }else if(c.kind==='browser-check126')result=await require('./browser-outcome126.cjs').observeOutcome126(getSurface('browser'),c);
    else result=await execute(c.tool,c.args);
   }catch(e){error=e.message;}
   afterId=await capture(kind,'after');
  });
  const body={taskId:c.taskId,effectId:c.id,nonce:c.nonce,sessionId,beforeId,afterId,duringIds:during,result:result&&typeof result==='object'&&!Array.isArray(result)?result:{value:result},...(error?{error}: {})};await api('/api/agency043/ack',body);
  }catch(e){await api('/api/agency043/abort',{taskId:c.taskId,effectId:c.id,nonce:c.nonce,sessionId,error:e.message}).catch(()=>{});activity('error',e.message);}
  finally{currentClaim=null;during=[];}
 }
 async function poll(){if(stopped||polling||canceling)return;polling=true;try{
  const generation=stateGeneration,state=await api('/api/agency043/state',{});if(generation!==stateGeneration)return;latestState=state;activeId=latestState.activeId;emit('agency043',latestState);const active=latestState.tasks.find(g=>g.id===activeId);
  if(active){lastRunning=true;emit('run',{active:true,goal:active.goal,model:active.model,phase:active.reason||active.status,turn:active.turn});}
  else if(lastRunning&&!pendingStarts&&!isAdmitting()){lastRunning=false;await refresh();emit('run',{active:false});const g=latestState.tasks.at(-1);if(g&&lastCompleted!==g.id+g.status){lastCompleted=g.id+g.status;emit('answer',g.summary||g.reason);activity(g.status==='completed'?'complete':'stopped',g.summary||g.reason,{javaTaskId:g.id,status:g.status});}}
  if(!executing&&active){const claim=await api('/api/agency043/claim',{sessionId});if(claim.taskId){executing=true;try{await runEffect(claim);}finally{executing=false;}}}
 }catch(e){if(!stopped)activity('error',e.message);}finally{polling=false;}}
 async function begin(args){const generation=startGeneration;stateGeneration++;pendingStarts++;try{const g=await api('/api/agency043/start',args);if(generation!==startGeneration)return await api('/api/agency043/control',{id:g.id,action:'cancel'});stateGeneration++;const prior=latestState.tasks.find(t=>t.id===activeId);latestState={...latestState,tasks:[...latestState.tasks.filter(t=>t.id!==g.id),g],activeId:prior?.id||g.id};activeId=latestState.activeId;lastRunning=true;emit('agency043',latestState);const active=prior||g;emit('run',{active:true,goal:active.goal,model:active.model,phase:active.reason||active.status,turn:active.turn});await poll();return g;}finally{pendingStarts--;}}
 async function cancel(){startGeneration++;stateGeneration++;canceling=true;try{const state=await api('/api/agency043/state',{});for(const g of state.tasks.filter(g=>['queued','ready','planning','waiting-host','waiting-native','vision'].includes(g.status)))await api('/api/agency043/control',{id:g.id,action:'cancel'});}finally{canceling=false;}await poll();}
 async function report(id){const task=await api('/api/agency043/read',{id}),{artifact}=await api('/api/agency043/report',{id});await showPreview();await openArtifact(artifact);const result=await execute('canvas.test',{steps:[{selector:'#visual-task',expectText:id},{selector:'#visual-before',expectText:task.lastPair.beforeSha256},{selector:'#visual-after',expectText:task.lastPair.afterSha256},{selector:'.images img',expectCount:2}]});if(!result.passed)throw Error('Actual visual report checks failed');return{id:artifact.id,version:artifact.version,passed:true};}
 return{async start(sourceSha256){await api('/api/agency043/register',{sourceSha256,tools:planTools(toolNames)});await poll();timer=setInterval(()=>poll(),400);timer.unref();},stop(){stopped=true;clearInterval(timer);},begin,poll,onTestStep,busy:()=>!!activeId||executing||pendingStarts>0||isAdmitting(),active:()=>latestState.tasks.find(g=>g.id===activeId),state:()=>latestState,cancel,report,async action(tool,args={}){if(Object.keys(args).length)throw Error('Visual tools use actual recorded task evidence, not supplied facts');const g=latestState.tasks.findLast(t=>t.lastFrameId);if(!g)throw Error('Choose See workspace to record an actual screen first');if(tool==='screen.read')return api('/api/agency043/read',{id:g.id});if(tool==='screen.report')return report(g.id);throw Error('Unknown visual tool');}};
}
module.exports={createAgency043,workbenchNames};
