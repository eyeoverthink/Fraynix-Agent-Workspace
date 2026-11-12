'use strict';
const {launch}=require('./calibration-integration.cjs');
const fs=require('node:fs/promises'),sync=require('node:fs'),path=require('node:path'),assert=require('node:assert/strict'),cp=require('node:child_process');
const repo=path.resolve(__dirname,'../..'),out=path.resolve(process.env.GAME124_OUTPUT||path.join(repo,'build/game124-ui')),resume=process.env.GAME124_RESUME?path.resolve(process.env.GAME124_RESUME):null,project=path.join(resume||out,'project'),sleep=ms=>new Promise(r=>setTimeout(r,ms));
async function main(){
 Object.assign(process.env,{FRAYNIX_WORKSPACE_PERSISTENT:'0',FRAYNIX_WORKSPACE_JAVA_HEAP_MIB:'512',FRAYNIX_WORKSPACE_JAVA_PROCESSORS:'2'});
 require('../../scripts/Study-Build095.cjs').verify(repo);assert(!sync.existsSync(out),'Keep earlier attempts; choose a fresh output');await fs.mkdir(project,{recursive:true});await fs.mkdir(out,{recursive:true});
 let app,page;const record={source:cp.execFileSync('git',['rev-parse','HEAD'],{cwd:repo,encoding:'utf8'}).trim(),startedAt:new Date().toISOString(),requests:[],errors:[],passed:false,scope:'Actual visible chat, native original renderer/physics, explicit Java route, bounded repairs and restart. No LLM inference claim.'};
 if(resume){const previous=JSON.parse(await fs.readFile(path.join(resume,'result.json'),'utf8'));assert(previous.requests.length>=9);assert.deepEqual(previous.errors,[]);assert(previous.requests.every(r=>r.checked||r.task?.status==='interrupted'));record.requests=previous.requests;for(const k of ['returnFrame','collectionFrame','waitFrame','riskFrame'])if(previous[k])record[k]=previous[k];record.continuation={source:previous.source,startedAt:previous.startedAt,parentResultSha256:require('node:crypto').createHash('sha256').update(await fs.readFile(path.join(resume,'result.json'))).digest('hex'),parentDirectory:resume,reason:'Retain the original 30-second abstention and verify a larger explicit allowance; no calibration observations discarded.'};for(const name of await fs.readdir(resume))if(name.endsWith('.png'))await fs.copyFile(path.join(resume,name),path.join(out,name));}
 const save=()=>fs.writeFile(path.join(out,'result.json'),JSON.stringify(record,null,2));
 const api=(route,args={})=>app.evaluate((_,x)=>global.workspaceTest.api(x.route,x.args),{route,args});
 const request=(operation,command,args={})=>page.evaluate(x=>window.workspace.request(x),{operation,command,args});
 const shot=async name=>{await page.evaluate(()=>new Promise(r=>requestAnimationFrame(()=>requestAnimationFrame(r))));await sleep(250);assert(!(await page.locator('#notice').textContent()).includes('current action authority'),'Ownership transition should be explained as waiting');await fs.writeFile(path.join(out,name),Buffer.from(await app.evaluate(()=>global.workspaceTest.capture()),'base64'));};
 const capture=async name=>{
  await page.waitForFunction(()=>{const image=document.querySelector('#game-frame');return image.complete&&image.naturalWidth>0&&image.src.startsWith('data:image/png;base64,')&&document.querySelector('#game-status').textContent.includes('tick 0');},{},{timeout:15000});
  await shot(name);
 };
 async function open(){({app,page}=await launch(project));page.on('pageerror',e=>record.errors.push(e.message));await app.evaluate(({BrowserWindow})=>{const w=BrowserWindow.getAllWindows().find(w=>w.isVisible());w.setSize(1600,1100);w.setTitle('Fraynix - edition 124 - durable level tasks');});await page.waitForFunction(()=>[...document.querySelector('#model').options].some(o=>o.value==='gpt-oss:20b'),{},{timeout:65000});await page.locator('#model').selectOption('gpt-oss:20b');}
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
  const geometry='seed=127 lengthTiles=72 gapTiles=2 secondGapTiles=2 pickupTile=42 goombaTile=46 obstacleTile=60 obstacleHeight=1 repairLimit=0';
  const prompt=strategy=>'Create a playable Mario level '+geometry+' pickupStrategy='+strategy;
  if(!resume)for(let repeat=0;repeat<3;repeat++)for(const strategy of (repeat%2?['full','direct']:['direct','full'])){const row=await run(prompt(strategy));assert.equal(row.checked.audit.aiPickups,strategy==='full'?1:0);assert(row.checked.pickupChoice124.eligible);}
  let generous;
  for(const allowance of [0,4,30,120]){
   if(!await page.locator('#game-level-builder').evaluate(e=>e.open))await page.locator('#game-level-builder > summary').click();
   await page.locator('#game-challenge-choice').selectOption('return');await page.locator('#game-pickup-strategy').selectOption('budget');await page.locator('#game-pickup-budget').fill(String(allowance));
   if(allowance===120)await capture('budget-controls.png');await page.locator('#game-challenge-chat').click();const text=await page.locator('#goal').inputValue();assert(text.includes('pickupBudgetSeconds='+allowance));let row=record.requests.find(r=>r.checked?.pickupChoice124?.decision?.result?.allowance===allowance);if(row){row.checked=await request('gameTasks115','verify',{id:row.id});await request('gameRuntime','open-level',{id:row.checked.audit.candidateId});}else row=await run(text,{restart:allowance===120});const d=row.checked.pickupChoice124.decision.result;
   assert(row.task.summary.includes('Budget choice: '+d.selected));assert.equal(d.allowance,allowance);if(allowance===0){assert.equal(d.selected,'direct');assert.equal(row.checked.pickupChoice124.decision.sources.length,0);}const qualified=d.alternatives.filter(o=>Number.isFinite(o.requiredSecondsPerPickup)&&o.requiredSecondsPerPickup<=allowance&&o.benefitLower>0).sort((a,b)=>a.requiredSecondsPerPickup-b.requiredSecondsPerPickup);assert.equal(d.selected,qualified[0]?.strategy||'direct');assert.equal(row.checked.audit.aiPickups,d.selected==='full'?1:0);if(allowance===120){generous=row;assert.equal(row.checked.pickupChoice124.eligible,false);}
   await page.locator('#game-task-history').evaluate(e=>e.open=true);await page.locator('#game-task-refresh').click();await page.waitForFunction(id=>[...document.querySelector('#game-task-choice').options].some(o=>o.value===id),row.id);await page.locator('#game-task-choice').selectOption(row.id);await page.waitForFunction(()=>!document.querySelector('#game-pickup-choice').hidden);await page.locator('#game-pickup-choice-summary').scrollIntoViewIfNeeded();await shot('budget-'+allowance+'.png');await page.locator('#game-task-history').evaluate(e=>e.open=false);
  }
  const unknown=await run(prompt('budget pickupBudgetSeconds=30').replace('seed=127','seed=319'));assert.equal(unknown.checked.pickupChoice124.decision.result.selected,'direct');await capture('unknown-scene.png');
  await run('Create a playable Mario level gapTiles=10 pickupTile=17 goombaTile=45 pickupStrategy=budget pickupBudgetSeconds=30.',{failed:true});await shot('failed-course.png');
  await request('gameTasks115','verify',{id:generous.id});await request('gameRuntime','open-level',{id:generous.checked.audit.candidateId});
  await app.evaluate(({BrowserWindow})=>BrowserWindow.getAllWindows().find(w=>w.isVisible()).setSize(1100,1000));await sleep(250);await capture('budget-1100.png');
  assert(record.returnFrame);assert(record.collectionFrame);assert.deepEqual(record.errors,[]);record.passed=true;await save();
 }finally{if(app)await app.close().catch(()=>{});record.finishedAt=new Date().toISOString();await save();}
 console.log(JSON.stringify({passed:true,requests:record.requests.map(r=>({id:r.id,status:r.task.status,elapsedMs:r.elapsedMs}))}));
}
main().catch(e=>{console.error(e);process.exitCode=1;});
