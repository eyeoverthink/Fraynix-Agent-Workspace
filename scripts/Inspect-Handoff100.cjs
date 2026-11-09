'use strict';
const fs=require('node:fs'),path=require('node:path'),assert=require('node:assert/strict'),crypto=require('node:crypto');
const sha=v=>crypto.createHash('sha256').update(v).digest('hex');
function inspect(r){
  assert.equal(r.passed,true);assert.equal(r.tasks.length,4);assert.deepEqual(r.errors,[]);
  const results=[];
  for(const [name,input] of [['validation',r.validation],['training',r.training]]){
    const data=r.tasks.find(t=>t.kind==='data-'+name),creation=r.tasks.find(t=>t.kind==='creation-'+name);assert(data&&creation);
    assert.equal(data.task.outcome,'verified');assert.equal(creation.task.outcome,'verified');assert(data.task.timing052.modelCalls>0);
    const text=input.csv.trim().split(/\r?\n/).map(l=>l.split(',')),headers=text.shift(),m=input.mapping;
    const expected=text.map((row,i)=>({sourceRow:i+2,timeS:Number(row[headers.indexOf(m.timeColumn)])+(m.timeOffsetS||0),distanceM:(Number(row[headers.indexOf(m.distanceColumn)])-(m.distanceOrigin||0))*(m.direction??1)}));
    assert.equal(m.timeUnit,'s');assert.equal(m.distanceUnit,'m');assert.equal(input.sourceSha256,sha(input.csv));assert.equal(data.goal.dataBinding.sourceSha256,sha(input.csv));assert.equal(data.artifact.contents['original.csv'],input.csv);
    const normalized=JSON.parse(data.artifact.contents['normalized.json']);assert.deepEqual(normalized,expected);
    let travel=0,gaps=0,speeds=0;
    for(let i=1;i<expected.length;i++){const dt=expected[i].timeS-expected[i-1].timeS,dx=expected[i].distanceM-expected[i-1].distanceM;assert(dt>0);travel+=Math.abs(dx);gaps+=dt>data.goal.maxGapS;speeds+=Math.abs(dx/dt)>data.goal.speedLimitMps;}
    assert.equal(data.goal.metrics.rowCount,expected.length);assert(Math.abs(data.goal.metrics.totalTravelM-travel)<1e-10);assert.equal(data.goal.metrics.gapFlags,gaps);assert.equal(data.goal.metrics.speedFlags,speeds);
    const link=creation.goal.dataHandoff100,source=link.source;assert.equal(link.schema,'verified-data-handoff-100-v1');assert.equal(source.dataGoalId100,data.goal.id);assert.equal(source.sourceArtifactId,data.artifact.id);assert.equal(source.sourceVersion,data.artifact.version);assert.equal(source.sourceFile,'normalized.csv');assert.deepEqual(link.dataBinding,data.goal.dataBinding);assert.deepEqual(link.quality,data.goal.metrics);
    const csv=data.artifact.contents['normalized.csv'];assert.equal(link.preview.fileSha256,sha(csv));assert.equal(creation.goal.sourceBinding.fileSha256,sha(csv));assert.equal(creation.artifact.contents['inputs/source.csv'],csv);
    const copied=JSON.parse(creation.artifact.contents['inputs/source.json']).rows;assert.equal(copied.length,expected.length);
    assert.equal(creation.displayedRows.length,expected.length);for(let i=0;i<expected.length;i++)for(const [column,key]of ['sourceRow','timeS','distanceM'].entries()){assert.equal(Number(copied[i][["source_row","time_s","displacement_m"][column]]),expected[i][key]);assert(Math.abs(Number(creation.displayedRows[i][column])-expected[i][key])<.000001);}
    assert(data.artifact.test.passed&&creation.artifact.test.passed&&creation.independent.every(c=>c.passed));assert.equal(creation.independent.length,12);assert(creation.generation.length);
    for(const job of creation.generation){assert.equal(job.model.digest,r.model.digest);assert.equal(job.compute.options.num_gpu,0);assert.equal(job.compute.options.num_thread,4);}
    results.push({input:name,rows:expected.length,travelM:travel,gapFlags:gaps,speedFlags:speeds,sourceSha256:sha(input.csv),normalizedSha256:sha(csv),dataMillis:data.callerMillis,creationMillis:creation.callerMillis,routerCalls:data.task.timing052.modelCalls+creation.task.timing052.modelCalls,generationJobs:creation.generation.length});
  }
  assert.notEqual(results[0].normalizedSha256,results[1].normalizedSha256);
  assert.equal(r.tasks[2].goal.skill.id,r.tasks[0].goal.savedSkill.id);assert.equal(r.tasks[3].goal.skill.id,r.tasks[1].goal.savedSkill.id);
  return {passed:true,results,scope:'Independent original CSV arithmetic, exact linked inputs, actual model receipts and rendered rows/count/filter/reset checks. No causal physics or speedup claim.'};
}
function refusalChecks(r){const cases=[['changed source CSV',x=>x.validation.csv+='0,999\n'],['substituted lineage',x=>x.tasks[1].goal.dataHandoff100.source.dataGoalId100='foreign'],['changed generated input',x=>x.tasks[1].artifact.contents['inputs/source.csv']='forged'],['invented displayed value',x=>x.tasks[3].displayedRows[0][2]='999']];return cases.map(([name,change])=>{const altered=structuredClone(r);change(altered);assert.throws(()=>inspect(altered),name);return {name,refused:true};});}
if(require.main===module){const file=path.resolve(process.argv[2]||'build/ui100-handoff-attempt3/result-resumed.json'),r=JSON.parse(fs.readFileSync(file,'utf8'));console.log(JSON.stringify({...inspect(r),refusals:refusalChecks(r)},null,2));}
module.exports={inspect,refusalChecks};
