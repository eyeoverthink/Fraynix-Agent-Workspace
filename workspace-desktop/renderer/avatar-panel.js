import {readBase64} from './media-workshop.js';

const SAMPLE_RATE=24000;
const MAX_CLIP_BYTES=64*1024*1024;
const INPUT_LIMIT=1_800_000;

export function isAvatarRequest(text) {
    const creation=/^(?:make|create|build|generate|animate|convert|turn)\b.{0,120}\b(?:avatar|talking (?:head|portrait)|(?:character|sprite)\s+from (?:an? |the |my |this )?(?:image|photo|picture|portrait|face|video|clip))\b/i;
    const conversion=/^(?:turn|convert|make)\b.{0,70}\b(?:image|photo|picture|portrait|face|video|clip|me)\b.{0,20}\binto (?:a |an )?(?:game )?(?:character|sprite)\b/i;
    return creation.test(text)||conversion.test(text);
}

function waitFor(media,event) {
    return new Promise((resolve,reject)=>{
        const timeout=setTimeout(()=>finish(new Error('The media took too long to decode.')),12000);
        const ready=()=>finish();
        const failed=()=>finish(new Error('This media format could not be decoded. Try PNG, JPEG, MP4 or WAV.'));
        function finish(error) {
            clearTimeout(timeout);
            media.removeEventListener(event,ready);
            media.removeEventListener('error',failed);
            if(error)reject(error);
            else resolve();
        }
        media.addEventListener(event,ready,{once:true});
        media.addEventListener('error',failed,{once:true});
    });
}

async function canonicalImage(source,width,height) {
    if(width<8||height<8)throw Error('Choose an image at least eight pixels wide and high.');
    const canvas=document.createElement('canvas');
    let scale=Math.min(1,800/Math.max(width,height));
    const context=canvas.getContext('2d');
    for(let attempt=0;attempt<4;attempt++){
        canvas.width=Math.max(8,Math.round(width*scale));
        canvas.height=Math.max(8,Math.round(height*scale));
        context.drawImage(source,0,0,canvas.width,canvas.height);
        const png=await new Promise(resolve=>canvas.toBlob(resolve,'image/png'));
        if(png?.size<=INPUT_LIMIT)return readBase64(png,INPUT_LIMIT);
        scale*=.78;
    }
    throw Error('The portrait is too large to store in this project.');
}

async function canonicalAudio(file) {
    if(file.size>MAX_CLIP_BYTES)throw Error('Choose an audio or video file under 64 MiB.');
    const context=new OfflineAudioContext(1,1,SAMPLE_RATE);
    let decoded;
    try{decoded=await context.decodeAudioData(await file.arrayBuffer());}
    catch{throw Error('Audio could not be decoded. Import the voice separately as a WAV file.');}
    const count=Math.min(decoded.length,SAMPLE_RATE*30);
    if(count<SAMPLE_RATE/8)throw Error('Choose at least 0.125 seconds of audio.');
    const samples=new Float32Array(count);
    for(let channel=0;channel<decoded.numberOfChannels;channel++){
        const track=decoded.getChannelData(channel);
        for(let i=0;i<count;i++)samples[i]+=track[i]/decoded.numberOfChannels;
    }
    const output=new ArrayBuffer(44+count*2);
    const view=new DataView(output);
    const tag=(offset,text)=>new Uint8Array(output,offset,text.length).set(new TextEncoder().encode(text));
    tag(0,'RIFF');view.setUint32(4,output.byteLength-8,true);tag(8,'WAVE');tag(12,'fmt ');
    view.setUint32(16,16,true);view.setUint16(20,1,true);view.setUint16(22,1,true);
    view.setUint32(24,SAMPLE_RATE,true);view.setUint32(28,SAMPLE_RATE*2,true);
    view.setUint16(32,2,true);view.setUint16(34,16,true);tag(36,'data');
    view.setUint32(40,count*2,true);
    for(let i=0;i<count;i++)view.setInt16(44+i*2,Math.round(Math.max(-1,Math.min(1,samples[i]))*32767),true);
    return readBase64(new Blob([output]),SAMPLE_RATE*60+44);
}

