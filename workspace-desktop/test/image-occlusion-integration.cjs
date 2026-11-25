'use strict';

const assert = require('node:assert/strict');
const {createHash} = require('node:crypto');
const fs = require('node:fs/promises');
const os = require('node:os');
const path = require('node:path');
const {launch} = require('./calibration-integration.cjs');
const {fitRegisteredViews, checkRotatedRoundTrip} = require('./image-assembly-integration.cjs');

const inputs = path.join(__dirname,'fixtures','image-occlusion');
const refs = {front:'front.png',side:'side.png',top:'top.png',heldOut:'elevated.png'};
const sha = bytes=>createHash('sha256').update(bytes).digest('hex');

function rotationError(actual, expected) {
    const dot = actual.reduce((sum,q,i)=>sum+q*expected[i],0);
    return 2*Math.acos(Math.min(1,Math.abs(dot)/(Math.hypot(...actual)*Math.hypot(...expected))))*180/Math.PI;
}

async function test() {
    process.env.FRAYNIX_WORKSPACE_NO_MODELS = '1';
    const out = path.resolve(process.argv[2] || 'build/image-occlusion252');
    const project = await fs.mkdtemp(path.join(os.tmpdir(),'Fraynix partial occlusion '));
    const truth = JSON.parse(await fs.readFile(path.join(inputs,'truth.json'),'utf8'));
    const args = process.platform==='linux' ? ['--no-sandbox','--disable-dev-shm-usage'] : [];
    await fs.mkdir(out,{recursive:true});
    let {app,page} = await launch(project,args);
    const call = (route,input={})=>app.evaluate((_,arg)=>global.workspaceTest.api('/api/spatial/'+arg.route,arg.input),{route,input});
    const field = name=>page.locator('#image-assembly [name='+name+']');
    const indicator = page=>page.locator('#image-assembly-status');
    const status = phrase=>page.waitForFunction(value=>document.querySelector('#image-assembly-status')?.textContent.includes(value),
        phrase,{timeout:90000});
    const capture = async file=>{
        await page.evaluate(()=>new Promise(resolve=>requestAnimationFrame(()=>requestAnimationFrame(resolve))));
        await fs.writeFile(path.join(out,file),Buffer.from(await app.evaluate(()=>global.workspaceTest.capture()),'base64'));
    };
    const read = async file=>(await fs.readFile(path.join(inputs,file))).toString('base64');
    const upload = (name,file)=>field(name).setInputFiles(path.join(inputs,file));
    const open = async()=>{
        await page.locator('#spatial-tab').click();
        const details = page.locator('#image-assembly');
        if (!await details.evaluate(el=>el.open)) await details.locator('summary').click();
    };

    try {
        await page.locator('#spatial-tab').click();
        await page.locator('#spatial-browser-views summary').click();
        const elbow = await fitRegisteredViews(page,call,'image-yaw/elbow',refs,60);
        const step = await fitRegisteredViews(page,call,'image-yaw/step',refs,60);
        const fork = await fitRegisteredViews(page,call,'image-pose-survey/fork',refs,60);
        await page.locator('#spatial-browser-views summary').click();
        await open();

        const assets = [step,fork,elbow], expected = [truth.samples[1],truth.samples[2],truth.samples[0]];
        const labels = ['Offset step','Fork bracket','Elbow bracket'];
        const title = 'Recovered with an extra fitting view';
        const request = {title,widthM:truth.widthM,cameraAngles:truth.cameraAngles,layoutPng:await read('overhead.png'),
            guidePng:await read('occluded-side.png'),witnessPng:await read('held-out.png'),
            props:assets.map((asset,i)=>({sceneId:asset.id,label:labels[i]}))};
        const recovery = {...request,additionalCamera:truth.additionalCamera,additionalPng:await read('extra-fitting.png')};
        const refusals = {};
        async function refuse(kind,input,diagnostic) {
            await assert.rejects(call('image-assembly',input),error=>{
                refusals[kind] = error.message.split('\n',1)[0].replace(/^.*?Error: /,'');
                return diagnostic.test(refusals[kind]);
            });
        }
        await refuse('withoutExtra',request,/Fitting side needs one separated silhouette/);
        await refuse('incompletePair',{...recovery,additionalCamera:undefined},/needs four registered images/);
        await refuse('missingRear',{...recovery,guidePng:await read('missing-rear.png')},/Occluded side view refused the combined silhouettes/);
        await refuse('wrongHeldOut',{...recovery,witnessPng:await read('wrong-held-out.png')},/Held-out view refused/);
        await refuse('reusedHeldOut',{...recovery,witnessPng:recovery.additionalPng},/Choose a separate held-out camera image/);
        assert.equal((await call('list')).scenes.length,3);

        await page.locator('#image-assembly-add').click();
        for (let i=0;i<assets.length;i++) {
            await field('sceneId').nth(i).selectOption(assets[i].id);
            await field('label').nth(i).fill(labels[i]);
        }
        await field('title').fill(title);
        await field('widthM').fill(String(truth.widthM));
        await field('mode').selectOption('occluded');
        for (const [key,value] of Object.entries(truth.cameraAngles)) await field(key).fill(String(value));
        await field('additionalHeadingDeg').fill(String(truth.additionalCamera.headingDeg));
        await field('additionalElevationDeg').fill(String(truth.additionalCamera.elevationDeg));
        await upload('layout','overhead.png');
        await upload('guide','occluded-side.png');
        await upload('additional','extra-fitting.png');
        await upload('witness','wrong-held-out.png');
        await page.locator('#image-assembly-build').click();
        await status('Held-out view refused');
        await indicator(page).evaluate(el=>el.scrollIntoView({block:'nearest'}));
        await capture('wrong-held-out-refused.png');

        await upload('witness','held-out.png');
        await page.locator('#image-assembly-build').click();
        await status('3 full poses verified');
        const id = await page.locator('#spatial-scenes').inputValue();
        const scene = await call('read',{id});
        const receipt = scene.imageAssembly;
        const binding = receipt.binding;
        const validation = binding.registration;
        const observed = validation.occludedSide;
        assert.equal(validation.policy,'registered-silhouettes-tilt-v5');
        assert.deepEqual(binding.input.cameraAngles,truth.cameraAngles);
        assert.deepEqual(binding.input.additionalCamera,truth.additionalCamera);
        assert.equal(validation.guideSha256,sha(Buffer.from(recovery.guidePng,'base64')));
        assert.equal(validation.additionalSha256,sha(Buffer.from(recovery.additionalPng,'base64')));
        assert.equal(validation.witnessSha256,sha(Buffer.from(recovery.witnessPng,'base64')));
        assert(observed.compositeIou>=.90);
        assert(Math.abs(observed.overlapPixels-truth.visibility.overlapPixels)<200);
        const visible = observed.props[0];
        assert(visible.exclusiveFraction>.35 && visible.exclusiveFraction<.8);
        assert(visible.exclusiveSupport>.85);
        assert.deepEqual(receipt.measured,receipt.repeat);
        const measured = expected.map((target,i)=>{
            const body=scene.objects[i+1], row=validation.props[i];
            assert.equal(body.visualMesh,assets[i].reconstruction.meshSha256);
            assert.deepEqual(body.position,row.positionM);
            assert(row.additionalIou>=.83 && row.witnessIou>=.83);
            const physics=receipt.measured[i];
            assert(physics.contactTick>0 && physics.initialRotationErrorMilliDeg<=100);
            const positionErrorM=Math.hypot(...body.position.map((v,axis)=>v-target.positionM[axis]));
            const angleErrorDeg=rotationError(body.rotation,target.quaternion);
            assert(positionErrorM<=.025 && angleErrorDeg<=7,JSON.stringify({target,positionErrorM,angleErrorDeg,row}));
            return {label:labels[i],positionErrorM,angleErrorDeg,reference:target};
        });

        await page.waitForFunction(id=>document.querySelector('#spatial-scenes').value===id
            && document.querySelector('#spatial-source').textContent.startsWith('Image scene')
            && !document.querySelector('#image-assembly-build').disabled,id,{timeout:20000});
        await page.locator('#image-assembly summary').click();
        await page.locator('#spatial-stage').evaluate(el=>el.scrollIntoView({block:'center'}));
        await page.locator('#spatial-stage').hover();
        await page.mouse.wheel(0,-160);
        await capture('recovered-three-props.png');

        const exported = await call('export-glb',{id});
        const bytes = Buffer.from(exported.base64,'base64');
        assert.equal(sha(bytes),exported.sha256);
        const original = JSON.parse(scene.sourceText);
        const restored = JSON.parse((await call('import-glb',{base64:exported.base64})).sourceText);
        const note = 'GLB SHA-256 '+exported.sha256+'. '+original.source.note;
        const maxVertexDeltaM = checkRotatedRoundTrip(original,restored,{...original.source,note});

        await app.close();
        ({app,page}=await launch(project,args));
        await open();
        await page.locator('#spatial-scenes').selectOption(id);
        await status('3 full poses verified');
        assert.equal(await field('mode').inputValue(),'occluded');
        assert(await field('customAngles').isHidden());
        assert.equal(await field('additionalHeadingDeg').inputValue(),String(truth.additionalCamera.headingDeg));
        assert.equal(await field('additionalElevationDeg').inputValue(),String(truth.additionalCamera.elevationDeg));
        for (const [key,value] of Object.entries(truth.cameraAngles)) assert.equal(await field(key).inputValue(),String(value));
        assert.deepEqual(await call('read',{id}),scene);
        await page.locator('#image-assembly-replay').click();
        await status('Full poses and occluded side rechecked');
        assert.equal((await call('export-glb',{id})).sha256,exported.sha256);
        await page.locator('#image-assembly summary').evaluate(el=>el.scrollIntoView({block:'start'}));
        await capture('occluded-side-after-restart.png');

        const retained = path.join(project,'spatial','image-assemblies',id);
        for (const [suffix,replacement] of [['.guide.png','missing-rear.png'],['.additional.png','held-out.png'],['.witness.png','wrong-held-out.png']]) {
            const file=retained+suffix, exact=await fs.readFile(file);
            await fs.writeFile(file,Buffer.from(await read(replacement),'base64'));
            await assert.rejects(call('image-assembly-replay',{id}),/registered views/);
            await assert.rejects(call('export-glb',{id}),/registered views/);
            await fs.writeFile(file,exact);
        }
        assert.deepEqual((await call('image-assembly-replay',{id})).measured,receipt.measured);
        const proof = {platform:process.platform,layoutSha256:binding.layoutSha256,sceneSha256:scene.sourceSha256,
            glbSha256:exported.sha256,validation,measured,truth,refusals,physics:receipt.measured,
            exactReplayAfterRestart:true,refusedChangedSideExtraAndHeldOut:true,maxVertexDeltaM};
        await fs.writeFile(path.join(out,'occlusion-verification.json'),JSON.stringify(proof,null,2)+'\n');
        await fs.writeFile(path.join(out,'image-workshop.json'),scene.sourceText);
        await fs.writeFile(path.join(out,'image-workshop.glb'),bytes);
        await fs.writeFile(path.join(out,'image-assembly-evidence.json'),JSON.stringify({glbSha256:exported.sha256})+'\n');
        console.log(JSON.stringify({observed,measured,refusals,glbSha256:exported.sha256}));
    } catch (error) {
        console.error('Isolated project',project);
        console.error('Image assembly status',await indicator(page).textContent().catch(()=>'unavailable'));
        await capture('failed.png').catch(()=>{});
        throw error;
    } finally {
        await app.close();
    }
}
test().catch(error=>{console.error(error);process.exitCode=1;});
