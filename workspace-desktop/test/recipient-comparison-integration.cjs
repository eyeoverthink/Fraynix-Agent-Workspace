'use strict';

const assert = require('node:assert/strict');
const {createHash} = require('node:crypto');
const fs = require('node:fs/promises');
const os = require('node:os');
const path = require('node:path');
const {launch} = require('./calibration-integration.cjs');

const sha = bytes => createHash('sha256').update(bytes).digest('hex');

async function main() {
    process.env.FRAYNIX_WORKSPACE_NO_MODELS = '1';
    const output = path.resolve(process.argv[2] || 'build/recipient-comparison242');
    const fixture = path.resolve(process.argv[3] || 'docs/readmes/241/signal-bay-collider.fraynix-recipient.json');
    const bytes = await fs.readFile(fixture);
    const id = sha(bytes);
    const archive = JSON.parse(bytes);
    const previous = JSON.parse(Buffer.from(archive.parentArchiveBase64,'base64'));
    const before = previous.revisions.at(-1).receipt;
    const after = archive.revisions.at(-1).receipt;
    const project = await fs.mkdtemp(path.join(os.tmpdir(),'Fraynix same approach '));
    const evidenceDir = path.join(project,'spatial','placement-exchanges');
    const comparisonPath = path.join(evidenceDir,id+'.comparison.json');
    const replayPath = path.join(evidenceDir,id+'.replay.json');
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
    async function viewer() {
        await page.locator('#spatial-tab').click();
        await page.locator('#placement-exchange').click();
        await page.locator('#placement-exchange-dialog[open]').waitFor();
    }
    const openComparison = async () => {
        await page.locator('#exchange-trial').selectOption('compare');
        assert(await page.locator('#exchange-fixed-approach').isVisible());
        assert(await page.locator('#exchange-canvas').isHidden());
        assert(await page.locator('#exchange-stage-label').isHidden());
    };

    try {
        await viewer();
        await page.waitForFunction(()=>!document.querySelector('#exchange-file').disabled);
        await page.locator('#exchange-file').setInputFiles(fixture);
        await check('exchange-status','Bundle and GLB checked');
        assert.equal(await page.locator('#exchange-trial option[value=compare]').getAttribute('hidden'),'');
        await assert.rejects(call('placement-exchange-compare',{id,replaySha256:archive.baseReplay.sha256}),
            /Replay the exact bundle locally before comparing/i);
        await assert.rejects(fs.access(comparisonPath),{code:'ENOENT'});
        await page.locator('#exchange-replay').click();
        await check('exchange-status','Local physics replay matched');
        const replay = await call('placement-exchange-latest');
        assert(replay.colliderComparisonReady && !replay.colliderComparison);
        assert.equal(replay.replay.previous.length,4);
        const replayBytes = await fs.readFile(replayPath);
        await assert.rejects(call('placement-exchange-compare',{id,replaySha256:'0'.repeat(64)}),
            /Replay the exact bundle locally before comparing/i);
        await openComparison();
        await check('exchange-comparison-status','most recent collider edit');
        await page.locator('#exchange-compare').click();
        await check('exchange-comparison-status','Verified comparison saved locally');
        await check('exchange-comparison-verdict','earlier');
        await capture('same-probe-comparison.png');

        const resolved = await call('placement-exchange-latest');
        const result = resolved.colliderComparison;
        const {binding,original,resized,result:metrics} = result;
        const {approach} = binding;
        assert.equal(binding.archiveSha256,id);
        assert.equal(binding.replaySha256,replay.replay.sha256);
        assert.equal(binding.beforeSceneId,previous.headSceneId);
        assert.equal(binding.afterSceneId,archive.headSceneId);
        assert.deepEqual(binding.beforeColliderM,after.review.collider.previousM);
        assert.deepEqual(binding.afterColliderM,after.review.collider.proposedM);
        assert.deepEqual(binding.targetM,after.review.targetM);
        assert.equal(binding.yawDeg,after.review.yawDeg);
        assert.equal(binding.meshSha256,after.review.collider.meshSha256);
        assert.equal(approach.trialCount,8);
        assert.equal(approach.stepS,1/240);
        assert.deepEqual(approach.velocityMps.map(v=>Number(v.toFixed(3))),[1.8,0,0]);
        assert.notDeepEqual(approach.hitStartM,approach.missStartM);
        for (const kind of ['hit','miss']) {
            const start = approach[kind+'StartM'];
            for (const stage of [original,resized]) {
                assert.deepEqual(stage[kind].probeStartM,start);
                assert.deepEqual(stage[kind].samples[0].probeM,start);
                assert.deepEqual(stage[kind].samples[0].probeVelocityMps,approach.velocityMps);
                assert.equal(stage[kind].probeRadiusM,approach.radiusM);
                assert(stage.playerEnvelopeClear && stage.routeLengthM===4.35);
            }
        }
        assert.equal(original.miss.contactTicks,0);
        assert.equal(resized.miss.contactTicks,0);
        const measuredDelta = original.hit.firstContactTick-resized.hit.firstContactTick;
        assert(measuredDelta>=3 && measuredDelta<=7);
        assert.equal(measuredDelta,metrics.resizedEarlierByTicks);
        assert.equal(metrics.resizedEarlierByMs,measuredDelta*approach.stepS*1000);
        assert((await page.locator('#exchange-comparison-verdict').textContent()).includes(String(measuredDelta)));
        assert.deepEqual(resolved.replay,replay.replay);
        assert((await fs.readFile(replayPath)).equals(replayBytes));
        assert.equal((await call('list')).scenes.length,0);
        assert((await fs.readFile(path.join(evidenceDir,id+'.json'))).equals(bytes));
        const comparisonBytes = await fs.readFile(comparisonPath);
        assert.deepEqual(JSON.parse(comparisonBytes),result);

        await app.close();
        ({app,page} = await launch(project,args));
        await viewer();
        await check('exchange-status','Saved local replay matches the archive');
        await openComparison();
        await check('exchange-comparison-status','Verified comparison saved locally');
        assert.deepEqual((await call('placement-exchange-latest')).colliderComparison,result);
        await capture('same-probe-after-restart.png');

        const corrupted = JSON.parse(comparisonBytes);
        corrupted.original.hit.samples[0].probeM[0] += .1;
        await fs.writeFile(comparisonPath,JSON.stringify(corrupted));
        await page.locator('#exchange-close').click();
        await page.locator('#placement-exchange').click();
        await openComparison();
        await check('exchange-comparison-status','saved comparison changed');
        const withheld = await call('placement-exchange-latest');
        assert(!withheld.colliderComparison && withheld.colliderComparisonUnavailable);
        assert.deepEqual(withheld.replay,replay.replay);
        await capture('changed-comparison-refused.png');
        await page.locator('#exchange-compare').click();
        await check('exchange-comparison-status','Verified comparison saved locally');
        assert.deepEqual((await call('placement-exchange-latest')).colliderComparison,result);
        assert((await fs.readFile(comparisonPath)).equals(comparisonBytes));

        const proof = {platform:process.platform,archiveSha256:id,comparisonSha256:result.sha256,
            hitStartM:approach.hitStartM,missStartM:approach.missStartM,velocityMps:approach.velocityMps,
            radiusM:approach.radiusM,stepS:approach.stepS,measuredTrials:approach.trialCount,
            adaptiveFirstContactTicks:[before.measured.hit.firstContactTick,after.measured.hit.firstContactTick],
            fixedFirstContactTicks:[original.hit.firstContactTick,resized.hit.firstContactTick],
            fixedHitContactTicks:[original.hit.contactTicks,resized.hit.contactTicks],
            fixedMissContactTicks:[original.miss.contactTicks,resized.miss.contactTicks],
            fixedPeakSpeedMps:[original.hit.maxPropPlanarSpeedMps,resized.hit.maxPropPlanarSpeedMps],
            resizedEarlierByMs:metrics.resizedEarlierByMs,aisleM:binding.aisleM,
            exactReplayRequired:true,noSceneOrApprovalCreated:true,retainedAfterRestart:true,
            changedComparisonRefusedAndRecovered:true,priorEvidenceUnchanged:true};
        await fs.writeFile(path.join(output,'fixed-approach-receipt.json'),comparisonBytes);
        await fs.writeFile(path.join(output,'fixed-approach-evidence.json'),JSON.stringify(proof,null,2)+'\n');
        console.log(JSON.stringify(proof));
    } finally {
        await app?.close().catch(()=>{});
    }
}
main().catch(error=>{console.error(error);process.exitCode=1;});
