
import {readBase64} from './media-workshop.js';
import {drawTileLevel, mountTileGame, tileImages, tileWorld} from './media-tile-play.js';

const {width, height, floor, tile} = tileWorld;
const clamp = (value, min, max) => Math.max(min, Math.min(max, value));
const spriteLayers = ['foreground', 'pickup', 'terrain', 'backdrop'];
const modes = ['select', 'paint', 'erase'];
const layoutAssets = layout => new Set([
    ...layout.objects.map(item => item.assetId), layout.stage?.hero?.assetId, layout.stage?.backdropAsset
].filter(Boolean));

export class TileEditor {
    constructor(request) {
        this.request = request;
        this.sprites = {};
        this.images = new Map();
        this.layout = {title:'Untitled level', objects:[]};
        this.mode = 'select';
        this.root = document.createElement('section');
        this.root.id = 'tile-editor';
        this.root.hidden = true;
        this.root.innerHTML = `
            <div class="tile-bar">
                <button id="tile-new">New level</button>
                <select id="tile-saved" aria-label="Saved tile levels"><option value="">Open saved level</option></select>
                <label class="media-file">Import level<input id="tile-import-level" type="file" accept=".json,application/json" aria-label="Import tile level"></label>
                <label class="media-file">Import Nexus design<input id="tile-import-nexus" type="file"
                    accept=".json,.html,application/json,text/html" aria-label="Import Nexus design"></label>
                <button id="tile-model-3d">Model sprites in 3D</button>
                <button id="media-legacy-mode" class="quiet">Single sprite</button>
                <button id="media-motion-mode">Motion media</button>
                <button id="media-timeline-mode">Timeline</button>
            </div>
            <div class="tile-library">
                <div class="tile-bar">
                    <strong>Sprite library</strong>
                    <label class="media-file">Import PNG<input id="tile-png" type="file" accept=".png,image/png" aria-label="Add sprite PNG"></label>
                    <button id="tile-pick">Pick in browser</button>
                    <button id="tile-export-png">Export PNG</button>
                </div>
                <div id="tile-assets" aria-label="Choose a sprite"></div>
            </div>
            <div class="tile-bar tile-scene">
                <label class="tile-name">Level name<input id="tile-title" maxlength="70" value="Untitled level"></label>
                <button id="tile-save" class="primary">Save level</button>
                <button id="tile-play">Play level</button>
                <button id="tile-html">Export offline HTML</button>
                <button id="tile-json">Export editable JSON</button>
            </div>
            <div class="tile-bar">
                <div id="tile-modes" class="tile-bar" role="group" aria-label="Canvas tools">
                    <button data-mode="select" aria-pressed="true">Select / move</button>
                    <button data-mode="paint" aria-pressed="false">Paint tiles</button>
                    <button data-mode="erase" aria-pressed="false">Erase tiles</button>
                </div>
                <label class="tile-layer">Layer <select id="tile-layer"><option value="backdrop">Backdrop</option>
                    <option value="terrain">Solid prop</option><option value="pickup">Pickup</option>
                    <option value="foreground">Foreground</option></select></label>
                <button id="tile-place">Add sprite</button>
            </div>
            <details id="tile-stage" hidden>
                <summary>Nexus stage and playable character</summary>
                <p id="tile-stage-hero"></p>
                <p id="tile-stage-style"></p>
                <div class="tile-bar">
                    <label>Spawn X<input type="number" min="0" max="696" step="1" data-stage="spawn.x"></label>
                    <label>Spawn Y<input type="number" min="0" max="280" step="1" data-stage="spawn.y"></label>
                    <label>Goal X<input type="number" min="0" max="696" step="1" data-stage="goal.x"></label>
                    <label>Goal Y<input type="number" min="0" max="288" step="1" data-stage="goal.y"></label>
                </div>
            </details>
            <p id="tile-status" role="status">Import a sprite to begin. Place at least one pickup before saving.</p>
            <canvas id="tile-canvas" width="720" height="336" tabindex="-1" aria-label="Tile level stage" aria-describedby="tile-progress"></canvas>
            <div class="tile-bar">
                <p id="tile-progress" role="status"></p>
                <button id="tile-stop" hidden>Back to editing</button>
            </div>
            <fieldset id="tile-inspector" hidden>
                <legend id="tile-selected">Selected item</legend>
                <div class="tile-bar">
                    <label>X <input data-position="x" type="number" min="0" step="1"></label>
                    <label>Y <input data-position="y" type="number" min="0" step="1"></label>
                    <label>Size <input data-position="size" type="number" min="16" max="192" step="1"></label>
                    <label id="tile-solid-label"><input id="tile-solid" type="checkbox"> Solid collider</label>
                    <button id="tile-delete">Delete item</button>
                </div>
                <div id="tile-collider" class="tile-bar">
                    <label>Offset X <input data-collider="x" type="number" min="0" step="1"></label>
                    <label>Offset Y <input data-collider="y" type="number" min="0" step="1"></label>
                    <label>Width <input data-collider="w" type="number" min="1" step="1"></label>
                    <label>Height <input data-collider="h" type="number" min="1" step="1"></label>
                </div>
            </fieldset>`;
        document.querySelector('#canvas-surface').before(this.root);
        this.canvas = this.find('#tile-canvas');
        this.title = this.find('#tile-title');
        this.saved = this.find('#tile-saved');

        this.find('#tile-new').onclick = () => {
            if (this.canReplace()) {
                this.setLevel({layout:{title:'Untitled level', objects:[]}});
                this.tell('New level. Add a pickup and paint or place terrain.');
            }
        };
        this.saved.onchange = () => {
            const id = this.saved.value;
            this.saved.value = this.levelId || '';
            if (id && this.canReplace()) this.run(async () => {
                this.setLevel(await this.call('open', {id}));
                this.tell('Opened checked level.');
            });
        };
        this.title.oninput = () => {
            this.layout.title = this.title.value;
            this.changed();
        };
        this.find('#tile-save').onclick = () => this.run(async () => {
            this.setLevel(await this.call('save', {layout:this.layout}), this.selected);
            await this.loadLevels();
            this.tell('Saved level. Sprite sources and collider bounds checked.');
        });
        this.find('#tile-pick').onclick = () => this.run(async () => {
            this.tell('Click a graphic in the in-app browser.');
            await this.addSprite(await this.call('pick'));
        });
        this.find('#tile-png').onchange = event => this.file(event, async file => {
            const source = {filename:file.name, png:await readBase64(file, 750_000)};
            await this.addSprite(await this.call('add', {source}));
        });
        this.find('#tile-import-level').onchange = event => this.file(event, async file => {
            if (!this.canReplace()) return;
            this.setLevel(await this.call('import', {base64:await readBase64(file, 6_500_000)}));
            await this.loadLibrary();
            await this.loadLevels();
            this.tell('Imported checked level and its sprites.');
        });
        this.find('#tile-import-nexus').onchange = event => this.file(event, async file => {
            if (!this.canReplace()) return;
            const level = await this.call('import-nexus', {base64:await readBase64(file, 3_300_000)});
            this.setLevel(level);
            await this.loadLibrary();
            await this.loadLevels();
            this.tell('Imported a playable Nexus level. Play it or edit the tiles, spawn and goal.');
        });
        for (const field of this.root.querySelectorAll('[data-stage]')) {
            field.onchange = () => {
                const [part, axis] = field.dataset.stage.split('.');
                if (!field.reportValidity() || !Number.isInteger(field.valueAsNumber)) {
                    this.refresh();
                    return;
                }
                this.layout.stage[part][axis] = field.valueAsNumber;
                this.changed();
            };
        }
        this.find('#tile-place').onclick = () => this.placeSprite();
        for (const button of this.root.querySelectorAll('[data-mode]'))
            button.onclick = () => this.setMode(button.dataset.mode);
        this.find('#tile-delete').onclick = () => {
            this.layout.objects = this.layout.objects.filter(item => item.id !== this.selected);
            this.selected = null;
            this.changed();
        };
        this.find('#tile-solid').onchange = event => {
            const item = this.selection();
            if (event.target.checked) item.collider = {x:0, y:0, w:item.size, h:item.size};
            else delete item.collider;
            this.changed();
        };
        for (const input of this.root.querySelectorAll('[data-position], [data-collider]')) {
            input.onchange = () => this.updateItem(input);
            input.onkeydown = event => {
                if (event.key === 'Enter') input.blur();
            };
        }
        this.canvas.onpointerdown = event => this.startGesture(event);
        this.canvas.onpointermove = event => this.moveGesture(event);
        this.canvas.onpointerup = this.canvas.onpointercancel = this.canvas.onlostpointercapture = () => {this.gesture = null;};
        this.canvas.onpointerleave = () => {this.hover = null; this.draw();};
        this.find('#tile-play').onclick = () => this.play();
        this.find('#tile-stop').onclick = () => this.stop();
        for (const [selector, format] of [['#tile-html','html'], ['#tile-json','json'], ['#tile-export-png','png']]) {
            this.find(selector).onclick = () => this.run(async () => {
                const id = format === 'png' ? this.active : this.levelId;
                const receipt = await this.call('export', {id, format});
                this.tell('Saved ' + receipt.path);
            });
        }
        this.refresh();
    }

