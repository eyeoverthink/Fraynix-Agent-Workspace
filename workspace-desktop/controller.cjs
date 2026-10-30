'use strict';
const {spawn}=require('node:child_process');
const {createHash}=require('node:crypto');
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
async function performControlled(next,policy,latest,execute){
  let decision=selectAction(next,policy,latest);
  try{return {decision,result:await execute(decision.tool,decision.args,decision.transition)};}
  catch(error){
    if(policy.refreshStale&&['browser.click','browser.type'].includes(next.tool)&&failureClass(error)==='stale-observation'){
      decision={tool:'browser.observe',args:{},transition:'refresh-after-change',requestedTool:next.tool};
      return {decision,result:await execute(decision.tool,decision.args,decision.transition)};
    }
    throw error;
  }
}
function observeProgress(job,tool,result){
  if(!job||!result?.observation)return false;
  const fingerprint=createHash('sha256').update(JSON.stringify({url:result.url,text:result.text,controls:result.controls})).digest('hex');
  const repeated=tool==='browser.observe'&&job.observationFingerprint===fingerprint;
  job.repeatedReads=repeated?(job.repeatedReads||0)+1:0;job.observationFingerprint=fingerprint;return repeated;
}
function stageControl(job,artifact,policy,names){
  validPolicy(policy);let allowed=[...names],stage='Choose an action that advances the request';
  if(job.needsArtifact&&!job.created)allowed=allowed.filter(name=>name!=='finish');
  if(job.needsArtifact&&(job.repeatedReads||0)>=policy.observationLimit&&!job.created){allowed=allowed.filter(name=>name!=='browser.observe');stage='The latest page observation is unchanged. Use its controls, navigate to needed information, or create the artifact. Do not repeat the same read.';}
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
module.exports={validPolicy,failureClass,selectAction,performControlled,observeProgress,stageControl,trainController};
