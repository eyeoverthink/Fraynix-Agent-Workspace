
const levelRequest = /^(?:please )?(?:place|put|add|drop)\s+([^.!?]{1,100}?)\s+(?:into|in|to) (?:my|the|this|our) (?:existing |open |current )?(?:level|scene)\b/i;
const propReference = /\b(?:prop|bracket|model|selected (?:object|asset))\b|^(?:it|this|that)$/i;
const existingLevelRequest = text => propReference.test(levelRequest.exec(text.trim())?.[1] ?? '');

export const isPlacementRequest = text => existingLevelRequest(text)
    || /^(?:please )?place (?:the )?(?:selected |verified )?(?:3d )?prop\b/i.test(text.trim());

export class SpatialPlacementPanel {
    constructor(parent) {
        this.parent = parent;
        this.dialog = document.createElement('dialog');
        this.dialog.id = 'placement-dialog';
        this.dialog.setAttribute('aria-labelledby','placement-title');
        this.dialog.innerHTML = `
            <div class="panel-head"><h2 id="placement-title">Reviewed prop placement</h2>
                <button id="placement-close">Close</button></div>
            <div class="placement-content">
                <p id="placement-request"></p>
                <form id="placement-level-fields" hidden>
                    <p>Choose the destination and mark a straight player aisle on its floor. Use metres.</p>
                    <div class="spatial-bind">
                        <label>Destination level<select id="placement-destination" required></select></label>
                        <label>Floor<select id="placement-floor" required></select></label>
                    </div>
                    <div class="spatial-task">
                        <label>Prop X<input name="x" type="number" step="any" required></label>
                        <label>Prop Z<input name="z" type="number" step="any" required></label>
                        <label>Yaw in degrees<input name="yaw" type="number" step="any" value="0" required></label>
                    </div>
                    <div class="spatial-task">
                        <label>Aisle start X<input name="startX" type="number" step="any" required></label>
                        <label>Aisle start Z<input name="startZ" type="number" step="any" required></label>
                        <label>Aisle end X<input name="endX" type="number" step="any" required></label>
                        <label>Aisle end Z<input name="endZ" type="number" step="any" required></label>
                    </div>
                    <button id="placement-level-prepare">Preview level edit</button>
                </form>
                <dl id="placement-details"></dl>
                <figure id="placement-level-preview" hidden>
                    <canvas width="480" height="220" role="img"
                        aria-label="Top-down preview of proposed prop and aisle on the existing floor"></canvas>
                    <figcaption>Amber prop · teal walking aisle · gray colliders</figcaption>
                </figure>
                <p id="placement-scope" class="spatial-note" hidden></p>
                <p id="placement-status" role="status"></p>
                <p id="placement-result" role="status"></p>
                <p id="placement-file"></p>
                <div class="spatial-task">
                    <button id="placement-approve" class="primary" hidden>Approve and run contact test</button>
                    <button id="placement-open-scene" hidden>Open placed scene</button>
                    <button id="placement-replay" hidden>Replay collision</button>
                    <button id="placement-export" hidden>Save scene and evidence</button>
                    <button id="placement-play" hidden>Save offline playable level</button>
                </div>
            </div>`;
        document.body.append(this.dialog);
        this.dialog.addEventListener('close',parent.layout);
        this.find('#placement-close').onclick = () => this.dialog.close();
        this.find('#placement-approve').onclick = () => this.parent.action(() => this.approve());
        this.find('#placement-replay').onclick = () => this.parent.action(() => this.replay());
        this.find('#placement-export').onclick = () => this.parent.action(() => this.save());
        this.find('#placement-play').onclick = () => this.parent.action(() => this.save('play'));
        this.find('#placement-destination').onchange = () => this.parent.action(() => this.updateFloors());
        this.find('#placement-level-fields').onsubmit = event => {
            event.preventDefault();
            void this.parent.action(() => this.prepareLevel());
        };
        this.find('#placement-open-scene').onclick = () => this.parent.action(async () => {
            this.dialog.close();
            await this.parent.open(this.result.sceneId);
        });
    }

    find(selector) {
        return this.dialog.querySelector(selector);
    }

    show() {
        this.result = null;
        const form = this.find('#placement-level-fields');
        form.hidden = true;
        form.reset();
        this.render();
        this.find('#placement-file').textContent = '';
        if (!this.dialog.open) this.dialog.showModal();
        this.parent.layout();
    }

