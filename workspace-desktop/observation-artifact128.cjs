'use strict';
// Host-owned DOM observations. The page cannot submit its own successful audit.
async function auditObservation128(canvas,effect,recordTest){
 const steps=[{selector:'#rows tr',expectCount:effect.rowCount},{selector:'#axis option',expectCount:2},{selector:'#frame',expectCount:1}],checks=await canvas.test(steps);
 if(checks.some(c=>!c.passed))throw Error('Observation explorer did not initialize');
 const receipt=await recordTest({id:effect.report.id,version:effect.report.version,steps,checks});if(!receipt.passed)throw Error('Actual report checks were not retained');
 const rows=await canvas.isolated(`Array.from(document.querySelectorAll('#rows tr'),r=>Array.from(r.querySelectorAll('td'),c=>c.textContent))`);
 const frames=[];
 for(const [axis,index] of [[0,0],[0,effect.rowCount-1],[1,Math.floor(effect.rowCount/2)],[1,effect.rowCount-1]]){
  await canvas.isolated(`(()=>{const axis=document.querySelector('#axis'),frame=document.querySelector('#frame');axis.value=${axis};axis.dispatchEvent(new Event('change'));frame.value=${index};frame.dispatchEvent(new Event('input'));return true})()`);
  frames.push(await canvas.isolated(`(()=>{const chart=document.querySelector('#chart'),r=chart.getBoundingClientRect(),s=getComputedStyle(chart);return {axis:Number(document.querySelector('#axis').value),index:Number(document.querySelector('#frame').value),selection:document.querySelector('#selection').textContent,visible:r.width>0&&r.height>0&&s.display!=='none'&&s.visibility!=='hidden'&&Number(s.opacity)>0,points:Array.from(chart.querySelectorAll('circle'),e=>{const c=getComputedStyle(e),b=e.getBoundingClientRect();return {series:e.getAttribute('data-series'),ordinal:Number(e.getAttribute('data-ordinal')),x:e.cx.baseVal.value,y:e.cy.baseVal.value,visible:b.width>0&&b.height>0&&c.display!=='none'&&c.visibility!=='hidden'&&Number(c.opacity)>0&&c.fill!=='none'&&Number(c.fillOpacity)>0}})}})()`));
 }
 const labels=await canvas.isolated(`({summary:Array.from(document.querySelectorAll('#metrics strong'),e=>e.textContent),future:document.querySelector('#future').textContent,source:document.querySelector('#source').textContent})`);
 return {id:effect.report.id,version:effect.report.version,rows,frames,...labels};
}
module.exports={auditObservation128};
