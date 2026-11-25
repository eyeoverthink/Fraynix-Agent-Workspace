'use strict';

const assert = require('node:assert/strict');
const {createHash} = require('node:crypto');
const fs = require('node:fs/promises');
const os = require('node:os');
const path = require('node:path');
const {launch} = require('./calibration-integration.cjs');
const {fitRegisteredViews,checkRotatedRoundTrip} = require('./image-assembly-integration.cjs');

const digest = bytes=>createHash('sha256').update(bytes).digest('hex');
const inputs = path.join(__dirname,'fixtures','image-tilt');
const refs = {front:'front.png',side:'side.png',top:'top.png',heldOut:'elevated.png'};

function quaternionError(actual, expected) {
    const dot=actual.reduce((sum,q,i)=>sum+q*expected[i],0);
    return 2*Math.acos(Math.min(1,Math.abs(dot)/(Math.hypot(...actual)*Math.hypot(...expected))))*180/Math.PI;
}

async function test() {
    process.env.FRAYNIX_WORKSPACE_NO_MODELS='1';
    const out=path.resolve(process.argv[2] || 'build/image-tilt250');
    const truth=JSON.parse(await fs.readFile(path.join(inputs,'truth.json'),'utf8'));
    const project=await fs.mkdtemp(path.join(os.tmpdir(),'Fraynix tilt '));
    const args=process.platform==='linux'?['--no-sandbox','--disable-dev-shm-usage']:[];
    await fs.mkdir(out,{recursive:true});
    let {app,page}=await launch(project,args);
    const call=(route,input={})=>app.evaluate((_,arg)=>global.workspaceTest.api('/api/spatial/'+arg.route,arg.input),{route,input});
    const field=name=>page.locator('#image-assembly [name='+name+']');
    const upload=(name,file)=>field(name).setInputFiles(path.join(inputs,file));
    const text=name=>page.locator(name).textContent();
    const status=phrase=>page.waitForFunction(value=>document.querySelector('#image-assembly-status')?.textContent.includes(value),
        phrase,{timeout:60000});
    const capture=async name=>{
        await page.evaluate(()=>new Promise(resolve=>requestAnimationFrame(()=>requestAnimationFrame(resolve))));
        await fs.writeFile(path.join(out,name),Buffer.from(await app.evaluate(()=>global.workspaceTest.capture()),'base64'));
    };
    const readInput=async file=>(await fs.readFile(path.join(inputs,file))).toString('base64');
    const open=async()=>{
        await page.locator('#spatial-tab').click();
        const details=page.locator('#image-assembly');
        if (!await details.evaluate(el=>el.open)) await details.locator('summary').click();
    };

    try {
        await page.locator('#spatial-tab').click();
        await page.locator('#spatial-browser-views summary').click();
        const elbow=await fitRegisteredViews(page,call,'image-yaw/elbow',refs,60);
        const step=await fitRegisteredViews(page,call,'image-yaw/step',refs,60);
        await page.locator('#spatial-browser-views summary').click();
        await open();

        const assets=[step,elbow], names=['Offset step','Elbow bracket'];
        for (let i=0;i<assets.length;i++) {
            await field('sceneId').nth(i).selectOption(assets[i].id);
            await field('label').nth(i).fill(names[i]);
        }
        await field('title').fill('Full poses from three views');
        await field('widthM').fill(String(truth.widthM));
        await field('mode').selectOption('tilt');
        await upload('layout','overhead.png');
        await upload('guide','fitting-side.png');
        await upload('witness','wrong-tilt.png');
        await page.locator('#image-assembly-build').click();
        await status('Held-out view refused Offset step');
        const wrongTilt=await text('#image-assembly-status');
        await page.locator('#image-assembly-status').evaluate(el=>el.scrollIntoView({block:'nearest'}));
        await capture('tilt-only-refused.png');
        assert.equal((await call('list')).scenes.length,2);

        const request={title:'Full poses from three views',widthM:truth.widthM,layoutPng:await readInput('overhead.png'),
            guidePng:await readInput('fitting-side.png'),witnessPng:await readInput('held-out.png'),
            props:assets.map((asset,i)=>({sceneId:asset.id,label:names[i]}))};
        await assert.rejects(call('image-assembly',{...request,witnessPng:await readInput('wrong-height.png')}),/Held-out view refused Offset step/);
        await assert.rejects(call('image-assembly',{...request,witnessPng:request.guidePng}),/separate held-out camera image/);
        assert.equal((await call('list')).scenes.length,2);

        await upload('witness','held-out.png');
        await page.locator('#image-assembly-build').click();
        await status('2 full poses verified');
        const id=await page.locator('#spatial-scenes').inputValue();
        const scene=await call('read',{id});
        const receipt=scene.imageAssembly;
        const validation=receipt.binding.registration;
        assert.equal(validation.policy,'registered-silhouettes-tilt-v3');
        assert.equal(validation.guideSha256,digest(Buffer.from(request.guidePng,'base64')));
        assert.equal(validation.witnessSha256,digest(Buffer.from(request.witnessPng,'base64')));
        assert.deepEqual(receipt.measured,receipt.repeat);
        const measured=truth.samples.slice().reverse().map((target,i)=>{
            const row=validation.props[i], body=scene.objects[i+1];
            assert.deepEqual(row.anglesDeg,receipt.binding.props[i].anglesDeg);
            assert.deepEqual(row.positionM,body.position);
            assert.equal(body.visualMesh,assets[i].reconstruction.meshSha256);
            assert(row.witnessIou>=validation.minimumWitnessIou);
            assert(receipt.measured[i].initialRotationErrorMilliDeg<=100);
            assert(receipt.measured[i].contactTick>0 && receipt.measured[i].rotationErrorMilliDeg>1000);
            const positionErrorM=Math.hypot(...body.position.map((v,j)=>v-target.positionM[j]));
            const angularErrorDeg=quaternionError(body.rotation,target.quaternion);
            assert(positionErrorM<=.024 && angularErrorDeg<=7,JSON.stringify({names,positionErrorM,angularErrorDeg,row}));
            return {label:names[i],positionErrorM,angularErrorDeg,reference:target};
        });
        await page.waitForFunction(id=>document.querySelector('#spatial-scenes').value===id
            && document.querySelector('#spatial-source').textContent.startsWith('Image scene')
            && !document.querySelector('#image-assembly-build').disabled,id,{timeout:20000});
        await page.locator('#image-assembly summary').click();
        await page.locator('#spatial-stage').evaluate(el=>el.scrollIntoView({block:'center'}));
        await page.locator('#spatial-stage').hover();
        await page.mouse.wheel(0,-270);
        await capture('assembled-tilted-scene.png');

        const glb=await call('export-glb',{id});
        const bytes=Buffer.from(glb.base64,'base64');
        assert.equal(digest(bytes),glb.sha256);
        const reimport=await call('import-glb',{base64:glb.base64});
        const original=JSON.parse(scene.sourceText);
        const restored=JSON.parse(reimport.sourceText);
        const expectedNote='GLB SHA-256 '+glb.sha256+'. '+original.source.note;
        const maxGlbVertexDeltaM=checkRotatedRoundTrip(original,restored,{...original.source,note:expectedNote});

        await app.close();
        ({app,page}=await launch(project,args));
        await open();
        await page.locator('#spatial-scenes').selectOption(id);
        await status('2 full poses verified');
        assert.equal(await field('mode').inputValue(),'tilt');
        assert.deepEqual(await call('read',{id}),scene);
        await page.locator('#image-assembly-replay').click();
        await status('Full poses rechecked.');
        assert.equal((await call('export-glb',{id})).sha256,glb.sha256);
        await page.locator('#image-assembly-status').evaluate(el=>el.scrollIntoView({block:'nearest'}));
        await capture('tilted-scene-after-restart.png');

        const retained=path.join(project,'spatial','image-assemblies',id);
        for (const [ext,replacement] of [['.guide.png','wrong-height.png'],['.witness.png','wrong-tilt.png']]) {
            const stored=retained+ext, exact=await fs.readFile(stored);
            await fs.writeFile(stored,await fs.readFile(path.join(inputs,replacement)));
            await assert.rejects(call('image-assembly-replay',{id}),/registered views/);
            await assert.rejects(call('export-glb',{id}),/registered views/);
            await fs.writeFile(stored,exact);
        }
        const propPackage=path.join(project,'spatial','reconstruction-sources',step.id+'.json');
        const exact=await fs.readFile(propPackage);
        await fs.writeFile(propPackage,Buffer.concat([exact,Buffer.from('changed')]));
        await assert.rejects(call('export-glb',{id}),/registered views/);
        await fs.writeFile(propPackage,exact);
        assert.deepEqual((await call('image-assembly-replay',{id})).measured,receipt.measured);

        const proof={platform:process.platform,layoutSha256:receipt.binding.layoutSha256,sceneSha256:scene.sourceSha256,
            glbSha256:glb.sha256,validation,measured,physics:receipt.measured,
            truth,wrongTilt,maxGlbVertexDeltaM,refusedHeightOnly:true,refusedDuplicateWitness:true,
            matchedReverseSelectionOrder:true,restoredAfterRestart:true,refusedChangedGuide:true,
            refusedChangedWitness:true,refusedChangedModelImages:true};
        await fs.writeFile(path.join(out,'pose-verification.json'),JSON.stringify(proof,null,2)+'\n');
        await fs.writeFile(path.join(out,'image-workshop.json'),scene.sourceText);
        await fs.writeFile(path.join(out,'image-workshop.glb'),bytes);
        // The same Blender readback validates scene IDs, all three axes, vertices and triangles.
        await fs.writeFile(path.join(out,'image-assembly-evidence.json'),JSON.stringify({glbSha256:glb.sha256})+'\n');
        console.log(JSON.stringify(proof));
    } catch (error) {
        console.error('Temporary pose test project',project);
        console.error('Image-scene status',await text('#image-assembly-status').catch(()=>'unavailable'));
        await capture('failed.png').catch(()=>{});
        throw error;
    } finally {
        await app.close();
    }
}

test().catch(error=>{console.error(error);process.exitCode=1;});
