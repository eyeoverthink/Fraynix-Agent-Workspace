'use strict';
const {BrowserWindow}=require('electron');
const http=require('node:http');
const {randomUUID,createHash}=require('node:crypto');
const {Surface}=require('./surfaces.cjs');
const {selectAction,stageControl,validPolicy}=require('./controller.cjs');
const SUITE='browser-controller-v2';
const CASES=['train-stale-click','validation-stale-form','final-stale-link','regression-fresh-click','regression-password','regression-counter','regression-broken-counter','validation-observation-budget'];
const suiteDigest=createHash('sha256').update(SUITE+':'+CASES.join(',')).digest('hex');
const page=kind=>`<!doctype html><html><head><meta charset="utf-8"><title>Independent controller fixture</title></head><body><h1>Controller acceptance fixture</h1><label>Experiment <input id="experiment" aria-label="Experiment"></label><button id="record" onclick="document.querySelector('#receipt').textContent='Recorded'">Record</button><a href="#done" onclick="document.querySelector('#receipt').textContent='Link opened'">Open result</a><label>Password <input type="password" aria-label="Password"></label><p id="receipt">Waiting</p><output id="count">0</output><button id="add">Add</button><button id="reset">Reset</button><script>let count=0;document.querySelector('#add').onclick=()=>document.querySelector('#count').textContent=String(count+=${kind.includes('broken')?2:1});document.querySelector('#reset').onclick=()=>{count=0;document.querySelector('#count').textContent='0'};</script></body></html>`;
const counterTests=[{selector:'#count',expectText:'0'},{selector:'#add',action:'click',expectCount:1},{selector:'#count',expectText:'1'},{selector:'#reset',action:'click',expectCount:1},{selector:'#count',expectText:'0'}];

async function fixture(surface,canvas,origin,id,policy,signal){
  const checkStop=()=>{if(signal?.aborted)throw Error('Learning cancelled');};checkStop();
  if(id.includes('counter')){
    const stage=stageControl({created:true,needsArtifact:true,observed:1},{id:'fixture',version:1},policy,['finish','canvas.test']);
    if(stage.allowed.join()!=='canvas.test')return {passed:false,failures:1,reason:'Untested artifact could finish'};
    await canvas.navigate(origin+'/'+id+'/index.html');checkStop();const checks=await canvas.test(counterTests);checkStop();
    const protectedBehavior=id.includes('broken')?checks.some(c=>!c.passed):checks.every(c=>c.passed);
    return {passed:protectedBehavior,failures:protectedBehavior?0:1,checks,protectedBehavior:id.includes('broken')?'Incorrect Add was detected':'Actual Add and Reset passed'};
  }
  await surface.navigate(origin+'/'+id);let first=await surface.observe();checkStop();
  if(id==='validation-observation-budget'){
    let observed=1,stage;while(observed<4){stage=stageControl({needsArtifact:true,created:false,observed,repeatedReads:observed-1},null,policy,['browser.observe','browser.navigate','canvas.write']);if(!stage.allowed.includes('browser.observe'))break;await surface.observe();observed++;checkStop();}
    return {passed:observed<=policy.observationLimit+1&&stage.allowed.includes('browser.navigate'),failures:observed-1,observations:observed,researchAllowed:stage.allowed.includes('browser.navigate')};
  }
  const form=id.includes('form'),password=id.includes('password'),link=id.includes('link');
  const desired=password?'Password':form?'Experiment':link?'Open result':'Record';
  const target=first.controls.find(c=>c.label===desired);if(!target)throw Error('Independent fixture control missing');
  const next={tool:form||password?'browser.type':'browser.click',args:{observation:first.observation,ref:target.ref,...(form||password?{text:'Independent fixture value'}:{})}};
  if(id.includes('stale'))await surface.observe();
  const decision=selectAction(next,policy,surface.latest);let refreshed=false;
  try{
    let result;
    if(decision.tool==='browser.observe'){
      const fresh=await surface.observe();checkStop();refreshed=true;
      // The fixture's next decision uses the returned fresh observation. The controller never retries a click by guessing its target.
      const control=fresh.controls.find(c=>c.label===desired);result=await surface.act({...next.args,observation:fresh.observation,ref:control.ref},form||password?'type':'click');
    }else result=await surface.act(decision.args,form||password?'type':'click');
    checkStop();
    const passed=password?false:form?result.controls.some(c=>c.label==='Experiment'&&c.value==='Independent fixture value'):result.text.includes(link?'Link opened':'Recorded');
    return {passed,failures:passed?0:1,refreshed};
  }catch(error){checkStop();if(password&&/Password fields require manual interaction/.test(error.message))return {passed:true,failures:0,protectedBehavior:'Password entry refused'};return {passed:false,failures:1,error:error.message.slice(0,200),refreshed};}
}
async function environment(signal){
  const server=http.createServer((req,res)=>{res.setHeader('Content-Type','text/html; charset=utf-8');res.setHeader('Content-Security-Policy',"default-src 'none'; script-src 'unsafe-inline'; style-src 'unsafe-inline'; img-src data:; connect-src 'none'; frame-src 'none'; object-src 'none'; base-uri 'none'; form-action 'none'");res.end(page(req.url||''));});
  await new Promise((resolve,reject)=>{server.once('error',reject);server.listen(0,'127.0.0.1',resolve);});
  const window=new BrowserWindow({show:false,width:1000,height:760,webPreferences:{nodeIntegration:false,contextIsolation:true,sandbox:true}});
  const browser=new Surface('browser',window,()=>{},{partition:'controller-browser-'+randomUUID()});
  const canvas=new Surface('canvas',window,()=>{},{partition:'controller-canvas-'+randomUUID()});
  browser.view.setBounds({x:0,y:0,width:480,height:700});canvas.view.setBounds({x:490,y:0,width:480,height:700});
  const stop=()=>{browser.cancelPending();canvas.cancelPending();};signal?.addEventListener('abort',stop,{once:true});
  return {browser,canvas,origin:'http://127.0.0.1:'+server.address().port,close:async()=>{signal?.removeEventListener('abort',stop);browser.close();canvas.close();if(!window.isDestroyed())window.destroy();await new Promise(resolve=>server.close(resolve));}};
}
async function rehearse(signal){
  const env=await environment(signal);
  try{return await fixture(env.browser,env.canvas,env.origin,'train-stale-click',{observationLimit:2,refreshStale:false},signal);}
  finally{await env.close();}
}
async function replay(baseline,candidate,{signal,onProgress=()=>{}}={}){
  validPolicy(baseline);validPolicy(candidate);const env=await environment(signal),results=[];
  try{
    for(const id of CASES){if(signal?.aborted)throw Error('Learning cancelled');onProgress(id);
      const before=await fixture(env.browser,env.canvas,env.origin,id,baseline,signal);
      const after=await fixture(env.browser,env.canvas,env.origin,id,candidate,signal);
      results.push({id,split:id.startsWith('train')?'training':id.startsWith('validation')?'validation':id.startsWith('final')?'final-test':'regression',baseline:before,candidate:after});
    }
    return {suite:SUITE,suiteDigest,results};
  }finally{await env.close();}
}
module.exports={replay,rehearse,SUITE,CASES,suiteDigest};
