
'use strict';

const assert = require('node:assert/strict');
const {createHash} = require('node:crypto');
const fs = require('node:fs/promises');
const os = require('node:os');
const path = require('node:path');
const {launch} = require('./calibration-integration.cjs');

const sha = bytes => createHash('sha256').update(bytes).digest('hex');
function checkedGeometry(imported, original) {
    const placed = original.objects[1];
    const reopened = imported.objects[1];
    for (const key of ['position','size','rotation']) {
        for (const [i,value] of reopened[key].entries()) assert(Math.abs(value-placed[key][i]) < 1e-5);
    }
    const before = Buffer.from(original.visualMeshes[placed.visualMesh],'base64');
    const after = Buffer.from(imported.visualMeshes[reopened.visualMesh],'base64');
    const end = 8+before.readUInt32LE()*12;
    assert(before.subarray(0,8).equals(after.subarray(0,8)));
    assert(before.subarray(end).equals(after.subarray(end)));
    for (let i=8;i<end;i+=4) assert(Math.abs(before.readFloatLE(i)-after.readFloatLE(i)) < 1e-5);
}
const command = 'Place the verified prop at X 0.9 m, Z -0.25 m, yaw 20 degrees, test the collision, and export the scene.';

async function main() {
    process.env.FRAYNIX_WORKSPACE_NO_MODELS = '1';
    const parent = process.env.FRAYNIX_PLACEMENT_TEST_PROJECT_PARENT || os.tmpdir();
    await fs.mkdir(parent, {recursive:true});
    const project = await fs.mkdtemp(path.join(parent,'Fraynix reviewed prop placement '));
    const out = path.resolve(process.argv[2] || 'build/visual-placement232');
    const fixtures = path.join(__dirname,'fixtures','library-prop');
    await fs.mkdir(out,{recursive:true});
    const args = process.platform === 'linux' ? ['--no-sandbox','--disable-dev-shm-usage'] : [];
    let {app,page} = await launch(project,args);
    const api = (route,input = {}) => app.evaluate((_,request) =>
        global.workspaceTest.api('/api/'+request.route,request.input),{route,input});
    const status = text => page.waitForFunction(text =>
        document.querySelector('#placement-status')?.textContent.includes(text),text);
    const capture = async name => {
        await page.evaluate(() => new Promise(done=>requestAnimationFrame(()=>requestAnimationFrame(done))));
        await fs.writeFile(path.join(out,name),Buffer.from(await app.evaluate(()=>global.workspaceTest.capture()),'base64'));
    };
    async function say(text) {
        await page.locator('#goal').fill(text);
        await page.locator('#run').click();
        await page.locator('#placement-dialog[open]').waitFor();
    }
    async function reopen() {
        await page.locator('#spatial-tab').click();
        await page.locator('#placement-resume').click();
        await page.locator('#placement-dialog[open]').waitFor();
    }
    async function exportOnDisk() {
        await status('Scene, GLB and contact evidence saved together');
        const file = (await page.locator('#placement-file').textContent()).slice('Saved '.length);
        const bytes = await fs.readFile(file);
        const sidecar = JSON.parse(await fs.readFile(file+'.receipt.json','utf8'));
        assert.equal(sha(bytes),sidecar.sha256);
        const pkg = JSON.parse(bytes);
        const glb = Buffer.from(pkg.glbBase64,'base64');
        assert.equal(sha(glb),pkg.glbSha256);
        assert.equal(pkg.glbSha256,sidecar.source.glbSha256);
        assert.equal(pkg.evidenceSha256,sidecar.source.evidenceSha256);
        assert.equal(pkg.sceneId,sidecar.source.sceneId);
        return {bytes,sidecar,pkg,glb};
    }

    try {
        await say('Place the verified prop.');
        await status('Select a verified reconstructed prop');
        assert.equal((await api('spatial/list')).scenes.length,0);
        await page.locator('#placement-close').click();

        const ids = {};
        for (const [role,name] of Object.entries({front:'front',side:'side',top:'top',heldOut:'elevated'})) {
            const filename = name+'.png';
            const png = (await fs.readFile(path.join(fixtures,filename))).toString('base64');
            ids[role] = (await api('media-tiles/add',{source:{filename,png}})).id;
        }
        const source = await api('spatial/library-views',{
            name:'Twin-slot deck plate',widthM:.5,heldOutElevationDeg:60,assets:ids
        });
        await page.locator('#spatial-tab').click();
        await page.waitForFunction(id=>document.querySelector('#spatial-scenes')?.value===id,source.id);
        await page.getByText('Reconstructed prop · dynamic',{exact:true}).waitFor({state:'attached'});
        await page.locator('#spatial-objects').selectOption(source.objects[1].id);
        await say('Place the verified prop beyond the edge and export the scene.');
        await status('Give a distance and direction');
        await page.locator('#placement-close').click();
        await say(command);
        await status('Review saved');
        const pending = await api('spatial/placement-latest');
        assert.equal(pending.status,'review');
        assert.deepEqual(pending.review.targetM,[.9,.05,-.25]);
        assert.equal(pending.review.yawDeg,20);
        const data = {reviewId:pending.reviewId};
        await assert.rejects(api('spatial/placement-run',data),/approve this placement/i);
        await assert.rejects(api('spatial/placement-export',data),/verify the collision/i);
        await assert.rejects(api('spatial/placement-approve',{...data,approvalSha256:'0'.repeat(64)}),/exact saved placement/i);
        assert.equal((await api('spatial/list')).scenes.length,1);
        await capture('placement-review.png');

        await app.close();
        ({app,page} = await launch(project,args));
        await reopen();
        await status('Review saved');
        assert.deepEqual(await api('spatial/placement-latest'),pending);
        await page.locator('#placement-approve').click();
        const first = await exportOnDisk();
        await capture('placement-verified.png');
        const placed = await api('spatial/placement-latest');
        assert.equal(placed.status,'verified');
        assert.equal(placed.reviewId,pending.reviewId);
        assert.equal(placed.sceneId,first.pkg.sceneId);
        assert.equal(placed.evidenceSha256,first.pkg.evidenceSha256);
        assert.deepEqual(first.pkg.evidence,placed.evidence);
        assert.deepEqual(first.pkg.sourceReconstruction,source.reconstruction);
        const {measured,repeat} = placed.evidence;
        assert.deepEqual(repeat,measured);
        assert(measured.hit.firstContactTick>0 && measured.hit.firstContactTick<=180);
        assert(measured.hit.contactTicks>0 && measured.hit.maxPropPlanarSpeedMps>.02);
        assert(measured.hit.firstContactSeparationM<=0);
        assert.equal(measured.miss.firstContactTick,0);
        assert.equal(measured.miss.contactTicks,0);
        assert(measured.miss.maxPropPlanarSpeedMps<.01);
        assert(measured.hit.samples.some(sample=>sample.tick===measured.hit.firstContactTick));
        const landed = await api('spatial/read',{id:placed.sceneId});
        assert.deepEqual(landed.objects[1].position,pending.review.targetM);
        assert.equal(landed.objects[1].visualMesh,source.objects[1].visualMesh);
        assert.deepEqual(await api('spatial/read',{id:source.id}),source);
        assert.equal((await api('spatial/list')).scenes.length,2);

        await page.locator('#placement-open-scene').click();
        await page.waitForFunction(id=>document.querySelector('#spatial-scenes')?.value===id,placed.sceneId);
        await page.locator('#spatial-stage').evaluate(el=>el.scrollIntoView({block:'start'}));
        await capture('placement-scene.png');
        await app.close();

        ({app,page} = await launch(project,args));
        await reopen();
        await status('Placement and native contact evidence verified');
        await page.locator('#placement-replay').click();
        await status('Fresh native replay matched');
        await page.locator('#placement-export').click();
        const afterRestart = await exportOnDisk();
        assert(first.bytes.equals(afterRestart.bytes));
        assert.equal((await api('spatial/list')).scenes.length,2);
        const repeatApproval = await api('spatial/placement-run',data);
        assert.equal(repeatApproval.sceneId,placed.sceneId);
        assert.equal((await api('spatial/list')).scenes.length,2);

        const recalled = await api('spatial/import-glb',{base64:first.glb.toString('base64')});
        checkedGeometry(recalled,landed);
        await page.locator('#placement-close').click();

        const reviewed = await api('spatial/placement-review',{sceneId:source.id,objectId:source.objects[1].id,
            goal:'Place the verified prop 0.5 m to the left and test the collision, then export the scene.'});
        assert.deepEqual(reviewed.review.targetM,[-.5,.05,0]);
        await api('spatial/placement-approve',{reviewId:reviewed.reviewId,approvalSha256:reviewed.reviewId});
        assert.equal((await api('spatial/placement-latest')).status,'approved');
        await app.close();
        ({app,page} = await launch(project,args));
        await reopen();
        await status('Approval saved');
        await page.getByRole('button',{name:'Run approved contact test',exact:true}).click();
        const resumed = await exportOnDisk();
        assert.equal(resumed.pkg.evidence.reviewId,reviewed.reviewId);
        assert.notEqual(resumed.pkg.sceneId,placed.sceneId);
        const unverified = await api('spatial/import',{text:source.sourceText});
        await assert.rejects(api('spatial/placement-review',{
            sceneId:unverified.id,objectId:unverified.objects[1].id,goal:command
        }), /checked held-out view/i);

        const asset = path.join(project,'media','tiles','sprites',ids.heldOut+'.json');
        await fs.appendFile(asset,' ');
        await assert.rejects(api('spatial/placement-export',data),/original visual reconstruction/i);
        await assert.rejects(api('spatial/placement-replay',data),/original visual reconstruction/i);

        await fs.writeFile(path.join(out,'verified-placement.fraynix-placement.json'),first.bytes);
        await fs.writeFile(path.join(out,'verified-placement.glb'),first.glb);
        const proof = {format:'fraynix.reviewed-prop-acceptance-v1',platform:process.platform,
            sourceId:source.id,sceneId:placed.sceneId,reviewId:placed.reviewId,command,
            hitTick:measured.hit.firstContactTick,contactTicks:measured.hit.contactTicks,
            firstContactSeparationM:measured.hit.firstContactSeparationM,
            propSpeedMps:measured.hit.maxPropPlanarSpeedMps,missContacts:measured.miss.contactTicks,
            originalViewOverlap:source.reconstruction.heldOutIou,targetM:pending.review.targetM,
            yawDeg:pending.review.yawDeg,exportSha256:first.sidecar.sha256,glbSha256:first.pkg.glbSha256,
            evidenceSha256:first.pkg.evidenceSha256,reviewRestart:true,approvedResume:true,
            verifiedReplayAfterRestart:true,identicalExportAfterRestart:true,unapprovedRefused:true,
            alteredSourceRefused:true,unverifiedSourceRefused:true,importedGlb:true};
        await fs.writeFile(path.join(out,'placement-proof.json'),JSON.stringify(proof,null,2)+'\n');
        console.log(JSON.stringify(proof));
    } catch (error) {
        console.error('Placement status:',await page.locator('#placement-status').textContent({timeout:1200}).catch(()=>''));
        throw error;
    } finally {
        await app?.close().catch(()=>{});
    }
}
main().catch(error=>{console.error(error);process.exitCode=1;});
