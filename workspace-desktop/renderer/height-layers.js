export class HeightLayersPanel {
    constructor(environment) {
        this.environment=environment;
        this.root=environment.find('environment-height-layers');
        this.root.innerHTML=`
          <summary>Height layers</summary>
          <div class="environment-plan-fields">
            <label>Height boundaries · m <input id="layers-bounds" type="text" value="0, 0.9, 2.1, 2.6"></label>
            <label>Cell size · m <input id="layers-cell" type="number" value=".10" min=".002" max="2" step=".001"></label>
            <button id="layers-measure" disabled>Measure layers</button>
            <button id="layers-restore" disabled>Review saved audit</button>
            <button id="layers-source" hidden>Open original model</button>
          </div>
          <p id="layers-status" role="status">Open a 3D environment to measure its height layers.</p>
          <div id="layers-preview" hidden>
            <div class="environment-plan-fields">
              <label>Preview band <select id="layers-band"></select></label>
              <button id="layers-export">Save audit JSON</button>
            </div>
            <img id="layers-diagram" alt="Three height samples compared on a shared grid">
            <p id="layers-band-result"></p>
            <p>Teal held across three cuts. Orange changed with height.</p>
            <label id="layers-confirm" hidden><input id="layers-omit" type="checkbox">Rebuild the agreed cells and leave changing cells out</label>
            <button id="layers-build">Build layered GLB</button>
          </div>`;
        this.get('layers-measure').onclick=()=>this.run(()=>this.measure());
        this.get('layers-restore').onclick=()=>this.run(()=>this.restore());
        this.get('layers-source').onclick=()=>this.run(()=>this.openSource());
        this.get('layers-build').onclick=()=>this.run(()=>this.build());
        this.get('layers-export').onclick=()=>this.run(()=>this.export());
        this.get('layers-band').onchange=()=>this.diagram();
        this.get('layers-omit').onchange=()=>this.controls();
        for (const id of ['layers-bounds','layers-cell']) this.get(id).oninput=()=>this.clear('Measure the new height settings.');
    }

    get(id) {return this.root.querySelector('#'+id);}
    status(message) {this.get('layers-status').textContent=message;}
    call(command,args) {return this.environment.call('height-'+command,args);}

    run(task) {
        return this.environment.run(async()=>{
            try {await task();}
            catch (error) {this.status(error.message.replace(/^Error invoking remote method '[^']+': (?:Error: )?/,''));}
        });
    }

    clear(message) {
        this.report=null;
        if (this.url) URL.revokeObjectURL(this.url);
        this.url=null;
        this.get('layers-preview').hidden=true;
        this.status(message);
    }

    bind(catalog) {
        this.clear('Measure three cuts in each band to compare what changes with height.');
        this.get('layers-measure').disabled=false;
        this.get('layers-restore').disabled=false;
        this.get('layers-source').hidden=!catalog.layerSource;
    }

    async measure() {
        const id=this.environment.catalog?.id;
        if(!id) throw Error('Open a 3D environment first.');
        const raw=this.get('layers-bounds').value.split(',').map(value=>value.trim());
        if(raw.some(value=>!value||!Number.isFinite(Number(value)))) throw Error('Enter height boundaries separated by commas.');
        const boundsM=raw.map(Number);
        const cellM=this.get('layers-cell').valueAsNumber;
        this.clear('Measuring the retained triangles at three heights in every band…');
        this.show(await this.call('audit',{id,boundsM,cellM}));
        return this.report;
    }

    async restore() {
        const catalog=this.environment.catalog;
        const args=catalog.layerSource?{auditId:catalog.layerSource.auditId}:{sourceId:catalog.id};
        const report=await this.call('read',{...args,preview:true});
        if(!report.auditId) throw Error('This environment has no saved height audit yet.');
        this.show(report);
    }

    async openSource() {
        const link=this.environment.catalog.layerSource;
        const report=await this.call('read',{auditId:link.auditId,preview:true});
        if(report.sourceId!==link.id) throw Error('The saved audit refers to a different source.');
        await this.environment.load(link.id,true);
        this.show(report);
    }

    show(report) {
        this.report=report;
        this.get('layers-bounds').value=report.boundsM.join(', ');
        this.get('layers-cell').value=report.requestedCellM;
        this.get('layers-band').replaceChildren(...report.bands.map((band,index)=>{
            const item=document.createElement('option');
            item.value=index;
            item.textContent=band.bottomM+'–'+band.topM+' m'+(band.varyingCells?' · changes':'');
            return item;
        }));
        this.get('layers-omit').checked=false;
        this.get('layers-preview').hidden=false;
        this.status(report.bands.length+' bands · '+report.solidBandCells.toLocaleString()+
            ' filled band-cells agree · '+report.varyingBandCells.toLocaleString()+' vary.');
        this.diagram();
        this.controls();
    }

    diagram() {
        const index=Number(this.get('layers-band').value);
        const band=this.report.bands[index];
        if(this.url) URL.revokeObjectURL(this.url);
        this.url=URL.createObjectURL(new Blob([this.report.diagrams[index]],{type:'image/svg+xml'}));
        this.get('layers-diagram').src=this.url;
        this.get('layers-band-result').textContent='Cuts at '+band.samples.map(sample=>Number(sample.heightM.toFixed(3))+' m').join(', ') +
            ' · '+band.solidCells+' agreed filled · '+band.varyingCells+' changing'+
            (band.openEdgesAcrossSamples?' · '+band.openEdgesAcrossSamples+' open edges excluded':'')+'.';
    }

    controls() {
        const needsChoice=this.report.varyingBandCells>0;
        this.get('layers-confirm').hidden=!needsChoice;
        this.get('layers-build').disabled=!this.report.solidBandCells||(needsChoice&&!this.get('layers-omit').checked);
    }

    async build() {
        const audit=this.report;
        const result=await this.call('rebuild',{auditId:audit.auditId,omitVarying:this.get('layers-omit').checked});
        await this.environment.load(result.id,true);
        this.show(audit);
        this.status(result.solidMeshes+' solids checked at '+result.witnessCuts+' heights · '+result.omittedBandCells+' changing band-cells omitted.');
    }

    async export() {
        const data=await this.call('export',{auditId:this.report.auditId});
        this.status('Saved audit JSON to '+data.path);
    }
}
