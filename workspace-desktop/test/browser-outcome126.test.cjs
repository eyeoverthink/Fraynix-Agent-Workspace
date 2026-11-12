'use strict';
const {test}=require('node:test'),assert=require('node:assert/strict'),{observeOutcome126}=require('../browser-outcome126.cjs');
const claim=()=>({taskId:'task',generation:2,id:'effect',extra:{browserContract126:{url:'http://127.0.0.1/fixture',checks:[{selector:'#result',text:'done'}]},contractSha256:'a'.repeat(64)}});
test('host boundary returns actual values without adopting a model success flag',async()=>{
 let code;const surface={kind:'browser',isolated:async s=>{code=s;return {url:'http://127.0.0.1/fixture',checks:[{selector:'#result',count:1,text:'wrong',visible:true}]};},view:{webContents:{getURL:()=> 'http://127.0.0.1/fixture'}}};
 const result=await observeOutcome126(surface,claim());assert.equal(result.checks[0].text,'wrong');assert.equal(result.passed,undefined);assert(!code.includes('.click('));assert(!code.includes('fetch('));assert.equal(result.generation,2);
});
test('navigation during result capture is refused',async()=>{
 const s={kind:'browser',isolated:async()=>({url:'http://127.0.0.1/fixture',checks:[]}),view:{webContents:{getURL:()=> 'http://127.0.0.1/other'}}};await assert.rejects(observeOutcome126(s,claim()),/navigated/);
});
test('arbitrary actions and canvas substitution cannot enter the browser check',async()=>{
 await assert.rejects(observeOutcome126({kind:'canvas'},claim()),/criteria/);const c=claim();c.extra.browserContract126.checks[0].action='click';await assert.rejects(observeOutcome126({kind:'browser'},c),/Invalid/);
});
