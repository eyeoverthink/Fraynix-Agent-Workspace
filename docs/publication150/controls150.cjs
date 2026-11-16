'use strict';
// Read-only actual application views of the new retention controls and saved reference kind.
const {launch}=require('../../workspace-desktop/test/calibration-integration.cjs');
const fs=require('node:fs/promises'),path=require('node:path'),assert=require('node:assert/strict');
(async()=>{
 const repo=process.cwd(),group='webcam150-displaced-return',out=path.join(repo,'build/reference150-controls');
 const study=JSON.parse(await fs.readFile('build/reference150-'+group+'-app/result.json','utf8'));assert(study.passed);
 const donor=study.runs.find(r=>r.role==='reference-source');assert(donor.retained.evidenceKind==='verified-starting-reference-150');
 require('../../scripts/Study-Build095.cjs').verify(repo);
 Object.assign(process.env,{FRAYNIX_WORKSPACE_PERSISTENT:'0',FRAYNIX_WORKSPACE_JAVA_HEAP_MIB:'768',FRAYNIX_WORKSPACE_JAVA_PROCESSORS:'2'});
 const project=path.join(repo,'build/reference150-'+group+'-app/project'),state=path.join(project,'world-observations/state.json'),before=await fs.readFile(state),mtime=(await fs.stat(state)).mtimeMs;
 await fs.mkdir(out);let app,page;const result={passed:false,startedAt:new Date().toISOString(),captures:[]};
 try{
  ({app,page}=await launch(project));await app.evaluate(({BrowserWindow})=>BrowserWindow.getAllWindows().find(w=>w.isVisible()).setSize(1500,1050));
  await page.waitForFunction(()=>window.association134View,{},{timeout:60000});
  const api=(route,args={})=>app.evaluate((_,x)=>global.workspaceTest.api(x.route,x.args),{route,args});
  const idle=()=>page.waitForFunction(()=>!window.association134View.busy&&!window.labels132View.observation.busy&&!window.labels132View.busy&&!window.labels132View.observation.refreshing,{},{timeout:180000});
  const hide=()=>page.evaluate(()=>{for(const e of document.querySelectorAll('canvas,img,video'))e.style.setProperty('display','none','important');});
  await hide();await page.locator('#spatial-tab').click();await page.locator('#expand-canvas').click();
  await page.locator('#association134-panel').evaluate(e=>e.open=true);await idle();await page.locator('#association134-refresh').click();await idle();
  await page.locator('#association134-saved').selectOption(donor.audit.id);await page.locator('#association134-read').click();await idle();await hide();
  assert.deepEqual(await page.evaluate(()=>window.association134View.audit),donor.audit);assert(await page.locator('#starting150-retain').isEnabled());
  assert.equal(await page.locator('canvas:visible,img:visible,video:visible').count(),0);
  await page.locator('#association134-panel').screenshot({path:path.join(out,'starting-reference-controls.png')});
  result.captures.push({name:'starting-reference-controls.png',caption:'Actual fresh-camera donor comparison and explicit starting-reference retention control. Original source imagery hidden; no retention action repeated for this screenshot.'});
  await page.locator('#observations-panel').evaluate(e=>e.open=true);await idle();await page.locator('#observations-references137').evaluate(e=>e.open=true);
  await page.locator('#observations-references-refresh137').click();await idle();await page.locator('#observations-reference-set137').selectOption(donor.retained.id);await hide();
  assert.match(await page.locator('#observations-reference-set137 option:checked').textContent(),/verified start · 0 learned/);
  assert.equal(await page.locator('canvas:visible,img:visible,video:visible').count(),0);
  await page.locator('#observations-references137').screenshot({path:path.join(out,'retained-starting-reference.png')});
  result.captures.push({name:'retained-starting-reference.png',caption:'Actual retained starting reference in the selector: one verified source reference, zero learned appearances. No tracking started.'});
  assert.deepEqual(await fs.readFile(state),before);assert.equal((await fs.stat(state)).mtimeMs,mtime);
  assert.equal((await api('/api/observations/cost136')).durableWrites,0);assert.deepEqual(await api('/api/world/observe',{worldId:study.worldId}),study.nativeBefore);
  result.passed=true;result.privatePixelsExcluded=true;result.completedStateUnchanged=true;result.zeroObservationWrites=true;
 }catch(e){result.error=e.stack;throw e;}finally{if(app)await app.close();result.endedAt=new Date().toISOString();await fs.writeFile(path.join(out,'result.json'),JSON.stringify(result,null,2)+'\n');}
 console.log(JSON.stringify(result));
})().catch(e=>{console.error(e);process.exitCode=1;});
