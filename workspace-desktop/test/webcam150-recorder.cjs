'use strict';
// Source-only review instrument, inside an isolated actual app. Never runs tracking.
const {launch}=require('./calibration-integration.cjs');
const fs=require('node:fs/promises'),sync=require('node:fs'),path=require('node:path'),crypto=require('node:crypto'),assert=require('node:assert/strict');
const {timing150}=require('../recorder150.cjs');
const sha=b=>crypto.createHash('sha256').update(b).digest('hex');
const repo=path.resolve(__dirname,'../..'),root=path.join(repo,'build/webcam150-private');
async function main(){
 const timeoutTest=process.argv.includes('--timeout-self-test');
 const selfTest=process.argv.includes('--self-test')||timeoutTest;
 await fs.mkdir(root,{recursive:true});
 const dir=await fs.mkdtemp(path.join(root,'attempt-')),project=path.join(dir,'project');await fs.mkdir(project);
 const receipt={schema:'webcam150-source-v1',startedAt:new Date().toISOString(),private:true,recorderRevision:2,timingSha256:sha((await fs.readFile(path.join(__dirname,'../renderer/recorder150-timing.js'),'utf8')).replaceAll('\r\n','\n')),recorderSha256:sha((await fs.readFile(__filename,'utf8')).replaceAll('\r\n','\n')),sampleIntervalS:1,syntheticRecorderCheck:selfTest,frames:[],events:[],bytes:0,complete:false};
 const manifest=path.join(dir,'capture.json');let queue=Promise.resolve(),app,page;
 const save=async()=>{const tmp=manifest+'.next';await fs.writeFile(tmp,JSON.stringify(receipt,null,2));await fs.rename(tmp,manifest);};
 const persist=data=>{const work=queue.then(async()=>{
  assert(!receipt.closed,'Recording is closed');
  if(data.kind==='frame'){
   assert(receipt.frames.length<600,'Sample limit');assert(data.sourceFrame===receipt.frames.length+1,'Contiguous source indices');
   assert(Number.isFinite(data.mediaTime)&&Number.isFinite(data.elapsedS)&&data.elapsedS>=0&&data.elapsedS<=301);
   assert(data.width>0&&data.width<=640&&data.height>0&&data.height<=480);
   if(receipt.frames.length)assert(data.mediaTime>receipt.frames.at(-1).mediaTime,'Camera timestamps must increase');
   const bytes=Buffer.from(data.image,'base64');assert(bytes.length>100&&bytes.length<1000000);assert(receipt.bytes+bytes.length<=120000000,'Byte limit');
   const file=String(data.sourceFrame).padStart(4,'0')+'.jpg';await fs.writeFile(path.join(dir,file),bytes,{flag:'wx'});
   const row={...data,image:undefined,file,imageSha256:sha(bytes),bytes:bytes.length,receivedAt:new Date().toISOString()};receipt.frames.push(row);receipt.bytes+=bytes.length;
   await fs.appendFile(path.join(dir,'frames.jsonl'),JSON.stringify(row)+'\n');
  }else{assert(data.kind==='event'&&typeof data.action==='string');receipt.events.push({...data,receivedAt:new Date().toISOString()});if(data.action==='finished'){receipt.complete=true;receipt.closed=true;}if(['aborted','limit','capture-error','timing-expired'].includes(data.action)){receipt.closed=true;receipt.complete=false;}}
  await save();return {samples:receipt.frames.length,bytes:receipt.bytes};
 });queue=work.catch(()=>{});return work;};
 await save();
 Object.assign(process.env,{FRAYNIX_WORKSPACE_PERSISTENT:'0',FRAYNIX_WORKSPACE_JAVA_HEAP_MIB:'768',FRAYNIX_WORKSPACE_JAVA_PROCESSORS:'2'});
 try{
  ({app,page}=await launch(project,selfTest?['--use-fake-device-for-media-stream','--use-fake-ui-for-media-stream']:[]));
  await app.evaluate(({BrowserWindow,screen})=>{const w=BrowserWindow.getAllWindows().find(w=>w.isVisible()),a=screen.getPrimaryDisplay().workArea;w.setBounds({x:a.x,y:a.y,width:Math.min(1500,a.width),height:Math.min(1000,a.height)});w.setTitle('Fraynix | Edition 150 | Private camera study · long-cover return test');w.show();w.focus();});
  await page.exposeFunction('saveSource150',persist);
  await page.locator('#spatial-tab').click();
  await page.addScriptTag({url:require('node:url').pathToFileURL(path.join(repo,'workspace-desktop/renderer/recorder150-timing.js')).href});
  await page.evaluate(()=>{
   const panel=document.createElement('dialog');panel.id='source150';panel.style.cssText='position:fixed;inset:8px;margin:0;width:calc(100% - 16px);height:calc(100% - 16px);max-width:none;max-height:none;box-sizing:border-box;z-index:9999;overflow:auto;background:#101b29;color:#edf5ff;border:1px solid #397a89;border-radius:14px;padding:24px;font:18px system-ui;';
   panel.innerHTML='<h1>Edition 150 · Private camera study · long-cover return test</h1><p>Black target on the left; red comparison lighter on the right. Clear reference → long full cover → return at a new position. No tracking runs during recording.</p><p id="s150-cue" style="padding:18px;background:#193849;font-size:25px;min-height:70px" role="status">Start the preview. Choose the target and keep the second object separate on the mat.</p><p id="s150-clock" role="timer" style="font-size:30px;font-weight:bold;color:#6be2cf">Waiting to start</p><div id="s150-video-slot"></div><p id="s150-count">Camera off. No microphone. Images stay on this computer.</p><div id="s150-controls" style="display:flex;gap:12px;flex-wrap:wrap"><button id="s150-preview">Start private preview</button><button id="s150-start" disabled>Start clear reference</button><button id="s150-next" disabled>Waiting for reference</button><button id="s150-stop">Cancel / stop camera</button></div><p>Recording is continuous. At COVER you have 12 seconds to cover black and click Covered. After 18 saved hidden samples, MOVE AND UNCOVER has 15 seconds: move the whole black lighter under the cover, reveal it, take your hand away and click Returned. Do not wait for another cue before uncovering. The recording saves automatically. Do not move the camera. The small cover must hide only the target.</p><details><summary>About this study</summary><p>One sample per second, up to two minutes. The visible countdown uses the real clock; a missed action deadline stops and preserves an incomplete attempt. Saving finishes automatically after the return samples. Actual camera times and every captured sample are saved. Button clicks describe your actions; source images will be checked before tracking. This review recorder does not change the app’s tracker or defaults.</p></details>';
   document.body.append(panel);panel.showModal();panel.addEventListener('cancel',e=>e.preventDefault());const $=id=>document.getElementById(id),v=$('metric-camera-video');$('s150-video-slot').append(v);v.style.cssText='display:block;width:auto;max-width:100%;max-height:38vh;margin:auto;';panel.insertBefore($('s150-controls'),$('s150-video-slot'));panel.insertBefore($('s150-count'),$('s150-video-slot'));
   for(const b of panel.querySelectorAll('button'))b.style.cssText='padding:14px 20px;font-size:18px;cursor:pointer;border-radius:8px;';
   const s=window.source150={phase:'idle',samples:0,recording:false,busy:false,first:undefined,started:undefined,lastSample:-Infinity,lastSavedAt:undefined,phaseStart:0,phaseStarted:performance.now(),error:null,stopping:false};
   const cue=t=>$('s150-cue').textContent=t;
   const event=action=>window.saveSource150({kind:'event',action,phase:s.phase,sourceFrame:s.samples,elapsedS:s.started===undefined?null:(performance.now()-s.started)/1000});
   const stop=async(reason)=>{if(s.stopping)return;s.stopping=true;s.recording=false;clearInterval(s.clock);clearInterval(s.watchdog);if(s.callback!=null)v.cancelVideoFrameCallback(s.callback);while(s.busy)await new Promise(r=>setTimeout(r,20));await event(reason);window.metricView.stopCamera();s.phase=reason;$('s150-next').disabled=true;$('s150-start').disabled=true;$('s150-preview').disabled=true;$('s150-stop').disabled=true;cue(reason==='finished'?'Recording saved locally. Camera off. We will review the original frames before running tracking.':(reason==='timing-expired'?'Action window ended. ':'Camera stopped. ')+'The saved partial attempt is preserved; it is not a completed tracking test.');};
   s.stop=stop;
   $('s150-preview').onclick=async()=>{if(s.phase!=='idle')return;s.phase='opening';$('s150-preview').disabled=true;try{await window.metricView.startCamera();if(s.stopping){window.metricView.stopCamera();return;}s.phase='preview';$('s150-start').disabled=false;cue('Preview only. Confirm the target and lookalike are fully visible, then click Start clear reference.');await event('preview-started');}catch(e){s.error=e.message;s.phase='idle';if(!s.stopping)$('s150-preview').disabled=false;cue('Camera unavailable: '+e.message);}};
   async function update(){
    const n=s.samples-s.phaseStart,b=$('s150-next');$('s150-count').textContent='PRIVATE RECORDING · '+s.samples+' samples · '+((performance.now()-s.started)/1000).toFixed(1)+' seconds · '+s.phase;
    if(s.stopping)return;
    if(s.phase==='reference'&&window.recorderTiming150(s.phase,s.samples,performance.now()-s.phaseStarted).ready){s.phase='covering';s.phaseStart=s.samples;s.phaseStarted=performance.now();cue('COVER NOW — 12 seconds. Cover the entire BLACK lighter including the metal end; leave RED visible. Take your hand away and click Black lighter covered.');b.disabled=false;b.textContent='Black lighter covered; hand away';await event('cue-cover');}
    else if(s.phase==='covered'&&window.recorderTiming150(s.phase,n,performance.now()-s.phaseStarted).ready){s.phase='revealing';s.phaseStarted=performance.now();cue('MOVE AND UNCOVER NOW — 15 seconds. Slide the WHOLE black lighter one width toward the tape while hidden. Immediately remove the cover and your hand, then click Black lighter returned. Do not wait for another cue. Leave RED still.');b.disabled=false;b.textContent='Black lighter returned; hand away';await event('cue-reveal');}
    else if(s.phase==='returned'&&window.recorderTiming150(s.phase,n,performance.now()-s.phaseStarted).ready){await stop('finished');}
   }
   const c=document.createElement('canvas');let ctx;
   async function sample(now,meta){
    if(!s.recording)return;
    try{
     if(performance.now()-s.started>=120000||s.samples>=150){await stop('limit');return;}
     if(meta.mediaTime-s.lastSample>=1&&!s.busy){
      s.busy=true;if(s.first===undefined)s.first=meta.mediaTime;ctx.drawImage(v,0,0,c.width,c.height);
      await window.saveSource150({kind:'frame',sourceFrame:s.samples+1,mediaTime:meta.mediaTime,seconds:meta.mediaTime-s.first,elapsedS:(performance.now()-s.started)/1000,width:c.width,height:c.height,phase:s.phase,image:c.toDataURL('image/jpeg',.9).split(',')[1]});
      s.samples++;s.lastSample=meta.mediaTime;s.lastSavedAt=performance.now();s.busy=false;await update();
     }
     if(s.recording)s.callback=v.requestVideoFrameCallback(sample);
    }catch(e){s.busy=false;s.error=e.message;await stop('capture-error');cue('Capture stopped: '+e.message+'. Saved samples are preserved.');}
   }
   $('s150-start').onclick=async()=>{if(s.recording||s.phase!=='preview')return;const scale=Math.min(1,640/v.videoWidth,480/v.videoHeight);c.width=Math.round(v.videoWidth*scale);c.height=Math.round(v.videoHeight*scale);ctx=c.getContext('2d');s.started=performance.now();s.recording=true;s.phase='reference';$('s150-start').disabled=true;cue('CLEAR REFERENCE: keep BOTH lighters uncovered and hands away. Wait for the large cue to say COVER THE BLACK LIGHTER. No click is needed during this reference stage.');await event('recording-started');s.callback=v.requestVideoFrameCallback(sample);};
   $('s150-next').onclick=async()=>{const b=$('s150-next');if(b.disabled||s.stopping)return;const timing=window.recorderTiming150(s.phase,s.samples-s.phaseStart,performance.now()-s.phaseStarted);if(timing.expired){await stop('timing-expired');return;}b.disabled=true;
    if(s.phase==='covering'){s.phase='covered';s.phaseStart=s.samples;s.phaseStarted=performance.now();cue('Black lighter covered. Keep the red one visible. Hold it fully hidden for 18 saved samples. Then a new cue will explain how to move it under the cover and reveal it at a new position. Prepare to move and uncover as soon as the 15-second countdown appears.');b.textContent='Saving covered samples';}
    else if(s.phase==='revealing'){s.phase='returned';s.phaseStart=s.samples;s.phaseStarted=performance.now();cue('Keep the black lighter at its NEW position and both lighters visible and still. Saving the return samples, then the camera will stop AUTOMATICALLY.');b.textContent='Saving automatically — no more clicks';}
    await event('user-'+s.phase);
   };
   $('s150-stop').onclick=()=>stop('aborted');
   s.clock=setInterval(()=>{if(!s.recording||s.stopping)return;const t=window.recorderTiming150(s.phase,s.samples-s.phaseStart,performance.now()-s.phaseStarted);$('s150-clock').textContent=t.kind==='action'?t.remaining+' SECONDS LEFT — act now':t.kind==='samples'?t.remaining+' saved samples left · '+s.phase:'Waiting';if(t.expired)stop('timing-expired').catch(()=>{});},100);
   s.watchdog=setInterval(()=>{if(!s.recording||s.stopping)return;const elapsed=performance.now()-s.started;if(elapsed>=120000)stop('limit').catch(()=>{});else if(performance.now()-(s.lastSavedAt??s.started)>5000){s.error='Camera stopped delivering source samples';stop('capture-error').catch(()=>{});}},1000);
   document.addEventListener('visibilitychange',()=>{if(document.hidden&&s.recording)stop('aborted').catch(()=>{});});
  });
  await page.evaluate(()=>window.workspace.request({operation:'layout',bounds:{browser:{x:0,y:0,width:1,height:1,visible:false},canvas:{x:0,y:0,width:1,height:1,visible:false}}}));
  await fs.writeFile(path.join(root,'active.json'),JSON.stringify({dir,manifest,pid:process.pid,readyAt:new Date().toISOString()},null,2));
  console.log(JSON.stringify({ready:true,dir,scope:'Source-only private recorder; no tracking, no camera access until preview button'}));
  if(selfTest){
   assert.equal(await page.locator('dialog#source150[open]').count(),1);
   const controls=await page.locator('#s150-controls').boundingBox(),viewport=await page.evaluate(()=>({w:innerWidth,h:innerHeight}));assert(controls.y>=0&&controls.y+controls.height<=viewport.h&&controls.x+controls.width<=viewport.w,'Controls must fit the visible window');
   await fs.writeFile(path.join(repo,'build/webcam150-controls.png'),Buffer.from(await app.evaluate(()=>global.workspaceTest.capture()),'base64'));
   await page.locator('#s150-preview').click();await page.waitForFunction(()=>window.source150.phase==='preview');
   await page.locator('#s150-start').click();await page.waitForFunction(()=>window.source150.phase==='covering',{},{timeout:60000});
   assert.equal(await page.locator('#s150-next').isEnabled(),true);assert(receipt.frames.length>=34);
   if(timeoutTest){
    await page.waitForFunction(()=>window.source150.phase==='timing-expired',{},{timeout:18000});await queue;
    assert.equal(receipt.complete,false);assert.equal(receipt.closed,true);assert(receipt.frames.length>=34&&receipt.frames.length<=48);assert.equal(await page.evaluate(()=>!!window.metricView.stream),false);
    assert(receipt.events.some(e=>e.action==='timing-expired'));const check={passed:true,recorderSha256:receipt.recorderSha256,attempt:dir,samples:receipt.frames.length,scope:'Actual fake-camera deadline expiry preserves an incomplete attempt and releases camera; not physical footage'};
    await fs.writeFile(path.join(repo,'build/webcam150-timeout-selftest.json'),JSON.stringify(check,null,2));console.log(JSON.stringify(check));return;
   }
   await page.locator('#s150-next').click();await page.waitForFunction(()=>window.source150.phase==='revealing',{},{timeout:35000});
   await page.locator('#s150-next').click();await page.waitForFunction(()=>window.source150.phase==='finished',{},{timeout:45000});
   await queue;assert(receipt.complete&&receipt.closed&&receipt.syntheticRecorderCheck);assert(receipt.frames.length>=76);assert(receipt.frames.length<=100);assert(receipt.events.some(e=>e.action==='cue-reveal'));assert.equal(await page.locator('#s150-clock').count(),1);assert.equal(await page.evaluate(()=>!!window.metricView.stream),false);
   for(let i=1;i<receipt.frames.length;i++)assert(receipt.frames[i].seconds>receipt.frames[i-1].seconds);
   const check={passed:true,recorderSha256:receipt.recorderSha256,scope:'Actual recorder controls and automatic persistence with explicitly synthetic Chromium camera; NOT tracking or physical-camera evidence',attempt:dir,samples:receipt.frames.length,checks:['Modal recorder and native browser layers separated; controls fit viewport','Clear reference and context saved before cover cue','Covered interval holds18samples before the15-second move-and-uncover window','Covered and returned declarations advance exactly once','Automatic completion after 24 return samples','Camera released and continuous timestamped journal saved','No tracking or model calls']};
   await fs.writeFile(path.join(repo,'build/webcam150-selftest.json'),JSON.stringify(check,null,2));console.log(JSON.stringify(check));return;
  }
  while(app.process().exitCode===null&&!sync.existsSync(path.join(dir,'close-request'))){
   await new Promise(r=>setTimeout(r,1000));
   const state=await page.evaluate(()=>({phase:window.source150.phase,samples:window.source150.samples,error:window.source150.error}));await fs.writeFile(path.join(dir,'status.json'),JSON.stringify(state));
  }
 }finally{
  await queue;
  if(!receipt.closed){receipt.closed=true;receipt.complete=false;receipt.events.push({kind:'event',action:'process-ended',receivedAt:new Date().toISOString()});await save();}
  await page?.evaluate(()=>window.metricView.stopCamera()).catch(()=>{});await app?.close().catch(()=>{});
 }
}
if(require.main===module)main().catch(e=>{console.error(e);process.exitCode=1;});
