'use strict';
// Repair hints only. Page-originated diagnostics never establish a passing verdict.
const PREFIX='FRAYNIX_DOM_DIAGNOSTIC102:';
const SOURCE=`(()=>{
  const original=Document.prototype.getElementById,seen=new Set(),log=console.info.bind(console);
  Document.prototype.getElementById=function(...args){
    const result=Reflect.apply(original,this,args),id=args[0];
    if(result===null&&typeof id==='string'&&id.length<=160&&seen.size<12&&!seen.has(id)){
      seen.add(id);
      const matches=Array.from(this.querySelectorAll('[id]')).map(e=>e.id).filter(x=>x.endsWith('-'+id)).slice(0,6);
      log(${JSON.stringify(PREFIX)}+JSON.stringify({missingId:id,suffixMatches:matches,scope:'Observed null lookup; possible related IDs, not an automatic fix or verification result.'}));
    }
    return result;
  };
})();`;
async function install(surface){
  if(surface.kind!=='canvas')return;
  const wc=surface.view.webContents,debug=wc.debugger;
  if(surface.diagnosticsReady102&&debug.isAttached())return;
  if(!wc.getURL())await wc.loadURL('about:blank');
  if(!debug.isAttached())debug.attach('1.3');
  await debug.sendCommand('Page.enable');
  await debug.sendCommand('Page.addScriptToEvaluateOnNewDocument',{source:SOURCE});
  surface.diagnosticsReady102=true;
}
function receive(surface,message){
  if(surface.kind!=='canvas'||!message.startsWith(PREFIX))return false;
  if(message.length>4096)return true;
  try{
    const value=JSON.parse(message.slice(PREFIX.length));
    if(typeof value.missingId==='string'&&value.missingId.length<=160&&Array.isArray(value.suffixMatches)&&surface.missingIds102.length<12){
      surface.missingIds102.push({missingId:value.missingId,suffixMatches:value.suffixMatches.filter(x=>typeof x==='string'&&x.length<=160).slice(0,6),scope:'Untrusted page diagnostic for repair; never proof of success.'});
    }
  }catch{/* Malformed page diagnostics do not affect outcome checks. */}
  return true;
}
module.exports={install,receive,PREFIX};
