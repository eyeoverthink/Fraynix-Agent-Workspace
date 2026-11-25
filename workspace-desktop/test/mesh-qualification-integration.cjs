'use strict';

const assert=require('node:assert/strict');
const {createHash}=require('node:crypto');
const {execFile}=require('node:child_process');
const fs=require('node:fs/promises');
const path=require('node:path');
const {promisify}=require('node:util');

const digest=bytes=>createHash('sha256').update(bytes).digest('hex');
const body=(sourceId,label,position,size,massKg)=>({
    sourceId,label,shape:'box',position,size,massKg,friction:.57,restitution:.04,rotation:[0,0,0,1]
});
const level={
    format:'fraynix.spatial-scene',version:1,units:'m',frame:'right-handed-y-up',title:'Signal Bay',
    source:{kind:'synthetic',physicalProperties:'declared',note:'Independent authored floor for the edited bracket.'},
    scaleReference:{a:[0,0,0],b:[1,0,0],knownLengthM:1,toleranceM:0,basis:'declared-test'},
    gravityMps2:9.81,objects:[
        body('floor','Signal bay floor',[0,-.1,0],[7,.2,4.8],0),
        body('rail','Walkway rail',[-2.15,.43,.2],[.25,.86,.45],0),
        body('crate','Service crate',[2.5,.22,-1.8],[.48,.44,.62],1.8)
    ]
};
const requested={x:.15,z:.7,yaw:31,startX:-2.15,startZ:-1.1,endX:2.2,endZ:-1.1};

function topTriangle(scene, object) {
    const data=Buffer.from(scene.visualMeshes[object.visualMesh],'base64');
    const vertices=data.readUInt32LE(0), count=data.readUInt32LE(4), start=8+12*vertices;
    const heights=Array.from({length:vertices},(_,index)=>data.readFloatLE(12+12*index));
    const top=Math.max(...heights);
    for(let offset=0;offset<count;offset+=3) {
        if([0,1,2].every(i=>heights[data.readUInt16LE(start+2*(offset+i))]===top)) return offset/3;
    }
    throw Error('The original model has no top triangle.');
}

