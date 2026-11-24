'use strict';

const assert=require('node:assert/strict');
const fs=require('node:fs/promises');
const os=require('node:os');
const path=require('node:path');
const {execFile}=require('node:child_process');
const {promisify}=require('node:util');
const {launch}=require('./calibration-integration.cjs');

function meshTop(scene, object) {
  const data=Buffer.from(scene.visualMeshes[object.visualMesh],'base64');
  const count=data.readUInt32LE(0);
  let top=-Infinity;
  for (let i=0;i<count;i++) top=Math.max(top,data.readFloatLE(12+i*12));
  return top;
}

async function main() {
  process.env.FRAYNIX_WORKSPACE_NO_MODELS='1';
  const output=path.resolve(process.argv[2]||'build/visual-reconstruction');
  const blender=process.env.FRAYNIX_TEST_BLENDER;
  const bundle=process.argv.includes('--use-bundled');
  const runBlender=(mode,...files)=>promisify(execFile)(blender,[
    '--background','--factory-startup','--disable-autoexec','--python-exit-code','17','--python',
    path.join(__dirname,'spatial-reconstruction-blender.py'),'--',mode,...files
  ],{timeout:90000,windowsHide:true});
  await fs.mkdir(output,{recursive:true});
  if(blender && !bundle) await runBlender('create',output);
  const project=await fs.mkdtemp(path.join(os.tmpdir(),'Fraynix reconstructed mesh '));
  const extra=process.platform==='linux'?['--no-sandbox','--disable-dev-shm-usage']:[];
  let {app,page}=await launch(project,extra);
  const api=(method,args={})=>app.evaluate((_,r)=>global.workspaceTest.api('/api/spatial/'+r.method,r.args),{method,args});
  try {
    await page.locator('#spatial-tab').click();
    await page.locator('#spatial-reconstruct summary').click();
    if(blender && !bundle) await page.locator('#spatial-reconstruct-file').setInputFiles(path.join(output,'bracket.json'));
    else await page.locator('#spatial-reconstruct-example').click();
    await page.waitForFunction(()=>document.querySelector('#spatial-reconstruct-result').textContent.includes('editable triangles'),null,{timeout:10000});
    const id=await page.locator('#spatial-scenes').inputValue();
    const scene=await api('read',{id});
    const result=scene.reconstruction;
    assert(result.triangles>30);
    assert.equal(result.inputProjectionIou.z,1);
    assert(scene.objects.find(o=>o.sourceId==='surface').visualMesh);
    await page.locator('#spatial-reconstruct-export').click();
    await page.getByText('Checked GLB saved to',{exact:false}).last().waitFor();
    const file=path.join(output,'reconstructed.glb');
    const exportBytes=await api('export-glb',{id});
    await fs.writeFile(file,Buffer.from(exportBytes.base64,'base64'));
    await page.locator('#spatial-reconstruct').evaluate(el=>el.scrollIntoView({block:'start'}));
    await page.evaluate(()=>new Promise(r=>requestAnimationFrame(()=>requestAnimationFrame(r))));
    await fs.writeFile(path.join(output,'fraynix-reconstruction.png'),Buffer.from(await app.evaluate(()=>global.workspaceTest.capture()),'base64'));

    let independent=null, editedRoundTrip=null;
    if (blender) {
      const hidden=await fs.readFile(path.join(output,'hidden-recess.json'));
      const ambiguous=await api('reconstruct',{base64:hidden.toString('base64')});
      assert.equal(ambiguous.reconstruction.meshSha256,result.meshSha256);
      assert.notEqual(ambiguous.reconstruction.sourceSha256,result.sourceSha256);
      await runBlender('verify',file,output);
      independent=JSON.parse(await fs.readFile(path.join(output,'blender-reconstruction.json'),'utf8'));
      assert(independent.passed);
      const edited=await fs.readFile(path.join(output,'blender-edited-reconstruction.glb'));
      const returned=await api('import-glb',{base64:edited.toString('base64')});
      const original=scene.objects.find(o=>o.sourceId==='surface');
      const changed=returned.objects.find(o=>o.sourceId==='surface');
      assert.notEqual(changed.visualMesh,original.visualMesh);
      assert.deepEqual(changed.size,original.size);
      const deltaM=meshTop(returned,changed)-meshTop(scene,original);
      assert(Math.abs(deltaM-.05)<1e-6);
      assert.deepEqual(await api('read',{id}),scene);
      editedRoundTrip={visualAssetChanged:true, originalPreserved:true, colliderSizeM:changed.size,
        returnedMeshSha256:changed.visualMesh, measuredHeightEditM:deltaM};
    }

    await app.close();
    ({app,page}=await launch(project,extra));
    assert.deepEqual(await api('read',{id}),scene);
    assert.equal((await api('export-glb',{id})).sha256,exportBytes.sha256);
    const originalFile=path.join(project,'spatial','reconstruction-sources',id+'.json');
    await fs.appendFile(originalFile,' ');
    await assert.rejects(api('export-glb',{id}),/original visual reconstruction/);
    await fs.writeFile(path.join(output,'reconstruction-evidence.json'),JSON.stringify({
      platform:process.platform,construction:result,originalBytesCheckedOnRestart:true,changedSourceRefused:true,
      withheldBlender:independent,editedRoundTrip,exportSha256:exportBytes.sha256
    },null,2)+'\n');
    console.log(JSON.stringify({vertices:result.vertices, triangles:result.triangles, independent}));
  } catch(error) {
    console.error((await page.locator('#spatial-reconstruct-result').textContent().catch(()=>null))||'No reconstruction status');
    throw error;
  } finally {await app.close().catch(()=>{});}
}
main().catch(error=>{console.error(error);process.exitCode=1;});
