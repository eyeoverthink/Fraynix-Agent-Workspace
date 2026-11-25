'use strict';

const assert = require('node:assert/strict');
const {createHash} = require('node:crypto');
const fs = require('node:fs/promises');
const os = require('node:os');
const path = require('node:path');
const {pathToFileURL} = require('node:url');
const {launch} = require('./calibration-integration.cjs');

const sha = bytes => createHash('sha256').update(bytes).digest('hex');
const command = 'Put this verified bracket into my existing level. Keep the aisle clear, test contact, and export a playable level.';
const box = (sourceId,label,position,size,massKg = 0) => ({
    sourceId,label,shape:'box',position,size,rotation:[0,0,0,1],massKg,friction:.65,restitution:0
});

function level(title, height, extra = []) {
    return {
        format:'fraynix.spatial-scene',version:1,title,units:'m',frame:'right-handed-y-up',
        source:{kind:'synthetic',physicalProperties:'declared',note:'Original workshop clearance test.'},
        scaleReference:{a:[0,height,0],b:[1,height,0],knownLengthM:1,toleranceM:0,basis:'declared-test'},
        gravityMps2:9.81,
        objects:[
            box('floor','Workshop floor',[0,height-.1,0],[8,.2,5]),
            box('post','Aisle side post',[-2.4,height+.38,.1],[.3,.76,.3]),
            box('crate','Original cargo',[2.65,height+.3,-1.85],[.5,.6,.55],1.5),
            ...extra
        ]
    };
}

