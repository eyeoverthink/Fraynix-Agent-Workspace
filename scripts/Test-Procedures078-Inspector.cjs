'use strict';
const fs=require('node:fs'),path=require('node:path'),assert=require('node:assert/strict'),{gzipSync}=require('node:zlib'),{createHash}=require('node:crypto');
const {inspectCase:baseInspect}=require('./Inspect-Procedures078.cjs'),{unpack}=require('./Inspect-Counterfactual045.cjs');
const folder=path.resolve(process.argv[2]),study=JSON.parse(fs.readFileSync(path.join(folder,'study.json'),'utf8'));
const result=JSON.parse(fs.readFileSync(path.join(folder,'result.json'),'utf8')),proofRoot=path.join(result.project,'procedures-078'),inspectCase=(x,s)=>baseInspect(x,s,proofRoot);
const raw=fs.readFileSync(path.join(folder,'train-open-originals.json'),'utf8'),read=()=>JSON.parse(raw);assert(inspectCase(read(),study));
const pack=r=>{const b=Buffer.from(JSON.stringify(r));return{format:'fraynix.native-world-run-gzip-v1',rawBytes:b.length,rawSha256:createHash('sha256').update(b).digest('hex'),gzipBase64:gzipSync(b).toString('base64')};};
function alterStage(x,change){const a=x.row.results['two-stage'],id=a.programs['two-stage'].forecast[1],item=x.originals.find(o=>o.attempt.id===id);assert(item);const run=unpack(item.run);change(run);item.run=pack(run);}
const probes=[
 ['changed payload',x=>x.row.spec.source.objects.find(o=>o.sourceId==='A').massKg+=.1],
 ['changed target',x=>x.row.spec.targets[0].positionM[0]+=.1],
 ['concealed cost',x=>x.row.results.direct.chargedTicks=0],
 ['false procedure success',x=>x.row.results.direct.success=!x.row.results.direct.success],
 ['missing native original',x=>x.originals.pop()],
 ['duplicated original',x=>x.originals.push(x.originals[0])],
 ['wrong stage reference',x=>x.row.results['two-stage'].programs['two-stage'].forecast.reverse()],
 ['forecast reused as actual',x=>x.row.results['two-stage'].programs['two-stage'].actual[0]=x.row.results['two-stage'].programs['two-stage'].forecast[0]],
 ['reset elapsed stage clock',x=>alterStage(x,r=>r.definition.worldFrame.elapsedTicks=0)],
 ['erased carried velocity',x=>alterStage(x,r=>r.definition.worldFrame.bodies[0].velocity=[0,0,0])],
 ['changed middle target',x=>alterStage(x,r=>r.plan.intent.targets[0].positionM[0]+=.2)],
 ['extra force authority',x=>alterStage(x,r=>r.plan.intent.forceLimitN+=1)],
 ['changed final frame',x=>x.row.results['two-stage'].programs['two-stage'].actualFrame.bodies[0].position[0]+=.1],
 ['falsified forecast residual',x=>x.row.results['two-stage'].programs['two-stage'].comparisons[0].maxPositionResidualM=1],
 ['hidden actual reserve',x=>x.row.results['two-stage'].attempts[0].admissionRemaining=1],
 ['changed bank',x=>x.bank[0].family='invented'],
 ['invented waypoint',x=>x.row.results['two-stage'].programs['two-stage'].waypoints['forecast/0'].targets[0].positionM[0]+=.01],
 ['erased search bound',x=>x.row.results['two-stage'].programs['two-stage'].waypoints['forecast/0'].candidateChecks=0],
 ['hypothesis called measured',x=>x.row.results['two-stage'].programs['two-stage'].waypoints['forecast/0'].geometricOnly=false]
].map(([name,change])=>{const x=read();change(x);assert.throws(()=>inspectCase(x,study),undefined,name);return{name,refused:true};});
const candidate=study.rows.find(r=>r.results.memory.retrieval),file=path.join(folder,candidate.id+'-originals.json');for(const [name,change] of [
 ['changed retrieved receipt',x=>x.row.results.memory.retrieval.selected.id='foreign'],
 ['changed ranked distance',x=>x.row.results.memory.retrieval.ranked[0].distance+=.1],
 ['control borrowed memory',x=>x.row.results.simple.retrieval=x.row.results.memory.retrieval],
 ['evaluation before bank freeze',x=>x.row.results.memory.attempts[0].plannedAt='2020-01-01T00:00:00Z']
]){const x=JSON.parse(fs.readFileSync(file,'utf8'));change(x);assert.throws(()=>inspectCase(x,study),undefined,name);probes.push({name,refused:true});}
const hitRow=study.rows.find(r=>r.results.cached.proofCheck?.mode==='hit');assert(hitRow,'Primary must expose real reused evidence for these probes');for(const [name,change] of [
 ['changed cached source',x=>x.row.results.cached.proofCheck.binding.code='foreign'],
 ['changed evidence bytes',x=>x.row.results.cached.proofCheck.evidence[0].sha256='0'.repeat(64)],
 ['foreign evidence path',x=>x.row.results.cached.proofCheck.evidence[0].path='../outside'],
 ['invented origin check',x=>x.row.results.cached.proofCheck.originReceiptSha256='0'.repeat(64)],
 ['negative proof time',x=>x.row.results.cached.proofCheck.hashMillis=-1],
 ['cache claimed a different selection',x=>x.row.results.cached.selected='direct']
]){const x=JSON.parse(fs.readFileSync(path.join(folder,hitRow.id+'-originals.json'),'utf8'));change(x);assert.throws(()=>inspectCase(x,study),undefined,name);probes.push({name,refused:true});}
console.log(JSON.stringify({passed:true,originalPassed:true,probes},null,2));
