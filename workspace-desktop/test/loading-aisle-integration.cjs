'use strict';

const assert = require('node:assert/strict');
const {createHash} = require('node:crypto');
const fs = require('node:fs/promises');
const os = require('node:os');
const path = require('node:path');
const {launch} = require('./calibration-integration.cjs');

const request = 'Move the selected crate 40 cm away from the loading door. Keep the 1 m aisle clear and leave the shelves alone.';
const sha = bytes => createHash('sha256').update(bytes).digest('hex');

function audit(parent, child, mission, yaw) {
    const {review, proof} = mission;
    const m = proof.measurements;
    assert.equal(review.request, request);
    assert.equal(review.parentId, parent.id);
    assert.equal(review.parentSha256, parent.sourceSha256);
    assert.equal(review.crate, 'cargo');
    assert.deepEqual(review.protected.map(item => item.sourceId), ['shelf-east','shelf-west']);
    assert.equal(m.translationM, .4);
    assert.equal(m.widthM, 1);
    assert(Math.abs(m.lengthM-3.6) < 1e-8);
    assert.equal(m.heightM, 1.8);
    assert.equal(m.shelvesPreserved, 2);
    assert.equal(m.crateSupportedTicks, 60);
    assert.equal(m.nativeAisleChecks, 3);
    assert.equal(proof.nativeRepeats, 2);
    assert.equal(m.sweptPathClear, true);
    assert.equal(m.candidateSourceSha256, child.sourceSha256);
    const source = JSON.parse(parent.sourceText);
    const candidate = JSON.parse(child.sourceText);
    const before = source.objects.find(item => item.sourceId === 'cargo').position;
    const after = candidate.objects.find(item => item.sourceId === 'cargo').position;
    const delta = after.map((value,i) => value - before[i]);
    assert(Math.abs(Math.hypot(...delta) - .40) < 1e-8);
    assert(Math.abs(delta[0] - .40*Math.sin(yaw*Math.PI/180)) < 1e-8);
    assert(Math.abs(delta[2] - .40*Math.cos(yaw*Math.PI/180)) < 1e-8);
    assert.equal(delta[1],0);
    for (const original of source.objects.filter(item => item.sourceId !== 'cargo'))
        assert.deepEqual(candidate.objects.find(item => item.sourceId === original.sourceId), original);
    return delta;
}

