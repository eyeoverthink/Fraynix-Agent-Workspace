const el=id=>document.getElementById('observer-'+id);
export class ObserverPanel {
  constructor({call,lab,requireSaved,notice}){
    Object.assign(this,{call,lab,requireSaved,notice});this.packet=null;this.busy=false;this.replaying=false;
    el('open').onclick=()=>this.action(async()=>{this.requireSaved();if(!lab.scene)throw Error('Open a scene first');await call('open',{sceneId:lab.scene.id});});
    el('new').onclick=()=>this.action(async()=>{this.requireSaved();if(lab.scene)await call('open',{sceneId:lab.scene.id,newObserver:true});});
    el('orbit').onclick=()=>{lab.observerPacket=null;lab.resize();this.replaying=true;el('status').textContent='Orbit view · observer controls paused; use Live observer to return';this.controls();};
    el('live').onclick=()=>this.action(()=>this.refresh());
    el('capture').onclick=()=>this.action(async()=>{this.requireSaved();await call('capture',{id:this.packet.observer.id});await this.history();});
    el('focus').onclick=()=>this.act('focus',{entityId:el('entities').value});
    el('inspect').onclick=()=>this.act('inspect',{entityId:el('entities').value});
    el('fov').onchange=()=>this.act('fov',{fovDeg:Number(el('fov').value)});
    el('replay').onclick=()=>this.action(()=>call('replay',{id:el('history').value}));
    for(const button of document.querySelectorAll('[data-observer-move]'))button.onclick=()=>this.act('move',JSON.parse(button.dataset.observerMove));
    const stage=document.getElementById('scene-stage');
    stage.addEventListener('keydown',event=>{
      if(event.ctrlKey||event.altKey||event.metaKey||!lab.observerPacket)return;
      const moves={w:{forward:.25},s:{forward:-.25},a:{right:-.25},d:{right:.25},r:{up:.25},f:{up:-.25}};
      const turns={j:{yawDeg:-10},l:{yawDeg:10},i:{pitchDeg:5},k:{pitchDeg:-5}},key=event.key.toLowerCase();
      if(!moves[key]&&!turns[key])return;event.preventDefault();if(this.busy||this.replaying||!document.getElementById('stop').disabled)return;
      this.act(moves[key]?'move':'look',moves[key]||turns[key]);
    });
    lab.observerDrag=(yaw,pitch)=>this.act('look',{yawDeg:Math.max(-90,Math.min(90,yaw)),pitchDeg:Math.max(-45,Math.min(45,pitch))});
    setInterval(()=>{if(!lab.exporting&&lab.visible&&lab.observerPacket&&!this.busy&&!this.replaying&&document.getElementById('stop').disabled)this.action(()=>this.refresh());},500);
  }
  async action(fn){if(this.busy)return;this.busy=true;this.controls();try{await fn();}catch(error){this.notice(error.message);}finally{this.busy=false;this.controls();}}
  act(action,args){return this.action(async()=>{this.requireSaved();if(!this.packet||this.replaying)throw Error('Return to the live observer first');await this.call('action',{id:this.packet.observer.id,observationId:this.packet.id,action,...args});});}
  async refresh(){if(!this.packet)return;await this.call('observe',{id:this.packet.observer.id});}
  sceneChanged(){
    if(this.packet&&this.packet.world.sceneId!==this.lab.scene?.id){
      this.packet=null;this.replaying=false;el('body').hidden=true;
      el('status').textContent='Open a first-person observer for this scene';
      el('history').replaceChildren();el('image').hidden=true;el('frame-hash').textContent='';
    }
    this.controls();
  }
  show(packet){
    const changed=this.packet?.observer.id!==packet.observer.id;this.packet=packet;this.replaying=packet.readOnly;this.lab.observerPacket=packet;this.lab.paint(packet.world.objects);this.lab.paintGrip(packet.world.grip);
    document.getElementById('scene-settings').open=false;el('body').hidden=false;
    const pose=packet.observer.pose,w=packet.world;el('status').textContent=(this.replaying?'Recorded view · read only':'Live free camera')+' · pose v'+packet.observer.version+' · x/y/z '+pose.position.map(n=>n.toFixed(3)).join(' / ')+' m · yaw '+pose.yawDeg.toFixed(1)+'° · pitch '+pose.pitchDeg.toFixed(1)+'° · '+w.source+' · scene v'+w.sceneVersion+' · tick '+w.tick;
    el('fov').value=String(pose.fovDeg);const previous=el('entities').value;el('entities').replaceChildren();
    for(const entity of packet.visibleEntities){const option=document.createElement('option');option.value=entity.id;option.textContent=entity.label+' · '+entity.distanceM.toFixed(2)+' m';el('entities').append(option);}
    if(packet.visibleEntities.some(e=>e.id===previous))el('entities').value=previous;else {const block=packet.visibleEntities.find(e=>e.bodyType==='dynamic');if(block)el('entities').value=block.id;}
    if(!packet.visibleEntities.length){const option=document.createElement('option');option.value='';option.textContent='No sampled visible entities';el('entities').append(option);}
    const selected=packet.visibleEntities.find(e=>e.id===packet.observer.selectedEntityId);
    el('detail').textContent=selected?JSON.stringify({id:selected.id,label:selected.label,position:selected.position,size:selected.size,bodyType:selected.bodyType},null,2):'Visible entities are computed from the actual box geometry. W/A/S/D move; R/F change height; I/J/K/L look. Click the viewport to focus keyboard controls.';
    this.controls();this.lab.resize();if(changed)this.history().catch(e=>this.notice(e.message));
  }
  async history(){
    if(!this.packet)return;const value=await this.call('history',{id:this.packet.observer.id});el('history').replaceChildren();
    for(const frame of value.captures.toReversed()){const option=document.createElement('option');option.value=frame.id;option.textContent='Pose v'+frame.observerVersion+' · '+frame.createdAt;el('history').append(option);}
    el('replay').disabled=this.busy||!value.captures.length;return value;
  }
  captured(value){el('image').src='data:image/png;base64,'+value.pngBase64;el('image').hidden=false;el('frame-hash').textContent='Saved native frame · SHA-256 '+value.capture.pngSha256;this.history().catch(e=>this.notice(e.message));}
  controls(){
    const agent=!document.getElementById('stop').disabled,locked=agent||this.busy;
    for(const button of el('panel').querySelectorAll('button,input,select'))button.disabled=locked;
    el('open').disabled=locked||!this.lab.scene;
    for(const id of ['capture','focus','inspect','fov'])el(id).disabled=locked||!this.packet||this.replaying;
    el('live').disabled=locked||!this.packet;el('replay').disabled=locked||!el('history').value;
    el('focus').disabled||=!el('entities').value;el('inspect').disabled||=!el('entities').value;
    for(const button of document.querySelectorAll('[data-observer-move]'))button.disabled=locked||!this.packet||this.replaying;
  }
}
