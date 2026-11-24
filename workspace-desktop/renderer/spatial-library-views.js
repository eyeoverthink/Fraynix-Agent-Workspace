
const roles = ['front', 'side', 'top', 'heldOut'];
const captions = {front:'Front', side:'Left side', top:'Top (optional)', heldOut:'Held-out 45° view'};

export class SpatialLibraryViews {
    constructor(parent) {
        this.parent = parent;
        this.root = document.createElement('details');
        this.root.className = 'spatial-detail';
        this.root.id = 'spatial-library-views';
        this.root.innerHTML = `
            <summary>Model from sprite library</summary>
            <p class="spatial-note">Import square, equally scaled views of the same prop in Media. Keep the original margins. Use a separate 45° view for verification.</p>
            <form class="spatial-task" id="library-views-form">
                <label>Prop name<input id="library-view-name" value="Library prop" maxlength="70" required></label>
                <label>Known front width · m<input id="library-view-width" type="number" min=".1" max="5" step="any" value=".5" required></label>
                <label>Held-out elevation<select id="library-view-elevation"><option value="0">Level</option><option value="60">60° above</option></select></label>
                <div class="spatial-task" id="library-view-picks"></div>
                <button id="library-views-build" class="primary" disabled>Fit and verify prop</button>
            </form>
            <p id="library-views-status" class="spatial-note" role="status">Choose the views from your sprite library.</p>`;
        document.querySelector('#spatial-browser-views').before(this.root);
        this.options = {};
        for (const role of roles) {
            const label = document.createElement('label');
            label.append(captions[role]);
            const select = document.createElement('select');
            select.id = 'library-view-' + role;
            select.add(new Option(role === 'top' ? 'No top view' : 'Choose sprite', ''));
            select.onchange = () => this.controls(this.blocked);
            label.append(select);
            this.find('#library-view-picks').append(label);
            this.options[role] = select;
        }
        this.root.ontoggle = () => {
            if (this.root.open && !this.alreadyOpened) this.parent.action(() => this.load());
            this.alreadyOpened = false;
        };
        this.find('form').onsubmit = event => {
            event.preventDefault();
            this.parent.action(() => this.build());
        };
        this.controls(false);
    }

    find(selector) {
        return this.root.querySelector(selector);
    }

    tell(message) {
        this.find('#library-views-status').textContent = message;
    }

    async open(preferredFront) {
        if (!this.root.open) {
            this.alreadyOpened = true;
            this.root.open = true;
        }
        await this.load(preferredFront);
        this.root.scrollIntoView({block:'start'});
    }

    async load(preferredFront) {
        const {assets} = await this.parent.call('library-sprites');
        const available = assets.filter(item => !item.unavailable);
        const names = new Map();
        for (const item of available) names.set(item.label, (names.get(item.label) || 0) + 1);
        for (const role of roles) {
            const control = this.options[role];
            const selected = role === 'front' && preferredFront ? preferredFront : control.value;
            control.replaceChildren(new Option(role === 'top' ? 'No top view' : 'Choose sprite', ''),
                ...available.map(item => {
                    const label = names.get(item.label) > 1
                        ? item.label + ' · ' + item.id.slice(0,6) : item.label;
                    return new Option(label, item.id);
                }));
            if (available.some(item => item.id === selected)) control.value = selected;
        }
        if (available.length < 3) this.tell('Add front, left side and a separate held-out PNG in Media first.');
        this.controls(this.blocked);
    }

    controls(blocked) {
        this.blocked = blocked;
        const busy = this.parent.busy || blocked;
        for (const input of this.find('form').querySelectorAll('select,input')) input.disabled = busy;
        const selected = roles.map(role => this.options[role].value);
        const used = selected.filter(Boolean);
        this.find('#library-views-build').disabled = busy || !selected[0] || !selected[1] || !selected[3]
            || new Set(used).size !== used.length;
    }

    async build() {
        const assets = Object.fromEntries(roles.map(role => [role, this.options[role].value]).filter(([,id]) => id));
        this.tell('Fitting the registered originals and checking the separate witness…');
        try {
            const scene = await this.parent.call('library-views', {
                name:this.find('#library-view-name').value.trim(),
                widthM:this.find('#library-view-width').valueAsNumber,
                heldOutElevationDeg:Number(this.find('#library-view-elevation').value),
                assets
            });
            await this.parent.open(scene.id);
            this.parent.notice("Verified library prop saved in Spatial.");
        } catch (error) {
            this.tell(error.message);
        }
    }

    show(proof) {
        if (proof?.viewSource !== 'sprite-library') {
            if (this.shown) this.tell('Choose the views from your sprite library.');
            this.shown = false;
            return;
        }
        this.shown = true;
        const views = proof.views.top ? 4 : 3;
        this.tell(views + ' checked library originals · ' + (proof.heldOutIou*100).toFixed(1)
            + '% held-out silhouette overlap · ' + proof.triangles + ' editable triangles · separate enclosing collider.');
    }
}
