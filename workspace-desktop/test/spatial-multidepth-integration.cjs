'use strict';

const assert=require('node:assert/strict');
const fs=require('node:fs/promises');
const os=require('node:os');
const path=require('node:path');
const {execFile}=require('node:child_process');
const {promisify}=require('node:util');
const {launch}=require('./calibration-integration.cjs');

async function main() {
  process.env.FRAYNIX_WORKSPACE_NO_MODELS='1';
  const output=path.resolve(process.argv[2]||'build/visual-multidepth222');
  const fixture=path.resolve(__dirname,'../../workspace-engine/src/main/resources/spatial/reconstruction/opposed-depth.json');
  const input=JSON.parse(await fs.readFile(fixture,'utf8'));
  const project=await fs.mkdtemp(path.join(os.tmpdir(),'Fraynix opposite recesses '));
  const extra=process.platform==='linux'?['--no-sandbox','--disable-dev-shm-usage']:[];
  if (process.env.FRAYNIX_TEST_NO_WEBGL==='1') extra.push('--disable-gpu','--disable-software-rasterizer');
  const blender=process.env.FRAYNIX_TEST_BLENDER;
  await fs.mkdir(output,{recursive:true});
  let {app,page}=await launch(project,extra);
  const api=(method,args={})=>app.evaluate((_,r)=>global.workspaceTest.api('/api/spatial/'+r.method,r.args),{method,args});
  const build=body=>api('reconstruct',{base64:Buffer.from(JSON.stringify(body)).toString('base64')});
  const status=text=>page.waitForFunction(text=>document.querySelector('#spatial-reconstruct-result')?.textContent.includes(text),text,{timeout:12000});
  const capture=async name=>{
    await page.locator('#spatial-reconstruct').evaluate(el=>el.scrollIntoView({block:'start'}));
    await page.evaluate(()=>new Promise(done=>requestAnimationFrame(()=>requestAnimationFrame(done))));
    await fs.writeFile(path.join(output,name),Buffer.from(await app.evaluate(()=>global.workspaceTest.capture()),'base64'));
  };
  const save=async (scene,name)=>{
    const file=await api('export-glb',{id:scene.id});
    await fs.writeFile(path.join(output,name+'.glb'),Buffer.from(file.base64,'base64'));
    return file.sha256;
  };
  try {
    const silhouettes={...input,format:'fraynix.orthographic-silhouettes-v1'};
    delete silhouettes.depthViews;
    const baseline=await build(silhouettes);
    const plus=await build({...input,depthViews:[input.depthViews[0]]});
    const minus=await build({...input,depthViews:[input.depthViews[1]]});
    assert.equal(baseline.reconstruction.voxels,650);
    assert.equal(plus.reconstruction.voxels,632);
    assert.equal(minus.reconstruction.voxels,632);
    await save(baseline,'silhouettes');
    await save(plus,'positive-only');
    await save(minus,'negative-only');

    await page.locator('#spatial-tab').click();
    await page.locator('#spatial-reconstruct summary').click();
    await page.locator('#spatial-reconstruct-opposed-example').click();
    await status('36 cells carved by depth from 2 views');
    const id=await page.locator('#spatial-scenes').inputValue();
    const combined=await api('read',{id});
    assert.equal(combined.reconstruction.voxels,614);
    assert.equal(combined.reconstruction.depth.views.length,2);
    assert.notEqual(combined.reconstruction.meshSha256,plus.reconstruction.meshSha256);
    assert.notEqual(combined.reconstruction.meshSha256,minus.reconstruction.meshSha256);
    await capture('fraynix-opposed-depth.png');
    // Software-only runners can use the app's visible fallback and still verify geometry and export.
    const viewport=await page.locator('#spatial-stage').evaluate(stage=>
      stage.querySelector('canvas')?.getBoundingClientRect().toJSON() || null);
    if (process.env.FRAYNIX_TEST_NO_WEBGL==='1') assert.equal(viewport,null);
    if (!viewport) {
      await page.locator('#spatial-stage').getByText('The 3D preview is unavailable on this display.',{exact:false}).waitFor();
    }
    if (viewport && viewport.width>520) {
      const x=viewport.x+80,y=viewport.y+Math.min(140,viewport.height/2);
      await page.mouse.move(x,y);
      await page.mouse.down();
      await page.mouse.move(x+392,y,{steps:14});
      await page.mouse.up();
      await capture('fraynix-opposite-side.png');
    }
    await page.locator('#spatial-reconstruct-export').click();
    await page.getByText('Checked GLB saved to',{exact:false}).last().waitFor();
    const sha=await save(combined,'opposed-depth');

    const conflict=JSON.parse(JSON.stringify(input));
    conflict.depthViews[1].samples[(15-10)*16+9]=500000;
    const bad=path.join(output,'conflict.json');
    await fs.writeFile(bad,JSON.stringify(conflict));
    await page.locator('#spatial-reconstruct-file').setInputFiles(bad);
    await status('Conflicting depth');
    assert.equal(await page.locator('#spatial-scenes').inputValue(),id);
    assert.equal((await api('list')).scenes.length,4);
    const reordered=await build({...input,depthViews:[...input.depthViews].reverse()});
    assert.deepEqual(reordered.reconstruction.depth,combined.reconstruction.depth);
    assert.equal(reordered.reconstruction.meshSha256,combined.reconstruction.meshSha256);
    assert.notEqual(reordered.reconstruction.sourceSha256,combined.reconstruction.sourceSha256);

    let independent=null;
    if (blender) {
      const script=path.join(__dirname,'spatial-multidepth-blender.py');
      await promisify(execFile)(blender,['--background','--factory-startup','--disable-autoexec','--python-exit-code','17',
        '--python',script,'--','verify',output],{timeout:90000,windowsHide:true});
      independent=JSON.parse(await fs.readFile(path.join(output,'multidepth-blender-evidence.json'),'utf8'));
      assert(independent.passed);
    }

    await app.close();
    ({app,page}=await launch(project,extra));
    assert.deepEqual(await api('read',{id}),combined);
    assert.equal((await api('export-glb',{id})).sha256,sha);
    const original=path.join(project,'spatial','reconstruction-sources',id+'.json');
    const altered=JSON.parse(await fs.readFile(original,'utf8'));
    altered.depthViews[1].samples[255]=-2;
    await fs.writeFile(original,JSON.stringify(altered));
    await assert.rejects(api('export-glb',{id}),/original visual reconstruction/);
    assert.deepEqual(await api('read',{id:plus.id}),plus);
    await fs.writeFile(path.join(output,'multidepth-evidence.json'),JSON.stringify({
      platform:process.platform,baseline:baseline.reconstruction,positiveOnly:plus.reconstruction,
      negativeOnly:minus.reconstruction,combined:combined.reconstruction,independentBlender:independent,
      exportSha256:sha,webglPreviewAvailable:viewport!==null,contradictionRefused:true,orderIndependentMesh:true,restoredAfterRestart:true,changedGridRefused:true
    },null,2)+'\n');
    console.log(JSON.stringify({cells:combined.reconstruction.voxels,carved:combined.reconstruction.depth.removedVoxels,independent}));
  } catch(error) {
    console.error((await page.locator('#spatial-reconstruct-result').textContent().catch(()=>null))||'No reconstruction status');
    throw error;
  } finally {await app.close().catch(()=>{});}
}
main().catch(error=>{console.error(error);process.exitCode=1;});