    async openFromChat(goal) {
        this.show();
        this.tell('Preparing a review for your selected prop…');
        try {
            if (existingLevelRequest(goal)) {
                await this.setupLevel(goal);
                return;
            }
            this.result = await this.parent.call('placement-review',{goal});
            this.render();
            this.parent.notice('Placement review is ready.');
        } catch (error) {
            this.tell(this.errorMessage(error));
        }
    }

    async setupLevel(goal) {
        const {scene, object} = this.parent;
        if (!scene?.reconstruction || !object?.visualMesh)
            throw Error('Select a verified reconstructed prop in Spatial first.');
        this.goal = goal;
        const destination = this.find('#placement-destination');
        const records = await this.parent.call('list');
        destination.replaceChildren(new Option('Choose an existing Spatial level',''));
        for (const entry of records.scenes) {
            if (entry.id !== scene.id) destination.add(new Option(entry.title,entry.id));
        }
        await this.updateFloors();
        this.find('#placement-request').textContent = goal;
        this.find('#placement-level-fields').hidden = false;
        this.tell('Choose an existing Spatial scene and enter the prop and aisle coordinates.');
    }

    async updateFloors() {
        const id = this.find('#placement-destination').value;
        const floor = this.find('#placement-floor');
        floor.replaceChildren(new Option('Choose the floor',''));
        if (!id) return;
        const destination = await this.parent.call('read',{id});
        for (const body of destination.objects) {
            if (!body.massKg && body.rotation.slice(0,3).every(value => Math.abs(value) <= 1e-8))
                floor.add(new Option(body.label,body.id));
        }
        if (floor.options.length === 2) floor.selectedIndex = 1;
    }

    async prepareLevel() {
        const form = this.find('#placement-level-fields');
        const coordinate = name => form.elements.namedItem(name).valueAsNumber;
        this.tell('Checking the prop footprint and player aisle…');
        try {
            this.result = await this.parent.call('placement-level-review',{
                goal:this.goal, destinationId:this.find('#placement-destination').value,
                floorId:this.find('#placement-floor').value, targetM:['x','z'].map(coordinate),
                yawDeg:coordinate('yaw'), aisle:['startX','startZ','endX','endZ'].map(coordinate)
            });
            form.hidden = true;
            this.render();
        } catch (error) {
            this.tell(this.errorMessage(error));
        }
    }

    errorMessage(error) {
        return error.message.replace(/^Error invoking remote method '[^']+': (?:Error: )?/, '');
    }

    preview(layout) {
        const figure = this.find('#placement-level-preview');
        figure.hidden = !layout;
        if (!layout) return;
        const canvas = figure.querySelector('canvas');
        const ctx = canvas.getContext('2d');
        const {floor,route,blockers,prop,radius} = layout;
        const w = floor[3]-floor[0], d = floor[5]-floor[2];
        const scale = Math.min(450/w,192/d);
        const x = value => (480-w*scale)/2+(value-floor[0])*scale;
        const z = value => (220-d*scale)/2+(value-floor[2])*scale;
        ctx.fillStyle = '#102429';
        ctx.fillRect(0,0,480,220);
        ctx.fillStyle = '#29434b';
        ctx.fillRect(x(floor[0]),z(floor[2]),w*scale,d*scale);
        ctx.fillStyle = '#4da78c';
        ctx.fillRect(x(Math.min(route[0],route[2])-radius),z(Math.min(route[1],route[3])-radius),
            (Math.abs(route[2]-route[0])+2*radius)*scale,(Math.abs(route[3]-route[1])+2*radius)*scale);
        for (const {id,box} of blockers) {
            ctx.fillStyle = id === prop ? '#f0b176' : '#8495aa';
            ctx.fillRect(x(box[0]),z(box[2]),(box[3]-box[0])*scale,(box[5]-box[2])*scale);
        }
    }

    async openLatest() {
        this.show();
        this.tell('Opening your saved review…');
        try {
            this.result = await this.parent.call('placement-latest');
            this.render();
        } catch (error) {
            this.tell(this.errorMessage(error));
        }
    }

    tell(text) {
        this.find('#placement-status').textContent = text;
    }

