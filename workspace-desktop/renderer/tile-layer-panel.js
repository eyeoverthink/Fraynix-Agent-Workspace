import {tileDrawingOrder,tileScoreDefaults} from './media-tile-play.js';

export class TileLayerPanel {
    constructor(editor) {
        this.editor=editor;
        this.selected=new Set();
        this.root=document.createElement('details');
        this.root.id='tile-layers';
        this.root.innerHTML=`
          <summary>Layers, selection and score</summary>
          <div class="tile-bar">
            <button id="tile-new-plane">Add layer</button>
            <label>Layer to edit<select id="tile-plane"></select></label>
            <button id="tile-remove-plane">Remove layer</button>
          </div>
          <div class="tile-bar" id="tile-plane-fields">
            <label>Name<input data-plane="name" maxlength="50"></label>
            <label>Z level<input data-plane="z" type="number" min="-999" max="999" step="1"></label>
            <label>Opacity %<input data-plane="opacity" type="number" min="0" max="100" step="1"></label>
            <label><input data-plane="visible" type="checkbox">Visible</label>
          </div>
          <div class="tile-bar">
            <label>Show items<select id="tile-plane-filter"></select></label>
            <button id="tile-select-visible">Select shown</button>
            <button id="tile-select-clear">Clear selection</button>
            <output id="tile-selection-count"></output>
          </div>
          <div id="tile-selection-list" aria-label="Level objects"></div>
          <div class="tile-bar">
            <button id="tile-assign-plane">Move selected to layer</button>
            <label>Item Z offset<input id="tile-item-z" type="number" value="0" min="-99" max="99" step="1"></label>
            <button id="tile-assign-z">Set Z for selected</button>
          </div>
          <div class="tile-bar">
            <label>Pickup points<input data-scoring="pickup" type="number" min="0" max="10000" step="1"></label>
            <label>Goal points<input data-scoring="goal" type="number" min="0" max="10000" step="1"></label>
            <label>Reset penalty<input data-scoring="resetPenalty" type="number" min="0" max="10000" step="1"></label>
          </div>`;
        const el=id=>this.root.querySelector('#tile-'+id);
        this.plane=el('plane');this.filter=el('plane-filter');this.items=el('selection-list');
        this.newPlane=el('new-plane');this.removePlane=el('remove-plane');
        this.assign=el('assign-plane');this.assignZ=el('assign-z');
        this.plane.onchange=()=>this.refresh();
        this.filter.onchange=()=>this.refresh();
        this.newPlane.onclick=()=>{
            const layers=editor.layout.planes ||= [];
            if (layers.length>=16) {editor.tell('A level can hold up to 16 visual layers.');return;}
            const plane={id:crypto.randomUUID(),name:'Layer '+(layers.length+1),z:100,opacity:1,visible:true};
            layers.push(plane);
            this.replaceOptions(this.plane,'Automatic layer',layers,plane.id);
            editor.changed();
        };
        this.removePlane.onclick=()=>{
            const id=this.plane.value;
            editor.layout.planes=editor.layout.planes.filter(plane=>plane.id!==id);
            for (const item of editor.layout.objects) if (item.plane===id) delete item.plane;
            editor.changed();
        };
        for (const field of this.root.querySelectorAll('[data-plane]')) field.onchange=()=>{
            const plane=editor.layout.planes?.find(layer=>layer.id===this.plane.value);
            if (!plane||!field.reportValidity()) {this.refresh();return;}
            const key=field.dataset.plane;
            const value=key==='name'?field.value.trim():key==='visible'?field.checked:field.valueAsNumber;
            if ((key==='name'&&!value)||typeof value==='number'&&!Number.isInteger(value)) {this.refresh();return;}
            plane[key]=key==='opacity'?value/100:value;
            editor.changed();
        };
        for (const field of this.root.querySelectorAll('[data-scoring]')) field.onchange=()=>{
            if (!field.reportValidity()||!Number.isInteger(field.valueAsNumber)) {this.refresh();return;}
            editor.layout.scoring={...tileScoreDefaults,...editor.layout.scoring,[field.dataset.scoring]:field.valueAsNumber};
            editor.changed();
        };
        el('select-visible').onclick=()=>{
            for (const item of this.shown()) this.selected.add(item.id);
            editor.selected=[...this.selected].at(-1);
            this.redraw();
        };
        el('select-clear').onclick=()=>this.select(null,false);
        this.assign.onclick=()=>{
            for (const item of editor.layout.objects.filter(item=>this.selected.has(item.id))) {
                if (this.plane.value) item.plane=this.plane.value;
                else delete item.plane;
            }
            editor.changed();
        };
        this.assignZ.onclick=()=>{
            const field=el('item-z');
            if (!field.reportValidity()||!Number.isInteger(field.valueAsNumber)) return;
            for (const item of editor.layout.objects.filter(item=>this.selected.has(item.id))) item.z=field.valueAsNumber;
            editor.changed();
        };
    }

