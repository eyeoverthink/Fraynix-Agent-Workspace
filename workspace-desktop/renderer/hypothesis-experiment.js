const $ = id => document.getElementById(id);

export const isHypothesisExperimentRequest = text =>
    /^(?:what observation would distinguish these two explanations|test two explanations in game lab|resume (?:my |the )?last (?:game lab )?hypothesis experiment)[.!?]?$/i.test(text.trim());

export class HypothesisExperimentPanel {
    constructor({call, layout, announce, getModel}) {
        Object.assign(this, {call, layout, announce, getModel});
        this.dialog = $('hypothesis-dialog');
        this.dialog.addEventListener('close', layout);
        $('hypothesis-close').onclick = () => this.dialog.close();
        $('hypothesis-form').onsubmit = event => {
            event.preventDefault();
            this.propose();
        };
        $('hypothesis-approve').onclick = () => this.approve();
    }

    show() {
        if (!this.dialog.open) this.dialog.showModal();
        this.layout();
    }

    async fromChat(request) {
        try {
            const result = await this.call('hypothesis-chat', {request});
            if (result.experiment?.result) return this.announceResult(result.experiment);
            if (result.experiment) return this.review(result.experiment);
            this.experiment = null;
            $('hypothesis-review').hidden = true;
            $('hypothesis-form').hidden = false;
            $('hypothesis-a').value = '';
            $('hypothesis-b').value = '';
            $('hypothesis-status').textContent = '';
            this.show();
        } catch (error) { this.announce(error.message); }
    }

    async propose() {
        if (this.busy) return;
        this.setBusy(true);
        try {
            const model = this.getModel();
            if (!model) throw Error('Select a local model in Chat first.');
            $('hypothesis-status').textContent = 'The local model is proposing one observation.';
            const result = await this.call('hypothesis-propose', {
                a: $('hypothesis-a').value, b: $('hypothesis-b').value, model
            });
            this.review(result);
        } catch (error) { $('hypothesis-status').textContent = error.message; }
        finally { this.setBusy(false); }
    }

    review(experiment) {
        this.experiment = experiment;
        const {review} = experiment;
        const {proposal} = review;
        $('hypothesis-form').hidden = true;
        $('hypothesis-review').hidden = false;
        $('hypothesis-probe').textContent = proposal.label;
        $('hypothesis-rationale').textContent = proposal.reason;
        for (const key of ['a','b']) {
            const {low, high} = proposal[key];
            $('hypothesis-claim-'+key).textContent = review[key];
            $('hypothesis-range-'+key).textContent = low + ' to ' + high + ' game units';
        }
        const total = review.sessions * (review.settleTicks + proposal.ticks);
        $('hypothesis-budget').textContent = review.sessions + ' isolated Game Lab sessions · ' + total
            + ' simulated ticks total, including the neutral settle';
        $('hypothesis-source').textContent = review.course + ' · ' + review.model;
        $('hypothesis-source-hash').textContent = review.source.manifestSha256;
        $('hypothesis-approve').textContent = experiment.attempts ? 'Resume approved experiment' : 'Approve and run';
        $('hypothesis-status').textContent = experiment.problem || '';
        this.show();
    }

    async approve() {
        if (this.busy || !this.experiment) return;
        this.setBusy(true);
        $('hypothesis-status').textContent = 'Running the observation and its fresh replay.';
        try {
            const {id, reviewSha256} = this.experiment;
            const result = await this.call('hypothesis-approve', {id, reviewSha256});
            this.dialog.close();
            this.layout();
            this.announceResult(result);
        } catch (error) { $('hypothesis-status').textContent = error.message; }
        finally { this.setBusy(false); }
    }

    announceResult({result}) {
        this.announce('Game Lab measured ' + result.primary + ' game units and verified a fresh replay. A — '
            + result.a + '. B — ' + result.b + '.');
    }

    setBusy(busy) {
        this.busy = busy;
        $('hypothesis-propose').disabled = busy;
        $('hypothesis-approve').disabled = busy;
    }
}
