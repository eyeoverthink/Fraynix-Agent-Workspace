
import {readBase64} from './media-workshop.js';
import {drawPlacementLayout} from './spatial-placement.js';

const styles = {
    probe:['probeM','#8bcde6'],
    prop:['propM','#ffc47d']
};

function drawTrace(canvas, review, archived, local, sample) {
    const ctx = canvas.getContext('2d');
    const {width,height} = canvas;
    const limits = {left:Infinity,top:Infinity,right:-Infinity,bottom:-Infinity};
    function include([x,,z]) {
        limits.left = Math.min(limits.left,x);
        limits.right = Math.max(limits.right,x);
        limits.top = Math.min(limits.top,z);
        limits.bottom = Math.max(limits.bottom,z);
    }
    const [x,,z] = review.targetM;
    const [w,,d] = review.colliderM;
    const pad = Math.hypot(w,d)/2;
    include([x-pad,0,z-pad]);
    include([x+pad,0,z+pad]);
    for (const trace of [archived,local].filter(Boolean))
        for (const point of trace.samples) {
            include(point.probeM);
            include(point.propM);
        }
    const cx = (limits.left+limits.right)/2, cz = (limits.top+limits.bottom)/2;
    const scale = Math.min((width-100)/Math.max(.35,limits.right-limits.left),
        (height-64)/Math.max(.35,limits.bottom-limits.top));
    const point = ([x,,z]) => [width/2+(x-cx)*scale,height/2+(z-cz)*scale];

    ctx.fillStyle = '#101e27';
    ctx.fillRect(0,0,width,height);
    ctx.strokeStyle = '#2a424b';
    ctx.lineWidth = 1;
    ctx.beginPath();
    for (let gx = 32; gx < width; gx += 32) {ctx.moveTo(gx,0);ctx.lineTo(gx,height);}
    for (let gz = 32; gz < height; gz += 32) {ctx.moveTo(0,gz);ctx.lineTo(width,gz);}
    ctx.stroke();
    ctx.save();
    ctx.translate(...point(review.targetM));
    ctx.rotate(-review.yawDeg*Math.PI/180);
    ctx.fillStyle = '#bd925c32';
    ctx.strokeStyle = '#cfaa65';
    ctx.fillRect(-w*scale/2,-d*scale/2,w*scale,d*scale);
    ctx.strokeRect(-w*scale/2,-d*scale/2,w*scale,d*scale);
    ctx.restore();

    for (const [trace,dotted] of [[archived,false],[local,true]]) {
        if (!trace) continue;
        ctx.setLineDash(dotted ? [7,5] : []);
        ctx.lineWidth = dotted ? 2 : 4;
        for (const [key,color] of Object.values(styles)) {
            ctx.strokeStyle = dotted ? '#eef9f4' : color;
            ctx.beginPath();
            for (const [i,row] of trace.samples.entries())
                ctx[i ? 'lineTo' : 'moveTo'](...point(row[key]));
            ctx.stroke();
        }
    }
    ctx.setLineDash([]);
    for (const [key,color] of Object.values(styles)) {
        ctx.fillStyle = color;
        ctx.strokeStyle = '#101e27';
        ctx.beginPath();
        ctx.arc(...point(sample[key]),6,0,2*Math.PI);
        ctx.fill();
        ctx.stroke();
    }
    ctx.font = '11px system-ui';
    ctx.fillStyle = '#b6c8cf';
    ctx.fillText('X →',width-35,height-9);
    ctx.strokeStyle = '#b6c8cf';
    ctx.lineWidth = 1;
    ctx.beginPath();
    ctx.moveTo(12,height-12);
    ctx.lineTo(12+.25*scale,height-12);
    ctx.stroke();
    ctx.fillText('0.25 m',12,height-18);
    ctx.fillText('Z ↓',9,17);
}

