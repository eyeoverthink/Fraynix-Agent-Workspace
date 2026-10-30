'use strict';
const {_electron}=require('playwright');const assert=require('node:assert/strict');const fs=require('node:fs/promises');const os=require('node:os');const path=require('node:path');
async function launch(project){const env={...process.env,FRAYNIX_WORKSPACE_PROJECT:project,FRAYNIX_WORKSPACE_TEST:'1'};delete env.ELECTRON_RUN_AS_NODE;const app=await _electron.launch({args:[path.resolve(__dirname,'..','main.cjs')],env,timeout:60000});const page=await app.firstWindow();await app.evaluate(async()=>{for(let i=0;i<200&&!global.workspaceTest;i++)await new Promise(r=>setTimeout(r,100));if(!global.workspaceTest)throw Error('Workspace did not become ready');});return {app,page};}
async function main(){
  const project=await fs.mkdtemp(path.join(os.tmpdir(),'fraynix-learning-proof-')),output=path.resolve(__dirname,'..','test-output');await fs.mkdir(output,{recursive:true});let {app,page}=await launch(project);const checks=[];
  try{
    page.on('pageerror',error=>console.error('SHELL_PAGE_ERROR',error.message));
    const before=await app.evaluate(()=>global.workspaceTest.execute('browser.observe'));
    await page.getByText('Learning controls',{exact:true}).click();await page.getByRole('button',{name:'Run learning proof',exact:true}).click();
    await page.waitForFunction(()=>/Controller improved and checkpointed|Candidate rejected|No new candidate/.test(document.querySelector('#notice').textContent),null,{timeout:90000});
    assert((await page.getByRole('status').innerText()).includes('Controller improved and checkpointed'),'First candidate did not pass independent replay');
    const learned=(await app.evaluate(()=>global.workspaceTest.api('/api/state'))).learning;assert(learned.activePolicy.refreshStale);assert.equal(learned.activePolicy.version,2);assert.equal(learned.episodeCount,1);
    const report=learned.evaluations[0];console.log('FIRST_REPLAY',JSON.stringify({accepted:report.accepted,baseline:report.baselinePassed,candidate:report.candidatePassed}));assert(report.accepted);assert.equal(report.candidatePassed,8);assert(report.candidatePassed>report.baselinePassed);assert.equal(learned.controllerMemory.totalUpdates,1);
    const after=await app.evaluate(()=>global.workspaceTest.execute('browser.observe'));assert.equal(after.url,before.url);assert.equal(after.text,before.text);checks.push('Real stale-action episode generates a prototype/rule candidate; independent 8-case replay promotes it without changing the user browser');
    await page.getByRole('button',{name:'Learning',exact:true}).click();await page.getByRole('button',{name:/Replay · controller v2/}).click();
    await page.getByRole('dialog').getByText(/Promoted ·/).waitFor();
    let png=await app.evaluate(()=>global.workspaceTest.capture());await fs.writeFile(path.join(output,'learning-report.png'),Buffer.from(png,'base64'));
    await page.locator('#close-learning').click();await page.evaluate(()=>new Promise(resolve=>requestAnimationFrame(()=>requestAnimationFrame(resolve))));png=await app.evaluate(()=>global.workspaceTest.capture());await fs.writeFile(path.join(output,'learning-workspace.png'),Buffer.from(png,'base64'));checks.push('Existing library and timeline expose actual episodes, controller version and independent replay evidence');
    const recovered=await app.evaluate(async()=>{
      const old=await global.workspaceTest.execute('browser.observe');await global.workspaceTest.execute('browser.observe');
      const policy=(await global.workspaceTest.api('/api/state')).learning.activePolicy;
      return global.workspaceTest.controlledExecute({tool:'browser.click',args:{observation:old.observation,ref:old.controls.find(c=>c.label==='Record experiment').ref}},policy);
    });assert.equal(recovered.decision.tool,'browser.observe');assert.equal(recovered.decision.transition,'refresh-before-action');assert(!recovered.result.text.includes('Recorded:'));checks.push('Promoted policy changes the actual host action path: stale click becomes a fresh observation without an unintended click');
    const cost=await app.evaluate(async(_,id)=>{
      await global.workspaceTest.api('/api/learning/begin',{id,taskKind:'artifact-task',goal:'Repeated observation acceptance fixture'});
      const first=await global.workspaceTest.execute('browser.observe'),second=await global.workspaceTest.execute('browser.observe');
      if(first.text!==second.text||first.url!==second.url)throw Error('The actual page changed; this is not a repeated-read fixture');
      await global.workspaceTest.api('/api/learning/step',{id,step:{tool:'browser.observe',ok:true,failure:'repeated-observation',observationId:second.observation,controlCount:second.controls.length}});
      await global.workspaceTest.api('/api/learning/end',{id,status:'completed-unverified'});return global.workspaceTest.learnFromEpisodes();
    },require('node:crypto').randomUUID());assert(cost.accepted);assert.equal(cost.baselinePassed,8);assert.equal(cost.candidatePassed,8);assert(cost.candidateFailures<cost.baselineFailures);checks.push('Actual unchanged repeated observations produce a second candidate; replay retains 8/8 while reducing extra reads');
    const costPolicy=(await app.evaluate(()=>global.workspaceTest.api('/api/state'))).learning.activePolicy;assert.equal(costPolicy.observationLimit,1);
    const rejected=await app.evaluate(async()=>{
      const state=(await global.workspaceTest.api('/api/state')).learning;
      const candidate=await global.workspaceTest.api('/api/learning/propose',{parentVersion:state.activePolicy.version,parentGeneration:state.generation,episodes:[state.episodes[0].id],policy:{observationLimit:2,refreshStale:false}});
      const result=await global.workspaceTest.replay(state.activePolicy,candidate.policy);
      return global.workspaceTest.api('/api/learning/evaluate',{candidateId:candidate.id,...result});
    });assert.equal(rejected.accepted,false);assert.equal(rejected.regression,true);assert((await app.evaluate(()=>global.workspaceTest.api('/api/state'))).learning.activePolicy.refreshStale);checks.push('A valid-schema harmful candidate regresses real fixtures, is rejected and cannot replace the active controller');
    await app.close();({app,page}=await launch(project));const restored=(await app.evaluate(()=>global.workspaceTest.api('/api/state'))).learning;assert.equal(restored.activePolicy.version,costPolicy.version);assert.equal(restored.controllerMemory.totalUpdates,2);assert.equal(restored.evaluations.length,3);checks.push('Restart restores policy, learned prototype memory, episode and evaluation records');
    const rolled=await app.evaluate(()=>global.workspaceTest.api('/api/learning/rollback',{}));assert.equal(rolled.activePolicy.version,2);
    const counts=(await app.evaluate(()=>global.workspaceTest.api('/api/state'))).learning;
    const pending=app.evaluate(()=>global.workspaceTest.learnFromEpisodes({proof:true}));
    await page.locator('#run-state').filter({hasText:'Independent replay:'}).waitFor({timeout:30000});await app.evaluate(()=>global.workspaceTest.stopAgent());await assert.rejects(pending,/cancelled|stopped/i);
    const cancelled=(await app.evaluate(()=>global.workspaceTest.api('/api/state'))).learning;assert.equal(cancelled.activePolicy.version,2);assert.equal(cancelled.evaluations.length,counts.evaluations.length);assert(cancelled.candidates.some(c=>c.status==='cancelled'));checks.push('Rollback restores the parent; takeover during independent replay cancels the candidate without promotion');
    const second=await app.evaluate(()=>global.workspaceTest.learnFromEpisodes({proof:true}));assert(second.accepted);assert((await app.evaluate(()=>global.workspaceTest.api('/api/state'))).learning.activePolicy.refreshStale);checks.push('A cancelled evaluation can be retried; recorded failures yield a new verified controller version');
    await app.evaluate(()=>global.workspaceTest.guidedProof());const final=(await app.evaluate(()=>global.workspaceTest.api('/api/state'))).learning;assert(final.episodes.some(e=>e.status==='verified'&&e.steps.some(s=>s.testPassed)));
    const raw=JSON.stringify(final);assert(!raw.includes('Independent fixture value'));assert(!raw.includes('<script>'));assert(!raw.includes('rawPage'));checks.push('Existing browser/canvas guided workflow records a version-bound verified episode while omitting typed/page/source content');
    const result={date:'2026-10-29',passed:true,checks,firstReplay:report,costReplay:cost,harmfulReplay:rejected,finalPolicy:final.activePolicy,episodeCount:final.episodeCount,project};await fs.writeFile(path.join(output,'learning-integration.json'),JSON.stringify(result,null,2));console.log(JSON.stringify(result,null,2));
  }finally{await app.close();}
}
main().catch(error=>{console.error(error);process.exitCode=1;});
