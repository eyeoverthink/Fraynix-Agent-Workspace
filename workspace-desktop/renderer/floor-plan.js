import {readBase64} from './media-workshop.js';

const MAX_SOURCE=4*1024*1024;

export class FloorPlanPanel {
    constructor(environment) {
        this.environment=environment;
        this.root=environment.find('environment-plans');
        this.root.innerHTML=`
          <summary>Floor plan ↔ 3D</summary>
          <div class="environment-plan-fields">
            <label>PNG width · m <input id="plan-width" type="number" placeholder="8.0" min=".02" max="500" step=".01"></label>
            <label>Wall height · m <input id="plan-wall-height" type="number" value="2.6" min=".15" max="20" step=".01"></label>
            <label class="spatial-file">Import plan PNG or JSON<input id="plan-file" type="file" accept=".png,.json"></label>
          </div>
          <p>For PNG, use dark wall areas on a light or transparent background and enter the full image width.</p>
          <div class="environment-plan-fields">
            <label>Cut height · m <input id="plan-height" type="number" value="1.2" step=".01"></label>
            <label>Cell size · m <input id="plan-cell" type="number" value=".10" min=".002" max="2" step=".001"></label>
            <button id="plan-section" disabled>Make 2D plan</button>
            <button id="plan-glb" disabled>Save GLB</button>
          </div>
          <p id="plan-result" role="status">Import a plan or open a 3D environment to make a section.</p>
          <div id="plan-preview" hidden>
            <img id="plan-diagram" alt="Horizontal section with closed geometry in teal and open edges in orange">
            <div class="environment-plan-fields">
              <button id="plan-svg">Save SVG</button>
              <button id="plan-json">Save plan JSON</button>
              <button id="plan-rebuild">Build 3D from this cut</button>
            </div>
          </div>`;
        this.get('plan-file').onchange=event=>{
            const file=event.target.files[0];
            event.target.value='';
            if (file) this.run(async()=>this.build(file.name,await readBase64(file,MAX_SOURCE)));
        };
        this.get('plan-section').onclick=()=>this.run(()=>this.section());
        this.get('plan-rebuild').onclick=()=>this.run(()=>this.build('section.json',btoa(JSON.stringify(this.last.plan))));
        for (const format of ['glb','svg','json'])
            this.get('plan-'+format).onclick=()=>this.run(()=>this.save(format));
        for (const id of ['plan-height','plan-cell'])
            this.get(id).oninput=()=>this.clear('Cut settings changed. Make a new 2D plan.');
    }

    get(id) {return this.root.querySelector('#'+id);}
    number(id) {return this.get(id).valueAsNumber;}
    status(text) {this.get('plan-result').textContent=text;}

    run(action) {
        return this.environment.run(async()=>{
            try {await action();}
            catch (error) {this.status(error.message.replace(/^Error invoking remote method '[^']+': (?:Error: )?/,''));}
        });
    }

    clear(note) {
        this.last=null;
        if (this.url) URL.revokeObjectURL(this.url);
        this.url=null;
        this.get('plan-preview').hidden=true;
        this.status(note);
    }

    bind() {
        this.clear('Make a horizontal cut from the current 3D environment.');
        this.get('plan-section').disabled=false;
        this.get('plan-glb').disabled=false;
    }

    async build(filename,base64) {
        this.status('Building wall meshes and checking their horizontal section…');
        const args={filename,base64,wallHeightM:this.number('plan-wall-height')};
        if (/\.png$/i.test(filename)) {
            args.widthM=this.number('plan-width');
            if (!Number.isFinite(args.widthM) || args.widthM<=0) throw Error('Enter the full PNG width in metres.');
        }
        const result=await this.environment.call('floor-plan-build',args);
        await this.environment.load(result.id,true);
        this.clear(result.wallMeshes+' wall meshes · '+result.verifiedCells.toLocaleString()+' plan cells verified against the mesh.');
        this.get('plan-height').value=result.sectionHeightM;
    }

    async section(height=this.number('plan-height')) {
        if (!this.environment.catalog) await this.environment.load();
        const id=this.environment.catalog?.id;
        if (!id) throw Error('Open a 3D environment in the Environment tab first.');
        this.get('plan-height').value=height;
        this.clear('Measuring triangle intersections…');
        const cellM=this.number('plan-cell');
        const data=await this.environment.call('floor-plan-section',{id,heightM:height,cellM});
        this.last=data;
        this.get('plan-cell').value=data.plan.grid.cellM;
        this.last.requestedCellM=cellM;
        this.url=URL.createObjectURL(new Blob([data.svg],{type:'image/svg+xml'}));
        this.get('plan-diagram').src=this.url;
        this.get('plan-preview').hidden=false;
        this.get('plan-rebuild').disabled=!data.occupiedCells;
        const grid=data.plan.grid;
        this.status('Cut at '+data.heightM+' m · '+grid.width+' × '+grid.depth+' cells · '+data.occupiedCells+
            ' filled · '+data.openEdges+' open edges'+(data.openEdges?' (orange, SVG only)':'')+'.');
        return data;
    }

    async save(format) {
        const args={id:this.environment.catalog.id,format};
        if (format!=='glb') Object.assign(args,{heightM:this.last.heightM,cellM:this.last.requestedCellM});
        const saved=await this.environment.call('export-plan',args);
        this.status('Saved '+format.toUpperCase()+' to '+saved.path);
    }
}
