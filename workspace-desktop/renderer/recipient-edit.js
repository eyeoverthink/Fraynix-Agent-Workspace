import {drawPlacementLayout} from './spatial-placement.js';

export const isRecipientApproachRequest = text =>
    /^(?:please )?(?:compare|test|check) (?:the )?(?:proposed|pending) collider(?: (?:across|using) (?:the )?five (?:probe )?approaches)?[.!?]?$/i.test(text.trim());

function drawCollider(canvas, geometry, previous, proposed) {
    const ctx = canvas.getContext('2d');
    const {width,height} = canvas;
    const scale = Math.min((width-36)/Math.max(previous[0],proposed[0]),
        (height-34)/Math.max(previous[2],proposed[2]));
    const x = value => width/2 + value*scale;
    const z = value => height/2 + value*scale;
    ctx.fillStyle = '#102429';
    ctx.fillRect(0,0,width,height);
    const position = geometry.getAttribute('position');
    const indices = geometry.index.array;
    ctx.fillStyle = '#90a3b8';
    for (let i=0;i<indices.length;i+=3) {
        ctx.beginPath();
        for (let j=0;j<3;j++) {
            const vertex = indices[i+j];
            const px = x(position.getX(vertex)), pz = z(position.getZ(vertex));
            if (j===0) ctx.moveTo(px,pz);
            else ctx.lineTo(px,pz);
        }
        ctx.closePath();
        ctx.fill();
    }
    ctx.lineWidth = 2;
    for (const [box,color,dashes] of [[previous,'#d2e0eb',[5,5]],[proposed,'#f0b176',[]]]) {
        ctx.strokeStyle = color;
        ctx.setLineDash(dashes);
        ctx.strokeRect(x(-box[0]/2),z(-box[2]/2),box[0]*scale,box[2]*scale);
    }
}

export class RecipientEditPanel {
    constructor(spatial, geometryFrom) {
        this.spatial = spatial;
        this.geometryFrom = geometryFrom;
        const dialog = document.createElement('dialog');
        dialog.id = 'recipient-edit-dialog';
        dialog.setAttribute('aria-labelledby','recipient-edit-title');
        dialog.innerHTML = `
            <div class="panel-head"><h2 id="recipient-edit-title">Edit the recipient copy</h2>
                <button id="recipient-edit-close">Close</button></div>
            <div class="placement-content">
                <p id="recipient-copy-summary"></p>
                <button id="recipient-open-evidence" type="button">Open original evidence</button>
                <form id="recipient-edit-form">
                    <label>Edit <select name="kind"><option value="pose">Prop position and yaw</option>
                        <option value="collider">Collider width and depth</option></select></label>
                    <div id="recipient-pose-fields" class="spatial-task">
                        <label>Prop X · m<input name="xM" type="number" min="-20" max="20" step="any" required></label>
                        <label>Prop Z · m<input name="zM" type="number" min="-20" max="20" step="any" required></label>
                        <label>Yaw · °<input name="yawDeg" type="number" min="-180" max="180" step="any" required></label>
                    </div>
                    <div id="recipient-collider-fields" class="spatial-task" hidden>
                        <label>Width (X) · m<input name="colliderWidthM" type="number" min=".05" max="40" step="any" required></label>
                        <label>Depth (Z) · m<input name="colliderDepthM" type="number" min=".05" max="40" step="any" required></label>
                    </div>
                    <button type="submit">Preview local edit</button>
                </form>
                <p id="recipient-edit-delta"></p>
                <div class="recipient-preview-grid">
                    <figure id="recipient-collider-figure" hidden>
                        <canvas width="440" height="230" aria-label="Top view of the visual mesh and both collider footprints"></canvas>
                        <figcaption>Mesh · dashed current box · amber proposed box</figcaption>
                    </figure>
                    <figure id="recipient-aisle-figure" hidden>
                        <canvas id="recipient-edit-preview" width="530" height="205"
                            aria-label="Edited prop and retained walking aisle"></canvas>
                        <figcaption>Teal walking aisle · amber prop · gray colliders</figcaption>
                    </figure>
                </div>
                <div id="recipient-approach-review" hidden>
                    <button id="recipient-compare">Test five approaches before approval</button>
                    <p id="recipient-approach-status" role="status"></p>
                    <div id="recipient-approach-data" hidden>
                        <p id="recipient-approach-summary"></p>
                        <table>
                            <thead><tr><th scope="col">Heading / offset</th><th scope="col">First contact</th>
                                <th scope="col">Timing check</th></tr></thead>
                            <tbody id="recipient-approach-rows"></tbody>
                        </table>
                    </div>
                </div>
                <p id="recipient-edit-status" role="status"></p>
                <div class="spatial-task">
                    <button id="recipient-edit-approve" class="primary" hidden>Approve and verify local edit</button>
                    <button id="recipient-edit-open" hidden>Open local revision</button>
                    <button id="recipient-edit-export" hidden>Export verified revision</button>
                </div>
                <p id="recipient-edit-export-path" role="status"></p>
            </div>`;
        document.body.append(dialog);
        this.dialog = dialog;
        dialog.addEventListener('close',spatial.layout);
        this.find('#recipient-edit-close').onclick = () => dialog.close();
        this.find('#recipient-open-evidence').onclick = () => spatial.action(async () => {
            dialog.close();
            await spatial.exchange.open(this.source.id);
        });
        this.find('#recipient-edit-form').oninput = () => {
            this.result = null;
            this.render();
        };
        this.find('#recipient-edit-form').onsubmit = event => {
            event.preventDefault();
            spatial.action(() => this.review());
        };
        this.find('#recipient-compare').onclick = () => spatial.action(() => this.compare());
        this.find('#recipient-edit-approve').onclick = () => spatial.action(() => this.approve());
        this.find('#recipient-edit-export').onclick = () => spatial.action(() => this.exportRevision());
        this.find('#recipient-edit-open').onclick = () => spatial.action(async () => {
            dialog.close();
            await spatial.open(this.result.sceneId);
        });
    }

