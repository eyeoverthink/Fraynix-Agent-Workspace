export class Inputs100Panel {
  constructor(request){
    this.request=request;this.busy=false;this.tasks=[];this.root=document.createElement('section');this.root.id='inputs100';
    this.root.setAttribute('aria-label','Selected task inputs');
    this.root.innerHTML='<div class="inputs100-heading"><strong>Task inputs</strong><button id="inputs100-toggle" aria-expanded="false">Change</button></div><p id="inputs100-data">Data: none selected</p><p id="inputs100-source">Creation: no source selected</p><p id="inputs100-status" role="status"></p><div id="inputs100-controls" hidden><label>Verified data result<select id="inputs100-results"></select></label><button id="inputs100-handoff">Create from verified data</button><label>CSV in the open artifact<select id="inputs100-files"></select></label><div class="inputs100-actions"><button id="inputs100-pin">Use this CSV</button><button id="inputs100-clear">Clear creation source</button></div><small>The selected file stays pinned when you browse. Creation checks its source and behavior independently.</small></div>';
    document.querySelector('#settings099').after(this.root);
    this.q('#inputs100-toggle').onclick=()=>{const p=this.q('#inputs100-controls');p.hidden=!p.hidden;this.q('#inputs100-toggle').setAttribute('aria-expanded',String(!p.hidden));window.dispatchEvent(new Event('resize'));};
    this.q('#inputs100-handoff').onclick=()=>this.act(async()=>{
      await this.request('inputs100',{command:'select',args:{dataGoalId100:this.q('#inputs100-results').value}});
      const composer=document.querySelector('#goal');composer.value='Create and verify an interactive explorer of the selected normalized motion data. Show all source rows, a row count, a time filter and a reset button. Read the supplied source data; keep units and source labels visible. Verify counts and the filter and reset behavior.';composer.dispatchEvent(new Event('input'));composer.focus();
    });
    this.q('#inputs100-pin').onclick=()=>this.act(()=>this.request('inputs100',{command:'select',args:{sourceArtifactId:this.state.artifact.id,sourceVersion:this.state.artifact.version,sourceFile:this.q('#inputs100-files').value}}));
    this.q('#inputs100-clear').onclick=()=>this.act(()=>this.request('inputs100',{command:'clear'}));
    for(const select of this.root.querySelectorAll('select'))select.onchange=()=>this.controls();
    this.refresh();
  }
  q(s){return this.root.querySelector(s);}
  async act(fn){try{this.busy=true;this.controls();await fn();await this.refresh();}catch(e){this.q('#inputs100-status').textContent=e.message;}finally{this.busy=false;this.controls();}}
  async refresh(){try{this.state=await this.request('inputs100',{command:'state'});this.render();}catch(e){this.q('#inputs100-status').textContent=e.message;}}
  agency(state){this.tasks=state.tasks||[];const s=this.q('#inputs100-results'),prior=s.value;s.replaceChildren(new Option('Choose a completed data task',''));for(const t of [...this.tasks].reverse()){const id=t.completion044?.javaGoal?.id;if(t.status==='completed'&&t.family044==='data'&&id)s.add(new Option(t.goal.slice(0,90),id));}if([...s.options].some(o=>o.value===prior))s.value=prior;else if(s.options.length>1)s.selectedIndex=1;this.controls();}
  render(){
    const {dataset,artifact,selection,error}=this.state;
    this.q('#inputs100-data').textContent=dataset?`Data: ${dataset.title} · ${dataset.rowCount} samples · v${dataset.version}`:'Data: none selected';
    this.q('#inputs100-source').textContent=selection?`Creation: ${selection.preview.file} · ${selection.preview.rowCount} rows · v${selection.preview.version}${selection.handoff?' · verified data result':''}`:'Creation: no source selected';
    this.q('#inputs100-source').title=selection?`${selection.preview.title}\nSHA-256: ${selection.preview.fileSha256}`:'';
    this.q('#inputs100-status').textContent=error||(selection?.handoff&&!selection.handoff.quality.qualityPassed?'Source quality flags are retained. A verified analysis does not mean the data passed its quality limits.':'');
    const s=this.q('#inputs100-files'),old=s.value;s.replaceChildren(new Option('Choose a file explicitly',''));for(const file of artifact?.files||[])s.add(new Option(file,file));if([...s.options].some(o=>o.value===old))s.value=old;
    this.controls();
  }
  controls(){const disabled=this.busy||this.running;this.q('#inputs100-handoff').disabled=disabled||!this.q('#inputs100-results').value;this.q('#inputs100-pin').disabled=disabled||!this.q('#inputs100-files').value;this.q('#inputs100-clear').disabled=disabled||!this.state?.selection&&!this.state?.error;for(const s of this.root.querySelectorAll('select'))s.disabled=disabled;}
  event(type,value){if(type==='run'){this.running=value.active;this.controls();}if(type==='artifact'||type==='dataset'||type==='inputs100')this.refresh();}
}
