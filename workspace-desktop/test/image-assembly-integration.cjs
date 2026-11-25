'use strict';

const assert = require('node:assert/strict');
const {createHash} = require('node:crypto');
const fs = require('node:fs/promises');
const os = require('node:os');
const path = require('node:path');
const {launch} = require('./calibration-integration.cjs');

const hash = bytes => createHash('sha256').update(bytes).digest('hex');
const fixtures = path.join(__dirname,'fixtures');
const layout = path.join(fixtures,'image-assembly','overhead.png');
const overlap = path.join(fixtures,'image-assembly','overlap.png');

async function main() {
    process.env.FRAYNIX_WORKSPACE_NO_MODELS = '1';
    const out = path.resolve(process.argv[2] || 'build/image-assembly247');
    await fs.mkdir(out,{recursive:true});
    const project = await fs.mkdtemp(path.join(os.tmpdir(),'Fraynix image assembly '));
    const extraArgs = process.platform==='linux' ? ['--no-sandbox','--disable-dev-shm-usage'] : [];
    let {app,page} = await launch(project,extraArgs);
    const call = (route,input={}) => app.evaluate((_,command) =>
        global.workspaceTest.api('/api/spatial/'+command.route,command.input),{route,input});
    const assetPixels = id=>path.join(project,'spatial','reconstruction-sources',id+'.json');
    const capture = async name => {
        await page.evaluate(()=>new Promise(resolve=>requestAnimationFrame(()=>requestAnimationFrame(resolve))));
        const capture = await app.evaluate(()=>global.workspaceTest.capture());
        await fs.writeFile(path.join(out,name),Buffer.from(capture,'base64'));
    };
    const status = text=>page.waitForFunction(phrase=>
        document.querySelector('#image-assembly-status')?.textContent.includes(phrase),text,{timeout:12000});
    const open = async () => {
        await page.locator('#spatial-tab').click();
        const panel = page.locator('#image-assembly');
        if (!await panel.evaluate(node=>node.open)) await panel.locator('summary').click();
    };
    const restart = async () => {
        await app.close();
        ({app,page} = await launch(project,extraArgs));
        await open();
    };
    const fit = async (folder,files,elevation) => {
        const clear = page.locator('#browser-views-clear');
        if (await clear.isEnabled()) await clear.click();
        const labels = {front:'front',side:'side',top:'top',heldOut:'held-out 45°'};
        for (const [role,file] of Object.entries(files)) {
            await page.locator('#registered-png-role').selectOption(role);
            await page.locator('#registered-png-file').setInputFiles(path.join(fixtures,folder,file));
            await page.waitForFunction(text=>document.querySelector('#browser-views-status').textContent.startsWith(text),
                'Loaded '+labels[role]);
        }
        await page.locator('#browser-views-elevation').selectOption(String(elevation));
        const before = (await call('list')).scenes.length;
        await page.locator('#browser-views-build').click();
        await page.waitForFunction(count=>{
            const select = document.querySelector('#spatial-scenes');
            return select.options.length===count+1 && select.value===select.options[count].value;
        },before,{timeout:12000});
        const id = await page.locator('#spatial-scenes').inputValue();
        const source = await call('read',{id});
        for (const [role,name] of Object.entries(files)) {
            const view = source.reconstruction.views[role];
            assert.equal(view.fileName,name);
            assert.equal(view.pngSha256,hash(await fs.readFile(path.join(fixtures,folder,name))));
        }
        return source;
    };

    try {
        await page.locator('#spatial-tab').click();
        await page.locator('#spatial-browser-views summary').click();
        const first = await fit('browser-views',{front:'front.png',side:'side.png',heldOut:'held-out.png'},0);
        const second = await fit('browser-top',{front:'front.png',side:'side.png',top:'top.png',heldOut:'elevated.png'},60);
        await page.locator('#browser-views-status').evaluate(element=>element.scrollIntoView({block:'nearest'}));
        await capture('local-views-checked.png');
        await page.locator('#spatial-browser-views summary').click();
        assert.notEqual(first.reconstruction.meshSha256,second.reconstruction.meshSha256);
        assert(first.reconstruction.heldOutIou>.9 && second.reconstruction.heldOutIou>.9);
        await open();
        const prop = page.locator('#image-assembly select[name=sceneId]');
        const label = page.locator('#image-assembly input[name=label]');
        await prop.nth(0).selectOption(first.id);
        await prop.nth(1).selectOption(second.id);
        await label.nth(0).fill('Stepped bracket');
        await label.nth(1).fill('Perforated plate');
        await page.locator('#image-assembly input[name=title]').fill('Image assembly workshop');
        await page.locator('#image-assembly input[name=layout]').setInputFiles(overlap);
        await page.locator('#image-assembly-build').click();
        await status('colliders overlap');
        assert.equal((await call('list')).scenes.length,2);
        await capture('overlapping-layout-refused.png');

        await page.locator('#image-assembly input[name=layout]').setInputFiles(layout);
        await page.locator('#image-assembly-build').click();
        await status('2 props · floor contact verified twice');
        const id = await page.locator('#spatial-scenes').inputValue();
        const scene = await call('read',{id});
        assert.equal(scene.title,'Image assembly workshop');
        assert.equal(scene.objects.length,3);
        const receipt = scene.imageAssembly;
        assert.deepEqual(receipt.binding.props.map(p=>p.markerCenterPx),[[110,105],[305,210]]);
        assert.equal(receipt.binding.metresPerPixel,.01);
        const expected = [[-.90,-.45],[1.05,.60]];
        for (let i=0;i<expected.length;i++) {
            const object = scene.objects[i+1];
            assert.deepEqual([object.position[0],object.position[2]],expected[i]);
            const asset = [first,second][i];
            assert.equal(object.visualMesh,asset.reconstruction.meshSha256);
            assert.equal(scene.visualMeshes[object.visualMesh],asset.visualMeshes[object.visualMesh]);
            assert.deepEqual(object.size,asset.objects[1].size);
            assert(receipt.measured[i].contactTick>0);
            assert(receipt.measured[i].speedMmps<=30);
        }
        assert.deepEqual(receipt.measured,receipt.repeat);
        await page.locator('#image-assembly summary').click();
        await page.locator('#spatial-stage').evaluate(element=>element.scrollIntoView({block:'center'}));
        await capture('image-layout-assembled.png');

        const exported = await call('export-glb',{id});
        const bytes = Buffer.from(exported.base64,'base64');
        assert.equal(hash(bytes),exported.sha256);
        const roundTrip = await call('import-glb',{base64:exported.base64});
        const authored = JSON.parse(scene.sourceText);
        const imported = JSON.parse(roundTrip.sourceText);
        assert.equal(imported.source.note,'GLB SHA-256 '+exported.sha256+'. '+authored.source.note);
        assert.deepEqual(imported,{...authored,source:{...authored.source,note:imported.source.note}});
        assert(!roundTrip.imageAssembly);

        await restart();
        await page.locator('#spatial-scenes').selectOption(id);
        await status('floor contact verified twice');
        assert.deepEqual(await call('read',{id}),scene);
        await page.locator('#image-assembly-replay').click();
        await status('Fresh floor contacts matched for all 2 props.');
        assert.equal((await call('export-glb',{id})).sha256,exported.sha256);
        await page.locator('#image-assembly-status').evaluate(element=>element.scrollIntoView({block:'nearest'}));
        await capture('image-scene-restarted.png');

        const originalLayout = await fs.readFile(layout);
        const retainedLayout = path.join(project,'spatial','image-assemblies',id+'.png');
        assert.equal(hash(originalLayout),receipt.binding.layoutSha256);
        assert((await fs.readFile(retainedLayout)).equals(originalLayout));
        await fs.writeFile(retainedLayout,Buffer.concat([originalLayout,Buffer.from('changed')]));
        await assert.rejects(call('export-glb',{id}),/recheck the layout PNG/);
        await fs.writeFile(retainedLayout,originalLayout);
        const pkg = assetPixels(second.id);
        const images = await fs.readFile(pkg);
        await fs.writeFile(pkg,Buffer.concat([images,Buffer.from('changed')]));
        await assert.rejects(call('image-assembly-replay',{id}),/recheck the layout PNG/);
        await fs.writeFile(pkg,images);
        const replay = await call('image-assembly-replay',{id});
        assert.deepEqual(replay.measured,receipt.measured);
        assert.deepEqual(await call('read',{id:first.id}),first);
        assert.deepEqual(await call('read',{id:second.id}),second);

        const proof = {platform:process.platform,layoutSha256:receipt.binding.layoutSha256,
            sceneSha256:scene.sourceSha256,glbSha256:exported.sha256,props:receipt.binding.props,
            measurements:receipt.measured,localPngImports:7,didRestart:true,exactGlbRoundTrip:true,
            sourceScenesPreserved:true,refusedOverlap:true,refusedChangedLayout:true,refusedChangedViewSet:true};
        await fs.writeFile(path.join(out,'image-assembly-evidence.json'),JSON.stringify(proof,null,2)+'\n');
        await fs.writeFile(path.join(out,'image-workshop.json'),scene.sourceText);
        await fs.writeFile(path.join(out,'image-workshop.glb'),bytes);
        console.log(JSON.stringify(proof));
    } catch (error) {
        await capture('failed.png').catch(()=>{});
        console.error('Temporary test project',project);
        throw error;
    } finally {
        await app.close();
    }
}

main().catch(error=>{console.error(error);process.exitCode=1;});
