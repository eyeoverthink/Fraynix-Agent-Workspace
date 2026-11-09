'use strict';

// Read the real isolated canvas after each action. Generated code cannot submit proof.
async function auditData101(canvas,effect,extra){
  const ready=await canvas.test([{selector:'#row-count',expectText:String(effect.rowCount)}]);
  if(ready.some(x=>!x.passed))throw Error('Data explorer did not initialize before independent audit');
  const frames=[];
  const snapshot=async()=>{const frame=await canvas.isolated(`(()=>{
    const one=s=>{const all=document.querySelectorAll(s);if(all.length!==1)throw Error('Expected one '+s);return all[0];};
    const table=one('table#data-table'),head=[...table.querySelectorAll('thead th')],rows=[...table.querySelectorAll('tbody tr')];
    if(rows.length>1000||head.length>3)throw Error('Audit table exceeds declared bounds');
    const visible=e=>{const r=e.getBoundingClientRect();if(r.width<=0||r.height<=0)return false;for(let p=e;p;p=p.parentElement){const c=getComputedStyle(p);if(c.display==='none'||c.visibility==='hidden'||c.visibility==='collapse'||Number(c.opacity)===0)return false;}return true;};
    const filter=one('#filter'),count=one('#row-count'),reset=one('#reset'),scope=one('#scope'),quality=one('#data-quality');
    let shown=[table,...head,filter,count,reset,scope,quality].every(visible);
    const cells=rows.map(r=>{const c=[...r.querySelectorAll('td')];if(c.length!==3)throw Error('Expected three source cells');shown=shown&&visible(r)&&c.every(visible);return c.map(e=>e.textContent.trim());});
    return {rows:cells,headers:head.map(e=>e.textContent.trim()),filter:String(filter.value),count:count.textContent.trim(),scope:scope.textContent.trim(),quality:quality.textContent.trim(),visible:shown};
  })()`);if(extra)await extra();return frame;};
  frames.push(await snapshot());
  for(const value of [effect.filter,'__no_match_101__']){
    const checks=await canvas.test([{selector:'#filter',action:'fill',value,expectValue:value}]);if(checks.some(x=>!x.passed))throw Error('Actual filter action failed');frames.push(await snapshot());
  }
  const reset=await canvas.test([{selector:'#reset',action:'click'},{selector:'#filter',expectValue:''}]);if(reset.some(x=>!x.passed))throw Error('Actual reset action failed');frames.push(await snapshot());
  return {id:effect.report.id,version:effect.report.version,frames};
}
module.exports={auditData101};
