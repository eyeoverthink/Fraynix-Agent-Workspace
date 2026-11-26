'use strict';
const assert=require('node:assert/strict');
const {createHash}=require('node:crypto');
const fs=require('node:fs/promises');
const os=require('node:os');
const path=require('node:path');
const {launch}=require('./calibration-integration.cjs');
const {fixture,environmentGraphics,forceGraphicsFailure}=require('./environment-integration.cjs');

const sha=bytes=>createHash('sha256').update(bytes).digest('hex');
const CLOSE=1e-4;

async function facade(){
    const {document,encode}=await fixture();
    const box=(name,x1,x2,y1,y2)=>({name,mesh:1,
        translation:[(x1+x2)/2,(y1+y2)/2,0],scale:[x2-x1,y2-y1,.2]});
    document.nodes=[
        {name:'Door and window facade',children:[1,2,3,4,5,6]},
        box('Left return',-3,-1.75,0,2.6),
        box('Middle pier',-.75,.75,0,2.6),
        box('Right return',2.25,3,0,2.6),
        box('Door header',-1.75,-.75,2.1,2.6),
        box('Window sill',.75,2.25,0,.9),
        box('Window header',.75,2.25,2.1,2.6)
    ];
    return encode(document);
}

async function main(){
    process.env.FRAYNIX_WORKSPACE_NO_MODELS='1';
    const out=path.resolve(process.argv[2]||'build/height-layers-test');
    await fs.mkdir(out,{recursive:true});
    const project=await fs.mkdtemp(path.join(os.tmpdir(),'Fraynix heights '));
    let app,page;
    const start=async()=>{
        ({app,page}=await launch(project,environmentGraphics()));
        await forceGraphicsFailure(page);
        await page.locator('#environment-tab').click();
        await page.locator('#environment-height-layers summary').click();
    };
    const tell=text=>page.waitForFunction(
        text=>document.querySelector('#layers-status').textContent.includes(text),text,{timeout:20000});
    const api=(op,args={})=>app.evaluate((_,request)=>global.workspaceTest.api('/api/environment/'+request.op,request.args),{op,args});
    const rays=async()=>{
        const points=[
            {name:'door below lintel',x:-1.25,y:1.37,blocked:false},
            {name:'door lintel',x:-1.25,y:2.33,blocked:true},
            {name:'window sill',x:1.5,y:.41,blocked:true},
            {name:'window opening',x:1.5,y:1.27,blocked:false},
            {name:'window lintel',x:1.5,y:2.37,blocked:true},
            {name:'middle pier',x:0,y:1.31,blocked:true}
        ];
        for (const point of points) {
            const args={origin:[point.x,point.y,1],direction:[0,0,-1],maxDistanceM:2};
            const hit=await app.evaluate((_,args)=>global.workspaceTest.execute('environment.probe',args),args);
            assert.equal(Boolean(hit.hit),point.blocked,point.name);
            point.distanceM=hit.hit?.distanceM??null;
            if(point.blocked) assert(Math.abs(point.distanceM-.9)<CLOSE,point.name);
        }
        return points;
    };
    const capture=async name=>{
        await page.evaluate(()=>new Promise(resolve=>requestAnimationFrame(()=>requestAnimationFrame(resolve))));
        await fs.writeFile(path.join(out,name),Buffer.from(await app.evaluate(()=>global.workspaceTest.capture()),'base64'));
    };

    try {
        await start();
        const original=await facade(), sourceId=sha(original);
        const fixturePath=path.join(out,'door-window-source.glb');
        await fs.writeFile(fixturePath,original);
        await page.locator('#environment-file').setInputFiles(fixturePath);
        await page.waitForFunction(()=>document.querySelector('#environment-status').textContent.includes('7 objects'));
        if (process.env.FRAYNIX_TEST_NO_WEBGL==='1')
            assert.equal(await page.locator('#environment-stage').getAttribute('data-graphics'),'geometry');
        const originalRays=await rays();

        const broad=await api('height-audit',{id:sourceId,boundsM:[0,2.6],cellM:.05});
        assert(broad.varyingBandCells>0);
        await page.locator('#layers-restore').click();
        await tell('vary.');
        assert(await page.locator('#layers-build').isDisabled());
        await page.locator('#layers-diagram').evaluate(img=>img.decode());
        await capture('varying-layer.png');
        await assert.rejects(api('height-rebuild',{auditId:broad.auditId}),/explicitly rebuild/);
        assert.equal((await api('read')).id,sourceId);
        await assert.rejects(api('height-audit',{id:sourceId,boundsM:[0,.9,.89],cellM:.05}),/must rise/);

        await page.locator('#layers-bounds').fill('0, 0.9, 2.1, 2.6');
        await page.locator('#layers-cell').fill('0.05');
        await page.locator('#goal').fill('Measure the height layers');
        await page.locator('#run').click();
        await page.getByText('I measured 3 height bands.',{exact:false}).waitFor();
        await tell('0 vary.');
        const report=await api('height-read',{sourceId});
        assert.deepEqual(report.boundsM,[0,.9,2.1,2.6]);
        assert.equal(report.varyingBandCells,0);
        assert.equal(report.bands.flatMap(b=>b.samples).length,9);
        for (const band of report.bands) {
            assert.equal(new Set(band.samples.map(s=>s.planSha256)).size,1);
            assert.equal(band.openEdgesAcrossSamples,0);
        }
        await page.locator('#layers-band').selectOption('1');
        await page.locator('#layers-diagram').evaluate(img=>img.decode());
        await capture('reviewed-layers.png');

        await page.locator('#layers-export').click();
        const prefix='Saved audit JSON to ';
        await tell(prefix);
        const auditFile=(await page.locator('#layers-status').textContent()).slice(prefix.length);
        const auditBytes=await fs.readFile(auditFile);
        assert.deepEqual(JSON.parse(auditBytes),report);
        await fs.writeFile(path.join(out,'door-window-audit.json'),auditBytes);

        await page.locator('#layers-build').click();
        await tell('checked at 9 heights');
        const built=await api('open');
        assert.deepEqual(built.layerSource,{id:sourceId,auditId:report.auditId});
        const mesh=Buffer.from(built.base64,'base64');
        assert.equal(sha(mesh),built.id);
        await fs.writeFile(path.join(out,'door-window-rebuilt.glb'),mesh);
        const builtRays=await rays();
        assert.deepEqual(builtRays.map(r=>r.blocked),originalRays.map(r=>r.blocked));
        for (let i=0;i<builtRays.length;i++) if (builtRays[i].blocked)
            assert(Math.abs(builtRays[i].distanceM-originalRays[i].distanceM)<CLOSE);
        await page.locator('#environment-height-layers summary').click();
        await capture('layered-model.png');
        await app.close();

        await start();
        await page.locator('#layers-restore').click();
        await tell('0 vary.');
        assert.deepEqual(await api('height-read',{auditId:report.auditId}),report);
        assert.equal((await api('read')).id,built.id);
        await page.locator('#layers-source').click();
        await page.waitForFunction(()=>document.querySelector('#environment-status').textContent.includes('door-window-source.glb'));
        assert.equal((await api('read')).id,sourceId);
        const kept=await api('height-rebuild',{auditId:broad.auditId,omitVarying:true});
        assert.equal(kept.omittedBandCells,broad.varyingBandCells);
        assert.equal((await api('read')).id,sourceId);
        const target=path.join(project,'environments',sourceId+'.glb'), changed=Buffer.from(original);
        changed[changed.length-1]^=1;
        try {
            await fs.writeFile(target,changed);
            await assert.rejects(api('height-read',{auditId:report.auditId}),/bytes changed/);
            await assert.rejects(api('height-rebuild',{auditId:report.auditId}),/bytes changed/);
        } finally {await fs.writeFile(target,original);}
        assert.deepEqual(await api('height-read',{sourceId}),report);
        const evidence={sourceId,auditId:report.auditId,builtId:built.id,solidBandCells:report.solidBandCells,
            bandCount:3,witnessCuts:9,verifiedRays:builtRays,broadVaryingCells:broad.varyingBandCells,
            exactAfterRestart:true,tamperedSourceRefused:true};
        await fs.writeFile(path.join(out,'height-layer-evidence.json'),JSON.stringify(evidence,null,2)+'\n');
        console.log('Verified nine slices, held-out rays and restart. The broader changing band required an explicit partial rebuild.');
    } catch (error) {
        console.error('Layers:',await page?.locator('#layers-status').textContent().catch(()=>null));
        throw error;
    } finally {
        if(app) await app.close().catch(()=>{});
        const absolute=await fs.realpath(project);
        if(path.dirname(absolute)===await fs.realpath(os.tmpdir())&&/^Fraynix heights [a-z0-9]+$/i.test(path.basename(absolute)))
            await fs.rm(absolute,{recursive:true,force:true,maxRetries:3}).catch(()=>{});
    }
}
main().catch(error=>{console.error(error);process.exitCode=1;});
