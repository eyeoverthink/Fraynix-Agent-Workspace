'use strict';
const fs=require('node:fs'),path=require('node:path');
const files=['index.html','style.css','measurement.css','app.js','scene-lab.js','measurement-panel.js','calibration-panel.js','calibration.css','observer-panel.js','observer.css','body-panel.js','scene-files.js','scene-video.js'];
function verifyRendererSources(folder){
 for(const name of files){const bytes=fs.readFileSync(path.join(folder,name));if(!bytes.length||bytes.includes(0))throw Error('Application source '+name+' is empty or contains NUL bytes; startup stopped before loading corrupted code');try{new TextDecoder('utf-8',{fatal:true}).decode(bytes);}catch{throw Error('Application source '+name+' is not valid UTF-8; startup stopped');}}
 return {checked:files.length};
}
module.exports={verifyRendererSources};
