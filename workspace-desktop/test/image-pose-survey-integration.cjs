'use strict';

const assert = require('node:assert/strict');
const {createHash} = require('node:crypto');
const fs = require('node:fs/promises');
const os = require('node:os');
const path = require('node:path');
const {launch} = require('./calibration-integration.cjs');
const {fitRegisteredViews, checkRotatedRoundTrip} = require('./image-assembly-integration.cjs');

const inputs = path.join(__dirname,'fixtures','image-pose-survey');
const refs = {front:'front.png',side:'side.png',top:'top.png',heldOut:'elevated.png'};
const sha = bytes=>createHash('sha256').update(bytes).digest('hex');

function rotationError(actual, expected) {
    const agreement = actual.reduce((sum,q,i)=>sum+q*expected[i],0);
    const cosine = Math.abs(agreement) / (Math.hypot(...actual)*Math.hypot(...expected));
    return 2*Math.acos(Math.min(1,cosine))*180/Math.PI;
}

async function test() {
    process.env.FRAYNIX_WORKSPACE_NO_MODELS = '1';
    const out = path.resolve(process.argv[2] || 'build/pose-survey251');
    const project = await fs.mkdtemp(path.join(os.tmpdir(),'Fraynix pose survey '));
    const truth = JSON.parse(await fs.readFile(path.join(inputs,'truth.json'),'utf8'));
    const args = process.platform==='linux' ? ['--no-sandbox','--disable-dev-shm-usage'] : [];
    await fs.mkdir(out,{recursive:true});
    let {app,page} = await launch(project,args);
    const call = (route,input={})=>app.evaluate((_,arg)=>global.workspaceTest.api('/api/spatial/'+arg.route,arg.input),{route,input});
    const field = name=>page.locator('#image-assembly [name='+name+']');
    const status = pageStatus=>page.waitForFunction(value=>document.querySelector('#image-assembly-status')?.textContent.includes(value),
        pageStatus,{timeout:90000});
    const upload = (name,file)=>field(name).setInputFiles(path.join(inputs,file));
    const resultText = ()=>page.locator('#image-assembly-status').textContent();
    const read = async file=>(await fs.readFile(path.join(inputs,file))).toString('base64');
    const open = async()=>{
        await page.locator('#spatial-tab').click();
        const details = page.locator('#image-assembly');
        if (!await details.evaluate(el=>el.open)) await details.locator('summary').click();
    };
    const capture = async name=>{
        await page.evaluate(()=>new Promise(resolve=>requestAnimationFrame(()=>requestAnimationFrame(resolve))));
        const bytes = Buffer.from(await app.evaluate(()=>global.workspaceTest.capture()),'base64');
        await fs.writeFile(path.join(out,name),bytes);
    };

    try {
        await page.locator('#spatial-tab').click();
        await page.locator('#spatial-browser-views summary').click();
        const elbow = await fitRegisteredViews(page,call,'image-yaw/elbow',refs,60);
        const step = await fitRegisteredViews(page,call,'image-yaw/step',refs,60);
        const fork = await fitRegisteredViews(page,call,'image-pose-survey/fork',refs,60);
        const symmetric = await fitRegisteredViews(page,call,'image-pose-survey/symmetric',refs,60);
        await page.locator('#spatial-browser-views summary').click();
        await open();

        const assets = [fork,elbow,step], expected = [truth.samples[2],truth.samples[0],truth.samples[1]];
        const names = ['Fork bracket','Elbow bracket','Offset step'];
        await page.locator('#image-assembly-add').click();
        for (let i=0;i<assets.length;i++) {
            await field('sceneId').nth(i).selectOption(assets[i].id);
            await field('label').nth(i).fill(names[i]);
        }
        await field('title').fill('Three props from calibrated angles');
        await field('widthM').fill(String(truth.widthM));
        await field('mode').selectOption('tilt');
        await field('customAngles').check();
        for (const [key,value] of Object.entries(truth.cameraAngles)) await field(key).fill(String(value));
        await upload('layout','overhead-occluded.png');
        await upload('guide','fitting-side.png');
        await upload('witness','held-out.png');
        await page.locator('#image-assembly-build').click();
        await status('Overhead needs one separated silhouette per selected prop');
        const refusedOcclusion = await resultText();
        await page.locator('#image-assembly-status').evaluate(el=>el.scrollIntoView({block:'nearest'}));
        await capture('overlapping-props-refused.png');

        const request = {title:'Three props from calibrated angles',widthM:truth.widthM,
            layoutPng:await read('overhead.png'),guidePng:await read('fitting-side.png'),witnessPng:await read('held-out.png'),
            cameraAngles:truth.cameraAngles,props:assets.map((asset,i)=>({sceneId:asset.id,label:names[i]}))};
        const refusals = {occlusion:refusedOcclusion};
        async function refuse(kind,input,diagnostic) {
            await assert.rejects(call('image-assembly',input),error=>{
                refusals[kind] = error.message.split('\n',1)[0].replace(/^.*?Error: /,'');
                return diagnostic.test(refusals[kind]);
            });
        }
        await refuse('insufficientSeparation',{...request,cameraAngles:{...truth.cameraAngles,
            witnessHeadingDeg:-18,witnessElevationDeg:49}},/must be at least 25° apart/);
        await refuse('wrongHeldOutCamera',{...request,witnessPng:await read('wrong-held-out-angle.png')},/Held-out view refused/);
        await refuse('ambiguousHalfTurn',{...request,props:[{sceneId:symmetric.id,label:'Symmetric block'},...request.props.slice(1)],
            layoutPng:await read('overhead-ambiguous.png'),guidePng:await read('fitting-side-ambiguous.png'),
            witnessPng:await read('held-out-ambiguous.png')},/Symmetric block's tilt or heading ambiguous/);
        assert.equal((await call('list')).scenes.length,4);

        await upload('layout','overhead.png');
        await page.locator('#image-assembly-build').click();
        await status('3 full poses verified');
        const id = await page.locator('#spatial-scenes').inputValue();
        const scene = await call('read',{id});
        const receipt = scene.imageAssembly;
        const validation = receipt.binding.registration;
        assert.equal(validation.policy,'registered-silhouettes-tilt-v4');
        assert.deepEqual(receipt.binding.input.cameraAngles,truth.cameraAngles);
        assert.equal(validation.guideHeadingDeg,truth.cameraAngles.sideHeadingDeg);
        assert.equal(validation.guideElevationDeg,truth.cameraAngles.sideElevationDeg);
        assert.equal(validation.witnessHeadingDeg,truth.cameraAngles.witnessHeadingDeg);
        assert.equal(validation.witnessElevationDeg,truth.cameraAngles.witnessElevationDeg);
        assert.equal(validation.guideSha256,sha(Buffer.from(request.guidePng,'base64')));
        assert.equal(validation.witnessSha256,sha(Buffer.from(request.witnessPng,'base64')));
        assert.deepEqual(receipt.measured,receipt.repeat);

        const measured = expected.map((target,i)=>{
            const row = validation.props[i], body = scene.objects[i+1];
            assert.equal(body.visualMesh,assets[i].reconstruction.meshSha256);
            assert.deepEqual(row.positionM,body.position);
            assert.deepEqual(row.anglesDeg,receipt.binding.props[i].anglesDeg);
            assert(row.witnessIou>=validation.minimumWitnessIou);
            assert(receipt.measured[i].contactTick>0 && receipt.measured[i].initialRotationErrorMilliDeg<=100);
            const positionErrorM = Math.hypot(...body.position.map((v,axis)=>v-target.positionM[axis]));
            const angleErrorDeg = rotationError(body.rotation,target.quaternion);
            assert(positionErrorM<=.02 && angleErrorDeg<=7,JSON.stringify({target,row,positionErrorM,angleErrorDeg}));
            return {label:names[i],positionErrorM,angleErrorDeg,reference:target};
        });
        await page.waitForFunction(id=>document.querySelector('#spatial-scenes').value===id
            && document.querySelector('#spatial-source').textContent.startsWith('Image scene')
            && !document.querySelector('#image-assembly-build').disabled,id,{timeout:20000});
        await page.locator('#image-assembly summary').click();
        await page.locator('#spatial-stage').evaluate(el=>el.scrollIntoView({block:'center'}));
        await capture('three-prop-scene.png');

        const glb = await call('export-glb',{id});
        const bytes = Buffer.from(glb.base64,'base64');
        assert.equal(sha(bytes),glb.sha256);
        const reimport = await call('import-glb',{base64:glb.base64});
        const original = JSON.parse(scene.sourceText);
        const restored = JSON.parse(reimport.sourceText);
        const note = 'GLB SHA-256 '+glb.sha256+'. '+original.source.note;
        const maxVertexDeltaM = checkRotatedRoundTrip(original,restored,{...original.source,note});

        await app.close();
        ({app,page}=await launch(project,args));
        await open();
        await page.locator('#spatial-scenes').selectOption(id);
        await status('3 full poses verified');
        assert(await field('customAngles').isChecked());
        for (const [key,value] of Object.entries(truth.cameraAngles)) assert.equal(await field(key).inputValue(),String(value));
        assert.deepEqual(await call('read',{id}),scene);
        await page.locator('#image-assembly-replay').click();
        await status('Fresh floor contacts matched for all 3 props');
        assert.equal((await call('export-glb',{id})).sha256,glb.sha256);
        await page.locator('#image-assembly-status').evaluate(el=>el.scrollIntoView({block:'nearest'}));
        await capture('camera-angles-after-restart.png');

        const witness = path.join(project,'spatial','image-assemblies',id+'.witness.png');
        const exact = await fs.readFile(witness);
        await fs.writeFile(witness,Buffer.from(await read('wrong-held-out-angle.png'),'base64'));
        await assert.rejects(call('image-assembly-replay',{id}),/registered views/);
        await assert.rejects(call('export-glb',{id}),/registered views/);
        await fs.writeFile(witness,exact);
        assert.deepEqual((await call('image-assembly-replay',{id})).measured,receipt.measured);

        const proof = {platform:process.platform,layoutSha256:receipt.binding.layoutSha256,sceneSha256:scene.sourceSha256,
            glbSha256:glb.sha256,validation,measured,truth,refusals,physics:receipt.measured,
            restoredAnglesAndGlbAfterRestart:true,refusedChangedWitness:true,maxVertexDeltaM};
        await fs.writeFile(path.join(out,'pose-survey.json'),JSON.stringify(proof,null,2)+'\n');
        await fs.writeFile(path.join(out,'image-workshop.json'),scene.sourceText);
        await fs.writeFile(path.join(out,'image-workshop.glb'),bytes);
        await fs.writeFile(path.join(out,'image-assembly-evidence.json'),JSON.stringify({glbSha256:glb.sha256})+'\n');
        console.log(JSON.stringify({measured,refusals,glbSha256:glb.sha256}));
    } catch (error) {
        console.error('Isolated project',project);
        console.error('Last status',await resultText().catch(()=>'unavailable'));
        await capture('failed.png').catch(()=>{});
        throw error;
    } finally {
        await app.close();
    }
}

test().catch(error=>{console.error(error);process.exitCode=1;});
