'use strict';

const assert=require('node:assert/strict');
const {createHash}=require('node:crypto');
const fs=require('node:fs/promises');
const os=require('node:os');
const path=require('node:path');
const {launch}=require('./calibration-integration.cjs');
const {environmentGraphics}=require('./environment-integration.cjs');
const sha=bytes=>createHash('sha256').update(bytes).digest('hex');

function tone() {
    const rate=24000,count=rate*2;
    const b=Buffer.alloc(44+count*2);
    b.write('RIFF'); b.writeUInt32LE(b.length-8,4); b.write('WAVEfmt ',8);
    b.writeUInt32LE(16,16); b.writeUInt16LE(1,20); b.writeUInt16LE(1,22);
    b.writeUInt32LE(rate,24); b.writeUInt32LE(rate*2,28); b.writeUInt16LE(2,32); b.writeUInt16LE(16,34);
    b.write('data',36); b.writeUInt32LE(count*2,40);
    for(let i=0;i<count;i++){
        const time=i/rate, amplitude=time<.4||time>1.6?0:time<1?.07:.37;
        b.writeInt16LE(Math.round(32767*amplitude*Math.sin(i/rate*2*Math.PI*220)),44+i*2);
    }
    return b;
}

async function drawFixture(page) {
    return Buffer.from(await page.evaluate(()=>{
        const canvas=document.createElement('canvas');
        canvas.width=640;canvas.height=360;
        const g=canvas.getContext('2d');
        const ellipse=(color,x,y,a,b)=>{
            g.fillStyle=color;g.beginPath();g.ellipse(x,y,a,b,0,0,Math.PI*2);g.fill();
        };
        g.fillStyle='#1d3748';g.fillRect(0,0,640,360);
        for (const [x,y] of [[95,70],[534,260],[395,47],[85,310],[562,97]])ellipse('#538590',x,y,3,3);
        ellipse('#0e223c',320,338,220,78);
        ellipse('#122527',320,171,103,127);
        ellipse('#ffb865',320,176,77,109);
        ellipse('#f7d285',320,174,63,94);
        ellipse('#173c50',289,153,15,12); ellipse('#173c50',351,153,15,12);
        ellipse('#e8fbeb',286,150,4,4); ellipse('#e8fbeb',348,150,4,4);
        ellipse('#135568',320,235,35,15);
        ellipse('#d58b7d',320,238,22,5);
        g.fillStyle='#aec9c6'; g.font='22px sans-serif'; g.fillText('Studio robot',27,42);
        return canvas.toDataURL().split(',')[1];
    }),'base64');
}

