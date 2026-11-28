'use strict';

const assert=require('node:assert/strict');
const {createHash}=require('node:crypto');
const {execFile}=require('node:child_process');
const fs=require('node:fs/promises');
const os=require('node:os');
const path=require('node:path');
const {promisify}=require('node:util');
const {launch}=require('./calibration-integration.cjs');
const {findBlender}=require('../blender-exchange.cjs');

const execute=promisify(execFile);
const json=value=>JSON.stringify(value,null,2)+'\n';
const sha=bytes=>createHash('sha256').update(bytes).digest('hex');
const angleDeg=(a,b)=>Math.acos(Math.max(-1,Math.min(1,a.reduce((sum,v,i)=>sum+v*b[i],0))))*180/Math.PI;

async function main() {
    Object.assign(process.env,{FRAYNIX_UNIFIED_TEST:'1',FRAYNIX_WORKSPACE_NO_MODELS:'1',FRAYNIX_WORKSPACE_PERSISTENT:'0'});
    const output=path.resolve(process.argv[2]||'build/timed-route-shot');
    await fs.mkdir(output,{recursive:true});
    const root=await fs.mkdtemp(path.join(os.tmpdir(),'Fraynix timed shot '));
    const author=path.join(root,'author'),recipient=path.join(root,'recipient');
    const flags=process.platform==='linux'?['--no-sandbox','--disable-dev-shm-usage']:[];
    const errors=[];
    let app,page;
    const api=(route,args={})=>app.evaluate((_,v)=>global.workspaceTest.api(v.route,v.args),{route,args});
    const spatial=(command,args)=>api('/api/spatial/'+command,args);
    const shot=(command,args)=>spatial('shot-'+command,args);
    const status=text=>page.locator('#shot-status').filter({hasText:text}).waitFor({timeout:90000});
    const aisle=text=>page.locator('#aisle-status').filter({hasText:text}).waitFor({timeout:90000});
    const field=(index,name)=>page.locator('#shot-track-rows fieldset').nth(index).locator('[name="'+name+'"]');

    async function open(project) {
        await fs.mkdir(project,{recursive:true});
        ({app,page}=await launch(project,flags));
        page.on('pageerror',error=>errors.push(error.message));
        assert.equal((await app.evaluate(()=>global.workspaceTest.network())).enabled,true);
        await ask('Open Spatial.');
        await page.locator('#route-shot-panel').waitFor();
    }

    async function stop() {
        if (!app) return;
        await app.close();
        app=null;
    }

    async function ask(text) {
        await page.locator('#goal').fill(text);
        await page.locator('#run').click();
    }

    async function details(selector) {
        const item=page.locator(selector);
        if (!await item.evaluate(node=>node.open)) await item.locator('summary').first().click();
    }

    async function capture(name,selector) {
        await page.locator(selector).evaluate(node=>node.scrollIntoView({block:'start'}));
        await page.evaluate(()=>new Promise(resolve=>requestAnimationFrame(()=>requestAnimationFrame(resolve))));
        await fs.writeFile(path.join(output,name+'.png'),Buffer.from(await app.evaluate(()=>global.workspaceTest.capture()),'base64'));
    }

    async function exportBundle(label,expected) {
        await page.locator('#shot-export-bundle').click();
        const prefix='Saved portable shot to ';
        await status(prefix);
        const filename=(await page.locator('#shot-status').textContent()).slice(prefix.length);
        const bytes=await fs.readFile(filename);
        assert.deepEqual(JSON.parse(bytes).shot,expected);
        const copy=path.join(output,label+'.fraynix-shot.json');
        await fs.writeFile(copy,bytes);
        return {copy,bytes};
    }

    async function emptyWorld() {
        assert.deepEqual((await spatial('list')).scenes,[]);
        assert.deepEqual(await spatial('aisle-latest'),{});
    }

    function timing(shot,expected) {
        const def=shot.definition;
        const labels=def.overlays.labels;
        assert.deepEqual(labels.map(cue=>[cue.firstFrame,cue.endExclusive]),expected);
        assert.equal(shot.audit.frames.length,144);
        for (const frame of shot.audit.frames) {
            for (const [i,row] of frame.labels.entries()) {
                const cue=labels[i];
                const active=cue.firstFrame<=frame.frame && frame.frame<cue.endExclusive;
                assert.equal(row.id,cue.id);
                assert.equal(row.active,active);
                if (!active) assert.equal(row.pixels,0);
            }
        }
        assert(shot.audit.frames.some(row=>row.labels.every(cue=>cue.centerVisible)));
    }

    try {
        await open(author);
        await details('#loading-aisle');
        await page.locator('#aisle-yaw').selectOption('film-bay');
        await page.locator('#aisle-load').click();
        await aisle('Choose the crate');
        await ask('Move the amber crate 1.73 m away from the loading door. Keep the 1 m aisle clear and protect the shelves.');
        await aisle('original 173 cm destination');
        await page.getByRole('button',{name:'Approve this route',exact:true}).click();
        await aisle('Verified route');
        const route=await spatial('aisle-latest');
        await ask('Film a six-second shot from the approved amber crate route.');
        await status('Verified both six-second takes');
        const baseline=await shot('latest');
        await details('#shot-label-tracks');
        await field(0,'text').fill('CASE A17');
        await field(0,'widthM').fill('0.76');
        await field(0,'firstFrame').fill('8');
        await field(0,'endExclusive').fill('136');
        await page.locator('#shot-add-label').click();
        assert.equal(await field(1,'target').inputValue(),'spare');
        await field(1,'text').fill('RESERVE');
        await field(1,'firstFrame').fill('18');
        await field(1,'endExclusive').fill('105');
        await page.locator('#shot-save-tracks').click();
        await status('Verified both six-second takes');
        const authored=await shot('latest');
        assert.equal(authored.definition.format,'fraynix.loading-route-shot-v3');
        timing(authored,[[8,136],[18,105]]);
        assert.deepEqual(authored.takes.clean,baseline.takes.clean);
        assert.notDeepEqual(authored.takes.annotated,baseline.takes.annotated);
        const invalid=structuredClone(authored.definition.overlays);
        invalid.labels[1].endExclusive=invalid.labels[1].firstFrame;
        await assert.rejects(shot('build',{routeId:route.id,overlays:invalid}),/endExclusive/);
        const original=await exportBundle('author',authored);
        await stop();

        await open(recipient);
        await emptyWorld();
        await details('#route-shot-panel');
        await page.locator('#shot-import').setInputFiles(original.copy);
        await status('Imported and verified the portable shot and both 144-frame takes.');
        assert.deepEqual(await shot('latest'),authored);
        await details('#shot-label-tracks');
        await field(0,'firstFrame').fill('11');
        await field(0,'endExclusive').fill('132');
        await field(1,'text').fill('BAY RESERVE');
        await field(1,'firstFrame').fill('12');
        await field(1,'endExclusive').fill('32');
        await page.locator('#shot-save-tracks').click();
        await status('Verified both six-second takes');
        const edited=await shot('latest');
        assert.notEqual(edited.id,authored.id);
        timing(edited,[[11,132],[12,32]]);
        assert.deepEqual(edited.takes.clean,authored.takes.clean);
        assert.notDeepEqual(edited.takes.annotated,authored.takes.annotated);
        assert.equal(edited.definition.routeEvidenceSha256,authored.definition.routeEvidenceSha256);
        assert.deepEqual(await shot('open',{id:authored.id}),authored);
        await emptyWorld();
        await capture('recipient-timing','#shot-label-tracks > summary');
        const packed=await exportBundle('recipient',edited);
        await stop();

        await open(recipient);
        await emptyWorld();
        await details('#route-shot-panel');
        await page.locator('#shot-latest').click();
        await status('Saved camera track and both verified takes reopened.');
        assert.deepEqual(await shot('latest'),edited);
        await page.locator('#shot-replay').click();
        await status('matched all 144 frames in each take');
        await page.locator('#shot-check-angle').click();
        await status('Verified the reserved camera across 144 frames.');
        const label=page.locator('#shot-angle-text');
        assert.match(await label.innerText(),/frames where it sees/);
        await page.waitForFunction(()=>document.querySelector('#shot-angle-image').naturalWidth===640);
        await capture('reserved-camera','#shot-angle-proof');
        const exchange=await shot('blender-source',{id:edited.id});
        const reserved=exchange.reserved, witness=reserved.witnessFrame;
        assert.deepEqual(exchange.shot,edited);
        assert(witness>=0 && reserved.shelfRevealFrames>50);
        assert.notDeepEqual(reserved.cameraKeys,edited.definition.cameraKeys);
        const min=Math.min(...reserved.frames.map((row,i)=>{
            assert.deepEqual(row.cratePositionM,edited.audit.frames[i].cratePositionM);
            assert.deepEqual(row.labels.map(x=>x.active),edited.audit.frames[i].labels.map(x=>x.active));
            return angleDeg(row.cameraForwardM,edited.audit.frames[i].cameraForwardM);
        }));
        assert(Math.abs(min-reserved.minimumAngleDeg)<1e-7 && min>50);
        assert.equal(edited.audit.frames[witness].labels[0].sceneOccluder,'dispatch-shelf');
        assert.equal(reserved.frames[witness].labels[0].centerVisible,true);
        const replayed=await exportBundle('recipient-replayed',edited);
        assert(replayed.bytes.equals(packed.bytes));
        await emptyWorld();

        const exchangeFile=path.join(output,'recipient-blender-source.json');
        await fs.writeFile(exchangeFile,json(exchange));
        let blender=null;
        if (process.env.FRAYNIX_ROUTE_SHOT_BLENDER==='1') {
            const blenderFile=path.join(output,'recipient-shot.blend');
            await app.evaluate(({dialog},file)=>{
                dialog.showSaveDialog=async()=>({canceled:false,filePath:file});
            },blenderFile);
            await page.locator('#shot-export-blender').click();
            await status('Saved editable Blender shot');
            const exe=await findBlender();
            const script=path.join(__dirname,'route-shot-blender.py');
            const result=await execute(exe,['--background','--factory-startup','--disable-autoexec',blenderFile,
                '--python-exit-code','21','--python',script,'--',exchangeFile,output],
            {windowsHide:true,timeout:90000,maxBuffer:512*1024});
            await fs.writeFile(path.join(output,'blender.log'),result.stdout+result.stderr);
            blender=JSON.parse(await fs.readFile(path.join(output,'blender-proof.json'),'utf8'));
            assert.equal(blender.shotId,edited.id);
            assert.equal(blender.reopenedBlend,true);
            assert.deepEqual(blender.views.map(view=>view.frames),[144,144]);
            assert(blender.views.every(view=>Object.values(view.labels).map(cue=>cue.activeFrames).join(',')==='121,20'));
            assert(blender.renders.every(render=>render.frame===witness));
            blender.blendSha256=sha(await fs.readFile(blenderFile));
        }
        assert.deepEqual(errors,[]);
        const result={passed:true,platform:process.platform,authorId:authored.id,recipientId:edited.id,
            recipientPackageSha256:sha(packed.bytes),recipientPackageBytes:packed.bytes.length,restartMatched:true,
            authoredCues:authored.definition.overlays.labels,recipientCues:edited.definition.overlays.labels,routeLegs:route.proof.measurements.route.segmentCount,
            minimumAngleDeg:min,shelfRevealFrames:reserved.shelfRevealFrames,witnessFrame:witness,
            recipientWorldUntouched:true,invalidTimingRefused:true,blender};
        await fs.writeFile(path.join(output,'result.json'),json(result));
        console.log(json(result));
    } catch (error) {
        if (page && !page.isClosed()) await fs.writeFile(path.join(output,'failure.txt'),await page.locator('body').innerText());
        throw error;
    } finally {
        await stop();
        await fs.rm(root,{recursive:true,force:true});
    }
}

main().catch(error=>{console.error(error);process.exitCode=1;});