    find(selector) {
        return this.root.querySelector(selector);
    }

    call(command, args = {}) {
        return this.request('media-tiles', {command,args});
    }

    tell(text) {
        this.find('#tile-status').textContent = text;
    }

    async run(action) {
        if (this.busy) return;
        this.busy = true;
        this.refresh();
        try {
            await action();
        } catch (error) {
            this.tell(error.message);
        } finally {
            this.busy = false;
            this.refresh();
        }
    }

    file(event, read) {
        const file = event.target.files[0];
        event.target.value = '';
        if (file) this.run(() => read(file));
    }

    canReplace() {
        return !this.dirty || window.confirm('Discard the unsaved changes to this level?');
    }

    show(visible) {
        if (this.visible === visible) return;
        this.visible = visible;
        this.root.hidden = !visible;
        if (!visible) {
            this.stop();
        } else if (!this.loaded) {
            this.loaded = true;
            this.run(async () => {
                await this.loadLibrary();
                await this.loadLevels();
                const level = await this.call('open');
                if (level.id) {
                    this.setLevel(level);
                    this.tell('Reopened checked level and sprites.');
                }
            });
        } else {
            this.draw();
        }
    }

    async loadLevels() {
        const {levels} = await this.call('list');
        this.saved.replaceChildren(new Option('Open saved level', ''),
            ...levels.map(level => {
                const opt = new Option(level.title + ' · ' + level.id.slice(0,6), level.id);
                opt.disabled = Boolean(level.unavailable);
                return opt;
            }));
        this.saved.value = this.levelId || '';
    }

