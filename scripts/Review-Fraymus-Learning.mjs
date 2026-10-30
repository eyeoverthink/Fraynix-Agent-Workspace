import assert from 'node:assert/strict';
import fs from 'node:fs';
import {pathToFileURL} from 'node:url';
import path from 'node:path';
import {execFileSync} from 'node:child_process';
const review=path.resolve(process.argv[2] || '../fraymus-os-one-review');
const reviewedCommit=execFileSync('git',['-C',review,'rev-parse','HEAD'],{encoding:'utf8'}).trim();
assert.equal(reviewedCommit,'2861859ad41dc51475159361946f70c585f8f686','Use the pinned reviewed commit');
const {createMind,think,normalizeMind}=await import(pathToFileURL(path.join(review,'client/src/tools/brainMind.js')));
const {FlyGNNv10,runFlyGnnMeasureTrainMeasure}=await import(pathToFileURL(path.join(review,'client/src/tools/flyGnnConnectome.js')));
const checks=[];const examples=[];
let mind=createMind();
for(const [goal,wanted,rewrite] of [['14 - 9',5,true],['100 - 37',63,false],['6 * 7',42,true],['8 / 2',4,true],['(14 - 9) * 2',10,false]]){
 const result=think(mind,goal);assert.equal(result.verdict,'PASS');assert.equal(result.got,wanted);assert.equal(result.steps.some(s=>s.phase==='rewrite'),rewrite);mind=result.mind;examples.push({goal,got:result.got,phases:result.steps.map(s=>s.phase)});
}
checks.push('Seed errors repaired; later goals reuse learned operators; past cases replay');
mind=normalizeMind(JSON.parse(JSON.stringify(mind)));
for(let i=1;i<=24;i++){
 const goal=`(${i+51} - ${i}) * 2 / 3`;
 const result=think(mind,goal);assert.equal(result.verdict,'PASS');assert.equal(result.got,34);assert.equal(result.steps.some(s=>s.phase==='rewrite'),false);mind=result.mind;
}
checks.push('Serialized learned mind passes 24 fresh arithmetic goals without another rewrite');
assert.equal(think(mind,'1 / 0').verdict,'FAIL');
assert.equal(think(mind,'recall nowhere').verdict,'FAIL');
assert.equal(think(mind,'Build a working web browser').verdict,'UNVERIFIED');
checks.push('Division by zero and missing facts fail; unsupported prose remains unverified');
mind=think(mind,'remember studio = fraymus').mind;
assert.equal(think(mind,'recall studio').got,'fraymus');
mind=think(mind,'bind fire 7').mind;
const collision=think(mind,'bind steam 4');
assert.equal(collision.verdict,'PASS');assert.equal(collision.mind.placement,'next-free');assert.equal(collision.mind.binds.length,2);
assert.ok(collision.steps.some(s=>s.phase==='rewrite'));
checks.push('Fact read-back works; bind collision revises placement while preserving two concepts');
const fly=new FlyGNNv10(42,1000);const weightBefore=fly.getHebbianForSlice(1);fly.trainHebbian(1,0.05);const weightAfter=fly.getHebbianForSlice(1);assert.ok(weightAfter>weightBefore);
checks.push('FlyGNN trainHebbian changes actual stored synaptic weights');
const zero=runFlyGnnMeasureTrainMeasure('test',0),ten=runFlyGnnMeasureTrainMeasure('test',10);
const fixedBefore=zero.benchmarkComparison.afterDedicated1000NIsolated.networkOnlyWordHeldoutPct;
const fixedAfter=ten.benchmarkComparison.afterDedicated1000NIsolated.networkOnlyWordHeldoutPct;
assert.equal(fixedBefore,fixedAfter);assert.notEqual(zero.afterHadWeight,ten.afterHadWeight);
checks.push('Reported held-out word percentage is unchanged for zero vs ten training iterations; it is a fixed demonstration value');
const result={date:'2026-10-29',repository:'eyeoverthink/Fraymus-OS-One',commit:'2861859ad41dc51475159361946f70c585f8f686',scope:'Isolated pure-engine probes; no UI, mail, project worker, provider or shared service started',passed:true,checks,examples,freshArithmeticCases:24,weights:{before:weightBefore,after:weightAfter},fixedBenchmark:{zeroIterations:fixedBefore,tenIterations:fixedAfter},limitations:['No full-model Ollama training performed','No real GGUF experiment rerun','No full browser UI acceptance run for the reviewed app']};
if(process.argv[3]) fs.writeFileSync(process.argv[3],JSON.stringify(result,null,2)+'\n');
console.log(JSON.stringify(result,null,2));
