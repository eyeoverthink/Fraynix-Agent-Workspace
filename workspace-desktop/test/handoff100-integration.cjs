'use strict';
// Real recorded inputs and real local model. No generated implementation is supplied by this harness.
const {launch}=require('./calibration-integration.cjs'),fs=require('node:fs/promises'),sync=require('node:fs'),path=require('node:path'),assert=require('node:assert/strict'),crypto=require('node:crypto'),cp=require('node:child_process');
const repo=path.resolve(__dirname,'../..'),out=path.resolve(repo,process.env.EDITION100_OUT||'build/ui100-handoff-attempt3'),project=path.join(out,'project');
const inspectOnly=process.env.EDITION100_INSPECT_ONLY==='1',resume=inspectOnly||process.env.EDITION100_RESUME==='1', resultName=inspectOnly?'result-final.json':resume?'result-resumed.json':'result.json';
const sleep=ms=>new Promise(r=>setTimeout(r,ms)),sha=s=>crypto.createHash('sha256').update(s).digest('hex');
const creationPrompt='Create a compact working data explorer, using plain HTML, minimal CSS and JavaScript, no libraries or chart. Read inputs/source.json supplied by Java. Show its title in #title, all original source rows in #data-table tbody, and digit-only visible #row-count. Text input #filter must immediately filter by EXACT sourceRow string on input; empty means all rows. Button #reset clears the input and restores all rows. Columns must be sourceRow, timeS, distanceM; display original numeric strings unchanged, with seconds/metres labels. Render cells safely. Use a readable teal design. Implement the supplied fixed behavior checks. Keep the complete implementation concise.';
(async()=>{
  assert(resume?sync.existsSync(path.join(out,'result.json')):!sync.existsSync(project),'Preserve previous attempts');await fs.mkdir(project,{recursive:true});
  Object.assign(process.env,{FRAYNIX_WORKSPACE_PERSISTENT:'0',FRAYNIX_WORKSPACE_PLAN_CPU_ONLY:'1',FRAYNIX_WORKSPACE_JAVA_HEAP_MIB:'512',FRAYNIX_WORKSPACE_JAVA_PROCESSORS:'2'});
  require('../../scripts/Study-Build095.cjs').verify(repo);
  let app,page;const errors=[],record={source:cp.execFileSync('git',['rev-parse','HEAD'],{cwd:repo,encoding:'utf8'}).trim(),startedAt:new Date().toISOString(),project,tasks:[],checks:[],captures:[],setupScope:'Harness imports existing recorded CSV and explicitly selects completed data results. Actual chat handles data and reuse. The existing creation dialog supplies explicit source-parameterized behavior checks before the installed model generates code; no model-authored check claim for that stage. Unit fixture receipts are not used here.'};
  if(resume){const priorName=inspectOnly?'result-resumed.json':'result.json';const prior=await fs.readFile(path.join(out,priorName));Object.assign(record,JSON.parse(prior));record.previousFailure={file:priorName,sha256:sha(prior),error:record.error};delete record.error;record.resumedAt=new Date().toISOString();record.inspectionSource=cp.execFileSync('git',['rev-parse','HEAD'],{cwd:repo,encoding:'utf8'}).trim();}
  const save=()=>fs.writeFile(path.join(out,resultName),JSON.stringify(record,null,2));
  const api=(route,args={})=>app.evaluate((_,x)=>global.workspaceTest.api(x.route,x.args),{route,args});
  const request=(operation,command,args={})=>page.evaluate(x=>window.workspace.request(x),{operation,command,args});
  async function open(){({app,page}=await launch(project));page.on('pageerror',e=>errors.push(e.message));await app.evaluate(({BrowserWindow})=>{const w=BrowserWindow.getAllWindows().find(x=>x.isVisible());w.setTitle('Fraynix Workspace Â· edition 100 verification');w.setSize(1600,1100);});await page.waitForFunction(()=>window.inputs100&&[...document.querySelector('#model').options].some(o=>o.value==='gpt-oss:20b'),{},{timeout:65000});await page.locator('#model').selectOption('gpt-oss:20b');}
  async function capture(file){await page.waitForTimeout(400);await fs.writeFile(path.join(out,file),Buffer.from(await app.evaluate(()=>global.workspaceTest.capture()),'base64'));record.captures.push(file);await save();}
  async function chat(kind,prompt){
    await page.waitForFunction(()=>!document.querySelector('#run').disabled,{},{timeout:45000});const prior=new Set((await api('/api/agency043/state')).tasks.map(t=>t.id)),start=performance.now();
    const row={kind,prompt,submittedAt:new Date().toISOString()};record.tasks.push(row);await save();
    if(kind==='creation-validation'){
      if(!await page.locator('#inputs100-controls').isVisible())await page.locator('#inputs100-toggle').click();
      await page.locator('#inputs100-handoff').click();await page.locator('#creation-dialog').waitFor({state:'visible'});
      row.suppliedChecks=JSON.parse(await page.locator('#creation-checks').inputValue());assert(row.suppliedChecks.some(s=>s.sourceRows===true));assert(row.suppliedChecks.some(s=>s.sourceCount));
      await page.locator('#creation-request').fill(prompt);await page.locator('#creation-skill').fill('explorer100');await save();await page.locator('#creation-build').click();
    }else{await page.locator('#goal').fill(prompt);await page.locator('#run').click();}
    const deadline=Date.now()+20*60000;let task;
    while(Date.now()<deadline){const state=await api('/api/agency043/state'),found=state.tasks.findLast(t=>!prior.has(t.id)&&t.goal===prompt);if(found){task=await api('/api/agency043/read',{id:found.id});row.task=task;if(['completed','interrupted','cancelled'].includes(task.status))break;}await sleep(900);if(task)await save();}
    row.callerMillis=performance.now()-start;await save();assert.equal(task?.status,'completed',JSON.stringify({status:task?.status,reason:task?.reason}));assert.equal(task.outcome,'verified');
    row.goal=await api('/api/goals/read',{id:task.completion044.javaGoal.id});row.artifact=await api('/api/artifact/read',{id:row.goal.report.id});assert(row.artifact.test.passed);
    if(row.goal.procedure==='creation-web-v1'){const saved=JSON.parse(await fs.readFile(path.join(project,'creation-044/requests.json'),'utf8'));row.generation=saved.data.jobs.filter(j=>j.goalId===row.goal.id);assert(row.generation.length);assert(row.generation.every(j=>j.model?.digest&&j.compute?.cpuOnly));}else{assert(task.timing052.modelCalls>0);assert(task.modelDigest);}
    await save();return row;
  }
  async function data(name){const csv=await fs.readFile(path.join(repo,'docs/datasets/delfys75-013',name+'.csv'),'utf8');const selected=await request('data','import',{title:'Recorded Delfys '+name+' / edition 100',filename:name+'.csv',csv,sourceKind:'measured',sourceNote:'Unchanged frozen edition013 recorded dataset; not a new sensor recording or causal validation.',missingPolicy:'reject',mapping:{timeColumn:'time_s',distanceColumn:'displacement_m',timeUnit:'s',distanceUnit:'m'}});const d=await api('/api/measurement/read',{id:selected.datasetId,version:selected.version});assert.equal(d.csv,csv);assert.equal(d.sourceSha256,sha(csv));return d;}
  async function pin(row){
    await page.waitForFunction(id=>[...document.querySelector('#inputs100-results').options].some(o=>o.value===id),row.goal.id);
    if(!await page.locator('#inputs100-controls').isVisible())await page.locator('#inputs100-toggle').click();await page.locator('#inputs100-results').selectOption(row.goal.id);await page.locator('#inputs100-handoff').click();
    await page.waitForFunction(()=>document.querySelector('#inputs100-source').textContent.includes('verified data result'));
    await page.locator('#creation-dialog').waitFor({state:'visible'});await page.locator('#creation-close').click();const current=await request('inputs100','state');assert.equal(current.selection.source.dataGoalId100,row.goal.id);assert.equal(current.selection.source.sourceFile,'normalized.csv');assert.equal(current.selection.preview.fileSha256,sha(row.artifact.contents['normalized.csv']));return current.selection;
  }
  async function inspectCreation(row,source){
    assert.deepEqual(row.goal.dataHandoff100,source.handoff);assert.equal(row.artifact.contents['inputs/source.csv'],sourceArtifact.contents['normalized.csv']);
    await request('agency043','mission-open',{id:row.task.id});await page.waitForTimeout(1500);
    const steps=[{selector:'tbody#data-table tr, #data-table tbody tr',expectCount:source.preview.rowCount},{selector:'#row-count',expectText:String(source.preview.rowCount)},
      {selector:'#filter',action:'fill',value:'2'},{selector:'tbody#data-table tr, #data-table tbody tr',expectCount:1},{selector:'#row-count',expectText:'1'},
      {selector:'#filter',action:'fill',value:'999999'},{selector:'tbody#data-table tr, #data-table tbody tr',expectCount:0},{selector:'#row-count',expectText:'0'},
      {selector:'#reset',action:'click'},{selector:'#filter',expectValue:''},{selector:'tbody#data-table tr, #data-table tbody tr',expectCount:source.preview.rowCount},{selector:'#row-count',expectText:String(source.preview.rowCount)}];
    if(row.independent)(row.priorInspections??=[]).push(row.independent);row.independent=await app.evaluate((_,s)=>global.workspaceTest.canvasChecks(s),steps);assert(row.independent.every(c=>c.passed),JSON.stringify(row.independent));
    const actual=await app.evaluate(()=>global.workspaceTest.canvasRead("JSON.stringify([...document.querySelectorAll('tbody#data-table tr, #data-table tbody tr')].map(r=>[...r.querySelectorAll('td')].map(c=>c.textContent.trim())))"));
    row.displayedRows=JSON.parse(actual);const expected=JSON.parse(row.artifact.contents['inputs/source.json']).rows;
    const numeric=(text,unit)=>{const value=unit&&text.endsWith(' '+unit)?text.slice(0,-unit.length-1):text;assert(/^[+-]?(?:\d+(?:\.\d*)?|\.\d+)(?:[eE][+-]?\d+)?$/.test(value),'Unexpected value or unit: '+text);return Number(value);};
    assert.equal(row.displayedRows.length,expected.length);for(let i=0;i<expected.length;i++){assert.equal(numeric(row.displayedRows[i][0],''),Number(expected[i].source_row));assert(Math.abs(numeric(row.displayedRows[i][1],'s')-Number(expected[i].time_s))<.000001);assert(Math.abs(numeric(row.displayedRows[i][2],'m')-Number(expected[i].displacement_m))<.000001);}
    record.checks.push(row.kind+': real code, exact source lineage, independent filter/reset/count/row checks');await save();
  }
  let sourceArtifact;
  try{
    record.model=(await (await fetch('http://127.0.0.1:11434/api/tags')).json()).models.find(m=>m.name==='gpt-oss:20b');assert(record.model);await open();
    let analyzed,created,pinned;
    if(inspectOnly){
      assert.equal(record.tasks.length,4);assert.deepEqual((await request('inputs100','state')).selection,record.changedSelection);
      for(const t of record.tasks){const saved=await api('/api/goals/read',{id:t.goal.id});assert.equal(saved.status,'completed');assert.deepEqual((await api('/api/artifact/read',{id:t.goal.report.id})).contents,t.artifact.contents);}
      for(const i of [0,2]){sourceArtifact=record.tasks[i].artifact;await inspectCreation(record.tasks[i+1],i===0?record.firstSelection:record.changedSelection);}
      assert.equal(record.tasks[2].goal.skill.id,record.tasks[0].goal.savedSkill.id);assert.equal(record.tasks[3].goal.skill.id,record.tasks[1].goal.savedSkill.id);
      await capture('changed100-1600.png');assert.deepEqual(errors,[]);record.passed=true;return;
    }
    if(resume){
      [analyzed,created]=record.tasks;assert.equal(record.tasks.length,2);sourceArtifact=analyzed.artifact;pinned=record.firstSelection;
      assert.deepEqual((await request('inputs100','state')).selection,pinned);
      for(const t of [analyzed,created]){const saved=await api('/api/goals/read',{id:t.goal.id});assert.equal(saved.status,'completed');assert.deepEqual((await api('/api/artifact/read',{id:t.goal.report.id})).contents,t.artifact.contents);}
      await inspectCreation(created,pinned);await capture('explorer100-1600.png');
    }else{
      record.validation=await data('validation');analyzed=await chat('data-validation','Analyze the selected motion dataset with gap limit 0.05 seconds and speed limit 5 m/s. Chart it, preserve its source and save the successful procedure as motion100.');
      sourceArtifact=analyzed.artifact;pinned=await pin(analyzed);record.firstSelection=pinned;await capture('source100-1600.png');
      await page.evaluate(()=>window.workspace.request({operation:'new'}));assert.deepEqual((await request('inputs100','state')).selection,pinned);
      created=await chat('creation-validation',creationPrompt);await inspectCreation(created,pinned);await capture('explorer100-1600.png');
    }
    await app.evaluate(({BrowserWindow})=>BrowserWindow.getAllWindows().find(x=>x.isVisible()).setSize(1100,1100));await capture('explorer100-1100.png');assert(await page.locator('#goal').isVisible());assert(await page.locator('#run').isVisible());
    await app.close();app=null;const restart=performance.now();await open();record.restartMillis=performance.now()-restart;await page.waitForTimeout(250);assert.deepEqual((await request('inputs100','state')).selection,pinned);record.checks.push('Pinned source and verified lineage restored by Java after cold restart');await capture('restored100-1600.png');
    await request('inputs100','clear');assert.equal((await request('inputs100','state')).selection,null);
    record.training=await data('training');const second=await chat('data-training','Reuse motion100 on the selected dataset with gap limit 0.02 seconds and speed limit 0.4 m/s. Preserve the new source and show the quality flags.');
    assert.equal(second.goal.skill.id,analyzed.goal.savedSkill.id);sourceArtifact=second.artifact;const fresh=await pin(second);assert.notEqual(fresh.preview.fileSha256,pinned.preview.fileSha256);assert.equal(fresh.handoff.quality.qualityPassed,false);record.changedSelection=fresh;
    const reused=await chat('creation-training','Reuse explorer100 with the selected creation source. Verify the source-bound counts, exact row filter and reset against this newly selected dataset.');await inspectCreation(reused,fresh);assert.equal(reused.goal.skill.id,created.goal.savedSkill.id);await capture('changed100-1600.png');
    assert.deepEqual(errors,[]);record.passed=true;
  }catch(e){record.error=e.stack;if(app){await capture('failure100.png').catch(()=>{});record.messages=await page.locator('#messages').innerText().catch(()=> '');}throw e;}
  finally{record.errors=errors;record.finishedAt=new Date().toISOString();await save();if(app)await app.close();require('../../scripts/Study-Build095.cjs').verify(repo);}
})().catch(e=>{console.error(e);process.exitCode=1;});
