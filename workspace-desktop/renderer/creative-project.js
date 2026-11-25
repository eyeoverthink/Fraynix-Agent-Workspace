const option=(label,value)=>new Option(label,value);
const frameLabel=frames=>frames+' frames';
const timing=cut=>frameLabel(cut.openingFrames)+' opening · '+frameLabel(cut.followupFrames)+' second shot';

function element(tag,text,className) {
    const node=document.createElement(tag);
    if (text) node.textContent=text;
    if (className) node.className=className;
    return node;
}

export class CreativeProjectPanel {
    constructor(request,{getCut,getAssets,openCut}) {
        this.request=request;
        this.getCut=getCut;
        this.getAssets=getAssets;
        this.openCut=openCut;
        this.root=element('section','','creative-project');
        this.root.innerHTML=`
          <div class="creative-head">
            <h2>Pick up where you left off</h2>
            <label>Project<select id="creative-project"></select></label>
            <button id="creative-new">New project</button>
          </div>
          <form id="creative-create" hidden>
            <label>Name<input id="creative-name" maxlength="75" required></label>
            <label>What do you want to make?<input id="creative-intention" maxlength="180" required></label>
            <button class="primary">Create project</button>
          </form>
          <p id="creative-status" role="status"></p>
          <section id="creative-journey" hidden>
            <div id="creative-home"></div>
            <details>
              <summary>Update what’s next</summary>
              <form id="creative-next">
                <label>Next intention<input id="creative-next-text" maxlength="180" required></label>
                <button>Save intention</button>
              </form>
            </details>
            <details id="creative-compose">
              <summary>Make another cut</summary>
              <form id="creative-propose">
                <label>Opening<select id="creative-opening" required></select></label>
                <label>Second shot<select id="creative-followup" required></select></label>
                <label>Sound<select id="creative-sound"></select></label>
                <label>Approved reference<select id="creative-reference"></select></label>
                <label class="creative-check"><input id="creative-use-memory" type="checkbox" checked>Use saved pacing</label>
                <label class="creative-prompt">Tell Fraynix how to edit
                  <input id="creative-request" maxlength="220" placeholder="Make another like the last one, a little quicker, for the family." required>
                </label>
                <button class="primary">Propose cut</button>
              </form>
              <div id="creative-reference-cards" hidden></div>
            </details>
            <section id="creative-draft" hidden>
              <h3>Review the proposed cut</h3>
              <div id="creative-explanation"></div>
              <div id="creative-draft-player"></div>
              <div class="media-actions">
                <button id="creative-preview">Preview this cut</button>
                <button class="primary" id="creative-approve-draft">Approve this cut</button>
              </div>
              <form id="creative-correct">
                <label class="creative-prompt">What should change?
                  <input id="creative-correction" maxlength="220" placeholder="Keep the opening slow. Speed up the rest." required>
                </label>
                <button name="scope" value="once">Just this cut</button>
                <button name="scope" value="remember">Remember for this project</button>
              </form>
            </section>
            <details id="creative-handoff">
              <summary>Prepare a shareable copy</summary>
              <form id="creative-review-share">
                <label>Approved cut<select id="creative-share-cut" required></select></label>
                <label>Audience<select id="creative-audience"><option>Family</option><option>Public</option></select></label>
                <button id="creative-begin-share">Review copy</button>
              </form>
              <section id="creative-share" hidden>
                <div id="creative-share-player"></div>
                <div id="creative-share-details"></div>
              </section>
            </details>
          </section>`;
        this.bind();
    }

    find(id) { return this.root.querySelector('#'+id); }
    call(command,args={}) { return this.request('creative',{command,args}); }
    action(command,args={}) { return this.call(command,{project:this.state.id,...args}); }
    tell(message) { this.find('creative-status').textContent=message; }

