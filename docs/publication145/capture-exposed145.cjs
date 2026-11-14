'use strict';
// Documentation-only review of completed runs. Never starts a tracker or changes labels.
const {launch}=require('../../workspace-desktop/test/calibration-integration.cjs');
const fs=require('node:fs/promises'),path=require('node:path'),assert=require('node:assert/strict');
(async()=>{
 const repo=process.cwd(),out=path.join(repo,'build/exposed145-review'),study=JSON.parse(await fs.readFile('build/exposed145-app/result.json','utf8'));
 assert(study.passed);require('../../scripts/Study-Build095.cjs').verify(repo);
 Object.assign(process.env,{FRAYNIX_WORKSPACE_PERSISTENT:'0',FRAYNIX_WORKSPACE_JAVA_HEAP_MIB:'768',FRAYNIX_WORKSPACE_JAVA_PROCESSORS:'2'});
 const project=path.join(repo,'build/exposed145-app/project'),state=path.join(project,'world-observations/state.json'),before=await fs.readFile(state),time=(await fs.stat(state)).mtimeMs;
 await fs.mkdir(out,{recursive:true});let app,page;const captures=[],errors=[];
 const result={passed:false,startedAt:new Date().toISOString(),scope:'Illustrative post-study screenshots of completed evidence. Frames selected for explanatory value; no new tracking, label changes or qualification decisions.',captures};
 try{
  ({app,page}=await launch(project));page.on('pageerror',e=>errors.push(e.message));
  await app.evaluate(({BrowserWindow})=>{const w=BrowserWindow.getAllWindows().find(w=>w.isVisible());w.setSize(1600,1050);w.setTitle('Fraynix | Edition 145 | Completed evidence review');});
  await page.waitForFunction(()=>window.association134View,{},{timeout:60000});
  const api=(route,args={})=>app.evaluate((_,x)=>global.workspaceTest.api(x.route,x.args),{route,args});
  const idle=()=>page.waitForFunction(()=>!window.association134View.busy&&!window.labels132View.observation.busy&&!window.labels132View.busy&&!window.labels132View.observation.refreshing,{},{timeout:180000});
  await page.locator('#spatial-tab').click();await page.locator('#persistent-world-panel').evaluate(e=>{e.open=true;e.querySelector('details').open=true;});
  await page.waitForFunction(id=>[...document.querySelector('#persistent-worlds').options].some(o=>o.value===id),study.worldId,{timeout:60000});
  await page.locator('#persistent-worlds').selectOption(study.worldId);
  await page.waitForFunction(id=>window.labels132View.observation.persistent.id===id,study.worldId,{timeout:60000});
  await page.locator('#expand-canvas').click();
  for(const name of ['exposed-return','exposed-copy']){
   const row=study.runs.find(r=>r.caseName===name&&r.policy==='trajectory-guard-145')||study.runs.find(r=>r.caseName===name&&r.policy==='foreground-guard-139');assert(row,name);
   const restored=row.run.trace.find(f=>f.objects[0].tracker.trajectory145?.restored);
   const contradicted=row.audit.scores.find(s=>s.confirmedDistractorMatch)||row.audit.scores.find(s=>s.visibility==='occluded');
   const ordinal=restored?.ordinal||contradicted?.ordinal||row.run.trace.find(f=>f.objects[0].tracker.trajectory145?.rivals?.length)?.ordinal||Math.min(20,row.run.trace.length);
   const caption='Exposed development regression; NOT fresh qualification'+' / '+name+' / '+row.policy+'. Illustrative completed source and trace; consult all outcomes, including refusals and wrong objects.';
   assert.deepEqual(await api('/api/observations/read',{id:row.run.id}),row.run);
   assert.deepEqual(await api('/api/association134/audit-read',{id:row.audit.id}),row.audit);
   await page.locator('#association134-panel').evaluate(e=>e.open=true);await page.locator('#association134-refresh').click();await idle();
   await page.locator('#association134-saved').selectOption(row.audit.id);await page.locator('#association134-read').click();await idle();
   await page.locator('#association134-frame').selectOption(String(ordinal));await idle();
   const sourceName=name+'-review-source.png';await page.locator('#association134-canvas').scrollIntoViewIfNeeded();await page.screenshot({path:path.join(out,sourceName)});
   captures.push({name:sourceName,case:name,ordinal,caption});
   await page.locator('#association134-panel').evaluate(e=>e.open=false);
   await page.evaluate(async id=>{const v=window.labels132View.observation;v.root.open=true;v.runId=id;v.follow=false;await v.refresh();},row.run.id);
   await page.waitForFunction(id=>window.labels132View.observation.run?.id===id,row.run.id,{timeout:180000});await idle();
   await page.evaluate(async ordinal=>{document.querySelector('#observations-frame-slider').value=String(ordinal);await window.labels132View.observation.preview();},ordinal);await idle();
   const driftName=name+'-review-drift.png';await page.locator('#observations-frame').evaluate(e=>e.scrollIntoView({block:'center'}));await page.waitForTimeout(500);await page.screenshot({path:path.join(out,driftName)});
   captures.push({name:driftName,case:name,ordinal,caption});
  }
  assert.deepEqual(await fs.readFile(state),before);assert.equal((await fs.stat(state)).mtimeMs,time);
  assert.equal((await api('/api/observations/cost136')).durableWrites,0);assert.deepEqual(await api('/api/world/observe',{worldId:study.worldId}),study.nativeBefore);assert.deepEqual(errors,[]);
  result.passed=true;result.completedStateUnchanged=true;result.zeroObservationWrites=true;result.nativeWorldUnchanged=true;
 }catch(e){result.error=e.stack;throw e;}
 finally{if(app)await app.close();result.endedAt=new Date().toISOString();await fs.writeFile(path.join(out,'result.json'),JSON.stringify(result,null,2)+'\n');}
 console.log(JSON.stringify(result));
})().catch(e=>{console.error(e);process.exitCode=1;});
