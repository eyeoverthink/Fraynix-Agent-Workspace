'use strict';
const {test}=require('node:test'),assert=require('node:assert/strict');
const {webURL,command,ObservationLease,taskMode}=require('../policy.cjs');
test('CSV web creation stays in artifact mode while native measurement comparisons use physical tools',()=>{
  assert.deepEqual(taskMode('Build a CSV spreadsheet app'),{needsScene:false,needsComparison:false});
  assert.deepEqual(taskMode('Create a ramp experiment and compare the saved dataset'),{needsScene:true,needsComparison:true});
  assert.deepEqual(taskMode('Run a 3D ramp'),{needsScene:true,needsComparison:false});
});
const {NetworkPolicy,localURL}=require('../network-policy.cjs');
test('a corrupted theme or invalid UTF-8 stops startup before code is loaded',()=>{
  const fs=require('node:fs'),path=require('node:path'),os=require('node:os');const {verifyRendererSources}=require('../source-integrity.cjs');const folder=fs.mkdtempSync(path.join(os.tmpdir(),'fraynix-source-guard-'));
  for(const name of ['index.html','style.css','measurement.css','app.js','scene-lab.js','measurement-panel.js'])fs.writeFileSync(path.join(folder,name),'/* valid UTF-8 fixture */');
  assert.equal(verifyRendererSources(folder).checked,6);fs.writeFileSync(path.join(folder,'style.css'),Buffer.alloc(128));assert.throws(()=>verifyRendererSources(folder),/style.css.*NUL/);
  fs.writeFileSync(path.join(folder,'style.css'),'body{}');fs.writeFileSync(path.join(folder,'app.js'),Buffer.from([255]));assert.throws(()=>verifyRendererSources(folder),/app.js.*UTF-8/);
});
test('offline application policy permits loopback and rejects external navigation without dispatch',()=>{
  const policy=new NetworkPolicy(true);for(const u of ['http://127.0.0.1:12/a','http://localhost/a','http://[::1]/'])assert(localURL(u));
  for(const u of ['https://example.com','http://127.0.0.1.evil.test','http://user:secret@localhost/','file:///x'])assert(!localURL(u));
  assert.throws(()=>policy.require('https://example.com'),/Offline mode/);assert.equal(policy.status().blockedRequests,1);policy.require('http://127.0.0.1:123/a');
});
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
