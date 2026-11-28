export const isLoadingAisleRequest = text =>
    /^(?:please\s+)?(?:move|shift)\b/i.test(text.trim()) &&
    /\bcrate\b/i.test(text) && /\bloading\s+door\b/i.test(text);

function drawFallback(canvas, scene, review, focusRoute=false) {
    const ctx = canvas.getContext('2d');
    const floor = scene.objects.find(item => item.sourceId === review.aisle.floor);
    const footprint = (center, size, rotation) => {
        const yaw = 2 * Math.atan2(rotation[1], rotation[3]);
        const c = Math.cos(yaw), s = Math.sin(yaw);
        return [[-1,-1],[1,-1],[1,1],[-1,1]].map(([x,z]) => [
            center[0] + c*x*size[0]/2 + s*z*size[2]/2,
            center[2] - s*x*size[0]/2 + c*z*size[2]/2]);
    };
    const floorPoints = footprint(floor.position, floor.size, floor.rotation);
    const path = review.route?.pointsM || [review.fromM,review.toM];
    const bounds = focusRoute ? path.map(([x,,z]) => [x,z]) : floorPoints;
    const padding = focusRoute ? .34 : 0;
    const xs = bounds.map(point => point[0]), zs = bounds.map(point => point[1]);
    const minX = Math.min(...xs)-padding, minZ = Math.min(...zs)-padding;
    const width = Math.max(...xs)+padding-minX, depth = Math.max(...zs)+padding-minZ;
    const scale = Math.min((canvas.width-32)/width, (canvas.height-30)/depth);
    const project = ([x,z]) => [(canvas.width-width*scale)/2+(x-minX)*scale,
        (canvas.height-depth*scale)/2+(z-minZ)*scale];
    const polygon = (points, color) => {
        ctx.beginPath();
        for (const [index, point] of points.entries()) {
            const [x,y] = project(point);
            if (index) ctx.lineTo(x,y); else ctx.moveTo(x,y);
        }
        ctx.closePath();
        ctx.fillStyle = color;
        ctx.fill();
    };
    ctx.fillStyle = '#122332';
    ctx.fillRect(0,0,canvas.width,canvas.height);
    polygon(floorPoints, '#2c4655');
    const {entryM, endM, widthM} = review.aisle;
    const [dx,,dz] = review.directionM;
    const offset = widthM/2;
    polygon([[entryM[0]-dz*offset,entryM[2]+dx*offset], [endM[0]-dz*offset,endM[2]+dx*offset],
             [endM[0]+dz*offset,endM[2]-dx*offset], [entryM[0]+dz*offset,entryM[2]-dx*offset]], '#429c90');
    const shelves = new Set(review.protected.map(item => item.sourceId));
    for (const body of scene.objects) {
        if (body === floor || body.sourceId === review.crate) continue;
        polygon(footprint(body.position,body.size,body.rotation),
            shelves.has(body.sourceId) ? '#9a8bdb' : '#77909f');
    }
    const crate = scene.objects.find(item => item.sourceId === review.crate);
    polygon(footprint(review.fromM,crate.size,crate.rotation), '#467d73');
    polygon(footprint(review.toM,crate.size,crate.rotation), '#eeb263');
    const points = path.map(([x,,z]) => project([x,z]));
    ctx.beginPath();
    ctx.moveTo(...points[0]);
    for (const point of points.slice(1)) ctx.lineTo(...point);
    ctx.strokeStyle = '#ffbe65';
    ctx.lineWidth = focusRoute ? 3 : 2;
    ctx.stroke();
    if (!focusRoute) return;
    ctx.font = 'bold 10px system-ui';
    ctx.textAlign = 'center';
    ctx.textBaseline = 'middle';
    points.forEach(([x,y],index) => {
        ctx.beginPath();
        ctx.arc(x,y,7,0,2*Math.PI);
        ctx.fillStyle = '#ffcf85';
        ctx.fill();
        ctx.fillStyle = '#142430';
        ctx.fillText(String(index),x,y+.5);
    });
}

