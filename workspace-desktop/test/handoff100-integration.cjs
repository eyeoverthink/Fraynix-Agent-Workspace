'use strict';
// Real recorded inputs and real local model. No generated implementation is supplied by this harness.
const {launch}=require('./calibration-integration.cjs'),fs=require('node:fs/promises'),sync=require('node:fs'),path=require('node:path'),assert=require('node:assert/strict'),crypto=require('node:crypto'),cp=require('node:child_process');
const repo=path.resolve(__dirname,'../..'),out=path.resolve(repo,process.env.EDITION100_OUT||'build/ui100-handoff'),project=path.join(out,'project');
const sleep=ms=>new Promise(r=>setTimeout(r,ms)),sha=s=>crypto.createHash('sha256').update(s).digest('hex');
const creationPrompt='Create and verify a working local explorer of the selected normalized motion CSV. Read inputs/source.json. Display every source row in #data-rows table body and a digit-only visible #row-count output. Add text input #source-filter, #apply-filter button and #reset-filter button. An empty filter shows all rows; a nonempty filter matches sourceRow EXACTLY as text. Filtering for 2 shows one row; filtering for 999999 shows zero rows; reset clears the filter and restores all rows and the full count. Display sourceRow, timeS and distanceM as columns, keeping numeric cell values unchanged from source strings, with seconds and metres labels. Show a simple SVG position-over-time chart from the supplied rows. Use a readable teal design, local files only, and no invented data. Freeze checks for original sourceRows count, filter 2, filter 999999 and reset before generating code. Save the skill as explorer100.';
(async()=>{
  assert(!sync.existsSync(project),'Preserve previous attempts');await fs.mkdir(project,{recursive:true});
  Object.assign(process.env,{FRAYNIX_WORKSPACE_PERSISTENT:'0',FRAYNIX_WORKSPACE_PLAN_CPU_ONLY:'1',FRAYNIX_WORKSPACE_JAVA_HEAP_MIB:'512',FRAYNIX_WORKSPACE_JAVA_PROCESSORS:'2'});
  require('../../scripts/Study-Build095.cjs').verify(repo);
  let app,page;const errors=[],record={source:cp.execFileSync('git',['rev-parse','HEAD'],{cwd:repo,encoding:'utf8'}).trim(),startedAt:new Date().toISOString(),project,tasks:[],checks:[],captures:[],setupScope:'Harness imports existing recorded CSV and explicitly selects completed data results. Actual chat and installed model generate and verify artifacts. Unit fixture receipts are not used here.'};
  const save=()=>fs.writeFile(path.join(out,'result.json'),JSON.stringify(record,null,2));
  const api=(route,args={})=>app.evaluate((_,x)=>global.workspaceTest.api(x.route,x.args),{route,args});
  const request=(operation,command,args={})=>page.evaluate(x=>window.workspace.request(x),{operation,command,args});
  async function open(){({app,page}=await launch(project));page.on('pageerror',e=>errors.push(e.message));await app.evaluate(({BrowserWindow})=>{const w=BrowserWindow.getAllWindows().find(x=>x.isVisible());w.setTitle('Fraynix Workspace · edition 100 verification');w.setSize(1600,1100);});await page.waitForFunction(()=>window.inputs100&&[...document.querySelector('#model').options].some(o=>o.value==='gpt-oss:20b'),{},{timeout:65000});await page.locator('#model').selectOption('gpt-oss:20b');}
  async function capture(file){await page.waitForTimeout(400);await fs.writeFile(path.join(out,file),Buffer.from(await app.evaluate(()=>global.workspaceTest.capture()),'base64'));record.captures.push(file);await save();}
  async function chat(kind,prompt){
    await page.waitForFunction(()=>!document.querySelector('#run').disabled,{},{timeout:45000});const prior=new Set((await api('/api/agency043/state')).tasks.map(t=>t.id)),start=performance.now();
    const row={kind,prompt,submittedAt:new Date().toISOString()};record.tasks.push(row);await save();await page.locator('#goal').fill(prompt);await page.locator('#run').click();
    const deadline=Date.now()+20*60000;let task;
    while(Date.now()<deadline){const state=await api('/api/agency043/state'),found=state.tasks.findLast(t=>!prior.has(t.id)&&t.goal===prompt);if(found){task=await api('/api/agency043/read',{id:found.id});row.task=task;if(['completed','interrupted','cancelled'].includes(task.status))break;}await sleep(900);if(task)await save();}
    row.callerMillis=performance.now()-start;await save();assert.equal(task?.status,'completed',JSON.stringify({status:task?.status,reason:task?.reason}));assert.equal(task.outcome,'verified');
    row.goal=await api('/api/goals/read',{id:task.completion044.javaGoal.id});row.artifact=await api('/api/artifact/read',{id:row.goal.report.id});assert(row.artifact.test.passed);
    if(row.goal.procedure==='creation-web-v1'){const saved=JSON.parse(await fs.readFile(path.join(project,'creation-044/requests.json'),'utf8'));row.generation=saved.data.jobs.filter(j=>j.goalId===row.goal.id);assert(row.generation.length);assert(row.generation.every(j=>j.model?.digest&&j.compute?.cpuOnly));}else{assert(task.timing052.modelCalls>0);assert(task.modelDigest);}
    await save();return row;
  }
  async function data(name){const csv=await fs.readFile(path.join(repo,'docs/datasets/delfys75-013',name+'.csv'),'utf8');const selected=await request('data','import',{title:'Recorded Delfys '+name+' · edition 100',filename:name+'.csv',csv,sourceKind:'measured',sourceNote:'Unchanged frozen edition013 recorded dataset; not a new sensor recording or causal validation.',missingPolicy:'reject',mapping:{timeColumn:'time_s',distanceColumn:'displacement_m',timeUnit:'s',distanceUnit:'m'}});const d=await api('/api/measurement/read',{id:selected.datasetId,version:selected.version});assert.equal(d.csv,csv);assert.equal(d.sourceSha256,sha(csv));return d;}
  async function pin(row){
    await page.waitForFunction(id=>[...document.querySelector('#inputs100-results').options].some(o=>o.value===id),row.goal.id);
    if(!await page.locator('#inputs100-controls').isVisible())await page.locator('#inputs100-toggle').click();await page.locator('#inputs100-results').selectOption(row.goal.id);await page.locator('#inputs100-handoff').click();
    await page.waitForFunction(()=>document.querySelector('#inputs100-source').textContent.includes('verified data result'));
    const current=await request('inputs100','state');assert.equal(current.selection.source.dataGoalId100,row.goal.id);assert.equal(current.selection.source.sourceFile,'normalized.csv');assert.equal(current.selection.preview.fileSha256,sha(row.artifact.contents['normalized.csv']));return current.selection;
  }
  async function inspectCreation(row,source){
    assert.deepEqual(row.goal.dataHandoff100,source.handoff);assert.equal(row.artifact.contents['inputs/source.csv'],sourceArtifact.contents['normalized.csv']);
    await page.waitForFunction(id=>document.querySelector('#task099').dataset.taskId===id&&document.querySelector('#task099-phase').textContent==='Verified result',row.task.id);await page.locator('#task099-open').click();
    const steps=[{selector:'#data-rows tr',expectCount:source.preview.rowCount},{selector:'#row-count',expectText:String(source.preview.rowCount)},
      {selector:'#source-filter',action:'fill',value:'2'},{selector:'#apply-filter',action:'click'},{selector:'#data-rows tr',expectCount:1},{selector:'#row-count',expectText:'1'},
      {selector:'#source-filter',action:'fill',value:'999999'},{selector:'#apply-filter',action:'click'},{selector:'#data-rows tr',expectCount:0},{selector:'#row-count',expectText:'0'},
      {selector:'#reset-filter',action:'click'},{selector:'#source-filter',expectValue:''},{selector:'#data-rows tr',expectCount:source.preview.rowCount},{selector:'#row-count',expectText:String(source.preview.rowCount)}];
    row.independent=await app.evaluate((_,s)=>global.workspaceTest.canvasChecks(s),steps);assert(row.independent.passed,JSON.stringify(row.independent));
    const actual=await app.evaluate(()=>global.workspaceTest.canvasRead("JSON.stringify([...document.querySelectorAll('#data-rows tr')].map(r=>[...r.querySelectorAll('td')].map(c=>c.textContent.trim())))"));
    row.displayedRows=JSON.parse(actual);const expected=JSON.parse(row.artifact.contents['inputs/source.json']).rows;
    assert.equal(row.displayedRows.length,expected.length);for(let i=0;i<expected.length;i++){assert.equal(Number(row.displayedRows[i][0]),Number(expected[i].sourceRow));assert(Math.abs(Number(row.displayedRows[i][1])-Number(expected[i].timeS))<.000001);assert(Math.abs(Number(row.displayedRows[i][2])-Number(expected[i].distanceM))<.000001);}
    record.checks.push(row.kind+': real code, exact source lineage, independent filter/reset/count/row checks');await save();
  }
  let sourceArtifact;
  try{
    record.model=(await (await fetch('http://127.0.0.1:11434/api/tags')).json()).models.find(m=>m.name==='gpt-oss:20b');assert(record.model);await open();
    record.validation=await data('validation');const analyzed=await chat('data-validation','Analyze the selected motion dataset with gap limit 0.05 seconds and speed limit 5 m/s. Chart it, preserve its source and save the successful procedure as motion100.');
    sourceArtifact=analyzed.artifact;const pinned=await pin(analyzed);record.firstSelection=pinned;await capture('source100-1600.png');
    // Looking at another artifact cannot change the pinned source.
    await page.evaluate(()=>window.workspace.request({operation:'new'}));assert.deepEqual((await request('inputs100','state')).selection,pinned);
    const created=await chat('creation-validation',creationPrompt);await inspectCreation(created,pinned);await capture('explorer100-1600.png');
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
