
export const tileWorld = Object.freeze({width:720, height:336, floor:312, tile:24, exit:672});
const PLAYER_W = 24, PLAYER_H = 32, STEP = 1 / 120;
const overlaps = (a, sizeA, b, sizeB) => a < b + sizeB && a + sizeA > b;
const defaultDepth = {backdrop:-300,terrain:-200,pickup:-100,foreground:100};
const maxPlayTicks = 7200;
export const tileFields = ['tick','x','y','vx','vy','support','wall','collectedHex','won','direction',
    'facing','animationTimeS','resets','bounces','spring','score','jumpPressed','events','inputDirection'];
export const tileScoreDefaults = {pickup:100,goal:500,resetPenalty:25};

export function tileDrawingOrder(layout) {
    const planes = new Map((layout.planes || []).map(plane=>[plane.id,plane]));
    return layout.objects.map(item=>{
        const plane=planes.get(item.plane);
        return {item,opacity:plane?.visible===false?0:plane?.opacity??1,
            depth:(plane?.z??defaultDepth[item.layer])*1000+(item.z??0)};
    }).sort((a,b)=>a.depth-b.depth);
}

function pickupMask(pickups, collected) {
    let mask='';
    for (let i=0;i<pickups.length;i+=4) {
        let bits=0;
        for (let b=0;b<4 && i+b<pickups.length;b++) if (collected.has(pickups[i+b].id)) bits|=(1<<b);
        mask+=bits.toString(16);
    }
    return mask;
}

function restoreTick(layout, row) {
    const pickups=layout.objects.filter(item=>item.layer==='pickup');
    const collected=new Set(pickups.filter((_,i)=>Number.parseInt(row[7][i>>2],16)&(1<<(i%4))).map(item=>item.id));
    return {x:row[1],y:row[2],vx:row[3],vy:row[4],support:row[5],wall:row[6],collected,won:row[8],
        direction:row[9],facing:row[10],time:row[11],resets:row[12],bounces:row[13],spring:row[14],score:row[15]};
}

export async function renderTileEvidence(level,row,region,assetId) {
    const canvas=document.createElement('canvas');
    const images=tileImages(level.sprites,()=>{});
    await Promise.all([...images.values()].map(image=>image.decode()));
    canvas.width=tileWorld.width;
    canvas.height=tileWorld.height;
    if (assetId) {
        const image=images.get(assetId);
        if (!image) throw Error('Choose an asset from this level.');
        canvas.width=image.naturalWidth;
        canvas.height=image.naturalHeight;
        if (!region) return {width:canvas.width,height:canvas.height,png:level.sprites[assetId].png};
        canvas.getContext('2d',{willReadFrequently:true}).drawImage(image,0,0);
    } else drawTileLevel(canvas, level.layout, images, restoreTick(level.layout,row));
    if (!region) return {width:canvas.width,height:canvas.height,png:canvas.toDataURL('image/png').split(',')[1]};
    const {x,y,width,height}=region;
    if (x+width>canvas.width || y+height>canvas.height) throw Error('The sample extends outside the image.');
    return {x,y,width,height,rgba:Array.from(canvas.getContext('2d',{willReadFrequently:true}).getImageData(x,y,width,height).data)};
}

const spawn = layout => ({...(layout.stage?.spawn || {x:48, y:tileWorld.floor - PLAYER_H}),
    vy:0, support:layout.stage?'':'floor', wall:'', collected:new Set(), won:false,
    direction:0, facing:1, time:0, resets:0, bounces:0, spring:'', score:0, vx:0});

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

function playerFrame(state) {
    return state.support && Math.abs(state.vx)>.01 ? Math.floor(Math.round(state.time*120)/12)%4 : -1;
}

