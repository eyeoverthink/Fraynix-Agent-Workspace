'use strict';
// Startup limits, never a permission to weaken native or evidence checks.
function budget(env=process.env){
 const integer=(name,min,max,fallback)=>{const raw=env[name];if(raw===undefined)return fallback;if(!/^[1-9][0-9]*$/.test(raw)||!Number.isSafeInteger(Number(raw))||Number(raw)<min||Number(raw)>max)throw Error(`${name} must be an integer from ${min} to ${max}`);return Number(raw);};
 const heapMiB=integer('FRAYNIX_WORKSPACE_JAVA_HEAP_MIB',256,768,768),processors=integer('FRAYNIX_WORKSPACE_JAVA_PROCESSORS',1,4,null);
 return {heapMiB,processors,explicit:env.FRAYNIX_WORKSPACE_JAVA_HEAP_MIB!==undefined||processors!==null,args:[`-Xmx${heapMiB}m`,...(processors===null?[]:[`-XX:ActiveProcessorCount=${processors}`])]};
}
function checkEffective(requested,actual){
 if(!requested.explicit)return;
 const bytes=requested.heapMiB*1024*1024;
 const configured=actual.configuredMaxHeapBytes??actual.profile.maxHeap;
 if(Math.abs(configured-bytes)>1024*1024||actual.profile.maxHeap>bytes+1024*1024||(requested.processors!==null&&actual.profile.cpus!==requested.processors))throw Error('The Java owner has different resource limits. Stop that owner before changing its startup budget; existing work was preserved.');
}
module.exports={budget,checkEffective};
