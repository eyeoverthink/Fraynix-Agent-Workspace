'use strict';
const assert=require('node:assert/strict');
const {execFile}=require('node:child_process');
const {createHash}=require('node:crypto');
const fs=require('node:fs/promises');
const path=require('node:path');
const {setTimeout:delay}=require('node:timers/promises');
const {promisify}=require('node:util');
const {importBlend}=require('../../workspace-desktop/blender-exchange.cjs');
const {launch}=require('../../workspace-desktop/test/calibration-integration.cjs');
const exec=promisify(execFile);

function similar(left,right,allowance=1e-6) {
    assert.equal(left.length,right.length);
    for(let i=0;i<left.length;i++) assert(Math.abs(left[i]-right[i])<=allowance,[left,right]);
}

async function main() {
    const output=path.resolve(process.argv[2]||'build/edition189-live');
    await fs.mkdir(output);
    const project=path.join(output,'project');
    await fs.mkdir(project);
    const executable=process.env.FRAYNIX_BLENDER || 'blender';
    const fixture=path.join(__dirname,'blender-fixtures.py');
    const blender=(prefix,mode,file)=>exec(executable,['--background','--factory-startup','--disable-autoexec',...prefix,
        '--python-exit-code','20','--python',fixture,'--',mode,file],{windowsHide:true,timeout:60000});
    await blender([],'fixture',output);
    Object.assign(process.env,{FRAYNIX_WORKSPACE_NO_MODELS:'1',FRAYNIX_WORKSPACE_PERSISTENT:'0',
        FRAYNIX_WORKSPACE_JAVA_HEAP_MIB:'768',FRAYNIX_WORKSPACE_JAVA_PROCESSORS:'2'});
    let {app,page}=await launch(project);
    const api=(route,args={})=>app.evaluate((_,[route,args])=>global.workspaceTest.api(route,args),[route,args]);
    const read=(id)=>api('/api/spatial/read',{id});
    const current=()=>page.locator('#spatial-scenes').inputValue();
    const source=(scene,id)=>scene.objects.find(body=>body.sourceId===id);
    const capture=async name=>{
        await page.evaluate(()=>new Promise(resolve=>requestAnimationFrame(()=>requestAnimationFrame(resolve))));
        await fs.writeFile(path.join(output,name),Buffer.from(await app.evaluate(()=>global.workspaceTest.capture()),'base64'));
    };
    const chooseOpen=(file)=>app.evaluate(({dialog},file)=>{
        dialog.showOpenDialog=async()=>({canceled:false,filePaths:[file]});
    },file);
    const chooseSave=(file)=>app.evaluate(({dialog},file)=>{
        dialog.showSaveDialog=async()=>({canceled:false,filePath:file});
    },file);
    const rendererErrors=[];
    page.on('pageerror',error=>rendererErrors.push(error.message));
    try {
        await app.evaluate(({BrowserWindow})=>BrowserWindow.getAllWindows().find(w=>w.isVisible()).setSize(1480,1030));
        await page.locator('#spatial-tab').click();
        await chooseOpen(path.join(output,'authored-bench.blend'));
        await page.locator('#spatial-import-blender').click();
        await page.waitForFunction(()=>document.querySelector('#notice').textContent.includes('Blender scene imported'),{},
            {timeout:25000});
        const first=await read(await current());
        const bytes=await fs.readFile(path.join(output,'authored-bench.blend'));
        const sourceSha=createHash('sha256').update(bytes).digest('hex');
        assert(first.source.note.includes(sourceSha));
        assert.equal(first.sourceUnits,'m');
        assert.equal(first.source.kind,'synthetic');
        assert.equal(first.scaleCheck.convertedLengthM,1);
        assert.equal(first.scaleCheck.independentlyCalibrated,false);
        similar(source(first,'crate').position,[-2,.25,.25]);
        similar(source(first,'floor').size,[8,.4,6]);
        similar(source(first,'obstacle').size,[.6,1,1.5]);
        similar(source(first,'obstacle').rotation,[0,Math.sin(Math.PI/18),0,Math.cos(Math.PI/18)]);
        assert.equal(source(first,'crate').massKg,1.25);
        assert.equal(source(first,'crate').friction,.4);
        assert.deepEqual(JSON.parse(first.sourceText).scaleReference.b,[0,0,-1]);
        await capture('blender-import.png');

        const returned=path.join(output,'editable-bench.blend');
        await chooseSave(returned);
        await page.locator('#spatial-export-blender').click();
        await page.waitForFunction(()=>document.querySelector('#notice').textContent.includes('Editable Blender scene saved'),{},
            {timeout:25000});
        const exportedBytes=await fs.readFile(returned);
        const exportedHash=createHash('sha256').update(exportedBytes).digest('hex');
        const secondData=await importBlend(returned);
        assert.equal(secondData.sha256,exportedHash);
        await chooseOpen(returned);
        await page.locator('#spatial-import-blender').click();
        await page.waitForFunction(id=>document.querySelector('#spatial-scenes').value!==id,first.id,{timeout:25000});
        await page.waitForFunction(()=>!document.querySelector('#spatial-import-blender').disabled);
        const second=await read(await current());
        assert.equal(second.scaleCheck.convertedLengthM,1);
        assert.equal(second.gravityMps2,first.gravityMps2);
        for(const original of first.objects) {
            const restored=source(second,original.sourceId);
            assert(restored);
            assert.notEqual(restored.id,original.id);
            assert.equal(restored.label,original.label);
            assert.equal(restored.friction,original.friction);
            assert.equal(restored.restitution,original.restitution);
            assert.equal(restored.massKg,original.massKg);
            for(const key of ['position','size','rotation']) similar(restored[key],original[key]);
        }
        assert.equal((await api('/api/spatial/list')).scenes.length,2);

        for(const [file,message] of [['wrong-scale.blend','scale reference disagrees'],['unsupported.blend','BOX rigid body']]) {
            await chooseOpen(path.join(output,file));
            await page.locator('#spatial-import-blender').click();
            await page.waitForFunction(text=>document.querySelector('#notice').textContent.includes(text),message,{timeout:25000});
            assert.equal(await current(),second.id);
            assert.equal((await api('/api/spatial/list')).scenes.length,2);
        }

        await page.locator('#spatial-skill').fill('blender-crate-route');
        await page.locator('#spatial-run').click();
        let goal;
        for(let i=0;i<500;i++) {
            goal=(await api('/api/goals/list')).goals.at(-1);
            if(goal && ['completed','failed','interrupted','cancelled'].includes(goal.status)) break;
            await delay(150);
        }
        assert(goal&&goal.status==='completed',JSON.stringify(goal));
        goal=await api('/api/goals/read',{id:goal.id});
        assert(goal.metrics.passed);
        assert.equal(goal.metrics.grabs,1);
        assert.equal(goal.metrics.releases,1);
        assert.equal(goal.savedSkill.name,'blender-crate-route');
        const report=await api('/api/artifact/read',{id:goal.report.id});
        assert(report.test.passed);
        assert.deepEqual(JSON.parse(report.contents['original-scene.json']),JSON.parse(second.sourceText));
        await page.locator('#spatial-tab').click();
        await page.waitForFunction(()=>document.querySelector('#spatial-live').textContent.includes('completed'),{},
            {timeout:20000});
        await capture('blender-spatial-verified.png');

        await page.locator('#game-tab').click();
        await page.waitForFunction(()=>document.querySelector('#game-frame').naturalWidth===960,{}, {timeout:60000});
        await page.locator('#game-relay').click();
        await page.waitForFunction(()=>document.querySelector('#notice').textContent.includes('Relay Garden:'),{}, {timeout:60000});
        const before=await api('/api/game/frame');
        await page.locator('#game-right').click();
        await page.waitForFunction(t=>document.querySelector('#game-status').textContent.includes('tick '+t),before.state.tick+30,
            {timeout:20000});
        const stepped=await api('/api/game/frame');
        assert(stepped.state.player.x>before.state.player.x);
        const layout=path.join(output,'relay-garden-tick-30.blend');
        await chooseSave(layout);
        await page.locator('#game-export-blender').click();
        await page.waitForFunction(()=>document.querySelector('#notice').textContent.includes('Saved the Game Lab layout'),{},
            {timeout:40000});
        await capture('blender-game-layout.png');
        const inspected=path.join(output,'inspected-game.json');
        await blender([layout],'inspect-game',inspected);
        const {text,quads}=JSON.parse(await fs.readFile(inspected,'utf8'));
        const checked=await api('/api/game/verify-layout',{text});
        assert(checked.passed);
        const changed=JSON.parse(text);
        changed.frame.source.tick+=1;
        await assert.rejects(api('/api/game/verify-layout',{text:JSON.stringify(changed)}),/checksum differs/);
        const frame=JSON.parse(text).frame;
        assert.equal(frame.source.sessionId,stepped.state.sessionId);
        assert.equal(frame.source.tick,stepped.state.tick);
        assert.equal(frame.source.levelId,'relay-garden-v1');
        assert.equal(frame.source.units,'legacy-game-unit');
        assert.equal((await api('/api/game/frame')).state.tick,stepped.state.tick);
        assert(quads>=100);
        await assert.rejects(importBlend(layout),/Metric or Imperial/);
        assert.deepEqual(rendererErrors,[]);
        await app.close();
        ({app,page}=await launch(project));
        assert.deepEqual(await read(second.id),second);
        assert((await api('/api/goals/list')).procedures.some(skill=>skill.name==='blender-crate-route'));

        const result={passed:true,blenderVersion:secondData.blenderVersion,
            exchange:{sourceSha256:sourceSha,exportSha256:exportedHash,sceneId:second.id,
                sourceIds:second.objects.map(body=>body.sourceId),roundTripToleranceM:.000001,
                scale:second.scaleCheck,rejections:['wrong scale','unsupported sphere collider'],restart:true},
            physics:{runId:goal.runId,reportId:goal.report.id,browserChecks:report.test.checks.length,metrics:goal.metrics},
            game:{levelId:frame.source.levelId,tick:frame.source.tick,quads,entities:frame.entities.length,
                units:frame.source.units,snapshotHash:checked.snapshotHash},
            screenshots:['blender-import.png','blender-spatial-verified.png','blender-game-layout.png']};
        await fs.writeFile(path.join(output,'result.json'),JSON.stringify(result,null,2)+'\n');
        console.log(JSON.stringify(result));
    } catch(error) {
        const notice=await page.locator('#notice').textContent().catch(()=>null);
        console.error('Workspace notice',notice);
        throw error;
    } finally {
        await app.close().catch(()=>{});
    }
}

main().catch(error=>{console.error(error);process.exitCode=1;});
