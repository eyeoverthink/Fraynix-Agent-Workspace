'use strict';
const {launch}=require('../../workspace-desktop/test/calibration-integration.cjs');
const fs=require('node:fs/promises'),path=require('node:path'),assert=require('node:assert/strict'),crypto=require('node:crypto'),cp=require('node:child_process');
const repo=process.cwd(),group=process.env.STUDY155_GROUP||'generated',attempt=process.env.STUDY155_ATTEMPT||'';
assert(['generated','exposed150'].includes(group));assert(/^(|-[a-z0-9-]+)$/.test(attempt));
const out=path.join(repo,'build/study155-'+group+attempt),project=path.join(out,'project'),sha=b=>crypto.createHash('sha256').update(b).digest('hex');
async function inventory(root){const rows={};async function walk(dir){for(const item of await fs.readdir(dir,{withFileTypes:true})){const file=path.join(dir,item.name);if(item.isSymbolicLink())throw Error('Linked source refused');if(item.isDirectory())await walk(file);else rows[path.relative(root,file).replaceAll('\\','/')]=sha(await fs.readFile(file));}}await walk(root);return rows;}
(async()=>{
 require('../../scripts/Study-Build095.cjs').verify(repo);const lock=JSON.parse(await fs.readFile('docs/OBSERVATION_155_LOCK.json','utf8'));
 for(const [name,hash]of Object.entries(lock.sourceHashes))assert.equal(sha((await fs.readFile(name,'utf8')).replaceAll('\r\n','\n')),hash,name);
 await fs.mkdir(out);let old,originalSource,sourceInventory;
 if(group==='exposed150'){originalSource='J:/Codex-Work/fraynix-workspace-edition150/build/reference150-webcam150-displaced-return-app';sourceInventory=await inventory(path.join(originalSource,'project'));await fs.cp(path.join(originalSource,'project'),project,{recursive:true,errorOnExist:true,force:false});old=JSON.parse(await fs.readFile(path.join(originalSource,'result.json'),'utf8'));}else await fs.mkdir(project);
 Object.assign(process.env,{FRAYNIX_WORKSPACE_PERSISTENT:'0',FRAYNIX_WORKSPACE_JAVA_HEAP_MIB:'768',FRAYNIX_WORKSPACE_JAVA_PROCESSORS:'2'});
 const result={passed:false,group,source:cp.execFileSync('git',['rev-parse','HEAD'],{encoding:'utf8'}).trim(),methodLockSha256:sha(await fs.readFile('docs/OBSERVATION_155_LOCK.json')),startedAt:new Date().toISOString(),cases:[],checks:[],captures:[],errors:[]};let app,page;
 const save=()=>fs.writeFile(path.join(out,'result.json'),JSON.stringify(result,null,2));const api=(route,args={})=>app.evaluate((_,x)=>global.workspaceTest.api(x.route,x.args),{route,args});
 async function open(){({app,page}=await launch(project));page.on('pageerror',e=>result.errors.push(e.message));await app.evaluate(({BrowserWindow})=>{const w=BrowserWindow.getAllWindows().find(x=>x.isVisible());w.setSize(1480,1000);w.setTitle('Fraynix | Edition 155 | Rotation search evidence');});await page.waitForFunction(()=>window.association134View,{},{timeout:60000});}
 const idle=()=>page.waitForFunction(()=>!window.association134View.busy,{},{timeout:900000});
 async function until(fn){for(let i=0;i<3000;i++){const v=await fn();if(v)return v;await new Promise(r=>setTimeout(r,200));}throw Error('Bounded test wait expired');}
 try{
  await open();await page.locator('#spatial-tab').click();assert.equal(await page.locator('#observations-method129').inputValue(),'legacy-flow');
  if(group==='exposed150'){
   result.worldId=old.worldId;result.nativeBefore=old.nativeBefore;const row=old.runs.find(x=>x.role==='evaluation'&&x.policy==='identity-return-149');result.cases.push({name:'exposed150-camera',auditId:row.audit.id,baseline:row.audit.summary});
  }else{
   await page.locator('#persistent-world-panel').evaluate(e=>{e.open=true;e.querySelector('details').open=true;});await until(async()=>await page.locator('#persistent-case option').count()===8);await page.locator('#persistent-case').selectOption('dev035-three-body-chain');await page.locator('#persistent-load').click();result.worldId=await until(()=>page.locator('#persistent-worlds').inputValue());result.nativeBefore=await api('/api/world/observe',{worldId:result.worldId});
   const specs=JSON.parse(await fs.readFile('build/reserved155/manifest.json','utf8')).cases;
   // Import and label ALL reserved sources before any baseline or experimental tracking.
   for(const spec of specs){const bytes=await fs.readFile(spec.pack),labelBytes=await fs.readFile(spec.labels);assert.equal(sha(bytes),spec.packSha256);assert.equal(sha(labelBytes),spec.labelsSha256);
    const dataset=await page.evaluate(x=>window.workspace.request({operation:'observations',command:'import',args:{pack:x}}),JSON.parse(bytes));
    const source=await api('/api/labels132/source',{datasetId:dataset.id,ordinal:1});const labels=await api('/api/association134/save',{...JSON.parse(labelBytes),datasetId:dataset.id,datasetSha256:source.datasetSha256});
    result.cases.push({name:spec.name,datasetId:dataset.id,labelId:labels.id,packSha256:spec.packSha256,labelsSha256:spec.labelsSha256});await save();
   }
   for(const row of result.cases){const native=await api('/api/world/observe',{worldId:result.worldId}),state=await api('/api/observations/state',{worldId:result.worldId});
    const run=await api('/api/observations/start',{worldId:result.worldId,expectedRevision:native.expectedRevision,expectedHash:native.expectedHash,expectedObservationRevision:state.estimate.revision,datasetId:row.datasetId,bindings:[{objectKey:'subject',bodyId:native.frame.bodies[0].id}],trackerPolicy:'legacy-flow'});
    const completed=await until(async()=>{const r=await api('/api/observations/read',{id:run.id});return r.status==='running'?null:r;});assert.equal(completed.status,'completed');
    const audit=await api('/api/association134/audit',{labelId:row.labelId,runId:run.id});row.auditId=audit.id;row.baseline=audit.summary;await save();
   }
  }
  const stateFile=path.join(project,'world-observations/state.json'),stateBefore=await fs.readFile(stateFile),snapshots={};
  await app.close();app=null;for(const name of ['world-observations','association-labels134'])snapshots[name]=await inventory(path.join(project,name));await open();await page.locator('#spatial-tab').click();
  for(const row of result.cases){
   await page.locator('#association134-panel').evaluate(e=>e.open=true);await idle();await page.locator('#association134-refresh').click();await idle();await page.locator('#association134-saved').selectOption(row.auditId);
   await page.locator('#continuity155-panel').evaluate(e=>e.open=true);await page.locator('#continuity155-run').click();await idle();const found=await page.evaluate(()=>window.association134View.continuity155);assert(found&&found.experiment.auditId===row.auditId,await page.locator('#association134-status').textContent());row.result=found;await save();

   const e=found.experiment;assert.equal(e.automaticAdmissions,0);assert.equal(e.physicalActionAuthorized,false);
   const s=e.summary;assert.equal(s.correct,e.scores.filter(x=>['visible','partial'].includes(x.visibility)&&x.accepted&&x.targetOverlap>=.5).length);assert.equal(s.unsupported,e.scores.filter(x=>['occluded','out-of-view'].includes(x.visibility)&&x.accepted).length);
   for(const combined of e.trace){const t=combined.rotation;if(t.chosen){assert(t.chosen.strong&&t.chosen.unique&&t.chosen.confirmations>=4&&!t.ambiguityRetained);for(let j=t.ordinal-4;j<t.ordinal;j++){const h=e.trace[j].rotation.hypotheses.find(x=>x.sourceKey===t.chosen.sourceKey);assert(h.eligible);if(j>t.ordinal-4)assert(h.coherentMotion);}}if(combined.origin==='original-continuity'){assert(combined.originalAccepted&&combined.originalStatus==='tracked'&&combined.originalSegmentOpen&&combined.continuityFlowPassed&&!t.ambiguityRetained);assert(combined.supportKey);}else if(combined.origin==='confirmed-return-153')assert(t.chosen);else assert.equal(combined.box,null);}
   // The new path runs unchanged153 on every frame; report it as a nested comparator,
   // not as another independently executed application run.
   row.warmTimings=[];for(let repeat=0;repeat<3;repeat++){await page.locator('#continuity155-read').click();await idle();const warm=await page.evaluate(()=>window.association134View.continuity155);assert(warm.cost155.reusedReplay);assert.deepEqual(warm.experiment,e);row.warmTimings.push(warm.totalMs);}
   await save();
   await page.evaluate(()=>{for(const e of document.querySelectorAll('canvas,img,video'))e.style.setProperty('display','none','important');});await page.locator('#continuity155-panel').scrollIntoViewIfNeeded();await page.screenshot({path:path.join(out,row.name+'.png')});result.captures.push({name:row.name+'.png',sha256:sha(await fs.readFile(path.join(out,row.name+'.png'))),privatePixelsExcluded:true});await save();
   console.log(JSON.stringify({case:row.name,summary:s,totalMs:found.totalMs}));
  }
  // Saved verification recomputes the experimental trace. An actual app restart
  // also reruns original verification; only this study-owned process is closed.
  const first=result.cases[0],last=result.cases.at(-1);await page.locator('#continuity155-force').click();await idle();const forced=await page.evaluate(()=>window.association134View.continuity155);assert(!forced.cost155.reusedReplay);assert.deepEqual(forced.experiment,last.result.experiment);result.forced={case:last.name,totalMs:forced.totalMs,cost:forced.cost155};
  await app.close();app=null;await open();const reopenJob=await api('/api/association134/continuity155-start',{id:first.result.experiment.id,verify:true});const reopenedStatus=await until(async()=>{const s=await api('/api/association134/continuity155-status',{jobId:reopenJob.jobId});return s.status==='running'?null:s;});assert.equal(reopenedStatus.status,'completed',reopenedStatus.error);const reopened=reopenedStatus.result;assert.deepEqual(reopened.experiment,first.result.experiment);result.restartMs=reopened.totalMs;result.restartCost=reopened.cost155;assert(!reopened.cost155.reusedReplay);
  const target=path.join(project,'world-observations/datasets',first.result.experiment.datasetId,'2.image'),original=await fs.readFile(target),stat=await fs.stat(target),changed=Buffer.from(original);changed[20]^=1;await fs.writeFile(target,changed);await fs.utimes(target,stat.atime,stat.mtime);await assert.rejects(api('/api/association134/continuity155-read',{id:first.result.experiment.id}),/changed|integrity/i);await fs.writeFile(target,original);await fs.utimes(target,stat.atime,stat.mtime);
  assert.deepEqual(await fs.readFile(stateFile),stateBefore);assert.deepEqual(await api('/api/world/observe',{worldId:result.worldId}),result.nativeBefore);await app.close();app=null;
  for(const [name,expected]of Object.entries(snapshots))assert.deepEqual(await inventory(path.join(project,name)),expected);if(originalSource)assert.deepEqual(await inventory(path.join(originalSource,'project')),sourceInventory);
  assert.deepEqual(result.errors,[]);result.checks.push('Actual compare, saved verification and force replay controls executed; three warm readings per case','Original continuity support and unchanged four-frame return confirmations independently checked','Actual app restart exactly reproduced saved trace','Same-size and same-time source corruption refused; original bytes restored','Original evidence namespaces and native world unchanged','No renderer errors');result.passed=true;
 }catch(e){result.error=e.stack;throw e;}finally{if(app)await app.close();result.endedAt=new Date().toISOString();await save();}
 console.log(JSON.stringify({passed:result.passed,group,cases:result.cases.length,restartMs:result.restartMs}));
})().catch(e=>{console.error(e);process.exitCode=1;});
