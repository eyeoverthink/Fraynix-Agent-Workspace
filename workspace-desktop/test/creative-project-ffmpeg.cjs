'use strict';

const assert=require('node:assert/strict');
const {execFileSync}=require('node:child_process');
const {createHash}=require('node:crypto');
const fs=require('node:fs');
const path=require('node:path');

const root=path.resolve(process.argv[2]||'build/creative256');
const at=name=>path.join(root,name);
const digest=data=>createHash('sha256').update(data).digest('hex');
const readMedia=(tool,args)=>execFileSync(tool,args,{maxBuffer:24_000_000});
const sound=file=>readMedia('ffmpeg',['-v','error','-i',file,'-map','0:a:0','-f','s16le','pipe:1']);
const pixels=640*360*3;
const rgbAt=(rgb,frame,x=320,y=100)=>[...rgb.subarray(frame*pixels+(y*640+x)*3,frame*pixels+(y*640+x)*3+3)];
const decode=file=>readMedia('ffmpeg',['-v','error','-i',file,'-map','0:v:0','-an','-pix_fmt','rgb24','-f','rawvideo','pipe:1']);
const privateRgb=rgbAt(decode(at('private-control.avi')),0,320,180);
assert(privateRgb[0]>170&&privateRgb[1]<100&&privateRgb[2]>140);
const crimson=([r,g,b])=>r>170&&g<110&&b<110;
const freshSound=sound(at('fresh-source.wav'));
const privateSound=sound(at('private-control.wav'));
const evidence=JSON.parse(fs.readFileSync(at('creative-evidence.json'),'utf8'));

const decodes=[];
for (const [name,boundary,frames] of [['without-memory.avi',10,20],['with-memory.avi',14,24],['family-cut.avi',14,24]]) {
    const file=at(name);
    const probe=JSON.parse(readMedia('ffprobe',['-v','error','-count_frames','-show_streams','-show_format','-of','json',file]));
    assert.equal(probe.streams.length,2);
    const [video,audio]=probe.streams;
    assert.equal(video.codec_name,'mjpeg');
    assert.equal(video.avg_frame_rate,'24/1');
    assert.equal(video.nb_read_frames,String(frames));
    assert.equal(audio.codec_name,'pcm_s16le');
    assert.equal(audio.sample_rate,'24000');
    assert.deepEqual(probe.format.tags||{},{},'The shareable media must not carry a source title or other container tags.');
    for (const stream of probe.streams) assert.deepEqual(stream.tags||{},{});
    const rgb=decode(file);
    assert.equal(rgb.length,frames*pixels);
    const before=rgbAt(rgb,boundary-1), atCut=rgbAt(rgb,boundary);
    assert(!crimson(before)&&crimson(atCut));
    for (let frame=0;frame<frames;frame++) {
        const pixel=rgbAt(rgb,frame,320,180);
        const distance=privateRgb.reduce((sum,channel,i)=>sum+Math.abs(channel-pixel[i]),0);
        assert(distance>80,'A private picture control appears in the audience copy.');
    }
    const pcm=sound(file);
    const expected=Buffer.alloc(frames*2000);
    for (let offset=0;offset<Math.min(expected.length,freshSound.length);offset+=2)
        expected.writeInt16LE(Math.round(freshSound.readInt16LE(offset)*0.8),offset);
    assert(pcm.equals(expected),'Every decoded PCM sample must match the selected fresh source at the approved gain.');
    assert(!pcm.subarray(0,Math.min(pcm.length,privateSound.length)).equals(privateSound.subarray(0,Math.min(pcm.length,privateSound.length))));
    const avi=fs.readFileSync(file);
    assert(!avi.includes(Buffer.from('PRIVATE-256-original-')));
    const expectedSha=name==='without-memory.avi'?evidence.compared.controlSha:evidence.compared.adaptedSha;
    assert.equal(digest(avi),expectedSha);
    decodes.push({name,cutAtFrame:boundary,before,atCut,frames,samples:pcm.length/2,exactPcm:true,privatePictureControlAbsent:true,containerTags:0,sha256:expectedSha});
}
assert(fs.readFileSync(at('family-cut.avi')).equals(fs.readFileSync(at('with-memory.avi'))));
fs.writeFileSync(at('independent-creative.json'),JSON.stringify({reader:'FFmpeg and ffprobe',decodes},null,2)+'\n');
console.log('Independently decoded frame 10 vs 14, verified all audio samples and confirmed an unchanged tag-free family AVI.');