    select(id,extend) {
        if (!extend) this.selected.clear();
        if (id) {
            if (extend&&this.selected.has(id)) this.selected.delete(id);
            else this.selected.add(id);
        }
        this.editor.selected=this.selected.has(id)?id:[...this.selected].at(-1);
        this.redraw();
    }

    contains(item) {
        const filter=this.filter.value;
        return !filter || (filter==='automatic' ? !item.plane : item.plane===filter);
    }

    shown() {return tileDrawingOrder(this.editor.layout).reverse().map(row=>row.item).filter(item=>this.contains(item));}
    redraw() {this.editor.refresh();this.editor.draw();}

    replaceOptions(select,placeholder,planes,preferred=select.value) {
        select.replaceChildren(new Option(placeholder,''),...planes.map(plane=>new Option(plane.name,plane.id)));
        if ([...select.options].some(option=>option.value===preferred)) select.value=preferred;
    }

    refresh(busy=false) {
        const editor=this.editor;
        const planes=editor.layout.planes||[];
        for (const id of this.selected) if (!editor.layout.objects.some(item=>item.id===id)) this.selected.delete(id);
        if (editor.selected) this.selected.add(editor.selected);
        this.root.hidden=Boolean(editor.game);
        this.replaceOptions(this.plane,'Automatic layer',planes);
        this.replaceOptions(this.filter,'All layers',[{id:'automatic',name:'Automatic layers'},...planes]);
        const current=planes.find(plane=>plane.id===this.plane.value);
        for (const field of this.root.querySelectorAll('[data-plane]')) {
            field.disabled=busy||!current;
            const key=field.dataset.plane;
            if (key==='visible') field.checked=current?.visible??false;
            else field.value=current?(key==='opacity'?Math.round(current[key]*100):current[key]):'';
        }
        this.removePlane.disabled=busy||!current;
        this.newPlane.disabled=busy;
        this.plane.disabled=busy;
        this.filter.disabled=busy;
        this.assign.disabled=busy||!this.selected.size;
        this.assignZ.disabled=busy||!this.selected.size;
        this.root.querySelector('#tile-selection-count').textContent=this.selected.size+' selected';
        for (const field of this.root.querySelectorAll('[data-scoring]')) {
            field.disabled=busy;
            field.value=(editor.layout.scoring||tileScoreDefaults)[field.dataset.scoring];
        }
        const visible=this.shown();
        const list=visible.map(item=>[item.id,item.effect||item.layer,editor.sprites[item.assetId]?.label||'Sprite',this.selected.has(item.id)]);
        const signature=JSON.stringify([busy,list]);
        if (signature===this.signature) return;
        this.signature=signature;
        this.items.replaceChildren(...list.map(([id,role,name,checked])=>{
            const label=document.createElement('label');
            const box=document.createElement('input');
            Object.assign(box,{type:'checkbox',checked,disabled:busy});
            box.dataset.item=id;
            box.onchange=()=>this.select(id,true);
            label.append(box,role+' · '+name+' · '+id.slice(0,18));
            return label;
        }));
    }
}
