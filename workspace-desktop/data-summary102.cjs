'use strict';
const {auditData101}=require('./data-creation101.cjs');
async function auditSummary102(canvas,effect){const summaries102=[];const result=await auditData101(canvas,effect,async()=>summaries102.push(await canvas.isolated(String.raw`(()=>{
 const one=s=>{const a=document.querySelectorAll(s);if(a.length!==1)throw Error('Expected one '+s);return a[0]};
 const visible=e=>{const r=e.getBoundingClientRect();if(r.width<=0||r.height<=0)return false;for(let p=e;p;p=p.parentElement){const c=getComputedStyle(p);if(c.display==='none'||c.visibility==='hidden'||c.visibility==='collapse'||Number(c.opacity)===0)return false;}return true;};
 const values={},elements=[];for(const k of ['samples','duration','travel','peak','gaps','speeds']){const e=one('#summary-'+k);elements.push(e);values[k]=e.textContent.trim();}
 const svg=one('svg#motion-plot'),line=one('polyline#motion-trace'),axis=s=>{const a=document.querySelectorAll(s);return a.length===1?a[0]:null;},time=axis('#time-axis'),distance=axis('#distance-axis');if(line.parentElement!==svg)throw Error('Trace must be directly within its declared SVG');if(line.points.numberOfItems>1000)throw Error('Trace point budget exceeded');
 const v=svg.viewBox.baseVal,c=getComputedStyle(line);let trace=c.stroke!=='none'&&Number(c.strokeOpacity)>0&&parseFloat(c.strokeWidth)>0;for(let p=line;p;p=p.parentElement){const s=getComputedStyle(p);trace=trace&&s.display!=='none'&&s.visibility!=='hidden'&&s.visibility!=='collapse'&&Number(s.opacity)>0;}
 if(line.hasAttribute('transform')||svg.hasAttribute('transform')||c.transform!=='none'||getComputedStyle(svg).transform!=='none'||c.stroke==='transparent'||/rgba\([^)]*,\s*0\s*\)$/.test(c.stroke))trace=false;
 return {values,points:Array.from({length:line.points.numberOfItems},(_,i)=>{const p=line.points.getItem(i);return [p.x,p.y]}),viewBox:[v.x,v.y,v.width,v.height],visible:trace&&[svg,time,distance,...elements].every(e=>e&&visible(e)),timeAxis:time?.textContent.trim()||'',distanceAxis:distance?.textContent.trim()||''};
})()`)));return {...result,summaries102};}
module.exports={auditSummary102};
