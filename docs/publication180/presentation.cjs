'use strict';

const assert=require('node:assert/strict');
const {createHash}=require('node:crypto');
const fs=require('node:fs/promises');
const path=require('node:path');
const {launch}=require('../../workspace-desktop/test/calibration-integration.cjs');
const {verify}=require('../../scripts/Study-Build095.cjs');
const digest=x=>createHash('sha256').update(x).digest('hex');

async function main(){
  const study=path.resolve('build','workbench180-live');
  const output=path.resolve('build','workbench180-final');
  const project=path.join(output,'project');
  const original=JSON.parse(await fs.readFile(path.join(study,'result.json'),'utf8'));
  const baseline=JSON.parse(await fs.readFile('build/workbench180-study-build.json','utf8'));
  const installed=verify(process.cwd());
  assert(original.passed&&baseline.source===original.source);
  assert.deepEqual(installed.files.artifacts,baseline.files.artifacts);
  const old=new Map(baseline.files.inputs.map(row=>[row.path,row.sha256]));
  const changed=installed.files.inputs.filter(row=>old.get(row.path)!==row.sha256).map(row=>row.path).sort();
  assert.deepEqual(changed,[
    'workspace-desktop/renderer/index.html',
    'workspace-desktop/renderer/request-workbench.css',
    'workspace-desktop/renderer/request-workbench.js'
  ]);
  await fs.mkdir(output);
  await fs.mkdir(project);
  for(const folder of ['workspace.fworld','workspace.fworld.previous','artifacts','receipts','request-workbench']){
    await fs.cp(path.join(study,'project',folder),path.join(project,folder),{recursive:true,errorOnExist:true});
  }
  Object.assign(process.env,{FRAYNIX_WORKSPACE_NO_MODELS:'1',FRAYNIX_WORKSPACE_PERSISTENT:'0',
    FRAYNIX_WORKSPACE_JAVA_HEAP_MIB:'768',FRAYNIX_WORKSPACE_JAVA_PROCESSORS:'2'});
  const {app,page}=await launch(project);
  const result={...original,source:installed.source,studySource:baseline.source,engineJarsMatched:true,captures:[]};
  try{
    await app.evaluate(({BrowserWindow})=>BrowserWindow.getAllWindows().find(w=>w.isVisible()).setSize(1480,1030));
    await page.locator('#creation-open').click();
    await page.locator('#creation-dialog').waitFor({state:'visible'});
    for(const id of ['maps-open','workflow-open','procedural-open']){
      assert.equal(await page.locator('#creation-dialog #'+id).count(),1);
      assert.equal(await page.locator('#requests-dialog #'+id).count(),0);
    }
    await page.locator('#creation-close').click();
    async function capture(name){
      await page.evaluate(()=>new Promise(resolve=>requestAnimationFrame(()=>requestAnimationFrame(resolve))));
      const bytes=Buffer.from(await app.evaluate(()=>global.workspaceTest.capture()),'base64');
      await fs.writeFile(path.join(output,name),bytes);
      result.captures.push({name,sha256:digest(bytes)});
    }
    async function suggest(text){
      await page.locator('#requests-text').fill(text);
      await page.locator('#requests-model').uncheck();
      const prior=await page.evaluate(()=>localStorage.getItem('fraynix-request-review'));
      await page.locator('#requests-suggest').click();
      await page.waitForFunction(id=>localStorage.getItem('fraynix-request-review')!==id,prior);
    }
    async function openSaved(task){
      await page.locator('#requests-open').click();
      await page.evaluate(id=>localStorage.setItem('fraynix-request-review',id),task.id);
      await page.locator('#requests-reopen').click();
      await page.waitForFunction(name=>document.querySelector('#requests-source').textContent.startsWith(name),task.source.name);
      await page.locator('#requests-result').click();
      await page.locator('#requests-dialog').waitFor({state:'hidden'});
      assert.equal(await app.evaluate(()=>global.workspaceTest.canvasRead('document.querySelector("#sha256").textContent')),task.source.sha256);
    }
    await page.locator('#requests-open').click();
    await suggest('Flip this CSV into JSON');
    const csv=Buffer.from('\ufeffCode,Name,Notes\r\n00017,南,"first\nsecond ""quoted"" line"\r\n0,Café,\r\n');
    await page.locator('#requests-file').setInputFiles({name:'incoming.csv',mimeType:'text/csv',buffer:csv});
    await page.waitForFunction(sha=>document.querySelector('#requests-source').textContent.includes(sha),digest(csv));
    assert.equal(await page.locator('#requests-file').evaluate(x=>x.files[0]?.name),'incoming.csv');
    await capture('request-review.png');
    await page.locator('#requests-close').click();
    await openSaved(original.files.forward);
    await capture('round-trip-output.png');
    await openSaved(original.files.inspected);
    assert.match(await app.evaluate(()=>global.workspaceTest.canvasRead('document.querySelector("pre").textContent')),/10001001/);
    await capture('byte-inspection.png');
    await page.locator('#requests-open').click();
    await suggest('Make the datasauce portable!');
    assert.match(await page.locator('#requests-choice').textContent(),/remembered wording/);
    assert(await page.locator('#requests-confirm').isDisabled());
    await capture('remembered-language.png');
    await page.locator('#requests-close').click();
    await page.evaluate(id=>window.workspace.request({operation:'tool',tool:'canvas.open',args:{id}}),original.creation.ref.id);
    const checks=await app.evaluate((_,steps)=>global.workspaceTest.canvasChecks(steps),original.creation.test.steps);
    assert(checks.every(x=>x.passed));
    await capture('created-tool.png');
    result.presentationPassed=true;
    await fs.writeFile(path.join(output,'result.json'),JSON.stringify(result,null,2)+'\n');
    console.log(JSON.stringify({passed:true,source:installed.source,studySource:baseline.source,engineJarsMatched:true,captures:result.captures.length}));
  }finally{await app.close();}
}
main().catch(error=>{console.error(error);process.exitCode=1;});
