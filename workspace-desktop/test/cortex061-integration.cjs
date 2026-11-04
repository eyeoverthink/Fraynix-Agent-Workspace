'use strict';
const {launch}=require('./calibration-integration.cjs');
const fs=require('node:fs/promises'),path=require('node:path'),os=require('node:os'),assert=require('node:assert/strict');
const sleep=ms=>new Promise(r=>setTimeout(r,ms));
async function main(){
 process.env.FRAYNIX_WORKSPACE_PERSISTENT='0';process.env.FRAYNIX_WORKSPACE_PLAN_CPU_ONLY='1';
 const prior=process.env.FRAYNIX061_RESUME?JSON.parse(await fs.readFile(process.env.FRAYNIX061_RESUME,'utf8')):null;
 const project=prior?.project||await fs.mkdtemp(path.join(os.tmpdir(),'fraynix-cortex061-ui-')),out=path.resolve(__dirname,'../test-output');await fs.mkdir(out,{recursive:true});
 const record={...(prior||{}),project,source:require('node:child_process').execFileSync('git',['rev-parse','HEAD'],{encoding:'utf8'}).trim(),date:new Date().toISOString(),checks:prior?.checks||[]};
 let app,page;const errors=[];
 const api=(route,args={})=>app.evaluate((_,x)=>global.workspaceTest.api(x.route,x.args),{route,args});
 async function until(f,ms=120000){let end=Date.now()+ms;while(Date.now()<end){const v=await f();if(v)return v;await sleep(100);}throw Error('Expected state timed out');}
 async function capture(name){await sleep(200);await fs.writeFile(path.join(out,'cortex061-'+name+'.png'),Buffer.from(await app.evaluate(()=>global.workspaceTest.capture()),'base64'));}
 async function open(){({app,page}=await launch(project));page.on('pageerror',e=>errors.push(e.message));await page.locator('#brain-open').click();await page.locator('#cortex061').evaluate(e=>e.open=true);}
 try{
  await open();if(!record.job){await page.locator('#c061-train').click();await until(async()=>(await api('/api/brain/state')).fraynixGPT061.available);
  const state=await api('/api/brain/state');record.job=await api('/api/brain/read',{id:state.fraynixGPT061.jobId});assert.equal(record.job.status,'completed');assert.equal(record.job.progress,36);assert.equal(record.job.outcomeEvaluation061.qualified,false);assert.equal(state.activePolicy.version,1);
  await page.locator('#c061-result').scrollIntoViewIfNeeded();await until(()=>page.locator('#c061-result').textContent());await capture('results');record.checks.push('Visible Learn & compare collected all 36 actual native trials and retained the failed qualification');}
  await page.locator('#c061-predict-panel').evaluate(e=>e.open=true);await page.locator('#c061-predict').click();await until(async()=>(await page.locator('#c061-prediction').textContent()).includes('No action executed'));record.prediction=await page.locator('#c061-prediction').textContent();await capture('prediction');
  await page.locator('#c061-angle').fill('90');await page.locator('#c061-predict').click();await until(async()=>(await page.locator('#c061-status').textContent()).includes('angle')||(await page.locator('#notice').textContent()).includes('angle'));record.checks.push('Real prediction operation audits original training/evaluation and rejects out-of-domain angle');
  const facts=await api('/api/agent042/state');assert.equal(facts.fraynixGPT061.modelSha256,record.job.modelSha256061);
  await app.close();app=null;await open();const cold=await api('/api/brain/state');assert.equal(cold.fraynixGPT061.modelSha256,record.job.modelSha256061);assert.equal(cold.activePolicy.version,1);
  await page.locator('#c061-predict-panel').evaluate(e=>e.open=true);await page.locator('#c061-predict').click();await until(async()=>(await page.locator('#c061-prediction').textContent()).includes('No action executed'));assert.equal(await page.locator('#c061-prediction').textContent(),record.prediction);record.checks.push('Cold Java/app restart preserves exact checkpoint and prediction without retraining');
  await app.evaluate(({BrowserWindow})=>BrowserWindow.getAllWindows()[0].setSize(1100,800));await page.locator('#c061-result').scrollIntoViewIfNeeded();await capture('narrow');
  assert(await page.locator('#c061-train').isVisible());record.checks.push('Existing Brain view works at 1100 px with visible controls');
  await app.evaluate(({BrowserWindow})=>BrowserWindow.getAllWindows()[0].setSize(1600,1000));await page.locator('#brain-close').click();await page.waitForFunction(()=>[...document.querySelector('#model').options].some(o=>o.value==='gpt-oss:20b'),{},{timeout:60000});await page.locator('#model').selectOption('gpt-oss:20b');
  const prior=(await api('/api/agency043/state')).tasks.map(t=>t.id);
  await page.getByLabel('Agent request').fill('Inspect FraynixGPT cortex outcome learning. Read the actual Java agent state and create its verified report. Explain that the analytical baseline won and the candidate is unqualified. Do not start training or move objects.');await page.locator('#run').click();
  const task=await until(async()=>(await api('/api/agency043/state')).tasks.find(t=>!prior.includes(t.id)));
  record.chat=await until(async()=>{let t=await api('/api/agency043/read',{id:task.id});return ['completed','interrupted','cancelled'].includes(t.status)?t:null;},480000);
  assert.equal(record.chat.status,'completed',record.chat.reason);assert.equal(record.chat.family044,'agent');assert(record.chat.history.some(h=>h.kind==='plan'));
  assert(record.chat.history.filter(h=>h.kind==='plan').every(h=>h.detail.compute.cpuOnly&&h.detail.compute.options.num_thread===4));
  assert(JSON.stringify(record.chat).includes(record.job.modelSha256061));await capture('chat');record.checks.push('Actual local gpt-oss:20b CPU/four-thread chat reads the saved outcome facts and verifies a report');
  await page.locator('#brain-open').click();await page.locator('#c061-train').click();await until(async()=>!await page.locator('#c061-stop').isDisabled());await page.locator('#c061-stop').click();await until(async()=>!(await api('/api/brain/state')).busy);
  record.stopped=(await api('/api/brain/state')).fraynixGPT061;assert.equal(record.stopped.status,'cancelled');assert.equal((await api('/api/brain/state')).activePolicy.version,1);record.checks.push('Visible Stop cancels fresh native learning; earlier original evidence remains');
  assert.deepEqual(errors,[]);record.passed=true;await fs.writeFile(path.join(out,'cortex061-live.json'),JSON.stringify(record,null,2));console.log(JSON.stringify({passed:true,project,checks:record.checks,evaluation:record.job.outcomeEvaluation061}));
 }catch(e){record.error=e.stack;await fs.writeFile(path.join(out,'cortex061-failed-'+Date.now()+'.json'),JSON.stringify(record,null,2));throw e;}
 finally{if(app)await app.close().catch(()=>{});}
}
main().catch(e=>{console.error(e);process.exitCode=1;});
