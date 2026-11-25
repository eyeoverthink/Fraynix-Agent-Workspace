const MARKERS = ['#ff8800','#2288ff','#20bf6b','#af4fe0'];
const MAX_PNG_BYTES = 512 * 1024;

export class ImageAssemblyPanel {
    constructor(spatial) {
        this.spatial = spatial;
        this.root = document.querySelector('#image-assembly');
        this.form = this.root.querySelector('form');
        this.rows = this.root.querySelector('.spatial-assembly-props');
        this.status = this.root.querySelector('[role=status]');
        this.add = this.root.querySelector('#image-assembly-add');
        this.replay = this.root.querySelector('#image-assembly-replay');
        this.assets = [];
        this.add.onclick = () => this.addRow();
        this.root.addEventListener('toggle', () => {
            if (this.root.open) spatial.action(() => this.refresh());
        });
        this.form.onsubmit = event => {
            event.preventDefault();
            spatial.action(() => this.build());
        };
        this.replay.onclick = () => spatial.action(() => this.recheck());
        this.addRow();
        this.addRow();
    }

    addRow() {
        const index = this.rows.children.length;
        if (index >= MARKERS.length) return;
        const colours = [...this.rows.querySelectorAll('input[name=color]')].map(input=>input.value);
        const marker = MARKERS.find(value=>!colours.includes(value));
        const row = document.createElement('div');
        row.className = 'spatial-task';
        row.innerHTML = `
            <label>Checked prop<select name="sceneId" required></select></label>
            <label>Name<input name="label" value="Image prop ${index+1}" maxlength="70" required></label>
            <label>Marker<input name="color" type="color" value="${marker}"></label>
            ${index>=2?'<button type="button">Remove</button>':''}`;
        row.querySelector('button')?.addEventListener('click', () => {
            row.remove();
            this.controls();
        });
        this.rows.append(row);
        this.options(row.querySelector('select'));
        this.controls();
    }

    options(select) {
        const selected = select.value;
        select.replaceChildren(new Option('Choose a checked model',''), ...this.assets.map(asset =>
            new Option(asset.title+' · '+asset.sizeM.map(x=>Math.round(x*1000)).join(' × ')+' mm · '+asset.id.slice(0,8), asset.id)));
        if (this.assets.some(asset=>asset.id===selected)) select.value = selected;
    }

    async refresh() {
        const {assets} = await this.spatial.call('image-assembly-assets');
        this.assets = assets;
        for (const select of this.rows.querySelectorAll('select')) this.options(select);
        this.controls();
    }

    controls(blocked = this.blocked) {
        this.blocked = blocked;
        const scene = this.spatial.worldMode ? null : this.spatial.scene;
        const receipt = scene?.imageAssembly;
        this.replay.hidden = !receipt;
        this.replay.disabled = this.spatial.busy || blocked;
        this.add.disabled = this.spatial.busy || blocked || this.rows.children.length>=MARKERS.length;
        this.root.querySelector('#image-assembly-build').disabled = this.spatial.busy || blocked || !this.assets.length;
        if (this.sceneId !== scene?.id) {
            this.sceneId = scene?.id;
            this.status.textContent = receipt ? receipt.measured.length+' props · floor contact verified twice' : '';
        }
    }

    async build() {
        const fields = this.form.elements;
        const file = fields.namedItem('layout').files[0];
        if (!file || file.size>MAX_PNG_BYTES) throw Error('Choose an opaque layout PNG up to 512 KiB.');
        const bytes = new Uint8Array(await file.arrayBuffer());
        let binary = '';
        for (let offset=0; offset<bytes.length; offset+=8192)
            binary += String.fromCharCode(...bytes.subarray(offset,offset+8192));
        const props = [...this.rows.children].map(row => Object.fromEntries(
            [...row.querySelectorAll('[name]')].map(input=>[input.name,input.value])));
        this.status.textContent = 'Checking the layout, source images and floor contacts…';
        try {
            const scene = await this.spatial.call('image-assembly',{layoutPng:btoa(binary),
                title:fields.namedItem('title').value,widthM:fields.namedItem('widthM').valueAsNumber,props});
            await this.spatial.open(scene.id);
            this.status.textContent = scene.imageAssembly.measured.length+' props · floor contact verified twice';
            this.spatial.notice('Image scene ready. Review the props or export the checked GLB.');
        } catch (error) {
            this.status.textContent = error.message.replace(/^Error invoking remote method '[^']+': (?:Error: )?/,'');
        }
    }

    async recheck() {
        try {
            const result = await this.spatial.call('image-assembly-replay',{id:this.spatial.scene.id});
            this.status.textContent = 'Fresh floor contacts matched for all '+result.measured.length+' props.';
        } catch (error) {
            this.status.textContent = error.message.replace(/^Error invoking remote method '[^']+': (?:Error: )?/,'');
        }
    }
}
