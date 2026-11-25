
'use strict';

const assert = require('node:assert/strict');
const { createHash } = require('node:crypto');
const { execFile } = require('node:child_process');
const fs = require('node:fs/promises');
const http = require('node:http');
const os = require('node:os');
const path = require('node:path');
const { promisify } = require('node:util');
const { launch } = require('./calibration-integration.cjs');
const {verifyPlacement} = require('./browser-placement-integration.cjs');

const withTop = process.argv[3] === 'top';
const withPlacement = process.argv[3] === 'placement';
const fixture = path.join(__dirname, 'fixtures', withTop ? 'browser-top' : 'browser-views');
const digest = bytes => createHash('sha256').update(bytes).digest('hex');
const roles = withTop ? ['front','side','top','level','elevated'] : ['front','side','held-out','wrong'];

async function main() {
  process.env.FRAYNIX_WORKSPACE_NO_MODELS = '1';
  const output = path.resolve(process.argv[2] || 'build/visual-browser-views227');
  const project = await fs.mkdtemp(path.join(os.tmpdir(), 'Fraynix browser views '));
  await fs.mkdir(output,{recursive:true});
  let imageDirectory = fixture;
  if(withPlacement && process.env.FRAYNIX_TEST_BLENDER){
    imageDirectory = path.join(output,'registered-views');
    await promisify(execFile)(process.env.FRAYNIX_TEST_BLENDER,['--background','--factory-startup','--disable-autoexec',
      '--python-exit-code','17','--python',path.join(__dirname,'browser-views-blender.py'),'--','create',imageDirectory],
    {timeout:90000,windowsHide:true});
  }
  const images = Object.fromEntries(await Promise.all(roles.map(async name=>[name,
    await fs.readFile(path.join(imageDirectory,name+'.png'))])));
  const server=http.createServer((req,res)=>{
    const name=req.url.split('?')[0].slice(1);
    if(name.endsWith('.png')&&Object.hasOwn(images,name.slice(0,-4))){
      res.setHeader('Content-Type','image/png');
      res.end(images[name.slice(0,-4)]);
    }else if(roles.includes(name)){
      res.setHeader('Content-Type','text/html; charset=utf-8');
      res.end('<!doctype html><title>'+name+' registered view</title><style>'+
        'body{margin:0;background:white}img{display:block;width:120px;height:auto}'+
        '@media(max-width:119px){img{width:100px}}@media(max-width:99px){img{width:80px}}'+
        '@media(max-width:79px){img{width:60px}}</style>'+
        '<a href="/wrong"><img alt="'+name+' of the stepped prop" id="graphic" src="/'+name+'.png"></a>');
    }else{res.writeHead(404);res.end();}
  });
  await new Promise(resolve=>server.listen(0,'127.0.0.1',resolve));
  const origin='http://127.0.0.1:'+server.address().port;
  const extra=process.platform==='linux'?['--no-sandbox','--disable-dev-shm-usage']:[];
  let {app,page}=await launch(project,extra);
  const api=(method,args={})=>app.evaluate((_,r)=>global.workspaceTest.api('/api/spatial/'+r.method,r.args),{method,args});
  const status=text=>page.waitForFunction(text=>
    document.getElementById('browser-views-status')?.textContent.includes(text),text,{timeout:20000});
  const visit=name=>app.evaluate((_,url)=>global.workspaceTest.execute('browser.navigate',{url}),origin+'/'+name);
  const clickGraphic=()=>app.evaluate(async({webContents})=>{
    const browser=webContents.fromId(global.workspaceTest.surfaces().browser);
    const region=await browser.executeJavaScriptInIsolatedWorld(1001,[{code:`(()=>{
      const item=document.getElementById('graphic'), r=item?.getBoundingClientRect();
      return globalThis.fraynixGraphicPick&&item.complete&&item.naturalWidth>0&&r.width<=innerWidth?
        {x:Math.floor(r.x+r.width/2),y:Math.floor(r.y+r.height/2)}:null;
    })()`}]);
    if(!region) return false;
    browser.sendInputEvent({type:'mouseDown',...region,button:'left',clickCount:1});
    browser.sendInputEvent({type:'mouseUp',...region,button:'left',clickCount:1});
    return true;
  });
  const capture=async name=>{
    await page.locator('#spatial-browser-views').evaluate(el=>el.scrollIntoView({block:'start'}));
    await page.evaluate(()=>new Promise(done=>requestAnimationFrame(()=>requestAnimationFrame(done))));
    await fs.writeFile(path.join(output,name),Buffer.from(await app.evaluate(()=>global.workspaceTest.capture()),'base64'));
  };
  const pick=async(role,source,label)=>{
    await visit(source+'?private=test#fragment');
    await page.locator('#browser-pick-'+role).click();
    await status('Click the '+label);
    let sent=false;
    for(let i=0;i<60&&!sent;i++){
      sent=await clickGraphic();
      if(!sent)await new Promise(resolve=>setTimeout(resolve,50));
    }
    assert(sent,'The actual browser pointer event could not be sent.');
    await status('Captured '+label);
    const stayed=await app.evaluate(({webContents})=>
      webContents.fromId(global.workspaceTest.surfaces().browser).getURL());
    assert.equal(stayed,origin+'/'+source+'?private=test#fragment');
  };

  try{
    await page.locator('#spatial-tab').click();
    await page.locator('#spatial-browser-views summary').click();
    assert(await page.locator('#browser-views-build').isDisabled());
    if(withPlacement){
      await verifyPlacement({app,page,pick,status,capture,api,project,output,extra,launch,
        stopSource:()=>new Promise((resolve,reject)=>server.close(error=>error?reject(error):resolve()))});
      return;
    }
    if(withTop){
      await pick('front','front','front');
      await pick('side','side','side');
      await pick('heldOut','level','held-out 45°');
      await page.locator('#browser-views-build').click();
      await status('held-out silhouette overlap');
      const controlId=await page.locator('#spatial-scenes').inputValue();
      const baseline=await api('read',{id:controlId});
      assert.equal(baseline.reconstruction.topIou,undefined);
      const originalGlb=await api('export-glb',{id:controlId});
      await fs.writeFile(path.join(output,'two-view.glb'),Buffer.from(originalGlb.base64,'base64'));

      await page.locator('#browser-views-elevation').selectOption('60');
      await pick('heldOut','elevated','held-out 45°');
      await page.locator('#browser-views-build').click();
      await status('Held-out view refused');
      const refusal=await page.locator('#browser-views-status').textContent();
      assert.equal((await api('list')).scenes.length,1);
      await capture('elevated-refused.png');

      await pick('top','top','top');
      await page.locator('#browser-views-build').click();
      await status('Top view included.');
      const id=await page.locator('#spatial-scenes').inputValue();
      const result=await api('read',{id});
      const proof=result.reconstruction;
      assert.equal(proof.heldOutElevationDeg,60);
      assert.equal(proof.grid,20);
      assert(proof.topIou>.97&&proof.heldOutIou>.90,JSON.stringify(proof));
      assert.notEqual(proof.meshSha256,baseline.reconstruction.meshSha256);
      for(const role of ['front','side']){
        assert.equal(proof.views[role].pngSha256,baseline.reconstruction.views[role].pngSha256);
      }
      await page.locator('#browser-views-export').click();
      await page.getByText('Checked GLB saved to',{exact:false}).last().waitFor();
      const glb=await api('export-glb',{id});
      await fs.writeFile(path.join(output,'with-top.glb'),Buffer.from(glb.base64,'base64'));
      await capture('top-verified.png');

      let blender=null;
      if(process.env.FRAYNIX_TEST_BLENDER){
        await promisify(execFile)(process.env.FRAYNIX_TEST_BLENDER,['--background','--factory-startup','--disable-autoexec',
          '--python-exit-code','17','--python',path.join(__dirname,'browser-top-blender.py'),'--','verify',output],
        {timeout:60000,windowsHide:true});
        blender=JSON.parse(await fs.readFile(path.join(output,'blender-top-evidence.json'),'utf8'));
      }

      await page.locator('#browser-top-clear').click();
      await status('Top view removed');
      await page.locator('#browser-views-build').click();
      await status('Held-out view refused');
      assert.equal((await api('list')).scenes.length,2);
      await app.close();
      ({app,page}=await launch(project,extra));
      assert.deepEqual(await api('read',{id}),result);
      const file=path.join(project,'spatial','reconstruction-sources',id+'.json');
      const stored=JSON.parse(await fs.readFile(file,'utf8'));
      await assert.rejects(api('browser-trace',{source:JSON.stringify({...stored,
        views:{...stored.views,top:stored.views.side}})}),/Align the top crop/);
      for(const role of ['front','side','top','heldOut']){
        const image=Buffer.from(stored.views[role].png,'base64');
        assert.equal(digest(image),proof.views[role].pngSha256);
        await fs.writeFile(path.join(output,'captured-'+role+'.png'),image);
      }
      await fs.appendFile(file,' ');
      await assert.rejects(api('export-glb',{id}),/original visual reconstruction/);
      await fs.writeFile(path.join(output,'browser-top-evidence.json'),JSON.stringify({
        proof,refusal,baseline:baseline.reconstruction,blender,removedTopRefused:true,misregisteredTopRefused:true,
        restoredAfterRestart:true,changedSourceRefused:true,controlSha256:originalGlb.sha256,exportSha256:glb.sha256
      },null,2)+'\n');
      console.log(JSON.stringify({topFit:proof.topIou,heldOut:proof.heldOutIou,refusal,blender}));
      return;
    }
    await visit('front');
    await page.locator('#browser-pick-front').click();
    await status('Click the front');
    let cancelled=false;
    for(let i=0;i<60&&!cancelled;i++){
      cancelled=await app.evaluate(async({webContents})=>{
        const browser=webContents.fromId(global.workspaceTest.surfaces().browser);
        const armed=await browser.executeJavaScriptInIsolatedWorld(1001,[{code:'Boolean(globalThis.fraynixGraphicPick)'}]);
        if(!armed)return false;
        browser.sendInputEvent({type:'keyDown',keyCode:'Escape'});
        return true;
      });
      if(!cancelled)await new Promise(resolve=>setTimeout(resolve,50));
    }
    assert(cancelled);
    await status('selection cancelled');
    assert.equal((await api('list')).scenes.length,0);
    await pick('front','front','front');
    await pick('side','side','side');
    await pick('heldOut','wrong','held-out 45°');
    await page.locator('#browser-views-build').click();
    await status('Held-out view refused');
    const failure=await page.locator('#browser-views-status').textContent();
    assert.equal((await api('list')).scenes.length,0);
    await capture('held-out-refused.png');

    await pick('heldOut','held-out','held-out 45°');
    await page.locator('#browser-views-build').click();
    await status('held-out silhouette overlap');
    const id=await page.locator('#spatial-scenes').inputValue(), result=await api('read',{id});
    const evidence=result.reconstruction;
    assert.equal(evidence.policy,'browser-registered-orthographic');
    assert.equal(evidence.grid,20);
    assert(evidence.frontIou>.98&&evidence.sideIou>.98&&evidence.heldOutIou>.90,JSON.stringify(evidence));
    assert(Math.abs(evidence.estimatedDepthM-.3)<.01);
    assert(Math.abs(evidence.meshDepthM-.3)<1e-6);
    assert.equal(evidence.views.front.url,origin+'/front');
    assert.equal(evidence.views.heldOut.url,origin+'/held-out');
    await page.locator('#browser-views-export').click();
    await page.getByText('Checked GLB saved to',{exact:false}).last().waitFor();
    const glb=await api('export-glb',{id});
    await fs.writeFile(path.join(output,'browser-verified.glb'),Buffer.from(glb.base64,'base64'));
    await capture('held-out-verified.png');
    let blender=null;
    if(process.env.FRAYNIX_TEST_BLENDER){
      await promisify(execFile)(process.env.FRAYNIX_TEST_BLENDER,['--background','--factory-startup','--disable-autoexec',
        '--python-exit-code','17','--python',path.join(__dirname,'browser-views-blender.py'),'--','verify',output],
      {timeout:60000,windowsHide:true});
      blender=JSON.parse(await fs.readFile(path.join(output,'blender-view-evidence.json'),'utf8'));
    }

    await app.close();
    ({app,page}=await launch(project,extra));
    assert.deepEqual(await api('read',{id}),result);
    const file=path.join(project,'spatial','reconstruction-sources',id+'.json');
    const stored=JSON.parse(await fs.readFile(file,'utf8'));
    await assert.rejects(api('browser-trace',{source:JSON.stringify({...stored,
      views:{...stored.views,heldOut:stored.views.front}})}),/separate 45° held-out view/);
    await assert.rejects(api('browser-trace',{source:JSON.stringify({...stored,views:{...stored.views,
      side:{...stored.views.side,png:images.side.toString('base64')}}})}),/crop size, scale/);
    for(const name of ['front','side','heldOut']){
      const bytes=Buffer.from(stored.views[name].png,'base64');
      assert.equal(digest(bytes),evidence.views[name].pngSha256);
      await fs.writeFile(path.join(output,'captured-'+name+'.png'),bytes);
    }
    await fs.appendFile(file,' ');
    await assert.rejects(api('export-glb',{id}),/original visual reconstruction/);
    await fs.writeFile(path.join(output,'browser-views-evidence.json'),JSON.stringify({
      evidence,refusedHeldOut:failure,refusalSavedNoScene:true,clickPreservedNavigation:true,escapeCancelled:true,
      restoredAfterRestart:true,changedSourceRefused:true,duplicateWitnessRefused:true,mismatchedScaleRefused:true,exportSha256:glb.sha256,blender
    },null,2)+'\n');
    console.log(JSON.stringify({depth:evidence.estimatedDepthM,overlap:evidence.heldOutIou,reason:failure,blender}));
  }catch(error){
    console.error(await page.locator('#browser-views-status').textContent().catch(()=>null));
    throw error;
  }finally{await app.close().catch(()=>{});server.close();}
}

main().catch(error=>{console.error(error);process.exitCode=1;});
