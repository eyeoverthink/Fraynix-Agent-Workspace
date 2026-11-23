import {BriefingPicker,sectionLabel} from './briefing-picker.js';

const $ = id => document.getElementById(id);
const PROJECT_PLAN = 'fraynix-classification-routing-plan';
const SOURCE = 'fraynix-verified-classification';
const COMPARISON = 'fraynix-last-routed-comparison';
const KEY_COMPARISON = 'fraynix-last-key-comparison';
const COMPARISON_SUMMARY = 'fraynix-last-comparison-summary';
const COMPARISON_SUMMARIES = 'fraynix-recent-comparison-summaries';
const COMPARISON_EXPLORER = 'fraynix-last-comparison-explorer';
const COMPARISON_STORY = 'fraynix-last-comparison-story';
const BRIEFING_REVIEW = 'fraynix-comparison-briefing-review';
const COMPLETED_BRIEFING = 'fraynix-comparison-briefing';
const briefingPickerRequest = /^Review a two-source comparison briefing[.!]?$/i;
const briefingReview = /^Review a (two-source )?comparison briefing with ([a-z ,]+)[.!]?$/i;
const briefingAction = /^(Resume|Recheck|Export) the comparison briefing[.!]?$/i;
const byKey = /^Compare the last two verified routed CSVs by key (?:"([^"\r\n]+)"|([^\s".!?]+))[.!]?$/i;
const recheckKey = /^Recheck the matched routed CSV comparison[.!]?$/i;
const exportKey = /^Export the matched routed CSV comparison[.!]?$/i;
const summaryRequest = /^(?:Summarize the last verified matched comparison CSV|Recheck the matched comparison CSV summary)[.!]?$/i;

const explorerRequest = /^(?:Build an explorer from the verified comparison totals|Recheck the verified comparison explorer)[.!]?$/i;
const storyRequest = /^(?:Build a change story from the verified comparison totals|Recheck the verified change story|Export the verified change story)[.!]?$/i;
const lessonRequest = /^(Teach|Use|Recheck) comparison explorer lesson ([a-z][a-z0-9-]{2,55})[.!]?$/i;
const lessonPin = name => 'fraynix-comparison-explorer-lesson-'+name;

export const isRoutingRequest = text => /^(?:Route the last verified classification|Resume classification routing)[.!]?$/i.test(text.trim());
export const isRoutedDashboardRequest = text =>
    byKey.test(text.trim()) || recheckKey.test(text.trim()) || exportKey.test(text.trim()) ||
    summaryRequest.test(text.trim()) || explorerRequest.test(text.trim()) || storyRequest.test(text.trim()) ||
    lessonRequest.test(text.trim()) || briefingPickerRequest.test(text.trim()) || briefingReview.test(text.trim()) || briefingAction.test(text.trim()) ||
    /^(?:Build a dashboard from the (?:last )?verified routed CSV|Recheck the routed CSV dashboard|Compare the last two verified routed CSVs|Recheck the routed CSV comparison)[.!]?$/i.test(text.trim());

