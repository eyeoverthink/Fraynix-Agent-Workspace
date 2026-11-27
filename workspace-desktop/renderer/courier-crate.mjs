export class CourierCratePanel {
    constructor(game, frameElement) {
        this.game = game;
        this.root = document.createElement('details');
        this.root.id = 'courier-crate';
        this.frameElement = frameElement;
        this.root.innerHTML = `
            <summary>Courier crate test</summary>
            <div class="runtime-controls">
                <button id="courier-crate-open">Open practice scene</button>
                <button id="courier-crate-test" disabled>Run paired test</button>
                <button id="courier-crate-replay" disabled>Replay saved test</button>
                <button id="courier-crate-export" disabled>Export evidence</button>
            </div>
            <p id="courier-crate-live" hidden></p>
            <p id="courier-crate-result" role="status">Open the practice scene to drive the Courier or compare a push with an approach that stops short.</p>`;
        frameElement.before(this.root);
        this.result = this.find('result');
        this.live = this.find('live');
        this.open = this.find('open');
        this.test = this.find('test');
        this.replay = this.find('replay');
        this.export = this.find('export');
        this.root.ontoggle = () => {
            if (this.root.open && !this.loaded && !this.busy) this.run(async () => {
                const proof = await this.call('latest');
                this.loaded = true;
                if (proof.id) this.show(proof, 'Saved result');
            });
        };
        this.open.onclick = () => this.run(async () => {
            game.requireSaved();
            this.result.textContent = 'Opening the native practice scene…';
            this.openFrame(await this.call('open'));
            if (!this.proof) this.result.textContent = 'Practice scene ready. Use the movement controls or run the paired test.';
            else this.show(this.proof, 'Saved result');
        });
        this.test.onclick = () => this.run(async () => {
            game.requireSaved();
            this.result.textContent = 'Testing two fresh scenes with the same approach…';
            this.show(await this.call('test'), 'Verified');
            this.openFrame(await game.call('gameRuntime', 'frame'));
        });
        this.replay.onclick = () => this.run(async () => {
            game.requireSaved();
            this.result.textContent = 'Rebuilding and replaying both trials…';
            this.show(await this.call('replay', {id: this.proof.id}), 'Fresh replay matched');
            this.openFrame(await game.call('gameRuntime', 'frame'));
        });
        this.export.onclick = () => this.run(async () => {
            const file = await this.call('export', {id: this.proof.id});
            this.show(this.proof, 'Exported evidence');
            game.notice('Saved ' + file.path);
        });
    }

    find(id) {
        return this.root.querySelector('#courier-crate-' + id);
    }

    call(command, args = {}) {
        return this.game.call('gameRuntime', 'courier-' + command, args);
    }

    openFrame(frame) {
        this.game.watching = true;
        this.game.show(frame);
    }

    show(proof, label) {
        const m = proof.measurements;
        this.proof = proof;
        this.result.textContent = label + ' · Shared approach through tick ' + m.sharedApproachThroughTick +
            ' · Contact ' + m.firstContactTick + ' · First crate motion ' + m.firstMotionTick +
            ' · Push ' + m.finalPushDx.toFixed(3) + ' game units · Control ' + m.controlMaxAbsDx.toFixed(6);
    }

    frame(frame) {
        const current = frame?.state?.courierCrate;
        this.sceneActive = Boolean(current);
        this.frameElement.toggleAttribute('data-courier', this.sceneActive);
        this.live.hidden = !current;
        if (current) {
            this.live.textContent = 'Crate X ' + current.crateX.toFixed(3) + ' · Horizontal speed ' +
                current.crateVx.toFixed(3) + ' · ' + current.beginContacts + ' contact(s)';
        }
        this.controls();
    }

    controls() {
        this.open.disabled = this.busy;
        this.test.disabled = this.busy || !this.sceneActive;
        this.replay.disabled = this.export.disabled = this.busy || !this.proof;
    }

    async run(action) {
        if (this.busy) return;
        this.busy = true;
        this.controls();
        try {
            await action();
        } catch (error) {
            this.result.textContent = error.message;
        } finally {
            this.busy = false;
            this.controls();
        }
    }
}
