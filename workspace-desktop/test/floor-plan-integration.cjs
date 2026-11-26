'use strict';
const assert=require('node:assert/strict');
const {createHash}=require('node:crypto');
const fs=require('node:fs/promises');
const os=require('node:os');
const path=require('node:path');
const {launch}=require('./calibration-integration.cjs');
const {fixture,environmentGraphics}=require('./environment-integration.cjs');

const sha=bytes=>createHash('sha256').update(bytes).digest('hex');
const inWall=(x,z)=>x>=6&&x<74&&z>=4&&z<54&&(
    x<8||x>=72||z<6||z>=52||x>=38&&x<40&&(z<24||z>=34));
const at=(plan,x,z)=>{
    const {grid,rows}=plan;
    const row=rows[Math.floor((z-grid.originZM)/grid.cellM)];
    const column=Math.floor((x-grid.originXM)/grid.cellM);
    return Boolean(row?.some((start,i)=>i%2===0&&column>=start&&column<row[i+1]));
};
const close=(a,b)=>Math.abs(a-b)<1e-6;

async function main(){
    process.env.FRAYNIX_WORKSPACE_NO_MODELS='1';
    const out=path.resolve(process.argv[2]||'build/floor-plan-test');
    await fs.mkdir(out,{recursive:true});
    const project=await fs.mkdtemp(path.join(os.tmpdir(),'Fraynix plans '));
    let app,page;
    const start=async()=>{
        ({app,page}=await launch(project,environmentGraphics()));
        await page.locator('#environment-tab').click();
        await page.locator('#environment-plans summary').click();
    };
    const tell=text=>page.waitForFunction(
        text=>document.querySelector('#plan-result').textContent.includes(text),text,{timeout:12000});
    const api=(op,args={})=>app.evaluate((_,r)=>global.workspaceTest.api('/api/environment/'+r.op,r.args),{op,args});
    const ray=args=>app.evaluate((_,args)=>global.workspaceTest.execute('environment.probe',args),args);
    const capture=async name=>{
        await page.evaluate(()=>new Promise(resolve=>requestAnimationFrame(()=>requestAnimationFrame(resolve))));
        await fs.writeFile(path.join(out,name),Buffer.from(await app.evaluate(()=>global.workspaceTest.capture()),'base64'));
    };
    const save=async format=>{
        await page.locator('#plan-'+format).click();
        const prefix='Saved '+format.toUpperCase()+' to ';
        await tell(prefix);
        return fs.readFile((await page.locator('#plan-result').textContent()).slice(prefix.length));
    };
    const cut=(heightM,cellM=.1)=>api('floor-plan-section',{heightM,cellM});

    try {
        await start();
        const wallCells=Array.from({length:80*60},(_,i)=>inWall(i%80,Math.floor(i/80)));
        const png=await page.evaluate(wallCells=>{
            const canvas=document.createElement('canvas');
            canvas.width=80;canvas.height=60;
            const ctx=canvas.getContext('2d');
            ctx.fillStyle='#fff';ctx.fillRect(0,0,80,60);ctx.fillStyle='#101416';
            for(let z=0;z<60;z++) for(let x=0;x<80;x++) if(wallCells[z*80+x]) ctx.fillRect(x,z,1,1);
            // Transparent marks never become walls even though their decoded RGB can be zero.
            ctx.clearRect(14,12,9,12);
            return canvas.toDataURL('image/png').split(',')[1];
        },wallCells);
        const mask=Buffer.from(png,'base64'), maskPath=path.join(out,'two-room-mask.png');
        await fs.writeFile(maskPath,mask);
        await page.locator('#plan-file').setInputFiles(maskPath);
        await tell('Enter the full PNG width in metres.');
        assert.deepEqual(await api('read'),{});
        await page.locator('#plan-width').fill('8');
        await page.locator('#plan-file').setInputFiles(maskPath);
        await tell('plan cells verified against the mesh');
        await capture('plan-to-3d.png');

        const original=await api('read');
        assert(original.id);
        await page.locator('#plan-section').click();
        await tell('open edges');
        await page.locator('#plan-diagram').evaluate(img=>img.decode());
        const section=await cut(1.3);
        assert.deepEqual(section.plan.grid,{width:80,depth:60,originXM:0,originZM:0,cellM:.1});
        assert.equal(section.openEdges,0);
        let expected=0;
        for (let z=0;z<60;z++) for (let x=0;x<80;x++){
            assert.equal(at(section.plan,(x+.5)/10,(z+.5)/10),inWall(x,z), 'cell '+x+','+z);
            expected+=Number(inWall(x,z));
        }
        assert.equal(section.occupiedCells,expected);
        const throughDoor=await ray({origin:[2.5,1.3,2.9],direction:[1,0,0],maxDistanceM:3});
        assert.equal(throughDoor.hit,null);
        const atWall=await ray({origin:[2.5,1.3,1.9],direction:[1,0,0],maxDistanceM:3});
        assert(close(atWall.hit.distanceM,1.3));
        await assert.rejects(cut(9),/No triangles cross/);

        const planBytes=await save('json'), svg=await save('svg'), model=await save('glb');
        assert.equal(sha(model),original.id);
        assert.deepEqual(JSON.parse(planBytes),section.plan);
        assert(svg.toString().startsWith('<svg'));
        await fs.writeFile(path.join(out,'two-room-plan.json'),planBytes);
        await fs.writeFile(path.join(out,'two-room-cut.svg'),svg);
        await fs.writeFile(path.join(out,'two-room-generated.glb'),model);
        await app.close();

        await start();
        assert.equal((await api('read')).id,original.id);
        await page.locator('#goal').fill('Make a floor plan at 1.3 m');
        await page.locator('#run').click();
        await page.getByText('I made a horizontal floor plan at 1.3 m.',{exact:false}).waitFor();
        await page.locator('#plan-diagram').evaluate(img=>img.decode());
        await capture('3d-to-plan.png');
        assert((await save('svg')).equals(svg));
        await page.locator('#plan-rebuild').click();
        await tell('plan cells verified against the mesh');
        assert.deepEqual((await cut(1.3)).plan,section.plan);
        const retained=(await api('read')).id;
        const file=path.join(project,'environments',retained+'.glb');
        const exact=await fs.readFile(file);
        const tampered=Buffer.from(exact);
        tampered[tampered.length-1]^=1;
        try {
            await fs.writeFile(file,tampered);
            await assert.rejects(cut(1.3),/bytes changed/);
        } finally {await fs.writeFile(file,exact);}

        // Independently authored GLB with a node matrix, a rotated child, and an open vertical surface.
        const {document,encode}=await fixture();
        const parent=document.nodes[5];
        delete parent.translation;
        parent.matrix=[1,0,0,0,0,1,0,0,0,0,1,0,.25,0,0,1];
        const bench=document.nodes[9];
        bench.rotation=[0,Math.sin(Math.PI/8),0,Math.cos(Math.PI/8)];
        // A flattened box provides a distinct open contour without filling room space.
        document.nodes.push({name:'Open vertical screen',mesh:0,translation:[-2.3,1.2,1],scale:[0,2,1]});
        document.nodes[0].children.push(11);
        const external=encode(document), externalPath=path.join(out,'independent-pavilion.glb');
        await fs.writeFile(externalPath,external);
        await page.locator('#environment-file').setInputFiles(externalPath);
        await page.waitForFunction(()=>document.querySelector('#environment-status').textContent.includes('12 objects'));
        const externalCut=await cut(.3,.05);
        assert.equal(externalCut.id,sha(external));
        assert(externalCut.openEdges>0);
        assert(at(externalCut.plan,-2,-1.6));
        assert(at(externalCut.plan,-1.6,-1.95));
        assert(!at(externalCut.plan,-1.48,-1.6));
        assert(!at(externalCut.plan,-2.3,1));
        assert(!at(externalCut.plan,0,0));
        assert(at(externalCut.plan,1.5,0));
        await fs.writeFile(path.join(out,'independent-pavilion-cut.svg'),externalCut.svg);
        const receipt={sourceSha256:sha(mask),generatedId:original.id,grid:section.plan.grid,
            comparedCells:4800,exactCells:true,occupiedCells:expected,throughDoor:throughDoor.hit,
            adjacentWallM:atWall.hit.distanceM,restartSvgExact:true,tamperRefused:true,
            external:{id:externalCut.id,closedEdges:externalCut.closedEdges,openEdges:externalCut.openEdges}};
        await fs.writeFile(path.join(out,'floor-plan-evidence.json'),JSON.stringify(receipt,null,2)+'\n');
        console.log('Floor-plan round trip preserved all 4,800 measured cells, doorway, SVG and restart.');
    } catch (error) {
        console.error('Floor plan:',await page?.locator('#plan-result').textContent().catch(()=>null));
        console.error('Environment:',await page?.locator('#environment-status').textContent().catch(()=>null));
        throw error;
    } finally {
        if(app) await app.close().catch(()=>{});
        const absolute=await fs.realpath(project);
        if (path.dirname(absolute)===await fs.realpath(os.tmpdir())&&/^Fraynix plans [a-z0-9]+$/i.test(path.basename(absolute)))
            await fs.rm(absolute,{recursive:true,force:true,maxRetries:3}).catch(()=>{});
    }
}
main().catch(error=>{console.error(error);process.exitCode=1;});