export class ClassificationRoutingPanel {
    constructor({call, layout, announce}) {
        Object.assign(this,{call,layout,announce});
        this.dialog = $('classification-routing');
        this.plan = null;
        this.busy = false;
        this.dialog.addEventListener('close',layout);
        $('routing-close').onclick = () => this.dialog.close();
        this.briefingDialog = $('comparison-briefing');
        this.briefingDialog.addEventListener('close',layout);
        this.briefingPicker = new BriefingPicker({call,layout,onReview:review=>this.rememberBriefing(review)});
        $('briefing-close').onclick = () => this.briefingDialog.close();
        $('briefing-build').onclick = async () => {
            const review = this.briefing;
            if (!review) return;
            $('briefing-build').disabled = true;
            this.briefingDialog.close();
            this.layout();
            try {
                const result = await this.call('briefing-build',{id:review.id,reviewSha256:review.reviewSha256});
                localStorage.setItem(COMPLETED_BRIEFING,review.id);
                this.announce('Comparison briefing verified against Java. '+result.browserChecks+' browser checks passed.');
            } catch (error) {
                this.showBriefing(review,error.message);
            }
        };
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
        if (briefingPickerRequest.test(text.trim())) {
            await this.briefingPicker.open();
            return;
        }
        const review = briefingReview.exec(text.trim());
        const briefing = briefingAction.exec(text.trim());
        if (review || briefing) {
            try {
                if (review) {
                    const exportId = localStorage.getItem(COMPARISON_SUMMARY);
                    const exportIds = JSON.parse(localStorage.getItem(COMPARISON_SUMMARIES) || '[]');
                    if (!exportId) throw Error('Summarize a comparison CSV first');
                    if (review[1] && exportIds.length !== 2) throw Error('Summarize two different comparison CSV exports first');
                    const sections = review[2].trim().toLowerCase().split(/\s*(?:,\s*(?:and\s+)?|\s+and\s+)\s*/);
                    const prepared = await this.call('briefing-review',{...(review[1]?{exportIds}:{exportId}),sections});
                    this.rememberBriefing(prepared);
                } else {
                    const action = briefing[1].toLowerCase();
                    const id = localStorage.getItem(action==='resume'?BRIEFING_REVIEW:COMPLETED_BRIEFING);
                    if (!id) throw Error(action==='resume'?'Review a comparison briefing first':'Build a comparison briefing first');
                    if (action==='resume') {
                        this.showBriefing(await this.call('briefing-read',{id}));
                    } else {
                        const result = await this.call('briefing-'+action,{id});
                        if (action==='export') {
                            const name = result.kind==='paired-comparison-briefing' ? 'two-source comparison briefing' : 'comparison briefing';
                            this.announce('Saved verified '+name+' to '+result.path);
                        } else {
                            this.announce('Comparison briefing verified against Java. '+result.browserChecks+' fresh browser checks passed.');
                        }
                    }
                }
            } catch (error) { this.announce(error.message); }
            return;
        }
        const lesson = lessonRequest.exec(text.trim());
        if (lesson) {
            try { await this.fromExplorerLesson(lesson[1].toLowerCase(),lesson[2].toLowerCase()); }
            catch (error) { this.announce(error.message); }
            return;
        }
        const explorer = explorerRequest.test(text.trim());
        const story = storyRequest.test(text.trim());
        if (explorer || story || summaryRequest.test(text.trim())) {
            try {
                const exportStory = story && /^export/i.test(text.trim());
                const verifyOnly = exportStory || /^recheck/i.test(text.trim());
                const storage = story ? COMPARISON_STORY : explorer ? COMPARISON_EXPLORER : COMPARISON_SUMMARY;
                const needsSummary = explorer || story;
                const previous = verifyOnly ? localStorage.getItem(storage)
                    : needsSummary ? localStorage.getItem(COMPARISON_SUMMARY) : undefined;
                if ((verifyOnly || needsSummary) && !previous) {
                    if (story && verifyOnly) throw Error('Build a verified change story first');
                    throw Error(explorer && verifyOnly ? 'Build a verified comparison explorer first' : 'Summarize a comparison CSV first');
                }
                const result = await this.call('routing-summary',{verifyOnly,exportId:previous,explorer,story,exportStory});
                if (exportStory) {
                    this.announce('Saved verified change story to '+result.path);
                    return;
                }
                if (!verifyOnly && !needsSummary) {
                    const recent = JSON.parse(localStorage.getItem(COMPARISON_SUMMARIES) || '[]');
                    const ids = [...recent,localStorage.getItem(COMPARISON_SUMMARY),result.comparisonExportId].filter(Boolean);
                    const newest = ids.pop();
                    localStorage.setItem(COMPARISON_SUMMARIES,JSON.stringify([...new Set(ids.filter(id=>id!==newest)),newest].slice(-2)));
                }
                localStorage.setItem(storage,result.comparisonExportId);
                const label = story ? 'Change story' : explorer ? 'Comparison explorer' : 'Comparison CSV totals';
                this.announce(label+' verified against Java. '+result.browserChecks
                    +(result.rechecked?' fresh browser checks passed.':' browser checks passed.'));
            } catch (error) { this.announce(error.message); }
            return;
        }
        if (isRoutedDashboardRequest(text)) {
            try {
                const request = text.trim();
                const keyed = byKey.exec(request);
                const exportCsv = exportKey.test(request);
                const matched = Boolean(keyed) || recheckKey.test(request) || exportCsv;
                const compare = matched || /^(compare|recheck the routed CSV comparison)/i.test(request);
                const verifyOnly = exportCsv || /^recheck/i.test(request);
                const storage = matched ? KEY_COMPARISON : COMPARISON;
                const previous = compare && verifyOnly ? JSON.parse(localStorage.getItem(storage)) : null;
                if (matched && verifyOnly && !previous) throw Error('Build a matched routed CSV comparison first');
                const keyColumn = keyed ? (keyed[1] ?? keyed[2]) : previous?.keyColumn;
                const result = await this.call('routing-dashboard',{verifyOnly,compare,
                    ...(previous ? {pair:previous} : {}), ...(keyColumn !== undefined ? {keyColumn} : {}),
                    ...(exportCsv ? {exportCsv:true} : {})});
                if (exportCsv) {
                    this.announce('Saved '+result.source.groups+' verified comparison groups to '+result.path);
                    return;
                }
                if (compare) localStorage.setItem(storage,JSON.stringify({
                    earlier:result.earlierExportId,later:result.laterExportId,keyColumn:result.keyColumn
                }));
                const title = matched ? 'Matched comparison' : compare ? 'Comparison' : 'Dashboard';
                this.announce(title+' verified against Java. '+result.browserChecks
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

    rememberBriefing(review) {
        localStorage.setItem(BRIEFING_REVIEW,review.id);
        this.showBriefing(review);
    }

    showBriefing(review,error='') {
        this.briefing = review;
        const labels = review.sections?.map(sectionLabel) ?? review.sources.map((source,index)=>
            'Source '+(index+1)+' · '+source.sections.map(sectionLabel).join(', '));
        $('briefing-sections').replaceChildren(...labels.map(label=>{
            const item = document.createElement('li');
            item.textContent = label;
            return item;
        }));
        const evidence = $('briefing-source');
        if (review.sources) {
            evidence.replaceChildren(...review.sources.map((source,index)=>{
                const item = document.createElement('div');
                item.textContent = 'Source '+(index+1)+' · Comparison '+source.exportId;
                return item;
            }));
        } else {
            evidence.textContent = 'Comparison '+review.exportId;
        }
        $('briefing-status').textContent = error;
        $('briefing-build').disabled = false;
        if (!this.briefingDialog.open) this.briefingDialog.showModal();
        this.layout();
    }

    async fromExplorerLesson(action,name) {
        if (action==='teach') {
            const exportId = localStorage.getItem(COMPARISON_EXPLORER);
            if (!exportId) throw Error('Build a verified comparison explorer first');
            await this.call('teach-comparison-explorer',{name,exportId});
            this.announce('Saved comparison explorer lesson '+name+'.');
            return;
        }
        const verifyOnly = action==='recheck';
        const exportId = verifyOnly ? localStorage.getItem(lessonPin(name)) : undefined;
        if (verifyOnly && !exportId) throw Error('Apply this comparison explorer lesson first');
        const result = await this.call('run-comparison-explorer-lesson',{name,exportId,verifyOnly});
        localStorage.setItem(lessonPin(name),result.comparisonExportId);
        this.announce('Comparison explorer lesson '+name+' verified against Java. '+result.browserChecks
            +(result.rechecked?' fresh browser checks passed.':' browser checks passed.'));
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
