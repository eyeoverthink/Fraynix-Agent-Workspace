'use strict';

const assert = require('node:assert/strict');
const {createHash} = require('node:crypto');
const fs = require('node:fs/promises');
const os = require('node:os');
const path = require('node:path');
const {launch} = require('./calibration-integration.cjs');

const digest = bytes => createHash('sha256').update(bytes).digest('hex');
const base = path.join(__dirname,'fixtures');
const fixture = path.join(base,'authored-level-exchange','edited-signal-bay.fraynix-level.json');

async function main() {
    process.env.FRAYNIX_WORKSPACE_NO_MODELS = '1';
    const project = await fs.mkdtemp(path.join(os.tmpdir(),'Fraynix recipient edit '));
    const output = path.resolve(process.argv[2] || 'build/recipient-edit238');
    await fs.mkdir(output,{recursive:true});
    const bytes = await fs.readFile(fixture);
    const document = JSON.parse(bytes);
    const archiveId = digest(bytes);
    const args = process.platform==='linux' ? ['--no-sandbox','--disable-dev-shm-usage'] : [];
    let {app,page} = await launch(project,args);
    const call = (route,input={}) => app.evaluate((_,request) =>
        global.workspaceTest.api('/api/spatial/'+request.route,request.input),{route,input});
    const check = (id,message) => page.waitForFunction(({id,message}) =>
        document.getElementById(id)?.textContent.includes(message),{id,message},{timeout:20000});
    const capture = async name => {
        await page.evaluate(()=>new Promise(resolve=>requestAnimationFrame(()=>requestAnimationFrame(resolve))));
        await fs.writeFile(path.join(output,name),Buffer.from(await app.evaluate(()=>global.workspaceTest.capture()),'base64'));
    };
    const plan = (sceneId,xM,zM,yawDeg) => call('recipient-edit-review',{sceneId,xM,zM,yawDeg});
    const approve = reviewId => call('recipient-edit-approve',{reviewId,approvalSha256:reviewId});
    const apply = reviewId => call('recipient-edit-run',{reviewId});
    const inspect = async () => {
        await page.locator('#spatial-tab').click();
        await page.locator('#placement-exchange').click();
    };
    const restart = async () => {
        await app.close();
        ({app,page} = await launch(project,args));
        await page.locator('#spatial-tab').click();
    };

    try {
        await inspect();
        await page.waitForFunction(()=>!document.querySelector('#exchange-file')?.disabled);
        await page.locator('#exchange-file').setInputFiles(fixture);
        await check('exchange-status','Bundle and GLB checked');
        assert(await page.locator('#exchange-fork').isHidden());
        await assert.rejects(call('placement-exchange-fork',{id:archiveId,replaySha256:'0'.repeat(64)}),/Replay this exact bundle/i);
        assert.equal((await call('list')).scenes.length,0);
        await page.locator('#exchange-replay').click();
        await check('exchange-status','Local physics replay matched');
        await capture('recipient-copy-ready.png');
        const local = await call('placement-exchange-latest');
        await assert.rejects(call('placement-exchange-fork',{id:archiveId,replaySha256:'0'.repeat(64)}),/Replay this exact bundle/i);
        const fork = {id:archiveId,replaySha256:local.replay.sha256};

        await page.locator('#exchange-fork').click();
        await page.locator('#recipient-edit-dialog[open]').waitFor();
        const root = await call('read',{id:await page.locator('#spatial-scenes').inputValue()});
        assert.deepEqual(JSON.parse(root.sourceText),document.scene);
        assert.deepEqual(root.recipientBaseReplay,local.replay);
        assert.equal(root.recipientBranch.archiveSha256,archiveId);
        assert.equal(root.recipientBranch.glbSha256,document.glbSha256);
        assert.equal(root.recipientRevision,0);
        assert(await page.locator('#recipient-edit-export').isHidden());
        await assert.rejects(call('recipient-edit-export',{sceneId:root.id}),/Complete a verified local edit/i);
        assert.equal((await call('placement-exchange-fork',fork)).id,root.id);
        assert.equal((await call('list')).scenes.length,1);
        assert.deepEqual(await call('placement-latest'),{});
        const prop = root.objects.find(item=>item.sourceId===root.recipientBranch.propSourceId);
        assert.equal(await page.locator('#spatial-objects').inputValue(),prop.id);
        await assert.rejects(call('mesh-preview',{id:root.id,objectId:prop.id,mode:'fit'}),/reviewed local pose editor/i);
        await assert.rejects(call('animation-save',{id:root.id,sourceId:prop.sourceId,keys:[]}),/reviewed local pose editor/i);
        await assert.rejects(plan(root.id,.15,-1.1,31),/player aisle is blocked/i);
        assert.deepEqual(await call('recipient-edit-latest',{sceneId:root.id}),{});

        const intercepted = await plan(root.id,-1.48,.2,0);
        await approve(intercepted.reviewId);
        await assert.rejects(apply(intercepted.reviewId),/intercepted a contact trial/i);
        assert.equal((await call('list')).scenes.length,1);
        assert.deepEqual(await call('read',{id:root.id}),root);

        const stale = await plan(root.id,.30,.80,48);
        await approve(stale.reviewId);
        for (const [key,value] of Object.entries({xM:.48,zM:.82,yawDeg:53}))
            await page.locator('#recipient-edit-form input[name='+key+']').fill(String(value));
        await page.getByRole('button',{name:'Preview local edit',exact:true}).click();
        await check('recipient-edit-status','Preview saved');
        const pending = await call('recipient-edit-latest',{sceneId:root.id});
        assert.equal(pending.status,'review');
        assert.deepEqual(pending.review.branch,root.recipientBranch);
        await assert.rejects(apply(pending.reviewId),/Approve this exact local edit/i);
        await assert.rejects(call('recipient-edit-approve',{reviewId:pending.reviewId,approvalSha256:archiveId}),/exact saved local edit/i);
        await capture('recipient-edit-preview.png');

        await restart();
        await page.locator('#recipient-edit').click();
        await check('recipient-edit-status','Preview saved');
        assert.deepEqual(await call('recipient-edit-latest',{sceneId:root.id}),pending);
        await approve(pending.reviewId);
        await restart();
        await page.locator('#recipient-edit').click();
        await check('recipient-edit-status','Approval saved');
        assert.equal((await call('list')).scenes.length,1);
        await page.getByRole('button',{name:'Run approved edit checks',exact:true}).click();
        await check('recipient-edit-status','Local revision verified');
        await capture('recipient-edit-verified.png');
        const applied = await apply(pending.reviewId);
        const revision = await call('read',{id:applied.sceneId});
        assert.equal(revision.recipientRevision,1);
        assert.deepEqual(revision.recipientBranch,root.recipientBranch);
        assert.deepEqual(revision.recipientEdit.measured,revision.recipientEdit.repeat);
        assert.equal(revision.recipientEdit.measured.miss.contactTicks,0);
        assert(revision.recipientEdit.measured.playerEnvelopeClear);
        assert.equal((await call('list')).scenes.length,2);
        await assert.rejects(apply(stale.reviewId),/newer local revision/i);
        const raw = JSON.parse(revision.sourceText);
        const edited = raw.objects.find(item=>item.sourceId===prop.sourceId);
        assert.deepEqual(edited.position,[.48,prop.position[1],.82]);
        assert.equal(edited.visualMesh,prop.visualMesh);
        assert.deepEqual(raw.objects.filter(item=>item.sourceId!==prop.sourceId),
            document.scene.objects.filter(item=>item.sourceId!==prop.sourceId));
        assert.deepEqual(await call('read',{id:root.id}),root);
        await page.locator('#recipient-edit-open').click();
        await page.waitForFunction(id=>document.querySelector('#spatial-scenes')?.value===id,revision.id);

        // Attachments stay pinned even when the main viewer has opened another archive.
        const deck = await fs.readFile(path.join(base,'placement-exchange','edition232.fraynix-placement.json'));
        await call('placement-exchange-open',{base64:deck.toString('base64')});
        await page.locator('#recipient-edit').click();
        await page.locator('#recipient-open-evidence').click();
        await check('exchange-scene-title','Signal Bay');
        assert.equal((await call('placement-exchange-latest')).id,digest(deck));
        await page.locator('#exchange-close').click();

        const second = await plan(revision.id,.59,.78,42);
        await approve(second.reviewId);
        const final = await apply(second.reviewId);
        assert.equal((await call('placement-exchange-fork',fork)).id,final.sceneId);
        await restart();
        const recovered = await call('read',{id:final.sceneId});
        assert.equal(recovered.recipientRevision,2);
        assert.deepEqual(recovered.recipientBranch,root.recipientBranch);
        assert.deepEqual(recovered.recipientEdit.measured,recovered.recipientEdit.repeat);
        assert.equal((await call('list')).scenes.length,3);

        const directory = path.join(project,'spatial','placement-exchanges');
        const replay = path.join(directory,archiveId+'.replay.json');
        await fs.writeFile(replay,'{}');
        assert.deepEqual(await call('read',{id:final.sceneId}),recovered);
        assert.deepEqual((await call('recipient-evidence',{sceneId:final.sceneId})).replay,local.replay);
        const approvalFile = path.join(project,'spatial','recipient-edits',second.reviewId+'.approved');
        const approvalBytes = await fs.readFile(approvalFile);
        await fs.appendFile(approvalFile,' ');
        await assert.rejects(call('read',{id:final.sceneId}),/recheck the recipient branch/i);
        await fs.writeFile(approvalFile,approvalBytes);
        const retained = path.join(directory,archiveId+'.json');
        assert((await fs.readFile(retained)).equals(bytes));
        await fs.appendFile(retained,' ');
        await assert.rejects(call('read',{id:final.sceneId}),/recheck the recipient branch/i);
        await assert.rejects(plan(final.sceneId,.7,.5,12),/recheck the recipient branch/i);
        await fs.writeFile(retained,bytes);
        assert.deepEqual(await call('read',{id:final.sceneId}),recovered);
        await page.locator('#spatial-scenes').selectOption(final.sceneId);
        await page.locator('#recipient-edit').click();
        await page.locator('#recipient-edit-export').click();
        await check('recipient-edit-export-path','Saved ');
        const file = await page.locator('#recipient-edit-export-path').getAttribute('title');
        const exported = await fs.readFile(file);
        const sidecar = JSON.parse(await fs.readFile(file+'.receipt.json','utf8'));
        const packageId = digest(exported);
        assert.equal(sidecar.sha256,packageId);
        assert.equal(sidecar.source.originalArchiveSha256,archiveId);
        const shared = JSON.parse(exported);
        assert(Buffer.from(shared.originalArchiveBase64,'base64').equals(bytes));
        assert.deepEqual(shared.baseReplay,local.replay);
        assert.deepEqual(shared.revisions.map(stage=>stage.approvalSha256),[pending.reviewId,second.reviewId]);
        assert.equal(shared.headSceneId,final.sceneId);
        assert.deepEqual(shared.revisions.at(-1).receipt,recovered.recipientEdit);
        const reconstructed = await call('recipient-edit-export',{sceneId:final.sceneId});
        assert.equal(reconstructed.sha256,packageId);
        assert(Buffer.from(reconstructed.base64,'base64').equals(exported));
        await capture('recipient-revision-exported.png');
        await fs.writeFile(path.join(output,'signal-bay-revision.fraynix-recipient.json'),exported);

        const measured = revision.recipientEdit.measured;
        const proof = {
            platform:process.platform, archiveSha256:archiveId, localReplaySha256:local.replay.sha256,
            rootSceneId:root.id, revisionSceneId:revision.id, finalSceneId:final.sceneId,
            fromM:prop.position,toM:edited.position,yawDeg:applied.review.yawDeg,
            hitTick:measured.hit.firstContactTick,hitContacts:measured.hit.contactTicks,
            missContacts:measured.miss.contactTicks,aisleM:measured.routeLengthM,
            packageSha256:packageId,exportedApprovals:shared.revisions.length,exportedFinalScene:shared.headSceneId,
            secondEditVerified:true,blockingPoseRefused:true,interceptedProbeRefused:true,unapprovedRefused:true,
            staleReviewRefused:true,previewRestart:true,approvalRestart:true,renderedOriginalPinned:true,
            archiveAlterationRefused:true,changedApprovalRefused:true,retainedReplayIndependentOfViewerCache:true,archiveBytesPreserved:true
        };
        await fs.writeFile(path.join(output,'recipient-edit-proof.json'),JSON.stringify(proof,null,2)+'\n');
        await fs.writeFile(path.join(output,'local-edit-receipt.json'),JSON.stringify(revision.recipientEdit,null,2)+'\n');
        console.log(JSON.stringify(proof));
    } finally {
        await app?.close().catch(()=>{});
    }
}
main().catch(error=>{console.error(error);process.exitCode=1;});
