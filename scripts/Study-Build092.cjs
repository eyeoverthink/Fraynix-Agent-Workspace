'use strict';
// An experimental build receipt, not motion authority. Run after a successful build.
const fs=require('node:fs'),path=require('node:path'),cp=require('node:child_process'),crypto=require('node:crypto'),assert=require('node:assert/strict');
const sha=b=>crypto.createHash('sha256').update(b).digest('hex');
function files(root){
 const tracked=cp.execFileSync('git',['ls-files'],{cwd:root,encoding:'utf8',maxBuffer:16*1024*1024}).trim().split(/\r?\n/);
 const inputs=tracked.filter(p=>p.endsWith('.gradle')||p.endsWith('.lockfile')||p.includes('src/main/')||p.startsWith('workspace-desktop/renderer/')||(p.startsWith('workspace-desktop/') && (p.endsWith('.cjs') && !p.includes('/test/') || p.endsWith('/package.json'))));
 const lib='workspace-engine/build/install/workspace-engine/lib',jars=fs.readdirSync(path.join(root,lib)).filter(p=>p.endsWith('.jar')).map(p=>lib+'/'+p);
 return {inputs:inputs.sort(),artifacts:jars.sort()};
}
function current(root){const result={};for(const [kind,paths]of Object.entries(files(root)))result[kind]=paths.map(p=>{const b=fs.readFileSync(path.join(root,p));return{path:p,bytes:b.length,sha256:sha(b)};});return result;}
function verify(root){
 const receipt=JSON.parse(fs.readFileSync(path.join(root,'build/study092-build.json'),'utf8'));
 assert.deepEqual(current(root),receipt.files,'Build inputs or installed JARs changed; rebuild before starting the study');
 return receipt;
}
if(require.main===module){
 const root=path.resolve(__dirname,'..');
 if(process.argv.includes('--stamp')){
  const own=path.join(root,'workspace-engine/build/install/workspace-engine/lib/workspace-engine.jar'),built=fs.statSync(own).mtimeMs;
  // Desktop sources are loaded directly by Electron and are hashed in the receipt.
  // Compiled Java/build inputs must predate the installed engine artifact.
  for(const file of files(root).inputs)if(!file.startsWith('workspace-desktop/'))assert(fs.statSync(path.join(root,file)).mtimeMs<=built,'Compiled input newer than installed engine: '+file);
  const receipt={source:cp.execFileSync('git',['rev-parse','HEAD'],{cwd:root,encoding:'utf8'}).trim(),createdAt:new Date().toISOString(),files:current(root),scope:'Input/artifact receipt after successful Gradle build; verify again before launch and after the study. Not a replacement for original outcome verification.'};
  fs.writeFileSync(path.join(root,'build/study092-build.json'),JSON.stringify(receipt,null,2)+'\n');console.log('Prepared study build receipt');
 }else{verify(root);console.log('Current inputs and installed artifacts match the study build receipt');}
}
module.exports={verify};
