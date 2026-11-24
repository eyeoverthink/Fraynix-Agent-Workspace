
'use strict';

const assert = require('node:assert/strict');
const {execFile} = require('node:child_process');
const {createHash} = require('node:crypto');
const fs = require('node:fs/promises');
const os = require('node:os');
const path = require('node:path');
const {promisify} = require('node:util');
const {launch} = require('./calibration-integration.cjs');

const sha = bytes => createHash('sha256').update(bytes).digest('hex');
const close = (value, expected) => assert(Math.abs(value - expected) < 2e-6, {value, expected});

function mesh(scene) {
    const body = scene.objects.find(obj => obj.sourceId === 'surface');
    const raw = Buffer.from(scene.visualMeshes[body.visualMesh], 'base64');
    const count = raw.readUInt32LE();
    return {body, count, triangles:raw.readUInt32LE(4) / 3,
        top:Math.max(...Array.from({length:count}, (_,i) => raw.readFloatLE(12 + i*12))),
        indices:raw.subarray(8 + count*12).toString('base64')};
}

async function main() {
    process.env.FRAYNIX_WORKSPACE_NO_MODELS = '1';
    const blender = process.env.FRAYNIX_TEST_BLENDER;
    if (blender) process.env.FRAYNIX_BLENDER = blender;
    const fixture = path.join(__dirname,'fixtures','library-prop');
    const output = path.resolve(process.argv[2] || 'build/visual-library-prop231');
    const project = await fs.mkdtemp(path.join(os.tmpdir(),'Fraynix spatial sprite library '));
    await fs.mkdir(output,{recursive:true});
    const args = process.platform === 'linux' ? ['--no-sandbox','--disable-dev-shm-usage'] : [];
    let {app,page} = await launch(project,args);
    const api = (route, input={}) => app.evaluate((_,request) =>
        global.workspaceTest.api('/api/'+request.route, request.input),{route,input});
    const status = (id,text) => page.waitForFunction(r =>
        document.getElementById(r.id)?.textContent.includes(r.text), {id,text});
    const libraryStatus = text => status('library-views-status',text);
    const capture = async (panel,file) => {
        await page.locator(panel).evaluate(el=>el.scrollIntoView({block:'start'}));
        await page.evaluate(() => new Promise(done => requestAnimationFrame(() => requestAnimationFrame(done))));
        await fs.writeFile(path.join(output,file),Buffer.from(await app.evaluate(()=>global.workspaceTest.capture()),'base64'));
    };
    const selectDialogFile = (mode,file) => app.evaluate(({dialog},source)=>{
        if (source.mode === 'save') dialog.showSaveDialog = async () => ({canceled:false,filePath:source.file});
        else dialog.showOpenDialog = async () => ({canceled:false,filePaths:[source.file]});
    }, {mode,file});

    try {
        await page.locator('#media-tab').click();
        const files = ['front','side','top','elevated','wrong'];
        for (const name of files) {
            await page.waitForFunction(() => !document.getElementById('tile-png').disabled);
            await page.locator('#tile-png').setInputFiles(path.join(fixture,name+'.png'));
            await status('tile-status','Added '+name+'.png');
        }
        const catalog = (await api('media-tiles/library')).assets;
        assert.equal(catalog.length,5);
        const ids = Object.fromEntries(files.map(name=>[name,catalog.find(item=>item.label===name+'.png').id]));
        assert.equal((await api('media-tiles/sprite',{id:ids.front})).isolation,'alpha');
        await page.locator('#tile-assets button').filter({hasText:'front.png'}).click();
        await page.locator('#tile-model-3d').click();
        await page.waitForFunction(front=>document.querySelector('#library-view-front')?.value===front,ids.front);
        assert.equal(await page.locator('#spatial-library-views').evaluate(el=>el.open),true);

        await page.locator('#library-view-name').fill('Twin-slot deck plate');
        await page.locator('#library-view-side').selectOption(ids.side);
        await page.locator('#library-view-top').selectOption(ids.top);
        await page.locator('#library-view-elevation').selectOption('60');
        await page.locator('#library-view-heldOut').selectOption(ids.front);
        assert(await page.locator('#library-views-build').isDisabled());
        const request = {name:'Twin-slot deck plate',widthM:.5,heldOutElevationDeg:60,
            assets:{front:ids.front,side:ids.side,top:ids.top,heldOut:ids.front}};
        await assert.rejects(api('spatial/library-views',request),/different sprite library entries/);
        const copied = await api('media-tiles/add',{source:{filename:'copied-front.png',
            png:(await fs.readFile(path.join(fixture,'front.png'))).toString('base64')}});
        assert.notEqual(copied.id,ids.front);
        await assert.rejects(api('spatial/library-views',{...request,assets:{...request.assets,heldOut:copied.id}}),
            /separate 45° held-out view/);

        await page.locator('#library-view-heldOut').selectOption(ids.wrong);
        await page.locator('#library-views-build').click();
        await libraryStatus('Held-out view refused');
        const refusal = await page.locator('#library-views-status').textContent();
        assert.equal((await api('spatial/list')).scenes.length,0);
        await capture('#spatial-library-views','library-witness-refused.png');

        await page.locator('#library-view-heldOut').selectOption(ids.elevated);
        await page.locator('#library-views-build').click();
        await libraryStatus('held-out silhouette overlap');
        const sceneId = await page.locator('#spatial-scenes').inputValue();
        const original = await api('spatial/read',{id:sceneId});
        const originalMesh = mesh(original);
        const proof = original.reconstruction;
        assert.equal(proof.viewSource,'sprite-library');
        assert.equal(proof.heldOutElevationDeg,60);
        assert.ok(proof.heldOutIou > .9);
        assert.ok(proof.topIou > .9);
        for (const [role,name] of Object.entries({front:'front',side:'side',top:'top',heldOut:'elevated'})) {
            const hash = sha(await fs.readFile(path.join(fixture,name+'.png')));
            assert.equal(proof.views[role].assetId,ids[name]);
            assert.equal(proof.views[role].pngSha256,hash);
        }
        originalMesh.body.size.forEach((v,i)=>close(v,[.5,.1,.4][i]));
        const checked = await api('spatial/export-glb',{id:sceneId});
        const glb = Buffer.from(checked.base64,'base64');
        assert.equal(sha(glb),checked.sha256);
        await fs.writeFile(path.join(output,'library-prop.glb'),glb);
        await page.locator('#spatial-top-down').click();
        await capture('#spatial-library-views','library-prop-verified.png');

        let blenderProof;
        if (blender) {
            const native = path.join(output,'library-prop.blend');
            await fs.unlink(native).catch(error=>{if(error.code!=='ENOENT')throw error;});
            await selectDialogFile('save',native);
            await page.locator('#spatial-export-blender').click();
            await page.getByText('Editable Blender scene saved:',{exact:false}).last().waitFor();
            await promisify(execFile)(blender, ['--background','--factory-startup','--disable-autoexec',
                native,'--python-exit-code','17','--python',path.join(__dirname,'library-prop-blender.py'),'--','measure',output],
                {timeout:60000,windowsHide:true});
            blenderProof = JSON.parse(await fs.readFile(path.join(output,'blender-library-proof.json'),'utf8'));
            const edited = path.join(output,'library-prop-edited.blend');
            await selectDialogFile('open',edited);
            await page.locator('#spatial-import-blender').click();
            await page.getByText('Blender scene imported.',{exact:false}).last().waitFor();
            const copy = await api('spatial/read',{id:await page.locator('#spatial-scenes').inputValue()});
            const returned = mesh(copy);
            assert.notEqual(copy.id,original.id);
            assert.notEqual(returned.body.visualMesh,originalMesh.body.visualMesh);
            returned.body.size.forEach((v,i)=>close(v,originalMesh.body.size[i]));
            close(returned.top-originalMesh.top,.005);
            assert.equal(returned.count,originalMesh.count);
            assert.equal(returned.triangles,proof.triangles);
            assert.equal(returned.indices,originalMesh.indices);
            const blendSha = sha(await fs.readFile(edited));
            assert.ok(copy.source.note.includes(blendSha));
            Object.assign(blenderProof,{importedAs:copy.id,exportedBlendSha256:sha(await fs.readFile(native)),
                editedBlendSha256:blendSha,returnedVisualTopM:returned.top,returnedColliderM:returned.body.size});
            await capture('#spatial-stage','library-prop-returned.png');
        }

        await app.close();
        ({app,page} = await launch(project,args));
        const restored = await api('spatial/read',{id:sceneId});
        assert.deepEqual(restored,original);
        assert.equal((await api('spatial/export-glb',{id:sceneId})).sha256,checked.sha256);

        const record = path.join(project,'media','tiles','sprites',ids.top+'.json');
        const stored = await fs.readFile(record);
        await fs.appendFile(record,' ');
        await assert.rejects(api('spatial/export-glb',{id:sceneId}),/original visual reconstruction/);
        await assert.rejects(api('spatial/library-views',{...request,assets:{...request.assets,heldOut:ids.elevated}}),/bytes changed/i);
        await fs.writeFile(record,stored);
        const retained = path.join(project,'spatial','reconstruction-sources',sceneId+'.json');
        const retainedBytes = await fs.readFile(retained);
        assert.equal(sha(retainedBytes),proof.sourceSha256);
        await fs.writeFile(path.join(output,'library-view-package.json'),retainedBytes);
        await fs.appendFile(retained,' ');
        await assert.rejects(api('spatial/read',{id:sceneId}),/original visual reconstruction/);

        const evidence = {format:'fraynix.library-prop-playtest-v1',platform:process.platform,sceneId,
            proof,refusal,colliderM:originalMesh.body.size,exportSha256:checked.sha256,
            blender:blenderProof||null,restartMatched:true,duplicateEntryRefused:true,relabelledWitnessRefused:true,
            changedLibrarySourceRefused:true,changedRetainedPackageRefused:true};
        await fs.writeFile(path.join(output,'library-prop-evidence.json'),JSON.stringify(evidence,null,2)+'\n');
        console.log(JSON.stringify({overlap:proof.heldOutIou,triangles:proof.triangles,refusal,blender:blenderProof||null}));
    } catch (error) {
        console.error('Last library status:',await page.locator('#library-views-status').textContent({timeout:1500}).catch(()=>''));
        throw error;
    } finally {
        await app?.close().catch(()=>{});
    }
}
main().catch(error=>{console.error(error);process.exitCode=1;});
