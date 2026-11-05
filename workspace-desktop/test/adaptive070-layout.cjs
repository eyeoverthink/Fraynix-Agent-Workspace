'use strict';
const {launch}=require('./calibration-integration.cjs'),fs=require('node:fs/promises'),path=require('node:path'),assert=require('node:assert/strict');
async function main(){
 const file=process.env.FRAYNIX070_LAYOUT_SOURCE;if(!file)throw Error('Set FRAYNIX070_LAYOUT_SOURCE to the completed isolated app validation JSON');
 const prior=JSON.parse(await fs.readFile(file,'utf8'));assert(prior.passed);const out=path.resolve(__dirname,'../test-output'),checks=[];let app,page;
 process.env.FRAYNIX_WORKSPACE_PERSISTENT='0';process.env.FRAYNIX_WORKSPACE_PLAN_CPU_ONLY='1';
 const source=require('node:child_process').execFileSync('git',['rev-parse','HEAD'],{encoding:'utf8'}).trim();
 const sleep=ms=>new Promise(r=>setTimeout(r,ms));async function until(f){const deadline=Date.now()+180000;while(Date.now()<deadline){if(await f())return;await sleep(500)}throw Error('Saved layout view did not load')}
 async function select(phase,index){await page.locator('#a070-phase').selectOption(phase);await until(async()=>await page.locator('#a070-case option').count()===prior.job[phase+'070'].length);await page.locator('#a070-case').selectOption(String(index));await until(async()=>(await page.locator('#a070-task').innerText()).includes(prior.job[phase+'070'][index].task.angleDeg.toFixed(3)+'°'))}
 async function capture(name){await page.evaluate(()=>new Promise(r=>requestAnimationFrame(()=>requestAnimationFrame(r))));await sleep(300);await fs.writeFile(path.join(out,'adaptive070-'+name+'.png'),Buffer.from(await app.evaluate(()=>global.workspaceTest.capture()),'base64'))}
 try{
  ({app,page}=await launch(prior.project));await app.evaluate(({BrowserWindow})=>BrowserWindow.getAllWindows()[0].setSize(1600,1100));await page.locator('#brain-open').click();await page.locator('#adaptive070').evaluate(e=>e.open=true);await page.locator('#a070-history').selectOption(prior.job.id);await until(async()=>!await page.locator('#a070-load').isDisabled());
  await select('unsupported',0);const boxes=await page.locator('#a070-goal').evaluate(svg=>{const texts=[...svg.querySelectorAll('text')];return ['fixed','fixedCalibrated','adaptive','adaptiveCalibrated'].map(name=>{const a=texts.find(t=>t.textContent===name),b=texts.find(t=>t.textContent==='Refused · no motion'&&t.getAttribute('y')===a.getAttribute('y'));const x=a.getBBox(),y=b.getBBox();return {name,labelRight:x.x+x.width,refusalLeft:y.x}})});assert(boxes.every(b=>b.labelRight+3<b.refusalLeft));checks.push({name:'All four refusal labels remain separate from policy names',boxes});await page.locator('#a070-task').scrollIntoViewIfNeeded();await capture('domain');
  await select('disturbed',Math.max(0,prior.savedGoalIndex));await page.locator('#a070-method').selectOption('adaptive');await page.locator('#a070-outcomes').scrollIntoViewIfNeeded();await capture('paired');
  const declined=prior.job.clean070.findIndex(r=>!r.plan.decisions.fixedCalibrated.execute);assert(declined>=0);await select('clean',declined);await page.locator('#a070-method').selectOption('adaptiveCalibrated');await page.locator('#a070-stage').scrollIntoViewIfNeeded();await capture('decline');
  const state=await app.evaluate(()=>global.workspaceTest.api('/api/brain/state',{}));assert.equal(state.activePolicy.version,1);assert.equal(state.adaptive070.lastCompleted.jobId,prior.job.id);assert(!state.busy);
  const result={passed:true,source,project:prior.project,date:new Date().toISOString(),checks,recaptured:['domain','paired','decline'],scope:'Layout-only validation against the identical completed study; no native experiment or model request repeated.'};await fs.writeFile(path.join(out,'adaptive070-layout.json'),JSON.stringify(result,null,2));console.log(JSON.stringify(result));
 }finally{if(app)await app.close()}
}
main().catch(e=>{console.error(e);process.exitCode=1});
