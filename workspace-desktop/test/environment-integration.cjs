'use strict';
const assert=require('node:assert/strict');
const {createHash}=require('node:crypto');
const fs=require('node:fs/promises');
const path=require('node:path');
const os=require('node:os');
const {pathToFileURL}=require('node:url');
const {launch}=require('./calibration-integration.cjs');

const digest=bytes=>createHash('sha256').update(bytes).digest('hex');
const hex=value=>Buffer.from(value).toString('base64');
const padding=(bytes,fill=0)=>Buffer.concat([bytes,Buffer.alloc((4-bytes.length%4)%4,fill)]);

async function fixture() {
    const dependency=pathToFileURL(path.join(__dirname,'..','node_modules','three','build','three.module.js'));
    const {BoxGeometry}=await import(dependency);
    const box=new BoxGeometry(1,1,1);
    const texture=await fs.readFile(path.join(__dirname,'fixtures','timeline-edit','overlay.png'));
    const views=[], payload=[];
    const add=bytes=>{
        const offset=payload.reduce((sum,part)=>sum+part.length,0);
        views.push({buffer:0,byteOffset:offset,byteLength:bytes.byteLength});
        payload.push(padding(bytes));
        return views.length-1;
    };
    const packed=values=>Buffer.from(values.buffer,values.byteOffset,values.byteLength);
    const start=add(packed(box.attributes.position.array)), normal=add(packed(box.attributes.normal.array));
    const uv=add(packed(box.attributes.uv.array)), indices=add(packed(box.index.array)), image=add(texture);
    const access=[
        {bufferView:start,componentType:5126,count:24,type:'VEC3',min:[-.5,-.5,-.5],max:[.5,.5,.5]},
        {bufferView:normal,componentType:5126,count:24,type:'VEC3'},
        {bufferView:uv,componentType:5126,count:24,type:'VEC2'},
        {bufferView:indices,componentType:5123,count:36,type:'SCALAR'}
    ];
    const mesh=(name,material)=>({name,primitives:[{attributes:{POSITION:0,NORMAL:1,TEXCOORD_0:2},indices:3,material}]});
    const cube=(name,mesh,translation,scale)=>({name,mesh,translation,scale});
    const nodes=[
        {name:'Two-room pavilion',children:[1,5,10]},
        {name:'Structure',children:[2,3,4,6,7,8]},
        cube('Floor',0,[0,-.1,0],[6,.2,5]),
        cube('West doorway wall',1,[-1.8,1.2,0],[2.4,2.4,.2]),
        cube('East doorway wall',1,[1.8,1.2,0],[2.4,2.4,.2]),
        {name:'Furnishings',translation:[.25,0,0],children:[9]},
        cube('Rear wall',1,[0,1.2,-2.45],[6,2.4,.1]),
        cube('Left wall',1,[-2.95,1.2,0],[.1,2.4,5]),
        cube('Roof slab',0,[0,2.45,0],[6,.1,5]),
        cube('Mosaic bench',2,[-2.25,.35,-1.6],[1.4,.7,.6]),
        {name:'INTERACT_inspect_bench',translation:[-2,.9,-1.6],extras:{escape_label:'Inspect mosaic bench'}}
    ];
    const bin=Buffer.concat(payload);
    const material=(name,color)=>({name,pbrMetallicRoughness:{baseColorFactor:[...color,1],metallicFactor:0,roughnessFactor:.8}});
    const mosaic=material('Mosaic tile',[1,1,1]);
    mosaic.pbrMetallicRoughness.baseColorTexture={index:0,extensions:{KHR_texture_transform:{offset:[.2,.15],scale:[.7,.7]}}};
    const document={
        asset:{version:'2.0',generator:'Fraynix synthetic environment test'},
        extensionsUsed:['KHR_texture_transform'],extensionsRequired:['KHR_texture_transform'],
        scene:0,scenes:[{nodes:[0]}],nodes,
        materials:[material('Blue-grey slab',[.18,.27,.34]),material('Ochre plaster',[.66,.36,.16]),mosaic],
        meshes:[mesh('Slab',0),mesh('Partition',1),mesh('Mosaic',2)],
        accessors:access,bufferViews:views,images:[{bufferView:image,mimeType:'image/png'}],
        textures:[{source:0}],buffers:[{byteLength:bin.length}]
    };
    function encode(data) {
        const json=padding(Buffer.from(JSON.stringify(data)),32);
        const result=Buffer.alloc(28+json.length+bin.length);
        result.write('glTF');
        result.writeUInt32LE(2,4);
        result.writeUInt32LE(result.length,8);
        result.writeUInt32LE(json.length,12);
        result.write('JSON',16);
        json.copy(result,20);
        result.writeUInt32LE(bin.length,20+json.length);
        result.write('BIN\0',24+json.length);
        bin.copy(result,28+json.length);
        return result;
    }
    return {document,encode};
}

