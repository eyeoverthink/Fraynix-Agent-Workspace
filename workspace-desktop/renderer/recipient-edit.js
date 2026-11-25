import {drawPlacementLayout} from './spatial-placement.js';

export class RecipientEditPanel {
    constructor(spatial) {
        this.spatial = spatial;
        const dialog = document.createElement('dialog');
        dialog.id = 'recipient-edit-dialog';
        dialog.setAttribute('aria-labelledby','recipient-edit-title');
        dialog.innerHTML = `
            <div class="panel-head"><h2 id="recipient-edit-title">Edit the recipient copy</h2>
                <button id="recipient-edit-close">Close</button></div>
            <div class="placement-content">
                <p id="recipient-copy-summary"></p>
                <button id="recipient-open-evidence" type="button">Open original evidence</button>
                <form id="recipient-edit-form" class="spatial-task">
                    <label>Prop X · m<input name="xM" type="number" min="-20" max="20" step="any" required></label>
                    <label>Prop Z · m<input name="zM" type="number" min="-20" max="20" step="any" required></label>
                    <label>Yaw · °<input name="yawDeg" type="number" min="-180" max="180" step="any" required></label>
                    <button type="submit">Preview local edit</button>
                </form>
                <p id="recipient-edit-delta"></p>
                <canvas id="recipient-edit-preview" width="530" height="205" aria-label="Edited prop and retained walking aisle" hidden></canvas>
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
        this.fields(prop.position,this.source.recipientEdit?.review.yawDeg ?? link.initialYawDeg);
        this.render();
        this.dialog.showModal();
        this.spatial.layout();
        try {
            this.result = await this.spatial.call('recipient-edit-latest',{sceneId:this.source.id});
            if (this.result.review) this.fields(this.result.review.targetM,this.result.review.yawDeg);
            this.render();
        } catch (error) { this.fail(error); }
    }

    fields(position,yaw) {
        const fields = this.find('#recipient-edit-form').elements;
        fields.namedItem('xM').value = position[0];
        fields.namedItem('zM').value = position[2];
        fields.namedItem('yawDeg').value = yaw;
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
        const values = Object.fromEntries(['xM','zM','yawDeg'].map(name=>[name,fields.namedItem(name).valueAsNumber]));
        try {
            this.result = await this.spatial.call('recipient-edit-review',{sceneId:this.source.id,...values});
            this.render();
        } catch (error) { this.fail(error); }
    }

    async approve() {
        const {reviewId,status} = this.result;
        try {
            if (status === 'review') {
                this.result = await this.spatial.call('recipient-edit-approve',{reviewId,approvalSha256:reviewId});
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
        preview.hidden = !layout;
        if (layout) drawPlacementLayout(preview,layout);
        this.find('#recipient-edit-delta').textContent = review
            ? 'X, Z · '+[review.fromM[0],review.fromM[2]].join(', ')+' → '
                +[review.targetM[0],review.targetM[2]].join(', ')+' m · '+review.yawDeg+'° yaw'
            : '';
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
            review:'Preview saved. Approve the exact pose to run the local checks twice.',
            approved:'Approval saved. Run the checks to finish this revision.',
            superseded:'A newer revision exists. Open it to continue.',
            verified:receipt && 'Local revision verified · hit at tick '+receipt.measured.hit.firstContactTick
                +' · '+receipt.measured.miss.contactTicks+' miss contacts · '
                +receipt.measured.routeLengthM.toFixed(2)+' m aisle clear.'
        }[status] || 'Set a position and yaw for the placed prop. The floor, colliders and aisle are included in the checks.');
        this.controls();
    }

    controls() {
        for (const field of this.dialog.querySelectorAll('.placement-content input, .placement-content button'))
            field.disabled = Boolean(this.spatial.busy);
    }
}
