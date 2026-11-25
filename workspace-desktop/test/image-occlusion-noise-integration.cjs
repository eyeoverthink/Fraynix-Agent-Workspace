'use strict';

const assert = require('node:assert/strict');
const {createHash} = require('node:crypto');
const fs = require('node:fs/promises');
const os = require('node:os');
const path = require('node:path');
const {launch} = require('./calibration-integration.cjs');
const {fitRegisteredViews, checkRotatedRoundTrip} = require('./image-assembly-integration.cjs');

const inputs = path.join(__dirname,'fixtures','image-occlusion-noise');
const refs = {front:'front.png',side:'side.png',top:'top.png',heldOut:'elevated.png'};
const labels = {step:'Offset step',fork:'Fork bracket',elbow:'Elbow bracket'};
const selection = ['fork','elbow','step'];
const sha = bytes=>createHash('sha256').update(bytes).digest('hex');
const asJson = value=>JSON.stringify(value,null,2)+'\n';

function rotationError(actual, expected) {
    const dot = actual.reduce((sum,q,i)=>sum+q*expected[i],0);
    return 2*Math.acos(Math.min(1,Math.abs(dot)/(Math.hypot(...actual)*Math.hypot(...expected))))*180/Math.PI;
}

async function test() {
    process.env.FRAYNIX_WORKSPACE_NO_MODELS = '1';
    const out = path.resolve(process.argv[2] || 'build/occlusion-noise253');
    const project = await fs.mkdtemp(path.join(os.tmpdir(),'Fraynix noisy occlusion '));
    const truth = JSON.parse(await fs.readFile(path.join(inputs,'truth.json'),'utf8'));
    const args = process.platform==='linux' ? ['--no-sandbox','--disable-dev-shm-usage'] : [];
    await fs.mkdir(out,{recursive:true});
    let {app,page} = await launch(project,args);
    const call = (route,input={})=>app.evaluate((_,arg)=>global.workspaceTest.api('/api/spatial/'+arg.route,arg.input),{route,input});
    const field = name=>page.locator('#image-assembly [name='+name+']');
    const status = phrase=>page.waitForFunction(value=>document.querySelector('#image-assembly-status')?.textContent.includes(value),
        phrase,{timeout:90000});
    const capture = async file=>{
        await page.evaluate(()=>new Promise(resolve=>requestAnimationFrame(()=>requestAnimationFrame(resolve))));
        await fs.writeFile(path.join(out,file),Buffer.from(await app.evaluate(()=>global.workspaceTest.capture()),'base64'));
    };
    const read = async (row,file)=>(await fs.readFile(path.join(inputs,row.id,file))).toString('base64');
    const upload = (row,name,file)=>field(name).setInputFiles(path.join(inputs,row.id,file));
    const open = async()=>{
        await page.locator('#spatial-tab').click();
        const details = page.locator('#image-assembly');
        if (!await details.evaluate(el=>el.open)) await details.locator('summary').click();
    };

    async function refusal(input, reason) {
        let message;
        await assert.rejects(call('image-assembly',input),error=>{
            message = error.message.split('\n',1)[0].replace(/^.*?Error: /,'');
            return reason.test(message);
        });
        return message;
    }

    try {
        await page.locator('#spatial-tab').click();
        await page.locator('#spatial-browser-views summary').click();
        const models = {
            elbow:await fitRegisteredViews(page,call,'image-yaw/elbow',refs,60),
            step:await fitRegisteredViews(page,call,'image-yaw/step',refs,60),
            fork:await fitRegisteredViews(page,call,'image-pose-survey/fork',refs,60)
        };
        await page.locator('#spatial-browser-views summary').click();
        await open();

        const request = async row=>({
            title:row.id.replaceAll('-',' '),widthM:truth.widthM,props:selection.map(model=>({sceneId:models[model].id,label:labels[model]})),
            layoutPng:await read(row,'overhead.png'),guidePng:await read(row,'occluded-noisy.png'),
            additionalPng:await read(row,'extra-fitting.png'),witnessPng:await read(row,'held-out.png'),
            cameraAngles:row.cameraAngles,additionalCamera:row.additionalCamera
        });
        const first = truth.cases[0], initial = await request(first);
        const negatives = {
            uncleaned:await refusal(initial,/Occluded side view refused the combined silhouettes/),
            excess:await refusal({...initial,cleanOccludedMask:true,guidePng:await read(first,'excess-noise.png')},
                /Too many isolated changes/),
            missingRear:await refusal({...initial,cleanOccludedMask:true,guidePng:await read(first,'missing-rear.png')},
                /Occluded side view refused the combined silhouettes/),
            noisyWitness:await refusal({...initial,cleanOccludedMask:true,witnessPng:await read(first,'held-out-isolated-speck.png')},
                /Held-out view contains detached pixels/),
            reusedWitness:await refusal({...initial,cleanOccludedMask:true,witnessPng:initial.additionalPng},
                /Choose a separate held-out camera image/)
        };
        assert.equal((await call('list')).scenes.length,3);

        await page.locator('#image-assembly-add').click();
        for (let i=0;i<selection.length;i++) {
            await field('sceneId').nth(i).selectOption(models[selection[i]].id);
            await field('label').nth(i).fill(labels[selection[i]]);
        }
        await field('title').fill(initial.title);
        await field('widthM').fill(String(truth.widthM));
        await field('mode').selectOption('occluded');
        assert.equal(await field('cleanOccludedMask').isChecked(),false);
        for (const [key,value] of Object.entries(first.cameraAngles)) await field(key).fill(String(value));
        await field('additionalHeadingDeg').fill(String(first.additionalCamera.headingDeg));
        await field('additionalElevationDeg').fill(String(first.additionalCamera.elevationDeg));
        await upload(first,'layout','overhead.png');
        await upload(first,'guide','occluded-noisy.png');
        await upload(first,'additional','extra-fitting.png');
        await upload(first,'witness','held-out.png');
        await page.locator('#image-assembly-build').click();
        await status('Occluded side view refused the combined silhouettes');
        await capture('uncleaned-mask-refused.png');

        await field('cleanOccludedMask').check();
        await page.locator('#image-assembly-build').click();
        await status('isolated pixels corrected');
        await page.waitForFunction(()=>!document.querySelector('#image-assembly-build')?.disabled
            && document.querySelector('#spatial-source')?.textContent.startsWith('Image scene'));
        await page.locator('#image-assembly summary').evaluate(el=>el.scrollIntoView({block:'start'}));
        await capture('noisy-side-recovered.png');
        const firstId = await page.locator('#spatial-scenes').inputValue();

        const results = [];
        for (const row of truth.cases) {
            const inputs = {...await request(row),cleanOccludedMask:true};
            const refusedWitness = await refusal({...inputs,witnessPng:await read(row,'wrong-held-out.png')},/Held-out view refused/);
            const scene = row===first?await call('read',{id:firstId}):await call('image-assembly',inputs);
            const binding = scene.imageAssembly.binding;
            const {registration} = binding;
            const {occludedSide} = registration;
            const cleanup = occludedSide.maskCleanup;
            assert.equal(registration.policy,'registered-silhouettes-tilt-v6');
            assert.equal(binding.input.cleanOccludedMask,true);
            assert.equal(registration.guideSha256,sha(Buffer.from(inputs.guidePng,'base64')));
            assert.equal(registration.additionalSha256,sha(Buffer.from(inputs.additionalPng,'base64')));
            assert.equal(registration.witnessSha256,sha(Buffer.from(inputs.witnessPng,'base64')));
            assert(Math.abs(cleanup.removedSpecks-row.noise.injectedSpecks)<=5,asJson({id:row.id,cleanup,noise:row.noise}));
            assert(Math.abs(cleanup.filledHoles-row.noise.injectedHoles)<=5,asJson({id:row.id,cleanup,noise:row.noise}));
            assert(cleanup.removedSpecks+cleanup.filledHoles<=cleanup.maximumChangedPixels);
            assert(occludedSide.compositeIou>=.90 && occludedSide.compositeIou-cleanup.rawCompositeIou>.05);
            assert(Math.abs(occludedSide.overlapPixels-row.overlapPixels)<260);
            assert.deepEqual(scene.imageAssembly.measured,scene.imageAssembly.repeat);
            const measured = selection.map((model,i)=>{
                const target = row.samples.find(ref=>ref.model===model);
                const body = scene.objects[i+1], projection = registration.props[i];
                assert.equal(body.visualMesh,models[model].reconstruction.meshSha256);
                assert(projection.witnessIou>=registration.minimumWitnessIou);
                assert(scene.imageAssembly.measured[i].contactTick>0);
                const positionErrorM = Math.hypot(...body.position.map((v,axis)=>v-target.positionM[axis]));
                const angularErrorDeg = rotationError(body.rotation,target.quaternion);
                assert(positionErrorM<=.025 && angularErrorDeg<=7,
                    asJson({id:row.id,target,projection,positionErrorM,angularErrorDeg}));
                return {model,label:labels[model],positionErrorM,angularErrorDeg};
            });
            const glb = await call('export-glb',{id:scene.id});
            const bytes = Buffer.from(glb.base64,'base64');
            assert.equal(sha(bytes),glb.sha256);
            if (row===first) {
                const original = JSON.parse(scene.sourceText);
                const roundTrip = JSON.parse((await call('import-glb',{base64:glb.base64})).sourceText);
                checkRotatedRoundTrip(original,roundTrip,{...original.source,note:'GLB SHA-256 '+glb.sha256+'. '+original.source.note});
            }
            const destination = path.join(out,row.id);
            await fs.mkdir(destination,{recursive:true});
            await fs.writeFile(path.join(destination,'image-workshop.glb'),bytes);
            await fs.writeFile(path.join(destination,'image-workshop.json'),scene.sourceText);
            await fs.writeFile(path.join(destination,'image-assembly-evidence.json'),asJson({glbSha256:glb.sha256}));
            results.push({id:row.id,sceneId:scene.id,sourceSha256:scene.sourceSha256,glbSha256:glb.sha256,
                measured,registration,refusedWitness,physics:scene.imageAssembly.measured});
            console.log('Completed',row.id,JSON.stringify({raw:cleanup.rawCompositeIou,checked:occludedSide.compositeIou,measured}));
        }

        await app.close();
        ({app,page}=await launch(project,args));
        await open();
        for (const result of results) {
            const id = result.sceneId;
            const reloaded = await call('read',{id});
            assert.equal(reloaded.sourceSha256,result.sourceSha256);
            assert.deepEqual(reloaded.imageAssembly.binding.registration,result.registration);
            assert.deepEqual((await call('image-assembly-replay',{id})).measured,result.physics);
            assert.equal((await call('export-glb',{id})).sha256,result.glbSha256);
        }
        const last = results.at(-1);
        await page.locator('#spatial-scenes').selectOption(last.sceneId);
        await status('isolated pixels corrected');
        assert(await field('cleanOccludedMask').isChecked());
        await page.locator('#image-assembly-replay').click();
        await status('Full poses and occluded side rechecked');
        await page.locator('#image-assembly summary').evaluate(el=>el.scrollIntoView({block:'start'}));
        await capture('noisy-side-after-restart.png');

        const changed = path.join(project,'spatial','image-assemblies',firstId+'.guide.png');
        const exact = await fs.readFile(changed);
        await fs.writeFile(changed,Buffer.from(await read(first,'occluded-clean.png'),'base64'));
        await assert.rejects(call('image-assembly-replay',{id:firstId}),/registered views/);
        await assert.rejects(call('export-glb',{id:firstId}),/registered views/);
        await fs.writeFile(changed,exact);
        assert.deepEqual((await call('image-assembly-replay',{id:firstId})).measured,results[0].physics);

        await fs.writeFile(path.join(out,'noisy-occlusion-evidence.json'),asJson({platform:process.platform,truth,negatives,results,
            exactReplayAfterRestart:true,refusedChangedOriginal:true}));
    } catch (error) {
        console.error('Isolated project',project);
        console.error('Last status',await page.locator('#image-assembly-status').textContent().catch(()=>'unavailable'));
        await capture('failed.png').catch(()=>{});
        throw error;
    } finally {
        await app.close();
    }
}
test().catch(error=>{console.error(error);process.exitCode=1;});
