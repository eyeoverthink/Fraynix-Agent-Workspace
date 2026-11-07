'use strict';
const {launch}=require('./calibration-integration.cjs'),fs=require('node:fs/promises'),sync=require('node:fs'),path=require('node:path'),assert=require('node:assert/strict'),cp=require('node:child_process'),crypto=require('node:crypto');
const sha=b=>crypto.createHash('sha256').update(b).digest('hex'),sleep=ms=>new Promise(r=>setTimeout(r,ms));
async function main(){
 const repo=path.resolve(__dirname,'../..'),protocol=JSON.parse(await fs.readFile(path.join(__dirname,'fixtures/procedures089-protocol.json'),'utf8')),budget={...protocol.budget},mode=budget.id,group={id:'warm-fixed-bank',warm:true,skill:false,training:protocol.training,calibration:protocol.calibration,cases:protocol.cases};
 assert(budget.id,'Choose four-768 or two-512');const buildReceipt=require('../../scripts/Study-Build089.cjs').verify(repo);
 process.env.FRAYNIX_WORKSPACE_PERSISTENT='0';process.env.FRAYNIX_WORKSPACE_PLAN_CPU_ONLY='1';process.env.FRAYNIX_WORKSPACE_JAVA_HEAP_MIB=String(budget.heapMiB);process.env.FRAYNIX_WORKSPACE_JAVA_PROCESSORS=String(budget.processors);
 const out=path.resolve(process.env.FRAYNIX089_OUTPUT||path.join(repo,'build/ui089-study')),project=path.join(out,'preparation');await fs.mkdir(out,{recursive:true});assert(!sync.existsSync(path.join(out,'checkpoint.json')),'Use a fresh output');
 const record={fits:[],pilots:[],previews:[],source:cp.execFileSync('git',['rev-parse','HEAD'],{cwd:repo,encoding:'utf8'}).trim(),budget,group,mode,project,out,buildReceipt,startedAt:new Date().toISOString(),training:[],calibration:[],rows:[],warmups:[],boots:[],checks:[],blocks:[],copies:[],armStates:{}};
 let app,page,currentProject,monitor,monitorExit,stopFile,boot,errors=[],bootIndex=0;
 const api=(route,args={})=>app.evaluate((_,x)=>global.workspaceTest.api(x.route,x.args),{route,args}),save=()=>fs.writeFile(path.join(out,'checkpoint.json'),JSON.stringify(record,null,2)),check=async text=>{record.checks.push(text);console.log('PASS',text);await save();};
 async function wait(fn,ms=1200000){const end=Date.now()+ms;while(Date.now()<end){const v=await fn();if(v)return v;await sleep(300);}throw Error('Bounded089 wait exceeded');}
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
 async function create(spec){const s=await api('/api/spatial/import',{text:JSON.stringify(spec.source)});return api('/api/world/create',{sceneId:s.id,title:'LABELLED089 '+spec.id});}
 async function binding(world){const o=await api('/api/world/observe',{worldId:world.id});return Object.fromEntries(['worldId','expectedRevision','expectedHash'].map(k=>[k,o[k]]));}
 async function run(spec,world,mode,policy,retain,skillName=''){
  const runtimePolicy=['reference085','paired089'].includes(policy)?'calibrated':policy;
  const bankBefore=await fs.readFile(path.join(currentProject,'world-procedures-079/bank.json')).catch(()=>null);
  await api('/api/procedures081/test-mode',{mode});const b=await binding(world),before=await api('/api/resources082/state'),beforeOS=osRead('read',before.pid),at=performance.now(),samples=[];
  const viaChatToolBridge=policy==='simple'&&record.uiChoice?.worldId===world.id;
  const goal=viaChatToolBridge?await app.evaluate((_,name)=>global.workspaceTest.goals.execute('world.arrange',{saveSkillName:name},{}),skillName):await api('/api/world/start',{...b,targets:spec.targets,forceLimitN:spec.forceLimitN,torqueLimitNm:spec.torqueLimitNm,selectiveMemory079:true,memoryMode079:runtimePolicy,retainExperience079:retain,...(skillName?{saveSkillName:skillName}:{}),...(policy!=='simple'?{procedureReceipt085:record.receiptId}:{}),request:'089 paired selection and sequence '+spec.id});
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

 async function inventory(dir){const rows=[];async function walk(base){for(const e of await fs.readdir(base,{withFileTypes:true})){if(['desktop-profile','engine.log','test-desktop-process.log','host.json'].includes(e.name))continue;const p=path.join(base,e.name);assert(!e.isSymbolicLink());if(e.isDirectory())await walk(p);else{const b=await fs.readFile(p);rows.push({path:path.relative(dir,p).split(path.sep).join('/'),bytes:b.length,sha256:sha(b)});}}}await walk(dir);return rows.sort((a,b)=>a.path.localeCompare(b.path));}
 async function clone(from,to,expected){assert(!sync.existsSync(to));await fs.cp(from,to,{recursive:true,filter:p=>!['desktop-profile','engine.log','test-desktop-process.log','host.json'].includes(path.basename(p))});assert.deepEqual(await inventory(to),expected);}
 function selector(policy){if(policy==='reference085')process.env.FRAYNIX_WORKSPACE_REFERENCE089='reference085';else delete process.env.FRAYNIX_WORKSPACE_REFERENCE089;}
 async function warmup(policy,block){const spec={...protocol.training,id:`089-warm-${policy}-${block}`};const row=await run(spec,await create(spec),'parallel','memory',false);row.armPolicy=policy;row.block=block;record.warmups.push(row);assert.equal(row.status,'completed');assert.equal(row.job.decision.candidate.warm,false);assert.equal(row.job.lookupProof.mode,'full');const began=performance.now(),m=await api('/api/procedures080/calibration'),modelWarmMillis=performance.now()-began;assert.deepEqual(m,record.model);await save();return{goalId:row.goalId,callerMillis:row.callerMillis,modelWarmMillis,scan:await api('/api/procedures081/diagnostics')};}
 try{
  selector('paired089');await open(project,'preparation');
  const seed=await run(protocol.training,await create(protocol.training),'parallel','simple',true);assert.equal(seed.status,'completed');record.training.push(seed);record.receiptId=seed.goalId;await save();
  for(let repeat=0;repeat<3;repeat++)for(let i=0;i<2;i++){
   const spec={...protocol.calibration[i],id:`089-fit-${i}-${repeat}`},results={};
   for(const policy of ((repeat+i)%2?['memory','simple']:['simple','memory'])){results[policy]=await run(spec,await create(spec),'parallel',policy,false);await validateRows([results[policy]]);}
   record.calibration.push({id:spec.id,repeat,results});await save();
  }
  const pairs=record.calibration.map(x=>({simpleId:x.results.simple.goalId,memoryId:x.results.memory.goalId}));record.selected=pairs;
  record.initial=await api('/api/procedures086/preview',{pairs});assert(!record.initial.allowances087.supported);
  for(let i=0;i<3;i++){const at=performance.now(),model=await api('/api/procedures086/test-unconditional-fit',{pairs});record.fits.push({model,callerMillis:performance.now()-at});record.model=model;await save();}
  assert.equal(record.model.version,'paired-complete-task-cost-089-v1');
  for(const p of record.model.pairs){const given=record.calibration.find(x=>x.results.simple.goalId===p.simpleId);const expected=require('../../scripts/Inspect-Procedures089.cjs').pairing089(given.results.simple.goal.taskCost080,given.results.memory.goal.taskCost080);assert.deepEqual(p.pairing089,expected);}
  for(const spec of protocol.pilots){const row=await run(spec,await create(spec),'parallel','paired089',false);record.pilots.push(row);await validateRows([row]);await save();}
  record.learningHistory=await api('/api/procedures087/history');assert.equal(record.learningHistory.samples.length,6);
  for(const horizon of [2,4,8,12]){const at=performance.now(),p=await api('/api/procedures086/preview',{pairs,budget:{...record.initial.budget,horizon}});record.previews.push({callerMillis:performance.now()-at,...p});assert(p.allowances087.supported);}
  record.frozenAllowances=record.previews.at(-1);assert.equal(record.frozenAllowances.budget.horizon,12);
  await selectPairs(pairs);await page.fill('#cost086-horizon','12');await page.locator('#cost086-preview').click();await wait(async()=>(await page.locator('#cost087-status').innerText()).startsWith('Measured allowances'));
  for(const width of [1600,1100]){await app.evaluate(({BrowserWindow},w)=>BrowserWindow.getAllWindows().find(x=>x.isVisible()).setSize(w,1100),width);await page.locator('#cost089-explanation').scrollIntoViewIfNeeded();await sleep(300);assert(await page.evaluate(()=>document.documentElement.scrollWidth<=innerWidth));await capture('paired-controls-'+width);}
  // Declared before outcomes. Evaluation never refits the model or changes the admission formula.
  record.frozenModelSha=sha(JSON.stringify(record.model));await check('New original timing pairs, three actual fits and three pilots frozen before twelve-task evaluation');
  await close();record.preparationInventory=await inventory(project);
  for(const policy of protocol.policies){const target=path.join(out,'arms',policy),at=performance.now();await clone(project,target,record.preparationInventory);record.copies.push({policy,project:target,callerMillis:performance.now()-at});}
  for(let block=0;block<3;block++){
   const order=[...protocol.policies];order.push(...order.splice(0,block));
   for(const policy of order){selector(policy);const copy=record.copies.find(x=>x.policy===policy);await open(copy.project,`${policy}-block-${block}`);
    if(record.armStates[policy]){const prior=record.armStates[policy],now=await api('/api/world/observe',{worldId:prior.worldId});assert.equal(now.expectedHash,prior.hash);assert.equal(now.expectedRevision,prior.revision);}
    const prepared=await warmup(policy,block),entry={policy,block,bootLabel:boot.label,startupMillis:boot.startupMillis,...prepared,rows:[]};record.blocks.push(entry);
    for(let i=block*4;i<(block+1)*4;i++){const spec=protocol.cases[i],row=await run(spec,await create(spec),'parallel',policy,false);row.sequenceIndex=i;row.block=block;record.rows.push(row);entry.rows.push(row.goalId);await validateRows([row]);assert.equal(row.job.selector089,policy==='reference085'?'reference085':'paired089');if(['paired089','reference085'].includes(policy))assert.deepEqual(row.job.calibration080,record.model);record.armStates[policy]={worldId:row.binding.worldId,hash:row.observed.expectedHash,revision:row.observed.expectedRevision};await save();}
    assert.deepEqual(await api('/api/procedures080/calibration'),record.model);if(block===2&&policy==='reference085'){await report(record.rows.at(-1).goalId);await capture('reference-decision-report');}await close();await check(`${policy} block ${block+1}: four actual outcomes; model and bank retained`);
   }
  }
  selector('paired089');await open(project,'refusal-checks');
  const root=path.join(project,'world-procedures-079'),{canonical}=require('../../scripts/Inspect-Spatial040.cjs'),reseal=v=>{v.sha256=sha(canonical(v.data));return JSON.stringify(v);};
  const modelPath=path.join(root,'cost-models080',record.model.id+'.json'),modelBytes=await fs.readFile(modelPath),beforeWorld=sha(await fs.readFile(path.join(project,'persistent-worlds/state.json'))),count=(await api('/api/procedures087/history')).samples.length;
  let error='';try{const bad=JSON.parse(modelBytes);bad.data.pairs[0].pairing089.gapMillis+=1;await fs.writeFile(modelPath,reseal(bad));await api('/api/procedures080/calibration');}catch(e){error=e.message;}finally{await fs.writeFile(modelPath,modelBytes);}assert(error.includes('Calibration arithmetic changed'));record.pairingRefusal=error;assert.deepEqual(await api('/api/procedures080/calibration'),record.model);
  const clockPath=path.join(root,pairs[0].simpleId,'cost080.json'),clockBytes=await fs.readFile(clockPath);error='';try{const bad=JSON.parse(clockBytes);bad.data.clock089.end=String(BigInt(bad.data.clock089.end)+1000000n);await fs.writeFile(clockPath,reseal(bad));await api('/api/procedures080/calibration');}catch(e){error=e.message;}finally{await fs.writeFile(clockPath,clockBytes);}assert(error.includes('Owned timing interval differs'));record.clockRefusal=error;
  const saved=[],modelNames=(await fs.readdir(path.join(root,'cost-models080'))).sort();try{
   for(const pair of pairs)for(const key of ['simpleId','memoryId']){const file=path.join(root,pair[key],'cost080.json'),raw=await fs.readFile(file),v=JSON.parse(raw);saved.push({file,raw});v.data.totalMillis=key==='simpleId'?1000000:1000;await fs.writeFile(file,reseal(v));}
   const q={pairs,allowanceMode:'manual',budget:{horizon:256,fitBudgetMillis:1000,checkBudgetMillis:100,additionalRuns:0}};record.forgedPreview=await api('/api/procedures086/preview',q);assert(record.forgedPreview.admit);let rejection='';try{await api('/api/procedures080/fit',q);}catch(e){rejection=e.message;}assert(rejection.includes('Stored goal timing differs'));record.refusal={rejection,receipt:(await api('/api/procedures086/history')).entries.at(-1)};assert.equal(record.refusal.receipt.status,'verification-refused');
  }finally{for(const x of saved){await fs.writeFile(x.file,x.raw);assert.equal(sha(await fs.readFile(x.file)),sha(x.raw));}}
  assert.deepEqual((await fs.readdir(path.join(root,'cost-models080'))).sort(),modelNames);assert.equal(sha(await fs.readFile(path.join(project,'persistent-worlds/state.json'))),beforeWorld);assert.equal((await api('/api/procedures087/history')).samples.length,count);assert.deepEqual(await api('/api/procedures080/calibration'),record.model);await check('Changed pairing metadata, changed owned clocks and attractive forged costs refused; exact restoration; no world/model or successful allowance changes');await close();
  const final=record.rows.filter(x=>x.policy==='paired089').at(-1);await open(final.project,'final-review');const world=await api('/api/world/observe',{worldId:final.binding.worldId});assert.equal(world.expectedHash,final.observed.expectedHash);record.finalWorld=world;await report(final.goalId);await capture('paired-decision-report');
  await page.locator('#spatial-tab').click();await page.locator('#persistent-world-panel').evaluate(e=>e.open=true);await wait(()=>page.locator('#persistent-worlds option').evaluateAll((xs,id)=>xs.some(x=>x.value===id),final.binding.worldId));await page.selectOption('#persistent-worlds',final.binding.worldId);await page.locator('#spatial-frame').click();await page.locator('#spatial-stage').scrollIntoViewIfNeeded();await sleep(400);await capture('verified-world');
  record.errors=errors;assert.deepEqual(errors,[]);record.passed=true;record.finishedAt=new Date().toISOString();await close();require('../../scripts/Study-Build089.cjs').verify(repo);await fs.writeFile(path.join(out,'result.json'),JSON.stringify(record,null,2));console.log('PASS edition089 actual long-sequence study');
 }finally{if(app)await close();await save();}
}
main().catch(e=>{console.error(e);process.exitCode=1;});