function wavUrl(encoded) {
    const raw=Uint8Array.from(atob(encoded),character=>character.charCodeAt(0));
    return URL.createObjectURL(new Blob([raw],{type:'audio/wav'}));
}

export class AvatarPanel {
    constructor(request,useTile) {
        this.request=request;
        this.useTile=useTile;
        this.root=document.createElement('section');
        this.root.id='avatar-studio';
        this.root.hidden=true;
        this.root.innerHTML=`
          <div class="avatar-toolbar">
            <label>Character name<input id="avatar-name" maxlength="64" value="My character"></label>
            <label>Saved avatars<select id="avatar-saved"><option value="">Choose a saved avatar</option></select></label>
            <label class="media-file">Open avatar project<input id="avatar-project" type="file" accept=".json,application/json" aria-label="Open avatar project"></label>
          </div>
          <div class="avatar-grid">
            <section class="avatar-card">
              <h3>Portrait source</h3>
              <div class="avatar-actions">
                <label class="media-file">Import image<input id="avatar-image" type="file" accept="image/png,image/jpeg,image/webp" aria-label="Import avatar image"></label>
                <label class="media-file">Import video<input id="avatar-clip" type="file" accept="video/*,.mov,.mp4" aria-label="Import avatar video"></label>
              </div>
              <div id="avatar-video-controls" hidden>
                <video id="avatar-video" muted playsinline controls preload="auto" aria-label="Source video"></video>
                <div class="avatar-actions">
                  <label>Frame time · s<input id="avatar-time" type="number" min="0" step="0.001" value="0"></label>
                  <button id="avatar-frame">Use this frame</button>
                  <button id="avatar-clip-audio">Use clip audio</button>
                </div>
              </div>
              <canvas id="avatar-source" hidden aria-label="Portrait with head crop and mouth position"></canvas>
              <div class="avatar-actions">
                <button id="avatar-crop">Draw head crop</button>
                <button id="avatar-mouth">Mark mouth center</button>
              </div>
              <p id="avatar-mark-help">Frame the hair, ears and chin inside the crop.</p>
              <details>
                <summary>Adjust crop and mouth numerically</summary>
                <div class="avatar-rig">
                  <label>Left %<input data-rig="x" type="number" min="0" max="99" step="any"></label>
                  <label>Top %<input data-rig="y" type="number" min="0" max="99" step="any"></label>
                  <label>Width %<input data-rig="w" type="number" min="8" max="100" step="any"></label>
                  <label>Height %<input data-rig="h" type="number" min="12" max="100" step="any"></label>
                  <label>Mouth X %<input data-rig="mouthX" type="number" min="0" max="100" step="any"></label>
                  <label>Mouth Y %<input data-rig="mouthY" type="number" min="0" max="100" step="any"></label>
                </div>
              </details>
            </section>
            <section class="avatar-card">
              <h3>Character and voice</h3>
              <div class="avatar-actions">
                <label class="media-file">Add audio<input id="avatar-audio-file" type="file" accept="audio/*,.wav,.m4a,.mp3" aria-label="Add avatar audio"></label>
                <button id="avatar-clear-audio">Remove audio</button>
                <button id="avatar-build" class="primary">Build &amp; save</button>
              </div>
              <p id="avatar-audio-note">Optional. Use up to 30 seconds of supplied audio.</p>
              <img id="avatar-portrait" hidden alt="Animated portrait preview">
              <audio id="avatar-play" controls hidden aria-label="Play talking portrait"></audio>
              <div id="avatar-assets" hidden>
                <figure><img id="avatar-sprite" alt="Transparent character sprite"><figcaption>Idle sprite</figcaption></figure>
                <figure><img id="avatar-walk" alt="Four walking poses"><figcaption>4 walk frames · 96 × 112 each</figcaption></figure>
              </div>
              <div id="avatar-exports" class="avatar-actions" hidden>
                <button data-export="sprite">Export sprite PNG</button>
                <button data-export="walk">Export walk sheet</button>
                <button data-export="portrait">Export portrait</button>
                <button data-export="avi">Export talking video</button>
                <button data-export="manifest">Export asset manifest</button>
                <button data-export="project">Export editable avatar</button>
                <button id="avatar-tile">Use in Tile Workshop</button>
              </div>
            </section>
          </div>
          <p id="avatar-status" role="status">Import a portrait image or choose a frame from a clip.</p>`;
        document.querySelector('#canvas-surface').before(this.root);
        this.source=this.find('source');
        this.video=this.find('video');
        this.audio=this.find('play');
        this.name=this.find('name');
        this.saved=this.find('saved');
        this.rigFields=[...this.root.querySelectorAll('[data-rig]')];
        this.markMode='crop';
        this.find('image').onchange=event=>this.file(event,file=>this.importImage(file));
        this.find('clip').onchange=event=>this.file(event,file=>this.importVideo(file));
        this.find('project').onchange=event=>this.file(event,async file=>{
            const base64=await readBase64(file,4_500_000);
            await this.apply(await this.call('import',{base64}));
            await this.loadLibrary();
        });
        this.find('audio-file').onchange=event=>this.file(event,file=>this.addAudio(file));
        this.find('clear-audio').onclick=()=>{this.wav='';this.changed();};
        this.find('clip-audio').onclick=()=>this.run(()=>this.addAudio(this.clip));
        this.find('frame').onclick=()=>this.run(()=>this.takeFrame());
        this.video.addEventListener('seeked',()=>{if(!this.seeking)this.find('time').value=this.video.currentTime.toFixed(3);});
        this.find('crop').onclick=()=>this.mark('crop');
        this.find('mouth').onclick=()=>this.mark('mouth');
        this.find('build').onclick=()=>this.run(()=>this.build());
        this.saved.onchange=()=>this.run(async()=>{
            if(this.saved.value)await this.apply(await this.call('open',{id:this.saved.value}));
        });
        this.name.oninput=()=>this.changed();
        for(const field of this.rigFields)field.onchange=()=>{
            if(!field.reportValidity()||!Number.isFinite(field.valueAsNumber))return;
            this.rig[field.dataset.rig]=field.valueAsNumber/100;
            this.changed();this.draw();
        };
        this.source.onpointerdown=event=>this.startMark(event);
        this.source.onpointermove=event=>this.dragMark(event);
        this.source.onpointerup=event=>this.finishMark(event);
        this.source.onpointercancel=()=>{this.drag=null;this.draw();};
        this.find('tile').onclick=()=>this.run(async()=>{
            const sprite=await this.call('tile',{id:this.output.id});
            await this.useTile(sprite);
        });
        for(const button of this.root.querySelectorAll('[data-export]'))button.onclick=()=>this.run(async()=>{
            this.tell('Verifying and exporting the saved avatar.');
            const file=await this.call('export',{id:this.output.id,kind:button.dataset.export});
            this.tell('Saved '+file.path);
        });
        this.audio.addEventListener('play',()=>this.play());
        for(const event of ['seeked','pause','ended'])this.audio.addEventListener(event,()=>this.showPose());
        this.update();
    }

