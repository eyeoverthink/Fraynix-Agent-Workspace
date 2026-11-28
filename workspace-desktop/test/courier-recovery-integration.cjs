'use strict';

const assert = require('node:assert/strict');
const {createHash} = require('node:crypto');
const fs = require('node:fs/promises');
const os = require('node:os');
const path = require('node:path');
const {launch} = require('./calibration-integration.cjs');

const sha = bytes => createHash('sha256').update(bytes).digest('hex');

function audit(proof, setup) {
    assert.equal(proof.passed, true);
    const {native, measurements: m} = proof;
    assert.deepEqual(native.setup, setup);
    assert.deepEqual(native.baseline.rows[0], native.adaptive.rows[0]);
    assert.equal(native.baseline.playerId, native.adaptive.playerId);
    assert.equal(native.baseline.playerMass, native.adaptive.playerMass);
    const direction = setup.direction;
    for (const [key, trial] of Object.entries(m)) {
        const run = native[key];
        const {rows} = run;
        const startX = rows[0].crateX;
        assert.equal(run.playerType, 'DYNAMIC');
        assert.equal(run.crateType, 'DYNAMIC');
        assert(Math.abs(run.supportFriction - setup.supportFriction) < .00001);
        assert.equal(run.obstacle, setup.obstacle);
        assert.equal(run.obstacleType, setup.obstacle ? 'STATIC' : 'none');
        assert.equal(run.firstContactX, startX);
        assert.equal(trial.contactTick, rows.find(row => row.begins > 0).tick);
        assert.equal(trial.firstMotionTick, rows.find(row => Math.abs(row.crateX - startX) > .00025).tick);
        const crossing = rows.find(row => direction * (row.playerX - run.obstacleX) >= 0);
        if (setup.obstacle) {
            assert.equal(crossing.tick, trial.obstacleCrossingTick);
            assert.equal(crossing.playerY, trial.crossingY);
            assert(crossing.playerY > .759 && crossing.tick < trial.contactTick);
            assert(rows.slice(0, crossing.tick).some(row => row.jump));
        } else assert.equal(trial.obstacleCrossingTick, -1);
        if (setup.disturbance) {
            const kick = run.disturbance;
            const quiet = rows.filter(row => row.phase === 'OBSERVE');
            const stopped = quiet.at(-1);
            assert.equal(quiet.length, 60);
            assert.equal(kick.tick, quiet[0].tick);
            assert(kick.tick > trial.contactTick && direction * kick.impulse < 0);
            const delta = direction * (kick.speedAfter - kick.speedBefore);
            assert(Math.abs(delta + .26) < .00001);
            assert(quiet.every(row => row.input === 0 && row.contacts === 0 && row.impulse === 0));
            assert.equal(trial.stoppedShortBy, direction * (native.targetX - stopped.crateX));
            assert(trial.stoppedShortBy > .18 && Math.abs(stopped.crateVx) <= .008);
            if (key === 'adaptive') {
                const recontact = rows[trial.recoveryContactTick];
                assert(recontact.tick > stopped.tick && recontact.phase === 'RECOVER');
                assert(recontact.impulse > 0 && recontact.begins > stopped.begins);
            } else assert.equal(trial.outcome, 'stopped-short');
        } else assert.equal(run.disturbance, undefined);
        if (trial.holdTicks) {
            const released = rows[trial.releaseTick];
            const hold = rows.slice(trial.releaseTick + 1);
            assert.equal(hold.length, 60);
            assert(hold.every(row => row.input === 0 && row.impulse === 0 && row.contacts === 0 &&
                row.begins === released.begins));
            const error = Math.max(...[released, ...hold].map(row => Math.abs(row.crateX - native.targetX)));
            assert.equal(trial.maxHeldError, error);
            assert.equal(trial.delivered, error <= .035 && trial.drift <= .018 && trial.finalSpeed <= .008);
        }
    }
    assert.equal(m.adaptive.outcome, 'delivered');
    const expected = setup.disturbance ? 'stopped-short' :
        setup.supportFriction === .32 ? 'missed-hold' : 'delivered';
    assert.equal(m.baseline.outcome, expected);
}

