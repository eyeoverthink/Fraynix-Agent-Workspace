
'use strict';

const assert = require('node:assert/strict');
const {createHash} = require('node:crypto');
const fs = require('node:fs/promises');
const os = require('node:os');
const path = require('node:path');
const {pathToFileURL} = require('node:url');
const {launch} = require('./calibration-integration.cjs');

const digest = bytes => createHash('sha256').update(bytes).digest('hex');
const args = process.platform === 'linux' ? ['--no-sandbox','--disable-dev-shm-usage'] : [];

async function main() {
    process.env.FRAYNIX_WORKSPACE_NO_MODELS = '1';
    const output = path.resolve(process.argv[2] || 'build/visual-tiles230');
    const project = await fs.mkdtemp(path.join(os.tmpdir(), 'Fraynix tile author '));
    const destination = await fs.mkdtemp(path.join(os.tmpdir(), 'Fraynix tile import '));
    await fs.mkdir(output, {recursive:true});
    let {app, page} = await launch(project, args);
    const call = (route, body = {}) => app.evaluate((_, request) =>
        global.workspaceTest.api('/api/media-tiles/' + request.route, request.body), {route,body});
    const status = text => page.waitForFunction(text => document.querySelector('#tile-status').textContent.includes(text), text);
    const choose = name => page.locator('#tile-assets button').filter({hasText:name}).click();
    const capture = async name => fs.writeFile(path.join(output,name),
        Buffer.from(await app.evaluate(() => global.workspaceTest.capture()), 'base64'));

    async function makePNG(file, colors) {
        const base64 = await page.evaluate(([main, trim, emblem]) => {
            const canvas = document.createElement('canvas');
            canvas.width = 48; canvas.height = 48;
            const ctx = canvas.getContext('2d');
            ctx.fillStyle = main;
            ctx.fillRect(4,4,40,40);
            ctx.fillStyle = trim;
            ctx.fillRect(4,4,40,7);
            ctx.fillRect(4,37,40,7);
            ctx.fillStyle = emblem;
            ctx.fillRect(13,13,22,22);
            ctx.clearRect(20,20,8,8);
            return canvas.toDataURL('image/png').split(',')[1];
        }, colors);
        await fs.writeFile(file, Buffer.from(base64,'base64'));
    }

    async function point(x,y) {
        const canvas = page.locator('#tile-canvas');
        await canvas.scrollIntoViewIfNeeded();
        return canvas.evaluate((el,{x,y}) => {
            const r = el.getBoundingClientRect();
            return {x:r.x+el.clientLeft+(x+.25)*el.clientWidth/720,
                y:r.y+el.clientTop+(y+.25)*el.clientHeight/336};
        }, {x,y});
    }

    async function gesture(from, to) {
        const a = await point(...from);
        const b = to ? await point(...to) : a;
        await page.mouse.move(a.x,a.y);
        await page.mouse.down();
        if (to) await page.mouse.move(b.x,b.y,{steps:4});
        await page.mouse.up();
    }

    async function field(query, value) {
        const input = page.locator(query);
        await input.fill(String(value));
        await input.press('Tab');
        assert.equal(await input.inputValue(), String(value));
    }

    async function place(layer) {
        await page.locator('#tile-layer').selectOption(layer);
        await page.locator('#tile-place').click();
    }

    async function scene(view, selector, prop, value) {
        await view.waitForFunction(r => document.querySelector(r.selector)?.dataset[r.prop] === r.value,
            {selector, prop, value});
    }

    const physicsState = (view, selector) => view.locator(selector).evaluate(el => ({...el.dataset}));

    async function fullRun(view, selector, obstacle, tileIds, probeInset = false) {
        const xAtLeast = value => view.waitForFunction(r =>
            Number(document.querySelector(r.selector)?.dataset.playerX) >= r.value, {selector,value});
        await view.keyboard.down('ArrowRight');
        await scene(view, selector, 'wall', obstacle);
        await scene(view, selector, 'playerX', '200.00');
        const wall = await physicsState(view,selector);
        assert.equal(wall.collected, '1');
        await view.keyboard.press('Space');
        await xAtLeast(235);
        await view.keyboard.up('ArrowRight');
        await scene(view, selector, 'support', obstacle);
        const landed = await physicsState(view,selector);
        assert.equal(landed.playerY, '236.00');
        await view.keyboard.down('ArrowRight');
        await xAtLeast(303);
        await view.keyboard.up('ArrowRight');
        await scene(view, selector, 'support', 'floor');
        await view.keyboard.down('ArrowRight');
        await view.keyboard.press('Space');
        await xAtLeast(probeInset ? 346 : 388);
        await view.keyboard.up('ArrowRight');
        if (probeInset) {
            await scene(view, selector, 'support', tileIds[0]);
            const inset = await physicsState(view,selector);
            assert.equal(inset.playerY, '212.00');
            return inset;
        }
        await scene(view, selector, 'collected', '2');
        await view.waitForFunction(r => r.ids.includes(document.querySelector(r.selector)?.dataset.support),
            {selector, ids:tileIds});
        const tiles = await physicsState(view,selector);
        assert.equal(tiles.playerY, '208.00');
        await view.keyboard.down('ArrowRight');
        await scene(view, selector, 'won', 'true');
        await view.keyboard.up('ArrowRight');
        return {wall, landed, tiles, exit:await physicsState(view,selector)};
    }

    try {
        await page.locator('#media-tab').click();
        const beacon = path.join(output,'amber-beacon.png'), slate = path.join(output,'cobalt-slate.png');
        await makePNG(beacon,['#dc8e48','#ffd69c','#fff0cb']);
        await makePNG(slate,['#3f7ca3','#8ad4d1','#396680']);
        await page.locator('#tile-png').setInputFiles(beacon);
        await status('Added amber-beacon.png');
        await page.locator('#tile-png').setInputFiles(slate);
        await status('Added cobalt-slate.png');
        const library = (await call('library')).assets;
        assert.equal(library.length,2);
        const amber = library.find(asset => asset.label === 'amber-beacon.png').id;
        const cobalt = library.find(asset => asset.label === 'cobalt-slate.png').id;
        await page.locator('#tile-title').fill('Signal Foundry');

        await choose('amber-beacon.png');
        await place('backdrop');
        await gesture([110,151],[146,120]);
        const draggedX = Number(await page.locator('[data-position=x]').inputValue());
        const draggedY = Number(await page.locator('[data-position=y]').inputValue());
        assert.ok(Math.abs(draggedX - 132) <= 1 && Math.abs(draggedY - 104) <= 1, 'Pointer moved the placed sprite');
        await place('pickup');
        await field('[data-position=x]',148);

        await choose('cobalt-slate.png');
        await place('terrain');
        await field('[data-position=x]',216);
        await field('[data-position=y]',264);
        await field('[data-collider=w]',36);
        await field('[data-collider=h]',44);
        await field('[data-collider=x]',8);
        await field('[data-collider=y]',4);

        await page.locator('[data-mode=paint]').click();
        await gesture([365,250],[466,250]);
        await page.locator('[data-mode=erase]').click();
        await gesture([466,250]);
        await page.locator('[data-mode=select]').click();
        await gesture([367,248]);
        await field('[data-collider=h]',16);
        await field('[data-collider=y]',4);

        await choose('amber-beacon.png');
        await place('pickup');
        await field('[data-position=x]',392);
        await field('[data-position=y]',208);
        await choose('cobalt-slate.png');
        await place('foreground');
        await field('[data-position=x]',534);
        await field('[data-position=y]',261);
        await page.locator('#tile-save').click();
        await status('Saved level. Sprite sources');
        const primary = await call('open');
        const objects = primary.layout.objects;
        assert.equal(objects.length,9);
        const block = objects.find(item => item.kind === 'sprite' && item.layer === 'terrain');
        assert.deepEqual(block.collider,{x:8,y:4,w:36,h:44});
        const tiles = objects.filter(item => item.kind === 'tile');
        assert.deepEqual(tiles.map(item => item.x),[360,384,408,432]);
        assert.deepEqual(tiles[0].collider,{x:0,y:4,w:24,h:16});
        assert.equal(objects.filter(item => item.assetId === amber).length,3);
        assert.equal(objects.filter(item => item.assetId === cobalt).length,6);
        const tamperedLayout = structuredClone(primary.layout);
        tamperedLayout.objects.find(item => item.id === block.id).collider.w = 55;
        await assert.rejects(call('save',{layout:tamperedLayout}), /w must|range|between/i);
        assert.equal((await call('open')).id,primary.id);

        await gesture([222,274]);
        await page.locator('#tile-editor').evaluate(el => {el.scrollTop = 0;});
        await capture('tile-workshop.png');
        await page.locator('#tile-editor').evaluate(el => {el.scrollTop = el.scrollHeight;});
        await capture('tile-collider.png');
        await page.locator('#tile-play').click();
        const inset = await fullRun(page,'#tile-canvas',block.id,tiles.map(item => item.id), true);
        await page.locator('#tile-stop').click();
        await page.locator('#tile-play').click();
        const desktop = await fullRun(page,'#tile-canvas',block.id,tiles.map(item => item.id));
        await page.locator('#tile-stop').click();

        const names = {html:'signal-foundry.html',json:'signal-foundry.json',png:'library-slate.png'};
        async function saveExport(control, format) {
            await page.locator(control).click();
            await page.waitForFunction(ext => {
                const text = document.querySelector('#tile-status').textContent;
                return text.startsWith('Saved ') && text.endsWith('.'+ext);
            }, format);
            const saved = (await page.locator('#tile-status').textContent()).slice(6);
            const bytes = await fs.readFile(saved);
            const receipt = JSON.parse(await fs.readFile(saved + '.receipt.json','utf8'));
            assert.equal(receipt.sha256,digest(bytes));
            assert.equal(receipt.source.id,format === 'png' ? cobalt : primary.id);
            const published = path.join(output,names[format]);
            await fs.writeFile(published,bytes);
            return {sha256:receipt.sha256,bytes:bytes.length,assets:receipt.source.assets};
        }
        const exports = {html:await saveExport('#tile-html','html'),json:await saveExport('#tile-json','json'),
            png:await saveExport('#tile-export-png','png')};
        assert.equal(Object.keys(exports.html.assets).length,2);
        assert.deepEqual(exports.html.assets,exports.json.assets);
        assert.equal(exports.png.sha256,primary.sprites[cobalt].sha256);

        await page.locator('#tile-new').click();
        await page.locator('#tile-title').fill('Clearwater siding');
        await choose('amber-beacon.png');
        await place('pickup');
        await field('[data-position=y]',225);
        await choose('cobalt-slate.png');
        await place('backdrop');
        await page.locator('#tile-save').click();
        await status('Saved level. Sprite sources');
        const secondary = await call('open');
        assert.notEqual(secondary.id,primary.id);
        assert.deepEqual(Object.keys(secondary.sprites).sort(),Object.keys(primary.sprites).sort());
        assert.equal((await call('library')).assets.length,2);
        await page.locator('#tile-play').click();
        await page.keyboard.down('ArrowRight');
        await scene(page,'#tile-canvas','playerX','648.00');
        await page.keyboard.up('ArrowRight');
        assert.equal((await physicsState(page,'#tile-canvas')).won,'false');
        assert.match(await page.locator('#tile-progress').textContent(),/exit is locked/i);
        await page.locator('#tile-stop').click();
        await page.locator('#tile-saved').selectOption(primary.id);
        await status('Opened checked level.');
        await app.close();

        ({app,page} = await launch(project,args));
        await page.locator('#media-tab').click();
        await status('Reopened checked level');
        assert.equal(await page.locator('#tile-title').inputValue(),'Signal Foundry');
        assert.deepEqual((await call('open')).layout,primary.layout);
        assert.equal((await call('list')).levels.length,2);
        await page.locator('#tile-play').click();
        const restored = await fullRun(page,'#tile-canvas',block.id,tiles.map(item => item.id));
        await page.locator('#tile-stop').click();
        const rebuilt = await call('export',{id:primary.id,format:'html'});
        assert.equal(rebuilt.sha256,exports.html.sha256);

        const source = path.join(project,'media','tiles','sprites',amber+'.json');
        await fs.appendFile(source,' ');
        await assert.rejects(call('export',{id:primary.id,format:'html'}),/bytes changed/i);
        await assert.rejects(call('open',{id:primary.id}),/bytes changed/i);
        await app.close();

        ({app,page} = await launch(destination,args));
        await page.locator('#media-tab').click();
        await page.waitForFunction(() => !document.querySelector('#tile-import-level').disabled);
        await page.locator('#tile-import-level').setInputFiles(path.join(output,names.json));
        await status('Imported checked level');
        assert.equal((await call('open')).id,primary.id);
        assert.equal((await call('library')).assets.length,2);
        const bundle = JSON.parse(await fs.readFile(path.join(output,names.json),'utf8'));
        bundle.assets[amber].spriteSha256 = '0'.repeat(64);
        await assert.rejects(call('import',{base64:Buffer.from(JSON.stringify(bundle)).toString('base64')}),/sprite record changed/i);
        assert.equal((await call('open')).id,primary.id);

        const fileURL = pathToFileURL(path.join(output,names.html)).href;
        await app.evaluate(async ({BrowserWindow},fileURL) => {
            const win = new BrowserWindow({width:920,height:620,webPreferences:{
                partition:'tiles-standalone-proof',sandbox:true,nodeIntegration:false,contextIsolation:true
            }});
            win.webContents.session.webRequest.onBeforeRequest((r,done) => done({cancel:!/^(file|data):/.test(r.url)}));
            await win.loadURL(fileURL);
        },fileURL);
        const standalonePage = app.windows().find(win => win.url() === fileURL);
        assert(standalonePage);
        await standalonePage.getByRole('heading',{name:'Signal Foundry'}).waitFor();
        await standalonePage.locator('canvas').focus();
        const offline = await fullRun(standalonePage,'canvas',block.id,tiles.map(item => item.id));
        await standalonePage.screenshot({path:path.join(output,'tile-offline.png')});
        await standalonePage.locator('#restart').click();
        assert.equal((await physicsState(standalonePage,'canvas')).collected,'0');

        const proof = {format:'fraynix.tile-workshop-playtest-v1', platform:process.platform, level:primary.id,
            secondLevel:secondary.id, libraryIds:[amber,cobalt], objects:objects.length, tiles:tiles.length,
            dragged:{x:draggedX,y:draggedY}, collider:block.collider, tileCollider:tiles[0].collider,
            exports, desktop, restored, offline, inset, exitLockedWithoutPickup:true, separateProjectImport:true,
            badColliderRefused:true, changedSourceRefused:true, changedBundleRefused:true,
            restartMatchedExport:true, standaloneNetworkBlocked:true};
        await fs.writeFile(path.join(output,'tile-evidence.json'),JSON.stringify(proof,null,2)+'\n');
        console.log(JSON.stringify(proof));
    } catch (error) {
        const lastStatus = await page.locator('#tile-status').textContent({timeout:1500}).catch(() => '');
        console.error('Tile editor status:', lastStatus);
        throw error;
    } finally {
        await app?.close().catch(()=>{});
    }
}
main().catch(error => {console.error(error);process.exitCode=1;});
