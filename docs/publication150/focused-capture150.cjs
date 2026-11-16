'use strict';
// Focused documentation captures of already completed studies. Every source/outcome check for the displayed run is preserved; the full comparison matrix remains in its original study receipt.
const {launch}=require('../../workspace-desktop/test/calibration-integration.cjs');
const fs=require('node:fs/promises'),path=require('node:path'),assert=require('node:assert/strict');
(async()=>{
 const group=process.env.RETURN150_GROUP;assert(['generated150','webcam150-displaced-return'].includes(group));const repo=process.cwd(),out=path.join(repo,'build/reference150-'+group+'-review'),study=JSON.parse(await fs.readFile('build/reference150-'+group+'-app/result.json','utf8'));
 assert(study.passed);require('../../scripts/Study-Build095.cjs').verify(repo);
 Object.assign(process.env,{FRAYNIX_WORKSPACE_PERSISTENT:'0',FRAYNIX_WORKSPACE_JAVA_HEAP_MIB:'768',FRAYNIX_WORKSPACE_JAVA_PROCESSORS:'2'});
 const project=path.join(repo,'build/reference150-'+group+'-app/project'),state=path.join(project,'world-observations/state.json'),before=await fs.readFile(state),time=(await fs.stat(state)).mtimeMs;
 await fs.mkdir(out,{recursive:true});if(await fs.stat(path.join(out,'result.json')).catch(()=>null))await fs.copyFile(path.join(out,'result.json'),path.join(out,'result-attempt-'+Date.now()+'.json'));let app,page;const captures=[],errors=[];
 const result={passed:false,startedAt:new Date().toISOString(),scope:'Focused actual app read-only candidate result views from a completed full comparison. Every original source/outcome check for the displayed run is preserved. Every image, canvas and video is hidden before screenshots. No new labels or tracking. The UI audit-read performs the original full audit once; the returned audit must exactly equal the completed study. Other methods remain in the complete original study receipt.',captures};
 try{
  ({app,page}=await launch(project));page.on('pageerror',e=>errors.push(e.message));
  await app.evaluate(({BrowserWindow})=>{const w=BrowserWindow.getAllWindows().find(w=>w.isVisible());w.setSize(1600,1050);w.setTitle('Fraynix | Edition 150 | Verified results · camera pixels private');});
  await page.waitForFunction(()=>window.association134View,{},{timeout:60000});
  const hidePrivatePixels=()=>page.evaluate(()=>{for(const e of document.querySelectorAll('canvas,img,video'))e.style.setProperty('display','none','important');const results=document.querySelector('#association134-results');results.style.setProperty('max-height','none','important');results.style.setProperty('overflow','visible','important');});
  await hidePrivatePixels();
  const api=(route,args={})=>app.evaluate((_,x)=>global.workspaceTest.api(x.route,x.args),{route,args});
  const idle=()=>page.waitForFunction(()=>!window.association134View.busy&&!window.labels132View.observation.busy&&!window.labels132View.busy&&!window.labels132View.observation.refreshing,{},{timeout:900000});
  await page.locator('#spatial-tab').click();await page.locator('#expand-canvas').click();
  result.captureHarnessSha256=require('node:crypto').createHash('sha256').update(await fs.readFile(__filename)).digest('hex');
  result.fullStudyRuns=study.runs.length;
  for(const row of study.runs.filter(r=>r.role==='evaluation'&&r.policy==='identity-return-149')){
   // The actual UI audit-read calls evaluate(), which reads and verifies the run, references, original pixels, labels and outcomes. Do not issue the same audit twice solely for a screenshot.
   await page.locator('#association134-panel').evaluate(e=>e.open=true);await page.locator('#association134-refresh').click();await idle();
   await page.locator('#association134-saved').selectOption(row.audit.id);await page.locator('#association134-read').click();await idle();
   assert.deepEqual(await page.evaluate(()=>window.association134View.audit),row.audit);
   assert.equal(row.audit.input.runId,row.run.id);
   await hidePrivatePixels();assert.equal(await page.locator('canvas:visible,img:visible,video:visible').count(),0);
   const name=row.caseName+'-'+row.policy+'-results.png';await page.locator('#association134-results').screenshot({path:path.join(out,name)});
   captures.push({name,case:row.caseName,policy:row.policy,caption:'Actual saved '+row.policy+' outcome comparison. Source camera pixels hidden; all counts retained.'});
  }
  assert.equal(captures.length,group==='generated150'?2:1);
  assert.deepEqual(await fs.readFile(state),before);assert.equal((await fs.stat(state)).mtimeMs,time);
  assert.equal((await api('/api/observations/cost136')).durableWrites,0);assert.deepEqual(await api('/api/world/observe',{worldId:study.worldId}),study.nativeBefore);assert.deepEqual(errors,[]);
  result.passed=true;result.completedStateUnchanged=true;result.zeroObservationWrites=true;result.nativeWorldUnchanged=true;result.privatePixelsExcluded=true;
 }catch(e){result.error=e.stack;throw e;}
 finally{if(app)await app.close();result.endedAt=new Date().toISOString();await fs.writeFile(path.join(out,'result.json'),JSON.stringify(result,null,2)+'\n');}
 console.log(JSON.stringify(result));
})().catch(e=>{console.error(e);process.exitCode=1;});
