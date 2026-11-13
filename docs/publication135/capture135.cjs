'use strict';
// Inspect already completed evidence; no tracking rerun or source alteration.
const {launch}=require('../../workspace-desktop/test/calibration-integration.cjs');
const fs=require('node:fs/promises'),path=require('node:path'),assert=require('node:assert/strict');
(async()=>{
 Object.assign(process.env,{FRAYNIX_WORKSPACE_PERSISTENT:'0',FRAYNIX_WORKSPACE_JAVA_HEAP_MIB:'768',FRAYNIX_WORKSPACE_JAVA_PROCESSORS:'2'});
 const out=path.resolve('build/appearance135-app'),record=JSON.parse(await fs.readFile(path.join(out,'result.json'),'utf8'));assert(record.passed);
 const {app,page}=await launch(path.join(out,'project'));const captures=[];
 try{
  await app.evaluate(({BrowserWindow})=>BrowserWindow.getAllWindows().find(x=>x.isVisible()).setSize(1600,1050));
  await page.waitForFunction(()=>window.association134View,{},{timeout:60000});await page.locator('#spatial-tab').click();await page.locator('#association134-panel').evaluate(e=>e.open=true);
  const idle=()=>page.waitForFunction(()=>!window.association134View.busy&&!window.labels132View.busy,{},{timeout:120000});await idle();await page.locator('#expand-canvas').click();
  for(const caseName of ['box-439','david3-57']){
   const r=record.runs.find(x=>x.caseName===caseName&&x.policy==='appearance-continuity-135');await page.locator('#association134-saved').selectOption(r.audit.id);await page.locator('#association134-read').click();await idle();
   assert.deepEqual(await page.evaluate(()=>window.association134View.audit),r.audit);
   const admission=r.audit.appearance135.admissions.find(x=>x.verdict==='incorrect');await page.locator('#association134-frame').selectOption(String(admission.ordinal));await idle();await page.locator('#association134-canvas').scrollIntoViewIfNeeded();
   const name=caseName+'-source-drift.png';await page.screenshot({path:path.join(out,name)});captures.push(name);
  }
 }finally{await app.close();}
 await fs.writeFile('build/capture135.json',JSON.stringify({passed:true,scope:'Actual UI source overlays from already completed immutable comparisons; no tracking rerun.',captures},null,2));
 for(const name of captures)if(!record.captures.includes(name))record.captures.push(name);await fs.writeFile(path.join(out,'result.json'),JSON.stringify(record,null,2));
})().catch(e=>{console.error(e);process.exitCode=1;});
