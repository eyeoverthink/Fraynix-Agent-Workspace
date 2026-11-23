const sections = [
    ['totals','Totals by unit'],
    ['chart','Interactive change chart'],
    ['rows','Source rows'],
];

export const sectionLabel = value => sections.find(([name])=>name===value)?.[1] ?? value;

export class BriefingPicker {
    constructor({call,layout,onReview}) {
        Object.assign(this,{call,layout,onReview});
        this.dialog = document.getElementById('briefing-picker');
        this.form = this.dialog.querySelector('form');
        this.slots = [...this.form.querySelectorAll('fieldset')];
        this.selects = this.slots.map(slot=>slot.querySelector('select'));
        this.older = this.dialog.querySelector('#briefing-older');
        this.submit = this.dialog.querySelector('#briefing-picker-review');
        this.status = this.dialog.querySelector('[role=status]');
        this.dialog.querySelector('#briefing-picker-close').onclick = () => this.dialog.close();
        this.dialog.addEventListener('close',()=>{
            if (!this.dialog.open) this.session = null;
            layout();
        });
        this.older.onclick = () => this.load(this.session);
        this.form.onsubmit = event => {
            event.preventDefault();
            this.review(this.session);
        };
    }

    async open() {
        if (this.dialog.open) return;
        this.form.reset();
        this.selects.forEach(select=>select.replaceChildren(new Option('Choose a comparison','')));
        this.session = {sources:new Set(),cursor:0,busy:false};
        this.dialog.showModal();
        this.layout();
        await this.load(this.session);
    }

    controls(session) {
        this.submit.disabled = session.busy || session.sources.size < 2;
        this.older.disabled = session.busy;
        this.older.hidden = session.cursor === null;
        this.slots.forEach(slot=>{slot.disabled=session.busy;});
    }

    async load(session) {
        if (!session || session.busy || session.cursor === null) return;
        session.busy = true;
        this.controls(session);
        this.status.textContent = 'Loading verified comparisons…';
        try {
            const result = await this.call('briefing-catalog',{cursor:session.cursor});
            if (session !== this.session || !this.dialog.open) return;
            for (const source of result.sources) {
                if (session.sources.has(source.exportId)) continue;
                const time = new Date(source.createdAt).toLocaleString([],{
                    dateStyle:'medium',timeStyle:'short',
                });
                const label = [time,source.groups+' groups',source.keyColumn,source.exportId.slice(0,8)].join(' · ');
                for (const select of this.selects) select.add(new Option(label,source.exportId));
                session.sources.add(source.exportId);
            }
            session.cursor = result.nextCursor;
            this.status.textContent = session.sources.size
                ? session.sources.size+' verified comparisons loaded.'
                : result.nextCursor===null ? 'Summarize two distinct comparison exports to start.'
                    : 'No verified summaries on this page. Load older comparisons to continue.';
        } catch (error) {
            if (session === this.session) this.status.textContent = error.message;
        } finally {
            session.busy = false;
            if (session === this.session) this.controls(session);
        }
    }

    async review(session) {
        if (!session || session.busy) return;
        const exportIds = this.selects.map(select=>select.value);
        const sourceSections = this.slots.map(slot=>[...slot.querySelectorAll('input:checked')].map(input=>input.value));
        if (exportIds.some(id=>!session.sources.has(id)) || exportIds[0]===exportIds[1]) {
            this.status.textContent = 'Choose two distinct comparisons.';
            return;
        }
        if (sourceSections.some(chosen=>!chosen.length)) {
            this.status.textContent = 'Choose at least one section for each source.';
            return;
        }
        session.busy = true;
        this.controls(session);
        try {
            const review = await this.call('briefing-review',{exportIds,sourceSections});
            if (session !== this.session || !this.dialog.open) return;
            this.dialog.close();
            this.onReview(review);
        } catch (error) {
            if (session === this.session) this.status.textContent = error.message;
        } finally {
            session.busy = false;
            if (session === this.session) this.controls(session);
        }
    }
}
