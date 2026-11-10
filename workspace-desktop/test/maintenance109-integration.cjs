'use strict';
// Isolated, actual Electron/Java qualification. Browser checks are observed, never supplied as passing fixtures.
const {launch}=require('./calibration-integration.cjs'),fs=require('node:fs/promises'),sync=require('node:fs'),path=require('node:path'),assert=require('node:assert/strict'),cp=require('node:child_process');
const crypto=require('node:crypto');
const sha=b=>crypto.createHash('sha256').update(b).digest('hex');
const {partition}=require('../../scripts/WorkflowCost102.cjs');
const repo=path.resolve(__dirname,'../..'),out=path.resolve(process.env.EDITION109_OUTPUT||path.join(repo,'build/maintenance109')),sleep=ms=>new Promise(r=>setTimeout(r,ms));
const sequence=(n,fn,dt=.1)=>'time_s,displacement_m\n'+Array.from({length:n},(_,i)=>(i*dt).toFixed(8)+','+fn(i).toFixed(8)).join('\n')+'\n';
async function main(){
 Object.assign(process.env,{FRAYNIX_WORKSPACE_PERSISTENT:'0',FRAYNIX_WORKSPACE_PLAN_CPU_ONLY:'1',FRAYNIX_WORKSPACE_JAVA_HEAP_MIB:'512',FRAYNIX_WORKSPACE_JAVA_PROCESSORS:'2'});
 require('../../scripts/Study-Build095.cjs').verify(repo);assert(!sync.existsSync(out),'Preserve original trials; choose a fresh output directory');await fs.mkdir(out,{recursive:true});

 const record={source:cp.execFileSync('git',['rev-parse','HEAD'],{cwd:repo,encoding:'utf8'}).trim(),startedAt:new Date().toISOString(),tasks:[],invalidInputs:[],checkpoints:[],hostSpans:[],injections:[],captures:[],sessions:[],scope:'Actual Java/Electron maintenance as a genuine saved-procedure bank grows to the first original capacity limit. Paired cold recheck/full-refresh, abrupt near-capacity recovery and native admission refusals. Original verifiers unchanged. Declared synthetic inputs, not real footage; no model-weight training. All caller/refusal costs retained.'};
 let app,page,project,session=0;const save=()=>fs.writeFile(path.join(out,'result.json'),JSON.stringify(record,null,2));
 const api=(route,args={})=>app.evaluate((_,x)=>global.workspaceTest.api(x.route,x.args),{route,args:route==='/api/state'?undefined:args});
 const request=(operation,command,args={})=>page.evaluate(x=>window.workspace.request(x),{operation,command,args});
 async function open(p){project=p;await fs.mkdir(project,{recursive:true});const start=Date.now();({app,page}=await launch(project));session++;
  await app.evaluate(({BrowserWindow},session)=>{
   const win=BrowserWindow.getAllWindows().find(w=>w.isVisible());win.setSize(1600,1100);win.setTitle('Fraynix - edition 109 - measured maintenance');
   global.numerical109={spans:[],injections:[],corrupt:false,gate:0,seen:0,blocked:null};const Surface=global.workspaceTest.surfaceClass102;
   for(const method of ['test','isolated']){const original=Surface.prototype[method];Surface.prototype[method]=async function(...args){if(this.kind!=='canvas')return original.apply(this,args);const state=global.numerical109;const matched=state.gate&&method==='isolated'&&String(args[0]).includes("one('table#data-table')")?(await global.workspaceTest.api('/api/workflows/list',{})).workflows.find(w=>w.qualification105&&w.status==='running'&&w.qualification105.checked===state.gate-1):null;if(matched){state.blocked={at:Date.now(),method,boundary:state.gate,checked:matched.qualification105.checked,workflowId:matched.id,scope:'Actual browser operation held before execution, then Java process terminated. No passing observation supplied.'};state.gate='';await new Promise(()=>{});}const start=Date.now(),clock=process.hrtime.bigint();let ok=false;try{
    if(method==='isolated'&&global.numerical109.corrupt&&String(args[0]).includes("one('table#data-table')")){global.numerical109.corrupt=false;const mutation=await original.call(this,"(()=>{const e=document.querySelector('#summary-travel');if(!e)throw Error('No fault target');const before=e.textContent;e.textContent='999.000000';return{before,after:e.textContent}})()");global.numerical109.injections.push({at:Date.now(),session,...mutation,scope:'Deliberately altered a displayed qualification value after behavior checks. Sources and authored code remain unchanged.'});}
    const v=await original.apply(this,args);ok=true;return v;
   }finally{global.numerical109.spans.push({kind:method==='test'?'browser-test':'browser-observation',session,start,end:Date.now(),nanos:String(process.hrtime.bigint()-clock),ok});}};}
  },session);
  await page.waitForFunction(()=>window.inputs100&&[...document.querySelector('#model').options].some(o=>o.value==='gpt-oss:20b'),{},{timeout:65000});await page.locator('#model').selectOption('gpt-oss:20b');record.sessions.push({session,project,start,ready:Date.now()});await save();
 }
 async function flush(){if(!app)return;const x=await app.evaluate(()=>{const s=global.numerical109,v={spans:s.spans,injections:s.injections};s.spans=[];s.injections=[];return v;});record.hostSpans.push(...x.spans);record.injections.push(...x.injections);await save();}
 async function close(){if(!app)return;await flush();await app.close();app=null;}
 async function capture(name){await sleep(250);await fs.writeFile(path.join(out,name),Buffer.from(await app.evaluate(()=>global.workspaceTest.capture()),'base64'));record.captures.push({name,project,at:Date.now()});}
 async function imported(name,csv,{measured=false,mapping={},missingPolicy='reject'}={}){const source=await request('data','import',{title:name,filename:name+'.csv',csv,sourceKind:measured?'measured':'synthetic',sourceNote:measured?'Unchanged Delfys013 recording, not a new physical measurement.':'Declared arithmetic109 software test, not footage or physical measurement.',missingPolicy,mapping:{timeColumn:'t',distanceColumn:'x',timeUnit:'s',distanceUnit:'m',...mapping}});await request('data','select',{id:source.datasetId,version:source.version});return api('/api/measurement/read',{id:source.datasetId,version:source.version});}
 async function readWorkflow(task){const id=task.completion044?.javaWorkflow?.id||task.context044?.ownedWorkflowId;return id?api('/api/workflows/read',{id}):null;}
 async function collectChild(id){const workflow=await api('/api/workflows/read',{id}),children=[],artifacts=[];for(const s of workflow.stages){if(!s.goalId)continue;const g=await api('/api/goals/read',{id:s.goalId});children.push(g);if(g.report)artifacts.push(await api('/api/artifact/read',{id:g.report.id}));}const b=workflow.dataBinding;return{workflow,children,artifacts,source:await api('/api/measurement/read',{id:b.datasetId,version:b.datasetVersion})};}
 async function run(name,source,{kind='growth',bank=0,expected='completed',restart=false,fault=false,prompt:givenPrompt,stop=true,rep=null}={}){
  const prompt=givenPrompt||`Qualify the input-driven summary, analyze the selected data, and save workflow as ${name}.`,row={name,kind,bank,rep,source,prompt,expected,project,captureKey:name,bankBefore:(await api('/api/state')).procedures};row.before=await inventory();assert.equal(row.bankBefore.length,bank);record.tasks.push(row);
  if(restart)await app.evaluate((_,gate)=>global.numerical109.gate=gate,restart);if(fault)await app.evaluate(()=>global.numerical109.corrupt=true);const previous=new Set((await api('/api/agency043/state')).tasks.map(t=>t.id));
  await page.waitForFunction(()=>!document.querySelector('#run').disabled,{},{timeout:30000});row.start=Date.now();await save();await page.locator('#goal').fill(prompt);await page.locator('#run').click();
  for(let i=0;i<150;i++){const t=(await api('/api/agency043/state')).tasks.find(t=>!previous.has(t.id)&&t.goal===prompt);if(t){row.id=t.id;break;}await sleep(200);}assert(row.id,'Chat request not admitted');
  let last='',crashed=false;const deadline=Date.now()+1_200_000;
  while(Date.now()<deadline){row.task=await api('/api/agency043/read',{id:row.id});const w=await readWorkflow(row.task),status=row.task.status+' '+(w?w.step+' '+w.qualification105?.checked+'/5':row.task.reason);if(status!==last){console.log(name+': '+status);last=status;}
   const blocked=restart&&!crashed?await app.evaluate(()=>global.numerical109.blocked):null;
   if(blocked){
    assert(w);assert.equal(w.qualification105.checked,restart-1);assert.equal(blocked.workflowId,w.id);row.boundary=w;row.boundaryOriginal=await collectChild(w.activeWorkflow105.id);assert.equal(row.boundaryOriginal.artifacts.length,2);row.heldOperation=blocked;row.pauseStart=blocked.at;row.restart=true;await capture(name+'-interrupted.png');await flush();row.crashReceipt=await app.evaluate(()=>global.workspaceTest.crashEngine());await sleep(300);await app.close().catch(()=>{});app=null;await open(project);row.restored=await api('/api/workflows/read',{id:w.id});row.restoredOriginal=await collectChild(row.restored.activeWorkflow105.id);assert.equal(row.restored.status,'interrupted');assert.deepEqual(row.restored.qualification105.maintenance107,w.qualification105.maintenance107);for(let j=0;j<6;j++)assert.equal(row.restored.stages[j].workflowId,w.stages[j].workflowId);for(let i=0;i<2;i++){assert.equal(row.restoredOriginal.workflow.stages[i].goalId,row.boundaryOriginal.workflow.stages[i].goalId);assert.equal(row.restoredOriginal.artifacts[i].id,row.boundaryOriginal.artifacts[i].id);assert.equal(row.restoredOriginal.artifacts[i].version,row.boundaryOriginal.artifacts[i].version);assert.deepEqual(row.restoredOriginal.artifacts[i].contents,row.boundaryOriginal.artifacts[i].contents);}
    await page.locator('#mission044-open').click();await page.locator('#mission044-task').selectOption(row.id);await page.waitForFunction(()=>!document.querySelector('#mission044-resume').disabled,{},{timeout:30000});await page.locator('#mission044-resume').click();for(let i=0;i<100;i++){if((await api('/api/agency043/read',{id:row.id})).resumes>0)break;await sleep(100);}row.pauseEnd=Date.now();crashed=true;continue;
   }
   if(['completed','interrupted','cancelled'].includes(row.task.status)){assert.equal(row.task.status,expected,'Unexpected task result: '+row.task.reason);row.end=Date.now();row.workflow=w;break;}await sleep(250);
  }
  assert(row.end,'Bounded caller exceeded 1200s');if(restart)assert(crashed);row.children=[];
  if(row.workflow){if(row.workflow.qualification105){for(const s of row.workflow.stages)if(s.workflowId)row.children.push(await collectChild(s.workflowId));}else row.children.push(await collectChild(row.workflow.id));}
  row.bankAfter=(await api('/api/state')).procedures;row.allWorkflows=(await api('/api/workflows/list',{})).workflows;row.nativeGoals=(await api('/api/goals/list',{})).goals;row.after=await inventory();
  let jobs=[];try{jobs=JSON.parse(await fs.readFile(path.join(project,'creation-044/requests.json'),'utf8')).data.jobs;}catch(e){if(e.code!=='ENOENT')throw e;}row.generation=jobs;assert.equal(jobs.length,0);await flush();
  const spans=record.hostSpans.filter(s=>s.start>=row.start&&s.end<=row.end).map(s=>({...s,kind:'browser-verification'}));if(row.restart)spans.push({kind:'pause-restart',start:row.pauseStart,end:row.pauseEnd});row.cost=partition(row.start,row.end,spans);
  await page.locator('#mission044-open').click();await page.locator('#mission044-task').selectOption(row.id);
  if(expected==='completed'){
   assert(row.workflow.audit.passed);assert.equal(row.bankAfter.length,row.bankBefore.length+1);assert.equal(row.workflow.qualification105.checked,5);assert(row.bankAfter.some(p=>p.id===row.workflow.savedSkill.id));await request('agency043','mission-open',{id:row.id});
  }else{
   assert.equal(row.bankAfter.length,row.bankBefore.length);if(fault){assert(row.workflow);assert(!row.workflow.savedSkill);await assert.rejects(()=>api('/api/workflows/resume',{id:row.workflow.id}));row.resumeRefused=true;assert(await page.locator('#mission044-resume').isDisabled());}
  }
  if(row.task.refusal106){assert(row.task.reason.includes('Created artifact was changed')||row.task.reason.includes('Original qualification dataset was revised')||row.task.reason.includes('Insufficient original workflow/goal capacity'));assert.deepEqual(row.bankAfter,row.bankBefore);assert.deepEqual(row.after.workflowStore,row.before.workflowStore);assert.equal(row.after.workflows,row.before.workflows);assert.equal(row.after.goals,row.before.goals);assert.equal(row.task.timing052.modelCalls,0);assert.equal(row.task.failures,0);assert(!row.workflow);await assert.rejects(()=>request('agency043','control',{id:row.id,action:'resume'}));assert(await page.locator('#mission044-resume').isDisabled());row.resumeRefused=true;}
  await capture(row.captureKey+'-1600.png');await save();if(expected!=='completed'&&stop){await request('agency043','control',{id:row.id,action:'cancel'});await sleep(500);}return row;
 }
 async function inventory(){
  const workflows=(await api('/api/workflows/list',{})).workflows,goals=(await api('/api/goals/list',{})).goals;
  const file=path.join(project,'workflows/tasks.fworld');let b;try{b=await fs.readFile(file);}catch(e){if(e.code!=='ENOENT')throw e;b=Buffer.alloc(0);}
  const paths=await fs.readdir(project,{recursive:true,withFileTypes:true});let bytes=0,files=0;
  for(const p of paths)if(p.isFile()){const f=path.join(p.parentPath||p.path,p.name);if(!f.endsWith('.log')){bytes+=(await fs.stat(f)).size;files++;}}
  return{workflows:workflows.length,executables:workflows.filter(w=>!w.qualification105).length,supervisors:workflows.filter(w=>w.qualification105).length,goals:goals.length,files,bytes,workflowStore:{bytes:b.length,sha256:sha(b)},limits:{workflows:20,goals:64,procedures:32,supervisors:16,versions:8,awakeSeconds:1200}};
 }
 async function checkedCopy(source,destination){
  assert(!sync.existsSync(destination),'Preserve earlier copies');const start=Date.now();await fs.cp(source,destination,{recursive:true});const files=[];
  for(const p of await fs.readdir(source,{recursive:true,withFileTypes:true}))if(p.isFile()){const full=path.join(p.parentPath||p.path,p.name),relative=path.relative(source,full),a=await fs.readFile(full),b=await fs.readFile(path.join(destination,relative));assert.deepEqual(b,a);files.push({path:relative.replaceAll('\\','/'),bytes:a.length,sha256:sha(a)});}
  record.copies.push({source,destination,start,end:Date.now(),files});await save();
 }
 async function checkpoint(n){
  const dest=path.join(out,'bank-'+n),start=Date.now(),bank=(await api('/api/state')).procedures,counts=await inventory();assert.equal(bank.length,n);const from=project;await close();await checkedCopy(from,dest);record.checkpoints.push({bank:n,project:dest,procedures:bank,counts,start,end:Date.now()});await save();return dest;
 }
 try{
  record.copies=[];const seed=path.join(out,'seed');await open(seed);
  for(let n=1;n<=15;n++){
   const source=await imported('Genuine bank growth '+n,`t,x\n0,${n*.001}\n0.125,${-n*.002}\n0.25,${n*.003}\n`);
   await run('growth-'+n,source,{kind:'growth',bank:n-1});
   if([1,8,9,14,15].includes(n)){await checkpoint(n);if(n<15)await open(seed);}
  }
  for(const bank of [1,8,9,14])for(const [rep,modes]of [[1,['recheck','refresh']],[2,['refresh','recheck']]])for(const kind of modes){
   const name=`bank-${bank}-pair-${rep}-${kind}`,destination=path.join(out,name);await checkedCopy(path.join(out,'bank-'+bank),destination);await open(destination);
   const source=await imported('Paired maintenance selected source','t,x\n0,0.01\n0.125,-0.02\n0.25,0.03\n');
   await run(name,source,{kind,bank,rep,expected:bank===14&&kind==='refresh'?'interrupted':'completed',prompt:`${kind==='refresh'?'Refresh qualification for':'Qualify'} the input-driven summary, analyze selected data and save workflow as paired109.`});await close();
  }
  {
   const name='near-capacity-restart',destination=path.join(out,name);await checkedCopy(path.join(out,'bank-9'),destination);await open(destination);
   const source=await imported(name,'t,x\n0,0\n0.125,-0.004\n0.251,0.007\n');await run(name,source,{kind:'refresh',bank:9,restart:3,prompt:'Refresh qualification for the input-driven summary, analyze selected data and save workflow as recovered109.'});
   await app.evaluate(({BrowserWindow})=>BrowserWindow.getAllWindows().find(w=>w.isVisible()).setSize(1100,1100));await capture('maintenance109-1100.png');await close();
  }
  for(const kind of ['recheck','refresh']){
   const name='full-bank-'+kind,destination=path.join(out,name);await checkedCopy(path.join(out,'bank-15'),destination);await open(destination);const source=await imported(name,'t,x\n0,0\n0.125,0.002\n');await run(name,source,{kind,bank:15,expected:'interrupted',prompt:`${kind==='refresh'?'Refresh qualification for':'Qualify'} the input-driven summary and save workflow as refused109.`});await close();
  }
  for(const type of ['artifact','dataset']){
   const destination=path.join(out,'refused-'+type);await checkedCopy(path.join(out,'bank-14'),destination);await open(destination);const first=(await api('/api/workflows/list',{})).workflows.find(w=>w.qualificationCase105?.name==='monotonic');assert(first);
   if(type==='artifact'){const before=await api('/api/artifact/read',{id:first.report.id});const after=await api('/api/artifact/write',{id:before.id,expectedVersion:before.version,title:'Deliberately altered original qualification',files:{'index.html':'<!doctype html><html><body>Altered original proof control109</body></html>'}});record.alteredOriginal={before,after,workflowId:first.id,project:destination};}
   if(type==='dataset'){const before=await api('/api/measurement/read',{id:first.dataBinding.datasetId,version:1});const args={id:before.datasetId,expectedVersion:1};for(const key of ['title','filename','csv','sourceKind','sourceNote','missingPolicy','mapping'])args[key]=before[key];const after=await api('/api/measurement/import',args);record.alteredDataset={before,after,workflowId:first.id,project:destination};}
   const source=await imported('Fresh source refusal109','t,x\n0,0\n0.125,0.002\n');await run('refused-'+type,source,{kind:'source-refusal',bank:14,expected:'interrupted',prompt:'Qualify the input-driven summary and save workflow as refused109.'});await close();
  }
  record.passed=true;
 }catch(e){record.error=e.stack;if(app)await capture('unexpected109.png').catch(()=>{});throw e;}
 finally{await close();record.finishedAt=new Date().toISOString();await save();require('../../scripts/Study-Build095.cjs').verify(repo);}
}
main().catch(e=>{console.error(e);process.exitCode=1;});
