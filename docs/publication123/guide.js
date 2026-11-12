'use strict';
const get=id=>document.getElementById(id),data=id=>JSON.parse(get(id).textContent),system=data('system-data'),inventory=data('inventory-data'),measurements=data('measurements-data');
function option(select,value,label){const o=document.createElement('option');o.value=value;o.textContent=label;select.append(o);}
const comparisons=measurements.comparisons;
comparisons.forEach((x,i)=>option(get('comparison'),i,`${x.partition} / ${x.case}: ${x.enabled} vs ${x.comparison}`));
const initial=comparisons.findIndex(x=>x.case==='supported-return'&&x.enabled==='full'&&x.comparison==='no-return');if(initial>=0)get('comparison').value=initial;
function cost(){const x=comparisons[Number(get('comparison').value)],v=Number(get('allowance').value),t=x.observedConservativeSecondsPerExtraPickup,d=x.elapsedDeltaSeconds;let answer=x.assessment;
 if(get('allowance').value.trim()===''||!Number.isFinite(v)||v<0||v>3600)answer='Enter a valid allowance from 0 to 3600 seconds.';
 else if(Number.isFinite(t))answer=v>=t?`Fits this allowance in the measured set: threshold ${t.toFixed(2)} extra seconds per pickup.`:`Exceeds this allowance: measured threshold ${t.toFixed(2)} extra seconds per pickup.`;
 get('result').textContent=answer;get('detail').textContent=`${x.partition} / ${x.case}: ${x.enabled} versus ${x.comparison}. ${x.pairs} pairs. Extra wall time: ${d.min.toFixed(2)} to ${d.max.toFixed(2)} seconds; median ${d.median.toFixed(2)}. Extra pickups: ${x.extraPickups.min} to ${x.extraPickups.max}. Safe paired finishes: ${x.safePairedFinishes?'yes':'no'}. Observed ranges, not statistical certainty.`;}
get('comparison').addEventListener('change',cost);get('allowance').addEventListener('input',cost);cost();
system.screenshots.forEach(x=>option(get('shot'),x.path.split('/').pop(),x.title));
function shot(){get('screenshot').src=get('shot').value;get('shot-full').href=get('shot').value;get('caption').textContent=get('shot').selectedOptions[0].textContent+' · actual app capture';}get('shot').addEventListener('change',shot);shot();
system.modules.forEach(x=>option(get('module'),x.id,x.name));
function moduleDetail(){const x=system.modules.find(x=>x.id===get('module').value);get('module-detail').textContent=x.name+' — '+x.source;}get('module').addEventListener('change',moduleDetail);moduleDetail();
function search(){get('files').replaceChildren();const q=get('search').value.toLowerCase();inventory.filter(x=>x.path.toLowerCase().includes(q)).slice(0,80).forEach(x=>{const li=document.createElement('li'),a=document.createElement('a');a.href='../../../'+x.path;a.textContent=x.path;li.append(a);get('files').append(li);});}get('search').addEventListener('input',search);search();
const phases={'123':'Completed: measure complete workflow cost with explicit wait/return choices, fresh projects and retained verification. No new automatic cost policy.','124':'Next: explicit user time allowances and uncertainty-aware estimates. Count estimation cost and test abstention on new scenes before changing defaults.','125':'Then: moving shells, reversals and changing supports. Measure contacts, false alarms, missed hazards and bounded recovery.'};
function phase(){get('phase-detail').textContent=phases[get('phase').value];}get('phase').addEventListener('change',phase);phase();
