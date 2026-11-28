export class CourierRecoveryPanel {
    constructor(game, frameElement) {
        this.game = game;
        this.root = document.createElement('details');
        this.root.id = 'courier-recovery';
        this.root.innerHTML = `
            <summary>Courier delivery recovery</summary>
            <form class="courier-target-form">
                <label>Direction<select id="courier-recovery-direction">
                    <option value="1">Right</option><option value="-1">Left</option>
                </select></label>
                <label>Support<select id="courier-recovery-support">
                    <option value="0.065">Low friction · 0.065</option><option value="0.32">High friction · 0.32</option>
                </select></label>
                <label class="courier-check"><input id="courier-recovery-obstacle" type="checkbox" checked> Approach hurdle</label>
                <label class="courier-check"><input id="courier-recovery-disturbance" type="checkbox" checked> Interrupt delivery</label>
                <button id="courier-recovery-run" type="submit">Compare controllers</button>
                <button id="courier-recovery-replay" type="button" disabled>Replay comparison</button>
                <button id="courier-recovery-export" type="button" disabled>Export comparison</button>
            </form>
            <p id="courier-recovery-result" role="status">Compare a slower single attempt with a controller that can resume a stopped delivery.</p>`;
        frameElement.before(this.root);
        this.form = this.root.querySelector('form');
        this.status = this.find('result');
        this.replay = this.find('replay');
        this.export = this.find('export');
        this.root.ontoggle = () => {
            if (this.root.open && !this.loaded && !this.busy) this.run(async () => {
                const proof = await this.call('latest');
                this.loaded = true;
                if (!proof.id) return;
                const setup = proof.native.setup;
                this.find('direction').value = String(setup.direction);
                this.find('support').value = String(setup.supportFriction);
                this.find('obstacle').checked = setup.obstacle;
                this.find('disturbance').checked = setup.disturbance;
                this.show(proof, 'Saved comparison');
            });
        };
        this.form.onsubmit = event => {
            event.preventDefault();
            const setup = {
                direction: Number(this.find('direction').value),
                supportFriction: Number(this.find('support').value),
                obstacle: this.find('obstacle').checked,
                disturbance: this.find('disturbance').checked
            };
            this.run(() => this.compare('run', setup, 'Verified comparison'));
        };
        this.replay.onclick = () => this.run(() => this.compare('replay', {id: this.proof.id}, 'Fresh replay matched'));
        this.export.onclick = () => this.run(async () => {
            const file = await this.call('export', {id: this.proof.id});
            this.show(this.proof, 'Exported comparison');
            game.notice('Saved ' + file.path);
        });
    }

    find(name) { return this.root.querySelector('#courier-recovery-' + name); }

    call(action, args = {}) {
        return this.game.call('gameRuntime', 'courier-recovery-' + action, args);
    }

    async compare(action, args, label) {
        this.game.requireSaved();
        this.status.textContent = 'Running both controllers and checking contact, position and release…';
        try {
            this.show(await this.call(action, args), label);
        } finally {
            const frame = await this.game.call('gameRuntime', 'frame').catch(() => null);
            if (frame) {
                this.game.watching = true;
                this.game.show(frame);
            }
        }
    }

    show(proof, label) {
        this.proof = proof;
        const {baseline, adaptive} = proof.measurements;
        const original = {
            delivered: 'delivered',
            'missed-hold': 'missed the tolerance',
            'stopped-short': 'stopped ' + Number(baseline.stoppedShortBy).toFixed(3) + ' short'
        }[baseline.outcome];
        this.status.textContent = label + ' · Recovery ' + (adaptive.recoveryContactTick ? 'resumed and delivered' : 'delivered') +
            ' in ' + adaptive.ticks + ' ticks · ' + adaptive.holdTicks + ' held · Single attempt ' + original +
            ' in ' + baseline.ticks + ' ticks';
    }

    controls() {
        for (const element of this.form.elements) element.disabled = this.busy;
        this.replay.disabled = this.export.disabled = this.busy || !this.proof;
    }

    async run(action) {
        if (this.busy) return;
        this.busy = true;
        this.controls();
        try {
            await action();
        } catch (error) {
            this.status.textContent = error.message;
        } finally {
            this.busy = false;
            this.controls();
        }
    }
}
