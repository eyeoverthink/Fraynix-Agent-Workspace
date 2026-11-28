const number = (name,label,min,max,step='any') =>
    '<label>'+label+'<input type="number" name="'+name+'" min="'+min+'" max="'+max+'" step="'+step+'" required></label>';

export class RouteOverlayEditor {
    constructor(anchor,save) {
        this.root=document.createElement('details');
        this.root.id='shot-overlays';
        this.root.className='spatial-detail';
        this.root.innerHTML=`
            <summary>Edit 3D overlays</summary>
            <form id="shot-overlay-form">
                <fieldset>
                    <legend>Object label</legend>
                    <div class="spatial-task">
                        <label><input type="checkbox" name="labelEnabled"> Show label</label>
                        <label>Text<input type="text" name="labelText" maxlength="20" title="Use letters, numbers, spaces, a dot or a dash" required></label>
                        <label>Label color<input type="color" name="labelColor"></label>
                        ${number('labelWidth','Width · m',.20,1.40)}
                        ${number('labelHeight','Height · m',.07,.45)}
                    </div>
                    <div class="spatial-task">
                        ${number('anchorX','Crate local X · m',-1.4,1.4)}
                        ${number('anchorY','Crate local Y · m',-1.4,1.4)}
                        ${number('anchorZ','Crate local Z · m',-1.4,1.4)}
                    </div>
                </fieldset>
                <fieldset>
                    <legend>Floor route line</legend>
                    <div class="spatial-task">
                        <label><input type="checkbox" name="lineEnabled"> Show route line</label>
                        <label>Route color<input type="color" name="lineColor"></label>
                        ${number('lineWidth','Line width · m',.012,.14)}
                        ${number('lineElevation','Above floor · m',.008,.08)}
                    </div>
                </fieldset>
                <div class="spatial-task">
                    <button type="button" id="shot-reset-overlays">Reset draft</button>
                    <button class="primary" id="shot-save-overlays">Render overlay revision</button>
                </div>
            </form>`;
        anchor.before(this.root);
        this.form=this.root.querySelector('form');
        this.root.querySelector('#shot-reset-overlays').onclick=()=>this.draw(this.saved);
        this.form.onsubmit=event=>{
            event.preventDefault();
            save(this.read());
        };
    }

    field(name) { return this.form.elements.namedItem(name); }

    load(value,timed=false) {
        this.form.querySelector("fieldset").hidden=timed;
        this.saved=value;
        this.draw(value);
    }

    draw({label,routeLine}) {
        this.field('labelEnabled').checked=label.enabled;
        this.field('lineEnabled').checked=routeLine.enabled;
        const values={
            labelText:label.text,labelColor:label.color,labelWidth:label.widthM,labelHeight:label.heightM,
            anchorX:label.localOffsetM[0],anchorY:label.localOffsetM[1],anchorZ:label.localOffsetM[2],
            lineColor:routeLine.color,lineWidth:routeLine.widthM,lineElevation:routeLine.elevationM,
        };
        for (const [name,value] of Object.entries(values)) this.field(name).value=value;
    }

    read() {
        const n = field=>this.field(field).valueAsNumber;
        return {
            label:{
                enabled:this.field('labelEnabled').checked,text:this.field('labelText').value,
                color:this.field('labelColor').value,widthM:n('labelWidth'),heightM:n('labelHeight'),
                localOffsetM:[n('anchorX'),n('anchorY'),n('anchorZ')],
            },
            routeLine:{
                enabled:this.field('lineEnabled').checked,color:this.field('lineColor').value,
                widthM:n('lineWidth'),elevationM:n('lineElevation'),
            },
        };
    }
}
