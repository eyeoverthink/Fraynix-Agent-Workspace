'use strict';
const {test}=require('node:test'),assert=require('node:assert/strict'),{classify}=require('../../scripts/Calibration-Payoff088.cjs');
function input(){return{admit:false,horizon:2,fitMillis:100,reviewMillis:10,methods:{simple:{goals:2,successes:2,callerMillis:1000},memory:{goals:2,successes:2,callerMillis:700},calibrated:{goals:2,successes:2,callerMillis:800}}};}
test('a profitable declined fit is labelled only for the comparator it beats',()=>{const r=classify(input());assert(r.comparators.simple.falseDecline);assert.equal(r.comparators.simple.actualNetMillis,90);assert(!r.comparators.memory.falseDecline);assert.equal(r.comparators.memory.actualNetMillis,-210);});
test('execution saving alone cannot repay fitting',()=>{const x=input();x.fitMillis=300;assert(!classify(x).comparators.simple.calibrationPays);});
test('zero net is not positive payback',()=>{const x=input();x.fitMillis=190;assert.equal(classify(x).comparators.simple.actualNetMillis,0);assert(!classify(x).comparators.simple.falseDecline);});
test('an admitted profitable fit is not a false decline',()=>{const x=input();x.admit=true;const r=classify(x);assert(r.comparators.simple.calibrationPays);assert(!r.comparators.simple.falseDecline);});
test('incomplete and unmatched horizons cannot claim successful payback',()=>{for(const key of ['goals','successes']){const x=input();x.methods.calibrated[key]=1;assert.throws(()=>classify(x));}});
test('nonfinite or invented absent costs are refused',()=>{for(const v of [NaN,Infinity,-1,0]){const x=input();x.fitMillis=v;assert.throws(()=>classify(x));}const x=input();x.methods.memory.callerMillis=NaN;assert.throws(()=>classify(x));});
test('review cost is charged, with unchanged input',()=>{const x=input();x.reviewMillis=101;const before=JSON.stringify(x);assert.equal(classify(x).comparators.simple.actualNetMillis,-1);assert.equal(JSON.stringify(x),before);});
test('no inference from unobserved long horizons',()=>{const x=input();x.horizon=256;assert.throws(()=>classify(x));});
