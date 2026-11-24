
import {mountGame} from './media-play.js';

const layoutFields = ['title', 'backdropX', 'platformX', 'platformY', 'pickupX', 'foregroundX'];

async function readBase64(file, max) {
    if (file.size > max) throw new Error('This file exceeds the media size limit.');
    const bytes = new Uint8Array(await file.arrayBuffer());
    let binary = '';
    for (let i = 0; i < bytes.length; i += 16384)
        binary += String.fromCharCode(...bytes.subarray(i, i + 16384));
    return btoa(binary);
}

export class MediaWorkshop {
    constructor(request) {
        this.request = request;
        this.root = document.createElement('section');
        this.root.id = 'media-workshop';
        this.root.hidden = true;
        this.root.innerHTML = `
            <div class="media-source">
                <div class="media-actions">
                    <button id="media-pick">Pick in browser</button>
                    <label class="media-file">Import PNG<input id="media-png" type="file" accept=".png,image/png" aria-label="Import PNG"></label>
                    <label class="media-file">Open level<input id="media-level" type="file" accept=".json,application/json" aria-label="Open editable level"></label>
                </div>
                <div id="media-source-preview" hidden>
                    <img id="media-sprite" alt="Isolated sprite on a transparency grid">
                    <span id="media-source-label"></span>
                </div>
            </div>
            <form id="media-layout">
                <label class="media-title">Level name<input name="title" value="Sprite Relay" maxlength="70" required></label>
                <div class="media-fields">
                    <label>Backdrop X<input name="backdropX" type="number" min="80" max="625" value="440" required></label>
                    <label>Platform X<input name="platformX" type="number" min="150" max="505" value="276" required></label>
                    <label>Platform top Y<input name="platformY" type="number" min="186" max="246" value="242" required></label>
                    <label>Pickup X<input name="pickupX" type="number" value="332" required></label>
                    <label>Foreground X<input name="foregroundX" type="number" min="80" max="670" value="555" required></label>
                </div>
                <button id="media-save" class="primary" disabled>Save level</button>
            </form>
            <p id="media-status" role="status">Pick a contrasting graphic or import a PNG with transparency.</p>
            <div id="media-play-area" hidden>
                <canvas id="media-canvas" tabindex="0" aria-label="Playable level" aria-describedby="media-controls"></canvas>
                <div class="media-play-row">
                    <p id="media-controls">Move with ← and →. Jump with ↑ or Space.</p>
                    <button id="media-restart">Play from start</button>
                </div>
                <p id="media-progress" role="status"></p>
            </div>
            <div id="media-export" class="media-actions" hidden>
                <button data-format="html">Export playable HTML</button>
                <button data-format="json">Export editable level</button>
                <button data-format="png">Export sprite PNG</button>
            </div>`;
        document.querySelector('#canvas-surface').before(this.root);
        this.form = this.find('#media-layout');
        this.form.oninput = () => {
            this.dirty = true;
            this.updatePickupBounds();
            this.startGame();
            this.refreshButtons();
        };
        this.updatePickupBounds();
        this.form.onsubmit = event => {
            event.preventDefault();
            const layout = this.readLayout();
            this.run(async () => {
                const binding = this.source ? {source:this.source} : {fromId:this.level.id};
                this.open(await this.call('save', {...binding, layout}), 'Saved level. Source and sprite checked.');
            });
        };
        this.find('#media-pick').onclick = () => this.run(async () => {
            this.tell('Click the graphic in the in-app browser.');
            const {source, sprite} = await this.call('pick');
            this.accept(source, sprite);
        });
        this.find('#media-png').onchange = event => this.choose(event, async file => {
            const source = {filename:file.name, png:await readBase64(file, 750_000)};
            this.accept(source, await this.call('isolate', {source}));
        });
        this.find('#media-level').onchange = event => this.choose(event, async file => {
            const base64 = await readBase64(file, 1_050_000);
            this.open(await this.call('import', {base64}), 'Opened editable level. Source and sprite checked.');
        });
        this.find('#media-restart').onclick = () => {
            this.game?.restart();
            this.find('#media-canvas').focus();
        };
        for (const button of this.root.querySelectorAll('[data-format]')) {
            button.onclick = () => this.run(async () => {
                this.tell('Exporting checked level files…');
                const report = await this.call('export', {id:this.level.id, format:button.dataset.format});
                this.tell('Saved ' + report.path);
            });
        }
    }

    find(selector) {
        return this.root.querySelector(selector);
    }

    call(command, args = {}) {
        return this.request('media', {command, args});
    }

    tell(text) {
        this.find('#media-status').textContent = text;
    }

    async run(action) {
        if (this.busy) return;
        this.busy = true;
        this.refreshButtons();
        try {
            await action();
        } catch (error) {
            this.tell(error.message);
        } finally {
            this.busy = false;
            this.refreshButtons();
        }
    }

    choose(event, use) {
        const file = event.target.files[0];
        event.target.value = '';
        if (file) this.run(() => use(file));
    }

    readLayout() {
        return Object.fromEntries(layoutFields.map(name => {
            const input = this.form.elements.namedItem(name);
            return [name, name === 'title' ? input.value : input.valueAsNumber];
        }));
    }

    updatePickupBounds() {
        const x = this.form.platformX.valueAsNumber;
        this.form.pickupX.min = x + 20;
        this.form.pickupX.max = x + 110;
    }

    show(visible) {
        if (this.visible === visible) return;
        this.visible = visible;
        this.root.hidden = !visible;
        if (!visible) {
            this.closeGame();
        } else if (!this.loaded) {
            this.loaded = true;
            this.run(async () => {
                const level = await this.call('latest');
                if (level.id) this.open(level, 'Reopened saved level. Source and sprite checked.');
            });
        } else {
            this.startGame();
        }
    }

    accept(source, sprite) {
        this.source = source;
        this.dirty = true;
        this.previewSprite(sprite);
        this.startGame();
        this.tell('Sprite isolated. Set the layer positions and save your level.');
    }

    previewSprite(sprite) {
        this.sprite = sprite;
        this.find('#media-source-preview').hidden = false;
        this.find('#media-sprite').src = 'data:image/png;base64,' + sprite.png;
        this.find('#media-source-label').textContent =
            sprite.width + ' × ' + sprite.height + ' px · ' + sprite.visible + ' visible pixels';
    }

    open(level, message) {
        this.level = level;
        this.source = null;
        this.dirty = false;
        for (const name of layoutFields) this.form.elements.namedItem(name).value = level.layout[name];
        this.updatePickupBounds();
        this.previewSprite(level.sprite);
        this.startGame();
        this.tell(message);
    }

    refreshButtons() {
        this.find('#media-pick').disabled = this.busy;
        this.find('#media-png').disabled = this.busy;
        this.find('#media-level').disabled = this.busy;
        this.find('#media-save').disabled = this.busy || !this.dirty || !(this.source || this.level);
        this.find('#media-play-area').hidden = !this.sprite || !this.form.checkValidity();
        this.find('#media-restart').disabled = this.dirty;
        this.find('#media-export').hidden = !this.level || this.dirty;
        for (const button of this.root.querySelectorAll('[data-format]')) button.disabled = this.busy;
    }

    startGame() {
        this.closeGame();
        if (!this.visible || !this.sprite || !this.form.checkValidity()) return;
        const canvas = this.find('#media-canvas');
        canvas.tabIndex = this.dirty ? -1 : 0;
        this.game = mountGame(canvas, this.find('#media-progress'),
            {layout:this.readLayout(), sprite:this.sprite}, !this.dirty);
    }

    closeGame() {
        this.game?.close();
        this.game = null;
    }
}
