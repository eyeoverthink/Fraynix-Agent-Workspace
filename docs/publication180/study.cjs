'use strict';

const assert=require('node:assert/strict');
const {createHash}=require('node:crypto');
const fs=require('node:fs/promises');
const path=require('node:path');
const {launch}=require('../../workspace-desktop/test/calibration-integration.cjs');
const {verify}=require('../../scripts/Study-Build095.cjs');
const output=path.resolve('build',process.argv[2]||'workbench180-live');
const project=path.join(output,'project');
const savedPhrase='Make the datasauce portable';
const csv='\ufeffCode,Name,Notes\r\n00017,南,"first\nsecond ""quoted"" line"\r\n0,Café,\r\n';
const binary=Buffer.from('89504e470d0a1a0a00010280ff','hex');
const toolBrief='Whip up a local tool that counts boxes. Use #count starting at 0, #add to add one and #reset to return to zero. It should work offline in the canvas.';
const appBrief='Spin up an app that keeps a list of errands.';
const fixedCases=[
  {id:'slang', action:'csv-to-json', text:'Yo, can you get these comma-separated rows into something my JSON reader can swallow? Keep the actual text.'},
  {id:'spanish', action:'create-app', text:'Hazme una mini aplicación local para anotar tareas y marcarlas como hechas.'},
  {id:'french', action:'inspect-bytes', text:'Tu peux me montrer les huit premiers octets et l’empreinte du fichier ?'},
  {id:'ambiguous', action:'clarify', text:'I’m torn between converting the CSV and making a calendar widget. I haven’t chosen either one.'}
];
const digest=x=>createHash('sha256').update(x).digest('hex');
const delay=ms=>new Promise(resolve=>setTimeout(resolve,ms));

