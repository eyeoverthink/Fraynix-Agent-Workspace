
'use strict';

const assert = require('node:assert/strict');
const {createHash} = require('node:crypto');
const fs = require('node:fs/promises');
const os = require('node:os');
const path = require('node:path');
const {launch} = require('./calibration-integration.cjs');

const sha = value => createHash('sha256').update(value).digest('hex');
const frozen = path.join(__dirname,'fixtures','placement-exchange','edition232.fraynix-placement.json');

async function main() {
    process.env.FRAYNIX_WORKSPACE_NO_MODELS = '1';
    const output = path.resolve(process.argv[2] || 'build/visual-placement-exchange233');
    const project = await fs.mkdtemp(path.join(os.tmpdir(),'Fraynix exchange recipient '));
    await fs.mkdir(output,{recursive:true});
    const original = await fs.readFile(frozen);
    const id = sha(original);
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
        assert.equal(archive.archived.hit.firstContactTick,43);
        assert.equal(archive.archived.miss.contactTicks,0);
        assert.deepEqual(await call('placement-latest'),{});
        assert.equal((await call('list')).scenes.length,0);
        await render('archive-inspected.png');

        const bad = JSON.parse(original);
        bad.evidence.measured.hit.samples[2].probeM[0] += .25;
        const badFile = path.join(output,'tampered-placement.json');
        await fs.writeFile(badFile,JSON.stringify(bad));
        await page.locator('#exchange-file').setInputFiles(badFile);
        await status('collision evidence checksum differs');
        assert.equal((await call('placement-exchange-latest')).id,id);
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
        await render('local-hit-replay.png');

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
        await fs.writeFile(path.join(output,'exchange-evidence.json'),JSON.stringify(evidence,null,2)+'\n');
        console.log(JSON.stringify(evidence));
    } finally {
        await app?.close().catch(()=>{});
    }
}
main().catch(error=>{console.error(error);process.exitCode=1;});
