
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

function drawImpactTimeline(canvas, original, resized) {
    const ctx = canvas.getContext('2d');
    const {width,height} = canvas;
    const lastTick = Math.ceil((Math.max(original,resized)+8)/10)*10;
    const x = tick => 95+(width-115)*tick/lastTick;
    ctx.fillStyle = '#101e27';
    ctx.fillRect(0,0,width,height);
    ctx.font = '13px system-ui';
    ctx.fillStyle = '#a9c3cf';
    ctx.strokeStyle = '#324b55';
    for (let tick=0;tick<=lastTick;tick+=10) {
        ctx.beginPath();
        ctx.moveTo(x(tick),13);
        ctx.lineTo(x(tick),height-24);
        ctx.stroke();
        ctx.fillText(String(tick),x(tick)-5,height-8);
    }
    for (const [label,tick,y,color] of [['Original',original,35,'#8bcde6'],['Resized',resized,76,'#ffbf77']]) {
        ctx.fillStyle = color;
        ctx.strokeStyle = color;
        ctx.fillText(label,12,y+4);
        ctx.beginPath();
        ctx.moveTo(x(0),y);
        ctx.lineTo(x(tick),y);
        ctx.stroke();
        ctx.beginPath();
        ctx.arc(x(tick),y,5,0,2*Math.PI);
        ctx.fill();
        ctx.fillText('tick '+tick,x(tick)+9,y+4);
    }
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
                    <button id="exchange-fork" hidden>Create editable copy</button>
                </div>
                <p id="exchange-status" role="status"></p>
                <section id="exchange-study" hidden>
                    <h3 id="exchange-scene-title"></h3>
                    <p id="exchange-provenance" class="spatial-note"></p>
                    <p id="exchange-revisions" class="spatial-note"></p>
                    <div class="spatial-task exchange-controls">
                        <label id="exchange-stage-label" hidden>Revision<select id="exchange-stage"></select></label>
                        <label>Trial<select id="exchange-trial"><option value="hit">Hit</option><option value="miss">Lateral miss</option>
                            <option value="aisle" hidden>Walking aisle</option>
                            <option value="compare" hidden>Same probe comparison</option>
                            <option value="sweep" hidden>Five probe approaches</option></select></label>
                    </div>
                    <div class="exchange-key" id="exchange-contact-key"><span>Probe</span><span>Prop</span><span id="exchange-local-key" hidden>Local replay · dashed</span></div>
                    <p class="spatial-note" id="exchange-aisle-key" hidden>Teal aisle · amber placed prop · gray existing colliders</p>
                    <canvas id="exchange-canvas" width="640" height="246" aria-label="Top-down probe and prop paths"></canvas>
                    <label class="exchange-scrubber" id="exchange-scrubber">Sample <input id="exchange-sample" aria-label="Trace sample" type="range" min="0" value="0"></label>
                    <p id="exchange-reading" aria-live="polite"></p>
                    <p id="exchange-metrics"></p>
                    <section id="exchange-fixed-approach" aria-label="Same probe comparison" hidden>
                        <div class="spatial-task exchange-controls">
                            <button id="exchange-compare" class="primary">Run same-approach trials</button>
                            <p id="exchange-comparison-status" role="status"></p>
                        </div>
                        <div id="exchange-sweep-overview" hidden>
                            <p id="exchange-sweep-summary"></p>
                            <table id="exchange-sweep-table">
                                <thead><tr><th scope="col">Heading</th><th scope="col">Offset</th>
                                    <th scope="col">Original hit</th><th scope="col">Resized hit</th></tr></thead>
                                <tbody></tbody>
                            </table>
                            <label>Inspect approach <select id="exchange-sweep-case"></select></label>
                        </div>
                        <div id="exchange-comparison-result" hidden>
                            <p id="exchange-comparison-verdict"></p>
                            <canvas id="exchange-comparison-chart" width="640" height="120" role="img"
                                aria-label="Original and resized first contact on the same timeline"></canvas>
                            <table>
                                <thead><tr><th scope="col">Measurement</th><th scope="col">Original</th><th scope="col">Resized</th></tr></thead>
                                <tbody>
                                    <tr><th scope="row">Box width × depth</th><td></td><td></td></tr>
                                    <tr><th scope="row">First contact tick</th><td></td><td></td></tr>
                                    <tr><th scope="row">Hit contact ticks</th><td></td><td></td></tr>
                                    <tr><th scope="row">Miss contact ticks</th><td></td><td></td></tr>
                                    <tr><th scope="row">Walking aisle</th><td></td><td></td></tr>
                                </tbody>
                            </table>
                            <p id="exchange-comparison-launch" class="spatial-note"></p>
                        </div>
                    </section>
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
        this.find('#exchange-compare').onclick = () => this.parent.action(() => this.compare());
        this.find('#exchange-sweep-case').onchange = () => this.drawComparison(true);
        this.find('#exchange-fork').onclick = () => this.parent.action(() => this.fork());
        this.find('#exchange-trial').onchange = () => this.draw(true);
        this.find('#exchange-stage').onchange = () => this.draw(true);
        this.find('#exchange-sample').oninput = () => this.draw(false);
    }

    find(selector) {
        return this.dialog.querySelector(selector);
    }

    tell(text) {
        this.find('#exchange-status').textContent = text;
    }

    async open(recipientSceneId) {
        this.attachmentMode = Boolean(recipientSceneId);
        if (!this.dialog.open) this.dialog.showModal();
        this.parent.layout();
        this.result = null;
        this.render();
        this.tell(recipientSceneId ? 'Checking the attached archive and replay…' : 'Checking the last opened bundle…');
        try {
            this.result = recipientSceneId ? await this.parent.call('recipient-evidence',{sceneId:recipientSceneId})
                : await this.parent.call('placement-exchange-latest');
            this.render();
        } catch (error) {
            this.fail(error);
        }
    }

    async load(file) {
        this.tell('Checking the exported scene and archived evidence…');
        try {
            this.result = await this.parent.call('placement-exchange-open',{base64:await readBase64(file,1_500_000)});
            this.attachmentMode = false;
            this.render();
            this.parent.notice('Collision evidence bundle opened.');
        } catch (error) {
            this.fail(error);
        }
    }

    async replay() {
        this.tell(this.result.recipientRevision
            ? 'Replaying the original level and each approved edit twice…'
            : this.result.layout ? 'Running both contact trials and aisle clearance twice in local physics…'
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

    async compare() {
        const {id,replay} = this.result;
        const sweep = this.find('#exchange-trial').value === 'sweep';
        const status = this.find('#exchange-comparison-status');
        status.textContent = sweep ? 'Running matched hit and miss tests at five approaches…'
            : 'Running both collider sizes from the same hit and miss starts…';
        try {
            const command = sweep ? 'placement-exchange-sweep' : 'placement-exchange-compare';
            const receipt = await this.parent.call(command,{id,replaySha256:replay.sha256});
            if (sweep) this.result = {...this.result,colliderSweep:receipt,colliderSweepUnavailable:false};
            else this.result = {...this.result,colliderComparison:receipt,colliderComparisonUnavailable:false};
            this.draw(false);
            this.parent.notice(sweep ? 'The five-approach comparison was saved.' : 'The same-approach comparison was saved.');
        } catch (error) {
            status.textContent = error.message.replace(/^Error invoking remote method '[^']+': (?:Error: )?/, '');
        }
    }

    async fork() {
        try {
            const {id,replay} = this.result;
            const scene = await this.parent.call('placement-exchange-fork',{id,replaySha256:replay.sha256});
            this.dialog.close();
            await this.parent.open(scene.id);
            await this.parent.recipient.open();
        } catch (error) { this.fail(error); }
    }

    fail(error) {
        this.tell(error.message.replace(/^Error invoking remote method '[^']+': (?:Error: )?/, ''));
    }

    controls() {
        const busy = Boolean(this.parent.busy);
        this.find('#exchange-file').disabled = busy;
        this.find('#exchange-replay').disabled = busy || !this.result?.id;
        this.find('#exchange-compare').disabled = busy || !this.result?.colliderComparisonReady;
        const fork = this.find('#exchange-fork');
        fork.hidden = this.attachmentMode || !this.result?.destinationVerified || !this.result.replay;
        fork.textContent = this.result?.recipientRevision ? 'Create editable copy of final revision' : 'Create editable copy';
        fork.disabled = busy || fork.hidden;
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
        for (const value of ['compare','sweep'])
            trial.querySelector('[value='+value+']').hidden = !result.colliderComparisonReady;
        if (trial.selectedOptions[0]?.hidden) trial.value = 'hit';
        const provenance = 'Archive reports '+(100*result.sourceOverlap).toFixed(1)
            +'% source-view overlap. Original images stay with the source project.';
        const destination = result.destinationVerified
            ? ' Original destination and existing colliders match the reviewed source.'
            : result.layout ? ' This older bundle supplies the finished scene and aisle.' : '';
        this.find('#exchange-provenance').textContent = provenance+destination;
        const count = result.recipientRevision || 0;
        const stage = this.find('#exchange-stage');
        const prior = stage.options.length === count + 1 ? stage.value : '';
        stage.replaceChildren();
        this.find('#exchange-stage-label').hidden = !count;
        if (count) {
            stage.add(new Option('Original placement','0'));
            for (let index=1;index<=count;index++) {
                const review = index===count ? result.review : result.previousStages[index].review;
                const label = review.collider ? 'Collider edit ' : result.handoffCount ? 'Approved edit ' : 'Local edit ';
                stage.add(new Option(label+index+(index===count?' · final':''),String(index)));
            }
            stage.value = prior || String(count);
        }
        this.find('#exchange-revisions').textContent = count
            ? count+' approved '+(count===1?'edit':'edits')+' · '
                +(result.replay?'Original and all revisions replayed locally.':'Original evidence and approvals checked.')
            : '';
        this.find('#exchange-local-key').hidden = !result.replay;
        this.tell(result.replay ? 'Saved local replay matches the archive.'
            : result.replayUnavailable ? 'The saved local replay changed. Run a fresh check.'
                : 'Bundle and GLB checked. Replay when you’re ready.');
        this.draw(true);
    }

    sweepCase(receipt) {
        const {binding, cases, summary} = receipt;
        this.find('#exchange-sweep-summary').textContent = summary.approaches+' approaches · '+summary.sphereTrials
            +' trials · Resized hit earlier in '+summary.earlier+', tied in '+summary.tied+', later in '+summary.later+'.';
        const body = this.find('#exchange-sweep-table tbody');
        const picker = this.find('#exchange-sweep-case');
        const index = Number(picker.value || 0);
        picker.replaceChildren();
        body.replaceChildren();
        for (const [i,definition] of binding.cases.entries()) {
            const label = [definition.headingDeg+'°',(100*definition.lateralOffsetM)+' cm'];
            picker.add(new Option(label.join(' · '),String(i)));
            const row = body.insertRow();
            for (const value of [...label, cases[i].original.hit.firstContactTick, cases[i].resized.hit.firstContactTick])
                row.insertCell().textContent = value;
        }
        picker.value = String(index);
        return {...cases[index],binding:{...binding.pair,approach:binding.cases[index].approach}};
    }

    drawComparison(sweep) {
        const receipt = sweep ? this.result.colliderSweep : this.result.colliderComparison;
        const damaged = sweep ? this.result.colliderSweepUnavailable : this.result.colliderComparisonUnavailable;
        this.find('#exchange-compare').textContent = sweep ? 'Run five approaches' : 'Run same-approach trials';
        this.find('#exchange-sweep-overview').hidden = !sweep || !receipt;
        this.find('#exchange-comparison-result').hidden = !receipt;
        this.find('#exchange-comparison-status').textContent = receipt ? 'Verified comparison saved locally.'
            : damaged ? 'The saved comparison changed. Run a fresh check.'
            : 'Compare the most recent collider edit with the scene immediately before it.';
        if (!receipt) return;
        const result = sweep ? this.sweepCase(receipt) : receipt;
        const {binding,original,resized,result:metrics} = result;
        const delta = metrics.resizedEarlierByTicks;
        const absoluteMs = Math.abs(metrics.resizedEarlierByMs).toFixed(2);
        this.find('#exchange-comparison-verdict').textContent = delta
            ? 'Resized collider hit '+Math.abs(delta)+' ticks ('+absoluteMs+' ms) '+(delta>0?'earlier.':'later.')
            : 'Both colliders hit on the same tick.';
        const canvas = this.find('#exchange-comparison-chart');
        canvas.setAttribute('aria-label','First contact, original tick '+original.hit.firstContactTick
            +' and resized tick '+resized.hit.firstContactTick);
        drawImpactTimeline(canvas,original.hit.firstContactTick,resized.hit.firstContactTick);
        const dims = size => size[0].toFixed(2)+' × '+size[2].toFixed(2)+' m';
        const cells = [
            [dims(binding.beforeColliderM),dims(binding.afterColliderM)],
            [original.hit.firstContactTick,resized.hit.firstContactTick],
            [original.hit.contactTicks,resized.hit.contactTicks],
            [original.miss.contactTicks,resized.miss.contactTicks],
            [original.routeLengthM.toFixed(2)+' m clear',resized.routeLengthM.toFixed(2)+' m clear']
        ].flat();
        this.find('#exchange-comparison-result').querySelectorAll('td').forEach((cell,index)=>{cell.textContent=cells[index];});
        const {approach} = binding;
        const point = xyz => '('+xyz.map(value=>value.toFixed(3)).join(', ')+') m';
        this.find('#exchange-comparison-launch').textContent = 'Both used '+approach.probeSpeedMps.toFixed(2)
            +' m/s · '+(approach.radiusM*1000).toFixed(0)+' mm radius. Hit starts at '
            +point(approach.hitStartM)+' and the miss starts at '+point(approach.missStartM)+'.';
    }

    draw(reset) {
        if (!this.result?.id) return;
        const kind = this.find('#exchange-trial').value;
        const compare = kind === 'compare' || kind === 'sweep';
        this.find('#exchange-stage-label').hidden = compare || !this.result.recipientRevision;
        this.find('#exchange-fixed-approach').hidden = !compare;
        for (const name of ['canvas','reading','metrics']) this.find('#exchange-'+name).hidden = compare;
        if (compare) {
            for (const key of ['contact-key','aisle-key','scrubber']) this.find('#exchange-'+key).hidden = true;
            this.drawComparison(kind === 'sweep');
            return;
        }
        const index = Number(this.find('#exchange-stage').value);
        const previous = index < (this.result.recipientRevision || 0);
        const current = previous ? this.result.previousStages[index] : this.result;
        const replay = previous ? this.result.replay?.previous[index] : this.result.replay;
        const aisle = kind === 'aisle';
        this.find('#exchange-contact-key').hidden = aisle;
        this.find('#exchange-scrubber').hidden = aisle;
        this.find('#exchange-aisle-key').hidden = !aisle;
        const canvas = this.find('#exchange-canvas');
        canvas.setAttribute('aria-label',aisle ? 'Top-down checked walking aisle and level colliders' : 'Top-down probe and prop paths');
        if (aisle) {
            const {layout} = current;
            this.find('#exchange-reading').textContent = layout.routeLengthM.toFixed(2)+' m route · '
                +(layout.radius*2).toFixed(2)+' m wide · '+layout.heightM.toFixed(2)+' m tall';
            this.find('#exchange-metrics').textContent = replay?.comparison.aisle.passed
                ? 'Archived clearance passed · local clearance passed in both runs.'
                : 'Archived clearance passed · local clearance pending.';
            drawPlacementLayout(canvas,layout);
            return;
        }
        const archived = current.archived[kind], fresh = replay?.trials[kind];
        const slider = this.find('#exchange-sample');
        slider.max = archived.samples.length-1;
        if (reset) {
            slider.value = archived.firstContactTick
                ? archived.samples.findIndex(s=>s.tick===archived.firstContactTick) : 0;
        }
        const sample = archived.samples[Number(slider.value)];
        const at = (...v) => v.map(n=>n.toFixed(3)).join(', ');
        const seconds = (sample.tick*current.review.contact.stepS).toFixed(3);
        this.find('#exchange-reading').textContent = seconds+' s · tick '+sample.tick+' · prop X, Z '
            +at(sample.propM[0],sample.propM[2])+' m';
        const comparison = replay?.comparison[kind];
        this.find('#exchange-metrics').textContent = comparison
            ? 'Largest position difference '+(comparison.maxPositionErrorM*1000).toFixed(2)
                +' mm · largest velocity difference '+comparison.maxVelocityErrorMps.toFixed(3)+' m/s · contacts '
                +archived.contactTicks+' archived / '+fresh.contactTicks+' local'
            : 'Archived contact steps '+archived.contactTicks+' · maximum prop speed '+archived.maxPropPlanarSpeedMps.toFixed(3)+' m/s';
        drawTrace(canvas,current.review,archived,fresh,sample);
    }
}
