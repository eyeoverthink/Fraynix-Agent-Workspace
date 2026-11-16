'use strict';

// Used by the real source recorder and its deterministic timing checks. No synthetic clock in capture.
function timing150(phase,phaseSamples,elapsedMs) {
  if(!Number.isInteger(phaseSamples)||phaseSamples<0||!Number.isFinite(elapsedMs)||elapsedMs<0)throw Error('Invalid recorder clock');
  const counts={reference:34,covered:18,returned:24},limits={covering:12000,revealing:15000};
  if(Object.hasOwn(counts,phase))return {kind:'samples',remaining:Math.max(0,counts[phase]-phaseSamples),ready:phaseSamples>=counts[phase],expired:false};
  if(Object.hasOwn(limits,phase))return {kind:'action',remaining:Math.max(0,Math.ceil((limits[phase]-elapsedMs)/1000)),ready:false,expired:elapsedMs>=limits[phase],limitMs:limits[phase]};
  if(['idle','opening','preview','finished','aborted','limit','capture-error','timing-expired'].includes(phase))return {kind:'idle',remaining:0,ready:false,expired:false};
  throw Error('Unknown recorder phase');
}
if(typeof module==='object' && module.exports)module.exports={timing150};else window.recorderTiming150=timing150;
