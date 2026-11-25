
export const tileWorld = Object.freeze({width:720, height:336, floor:312, tile:24, exit:672});
const PLAYER_W = 24, PLAYER_H = 32, STEP = 1 / 120;
const overlaps = (a, sizeA, b, sizeB) => a < b + sizeB && a + sizeA > b;
const layers = ['backdrop', 'terrain', 'pickup', 'foreground'];

const spawn = layout => ({...(layout.stage?.spawn || {x:48, y:tileWorld.floor - PLAYER_H}),
    vy:0, support:layout.stage?'':'floor', wall:'', collected:new Set(), won:false,
    direction:0, facing:1, time:0, resets:0, bounces:0, spring:''});

function drawStageTile(g, item, colors) {
    const {x,y,size,effect}=item;
    g.fillStyle={ground:colors.ground,brick:colors.rock,coin:colors.accent,spring:colors.water,hazard:'#f6756c'}[effect];
    if (effect==='coin') {
        g.beginPath();
        g.arc(x+size/2,y+size/2,size/4,0,Math.PI*2);
        g.fill();
    } else if (effect==='hazard') {
        g.beginPath();
        g.moveTo(x,y+size);
        g.lineTo(x+size/2,y+4);
        g.lineTo(x+size,y+size);
        g.fill();
    } else if (effect==='spring') {
        g.fillRect(x+2,y+size-14,size-4,4);
        g.fillRect(x+5,y+size-8,size-10,6);
    } else {
        g.fillRect(x,y,size,size);
        g.fillStyle=colors.accent+'38';
        g.fillRect(x,y,size,3);
        g.strokeStyle=colors.sky+'99';
        g.strokeRect(x+.5,y+.5,size-1,size-1);
    }
}

function drawHero(g, image, hero, state) {
    const moving=state.direction!==0;
    const stride=hero.animation==='stride'&&moving;
    const lift=hero.animation==='hover' || (hero.animation==='bob'&&moving);
    const squash=hero.animation==='squash' && Math.abs(state.vy)>25;
    const wobble=Math.sin(state.time*(stride?18:8));
    const height=PLAYER_H*hero.scale*(squash?1.14:1);
    const width=height*image.naturalWidth/image.naturalHeight*(squash ? .83 : 1);
    g.save();
    g.imageSmoothingEnabled=false;
    g.translate(state.x+PLAYER_W/2,state.y+PLAYER_H-(stride?Math.abs(wobble)*3:lift?3+wobble*2:0));
    if (stride) g.rotate(wobble*.09);
    g.scale(state.facing,1);
    g.drawImage(image,-width/2,-height,width,height);
    g.restore();
}

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

