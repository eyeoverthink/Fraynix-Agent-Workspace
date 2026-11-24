
'use strict';

const assert = require('node:assert/strict');
const {createHash} = require('node:crypto');
const fs = require('node:fs/promises');
const http = require('node:http');
const os = require('node:os');
const path = require('node:path');
const {pathToFileURL} = require('node:url');
const {launch} = require('./calibration-integration.cjs');

const fixture = '<svg xmlns="http://www.w3.org/2000/svg" width="120" height="86" viewBox="0 0 120 86">' +
    '<rect width="120" height="86" fill="white"/>' +
    '<path fill="#298d9b" fill-rule="evenodd" d="M15 44 30 16H85L106 44 84 70H31Z M48 31H73V54H48Z"/>' +
    '<path fill="#eea95f" d="m19 44 11-20 13 6-10 27Z M76 31l10-9 14 21-12 10Z"/>' +
    '<path fill="#12485d" d="M36 62h49l-5 5H38Z"/></svg>';
const digest = value => createHash('sha256').update(value).digest('hex');

async function main() {
    process.env.FRAYNIX_WORKSPACE_NO_MODELS = '1';
    const output = path.resolve(process.argv[2] || 'build/visual-media229');
    const project = await fs.mkdtemp(path.join(os.tmpdir(), 'Fraynix media source '));
    const target = await fs.mkdtemp(path.join(os.tmpdir(), 'Fraynix media destination '));
    await fs.mkdir(output, {recursive:true});

    const server = http.createServer((req, res) => {
        if (req.url === '/image.svg') {
            res.setHeader('Content-Type', 'image/svg+xml');
            res.end(fixture);
        } else {
            res.setHeader('Content-Type', 'text/html; charset=utf-8');
            res.end('<style>body{margin:0;background:white}img{display:block;width:min(100%,120px);height:auto}</style>' +
                '<a href="/wrong"><img src="/image.svg" id="graphic" alt="Original Signal Beacon"></a>');
        }
    });
    await new Promise(resolve => server.listen(0, '127.0.0.1', resolve));
    const url = 'http://127.0.0.1:' + server.address().port + '/beacon';
    const args = process.platform === 'linux' ? ['--no-sandbox','--disable-dev-shm-usage'] : [];
    let {app, page} = await launch(project, args);
    const api = (route, body = {}) => app.evaluate((_, r) => global.workspaceTest.api('/api/media/' + r.route, r.body),
        {route, body});
    const mediaStatus = text => page.waitForFunction(text =>
        document.querySelector('#media-status')?.textContent.includes(text), text);
    const enter = async () => {
        await page.locator('#media-tab').click();
        await page.locator('#media-legacy-mode').click();
    };
    const canvas = (view, id) => view.locator(id);
    const state = (view, id) => canvas(view, id).evaluate(el => ({...el.dataset}));
    const scene = (view, id, prop, value) => view.waitForFunction(
        request => document.querySelector(request.id)?.dataset[request.prop] === request.value, {id, prop, value});
    const capture = async name => fs.writeFile(path.join(output, name),
        Buffer.from(await app.evaluate(() => global.workspaceTest.capture()), 'base64'));

    async function play(view, id, restart) {
        await view.locator(restart).click();
        await scene(view, id, 'playerX', '48.00');
        assert.equal((await state(view, id)).won, 'false');
        await view.keyboard.down('ArrowRight');
        await view.waitForFunction(id => Number(document.querySelector(id)?.dataset.playerX) >= 245, id);
        await scene(view, id, 'playerX', '252.00');
        await new Promise(resolve => setTimeout(resolve, 110));
        const blocked = await state(view, id);
        assert.equal(blocked.playerX, '252.00');
        assert.equal(blocked.support, 'floor');
        assert.equal(blocked.collected, 'false');
        await view.keyboard.press('Space');
        await view.waitForFunction(id => Number(document.querySelector(id)?.dataset.playerX) >= 312, id);
        await view.keyboard.up('ArrowRight');
        await scene(view, id, 'collected', 'true');
        await scene(view, id, 'support', 'platform');
        const landed = await state(view, id);
        assert.equal(landed.playerY, '210.00');
        await view.keyboard.down('ArrowRight');
        await scene(view, id, 'won', 'true');
        await view.keyboard.up('ArrowRight');
        return {blocked, landed, complete:await state(view, id)};
    }

    try {
        await app.evaluate((_, url) => global.workspaceTest.execute('browser.navigate', {url}), url + '?private=test#unused');
        await enter();
        await page.locator('#media-pick').click();
        await mediaStatus('Click the graphic');
        let clicked = false;
        for (let attempt = 0; attempt < 80 && !clicked; attempt++) {
            clicked = await app.evaluate(async ({webContents}) => {
                const browser = webContents.fromId(global.workspaceTest.surfaces().browser);
                const point = await browser.executeJavaScriptInIsolatedWorld(1001, [{code:
                    '(()=>{const item=document.querySelector("#graphic"), r=item?.getBoundingClientRect();' +
                    'return globalThis.fraynixGraphicPick&&item.complete&&item.naturalWidth&&r.width<=innerWidth?' +
                    '{x:Math.floor(r.width/2),y:Math.floor(r.height/2)}:null;})()'}]);
                if (!point) return false;
                browser.sendInputEvent({type:'mouseDown', ...point, button:'left', clickCount:1});
                browser.sendInputEvent({type:'mouseUp', ...point, button:'left', clickCount:1});
                return true;
            });
            if (!clicked) await new Promise(resolve => setTimeout(resolve, 60));
        }
        assert(clicked, 'Could not send a trusted click to the source graphic.');
        await mediaStatus('Sprite isolated');
        await page.getByText('Save the draft to play it.', {exact:true}).waitFor();
        assert.equal(await page.locator('#media-restart').isDisabled(), true);
        await page.locator('input[name="platformX"]').fill('290');
        await page.locator('#media-canvas').evaluate(c => {
            const rgb = Array.from(c.getContext('2d').getImageData(290,242,1,1).data);
            if (rgb.join(',') !== '130,178,172,255') throw Error('The platform did not move in the draft preview.');
        });
        await page.locator('input[name="platformX"]').fill('276');
        assert.equal(await app.evaluate(({webContents}) =>
            webContents.fromId(global.workspaceTest.surfaces().browser).getURL()), url + '?private=test#unused');
        await page.locator('#media-save').click();
        await mediaStatus('Saved level');
        const level = await api('latest');
        assert.equal(level.sprite.isolation, 'contrast');
        assert.equal(level.sprite.origin, url);
        const mask = await page.locator('#media-sprite').evaluate(async image => {
            await image.decode();
            const c = document.createElement('canvas');
            c.width = image.naturalWidth; c.height = image.naturalHeight;
            const ctx = c.getContext('2d');
            ctx.drawImage(image, 0, 0);
            const pixel = (x,y) => [...ctx.getImageData(x,y,1,1).data];
            return {inside:pixel(Math.floor(c.width/2), Math.floor(c.height/2)),
                corner:pixel(0,0), rail:pixel(Math.floor(c.width/2), Math.floor(c.height/8))};
        });
        assert.equal(mask.inside[3], 0);
        assert.equal(mask.corner[3], 0);
        assert.deepEqual(mask.rail, [41,141,155,255]);
        await capture('media-workshop.png');
        const desktop = await play(page, '#media-canvas', '#media-restart');
        await page.getByText('Level complete.', {exact:true}).waitFor();

        const exported = {};
        for (const [extension, name] of Object.entries({png:'beacon.png',json:'sprite-relay.json',html:'sprite-relay.html'})) {
            const button = page.locator('[data-format="'+extension+'"]');
            await button.click();
            await page.waitForFunction(extension => document.querySelector('#media-status')?.textContent.endsWith('.' + extension), extension);
            const file = (await page.locator('#media-status').textContent()).slice('Saved '.length);
            assert.equal(path.extname(file), '.' + extension);
            const bytes = await fs.readFile(file);
            const receipt = JSON.parse(await fs.readFile(file + '.receipt.json', 'utf8'));
            assert.equal(digest(bytes), receipt.sha256);
            assert.equal(receipt.source.sourceSha256, level.sprite.sourceSha256);
            assert.equal(receipt.source.spriteSha256, level.sprite.sha256);
            await fs.copyFile(file, path.join(output, name));
            exported[extension] = {sha256:receipt.sha256, bytes:bytes.length};
        }
        assert.equal(exported.png.sha256, level.sprite.sha256);
        const editable = JSON.parse(await fs.readFile(path.join(output, 'sprite-relay.json'), 'utf8'));
        assert.equal(digest(Buffer.from(editable.source.png, 'base64')), level.sprite.imageSha256);
        assert.equal(digest(await fs.readFile(path.join(output, 'sprite-relay.json'))), level.id);
        assert.equal(editable.source.url, url);
        await fs.writeFile(path.join(output, 'captured-beacon.png'), Buffer.from(editable.source.png,'base64'));

        await app.close();
        ({app, page} = await launch(project, args));
        await enter();
        await mediaStatus('Reopened saved level');
        assert.equal((await api('latest')).id, level.id);
        await page.locator('input[name="title"]').fill('Signal crossing');
        await page.locator('input[name="foregroundX"]').fill('574');
        await page.locator('#media-save').click();
        await mediaStatus('Saved level');
        const revised = await api('latest');
        assert.notEqual(revised.id, level.id);
        assert.equal(revised.sprite.sha256, level.sprite.sha256);
        assert.equal(revised.layout.foregroundX, 574);
        const source = path.join(project, 'media', revised.id + '.json');
        await fs.appendFile(source, ' ');
        await assert.rejects(api('export', {id:revised.id,format:'html'}), /saved level bytes changed/i);
        await assert.rejects(api('latest'), /saved level bytes changed/i);
        await app.close();

        ({app,page} = await launch(target,args));
        await enter();
        await page.locator('#media-level').setInputFiles(path.join(output, 'sprite-relay.json'));
        await mediaStatus('Opened editable level');
        assert.equal((await api('latest')).sprite.sha256, level.sprite.sha256);
        const changed = structuredClone(editable);
        changed.spriteSha256 = '0'.repeat(64);
        await assert.rejects(api('import', {base64:Buffer.from(JSON.stringify(changed)).toString('base64')}),
            /pixels differ/i);
        await page.locator('#media-png').setInputFiles(path.join(output, 'beacon.png'));
        await mediaStatus('Sprite isolated');
        await page.locator('#media-save').click();
        await mediaStatus('Saved level');
        const pngImport = await api('latest');
        assert.equal(pngImport.sprite.isolation, 'alpha');
        assert.equal(pngImport.sprite.sha256, level.sprite.sha256);
        await app.close();

        await new Promise((resolve,reject) => server.close(error => error ? reject(error) : resolve()));
        ({app,page} = await launch(target,args));
        const fileURL = pathToFileURL(path.join(output, 'sprite-relay.html')).href;
        await app.evaluate(async ({BrowserWindow}, fileURL) => {
            const preview = new BrowserWindow({width:920,height:610,webPreferences:{
                partition:'media-standalone-test', nodeIntegration:false, sandbox:true, contextIsolation:true
            }});
            preview.webContents.session.webRequest.onBeforeRequest((details, callback) =>
                callback({cancel:!/^(file|data):/.test(details.url)}));
            await preview.loadURL(fileURL);
        }, fileURL);
        const standalonePage = app.windows().find(win => win.url() === fileURL);
        assert(standalonePage);
        await standalonePage.getByRole('heading', {name:'Sprite Relay'}).waitFor();
        const standalone = await play(standalonePage, 'canvas', '#restart');
        await standalonePage.screenshot({path:path.join(output,'standalone-level.png')});
        await standalonePage.locator('#restart').click();
        assert.equal((await state(standalonePage, 'canvas')).collected, 'false');
        assert.equal((await state(standalonePage, 'canvas')).won, 'false');

        const report = {format:'fraynix.sprite-workshop-playtest-v1', sceneId:level.id,
            sprite:{width:level.sprite.width,height:level.sprite.height,visible:level.sprite.visible,
                imageSha256:level.sprite.imageSha256,sourceSha256:level.sprite.sourceSha256,mask},
            exports:exported, desktop, standalone, imports:{editable:true,alpha:true},
            restartRechecked:true, sourceTamperingRefused:true, changedSpriteHashRefused:true,
            liveDraftPreview:true, standaloneReset:true, standaloneNetworkBlocked:true, platform:process.platform};
        await fs.writeFile(path.join(output, 'media-evidence.json'), JSON.stringify(report,null,2) + '\n');
        console.log(JSON.stringify(report));
    } finally {
        await app?.close().catch(()=>{});
        if (server.listening) await new Promise(resolve => server.close(resolve));
    }
}
main().catch(error => { console.error(error); process.exitCode=1; });
