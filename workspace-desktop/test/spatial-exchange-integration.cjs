'use strict';

const assert = require('node:assert/strict');
const fs = require('node:fs/promises');
const path = require('node:path');
const os = require('node:os');
const {execFile} = require('node:child_process');
const {promisify} = require('node:util');
const {createHash} = require('node:crypto');
const {launch} = require('./calibration-integration.cjs');
const runFile = promisify(execFile);

async function main() {
    const output = path.resolve(process.argv[2] || 'build/spatial-exchange');
    const project = await fs.mkdtemp(path.join(os.tmpdir(),'Fraynix spatial exchange '));
    await fs.mkdir(output,{recursive:true});
    process.env.FRAYNIX_WORKSPACE_NO_MODELS = '1';
    process.env.FRAYNIX_WORKSPACE_PERSISTENT = '0';
    const args = process.platform === 'linux' && process.env.FRAYNIX_WORKSPACE_CI_CHROMIUM === '1' ? ['--no-sandbox'] : [];
    let {app,page} = await launch(project,args);
    const api = (route,data) => app.evaluate((_,[route,data])=>global.workspaceTest.api(route,data),[route,data]);
    const call = (command,args) => page.evaluate(([command,args])=>window.workspace.request({operation:'spatial',command,args}),[command,args]);
    const execute = (tool,args) => app.evaluate((_,[tool,args])=>global.workspaceTest.execute(tool,args),[tool,args]);
    const hash = bytes => createHash('sha256').update(bytes).digest('hex');
    const checks = [];
    const capture = async name => fs.writeFile(path.join(output,name),Buffer.from(await app.evaluate(()=>global.workspaceTest.capture()),'base64'));
    const near = (a,b) => assert(Math.abs(a-b)<1e-5,String(a)+' differs from '+b);
    const selected = () => page.locator('#spatial-scenes').inputValue();
    const marker = scene => scene.objects.find(obj=>obj.sourceId==='marker');
    async function frameScene() {
        await page.locator('#spatial-stage').evaluate(stage=>{
            stage.scrollIntoView({block:'center'});
            return new Promise(resolve=>requestAnimationFrame(()=>requestAnimationFrame(resolve)));
        });
    }

    try {
        const reference = await api('/api/maps/import-image',await api('/api/maps/example',{variant:0}));
        await fs.writeFile(path.join(output,'reference.png'),Buffer.from(reference.pngBase64,'base64'));
        await page.locator('#spatial-tab').click();
        await page.locator('#spatial-exchange summary').click();
        await page.locator('#spatial-blueprint').selectOption(reference.id);
        await page.locator('#spatial-blueprint-build').click();
        await page.waitForFunction(()=>document.querySelector('#spatial-scenes').selectedOptions[0]?.textContent.includes('3D copy'));
        const original = await api('/api/spatial/read',{id:await selected()});
        assert(original.source.note.includes(reference.imageSha256));
        assert.equal(original.objects.filter(obj=>obj.massKg>0).length,1);
        const {width,height,cells,walkable} = reference;
        const walls = original.objects.filter(obj=>obj.sourceId.startsWith('wall-'));
        for (let y=0;y<height;y++) for(let x=0;x<width;x++){
            const px=x-(width-1)/2, pz=y-(height-1)/2;
            const hits=walls.filter(o=>Math.abs(o.position[0]-px)<o.size[0]/2 && Math.abs(o.position[2]-pz)<o.size[2]/2).length;
            assert.equal(hits,walkable[cells[y*width+x]]?0:1,'Each original cell keeps its declared collision class');
        }
        checks.push('Every original PNG tile matches the collision footprint of the editable 3D copy.');
        await page.locator('#spatial-top-down').click();
        assert.equal(await page.locator('#spatial-top-down').getAttribute('aria-pressed'),'true');
        assert.equal(await page.locator('#spatial-observe').isDisabled(),true);
        await frameScene();
        await capture('blueprint-spatial.png');
        await page.locator('#spatial-top-down').click();

        const exported = await call('export-glb',{id:original.id});
        const glb = await fs.readFile(exported.path);
        assert.equal(hash(glb),exported.sha256);
        assert.equal(exported.source.sourceSha256,original.sourceSha256);
        const originalGlb = path.join(output,'blueprint.glb');
        await fs.copyFile(exported.path,originalGlb);
        await page.locator('#spatial-glb').setInputFiles(originalGlb);
        await page.waitForFunction(id=>document.querySelector('#spatial-scenes').value!==id,original.id);
        const copy = await api('/api/spatial/read',{id:await selected()});
        assert(copy.source.note.includes(exported.sha256));
        assert.equal(copy.glbSha256,exported.sha256);
        for (let i=0;i<copy.objects.length;i++){
            const a=original.objects[i], b=copy.objects[i];
            assert.equal(a.sourceId,b.sourceId);
            assert.notEqual(a.id,b.id);
            assert.deepEqual(a.size,b.size);
            assert.deepEqual(a.position,b.position);
            assert.equal(a.massKg,b.massKg);
        }
        const malformed = Buffer.from(glb);
        malformed.writeInt32LE(0,12);
        await assert.rejects(api('/api/spatial/import-glb',{base64:malformed.toString('base64')}),/Invalid GLB chunk/);
        checks.push('Binary GLB bytes are hashed, bad chunks are refused and a copied scene has new IDs and unchanged geometry.');

        let active = copy;
        let blender = null;
        if(process.env.FRAYNIX_TEST_BLENDER){
            const destination = path.join(output,'blender-edited.glb');
            const executable = process.env.FRAYNIX_TEST_BLENDER;
            const run = await runFile(executable,['--background','--factory-startup','--disable-autoexec',
                '--python-exit-code','17','--python',path.join(__dirname,'spatial-glb-blender.py'),'--',originalGlb,destination],
                {timeout:75000,windowsHide:true});
            const version = (await runFile(executable,['--version'],{timeout:15000,windowsHide:true}))
                .stdout.match(/^Blender [0-9][^\r\n]*/m)?.[0];
            assert(version && run.stdout.includes('export'));
            const changed = await fs.readFile(destination);
            await page.locator('#spatial-glb').setInputFiles(destination);
            await page.waitForFunction(id=>document.querySelector('#spatial-scenes').value!==id,copy.id);
            active = await api('/api/spatial/read',{id:await selected()});
            near(marker(active).position[0],marker(original).position[0]+0.15);
            near(marker(active).position[2],marker(original).position[2]);
            assert.deepEqual(marker(original).position,marker(await api('/api/spatial/read',{id:original.id})).position);
            blender = {version,sourceSha256:hash(changed),
                originalX:marker(original).position[0],importedX:marker(active).position[0]};
            checks.push('Stock Blender reparented the marker, translated its parent and exported an independently edited copy.');
            await frameScene();
            await capture('blender-return.png');
        }

        const base = marker(original).position;
        const row = Math.round(base[2]+(height-1)/2);
        const col = Math.round(base[0]+(width-1)/2);
        const neighbor = [[1,0],[-1,0],[0,1],[0,-1]].find(([dx,dz])=>walkable[cells[(row+dz)*width+col+dx]]);
        assert(neighbor);
        await page.locator('#spatial-objects').selectOption(marker(active).id);
        const g = await execute('spatial.start',{displacementM:[neighbor[0],0,neighbor[1]]});
        let state;
        const deadline=Date.now()+80000;
        do{
            state=await api('/api/goals/read',{id:g.id});
            if(['interrupted','cancelled','failed'].includes(state.status))throw Error(JSON.stringify(state));
            if(state.status!=='completed')await new Promise(r=>setTimeout(r,120));
        }while(state.status!=='completed'&&Date.now()<deadline);
        assert.equal(state.status,'completed');
        assert(state.metrics.passed);
        assert.equal(state.spatialBinding.sceneId,active.id);
        assert.equal(state.objectId,marker(active).id);
        const report=await api('/api/artifact/read',{id:state.report.id});
        assert(report.test.passed);
        await capture('native-move.png');
        checks.push('The existing agent tool binds the copy’s object, forecasts, grabs, moves, releases and passes its original verification.');

        await app.close();
        ({app,page}=await launch(project,args));
        assert.deepEqual(marker(await api('/api/spatial/read',{id:active.id})),marker(active));
        assert.equal((await api('/api/goals/read',{id:g.id})).status,'completed');
        const savedGlb = path.join(project,'spatial','glb-sources',active.id+'.glb');
        const savedBytes = await fs.readFile(savedGlb);
        assert.equal(hash(savedBytes),active.glbSha256);
        try {
            const changed=Buffer.from(savedBytes);
            changed[changed.length-1]^=1;
            await fs.writeFile(savedGlb,changed);
            await assert.rejects(api('/api/spatial/read',{id:active.id}),/recheck the original GLB source/);
        } finally { await fs.writeFile(savedGlb,savedBytes); }
        checks.push('Imported identity and the verified goal survive restart. A changed retained GLB is refused.');
        const proof={passed:true,blender,checks,reference:{sha256:reference.imageSha256,width,height},
            boxedObjects:original.objects.length,glbSha256:exported.sha256,metrics:state.metrics,browserChecks:report.test.checks.length};
        await fs.writeFile(path.join(output,'result.json'),JSON.stringify(proof,null,2)+'\n');
        console.log(JSON.stringify(proof));
    } catch (error) {
        console.error('App:',await page.locator('#notice').innerText().catch(()=>''));throw error;
    } finally { await app.close().catch(()=>{}); }
}
main().catch(error=>{console.error(error);process.exitCode=1;});
