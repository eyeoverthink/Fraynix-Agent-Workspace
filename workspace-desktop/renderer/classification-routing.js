const $ = id => document.getElementById(id);
const PROJECT_PLAN = 'fraynix-classification-routing-plan';
const SOURCE = 'fraynix-verified-classification';
const COMPARISON = 'fraynix-last-routed-comparison';

export const isRoutingRequest = text => /^(?:Route the last verified classification|Resume classification routing)[.!]?$/i.test(text.trim());
export const isRoutedDashboardRequest = text =>
    /^(?:Build a dashboard from the (?:last )?verified routed CSV|Recheck the routed CSV dashboard|Compare the last two verified routed CSVs|Recheck the routed CSV comparison)[.!]?$/i.test(text.trim());

export class ClassificationRoutingPanel {
    constructor({call, layout, announce}) {
        Object.assign(this,{call,layout,announce});
        this.dialog = $('classification-routing');
        this.plan = null;
        this.busy = false;
        this.dialog.addEventListener('close',layout);
        $('routing-close').onclick = () => this.dialog.close();
        $('routing-export').onclick = () => this.work(async()=>{
            const saved = await this.call('export-routing',{id:this.plan.id});
            const status = $('routing-export-status');
            status.textContent='Saved '+saved.source.rows+' verified rows to '+saved.path;
            status.hidden=false;
        });
        $('routing-file').onchange = () => this.work(async()=>{
            const file = $('routing-file').files[0];
            if (!file) return;
            if (file.size > 2048) throw Error('Choose a mapping CSV up to 2 KiB.');
            const bytes = new Uint8Array(await file.arrayBuffer());
            this.plan = await this.call('routing-attach',{id:this.plan.id,base64:btoa(String.fromCharCode(...bytes))});
        });
        $('routing-run').onclick = () => this.work(async()=>{
            const {id,status,decisionSha256} = this.plan;
            const pauseAfterNext = $('routing-pause').checked;
            this.dialog.close();
            this.layout();
            try {
                this.plan = await this.call('run-routing',{id,pauseAfterNext,...(status==='review'?{decisionSha256}:{})});
                this.announce(this.plan.status==='verified'
                    ? 'Routing verified. Open each saved result from the plan.'
                    : 'The next procedure verified. Resume routing to continue.');
            } catch (error) {
                this.plan = await this.call('routing-read',{id}).catch(()=>this.plan);
                throw error;
            } finally {
                this.dialog.showModal();
                this.layout();
            }
        });
    }

    remember(task) {
        if (task.status==='verified' && task.choice.column==='range') localStorage.setItem(SOURCE,task.id);
    }

    async fromChat(text) {
        if (isRoutedDashboardRequest(text)) {
            try {
                const compare = /^(compare|recheck the routed CSV comparison)/i.test(text.trim());
                const verifyOnly = /^recheck/i.test(text.trim());
                const previous = compare&&verifyOnly ? localStorage.getItem(COMPARISON) : null;
                const result = await this.call('routing-dashboard',{verifyOnly,compare,...(previous?{pair:JSON.parse(previous)}:{})});
                if(compare)localStorage.setItem(COMPARISON,JSON.stringify({earlier:result.earlierExportId,later:result.laterExportId}));
                this.announce((compare?'Comparison':'Dashboard')+' verified against Java. '+result.browserChecks
                    +(result.rechecked?' fresh browser checks passed.':' browser checks passed.'));
            } catch (error) { this.announce(error.message); }
            return;
        }
        const resume = /^resume/i.test(text);
        const saved = localStorage.getItem(resume ? PROJECT_PLAN : SOURCE);
        if (!saved) {
            this.announce(resume ? 'Start a classification routing plan first.' : 'Build and verify a classifier first.');
            return;
        }
        try {
            this.plan = await this.call(resume?'routing-read':'routing-start',resume?{id:saved}:{sourceId:saved});
            localStorage.setItem(PROJECT_PLAN,this.plan.id);
            $('routing-file').value='';
            $('routing-pause').checked=false;
            $('routing-export-status').hidden=true;
            this.render();
            this.dialog.showModal();
            this.layout();
        } catch (error) { this.announce(error.message); }
    }

    async work(action) {
        if (this.busy) return;
        this.busy=true;
        this.render();
        let failure='';
        try { await action(); }
        catch (error) { failure=error.message; }
        finally {
            this.busy=false;
            this.render();
            if (failure) $('routing-status').textContent=failure;
        }
    }

    render() {
        const {status,source,rows=[],steps=[]} = this.plan;
        const review = status==='review';
        const approved = status==='approved';
        const done = status==='verified';
        $('routing-source').textContent='Verified classification · '+source.reviewId;
        $('routing-map-label').hidden=approved||done;
        $('routing-file').disabled=this.busy||approved||done;
        $('routing-status').textContent=this.busy?'Checking the plan and its evidence…':{
            'awaiting-map':'Attach a CSV with label,lesson. Map each label that occurs in the input.',
            review:'Review the choices before running any procedure.',
            approved:'Confirmed plan. Resume from its next unfinished procedure.',
            verified:'Every scheduled procedure has a verified result.',
        }[status];
        $('routing-choices').hidden=!rows.length;
        $('routing-rows').replaceChildren(...rows.map(row=>{
            const tr=document.createElement('tr');
            const reason=row.min+' ≤ '+row.reading+' < '+row.max+' ('+row.measurement+')';
            for(const value of [row.row,reason,row.label,row.lesson,row.inputColumn+' = '+row.value+' ('+row.unit+')']){
                const cell=document.createElement('td');
                cell.textContent=value;
                tr.append(cell);
            }
            return tr;
        }));
        $('routing-results').replaceChildren(...steps.map(step=>{
            const li=document.createElement('li');
            const label=document.createElement('span');
            label.textContent=step.name+' · '+(step.result?'Verified':'Pending');
            li.append(label);
            if(step.result){
                const button=document.createElement('button');
                button.textContent='Open '+step.name;
                button.disabled=this.busy;
                button.onclick=()=>this.work(async()=>{
                    await this.call('open-routing-result',{id:this.plan.id,childId:step.result.childId});
                    this.dialog.close();
                });
                li.append(button);
            }
            return li;
        }));
        $('routing-pause-label').hidden=!(review||approved);
        $('routing-pause').disabled=this.busy;
        $('routing-run').hidden=!(review||approved);
        $('routing-run').disabled=this.busy;
        $('routing-run').textContent=review?'Confirm and run':'Resume confirmed plan';
        $('routing-export').hidden=!done;
        $('routing-export').disabled=this.busy;
    }
}
