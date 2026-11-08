'use strict';
const {launch}=require('./calibration-integration.cjs'),fs=require('node:fs/promises'),sync=require('node:fs'),path=require('node:path'),assert=require('node:assert/strict'),cp=require('node:child_process'),crypto=require('node:crypto');
const {unpack}=require('../../scripts/Inspect-Counterfactual045.cjs');
const sleep=ms=>new Promise(r=>setTimeout(r,ms)),sha=b=>crypto.createHash('sha256').update(b).digest('hex');
async function main(){
 const repo=path.resolve(__dirname,'../..'),out=path.join(repo,'build/ui092-study'),project=path.join(out,'project');
 assert(!sync.existsSync(path.join(out,'checkpoint.json')),'Use fresh output; retain failed evidence');await fs.mkdir(project,{recursive:true});
 const protocol=JSON.parse(await fs.readFile(path.join(__dirname,'fixtures/procedures092-protocol.json'),'utf8'));
 const record={protocol,source:cp.execFileSync('git',['rev-parse','HEAD'],{cwd:repo,encoding:'utf8'}).trim(),buildReceipt:require('../../scripts/Study-Build092.cjs').verify(repo),project,startedAt:new Date().toISOString(),rows:[],audits:[],boots:[],captures:[],checks:[]};
 Object.assign(process.env,{FRAYNIX_WORKSPACE_PERSISTENT:'0',FRAYNIX_WORKSPACE_PLAN_CPU_ONLY:'1',FRAYNIX_WORKSPACE_JAVA_HEAP_MIB:'512',FRAYNIX_WORKSPACE_JAVA_PROCESSORS:'2'});
 let app,page;const errors=[],save=()=>fs.writeFile(path.join(out,'checkpoint.json'),JSON.stringify(record,null,2));
 const api=(route,args={})=>app.evaluate((_,x)=>global.workspaceTest.api(x.route,x.args),{route,args});
 function osRead(action,pid){return JSON.parse(cp.execFileSync('pwsh',['-NoProfile','-File',path.join(repo,'scripts/Measure-Resources082.ps1'),'-Action',action,'-TargetPid',String(pid),'-Processors','2','-Checkout',repo],{encoding:'utf8',windowsHide:true}));}
 async function open(){const at=performance.now();({app,page}=await launch(project));page.on('pageerror',e=>errors.push(e.message));await page.waitForFunction(()=>window.agency043View);const r=await api('/api/resources082/state');assert.equal(r.profile.cpus,2);assert.equal(r.configuredMaxHeapBytes,512*1024*1024);const pinned=osRead('pin',r.pid);assert.equal(BigInt(pinned.affinity).toString(2).split('1').length-1,2);const initial=await api('/api/procedures091/diagnostics');assert.equal(initial.entries,0);record.boots.push({callerMillis:performance.now()-at,resources:r,pinned,initial});await save();}
 async function close(){if(app){await app.close();app=null;}}
 async function wait(fn,ms=240000){const end=Date.now()+ms;while(Date.now()<end){const x=await fn();if(x)return x;await sleep(60);}throw Error('Bounded092 wait exceeded');}
 async function capture(row,label){
  await app.evaluate((_,id)=>global.workspaceTest.goals.execute('goal.read',{id}),row.goalId);await sleep(1000);let visible=false;
  for(let i=0;i<5&&!visible;i++){await app.evaluate((_,id)=>global.workspaceTest.execute('canvas.open',{id}),row.goal.report.id);await page.waitForFunction(id=>document.querySelector('#object-detail').textContent.includes(id),row.goal.report.id);await page.locator('#preview-tab').click();await sleep(1200);visible=await page.evaluate(id=>document.querySelector('#preview-tab').classList.contains('selected')&&document.querySelector('#object-detail').textContent.includes(id),row.goal.report.id);}assert(visible);
  const text=await app.evaluate(()=>global.workspaceTest.canvasRead('document.querySelector("#procedure079-decision").innerText'));assert(text.includes('Memory decision'));
  for(const width of [1600,1100]){await app.evaluate(({BrowserWindow},w)=>BrowserWindow.getAllWindows().find(x=>x.isVisible()).setSize(w,1100),width);await sleep(300);assert(await page.evaluate(()=>document.documentElement.scrollWidth<=innerWidth));const file=label+'-'+width+'.png';await fs.writeFile(path.join(out,file),Buffer.from(await app.evaluate(()=>global.workspaceTest.capture()),'base64'));record.captures.push({file,width,goalId:row.goalId,report:row.goal.report.id,text});}await save();
 }
 async function run(spec,mode,bank,lifecycle,retain=false){
  await close();await open();await api('/api/procedures091/test-mode',{mode});
  const members=(await api('/api/procedures079/memories')).memories;assert.equal(members.length,retain?bank-1:bank);
  const scene=await api('/api/spatial/import',{text:JSON.stringify(spec.source)}),world=await api('/api/world/create',{sceneId:scene.id,title:'LABELLED092 '+spec.id+' '+lifecycle}),ob=await api('/api/world/observe',{worldId:world.id}),binding=Object.fromEntries(['worldId','expectedRevision','expectedHash'].map(k=>[k,ob[k]]));
  let before=await api('/api/procedures091/diagnostics');const resource=await api('/api/resources082/state'),beforeOS=osRead('read',resource.pid);
  const row={id:spec.id,bank,lifecycle,mode,retain,binding,resource,beforeOS,bankBefore:members.map(x=>x.id),segments:[]};record.rows.push(row);await save();
  const at=performance.now(),g=await api('/api/world/start',{...binding,targets:spec.targets,forceLimitN:spec.forceLimitN,torqueLimitNm:spec.torqueLimitNm,selectiveMemory079:true,memoryMode079:retain?'simple':'memory',retainExperience079:retain,...(!retain?{procedureReceipt085:record.rows[0].goalId}:{}),request:'Edition092 measured growing bank '+bank+' '+lifecycle});row.goalId=g.id;await save();
  let prefix;
  if(lifecycle==='restarted'){
   row.beforeInterruption=await wait(async()=>{const o=await api('/api/world/observe',{worldId:world.id});return o.pending&&o.nativeObservation?.sample?.tick>=1200&&o.nativeObservation.status==='running'?o:null;});assert.equal(row.beforeInterruption.expectedRevision,0);
   row.segments.push({before,after:await api('/api/procedures091/diagnostics')});const restartAt=performance.now();await close();await open();row.restartBoot=record.boots.at(-1);
   row.interrupted=await api('/api/goals/read',{id:g.id});assert.equal(row.interrupted.status,'interrupted');prefix=await api('/api/procedures079/export',{id:g.id});
   row.prefixFile=g.id+'-prefix.json';await fs.writeFile(path.join(out,row.prefixFile),JSON.stringify(prefix));assert(prefix.job.arm.attempts.some(a=>a.stage.startsWith('actual/')&&a.status==='interrupted'));assert.equal((await api('/api/world/observe',{worldId:world.id})).pending.goalId,g.id);
   await api('/api/procedures091/test-mode',{mode});before=await api('/api/procedures091/diagnostics');assert.equal(before.entries,0);await api('/api/goals/resume',{id:g.id});row.restartMillis=performance.now()-restartAt;
  }
  row.goal=await wait(async()=>{const x=await api('/api/goals/read',{id:g.id});return ['completed','interrupted','cancelled'].includes(x.status)?x:null;});row.callerMillis=performance.now()-at;
  row.segments.push({before,after:await api('/api/procedures091/diagnostics')});row.afterOS=osRead('read',(await api('/api/resources082/state')).pid);await save();
  assert.equal(row.goal.status,'completed',JSON.stringify(row.goal));assert(row.goal.poseView.passed);assert.equal(row.goal.taskCost080.eligible,lifecycle!=='restarted');assert.equal(row.goal.worldCommit.revision,1);assert.equal(beforeOS.affinity,row.afterOS.affinity);
  const exportAt=performance.now(),original=await api('/api/procedures079/export',{id:g.id});row.originalFile=g.id+'-originals.json';await fs.writeFile(path.join(out,row.originalFile),JSON.stringify(original));row.exportMillis=performance.now()-exportAt;row.job=original.job;
  if(prefix){
   assert.equal(row.goal.resumes,1);assert(!row.goal.taskCost080.clock089);assert(original.job.arm.attempts.some(a=>a.unknownTail&&a.chargedTicks===14400));
   for(const a of prefix.job.arm.attempts)assert(original.job.arm.attempts.some(b=>b.id===a.id&&b.stage===a.stage));
   const runs=new Map(original.originals.map(p=>{const x=unpack(p);return[x.id,x];}));for(const p of prefix.originals){const prior=unpack(p);assert.deepEqual(runs.get(prior.id).samples.slice(0,prior.samples.length),prior.samples);}
   row.prefixPreserved=true;
  }
  row.observed=await api('/api/world/observe',{worldId:world.id});assert.equal(row.observed.expectedHash,row.goal.worldCommit.hash);
  const auditAt=performance.now();row.fullVerify=await api('/api/procedures090/verify',{id:g.id});row.fullVerifyMillis=performance.now()-auditAt;assert(row.fullVerify.passed);
  assert.equal((await api('/api/procedures079/memories')).memories.length,bank);
  if(!retain){assert.equal(row.job.decision.candidate.id,record.rows[0].goalId);assert(row.job.decision.useMemory);}
  console.log('RESULT',spec.id,bank,lifecycle,mode,row.callerMillis.toFixed(1),'ms');await save();return row;
 }
 async function audit(row,mode,pass){const before=await api('/api/procedures091/diagnostics'),at=performance.now(),result=await api('/api/procedures090/verify',{id:row.goalId}),callerMillis=performance.now()-at,after=await api('/api/procedures091/diagnostics');assert.deepEqual(result,row.fullVerify);record.audits.push({id:row.goalId,mode,pass,before,after,callerMillis,result});await save();}
 try{
  for(let n=1;n<=8;n++){
   await run(protocol.training[n-1],'reuse',n,'preparation',true);
   if(protocol.banks.includes(n))for(const lifecycle of ['uninterrupted','restarted']){
    const order=(protocol.banks.indexOf(n)+(lifecycle==='restarted'?1:0))%2?['reuse','reference']:['reference','reuse'];
    for(const mode of order)await run(protocol.task,mode,n,lifecycle);
   }
  }
  for(const [mode,pass] of [['reference','reference'],['reuse','cold-table'],['reuse','second-pass']]){
   if(pass!=='second-pass')await api('/api/procedures091/test-mode',{mode});for(const row of record.rows)await audit(row,mode,pass);
  }
  const selected=record.rows.findLast(r=>r.lifecycle==='restarted'&&r.mode==='reuse');await api('/api/procedures090/verify',{id:selected.goalId});
  const attempt=selected.job.arm.attempts[0],file=path.join(project,'world-procedures-079',selected.goalId,'attempts',attempt.id,'original.json'),original=await fs.readFile(file),stat=await fs.stat(file);
  const tampered=Buffer.from(original.toString('utf8').replace(/"sha256":"([a-f0-9])/,(_,c)=>'"sha256":"'+(c==='0'?'1':'0')));assert.equal(tampered.length,original.length);assert.notEqual(sha(tampered),sha(original));
  try{await fs.writeFile(file,tampered);await fs.utimes(file,stat.atime,stat.mtime);await assert.rejects(api('/api/procedures090/verify',{id:selected.goalId}),/envelope|native|integrity/i);record.checks.push('Warmed restarted outcome refused same-size native-envelope mutation with preserved mtime');}finally{await fs.writeFile(file,original);assert.equal(sha(await fs.readFile(file)),sha(original));}
  assert.deepEqual(await api('/api/procedures090/verify',{id:selected.goalId}),selected.fullVerify);
  await capture(selected,'bank-eight-resumed');await capture(record.rows.findLast(r=>r.lifecycle==='uninterrupted'&&r.mode==='reuse'),'bank-eight-uninterrupted');
  assert.deepEqual(errors,[]);record.errors=errors;record.finishedAt=new Date().toISOString();record.passed=true;require('../../scripts/Study-Build092.cjs').verify(repo);await fs.writeFile(path.join(out,'result.json'),JSON.stringify(record,null,2));console.log('PASS092 growing-bank and actual interrupted native tasks');
 }catch(e){record.error=e.stack;throw e;}finally{await close();await save();}
}
main().catch(e=>{console.error(e);process.exitCode=1;});
