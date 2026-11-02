'use strict';
const {_electron}=require('playwright');const assert=require('node:assert/strict');const path=require('node:path');const fs=require('node:fs/promises');const os=require('node:os');
async function launch(project){const env={...process.env,FRAYNIX_WORKSPACE_PROJECT:project,FRAYNIX_WORKSPACE_TEST:'1'};delete env.ELECTRON_RUN_AS_NODE;const app=await _electron.launch({args:[path.resolve(__dirname,'..','main.cjs')],env,timeout:60000});const page=await app.firstWindow();await app.evaluate(async()=>{for(let i=0;i<200&&!global.workspaceTest;i++)await new Promise(r=>setTimeout(r,100));if(!global.workspaceTest)throw Error('Workspace did not become ready');});return{app,page};}
async function main(){
  const output=path.resolve(__dirname,'..','test-output');await fs.mkdir(output,{recursive:true});
  const project=await fs.mkdtemp(path.join(os.tmpdir(),'fraynix-workspace-proof-'));let {app,page}=await launch(project);const checks=[];
  try{
    page.on('pageerror',error=>console.error('SHELL_PAGE_ERROR',error.message));
    assert.equal(await page.locator('.layout').evaluate(e=>getComputedStyle(e).display),'grid');checks.push('Committed theme loads and the main application retains its grid layout');
    await page.getByRole('button',{name:'Run guided proof'}).click();
    await page.getByRole('status').filter({hasText:'Guided proof passed'}).waitFor({timeout:60000});
    checks.push('Visible guided proof: real browser fill/click, runnable artifact, exact assertions, verified recipe');
    const guidedPng=await app.evaluate(()=>global.workspaceTest.capture());await fs.writeFile(path.join(output,'workspace-guided.png'),Buffer.from(guidedPng,'base64'));
    await page.getByRole('button',{name:'Source',exact:true}).click();assert((await page.getByLabel('Artifact source code').inputValue()).includes('querySelector'));
    await page.getByRole('button',{name:'Live preview',exact:true}).click();
    const beforeWidth=await page.locator('#browser-surface').evaluate(e=>e.getBoundingClientRect().width);
    await page.locator('#expand-browser').click();try{await page.waitForFunction(w=>document.querySelector('#browser-surface').getBoundingClientRect().width>w*1.5,beforeWidth,{timeout:4000});}catch(error){console.log('EXPANSION_DIAGNOSTIC',beforeWidth,await page.evaluate(()=>({body:document.body.className,width:innerWidth,browser:document.querySelector('#browser-surface').getBoundingClientRect().toJSON(),button:document.querySelector('#expand-browser').textContent})));await fs.writeFile(path.join(output,'expansion-failure.png'),Buffer.from(await app.evaluate(()=>global.workspaceTest.capture()),'base64'));throw error;}const expandedWidth=await page.locator('#browser-surface').evaluate(e=>e.getBoundingClientRect().width);assert(expandedWidth>beforeWidth*1.5);
    await page.locator('#expand-browser').click();
    await app.evaluate(({BrowserWindow})=>BrowserWindow.getAllWindows()[0].setSize(1100,800));
    const narrow=await page.evaluate(()=>({width:innerWidth,documentWidth:document.documentElement.scrollWidth}));assert(narrow.documentWidth<=narrow.width);
    await app.evaluate(({BrowserWindow})=>BrowserWindow.getAllWindows()[0].setSize(1600,1040));checks.push('Source view, browser expansion and 1100px desktop layout work');
    const external=await app.evaluate(()=>global.workspaceTest.execute('browser.navigate',{url:'https://example.com'}));assert.equal(new URL(external.url).hostname,'example.com');assert(external.text.length>50);checks.push('Real external HTTPS browsing works');
    await assert.rejects(()=>app.evaluate((_,id)=>global.workspaceTest.execute('browser.click',{observation:id,ref:'0'}),'old-observation'),/Stale observation/);checks.push('Stale browser observations are rejected');
    const isolation=await app.evaluate(async({webContents})=>{const ids=global.workspaceTest.surfaces();return Promise.all(Object.values(ids).map(id=>webContents.fromId(id).executeJavaScript('({node:typeof require,bridge:typeof window.workspace})')));});
    assert(isolation.every(x=>x.node==='undefined'&&x.bridge==='undefined'));checks.push('Browser and artifact renderers have no Node or privileged workspace bridge');
    const auth=await app.evaluate(async()=>({status:(await fetch(global.workspaceTest.origin()+'/api/state')).status}));assert.equal(auth.status,403);checks.push('Unprivileged HTTP cannot call the Java control plane');
    const broken='<h1>Repair fixture</h1><output id="count">0</output><button id="add">Add</button><script>document.querySelector("#add").onclick=()=>document.querySelector("#count").textContent="2"</script>';
    const artifact=await app.evaluate((_,files)=>global.workspaceTest.execute('canvas.write',{title:'Repair and verification',files}),{'index.html':broken});
    const steps=[{selector:'#count',expectText:'0'},{selector:'#add',action:'click'},{selector:'#count',expectText:'1'}];
    const failed=await app.evaluate((_,steps)=>global.workspaceTest.execute('canvas.test',{steps}),steps);assert.equal(failed.passed,false);
    await assert.rejects(()=>app.evaluate(()=>global.workspaceTest.execute('skill.save',{name:'unverified-fixture',description:'Must be rejected'})),/passing tests/);
    const fixed=broken.replace('textContent="2"','textContent="1"');
    const revised=await app.evaluate((_,input)=>global.workspaceTest.execute('canvas.write',input),{id:artifact.id,expectedVersion:1,title:'Repair and verification',files:{'index.html':fixed}});assert.equal(revised.version,2);
    const passed=await app.evaluate((_,steps)=>global.workspaceTest.execute('canvas.test',{steps}),steps);assert.equal(passed.passed,true);
    await app.evaluate(()=>global.workspaceTest.execute('skill.save',{name:'repair-proof',description:'A corrected and tested counter recipe'}));checks.push('Failed behavior is caught; revision 2 repairs it; only passing evidence can become a skill');
    const png=await app.evaluate(()=>global.workspaceTest.capture());
    await fs.writeFile(path.join(output,'workspace-proof.png'),Buffer.from(png,'base64'));
    await app.close();({app,page}=await launch(project));
    const restored=await app.evaluate(()=>global.workspaceTest.api('/api/state'));assert(restored.artifacts.some(a=>a.id===artifact.id&&a.version===2));assert(restored.skills.some(s=>s.name==='repair-proof'));
    const replay=await app.evaluate(()=>global.workspaceTest.execute('skill.run',{name:'repair-proof'}));assert.equal(replay.passed,true);checks.push('Project restart restores saved artifacts and skills; verified recipe reruns and passes');
    const takeover=await app.evaluate(async()=>{const before=(await global.workspaceTest.api('/api/state')).artifacts.length;const task=global.workspaceTest.startAgent('Create a new artifact','llama3.2:latest');await global.workspaceTest.stopAgent();await task;return {before,after:(await global.workspaceTest.api('/api/state')).artifacts.length};});assert.equal(takeover.after,takeover.before);checks.push('Immediate takeover prevents any subsequent agent mutation');
    await app.evaluate(()=>global.workspaceTest.launchGame());
    for(let i=0;i<100;i++){try{await fs.access(path.join(project,'game-lab-start.png'));break;}catch{await new Promise(r=>setTimeout(r,250));}}
    await fs.copyFile(path.join(project,'game-lab-start.png'),path.join(output,'game-lab.png'));await app.evaluate(()=>global.workspaceTest.stopTestGame());checks.push('Preserved Game Lab renders using the successor project directory');
    await assert.rejects(()=>app.evaluate(()=>global.workspaceTest.execute('canvas.write',{title:'Bounded renderer fixture',files:{'index.html':'<script>while(true){}</script>'}})),/timed out/);
    const afterHang=await app.evaluate(()=>global.workspaceTest.api('/api/state')),hung=afterHang.artifacts.find(a=>a.title==='Bounded renderer fixture');assert(hung);
    await app.evaluate((_,id)=>global.workspaceTest.execute('canvas.write',{id,expectedVersion:1,title:'Bounded renderer fixture',files:{'index.html':'<h1 id="recovered">Recovered</h1>'}}),hung.id);
    const recovered=await app.evaluate(()=>global.workspaceTest.execute('canvas.test',{steps:[{selector:'#recovered',expectText:'Recovered'}]}));assert(recovered.passed);checks.push('An infinite-loop artifact times out; the host survives and a corrected version runs');
    const result={date:new Date().toISOString(),passed:true,checks,project,artifact:artifact.id};await fs.writeFile(path.join(output,'integration.json'),JSON.stringify(result,null,2));console.log(JSON.stringify(result,null,2));
  }finally{await app.close();}
}
main().catch(error=>{console.error(error);process.exitCode=1;});
