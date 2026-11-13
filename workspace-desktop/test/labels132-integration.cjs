'use strict';
const {launch}=require('./calibration-integration.cjs');
const fs=require('node:fs/promises'),sync=require('node:fs'),path=require('node:path'),assert=require('node:assert/strict'),cp=require('node:child_process'),crypto=require('node:crypto');
const repo=path.resolve(__dirname,'../..'),out=path.join(repo,'build/labels132-app'),project=path.join(out,'project');
const media=process.env.LABELS132_MEDIA||'S:/Codex-Work/fraynix-workspace-edition131/build/footage131-held';
const sleep=ms=>new Promise(r=>setTimeout(r,ms));
(async()=>{
 Object.assign(process.env,{FRAYNIX_WORKSPACE_PERSISTENT:'0',FRAYNIX_WORKSPACE_JAVA_HEAP_MIB:'768',FRAYNIX_WORKSPACE_JAVA_PROCESSORS:'2'});
 require('../../scripts/Study-Build095.cjs').verify(repo);
 assert(!sync.existsSync(out),'Preserve previous app attempts; use explicit continuation after inspection');await fs.mkdir(project,{recursive:true});
 const annotations=JSON.parse(await fs.readFile(path.join(repo,'docs/publication132/source-labels.json'),'utf8'));
 const record={passed:false,source:cp.execFileSync('git',['rev-parse','HEAD'],{cwd:repo,encoding:'utf8'}).trim(),buildReceipt:JSON.parse(await fs.readFile(path.join(repo,'build/study095-build.json'),'utf8')),startedAt:new Date().toISOString(),checks:[],datasets:[],comparisons:[],captures:[],pageErrors:[],scope:annotations.provenance};
 let app,page;const save=()=>fs.writeFile(path.join(out,'result.json'),JSON.stringify(record,null,2));
 const api=(route,args={})=>app.evaluate((_,x)=>global.workspaceTest.api(x.route,x.args),{route,args});
 const request=(operation,command,args={})=>page.evaluate(x=>window.workspace.request(x),{operation,command,args});
 async function until(fn,seconds=90){const end=Date.now()+seconds*1000;while(Date.now()<end){const r=await fn();if(r)return r;await sleep(100);}throw Error('Bounded app wait expired');}
 async function open(){({app,page}=await launch(project));page.on('pageerror',e=>record.pageErrors.push(e.message));await app.evaluate(({BrowserWindow})=>{const w=BrowserWindow.getAllWindows().find(w=>w.isVisible());w.setSize(1600,1050);w.setTitle('Fraynix | Edition 132 | independent source labels');});await page.waitForFunction(()=>window.labels132View,{},{timeout:60000});}
 async function idle(){await page.waitForFunction(()=>!window.labels132View.busy);}
 async function capture(name,target){await page.locator(target).scrollIntoViewIfNeeded();await sleep(350);await page.screenshot({path:path.join(out,name)});record.captures.push(name);await save();}
 try{
  await open();assert.equal(await page.locator('#observations-method129').inputValue(),'legacy-flow');await page.locator('#spatial-tab').click();await page.locator('#persistent-world-panel').evaluate(e=>{e.open=true;e.querySelector('details').open=true;});
  await until(async()=>await page.locator('#persistent-case option').count()===8);await page.locator('#persistent-case').selectOption('dev035-three-body-chain');await page.locator('#persistent-load').click();record.worldId=await until(()=>page.locator('#persistent-worlds').inputValue());
  const original=await api('/api/world/observe',{worldId:record.worldId});record.nativeBefore=original;
  record.geometry=await request('metrics','geometry',JSON.parse(await fs.readFile(path.join(media,'geometry.json'),'utf8')));
  for(const cohort of annotations.cohorts){
   const raw=await fs.readFile(path.join(media,`held-${cohort.window}.fobs.json`));assert.equal(crypto.createHash('sha256').update(raw).digest('hex'),cohort.packSha256);
   const dataset=await request('observations','import',{pack:JSON.parse(raw)});const source=await request('labels132','source',{datasetId:dataset.id,ordinal:1});assert(!source.tracker&&!source.estimate&&!source.run);
   const input={schema:'observation-labels-132-v1',datasetId:dataset.id,datasetSha256:source.datasetSha256,objectKey:'subject',annotator:annotations.annotator,method:annotations.method,exposure:annotations.exposure,provenance:annotations.provenance,rows:cohort.rows};
   record.datasets.push({window:cohort.window,dataset,packSha256:cohort.packSha256,input});
  }
  await page.locator('#labels132-panel').evaluate(e=>e.open=true);await idle();await page.locator('#expand-canvas').click();
  const first=record.datasets[0];await page.locator('#labels132-dataset').selectOption(first.dataset.id);await page.locator('#labels132-load').click();await idle();
  await page.locator('#labels132-visibility').selectOption('visible');await page.locator('#labels132-contact').selectOption('observed');
  await page.locator('#labels132-canvas').scrollIntoViewIfNeeded();const size=await page.locator('#labels132-canvas').boundingBox();const target=first.input.rows[0].pointImage;
  await page.locator('#labels132-canvas').click({position:{x:target[0]*size.width,y:target[1]*size.height}});await page.locator('#labels132-note').fill('UI point placement on the visible original sole; demonstration only, separate from the fixed study labels.');
  await page.locator('#labels132-keep').click();await idle();await page.locator('#labels132-annotator').fill('Codex source-only UI demonstration');await page.locator('#labels132-method').selectOption('ai-source-only');await page.locator('#labels132-exposure').selectOption('tracker-results-seen');await page.locator('#labels132-provenance').fill('Retrospective public EPFL source image, no current tracker output. One-point UI demonstration, not a study replacement.');
  await capture('source-contact-label.png','#labels132-canvas');await page.locator('#labels132-freeze').click();await idle();record.uiLabel=await page.evaluate(()=>window.labels132View.record);assert(record.uiLabel?.id);assert.equal(record.uiLabel.input.rows.length,1);
  assert(Math.hypot(...record.uiLabel.input.rows[0].pointImage.map((v,i)=>(v-target[i])*[360,288][i]))<1);
  for(const d of record.datasets){
   const file=path.join(out,`labels-${d.window}.json`);await fs.writeFile(file,JSON.stringify(d.input,null,2));await page.locator('#labels132-import').setInputFiles(file);await idle();
   d.labels=await page.evaluate(()=>window.labels132View.record);assert.deepEqual(d.labels.input,d.input);await save();
  }
  assert.equal((await api('/api/observations/state',{worldId:record.worldId})).runs.length,0);
  record.checks.push('Original source view and real canvas contact click work before tracking. All 25 fixed study labels frozen through actual import controls before any new run. Three unobservable contacts retain no coordinates.');
  for(const d of record.datasets)for(const policy of ['legacy-flow','renewing-flow-131']){
   const start=performance.now();await request('observations','select',{worldId:record.worldId,datasetId:d.dataset.id,bindings:[{objectKey:'subject',bodyId:original.frame.bodies[0].id}],trackerPolicy:policy});const started=await request('observations','start');
   const run=await until(async()=>{const r=await api('/api/observations/read',{id:started.id});return r.status==='running'?null:r;});assert.equal(run.status,'completed',run.reason);
   const trackingMs=performance.now()-start;assert.deepEqual(run.trace.map(f=>f.imageSha256),d.dataset.frames.slice(1).map(f=>f.imageSha256));
   const auditStart=performance.now();await page.evaluate(async()=>window.labels132View.refresh());await page.locator('#labels132-saved').selectOption(d.labels.id);await page.locator('#labels132-run').selectOption(run.id);await page.locator('#labels132-geometry').selectOption(record.geometry.id);await page.locator('#labels132-audit').click();await idle();
   const audit=await page.evaluate(()=>window.labels132View.audit);assert.equal(audit.input.runId,run.id);assert.equal(audit.summary.labels,5);const unknown=audit.scores.filter(r=>r.contact!=='observed');assert.equal(await page.locator('#labels132-contact-unknown').textContent(),`Unobservable contact: ${unknown.length} / 5 labels. ${unknown.filter(r=>r.accepted).length} accepted measurements among those frames have no visible contact check.`);
   const metrics=await request('metrics','view',{runId:run.id,geometryId:record.geometry.id});
   // Independently recompute pixel distances from persisted observations and the frozen manual points.
   for(const score of audit.scores){const frame=run.trace.find(f=>f.ordinal===score.ordinal),object=frame.objects[0];assert.equal(score.accepted,object.estimate.accepted);const metric=metrics.rows.find(r=>r.ordinal===score.ordinal);if(score.filteredAnchorErrorPx!==undefined){const expected=Math.hypot((metric.pointImage[0]-score.pointImage[0])*d.dataset.width,(metric.pointImage[1]-score.pointImage[1])*d.dataset.height);assert(Math.abs(expected-score.filteredAnchorErrorPx)<1e-9);}else assert(score.contact!=='observed'||!score.accepted);}
   record.comparisons.push({window:d.window,policy,trackingMs,auditMs:performance.now()-auditStart,run,audit});assert.deepEqual(await api('/api/world/observe',{worldId:record.worldId}),original);await save();
  }
  record.checks.push('Ten real Java tracker runs complete serially on the fixed public pixels. Independent host arithmetic agrees with every available contact error. Each audit retains original frame hashes, source binding and missing labels. Native world unchanged.');
  const selected=record.comparisons.find(c=>c.window===2600&&c.policy==='legacy-flow');await page.locator('#labels132-audits').selectOption(selected.audit.id);await page.locator('#labels132-audit-read').click();await idle();assert.deepEqual(await page.evaluate(()=>window.labels132View.audit),selected.audit);
  await capture('visibility-contact-audit.png','#labels132-results');
  for(const width of [1100,1600]){await app.evaluate(({BrowserWindow},w)=>BrowserWindow.getAllWindows().find(x=>x.isVisible()).setSize(w,1050),width);await page.locator('#labels132-results').scrollIntoViewIfNeeded();assert(await page.evaluate(()=>document.documentElement.scrollWidth<=innerWidth+1));const s=selected.audit.summary;assert.equal(await page.locator('#labels132-summary').textContent(),`Observable contacts: ${s.contactAvailable} / ${s.observableContacts} measured; ${s.contactMissing} missing. Within declared point allowance: ${s.withinLabelRadius} / ${s.observableContacts}. Unsupported measurements while hidden: ${s.unsupportedHiddenMeasurements}.`);await capture(`audit-${width}.png`,'#labels132-results');}
  record.export=await request('labels132','export',{id:record.datasets[2].labels.id});const exported=JSON.parse(await fs.readFile(record.export.path,'utf8'));assert.deepEqual(exported,record.datasets[2].labels);await page.locator('#labels132-import').setInputFiles(record.export.path);await idle();assert.deepEqual(await page.evaluate(()=>window.labels132View.record),exported);
  record.checks.push('Actual comparison values checked at 1100 and 1600 px without horizontal page overflow; export bytes re-import idempotently with the same immutable identity.');
  await app.evaluate(()=>global.workspaceTest.crashEngine());await sleep(200);await app.close().catch(()=>{});app=null;await open();
  for(const d of record.datasets)assert.deepEqual(await api('/api/labels132/read',{id:d.labels.id}),d.labels);
  for(const c of record.comparisons){assert.deepEqual(await api('/api/labels132/audit-read',{id:c.audit.id}),c.audit);assert.deepEqual(await api('/api/observations/read',{id:c.run.id}),c.run);}
  assert.deepEqual(await api('/api/world/observe',{worldId:record.worldId}),original);
  await page.locator('#spatial-tab').click();await page.locator('#labels132-panel').evaluate(e=>e.open=true);await idle();await page.locator('#labels132-audits').selectOption(selected.audit.id);await page.locator('#labels132-audit-read').click();await idle();assert.deepEqual(await page.evaluate(()=>window.labels132View.audit),selected.audit);
  record.checks.push('Abrupt owned-Java termination and cold restart preserve all five frozen label sets, all ten original runs/audits and the untouched native world. Actual saved-comparison control recomputes the same result.');
  assert.deepEqual(record.pageErrors,[]);record.passed=true;console.log(JSON.stringify({passed:true,checks:record.checks,comparisons:record.comparisons.length,captures:record.captures}));
 }catch(e){record.error=e.stack;if(page)await page.screenshot({path:path.join(out,'failure.png')}).catch(()=>{});throw e;}
 finally{record.endedAt=new Date().toISOString();await save();if(app)await app.close();}
})().catch(e=>{console.error(e);process.exitCode=1;});