async function main() {
    process.env.FRAYNIX_UNIFIED_TEST = '1';
    process.env.FRAYNIX_WORKSPACE_NO_MODELS = '1';
    process.env.FRAYNIX_WORKSPACE_PERSISTENT = '0';
    const output = path.resolve(process.argv[2] || 'build/loading-aisle');
    const project = await fs.mkdtemp(path.join(os.tmpdir(),'Fraynix loading aisle '));
    await fs.mkdir(output,{recursive:true});
    const flags = process.platform === 'linux' ? ['--no-sandbox','--disable-dev-shm-usage'] : [];
    const withoutWebGL = process.env.FRAYNIX_AISLE_NO_WEBGL === '1';
    let app, page;
    const errors = [];
    const call = (command,args={}) => app.evaluate((_, v) => global.workspaceTest.api('/api/spatial/'+v.command,v.args),{command,args});
    const write = (name,value) => fs.writeFile(path.join(output,name),JSON.stringify(value,null,2)+'\n');
    const status = text => page.locator('#aisle-status').filter({hasText:text}).waitFor({timeout: 25000});
    const body = scene => scene.objects.find(item => item.sourceId === 'cargo');
    const read = id => call('read',{id});
    const count = async () => (await call('list')).scenes.length;
    const review = (scene, requestText=request) => call('aisle-review',{sceneId:scene.id,objectId:body(scene).id,request:requestText});
    const approve = id => call('aisle-approve',{id,approvalSha256:id});
    async function ask(text) {
        await page.locator('#goal').fill(text);
        await page.locator('#run').click();
    }
    async function open() {
        ({app,page} = await launch(project,flags));
        if (withoutWebGL) {
            await page.addInitScript(() => {
                const getContext = HTMLCanvasElement.prototype.getContext;
                HTMLCanvasElement.prototype.getContext = function (type, ...options) {
                    return /webgl/i.test(type) ? null : getContext.call(this,type,...options);
                };
            });
            await page.reload();
        }
        page.on('pageerror',error => errors.push(error.message));
        assert.equal((await app.evaluate(() => global.workspaceTest.network())).enabled,true);
        assert.equal((await app.evaluate(() => global.workspaceTest.api('/api/state'))).models.length,0);
    }
    async function close() {
        if (!app) return;
        await app.close();
        app = null;
    }
    async function panel() {
        await ask('Open Spatial.');
        const panel = page.locator('#loading-aisle');
        await panel.waitFor();
        if (!await panel.evaluate(node => node.open)) await panel.locator('summary').click();
    }
    async function capture(name) {
        await page.locator('#loading-aisle').evaluate(panel => {
            const view = document.querySelector('#spatial-view');
            view.scrollTop += panel.getBoundingClientRect().top - view.getBoundingClientRect().top - 70;
        });
        await page.evaluate(() => new Promise(resolve => requestAnimationFrame(() => requestAnimationFrame(resolve))));
        const image = await app.evaluate(() => global.workspaceTest.capture());
        await fs.writeFile(path.join(output,name),Buffer.from(image,'base64'));
    }
    async function fixture(yaw,obstruction) {
        const source = await call('aisle-fixture',{yaw,obstruction});
        return call('import',{text:JSON.stringify(source)});
    }
    try {
        await open();
        await panel();
        await page.locator('#aisle-yaw').selectOption('37');
        await page.locator('#aisle-load').click();
        await status('Choose the crate');
        const parentId = await page.locator('#spatial-scenes').inputValue();
        const parent = await read(parentId);
        assert.equal(parent.title,'Loading Bay · 37°');
        await page.locator('#spatial-objects').selectOption(body(parent).id);
        await ask(request);
        await status('Review the 40 cm move');
        const draft = await call('aisle-latest');
        assert.equal(draft.status,'review');
        assert.equal(draft.review.objectId,body(parent).id);
        assert.equal(await count(),1);
        assert.deepEqual(await read(parentId),parent);
        await assert.rejects(call('aisle-approve',{id:draft.id,approvalSha256:'0'.repeat(64)}),/digest/);
        await assert.rejects(call('aisle-replay',{id:draft.id}),/Approve and verify/);
        await assert.rejects(call('aisle-export',{id:draft.id}),/Verify/);
        await assert.rejects(review(parent,request+' Move the East shelf.'),/permits only/);
        assert.equal(await count(),1);
        if (withoutWebGL) {
            await page.locator('#aisle-plan').waitFor();
            assert.equal(await page.locator('#spatial-stage canvas').count(),0);
        }
        else if (await page.locator('#spatial-stage canvas').count()) await page.locator('#spatial-top-down').click();
        await capture('aisle-proposal.png');

        await close();
        await open();
        await panel();
        await page.locator('#aisle-resume').click();
        await status('Restored pending review');
        assert.deepEqual(await call('aisle-latest'),draft);
        assert.equal(await count(),1);
        await page.locator('#aisle-approve:enabled').click();
        await status('Verified · 40 cm away from door');
        const approved = await call('aisle-latest');
        const child = await read(approved.sceneId);
        const deltaM = audit(parent,child,approved,37);
        assert.equal(await count(),2);
        assert.deepEqual(await read(parentId),parent);
        assert.deepEqual(await approve(draft.id),approved);
        assert.equal(await count(),2);
        await capture('aisle-approved.png');

        await close();
        await open();
        await panel();
        await page.locator('#aisle-resume').click();
        await status('Verified · 40 cm away from door');
        await page.locator('#aisle-replay:enabled').click();
        await status('Offline replay matched');
        assert.deepEqual(await call('aisle-read',{id:approved.id}),approved);
        assert.deepEqual(await read(approved.sceneId),child);
        await capture('aisle-restart.png');

        const expected = await call('aisle-export',{id:approved.id});
        const expectedBytes = Buffer.from(expected.base64,'base64');
        await page.locator('#aisle-export:enabled').click();
        await status('Exported the checked scene');
        const manifest = await fs.readFile(path.join(project,'exports','manifest.jsonl'),'utf8');
        const exportedRecord = manifest.trim().split('\n').map(JSON.parse).find(item => item.kind === 'verified-loading-aisle');
        const exported = await fs.readFile(exportedRecord.path);
        assert(exported.equals(expectedBytes));
        assert.equal(sha(exported),exportedRecord.sha256);
        await fs.writeFile(path.join(output,'aisle-verified.json'),exported);

        const proofFile = path.join(project,'spatial','loading-aisle',draft.id+'.proof.json');
        const saved = await fs.readFile(proofFile);
        const changed = JSON.parse(saved);
        changed.data.measurements.shelvesPreserved = 0;
        await fs.writeFile(proofFile,JSON.stringify(changed));
        await assert.rejects(call('aisle-replay',{id:draft.id}),/integrity mismatch/);
        await fs.writeFile(proofFile,saved);

        const square = await fixture(0,'none');
        const squareApproval = await approve((await review(square)).id);
        audit(square,await read(squareApproval.sceneId),squareApproval,0);
        const refusals = [];
        for (const [kind,reason] of [['aisle','aisle is blocked'],['path','crate path is blocked']]) {
            const blocked = await fixture(37,kind);
            const pending = await review(blocked);
            const savedScenes = await count();
            const diagnostic = new RegExp(reason+' by Parked trolley');
            await assert.rejects(approve(pending.id),diagnostic);
            await assert.rejects(call('aisle-export',{id:pending.id}),/Verify/);
            assert.equal(await count(),savedScenes);
            assert.deepEqual(await read(blocked.id),blocked);
            refusals.push(kind);
        }
        assert.deepEqual(errors,[]);
        assert.equal((await app.evaluate(() => global.workspaceTest.network())).enabled,true);
        const summary = {passed:true,platform:process.platform,yaw:37,deltaM,approveAfterRestart:true,
            verifiedAfterSecondRestart:true,protectedTwoShelves:true,otherBodiesUnchanged:true,
            invalidApprovalRefused:true,unapprovedChangesRefused:true,changedEvidenceRefused:true,
            obstructionRefusals:refusals,squareControl:true,webglFallback:withoutWebGL,
            evidenceSha256:sha(exported),measurements:approved.proof.measurements};
        await write('result.json',summary);
        console.log(JSON.stringify(summary));
    } catch(error) {
        if(page) await fs.writeFile(path.join(output,'failure.txt'),await page.locator('body').innerText()).catch(()=>{});
        throw error;
    } finally {
        await close();
        await fs.rm(project,{recursive:true,force:true});
    }
}
main().catch(error=>{console.error(error);process.exitCode=1;});
