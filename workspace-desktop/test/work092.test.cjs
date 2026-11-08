'use strict';
const {test}=require('node:test'),assert=require('node:assert/strict'),{work,pair}=require('../../scripts/Work092.cjs');
const zero={reads:0,bytes:0,canonicalChecks:0,hits:0,failures:0,readMillis:0};
test('Cross-boot work sums segments, never subtracts reset counters',()=>{
 const a={reads:10,bytes:100,canonicalChecks:3,hits:7,failures:0,readMillis:5};
 const b={reads:2,bytes:20,canonicalChecks:1,hits:1,failures:0,readMillis:2};
 assert.deepEqual(work([{before:zero,after:a},{before:zero,after:b}]),{reads:12,bytes:120,canonicalChecks:4,hits:8,failures:0,readMillis:7});
 assert.throws(()=>work([{before:a,after:b}]),/Counter reset/);
});
test('Accounting rejects missing reads and non-finite elapsed work',()=>{
 assert.throws(()=>work([{before:zero,after:{...zero,reads:1}}]));
 assert.throws(()=>work([{before:zero,after:{...zero,readMillis:NaN}}]));
});
test('Restart pair rejects calibration eligibility and changed task',()=>{
 const row={bank:4,lifecycle:'restarted',callerMillis:10,segments:[],goal:{taskCost080:{task:{x:1},eligible:false},worldCommit:{revision:1}},job:{arm:{selected:'two-stage',chargedTicks:14400}}};
 assert.equal(pair(row,structuredClone(row)).savingMillis,0);
 const bad=structuredClone(row);bad.goal.taskCost080.eligible=true;assert.throws(()=>pair(row,bad));
 bad.goal.taskCost080.eligible=false;bad.goal.taskCost080.task.x=2;assert.throws(()=>pair(row,bad));
});
