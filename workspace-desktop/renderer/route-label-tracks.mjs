const field=(name,text,type,options='')=>'<label>'+text+'<input name="'+name+'" type="'+type+'" '+options+'></label>';
const meter=(name,text,min,max)=>field(name,text,'number','required min="'+min+'" max="'+max+'" step="any"');

export class RouteLabelTracks {
    constructor(anchor,save) {
        this.root=document.createElement('details');
        this.root.id='shot-label-tracks';
        this.root.className='spatial-detail';
        this.root.innerHTML=`
            <summary>Edit timed labels</summary>
            <form id="shot-track-form">
                <p class="spatial-note">Frames use 0–143. Start is included. End is the first hidden frame, or 144 to hold through the cut.</p>
                <div id="shot-track-rows"></div>
                <div class="spatial-task">
                    <button type="button" id="shot-add-label">Add label</button>
                    <button type="button" id="shot-reset-tracks">Reset draft</button>
                    <button class="primary" id="shot-save-tracks">Render timed labels</button>
                </div>
            </form>`;
        anchor.before(this.root);
        this.rows=this.root.querySelector('#shot-track-rows');
        this.add=this.root.querySelector('#shot-add-label');
        const form=this.root.querySelector('form');
        this.add.onclick=()=>{
            if (!form.reportValidity()) return;
            const rows=this.read();
            if (rows.length>=6) return;
            const target=this.targets.find(t=>t.id==='spare'&&!rows.some(row=>row.target===t.id))??
                this.targets.find(t=>!rows.some(row=>row.target===t.id))??this.targets[0];
            rows.push({id:'label-'+crypto.randomUUID().slice(0,8),target:target.id,firstFrame:0,endExclusive:144,
                style:{...this.saved[0].style,text:target.label.toUpperCase().replace(/[^A-Z0-9. -]/g,' ').trim().slice(0,20)||'OBJECT',
                    localOffsetM:[0,target.anchorY,0],color:'#baf3d3',enabled:true}});
            this.draw(rows);
        };
        this.root.querySelector('#shot-reset-tracks').onclick=()=>this.draw(this.saved);
        form.onsubmit=event=>{
            event.preventDefault();
            save(this.read());
        };
    }

    load(labels,style,crate,targets) {
        this.targets=targets;
        this.saved=labels??[{id:'label-1',target:crate,firstFrame:0,endExclusive:144,style}];
        this.draw(this.saved);
    }

    draw(labels) {
        this.rows.replaceChildren();
        labels.forEach((label,index)=>{
            const card=document.createElement('fieldset');
            card.dataset.labelId=label.id;
            card.innerHTML=`<legend>Label ${index+1}</legend>
                <div class="spatial-task">
                    <label>Target<select name="target"></select></label>
                    ${field('text','Text','text','maxlength="20" required')}
                    ${field('color','Color','color')}
                    ${field('firstFrame','Start frame','number','required min="0" max="143" step="1"')}
                    ${field('endExclusive','End frame (exclusive)','number','required min="1" max="144" step="1"')}
                    <label><input type="checkbox" name="enabled"> Show label</label>
                </div>
                <div class="spatial-task">
                    ${meter('x','Local X · m',-1.4,1.4)}${meter('y','Local Y · m',-1.4,1.4)}${meter('z','Local Z · m',-1.4,1.4)}
                    ${meter('widthM','Width · m',.20,1.40)}${meter('heightM','Height · m',.07,.45)}
                    <button type="button" class="remove-shot-label">Remove label</button>
                </div>`;
            const get=name=>card.querySelector('[name="'+name+'"]');
            const target=get('target');
            for (const item of this.targets) target.add(new Option(item.label,item.id));
            const {text,color,localOffsetM:[x,y,z],widthM,heightM,enabled}=label.style;
            const values={target:label.target,text,color,x,y,z,widthM,heightM,firstFrame:label.firstFrame,endExclusive:label.endExclusive};
            for (const [key,value] of Object.entries(values)) get(key).value=value;
            get('enabled').checked=enabled;
            target.onchange=()=>{get('y').value=this.targets.find(item=>item.id===target.value).anchorY;};
            const remove=card.querySelector('.remove-shot-label');
            remove.disabled=labels.length<2;
            remove.onclick=()=>this.draw(this.read().filter(row=>row.id!==label.id));
            this.rows.append(card);
        });
        this.add.disabled=labels.length>=6;
    }

    read() {
        return [...this.rows.children].map(card=>{
            const get=name=>card.querySelector('[name="'+name+'"]');
            const n=name=>get(name).valueAsNumber;
            return {id:card.dataset.labelId,target:get('target').value,firstFrame:n('firstFrame'),endExclusive:n('endExclusive'),
                style:{enabled:get('enabled').checked,text:get('text').value,color:get('color').value,
                    localOffsetM:[n('x'),n('y'),n('z')],widthM:n('widthM'),heightM:n('heightM')}};
        });
    }
}