const normalizeChoice = text => text.trim().toLowerCase().replace(/[.!?]+$/,'')
    .replace(/^(?:use|choose|pick|move)\s+/,'').replace(/^the\s+/,'').replace(/\s+/g,' ');

function moveDescription(review) {
    if (review.route) return '40 cm away from the door using ' + review.preview.route.segmentCount + ' verified segments';
    if (!review.alternate) return '40 cm away from the door';
    const offset = review.alternate.lateralM;
    return '40 cm farther into the bay and ' + Math.round(100*Math.abs(offset)) +
        ' cm ' + (offset < 0 ? 'left' : 'right');
}

export class LoadingAislePanel {
    constructor(parent, stage) {
        this.parent = parent;
        this.root = document.createElement('details');
        this.root.id = 'loading-aisle';
        this.root.className = 'spatial-detail';
        this.root.innerHTML = `
            <summary>Loading bay aisle review</summary>
            <div class="spatial-task">
                <label>Example to load
                    <select id="aisle-yaw">
                        <option value="37">Loading bay · 37°</option>
                        <option value="0">Loading bay · Square</option>
                        <option value="crossdock">Cross-dock · −29°</option>
                        <option value="quayside">Quayside · 68°</option>
                        <option value="closed">Closed bay · 121°</option>
                        <option value="pinched">Narrow bay · −73°</option>
                        <option value="staggered-east">Staggered bay · −41°</option>
                        <option value="staggered-west">Mirrored staggered bay · 83°</option>
                        <option value="staggered-closed">Blocked crossover · −118°</option>
                    </select>
                </label>
                <button id="aisle-load">Load labelled bay</button>
                <button id="aisle-resume">Resume saved review</button>
                <button id="aisle-approve" class="primary" hidden>Approve exact move</button>
                <button id="aisle-replay" hidden>Replay native checks</button>
                <button id="aisle-export" hidden>Export scene and evidence</button>
            </div>
            <p id="aisle-active" class="spatial-note" hidden></p>
            <p id="aisle-status" class="spatial-note" role="status">Select a crate, then ask Chat to move it 40 cm away from the loading door.</p>
            <div id="aisle-choices" class="spatial-task" aria-label="Crate choices" hidden></div>
            <p id="aisle-legend" class="spatial-note" hidden>Amber · proposed translation. Teal · 1 m aisle. Violet · protected shelving.</p>
            <div id="aisle-route-details" hidden>
                <canvas id="aisle-route-map" width="380" height="170" role="img" aria-label="Numbered crate route from start to destination"></canvas>
                <div class="aisle-steps">
                    <strong id="aisle-route-length"></strong>
                    <ol id="aisle-segments" aria-label="Checked route segments"></ol>
                </div>
            </div>
            <canvas id="aisle-plan" hidden width="480" height="236" role="img" aria-label="Top-down loading bay review"></canvas>`;
        stage.before(this.root);
        this.status = this.find('status');
        this.find('load').onclick = () => this.handle(async () => {
            const variant = this.find('yaw').value;
            const fixture = ['0','37'].includes(variant) ? await parent.call('aisle-fixture',{yaw:Number(variant)}) :
                await parent.call('aisle-bay',{variant});
            await parent.import(JSON.stringify(fixture));
            this.record = null;
            this.status.textContent = 'Choose the crate and ask Chat to move it 40 cm away from the loading door.';
            this.render();
        });
        this.find('resume').onclick = () => this.handle(async () => {
            const record = await parent.call('aisle-latest');
            if (!record.id) throw Error('There is no saved aisle review yet.');
            await this.openRecord(record);
        });
        this.find('approve').onclick = () => this.handle(async () => {
            if (parent.scene?.id !== this.record.review.parentId) throw Error('Return to the exact reviewed parent scene.');
            this.status.textContent = 'Measuring the crate path, shelf scope and full aisle in native physics…';
            const record = await parent.call('aisle-approve',{id:this.record.id,approvalSha256:this.record.id});
            await this.openRecord(record);
            parent.announce('I moved only ' + record.review.crateLabel + ' ' + moveDescription(record.review) +
                '. The 1 m aisle was clear in both native repeats, and the shelves are unchanged.');
        });
        this.find('replay').onclick = () => this.handle(async () => {
            const record = await parent.call('aisle-replay',{id:this.record.id});
            await this.openRecord(record);
        });
        this.find('export').onclick = () => this.handle(async () => {
            const file = await parent.call('aisle-export',{id:this.record.id});
            this.status.textContent = 'Exported the checked scene and receipt.';
            parent.notice('Saved ' + file.path);
        });
    }

