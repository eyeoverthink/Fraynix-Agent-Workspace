'use strict';

const assert = require('node:assert/strict');
const {createHash} = require('node:crypto');
const fs = require('node:fs/promises');
const os = require('node:os');
const path = require('node:path');
const {launch} = require('./calibration-integration.cjs');

async function main() {
    process.env.FRAYNIX_UNIFIED_TEST = '1';
    process.env.FRAYNIX_WORKSPACE_NO_MODELS = '1';
    process.env.FRAYNIX_WORKSPACE_PERSISTENT = '0';
    const out = path.resolve(process.argv[2] || 'build/courier-crate');
    const project = await fs.mkdtemp(path.join(os.tmpdir(), 'Fraynix Courier crate '));
    const args = process.platform === 'linux' ? ['--no-sandbox', '--disable-dev-shm-usage'] : [];
    await fs.mkdir(out, {recursive: true});
    let app, page;
    const errors = [];
    const checked = async (endpoint, input = {}) =>
        app.evaluate((_, context) => global.workspaceTest.api(context.endpoint, context.input), {endpoint, input});
    const game = async (action, input) => checked('/api/game/' + action, input);
    const capture = async name => {
        await page.evaluate(() => new Promise(resolve => requestAnimationFrame(() => requestAnimationFrame(resolve))));
        const bytes = await app.evaluate(() => global.workspaceTest.capture());
        await fs.writeFile(path.join(out, name), Buffer.from(bytes, 'base64'));
    };
    const nativeCapture = async (frame, filename) => {
        const bytes = Buffer.from(frame.pngBase64, 'base64');
        assert.equal(createHash('sha256').update(bytes).digest('hex'), frame.pngSha256);
        assert.deepEqual([bytes.readUInt32BE(16), bytes.readUInt32BE(20)], [960, 540]);
        await fs.writeFile(path.join(out, filename), bytes);
    };
    const result = text => page.locator('#courier-crate-result').filter({hasText: text}).waitFor({timeout: 55_000});
    const status = text => page.locator('#game-status').filter({hasText: text}).waitFor({timeout: 55_000});

    async function open() {
        ({app, page} = await launch(project, args));
        page.on('pageerror', error => errors.push(error.message));
        assert.equal((await app.evaluate(() => global.workspaceTest.network())).enabled, true);
        assert.equal((await app.evaluate(() => global.workspaceTest.api('/api/state'))).models?.length ?? 0, 0);
    }
    async function close() {
        if (app) {
            await app.close();
            app = null;
        }
    }
    async function enter() {
        await page.locator('#goal').fill('Open Game Lab.');
        await page.locator('#run').click();
        await page.locator('#courier-crate summary').click({timeout: 40_000});
    }

    try {
        await open();
        // Check the native display before driving the UI, so the hosted Mac display limitation stays distinguishable.
        try {
            await game('open-relay');
        } catch (error) {
            if (process.platform !== 'darwin' || process.env.FRAYNIX_CI_MAC_DISPLAY !== '1' ||
                    !error.message.includes('Game Lab cannot get a macOS OpenGL 4.1 context from the current display.')) throw error;
            assert.equal((await game('state')).running, false);
            await fs.writeFile(path.join(out, 'result.json'), JSON.stringify({nativeGame: 'display-unavailable'}) + '\n');
            console.warn('Courier proof needs a macOS OpenGL 4.1 display. This hosted run checked startup reporting.');
            return;
        }
        await enter();
        await page.locator('#courier-crate-open:enabled').click();
        await result('Practice scene ready');
        const before = await game('frame');
        assert.equal(before.state.visibleWindow, false);
        assert.equal(before.state.editorInitialized, false);
        assert.equal(before.state.courierCrate.crateX, 1.62);
        assert.equal(before.state.courierCrate.beginContacts, 0);
        assert.deepEqual(await game('courier-latest'), {});
        await nativeCapture(before, 'courier-native-before.png');
        await capture('courier-before.png');

        // Usual Game Lab controls must drive the same scene and body.
        await page.locator('#game-right').click();
        await status('tick 30');
        const approach = await game('frame');
        assert(approach.state.player.x > before.state.player.x);
        assert.equal(approach.state.courierCrate.crateX, before.state.courierCrate.crateX);
        await page.locator('#game-right').click();
        await status('tick 60');
        const manual = await game('frame');
        assert(manual.state.courierCrate.beginContacts > 0);
        assert(manual.state.courierCrate.crateX > before.state.courierCrate.crateX);
        await page.locator('#game-reset').click();
        await status('tick 0');
        assert.equal((await game('frame')).state.courierCrate.crateX, before.state.courierCrate.crateX);

        await page.locator('#courier-crate-test:enabled').click();
        await result('Verified · Shared approach');
        await status('tick 120');
        const proof = await game('courier-latest');
        const {control, push, atlasSha256, playerId} = proof.native;
        const {measurements: m} = proof;
        assert.equal(proof.passed, true);
        assert.equal(playerId, before.state.player.id);
        assert.equal(atlasSha256, 'cdbd0c2c0ddae75bbac1ff4056eb39c7ae18410705686b2adaaa5f229602ecc1');
        assert.equal(push.playerType, 'DYNAMIC');
        assert.equal(push.crateType, 'DYNAMIC');
        assert.equal(push.playerFixedRotation, true);
        assert(push.playerMass > 0 && push.crateMass > 0);
        assert.deepEqual(push.rows.slice(0, 49), control.rows.slice(0, 49));
        assert.equal(control.rows.length, 121);
        assert.equal(push.rows.length, 121);
        const initialX = push.rows[0].crateX;
        const firstContact = push.rows.find(row => row.beginContacts > 0);
        const firstImpulse = push.rows.find(row => row.horizontalNormalImpulse > 0);
        const firstMotion = push.rows.find(row => Math.abs(row.crateX - initialX) > m.motionThreshold);
        assert(firstContact && firstImpulse && firstMotion);
        assert.equal(push.crateXAtFirstContact, initialX);
        assert.equal(m.firstContactTick, firstContact.tick);
        assert.equal(m.firstHorizontalImpulseTick, firstImpulse.tick);
        assert.equal(m.firstMotionTick, firstMotion.tick);
        assert(firstImpulse.tick >= firstContact.tick && firstMotion.tick >= firstImpulse.tick);
        assert(control.rows.every(row => row.crateX === initialX && row.beginContacts === 0 && row.horizontalNormalImpulse === 0));
        assert(push.rows.slice(0, firstContact.tick).every(row => row.crateX === initialX));
        assert.equal(m.finalPushDx, push.rows.at(-1).crateX - initialX);
        assert(m.finalPushDx > .15);
        const after = await game('frame');
        assert.notEqual(after.pngSha256, before.pngSha256);
        await nativeCapture(after, 'courier-native-after.png');
        await capture('courier-verified.png');
        await fs.writeFile(path.join(out, 'courier-crate-proof.json'), JSON.stringify(proof, null, 2) + '\n');

        await close();
        await open();
        await enter();
        await result('Saved result');
        assert.deepEqual(await game('courier-latest'), proof);
        await page.locator('#courier-crate-replay:enabled').click();
        await result('Fresh replay matched');
        await status('tick 120');
        const replay = await game('frame');
        assert.notEqual(replay.pid, after.pid);
        assert.notEqual(replay.state.sessionId, after.state.sessionId);
        assert.equal(replay.pngSha256, after.pngSha256);
        assert.deepEqual(await game('courier-latest'), proof);
        assert.equal((await app.evaluate(() => global.workspaceTest.network())).enabled, true);
        await capture('courier-restart.png');

        await page.locator('#courier-crate-export:enabled').click();
        await result('Exported evidence');
        const directory = path.join(project, 'exports');
        const entries = (await fs.readFile(path.join(directory, 'manifest.jsonl'), 'utf8')).trim().split('\n').map(JSON.parse);
        const entry = entries.find(row => row.kind === 'courier-crate-trial');
        assert.equal(entry.source.id, proof.id);
        const exported = await fs.readFile(entry.path);
        assert.equal(createHash('sha256').update(exported).digest('hex'), entry.sha256);
        assert.deepEqual(JSON.parse(exported), proof);

        const saved = path.join(project, 'courier-crate', proof.id + '.json');
        const retained = await fs.readFile(saved);
        await fs.appendFile(saved, '\n');
        await assert.rejects(game('courier-replay', {id: proof.id}), /evidence changed/);
        assert.equal((await game('frame')).state.sessionId, replay.state.sessionId);
        await fs.writeFile(saved, retained);
        assert.deepEqual(await game('courier-latest'), proof);
        assert.deepEqual(errors, []);
        const findings = {passed: true, platform: process.platform, proofId: proof.id,
            firstRunCaptureSha256: after.pngSha256, replayCaptureSha256: replay.pngSha256,
            offline: true, newRuntimeAfterRestart: true, measuredTraceMatched: true,
            tamperedReceiptRefused: true, exportsMatched: true, manualPushAndReset: true, measurements: m};
        await fs.writeFile(path.join(out, 'result.json'), JSON.stringify(findings, null, 2) + '\n');
        console.log(JSON.stringify(findings));
    } catch (error) {
        if (page) await fs.writeFile(path.join(out, 'failure.txt'), await page.locator('body').innerText()).catch(() => {});
        throw error;
    } finally {
        await close();
        await fs.rm(project, {recursive: true, force: true});
    }
}

main().catch(error => { console.error(error); process.exitCode = 1; });
