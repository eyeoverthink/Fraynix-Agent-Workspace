'use strict';
const {launch}=require('../../workspace-desktop/test/calibration-integration.cjs'),fs=require('node:fs/promises'),assert=require('node:assert/strict');
(async()=>{
 const d=JSON.parse(await fs.readFile('build/model126-complete/result.json','utf8'));assert(d.passed);
 Object.assign(process.env,{JAVA_HOME:'C:/Program Files/Eclipse Adoptium/jdk-21.0.10.7-hotspot',FRAYNIX_WORKSPACE_PERSISTENT:'0',FRAYNIX_WORKSPACE_JAVA_HEAP_MIB:'512',FRAYNIX_WORKSPACE_JAVA_PROCESSORS:'2'});
 const {app,page}=await launch(d.project);const record={passed:false,scope:'Later visual review of the same saved replay artifact. No model call, new implementation, replacement proof or retiming.',artifactId:d.replay.artifactId,captures:[]};
 try{
  await page.waitForFunction(()=>window.browserChecks126);
  const before=await app.evaluate((_,id)=>global.workspaceTest.api('/api/artifact/read',{id}),d.replay.artifactId);
  assert.deepEqual(before.contents,d.requests.find(x=>x.kind==='creation').artifact.contents);assert.deepEqual(before.test,d.replay.proof);
  await app.evaluate((_,id)=>global.workspaceTest.execute('canvas.open',{id}),d.replay.artifactId);
  await page.locator('#preview-tab').click();
  record.visibleChecks=await app.evaluate(()=>global.workspaceTest.canvasChecks([{selector:'#quantity',action:'fill',value:'7'},{selector:'#calculate',action:'click'},{selector:'#total',expectText:'21'}]));
  assert(record.visibleChecks.every(c=>c.passed));
  for(const width of [1600,1100]){
   await app.evaluate(({BrowserWindow},width)=>BrowserWindow.getAllWindows().find(w=>w.isVisible()).setSize(width,1100),width);
   await new Promise(r=>setTimeout(r,500));assert(await page.evaluate(()=>document.documentElement.scrollWidth<=innerWidth));
   const name=`calculator-replay-${width}.png`;await fs.writeFile('build/model126-complete/'+name,Buffer.from(await app.evaluate(()=>global.workspaceTest.capture()),'base64'));record.captures.push(name);
  }
  assert.deepEqual((await app.evaluate((_,id)=>global.workspaceTest.api('/api/artifact/read',{id}),d.replay.artifactId)).test,before.test);record.passed=true;
 }finally{await fs.writeFile('build/model126-replay-review.json',JSON.stringify(record,null,2));await app.close();}
 console.log('Saved replay displayed at two widths; original implementation and proof unchanged');
})().catch(e=>{console.error(e);process.exitCode=1;});