export class PlacementExchangePanel {
    constructor(parent) {
        this.parent = parent;
        this.dialog = document.createElement('dialog');
        this.dialog.id = 'placement-exchange-dialog';
        this.dialog.setAttribute('aria-labelledby','exchange-title');
        this.dialog.innerHTML = `
            <div class="panel-head"><h2 id="exchange-title">Collision evidence viewer</h2>
                <button id="exchange-close">Close</button></div>
            <div class="placement-content">
                <div class="spatial-task exchange-controls">
                    <label class="media-file">Open evidence bundle
                        <input id="exchange-file" aria-label="Open evidence bundle" type="file" accept=".json,application/json"></label>
                    <button id="exchange-replay" class="primary" disabled>Replay in local physics</button>
                </div>
                <p id="exchange-status" role="status"></p>
                <section id="exchange-study" hidden>
                    <h3 id="exchange-scene-title"></h3>
                    <p id="exchange-provenance" class="spatial-note"></p>
                    <div class="spatial-task exchange-controls">
                        <label>Trial<select id="exchange-trial"><option value="hit">Hit</option><option value="miss">Lateral miss</option>
                            <option value="aisle" hidden>Walking aisle</option></select></label>
                    </div>
                    <div class="exchange-key" id="exchange-contact-key"><span>Probe</span><span>Prop</span><span id="exchange-local-key" hidden>Local replay · dashed</span></div>
                    <p class="spatial-note" id="exchange-aisle-key" hidden>Teal aisle · amber placed prop · gray existing colliders</p>
                    <canvas id="exchange-canvas" width="640" height="246" aria-label="Top-down probe and prop paths"></canvas>
                    <label class="exchange-scrubber" id="exchange-scrubber">Sample <input id="exchange-sample" aria-label="Trace sample" type="range" min="0" value="0"></label>
                    <p id="exchange-reading" aria-live="polite"></p>
                    <p id="exchange-metrics"></p>
                </section>
            </div>`;
        document.body.append(this.dialog);
        this.dialog.addEventListener('close',parent.layout);
        this.find('#exchange-close').onclick = () => this.dialog.close();
        this.find('#exchange-file').onchange = event => {
            const file = event.target.files[0];
            event.target.value = '';
            if (file) this.parent.action(() => this.load(file));
        };
        this.find('#exchange-replay').onclick = () => this.parent.action(() => this.replay());
        this.find('#exchange-trial').onchange = () => this.draw(true);
        this.find('#exchange-sample').oninput = () => this.draw(false);
    }

    find(selector) {
        return this.dialog.querySelector(selector);
    }

    tell(text) {
        this.find('#exchange-status').textContent = text;
    }

    async open() {
        if (!this.dialog.open) this.dialog.showModal();
        this.parent.layout();
        this.result = null;
        this.render();
        this.tell('Checking the last opened bundle…');
        try {
            this.result = await this.parent.call('placement-exchange-latest');
            this.render();
        } catch (error) {
            this.fail(error);
        }
    }

    async load(file) {
        this.tell('Checking the exported scene and archived evidence…');
        try {
            this.result = await this.parent.call('placement-exchange-open',{base64:await readBase64(file,1_200_000)});
            this.render();
            this.parent.notice('Collision evidence bundle opened.');
        } catch (error) {
            this.fail(error);
        }
    }

    async replay() {
        this.tell(this.result.layout
            ? 'Running both contact trials and aisle clearance twice in local physics…'
            : 'Running the hit and miss twice in local physics…');
        try {
            this.result = await this.parent.call('placement-exchange-replay',{id:this.result.id});
            this.render();
            this.tell('Local physics replay matched the archive within tolerance.');
            this.parent.notice('The local replay matched the archived collision.');
        } catch (error) {
            this.result = {...this.result, replay:null};
            this.render();
            this.fail(error);
        }
    }

