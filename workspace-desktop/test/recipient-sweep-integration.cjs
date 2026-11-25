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
    const out = path.resolve(process.argv[2] || 'build/recipient-sweep243');
    const archive = path.resolve(process.argv[3] || 'docs/readmes/241/signal-bay-collider.fraynix-recipient.json');
    const bytes = await fs.readFile(archive);
    const id = sha(bytes);
    const project = await fs.mkdtemp(path.join(os.tmpdir(),'Fraynix probe sweep '));
    const directory = path.join(project,'spatial','placement-exchanges');
    const sweepFile = path.join(directory,id+'.sweep.json');
    const comparisonFile = path.join(directory,id+'.comparison.json');
    const replayFile = path.join(directory,id+'.replay.json');
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
    const viewer = async () => {
        await page.locator('#spatial-tab').click();
        await page.locator('#placement-exchange').click();
        await page.locator('#placement-exchange-dialog[open]').waitFor();
    };
    const selectSweep = async () => {
        await page.locator('#exchange-trial').selectOption('sweep');
        await page.getByRole('button',{name:'Run five approaches',exact:true}).waitFor();
    };
    try {
        await viewer();
        await page.waitForFunction(()=>!document.querySelector('#exchange-file').disabled);
        await page.locator('#exchange-file').setInputFiles(archive);
        await check('exchange-status','Bundle and GLB checked');
        const run = replaySha256 => call('placement-exchange-sweep',{id,replaySha256});
        await assert.rejects(run('0'.repeat(64)),/Replay the exact bundle locally/i);
        await assert.rejects(fs.access(sweepFile),{code:'ENOENT'});
        await page.locator('#exchange-replay').click();
        await check('exchange-status','Local physics replay matched');
        const initial = await call('placement-exchange-latest');
        const replayHash = initial.replay.sha256;
        const replayBytes = await fs.readFile(replayFile);
        await assert.rejects(run('0'.repeat(64)),/Replay the exact bundle locally/i);
        const fixed = await call('placement-exchange-compare',{id,replaySha256:replayHash});
        const fixedBytes = await fs.readFile(comparisonFile);
        await selectSweep();
        await page.locator('#exchange-compare').click();
        await check('exchange-sweep-summary','40 trials');
        const latest = await call('placement-exchange-latest');
        const sweep = latest.colliderSweep;
        assert.deepEqual(latest.colliderComparison,fixed);
        assert.equal(sweep.binding.pair.replaySha256,replayHash);
        assert.equal(sweep.binding.pair.archiveSha256,id);
        const definitions = sweep.binding.cases;
        assert.deepEqual(definitions.map(x=>[x.headingDeg,x.lateralOffsetM]),
            [[0,0],[-25,0],[25,0],[0,-.12],[0,.12]]);
        assert.equal(sweep.cases.length,5);
        assert.equal(sweep.summary.sphereTrials,40);
        assert.deepEqual(sweep.cases[0],{original:fixed.original,resized:fixed.resized,result:fixed.result});
        const measured = [];
        for (const [i,definition] of definitions.entries()) {
            const {headingDeg,lateralOffsetM,approach} = definition;
            const data = sweep.cases[i];
            const [vx,,vz]=approach.velocityMps;
            assert(Math.abs(Math.atan2(vz,vx)*180/Math.PI-headingDeg)<1e-5);
            assert(Math.abs(Math.hypot(vx,vz)-1.8)<1e-6);
            const [tx,,tz] = sweep.binding.pair.targetM;
            const [hx,,hz] = approach.hitStartM;
            assert(Math.abs((-(hx-tx)*vz+(hz-tz)*vx)/Math.hypot(vx,vz)-lateralOffsetM)<1e-6);
            for (const collider of [data.original,data.resized]) {
                assert(collider.passed && collider.playerEnvelopeClear);
                assert.equal(collider.routeLengthM,4.35);
                assert.equal(collider.miss.contactTicks,0);
                for (const code of ['hit','miss']) {
                    const sample = collider[code].samples[0];
                    assert.deepEqual(sample.probeM,approach[code+'StartM']);
                    assert.deepEqual(sample.probeVelocityMps,approach.velocityMps);
                    assert.equal(collider[code].probeRadiusM,approach.radiusM);
                }
            }
            const startTicks = [data.original.hit.firstContactTick,data.resized.hit.firstContactTick];
            const leadTicks = startTicks[0]-startTicks[1];
            assert(leadTicks>=2 && leadTicks<=6);
            assert.equal(data.result.resizedEarlierByTicks,leadTicks);
            measured.push({headingDeg,lateralOffsetM,startTicks,
                hitContactTicks:[data.original.hit.contactTicks,data.resized.hit.contactTicks],leadTicks});
        }
        assert.equal(sweep.summary.earlier,5);
        assert.equal(sweep.summary.minLeadTicks,Math.min(...measured.map(x=>x.leadTicks)));
        assert.equal(sweep.summary.maxLeadTicks,Math.max(...measured.map(x=>x.leadTicks)));
        assert.equal(await page.locator('#exchange-sweep-table tbody tr').count(),5);
        await page.locator('#exchange-sweep-case').selectOption('2');
        await check('exchange-comparison-verdict',String(measured[2].leadTicks)+' ticks');
        await capture('five-probe-approaches.png');
        const saved = await fs.readFile(sweepFile);

        await app.close();
        ({app,page} = await launch(project,args));
        await viewer();
        await check('exchange-status','Saved local replay matches');
        await selectSweep();
        await check('exchange-sweep-summary','40 trials');
        assert.deepEqual((await call('placement-exchange-latest')).colliderSweep,sweep);
        await page.locator('#exchange-sweep-case').selectOption('3');
        await check('exchange-comparison-verdict',String(measured[3].leadTicks)+' ticks');
        await page.locator('#exchange-comparison-launch').scrollIntoViewIfNeeded();
        await capture('offset-approach-after-restart.png');
        const damaged = JSON.parse(saved);
        [damaged.cases[1],damaged.cases[2]]=[damaged.cases[2],damaged.cases[1]];
        await fs.writeFile(sweepFile,JSON.stringify(damaged));
        await page.locator('#exchange-close').click();
        await page.locator('#placement-exchange').click();
        await selectSweep();
        await check('exchange-comparison-status','saved comparison changed');
        assert(await page.locator('#exchange-sweep-overview').isHidden());
        const retained = await call('placement-exchange-latest');
        assert(retained.colliderSweepUnavailable && !retained.colliderSweep);
        assert.deepEqual(retained.colliderComparison,fixed);
        await capture('altered-approach-refused.png');
        await page.locator('#exchange-compare').click();
        await check('exchange-sweep-summary','40 trials');
        await check('exchange-comparison-status','Verified comparison saved locally');
        assert((await fs.readFile(sweepFile)).equals(saved));
        assert((await fs.readFile(comparisonFile)).equals(fixedBytes));
        assert((await fs.readFile(replayFile)).equals(replayBytes));
        assert((await fs.readFile(path.join(directory,id+'.json'))).equals(bytes));
        assert.equal((await call('list')).scenes.length,0);
        const proof = {platform:process.platform,archiveSha256:id,sweepSha256:sweep.sha256,
            summary:sweep.summary,measured,aisleM:4.35,missContacts:0,retainedAfterRestart:true,
            reorderedCasesRefusedAndRecreated:true,originalEvidenceAndSingleResultUnchanged:true};
        await fs.writeFile(path.join(out,'approach-sweep-receipt.json'),saved);
        await fs.writeFile(path.join(out,'approach-sweep-evidence.json'),JSON.stringify(proof,null,2)+'\n');
        console.log(JSON.stringify(proof));
    } finally {
        await app?.close().catch(()=>{});
    }
}
main().catch(error=>{console.error(error);process.exitCode=1;});
