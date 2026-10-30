const el=id=>document.getElementById('body-'+id);
export class BodyPanel {
 constructor({call,lab,notice,requireSaved}){
  Object.assign(this,{call,lab,notice,requireSaved});this.packet=null;this.busy=false;
  el('grab').onclick=()=>this.perform('grab',{entityId:document.getElementById('observer-entities').value});
  el('lift').onclick=()=>this.offset([0,.5,0]);el('release').onclick=()=>this.perform('release');el('cancel').onclick=()=>this.perform('cancel');
  el('move').onclick=()=>this.perform('move',{target:['x','y','z'].map(k=>Number(el(k).value))});
  el('current').onclick=()=>this.fill();lab.bodyProgress=value=>{
   if(value.bodyProgress){el('status').textContent='Native '+value.bodyProgress.action+' · '+value.bodyProgress.completedTicks+' / '+value.bodyProgress.totalTicks+' solver ticks';}
  };
  document.getElementById('observer-entities').addEventListener('change',()=>this.controls());
 }
 observe(packet){
  const old=this.packet?.world.grip?.id;this.packet=packet;
  if(packet.world.grip?.held&&packet.world.grip.id!==old)this.fill();
  if(!this.busy){const grip=packet.world.grip;el('status').textContent=packet.readOnly?'Historical view · return to Live observer to manipulate':grip?.held?'Holding actual body · '+grip.kind+' · 300 N equivalent per-axis impulse clamp':'No body held · grab a visible dynamic Block; Reset after a settled run';}
  this.controls();
 }
 fill(){const block=this.packet?.world.knownEntities?.find(o=>o.label==='Block')||this.packet?.world.objects?.find(o=>o.label==='Block');if(block)for(const [i,k]of ['x','y','z'].entries())el(k).value=String(block.position[i]);}
 async offset(delta){const block=(this.packet?.world.objects||this.packet?.world.knownEntities)?.find(o=>o.label==='Block');if(block)return this.perform('move',{target:block.position.map((n,i)=>n+delta[i])});}
 async perform(action,extra={}){
  if(this.busy&&action!=='cancel')return;this.busy=true;this.controls();
  try{
   this.requireSaved();const packet=this.packet;if(!packet||packet.readOnly)throw Error('Open the live first-person observer');
   const args={id:packet.observer.id,...extra};
   if(action!=='cancel')args.observationId=packet.id;
   if(action!=='grab'){args.runId=packet.world.runId;if(action!=='cancel')args.gripId=packet.world.grip?.id;}
   const result=await this.call(action,args);this.observe(result);el('status').textContent='Native '+result.receipt.action+' · '+result.receipt.outcome+(result.receipt.targetErrorM!==undefined?' · actual target error '+result.receipt.targetErrorM.toFixed(4)+' m':'')+' · tick '+result.receipt.endTick;
   this.last=result.receipt;if(result.receipt.action==='release')await this.lab.history(result.receipt.runId);
  }catch(error){this.notice(error.message);}finally{this.busy=false;this.controls();}
 }
 controls(){
  const packet=this.packet,agent=!document.getElementById('stop').disabled,live=packet&&!packet.readOnly,grip=packet?.world.grip,owned=live&&grip?.held&&grip.ownerId===packet.observer.id;
  const selected=packet?.visibleEntities?.find(e=>e.id===document.getElementById('observer-entities').value),locked=agent||this.busy||this.lab.exporting;
  el('grab').disabled=locked||!live||grip?.held||selected?.bodyType!=='dynamic'||packet.world.source!=='authored-geometry';
  for(const id of ['lift','move','current','x','y','z','release'])el(id).disabled=locked||!owned;
  el('cancel').disabled=agent||!live||(!owned&&!this.busy);
 }
}
