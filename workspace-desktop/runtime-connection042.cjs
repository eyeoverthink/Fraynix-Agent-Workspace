'use strict';
const fs=require('node:fs'),fsp=require('node:fs/promises'),path=require('node:path'),{createHash}=require('node:crypto'),{spawn}=require('node:child_process');
const protocol='fraynix-agent-host-042-v1';
const {budget,checkEffective}=require('./java-budget082.cjs');
function buildId(lib){const text=protocol+'\n'+fs.readdirSync(lib).filter(n=>n.endsWith('.jar')).sort().map(n=>n+':'+createHash('sha256').update(fs.readFileSync(path.join(lib,n))).digest('hex')+'\n').join('');return createHash('sha256').update(text).digest('hex');}
function validDescriptor(d,project){return d?.protocol===protocol&&/^http:\/\/127\.0\.0\.1:[1-9][0-9]{0,4}$/.test(d.origin)&&/^[a-f0-9]{64}$/.test(d.token)&&/^[a-f0-9]{64}$/.test(d.buildId)&&path.resolve(d.project).toLowerCase()===path.resolve(project).toLowerCase()&&Number.isSafeInteger(d.pid)&&d.pid>0&&typeof d.bootId==='string'&&typeof d.agentId==='string';}
function launchOwner(repo,project,log){
 const java=process.env.JAVA_HOME?path.join(process.env.JAVA_HOME,'bin',process.platform==='win32'?'java.exe':'java'):'java',lib=path.join(repo,'workspace-engine','build','install','workspace-engine','lib','*');
 const options={cwd:repo,windowsHide:true,detached:true,env:{...process.env,FRAYNIX_WORKSPACE_PROJECT:project},stdio:['ignore',log,log]};
 const limits=budget();
 if(process.platform!=='win32')return spawn(java,[...limits.args,'-cp',lib,'fraymus.workspace.AgentHost042'],options);
 // Start-Process gives Java an independent Windows lifetime and avoids inheriting interface/debugger handles.
 const literal=s=>"'"+s.replaceAll("'","''")+"'",args=limits.args.join(' ')+' -cp "'+lib+'" fraymus.workspace.AgentHost042';
 const command='$launch042=Start-Process -FilePath '+literal(java)+' -ArgumentList '+literal(args)+' -WorkingDirectory '+literal(repo)+' -WindowStyle Hidden -PassThru -ErrorAction Stop; $null=$launch042.WaitForExit(1000)';
 return spawn(path.join(process.env.SystemRoot,'System32','WindowsPowerShell','v1.0','powershell.exe'),['-NoProfile','-NonInteractive','-WindowStyle','Hidden','-EncodedCommand',Buffer.from(command,'utf16le').toString('base64')],{...options,detached:false});
}
async function connectRuntime042({repo,project,sessionId,onState=()=>{},onError=()=>{},timeoutMs=30000}){
 const requested=budget();
 await fsp.mkdir(project,{recursive:true});project=await fsp.realpath(project);const descriptor=path.join(project,'runtime-042','host.json'),expected=buildId(path.join(repo,'workspace-engine','build','install','workspace-engine','lib'));let d,child,timer,detached=false,updating=false;
 async function call(route,input={},owner=d){const r=await fetch(owner.origin+route,{method:'POST',headers:{Authorization:'Bearer '+owner.token,'Content-Type':'application/json','X-Fraynix-Interface':sessionId},body:JSON.stringify(input),signal:AbortSignal.timeout(5000)});const data=await r.json();if(!r.ok)throw Error(data.error||'Java agent request failed');return data;}
 async function probe(){let candidate;try{if((await fsp.stat(descriptor)).size>16384)throw Error('descriptor too large');candidate=JSON.parse(await fsp.readFile(descriptor,'utf8'));}catch{return null;}if(!validDescriptor(candidate,project))throw Error('The Java agent owner record is invalid; retain it for inspection');let value;try{value=await call('/api/agent042/state',{},candidate);}catch{return null;}if(value.bootId!==candidate.bootId||value.agentId!==candidate.agentId||value.pid!==candidate.pid)throw Error('Java agent ownership does not match its private record');if(candidate.buildId!==expected)throw Error('This project is running another app build. Use that app to stop Java before opening this build.');d=candidate;return value;}
 let state=await probe();if(!state){const log=fs.openSync(path.join(project,'engine.log'),'a');try{child=launchOwner(repo,project,log);child.on('error',onError);child.unref();}finally{fs.closeSync(log);}const start=Date.now();while(!state&&Date.now()-start<timeoutMs){await new Promise(r=>setTimeout(r,150));state=await probe();}if(!state)throw Error('The Java agent could not start. Its project may have another owner; see the local engine log.');}
 const leaseStart=Date.now();for(;;){try{state=await call('/api/agent042/attach',{sessionId});break;}catch(e){if(!/interface owns|cleanup is pending/.test(e.message)||Date.now()-leaseStart>=10000)throw e;await new Promise(r=>setTimeout(r,250));}}
 if(requested.explicit)try{checkEffective(requested,await call('/api/resources082/state'));}catch(e){await call('/api/agent042/detach',{sessionId});throw e;}
 onState(state);
 timer=setInterval(async()=>{if(updating||detached)return;updating=true;try{onState(await call('/api/agent042/touch',{sessionId}));}catch(e){onError(e);}finally{updating=false;}},2000);timer.unref();
 return{persistent:true,origin:d.origin,token:d.token,bootId:d.bootId,pid:d.pid,child,async detach(){if(detached)return;detached=true;clearInterval(timer);await call('/api/agent042/detach',{sessionId});},async stop(){return call('/api/agent042/stop',{bootId:d.bootId});}};
}
module.exports={buildId,validDescriptor,connectRuntime042};
