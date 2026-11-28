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
const command = 'Make a six-second shot from the approved blue-crate route, clean and annotated.';
const move = 'Move the blue crate 1.20 m away from the loading door. Keep the 1 m aisle clear and protect the shelves.';

function distanceToLeg(point,leg) {
    const d=leg.toM.map((c,i)=>c-leg.fromM[i]);
    const a=point.map((c,i)=>c-leg.fromM[i]);
    const dot=(x,y)=>x.reduce((sum,c,i)=>sum+c*y[i],0);
    const t=Math.max(0,Math.min(1,dot(a,d)/dot(d,d)));
    return Math.hypot(...a.map((c,i)=>c-t*d[i]));
}

async function main() {
    Object.assign(process.env,{
        FRAYNIX_UNIFIED_TEST:'1',FRAYNIX_WORKSPACE_NO_MODELS:'1',FRAYNIX_WORKSPACE_PERSISTENT:'0',
    });
    const output = path.resolve(process.argv[2] || 'build/route-shot');
    await fs.mkdir(output,{recursive:true});
    const project = await fs.mkdtemp(path.join(os.tmpdir(),'Fraynix route shot '));
    const flags = process.platform==='linux'?['--no-sandbox','--disable-dev-shm-usage']:[];
    const errors = [];
    let app,page;
    const api = (route,args={})=>app.evaluate((_,v)=>global.workspaceTest.api(v.route,v.args),{route,args});
    const spatial = (command,args)=>api('/api/spatial/'+command,args);
    const timeline = (command,args)=>api('/api/timeline/'+command,args);
    const status = text=>page.locator('#shot-status').filter({hasText:text}).waitFor({timeout:60000});
    const visible = (selector,text)=>page.locator(selector).filter({hasText:text}).waitFor({timeout:60000});
    const shot = '#route-shot-panel';

    async function ask(text) {
        await page.locator('#goal').fill(text);
        await page.locator('#run').click();
    }

    async function start() {
        ({app,page}=await launch(project,flags));
        page.on('pageerror',error=>errors.push(error.message));
        assert.equal((await app.evaluate(()=>global.workspaceTest.network())).enabled,true);
        assert.equal((await app.evaluate(()=>global.workspaceTest.api('/api/state'))).models.length,0);
        await ask('Open Spatial.');
        await page.locator(shot).waitFor();
    }

    async function stop() {
        if (!app) return;
        await app.close();
        app=null;
    }

    async function show(name,frame,bytes) {
        if (frame!==null) {
            await page.waitForFunction(expected=>{
                const img=document.querySelector('#shot-preview');
                const index=Number(img.dataset.frame);
                return index===expected && img.complete && img.naturalWidth===640;
            },frame);
            const jpeg=await page.locator('#shot-preview').getAttribute('src');
            assert.equal(sha(Buffer.from(jpeg.split(',')[1],'base64')),bytes);
        }
        await page.locator('#route-shot-panel > summary').evaluate(node=>node.scrollIntoView({block:'start'}));
        assert(await page.locator('#shot-preview').evaluate(node=>{
            const frame=node.getBoundingClientRect();
            return frame.top>=0 && frame.bottom<=window.innerHeight;
        }),'Screenshot must include the full verified frame.');
        await page.evaluate(()=>new Promise(resolve=>requestAnimationFrame(()=>requestAnimationFrame(resolve))));
        await fs.writeFile(path.join(output,name+'.png'),Buffer.from(await app.evaluate(()=>global.workspaceTest.capture()),'base64'));
    }

    async function exported(take,id) {
        await page.locator('#shot-export-'+take).click();
        const label = 'Saved '+(take==='json'?'camera JSON':take+' AVI')+' to ';
        await status(label);
        const target=(await page.locator('#shot-status').textContent()).slice(label.length);
        const bytes=await fs.readFile(target);
        const receipt=JSON.parse(await fs.readFile(target+'.receipt.json','utf8'));
        assert.equal(receipt.source.shotId,id);
        assert.equal(receipt.source.take,take);
        assert.equal(receipt.sha256,sha(bytes));
        await fs.writeFile(path.join(output,take+(take==='json'?'.json':'.avi')),bytes);
        return receipt;
    }

    try {
        await start();
        const aisle=page.locator('#loading-aisle');
        if (!await aisle.evaluate(node=>node.open)) await aisle.locator('summary').click();
        await page.locator('#aisle-yaw').selectOption('wide-fine');
        await page.locator('#aisle-load').click();
        await visible('#aisle-status','Choose the crate');
        const parentId=await page.locator('#spatial-scenes').inputValue();
        const parent=await spatial('read',{id:parentId});
        await ask(move);
        await visible('#aisle-status','original 120 cm destination');
        const draft=await spatial('aisle-latest');
        await assert.rejects(spatial('shot-build',{routeId:draft.id}),/Approve and verify/);
        await page.getByRole('button',{name:'Approve this route',exact:true}).click();
        await visible('#aisle-status','Verified route');
        const route=await spatial('aisle-latest');
        const child=await spatial('read',{id:route.sceneId});
        await ask(command);
        await status('Verified both six-second takes');
        const saved=await spatial('shot-latest');
        const {id,audit,definition,takes}=saved;
        assert.equal(definition.routeId,route.id);
        assert.equal(definition.sceneSha256,parent.sourceSha256);
        assert.equal(definition.motion,'planned-route');
        assert.deepEqual(definition.pathM,route.review.route.pointsM);
        assert.equal(definition.fps,24);
        assert.equal(audit.frames.length,144);
        assert.equal(audit.decodedFramesPerTake,144);
        assert.equal(audit.occlusionDemonstrated,true);
        assert(audit.shelfHiddenFrames>0);
        assert(audit.firstVisibleFrame<audit.shelfHiddenFrame && audit.shelfHiddenFrame<audit.returnedFrame);
        assert.deepEqual(audit.frames[0].cratePositionM,route.review.fromM);
        assert.deepEqual(audit.frames.at(-1).cratePositionM,route.review.toM);
        const legs=route.proof.measurements.route.segments;
        audit.frames.forEach((row,i)=>{
            assert.equal(row.frame,i);
            assert.equal(row.timeSeconds,i/24);
            assert(legs.some(leg=>distanceToLeg(row.cratePositionM,leg)<1e-8));
            if (row.centerVisible) assert.notEqual(row.cleanJpegSha256,row.annotatedJpegSha256);
            if (row.fullyHiddenByShelf) {
                assert(!row.centerVisible && row.labelPixels===0);
                assert(route.review.protected.some(shelf=>shelf.sourceId===row.centerOccluder));
            }
        });

        const before = audit.frames.filter(row=>row.centerVisible && row.frame<audit.shelfHiddenFrame)
            .reduce((best,row)=>row.labelPixels>best.labelPixels?row:best);
        const fullyCovered = audit.frames.filter(row=>row.fullyHiddenByShelf && row.cleanJpegSha256===row.annotatedJpegSha256);
        const hidden = fullyCovered[Math.floor(fullyCovered.length/2)];
        assert(hidden);
        await page.locator('#shot-scrub').evaluate((slider,index)=>{
            slider.value=index;
            slider.dispatchEvent(new Event('input',{bubbles:true}));
        },before.frame);
        await show('shot-before',before.frame,before.annotatedJpegSha256);
        await page.locator('#shot-scrub').evaluate((slider,index)=>{
            slider.value=index;
            slider.dispatchEvent(new Event('input',{bubbles:true}));
        },hidden.frame);
        await visible('#shot-evidence','Label fully behind');
        await show('shot-hidden',hidden.frame,hidden.annotatedJpegSha256);
        const playStart=Date.now();
        await page.locator('#shot-play').click();
        await visible('#shot-play','Stop');
        await page.waitForFunction(()=>{
            return document.querySelector('#shot-scrub').value==='143' &&
                document.querySelector('#shot-play').textContent==='Play 6 seconds';
        },null,{timeout:15000});
        assert(Date.now()-playStart>=5500);
        const playback = await spatial('shot-playback',{id,take:'clean'});
        assert.deepEqual(playback.frames.map(jpeg=>sha(Buffer.from(jpeg,'base64'))),audit.frames.map(row=>row.cleanJpegSha256));

        const receipts={};
        for (const take of ['clean','annotated','json']) {
            receipts[take]=await exported(take,id);
            assert.match(receipts[take].source.routeEvidenceSha256,/^[a-f0-9]{64}$/);
            if (take!=='json') assert.equal(receipts[take].sha256,takes[take].sha256);
        }
        assert.deepEqual(JSON.parse(await fs.readFile(path.join(output,'json.json'))),saved);
        await fs.rename(path.join(output,'json.json'),path.join(output,'shot.json'));

        await page.locator('#shot-keys').locator('..').locator('summary').click();
        await page.locator('#shot-key-rows input[name=fov]').first().fill('44');
        await page.locator('#shot-save-keys').click();
        await status('Verified both six-second takes');
        const alternate=await spatial('shot-latest');
        assert.notEqual(alternate.id,id);
        assert.equal(alternate.definition.cameraKeys[0].fovDeg,44);
        assert.equal(alternate.definition.routeEvidenceSha256,definition.routeEvidenceSha256);
        assert.notEqual(alternate.takes.clean.sha256,takes.clean.sha256);
        assert.deepEqual(await spatial('shot-open',{id}),saved);
        await show('shot-keys',0,alternate.audit.frames[0].annotatedJpegSha256);
        await assert.rejects(spatial('shot-build',{routeId:route.id,cameraKeys:[
            {frame:0,offsetM:[0,2,2],fovDeg:40},{frame:141,offsetM:[0,2,2],fovDeg:40}
        ]}),/frames 0 and 143/);
        await spatial('shot-build',{routeId:route.id});

        await stop();
        await start();
        await page.locator(shot+' > summary').click();
        await page.locator('#shot-latest').click();
        await status('Saved camera track and both verified takes reopened.');
        assert.deepEqual(await spatial('shot-latest'),saved);
        await page.locator('#shot-replay').click();
        await status('matched all 144 frames in each take');
        await page.locator('#shot-return').click();
        await visible('#shot-evidence','Label visible');
        const returned = audit.frames.filter(row=>row.centerVisible && row.frame>=audit.returnedFrame)
            .reduce((best,row)=>row.labelPixels>best.labelPixels?row:best);
        await page.locator('#shot-scrub').evaluate((slider,index)=>{
            slider.value=index;
            slider.dispatchEvent(new Event('input',{bubbles:true}));
        },returned.frame);
        await show('shot-reopened',returned.frame,returned.annotatedJpegSha256);

        await page.locator('#shot-edit-timeline').click();
        await visible('#timeline-status','Added both verified loading-bay takes');
        const assets=(await timeline('assets')).assets.filter(a=>a.source.origin.type==='route-shot');
        assert.equal(assets.length,2);
        for (const asset of assets) {
            const bound=asset.source.origin.nativeSource;
            assert.equal(asset.pictureFrames,144);
            assert.equal(asset.source.sha256,takes[bound.take].sha256);
            assert.equal(bound.routeEvidenceSha256,definition.routeEvidenceSha256);
        }
        const selected = assets.find(a=>a.source.origin.nativeSource.take==='annotated');
        const cut=await timeline('render',{title:'Verified crate cut',strips:[
            {assetId:selected.id,lane:'PICTURE',start:0,in:hidden.frame,length:12},
        ]});
        assert.equal(cut.metrics.frames,12);

        const moviePath=path.join(project,'route-shots',id+'.annotated');
        const original=await fs.readFile(moviePath), changed=Buffer.from(original);
        changed[100]^=1;
        await fs.writeFile(moviePath,changed);
        await assert.rejects(spatial('shot-export',{id,take:'clean'}),/differs from its receipt/);
        await assert.rejects(timeline('open',{id:cut.id}),/differs from its receipt|differs from its verified bytes/);
        await fs.writeFile(moviePath,original);
        const routePath=path.join(project,'spatial','loading-aisle',route.id+'.review.json');
        const raw=await fs.readFile(routePath);
        const contaminated=JSON.parse(raw);
        contaminated.data.route.pointsM[1][0]+=.04;
        await fs.writeFile(routePath,json(contaminated));
        await assert.rejects(spatial('shot-replay',{id}),/integrity mismatch/);
        await fs.writeFile(routePath,raw);
        assert.deepEqual(await spatial('read',{id:parentId}),parent);
        assert.deepEqual(await spatial('read',{id:route.sceneId}),child);
        assert.deepEqual(errors,[]);

        const external=[];
        if (process.env.FRAYNIX_ROUTE_SHOT_FFMPEG==='1') {
            for (const take of ['clean','annotated']) {
                const file=path.join(output,take+'.avi');
                const probe=execFileSync('ffprobe',['-v','error','-count_frames','-select_streams','v:0',
                    '-show_entries','stream=width,height,r_frame_rate,duration,nb_read_frames','-of','json',file],{encoding:'utf8'});
                const video=JSON.parse(probe).streams[0];
                assert.deepEqual(video,{width:640,height:360,r_frame_rate:'24/1',duration:'6.000000',nb_read_frames:'144'});
                execFileSync('ffmpeg',['-v','error','-xerror','-i',file,'-f','null','-'],{stdio:'pipe'});
                external.push({take,...video});
            }
        }
        const result={passed:true,platform:process.platform,offlineRestarts:1,frameCount:144,
            cleanAndAnnotatedVerified:true,approvedRouteUnchanged:true,cameraEditKeptOriginal:true,
            restartReplay:true,wrongApprovalRefused:true,changedMovieRefused:true,changedRouteRefused:true,
            timelineFrames:cut.metrics.frames,shotId:id,alternateShotId:alternate.id,routeId:route.id,
            beforeFrame:before.frame,hiddenFrame:hidden.frame,returnedFrame:audit.returnedFrame,
            shelfHiddenFrames:audit.shelfHiddenFrames,returnedScreenshotFrame:returned.frame,routeLegs:legs.length,takes,external};
        await fs.writeFile(path.join(output,'result.json'),json(result));
        console.log(json(result));
    } catch (error) {
        if (page && !page.isClosed()) await fs.writeFile(path.join(output,'failure.txt'),await page.locator('body').innerText());
        throw error;
    } finally {
        await stop();
        await fs.rm(project,{recursive:true,force:true});
    }
}

if (require.main===module) main().catch(error=>{console.error(error); process.exitCode=1;});
