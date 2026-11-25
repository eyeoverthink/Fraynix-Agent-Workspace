'use strict';
const fs=require('node:fs');
const path=require('node:path');
const {execFileSync}=require('node:child_process');
const assert=require('node:assert/strict');
const {createHash}=require('node:crypto');

const root=path.resolve(process.argv[2]||'build/motion-media254');
const sha=data=>createHash('sha256').update(data).digest('hex');
const run=(name,args,options={})=>execFileSync(name,args,{maxBuffer:4_000_000,...options});
const probe=file=>JSON.parse(run('ffprobe',['-v','error','-count_frames','-show_streams','-of','json',file]));
const pcm=file=>run('ffmpeg',['-v','error','-i',file,'-map','0:a:0','-f','s16le','-acodec','pcm_s16le','pipe:1']);
const frame=(file,n)=>run('ffmpeg',['-v','error','-i',file,'-vf','select=eq(n\\,'+n+')','-frames:v','1',
    '-an','-pix_fmt','rgb24','-f','rawvideo','pipe:1']);
function markerX(rgb,width,height) {
    assert.equal(rgb.length,width*height*3);
    let mass=0,sum=0;
    for(let y=53;y<302;y++) for(let x=0;x<width;x++){
        const i=(y*width+x)*3,r=rgb[i],g=rgb[i+1],b=rgb[i+2];
        if(r>198 && g>130 && g<214 && b<165 && r-g>34 && g-b>35) {
            mass++;
            sum+=x;
        }
    }
    assert(mass>40,'The decoded video did not contain the motion marker.');
    return sum/mass;
}
const rows=[];
for(const name of ['shallow','steep']){
    const dir=path.join(root,name),avi=path.join(dir,'motion.avi'),wav=path.join(dir,'motion.wav'),png=path.join(dir,'motion.png');
    const receipt=JSON.parse(fs.readFileSync(path.join(dir,'receipt.json'),'utf8'));
    const videoStreams=probe(avi).streams;
    assert.equal(videoStreams.length,2);
    const [video,audio]=videoStreams;
    assert.equal(video.codec_name,'mjpeg');
    assert.equal(video.width,receipt.width);
    assert.equal(video.height,receipt.height);
    assert.equal(video.avg_frame_rate,'24/1');
    assert.equal(Number(video.nb_read_frames),receipt.frames);
    assert(Math.abs(Number(video.duration)-receipt.durationS)<1e-5);
    assert.equal(audio.codec_name,'pcm_s16le');
    assert.equal(audio.sample_rate,'24000');
    assert.equal(audio.channels,1);
    const videoAudio=pcm(avi),standalone=pcm(wav);
    assert(videoAudio.equals(standalone),'Independently decoded audio tracks differ.');
    assert.equal(standalone.length,receipt.frames*2000);
    let peak=0;
    for(let i=0;i<standalone.length;i+=2) peak=Math.max(peak,Math.abs(standalone.readInt16LE(i)));
    assert(Math.abs(peak/32767-.72)<.00005);
    const firstX=markerX(frame(avi,0),receipt.width,receipt.height);
    const lastX=markerX(frame(avi,receipt.frames-1),receipt.width,receipt.height);
    assert(lastX>firstX+4,'The decoded marker should move down the ramp.');
    const [still]=probe(png).streams;
    assert(still.codec_name==='png'&&still.width===receipt.width&&still.height===receipt.height);
    for(const ext of ['avi','wav','png','json'])
        assert.equal(sha(fs.readFileSync(path.join(dir,'motion.'+ext))),receipt.files[ext].sha256);
    rows.push({name,videoFrames:receipt.frames,fps:24,audioSamples:standalone.length/2,audioHz:24000,
        decodedPcmSha256:sha(standalone),pcmMatchesStandaloneWav:true,decodedMarkerTravelPx:lastX-firstX,
        independentlyDecodedPoster:[still.width,still.height],videoSha256:receipt.files.avi.sha256});
}
const version=run('ffprobe',['-version'],{encoding:'utf8'}).split('\n')[0].trim();
fs.writeFileSync(path.join(root,'independent-decode.json'),JSON.stringify({decoder:version,results:rows},null,2)+'\n');
console.log(JSON.stringify(rows));
