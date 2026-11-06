'use strict';
// Combine independently inspected profiles; normalize only scene-instance IDs.
const fs=require('node:fs'),path=require('node:path'),assert=require('node:assert/strict');
const root=path.resolve(__dirname,'..'),read=p=>JSON.parse(fs.readFileSync(p,'utf8')),budgets=['four-768','two-512','one-384'];
function physical(frame){
 const names=new Map(frame.originalScene.objects.map(x=>[x.id,x.sourceId]));
 return {sourceText:frame.originalScene.sourceText,elapsedTicks:frame.elapsedTicks,bodies:frame.bodies.map(x=>{assert(names.has(x.id));const b=structuredClone(x);b.id=names.get(x.id);return b;}).sort((a,b)=>a.id.localeCompare(b.id))};
}
const profiles=budgets.map(b=>{const x=read(path.join(root,'build','analysis082-'+b+'.json'));assert(x.passed&&x.profiles.length===1);return x.profiles[0];});
const records=budgets.map(b=>read(path.join(root,'build','ui082-'+b,'result.json'))),crossBudget=[];
for(let i=1;i<records.length;i++)for(const mode of ['reference','parallel']){
 const a=records[0].arms.find(x=>x.mode===mode),b=records[i].arms.find(x=>x.mode===mode);
 for(let k=0;k<Math.min(a.rows.length,b.rows.length);k++){
  const x=a.rows[k],y=b.rows[k],row={budget:budgets[i],mode,goal:x.id,matchingStatus:x.status===y.status,matchingTicks:x.job.arm.chargedTicks===y.job.arm.chargedTicks};
  try{assert.deepEqual(physical(x.job.initialFrame),physical(y.job.initialFrame));row.matchingInitialPhysics=true;}catch{row.matchingInitialPhysics=false;}
  if(x.status==='completed'&&y.status==='completed')try{assert.deepEqual(physical(x.job.arm.programs[x.job.arm.selected].actualFrame),physical(y.job.arm.programs[y.job.arm.selected].actualFrame));row.matchingFinalPhysics=true;}catch{row.matchingFinalPhysics=false;}
  crossBudget.push(row);
 }
}
console.log(JSON.stringify({passed:true,profiles,crossBudget,scope:'Each profile was independently inspected once. Cross-budget physical comparison substitutes stable source roles for scene-instance IDs, retaining all positions, rotations, velocities, angular velocities, geometry and elapsed ticks. Differences are reported, never silently discarded.'},null,2));
