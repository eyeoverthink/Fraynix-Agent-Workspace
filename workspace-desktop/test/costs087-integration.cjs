'use strict';
const {launch}=require('./calibration-integration.cjs'),fs=require('node:fs/promises'),sync=require('node:fs'),path=require('node:path'),assert=require('node:assert/strict'),cp=require('node:child_process'),crypto=require('node:crypto');
const sha=b=>crypto.createHash('sha256').update(b).digest('hex'),sleep=ms=>new Promise(r=>setTimeout(r,ms));
async function main(){
 const repo=path.resolve(__dirname,'../..'),protocol=JSON.parse(await fs.readFile(path.join(__dirname,'fixtures/procedures087-protocol.json'),'utf8')),budget=protocol.budget,mode='study',group={id:'warm-fixed-bank',warm:true,skill:false,training:protocol.training,calibration:protocol.calibration,cases:protocol.cases};
 assert(group,'Choose a frozen group');const buildReceipt=require('../../scripts/Study-Build087.cjs').verify(repo);
 process.env.FRAYNIX_WORKSPACE_PERSISTENT='0';process.env.FRAYNIX_WORKSPACE_PLAN_CPU_ONLY='1';process.env.FRAYNIX_WORKSPACE_JAVA_HEAP_MIB=String(budget.heapMiB);process.env.FRAYNIX_WORKSPACE_JAVA_PROCESSORS=String(budget.processors);
 const out=path.resolve(process.env.FRAYNIX087_OUTPUT||path.join(repo,'build/ui087-'+mode)),project=path.join(out,'preparation');await fs.mkdir(out,{recursive:true});assert(!sync.existsSync(path.join(out,'checkpoint.json')),'Use a fresh output');
 const record={fits:[],pilots:[],previews:[],source:cp.execFileSync('git',['rev-parse','HEAD'],{cwd:repo,encoding:'utf8'}).trim(),budget,group,mode,project,out,buildReceipt,startedAt:new Date().toISOString(),training:[],calibration:[],rows:[],warmups:[],boots:[],checks:[]};
 let app,page,currentProject,monitor,monitorExit,stopFile,boot,errors=[],bootIndex=0;
 const api=(route,args={})=>app.evaluate((_,x)=>global.workspaceTest.api(x.route,x.args),{route,args}),save=()=>fs.writeFile(path.join(out,'checkpoint.json'),JSON.stringify(record,null,2)),check=async text=>{record.checks.push(text);console.log('PASS',text);await save();};
 async function wait(fn,ms=1200000){const end=Date.now()+ms;while(Date.now()<end){const v=await fn();if(v)return v;await sleep(300);}throw Error('Bounded087 wait exceeded');}
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
 async function create(spec){const s=await api('/api/spatial/import',{text:JSON.stringify(spec.source)});return api('/api/world/create',{sceneId:s.id,title:'LABELLED087 '+spec.id});}
 async function binding(world){const o=await api('/api/world/observe',{worldId:world.id});return Object.fromEntries(['worldId','expectedRevision','expectedHash'].map(k=>[k,o[k]]));}
 async function run(spec,world,mode,policy,retain,skillName=''){
  const bankBefore=await fs.readFile(path.join(currentProject,'world-procedures-079/bank.json')).catch(()=>null);
  await api('/api/procedures081/test-mode',{mode});const b=await binding(world),before=await api('/api/resources082/state'),beforeOS=osRead('read',before.pid),at=performance.now(),samples=[];
  const viaChatToolBridge=policy==='simple'&&record.uiChoice?.worldId===world.id;
  const goal=viaChatToolBridge?await app.evaluate((_,name)=>global.workspaceTest.goals.execute('world.arrange',{saveSkillName:name},{}),skillName):await api('/api/world/start',{...b,targets:spec.targets,forceLimitN:spec.forceLimitN,torqueLimitNm:spec.torqueLimitNm,selectiveMemory079:true,memoryMode079:policy,retainExperience079:retain,...(skillName?{saveSkillName:skillName}:{}),...(policy!=='simple'?{procedureReceipt085:record.receiptId}:{}),request:'087 measured allowance '+spec.id});
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

 async function selectPairs(pairs){
  await page.locator('#spatial-tab').click();await page.locator('#persistent-world-panel').evaluate(e=>e.open=true);await page.locator('#cost080-panel').evaluate(e=>e.open=true);await page.locator('#cost080-review').click();
  await wait(()=>page.locator('#cost080-pairs input').count());const review=await api('/api/procedures086/review');
  for(const pair of pairs){const i=review.pairs.findIndex(p=>p.simpleId===pair.simpleId&&p.memoryId===pair.memoryId);assert(i>=0);await page.locator(`#cost080-pairs input[data-pair="${i}"]`).check();}
 }
 async function validateRows(rows){for(const row of rows){assert.equal(row.status,'completed');assert.equal(row.bankAfter.count,1);if(row.policy!=='simple'){assert.equal(row.job.decision.candidate.id,record.receiptId);assert.equal(row.job.decision.candidate.warm,true);}}}
 try{
  await open(project,'preparation-and-evaluation');
  const seed=await run(protocol.training,await create(protocol.training),'parallel','simple',true);assert.equal(seed.status,'completed');record.training.push(seed);record.receiptId=seed.goalId;await save();
  for(let repeat=0;repeat<3;repeat++)for(let i=0;i<2;i++){
   const spec={...protocol.calibration[i],id:`087-fit-${i}-${repeat}`},results={};
   for(const policy of ((repeat+i)%2?['memory','simple']:['simple','memory'])){results[policy]=await run(spec,await create(spec),'parallel',policy,false);await validateRows([results[policy]]);}
   record.calibration.push({id:spec.id,repeat,results});await save();
  }
  const pairs=record.calibration.map(x=>({simpleId:x.results.simple.goalId,memoryId:x.results.memory.goalId}));record.selected=pairs;
  record.initial=await api('/api/procedures086/preview',{pairs});assert(!record.initial.allowances087.supported);assert.equal(record.initial.budget.fitBudgetMillis,120000);assert.equal(record.initial.originalTraceBytes,0);await save();
  for(const budget of [null,[],3,'wrong'])await assert.rejects(api('/api/procedures086/preview',{pairs,budget}));
  await assert.rejects(api('/api/procedures086/preview',{pairs,allowanceMode:'fabricated'}));
  await assert.rejects(api('/api/procedures087/history',{millis:1}));
  await check('Absent observations use declared defaults; malformed budgets and caller-supplied timings refused');
  for(let i=0;i<3;i++){const at=performance.now(),model=await api('/api/procedures086/test-unconditional-fit',{pairs});record.fits.push({model,callerMillis:performance.now()-at});record.model=model;await save();}
  assert.equal((await api('/api/procedures087/history')).samples.filter(x=>x.kind==='fit').length,3);
  for(const spec of protocol.pilots){const row=await run(spec,await create(spec),'parallel','calibrated',false);record.pilots.push(row);await validateRows([row]);await save();}
  record.learningHistory=await api('/api/procedures087/history');assert.equal(record.learningHistory.samples.length,6);
  for(const horizon of [1,12,64,256]){const at=performance.now(),p=await api('/api/procedures086/preview',{pairs,budget:{...record.initial.budget,horizon}});record.previews.push({callerMillis:performance.now()-at,...p});assert(p.allowances087.supported);assert.equal(p.allowances087.fitSamples,3);assert.equal(p.allowances087.checkSamples,3);}
  record.frozenAllowances=record.previews[1];await check('Three actual verified fits and three calibrated tasks produce workload-matched learned allowances');
  await selectPairs(pairs);await page.fill('#cost086-horizon','12');await page.locator('#cost086-preview').click();await wait(async()=>(await page.locator('#cost087-status').innerText()).startsWith('Measured allowances'));
  for(const width of [1600,1100]){await app.evaluate(({BrowserWindow},w)=>BrowserWindow.getAllWindows().find(x=>x.isVisible()).setSize(w,1100),width);await page.locator('#cost087-status').scrollIntoViewIfNeeded();await sleep(300);assert(await page.evaluate(()=>document.documentElement.scrollWidth<=innerWidth));await capture('learned-allowances-'+width);}
  // Frozen pilot history remains the evaluation forecast. Later telemetry is not fed back into this comparison.
  for(let i=0;i<protocol.cases.length;i++){
   const order=['simple','memory','calibrated'];order.push(...order.splice(0,i%3));
   for(const policy of order){const spec=protocol.cases[i],row=await run(spec,await create(spec),'parallel',policy,false);record.rows.push(row);await validateRows([row]);await save();}
  }
  // Exercise the genuine guarded action at the shortest horizon when its measured preview declines.
  const p1=await api('/api/procedures086/preview',{pairs,budget:{...record.initial.budget,horizon:1}});record.finalPreview=p1;
  if(!p1.admit){await selectPairs(pairs);await page.fill('#cost086-horizon','1');const before=await fs.readFile(path.join(project,'world-procedures-079/cost-model080.json'));const began=performance.now();await page.locator('#cost080-fit').click();await wait(async()=>(await page.locator('#cost080-status').innerText()).includes('Fitting skipped'));record.guardedMillis=performance.now()-began;assert.equal(sha(before),sha(await fs.readFile(path.join(project,'world-procedures-079/cost-model080.json'))));record.guarded=(await api('/api/procedures086/history')).entries.at(-1);await capture('guarded-short-horizon');}
  // Repeated completed-goal reads must not manufacture repeated measurements.
  const h=await api('/api/procedures087/history');for(let i=0;i<3;i++)await api('/api/goals/read',{id:record.rows.at(-1).goalId});assert.deepEqual(await api('/api/procedures087/history'),h);record.finalHistory=h;
  const last=record.rows.at(-1),worldId=last.binding.worldId;record.finalWorld=await api('/api/world/observe',{worldId});await report(last.goalId);await capture('verified-report');
  await page.locator('#spatial-tab').click();await page.locator('#persistent-world-panel').evaluate(e=>e.open=true);await wait(()=>page.locator('#persistent-worlds option').evaluateAll((xs,id)=>xs.some(x=>x.value===id),worldId));await page.selectOption('#persistent-worlds',worldId);await page.locator('#spatial-stage').scrollIntoViewIfNeeded();await sleep(500);await capture('verified-world');
  await close();await open(project,'restart-verification');assert.deepEqual(await api('/api/procedures087/history'),record.finalHistory);const reopened=await api('/api/world/observe',{worldId});assert.equal(reopened.expectedHash,record.finalWorld.expectedHash);record.reopened={hash:reopened.expectedHash,revision:reopened.expectedRevision};
  const learned=await api('/api/procedures086/preview',{pairs});assert(learned.allowances087.supported);record.reopenedEstimate=learned;
  // Tampering changes proposal eligibility, never the original calibration's acceptance.
  const sample=h.samples.find(x=>x.kind==='fit'),source=path.join(project,'world-procedures-079/cost-models080',sample.modelId+'.json'),original=await fs.readFile(source);
  try{await fs.writeFile(source,Buffer.from('corrupted source'));const refused=await api('/api/procedures086/preview',{pairs});assert(!refused.allowances087.supported);assert(refused.allowances087.excludedSources.includes(sample.id));record.changedSource=refused;}finally{await fs.writeFile(source,original);}
  assert((await api('/api/procedures086/preview',{pairs})).allowances087.supported);await check('Completed measurements survive restart, repeated reads do not duplicate samples, changed model source falls back and exact restoration recovers');
  const root=path.join(project,'world-procedures-079'),saved=[];const {canonical}=require('../../scripts/Inspect-Spatial040.cjs');const modelNames=(await fs.readdir(path.join(root,'cost-models080'))).sort(),worldHash=sha(await fs.readFile(path.join(project,'persistent-worlds/state.json'))),count=(await api('/api/procedures087/history')).samples.length;
  try{
   for(const pair of pairs)for(const key of ['simpleId','memoryId']){const file=path.join(root,pair[key],'cost080.json'),raw=await fs.readFile(file),v=JSON.parse(raw);saved.push({file,raw});v.data.totalMillis=key==='simpleId'?1000000:1000;v.sha256=sha(canonical(v.data));await fs.writeFile(file,JSON.stringify(v));}
   const q={pairs,allowanceMode:'manual',budget:{horizon:256,fitBudgetMillis:1000,checkBudgetMillis:100,additionalRuns:0}};record.forgedPreview=await api('/api/procedures086/preview',q);assert(record.forgedPreview.admit);let rejection='';try{await api('/api/procedures080/fit',q);}catch(e){rejection=e.message;}assert(rejection.includes('Stored goal timing differs'));record.refusal={rejection,receipt:(await api('/api/procedures086/history')).entries.at(-1)};assert.equal(record.refusal.receipt.status,'verification-refused');
  }finally{for(const x of saved){await fs.writeFile(x.file,x.raw);assert.equal(sha(await fs.readFile(x.file)),sha(x.raw));}}
  assert.deepEqual((await fs.readdir(path.join(root,'cost-models080'))).sort(),modelNames);assert.equal(sha(await fs.readFile(path.join(project,'persistent-worlds/state.json'))),worldHash);assert.equal((await api('/api/procedures087/history')).samples.length,count);await check('Admitted forged summaries still fail original verification; failed fitting teaches no cheap allowance and no world/model changes');
  await close();
  const old=JSON.parse(await fs.readFile(path.resolve(repo,'../fraynix-workspace-edition085/build/ui085-study/result.json'),'utf8')),negative=path.join(out,'negative085');
  await fs.cp(old.project,negative,{recursive:true,filter:p=>!['desktop-profile','engine.log','test-desktop-process.log'].includes(path.basename(p))});
  await open(negative,'compatible-negative085');const oldPairs=old.calibration.map(x=>({simpleId:x.results.simple.goalId,memoryId:x.results.memory.goalId}));
  const oldModel=await fs.readFile(path.join(negative,'world-procedures-079/cost-model080.json')),oldWorld=await fs.readFile(path.join(negative,'persistent-worlds/state.json')),began=performance.now();
  const negativePreview=await api('/api/procedures086/preview',{pairs:oldPairs});assert(negativePreview.supported);assert(!negativePreview.admit);assert(!negativePreview.allowances087.supported);const negativeDecision=await api('/api/procedures080/fit',{pairs:oldPairs});assert.equal(negativeDecision.status,'declined');assert.equal(sha(oldModel),sha(await fs.readFile(path.join(negative,'world-procedures-079/cost-model080.json'))));assert.equal(sha(oldWorld),sha(await fs.readFile(path.join(negative,'persistent-worlds/state.json'))));
  record.negative085={source:old.source,modelCode:old.model.code,scope:'Exact compatible prior085 measurements, not new outcomes; separate cold retaining/skill workload, declared fallback',preview:negativePreview,decision:negativeDecision,callerMillis:performance.now()-began};assert.equal(old.model.code,record.model.code);await check('Compatible prior cold saving cohort still declines; no transfer of warm no-saving allowance or changes to prior model/world');
  record.errors=errors;assert.deepEqual(errors,[]);record.passed=true;record.finishedAt=new Date().toISOString();await close();require('../../scripts/Study-Build087.cjs').verify(repo);await fs.writeFile(path.join(out,'result.json'),JSON.stringify(record,null,2));console.log('PASS edition087 real-run study');
 }finally{if(app)await close();await save();}
}
main().catch(e=>{console.error(e);process.exitCode=1;});
