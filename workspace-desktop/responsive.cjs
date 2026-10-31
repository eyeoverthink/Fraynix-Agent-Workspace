'use strict';
const {BrowserWindow}=require('electron');
const sleep=ms=>new Promise(r=>setTimeout(r,ms));
/** A hidden, unprivileged page in the canvas's already restricted session. No CSS is rewritten. */
async function measureResponsive(canvas,url,rowCount){
 const probe=new BrowserWindow({width:780,height:900,useContentSize:true,show:false,webPreferences:{session:canvas.view.webContents.session,nodeIntegration:false,contextIsolation:true,sandbox:true,webSecurity:true,backgroundThrottling:false}}),wc=probe.webContents,views=[];
 wc.setWindowOpenHandler(()=>({action:'deny'}));wc.on('will-navigate',(e,next)=>{if(next!==url)e.preventDefault();});
 const read=code=>wc.executeJavaScriptInIsolatedWorld(1001,[{code}],true);
 try{
  await wc.loadURL(url);let ready=false;for(let i=0;i<70;i++){ready=await read(`document.querySelector('#row-count')?.textContent.trim()===${JSON.stringify(String(rowCount))}`);if(ready)break;await sleep(80);}if(!ready)throw Error('Responsive probe did not load the exact source rows');
  for(const width of [780,390]){
   probe.setContentSize(width,900);await read(`new Promise(r=>requestAnimationFrame(()=>requestAnimationFrame(r)))`);
   views.push(await read(`(()=>{scrollTo(0,0);const rect=e=>{if(!e)return {text:'',visible:false,left:0,right:0,top:0,bottom:0,width:0,height:0};const r=e.getBoundingClientRect(),s=getComputedStyle(e);return {text:e.textContent.trim(),visible:r.width>0&&r.height>0&&s.display!=='none'&&s.visibility!=='hidden'&&Number(s.opacity)>0,left:r.left,right:r.right,top:r.top,bottom:r.bottom,width:r.width,height:r.height}};const table=document.querySelector('#data-table'),headers=[...document.querySelectorAll('#data-table thead th')].slice(0,8),row=[...document.querySelectorAll('#data-table tbody tr:first-child td')].slice(0,8);return {width:innerWidth,height:innerHeight,scrollWidth:document.documentElement.scrollWidth,table:rect(table),headers:headers.map(rect),row:row.map(rect),controls:['#filter','#reset'].map(s=>rect(document.querySelector(s))),fonts:[...headers,...row].map(e=>parseFloat(getComputedStyle(e).fontSize))}})()`));
  }
  return views;
 }finally{probe.destroy();}
}
module.exports={measureResponsive};
