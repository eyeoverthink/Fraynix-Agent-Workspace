'use strict';
const {launch}=require('./calibration-integration.cjs'),fs=require('node:fs/promises'),path=require('node:path'),assert=require('node:assert/strict');
async function main(){
 process.env.FRAYNIX_WORKSPACE_PERSISTENT='0';
 const out=path.resolve(__dirname,'../test-output'),record=JSON.parse(await fs.readFile(path.join(out,'chat052-live.json'),'utf8'));assert(record.passed);assert(path.basename(record.project).startsWith('fraynix-chat052-'));
 const {app,page}=await launch(record.project),errors=[];page.on('pageerror',e=>errors.push(e.message));
 const api=(route,args={})=>app.evaluate((_,x)=>global.workspaceTest.api(x.route,x.args),{route,args});
 async function paint(){await page.evaluate(()=>new Promise(r=>requestAnimationFrame(()=>requestAnimationFrame(r))));}
 async function capture(name){await paint();await fs.writeFile(path.join(out,name),Buffer.from(await app.evaluate(()=>global.workspaceTest.capture()),'base64'));}
 try{
  await page.locator('#spatial-tab').click();await page.locator('#construction051').evaluate(e=>e.open=true);await page.waitForFunction(()=>document.querySelector('#c051-world').options.length>1);await page.locator('#c051-world').selectOption(record.worldId);
  await page.locator('#mission044-open').click();await page.locator('#mission044-task').selectOption(record.liveStart.id);await page.waitForFunction(id=>document.querySelector('#mission044').dataset.taskId===id,record.liveStart.id);
  assert.match(await page.locator('#mission052-outcome').innerText(),/World revision 4 saved/);assert.match(await page.locator('#mission044-facts').innerText(),/Model requests: 1/);
  await page.locator('#mission044-result').click();await page.waitForFunction(()=>document.querySelector('#preview-tab').classList.contains('selected'));await page.locator('#mission044').evaluate(e=>e.scrollTop=0);await capture('chat052-mission.png');
  await page.locator('#mission052-details').evaluate(e=>{e.open=true;e.scrollIntoView({block:'start'});});await paint();assert(await page.locator('#mission052-evidence').isVisible());await capture('chat052-evidence.png');
  assert.notDeepEqual(await fs.readFile(path.join(out,'chat052-mission.png')),await fs.readFile(path.join(out,'chat052-evidence.png')));
  await page.locator('#mission052-details').evaluate(e=>e.open=false);await page.locator('#mission044').evaluate(e=>e.scrollTop=0);
  await app.evaluate(({BrowserWindow})=>BrowserWindow.getAllWindows()[0].setSize(1100,920));await capture('chat052-narrow.png');assert(await page.evaluate(()=>document.documentElement.scrollWidth<=innerWidth));
  await page.locator('#mission044-task').selectOption(record.liveCounter.id);await page.waitForFunction(id=>document.querySelector('#mission044').dataset.taskId===id,record.liveCounter.id);await page.locator('#mission044-result').click();
  await page.waitForFunction(()=>document.querySelector('#preview-tab').classList.contains('selected'));assert.equal(await app.evaluate(()=>global.workspaceTest.canvasRead('document.querySelector("#count").textContent')),'0');await capture('chat052-counter.png');
  const restored=await api('/api/agency043/read',{id:record.liveCounter.id});assert.equal(restored.outcome,'verified');assert.deepEqual(restored.timing052,record.liveCounter.timing052);assert.deepEqual(errors,[]);
  const result={passed:true,date:new Date().toISOString(),source:require('node:child_process').execFileSync('git',['rev-parse','HEAD'],{encoding:'utf8'}).trim(),project:record.project,checks:['Past construction Mission restores exact verified result and measured one-call timing','Expanded evidence is actually visible and produces a distinct captured frame','1100-pixel integrated layout has no page overflow','Saved counter result reopens after cold restart without new inference'],errors};await fs.writeFile(path.join(out,'chat052-review.json'),JSON.stringify(result,null,2));console.log(JSON.stringify(result));
 }finally{await app.close();}
}
main().catch(e=>{console.error(e);process.exitCode=1;});
