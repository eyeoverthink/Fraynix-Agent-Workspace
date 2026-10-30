'use strict';
const {launch}=require('./calibration-integration.cjs'),fs=require('node:fs/promises'),path=require('node:path'),os=require('node:os'),assert=require('node:assert/strict'),crypto=require('node:crypto');
async function main(){
 const project=await fs.mkdtemp(path.join(os.tmpdir(),'fraynix-live-observer-')),model=process.env.FRAYNIX_LIVE_MODEL||'gpt-oss:20b';
 const {app,page}=await launch(project),output=path.resolve(__dirname,'../test-output');await fs.mkdir(output,{recursive:true});
 const api=(route,args)=>app.evaluate((_,x)=>global.workspaceTest.api(x.route,x.args),{route,args});
 try{
  await page.getByRole('button',{name:'3D Lab',exact:true}).click();
  await page.getByRole('button',{name:'First-person view',exact:true}).waitFor();
  const state=await api('/api/state'),scene=await api('/api/scene/read',{id:state.scenes[0].id});
  const world=await api('/api/observer/open',{sceneId:scene.id});
  const initial=world.pose.position;
  await page.waitForFunction(m=>[...document.querySelector('#model').options].some(o=>o.value===m),model,{timeout:60000});
  const goal='Use the native first-person observer for saved 3D scene '+scene.id+'. Move the observer exactly 1 metre to its right, once. Focus the real Block entity, inspect it, capture the final actual native view, then finish with the actual coordinates, inspected entity id and PNG hash. Do not move any objects or create another scene.';
  await page.getByLabel('Local model').selectOption(model);await page.getByLabel('Agent request').fill(goal);await page.getByRole('button',{name:'Start task',exact:false}).click();
  for(let i=0;i<240;i++){await new Promise(r=>setTimeout(r,2000));if(i%15===0)console.log(await page.locator('#run-state').innerText());if(!await page.locator('#stop').isEnabled())break;if(i===239){await app.evaluate(()=>global.workspaceTest.stopAgent());throw Error('Local observer task exceeded eight minutes');}}
  const events=(await fs.readFile(path.join(project,'events.jsonl'),'utf8')).trim().split('\n').map(JSON.parse),completed=events.findLast(e=>e.kind==='agent_completed');
  assert(completed,'Actual model did not complete: '+await page.locator('#messages').innerText());
  const observer=await api('/api/observer/read',{id:world.id}),history=await api('/api/observer/history',{id:world.id});
  assert.equal((await api('/api/observer/list',{})).observers.length,1);
  assert(Math.abs(observer.pose.position[0]-initial[0]-1)<1e-12);assert.equal(observer.pose.position[1],initial[1]);assert.equal(observer.pose.position[2],initial[2]);
  const moves=history.events.filter(e=>e.action==='move');assert.equal(moves.length,1);
  const inspected=history.events.find(e=>e.action==='inspect');assert(inspected);assert.equal(inspected.inspected.label,'Block');assert.equal(observer.selectedEntityId,inspected.inspected.id);
  assert(history.events.some(e=>e.action==='focus'));
  const saved=history.captures.at(-1);assert(saved);assert.equal(saved.observerVersion,observer.version);
  const replay=await api('/api/observer/replay',{id:saved.id});
  assert.equal(crypto.createHash('sha256').update(Buffer.from(replay.pngBase64,'base64')).digest('hex'),saved.pngSha256);
  assert.equal(replay.packet.world.sceneId,scene.id);assert.equal(replay.packet.world.authoredVersion,1);
  assert.equal((await api('/api/scene/read',{id:scene.id})).version,1);
  const current=await api('/api/state'),episode=current.learning.episodes.find(e=>e.id===completed.data.episodeId);
  assert.equal(episode.status,'completed-unverified');
  const shell=await page.evaluate(()=>window.workspace.request({operation:'state'})),admittedModel=shell.log.find(e=>e.kind==='model')?.data;assert.match(admittedModel.digest,/^[a-f0-9]{64}$/i);
  await page.locator('#scene-stage').scrollIntoViewIfNeeded();await page.waitForTimeout(150);
  await fs.writeFile(path.join(output,'live-observer.png'),Buffer.from(await app.evaluate(()=>global.workspaceTest.capture()),'base64'));
  await fs.writeFile(path.join(output,'live-observer-frame.png'),Buffer.from(replay.pngBase64,'base64'));
  const checks=['Actual local model used native tools and completed','Exactly one observer and one 1 m right movement; other position axes preserved','Focus and inspection identify the real Block entity','Final saved PNG matches independent SHA-256 and current observer pose version','Authored scene and objects remain unchanged','Model digest recorded; observer evidence remains separate from artifact-policy training'];
  const result={date:new Date().toISOString(),passed:true,model,admittedModel,goal,observer,history,capture:saved,packet:replay.packet,tools:episode.steps.map(s=>s.tool),summary:completed.data.summary,checks,learning:{status:episode.status},scope:'One actual local-language-model adapter task. Java performs observer logic; renderer provides actual pixels. No video transformer or model-weight training.'};
  await fs.writeFile(path.join(output,'live-observer.json'),JSON.stringify(result,null,2));console.log(JSON.stringify({passed:true,model,checks,tools:result.tools},null,2));
 }catch(e){await fs.writeFile(path.join(output,'live-observer-failure.txt'),await page.locator('body').innerText());throw e;}finally{await app.close();}
}
main().catch(e=>{console.error(e);process.exitCode=1;});
