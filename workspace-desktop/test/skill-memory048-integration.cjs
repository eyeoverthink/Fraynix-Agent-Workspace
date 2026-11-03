'use strict';
const assert=require('node:assert/strict');
const fs=require('node:fs/promises');
const os=require('node:os');
const path=require('node:path');
const {launch}=require('./calibration-integration.cjs');

async function main(){
 process.env.FRAYNIX_WORKSPACE_PERSISTENT='0';
 const project=await fs.mkdtemp(path.join(os.tmpdir(),'fraynix-048-memory-'));
 const checkpoint=path.join(project,'brain','skill-recall-048.bin');
 const output=path.resolve(__dirname,'../test-output');
 await fs.mkdir(output,{recursive:true});
 let app,page;
 const errors=[];
 async function open(location=project){
  ({app,page}=await launch(location));
  page.on('pageerror',error=>errors.push(error.message));
  await page.waitForFunction(()=>document.querySelector('#engine-state').textContent.includes('Java engine online'));
 }
 const api=(route,args={})=>app.evaluate((_,packet)=>global.workspaceTest.api(packet.route,packet.args),{route,args});
 async function memoryPanel(){
  await page.locator('#brain-open').click();
  const panel=page.locator('#skill-memory-panel');
  if(await panel.getAttribute('open')===null)await panel.locator('summary').click();
  await page.waitForFunction(()=>document.querySelector('#skill-memory-state').textContent!=='Open to load saved world skills.');
 }
 const phrase=pageSelector=>'#skill-memory-'+pageSelector;
 async function enter(text,command){
  await page.locator(phrase('phrase')).fill(text);
  await page.locator(phrase(command)).click();
  await page.waitForFunction(()=>document.querySelector('#skill-memory-result').textContent.trim().length>0);
  return page.locator(phrase('result')).innerText();
 }
 async function capture(name){
  await page.evaluate(()=>new Promise(resolve=>requestAnimationFrame(()=>requestAnimationFrame(resolve))));
  const png=Buffer.from(await app.evaluate(()=>global.workspaceTest.capture()),'base64');
  await fs.writeFile(path.join(output,name),png);
 }
 const state='/api/brain/skill-memory-state';
 const recall='/api/brain/skill-memory-recall';
 const teach='/api/brain/skill-memory-teach';
 try{
  await open();
  const firstBoot=(await api('/api/agent042/state')).bootId;
  assert(firstBoot);
  await memoryPanel();
  assert(await page.locator(phrase('teach')).isDisabled());
  assert.equal((await api(state)).examples,0);
  await assert.rejects(fs.readFile(checkpoint),{code:'ENOENT'});
  await page.locator('#brain-close').click();
  await page.locator('#skills-tab').click();
  await page.locator('#first-world-library').click();
  await page.locator('#first-world-start').click();
  await page.locator('#first-world-resume-guide').waitFor({timeout:120000});
  const beforeGoals=await api('/api/goals/list');
  assert.equal(beforeGoals.goals.length,1);
  const goal=beforeGoals.goals[0];
  assert.equal(goal.status,'completed');
  assert.equal(goal.savedSkill.name,'first-world-skill');
  const worldId=goal.worldBinding.worldId;
  const worldBefore=await api('/api/world/observe',{worldId});

  await memoryPanel();
  const selection=(await api(state)).choices[0];
  assert.equal(selection.name,'first-world-skill');
  assert.equal(selection.id,goal.savedSkill.id);
  await assert.rejects(api(teach,{skill:{...selection,sha256:'f'.repeat(64)},phrase:'foreign source'}),/saved skill changed/i);
  const teaching='arrange the blue crate beside the marker';
  assert.match(await enter(teaching,'teach'),/Phrase saved/);
  assert.match(await enter('arrange the blue crate next to the marker','teach'),/Phrase saved/);
  assert.match(await enter(teaching.toUpperCase()+'!','teach'),/already taught/);
  const trained=await api(state);
  assert.equal(trained.examples,2);
  const snapshot=await fs.readFile(checkpoint);
  assert.match(await enter(teaching,'recall'),/Exact phrase · first-world-skill v1/);
  const query='arange the blue crat beside the marker';
  assert.match(await enter(query,'recall'),/Packed HDC · first-world-skill v1/);
  await page.locator(phrase('result')).scrollIntoViewIfNeeded();
  await capture('skill-memory-048-compare.png');
  const beforeRecall=await api(recall,{phrase:query});
  assert.equal(beforeRecall.exact.status,'abstain');
  assert.equal(beforeRecall.hdc.skill.id,selection.id);
  const unsupported=await api(recall,{phrase:'book a hotel room in Seattle'});
  for(const key of ['exact','lexical','hdc'])assert.equal(unsupported[key].status,'abstain');
  assert.deepEqual(await fs.readFile(checkpoint),snapshot);
  assert.equal((await api('/api/goals/list')).goals.length,1);
  assert.equal((await api('/api/world/observe',{worldId})).expectedHash,worldBefore.expectedHash);

  await app.close();
  app=null;
  await open();
  const secondBoot=(await api('/api/agent042/state')).bootId;
  assert(secondBoot&&secondBoot!==firstBoot);
  await memoryPanel();
  assert.deepEqual(await api(state),trained);
  assert.deepEqual(await api(recall,{phrase:query}),beforeRecall);
  assert.deepEqual(await fs.readFile(checkpoint),snapshot);
  await page.locator(phrase('state')).scrollIntoViewIfNeeded();
  await capture('skill-memory-048-restored.png');
  await app.close();
  app=null;
  // Simulate offline evidence damage in this isolated, closed synthetic project.
  const reportFile=path.join(project,'artifacts',goal.report.id,'versions',String(goal.report.version),'index.html');
  await fs.appendFile(reportFile,'\n<!-- altered synthetic receipt -->\n');
  await open();
  const afterEvidenceEdit=await api(recall,{phrase:teaching});
  assert.equal(afterEvidenceEdit.exact.status,'abstain');
  assert.match(afterEvidenceEdit.exact.reason,/evidence.*changed/i);
  await assert.rejects(api(teach,{skill:selection,phrase:'one more lesson'}),/passing canvas receipt/i);
  assert.deepEqual(await fs.readFile(checkpoint),snapshot);
  await app.close();
  app=null;

  const damaged=await fs.mkdtemp(path.join(os.tmpdir(),'fraynix-048-damaged-'));
  const damagedFile=path.join(damaged,'brain','skill-recall-048.bin');
  await fs.mkdir(path.dirname(damagedFile),{recursive:true});
  const modified=Buffer.from(snapshot);
  modified[modified.length-33]^=1;
  await fs.writeFile(damagedFile,modified);
  await open(damaged);
  await memoryPanel();
  assert.match(await page.locator(phrase('state')).innerText(),/could not be restored/i);
  assert(await page.locator(phrase('recall')).isDisabled());
  await assert.rejects(api(recall,{phrase:query}),/could not be restored/i);
  assert.deepEqual(await fs.readFile(damagedFile),modified);
  assert.deepEqual(errors,[]);
  const receipt={passed:true,project,bootChanged:firstBoot!==secondBoot,originalSkill:selection,trained,
   query,beforeRecall,unsupported,checkpointBytes:snapshot.length,checkpointUnchanged:true,
   worldHeadUnchanged:worldBefore.expectedHash,goalCount:1,staleSkillRejected:true,
   editedReportAbstained:true,damagedCheckpointRetained:true};
  await fs.writeFile(path.join(output,'skill-memory048-integration.json'),JSON.stringify(receipt,null,2));
  console.log('Actual saved world skill taught and recalled, snapshot unchanged across a cold Java restart, corrupt snapshot retained.');
 }catch(error){
  console.error('Project:',project,'Renderer:',errors);
  if(app)await capture('skill-memory-048-failed.png').catch(()=>{});
  throw error;
 }finally{if(app)await app.close();}
}
main().catch(error=>{console.error(error);process.exitCode=1;});