    bind() {
        this.find('creative-new').onclick=()=>{
            const form=this.find('creative-create');
            form.hidden=!form.hidden;
            if (!form.hidden) this.find('creative-name').focus();
        };
        this.find('creative-project').onchange=event=>{
            const id=event.target.value;
            if (id) this.run(async()=>this.showState(await this.call('open',{project:id}),true));
        };
        this.find('creative-create').onsubmit=event=>{
            event.preventDefault();
            this.run(async()=>{
                const created=await this.call('create',{name:this.find('creative-name').value,intent:this.find('creative-intention').value});
                event.target.reset();
                event.target.hidden=true;
                await this.load(created.id);
                this.tell('Project created. Approve a verified timeline cut to set its reference.');
            });
        };
        this.find('creative-next').onsubmit=event=>{
            event.preventDefault();
            this.change('intent',{intent:this.find('creative-next-text').value},'Saved your next intention.');
        };
        this.find('creative-propose').onsubmit=event=>{
            event.preventDefault();
            this.run(async()=>{
                const result=await this.action('propose',{
                    request:this.find('creative-request').value,
                    opening:this.find('creative-opening').value,followup:this.find('creative-followup').value,
                    audio:this.find('creative-sound').value,reference:this.find('creative-reference').value,
                    useMemory:this.find('creative-use-memory').checked
                });
                if (result.clarification) {
                    this.tell(result.clarification);
                    if (result.references) await this.referenceCards(result.references);
                    return;
                }
                this.find('creative-reference-cards').hidden=true;
                this.showState(result);
                this.tell('Proposed a verified cut from the new sources.');
            });
        };
        this.find('creative-preview').onclick=()=>this.run(()=>this.play('creative-draft-player',this.state.draft.revision));
        this.find('creative-approve-draft').onclick=()=>this.change('approve-draft',{draft:this.state.draft.id},'Cut approved and saved to this project.');
        this.find('creative-correct').onsubmit=event=>{
            event.preventDefault();
            const remember=event.submitter.value==='remember';
            this.change('correct',{draft:this.state.draft.id,request:this.find('creative-correction').value,remember},
                remember?'Preview updated. Approve the cut to remember the opening pace.':'Preview updated for this cut.');
        };
        const review=()=>{
            const revision=this.find('creative-share-cut').value;
            if (revision) this.change('review-share',{revision,audience:this.find('creative-audience').value},'Review the actual cut and its sources for this audience.');
        };
        this.find('creative-review-share').onsubmit=event=>{event.preventDefault();review();};
        for (const field of ['creative-share-cut','creative-audience']) this.find(field).onchange=()=>{
            if (this.state.share) review();
        };
    }

    async load(preferred=localStorage.getItem('creative-project')) {
        if (this.loaded && this.state?.id===preferred) return;
        const {projects}=await this.call('list');
        const id=projects.find(project=>project.id===preferred)?.id||projects[0]?.id;
        this.find('creative-project').replaceChildren(option('Choose a project',''),...projects.map(project=>option(project.name,project.id)));
        if (id) this.showState(await this.call('open',{project:id}),true);
        else this.tell('Create a project to keep your cuts, pacing and what you want to try next.');
        this.loaded=true;
    }

    refreshSources() {
        const assets=this.getAssets();
        for (const [id,empty,accept] of [
            ['creative-opening','Choose footage',asset=>asset.pictureFrames>0],
            ['creative-followup','Choose footage',asset=>asset.pictureFrames>0],
            ['creative-sound','No source audio',asset=>asset.soundFrames>0]
        ]) this.options(id,empty,assets.filter(accept).map(asset=>option(asset.source.title,asset.id)));
        if (this.state) this.drawHome();
    }

    options(id,empty,items) {
        const select=this.find(id);
        const before=select.value;
        select.replaceChildren(...(empty?[option(empty,'')]:[]),...items);
        if (Array.from(select.options).some(entry=>entry.value===before)) select.value=before;
    }

    showState(state,switching=false) {
        this.state=state;
        this.find('creative-journey').hidden=false;
        this.find('creative-project').value=state.id;
        localStorage.setItem('creative-project',state.id);
        this.find('creative-next-text').value=state.intent;
        if (switching) {
            for (const id of ['creative-propose','creative-correct','creative-review-share']) this.find(id).reset();
            this.find('creative-reference-cards').hidden=true;
            this.find('creative-compose').open=Boolean(state.draft);
            this.find('creative-handoff').open=Boolean(state.share);
            this.tell('Reopened '+state.name+'.');
        }
        const cutOptions=()=>[...state.approved].reverse().map(cut=>option(cut.title+' · '+new Date(cut.approvedAt).toLocaleString(),cut.id));
        this.options('creative-reference','Let Fraynix ask if unclear',cutOptions());
        this.options('creative-share-cut',null,cutOptions());
        this.find('creative-begin-share').disabled=!state.approved.length;
        if (state.share) {
            this.find('creative-share-cut').value=state.share.revision;
            this.find('creative-audience').value=state.share.audience;
        } else if (switching || !this.find('creative-share-cut').value) {
            this.find('creative-audience').value=state.finished?.audience==='Public'?'Public':'Family';
        }
        if (state.draft && switching) {
            const draft=state.draft;
            for (const [field,value] of Object.entries({request:draft.request,correction:draft.correction||'',reference:draft.reference,
                opening:draft.openingAsset,followup:draft.followupAsset,sound:draft.audioAsset})) this.find('creative-'+field).value=value;
        }
        this.drawHome();
        this.drawDraft();
        this.drawShare();
    }

