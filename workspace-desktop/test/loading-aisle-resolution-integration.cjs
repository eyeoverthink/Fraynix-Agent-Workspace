'use strict';

const assert = require('node:assert/strict');
const {createHash} = require('node:crypto');
const fs = require('node:fs/promises');
const os = require('node:os');
const path = require('node:path');
const {launch} = require('./calibration-integration.cjs');

const move = distance => 'Move the blue crate ' + distance + ' away from the loading door. Keep the 1 m aisle clear and protect the shelves.';
const sha = bytes => createHash('sha256').update(bytes).digest('hex');

function verify(parent, child, record, expected) {
    const {review,proof} = record;
    const measured = proof.measurements;
    const route = measured.route;
    const search = route.search;
    assert.equal(review.parentId,parent.id);
    assert.equal(review.parentSha256,parent.sourceSha256);
    assert.equal(review.crate,'cargo');
    assert.equal(review.forwardCm,expected.forwardCm);
    assert.equal(review.route.direct.gate,'path');
    assert.match(review.route.direct.reason,/Inner barrier, Outer barrier/);
    assert.equal(review.route.policy,'door-relative-'+(expected.fine?'fine':'coarse')+'-v2');
    assert.equal(measured.forwardM,expected.forwardCm/100);
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
    assert.deepEqual(search,review.route.search);
    assert.equal(search.forwardStepCm,expected.fine?5:10);
    assert.equal(search.lateralStepCm,expected.fine?5:15);
    assert.equal(search.coarseResult,expected.fine?'no-route':'accepted');
    assert(search.coarseEdgesChecked>0);
    assert.equal(search.fineEdgesChecked>0,expected.fine);
    assert(route.segmentCount>=5 && route.segmentCount<=12);
    assert.equal(route.segments.length,route.segmentCount);
    assert.equal(review.route.cells.at(-1)[0],Math.ceil(expected.forwardCm/search.forwardStepCm));
    assert(route.distanceM>measured.forwardM && route.distanceM<=measured.forwardM+1.800001);
    assert(Math.abs(route.distanceM-route.segments.reduce((sum,leg)=>sum+leg.distanceM,0))<1e-5);
    for (const [i,leg] of route.segments.entries()) {
        assert.equal(leg.number,i+1);
        assert.deepEqual(leg.fromM,review.route.pointsM[i]);
        assert.deepEqual(leg.toM,review.route.pointsM[i+1]);
        assert.equal(leg.endpointsClear,true);
        assert.equal(leg.aisleClear,true);
        assert.equal(leg.sweptPathClear,true);
    }
    const original = JSON.parse(parent.sourceText).objects;
    const result = JSON.parse(child.sourceText).objects;
    const cargo = rows => rows.find(item => item.sourceId==='cargo');
    const delta = cargo(result).position.map((coordinate,index)=>coordinate-cargo(original).position[index]);
    const angle = expected.yaw*Math.PI/180;
    const forward = expected.forwardCm/100;
    assert(Math.abs(delta[0]-forward*Math.sin(angle))<1e-8);
    assert(Math.abs(delta[2]-forward*Math.cos(angle))<1e-8);
    assert.equal(delta[1],0);
    assert.equal(cargo(original).size[0],expected.width);
    assert.deepEqual(cargo(result).position,review.toM);
    assert.deepEqual({...cargo(result),position:cargo(original).position},cargo(original));
    assert.deepEqual(result.filter(item=>item.sourceId!=='cargo'),original.filter(item=>item.sourceId!=='cargo'));
    return {...expected,deltaM:delta,distanceM:route.distanceM,segmentCount:route.segmentCount,search,
        aisleM:measured.widthM,shelvesPreserved:measured.shelvesPreserved,heldTicks:measured.crateSupportedTicks,
        segments:route.segments,sourceSha256:child.sourceSha256};
}