    find(selector) { return this.dialog.querySelector(selector); }

    tell(message) { this.find('#recipient-edit-status').textContent = message; }

    async open() {
        this.source = this.spatial.scene;
        this.result = null;
        const link = this.source.recipientBranch;
        const prop = this.source.objects.find(item => item.sourceId === link.propSourceId);
        const attachment = link.inheritedRevisions
            ? 'Inherited approved edits: '+link.inheritedRevisions
            : 'Original archive '+link.archiveSha256.slice(0,12)+'…';
        this.find('#recipient-copy-summary').textContent = 'Local revision '+this.source.recipientRevision+' · '+prop.label
            +' · '+attachment;
        this.find('#recipient-open-evidence').textContent = link.inheritedRevisions ? 'Open inherited evidence' : 'Open original evidence';
        const fields = this.find('#recipient-edit-form').elements;
        fields.namedItem('kind').value = 'pose';
        this.poseFields(prop.position,this.source.recipientEdit?.review.yawDeg ?? link.initialYawDeg);
        this.colliderFields(prop.size);
        this.visualMesh = this.source.visualMeshes?.[prop.visualMesh];
        fields.namedItem('kind').querySelector('[value=collider]').disabled = !this.visualMesh;
        this.render();
        this.dialog.showModal();
        this.spatial.layout();
        try {
            this.result = await this.spatial.call('recipient-edit-latest',{sceneId:this.source.id});
            const {review} = this.result;
            if (review?.collider) {
                fields.namedItem('kind').value = 'collider';
                this.colliderFields(review.collider.proposedM);
            } else if (review) this.poseFields(review.targetM,review.yawDeg);
            this.render();
        } catch (error) { this.fail(error); }
    }

