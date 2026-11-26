import {readBase64} from './media-workshop.js';

const MAX_VIDEO_BYTES=24*1024*1024;
const FPS=8, DURATION=10;

function videoEvent(video,type) {
    return new Promise((resolve,reject)=>{
        const clean=()=>{
            clearTimeout(timer);
            video.removeEventListener(type,complete);
            video.removeEventListener('error',failed);
        };
        const complete=()=>{clean();resolve();};
        const failed=()=>{clean();reject(Error('The desktop could not decode this source recording. Try H.264 MP4 or VP8 WebM.'));};
        const timer=setTimeout(failed,10000);
        video.addEventListener(type,complete,{once:true});
        video.addEventListener('error',failed,{once:true});
    });
}

async function frameAt(video,current,context) {
    if(Math.abs(video.currentTime-current)>1e-6) {
        const ready=videoEvent(video,'seeked');
        video.currentTime=current;
        await ready;
    }
    const scale=Math.min(640/video.videoWidth,360/video.videoHeight);
    const width=video.videoWidth*scale, height=video.videoHeight*scale;
    context.fillStyle='#000';
    context.fillRect(0,0,640,360);
    context.drawImage(video,(640-width)/2,(360-height)/2,width,height);
    return context.canvas.toDataURL('image/jpeg',.78).split(',')[1];
}

export class DeskRecordingPanel {
    constructor(environment) {
        this.environment=environment;
        this.root=environment.find('environment-desk');
        this.root.innerHTML=`
          <summary>Desk recording mission</summary>
          <details id="desk-inputs" open>
            <summary>Footage and mission setup</summary>
          <div class="environment-plan-fields">
            <label class="spatial-file">Select existing footage<input id="desk-footage" type="file" accept=".mp4,.mov,.webm"></label>
            <label>Clip starts · s <input id="desk-start-s" type="number" value="0" min="0" step=".125"></label>
            <label>Anchor frame · 0–79 <input id="desk-anchor-sample" type="number" value="0" min="0" max="79" step="1"></label>
            <button id="desk-anchor-frame">Show anchor frame</button>
          </div>
          <p id="desk-clip-name">Choose a local clip with ten seconds available.</p>
          <div class="environment-plan-fields">
            <label class="spatial-file">Load mission setup JSON<input id="desk-setup-file" type="file" accept=".json"></label>
          </div>
          <label class="desk-setup-label">Landmarks and availability JSON<textarea id="desk-setup" rows="4"></textarea></label>
          <div class="environment-plan-fields">
            <button id="desk-start">Prepare mission</button>
          </div>
          </details>
          <div class="environment-plan-fields">
            <button id="desk-advance" disabled>Verify next step</button>
            <button id="desk-run" disabled>Run remaining steps</button>
            <button id="desk-footage-only" hidden>Continue with footage and draft</button>
          </div>
          <p id="desk-status" role="status">Open an environment to anchor a recorded view.</p>
          <ol id="desk-steps" hidden></ol>
          <div class="environment-plan-fields">
            <button id="desk-play" disabled>Play 10-second preview</button>
            <button id="desk-avi" disabled>Save preview AVI</button>
            <button id="desk-ics" disabled>Save calendar draft</button>
            <output id="desk-frame-time"></output>
          </div>
          <img id="desk-frame" hidden alt="Recorded camera frame">`;
        this.get('desk-footage').onchange=event=>{
            this.file=event.target.files[0];
            this.get('desk-clip-name').textContent=this.file?.name||'Choose a local clip with ten seconds available.';
        };
        this.get('desk-setup-file').onchange=event=>this.run(async()=>{
            const file=event.target.files[0];
            event.target.value='';
            if(!file)return;
            if(file.size>128*1024)throw Error('The mission setup exceeds 128 KiB.');
            this.get('desk-setup').value=JSON.stringify(JSON.parse(await file.text()),null,2);
        });
        this.get('desk-footage-only').onclick=()=>this.run(async()=>{
            await this.step(true);
            return this.finish();
        });
        this.get('desk-anchor-frame').onclick=()=>this.run(()=>this.showAnchor());
        this.get('desk-start').onclick=()=>this.run(()=>this.start());
        this.get('desk-advance').onclick=()=>this.run(()=>this.step());
        this.get('desk-run').onclick=()=>this.run(()=>this.finish());
        this.get('desk-play').onclick=()=>this.run(()=>this.play());
        for(const format of ['avi','ics']) this.get('desk-'+format).onclick=()=>this.run(()=>this.save(format));
        this.root.ontoggle=()=>{
            if(this.root.open&&!this.mission) this.restore().catch(error=>this.status(error.message));
        };
    }