async function verifyQualification({app,page,pick,status,api,project,output,extra,launch,visit,clickGraphic,stopSource}) {
    const open=async(page,id)=>{
        const details=page.locator('#'+id);
        if(!await details.evaluate(el=>el.open)) await details.locator('summary').click();
    };
    const wait=(page,id,phrase)=>page.waitForFunction(({id,phrase})=>
        document.getElementById(id)?.textContent.includes(phrase),{id,phrase},{timeout:20000});
    const screenshot=async(app,page,name)=>{
        await page.evaluate(()=>new Promise(resolve=>requestAnimationFrame(()=>requestAnimationFrame(resolve))));
        await fs.writeFile(path.join(output,name),Buffer.from(await app.evaluate(()=>global.workspaceTest.capture()),'base64'));
    };
    const destination=await api('import',{text:JSON.stringify(level)});
    await pick('front','front','front');
    await pick('side','side','side');
    await pick('heldOut','held-out','held-out 45°');
    await page.locator('#browser-views-build').click();
    await status('held-out silhouette overlap');
    const original=await api('read',{id:await page.locator('#spatial-scenes').inputValue()});
    const source=original.objects.find(object=>object.visualMesh);

    await open(page,'spatial-mesh-edit');
    await page.locator('#mesh-edit-mode').selectOption('extrude');
    await page.locator('#mesh-edit-triangle').fill(String(topTriangle(original,source)));
    await page.locator('#mesh-edit-distance').fill('150');
    await page.locator('#mesh-fit').check();
    await page.locator('#mesh-preview-button').click();
    await wait(page,'mesh-edit-status','connected triangles');
    await page.locator('#mesh-edit-save').click();
    await wait(page,'mesh-edit-status','Mesh copy saved');
    const edit=await api('read',{id:await page.locator('#spatial-scenes').inputValue()});
    const editedBody=edit.objects.find(object=>object.visualMesh);
    assert.notEqual(editedBody.visualMesh,source.visualMesh);
    assert(Math.abs(editedBody.size[1]-.55)<1e-5);

    const inputs={sceneId:edit.id,objectId:editedBody.id,destinationId:destination.id,floorId:destination.objects[0].id,
        goal:'Place the edited prop in this level.',targetM:[requested.x,requested.z],yawDeg:requested.yaw,
        aisle:[requested.startX,requested.startZ,requested.endX,requested.endZ]};
    await assert.rejects(api('placement-level-review',inputs),/checked held-out view/i);
    const pickWitness=async(view,elevation,phrase)=>{
        await visit(view);
        await page.locator('#mesh-witness-angle').selectOption(String(elevation));
        await page.locator('#mesh-witness-pick').click();
        await wait(page,'mesh-witness-status','Click the new registered view');
        let sent=false;
        for(let i=0;i<60&&!sent;i++){
            sent=await clickGraphic();
            if(!sent)await new Promise(resolve=>setTimeout(resolve,50));
        }
        assert(sent,'The browser did not receive a trusted witness click.');
        await wait(page,'mesh-witness-status',phrase);
    };
    await pickWitness('held-out',0,'other registered elevation');
    await pickWitness('held-out',60,'fresh witness showing the edited shape');
    await pickWitness('unchanged-elevated',60,'did not verify the edit');
    const refusal=await page.locator('#mesh-witness-status').textContent();
    assert.equal((await api('list')).scenes.length,3);
    await screenshot(app,page,'wrong-witness-refused.png');

    await pickWitness('edited-elevated',60,'Qualified edit');
    const qualified=await api('read',{id:await page.locator('#spatial-scenes').inputValue()});
    const receipt=qualified.meshQualification;
    assert.equal(receipt.original.sceneId,original.id);
    assert.equal(receipt.edited.sceneId,edit.id);
    assert.equal(receipt.meshSha256,editedBody.visualMesh);
    assert(receipt.heldOutIou>.93 && receipt.heldOutIou-receipt.originalIou>=.02,JSON.stringify(receipt));
    assert.deepEqual(JSON.parse(qualified.sourceText),JSON.parse(edit.sourceText));
    assert.deepEqual(await api('read',{id:original.id}),original);
    const witness=path.join(project,'spatial','mesh-qualification-sources',qualified.id+'.json');
    const pixels=await fs.readFile(witness);
    assert.equal(digest(pixels),receipt.captureSha256);
    assert.equal(digest(Buffer.from(JSON.parse(pixels).png,'base64')),receipt.witness.pngSha256);
    await screenshot(app,page,'qualified-mesh.png');

    await stopSource();
    await app.close();
    let next=await launch(project,extra);
    const call=(route,input={})=>next.app.evaluate((_,req)=>global.workspaceTest.api('/api/spatial/'+req.route,req.input),{route,input});
    try {
        await next.page.locator('#spatial-tab').click();
        await next.page.locator('#spatial-scenes').selectOption(qualified.id);
        await open(next.page,'spatial-mesh-edit');
        await wait(next.page,'mesh-witness-status','Qualified edit');
        await next.page.locator('#mesh-qualified-place').click();
        await next.page.locator('#placement-level-fields').waitFor();
        await next.page.locator('#placement-destination').selectOption(destination.id);
        await next.page.locator('#placement-floor').selectOption(destination.objects[0].id);
        for(const [name,value] of Object.entries(requested))
            await next.page.locator('#placement-level-fields input[name='+name+']').fill(String(value));
        await next.page.locator('#placement-level-prepare').click();
        await wait(next.page,'placement-status','Review saved');
        const pending=await call('placement-latest');
        const args={reviewId:pending.reviewId};
        assert.equal(pending.review.editReference.receiptSha256,qualified.meshQualificationSha256);
        assert.deepEqual(pending.review.editReference,{...receipt,receiptSha256:qualified.meshQualificationSha256});
        await assert.rejects(call('placement-run',args),/approve this placement/i);
        await screenshot(next.app,next.page,'qualified-edit-review.png');
        await next.app.close();
        next=await launch(project,extra);
        await next.page.locator('#spatial-tab').click();
        await next.page.locator('#placement-resume').click();
        await wait(next.page,'placement-status','Review saved');
        await next.page.locator('#placement-approve').click();
        await wait(next.page,'placement-status','Placement and native contact evidence verified');
        const result=await call('placement-latest');
        const {measured,repeat}=result.evidence;
        assert.deepEqual(measured,repeat);
        assert(measured.hit.firstContactTick>0);
        assert.equal(measured.miss.contactTicks,0);
        assert.equal(measured.playerEnvelopeClear,true);
        const exported=await call('placement-export',{...args,kind:'glb'});
        const glb=Buffer.from(exported.base64,'base64');
        assert.equal(digest(glb),exported.sha256);
        await fs.writeFile(path.join(output,'qualified-edit.glb'),glb);
        const saved=await call('read',{id:result.sceneId});
        assert.equal(saved.objects.at(-1).visualMesh,editedBody.visualMesh);
        assert.deepEqual(await call('read',{id:destination.id}),destination);
        assert.deepEqual(JSON.parse(saved.sourceText).objects.slice(0,-1),level.objects);
        await next.app.close();
        next=await launch(project,extra);
        const replay=await call('placement-replay',args);
        assert.equal(replay.sceneId,result.sceneId);
        assert.deepEqual(replay.evidence,result.evidence);
        assert.equal((await call('placement-export',{...args,kind:'glb'})).sha256,exported.sha256);
        await fs.appendFile(witness,' ');
        await assert.rejects(call('placement-export',{...args,kind:'glb'}),/recheck the approved level placement/i);
        await fs.writeFile(witness,pixels);
        const sourceFile=path.join(project,'spatial','reconstruction-sources',original.id+'.json');
        await fs.appendFile(sourceFile,' ');
        await assert.rejects(call('read',{id:qualified.id}),/recheck the edited mesh and its retained witness/i);

        let blender=null;
        if(process.env.FRAYNIX_TEST_BLENDER){
            await promisify(execFile)(process.env.FRAYNIX_TEST_BLENDER,['--background','--factory-startup','--disable-autoexec',
                '--python-exit-code','17','--python',path.join(__dirname,'mesh-qualification-blender.py'),'--','verify',output],
            {timeout:90000,windowsHide:true});
            blender=JSON.parse(await fs.readFile(path.join(output,'qualified-blender-proof.json'),'utf8'));
            assert.equal(blender.glbSha256,exported.sha256);
        }
        const proof={platform:process.platform,receipt,refusal,meshTriangles:edit.meshEdit.triangles,heightM:editedBody.size[1],
            hitTick:measured.hit.firstContactTick,hitContacts:measured.hit.contactTicks,missContacts:measured.miss.contactTicks,
            aisleM:measured.routeLengthM,glbSha256:exported.sha256,blender,resumedWithSourceOffline:true,reviewRestart:true,replayedAfterRestart:true,
            unqualifiedRefused:true,reusedWitnessRefused:true,wrongWitnessRefused:true,unapprovedRefused:true,
            changedOriginalRefused:true,changedWitnessRefused:true,originalDestinationPreserved:true};
        await fs.writeFile(path.join(output,'mesh-qualification-proof.json'),JSON.stringify(proof,null,2)+'\n');
        console.log(JSON.stringify(proof));
    } finally {
        await next?.app.close().catch(()=>{});
    }
}

module.exports={verifyQualification};
