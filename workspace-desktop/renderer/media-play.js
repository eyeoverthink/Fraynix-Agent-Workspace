
const WIDTH = 720, HEIGHT = 320, FLOOR = 284, EXIT = 672;
const PLAYER_W = 24, PLAYER_H = 32, PLATFORM_W = 140, PLATFORM_H = 16;
const STEP = 1 / 120;
const overlaps = (a, sizeA, b, sizeB) => a < b + sizeB && a + sizeA > b;

export function mountGame(canvas, status, level, playable = true) {
    const {backdropX, platformX, platformY, pickupX, foregroundX} = level.layout;
    const platform = {x: platformX, y: platformY};
    const pickup = {x: pickupX - 15, y: platformY - 32};
    const context = canvas.getContext('2d');
    canvas.width = WIDTH;
    canvas.height = HEIGHT;
    const sprite = new Image();
    sprite.src = 'data:image/png;base64,' + level.sprite.png;
    const keys = new Set();
    const events = new AbortController();
    const options = {signal: events.signal};
    let state, jump, frame = 0, running = true;
    let last = 0, accumulator = 0;

    function tell(text) {
        status.textContent = text;
    }

    function restart() {
        state = {x: 48, y: FLOOR - PLAYER_H, vy: 0, support: 'floor', collected: false, won: false};
        keys.clear();
        jump = false;
        tell(playable ? 'Find the sprite, then reach the exit.' : 'Save the draft to play it.');
        draw();
    }

    function step() {
        if (state.won) return;
        if (jump && state.support) {
            state.vy = -610;
            state.support = '';
        }
        jump = false;
        const direction = Number(keys.has('ArrowRight')) - Number(keys.has('ArrowLeft'));
        let nextX = state.x + direction * 215 * STEP;
        if (overlaps(state.y, PLAYER_H, platform.y, PLATFORM_H)) {
            if (direction > 0 && state.x + PLAYER_W <= platform.x)
                nextX = Math.min(nextX, platform.x - PLAYER_W);
            if (direction < 0 && state.x >= platform.x + PLATFORM_W)
                nextX = Math.max(nextX, platform.x + PLATFORM_W);
        }
        state.x = Math.max(0, Math.min(WIDTH - PLAYER_W, nextX));
        const platformUnder = overlaps(state.x, PLAYER_W, platform.x, PLATFORM_W);
        state.vy += 1560 * STEP;
        const oldY = state.y;
        state.y += state.vy * STEP;
        state.support = '';
        if (platformUnder && state.vy < 0 && oldY >= platform.y + PLATFORM_H
            && state.y < platform.y + PLATFORM_H) {
            state.y = platform.y + PLATFORM_H;
            state.vy = 0;
        }
        if (platformUnder && state.vy >= 0 && oldY + PLAYER_H <= platform.y
            && state.y + PLAYER_H >= platform.y) {
            state.y = platform.y - PLAYER_H;
            state.vy = 0;
            state.support = 'platform';
        }
        if (state.y + PLAYER_H >= FLOOR) {
            state.y = FLOOR - PLAYER_H;
            state.vy = 0;
            state.support = 'floor';
        }
        if (!state.collected && overlaps(state.x, PLAYER_W, pickup.x, 30)
            && overlaps(state.y, PLAYER_H, pickup.y, 32)) {
            state.collected = true;
            tell('Sprite collected. The exit is open.');
        }
        if (state.collected && state.x + PLAYER_W >= EXIT) {
            state.won = true;
            tell('Level complete.');
        }
    }

    function imageAt(x, y, size, opacity = 1) {
        if (!sprite.complete || !sprite.naturalWidth) return;
        const scale = size / Math.max(sprite.naturalWidth, sprite.naturalHeight);
        const w = sprite.naturalWidth * scale, h = sprite.naturalHeight * scale;
        context.globalAlpha = opacity;
        context.drawImage(sprite, x - w / 2, y - h, w, h);
        context.globalAlpha = 1;
    }

    function rect(color, x, y, width, height) {
        context.fillStyle = color;
        context.fillRect(x, y, width, height);
    }

    function draw() {
        const sky = context.createLinearGradient(0, 0, 0, FLOOR);
        sky.addColorStop(0, '#16283d');
        sky.addColorStop(1, '#304e58');
        rect(sky, 0, 0, WIDTH, HEIGHT);
        imageAt(backdropX, FLOOR, 200, 0.33);
        rect('#537375', 0, FLOOR, WIDTH, 4);
        rect('#1a3035', 0, FLOOR + 4, WIDTH, HEIGHT - FLOOR);
        rect('#284044', platform.x, platform.y, PLATFORM_W, PLATFORM_H);
        rect('#82b2ac', platform.x, platform.y, PLATFORM_W, 4);
        rect(state.collected ? '#6fddb3' : '#a3746e', EXIT, FLOOR - 48, 6, 48);
        rect(state.collected ? '#6fddb3' : '#a3746e', EXIT, FLOOR - 48, 28, 5);
        if (!state.collected) imageAt(pickupX, platformY, 32);
        rect('#ffe3a5', state.x, state.y, PLAYER_W, PLAYER_H);
        rect('#284044', state.x + 4, state.y + 5, PLAYER_W - 8, 11);
        imageAt(foregroundX, HEIGHT + 5, 115, 0.74);
        canvas.dataset.playerX = state.x.toFixed(2);
        canvas.dataset.playerY = state.y.toFixed(2);
        canvas.dataset.support = state.support;
        canvas.dataset.collected = String(state.collected);
        canvas.dataset.won = String(state.won);
    }

    function advance(now) {
        if (!running) return;
        accumulator += last ? Math.min((now - last) / 1000, 0.05) : 0;
        last = now;
        let dirty = false;
        while (accumulator >= STEP) {
            step();
            accumulator -= STEP;
            dirty = true;
        }
        if (dirty) draw();
        frame = requestAnimationFrame(advance);
    }

    function press(event) {
        if (!['ArrowLeft', 'ArrowRight', 'ArrowUp', 'Space'].includes(event.code)) return;
        event.preventDefault();
        if (event.code === 'ArrowUp' || event.code === 'Space') {
            if (!event.repeat) jump = true;
        } else {
            keys.add(event.code);
        }
    }
    if (playable) {
        canvas.addEventListener('keydown', press, options);
        canvas.addEventListener('keyup', event => keys.delete(event.code), options);
        canvas.addEventListener('blur', () => { keys.clear(); jump = false; }, options);
        frame = requestAnimationFrame(advance);
    }
    sprite.addEventListener('load', draw, options);
    restart();
    return {
        restart,
        close() {
            running = false;
            cancelAnimationFrame(frame);
            events.abort();
        }
    };
}
