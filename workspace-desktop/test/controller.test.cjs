'use strict';
const test=require('node:test'),assert=require('node:assert/strict');
const {validPolicy,stageControl,selectAction,trainController}=require('../controller.cjs');
test('controller refreshes stale observations without guessing or retrying the action',()=>{
  const next={tool:'browser.click',args:{observation:'old',ref:'7'}};
  assert.equal(selectAction(next,{observationLimit:1,refreshStale:true},{observation:'new'}).tool,'browser.observe');
  assert.equal(selectAction(next,{observationLimit:2,refreshStale:false},{observation:'new'}).tool,'browser.click');
  assert.throws(()=>validPolicy({observationLimit:1,refreshStale:true,shell:true}));
});
test('adapted read budget preserves mandatory testing and repair stages',()=>{
  const policy={observationLimit:1,refreshStale:true};
  assert.deepEqual(stageControl({needsArtifact:true,observed:1},null,policy,['browser.observe','canvas.write']).allowed,['canvas.write']);
  assert.deepEqual(stageControl({created:true}, {id:'a',version:1},policy,['finish']).allowed,['canvas.test']);
  assert.deepEqual(stageControl({created:true,lastReadVersion:2},{id:'a',version:2,test:{passed:false}},policy,['finish']).allowed,['canvas.write']);
});
test('bounded worker updates actual Fraymus prototypes from recorded failure classes',async()=>{
  const input={activePolicy:{version:1,observationLimit:2,refreshStale:false},episodes:[{id:'episode',status:'failed',steps:[{tool:'browser.click',failure:'stale-observation'}]}]};
  const result=await trainController(input);assert.equal(result.routes['stale-observation'],'stale-observation');assert.equal(result.proposal.policy.refreshStale,true);assert.equal(result.proposal.controllerMemory.totalUpdates,1);assert.equal(result.proposal.controllerMemory.classes.find(c=>c.id==='stale-observation').samples,2);
  assert(result.proposal.controllerMemory.classes.every(c=>c.centroid.length===64));
  const repeated=await trainController({...input,controllerMemory:result.proposal.controllerMemory});assert.equal(repeated.prototypeUpdates,1,'A recorded episode must not be trained twice');
  const quiet=await trainController({...input,activePolicy:{version:2,observationLimit:2,refreshStale:true}});assert.equal(quiet.proposal,null);
  const repeatedRead=await trainController({...input,activePolicy:{version:2,observationLimit:2,refreshStale:true},episodes:[{id:'another-episode',status:'completed-unverified',steps:[{tool:'browser.observe',failure:'repeated-observation'}]}]});assert.equal(repeatedRead.proposal.policy.observationLimit,1);assert.equal(repeatedRead.routes['repeated-observation'],'repeated-observation');
  const abort=new AbortController();abort.abort();await assert.rejects(trainController(input,{signal:abort.signal}),/cancelled/);
});
test('pinned Think fixture retains a learned rule after serialization',async()=>{
  const {createMind,think,normalizeMind}=await import('../vendor/fraymus-brain/brainMind.js');const first=think(createMind(),'14 - 9');assert(first.steps.some(s=>s.phase==='rewrite'));
  const second=think(normalizeMind(JSON.parse(JSON.stringify(first.mind))),'100 - 37');assert.equal(second.got,63);assert(!second.steps.some(s=>s.phase==='rewrite'));
});
