'use strict';

const assert = require('node:assert/strict');
const {createHash} = require('node:crypto');
const fs = require('node:fs/promises');
const path = require('node:path');
const os = require('node:os');
const {launch} = require('./calibration-integration.cjs');

const sha = bytes => createHash('sha256').update(bytes).digest('hex');
const request = 'Please compare the pending collider using the five probe approaches.';

async function main() {
    process.env.FRAYNIX_WORKSPACE_NO_MODELS = '1';
    const out = path.resolve(process.argv[2] || 'build/recipient-mission245');
    const fixture = path.resolve(process.argv[3] || 'build/raised-switchyard245/raised-switchyard.fraynix-level.json');
    const bytes = await fs.readFile(fixture);
    const archive = JSON.parse(bytes);
    assert.equal(archive.scene.title,'Raised Switchyard');
    assert.equal(archive.evidence.review.yawDeg,65);
    assert.equal(archive.evidence.measured.routeLengthM,4.9);
    const project = await fs.mkdtemp(path.join(os.tmpdir(),'Fraynix collider mission '));
    const args = process.platform==='linux' ? ['--no-sandbox','--disable-dev-shm-usage'] : [];
    await fs.mkdir(out,{recursive:true});
    let {app,page} = await launch(project,args);
    const call = (route,input={}) => app.evaluate((_,request) =>
        global.workspaceTest.api('/api/spatial/'+request.route,request.input),{route,input});
    const check = (id,value) => page.waitForFunction(({id,value}) =>
        document.getElementById(id)?.textContent.includes(value),{id,value});
    const capture = async name => {
        await page.evaluate(()=>new Promise(resolve=>requestAnimationFrame(()=>requestAnimationFrame(resolve))));
        await fs.writeFile(path.join(out,name),Buffer.from(await app.evaluate(()=>global.workspaceTest.capture()),'base64'));
    };
    const restart = async () => {
        await app.close();
        ({app,page} = await launch(project,args));
        await page.locator('#spatial-tab').click();
    };
    const inspect = async expect => {
        await page.locator('#recipient-mission').click();
        await page.locator('#recipient-mission-dialog[open]').waitFor();
        await check('recipient-mission-export-status',expect);
        return call('recipient-mission-latest');
    };
    try {
        await page.locator('#spatial-tab').click();
        assert.deepEqual(await call('recipient-mission-latest'),{});
        await page.locator('#placement-exchange').click();
        await page.waitForFunction(()=>!document.querySelector('#exchange-file').disabled);
        await page.locator('#exchange-file').setInputFiles(fixture);
        await check('exchange-status','Bundle and GLB checked');
        assert(await page.locator('#exchange-fork').isHidden());
        await page.locator('#exchange-replay').click();
        await check('exchange-status','Local physics replay matched');
        await page.locator('#exchange-fork').click();
        await page.locator('#recipient-edit-dialog[open]').waitFor();
        const sceneId = await page.locator('#spatial-scenes').inputValue();
        const parent = await call('read',{id:sceneId});
        await page.locator('#recipient-edit-form select[name=kind]').selectOption('collider');
        await page.locator('#recipient-edit-form input[name=colliderWidthM]').fill('0.54');
        await page.locator('#recipient-edit-form input[name=colliderDepthM]').fill('0.435');
        await page.getByRole('button',{name:'Preview local edit',exact:true}).click();
        await check('recipient-edit-status','Approve the exact collider');
        await page.locator('#recipient-edit-close').click();
        await page.locator('#goal').fill(request);
        await page.locator('#run').click();
        await check('recipient-approach-status','Saved comparison for this exact preview');
        const draft = await call('recipient-mission-latest');
        const id = draft.reviewId;
        assert.equal(draft.request,request);
        assert.equal(draft.sceneTitle,'Raised Switchyard');
        assert.equal(draft.parentSceneId,sceneId);
        assert.equal(draft.comparison.binding.pair.reviewId,id);
        assert.deepEqual(draft.comparison.binding.pair.beforeColliderM,[.5,.1,.4]);
        assert.deepEqual(draft.comparison.binding.pair.afterColliderM,[.54,.1,.435]);
        assert(draft.comparison.summary.earlier>0 && draft.comparison.summary.later>0);
        assert(!draft.approval && !draft.revision && !draft.export);
        assert.deepEqual(await call('read',{id:sceneId}),parent);
        assert.equal((await call('list')).scenes.length,1);
        await assert.rejects(call('recipient-edit-run',{reviewId:id}),/Approve this exact local edit/i);
        await assert.rejects(call('recipient-mission-export',{reviewId:id}),/Approve the verified comparison/i);
        await page.locator('#recipient-edit-close').click();
        await inspect('Awaiting export');
        await page.locator('#recipient-mission-approaches summary').click();
        await capture('held-out-mission-pending.png');

        await restart();
        assert.deepEqual(await inspect('Awaiting export'),draft);
        await call('recipient-edit-approve',{reviewId:id,approvalSha256:id,comparisonSha256:draft.comparison.sha256});
        const approved = await call('recipient-mission-latest');
        assert.equal(approved.approvalConfirmed,true);
        assert.equal(approved.approval.comparisonSha256,draft.comparison.sha256);
        assert(!approved.revision && !approved.export);
        await assert.rejects(call('recipient-mission-export',{reviewId:id}),/Run the approved native checks/i);
        const cache=path.join(project,'spatial','recipient-edits',id+'.sweep.json');
        await fs.writeFile(cache,'{}');
        await restart();
        assert.deepEqual(await inspect('Awaiting export'),approved);
        await check('recipient-mission-approval','Exact proposal approved with its evidence');
        await capture('held-out-approval-after-restart.png');
        await page.getByRole('button',{name:'Continue review',exact:true}).click();
        await check('recipient-edit-status','Approval saved');
        await page.getByRole('button',{name:'Run approved edit checks',exact:true}).click();
        await check('recipient-edit-status','Local revision verified');
        await page.locator('#recipient-edit-close').click();
        const verified = await inspect('Awaiting export');
        assert(verified.revision.receipt.measured.playerEnvelopeClear);
        assert.equal(verified.revision.receipt.measured.miss.contactTicks,0);
        assert.deepEqual(verified.revision.receipt.measured,verified.revision.receipt.repeat);
        assert.equal((await call('list')).scenes.length,2);
        await page.getByRole('button',{name:'Save verified export',exact:true}).click();
        await check('recipient-mission-export-status','Verified · exports/');
        const exported = await call('recipient-mission-latest');
        const exportPath = path.join(project,'exports',exported.export.fileName);
        const packageBytes = await fs.readFile(exportPath);
        assert.equal(sha(packageBytes),exported.export.sha256);
        const bundle = JSON.parse(packageBytes);
        assert.equal(bundle.headSceneId,verified.revision.sceneId);
        assert(Buffer.from(bundle.originalArchiveBase64,'base64').equals(bytes));

        await restart();
        assert.deepEqual(await inspect('Verified · exports/'),exported);
        await page.locator('#recipient-mission-approaches summary').click();
        await capture('held-out-export-after-restart.png');
        await fs.writeFile(exportPath,Buffer.concat([packageBytes,Buffer.from(' ')]));
        await page.locator('#recipient-mission-close').click();
        const damaged = await inspect('Saved file changed');
        assert(!damaged.exportAvailable);
        assert.deepEqual(damaged.comparison,exported.comparison);
        await page.getByRole('button',{name:'Save verified export',exact:true}).click();
        await check('recipient-mission-export-status','Verified · exports/');
        assert((await fs.readFile(exportPath)).equals(packageBytes));
        assert.deepEqual(await call('recipient-mission-latest'),exported);

        const proof = {platform:process.platform,scene:exported.sceneTitle,inputSha256:sha(bytes),
            reviewId:id,request,comparisonSha256:draft.comparison.sha256,
            summary:draft.comparison.summary,rows:draft.comparison.cases.map(row=>row.reason ? {reason:row.reason}
                : {before:row.original.hit.firstContactTick,after:row.resized.hit.firstContactTick}),
            move:exported.review.collider.proposedM,launchYawDeg:exported.review.yawDeg,
            routeM:exported.revision.receipt.measured.routeLengthM,exportSha256:exported.export.sha256,
            restartedBeforeAndAfterApproval:true,exactExportRecoveredAfterChange:true,
            copiedEvidenceIndependentOfPreviewCache:true,noApprovalBeforeUserAction:true};
        await fs.writeFile(path.join(out,'collider-mission.json'),JSON.stringify(exported,null,2)+'\n');
        await fs.writeFile(path.join(out,'held-out-evidence.json'),JSON.stringify(proof,null,2)+'\n');
        await fs.writeFile(path.join(out,'raised-switchyard-reviewed.fraynix-recipient.json'),packageBytes);
        console.log(JSON.stringify(proof));
    } finally {
        await app?.close().catch(()=>{});
    }
}
main().catch(error=>{console.error(error);process.exitCode=1;});
