'use strict';
const {observeOutcome126}=require('./browser-outcome126.cjs');
async function inspectBrowser158(browser,effect){
 const contract=effect.browserContract126;
 if(!effect.context158?.uncertainty||effect.context158.physicalActionAuthorized!==false||effect.world158?.physicalPlacement!=='refused')throw Error('Verified observation limits required');
 await browser.navigate(contract.url);
 return observeOutcome126(browser,{taskId:effect.workflowId,generation:effect.generation,id:effect.id,extra:{browserContract126:contract,contractSha256:effect.contractSha256}});
}
async function auditReport158(canvas,effect,recordTest){
 const steps=[{selector:'#rows tr',expectCount:effect.context158.rows.length},{selector:'#filter option',expectCount:3},{selector:'#chart',expectCount:1}],checks=await canvas.test(steps);
 if(checks.some(c=>!c.passed))throw Error('Observation report did not initialize');const receipt=await recordTest({id:effect.report.id,version:effect.report.version,steps,checks});if(!receipt.passed)throw Error('Independent report checks were not retained');
 const filters=[];for(const filter of ['all','accepted','withheld']){await canvas.isolated(`(()=>{const e=document.querySelector('#filter');e.value=${JSON.stringify(filter)};e.dispatchEvent(new Event('change'));return true})()`);filters.push(await canvas.isolated(`({filter:document.querySelector('#filter').value,rows:Array.from(document.querySelectorAll('#rows tr'),r=>Array.from(r.querySelectorAll('td'),c=>c.textContent))})`));}
 const frames=[];for(const index of effect.indexes){await canvas.isolated(`(()=>{const e=document.querySelector('#frame');e.value=${index};e.dispatchEvent(new Event('input'));return true})()`);frames.push(await canvas.isolated(`(()=>{const v=e=>{const r=e.getBoundingClientRect(),s=getComputedStyle(e);return r.width>0&&r.height>0&&s.display!=='none'&&s.visibility!=='hidden'&&Number(s.opacity)>0};return {index:Number(document.querySelector('#frame').value),selection:document.querySelector('#selection').textContent,visible:v(document.querySelector('#chart')),rectangles:Array.from(document.querySelectorAll('#chart rect'),e=>({x:e.x.baseVal.value,y:e.y.baseVal.value,width:e.width.baseVal.value,height:e.height.baseVal.value,visible:v(e)&&getComputedStyle(e).stroke!=='none'}))}})()`));}
 const text=await canvas.isolated(`({labels:Object.fromEntries(['limits','world','decision','browser-url','source'].map(k=>[k,document.getElementById(k).textContent])),summary:Array.from(document.querySelectorAll('#metrics strong'),e=>e.textContent),browser:Array.from(document.querySelectorAll('#browser-checks li'),e=>e.textContent)})`);
 await canvas.isolated(`(()=>{const f=document.querySelector('#filter');f.value='all';f.dispatchEvent(new Event('change'));return true})()`);
 return {id:effect.report.id,version:effect.report.version,filters,frames,...text};
}
module.exports={inspectBrowser158,auditReport158};
