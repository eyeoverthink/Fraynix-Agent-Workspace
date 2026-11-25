'use strict';

const assert = require('node:assert/strict');
const {createHash} = require('node:crypto');
const fs = require('node:fs/promises');
const os = require('node:os');
const path = require('node:path');
const {launch} = require('./calibration-integration.cjs');

const hash = bytes => createHash('sha256').update(bytes).digest('hex');
const fixtures = path.join(__dirname,'fixtures');
const overlap = path.join(fixtures,'image-assembly','overlap.png');
const registrationFixtures = path.join(fixtures,'image-registration');

function checkRotatedRoundTrip(authored, imported, source) {
    const {objects:original,visualMeshes:originalMeshes,...before}=authored;
    const {objects:restored,visualMeshes:restoredMeshes,...after}=imported;
    assert.deepEqual(after,{...before,source});
    assert.equal(restored.length,original.length);
    let vertexDeltaM=0;
    for (let index=0;index<original.length;index++) {
        const {rotation,position,size,visualMesh,...expected}=original[index];
        const {rotation:q,position:p,size:s,visualMesh:mesh,...actual}=restored[index];
        assert.deepEqual(actual,expected);
        const dot=rotation.reduce((sum,value,axis)=>sum+value*q[axis],0);
        assert(Math.abs(dot)>1-1e-10);
        assert(Math.hypot(...position.map((x,i)=>x-p[i]))<=1e-5);
        assert(Math.hypot(...size.map((x,i)=>x-s[i]))<=1e-5);
        assert.equal(Boolean(mesh),Boolean(visualMesh));
        if (!mesh) continue;
        const a=Buffer.from(originalMeshes[visualMesh],'base64'), b=Buffer.from(restoredMeshes[mesh],'base64');
        assert(a.subarray(0,8).equals(b.subarray(0,8)));
        const end=8+a.readUInt32LE(0)*12;
        assert(a.subarray(end).equals(b.subarray(end)));
        for (let at=8;at<end;at+=4) vertexDeltaM=Math.max(vertexDeltaM,Math.abs(a.readFloatLE(at)-b.readFloatLE(at)));
    }
    assert(vertexDeltaM<=1e-5);
    return vertexDeltaM;
}

async function fitRegisteredViews(page,call,folder,files,elevation) {
    const clear = page.locator('#browser-views-clear');
    if (await clear.isEnabled()) await clear.click();
    const labels = {front:'front',side:'side',top:'top',heldOut:'held-out 45°'};
    for (const [role,file] of Object.entries(files)) {
        await page.locator('#registered-png-role').selectOption(role);
        await page.locator('#registered-png-file').setInputFiles(path.join(fixtures,folder,file));
        await page.waitForFunction(text=>document.querySelector('#browser-views-status').textContent.startsWith(text),
            'Loaded '+labels[role]);
    }
    await page.locator('#browser-views-elevation').selectOption(String(elevation));
    const before = (await call('list')).scenes.length;
    await page.locator('#browser-views-build').click();
    await page.waitForFunction(count=>{
        const select = document.querySelector('#spatial-scenes');
        const status = document.querySelector('#browser-views-status').textContent;
        if (status.includes('refused') || status.includes('could not be reproduced')) throw Error(status);
        return select.options.length===count+1 && select.value===select.options[count].value;
    },before,{timeout:12000});
    const id = await page.locator('#spatial-scenes').inputValue();
    const source = await call('read',{id});
    for (const [role,name] of Object.entries(files)) {
        const view = source.reconstruction.views[role];
        assert.equal(view.fileName,name);
        assert.equal(view.pngSha256,hash(await fs.readFile(path.join(fixtures,folder,name))));
    }
    return source;
}

