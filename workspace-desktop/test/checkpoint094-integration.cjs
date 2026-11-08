'use strict';
const {launch}=require('./calibration-integration.cjs'),fs=require('node:fs/promises'),sync=require('node:fs'),path=require('node:path'),assert=require('node:assert/strict'),cp=require('node:child_process'),crypto=require('node:crypto');
const sha=b=>crypto.createHash('sha256').update(b).digest('hex'),sleep=ms=>new Promise(r=>setTimeout(r,ms));
async function main(){
 const repo=path.resolve(__dirname,'../..'),out=path.join(repo,'build/ui094-checkpoint');assert(!sync.existsSync(out),'Retain prior evidence');await fs.mkdir(out);
 const study=JSON.parse(await fs.readFile(path.join(repo,'build/ui094-study/result.json'),'utf8')),s=study.stoppedSnapshot;
 const record={startedAt:new Date().toISOString(),source:cp.execFileSync('git',['rev-parse','HEAD'],{cwd:repo,encoding:'utf8'}).trim(),buildReceipt:require('../../scripts/Study-Build094.cjs').verify(repo),snapshot:s,boots:[],checks:[]};
 const save=()=>fs.writeFile(path.join(out,'result.json'),JSON.stringify(record,null,2));
 Object.assign(process.env,{FRAYNIX_WORKSPACE_PERSISTENT:'0',FRAYNIX_WORKSPACE_PLAN_CPU_ONLY:'1',FRAYNIX_WORKSPACE_JAVA_HEAP_MIB:'512',FRAYNIX_WORKSPACE_JAVA_PROCESSORS:'2'});
 let app;const api=(route,args={})=>app.evaluate((_,x)=>global.workspaceTest.api(x.route,x.args),{route,args});
 async function open(project){({app}=await launch(project));const r=await api('/api/resources082/state');assert.equal(r.profile.cpus,2);const pinned=JSON.parse(cp.execFileSync('pwsh',['-NoProfile','-File',path.join(repo,'scripts/Measure-Resources082.ps1'),'-Action','pin','-TargetPid',String(r.pid),'-Processors','2','-Checkout',repo],{encoding:'utf8',windowsHide:true}));record.boots.push({project,resources:r,pinned});await save();}
 async function close(){if(app){await app.close();app=null;}}
 try{
  const bad=path.join(out,'corrupt-copy');await fs.cp(s.path,bad,{recursive:true});const file=path.join(bad,s.checkpointRelative),original=await fs.readFile(file),changed=JSON.parse(original);changed.payload.physics.tick+=12;await fs.writeFile(file,JSON.stringify(changed));record.originalCheckpointSha256=sha(original);record.mutatedCheckpointSha256=sha(await fs.readFile(file));
  for(const phase of ['corrupt','checkpoint-restored-quarantine']){
   if(phase!=='corrupt')await fs.writeFile(file,original);
   await open(bad);const before=await api('/api/world/observe',{worldId:s.binding.worldId}),goal=await api('/api/goals/read',{id:s.goalId});assert.equal(goal.status,'interrupted');assert.equal(before.expectedRevision,0);assert.equal(before.pending.goalId,s.goalId);
   const j=JSON.parse(await fs.readFile(path.join(bad,'world-procedures-079',s.goalId,'goal.json'),'utf8'));assert(j.data.recoveryError.includes('checksum/version'));
   let error='';try{await api('/api/goals/resume',{id:s.goalId});}catch(e){error=e.message;}assert(error.includes('checksum/version'));await sleep(300);const after=await api('/api/world/observe',{worldId:s.binding.worldId});assert.deepEqual(after,before);
   assert.equal(JSON.parse(await fs.readFile(path.join(bad,'world-procedures-079',s.goalId,'goal.json'),'utf8')).data.resumes,0);
   record.checks.push({phase,error,goal,before,after,job:j,checkpointSha256:sha(await fs.readFile(file)),scope:'Corruption quarantines the procedure. Restoring only checkpoint bytes does not clear the recorded recovery error or authorize motion.'});await close();await save();
  }
  // Restore the complete original stopped state into a NEW isolated project; do not clear a quarantine flag or forge a receipt.
  const restored=path.join(out,'restored-copy');await fs.cp(s.path,restored,{recursive:true});assert.equal(sha(await fs.readFile(path.join(restored,s.checkpointRelative))),sha(original));record.restoredProject=restored;await open(restored);
  const before=await api('/api/world/observe',{worldId:s.binding.worldId});assert.equal(before.expectedRevision,0);assert.equal(before.pending.goalId,s.goalId);assert.equal((await api('/api/goals/read',{id:s.goalId})).status,'interrupted');
  await api('/api/goals/resume',{id:s.goalId});let g;const until=Date.now()+300000;do{g=await api('/api/goals/read',{id:s.goalId});if(['completed','interrupted','failed'].includes(g.status))break;await sleep(100);}while(Date.now()<until);
  assert.equal(g.status,'completed',JSON.stringify(g));assert.equal(g.worldCommit.revision,1);assert.equal(g.taskCost080.eligible,false);record.goal=g;record.fullVerify=await api('/api/procedures090/verify',{id:s.goalId});assert(record.fullVerify.passed);
  const originals=await api('/api/procedures079/export',{id:s.goalId});await fs.writeFile(path.join(out,'restored-originals.json'),JSON.stringify(originals));record.after=await api('/api/world/observe',{worldId:s.binding.worldId});assert.equal(record.after.expectedRevision,1);assert.equal(record.after.expectedHash,g.worldCommit.hash);
  record.passed=true;record.finishedAt=new Date().toISOString();console.log('PASS094 corruption refuses motion; full stopped-state restoration explicitly resumes and verifies');
 }catch(e){record.error=e.stack;throw e;}finally{await close();await save();}
}
main().catch(e=>{console.error(e);process.exitCode=1;});
