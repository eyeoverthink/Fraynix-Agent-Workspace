const el=id=>document.getElementById('native-file-'+id);
export class SceneFiles {
 constructor({call,lab,notice,requireSaved}){
  Object.assign(this,{call,lab,notice,requireSaved});this.pending=null;this.busy=false;
  for(const [id,command]of [['scene','scene'],['clone','clone'],['run','run'],['csv','csv'],['video','video']])el(id).onclick=()=>this.perform(command);
  el('choose').onclick=()=>el('input').click();
  el('input').onchange=async()=>{
   this.pending=null;try{const file=el('input').files[0];if(!file)return;if(file.size>64000)throw Error('Scene file exceeds 64 KB');const text=await file.text(),data=JSON.parse(text);if(data.format!=='fraynix.scene'||data.version!==1)throw Error('Choose a supported fraynix.scene version 1 JSON file');this.pending=data;el('preview').textContent='Selected '+file.name+' · '+String(data.title)+' · Import will validate geometry/units and create a separate scene.';}catch(error){this.notice(error.message);}this.controls();
  };
  el('import').onclick=()=>this.perform('import');el('reveal').onclick=()=>this.perform('reveal');
 }
 async perform(command){
  if(this.busy)return;this.busy=true;this.controls();
  try{
   this.requireSaved();const args=command==='import'?{package:this.pending}:command==='scene'||command==='clone'?{id:this.lab.scene.id}:{id:document.getElementById('scene-history').value};
   if(['run','csv','video'].includes(command)&&!args.id)throw Error('Choose a settled recorded run first');
   el('status').textContent=command==='video'?'Rendering actual recorded physics poses to WebM…':'Working…';
   const result=await this.call(command,args);
   if(result.scene){this.pending=null;el('preview').textContent='Imported with new scene/entity identities; native geometry verified.';await this.lab.open(result.scene);}
   else {const file=result.file||result;el('status').textContent=file.kind+' · '+file.bytes+' bytes · '+file.path+' · SHA-256 '+file.sha256;el('reveal').disabled=false;}
  }catch(error){el('status').textContent=error.message;this.notice(error.message);}finally{this.busy=false;this.controls();}
 }
 video(value){const bytes=Uint8Array.from(atob(value.base64),c=>c.charCodeAt(0));if(this.videoURL)URL.revokeObjectURL(this.videoURL);this.videoURL=URL.createObjectURL(new Blob([bytes],{type:value.mime}));el('video-preview').src=this.videoURL;el('video-preview').hidden=false;}
 exported(file){el('status').textContent=file.kind+' · '+file.path+' · SHA-256 '+file.sha256;this.last=file;this.controls();}
 controls(){
  const locked=this.busy||!document.getElementById('stop').disabled;
  for(const button of document.querySelectorAll('#native-file-panel button'))button.disabled=locked;
  el('scene').disabled=locked||!this.lab.scene;el('clone').disabled=locked||!this.lab.scene;el('import').disabled=locked||!this.pending;el('reveal').disabled=locked||!this.last;
 }
}