    async loadLibrary() {
        const {assets} = await this.call('library');
        this.library = assets;
        this.find('#tile-assets').replaceChildren(...assets.map(asset => {
            const card = document.createElement('button');
            card.type = 'button';
            card.dataset.asset = asset.id;
            card.title = asset.label;
            const thumb = this.sprites[asset.id];
            if (thumb) {
                const image = document.createElement('img');
                image.alt = '';
                image.src = 'data:image/png;base64,' + thumb.png;
                card.append(image);
            }
            card.append(asset.label);
            card.disabled = Boolean(asset.unavailable);
            card.onclick = () => this.run(async () => {
                if (!this.sprites[asset.id]) this.addImage(await this.call('sprite', {id:asset.id}));
                this.active = asset.id;
                this.showThumbnail(card, this.sprites[asset.id]);
                this.tell(asset.label + ' selected. Add a sprite or paint terrain.');
            });
            return card;
        }));
        this.refresh();
    }

    showThumbnail(button, sprite) {
        if (button.querySelector('img')) return;
        const image = document.createElement('img');
        image.alt = '';
        image.src = 'data:image/png;base64,' + sprite.png;
        button.prepend(image);
    }

    addImage(sprite) {
        this.sprites[sprite.id] = sprite;
        tileImages({[sprite.id]:sprite}, () => this.draw(), this.images);
    }

    async addSprite(sprite) {
        this.addImage(sprite);
        this.active = sprite.id;
        await this.loadLibrary();
        this.tell('Added ' + sprite.label + ' to the reusable sprite library.');
    }

    setLevel(level, selected = null) {
        this.stop();
        this.levelId = level.id;
        this.layout = level.layout;
        this.title.value = this.layout.title;
        this.selected = selected;
        this.dirty = false;
        for (const sprite of Object.values(level.sprites || {})) {
            this.addImage(sprite);
            const card = this.find('[data-asset="' + sprite.id + '"]');
            if (card) this.showThumbnail(card, sprite);
        }
        this.saved.value = level.id || '';
        this.refresh();
        this.draw();
    }

    selection() {
        return this.layout.objects.find(item => item.id === this.selected);
    }

    setMode(mode) {
        if (!modes.includes(mode)) return;
        this.mode = mode;
        this.hover = null;
        this.refresh();
        this.draw();
    }

    item(kind, layer, x, y, size) {
        const item = {id:crypto.randomUUID(), assetId:this.active, kind, layer, x, y, size};
        if (layer === 'terrain') item.collider = {x:0, y:0, w:size, h:size};
        return item;
    }

