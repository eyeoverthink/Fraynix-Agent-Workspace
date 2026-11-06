'use strict';
const fs=require('node:fs'),path=require('node:path'),assert=require('node:assert/strict');
const {inspectCase}=require('./Inspect-Memory074.cjs');
const folder=path.resolve(process.argv[2]),bankDir=path.resolve(process.argv[3]);
const study=JSON.parse(fs.readFileSync(path.join(folder,'study.json'),'utf8'));
const bank=study.bank.map(b=>JSON.parse(fs.readFileSync(path.join(bankDir,b.id+'.json'),'utf8')).data);
const file=path.join(folder,'footprint-1-originals.json'),read=()=>JSON.parse(fs.readFileSync(file,'utf8'));
assert(inspectCase(read(),bank));
const probes=[
 ['hidden reconstruction cost',x=>{x.row.results.memory.chargedTicks=0;}],
 ['changed held-out payload',x=>{x.row.spec.source.objects.find(o=>o.sourceId==='A').massKg+=.1;}],
 ['changed target goal',x=>{x.row.spec.targets[0].positionM[0]+=.2;}],
 ['changed remembered family',x=>{x.row.results.memory.retrieval.selected.id='00000000-0000-0000-0000-000000000000';}],
 ['forecast reused as actual',x=>{x.row.results.simple.actualRunId=x.row.results.simple.forecastRunId;}],
 ['false outcome label',x=>{x.row.results.simple.success=!x.row.results.simple.success;}],
 ['changed candidate selection',x=>{x.row.results.search.selected='unverified-candidate';}],
 ['missing recorded attempt',x=>{x.originals.splice(x.originals.findIndex(o=>o.attempt.stage==='forecast'),1);}]
].map(([name,change])=>{const x=read();change(x);assert.throws(()=>inspectCase(x,bank),undefined,name);return{name,refused:true};});
console.log(JSON.stringify({passed:true,originalPassed:true,probes},null,2));