    poseFields(position,yaw) {
        const fields = this.find('#recipient-edit-form').elements;
        fields.namedItem('xM').value = position[0];
        fields.namedItem('zM').value = position[2];
        fields.namedItem('yawDeg').value = yaw;
    }

    colliderFields(size) {
        const fields = this.find('#recipient-edit-form').elements;
        fields.namedItem('colliderWidthM').value = size[0];
        fields.namedItem('colliderDepthM').value = size[2];
    }

    async exportRevision() {
        const sceneId = this.result?.status === 'verified' ? this.result.sceneId : this.source.id;
        const output = this.find('#recipient-edit-export-path');
        try {
            const saved = await this.spatial.call('recipient-edit-export',{sceneId});
            const filename = saved.path.split(/[\\/]/).at(-1);
            output.textContent = 'Saved '+filename+' in this project’s exports folder.';
            output.title = saved.path;
            this.spatial.notice('Saved the verified revision and its original evidence.');
        } catch (error) { this.fail(error); }
    }

    async review() {
        const fields = this.find('#recipient-edit-form').elements;
        const inputNames = fields.namedItem('kind').value === 'collider'
            ? ['colliderWidthM','colliderDepthM'] : ['xM','zM','yawDeg'];
        const values = Object.fromEntries(inputNames.map(name=>[name,fields.namedItem(name).valueAsNumber]));
        try {
            this.result = await this.spatial.call('recipient-edit-review',{sceneId:this.source.id,...values});
            this.render();
        } catch (error) { this.fail(error); }
    }

    async compareFromChat() {
        if (!this.spatial.scene?.recipientBranch || this.spatial.worldMode) await this.spatial.open();
        if (!this.spatial.scene?.recipientBranch || this.spatial.worldMode) {
            this.spatial.announce('Open an editable recipient copy in Spatial, then save a collider preview to compare.');
            return;
        }
        this.spatial.activate('spatial');
        await this.open();
        if (this.result?.status === 'review' && this.result.review?.collider) await this.compare();
        else this.tell('Save a pending collider preview before comparing it.');
    }

    async compare() {
        const status = this.find('#recipient-approach-status');
        status.textContent = 'Testing the current and proposed boxes in isolated simulations…';
        try {
            this.result = await this.spatial.call('recipient-edit-compare',{reviewId:this.result.reviewId});
            this.render();
        } catch (error) {
            status.textContent = error.message.replace(/^Error invoking remote method '[^']+': (?:Error: )?/, '');
        }
    }

    async approve() {
        const {reviewId,status} = this.result;
        try {
            if (status === 'review') {
                const approval = {reviewId,approvalSha256:reviewId};
                if (this.result.comparison) approval.comparisonSha256 = this.result.comparison.sha256;
                this.result = await this.spatial.call('recipient-edit-approve',approval);
                this.render();
            }
            this.tell('Running the hit, miss and aisle checks twice…');
            this.result = await this.spatial.call('recipient-edit-run',{reviewId});
            this.render();
        } catch (error) { this.fail(error); }
    }

