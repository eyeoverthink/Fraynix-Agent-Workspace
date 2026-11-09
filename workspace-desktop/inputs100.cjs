'use strict';
const fs=require('node:fs/promises');
const {isDeepStrictEqual}=require('node:util');

// Presentation selection only. Java rechecks the original source and task outcome.
class Inputs100 {
  constructor(api,file){this.api=api;this.file=file;this.record=null;this.error='';this.loaded=null;}
  async ready(){return this.loaded??=(async()=>{
    try{
      const info=await fs.stat(this.file);if(info.size>32768)throw Error('Saved input selection is too large');
      const saved=JSON.parse(await fs.readFile(this.file,'utf8'));
      if(saved.schema!=='input-selection-100-v1'||!saved.selection)throw Error('Unknown saved input selection');
      this.record=saved.selection;await this.verify();
    }catch(e){if(e.code!=='ENOENT')this.error='Saved source needs attention: '+e.message;}
  })();}
  async resolve(source){
    if(!source||typeof source!=='object'||Array.isArray(source)||Object.keys(source).some(k=>!['sourceArtifactId','sourceVersion','sourceFile','dataGoalId100'].includes(k)))throw Error('Choose one exact source file');
    if(source.dataGoalId100){
      const handoff=await this.api('/api/goals/creation-source100',{goalId:source.dataGoalId100});
      for(const k of ['sourceArtifactId','sourceVersion','sourceFile'])if(source[k]!==undefined&&source[k]!==handoff.source[k])throw Error('Selected file differs from the verified data result');
      return {source:handoff.source,preview:handoff.preview,handoff};
    }
    const preview=await this.api('/api/creation/source',source);
    return {source:{sourceArtifactId:preview.id,sourceVersion:preview.version,sourceFile:preview.file},preview};
  }
  async verify(){
    if(!this.record)return;
    const actual=await this.resolve(this.record.source);
    if(!isDeepStrictEqual(actual,this.record))throw Error('Pinned source or its verified lineage changed; select a new source explicitly');
  }
  async select(source){
    await this.ready();const next=await this.resolve(source);
    await fs.writeFile(this.file+'.tmp',JSON.stringify({schema:'input-selection-100-v1',selection:next},null,2),'utf8');
    await fs.rename(this.file+'.tmp',this.file);this.record=next;this.error='';return this.snapshot();
  }
  async clear(){await this.ready();await fs.rm(this.file,{force:true});this.record=null;this.error='';return this.snapshot();}
  snapshot(){return {selection:this.record?structuredClone(this.record):null,error:this.error};}
  binding(){if(this.error)throw Error(this.error);return this.record?structuredClone(this.record.source):null;}
  async beforeStart(){await this.ready();if(this.error)throw Error(this.error);try{await this.verify();}catch(e){this.error=e.message;throw e;}}
}
module.exports={Inputs100};
