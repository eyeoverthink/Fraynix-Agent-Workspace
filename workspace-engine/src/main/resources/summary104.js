'use strict';
// Authored implementation shipped by Java. No expected result constants or network dependencies.
// Decimal half-up formatting matches the independent Java six-decimal display contract.
function fixed6(value){
 if(!Number.isFinite(value)||value<0)throw Error('Non-finite or negative summary value');
 const [mantissa,exponent='0']=String(value).split('e'),parts=mantissa.split('.');
 const digits=parts.join(''),shift=6+Number(exponent)-(parts[1]||'').length;
 let whole=BigInt(digits);if(shift>=0)whole*=10n**BigInt(shift);else{const scale=10n**BigInt(-shift);whole=(whole+scale/2n)/scale;}
 const text=whole.toString().padStart(7,'0');return text.slice(0,-6)+'.'+text.slice(-6);
}
async function start(){
 const response=await fetch('inputs/source.json');if(!response.ok)throw Error('Fresh input could not be read');const source=await response.json();
 if(JSON.stringify(source.headers)!==JSON.stringify(['source_row','time_s','displacement_m'])||!Array.isArray(source.rows)||source.rows.length<2||source.rows.length>1000)throw Error('Unsupported source schema');
 const c=source.criteria104;if(!c||!Number.isFinite(c.maxGapS)||c.maxGapS<=0||!Number.isFinite(c.speedLimitMps)||c.speedLimitMps<=0||!Number.isInteger(c.skippedSourceRows)||c.skippedSourceRows<0)throw Error('Fresh criteria missing or invalid');
 const rows=source.rows,ts=rows.map(r=>Number(r.time_s)),ds=rows.map(r=>Number(r.displacement_m));
 if([...ts,...ds].some(v=>!Number.isFinite(v)))throw Error('Non-finite source');
 let travel=0,peak=0,gaps=0,speeds=0;
 for(let i=1;i<rows.length;i++){const dt=ts[i]-ts[i-1],dx=ds[i]-ds[i-1];if(dt<=0)throw Error('Time must increase');const speed=Math.abs(dx/dt);travel+=Math.abs(dx);peak=Math.max(peak,speed);if(dt>c.maxGapS)gaps++;if(speed>c.speedLimitMps)speeds++;}
 const values={samples:String(rows.length),duration:fixed6(ts.at(-1)-ts[0]),travel:fixed6(travel),peak:fixed6(peak),gaps:String(gaps),speeds:String(speeds)};
 for(const [name,value]of Object.entries(values))document.getElementById('summary-'+name).textContent=value;
 document.getElementById('title').textContent=source.title;
 const flagged=gaps>0||speeds>0||c.skippedSourceRows>0;
 document.getElementById('data-quality').textContent=flagged?'Quality flags retained':'Within declared thresholds';
 document.querySelector('.quality').classList.toggle('flagged',flagged);
 document.getElementById('criteria').textContent='Gap limit '+c.maxGapS+' s · speed limit '+c.speedLimitMps+' m/s · skipped source rows '+c.skippedSourceRows+'.';
 const lo=Math.min(0,...ds),hi=Math.max(0,...ds),span=Math.max(1e-12,hi-lo),duration=ts.at(-1)-ts[0];
 document.getElementById('motion-trace').setAttribute('points',rows.map((r,i)=>(40+560*(ts[i]-ts[0])/duration)+','+(200-160*(ds[i]-lo)/span)).join(' '));
 const heading=document.querySelector('#data-table thead tr');for(const key of source.headers){const th=document.createElement('th');th.textContent=key;heading.append(th);}
 const filter=document.getElementById('filter'),tbody=document.querySelector('#data-table tbody');
 function render(){const selected=rows.filter(r=>filter.value===''||r.source_row===filter.value);tbody.replaceChildren();for(const row of selected){const tr=document.createElement('tr');for(const key of source.headers){const td=document.createElement('td');td.textContent=row[key];tr.append(td);}tbody.append(tr);}document.getElementById('row-count').textContent=String(selected.length);}
 filter.addEventListener('input',render);document.getElementById('reset').addEventListener('click',()=>{filter.value='';render();});render();
}
start().catch(error=>{document.getElementById('error').textContent='Summary refused: '+error.message;console.error(error);});