    fail(error) { this.tell(error.message.replace(/^Error invoking remote method '[^']+': (?:Error: )?/,'')); }

    render() {
        const {review,layout,receipt,status} = this.result || {};
        this.find('#recipient-edit-form').hidden = ['verified','superseded'].includes(status);
        const preview = this.find('#recipient-edit-preview');
        this.find('#recipient-aisle-figure').hidden = !layout;
        if (layout) drawPlacementLayout(preview,layout);
        const collider = review?.collider;
        const figure = this.find('#recipient-collider-figure');
        figure.hidden = !collider;
        const dimensions = box => [box[0],box[2]].map(m=>Number(m.toFixed(4))).join(' × ');
        this.find('#recipient-edit-delta').textContent = collider
            ? 'Width × depth · '+dimensions(collider.previousM)+' → '+dimensions(collider.proposedM)+' m'
            : review ? 'X, Z · '+[review.fromM[0],review.fromM[2]].join(', ')+' → '
                +[review.targetM[0],review.targetM[2]].join(', ')+' m · '+review.yawDeg+'° yaw'
            : '';
        if (collider) {
            const geometry = this.geometryFrom(this.visualMesh);
            try { drawCollider(figure.querySelector('canvas'),geometry,collider.previousM,collider.proposedM); }
            finally { geometry.dispose(); }
        }
        this.renderApproaches(collider,status);
        const approve = this.find('#recipient-edit-approve');
        approve.hidden = !['review','approved'].includes(status);
        approve.textContent = status === 'approved' ? 'Run approved edit checks' : 'Approve and verify local edit';
        this.find('#recipient-edit-open').hidden = !['verified','superseded'].includes(status);
        this.find('#recipient-edit-export').hidden = status !== 'verified'
            && (this.source.recipientRevision === 0 || Boolean(status));
        const exportPath = this.find('#recipient-edit-export-path');
        exportPath.textContent = '';
        exportPath.title = '';
        this.tell({
            review:'Preview saved. Approve the exact '+(collider?'collider':'pose')+' to run the local checks twice.',
            approved:'Approval saved. Run the checks to finish this revision.',
            superseded:'A newer revision exists. Open it to continue.',
            verified:receipt && 'Local revision verified · hit at tick '+receipt.measured.hit.firstContactTick
                +' · '+receipt.measured.miss.contactTicks+' miss contacts · '
                +receipt.measured.routeLengthM.toFixed(2)+' m aisle clear.'
        }[status] || (this.find('[name=kind]').value === 'collider'
            ? 'Adjust width and depth by up to 0.10 m each. The box must enclose the mesh and preserve the aisle.'
            : 'Set a position and yaw for the placed prop. The floor, colliders and aisle are included in the checks.'));
        this.controls();
    }

    renderApproaches(collider,status) {
        const pending = collider && ['review','approved'].includes(status);
        this.find('#recipient-approach-review').hidden = !pending;
        this.find('#recipient-compare').hidden = status !== 'review';
        const comparison = this.result?.comparison;
        this.find('#recipient-approach-data').hidden = !comparison;
        this.find('#recipient-approach-status').textContent = comparison ? 'Saved comparison for this exact preview.'
            : this.result?.comparisonUnavailable ? 'The saved comparison changed. Run a fresh check.'
            : 'Check whether first-contact timing stays within one tick.';
        if (!comparison) return;
        const {summary,binding,cases} = comparison;
        this.find('#recipient-approach-summary').textContent = summary.withinTolerance+' within one tick · '
            +summary.earlier+' earlier · '+summary.later+' later · '+summary.needsCheck+' need another check';
        const rows = this.find('#recipient-approach-rows');
        rows.replaceChildren();
        for (const [i,data] of cases.entries()) {
            const approach = binding.cases[i];
            const row = rows.insertRow();
            row.insertCell().textContent = approach.headingDeg+'° / '+Math.round(100*approach.lateralOffsetM)+' cm';
            if (data.reason) {
                row.insertCell().textContent = 'Unverified';
                row.insertCell().textContent = data.reason;
                continue;
            }
            row.insertCell().textContent = data.original.hit.firstContactTick+' → '+data.resized.hit.firstContactTick;
            const lead = data.result.resizedEarlierByTicks;
            row.insertCell().textContent = Math.abs(lead)<=summary.toleranceTicks ? 'Within one tick'
                : Math.abs(lead)+' ticks '+(lead>0 ? 'earlier' : 'later');
        }
    }

    controls() {
        const busy = Boolean(this.spatial.busy);
        const colliderMode = this.find('[name=kind]').value === 'collider';
        this.find('#recipient-pose-fields').hidden = colliderMode;
        this.find('#recipient-collider-fields').hidden = !colliderMode;
        for (const field of this.dialog.querySelectorAll('.placement-content :is(input,select,button)'))
            field.disabled = busy || Boolean(field.closest('#recipient-pose-fields[hidden],#recipient-collider-fields[hidden]'));
    }
}
