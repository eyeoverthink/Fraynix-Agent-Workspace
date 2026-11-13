'use strict';
// Inspect already completed evidence; no tracking rerun or source alteration.
const {launch}=require('../../workspace-desktop/test/calibration-integration.cjs');
const fs=require('node:fs/promises'),path=require('node:path'),assert=require('node:assert/strict');
(async()=>{
 Object.assign(process.env,{FRAYNIX_WORKSPACE_PERSISTENT:'0',FRAYNIX_WORKSPACE_JAVA_HEAP_MIB:'768',FRAYNIX_WORKSPACE_JAVA_PROCESSORS:'2'});
 const out=path.resolve('build/reference137-app'),record=JSON.parse(await fs.readFile(path.join(out,'result.json'),'utf8'));assert(record.passed);
 const {app,page}=await launch(path.join(out,'project'));const captures=[];
 try{
  await app.evaluate(({BrowserWindow})=>BrowserWindow.getAllWindows().find(x=>x.isVisible()).setSize(1600,1050));
  await page.waitForFunction(()=>window.association134View,{},{timeout:60000});await page.locator('#spatial-tab').click();await page.locator('#association134-panel').evaluate(e=>e.open=true);
  const idle=()=>page.waitForFunction(()=>!window.association134View.busy&&!window.labels132View.busy,{},{timeout:120000});await idle();await page.locator('#expand-canvas').click();
  for(const caseName of ['box-535','tiger2-155','bird1-193']){
   const r=record.runs.find(x=>x.caseName===caseName&&x.policy==='verified-memory-137');await page.locator('#association134-saved').selectOption(r.audit.id);await page.locator('#association134-read').click();await idle();
   assert.deepEqual(await page.evaluate(()=>window.association134View.audit),r.audit);
   const ordinal=caseName==='box-535'?65:caseName==='tiger2-155'?30:26;await page.locator('#association134-frame').selectOption(String(ordinal));await idle();await page.locator('#association134-canvas').scrollIntoViewIfNeeded();
   const name=caseName+'-source-overlay.png';await page.screenshot({path:path.join(out,name)});captures.push(name);
  }
 }finally{await app.close();}
 await fs.writeFile('build/capture137.json',JSON.stringify({passed:true,scope:'Actual UI source overlays from already completed immutable comparisons; no tracking rerun.',captures},null,2));
 for(const name of captures)if(!record.captures.includes(name))record.captures.push(name);await fs.writeFile(path.join(out,'result.json'),JSON.stringify(record,null,2));
})().catch(e=>{console.error(e);process.exitCode=1;});
