'use strict';
const {connectRuntime042}=require('../runtime-connection042.cjs'),fs=require('node:fs/promises'),path=require('node:path'),crypto=require('node:crypto'),assert=require('node:assert/strict');
async function main(){
 const repo=path.resolve(__dirname,'../..'),project=path.join(repo,'build/owner082'),out=path.join(repo,'build/owner082-result.json');await fs.mkdir(project,{recursive:true});const errors=[];
 process.env.FRAYNIX_WORKSPACE_OFFLINE='1';process.env.FRAYNIX_WORKSPACE_PLAN_CPU_ONLY='1';process.env.FRAYNIX_WORKSPACE_JAVA_HEAP_MIB='512';process.env.FRAYNIX_WORKSPACE_JAVA_PROCESSORS='2';
 let owner,started;const record={project,checks:[]};
 const connect=()=>connectRuntime042({repo,project,sessionId:crypto.randomUUID(),onError:e=>errors.push(e.message)});
 try{
  owner=await connect();started=owner;record.pid=owner.pid;record.bootId=owner.bootId;await owner.detach();owner=null;
  process.env.FRAYNIX_WORKSPACE_JAVA_HEAP_MIB='384';process.env.FRAYNIX_WORKSPACE_JAVA_PROCESSORS='1';
  await assert.rejects(connect(),/different resource limits/);record.checks.push('Actual persistent owner refuses requested budget mismatch without replacement');
  process.env.FRAYNIX_WORKSPACE_JAVA_HEAP_MIB='512';process.env.FRAYNIX_WORKSPACE_JAVA_PROCESSORS='2';owner=await connect();assert.equal(owner.pid,record.pid);assert.equal(owner.bootId,record.bootId);record.checks.push('Matching settings reconnect to the same preserved Java owner');
  assert.deepEqual(errors,[]);record.passed=true;await fs.writeFile(out,JSON.stringify(record,null,2));console.log(JSON.stringify(record));
 }catch(e){record.error=e.stack;await fs.writeFile(out,JSON.stringify(record,null,2));throw e;}finally{if(owner||started){await (owner||started).stop();if(owner)await owner.detach().catch(()=>{});}}
}
main().catch(e=>{console.error(e);process.exitCode=1;});