async function main() {
    process.env.FRAYNIX_WORKSPACE_NO_MODELS = '1';
    const project = await fs.mkdtemp(path.join(os.tmpdir(),'fraynix-level234-'));
    const recipient = await fs.mkdtemp(path.join(os.tmpdir(),'fraynix-level234-clean-'));
    const out = path.resolve(process.argv[2] || 'build/spatial-level234');
    await fs.mkdir(out,{recursive:true});
    const args = process.platform === 'linux' ? ['--no-sandbox','--disable-dev-shm-usage'] : [];
    let {app,page} = await launch(project,args);
    const api = (route,input = {}) => app.evaluate((_,request) =>
        global.workspaceTest.api('/api/spatial/'+request.route,request.input),{route,input});
    const status = phrase => page.waitForFunction(text =>
        document.querySelector('#placement-status')?.textContent.includes(text),phrase);
    const count = async () => (await api('list')).scenes.length;
    const save = async (control, phrase) => {
        await page.locator(control).click();
        await status(phrase);
        const filename = (await page.locator('#placement-file').textContent()).slice('Saved '.length);
        const bytes = await fs.readFile(filename);
        const receipt = JSON.parse(await fs.readFile(filename+'.receipt.json','utf8'));
        assert.equal(sha(bytes),receipt.sha256);
        return {bytes,receipt};
    };
    async function restart() {
        await app.close();
        ({app,page} = await launch(project,args));
        await page.locator('#spatial-tab').click();
        await page.locator('#placement-resume').click();
        await page.locator('#placement-dialog[open]').waitFor();
    }
    async function capture(name) {
        await page.evaluate(() => new Promise(done=>requestAnimationFrame(()=>requestAnimationFrame(done))));
        const png = await app.evaluate(()=>global.workspaceTest.capture());
        await fs.writeFile(path.join(out,name),Buffer.from(png,'base64'));
    }

    try {
        const routing = await page.evaluate(async goal => {
            const {isPlacementRequest} = await import('./spatial-placement.js');
            return [goal,'Drop it in the open scene','Place the verified prop',
                'Add more enemies to my level'].map(isPlacementRequest);
        },command);
        assert.deepEqual(routing,[true,true,true,false]);
        const firstDefinition = level('Siding Workshop',0);
        const first = await api('import',{text:JSON.stringify(firstDefinition)});
        const secondDefinition = level('Raised Switchyard',.25,[
            box('lane-divider','Switchyard divider',[-1.1,.525,.35],[.3,.55,2.6])
        ]);
        secondDefinition.objects[0].size = [7,.2,6];
        secondDefinition.objects[0].friction = .38;
        const second = await api('import',{text:JSON.stringify(secondDefinition)});
        const intercepted = await api('import',{text:JSON.stringify(level('Intercepted test',0,[
            box('striker-stop','Probe fence',[-.42,.14,.58],[.12,.28,.13])
        ]))});
        const startingCount = await count();
        const assets = {};
        for (const [role,name] of Object.entries({front:'front',side:'side',top:'top',heldOut:'elevated'})) {
            const filename = name+'.png';
            const png = (await fs.readFile(path.join(__dirname,'fixtures','library-prop',filename))).toString('base64');
            assets[role] = await app.evaluate((_,source)=>global.workspaceTest.api('/api/media-tiles/add',{source}),{filename,png})
                .then(result=>result.id);
        }
        const source = await api('library-views',{
            name:'Twin-slot deck plate',widthM:.5,heldOutElevationDeg:60,assets
        });
        const object = source.objects.find(body=>body.visualMesh);
        const request = destination => ({
            sceneId:source.id,objectId:object.id,destinationId:destination.id,
            floorId:destination.objects[0].id,goal:command,targetM:[0,.58],yawDeg:20,aisle:[-2.4,-1,2.35,-1]
        });
        const review = input => api('placement-level-review',input);
        const approve = id => api('placement-approve',{reviewId:id,approvalSha256:id});
        const run = id => api('placement-run',{reviewId:id});
        const exportPlay = id => api('placement-export',{reviewId:id,kind:'play'});

        await assert.rejects(review({...request(first),targetM:[0,-1]}),/aisle is blocked/i);
        await assert.rejects(review({...request(first),aisle:[-2,-1,1,0]}),/straight X or Z aisle/i);
        await assert.rejects(review({...request(first),targetM:[5,0]}),/whole prop must rest/i);
        await assert.rejects(review({...request(first),targetM:[-2.4,.1]}),/intersects existing collider/i);
        assert.equal(await count(),startingCount+1);
        assert.deepEqual(await api('placement-latest'),{});

        const interference = await review(request(intercepted));
        await approve(interference.reviewId);
        await assert.rejects(run(interference.reviewId),/intercepted a contact trial/i);
        assert.equal(await count(),startingCount+1);
        assert.deepEqual(await api('read',{id:intercepted.id}),intercepted);

        const competing = await review({...request(first),yawDeg:35});
        await approve(competing.reviewId);
        await page.locator('#spatial-tab').click();
        await page.locator('#spatial-scenes').selectOption(source.id);
        await page.waitForFunction(id=>document.querySelector('#spatial-objects')?.value===id,object.id);
        await page.locator('#goal').fill(command);
        await page.locator('#run').click();
        await page.locator('#placement-level-fields').waitFor();
        await page.locator('#placement-destination').selectOption(first.id);
        await page.locator('#placement-floor').selectOption(first.objects[0].id);
        for (const [key,value] of Object.entries({x:0,z:.58,yaw:20,startX:-2.4,startZ:-1,endX:2.35,endZ:-1}))
            await page.locator('#placement-level-fields input[name='+key+']').fill(String(value));
        await page.locator('#placement-level-prepare').click();
        await status('Review saved');
        const pending = await api('placement-latest');
        assert.equal(pending.review.destination.sceneId,first.id);
        assert.equal(pending.layout.routeLengthM,4.75);
        await assert.rejects(run(pending.reviewId),/approve this placement/i);
        await assert.rejects(exportPlay(pending.reviewId),/verify the collision/i);
        assert.deepEqual(await api('read',{id:first.id}),first);
        await capture('level-review.png');

        await restart();
        await status('Review saved');
        assert.deepEqual(await api('placement-latest'),pending);
        await approve(pending.reviewId);
        await restart();
        await status('Approval saved');
        assert.equal(await count(),startingCount+1);
        await page.locator('#placement-approve').click();
        await status('Placement and native contact evidence verified');
        await capture('level-verified.png');
        const verified = await api('placement-latest');
        assert.equal(verified.reviewId,pending.reviewId);
        const {measured,repeat} = verified.evidence;
        assert.deepEqual(measured,repeat);
        assert(measured.hit.firstContactTick>0 && measured.hit.maxPropPlanarSpeedMps>.02);
        assert.equal(measured.miss.contactTicks,0);
        assert.equal(measured.playerEnvelopeClear,true);
        const installed = await api('read',{id:verified.sceneId});
        const edited = JSON.parse(installed.sourceText);
        assert.deepEqual(edited.objects.slice(0,-1),firstDefinition.objects);
        assert.equal(edited.objects.at(-1).visualMesh,object.visualMesh);
        assert.deepEqual(await api('read',{id:first.id}),first);
        assert.deepEqual(await api('read',{id:source.id}),source);
        assert.equal(await count(),startingCount+2);
        assert.equal((await run(pending.reviewId)).sceneId,verified.sceneId);
        assert.equal(await count(),startingCount+2);
        await assert.rejects(run(competing.reviewId),/newer reviewed revision/i);
        await assert.rejects(review(request(first)),/newer reviewed revision/i);

        const html = await save('#placement-play','Offline playable level saved');
        const evidence = await save('#placement-export','Scene, GLB and contact evidence saved together');
        const bundle = JSON.parse(evidence.bytes);
        assert.equal(html.receipt.source.evidenceSha256,verified.evidenceSha256);
        assert.equal(bundle.evidenceSha256,verified.evidenceSha256);
        assert.equal(sha(Buffer.from(bundle.glbBase64,'base64')),bundle.glbSha256);
        assert.deepEqual(bundle.scene,edited);

        await restart();
        await status('Placement and native contact evidence verified');
        await page.locator('#placement-replay').click();
        await status('Fresh native replay matched');
        const rebuilt = await exportPlay(pending.reviewId);
        assert.equal(rebuilt.sha256,html.receipt.sha256);
        assert.equal((await api('placement-export',{reviewId:pending.reviewId,kind:'evidence'})).sha256,evidence.receipt.sha256);

        const raised = await review({...request(second),targetM:[.4,.65],yawDeg:65,aisle:[2,-2.45,2,2.45]});
        assert(raised.review.targetM[1]>.25);
        await approve(raised.reviewId);
        const raisedResult = await run(raised.reviewId);
        assert.equal(raisedResult.evidence.measured.playerEnvelopeClear,true);
        assert(raisedResult.evidence.measured.hit.contactTicks>0);
        assert.equal(raisedResult.evidence.measured.miss.contactTicks,0);
        const raisedScene = await api('read',{id:raisedResult.sceneId});
        assert.deepEqual(JSON.parse(raisedScene.sourceText).objects.slice(0,-1),secondDefinition.objects);

        await app.close();
        const stateFile = path.join(project,'spatial','state.json');
        const disk = await fs.readFile(stateFile,'utf8');
        const amended = JSON.parse(disk);
        const swapped = amended.scenes.find(scene=>scene.id===first.id);
        swapped.title = 'Renamed outside the review';
        swapped.sourceText = JSON.stringify({...firstDefinition,title:swapped.title});
        swapped.sourceSha256 = sha(swapped.sourceText);
        await fs.writeFile(stateFile,JSON.stringify(amended));
        ({app,page} = await launch(project,args));
        await assert.rejects(exportPlay(pending.reviewId),/recheck the approved level placement/i);
        await app.close();
        await fs.writeFile(stateFile,disk);
        ({app,page} = await launch(project,args));
        await fs.appendFile(path.join(project,'media','tiles','sprites',assets.heldOut+'.json'),' ');
        await assert.rejects(exportPlay(pending.reviewId),/recheck the approved level placement/i);

        const output = path.join(out,'siding-workshop.html');
        await fs.writeFile(output,html.bytes);
        await fs.writeFile(path.join(out,'siding-workshop.fraynix-level.json'),evidence.bytes);
        await app.close();
        ({app,page} = await launch(recipient,args));
        assert.equal(await count(),0);
        assert.deepEqual(await api('placement-latest'),{});
        const reimport = await api('import',{text:JSON.stringify(bundle.scene)});
        assert.deepEqual(JSON.parse(reimport.sourceText),edited);
        assert.equal(reimport.objects.at(-1).visualMesh,object.visualMesh);
        const fileURL = pathToFileURL(output).href;
        await app.evaluate(async ({BrowserWindow},fileURL) => {
            const win = new BrowserWindow({width:940,height:690,webPreferences:{
                partition:'level234-standalone',sandbox:true,nodeIntegration:false,contextIsolation:true
            }});
            win.webContents.session.webRequest.onBeforeRequest((r,done)=>done({cancel:!/^(file|data):/.test(r.url)}));
            await win.loadURL(fileURL);
        },fileURL);
        const standalone = app.windows().find(win=>win.url()===fileURL);
        assert(standalone);
        await standalone.locator('canvas').focus();
        const outcome = state => standalone.locator('#progress[data-outcome='+state+']').waitFor({timeout:9000});
        await outcome('walking');
        await standalone.keyboard.down('ArrowDown');
        await outcome('blocked');
        await standalone.keyboard.up('ArrowDown');
        await standalone.locator('#restart').click();
        await standalone.keyboard.down('ArrowRight');
        await outcome('complete');
        await standalone.keyboard.up('ArrowRight');
        await standalone.screenshot({path:path.join(out,'level-offline.png')});
        const proof = {
            platform:process.platform,command,sourceOverlap:source.reconstruction.heldOutIou,
            testedLevels:[verified,raisedResult].map(item=>({
                title:item.review.destinationTitle,poseM:item.review.targetM,yawDeg:item.review.yawDeg,
                hitTick:item.evidence.measured.hit.firstContactTick,hitTicks:item.evidence.measured.hit.contactTicks,
                missTicks:item.evidence.measured.miss.contactTicks,routeM:item.evidence.measured.routeLengthM
            })),
            blockedPlacementRefused:true,interceptingColliderRefused:true,changedSourceRefused:true,
            staleDestinationRefused:true,changedDestinationRefused:true,duplicateCommitPrevented:true,unapprovedRunRefused:true,
            reviewRestart:true,approvalRestart:true,replayAfterRestart:true,unchangedExistingObjects:true,
            cleanProjectReimport:true,offlineObstacleStopsPlayer:true,offlineExitReached:true,
            evidenceSha256:verified.evidenceSha256,evidenceExportSha256:evidence.receipt.sha256,playSha256:html.receipt.sha256
        };
        await fs.writeFile(path.join(out,'level-proof.json'),JSON.stringify(proof,null,2)+'\n');
        console.log(JSON.stringify(proof));
    } catch (error) {
        console.error('Placement status:',await page.locator('#placement-status').textContent({timeout:1000}).catch(()=>''));
        throw error;
    } finally {
        await app?.close().catch(()=>{});
    }
}
main().catch(error=>{console.error(error);process.exitCode=1;});
