'use strict';
// Follow-up durability check on the now-exposed fixed source, outside the fresh comparison.
const {launch}=require('../../workspace-desktop/test/calibration-integration.cjs');
const fs=require('node:fs/promises'),path=require('node:path'),assert=require('node:assert/strict');
(async()=>{
 const repo=process.cwd(),study=JSON.parse(await fs.readFile('build/trajectory145-app/result.json','utf8'));assert(study.passed);
 const source=study.runs.find(r=>r.caseName==='trajectory-return'&&r.policy==='trajectory-guard-145');assert(source);
 const out=path.join(repo,'build/trajectory145-restart');await fs.mkdir(out);const project=path.join(repo,'build/trajectory145-app/project');
 Object.assign(process.env,{FRAYNIX_WORKSPACE_PERSISTENT:'0',FRAYNIX_WORKSPACE_JAVA_HEAP_MIB:'768',FRAYNIX_WORKSPACE_JAVA_PROCESSORS:'2'});
 const receipt=require('../../scripts/Study-Build095.cjs').verify(repo);let app;const result={passed:false,buildReceipt:receipt,harnessSha256:require('node:crypto').createHash('sha256').update(await fs.readFile(__filename)).digest('hex'),scope:'Additional durability regression using exposed145 trajectory-return pixels; not another independent fresh sequence.',startedAt:new Date().toISOString()};
 const api=(route,args={})=>app.evaluate((_,x)=>global.workspaceTest.api(x.route,x.args),{route,args});
 async function open(){({app}=await launch(project));}
 async function until(fn){const end=Date.now()+900000;while(Date.now()<end){const r=await fn();if(r)return r;await new Promise(r=>setTimeout(r,250));}throw Error('Bounded recovery wait expired');}
 try{
  await open();const state=await api('/api/observations/state',{worldId:study.worldId});
  const started=await api('/api/observations/start',{...source.run.nativeBinding,expectedObservationRevision:state.estimate.revision,datasetId:source.dataset.id,bindings:source.run.bindings,trackerPolicy:'trajectory-guard-145',appearanceSet137:source.run.referenceMemory137.setId,referenceDeclaration137:source.run.referenceMemory137.declaration});
  result.beforeCrash=await until(async()=>{const r=await api('/api/observations/read',{id:started.id});if(r.status!=='running')throw Error('Run settled before interruption: '+r.status);return r.completedFrames>=8&&r.trace.some(f=>f.objects[0].tracker.ambiguity144.latched)?r:null;});
  await app.evaluate(()=>global.workspaceTest.crashEngine());await app.close().catch(()=>{});app=null;await open();
  result.recovered=await api('/api/observations/read',{id:started.id});assert.equal(result.recovered.status,'interrupted');assert(result.recovered.completedFrames<result.recovered.totalFrames);assert.deepEqual(result.recovered.trace.slice(0,result.beforeCrash.trace.length),result.beforeCrash.trace);assert(result.recovered.trace.some(f=>f.objects[0].tracker.ambiguity144.latched));
  await api('/api/observations/resume',{id:started.id});result.completed=await until(async()=>{const r=await api('/api/observations/read',{id:started.id});return r.status==='running'?null:r;});assert.equal(result.completed.status,'completed',result.completed.reason);assert.equal(result.completed.resumes,1);assert.deepEqual(result.completed.trace,source.run.trace);
  result.audit=await api('/api/association134/audit',{labelId:source.label.id,runId:started.id});assert.deepEqual(result.audit.summary,source.audit.summary);assert.deepEqual(result.audit.trajectory145,source.audit.trajectory145);assert.deepEqual(await api('/api/world/observe',{worldId:study.worldId}),study.nativeBefore);
  result.checks=['Killed only the owned Java process while ambiguity was latched and the run was unfinished','Cold reopen retained the exact committed prefix and interrupted status','Resume reconstructed the ambiguity state from original pixels before continuing','Complete trace and independent outcome audit exactly match the uninterrupted run','Native world unchanged; Additional observation run uses the existing capacity without expansion'];result.passed=true;
 }catch(e){result.error=e.stack;throw e;}finally{result.endedAt=new Date().toISOString();await fs.writeFile(path.join(out,'result.json'),JSON.stringify(result,null,2)+'\n');if(app)await app.close().catch(()=>{});}
 console.log(JSON.stringify({passed:result.passed,checks:result.checks}));
})().catch(e=>{console.error(e);process.exitCode=1;});
