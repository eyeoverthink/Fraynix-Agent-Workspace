import {workspaceRequestText} from './workspace-capabilities.mjs';

export function avatarLevelIntent(request) {
    const text=workspaceRequestText(request);
    if (/^(?:resume|continue|finish|reopen|export)\s+(?:my |the |last |saved )*(?:avatar|portrait|character)[ -](?:level|game|mission|course)(?:\s+mission)?[.!?]*$/i.test(text))
        return {resume:true};
    if (text.length>1200 || !/^(?:take|use|turn|convert|make|build|create|put|design|generate)\b/i.test(text)
            || !/\b(?:image|photo|picture|portrait|avatar|character|sprite)\b/i.test(text)
            || !/\b(?:level|platformer|playable game|browser game)\b/i.test(text)
            || !/\b(?:offline|export|standalone|playable|playtest|test)\b/i.test(text))
        return null;
    const title=text.match(/\b(?:called|named|titled)\s+["“]([^"”]{1,70})["”]/i)?.[1]?.trim()||'';
    const jump=/\b(?:jump(?:s|ing)?|obstacle(?:s)?|hurdle(?:s)?|platforms|platformer)\b/i.test(text)
        && !/\b(?:no|without)\s+(?:jumps|jumping|obstacles|hurdles)\b/i.test(text);
    return {request,title,course:jump?'jump':'relay',
        pause:/\b(?:(?:pause|stop|wait)\s+before\s+(?:the\s+)?export(?:ing)?|without exporting|(?:do not|don't) export)\b/i.test(text)};
}

export class AvatarLevelPanel {
    constructor({avatar,request,shell,requireSaved,announce,notify,review}) {
        this.avatar=avatar;
        this.request=request;
        this.shell=shell;
        this.requireSaved=requireSaved;
        this.announce=announce;
        this.notify=notify;
        this.review=review;
        this.root=document.createElement('section');
        this.root.id='avatar-level-workflow';
        this.root.className='avatar-card';
        this.root.hidden=true;
        this.root.innerHTML=`
            <h3>Image to playable level</h3>
            <p id="avatar-level-status" role="status"></p>
            <div class="avatar-actions">
                <button id="avatar-level-resume" class="primary">Continue mission</button>
                <button id="avatar-level-review" hidden>Open in Tile Workshop</button>
                <button id="avatar-level-files" hidden>Show offline files</button>
            </div>
            <details id="avatar-level-evidence" hidden>
                <summary>Playtest and export evidence</summary>
                <dl id="avatar-level-details"></dl>
            </details>`;
        avatar.root.prepend(this.root);
        this.status=this.root.querySelector('#avatar-level-status');
        this.resumeButton=this.root.querySelector('#avatar-level-resume');
        this.reviewButton=this.root.querySelector('#avatar-level-review');
        this.filesButton=this.root.querySelector('#avatar-level-files');
        this.evidence=this.root.querySelector('#avatar-level-evidence');
        this.details=this.root.querySelector('#avatar-level-details');
        this.resumeButton.onclick=()=>this.run(()=>this.resume());
        this.reviewButton.onclick=()=>this.run(()=>this.review(this.mission.levelId));
        this.filesButton.onclick=()=>this.run(()=>this.call('reveal',{id:this.mission.id}));
    }

    call(command,args={}){return this.request('avatar-level',{command,args});}

    async load({show=false}={}){
        const mission=await this.call('read');
        if(mission.id||show){
            this.root.hidden=false;
            this.accept(mission);
        }
        return mission;
    }

    async start(intent){
        if(this.pending)return;
        await this.run(async()=>{
            this.requireSaved();
            const mission=await this.call('start',intent);
            this.accept(mission);
            await this.continueMission(mission);
        });
    }

    async resume(){
        this.requireSaved();
        const mission=await this.load({show:true});
        if(!mission.id)return;
        await this.continueMission(mission);
    }

    async continueMission(mission){
        if(mission.stage==='awaiting-image'&&!this.avatar.hasSource){
            this.announce('Choose a portrait image or a video frame. I’ll build the character and continue this saved level request.');
            this.shell().needInput({title:'Portrait needed for the level',
                description:'Import a local image or choose a frame from a local video.',
                choose:{label:'Choose portrait image',action:()=>this.avatar.chooseImage()},
                canContinue:()=>this.avatar.hasSource&& !this.avatar.pending,
                resume:()=>this.run(()=>this.resume())});
            this.avatar.onSource=()=>{
                this.shell().syncFollowup();
                if(this.mission?.stage==='awaiting-image' && !this.pending && this.avatar.hasSource && !this.avatar.pending)
                    this.status.textContent='Portrait ready. Review the crop, then continue.';
            };
            return;
        }
        let avatarId;
        if(mission.stage==='awaiting-image'){
            this.progress('Building the character');
            const avatar=await this.avatar.run(()=>this.avatar.build());
            if(!avatar)throw Error(this.avatar.find('status').textContent);
            avatarId=avatar.id;
        }
        this.progress(mission.stage==='verified'?'Checking export files':'Preparing the playable level');
        const result=await this.call('advance',{id:mission.id,avatarId,release:mission.stage==='verified'});
        this.accept(result);
        if(result.stage==='verified'){
            this.shell().setStageStatus('Verified');
            this.announce(result.title+' passed its offline playtest at '+result.evidence.score+
                ' points. Continue the mission when you’re ready to export.');
        }
        if(result.stage==='exported'){
            this.shell().setStageStatus('Exported');
            this.notify('Verified offline export saved.');
            this.announce(result.title+' is ready. The playtest collected '+result.evidence.pickups+
                ' pickups, cleared '+result.evidence.hurdles+' hurdles and scored '+result.evidence.score+
                '. The offline game, editable level and evidence are saved.');
        }
    }

    async run(action){
        if(this.pending)return this.pending;
        this.root.hidden=false;
        const promise=Promise.resolve().then(action).catch(error=>{
            this.status.textContent=error.message.replace(/^Error invoking remote method '[^']+': (?:Error: )?/,'');
            this.announce(this.status.textContent);
        });
        this.pending=promise;
        this.controls();
        try{return await promise;}
        finally{this.pending=null;this.controls();}
    }

    progress(text){
        this.status.textContent=text+'…';
        this.shell().setStageStatus(text);
    }

    accept(mission){
        this.mission=mission;
        const proof=mission.evidence;
        this.root.hidden=false;
        this.status.textContent={
            'awaiting-image':'Waiting for the portrait',
            built:'Level built. Ready to playtest',
            verified:'Playtest passed. Export is ready',
            exported:'Offline game and editable level saved'
        }[mission.stage]||'Ask “Turn this image into a character, build a playable level, then test and export it offline.”';
        this.evidence.hidden=!proof;
        this.details.replaceChildren();
        if(proof){
            this.detail('Level',mission.title);
            this.detail('Playtest',proof.score+' points · '+proof.pickups+' pickups · '+proof.hurdles+' hurdles');
            this.detail('Checks','Desktop and offline replay agreed');
            if(proof.straightWalkBlocked)this.detail('Straight-walk comparison','Blocked at the first hurdle');
            for(const [kind,file] of Object.entries(mission.files||{}))this.detail(kind,file.path);
        }
        this.controls();
    }

    detail(label,value){
        const term=document.createElement('dt'), definition=document.createElement('dd');
        term.textContent=label;
        definition.textContent=value;
        this.details.append(term,definition);
    }

    controls(){
        const stage=this.mission?.stage;
        this.resumeButton.hidden=!stage||stage==='exported';
        this.resumeButton.disabled=Boolean(this.pending);
        this.reviewButton.hidden=stage!=='verified'&&stage!=='exported';
        this.reviewButton.disabled=Boolean(this.pending);
        this.filesButton.hidden=stage!=='exported';
        this.filesButton.disabled=Boolean(this.pending);
    }
}