async function main() {
    process.env.FRAYNIX_UNIFIED_TEST = '1';
    process.env.FRAYNIX_WORKSPACE_NO_MODELS = '1';
    process.env.FRAYNIX_WORKSPACE_PERSISTENT = '0';
    const output = path.resolve(process.argv[2] || 'build/courier-recovery');
    const project = await fs.mkdtemp(path.join(os.tmpdir(), 'Fraynix Courier recovery '));
    await fs.mkdir(output, {recursive: true});
    const flags = process.platform === 'linux' ? ['--no-sandbox', '--disable-dev-shm-usage'] : [];
    let app, page;
    const errors = [];
    const right = {direction: 1, supportFriction: .065, obstacle: true, disturbance: true};
    const left = {direction: -1, supportFriction: .32, obstacle: true, disturbance: true};
    const game = (command, args = {}) => app.evaluate(
        (_, input) => global.workspaceTest.api('/api/game/' + input.command, input.args), {command, args});
    const write = (name, value) => fs.writeFile(path.join(output, name), JSON.stringify(value, null, 2) + '\n');
    const status = text => page.locator('#courier-recovery-result').filter({hasText: text}).waitFor({timeout: 60_000});

    async function open() {
        ({app, page} = await launch(project, flags));
        page.on('pageerror', error => errors.push(error.message));
        assert.equal((await app.evaluate(() => global.workspaceTest.network())).enabled, true);
        assert.equal((await app.evaluate(() => global.workspaceTest.api('/api/state'))).models.length, 0);
    }
    async function close() {
        if (!app) return;
        await app.close();
        app = null;
    }
    async function enter() {
        await page.locator('#goal').fill('Open Game Lab.');
        await page.locator('#run').click();
        await page.locator('#courier-recovery summary').click({timeout: 40_000});
    }
    async function capture(proof, file) {
        await page.locator('#game-status').filter({hasText: 'tick ' + proof.measurements.adaptive.ticks + ' ·'}).waitFor();
        const frame = await game('frame');
        assert.equal(frame.state.courierCrate.targetX, proof.native.targetX);
        assert.equal(frame.state.courierCrate.crateX, proof.measurements.adaptive.finalX);
        assert.equal(frame.state.courierCrate.activeContacts, 0);
        assert.equal(sha(Buffer.from(frame.pngBase64, 'base64')), frame.pngSha256);
        await page.locator('#courier-recovery').evaluate(panel => {
            const view = document.querySelector('#game-view');
            view.scrollTop += panel.getBoundingClientRect().top - view.getBoundingClientRect().top - 40;
        });
        await page.evaluate(() => new Promise(resolve => requestAnimationFrame(() => requestAnimationFrame(resolve))));
        const image = await app.evaluate(() => global.workspaceTest.capture());
        await fs.writeFile(path.join(output, file), Buffer.from(image, 'base64'));
        return frame;
    }
    async function runForm(setup, file) {
        await page.locator('#courier-recovery-direction').selectOption(String(setup.direction));
        await page.locator('#courier-recovery-support').selectOption(String(setup.supportFriction));
        await page.locator('#courier-recovery-obstacle').setChecked(setup.obstacle);
        await page.locator('#courier-recovery-disturbance').setChecked(setup.disturbance);
        await page.locator('#courier-recovery-run:enabled').click();
        await status('Verified comparison');
        const proof = await game('courier-recovery-latest');
        audit(proof, setup);
        return {proof, frame: await capture(proof, file)};
    }

    try {
        await open();
        try {
            await game('open-relay');
        } catch (error) {
            if (process.platform !== 'darwin' || process.env.FRAYNIX_CI_MAC_DISPLAY !== '1' ||
                    !error.message.includes('Game Lab cannot get a macOS OpenGL 4.1 context from the current display.')) throw error;
            assert.equal((await game('state')).running, false);
            await write('result.json', {nativeGame: 'display-unavailable'});
            console.warn('Courier recovery needs a macOS OpenGL 4.1 display. This hosted run checked startup reporting.');
            return;
        }
        await enter();
        const rightRun = await runForm(right, 'courier-recovery-right.png');
        const matrix = [];
        for (const direction of [1, -1])
            for (const supportFriction of [.065, .32])
                for (const obstacle of [false, true])
                    for (const disturbance of [false, true]) {
                        const setup = {direction, supportFriction, obstacle, disturbance};
                        const proof = await game('courier-recovery-run', setup);
                        audit(proof, setup);
                        matrix.push({id: proof.id, setup, ...proof.measurements});
                    }
        assert.equal(new Set(matrix.map(row => row.id)).size, 16);
        const leftRun = await runForm(left, 'courier-recovery-left.png');
        await write('courier-recovery-matrix.json', matrix);
        await write('courier-recovery-left.json', leftRun.proof);
        await write('courier-recovery-right.json', rightRun.proof);
        await close();

        await open();
        await enter();
        await status('Saved comparison');
        assert.equal(await page.locator('#courier-recovery-direction').inputValue(), '-1');
        for (const record of matrix) {
            const check = await game('courier-recovery-read', {id: record.id});
            audit(check, record.setup);
        }
        await page.locator('#courier-recovery-replay:enabled').click();
        await status('Fresh replay matched');
        const leftReplay = await capture(leftRun.proof, 'courier-recovery-restart.png');
        assert.notEqual(leftReplay.state.sessionId, leftRun.frame.state.sessionId);
        assert.equal(leftReplay.pngSha256, leftRun.frame.pngSha256);

        const recovered = await game('courier-recovery-replay', {id: rightRun.proof.id});
        assert.equal(recovered.replayed, true);
        assert.deepEqual(recovered.native, rightRun.proof.native);
        const rightReplay = await game('frame');
        assert.notEqual(rightReplay.state.sessionId, rightRun.frame.state.sessionId);
        assert.equal(rightReplay.pngSha256, rightRun.frame.pngSha256);

        await page.locator('#courier-recovery-export:enabled').click();
        await status('Exported comparison');
        const manifest = await fs.readFile(path.join(project, 'exports', 'manifest.jsonl'), 'utf8');
        const receipt = manifest.trim().split('\n').map(JSON.parse).find(entry => entry.kind === 'courier-delivery-recovery');
        const exported = await fs.readFile(receipt.path);
        assert.equal(sha(exported), receipt.sha256);
        assert.deepEqual(JSON.parse(exported), leftRun.proof);

        const savedFile = path.join(project, 'courier-recovery', leftRun.proof.id + '.json');
        const altered = JSON.parse(await fs.readFile(savedFile, 'utf8'));
        altered.native.adaptive.rows[altered.measurements.adaptive.releaseTick + 12].crateX += .08;
        const forged = Buffer.from(JSON.stringify(altered));
        const id = sha(forged);
        await fs.writeFile(path.join(project, 'courier-recovery', id + '.json'), forged, {flag: 'wx'});
        await assert.rejects(game('courier-recovery-read', {id}), /did not stay on target/);
        await fs.appendFile(savedFile, '\n');
        await assert.rejects(game('courier-recovery-replay', {id: leftRun.proof.id}), /evidence changed/);
        assert.equal((await game('frame')).state.sessionId, rightReplay.state.sessionId);
        assert.deepEqual(errors, []);

        const result = {passed: true, platform: process.platform, pairedCases: matrix.length,
            adaptiveDelivered: matrix.filter(row => row.adaptive.delivered).length,
            baselineDelivered: matrix.filter(row => row.baseline.delivered).length,
            baselineStoppedShort: matrix.filter(row => row.baseline.outcome === 'stopped-short').length,
            baselineMissedHold: matrix.filter(row => row.baseline.outcome === 'missed-hold').length,
            maxHeldError: Math.max(...matrix.map(row => row.adaptive.maxHeldError)),
            maxDrift: Math.max(...matrix.map(row => row.adaptive.drift)),
            offlineRestart: true, allReceiptsRechecked: true, twoNativeComparisonsReplayed: true,
            tamperRefused: true, failedHoldRejected: true, exportVerified: true,
            left: {id: leftRun.proof.id, frameSha256: leftReplay.pngSha256},
            right: {id: rightRun.proof.id, frameSha256: rightReplay.pngSha256}};
        await write('result.json', result);
        console.log(JSON.stringify(result));
    } catch (error) {
        if (page) await fs.writeFile(path.join(output, 'failure.txt'), await page.locator('body').innerText()).catch(() => {});
        throw error;
    } finally {
        await close();
        await fs.rm(project, {recursive: true, force: true});
    }
}

main().catch(error => { console.error(error); process.exitCode = 1; });
