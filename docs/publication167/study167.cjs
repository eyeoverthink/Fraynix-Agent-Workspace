'use strict';
const fs=require('node:fs/promises'),path=require('node:path'),assert=require('node:assert/strict');
const cp=require('node:child_process'),crypto=require('node:crypto');
const root=process.cwd(),out=path.join(root,'build/game167-'+(process.env.STUDY167_ATTEMPT||'a'));
const sha=b=>crypto.createHash('sha256').update(b).digest('hex');
(async()=>{
 require('../../scripts/Study-Build095.cjs').verify(root);
 const lock=JSON.parse(await fs.readFile('docs/ORIGINAL_CONTENT_167_LOCK.json'));
 for(const[n,h]of Object.entries(lock.sourceHashes)){
  const bytes=await fs.readFile(n),canonical=/\.(png|wav)$/.test(n)?bytes:Buffer.from(bytes.toString('utf8').replaceAll('\r\n','\n'));
  assert.equal(sha(canonical),h,n);
 }
 await fs.mkdir(out);const project=path.join(out,'project');await fs.mkdir(project);
 Object.assign(process.env,{FRAYNIX_WORKSPACE_PERSISTENT:'0',FRAYNIX_WORKSPACE_JAVA_HEAP_MIB:'768',FRAYNIX_WORKSPACE_JAVA_PROCESSORS:'2'});
 const result={passed:false,source:cp.execFileSync('git',['rev-parse','HEAD'],{encoding:'utf8'}).trim(),
  date:new Date().toISOString(),scope:'Actual Java/Electron, original pack, input-driven native physics. No model inference or physical-camera claim.',captures:[],trace:[],errors:[]};
 let app,page;const {launch}=require('../../workspace-desktop/test/calibration-integration.cjs');
 async function start(){({app,page}=await launch(project));page.on('pageerror',e=>result.errors.push(e.message));await app.evaluate(({BrowserWindow})=>{const w=BrowserWindow.getAllWindows().find(w=>w.isVisible());w.setSize(1540,1020);w.setTitle('Fraynix · Relay Garden · edition 167');});}
 const api=(route,args={})=>app.evaluate((_,x)=>global.workspaceTest.api(x.route,x.args),{route,args});
 const command=args=>api('/api/game/command',args);
 async function capture(name){await page.waitForTimeout(250);const bytes=Buffer.from(await app.evaluate(()=>global.workspaceTest.capture()),'base64');await fs.writeFile(path.join(out,name),bytes);result.captures.push({name,sha256:sha(bytes)});}
 try{
  await start();await page.locator('#game-tab').click();await page.waitForFunction(()=>window.brainGame?.running||document.querySelector('#game-status').textContent.includes('tick'),{},{timeout:60000});
  const legacy=await api('/api/game/frame');assert.equal(legacy.levelId,'original');assert.equal(legacy.state.contentId,undefined);result.legacyDefault={levelId:legacy.levelId,objects:legacy.state.objects};
  await page.locator('#game-relay').click();await page.waitForFunction(()=>document.querySelector('#game-target').value==='13.625',{},{timeout:60000});
  let frame=await api('/api/game/frame');result.initial=frame.state;assert.equal(frame.levelId,'relay-garden-v1');
  assert(frame.state.contentPaths.every(p=>p.startsWith('assets/original/relay-garden/')));
  await capture('relay-garden-start.png');
  await page.locator('#game-right').click();await page.waitForFunction(()=>Number(document.querySelector('#game-status').textContent.match(/Player X ([\d.]+)/)?.[1])>1.0);
  result.manual=(await api('/api/game/frame')).state;assert(result.manual.player.x>result.initial.player.x+.1);
  await page.locator('#game-reset').click();await page.waitForFunction(()=>document.querySelector('#game-status').textContent.includes('tick 0'));
  for(let n=0;n<180;n++){
   frame=await command({action:'ai-step',ticks:12,targetX:14.5});result.trace.push(frame.state);
   if(frame.state.player.won||frame.state.player.dead)break;
   if(n===12)await capture('relay-garden-jump.png');
  }
  result.final=frame.state;assert.equal(frame.state.controller.policy,'contact122');assert.equal(frame.state.player.won,true,'Existing default AI must complete the original course');
  await page.waitForFunction(()=>document.querySelector('#game-status').textContent.includes('goal reached'));await capture('relay-garden-finish.png');
  await app.evaluate(({BrowserWindow})=>BrowserWindow.getAllWindows().find(w=>w.isVisible()).setSize(1100,1000));await capture('relay-garden-compact.png');
  assert.equal(await page.locator('#game-relay').isVisible(),true);assert.equal(await page.locator('#game-frame').isVisible(),true);
  await app.close();app=null;await start();await page.locator('#game-tab').click();await page.waitForFunction(()=>document.querySelector('#game-status').textContent.includes('tick'),{},{timeout:60000});
  await page.locator('#game-relay').click();await page.waitForFunction(()=>document.querySelector('#game-target').value==='13.625',{},{timeout:60000});
  frame=await api('/api/game/frame');result.reopened=frame.state;assert.notEqual(frame.state.sessionId,result.initial.sessionId);
  assert.equal(frame.state.player.id,result.initial.player.id);assert.equal(frame.state.player.x,result.initial.player.x);assert.equal(frame.state.player.won,false);
  await assert.rejects(api('/api/game/open-relay',{path:'../anything'}),/Unknown|Unsupported|Unexpected|field/i);
  assert.equal((await api('/api/game/frame')).state.sessionId,frame.state.sessionId);
  await api('/api/game/close');assert.deepEqual(result.errors,[]);result.passed=true;
 }finally{if(app)await app.close().catch(()=>{});await fs.writeFile(path.join(out,'result.json'),JSON.stringify(result,null,2)+'\n');}
 require('../../scripts/Study-Build095.cjs').verify(root);
 console.log(JSON.stringify({passed:result.passed,trace:result.trace.length,captures:result.captures.length}));
})().catch(e=>{console.error(e);process.exitCode=1;});