function drawAnimatedPlayer(g, images, player, state, scale) {
    const idle=images.get(player.idleAssetId), walk=images.get(player.walkAssetId);
    if (!idle?.complete || !idle.naturalWidth) return false;
    const frame=walk?.complete&&walk.naturalWidth ? playerFrame(state) : -1;
    const h=PLAYER_H*scale, w=h*idle.naturalWidth/idle.naturalHeight;
    g.save();
    g.imageSmoothingEnabled=false;
    g.translate(state.x+PLAYER_W/2,state.y+PLAYER_H);
    g.scale(state.facing,1);
    if (frame<0) g.drawImage(idle,-w/2,-h,w,h);
    else {
        const cell=walk.naturalWidth/4;
        g.drawImage(walk,frame*cell,0,cell,walk.naturalHeight,-w/2,-h,w,h);
    }
    g.restore();
    return true;
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
    const g = canvas.getContext('2d',{willReadFrequently:true});
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
    const actor=()=>{
        const hero=stage?.hero;
        if (layout.player && drawAnimatedPlayer(g,images,layout.player,state,hero?.scale??1)) return;
        const image=hero&&images.get(hero.assetId);
        if (image?.complete&&image.naturalWidth) drawHero(g,image,hero,state);
        else {
            rect('#ffe3a5', state.x, state.y, PLAYER_W, PLAYER_H);
            rect('#284044', state.x + 4, state.y + 5, PLAYER_W - 8, 11);
        }
    };
    let drawnActor=false;
    for (const {item,opacity,depth} of tileDrawingOrder(layout)) {
        if (!drawnActor && depth>=0) {
            actor();
            drawnActor=true;
        }
        if (!opacity || state.collected.has(item.id)) continue;
        g.globalAlpha=opacity;
        if (item.effect&&colors) drawStageTile(g,item,colors);
        else {
            const image = images.get(item.assetId);
            if (image?.complete && image.naturalWidth) {
                const scale = item.size / Math.max(image.naturalWidth, image.naturalHeight);
                const w = image.naturalWidth * scale, h = image.naturalHeight * scale;
                g.drawImage(image, item.x + (item.size - w) / 2, item.y + item.size - h, w, h);
            }
        }
        g.globalAlpha=1;
    }
    if (!drawnActor) actor();
    if (!editor) return;
    for (const item of layout.objects) {
        if (item.collider) {
            const {x, y, w, h} = item.collider;
            g.strokeStyle = '#f5a65f';
            g.strokeRect(item.x + x + .5, item.y + y + .5, w - 1, h - 1);
        }
        if (item.id === editor.selected || editor.layerTools?.selected.has(item.id)) {
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

export function mountTileGame(canvas, status, level, onRun=null) {
    const {width,height}=tileWorld;
    const {layout}=level;
    const keys = new Set();
    const events = new AbortController();
    const options = {signal:events.signal};
    let simulation, jump=false, frame=0, previous=0, accumulator=0;
    let rows=[], sealed=false;
    function seal(reason) {
        if (!onRun || sealed) return;
        sealed=true;
        onRun({levelId:level.id,reason,rows});
    }
    canvas.width = width;
    canvas.height = height;
    const images = tileImages(level.sprites, draw);

    function draw() {
        const {state,tick}=simulation;
        drawTileLevel(canvas, layout, images, state);
        Object.assign(canvas.dataset, {playerX:state.x.toFixed(2), playerY:state.y.toFixed(2),
            support:state.support, wall:state.wall, collected:String(state.collected.size),
            required:String(simulation.pickupCount), won:String(state.won), tick:String(tick), score:String(state.score)});
        status.dataset.score=String(state.score);
        if (layout.stage) Object.assign(canvas.dataset,{bounces:String(state.bounces),resets:String(state.resets),
            hero:layout.stage.hero?.name||'',phase:state.won?'complete':'playing'});
        else for (const key of ['bounces','resets','hero','phase']) delete canvas.dataset[key];
        if (layout.player) {
            canvas.dataset.hero=layout.player.name;
            const frame=playerFrame(state);
            canvas.dataset.playerFrame=frame<0?'idle':String(frame);
        } else delete canvas.dataset.playerFrame;
    }

    function restart() {
        simulation=createTileSimulation(level);
        sealed=false;
        rows=onRun?[simulation.first()]:[];
        keys.clear();
        jump=false;
        previous=0;
        accumulator=0;
        status.textContent=simulation.status;
        draw();
    }

    function advance(now) {
        accumulator += previous ? Math.min((now - previous) / 1000, .05) : 0;
        previous = now;
        let dirty = false;
        while (accumulator>=STEP && !simulation.state.won && (!onRun || simulation.tick<maxPlayTicks)) {
            const direction=Number(keys.has('ArrowRight'))-Number(keys.has('ArrowLeft'));
            const resets=simulation.state.resets;
            const row=simulation.step(direction,jump,Boolean(onRun));
            if (resets!==simulation.state.resets) keys.clear();
            jump=false;
            if (onRun) rows.push(row);
            accumulator-=STEP;
            dirty=true;
        }
        if (dirty && status.textContent!==simulation.status) status.textContent=simulation.status;
        if (dirty) draw();
        if (simulation.state.won) seal('won');
        else if (onRun && simulation.tick===maxPlayTicks) {
            status.textContent='The 60-second playtest is complete.';
            seal('limit');
        } else {
            frame = requestAnimationFrame(advance);
            return;
        }
        frame=0;
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
        evidence(start,limit) {return {levelId:level.id,hz:120,fields:tileFields,lastTick:simulation.tick,score:simulation.state.score,sealed,
            rows:rows.slice(start,start+limit),start,next:start+limit<rows.length?start+limit:null};},
        close() {
            seal(simulation.state.won?'won':'stopped');
            cancelAnimationFrame(frame);
            events.abort();
        }
    };
}

function createTileSimulation(level) {
    const {width, height, floor, exit} = tileWorld;
    const {layout}=level;
    const stage=layout.stage;
    const speed=stage?.hero?.speed??215;
    const lift=stage?.hero?.jump??610;
    const pickups = layout.objects.filter(item => item.layer === 'pickup');
    const rules = layout.scoring || tileScoreDefaults;
    const hazards=layout.objects.filter(item=>item.effect==='hazard');
    const springs=layout.objects.filter(item=>item.effect==='spring');
    const touches=item=>overlaps(state.x,PLAYER_W,item.x,item.size)&&overlaps(state.y,PLAYER_H,item.y,item.size);
    const solids = level.layout.objects.filter(item => item.collider).map(({id,x,y,collider:c}) =>
        ({id, x:x+c.x, y:y+c.y, w:c.w, h:c.h}));
    let state=spawn(layout), status=stage
        ? 'Move with ← and →. Jump with ↑ or Space. Reach the gate.'
        : 'Move with ← and →. Jump with ↑ or Space. Collect all pickups.';
    let tick=0, notes=[];
    const earned=new Set();
    const award=(type,id,delta=0)=>{
        const changed=Math.max(0,state.score+delta)-state.score;
        state.score+=changed;
        notes.push([type,id,changed]);
    };
    const sample=(jumpPressed,inputDirection)=>[tick,state.x,state.y,state.vx,state.vy,state.support,state.wall,
        pickupMask(pickups,state.collected),state.won,state.direction,state.facing,state.time,
        state.resets,state.bounces,state.spring,state.score,jumpPressed,notes,inputDirection];
    function update(direction, jump) {
        if (state.won) return;
        if (jump && state.support) {
            state.vy = -lift;
            award('jump','');
        }
        const lastX=state.x;
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
        state.vx=(state.x-lastX)/STEP;
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
            const hazard=hazards.find(touches);
            if (hazard || state.y > height+PLAYER_H) {
                const {resets,bounces,score}=state;
                state=spawn(layout);
                state.resets=resets+1;
                state.bounces=bounces;
                state.score=score;
                award('reset',hazard?.id||'',-rules.resetPenalty);
                status=hazard?'Hit a hazard. Returned to spawn.':'Fell below the level. Returned to spawn.';
                return;
            }
            const spring=springs.find(touches);
            if (spring && state.spring!==spring.id) {
                state.vy=-lift*1.42;
                state.support='';
                state.bounces++;
                award('spring',spring.id);
                status='Spring boost.';
            }
            state.spring=spring?.id||'';
        }
        for (const item of pickups) {
            if (!state.collected.has(item.id) && overlaps(state.x, PLAYER_W, item.x, item.size)
                && overlaps(state.y, PLAYER_H, item.y, item.size)) {
                state.collected.add(item.id);
                award('pickup',item.id,earned.has(item.id)?0:item.points??rules.pickup);
                earned.add(item.id);
                status = stage ? state.collected.size+' picked up.' : state.collected.size === pickups.length
                    ? 'All sprites collected. The exit is open.'
                    : state.collected.size + ' of ' + pickups.length + ' sprites collected.';
            }
        }
        if (stage) {
            if (touches({...stage.goal,size:tileWorld.tile})) {
                state.won=true;
                award('goal','',rules.goal);
                status='Level complete.';
            }
            return;
        }
        if (state.x+PLAYER_W >= exit) {
            if (state.collected.size === pickups.length) {
                state.won = true;
                award('goal','',rules.goal);
                status = 'Level complete.';
            } else {
                state.x = Math.min(state.x, exit - PLAYER_W);
                state.vx=(state.x-lastX)/STEP;
                status = 'The exit is locked. Collect every pickup.';
            }
        }
    }

    return {
        get state() {return state;},
        get status() {return status;},
        get tick() {return tick;},
        get pickupCount() {return pickups.length;},
        first:()=>sample(false,0),
        step(direction,jump,capture=true) {
            tick++;
            notes=[];
            update(direction,jump);
            if (capture) return sample(jump,direction);
        }
    };
}

export function replayTileInputs(level, inputs) {
    const simulation=createTileSimulation(level);
    const rows=[simulation.first()];
    for (const [direction,jump] of inputs) {
        if (simulation.state.won) break;
        rows.push(simulation.step(direction,jump));
    }
    const reason=simulation.state.won?'won':simulation.tick===maxPlayTicks?'limit':'stopped';
    return {levelId:level.id,reason,rows};
}
