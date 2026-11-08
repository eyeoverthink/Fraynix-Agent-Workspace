'use strict';
const {launch}=require('./calibration-integration.cjs'),fs=require('node:fs/promises'),sync=require('node:fs'),path=require('node:path'),assert=require('node:assert/strict'),cp=require('node:child_process'),crypto=require('node:crypto');
const sha=b=>crypto.createHash('sha256').update(b).digest('hex'),sleep=ms=>new Promise(r=>setTimeout(r,ms));
async function main(){
 const repo=path.resolve(__dirname,'../..'),protocol=JSON.parse(await fs.readFile(path.join(__dirname,'fixtures/procedures090-followup.json'),'utf8')),budget={...protocol.budget},mode=budget.id,group={id:'warm-fixed-bank',warm:true,skill:false,training:protocol.training,calibration:protocol.calibration,cases:protocol.cases};
 assert(budget.id,'Choose four-768 or two-512');const buildReceipt=require('../../scripts/Study-Build090.cjs').verify(repo);
 process.env.FRAYNIX_WORKSPACE_PERSISTENT='0';process.env.FRAYNIX_WORKSPACE_PLAN_CPU_ONLY='1';process.env.FRAYNIX_WORKSPACE_JAVA_HEAP_MIB=String(budget.heapMiB);process.env.FRAYNIX_WORKSPACE_JAVA_PROCESSORS=String(budget.processors);
 const out=path.resolve(process.env.FRAYNIX090_FOLLOWUP_OUTPUT||path.join(repo,'build/ui090-followup')),project=path.join(out,'preparation');await fs.mkdir(out,{recursive:true});assert(!sync.existsSync(path.join(out,'checkpoint.json')),'Use a fresh output');
 const record={fits:[],pilots:[],previews:[],source:cp.execFileSync('git',['rev-parse','HEAD'],{cwd:repo,encoding:'utf8'}).trim(),budget,group,mode,project,out,buildReceipt,startedAt:new Date().toISOString(),training:[],calibration:[],rows:[],warmups:[],boots:[],checks:[],blocks:[],copies:[],armStates:{}};
 let app,page,currentProject,monitor,monitorExit,stopFile,boot,errors=[],bootIndex=0;
 const api=(route,args={})=>app.evaluate((_,x)=>global.workspaceTest.api(x.route,x.args),{route,args}),save=()=>fs.writeFile(path.join(out,'checkpoint.json'),JSON.stringify(record,null,2)),check=async text=>{record.checks.push(text);console.log('PASS',text);await save();};
 async function wait(fn,ms=1200000){const end=Date.now()+ms;while(Date.now()<end){const v=await fn();if(v)return v;await sleep(300);}throw Error('Bounded090 wait exceeded');}
 function osRead(action,pid,extra=[]){return JSON.parse(cp.execFileSync('pwsh',['-NoProfile','-File',path.join(repo,'scripts/Measure-Resources082.ps1'),'-Action',action,'-TargetPid',String(pid),'-Processors',String(budget.processors),'-Checkout',repo,...extra],{encoding:'utf8',windowsHide:true}));}
 async function open(where,label){
  label=String(++bootIndex)+'-'+label;
  const began=performance.now();currentProject=where;await fs.mkdir(where,{recursive:true});({app,page}=await launch(where));page.on('pageerror',e=>errors.push(e.message));await page.waitForFunction(()=>window.agency043View);
  await app.evaluate(({BrowserWindow})=>BrowserWindow.getAllWindows().find(w=>w.isVisible()).setSize(1600,1100));
  await app.evaluate(({BrowserWindow})=>BrowserWindow.getAllWindows().find(w=>w.isVisible()).setTitle('Fraynix - Edition 090 validation'));
  const resource=await api('/api/resources082/state');assert.equal(resource.profile.cpus,budget.processors);assert.equal(resource.configuredMaxHeapBytes,budget.heapMiB*1024*1024);
  const pinned=osRead('pin',resource.pid);assert.equal(BigInt(pinned.affinity).toString(2).split('1').length-1,budget.processors);
  const state=await api('/api/agent042/state');boot={label,project:where,pid:resource.pid,bootId:state.bootId,startupMillis:performance.now()-began,resources:resource,pinned};record.boots.push(boot);
  stopFile=path.join(out,label+'-stop');const file=path.join(out,label+'-os.jsonl');monitor=cp.spawn('pwsh',['-NoProfile','-File',path.join(repo,'scripts/Measure-Resources082.ps1'),'-Action','monitor','-TargetPid',String(resource.pid),'-Checkout',repo,'-OutputFile',file,'-StopFile',stopFile],{windowsHide:true,stdio:['ignore','pipe','pipe']});let monitorText='';monitor.stderr.on('data',b=>monitorText+=b);monitor.stdout.on('data',b=>monitorText+=b);monitorExit=new Promise((resolve,reject)=>{monitor.once('error',reject);monitor.once('exit',code=>code===0?resolve():reject(Error('Resource monitor: '+monitorText)));});boot.osFile=path.basename(file);await wait(async()=>sync.existsSync(file)&&(await fs.stat(file)).size>0,15000);await save();
 }
 async function close(){if(!app)return;boot.finalResources=await api('/api/resources082/state');boot.finalOS=osRead('read',boot.pid);await fs.writeFile(stopFile,'done');await monitorExit;await app.close();app=null;await save();}
 async function capture(name){await sleep(300);await fs.writeFile(path.join(out,name+'.png'),Buffer.from(await app.evaluate(()=>global.workspaceTest.capture()),'base64'));}
 async function report(id){await app.evaluate((_,id)=>global.workspaceTest.goals.execute('goal.read',{id}),id);await page.waitForFunction(id=>document.querySelector('#java-goal').dataset.goalId===id&&document.querySelector('#java-goal-status').textContent==='completed',id);await page.locator('#java-goal-report').click();await sleep(500);}
 async function create(spec){const s=await api('/api/spatial/import',{text:JSON.stringify(spec.source)});return api('/api/world/create',{sceneId:s.id,title:'LABELLED090 '+spec.id});}
 async function binding(world){const o=await api('/api/world/observe',{worldId:world.id});return Object.fromEntries(['worldId','expectedRevision','expectedHash'].map(k=>[k,o[k]]));}
 async function run(spec,world,mode,policy,retain,skillName=''){
  const runtimePolicy=['guarded090'].includes(policy)?'calibrated':policy;
  const bankBefore=await fs.readFile(path.join(currentProject,'world-procedures-079/bank.json')).catch(()=>null);
  await api('/api/procedures081/test-mode',{mode});const b=await binding(world),before=await api('/api/resources082/state'),beforeOS=osRead('read',before.pid),at=performance.now(),samples=[];
  const viaChatToolBridge=policy==='simple'&&record.uiChoice?.worldId===world.id;
  const goal=viaChatToolBridge?await app.evaluate((_,name)=>global.workspaceTest.goals.execute('world.arrange',{saveSkillName:name},{}),skillName):await api('/api/world/start',{...b,targets:spec.targets,forceLimitN:spec.forceLimitN,torqueLimitNm:spec.torqueLimitNm,selectiveMemory079:true,memoryMode079:runtimePolicy,retainExperience079:retain,...(skillName?{saveSkillName:skillName}:{}),...(policy!=='simple'&&!record.skipReceipt090?{procedureReceipt085:record.receiptId}:{}),request:'090 paired selection and sequence '+spec.id});
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

 async function inventory(dir){const rows=[];async function walk(base){for(const e of await fs.readdir(base,{withFileTypes:true})){if(['desktop-profile','engine.log','test-desktop-process.log','host.json'].includes(e.name))continue;const p=path.join(base,e.name);assert(!e.isSymbolicLink());if(e.isDirectory())await walk(p);else{const b=await fs.readFile(p);rows.push({path:path.relative(dir,p).split(path.sep).join('/'),bytes:b.length,sha256:sha(b)});}}}await walk(dir);return rows.sort((a,b)=>a.path.localeCompare(b.path));}
 async function clone(from,to,expected){assert(!sync.existsSync(to));await fs.cp(from,to,{recursive:true,filter:p=>!['desktop-profile','engine.log','test-desktop-process.log','host.json'].includes(path.basename(p))});assert.deepEqual(await inventory(to),expected);}

 const base=JSON.parse(await fs.readFile(path.join(repo,'build/ui090-study/result.json'),'utf8'));assert(base.passed);record.baseSource=base.source;record.receiptId=base.receiptId;record.model=base.model;record.baseProject=base.project;record.sourceInventory=await inventory(base.project);record.copies=[];
 const baseRow=base.rows.find(r=>r.goalId===base.finalReview.uncovered);
 try{
  for(let i=0;i<protocol.cases.length;i++){
   const spec=protocol.cases[i],order=[...protocol.policies];order.push(...order.splice(0,i%3));
   for(const policy of order){const target=path.join(out,spec.id+'-'+policy),copyAt=performance.now();await clone(base.project,target,record.sourceInventory);record.copies.push({project:target,policy,caseId:spec.id,callerMillis:performance.now()-copyAt});await open(target,spec.id+'-'+policy);
    const observed=await api('/api/world/observe',{worldId:baseRow.binding.worldId});assert.equal(observed.expectedHash,baseRow.observed.expectedHash);
    const warmSpec={...baseRow.job.spec,id:spec.id+'-'+policy+'-warmup',targets:baseRow.job.spec.targets.map(t=>{const o=observed.frame.originalScene.objects.find(o=>o.sourceId===t.sourceId),b=observed.frame.bodies.find(b=>b.id===o.id),q=b.rotation;return{sourceId:t.sourceId,positionM:b.position,yawDeg:Math.atan2(2*(q[3]*q[1]+q[0]*q[2]),1-2*(q[1]*q[1]+q[2]*q[2]))*180/Math.PI};})};
    record.skipReceipt090=true;let warm;try{warm=await run(warmSpec,{id:baseRow.binding.worldId},'parallel','memory',false);}finally{record.skipReceipt090=false;}
    record.warmups.push(warm);await save();assert.equal(warm.status,'completed');assert.equal(warm.job.lookupProof.mode,'full');assert.equal(warm.job.decision.candidate.warm,false);
    const row=await run(spec,await create(spec),'parallel',policy,false);row.condition=spec.condition;row.sequenceIndex=i;record.rows.push(row);await save();assert.equal(row.status,'completed');if(policy!=='simple')assert(row.job.decision.candidate.warm);await close();
   }await check('Shorter diagnostic '+spec.id+': all three native outcomes retained');
  }
  assert.deepEqual(errors,[]);record.errors=errors;record.passed=true;record.finishedAt=new Date().toISOString();require('../../scripts/Study-Build090.cjs').verify(repo);await fs.writeFile(path.join(out,'result.json'),JSON.stringify(record,null,2));console.log('PASS separately declared shorter-trip diagnostics');
 }finally{if(app)await close();await save();}
}
main().catch(e=>{console.error(e);process.exitCode=1;});
