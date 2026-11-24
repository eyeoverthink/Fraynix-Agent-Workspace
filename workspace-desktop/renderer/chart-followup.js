const $ = id => document.getElementById(id);

export class ChartFollowup {
    constructor({request, layout, announce, requireSaved}) {
        Object.assign(this, {request, layout, announce, requireSaved});
        this.dialog = $('chart-request-dialog');
        this.button = $('chart-request-open');
        this.select = $('chart-request-source');
        this.submit = $('chart-request-submit');
        this.discard = $('chart-request-discard');
        this.button.onclick = () => this.handle(async () => {
            this.retain(await this.call('state'));
            if (this.state.id) this.show();
        });
        $('chart-request-close').onclick = () => this.dialog.close();
        this.dialog.addEventListener('close', layout);
        this.select.onchange = () => this.controls();
        this.submit.onclick = () => this.handle(async () => {
            if (this.state.status === 'choose') {
                this.retain(await this.call('choose', {...this.pin(), taskId:this.select.value}));
            }
            await this.open();
        });
        this.discard.onclick = () => this.handle(async () => {
            this.retain(await this.call('discard', this.pin()));
            this.dialog.close();
        });
    }

    call(command, args = {}) {
        return this.request('chartFollowup', {command, args});
    }

    async refresh() {
        this.retain(await this.call('state'));
    }

    retain(state) {
        this.state = state;
        this.button.hidden = !state.id;
        this.button.textContent = state.status === 'verified' ? 'Last chart' : 'Chart request';
    }

    pin() {
        return {id:this.state.id, decisionSha256:this.state.decisionSha256};
    }

    async start(goal) {
        // Java matches the complete request. This hint only avoids an extra call for unrelated chat.
        if (!/\b(?:chart|plot)\b/i.test(goal) || !/\b(?:that|this|it|result)\b/i.test(goal)) return false;
        const state = await this.call('start', {request:goal});
        if (!state.matched) return false;
        this.retain(state);
        if (state.status === 'missing') {
            this.announce('Finish a verified motion summary or ramp comparison, then ask me to chart it.');
        } else if (state.status === 'choose') {
            this.announce('Which verified result should I chart?');
            this.show();
        } else {
            await this.handle(() => this.open());
        }
        return true;
    }

    show() {
        const state = this.state;
        $('chart-request-heading').textContent = state.status === 'choose' ? 'Which verified result should I chart?' : 'Your saved chart request';
        const options = state.choices.map(choice => {
            const time = new Date(choice.finishedAt).toLocaleString([], {dateStyle:'medium', timeStyle:'short'});
            return new Option(choice.label + ' · ' + time + ' · ' + choice.taskId.slice(0,8), choice.taskId);
        });
        this.select.replaceChildren(new Option('Choose a verified result', ''), ...options);
        this.select.size = state.status === 'choose' ? Math.min(5, options.length + 1) : 1;
        this.select.value = state.selected;
        $('chart-request-status').textContent = state.status === 'choose'
            ? 'Several saved results fit. Choose the one you meant.'
            : 'Fraynix will recheck the original evidence before opening the chart.';
        if (!this.dialog.open) this.dialog.showModal();
        this.controls();
        this.layout();
    }

    controls() {
        const choice = this.state.choices?.find(row => row.taskId === this.select.value);
        $('chart-request-description').textContent = choice?.goal || '';
        this.select.disabled = this.busy || this.state.status !== 'choose';
        this.submit.disabled = this.busy || !choice;
        this.submit.textContent = this.state.status === 'verified' ? 'Open verified chart' : 'Verify and open chart';
        this.discard.hidden = this.state.status === 'verified';
        this.discard.disabled = this.busy;
        this.button.disabled = this.busy;
    }

    async open() {
        this.requireSaved();
        if (this.dialog.open) this.dialog.close();
        this.layout();
        const result = await this.call('open', this.pin());
        this.retain(result);
        const source = result.choices.find(choice => choice.taskId === result.selected);
        this.announce('I charted ' + source.label + ' in the canvas. I rechecked the original source and saved the browser evidence.');
    }

    async handle(action) {
        if (this.busy) return;
        this.busy = true;
        if (this.state) this.controls();
        try {
            await action();
        } catch (error) {
            if (this.state?.id) {
                this.show();
                $('chart-request-status').textContent = error.message;
            }
            this.announce(error.message);
        } finally {
            this.busy = false;
            if (this.state) this.controls();
        }
    }
}
