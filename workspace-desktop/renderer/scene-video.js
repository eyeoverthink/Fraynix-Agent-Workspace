import * as THREE from '../node_modules/three/build/three.module.js';
export async function exportSceneVideo(lab,run){
 if(lab.exporting)throw Error('A video export is already running');
 if(!run.final||!run.samples?.length)throw Error('A settled run with actual samples is required');
 const mime='video/webm;codecs=vp8';if(!MediaRecorder.isTypeSupported(mime))throw Error('This runtime cannot encode VP8 WebM');
 const duration=run.final.timeS;if(duration<=0||duration>30)throw Error('Video export supports recorded runs up to 30 seconds');
 const savedObjects=JSON.parse(JSON.stringify(lab.objects)),savedPacket=lab.observerPacket;
 lab.exporting=true;const width=960,height=540,fps=30,renderer=new THREE.WebGLRenderer({antialias:true,preserveDrawingBuffer:true});renderer.setSize(width,height);renderer.setPixelRatio(1);
 renderer.setClearColor(0x101f2a,1);const camera=new THREE.PerspectiveCamera(45,width/height,.1,200),radius=Math.max(14,run.definition.parameters.lengthM*1.5);
 camera.position.set(radius*Math.cos(.52)*Math.sin(.85),2+radius*Math.sin(.52),radius*Math.cos(.52)*Math.cos(.85));camera.lookAt(0,2,0);
 const canvas=document.createElement('canvas');canvas.width=width;canvas.height=height;const context=canvas.getContext('2d'),stream=canvas.captureStream(0),track=stream.getVideoTracks()[0],chunks=[];
 const recorder=new MediaRecorder(stream,{mimeType:mime,videoBitsPerSecond:2_500_000});let recordingError;
 recorder.ondataavailable=event=>{if(event.data.size)chunks.push(event.data);};
 const stopped=new Promise((resolve,reject)=>{recorder.onstop=resolve;recorder.onerror=event=>{recordingError=event.error||Error('Video encoder failed');reject(recordingError);};});
 const frames=[];for(let t=0;t<duration;t+=1/fps)frames.push(run.samples.findLast(s=>s.timeS<=t+1e-9)||run.samples[0]);frames.push(run.final);
 try{
  recorder.start(100);const started=performance.now();
  for(let i=0;i<frames.length;i++){
   if(recordingError)throw recordingError;const sample=frames[i],objects=JSON.parse(JSON.stringify(run.definition.objects)),block=objects.find(o=>o.label==='Block');
   block.position=sample.position;block.rotation=sample.rotation;lab.paint(objects);lab.paintGrip({held:false});renderer.render(lab.world,camera);
   context.drawImage(renderer.domElement,0,0,width,height);context.fillStyle='rgba(8,18,24,.86)';context.fillRect(0,0,width,48);context.fillStyle='#b6f4df';context.font='14px sans-serif';
   context.fillText('FRAYNIX · recorded native physics · tick '+sample.tick+' · t '+sample.timeS.toFixed(4)+' s',16,22);context.fillStyle='#b8c9d0';context.font='11px sans-serif';context.fillText('scene v'+run.sceneVersion+' · '+run.id,16,40);
   track.requestFrame();const delay=started+(i+1)*1000/fps-performance.now();if(delay>0)await new Promise(resolve=>setTimeout(resolve,delay));
  }
  recorder.stop();await stopped;
  const blob=new Blob(chunks,{type:mime});if(blob.size>20_000_000)throw Error('Video exceeds 20 MB');
  const base64=await new Promise((resolve,reject)=>{const reader=new FileReader();reader.onload=()=>resolve(reader.result.split(',')[1]);reader.onerror=reject;reader.readAsDataURL(blob);});
  return {runId:run.id,mime,base64,width,height,frames:frames.length,firstTick:frames[0].tick,lastTick:frames.at(-1).tick,nativeDurationS:duration};
 }finally{
  if(recorder.state!=='inactive'){recorder.stop();await stopped.catch(()=>{});}for(const t of stream.getTracks())t.stop();renderer.dispose();renderer.forceContextLoss();
  lab.paint(savedObjects);lab.observerPacket=savedPacket;lab.paintGrip(savedPacket?.world.grip||lab.snapshot?.current.grip||{held:false});lab.exporting=false;lab.resize();
 }
}