async function main(){
    process.env.FRAYNIX_UNIFIED_TEST='1';
    process.env.FRAYNIX_WORKSPACE_NO_MODELS='1';
    const clip=process.env.FRAYNIX_AVATAR_SAMPLE;
    const output=path.resolve(process.argv[2]||'build/avatar-test');
    await fs.mkdir(output,{recursive:true});
    const project=clip?path.join(output,'project'):await fs.mkdtemp(path.join(os.tmpdir(),'Fraynix avatar '));
    await fs.mkdir(project,{recursive:true});
    let app,page,recipient;
    const errors=[];
    async function open(location=project){
        ({app,page}=await launch(location,environmentGraphics()));
        page.on('pageerror',error=>errors.push(error.message));
        await page.locator('#workspace-home h1').waitFor();
    }
    const api=(route,args={})=>app.evaluate((_,input)=>global.workspaceTest.api(input.route,input.args),{route,args});
    const ask=async text=>{await page.locator('#goal').fill(text);await page.locator('#run').click();};
    const capture=async name=>{
        await page.evaluate(()=>new Promise(resolve=>requestAnimationFrame(()=>requestAnimationFrame(resolve))));
        await fs.writeFile(path.join(output,name),Buffer.from(await app.evaluate(()=>global.workspaceTest.capture()),'base64'));
    };
    const exports=[['sprite','character.png'],['walk','walk-sheet.png'],['portrait','portrait.png'],['manifest','asset-manifest.json'],['project','editable-avatar.avatar.json'],['avi','talking-portrait.avi']];
    async function exportFile(kind,destination){
        const before=await page.locator('#avatar-status').textContent();
        await page.locator('#avatar-exports [data-export='+kind+']').click();
        const status=page.locator('#avatar-status').filter({hasText:/Saved .*[/\\]exports[/\\]/});
        await page.waitForFunction(previous=>{
            const text=document.querySelector('#avatar-status').textContent;
            return text!==previous&&/^Saved .+[/\\]exports[/\\]/.test(text);
        },before);
        await status.waitFor();
        const file=(await status.textContent()).slice(6);
        const bytes=await fs.readFile(file);
        const receipt=JSON.parse(await fs.readFile(file+'.receipt.json','utf8'));
        assert.equal(sha(bytes),receipt.sha256);
        assert.equal(receipt.kind,'avatar-'+kind);
        if(destination)await fs.writeFile(path.join(output,destination),bytes);
        return receipt.sha256;
    }

    try{
        await open();
        let command='Make an avatar from an image.';
        if(clip){
            await ask('Open Avatar Studio.');
            await page.locator('#avatar-studio:visible #avatar-clip:enabled').setInputFiles(clip);
            await page.locator('#avatar-video-controls').waitFor();
            await page.locator('#avatar-time').fill('3');
            await page.locator('#avatar-frame').click();
            await page.locator('#avatar-status').filter({hasText:'Frame at 3.000 s loaded'}).waitFor();
            await page.locator('#avatar-clip-audio').click();
            await page.locator('#avatar-status').filter({hasText:'Audio ready'}).waitFor();
        }else{
            const png=path.join(output,'sample-portrait.png');
            const wav=path.join(output,'sample-voice-timing.wav');
            await fs.writeFile(png,await drawFixture(page));
            await fs.writeFile(wav,tone());
            await ask(command);
            await page.locator('#avatar-studio:visible #avatar-image:enabled').setInputFiles(png);
            await page.locator('#workspace-next-continue:not([disabled])').waitFor();
            await page.locator('#avatar-audio-file').setInputFiles(wav);
            await page.locator('#avatar-status').filter({hasText:'Audio ready'}).waitFor();
        }

        const marks=clip?{x:33,y:4.7,w:32.7,h:80,mouthX:50.5,mouthY:64.7}
            :{x:34,y:12,w:32,h:71,mouthX:50,mouthY:65};
        await page.locator('#avatar-source').waitFor();
        await page.locator('#avatar-name').fill(clip?'Vaughn · Studio Character':'Studio Robot');
        await page.locator('#avatar-studio details > summary').click();
        for(const [field,value] of Object.entries(marks)){
            const input=page.locator('#avatar-studio input[data-rig='+field+']');
            await input.fill(String(value));
            await input.press('Tab');
        }
        await page.locator('#avatar-studio details > summary').click();
        if(clip)await page.locator('#avatar-build').click();
        else await page.locator('#workspace-next-continue').click();
        await page.locator('#avatar-assets').waitFor();
        if(!clip)assert.equal(await page.locator('#messages .user').filter({hasText:command}).count(),1);
        const first=await api('/api/avatar/open');
        assert.equal(first.mouth.length,clip?720:48);
        assert(first.mouth.includes(0)&&first.mouth.includes(4));
        assert.equal(new Set(first.hashes.poseSha256).size,5);
        if(!clip){
            assert(first.mouth.slice(0,8).every(pose=>pose===0));
            assert(first.mouth.slice(29,38).every(pose=>pose===4));
        }
        const pixels=await page.locator('#avatar-sprite').evaluate(async image=>{
            await image.decode();
            const canvas=document.createElement('canvas');
            canvas.width=image.naturalWidth;canvas.height=image.naturalHeight;
            const context=canvas.getContext('2d');
            context.drawImage(image,0,0);
            return [canvas.width,canvas.height,context.getImageData(0,0,1,1).data[3],context.getImageData(48,20,1,1).data[3]];
        });
        assert.deepEqual(pixels,[96,112,0,255]);
        const quiet=await page.locator('#avatar-portrait').getAttribute('src');
        const active=first.mouth.indexOf(4);
        await page.locator('#avatar-play').evaluate((audio,time)=>{audio.currentTime=time;},(active+.25)/24);
        await page.waitForFunction(previous=>document.querySelector('#avatar-portrait').src!==previous,quiet);
        if(clip)await page.locator('#avatar-video-controls').evaluate(node=>node.hidden=true);
        await capture('avatar-studio.png');

        const digests={};
        for(const [kind,name] of exports)digests[kind]=await exportFile(kind,name);
        assert.equal(digests.sprite,first.hashes.spriteSha256);
        assert.equal(digests.walk,first.hashes.walkSha256);
        await page.locator('#avatar-tile').click();
        await page.locator('#tile-status').filter({hasText:'to the reusable sprite library'}).waitFor();
        const {assets}=await api('/api/media-tiles/library');
        const tile=assets.find(a=>a.label.includes(first.id.slice(0,12)));
        assert(tile);
        const checkedTile=await api('/api/media-tiles/sprite',{id:tile.id});
        assert.equal(checkedTile.isolation,'alpha');
        assert.equal(checkedTile.imageSha256,first.hashes.spriteSha256);
        await app.close();app=null;

        await open();
        await ask('Open Avatar Studio.');
        await page.locator('#avatar-assets').waitFor();
        assert.equal(await page.locator('#avatar-name').inputValue(),first.name);
        const restarted=await api('/api/avatar/open');
        assert.equal(restarted.id,first.id);
        assert.deepEqual(restarted.hashes,first.hashes);
        assert.equal(await exportFile('avi'),digests.avi);

        if(!clip){
            const record=path.join(project,'media','avatars',first.id+'.json');
            await fs.appendFile(record,' ');
            await assert.rejects(api('/api/avatar/open',{id:first.id}),/bytes changed/);
            await assert.rejects(api('/api/avatar/export',{id:first.id,kind:'sprite'}),/bytes changed/);
            await app.close();app=null;
            recipient=await fs.mkdtemp(path.join(os.tmpdir(),'Fraynix avatar '));
            await open(recipient);
            await ask('Open Avatar Studio.');
            const portable=path.join(output,'editable-avatar.avatar.json');
            const altered=JSON.parse(await fs.readFile(portable,'utf8'));
            altered.rig.mouthX+=.02;
            await assert.rejects(api('/api/avatar/import',{base64:Buffer.from(JSON.stringify(altered)).toString('base64')}),/differs from the saved outputs/);
            assert.deepEqual(await api('/api/avatar/open'),{});
            await page.locator('#avatar-studio:visible #avatar-project:enabled').setInputFiles(portable);
            await page.locator('#avatar-assets').waitFor();
            assert.equal((await api('/api/avatar/open')).id,first.id);
            assert.equal(await exportFile('avi'),digests.avi);
        }
        assert.deepEqual(errors,[]);
        await fs.writeFile(path.join(output,'avatar-test-evidence.json'),JSON.stringify({
            offline:true,source:clip?'User-provided video':'Original drawn robot with a timed audio fixture',
            durationS:first.mouth.length/24,videoFps:24,distinctMouthPoses:new Set(first.mouth).size,
            spriteSize:[96,112],transparentSprite:true,walkFrames:first.walkFrames,
            sameExportAfterRestart:true,changedRecipeRefused:!clip,cleanProjectImport:!clip,tileLibraryAsset:true,digests
        },null,2)+'\n');
        console.log('Avatar verified offline, including mouth motion, transparent sprite, tile library and byte-identical video after restart.');
    }catch(error){
        console.error('Avatar status',await page?.locator('#avatar-status').textContent().catch(()=>null),errors);
        throw error;
    }finally{
        if(app)await app.close().catch(()=>{});
        for(const location of [clip?null:project,recipient].filter(Boolean)){
            const resolved=await fs.realpath(location);
            if(path.dirname(resolved)===await fs.realpath(os.tmpdir())&&/^Fraynix avatar [a-z0-9]+$/i.test(path.basename(resolved)))
                await fs.rm(resolved,{recursive:true,force:true,maxRetries:3}).catch(()=>{});
        }
    }
}
main().catch(error=>{console.error(error);process.exitCode=1;});
