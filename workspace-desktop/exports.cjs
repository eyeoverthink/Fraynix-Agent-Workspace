'use strict';
const fs=require('node:fs/promises'),path=require('node:path'),{randomUUID,createHash}=require('node:crypto');
const {createReadStream}=require('node:fs');
const {createInterface}=require('node:readline');
const {isDeepStrictEqual}=require('node:util');
class NativeExports {
 constructor(project){this.directory=path.join(project,'exports');this.latest=null;}
 async save(kind,extension,bytes,source,expectedSha256){
  await fs.mkdir(this.directory,{recursive:true});const id=randomUUID(),target=path.join(this.directory,id+'.'+extension),temporary=target+'.tmp';
  const data=Buffer.isBuffer(bytes)?bytes:Buffer.from(bytes,'utf8');if(data.length>20_000_000)throw Error('Export exceeds 20 MB');
  const receipt={id,kind,path:target,bytes:data.length,sha256:createHash('sha256').update(data).digest('hex'),source,createdAt:new Date().toISOString()};
  if(expectedSha256!==undefined&&receipt.sha256!==expectedSha256)throw Error('The verified export bytes changed');
  await fs.writeFile(temporary,data,{flag:'wx'});
  try {
   if(expectedSha256!==undefined&&!data.equals(await fs.readFile(temporary)))
    throw Error('The written export differs from the checked bytes');
   await fs.rename(temporary,target);
  } catch(error) {
   await fs.unlink(temporary).catch(()=>{});
   throw error;
  }
  await fs.writeFile(target+'.receipt.json',JSON.stringify(receipt,null,2)+'\n',{flag:'wx'});
  await fs.appendFile(path.join(this.directory,'manifest.jsonl'),JSON.stringify(receipt)+'\n');this.latest=receipt;return receipt;
 }
 async matchedComparison(result){
  if(!result.passed||!result.rechecked||result.csvExport?.format!=='fraynix.routed-key-comparison-csv-v1')
   throw Error('The matched comparison has no verified CSV');
  const {csv,sha256,format,groups,browserEvidenceSha256}=result.csvExport;
  if(typeof sha256!=='string'||!/^[a-f0-9]{64}$/.test(sha256))throw Error('The verified comparison hash is unavailable');
  return this.save('verified-routed-comparison','csv',csv,{
   format,groups,comparisonId:result.id,artifact:result.artifact,keyColumn:result.keyColumn,
   earlier:{exportId:result.earlierExportId,sha256:result.earlierCsvSha256},
   later:{exportId:result.laterExportId,sha256:result.csvSha256},
   browserChecks:result.browserChecks,browserEvidenceSha256
  },sha256);
 }
 async *entries(kind,missing){
  const manifest=path.join(this.directory,'manifest.jsonl');
  const stat=await fs.lstat(manifest).catch(error=>{if(error.code==='ENOENT')return null;throw error;});
  if(!stat?.isFile())throw Error(missing);
  for await(const line of createInterface({input:createReadStream(manifest),crlfDelay:Infinity})){
   if(!line.trim())continue;
   const entry=JSON.parse(line);
   if(entry.kind===kind)yield entry;
  }
 }
 routingEntries(){return this.entries('verified-routed-results','Export a verified routing CSV first');}
 comparisonEntries(){return this.entries('verified-routed-comparison','Export a verified matched comparison CSV first');}
 async comparisonCsv(exportId){
  const missing='Export a verified matched comparison CSV first';
  let selected;
  for await(const entry of this.comparisonEntries()){
   if(exportId===undefined||entry.id===exportId)selected=entry;
  }
  if(!selected)throw Error(missing);
  if(typeof selected.id!=='string'||!/^[0-9a-f]{8}-(?:[0-9a-f]{4}-){3}[0-9a-f]{12}$/.test(selected.id))
   throw Error('The comparison export ID changed');
  const file=path.join(this.directory,selected.id+'.csv');
  const sidecar=file+'.receipt.json';
  const [csvInfo,receiptInfo]=await Promise.all([fs.lstat(file),fs.lstat(sidecar)]);
  if(!csvInfo.isFile()||csvInfo.size>262144||!receiptInfo.isFile()||receiptInfo.size>64000)
   throw Error('The comparison CSV or export receipt is unavailable');
  const [bytes,receiptText]=await Promise.all([fs.readFile(file),fs.readFile(sidecar,'utf8')]);
  const receipt=JSON.parse(receiptText);
  const sha256=createHash('sha256').update(bytes).digest('hex');
  if(!isDeepStrictEqual(receipt,selected)||receipt.bytes!==bytes.length||receipt.sha256!==sha256)
   throw Error('The comparison CSV bytes or receipt disagree with the export manifest');
  const {path:localPath,...evidence}=receipt;
  return {receipt:evidence,base64:bytes.toString('base64')};
 }
 async routedCsvPair(){
  const latestByPlan=new Map();
  for await(const entry of this.routingEntries()){
   const plan=entry.source?.planId;
   if(typeof plan!=='string'||!plan)throw Error('A routing export has no verified plan ID');
   latestByPlan.delete(plan);
   latestByPlan.set(plan,entry.id);
  }
  if(latestByPlan.size<2)throw Error('Export CSVs from two different verified routing plans first');
  const [earlier,later]=[...latestByPlan.values()].slice(-2);
  const [baseline,file]=await Promise.all([this.routedCsv(earlier),this.routedCsv(later)]);
  return {file,baseline};
 }
 async routedCsv(exportId){
  if(!exportId)for await(const entry of this.routingEntries())exportId=entry.id;
  if(!exportId)throw Error('Export a verified routing CSV first');
  if(!/^[0-9a-f]{8}-(?:[0-9a-f]{4}-){3}[0-9a-f]{12}$/.test(exportId))throw Error('The routing export ID changed');
  const file=path.join(this.directory,exportId+'.csv');
  const receiptFile=file+'.receipt.json';
  const [receiptInfo,csvInfo]=await Promise.all([fs.lstat(receiptFile),fs.lstat(file)]);
  if(!receiptInfo.isFile()||receiptInfo.size>64000||!csvInfo.isFile()||csvInfo.size>12000)
   throw Error('The routed CSV or its export receipt is unavailable');
  const [receiptText,bytes]=await Promise.all([fs.readFile(receiptFile,'utf8'),fs.readFile(file)]);
  const receipt=JSON.parse(receiptText);
  const sha256=createHash('sha256').update(bytes).digest('hex');
  if(receipt.id!==exportId||receipt.kind!=='verified-routed-results'||receipt.bytes!==bytes.length||receipt.sha256!==sha256)
   throw Error('The routed CSV bytes do not match the original export receipt');
  return {exportId,sha256,source:receipt.source,base64:bytes.toString('base64')};
 }
 async payloadStudy(value,source){
  const old=value.format==='fraynix.payload-study-057-v1'&&value.original?.format==='fraynix.payload-native-gzip-v1';const redesign=((value.format==='fraynix.payload-redesign-058-v1'&&value.originals?.manifest?.edition===58)||(value.format==='fraynix.payload-redesign-059-v1'&&value.originals?.manifest?.edition===59)||(value.format==='fraynix.payload-redesign-060-v1'&&value.originals?.manifest?.edition===60))&&value.originals?.manifest?.format==='fraynix.payload-attempt-manifest-057-v1'&&Array.isArray(value.originals.records)&&value.originals.records.length>0;if(!old&&!redesign)throw Error('Invalid payload study transport');
  const data=Buffer.from(JSON.stringify(value));if(data.length<=20_000_000)return this.save('dynamic-payloads','json',data,source);if(data.length>64_000_000)throw Error('Payload transport exceeds64MB');
  const parts=[];for(let offset=0;offset<data.length;offset+=10_000_000){const r=await this.save('dynamic-payload-part','part.bin',data.subarray(offset,Math.min(data.length,offset+10_000_000)),source);parts.push({name:path.basename(r.path),bytes:r.bytes,sha256:r.sha256});}
  return this.save('dynamic-payloads','json',JSON.stringify({format:'fraynix.payload-export-parts-057-v1',originalBytes:data.length,originalSha256:createHash('sha256').update(data).digest('hex'),parts}),source);
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
async function readPayloadExport(file){let value=JSON.parse(await fs.readFile(file,'utf8'));if(value.format==='fraynix.payload-export-parts-057-v1'){if(!Number.isSafeInteger(value.originalBytes)||value.originalBytes>64_000_000||!Array.isArray(value.parts)||value.parts.length<2||value.parts.length>7)throw Error('Invalid payload parts');const chunks=[];for(const part of value.parts){if(path.basename(part.name)!==part.name||!/[a-f0-9-]+\.part\.bin$/.test(part.name)||!Number.isSafeInteger(part.bytes)||part.bytes<1||part.bytes>10_000_000)throw Error('Invalid payload part path or size');const bytes=await fs.readFile(path.join(path.dirname(file),part.name));if(bytes.length!==part.bytes||createHash('sha256').update(bytes).digest('hex')!==part.sha256)throw Error('Payload part bytes/hash changed');chunks.push(bytes);}const bytes=Buffer.concat(chunks);if(bytes.length!==value.originalBytes||createHash('sha256').update(bytes).digest('hex')!==value.originalSha256)throw Error('Payload complete bytes/hash changed');value=JSON.parse(bytes.toString('utf8'));}const old=value.format==='fraynix.payload-study-057-v1'&&value.original?.format==='fraynix.payload-native-gzip-v1',redesign=((value.format==='fraynix.payload-redesign-058-v1'&&value.originals?.manifest?.edition===58)||(value.format==='fraynix.payload-redesign-059-v1'&&value.originals?.manifest?.edition===59)||(value.format==='fraynix.payload-redesign-060-v1'&&value.originals?.manifest?.edition===60))&&value.originals?.manifest?.format==='fraynix.payload-attempt-manifest-057-v1'&&Array.isArray(value.originals.records)&&value.originals.records.length>0;if(!old&&!redesign)throw Error('Invalid payload study');return value;}
module.exports={NativeExports,readNativeExport,readPayloadExport};