    fail(error) {
        this.tell(error.message.replace(/^Error invoking remote method '[^']+': (?:Error: )?/, ''));
    }

    controls() {
        const busy = Boolean(this.parent.busy);
        this.find('#exchange-file').disabled = busy;
        this.find('#exchange-replay').disabled = busy || !this.result?.id;
    }

    render() {
        const result = this.result;
        this.find('#exchange-study').hidden = !result?.id;
        this.controls();
        if (!result?.id) {
            this.tell('Open a Fraynix scene and contact evidence bundle.');
            return;
        }
        this.find('#exchange-scene-title').textContent = result.title;
        const trial = this.find('#exchange-trial');
        trial.querySelector('[value=aisle]').hidden = !result.layout;
        if (!result.layout && trial.value === 'aisle') trial.value = 'hit';
        const provenance = 'Archive reports '+(100*result.sourceOverlap).toFixed(1)
            +'% source-view overlap. Original images stay with the source project.';
        const destination = result.destinationVerified
            ? ' Original destination and existing colliders match the reviewed source.'
            : result.layout ? ' This older bundle supplies the finished scene and aisle.' : '';
        this.find('#exchange-provenance').textContent = provenance+destination;
        this.find('#exchange-local-key').hidden = !result.replay;
        this.tell(result.replay ? 'Saved local replay matches the archive.'
            : result.replayUnavailable ? 'The saved local replay changed. Run a fresh check.'
                : 'Bundle and GLB checked. Replay when you’re ready.');
        this.draw(true);
    }

    draw(reset) {
        if (!this.result?.id) return;
        const kind = this.find('#exchange-trial').value;
        const aisle = kind === 'aisle';
        this.find('#exchange-contact-key').hidden = aisle;
        this.find('#exchange-scrubber').hidden = aisle;
        this.find('#exchange-aisle-key').hidden = !aisle;
        const canvas = this.find('#exchange-canvas');
        canvas.setAttribute('aria-label',aisle ? 'Top-down checked walking aisle and level colliders' : 'Top-down probe and prop paths');
        if (aisle) {
            const {layout, replay} = this.result;
            this.find('#exchange-reading').textContent = layout.routeLengthM.toFixed(2)+' m route · '
                +(layout.radius*2).toFixed(2)+' m wide · '+layout.heightM.toFixed(2)+' m tall';
            this.find('#exchange-metrics').textContent = replay?.comparison.aisle.passed
                ? 'Archived clearance passed · local clearance passed in both runs.'
                : 'Archived clearance passed · local clearance pending.';
            drawPlacementLayout(canvas,layout);
            return;
        }
        const archived = this.result.archived[kind], fresh = this.result.replay?.trials[kind];
        const slider = this.find('#exchange-sample');
        slider.max = archived.samples.length-1;
        if (reset) {
            slider.value = archived.firstContactTick
                ? archived.samples.findIndex(s=>s.tick===archived.firstContactTick) : 0;
        }
        const sample = archived.samples[Number(slider.value)];
        const at = (...v) => v.map(n=>n.toFixed(3)).join(', ');
        const seconds = (sample.tick*this.result.review.contact.stepS).toFixed(3);
        this.find('#exchange-reading').textContent = seconds+' s · tick '+sample.tick+' · prop X, Z '
            +at(sample.propM[0],sample.propM[2])+' m';
        const comparison = this.result.replay?.comparison[kind];
        this.find('#exchange-metrics').textContent = comparison
            ? 'Largest position difference '+(comparison.maxPositionErrorM*1000).toFixed(2)
                +' mm · largest velocity difference '+comparison.maxVelocityErrorMps.toFixed(3)+' m/s · contacts '
                +archived.contactTicks+' archived / '+fresh.contactTicks+' local'
            : 'Archived contact steps '+archived.contactTicks+' · maximum prop speed '+archived.maxPropPlanarSpeedMps.toFixed(3)+' m/s';
        drawTrace(canvas,this.result.review,archived,fresh,sample);
    }
}
