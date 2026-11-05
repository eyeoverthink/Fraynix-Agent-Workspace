'use strict';
const fs=require('node:fs/promises'),assert=require('node:assert/strict'),{createHash}=require('node:crypto'),{project}=require('./Inspect-MetricWorld.cjs');
const sha=b=>createHash('sha256').update(b).digest('hex');
function verify(v){
 assert.equal(v.schema,'tabletop-038-v1');const r=v.run,g=r.geometry.definition,c=r.check;
 assert.equal(r.cameraKey,g.cameraKey);assert.equal(r.cameraKey,c.cameraKey);assert.equal(r.geometry.sha256,c.geometry.sha256);assert(c.passed);assert.equal(r.trace.length,r.completedFrames);assert.equal(v.images.length,r.completedFrames+1);assert.equal(v.understanding.runId,r.id);assert.deepEqual(v.understanding.nativeBinding,r.nativeBinding);
 const tapeA=project(g,...c.pointA),tapeB=project(g,...c.pointB),length=Math.hypot(...tapeA.map((x,i)=>x-tapeB[i]));assert(Math.abs(length-c.measuredM)<1e-9);assert(Math.abs(Math.abs(length-c.expectedM)-c.absoluteErrorM)<1e-9);assert(c.absoluteErrorM<=c.toleranceM);assert.equal(sha(Buffer.from(v.checkImage,'base64')),c.frame.imageSha256);
 let previous=-1,accepted=0;for(let i=0;i<v.images.length;i++){
  const image=v.images[i],ref=i?r.trace[i-1].frame:r.seed;assert.deepEqual(image.reference,ref);assert.equal(sha(Buffer.from(image.image,'base64')),ref.imageSha256);assert.equal(Buffer.from(image.image,'base64').length,ref.bytes);assert(ref.seconds>previous);previous=ref.seconds;if(!i)continue;
  const row=r.trace[i-1],e=row.estimate;assert.equal(row.ordinal,i);assert.equal(row.bodyId,r.bodyId);assert.equal(e.seconds,ref.seconds);assert(e.dtS>0);assert.equal(e.dtS,ref.seconds-(i===1?r.seed.seconds:r.trace[i-2].frame.seconds));
  const radius=2.5*e.sigmaM+g.anchorSigmaM+g.scaleRelativeSigma*Math.hypot(...e.positionM);assert(Math.abs(radius-e.radiusM)<1e-9);
  if(e.measurementM){const p=project(g,...row.tracker.point);assert(Math.hypot(...p.map((x,j)=>x-e.measurementM[j]))<1e-9);}
  if(e.accepted){accepted++;assert(row.reference.valid&&row.tracker.accepted&&e.measurementM);}
 }
 for(const p of v.pendingOriginals||[])assert.equal(sha(Buffer.from(p.bytesBase64,'base64')),p.sha256);
 return{passed:true,runId:r.id,frames:r.completedFrames,accepted,originalImages:v.images.length,pendingFiles:(v.pendingOriginals||[]).length,scope:'Independent source hashes, camera/body/clock links, planar measurements, declared tape/radius arithmetic. Does not rerun appearance tracking, authenticate hardware or certify physical accuracy.'};
}
async function inspect(file){return verify(JSON.parse(await fs.readFile(file,'utf8')));}
if(require.main===module)inspect(process.argv[2]).then(x=>console.log(JSON.stringify(x))).catch(e=>{console.error(e.message);process.exitCode=1;});module.exports={inspect,verify};
