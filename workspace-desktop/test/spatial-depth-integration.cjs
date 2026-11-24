'use strict';

const assert=require('node:assert/strict');
const fs=require('node:fs/promises');
const os=require('node:os');
const path=require('node:path');
const {execFile}=require('node:child_process');
const {promisify}=require('node:util');
const {launch}=require('./calibration-integration.cjs');

function depthAt(scene,body,x,y) {
  const data=Buffer.from(scene.visualMeshes[body.visualMesh],'base64');
  const count=data.readUInt32LE(0),indexCount=data.readUInt32LE(4),start=8+12*count;
  const world=(vertex,axis)=>data.readFloatLE(8+12*vertex+4*axis)+body.position[axis];
  let z=-Infinity;
  for(let i=0;i<indexCount;i+=3) {
    const a=data.readUInt16LE(start+2*i), b=data.readUInt16LE(start+2*i+2), c=data.readUInt16LE(start+2*i+4);
    const ax=world(a,0), ay=world(a,1);
    const bx=world(b,0)-ax, by=world(b,1)-ay, cx=world(c,0)-ax, cy=world(c,1)-ay;
    const determinant=bx*cy-by*cx;
    if (Math.abs(determinant)<1e-9) continue;
    const u=((x-ax)*cy-(y-ay)*cx)/determinant, v=(bx*(y-ay)-by*(x-ax))/determinant;
    if (u>=0&&v>=0&&u+v<=1) z=Math.max(z,(1-u-v)*world(a,2)+u*world(b,2)+v*world(c,2));
  }
  return .4-z;
}