    find(id){return this.root.querySelector('#avatar-'+id);}
    call(command,args={}){return this.request('avatar',{command,args});}
    tell(text){this.find('status').textContent=text;}
    get hasSource(){return Boolean(this.image);}
    chooseImage(){this.find('image').click();}

    show(visible){
        this.root.hidden=!visible;
        if(!visible){this.audio.pause();this.video.pause();return;}
        if(this.loaded||this.pending)return;
        this.run(async()=>{
            await this.loadLibrary();
            this.loaded=true;
            const latest=await this.call('open');
            if(latest.id)await this.apply(latest);
        });
    }

    run(action){
        if(this.pending)return this.pending;
        this.pending=Promise.resolve().then(action).catch(error=>{
            this.tell(error.message.replace(/^Error invoking remote method '[^']+': (?:Error: )?/,''));
        }).finally(()=>{this.pending=null;this.update();this.onSource?.();});
        this.update();
        return this.pending;
    }

    file(event,action){
        const file=event.currentTarget.files[0];
        event.currentTarget.value='';
        if(file)this.run(()=>action(file));
    }

    async importImage(file){
        if(file.size>24*1024*1024)throw Error('Choose an image under 24 MiB.');
        const bitmap=await createImageBitmap(file);
        try{
            await this.setImage(await canonicalImage(bitmap,bitmap.width,bitmap.height));
            this.wav='';this.changed();
        }
        finally{bitmap.close();}
        this.tell('Image loaded. Adjust the head and mouth, then build your character.');
    }

    async importVideo(file){
        if(file.size>MAX_CLIP_BYTES)throw Error('Choose a clip under 64 MiB.');
        if(this.clipUrl)URL.revokeObjectURL(this.clipUrl);
        this.clip=file;
        this.clipUrl=URL.createObjectURL(file);
        const ready=waitFor(this.video,'loadeddata');
        this.video.src=this.clipUrl;
        await ready;
        if(!Number.isFinite(this.video.duration)||this.video.duration<=0)throw Error('The video duration is unavailable.');
        this.wav='';this.changed();
        this.find('time').max=String(Math.max(0,this.video.duration-.001));
        this.find('time').value='0';
        this.find('video-controls').hidden=false;
        this.tell('Choose a frame time or seek in the video, then select Use this frame.');
    }

    async takeFrame(){
        const time=this.find('time');
        if(!time.reportValidity())return;
        const seconds=time.valueAsNumber;
        if(!Number.isFinite(seconds))throw Error('Enter a frame time in seconds.');
        this.video.pause();
        if(Math.abs(this.video.currentTime-seconds)>.0005){
            this.seeking=true;
            try{
                const ready=waitFor(this.video,'seeked');
                this.video.currentTime=seconds;
                await ready;
            }finally{this.seeking=false;}
        }
        await this.setImage(await canonicalImage(this.video,this.video.videoWidth,this.video.videoHeight));
        this.tell('Frame at '+seconds.toFixed(3)+' s loaded. Adjust the head and mouth marks if needed.');
    }

    async addAudio(file){
        this.wav=await canonicalAudio(file);
        this.changed();
        this.tell('Audio ready. Build to animate this portrait with the supplied sound.');
    }

    async setImage(base64,rig){
        const image=new Image();
        image.src='data:image/png;base64,'+base64;
        await image.decode();
        this.image=base64;
        this.sourceImage=image;
        this.rig=rig||{x:.24,y:.06,w:.52,h:.86,mouthX:.5,mouthY:.68};
        this.source.width=image.naturalWidth;
        this.source.height=image.naturalHeight;
        this.source.hidden=false;
        this.changed();
        this.draw();
    }

    mark(mode){
        this.markMode=mode;
        this.find('mark-help').textContent=mode==='crop'?'Drag from above the hair to below the chin.':'Click the center of the lips.';
        this.update();
    }

    position(event){
        const box=this.source.getBoundingClientRect();
        const unit=n=>Math.min(1,Math.max(0,n));
        return {x:unit((event.clientX-box.left)/box.width),y:unit((event.clientY-box.top)/box.height)};
    }

    startMark(event){
        if(this.pending||event.button!==0)return;
        const point=this.position(event);
        if(this.markMode==='mouth'){
            Object.assign(this.rig,{mouthX:point.x,mouthY:point.y});
            this.changed();this.draw();
            return;
        }
        this.source.setPointerCapture(event.pointerId);
        this.drag={from:point,to:point};
    }

    dragMark(event){
        if(!this.drag)return;
        this.drag.to=this.position(event);
        this.draw();
    }

    crop(){
        const {from,to}=this.drag;
        return {x:Math.min(from.x,to.x),y:Math.min(from.y,to.y),w:Math.abs(to.x-from.x),h:Math.abs(to.y-from.y)};
    }

    finishMark(event){
        if(!this.drag)return;
        this.drag.to=this.position(event);
        const box=this.crop();
        this.drag=null;
        if(box.w>=.08&&box.h>=.12){
            this.rig={...box,mouthX:box.x+box.w*.5,mouthY:box.y+box.h*.75};
            this.changed();
            this.mark('mouth');
        }
        this.draw();
    }

    draw(){
        const {width,height}=this.source;
        const g=this.source.getContext('2d');
        g.clearRect(0,0,width,height);
        g.drawImage(this.sourceImage,0,0);
        const {x,y,w,h}=this.drag?this.crop():this.rig;
        g.strokeStyle='#a8f8de';g.lineWidth=2;
        g.strokeRect(x*width,y*height,w*width,h*height);
        g.strokeStyle='#ffb66e';g.beginPath();
        g.arc(this.rig.mouthX*width,this.rig.mouthY*height,5,0,Math.PI*2);g.stroke();
        for(const field of this.rigFields)if(document.activeElement!==field)
            field.value=(this.rig[field.dataset.rig]*100).toFixed(3);
    }

    async build(){
        if(!this.image)throw Error('Choose a portrait image or a video frame first.');
        if(!this.name.value.trim())throw Error('Enter a character name.');
        await this.apply(await this.call('save',{name:this.name.value.trim(),image:this.image,rig:this.rig,wav:this.wav||''}));
        await this.loadLibrary();
        return this.output;
    }

    async loadLibrary(){
        const {avatars}=await this.call('library');
        this.saved.replaceChildren(new Option('Choose a saved avatar',''),
            ...avatars.map(row=>new Option(row.name,row.id)));
        this.saved.value=this.output?.id||'';
    }

    async apply(output){
        this.name.value=output.name;
        this.wav=output.wav;
        await this.setImage(output.image,output.rig);
        this.output=output;
        this.dirty=false;
        this.poseImages=output.poses.map(png=>{
            const image=new Image();
            image.src='data:image/png;base64,'+png;
            return image;
        });
        this.find('sprite').src='data:image/png;base64,'+output.sprite;
        this.find('walk').src='data:image/png;base64,'+output.walk;
        if(this.soundUrl)URL.revokeObjectURL(this.soundUrl);
        this.soundUrl=output.wav?wavUrl(output.wav):null;
        if(this.soundUrl)this.audio.src=this.soundUrl;
        else {this.audio.removeAttribute('src');this.audio.load();}
        this.saved.value=output.id;
        this.showPose();
        this.update();
        this.tell(output.wav?'Saved. Play the portrait, or export a talking video or game sprite.':'Saved. Export the character, or add audio to animate the portrait.');
    }

    changed(){
        this.dirty=true;
        this.audio.pause();
        this.update();
        this.onSource?.();
    }

    update(){
        const busy=Boolean(this.pending);
        const saved=Boolean(this.output&&!this.dirty);
        for(const id of ['image','clip','audio-file','saved','name','project'])this.find(id).disabled=busy;
        for(const field of this.rigFields)field.disabled=busy||!this.image;
        for(const id of ['crop','mouth','build'])this.find(id).disabled=busy||!this.image;
        this.find('clear-audio').disabled=busy||!this.wav;
        for(const id of ['frame','clip-audio','time'])this.find(id).disabled=busy;
        for(const id of ['portrait','assets','exports'])this.find(id).hidden=!saved;
        for(const button of this.root.querySelectorAll('[data-export],#avatar-tile'))
            button.disabled=busy||!saved||(button.dataset.export==='avi'&&!this.wav);
        this.audio.hidden=!saved||!this.wav;
        this.find('audio-note').textContent=this.wav?'Local audio ready · up to 30 seconds.':'Optional. Add a local recording to drive the mouth.';
        this.find('crop').setAttribute('aria-pressed',String(this.markMode==='crop'));
        this.find('mouth').setAttribute('aria-pressed',String(this.markMode==='mouth'));
    }

    showPose(){
        if(!this.output)return;
        const index=Math.min(this.output.mouth.length-1,Math.floor(this.audio.currentTime*this.output.videoFps));
        const next=this.poseImages[this.output.mouth[index]||0].src;
        if(this.find('portrait').getAttribute('src')!==next)this.find('portrait').src=next;
    }

    play(){
        cancelAnimationFrame(this.animation);
        const loop=()=>{
            this.showPose();
            if(!this.audio.paused)this.animation=requestAnimationFrame(loop);
        };
        loop();
    }
}
