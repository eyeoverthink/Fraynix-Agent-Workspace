'use strict';
const {_electron}=require('playwright');const fs=require('node:fs/promises');const path=require('node:path');const os=require('node:os');const assert=require('node:assert/strict');
async function main(){
 const project=await fs.mkdtemp(path.join(os.tmpdir(),'fraynix-workspace-live-'));
 const env={...process.env,FRAYNIX_WORKSPACE_PROJECT:project,FRAYNIX_WORKSPACE_TEST:'1'};delete env.ELECTRON_RUN_AS_NODE;
 const app=await _electron.launch({args:[path.resolve(__dirname,'..','main.cjs')],env,timeout:60000});
 try{
  const page=await app.firstWindow();const model=process.env.FRAYNIX_LIVE_MODEL||'gpt-oss:20b';
  await app.evaluate(async()=>{for(let i=0;i<200&&!global.workspaceTest;i++)await new Promise(r=>setTimeout(r,100));if(!global.workspaceTest)throw Error('Workspace did not become ready');});
  const learningProof=await app.evaluate(()=>global.workspaceTest.learnFromEpisodes({proof:true}));assert(learningProof.accepted);
  await page.waitForFunction(model=>Array.from(document.querySelector('#model').options).some(o=>o.value===model),model,{timeout:60000});
  await page.getByLabel('Local model').selectOption(model);
  const goal='Use browser.observe to read the practice page. Then create a NEW working counter web app titled Agent-built counter. Use output id="count" initially showing 0, button id="add" adding one, and button id="reset" restoring zero. Make it attractive with inline CSS. Run canvas.test to check 0, click Add, check 1, click Reset, check 0. If it fails, repair the source and retest. Save the verified recipe as agent-counter. Finish only after actual passing tests and skill.save.';
  await page.getByLabel('Agent request').fill(goal);await page.getByRole('button',{name:'Start task'}).click();
  const deadline=Date.now()+480000;let last='';
  while(Date.now()<deadline){
   await new Promise(r=>setTimeout(r,2000));
   const status=await page.evaluate(()=>({busy:!document.querySelector('#stop').disabled,events:Array.from(document.querySelectorAll('#events .text')).slice(-3).map(e=>e.textContent)}));
   const summary=status.events.join(' | ');if(summary!==last){console.log(summary);last=summary;}
   if(!status.busy)break;
  }
  const state=await app.evaluate(()=>global.workspaceTest.api('/api/state'));
  const log=(await fs.readFile(path.join(project,'events.jsonl'),'utf8')).trim().split('\n').map(line=>JSON.parse(line));
  const completed=log.find(e=>e.kind==='agent_completed');assert(completed,'Live model did not finish a verified workflow');
  assert(state.skills.some(s=>s.name==='agent-counter'),'Live model did not save the requested verified recipe');
  const artifacts=state.artifacts.filter(a=>a.title==='Agent-built counter');assert(artifacts.length);
  const artifact=await app.evaluate((_,id)=>global.workspaceTest.api('/api/artifact/read',{id}),artifacts.at(-1).id);assert.equal(artifact.test?.passed,true);
  const episode=state.learning.episodes.find(e=>e.model===model&&e.status==='verified');assert(episode,'Live task did not retain verified learning evidence');assert.equal(episode.policy.version,2);assert(episode.steps.some(s=>s.tool==='canvas.test'&&s.testPassed));
  const acceptance=await app.evaluate(()=>global.workspaceTest.execute('canvas.test',{steps:[{selector:'#count',expectText:'0'},{selector:'#add',action:'click',expectCount:1},{selector:'#count',expectText:'1'},{selector:'#reset',action:'click',expectCount:1},{selector:'#count',expectText:'0'}]}));assert.equal(acceptance.passed,true,'Independent Add/Reset acceptance checks failed');
  const output=path.resolve(__dirname,'..','test-output');await fs.mkdir(output,{recursive:true});
  const png=await app.evaluate(()=>global.workspaceTest.capture());await fs.writeFile(path.join(output,'live-agent.png'),Buffer.from(png,'base64'));
  const result={date:'2026-10-29',passed:true,model,goal,artifact:artifact.id,version:artifact.version,modelChecks:artifact.test.checks,acceptanceChecks:acceptance.checks,tools:log.filter(e=>e.kind==='tool_completed').map(e=>e.data.tool),summary:completed.data.summary,learning:{episodeId:episode.id,policyUsed:episode.policy,steps:episode.steps.length,finalPolicy:state.learning.activePolicy}};
  await fs.writeFile(path.join(output,'live-agent.json'),JSON.stringify(result,null,2));console.log(JSON.stringify(result,null,2));
 }finally{await app.close();}
}
main().catch(e=>{console.error(e);process.exitCode=1;});
