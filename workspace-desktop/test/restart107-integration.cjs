'use strict';
// Isolated, actual Electron/Java qualification. Browser checks are observed, never supplied as passing fixtures.
const {launch}=require('./calibration-integration.cjs'),fs=require('node:fs/promises'),sync=require('node:fs'),path=require('node:path'),assert=require('node:assert/strict'),cp=require('node:child_process');
const {partition}=require('../../scripts/WorkflowCost102.cjs');
const repo=path.resolve(__dirname,'../..'),out=path.resolve(process.env.EDITION107_OUTPUT||path.join(repo,'build/restart107')),sleep=ms=>new Promise(r=>setTimeout(r,ms));
const sequence=(n,fn,dt=.1)=>'time_s,displacement_m\n'+Array.from({length:n},(_,i)=>(i*dt).toFixed(8)+','+fn(i).toFixed(8)).join('\n')+'\n';
async function main(){
 Object.assign(process.env,{FRAYNIX_WORKSPACE_PERSISTENT:'0',FRAYNIX_WORKSPACE_PLAN_CPU_ONLY:'1',FRAYNIX_WORKSPACE_JAVA_HEAP_MIB:'512',FRAYNIX_WORKSPACE_JAVA_PROCESSORS:'2'});
 require('../../scripts/Study-Build095.cjs').verify(repo);assert(!sync.existsSync(out),'Preserve original trials; choose a fresh output directory');await fs.mkdir(out,{recursive:true});
 const cases=JSON.parse(await fs.readFile(path.join(repo,'workspace-engine/src/test/resources/restart107/cases.json'),'utf8')).cases;
 const record={source:cp.execFileSync('git',['rev-parse','HEAD'],{cwd:repo,encoding:'utf8'}).trim(),startedAt:new Date().toISOString(),tasks:[],invalidInputs:[],checkpoints:[],hostSpans:[],injections:[],captures:[],sessions:[],scope:'Actual Java/Electron qualification preparation, fresh numerical edge cases through retained implementation reuse, authentic native admission refusals and cold restart. Original verifiers unchanged. Declared synthetic inputs, not real footage; no model-weight training. All caller/refusal costs retained.'};
 let app,page,project,session=0;const save=()=>fs.writeFile(path.join(out,'result.json'),JSON.stringify(record,null,2));
 const api=(route,args={})=>app.evaluate((_,x)=>global.workspaceTest.api(x.route,x.args),{route,args:route==='/api/state'?undefined:args});
 const request=(operation,command,args={})=>page.evaluate(x=>window.workspace.request(x),{operation,command,args});
 async function open(p){project=p;await fs.mkdir(project,{recursive:true});const start=Date.now();({app,page}=await launch(project));session++;
  await app.evaluate(({BrowserWindow},session)=>{
   const win=BrowserWindow.getAllWindows().find(w=>w.isVisible());win.setSize(1600,1100);win.setTitle('Fraynix · edition 107 · automatic qualification');
   global.numerical107={spans:[],injections:[],corrupt:false,gate:'',blocked:null};const Surface=global.workspaceTest.surfaceClass102;
   for(const method of ['test','isolated']){const original=Surface.prototype[method];Surface.prototype[method]=async function(...args){if(this.kind!=='canvas')return original.apply(this,args);const state=global.numerical107;if((state.gate==='behavior'&&method==='test'&&JSON.stringify(args[0]).includes('#summary-'))||(state.gate==='audit'&&method==='isolated'&&String(args[0]).includes("one('table#data-table')"))){state.blocked={at:Date.now(),method,boundary:state.gate,scope:'Actual browser operation held before execution, then Java process terminated. No passing observation supplied.'};state.gate='';await new Promise(()=>{});}const start=Date.now(),clock=process.hrtime.bigint();let ok=false;try{
    if(method==='isolated'&&global.numerical107.corrupt&&String(args[0]).includes("one('table#data-table')")){global.numerical107.corrupt=false;const mutation=await original.call(this,"(()=>{const e=document.querySelector('#summary-travel');if(!e)throw Error('No fault target');const before=e.textContent;e.textContent='999.000000';return{before,after:e.textContent}})()");global.numerical107.injections.push({at:Date.now(),session,...mutation,scope:'Deliberately altered a displayed qualification value after behavior checks. Sources and authored code remain unchanged.'});}
    const v=await original.apply(this,args);ok=true;return v;
   }finally{global.numerical107.spans.push({kind:method==='test'?'browser-test':'browser-observation',session,start,end:Date.now(),nanos:String(process.hrtime.bigint()-clock),ok});}};}
  },session);
  await page.waitForFunction(()=>window.inputs100&&[...document.querySelector('#model').options].some(o=>o.value==='gpt-oss:20b'),{},{timeout:65000});await page.locator('#model').selectOption('gpt-oss:20b');record.sessions.push({session,project,start,ready:Date.now()});await save();
 }
 async function flush(){if(!app)return;const x=await app.evaluate(()=>{const s=global.numerical107,v={spans:s.spans,injections:s.injections};s.spans=[];s.injections=[];return v;});record.hostSpans.push(...x.spans);record.injections.push(...x.injections);await save();}
 async function close(){if(!app)return;await flush();await app.close();app=null;}
 async function capture(name){await sleep(250);await fs.writeFile(path.join(out,name),Buffer.from(await app.evaluate(()=>global.workspaceTest.capture()),'base64'));record.captures.push({name,project,at:Date.now()});}
 async function imported(name,csv,{measured=false,mapping={},missingPolicy='reject'}={}){const source=await request('data','import',{title:name,filename:name+'.csv',csv,sourceKind:measured?'measured':'synthetic',sourceNote:measured?'Unchanged Delfys013 recording, not a new physical measurement.':'Declared arithmetic107 software test, not footage or physical measurement.',missingPolicy,mapping:{timeColumn:'t',distanceColumn:'x',timeUnit:'s',distanceUnit:'m',...mapping}});await request('data','select',{id:source.datasetId,version:source.version});return api('/api/measurement/read',{id:source.datasetId,version:source.version});}
 async function readWorkflow(task){const id=task.completion044?.javaWorkflow?.id||task.context044?.ownedWorkflowId;return id?api('/api/workflows/read',{id}):null;}
 async function collectChild(id){const workflow=await api('/api/workflows/read',{id}),children=[],artifacts=[];for(const s of workflow.stages){if(!s.goalId)continue;const g=await api('/api/goals/read',{id:s.goalId});children.push(g);if(g.report)artifacts.push(await api('/api/artifact/read',{id:g.report.id}));}const b=workflow.dataBinding;return{workflow,children,artifacts,source:await api('/api/measurement/read',{id:b.datasetId,version:b.datasetVersion})};}
 async function run(name,source,{kind='numeric',bank=1,expected='completed',restart=false,fault=false,prompt:givenPrompt,case107,stop=true}={}){
  const prompt=givenPrompt||`Qualify the input-driven summary, analyze the selected data, and save workflow as ${name}.`,row={name,kind,bank,source,prompt,case107,expected,project,captureKey:name,bankBefore:(await api('/api/state')).procedures};record.tasks.push(row);
  if(restart)await app.evaluate((_,gate)=>global.numerical107.gate=gate,restart);if(fault)await app.evaluate(()=>global.numerical107.corrupt=true);const previous=new Set((await api('/api/agency043/state')).tasks.map(t=>t.id));
  await page.waitForFunction(()=>!document.querySelector('#run').disabled,{},{timeout:30000});row.start=Date.now();await save();await page.locator('#goal').fill(prompt);await page.locator('#run').click();
  for(let i=0;i<150;i++){const t=(await api('/api/agency043/state')).tasks.find(t=>!previous.has(t.id)&&t.goal===prompt);if(t){row.id=t.id;break;}await sleep(200);}assert(row.id,'Chat request not admitted');
  let last='',crashed=false;const deadline=Date.now()+1_200_000;
  while(Date.now()<deadline){row.task=await api('/api/agency043/read',{id:row.id});const w=await readWorkflow(row.task),status=row.task.status+' '+(w?w.step+' '+w.qualification105?.checked+'/5':row.task.reason);if(status!==last){console.log(name+': '+status);last=status;}
   const blocked=restart&&!crashed?await app.evaluate(()=>global.numerical107.blocked):null;
   if(blocked){
    assert(w);row.boundary=w;row.boundaryOriginal=await collectChild(w.id);assert.equal(row.boundaryOriginal.artifacts.length,2);row.heldOperation=blocked;row.pauseStart=blocked.at;row.restart=true;await capture(name+'-interrupted.png');await flush();row.crashReceipt=await app.evaluate(()=>global.workspaceTest.crashEngine());await sleep(300);await app.close().catch(()=>{});app=null;await open(project);row.restored=await api('/api/workflows/read',{id:w.id});row.restoredOriginal=await collectChild(w.id);assert.equal(row.restored.status,'interrupted');assert.deepEqual(row.restored.implementation103,w.implementation103);for(let i=0;i<2;i++){assert.equal(row.restored.stages[i].goalId,w.stages[i].goalId);assert.equal(row.restoredOriginal.artifacts[i].id,row.boundaryOriginal.artifacts[i].id);assert.equal(row.restoredOriginal.artifacts[i].version,row.boundaryOriginal.artifacts[i].version);assert.deepEqual(row.restoredOriginal.artifacts[i].contents,row.boundaryOriginal.artifacts[i].contents);}
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
   assert(row.workflow.audit.passed);assert.equal(row.bankAfter.length,row.bankBefore.length+(['preparation','recheck','refresh'].includes(kind)?1:0));if(['preparation','recheck','refresh'].includes(kind)){assert.equal(row.workflow.qualification105.checked,5);assert(row.bankAfter.some(p=>p.id===row.workflow.savedSkill.id));}else{assert(row.workflow.implementation103);assert(!row.workflow.savedSkill);}await request('agency043','mission-open',{id:row.id});
  }else{
   assert.equal(row.bankAfter.length,row.bankBefore.length);if(fault){assert(row.workflow);assert(!row.workflow.savedSkill);await assert.rejects(()=>api('/api/workflows/resume',{id:row.workflow.id}));row.resumeRefused=true;assert(await page.locator('#mission044-resume').isDisabled());}
  }
  if(row.task.refusal106){assert(['Created artifact was changed outside its owned revision','Original qualification dataset was revised'].includes(row.task.reason));assert.equal(row.task.timing052.modelCalls,0);assert.equal(row.task.failures,0);assert(!row.workflow);await assert.rejects(()=>request('agency043','control',{id:row.id,action:'resume'}));assert(await page.locator('#mission044-resume').isDisabled());row.resumeRefused=true;}
  await capture(row.captureKey+'-1600.png');await save();if(expected!=='completed'&&stop){await request('agency043','control',{id:row.id,action:'cancel'});await sleep(500);}return row;
 }
 async function checkpoint(n){const dest=path.join(out,'bank-'+n),start=Date.now();const bank=(await api('/api/state')).procedures;assert.equal(bank.length,n);await close();assert(!sync.existsSync(dest));await fs.cp(project,dest,{recursive:true});record.checkpoints.push({bank:n,project:dest,procedures:bank,start,end:Date.now()});await save();return dest;}
 try{
  const seed=path.join(out,'seed');await open(seed);const selected=await imported('Qualification preparation107','t,x\n0,0\n0.1,0.01\n0.2,-0.02\n');
  await run('qualified107-seed',selected,{kind:'preparation',bank:0});const frozen=await checkpoint(1);
  for(const [rep,modes]of [['1',['recheck','refresh']],['2',['refresh','recheck']]])for(const mode of modes){
   const name='pair-'+rep+'-'+mode,destination=path.join(out,name);await fs.cp(frozen,destination,{recursive:true});await open(destination);const source=await imported('Paired selected source','t,x\n0,0.01\n0.125,-0.02\n0.25,0.03\n');
   await run(name,source,{kind:mode,prompt:`${mode==='refresh'?'Refresh qualification for':'Qualify'} the input-driven summary, analyze selected data and save workflow as paired107.`});await close();
  }
  for(const [i,c]of cases.entries()){
   const destination=path.join(out,c.name);await fs.cp(frozen,destination,{recursive:true});await open(destination);const source=await imported(c.name,c.csv,{mapping:c.mapping,missingPolicy:c.missingPolicy||'reject'});
   const number=n=>Number(n).toLocaleString('en-US',{useGrouping:false,maximumFractionDigits:20});
   const prompt=`Reuse qualified107-seed version 1 on the selected dataset. Gap limit ${number(c.maxGapS)} seconds and speed limit ${number(c.speedLimitMps)} m/s.`;
   await run(c.name,source,{case107:c,prompt,restart:i%2?'audit':'behavior'});
   if(c.name==='mapped-inversion'){await app.evaluate(({BrowserWindow})=>BrowserWindow.getAllWindows().find(w=>w.isVisible()).setSize(1100,1100));await capture('restart107-1100.png');}await close();
  }
  for(const type of ['artifact','dataset']){
   const altered=path.join(out,'altered-'+type);await fs.cp(frozen,altered,{recursive:true});await open(altered);const first=(await api('/api/workflows/list',{})).workflows.find(w=>w.qualificationCase105?.name==='monotonic');assert(first);
   if(type==='artifact'){const before=await api('/api/artifact/read',{id:first.report.id});const after=await api('/api/artifact/write',{id:before.id,expectedVersion:before.version,title:'Deliberately altered retained qualification',files:{'index.html':'<!doctype html><html><body>Altered retained proof control107</body></html>'}});record.alteredOriginal={before,after,workflowId:first.id,project:altered};}
   else{const before=await api('/api/measurement/read',{id:first.dataBinding.datasetId,version:1});const args={id:before.datasetId,expectedVersion:1};for(const key of ['title','filename','csv','sourceKind','sourceNote','missingPolicy','mapping'])args[key]=before[key];const after=await api('/api/measurement/import',args);record.alteredDataset={before,after,workflowId:first.id,project:altered};}
   const fresh=await imported('Fresh refusal107','t,x\n0,0\n0.125,0.002\n');await run('altered-'+type+'107',fresh,{kind:'source-refusal',expected:'interrupted',prompt:'Refresh qualification for the input-driven summary and save workflow as refused107.'});await close();
  }
  record.passed=true;
 }catch(e){record.error=e.stack;if(app)await capture('unexpected107.png').catch(()=>{});throw e;}
 finally{await close();record.finishedAt=new Date().toISOString();await save();require('../../scripts/Study-Build095.cjs').verify(repo);}
}
main().catch(e=>{console.error(e);process.exitCode=1;});