    placeSprite() {
        const layer = this.find('#tile-layer').value;
        if (!this.active || !spriteLayers.includes(layer)) return;
        const size = layer === 'backdrop' ? 128 : layer === 'pickup' ? 32 : 48;
        const x = 96 + (this.layout.objects.length * 29) % 360;
        const y = layer === 'backdrop' ? 135 : floor - size;
        const item = this.item('sprite', layer, x, y, size);
        if (!this.canPlace(item)) return;
        this.layout.objects.push(item);
        this.selected = item.id;
        this.setMode('select');
        this.changed();
    }

    canPlace(item) {
        if (this.layout.objects.length >= 160) {
            this.tell('A level can contain up to 160 items.');
            return false;
        }
        if (layoutAssets(this.layout).add(item.assetId).size > 6) {
            this.tell('A level can use up to six different sprites.');
            return false;
        }
        return true;
    }

    position(event) {
        const rect = this.canvas.getBoundingClientRect();
        return {x:clamp(Math.floor((event.clientX - rect.left - this.canvas.clientLeft) * width / this.canvas.clientWidth), 0, width-1),
            y:clamp(Math.floor((event.clientY - rect.top - this.canvas.clientTop) * height / this.canvas.clientHeight), 0, height-1)};
    }

    cell(point) {
        return {x:Math.floor(point.x / tile)*tile, y:Math.floor(point.y / tile)*tile};
    }

    startGesture(event) {
        if (this.game || this.busy || event.button !== 0 || this.gesture) return;
        const point = this.position(event);
        this.canvas.setPointerCapture(event.pointerId);
        if (this.mode !== 'select') {
            this.gesture = {pointer:event.pointerId, cell:this.cell(point)};
            this.stroke(this.gesture.cell);
            return;
        }
        const order = {backdrop:0, terrain:1, pickup:2, foreground:3};
        const item = [...this.layout.objects].reverse().sort((a,b) => order[b.layer] - order[a.layer])
            .find(item => point.x >= item.x && point.x < item.x + item.size
                && point.y >= item.y && point.y < item.y + item.size);
        this.selected = item?.id;
        if (item?.kind === 'sprite') this.gesture = {pointer:event.pointerId, item,
            x:point.x-item.x, y:point.y-item.y};
        this.refresh();
        this.draw();
    }

    moveGesture(event) {
        if (this.game || this.busy || (this.gesture && this.gesture.pointer !== event.pointerId)) return;
        const point = this.position(event);
        const drag = this.gesture;
        if (drag?.item) {
            const item = drag.item;
            const x = clamp(point.x - drag.x, 0, width - item.size);
            const y = clamp(point.y - drag.y, 0, floor - item.size);
            if (item.x !== x || item.y !== y) {
                item.x = x;
                item.y = y;
                this.changed();
            }
        } else if (this.mode !== 'select') {
            this.hover = this.cell(point);
            if (drag && (drag.cell.x !== this.hover.x || drag.cell.y !== this.hover.y)) {
                const start = drag.cell, end = this.hover;
                const span = Math.max(Math.abs(end.x - start.x), Math.abs(end.y - start.y)) / tile;
                for (let i = 1; i <= span; i++) this.stroke({
                    x:start.x + Math.round((end.x - start.x) * i / span / tile) * tile,
                    y:start.y + Math.round((end.y - start.y) * i / span / tile) * tile
                });
                drag.cell = end;
            }
            this.draw();
        }
    }

    stroke({x,y}) {
        if (y + tile > floor) return;
        const item = this.layout.objects.find(it => it.kind === 'tile' && it.x === x && it.y === y);
        if (this.mode === 'erase') {
            if (!item) return;
            this.layout.objects.splice(this.layout.objects.indexOf(item), 1);
            if (this.selected === item.id) this.selected = null;
        } else {
            if (!this.active || item?.assetId === this.active) return;
            if (item) {
                const peers = this.layout.objects.filter(peer => peer !== item);
                if (new Set(peers.map(peer => peer.assetId).concat(this.active)).size > 6) {
                    this.tell('A level can use up to six different sprites.');
                    return;
                }
                item.assetId = this.active;
            } else {
                const placed = this.item('tile', 'terrain', x, y, tile);
                if (!this.canPlace(placed)) return;
                this.layout.objects.push(placed);
            }
        }
        this.changed();
    }

    updateItem(input) {
        const item = this.selection();
        if (!item || !input.reportValidity()) return;
        const value = input.valueAsNumber;
        if (input.dataset.collider) {
            item.collider[input.dataset.collider] = value;
        } else {
            item[input.dataset.position] = value;
            item.x = Math.min(item.x, width - item.size);
            item.y = Math.min(item.y, floor - item.size);
            if (item.collider) {
                item.collider.x = Math.min(item.collider.x, item.size - 1);
                item.collider.y = Math.min(item.collider.y, item.size - 1);
                item.collider.w = Math.min(item.collider.w, item.size - item.collider.x);
                item.collider.h = Math.min(item.collider.h, item.size - item.collider.y);
            }
        }
        this.changed();
    }

