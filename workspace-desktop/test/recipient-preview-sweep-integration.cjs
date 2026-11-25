'use strict';

const assert = require('node:assert/strict');
const {createHash} = require('node:crypto');
const fs = require('node:fs/promises');
const path = require('node:path');
const os = require('node:os');
const {launch} = require('./calibration-integration.cjs');

const sha = bytes => createHash('sha256').update(bytes).digest('hex');

async function main() {
    process.env.FRAYNIX_WORKSPACE_NO_MODELS = '1';
    const out = path.resolve(process.argv[2] || 'build/recipient-preview244');
    const fixture = path.resolve(process.argv[3] || 'docs/readmes/240/signal-bay-handoff.fraynix-recipient.json');
    const archive = await fs.readFile(fixture);
    const project = await fs.mkdtemp(path.join(os.tmpdir(),'Fraynix pending collider '));
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
    try {
        await page.locator('#spatial-tab').click();
        await page.locator('#placement-exchange').click();
        await page.waitForFunction(()=>!document.querySelector('#exchange-file').disabled);
        await page.locator('#exchange-file').setInputFiles(fixture);
        await check('exchange-status','Bundle and GLB checked');
        await page.locator('#exchange-replay').click();
        await check('exchange-status','Local physics replay matched');
        await page.locator('#exchange-fork').click();
        await page.locator('#recipient-edit-dialog[open]').waitFor();
        const sceneId = await page.locator('#spatial-scenes').inputValue();
        const base = await call('read',{id:sceneId});
        const plan = (colliderWidthM,colliderDepthM) => call('recipient-edit-review',{sceneId,colliderWidthM,colliderDepthM});
        const review = await plan(.56,.34);
        await page.locator('#recipient-edit-close').click();
        await page.locator('#goal').fill('Compare the proposed collider across five approaches.');
        await page.getByRole('button',{name:'Start task',exact:false}).click();
        await check('recipient-approach-status','Saved comparison for this exact preview');
        const view = await call('recipient-edit-latest',{sceneId});
        assert.equal(view.status,'review');
        assert.equal(view.reviewId,review.reviewId);
        const comparison = view.comparison;
        const binding = comparison.binding.pair;
        assert.equal(binding.reviewId,review.reviewId);
        assert.equal(binding.beforeSceneId,sceneId);
        assert.deepEqual(binding.parent,review.review.parent);
        assert.deepEqual(comparison.summary,{withinTolerance:0,earlier:5,later:0,needsCheck:0,toleranceTicks:1});
        assert.equal(await page.locator('#recipient-approach-rows tr').count(),5);
        assert.equal((await call('list')).scenes.length,1);
        assert.deepEqual(await call('read',{id:sceneId}),base);
        await assert.rejects(call('recipient-edit-run',{reviewId:review.reviewId}),/Approve this exact local edit/i);
        const directory = path.join(project,'spatial','recipient-edits');
        const receiptPath = path.join(directory,review.reviewId+'.sweep.json');
        const approvalPath = path.join(directory,review.reviewId+'.approved');
        await assert.rejects(fs.access(approvalPath),{code:'ENOENT'});
        const first = await fs.readFile(receiptPath);
        await page.locator('#recipient-approach-data').scrollIntoViewIfNeeded();
        await capture('chat-pending-approaches.png');

        await restart();
        await page.locator('#recipient-edit').click();
        await check('recipient-approach-status','Saved comparison for this exact preview');
        assert.deepEqual((await call('recipient-edit-latest',{sceneId})).comparison,comparison);
        await page.locator('#recipient-approach-data').scrollIntoViewIfNeeded();
        await page.locator('#recipient-edit-approve').scrollIntoViewIfNeeded();
        await capture('pending-evidence-restored.png');
        const replaced = await plan(.55,.33);
        assert.notEqual(replaced.reviewId,review.reviewId);
        assert(!replaced.comparison && replaced.status==='review');
        await assert.rejects(call('recipient-edit-compare',{reviewId:review.reviewId}),/review was replaced/i);
        await assert.rejects(call('recipient-edit-approve',{reviewId:replaced.reviewId,approvalSha256:replaced.reviewId,
            comparisonSha256:comparison.sha256}),/selected comparison changed/i);
        await plan(.56,.34);
        const damaged = JSON.parse(first);
        damaged.cases[1].original.hit.samples[0].probeM[0] += .1;
        await fs.writeFile(receiptPath,JSON.stringify(damaged));
        await assert.rejects(call('recipient-edit-approve',{reviewId:review.reviewId,approvalSha256:review.reviewId,
            comparisonSha256:comparison.sha256}),/selected comparison changed/i);
        await page.locator('#recipient-edit-close').click();
        await page.locator('#recipient-edit').click();
        await check('recipient-approach-status','saved comparison changed');
        assert(await page.locator('#recipient-approach-data').isHidden());
        await capture('changed-preview-evidence-refused.png');
        await page.locator('#recipient-compare').click();
        await check('recipient-approach-status','Saved comparison for this exact preview');
        assert((await fs.readFile(receiptPath)).equals(first));
        await page.getByRole('button',{name:'Approve and verify local edit',exact:true}).click();
        await check('recipient-edit-status','Local revision verified');
        const final = await call('recipient-edit-run',{reviewId:review.reviewId});
        const edited = await call('read',{id:final.sceneId});
        assert.equal(edited.recipientRevision,1);
        assert.equal((await call('list')).scenes.length,2);
        assert.deepEqual(edited.recipientEdit.measured,edited.recipientEdit.repeat);
        assert.equal(edited.recipientEdit.measured.miss.contactTicks,0);
        assert(edited.recipientEdit.measured.playerEnvelopeClear);
        await page.locator('#recipient-edit-export').click();
        await check('recipient-edit-export-path','Saved ');
        const file=await page.locator('#recipient-edit-export-path').getAttribute('title');
        const exported=await fs.readFile(file);
        assert.equal(JSON.parse(exported).headSceneId,edited.id);
        assert(Buffer.from(JSON.parse(exported).parentArchiveBase64,'base64').equals(archive));
        const proof={platform:process.platform,archiveSha256:sha(archive),reviewId:review.reviewId,
            comparisonSha256:comparison.sha256,timing:{...comparison.summary},
            rows:comparison.cases.map(row=>[row.original.hit.firstContactTick,row.resized.hit.firstContactTick]),
            explicitChatRoute:true,unchangedBeforeApproval:true,noEarlyApproval:true,restartVerified:true,
            changedCandidateAndReceiptRefused:true,regeneratedExactReceipt:true,postApprovalNativeRepeatsAndExport:true};
        await fs.writeFile(path.join(out,'pending-approach-evidence.json'),JSON.stringify(proof,null,2)+'\n');
        await fs.writeFile(path.join(out,'pending-approach-receipt.json'),first);
        console.log(JSON.stringify(proof));
    } finally {
        await app?.close().catch(()=>{});
    }
}
main().catch(error=>{console.error(error);process.exitCode=1;});
