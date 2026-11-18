'use strict';
async function auditAccuracy160(canvas,effect,recordTest){
 const steps=[{selector:'#rows tr',expectCount:effect.rowCount},{selector:'#mode option',expectCount:3}],checks=await canvas.test(steps);
 if(checks.some(c=>!c.passed))throw Error('Accuracy report did not initialize');
 if(!(await recordTest({id:effect.report.id,version:effect.report.version,steps,checks})).passed)throw Error('Accuracy report checks were not retained');
 const states=[];
 for(const mode of ['all','error','bound']){
  await canvas.isolated(`(()=>{const e=document.querySelector('#mode');e.value=${JSON.stringify(mode)};e.dispatchEvent(new Event('change'));return true})()`);
  states.push(await canvas.isolated(`(()=>{const e=document.querySelector('#plot'),r=e.getBoundingClientRect(),s=getComputedStyle(e);return {mode:document.querySelector('#mode').value,visible:r.width>0&&r.height>0&&s.display!=='none'&&s.visibility==='visible'&&Number(s.opacity)>0,bars:[...e.querySelectorAll('rect')].map(x=>{const p=getComputedStyle(x);if(p.display==='none'||p.visibility!=='visible'||Number(p.opacity)===0||p.fill==='none'||Number(p.fillOpacity)===0)throw Error('Accuracy chart bars are hidden');return {kind:x.dataset.kind,x:x.x.baseVal.value,y:x.y.baseVal.value,width:x.width.baseVal.value,height:x.height.baseVal.value}})}})()`));
 }
 const data=await canvas.isolated(`({rows:[...document.querySelectorAll('#rows tr')].map(r=>[...r.querySelectorAll('td')].map(c=>c.textContent)),...Object.fromEntries(['limits','summary','source'].map(k=>[k,document.getElementById(k).textContent]))})`);
 await canvas.isolated("(()=>{const e=document.querySelector('#mode');e.value='all';e.dispatchEvent(new Event('change'));return true})()");return {...data,states};
}
module.exports={auditAccuracy160};
