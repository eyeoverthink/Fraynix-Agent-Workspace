'use strict';
const { randomUUID } = require('node:crypto');
const catalog = [
  ['browser.navigate','Navigate the visible real browser to an http/https URL.','{url}'],
  ['browser.observe','Read bounded page text and interactive controls, with observation ID.','{}'],
  ['browser.click','Click a control from a fresh observation.','{observation,ref}'],
  ['browser.type','Fill a text control from a fresh observation.','{observation,ref,text}'],
  ['canvas.write','Create or revise a real runnable web artifact. index.html required. Existing artifact requires expectedVersion. Opens live preview.','{title,files:{"index.html":"HTML source",...},sources?:[URL],id?:UUID,expectedVersion?:number}'],
  ['canvas.read','Read an existing artifact and current revision.','{id}'],
  ['canvas.observe','Inspect the visible running artifact.','{}'],
  ['canvas.test','Interact with the current artifact and check exact observable results. At least one assertion. Saves real receipt.','{steps:[{selector,action?:"click"|"fill",value?:string,expectText?:string,expectValue?:string,expectCount?:number}]}'],
  ['artifacts.list','List saved artifacts and versions.','{}'],
  ['skills.list','List verified reusable web-artifact recipes.','{}'],
  ['skill.save','Save the current artifact as a reusable recipe only after tests pass.','{name:"lowercase-name",description}'],
  ['skill.run','Recreate and test an existing verified recipe as a new artifact.','{name}'],
  ['memory.search','Search existing project memory through the Fraynix kernel.','{query}'],
  ['finish','Report completion, grounded in actual tool results. Created artifacts must have passed tests.','{summary}']
].map(([name,description,args])=>({name,description,args}));
function webURL(value) {
  const url = new URL(value);
  if (!['http:','https:'].includes(url.protocol) || url.username || url.password) throw Error('Use an HTTP or HTTPS URL without embedded credentials');
  return url.href;
}
function command(raw) {
  if (!raw || typeof raw!=='object' || Array.isArray(raw) || !catalog.some(t=>t.name===raw.tool)) throw Error('Unknown tool');
  if (!raw.args || typeof raw.args!=='object' || Array.isArray(raw.args)) throw Error('Tool args must be an object');
  if (Object.keys(raw).some(k=>!['tool','args','reason'].includes(k))) throw Error('Only tool, args and reason are allowed');
  return raw;
}
class ObservationLease {
  constructor(){this.revision=0;this.id='';}
  invalidate(){this.revision++;this.id='';}
  create(){this.id=randomUUID();return this.id;}
  require(id){if(!this.id || id!==this.id)throw Error('Stale observation. Observe this surface again before acting.');}
}
const text={type:'string'},empty={type:'object',properties:{},additionalProperties:false};
function object(properties,required=Object.keys(properties)){return {type:'object',properties,required,additionalProperties:false};}
const argumentSchemas={
 'browser.navigate':object({url:text}), 'browser.observe':empty,
 'browser.click':object({observation:text,ref:text}), 'browser.type':object({observation:text,ref:text,text}),
 'canvas.write':object({title:{type:'string',minLength:1},files:{type:'object',properties:{'index.html':{type:'string',minLength:30}},required:['index.html'],additionalProperties:text},sources:{type:'array',items:text},id:text,expectedVersion:{type:'integer',minimum:1}},['title','files']),
 'canvas.read':object({id:text}),'canvas.observe':empty,
 'canvas.test':object({steps:{type:'array',minItems:1,maxItems:24,items:{...object({selector:text,action:{type:'string',enum:['click','fill']},value:text,expectText:text,expectValue:text,expectCount:{type:'integer',minimum:0}},['selector']),anyOf:[{required:['expectText']},{required:['expectValue']},{required:['expectCount']}]}}}),
 'artifacts.list':empty,'skills.list':empty,
 'skill.save':object({name:{type:'string',pattern:'^[a-z][a-z0-9-]{2,55}$'},description:text}),
 'skill.run':object({name:text}),'memory.search':object({query:text}),'finish':object({summary:text})
};
function planTools(allowed){return catalog.filter(t=>allowed.includes(t.name)).map(t=>({type:'function',function:{name:t.name.replace('.','_'),description:t.description,parameters:argumentSchemas[t.name]}}));}
module.exports={catalog,webURL,command,ObservationLease,planTools};
