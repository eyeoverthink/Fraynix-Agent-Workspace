'use strict';
// After the frozen comparison closes: alter isolated test evidence, verify refusal,
// restore its exact bytes. No physics, fitting or active-world action is requested.
const {launch}=require('./calibration-integration.cjs'),fs=require('node:fs/promises'),path=require('node:path'),assert=require('node:assert/strict'),crypto=require('node:crypto');
const {canonical}=require('../../scripts/Inspect-Spatial040.cjs');
const sha=b=>crypto.createHash('sha256').update(b).digest('hex');
async function main(){
 process.env.FRAYNIX_WORKSPACE_PERSISTENT='0';process.env.FRAYNIX_WORKSPACE_PLAN_CPU_ONLY='1';
 const input=path.resolve(process.env.FRAYNIX082_PRIMARY||'../build/ui082-one-384'),primary=JSON.parse(await fs.readFile(path.join(input,'result.json'),'utf8'));assert(primary.passed);
 const project=primary.project,out=path.resolve(process.env.FRAYNIX082_FAULTS_OUTPUT||'../build/ui082-faults');await fs.mkdir(out,{recursive:true});
 const record={project,out,source:require('node:child_process').execFileSync('git',['rev-parse','HEAD'],{encoding:'utf8'}).trim(),checks:[],probes:[],startedAt:new Date().toISOString()},root=path.join(project,'world-procedures-079'),ledger=path.join(project,'persistent-worlds/state.json'),before=await fs.readFile(ledger),bankBefore=await fs.readFile(path.join(root,'bank.json'));let app;
 const api=(route,args={})=>app.evaluate((_,x)=>global.workspaceTest.api(x.route,x.args),{route,args}),save=()=>fs.writeFile(path.join(out,'checkpoint.json'),JSON.stringify(record,null,2));
 async function probe(label,relative,mutate){const file=path.join(root,relative),original=await fs.readFile(file),value=JSON.parse(original);mutate(value);if(value.format==='java-agent-042-v1')value.sha256=sha(canonical(value.data));const changed=Buffer.from(JSON.stringify(value));let rejection='';
  try{await fs.writeFile(file,changed);try{await api('/api/procedures080/calibration');}catch(e){rejection=e.message;}assert(rejection,label+' must be refused');}
  finally{await fs.writeFile(file,original);assert.equal(sha(await fs.readFile(file)),sha(original));}
  assert.deepEqual(await api('/api/procedures080/calibration'),primary.model);assert.equal(sha(await fs.readFile(ledger)),sha(before));assert.equal(sha(await fs.readFile(path.join(root,'bank.json'))),sha(bankBefore));record.probes.push({mode:record.mode,label,path:relative,originalSha256:sha(original),changedSha256:sha(changed),rejection,exactRestoration:true});await save();console.log('PASS',label);
 }
 try{await save();({app}=await launch(project));
  const repo=path.resolve(__dirname,'../..');record.resources=await api('/api/resources082/state');assert.equal(record.resources.profile.cpus,1);assert.equal(record.resources.configuredMaxHeapBytes,384*1024*1024);
  const osRead=action=>JSON.parse(require('node:child_process').execFileSync('pwsh',['-NoProfile','-File',path.join(repo,'scripts/Measure-Resources082.ps1'),'-Action',action,'-TargetPid',String(record.resources.pid),'-Processors','1','-Checkout',repo],{encoding:'utf8',windowsHide:true}));
  record.pinned=osRead('pin');assert.equal(BigInt(record.pinned.affinity).toString(2).split('1').length-1,1);
  assert.deepEqual(await api('/api/procedures080/calibration'),primary.model);
  const p=primary.model.pairs[0],model='cost-models080/'+primary.model.id+'.json',evidence=primary.model.evidence[p.simpleId],native=evidence.find(x=>x.path.endsWith('/original.json'));
  for(const mode of ['reference','parallel']){await api('/api/procedures081/test-mode',{mode});record.mode=mode;
  await probe('Re-sealed changed complete duration refuses',p.simpleId+'/cost080.json',x=>x.data.totalMillis+=1000);
  await probe('Re-sealed ineligible timing refuses',p.simpleId+'/cost080.json',x=>x.data.eligible=false);
  await probe('Re-sealed changed goal provenance refuses',p.simpleId+'/goal.json',x=>x.data.spec.forceLimitN+=1);
  await probe('Changed original native bytes refuse',native.path,x=>x.cost080Fault='changed-test-evidence');
  await probe('Re-sealed changed paired arithmetic refuses',model,x=>x.data.pairs[0].savingMillis+=1000);
  await probe('Re-sealed duplicate pair provenance refuses',model,x=>x.data.pairs[1]=structuredClone(x.data.pairs[0]));
  await probe('Re-sealed foreign implementation refuses',model,x=>x.data.code='foreign-implementation');
  await probe('Re-sealed changed evidence hash refuses',model,x=>x.data.evidence[p.simpleId][0].sha256='0'.repeat(64));
  }
  assert.equal(sha(await fs.readFile(ledger)),sha(before));assert.equal(sha(await fs.readFile(path.join(root,'bank.json'))),sha(bankBefore));
  record.checks.push('Sixteen actual Java API probes across both readers refused changed timings, ineligible samples, original bytes, arithmetic, duplicate provenance and foreign code even when their envelopes were re-sealed');record.checks.push('Every altered file restored byte-for-byte; calibration reloaded unchanged and world ledger unchanged after each refusal');record.finalOS=osRead('read');assert.equal(record.finalOS.affinity,record.pinned.affinity);record.passed=true;await fs.writeFile(path.join(out,'result.json'),JSON.stringify(record,null,2));console.log('COMPLETE',out);
 }catch(e){record.error=e.stack;await save();throw e;}finally{if(app)await app.close();}
}
main().catch(e=>{console.error(e);process.exitCode=1;});
