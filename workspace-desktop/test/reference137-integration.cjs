'use strict';
const {launch}=require('./calibration-integration.cjs');
const fs=require('node:fs/promises'),sync=require('node:fs'),path=require('node:path'),assert=require('node:assert/strict'),cp=require('node:child_process'),crypto=require('node:crypto'),zlib=require('node:zlib');
const repo=path.resolve(__dirname,'../..'),out=path.join(repo,'build/reference137-app'),project=path.join(out,'project');
const sleep=ms=>new Promise(r=>setTimeout(r,ms)),sha=b=>crypto.createHash('sha256').update(b).digest('hex');
const overlap=(a,b)=>{const i=Math.max(0,Math.min(a[2],b[2])-Math.max(a[0],b[0]))*Math.max(0,Math.min(a[3],b[3])-Math.max(a[1],b[1]));return i/((a[2]-a[0])*(a[3]-a[1])+(b[2]-b[0])*(b[3]-b[1])-i);};
(async()=>{
 Object.assign(process.env,{FRAYNIX_WORKSPACE_PERSISTENT:'0',FRAYNIX_WORKSPACE_JAVA_HEAP_MIB:'768',FRAYNIX_WORKSPACE_JAVA_PROCESSORS:'2'});
 require('../../scripts/Study-Build095.cjs').verify(repo);assert(!sync.existsSync(out),'Preserve previous app attempts');await fs.mkdir(project,{recursive:true});
 const record={passed:false,source:cp.execFileSync('git',['rev-parse','HEAD'],{cwd:repo,encoding:'utf8'}).trim(),buildReceipt:JSON.parse(await fs.readFile(path.join(repo,'build/study095-build.json'),'utf8')),startedAt:new Date().toISOString(),checks:[],runs:[],captures:[],pageErrors:[]};
 const manifest=JSON.parse(await fs.readFile(path.join(repo,'docs/publication137/source137.json'),'utf8'));
 const lock=JSON.parse(await fs.readFile(path.join(repo,'docs/OBSERVATION_137_LOCK.json'),'utf8'));for(const [name,hash]of Object.entries(lock.sourceHashes))assert.equal(sha((await fs.readFile(path.join(repo,name),'utf8')).replaceAll('\r\n','\n')),hash);
 let app,page;const save=()=>fs.writeFile(path.join(out,'result.json'),JSON.stringify(record,null,2));
 const api=(route,args={})=>app.evaluate((_,x)=>global.workspaceTest.api(x.route,x.args),{route,args});
 const request=(operation,command,args={})=>page.evaluate(x=>window.workspace.request(x),{operation,command,args});
 async function until(fn,seconds=900){const end=Date.now()+seconds*1000;while(Date.now()<end){const r=await fn();if(r)return r;await sleep(300);}throw Error('Bounded app wait expired');}
 async function open(){({app,page}=await launch(project));page.on('pageerror',e=>record.pageErrors.push(e.message));await app.evaluate(({BrowserWindow})=>{const w=BrowserWindow.getAllWindows().find(w=>w.isVisible());w.setSize(1600,1050);w.setTitle('Fraynix | Edition 137 | verified references and durable evidence');});await page.waitForFunction(()=>window.association134View,{},{timeout:60000});}
 async function capture(name,target){await page.locator(target).scrollIntoViewIfNeeded();await sleep(300);await page.screenshot({path:path.join(out,name)});record.captures.push(name);await save();}
 const idle=()=>page.waitForFunction(()=>!window.association134View.busy&&!window.labels132View.observation.busy,{},{timeout:120000});
 async function load(spec){
  const pixels=await fs.readFile(spec.pack);assert.equal(sha(pixels),spec.packSha256);const labels=await fs.readFile(path.join(repo,spec.labels));assert.equal(sha(labels),spec.labelsSha256);
  const dataset=await request('observations','import',{pack:JSON.parse(pixels)}),source=await api('/api/association134/source',{datasetId:dataset.id,ordinal:1});
  const input={...JSON.parse(labels),datasetId:dataset.id,datasetSha256:source.datasetSha256};const file=path.join(out,spec.case+'-labels.json');await fs.writeFile(file,JSON.stringify(input,null,2));
  await page.locator('#association134-panel').evaluate(e=>e.open=true);await idle();await page.locator('#association134-import').setInputFiles(file);await idle();const label=await page.evaluate(()=>window.association134View.record);assert.deepEqual(label.input,input);return {spec,dataset,label,input};
 }
 async function track(f,policy,set){
  await page.evaluate(async id=>{const v=window.labels132View.observation;v.root.open=true;v.datasetId=id;v.bindingKey='';await v.refresh();},f.dataset.id);await idle();
  await page.locator('#observations-method129').selectOption(policy);
  if(set){await page.locator('#observations-references137').evaluate(e=>e.open=true);await page.locator('#observations-references-refresh137').click();await idle();await page.locator('#observations-reference-set137').selectOption(set.id);await page.locator('#observations-reference-declaration137').fill('Same labelled '+f.spec.case.split('-')[0]+' target in a later non-overlapping window; operator declaration, not semantic identity proof.');}
  await page.locator('#observations-select').click();await idle();assert(await page.evaluate(()=>window.labels132View.observation.selected));
  const start=performance.now();await page.locator('#observations-start').click();await idle();const id=await until(()=>page.evaluate(()=>window.labels132View.observation.runId));
  if(f.spec.case==='box-535'&&policy==='verified-memory-137'){
   const prefix=await until(async()=>{const r=await api('/api/observations/read',{id});return r.completedFrames>=12?r:null;});assert.equal(prefix.status,'running');await api('/api/observations/stop',{id});record.stoppedPrefix=await api('/api/observations/read',{id});await api('/api/observations/resume',{id});
  }
  const run=await until(async()=>{const r=await api('/api/observations/read',{id});return r.status==='running'?null:r;});assert.equal(run.status,'completed',run.reason);
  const trackingMs=performance.now()-start,auditStart=performance.now();const audit=await api('/api/association134/audit',{labelId:f.label.id,runId:id});
  for(const row of audit.scores){const actual=row.accepted&&row.targetBox?overlap(row.box,row.targetBox):0;assert(Math.abs(actual-row.targetOverlap)<1e-9);}
  if(set){assert.equal(run.referenceMemory137.setId,set.id);assert.equal(audit.reference137.automaticAdmissions,0);assert.equal(audit.reference137.samples,95);for(const frame of run.trace){const t=frame.objects[0].tracker.reference137;assert.equal(t.automaticAdmissions,0);assert.equal(t.importedEnabled,policy==='verified-memory-137');}}
  if(f.spec.case==='box-535'&&policy==='verified-memory-137'){assert.equal(run.resumes,1);assert.deepEqual(run.trace.slice(0,record.stoppedPrefix.trace.length),record.stoppedPrefix.trace);}
  let retained=null;if(policy==='appearance-continuity-135'){retained=await api('/api/appearances135/retain',{auditId:audit.id});assert(retained.examples.length);for(const e of retained.examples){const row=f.input.rows.find(r=>r.ordinal===e.ordinal);assert.equal(row.visibility,'visible');assert.deepEqual(row.targetBox,e.referenceBox);assert(e.proposedOverlap>=.5);}}
  const result={caseName:f.spec.case,role:f.spec.role,policy,dataset:f.dataset,label:f.label,run,audit,retained,trackingMs,auditMs:performance.now()-auditStart};record.runs.push(result);assert.deepEqual(await api('/api/world/observe',{worldId:record.worldId}),record.nativeBefore);await save();console.log(JSON.stringify({case:f.spec.case,policy,summary:audit.summary,retained:retained?.examples.length,reference:audit.reference137,trackingMs}));return result;
 }
 try{
  await open();assert.equal(await page.locator('#observations-method129').inputValue(),'legacy-flow');await page.locator('#spatial-tab').click();await page.locator('#persistent-world-panel').evaluate(e=>{e.open=true;e.querySelector('details').open=true;});await until(async()=>await page.locator('#persistent-case option').count()===8);await page.locator('#persistent-case').selectOption('dev035-three-body-chain');await page.locator('#persistent-load').click();record.worldId=await until(()=>page.locator('#persistent-worlds').inputValue());record.nativeBefore=await api('/api/world/observe',{worldId:record.worldId});await page.locator('#expand-canvas').click();
  const donors={};for(const spec of manifest.records.filter(r=>r.role==='reference-source')){const f=await load(spec);const result=await track(f,'appearance-continuity-135');donors[spec.case.split('-')[0]]=result;}
  record.checks.push('Four current-code source runs independently audited; only fully visible correct proposals curated from original reference-box pixels. All source labels and original bytes fixed before evaluation.');
  for(const spec of manifest.records.filter(r=>r.role==='evaluation')){
   const f=await load(spec),donor=donors[spec.case.split('-')[0]];
   for(const policy of ['legacy-flow','seed-only-137','verified-memory-137']){
    const result=await track(f,policy,policy==='legacy-flow'?null:donor.retained);
    if(policy==='verified-memory-137'){
     await page.locator('#association134-panel').evaluate(e=>e.open=true);await page.locator('#association134-refresh').click();await idle();await page.locator('#association134-saved').selectOption(result.audit.id);await page.locator('#association134-read').click();await idle();assert.deepEqual(await page.evaluate(()=>window.association134View.audit),result.audit);await page.locator('#association134-frame').selectOption('30');await idle();await capture(spec.case+'-verified.png','#association134-results');
     if(spec.case==='box-535'){await capture('references137.png','#observations-references137');record.export=await request('observations','export',{id:result.run.id});const envelope=JSON.parse(await fs.readFile(record.export.path,'utf8')),raw=zlib.gunzipSync(Buffer.from(envelope.data,'base64'));assert.equal(sha(raw),envelope.rawSha256);const exported=JSON.parse(raw);assert(exported.referenceBundles137.some(b=>b.set.id===donor.retained.id));assert(exported.relatedDatasets.some(d=>d.dataset.id===donor.dataset.id));}
    }
   }
  }
  record.checks.push('Twelve separated evaluation runs (three methods × four windows), 1140 original frame outcomes independently scored. No automatic admissions; candidate/control share the same frozen source references. Default unchanged.');
  record.checks.push('Actual reference-selection controls, same-target declaration, stopped prefix/resume, complete reference bundle export including donor pixels, and four independent overlays exercised. Native world unchanged.');
  for(const width of [1100,1600]){await app.evaluate(({BrowserWindow},w)=>BrowserWindow.getAllWindows().find(x=>x.isVisible()).setSize(w,1050),width);assert(await page.evaluate(()=>document.documentElement.scrollWidth<=innerWidth+1));await capture('reference-layout-'+width+'.png','#observations-references137');}
  await page.locator('#observations-cost136').click();await idle();await capture('evidence-cost137.png','#observations-cost136-result');record.evidenceCost=await api('/api/observations/cost136');
  const stateFile=path.join(project,'world-observations/state.json'),before=await fs.readFile(stateFile),time=(await fs.stat(stateFile)).mtimeMs;
  await app.evaluate(()=>global.workspaceTest.crashEngine());await app.close().catch(()=>{});app=null;await open();assert.deepEqual(await fs.readFile(stateFile),before);assert.equal((await fs.stat(stateFile)).mtimeMs,time);
  for(const r of record.runs){assert.deepEqual(await api('/api/association134/audit-read',{id:r.audit.id}),r.audit);assert.deepEqual(await api('/api/observations/read',{id:r.run.id}),r.run);if(r.retained)assert.deepEqual(await api('/api/appearances135/read',{id:r.retained.id}),r.retained);}
  assert.equal((await api('/api/observations/cost136')).durableWrites,0);assert.deepEqual(await api('/api/world/observe',{worldId:record.worldId}),record.nativeBefore);
  const target=record.runs.find(r=>r.caseName==='box-535'&&r.policy==='verified-memory-137'),donor=donors.box,pixel=path.join(project,'world-observations/datasets',donor.dataset.id,'12.image');const original=await fs.readFile(pixel),bad=Buffer.from(original);bad[20]^=1;await fs.writeFile(pixel,bad);
  try{await assert.rejects(()=>api('/api/association134/audit-read',{id:target.audit.id}),/changed|integrity|checksum/i);await assert.rejects(()=>api('/api/observations/export',{id:target.run.id}),/changed|integrity|checksum/i);}finally{await fs.writeFile(pixel,original);}
  assert.deepEqual(await api('/api/association134/audit-read',{id:target.audit.id}),target.audit);
  record.checks.push('Owned-process crash and cold reopen preserve exact completed-state bytes and timestamp with zero observation writes. All16 runs, labels, source examples and outcomes reverified; changed donor pixels refuse audit/export, restored originals reverify.');
  assert.deepEqual(record.pageErrors,[]);record.passed=true;console.log(JSON.stringify({passed:true,checks:record.checks}));
 }catch(e){record.error=e.stack;if(page)await page.screenshot({path:path.join(out,'failure.png')}).catch(()=>{});throw e;}
 finally{record.endedAt=new Date().toISOString();await save();if(app)await app.close();}
})().catch(e=>{console.error(e);process.exitCode=1;});
