'use strict';
const {test}=require('node:test'),assert=require('node:assert/strict'),{timing150}=require('../recorder150.cjs');
test('source recorder holds actual sample counts, never substitutes elapsed time',()=>{
 assert.equal(timing150('covered',17,90000).ready,false);
 assert.equal(timing150('covered',18,500).ready,true);
 assert.equal(timing150('reference',33,90000).remaining,1);
 assert.equal(timing150('returned',24,30000).ready,true);
});
test('action deadlines expire at exact bounded time and do not extend on repeated checks',()=>{
 assert.equal(timing150('covering',7,11999).expired,false);assert.equal(timing150('covering',7,12000).expired,true);
 assert.equal(timing150('revealing',1,14999).remaining,1);assert.equal(timing150('revealing',1,15000).expired,true);
 assert.equal(timing150('revealing',0,20000).remaining,0);
 assert.deepEqual(timing150('revealing',9,11000),timing150('revealing',9,11000));
});
test('invalid clocks and phases refuse instead of silently choosing another phase',()=>{
 for(const args of [['covered',-1,1],['covered',1.1,1],['covered',1,NaN],['covered',1,-1],['invented',1,1]])assert.throws(()=>timing150(...args));
 assert.equal(timing150('timing-expired',0,1).kind,'idle');
});
