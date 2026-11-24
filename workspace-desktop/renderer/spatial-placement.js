
export const isPlacementRequest = text =>
    /^(?:please )?place (?:the )?(?:selected |verified )?(?:3d )?prop\b/i.test(text.trim());

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
                <dl id="placement-details"></dl>
                <p id="placement-scope" class="spatial-note" hidden>
                    The trial uses a new contact deck. The source model stays in its original scene.
                    Approval runs a moving hit and a lateral miss, repeats both, then saves the scene and evidence.
                </p>
                <p id="placement-status" role="status"></p>
                <p id="placement-result" role="status"></p>
                <p id="placement-file"></p>
                <div class="spatial-task">
                    <button id="placement-approve" class="primary" hidden>Approve and run contact test</button>
                    <button id="placement-open-scene" hidden>Open placed scene</button>
                    <button id="placement-replay" hidden>Replay collision</button>
                    <button id="placement-export" hidden>Save scene and evidence</button>
                </div>
            </div>`;
        document.body.append(this.dialog);
        this.dialog.addEventListener('close',parent.layout);
        this.find('#placement-close').onclick = () => this.dialog.close();
        this.find('#placement-approve').onclick = () => this.parent.action(() => this.approve());
        this.find('#placement-replay').onclick = () => this.parent.action(() => this.replay());
        this.find('#placement-export').onclick = () => this.parent.action(() => this.save());
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
        this.render();
        this.find('#placement-file').textContent = '';
        if (!this.dialog.open) this.dialog.showModal();
        this.parent.layout();
    }

    async openFromChat(goal) {
        this.show();
        this.tell('Preparing a review for your selected prop…');
        try {
            this.result = await this.parent.call('placement-review',{goal});
            this.render();
            this.parent.notice('Placement review is ready.');
        } catch (error) {
            this.tell(error.message.replace(/^Error invoking remote method '[^']+': (?:Error: )?/, ''));
        }
    }

    async openLatest() {
        this.show();
        this.tell('Opening your saved review…');
        try {
            this.result = await this.parent.call('placement-latest');
            this.render();
        } catch (error) {
            this.tell(error.message.replace(/^Error invoking remote method '[^']+': (?:Error: )?/, ''));
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
        const approve = this.find('#placement-approve');
        approve.hidden = !review || hasResult;
        approve.textContent = state === 'approved' ? 'Run approved contact test' : 'Approve and run contact test';
        this.find('#placement-scope').hidden = !review || hasResult;
        this.find('#placement-request').textContent = review?.goal || '';
        const details = this.find('#placement-details');
        details.replaceChildren();
        if (review) {
            const xyz = review.targetM.map(m => Number(m.toFixed(4))).join(', ');
            for (const [key,value] of [
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
        const measurement = result?.evidence?.measured;
        this.find('#placement-result').textContent = measurement
            ? 'Native hit at tick '+measurement.hit.firstContactTick+' · prop peaked at '
              +measurement.hit.maxPropPlanarSpeedMps.toFixed(3)+' m/s · miss: '
              +measurement.miss.contactTicks+' contact ticks · repeat matched.'
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
            await this.save();
        } catch (error) {
            this.tell(error.message.replace(/^Error invoking remote method '[^']+': (?:Error: )?/, ''));
        }
    }

    async replay() {
        this.tell('Replaying native contact trials…');
        try {
            this.result = await this.parent.call('placement-replay',{reviewId:this.result.reviewId});
            this.render();
            this.tell('Fresh native replay matched the saved evidence.');
        } catch (error) {
            this.tell(error.message.replace(/^Error invoking remote method '[^']+': (?:Error: )?/, ''));
        }
    }

    async save() {
        this.tell('Checking the scene, source and evidence before export…');
        try {
            const receipt = await this.parent.call('placement-export',{reviewId:this.result.reviewId});
            this.find('#placement-file').textContent = 'Saved ' + receipt.path;
            this.tell('Scene, GLB and contact evidence saved together.');
            await this.parent.announce('Verified scene and contact evidence saved to '+receipt.path);
        } catch (error) {
            this.tell(error.message.replace(/^Error invoking remote method '[^']+': (?:Error: )?/, ''));
        }
    }
}
