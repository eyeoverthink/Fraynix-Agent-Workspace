const motions = {
    Face: {frames: ['idle', 'blink', 'idle'], milliseconds: [1450, 130, 610]},
    Walk: {frames: ['walk-1', 'walk-2', 'walk-3', 'walk-4'], milliseconds: [140, 140, 140, 140]},
    Jump: {frames: ['idle', 'takeoff', 'airborne', 'landing'], milliseconds: [380, 150, 430, 175]}
};

export function characterRigRequest(text) {
    const match = /^(?:build|bake|make|create)\s+(?:an?\s+|the\s+)?(?:blender\s+)?(?:(teal|amber)\s+)?courier(?:\s+(?:avatar|character|rig))?[.!]?$/i.exec(text);
    return match ? (match[1]?.toLowerCase() || 'teal') : null;
}

export class CharacterRigPanel {
    constructor(parent, request, usePlayer) {
        this.call = (command, args) => request('character-rig', {command, args});
        this.usePlayer = usePlayer;
        this.root = document.createElement('details');
        this.root.id = 'character-rig';
        this.root.className = 'avatar-card';
        this.root.innerHTML = String.raw`
            <summary>Blender character and motion</summary>
            <div class="character-rig-body">
                <div class="character-rig-controls">
                    <div class="avatar-toolbar">
                        <label>Style<select id="character-rig-palette"><option value="teal">Teal Courier</option><option value="amber">Amber Courier</option></select></label>
                        <button class="primary" id="character-rig-bake">Bake character</button>
                        <label>Saved rigs<select id="character-rig-library"><option value="">Choose a rig</option></select></label>
                    </div>
                    <div id="character-rig-actions" hidden>
                        <div class="avatar-actions" aria-label="Motion preview">
                            <button data-motion="Face" aria-pressed="true">Face</button>
                            <button data-motion="Walk" aria-pressed="false">Walk</button>
                            <button data-motion="Jump" aria-pressed="false">Jump</button>
                        </div>
                        <div class="avatar-actions">
                            <button id="character-rig-player">Use as tile player</button>
                            <button id="character-rig-blend">Export editable Blender rig</button>
                            <button id="character-rig-atlas">Export pose sheet</button>
                        </div>
                    </div>
                    <p id="character-rig-status" role="status">Bake an original Courier with your local Blender installation.</p>
                </div>
                <img id="character-rig-preview" alt="Courier motion preview" width="96" height="112" hidden>
            </div>`;
        parent.prepend(this.root);
        this.library = this.find('library');
        this.palette = this.find('palette');
        this.preview = this.find('preview');
        this.status = this.find('status');
        this.actions = this.find('actions');
        this.motionButtons = [...this.root.querySelectorAll('[data-motion]')];
        this.motion = 'Face';
        this.find('bake').onclick = () => this.bake(this.palette.value);
        this.library.onchange = () => {
            const id = this.library.value;
            this.clear();
            if (id) this.run(async () => this.show(await this.call('open', {id})));
        };
        this.find('player').onclick = () => this.run(async () => {
            await this.usePlayer(await this.call('player', {id: this.rig.id}));
            this.status.textContent = 'Player applied. Save the tile level before playing.';
        });
        for (const kind of ['blend', 'atlas']) this.find(kind).onclick = () => this.run(async () => {
            this.status.textContent = 'Checking saved rig and writing export…';
            const result = await this.call(kind, {id: this.rig.id});
            this.status.textContent = 'Exported and verified ' + result.path;
        });
        for (const button of this.motionButtons) button.onclick = () => this.animate(button.dataset.motion);
        this.root.ontoggle = () => {
            if (this.root.open && !this.loaded && !this.busy) this.run(() => this.load());
            this.animate(this.motion);
        };
        document.addEventListener('visibilitychange', () => this.animate(this.motion));
    }

    find(id) { return this.root.querySelector('#character-rig-' + id); }

    async run(action) {
        if (this.busy) return;
        const controls = [this.library, this.palette, this.find('bake'), this.find('player'), this.find('blend'), this.find('atlas')];
        this.busy = true;
        for (const control of controls) control.disabled = true;
        try {
            return await action();
        } catch (error) {
            this.status.textContent = error.message;
        } finally {
            this.busy = false;
            for (const control of controls) control.disabled = false;
        }
    }

    async load(id) {
        const entries = await this.call('list');
        this.library.replaceChildren(new Option('Choose a rig', ''),
            ...entries.map(entry => new Option(entry.name + ' · ' + entry.id.slice(0, 8), entry.id)));
        this.library.value = id || this.rig?.id || '';
        this.loaded = true;
    }

    bake(palette) {
        this.root.open = true;
        this.palette.value = palette;
        return this.run(async () => {
            this.status.textContent = 'Baking nine poses and reopening the saved Blender rig…';
            const rig = await this.call('bake', {palette});
            await this.load(rig.id);
            this.show(rig);
            return rig;
        });
    }

    clear() {
        clearTimeout(this.timer);
        this.rig = null;
        this.actions.hidden = this.preview.hidden = true;
        this.preview.removeAttribute('src');
        this.status.textContent = 'Choose a saved rig or bake a new character.';
    }

    show(rig) {
        this.rig = rig;
        for (const frame of rig.frames) frame.url = 'data:image/png;base64,' + frame.png;
        this.actions.hidden = this.preview.hidden = false;
        this.status.textContent = rig.name + ' · ' + rig.inspection.bones + ' bones · 9 verified poses';
        this.animate('Face');
    }

    animate(motion) {
        clearTimeout(this.timer);
        this.motion = motion;
        for (const button of this.motionButtons) button.setAttribute('aria-pressed', String(button.dataset.motion === motion));
        if (this.paused || !this.rig || !this.root.open || document.hidden) return;
        const {frames, milliseconds} = motions[motion];
        const render = index => {
            this.preview.src = this.rig.frames.find(frame => frame.name === frames[index]).url;
            this.timer = setTimeout(() => render((index + 1) % frames.length), milliseconds[index]);
        };
        render(0);
    }

    setVisible(visible) {
        if (this.paused === !visible) return;
        this.paused = !visible;
        this.animate(this.motion);
    }
}