    find(name) { return this.root.querySelector('#aisle-' + name); }

    handle(action) {
        return this.parent.action(async () => {
            try { await action(); }
            catch (error) { this.status.textContent = error.message; this.parent.notice(error.message); }
        });
    }

    async fromChat(request) {
        this.root.open = true;
        this.parent.activate('spatial');
        this.record = null;
        this.render();
        return this.handle(async () => {
            if (!this.parent.scene || this.parent.worldMode || this.parent.poseMode)
                throw Error('Choose an imported Spatial scene for this edit.');
            const record = await this.parent.call('aisle-intent',{sceneId:this.parent.scene.id,request});
            await this.show(record,{announce:true});
        });
    }

    async answerFromChat(text) {
        const record = this.record;
        if (record?.status !== 'clarify' || record.parentId !== this.parent.scene?.id ||
            this.parent.worldMode || this.parent.poseMode) return false;
        const name = normalizeChoice(text);
        if (!record.choices.some(choice => [normalizeChoice(choice.label),choice.sourceId.toLowerCase()]
            .some(value => value === name || value === name + ' crate')) && !/^[\w -]{0,38}\bcrate$/.test(name))
            return false;
        await this.answer({answer:text});
        return true;
    }

    answer(choice) {
        return this.handle(async () => {
            const record = await this.parent.call('aisle-choose',{
                id:this.record.id,sceneId:this.parent.scene.id,...choice,
            });
            await this.show(record,{announce:true});
        });
    }

    async openRecord(record) {
        await this.parent.open(record.sceneId || record.review?.parentId || record.parentId);
        await this.show(record,{restoring:true});
    }

    async show(record,{restoring=false,announce=false}={}) {
        const review = record.review;
        const scene = this.parent.scene;
        const crate = review && scene.objects.find(item => item.sourceId === review.crate);
        if (crate && crate.id !== this.parent.object?.id) await this.parent.selectObject(crate.id);
        this.record = record;
        this.root.open = true;
        let message;
        if (record.status === 'clarify') {
            const labels = record.choices.map(item => item.label);
            const choices = record.choices.map(item => labels.filter(label => label === item.label).length > 1 ?
                item.label + ' (' + item.sourceId + ')' : item.label);
            message = 'Which crate should I move? ' + choices.join(' · ');
        } else if (record.status === 'blocked') {
            message = record.direct.reason + ' I found no safe alternate for ' + record.crateLabel + '. The bay is unchanged.';
        } else if (record.status === 'verified') {
            const measured = record.proof.measurements;
            message = (record.replayed ? 'Offline replay matched' :
                review.route ? 'Verified route · ' + review.preview.route.segmentCount + ' segments' :
                review.alternate ? 'Verified alternate · ' + moveDescription(review) : 'Verified · 40 cm away from door') +
                ' · 1.00 m aisle clear · ' + measured.shelvesPreserved + ' shelves unchanged · ' +
                measured.crateSupportedTicks + ' supported ticks';
        } else if (review.route) {
            message = (restoring ? 'Restored route. ' : '') + review.route.direct.reason + ' Found a ' +
                review.preview.route.segmentCount + '-segment route to the original 40 cm destination. Approve this exact route to apply it.';
        } else if (review.alternate) {
            message = (restoring ? 'Restored alternate. ' : '') + review.alternate.direct.reason + ' Safe alternate for ' +
                review.crateLabel + ': ' + moveDescription(review) + '. Approve this exact path to apply it.';
        } else if (record.status === 'approved') {
            message = 'Approval saved. Re-run the native checks for this exact move.';
        } else {
            message = restoring ? 'Restored pending review. Approve the exact 40 cm move to run the native checks.' :
                'Review the 40 cm move before approving. The original scene is still open.';
        }
        this.status.textContent = message;
        this.render();
        if (announce) this.parent.announce(review && !review.alternate && !review.route ?
            'The 40 cm move is ready for review in Spatial. The teal overlay marks the one-metre aisle.' : message);
    }

