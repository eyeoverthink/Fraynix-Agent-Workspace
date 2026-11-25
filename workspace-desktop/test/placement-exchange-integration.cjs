
'use strict';

const assert = require('node:assert/strict');
const {createHash} = require('node:crypto');
const fs = require('node:fs/promises');
const os = require('node:os');
const path = require('node:path');
const {launch} = require('./calibration-integration.cjs');

const sha = value => createHash('sha256').update(value).digest('hex');
const frozen = path.resolve(process.argv[3] || path.join(__dirname,'fixtures','placement-exchange','edition232.fraynix-placement.json'));

async function main() {
    process.env.FRAYNIX_WORKSPACE_NO_MODELS = '1';
    const output = path.resolve(process.argv[2] || 'build/visual-placement-exchange233');
    const project = await fs.mkdtemp(path.join(os.tmpdir(),'Fraynix exchange recipient '));
    await fs.mkdir(output,{recursive:true});
    const original = await fs.readFile(frozen);
    const id = sha(original);
    const document = JSON.parse(original);
    const isRecipient = document.format === 'fraynix.verified-recipient-level-v1';
    const isLevel = isRecipient || document.format.startsWith('fraynix.verified-spatial-level-');
    const exactDestination = isRecipient || document.format === 'fraynix.verified-spatial-level-v2';
    const terminal = isRecipient ? document.revisions.at(-1).receipt : document.evidence;
    const args = process.platform === 'linux' ? ['--no-sandbox','--disable-dev-shm-usage'] : [];
    let {app,page} = await launch(project,args);
    const call = (route,input={}) => app.evaluate((_,request) =>
        global.workspaceTest.api('/api/spatial/'+request.route,request.input), {route,input});
    const status = text => page.waitForFunction(value =>
        document.querySelector('#exchange-status')?.textContent.includes(value),text);
    const receiptPath = path.join(project,'spatial','placement-exchanges',id+'.replay.json');
    const render = async name => {
        await page.evaluate(()=>new Promise(done=>requestAnimationFrame(()=>requestAnimationFrame(done))));
        await fs.writeFile(path.join(output,name),Buffer.from(await app.evaluate(()=>global.workspaceTest.capture()),'base64'));
    };
    async function viewer() {
        await page.locator('#spatial-tab').click();
        await page.locator('#placement-exchange').click();
        await page.locator('#placement-exchange-dialog[open]').waitFor();
    }

    try {
        await viewer();
        await status('Open a Fraynix scene');
        assert(await page.locator('#exchange-replay').isDisabled());
        await page.waitForFunction(()=>!document.querySelector('#exchange-file').disabled);
        await page.locator('#exchange-file').setInputFiles(frozen);
        await status('Bundle and GLB checked');
        const archive = await call('placement-exchange-latest');
        assert.equal(archive.id,id);
        assert.equal(archive.replay,undefined);
        assert.equal(archive.archived.hit.firstContactTick,
            isLevel ? terminal.measured.hit.firstContactTick : 43);
        assert.equal(archive.archived.miss.contactTicks,0);
        assert.deepEqual(await call('placement-latest'),{});
        assert.equal((await call('list')).scenes.length,0);
        if (isLevel) {
            assert.equal(archive.destinationVerified,exactDestination);
            assert.equal(archive.layout.routeLengthM,terminal.measured.routeLengthM);
            assert(archive.layout.blockers.some(item=>item.id===archive.layout.prop));
            assert(archive.layout.blockers.some(item=>item.id!==archive.layout.prop));
            if (exactDestination) assert.match(await page.locator('#exchange-provenance').textContent(),/Existing colliders|existing colliders/);
            await page.locator('#exchange-trial').selectOption('aisle');
            assert.match(await page.locator('#exchange-metrics').textContent(),/local clearance pending/);
            assert(await page.locator('#exchange-sample').isHidden());
        }
        if (isRecipient) {
            assert.equal(archive.recipientRevision,document.revisions.length);
            assert.equal(archive.originalArchiveSha256,document.originalArchiveSha256);
            assert.match(await page.locator('#exchange-revisions').textContent(),/approvals checked/);
            assert.equal(await page.locator('#exchange-stage').inputValue(),String(document.revisions.length));
            assert(await page.locator('#exchange-fork').isHidden());
        }
        await render('archive-inspected.png');

        const bad = JSON.parse(original);
        const badReceipt = isRecipient ? bad.revisions[0].receipt : bad.evidence;
        badReceipt.measured.hit.samples[2].probeM[0] += .25;
        const badFile = path.join(output,'tampered-placement.json');
        await fs.writeFile(badFile,JSON.stringify(bad));
        await page.locator('#exchange-file').setInputFiles(badFile);
        await status(isRecipient ? 'approval, review or trial receipt differs' : 'collision evidence checksum differs');
        assert.equal((await call('placement-exchange-latest')).id,id);
        if (isRecipient) {
            const missingApproval = JSON.parse(original);
            missingApproval.revisions[0].approvalSha256 = '0'.repeat(64);
            await assert.rejects(call('placement-exchange-open',
                {base64:Buffer.from(JSON.stringify(missingApproval)).toString('base64')}),/approval, review or trial receipt differs/);
        }
        const badGlb = JSON.parse(original);
        badGlb.glbSha256 = '0'.repeat(64);
        await assert.rejects(call('placement-exchange-open',
            {base64:Buffer.from(JSON.stringify(badGlb)).toString('base64')}),/embedded GLB checksum/);

        await page.locator('#exchange-replay').click();
        await status('Local physics replay matched');
        const restored = await call('placement-exchange-latest');
        const report = restored.replay.comparison;
        assert(report.passed);
        assert(report.hit.maxPositionErrorM <= report.positionToleranceM);
        assert(report.hit.maxVelocityErrorMps <= report.velocityToleranceMps);
        assert.equal(restored.replay.trials.miss.contactTicks,0);
        if (isLevel) {
            assert.equal(restored.replay.trials.playerEnvelopeClear,true);
            assert.equal(report.aisle.routeLengthErrorM,0);
            assert.match(await page.locator('#exchange-metrics').textContent(),/local clearance passed in both runs/);
            await render('local-aisle-replay.png');
            await page.locator('#exchange-trial').selectOption('hit');
        }
        await render('local-hit-replay.png');
        if (isRecipient) {
            const earlier = restored.replay.previous;
            assert.equal(earlier.length,document.revisions.length);
            const source = JSON.parse(Buffer.from(document.originalArchiveBase64,'base64'));
            assert.equal(earlier[0].sceneId,source.sceneId);
            const expected = [source.evidence,...document.revisions.slice(0,-1).map(stage=>stage.receipt)];
            for (const [index,receipt] of expected.entries()) {
                const native = earlier[index];
                assert(native.comparison.passed&&native.comparison.aisle.passed);
                assert(Math.abs(native.trials.hit.firstContactTick-receipt.measured.hit.firstContactTick)<=1);
                await page.locator('#exchange-stage').selectOption(String(index));
                assert.match(await page.locator('#exchange-metrics').textContent(),/archived \/ .* local/);
            }
            await page.locator('#exchange-stage').selectOption('0');
            await render('original-placement-replayed.png');
            await page.locator('#exchange-stage').selectOption(String(document.revisions.length));
            assert.match(await page.locator('#exchange-revisions').textContent(),/all revisions replayed locally/);
            await assert.rejects(call('placement-exchange-fork',{id,replaySha256:restored.replay.sha256}),
                /authored bundle with its checked original destination/i);
        }

        await page.locator('#exchange-trial').selectOption('miss');
        await page.locator('#exchange-sample').focus();
        await page.keyboard.press('End');
        await page.waitForFunction(()=>document.querySelector('#exchange-reading').textContent.includes('tick 180'));
        assert.match(await page.locator('#exchange-metrics').textContent(),/0 archived \/ 0 local/);
        await render('local-miss-replay.png');

        await app.close();
        ({app,page} = await launch(project,args));
        await viewer();
        await status('Saved local replay matches the archive');
        assert.deepEqual(await call('placement-exchange-latest'),restored);

        const clipped = JSON.parse(await fs.readFile(receiptPath,'utf8'));
        clipped.trials.hit.samples[1].probeM[0] += .5;
        await fs.writeFile(receiptPath,JSON.stringify(clipped));
        await page.locator('#exchange-close').click();
        await page.locator('#placement-exchange').click();
        await status('saved local replay changed');
        assert.equal((await call('placement-exchange-latest')).replay,undefined);
        await page.locator('#exchange-replay').click();
        await status('Local physics replay matched');
        assert.deepEqual(await call('placement-exchange-latest'),restored);
        assert.equal((await call('list')).scenes.length,0);
        assert.deepEqual(await call('placement-latest'),{});
        const saved = await fs.readFile(path.join(project,'spatial','placement-exchanges',id+'.json'));
        assert(saved.equals(original));
        await fs.writeFile(path.join(output,'local-replay.json'),await fs.readFile(receiptPath));
        const evidence = {format:'fraynix.placement-exchange-acceptance-v1',platform:process.platform,
            archiveSha256:id,localReplaySha256:sha(await fs.readFile(receiptPath)),comparison:report,
            archiveHitTick:archive.archived.hit.firstContactTick,localHitTick:restored.replay.trials.hit.firstContactTick,
            archiveMissContacts:archive.archived.miss.contactTicks,localMissContacts:restored.replay.trials.miss.contactTicks,
            freshProject:true,noPlacementAuthorityCreated:true,restoredAfterRestart:true,archiveUntouched:true,
            refusedChangedArchive:true,refusedChangedGlb:true,changedLocalReplayRefusedAndRecovered:true};
        if (isLevel) Object.assign(evidence,{destinationVerified:archive.destinationVerified,
            routeLengthM:archive.layout.routeLengthM,playerEnvelopeClear:restored.replay.trials.playerEnvelopeClear,
            editedMesh:Boolean((isRecipient ? archive.previousStages[0].review : archive.review).editReference),
            archiveHitContacts:archive.archived.hit.contactTicks,localHitContacts:restored.replay.trials.hit.contactTicks});
        if (isRecipient) Object.assign(evidence,{
            originalArchiveSha256:archive.originalArchiveSha256,
            approvedRevisions:archive.recipientRevision, previousStageCount:restored.replay.previous.length,
            stageHitTicks:[...restored.replay.previous.map(stage=>stage.trials.hit.firstContactTick),
                restored.replay.trials.hit.firstContactTick],
            stageMissContacts:[...restored.replay.previous.map(stage=>stage.trials.miss.contactTicks),
                restored.replay.trials.miss.contactTicks],
            refusedAlteredApproval:true, allStagesReplayedLocally:true
        });
        await fs.writeFile(path.join(output,'exchange-evidence.json'),JSON.stringify(evidence,null,2)+'\n');
        console.log(JSON.stringify(evidence));
    } finally {
        await app?.close().catch(()=>{});
    }
}
main().catch(error=>{console.error(error);process.exitCode=1;});