    render() {
        const result = this.result;
        const review = result?.review;
        const state = result?.status;
        const hasResult = state === 'verified';
        const level = Boolean(review?.destination);
        const approve = this.find('#placement-approve');
        approve.hidden = !review || hasResult;
        approve.textContent = state === 'approved' ? 'Run approved contact test' : 'Approve and run contact test';
        const scope = this.find('#placement-scope');
        scope.hidden = !review || hasResult;
        scope.textContent = level
            ? 'Approval runs native contact and aisle checks on a copy. Fraynix saves a new revision only if the repeat matches.'
            : 'Approval runs two hit and miss pairs on a new contact deck. The source model stays in its original scene.';
        this.preview(result?.layout);
        this.find('#placement-request').textContent = review?.goal || '';
        const details = this.find('#placement-details');
        details.replaceChildren();
        if (review) {
            const xyz = review.targetM.map(m => Number(m.toFixed(4))).join(', ');
            const destination = level ? [
                ['Destination',review.destinationTitle], ['Revision',review.destination.sceneId],
                ['Floor',review.floorLabel],
                ['Clear aisle',result.layout.routeLengthM.toFixed(2)+' m · '+(2*result.layout.radius).toFixed(2)+' m wide'],
                ['Aisle start → exit',review.aisle.slice(0,2).join(', ')+' → '+review.aisle.slice(2).join(', ')+' m (X, Z)']
            ] : [];
            for (const [key,value] of [...destination,
                ['Source prop',review.sourceTitle],['Checked view',(100*review.sourceOverlap).toFixed(1)+'% overlap'],
                ['Placement X, Y, Z',xyz+' m'],['Yaw',review.yawDeg+'°'],
                ['Collider',review.colliderM.map(m=>Number(m.toFixed(4))).join(' × ')+' m']
            ]) {
                const label = document.createElement('dt'), text = document.createElement('dd');
                label.textContent = key;
                text.textContent = value;
                details.append(label,text);
            }
        }
        for (const selector of ['#placement-open-scene','#placement-replay','#placement-export'])
            this.find(selector).hidden = !hasResult;
        this.find('#placement-play').hidden = !hasResult || !level;
        const measurement = result?.evidence?.measured;
        this.find('#placement-result').textContent = measurement
            ? 'Native hit at tick '+measurement.hit.firstContactTick+' · prop peaked at '
              +measurement.hit.maxPropPlanarSpeedMps.toFixed(3)+' m/s · miss: '
              +measurement.miss.contactTicks+' contact ticks · repeat matched.'
              +(level ? ' Native aisle envelope cleared.' : '')
            : '';
        this.tell({
            review:'Review saved. Place the prop only after approval.',
            approved:'Approval saved. Ready to run the contact test.',
            verified:'Placement and native contact evidence verified.'
        }[state] || 'No saved prop placement. Select a checked prop in Spatial and ask chat to place it.');
        this.controls();
    }

    controls() {
        for (const button of this.dialog.querySelectorAll('.placement-content button'))
            button.disabled = Boolean(this.parent.busy);
    }

    async approve() {
        const reviewId = this.result.reviewId;
        try {
            if (this.result.status === 'review') {
                this.result = await this.parent.call('placement-approve',{reviewId,approvalSha256:reviewId});
                this.render();
            }
            this.tell('Running the hit and miss trials, then repeating both…');
            this.result = await this.parent.call('placement-run',{reviewId});
            this.render();
            this.parent.announce(this.find('#placement-result').textContent);
            if (!this.result.review.destination) await this.save();
        } catch (error) {
            this.tell(this.errorMessage(error));
        }
    }

    async replay() {
        this.tell('Replaying native contact trials…');
        try {
            this.result = await this.parent.call('placement-replay',{reviewId:this.result.reviewId});
            this.render();
            this.tell('Fresh native replay matched the saved evidence.');
        } catch (error) {
            this.tell(this.errorMessage(error));
        }
    }

    async save(kind) {
        this.tell('Checking the scene, source and evidence before export…');
        try {
            kind ??= this.result.review.destination ? 'evidence' : undefined;
            const receipt = await this.parent.call('placement-export',{reviewId:this.result.reviewId,kind});
            this.find('#placement-file').textContent = 'Saved ' + receipt.path;
            this.tell(kind === 'play' ? 'Offline playable level saved.' : 'Scene, GLB and contact evidence saved together.');
            await this.parent.announce(this.find('#placement-status').textContent+' '+receipt.path);
        } catch (error) {
            this.tell(this.errorMessage(error));
        }
    }
}
