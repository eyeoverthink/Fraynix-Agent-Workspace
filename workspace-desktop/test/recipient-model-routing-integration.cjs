'use strict';

const assert = require('node:assert/strict');
const {createHash} = require('node:crypto');
const fs = require('node:fs/promises');
const path = require('node:path');
const os = require('node:os');
const {launch} = require('./calibration-integration.cjs');

const sha = bytes => createHash('sha256').update(bytes).digest('hex');
const clear = 'Can you check where the new box makes contact when it comes in from an angle, before I commit?';
const ambiguous = 'Try it.';
const answer = 'Yes, compare the original and proposed collision boxes across the five approaches.';
const question = 'Do you want to compare the saved collider proposal across five approaches before approval?';

async function main() {
    delete process.env.FRAYNIX_WORKSPACE_NO_MODELS;
    process.env.FRAYNIX_WORKSPACE_PLAN_CPU_ONLY ||= '1';
    const model = process.env.FRAYNIX_TEST_MODEL || 'gpt-oss:20b';
    const out = path.resolve(process.argv[2] || 'build/recipient-model-routing246');
    const fixture = path.resolve(process.argv[3] || 'docs/readmes/245/raised-switchyard.fraynix-level.json');
    const bytes = await fs.readFile(fixture);
    const project = await fs.mkdtemp(path.join(os.tmpdir(),'Fraynix collider model '));
    const args = process.platform==='linux' ? ['--no-sandbox','--disable-dev-shm-usage'] : [];
    await fs.mkdir(out,{recursive:true});
    let {app,page} = await launch(project,args);
    const call = (route,input={}) => app.evaluate((_,request) =>
        global.workspaceTest.api('/api/spatial/'+request.route,request.input),{route,input});
    const check = (id,value,timeout=120000) => page.waitForFunction(({id,value}) => {
        const error = [...document.querySelectorAll('#messages .assistant')].find(message =>
            message.textContent.startsWith('I could not complete the collider comparison.'));
        if (error) throw Error(error.textContent);
        return document.getElementById(id)?.textContent.includes(value);
    },{id,value},{timeout});
    const capture = async name => {
        await page.evaluate(()=>new Promise(resolve=>requestAnimationFrame(()=>requestAnimationFrame(resolve))));
        await fs.writeFile(path.join(out,name),Buffer.from(await app.evaluate(()=>global.workspaceTest.capture()),'base64'));
    };
    const chooseModel = async () => {
        await page.waitForFunction(model=>[...document.querySelector('#model').options].some(option=>option.value===model),
            model,{timeout:60000});
        await page.locator('#model').selectOption(model);
    };
    const submit = async text => {
        await page.locator('#goal').fill(text);
        await page.locator('#run').click();
    };
    const preview = async (width,depth) => {
        await page.locator('#recipient-edit-form select[name=kind]').selectOption('collider');
        await page.locator('#recipient-edit-form input[name=colliderWidthM]').fill(width);
        await page.locator('#recipient-edit-form input[name=colliderDepthM]').fill(depth);
        await page.getByRole('button',{name:'Preview local edit',exact:true}).click();
        await check('recipient-edit-status','Approve the exact collider');
        await page.locator('#recipient-edit-close').click();
    };
    const restart = async () => {
        await app.close();
        ({app,page} = await launch(project,args));
        await chooseModel();
    };
    const verifyModel = (route,choice) => {
        assert.equal(route.modelChoice,choice);
        assert.equal(route.model.name,model);
        assert.match(route.model.digest,/^(sha256:)?[a-f0-9]{64}$/);
        assert.equal(route.model.digest,route.model.provider.modelDigest);
        assert.equal(route.model.provider.transport,'ollama-http-ndjson');
        assert.equal(route.model.provider.requestedModel,model);
        assert.match(route.model.provider.responseSha256,/^[a-f0-9]{64}$/);
        assert.match(route.model.provider.endpoint,/^http:\/\/(?:127\.0\.0\.1|localhost|\[::1\])(?::\d+)?$/);
    };

    try {
        await chooseModel();
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
        const parent = await call('read',{id:sceneId});
        await preview('0.54','0.435');

        const first = await call('recipient-edit-latest',{sceneId});
        await submit(clear);
        await check('recipient-approach-status','Saved comparison for this exact preview',180000);
        const direct = await call('recipient-route-latest');
        verifyModel(direct,'compare');
        assert.equal(direct.request,clear);
        assert.equal(direct.target.reviewId,first.reviewId);
        const draft = await call('recipient-mission-latest');
        assert.deepEqual(draft.routing,direct);
        assert.equal(draft.comparison.cases.length,5);
        assert(!draft.approval && !draft.revision && !draft.export);
        await capture('plain-language-collider.png');

        await page.locator('#recipient-edit-close').click();
        await page.locator('#recipient-edit').click();
        await preview('0.55','0.44');
        const pending = await call('recipient-edit-latest',{sceneId});
        assert.notEqual(pending.reviewId,first.reviewId);
        await submit(ambiguous);
        await check('messages',question,180000);
        const clarification = await call('recipient-route-latest');
        verifyModel(clarification,'clarify');
        assert.equal(clarification.request,ambiguous);
        assert.equal(clarification.question,question);
        assert.equal(clarification.target.reviewId,pending.reviewId);
        assert.equal((await call('recipient-mission-latest')).reviewId,first.reviewId);
        assert.deepEqual(await call('read',{id:sceneId}),parent);
        assert.equal((await call('list')).scenes.length,1);
        await capture('collider-clarification.png');

        await restart();
        await check('messages',question);
        assert.deepEqual(await call('recipient-route-latest'),clarification);
        await submit(answer);
        await check('recipient-approach-status','Saved comparison for this exact preview',180000);
        const resumed = await call('recipient-route-latest');
        verifyModel(resumed,'compare');
        assert.equal(resumed.originalRequest,ambiguous);
        assert.equal(resumed.request,answer);
        assert.equal(resumed.clarificationOf,clarification.id);
        assert.equal(resumed.priorQuestion,question);
        assert.deepEqual(resumed.priorModel,clarification.model);
        assert.equal(resumed.target.reviewId,pending.reviewId);
        assert.equal(resumed.model.digest,direct.model.digest);
        const verifiedDraft = await call('recipient-mission-latest');
        assert.equal(verifiedDraft.request,ambiguous);
        assert.deepEqual(verifiedDraft.routing,resumed);
        assert.equal(verifiedDraft.comparison.binding.pair.reviewId,pending.reviewId);
        assert.equal(verifiedDraft.comparison.cases.length,5);
        assert(!verifiedDraft.approval && !verifiedDraft.revision && !verifiedDraft.export);
        assert.deepEqual(await call('read',{id:sceneId}),parent);
        await assert.rejects(call('recipient-edit-run',{reviewId:pending.reviewId}),/Approve this exact local edit/i);
        await assert.rejects(call('recipient-mission-export',{reviewId:pending.reviewId}),/Approve the verified comparison/i);

        await page.locator('#recipient-edit-approve').click();
        await check('recipient-edit-status','Local revision verified');
        await page.locator('#recipient-edit-close').click();
        await page.locator('#recipient-mission').click();
        await page.getByRole('button',{name:'Save verified export',exact:true}).click();
        await check('recipient-mission-export-status','Verified · exports/');
        const completed = await call('recipient-mission-latest');
        assert.deepEqual(completed.routing,resumed);
        assert.equal(completed.approvalConfirmed,true);
        assert.equal(completed.approval.comparisonSha256,verifiedDraft.comparison.sha256);
        assert.equal(completed.revision.receipt.measured.miss.contactTicks,0);
        assert(completed.revision.receipt.measured.playerEnvelopeClear);
        assert.deepEqual(completed.revision.receipt.measured,completed.revision.receipt.repeat);
        const exported = await fs.readFile(path.join(project,'exports',completed.export.fileName));
        assert.equal(sha(exported),completed.export.sha256);
        assert(Buffer.from(JSON.parse(exported).originalArchiveBase64,'base64').equals(bytes));

        await restart();
        await page.locator('#spatial-tab').click();
        await page.locator('#recipient-mission').click();
        await check('recipient-mission-export-status','Verified · exports/');
        assert.deepEqual(await call('recipient-mission-latest'),completed);
        await check('recipient-mission-routing',model);
        await capture('routed-mission-after-restart.png');

        const proof = {platform:process.platform,scene:completed.sceneTitle,inputSha256:sha(bytes),
            direct,clarification,resumed,approaches:completed.comparison.cases.length,
            comparisonSha256:completed.comparison.sha256,exportSha256:completed.export.sha256,
            explicitApprovalRequired:true,clarificationRestored:true,exportedMissionRestored:true};
        await fs.writeFile(path.join(out,'model-routing-proof.json'),JSON.stringify(proof,null,2)+'\n');
        await fs.writeFile(path.join(out,'collider-mission.json'),JSON.stringify(completed,null,2)+'\n');
        await fs.writeFile(path.join(out,'raised-switchyard-model-reviewed.fraynix-recipient.json'),exported);
        console.log(JSON.stringify(proof));
    } catch (error) {
        await capture('failed.png').catch(()=>{});
        console.error('Project evidence saved locally at',project);
        throw error;
    } finally {
        await app?.close().catch(()=>{});
    }
}
main().catch(error=>{console.error(error);process.exitCode=1;});
