'use strict';

const assert = require('node:assert/strict');
const {execFile} = require('node:child_process');
const {promisify} = require('node:util');
const {createHash} = require('node:crypto');
const fs = require('node:fs/promises');
const os = require('node:os');
const path = require('node:path');
const {launch} = require('./calibration-integration.cjs');

async function main() {
    const output = path.resolve(process.argv[2] || 'build/spatial-prop');
    const fixture = process.argv[3];
    const blender = process.env.FRAYNIX_TEST_BLENDER;
    const project = await fs.mkdtemp(path.join(os.tmpdir(),'Fraynix detailed prop '));
    await fs.mkdir(output,{recursive:true});
    const source = path.join(output,'source.glb'), resultGlb = path.join(output,'copied.glb');
    const runBlender = (...args) => promisify(execFile)(blender,['--background','--factory-startup','--disable-autoexec',
        '--python-exit-code','17','--python',path.join(__dirname,'spatial-prop-blender.py'),'--',...args],{timeout:70000,windowsHide:true});
    if (blender) await runBlender('create',source);
    else if (fixture) await fs.copyFile(fixture,source);
    else throw Error('Set FRAYNIX_TEST_BLENDER or pass the checked edition 217 fixture.');
    process.env.FRAYNIX_WORKSPACE_NO_MODELS = '1';
    process.env.FRAYNIX_WORKSPACE_PERSISTENT = '0';
    const linuxArgs=process.platform==='linux'&&process.env.FRAYNIX_WORKSPACE_CI_CHROMIUM==='1'?['--no-sandbox']:[];
    let {app,page} = await launch(project,linuxArgs);
    const api=(route,data)=>app.evaluate((_,[route,data])=>global.workspaceTest.api(route,data),[route,data]);
    const call=(command,args)=>page.evaluate(([command,args])=>window.workspace.request({operation:'spatial',command,args}),[command,args]);
    const hash=bytes=>createHash('sha256').update(bytes).digest('hex');
    async function capture(name) {
        await fs.writeFile(path.join(output,name),Buffer.from(await app.evaluate(()=>global.workspaceTest.capture()),'base64'));
    }

    try {
        await page.locator('#spatial-tab').click();
        await page.locator('#spatial-exchange summary').click();
        await page.locator('#spatial-glb').setInputFiles(source);
        await page.waitForFunction(()=>document.querySelector('#spatial-scenes').selectedOptions[0]?.textContent.includes('beacon workshop'));
        const imported = await api('/api/spatial/read',{id:await page.locator('#spatial-scenes').inputValue()});
        const parent = imported.objects.find(o=>o.sourceId==='beacon');
        assert(parent.visualMesh);
        const mesh = Buffer.from(imported.visualMeshes[parent.visualMesh],'base64');
        assert.equal(hash(mesh),parent.visualMesh);
        assert(mesh.readUInt32LE(4)/3>500);
        assert.equal(imported.objects.length,2);
        assert.equal(imported.glbSha256,hash(await fs.readFile(source)));

        await page.locator('#spatial-objects').selectOption(parent.id);
        const invalidArgs={sceneId:imported.id,objectId:parent.id,label:'Blocked copy',offsetM:[.15,0,0],yawDeg:0};
        const invalid=await api('/api/spatial/prop-review',invalidArgs);
        await assert.rejects(api('/api/spatial/prop-copy',{args:invalidArgs,approvalSha256:'wrong'}),/Approve the exact/);
        await assert.rejects(api('/api/spatial/prop-copy',{args:invalidArgs,approvalSha256:invalid.approvalSha256}),/Native collision refuses/);
        assert.equal((await api('/api/spatial/list',{})).scenes.length,1);

        await page.locator('#goal').fill('Copy and transform the selected prop.');
        await page.locator('#run').click();
        await page.locator('#spatial-prop-dialog').waitFor();
        await page.locator('#spatial-prop-review').click();
        await page.waitForFunction(()=>document.querySelector('#spatial-prop-status').textContent.includes('two isolated'));
        assert.equal((await api('/api/spatial/list',{})).scenes.length,1);
        await capture('review.png');
        await page.locator('#spatial-prop-approve').click();
        try { await page.getByText('Prop copy verified', {exact:false}).last().waitFor({timeout:45000}); }
        catch (error) { throw Error(error.message+' '+await page.locator('#spatial-prop-status').textContent()); }
        const id=await page.locator('#spatial-scenes').inputValue();
        assert.notEqual(id,imported.id);
        const scene=await api('/api/spatial/read',{id});
        const {verify,replay}=scene.propCopy;
        assert.deepEqual(verify,replay);
        assert.equal(verify.collider,'BoxCollisionShape');
        assert(verify.coreContacts>0);
        assert.equal(verify.visualOnlyContacts,0);
        assert(verify.visualOverhangTested);
        assert.equal(verify.support,'floor');
        assert.deepEqual(await api('/api/spatial/read',{id:imported.id}),imported);
        assert.equal(scene.objects.find(o=>o.id===verify.objectId).visualMesh,parent.visualMesh);
        assert.equal(Object.keys(scene.visualMeshes).length,1);

        await page.locator('#spatial-colliders').check();
        await page.locator('#spatial-stage').evaluate(stage=>{
            stage.scrollIntoView({block:'center'});
            return new Promise(resolve=>requestAnimationFrame(()=>requestAnimationFrame(resolve)));
        });
        await page.locator('#spatial-stage canvas').hover();
        await page.mouse.wheel(0,-460);
        await page.locator('#spatial-stage').evaluate(()=>new Promise(resolve=>requestAnimationFrame(()=>requestAnimationFrame(resolve))));
        await capture('verified-copy.png');
        const exported=await call('export-glb',{id});
        await fs.copyFile(exported.path,resultGlb);
        assert.equal(hash(await fs.readFile(resultGlb)),exported.sha256);
        let external = null;
        if (blender) {
            const report=path.join(output,'blender-verification.json');
            await runBlender('verify',resultGlb,report);
            external=JSON.parse(await fs.readFile(report,'utf8'));
            assert(external.passed);
        }

        await app.close();
        ({app,page}=await launch(project,linuxArgs));
        assert.deepEqual((await api('/api/spatial/read',{id})).propCopy,scene.propCopy);
        const repeat=await api('/api/spatial/prop-copy',{
            args:{sceneId:imported.id,objectId:parent.id,label:scene.propCopy.review.label,
                offsetM:scene.propCopy.review.offsetM,yawDeg:scene.propCopy.review.yawDeg},approvalSha256:scene.propCopy.approvalSha256});
        assert.equal(repeat.id,id);
        assert.equal((await api('/api/spatial/list',{})).scenes.length,2);
        const proof={passed:true,native:verify,blender:external,inputSha256:imported.glbSha256,outputSha256:exported.sha256,
            review:scene.propCopy.review,checks:[
                'Visual triangles and declared collider load as distinct components.',
                'Stale approval and a colliding copy are refused before committing a scene.',
                'Chat reaches a reviewed, Java-owned copy and transform.',
                'Two isolated native worlds agree on support, the collider hit and the visual-only miss.',
                'Original geometry is preserved and the copy shares one immutable visual asset.',
                'The checked copy survives restart and repeated approval reopens the same copy.'
            ]};
        await fs.writeFile(path.join(output,'result.json'),JSON.stringify(proof,null,2)+'\n');
        console.log(JSON.stringify(proof));
    } catch(error) {
        console.error('App:',await page.locator('#notice').innerText().catch(()=>''));throw error;
    } finally { await app.close().catch(()=>{}); }
}
main().catch(error=>{console.error(error);process.exitCode=1;});
