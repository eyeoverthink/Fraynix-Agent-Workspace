'use strict';
// Restore published102 evidence into NEW isolated projects. Never touches an existing destination.
const fs=require('node:fs'),path=require('node:path'),zlib=require('node:zlib'),crypto=require('node:crypto'),assert=require('node:assert/strict');
const repo=path.resolve(__dirname,'..'),out=path.join(repo,'build/reuse103'),sha=b=>crypto.createHash('sha256').update(b).digest('hex');
function prepare(){
 assert(!fs.existsSync(out),'Keep previous trials; choose a separate checkout or move their complete folder explicitly');
 const manifest=JSON.parse(fs.readFileSync(path.join(repo,'docs/readmes/102/original-evidence.json'),'utf8')),receipt=[];
 for(const [name,prefix,result] of [['explorer','cost102-baseline-attempt2','result-recovered.json'],['summary','summary102-attempt3','result-mixed.json']]){
  const root=path.join(out,name),entries=manifest.files.filter(f=>f.logicalPath.startsWith(prefix+'/project/')||f.logicalPath===prefix+'/'+result);assert(entries.length>10,'Published starting evidence missing');const files=[];
  for(const f of entries){const compressed=fs.readFileSync(path.join(repo,f.path));assert.equal(sha(compressed),f.sha256);const raw=zlib.gunzipSync(compressed);assert.equal(raw.length,f.rawBytes);assert.equal(sha(raw),f.rawSha256);
   let relative=f.logicalPath.slice(prefix.length+1);if(relative===result)relative='original102.json';if(relative.split('/').some(p=>['desktop-profile','runtime-042','.northstar-kernel'].includes(p))||relative.endsWith('.lock'))continue;
   const target=path.resolve(root,relative);assert(target.startsWith(root+path.sep));fs.mkdirSync(path.dirname(target),{recursive:true});fs.writeFileSync(target,raw);files.push({path:relative,sha256:sha(raw),bytes:raw.length,original:f.logicalPath});
  }receipt.push({name,source:'Frozen edition102 archive',target:root,files});
 }
 fs.writeFileSync(path.join(out,'copy-receipt.json'),JSON.stringify(receipt,null,2)+'\n');console.log('Restored two isolated original projects with compressed and original byte checks.');return receipt;
}
if(require.main===module)prepare();module.exports={prepare};
