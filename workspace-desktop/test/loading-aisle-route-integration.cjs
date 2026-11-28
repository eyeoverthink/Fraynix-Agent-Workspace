'use strict';

const assert = require('node:assert/strict');
const {createHash} = require('node:crypto');
const fs = require('node:fs/promises');
const os = require('node:os');
const path = require('node:path');
const {launch} = require('./calibration-integration.cjs');

const request = 'Move the blue crate 40 cm away from the loading door. Keep the 1 m aisle clear and protect the shelves.';
const sha = bytes => createHash('sha256').update(bytes).digest('hex');

function verifyRoute(parent, child, record, yaw, firstSide) {
    const {review,proof} = record;
    const measured = proof.measurements;
    const route = measured.route;
    assert.equal(review.parentId,parent.id);
    assert.equal(review.parentSha256,parent.sourceSha256);
    assert.equal(review.request,request);
    assert.equal(review.crate,'cargo');
    assert.equal(review.route.direct.gate,'path');
    assert.match(review.route.direct.reason,/Inside staggered barrier, Outside staggered barrier/i);
    assert.equal(review.alternate,undefined);
    assert.equal(measured.forwardM,.4);
    assert.equal(measured.lateralM,0);
    assert.equal(measured.widthM,1);
    assert.equal(measured.aisleSweepClear,true);
    assert.equal(measured.destinationClear,true);
    assert.equal(measured.nativeAisleChecks,3);
    assert.equal(measured.shelvesPreserved,2);
    assert.equal(measured.crateSupportedTicks,60);
    assert.equal(measured.candidateSourceSha256,child.sourceSha256);
    assert.equal(proof.nativeRepeats,2);
    assert.deepEqual(review.preview,measured);
    assert.equal(route.segmentCount,5);
    assert.equal(route.distanceM,1.6);
    assert.deepEqual(review.route.cells,[[0,0],[0,2*firstSide],[2,2*firstSide],[2,-2*firstSide],[4,-2*firstSide],[4,0]]);
    assert.deepEqual(route.segments.map(segment => segment.distanceM),[.30,.20,.60,.20,.30]);
    for (const [i,segment] of route.segments.entries()) {
        assert.equal(segment.number,i+1);
        assert.deepEqual(segment.fromM,review.route.pointsM[i]);
        assert.deepEqual(segment.toM,review.route.pointsM[i+1]);
        assert.equal(segment.endpointsClear,true);
        assert.equal(segment.aisleClear,true);
        assert.equal(segment.sweptPathClear,true);
    }
    assert.deepEqual(route.segments[0].fromM,review.fromM);
    assert.deepEqual(route.segments.at(-1).toM,review.toM);

    const before = JSON.parse(parent.sourceText).objects;
    const after = JSON.parse(child.sourceText).objects;
    const cargo = rows => rows.find(item => item.sourceId==='cargo');
    const original = cargo(before);
    const revised = cargo(after);
    const delta = revised.position.map((coordinate,i) => coordinate-original.position[i]);
    const theta = yaw*Math.PI/180;
    assert(Math.abs(delta[0]-.4*Math.sin(theta))<1e-8);
    assert(Math.abs(delta[2]-.4*Math.cos(theta))<1e-8);
    assert.equal(delta[1],0);
    assert.deepEqual(revised.position,review.toM);
    assert.deepEqual({...revised,position:original.position},original);
    assert.deepEqual(after.filter(item => item.sourceId!=='cargo'),before.filter(item => item.sourceId!=='cargo'));
    return {yaw,firstSide,deltaM:delta,distanceM:route.distanceM,segmentCount:route.segmentCount,
        widthM:measured.widthM,protectedShelves:measured.shelvesPreserved,heldTicks:measured.crateSupportedTicks,
        pointsM:review.route.pointsM,segments:route.segments,sourceSha256:child.sourceSha256};
}

