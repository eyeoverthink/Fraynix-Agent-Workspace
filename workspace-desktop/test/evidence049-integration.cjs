'use strict';
const assert=require('node:assert/strict'),fs=require('node:fs/promises'),os=require('node:os'),path=require('node:path');
const {launch}=require('./calibration-integration.cjs');
async function main(){
 process.env.FRAYNIX_WORKSPACE_PERSISTENT='0';
 const project=await fs.mkdtemp(path.join(os.tmpdir(),'fraynix-evidence049-'));
 const fixtures=path.resolve(__dirname,'../../examples/evidence-049'),out=path.resolve(__dirname,'../test-output');
 await fs.mkdir(out,{recursive:true});
 let app,page;const errors=[],checks=[];
 const api=(route,args={})=>app.evaluate((_,x)=>global.workspaceTest.api(x.route,x.args),{route,args});
 async function open(){({app,page}=await launch(project));page.on('pageerror',e=>errors.push(e.message));await page.waitForFunction(()=>document.querySelector('#engine-state').textContent.includes('Java engine online'));}
 async function panel(){await page.locator('#brain-open').click();await page.locator('#evidence049-panel > summary').click();await page.waitForFunction(()=>!document.querySelector('#evidence049-file').disabled);}
 async function upload(name){await page.locator('#evidence049-file').setInputFiles(path.join(fixtures,name+'.json'));await page.waitForFunction(()=>document.querySelector('#evidence049-status').textContent.includes('LABELLED SYNTHETIC'));await page.waitForFunction(()=>!document.querySelector('#evidence049-file').disabled);}
 async function seekEnd(){await page.locator('#evidence049-seek').evaluate(e=>{e.value=e.max;e.dispatchEvent(new Event('input',{bubbles:true}));});}
 async function capture(name){await page.locator('#evidence049-panel').scrollIntoViewIfNeeded();await page.evaluate(()=>new Promise(r=>requestAnimationFrame(()=>requestAnimationFrame(r))));await fs.writeFile(path.join(out,name),Buffer.from(await app.evaluate(()=>global.workspaceTest.capture()),'base64'));}
 try{
  await open();const boot=(await api('/api/agent042/state')).bootId;
  const suite=await api('/api/world/suite');const scene=await api('/api/spatial/import',{text:JSON.stringify(suite.cases[0].source)});
  const world=await api('/api/world/create',{sceneId:scene.id,title:'Untouched evidence test world'});
  const worldBefore=await api('/api/world/observe',{worldId:world.id});
  await panel();assert(await page.locator('#evidence049-play').isDisabled());
  await upload('visible');const one=(await api('/api/evidence049/list')).recordings[0];
  const visible=await api('/api/evidence049/read',{id:one.id});
  assert.equal(visible.associations[0].status,'measured-crossing-supported');
  await seekEnd();assert.equal(await page.locator('#evidence049-chart circle').count(),4);
  assert.match(await page.locator('#evidence049-results').innerText(),/Beam supports a measured crossing/);
  await capture('evidence049-visible.png');
  checks.push('Actual file input imports the frozen visible sequence through desktop IPC and Java; offset-clock beam and measured crossing are displayed');
  await page.locator('#evidence049-export').click();await page.waitForFunction(()=>document.querySelector('#evidence049-status').textContent.startsWith('Recording exported'));
  const manifest=(await fs.readFile(path.join(project,'exports/manifest.jsonl'),'utf8')).trim().split('\n').map(JSON.parse);
  const exported=JSON.parse(await fs.readFile(manifest.at(-1).path,'utf8'));
  assert.deepEqual(exported,JSON.parse(await fs.readFile(path.join(fixtures,'visible.json'),'utf8')));
  const storedPath=path.join(project,'evidence-049',one.id+'.json'),original=await fs.readFile(storedPath);
  await upload('visible');assert.equal((await api('/api/evidence049/list')).recordings.length,1);assert.deepEqual(await fs.readFile(storedPath),original);
  checks.push('Export reproduces the original recording; duplicate import leaves retained bytes unchanged');
  await upload('occluded');const two=(await api('/api/evidence049/list')).recordings.find(r=>r.id!==one.id);
  assert.equal((await api('/api/evidence049/read',{id:two.id})).associations[0].status,'prediction-near-witness');
  await seekEnd();assert.equal(await page.locator('#evidence049-chart circle').count(),2);assert.equal(await page.locator('#evidence049-chart rect[fill="none"]').count(),2);
  assert.match(await page.locator('#evidence049-results').innerText(),/position remains predicted/);
  await capture('evidence049-occluded.png');
  await page.locator('#evidence049-play').click();await page.waitForFunction(()=>document.querySelector('#evidence049-play').textContent==='Pause replay');
  await page.waitForFunction(()=>Number(document.querySelector('#evidence049-seek').value)>1200);
  await page.locator('#evidence049-play').click();const paused=await page.locator('#evidence049-seek').inputValue();
  await page.waitForTimeout(180);assert.equal(await page.locator('#evidence049-seek').inputValue(),paused);
  await page.locator('#evidence049-seek').evaluate(e=>{e.value=2500;e.dispatchEvent(new Event('input',{bubbles:true}));});
  assert.match(await page.locator('#evidence049-current').innerText(),/camera: last predicted/);
  checks.push('Occlusion keeps two predictions visually distinct; real-time replay advances, pauses and seeks without filling hidden positions');
  await app.evaluate(({BrowserWindow})=>BrowserWindow.getAllWindows()[0].setSize(1100,900));
  await seekEnd();await capture('evidence049-narrow.png');
  assert(await page.locator('#brain-dialog').evaluate(e=>e.scrollWidth<=e.clientWidth+2));
  checks.push('Integrated view fits an 1100-pixel application window');
  assert.deepEqual(await api('/api/world/observe',{worldId:world.id}),worldBefore);
  const before=await api('/api/evidence049/read',{id:two.id});
  await app.close();app=null;await open();assert.notEqual((await api('/api/agent042/state')).bootId,boot);
  assert.deepEqual(await api('/api/evidence049/read',{id:two.id}),before);assert.deepEqual(await fs.readFile(storedPath),original);
  assert.deepEqual(await api('/api/world/observe',{worldId:world.id}),worldBefore);
  await panel();await page.locator('#evidence049-choice').selectOption(two.id);
  await page.waitForFunction(()=>document.querySelector('#evidence049-status').textContent.includes('LABELLED SYNTHETIC'));
  await seekEnd();await capture('evidence049-restored.png');
  checks.push('Cold Java restart preserves recording bytes, analysis and the existing native world head');
  await app.close();app=null;
  const envelope=JSON.parse(original);envelope.recording.events[0].xM=.03;await fs.writeFile(storedPath,JSON.stringify(envelope));const corrupted=await fs.readFile(storedPath);
  await open();assert.equal((await api('/api/evidence049/list')).recordings.find(r=>r.id===one.id).available,false);
  await assert.rejects(api('/api/evidence049/read',{id:one.id}),/checksum changed/);
  assert.deepEqual(await fs.readFile(storedPath),corrupted);assert.equal((await api('/api/evidence049/read',{id:two.id})).id,two.id);
  checks.push('Changed retained evidence is refused without rewriting it or blocking unaffected recordings');
  assert.deepEqual(errors,[]);
  await fs.writeFile(path.join(out,'evidence049-integration.json'),JSON.stringify({passed:true,date:new Date().toISOString(),checks,bootChanged:true,worldUnchanged:true,visible,occluded:before,scope:'Labelled synthetic camera/beam recordings in an actual isolated desktop and Java runtime. No live hardware acquisition or physical calibration.'},null,2));
  console.log(JSON.stringify({passed:true,checks},null,2));
 }catch(e){if(page){await fs.writeFile(path.join(out,'evidence049-failure.txt'),await page.locator('body').innerText().catch(()=>''));}throw e;}
 finally{if(app)await app.close();}
}
main().catch(e=>{console.error(e);process.exitCode=1;});
