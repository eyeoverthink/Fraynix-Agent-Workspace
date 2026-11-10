'use strict';
// Fresh isolated project; actual Java task lifecycle and Electron DOM observations, no model fixtures.
const {launch}=require('./calibration-integration.cjs'),fs=require('node:fs/promises'),path=require('node:path'),assert=require('node:assert/strict'),cp=require('node:child_process');
const {taskCosts}=require('../../scripts/WorkflowCost102.cjs');
const repo=path.resolve(__dirname,'../..'),out=path.join(repo,'build/summary104'),project=path.join(out,'project'),sleep=ms=>new Promise(r=>setTimeout(r,ms));
async function main(){
 Object.assign(process.env,{FRAYNIX_WORKSPACE_PERSISTENT:'0',FRAYNIX_WORKSPACE_PLAN_CPU_ONLY:'1',FRAYNIX_WORKSPACE_JAVA_HEAP_MIB:'512',FRAYNIX_WORKSPACE_JAVA_PROCESSORS:'2'});
 require('../../scripts/Study-Build095.cjs').verify(repo);assert(!require('node:fs').existsSync(out),'Preserve prior trials; use a new destination');await fs.mkdir(project,{recursive:true});
 const record={source:cp.execFileSync('git',['rev-parse','HEAD'],{cwd:repo,encoding:'utf8'}).trim(),startedAt:new Date().toISOString(),tasks:[],hostSpans:[],injections:[],captures:[],scope:'Java-owned implementation, no model-generation claims. Actual source and outcome checks. Recorded data and synthetic cases labelled. Complete caller intervals include recovery/refusal, not a matched speed comparison.'};
 let app,page,session=0;const save=()=>fs.writeFile(path.join(out,'result.json'),JSON.stringify(record,null,2));
 const api=(route,args={})=>app.evaluate((_,x)=>global.workspaceTest.api(x.route,x.args),{route,args:route==='/api/state'?undefined:args});
 const request=(operation,command,args={})=>page.evaluate(x=>window.workspace.request(x),{operation,command,args});
 async function open(){({app,page}=await launch(project));session++;await app.evaluate(({BrowserWindow},session)=>{
  const w=BrowserWindow.getAllWindows().find(w=>w.isVisible());w.setSize(1600,1100);w.setTitle('Fraynix · edition 104 · input-driven summaries');global.summary104={spans:[],injections:[],corrupt:false};const Surface=global.workspaceTest.surfaceClass102;
  for(const method of ['test','isolated']){const original=Surface.prototype[method];Surface.prototype[method]=async function(...args){if(this.kind!=='canvas')return original.apply(this,args);const start=Date.now(),clock=process.hrtime.bigint();let ok=false;try{
   if(method==='isolated'&&global.summary104.corrupt&&String(args[0]).includes("one('table#data-table')")){global.summary104.corrupt=false;const mutation=await original.call(this,"(()=>{const e=document.querySelector('#summary-travel');if(!e)throw Error('No summary fault target');const before=e.textContent;e.textContent='999.000000';return {before,after:e.textContent}})()");global.summary104.injections.push({at:Date.now(),session,...mutation,scope:'Deliberately changed one displayed summary value after behavior checks; source and code unchanged'});}
   const v=await original.apply(this,args);ok=true;return v;
  }finally{global.summary104.spans.push({kind:method==='test'?'browser-test':'browser-observation',session,start,end:Date.now(),nanos:String(process.hrtime.bigint()-clock),ok});}};}
 },session);await page.waitForFunction(()=>window.inputs100&&[...document.querySelector('#model').options].some(o=>o.value==='gpt-oss:20b'),{},{timeout:65000});await page.locator('#model').selectOption('gpt-oss:20b');}
 async function flush(){if(!app)return;const x=await app.evaluate(()=>{const s=global.summary104;const out={spans:s.spans,injections:s.injections};s.spans=[];s.injections=[];return out;});record.hostSpans.push(...x.spans);record.injections.push(...x.injections);await save();}
 async function close(){await flush();await app.close();app=null;}
 async function capture(name){await sleep(350);await fs.writeFile(path.join(out,name),Buffer.from(await app.evaluate(()=>global.workspaceTest.capture()),'base64'));record.captures.push(name);}
 async function imported(name,csv,{measured=false,mapping={},missingPolicy='reject'}={}){const d=await request('data','import',{title:name,filename:name+'.csv',csv,sourceKind:measured?'measured':'synthetic',sourceNote:measured?'Unchanged published Delfys013 recording; not a new measurement.':'Declared arithmetic104 test sequence; not physical footage.',missingPolicy,mapping:{timeColumn:'time_s',distanceColumn:'displacement_m',timeUnit:'s',distanceUnit:'m',...mapping}});await request('data','select',{id:d.datasetId,version:d.version});return api('/api/measurement/read',{id:d.datasetId,version:d.version});}
 async function submit(row){row.start=Date.now();row.project=project;record.tasks.push(row);await save();const before=new Set((await api('/api/agency043/state')).tasks.map(t=>t.id));await page.waitForFunction(()=>!document.querySelector('#run').disabled,{},{timeout:30000});await page.locator('#goal').fill(row.prompt);await page.locator('#run').click();for(let i=0;i<150;i++){const t=(await api('/api/agency043/state')).tasks.find(t=>!before.has(t.id)&&t.goal===row.prompt);if(t){row.id=t.id;return;}await sleep(200);}throw Error('Chat request not admitted');}
 async function wait(row,target){let last='';for(let i=0;i<800;i++){row.task=await api('/api/agency043/read',{id:row.id});const status=row.task.status+' '+row.task.reason;if(status!==last){console.log(row.name+': '+status);last=status;}if(row.task.status===target)return;if(['completed','cancelled','interrupted'].includes(row.task.status))throw Error('Unexpected task outcome: '+status);await sleep(250);}throw Error('Bounded trial exceeded 200s');}
 const workflow=row=>api('/api/workflows/read',{id:row.task.completion044?.javaWorkflow?.id||row.task.context044?.ownedWorkflowId});
 async function run(name,source,prompt,{expected='completed',restart=false,fault=false}={}){
  const row={name,source,prompt,expected};if(fault)await app.evaluate(()=>global.summary104.corrupt=true);await submit(row);
  if(restart){await wait(row,'interrupted');row.boundary=await workflow(row);assert.equal(row.boundary.stages[0].status,'completed');row.restart=true;row.pauseStart=Date.now();await capture('restart104.png');await flush();await app.evaluate(()=>global.workspaceTest.crashEngine());await sleep(300);await app.close().catch(()=>{});app=null;await open();const restored=await workflow(row);assert.equal(restored.stages[0].goalId,row.boundary.stages[0].goalId);assert.deepEqual(restored.implementation103,row.boundary.implementation103);await page.locator('#mission044-open').click();await page.locator('#mission044-task').selectOption(row.id);await page.waitForFunction(()=>!document.querySelector('#mission044-resume').disabled);await page.locator('#mission044-resume').click();for(let i=0;i<100;i++){if((await api('/api/agency043/read',{id:row.id})).resumes>0)break;await sleep(100);}row.pauseEnd=Date.now();}
  await wait(row,expected);row.end=Date.now();row.workflow=await workflow(row);row.children=[];row.artifacts=[];
  for(const stage of row.workflow.stages){const g=await api('/api/goals/read',{id:stage.goalId});row.children.push(g);row.artifacts.push(await api('/api/artifact/read',{id:g.report.id}));}
  let jobs=[];try{jobs=JSON.parse(await fs.readFile(path.join(project,'creation-044/requests.json'),'utf8')).data.jobs;}catch(e){if(e.code!=='ENOENT')throw e;}row.generation=jobs.filter(j=>j.goalId===row.children[1].id);assert.equal(row.generation.length,0);assert.equal(row.children[1].draftNumber,1);assert(row.children[1].summary104);assert.equal(row.children[1].modelName,'Java input-summary104');
  await flush();row.cost=taskCosts(row,record.hostSpans);await page.locator('#mission044-open').click();await page.locator('#mission044-task').selectOption(row.id);
  if(expected==='completed'){assert(row.workflow.audit.passed);assert(row.artifacts[1].test.passed);await request('agency043','mission-open',{id:row.id});assert((await page.locator('#mission044-facts').innerText()).match(/Java summary|Reused code/));}
  else{assert(!row.workflow.savedSkill);if(fault){await assert.rejects(()=>api('/api/workflows/resume',{id:row.workflow.id}));row.resumeRefused=true;assert(await page.locator('#mission044-resume').isDisabled());}}
  await capture(name+'-1600.png');if(expected==='interrupted'){await request('agency043','control',{id:row.id,action:'cancel'});await page.waitForFunction(()=>!document.querySelector('#run').disabled,{},{timeout:20000});await sleep(400);}await save();return row;
 }
 const fresh=(extra='')=>'Analyze the selected data and create an input-driven motion summary report. '+extra;
 const reuse=(extra='')=>'Reuse fresh-summary104 version 1 on the selected dataset. '+extra;
 const sequence=(n,fn,dt=.1)=>'time_s,displacement_m\n'+Array.from({length:n},(_,i)=>(i*dt).toFixed(8)+','+fn(i).toFixed(8)).join('\n')+'\n';
 try{
  await open();
  await run('early-retention-refused',await imported('Early save gate104',sequence(7,i=>i*.002)),fresh('Save the whole workflow as early-summary104.'),{expected:'interrupted'});assert.equal((await api('/api/state')).procedures.length,0);
  await run('recorded52',await imported('Recorded52 qualification104',await fs.readFile(path.join(repo,'docs/datasets/delfys75-013/training.csv'),'utf8'),{measured:true}),fresh('Gap limit 0.05 seconds and speed limit 5 m/s.'));
  await run('reversal96',await imported('Synthetic96 reversal104',sequence(96,i=>(i%24<12?i%12:12-i%12)*.002-.01,.02)),fresh());
  await run('flat32',await imported('Synthetic32 flat104',sequence(32,()=>-.02,.02)),fresh());
  await run('units-flags16',await imported('Synthetic16 units and flags104',sequence(16,i=>i<8?i*32:(15-i)*32,100),{mapping:{timeUnit:'ms',distanceUnit:'cm'}}),fresh('Gap limit 0.05 seconds and speed limit 0.2 m/s.'));
  await run('skipped-input',await imported('Synthetic missing rows104','time_s,displacement_m\n0,0\n0.1,\n0.2,0.01\n0.3,0.02\n',{missingPolicy:'skip'}),fresh());
  const retained=await run('heldout37-saved',await imported('Synthetic37 held-out104',sequence(37,i=>(i<13?i*.003:i<25?.036-(i-12)*.005:-.024+(i-24)*.001),.07)),fresh('Save the whole workflow as fresh-summary104.'));
  assert.equal(retained.workflow.qualification104.proofs.length,5);assert(retained.workflow.savedSkill);await close();await open();record.coldSavedSkill=(await api('/api/state')).procedures.find(p=>p.id===retained.workflow.savedSkill.id);assert(record.coldSavedSkill);
  await run('reused27-mapped',await imported('Synthetic27 mapped reuse104',sequence(27,i=>15+Math.sin(i/4)*12,80),{mapping:{timeUnit:'ms',distanceUnit:'mm',direction:-1,distanceOrigin:10,timeOffsetS:.3}}),reuse('Gap limit 0.1 seconds and speed limit 0.05 m/s.'));
  await run('reused109-restarted',await imported('Synthetic109 restart104',sequence(109,i=>Math.sin(i/9)*.015,.03)),reuse('Review after data.'),{restart:true});
  await run('precision2',await imported('Synthetic decimal boundary104','time_s,displacement_m\n0,0\n1,0.0000005\n'),reuse());
  await app.evaluate(({BrowserWindow})=>BrowserWindow.getAllWindows().find(w=>w.isVisible()).setSize(1100,1100));await capture('summary104-1100.png');await app.evaluate(({BrowserWindow})=>BrowserWindow.getAllWindows().find(w=>w.isVisible()).setSize(1600,1100));
  await run('rendered-fault-refused',await imported('Synthetic rendered fault104',sequence(12,i=>i*.002)),reuse(),{expected:'interrupted',fault:true});assert.equal(record.injections.length,1);assert.equal((await api('/api/state')).procedures.length,1);record.passed=true;
 }catch(e){record.error=e.stack;if(app)await capture('unexpected104.png').catch(()=>{});throw e;}
 finally{if(app)await close();record.finishedAt=new Date().toISOString();await save();require('../../scripts/Study-Build095.cjs').verify(repo);}
}
main().catch(e=>{console.error(e);process.exitCode=1;});
