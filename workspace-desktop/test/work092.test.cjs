'use strict';
const {test}=require('node:test'),assert=require('node:assert/strict'),{work,taskWork,pair,recoveredClock}=require('../../scripts/Work092.cjs');
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
test('Prefix export after reopening is charged before the resumed segment',()=>{
 const prefix={...zero,reads:2,canonicalChecks:2,bytes:30,readMillis:4};
 const after={...prefix,reads:3,hits:1,bytes:45,readMillis:5};
 assert.deepEqual(taskWork({restartBoot:{initial:{...zero,reads:1,canonicalChecks:1,bytes:15,readMillis:2}},segments:[{before:zero,after:zero},{before:prefix,after}]}),after);
});
test('Restart pair rejects calibration eligibility and changed task',()=>{
 const row={bank:4,lifecycle:'restarted',callerMillis:10,segments:[],goal:{taskCost080:{task:{x:1},eligible:false},worldCommit:{revision:1}},job:{arm:{selected:'two-stage',chargedTicks:14400}}};
 assert.equal(pair(row,structuredClone(row)).savingMillis,0);
 const bad=structuredClone(row);bad.goal.taskCost080.eligible=true;assert.throws(()=>pair(row,bad));
 bad.goal.taskCost080.eligible=false;bad.goal.taskCost080.task.x=2;assert.throws(()=>pair(row,bad));
});
test('Process restart retains an unavailable complete clock, never a partial fit',()=>{
 const cost={eligible:false,totalMillis:0,executionMillis:25,reason:'Interrupted; excluded from calibration'},job={activeMillis:25,resumes:1};recoveredClock(cost,job);
 for(const patch of [{eligible:true},{totalMillis:10},{clock089:{}},{executionMillis:24}])assert.throws(()=>recoveredClock({...cost,...patch},job));
});
