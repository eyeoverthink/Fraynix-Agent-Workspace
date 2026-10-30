'use strict';
const fs=require('node:fs/promises'),path=require('node:path'),{randomUUID,createHash}=require('node:crypto');
class NativeExports {
 constructor(project){this.directory=path.join(project,'exports');this.latest=null;}
 async save(kind,extension,bytes,source){
  await fs.mkdir(this.directory,{recursive:true});const id=randomUUID(),target=path.join(this.directory,id+'.'+extension),temporary=target+'.tmp';
  const data=Buffer.isBuffer(bytes)?bytes:Buffer.from(bytes,'utf8');if(data.length>20_000_000)throw Error('Export exceeds 20 MB');
  const receipt={id,kind,path:target,bytes:data.length,sha256:createHash('sha256').update(data).digest('hex'),source,createdAt:new Date().toISOString()};
  await fs.writeFile(temporary,data,{flag:'wx'});await fs.rename(temporary,target);
  await fs.writeFile(target+'.receipt.json',JSON.stringify(receipt,null,2)+'\n',{flag:'wx'});
  await fs.appendFile(path.join(this.directory,'manifest.jsonl'),JSON.stringify(receipt)+'\n');this.latest=receipt;return receipt;
 }
 async scene(file){return this.save('native-scene','fraynix-scene.json',JSON.stringify(file,null,2)+'\n',file.source);}
 async run(file){return this.save('native-run','fraynix-run.json',file.serialized+'\n',{runId:file.run.id,sceneId:file.run.sceneId,sceneVersion:file.run.sceneVersion,nativeRecordSha256:file.runSha256});}
 async csv(file){
  const rows=['tick,time_s,x_m,y_m,z_m,vx_mps,vy_mps,vz_mps,qx,qy,qz,qw,contacts,distance_m'];
  for(const s of file.run.samples)rows.push([s.tick,s.timeS,...s.position,...s.velocity,...s.rotation,s.contacts,s.distanceM].join(','));
  return this.save('native-trajectory','csv',rows.join('\n')+'\n',{runId:file.run.id,sceneId:file.run.sceneId,sceneVersion:file.run.sceneVersion,nativeRecordSha256:file.runSha256,samples:file.run.samples.length});
 }
 async video(file,rendered){
  if(rendered.mime!=='video/webm;codecs=vp8'||rendered.runId!==file.run.id)throw Error('Video renderer returned a different run or unsupported format');
  const bytes=Buffer.from(rendered.base64,'base64');if(bytes.length<100||!bytes.subarray(0,4).equals(Buffer.from([0x1a,0x45,0xdf,0xa3])))throw Error('Renderer returned no valid WebM container');
  return this.save('native-video','webm',bytes,{runId:file.run.id,sceneId:file.run.sceneId,sceneVersion:file.run.sceneVersion,nativeRecordSha256:file.runSha256,width:rendered.width,height:rendered.height,nominalFps:30,frames:rendered.frames,firstTick:rendered.firstTick,lastTick:rendered.lastTick,nativeDurationS:rendered.nativeDurationS,silent:true});
 }
}
module.exports={NativeExports};
