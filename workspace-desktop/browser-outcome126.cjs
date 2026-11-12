'use strict';
// This check is invoked by Java's finish gate. It is not an LLM-selectable script or a page-supplied assertion.
async function observeOutcome126(surface,claim){
 const contract=claim.extra.browserContract126;
 if(surface.kind!=='browser'||!contract||typeof contract.url!=='string'||!Array.isArray(contract.checks)||contract.checks.length<1||contract.checks.length>8)throw Error('Frozen browser criteria required');
 for(const c of contract.checks)if(typeof c.selector!=='string'||!c.selector.trim()||c.selector.length>200||typeof c.text!=='string'||c.text.length>500||Object.keys(c).some(k=>!['selector','text'].includes(k)))throw Error('Invalid frozen browser criteria');
 // One isolated DOM snapshot: no actions, network, page functions or changing model-authored tests.
 const actual=await surface.isolated(`(()=>{const selectors=${JSON.stringify(contract.checks.map(c=>c.selector))};return {url:location.href,checks:selectors.map(selector=>{const es=document.querySelectorAll(selector),e=es[0],r=e?.getBoundingClientRect(),s=e?getComputedStyle(e):null;return {selector,count:es.length,text:String(e?.innerText??e?.textContent??'').trim().slice(0,501),visible:!!r&&r.width>0&&r.height>0&&s.visibility!=='hidden'&&s.display!=='none'&&s.opacity!=='0'};})};})()`);
 if(surface.view.webContents.getURL()!==actual.url)throw Error('Browser navigated during its outcome check; observe again');
 return {schema:'browser-outcome-126-v1',taskId:claim.taskId,generation:claim.generation,effectId:claim.id,contractSha256:claim.extra.contractSha256,url:actual.url,observedAt:new Date().toISOString(),checks:actual.checks};
}
module.exports={observeOutcome126};
