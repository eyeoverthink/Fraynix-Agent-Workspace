'use strict';

const assert = require('node:assert/strict');
const {createHash} = require('node:crypto');
const {execFile} = require('node:child_process');
const fs = require('node:fs/promises');
const path = require('node:path');
const {promisify} = require('node:util');

const sha = bytes => createHash('sha256').update(bytes).digest('hex');
const box = (sourceId,label,position,size,massKg) => ({
    sourceId,label,shape:'box',position,size,rotation:[0,0,0,1],massKg,friction:.57,restitution:.04
});
const destination = {
    format:'fraynix.spatial-scene',version:1,title:'Harbour Relay Workshop',units:'m',frame:'right-handed-y-up',
    source:{kind:'synthetic',physicalProperties:'declared',note:'Authored level for browser placement validation.'},
    scaleReference:{a:[0,0,0],b:[1,0,0],knownLengthM:1,toleranceM:0,basis:'declared-test'},
    gravityMps2:9.81,
    objects:[
        box('floor','Workshop floor',[0,-.1,0],[7,.2,4.8],0),
        box('rail','Rail by the entrance',[-2.15,.43,.2],[.25,.86,.45],0),
        box('crate','Original relay crate',[2.5,.22,-1.8],[.48,.44,.62],1.8)
    ]
};
const placement = {x:.15,z:.7,yaw:31,startX:-2.15,startZ:-1.1,endX:2.2,endZ:-1.1};

