function mountSpatialLevel(canvas, progress, {layout}) {
    const {floor, route, blockers, radius, prop} = layout;
    const [startX, startZ, goalX, goalZ] = route;
    const w = floor[3] - floor[0], d = floor[5] - floor[2];
    canvas.width = 760;
    canvas.height = 480;
    canvas.setAttribute('aria-label', 'Top-down playtest of the reviewed Spatial level');
    document.querySelector('#controls').textContent = 'Focus the level and use the arrow keys or WASD to reach the green exit.';
    const scale = Math.min(696 / w, 416 / d);
    const px = x => (760 - w * scale) / 2 + (x - floor[0]) * scale;
    const py = z => (480 - d * scale) / 2 + (z - floor[2]) * scale;
    const context = canvas.getContext('2d');
    const keys = new Set();
    const directions = new Map([
        ['ArrowLeft',[-1,0]], ['a',[-1,0]], ['ArrowRight',[1,0]], ['d',[1,0]],
        ['ArrowUp',[0,-1]], ['w',[0,-1]], ['ArrowDown',[0,1]], ['s',[0,1]]
    ]);
    let x, z, won, bumped, previous;

    function legal(a, b) {
        return a - radius >= floor[0] && a + radius <= floor[3]
            && b - radius >= floor[2] && b + radius <= floor[5]
            && blockers.every(({box}) => a + radius <= box[0] || a - radius >= box[3]
                || b + radius <= box[2] || b - radius >= box[5]);
    }

    function report() {
        if (won) progress.textContent = 'Exit reached. Aisle playtest passed.';
        else if (bumped) progress.textContent = 'Collider blocked your movement. Go around it.';
        else progress.textContent = 'Reach the green exit. Amber marks the added prop.';
        progress.dataset.outcome = won ? 'complete' : bumped ? 'blocked' : 'walking';
    }

    function restart() {
        x = startX; z = startZ; won = false; bumped = false;
        keys.clear();
        report();
        paint();
    }

    function paint() {
        context.fillStyle = '#112329';
        context.fillRect(0, 0, 760, 480);
        context.fillStyle = '#29434b';
        context.fillRect(px(floor[0]), py(floor[2]), w * scale, d * scale);
        context.strokeStyle = '#74bdac';
        context.setLineDash([7, 8]);
        context.beginPath();
        context.moveTo(px(startX), py(startZ));
        context.lineTo(px(goalX), py(goalZ));
        context.stroke();
        context.setLineDash([]);
        for (const {id, box} of blockers) {
            context.fillStyle = id === prop ? '#f0b176' : '#8495aa';
            context.fillRect(px(box[0]), py(box[2]), (box[3] - box[0]) * scale, (box[5] - box[2]) * scale);
        }
        context.fillStyle = '#81df9e';
        context.fillRect(px(goalX) - radius * scale, py(goalZ) - radius * scale, 2 * radius * scale, 2 * radius * scale);
        context.fillStyle = '#eef9f5';
        context.fillRect(px(x) - radius * scale, py(z) - radius * scale, 2 * radius * scale, 2 * radius * scale);
    }

    function step(now) {
        const dt = Math.min(.025, (now - (previous ?? now)) / 1000);
        previous = now;
        if (!won && keys.size) {
            let dx = 0, dz = 0;
            for (const key of keys) {
                const [a,b] = directions.get(key);
                dx += a;
                dz += b;
            }
            const speed = 1.8 * dt / Math.max(1, Math.hypot(dx, dz));
            const targetX = x + dx * speed, targetZ = z + dz * speed;
            bumped = !legal(targetX, targetZ);
            if (!bumped) {
                x = targetX;
                z = targetZ;
            }
            won = Math.hypot(x - goalX, z - goalZ) <= radius;
            report();
            paint();
        }
        requestAnimationFrame(step);
    }

    canvas.addEventListener('keydown', event => {
        const key = event.key.length === 1 ? event.key.toLowerCase() : event.key;
        if (!directions.has(key)) return;
        event.preventDefault();
        keys.add(key);
    });
    canvas.addEventListener('keyup', event => keys.delete(event.key.length === 1 ? event.key.toLowerCase() : event.key));
    canvas.addEventListener('blur', () => keys.clear());
    restart();
    requestAnimationFrame(step);
    return {restart};
}
