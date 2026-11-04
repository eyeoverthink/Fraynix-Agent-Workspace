'use strict';
const {launch}=require('./calibration-integration.cjs'),fs=require('node:fs/promises'),path=require('node:path'),assert=require('node:assert/strict');
(async()=>{
 process.env.FRAYNIX_WORKSPACE_PERSISTENT='0';
 const out=path.resolve(__dirname,'../test-output'),prior=JSON.parse(await fs.readFile(path.join(out,'cortex061-live.json'),'utf8'));
 const {app,page}=await launch(prior.project),errors=[];page.on('pageerror',e=>errors.push(e.message));
 const api=(route,args={})=>app.evaluate((_,x)=>global.workspaceTest.api(x.route,x.args),{route,args});
 const capture=async key=>{await new Promise(r=>setTimeout(r,500));await fs.writeFile(path.join(out,`cortex061-${key}.png`),Buffer.from(await app.evaluate(()=>global.workspaceTest.capture()),'base64'));};
 try{
  await app.evaluate(({BrowserWindow})=>BrowserWindow.getAllWindows()[0].setSize(1600,1000));
  await page.locator('#brain-open').click();await page.locator('#cortex061').evaluate(e=>e.open=true);
  await page.locator('#c061-history').selectOption(prior.job.id);await page.waitForFunction(()=>document.querySelector('#c061-result').textContent.includes('52.890'));
  assert.equal((await api('/api/brain/read',{id:prior.job.id})).modelSha256061,prior.job.modelSha256061);
  await page.locator('#c061-result').scrollIntoViewIfNeeded();await capture('results');
  await page.locator('#c061-predict-panel').evaluate(e=>e.open=true);await page.locator('#c061-predict').click();await page.waitForFunction(()=>document.querySelector('#c061-prediction').textContent.includes('No action executed'));assert.equal(await page.locator('#c061-prediction').textContent(),prior.prediction);await capture('prediction');
  await app.evaluate(({BrowserWindow})=>BrowserWindow.getAllWindows()[0].setSize(1100,800));await page.locator('#c061-result').scrollIntoViewIfNeeded();await capture('narrow');
  await app.evaluate(({BrowserWindow})=>BrowserWindow.getAllWindows()[0].setSize(1600,1000));await page.locator('#brain-close').click();
  await page.locator('#library-items button').filter({hasText:'Continuing Java agent042'}).click();
  await page.waitForFunction(()=>document.querySelector('#artifact-title').textContent.includes('Continuing Java agent042'));
  const report=await app.evaluate(()=>global.workspaceTest.canvasRead('document.body.innerText'));assert(report.includes(prior.job.modelSha256061));assert(report.includes('fraynixGPT061'));await capture('chat');
  assert.deepEqual(errors,[]);await fs.writeFile(path.join(out,'cortex061-review.json'),JSON.stringify({passed:true,source:require('node:child_process').execFileSync('git',['rev-parse','HEAD'],{encoding:'utf8'}).trim(),date:new Date().toISOString(),originalSource:prior.source,jobId:prior.job.id,modelSha256:prior.job.modelSha256061,checks:['Select completed original after cancelled job and cold restart','Reproduce exact prior prediction','Actual Electron window 1600 and 1100 widths','Retained local-model report contains exact outcome checkpoint'],errors},null,2));console.log('Final review passed; existing original experiment and report retained.');
 }finally{await app.close();}
})().catch(e=>{console.error(e);process.exitCode=1;});
