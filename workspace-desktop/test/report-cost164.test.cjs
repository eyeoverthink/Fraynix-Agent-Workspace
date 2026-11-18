'use strict';
const test=require('node:test'),assert=require('node:assert/strict'),{createReportCosts164}=require('../report-cost164.cjs');
test('report timings preserve values, exceptions and nested exclusive cost',async()=>{
 let tick=0n;const c=createReportCosts164({clock:()=>tick}),value={source:'actual caller result'};
 assert.equal(await c.measure('canvas.test',async()=>{tick+=2_000_000n;await c.measure('canvas.isolated',async()=>{tick+=5_000_000n;});tick+=3_000_000n;return value;}),value);
 const t=c.status().totals;assert.equal(t['canvas.test'].inclusiveMs,10);assert.equal(t['canvas.test'].exclusiveMs,5);assert.equal(t['canvas.isolated'].inclusiveMs,5);
 const failure=new Error('Original report refusal');await assert.rejects(c.measure('canvas.test',()=>{throw failure;}),e=>e===failure);assert.equal(c.status().totals['canvas.test'].failed,1);
});
test('overlapping asynchronous child spans are subtracted once',async()=>{
 let tick=0n,finishA,finishB;const c=createReportCosts164({clock:()=>tick});await c.measure('canvas.test',async()=>{
  tick=1_000_000n;const a=c.measure('canvas.isolated',()=>new Promise(r=>finishA=r));tick=2_000_000n;const b=c.measure('canvas.isolated',()=>new Promise(r=>finishB=r));tick=5_000_000n;finishA();await a;tick=7_000_000n;finishB();await b;tick=10_000_000n;
 });assert.equal(c.status().totals['canvas.test'].exclusiveMs,4);assert.equal(c.status().totals['canvas.isolated'].inclusiveMs,9);
});
test('disabled measurement delegates without sampling; retained details are bounded and isolated',async()=>{
 const off=createReportCosts164({enabled:false,clock:()=>{throw Error('No disabled clock');}}),v={};assert.equal(await off.measure('canvas.test',()=>v),v);assert.equal(off.status().operations,0);
 const c=createReportCosts164();for(let i=0;i<140;i++)await c.measure('canvas.test',()=>42);let s=c.status();assert.equal(s.recent.length,128);assert.equal(s.dropped,12);s.totals['canvas.test'].count=0;assert.equal(c.status().totals['canvas.test'].count,140);
});
test('attached methods keep their receiver, arguments and independently observable results',async()=>{
 const c=createReportCosts164(),surface={x:3,async navigate(n){return this.x+n;},async test(n){return this.navigate(n);},async isolated(n){return {n};}};c.attach(surface);assert.equal(await surface.test(4),7);assert.deepEqual(await surface.isolated('observation'),{n:'observation'});assert.equal(c.status().totals['canvas.navigate'].count,1);assert.equal(c.status().totals['canvas.test'].count,1);assert.equal(c.status().totals['canvas.isolated'].count,1);
});