function environmentGraphics() {
    if (process.platform==='linux') return ['--no-sandbox','--disable-dev-shm-usage','--use-angle=swiftshader','--enable-unsafe-swiftshader'];
    if (process.env.CI && process.platform==='darwin' && process.arch==='x64')
        return ['--use-gl=angle','--use-angle=swiftshader','--enable-unsafe-swiftshader'];
    return [];
}

async function main() {
    process.env.FRAYNIX_WORKSPACE_NO_MODELS='1';
    const out=path.resolve(process.argv[2]||'build/env260');
    await fs.mkdir(out,{recursive:true});
    const project=await fs.mkdtemp(path.join(os.tmpdir(),'Fraynix environments '));
    const flags=environmentGraphics();
    let app,page;
    const start=async()=>{
        ({app,page}=await launch(project,flags));
        await page.waitForFunction(()=>document.querySelector('#engine-state')?.textContent.includes('0 local models'));
        await page.locator('#environment-tab').click();
    };
    const api=(op,args={})=>app.evaluate((_,r)=>global.workspaceTest.api('/api/environment/'+r.op,r.args),{op,args});
    const tool=(name,args={})=>app.evaluate((_,r)=>global.workspaceTest.execute(r.name,r.args),{name,args});
    const tell=part=>page.waitForFunction(part=>document.querySelector('#environment-status')?.textContent.includes(part),part);
    const capture=async name=>fs.writeFile(path.join(out,name),Buffer.from(await app.evaluate(()=>global.workspaceTest.capture()),'base64'));
    const node=index=>page.locator('[data-environment-node="'+index+'"]');

    try {
        const {document,encode}=await fixture();
        const bytes=encode(document), model=path.join(out,'two-room-pavilion.glb');
        await fs.writeFile(model,bytes);
        await start();

        const external=structuredClone(document);
        external.images[0].uri='https://example.invalid/outside.png';
        await assert.rejects(api('import',{filename:'external.glb',base64:hex(encode(external))}),/Embed PNG or JPEG/);
        const loop=structuredClone(document);
        loop.nodes[1].children.push(0);
        await assert.rejects(api('import',{filename:'cycle.glb',base64:hex(encode(loop))}),/cycle/);
        const required=structuredClone(document);
        required.extensionsRequired.push('VENDOR_missing');
        await assert.rejects(api('import',{filename:'extension.glb',base64:hex(encode(required))}),/Unsupported required GLB/);
        assert.deepEqual(await api('read'),{});
        await page.locator('#environment-file').setInputFiles(model);
        await tell('11 objects');
        let manifest=await api('read');
        assert.equal(manifest.id,digest(bytes));
        assert.equal(manifest.meshNodes,7);
        assert.equal(manifest.triangleCount,84);
        assert.equal(manifest.imageCount,1);
        assert.equal(manifest.markerCount,1);
        await page.locator('#environment-cut').check();
        await page.locator('#environment-cut-height').fill('1.72');
        await page.locator('#environment-filter').fill('mosaic');
        assert.equal(await page.locator('[data-environment-node]').count(),2);
        await node(9).click();
        await page.waitForFunction(()=>document.querySelector('#environment-properties').textContent.includes('Bounds size'));
        const bench=await tool('environment.read',{filter:'mosaic',node:9});
        assert.equal(bench.details.parent,5);
        assert.deepEqual(bench.details.position,[-2,.35,-1.6]);
        assert(bench.details.dimensions.every((size,i)=>Math.abs(size-[1.4,.7,.6][i])<1e-7));
        await page.waitForFunction(()=>{
            const canvas=document.querySelector('#environment-stage canvas');
            const ctx=canvas.getContext('webgl2');
            const pixels=new Uint8Array(canvas.width*canvas.height*4);
            ctx.readPixels(0,0,canvas.width,canvas.height,ctx.RGBA,ctx.UNSIGNED_BYTE,pixels);
            const colors=new Set();
            for (let i=0;i<pixels.length;i+=64) colors.add(pixels[i]+','+pixels[i+1]+','+pixels[i+2]);
            return colors.size>30;
        });
        await capture('environment-section.png');

        const openDoor=await tool('environment.probe',{origin:[0,1.2,2],direction:[0,0,-1],maxDistanceM:3});
        const partition=await tool('environment.probe',{origin:[1.5,1.2,2],direction:[0,0,-1],maxDistanceM:3});
        assert.equal(openDoor.hit,null);
        assert.equal(partition.hit.node,4);
        assert(Math.abs(partition.hit.distanceM-1.9)<1e-7);
        assert(partition.hit.point.slice(0,2).every((value,i)=>Math.abs(value-[1.5,1.2][i])<1e-7));
        await page.locator('#environment-markers').check();
        await node(10).click();
        await page.waitForFunction(()=>document.querySelector('#environment-properties').textContent.includes('Interaction')||
            document.querySelector('#environment-properties').textContent.includes('Inspect mosaic bench'));
        const marker=(await tool('environment.read',{node:10})).details;
        assert.equal(marker.marker,'Inspect mosaic bench');
        assert.equal(marker.dimensions,null);
        await app.close();

        await start();
        await tell('11 objects');
        assert.equal((await api('read')).selected,10);
        const replay=(await tool('environment.read',{limit:1})).details;
        assert.deepEqual(replay,marker);
        await page.locator('#goal').fill('Inspect the imported environment');
        await page.locator('#run').click();
        await page.getByText('Interaction marker: Inspect mosaic bench.',{exact:false}).waitFor();
        await capture('environment-chat.png');
        await page.locator('#environment-export').click();
        await tell('Saved inventory to ');
        const savedPath=(await page.locator('#environment-status').textContent()).slice('Saved inventory to '.length);
        const inventory=JSON.parse(await fs.readFile(savedPath,'utf8'));
        assert.equal(inventory.id,manifest.id);
        assert.equal(inventory.nodes.length,11);
        assert.deepEqual(inventory.nodes.find(node=>node.index===9),bench.details);
        await fs.copyFile(savedPath,path.join(out,'pavilion-inventory.json'));
        const original=path.join(project,'environments',manifest.id+'.glb');
        const changed=Buffer.from(bytes);
        changed[changed.length-1]^=1;
        try {
            await fs.writeFile(original,changed);
            await assert.rejects(api('read'),/bytes changed/);
            await assert.rejects(tool('environment.probe',{origin:[0,1,2],direction:[0,0,-1]}),/bytes changed/);
        } finally {await fs.writeFile(original,bytes);}
        assert.equal((await api('read')).id,manifest.id);
        await fs.writeFile(path.join(out,'environment-evidence.json'),JSON.stringify({sourceId:manifest.id,total:11,
            meshes:manifest.meshNodes,triangles:manifest.triangleCount,images:manifest.imageCount,markers:manifest.markerCount,
            bench:bench.details,marker,openDoor,partition,persistedSelection:true,changedBytesRefused:true,offline:true},null,2)+'\n');
        console.log('Verified environment hierarchy, texture, transformed bounds, ray hits and restart.');
    } catch (error) {
        if (page) console.error('Environment status',await page.locator('#environment-status').textContent().catch(()=>null));
        throw error;
    } finally {
        if (app) await app.close().catch(()=>{});
        const log=await fs.readFile(path.join(project,'test-desktop-process.log')).catch(()=>null);
        if (log) await fs.writeFile(path.join(out,'desktop-process.log'),log);
        const actual=await fs.realpath(project);
        if (path.dirname(actual)===await fs.realpath(os.tmpdir())&&/^Fraynix environments [a-z0-9]+$/i.test(path.basename(actual)))
            await fs.rm(actual,{recursive:true,force:true,maxRetries:3}).catch(()=>{});
    }
}
if (require.main===module) main().catch(error=>{console.error(error);process.exitCode=1;});
module.exports={fixture,environmentGraphics};