    changed() {
        this.dirty = true;
        this.refresh();
        this.draw();
    }

    refresh() {
        const busy = Boolean(this.busy || this.game);
        for (const selector of ['#tile-new','#tile-saved','#tile-import-level','#tile-import-nexus','#tile-png','#tile-pick','#tile-model-3d','#tile-title','#tile-layer'])
            this.find(selector).disabled = busy;
        this.find('#tile-modes').hidden = Boolean(this.game);
        this.find('#tile-place').disabled = busy || !this.active;
        this.find('#tile-export-png').disabled = busy || !this.active;
        const canPlay = this.levelId && !this.dirty && !busy;
        this.find('#tile-play').disabled = !canPlay;
        this.find('#tile-html').disabled = !canPlay;
        this.find('#tile-json').disabled = !canPlay;
        this.find('#tile-save').disabled = busy || !this.dirty || !this.title.value.trim()
            || (!this.layout.stage && !this.layout.objects.some(item => item.layer === 'pickup'));
        for (const card of this.root.querySelectorAll('[data-asset]')) {
            card.disabled = busy || Boolean(this.library?.find(asset => asset.id === card.dataset.asset)?.unavailable);
            card.setAttribute('aria-pressed', String(this.active === card.dataset.asset));
        }
        for (const tool of this.root.querySelectorAll('[data-mode]')) {
            tool.disabled = busy || (tool.dataset.mode === 'paint' && !this.active);
            tool.setAttribute('aria-pressed', String(this.mode === tool.dataset.mode));
        }
        this.canvas.dataset.mode = this.game ? 'play' : this.mode;
        this.find('#tile-stop').hidden = !this.game;
        const stage = this.layout.stage;
        const stagePanel = this.find('#tile-stage');
        stagePanel.hidden = !stage || Boolean(this.game);
        if (stage) {
            this.find('#tile-stage-hero').textContent = stage.hero
                ? stage.hero.name + ' · ' + stage.hero.animation + ' animation' : 'Default player';
            const style = this.find('#tile-stage-style');
            style.textContent = stage.style ? 'Style · ' + stage.style : '';
            style.hidden = !stage.style;
            for (const field of stagePanel.querySelectorAll('[data-stage]')) {
                const [part, axis] = field.dataset.stage.split('.');
                field.disabled = busy;
                field.value = stage[part][axis];
            }
        }
        this.refreshInspector(busy);
    }

    refreshInspector(busy) {
        const item = this.selection();
        const inspector = this.find('#tile-inspector');
        inspector.hidden = !item || Boolean(this.game);
        inspector.disabled = busy;
        if (!item) return;
        this.find('#tile-selected').textContent = (this.sprites[item.assetId]?.label || 'Sprite') + ' · ' + (item.effect || item.layer);
        this.find('#tile-solid-label').hidden = item.layer !== 'terrain';
        this.find('#tile-solid').checked = Boolean(item.collider);
        this.find('#tile-solid').disabled = Boolean(item.effect);
        this.find('#tile-collider').hidden = !item.collider;
        for (const input of inspector.querySelectorAll('[data-position]')) {
            const key = input.dataset.position;
            input.disabled = item.kind === 'tile';
            input.max = {x:width-item.size, y:floor-item.size, size:192}[key];
            input.value = item[key];
        }
        if (!item.collider) return;
        const c = item.collider;
        for (const input of inspector.querySelectorAll('[data-collider]')) {
            const key = input.dataset.collider;
            input.required = true;
            input.max = {x:item.size-c.w, y:item.size-c.h, w:item.size-c.x, h:item.size-c.y}[key];
            input.value = c[key];
        }
    }

    draw() {
        if (!this.visible || this.game) return;
        drawTileLevel(this.canvas, this.layout, this.images, undefined, this);
    }

    play() {
        const sprites = Object.fromEntries([...layoutAssets(this.layout)].map(id => [id,this.sprites[id]]));
        this.canvas.tabIndex = 0;
        this.game = mountTileGame(this.canvas, this.find('#tile-progress'), {layout:this.layout, sprites});
        this.refresh();
        this.canvas.focus();
    }

    stop() {
        if (!this.game) return;
        this.game.close();
        this.game = null;
        this.canvas.tabIndex = -1;
        this.find('#tile-progress').textContent = '';
        this.refresh();
        this.draw();
    }
}
