'use strict';

const assert = require('node:assert/strict');
const fs = require('node:fs/promises');
const path = require('node:path');
const { launch } = require('../../workspace-desktop/test/calibration-integration.cjs');
const { verify } = require('../../scripts/Study-Build095.cjs');

async function main() {
  const source = path.resolve('build','chain177-live');
  const previous = JSON.parse(await fs.readFile(path.join(source,'result.json'),'utf8'));
  assert(previous.passed);
  const { source: commit } = verify(process.cwd());
  const project = await fs.mkdtemp(path.resolve('build','relay178-prior-'));
  for (const folder of ['workspace.fworld','workspace.fworld.previous','artifacts','receipts',
    'agency-043','learning','ramp-missions','measurements','workflows','goals','creation-044']) {
    await fs.cp(path.join(source,'project',folder),path.join(project,folder),{recursive:true,errorOnExist:true});
  }
  Object.assign(process.env,{FRAYNIX_WORKSPACE_NO_MODELS:'1',FRAYNIX_WORKSPACE_PERSISTENT:'0',
    FRAYNIX_WORKSPACE_JAVA_HEAP_MIB:'768',FRAYNIX_WORKSPACE_JAVA_PROCESSORS:'2'});
  const {app,page}=await launch(project);
  const id=previous.recovered.done.id;
  try {
    const opened=await page.evaluate(id=>window.workspace.request({operation:'history113',command:'open',args:{id,kind:'chat'}}),id);
    assert.equal(opened.artifact.id,previous.recovered.workflow.report.id);
    for(const key of ['ramp','workflow','handoffSha256'])assert.deepEqual(opened.proof[key],previous.archived[key]);
    const receipt={passed:true, source:commit, previousSource:previous.source, id,
      artifact:opened.artifact.id, handoffSha256:opened.proof.handoffSha256};
    await fs.writeFile('build/relay178-prior.json',JSON.stringify(receipt,null,2)+'\n');
    console.log(JSON.stringify(receipt));
  }finally{await app.close();}
}
main().catch(error=>{console.error(error);process.exitCode=1;});
