'use strict';
const {launch}=require('./calibration-integration.cjs'),fs=require('node:fs/promises'),sync=require('node:fs'),path=require('node:path'),assert=require('node:assert/strict'),cp=require('node:child_process'),crypto=require('node:crypto');
const sha=b=>crypto.createHash('sha256').update(b).digest('hex'),sleep=ms=>new Promise(r=>setTimeout(r,ms));
async function main(){
 const repo=path.resolve(__dirname,'../..'),protocol=JSON.parse(await fs.readFile(path.join(__dirname,'fixtures/procedures084-protocol.json'),'utf8')),budget=protocol.budget,mode=process.argv[2],group=protocol.groups.find(g=>g.id===mode);
 assert(group,'Choose a frozen group');const buildReceipt=require('../../scripts/Study-Build084.cjs').verify(repo);
 process.env.FRAYNIX_WORKSPACE_PERSISTENT='0';process.env.FRAYNIX_WORKSPACE_PLAN_CPU_ONLY='1';process.env.FRAYNIX_WORKSPACE_JAVA_HEAP_MIB=String(budget.heapMiB);process.env.FRAYNIX_WORKSPACE_JAVA_PROCESSORS=String(budget.processors);
 const out=path.resolve(process.env.FRAYNIX084_OUTPUT||path.join(repo,'build/ui084-'+mode)),project=path.join(out,'preparation');await fs.mkdir(out,{recursive:true});assert(!sync.existsSync(path.join(out,'checkpoint.json')),'Use a fresh output');
 const record={source:cp.execFileSync('git',['rev-parse','HEAD'],{cwd:repo,encoding:'utf8'}).trim(),budget,group,mode,project,out,buildReceipt,startedAt:new Date().toISOString(),training:[],calibration:[],rows:[],warmups:[],boots:[],checks:[]};
 let app,page,currentProject,monitor,monitorExit,stopFile,boot,errors=[],bootIndex=0;
 const api=(route,args={})=>app.evaluate((_,x)=>global.workspaceTest.api(x.route,x.args),{route,args}),save=()=>fs.writeFile(path.join(out,'checkpoint.json'),JSON.stringify(record,null,2)),check=async text=>{record.checks.push(text);console.log('PASS',text);await save();};
 async function wait(fn,ms=1200000){const end=Date.now()+ms;while(Date.now()<end){const v=await fn();if(v)return v;await sleep(300);}throw Error('Bounded084 wait exceeded');}
 function osRead(action,pid,extra=[]){return JSON.parse(cp.execFileSync('pwsh',['-NoProfile','-File',path.join(repo,'scripts/Measure-Resources082.ps1'),'-Action',action,'-TargetPid',String(pid),'-Processors',String(budget.processors),'-Checkout',repo,...extra],{encoding:'utf8',windowsHide:true}));}
 async function open(where,label){
  label=String(++bootIndex)+'-'+label;
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
 async function create(spec){const s=await api('/api/spatial/import',{text:JSON.stringify(spec.source)});return api('/api/world/create',{sceneId:s.id,title:'LABELLED084 '+spec.id});}
 async function binding(world){const o=await api('/api/world/observe',{worldId:world.id});return Object.fromEntries(['worldId','expectedRevision','expectedHash'].map(k=>[k,o[k]]));}
 async function run(spec,world,mode,policy,retain,skillName=''){
  const bankBefore=await fs.readFile(path.join(currentProject,'world-procedures-079/bank.json')).catch(()=>null);
  await api('/api/procedures081/test-mode',{mode});const b=await binding(world),before=await api('/api/resources082/state'),beforeOS=osRead('read',before.pid),at=performance.now(),samples=[];
  const goal=await api('/api/world/start',{...b,targets:spec.targets,forceLimitN:spec.forceLimitN,torqueLimitNm:spec.torqueLimitNm,selectiveMemory079:true,memoryMode079:policy,retainExperience079:retain,...(skillName?{saveSkillName:skillName}:{}),request:'084 continuing-world '+spec.id});
  let sampleAt=performance.now();const settled=await wait(async()=>{const g=await api('/api/goals/read',{id:goal.id});if(performance.now()-sampleAt>=2000){samples.push(await api('/api/resources082/state'));sampleAt=performance.now();}return ['completed','cancelled','interrupted'].includes(g.status)?g:null;});
  const callerMillis=performance.now()-at,after=await api('/api/resources082/state'),afterOS=osRead('read',before.pid),scan=await api('/api/procedures081/diagnostics'),observed=await api('/api/world/observe',{worldId:world.id});
  const exportAt=performance.now(),original=await api('/api/procedures079/export',{id:goal.id}),file=spec.id+'-'+mode+'-'+goal.id+'-originals.json';await fs.writeFile(path.join(out,file),JSON.stringify(original));
  const row={ownerBootId:boot.bootId,bootLabel:boot.label,id:spec.id,goalId:goal.id,mode,policy,retain,skillName,project:currentProject,status:settled.status,reason:settled.reason,callerMillis,exportMillis:performance.now()-exportAt,scan,before,after,beforeOS,afterOS,samples,binding:b,observed,goal:settled,job:original.job,originalFile:file};
  const bankAfter=await fs.readFile(path.join(currentProject,'world-procedures-079/bank.json')).catch(()=>null);row.bankBefore={sha256:bankBefore?sha(bankBefore):null,count:bankBefore?JSON.parse(bankBefore).data.entries.length:0};row.bankAfter={sha256:bankAfter?sha(bankAfter):null,count:bankAfter?JSON.parse(bankAfter).data.entries.length:0};
  console.log('RESULT',budget.id,spec.id,mode,settled.status,callerMillis.toFixed(1));
  assert.equal(beforeOS.affinity,afterOS.affinity);assert.equal(beforeOS.affinity,boot.pinned.affinity);assert.equal(before.pid,after.pid);assert(after.processCpuNanos>=before.processCpuNanos);
  if(settled.status==='completed'){
   assert(settled.poseView.passed);assert(settled.taskCost080.eligible);assert(settled.taskCost080.totalMillis>0&&settled.taskCost080.totalMillis<=callerMillis);assert.deepEqual(settled.taskCost080.profile,before.profile);assert.equal(settled.taskCost080.workload.retain,retain);assert.equal(settled.taskCost080.workload.skill,!!skillName);assert.equal(settled.worldCommit.revision,b.expectedRevision+1);assert.equal(observed.expectedRevision,b.expectedRevision+1);assert.equal(settled.worldCommit.hash,observed.expectedHash);assert.deepEqual(settled.worldBinding,b);
  }else{assert.equal(observed.expectedRevision,b.expectedRevision);(record.failures||=[]).push(row);await save();}
  return row;
 }
 async function inventory(dir){const rows=[];async function walk(base){for(const e of await fs.readdir(base,{withFileTypes:true})){if(e.name==='desktop-profile'||e.name==='engine.log'||e.name==='test-desktop-process.log')continue;const p=path.join(base,e.name);assert(!e.isSymbolicLink());if(e.isDirectory())await walk(p);else {const b=await fs.readFile(p);rows.push({path:path.relative(dir,p).replaceAll('\\','/'),bytes:b.length,sha256:sha(b)});}}}await walk(dir);return rows.sort((a,b)=>a.path.localeCompare(b.path));}
 async function viewWorld(world){await page.locator('#spatial-tab').click();await page.locator('#persistent-world-panel').evaluate(e=>e.open=true);await wait(()=>page.locator('#persistent-worlds option').evaluateAll((xs,id)=>xs.some(x=>x.value===id),world.id));await page.locator('#persistent-worlds').selectOption(world.id);await page.locator('#procedure079-panel').scrollIntoViewIfNeeded();}
 async function clone(from,to,expected){assert(!sync.existsSync(to));await fs.cp(from,to,{recursive:true,filter:p=>!['desktop-profile','engine.log','test-desktop-process.log'].includes(path.basename(p))});assert.deepEqual(await inventory(to),expected);}
 async function warmup(){const spec={...group.training,id:'warm-up-'+bootIndex},world=await create(spec),r=await run(spec,world,'parallel','memory',false);record.warmups.push(r);await save();assert.equal(r.status,'completed');assert.equal(r.job.decision.candidate.id,record.receiptId);assert.equal(r.job.decision.candidate.warm,false);}
 async function start(where,label){await open(where,label);if(group?.warm)await warmup();}
 async function stopAndRemember(row,world,label){
  const before=await api('/api/world/observe',{worldId:world.id});await close();const at=performance.now();await open(row.project,label+'-reopen');const after=await api('/api/world/observe',{worldId:world.id});assert.equal(after.expectedHash,before.expectedHash);assert.equal(after.expectedRevision,before.expectedRevision);
  const g=await api('/api/goals/read',{id:row.goalId});if(group.skill){assert.equal(g.savedSkill.name,row.skillName);assert.deepEqual(g.savedSkill,row.goal.savedSkill);}row.persistence={millis:performance.now()-at,hash:after.expectedHash,revision:after.expectedRevision,skill:g.savedSkill||null};await report(g.id);await capture(label+'-saved');await close();
 }
 try{
  await save();await open(project,'learn');const learnedWorld=await create(group.training),learned=await run(group.training,learnedWorld,'parallel','simple',true);
  record.training.push(learned);await save();assert.equal(learned.status,'completed');record.receiptId=learned.goalId;await close();
  for(let repeat=0;repeat<protocol.calibrationRepeats;repeat++)for(let i=0;i<group.calibration.length;i++){
   const spec={...group.calibration[i],id:group.calibration[i].id+'-r'+repeat},pair={id:spec.id,taskIndex:i,repeat,results:{}};record.calibration.push(pair);
   const order=(repeat+i)%2?['memory','simple']:['simple','memory'];pair.order=order;
   for(const policy of order){await start(project,'fit-'+repeat+'-'+i+'-'+policy);const world=await create(spec),skillName=group.skill?'084-fit-'+repeat+'-'+i+'-'+policy:'';
    const row=await run(spec,world,'parallel',policy,false,skillName);pair.results[policy]=row;await save();assert.equal(row.status,'completed');assert.equal(row.bankBefore.sha256,row.bankAfter.sha256);
    if(policy==='memory'){assert(row.job.decision.useMemory);assert.equal(row.job.decision.candidate.id,record.receiptId);assert.equal(row.job.decision.candidate.warm,false);}
    if(group.skill)assert.equal(row.goal.savedSkill.name,skillName);await close();
   }
  }
  await open(project,'fit');const pairs=record.calibration.map(x=>({simpleId:x.results.simple.goalId,memoryId:x.results.memory.goalId})),fitAt=performance.now();
  record.model=await api('/api/procedures080/fit',{pairs});record.fitMillis=performance.now()-fitAt;
  assert.equal(record.model.version,'repeated-complete-task-cost-084-v1');assert.equal(record.model.pairs.length,6);
  for(const p of record.model.pairs){assert.equal(p.receiptId,record.receiptId);assert.equal(p.warm,false);assert.deepEqual(p.workload,{retain:false,skill:group.skill});}
  record.coverage=(await api('/api/procedures080/pairs')).coverage083;assert(record.coverage.profileMatches);assert.equal(record.coverage.groups.length,1);assert.equal(record.coverage.groups[0].repeatedTasks,2);assert.deepEqual(record.coverage.groups[0].repeatCounts,[3,3]);
  await page.locator('#spatial-tab').click();await page.locator('#persistent-world-panel').evaluate(e=>e.open=true);await page.locator('#cost080-panel').evaluate(e=>e.open=true);await page.locator('#cost080-review').click();
  await wait(async()=>(await page.locator('#cost083-coverage').innerText()).includes('Repeat counts: 3, 3'));
  for(const width of [1600,1100]){await app.evaluate(({BrowserWindow},w)=>BrowserWindow.getAllWindows().find(x=>x.isVisible()).setSize(w,1000),width);assert(await page.evaluate(()=>document.documentElement.scrollWidth<=innerWidth));await page.locator('#cost080-panel').scrollIntoViewIfNeeded();await capture('calibration-'+width);}
  record.frozenBankSha=sha(await fs.readFile(path.join(project,'world-procedures-079/bank.json')));record.modelSha=sha(await fs.readFile(path.join(project,'world-procedures-079/cost-models080',record.model.id+'.json')));await close();record.preparedFiles=await inventory(project);await check('Six verified comparison pairs: two tasks, three independent repetitions each, exact receipt/restart/saving boundaries');
  for(let repeat=0;repeat<protocol.evaluationRepeats;repeat++)for(let i=0;i<group.cases.length;i++){
   const spec={...group.cases[i],id:group.cases[i].id+'-r'+repeat},order=protocol.methods.map((_,k)=>protocol.methods[(k+i+repeat+protocol.groups.findIndex(g=>g.id===group.id))%protocol.methods.length]);
   for(const policy of order){const where=path.join(out,'evaluation-'+repeat+'-'+i+'-'+policy);await clone(project,where,record.preparedFiles);await start(where,'evaluate-'+repeat+'-'+i+'-'+policy);
    const world=await create(spec),skillName=group.skill?'084-held-'+repeat+'-'+i+'-'+policy:'',row=await run(spec,world,'parallel',policy,false,skillName);
    row.caseIndex=i;row.repeat=repeat;row.order=order;row.initialCloneVerified=true;record.rows.push(row);await save();assert.equal(row.status,'completed');assert.equal(row.bankBefore.sha256,record.frozenBankSha);assert.equal(row.bankAfter.sha256,record.frozenBankSha);
    if(policy!=='simple'){assert.equal(row.job.decision.candidate.id,record.receiptId);assert.equal(row.job.decision.candidate.warm,false);}
    if(policy==='calibrated'){assert(row.job.decision.cost080.available);assert.deepEqual(row.job.decision.cost080.repeatCounts,[3,3]);await report(row.goalId);await capture('case-'+repeat+'-'+i+'-report');await viewWorld(world);await capture('case-'+repeat+'-'+i+'-world');}
    assert.equal(sha(await fs.readFile(path.join(where,'world-procedures-079/cost-models080',record.model.id+'.json'))),record.modelSha);
    await stopAndRemember(row,world,'case-'+repeat+'-'+i+'-'+policy);await save();
   }
  }
  await check('All held-out controls completed; exact calibration frozen, world and saved skills survive reopen');
  require('../../scripts/Study-Build084.cjs').verify(repo);assert.deepEqual(errors,[]);record.errors=errors;record.passed=true;record.finishedAt=new Date().toISOString();await fs.writeFile(path.join(out,'result.json'),JSON.stringify(record,null,2));console.log('COMPLETE',mode,out);
 }catch(e){record.error=e.stack;await save();if(app)await capture('failure').catch(()=>{});throw e;}finally{if(app)await close().catch(e=>console.error('Closing:',e));}
}
main().catch(e=>{console.error(e);process.exitCode=1;});
