// Pinned Fraymus prototype updates run outside the privileged desktop process.
import fs from 'node:fs';
import {createHash} from 'node:crypto';
const base=new URL('./vendor/fraymus-brain/',import.meta.url);
const provenance=JSON.parse(fs.readFileSync(new URL('provenance.json',base),'utf8'));
for(const file of provenance.files){const bytes=fs.readFileSync(new URL(file.path,base));if(createHash('sha256').update(bytes.toString('utf8').replaceAll('\r\n','\n')).digest('hex')!==file.sha256)throw Error('Pinned learning source changed: '+file.path);}
const {classifyAndUpdateNcm,embedVector64}=await import(new URL('sovereignEcosystem.js',base));
let text='';for await(const chunk of process.stdin){text+=chunk;if(text.length>128_000)throw Error('Input too large');}
const input=JSON.parse(text),prior=input.activePolicy;
if(![1,2].includes(prior.observationLimit)||typeof prior.refreshStale!=='boolean')throw Error('Invalid parent policy');
const ids=['stale-observation','repeated-observation'];
let memory=input.controllerMemory||{version:1,alpha:.62,totalUpdates:0,classes:ids.map(id=>({id,label:id,seedText:id,centroid:Array.from(embedVector64(id)),subPrototypes:[Array.from(embedVector64(id))],samples:1}))};
const episodes=[],counts=Object.fromEntries(ids.map(id=>[id,0])),examples={},trained=new Set(memory.episodeIds||[]);
for(const episode of input.episodes||[]){
  if(!['failed','verified','completed-unverified'].includes(episode.status))continue;
  let used=false;
  for(const step of episode.steps||[]){
    if(!ids.includes(step.failure))continue;
    if(!trained.has(episode.id)){const previous=memory.totalUpdates||0;memory=classifyAndUpdateNcm(memory,`${step.failure} ${step.tool}`,step.failure);memory.totalUpdates=previous+1;}
    counts[step.failure]++;examples[step.failure]=`${step.failure} ${step.tool}`;used=true;
  }
  if(used){episodes.push(episode.id);trained.add(episode.id);}
}
// Read back routing from the updated prototypes. This additional call returns new state;
// discard its unsupervised update and retain only the supervised memory above.
const routes=Object.fromEntries(Object.entries(examples).map(([id,query])=>[id,classifyAndUpdateNcm(memory,query).lastClassification.id]));
const policy={observationLimit:counts['repeated-observation']&&routes['repeated-observation']==='repeated-observation'?1:prior.observationLimit,refreshStale:counts['stale-observation']&&routes['stale-observation']==='stale-observation'?true:prior.refreshStale};
const clean={version:1,alpha:.62,totalUpdates:memory.totalUpdates,episodeIds:[...trained].slice(-256),classes:memory.classes.map(c=>({id:c.id,label:c.id,seedText:c.id,centroid:c.centroid,subPrototypes:c.subPrototypes,samples:c.samples}))};
const changed=policy.observationLimit!==prior.observationLimit||policy.refreshStale!==prior.refreshStale;
process.stdout.write(JSON.stringify({proposal:changed?{policy,episodes,controllerMemory:clean}:null,counts,routes,prototypeUpdates:clean.totalUpdates,kernelCommit:provenance.commit,mechanism:'Supervised Fraymus nearest-class-mean prototype updates plus bounded rule proposal; no LLM weight update'}));
