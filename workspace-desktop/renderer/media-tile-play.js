
export const tileWorld = Object.freeze({width:720, height:336, floor:312, tile:24, exit:672});
const PLAYER_W = 24, PLAYER_H = 32, STEP = 1 / 120;
const overlaps = (a, sizeA, b, sizeB) => a < b + sizeB && a + sizeA > b;
const layers = ['backdrop', 'terrain', 'pickup', 'foreground'];

const spawn = () => ({x:48, y:tileWorld.floor - PLAYER_H, vy:0, support:'floor', wall:'',
    collected:new Set(), won:false});

export function tileImages(sprites, invalidate, images = new Map()) {
    for (const [id, sprite] of Object.entries(sprites)) {
        if (images.has(id)) continue;
        const image = new Image();
        images.set(id, image);
        image.addEventListener('load', invalidate, {once:true});
        image.src = 'data:image/png;base64,' + sprite.png;
    }
    return images;
}

export function drawTileLevel(canvas, layout, images, state = spawn(), editor = null) {
    const {width, height, floor, tile, exit} = tileWorld;
    const g = canvas.getContext('2d');
    const rect = (fill, x, y, w, h) => {
        g.fillStyle = fill;
        g.fillRect(x, y, w, h);
    };
    const sky = g.createLinearGradient(0, 0, 0, floor);
    sky.addColorStop(0, '#142337');
    sky.addColorStop(1, '#36565b');
    rect(sky, 0, 0, width, height);
    rect('#829a91', 0, floor, width, 3);
    rect('#213b40', 0, floor + 3, width, height - floor);
    if (editor) {
        g.beginPath();
        g.strokeStyle = '#bed0d02e';
        for (let x = tile; x < width; x += tile) { g.moveTo(x, 0); g.lineTo(x, floor); }
        for (let y = tile; y < floor; y += tile) { g.moveTo(0, y); g.lineTo(width, y); }
        g.stroke();
    }
    const ready = layout.objects.every(item => item.layer !== 'pickup' || state.collected.has(item.id));
    const gate = ready ? '#8de9bd' : '#d88376';
    rect(gate, exit, floor - 48, 5, 48);
    rect(gate, exit, floor - 48, 28, 5);
    for (const layer of layers) {
        if (layer === 'foreground') {
            rect('#ffe3a5', state.x, state.y, PLAYER_W, PLAYER_H);
            rect('#284044', state.x + 4, state.y + 5, PLAYER_W - 8, 11);
        }
        for (const item of layout.objects) {
            if (item.layer !== layer || state.collected.has(item.id)) continue;
            const image = images.get(item.assetId);
            if (!image?.complete || !image.naturalWidth) continue;
            const scale = item.size / Math.max(image.naturalWidth, image.naturalHeight);
            const w = image.naturalWidth * scale, h = image.naturalHeight * scale;
            g.drawImage(image, item.x + (item.size - w) / 2, item.y + item.size - h, w, h);
        }
    }
    if (!editor) return;
    for (const item of layout.objects) {
        if (item.collider) {
            const {x, y, w, h} = item.collider;
            g.strokeStyle = '#f5a65f';
            g.strokeRect(item.x + x + .5, item.y + y + .5, w - 1, h - 1);
        }
        if (item.id === editor.selected) {
            g.strokeStyle = '#a3ffdb';
            g.lineWidth = 2;
            g.strokeRect(item.x + 1, item.y + 1, item.size - 2, item.size - 2);
            g.lineWidth = 1;
        }
    }
    if (editor.hover) {
        const {x,y} = editor.hover;
        rect(editor.mode === 'erase' ? '#e57b723b' : '#91eac447', x, y, tile, tile);
    }
}

