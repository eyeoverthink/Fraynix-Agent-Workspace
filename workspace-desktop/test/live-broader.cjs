'use strict';
const {_electron}=require('playwright');const assert=require('node:assert/strict');const fs=require('node:fs/promises');const os=require('node:os');const path=require('node:path');const {serve}=require('./validation-fixtures.cjs');
async function main(){
  const project=process.env.FRAYNIX_VALIDATE_EXISTING_PROJECT||await fs.mkdtemp(path.join(os.tmpdir(),'fraynix-live-varied-'));const output=path.resolve(__dirname,'..','test-output');await fs.mkdir(output,{recursive:true});
  const model=process.env.FRAYNIX_LIVE_MODEL||'gpt-oss:20b',fixture=await serve(60337),env={...process.env,FRAYNIX_WORKSPACE_PROJECT:project,FRAYNIX_WORKSPACE_TEST:'1'};delete env.ELECTRON_RUN_AS_NODE;
  const app=await _electron.launch({args:[path.resolve(__dirname,'..','main.cjs')],env,timeout:60000}),results=[];
  const cases=[
    {name:'quantity-calculator',title:'Varied quantity calculator',recipe:'varied-calculator',goal:'Create a NEW attractive app titled Varied quantity calculator. Unit price is 2.37. Use number input #qty initially 1, button #calculate, output #total initially 0.00. Clicking Calculate must multiply quantity by the price with exactly two decimal places. Test quantity 2 and then 5. Save verified recipe varied-calculator and finish.',steps:[{selector:'#qty',action:'fill',value:'4',expectValue:'4'},{selector:'#calculate',action:'click',expectCount:1},{selector:'#total',expectText:'9.48'},{selector:'#qty',action:'fill',value:'0',expectValue:'0'},{selector:'#calculate',action:'click',expectCount:1},{selector:'#total',expectText:'0.00'},{selector:'#qty',action:'fill',value:'3',expectValue:'3'},{selector:'#calculate',action:'click',expectCount:1},{selector:'#total',expectText:'7.11'}]},
    {name:'catalog-research',title:'Researched field catalog',recipe:'researched-catalog',goal:`Navigate the browser to ${fixture.origin}/catalog and read the actual catalog. Follow Read field details and read the reference code and quantity. Create a NEW attractive app titled Researched field catalog using those observed values. Show all three actual product names in #products, actual reference code in #reference, actual quantity in #quantity. Add a text input #filter that filters .product rows as you type (empty shows all). Test all three product rows, the reference, and filtering. Save verified recipe researched-catalog and finish. Do not guess the page values.`,steps:[...fixture.data.names.map(name=>({selector:'#products',expectText:name})),{selector:'#reference',expectText:fixture.data.code},{selector:'#quantity',expectText:String(fixture.data.quantity)},{selector:'.product',expectCount:3},{selector:'#filter',action:'fill',value:'Birch',expectValue:'Birch'},{selector:'.product:visible',expectCount:1},{selector:'#filter',action:'fill',value:'',expectValue:''},{selector:'.product:visible',expectCount:3}]}
  ];
  try{
    await app.evaluate(async()=>{for(let i=0;i<200&&!global.workspaceTest;i++)await new Promise(r=>setTimeout(r,100));if(!global.workspaceTest)throw Error('Workspace not ready');});
    if(!process.env.FRAYNIX_VALIDATE_EXISTING_PROJECT)assert((await app.evaluate(()=>global.workspaceTest.learnFromEpisodes({proof:true}))).accepted);
    for(const task of cases){
      const started=Date.now();let result={name:task.name,goal:task.goal,passed:false};
      try{
        console.log('LIVE_START',task.name);
        const before=(await app.evaluate(()=>global.workspaceTest.api('/api/state'))).learning.episodeCount;
        if(!process.env.FRAYNIX_VALIDATE_EXISTING_PROJECT){
        await app.evaluate((_,input)=>{global.variedDone=false;global.workspaceTest.startAgent(input.goal,input.model).finally(()=>{global.variedDone=true;});},{goal:task.goal,model});
        let last='';for(let i=0;i<240;i++){await new Promise(r=>setTimeout(r,2000));const state=await app.evaluate(()=>({done:global.variedDone}));const log=(await fs.readFile(path.join(project,'events.jsonl'),'utf8')).trim().split('\n').map(JSON.parse);const event=log.at(-1);const summary=event.kind+': '+JSON.stringify(event.data).slice(0,160);if(last!==summary){console.log(task.name,summary);last=summary;}if(state.done)break;if(i===239){await app.evaluate(()=>global.workspaceTest.stopAgent());throw Error('Live task timed out');}}
        }
        const state=await app.evaluate(()=>global.workspaceTest.api('/api/state'));if(!process.env.FRAYNIX_VALIDATE_EXISTING_PROJECT)assert(state.learning.episodeCount>before);
        const artifact=state.artifacts.filter(a=>a.title===task.title).at(-1);assert(artifact,'Expected new artifact was not created');
        const episode=state.learning.episodes.find(e=>e.model===model&&e.steps.some(s=>s.artifactId===artifact.id));assert(episode&&episode.status==='verified','Model task did not finish verified');
        assert(state.skills.some(s=>s.name===task.recipe),'Model did not save verified recipe');
        await app.evaluate((_,id)=>global.workspaceTest.execute('canvas.open',{id}),artifact.id);
        // DOM/CSS checks below cover filtering visibility; canvas.test accepts standard CSS only.
        const steps=task.name==='catalog-research'?task.steps.filter(s=>!s.selector.includes(':visible')&&s.selector!=='#products'):task.steps;
        const acceptance=await app.evaluate((_,steps)=>global.workspaceTest.execute('canvas.test',{steps}),steps);
        if(task.name==='catalog-research'){
          const contents=await app.evaluate(({webContents})=>webContents.fromId(global.workspaceTest.surfaces().canvas).executeJavaScript('document.querySelector("#products").innerText'));
          for(const name of fixture.data.names)acceptance.checks.push({name:'Observed product '+name,passed:contents.includes(name)});
          await app.evaluate(()=>global.workspaceTest.execute('canvas.test',{steps:[{selector:'#filter',action:'fill',value:'Birch',expectValue:'Birch'}]}));
          const filtered=await app.evaluate(({webContents})=>webContents.fromId(global.workspaceTest.surfaces().canvas).executeJavaScript('Array.from(document.querySelectorAll(".product")).filter(e=>e.getBoundingClientRect().height>0&&getComputedStyle(e).visibility!=="hidden").map(e=>e.innerText)'));
          acceptance.checks.push({name:'Actual visible filter result',passed:filtered.length===1&&filtered[0].includes(fixture.data.names[1]),actual:filtered});
          await app.evaluate(()=>global.workspaceTest.execute('canvas.test',{steps:[{selector:'#filter',action:'fill',value:'',expectValue:''}]}));
          const restored=await app.evaluate(({webContents})=>webContents.fromId(global.workspaceTest.surfaces().canvas).executeJavaScript('Array.from(document.querySelectorAll(".product")).filter(e=>e.getBoundingClientRect().height>0&&getComputedStyle(e).visibility!=="hidden").length'));
          acceptance.checks.push({name:'Clearing filter restores all products',passed:restored===3,actual:restored});
          acceptance.passed=acceptance.checks.every(c=>c.passed);
        }
        result={...result,artifactId:artifact.id,version:artifact.version,episodeId:episode.id,policy:episode.policy,acceptanceChecks:acceptance.checks,passed:acceptance.passed};
        const events=(await fs.readFile(path.join(project,'events.jsonl'),'utf8')).trim().split('\n').map(JSON.parse);const start=events.findLastIndex(e=>e.kind==='agent_completed'&&e.data.episodeId===episode.id);assert(start>=0,'No model completion event');result.goal=events[start].data.goal;result.tools=episode.steps.map(s=>s.tool);result.acceptanceOnReopenedOutput=!!process.env.FRAYNIX_VALIDATE_EXISTING_PROJECT;
        if(task.name==='catalog-research'){assert(result.tools.includes('browser.navigate'));assert(result.tools.includes('browser.click'));}
        assert(result.passed,'Independent acceptance failed');
      }catch(error){result.error=error.message;}
      result.elapsedMs=Date.now()-started;results.push(result);
      const png=await app.evaluate(()=>global.workspaceTest.capture());await fs.writeFile(path.join(output,'live-'+task.name+'.png'),Buffer.from(png,'base64'));
      await fs.writeFile(path.join(output,'live-broader.json'),JSON.stringify({date:new Date().toLocaleDateString('en-CA',{timeZone:'America/Los_Angeles'}),model,passed:results.every(r=>r.passed),results,scope:'Actual local-model workflows; independent acceptance values differ from requested self-tests. Two task instances, not a general capability benchmark.'},null,2));
      console.log('LIVE_RESULT',JSON.stringify({name:task.name,passed:result.passed,error:result.error}));
    }
    if(results.some(r=>!r.passed))process.exitCode=1;
  }finally{await app.close();await fixture.close();}
}
main().catch(error=>{console.error(error);process.exitCode=1;});
