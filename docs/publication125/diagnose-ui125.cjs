const {launch}=require('../../workspace-desktop/test/calibration-integration.cjs'),fs=require('node:fs/promises'),path=require('node:path');
(async()=>{
 const repo=path.resolve(__dirname,'../..'),prior=JSON.parse(await fs.readFile(path.join(repo,'build/game125-ui-final/result.json'),'utf8')),id=prior.requests[6].id;
 Object.assign(process.env,{FRAYNIX_WORKSPACE_PERSISTENT:'0',FRAYNIX_WORKSPACE_JAVA_HEAP_MIB:'512',FRAYNIX_WORKSPACE_JAVA_PROCESSORS:'2'});
 const {app,page}=await launch(path.join(repo,'build/game125-ui-final/project'));const record={taskId:id,events:[]};page.on('pageerror',e=>record.events.push({error:e.message}));
 try{
  await page.locator('#game-tab').click();await page.locator('#game-task-history').evaluate(e=>e.open=true);await page.locator('#game-task-refresh').click();await page.waitForFunction(x=>[...document.querySelector('#game-task-choice').options].some(o=>o.value===x),id);await page.locator('#game-task-choice').selectOption(id);await page.locator('#game-cost125').scrollIntoViewIfNeeded();await page.locator('#game-cost125-review').click();
  for(let i=0;i<12;i++){await new Promise(r=>setTimeout(r,1000));record.events.push(await page.evaluate(()=>({status:document.querySelector('#game-cost125-status').textContent,selected:document.querySelector('#game-task-choice').value,result:document.querySelector('#game-cost125-result').textContent})));}
  record.backend=await page.evaluate(id=>window.workspace.request({operation:'gameTasks115',command:'review-latest',args:{id}}),id);await fs.writeFile(path.join(repo,'build/game125-ui-diagnostic-after.png'),Buffer.from(await app.evaluate(()=>global.workspaceTest.capture()),'base64'));
  record.passed=record.events.at(-1).status==='Saved review '+record.backend.id;
 }catch(e){record.error=e.message;}finally{await app.close();await fs.writeFile(path.join(repo,'build/game125-ui-diagnostic-after.json'),JSON.stringify(record,null,2));console.log(JSON.stringify({events:record.events,backendId:record.backend?.id,error:record.error}));}
 if(!record.passed)throw Error('Saved review must remain visible after concurrent refreshes');
})().catch(e=>{console.error(e);process.exitCode=1});
