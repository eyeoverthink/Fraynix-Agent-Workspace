import {readBase64} from './media-workshop.js';
import {CreativeProjectPanel} from './creative-project.js';

const lanes={PICTURE:'Picture',OVERLAY:'Overlay',AUDIO1:'Audio 1',AUDIO2:'Audio 2'};
const formats={avi:'AVI',wav:'WAV',png:'PNG',json:'Timeline JSON'};
const isAudio=lane=>lane.startsWith('AUDIO');

export class TimelinePanel {
    constructor(request,back) {
        this.request=request;
        this.assets=[];
        this.strips=[];
        this.root=document.createElement('section');
        this.root.id='timeline-editor';
        this.root.hidden=true;
        this.root.innerHTML=`
          <div class="media-actions">
            <button id="timeline-back">Tile editor</button>
            <label class="media-file">Import PNG, AVI or WAV<input id="timeline-import" type="file" accept=".png,.avi,.wav"></label>
            <button id="timeline-attach">Add latest generated media</button>
            <button id="timeline-previous" hidden>Open previous revision</button>
          </div>
          <form id="timeline-add">
            <label>Asset<select id="timeline-asset"></select></label>
            <label>Track<select id="timeline-lane"></select></label>
            <button id="timeline-add-strip">Add strip</button>
          </form>
          <div id="timeline-tracks" aria-label="Timeline tracks"></div>
          <form id="timeline-render">
            <label>Timeline name<input id="timeline-title" maxlength="90" value="Untitled timeline" required></label>
            <button class="primary" id="timeline-save">Save and render revision</button>
          </form>
          <p id="timeline-status" role="status">Import files or add generated media. Time is in frames at 24 fps.</p>
          <div id="timeline-output" hidden>
            <img id="timeline-picture" alt="Rendered timeline frame">
            <label id="timeline-scrub-label">Preview frame <output id="timeline-time"></output>
              <input id="timeline-scrub" type="range" min="0" max="0" step="1" value="0"></label>
            <div class="media-actions" id="timeline-exports"></div>
            <audio id="timeline-audio" controls hidden></audio>
          </div>`;
        document.querySelector('#canvas-surface').before(this.root);
        this.project=new CreativeProjectPanel(request,{
            getCut:()=>this.revision,
            getAssets:()=>this.assets,
            openCut:id=>this.run(async()=>{
                this.present(await this.call('open',{id}),'Opened and verified this project cut.');
                this.find('#timeline-render').scrollIntoView({block:'nearest'});
            })
        });
        this.find('#timeline-add').before(this.project.root);
        this.find('#timeline-back').onclick=back;
        this.asset=this.find('#timeline-asset');
        this.lane=this.find('#timeline-lane');
        this.preview=this.find('#timeline-picture');
        this.audio=this.find('#timeline-audio');
        this.lane.replaceChildren(...Object.entries(lanes).map(([id,label])=>new Option(label,id)));
        this.find('#timeline-import').onchange=event=>{
            const source=event.target.files[0];
            event.target.value='';
            if (source) this.run(async()=>{
                const asset=await this.call('import',{filename:source.name,base64:await readBase64(source,6_000_000)});
                await this.reloadAssets();
                this.asset.value=asset.id;
                this.tell('Imported and checked '+asset.source.title+'.');
            });
        };
        this.find('#timeline-attach').onclick=()=>this.attach();
        this.find('#timeline-add').onsubmit=event=>{
            event.preventDefault();
            const asset=this.assets.find(item=>item.id===this.asset.value);
            const lane=this.lane.value;
            if (!asset) return;
            const length=this.sourceLength(asset,lane);
            if (!length || (lane==='OVERLAY' && asset.source.format!=='png')) {
                this.tell('Choose a compatible asset for this track.');
                return;
            }
            const start=Math.max(0,...this.strips.filter(row=>row.lane===lane).map(row=>row.start+row.length));
            if (start>=144 || this.strips.length>=20) {
                this.tell('Trim a strip to make room on the timeline.');
                return;
            }
            const row={assetId:asset.id,lane,start,in:0,length:Math.min(12,length,144-start)};
            if (lane==='OVERLAY') Object.assign(row,{x:12,y:12,size:100,opacity:100});
            if (isAudio(lane)) row.gain=.8;
            this.strips.push(row);
            this.redraw();
            this.draftChanged();
        };
        this.find('#timeline-title').oninput=()=>this.draftChanged();
        this.find('#timeline-render').onsubmit=event=>{
            event.preventDefault();
            this.run(async()=>{
                const form=this.find('#timeline-tracks');
                if ([...form.querySelectorAll('input')].some(input=>!input.reportValidity())) return;
                const input={title:this.find('#timeline-title').value,strips:this.strips};
                if (this.revision) input.parent=this.revision.id;
                this.tell('Rendering and checking the new timeline revision…');
                this.present(await this.call('render',input),'Saved and verified a new timeline revision.');
            });
        };
        this.find('#timeline-previous').onclick=()=>this.run(async()=>{
            this.present(await this.call('open',{id:this.revision.definition.parent}),'Opened and verified the previous revision.');
        });
        for (const [format,name] of Object.entries(formats)) {
            const button=document.createElement('button');
            button.dataset.timelineFormat=format;
            button.textContent='Export '+name;
            button.onclick=()=>this.run(async()=>{
                const exported=await this.call('export',{id:this.revision.id,format});
                this.tell('Exported '+name+' to '+exported.path);
            });
            this.find('#timeline-exports').append(button);
        }
        const listen=document.createElement('button');
        listen.id='timeline-listen';
        listen.textContent='Listen to mix';
        listen.onclick=()=>this.run(async()=>{
            const {base64}=await this.call('preview-audio',{id:this.revision.id});
            this.clearAudio();
            const data=Uint8Array.from(atob(base64),value=>value.charCodeAt(0));
            this.audio.src=URL.createObjectURL(new Blob([data],{type:'audio/wav'}));
            this.audio.hidden=false;
            await this.audio.play();
            this.tell('Playing the verified timeline mix.');
        });
        this.find('#timeline-exports').append(listen);
        this.scrub=this.find('#timeline-scrub');
        this.scrub.onchange=()=>this.run(async()=>{
            const index=this.scrub.valueAsNumber;
            const res=await this.call('frame',{id:this.revision.id,frame:index});
            this.preview.src='data:image/png;base64,'+res.png;
            this.find('#timeline-time').textContent=index+' · '+(index/24).toFixed(3)+' s';
        });
        this.redraw();
        this.controls();
    }

