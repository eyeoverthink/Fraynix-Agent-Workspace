'use strict';
const test=require('node:test'),assert=require('node:assert/strict'),{createObservations,asksForObservedWorld}=require('../observations.cjs');
test('model cannot supply identities, fake measurements or custom observation gates',async()=>{
 const bridge=createObservations({api:async()=>{throw Error('Should not dispatch');}});
 for(const args of [{worldId:'fake'},{measurement:[1,2]},{threshold:0}])await assert.rejects(bridge.action('observations.start',args),/no model supplied/);
 await assert.rejects(bridge.action('observations.start',{}),/Select a saved world/);
});
test('host pins both native and observation revisions and retains stale refusal',async()=>{
 const calls=[],binding={worldId:'test-world',expectedRevision:3,expectedHash:'abc'};
 const api=async(route,args)=>{calls.push({route,args});if(route.endsWith('/state'))return {nativeBinding:binding,estimate:{revision:9,tracks:{},history:[]},runs:[]};throw Error('Stale observation revision');};
 const bridge=createObservations({api});await bridge.select({worldId:'test-world',datasetId:'pixels',bindings:[{objectKey:'subject',bodyId:'stable-body'}]});assert.equal(bridge.selection().expectedObservationRevision,9);assert.equal(bridge.selection().expectedRevision,3);await assert.rejects(bridge.action('observations.start',{}),/Stale observation/);assert.equal(calls.at(-1).args.expectedHash,'abc');
});
test('footage chat selects the typed observation workflow without changing ordinary world goals',()=>{
 assert(asksForObservedWorld('Explain the observed world and its footage prediction error'));assert(asksForObservedWorld('Update the world estimate'));assert(!asksForObservedWorld('Arrange the blocks and save a skill'));assert(!asksForObservedWorld('Build an HTML footage viewer'));assert(!asksForObservedWorld('Create a website using the observed world'));assert(asksForObservedWorld('Track the selected footage and update its saved world'));
});
test('agent readout separates last accepted world position from a later missing-observation prediction',async()=>{
 const state={nativeBinding:{worldId:'world',expectedRevision:0,expectedHash:'head'},estimate:{revision:7,tracks:{body:{position:[.2,.3],sourceFrame:3}},history:[]},runs:[{id:'actual-run',nativeBinding:{worldId:'world'},status:'completed'}],stale:false,scope:'Observed image-plane state'};
 const bridge=createObservations({api:async route=>route.endsWith('/state')?state:{id:'actual-run',datasetId:'source-camera',nativeBinding:state.nativeBinding,trace:[{ordinal:10,sourceFrame:10,objects:[{bodyId:'body',tracker:{status:'lost'},estimate:{status:'predicted',accepted:false,position:[.4,.3],positionSigma:.2}}]}]}});
 await bridge.select({worldId:'world'});const result=await bridge.action('observations.read',{});assert.equal(result.estimate.revision,7);assert.deepEqual(result.estimate.tracks.body.position,[.2,.3]);assert.equal(result.lastFrame.sourceFrame,10);assert.equal(result.lastFrame.datasetId,'source-camera');assert.equal(result.lastFrame.runId,'actual-run');assert.equal(result.lastFrame.objects[0].trackingStatus,'lost');assert.equal(result.lastFrame.objects[0].accepted,false);assert.equal(result.lastFrame.objects[0].positionSigma,.2);assert(result.scope.includes('not a live camera'));
});

test('037 preserves explicit clocks beside unchanged036 pixel imports and refuses malformed timing before writes',async()=>{
 const calls=[];const bridge=createObservations({api:async(route,args)=>{calls.push({route,args});if(route.endsWith('/import-begin'))return{id:'dataset'};if(route.endsWith('/import-commit'))return{id:'dataset'};return{};}});
 const pack={schema:'fraynix-observation-frames-037-v1',title:'control',cameraKey:'fixed',timingSource:'Measured source clock',seconds:[0,.2,.41,.6,.8],frames:Array.from({length:5},(_,i)=>({sourceFrame:i,image:'test',imageSha256:'test'})),objects:[]};
 await bridge.importPack(pack);assert.equal(calls[0].args.schema,'fraynix-observation-frames-036-v1');assert(!Object.hasOwn(calls[0].args,'seconds'));assert.equal(calls.at(-1).route,'/api/metrics/clock');assert.deepEqual(calls.at(-1).args.seconds,pack.seconds);const n=calls.length;await assert.rejects(bridge.importPack({...pack,seconds:[0,.2,.2,.6,.8]}),/valid source seconds/);assert.equal(calls.length,n);
});

test('chat metric readout preserves qualification and source binding without allowing model fitting or fake measurements',async()=>{
 const state={nativeBinding:{worldId:'w'},estimate:{revision:0,history:[],tracks:{}},runs:[{id:'r',status:'completed',nativeBinding:{worldId:'w'}}],scope:'base'};
 const metric={runId:'r',nativeBinding:{worldId:'w'},rows:[{positionM:[.1,.2],radiusM:.3}],clock:{seconds:[0,.1],source:'Measured clock'},uncertaintyMeaning:'experimental empirical interval',nativeStale:true};
 const bridge=createObservations({api:async(route)=>route.endsWith('/metrics/view')?metric:route.endsWith('/state')?state:{id:'r',trace:[]}});await bridge.select({worldId:'w'});await bridge.metricSelect({runId:'r',geometryId:'g'});const context={};const result=await bridge.action('observations.read',{},context);assert.equal(result.metric.uncertaintyMeaning,metric.uncertaintyMeaning);assert.equal(result.metric.nativeStale,true);assert.deepEqual(context.metricSelection,{runId:'r',geometryId:'g'});await assert.rejects(bridge.action('metrics.fit',{references:[]}),/no model supplied/);
});
