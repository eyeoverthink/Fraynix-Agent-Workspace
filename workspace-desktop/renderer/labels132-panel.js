const $ = id => document.getElementById(id);
const choices = values => values.map(v => `<option value="${v}">${v.replaceAll('-', ' ')}</option>`).join('');

export class Labels132Panel {
 constructor({call, observation, notice}) {
  Object.assign(this, {call, observation, notice}); this.rows = new Map();
  this.root = document.createElement('details'); this.root.id = 'labels132-panel'; this.root.className = 'spatial-detail observations-panel';
  this.root.innerHTML = `
   <summary>Independent labels · visibility & ground contact</summary>
   <p>Mark original images before comparing results. This view has no tracker overlay. Labels stay separate from tracking, forecasts and the physical world. Unclear contact stays unknown.</p>
   <div class="observation-toolbar"><label>Source footage<select id="labels132-dataset"></select></label><button id="labels132-load">Open source-only view</button><label>Source object<select id="labels132-object"></select></label></div>
   <div id="labels132-editor" hidden>
    <p id="labels132-source"></p><canvas id="labels132-canvas" width="360" height="288" aria-label="Original image: click only a visible ground-contact point"></canvas>
    <label>Source sample<input id="labels132-frame" type="range" min="0" max="95" value="1"></label><p id="labels132-frame-text"></p>
    <div class="observation-toolbar"><label>Visibility<select id="labels132-visibility">${choices(['uncertain','visible','partial','occluded','out-of-view'])}</select></label><label>Ground contact<select id="labels132-contact">${choices(['uncertain','observed','occluded','out-of-view','not-grounded'])}</select></label><label>Point allowance (source pixels)<input id="labels132-radius" type="number" min="1" max="32" value="3"></label></div>
    <p id="labels132-point">No observed contact point. Do not guess a hidden foot.</p><label>Reason / visual evidence<input id="labels132-note" maxlength="500" placeholder="What is visible or uncertain in this frame?"></label><button id="labels132-keep">Keep this label in draft</button><button id="labels132-remove">Remove draft label</button>
    <p id="labels132-draft">No draft labels. Drafts remain in this window until frozen.</p>
    <div class="observation-toolbar"><label>Annotator<input id="labels132-annotator" maxlength="120" placeholder="Your name or AI reviewer"></label><label>Method<select id="labels132-method">${choices(['human-source-only','ai-source-only','external-annotations','synthetic-control'])}</select></label><label>Prior exposure<select id="labels132-exposure">${choices(['tracker-results-seen','retrospective-source-only','before-tracking'])}</select></label></div>
    <label>Label provenance<input id="labels132-provenance" maxlength="1500" placeholder="Who marked these points, and what evidence had they already seen?"></label><button id="labels132-freeze">Freeze source-bound labels</button>
   </div>
   <div class="observation-toolbar"><label>Import labels for the same source<input id="labels132-import" type="file" accept=".json"></label><label>Frozen label set<select id="labels132-saved"></select></label><button id="labels132-read">Recheck original labels</button><button id="labels132-export">Export verified labels</button></div>
   <p id="labels132-frozen">A frozen set cannot be edited. Corrections create a separate set and retain the original.</p>
   <div class="observation-toolbar"><label>Completed tracking run<select id="labels132-run"></select></label><label>Optional camera calibration<select id="labels132-geometry"></select></label><button id="labels132-audit">Compare with frozen labels</button></div>
   <div class="observation-toolbar"><label>Saved comparison<select id="labels132-audits"></select></label><button id="labels132-audit-read">Recheck original comparison</button></div>
   <div id="labels132-results" aria-live="polite"></div><p id="labels132-status" role="status">No labels selected.</p>`;
  observation.root.after(this.root);
  this.root.ontoggle = () => { if(this.root.open) this.action(() => this.refresh()); };
  const on = (id, fn) => $(id).onclick = () => this.action(fn);
  on('labels132-load', async () => {this.rows.clear(); this.datasetId=$('labels132-dataset').value; await this.frame(1); this.draftStatus();});
  $('labels132-frame').onchange = () => this.action(() => this.frame(Number($('labels132-frame').value)));
  $('labels132-object').onchange = () => {this.rows.clear(); this.draftStatus(); this.point=null; this.pointStatus();};
  $('labels132-contact').onchange = () => {if($('labels132-contact').value!=='observed') this.point=null; this.paint();};
  $('labels132-canvas').onclick = e => {if(!this.source || this.busy)return; const b=e.currentTarget.getBoundingClientRect(); this.point=[Math.max(0,Math.min(1,(e.clientX-b.left)/b.width)),Math.max(0,Math.min(1,(e.clientY-b.top)/b.height))]; $('labels132-contact').value='observed'; this.paint();};
  on('labels132-keep', () => this.keep());
  on('labels132-remove', () => {this.rows.delete(this.source.ordinal);this.draftStatus();});
  on('labels132-freeze', async () => {const record=await call('save',this.input());this.labelId=record.id;await this.showLabel(record);this.rows.clear();this.draftStatus();await this.refresh();});
  $('labels132-import').onchange = () => this.action(async () => {const file=$('labels132-import').files[0]; if(!file)return; if(file.size>1_000_000)throw Error('Choose a label file under 1 MB'); const parsed=JSON.parse(await file.text());const record=await call(parsed.input?'import':'save',parsed);this.labelId=record.id;await this.showLabel(record);await this.refresh();});
  $('labels132-saved').onchange = () => {this.labelId=$('labels132-saved').value;this.record=null;this.refreshRuns();};
  on('labels132-read', async () => this.showLabel(await call('read',{id:$('labels132-saved').value})));
  on('labels132-export', async () => {const result=await call('export',{id:$('labels132-saved').value});this.notice('Verified label export saved: '+result.path);});
  on('labels132-audit', async () => {const labelId=$('labels132-saved').value;await this.showLabel(await call('read',{id:labelId}));const args={labelId,runId:$('labels132-run').value};if($('labels132-geometry').value)args.geometryId=$('labels132-geometry').value;const result=await call('audit',args);this.auditId=result.id;this.showAudit(result);await this.refresh();});
  on('labels132-audit-read', async () => {const result=await call('audit-read',{id:$('labels132-audits').value});this.auditId=result.id;await this.showLabel(await call('read',{id:result.input.labelId}));this.showAudit(result);});
 }
 async action(fn) { if(this.busy)return;this.busy=true;this.root.setAttribute('aria-busy','true');try{await fn();$('labels132-status').textContent='Source and evidence checks completed.';}catch(e){$('labels132-status').textContent=e.message;this.notice(e.message);}finally{this.busy=false;this.root.removeAttribute('aria-busy');} }
 options(id, values, selected, empty) {const el=$(id);el.replaceChildren();if(empty!==undefined)el.add(new Option(empty,''));for(const v of values)el.add(new Option(v.text,v.id));if(values.some(v=>v.id===selected))el.value=selected;}
 async refresh() {
  this.state=await this.observation.call('state',{});const saved=await this.call('state');this.saved=saved;
  this.options('labels132-dataset',this.state.datasets.map(d=>({id:d.id,text:d.title})),this.datasetId||$('labels132-dataset').value);
  this.options('labels132-saved',saved.labels.map(l=>({id:l.id,text:l.objectKey+' · '+l.rows+' labels · '+l.id.slice(0,10)})),this.labelId||$('labels132-saved').value);
  this.options('labels132-audits',saved.audits.map(a=>({id:a.id,text:a.id.slice(0,12)+' · run '+a.runId.slice(0,8)})),this.auditId||$('labels132-audits').value);
  this.refreshRuns();
  const geometries=await window.workspace.request({operation:'metrics',command:'state',args:{}});
  this.options('labels132-geometry',geometries.geometries.map(g=>({id:g.id,text:g.definition.cameraKey})), $('labels132-geometry').value, 'Image pixels only');
 }
 refreshRuns() {if(!this.state)return;const label=this.saved.labels.find(l=>l.id===$('labels132-saved').value);this.options('labels132-run',this.state.runs.filter(r=>r.status==='completed'&&r.datasetId===label?.datasetId).map(r=>({id:r.id,text:(r.trackerPolicy||'legacy-flow')+' · '+r.id.slice(0,8)})),$('labels132-run').value);}
 async frame(ordinal) {
  const source=await this.call('source',{datasetId:this.datasetId,ordinal});this.source=source;this.image=new Image();this.image.src='data:image/jpeg;base64,'+source.image;await this.image.decode();
  $('labels132-editor').hidden=false;const old=$('labels132-object').value;this.options('labels132-object',source.objects.map(o=>({id:o.key,text:o.label+' ('+o.key+')'})),old);
  $('labels132-source').textContent=source.title+' · '+source.width+' × '+source.height+' original pixels. '+source.license;
  $('labels132-frame').max=source.frameCount-1;$('labels132-frame').value=ordinal;
  $('labels132-frame-text').textContent='Sample '+ordinal+' · original frame '+source.frame.sourceFrame+' · SHA '+source.frame.imageSha256.slice(0,16)+(ordinal===0?' · seed, excluded from scoring':'');
  const row=this.rows.get(ordinal);this.point=row?.pointImage||null;$('labels132-visibility').value=row?.visibility||'uncertain';$('labels132-contact').value=row?.contact||'uncertain';$('labels132-radius').value=row?.radiusPx||3;$('labels132-note').value=row?.note||'';this.paint();
 }
 paint() {if(!this.image)return;const canvas=$('labels132-canvas');canvas.width=this.image.width;canvas.height=this.image.height;const ctx=canvas.getContext('2d');ctx.drawImage(this.image,0,0);if(this.point){const x=this.point[0]*canvas.width,y=this.point[1]*canvas.height;ctx.strokeStyle='#ffe07d';ctx.lineWidth=1;ctx.beginPath();ctx.moveTo(x-6,y);ctx.lineTo(x+6,y);ctx.moveTo(x,y-6);ctx.lineTo(x,y+6);ctx.stroke();}this.pointStatus();}
 pointStatus() {$('labels132-point').textContent=this.point?'Your contact mark: ['+this.point.map((v,i)=>(v*[this.source.width,this.source.height][i]).toFixed(2)).join(', ')+'] source pixels. No tracker output displayed.':'No observed contact point. Do not guess a hidden foot.';}
 keep() {
  if(!this.source||this.source.ordinal===0)throw Error('Choose a later frame; seed excluded');
  const row={ordinal:this.source.ordinal,sourceFrame:this.source.frame.sourceFrame,imageSha256:this.source.frame.imageSha256,visibility:$('labels132-visibility').value,contact:$('labels132-contact').value,note:$('labels132-note').value.trim()};
  if(!row.note)throw Error('Describe what you can or cannot see');
  if(row.contact==='observed'){if(!this.point)throw Error('Click the visible contact point first');if(!['visible','partial'].includes(row.visibility))throw Error('Observed contact requires a visible or partly visible target');row.pointImage=[...this.point];row.radiusPx=Number($('labels132-radius').value);if(!Number.isFinite(row.radiusPx)||row.radiusPx<1||row.radiusPx>32)throw Error('Point allowance must be 1–32 source pixels');}
  this.rows.set(row.ordinal,row);this.draftStatus();
 }
 draftStatus() {$('labels132-draft').textContent=this.rows.size+' draft labels · samples '+[...this.rows.keys()].sort((a,b)=>a-b).join(', ')+'. Freeze to retain after restart.';}
 input() {if(!this.source)throw Error('Open source footage first');return {schema:'observation-labels-132-v1',datasetId:this.datasetId,datasetSha256:this.source.datasetSha256,objectKey:$('labels132-object').value,annotator:$('labels132-annotator').value.trim(),method:$('labels132-method').value,exposure:$('labels132-exposure').value,provenance:$('labels132-provenance').value.trim(),rows:[...this.rows.values()].sort((a,b)=>a.ordinal-b.ordinal)};}
 async showLabel(record) {this.record=record;this.labelId=record.id;if([...$('labels132-saved').options].some(o=>o.value===record.id)){$('labels132-saved').value=record.id;this.refreshRuns();}const l=record.input;$('labels132-frozen').textContent='Frozen '+record.id.slice(0,16)+' · '+l.rows.length+' labels · '+l.annotator+' · '+l.method+' · '+l.exposure+'. '+l.provenance;}
 showAudit(audit) {
  this.audit= audit;$('labels132-run').value=audit.input.runId;$('labels132-geometry').value=audit.input.geometryId||''; const host=$('labels132-results');host.replaceChildren();
  const heading=document.createElement('h3');heading.textContent='Visibility and contact comparison';host.append(heading);
  const table=document.createElement('table');const head=document.createElement('tr');for(const name of ['Source visibility','Labelled frames','Accepted','Missing']){const th=document.createElement('th');th.textContent=name;head.append(th);}table.append(head);
  for(const [name,v]of Object.entries(audit.visibility)){const tr=document.createElement('tr');for(const value of [name,v.labels,v.accepted,v.missing]){const td=document.createElement('td');td.textContent=String(value);tr.append(td);}table.append(tr);}host.append(table);
  const s=audit.summary;const p=document.createElement('p');p.id='labels132-summary';p.textContent=`Observable contacts: ${s.contactAvailable} / ${s.observableContacts} measured; ${s.contactMissing} missing. Within declared point allowance: ${s.withinLabelRadius} / ${s.observableContacts}. Unsupported measurements while hidden: ${s.unsupportedHiddenMeasurements}.`;host.append(p);
  const errors=document.createElement('p');errors.id='labels132-errors';const f=v=>v===null?'unavailable':v.toFixed(3);errors.textContent=`Mean contact error · raw box: ${f(s.rawAnchorMeanErrorPx)} px · filtered anchor: ${f(s.filteredAnchorMeanErrorPx)} px · shared-calibration projection: ${f(s.projectedContactMeanErrorM)} m (${s.projectedContactAvailable} samples).`;host.append(errors);
  const scope=document.createElement('p');scope.textContent=audit.scope;host.append(scope);const detail=document.createElement('details');const summary=document.createElement('summary');summary.textContent='Original labels, per-frame errors and evidence identities';const pre=document.createElement('pre');pre.textContent=JSON.stringify(audit,null,2);detail.append(summary,pre);host.append(detail);
 }
}
