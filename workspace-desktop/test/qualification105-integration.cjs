'use strict';
// Isolated, actual Electron/Java qualification. Browser checks are observed, never supplied as passing fixtures.
const {launch}=require('./calibration-integration.cjs'),fs=require('node:fs/promises'),sync=require('node:fs'),path=require('node:path'),assert=require('node:assert/strict'),cp=require('node:child_process');
const {partition}=require('../../scripts/WorkflowCost102.cjs');
const repo=path.resolve(__dirname,'../..'),out=path.resolve(process.env.EDITION105_OUTPUT||path.join(repo,'build/qualification105')),sleep=ms=>new Promise(r=>setTimeout(r,ms));
const sequence=(n,fn,dt=.1)=>'time_s,displacement_m\n'+Array.from({length:n},(_,i)=>(i*dt).toFixed(8)+','+fn(i).toFixed(8)).join('\n')+'\n';
async function main(){
 Object.assign(process.env,{FRAYNIX_WORKSPACE_PERSISTENT:'0',FRAYNIX_WORKSPACE_PLAN_CPU_ONLY:'1',FRAYNIX_WORKSPACE_JAVA_HEAP_MIB:'512',FRAYNIX_WORKSPACE_JAVA_PROCESSORS:'2'});
 require('../../scripts/Study-Build095.cjs').verify(repo);assert(!sync.existsSync(out),'Preserve original trials; choose a fresh output directory');await fs.mkdir(out,{recursive:true});
 const record={source:cp.execFileSync('git',['rev-parse','HEAD'],{cwd:repo,encoding:'utf8'}).trim(),startedAt:new Date().toISOString(),tasks:[],checkpoints:[],hostSpans:[],injections:[],captures:[],sessions:[],scope:'Actual single chat requests; Java-authored five-case software qualification then fresh selected-source verification and original104 skill publication. Real browser observations, no content-generation calls. Preparation and refusals retained. Small sequential collection-growth study, not causal/universal speedup or weight training.'};
 let app,page,project,session=0;const save=()=>fs.writeFile(path.join(out,'result.json'),JSON.stringify(record,null,2));
 const api=(route,args={})=>app.evaluate((_,x)=>global.workspaceTest.api(x.route,x.args),{route,args:route==='/api/state'?undefined:args});
 const request=(operation,command,args={})=>page.evaluate(x=>window.workspace.request(x),{operation,command,args});
 async function open(p){project=p;await fs.mkdir(project,{recursive:true});const start=Date.now();({app,page}=await launch(project));session++;
  await app.evaluate(({BrowserWindow},session)=>{
   const win=BrowserWindow.getAllWindows().find(w=>w.isVisible());win.setSize(1600,1100);win.setTitle('Fraynix · edition 105 · automatic qualification');
   global.qualification105={spans:[],injections:[],corrupt:false};const Surface=global.workspaceTest.surfaceClass102;
   for(const method of ['test','isolated']){const original=Surface.prototype[method];Surface.prototype[method]=async function(...args){if(this.kind!=='canvas')return original.apply(this,args);const start=Date.now(),clock=process.hrtime.bigint();let ok=false;try{
    if(method==='isolated'&&global.qualification105.corrupt&&String(args[0]).includes("one('table#data-table')")){global.qualification105.corrupt=false;const mutation=await original.call(this,"(()=>{const e=document.querySelector('#summary-travel');if(!e)throw Error('No fault target');const before=e.textContent;e.textContent='999.000000';return{before,after:e.textContent}})()");global.qualification105.injections.push({at:Date.now(),session,...mutation,scope:'Deliberately altered a displayed qualification value after behavior checks. Sources and authored code remain unchanged.'});}
    const v=await original.apply(this,args);ok=true;return v;
   }finally{global.qualification105.spans.push({kind:method==='test'?'browser-test':'browser-observation',session,start,end:Date.now(),nanos:String(process.hrtime.bigint()-clock),ok});}};}
  },session);
  await page.waitForFunction(()=>window.inputs100&&[...document.querySelector('#model').options].some(o=>o.value==='gpt-oss:20b'),{},{timeout:65000});await page.locator('#model').selectOption('gpt-oss:20b');record.sessions.push({session,project,start,ready:Date.now()});await save();
 }
 async function flush(){if(!app)return;const x=await app.evaluate(()=>{const s=global.qualification105,v={spans:s.spans,injections:s.injections};s.spans=[];s.injections=[];return v;});record.hostSpans.push(...x.spans);record.injections.push(...x.injections);await save();}
 async function close(){if(!app)return;await flush();await app.close();app=null;}
 async function capture(name){await sleep(250);await fs.writeFile(path.join(out,name),Buffer.from(await app.evaluate(()=>global.workspaceTest.capture()),'base64'));record.captures.push({name,project,at:Date.now()});}
 async function imported(name,csv,{measured=false,mapping={},missingPolicy='reject'}={}){const source=await request('data','import',{title:name,filename:name+'.csv',csv,sourceKind:measured?'measured':'synthetic',sourceNote:measured?'Unchanged Delfys013 recording, not a new physical measurement.':'Declared arithmetic105 software test, not footage or physical measurement.',missingPolicy,mapping:{timeColumn:'time_s',distanceColumn:'displacement_m',timeUnit:'s',distanceUnit:'m',...mapping}});await request('data','select',{id:source.datasetId,version:source.version});return api('/api/measurement/read',{id:source.datasetId,version:source.version});}
 async function readWorkflow(task){const id=task.completion044?.javaWorkflow?.id||task.context044?.ownedWorkflowId;return id?api('/api/workflows/read',{id}):null;}
 async function collectChild(id){const workflow=await api('/api/workflows/read',{id}),children=[],artifacts=[];for(const s of workflow.stages){if(!s.goalId)continue;const g=await api('/api/goals/read',{id:s.goalId});children.push(g);if(g.report)artifacts.push(await api('/api/artifact/read',{id:g.report.id}));}const b=workflow.dataBinding;return{workflow,children,artifacts,source:await api('/api/measurement/read',{id:b.datasetId,version:b.datasetVersion})};}
 async function run(name,source,{kind='probe',bank,expected='completed',restart=false,fault=false}={}){
  const prompt=`Qualify the input-driven summary, analyze the selected data, and save workflow as ${name}.`,row={name,kind,bank,source,prompt,expected,project,captureKey:kind==='probe'?path.basename(project):name,bankBefore:(await api('/api/state')).procedures};record.tasks.push(row);
  if(fault)await app.evaluate(()=>global.qualification105.corrupt=true);const previous=new Set((await api('/api/agency043/state')).tasks.map(t=>t.id));
  await page.waitForFunction(()=>!document.querySelector('#run').disabled,{},{timeout:30000});row.start=Date.now();await save();await page.locator('#goal').fill(prompt);await page.locator('#run').click();
  for(let i=0;i<150;i++){const t=(await api('/api/agency043/state')).tasks.find(t=>!previous.has(t.id)&&t.goal===prompt);if(t){row.id=t.id;break;}await sleep(200);}assert(row.id,'Chat request not admitted');
  let last='',crashed=false;const deadline=Date.now()+1_200_000;
  while(Date.now()<deadline){row.task=await api('/api/agency043/read',{id:row.id});const w=await readWorkflow(row.task),status=row.task.status+' '+(w?w.step+' '+w.qualification105?.checked+'/5':row.task.reason);if(status!==last){console.log(name+': '+status);last=status;}
   if(restart&&!crashed&&w?.qualification105?.checked===2&&w.status==='running'){
    row.boundary=w;row.pauseStart=Date.now();row.restart=true;await capture(name+'-interrupted.png');await flush();await app.evaluate(()=>global.workspaceTest.crashEngine());await sleep(300);await app.close().catch(()=>{});app=null;await open(project);row.restored=await api('/api/workflows/read',{id:w.id});assert.equal(row.restored.status,'interrupted');for(let i=0;i<2;i++)assert.equal(row.restored.stages[i].workflowId,w.stages[i].workflowId);
    await page.locator('#mission044-open').click();await page.locator('#mission044-task').selectOption(row.id);await page.waitForFunction(()=>!document.querySelector('#mission044-resume').disabled,{},{timeout:30000});await page.locator('#mission044-resume').click();for(let i=0;i<100;i++){if((await api('/api/agency043/read',{id:row.id})).resumes>0)break;await sleep(100);}row.pauseEnd=Date.now();crashed=true;continue;
   }
   if(['completed','interrupted','cancelled'].includes(row.task.status)){assert.equal(row.task.status,expected,'Unexpected task result: '+row.task.reason);row.end=Date.now();row.workflow=w;break;}await sleep(250);
  }
  assert(row.end,'Bounded caller exceeded 1200s');if(restart)assert(crashed);row.children=[];
  if(row.workflow)for(const s of row.workflow.stages)if(s.workflowId){try{row.children.push(await collectChild(s.workflowId));}catch(e){if(!String(e.message).includes('Unknown workflow'))throw e;}}
  row.bankAfter=(await api('/api/state')).procedures;row.allWorkflows=(await api('/api/workflows/list',{})).workflows;row.nativeGoals=(await api('/api/goals/list',{})).goals;
  let jobs=[];try{jobs=JSON.parse(await fs.readFile(path.join(project,'creation-044/requests.json'),'utf8')).data.jobs;}catch(e){if(e.code!=='ENOENT')throw e;}row.generation=jobs;assert.equal(jobs.length,0);await flush();
  const spans=record.hostSpans.filter(s=>s.start>=row.start&&s.end<=row.end).map(s=>({...s,kind:'browser-verification'}));if(row.restart)spans.push({kind:'pause-restart',start:row.pauseStart,end:row.pauseEnd});row.cost=partition(row.start,row.end,spans);
  await page.locator('#mission044-open').click();await page.locator('#mission044-task').selectOption(row.id);
  if(expected==='completed'){
   assert(row.workflow.audit.passed);assert.equal(row.bankAfter.length,row.bankBefore.length+1);assert.equal(row.workflow.qualification105.checked,5);assert(row.bankAfter.some(p=>p.id===row.workflow.savedSkill.id));await request('agency043','mission-open',{id:row.id});
  }else{
   assert.equal(row.bankAfter.length,row.bankBefore.length);if(fault){assert(row.workflow);assert(!row.workflow.savedSkill);await assert.rejects(()=>api('/api/workflows/resume',{id:row.workflow.id}));row.resumeRefused=true;assert(await page.locator('#mission044-resume').isDisabled());}
  }
  await capture(row.captureKey+'-1600.png');await save();if(expected!=='completed'){await request('agency043','control',{id:row.id,action:'cancel'});await sleep(500);}return row;
 }
 async function checkpoint(n){const dest=path.join(out,'bank-'+n),start=Date.now();const bank=(await api('/api/state')).procedures;assert.equal(bank.length,n);await close();assert(!sync.existsSync(dest));await fs.cp(project,dest,{recursive:true});record.checkpoints.push({bank:n,project:dest,procedures:bank,start,end:Date.now()});await save();return dest;}
 try{
  const seed=path.join(out,'seed');await open(seed);const probe=await imported('Common held-out37',sequence(37,i=>Math.sin(i/5)*.018+(i>20?.005:0),.07));await checkpoint(0);await open(seed);
  for(let i=1;i<=8;i++){
   const source=i===1?await imported('Recorded52 preparation',await fs.readFile(path.join(repo,'docs/datasets/delfys75-013/training.csv'),'utf8'),{measured:true}):await imported('Varied preparation '+i,sequence(21+i*4,j=>(j%13<7?j%13:13-j%13)*.001*i-.005,.025+i*.003));
   await run('qualified105-'+i,source,{kind:'preparation',bank:i-1});if(i===4||i===8){await checkpoint(i);if(i===4)await open(seed);}
  }
  // Each checkpoint is copied cold. The same held-out source and prompt are used in every repeat.
  for(const n of [0,4,8])for(let repeat=1;repeat<=2;repeat++){
   const destination=path.join(out,`probe-${n}-${repeat}`);await fs.cp(path.join(out,'bank-'+n),destination,{recursive:true});await open(destination);await request('data','select',{id:probe.datasetId,version:probe.version});await run('growth-probe105',probe,{bank:n});record.tasks.at(-1).repeat=repeat;await save();await close();
  }
  await open(path.join(out,'recovery'));await run('recovery-summary105',await imported('Fresh recovery109',sequence(109,i=>Math.sin(i/9)*.012,.03)),{kind:'recovery',bank:0,restart:true});await close();
  await open(path.join(out,'rendered-fault'));await run('fault-summary105',await imported('Fresh fault12',sequence(12,i=>i*.003)),{kind:'rendered-refusal',bank:0,expected:'interrupted',fault:true});await close();
  const altered=path.join(out,'altered-original');await fs.cp(path.join(out,'bank-8'),altered,{recursive:true});await open(altered);
  const first=(await api('/api/workflows/list',{})).workflows.find(w=>w.qualificationCase105?.name==='monotonic');assert(first);const before=await api('/api/artifact/read',{id:first.report.id});
  const mutation=await api('/api/artifact/write',{id:before.id,expectedVersion:before.version,title:'Deliberately altered retained qualification',files:{'index.html':'<!doctype html><html><body>Altered retained proof control</body></html>'}});record.alteredOriginal={before,after:mutation,workflowId:first.id,project:altered};
  await request('data','select',{id:probe.datasetId,version:probe.version});await run('altered-proof105',probe,{kind:'source-refusal',bank:8,expected:'interrupted'});
  assert.equal(record.injections.length,1);await close();
  const last=record.tasks.find(t=>t.kind==='probe'&&t.bank===8);await open(last.project);await page.locator('#mission044-open').click();await page.locator('#mission044-task').selectOption(last.id);await request('agency043','mission-open',{id:last.id});await app.evaluate(({BrowserWindow})=>BrowserWindow.getAllWindows().find(w=>w.isVisible()).setSize(1100,1100));await capture('qualification105-1100.png');await close();record.passed=true;
 }catch(e){record.error=e.stack;if(app)await capture('unexpected105.png').catch(()=>{});throw e;}
 finally{await close();record.finishedAt=new Date().toISOString();await save();require('../../scripts/Study-Build095.cjs').verify(repo);}
}
main().catch(e=>{console.error(e);process.exitCode=1;});
