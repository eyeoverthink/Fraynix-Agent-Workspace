// Read-only source probes. Real imported modules with explicit substitute boundaries;
// no Chandra service, personal database, real model, tool action or neural worker starts.
import fs from 'node:fs';
import path from 'node:path';
import vm from 'node:vm';
import crypto from 'node:crypto';
import assert from 'node:assert/strict';
import { DatabaseSync } from 'node:sqlite';
const root=path.resolve(process.argv[2]), output=path.resolve(process.argv[3]);
const evidence={source:'e984a33685c0ed9e0c4f4c79e4cd1f094b4118de',scope:'Exact source evaluated with explicit test dependencies; disposable in-memory SQLite, simulated agent replies and neural sink. No model-quality, real-tool, physical-effect or full-app validation.',files:[],checks:[]};
const db=new DatabaseSync(':memory:');
async function moduleAt(file,dependencies){
 const filename=path.join(root,file),code=fs.readFileSync(filename,'utf8');evidence.files.push({path:file,sha256:crypto.createHash('sha256').update(code.replace(/\r\n/g,'\n')).digest('hex')});
 const context=vm.createContext({console,Date,Math,JSON,Set,Map,Error,Number,String,Array,Object,Promise,setTimeout,clearTimeout});
 const m=new vm.SourceTextModule(code,{context,identifier:filename});
 await m.link(async spec=>{const values=spec==='node:crypto'?crypto:dependencies[spec];assert(values,'Unexpected dependency '+spec);const names=Object.keys(values);return new vm.SyntheticModule(names,function(){for(const name of names)this.setExport(name,values[name]);},{context});});
 await m.evaluate();return m.namespace;
}
try{
 const collab=await moduleAt('server/fleet/collab.js',{'../db.js':{db},'./bus.js':{publish:()=>{}}});
 collab.initCollab({runAgent:async()=>({status:'timeout'})});
 const timeout=collab.createCollab({goal:'No side effects: timeout fixture',strategy:'chain',agents:['first','second']});
 await collab.runCollab(timeout.id);const timed=collab.getCollab(timeout.id);
 assert.equal(timed.status,'done');assert(timed.steps.every(s=>s.status==='done'));assert.equal(timed.result.finalOutput,'[No response]');
 evidence.checks.push({name:'Timeout is labeled completed',reproduced:true,sessionStatus:timed.status,stepStatuses:timed.steps.map(s=>s.status),output:timed.result.finalOutput});
 collab.initCollab({runAgent:async()=>{throw Error('Deliberate no-execution fixture')}});
 const failed=collab.createCollab({goal:'Every agent fails',strategy:'parallel',agents:['first','second']});await collab.runCollab(failed.id);const broken=collab.getCollab(failed.id);
 assert.equal(broken.status,'done');assert(broken.steps.every(s=>s.status==='error'));
 evidence.checks.push({name:'All parallel agents and synthesis fail yet session is done',reproduced:true,sessionStatus:broken.status,stepStatuses:broken.steps.map(s=>s.status)});
 const harness=await moduleAt('server/fleet/harness.js',{'../db.js':{db}});
 const sealed=harness.sealToolResult('missing-executor',{}, {error:'tool has no executor'},0);assert.equal(sealed.evidence.executed,true);
 evidence.checks.push({name:'Hash sealer alone does not establish execution',reproduced:true,executed:sealed.evidence.executed,result:sealed.result,scope:'Sealer contract probe; runtime source separately inspected for the no-executor branch.'});
 const large='x'.repeat(10000),bound=harness.boundResult(large,'single-line');assert(bound.bounded.length>4000);
 evidence.checks.push({name:'Long one-line output exceeds declared bound',reproduced:true,inputCharacters:large.length,outputCharacters:bound.bounded.length});
 const injections=[];
 const active=await moduleAt('server/intelligence/active-inference.js',{'../db.js':{db},'./ctrnn-instance.js':{getCTRNN:()=>({running:true,inject:(...v)=>injections.push(v)})},'./consciousness-gate.js':{getAutonomyModifiers:()=>({evolutionRate:1,tier:'fixture'})},'../fleet/bus.js':{publish:()=>{}}});
 const before=active.updateActiveInference({errors:{health:1},ep3Surprise:{}});
 const after=active.updateActiveInference({errors:{health:.1},ep3Surprise:{}});
 assert.equal(before.biasInjections,0);assert.equal(after.biasInjections,0);assert(after.effectiveness>before.effectiveness);
 evidence.checks.push({name:'Effectiveness rises without a motor action',reproduced:true,before:before.effectiveness,after:after.effectiveness,motorBiasInjections:0,scope:'Two substituted observations and a neural injection sink; no action executed. Error reduction does not identify its cause.'});
 const rsi=await moduleAt('server/vertiso/rsi.js',{'../config.js':{OLLAMA_URL:'http://127.0.0.1:1'},'../fraymus/intelligence/ollama-sanitize.js':{sanitizeOllamaContent:s=>s}});
 // Constructor accepts the disposable database; deterministic random isolates the heuristic.
 const engine=new rsi.RSIEngine({db});const saved=Math.random;Math.random=()=>.99;
 try{const result=await engine.evolve('probe',{}, {populationSize:2,mutationRate:0});assert.equal(result.mutations.length,0);assert.equal(result.improved,true);evidence.checks.push({name:'Unchanged genome receives improved fitness',reproduced:true,result});}finally{Math.random=saved;}
 evidence.passed=true;
}finally{db.close();fs.mkdirSync(path.dirname(output),{recursive:true});fs.writeFileSync(output,JSON.stringify(evidence,null,2)+'\n');}
console.log(JSON.stringify(evidence));