async function main(){
  const build=verify(process.cwd());
  await fs.mkdir(output);
  await fs.mkdir(project);
  Object.assign(process.env,{FRAYNIX_WORKSPACE_PERSISTENT:'0',FRAYNIX_WORKSPACE_JAVA_HEAP_MIB:'768',
    FRAYNIX_WORKSPACE_JAVA_PROCESSORS:'2'});
  const result={passed:false,source:build.source,captures:[],models:{},errors:[]};
  let app,page;
  const api=(route,args={})=>app.evaluate((_,x)=>global.workspaceTest.api(x.route,x.args),{route,args});
  const ui=(command,args)=>page.evaluate(x=>window.workspace.request({operation:'requestWorkbench',...x}),{command,args});
  const task=id=>api('/api/requests/read',{id});
  const save=()=>fs.writeFile(path.join(output,'result.json'),JSON.stringify(result,null,2)+'\n');

  async function until(check,label,timeout=170000){
    for(let end=Date.now()+timeout;Date.now()<end;){
      const value=await check();
      if(value)return value;
      await delay(150);
    }
    throw Error('Timed out waiting for '+label);
  }
  async function open(withModels){
    if(withModels)delete process.env.FRAYNIX_WORKSPACE_NO_MODELS;
    else process.env.FRAYNIX_WORKSPACE_NO_MODELS='1';
    ({app,page}=await launch(project));
    assert((await app.evaluate(()=>global.workspaceTest.network())).enabled);
    await app.evaluate(({BrowserWindow})=>BrowserWindow.getAllWindows().find(w=>w.isVisible()).setSize(1480,1030));
    page.on('pageerror',e=>result.errors.push(e.message));
    if(withModels)await until(()=>page.locator('#model option[value="gpt-oss:20b"]').count(),'local model discovery');
  }
  async function close(){
    if(app)await app.close().catch(()=>{});
    app=null;
  }
  async function dialog(){
    if(!(await page.locator('#requests-dialog').isVisible()))await page.locator('#requests-open').click();
  }
  async function suggest(text, model){
    await dialog();
    const prior=await page.evaluate(()=>localStorage.getItem('fraynix-request-review'));
    await page.locator('#requests-text').fill(text);
    await page.locator('#requests-model').setChecked(Boolean(model));
    if(model)await page.locator('#model').evaluate((select,name)=>{
      select.value=name;
      select.dispatchEvent(new Event('change'));
    },model);
    const start=performance.now();
    await page.locator('#requests-suggest').click();
    await until(async()=>await page.locator('#requests-suggest').isEnabled(),'interpretation');
    const id=await page.evaluate(()=>localStorage.getItem('fraynix-request-review'));
    if(!id||id===prior)throw Error(await page.locator('#requests-status').textContent());
    return {task:await task(id),milliseconds:Math.round(performance.now()-start)};
  }
  async function attach(id,name,bytes){
    await page.locator('#requests-file').setInputFiles({name,mimeType:'application/octet-stream',buffer:Buffer.from(bytes)});
    return until(async()=>{
      const state=await task(id);
      return state.source?.sha256===digest(bytes)&&state;
    },'source attachment');
  }
  async function commit(review){
    const ad=page.locator('#requests-confirm');
    await until(()=>ad.isEnabled(),'explicit confirmation');
    await ad.click();
    const checked=await until(async()=>{
      const found=await task(review.id);
      return found.status==='verified'&&found;
    },'verified artifact');
    await page.locator('#requests-dialog').waitFor({state:'hidden'});
    const artifact=await api('/api/artifact/read',{id:checked.report.id});
    return {task:checked,artifact};
  }
  async function capture(name){
    await page.evaluate(()=>new Promise(resolve=>requestAnimationFrame(()=>requestAnimationFrame(resolve))));
    const png=Buffer.from(await app.evaluate(()=>global.workspaceTest.capture()),'base64');
    await fs.writeFile(path.join(output,name),png);
    result.captures.push({name,sha256:digest(png)});
    await save();
  }
  async function handoff(brief){
    await page.locator('#requests-confirm').click();
    await page.locator('#creation-dialog').waitFor({state:'visible'});
    await until(async()=>await page.locator('#creation-request').inputValue()===brief,'original creation request');
  }

  try{
    await open(false);
    const direct=await suggest('Flip this CSV into JSON');
    assert.equal(direct.task.choice.action,'csv-to-json');
    assert.equal(direct.task.choice.by,'java');
    const packed=await attach(direct.task.id,'incoming.csv',csv);
    await assert.rejects(()=>api('/api/requests/verify',{id:packed.id}));
    await assert.rejects(()=>api('/api/requests/execute',{id:packed.id,reviewSha256:direct.task.reviewSha256}),/source changed/);
    await capture('request-review.png');
    const forward=await commit(packed);
    const json=forward.artifact.contents['converted.json'];
    assert.deepEqual(JSON.parse(json),{columns:['Code','Name','Notes'],rows:[['00017','南','first\nsecond "quoted" line'],['0','Café','']]});
    await capture('round-trip-output.png');
    const french=await suggest('Convertis ce JSON en CSV stp');
    assert.equal(french.task.choice.action,'json-to-csv');
    const reverse=await commit(await attach(french.task.id,'portable.json',json));
    assert.equal(reverse.artifact.contents['converted.csv'],'"Code","Name","Notes"\r\n"00017","南","first\nsecond ""quoted"" line"\r\n"0","Café",""\r\n');
    const exported=await ui('export',{id:reverse.task.id});
    assert.equal(await fs.readFile(exported.path,'utf8'),reverse.artifact.contents['converted.csv']);
    const spanish=await suggest('Convierte este CSV a JSON');
    assert.equal(spanish.task.choice.action,'csv-to-json');
    await page.locator('#requests-close').click();
    const byteReview=await suggest('Show me the bytes');
    const inspected=await commit(await attach(byteReview.task.id,'signature-sample.bin',binary));
    const i=JSON.parse(inspected.artifact.contents['inspection.json']);
    assert.equal(i.signatureHint,'PNG');
    assert.equal(i.sha256,digest(binary));
    assert.deepEqual(i.preview.map(x=>x.decimal),[...binary]);
    assert.equal(i.preview[0].bits,'10001001');
    await capture('byte-inspection.png');
    const unknown=await suggest(savedPhrase);
    assert.equal(unknown.task.choice.action,'clarify');
    await page.locator('#requests-action').selectOption('csv-to-json');
    await until(async()=>(await task(unknown.task.id)).choice.by==='user','explicit correction');
    const taught=await commit(await attach(unknown.task.id,'taught.csv',csv));
    await dialog();
    await page.locator('#requests-learn').click();
    await until(async()=>(await page.locator('#requests-status').innerText()).includes('Phrase remembered'),'phrase memory');
    assert(await app.evaluate(()=>global.workspaceTest.crashEngine()));
    await delay(250);
    await close();
    await open(false);
    await dialog();
    await page.locator('#requests-reopen').click();
    await until(()=>page.locator('#requests-result').isVisible(),'reopen verified output');
    assert.equal((await api('/api/requests/verify',{id:taught.task.id})).artifact.id,taught.task.report.id);
    const learned=await suggest(savedPhrase+'!');
    assert.equal(learned.task.choice.by,'remembered');
    assert(!learned.task.source);
    assert(await page.locator('#requests-confirm').isDisabled());
    await capture('remembered-language.png');
    const bad=await suggest('JSON to CSV');
    const badSource=await attach(bad.task.id,'numeric.json','[{"number":9007199254740993}]');
    await assert.rejects(()=>api('/api/requests/execute',{id:badSource.id,reviewSha256:badSource.reviewSha256}),/type mapping/);
    await assert.rejects(()=>api('/api/requests/verify',{id:badSource.id}));
    result.files={forward:forward.task,reverse:reverse.task,inspected:inspected.task,
      cells:JSON.parse(json),outputDigest:exported.sha256,inspection:i,taught:taught.task,learned:learned.task,
      staleReviewRefused:true,unsupportedMappingRefused:true};

    await close();
    await open(true);
    await page.locator('#model').selectOption('gpt-oss:20b');
    const appIdea=await suggest(appBrief);
    assert.equal(appIdea.task.choice.action,'create-app');
    await handoff(appBrief);
    await page.locator('#creation-close').click();
    const toolIdea=await suggest(toolBrief);
    assert.equal(toolIdea.task.choice.action,'create-tool');
    await handoff(toolBrief);
    const checks=[{selector:'#count',expectText:'0'},{selector:'#add',action:'click'},
      {selector:'#count',expectText:'1'},{selector:'#reset',action:'click'},{selector:'#count',expectText:'0'}];
    await page.locator('#creation-dialog details').evaluate(element=>{element.open=true;});
    await page.locator('#creation-checks').fill(JSON.stringify(checks));
    await page.locator('#creation-build').click();
    const action=await until(async()=> (await api('/api/agency043/state')).tasks.find(x=>x.goal===toolBrief),'creation task');
    const finished=await until(async()=>{
      const row=await api('/api/agency043/read',{id:action.id});
      return ['completed','interrupted'].includes(row.status)&&row;
    },'actual creation test',260000);
    assert.equal(finished.outcome,'verified',finished.reason);
    const ref=finished.completion044.javaGoal.report;
    const made=await api('/api/artifact/read',{id:ref.id});
    assert.deepEqual(made.test.steps,checks);
    assert(made.test.passed);
    result.creation={appBrief:appIdea.task, toolBrief:toolIdea.task,ref,test:made.test,completed:true};
    await capture('created-tool.png');

    for(const model of ['gpt-oss:20b','gemma4:latest']){
      const options=await page.locator('#model option').evaluateAll(rows=>rows.map(x=>x.value));
      if(!options.includes(model)){result.models[model]={available:false,cases:[]};continue;}
      const cases=[];
      result.models[model]={available:true,cases};
      for(const fixture of fixedCases){
        try {
          const review=await suggest(fixture.text,model);
          const c=review.task.choice;
          assert.equal(c.by,'model');
          assert.equal(c.model.model,model);
          assert(Number(c.provider.eval_count)>0);
          assert.equal(review.task.status,'review');
          assert(!review.task.report&&!review.task.source);
          cases.push({fixture,suggested:c.action,choice:c,ms:review.milliseconds,match:c.action===fixture.action});
          console.log(model+' · '+fixture.id+' · '+c.action+' · '+review.milliseconds+' ms');
        } catch(error) {
          cases.push({fixture,error:error.message,match:false});
          console.log(model+' · '+fixture.id+' · '+error.message);
        }
        await save();
      }
    }
    assert(result.models['gpt-oss:20b'].cases.some(x=>x.choice?.provider.eval_count));
    assert.deepEqual(result.errors,[]);
    result.passed=true;
    console.log(JSON.stringify({passed:true,screenshots:result.captures.length,models:Object.fromEntries(Object.entries(result.models)
      .map(([name,x])=>[name,{matched:x.cases.filter(t=>t.match).length,total:x.cases.length}]))}));
  }finally{
    await close();
    await save();
    verify(process.cwd());
  }
}
main().catch(e=>{console.error(e);process.exitCode=1;});
