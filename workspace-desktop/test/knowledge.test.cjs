'use strict';
const {test}=require('node:test'),assert=require('node:assert/strict');
const {publicURL,privateAddress,resultURL,selectLinks}=require('../research-worker.cjs');
const {asksSelf,asksResearch,selfSummary,createKnowledge}=require('../knowledge.cjs');
const {planTools}=require('../policy.cjs');
test('public research rejects local, private, multicast, credentials and private DNS results',async()=>{
 for(const url of ['http://localhost/a','http://127.0.0.1','http://10.2.3.4','http://169.254.1.2','http://100.64.0.1','http://[::1]','http://[ff02::1]','https://user:password@example.org','file:///C:/x'])await assert.rejects(publicURL(url));
 await assert.rejects(publicURL('https://example.org',false,async()=>[{address:'192.168.1.1'}]));
 assert.equal(await publicURL('http://127.0.0.1/a',true),'http://127.0.0.1/a');
 assert.equal(await publicURL('https://example.org',false,async()=>[{address:'93.184.215.14'}]),'https://example.org/');assert(privateAddress('::ffff:127.0.0.1'));
});
test('search results unwrap actual redirect URLs and reject executable or duplicated links',()=>{
 const target='https://reference.example/object';assert.equal(resultURL('/url?q='+encodeURIComponent(target),'https://google.com/search'),target);assert.equal(resultURL('https://www.bing.com/ck/a?u=a1'+Buffer.from(target).toString('base64url'),'https://bing.com/search'),target);
 assert.equal(resultURL('javascript:alert(1)','https://example.org'),null);assert.equal(resultURL('https://bing.com/images','https://example.org'),null);
 assert.deepEqual(selectLinks([{url:target,title:'Reference one'},{url:target,title:'Again'},{url:'https://other.example/page',title:'Reference two'}],'https://bing.com/search').map(x=>x.url),[target,'https://other.example/page']);
});
test('knowledge routing preserves ordinary creation and explicitly targets self or selected-object research',()=>{
 assert(asksSelf('What tools can you use?'));assert(asksSelf('Describe your capabilities and internet access'));assert(!asksSelf('Create a website showing your capabilities'));assert(asksResearch('Research the selected object and find references'));assert(!asksResearch('Build a research website about lighters'));
});
test('typed model tools cannot supply object identity, mass, URLs or success flags',()=>{
 for(const tool of planTools(['self.read','object.read','object.research','object.wait','object.report'])){assert.equal(tool.function.parameters.additionalProperties,false);if(tool.function.name!=='self_read')assert.deepEqual(Object.keys(tool.function.parameters.properties),[]);}
});
test('self explanation separates allowed access, actual receipts, stale host state and tool prerequisites',()=>{
 const s=selfSummary({registeredToolCount:5,persistentObjects:1,activeResearch:[],host:{offline:false,browserReady:true,cameraActive:false},hostFresh:false,lastExternalSource:null});assert(s.includes('allowed'));assert(s.includes('no dated successful'));assert(s.includes('stale'));assert(s.includes('prerequisites'));
});
test('object tools pin trusted selection and cancellation only affects a task-owned job',async()=>{
 const calls=[],api=async(route,args)=>{calls.push({route,args});if(route.endsWith('/object'))return{id:args.id,identityRevision:1,research:[]};if(route.endsWith('/start'))return{id:'job-1',status:'queued'};return{};},aborted=[];
 const bridge=createKnowledge({api,execute:async()=>{},openArtifact:async()=>{},emit:()=>{},activity:()=>{},worker:{abort:id=>aborted.push(id)}});await bridge.select('object-1');const context={};await bridge.action('object.read',{},context);await bridge.select('object-2');await bridge.action('object.research',{},context);assert.equal(calls.find(x=>x.route.endsWith('/start')).args.objectId,'object-1');
 await assert.rejects(bridge.action('object.read',{id:'invented'},context),/no model-supplied/);await bridge.cancelOwned({researchJobId:'joined-job'});assert.deepEqual(aborted,[]);await bridge.cancelOwned(context);assert.deepEqual(aborted,['job-1']);
});
