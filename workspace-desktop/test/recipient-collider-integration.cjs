'use strict';

const assert = require('node:assert/strict');
const {execFileSync} = require('node:child_process');
const {createHash} = require('node:crypto');
const fs = require('node:fs/promises');
const os = require('node:os');
const path = require('node:path');
const {launch} = require('./calibration-integration.cjs');

const sha = bytes => createHash('sha256').update(bytes).digest('hex');

async function main() {
    process.env.FRAYNIX_WORKSPACE_NO_MODELS = '1';
    const folder = path.resolve(process.argv[2] || 'build/recipient-collider241');
    const fixture = path.resolve(process.argv[3] || 'docs/readmes/240/signal-bay-handoff.fraynix-recipient.json');
    const previousBytes = await fs.readFile(fixture);
    const previous = JSON.parse(previousBytes);
    const archiveId = sha(previousBytes);
    const project = await fs.mkdtemp(path.join(os.tmpdir(),'Fraynix collider recipient '));
    const args = process.platform==='linux' ? ['--no-sandbox','--disable-dev-shm-usage'] : [];
    await fs.mkdir(folder,{recursive:true});
    let {app,page} = await launch(project,args);
    const call = (route,input={}) => app.evaluate((_,request) =>
        global.workspaceTest.api('/api/spatial/'+request.route,request.input),{route,input});
    const check = (id,value) => page.waitForFunction(({id,value}) =>
        document.getElementById(id)?.textContent.includes(value),{id,value});
    const capture = async name => {
        await page.evaluate(()=>new Promise(resolve=>requestAnimationFrame(()=>requestAnimationFrame(resolve))));
        await fs.writeFile(path.join(folder,name),Buffer.from(await app.evaluate(()=>global.workspaceTest.capture()),'base64'));
    };
    const restart = async () => {
        await app.close();
        ({app,page} = await launch(project,args));
        await page.locator('#spatial-tab').click();
    };
    const findProp = (scene,key) => scene.objects.find(body=>body.sourceId===key);
    try {
        await page.locator('#spatial-tab').click();
        await page.locator('#placement-exchange').click();
        await page.waitForFunction(()=>!document.querySelector('#exchange-file').disabled);
        await page.locator('#exchange-file').setInputFiles(fixture);
        await check('exchange-status','Bundle and GLB checked');
        assert(await page.locator('#exchange-fork').isHidden());
        assert.equal((await call('list')).scenes.length,0);
        await page.locator('#exchange-replay').click();
        await check('exchange-status','Local physics replay matched');
        const baseline = await call('placement-exchange-latest');
        await page.locator('#exchange-fork').click();
        await page.locator('#recipient-edit-dialog[open]').waitFor();
        const root = await call('read',{id:await page.locator('#spatial-scenes').inputValue()});
        const key = root.recipientBranch.propSourceId;
        assert.equal(root.recipientBranch.archiveSha256,archiveId);
        const placed = findProp(root,key);
        const plan = (colliderWidthM,colliderDepthM) =>
            call('recipient-edit-review',{sceneId:root.id,colliderWidthM,colliderDepthM});
        const [oldW,oldH,oldD] = placed.size;
        const width = Number((oldW+.06).toFixed(6));
        const depth = Number((oldD+.04).toFixed(6));
        await assert.rejects(plan(oldW,oldD),/Change the collider width or depth/i);
        await assert.rejects(plan(oldW+.101,oldD),/at most 0.10 m per edit/i);
        await assert.rejects(call('recipient-edit-review',{sceneId:root.id,colliderWidthM:width,colliderDepthM:depth,yawDeg:8}),
            /unsupported spatial field/i);

        await page.locator('#recipient-edit-form select[name=kind]').selectOption('collider');
        assert(await page.locator('#recipient-pose-fields').isHidden());
        const widthInput = page.locator('#recipient-edit-form input[name=colliderWidthM]');
        const depthInput = page.locator('#recipient-edit-form input[name=colliderDepthM]');
        const preview = page.getByRole('button',{name:'Preview local edit',exact:true});
        await widthInput.fill(String(oldW-.04));
        await preview.click();
        await check('recipient-edit-status','must enclose the retained visual mesh');
        assert(await page.locator('#recipient-edit-approve').isHidden());
        assert.deepEqual(await call('recipient-edit-latest',{sceneId:root.id}),{});
        await capture('collider-cut-refused.png');
        await widthInput.fill(String(width));
        await depthInput.fill(String(depth));
        await preview.click();
        await check('recipient-edit-status','Approve the exact collider');
        await page.locator('#recipient-collider-figure').waitFor();
        const pending = await call('recipient-edit-latest',{sceneId:root.id});
        const id = pending.reviewId;
        const spec = pending.review.collider;
        assert.equal(spec.meshSha256,placed.visualMesh);
        assert.deepEqual(spec.previousM,placed.size);
        assert.deepEqual(spec.proposedM,[width,oldH,depth]);
        assert.deepEqual(pending.review.targetM,placed.position);
        assert.deepEqual(pending.review.rotation,placed.rotation);
        await assert.rejects(call('recipient-edit-run',{reviewId:id}),/Approve this exact local edit/i);
        await assert.rejects(call('recipient-edit-approve',{reviewId:id,approvalSha256:previous.revisions.at(-1).approvalSha256}),
            /Approve the exact saved local edit/i);
        await capture('mesh-and-collider-preview.png');

        await restart();
        await page.locator('#recipient-edit').click();
        await check('recipient-edit-status','Approve the exact collider');
        assert.equal(await page.locator('#recipient-edit-form select[name=kind]').inputValue(),'collider');
        assert.equal(Number(await page.locator('#recipient-edit-form input[name=colliderWidthM]').inputValue()),width);
        assert.equal(Number(await page.locator('#recipient-edit-form input[name=colliderDepthM]').inputValue()),depth);
        assert.deepEqual(await call('recipient-edit-latest',{sceneId:root.id}),pending);
        await call('recipient-edit-approve',{reviewId:id,approvalSha256:id});
        await restart();
        await page.locator('#recipient-edit').click();
        await check('recipient-edit-status','Approval saved');
        await page.getByRole('button',{name:'Run approved edit checks',exact:true}).click();
        await check('recipient-edit-status','Local revision verified');
        const applied = await call('recipient-edit-run',{reviewId:id});
        const revision = await call('read',{id:applied.sceneId});
        const oldScene = JSON.parse(root.sourceText);
        const newScene = JSON.parse(revision.sourceText);
        findProp(oldScene,key).size = spec.proposedM;
        assert.deepEqual(newScene,oldScene);
        const result = revision.recipientEdit;
        assert.deepEqual(result.measured,result.repeat);
        assert.equal(result.measured.miss.contactTicks,0);
        assert(result.measured.playerEnvelopeClear);
        assert.deepEqual(await call('recipient-evidence',{sceneId:revision.id}),baseline);
        assert.equal((await call('list')).scenes.length,2);

        await page.locator('#recipient-edit-export').click();
        await check('recipient-edit-export-path','Saved ');
        const nativePath = await page.locator('#recipient-edit-export-path').getAttribute('title');
        const bytes = await fs.readFile(nativePath);
        const archive = JSON.parse(bytes);
        const glb = Buffer.from(archive.glbBase64,'base64');
        assert.equal(sha(glb),archive.glbSha256);
        assert(Buffer.from(archive.parentArchiveBase64,'base64').equals(previousBytes));
        assert.deepEqual(archive.baseReplay,baseline.replay);
        assert.deepEqual(archive.revisions.map(entry=>entry.approvalSha256),[id]);
        assert.deepEqual(archive.revisions[0].receipt,result);
        const swapped = {...archive,glbBase64:previous.glbBase64,glbSha256:previous.glbSha256};
        await assert.rejects(call('placement-exchange-open',{base64:Buffer.from(JSON.stringify(swapped)).toString('base64')}),
            /changed an oriented box/i);
        assert.equal((await call('placement-exchange-latest')).id,archiveId);
        await capture('collider-revision-exported.png');
        await fs.writeFile(path.join(folder,'signal-bay-collider.fraynix-recipient.json'),bytes);
        await fs.writeFile(path.join(folder,'incoming-final.glb'),Buffer.from(previous.glbBase64,'base64'));
        await fs.writeFile(path.join(folder,'collider-final.glb'),glb);
        await fs.writeFile(path.join(folder,'blender-input.json'),JSON.stringify({sourceId:key,old:spec.previousM,
            changed:spec.proposedM,position:placed.position,yawDeg:pending.review.yawDeg,glbSha256:archive.glbSha256}));
        let blender = null;
        if (process.env.FRAYNIX_TEST_BLENDER) {
            execFileSync(process.env.FRAYNIX_TEST_BLENDER,['--background','--factory-startup','--disable-autoexec',
                '--python-exit-code','17','--python',path.join(__dirname,'recipient-collider-blender.py'),'--',folder],
            {stdio:'inherit',timeout:90000});
            blender = JSON.parse(await fs.readFile(path.join(folder,'blender-collider-proof.json'),'utf8'));
            assert.equal(blender.glbSha256,archive.glbSha256);
        }
        const {measured} = result;
        const proof = {platform:process.platform,parentSha256:archiveId,archiveSha256:sha(bytes),glbSha256:archive.glbSha256,
            revision:revision.id,approvalSha256:id,collider:spec,position:placed.position,yawDeg:pending.review.yawDeg,
            hitTick:measured.hit.firstContactTick,hitContacts:measured.hit.contactTicks,missContacts:measured.miss.contactTicks,
            aisleM:measured.routeLengthM,cuttingMeshRefused:true,oversizedChangeRefused:true,mixedEditRefused:true,
            needsFreshApproval:true,previewAndApprovalSurvivedRestart:true,sourceChangedOnlyColliderSize:true,
            priorGlbWithMatchingChecksumRefused:true,blender};
        await fs.writeFile(path.join(folder,'collider-evidence.json'),JSON.stringify(proof,null,2)+'\n');
        console.log(JSON.stringify(proof));
    } finally {
        await app?.close().catch(()=>{});
    }
}
main().catch(error=>{console.error(error);process.exitCode=1;});
