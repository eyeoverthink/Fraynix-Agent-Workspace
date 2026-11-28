'use strict';

const assert = require('node:assert/strict');
const {createHash} = require('node:crypto');
const fs = require('node:fs/promises');
const os = require('node:os');
const path = require('node:path');
const {launch} = require('./calibration-integration.cjs');

const move = reference => 'Move the ' + reference + ' 40 cm away from the loading door. Keep the 1 m aisle clear and protect the shelves.';
const sha = bytes => createHash('sha256').update(bytes).digest('hex');

function checkRevision(parent, child, mission, yaw, crate, lateral, aisleLength) {
    const {review, proof} = mission;
    const measured = proof.measurements;
    assert.equal(review.parentId,parent.id);
    assert.equal(review.parentSha256,parent.sourceSha256);
    assert.equal(review.crate,crate);
    assert.equal(measured.forwardM,.4);
    assert.equal(measured.lateralM,lateral);
    assert.equal(measured.widthM,1);
    assert(Math.abs(measured.lengthM-aisleLength)<1e-8);
    assert.equal(measured.aisleSweepClear,true);
    assert.equal(measured.destinationClear,true);
    assert.equal(measured.sweptPathClear,true);
    assert.equal(measured.nativeAisleChecks,3);
    assert.equal(measured.crateSupportedTicks,60);
    assert.equal(measured.shelvesPreserved,2);
    assert.equal(measured.candidateSourceSha256,child.sourceSha256);
    assert.equal(proof.nativeRepeats,2);
    assert.deepEqual(review.preview,measured);
    const original = JSON.parse(parent.sourceText).objects;
    const revised = JSON.parse(child.sourceText).objects;
    const target = rows => rows.find(item => item.sourceId===crate);
    const delta = target(revised).position.map((coordinate,index) => coordinate-target(original).position[index]);
    const angle = yaw*Math.PI/180;
    assert(Math.abs(delta[0]-(.4*Math.sin(angle)+lateral*Math.cos(angle)))<1e-8);
    assert(Math.abs(delta[2]-(.4*Math.cos(angle)-lateral*Math.sin(angle)))<1e-8);
    assert.equal(delta[1],0);
    assert.deepEqual(revised.filter(item => item.sourceId!==crate),original.filter(item => item.sourceId!==crate));
    assert.deepEqual({...target(revised),position:target(original).position},target(original));
    assert.equal(Boolean(review.alternate),Boolean(lateral));
    return {yaw,crate,forwardM:measured.forwardM,lateralM:lateral,aisleLength,worldDeltaM:delta,
        directFailure:review.alternate?.direct || null, aisleSweepClear:true,shelvesPreserved:2,
        supportedTicks:60,sourceSha256:child.sourceSha256};
}

