'use strict';
// Original evidence is copied into a new private project; exposed150 data is diagnostic, never held out.
const {launch}=require('../../workspace-desktop/test/calibration-integration.cjs');
const fs=require('node:fs/promises'),path=require('node:path'),assert=require('node:assert/strict'),crypto=require('node:crypto'),cp=require('node:child_process');
const repo=process.cwd(),prior=process.env.PRIOR151_ROOT;
const group=process.env.STUDY151_GROUP||'webcam150-displaced-return';assert(['webcam150-displaced-return','generated150'].includes(group));assert(prior);
const out=path.join(repo,'build/study151-'+group),project=path.join(out,'project'),sha=b=>crypto.createHash('sha256').update(b).digest('hex');
async function inventory(root){const result={};async function walk(folder){for(const item of await fs.readdir(folder,{withFileTypes:true})){const file=path.join(folder,item.name);if(item.isSymbolicLink())throw Error('Linked source refused');if(item.isDirectory())await walk(file);else result[path.relative(root,file).replaceAll('\\','/')]=sha(await fs.readFile(file));}}await walk(root);return result;}
(async()=>{
 assert(!(await fs.stat(out).catch(()=>null)),'Keep prior attempts; use a new named output if retry is needed');await fs.mkdir(out,{recursive:true});
 const source=path.join(prior,'build/reference150-'+group+'-app'),study=JSON.parse(await fs.readFile(path.join(source,'result.json'),'utf8'));assert(study.passed);
 const beforeSource=await inventory(path.join(source,'project'));await fs.cp(path.join(source,'project'),project,{recursive:true});assert.deepEqual(await inventory(project),beforeSource);
 Object.assign(process.env,{FRAYNIX_WORKSPACE_PERSISTENT:'0',FRAYNIX_WORKSPACE_JAVA_HEAP_MIB:'768',FRAYNIX_WORKSPACE_JAVA_PROCESSORS:'2'});
 const result={passed:false,source:cp.execFileSync('git',['rev-parse','HEAD'],{encoding:'utf8'}).trim(),startedAt:new Date().toISOString(),group,domain:group==='generated150'?'exposed generated controls':'exposed edition150 camera footage',priorReceiptSha256:sha(await fs.readFile(path.join(source,'result.json'))),sourceFiles:Object.keys(beforeSource).length,checks:[],cases:[],captures:[],errors:[]};
 let app,page;const save=()=>fs.writeFile(path.join(out,'result.json'),JSON.stringify(result,null,2)+'\n');
 const api=(route,args={})=>app.evaluate((_,x)=>global.workspaceTest.api(x.route,x.args),{route,args});
 const read=(id,forceReplay=false)=>api('/api/association134/verified-read151',{id,forceReplay});
 async function open(){({app,page}=await launch(project));page.on('pageerror',e=>result.errors.push(e.message));await app.evaluate(({BrowserWindow})=>{const w=BrowserWindow.getAllWindows().find(w=>w.isVisible());w.setSize(1500,1000);w.setTitle('Fraynix | Edition 151 | Return diagnosis and verified replay');});await page.waitForFunction(()=>window.association134View,{},{timeout:60000});}
 const stateFile=path.join(project,'world-observations/state.json'),initialState=await fs.readFile(stateFile);const originalNamespaces={};for(const dir of ['world-observations','association-labels134','verified-appearances135'])originalNamespaces[dir]=await inventory(path.join(project,dir));
 async function capture(name,selector){await page.addStyleTag({content:'canvas,img,video{display:none!important} #association134-results{max-height:none!important;overflow:visible!important}'});assert.equal(await page.locator('canvas:visible,img:visible,video:visible').count(),0);await page.locator(selector).screenshot({path:path.join(out,name)});result.captures.push({name,sha256:sha(await fs.readFile(path.join(out,name))),privatePixelsExcluded:true});}
 try {
  await open();result.checks.push('Byte-identical private copy; original edition150 project remains separate');
  for(const old of study.runs.filter(x=>x.role==='evaluation'&&x.policy==='identity-return-149')){
   const row={case:old.caseName,auditId:old.audit.id,summary:old.audit.summary,timings:[],checks:[]};result.cases.push(row);await save();
   let value=await read(old.audit.id);assert.deepEqual(value.audit,old.audit);assert.equal(value.cost151.reusedReplay,false);row.timings.push({kind:'cold',...value.cost151});await save();
   for(let i=0;i<3;i++){value=await read(old.audit.id);assert.deepEqual(value.audit,old.audit);assert.equal(value.cost151.reusedReplay,true);row.timings.push({kind:'warm',...value.cost151});}await save();
   const diagnosis=await api('/api/association134/diagnose151',{id:old.audit.id});assert.equal(diagnosis.auditId,old.audit.id);assert.equal(diagnosis.cost151.reusedReplay,true);row.diagnosis=diagnosis;
   // Independent arithmetic over source-labelled rows; no additional tracker output is claimed.
   const returned=diagnosis.rows.filter(x=>x.afterOcclusion);assert.equal(diagnosis.summary.fullyVisibleReturns,returned.length);assert.equal(diagnosis.summary.strongAtLabelledTarget,returned.filter(x=>x.axisAlignedTarget.strong).length);assert.equal(diagnosis.summary.strongWithLabelGuidedSweep,returned.filter(x=>x.bestLabelGuidedProbe.strong).length);assert.equal(diagnosis.summary.searchOverlapsTarget,returned.filter(x=>x.searchTargetOverlap>=.5).length);
   row.checks.push('Complete original audit exactly equals edition150 result after cold and warm reads; labelled diagnostic arithmetic independently checked');await save();
   await page.locator('#spatial-tab').click();if(await page.locator('#expand-canvas').isVisible())await page.locator('#expand-canvas').click();
   await page.locator('#association134-panel').evaluate(e=>e.open=true);await page.waitForFunction(()=>!window.association134View.busy,{},{timeout:60000});await page.locator('#association134-refresh').click();await page.waitForFunction(()=>!window.association134View.busy,{},{timeout:60000});await page.locator('#association134-saved').selectOption(old.audit.id);
   await page.locator('#association134-read').click();await page.waitForFunction(()=>!window.association134View.busy,{},{timeout:60000});assert.deepEqual(await page.evaluate(()=>window.association134View.audit),old.audit);assert.match(await page.locator('#replay151-cost').textContent(),/Verified replay reused/);await capture(old.caseName+'-replay.png','#replay151-cost');
   await page.locator('#diagnosis151-run').click();await page.waitForFunction(()=>!window.association134View.busy,{},{timeout:180000});assert.deepEqual((await page.evaluate(()=>window.association134View.diagnosis151)).summary,diagnosis.summary);await capture(old.caseName+'-diagnosis.png','#diagnosis151-panel');
   row.checks.push('Actual visible read and diagnostic buttons return matching results; source pixels hidden in screenshots');await save();
   if(group==='webcam150-displaced-return'){
    // The explicit replay UI remains available and performs the full original computation.
    await page.locator('#replay151-full').click();await page.waitForFunction(()=>!window.association134View.busy,{},{timeout:900000});const forced=await page.evaluate(()=>window.association134View.replay151);assert.deepEqual(forced.audit,old.audit);assert.equal(forced.cost151.reusedReplay,false);row.timings.push({kind:'forced',...forced.cost151});await save();
    const dataset=old.dataset.id,frame=path.join(project,'world-observations/datasets',dataset,'45.image'),original=await fs.readFile(frame),stat=await fs.stat(frame),bad=Buffer.from(original);bad[20]^=1;
    await fs.writeFile(frame,bad);await fs.utimes(frame,stat.atime,stat.mtime);await assert.rejects(read(old.audit.id),/changed|integrity/i);await fs.writeFile(frame,original);await fs.utimes(frame,stat.atime,stat.mtime);row.checks.push('Same-size and restored-mtime source corruption refused; original bytes restored');await save();
    await app.close();app=null;await open();value=await read(old.audit.id);assert.deepEqual(value.audit,old.audit);assert.equal(value.cost151.reusedReplay,false);row.timings.push({kind:'cold-restart-after-restoration',...value.cost151});row.checks.push('Actual app restart performs full replay; no persisted memo authority');await save();
   }
  }
  assert.deepEqual(await fs.readFile(stateFile),initialState);assert.equal((await api('/api/observations/cost136')).durableWrites,0);assert.deepEqual(await api('/api/world/observe',{worldId:study.worldId}),study.nativeBefore);
  for(const [dir,original]of Object.entries(originalNamespaces))assert.deepEqual(await inventory(path.join(project,dir)),original);
  assert.deepEqual(await inventory(path.join(source,'project')),beforeSource);assert.deepEqual(result.errors,[]);result.checks.push('Original project unchanged; copied evidence namespaces unchanged; zero observation writes; native world unchanged; no renderer errors');result.passed=true;
 }catch(e){result.error=e.stack;throw e;}finally{if(app)await app.close();result.endedAt=new Date().toISOString();await save();}
 console.log(JSON.stringify({passed:result.passed,group,cases:result.cases.map(x=>({case:x.case,summary:x.diagnosis.summary,timings:x.timings})),checks:result.checks}));
})().catch(e=>{console.error(e);process.exitCode=1;});
