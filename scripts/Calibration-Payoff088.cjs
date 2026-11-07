'use strict';
// Retrospective arithmetic only, after original native/cost/commit inspection.
const assert=require('node:assert/strict');
function classify({admit,horizon,fitMillis,reviewMillis,methods}){
 assert.equal(typeof admit,'boolean');assert(Number.isInteger(horizon)&&horizon>0&&horizon<=256);
 for(const x of [fitMillis,reviewMillis])assert(Number.isFinite(x)&&x>0,'Actual positive fit and preflight costs required');
 for(const mode of ['simple','memory','calibrated']){const m=methods[mode];assert(m&&m.goals===horizon&&m.successes===horizon,'Complete matched successful sequence required');assert(Number.isFinite(m.callerMillis)&&m.callerMillis>0);}
 const result={horizon,declined:!admit,fitCallerMillis:fitMillis,preflightCallerMillis:reviewMillis,comparators:{}};
 for(const name of ['simple','memory']){const executionSaving=methods[name].callerMillis-methods.calibrated.callerMillis,actualNet=executionSaving-fitMillis-reviewMillis;result.comparators[name]={executionSavingMillis:executionSaving,actualNetMillis:actualNet,calibrationPays:actualNet>0,falseDecline:!admit&&actualNet>0};}
 return result;
}
module.exports={classify};
