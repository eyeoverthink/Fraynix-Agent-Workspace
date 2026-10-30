'use strict';
const {WebContentsView,session}=require('electron');
const {ObservationLease,webURL}=require('./policy.cjs');

class Surface {
  constructor(kind,window,notify,options={}){
    this.kind=kind;this.notify=notify;this.lease=new ObservationLease();this.errors=[];this.allowedPrefix='';
    const profile=options.partition||(kind==='browser'?'persist:workspace-browser':'workspace-canvas');
    const partition=session.fromPartition(profile);
    this.networkPolicy=options.networkPolicy;
    partition.setPermissionRequestHandler((_wc,_permission,callback)=>callback(false));
    partition.setPermissionCheckHandler(()=>false);
    if(kind==='canvas'||this.networkPolicy?.enabled)partition.webRequest.onBeforeRequest((details,callback)=>{
      const blocked=this.networkPolicy&&!this.networkPolicy.allow(details.url);if(blocked)this.networkPolicy.blocked++;
      callback({cancel:!!blocked||(kind==='canvas'&&(!this.allowedPrefix||!details.url.startsWith(this.allowedPrefix)))});
    });
    this.view=new WebContentsView({webPreferences:{session:partition,nodeIntegration:false,contextIsolation:true,sandbox:true,webSecurity:true,backgroundThrottling:false}});
    window.contentView.addChildView(this.view);
    this.view.setBounds({x:0,y:0,width:1,height:1});
    const wc=this.view.webContents;
    wc.setWindowOpenHandler(({url})=>{if(kind==='browser')this.navigate(url).catch(e=>notify('error',e.message));return {action:'deny'};});
    wc.on('will-navigate',(event,url)=>{try {webURL(url);if(kind==='canvas'&&!url.startsWith(this.allowedPrefix))event.preventDefault();}catch{event.preventDefault();}});
    wc.on('did-start-navigation',()=>this.lease.invalidate());
    wc.on('did-navigate',()=>notify('surface',{kind,url:wc.getURL(),title:wc.getTitle()}));
    wc.on('page-title-updated',()=>notify('surface',{kind,url:wc.getURL(),title:wc.getTitle()}));
    wc.on('console-message',(...args)=>{const data=typeof args[1]==='object'?args[1]:{level:args[1],message:args[2]};if(data.level==='error'||data.level===3||data.level===2)this.errors.push(String(data.message).slice(0,600));});
    wc.on('render-process-gone',(_event,details)=>notify('error',`${kind} renderer stopped: ${details.reason}`));
  }
  async bounded(promise,label,milliseconds=8000){
    let timer;this.pending=true;
    try{return await Promise.race([promise,new Promise((_,reject)=>{timer=setTimeout(()=>{this.view.webContents.forcefullyCrashRenderer();reject(Error(`${this.kind} ${label} timed out; its renderer was stopped. Revise the artifact or navigate again.`));},milliseconds);})]);}
    finally{clearTimeout(timer);this.pending=false;}
  }
  cancelPending(){if(this.pending)this.view.webContents.forcefullyCrashRenderer();else this.view.webContents.stop();this.lease.invalidate();}
  async navigate(url){url=webURL(url);this.networkPolicy?.require(url);if(this.kind==='canvas')this.allowedPrefix=url.slice(0,url.lastIndexOf('/')+1);this.errors=[];this.lease.invalidate();await this.bounded(this.view.webContents.loadURL(url),'navigation',15000);return this.observe();}
  async isolated(code){return this.bounded(this.view.webContents.executeJavaScriptInIsolatedWorld(1001,[{code}],true),'script operation');}
  async observe(){
    const observation=this.lease.create();
    const data=await this.isolated(`(()=>{
      const shown=e=>{const r=e.getBoundingClientRect();return r.width>0&&r.height>0&&getComputedStyle(e).visibility!=='hidden'};
      const elements=Array.from(document.querySelectorAll('a[href],button,input,textarea,select,[role="button"],[contenteditable="true"]')).filter(shown).slice(0,60);
      globalThis.fraynixElements=elements;
      return {title:document.title,url:location.href,text:document.body?.innerText.slice(0,9000)||'',controls:elements.map((e,i)=>({ref:String(i),tag:e.tagName.toLowerCase(),role:e.getAttribute('role')||e.type||e.tagName.toLowerCase(),label:(e.getAttribute('aria-label')||e.labels?.[0]?.innerText||e.innerText||e.placeholder||e.name||e.id||'').slice(0,180),value:e.type==='password'?'[redacted]':String(e.value||'').slice(0,200),disabled:!!e.disabled}))};})()`);
    this.latest={...data,observation,kind:this.kind,errors:this.errors.slice(-5)};return this.latest;
  }
  async act(args,type){
    this.lease.require(args.observation);
    if(!/^\d{1,2}$/.test(String(args.ref)))throw Error('Invalid control reference');
    if(this.latest?.controls?.some(c=>c.ref===String(args.ref)&&c.role==='password'))throw Error('Password fields require manual interaction');
    const ref=Number(args.ref);
    const rect=await this.isolated(`(()=>{const e=globalThis.fraynixElements?.[${ref}];if(!e||!e.isConnected||e.disabled)return {error:'Control changed; observe again'};if(e.type==='password')return {error:'Password fields require manual interaction'};e.scrollIntoView({block:'center',inline:'center'});e.focus();const r=e.getBoundingClientRect();if(!r.width||!r.height||getComputedStyle(e).visibility==='hidden')return {error:'Control changed; observe again'};return {x:Math.round(r.x+r.width/2),y:Math.round(r.y+r.height/2),tag:e.tagName,type:e.type||''};})()`);
    if(rect.error)throw Error(rect.error);
    if(type==='type'){
      if(!['INPUT','TEXTAREA'].includes(rect.tag)||['file','checkbox','radio','submit'].includes(rect.type))throw Error('This control is not a text field');
      if(typeof args.text!=='string'||args.text.length>12000)throw Error('Text is too large');
      this.view.webContents.focus();
      await this.isolated(`(()=>{const e=globalThis.fraynixElements[${ref}];const prototype=e.tagName==='TEXTAREA'?HTMLTextAreaElement.prototype:HTMLInputElement.prototype;Object.getOwnPropertyDescriptor(prototype,'value').set.call(e,'');})()`);
      await this.view.webContents.insertText(args.text);
    }else{
      this.view.webContents.sendInputEvent({type:'mouseDown',x:rect.x,y:rect.y,button:'left',clickCount:1});this.view.webContents.sendInputEvent({type:'mouseUp',x:rect.x,y:rect.y,button:'left',clickCount:1});
    }
    this.lease.invalidate();await new Promise(r=>setTimeout(r,160));return this.observe();
  }
  async test(steps){
    if(!Array.isArray(steps)||steps.length<1||steps.length>24)throw Error('Tests need 1..24 steps');
    if(!steps.some(s=>['expectText','expectValue','expectCount'].some(k=>Object.hasOwn(s,k))))throw Error('Tests need an actual assertion');
    const checks=[];
    for(const [index,step]of steps.entries()){
      if(typeof step.selector!=='string'||step.selector.length>400)throw Error('Use a bounded CSS selector');
      if(Object.keys(step).some(k=>!['selector','action','value','expectText','expectValue','expectCount'].includes(k)))throw Error('Unsupported test operation');
      const selector=JSON.stringify(step.selector);
      if(step.action){
        if(!['click','fill'].includes(step.action))throw Error('Only click and fill actions are supported');
        const observed=await this.observe();
        const ref=await this.isolated(`(()=>{const e=document.querySelector(${selector});return globalThis.fraynixElements.indexOf(e)})()`);
        if(ref<0){checks.push({index,selector:step.selector,passed:false,error:'Interactive control not found'});continue;}
        await this.act({observation:observed.observation,ref:String(ref),text:step.value||''},step.action==='fill'?'type':'click');
      }
      let actual={},passed=false;
      for(let attempt=0;attempt<12;attempt++){
        actual=await this.isolated(`(()=>{const items=document.querySelectorAll(${selector}),e=items[0];return {count:items.length,text:e?.innerText??e?.textContent??'',value:e?.value??null}})()`);
        passed=(!Object.hasOwn(step,'expectText')||actual.text.trim()===String(step.expectText))&&(!Object.hasOwn(step,'expectValue')||actual.value===String(step.expectValue))&&(!Object.hasOwn(step,'expectCount')||actual.count===step.expectCount);
        if(passed)break;await new Promise(r=>setTimeout(r,80));
      }
      checks.push({index,selector:step.selector,passed,actual,expected:step});
    }
    if(this.errors.length)checks.push({index:checks.length,passed:false,error:'Browser JavaScript errors',errors:this.errors.slice(-5)});
    return checks;
  }
  close(){if(!this.view.webContents.isDestroyed())this.view.webContents.close();}
}
module.exports={Surface};
