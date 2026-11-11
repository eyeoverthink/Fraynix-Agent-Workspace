'use strict';
// Presentation boundary only. Java's existing original-evidence verifiers remain authoritative.
const {createHash}=require('node:crypto');
const routes={chat:['/api/agency043/read','/api/agency043/verify'],workflow:['/api/workflows/read','/api/workflows/verify-archive'],child:['/api/workflows/child-read','/api/workflows/verify-child']};
function digest(contents){const hash=createHash('sha256');for(const name of Object.keys(contents).sort())hash.update(name).update('\0').update(contents[name]).update('\0');return hash.digest('hex');}
async function openHistoryResult(input,{api,openArtifact,showPreview}){
 if(!input||Object.keys(input).some(k=>!['kind','id'].includes(k))||!Object.hasOwn(routes,input.kind)||! /^[a-f0-9]{8}-[a-f0-9]{4}-[a-f0-9]{4}-[a-f0-9]{4}-[a-f0-9]{12}$/i.test(input.id||''))throw Error('Choose a retained history record');
 const [read,verify]=routes[input.kind],record=await api(read,{id:input.id});
 if(record.id!==input.id||record.status!=='completed'||input.kind==='chat'&&record.outcome!=='verified')throw Error('Choose a completed task with a verified saved workflow result');
 const ref=input.kind==='chat'?record.completion044?.javaWorkflow?.report:record.report;
 if(!ref||!Number.isInteger(ref.version)||ref.version<1||!ref.id||! /^[a-f0-9]{64}$/.test(ref.sha256||''))throw Error('This record has no supported saved workflow result');
 const proof=await api(verify,{id:input.id});if(proof.passed!==true||proof.id!==input.id)throw Error('Original history evidence did not verify');
 const artifact=await api('/api/artifact/read',{id:ref.id});
 if(artifact.id!==ref.id||artifact.version!==ref.version||!artifact.contents||digest(artifact.contents)!==ref.sha256)throw Error('The saved result changed; its original history remains available');
 const opened=await openArtifact(artifact);showPreview();return{recordId:input.id,kind:input.kind,artifact:opened,proof,scope:'Original stored evidence rechecked and exact retained report opened. Not a new observation or permission to replay actions.'};
}
module.exports={openHistoryResult};
