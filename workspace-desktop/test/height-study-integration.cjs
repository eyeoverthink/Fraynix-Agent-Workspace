'use strict';

const assert = require('node:assert/strict');
const fs = require('node:fs/promises');
const os = require('node:os');
const path = require('node:path');
const {launch} = require('./calibration-integration.cjs');

async function main() {
  process.env.FRAYNIX_WORKSPACE_NO_MODELS='1';
  const output=path.resolve(process.argv[2] || 'workspace-desktop/test-output/height');
  await fs.mkdir(output,{recursive:true});
  const project=await fs.mkdtemp(path.join(os.tmpdir(),'Fraynix metric height '));
  const fixtures=path.resolve(__dirname,'../../workspace-engine/src/test/resources/spatial/height-studies');
  const truth=JSON.parse(await fs.readFile(path.join(fixtures,'independent-mesh-measurements.json'),'utf8'));
  const extra=process.platform==='linux'?['--no-sandbox','--disable-dev-shm-usage']:[];
  if(process.env.FRAYNIX_HEIGHT_TEST_NO_WEBGL==='1') extra.push('--disable-webgl');
  let {app,page}=await launch(project,extra);
  const api=(method,args={})=>app.evaluate((_,x)=>global.workspaceTest.api('/api/spatial/'+x.method,x.args),{method,args});
  const record=()=>api('height-latest');
  const state=async status=>{
    await page.waitForFunction(wanted=>[wanted,'error'].includes(document.querySelector('#height-status')?.dataset.status),status,{timeout:20000});
    assert.equal(await page.locator('#height-status').getAttribute('data-status'),status,await page.locator('#height-status').textContent());
  };
  const click=id=>page.locator('#'+id).click();
  const capture=async name=>{
    await page.locator('#spatial-height').evaluate(element=>element.scrollIntoView({block:'start'}));
    await page.waitForFunction(()=>[...document.querySelectorAll('#height-views img')].every(image=>image.complete&&image.naturalWidth>0));
    await page.evaluate(()=>new Promise(resolve=>requestAnimationFrame(()=>requestAnimationFrame(resolve))));
    await fs.writeFile(path.join(output,name),Buffer.from(await app.evaluate(()=>global.workspaceTest.capture()),'base64'));
  };
  const open=async()=>{
    await click('spatial-tab');
    const panel=page.locator('#spatial-height');
    if (!await panel.evaluate(element=>element.open)) await panel.locator('summary').click();
  };
  const choose=async name=>{
    await page.locator('#height-import:enabled').waitFor({state:'attached'});
    await page.locator('#height-import').setInputFiles(path.join(fixtures,name+'.json'));
    await state('review');
  };
  function withoutPictures(row) {
    const {previews,...result}=row;
    return result;
  }
  function check(row,name) {
    const m=row.measurement, independent=truth[name].meshHeightM;
    assert(m.passed);
    assert(m.lowM<=independent && independent<=m.highM);
    assert(Math.abs(m.heightM-independent)<=.005);
    assert(m.withheldErrorPx<=2);
    assert.equal(row.collider.objects[1].size[1],m.highM);
  }

  try {
    await open();
    await click('height-example');
    await state('review');
    const planned=await record();
    assert.deepEqual(Object.keys(planned.previews),['initial']);
    assert.equal(planned.plan.selectedView,'side');
    assert.equal(planned.plan.candidateScores.find(r=>r.view==='repeat-overhead').separationLowerPx,0);
    assert(await page.locator('#height-collider').isDisabled());
    await page.locator('#goal').fill('Which camera view would tell you how tall this is?');
    await click('run');
    await page.getByText('The side view gives the strongest separation in',{exact:false}).last().waitFor();
    await capture('view-plan.png');

    await app.close();
    ({app,page}=await launch(project,extra));
    await open();
    await click('height-reopen');
    await state('review');
    assert.deepEqual(await record(),planned);
    await click('height-approve');
    await state('verified');
    const low=await record();
    check(low,'low');
    assert.equal(await page.locator('#height-views img').count(),3);
    await capture('verified-height.png');
    assert.equal((await api('list')).scenes.length,0);
    await click('height-collider');
    await page.waitForFunction(()=>document.querySelector('#height-status').dataset.status==='error'
      || Boolean(document.querySelector('#height-status').dataset.collider));
    assert.notEqual(await page.locator('#height-status').getAttribute('data-status'),'error',
      await page.locator('#height-status').textContent());
    const importedId=await page.locator('#spatial-scenes').inputValue();
    assert.equal(await page.locator('#height-status').getAttribute('data-collider'),importedId);
    assert((await page.locator('#spatial-objects').locator('option:checked').textContent()).includes('Upper-bound height collider'));
    const imported=await api('read',{id:importedId});
    assert.equal(imported.objects.find(body=>body.sourceId==='prop').size[1],low.measurement.highM);
    await click('height-collider');
    await page.locator('#height-collider:enabled').waitFor();
    assert.equal((await api('list')).scenes.length,1);
    await page.locator('#goal').fill('How tall is this prop?');
    await click('run');
    await page.getByText('A scene is open in Spatial.',{exact:false}).last().waitFor();

    await choose('tall');
    const tallPlan=await record();
    assert.deepEqual(tallPlan.plan,planned.plan);
    await click('height-approve');
    await state('verified');
    const tall=await record();
    check(tall,'tall');
    const savedTallSource=path.join(project,'spatial','height-studies',tall.id+'.source');
    const tallData=await fs.readFile(savedTallSource);
    await fs.writeFile(savedTallSource,JSON.stringify({altered:true}));
    await assert.rejects(api('height-collider',{id:tall.id,colliderApprovalSha256:tall.colliderApprovalSha256}),/bytes changed/);
    await fs.writeFile(savedTallSource,tallData);

    await click('height-example');
    await state('verified');
    await page.locator('#goal').fill('Measure height in the open study.');
    await click('run');
    const oldMeasure=(low.measurement.heightM*1000).toFixed(1)+' mm';
    await page.getByText('measured '+oldMeasure,{exact:false}).last().waitFor();
    await page.locator('#height-import:enabled').waitFor({state:'attached'});
    await choose('occluded');
    await click('height-approve');
    await state('refused');
    const refused=await record();
    assert(await page.locator('#height-collider').isDisabled());
    assert(!Object.hasOwn(refused,'collider'));
    await capture('refused-height.png');
    assert.equal((await api('list')).scenes.length,1);

    await fs.writeFile(path.join(output,'evidence.json'),JSON.stringify({
      platform:process.platform, survivedRestart:true, independentBlenderMeshMeasurements:truth,
      plan:withoutPictures(planned), low:withoutPictures(low),tall:withoutPictures(tall),
      refused:withoutPictures(refused), imported,
      refusedChangedSource:true, duplicateImportReturnedOriginalScene:true, scopedChatToOpenStudy:true
    },null,2)+'\n');
    console.log(JSON.stringify({passed:true, low:low.measurement,tall:tall.measurement,refused:refused.measurement}));
  } finally { await app.close().catch(()=>{}); }
}
main().catch(error=>{console.error(error);process.exitCode=1;});
