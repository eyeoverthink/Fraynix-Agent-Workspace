'use strict';

const assert=require('node:assert/strict');
const fs=require('node:fs');
const path=require('node:path');
const {execFileSync}=require('node:child_process');
const {createHash}=require('node:crypto');

const root=path.resolve(process.argv[2]||'build/timeline255');
const imported=path.join(__dirname,'fixtures','timeline-edit');
const run=(tool,args,options={})=>execFileSync(tool,args,{maxBuffer:20_000_000,...options});
const wav=file=>run('ffmpeg',['-v','error','-i',file,'-map','0:a:0','-f','s16le','-acodec','pcm_s16le','pipe:1']);
const image=file=>run('ffmpeg',['-v','error','-i',file,'-map','0:v:0','-an','-pix_fmt','rgb24','-f','rawvideo','pipe:1']);
const digest=data=>createHash('sha256').update(data).digest('hex');
const pixels=640*360*3;
const color=(rgb,frame,x,y)=>Array.from(rgb.subarray(frame*pixels+(y*640+x)*3,frame*pixels+(y*640+x)*3+3));
const magenta=([r,g,b])=>r>170 && g<100 && b>140;
const blue=([r,g,b])=>r<65 && g>90 && g<155 && b>165;
const crimson=([r,g,b])=>r>170 && g<110 && b<110;

function reconstructPcm(definition,frameCount,generated,cue) {
    const buffer=new Float64Array(frameCount*1000);
    for (const strip of definition.strips.filter(x=>x.lane.startsWith('AUDIO'))) {
        const origin=definition.assets[strip.assetId].source.origin.type;
        const input=origin==='generated'?generated:cue;
        for (let i=0;i<strip.length*1000;i++) {
            const source=strip.in*1000+i;
            if (source<input.length/2) buffer[strip.start*1000+i]+=input.readInt16LE(source*2)*strip.gain;
        }
    }
    let peak=0, over=0;
    for (const sample of buffer) {
        peak=Math.max(peak,Math.abs(sample));
        if (Math.abs(sample)>32767) over++;
    }
    const gain=peak>32767?32000/peak:1;
    const mix=Buffer.alloc(buffer.length*2);
    for (let i=0;i<buffer.length;i++) mix.writeInt16LE(Math.round(buffer[i]*gain),i*2);
    return {mix,gain,over};
}

const cue=wav(path.join(imported,'cue.wav'));
const gen=wav(path.join(root,'generated.avi'));
assert(gen.equals(wav(path.join(root,'generated.wav'))));
const sources=image(path.join(imported,'two-colors.avi'));
assert.equal(sources.length,16*pixels);
assert(crimson(color(sources,7,320,100)) && blue(color(sources,8,320,100)));

const results=[];
for (const version of ['original','revised']) {
    const dir=path.join(root,version);
    const file=path.join(dir,'edit.avi');
    const receipt=JSON.parse(fs.readFileSync(path.join(dir,'receipt.json'),'utf8'));
    const def=JSON.parse(fs.readFileSync(path.join(dir,'edit.json'),'utf8'));
    const streams=JSON.parse(run('ffprobe',['-v','error','-count_frames','-show_streams','-of','json',file])).streams;
    assert.equal(streams.length,2);
    const [video,audio]=streams;
    assert.equal(video.codec_name,'mjpeg');
    assert.equal(video.avg_frame_rate,'24/1');
    assert.equal(video.nb_read_frames,'24');
    assert.equal(video.width,640);
    assert.equal(video.height,360);
    assert.equal(audio.sample_rate,'24000');
    assert.equal(audio.codec_name,'pcm_s16le');
    const decoded=wav(file);
    const expected=reconstructPcm(def,24,gen,cue);
    assert(decoded.equals(wav(path.join(dir,'edit.wav'))));
    assert(decoded.equals(expected.mix),'The exported PCM differs from independently rebuilt source edits.');
    assert.equal(decoded.length,48000);
    assert.equal(expected.gain,receipt.metrics.mixGain);
    assert.equal(expected.over,receipt.metrics.unscaledClippedSamples);
    const frames=image(file);
    assert.equal(frames.length,24*pixels);
    const check=version==='original'?blue:crimson;
    const before=color(frames,11,320,100), cut=color(frames,12,320,100);
    assert(!check(before) && check(cut) && check(color(frames,17,320,100)));
    assert(magenta(color(frames,18,320,180)));
    const overlayX=version==='original'?55:151;
    assert(!magenta(color(frames,3,overlayX,52)));
    assert(magenta(color(frames,4,overlayX,52)) && magenta(color(frames,15,overlayX,52)));
    assert(!magenta(color(frames,16,overlayX,52)));
    const alternate=version==='original'?151:55;
    assert(!magenta(color(frames,10,alternate,52)));
    const editedAudio=def.strips.find(strip=>strip.lane==='AUDIO2');
    assert.equal(editedAudio.start,version==='original'?6:9);
    for (const extension of ['avi','wav','png','json'])
        assert.equal(digest(fs.readFileSync(path.join(dir,'edit.'+extension))),receipt.files[extension].sha256);
    results.push({version,videoFrames:24,audioSamples:decoded.length/2,pcmMatchesIndependentMix:true,
        clipStartsAtFrame:12,cutPixel:cut,overlayOnFrame:4,overlayOffFrame:16,overlayPixelX:overlayX,
        stillStartsAtFrame:18,secondAudioStartsAtSample:editedAudio.start*1000,outputSha256:receipt.files.avi.sha256});
}
const decoder=run('ffprobe',['-version'],{encoding:'utf8'}).split('\n')[0].trim();
fs.writeFileSync(path.join(root,'independent-timeline.json'),JSON.stringify({decoder,results},null,2)+'\n');
console.log(JSON.stringify(results));
