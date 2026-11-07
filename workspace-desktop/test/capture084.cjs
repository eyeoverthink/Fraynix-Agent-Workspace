'use strict';
const {launch}=require('./calibration-integration.cjs'),fs=require('node:fs/promises'),path=require('node:path'),assert=require('node:assert/strict'),crypto=require('node:crypto');
const sha=b=>crypto.createHash('sha256').update(b).digest('hex');
(async()=>{
 Object.assign(process.env,{FRAYNIX_WORKSPACE_PERSISTENT:'0',FRAYNIX_WORKSPACE_PLAN_CPU_ONLY:'1',FRAYNIX_WORKSPACE_JAVA_HEAP_MIB:'768',FRAYNIX_WORKSPACE_JAVA_PROCESSORS:'4'});
 const repo=path.resolve(__dirname,'../..'),out=path.join(repo,'build/ui084-final-captures');await fs.mkdir(out,{recursive:true});const checks=[];
 for(const group of ['cold-run','cold-skill']){
  const r=JSON.parse(await fs.readFile(path.join(repo,'build/ui084-'+group+(group==='cold-skill'?'-v2':''),'result.json'),'utf8'));assert(r.passed);let app;
  try{const opened=await launch(r.project);app=opened.app;const page=opened.page;await page.waitForFunction(()=>window.agency043View);
   await page.locator('#spatial-tab').click();await page.locator('#persistent-world-panel').evaluate(e=>e.open=true);
   assert.equal(await page.locator('#cost080-mode option[value=calibrated]').textContent(),'Repeated task measurements (084)');
   await page.locator('#cost080-panel').evaluate(e=>e.open=true);await page.locator('#cost080-review').click();await page.waitForFunction(()=>document.querySelector('#cost083-coverage').textContent.includes('Repeat counts: 3, 3'));
   for(const width of [1600,1100]){await app.evaluate(({BrowserWindow},w)=>BrowserWindow.getAllWindows().find(x=>x.isVisible()).setSize(w,1000),width);await page.locator('#cost083-coverage').scrollIntoViewIfNeeded();assert(await page.evaluate(()=>document.documentElement.scrollWidth<=innerWidth));await fs.writeFile(path.join(out,group+'-calibration-'+width+'.png'),Buffer.from(await app.evaluate(()=>global.workspaceTest.capture()),'base64'));checks.push({group,width,label:'Repeated task measurements (084)',repeatCounts:[3,3]});}
  }finally{if(app)await app.close();}
  if(group==='cold-skill'){
   const row=r.rows.at(-1),ledger=path.join(row.project,'persistent-worlds/state.json'),before=await fs.readFile(ledger);let opened;
   try{opened=await launch(row.project);const {app,page}=opened;await page.waitForFunction(()=>window.agency043View);await app.evaluate(({BrowserWindow})=>BrowserWindow.getAllWindows().find(x=>x.isVisible()).setSize(1600,1100));
    await page.locator('#spatial-tab').click();await page.locator('#persistent-world-panel').evaluate(e=>e.open=true);await page.waitForFunction(id=>[...document.querySelectorAll('#persistent-worlds option')].some(x=>x.value===id),row.binding.worldId);await page.locator('#persistent-worlds').selectOption(row.binding.worldId);
    await page.waitForFunction(()=>document.querySelector('#spatial-live').textContent.includes('revision 1'));await page.locator('#persistent-world-panel').evaluate(e=>e.open=false);await page.locator('#spatial-frame').click();await page.locator('#spatial-stage').scrollIntoViewIfNeeded();await page.waitForTimeout(400);
    assert(await page.locator('#spatial-stage canvas').isVisible());const state=await app.evaluate((_,id)=>global.workspaceTest.api('/api/world/observe',{worldId:id}),row.binding.worldId);assert.equal(state.expectedHash,row.persistence.hash);
    await fs.writeFile(path.join(out,'cold-skill-case-1-1-world.png'),Buffer.from(await app.evaluate(()=>global.workspaceTest.capture()),'base64'));checks.push({worldId:row.binding.worldId,hash:state.expectedHash,revision:state.expectedRevision,view:'Actual committed world in the existing renderer'});
   }finally{if(opened)await opened.app.close();}
   assert.equal(sha(await fs.readFile(ledger)),sha(before));
  }
 }
 await fs.writeFile(path.join(out,'result.json'),JSON.stringify({passed:true,checks,scope:'Final display label and real renderer captures after timing; no new native action or world commit.'},null,2));console.log('Final label, coverage and committed world captures passed.');
})().catch(e=>{console.error(e);process.exitCode=1;});
