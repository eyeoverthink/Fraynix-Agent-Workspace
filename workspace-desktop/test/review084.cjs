'use strict';
// Read-only visual review after timed studies. Keep its cost outside timed goals.
const {launch}=require('./calibration-integration.cjs'),fs=require('node:fs/promises'),path=require('node:path'),assert=require('node:assert/strict'),crypto=require('node:crypto');
const sha=b=>crypto.createHash('sha256').update(b).digest('hex');
(async()=>{
 process.env.FRAYNIX_WORKSPACE_PERSISTENT='0';process.env.FRAYNIX_WORKSPACE_PLAN_CPU_ONLY='1';process.env.FRAYNIX_WORKSPACE_JAVA_HEAP_MIB='768';process.env.FRAYNIX_WORKSPACE_JAVA_PROCESSORS='4';
 const repo=path.resolve(__dirname,'../..'),out=path.join(repo,'build/ui084-review');await fs.mkdir(out,{recursive:true});const checks=[],skills=[];
 const verifySkill=(row,procedures)=>{const p=procedures.find(x=>x.id===row.goal.savedSkill.id);assert(p,'Saved procedure must be present in the reopened Java registry');assert.equal(p.name,row.skillName);assert.equal(p.version,row.goal.savedSkill.version);assert.equal(p.sourceGoalId,row.goalId);assert.equal(p.code,row.job.code);assert.deepEqual(p.defaults.targets,row.job.args.targets);for(const k of ['prerequisites','actions','recovery'])assert(p[k].length>0);skills.push({project:row.project,name:p.name,id:p.id,version:p.version,sourceGoalId:p.sourceGoalId,code:p.code});};
 for(const group of ['cold-run','cold-skill']){
  const source=path.join(repo,'build/ui084-'+group+(group==='cold-skill'?'-v2':'')),r=JSON.parse(await fs.readFile(path.join(source,'result.json'),'utf8'));assert(r.passed);
  const bank=path.join(r.project,'world-procedures-079/bank.json'),ledger=path.join(r.project,'persistent-worlds/state.json'),before=await fs.readFile(bank),worldBefore=await fs.readFile(ledger);let app;
  try{const opened=await launch(r.project);app=opened.app;const page=opened.page;await page.waitForFunction(()=>window.agency043View);
   if(r.group.skill){const registry=await app.evaluate(()=>global.workspaceTest.api('/api/goals/list',{}));for(const row of r.calibration.flatMap(x=>Object.values(x.results)))verifySkill(row,registry.procedures);}
   await page.locator('#spatial-tab').click();await page.locator('#persistent-world-panel').evaluate(e=>e.open=true);await page.locator('#cost080-panel').evaluate(e=>e.open=true);await page.locator('#cost080-review').click();
   await page.waitForFunction(()=>document.querySelector('#cost083-coverage').textContent.includes('Repeat counts: 3, 3'));
   for(const width of [1600,1100]){
    await app.evaluate(({BrowserWindow},w)=>BrowserWindow.getAllWindows().find(x=>x.isVisible()).setSize(w,1000),width);
    await page.locator('#cost083-coverage').scrollIntoViewIfNeeded();await page.waitForTimeout(200);
    assert(await page.evaluate(()=>document.documentElement.scrollWidth<=innerWidth));assert(await page.locator('#cost083-coverage').isVisible());
    const text=await page.locator('#cost083-coverage').innerText();assert(text.includes('Repeat counts: 3, 3'));assert(text.includes(group==='cold-skill'?'Save skill':'Run without saving'));
    await fs.writeFile(path.join(out,group+'-calibration-'+width+'.png'),Buffer.from(await app.evaluate(()=>global.workspaceTest.capture()),'base64'));
    checks.push({group,width,text});
   }
  }finally{if(app)await app.close();}
  assert.equal(sha(await fs.readFile(bank)),sha(before));assert.equal(sha(await fs.readFile(ledger)),sha(worldBefore));
  if(r.group.skill)for(const row of r.rows){let opened;try{opened=await launch(row.project);const registry=await opened.app.evaluate(()=>global.workspaceTest.api('/api/goals/list',{}));verifySkill(row,registry.procedures);}finally{if(opened)await opened.app.close();}}
 }
 await fs.writeFile(path.join(out,'result.json'),JSON.stringify({passed:true,checks,skills,scope:'Actual post-study coverage view at two widths; no world or bank mutation. Earlier timed-study screenshots retained separately.'},null,2));
 console.log('Actual coverage views captured with repeat counts in view.');
})().catch(e=>{console.error(e);process.exitCode=1;});
