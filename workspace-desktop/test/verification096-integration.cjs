'use strict';
// Actual app/native review, execution, report, saved skill and cold restart.
// No model inference or substitute motion; the schema boundary is tested in Java.
const {launch}=require('./calibration-integration.cjs');
const fs=require('node:fs/promises'),sync=require('node:fs'),path=require('node:path'),assert=require('node:assert/strict'),cp=require('node:child_process');
const {unpack}=require('../../scripts/Inspect-Counterfactual045.cjs');
const sleep=ms=>new Promise(r=>setTimeout(r,ms));
async function main(){
 const repo=path.resolve(__dirname,'../..'),out=path.join(repo,'build/ui096'),project=path.join(out,'project');
 assert(!sync.existsSync(project),'Preserve original evidence; use a fresh checkout for another run');
 await fs.mkdir(project,{recursive:true});
 Object.assign(process.env,{FRAYNIX_WORKSPACE_PERSISTENT:'0',FRAYNIX_WORKSPACE_PLAN_CPU_ONLY:'1',FRAYNIX_WORKSPACE_JAVA_HEAP_MIB:'512',FRAYNIX_WORKSPACE_JAVA_PROCESSORS:'2'});
 const buildReceipt=require('../../scripts/Study-Build095.cjs').verify(repo);
 const record={buildReceipt,source:cp.execFileSync('git',['rev-parse','HEAD'],{cwd:repo,encoding:'utf8'}).trim(),startedAt:new Date().toISOString(),project,checks:[],captures:[],errors:[],scope:'Actual Electron/Java/native construction controls and report. No LLM inference. Synthetic bridge, not a physical load validation.'};
 let app,page;
 const api=(route,args={})=>app.evaluate((_,x)=>global.workspaceTest.api(x.route,x.args),{route,args});
 async function until(fn,ms=900000){const end=Date.now()+ms;while(Date.now()<end){const v=await fn();if(v)return v;await sleep(120);}throw Error('Bounded096 wait exceeded');}
 async function open(){({app,page}=await launch(project));page.on('pageerror',e=>record.errors.push(e.message));await page.locator('#spatial-tab').click();await page.locator('#construction051').evaluate(e=>e.open=true);await page.locator('#design053-options').evaluate(e=>e.open=true);}
 async function capture(label){for(const width of [1600,1100]){await app.evaluate(({BrowserWindow},width)=>BrowserWindow.getAllWindows().find(w=>w.isVisible()).setSize(width,1100),width);await sleep(400);assert(await page.evaluate(()=>document.documentElement.scrollWidth<=innerWidth));const file=label+'-'+width+'.png';await fs.writeFile(path.join(out,file),Buffer.from(await app.evaluate(()=>global.workspaceTest.capture()),'base64'));record.captures.push({file,width,label});}}
 async function exportStudy(goal,label){const bundle=await api('/api/world/counter045/export',{id:goal.counterfactual045.studyId});const counts=[];for(const attempt of bundle.originalAttempts){const run=unpack(attempt.run);assert.deepEqual(run.definition,bundle.study.source);let tick=0,effort=0;for(const s of run.samples)for(const row of s.integration){assert.equal(row[0],++tick);effort+=Math.hypot(...row[2])/240;}const c=bundle.study.candidates.find(c=>c.id===attempt.candidate);if(c.runId===run.id){assert.equal(tick,c.metrics.nativeTicks);assert(Math.abs(effort-c.metrics.effortNs)<1e-5);}counts.push({id:run.id,tick,effort});}await fs.writeFile(path.join(out,label+'.json'),JSON.stringify(bundle));return counts;}
 try{
  await open();await page.waitForFunction(()=>!document.querySelector('#c051-create').disabled);await page.locator('#c051-create').click();const worldId=await until(()=>page.locator('#c051-world').inputValue());record.worldId=worldId;record.before=await api('/api/world/observe',{worldId});
  await page.locator('#c051-skill').fill('verified-bridge096');await page.locator('#d053-review').check();await page.locator('#d053-build').click();
  const goal=await until(async()=>(await api('/api/goals/list')).goals.findLast(g=>g.worldBinding?.worldId===worldId));record.goalId=goal.id;
  const reviewed=await until(async()=>{const g=await api('/api/goals/read',{id:goal.id});if(g.status==='interrupted'&&g.step!=='world-review')throw Error(g.reason);return g.step==='world-review'?g:null;});record.review=reviewed;
  assert.equal(reviewed.status,'interrupted');assert.equal(reviewed.counterfactual045.candidates.length,3);assert.deepEqual(await api('/api/world/observe',{worldId}),record.before);record.reviewOriginals=await exportStudy(reviewed,'review-originals');
  await page.waitForFunction(()=>document.querySelectorAll('#d053-trials li').length===3);await page.locator('#design053-options').scrollIntoViewIfNeeded();await capture('review');record.checks.push('Three actual alternative trials and independently reconstructed effort; review leaves the active world unchanged.');
  await app.close();app=null;await open();assert.equal((await api('/api/goals/read',{id:goal.id})).counterfactual045.decisionSha256,reviewed.counterfactual045.decisionSha256);assert.deepEqual(await api('/api/world/observe',{worldId}),record.before);
  await page.locator('#c051-resume').click();const built=await until(async()=>{const g=await api('/api/goals/read',{id:goal.id});if(['interrupted','cancelled'].includes(g.status)&&g.step!=='world-review')throw Error(g.reason);return g.status==='completed'?g:null;});record.built=built;
  for(const k of ['metrics','constructionVerification051','poseView','counterfactual045View'])assert(built[k].passed,k);assert(built.counterfactual045.comparison.passed);const selected=built.counterfactual045.candidates.find(c=>c.id===built.counterfactual045.selectedCandidate);assert(selected.metrics.passed&&selected.structure053.passed);assert.equal(built.worldCommit.revision,1);assert.equal(built.savedSkill.name,'verified-bridge096');record.executionOriginals=await exportStudy(built,'built-originals');
  await page.locator('#d053-report').click();await page.waitForFunction(()=>document.querySelector('#preview-tab').classList.contains('selected'));await until(()=>app.evaluate(()=>global.workspaceTest.canvasRead('document.querySelector("#design053-policy")?.textContent==="Original motion and bridge support checks; then lowest measured effort"')));await capture('verified-report');record.checks.push('Cold restart preserves the exact reviewed decision; explicit resume verifies native motion, bridge support and rendered report before one commit and saved skill.');
  const held=await api('/api/world/observe',{worldId});const artifact=await api('/api/artifact/read',{id:built.report.id});assert(artifact.test.passed);record.artifact=artifact;
  const retainedSkill=(await api('/api/goals/list')).procedures.find(s=>s.name==='verified-bridge096'&&s.version===built.savedSkill.version);assert(retainedSkill);assert(retainedSkill.defaults.design053);assert.equal(retainedSkill.defaults.forceLimitN,40);
  await app.close();app=null;await open();assert.deepEqual(await api('/api/world/observe',{worldId}),held);const skills=(await api('/api/goals/list')).procedures;assert.deepEqual(skills.find(s=>s.id===retainedSkill.id),retainedSkill);record.skills=skills;record.after=held;record.checks.push('Second cold restart retains revision1, exact world hash and the complete verified procedure including its original defaults.');
  assert.deepEqual(record.errors,[]);require('../../scripts/Study-Build095.cjs').verify(repo);record.passed=true;record.finishedAt=new Date().toISOString();
 }catch(e){record.error=e.stack;throw e;}finally{if(app)await app.close();await fs.writeFile(path.join(out,'result.json'),JSON.stringify(record,null,2));}
 console.log(JSON.stringify({passed:record.passed,checks:record.checks,captures:record.captures}));
}
main().catch(e=>{console.error(e);process.exitCode=1;});
