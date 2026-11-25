export class RecipientMissionPanel {
    constructor(spatial) {
        this.spatial = spatial;
        const dialog = document.createElement('dialog');
        dialog.id = 'recipient-mission-dialog';
        dialog.setAttribute('aria-labelledby','recipient-mission-title');
        dialog.innerHTML = `
            <div class="panel-head"><h2 id="recipient-mission-title">Collider mission</h2>
                <button id="recipient-mission-close">Close</button></div>
            <div class="placement-content">
                <p id="recipient-mission-status" role="status"></p>
                <div id="recipient-mission-record" hidden>
                    <dl>
                        <dt>Chat request</dt><dd id="recipient-mission-request"></dd>
                        <dt>Scene and proposal</dt><dd id="recipient-mission-scene"></dd>
                        <dt>Measured evidence</dt><dd id="recipient-mission-evidence"></dd>
                        <dt>Approval</dt><dd id="recipient-mission-approval"></dd>
                        <dt>Committed revision</dt><dd id="recipient-mission-revision"></dd>
                        <dt>Export</dt><dd id="recipient-mission-export-status"></dd>
                    </dl>
                    <details id="recipient-mission-approaches" hidden>
                        <summary>Review the five approaches</summary><ol></ol>
                    </details>
                    <div class="spatial-task">
                        <button id="recipient-mission-continue">Continue review</button>
                        <button id="recipient-mission-export">Save verified export</button>
                    </div>
                </div>
            </div>`;
        document.body.append(dialog);
        this.dialog = dialog;
        dialog.addEventListener('close',spatial.layout);
        this.find('#recipient-mission-close').onclick = () => dialog.close();
        this.find('#recipient-mission-continue').onclick = () => spatial.action(async () => {
            dialog.close();
            await spatial.open(this.record.parentSceneId);
            await spatial.recipient.open();
        });
        this.find('#recipient-mission-export').onclick = () => spatial.action(async () => {
            const button = this.find('#recipient-mission-export');
            button.disabled = true;
            try {
                await spatial.call('recipient-mission-export',{reviewId:this.record.reviewId});
                await this.load();
            } catch (error) {
                this.find('#recipient-mission-status').textContent = error.message;
            } finally { button.disabled = false; }
        });
    }

    find(selector) { return this.dialog.querySelector(selector); }

    async open() {
        this.find('#recipient-mission-record').hidden = true;
        this.find('#recipient-mission-status').textContent = 'Checking the saved mission…';
        this.dialog.showModal();
        this.spatial.layout();
        await this.load();
    }

    async load() {
        try {
            this.record = await this.spatial.call('recipient-mission-latest');
            this.render();
        } catch (error) {
            this.find('#recipient-mission-record').hidden = true;
            this.find('#recipient-mission-status').textContent = error.message;
        }
    }

    render() {
        const {request,sceneTitle,review,comparison,approvalConfirmed,revision,export:artifact,exportAvailable,superseded}
            = this.record;
        const status = this.find('#recipient-mission-status');
        this.find('#recipient-mission-record').hidden = !request;
        status.textContent = request ? superseded ? 'This pending proposal was replaced.' : ''
            : 'Start a collider comparison from Chat to create a mission.';
        if (!request) return;
        this.find('#recipient-mission-request').textContent = request;
        const size = review.collider.proposedM;
        this.find('#recipient-mission-scene').textContent = sceneTitle+' · '+size[0]+' × '+size[2]+' m collider';
        const summary = comparison?.summary;
        this.find('#recipient-mission-evidence').textContent = summary
            ? (5-summary.needsCheck)+' measured · '+summary.needsCheck+' need another check'
            : 'Comparison pending';
        this.find('#recipient-mission-approval').textContent = approvalConfirmed ? 'Exact proposal approved with its evidence'
            : 'Awaiting explicit approval in this mission';
        this.find('#recipient-mission-revision').textContent = revision
            ? 'Native hit, miss, aisle and repeat checks passed' : 'Awaiting native commit checks';
        const exported = this.find('#recipient-mission-export-status');
        exported.textContent = artifact ? exportAvailable ? 'Verified · exports/'+artifact.fileName
            : 'Saved file changed or is missing. Save the verified export again.' : 'Awaiting export';
        exported.title = exportAvailable ? artifact.sha256 : '';
        const details = this.find('#recipient-mission-approaches');
        details.hidden = !comparison;
        const list = details.querySelector('ol');
        list.replaceChildren();
        for (const [index,item] of (comparison?.cases || []).entries()) {
            const entry = document.createElement('li');
            const setup = comparison.binding.cases[index];
            const outcome = item.reason ? item.reason
                : 'First contact '+item.original.hit.firstContactTick+' → '+item.resized.hit.firstContactTick;
            entry.textContent = setup.headingDeg+'° / '+Math.round(setup.lateralOffsetM*100)+' cm · '+outcome;
            list.append(entry);
        }
        this.find('#recipient-mission-continue').hidden = Boolean(revision || superseded);
        this.find('#recipient-mission-export').hidden = !revision || !approvalConfirmed || Boolean(exportAvailable);
    }
}
