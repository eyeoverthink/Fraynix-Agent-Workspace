'use strict';
const {_electron}=require('playwright'),fs=require('node:fs/promises'),path=require('node:path'),os=require('node:os'),assert=require('node:assert/strict');
async function main(){
 const project=await fs.mkdtemp(path.join(os.tmpdir(),'fraynix-startup055-')),out=path.resolve(__dirname,'../test-output');let app;
 const record={source:require('node:child_process').execFileSync('git',['rev-parse','HEAD'],{encoding:'utf8'}).trim(),project,checks:[]};
 async function open(){const env={...process.env,FRAYNIX_WORKSPACE_PROJECT:project,FRAYNIX_WORKSPACE_TEST:'1',FRAYNIX_WORKSPACE_OFFLINE:'1',FRAYNIX_WORKSPACE_PERSISTENT:'0',FRAYNIX_WORKSPACE_PLAN_CPU_ONLY:'1'};delete env.ELECTRON_RUN_AS_NODE;app=await _electron.launch({args:[path.resolve(__dirname,'../main.cjs')],env,timeout:60000});for(const stream of [app.process().stdout,app.process().stderr])stream.on('data',b=>require('node:fs').appendFileSync(path.join(project,'test-desktop-process.log'),b));await app.firstWindow();await app.evaluate(async()=>{for(let i=0;i<600&&!global.workspaceTest;i++)await new Promise(r=>setTimeout(r,100));if(!global.workspaceTest)throw Error('Workspace did not recover after bounded startup failure');});}
 const api=(route,args={})=>app.evaluate((_,a)=>global.workspaceTest.api(a.route,a.args),{route,args});
 try{
  await open();record.fault=await api('/api/artifact/write',{title:'LABELLED renderer timeout fixture',files:{'index.html':'<!doctype html><html><body><h1>Deliberately unresponsive test artifact</h1><script>while(true){}</script></body></html>'}});await app.close();app=null;
  const start=Date.now();await open();record.recoveryWallMs=Date.now()-start;const events=await app.evaluate(()=>global.workspaceTest.activityTrace());assert(events.some(e=>JSON.stringify(e).includes('Saved artifact could not open; the workspace remains available')));record.events=events;
  const saved=await api('/api/artifact/read',{id:record.fault.id});assert.equal(saved.version,1);assert.equal(saved.contents['index.html'],record.fault.contents['index.html']);
  record.checks.push('Actual infinite-script artifact hits the bounded renderer stop during cold startup; workspace and Java API stay available, and the original file/version are preserved');
  await app.evaluate(()=>global.workspaceTest.execute('canvas.write',{title:'Recovered working canvas',files:{'index.html':'<!doctype html><html><body><h1 id="recovered055">Workspace recovered</h1></body></html>'}}));assert.equal(await app.evaluate(()=>global.workspaceTest.canvasRead('document.querySelector("#recovered055").textContent')),'Workspace recovered');
  record.checks.push('The same app opens and reads a new real canvas after the failed renderer; no restart, model request or native world mutation needed');
  record.passed=true;await fs.mkdir(out,{recursive:true});await fs.writeFile(path.join(out,'startup055-live.json'),JSON.stringify(record,null,2));console.log(JSON.stringify({passed:true,project,checks:record.checks,recoveryWallMs:record.recoveryWallMs}));
 }catch(e){record.error=e.stack;await fs.mkdir(out,{recursive:true});await fs.writeFile(path.join(out,'startup055-failed-'+Date.now()+'.json'),JSON.stringify(record,null,2));throw e;}
 finally{if(app)await app.close();}
}
main().catch(e=>{console.error(e);process.exitCode=1;});
