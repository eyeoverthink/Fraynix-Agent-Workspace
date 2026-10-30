const el=id=>document.getElementById('measurement-'+id);
export class MeasurementPanel {
  constructor({call,scene,requireSaved,notice,preview}) {
    Object.assign(this,{call,scene,requireSaved,notice,preview});this.csv='';this.filename='';this.datasets=[];this.comparisons=[];
    const action=(id,fn)=>el(id).onclick=()=>this.action(fn);
    action('example',async()=>{
      this.requireSaved();const s=this.scene();if(!s)throw Error('Open a saved scene first');
      const acceleration=s.parameters.gravityMps2*Math.sin(s.parameters.angleDeg*Math.PI/180);
      const csv='time_ms,distance_mm\n'+[0,.25,.5,.75].map(t=>`${t*1000},${.5*acceleration*t*t*1000}`).join('\n')+'\n';
      await this.load(csv,'synthetic-ideal-incline.csv');el('kind').value='synthetic';el('time-unit').value='ms';el('distance-unit').value='mm';
      el('title').value='Synthetic ideal incline';el('note').value=`Synthetic analytical values, not instrument measurements: 0.5*g*sin(angle)*t^2, ${s.parameters.angleDeg} degrees, g ${s.parameters.gravityMps2}, frictionless. Generated from saved scene v${s.version}.`;
    });
    el('file').onchange=()=>this.action(async()=>{
      const file=el('file').files[0];if(!file)return;
      if(file.size>262144)throw Error('CSV must be at most 256 KiB');
      const text=new TextDecoder('utf-8',{fatal:true,ignoreBOM:true}).decode(await file.arrayBuffer());
      await this.load(text,file.name);
    });
    action('import',async()=>{
      this.requireSaved();if(!this.csv)throw Error('Choose a CSV file or load the synthetic example');
      const input={title:el('title').value,filename:this.filename,csv:this.csv,sourceKind:el('kind').value,sourceNote:el('note').value,missingPolicy:el('missing').value,
        mapping:{timeColumn:el('time-column').value,distanceColumn:el('distance-column').value,timeUnit:el('time-unit').value,distanceUnit:el('distance-unit').value,
          timeOffsetS:this.number('offset'),distanceOrigin:this.number('origin'),direction:Number(el('direction').value)}};
      if(el('uncertainty').value!=='')input.uncertaintyM=this.number('uncertainty');
      if(el('revise').checked){const d=this.datasets.find(d=>d.id===el('dataset').value);if(!d)throw Error('Select an existing dataset before adding a version');input.id=d.id;input.expectedVersion=d.version;}
      const data=await this.call('import',input);await this.refresh(data.datasetId);el('status').textContent=`Imported ${data.rows.length} rows as v${data.version} · skipped ${data.skippedRows.length} · ${data.sourceKind}`;
      el('import-form').hidden=true;this.notice('Dataset and original CSV retained with source SHA-256');
    });
    action('compare',async()=>{
      this.requireSaved();const s=this.scene(),d=this.datasets.find(d=>d.id===el('dataset').value),runId=document.getElementById('scene-history').value;
      if(!s||!d||!runId)throw Error('Select a saved dataset and a completed experiment');
      const result=await this.call('compare',{datasetId:d.id,datasetVersion:d.version,runId,expectedSceneVersion:s.version,toleranceM:this.number('tolerance')});
      await this.refresh(d.id,result.id);this.show(result);
    });
    action('report',async()=>{this.requireSaved();const id=el('history').value;if(!id)throw Error('Select a comparison');this.preview();await this.call('report',{id});this.notice('Local report opened with original CSV and comparison JSON in Source');});
    action('repeat',async()=>{this.requireSaved();const id=el('history').value;if(!id)throw Error('Select a saved comparison');const result=await this.call('repeat',{id});await this.refresh(result.datasetId,result.id);this.show(result);this.notice('Native physics rerun and new comparison recorded; previous evidence retained');});
    el('dataset').onchange=()=>this.provenance();
    el('history').onchange=()=>this.action(async()=>{if(el('history').value)this.show(await this.call('comparison',{id:el('history').value}));});
    document.getElementById('measurement-panel').addEventListener('toggle',()=>{if(document.getElementById('measurement-panel').open)this.action(()=>this.refresh());});
  }
  number(id){if(el(id).value.trim()==='')throw Error('Supply '+id);const value=Number(el(id).value);if(!Number.isFinite(value))throw Error('Use finite numeric values');return value;}
  async action(fn){
    if(this.busy)return;this.busy=true;const panel=document.getElementById('measurement-panel');panel.setAttribute('aria-busy','true');
    for(const field of panel.querySelectorAll('button,input,select,textarea'))field.disabled=true;
    try{await fn();}catch(e){el('status').textContent=e.message;this.notice(e.message);}finally{
      this.busy=false;panel.setAttribute('aria-busy','false');const running=!document.getElementById('stop').disabled;
      for(const field of panel.querySelectorAll('button,input,select,textarea'))field.disabled=running;
      el('report').disabled=running||!this.comparisons.length;el('repeat').disabled=running||!this.comparisons.length;
    }
  }
  async load(csv,filename){
    const info=await this.call('inspect',{csv});this.csv=csv;this.filename=filename;
    for(const id of ['time-column','distance-column']){el(id).replaceChildren(...info.columns.map(name=>{const o=document.createElement('option');o.value=name;o.textContent=name;return o;}));}
    if(info.columns.length>1)el('distance-column').selectedIndex=1;
    el('title').value=filename.replace(/\.csv$/i,'');el('kind').value='unverified';el('note').value='';el('revise').checked=false;el('time-unit').value='';el('distance-unit').value='';
    el('offset').value='0';el('origin').value='0';el('direction').value='1';el('uncertainty').value='';el('missing').value='reject';
    el('file-label').textContent=`${filename} · ${info.dataRows} source rows`;el('import-form').hidden=false;el('status').textContent='Choose column meanings, units and source notes, then import. No units are inferred.';
  }
  async refresh(dataset='',comparison=''){
    const result=await this.call('list',{sceneId:this.scene()?.id||''});this.datasets=result.datasets;this.comparisons=result.comparisons;
    this.options('dataset',this.datasets,d=>`${d.title} · v${d.version} · ${d.sourceKind}`,dataset||el('dataset').value,'No imported data');
    this.options('history',this.comparisons.toReversed(),c=>`${c.title} · ${c.passed?'within':'outside'} tolerance · data v${c.datasetVersion} / scene v${c.sceneVersion}`,comparison||el('history').value,'No comparisons');
    el('report').disabled=this.busy||!this.comparisons.length;el('repeat').disabled=this.busy||!this.comparisons.length;this.provenance();
    if(el('history').value)this.show(await this.call('comparison',{id:el('history').value}));else{el('chart').hidden=true;this.last=null;}
  }
  options(id,items,label,selected,empty){el(id).replaceChildren();for(const item of items){const o=document.createElement('option');o.value=item.id;o.textContent=label(item);el(id).append(o);}if(!items.length){const o=document.createElement('option');o.value='';o.textContent=empty;el(id).append(o);}else if(items.some(i=>i.id===selected))el(id).value=selected;}
  provenance(){const d=this.datasets.find(d=>d.id===el('dataset').value);el('provenance').textContent=d?`${d.rowCount} rows · ${d.sourceKind} (user supplied) · original SHA-256 ${d.sourceSha256}`:'No dataset selected';}
  show(result){
    this.last=result;const historical=result.sceneVersion!==this.scene()?.version||this.datasets.find(d=>d.id===result.datasetId)?.version!==result.datasetVersion;
    el('status').dataset.passed=String(result.passed);el('status').textContent=`${historical?'Historical receipt · ':''}${result.title} · dataset v${result.datasetVersion} / scene v${result.sceneVersion} · ${result.passed?'Within':'Outside'} declared tolerance · RMSE ${result.rmseM.toFixed(6)} m · max residual ${result.maxAbsErrorM.toFixed(6)} m · ${result.count} rows · ${result.sourceKind}`;
    const canvas=el('chart');canvas.hidden=false;canvas.width=Math.max(400,canvas.clientWidth*devicePixelRatio);canvas.height=150*devicePixelRatio;
    const ctx=canvas.getContext('2d'),w=canvas.width,h=canvas.height,points=result.points,maxT=Math.max(.001,...points.map(p=>p.timeS)),lo=Math.min(0,...points.flatMap(p=>[p.observedM,p.simulatedM])),hi=Math.max(.001,...points.flatMap(p=>[p.observedM,p.simulatedM]));
    ctx.clearRect(0,0,w,h);ctx.font=`${11*devicePixelRatio}px system-ui`;ctx.fillStyle='#adc9c4';ctx.fillText('Displacement m · orange observed / green simulated · elapsed time s',12,18*devicePixelRatio);
    for(const [key,color]of [['observedM','#f3ba72'],['simulatedM','#85ddc4']]){ctx.strokeStyle=color;ctx.lineWidth=2*devicePixelRatio;ctx.beginPath();points.forEach((p,i)=>{const x=20+(w-40)*p.timeS/maxT,y=h-22-(h-55)*(p[key]-lo)/(hi-lo);i?ctx.lineTo(x,y):ctx.moveTo(x,y);});ctx.stroke();}
    ctx.fillStyle='#adc9c4';ctx.fillText(`0 → ${maxT.toFixed(3)} s · displacement ${lo.toFixed(3)} → ${hi.toFixed(3)} m`,12,h-3*devicePixelRatio);
  }
}
