'use strict';
const {spawn}=require('node:child_process');
const path=require('node:path');

function validPolicy(value){
  if(!value||![1,2].includes(value.observationLimit)||typeof value.refreshStale!=='boolean'||Object.keys(value).some(k=>!['version','observationLimit','refreshStale'].includes(k)))throw Error('Invalid bounded controller policy');
  return value;
}
function failureClass(error){
  const text=String(error?.message||error);
  if(/Stale observation|Control changed; observe again/i.test(text))return 'stale-observation';
  if(/Artifact changed|current version|older artifact version/i.test(text))return 'version-conflict';
  if(/Stopped by you|cancelled/i.test(text))return 'cancelled';
  return 'tool-error';
}
function selectAction(next,policy,latest){
  validPolicy(policy);
  if(policy.refreshStale&&['browser.click','browser.type'].includes(next.tool)&&(!latest||next.args.observation!==latest.observation))
    return {tool:'browser.observe',args:{},transition:'refresh-before-action',requestedTool:next.tool};
  return {...next,transition:'none'};
}
function stageControl(job,artifact,policy,names){
  validPolicy(policy);let allowed=[...names],stage='Choose an action that advances the request';
  if(job.needsArtifact&&job.observed>=policy.observationLimit&&!job.created){allowed=['canvas.write'];stage='The page has been observed. Create the requested working artifact now.';}
  if(job.created){
    if(!artifact?.test){allowed=['canvas.test'];stage=`Artifact ${artifact?.id} version ${artifact?.version} exists. Test its requested behavior now.`;}
    else if(!artifact.test.passed){allowed=job.lastReadVersion===artifact.version?['canvas.write']:['canvas.read','canvas.write'];stage=`Tests failed for ${artifact.id} version ${artifact.version}. Repair the actual behavior using canvas.write with id and expectedVersion=${artifact.version}. After reading source, change it rather than read it again.`;}
    else if(job.needsRecipe&&!job.saved){allowed=['skill.save'];stage='Tests passed. Save the requested reusable recipe now.';}
    else {allowed=['finish'];stage='The requested artifact has passing evidence. Report actual completion now.';}
  }
  return {allowed,stage};
}
function trainController(input,{signal}={}){
  return new Promise((resolve,reject)=>{
    if(signal?.aborted)return reject(Error('Learning cancelled'));
    const env={PATH:process.env.PATH,SystemRoot:process.env.SystemRoot,TEMP:process.env.TEMP,TMP:process.env.TMP};
    const child=spawn('node.exe',['--max-old-space-size=96',path.join(__dirname,'controller-worker.mjs')],{cwd:__dirname,windowsHide:true,env,stdio:['pipe','pipe','pipe']});
    let output='',errors='',settled=false;
    function finish(error,value){if(settled)return;settled=true;clearTimeout(timer);signal?.removeEventListener('abort',cancel);if(child.exitCode===null)child.kill();error?reject(error):resolve(value);}
    const cancel=()=>finish(Error('Learning cancelled'));
    const timer=setTimeout(()=>finish(Error('Controller worker exceeded its 5-second deadline')),5000);
    signal?.addEventListener('abort',cancel,{once:true});
    child.on('error',finish);child.stdin.on('error',error=>finish(error));
    child.stdout.on('data',chunk=>{output+=chunk;if(output.length>65_536)finish(Error('Controller result exceeded its data budget'));});
    child.stderr.on('data',chunk=>{errors=(errors+chunk).slice(-1000);});
    child.on('exit',code=>{if(code!==0)return finish(Error('Controller worker failed: '+errors));try{finish(null,JSON.parse(output));}catch{finish(Error('Controller worker returned invalid data'));}});
    const data=JSON.stringify(input);if(data.length>128_000)return finish(Error('Controller input exceeded its data budget'));child.stdin.end(data);
  });
}
module.exports={validPolicy,failureClass,selectAction,stageControl,trainController};
