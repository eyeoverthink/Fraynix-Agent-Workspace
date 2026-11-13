const $=id=>document.getElementById(id);
export class Contact133Panel {
 constructor({labels,call,notice}){
  Object.assign(this,{labels,call,notice});this.root=document.createElement('details');this.root.id='contact133-panel';this.root.className='spatial-detail observations-panel';
  this.root.innerHTML=`<summary>Visual support · compare contact with original pixels</summary>
   <p>Choose a frozen label set and completed run above. This experimental Java estimator works with either the existing tracker or the explicit appearance-association candidate. It can withhold a contact point. Existing metric anchors and forecasts stay unchanged.</p>
   <button id="contact133-audit">Evaluate selected run &amp; labels</button><div class="observation-toolbar"><label>Retained contact evidence<select id="contact133-saved"></select></label><button id="contact133-read">Recheck source &amp; outcome</button><button id="contact133-export">Export verified comparison</button></div>
   <p id="contact133-status" role="status">No contact comparison selected.</p><div id="contact133-results"></div>
   <label>Inspect labelled source sample<select id="contact133-frame"></select></label><canvas id="contact133-canvas" width="360" height="288" aria-label="Audit image: candidate box, visual support and independent reference"></canvas><p id="contact133-caption">Audit overlay only. Source-only annotation view above remains separate.</p>`;
  labels.root.after(this.root);this.root.ontoggle=()=>{if(this.root.open)this.action(()=>this.refresh());};
  $('contact133-audit').onclick=()=>this.action(async()=>{const a=await call('audit',{labelId:$('labels132-saved').value,runId:$('labels132-run').value});await this.show(a);await this.refresh();});
  $('contact133-read').onclick=()=>this.action(async()=>this.show(await call('read',{id:$('contact133-saved').value})));
  $('contact133-export').onclick=()=>this.action(async()=>{const r=await call('export',{id:$('contact133-saved').value});notice('Verified contact export saved: '+r.path);});
  $('contact133-frame').onchange=()=>this.action(()=>this.paint());
 }
 async action(fn){if(this.busy)return;this.busy=true;this.root.setAttribute('aria-busy','true');$('contact133-status').textContent='Checking original source and outcome…';try{await fn();$('contact133-status').textContent='Original pixels, label and run bindings verified; contact outcome recomputed.';}catch(e){$('contact133-status').textContent=e.message;this.notice(e.message);}finally{this.busy=false;this.root.removeAttribute('aria-busy');}}
 async refresh(){const r=await this.call('state');const selected=this.audit?.id||$('contact133-saved').value;this.labels.options('contact133-saved',r.audits.map(a=>({id:a.id,text:a.id.slice(0,12)+' · run '+a.input.runId.slice(0,8)})),selected);}
 async show(a){
  this.audit=a;const host=$('contact133-results');host.replaceChildren();const s=a.summary;const summary=document.createElement('p');summary.id='contact133-summary';summary.textContent=`${a.trackerPolicy} · association available ${s.associated}/${s.labels} labels. Visual contacts ${s.contactAvailable}/${s.observableContacts}; missing ${s.contactMissing}; within point allowance ${s.withinLabelRadius}/${s.observableContacts}. Unobservable references ${s.unobservableContacts}; unsupported contact candidates ${s.unsupportedContacts}.`;host.append(summary);
  const errors=document.createElement('p');const f=v=>v==null?'unavailable':v.toFixed(3);errors.textContent=`Accepted-only mean error: original box bottom ${f(s.boxMeanPx)} px (${s.boxAvailable} samples); visual support ${f(s.contactMeanPx)} px (${s.contactAvailable} samples). These are different subsets when measurements are missing.`;host.append(errors);
  const scope=document.createElement('p');scope.textContent='Visual support is an image hypothesis, not proof of physical ground contact. Appearance is not personal identity. This audit recomputes support from original pixels and retained boxes; it does not rerun or relabel the earlier tracker. Hidden or missing evidence remains explicit.';host.append(scope);
  const details=document.createElement('details');const title=document.createElement('summary');title.textContent='Per-frame errors, refusal reasons and evidence identities';const pre=document.createElement('pre');pre.textContent=JSON.stringify(a,null,2);details.append(title,pre);host.append(details);
  this.labels.options('contact133-frame',a.scores.map(r=>({id:String(r.ordinal),text:'Sample '+r.ordinal+' · '+r.visibility+' · '+r.contact.status})));await this.paint();
 }
 async paint(){if(!this.audit)return;const r=this.audit.scores.find(r=>String(r.ordinal)===$('contact133-frame').value);if(!r)return;const source=await this.labels.call('source',{datasetId:this.audit.datasetId,ordinal:r.ordinal});if(source.frame.imageSha256!==r.imageSha256)throw Error('Audit image identity differs');const image=new Image();image.src='data:image/jpeg;base64,'+source.image;await image.decode();const canvas=$('contact133-canvas');canvas.width=image.width;canvas.height=image.height;const ctx=canvas.getContext('2d');ctx.drawImage(image,0,0);ctx.lineWidth=1;
  if(r.box){ctx.strokeStyle='#f47bd2';ctx.strokeRect(r.box[0]*canvas.width,r.box[1]*canvas.height,(r.box[2]-r.box[0])*canvas.width,(r.box[3]-r.box[1])*canvas.height);}
  const mark=(p,color)=>{if(!p)return;const x=p[0]*canvas.width,y=p[1]*canvas.height;ctx.strokeStyle=color;ctx.beginPath();ctx.moveTo(x-5,y);ctx.lineTo(x+5,y);ctx.moveTo(x,y-5);ctx.lineTo(x,y+5);ctx.stroke();};mark(r.pointImage,'#ffe07d');mark(r.contact.pointImage,'#51ead4');
  $('contact133-caption').textContent=`Original frame ${r.sourceFrame} · pink: retained box · gold: independent annotation · teal: visual-support candidate. ${r.contact.reason}. ${r.contactErrorPx===undefined?'No observable contact comparison.':'Error '+r.contactErrorPx.toFixed(3)+' source pixels.'}`;
 }
}
