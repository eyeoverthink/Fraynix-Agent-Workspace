"use strict";
/** Only transport for the persistent Java creation worker; no model prompts or decisions in this process. */
function createCreation({api,activity}){
 const pending=new Map();
 async function generate(effect){const job=await api('/api/creation044/start',{goalId:effect.goalId,id:effect.id,nonce:effect.nonce});pending.set(effect.goalId,job.id);activity('creation','Java owns the frozen model generation',{goalId:effect.goalId,requestId:job.id});return{javaOwned:true,id:job.id};}
 async function cancel(goalId){pending.delete(goalId);return api('/api/creation044/cancel',{goalId});}
 return{generate,cancel,stop(){pending.clear();},pending:()=>pending.size};
}
function creationSummary(g){return `Creation ${g.status}: ${g.reason}. Checks and source versions are owned by Java.`;}
module.exports={createCreation,creationSummary};
