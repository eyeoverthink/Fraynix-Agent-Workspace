'use strict';
// Isolated, actual Electron/Java qualification. Browser checks are observed, never supplied as passing fixtures.
const {launch}=require('./calibration-integration.cjs'),fs=require('node:fs/promises'),sync=require('node:fs'),path=require('node:path'),assert=require('node:assert/strict'),cp=require('node:child_process');
const {partition}=require('../../scripts/WorkflowCost102.cjs');
const repo=path.resolve(__dirname,'../..'),out=path.resolve(process.env.EDITION106_OUTPUT||path.join(repo,'build/numerical106-final')),sleep=ms=>new Promise(r=>setTimeout(r,ms));
const sequence=(n,fn,dt=.1)=>'time_s,displacement_m\n'+Array.from({length:n},(_,i)=>(i*dt).toFixed(8)+','+fn(i).toFixed(8)).join('\n')+'\n';
async function main(){
 Object.assign(process.env,{FRAYNIX_WORKSPACE_PERSISTENT:'0',FRAYNIX_WORKSPACE_PLAN_CPU_ONLY:'1',FRAYNIX_WORKSPACE_JAVA_HEAP_MIB:'512',FRAYNIX_WORKSPACE_JAVA_PROCESSORS:'2'});
 require('../../scripts/Study-Build095.cjs').verify(repo);assert(!sync.existsSync(out),'Preserve original trials; choose a fresh output directory');await fs.mkdir(out,{recursive:true});
 const cases=JSON.parse(await fs.readFile(path.join(repo,'workspace-engine/src/test/resources/numerical106/cases.json'),'utf8')).cases;
 const record={source:cp.execFileSync('git',['rev-parse','HEAD'],{cwd:repo,encoding:'utf8'}).trim(),startedAt:new Date().toISOString(),tasks:[],invalidInputs:[],checkpoints:[],hostSpans:[],injections:[],captures:[],sessions:[],scope:'Actual Java/Electron qualification preparation, fresh numerical edge cases through retained implementation reuse, authentic native admission refusals and cold restart. Original verifiers unchanged. Declared synthetic inputs, not real footage; no model-weight training. All caller/refusal costs retained.'};
 let app,page,project,session=0;const save=()=>fs.writeFile(path.join(out,'result.json'),JSON.stringify(record,null,2));
 const api=(route,args={})=>app.evaluate((_,x)=>global.workspaceTest.api(x.route,x.args),{route,args:route==='/api/state'?undefined:args});
 const request=(operation,command,args={})=>page.evaluate(x=>window.workspace.request(x),{operation,command,args});
 async function open(p){project=p;await fs.mkdir(project,{recursive:true});const start=Date.now();({app,page}=await launch(project));session++;
  await app.evaluate(({BrowserWindow},session)=>{
   const win=BrowserWindow.getAllWindows().find(w=>w.isVisible());win.setSize(1600,1100);win.setTitle('Fraynix · edition 106 · automatic qualification');
   global.numerical106={spans:[],injections:[],corrupt:false};const Surface=global.workspaceTest.surfaceClass102;
   for(const method of ['test','isolated']){const original=Surface.prototype[method];Surface.prototype[method]=async function(...args){if(this.kind!=='canvas')return original.apply(this,args);const start=Date.now(),clock=process.hrtime.bigint();let ok=false;try{
    if(method==='isolated'&&global.numerical106.corrupt&&String(args[0]).includes("one('table#data-table')")){global.numerical106.corrupt=false;const mutation=await original.call(this,"(()=>{const e=document.querySelector('#summary-travel');if(!e)throw Error('No fault target');const before=e.textContent;e.textContent='999.000000';return{before,after:e.textContent}})()");global.numerical106.injections.push({at:Date.now(),session,...mutation,scope:'Deliberately altered a displayed qualification value after behavior checks. Sources and authored code remain unchanged.'});}
    const v=await original.apply(this,args);ok=true;return v;
   }finally{global.numerical106.spans.push({kind:method==='test'?'browser-test':'browser-observation',session,start,end:Date.now(),nanos:String(process.hrtime.bigint()-clock),ok});}};}
  },session);
  await page.waitForFunction(()=>window.inputs100&&[...document.querySelector('#model').options].some(o=>o.value==='gpt-oss:20b'),{},{timeout:65000});await page.locator('#model').selectOption('gpt-oss:20b');record.sessions.push({session,project,start,ready:Date.now()});await save();
 }
 async function flush(){if(!app)return;const x=await app.evaluate(()=>{const s=global.numerical106,v={spans:s.spans,injections:s.injections};s.spans=[];s.injections=[];return v;});record.hostSpans.push(...x.spans);record.injections.push(...x.injections);await save();}
 async function close(){if(!app)return;await flush();await app.close();app=null;}
 async function capture(name){await sleep(250);await fs.writeFile(path.join(out,name),Buffer.from(await app.evaluate(()=>global.workspaceTest.capture()),'base64'));record.captures.push({name,project,at:Date.now()});}
 async function imported(name,csv,{measured=false,mapping={},missingPolicy='reject'}={}){const source=await request('data','import',{title:name,filename:name+'.csv',csv,sourceKind:measured?'measured':'synthetic',sourceNote:measured?'Unchanged Delfys013 recording, not a new physical measurement.':'Declared arithmetic106 software test, not footage or physical measurement.',missingPolicy,mapping:{timeColumn:'t',distanceColumn:'x',timeUnit:'s',distanceUnit:'m',...mapping}});await request('data','select',{id:source.datasetId,version:source.version});return api('/api/measurement/read',{id:source.datasetId,version:source.version});}
 async function readWorkflow(task){const id=task.completion044?.javaWorkflow?.id||task.context044?.ownedWorkflowId;return id?api('/api/workflows/read',{id}):null;}
 async function collectChild(id){const workflow=await api('/api/workflows/read',{id}),children=[],artifacts=[];for(const s of workflow.stages){if(!s.goalId)continue;const g=await api('/api/goals/read',{id:s.goalId});children.push(g);if(g.report)artifacts.push(await api('/api/artifact/read',{id:g.report.id}));}const b=workflow.dataBinding;return{workflow,children,artifacts,source:await api('/api/measurement/read',{id:b.datasetId,version:b.datasetVersion})};}
 async function run(name,source,{kind='numeric',bank=1,expected='completed',restart=false,fault=false,prompt:givenPrompt,case106,stop=true}={}){
  const prompt=givenPrompt||`Qualify the input-driven summary, analyze the selected data, and save workflow as ${name}.`,row={name,kind,bank,source,prompt,case106,expected,project,captureKey:name,bankBefore:(await api('/api/state')).procedures};record.tasks.push(row);
  if(fault)await app.evaluate(()=>global.numerical106.corrupt=true);const previous=new Set((await api('/api/agency043/state')).tasks.map(t=>t.id));
  await page.waitForFunction(()=>!document.querySelector('#run').disabled,{},{timeout:30000});row.start=Date.now();await save();await page.locator('#goal').fill(prompt);await page.locator('#run').click();
  for(let i=0;i<150;i++){const t=(await api('/api/agency043/state')).tasks.find(t=>!previous.has(t.id)&&t.goal===prompt);if(t){row.id=t.id;break;}await sleep(200);}assert(row.id,'Chat request not admitted');
  let last='',crashed=false;const deadline=Date.now()+1_200_000;
  while(Date.now()<deadline){row.task=await api('/api/agency043/read',{id:row.id});const w=await readWorkflow(row.task),status=row.task.status+' '+(w?w.step+' '+w.qualification105?.checked+'/5':row.task.reason);if(status!==last){console.log(name+': '+status);last=status;}
   if(restart&&!crashed&&row.task.status==='interrupted'&&w?.stages[0].status==='completed'){
    row.boundary=w;row.pauseStart=Date.now();row.restart=true;await capture(name+'-interrupted.png');await flush();await app.evaluate(()=>global.workspaceTest.crashEngine());await sleep(300);await app.close().catch(()=>{});app=null;await open(project);row.restored=await api('/api/workflows/read',{id:w.id});assert.equal(row.restored.status,'interrupted');assert.equal(row.restored.stages[0].goalId,w.stages[0].goalId);assert.deepEqual(row.restored.implementation103,w.implementation103);
    await page.locator('#mission044-open').click();await page.locator('#mission044-task').selectOption(row.id);await page.waitForFunction(()=>!document.querySelector('#mission044-resume').disabled,{},{timeout:30000});await page.locator('#mission044-resume').click();for(let i=0;i<100;i++){if((await api('/api/agency043/read',{id:row.id})).resumes>0)break;await sleep(100);}row.pauseEnd=Date.now();crashed=true;continue;
   }
   if(['completed','interrupted','cancelled'].includes(row.task.status)){assert.equal(row.task.status,expected,'Unexpected task result: '+row.task.reason);row.end=Date.now();row.workflow=w;break;}await sleep(250);
  }
  assert(row.end,'Bounded caller exceeded 1200s');if(restart)assert(crashed);row.children=[];
  if(row.workflow){if(row.workflow.qualification105){for(const s of row.workflow.stages)if(s.workflowId)row.children.push(await collectChild(s.workflowId));}else row.children.push(await collectChild(row.workflow.id));}
  row.bankAfter=(await api('/api/state')).procedures;row.allWorkflows=(await api('/api/workflows/list',{})).workflows;row.nativeGoals=(await api('/api/goals/list',{})).goals;
  let jobs=[];try{jobs=JSON.parse(await fs.readFile(path.join(project,'creation-044/requests.json'),'utf8')).data.jobs;}catch(e){if(e.code!=='ENOENT')throw e;}row.generation=jobs;assert.equal(jobs.length,0);await flush();
  const spans=record.hostSpans.filter(s=>s.start>=row.start&&s.end<=row.end).map(s=>({...s,kind:'browser-verification'}));if(row.restart)spans.push({kind:'pause-restart',start:row.pauseStart,end:row.pauseEnd});row.cost=partition(row.start,row.end,spans);
  await page.locator('#mission044-open').click();await page.locator('#mission044-task').selectOption(row.id);
  if(expected==='completed'){
   assert(row.workflow.audit.passed);assert.equal(row.bankAfter.length,row.bankBefore.length+(kind==='preparation'?1:0));if(kind==='preparation'){assert.equal(row.workflow.qualification105.checked,5);assert(row.bankAfter.some(p=>p.id===row.workflow.savedSkill.id));}else{assert(row.workflow.implementation103);assert(!row.workflow.savedSkill);}await request('agency043','mission-open',{id:row.id});
  }else{
   assert.equal(row.bankAfter.length,row.bankBefore.length);if(fault){assert(row.workflow);assert(!row.workflow.savedSkill);await assert.rejects(()=>api('/api/workflows/resume',{id:row.workflow.id}));row.resumeRefused=true;assert(await page.locator('#mission044-resume').isDisabled());}
  }
  if(row.task.refusal106){assert.equal(row.task.reason,'Created artifact was changed outside its owned revision');assert.equal(row.task.timing052.modelCalls,0);assert.equal(row.task.failures,0);assert(!row.workflow);await assert.rejects(()=>request('agency043','control',{id:row.id,action:'resume'}));assert(await page.locator('#mission044-resume').isDisabled());row.resumeRefused=true;}
  await capture(row.captureKey+'-1600.png');await save();if(expected!=='completed'&&stop){await request('agency043','control',{id:row.id,action:'cancel'});await sleep(500);}return row;
 }
 async function checkpoint(n){const dest=path.join(out,'bank-'+n),start=Date.now();const bank=(await api('/api/state')).procedures;assert.equal(bank.length,n);await close();assert(!sync.existsSync(dest));await fs.cp(project,dest,{recursive:true});record.checkpoints.push({bank:n,project:dest,procedures:bank,start,end:Date.now()});await save();return dest;}
 try{
  const seed=path.join(out,'seed');await open(seed);const selected=await imported('Qualification preparation106','t,x\n0,0\n0.1,0.01\n0.2,-0.02\n');
  await run('qualified106-seed',selected,{kind:'preparation',bank:0});const frozen=await checkpoint(1);
  for(const c of cases){
   const destination=path.join(out,c.name);await fs.cp(frozen,destination,{recursive:true});await open(destination);
   const source=await imported(c.name,c.csv,{mapping:c.mapping});
   const number=n=>Number(n).toLocaleString('en-US',{useGrouping:false,maximumFractionDigits:20});
   const restart=c.name==='negative-reversal',prompt=`Reuse qualified106-seed version 1 on the selected dataset. Gap limit ${number(c.maxGapS)} seconds and speed limit ${number(c.speedLimitMps)} m/s.${restart?' Review after data.':''}`;
   await run(c.name,source,{case106:c,prompt,restart});
   if(c.name==='mapped-inversion'){await app.evaluate(({BrowserWindow})=>BrowserWindow.getAllWindows().find(w=>w.isVisible()).setSize(1100,1100));await capture('numerical106-1100.png');}
   await close();
  }
  const destination=path.join(out,'invalid-numeric');await fs.cp(frozen,destination,{recursive:true});await open(destination);
  for(const [name,csv]of [['duplicate-time','t,x\n0,0\n0,1\n'],['nan','t,x\n0,0\n1,NaN\n'],['infinite','t,x\n0,0\n1,Infinity\n'],['time-range','t,x\n0,0\n86401,1\n'],['distance-range','t,x\n0,0\n1,1000001\n']]){
   const row={name,csv,start:Date.now(),before:(await api('/api/state')).datasets.length};try{await imported(name,csv);throw Error('Invalid input was accepted');}catch(e){row.reason=e.message;assert(!row.reason.includes('was accepted'));}row.end=Date.now();row.after=(await api('/api/state')).datasets.length;assert.equal(row.before,row.after);record.invalidInputs.push(row);await save();
  }
  await run('derived-overflow',await imported('derived-overflow','t,x\n0,0\n5e-324,1\n'),{kind:'derived-refusal',expected:'interrupted',prompt:'Reuse qualified106-seed version 1 on the selected dataset.'});await close();
  const altered=path.join(out,'altered-original');await fs.cp(frozen,altered,{recursive:true});await open(altered);
  const first=(await api('/api/workflows/list',{})).workflows.find(w=>w.qualificationCase105?.name==='monotonic');assert(first);const before=await api('/api/artifact/read',{id:first.report.id});
  const mutation=await api('/api/artifact/write',{id:before.id,expectedVersion:before.version,title:'Deliberately altered retained qualification',files:{'index.html':'<!doctype html><html><body>Altered retained proof control106</body></html>'}});record.alteredOriginal={before,after:mutation,workflowId:first.id,project:altered};
  const fresh=await imported('Fresh refusal106','t,x\n0,0\n0.125,0.002\n');
  const refused=await run('altered-proof106',fresh,{kind:'source-refusal',expected:'interrupted'});
  // run() stops the refused request after retaining its terminal original. The next
  // independent request exercises saved-procedure scheduling against the same revision.
  const reuse=await run('altered-reuse106',fresh,{kind:'reuse-refusal',expected:'interrupted',stop:false,prompt:'Reuse qualified106-seed version 1 on the selected dataset.'});
  await close();const coldStart=Date.now();await open(altered);record.coldRefusal={start:coldStart,end:Date.now(),task:await api('/api/agency043/read',{id:reuse.id})};
  assert.equal(record.coldRefusal.task.status,'interrupted');assert.equal(record.coldRefusal.task.refusal106.reason,reuse.task.reason);assert.deepEqual(record.coldRefusal.task.refusal106,reuse.task.refusal106);assert.equal(record.coldRefusal.task.timing052.modelCalls,0);await assert.rejects(()=>request('agency043','control',{id:reuse.id,action:'resume'}));
  await page.locator('#mission044-open').click();await page.locator('#mission044-task').selectOption(reuse.id);assert(await page.locator('#mission044-resume').isDisabled());await capture('refusal106-cold.png');await request('agency043','control',{id:reuse.id,action:'cancel'});await close();record.passed=true;
 }catch(e){record.error=e.stack;if(app)await capture('unexpected106.png').catch(()=>{});throw e;}
 finally{await close();record.finishedAt=new Date().toISOString();await save();require('../../scripts/Study-Build095.cjs').verify(repo);}
}
main().catch(e=>{console.error(e);process.exitCode=1;});