async function verifyPlacement(options) {
    const {app,page,pick,status,capture,api,project,output,extra,launch,stopSource} = options;
    const original = await api('import',{text:JSON.stringify(destination)});
    assert(await page.locator('#browser-views-place').isDisabled());
    await pick('front','front','front');
    await pick('side','side','side');
    await pick('heldOut','wrong','held-out 45°');
    await page.locator('#browser-views-place').click();
    await status('Held-out view refused');
    assert.equal((await api('list')).scenes.length,1);
    assert.deepEqual(await api('browser-placement-latest'),{});
    assert.deepEqual(await api('placement-latest'),{});
    assert.equal(await page.locator('#placement-dialog[open]').count(),0);

    await pick('heldOut','held-out','held-out 45°');
    await page.locator('#browser-views-place').click();
    await page.locator('#placement-level-fields').waitFor();
    const saved = await api('browser-placement-latest');
    const source = await api('read',{id:saved.source.sceneId});
    const proof = source.reconstruction;
    assert(proof.heldOutIou>.90);
    assert.equal(await page.locator('#spatial-objects').inputValue(),saved.objectId);
    assert.equal(saved.meshSha256,proof.meshSha256);
    assert.match(await page.locator('#placement-source-choice').textContent(),/3 browser views/);
    assert.deepEqual(await api('placement-latest'),{});
    assert.deepEqual(await api('read',{id:original.id}),original);
    const pkg = path.join(project,'spatial','reconstruction-sources',source.id+'.json');
    const bytes = await fs.readFile(pkg);
    assert.equal(saved.reference.sourceSha256,sha(bytes));
    const captured = JSON.parse(bytes).views;
    for (const [name,view] of Object.entries(saved.reference.crops)) {
        assert.equal(view.pngSha256,sha(Buffer.from(captured[name].png,'base64')));
        assert.deepEqual(Object.keys(view).sort(),['pixels','pngSha256','tag']);
    }
    assert.equal(Object.keys(saved.reference.crops).length,3);
    await page.locator('#placement-close').click();
    await capture('browser-captures.png');

    await stopSource();
    await app.close();
    let next;
    const call = (route,input = {})=>next.app.evaluate((_,request)=>
        global.workspaceTest.api('/api/spatial/'+request.route,request.input),{route,input});
    const placementStatus = text => next.page.waitForFunction(phrase =>
        document.querySelector('#placement-status')?.textContent.includes(phrase),text,{timeout:20000});
    const screenshot = async filename => {
        await next.page.evaluate(() => new Promise(resolve=>requestAnimationFrame(()=>requestAnimationFrame(resolve))));
        await fs.writeFile(path.join(output,filename),Buffer.from(await next.app.evaluate(()=>global.workspaceTest.capture()),'base64'));
    };
    const restart = async () => {
        await next?.app.close();
        next = await launch(project,extra);
        await next.page.locator('#spatial-tab').click();
    };
    try {
        await restart();
        assert.deepEqual(await call('browser-placement-latest'),saved);
        const journal = path.join(project,'spatial','placement-reviews','browser-source.json');
        const retained = await fs.readFile(journal);
        const changed = JSON.parse(retained);
        changed.binding.objectId = original.objects[2].id;
        await fs.writeFile(journal,JSON.stringify(changed));
        await assert.rejects(call('browser-placement-latest'),/saved browser placement source changed/i);
        await fs.writeFile(journal,retained);

        await next.page.locator('#spatial-scenes').selectOption(original.id);
        const panel = next.page.locator('#spatial-browser-views');
        if (!await panel.evaluate(el=>el.open)) await panel.locator('summary').click();
        await next.page.locator('#browser-views-resume').click();
        await next.page.locator('#placement-level-fields').waitFor();
        assert.equal(await next.page.locator('#spatial-scenes').inputValue(),source.id);
        await next.page.locator('#placement-destination').selectOption(original.id);
        await next.page.locator('#placement-floor').selectOption(original.objects[0].id);
        for (const [key,value] of Object.entries(placement))
            await next.page.locator('#placement-level-fields input[name='+key+']').fill(String(value));
        await next.page.locator('#placement-level-prepare').click();
        await placementStatus('Review saved');
        await screenshot('browser-placement-review.png');

        const pending = await call('placement-latest');
        assert.equal(pending.status,'review');
        assert.equal(pending.review.source.sceneId,source.id);
        assert.equal(pending.review.objectId,saved.objectId);
        assert.equal(pending.review.meshSha256,saved.meshSha256);
        assert.equal(pending.review.destination.sceneId,original.id);
        assert.deepEqual(pending.review.browserReference,saved.reference);
        const args = {reviewId:pending.reviewId};
        await assert.rejects(call('placement-run',args),/approve this placement/i);
        assert.equal((await call('list')).scenes.length,2);

        await restart();
        await next.page.locator('#placement-resume').click();
        await placementStatus('Review saved');
        assert.deepEqual(await call('placement-latest'),pending);
        await next.page.locator('#placement-approve').click();
        await placementStatus('Placement and native contact evidence verified');
        await screenshot('browser-placement-verified.png');
        const installed = await call('placement-latest');
        assert.equal(installed.reviewId,pending.reviewId);
        assert.deepEqual(installed.evidence.measured,installed.evidence.repeat);
        assert(installed.evidence.measured.hit.firstContactTick>0);
        assert.equal(installed.evidence.measured.miss.contactTicks,0);
        assert.equal(installed.evidence.measured.playerEnvelopeClear,true);
        assert.equal((await call('placement-run',args)).sceneId,installed.sceneId);
        assert.equal((await call('list')).scenes.length,3);
        assert.deepEqual(await call('read',{id:original.id}),original);
        const landed = await call('read',{id:installed.sceneId});
        assert.deepEqual(JSON.parse(landed.sourceText).objects.slice(0,-1),destination.objects);
        assert.equal(landed.objects.at(-1).visualMesh,saved.meshSha256);
        const preview = await call('mesh-preview',{id:landed.id,objectId:landed.objects.at(-1).id,
            mode:'vertex',index:0,deltaM:[0,.001,0]});
        assert.equal(preview.evidence.sourceMeshSha256,saved.meshSha256);
        assert.notEqual(preview.evidence.meshSha256,saved.meshSha256);
        assert.deepEqual(await call('read',{id:landed.id}),landed);
        await next.page.locator('#placement-glb').click();
        await placementStatus('Checked GLB saved for Blender');
        const savedFile = (await next.page.locator('#placement-file').textContent()).slice('Saved '.length);
        const glb = await fs.readFile(savedFile);
        const sidecar = JSON.parse(await fs.readFile(savedFile+'.receipt.json','utf8'));
        const exported = await call('placement-export',{...args,kind:'evidence'});
        const bundle = JSON.parse(Buffer.from(exported.base64,'base64'));
        assert.equal(sha(glb),sidecar.sha256);
        assert.equal(sha(glb),bundle.glbSha256);
        assert.deepEqual(bundle.evidence.review.browserReference,saved.reference);
        assert.equal(bundle.evidenceSha256,installed.evidenceSha256);
        assert.equal(sidecar.source.evidenceSha256,installed.evidenceSha256);

        await restart();
        assert.equal((await call('placement-export',{...args,kind:'glb'})).sha256,sidecar.sha256);
        await call('placement-replay',args);
        await fs.appendFile(pkg,' ');
        await assert.rejects(call('browser-placement-latest'),/original visual reconstruction/i);
        await assert.rejects(call('placement-export',{...args,kind:'glb'}),/recheck the approved level placement/i);
        assert.equal((await call('list')).scenes.length,3);
        await fs.writeFile(pkg,bytes);
        await fs.writeFile(path.join(output,'browser-level.glb'),glb);
        await fs.writeFile(path.join(output,'browser-level.fraynix-level.json'),Buffer.from(exported.base64,'base64'));
        await fs.writeFile(path.join(output,'expected-level.json'),JSON.stringify({destination,placement},null,2)+'\n');

        let blender = null;
        if (process.env.FRAYNIX_TEST_BLENDER) {
            await promisify(execFile)(process.env.FRAYNIX_TEST_BLENDER,['--background','--factory-startup','--disable-autoexec',
                '--python-exit-code','17','--python',path.join(__dirname,'browser-placement-blender.py'),'--',output],
            {timeout:120000,windowsHide:true});
            blender = JSON.parse(await fs.readFile(path.join(output,'blender-level-proof.json'),'utf8'));
            assert.equal(blender.glbSha256,sidecar.sha256);
        }
        const measures = {
            platform:process.platform,heldOutIou:proof.heldOutIou,triangles:proof.triangles,estimatedDepthM:proof.estimatedDepthM,
            browserReference:saved.reference,meshSha256:saved.meshSha256,sceneId:installed.sceneId,
            hitTick:installed.evidence.measured.hit.firstContactTick,contacts:installed.evidence.measured.hit.contactTicks,
            missContacts:installed.evidence.measured.miss.contactTicks,aisleM:installed.evidence.measured.routeLengthM,
            wrongWitnessRefused:true,modifiedHandoffRefused:true,changedOriginalRefused:true,
            resumedWithSourceOffline:true,reviewRestart:true,unapprovedRefused:true,duplicateCommitPrevented:true,
            exportRebuiltAfterRestart:true,replayedAfterRestart:true,originalDestinationPreserved:true,
            componentEditPreviewVerified:true,
            exportSha256:sidecar.sha256,blender
        };
        await fs.writeFile(path.join(output,'browser-placement-proof.json'),JSON.stringify(measures,null,2)+'\n');
        console.log(JSON.stringify(measures));
    } finally {
        await next?.app.close().catch(()=>{});
    }
}

module.exports = {verifyPlacement};
