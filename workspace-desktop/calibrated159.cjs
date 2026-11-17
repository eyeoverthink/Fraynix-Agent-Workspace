'use strict';
// Read the actual DOM and exercise each chart control. No expected geometry is sent by Java.
async function auditCalibrated159(canvas,effect,recordTest){
 const steps=[{selector:'#rows tr',expectCount:effect.rowCount},{selector:'#mode option',expectCount:3},{selector:'#plot',expectCount:1}],checks=await canvas.test(steps);
 if(checks.some(c=>!c.passed))throw Error('Calibrated report did not initialize');
 if(!(await recordTest({id:effect.report.id,version:effect.report.version,steps,checks})).passed)throw Error('Calibrated report checks not retained');
 const states=[];
 for(const mode of ['all','bounds','actual']){
  await canvas.isolated(`(()=>{const e=document.querySelector('#mode');e.value=${JSON.stringify(mode)};e.dispatchEvent(new Event('change'));return true})()`);
  states.push(await canvas.isolated(`(()=>{const v=e=>{const r=e.getBoundingClientRect(),s=getComputedStyle(e);return r.width>0&&r.height>0&&s.display!=='none'&&s.visibility!=='hidden'&&Number(s.opacity)>0};return {mode:document.querySelector('#mode').value,visible:v(document.querySelector('#plot')),rectangles:[...document.querySelectorAll('#plot rect')].map(e=>({x:e.x.baseVal.value,y:e.y.baseVal.value,width:e.width.baseVal.value,height:e.height.baseVal.value,visible:v(e)&&getComputedStyle(e).stroke!=='none'&&Number(getComputedStyle(e).strokeOpacity)>0&&parseFloat(getComputedStyle(e).strokeWidth)>0})),circles:[...document.querySelectorAll('#plot circle')].map(e=>({cx:e.cx.baseVal.value,cy:e.cy.baseVal.value,visible:v(e)&&getComputedStyle(e).fill!=='none'&&Number(getComputedStyle(e).fillOpacity)>0}))}})()`));
 }
 const content=await canvas.isolated(`({rows:[...document.querySelectorAll('#rows tr')].map(r=>[...r.querySelectorAll('td')].map(c=>c.textContent)),...Object.fromEntries(['limits','source','checks','world','axes'].map(k=>[k,document.getElementById(k).textContent]))})`);
 await canvas.isolated(`(()=>{const e=document.querySelector('#mode');e.value='all';e.dispatchEvent(new Event('change'));return true})()`);
 return {id:effect.report.id,version:effect.report.version,...content,states};
}
module.exports={auditCalibrated159};
