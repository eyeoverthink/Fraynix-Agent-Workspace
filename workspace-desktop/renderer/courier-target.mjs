export class CourierTargetPanel {
    constructor(game, frameElement) {
        this.game = game;
        this.root = document.createElement('details');
        this.root.id = 'courier-target';
        this.root.innerHTML = `
            <summary>Courier target delivery</summary>
            <form class="courier-target-form">
                <label>Direction<select id="courier-target-direction">
                    <option value="1">Right</option><option value="-1">Left</option>
                </select></label>
                <label>Distance<input id="courier-target-distance" type="number" min="0.30" max="1.00" step="0.01" value="0.65" required></label>
                <label>Mass<input id="courier-target-mass" type="number" min="0.35" max="1.30" step="0.01" value="0.78" required></label>
                <label>Friction<input id="courier-target-friction" type="number" min="0.06" max="0.75" step="0.01" value="0.40" required></label>
                <button id="courier-target-run" type="submit">Push to target</button>
                <button id="courier-target-replay" type="button" disabled>Replay saved delivery</button>
                <button id="courier-target-export" type="button" disabled>Export delivery evidence</button>
            </form>
            <p id="courier-target-result" role="status">Choose the setup. The Courier must back away and leave the crate on the marker for one second.</p>`;
        frameElement.before(this.root);
        this.form = this.root.querySelector('form');
        this.status = this.find('result');
        this.replay = this.find('replay');
        this.export = this.find('export');
        this.root.ontoggle = () => {
            if (this.root.open && !this.loaded && !this.busy) this.run(async () => {
                const proof = await this.call('latest');
                this.loaded = true;
                if (proof.id) {
                    const setup = proof.native.request;
                    this.find('direction').value = String(Math.sign(setup.distance));
                    this.find('distance').value = Math.abs(setup.distance);
                    this.find('mass').value = setup.mass;
                    this.find('friction').value = setup.friction;
                    this.show(proof, 'Saved delivery');
                }
            });
        };
        this.form.onsubmit = event => {
            event.preventDefault();
            if (!this.form.reportValidity()) return;
            const setup = {
                distance: Number(this.find('direction').value) * this.find('distance').valueAsNumber,
                mass: this.find('mass').valueAsNumber,
                friction: this.find('friction').valueAsNumber
            };
            this.run(() => this.deliver('run', setup, 'Verified delivery'));
        };
        this.replay.onclick = () => this.run(() => this.deliver('replay', {id: this.proof.id}, 'Fresh replay matched'));
        this.export.onclick = () => this.run(async () => {
            const file = await this.call('export', {id: this.proof.id});
            this.show(this.proof, 'Exported delivery evidence');
            game.notice('Saved ' + file.path);
        });
    }

    find(name) {
        return this.root.querySelector('#courier-target-' + name);
    }

    call(command, args = {}) {
        return this.game.call('gameRuntime', 'courier-target-' + command, args);
    }

    async deliver(command, args, label) {
        this.game.requireSaved();
        this.status.textContent = 'Running the native delivery and checking the crate after release…';
        try {
            this.show(await this.call(command, args), label);
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
        const m = proof.measurements;
        this.status.textContent = label + ' · Crate X ' + m.finalX.toFixed(3) + ' / target ' +
            m.targetX.toFixed(3) + ' · Released at tick ' + m.releaseTick +
            ' · ' + m.holdTicks + ' contact-free ticks · Worst error ' + m.maxError.toFixed(4);
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
