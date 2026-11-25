'use strict';

const assert = require('node:assert/strict');
const {createHash} = require('node:crypto');
const fs = require('node:fs/promises');
const os = require('node:os');
const path = require('node:path');
const {launch} = require('./calibration-integration.cjs');

const sha = bytes => createHash('sha256').update(bytes).digest('hex');
const pack = value => Buffer.from(JSON.stringify(value)).toString('base64');

async function main() {
    process.env.FRAYNIX_WORKSPACE_NO_MODELS = '1';
    const output = path.resolve(process.argv[2] || 'build/signal-bay-handoff240');
    const fixture = path.resolve(process.argv[3] || path.join(__dirname,'fixtures','recipient-revision-exchange',
        'signal-bay-revision.fraynix-recipient.json'));
    const source = await fs.readFile(fixture);
    const archived = JSON.parse(source);
    const original = JSON.parse(Buffer.from(archived.originalArchiveBase64,'base64'));
    const inherited = archived.revisions.at(-1).receipt;
    const id = sha(source);
    const project = await fs.mkdtemp(path.join(os.tmpdir(),'Fraynix forwarded recipient '));
    const args = process.platform==='linux' ? ['--no-sandbox','--disable-dev-shm-usage'] : [];
    await fs.mkdir(output,{recursive:true});
    let {app,page} = await launch(project,args);
    const call = (route,input={}) => app.evaluate((_,request) =>
        global.workspaceTest.api('/api/spatial/'+request.route,request.input),{route,input});
    const check = (id,value) => page.waitForFunction(({id,value}) =>
        document.getElementById(id)?.textContent.includes(value),{id,value});
    const capture = async name => {
        await page.evaluate(()=>new Promise(resolve=>requestAnimationFrame(()=>requestAnimationFrame(resolve))));
        await fs.writeFile(path.join(output,name),Buffer.from(await app.evaluate(()=>global.workspaceTest.capture()),'base64'));
    };
    const restart = async () => {
        await app.close();
        ({app,page} = await launch(project,args));
        await page.locator('#spatial-tab').click();
    };
    try {
        await page.locator('#spatial-tab').click();
        await page.locator('#placement-exchange').click();
        await page.waitForFunction(()=>!document.querySelector('#exchange-file').disabled);
        await page.locator('#exchange-file').setInputFiles(fixture);
        await check('exchange-status','Bundle and GLB checked');
        assert(await page.locator('#exchange-fork').isHidden());
        assert.equal((await call('list')).scenes.length,0);
        const createCopy = replaySha256 => call('placement-exchange-fork',{id,replaySha256});
        await assert.rejects(createCopy(archived.baseReplay.sha256),/Replay this exact bundle locally/i);
        await page.locator('#exchange-replay').click();
        await check('exchange-status','Local physics replay matched');
        const live = await call('placement-exchange-latest');
        assert.equal((await call('list')).scenes.length,0);
        assert.equal(live.replay.previous.length,archived.revisions.length);
        assert.equal(live.replay.trials.hit.firstContactTick,inherited.measured.hit.firstContactTick);
        await assert.rejects(createCopy(archived.baseReplay.sha256),/Replay this exact bundle locally/i);
        await capture('forwarded-replay-ready.png');

        await page.getByRole('button',{name:'Create editable copy of final revision',exact:true}).click();
        await page.locator('#recipient-edit-dialog[open]').waitFor();
        await check('recipient-copy-summary','Inherited approved edits: '+archived.revisions.length);
        const rootId = await page.locator('#spatial-scenes').inputValue();
        const root = await call('read',{id:rootId});
        assert.notEqual(root.id,archived.headSceneId);
        assert.equal(root.recipientRevision,0);
        assert.equal(root.recipientEdit,undefined);
        assert.equal(root.recipientBranch.archiveSha256,id);
        assert.equal(root.recipientBranch.originalArchiveSha256,archived.originalArchiveSha256);
        assert.equal(root.recipientBranch.glbSha256,archived.glbSha256);
        assert.equal(root.recipientBranch.localReplaySha256,live.replay.sha256);
        assert.deepEqual(root.recipientBaseReplay,live.replay);
        assert.deepEqual((await createCopy(live.replay.sha256)),root);
        assert.equal((await call('list')).scenes.length,1);
        assert.deepEqual(await call('placement-latest'),{});
        assert((await fs.readdir(path.join(project,'spatial','recipient-edits')).catch(error=>{
            if (error.code === 'ENOENT') return [];
            throw error;
        })).every(name=>!name.endsWith('.approved')));
        const propKey = root.recipientBranch.propSourceId;
        const rootObjects = JSON.parse(root.sourceText).objects;
        const placed = rootObjects.find(item=>item.sourceId===propKey);
        assert.deepEqual(placed.position,inherited.review.targetM);
        assert.deepEqual(placed.rotation,inherited.review.rotation);
        assert.deepEqual(rootObjects.filter(item=>item.sourceId!==propKey),
            original.scene.objects.filter(item=>item.sourceId!==propKey));
        assert.equal(Number(await page.locator('#recipient-edit-form input[name=yawDeg]').inputValue()),inherited.review.yawDeg);
        assert(await page.locator('#recipient-edit-export').isHidden());
        await assert.rejects(call('recipient-edit-export',{sceneId:root.id}),/Complete a verified local edit/i);
        await capture('editable-inherited-copy.png');

        await page.getByRole('button',{name:'Open inherited evidence',exact:true}).click();
        await check('exchange-status','Saved local replay matches');
        const evidence = await call('recipient-evidence',{sceneId:root.id});
        assert.deepEqual(evidence,live);
        assert(await page.locator('#exchange-fork').isHidden());
        await page.locator('#exchange-close').click();
        await page.locator('#recipient-edit').click();
        await assert.rejects(call('recipient-edit-review',{sceneId:root.id,xM:.59,zM:-1.1,yawDeg:35}),
            /player aisle is blocked/i);
        for (const [field,value] of Object.entries({xM:.72,zM:.85,yawDeg:35}))
            await page.locator('#recipient-edit-form input[name='+field+']').fill(String(value));
        await page.getByRole('button',{name:'Preview local edit',exact:true}).click();
        await check('recipient-edit-status','Preview saved');
        const review = await call('recipient-edit-latest',{sceneId:root.id});
        const reviewId = review.reviewId;
        assert.notEqual(reviewId,inherited.reviewId);
        assert.equal(review.review.parent.sceneId,root.id);
        assert.deepEqual(review.review.branch,root.recipientBranch);
        await assert.rejects(call('recipient-edit-run',{reviewId}),/Approve this exact local edit/i);
        await assert.rejects(call('recipient-edit-approve',{reviewId,approvalSha256:inherited.reviewId}),
            /Approve the exact saved local edit/i);
        await assert.rejects(call('recipient-edit-run',{reviewId:inherited.reviewId}));
        await capture('new-local-approval-required.png');

        await restart();
        await page.locator('#recipient-edit').click();
        await check('recipient-edit-status','Preview saved');
        assert.deepEqual(await call('recipient-edit-latest',{sceneId:root.id}),review);
        await call('recipient-edit-approve',{reviewId,approvalSha256:reviewId});
        await restart();
        await page.locator('#recipient-edit').click();
        await check('recipient-edit-status','Approval saved');
        await page.getByRole('button',{name:'Run approved edit checks',exact:true}).click();
        await check('recipient-edit-status','Local revision verified');
        const applied = await call('recipient-edit-run',{reviewId});
        const result = await call('read',{id:applied.sceneId});
        assert(result.recipientRevision===1 && result.id!==root.id);
        assert.deepEqual(result.recipientBranch,root.recipientBranch);
        assert.deepEqual(result.recipientEdit.measured,result.recipientEdit.repeat);
        assert.equal(result.recipientEdit.measured.miss.contactTicks,0);
        assert(result.recipientEdit.measured.playerEnvelopeClear);
        assert.equal((await call('list')).scenes.length,2);
        assert.deepEqual(await createCopy(live.replay.sha256),result);

        const directory = path.join(project,'spatial','placement-exchanges');
        await fs.writeFile(path.join(directory,id+'.replay.json'),'{}');
        assert.equal((await call('placement-exchange-latest')).replay,undefined);
        assert.deepEqual(await call('recipient-evidence',{sceneId:result.id}),evidence);
        const exact = path.join(directory,id+'.json');
        assert((await fs.readFile(exact)).equals(source));
        await fs.appendFile(exact,' ');
        await assert.rejects(call('read',{id:result.id}),/recheck the recipient branch/i);
        await assert.rejects(call('recipient-edit-export',{sceneId:result.id}),/recheck the recipient branch/i);
        await fs.writeFile(exact,source);
        assert.deepEqual(await call('read',{id:result.id}),result);

        await page.locator('#recipient-edit-export').click();
        await check('recipient-edit-export-path','Saved ');
        const file = await page.locator('#recipient-edit-export-path').getAttribute('title');
        const bytes = await fs.readFile(file);
        const sidecar = JSON.parse(await fs.readFile(file+'.receipt.json','utf8'));
        const shared = JSON.parse(bytes);
        assert.equal(sidecar.sha256,sha(bytes));
        assert.equal(sidecar.source.parentArchiveSha256,id);
        assert.equal(sidecar.source.originalArchiveSha256,archived.originalArchiveSha256);
        assert.equal(shared.format,'fraynix.verified-recipient-handoff-v2');
        assert.equal(shared.parentArchiveSha256,id);
        assert(Buffer.from(shared.parentArchiveBase64,'base64').equals(source));
        assert.deepEqual(shared.baseReplay,live.replay);
        assert.equal(shared.revisions.length,1);
        assert.equal(shared.revisions[0].approvalSha256,reviewId);
        assert.deepEqual(shared.revisions[0].receipt,result.recipientEdit);
        assert.equal(shared.headSceneId,result.id);
        const repeated = await call('recipient-edit-export',{sceneId:result.id});
        assert(Buffer.from(repeated.base64,'base64').equals(bytes));

        const changed = Buffer.concat([source,Buffer.from(' ')]);
        const altered = {...shared,parentArchiveBase64:changed.toString('base64')};
        await assert.rejects(call('placement-exchange-open',{base64:pack(altered)}),/attached archive checksum differs/i);
        altered.parentArchiveSha256 = sha(changed);
        await assert.rejects(call('placement-exchange-open',{base64:pack(altered)}),/pinned local replay differs/i);
        const badReceipt = {...shared,baseReplay:{...shared.baseReplay,previous:[]}};
        await assert.rejects(call('placement-exchange-open',{base64:pack(badReceipt)}),/pinned local replay differs/i);
        await capture('new-revision-exported.png');
        await fs.writeFile(path.join(output,'signal-bay-handoff.fraynix-recipient.json'),bytes);
        const measured = result.recipientEdit.measured;
        const proof = {platform:process.platform,forwardedSha256:id,originalArchiveSha256:archived.originalArchiveSha256,
            forwardHead:archived.headSceneId,editableCopy:root.id,inheritedApprovalCount:archived.revisions.length,
            newRevision:result.id,newApprovalSha256:reviewId,exportSha256:sha(bytes),
            fromM:placed.position,toM:result.recipientEdit.review.targetM,yawDeg:35,
            hitTick:measured.hit.firstContactTick,hitContacts:measured.hit.contactTicks,missContacts:measured.miss.contactTicks,
            aisleM:measured.routeLengthM,noSceneBeforeExplicitCopy:true,copyRequiresLocalReplay:true,oldApprovalRefused:true,
            newPreviewAndApprovalSurvivedRestart:true,protectedAisleRefused:true,attachedReceiptSurvivedCacheDamage:true,
            modifiedParentRefused:true,modifiedPinnedReplayRefused:true,originalBytesPreserved:true};
        await fs.writeFile(path.join(output,'handoff-evidence.json'),JSON.stringify(proof,null,2)+'\n');
        console.log(JSON.stringify(proof));
    } finally {
        await app?.close().catch(()=>{});
    }
}
main().catch(error=>{console.error(error);process.exitCode=1;});