export function mountTileGame(canvas, status, level) {
    const {width, height, floor, exit} = tileWorld;
    const pickups = level.layout.objects.filter(item => item.layer === 'pickup');
    const solids = level.layout.objects.filter(item => item.collider).map(({id,x,y,collider:c}) =>
        ({id, x:x+c.x, y:y+c.y, w:c.w, h:c.h}));
    const keys = new Set();
    const events = new AbortController();
    const options = {signal:events.signal};
    let state, jump = false, frame = 0, previous = 0, accumulator = 0;
    canvas.width = width;
    canvas.height = height;
    const images = tileImages(level.sprites, draw);

    function draw() {
        drawTileLevel(canvas, level.layout, images, state);
        Object.assign(canvas.dataset, {playerX:state.x.toFixed(2), playerY:state.y.toFixed(2),
            support:state.support, wall:state.wall, collected:String(state.collected.size),
            required:String(pickups.length), won:String(state.won)});
    }

    function restart() {
        state = spawn();
        keys.clear();
        jump = false;
        previous = 0;
        accumulator = 0;
        status.textContent = 'Move with ← and →. Jump with ↑ or Space. Collect all pickups.';
        draw();
    }

    function step() {
        if (state.won) return;
        if (jump && state.support) state.vy = -610;
        jump = false;
        const direction = Number(keys.has('ArrowRight')) - Number(keys.has('ArrowLeft'));
        let nextX = Math.max(0, Math.min(width-PLAYER_W, state.x + direction*215*STEP));
        state.wall = '';
        for (const box of solids) {
            if (!overlaps(state.y, PLAYER_H, box.y, box.h)) continue;
            if (direction > 0 && state.x + PLAYER_W <= box.x && nextX + PLAYER_W > box.x) {
                nextX = box.x - PLAYER_W;
                state.wall = box.id;
            }
            if (direction < 0 && state.x >= box.x + box.w && nextX < box.x + box.w) {
                nextX = box.x + box.w;
                state.wall = box.id;
            }
        }
        state.x = nextX;
        state.vy += 1560 * STEP;
        let nextY = state.y + state.vy * STEP;
        state.support = '';
        for (const box of solids) {
            if (!overlaps(state.x, PLAYER_W, box.x, box.w)) continue;
            if (state.vy < 0 && state.y >= box.y+box.h && nextY < box.y+box.h) {
                nextY = box.y+box.h;
            } else if (state.vy >= 0 && state.y + PLAYER_H <= box.y && nextY + PLAYER_H >= box.y) {
                nextY = box.y - PLAYER_H;
                state.support = box.id;
            }
        }
        if (nextY < 0) nextY = 0;
        if (nextY + PLAYER_H >= floor) {
            nextY = floor - PLAYER_H;
            state.support = 'floor';
        }
        if (nextY !== state.y + state.vy * STEP || state.support) state.vy = 0;
        state.y = nextY;
        for (const item of pickups) {
            if (!state.collected.has(item.id) && overlaps(state.x, PLAYER_W, item.x, item.size)
                && overlaps(state.y, PLAYER_H, item.y, item.size)) {
                state.collected.add(item.id);
                status.textContent = state.collected.size === pickups.length
                    ? 'All sprites collected. The exit is open.'
                    : state.collected.size + ' of ' + pickups.length + ' sprites collected.';
            }
        }
        if (state.x+PLAYER_W >= exit) {
            if (state.collected.size === pickups.length) {
                state.won = true;
                status.textContent = 'Level complete.';
            } else {
                state.x = Math.min(state.x, exit - PLAYER_W);
                status.textContent = 'The exit is locked. Collect every pickup.';
            }
        }
    }

    function advance(now) {
        accumulator += previous ? Math.min((now - previous) / 1000, .05) : 0;
        previous = now;
        let dirty = false;
        while (accumulator >= STEP) {
            step();
            accumulator -= STEP;
            dirty = true;
        }
        if (dirty) draw();
        if (!state.won) frame = requestAnimationFrame(advance);
        else frame = 0;
    }

    function release() {
        keys.clear();
        jump = false;
    }

    canvas.addEventListener('keydown', event => {
        if (!['ArrowLeft', 'ArrowRight', 'ArrowUp', 'Space'].includes(event.code)) return;
        event.preventDefault();
        if (event.code === 'ArrowUp' || event.code === 'Space') {
            if (!event.repeat) jump = true;
        } else keys.add(event.code);
    }, options);
    canvas.addEventListener('keyup', event => keys.delete(event.code), options);
    canvas.addEventListener('blur', release, options);
    function start() {
        restart();
        cancelAnimationFrame(frame);
        frame = requestAnimationFrame(advance);
    }
    start();
    return {
        restart:start,
        close() {
            cancelAnimationFrame(frame);
            events.abort();
        }
    };
}
