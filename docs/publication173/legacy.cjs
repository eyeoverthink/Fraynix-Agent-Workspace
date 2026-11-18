'use strict';

const assert = require('node:assert/strict');
const fs = require('node:fs/promises');
const path = require('node:path');
const {launch} = require('../../workspace-desktop/test/calibration-integration.cjs');
const {verify} = require('../../scripts/Study-Build095.cjs');

async function main(){
 const source=path.resolve(process.argv[2]||'build/ramp172-verified');
 const prior=JSON.parse(await fs.readFile(path.join(source,'result.json'),'utf8'));
 assert(prior.passed);
 const expected=prior.one.verified;
 const archived=prior.archived;
 const {source:verifiedBuild}=verify(process.cwd());
 const project=await fs.mkdtemp(path.resolve('build','ramp173-legacy-'));
 const copied=['workspace.fworld','workspace.fworld.previous','artifacts','receipts','ramp-missions','agency-043','learning'];
 for(const name of copied){
  await fs.cp(path.join(source,'project',name),path.join(project,name),{recursive:true,errorOnExist:true});
 }
 Object.assign(process.env,{FRAYNIX_WORKSPACE_NO_MODELS:'1',FRAYNIX_WORKSPACE_PERSISTENT:'0',
  FRAYNIX_WORKSPACE_JAVA_HEAP_MIB:'768',FRAYNIX_WORKSPACE_JAVA_PROCESSORS:'2'});
 const {app,page}=await launch(project);
 const api=(route,args)=>app.evaluate((_,req)=>global.workspaceTest.api(req.route,req.args),{route,args});
 try{
  const actual=await api('/api/ramp-mission/verify',{id:expected.id});
  assert.deepEqual(actual,expected);
  const proof=await api('/api/agency043/verify',{id:expected.id});
  assert(proof.passed);
  assert.deepEqual(proof.ramp,archived.ramp);
  const opened=await page.evaluate(id=>window.workspace.request({operation:'history113',command:'open',args:{id,kind:'chat'}}),expected.id);
  assert.equal(opened.artifact.id,expected.report.id);
  const revision=await app.evaluate(()=>global.workspaceTest.canvasRead("document.querySelector('#revision').textContent"));
  assert.equal(revision,'Input revision 1');
  const result={passed:true,source:verifiedBuild,priorSource:prior.source,originalMission:expected.id,
   unchangedReport:expected.report,unchangedAudit:expected.audit,archivedProof:proof.ramp};
  await fs.writeFile('build/ramp173-legacy.json',JSON.stringify(result,null,2)+'\n');
  console.log(JSON.stringify(result));
 }finally{await app.close();}
}

main().catch(error=>{console.error(error);process.exitCode=1;});
