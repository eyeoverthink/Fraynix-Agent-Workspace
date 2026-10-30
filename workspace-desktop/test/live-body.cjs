'use strict';
const {launch}=require('./calibration-integration.cjs'),fs=require('node:fs/promises'),path=require('node:path'),os=require('node:os'),assert=require('node:assert/strict');
async function main(){
 const project=await fs.mkdtemp(path.join(os.tmpdir(),'fraynix-live-body-')),model=process.env.FRAYNIX_LIVE_MODEL||'gpt-oss:20b',output=path.resolve(__dirname,'../test-output');await fs.mkdir(output,{recursive:true});const {app,page}=await launch(project);
 const api=(route,args)=>app.evaluate((_,x)=>global.workspaceTest.api(x.route,x.args),{route,args});
 try{
  const scene=await api('/api/scene/save',{title:'Local agent physical manipulation',parameters:{angleDeg:0,friction:0}});
  await page.waitForFunction(m=>[...document.querySelector('#model').options].some(o=>o.value===m),model,{timeout:60000});
  const goal='In saved native 3D scene '+scene.id+', use the physical body tools: grab the visible dynamic Block, move the BODY once by +0.75 metres in world Y and +0.25 metres in world Z, preserving world X. Use its actual grabbed position as the starting point. Release it, capture the actual final native view, then finish with measured target error and release result. Do not move the observer or create another scene.';
  await page.getByLabel('Local model').selectOption(model);await page.getByLabel('Agent request').fill(goal);await page.getByRole('button',{name:'Start task',exact:false}).click();
  for(let i=0;i<240;i++){await new Promise(r=>setTimeout(r,2000));if(i%15===0)console.log(await page.locator('#run-state').innerText());if(!await page.locator('#stop').isEnabled())break;if(i===239){await app.evaluate(()=>global.workspaceTest.stopAgent());throw Error('Local body task exceeded eight minutes');}}
  const events=(await fs.readFile(path.join(project,'events.jsonl'),'utf8')).trim().split('\n').map(JSON.parse),completed=events.findLast(e=>e.kind==='agent_completed');
  assert(completed,'Local model did not complete: '+await page.locator('#messages').innerText());
  const runs=(await api('/api/experiment/list',{sceneId:scene.id})).runs;assert.equal(runs.length,1);const run=await api('/api/experiment/read',{id:runs[0].id});assert.equal(run.scope,'manipulation');assert.equal(run.status,'completed');assert.equal(run.actuatorEvents.length,3);
  const [grab,move,release]=run.actuatorEvents;assert.deepEqual(run.actuatorEvents.map(e=>e.action),['grab','move','release']);
  assert.equal(move.reached,true);assert(move.targetErrorM<=.08);assert(Math.abs(move.target[0]-grab.after.position[0])<1e-8);assert(Math.abs(move.target[1]-grab.after.position[1]-.75)<1e-6);assert(Math.abs(move.target[2]-grab.after.position[2]-.25)<1e-6);
  assert.deepEqual(release.velocityAtConstraintRemoval,release.before.velocity);assert(!release.after.grip.held);assert(release.after.position[1]<release.before.position[1]-.25);
  assert(completed.data.summary.includes('Measured target error '+move.targetErrorM.toPrecision(6)+' m'));
  assert(!completed.data.summary.includes('Target error: 0.790569'));
  assert.equal((await api('/api/scene/read',{id:scene.id})).version,1);const observers=(await api('/api/observer/list',{})).observers;assert.equal(observers.length,1);assert.equal(observers[0].actionCount,0);
  const history=await api('/api/observer/history',{id:observers[0].id});assert.equal(history.captures.length,1);const replay=await api('/api/observer/replay',{id:history.captures[0].id});assert.equal(replay.packet.world.runId,run.id);assert.equal(replay.packet.world.tick,run.final.tick);assert(!replay.packet.world.grip.held);
  const state=await api('/api/state'),episode=state.learning.episodes.find(e=>e.id===completed.data.episodeId);assert.equal(episode.status,'completed-unverified');const shell=await page.evaluate(()=>window.workspace.request({operation:'state'})),admission=shell.log.find(e=>e.kind==='model')?.data;assert.match(admission.digest,/^[a-f0-9]{64}$/i);
  await page.locator('#scene-stage').scrollIntoViewIfNeeded();await fs.writeFile(path.join(output,'live-body.png'),Buffer.from(await app.evaluate(()=>global.workspaceTest.capture()),'base64'));
  const checks=['Actual local model completed one native grab/move/release task','Exactly one body movement has requested +0.75 m Y/+0.25 m Z and unchanged X','Native target error is within 0.08 m; dynamic collisions and constraint limits remain active','Release preserves velocity and actual gravity reduces body height; no constraint remains','Authored scene/camera unchanged; displayed numerical report comes from actual native receipts','Saved final actual PNG binds the same native run/final tick; model digest and unverified physical-learning status retained'];
  const result={date:new Date().toISOString(),passed:true,model,admittedModel:admission,goal,checks,run,capture:history.captures[0],packet:replay.packet,summary:completed.data.summary,steps:episode.steps,scope:'One actual local-language-model physical actuation task. No learned body policy, model-weight training or general robot/mesh support claimed.'};
  await fs.writeFile(path.join(output,'live-body.json'),JSON.stringify(result,null,2));console.log(JSON.stringify({passed:true,model,checks,targetErrorM:move.targetErrorM,tools:episode.steps.map(s=>s.tool)},null,2));
 }catch(e){await fs.writeFile(path.join(output,'live-body-failure.txt'),await page.locator('body').innerText());throw e;}finally{await app.close();}
}
main().catch(e=>{console.error(e);process.exitCode=1;});