async function main() {
  process.env.FRAYNIX_WORKSPACE_NO_MODELS='1';
  const output=path.resolve(process.argv[2]||'build/visual-depth221');
  const blender=process.env.FRAYNIX_TEST_BLENDER;
  const runBlender=(mode,...files)=>promisify(execFile)(blender,[
    '--background','--factory-startup','--disable-autoexec','--python-exit-code','17','--python',
    path.join(__dirname,'spatial-depth-blender.py'),'--',mode,...files
  ],{timeout:90000,windowsHide:true});
  await fs.mkdir(output,{recursive:true});
  const fixture=path.resolve(__dirname,'../../workspace-engine/src/main/resources/spatial/reconstruction/recess-depth.json');
  const bytes=await fs.readFile(fixture);
  const baseline=JSON.parse(bytes.toString('utf8'));
  delete baseline.depth;
  baseline.format='fraynix.orthographic-silhouettes-v1';
  const comparisonInput=path.join(output,'comparison-input.json');
  await fs.writeFile(comparisonInput,JSON.stringify(baseline));
  const project=await fs.mkdtemp(path.join(os.tmpdir(),'Fraynix verified depth '));
  const extra=process.platform==='linux'?['--no-sandbox','--disable-dev-shm-usage']:[];
  let {app,page}=await launch(project,extra);
  const api=(method,args={})=>app.evaluate((_,r)=>global.workspaceTest.api('/api/spatial/'+r.method,r.args),{method,args});
  const read=()=>page.locator('#spatial-scenes').inputValue().then(id=>api('read',{id}));
  const capture=async name=>{
    await page.locator('#spatial-reconstruct').evaluate(el=>el.scrollIntoView({block:'start'}));
    await page.evaluate(()=>new Promise(done=>requestAnimationFrame(()=>requestAnimationFrame(done))));
    await fs.writeFile(path.join(output,name),Buffer.from(await app.evaluate(()=>global.workspaceTest.capture()),'base64'));
  };
  const writeExport=async (scene,name)=>{
    const exported=await api('export-glb',{id:scene.id});
    await fs.writeFile(path.join(output,name),Buffer.from(exported.base64,'base64'));
    return exported.sha256;
  };

  try {
    await page.locator('#spatial-tab').click();
    await page.locator('#spatial-reconstruct summary').click();
    await page.locator('#spatial-reconstruct-file').setInputFiles(comparisonInput);
    await page.waitForFunction(()=>document.querySelector('#spatial-reconstruct-result').textContent.includes('650 reconstructed cells'));
    const uncarved=await read();
    await capture('fraynix-silhouettes.png');
    await writeExport(uncarved,'silhouettes-only.glb');

    await page.locator('#spatial-reconstruct-depth-example').click();
    await page.waitForFunction(()=>document.querySelector('#spatial-reconstruct-result').textContent.includes('18 cells carved by depth'));
    const refined=await read();
    const phi=refined.reconstruction;
    assert.equal(phi.voxels,632);
    assert.equal(phi.depth.removedVoxels,18);
    assert.deepEqual(phi.pngSha256,uncarved.reconstruction.pngSha256);
    const body=refined.objects.find(o=>o.sourceId==='surface');
    assert(Math.abs(depthAt(refined,body,.0635,.112)-.3)<1e-6);
    await capture('fraynix-depth-reconstruction.png');
    await page.locator('#spatial-reconstruct-export').click();
    await page.getByText('Checked GLB saved to',{exact:false}).last().waitFor();
    const sha=await writeExport(refined,'recovered-recess.glb');

    let independent=null,returned=null;
    if (blender) {
      await runBlender('verify',path.join(output,'silhouettes-only.glb'),path.join(output,'recovered-recess.glb'),output);
      independent=JSON.parse(await fs.readFile(path.join(output,'depth-blender-evidence.json'),'utf8'));
      assert(independent.passed);
      const edited=await fs.readFile(path.join(output,'blender-edited-recess.glb'));
      const imported=await api('import-glb',{base64:edited.toString('base64')});
      const changed=imported.objects.find(o=>o.sourceId==='surface');
      const editDepth=depthAt(imported,changed,.0635,.112);
      assert(Math.abs(editDepth-.325)<1e-6);
      assert.deepEqual(changed.size,body.size);
      assert.notEqual(changed.visualMesh,body.visualMesh);
      assert.deepEqual(await api('read',{id:refined.id}),refined);
      returned={editedProbeM:editDepth,colliderSizeM:changed.size,originalPreserved:true};
    }

    await page.locator('#spatial-scenes').selectOption(uncarved.id);
    await page.waitForFunction(()=>document.querySelector('#spatial-reconstruct-result').textContent.includes('650 reconstructed cells'));
    assert(!(await page.locator('#spatial-reconstruct-result').textContent()).includes('carved by depth'));
    await page.locator('#spatial-scenes').selectOption(refined.id);
    await page.waitForFunction(()=>document.querySelector('#spatial-reconstruct-result').textContent.includes('18 cells carved by depth'));

    await app.close();
    ({app,page}=await launch(project,extra));
    assert.deepEqual(await api('read',{id:refined.id}),refined);
    assert.equal((await api('export-glb',{id:refined.id})).sha256,sha);
    const original=path.join(project,'spatial','reconstruction-sources',refined.id+'.json');
    const tampered=JSON.parse(await fs.readFile(original,'utf8'));
    const index=(15-10)*16+9;
    tampered.depth.samples[index]=-2;
    await fs.writeFile(original,JSON.stringify(tampered));
    await assert.rejects(api('export-glb',{id:refined.id}),/original visual reconstruction/);
    assert.deepEqual(await api('read',{id:uncarved.id}),uncarved);
    await fs.writeFile(path.join(output,'depth-reconstruction-evidence.json'),JSON.stringify({
      platform:process.platform,baseline:uncarved.reconstruction,depth:phi,exportSha256:sha,
      independentBlender:independent,editedReturn:returned,restoredAfterRestart:true,changedDepthRefused:true
    },null,2)+'\n');
    console.log(JSON.stringify({voxels:phi.voxels,carved:phi.depth.removedVoxels,independent,returned}));
  } catch(error) {
    console.error((await page.locator('#spatial-reconstruct-result').textContent().catch(()=>null))||'No reconstruction status');
    throw error;
  } finally {await app.close().catch(()=>{});}
}
main().catch(error=>{console.error(error);process.exitCode=1;});
