'use strict';
async function auditSmall161(canvas,effect,recordTest){
 const steps=[{selector:'#frame',expectCount:1},{selector:'#path',expectCount:1},{selector:'#body',expectCount:1}],checks=await canvas.test(steps);
 if(checks.some(c=>!c.passed))throw Error('Small-body report did not initialize');
 if(!(await recordTest({id:effect.report.id,version:effect.report.version,steps,checks})).passed)throw Error('Small-body report tests were not retained');
 const states=[];
 for(const index of [0,Math.floor(effect.samples/2),effect.samples-1]){
  await canvas.isolated(`(()=>{const e=document.querySelector('#frame');e.value=${index};e.dispatchEvent(new Event('input'));return true})()`);
  states.push(await canvas.isolated(`(()=>{const e=document.querySelector('#body'),p=document.querySelector('#plot'),r=p.getBoundingClientRect(),s=getComputedStyle(e),t=getComputedStyle(p);return {index:Number(document.querySelector('#frame').value),pose:document.querySelector('#pose').textContent,x:e.cx.baseVal.value,y:e.cy.baseVal.value,visible:r.width>0&&r.height>0&&t.display!=='none'&&t.visibility==='visible'&&Number(t.opacity)>0&&s.display!=='none'&&s.visibility==='visible'&&Number(s.opacity)>0&&s.fill!=='none'&&Number(s.fillOpacity)>0}})()`));
 }
 const data=await canvas.isolated(`(()=>{const e=document.querySelector('#path'),s=getComputedStyle(e);if(s.display==='none'||s.visibility!=='visible'||Number(s.opacity)===0||s.stroke==='none'||Number(s.strokeOpacity)===0||parseFloat(s.strokeWidth)<=0)throw Error('Actual trajectory hidden');return {summary:document.querySelector('#result').textContent,scope:document.querySelector('#scope').textContent,source:document.querySelector('#source').textContent,path:Array.from({length:e.points.numberOfItems},(_,i)=>{const p=e.points.getItem(i);return [p.x,p.y]})}})()`);
 return {...data,states};
}
module.exports={auditSmall161};