    find(selector) { return this.root.querySelector(selector); }
    call(command,args={}) { return this.request('timeline',{command,args}); }
    tell(message) { this.find('#timeline-status').textContent=message; }

    show(visible) {
        this.root.hidden=!visible;
        if (!visible) {
            this.clearAudio();
            this.project.close();
        }
        if (visible && !this.initialized) this.run(async()=>{
            await this.reloadAssets();
            const latest=await this.call('latest');
            if (latest.id) this.present(latest,'Reopened and verified the saved timeline.');
            await this.project.load();
            this.initialized=true;
        });
    }

    attach(recipeId) {
        return this.run(async()=>{
            const result=await this.call('attach',recipeId?{id:recipeId}:{});
            await this.reloadAssets();
            this.asset.value=result.assets[0].id;
            this.tell('Added the verified generated video, audio and poster.');
        });
    }

    async reloadAssets() {
        const previous=this.asset.value;
        this.assets=(await this.call('assets')).assets;
        this.asset.replaceChildren(new Option('Choose a source',''),...this.assets.map(asset=>{
            return new Option(asset.source.title,asset.id);
        }));
        if (this.assets.some(asset=>asset.id===previous)) this.asset.value=previous;
        this.project.refreshSources();
    }

    sourceLength(asset,lane) { return isAudio(lane)?asset.soundFrames:asset.pictureFrames; }

    clearAudio() {
        if (this.audio.src.startsWith('blob:')) URL.revokeObjectURL(this.audio.src);
        this.audio.pause();
        this.audio.removeAttribute('src');
        this.audio.hidden=true;
    }