async function main() {
    process.env.FRAYNIX_UNIFIED_TEST = '1';
    process.env.FRAYNIX_WORKSPACE_NO_MODELS = '1';
    process.env.FRAYNIX_WORKSPACE_PERSISTENT = '0';
    const output = path.resolve(process.argv[2] || 'build/loading-aisle-alternates');
    await fs.mkdir(output,{recursive:true});
    const project = await fs.mkdtemp(path.join(os.tmpdir(),'Fraynix multi-crate aisle '));
    const flags = process.platform==='linux' ? ['--no-sandbox','--disable-dev-shm-usage'] : [];
    const errors = [];
    let app, page;
    const call = (command,args={}) => app.evaluate((_,v) => global.workspaceTest.api('/api/spatial/'+v.command,v.args),{command,args});
    const read = id => call('read',{id});
    const count = async () => (await call('list')).scenes.length;
    const status = text => page.locator('#aisle-status').filter({hasText:text}).waitFor({timeout:25000});
    const picture = name => capture(name+'.png');
    const approve = id => call('aisle-approve',{id,approvalSha256:id});
    const latest = () => call('aisle-latest');
    const body = (scene,sourceId) => scene.objects.find(item => item.sourceId===sourceId);

    async function open() {
        ({app,page} = await launch(project,flags));
        page.on('pageerror',error => errors.push(error.message));
        assert.equal((await app.evaluate(() => global.workspaceTest.network())).enabled,true);
        assert.equal((await app.evaluate(() => global.workspaceTest.api('/api/state'))).models.length,0);
        await ask('Open Spatial.');
        const panel = page.locator('#loading-aisle');
        await panel.waitFor();
        if (!await panel.evaluate(node => node.open)) await panel.locator('summary').click();
    }
    async function close() {
        if (!app) return;
        await app.close();
        app = null;
    }
    async function ask(text) {
        await page.locator('#goal').fill(text);
        await page.locator('#run').click();
    }
    async function load(variant) {
        await page.locator('#aisle-yaw').selectOption(variant);
        await page.locator('#aisle-load').click();
        await status('Choose the crate');
        return read(await page.locator('#spatial-scenes').inputValue());
    }
    async function capture(name) {
        await page.locator('#loading-aisle').evaluate(panel => {
            const view = document.querySelector('#spatial-view');
            view.scrollTop += panel.getBoundingClientRect().top-view.getBoundingClientRect().top-60;
        });
        await page.evaluate(() => new Promise(resolve => requestAnimationFrame(() => requestAnimationFrame(resolve))));
        const png = await app.evaluate(() => global.workspaceTest.capture());
        await fs.writeFile(path.join(output,name),Buffer.from(png,'base64'));
    }

    try {
        await open();
        const crossdock = await load('crossdock');
        assert.equal(await page.locator('#spatial-objects').inputValue(),body(crossdock,'cargo').id);
        await ask(move('green crate'));
        await status('Which crate should I move?');
        const duplicate = await latest();
        assert.deepEqual(duplicate.choices.map(item => item.sourceId),['green','green-extra']);
        await page.locator('#aisle-choices').getByRole('button',{name:'Green crate (green-extra)',exact:true}).waitFor();
        await ask('Green crate.');
        await status('Which crate should I move?');
        assert.deepEqual(await latest(),duplicate);
        await page.locator('#aisle-choices').getByRole('button',{name:'Green crate (green-extra)',exact:true}).click();
        await status('Review the 40 cm move');
        const buttonChoice = await latest();
        assert.equal(buttonChoice.review.crate,'green-extra');
        assert.equal(buttonChoice.review.intent.questionId,duplicate.id);
        assert.equal(await count(),1);
        await ask(move('crate'));
        await status('Blue crate · Red crate');
        const question = await latest();
        assert.equal(question.parentId,crossdock.id);
        assert.equal(question.parentSha256,crossdock.sourceSha256);
        assert.equal(question.choices.length,4);
        assert.equal(await page.locator('#aisle-approve').isVisible(),false);
        await assert.rejects(approve(question.id),/verified path/);
        await assert.rejects(call('aisle-choose',{id:duplicate.id,sceneId:crossdock.id,answer:'Green crate'}),/superseded/);
        await assert.rejects(call('aisle-choose',{id:question.id,sceneId:'another-bay',answer:'Blue crate'}),/named in/);
        assert.deepEqual(await call('aisle-choose',{id:question.id,sceneId:crossdock.id,objectId:'unknown'}),question);
        const questionFile = path.join(project,'spatial','loading-aisle',question.id+'.intent.json');
        const savedQuestion = await fs.readFile(questionFile);
        const tamperedQuestion = JSON.parse(savedQuestion);
        tamperedQuestion.data.choices[0].label = 'A different crate';
        await fs.writeFile(questionFile,JSON.stringify(tamperedQuestion));
        await assert.rejects(latest(),/integrity mismatch/);
        await fs.writeFile(questionFile,savedQuestion);
        await picture('which-crate');

        await close();
        await open();
        await page.locator('#aisle-resume').click();
        await status('Which crate should I move?');
        assert.deepEqual(await latest(),question);
        await ask('Use the blue crate.');
        await status('Safe alternate for Blue crate');
        const draft = await latest();
        assert.equal(draft.review.intent.questionId,question.id);
        assert.equal(draft.review.objectId,body(crossdock,'cargo').id);
        assert.equal(draft.review.alternate.direct.gate,'path');
        assert.match(draft.review.alternate.direct.reason,/Parked trolley/);
        assert.equal(draft.review.alternate.lateralM,.5);
        assert.equal(await count(),1);
        assert.deepEqual(await read(crossdock.id),crossdock);
        await assert.rejects(call('aisle-approve',{id:draft.id,approvalSha256:question.id}),/digest/);
        await assert.rejects(call('aisle-export',{id:draft.id}),/Verify/);
        if (await page.locator('#spatial-stage canvas').count()) await page.locator('#spatial-top-down').click();
        await picture('alternate-review');

        await close();
        await open();
        await page.locator('#aisle-resume').click();
        await status('Restored alternate');
        assert.deepEqual(await latest(),draft);
        assert.equal(await count(),1);
        await page.getByRole('button',{name:'Approve this alternate',exact:true}).click();
        await status('Verified alternate');
        const approved = await latest();
        const child = await read(approved.sceneId);
        const right = checkRevision(crossdock,child,approved,-29,'cargo',.5,3.4);
        assert.equal(await count(),2);
        assert.deepEqual(await read(crossdock.id),crossdock);

        await close();
        await open();
        await page.locator('#aisle-resume').click();
        await status('Verified alternate');
        await page.locator('#aisle-replay:enabled').click();
        await status('Offline replay matched');
        assert.deepEqual(await latest(),approved);
        assert.deepEqual(await read(approved.sceneId),child);
        await picture('alternate-restart');
        const evidence = Buffer.from((await call('aisle-export',{id:approved.id})).base64,'base64');
        await page.locator('#aisle-export:enabled').click();
        await status('Exported the checked scene');
        const exports = (await fs.readFile(path.join(project,'exports','manifest.jsonl'),'utf8')).trim().split('\n').map(JSON.parse);
        const saved = exports.find(item => item.kind==='verified-loading-aisle');
        assert.equal(saved.sha256,sha(evidence));
        assert((await fs.readFile(saved.path)).equals(evidence));
        await fs.writeFile(path.join(output,'alternate-verified.json'),evidence);
        const reviewPath = path.join(project,'spatial','loading-aisle',draft.id+'.review.json');
        const savedReview = await fs.readFile(reviewPath);
        const changed = JSON.parse(savedReview);
        changed.data.alternate.lateralM = -.5;
        await fs.writeFile(reviewPath,JSON.stringify(changed));
        await assert.rejects(call('aisle-replay',{id:draft.id}),/integrity mismatch/);
        await fs.writeFile(reviewPath,savedReview);

        const quayside = await load('quayside');
        assert.equal(await page.locator('#spatial-objects').inputValue(),body(quayside,'cargo').id);
        await ask(move('red crate'));
        await status('Safe alternate for Red crate');
        const leftDraft = await latest();
        assert.equal(leftDraft.review.objectId,body(quayside,'spare').id);
        const leftApproved = await approve(leftDraft.id);
        const left = checkRevision(quayside,await read(leftApproved.sceneId),leftApproved,68,'spare',-.5,4.2);
        assert.equal((await call('aisle-replay',{id:leftApproved.id})).replayed,true);
        await ask(move('blue crate'));
        await status('Review the 40 cm move');
        const directApproved = await approve((await latest()).id);
        const direct = checkRevision(quayside,await read(directApproved.sceneId),directApproved,68,'cargo',0,4.2);

        const refused = [];
        for (const [variant,gate,checked] of [['closed','initial-aisle',0],['pinched','path',8]]) {
            const parent = await load(variant);
            const before = await count();
            await ask(move('blue crate'));
            await status('I found no safe alternate for Blue crate');
            const decision = await latest();
            assert.equal(decision.status,'blocked');
            assert.equal(decision.direct.gate,gate);
            assert.equal(decision.alternatesConsidered,checked);
            assert.equal(await page.locator('#aisle-approve').isVisible(),false);
            await assert.rejects(approve(decision.id),/verified path/);
            await assert.rejects(call('aisle-export',{id:decision.id}),/Verify/);
            assert.equal(await count(),before);
            assert.deepEqual(await read(parent.id),parent);
            refused.push({variant,gate,checked,reason:decision.direct.reason});
        }
        assert.deepEqual(errors,[]);
        assert.equal((await app.evaluate(() => global.workspaceTest.network())).enabled,true);
        const summary = {passed:true,platform:process.platform,offlineRestarts:3,multipleCrates:question.choices.length,
            duplicateLabelsRequiredChoice:true,exactChoiceButtonWorked:true,staleAndChangedQuestionRefused:true,namedCrateOverridesHighlight:true,
            wrongApprovalAndChangedReviewRefused:true,alternateExportSha256:sha(evidence),right,left,direct,refused};
        await fs.writeFile(path.join(output,'result.json'),JSON.stringify(summary,null,2)+'\n');
        console.log(JSON.stringify(summary));
    } catch (error) {
        if (page) await fs.writeFile(path.join(output,'failure.txt'),await page.locator('body').innerText()).catch(()=>{});
        throw error;
    } finally {
        await close();
        await fs.rm(project,{recursive:true,force:true});
    }
}

main().catch(error => {console.error(error);process.exitCode=1;});
