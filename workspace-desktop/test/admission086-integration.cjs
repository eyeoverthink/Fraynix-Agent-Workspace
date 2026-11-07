'use strict';
const {launch}=require('./calibration-integration.cjs'),fs=require('node:fs/promises'),sync=require('node:fs'),path=require('node:path'),assert=require('node:assert/strict'),cp=require('node:child_process'),crypto=require('node:crypto');
const sha=b=>crypto.createHash('sha256').update(b).digest('hex'),sleep=ms=>new Promise(r=>setTimeout(r,ms));
async function main(){
 const repo=path.resolve(__dirname,'../..'),protocol=JSON.parse(await fs.readFile(path.join(__dirname,'fixtures/procedures085-protocol.json'),'utf8')),budget=protocol.budget,mode='study',group={id:'growing-bank',warm:false,skill:true,training:protocol.training,calibration:protocol.calibration,cases:protocol.cases};
 assert(group,'Choose a frozen group');const buildReceipt=require('../../scripts/Study-Build086.cjs').verify(repo);
 process.env.FRAYNIX_WORKSPACE_PERSISTENT='0';process.env.FRAYNIX_WORKSPACE_PLAN_CPU_ONLY='1';process.env.FRAYNIX_WORKSPACE_JAVA_HEAP_MIB=String(budget.heapMiB);process.env.FRAYNIX_WORKSPACE_JAVA_PROCESSORS=String(budget.processors);
 const out=path.resolve(process.env.FRAYNIX086_OUTPUT||path.join(repo,'build/ui086-'+mode)),project=path.join(out,'preparation');await fs.mkdir(out,{recursive:true});assert(!sync.existsSync(path.join(out,'checkpoint.json')),'Use a fresh output');
 const record={source:cp.execFileSync('git',['rev-parse','HEAD'],{cwd:repo,encoding:'utf8'}).trim(),budget,group,mode,project,out,buildReceipt,startedAt:new Date().toISOString(),training:[],calibration:[],rows:[],warmups:[],boots:[],checks:[]};
 let app,page,currentProject,monitor,monitorExit,stopFile,boot,errors=[],bootIndex=0;
 const api=(route,args={})=>app.evaluate((_,x)=>global.workspaceTest.api(x.route,x.args),{route,args}),save=()=>fs.writeFile(path.join(out,'checkpoint.json'),JSON.stringify(record,null,2)),check=async text=>{record.checks.push(text);console.log('PASS',text);await save();};
 async function wait(fn,ms=1200000){const end=Date.now()+ms;while(Date.now()<end){const v=await fn();if(v)return v;await sleep(300);}throw Error('Bounded086 wait exceeded');}
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
 async function create(spec){const s=await api('/api/spatial/import',{text:JSON.stringify(spec.source)});return api('/api/world/create',{sceneId:s.id,title:'LABELLED086 '+spec.id});}
 async function binding(world){const o=await api('/api/world/observe',{worldId:world.id});return Object.fromEntries(['worldId','expectedRevision','expectedHash'].map(k=>[k,o[k]]));}
 async function run(spec,world,mode,policy,retain,skillName=''){
  const bankBefore=await fs.readFile(path.join(currentProject,'world-procedures-079/bank.json')).catch(()=>null);
  await api('/api/procedures081/test-mode',{mode});const b=await binding(world),before=await api('/api/resources082/state'),beforeOS=osRead('read',before.pid),at=performance.now(),samples=[];
  const viaChatToolBridge=policy==='simple'&&record.uiChoice?.worldId===world.id;
  const goal=viaChatToolBridge?await app.evaluate((_,name)=>global.workspaceTest.goals.execute('world.arrange',{saveSkillName:name},{}),skillName):await api('/api/world/start',{...b,targets:spec.targets,forceLimitN:spec.forceLimitN,torqueLimitNm:spec.torqueLimitNm,selectiveMemory079:true,memoryMode079:policy,retainExperience079:retain,...(skillName?{saveSkillName:skillName}:{}),...(policy!=='simple'?{procedureReceipt085:record.receiptId}:{}),request:'086 repeated-world '+spec.id});
  let sampleAt=performance.now();const settled=await wait(async()=>{const g=await api('/api/goals/read',{id:goal.id});if(performance.now()-sampleAt>=2000){samples.push(await api('/api/resources082/state'));sampleAt=performance.now();}return ['completed','cancelled','interrupted'].includes(g.status)?g:null;});
  const callerMillis=performance.now()-at,after=await api('/api/resources082/state'),afterOS=osRead('read',before.pid),scan=await api('/api/procedures081/diagnostics'),observed=await api('/api/world/observe',{worldId:world.id});
  const exportAt=performance.now(),original=await api('/api/procedures079/export',{id:goal.id}),file=spec.id+'-'+mode+'-'+goal.id+'-originals.json';await fs.writeFile(path.join(out,file),JSON.stringify(original));
  const row={viaChatToolBridge,ownerBootId:boot.bootId,bootLabel:boot.label,id:spec.id,goalId:goal.id,mode,policy,retain,skillName,project:currentProject,status:settled.status,reason:settled.reason,callerMillis,exportMillis:performance.now()-exportAt,scan,before,after,beforeOS,afterOS,samples,binding:b,observed,goal:settled,job:original.job,originalFile:file};
  row.retainedReceipt=await fs.readFile(path.join(currentProject,'world-procedures-079',goal.id,'memory.json'),'utf8').then(JSON.parse).catch(()=>null);
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
 async function start(where,label){await open(where,label);}
 async function stopAndRemember(row,world,label){
  const before=await api('/api/world/observe',{worldId:world.id});await close();const at=performance.now();await open(row.project,label+'-reopen');const after=await api('/api/world/observe',{worldId:world.id});assert.equal(after.expectedHash,before.expectedHash);assert.equal(after.expectedRevision,before.expectedRevision);
  const g=await api('/api/goals/read',{id:row.goalId});if(group.skill){assert.equal(g.savedSkill.name,row.skillName);assert.deepEqual(g.savedSkill,row.goal.savedSkill);}row.persistence={millis:performance.now()-at,hash:after.expectedHash,revision:after.expectedRevision,skill:g.savedSkill||null};await report(g.id);await capture(label+'-saved');if(label==='case-1-gated'){await viewWorld(world);await wait(async()=>(await page.locator('#persistent-status').innerText()).includes(after.expectedHash.slice(0,12)));await page.locator('#spatial-viewport').scrollIntoViewIfNeeded().catch(()=>{});await capture('verified-world');}await close();
 }
 async function retained(row){
  assert.equal(row.status,'completed');assert.equal(row.bankAfter.count,row.bankBefore.count+1);
  assert.equal(row.retainedReceipt.data.id,row.goalId);assert.equal(row.retainedReceipt.data.commitHash,row.observed.expectedHash);
  assert.equal(row.retainedReceipt.data.code,row.job.code);
  if(row.policy!=='simple'){assert.equal(row.job.decision.candidate.id,record.receiptId);assert.equal(row.job.decision.requestedReceipt085,record.receiptId);assert.equal(row.job.decision.candidate.warm,false);}
  if(row.skillName){assert.equal(row.goal.savedSkill.name,row.skillName);const ps=(await api('/api/goals/list')).procedures,p=ps.find(p=>p.id===row.goal.savedSkill.id);assert(p);assert.equal(p.sourceGoalId,row.goalId);assert.equal(p.code,row.job.code);assert.deepEqual(p.defaults.targets,row.job.args.targets);if(row.policy!=='simple')assert.equal(p.defaults.procedureReceipt085,record.receiptId);row.savedProcedure=p;}
 }
 async function showChoice(world,spec){
  await viewWorld(world);await page.locator('#procedure079-enabled').check();await page.selectOption('#cost080-mode','calibrated');
  await page.locator('#procedure079-memories').click();await wait(()=>page.locator('#procedure085-receipt option').evaluateAll((xs,id)=>xs.some(x=>x.value===id),record.receiptId));
  await page.selectOption('#procedure085-receipt',record.receiptId);
  await page.locator('#persistent-targets').evaluate(e=>{for(let x=e.parentElement;x;x=x.parentElement)if(x.tagName==='DETAILS')x.open=true;});
  await page.fill('#persistent-targets',JSON.stringify(spec.targets));await page.fill('#persistent-force',String(spec.forceLimitN));await page.fill('#persistent-torque',String(spec.torqueLimitNm));await page.locator('#persistent-apply').click();
  await wait(async()=>(await page.locator('#persistent-confirm-state').innerText()).includes('Confirmed revision'));
  assert.equal(await page.locator('#procedure085-receipt').inputValue(),record.receiptId);
  record.uiChoice={receipt:record.receiptId,worldId:world.id,confirmed:await page.locator('#persistent-confirm-state').innerText()};
  await page.locator('#procedure085-receipt').scrollIntoViewIfNeeded();await capture('confirmed-procedure');
 }
 try{
  const old=JSON.parse(await fs.readFile(path.resolve(repo,'../fraynix-workspace-edition085/build/ui085-study/result.json'),'utf8'));assert(old.passed);
  record.reusedEdition085={source:old.source,project:old.project,receiptId:old.receiptId,modelId:old.model.id,scope:'Existing exact compatible measurements; not new calibration trials'};record.receiptId=old.receiptId;
  const sourceFiles=await inventory(old.project);record.sourceFiles=sourceFiles;
  const projects={gated:path.join(out,'gated'),baseline:path.join(out,'baseline')};record.evaluationProjects=projects;
  for(const where of Object.values(projects))await clone(old.project,where,sourceFiles);
  const pairs=old.calibration.map(x=>({simpleId:x.results.simple.goalId,memoryId:x.results.memory.goalId}));record.selected=pairs;
  await open(projects.gated,'admission');const root=path.join(projects.gated,'world-procedures-079'),modelPointer=await fs.readFile(path.join(root,'cost-model080.json')),ledgerBefore=await fs.readFile(path.join(projects.gated,'persistent-worlds/state.json'));
  record.previews=[];for(let i=0;i<3;i++){const at=performance.now(),a=await api('/api/procedures086/preview',{pairs});record.previews.push({callerMillis:performance.now()-at,...a});assert(a.supported);assert(!a.admit);assert.equal(a.originalTraceBytes,0);}
  await page.locator('#spatial-tab').click();await page.locator('#persistent-world-panel').evaluate(e=>e.open=true);await page.locator('#cost080-panel').evaluate(e=>e.open=true);await page.locator('#cost080-review').click();
  await wait(()=>page.locator('#cost080-pairs input').count());
  const review=await api('/api/procedures086/review');for(const pair of pairs){const i=review.pairs.findIndex(p=>p.simpleId===pair.simpleId&&p.memoryId===pair.memoryId);assert(i>=0);await page.locator(`#cost080-pairs input[data-pair="${i}"]`).check();}
  const fitAt=performance.now();await page.locator('#cost080-fit').click();await wait(async()=>(await page.locator('#cost080-status').innerText()).includes('Fitting skipped'));record.gatedCallerMillis=performance.now()-fitAt;
  record.decision=(await api('/api/procedures086/history')).entries.at(-1);assert.equal(record.decision.status,'declined');assert(!record.decision.fitted);assert.equal(await page.locator('#cost080-mode').inputValue(),'simple');
  assert.equal(sha(await fs.readFile(path.join(root,'cost-model080.json'))),sha(modelPointer));assert.equal(sha(await fs.readFile(path.join(projects.gated,'persistent-worlds/state.json'))),sha(ledgerBefore));
  for(const width of [1600,1100]){await app.evaluate(({BrowserWindow},w)=>BrowserWindow.getAllWindows().find(x=>x.isVisible()).setSize(w,1100),width);await page.locator('#cost086-result').scrollIntoViewIfNeeded();await sleep(350);assert(await page.evaluate(()=>document.documentElement.scrollWidth<=innerWidth));await capture('admission-'+width);}
  await check('Real proposals decline fitting before original traces; visible action selects simple control and preserves prior model and world');await close();
  await open(projects.baseline,'unconditional-fit');const at=performance.now();record.model=await api('/api/procedures086/test-unconditional-fit',{pairs});record.fitMillis=performance.now()-at;assert.equal(record.model.code,old.model.code);assert.deepEqual(record.model.pairs,old.model.pairs);await close();
  await check('Unconditional test control really fitted the same six original pairs using unchanged full verification');
  for(let i=0;i<2;i++)for(const method of (i%2?['baseline','gated']:['gated','baseline'])){
    const policy=method==='gated'?'simple':'calibrated',spec=group.cases[i],where=projects[method];await open(where,'case-'+i+'-'+method);const world=await create(spec);
    if(i===0&&method==='gated'){
      await showChoice(world,spec);await page.selectOption('#cost080-mode','simple');await page.locator('#persistent-apply').click();await wait(async()=>(await page.locator('#persistent-confirm-state').innerText()).includes('Confirmed revision'));await capture('confirmed-simple');
    }
    const row=await run(spec,world,'parallel',policy,true,'route-086-'+i+'-'+method);row.method=method;row.caseIndex=i;record.rows.push(row);await retained(row);assert.equal(row.status,'completed');assert.equal(row.bankBefore.count,13+i);assert.equal(row.bankAfter.count,14+i);await save();
    await report(row.goalId);await capture('case-'+i+'-'+method+'-report');await stopAndRemember(row,world,'case-'+i+'-'+method);await save();
  }
  await open(projects.gated,'verify-refusal');const originals=[];const {canonical}=require('../../scripts/Inspect-Spatial040.cjs');const beforeModels=(await fs.readdir(path.join(root,'cost-models080'))).sort(),beforeWorld=sha(await fs.readFile(path.join(projects.gated,'persistent-worlds/state.json')));
  try{
    for(const pair of pairs)for(const k of ['simpleId','memoryId']){const file=path.join(root,pair[k],'cost080.json'),raw=await fs.readFile(file),v=JSON.parse(raw);originals.push({file,raw});v.data.totalMillis=k==='simpleId'?1000000:1000;v.sha256=sha(canonical(v.data));await fs.writeFile(file,JSON.stringify(v));}
    record.tamperedProposal=await api('/api/procedures086/preview',{pairs});assert(record.tamperedProposal.admit);let rejection='';try{await api('/api/procedures080/fit',{pairs});}catch(e){rejection=e.message;}assert(rejection.includes('Stored goal timing differs'));record.refusal={rejection,receipt:(await api('/api/procedures086/history')).entries.at(-1)};assert.equal(record.refusal.receipt.status,'verification-refused');
  }finally{for(const x of originals){await fs.writeFile(x.file,x.raw);assert.equal(sha(await fs.readFile(x.file)),sha(x.raw));}}
  assert.deepEqual((await fs.readdir(path.join(root,'cost-models080'))).sort(),beforeModels);assert.equal(sha(await fs.readFile(path.join(projects.gated,'persistent-worlds/state.json'))),beforeWorld);
  record.history=await api('/api/procedures086/history');await close();await open(projects.gated,'receipt-reopen');assert.deepEqual(await api('/api/procedures086/history'),record.history);await close();
  await check('Attractive forged summaries reached the original verifier and were refused; exact restoration, unchanged models/world, durable decision receipts');
  assert.deepEqual(await inventory(old.project),sourceFiles);require('../../scripts/Study-Build086.cjs').verify(repo);assert.deepEqual(errors,[]);record.passed=true;record.finishedAt=new Date().toISOString();await fs.writeFile(path.join(out,'result.json'),JSON.stringify(record,null,2));console.log('COMPLETE',out);
 }catch(e){record.error=e.stack;await save();if(app)await capture('failure').catch(()=>{});throw e;}finally{if(app)await close().catch(e=>console.error('Closing:',e));}
}
main().catch(e=>{console.error(e);process.exitCode=1;});
