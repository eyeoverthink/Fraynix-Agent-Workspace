'use strict';

const assert=require('node:assert/strict');
const {createHash}=require('node:crypto');
const fs=require('node:fs/promises');
const os=require('node:os');
const path=require('node:path');
const {launch}=require('./calibration-integration.cjs');

const formats=['avi','wav','png','json'];
const inputs=path.join(__dirname,'fixtures','timeline-edit');
const sha=bytes=>createHash('sha256').update(bytes).digest('hex');
const json=value=>JSON.stringify(value,null,2)+'\n';

async function test() {
    process.env.FRAYNIX_WORKSPACE_NO_MODELS='1';
    const out=path.resolve(process.argv[2]||'build/timeline255');
    const project=await fs.mkdtemp(path.join(os.tmpdir(),'Fraynix timeline '));
    const args=process.platform==='linux'?['--no-sandbox','--disable-dev-shm-usage']:[];
    await fs.mkdir(out,{recursive:true});
    let {app,page}=await launch(project,args);
    const api=(route,body={})=>app.evaluate((_,args)=>global.workspaceTest.api(args.route,args.body),{route,body});
    const call=(op,body)=>api('/api/timeline/'+op,body);
    const status=text=>page.waitForFunction(text=>document.querySelector('#timeline-status')?.textContent.includes(text),text,{timeout:90000});
    const settle=()=>page.waitForFunction(()=>!document.querySelector('#timeline-save')?.disabled);
    const capture=async name=>{
        await page.evaluate(()=>new Promise(resolve=>requestAnimationFrame(()=>requestAnimationFrame(resolve))));
        await fs.writeFile(path.join(out,name),Buffer.from(await app.evaluate(()=>global.workspaceTest.capture()),'base64'));
    };
    const lane=name=>page.locator('.timeline-track[data-lane="'+name+'"]');
    const field=(row,name)=>row.locator('input[data-field="'+name+'"]');

    async function enter() {
        await page.locator('#media-tab').click();
        await page.locator('#media-timeline-mode').click();
    }

    async function add(asset,track,fields) {
        await page.locator('#timeline-asset').selectOption(asset.id);
        await page.locator('#timeline-lane').selectOption(track);
        await page.locator('#timeline-add-strip').click();
        const strip=lane(track).locator('.timeline-strip').last();
        for (const [key,value] of Object.entries(fields)) await field(strip,key).fill(String(value));
    }

    async function collect(name,record,throughUi) {
        const dir=path.join(out,name);
        await fs.mkdir(dir,{recursive:true});
        for (const format of formats) {
            let bytes;
            if (throughUi) {
                await page.locator('[data-timeline-format="'+format+'"]').click();
                await status('Exported '+(format==='json'?'Timeline JSON':format.toUpperCase())+' to ');
                const statusText=await page.locator('#timeline-status').textContent();
                const target=statusText.slice(statusText.indexOf(' to ')+4);
                bytes=await fs.readFile(target);
                const exported=JSON.parse(await fs.readFile(target+'.receipt.json','utf8'));
                assert.equal(exported.source.id,record.id);
                assert.equal(exported.sha256,record.files[format].sha256);
            } else {
                const result=await call('export',{id:record.id,format});
                bytes=Buffer.from(result.base64,'base64');
            }
            assert.equal(sha(bytes),record.files[format].sha256);
            await fs.writeFile(path.join(dir,'edit.'+format),bytes);
        }
        const {posterPng,...withoutPreview}=record;
        await fs.writeFile(path.join(dir,'receipt.json'),json(withoutPreview));
        assert.deepEqual(JSON.parse(await fs.readFile(path.join(dir,'edit.json'),'utf8')),record.definition);
        return withoutPreview;
    }

    try {
        await page.waitForFunction(()=>document.querySelector('#engine-state')?.textContent.includes('0 local models'));
        const scene=await api('/api/scene/save',{title:'Timeline ramp',parameters:{angleDeg:22,friction:0}});
        const run=await api('/api/experiment/run',{id:scene.id,expectedVersion:scene.version,seconds:.75});
        assert.equal(run.status,'completed');
        const generated=await api('/api/motion-media/build',{runId:run.id});
        const generatedWav=await api('/api/motion-media/export',{id:generated.id,format:'wav'});
        const generatedAvi=await api('/api/motion-media/export',{id:generated.id,format:'avi'});
        await fs.writeFile(path.join(out,'generated.wav'),Buffer.from(generatedWav.base64,'base64'));
        await fs.writeFile(path.join(out,'generated.avi'),Buffer.from(generatedAvi.base64,'base64'));
        await page.locator('#media-tab').click();
        await page.locator('#media-motion-mode').click();
        await page.waitForFunction(()=>document.querySelector('#motion-status')?.textContent.includes('Reopened and verified'));
        await page.locator('#motion-edit-timeline').click();
        await status('Added the verified generated video, audio and poster.');
        for (const file of ['overlay.png','two-colors.avi','cue.wav']) {
            await page.locator('#timeline-import').setInputFiles(path.join(inputs,file));
            await status('Imported and checked '+file);
        }
        const {assets}=await call('assets');
        assert.equal(assets.length,6);
        const nativeClip=assets.find(a=>a.source.format==='avi'&&a.source.origin.type==='generated');
        const clip=assets.find(a=>a.source.title==='two-colors.avi');
        const still=assets.find(a=>a.source.title==='overlay.png');
        const audio=assets.find(a=>a.source.title==='cue.wav');
        assert.equal(nativeClip.soundFrames,19);
        assert.equal(clip.pictureFrames,16);
        assert.equal(clip.soundFrames,0);
        assert.equal(audio.soundFrames,24);

        const overlap=[{lane:'PICTURE',assetId:nativeClip.id,start:0,in:0,length:12},
            {lane:'PICTURE',assetId:clip.id,start:11,in:8,length:6}];
        await assert.rejects(call('render',{title:'Refused overlap',strips:overlap}),/one lane cannot overlap/);
        await assert.rejects(call('render',{title:'Missing source frames',strips:[
            {lane:'PICTURE',assetId:clip.id,start:0,in:13,length:6}]}),/extends past its source/);
        await assert.rejects(call('import',{filename:'not-a-clip.avi',base64:Buffer.from('wrong').toString('base64')}));
        assert.deepEqual(await call('latest'),{});

        await page.locator('#timeline-title').fill('Motion and color study');
        await add(nativeClip,'PICTURE',{start:0,in:0,length:12});
        await add(clip,'PICTURE',{start:12,in:8,length:6});
        await add(still,'PICTURE',{start:18,in:0,length:6});
        await add(still,'OVERLAY',{start:4,in:0,length:12,x:32,y:28,size:100,opacity:100});
        await add(nativeClip,'AUDIO1',{start:0,in:2,length:16,gain:.85});
        await add(audio,'AUDIO2',{start:6,in:3,length:13,gain:.85});
        await page.locator('#timeline-save').click();
        await status('Saved and verified a new timeline revision.');
        await settle();
        await page.locator('#timeline-editor').evaluate(e=>{e.scrollTop=0;});
        await capture('timeline-first-edit.png');

        const original=await call('latest');
        assert.equal(original.metrics.frames,24);
        assert.equal(original.metrics.audioSamples,24000);
        const played=await call('frame',{id:original.id,frame:12});
        assert.equal(played.frame,12);
        const previousPreview=await page.locator('#timeline-picture').getAttribute('src');
        await page.locator('#timeline-scrub').evaluate(element=>{
            element.value='13';
            element.dispatchEvent(new Event('change'));
        });
        await page.waitForFunction(()=>document.querySelector('#timeline-time')?.textContent.startsWith('13 ·'));
        assert.notEqual(await page.locator('#timeline-picture').getAttribute('src'),previousPreview);
        await page.locator('#timeline-picture').scrollIntoViewIfNeeded();
        await capture('timeline-frame-preview.png');
        const first=await collect('original',original,true);

        await app.close();
        ({app,page}=await launch(project,args));
        await enter();
        await status('Reopened and verified the saved timeline.');
        await settle();
        assert.equal(await field(lane('PICTURE').locator('.timeline-strip').nth(1),'in').inputValue(),'8');
        assert.deepEqual((await call('latest')).definition,first.definition);

        await page.locator('#timeline-title').fill('Motion and color study · revised');
        await field(lane('PICTURE').locator('.timeline-strip').nth(1),'in').fill('0');
        await field(lane('OVERLAY').locator('.timeline-strip'),'x').fill('128');
        await field(lane('AUDIO2').locator('.timeline-strip'),'start').fill('9');
        await status('Draft changed.');
        await page.locator('#timeline-save').click();
        await status('Saved and verified a new timeline revision.');
        const revised=await call('latest');
        assert.equal(revised.definition.parent,first.id);
        assert.notEqual(revised.files.avi.sha256,first.files.avi.sha256);
        assert.notEqual(revised.files.wav.sha256,first.files.wav.sha256);
        const next=await collect('revised',revised,false);
        await page.locator('#timeline-listen').click();
        await status('Playing the verified timeline mix.');
        const audioDuration=await page.locator('#timeline-audio').evaluate(audio=>audio.duration);
        assert(Math.abs(audioDuration-1)<.001);

        await app.close();
        ({app,page}=await launch(project,args));
        await enter();
        await status('Reopened and verified the saved timeline.');
        assert.deepEqual((await call('latest')).files,next.files);
        await page.locator('#timeline-previous').click();
        await status('Opened and verified the previous revision.');
        await settle();
        assert.equal(await field(lane('PICTURE').locator('.timeline-strip').nth(1),'in').inputValue(),'8');
        await page.locator('#timeline-editor').evaluate(e=>{e.scrollTop=0;});
        await capture('timeline-previous-reopened.png');
        for (const receipt of [first,next]) {
            assert.deepEqual((await call('open',{id:receipt.id})).files,receipt.files);
            for (const format of formats) {
                const exported=await call('export',{id:receipt.id,format});
                assert.equal(sha(Buffer.from(exported.base64,'base64')),receipt.files[format].sha256);
            }
        }

        const stored=path.join(project,'timeline','assets',clip.id+'.source');
        const exact=await fs.readFile(stored);
        const tampered=Buffer.from(exact);
        tampered[100]^=1;
        await fs.writeFile(stored,tampered);
        await assert.rejects(call('export',{id:first.id,format:'wav'}),/Timeline source two-colors.avi differs/);
        await assert.rejects(call('open',{id:next.id}),/Timeline source two-colors.avi differs/);
        await fs.writeFile(stored,exact);
        assert.deepEqual((await call('open',{id:next.id})).files,next.files);
        await fs.writeFile(path.join(out,'timeline-evidence.json'),json({
            platform:process.platform,models:0,offline:true,generated:{recipeId:generated.id,runId:run.id,source:generated.source},
            original:first,revised:next,exactBothAfterRestart:true,tamperedSourceRefused:true,
            sourceBoundsAndOverlapRefused:true,previewVerified:true
        }));
        console.log(json({first:first.metrics,revised:next.metrics}));
    } catch(error) {
        console.error('Test project',project);
        console.error('Timeline status',await page.locator('#timeline-status').textContent().catch(()=>null));
        await capture('failed.png').catch(()=>{});
        throw error;
    } finally {
        await app.close();
    }
}
test().catch(error=>{console.error(error);process.exitCode=1;});