    present(result,message) {
        this.clearAudio();
        this.revision=result;
        this.strips=result.definition.strips.map(row=>({...row}));
        this.find('#timeline-title').value=result.definition.title;
        this.find('#timeline-previous').hidden=!result.definition.parent;
        this.find('#timeline-output').hidden=false;
        this.preview.src='data:image/png;base64,'+result.posterPng;
        this.scrub.max=result.metrics.frames-1;
        this.scrub.value=result.metrics.posterFrame;
        this.find('#timeline-time').textContent=result.metrics.posterFrame+' · '+(result.metrics.posterFrame/24).toFixed(3)+' s';
        this.redraw();
        this.tell(message);
        if (this.project.state) this.project.drawHome();
    }

    draftChanged() {
        this.drawBlocks();
        this.tell(this.revision?'Draft changed. Preview and exports still show the saved revision.':'Draft updated. Time is in frames at 24 fps.');
    }

    redraw() {
        const container=this.find('#timeline-tracks');
        container.replaceChildren();
        for (const [lane,title] of Object.entries(lanes)) {
            const track=document.createElement('section');
            track.className='timeline-track';
            track.dataset.lane=lane;
            const label=document.createElement('h3');
            label.textContent=title;
            const bars=document.createElement('div');
            bars.className='timeline-bars';
            track.append(label,bars);
            this.strips.filter(strip=>strip.lane===lane).forEach(strip=>track.append(this.stripEditor(strip)));
            container.append(track);
        }
        this.drawBlocks();
        this.controls();
    }

    drawBlocks() {
        const extent=Math.max(24,...this.strips.map(row=>row.start+row.length));
        for (const track of this.root.querySelectorAll('.timeline-track')) {
            const bars=track.querySelector('.timeline-bars');
            bars.replaceChildren();
            for (const row of this.strips.filter(strip=>strip.lane===track.dataset.lane)) {
                if (!Number.isFinite(row.start+row.length)) continue;
                const span=document.createElement('span');
                span.style.left=100*row.start/extent+'%';
                span.style.width=100*row.length/extent+'%';
                span.textContent=this.assetTitle(row);
                span.title='Frame '+row.start+' to '+(row.start+row.length);
                bars.append(span);
            }
        }
    }

    assetTitle(row) { return this.assets.find(asset=>asset.id===row.assetId)?.source.title||'Unavailable asset'; }

    stripEditor(strip) {
        const row=document.createElement('div');
        row.className='timeline-strip';
        const title=document.createElement('strong');
        title.textContent=this.assetTitle(strip);
        row.append(title);
        const add=(key,label,min,max,step=1)=>{
            const field=document.createElement('label');
            field.textContent=label;
            const input=document.createElement('input');
            Object.assign(input,{type:'number',value:strip[key],min,max,step,required:true});
            input.dataset.field=key;
            input.oninput=()=>{
                strip[key]=input.valueAsNumber;
                this.draftChanged();
            };
            field.append(input);
            row.append(field);
        };
        add('start','Start',0,143);
        add('in','Source in',0,240);
        add('length','Frames',1,144);
        if (strip.lane==='OVERLAY') {
            add('x','X',-640,640);
            add('y','Y',-360,360);
            add('size','Size %',1,200);
            add('opacity','Opacity %',1,100);
        }
        if (isAudio(strip.lane)) add('gain','Gain',0,1,.01);
        const remove=document.createElement('button');
        remove.textContent='Remove';
        remove.onclick=()=>{
            this.strips=this.strips.filter(item=>item!==strip);
            this.redraw();
            this.draftChanged();
        };
        row.append(remove);
        return row;
    }

    controls() {
        this.find('#timeline-save').disabled=Boolean(this.pending)||!this.strips.length;
        this.find('#timeline-add-strip').disabled=Boolean(this.pending)||!this.assets.length;
        for (const button of this.find('#timeline-exports').querySelectorAll('button'))
            button.disabled=Boolean(this.pending)||!this.revision;
    }

    run(action) {
        if (this.pending) return this.pending;
        this.pending=Promise.resolve().then(action).catch(error=>{
            this.tell(error.message.replace(/^Error invoking remote method '[^']+': (?:Error: )?/,''));
        }).finally(()=>{
            this.pending=null;
            this.controls();
        });
        this.controls();
        return this.pending;
    }
}
