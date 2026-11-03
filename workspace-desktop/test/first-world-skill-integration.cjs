'use strict';
const assert=require('node:assert/strict');
const fs=require('node:fs/promises');
const os=require('node:os');
const path=require('node:path');
const {launch}=require('./calibration-integration.cjs');

const sleep=ms=>new Promise(resolve=>setTimeout(resolve,ms));
async function until(predicate,timeout=150000){
 const deadline=Date.now()+timeout;
 while(Date.now()<deadline){
  const value=await predicate();
  if(value)return value;
  await sleep(200);
 }
 throw Error('Practice workflow did not reach the expected state');
}

async function exercise(output,cancel){
 const project=await fs.mkdtemp(path.join(os.tmpdir(),cancel?'fraynix-047-stop-':'fraynix-047-first-'));
 const errors=[];
 let app,page;
 async function open(){
  ({app,page}=await launch(project));
  page.on('pageerror',error=>errors.push(error.message));
  await page.waitForFunction(()=>document.querySelector('#engine-state').textContent.includes('Java engine online'));
 }
 const api=(route,args={})=>app.evaluate((_,packet)=>global.workspaceTest.api(packet.route,packet.args),{route,args});
 async function capture(name){
  await page.evaluate(()=>new Promise(resolve=>requestAnimationFrame(()=>requestAnimationFrame(resolve))));
  const png=Buffer.from(await app.evaluate(()=>global.workspaceTest.capture()),'base64');
  await fs.writeFile(path.join(output,name),png);
 }
 const result={project};
 try{
  await open();
  await page.locator('#skills-tab').click();
  await page.locator('#first-world-library').waitFor();
  assert.match(await page.locator('#first-world-library').innerText(),/Create first world skill/);
  if(cancel){
   await page.locator('#spatial-tab').click();
   await page.locator('#persistent-world-panel > summary').click();
   await page.locator('#graph046-open').evaluate(button=>button.scrollIntoView({block:'center'}));
   await page.locator('#graph046-open').click({timeout:8000});
   await page.locator('#graph046-dialog').waitFor();
   assert(await page.locator('#graph046-run').isDisabled());
   await page.locator('#graph047-first-skill').click();
  }else await page.locator('#first-world-library').click();
  await page.locator('#first-world-guide').waitFor({state:'visible'});
  assert.equal((await api('/api/world/list')).worlds.length,0);
  if(!cancel)await capture('first-world-walkthrough.png');
  await page.locator('#first-world-start').click();
  const admission=await until(async()=>(await api('/api/goals/list')).goals.at(-1));
  result.admittedId=admission.id;
  const worldId=admission.worldBinding.worldId;
  const settled=()=>until(async()=>{
   const goal=await api('/api/goals/read',{id:admission.id});
   return ['completed','interrupted','cancelled'].includes(goal.status)?goal:null;
  });
  if(cancel){
   await page.locator('#java-goal-cancel').click();
   const stopped=await settled();
   assert.equal(stopped.status,'cancelled');
   assert.equal((await api('/api/world/observe',{worldId})).expectedRevision,0);
   assert(!stopped.savedSkill);
   assert(await page.locator('#first-world-connect').isHidden());
   assert.deepEqual(errors,[]);
   return {...result,cancelled:true,revision:0};
  }
  const goal=await settled();
  assert.equal(goal.status,'completed',goal.reason);
  assert.equal(goal.savedSkill.name,'first-world-skill');
  assert(goal.metrics.releases>=2);
  assert.equal(goal.worldCommit.revision,1);
  assert(goal.report.id);
  assert.equal((await api('/api/world/list')).worlds.length,1);
  assert.equal((await api('/api/goals/list')).goals.length,1);
  await page.locator('#first-world-resume-guide').click();
  await page.locator('#first-world-connect').waitFor({state:'visible'});
  await capture('first-world-verified.png');
  await page.locator('#first-world-report').click();
  await page.waitForFunction(()=>!document.querySelector('#canvas-surface').hidden);
  await until(()=>app.evaluate((_,id)=>global.workspaceTest.canvasRead(
   'location.pathname.includes('+JSON.stringify('/artifact/'+id+'/')+') && document.body.innerText.length > 80'
  ),goal.report.id));
  await capture('first-world-report.png');
  await page.locator('#first-world-resume-guide').click();
  await page.locator('#first-world-connect').click();
  await page.locator('#graph046-dialog').waitFor();
  assert.equal(await page.locator('#graph046-world').inputValue(),worldId);
  const definition=JSON.parse(await page.locator('#graph046-json').inputValue());
  assert.equal(definition.nodes.length,3);
  for(const node of definition.nodes.slice(0,2))assert.equal(node.skill.name,goal.savedSkill.name);
  assert.notDeepEqual(definition.nodes[0].targets,definition.nodes[1].targets);
  await capture('first-world-connected.png');
  await page.locator('#graph046-name').fill('first-world-chain');
  await page.locator('#graph046-run').click();
  const task=await until(async()=>(await api('/api/agency043/state')).tasks.at(-1));
  const completed=await until(async()=>{
   const read=await api('/api/agency043/read',{id:task.id});
   return ['completed','interrupted','cancelled'].includes(read.status)?read:null;
  });
  assert.equal(completed.status,'completed',completed.reason);
  const final=await api('/api/world/observe',{worldId});
  assert.equal(final.expectedRevision,3);
  await app.close();
  app=null;
  await open();
  await page.locator('#skills-tab').click();
  await page.locator('#library-items').getByText('first-world-skill',{exact:true}).waitFor();
  await page.locator('#library-items').getByText('first-world-chain',{exact:true}).waitFor();
  assert.equal((await api('/api/world/observe',{worldId})).expectedHash,final.expectedHash);
  assert.deepEqual(errors,[]);
  return {...result,goal,chainId:completed.id,finalRevision:final.expectedRevision,finalHash:final.expectedHash,reopened:true};
 }catch(error){
  console.error('Project:',project,'Notice:',await page?.locator('#notice').innerText({timeout:2000}).catch(()=>''),'Renderer:',errors);
  if(app&&page)await capture(cancel?'first-world-stop-failed.png':'first-world-failed.png').catch(()=>{});
  throw error;
 }finally{if(app)await app.close();}
}

async function main(){
 process.env.FRAYNIX_WORKSPACE_PERSISTENT='0';
 const output=path.resolve(__dirname,'../test-output');
 await fs.mkdir(output,{recursive:true});
 const stopped=await exercise(output,true);
 console.log('Stopped practice run left revision 0 and earned no skill');
 const completed=await exercise(output,false);
 await fs.writeFile(path.join(output,'first-world-skill-integration.json'),JSON.stringify({stopped,completed},null,2));
 console.log('Practice skill verified; graph reached revision 3; both skills and the head survived reopen');
}
main().catch(error=>{console.error(error);process.exitCode=1;});
