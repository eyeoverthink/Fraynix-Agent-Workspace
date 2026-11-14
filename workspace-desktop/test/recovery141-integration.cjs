'use strict';
const {launch}=require('./calibration-integration.cjs');
const fs=require('node:fs/promises'),sync=require('node:fs'),path=require('node:path'),assert=require('node:assert/strict'),cp=require('node:child_process'),crypto=require('node:crypto'),zlib=require('node:zlib');
const repo=path.resolve(__dirname,'../..'),out=path.join(repo,'build/recovery141-app'),project=path.join(out,'project'),sha=b=>crypto.createHash('sha256').update(b).digest('hex');
const overlap=(a,b)=>{const i=Math.max(0,Math.min(a[2],b[2])-Math.max(a[0],b[0]))*Math.max(0,Math.min(a[3],b[3])-Math.max(a[1],b[1]));return i/((a[2]-a[0])*(a[3]-a[1])+(b[2]-b[0])*(b[3]-b[1])-i);};
(async()=>{
 Object.assign(process.env,{FRAYNIX_WORKSPACE_PERSISTENT:'0',FRAYNIX_WORKSPACE_JAVA_HEAP_MIB:'768',FRAYNIX_WORKSPACE_JAVA_PROCESSORS:'2'});
 const resume=process.env.DRIFT141_RESUME==='1';require('../../scripts/Study-Build095.cjs').verify(repo);assert.equal(sync.existsSync(out),resume,'Explicit continuation required; preserve all attempts');await fs.mkdir(project,{recursive:true});
 const record=resume?JSON.parse(await fs.readFile(path.join(out,'result.json'),'utf8')):{passed:false,source:cp.execFileSync('git',['rev-parse','HEAD'],{cwd:repo,encoding:'utf8'}).trim(),buildReceipt:JSON.parse(await fs.readFile(path.join(repo,'build/study095-build.json'),'utf8')),startedAt:new Date().toISOString(),runs:[],checks:[],captures:[],pageErrors:[]};
 if(resume){await fs.copyFile(path.join(out,'result.json'),path.join(out,'result-attempt-'+Date.now()+'.json'));record.attempts??=[];record.attempts.push({error:record.error,endedAt:record.endedAt});delete record.error;record.passed=false;}
 const manifest=JSON.parse(await fs.readFile(path.join(repo,'docs/publication141/source141.json'),'utf8')),lockBytes=await fs.readFile(path.join(repo,'docs/OBSERVATION_141_LOCK.json')),lock=JSON.parse(lockBytes);
 assert.equal(sha(lockBytes),manifest.algorithmLockSha256);for(const [name,hash] of Object.entries(lock.sourceHashes))assert.equal(sha((await fs.readFile(path.join(repo,name),'utf8')).replaceAll('\r\n','\n')),hash);
 let app,page;const save=()=>fs.writeFile(path.join(out,'result.json'),JSON.stringify(record,null,2));
 const api=(route,args={})=>app.evaluate((_,x)=>global.workspaceTest.api(x.route,x.args),{route,args});
 const request=(operation,command,args={})=>page.evaluate(x=>window.workspace.request(x),{operation,command,args});
 async function until(fn,seconds=900){const end=Date.now()+seconds*1000;while(Date.now()<end){const r=await fn();if(r)return r;await new Promise(r=>setTimeout(r,300));}throw Error('Bounded app wait expired');}
 async function open(){({app,page}=await launch(project));page.on('pageerror',e=>record.pageErrors.push(e.message));await app.evaluate(({BrowserWindow})=>{const w=BrowserWindow.getAllWindows().find(w=>w.isVisible());w.setSize(1600,1050);w.setTitle('Fraynix | Edition 141 | recovery after loss and independent evidence');});await page.waitForFunction(()=>window.association134View,{},{timeout:60000});}
 const idle=()=>page.waitForFunction(()=>!window.association134View.busy&&!window.labels132View.observation.busy&&!window.labels132View.busy,{},{timeout:180000});
 async function capture(name,target){await page.locator(target).scrollIntoViewIfNeeded();await page.waitForTimeout(250);await page.screenshot({path:path.join(out,name)});if(!record.captures.includes(name))record.captures.push(name);await save();}
 async function load(spec){
  const old=record.runs.find(x=>x.caseName===spec.case);if(old)return{spec,dataset:old.dataset,label:old.label};
  const pixels=await fs.readFile(path.join(repo,spec.pack)),labels=await fs.readFile(path.join(repo,spec.labels));assert.equal(sha(pixels),spec.packSha256);assert.equal(sha(labels),spec.labelsSha256);
  const dataset=await request('observations','import',{pack:JSON.parse(pixels)}),source=await api('/api/labels132/source',{datasetId:dataset.id,ordinal:1});
  const input={...JSON.parse(labels),datasetId:dataset.id,datasetSha256:source.datasetSha256},file=path.join(out,spec.case+'-labels.json');await fs.writeFile(file,JSON.stringify(input,null,2));
  await page.locator('#association134-panel').evaluate(e=>e.open=true);await idle();await page.locator('#association134-import').setInputFiles(file);await idle();const label=await page.evaluate(()=>window.association134View.record);assert.deepEqual(label.input,input);return{spec,dataset,label};
 }
 function independent(run,audit,label){
  const extra={returned:0,correct:0,incorrect:0,unknown:0},filtered={accepted:0,correct:0,incorrect:0,unknown:0};let preserved=0;
  for(const row of audit.scores){
   const object=run.trace[row.ordinal-1].objects[0],t=object.tracker;
   const actual=row.accepted&&row.targetBox?overlap(row.box,row.targetBox):0;assert(Math.abs(actual-row.targetOverlap)<1e-9);
   const other=row.accepted?Math.max(0,...(row.otherBoxes||[]).map(b=>overlap(row.box,b))):0;assert.equal(row.confirmedDistractorMatch,row.accepted&&other>=.5&&(!row.targetBox||actual<.2));
   const m=t.recovery141;if(!m)continue;assert.equal(m.automaticAdmissions,0);if(m.baselineAccepted)preserved++;
   if(!m.recovery.accepted)continue;
   const seen=['visible','partial'].includes(row.visibility),lost=['occluded','out-of-view'].includes(row.visibility);
   const outcome=seen&&overlap(t.box,row.targetBox)>=.5?'correct':seen||lost?'incorrect':'unknown';extra.returned++;extra[outcome]++;
   if(object.estimate.accepted){filtered.accepted++;filtered[outcome]++;}
  }
  if(audit.recovery141){const d=audit.recovery141;assert.deepEqual(d.additional,extra);assert.deepEqual(d.filtered,filtered);assert.equal(d.baselineAcceptedPreserved,preserved);assert.equal(d.replayedFrames,run.trace.length);}
 }
 async function track(f,policy){
  const old=record.runs.find(x=>x.caseName===f.spec.case&&x.policy===policy);if(old){assert.deepEqual(await api('/api/observations/read',{id:old.run.id}),old.run);assert.deepEqual(await api('/api/association134/audit-read',{id:old.audit.id}),old.audit);return old;}
  await page.waitForFunction(()=>!window.labels132View.observation.refreshing);await page.evaluate(async id=>{const v=window.labels132View.observation;v.root.open=true;v.datasetId=id;v.bindingKey='';await v.refresh();},f.dataset.id);await idle();await page.locator('#observations-method129').selectOption(policy);await page.locator('#observations-select').click();await idle();assert(await page.evaluate(()=>window.labels132View.observation.selected));
  const start=performance.now();await page.locator('#observations-start').click();await idle();const id=await until(()=>page.evaluate(()=>window.labels132View.observation.runId));
  if(f.spec.case===manifest.records[0].case&&policy==='recovery-guard-141'){
   const prefix=await until(async()=>{const r=await api('/api/observations/read',{id});return r.completedFrames>=10?r:null;});assert.equal(prefix.status,'running');await api('/api/observations/stop',{id});record.stoppedPrefix=await api('/api/observations/read',{id});await api('/api/observations/resume',{id});
  }
  const run=await until(async()=>{const r=await api('/api/observations/read',{id});return r.status==='running'?null:r;});assert.equal(run.status,'completed',run.reason);const trackingMs=performance.now()-start,auditStart=performance.now();
  const audit=await api('/api/association134/audit',{labelId:f.label.id,runId:id});independent(run,audit,f.label);
  if(policy==='recovery-monitor-141'){
   const base=record.runs.find(x=>x.caseName===f.spec.case&&x.policy==='foreground-guard-139');const trace=structuredClone(run.trace);for(const frame of trace)delete frame.objects[0].tracker.recovery141;assert.deepEqual(trace,base.run.trace);
  }
  if(policy==='recovery-guard-141'){
   const base=record.runs.find(x=>x.caseName===f.spec.case&&x.policy==='foreground-guard-139');let preserved=0;
   for(let i=0;i<run.trace.length;i++){const b=base.run.trace[i].objects[0].tracker;if(!b.box)continue;const t=structuredClone(run.trace[i].objects[0].tracker);delete t.recovery141;assert.deepEqual(t,b);preserved++;}
   assert.equal(audit.recovery141.baselineAcceptedPreserved,preserved);
  }
  if(f.spec.case===manifest.records[0].case&&policy==='recovery-guard-141'){assert.equal(run.resumes,1);assert.deepEqual(run.trace.slice(0,record.stoppedPrefix.trace.length),record.stoppedPrefix.trace);}
  const result={caseName:f.spec.case,policy,dataset:f.dataset,label:f.label,run,audit,trackingMs,auditMs:performance.now()-auditStart};record.runs.push(result);assert.deepEqual(await api('/api/world/observe',{worldId:record.worldId}),record.nativeBefore);await save();console.log(JSON.stringify({case:f.spec.case,policy,summary:audit.summary,drift:audit.recovery141?.additional,trackingMs}));return result;
 }
 try{
  await open();assert.equal(await page.locator('#observations-method129').inputValue(),'legacy-flow');await page.locator('#spatial-tab').click();await page.locator('#persistent-world-panel').evaluate(e=>{e.open=true;e.querySelector('details').open=true;});await until(async()=>await page.locator('#persistent-case option').count()===8);
  if(!record.worldId){await page.locator('#persistent-case').selectOption('dev035-three-body-chain');await page.locator('#persistent-load').click();record.worldId=await until(()=>page.locator('#persistent-worlds').inputValue());record.nativeBefore=await api('/api/world/observe',{worldId:record.worldId});}else{await page.locator('#persistent-worlds').selectOption(record.worldId);await until(()=>page.evaluate(id=>window.labels132View.observation.persistent.id===id,record.worldId));}await page.locator('#expand-canvas').click();
  for(const spec of manifest.records){const f=await load(spec);for(const policy of lock.methods){const result=await track(f,policy);if(policy==='recovery-guard-141'){
   await page.locator('#association134-panel').evaluate(e=>e.open=true);await page.locator('#association134-refresh').click();await idle();await page.locator('#association134-saved').selectOption(result.audit.id);await page.locator('#association134-read').click();await idle();assert.deepEqual(await page.evaluate(()=>window.association134View.audit),result.audit);
   const first=result.run.trace.find(t=>t.objects[0].tracker.recovery141.recovery.accepted||t.objects[0].tracker.recovery141.lossObserved);const ordinal=first?.ordinal||Math.min(20,result.run.trace.length);await page.locator('#association134-frame').selectOption(String(ordinal));await idle();await capture(spec.case+'-outcome.png','#association134-results');await capture(spec.case+'-source.png','#association134-canvas');
   await page.evaluate(async ({id,ordinal})=>{const v=window.labels132View.observation;v.runId=id;v.follow=false;await v.refresh();document.querySelector('#observations-frame-slider').value=String(ordinal);await v.preview();},{id:result.run.id,ordinal});await capture(spec.case+'-drift.png','#observations-frame');
   if(spec.case===manifest.records[0].case){
    const exported=await request('observations','export',{id:result.run.id}),envelope=JSON.parse(await fs.readFile(exported.path,'utf8')),raw=zlib.gunzipSync(Buffer.from(envelope.data,'base64'));assert.equal(sha(raw),envelope.rawSha256);const full=JSON.parse(raw);assert.equal(full.images.length,spec.frames);record.export={bytes:(await fs.stat(exported.path)).size,rawBytes:raw.length,sha256:sha(raw),images:full.images.length};
   }
  }}}
  assert.equal(record.runs.length,manifest.records.length*lock.methods.length);
  record.checks.push('All eligible fixed source windows compared with four methods and identical pixels. Independent host overlap/distractor/supplemental-outcome arithmetic agrees with Java. Monitor preserves every139 tracker/filter output; candidate preserves every accepted139 tracker proposal. No automatic anchor admissions; defaults unchanged.');
  record.checks.push('Explicit method selection, recovery explanation, original-source overlay, independent audit UI, complete bounded export and exact stopped-prefix resume passed. Native physical world unchanged.');
  for(const width of [1100,1600]){await app.evaluate(({BrowserWindow},w)=>BrowserWindow.getAllWindows().find(x=>x.isVisible()).setSize(w,1050),width);assert(await page.evaluate(()=>document.documentElement.scrollWidth<=innerWidth+1));await capture('drift-layout-'+width+'.png','#observations-panel');}
  const state=path.join(project,'world-observations/state.json'),before=await fs.readFile(state),time=(await fs.stat(state)).mtimeMs;await app.evaluate(()=>global.workspaceTest.crashEngine());await app.close().catch(()=>{});app=null;await open();assert.deepEqual(await fs.readFile(state),before);assert.equal((await fs.stat(state)).mtimeMs,time);
  for(const r of record.runs){assert.deepEqual(await api('/api/observations/read',{id:r.run.id}),r.run);assert.deepEqual(await api('/api/association134/audit-read',{id:r.audit.id}),r.audit);}
  assert.equal((await api('/api/observations/cost136')).durableWrites,0);assert.deepEqual(await api('/api/world/observe',{worldId:record.worldId}),record.nativeBefore);
  const target=record.runs.find(r=>r.policy==='recovery-guard-141'),file=path.join(project,'world-observations/datasets',target.dataset.id,'12.image'),original=await fs.readFile(file),bad=Buffer.from(original);bad[20]^=1;await fs.writeFile(file,bad);
  try{await assert.rejects(()=>api('/api/association134/audit-read',{id:target.audit.id}),/changed|integrity|checksum/i);await assert.rejects(()=>api('/api/observations/export',{id:target.run.id}),/changed|integrity|checksum/i);}finally{await fs.writeFile(file,original);}
  assert.deepEqual(await api('/api/association134/audit-read',{id:target.audit.id}),target.audit);record.checks.push('Owned-process crash and cold reopen preserve exact completed-state bytes/timestamp and zero observation writes. All recorded runs/audits reverified, including original-pixel decision replay. Source modification refuses audit/export; original restoration revalidates.');assert.deepEqual(record.pageErrors,[]);record.passed=true;
 }catch(e){record.error=e.stack;if(page)await page.screenshot({path:path.join(out,'failure.png')}).catch(()=>{});throw e;}
 finally{record.endedAt=new Date().toISOString();await save();if(app)await app.close();}
})().catch(e=>{console.error(e);process.exitCode=1;});
