'use strict';
// Source-only review instrument, inside an isolated actual app. Never runs tracking.
const {launch}=require('./calibration-integration.cjs');
const fs=require('node:fs/promises'),sync=require('node:fs'),path=require('node:path'),crypto=require('node:crypto'),assert=require('node:assert/strict');
const sha=b=>crypto.createHash('sha256').update(b).digest('hex');
const repo=path.resolve(__dirname,'../..'),root=path.join(repo,'build/webcam147-private');
async function main(){
 const selfTest=process.argv.includes('--self-test');
 await fs.mkdir(root,{recursive:true});
 const dir=await fs.mkdtemp(path.join(root,'attempt-')),project=path.join(dir,'project');await fs.mkdir(project);
 const receipt={schema:'webcam147-source-v1',startedAt:new Date().toISOString(),private:true,recorderRevision:2,sampleIntervalS:1,syntheticRecorderCheck:selfTest,frames:[],events:[],bytes:0,complete:false};
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
  }else{assert(data.kind==='event'&&typeof data.action==='string');receipt.events.push({...data,receivedAt:new Date().toISOString()});if(data.action==='finished'){receipt.complete=true;receipt.closed=true;}if(['aborted','limit','capture-error'].includes(data.action)){receipt.closed=true;receipt.complete=false;}}
  await save();return {samples:receipt.frames.length,bytes:receipt.bytes};
 });queue=work.catch(()=>{});return work;};
 await save();
 Object.assign(process.env,{FRAYNIX_WORKSPACE_PERSISTENT:'0',FRAYNIX_WORKSPACE_JAVA_HEAP_MIB:'768',FRAYNIX_WORKSPACE_JAVA_PROCESSORS:'2'});
 try{
  ({app,page}=await launch(project,selfTest?['--use-fake-device-for-media-stream','--use-fake-ui-for-media-stream']:[]));
  await app.evaluate(({BrowserWindow,screen})=>{const w=BrowserWindow.getAllWindows().find(w=>w.isVisible()),a=screen.getPrimaryDisplay().workArea;w.setBounds({x:a.x,y:a.y,width:Math.min(1500,a.width),height:Math.min(1000,a.height)});w.setTitle('Fraynix | Edition 147 | Private camera study · simpler controls');w.show();w.focus();});
  await page.exposeFunction('saveSource147',persist);
  await page.locator('#spatial-tab').click();
  await page.evaluate(()=>{
   const panel=document.createElement('dialog');panel.id='source147';panel.style.cssText='position:fixed;inset:8px;margin:0;width:calc(100% - 16px);height:calc(100% - 16px);max-width:none;max-height:none;box-sizing:border-box;z-index:9999;overflow:auto;background:#101b29;color:#edf5ff;border:1px solid #397a89;border-radius:14px;padding:24px;font:18px system-ui;';
   panel.innerHTML='<h1>Edition 147 · Private camera study · simpler controls</h1><p>Clear reference → cover → return, with a separate lookalike. No tracking runs during recording.</p><p id="s147-cue" style="padding:18px;background:#193849;font-size:25px;min-height:70px" role="status">Start the preview. Choose the target and keep the second object separate on the mat.</p><div id="s147-video-slot"></div><p id="s147-count">Camera off. No microphone. Images stay on this computer.</p><div id="s147-controls" style="display:flex;gap:12px;flex-wrap:wrap"><button id="s147-preview">Start private preview</button><button id="s147-start" disabled>Start clear reference</button><button id="s147-next" disabled>Waiting for reference</button><button id="s147-stop">Cancel / stop camera</button></div><p>This recording keeps running while you move between steps. Follow the large cue. Confirm Covered, then Returned. The recording saves automatically. Do not move the camera. The small cover must hide only the target.</p><details><summary>About this study</summary><p>One sample per second, up to five minutes. Saving finishes automatically after the return samples. Actual camera times and every captured sample are saved. Button clicks describe your actions; source images will be checked before tracking. This review recorder does not change the app’s tracker or defaults.</p></details>';
   document.body.append(panel);panel.showModal();panel.addEventListener('cancel',e=>e.preventDefault());const $=id=>document.getElementById(id),v=$('metric-camera-video');$('s147-video-slot').append(v);v.style.cssText='display:block;width:auto;max-width:100%;max-height:38vh;margin:auto;';panel.insertBefore($('s147-controls'),$('s147-video-slot'));panel.insertBefore($('s147-count'),$('s147-video-slot'));
   for(const b of panel.querySelectorAll('button'))b.style.cssText='padding:14px 20px;font-size:18px;cursor:pointer;border-radius:8px;';
   const s=window.source147={phase:'idle',samples:0,recording:false,busy:false,first:undefined,started:undefined,lastSample:-Infinity,lastSavedAt:undefined,phaseStart:0,error:null,stopping:false};
   const cue=t=>$('s147-cue').textContent=t;
   const event=action=>window.saveSource147({kind:'event',action,phase:s.phase,sourceFrame:s.samples,elapsedS:s.started===undefined?null:(performance.now()-s.started)/1000});
   const stop=async(reason)=>{if(s.stopping)return;s.stopping=true;s.recording=false;if(s.callback!=null)v.cancelVideoFrameCallback(s.callback);while(s.busy)await new Promise(r=>setTimeout(r,20));await event(reason);window.metricView.stopCamera();s.phase=reason;$('s147-next').disabled=true;$('s147-start').disabled=true;$('s147-preview').disabled=true;$('s147-stop').disabled=true;cue(reason==='finished'?'Recording saved locally. Camera off. We will review the original frames before running tracking.':'Camera stopped. The saved partial attempt is preserved; it is not a completed tracking test.');};
   s.stop=stop;
   $('s147-preview').onclick=async()=>{if(s.phase!=='idle')return;s.phase='opening';$('s147-preview').disabled=true;try{await window.metricView.startCamera();if(s.stopping){window.metricView.stopCamera();return;}s.phase='preview';$('s147-start').disabled=false;cue('Preview only. Confirm the target and lookalike are fully visible, then click Start clear reference.');await event('preview-started');}catch(e){s.error=e.message;s.phase='idle';if(!s.stopping)$('s147-preview').disabled=false;cue('Camera unavailable: '+e.message);}};
   async function update(){
    const n=s.samples-s.phaseStart,b=$('s147-next');$('s147-count').textContent='PRIVATE RECORDING · '+s.samples+' samples · '+((performance.now()-s.started)/1000).toFixed(1)+' seconds · '+s.phase;
    if(s.phase==='reference'&&s.samples>=34){s.phase='covering';s.phaseStart=s.samples;cue('COVER THE BLACK LIGHTER NOW. Leave the red lighter visible. Click Black lighter covered when your hand is away.');b.disabled=false;b.textContent='Black lighter covered; hand away';await event('cue-cover');}
    else if(s.phase==='covered'&&n>=4){s.phase='revealing';cue('UNCOVER THE BLACK LIGHTER NOW. Slide it slightly, leave the red lighter still, then click Black lighter returned when your hand is away.');b.disabled=false;b.textContent='Black lighter returned; hand away';await event('cue-reveal');}
    else if(s.phase==='returned'&&n>=24){await stop('finished');}
   }
   const c=document.createElement('canvas');let ctx;
   async function sample(now,meta){
    if(!s.recording)return;
    try{
     if(performance.now()-s.started>=300000||s.samples>=600){await stop('limit');return;}
     if(meta.mediaTime-s.lastSample>=1&&!s.busy){
      s.busy=true;if(s.first===undefined)s.first=meta.mediaTime;ctx.drawImage(v,0,0,c.width,c.height);
      await window.saveSource147({kind:'frame',sourceFrame:s.samples+1,mediaTime:meta.mediaTime,seconds:meta.mediaTime-s.first,elapsedS:(performance.now()-s.started)/1000,width:c.width,height:c.height,phase:s.phase,image:c.toDataURL('image/jpeg',.9).split(',')[1]});
      s.samples++;s.lastSample=meta.mediaTime;s.lastSavedAt=performance.now();s.busy=false;await update();
     }
     if(s.recording)s.callback=v.requestVideoFrameCallback(sample);
    }catch(e){s.busy=false;s.error=e.message;await stop('capture-error');cue('Capture stopped: '+e.message+'. Saved samples are preserved.');}
   }
   $('s147-start').onclick=async()=>{if(s.recording||s.phase!=='preview')return;const scale=Math.min(1,640/v.videoWidth,480/v.videoHeight);c.width=Math.round(v.videoWidth*scale);c.height=Math.round(v.videoHeight*scale);ctx=c.getContext('2d');s.started=performance.now();s.recording=true;s.phase='reference';$('s147-start').disabled=true;cue('CLEAR REFERENCE: keep BOTH lighters uncovered and hands away. Wait for the large cue to say COVER THE BLACK LIGHTER. No click is needed during this reference stage.');await event('recording-started');s.callback=v.requestVideoFrameCallback(sample);};
   $('s147-next').onclick=async()=>{const b=$('s147-next');b.disabled=true;
    if(s.phase==='covering'){s.phase='covered';s.phaseStart=s.samples;cue('Black lighter covered. Keep the red one visible. The UNCOVER cue will appear after four saved samples.');b.textContent='Saving covered samples';}
    else if(s.phase==='revealing'){s.phase='returned';s.phaseStart=s.samples;cue('Keep both lighters visible and still. Saving the return samples, then the camera will stop AUTOMATICALLY.');b.textContent='Saving automatically — no more clicks';}
    await event('user-'+s.phase);
   };
   $('s147-stop').onclick=()=>stop('aborted');
   setInterval(()=>{if(!s.recording||s.stopping)return;const elapsed=performance.now()-s.started;if(elapsed>=300000)stop('limit').catch(()=>{});else if(performance.now()-(s.lastSavedAt??s.started)>5000){s.error='Camera stopped delivering source samples';stop('capture-error').catch(()=>{});}},1000);
   document.addEventListener('visibilitychange',()=>{if(document.hidden&&s.recording)stop('aborted').catch(()=>{});});
  });
  await page.evaluate(()=>window.workspace.request({operation:'layout',bounds:{browser:{x:0,y:0,width:1,height:1,visible:false},canvas:{x:0,y:0,width:1,height:1,visible:false}}}));
  await fs.writeFile(path.join(root,'active.json'),JSON.stringify({dir,manifest,pid:process.pid,readyAt:new Date().toISOString()},null,2));
  console.log(JSON.stringify({ready:true,dir,scope:'Source-only private recorder; no tracking, no camera access until preview button'}));
  if(selfTest){
   assert.equal(await page.locator('dialog#source147[open]').count(),1);
   const controls=await page.locator('#s147-controls').boundingBox(),viewport=await page.evaluate(()=>({w:innerWidth,h:innerHeight}));assert(controls.y>=0&&controls.y+controls.height<=viewport.h&&controls.x+controls.width<=viewport.w,'Controls must fit the visible window');
   await fs.writeFile(path.join(repo,'build/webcam147-v2-controls.png'),Buffer.from(await app.evaluate(()=>global.workspaceTest.capture()),'base64'));
   await page.locator('#s147-preview').click();await page.waitForFunction(()=>window.source147.phase==='preview');
   await page.locator('#s147-start').click();await page.waitForFunction(()=>window.source147.phase==='covering',{},{timeout:60000});
   assert.equal(await page.locator('#s147-next').isEnabled(),true);assert(receipt.frames.length>=34);
   await page.locator('#s147-next').click();await page.waitForFunction(()=>window.source147.phase==='revealing',{},{timeout:15000});
   await page.locator('#s147-next').click();await page.waitForFunction(()=>window.source147.phase==='finished',{},{timeout:45000});
   await queue;assert(receipt.complete&&receipt.closed&&receipt.syntheticRecorderCheck);assert(receipt.frames.length>=62);assert.equal(await page.evaluate(()=>!!window.metricView.stream),false);
   for(let i=1;i<receipt.frames.length;i++)assert(receipt.frames[i].seconds>receipt.frames[i-1].seconds);
   const check={passed:true,scope:'Actual recorder controls and automatic persistence with explicitly synthetic Chromium camera; NOT tracking or physical-camera evidence',attempt:dir,samples:receipt.frames.length,checks:['Modal recorder and native browser layers separated; controls fit viewport','Clear reference and context saved before cover cue','Covered and returned declarations advance exactly once','Automatic completion after24return samples','Camera released and continuous timestamped journal saved','No tracking or model calls']};
   await fs.writeFile(path.join(repo,'build/webcam147-v2-selftest.json'),JSON.stringify(check,null,2));console.log(JSON.stringify(check));return;
  }
  while(app.process().exitCode===null&&!sync.existsSync(path.join(dir,'close-request'))){
   await new Promise(r=>setTimeout(r,1000));
   const state=await page.evaluate(()=>({phase:window.source147.phase,samples:window.source147.samples,error:window.source147.error}));await fs.writeFile(path.join(dir,'status.json'),JSON.stringify(state));
  }
 }finally{
  await queue;
  if(!receipt.closed){receipt.closed=true;receipt.complete=false;receipt.events.push({kind:'event',action:'process-ended',receivedAt:new Date().toISOString()});await save();}
  await page?.evaluate(()=>window.metricView.stopCamera()).catch(()=>{});await app?.close().catch(()=>{});
 }
}
if(require.main===module)main().catch(e=>{console.error(e);process.exitCode=1;});
