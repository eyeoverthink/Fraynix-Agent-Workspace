'use strict';
// Reopen the completed goal for a settled, genuine UI capture; no new action or replay.
const {launch}=require('./calibration-integration.cjs'),fs=require('node:fs/promises'),path=require('node:path'),assert=require('node:assert/strict'),crypto=require('node:crypto');
const sha=b=>crypto.createHash('sha256').update(b).digest('hex');
(async()=>{
 process.env.FRAYNIX_WORKSPACE_PERSISTENT='0';process.env.FRAYNIX_WORKSPACE_PLAN_CPU_ONLY='1';
 const root=path.resolve(__dirname,'../..'),primary=JSON.parse(await fs.readFile(path.join(root,'build/ui081-primary/result.json'),'utf8')),out=path.join(root,'build/ui081-review');
 await fs.mkdir(out,{recursive:true});require('../../scripts/Study-Build081.cjs').verify(root);
 const protectedPaths=['persistent-worlds/state.json','world-procedures-079/bank.json','world-procedures-079/cost-models080/'+primary.model.id+'.json'],before={};
 for(const p of protectedPaths)before[p]=sha(await fs.readFile(path.join(primary.project,p)));
 let app;
 try{
  const opened=await launch(primary.project);app=opened.app;const page=opened.page;await page.waitForFunction(()=>window.agency043View);
  await app.evaluate(({BrowserWindow})=>BrowserWindow.getAllWindows().find(w=>w.isVisible()).setSize(1600,1100));
  const g=await app.evaluate((_,id)=>global.workspaceTest.goals.execute('goal.read',{id}),primary.reuse.id);
  assert.equal(g.status,'completed');assert.equal(g.id,primary.reuse.id);assert.equal(g.worldCommit.hash,primary.reuse.worldCommit.hash);
  await page.waitForFunction(id=>document.querySelector('#java-goal').dataset.goalId===id&&document.querySelector('#java-goal-status').textContent==='completed',g.id);
  await page.locator('#java-goal-report').click();await page.waitForTimeout(1500);
  assert.equal(await page.locator('#java-goal-status').textContent(),'completed');assert(await page.locator('#java-goal-cancel').isHidden());
  await fs.writeFile(path.join(out,'cold-skill-confirmed.png'),Buffer.from(await app.evaluate(()=>global.workspaceTest.capture()),'base64'));
  for(const p of protectedPaths)assert.equal(sha(await fs.readFile(path.join(primary.project,p))),before[p]);
  await fs.writeFile(path.join(out,'result.json'),JSON.stringify({passed:true,project:primary.project,source:require('node:child_process').execFileSync('git',['rev-parse','HEAD'],{cwd:root,encoding:'utf8'}).trim(),goalId:g.id,checks:['Read-only reopen shows actual completed saved-skill goal and verified report','World ledger, retained bank and calibration remain byte-identical'],scope:'Settled UI capture after fresh reopen. No new native action, fitting, skill execution or completion claim from pixels.'},null,2));
  console.log('Settled saved-skill review captured without changing world evidence.');
 }finally{if(app)await app.close();}
})().catch(e=>{console.error(e);process.exitCode=1;});
