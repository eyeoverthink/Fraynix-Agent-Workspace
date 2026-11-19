'use strict';

const assert=require('node:assert/strict');
const fs=require('node:fs/promises');
const path=require('node:path');
const {launch}=require('../../workspace-desktop/test/calibration-integration.cjs');
const {verify}=require('../../scripts/Study-Build095.cjs');

async function main(){
  const base=path.resolve('build','recommend179-live');
  const actual=JSON.parse(await fs.readFile(path.join(base,'result.json'),'utf8'));
  assert(actual.passed);
  const installed=verify(process.cwd());
  const target=await fs.mkdtemp(path.resolve('build','recommend179-history-'));
  for(const folder of ['workspace.fworld','workspace.fworld.previous','artifacts','receipts','agency-043',
    'learning','relay-playtests','ramp-missions','measurements','workflows','goals','creation-044']){
    await fs.cp(path.join(base,'project',folder),path.join(target,folder),{recursive:true,errorOnExist:true});
  }
  Object.assign(process.env,{FRAYNIX_WORKSPACE_NO_MODELS:'1',FRAYNIX_WORKSPACE_PERSISTENT:'0',
    FRAYNIX_WORKSPACE_JAVA_HEAP_MIB:'768',FRAYNIX_WORKSPACE_JAVA_PROCESSORS:'2'});
  const candidates=actual.trials.filter(row=>row.proof?.passed);
  assert.equal(candidates.length,2);
  let {app,page}=await launch(target);
  const api=(route,args={})=>app.evaluate((_,input)=>global.workspaceTest.api(input.route,input.args),{route,args});
  try{
    const revised=actual.trials.find(row=>row.fixture.id==='revised-ramp');
    if(revised.refusal){
      await assert.rejects(()=>api('/api/ramp-mission/read',{id:revised.id}));
      await assert.rejects(()=>api('/api/agency043/verify',{id:revised.id}));
    }
    const history=await api('/api/agency043/history');
    const ids=candidates.map(x=>x.id);
    assert(ids.every(id=>history.records.some(row=>row.id===id&&row.eligible)));
    await api('/api/agency043/archive',{planSha256:history.planSha256,ids});
    await app.close();
    ({app,page}=await launch(target));
    const opened=[];
    for(const trial of candidates){
      const receipt=await page.evaluate(id=>window.workspace.request({operation:'history113',command:'open',args:{id,kind:'chat'}}),trial.id);
      const report=trial.completion.javaWorkflow?.report||trial.completion.relayDashboard;
      assert.equal(receipt.artifact.id,report.id);
      for(const key of ['relay','ramp','workflow','handoffSha256']){
        if(trial.proof[key])assert.deepEqual(receipt.proof[key],trial.proof[key]);
      }
      opened.push({id:trial.id, report:receipt.artifact.id, archived:true});
    }
    const proof={passed:true,source:installed.source,opened,revisedRampHadNoChild:!!revised.refusal};
    await fs.writeFile('build/recommend179-history.json',JSON.stringify(proof,null,2)+'\n');
    console.log(JSON.stringify(proof));
  }finally{await app.close();}
}
main().catch(error=>{console.error(error);process.exitCode=1;});