export function drawTileLevel(canvas, layout, images, state = spawn(layout), editor = null) {
    const {width, height, floor, tile, exit} = tileWorld;
    const g = canvas.getContext('2d');
    const rect = (fill, x, y, w, h) => {
        g.fillStyle = fill;
        g.fillRect(x, y, w, h);
    };
    const stage=layout.stage;
    const colors=stage?.palette;
    const sky = g.createLinearGradient(0, 0, 0, floor);
    sky.addColorStop(0, colors?.sky||'#142337');
    sky.addColorStop(1, colors?.water||'#36565b');
    rect(sky, 0, 0, width, height);
    if (stage) {
        const backdrop=images.get(stage.backdropAsset);
        if (backdrop?.complete&&backdrop.naturalWidth) {
            g.save();
            g.globalAlpha=stage.backdropOpacity;
            const drift=(state.x-stage.spawn.x)*stage.parallax*.05;
            g.drawImage(backdrop,-48-drift,0,width+96,height);
            g.restore();
        }
    } else {
        rect('#829a91', 0, floor, width, 3);
        rect('#213b40', 0, floor + 3, width, height - floor);
    }
    if (editor) {
        g.beginPath();
        g.strokeStyle = '#bed0d02e';
        for (let x = tile; x < width; x += tile) { g.moveTo(x, 0); g.lineTo(x, floor); }
        for (let y = tile; y < floor; y += tile) { g.moveTo(0, y); g.lineTo(width, y); }
        g.stroke();
    }
    const ready = stage || layout.objects.every(item => item.layer !== 'pickup' || state.collected.has(item.id));
    const gate = ready ? colors?.accent||'#8de9bd' : '#d88376';
    const goal=stage?.goal||{x:exit,y:floor-48};
    rect(gate, goal.x, goal.y, 5, stage?tile:48);
    rect(gate, goal.x, goal.y, stage?tile:28, 5);
    if (stage) rect(gate,goal.x+tile-5,goal.y,5,tile);
    for (const layer of layers) {
        if (layer === 'foreground') {
            const hero=stage?.hero;
            const image=hero&&images.get(hero.assetId);
            if (image?.complete&&image.naturalWidth) drawHero(g,image,hero,state);
            else {
                rect('#ffe3a5', state.x, state.y, PLAYER_W, PLAYER_H);
                rect('#284044', state.x + 4, state.y + 5, PLAYER_W - 8, 11);
            }
        }
        for (const item of layout.objects) {
            if (item.layer !== layer || state.collected.has(item.id)) continue;
            if (item.effect&&colors) {drawStageTile(g,item,colors);continue;}
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
    const {layout}=level;
    const stage=layout.stage;
    const speed=stage?.hero?.speed??215;
    const lift=stage?.hero?.jump??610;
    const pickups = layout.objects.filter(item => item.layer === 'pickup');
    const hazards=layout.objects.filter(item=>item.effect==='hazard');
    const springs=layout.objects.filter(item=>item.effect==='spring');
    const touches=item=>overlaps(state.x,PLAYER_W,item.x,item.size)&&overlaps(state.y,PLAYER_H,item.y,item.size);
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
        if (stage) Object.assign(canvas.dataset,{bounces:String(state.bounces),resets:String(state.resets),
            hero:stage.hero?.name||'',phase:state.won?'complete':'playing'});
        else for (const key of ['bounces','resets','hero','phase']) delete canvas.dataset[key];
    }

    function restart() {
        state = spawn(layout);
        keys.clear();
        jump = false;
        previous = 0;
        accumulator = 0;
        status.textContent = stage
            ? 'Move with ← and →. Jump with ↑ or Space. Reach the gate.'
            : 'Move with ← and →. Jump with ↑ or Space. Collect all pickups.';
        draw();
    }

    function step() {
        if (state.won) return;
        if (jump && state.support) state.vy = -lift;
        jump = false;
        const direction = Number(keys.has('ArrowRight')) - Number(keys.has('ArrowLeft'));
        state.direction=direction;
        if (direction) state.facing=direction;
        state.time+=STEP;
        let nextX = Math.max(0, Math.min(width-PLAYER_W, state.x + direction*speed*STEP));
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
        if (!stage && nextY + PLAYER_H >= floor) {
            nextY = floor - PLAYER_H;
            state.support = 'floor';
        }
        if (nextY !== state.y + state.vy * STEP || state.support) state.vy = 0;
        state.y = nextY;
        if (stage) {
            const hazard=hazards.some(touches);
            if (hazard || state.y > height+PLAYER_H) {
                const {resets,bounces}=state;
                state=spawn(layout);
                state.resets=resets+1;
                state.bounces=bounces;
                keys.clear();
                status.textContent=hazard?'Hit a hazard. Returned to spawn.':'Fell below the level. Returned to spawn.';
                return;
            }
            const spring=springs.find(touches);
            if (spring && state.spring!==spring.id) {
                state.vy=-lift*1.42;
                state.support='';
                state.bounces++;
                status.textContent='Spring boost.';
            }
            state.spring=spring?.id||'';
        }
        for (const item of pickups) {
            if (!state.collected.has(item.id) && overlaps(state.x, PLAYER_W, item.x, item.size)
                && overlaps(state.y, PLAYER_H, item.y, item.size)) {
                state.collected.add(item.id);
                status.textContent = stage ? state.collected.size+' picked up.' : state.collected.size === pickups.length
                    ? 'All sprites collected. The exit is open.'
                    : state.collected.size + ' of ' + pickups.length + ' sprites collected.';
            }
        }
        if (stage) {
            if (touches({...stage.goal,size:tileWorld.tile})) {
                state.won=true;
                status.textContent='Level complete.';
            }
            return;
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
