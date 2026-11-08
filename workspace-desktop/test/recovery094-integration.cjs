'use strict';
const {launch}=require('./calibration-integration.cjs'),fs=require('node:fs/promises'),sync=require('node:fs'),path=require('node:path'),crypto=require('node:crypto'),assert=require('node:assert/strict'),cp=require('node:child_process');
const {unpack}=require('../../scripts/Inspect-Counterfactual045.cjs');
const sleep=ms=>new Promise(r=>setTimeout(r,ms));
async function main(){
 const repo=path.resolve(__dirname,'../..'),out=path.join(repo,'build/ui094-study'),project=path.join(out,'project');
 assert(!sync.existsSync(path.join(out,'checkpoint.json')),'Keep previous evidence; use fresh output');await fs.mkdir(project,{recursive:true});
 const protocol=JSON.parse(await fs.readFile(path.join(__dirname,'fixtures/procedures094-protocol.json'),'utf8'));
 const record={protocol,source:cp.execFileSync('git',['rev-parse','HEAD'],{cwd:repo,encoding:'utf8'}).trim(),buildReceipt:require('../../scripts/Study-Build094.cjs').verify(repo),project,startedAt:new Date().toISOString(),rows:[],boots:[],captures:[],checks:[]};
 Object.assign(process.env,{FRAYNIX_WORKSPACE_PERSISTENT:'0',FRAYNIX_WORKSPACE_PLAN_CPU_ONLY:'1',FRAYNIX_WORKSPACE_JAVA_HEAP_MIB:'512',FRAYNIX_WORKSPACE_JAVA_PROCESSORS:'2'});
 let app,page,currentBoot;const errors=[],save=()=>fs.writeFile(path.join(out,'checkpoint.json'),JSON.stringify(record,null,2));
 const api=(route,args={})=>app.evaluate((_,x)=>global.workspaceTest.api(x.route,x.args),{route,args});
 async function open(){const at=performance.now();({app,page}=await launch(project));page.on('pageerror',e=>errors.push(e.message));await page.waitForFunction(()=>window.agency043View);const r=await api('/api/resources082/state');assert.equal(r.profile.cpus,2);assert.equal(r.configuredMaxHeapBytes,512*1024*1024);const pinned=JSON.parse(cp.execFileSync('pwsh',['-NoProfile','-File',path.join(repo,'scripts/Measure-Resources082.ps1'),'-Action','pin','-TargetPid',String(r.pid),'-Processors','2','-Checkout',repo],{encoding:'utf8',windowsHide:true}));assert.equal(BigInt(pinned.affinity).toString(2).split('1').length-1,2);assert.equal(path.resolve(await app.evaluate(()=>global.workspaceTest.project)),path.resolve(project));currentBoot={callerMillis:performance.now()-at,resources:r,pinned,parentPid:app.process().pid};record.boots.push(currentBoot);await save();}
 async function close(){if(app){await app.close();app=null;}}
 async function wait(fn,ms=300000){const end=Date.now()+ms;while(Date.now()<end){const x=await fn();if(x)return x;await sleep(60);}throw Error('Bounded094 wait exceeded');}
 async function capture(row,label){
  await app.evaluate((_,id)=>global.workspaceTest.goals.execute('goal.read',{id}),row.goalId);await sleep(1000);
  let visible=false;for(let i=0;i<5&&!visible;i++){await app.evaluate((_,id)=>global.workspaceTest.execute('canvas.open',{id}),row.goal.report.id);await page.waitForFunction(id=>document.querySelector('#object-detail').textContent.includes(id),row.goal.report.id);await page.locator('#preview-tab').click();await sleep(1200);visible=await page.evaluate(()=>document.querySelector('#preview-tab').classList.contains('selected'));}assert(visible);
  const text=await app.evaluate(()=>global.workspaceTest.canvasRead('document.querySelector("#recovery093")?.innerText || "Baseline: original forecasts"'));
  assert(text.includes('Recovery forecasts: '+row.job.arm.attempts.filter(a=>a.stage.startsWith('reforecast/')).length)||!row.prefixes.length);
  for(const width of [1600,1100]){await app.evaluate(({BrowserWindow},w)=>BrowserWindow.getAllWindows().find(x=>x.isVisible()).setSize(w,1100),width);await sleep(500);assert(await page.evaluate(()=>document.documentElement.scrollWidth<=innerWidth));const file=label+'-'+width+'.png';await fs.writeFile(path.join(out,file),Buffer.from(await app.evaluate(()=>global.workspaceTest.capture()),'base64'));record.captures.push({file,width,goalId:row.goalId,report:row.goal.report.id,text});}await save();
 }
 const sha=b=>crypto.createHash('sha256').update(b).digest('hex');
 async function stageRunning(row,stage){return wait(async()=>{
  const goalFile=path.join(project,'world-procedures-079',row.goalId,'goal.json');if(!sync.existsSync(goalFile))return null;
  const e=JSON.parse(await fs.readFile(goalFile,'utf8')),a=e.data.arm.attempts.find(a=>a.stage===stage&&a.status==='running');
  if(!a){const goal=await api('/api/goals/read',{id:row.goalId});if(['completed','interrupted','cancelled','failed'].includes(goal.status))throw Error('Goal ended before '+stage+': '+JSON.stringify(goal));return null;}
  const file=path.join(project,'world-procedures-079',row.goalId,'attempts',a.id,'world-native/checkpoints',a.id+'.json');if(!sync.existsSync(file))return null;
  const checkpoint=JSON.parse(await fs.readFile(file,'utf8'));return checkpoint.payload.physics.tick>=protocol.interruptTick?{attempt:a,checkpoint}:null;
 });}
 async function restart(row,interruption){
  const {stage,method}=interruption,at=performance.now(),before=await stageRunning(row,stage),attemptId=before.attempt.id;
  const interruptionRecord={stage,method,before,startedAt:new Date().toISOString(),boot:record.boots.length-1};row.interruptions.push(interruptionRecord);await save();
  if(method==='kill'){
   assert.equal(path.resolve(await app.evaluate(()=>global.workspaceTest.project)),path.resolve(project));
   interruptionRecord.process=JSON.parse(cp.execFileSync('pwsh',['-NoProfile','-File',path.join(repo,'scripts/Interrupt-Study094.ps1'),'-TargetPid',String(currentBoot.resources.pid),'-ExpectedParentPid',String(app.process().pid),'-ExpectedStartUtc',currentBoot.pinned.startUtc,'-Checkout',repo,'-Project',project],{encoding:'utf8',windowsHide:true}));assert(interruptionRecord.process.exited);
  }else await close();
  // Snapshot persisted bytes after exit and BEFORE any Java owner reopens the project.
  const attemptRoot=path.join(project,'world-procedures-079',row.goalId,'attempts',attemptId);
  interruptionRecord.persisted=[];
  for(const [kind,file] of [['checkpoint',path.join(attemptRoot,'world-native/checkpoints',attemptId+'.json')],['nativeState',path.join(attemptRoot,'world-native/state.json')],['job',path.join(project,'world-procedures-079',row.goalId,'goal.json')]]){
   const bytes=await fs.readFile(file),saved=row.goalId+'-'+row.interruptions.length+'-persisted-'+kind+'.json';await fs.writeFile(path.join(out,saved),bytes);interruptionRecord.persisted.push({kind,file:saved,sha256:sha(bytes),bytes:bytes.length});
  }
  const committed=JSON.parse(await fs.readFile(path.join(out,interruptionRecord.persisted[0].file),'utf8'));assert.equal(committed.payload.run.id,attemptId);interruptionRecord.committedTick=committed.payload.physics.tick;assert(interruptionRecord.committedTick>=protocol.interruptTick);
  if(row.kind==='crash-first'){
   const target=path.join(out,'stopped-crash-first');await fs.cp(project,target,{recursive:true,filter:p=>!['desktop-profile','host.json','test-desktop-process.log'].includes(path.basename(p))});record.stoppedSnapshot={path:target,goalId:row.goalId,attemptId,checkpointRelative:path.relative(project,path.join(attemptRoot,'world-native/checkpoints',attemptId+'.json')).replaceAll('\\','/'),binding:row.binding};
  }
  await save();await close();await open();const g=await api('/api/goals/read',{id:row.goalId});assert.equal(g.status,'interrupted');
  const prefix=await api('/api/procedures079/export',{id:row.goalId});assert(prefix.job.arm.attempts.some(a=>a.id===attemptId&&a.stage===stage&&a.status==='interrupted'));
  const file=row.goalId+'-prefix-'+row.prefixes.length+'.json';await fs.writeFile(path.join(out,file),JSON.stringify(prefix));row.prefixes.push({file,stage,goal:g});
  const ob=await api('/api/world/observe',{worldId:row.binding.worldId});assert.equal(ob.expectedRevision,0);assert.equal(ob.pending.goalId,row.goalId);
  const stable=JSON.stringify(prefix.job.arm.attempts);await sleep(300);assert.equal(JSON.stringify((await api('/api/procedures079/export',{id:row.goalId})).job.arm.attempts),stable,'No automatic continuation before explicit resume');
  await api('/api/goals/resume',{id:row.goalId});row.restartMillis+=performance.now()-at;await save();
 }
 async function run(kind,spec,retain=false,interruptions=[]){
  await close();await open();const scene=await api('/api/spatial/import',{text:JSON.stringify(spec.source)}),world=await api('/api/world/create',{sceneId:scene.id,title:'LABELLED094 '+kind}),ob=await api('/api/world/observe',{worldId:world.id}),binding=Object.fromEntries(['worldId','expectedRevision','expectedHash'].map(k=>[k,ob[k]]));
  const row={kind,binding,prefixes:[],interruptions:[],restartMillis:0};record.rows.push(row);await save();const at=performance.now();
  const g=await api('/api/world/start',{...binding,targets:spec.targets,forceLimitN:spec.forceLimitN,torqueLimitNm:spec.torqueLimitNm,selectiveMemory079:true,memoryMode079:retain?'simple':'memory',retainExperience079:retain,...(!retain?{procedureReceipt085:record.rows[0].goalId}:{}),request:'Edition094 three-stage abrupt recovery '+kind});row.goalId=g.id;await save();
  for(const interruption of interruptions)await restart(row,interruption);
  row.goal=await wait(async()=>{const x=await api('/api/goals/read',{id:g.id});return ['completed','interrupted','cancelled'].includes(x.status)?x:null;});row.callerMillis=performance.now()-at;await save();
  const original=await api('/api/procedures079/export',{id:g.id});row.originalFile=g.id+'-originals.json';await fs.writeFile(path.join(out,row.originalFile),JSON.stringify(original));row.job=original.job;
  row.observed=await api('/api/world/observe',{worldId:world.id});await save();
  assert.equal(row.goal.status,'completed',JSON.stringify(row.goal));assert(row.goal.poseView.passed);assert.equal(row.goal.worldCommit.revision,1);assert.equal(row.observed.expectedHash,row.goal.worldCommit.hash);
  assert.equal(row.job.arm.programs[row.job.arm.selected].forecast.length,3,'The ordinary selector must genuinely earn three stages');
  assert.equal(row.goal.taskCost080.eligible,row.prefixes.length===0);assert.equal(row.goal.resumes,row.prefixes.length);
  const replacements=row.job.arm.attempts.filter(a=>a.stage.startsWith('reforecast/'));assert.equal(replacements.length,interruptions.length?(interruptions[0].stage==='actual/0'?2:1):0);
  if(row.prefixes.length){assert(!row.goal.taskCost080.clock089);assert.equal(row.goal.taskCost080.totalMillis,0);assert(replacements.every(a=>a.metrics.passed));}
  row.fullVerify=await api('/api/procedures090/verify',{id:g.id});assert(row.fullVerify.passed);
  const runs=new Map(original.originals.map(p=>{const x=unpack(p);return[x.id,x];}));
  for(const p of row.prefixes){const prefix=JSON.parse(await fs.readFile(path.join(out,p.file),'utf8'));for(const a of prefix.job.arm.attempts)assert(row.job.arm.attempts.some(b=>b.id===a.id&&b.stage===a.stage));for(const z of prefix.originals){const prior=unpack(z),current=runs.get(prior.id);assert.deepEqual(current.samples.slice(0,prior.samples.length),prior.samples);if(prior.status==='completed')assert.deepEqual(current,prior);}}
  for(const interruption of row.interruptions){const durable=JSON.parse(await fs.readFile(path.join(out,interruption.persisted.find(x=>x.kind==='checkpoint').file),'utf8'));const run=durable.payload.run,current=runs.get(run.id);assert.deepEqual(current.samples.slice(0,run.samples.length),run.samples);assert(current.events.some(e=>e.kind==='resume'&&e.checkpointSha256===durable.sha256));interruption.durablePrefixPreserved=true;}
  row.prefixPreserved=true;assert.equal((await api('/api/procedures079/memories')).memories.length,1);
  console.log('RESULT',kind,row.goal.status,row.callerMillis.toFixed(1),row.job.arm.chargedTicks);await save();return row;
 }
 try{
  await run('seed',protocol.seed,true);for(const c of protocol.cases){const row=await run(c.kind,c.variant?protocol.variant:protocol.task,false,c.interruptions);if(['baseline','crash-first','crash-reforecast'].includes(c.kind))await capture(row,c.kind);}
  assert.deepEqual(errors,[]);record.errors=errors;record.finishedAt=new Date().toISOString();record.passed=true;require('../../scripts/Study-Build094.cjs').verify(repo);await fs.writeFile(path.join(out,'result.json'),JSON.stringify(record,null,2));console.log('PASS094 three-stage recovery and abrupt isolated process termination');
 }catch(e){record.error=e.stack;throw e;}finally{await close();await save();}
}
main().catch(e=>{console.error(e);process.exitCode=1;});