    drawHome() {
        const state=this.state;
        const home=this.find('creative-home');
        const last=state.approved.at(-1);
        const saved=state.choices.at(-1);
        home.replaceChildren(element('p',state.intent,'creative-next-intent'));
        if (last) {
            const line=element('div','','creative-summary');
            line.append(element('span','Last approved · '+last.title+' · '+frameLabel(last.frames)),
                this.button('Open approved cut',()=>this.openCut(last.id)));
            home.append(line);
        } else home.append(element('p','Approve a rendered timeline cut to give this project a reference.'));
        const current=this.getCut();
        if (current && !state.approved.some(cut=>cut.id===current.id)) {
            const button=this.button('Approve current timeline cut',()=>this.change('approve-cut',{revision:current.id},
                'Current timeline cut approved as a project reference.'));
            button.id='creative-approve-current';
            home.append(button);
        }
        if (saved) {
            const source=state.approved.find(cut=>cut.id===saved.revision);
            const line=element('div','','creative-summary');
            line.append(element('span','Saved pacing · '+saved.label),
                this.button('Open pacing example',()=>this.openCut(saved.revision)),
                this.button('Undo saved pacing',()=>this.change('undo-choice',{},'Restored the earlier pacing choice.')));
            if (source) line.title='From '+source.title;
            home.append(line);
        }
        const finished=state.finished;
        if (finished && finished.audience!=='Private' &&
            (state.share?.revision!==finished.revision||state.share?.audience!==finished.audience)) {
            home.append(this.button('Prepare approved '+finished.audience.toLowerCase()+' cut',()=>{
                this.find('creative-handoff').open=true;
                return this.change('review-share',{revision:finished.revision,audience:finished.audience},
                    'Review the actual cut and its sources for this audience.');
            }));
        }
        if (state.share && state.share.phase!=='review') {
            const status=state.share.phase==='exported'?'Exported locally':'Ready to export';
            home.append(this.button(status+' · '+state.share.audience,()=>{this.find('creative-handoff').open=true;this.find('creative-handoff').scrollIntoView();}));
        }
    }

    drawDraft() {
        const draft=this.state.draft;
        this.pause('creative-draft-player');
        this.find('creative-draft').hidden=!draft;
        if (!draft) return;
        const reference=this.state.approved.find(cut=>cut.id===draft.reference);
        const explanation=this.find('creative-explanation');
        explanation.replaceChildren(
            element('p','Reference · '+reference.title+' · '+new Date(reference.approvedAt).toLocaleDateString()),
            element('p',timing(draft)+' · '+draft.audience,'creative-cut-time'),
            element('p','Reference opening · '+frameLabel(draft.referenceOpeningFrames)+' · After your request alone · '+frameLabel(draft.requestOpeningFrames))
        );
        if (draft.memory) {
            const prompt=element('p','Applied saved pacing · '+draft.memory.label);
            prompt.title='You said: '+draft.memory.words;
            prompt.append(' ',this.button('Open style example',()=>this.openCut(draft.memory.revision)));
            explanation.append(prompt);
        }
        if (draft.correction) explanation.append(element('p',draft.correction+
            (draft.remember?' · Remember opening on approval':' · This cut only')+
            (draft.remember&&draft.restTrim?' · Second-shot change stays with this cut':'')));
        if (draft.openingLimited) explanation.append(element('p','The opening uses all available frames in the selected footage.'));
    }

    async referenceCards(cuts) {
        const cards=this.find('creative-reference-cards');
        cards.hidden=false;
        cards.replaceChildren();
        for (const cut of [...cuts].reverse().slice(0,4)) {
            const checked=await this.request('timeline',{command:'open',args:{id:cut.id}}).catch(()=>null);
            const control=this.button(cut.title+' · '+new Date(cut.approvedAt).toLocaleString(),()=>{
                this.find('creative-reference').value=cut.id;
                cards.hidden=true;
                this.find('creative-request').focus();
            });
            if (checked) {
                const poster=element('img');
                poster.src='data:image/png;base64,'+checked.posterPng;
                poster.alt='';
                control.prepend(poster);
            } else control.disabled=true;
            cards.append(control);
        }
    }

