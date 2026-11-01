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
