'use strict';
const {AsyncLocalStorage}=require('node:async_hooks'),{randomUUID}=require('node:crypto');
function createReportCosts164({enabled=process.env.FRAYNIX_WORKSPACE_COSTS164!=='off',clock=()=>process.hrtime.bigint()}={}){
 const context=new AsyncLocalStorage(),totals={},recent=[],epoch=randomUUID();let sequence=0,detached=0;
 async function measure(kind,fn){
  if(!['canvas.navigate','canvas.test','canvas.isolated'].includes(kind))throw Error('Unknown report timing category');
  if(!enabled)return fn();
  const parent=context.getStore(),frame={start:clock(),children:[],closed:false};let passed=false;
  return context.run(frame,async()=>{try{const result=await fn();passed=true;return result;}finally{
   const end=clock();frame.closed=true;let covered=0n,until=frame.start;
   for(const [a,b]of frame.children.sort((x,y)=>x[0]<y[0]?-1:1)){const start=a>until?a:until,stop=b<end?b:end;if(stop>start)covered+=stop-start;if(stop>until)until=stop;}
   const duration=end-frame.start,exclusive=duration-covered;if(parent){if(parent.closed)detached++;else parent.children.push([frame.start,end]);}
   const ms=Number(duration)/1e6,ex=Number(exclusive)/1e6,t=totals[kind]||={count:0,failed:0,inclusiveMs:0,exclusiveMs:0,maxMs:0};t.count++;if(!passed)t.failed++;t.inclusiveMs+=ms;t.exclusiveMs+=ex;t.maxMs=Math.max(t.maxMs,ms);
   recent.push({sequence:++sequence,kind,passed,inclusiveMs:ms,exclusiveMs:ex});if(recent.length>128)recent.shift();
  }});
 }
 function attach(surface){for(const key of ['navigate','test','isolated']){const original=surface[key];if(typeof original!=='function')throw Error('Report surface method missing: '+key);surface[key]=function(...args){return measure('canvas.'+key,()=>original.apply(this,args));};}return surface;}
 const status=()=>JSON.parse(JSON.stringify({enabled,epoch,operations:sequence,dropped:Math.max(0,sequence-128),detached,totals,recent,scope:'Diagnostic actual canvas navigation, assertion and observation spans. Nested inclusive spans overlap; exclusive spans subtract the union of completed child intervals. Detached children are counted separately. Browser and Java wall spans can overlap; do not sum them as total task time. No acceptance result is inferred from timing.'}));
 return {measure,attach,status};
}
module.exports={createReportCosts164};
