'use strict';
const {launch}=require('./calibration-integration.cjs');
const fs=require('node:fs/promises'),sync=require('node:fs'),path=require('node:path'),assert=require('node:assert/strict'),cp=require('node:child_process');
const repo=path.resolve(__dirname,'../..'),out=path.resolve(process.env.GAME123_OUTPUT||path.join(repo,'build/game123-ui')),project=path.join(out,'project'),sleep=ms=>new Promise(r=>setTimeout(r,ms));
async function main(){
 Object.assign(process.env,{FRAYNIX_WORKSPACE_PERSISTENT:'0',FRAYNIX_WORKSPACE_JAVA_HEAP_MIB:'512',FRAYNIX_WORKSPACE_JAVA_PROCESSORS:'2'});
 require('../../scripts/Study-Build095.cjs').verify(repo);assert(!sync.existsSync(out),'Keep earlier attempts; choose a fresh output');await fs.mkdir(project,{recursive:true});
 let app,page;const record={source:cp.execFileSync('git',['rev-parse','HEAD'],{cwd:repo,encoding:'utf8'}).trim(),startedAt:new Date().toISOString(),requests:[],errors:[],passed:false,scope:'Actual visible chat, native original renderer/physics, explicit Java route, bounded repairs and restart. No LLM inference claim.'};
 const save=()=>fs.writeFile(path.join(out,'result.json'),JSON.stringify(record,null,2));
 const api=(route,args={})=>app.evaluate((_,x)=>global.workspaceTest.api(x.route,x.args),{route,args});
 const request=(operation,command,args={})=>page.evaluate(x=>window.workspace.request(x),{operation,command,args});
 const shot=async name=>{await page.evaluate(()=>new Promise(r=>requestAnimationFrame(()=>requestAnimationFrame(r))));await sleep(250);assert(!(await page.locator('#notice').textContent()).includes('current action authority'),'Ownership transition should be explained as waiting');await fs.writeFile(path.join(out,name),Buffer.from(await app.evaluate(()=>global.workspaceTest.capture()),'base64'));};
 const capture=async name=>{
  await page.waitForFunction(()=>{const image=document.querySelector('#game-frame');return image.complete&&image.naturalWidth>0&&image.src.startsWith('data:image/png;base64,')&&document.querySelector('#game-status').textContent.includes('tick 0');},{},{timeout:15000});
  await shot(name);
 };
 async function open(){({app,page}=await launch(project));page.on('pageerror',e=>record.errors.push(e.message));await app.evaluate(({BrowserWindow})=>{const w=BrowserWindow.getAllWindows().find(w=>w.isVisible());w.setSize(1600,1100);w.setTitle('Fraynix - edition 123 - durable level tasks');});await page.waitForFunction(()=>[...document.querySelector('#model').options].some(o=>o.value==='gpt-oss:20b'),{},{timeout:65000});await page.locator('#model').selectOption('gpt-oss:20b');}
 async function run(prompt,{restart=false,failed=false}={}){
  const previous=new Set((await api('/api/agency043/state')).tasks.map(t=>t.id)),row={prompt,started:Date.now()};record.requests.push(row);await page.waitForFunction(()=>!document.querySelector('#run').disabled,{},{timeout:30000});await page.locator('#goal').fill(prompt);await page.locator('#run').click();
  for(let i=0;i<160;i++){row.task=(await api('/api/agency043/state')).tasks.find(t=>!previous.has(t.id));if(row.task)break;await sleep(150);}assert(row.task,'Chat admitted');row.id=row.task.id;let crashed=false,last='';
  while(Date.now()-row.started<360000){row.task=await api('/api/agency043/read',{id:row.id});row.child=(await api('/api/game-tasks115/list')).tasks.find(t=>t.id===row.id);if(row.task.reason!==last){console.log(row.id.slice(0,8)+': '+row.task.status+' '+row.task.reason);last=row.task.reason;}await save();
   if(restart&&!crashed&&row.child?.stage==='trial'&&row.child.trials.at(-1).inputChunks>=2){row.beforeCrash=row.child;row.crash=await app.evaluate(()=>global.workspaceTest.crashEngine());await sleep(500);await app.close().catch(()=>{});app=null;await open();const restored=await api('/api/game-tasks115/read',{id:row.id});assert.equal(restored.status,'interrupted');row.restored=restored;await page.locator('#mission044-open').click();await page.locator('#mission044-task').selectOption(row.id);await page.waitForFunction(()=>!document.querySelector('#mission044-resume').disabled);await page.locator('#mission044-resume').click();await page.locator('#mission044-activity').click();await page.locator('#game-tab').click();crashed=true;continue;}
   if(row.child?.stage==='trial'&&row.child.trials.at(-1)?.control==='existing-ai'){
    const observed=await api('/api/game/frame');
    const co=observed.state?.controller;
    if(!record.waitFrame&&co?.opportunity120?.phase==='WAIT'){record.waitFrame={state:observed.state};await page.waitForFunction(()=>document.querySelector('#game-status').textContent.includes('Pickup wait'),{},{timeout:15000});await shot('live-wait.png');}
    if(!record.returnFrame&&co?.opportunity120?.phase==='REVISIT'){
     record.returnFrame={state:observed.state};await fs.writeFile(path.join(out,'return-native.png'),Buffer.from(observed.pngBase64,'base64'));await page.waitForFunction(()=>document.querySelector('#game-status').textContent.includes('Pickup revisit'),{},{timeout:15000});await shot('return-live.png');
    }
    if(!record.collectionFrame&&co?.opportunity120?.revisitStarts>0&&co?.pickups>0){record.collectionFrame={state:observed.state};await fs.writeFile(path.join(out,'collected-native.png'),Buffer.from(observed.pngBase64,'base64'));await shot('collected-after-return.png');}
    if(!record.riskFrame&&co?.pickupSkips>0){record.riskFrame={state:observed.state};await page.waitForFunction(()=>document.querySelector('#game-status').textContent.includes('Skipped 1'),{},{timeout:15000});await shot('declined-pickup.png');}
   }
   if(['completed','interrupted','cancelled'].includes(row.task.status)){row.elapsedMs=Date.now()-row.started;if(failed){assert.equal(row.child.status,'failed');assert.equal(row.task.status,'interrupted');assert.equal(row.task.timing052.modelCalls,0);await request('agency043','control',{id:row.id,action:'cancel'});}else{assert.equal(row.task.status,'completed',row.task.reason);row.checked=await request('gameTasks115','verify',{id:row.id});assert(row.checked.audit.passed);assert(row.task.summary.includes(row.checked.audit.aiPickups+' power-ups collected, '+row.checked.audit.aiDamage+' damage events'));assert.equal(row.checked.review.state.tick,0);assert.equal(row.checked.review.state.player.won,false);assert(['won','failed'].includes(row.checked.audit.existingAI));assert.equal(row.task.timing052.modelCalls,0);}break;}
   await sleep(100);
  }
  assert(row.elapsedMs,'Bounded task timeout');if(restart){assert(crashed);assert.notEqual(row.child.trials[0].sessionId,row.child.trials[1].sessionId);assert.equal(row.child.trials[0].status,'interrupted');}await save();return row;
 }
 try{
  await open();record.self=await api('/api/self/read',{query:'game'});assert(JSON.stringify(record.self).includes('pickupStrategy=full/no-wait/no-return/direct'),'Agent capability list exposes current pickup choices');await page.locator('#game-tab').click();await page.waitForFunction(()=>document.querySelector('#game-status').textContent.includes('tick'));
  const cases=[['return','full',1,true,'return-full.png'],['return','no-return',0,false,'return-disabled.png'],['turtle','full',1,false,'wait-full.png'],['turtle','direct',0,false,'wait-disabled.png']];let full,last;
  for(const [choice,strategy,pickups,restart,file] of cases){
   if(!await page.locator('#game-level-builder').evaluate(e=>e.open))await page.locator('#game-level-builder > summary').click();
   await page.locator('#game-challenge-choice').selectOption(choice);await page.locator('#game-pickup-strategy').selectOption(strategy);if(!record.controlsCaptured){await capture('pickup-controls.png');record.controlsCaptured=true;}
   await page.locator('#game-challenge-chat').click();const prompt=await page.locator('#goal').inputValue();assert(prompt.includes('pickupStrategy='+strategy));last=await run(prompt,{restart});if(!full)full=last;
   const a=last.checked.audit;assert.equal(a.pickupStrategy,strategy);assert.equal(a.existingAI,'won');assert.equal(a.aiPickups,pickups);assert.equal(a.aiDamage,0);assert.equal(a.completionControl,strategy==='full'?'contact122':'contact123-'+strategy);
   if(strategy==='no-return'||strategy==='direct')assert.equal(a.aiOpportunity.revisitStarts,0);if(strategy==='direct')assert.equal(a.aiOpportunity.waitStarts,0);if(choice==='turtle'&&strategy==='full')assert(a.aiOpportunity.waitClears>0);await capture(file);
  }
  await page.locator('#game-task-history > summary').click();await page.locator('#game-task-refresh').click();await page.waitForFunction(()=>document.querySelector('#game-task-choice').options.length===4);assert.equal(await page.locator('#game-task-choice').inputValue(),last.id);await page.waitForFunction(()=>document.querySelector('#game-task-detail').textContent.includes('pickupStrategy'));await capture('verified-strategy.png');await page.locator('#game-task-history > summary').click();
  const skipped=await run('Create a playable Mario level seed=50 lengthTiles=72 gapTiles=3 secondGapTiles=2 pickupTile=18 goombaTile=28 obstacleTile=25 obstacleHeight=2 repairLimit=0 pickupStrategy=full');assert.equal(skipped.checked.audit.aiPickups,0);assert.equal(skipped.checked.audit.aiPickupSkips,1);await capture('unsupported-pickup.png');
  await run('Create a playable Mario level gapTiles=10 pickupTile=17 goombaTile=45 pickupStrategy=direct.',{failed:true});await shot('failed-course.png');
  await request('gameRuntime','open-level',{id:full.checked.audit.candidateId});await app.evaluate(({BrowserWindow})=>BrowserWindow.getAllWindows().find(w=>w.isVisible()).setSize(1100,1000));await sleep(250);await capture('pickup-1100.png');
  await page.locator('#game-ai').click();await sleep(250);record.manualPlay=await api('/api/game/frame');delete record.manualPlay.pngBase64;assert.equal(record.manualPlay.state.controller.policy,'contact122');await page.locator('#game-pause').click();
  assert(record.waitFrame);assert(record.returnFrame);assert(record.collectionFrame);assert(record.riskFrame);assert.deepEqual(record.errors,[]);record.passed=true;await save();
 }finally{if(app)await app.close().catch(()=>{});record.finishedAt=new Date().toISOString();await save();}
 console.log(JSON.stringify({passed:true,requests:record.requests.map(r=>({id:r.id,status:r.task.status,elapsedMs:r.elapsedMs}))}));
}
main().catch(e=>{console.error(e);process.exitCode=1;});