async function main() {
    process.env.FRAYNIX_UNIFIED_TEST='1';
    process.env.FRAYNIX_WORKSPACE_NO_MODELS='1';
    process.env.FRAYNIX_WORKSPACE_PERSISTENT='0';
    const output = path.resolve(process.argv[2] || 'build/loading-aisle-resolution');
    await fs.mkdir(output,{recursive:true});
    const project = await fs.mkdtemp(path.join(os.tmpdir(),'Fraynix wide crate route '));
    const flags = process.platform==='linux'?['--no-sandbox','--disable-dev-shm-usage']:[];
    const errors = [];
    let app, page;
    const call = (command,args={})=>app.evaluate((_,v)=>global.workspaceTest.api('/api/spatial/'+v.command,v.args),{command,args});
    const read = id=>call('read',{id});
    const latest = ()=>call('aisle-latest');
    const count = async()=> (await call('list')).scenes.length;
    const status = text=>page.locator('#aisle-status').filter({hasText:text}).waitFor({timeout:25000});
    const approve = id=>call('aisle-approve',{id,approvalSha256:id});

    async function ask(text) {
        await page.locator('#goal').fill(text);
        await page.locator('#run').click();
    }
    async function open() {
        ({app,page} = await launch(project,flags));
        page.on('pageerror',error=>errors.push(error.message));
        assert.equal((await app.evaluate(()=>global.workspaceTest.network())).enabled,true);
        assert.equal((await app.evaluate(()=>global.workspaceTest.api('/api/state'))).models.length,0);
        await ask('Open Spatial.');
        const panel = page.locator('#loading-aisle');
        await panel.waitFor();
        if (!await panel.evaluate(node=>node.open)) await panel.locator('summary').click();
    }
    async function close() {
        if (!app) return;
        await app.close();
        app=null;
    }
    async function load(variant) {
        await page.locator('#aisle-yaw').selectOption(variant);
        await page.locator('#aisle-load').click();
        await status('Choose the crate');
        return read(await page.locator('#spatial-scenes').inputValue());
    }
    async function picture(name) {
        await page.locator('#loading-aisle').evaluate(panel=>{
            const view=document.querySelector('#spatial-view');
            view.scrollTop+=panel.getBoundingClientRect().top-view.getBoundingClientRect().top-32;
        });
        await page.evaluate(()=>new Promise(resolve=>requestAnimationFrame(()=>requestAnimationFrame(resolve))));
        const png=await app.evaluate(()=>global.workspaceTest.capture());
        await fs.writeFile(path.join(output,name+'.png'),Buffer.from(png,'base64'));
    }
    async function mapData() {
        const canvas=page.locator('#aisle-route-map');
        await canvas.waitFor();
        return canvas.evaluate(node=>node.toDataURL('image/png'));
    }

    try {
        await open();
        const parent=await load('wide-fine');
        await ask(move('1.20 m'));
        await status('original 120 cm destination');
        const draft=await latest();
        assert.equal(draft.status,'review');
        assert.equal(draft.review.route.search.coarseResult,'no-route');
        assert.match(await page.locator('#aisle-route-search').innerText(),/5 × 5 cm grid/);
        assert.equal(await count(),1);
        assert.deepEqual(await read(parent.id),parent);
        await assert.rejects(call('aisle-approve',{id:draft.id,approvalSha256:'0'.repeat(64)}),/digest/);
        await assert.rejects(call('aisle-export',{id:draft.id}),/Verify/);
        const draftMap=await mapData();
        await picture('fine-review');

        await close();
        await open();
        await page.locator('#aisle-resume').click();
        await status('Restored route');
        assert.deepEqual(await latest(),draft);
        assert.equal(await mapData(),draftMap);
        assert.equal(await count(),1);
        const reviewPath=path.join(project,'spatial','loading-aisle',draft.id+'.review.json');
        const original=await fs.readFile(reviewPath);
        const changed=JSON.parse(original);
        changed.data.route.search.coarseResult='accepted';
        await fs.writeFile(reviewPath,JSON.stringify(changed));
        await assert.rejects(approve(draft.id),/integrity mismatch/);
        await fs.writeFile(reviewPath,original);
        await page.getByRole('button',{name:'Approve this route',exact:true}).click();
        await status('Verified route');
        const approved=await latest();
        const child=await read(approved.sceneId);
        const fine=verify(parent,child,approved,{width:.42,forwardCm:120,yaw:-53,fine:true});
        assert.equal(await count(),2);
        assert.deepEqual(await read(parent.id),parent);

        await close();
        await open();
        await page.locator('#aisle-resume').click();
        await status('Verified route');
        await page.locator('#aisle-replay:enabled').click();
        await status('Offline replay matched');
        assert.deepEqual(await latest(),approved);
        assert.deepEqual(await read(approved.sceneId),child);
        await picture('fine-restart');
        const receipt=Buffer.from((await call('aisle-export',{id:approved.id})).base64,'base64');
        await page.locator('#aisle-export:enabled').click();
        await status('Exported the checked scene');
        const rows=(await fs.readFile(path.join(project,'exports','manifest.jsonl'),'utf8')).trim().split('\n').map(JSON.parse);
        const entry=rows.find(row=>row.kind==='verified-loading-aisle');
        assert.equal(entry.sha256,sha(receipt));
        assert((await fs.readFile(entry.path)).equals(receipt));
        await fs.writeFile(path.join(output,'fine-verified.json'),receipt);

        const results=[];
        for (const [variant,distance,expected] of [
            ['wide-mirror','1730 mm',{width:.58,forwardCm:173,yaw:96,fine:true}],
            ['wide-coarse','120 cm',{width:.42,forwardCm:120,yaw:27,fine:false}],
        ]) {
            const scene=await load(variant);
            await ask(move(distance));
            await status('original '+expected.forwardCm+' cm destination');
            const selected=await latest();
            const accepted=await approve(selected.id);
            results.push(verify(scene,await read(accepted.sceneId),accepted,expected));
            assert.equal((await call('aisle-replay',{id:accepted.id})).replayed,true);
            if (variant==='wide-mirror') await picture('fine-longer');
        }

        const oversized=await load('wide-blocked');
        const before=await count();
        await ask(move('120 cm'));
        await status('Both route grids were checked');
        const blocked=await latest();
        assert.equal(blocked.status,'blocked');
        assert.equal(blocked.alternatesConsidered,8);
        assert(blocked.routeEdgesChecked>0);
        assert(blocked.fineRouteEdgesChecked>0);
        assert.equal(await page.locator('#aisle-approve').isVisible(),false);
        await assert.rejects(approve(blocked.id),/verified path/);
        await assert.rejects(call('aisle-export',{id:blocked.id}),/Verify/);
        assert.equal(await count(),before);
        assert.deepEqual(await read(oversized.id),oversized);
        await ask(move('201 cm'));
        await status('whole-centimetre move from 40 to 200 cm');
        assert.equal(await count(),before);
        assert.deepEqual(errors,[]);
        assert.equal((await app.evaluate(()=>global.workspaceTest.network())).enabled,true);
        const summary={passed:true,platform:process.platform,offlineRestarts:2,pendingRestored:true,
            approvedReplayed:true,changedSearchRefused:true,wrongApprovalRefused:true,
            exportSha256:sha(receipt),fine,mirrored:results[0],coarse:results[1],
            refused:{width:.74,forwardCm:120,coarseEdges:blocked.routeEdgesChecked,fineEdges:blocked.fineRouteEdgesChecked,
                reason:blocked.direct.reason,sourceUnchanged:true}};
        await fs.writeFile(path.join(output,'result.json'),JSON.stringify(summary,null,2)+'\n');
        console.log(JSON.stringify({...summary,fine:fine.search,mirrored:results[0].search,coarse:results[1].search}));
    } catch (error) {
        if (page) await fs.writeFile(path.join(output,'failure.txt'),await page.locator('body').innerText()).catch(()=>{});
        throw error;
    } finally {
        await close();
        await fs.rm(project,{recursive:true,force:true});
    }
}

main().catch(error=>{console.error(error);process.exitCode=1;});
