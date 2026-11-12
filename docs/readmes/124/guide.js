'use strict';
const get=id=>document.getElementById(id),data=id=>JSON.parse(get(id).textContent),system=data('system-data'),inventory=data('inventory-data'),measurements=data('measurements-data');
function option(select,value,label){const o=document.createElement('option');o.value=value;o.textContent=label;select.append(o);}
const comparisons=measurements.rows;
comparisons.forEach((x,i)=>option(get('comparison'),i,`${x.partition} / ${x.case}: recorded allowance ${x.allowance}`));
const initial=comparisons.findIndex(x=>x.case==='supported-return'&&x.allowance===30);if(initial>=0)get('comparison').value=initial;
function cost(){const x=comparisons[Number(get('comparison').value)],v=Number(get('allowance').value),t=x.requiredSecondsPerPickup;let answer=x.reason;
 if(get('allowance').value.trim()===''||!Number.isFinite(v)||v<0||v>120)answer='Enter a valid allowance from 0 to 120 seconds.';
 else if(Number.isFinite(t))answer=v>=t?`Fits this allowance in the measured set: threshold ${t.toFixed(2)} extra seconds per pickup.`:`Exceeds this allowance: measured threshold ${t.toFixed(2)} extra seconds per pickup.`;
 get('result').textContent=answer;get('detail').textContent=`Recorded choice: ${x.selected}; actual pickups: ${x.pickups}. First verified outcome: ${x.seconds.toFixed(2)}s. External request plus re-audit: ${x.externalSeconds.toFixed(2)}s. Decision overhead and completion reserve: ${x.overheadSeconds.toFixed(2)}s. Exploring an allowance here does not change the original receipt.`;}
get('comparison').addEventListener('change',cost);get('allowance').addEventListener('input',cost);cost();
system.screenshots.forEach(x=>option(get('shot'),x.path.split('/').pop(),x.title));
function shot(){get('screenshot').src=get('shot').value;get('shot-full').href=get('shot').value;get('caption').textContent=get('shot').selectedOptions[0].textContent+' · actual app capture';}get('shot').addEventListener('change',shot);shot();
system.modules.forEach(x=>option(get('module'),x.id,x.name));
function moduleDetail(){const x=system.modules.find(x=>x.id===get('module').value);get('module-detail').textContent=x.name+' — '+x.source;}get('module').addEventListener('change',moduleDetail);moduleDetail();
function search(){get('files').replaceChildren();const q=get('search').value.toLowerCase();inventory.filter(x=>x.path.toLowerCase().includes(q)).slice(0,80).forEach(x=>{const li=document.createElement('li'),a=document.createElement('a');a.href='../../../'+x.path;a.textContent=x.path;li.append(a);get('files').append(li);});}get('search').addEventListener('input',search);search();
const phases={'124':'Completed: explicit allowance, matched verified samples, uncertainty-aware choice and abstention. Count decision and completion-check cost.','125':'Next: evaluate cost prediction, late verification and recovery; measure useful opportunities missed by abstention before expanding transfer.','126':'Then: moving shells, reversals and changing supports, with actual contact and bounded recovery evidence.'};
function phase(){get('phase-detail').textContent=phases[get('phase').value];}get('phase').addEventListener('change',phase);phase();
