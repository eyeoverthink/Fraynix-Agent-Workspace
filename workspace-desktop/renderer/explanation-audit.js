const $ = id => document.getElementById(id);
const auditRequest = /^(?:(?:can|could|would) you )?(?:please )?(?:(?:audit|test) (?:an?|a proposed) explanation for (?:that briefing|the last chart briefing)|(?:resume|open|recheck) (?:my |the )?last explanation audit)[.!?]?$/i;

export const isExplanationAuditRequest = text => auditRequest.test(text.trim());

export class ExplanationAuditPanel {
    constructor({call, layout, announce}) {
        Object.assign(this, {call, layout, announce});
        this.dialog = $('explanation-audit-dialog');
        this.dialog.addEventListener('close', layout);
        $('audit-close').onclick = () => this.dialog.close();
        $('audit-claim').onchange = () => this.updateMargin();
        $('audit-file').onchange = () => this.updateButton();
        $('audit-form').onsubmit = event => {
            event.preventDefault();
            this.submit();
        };
    }

    async fromChat(request) {
        try {
            const result = await this.call('explanation-audit-chat', {request});
            if (!result.matched) throw Error('Try “Audit a proposed explanation for that briefing.”');
            if (result.plan) this.open(result.plan);
            else this.report(result);
        } catch (error) {
            this.announce(error.message);
        }
    }

    open(plan) {
        this.plan = plan;
        $('audit-source').textContent = 'Briefing for “' + plan.name + '”';
        $('audit-claim').replaceChildren(...plan.claims.map(claim => new Option(claim.sentence, String(claim.index))));
        $('audit-columns').textContent = plan.columns.join(',');
        const proposal = plan.proposal;
        if (proposal) $('audit-claim').value = String(proposal.claimIndex);
        $('audit-hypothesis').value = proposal?.hypothesis ?? '';
        $('audit-assumptions').value = proposal?.assumptions ?? '';
        $('audit-margin').value = proposal?.margin ?? '';
        $('audit-file').value = '';
        $('audit-status').textContent = proposal ? 'Draft saved. Attach separate trial observations to run the audit.' : '';
        this.updateMargin();
        this.updateButton();
        this.show();
    }

    show() {
        if (!this.dialog.open) this.dialog.showModal();
        this.layout();
    }

    updateMargin() {
        const claim = this.plan.claims.find(item => String(item.index) === $('audit-claim').value);
        $('audit-margin-label').textContent = 'Allowed difference from original change (' + claim.unit + ')';
    }

    updateButton() {
        $('audit-submit').textContent = $('audit-file').files.length ? 'Run audit' : 'Save draft';
    }

    async submit() {
        if (this.busy || !this.plan) return;
        this.busy = true;
        $('audit-submit').disabled = true;
        $('audit-status').textContent = '';
        try {
            const input = {
                id: this.plan.id,
                claimIndex: Number($('audit-claim').value),
                hypothesis: $('audit-hypothesis').value,
                assumptions: $('audit-assumptions').value,
                margin: $('audit-margin').value
            };
            const file = $('audit-file').files[0];
            if (file) {
                if (file.size > 65536) throw Error('Choose an observation CSV up to 64 KiB.');
                const bytes = new Uint8Array(await file.arrayBuffer());
                let binary = '';
                for (let start = 0; start < bytes.length; start += 8192) {
                    binary += String.fromCharCode(...bytes.subarray(start, start + 8192));
                }
                input.file = {name: file.name, base64: btoa(binary)};
                this.dialog.close();
                this.layout();
            }
            const result = await this.call('explanation-audit-submit', input);
            if (result.plan) {
                this.plan = result.plan;
                $('audit-status').textContent = 'Draft saved. Attach separate trial observations to run the audit.';
            } else this.report(result);
        } catch (error) {
            $('audit-status').textContent = error.message;
            this.show();
        } finally {
            $('audit-submit').disabled = false;
            this.busy = false;
        }
    }

    report(result) {
        this.announce(result.explanationAudit.label + '. ' + result.browserChecks + ' browser checks passed.');
    }
}