async function main() {
    process.env.FRAYNIX_WORKSPACE_NO_MODELS = '1';
    const recoverYaw = process.argv[3]==='yaw';
    const registered = recoverYaw || process.argv[3]==='registered';
    const registration = recoverYaw ? path.join(fixtures,'image-yaw') : registrationFixtures;
    const out = path.resolve(process.argv[2] || (recoverYaw?'build/image-yaw249':registered?'build/image-registration248':'build/image-assembly247'));
    const layout = path.join(registered?registration:path.join(fixtures,'image-assembly'),'overhead.png');
    const witness = path.join(registration,'second-angle.png');
    const truth = registered?JSON.parse(await fs.readFile(path.join(registration,'truth.json'),'utf8')):null;
    await fs.mkdir(out,{recursive:true});
    const project = await fs.mkdtemp(path.join(os.tmpdir(),'Fraynix image assembly '));
    const extraArgs = process.platform==='linux' ? ['--no-sandbox','--disable-dev-shm-usage'] : [];
    let {app,page} = await launch(project,extraArgs);
    const call = (route,input={}) => app.evaluate((_,command) =>
        global.workspaceTest.api('/api/spatial/'+command.route,command.input),{route,input});
    const assetPixels = id=>path.join(project,'spatial','reconstruction-sources',id+'.json');
    const capture = async name => {
        await page.evaluate(()=>new Promise(resolve=>requestAnimationFrame(()=>requestAnimationFrame(resolve))));
        const capture = await app.evaluate(()=>global.workspaceTest.capture());
        await fs.writeFile(path.join(out,name),Buffer.from(capture,'base64'));
    };
    const status = text=>page.waitForFunction(phrase=>
        document.querySelector('#image-assembly-status')?.textContent.includes(phrase),text,{timeout:12000});
    const open = async () => {
        await page.locator('#spatial-tab').click();
        const panel = page.locator('#image-assembly');
        if (!await panel.evaluate(node=>node.open)) await panel.locator('summary').click();
    };
    const restart = async () => {
        await app.close();
        ({app,page} = await launch(project,extraArgs));
        await open();
    };
    const fit = (folder,files,elevation) => fitRegisteredViews(page,call,folder,files,elevation);

    try {
        await page.locator('#spatial-tab').click();
        await page.locator('#spatial-browser-views summary').click();
        const elevated = {front:'front.png',side:'side.png',top:'top.png',heldOut:'elevated.png'};
        const first = recoverYaw ? await fit('image-yaw/elbow',elevated,60)
            : await fit('browser-views',{front:'front.png',side:'side.png',heldOut:'held-out.png'},0);
        const second = await fit(recoverYaw?'image-yaw/step':'browser-top',elevated,60);
        await page.locator('#browser-views-status').evaluate(element=>element.scrollIntoView({block:'nearest'}));
        await capture('local-views-checked.png');
        await page.locator('#spatial-browser-views summary').click();
        assert.notEqual(first.reconstruction.meshSha256,second.reconstruction.meshSha256);
        assert(first.reconstruction.heldOutIou>.9 && second.reconstruction.heldOutIou>.9);
        await open();
        const assets = registered ? [second,first] : [first,second];
        const labels = recoverYaw ? ['Offset step','Elbow bracket']
            : registered ? ['Perforated plate','Stepped bracket'] : ['Stepped bracket','Perforated plate'];
        const prop = page.locator('#image-assembly select[name=sceneId]');
        const label = page.locator('#image-assembly input[name=label]');
        for (const [index,asset] of assets.entries()) {
            await prop.nth(index).selectOption(asset.id);
            await label.nth(index).fill(labels[index]);
        }
        const title = recoverYaw?'Unmarked rotating props':registered?'Unmarked image workshop':'Image assembly workshop';
        await page.locator('#image-assembly input[name=title]').fill(title);
        const layoutField = page.locator('#image-assembly input[name=layout]');
        const witnessField = page.locator('#image-assembly input[name=witness]');
        if (registered) {
            await page.locator('#image-assembly select[name=mode]').selectOption(recoverYaw?'yaw':'views');
            await page.locator('#image-assembly .assembly-color').first().waitFor({state:'hidden'});
            await layoutField.setInputFiles(layout);
            await witnessField.setInputFiles(path.join(registration,recoverYaw?'rotated-second-angle.png':'shifted-second-angle.png'));
        } else await layoutField.setInputFiles(overlap);
        await page.locator('#image-assembly-build').click();
        await status(recoverYaw?'Second view refused Offset step orientation':registered?'Second view refused Perforated plate':'colliders overlap');
        const refusal = await page.locator('#image-assembly-status').textContent();
        assert.equal((await call('list')).scenes.length,2);
        const rejected = page.locator('#image-assembly-status');
        await rejected.evaluate(element=>element.scrollIntoView({block:'nearest'}));
        await capture(recoverYaw?'rotated-second-view-refused.png':registered?'shifted-second-view-refused.png':'overlapping-layout-refused.png');

        if (registered) {
            const layoutPng=(await fs.readFile(layout)).toString('base64');
            const witnessPng=(await fs.readFile(witness)).toString('base64');
            const request={title,widthM:truth.widthM,layoutPng,witnessPng,
                props:assets.map((asset,index)=>({sceneId:asset.id,label:labels[index]})),...(recoverYaw?{recoverYaw:true}:{})};
            await assert.rejects(call('image-assembly',{...request,witnessPng:layoutPng}),/separate registered second view/);
            await assert.rejects(call('image-assembly',{...request,props:[request.props[0],request.props[0]]}),/do not uniquely identify/);
            if (recoverYaw) {
                const shifted=(await fs.readFile(path.join(registration,'shifted-second-angle.png'))).toString('base64');
                await assert.rejects(call('image-assembly',{...request,witnessPng:shifted}),/Second view refused Offset step at its overhead position/);
            }
            assert.equal((await call('list')).scenes.length,2);
            await witnessField.setInputFiles(witness);
        } else await layoutField.setInputFiles(layout);
        await page.locator('#image-assembly-build').click();
        await status(recoverYaw?'2 positions and rotations verified':registered?'2 positions verified in the second view':'2 props · floor contact verified twice');
        const id = await page.locator('#spatial-scenes').inputValue();
        const scene = await call('read',{id});
        assert.equal(scene.title,title);
        assert.equal(scene.objects.length,3);
        const receipt = scene.imageAssembly;
        const placement = receipt.binding.registration;
        if (registered) {
            assert.equal(placement.witnessSha256,hash(await fs.readFile(witness)));
            assert(placement.props.every(p=>p.witnessIou>=placement.minimumWitnessIou
                && p.witnessCenterErrorPx<=placement.maximumWitnessCenterErrorPx));
        } else assert.deepEqual(receipt.binding.props.map(p=>p.markerCenterPx),[[110,105],[305,210]]);
        assert.equal(receipt.binding.metresPerPixel,.01);
        const reference = recoverYaw?[truth.samples[1],truth.samples[0]]:null;
        const expected = recoverYaw?reference.map(prop=>prop.positionXZ)
            : registered?[truth.propsXZ[1],truth.propsXZ[0]]:[[-.90,-.45],[1.05,.60]];
        const positionErrorsM = [];
        const yawErrorsDeg = [];
        for (let i=0;i<expected.length;i++) {
            const object = scene.objects[i+1];
            positionErrorsM.push(Math.hypot(object.position[0]-expected[i][0],object.position[2]-expected[i][1]));
            assert(positionErrorsM[i]<=(registered ? .02 : 0));
            const asset = assets[i];
            assert.equal(object.label,labels[i]);
            assert.equal(object.visualMesh,asset.reconstruction.meshSha256);
            assert.equal(scene.visualMeshes[object.visualMesh],asset.visualMeshes[object.visualMesh]);
            assert.deepEqual(object.size,asset.objects[1].size);
            assert(receipt.measured[i].contactTick>0);
            assert(receipt.measured[i].speedMmps<=30);
            if (recoverYaw) {
                const measured = receipt.binding.props[i];
                const angle = 2*Math.atan2(object.rotation[1],object.rotation[3])*180/Math.PI;
                assert(Math.abs(angle-measured.yawDeg)<1e-8);
                yawErrorsDeg.push(Math.abs(measured.yawDeg-reference[i].yawDeg));
                assert(yawErrorsDeg[i]<=4,JSON.stringify({measured,reference:reference[i]}));
                assert(placement.props[i].witnessYawErrorDeg<=placement.maximumWitnessYawErrorDeg);
                assert(receipt.measured[i].rotationErrorMilliDeg<=1000);
            }
        }
        assert.deepEqual(receipt.measured,receipt.repeat);
        await page.waitForFunction(selected=>{
            const source=document.querySelector('#spatial-source');
            return document.querySelector('#spatial-scenes').value===selected && source.textContent.startsWith('Image scene')
                && !document.querySelector('#image-assembly-build').disabled;
        },id,{timeout:18000});
        await page.locator('#image-assembly summary').click();
        if (recoverYaw) await page.locator('#spatial-top-down').click();
        await page.locator('#spatial-stage').evaluate(element=>element.scrollIntoView({block:'center'}));
        if (recoverYaw) {
            await page.locator('#spatial-stage').hover();
            await page.mouse.wheel(0,-300);
        }
        await capture('image-layout-assembled.png');

        const exported = await call('export-glb',{id});
        const bytes = Buffer.from(exported.base64,'base64');
        assert.equal(hash(bytes),exported.sha256);
        const roundTrip = await call('import-glb',{base64:exported.base64});
        const authored = JSON.parse(scene.sourceText);
        const imported = JSON.parse(roundTrip.sourceText);
        assert.equal(imported.source.note,'GLB SHA-256 '+exported.sha256+'. '+authored.source.note);
        const restoredSource={...authored.source,note:imported.source.note};
        let glbVertexDeltaM;
        if (recoverYaw) glbVertexDeltaM=checkRotatedRoundTrip(authored,imported,restoredSource);
        else assert.deepEqual(imported,{...authored,source:restoredSource});
        assert(!roundTrip.imageAssembly);

        await restart();
        await page.locator('#spatial-scenes').selectOption(id);
        await status('floor contact verified twice');
        if (registered) assert.equal(await page.locator('#image-assembly select[name=mode]').inputValue(),recoverYaw?'yaw':'views');
        assert.deepEqual(await call('read',{id}),scene);
        await page.locator('#image-assembly-replay').click();
        await status('Fresh floor contacts matched for all 2 props.');
        assert.equal((await call('export-glb',{id})).sha256,exported.sha256);
        if (registered) await status(recoverYaw?'Positions and rotations rechecked.':'Both registered views rechecked.');
        await page.locator('#image-assembly-status').evaluate(element=>element.scrollIntoView({block:'nearest'}));
        await capture('image-scene-restarted.png');

        const originalLayout = await fs.readFile(layout);
        const retainedLayout = path.join(project,'spatial','image-assemblies',id+'.png');
        assert.equal(hash(originalLayout),receipt.binding.layoutSha256);
        assert((await fs.readFile(retainedLayout)).equals(originalLayout));
        await fs.writeFile(retainedLayout,Buffer.concat([originalLayout,Buffer.from('changed')]));
        await assert.rejects(call('export-glb',{id}),/recheck the layout PNG/);
        await fs.writeFile(retainedLayout,originalLayout);
        if (registered) {
            const originalWitness = await fs.readFile(witness);
            const retainedWitness = path.join(project,'spatial','image-assemblies',id+'.witness.png');
            assert((await fs.readFile(retainedWitness)).equals(originalWitness));
            await fs.writeFile(retainedWitness,await fs.readFile(path.join(registration,'shifted-second-angle.png')));
            await assert.rejects(call('image-assembly-replay',{id}),/registered views/);
            await assert.rejects(call('export-glb',{id}),/registered views/);
            await fs.writeFile(retainedWitness,originalWitness);
        }
        const pkg = assetPixels(second.id);
        const images = await fs.readFile(pkg);
        await fs.writeFile(pkg,Buffer.concat([images,Buffer.from('changed')]));
        await assert.rejects(call('image-assembly-replay',{id}),/recheck the layout PNG/);
        await fs.writeFile(pkg,images);
        const replay = await call('image-assembly-replay',{id});
        assert.deepEqual(replay.measured,receipt.measured);
        assert.deepEqual(await call('read',{id:first.id}),first);
        assert.deepEqual(await call('read',{id:second.id}),second);

        if (recoverYaw) {
            const pane=page.locator('#spatial-browser-views');
            if (!await pane.evaluate(el=>el.open)) await pane.locator('summary').click();
            const rectangular=await fit('browser-views',{front:'front.png',side:'side.png',heldOut:'held-out.png'},0);
            const perforated=await fit('browser-top',elevated,60);
            const layoutPng=(await fs.readFile(path.join(registrationFixtures,'overhead.png'))).toString('base64');
            const witnessPng=(await fs.readFile(path.join(registrationFixtures,'second-angle.png'))).toString('base64');
            const scenes=(await call('list')).scenes.length;
            await assert.rejects(call('image-assembly',{title,widthM:4,layoutPng,witnessPng,recoverYaw:true,
                props:[{sceneId:rectangular.id,label:'Symmetric bracket'},{sceneId:perforated.id,label:'Perforated plate'}]}),
            /Overhead orientation is ambiguous for Symmetric bracket/);
            assert.equal((await call('list')).scenes.length,scenes);
        }

        const proof = {platform:process.platform,layoutSha256:receipt.binding.layoutSha256,
            sceneSha256:scene.sourceSha256,glbSha256:exported.sha256,props:receipt.binding.props,
            measurements:receipt.measured,localPngImports:recoverYaw?8:7,didRestart:true,
            ...(recoverYaw?{glbGeometryRoundTrip:true,glbVertexDeltaM}:{exactGlbRoundTrip:true}),
            sourceScenesPreserved:true,refusedChangedLayout:true,refusedChangedViewSet:true,
            ...(registered?{registration:placement,truth,labels,positionErrorsM,
                ...(recoverYaw?{yawErrorsDeg,yawOnlyRefusal:refusal,refusedShiftOnly:true,refusedSymmetricHeading:true}
                    :{shiftedViewRefusal:refusal}),
                detectedInReverseInputOrder:true,
                refusedReusedView:true,refusedAmbiguousIdentity:true,refusedChangedWitness:true}:{refusedOverlap:true})};
        await fs.writeFile(path.join(out,'image-assembly-evidence.json'),JSON.stringify(proof,null,2)+'\n');
        await fs.writeFile(path.join(out,'image-workshop.json'),scene.sourceText);
        await fs.writeFile(path.join(out,'image-workshop.glb'),bytes);
        console.log(JSON.stringify(proof));
    } catch (error) {
        await capture('failed.png').catch(()=>{});
        console.error('Temporary test project',project);
        throw error;
    } finally {
        await app.close();
    }
}

if (require.main===module) main().catch(error=>{console.error(error);process.exitCode=1;});
module.exports={fitRegisteredViews,checkRotatedRoundTrip};
