import {workspaceRequestText} from './workspace-capabilities.mjs';

const words={zero:0,no:0,one:1,single:1,a:1,an:1,two:2,three:3,four:4,five:5,six:6,seven:7,eight:8,nine:9,ten:10};

function mentionedCount(text,nouns){
    const match=new RegExp('\\b(\\d{1,3}|'+Object.keys(words).join('|')+')\\s+(?:regular\\s+)?(?:'+nouns+')\\b','i').exec(text);
    if(!match)return undefined;
    return words[match[1].toLowerCase()]??Number(match[1]);
}

function courseBrief(text, jump){
    const hurdles=mentionedCount(text,'(?:hurdles?|obstacles?|barriers?|oversized (?:first )?hurdle)');
    const pickups=mentionedCount(text,'pickups?|coins?|tokens?');
    const statedSpacing=text.match(/\b(staggered|evenly spaced|even spacing)\b/i);
    const oversized=/\b(?:oversized|giant|extra tall)(?:\s+first)?\s+(?:hurdle|barrier|obstacle)s?\b/i.test(text);
    const elevated=/\b(?:elevated|high|floating)\s+(?:pickup|coin|token)s?\b/i.test(text);
    const locked=/\b(?:keep (?:the )?(?:layout|geometry|positions|sizes) fixed|do not (?:repair|change (?:the )?(?:layout|geometry))|don't repair|no repairs)\b/i.test(text);
    const pauseRepair=/\b(?:pause|stop|wait) (?:before (?:the )?(?:first )?repair|after (?:the )?diagnosis)\b/i.test(text);
    if(hurdles===undefined&&pickups===undefined&&!statedSpacing&&!oversized&&!elevated&&!locked&&!pauseRepair)return null;
    return {hurdles:hurdles??(jump?2:0),pickups:pickups??3,
        spacing:statedSpacing?.[1]?.toLowerCase()==='staggered'?'staggered':'even',oversized,elevated,locked,pauseRepair};
}

function issueText(issue){
    if(issue.kind==='blocked-hurdle')return issue.objectId+' held the player in place with '+issue.contacts+' final wall-contact ticks.';
    if(issue.kind==='missed-pickup')return issue.objectId+' was never collected, so the exit stayed locked.';
    return 'The run stopped at x='+Number(issue.lastX).toFixed(1)+' without a confirmed cause.';
}

export function avatarLevelIntent(request) {
    const text=workspaceRequestText(request);
    if (/^(?:resume|continue|finish|reopen|export)\s+(?:my |the |last |saved )*(?:avatar|portrait|character)[ -](?:level|game|mission|course)(?:\s+mission)?[.!?]*$/i.test(text))
        return {resume:true};
    if (text.length>1200 || !/^(?:take|use|turn|convert|make|build|create|put|design|generate)\b/i.test(text)
            || !/\b(?:image|photo|picture|portrait|avatar|character|sprite)\b/i.test(text)
            || !/\b(?:level|platformer|playable (?:game|relay|course)|browser game|(?:relay|jump) course)\b/i.test(text)
            || !/\b(?:offline|export|standalone|playable|playtest|test)\b/i.test(text))
        return null;
    const title=text.match(/\b(?:called|named|titled)\s+["“]([^"”]{1,70})["”]/i)?.[1]?.trim()||'';
    const jump=/\b(?:jump(?:s|ing)?|obstacle(?:s)?|hurdle(?:s)?|platforms|platformer)\b/i.test(text)
        && !/\b(?:no|without)\s+(?:jumps|jumping|obstacles|hurdles)\b/i.test(text);
    const brief=courseBrief(text,jump);
    return {request,title,course:(brief?brief.hurdles>0:jump)?'jump':'relay',...(brief?{brief}:{}),
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
        const result=await this.call('advance',{id:mission.id,avatarId,release:mission.stage==='verified',
            releaseRepair:mission.stage==='diagnosed'});
        this.accept(result);
        if(result.stage==='diagnosed'){
            this.shell().setStageStatus('Repair ready');
            this.announce(issueText(result.diagnosis)+' Continue the mission to apply and retest the repair.');
        }
        if(result.stage==='needs-review'){
            this.shell().setStageStatus('Needs review','needs-input');
            this.announce(issueText(result.diagnosis)+' '+result.refusal);
        }
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
                '. '+(result.repairs?.length?'Passed after '+result.repairs.length+' recorded repair'+(result.repairs.length===1?'':'s')+'. ':'')+
                'The offline game, editable level and evidence are saved.');
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
            diagnosed:'Failed playtest diagnosed. Repair is ready',
            'needs-review':'Needs review. Export is blocked',
            verified:'Playtest passed. Export is ready',
            exported:'Offline game and editable level saved'
        }[mission.stage]||'Ask “Turn this image into a character, build a playable level, then test and export it offline.”';
        this.evidence.hidden=!proof&&!mission.diagnosis&&!mission.repairs?.length;
        this.details.replaceChildren();
        if(mission.diagnosis)this.detail('Diagnosis',issueText(mission.diagnosis));
        if(mission.refusal)this.detail('Review',mission.refusal);
        for(const item of mission.repairs||[]){
            const before=item.diagnosis.before, after=item.after;
            const change=item.diagnosis.kind==='blocked-hurdle'
                ?before.size+' px → '+after.size+' px'
                :'('+before.x+', '+before.y+') → ('+after.x+', '+after.y+')';
            this.detail('Repair '+item.diagnosis.objectId,change);
        }
        if(proof){
            this.detail('Level',mission.title);
            this.detail('Playtest',proof.score+' points · '+proof.pickups+' pickups · '+proof.hurdles+' hurdles');
            this.detail('Checks','Desktop and offline replay agreed');
            if(proof.straightWalkBlocked)this.detail('Straight-walk comparison','Blocked at the first hurdle');
            const labels={html:'Offline game',level:'Editable level',playtest:'Winning playtest',
                repairs:'Repair history',evidence:'Verification receipt'};
            for(const [kind,label] of Object.entries(labels)){
                const file=mission.files?.[kind];
                if(file)this.detail(label,file.path.split(/[\\/]/).at(-1));
            }
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
        this.resumeButton.hidden=!stage||stage==='exported'||stage==='needs-review';
        this.resumeButton.disabled=Boolean(this.pending);
        this.reviewButton.hidden=stage!=='verified'&&stage!=='exported';
        this.reviewButton.disabled=Boolean(this.pending);
        this.filesButton.hidden=stage!=='exported';
        this.filesButton.disabled=Boolean(this.pending);
    }
}
