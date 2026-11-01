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
 async poseComparison(text,source){
  const data=Buffer.from(text,'utf8'),packed=JSON.parse(text);if(packed.format!=='fraynix.native-pose-comparison-gzip-v1'||!Number.isSafeInteger(packed.rawBytes)||packed.rawBytes<1||!/^[a-f0-9]{64}$/.test(packed.rawSha256)||typeof packed.gzipBase64!=='string')throw Error('Invalid native pose comparison transport');
  if(data.length<=20_000_000)return this.save('native-pose-comparison','json',data,source);if(data.length>64_000_000)throw Error('Native comparison transport exceeds64MB bounded bundle');
  const parts=[];for(let offset=0;offset<data.length;offset+=10_000_000){const receipt=await this.save('native-pose-comparison-part','part.bin',data.subarray(offset,Math.min(data.length,offset+10_000_000)),source);parts.push({name:path.basename(receipt.path),bytes:receipt.bytes,sha256:receipt.sha256});}
  return this.save('native-pose-comparison','json',JSON.stringify({format:'fraynix.native-export-parts-v1',originalBytes:data.length,originalSha256:createHash('sha256').update(data).digest('hex'),sourceFormat:packed.format,rawBytes:packed.rawBytes,rawSha256:packed.rawSha256,parts},null,2)+'\n',source);
 }
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
async function readNativeExport(file){const buffer=await fs.readFile(file);let packed=JSON.parse(buffer.toString('utf8'));if(packed.format==='fraynix.native-export-parts-v1'){if(!Array.isArray(packed.parts)||packed.parts.length<2||packed.parts.length>8)throw Error('Invalid native export part count');const chunks=[];for(const part of packed.parts){if(path.basename(part.name)!==part.name||!/^[a-f0-9]{8}-[a-f0-9-]+\.part\.bin$/.test(part.name))throw Error('Native export part path is not a local data file');const bytes=await fs.readFile(path.join(path.dirname(file),part.name));if(bytes.length!==part.bytes||bytes.length>10_000_000||createHash('sha256').update(bytes).digest('hex')!==part.sha256)throw Error('Native export part bytes/hash changed');chunks.push(bytes);}const full=Buffer.concat(chunks);if(full.length!==packed.originalBytes||createHash('sha256').update(full).digest('hex')!==packed.originalSha256)throw Error('Native export bundle differs from its original transport');packed=JSON.parse(full.toString('utf8'));}
 if(!['fraynix.persistent-world-export-gzip-v1','fraynix.native-world-run-gzip-v1','fraynix.native-pose-comparison-gzip-v1','fraynix.native-pose-run-gzip-v1'].includes(packed.format))throw Error('Unsupported native pose export');if(!Number.isSafeInteger(packed.rawBytes)||packed.rawBytes<1||packed.rawBytes>512_000_000)throw Error('Native export raw size exceeds bound');const raw=require('node:zlib').gunzipSync(Buffer.from(packed.gzipBase64,'base64'),{maxOutputLength:512_000_000});if(raw.length!==packed.rawBytes||createHash('sha256').update(raw).digest('hex')!==packed.rawSha256)throw Error('Native export raw bytes/hash changed');return {packed,raw,value:JSON.parse(raw.toString('utf8'))};}
module.exports={NativeExports,readNativeExport};
