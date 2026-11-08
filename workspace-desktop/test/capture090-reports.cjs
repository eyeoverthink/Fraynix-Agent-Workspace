'use strict';
// Read-only UI inspection after all measured runs. No native goals are executed.
const {launch}=require('./calibration-integration.cjs'),fs=require('node:fs/promises'),path=require('node:path'),assert=require('node:assert/strict');
(async()=>{
 const repo=path.resolve(__dirname,'../..'),base=JSON.parse(await fs.readFile(path.join(repo,'build/ui090-study/result.json'),'utf8')),follow=JSON.parse(await fs.readFile(path.join(repo,'build/ui090-followup/result.json'),'utf8'));
 assert(base.passed&&follow.passed);require('../../scripts/Study-Build090.cjs').verify(repo);
 process.env.FRAYNIX_WORKSPACE_PERSISTENT='0';process.env.FRAYNIX_WORKSPACE_PLAN_CPU_ONLY='1';process.env.FRAYNIX_WORKSPACE_JAVA_HEAP_MIB='512';process.env.FRAYNIX_WORKSPACE_JAVA_PROCESSORS='2';
 const selections=[...['covered','uncovered'].map(k=>({label:k==='covered'?'covered':'changed-scene',row:base.rows.find(x=>x.goalId===base.finalReview[k]),out:'ui090-study',widths:[1600,1100]})),...follow.rows.filter(x=>x.sequenceIndex===2).map(row=>({label:'short-'+row.policy,row,out:'ui090-followup',widths:[1600]}))],checks=[];
 for(const s of selections){
  const began=performance.now(),{app,page}=await launch(s.row.project),errors=[];page.on('pageerror',e=>errors.push(e.message));
  try{
   await page.waitForFunction(()=>window.agency043View);
   await app.evaluate((_,id)=>global.workspaceTest.goals.execute('goal.read',{id}),s.row.goalId);
   // goal.read asynchronously restores the world panel. Wait for that real UI
   // transition before requesting its report, so a late restore cannot hide it.
   await page.waitForFunction(id=>document.querySelector('#spatial-source').textContent.includes(id),s.row.id);
   await page.waitForTimeout(600);
   await page.locator('#java-goal-report').click();
   await page.waitForFunction(id=>document.querySelector('#object-detail').textContent.includes(id),s.row.goal.report.id);
   await page.locator('#preview-tab').click();await page.waitForTimeout(600);
   assert(await page.locator('#preview-tab').evaluate(e=>e.classList.contains('selected')));
   const text=await app.evaluate(()=>global.workspaceTest.canvasRead('document.body.innerText'));
   assert(typeof text==='string'&&text.includes('Verified world procedure'));
   for(const width of s.widths){
    await app.evaluate(({BrowserWindow},w)=>BrowserWindow.getAllWindows().find(x=>x.isVisible()).setSize(w,1100),width);await page.waitForTimeout(400);
    assert(await page.evaluate(()=>document.documentElement.scrollWidth<=innerWidth));
    await fs.writeFile(path.join(repo,'build',s.out,s.label+'-report-visible-'+width+'.png'),Buffer.from(await app.evaluate(()=>global.workspaceTest.capture()),'base64'));
    assert(await page.locator('#preview-tab').evaluate(e=>e.classList.contains('selected')));
   }
   assert.deepEqual(errors,[]);checks.push({goalId:s.row.goalId,project:s.row.project,label:s.label,widths:s.widths,callerMillis:performance.now()-began,reportText:text,passed:true});
  }finally{await app.close();}
 }
 await fs.writeFile(path.join(repo,'build/report-visual090.json'),JSON.stringify({passed:true,checks},null,2));console.log('PASS actual visible report previews; no timed runs changed');
})().catch(e=>{console.error(e);process.exitCode=1;});
