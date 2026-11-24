'use strict';

const assert = require('node:assert/strict');
const fs = require('node:fs/promises');
const os = require('node:os');
const path = require('node:path');
const {launch} = require('./calibration-integration.cjs');

async function main() {
    const output = path.resolve(process.argv[2] || 'build/hypothesis-experiment');
    const model = process.env.FRAYNIX_EXPERIMENT_MODEL || 'gpt-oss:20b';
    const jump = process.argv[3] === 'jump';
    const scenario = jump ? {
        a:'Jump is disabled. Pressing Jump in place for 12 ticks after settling leaves vertical displacement from -0.01 to 0.01 game units.',
        b:'Jump works. Pressing Jump in place for 12 ticks after settling raises the player by 0.05 to 3 game units.',
        probe:'jump-12', tick:36,
    } : {
        a:'The horizontal movement input is disabled. Holding Right for 24 ticks after settling leaves horizontal displacement within -0.01 to 0.01 game units.',
        b:'The horizontal movement input works. Holding Right for 24 ticks after settling moves the player right by 0.1 to 3 game units.',
        probe:'right-24', tick:48,
    };
    const project = await fs.mkdtemp(path.join(os.tmpdir(), 'Fraynix hypothesis '));
    await fs.mkdir(output,{recursive:true});
    process.env.FRAYNIX_WORKSPACE_PERSISTENT = '0';
    delete process.env.FRAYNIX_WORKSPACE_NO_MODELS;
    const args = process.platform==='linux' && process.env.FRAYNIX_WORKSPACE_CI_CHROMIUM==='1' ? ['--no-sandbox'] : [];
    const start = 'What observation would distinguish these two explanations?';
    const resume = 'Resume my last hypothesis experiment.';
    let {app,page} = await launch(project,args);
    const api = (route,input) => app.evaluate((_,[route,input])=>global.workspaceTest.api(route,input),[route,input]);
    const call = (command,input) => page.evaluate(([command,args])=>
        window.workspace.request({operation:'requestWorkbench',command,args}),[command,input]);
    const canvas = code => app.evaluate((_,code)=>global.workspaceTest.canvasRead(code),code);
    const dialog = () => page.locator('#hypothesis-dialog');
    async function say(request) {
        await page.locator('#goal').fill(request);
        await page.locator('#run').click();
    }
    async function capture(name) {
        await fs.writeFile(path.join(output,name),Buffer.from(await app.evaluate(()=>global.workspaceTest.capture()),'base64'));
    }
    async function restart() {
        await app.close();
        ({app,page} = await launch(project,args));
    }
    try {
        const initialGame = await api('/api/game/state',{});
        await page.waitForFunction(model=>Array.from(document.querySelector('#model').options).some(option=>option.value===model),
            model,{timeout:65000});
        await page.locator('#model').selectOption(model);
        await say(start);
        await dialog().waitFor();
        await page.locator('#hypothesis-a').fill(scenario.a);
        await page.locator('#hypothesis-b').fill(scenario.b);
        await page.locator('#hypothesis-propose').click();
        await page.locator('#hypothesis-propose').waitFor({state:'hidden',timeout:360000}).catch(async error=>{
            throw Error(error.message + ' ' + await page.locator('#hypothesis-status').innerText());
        });
        const {experiment:pending} = await api('/api/requests/hypothesis-intent',{request:resume});
        assert.equal(pending.status,'review');
        assert.equal(pending.review.proposal.probe,scenario.probe);
        assert.equal(pending.review.model,model);
        assert(pending.review.provider.requestSha256 && pending.review.provider.responseSha256 && pending.review.provider.modelDigest);
        assert.deepEqual(await api('/api/game/state',{}),initialGame);
        await assert.rejects(fs.access(path.join(project,'hypothesis-experiments','runtime')));
        await assert.rejects(api('/api/requests/hypothesis-approve',{id:pending.id,reviewSha256:'wrong'}),/exact saved experiment review/);
        await capture('review.png');

        await restart();
        await say(resume);
        await page.getByRole('button',{name:'Approve and run'}).waitFor();
        const retained = (await api('/api/requests/hypothesis-intent',{request:resume})).experiment;
        assert.deepEqual(retained,pending);
        await page.getByRole('button',{name:'Approve and run'}).click();
        const doneMessage = page.getByText('Game Lab measured',{exact:false}).last();
        await doneMessage.waitFor({timeout:90000}).catch(async error=>{
            throw Error(error.message + ' ' + await page.locator('#hypothesis-status').textContent());
        });
        const completed = (await api('/api/requests/hypothesis-intent',{request:resume})).experiment;
        assert.equal(completed.status,'complete');
        assert.equal(completed.result.a,'Conflicts with prediction');
        assert.equal(completed.result.b,'Supports prediction');
        assert.equal(await canvas('document.querySelector("#outcome-b").textContent'),'Supports prediction');
        assert.deepEqual(await api('/api/game/state',{}),initialGame);
        const artifact = await api('/api/artifact/read',{id:completed.artifact.id});
        assert.equal(artifact.test.passed,true);
        assert.equal(completed.browserChecks,6);
        const alteredBrowser = structuredClone(artifact.test.checks);
        alteredBrowser[2].actual.text = 'Altered verdict';
        await assert.rejects(api('/api/requests/hypothesis-browser-checks',{
            id:completed.id,version:completed.artifact.version,checks:alteredBrowser
        }),/Browser result differed/);
        const evidence = JSON.parse(artifact.contents['experiment-evidence.json']);
        assert.notEqual(evidence.original.sessionId,evidence.replay.sessionId);
        assert.equal(evidence.original.observed.tick,scenario.tick);
        assert.equal(evidence.replay.observed.tick,scenario.tick);
        assert.equal(await canvas('document.querySelector("#game-observed").naturalWidth'),960);
        await capture('result.png');
        for (const [file,data] of Object.entries(artifact.contents)) await fs.writeFile(path.join(output,file),data);

        await restart();
        const opened = (await call('hypothesis-chat',{request:resume})).experiment;
        assert.deepEqual(opened,completed);
        assert.equal(await canvas('document.querySelector("#outcome-a").textContent'),'Conflicts with prediction');
        const checkpoint = path.join(project,'hypothesis-experiments',pending.id+'.json');
        const untouched = await fs.readFile(checkpoint);
        try {
            const changed = JSON.parse(untouched);
            changed.data.original.observed.x = 999;
            await fs.writeFile(checkpoint,JSON.stringify(changed));
            await assert.rejects(call('hypothesis-chat',{request:resume}),/checkpoint integrity mismatch/);
        } finally {
            await fs.writeFile(checkpoint,untouched);
        }
        const proof = {passed:true,model,review:pending.review,result:completed.result,
            original:evidence.original,replay:evidence.replay,checks:[
                'One native local model call chooses the intended measurement.',
                'The original course, observation, intervals and budget are bound before approval.',
                'Incorrect approval is refused and pre-approval leaves Game Lab untouched.',
                'Review and completed result survive separate desktop restarts.',
                'One actual Game Lab run and one fresh runtime replay use the original player and planned tick.',
                'Both browser verdicts match the exact retained report.',
                'A modified native measurement checkpoint is refused.'
            ]};
        await fs.writeFile(path.join(output,'result.json'),JSON.stringify(proof,null,2)+'\n');
        console.log(JSON.stringify({passed:true,model,result:proof.result}));
    } finally {
        await app.close().catch(()=>{});
        await fs.copyFile(path.join(project,'test-desktop-process.log'),path.join(output,'desktop.log')).catch(()=>{});
    }
}
main().catch(error=>{console.error(error);process.exitCode=1;});
