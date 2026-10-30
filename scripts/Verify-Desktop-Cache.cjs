'use strict';
const fs=require('node:fs'),path=require('node:path');
function verifyCache(root){
 const read=file=>JSON.parse(fs.readFileSync(file,'utf8'));
 const lock=read(path.join(root,'package-lock.json')),pkg=read(path.join(root,'package.json')),installed=read(path.join(root,'node_modules','.package-lock.json'));
 for(const kind of ['dependencies','devDependencies']){
  const desired=pkg[kind]||{},recorded=lock.packages[''][kind]||{};
  if(JSON.stringify(Object.entries(desired).sort())!==JSON.stringify(Object.entries(recorded).sort()))throw Error('Package manifest and lock disagree');
 }
 let count=0;
 for(const [name,entry]of Object.entries(lock.packages)){
  if(!name)continue;
  if(!name.startsWith('node_modules/')||name.split('/').includes('..')||entry.link)throw Error('Unexpected dependency path');
  const file=path.join(root,name,'package.json');
  if(!fs.existsSync(file)&&entry.optional)continue;
  const actual=read(file),metadata=installed.packages[name];
  if(actual.version!==entry.version||!metadata||metadata.version!==entry.version||metadata.integrity!==entry.integrity||metadata.resolved!==entry.resolved)throw Error('Cached package does not match lock: '+name);
  count++;
 }
 const electron=lock.packages['node_modules/electron'].version;
 for(const file of ['electron/cli.js','electron/index.js','three/build/three.module.js','playwright/index.js'])if(!fs.statSync(path.join(root,'node_modules',file)).isFile())throw Error('Required dependency entry is missing: '+file);
 if(fs.readFileSync(path.join(root,'node_modules/electron/dist/version'),'utf8').trim()!==electron)throw Error('Cached Electron binary version differs');
 const executable=process.platform==='win32'?'electron.exe':process.platform==='darwin'?'Electron.app/Contents/MacOS/Electron':'electron';
 if(!fs.statSync(path.join(root,'node_modules/electron/dist',executable)).isFile())throw Error('Electron executable is missing');
 if(fs.readFileSync(path.join(root,'node_modules/electron/path.txt'),'utf8').trim()!==executable)throw Error('Electron executable path is missing or different');
 return {packages:count,electron,scope:'Installed package versions and lock metadata plus Electron executable presence/version; not a hash of every installed file'};
}
if(require.main===module){try{console.log(JSON.stringify(verifyCache(path.resolve(process.argv[2]||'workspace-desktop'))));}catch(e){console.error('Desktop cache unavailable: '+e.message);process.exitCode=1;}}
module.exports={verifyCache};