    get(id) {return this.root.querySelector('#'+id);}
    status(text) {this.get('desk-status').textContent=text;}
    call(command,args={}) {return this.environment.call('desk-'+command,args);}
    run(action) {
        return this.environment.run(async()=>{
            try {await action();}
            catch(error){this.status(error.message.replace(/^Error invoking remote method '[^']+': (?:Error: )?/,''));}
        });
    }

    async decoder(action) {
        const source=this.file;
        if(!source||source.size>MAX_VIDEO_BYTES) throw Error('Choose a local clip no larger than 24 MiB.');
        const start=this.get('desk-start-s').valueAsNumber;
        if(!Number.isFinite(start)||start<0) throw Error('Enter a valid clip start time.');
        const video=document.createElement('video'), uri=URL.createObjectURL(source);
        video.preload='auto';
        video.muted=true;
        const loaded=videoEvent(video,'loadeddata');
        video.src=uri;
        try {
            await loaded;
            if(!Number.isFinite(video.duration)||video.duration+1e-4<start+DURATION)
                throw Error('Choose a source interval with ten seconds available.');
            const canvas=document.createElement('canvas');
            canvas.width=640;canvas.height=360;
            return await action(video,canvas.getContext('2d',{alpha:false}),start,source);
        } finally {
            video.removeAttribute('src');
            video.load();
            URL.revokeObjectURL(uri);
        }
    }

    anchorSample() {
        const sample=this.get('desk-anchor-sample').valueAsNumber;
        if(!Number.isInteger(sample)||sample<0||sample>=FPS*DURATION)throw Error('Choose an anchor frame from 0 to 79.');
        return sample;
    }

    async showAnchor() {
        const sample=this.anchorSample();
        await this.decoder(async(video,ctx,when)=>{
            const time=when+sample/FPS;
            this.showFrame(await frameAt(video,time,ctx),time);
            this.status('Sample '+sample+' at '+time.toFixed(3)+' s · use its 640 × 360 pixel coordinates.');
        });
    }

    showFrame(encoded,second) {
        const img=this.get('desk-frame');
        img.src='data:image/jpeg;base64,'+encoded;
        img.hidden=false;
        this.get('desk-frame-time').textContent=second.toFixed(3)+' s';
    }

    async start() {
        if(!this.environment.catalog) await this.environment.load();
        if(!this.environment.catalog) throw Error('Open a 3D environment before preparing the recording.');
        const environmentId=this.environment.catalog.id;
        const text=this.get('desk-setup').value.trim();
        if(!text) throw Error('Supply the landmark and calendar availability JSON.');
        const setup=JSON.parse(text);
        const anchorSample=this.anchorSample();
        this.status('Decoding ten seconds of local footage…');
        await this.decoder(async(video,ctx,start,file)=>{
            const frames=[];
            for(let i=0;i<FPS*DURATION;i++) {
                frames.push(await frameAt(video,start+i/FPS,ctx));
                if(i%8===0)this.status('Reading source sample '+(i+1)+' of '+FPS*DURATION+'…');
            }
            this.showFrame(frames[anchorSample],start+anchorSample/FPS);
            const chosen=await readBase64(file,MAX_VIDEO_BYTES);
            this.render(await this.call('prepare',{environmentId,setup,frames,startS:start,anchorSample,sourceName:file.name,video:chosen}));
        });
    }

    async restore() {
        const record=await this.call('read');
        if(record.id&&!this.mission)this.render(record);
    }

    async step(footageOnly=false) {
        if(!this.mission?.id)throw Error('Prepare a desk mission first.');
        try {
            this.render(await this.call('advance',{id:this.mission.id,footageOnly}));
        } catch(error) {
            if(this.mission.stage==='prepared') this.get('desk-footage-only').hidden=false;
            throw error;
        }
    }

    async finish() {
        if(!this.mission?.id) await this.restore();
        if(!this.mission?.id) await this.start();
        for(let i=0;i<4 && !this.mission.draft;i++)await this.step();
        return this.mission;
    }

    render(record) {
        this.mission=record;
        const list=this.get('desk-steps');
        const items=[
            ['Source',record.source.name+' · '+record.source.durationS+' s excerpt'],
            ['Camera anchor',record.anchor?
                record.anchor.heldOut.maxPx.toFixed(2)+' px held-out maximum · desk sightline and three doorway rays clear':
                record.anchorFailure||'Waiting'],
            ['Desk recording',record.recording?record.recording.frameCount+' decoded samples from the retained clip':'Waiting'],
            ['Preview',record.preview?record.preview.durationS+' s, silent, verified AVI':'Waiting'],
            ['Calendar draft',record.draft?new Intl.DateTimeFormat(undefined,{dateStyle:'medium',timeStyle:'short',timeZone:record.draft.timezone})
                .format(new Date(record.draft.start))+' · '+record.draft.timezone+' · local draft':'Waiting']
        ];
        list.replaceChildren(...items.map(([name,detail])=>{
            const line=document.createElement('li');
            line.textContent=name+' · '+detail;
            return line;
        }));
        list.hidden=false;
        const done=Boolean(record.draft);
        if(done) this.get('desk-inputs').open=false;
        this.get('desk-footage-only').hidden=true;
        this.get('desk-advance').disabled=this.get('desk-run').disabled=done;
        this.get('desk-play').disabled=this.get('desk-avi').disabled=!record.preview;
        this.get('desk-ics').disabled=!record.draft;
        this.status(done?
            'Calendar draft ready for review.'+(record.anchorFailure?' Camera anchor unresolved.':''):
            'Mission stage · '+record.stage);
    }

    async play() {
        this.status('Loading verified preview…');
        const result=await this.call('artifact',{id:this.mission.id,format:'frames'});
        const frames=result.jpegFrames, fps=result.framesPerSecond;
        const start=performance.now();
        this.status('Playing the silent preview.');
        for(let i=0;i<frames.length;i++) {
            this.showFrame(frames[i],i/fps);
            await new Promise(resolve=>setTimeout(resolve,Math.max(0,start+(i+1)*1000/fps-performance.now())));
        }
        this.status('Preview complete · 10 seconds.');
    }

    async save(format) {
        const file=await this.call('export',{id:this.mission.id,format});
        this.status('Saved '+format.toUpperCase()+' to '+file.path);
    }
}