async function main() {
    process.env.FRAYNIX_UNIFIED_TEST = '1';
    process.env.FRAYNIX_WORKSPACE_NO_MODELS = '1';
    process.env.FRAYNIX_WORKSPACE_PERSISTENT = '0';
    const output = path.resolve(process.argv[2] || 'build/loading-aisle-route');
    await fs.mkdir(output,{recursive:true});
    const project = await fs.mkdtemp(path.join(os.tmpdir(),'Fraynix staggered loading bay '));
    const flags = process.platform==='linux' ? ['--no-sandbox','--disable-dev-shm-usage'] : [];
    const errors = [];
    let app, page;
    const call = (command,args={}) => app.evaluate((_,v) => global.workspaceTest.api('/api/spatial/'+v.command,v.args),{command,args});
    const read = id => call('read',{id});
    const latest = () => call('aisle-latest');
    const count = async () => (await call('list')).scenes.length;
    const status = text => page.locator('#aisle-status').filter({hasText:text}).waitFor({timeout:25000});
    const approve = id => call('aisle-approve',{id,approvalSha256:id});

    async function open() {
        ({app,page} = await launch(project,flags));
        page.on('pageerror',error => errors.push(error.message));
        assert.equal((await app.evaluate(() => global.workspaceTest.network())).enabled,true);
        assert.equal((await app.evaluate(() => global.workspaceTest.api('/api/state'))).models.length,0);
        await ask('Open Spatial.');
        const panel = page.locator('#loading-aisle');
        await panel.waitFor();
        if (!await panel.evaluate(node => node.open)) await panel.locator('summary').click();
    }
    async function close() {
        if (!app) return;
        await app.close();
        app = null;
    }
    async function ask(text) {
        await page.locator('#goal').fill(text);
        await page.locator('#run').click();
    }
    async function load(variant) {
        await page.locator('#aisle-yaw').selectOption(variant);
        await page.locator('#aisle-load').click();
        await status('Choose the crate');
        return read(await page.locator('#spatial-scenes').inputValue());
    }
    async function picture(name) {
        await page.locator('#loading-aisle').evaluate(panel => {
            const view = document.querySelector('#spatial-view');
            view.scrollTop += panel.getBoundingClientRect().top-view.getBoundingClientRect().top-32;
        });
        await page.evaluate(() => new Promise(resolve => requestAnimationFrame(() => requestAnimationFrame(resolve))));
        await fs.writeFile(path.join(output,name+'.png'),
            Buffer.from(await app.evaluate(() => global.workspaceTest.capture()),'base64'));
    }
    async function compareRouteMap() {
        await page.locator('#aisle-route-map').waitFor();
        assert.equal(await page.locator('#aisle-segments li').count(),5);
        assert.equal(await page.locator('#aisle-route-length').innerText(),'5 checked segments · 1.60 m total');
        return page.locator('#aisle-route-map').evaluate(canvas => canvas.toDataURL('image/png'));
    }

    try {
        await open();
        const parent = await load('staggered-east');
        await ask(request);
        await status('Found a 5-segment route');
        const pending = await latest();
        assert.equal(pending.status,'review');
        assert.equal(await count(),1);
        assert.deepEqual(await read(parent.id),parent);
        const routeMap = await compareRouteMap();
        await assert.rejects(call('aisle-approve',{id:pending.id,approvalSha256:'0'.repeat(64)}),/digest/);
        await assert.rejects(call('aisle-replay',{id:pending.id}),/Approve and verify/);
        await assert.rejects(call('aisle-export',{id:pending.id}),/Verify/);
        await picture('route-review');

        await close();
        await open();
        await page.locator('#aisle-resume').click();
        await status('Restored route');
        assert.deepEqual(await latest(),pending);
        assert.equal(await compareRouteMap(),routeMap);
        assert.equal(await count(),1);
        await page.getByRole('button',{name:'Approve this route',exact:true}).click();
        await status('Verified route · 5 segments');
        const approved = await latest();
        const revised = await read(approved.sceneId);
        const east = verifyRoute(parent,revised,approved,-41,1);
        assert.deepEqual(await approve(pending.id),approved);
        assert.deepEqual(await read(parent.id),parent);
        assert.equal(await count(),2);

        await close();
        await open();
        await page.locator('#aisle-resume').click();
        await status('Verified route · 5 segments');
        await page.locator('#aisle-replay:enabled').click();
        await status('Offline replay matched');
        assert.deepEqual(await latest(),approved);
        assert.deepEqual(await read(approved.sceneId),revised);
        await compareRouteMap();
        await picture('route-restart');
        const data = await call('aisle-export',{id:approved.id});
        const proven = Buffer.from(data.base64,'base64');
        await page.locator('#aisle-export:enabled').click();
        await status('Exported the checked scene');
        const entries = (await fs.readFile(path.join(project,'exports','manifest.jsonl'),'utf8')).trim().split('\n').map(JSON.parse);
        const exported = entries.find(entry => entry.kind==='verified-loading-aisle');
        assert.equal(exported.sha256,sha(proven));
        assert((await fs.readFile(exported.path)).equals(proven));
        await fs.writeFile(path.join(output,'route-verified.json'),proven);

        const reviewPath = path.join(project,'spatial','loading-aisle',pending.id+'.review.json');
        const original = await fs.readFile(reviewPath);
        const tampered = JSON.parse(original);
        tampered.data.route.cells[3][1] = 2;
        await fs.writeFile(reviewPath,JSON.stringify(tampered));
        await assert.rejects(call('aisle-replay',{id:pending.id}),/integrity mismatch/);
        await assert.rejects(call('aisle-export',{id:pending.id}),/integrity mismatch/);
        await fs.writeFile(reviewPath,original);

        const westParent = await load('staggered-west');
        await ask(request);
        await status('Found a 5-segment route');
        const westPending = await latest();
        const westApproved = await approve(westPending.id);
        const west = verifyRoute(westParent,await read(westApproved.sceneId),westApproved,83,-1);
        assert.equal((await call('aisle-replay',{id:westApproved.id})).replayed,true);
        await picture('route-mirrored');

        const blockedParent = await load('staggered-closed');
        const before = await count();
        await ask(request);
        await status('I found no safe alternate');
        const refusal = await latest();
        assert.equal(refusal.status,'blocked');
        assert.match(refusal.direct.reason,/Crossover barrier/);
        assert.equal(refusal.alternatesConsidered,8);
        assert(refusal.routeEdgesChecked>0);
        assert.equal(await page.locator('#aisle-approve').isVisible(),false);
        await assert.rejects(approve(refusal.id),/verified path/);
        await assert.rejects(call('aisle-export',{id:refusal.id}),/Verify/);
        assert.equal(await count(),before);
        assert.deepEqual(await read(blockedParent.id),blockedParent);
        assert.deepEqual(errors,[]);
        assert.equal((await app.evaluate(() => global.workspaceTest.network())).enabled,true);

        const summary = {passed:true,platform:process.platform,offlineRestarts:2,
            pendingRouteRestored:true,verifiedRouteReplayed:true,changedWaypointRefused:true,
            wrongApprovalRefused:true,routeExportSha256:sha(proven),east,west,
            blocked:{reason:refusal.direct.reason,edgesChecked:refusal.routeEdgesChecked,sourceUnchanged:true}};
        await fs.writeFile(path.join(output,'result.json'),JSON.stringify(summary,null,2)+'\n');
        console.log(JSON.stringify({...summary,east:{legs:east.segmentCount},west:{legs:west.segmentCount}}));
    } catch (error) {
        if (page) await fs.writeFile(path.join(output,'failure.txt'),await page.locator('body').innerText()).catch(()=>{});
        throw error;
    } finally {
        await close();
        await fs.rm(project,{recursive:true,force:true});
    }
}

main().catch(error => {console.error(error);process.exitCode=1;});
