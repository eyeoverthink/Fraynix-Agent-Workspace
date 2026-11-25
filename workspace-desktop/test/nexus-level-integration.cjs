'use strict';

const assert=require('node:assert/strict');
const {createHash}=require('node:crypto');
const fs=require('node:fs/promises');
const os=require('node:os');
const path=require('node:path');
const {pathToFileURL}=require('node:url');
const {launch}=require('./calibration-integration.cjs');

const digest=bytes=>createHash('sha256').update(bytes).digest('hex');
const base64=data=>Buffer.from(data).toString('base64');
const writeJson=(filename,value)=>fs.writeFile(filename,JSON.stringify(value,null,2)+'\n');

async function main() {
    process.env.FRAYNIX_WORKSPACE_NO_MODELS='1';
    const out=path.resolve(process.argv[2]||'build/nexus257');
    const root=await fs.mkdtemp(path.join(os.tmpdir(),'Fraynix nexus '));
    const project=path.join(root,'author'), recipient=path.join(root,'recipient');
    const args=process.platform==='linux'?['--no-sandbox','--disable-dev-shm-usage']:[];
    await fs.mkdir(out,{recursive:true});
    let app,page;
    async function start(projectPath) {
        ({app,page}=await launch(projectPath,args));
        await page.waitForFunction(()=>document.querySelector('#engine-state')?.textContent.includes('0 local models'));
        await page.locator('#media-tab').click();
    }
    const call=(route,body={})=>app.evaluate((_,r)=>global.workspaceTest.api('/api/media-tiles/'+r.route,r.body),{route,body});
    const status=text=>page.waitForFunction(text=>document.querySelector('#tile-status')?.textContent.includes(text),text);
    const capture=async filename=>fs.writeFile(path.join(out,filename),
        Buffer.from(await app.evaluate(()=>global.workspaceTest.capture()),'base64'));
    const stage=layout=>layout.stage;
    const has=(view,selector,key,value)=>view.waitForFunction(r=>document.querySelector(r.selector)?.dataset[r.key]===r.value,
        {selector,key,value});
    const read=(view,selector)=>view.locator(selector).evaluate(element=>({...element.dataset}));
    const xReached=(view,selector,x)=>view.waitForFunction(r=>Number(document.querySelector(r.selector)?.dataset.playerX)>=r.x,
        {selector,x});

    async function run(view,selector,saveScreenshot=false) {
        const canvas=view.locator(selector);
        await canvas.focus();
        await has(view,selector,'hero','Kite the Courier');
        await has(view,selector,'support','nexus-10-1');
        assert.equal((await read(view,selector)).playerX,'144.00');
        // Verify authored pixels actually reach the live player renderer, with no stock avatar fallback.
        await view.waitForFunction(selector=>{
            const canvas=document.querySelector(selector);
            const [r,g,b]=canvas.getContext('2d').getImageData(154,275,1,1).data;
            return r>160&&g<140&&b>175;
        },selector);
        if (saveScreenshot) {
            await canvas.evaluate(element=>element.scrollIntoView({block:'center'}));
            await capture('nexus-in-play.png');
        }
        await view.keyboard.down('ArrowRight');
        await has(view,selector,'resets','1');
        await view.keyboard.up('ArrowRight');
        const reset=await read(view,selector);
        assert.equal(reset.playerX,'144.00');
        assert.equal(reset.collected,'0');

        // Jump the same hazard, pause over the coin to land, then seek the spring.
        await has(view,selector,'support','nexus-10-1');
        await view.keyboard.press('Space');
        await view.waitForFunction(selector=>Number(document.querySelector(selector)?.dataset.playerY)<245,selector);
        await view.keyboard.down('ArrowRight');
        await xReached(view,selector,238);
        await view.keyboard.up('ArrowRight');
        await has(view,selector,'collected','1');
        const collected=await read(view,selector);
        assert.equal(collected.resets,'1');
        await view.keyboard.down('ArrowRight');
        await has(view,selector,'bounces','1');
        await view.waitForFunction(selector=>Number(document.querySelector(selector)?.dataset.playerY)<213,selector);
        const boosted=await read(view,selector);
        assert.equal(boosted.resets,'1');
        await has(view,selector,'won','true');
        await view.keyboard.up('ArrowRight');
        const completed=await read(view,selector);
        assert.equal(completed.resets,'1');
        assert.equal(completed.bounces,'1');
        assert.equal(completed.collected,'1');
        assert.equal(completed.phase,'complete');
        assert(Number(completed.playerX)>=478&&Number(completed.playerX)<520);
        return {spawnX:144,resets:1,collected:1,bounces:1,goalReached:true};
    }

    try {
        await start(project);
        const images=await page.evaluate(()=>{
            const make=(w,h,draw)=>{
                const c=document.createElement('canvas');
                c.width=w;c.height=h;
                draw(c.getContext('2d'));
                return c.toDataURL('image/png');
            };
            const hero=make(40,48,g=>{
                g.fillStyle='#9af4e9';g.fillRect(8,3,25,13);
                g.fillStyle='#1e2a49';g.fillRect(18,7,13,6);
                g.fillStyle='#dd64ec';g.fillRect(8,16,24,19);
                g.fillStyle='#9af4e9';g.fillRect(13,20,10,4);
                g.fillStyle='#f0a578';g.fillRect(7,33,11,12);g.fillRect(23,33,11,12);
                g.fillStyle='#9af4e9';g.fillRect(4,45,15,3);g.fillRect(23,45,14,3);
            });
            const backdrop=make(480,224,g=>{
                const sky=g.createLinearGradient(0,0,0,224);
                sky.addColorStop(0,'#291a45');sky.addColorStop(.55,'#643a70');sky.addColorStop(1,'#253759');
                g.fillStyle=sky;g.fillRect(0,0,480,224);
                g.fillStyle='#f2a584';g.beginPath();g.arc(322,73,44,0,Math.PI*2);g.fill();
                g.fillStyle='#5b466f';
                for(let i=0;i<9;i++) g.fillRect(i*61,135-(i*17%39),39,140);
                g.fillStyle='#373754';
                for(let i=0;i<14;i++) g.fillRect(i*37,154-(i*23%42),26,130);
                g.fillStyle='#f3afaa';
                for(let i=0;i<12;i++) for(let j=0;j<4;j++) g.fillRect(i*40+6,169+j*9,2,3);
            });
            return {hero,backdrop};
        });
        const grid=Array.from({length:11},()=>Array(20).fill(0));
        grid[10].fill(1);
        grid[9][1]=7;grid[9][3]=3;grid[9][5]=4;grid[9][7]=5;grid[9][16]=6;
        grid[9][13]=2;grid[7][10]=2;
        const world={name:'Twilight Relay',styleId:'synthetic-neon-rooftop',layerRole:'background',
            opacity:.9,parallaxSpeed:.65,biomePalette:{sky:'#291a45',ground:'#354365',rock:'#705486',accent:'#a0eddb',water:'#e8b0ce'},
            sourceImageUrl:images.backdrop};
        const characters=[{id:'kite',role:'playable',name:'Kite the Courier',behavior:'platform_hero',
            spriteDataUrl:images.hero,speed:235,jumpPower:475,scale:1,animMode:'stride'}];
        const contract={world,characters,grid};
        const inputHtml='<!doctype html><html><body><script>\nconst SCENE = '+JSON.stringify(contract)+
            ';\nconst WASM_B64 = "ignored";\nthrow Error("The Nexus input script must never run.");\n</script></body></html>';
        const inputFile=path.join(out,'twilight-runner-input.html');
        await fs.writeFile(inputFile,inputHtml);
        const scene={version:'2.0',world,characters,levelGrid2D:grid};
        await writeJson(path.join(out,'twilight-source.scene.json'),scene);
        await assert.rejects(call('import-nexus',{base64:base64(JSON.stringify({...scene,levelGrid2D:null}))}),/after extracting a 2D grid/);
        const bad=structuredClone(scene);
        bad.levelGrid2D[9][2]=7;
        await assert.rejects(call('import-nexus',{base64:base64(JSON.stringify(bad))}),/one Nexus spawn and one goal/);
        bad.levelGrid2D[9][2]=0;
        bad.characters[0].spriteDataUrl='https://example.invalid/not-local.png';
        await assert.rejects(call('import-nexus',{base64:base64(JSON.stringify(bad))}),/embedded PNG/);
        assert.deepEqual((await call('list')).levels,[]);
        assert.deepEqual((await call('library')).assets,[]);

        await page.locator('#tile-import-nexus').setInputFiles(inputFile);
        await status('Imported a playable Nexus level');
        const imported=await call('open');
        assert.equal(stage(imported.layout).sourceSha256,digest(inputHtml));
        const same=await call('import-nexus',{base64:base64(inputHtml)});
        assert.deepEqual(same,imported);
        const jsonLevel=await call('import-nexus',{base64:base64(await fs.readFile(path.join(out,'twilight-source.scene.json')))});
        if (process.env.FRAYNIX_NEXUS_RUNNER_FIXTURE) {
            const runner=await fs.readFile(process.env.FRAYNIX_NEXUS_RUNNER_FIXTURE);
            const real=await call('import-nexus',{base64:base64(runner)});
            const shape=structuredClone(real.layout);
            assert.equal(shape.stage.sourceSha256,digest(runner));
            shape.stage.sourceSha256=imported.layout.stage.sourceSha256;
            assert.deepEqual(shape,imported.layout);
        }
        const copy=structuredClone(jsonLevel.layout);
        copy.stage.sourceSha256=imported.layout.stage.sourceSha256;
        assert.deepEqual(copy,imported.layout);
        const alternate=structuredClone(scene);
        alternate.world.styleId='synthetic-sunrise';
        alternate.world.biomePalette.sky='#eed57a';
        alternate.world.sourceImageUrl='https://example.invalid/offline-backdrop.png';
        const alternateLevel=await call('import-nexus',{base64:base64(JSON.stringify(alternate))});
        assert.equal(stage(alternateLevel.layout).style,'synthetic-sunrise');
        assert.equal(stage(alternateLevel.layout).palette.sky,'#eed57a');
        assert.equal(stage(alternateLevel.layout).backdropAsset,undefined);
        assert.deepEqual(stage(alternateLevel.layout).spawn,stage(imported.layout).spawn);
        assert.deepEqual(stage(alternateLevel.layout).goal,stage(imported.layout).goal);
        const mechanics=layout=>layout.objects.map(({effect,x,y,collider})=>({effect,x,y,collider}));
        assert.deepEqual(mechanics(alternateLevel.layout),mechanics(imported.layout));
        assert.deepEqual(stage(imported.layout).spawn,{x:144,y:256});
        assert.deepEqual(stage(imported.layout).goal,{x:504,y:264});
        assert.equal(stage(imported.layout).style,'synthetic-neon-rooftop');
        assert.equal(stage(imported.layout).hero.speed,235);
        assert.equal(stage(imported.layout).hero.jump,475);
        assert.equal(Object.keys(imported.sprites).length,3);
        const effectCounts={};
        for (const item of imported.layout.objects) effectCounts[item.effect]=(effectCounts[item.effect]||0)+1;
        assert.deepEqual(effectCounts,{ground:20,brick:2,hazard:1,coin:1,spring:1});

        // Edit the goal through the desktop and prove the former source lineage remains attached.
        await page.locator('#tile-stage summary').click();
        assert.match(await page.locator('#tile-stage-hero').textContent(),/Kite the Courier/);
        const goal=page.locator('[data-stage="goal.x"]');
        await goal.fill('506');
        await goal.press('Tab');
        await page.locator('#tile-save').click();
        await status('Saved level. Sprite sources');
        const edited=await call('open');
        assert.notEqual(edited.id,imported.id);
        assert.deepEqual(stage(edited.layout).goal,{x:506,y:264});
        assert.equal(stage(edited.layout).sourceSha256,stage(imported.layout).sourceSha256);
        await page.locator('#tile-canvas').evaluate(element=>element.scrollIntoView({block:'center'}));
        await capture('nexus-level-editor.png');
        await page.locator('#tile-play').click();
        const inApp=await run(page,'#tile-canvas',true);
        await page.locator('#tile-stop').click();

        const outputs={};
        for (const format of ['html','json']) {
            await page.locator('#tile-'+format).click();
            await page.waitForFunction(ext=>document.querySelector('#tile-status')?.textContent.endsWith('.'+ext),format);
            const filename=(await page.locator('#tile-status').textContent()).trim().slice('Saved '.length);
            const bytes=await fs.readFile(filename);
            const receipt=JSON.parse(await fs.readFile(filename+'.receipt.json','utf8'));
            assert.equal(receipt.source.id,edited.id);
            assert.equal(receipt.sha256,digest(bytes));
            const saved='twilight-relay.'+format;
            await fs.writeFile(path.join(out,saved),bytes);
            outputs[format]={name:saved,sha256:receipt.sha256,bytes:bytes.length};
        }
        const player=await fs.readFile(path.join(out,outputs.html.name),'utf8');
        assert(!player.includes('const WASM_B64'));
        assert(!player.includes('input script must never run'));
        await app.close();

        await start(project);
        await status('Reopened checked level');
        const reopened=await call('open');
        assert.equal(reopened.id,edited.id);
        assert.deepEqual(reopened.layout,edited.layout);
        assert.equal((await call('export',{id:edited.id,format:'html'})).sha256,outputs.html.sha256);
        await page.locator('#tile-play').click();
        const afterRestart=await run(page,'#tile-canvas');
        await app.close();

        await start(recipient);
        await page.locator('#tile-import-level').setInputFiles(path.join(out,outputs.json.name));
        await status('Imported checked level');
        assert.equal((await call('open')).id,edited.id);
        assert.equal((await call('export',{id:edited.id,format:'html'})).sha256,outputs.html.sha256);
        const url=pathToFileURL(path.join(out,outputs.html.name)).href;
        await app.evaluate(async ({BrowserWindow},url)=>{
            const window=new BrowserWindow({width:950,height:610,webPreferences:{
                partition:'nexus-standalone-proof',sandbox:true,nodeIntegration:false,contextIsolation:true
            }});
            window.webContents.session.webRequest.onBeforeRequest((request,done)=>done({cancel:!/^(file|data):/.test(request.url)}));
            await window.loadURL(url);
        },url);
        const portable=app.windows().find(window=>window.url()===url);
        assert(portable);
        const standalone=await run(portable,'canvas');
        const thumbnail=portable.locator('canvas');
        await thumbnail.screenshot({path:path.join(out,'nexus-standalone-complete.png')});
        await writeJson(path.join(out,'nexus-evidence.json'),{
            modelCount:0,offline:true,sourceHash:digest(inputHtml),levelId:edited.id,sourceParsers:['runner embedded JSON','version 2 scene JSON'],
            effectCounts,goalAfterEdit:stage(edited.layout).goal,hero:stage(edited.layout).hero.name,
            inApp,afterRestart,standalone,outputs,cleanProjectExact:true,
            originalExporterChecked:Boolean(process.env.FRAYNIX_NEXUS_RUNNER_FIXTURE), styleVariantPreservedMechanics:true
        });
        console.log('Nexus scene parsed as data, played with its original hero and mechanics, and reopened as exact offline exports.');
    } finally {
        if (app) await app.close().catch(()=>{});
        const logs=await fs.readFile(path.join(project,'test-desktop-process.log')).catch(()=>null);
        if (logs) await fs.writeFile(path.join(out,'desktop-process.log'),logs);
        const temp=await fs.realpath(os.tmpdir());
        const testRoot=await fs.realpath(root);
        if (path.dirname(testRoot)===temp && /^Fraynix nexus [a-z0-9]+$/i.test(path.basename(testRoot)))
            await fs.rm(testRoot,{recursive:true,force:true,maxRetries:3}).catch(()=>{});
    }
}
main().catch(error=>{console.error(error);process.exitCode=1;});
