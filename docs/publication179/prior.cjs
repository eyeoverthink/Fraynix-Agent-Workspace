'use strict';

const assert=require('node:assert/strict');
const fs=require('node:fs/promises');
const path=require('node:path');
const {launch}=require('../../workspace-desktop/test/calibration-integration.cjs');
const {verify}=require('../../scripts/Study-Build095.cjs');

async function main(){
  const source=path.resolve('build','relay178-live');
  const previous=JSON.parse(await fs.readFile(path.join(source,'result.json'),'utf8'));
  assert(previous.passed&&previous.archive.passed);
  const {source:commit}=verify(process.cwd());
  const target=await fs.mkdtemp(path.resolve('build','recommend179-prior-'));
  for(const folder of ['workspace.fworld','workspace.fworld.previous','artifacts','receipts',
    'agency-043','learning','relay-playtests']){
    await fs.cp(path.join(source,'project',folder),path.join(target,folder),{recursive:true,errorOnExist:true});
  }
  Object.assign(process.env,{FRAYNIX_WORKSPACE_NO_MODELS:'1',FRAYNIX_WORKSPACE_PERSISTENT:'0',
    FRAYNIX_WORKSPACE_JAVA_HEAP_MIB:'768',FRAYNIX_WORKSPACE_JAVA_PROCESSORS:'2'});
  const {app,page}=await launch(target);
  try{
    const id=previous.recovered.id;
    const result=await page.evaluate(id=>window.workspace.request({operation:'history113',command:'open',args:{id,kind:'chat'}}),id);
    assert.equal(result.artifact.id,previous.recovered.dashboard.id);
    assert.deepEqual(result.proof.relay,previous.archive.proof);
    const receipt={passed:true, source:commit, priorSource:previous.source, id,
      report:result.artifact.id, proof:result.proof.relay};
    await fs.writeFile('build/recommend179-prior.json',JSON.stringify(receipt,null,2)+'\n');
    console.log(JSON.stringify(receipt));
  }finally{await app.close();}
}
main().catch(error=>{console.error(error);process.exitCode=1;});
