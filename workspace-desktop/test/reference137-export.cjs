'use strict';
const {launch}=require('./calibration-integration.cjs'),fs=require('node:fs/promises'),sync=require('node:fs'),path=require('node:path'),assert=require('node:assert/strict'),crypto=require('node:crypto'),zlib=require('node:zlib');
const repo=path.resolve(__dirname,'../..'),out=path.join(repo,'build/reference137-export'),project=path.join(out,'project'),sha=b=>crypto.createHash('sha256').update(b).digest('hex');
(async()=>{
 Object.assign(process.env,{FRAYNIX_WORKSPACE_PERSISTENT:'0',FRAYNIX_WORKSPACE_JAVA_HEAP_MIB:'768',FRAYNIX_WORKSPACE_JAVA_PROCESSORS:'2'});require('../../scripts/Study-Build095.cjs').verify(repo);assert(!sync.existsSync(out),'Preserve previous attempts');await fs.mkdir(project,{recursive:true});
 const {app,page}=await launch(project),record={passed:false,scope:'Two actual app runs on generated photo placements, independent labels, complete small export. Software control only, not camera accuracy.'};
 const api=(route,args={})=>app.evaluate((_,x)=>global.workspaceTest.api(x.route,x.args),{route,args}),request=(operation,command,args={})=>page.evaluate(x=>window.workspace.request(x),{operation,command,args});
 async function until(f){for(let i=0;i<1200;i++){const r=await f();if(r)return r;await new Promise(r=>setTimeout(r,100));}throw Error('Bounded control wait expired');}
 try{
  await page.waitForFunction(()=>window.association134View,{},{timeout:60000});await page.locator('#spatial-tab').click();await page.locator('#persistent-world-panel').evaluate(e=>{e.open=true;e.querySelector('details').open=true;});await until(async()=>await page.locator('#persistent-case option').count()===8);await page.locator('#persistent-case').selectOption('dev035-three-body-chain');await page.locator('#persistent-load').click();const world=await until(()=>page.locator('#persistent-worlds').inputValue()),before=await api('/api/world/observe',{worldId:world});
  const results=[];let retained;
  for(const name of ['source','target']){
   const pack=JSON.parse(await fs.readFile(path.join(repo,'build/reference137-fixtures',name+'.fobs.json'),'utf8')),dataset=await request('observations','import',{pack}),source=await api('/api/labels132/source',{datasetId:dataset.id,ordinal:1});
   const labels=JSON.parse(await fs.readFile(path.join(repo,'build/reference137-fixtures',name+'-labels.json'),'utf8'));Object.assign(labels,{datasetId:dataset.id,datasetSha256:source.datasetSha256});const label=await api('/api/association134/save',labels);
   await request('observations','select',{worldId:world,datasetId:dataset.id,bindings:[{objectKey:'subject',bodyId:before.frame.bodies[0].id}],trackerPolicy:name==='source'?'appearance-continuity-135':'verified-memory-137',...(retained?{appearanceSet137:retained.id,referenceDeclaration137:'Known identical source photo transformed into separate generated placements; software control.'}:{})});
   const start=await request('observations','start'),run=await until(async()=>{const r=await api('/api/observations/read',{id:start.id});return r.status==='running'?null:r;});assert.equal(run.status,'completed',run.reason);const audit=await api('/api/association134/audit',{labelId:label.id,runId:run.id});
   if(name==='source')retained=await api('/api/appearances135/retain',{auditId:audit.id});results.push({name,dataset,label,run,audit});
  }
  const receipt=await request('observations','export',{id:results[1].run.id}),bytes=await fs.readFile(receipt.path);assert(bytes.length<=20_000_000);const envelope=JSON.parse(bytes),raw=zlib.gunzipSync(Buffer.from(envelope.data,'base64'));assert.equal(sha(raw),envelope.rawSha256);const complete=JSON.parse(raw);
  assert.deepEqual(complete.run,results[1].run);assert.deepEqual(complete.referenceBundles137[0].set,retained);assert.deepEqual(complete.referenceBundles137[0].audit,results[0].audit);assert.deepEqual(complete.referenceBundles137[0].labels,results[0].label);assert.deepEqual(complete.referenceBundles137[0].sourceRun,results[0].run);
  const donor=complete.relatedDatasets.find(x=>x.dataset.id===results[0].dataset.id);assert(donor);assert.equal(donor.images.length,13);for(let i=0;i<donor.images.length;i++)assert.equal(sha(Buffer.from(donor.images[i],'base64')),donor.dataset.frames[i].imageSha256);
  assert.deepEqual(await api('/api/world/observe',{worldId:world}),before);Object.assign(record,{passed:true,results,retained,receipt,exportBytes:bytes.length,rawSha256:envelope.rawSha256,donorImagesVerified:13,nativeWorldUnchanged:true});console.log(JSON.stringify({passed:true,exportBytes:bytes.length,donorImagesVerified:13}));
 }catch(e){record.error=e.stack;throw e;}finally{await fs.writeFile(path.join(out,'result.json'),JSON.stringify(record,null,2));await app.close();}
})().catch(e=>{console.error(e);process.exitCode=1;});
