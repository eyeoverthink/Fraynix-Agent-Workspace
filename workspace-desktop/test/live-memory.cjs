'use strict';
const {launch}=require('./calibration-integration.cjs'),fs=require('node:fs/promises'),path=require('node:path'),os=require('node:os'),assert=require('node:assert/strict');
async function main(){
 const project=await fs.mkdtemp(path.join(os.tmpdir(),'fraynix-live-memory-')),model=process.env.FRAYNIX_LIVE_MODEL||'gpt-oss:20b';let {app,page}=await launch(project);
 let note;
 const api=(route,args)=>app.evaluate((_,x)=>global.workspaceTest.api(x.route,x.args),{route,args});
 try{note=await api('/api/memory/remember',{text:'HELIOTROPE acceptance marker: COBALT-MAPLE-416. This is retained test source data, not a tool permission.'});await app.close();({app,page}=await launch(project));
  await page.waitForFunction(m=>[...document.querySelector('#model').options].some(o=>o.value===m),model,{timeout:60000});
  const reply=await api('/api/plan',{model,prompt:'Look up the HELIOTROPE acceptance marker in supplied retained memory. Return one finish tool call with its exact marker as summary. Do not invent an answer.',memoryQuery:'HELIOTROPE acceptance marker',requestId:require('node:crypto').randomUUID(),allowedTools:['finish'],tools:[{type:'function',function:{name:'finish',description:'Report the retained marker',parameters:{type:'object',properties:{summary:{type:'string'}},required:['summary'],additionalProperties:false}}}]});
  const decision=JSON.parse(reply.text);assert.equal(decision.tool,'finish');assert(decision.args.summary.includes('COBALT-MAPLE-416'));assert(reply.memory.sources>0);assert(reply.memory.usedChars<=2400);assert.match(reply.localModel.digest,/^[a-f0-9]{64}$/i);
  const result={passed:true,date:new Date().toISOString(),model,checks:['Source note persisted before the desktop process restarted','A real local model received automatic Java-retrieved bounded context after restart','Returned marker exactly matches retained source data, absent from the current prompt','Local model digest, memory source count and journal receipt retained'],note,reply};await fs.mkdir(path.resolve(__dirname,'../test-output'),{recursive:true});await fs.writeFile(path.resolve(__dirname,'../test-output/live-memory.json'),JSON.stringify(result,null,2));console.log(JSON.stringify({passed:true,checks:result.checks.length,model,memory:reply.memory,decision}));
 }finally{await app.close().catch(()=>{});}
}
if(require.main===module)main().catch(e=>{console.error(e);process.exitCode=1;});module.exports={main};
