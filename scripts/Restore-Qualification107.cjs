'use strict';
// Restore the published, real-browser-qualified bank; no fabricated old runtime identity.
const fs=require('node:fs'),path=require('node:path'),zlib=require('node:zlib'),crypto=require('node:crypto'),assert=require('node:assert/strict');
const sha=b=>crypto.createHash('sha256').update(b).digest('hex');
function workflows(file){
 const b=fs.readFileSync(file);assert.equal(b.subarray(0,4).toString(),'FWRL');assert.equal(b.readInt32BE(4),1);assert.equal(b.readInt32BE(8),b.length-44);assert.equal(b.subarray(12,44).toString('hex'),sha(b.subarray(44)));let at=52;
 const int=()=>{const n=b.readInt32BE(at);at+=4;assert(n>=0&&n<=1_000_000);return n;},str=()=>{const n=int();assert(at+n<=b.length);const s=b.subarray(at,at+n).toString('utf8');at+=n;return s;};const count=int(),out=[];
 for(let i=0;i<count;i++){str();str();const cn=int();for(let j=0;j<cn;j++){str();assert.equal(str(),'workspace.workflow');assert.equal(int(),1);const fn=int(),values={};for(let k=0;k<fn;k++){const key=str();assert(!Object.hasOwn(values,key));values[key]=str();}out.push(JSON.parse(values.payload));}}
 assert.equal(at,b.length);return out;
}
function restore(repo,dest){
 assert(!fs.existsSync(dest),'Preserve previous attempts');const started=Date.now(),prefix='build/restart107/bank-1/';
 const index=JSON.parse(fs.readFileSync(path.join(repo,'docs/readmes/107/original-evidence.json'),'utf8')),files=[];
 for(const f of index.files){if(!f.logicalPath.startsWith(prefix))continue;const packed=fs.readFileSync(path.join(repo,f.path));assert.equal(sha(packed),f.sha256);const raw=zlib.gunzipSync(packed);assert.equal(sha(raw),f.rawSha256);assert.equal(raw.length,f.rawBytes);const relative=f.logicalPath.slice(prefix.length),out=path.resolve(dest,relative);assert(out.startsWith(path.resolve(dest)+path.sep));fs.mkdirSync(path.dirname(out),{recursive:true});fs.writeFileSync(out,raw);files.push({relative,sha256:sha(raw),bytes:raw.length,archive:f.path});}
 assert.equal(files.length,108);return{files,workflows:workflows(path.join(dest,'workflows/tasks.fworld')),started,finished:Date.now(),edition:107,release:'5058a6ca4d5b0165f1cb100c9679a4eba56b25d3',scope:'Unchanged published107 originals, restored from checked lossless archives. Subsequent app launches may append operational history; original source/proof identities remain checked.'};
}
module.exports={restore,workflows};
