'use strict';
const {test}=require('node:test'),assert=require('node:assert/strict');
const {auditData101}=require('../data-creation101.cjs');
test('pending publication does not claim a saved skill',()=>{
 const {workflowSummary}=require('../workflows.cjs');const text=workflowSummary({profile101:'data-creation-workflow-101-v1',status:'completed',reason:'Verified',pendingSkill101:{id:'pending'},savedSkill:{name:'pending-one',version:1}});assert(text.includes('publication pending'));assert(!text.includes('Saved pending-one'));
});
test('independent audit uses asserted actions and retains four observations',async()=>{
 const calls=[],frames=[];const surface={async test(steps){assert(steps.some(s=>['expectText','expectValue','expectCount'].some(k=>k in s)),'Surface requires an actual assertion');calls.push(steps);return steps.map(()=>({passed:true}));},async isolated(){const value={frame:frames.length};frames.push(value);return value;}};
 const actual=await auditData101(surface,{rowCount:49,filter:'2',report:{id:'fixture',version:3}});
 assert.equal(calls.length,4);assert.equal(frames.length,4);assert.deepEqual(actual,{id:'fixture',version:3,frames});assert.equal(calls[1][0].expectValue,'2');assert.equal(calls[2][0].expectValue,'__no_match_101__');assert.equal(calls[3][1].expectValue,'');
});
test('failed action cannot become independent evidence',async()=>{
 let calls=0,snapshots=0;await assert.rejects(auditData101({async test(){return[{passed:++calls===1}];},async isolated(){snapshots++;return{};}},{rowCount:2,filter:'2',report:{id:'fixture',version:1}}),/Actual filter action failed/);assert.equal(snapshots,1);
});
