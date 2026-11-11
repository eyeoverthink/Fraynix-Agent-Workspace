const fs=require('node:fs/promises'),path=require('node:path'),assert=require('node:assert/strict');
const {launch}=require('./test/calibration-integration.cjs');
async function main(){
 const out=path.resolve('../build/game-level114-ui'),project=path.join(out,'project');await fs.mkdir(project,{recursive:true});const started=performance.now(),errors=[],trace=[];let app,page;
 const result={date:new Date().toISOString(),scope:'Actual Electron/Java/original game renderer, source-based candidate. No model calls or chat-generation claim.',passed:false};
 try{
  ({app,page}=await launch(project));page.on('pageerror',e=>errors.push(e.message));await page.setViewportSize({width:1600,height:1000});
  const api=(route,args={})=>app.evaluate((_,x)=>global.workspaceTest.api(x.route,x.args),{route,args});
  const request=(command,args={})=>page.evaluate(x=>window.workspace.request({operation:'gameRuntime',command:x.command,args:x.args}),{command,args});
  const capture=async name=>fs.writeFile(path.join(out,name),Buffer.from(await app.evaluate(()=>global.workspaceTest.capture()),'base64'));
  await page.locator('#game-tab').click();await page.waitForFunction(()=>document.querySelector('#game-status').textContent.includes('tick'));
  await page.locator('#game-level-builder > summary').click();await page.locator('#game-level-analyze').click();await page.waitForFunction(()=>document.querySelector('#game-level-status').textContent.includes('1248 objects'));
  result.analysis=await api('/api/game/analyze-levels');await capture('source-analysis.png');
  await page.locator('#game-level-create').click();await page.waitForFunction(()=>document.querySelector('#game-level-status').textContent.includes('Playtest still required'));
  const candidates=await api('/api/game/levels');result.candidate=candidates.candidates.at(-1);const id=result.candidate.id;
  await page.locator('#game-level-open').click();await page.waitForFunction(()=>document.querySelector('#game-level-status').textContent.startsWith('Opened saved candidate'),{timeout:60000});
  const first=await request('frame');assert.equal(first.levelId,id);assert.equal(first.state.visibleWindow,false);assert.equal(first.state.editorInitialized,false);result.initial=first.state;await capture('candidate-start.png');
  await page.locator('#game-right').click();await page.waitForFunction(x=>Number(document.querySelector('#game-status').textContent.match(/Player X ([\d.]+)/)?.[1])>x+.1,first.state.player.x);result.manual=await request('frame');delete result.manual.pngBase64;
  await page.locator('#game-reset').click();await page.waitForTimeout(250);
  let frame=await request('frame');for(let n=0;n<180;n++){
   const state=frame.state,p=state.player;trace.push(state);if(p.won||p.dead)break;
   const jump=result.candidate.gaps.some(g=>p.x>g.startX-.7&&p.x<g.endX+.1);frame=await request('command',{action:'step',ticks:12,horizontal:1,jump});
  }
  result.final=frame.state;result.trace=trace;assert.equal(frame.state.player.won,true,'Actual flag-contact outcome');await capture('candidate-finish.png');
  await page.setViewportSize({width:1100,height:800});await capture('candidate-1100.png');assert.equal(await page.locator('#game-level-create').isVisible(),true);
  await app.close();({app,page}=await launch(project));await page.setViewportSize({width:1600,height:1000});
  await page.locator('#game-tab').click();await page.locator('#game-level-builder > summary').click();await page.locator('#game-level-refresh').click();await page.waitForFunction(()=>document.querySelector('#game-level-choice').options.length>0);await page.locator('#game-level-choice').selectOption(id);await page.locator('#game-level-open').click();await page.waitForFunction(()=>document.querySelector('#game-level-status').textContent.startsWith('Opened saved candidate'),{timeout:60000});
  result.reopened=(await request('frame')).state;assert.notEqual(result.reopened.sessionId,result.initial.sessionId);assert.equal(result.reopened.player.won,false);assert.equal(result.reopened.player.id,result.initial.player.id);assert.equal(result.reopened.player.x,result.initial.player.x);
  await capture('candidate-reopened.png');await request('close');
  const level=path.join(project,'game-levels114',id,'level.json'),original=await fs.readFile(level);await fs.appendFile(level,' ');
  await assert.rejects(request('open-level',{id}),/bytes changed/);assert.equal((await api('/api/game/state')).running,false);await fs.writeFile(level,original);
  await assert.rejects(request('create-level',{gapTiles:3}),/integer/);await assert.rejects(request('open-level',{id:'../level.txt'}),/saved candidate ID/);
  result.refusals=['changed candidate bytes','unsupported gap width','path traversal'];assert.deepEqual(errors,[]);result.rendererErrors=errors;result.passed=true;
 }finally{if(app)await app.close().catch(()=>{});result.elapsedSeconds=(performance.now()-started)/1000;await fs.writeFile(path.join(out,'result.json'),JSON.stringify(result,null,2));}
 console.log(JSON.stringify({passed:result.passed,elapsedSeconds:result.elapsedSeconds,candidate:result.candidate.id,refusals:result.refusals}));
}
main().catch(e=>{console.error(e);process.exitCode=1;});
