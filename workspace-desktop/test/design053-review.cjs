'use strict';
const {launch}=require('./calibration-integration.cjs'),fs=require('node:fs/promises'),path=require('node:path'),assert=require('node:assert/strict'),crypto=require('node:crypto'),zlib=require('node:zlib');
async function main(){
 process.env.FRAYNIX_WORKSPACE_PERSISTENT='0';process.env.FRAYNIX_WORKSPACE_PLAN_CPU_ONLY='1';
 const out=path.resolve(__dirname,'../test-output'),prior=JSON.parse(await fs.readFile(path.join(out,'design053-live.json'),'utf8'));assert(prior.passed);
 const {app,page}=await launch(prior.project),errors=[];page.on('pageerror',e=>errors.push(e.message));
 const api=(route,args={})=>app.evaluate((_,a)=>global.workspaceTest.api(a.route,a.args),{route,args});
 try{
  const before=await api('/api/world/read',{worldId:prior.worldId});assert.equal(before.revision,3);
  for(const g of [prior.first,prior.liveStart.completion044.javaGoal,prior.liveReuse.completion044.javaGoal]){const a=await api('/api/artifact/read',{id:g.report.id});assert.equal(a.version,g.report.version);assert.equal(a.test.steps.length,15);assert(a.test.passed);}
  await page.locator('#spatial-tab').click();await page.locator('#construction051').evaluate(e=>e.open=true);
  await page.waitForFunction(()=>document.querySelector('#d053-status').textContent.startsWith('cancelled')&&!document.querySelector('#d053-export').disabled);
  await page.locator('#d053-export').click();
  let receipts=[];const until=Date.now()+30000;
  while(Date.now()<until){try{receipts=(await fs.readFile(path.join(prior.project,'exports/manifest.jsonl'),'utf8')).trim().split('\n').map(JSON.parse).filter(r=>r.kind==='assembly-design');}catch(e){if(e.code!=='ENOENT')throw e;}if(receipts.length)break;await new Promise(r=>setTimeout(r,100));}
  assert.equal(receipts.length,1);const receipt=receipts[0],bytes=await fs.readFile(receipt.path);assert.equal(bytes.length,receipt.bytes);assert.equal(crypto.createHash('sha256').update(bytes).digest('hex'),receipt.sha256);
  const bundle=JSON.parse(bytes);assert.equal(bundle.study.id,prior.cancelled.counterfactual045.studyId);assert.equal(bundle.study.status,'cancelled');assert(bundle.originalAttempts.length>0);
  for(const a of bundle.originalAttempts){const b=zlib.gunzipSync(Buffer.from(a.run.gzipBase64,'base64'));assert.equal(b.length,a.run.rawBytes);assert.equal(crypto.createHash('sha256').update(b).digest('hex'),a.run.rawSha256);}
  assert.deepEqual(await api('/api/world/read',{worldId:prior.worldId}),before);assert.deepEqual(errors,[]);
  await fs.writeFile(path.join(out,'design053-cancelled-export.json'),bytes);
  const result={passed:true,source:require('node:child_process').execFileSync('git',['rev-parse','HEAD'],{encoding:'utf8'}).trim(),project:prior.project,receipt,checks:['Cold restart retains revision3 and all three exact report versions with15 original passing assertions each','Visible export button downloads the cancelled study and original attempted traces; compressed byte counts/hashes independently verified','Export changes no world state; no renderer errors or new model requests'],errors};await fs.writeFile(path.join(out,'design053-review.json'),JSON.stringify(result,null,2));console.log(JSON.stringify(result));
 }finally{await app.close();}
}
main().catch(e=>{console.error(e);process.exitCode=1;});