    renderChoices(record,busy) {
        const list = this.find('choices');
        if (this.questionKey !== record?.id) {
            this.questionKey = record?.id;
            list.replaceChildren(...(record?.choices || []).map(item => {
                const button = document.createElement('button');
                button.type = 'button';
                button.textContent = record.choices.filter(choice => choice.label === item.label).length > 1 ?
                    item.label + ' (' + item.sourceId + ')' : item.label;
                button.onclick = () => this.answer({objectId:item.id});
                return button;
            }));
        }
        list.hidden = !record;
        for (const button of list.children) button.disabled = busy;
    }

    renderRoute(scene,review) {
        const route = review?.preview?.route;
        this.find('route-details').hidden = !route;
        if (!route) return;
        drawFallback(this.find('route-map'),scene,review,true);
        this.find('route-length').textContent = route.segmentCount + ' checked segments · ' + route.distanceM.toFixed(2) + ' m total';
        this.find('segments').replaceChildren(...route.segments.map(segment => {
            const item = document.createElement('li');
            const distance = Math.round(segment.distanceM*100);
            item.textContent = 'Point ' + (segment.number-1) + ' → ' + segment.number + ' · ' + distance + ' cm · clear';
            return item;
        }));
    }

    render() { this.controls(this.parent.worldMode || this.parent.poseMode || this.parent.agentBusy); }

    controls(blocked) {
        const record = this.record;
        const scene = this.parent.scene;
        const available = !this.parent.worldMode && !this.parent.poseMode;
        const matches = available && record?.review && [record.review.parentId,record.sceneId].includes(scene?.id);
        const active = matches ? record.review : null;
        const signature = active ? record.id + ':' + scene.id : '';
        if (signature !== this.previewKey) {
            this.previewKey = signature;
            this.parent.aisleOverlay(active);
            const canvas = this.find('plan');
            canvas.hidden = !active || Boolean(this.parent.renderer);
            if (!canvas.hidden) drawFallback(canvas,scene,active);
            this.renderRoute(scene,active);
        }
        const busy = this.parent.busy || blocked;
        this.find('load').disabled = this.find('resume').disabled = busy;
        this.find('legend').hidden = !active;
        const activeName = this.find('active');
        activeName.hidden = !available || !scene || !this.root.open || !record;
        if (!activeName.hidden) activeName.textContent = 'Active bay · ' + scene.title;
        this.renderChoices(available && record?.status === 'clarify' && record.parentId === scene?.id ? record : null,busy);
        this.find('approve').hidden = !matches || record.status === 'verified';
        this.find('approve').disabled = busy;
        this.find('approve').textContent = active?.route ? 'Approve this route' :
            active?.alternate ? 'Approve this alternate' : 'Approve exact move';
        this.find('replay').hidden = this.find('export').hidden = !matches || record.status !== 'verified';
        this.find('replay').disabled = this.find('export').disabled = busy;
    }
}
