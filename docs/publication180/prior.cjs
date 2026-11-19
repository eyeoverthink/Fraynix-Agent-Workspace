'use strict';

const assert=require('node:assert/strict');
const fs=require('node:fs/promises');
const path=require('node:path');
const {launch}=require('../../workspace-desktop/test/calibration-integration.cjs');
const {verify}=require('../../scripts/Study-Build095.cjs');

async function main(){
  const base=path.resolve('build','recommend179-live');
  const previous=JSON.parse(await fs.readFile(path.join(base,'result.json'),'utf8'));
  assert(previous.passed);
  const installed=verify(process.cwd());
  const target=await fs.mkdtemp(path.resolve('build','workbench180-prior-'));
  for(const folder of ['workspace.fworld','workspace.fworld.previous','artifacts','receipts','agency-043',
    'learning','relay-playtests','ramp-missions','measurements','workflows','goals','creation-044']){
    await fs.cp(path.join(base,'project',folder),path.join(target,folder),{recursive:true,errorOnExist:true});
  }
  Object.assign(process.env,{FRAYNIX_WORKSPACE_NO_MODELS:'1',FRAYNIX_WORKSPACE_PERSISTENT:'0',
    FRAYNIX_WORKSPACE_JAVA_HEAP_MIB:'768',FRAYNIX_WORKSPACE_JAVA_PROCESSORS:'2'});
  const {app,page}=await launch(target);
  try{
    const checks=[];
    for(const trial of previous.trials.filter(x=>x.proof?.passed)){
      const opened=await page.evaluate(id=>window.workspace.request({operation:'history113',command:'open',args:{id,kind:'chat'}}),trial.id);
      const report=trial.completion.javaWorkflow?.report||trial.completion.relayDashboard;
      assert.equal(opened.artifact.id,report.id);
      for(const key of ['relay','ramp','workflow','handoffSha256']){
        if(trial.proof[key])assert.deepEqual(opened.proof[key],trial.proof[key]);
      }
      checks.push({id:trial.id,report:opened.artifact.id,action:trial.actual});
    }
    assert.equal(checks.length,2);
    const result={passed:true,source:installed.source,priorSource:previous.source,checks};
    await fs.writeFile('build/workbench180-prior.json',JSON.stringify(result,null,2)+'\n');
    console.log(JSON.stringify(result));
  }finally{await app.close();}
}
main().catch(error=>{console.error(error);process.exitCode=1;});
