'use strict';
const {launch}=require('./calibration-integration.cjs'),fs=require('node:fs/promises'),sync=require('node:fs'),path=require('node:path'),assert=require('node:assert/strict'),crypto=require('node:crypto'),cp=require('node:child_process');
const protocol=require('./fixtures/procedures082-protocol.json'),repo=path.resolve(__dirname,'../..'),sleep=ms=>new Promise(r=>setTimeout(r,ms)),sha=b=>crypto.createHash('sha256').update(b).digest('hex');
async function main(){
 const buildReceipt=require('../../scripts/Study-Build082.cjs').verify(repo),budget=protocol.budgets.find(b=>b.id===process.argv[2]);assert(budget,'Name a frozen budget');
 process.env.FRAYNIX_WORKSPACE_PERSISTENT='0';process.env.FRAYNIX_WORKSPACE_PLAN_CPU_ONLY='1';process.env.FRAYNIX_WORKSPACE_JAVA_HEAP_MIB=String(budget.heapMiB);process.env.FRAYNIX_WORKSPACE_JAVA_PROCESSORS=String(budget.processors);
 const out=path.resolve(process.env.FRAYNIX082_OUTPUT||path.join(repo,'build/ui082-'+budget.id));await fs.mkdir(out,{recursive:true});assert(!sync.existsSync(path.join(out,'checkpoint.json')),'Use a fresh output; do not overwrite a run');
 const project=path.join(out,'preparation'),record={source:cp.execFileSync('git',['rev-parse','HEAD'],{cwd:repo,encoding:'utf8'}).trim(),budget,project,out,buildReceipt,startedAt:new Date().toISOString(),training:[],calibration:[],arms:[],boots:[],checks:[]};
 let app,page,currentProject,monitor,monitorExit,stopFile,boot,errors=[];
 const api=(route,args={})=>app.evaluate((_,x)=>global.workspaceTest.api(x.route,x.args),{route,args});const save=()=>fs.writeFile(path.join(out,'checkpoint.json'),JSON.stringify(record,null,2));
 const check=async text=>{record.checks.push(text);console.log('PASS',text);await save();};
 async function wait(fn,ms=1200000){const end=Date.now()+ms;while(Date.now()<end){const v=await fn();if(v)return v;await sleep(300);}throw Error('Bounded082 wait exceeded');}
 function osRead(action,pid,extra=[]){return JSON.parse(cp.execFileSync('pwsh',['-NoProfile','-File',path.join(repo,'scripts/Measure-Resources082.ps1'),'-Action',action,'-TargetPid',String(pid),'-Processors',String(budget.processors),'-Checkout',repo,...extra],{encoding:'utf8',windowsHide:true}));}
 async function open(where,label){
  const began=performance.now();currentProject=where;await fs.mkdir(where,{recursive:true});({app,page}=await launch(where));page.on('pageerror',e=>errors.push(e.message));await page.waitForFunction(()=>window.agency043View);
  await app.evaluate(({BrowserWindow})=>BrowserWindow.getAllWindows().find(w=>w.isVisible()).setSize(1600,1100));
  const resource=await api('/api/resources082/state');assert.equal(resource.profile.cpus,budget.processors);assert.equal(resource.configuredMaxHeapBytes,budget.heapMiB*1024*1024);
  const pinned=osRead('pin',resource.pid);assert.equal(BigInt(pinned.affinity).toString(2).split('1').length-1,budget.processors);
  const state=await api('/api/agent042/state');boot={label,project:where,pid:resource.pid,bootId:state.bootId,startupMillis:performance.now()-began,resources:resource,pinned};record.boots.push(boot);
  stopFile=path.join(out,label+'-stop');const file=path.join(out,label+'-os.jsonl');monitor=cp.spawn('pwsh',['-NoProfile','-File',path.join(repo,'scripts/Measure-Resources082.ps1'),'-Action','monitor','-TargetPid',String(resource.pid),'-Checkout',repo,'-OutputFile',file,'-StopFile',stopFile],{windowsHide:true,stdio:['ignore','pipe','pipe']});let monitorText='';monitor.stderr.on('data',b=>monitorText+=b);monitor.stdout.on('data',b=>monitorText+=b);monitorExit=new Promise((resolve,reject)=>{monitor.once('error',reject);monitor.once('exit',code=>code===0?resolve():reject(Error('Resource monitor: '+monitorText)));});boot.osFile=path.basename(file);await wait(async()=>sync.existsSync(file)&&(await fs.stat(file)).size>0,15000);await save();
 }
 async function close(){if(!app)return;boot.finalResources=await api('/api/resources082/state');boot.finalOS=osRead('read',boot.pid);await fs.writeFile(stopFile,'done');await monitorExit;await app.close();app=null;await save();}
 async function capture(name){await sleep(300);await fs.writeFile(path.join(out,name+'.png'),Buffer.from(await app.evaluate(()=>global.workspaceTest.capture()),'base64'));}
 async function report(id){await app.evaluate((_,id)=>global.workspaceTest.goals.execute('goal.read',{id}),id);await page.waitForFunction(id=>document.querySelector('#java-goal').dataset.goalId===id&&document.querySelector('#java-goal-status').textContent==='completed',id);await page.locator('#java-goal-report').click();await sleep(500);}
 async function create(spec){const s=await api('/api/spatial/import',{text:JSON.stringify(spec.source)});return api('/api/world/create',{sceneId:s.id,title:'LABELLED082 '+spec.id});}
 async function binding(world){const o=await api('/api/world/observe',{worldId:world.id});return Object.fromEntries(['worldId','expectedRevision','expectedHash'].map(k=>[k,o[k]]));}
 async function run(spec,world,mode,policy,retain,skillName=''){
  await api('/api/procedures081/test-mode',{mode});const b=await binding(world),before=await api('/api/resources082/state'),beforeOS=osRead('read',before.pid),at=performance.now(),samples=[];
  const goal=await api('/api/world/start',{...b,targets:spec.targets,forceLimitN:spec.forceLimitN,torqueLimitNm:spec.torqueLimitNm,selectiveMemory079:true,memoryMode079:policy,retainExperience079:retain,...(skillName?{saveSkillName:skillName}:{}),request:'082 continuing-world '+spec.id});
  let sampleAt=performance.now();const settled=await wait(async()=>{const g=await api('/api/goals/read',{id:goal.id});if(performance.now()-sampleAt>=2000){samples.push(await api('/api/resources082/state'));sampleAt=performance.now();}return ['completed','cancelled','interrupted'].includes(g.status)?g:null;});
  const callerMillis=performance.now()-at,after=await api('/api/resources082/state'),afterOS=osRead('read',before.pid),scan=await api('/api/procedures081/diagnostics'),observed=await api('/api/world/observe',{worldId:world.id});
  const exportAt=performance.now(),original=await api('/api/procedures079/export',{id:goal.id}),file=spec.id+'-'+mode+'-'+goal.id+'-originals.json';await fs.writeFile(path.join(out,file),JSON.stringify(original));
  const row={id:spec.id,goalId:goal.id,mode,policy,retain,skillName,project:currentProject,status:settled.status,reason:settled.reason,callerMillis,exportMillis:performance.now()-exportAt,scan,before,after,beforeOS,afterOS,samples,binding:b,observed,goal:settled,job:original.job,originalFile:file};
  console.log('RESULT',budget.id,spec.id,mode,settled.status,callerMillis.toFixed(1));
  assert.equal(beforeOS.affinity,afterOS.affinity);assert.equal(beforeOS.affinity,boot.pinned.affinity);assert.equal(before.pid,after.pid);assert(after.processCpuNanos>=before.processCpuNanos);
  if(settled.status==='completed'){
   assert(settled.poseView.passed);assert(settled.taskCost080.eligible);assert(settled.taskCost080.totalMillis>0&&settled.taskCost080.totalMillis<=callerMillis);assert.deepEqual(settled.taskCost080.profile,before.profile);assert.equal(settled.taskCost080.workload.retain,retain);assert.equal(settled.taskCost080.workload.skill,!!skillName);assert.equal(settled.worldCommit.revision,b.expectedRevision+1);assert.equal(observed.expectedRevision,b.expectedRevision+1);assert.equal(settled.worldCommit.hash,observed.expectedHash);assert.deepEqual(settled.worldBinding,b);
  }else{assert.equal(observed.expectedRevision,b.expectedRevision);(record.failures||=[]).push(row);await save();}
  return row;
 }
 async function inventory(dir){const rows=[];async function walk(base){for(const e of await fs.readdir(base,{withFileTypes:true})){if(e.name==='desktop-profile'||e.name==='engine.log'||e.name==='test-desktop-process.log')continue;const p=path.join(base,e.name);assert(!e.isSymbolicLink());if(e.isDirectory())await walk(p);else {const b=await fs.readFile(p);rows.push({path:path.relative(dir,p).replaceAll('\\','/'),bytes:b.length,sha256:sha(b)});}}}await walk(dir);return rows.sort((a,b)=>a.path.localeCompare(b.path));}
 async function viewWorld(world){await page.locator('#spatial-tab').click();await page.locator('#persistent-world-panel').evaluate(e=>e.open=true);await wait(()=>page.locator('#persistent-worlds option').evaluateAll((xs,id)=>xs.some(x=>x.value===id),world.id));await page.locator('#persistent-worlds').selectOption(world.id);await page.locator('#procedure079-panel').scrollIntoViewIfNeeded();}
 try{
  await save();await open(project,'prepare');await assert.rejects(api('/api/resources082/state',{passed:true}));
  for(const spec of protocol.training){const world=await create(spec),row=await run(spec,world,'parallel','simple',true);record.training.push(row);await save();assert.equal(row.status,'completed','Learning failed; retain original attempt');}
  const bank=path.join(project,'world-procedures-079/bank.json');record.frozenBankSha=sha(await fs.readFile(bank));const pairs=[];
  for(let i=0;i<protocol.calibration.length;i++){const spec=protocol.calibration[i],world=await create(spec),row={id:spec.id,results:{}};for(const policy of i%2?['memory','simple']:['simple','memory']){if((await binding(world)).expectedRevision)await api('/api/world/restore',{...await binding(world),targetRevision:0});row.results[policy]=await run(spec,world,'parallel',policy,false);await save();assert.equal(row.results[policy].status,'completed');}record.calibration.push(row);pairs.push({simpleId:row.results.simple.goalId,memoryId:row.results.memory.goalId});await save();}
  const fitAt=performance.now();record.model=await api('/api/procedures080/fit',{pairs});record.fitMillis=performance.now()-fitAt;assert.equal(record.model.pairs.length,2);assert.deepEqual(record.model.profile,boot.resources.profile);record.modelSha=sha(await fs.readFile(path.join(project,'world-procedures-079/cost-models080',record.model.id+'.json')));
  record.micro=[];for(let i=-1;i<protocol.microRepeats;i++)for(const mode of i%2?['parallel','reference']:['reference','parallel']){await api('/api/procedures081/test-mode',{mode});const before=await api('/api/resources082/state'),at=performance.now();assert.deepEqual(await api('/api/procedures080/calibration'),record.model);record.micro.push({iteration:i,mode,callerMillis:performance.now()-at,before,after:await api('/api/resources082/state'),scan:await api('/api/procedures081/diagnostics')});await save();}
  const world=await create({id:'continuing-world',source:protocol.source});record.world=world;record.initial=await api('/api/world/observe',{worldId:world.id});await close();record.preparedFiles=await inventory(project);await check('Fresh measured calibration belongs to this exact runtime; prepared world/evidence saved before cloning');
  for(const mode of budget.order){
   const armProject=path.join(out,'sequence-'+mode);assert(!sync.existsSync(armProject));await fs.cp(project,armProject,{recursive:true,filter:p=>!['desktop-profile','engine.log','test-desktop-process.log'].includes(path.basename(p))});assert.deepEqual(await inventory(armProject),record.preparedFiles);
   const arm={mode,project:armProject,rows:[],restarts:[],initialCloneVerified:true,startedAt:new Date().toISOString()},armAt=performance.now();record.arms.push(arm);await save();await open(armProject,mode+'-start');await api('/api/procedures081/test-mode',{mode});let last=record.initial;
   assert.deepEqual(await api('/api/world/observe',{worldId:world.id}),last);
   for(let i=0;i<protocol.sequence.length;i++){
    if(i===protocol.restartAfter){const start=performance.now(),prior=boot.bootId;await close();await open(armProject,mode+'-restart');assert.notEqual(boot.bootId,prior);const current=await api('/api/world/observe',{worldId:world.id});assert.equal(current.expectedHash,last.expectedHash);assert.equal(current.expectedRevision,last.expectedRevision);arm.restarts.push({after:i,millis:performance.now()-start,priorBoot:prior,newBoot:boot.bootId,revision:current.expectedRevision,hash:current.expectedHash});}
    const spec=protocol.sequence[i],row=await run(spec,world,mode,'calibrated',spec.retain,spec.skillName);arm.rows.push(row);await save();assert.equal(row.binding.expectedHash,last.expectedHash);assert.equal(row.binding.expectedRevision,last.expectedRevision);
    if(row.status!=='completed'){arm.failedAt=i+1;await api('/api/goals/cancel',{id:row.goalId});break;}
    if(i===0||i===protocol.restartAfter)assert.equal(row.job.decision.candidates.some(c=>c.warm),false);
    last=row.observed;
    assert.equal(sha(await fs.readFile(path.join(armProject,'world-procedures-079/cost-models080',record.model.id+'.json'))),record.modelSha);
    if(!spec.retain)assert.equal(sha(await fs.readFile(path.join(armProject,'world-procedures-079/bank.json'))),record.frozenBankSha);
    if(i===0||i===5){await report(row.goalId);await capture(mode+'-goal-'+(i+1)+'-report');}
   }
   arm.executionEnvelopeMillis=performance.now()-armAt;arm.final=last;await viewWorld(world);await capture(mode+'-continuing-world');
   const reopenAt=performance.now();await close();await open(armProject,mode+'-verify');const final=await api('/api/world/observe',{worldId:world.id});assert.equal(final.expectedHash,last.expectedHash);assert.equal(final.expectedRevision,last.expectedRevision);arm.persistenceMillis=performance.now()-reopenAt;
   if(!arm.failedAt){const g=await api('/api/goals/read',{id:arm.rows.at(-1).goalId});assert.equal(g.savedSkill.name,'resource-route-082');assert(g.savedSkill.version>=1);arm.savedSkill=g.savedSkill;await report(g.id);await capture(mode+'-saved-skill');}
   if(mode==='parallel'){await viewWorld(world);for(const width of [1600,1100]){await app.evaluate(({BrowserWindow},w)=>BrowserWindow.getAllWindows().find(x=>x.isVisible()).setSize(w,1000),width);assert(await page.evaluate(()=>document.documentElement.scrollWidth<=innerWidth));await capture(mode+'-layout-'+width);}}
   await close();arm.finishedAt=new Date().toISOString();await save();
  }
  require('../../scripts/Study-Build082.cjs').verify(repo);assert.deepEqual(errors,[]);record.errors=errors;record.passed=true;record.finishedAt=new Date().toISOString();await fs.writeFile(path.join(out,'result.json'),JSON.stringify(record,null,2));console.log('COMPLETE',budget.id,out);
 }catch(e){record.error=e.stack;await save();if(app)await capture('failure').catch(()=>{});throw e;}finally{if(app)await close().catch(e=>console.error('Closing:',e));}
}
main().catch(e=>{console.error(e);process.exitCode=1;});
