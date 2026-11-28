'use strict';

const assert = require('node:assert/strict');
const {createHash} = require('node:crypto');
const {execFileSync} = require('node:child_process');
const fs = require('node:fs/promises');
const os = require('node:os');
const path = require('node:path');
const {launch} = require('./calibration-integration.cjs');

const sha = bytes => createHash('sha256').update(bytes).digest('hex');
const json = value => JSON.stringify(value,null,2)+'\n';
const command = 'Film a six-second shot from the approved amber crate route.';
const move = 'Move the crate 1.73 m away from the loading door. Keep the 1 m aisle clear and protect the shelves.';

function checkAnchor(shot,object) {
    const {localOffsetM:local} = shot.definition.overlays.label;
    const [,y,,w] = object.rotation;
    const cos = 1-2*y*y, sin = 2*w*y;
    const displacement = [local[0]*cos+local[2]*sin,local[1],local[2]*cos-local[0]*sin];
    assert(Math.abs(sin)>.9);
    for (const frame of shot.audit.frames) {
        assert(frame.labelEnabled);
        frame.labelAnchorM.forEach((corner,i)=>{
            assert(Math.abs(corner-frame.cratePositionM[i]-displacement[i])<1e-8);
        });
    }
}

async function main() {
    Object.assign(process.env,{
        FRAYNIX_UNIFIED_TEST:'1',FRAYNIX_WORKSPACE_NO_MODELS:'1',FRAYNIX_WORKSPACE_PERSISTENT:'0',
    });
    const output=path.resolve(process.argv[2]||'build/route-shot-portable');
    await fs.mkdir(output,{recursive:true});
    const projects=await fs.mkdtemp(path.join(os.tmpdir(),'Fraynix portable shot '));
    const source=path.join(projects,'author'), target=path.join(projects,'recipient');
    const flags=process.platform==='linux'?['--no-sandbox','--disable-dev-shm-usage']:[];
    let app,page;
    const errors=[];
    const api=(route,args={})=>app.evaluate((_,v)=>global.workspaceTest.api(v.route,v.args),{route,args});
    const shot=(command,args)=>api('/api/spatial/shot-'+command,args);
    const status=text=>page.locator('#shot-status').filter({hasText:text}).waitFor({timeout:60000});
    const aisleStatus=text=>page.locator('#aisle-status').filter({hasText:text}).waitFor({timeout:60000});
    const input=name=>page.locator('#shot-overlay-form [name="'+name+'"]');
    const evidence=()=>page.locator('#shot-evidence').innerText();

    async function open(project) {
        await fs.mkdir(project,{recursive:true});
        ({app,page}=await launch(project,flags));
        page.on('pageerror',error=>errors.push(error.message));
        assert.equal((await app.evaluate(()=>global.workspaceTest.network())).enabled,true);
        await ask('Open Spatial.');
        await page.locator('#route-shot-panel').waitFor();
    }

    async function close() {
        if (!app) return;
        await app.close();
        app=null;
    }

    async function ask(text) {
        await page.locator('#goal').fill(text);
        await page.locator('#run').click();
    }

    async function openDetails(selector) {
        const node=page.locator(selector);
        if (!await node.evaluate(element=>element.open)) await node.locator('summary').first().click();
    }

    async function setFrame(row,take='annotated') {
        await page.locator('#shot-take').selectOption(take);
        await page.locator('#shot-scrub').evaluate((slider,value)=>{
            slider.value=value;
            slider.dispatchEvent(new Event('input',{bubbles:true}));
        },row.frame);
        await page.waitForFunction(index=>{
            const image=document.querySelector('#shot-preview');
            return image.complete && image.naturalWidth===640 && Number(image.dataset.frame)===index;
        },row.frame);
        const jpg=(await page.locator('#shot-preview').getAttribute('src')).split(',')[1];
        assert.equal(sha(Buffer.from(jpg,'base64')),row[take+'JpegSha256']);
    }

    async function picture(name,focus) {
        await page.locator(focus).evaluate(node=>node.scrollIntoView({block:'start'}));
        await page.evaluate(()=>new Promise(resolve=>requestAnimationFrame(()=>requestAnimationFrame(resolve))));
        const png=Buffer.from(await app.evaluate(()=>global.workspaceTest.capture()),'base64');
        await fs.writeFile(path.join(output,name+'.png'),png);
    }

    async function saveBundle(expected) {
        await page.locator('#shot-export-bundle').click();
        const prefix='Saved portable shot to ';
        await status(prefix);
        const filename=(await page.locator('#shot-status').textContent()).slice(prefix.length);
        assert(filename.endsWith('.fraynix-shot.json'));
        const file=await fs.readFile(filename);
        const receipt=JSON.parse(await fs.readFile(filename+'.receipt.json','utf8'));
        assert.equal(receipt.sha256,sha(file));
        assert.equal(receipt.source.shotId,expected.id);
        return file;
    }

    async function absentWorld() {
        assert.equal((await api('/api/spatial/list')).scenes.length,0);
        assert.deepEqual(await api('/api/spatial/aisle-latest'),{});
    }

    try {
        await open(source);
        await openDetails('#loading-aisle');
        await page.locator('#aisle-yaw').selectOption('film-bay');
        await page.locator('#aisle-load').click();
        await aisleStatus('Choose the crate');
        const sceneId=await page.locator('#spatial-scenes').inputValue();
        const scene=await api('/api/spatial/read',{id:sceneId});
        assert.match(scene.title,/Night dispatch bay/);
        const crate=scene.objects.find(o=>o.sourceId==='flight-case');
        assert.equal(crate.label,'Amber crate');
        assert.equal(crate.massKg,9.5);
        assert.deepEqual(crate.size,[.58,.48,.26]);
        await ask(move);
        await aisleStatus('Which crate should I move?');
        const question=await api('/api/spatial/aisle-latest');
        assert(question.choices.length>1);
        const select=page.getByRole('button',{name:'Amber crate',exact:true});
        await select.click();
        await aisleStatus('original 173 cm destination');
        const draft=await api('/api/spatial/aisle-latest');
        await page.getByRole('button',{name:'Approve this route',exact:true}).click();
        await aisleStatus('Verified route');
        const approved=await api('/api/spatial/aisle-latest');
        assert.equal(approved.id,draft.id);
        assert.equal(approved.review.crate,'flight-case');
        assert.equal(approved.review.intent.questionId,question.id);
        assert.equal(approved.proof.measurements.shelvesPreserved,2);
        assert.equal(approved.proof.measurements.widthM,1);
        const child=await api('/api/spatial/read',{id:approved.sceneId});

        await ask(command);
        await status('Verified both six-second takes');
        const baseline=await shot('latest');
        await openDetails('#shot-overlays');
        await input('labelText').fill('CASE A17');
        await input('anchorX').fill('0.09');
        await input('anchorY').fill('0.37');
        await input('anchorZ').fill('0.06');
        await input('labelWidth').fill('0.76');
        await input('labelHeight').fill('0.18');
        await input('labelColor').fill('#f3c872');
        await input('lineColor').fill('#e5a8ff');
        await input('lineWidth').fill('0.06');
        await input('lineElevation').fill('0.023');
        await page.locator('#shot-save-overlays').click();
        await status('Verified both six-second takes');
        const edited=await shot('latest');
        assert.notEqual(edited.id,baseline.id);
        assert.equal(edited.definition.overlays.label.text,'CASE A17');
        assert.equal(edited.definition.overlays.routeLine.color,'#e5a8ff');
        assert.deepEqual(edited.definition.cameraKeys,baseline.definition.cameraKeys);
        assert.deepEqual(edited.takes.clean,baseline.takes.clean);
        assert.notDeepEqual(edited.takes.annotated,baseline.takes.annotated);
        assert.equal(edited.definition.routeEvidenceSha256,baseline.definition.routeEvidenceSha256);
        assert.equal(edited.audit.frames.length,144);
        assert.equal(edited.audit.occlusionDemonstrated,true);
        assert(edited.audit.frames.some(row=>row.routePixels>0));
        checkAnchor(edited,crate);
        const before=edited.audit.frames.filter(row=>row.centerVisible && row.frame<edited.audit.shelfHiddenFrame)
            .reduce((best,row)=>row.labelPixels>best.labelPixels?row:best);
        const hidden=edited.audit.frames.filter(row=>row.fullyHiddenByShelf &&
            row.centerOccluder==='dispatch-shelf' && row.routePixels===0);
        assert(hidden.length>0);
        const covered=hidden[Math.floor(hidden.length/2)];
        assert.equal(covered.cleanJpegSha256,covered.annotatedJpegSha256);
        await setFrame(before);
        await picture('overlay-editor','#shot-overlays > summary');
        await openDetails('#shot-overlays');
        await page.locator('#shot-overlays > summary').click();
        await picture('dispatch-visible','#route-shot-panel > summary');
        await setFrame(covered);
        assert.match(await evidence(),/dispatch-shelf.*0 label pixels.*0 route pixels/);
        await picture('dispatch-occluded','#route-shot-panel > summary');

        const packed=await saveBundle(edited);
        const packPath=path.join(output,'dispatch.fraynix-shot.json');
        await fs.writeFile(packPath,packed);
        const bundle=JSON.parse(packed);
        assert.deepEqual(bundle.shot,edited);
        assert.equal(bundle.route.sourceText,scene.sourceText);
        assert.deepEqual(bundle.route.mission,approved);
        const {id:questionId,...questionBody}=question;
        assert.deepEqual(bundle.route.question,questionBody);
        assert.equal(bundle.route.approval.reviewSha256,approved.id);

        for (const take of ['clean','annotated']) {
            const bytes=Buffer.from(bundle.video[take],'base64');
            assert.equal(sha(bytes),edited.takes[take].sha256);
            await fs.writeFile(path.join(output,take+'.avi'),bytes);
        }
        await fs.writeFile(path.join(output,'shot.json'),json(edited));
        assert.deepEqual(await api('/api/spatial/read',{id:sceneId}),scene);
        assert.deepEqual(await api('/api/spatial/read',{id:approved.sceneId}),child);
        await close();

        await open(target);
        await absentWorld();
        assert.deepEqual(await shot('latest'),{});
        const changed=structuredClone(bundle);
        changed.route.question.choices[0].label='Changed crate';
        await assert.rejects(shot('import',{bundle:changed}),/differs from the original question/);
        changed.route=bundle.route;
        const foreign=Buffer.from(changed.video.annotated,'base64');
        foreign[100]^=1;
        changed.video.annotated=foreign.toString('base64');
        changed.shot.takes.annotated.sha256=sha(foreign);
        await assert.rejects(shot('import',{bundle:changed}),/disagrees with freshly rendered frames/);
        assert.deepEqual(await shot('latest'),{});
        await absentWorld();
        await openDetails('#route-shot-panel');
        await page.locator('#shot-import').setInputFiles(packPath);
        await status('Imported and verified the portable shot and both 144-frame takes.');
        assert.deepEqual(await shot('latest'),edited);
        assert((await saveBundle(edited)).equals(packed));
        await absentWorld();
        await close();

        await open(target);
        await absentWorld();
        await openDetails('#route-shot-panel');
        await page.locator('#shot-latest').click();
        await status('Saved camera track and both verified takes reopened.');
        assert.deepEqual(await shot('latest'),edited);
        await page.locator('#shot-replay').click();
        await status('matched all 144 frames in each take');
        const returned=edited.audit.frames.filter(row=>row.centerVisible && row.frame>=edited.audit.returnedFrame)
            .reduce((best,row)=>row.labelPixels>best.labelPixels?row:best);
        await setFrame(returned);
        await picture('recipient-replay','#route-shot-panel > summary');
        assert((await saveBundle(edited)).equals(packed));

        await openDetails('#shot-overlays');
        assert.equal(await input('labelText').inputValue(),'CASE A17');
        await input('labelText').fill('A17 RETURNED');
        await input('lineEnabled').uncheck();
        await page.locator('#shot-save-overlays').click();
        await status('Verified both six-second takes');
        const variant=await shot('latest');
        assert.notEqual(variant.id,edited.id);
        assert.deepEqual(variant.takes.clean,edited.takes.clean);
        assert.notDeepEqual(variant.takes.annotated,edited.takes.annotated);
        assert(variant.audit.frames.every(row=>row.routePixels===0));
        assert.equal(variant.definition.routeEvidenceSha256,edited.definition.routeEvidenceSha256);
        assert.deepEqual(await shot('open',{id:edited.id}),edited);
        await absentWorld();

        const withoutGraphics=structuredClone(variant.definition.overlays);
        withoutGraphics.label.enabled=false;
        const control=await shot('build',{routeId:variant.definition.routeId,cameraKeys:variant.definition.cameraKeys,overlays:withoutGraphics});
        assert.deepEqual(control.takes.annotated,control.takes.clean);
        assert.equal(control.audit.occlusionDemonstrated,false);
        assert(control.audit.frames.every(row=>!row.labelEnabled && row.labelPixels===0 &&
            row.routePixels===0 && row.centerOccluder===''));
        assert.deepEqual(await shot('open',{id:edited.id}),edited);

        const external=[];
        if (process.env.FRAYNIX_ROUTE_SHOT_FFMPEG==='1') {
            for (const take of ['clean','annotated']) {
                const file=path.join(output,take+'.avi');
                const response=execFileSync('ffprobe',['-v','error','-count_frames','-select_streams','v:0',
                    '-show_entries','stream=r_frame_rate,duration,nb_read_frames','-of','json',file],{encoding:'utf8'});
                const video=JSON.parse(response).streams[0];
                assert.deepEqual(video,{r_frame_rate:'24/1',duration:'6.000000',nb_read_frames:'144'});
                execFileSync('ffmpeg',['-v','error','-xerror','-i',file,'-f','null','-'],{stdio:'pipe'});
                external.push({take,...video});
            }
        }
        assert.deepEqual(errors,[]);
        const result={passed:true,platform:process.platform,sourceScene:scene.title,object:crate.sourceId,
            routeLegs:approved.proof.measurements.route.segmentCount,aisleM:approved.proof.measurements.widthM,
            protectedShelves:approved.proof.measurements.shelvesPreserved,
            portableShotId:edited.id,recipientRevisionId:variant.id,frameCount:144,
            hiddenByDispatchShelf:edited.audit.frames.filter(row=>row.fullyHiddenByShelf&&row.centerOccluder==='dispatch-shelf').length,
            beforeFrame:before.frame,coveredFrame:covered.frame,returnedFrame:returned.frame,
            packageBytes:packed.length,packageSha256:sha(packed),takes:edited.takes,
            changedQuestionRefused:true,rehashedForeignMediaRefused:true,
            recipientRestart:true,recipientRevision:true,recipientWorldUntouched:true,disableBothMatchedClean:true,external};
        await fs.writeFile(path.join(output,'result.json'),json(result));
        console.log(json(result));
    } catch (error) {
        if (page && !page.isClosed()) await fs.writeFile(path.join(output,'failure.txt'),await page.locator('body').innerText());
        throw error;
    } finally {
        await close();
        await fs.rm(projects,{recursive:true,force:true});
    }
}

main().catch(error=>{console.error(error);process.exitCode=1;});
