'use strict';

const assert = require('node:assert/strict');
const {createHash} = require('node:crypto');
const fs = require('node:fs/promises');
const os = require('node:os');
const path = require('node:path');
const {launch} = require('./calibration-integration.cjs');

const digest = bytes => createHash('sha256').update(bytes).digest('hex');

function audit(proof, setup) {
    assert.equal(proof.passed, true);
    const {native, measurements: m} = proof;
    assert.deepEqual(native.request, setup);
    assert.equal(native.playerType, 'DYNAMIC');
    assert.equal(native.crateType, 'DYNAMIC');
    assert(native.playerMass > 0 && native.playerFixedRotation);
    assert(Math.abs(native.actualMass - setup.mass) < 0.00001);
    assert(Math.abs(native.actualFriction - setup.friction) < 0.00001);
    const rows = native.rows;
    const startX = rows[0].crateX;
    const direction = Math.sign(setup.distance);
    assert(direction * (startX - rows[0].playerX) > .65);
    assert.equal(native.firstContactX, startX);
    const contact = rows.find(row => row.begins > 0);
    const impulse = rows.find(row => row.impulse > 0);
    assert(contact && impulse && impulse.tick >= contact.tick);
    assert.equal(m.firstContactTick, contact.tick);
    assert.equal(m.firstImpulseTick, impulse.tick);
    assert.equal(m.firstMotionTick, rows.find(row => Math.abs(row.crateX - startX) > .00025).tick);
    assert(m.firstMotionTick >= m.firstImpulseTick);
    assert(rows.slice(0, contact.tick).every(row => row.crateX === startX));
    assert(rows.some(row => row.phase === 'PUSH' && row.input === direction));
    assert(rows.some(row => row.phase === 'RETREAT' && row.input === -direction));
    const release = rows[m.releaseTick];
    const hold = rows.slice(m.releaseTick + 1);
    assert.equal(hold.length, 60);
    assert.equal(release.phase, 'RETREAT');
    assert.equal(release.contacts, 0);
    assert(hold.every(row => row.phase === 'HOLD' && row.input === 0 && row.contacts === 0 && row.impulse === 0));
    assert(hold.every(row => row.begins === release.begins));
    assert(Math.abs(native.targetX - startX - setup.distance) < .00001);
    assert.equal(m.maxError, Math.max(...[release, ...hold].map(row => Math.abs(row.crateX - native.targetX))));
    assert.equal(m.maxDrift, Math.max(...hold.map(row => Math.abs(row.crateX - release.crateX))));
    assert(m.maxError <= .035 && m.maxDrift <= .018);
    assert.equal(hold.at(-1).crateX, m.finalX);
    assert.equal(Math.abs(hold.at(-1).crateVx), m.finalSpeed);
    assert(m.finalSpeed <= .008);
}

