'use strict';

const assert = require('node:assert/strict');
const {createHash} = require('node:crypto');
const fs = require('node:fs/promises');
const os = require('node:os');
const path = require('node:path');
const {performance} = require('node:perf_hooks');
const {pathToFileURL} = require('node:url');
const {launch} = require('./calibration-integration.cjs');

const hash = bytes => createHash('sha256').update(bytes).digest('hex');

async function main() {
    process.env.FRAYNIX_UNIFIED_TEST = '1';
    process.env.FRAYNIX_WORKSPACE_NO_MODELS = '1';
    const output = path.resolve(process.argv[2] || 'build/character-rig-test');
    const project = await fs.mkdtemp(path.join(os.tmpdir(), 'Fraynix Courier '));
    await fs.mkdir(output, {recursive: true});
    const flags = process.platform === 'linux' ? ['--no-sandbox', '--disable-dev-shm-usage'] : [];
    let app, page;
    const errors = [];
    async function open() {
        ({app, page} = await launch(project, flags));
        page.on('pageerror', error => errors.push(error.message));
        await page.locator('#workspace-home h1').waitFor();
    }

    const api = (route, args = {}) => app.evaluate((_, input) => global.workspaceTest.api(input.route, input.args), {route, args});
    const tile = (command, args) => api('/api/media-tiles/' + command, args);
    const capture = async file => {
        const data = await app.evaluate(() => global.workspaceTest.capture());
        await fs.writeFile(path.join(output, file), Buffer.from(data, 'base64'));
    };
    const ask = async message => {
        await page.locator('#goal').fill(message);
        await page.locator('#run').click();
    };
    const status = text => page.locator('#character-rig-status').filter({hasText: text}).waitFor({timeout: 125_000});
    const rigPath = id => path.join(project, 'media', 'character-rigs', id);

    async function play(view, selector, captureCourse = false) {
        const stage = view.locator(selector);
        await stage.focus();
        await view.waitForFunction(selector => document.querySelector(selector)?.dataset.hero === 'Teal Courier', selector);
        await stage.evaluate(canvas => {
            window.walkFrames = new Set();
            const changes = new MutationObserver(() => {
                if (canvas.dataset.playerFrame !== 'idle') window.walkFrames.add(canvas.dataset.playerFrame);
                if (canvas.dataset.won === 'true') changes.disconnect();
            });
            changes.observe(canvas, {attributes: true});
        });
        await view.keyboard.down('ArrowRight');
        try {
            await view.waitForFunction(selector => Number(document.querySelector(selector)?.dataset.playerX) === 336, selector);
            await view.keyboard.press('Space');
            if (captureCourse) {
                await view.waitForFunction(selector => Number(document.querySelector(selector)?.dataset.playerX) > 375, selector);
                await capture('courier-level.png');
            }
            await view.waitForFunction(selector => document.querySelector(selector)?.dataset.won === 'true', selector);
        } finally {
            await view.keyboard.up('ArrowRight');
        }
        const result = await stage.evaluate(canvas => ({
            score: Number(canvas.dataset.score), collected: Number(canvas.dataset.collected),
            frames: [...window.walkFrames].sort()
        }));
        assert.deepEqual(result, {score: 600, collected: 1, frames: ['0', '1', '2', '3']});
        return result;
    }

    try {
        await open();
        await ask('Make a Blender teal Courier character.');
        const start = performance.now();
        await status('Teal Courier · 13 bones · 9 verified poses');
        const bakeMs = Math.round(performance.now() - start);
        const id = await page.locator('#character-rig-library').inputValue();
        const projectFile = path.join(rigPath(id), 'courier.blend');
        const raw = await fs.readFile(path.join(rigPath(id), 'rig.json'));
        assert.equal(hash(raw), id);
        const manifest = JSON.parse(raw);
        assert.equal(hash(await fs.readFile(projectFile)), manifest.blendSha256);
        assert.deepEqual(manifest.inspection.blinkWeights, [0, 1]);
        assert.deepEqual(manifest.inspection.leftThighDegrees, [24.92, -31.08]);
        assert.equal(manifest.inspection.kinematicBox, true);
        const pose = async name => 'data:image/png;base64,' + (await fs.readFile(path.join(rigPath(id), name + '.png'))).toString('base64');
        const blink = await pose('blink');
        await page.waitForFunction(blink => document.querySelector('#character-rig-preview').src === blink, blink);
        const jumpButton = page.getByRole('button', {name:'Jump', exact:true});
        await jumpButton.click();
        await page.waitForFunction(air => document.querySelector('#character-rig-preview').src === air, await pose('airborne'));
        await page.getByRole('button', {name:'Walk', exact:true}).click();
        await capture('character-studio.png');

        const frameBytes = await Promise.all(Object.keys(manifest.frames).map(name => fs.readFile(path.join(rigPath(id), name + '.png'))));
        const frames = frameBytes.map(bytes => bytes.toString('base64'));
        const atlas = await api('/api/avatar/rig-frames', {frames});
        assert.equal(atlas.frames, 9);
        const sheet = Buffer.from(atlas.png, 'base64');
        assert.equal(hash(sheet), atlas.sha256);
        assert.deepEqual([sheet.readUInt32BE(16), sheet.readUInt32BE(20)], [288, 336]);
        await assert.rejects(api('/api/avatar/rig-frames', {frames: frames.map(() => frames[0])}), /different rendered pixels/);
        for (const [kind, name, sha] of [
            ['blend', 'courier.blend', manifest.blendSha256], ['atlas', 'courier-poses.png', atlas.sha256]
        ]) {
            await page.locator('#character-rig-' + kind).click();
            await status('Exported and verified');
            const saved = (await page.locator('#character-rig-status').innerText()).slice('Exported and verified '.length);
            const exported = await fs.readFile(saved);
            assert.equal(hash(exported), sha);
            await fs.writeFile(path.join(output, name), exported);
        }

        const png = await page.evaluate(() => {
            const canvas = document.createElement('canvas');
            canvas.width = canvas.height = 24;
            const draw = canvas.getContext('2d');
            draw.fillStyle = '#ffb25e';
            draw.beginPath();
            draw.arc(12, 12, 9, 0, Math.PI * 2);
            draw.fill();
            return canvas.toDataURL().split(',')[1];
        });
        const pickup = await tile('add', {source: {filename: 'Courier relay signal.png', png}});
        const relayArt = await page.evaluate(() => {
            const canvas = document.createElement('canvas');
            canvas.width = canvas.height = 80;
            const draw = canvas.getContext('2d');
            const gradient = draw.createLinearGradient(0, 1, 80, 72);
            gradient.addColorStop(0, '#51827d');
            gradient.addColorStop(1, '#243951');
            draw.beginPath();
            draw.moveTo(15, 76);
            draw.lineTo(15, 34);
            draw.lineTo(30, 15);
            draw.lineTo(63, 15);
            draw.lineTo(72, 30);
            draw.lineTo(72, 76);
            draw.closePath();
            draw.fillStyle = gradient;
            draw.fill();
            draw.strokeStyle = '#3aaea4';
            draw.stroke();
            draw.fillStyle = '#233f59';
            draw.fillRect(30, 33, 31, 9);
            draw.fillStyle = '#ffc282';
            for (const x of [34, 42, 50]) draw.fillRect(x, 36, 4, 3);
            draw.fillStyle = '#68d6d2';
            draw.fillRect(44, 5, 3, 10);
            draw.fillRect(42, 3, 7, 3);
            return canvas.toDataURL().split(',')[1];
        });
        const station = await tile('add', {source: {filename: 'Courier relay terminal.png', png: relayArt}});
        const base = await tile('save', {layout: {title: 'Courier Relay', objects: [
            {id: 'mast', assetId: station.id, kind: 'sprite', layer: 'backdrop', x: 110, y: 192, size: 120},
            {id: 'tower', assetId: station.id, kind: 'sprite', layer: 'backdrop', x: 452, y: 170, size: 142},
            {id: 'signal', assetId: pickup.id, kind: 'sprite', layer: 'pickup', x: 160, y: 288, size: 24},
            {id: 'hurdle', assetId: station.id, kind: 'sprite', layer: 'terrain', x: 360, y: 266, size: 46,
                collider: {x: 0, y: 3, w: 46, h: 43}}
        ]}});
        await page.locator('#character-rig-player').click();
        await page.locator('#tile-status').filter({hasText:'Save the level to play'}).waitFor();
        await page.locator('#tile-save').click();
        await page.locator('#tile-status').filter({hasText:'Saved level. Sprite sources'}).waitFor();
        const level = await tile('open');
        assert.deepEqual(level.layout.objects, base.layout.objects);
        assert.equal(level.layout.player.avatarId, id);
        assert.equal(level.layout.player.name, 'Teal Courier');
        await page.locator('#tile-play:enabled').click();
        const desktop = await play(page, '#tile-canvas', true);
        await page.locator('#tile-stop').click();
        await page.locator('#tile-run-summary').filter({hasText: '600'}).waitFor();
        const trace = await tile('export-run');
        const contact = trace.rows.find(row => row[6] === 'hurdle');
        const jump = trace.rows.find(row => row[17].some(([event]) => event === 'jump'));
        assert(contact && jump && contact[0] < jump[0]);
        const landing = trace.rows.find(row => row[0] > jump[0] && row[1] > 406 && row[5] === 'floor');
        assert(landing, 'The character landed past the hurdle.');
        await fs.writeFile(path.join(output, 'courier-playtest.json'), JSON.stringify(trace) + '\n');

        const html = await tile('export', {id: level.id, format: 'html'});
        const htmlFile = path.join(output, 'courier-relay.html');
        assert.equal(hash(Buffer.from(html.base64, 'base64')), html.sha256);
        await fs.writeFile(htmlFile, Buffer.from(html.base64, 'base64'));
        await app.close();
        app = null;

        await open();
        await ask('Open Avatar Studio.');
        await page.locator('#character-rig summary').click();
        await page.locator('#character-rig-library').selectOption(id);
        await status('Teal Courier · 13 bones · 9 verified poses');
        assert.equal((await tile('export', {id: level.id, format: 'html'})).sha256, html.sha256);

        const url = pathToFileURL(htmlFile).href;
        await app.evaluate(async ({BrowserWindow}, url) => {
            const window = new BrowserWindow({width: 950, height: 610, webPreferences: {
                partition: 'courier-local-only', sandbox: true, nodeIntegration: false, contextIsolation: true
            }});
            window.webContents.session.webRequest.onBeforeRequest((request, done) => done({cancel: !/^(file|data):/.test(request.url)}));
            await window.loadURL(url);
        }, url);
        const standalone = app.windows().find(window => window.url() === url);
        assert(standalone);
        const portable = await play(standalone, 'canvas');

        const tampered = path.join(rigPath(id), 'blink.png');
        const blinkBytes = await fs.readFile(tampered);
        await fs.writeFile(tampered, frameBytes[0]);
        await page.locator('#character-rig-library').selectOption('');
        await page.locator('#character-rig-library').selectOption(id);
        await status('The saved blink frame changed.');
        await fs.writeFile(tampered, blinkBytes);
        await page.locator('#character-rig-library').selectOption('');
        await page.locator('#character-rig-library').selectOption(id);
        await status('Teal Courier · 13 bones · 9 verified poses');
        const startSecond = performance.now();
        await page.locator('#character-rig-palette').selectOption('amber');
        await page.locator('#character-rig-bake').click();
        await status('Amber Courier · 13 bones · 9 verified poses');
        const amberBakeMs = Math.round(performance.now() - startSecond);
        const amberId = await page.locator('#character-rig-library').inputValue();
        const amber = JSON.parse(await fs.readFile(path.join(rigPath(amberId), 'rig.json'), 'utf8'));
        assert.notEqual(amber.frames.idle, manifest.frames.idle);
        assert.notEqual(amber.blendSha256, manifest.blendSha256);
        assert.deepEqual(errors, []);
        const results = {passed: true, platform: process.platform, bakeMs, amberBakeMs,
            rig: {id, ...manifest}, atlasSha256: atlas.sha256, level: {id: level.id, sha256: html.sha256},
            traversal: {contactTick: contact[0], jumpTick: jump[0], farSideGroundedTick: landing[0]},
            desktop, portable, checks: ['Blender readback', 'blink and jump preview', 'duplicate poses refused',
                'rig reopened after desktop restart', 'changed frame refused', 'distinct Amber palette',
                'exact standalone export after restart']};
        await fs.writeFile(path.join(output, 'result.json'), JSON.stringify(results, null, 2) + '\n');
        console.log(JSON.stringify(results));
    } catch (error) {
        if (page) await fs.writeFile(path.join(output, 'failure.txt'), await page.locator('body').innerText()).catch(() => {});
        throw error;
    } finally {
        if (app) await app.close();
        await fs.rm(project, {recursive: true, force: true});
    }
}

main().catch(error => { console.error(error); process.exitCode = 1; });
