const formats = {avi:'Export video · AVI',wav:'Export audio · WAV',png:'Export poster · PNG',json:'Export timeline · JSON'};

export function isMotionMediaRequest(text) {
    return /^(?:(?:make|create|build) (?:a )?(?:motion media package|video with sound and a poster) from (?:this|the selected) run|turn (?:this|the selected) run into (?:a )?(?:motion media package|video with sound and a poster))[.!]?$/i.test(text.trim());
}

export class MotionMediaPanel {
    constructor(request,back,edit) {
        this.request=request;
        this.root=document.createElement('section');
        this.root.id='motion-media';
        this.root.hidden=true;
        this.root.innerHTML='<div class="media-actions"><button id="motion-back">Tile editor</button>' +
            '<button id="motion-refresh">Refresh recordings</button></div>' +
            '<form><label>Native motion recording<select id="motion-source"></select></label>' +
            '<button class="primary" id="motion-build">Build motion media</button></form>' +
            '<p id="motion-status" role="status">Choose a completed recording from 3D Lab.</p>' +
            '<div id="motion-output" hidden><figure><img id="motion-poster" alt="Verified poster of the recorded motion">' +
            '<figcaption id="motion-evidence"></figcaption></figure><div class="media-actions" id="motion-exports"></div>' +
            '<audio id="motion-audio" controls hidden></audio></div>';
        document.querySelector('#canvas-surface').before(this.root);
        this.source=this.find('#motion-source');
        this.status=this.find('#motion-status');
        this.audio=this.find('#motion-audio');
        this.find('#motion-back').onclick=back;
        this.find('#motion-refresh').onclick=()=>this.action(()=>this.refresh());
        this.root.querySelector('form').onsubmit=event=>{
            event.preventDefault();
            this.action(()=>this.build());
        };
        const exports=this.find('#motion-exports');
        for (const [format,caption] of Object.entries(formats)) {
            const button=optionButton(caption,format);
            button.onclick=()=>this.action(async()=>{
                const receipt=await this.call('export',{id:this.media.id,format});
                this.status.textContent='Exported '+format.toUpperCase()+' to '+receipt.path;
            });
            exports.append(button);
        }
        const listen=document.createElement('button');
        listen.id='motion-listen';
        listen.textContent='Listen to WAV';
        listen.onclick=()=>this.action(()=>this.listen());
        exports.append(listen);
        const editButton=document.createElement('button');
        editButton.id='motion-edit-timeline';
        editButton.textContent='Edit on timeline';
        editButton.onclick=()=>edit(this.media.id);
        exports.append(editButton);
        this.source.onchange=()=>this.controls();
        this.controls();
    }

    find(selector) { return this.root.querySelector(selector); }
    call(command,args={}) { return this.request('motion-media',{command,args}); }

    show(visible) {
        if (this.visible===visible) return;
        this.visible=visible;
        this.root.hidden=!visible;
        if (visible) this.action(()=>this.refresh());
        else this.clearAudio();
    }

    async refresh() {
        const old=this.source.value;
        const {runs}=await this.call('sources');
        this.source.replaceChildren(new Option('Choose a completed run',''),...runs.map(run=>
            new Option(run.title+' · '+run.id.slice(0,8),run.id)));
        if (runs.some(run=>run.id===old)) this.source.value=old;
        else if (runs.length===1) this.source.value=runs[0].id;
        const latest=await this.call('latest');
        if (latest.id) {
            if (!this.source.value && runs.some(run=>run.id===latest.source.runId)) this.source.value=latest.source.runId;
            this.present(latest,'Reopened and verified the four saved files.');
        }
    }

    async build() {
        if (!this.source.value) throw Error('Choose the completed motion recording in Media.');
        this.status.textContent='Rendering, decoding and checking the four media files…';
        const result=await this.call('build',{runId:this.source.value});
        this.present(result,'Motion media verified and saved.');
        return result;
    }

    present(result,message) {
        this.media=result;
        this.clearAudio();
        this.find('#motion-output').hidden=false;
        this.find('#motion-poster').src='data:image/png;base64,'+result.posterPng;
        this.find('#motion-evidence').textContent=result.source.title+' · '+result.frames+' frames · '+
            result.durationS.toFixed(3)+' s · '+result.encoder.tested.length+' video setting'+
            (result.encoder.tested.length===1?'':'s')+' evaluated · WAV peak '+Math.round(result.sound.exportPeak*100)+'%'+
            (result.encoder.rememberedStart?' · started with a previously verified setting':'');
        this.status.textContent=message;
    }

    async listen() {
        const {base64}=await this.call('preview-audio',{id:this.media.id});
        this.clearAudio();
        const data=Uint8Array.from(atob(base64),character=>character.charCodeAt(0));
        this.audio.src=URL.createObjectURL(new Blob([data],{type:'audio/wav'}));
        this.audio.hidden=false;
        await this.audio.play();
        this.status.textContent='Playing the verified audio track.';
    }

    clearAudio() {
        if (this.audio.src.startsWith('blob:')) URL.revokeObjectURL(this.audio.src);
        this.audio.pause();
        this.audio.removeAttribute('src');
        this.audio.hidden=true;
    }

    controls() {
        this.find('#motion-build').disabled=Boolean(this.pending)||!this.source.value;
        this.source.disabled=Boolean(this.pending);
        for (const button of this.find('#motion-exports').querySelectorAll('button'))
            button.disabled=Boolean(this.pending)||!this.media;
    }

    action(use) {
        if (this.pending) return this.pending;
        this.pending=Promise.resolve().then(use).catch(error=>{
            this.status.textContent=error.message.replace(/^Error invoking remote method '[^']+': (?:Error: )?/,'');
            return null;
        }).finally(()=>{
            this.pending=null;
            this.controls();
        });
        this.controls();
        return this.pending;
    }
}

function optionButton(caption,format) {
    const button=document.createElement('button');
    button.textContent=caption;
    button.dataset.motionFormat=format;
    return button;
}
