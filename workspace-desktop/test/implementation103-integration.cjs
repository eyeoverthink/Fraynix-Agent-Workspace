'use strict';
// Actual retained102 model implementations, fresh Java data tasks and real Electron observations.
const {launch}=require('./calibration-integration.cjs'),fs=require('node:fs/promises'),path=require('node:path'),assert=require('node:assert/strict'),cp=require('node:child_process');
const {taskCosts}=require('../../scripts/WorkflowCost102.cjs');
const repo=path.resolve(__dirname,'../..'),out=path.join(repo,'build/reuse103'),sleep=ms=>new Promise(r=>setTimeout(r,ms));
async function main(){
 Object.assign(process.env,{FRAYNIX_WORKSPACE_PERSISTENT:'0',FRAYNIX_WORKSPACE_PLAN_CPU_ONLY:'1',FRAYNIX_WORKSPACE_JAVA_HEAP_MIB:'512',FRAYNIX_WORKSPACE_JAVA_PROCESSORS:'2'});
 require('../../scripts/Study-Build095.cjs').verify(repo);
 const record={source:cp.execFileSync('git',['rev-parse','HEAD'],{cwd:repo,encoding:'utf8'}).trim(),startedAt:new Date().toISOString(),tasks:[],hostSpans:[],injections:[],captures:[],projects:[],errors:[],scope:'Actual retained102 local-model code; no generated replacement or model output fixture. Fresh original Java source and outcome checks. Deliberate DOM fault separately labelled. Complete caller wall time includes restart, host checks and polling; not a matched speed comparison.'};
 let app,page,project,session=0;const save=()=>fs.writeFile(path.join(out,'result.json'),JSON.stringify(record,null,2));
 const api=(route,args={})=>app.evaluate((_,x)=>global.workspaceTest.api(x.route,x.args),{route,args});
 const request=(operation,command,args={})=>page.evaluate(x=>window.workspace.request(x),{operation,command,args});
 async function open(name){project=path.join(out,name,'project');({app,page}=await launch(project));session++;
  await app.evaluate(({BrowserWindow},session)=>{const w=BrowserWindow.getAllWindows().find(w=>w.isVisible());w.setSize(1600,1100);w.setTitle('Fraynix · edition 103 · verified code, fresh data');global.reuse103={spans:[],injections:[],corrupt:false};const Surface=global.workspaceTest.surfaceClass102;
   for(const method of ['test','isolated']){const original=Surface.prototype[method];Surface.prototype[method]=async function(...args){if(this.kind!=='canvas')return original.apply(this,args);const start=Date.now(),clock=process.hrtime.bigint();let ok=false;try{
    if(method==='isolated'&&global.reuse103.corrupt&&String(args[0]).includes("one('table#data-table')")){global.reuse103.corrupt=false;const mutation=await original.call(this,"(()=>{const e=document.querySelector('#data-table tbody tr td');if(!e)throw Error('No fault target');const before=e.textContent;e.textContent='INJECTED-FAULT-103';return {before,after:e.textContent}})()");global.reuse103.injections.push({at:Date.now(),session,...mutation,scope:'Deliberate one-time displayed cell corruption; retained code and files unchanged'});}
    const value=await original.apply(this,args);ok=true;return value;
   }finally{global.reuse103.spans.push({kind:method==='test'?'browser-test':'browser-observation',session,start,end:Date.now(),nanos:String(process.hrtime.bigint()-clock),ok});}};}
  },session);
  await page.waitForFunction(()=>window.inputs100&&[...document.querySelector('#model').options].some(o=>o.value==='gpt-oss:20b'),{},{timeout:65000});await page.locator('#model').selectOption('gpt-oss:20b');
 }
 async function flush(){if(!app)return;const v=await app.evaluate(()=>{const r={spans:global.reuse103.spans,injections:global.reuse103.injections};global.reuse103.spans=[];global.reuse103.injections=[];return r;});record.hostSpans.push(...v.spans);record.injections.push(...v.injections);await save();}
 async function close(){await flush();await app.close();app=null;}
 async function capture(name){await sleep(300);await fs.writeFile(path.join(out,name),Buffer.from(await app.evaluate(()=>global.workspaceTest.capture()),'base64'));record.captures.push(name);}
 async function importData(title,csv,measured=false){const d=await request('data','import',{title,filename:title+'.csv',csv,sourceKind:measured?'measured':'synthetic',sourceNote:measured?'Unchanged recorded Delfys edition013 source; not a new measurement.':'Labelled103 arithmetic sequence, not physical footage.',missingPolicy:'reject',mapping:{timeColumn:'time_s',distanceColumn:'displacement_m',timeUnit:'s',distanceUnit:'m'}});await request('data','select',{id:d.datasetId,version:d.version});return api('/api/measurement/read',{id:d.datasetId,version:d.version});}
 async function submit(row){row.start=Date.now();row.project=project;record.tasks.push(row);await save();const before=new Set((await api('/api/agency043/state')).tasks.map(t=>t.id));await page.waitForFunction(()=>!document.querySelector('#run').disabled,{},{timeout:30000});await page.locator('#goal').fill(row.prompt);await page.locator('#run').click();
  for(let i=0;i<100;i++){const t=(await api('/api/agency043/state')).tasks.find(t=>!before.has(t.id)&&t.goal===row.prompt);if(t){row.id=t.id;return;}await sleep(200);}throw Error('No admitted chat request');
 }
 async function wait(row,target){let last='';for(let i=0;i<600;i++){row.task=await api('/api/agency043/read',{id:row.id});const state=row.task.status+' '+row.task.reason;if(state!==last){console.log(row.name+': '+state);last=state;}if(row.task.status===target)return;if(['completed','cancelled','interrupted'].includes(row.task.status))throw Error('Unexpected outcome: '+state);await sleep(250);}throw Error('Reuse exceeded150s harness bound');}
 async function workflow(row){return api('/api/workflows/read',{id:row.task.completion044?.javaWorkflow?.id||row.task.context044?.ownedWorkflowId});}
 async function collect(row){row.end=Date.now();row.workflow=await workflow(row);row.children=[];row.artifacts=[];for(const s of row.workflow.stages){const g=await api('/api/goals/read',{id:s.goalId});row.children.push(g);row.artifacts.push(await api('/api/artifact/read',{id:g.report.id}));}
  const journal=JSON.parse(await fs.readFile(path.join(project,'creation-044/requests.json'),'utf8')).data.jobs;row.generation=journal.filter(j=>j.goalId===row.children[1].id);assert.equal(row.generation.length,0);assert.equal(row.children[1].draftNumber,1);assert(row.workflow.implementation103);assert.deepEqual(row.workflow.implementation103,row.children[1].implementation103);
  row.original=await api('/api/artifact/read',{id:row.workflow.implementation103.artifact.id});const code=a=>Object.fromEntries(Object.entries(a.contents).filter(([k])=>!k.startsWith('inputs/')));assert.deepEqual(code(row.original),code(row.artifacts[1]));assert.notEqual(row.original.id,row.artifacts[1].id);assert.equal(row.artifacts[1].contents['inputs/source.csv'],row.artifacts[0].contents['normalized.csv']);await flush();row.cost=taskCosts(row,record.hostSpans);await save();
 }
 async function stopped(row){await request('agency043','control',{id:row.id,action:'cancel'});await page.waitForFunction(()=>!document.querySelector('#run').disabled,{},{timeout:20000});await sleep(450);}
 async function run(row,{restart=false,fault=false}={}){if(fault)await app.evaluate(()=>global.reuse103.corrupt=true);await submit(row);
  if(restart){await wait(row,'interrupted');row.boundary=await workflow(row);assert.equal(row.boundary.step,'creation');assert.equal(row.boundary.stages[0].status,'completed');row.restart=true;row.pauseStart=Date.now();await capture('restart103.png');await flush();await app.evaluate(()=>global.workspaceTest.crashEngine());await sleep(300);await app.close().catch(()=>{});app=null;await open('explorer');const restored=await workflow(row);assert.equal(restored.stages[0].goalId,row.boundary.stages[0].goalId);assert.deepEqual(restored.implementation103,row.boundary.implementation103);
   await page.locator('#mission044-open').click();await page.locator('#mission044-task').selectOption(row.id);await page.waitForFunction(()=>!document.querySelector('#mission044-resume').disabled);await page.locator('#mission044-resume').click();for(let i=0;i<100;i++){if((await api('/api/agency043/read',{id:row.id})).resumes>0)break;await sleep(100);}assert((await api('/api/agency043/read',{id:row.id})).resumes>0);row.pauseEnd=Date.now();
  }
  await wait(row,row.expected);await collect(row);await page.locator('#mission044-open').click();await page.locator('#mission044-task').selectOption(row.id);
  if(row.expected==='completed'){assert(row.workflow.audit.passed);assert(row.artifacts[1].test.passed);await request('agency043','mission-open',{id:row.id});}
  else{assert(!row.workflow.savedSkill);await assert.rejects(()=>api('/api/workflows/resume',{id:row.workflow.id}));row.resumeRefused=true;}
  await capture(row.name+'-1600.png');if(row.expected==='interrupted')await stopped(row);await save();
 }
 try{
  await open('explorer');const original=JSON.parse(await fs.readFile(path.join(out,'explorer/original102.json'),'utf8'));record.projects.push({name:'explorer',project,originalSkill:original.tasks[0].workflow.savedSkill});
  const fresh=await importData('Recorded52 fresh input103',await fs.readFile(path.join(repo,'docs/datasets/delfys75-013/training.csv'),'utf8'),true);
  await run({name:'recorded52-restarted',source:fresh,expected:'completed',prompt:'Reuse cost-explorer102 version 1 on the selected dataset with gap limit 0.05 seconds and speed limit 5 m/s. Save the whole workflow as retained-explorer103. Review after data.'},{restart:true});
  assert(record.tasks[0].workflow.savedSkill);await close();await open('explorer');record.coldSavedSkill=(await api('/api/state')).procedures.find(p=>p.id===record.tasks[0].workflow.savedSkill.id);assert(record.coldSavedSkill);
  const csv96='time_s,displacement_m\n'+Array.from({length:96},(_,i)=>(i*.02).toFixed(5)+','+((i%24<12?i%12:12-i%12)*.002-.01).toFixed(5)).join('\n')+'\n';
  await run({name:'synthetic96-descendant',source:await importData('Synthetic96 reversal103',csv96),expected:'completed',prompt:'Reuse retained-explorer103 version 1 on the selected dataset with gap limit 0.05 seconds and speed limit 5 m/s.'});
  const flat='time_s,displacement_m\n'+Array.from({length:32},(_,i)=>(i*.02).toFixed(5)+',-0.02000').join('\n')+'\n';
  await run({name:'synthetic32-flat',source:await importData('Synthetic32 flat103',flat),expected:'completed',prompt:'Reuse retained-explorer103 version 1 on the selected dataset with gap limit 0.05 seconds and speed limit 5 m/s.'});
  await app.evaluate(({BrowserWindow})=>BrowserWindow.getAllWindows().find(w=>w.isVisible()).setSize(1100,1100));await capture('reuse103-1100.png');await app.evaluate(({BrowserWindow})=>BrowserWindow.getAllWindows().find(w=>w.isVisible()).setSize(1600,1100));
  await run({name:'quality-refused',source:await importData('Synthetic32 strict quality103',flat),expected:'interrupted',prompt:'Reuse cost-explorer102 version 1 on the selected dataset with gap limit 0.01 seconds and speed limit 5 m/s.'});
  await run({name:'display-fault-refused',source:await importData('Synthetic32 visible fault103',flat),expected:'interrupted',prompt:'Reuse cost-explorer102 version 1 on the selected dataset with gap limit 0.05 seconds and speed limit 5 m/s.'},{fault:true});
  assert(record.tasks.at(-1).workflow.auditHistory.some(a=>!a.passed));assert.equal(record.injections.length,1);await close();
  await open('summary');const old=JSON.parse(await fs.readFile(path.join(out,'summary/original102.json'),'utf8'));record.projects.push({name:'summary',project,originalSkill:old.tasks[0].workflow.savedSkill});
  // Only the isolated copy's exhausted102 request is stopped to release its shared workspace.
  const prior=await api('/api/agency043/read',{id:old.tasks[1].id});record.prior102={id:prior.id,status:prior.status};await stopped({id:prior.id});
  const rows=['time_s,displacement_m'];let time=0;for(let i=0;i<16;i++){if(i)time+=i===9?.45:.15;const x=i<4?i*.03:i<8?.09:i<12?.09-(i-7)*.04:-.07+(i-11)*.015;rows.push(time.toFixed(5)+','+x.toFixed(5));}
  await run({name:'summary16-refused',source:await importData('Synthetic16 changed summary103',rows.join('\n')+'\n'),expected:'interrupted',prompt:'Reuse summary-cost102 version 1 on the selected dataset with gap limit 0.2 seconds and speed limit 0.2 m/s.'});
  assert.equal((await api('/api/state')).procedures.length,1);record.passed=true;
 }catch(e){record.error=e.stack;if(app)await capture('unexpected103.png').catch(()=>{});throw e;}
 finally{if(app)await close();record.finishedAt=new Date().toISOString();await save();require('../../scripts/Study-Build095.cjs').verify(repo);}
}
main().catch(e=>{console.error(e);process.exitCode=1;});