    drawShare() {
        const share=this.state.share;
        this.pause('creative-share-player');
        this.find('creative-share').hidden=!share;
        if (!share) return;
        const details=this.find('creative-share-details');
        const labels={review:'Review for ',prepared:'Ready to export · ',exported:'Exported locally · '};
        details.replaceChildren(element('h3',labels[share.phase]+share.audience),
            element('p','This copy contains the rendered picture and sound in one AVI. Review what the audience will see and hear.'));
        const preview=this.button('Preview exact export',()=>this.run(async()=>{
            await this.play('creative-share-player',share.revision);
            if (share.phase==='review') await this.drawClearance(share);
        }));
        preview.id='creative-share-preview';
        details.append(preview);
        if (share.phase!=='review') {
            const button=share.phase==='prepared'
                ?this.button('Export approved AVI',()=>this.change('export-share',{review:share.id},'Exported the approved copy to this workspace.'))
                :this.button('Show saved AVI',()=>this.run(()=>this.action('locate-share',{review:share.id})));
            details.append(button);
        }
    }

    async drawClearance(share) {
        const cut=await this.request('timeline',{command:'open',args:{id:share.revision}});
        const details=this.find('creative-share-details');
        details.querySelector('fieldset')?.remove();
        const fieldset=element('fieldset');
        fieldset.append(element('legend','Clear included sources for '+share.audience));
        for (const [id,asset] of Object.entries(cut.definition.assets)) {
            const label=element('label','','creative-check');
            const box=element('input');
            Object.assign(box,{type:'checkbox',value:id});
            const roles=[...new Set(cut.definition.strips.filter(strip=>strip.assetId===id)
                .map(strip=>strip.lane.startsWith('AUDIO')?'sound':strip.lane==='OVERLAY'?'overlay':'picture'))];
            label.append(box,asset.source.title+' · '+roles.join(' and '));
            fieldset.append(label);
        }
        fieldset.append(this.button('Approve this audience copy',()=>this.change('approve-share',{
            review:share.id,cleared:[...fieldset.querySelectorAll(':checked')].map(box=>box.value)
        },'The approved audience copy is ready to export.')));
        details.append(fieldset);
    }

    async play(target,revision) {
        const data=await this.action('play',{revision});
        this.pause(target);
        const player=this.find(target);
        const picture=element('img');
        picture.alt='Actual rendered cut';
        const audio=element('audio');
        audio.preload='auto';
        const bytes=Uint8Array.from(atob(data.wav),value=>value.charCodeAt(0));
        audio.src=URL.createObjectURL(new Blob([bytes],{type:'audio/wav'}));
        const scrub=element('input');
        Object.assign(scrub,{type:'range',min:0,max:data.frames.length-1,value:0});
        scrub.setAttribute('aria-label','Preview frame');
        const readout=element('output');
        const sources=data.frames.map(frame=>'data:image/jpeg;base64,'+frame);
        let animation=0, current=-1;
        const paint=index=>{
            if (current===index) return;
            current=index;
            picture.src=sources[index];
            scrub.value=index;
            readout.textContent='Frame '+(index+1)+' of '+data.frames.length;
        };
        const tick=()=>{
            paint(Math.min(data.frames.length-1,Math.floor(audio.currentTime*data.fps)));
            if (!audio.paused) animation=requestAnimationFrame(tick);
        };
        const play=this.button('Play picture and sound',()=>{
            if (!audio.paused) { audio.pause();return; }
            audio.play().catch(error=>this.tell(error.message));
        });
        audio.onplay=()=>{play.textContent='Pause';tick();};
        audio.onpause=()=>{play.textContent='Play picture and sound';cancelAnimationFrame(animation);};
        scrub.oninput=()=>{
            const frame=scrub.valueAsNumber;
            audio.currentTime=frame/data.fps;
            paint(frame);
        };
        const controls=element('div','','creative-play-controls');
        controls.append(play,scrub,readout);
        player.append(picture,controls);
        player.stopPreview=()=>{
            audio.pause();
            cancelAnimationFrame(animation);
            URL.revokeObjectURL(audio.src);
        };
        paint(0);
    }

    pause(target) {
        const player=this.find(target);
        player.stopPreview?.();
        player.stopPreview=null;
        player.replaceChildren();
    }

    close() {
        this.pause('creative-draft-player');
        this.pause('creative-share-player');
    }

    button(label,action) {
        const button=element('button',label);
        button.type='button';
        button.onclick=action;
        return button;
    }

    change(command,args,message) {
        return this.run(async()=>{
            const result=await this.action(command,args);
            if (result.clarification) { this.tell(result.clarification);return; }
            this.showState(result);
            this.tell(message);
        });
    }

    run(task) {
        if (this.pending) return this.pending;
        this.root.setAttribute('aria-busy','true');
        this.tell('Checking this project…');
        this.pending=Promise.resolve().then(task).catch(error=>{
            this.tell(error.message.replace(/^Error invoking remote method '[^']+': (?:Error: )?/,''));
        }).finally(()=>{
            this.pending=null;
            this.root.removeAttribute('aria-busy');
        });
        return this.pending;
    }
}
