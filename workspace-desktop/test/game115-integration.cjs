'use strict';
const {launch}=require('./calibration-integration.cjs');
const fs=require('node:fs/promises'),sync=require('node:fs'),path=require('node:path'),assert=require('node:assert/strict'),cp=require('node:child_process');
const repo=path.resolve(__dirname,'../..'),out=path.resolve(process.env.GAME115_OUTPUT||path.join(repo,'build/game115-ui')),project=path.join(out,'project'),sleep=ms=>new Promise(r=>setTimeout(r,ms));
async function main(){
 Object.assign(process.env,{FRAYNIX_WORKSPACE_PERSISTENT:'0',FRAYNIX_WORKSPACE_JAVA_HEAP_MIB:'512',FRAYNIX_WORKSPACE_JAVA_PROCESSORS:'2'});
 require('../../scripts/Study-Build095.cjs').verify(repo);assert(!sync.existsSync(out),'Keep earlier attempts; choose a fresh output');await fs.mkdir(project,{recursive:true});
 let app,page;const record={source:cp.execFileSync('git',['rev-parse','HEAD'],{cwd:repo,encoding:'utf8'}).trim(),startedAt:new Date().toISOString(),requests:[],errors:[],passed:false,scope:'Actual visible chat, native original renderer/physics, explicit Java route, bounded repairs and restart. No LLM inference claim.'};
 const save=()=>fs.writeFile(path.join(out,'result.json'),JSON.stringify(record,null,2));
 const api=(route,args={})=>app.evaluate((_,x)=>global.workspaceTest.api(x.route,x.args),{route,args});
 const request=(operation,command,args={})=>page.evaluate(x=>window.workspace.request(x),{operation,command,args});
 const capture=async name=>{await fs.writeFile(path.join(out,name),Buffer.from(await app.evaluate(()=>global.workspaceTest.capture()),'base64'));};
 async function open(){({app,page}=await launch(project));page.on('pageerror',e=>record.errors.push(e.message));await app.evaluate(({BrowserWindow})=>{const w=BrowserWindow.getAllWindows().find(w=>w.isVisible());w.setSize(1600,1100);w.setTitle('Fraynix · edition 115 · durable level tasks');});await page.waitForFunction(()=>[...document.querySelector('#model').options].some(o=>o.value==='gpt-oss:20b'),{},{timeout:65000});await page.locator('#model').selectOption('gpt-oss:20b');}
 async function run(prompt,{restart=false,failed=false}={}){
  const previous=new Set((await api('/api/agency043/state')).tasks.map(t=>t.id)),row={prompt,started:Date.now()};record.requests.push(row);await page.waitForFunction(()=>!document.querySelector('#run').disabled,{},{timeout:30000});await page.locator('#goal').fill(prompt);await page.locator('#run').click();
  for(let i=0;i<160;i++){row.task=(await api('/api/agency043/state')).tasks.find(t=>!previous.has(t.id));if(row.task)break;await sleep(150);}assert(row.task,'Chat admitted');row.id=row.task.id;let crashed=false,last='';
  while(Date.now()-row.started<360000){row.task=await api('/api/agency043/read',{id:row.id});row.child=(await api('/api/game-tasks115/list')).tasks.find(t=>t.id===row.id);if(row.task.reason!==last){console.log(row.id.slice(0,8)+': '+row.task.status+' '+row.task.reason);last=row.task.reason;}await save();
   if(restart&&!crashed&&row.child?.stage==='trial'&&row.child.trials.at(-1).inputChunks>=2){row.beforeCrash=row.child;row.crash=await app.evaluate(()=>global.workspaceTest.crashEngine());await sleep(500);await app.close().catch(()=>{});app=null;await open();const restored=await api('/api/game-tasks115/read',{id:row.id});assert.equal(restored.status,'interrupted');row.restored=restored;await page.locator('#mission044-open').click();await page.locator('#mission044-task').selectOption(row.id);await page.waitForFunction(()=>!document.querySelector('#mission044-resume').disabled);await page.locator('#mission044-resume').click();await page.locator('#mission044-activity').click();crashed=true;continue;}
   if(['completed','interrupted','cancelled'].includes(row.task.status)){row.elapsedMs=Date.now()-row.started;if(failed){assert.equal(row.child.status,'failed');assert.equal(row.task.status,'interrupted');assert.equal(row.task.timing052.modelCalls,0);await request('agency043','control',{id:row.id,action:'cancel'});}else{assert.equal(row.task.status,'completed',row.task.reason);row.checked=await request('gameTasks115','verify',{id:row.id});assert(row.checked.audit.passed);assert.equal(row.task.timing052.modelCalls,0);}break;}
   await sleep(100);
  }
  assert(row.elapsedMs,'Bounded task timeout');if(restart){assert(crashed);assert.notEqual(row.child.trials[0].sessionId,row.child.trials[1].sessionId);assert.equal(row.child.trials[0].status,'interrupted');}await save();return row;
 }
 try{
  await open();await page.locator('#game-tab').click();await page.waitForFunction(()=>document.querySelector('#game-status').textContent.includes('tick'));
  const repaired=await run('Create a playable Mario level gapTiles=10 repairLimit=2; construct, playtest and repair it.');assert.equal(repaired.checked.audit.repairCount,2);await capture('chat-level-completed.png');
  await page.locator('#game-task-history > summary').click();await page.locator('#game-task-refresh').click();await page.waitForFunction(()=>document.querySelector('#game-task-detail').textContent.includes('Observed fall'));await capture('retained-repairs.png');await page.locator('#game-task-history > summary').click();
  const recovered=await run('Create a playable Mario level seed=3.',{restart:true});await page.locator('#game-tab').click();await capture('recovered-level.png');
  await run('Create a playable Mario level gapTiles=10 without repairs.',{failed:true});
  await request('gameRuntime','open-level',{id:recovered.checked.audit.candidateId});const overview=await request('gameRuntime','command',{action:'overview'});await fs.writeFile(path.join(out,'actual-reference.png'),Buffer.from(overview.pngBase64,'base64'));record.referenceFrame={...overview};delete record.referenceFrame.pngBase64;
  await page.locator('#game-level-builder > summary').click();await page.locator('#game-reference-file').setInputFiles(path.join(out,'actual-reference.png'));await page.locator('#game-reference-import').click();await page.waitForFunction(()=>!document.querySelector('#game-reference-chat').disabled,{},{timeout:60000});await capture('reference-analysis.png');await page.locator('#game-reference-chat').click();const text=await page.locator('#goal').inputValue();const reconstructed=await run(text);assert(reconstructed.child.reference.accepted);assert.notEqual(reconstructed.checked.audit.candidateId,recovered.checked.audit.candidateId);await capture('reference-playtested.png');
  await app.evaluate(({BrowserWindow})=>BrowserWindow.getAllWindows().find(w=>w.isVisible()).setSize(1100,1000));await sleep(200);await capture('level-1100.png');assert(await page.locator('#game-frame').isVisible());
  assert.deepEqual(record.errors,[]);record.passed=true;await save();
 }finally{if(app)await app.close().catch(()=>{});record.finishedAt=new Date().toISOString();await save();}
 console.log(JSON.stringify({passed:true,requests:record.requests.map(r=>({id:r.id,status:r.task.status,elapsedMs:r.elapsedMs}))}));
}
main().catch(e=>{console.error(e);process.exitCode=1;});
