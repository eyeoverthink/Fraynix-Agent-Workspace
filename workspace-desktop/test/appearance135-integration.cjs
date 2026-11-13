'use strict';
const {launch}=require('./calibration-integration.cjs');
const fs=require('node:fs/promises'),sync=require('node:fs'),path=require('node:path'),assert=require('node:assert/strict'),cp=require('node:child_process'),crypto=require('node:crypto');
const repo=path.resolve(__dirname,'../..'),out=path.join(repo,'build/appearance135-app'),project=path.join(out,'project');
const sleep=ms=>new Promise(r=>setTimeout(r,ms));
(async()=>{
 Object.assign(process.env,{FRAYNIX_WORKSPACE_PERSISTENT:'0',FRAYNIX_WORKSPACE_JAVA_HEAP_MIB:'768',FRAYNIX_WORKSPACE_JAVA_PROCESSORS:'2'});
 require('../../scripts/Study-Build095.cjs').verify(repo);assert(!sync.existsSync(out),'Preserve previous app attempts');await fs.mkdir(project,{recursive:true});
 const record={passed:false,source:cp.execFileSync('git',['rev-parse','HEAD'],{cwd:repo,encoding:'utf8'}).trim(),buildReceipt:JSON.parse(await fs.readFile(path.join(repo,'build/study095-build.json'),'utf8')),startedAt:new Date().toISOString(),checks:[],runs:[],captures:[],pageErrors:[]};
 let app,page;const save=()=>fs.writeFile(path.join(out,'result.json'),JSON.stringify(record,null,2));
 const api=(route,args={})=>app.evaluate((_,x)=>global.workspaceTest.api(x.route,x.args),{route,args});
 const request=(operation,command,args={})=>page.evaluate(x=>window.workspace.request(x),{operation,command,args});
 async function until(fn,seconds=600){const end=Date.now()+seconds*1000;while(Date.now()<end){const r=await fn();if(r)return r;await sleep(150);}throw Error('Bounded app wait expired');}
 async function open(){({app,page}=await launch(project));page.on('pageerror',e=>record.pageErrors.push(e.message));await app.evaluate(({BrowserWindow})=>{const w=BrowserWindow.getAllWindows().find(w=>w.isVisible());w.setSize(1600,1050);w.setTitle('Fraynix | Edition 135 | continuity and checked appearance evidence');});await page.waitForFunction(()=>window.association134View,{},{timeout:60000});}
 const idle=()=>page.waitForFunction(()=>!window.association134View.busy&&!window.labels132View.busy,{},{timeout:120000});
 async function capture(name,target){await page.locator(target).scrollIntoViewIfNeeded();await sleep(350);await page.screenshot({path:path.join(out,name)});record.captures.push(name);await save();}
 try{
  await open();assert.equal(await page.locator('#observations-method129').inputValue(),'legacy-flow');
  await page.locator('#spatial-tab').click();await page.locator('#persistent-world-panel').evaluate(e=>{e.open=true;e.querySelector('details').open=true;});await until(async()=>await page.locator('#persistent-case option').count()===8);await page.locator('#persistent-case').selectOption('dev035-three-body-chain');await page.locator('#persistent-load').click();record.worldId=await until(()=>page.locator('#persistent-worlds').inputValue());record.nativeBefore=await api('/api/world/observe',{worldId:record.worldId});
  const sourceManifest=JSON.parse(await fs.readFile(path.join(repo,'docs/publication135/source135.json'),'utf8'));
  for(const caseName of ['box-439','david3-57','tiger2-59','bird1-97']){const policies=caseName==='box-439'?['appearance-continuity-135','legacy-flow','continuity-no-bank-135']:['appearance-continuity-135'];
   const spec=sourceManifest.records.find(x=>x.case===caseName);const raw=await fs.readFile(path.join(repo,spec.pack));assert.equal(crypto.createHash('sha256').update(raw).digest('hex'),spec.packSha256);
   const dataset=await request('observations','import',{pack:JSON.parse(raw)}),source=await request('labels132','source',{datasetId:dataset.id,ordinal:1});
   const sourceLabels=JSON.parse(await fs.readFile(path.join(repo,spec.labels),'utf8'));const input={...sourceLabels,datasetId:dataset.id,datasetSha256:source.datasetSha256};
   await page.locator('#association134-panel').evaluate(e=>e.open=true);await idle();if(caseName==='box-439')await page.locator('#expand-canvas').click();
   const file=path.join(out,caseName+'-labels.json');await fs.writeFile(file,JSON.stringify(input,null,2));await page.locator('#association134-import').setInputFiles(file);await idle();const label=await page.evaluate(()=>window.association134View.record);assert.deepEqual(label.input,input);
   for(const policy of policies){
    const expected=JSON.parse(await fs.readFile(path.join(repo,'build/association135-comparison',caseName+'-'+policy+'.json'),'utf8'));
    await page.evaluate(()=>window.labels132View.observation.root.open=true);await page.locator('#observations-method129').selectOption(policy);const start=performance.now();await request('observations','select',{worldId:record.worldId,datasetId:dataset.id,bindings:[{objectKey:'subject',bodyId:record.nativeBefore.frame.bodies[0].id}],trackerPolicy:policy});const started=await request('observations','start');
    if(caseName==='box-439'&&policy==='appearance-continuity-135'){
     const prefix=await until(async()=>{const r=await api('/api/observations/read',{id:started.id});return r.completedFrames>=12?r:null;});assert.equal(prefix.status,'running');
     await api('/api/observations/stop',{id:started.id});const stopped=await api('/api/observations/read',{id:started.id});assert.equal(stopped.status,'cancelled');record.stoppedPrefix=stopped;
     await api('/api/observations/resume',{id:started.id});
    }
    const run=await until(async()=>{const r=await api('/api/observations/read',{id:started.id});return r.status==='running'?null:r;});assert.equal(run.status,'completed',run.reason);const trackingMs=performance.now()-start;
    for(let i=0;i<run.trace.length;i++){const current=run.trace[i].objects[0],prior=expected.trace[i].objects[0];assert.deepEqual(current.estimate,prior.estimate);const strip=o=>Object.fromEntries(Object.entries(o).filter(([k])=>!['id','label'].includes(k)));assert.deepEqual(strip(current.tracker),strip(prior.tracker));}
    await page.locator('#association134-refresh').click();await idle();await page.locator('#association134-labels').selectOption(label.id);await idle();await page.locator('#association134-run').selectOption(run.id);const auditStart=performance.now();await page.locator('#association134-audit').click();await idle();const audit=await page.evaluate(()=>window.association134View.audit);assert.equal(audit.input.runId,run.id);assert.deepEqual(audit.summary,expected.audit.summary);assert.deepEqual(audit.scores,expected.audit.scores);assert.deepEqual(audit.episodes,expected.audit.episodes);assert.deepEqual(audit.appearance135,expected.audit.appearance135);
    const iou=(a,b)=>{const intersection=Math.max(0,Math.min(a[2],b[2])-Math.max(a[0],b[0]))*Math.max(0,Math.min(a[3],b[3])-Math.max(a[1],b[1]));return intersection/((a[2]-a[0])*(a[3]-a[1])+(b[2]-b[0])*(b[3]-b[1])-intersection);};
    for(const row of audit.scores){const overlap=row.accepted&&row.targetBox?iou(row.box,row.targetBox):0;assert(Math.abs(overlap-row.targetOverlap)<1e-9);const other=row.accepted?Math.max(0,...(row.otherBoxes||[]).map(b=>iou(row.box,b))):0;assert.equal(row.confirmedDistractorMatch,row.accepted&&other>=.5&&(!row.targetBox||overlap<.2));}
    for(const a of audit.appearance135.admissions){const row=input.rows.find(x=>x.ordinal===a.ordinal);const overlap=row?.targetBox?iou(a.example.box,row.targetBox):null;assert.equal(a.verdict,row?.visibility==='uncertain'||!row?'unknown':overlap!==null&&overlap>=.5?'correct':'incorrect');if(overlap!==null)assert(Math.abs(overlap-a.targetOverlap)<1e-9);}
    if(caseName==='box-439'&&policy==='appearance-continuity-135'){assert.equal(run.resumes,1);assert.deepEqual(run.trace.slice(0,record.stoppedPrefix.trace.length),record.stoppedPrefix.trace);record.checks.push('Actual stopped Box task resumes the same source/run, preserves its complete original prefix and reproduces every checked appearance and later prediction.');}
    let appearances=null,appearanceExport=null;
    if(policy==='appearance-continuity-135'){
     await page.locator('#appearances135-export').click();await idle();assert.equal(await page.locator('#association134-status').textContent(),'Original source and outcome checks completed.');
     appearances=await api('/api/appearances135/retain',{auditId:audit.id});
     assert.equal(appearances.examples.length,{'box-439':2,'david3-57':6,'tiger2-59':3,'bird1-97':5}[caseName]);
     for(const example of appearances.examples){const row=input.rows.find(x=>x.ordinal===example.ordinal);assert.equal(row.visibility,'visible');assert.deepEqual(example.referenceBox,row.targetBox);assert.equal(example.imageSha256,row.imageSha256);assert.equal(example.descriptor.length,192);assert(example.proposedOverlap>=.5);}
     appearanceExport=await request('appearances135','export',{auditId:audit.id});assert.deepEqual(JSON.parse(await fs.readFile(appearanceExport.path,'utf8')),appearances);
    }
    record.runs.push({caseName,policy,dataset,label,run,audit,appearances,appearanceExport,trackingMs,auditMs:performance.now()-auditStart});assert.deepEqual(await api('/api/world/observe',{worldId:record.worldId}),record.nativeBefore);
    const admission=audit.appearance135.admissions.find(x=>x.verdict==='incorrect')||audit.appearance135.admissions[0];const ordinal=caseName==='bird1-97'?86:admission?.ordinal||30;
    await page.locator('#association134-frame').selectOption(String(ordinal));await idle();await capture(caseName+'-'+policy+'.png','#association134-results');
    if(caseName==='bird1-97'){await page.locator('#association134-frame').selectOption('60');await idle();await capture('rendered-hidden-target.png','#association134-canvas');}
    await save();
   }
  }
  record.checks.push('External OTB boxes plus source-only AI visibility frozen through actual import controls before each tracking task. Labels never enter tracking. Existing legacy-flow default remains selected initially.');
  record.checks.push('Six actual Java runs: four candidate windows plus incumbent and bank-disabled Box controls. All570 tracker/filter samples equal frozen evidence; independent host overlap, admission-correctness and distractor arithmetic match. Native world remains unchanged.');
  for(const width of [1100,1600]){await app.evaluate(({BrowserWindow},w)=>BrowserWindow.getAllWindows().find(x=>x.isVisible()).setSize(w,1050),width);await page.locator('#association134-frame').selectOption('86');await idle();assert(await page.evaluate(()=>document.documentElement.scrollWidth<=innerWidth+1));await capture(`appearance-${width}.png`,'#association134-results');}
  const last=record.runs.at(-1);record.export=await request('association134','audit-export',{id:last.audit.id});assert.deepEqual(JSON.parse(await fs.readFile(record.export.path,'utf8')),last.audit);record.labelExport=await request('association134','export',{id:last.label.id});assert.deepEqual(JSON.parse(await fs.readFile(record.labelExport.path,'utf8')),last.label);await page.locator('#association134-import').setInputFiles(record.labelExport.path);await idle();assert.deepEqual(await page.evaluate(()=>window.association134View.record),last.label);
  record.checks.push('Source overlays, hidden/partial states, appearance provenance and independent admission verdicts displayed at1100/1600 without page overflow. Label and audit exports match their immutable records; exact label reimport preserves identity.');
  await app.evaluate(()=>global.workspaceTest.crashEngine());await sleep(200);await app.close().catch(()=>{});app=null;await open();for(const r of record.runs){assert.deepEqual(await api('/api/association134/audit-read',{id:r.audit.id}),r.audit);assert.deepEqual(await api('/api/observations/read',{id:r.run.id}),r.run);if(r.appearances)assert.deepEqual(await api('/api/appearances135/read',{id:r.appearances.id}),r.appearances);}assert.deepEqual(await api('/api/world/observe',{worldId:record.worldId}),record.nativeBefore);for(const r of record.runs)assert.deepEqual(await api('/api/association134/read',{id:r.label.id}),r.label);
  await page.locator('#spatial-tab').click();await page.locator('#association134-panel').evaluate(e=>e.open=true);await idle();await page.locator('#association134-saved').selectOption(record.runs.at(-1).audit.id);await page.locator('#association134-read').click();await idle();assert.deepEqual(await page.evaluate(()=>window.association134View.audit),record.runs.at(-1).audit);
  record.checks.push('Sixteen fully visible reference-supported descriptors retained/exported from original source pixels; 11 camera and5 rendered. All four sets reverified after restart; incorrect/partial examples excluded, no feedback into measured tracking.');
  record.checks.push('Abrupt owned-Java termination and cold restart retain all six original runs, labels, association proofs and untouched native world. Reopening actual saved comparison recomputes the same result.');assert.deepEqual(record.pageErrors,[]);record.passed=true;console.log(JSON.stringify({passed:true,checks:record.checks,captures:record.captures}));
 }catch(e){record.error=e.stack;if(page)await page.screenshot({path:path.join(out,'failure.png')}).catch(()=>{});throw e;}
 finally{record.endedAt=new Date().toISOString();await save();if(app)await app.close();}
})().catch(e=>{console.error(e);process.exitCode=1;});
