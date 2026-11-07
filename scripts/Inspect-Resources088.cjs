'use strict';
// Descriptive counters from already inspected runs. Never a new performance decision.
const fs=require('node:fs'),path=require('node:path'),assert=require('node:assert/strict');
const root=path.resolve(__dirname,'..'),load=p=>JSON.parse(fs.readFileSync(p,'utf8'));
const cohorts={};
for(const name of ['four-768','two-512']){
 const r=load(path.join(root,'build/ui088-'+name+'/result.json')),a=load(path.join(root,'build/analysis088-'+name+'.json'));assert(r.passed&&a.passed);
 const rows=[...r.training,...r.calibration.flatMap(x=>Object.values(x.results)),...r.pilots,...r.rows],details=[];
 for(const x of rows){assert.equal(x.before.pid,x.after.pid);assert.deepEqual(x.before.profile,x.after.profile);assert.equal(x.beforeOS.affinity,x.afterOS.affinity);
  const cpu=x.after.processCpuNanos-x.before.processCpuNanos;assert(cpu>=0);
  const gcBefore=new Map(x.before.gc.map(g=>[g.name,g]));let gc=0,collections=0,available=true;
  for(const g of x.after.gc){const b=gcBefore.get(g.name);if(!b||[b.millis,b.count,g.millis,g.count].some(v=>v<0)){available=false;continue;}assert(g.millis>=b.millis&&g.count>=b.count);gc+=g.millis-b.millis;collections+=g.count-b.count;}
  const samples=[x.before,...x.samples,x.after];assert(samples.every(s=>s.heapUsedBytes>=0&&s.heapUsedBytes<=s.heapMaxBytes));
  details.push({id:x.id,goalId:x.goalId,policy:x.policy,phase:r.rows.includes(x)?'heldout':r.pilots.includes(x)?'pilot':r.training.includes(x)?'seed':'comparison',processCpuMillis:cpu/1e6,collectorMillis:available?gc:null,collections:available?collections:null,observedHeapPeakBytes:Math.max(...samples.map(s=>s.heapUsedBytes)),observedOsWorkingSetPeakBytes:Math.max(x.beforeOS.workingSetBytes,x.afterOS.workingSetBytes)});
 }
 const heldout={};for(const policy of ['simple','memory','calibrated']){const xs=details.filter(x=>x.phase==='heldout'&&x.policy===policy);assert.equal(xs.length,2);heldout[policy]={processCpuMillis:xs.reduce((n,x)=>n+x.processCpuMillis,0),collectorMillis:xs.every(x=>x.collectorMillis!==null)?xs.reduce((n,x)=>n+x.collectorMillis,0):null,observedHeapPeakBytes:Math.max(...xs.map(x=>x.observedHeapPeakBytes)),observedOsWorkingSetPeakBytes:Math.max(...xs.map(x=>x.observedOsWorkingSetPeakBytes))};}
 cohorts[name]={budget:r.budget,heldout,details};
}
const result={passed:true,cohorts,scope:'Descriptive CPU/GC deltas and sampled memory from before/after task observations, not causal attribution. Counter intervals include their measurement calls and are not identical to the Java task clock. Collector times can overlap work and must not be added to task elapsed time. Sampled peaks can miss between-sample peaks; heap excludes native/Electron/other processes. No threshold, fixture or outcome changed.'};
fs.writeFileSync(path.join(root,'build/resources088.json'),JSON.stringify(result,null,2)+'\n');console.log(JSON.stringify({passed:true,cohorts:Object.fromEntries(Object.entries(cohorts).map(([k,v])=>[k,v.heldout]))}));
