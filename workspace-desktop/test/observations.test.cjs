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
