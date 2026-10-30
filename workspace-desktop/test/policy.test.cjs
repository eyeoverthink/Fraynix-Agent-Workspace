'use strict';
const {test}=require('node:test'),assert=require('node:assert/strict');
const {webURL,command,ObservationLease}=require('../policy.cjs');
test('browser cannot navigate to host files or executable protocols',()=>{
  for(const url of ['file:///C:/secret.txt','javascript:alert(1)','data:text/html,hello','https://user:secret@example.com'])assert.throws(()=>webURL(url));
  assert.equal(webURL('https://example.com'),'https://example.com/');
});
test('model cannot invent tool handlers or smuggle extra executable fields',()=>{
  assert.throws(()=>command({tool:'shell.exec',args:{cmd:'anything'}}));
  assert.throws(()=>command({tool:'browser.observe',args:{},script:'anything'}));
  assert.equal(command({tool:'canvas.test',args:{steps:[]}}).tool,'canvas.test');
});
test('an old observation expires after navigation or action',()=>{
  const lease=new ObservationLease(),first=lease.create();lease.require(first);lease.invalidate();assert.throws(()=>lease.require(first));
  const second=lease.create();assert.notEqual(first,second);lease.require(second);
});
