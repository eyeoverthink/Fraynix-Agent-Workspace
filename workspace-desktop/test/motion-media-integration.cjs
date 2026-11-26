'use strict';

const assert = require('node:assert/strict');
const {createHash} = require('node:crypto');
const fs = require('node:fs/promises');
const os = require('node:os');
const path = require('node:path');
const {launch} = require('./calibration-integration.cjs');

const sha = bytes=>createHash('sha256').update(bytes).digest('hex');
const formats=['avi','wav','png','json'];
const brief='Turn this run into a video with sound and a poster.';
const json=value=>JSON.stringify(value,null,2)+'\n';

async function test() {
    process.env.FRAYNIX_WORKSPACE_NO_MODELS='1';
    process.env.FRAYNIX_UNIFIED_TEST='1';
    const output=path.resolve(process.argv[2]||'build/motion-media254');
    const project=await fs.mkdtemp(path.join(os.tmpdir(),'Fraynix motion media '));
    const args=process.platform==='linux'?['--no-sandbox','--disable-dev-shm-usage']:[];
    await fs.mkdir(output,{recursive:true});
    let {app,page}=await launch(project,args);
    const api=(route,body={})=>app.evaluate((_,args)=>global.workspaceTest.api(args.route,args.body),{route,body});
    const media=(kind,body)=>api('/api/motion-media/'+kind,body);
    const status=phrase=>page.waitForFunction(phrase=>document.querySelector('#motion-status')?.textContent.includes(phrase),phrase,{timeout:90000});
    const capture=async file=>{
        await page.evaluate(()=>new Promise(resolve=>requestAnimationFrame(()=>requestAnimationFrame(resolve))));
        await fs.writeFile(path.join(output,file),Buffer.from(await app.evaluate(()=>global.workspaceTest.capture()),'base64'));
    };

    async function nativeRun(title,angleDeg,seconds) {
        const scene=await api('/api/scene/save',{title,parameters:{angleDeg,friction:0}});
        const run=await api('/api/experiment/run',{id:scene.id,expectedVersion:scene.version,seconds});
        await api('/api/experiment/control',{action:'reset'});
        assert.equal(run.status,'completed');
        assert(run.samples.length>12);
        return run;
    }

    async function enter() {
        await page.locator('#goal').fill('Open motion media.');
        await page.locator('#run').click();
        await page.locator('#motion-media').waitFor();
    }

    function verifyTimeline(timeline,run,receipt) {
        assert.equal(timeline.source.runId,run.id);
        assert.equal(timeline.source.runSha256,receipt.source.runSha256);
        assert.equal(timeline.timeline.fps,24);
        assert.equal(timeline.timeline.sampleRateHz,24000);
        const points=timeline.timeline.frames;
        assert.equal(points.length,Math.ceil(run.final.tick/10)+1);
        assert.equal(points.at(-1).sourceTick,run.final.tick);
        const source=[...run.samples.filter(x=>x.tick<run.final.tick),run.final];
        for (let i=0;i<points.length;i++) {
            const frame=points[i];
            const tick=Math.min(10*i,run.final.tick);
            assert.equal(frame.sourceTick,tick);
            const first=source.findLast(s=>s.tick<=tick);
            const last=source.find(s=>s.tick>=tick);
            const blend=last.tick===first.tick?0:(tick-first.tick)/(last.tick-first.tick);
            for (const [field,axis] of [['xM',0],['yM',1]]) {
                assert(Math.abs(frame[field]-(first.position[axis]+(last.position[axis]-first.position[axis])*blend))<1e-10);
            }
        }
        assert(Math.abs(points.at(-1).xM-points[0].xM)>.1);
        assert.equal(timeline.sound.samples,points.length*1000);
        assert.equal(timeline.sound.exportClippedSamples,0);
        assert(Math.abs(timeline.sound.exportPeak-.72)<.00005);
        assert(timeline.encoder.tested.at(-1).passed);
        assert(timeline.encoder.tested.slice(0,-1).every(candidate=>!candidate.passed));
    }

    async function collect(name,run,clickExports) {
        const recipe=await media('latest');
        assert.equal(recipe.source.runId,run.id);
        const directory=path.join(output,name);
        await fs.mkdir(directory,{recursive:true});
        for (const extension of formats) {
            let bytes;
            if (clickExports) {
                await page.locator('[data-motion-format="'+extension+'"]').click();
                await status('Exported '+extension.toUpperCase()+' to ');
                const message=await page.locator('#motion-status').textContent();
                const saved=message.slice(message.indexOf(' to ')+4);
                bytes=await fs.readFile(saved);
                const exported=JSON.parse(await fs.readFile(saved+'.receipt.json','utf8'));
                assert.equal(exported.source.runId,run.id);
                assert.equal(exported.sha256,sha(bytes));
            } else {
                const exported=await media('export',{id:recipe.id,format:extension});
                bytes=Buffer.from(exported.base64,'base64');
                assert.equal(exported.sha256,sha(bytes));
            }
            assert.equal(recipe.files[extension].sha256,sha(bytes));
            await fs.writeFile(path.join(directory,'motion.'+extension),bytes);
        }
        const timeline=JSON.parse(await fs.readFile(path.join(directory,'motion.json'),'utf8'));
        verifyTimeline(timeline,run,recipe);
        await fs.writeFile(path.join(directory,'native-source.json'),json(run));
        const {posterPng,...retained}=recipe;
        await fs.writeFile(path.join(directory,'receipt.json'),json(retained));
        return retained;
    }

    try {
        await page.waitForFunction(()=>document.querySelector('#engine-state')?.textContent.includes('0 local models'));
        await page.locator('#goal').fill(brief);
        await page.locator('#run').click();
        await page.getByText('This request needs a completed motion recording.',{exact:true}).waitFor();
        assert.deepEqual(await media('latest'),{});

        const gentle=await nativeRun('Shallow ramp',9,.72);
        const steep=await nativeRun('Steep ramp',38,1.38);
        await page.locator('#motion-refresh').click();
        await page.locator('#motion-source').selectOption(gentle.id);
        await page.locator('#workspace-next-continue:not([disabled])').waitFor();
        await page.locator('#workspace-next-continue').click();
        assert.equal(await page.locator('#messages .user').filter({hasText:brief}).count(),1);
        await status('Motion media verified and saved.');
        const first=await collect('shallow',gentle,false);

        await app.close();
        ({app,page}=await launch(project,args));
        await enter();
        await status('Reopened and verified the four saved files.');
        await page.locator('#motion-source').selectOption(steep.id);
        await page.locator('#goal').fill(brief);
        await page.locator('#run').click();
        await page.getByText('The verified video, audio, poster and timeline are open for review and export.',{exact:true}).waitFor();
        await status('Motion media verified and saved.');
        await page.waitForFunction(()=>document.querySelector('#motion-poster')?.naturalWidth===640);
        await capture('media-built-offline.png');
        const second=await collect('steep',steep,true);
        assert.notEqual(first.files.avi.sha256,second.files.avi.sha256);
        assert.notEqual(first.sound.gain,second.sound.gain);
        assert(second.sound.rawClippedSamples>0);
        assert(second.frames>first.frames);
        assert.deepEqual(second.encoder.rememberedStart,{recipeId:first.id,quality:first.encoder.quality});
        assert.equal(second.encoder.tested[0].quality,first.encoder.quality);
        assert(second.encoder.tested.at(-1).passed);
        if (process.platform==='win32') assert(second.encoder.tested.length<first.encoder.tested.length);

        await app.close();
        ({app,page}=await launch(project,args));
        await enter();
        await status('Reopened and verified the four saved files.');
        await page.waitForFunction(()=>document.querySelector('#motion-poster')?.naturalWidth===640);
        assert.equal(await page.locator('#motion-source').inputValue(),steep.id);
        await capture('media-after-restart.png');
        for (const receipt of [first,second]) {
            const {posterPng,...opened}=await media('open',{id:receipt.id});
            assert.deepEqual(opened,receipt);
            const repeated=await media('build',{runId:receipt.source.runId});
            assert.deepEqual(repeated.files,receipt.files);
            for (const extension of formats) {
                const data=await media('export',{id:receipt.id,format:extension});
                assert.equal(sha(Buffer.from(data.base64,'base64')),receipt.files[extension].sha256);
            }
        }
        await page.locator('#motion-listen').click();
        await status('Playing the verified audio track.');
        await page.waitForFunction(()=>document.querySelector('#motion-audio')?.duration>1);
        assert(Math.abs(await page.locator('#motion-audio').evaluate(audio=>audio.duration)-second.durationS)<.001);

        const file=path.join(project,'motion-media',second.id+'.wav');
        const exact=await fs.readFile(file);
        const changed=Buffer.from(exact);
        changed[changed.length-31]^=1;
        await fs.writeFile(file,changed);
        await assert.rejects(media('export',{id:second.id,format:'avi'}),/saved WAV differs from its verified bytes/);
        await assert.rejects(media('open',{id:second.id}),/saved WAV differs from its verified bytes/);
        await fs.writeFile(file,exact);
        assert.deepEqual((await media('open',{id:second.id})).files,second.files);
        await assert.rejects(media('export',{id:second.id,format:'mp4'}),/Choose PNG, WAV, AVI, or timeline JSON/);

        const evidence={platform:process.platform,offline:true,models:0,chat:brief,
            recipes:[first,second],rememberedSettingQualifiedOnFreshRun:true,
            exactAfterRestart:true,tamperingRefused:true,unsupportedFormatRefused:true};
        await fs.writeFile(path.join(output,'media-evidence.json'),json(evidence));
        console.log(json({recipes:evidence.recipes.map(r=>({frames:r.frames,encoder:r.encoder,sound:r.sound}))}));
    } catch (error) {
        console.error('Test project',project);
        console.error('Media status',await page.locator('#motion-status').textContent().catch(()=>null));
        await capture('failed.png').catch(()=>{});
        throw error;
    } finally {
        await app.close();
    }
}
test().catch(error=>{console.error(error);process.exitCode=1;});
