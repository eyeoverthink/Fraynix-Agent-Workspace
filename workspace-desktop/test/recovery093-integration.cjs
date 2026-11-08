'use strict';
const {launch}=require('./calibration-integration.cjs'),fs=require('node:fs/promises'),sync=require('node:fs'),path=require('node:path'),assert=require('node:assert/strict'),cp=require('node:child_process');
const {unpack}=require('../../scripts/Inspect-Counterfactual045.cjs');
const sleep=ms=>new Promise(r=>setTimeout(r,ms));
async function main(){
 const repo=path.resolve(__dirname,'../..'),out=path.join(repo,'build/ui093-study'),project=path.join(out,'project');
 assert(!sync.existsSync(path.join(out,'checkpoint.json')),'Keep previous evidence; use fresh output');await fs.mkdir(project,{recursive:true});
 const protocol=JSON.parse(await fs.readFile(path.join(__dirname,'fixtures/procedures093-protocol.json'),'utf8'));
 const record={protocol,source:cp.execFileSync('git',['rev-parse','HEAD'],{cwd:repo,encoding:'utf8'}).trim(),buildReceipt:require('../../scripts/Study-Build093.cjs').verify(repo),project,startedAt:new Date().toISOString(),rows:[],boots:[],captures:[],checks:[]};
 Object.assign(process.env,{FRAYNIX_WORKSPACE_PERSISTENT:'0',FRAYNIX_WORKSPACE_PLAN_CPU_ONLY:'1',FRAYNIX_WORKSPACE_JAVA_HEAP_MIB:'512',FRAYNIX_WORKSPACE_JAVA_PROCESSORS:'2'});
 let app,page;const errors=[],save=()=>fs.writeFile(path.join(out,'checkpoint.json'),JSON.stringify(record,null,2));
 const api=(route,args={})=>app.evaluate((_,x)=>global.workspaceTest.api(x.route,x.args),{route,args});
 async function open(){const at=performance.now();({app,page}=await launch(project));page.on('pageerror',e=>errors.push(e.message));await page.waitForFunction(()=>window.agency043View);const r=await api('/api/resources082/state');assert.equal(r.profile.cpus,2);assert.equal(r.configuredMaxHeapBytes,512*1024*1024);const pinned=JSON.parse(cp.execFileSync('pwsh',['-NoProfile','-File',path.join(repo,'scripts/Measure-Resources082.ps1'),'-Action','pin','-TargetPid',String(r.pid),'-Processors','2','-Checkout',repo],{encoding:'utf8',windowsHide:true}));assert.equal(BigInt(pinned.affinity).toString(2).split('1').length-1,2);record.boots.push({callerMillis:performance.now()-at,resources:r,pinned});await save();}
 async function close(){if(app){await app.close();app=null;}}
 async function wait(fn,ms=300000){const end=Date.now()+ms;while(Date.now()<end){const x=await fn();if(x)return x;await sleep(60);}throw Error('Bounded093 wait exceeded');}
 async function capture(row,label){
  await app.evaluate((_,id)=>global.workspaceTest.goals.execute('goal.read',{id}),row.goalId);await sleep(1000);
  let visible=false;for(let i=0;i<5&&!visible;i++){await app.evaluate((_,id)=>global.workspaceTest.execute('canvas.open',{id}),row.goal.report.id);await page.waitForFunction(id=>document.querySelector('#object-detail').textContent.includes(id),row.goal.report.id);await page.locator('#preview-tab').click();await sleep(1200);visible=await page.evaluate(()=>document.querySelector('#preview-tab').classList.contains('selected'));}assert(visible);
  const text=await app.evaluate(()=>global.workspaceTest.canvasRead('document.querySelector("#recovery093")?.innerText || "Baseline: original forecasts"'));
  if(row.prefixes.length)assert(text.includes('Recovery forecasts: 1'));
  for(const width of [1600,1100]){await app.evaluate(({BrowserWindow},w)=>BrowserWindow.getAllWindows().find(x=>x.isVisible()).setSize(w,1100),width);await sleep(500);assert(await page.evaluate(()=>document.documentElement.scrollWidth<=innerWidth));const file=label+'-'+width+'.png';await fs.writeFile(path.join(out,file),Buffer.from(await app.evaluate(()=>global.workspaceTest.capture()),'base64'));record.captures.push({file,width,goalId:row.goalId,report:row.goal.report.id,text});}await save();
 }
 async function restart(row,stage){
  const at=performance.now();await close();await open();const g=await api('/api/goals/read',{id:row.goalId});assert.equal(g.status,'interrupted');
  const prefix=await api('/api/procedures079/export',{id:row.goalId});assert(prefix.job.arm.attempts.some(a=>a.stage===stage&&a.status==='interrupted'));
  const file=row.goalId+'-prefix-'+row.prefixes.length+'.json';await fs.writeFile(path.join(out,file),JSON.stringify(prefix));row.prefixes.push({file,stage,goal:g});
  const ob=await api('/api/world/observe',{worldId:row.binding.worldId});assert.equal(ob.expectedRevision,0);assert.equal(ob.pending.goalId,row.goalId);
  await api('/api/goals/resume',{id:row.goalId});row.restartMillis+=performance.now()-at;await save();
 }
 async function reforecastRunning(row){return wait(async()=>{
  const e=JSON.parse(await fs.readFile(path.join(project,'world-procedures-079',row.goalId,'goal.json'),'utf8')),a=e.data.arm.attempts.find(a=>a.stage==='reforecast/1'&&a.status==='running');if(!a)return null;
  const file=path.join(project,'world-procedures-079',row.goalId,'attempts',a.id,'world-native/checkpoints',a.id+'.json');if(!sync.existsSync(file))return null;const checkpoint=JSON.parse(await fs.readFile(file,'utf8'));return checkpoint.payload.physics.tick>=protocol.interruptTick?{attempt:a,checkpoint}:null;
 });}
 async function run(kind,spec,retain=false){
  await close();await open();const scene=await api('/api/spatial/import',{text:JSON.stringify(spec.source)}),world=await api('/api/world/create',{sceneId:scene.id,title:'LABELLED093 '+kind}),ob=await api('/api/world/observe',{worldId:world.id}),binding=Object.fromEntries(['worldId','expectedRevision','expectedHash'].map(k=>[k,ob[k]]));
  const row={kind,binding,prefixes:[],restartMillis:0};record.rows.push(row);await save();const at=performance.now();
  const g=await api('/api/world/start',{...binding,targets:spec.targets,forceLimitN:spec.forceLimitN,torqueLimitNm:spec.torqueLimitNm,selectiveMemory079:true,memoryMode079:retain?'simple':'memory',retainExperience079:retain,...(!retain?{procedureReceipt085:record.rows[0].goalId}:{}),request:'Edition093 bounded recovered forecast '+kind});row.goalId=g.id;await save();
  if(!retain&&kind!=='baseline'){
   row.beforeInterruption=await wait(async()=>{const o=await api('/api/world/observe',{worldId:world.id});return o.pending&&o.nativeObservation?.elapsedTicksBeforeStage===0&&o.nativeObservation?.sample?.tick>=protocol.interruptTick&&o.nativeObservation.status==='running'?o:null;});
   await restart(row,'actual/0');
   if(kind.startsWith('reforecast-')){
    row.reforecastCheckpoint=await reforecastRunning(row);await save();
    if(kind==='reforecast-restart')await restart(row,'reforecast/1');
    else {await api('/api/goals/cancel',{id:g.id});await sleep(1500);}
   }
  }
  row.goal=await wait(async()=>{const x=await api('/api/goals/read',{id:g.id});return ['completed','interrupted','cancelled'].includes(x.status)?x:null;});row.callerMillis=performance.now()-at;await save();
  const original=await api('/api/procedures079/export',{id:g.id});row.originalFile=g.id+'-originals.json';await fs.writeFile(path.join(out,row.originalFile),JSON.stringify(original));row.job=original.job;
  row.observed=await api('/api/world/observe',{worldId:world.id});await save();
  if(kind==='reforecast-cancel'){
   assert.equal(row.goal.status,'cancelled');assert.equal(row.observed.expectedRevision,0);assert.equal(row.observed.pending.goalId,g.id);assert(!row.job.arm.attempts.some(a=>a.stage==='actual/1'));assert(!row.goal.worldCommit);assert(!row.goal.taskCost080);record.checks.push('Stop during reforecast: no next actual stage, no world commit, no eligible cost');
  }else{
   assert.equal(row.goal.status,'completed',JSON.stringify(row.goal));assert(row.goal.poseView.passed);assert.equal(row.goal.worldCommit.revision,1);assert.equal(row.observed.expectedHash,row.goal.worldCommit.hash);
   assert.equal(row.goal.taskCost080.eligible,row.prefixes.length===0);assert.equal(row.goal.resumes,row.prefixes.length);
   const replacements=row.job.arm.attempts.filter(a=>a.stage.startsWith('reforecast/'));assert.equal(replacements.length,row.prefixes.length?1:0);
   if(row.prefixes.length){assert(!row.goal.taskCost080.clock089);assert.equal(row.goal.taskCost080.totalMillis,0);assert(replacements.every(a=>a.metrics.passed));}
   row.fullVerify=await api('/api/procedures090/verify',{id:g.id});assert(row.fullVerify.passed);
  }
  const runs=new Map(original.originals.map(p=>{const x=unpack(p);return[x.id,x];}));
  for(const p of row.prefixes){const prefix=JSON.parse(await fs.readFile(path.join(out,p.file),'utf8'));for(const a of prefix.job.arm.attempts)assert(row.job.arm.attempts.some(b=>b.id===a.id&&b.stage===a.stage));for(const z of prefix.originals){const prior=unpack(z),current=runs.get(prior.id);assert.deepEqual(current.samples.slice(0,prior.samples.length),prior.samples);if(prior.status==='completed')assert.deepEqual(current,prior);}}
  row.prefixPreserved=true;assert.equal((await api('/api/procedures079/memories')).memories.length,1);
  console.log('RESULT',kind,row.goal.status,row.callerMillis.toFixed(1),row.job.arm.chargedTicks);await save();return row;
 }
 try{
  await run('seed',protocol.seed,true);for(const kind of protocol.cases){const row=await run(kind,kind==='varied-early-restart'?protocol.variant:protocol.task);if(['baseline','early-restart','reforecast-restart'].includes(kind))await capture(row,kind);}
  assert.deepEqual(errors,[]);record.errors=errors;record.finishedAt=new Date().toISOString();record.passed=true;require('../../scripts/Study-Build093.cjs').verify(repo);await fs.writeFile(path.join(out,'result.json'),JSON.stringify(record,null,2));console.log('PASS093 fresh forecasts, actual recovery, second interruption and stop');
 }catch(e){record.error=e.stack;throw e;}finally{await close();await save();}
}
main().catch(e=>{console.error(e);process.exitCode=1;});