async function main() {
    process.env.FRAYNIX_UNIFIED_TEST = '1';
    process.env.FRAYNIX_WORKSPACE_NO_MODELS = '1';
    process.env.FRAYNIX_WORKSPACE_PERSISTENT = '0';
    const output = path.resolve(process.argv[2] || 'build/courier-target');
    const project = await fs.mkdtemp(path.join(os.tmpdir(), 'Fraynix Courier target '));
    await fs.mkdir(output, {recursive: true});
    const args = process.platform === 'linux' ? ['--no-sandbox', '--disable-dev-shm-usage'] : [];
    let app, page;
    const errors = [];
    const right = {distance: .65, mass: .35, friction: .75};
    const left = {distance: -.93, mass: 1.3, friction: .06};
    const game = async (command, args = {}) => app.evaluate(
        (_, input) => global.workspaceTest.api('/api/game/' + input.command, input.args), {command, args});
    const write = async (name, value) => fs.writeFile(path.join(output, name), JSON.stringify(value, null, 2) + '\n');
    const status = text => page.locator('#courier-target-result').filter({hasText: text}).waitFor({timeout: 55_000});

    async function open() {
        ({app, page} = await launch(project, args));
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
        await page.locator('#courier-target summary').click({timeout: 40_000});
    }
    async function screenshot(name) {
        const result = await app.evaluate(() => global.workspaceTest.capture());
        await fs.writeFile(path.join(output, name), Buffer.from(result, 'base64'));
    }
    async function displayedFrame(label, proof, screenshotFile) {
        await status(label);
        const endTick = proof.native.rows.at(-1).tick;
        await page.locator('#game-status').filter({hasText: 'tick ' + endTick + ' ·'}).waitFor();
        const frame = await game('frame');
        assert.equal(frame.state.courierCrate.targetX, proof.native.targetX);
        assert.equal(frame.state.courierCrate.activeContacts, 0);
        assert.equal(frame.state.courierCrate.crateX, proof.measurements.finalX);
        assert.equal(digest(Buffer.from(frame.pngBase64, 'base64')), frame.pngSha256);
        await screenshot(screenshotFile);
        return frame;
    }
    async function deliverFromForm(setup, file) {
        await page.locator('#courier-target-direction').selectOption(String(Math.sign(setup.distance)));
        await page.locator('#courier-target-distance').fill(String(Math.abs(setup.distance)));
        await page.locator('#courier-target-mass').fill(String(setup.mass));
        await page.locator('#courier-target-friction').fill(String(setup.friction));
        await page.locator('#courier-target-run:enabled').click();
        await status('Verified delivery');
        const proof = await game('courier-target-latest');
        audit(proof, setup);
        const frame = await displayedFrame('Verified delivery', proof, file);
        return {proof, frame};
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
            console.warn('Courier delivery needs a macOS OpenGL 4.1 display. This hosted run checked startup reporting.');
            return;
        }
        await enter();
        const rightRun = await deliverFromForm(right, 'courier-target-right.png');

        const cases = [];
        for (const direction of [1, -1])
            for (const length of [.32, .65, .93])
                for (const mass of [.35, .78, 1.30])
                    for (const friction of [.06, .40, .75])
                        cases.push({distance: direction * length, mass, friction});
        const extra = [
            {distance: .30, mass: .68, friction: .57},
            {distance: -.30, mass: .51, friction: .09},
            {distance: 1, mass: 1.16, friction: .12},
            {distance: -1, mass: 1.21, friction: .69}
        ];
        const matrix = [];
        for (const setup of [...cases, ...extra]) {
            const result = await game('courier-target-run', setup);
            audit(result, setup);
            matrix.push({setup, id: result.id, ...result.measurements});
        }
        assert.equal(new Set(matrix.map(result => result.id)).size, 58);

        const leftRun = await deliverFromForm(left, 'courier-target-left.png');
        await write('courier-target-left.json', leftRun.proof);
        await write('courier-target-right.json', rightRun.proof);
        await write('courier-target-matrix.json', matrix);
        const beforeRejectedSetup = await game('frame');
        await assert.rejects(game('courier-target-run', {...right, mass: 2}), /mass/i);
        assert.equal((await game('frame')).state.sessionId, beforeRejectedSetup.state.sessionId);
        assert.equal((await game('courier-target-latest')).id, leftRun.proof.id);
        await close();

        await open();
        await enter();
        await status('Saved delivery');
        assert.equal(await page.locator('#courier-target-direction').inputValue(), '-1');
        assert.equal((await game('courier-target-latest')).id, leftRun.proof.id);
        for (const record of matrix) {
            const saved = await game('courier-target-read', {id: record.id});
            audit(saved, record.setup);
            assert.equal(saved.id, record.id);
        }
        await page.locator('#courier-target-replay:enabled').click();
        const leftReplay = await displayedFrame('Fresh replay matched', leftRun.proof, 'courier-target-restart.png');
        assert.notEqual(leftReplay.state.sessionId, leftRun.frame.state.sessionId);
        assert.equal(leftReplay.pngSha256, leftRun.frame.pngSha256);

        const rightReplay = await game('courier-target-replay', {id: rightRun.proof.id});
        assert.equal(rightReplay.replayed, true);
        assert.deepEqual(rightReplay.native.rows, rightRun.proof.native.rows);
        const rightFrame = await game('frame');
        assert.notEqual(rightFrame.state.sessionId, rightRun.frame.state.sessionId);
        assert.equal(rightFrame.pngSha256, rightRun.frame.pngSha256);

        await page.locator('#courier-target-export:enabled').click();
        await status('Exported delivery evidence');
        const manifest = await fs.readFile(path.join(project, 'exports', 'manifest.jsonl'), 'utf8');
        const entry = manifest.trim().split('\n').map(JSON.parse).find(record => record.kind === 'courier-target-delivery');
        assert.equal(entry.source.id, leftRun.proof.id);
        const exported = await fs.readFile(entry.path);
        assert.equal(digest(exported), entry.sha256);
        assert.deepEqual(JSON.parse(exported), leftRun.proof);

        const retained = path.join(project, 'courier-target', leftRun.proof.id + '.json');
        const failedHold = JSON.parse(await fs.readFile(retained, 'utf8'));
        failedHold.native.rows[failedHold.measurements.releaseTick + 12].crateX += .08;
        const changedBytes = Buffer.from(JSON.stringify(failedHold));
        const changedId = digest(changedBytes);
        await fs.writeFile(path.join(project, 'courier-target', changedId + '.json'), changedBytes, {flag: 'wx'});
        await assert.rejects(game('courier-target-read', {id: changedId}), /Target not verified/);

        await fs.appendFile(retained, '\n');
        await assert.rejects(game('courier-target-replay', {id: leftRun.proof.id}), /evidence changed/);
        assert.equal((await game('frame')).state.sessionId, rightFrame.state.sessionId);
        assert.equal((await app.evaluate(() => global.workspaceTest.network())).enabled, true);
        assert.deepEqual(errors, []);

        const result = {passed: true, platform: process.platform, matrix: matrix.length,
            offlineRestart: true, receiptsRecheckedAfterRestart: matrix.length, tamperedReceiptRefused: true,
            invalidSetupRefused: true, failedHoldRejected: true, checkedExport: true, holdTicks: 60,
            worstHeldError: Math.max(...matrix.map(record => record.maxError)),
            maxDrift: Math.max(...matrix.map(record => record.maxDrift)),
            maxFinalSpeed: Math.max(...matrix.map(record => record.finalSpeed)),
            rightReplay: {id: rightRun.proof.id, frameSha256: rightFrame.pngSha256},
            leftReplay: {id: leftRun.proof.id, frameSha256: leftReplay.pngSha256}};
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
