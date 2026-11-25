const MARKERS = ['#ff8800','#2288ff','#20bf6b','#af4fe0'];
const MAX_PNG_BYTES = 512 * 1024;

export class ImageAssemblyPanel {
    constructor(spatial) {
        this.spatial = spatial;
        this.root = document.querySelector('#image-assembly');
        this.form = this.root.querySelector('form');
        this.rows = this.root.querySelector('.spatial-assembly-props');
        this.status = this.root.querySelector('[role=status]');
        this.mode = this.form.elements.namedItem('mode');
        this.witness = this.form.elements.namedItem('witness');
        this.add = this.root.querySelector('#image-assembly-add');
        this.replay = this.root.querySelector('#image-assembly-replay');
        this.assets = [];
        this.add.onclick = () => this.addRow();
        this.mode.onchange = () => this.controls();
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
            <label class="assembly-color">Marker<input name="color" type="color" value="${marker}"></label>
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
        if (this.sceneId !== scene?.id) {
            this.sceneId = scene?.id;
            if (receipt) this.mode.value = receipt.binding.registration?'views':'markers';
            this.status.textContent = receipt ? this.receiptSummary(receipt) : '';
        }
        const views = this.mode.value==='views';
        this.root.querySelector('#image-assembly-layout-label').textContent = views?'Overhead PNG':'Layout PNG';
        this.witness.required = views;
        this.witness.disabled = !views;
        this.root.querySelector('#image-assembly-witness-fields').hidden = !views;
        for (const label of this.rows.querySelectorAll('.assembly-color')) label.hidden = views;
        this.replay.hidden = !receipt;
        this.replay.disabled = this.spatial.busy || blocked;
        this.add.disabled = this.spatial.busy || blocked || this.rows.children.length>=MARKERS.length;
        this.root.querySelector('#image-assembly-build').disabled = this.spatial.busy || blocked || !this.assets.length;
    }

    receiptSummary(receipt) {
        const registered = receipt.binding.registration;
        if (!registered) return receipt.measured.length+' props · floor contact verified twice';
        const scores = registered.props.map(prop=>Math.round(100*prop.witnessIou)+'%');
        return receipt.measured.length+' positions verified in the second view · '+scores.join(' / ')+' overlap · floor contact verified twice';
    }

    async readPng(file) {
        if (!file || file.size>MAX_PNG_BYTES) throw Error('Choose an opaque PNG up to 512 KiB per view.');
        const bytes = new Uint8Array(await file.arrayBuffer());
        let binary = '';
        for (let offset=0; offset<bytes.length; offset+=8192)
            binary += String.fromCharCode(...bytes.subarray(offset,offset+8192));
        return btoa(binary);
    }

    async build() {
        const fields = this.form.elements;
        const views = this.mode.value==='views';
        const props = [...this.rows.children].map(row => Object.fromEntries(
            [...row.querySelectorAll('[name]')].filter(input=>!views||input.name!=='color')
                .map(input=>[input.name,input.value])));
        this.status.textContent = 'Checking the layout, source images and floor contacts…';
        try {
            const request = {layoutPng:await this.readPng(fields.namedItem('layout').files[0]),
                title:fields.namedItem('title').value,widthM:fields.namedItem('widthM').valueAsNumber,props};
            if (views) request.witnessPng = await this.readPng(this.witness.files[0]);
            const scene = await this.spatial.call('image-assembly',request);
            await this.spatial.open(scene.id);
            this.status.textContent = this.receiptSummary(scene.imageAssembly);
            this.spatial.notice('Image scene ready. Review the props or export the checked GLB.');
        } catch (error) {
            this.status.textContent = error.message.replace(/^Error invoking remote method '[^']+': (?:Error: )?/,'');
        }
    }

    async recheck() {
        try {
            const result = await this.spatial.call('image-assembly-replay',{id:this.spatial.scene.id});
            const positions = this.spatial.scene.imageAssembly.binding.registration;
            this.status.textContent = positions ? 'Both registered views rechecked. Fresh floor contacts matched for all '+result.measured.length+' props.'
                : 'Fresh floor contacts matched for all '+result.measured.length+' props.';
        } catch (error) {
            this.status.textContent = error.message.replace(/^Error invoking remote method '[^']+': (?:Error: )?/,'');
        }
    }
}
