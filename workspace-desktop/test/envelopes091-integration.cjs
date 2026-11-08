'use strict';
const {launch}=require('./calibration-integration.cjs'),fs=require('node:fs/promises'),sync=require('node:fs'),path=require('node:path'),assert=require('node:assert/strict'),cp=require('node:child_process'),crypto=require('node:crypto');
const sleep=ms=>new Promise(r=>setTimeout(r,ms)),sha=b=>crypto.createHash('sha256').update(b).digest('hex');
async function main(){
 const repo=path.resolve(__dirname,'../..'),out=path.join(repo,'build/ui091-study'),project=path.join(out,'project');
 assert(!sync.existsSync(path.join(out,'checkpoint.json')),'Use a fresh study output');await fs.mkdir(project,{recursive:true});
 const protocol=JSON.parse(await fs.readFile(path.join(__dirname,'fixtures/procedures091-protocol.json'),'utf8'));
 const record={protocol,source:cp.execFileSync('git',['rev-parse','HEAD'],{cwd:repo,encoding:'utf8'}).trim(),buildReceipt:require('../../scripts/Study-Build091.cjs').verify(repo),project,startedAt:new Date().toISOString(),rows:[],audits:[],checks:[],boots:[],captures:[]};
 process.env.FRAYNIX_WORKSPACE_PERSISTENT='0';process.env.FRAYNIX_WORKSPACE_PLAN_CPU_ONLY='1';process.env.FRAYNIX_WORKSPACE_JAVA_HEAP_MIB='512';process.env.FRAYNIX_WORKSPACE_JAVA_PROCESSORS='2';
 let app,page;const errors=[],save=()=>fs.writeFile(path.join(out,'checkpoint.json'),JSON.stringify(record,null,2));
 const api=(route,args={})=>app.evaluate((_,x)=>global.workspaceTest.api(x.route,x.args),{route,args});
 function osRead(action,pid){return JSON.parse(cp.execFileSync('pwsh',['-NoProfile','-File',path.join(repo,'scripts/Measure-Resources082.ps1'),'-Action',action,'-TargetPid',String(pid),'-Processors','2','-Checkout',repo],{encoding:'utf8',windowsHide:true}));}
 async function open(){const at=performance.now();({app,page}=await launch(project));page.on('pageerror',e=>errors.push(e.message));await page.waitForFunction(()=>window.agency043View);const r=await api('/api/resources082/state');assert.equal(r.profile.cpus,2);assert.equal(r.configuredMaxHeapBytes,512*1024*1024);const pinned=osRead('pin',r.pid);assert.equal(BigInt(pinned.affinity).toString(2).split('1').length-1,2);record.boots.push({callerMillis:performance.now()-at,resources:r,pinned,initial:await api('/api/procedures091/diagnostics')});await save();}
 async function capture(row,label){
  await app.evaluate((_,id)=>global.workspaceTest.goals.execute('goal.read',{id}),row.goalId);await sleep(1000);let visible=false;
  for(let i=0;i<5&&!visible;i++){await app.evaluate((_,id)=>global.workspaceTest.execute('canvas.open',{id}),row.goal.report.id);await page.waitForFunction(id=>document.querySelector('#object-detail').textContent.includes(id),row.goal.report.id);await page.locator('#preview-tab').click();await sleep(1200);visible=await page.evaluate(id=>document.querySelector('#preview-tab').classList.contains('selected')&&document.querySelector('#object-detail').textContent.includes(id),row.goal.report.id);}assert(visible);
  const text=await app.evaluate(()=>global.workspaceTest.canvasRead('document.querySelector("#procedure079-decision").innerText'));assert(text.includes('Memory decision'));
  for(const width of [1600,1100]){await app.evaluate(({BrowserWindow},w)=>BrowserWindow.getAllWindows().find(x=>x.isVisible()).setSize(w,1100),width);await sleep(300);assert(await page.evaluate(()=>document.documentElement.scrollWidth<=innerWidth));const file=label+'-'+width+'.png';await fs.writeFile(path.join(out,file),Buffer.from(await app.evaluate(()=>global.workspaceTest.capture()),'base64'));record.captures.push({file,width,goalId:row.goalId,report:row.goal.report.id,text});}await save();
 }
 async function run(spec,policy,mode,retain=false){
  const scene=await api('/api/spatial/import',{text:JSON.stringify(spec.source)}),world=await api('/api/world/create',{sceneId:scene.id,title:'LABELLED091 '+spec.id}),ob=await api('/api/world/observe',{worldId:world.id}),binding=Object.fromEntries(['worldId','expectedRevision','expectedHash'].map(k=>[k,ob[k]]));
  await api('/api/procedures091/test-mode',{mode});const before=await api('/api/procedures091/diagnostics'),resource=await api('/api/resources082/state'),beforeOS=osRead('read',resource.pid),at=performance.now();
  const g=await api('/api/world/start',{...binding,targets:spec.targets,forceLimitN:spec.forceLimitN,torqueLimitNm:spec.torqueLimitNm,selectiveMemory079:true,memoryMode079:policy,retainExperience079:retain,...(policy==='memory'?{procedureReceipt085:record.seed.goalId}:{}),request:'Edition091 native verification '+spec.id});
  let goal;for(let i=0;i<4000;i++){goal=await api('/api/goals/read',{id:g.id});if(['completed','interrupted','cancelled'].includes(goal.status))break;await sleep(200);}const callerMillis=performance.now()-at,after=await api('/api/procedures091/diagnostics'),afterOS=osRead('read',resource.pid);
  const row={id:spec.id,policy,mode,retain,goalId:g.id,goal,status:goal.status,callerMillis,before,after,resource,beforeOS,afterOS,binding};record.rows.push(row);await save();assert.equal(goal.status,'completed',JSON.stringify(goal));assert(goal.poseView.passed);assert(goal.taskCost080.eligible);assert(goal.taskCost080.totalMillis<=callerMillis);assert.equal(goal.worldCommit.revision,binding.expectedRevision+1);assert.equal(beforeOS.affinity,afterOS.affinity);
  const exportAt=performance.now(),original=await api('/api/procedures079/export',{id:g.id});row.originalFile=g.id+'-originals.json';await fs.writeFile(path.join(out,row.originalFile),JSON.stringify(original));row.exportMillis=performance.now()-exportAt;row.job=original.job;row.observed=await api('/api/world/observe',{worldId:world.id});assert.equal(row.observed.expectedHash,goal.worldCommit.hash);
  console.log('RESULT',spec.id,policy,mode,callerMillis.toFixed(1),'ms',after.hits-before.hits,'hits');await save();return row;
 }
 async function audit(row,mode,pass){if(pass==='cold')await api('/api/procedures091/test-mode',{mode});const before=await api('/api/procedures091/diagnostics'),at=performance.now(),result=await api('/api/procedures090/verify',{id:row.goalId}),callerMillis=performance.now()-at,after=await api('/api/procedures091/diagnostics');assert(result.passed);record.audits.push({id:row.goalId,mode,pass,before,after,callerMillis,result});await save();}
 try{
  await open();record.seed=await run(protocol.training,'simple','reuse',true);assert.equal(record.seed.job.arm.selected,'two-stage');
  for(const item of protocol.tasks)for(const mode of item.order)await run(item.spec,item.policy,mode);
  const selected=record.rows.findLast(r=>r.mode==='reuse'&&r.policy==='memory');
  for(const modes of [['reference','reuse'],['reuse','reference']])for(const mode of modes){await audit(selected,mode,'cold');await audit(selected,mode,'warm');}
  const baseline=record.audits[0].result;for(const a of record.audits)assert.deepEqual(a.result,baseline);
  await api('/api/procedures091/test-mode',{mode:'reuse'});await api('/api/procedures090/verify',{id:selected.goalId});
  const attempt=selected.job.arm.attempts[0],file=path.join(project,'world-procedures-079',selected.goalId,'attempts',attempt.id,'original.json'),original=await fs.readFile(file),stat=await fs.stat(file),text=original.toString('utf8');
  const tampered=Buffer.from(text.replace(/"sha256":"([a-f0-9])/,(_,c)=>'"sha256":"'+(c==='0'?'1':'0')));assert.equal(tampered.length,original.length);assert.notEqual(sha(tampered),sha(original));
  try{await fs.writeFile(file,tampered);await fs.utimes(file,stat.atime,stat.mtime);await assert.rejects(api('/api/procedures090/verify',{id:selected.goalId}),/envelope|native|integrity/i);record.checks.push('Warm full verification refused same-size native-envelope mutation with preserved mtime');}finally{await fs.writeFile(file,original);assert.equal(sha(await fs.readFile(file)),sha(original));}
  assert.deepEqual(await api('/api/procedures090/verify',{id:selected.goalId}),baseline);
  await capture(selected,'verified-memory');await capture(record.rows.find(r=>r.id.includes('short')&&r.mode==='reuse'),'verified-short');
  await app.close();app=null;await open();assert.equal(record.boots.at(-1).initial.entries,0);const before=await api('/api/procedures091/diagnostics'),at=performance.now(),cold=await api('/api/procedures090/verify',{id:selected.goalId});record.restart={before,after:await api('/api/procedures091/diagnostics'),callerMillis:performance.now()-at,result:cold};assert.deepEqual(cold,baseline);assert(record.restart.after.canonicalChecks>before.canonicalChecks);assert.equal((await api('/api/world/observe',{worldId:selected.binding.worldId})).expectedHash,selected.goal.worldCommit.hash);
  assert.deepEqual(errors,[]);record.errors=errors;record.finishedAt=new Date().toISOString();record.passed=true;require('../../scripts/Study-Build091.cjs').verify(repo);await fs.writeFile(path.join(out,'result.json'),JSON.stringify(record,null,2));console.log('PASS actual091 native task, source, outcome, report, commit, mutation and cold-restart checks');
 }finally{if(app)await app.close();await save();}
}
main().catch(e=>{console.error(e);process.exitCode=1;});
