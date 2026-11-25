'use strict';
const assert=require('node:assert/strict');
const {createHash}=require('node:crypto');
const fs=require('node:fs/promises');
const path=require('node:path');
const os=require('node:os');
const {pathToFileURL}=require('node:url');
const {launch}=require('./calibration-integration.cjs');
const sha=bytes=>createHash('sha256').update(bytes).digest('hex');
const fixture=path.resolve(__dirname,'..','..','docs','readmes','257','twilight-source.scene.json');

async function main() {
    process.env.FRAYNIX_WORKSPACE_NO_MODELS='1';
    const out=path.resolve(process.argv[2]||'build/tile258');
    const project=await fs.mkdtemp(path.join(os.tmpdir(),'Fraynix tile evidence '));
    const flags=process.platform==='linux'?['--no-sandbox','--disable-dev-shm-usage']:[];
    await fs.mkdir(out,{recursive:true});
    let app,page,sceneSelector='#tile-canvas';
    const api=(name,args={})=>app.evaluate((_,{name,args})=>global.workspaceTest.api('/api/media-tiles/'+name,args),{name,args});
    const tool=(name,args={})=>app.evaluate((_,{name,args})=>global.workspaceTest.execute(name,args),{name,args});
    const q=id=>page.locator('#'+id);
    const status=text=>page.waitForFunction(text=>document.querySelector('#tile-status')?.textContent.includes(text),text);
    const findState=(key,value)=>page.waitForFunction(r=>document.querySelector(r.selector)?.dataset[r.key]===String(r.value),
        {key,value,selector:sceneSelector});
    const xAt=x=>page.waitForFunction(r=>Number(document.querySelector(r.selector)?.dataset.playerX)>=r.x,
        {x,selector:sceneSelector});
    const capture=async name=>fs.writeFile(path.join(out,name),Buffer.from(await app.evaluate(()=>global.workspaceTest.capture()),'base64'));
    const pixel=(x,y)=>q('tile-canvas').evaluate((canvas,{x,y})=>Array.from(canvas.getContext('2d').getImageData(x,y,1,1).data),{x,y});
    async function open() {
        ({app,page}=await launch(project,flags));
        await page.waitForFunction(()=>document.querySelector('#engine-state')?.textContent.includes('0 local models'));
        await q('media-tab').click();
        await page.waitForFunction(()=>!document.querySelector('#tile-import-nexus')?.disabled);
    }
    async function field(selector,value) {
        const input=page.locator(selector);
        await input.fill(String(value));
        await input.press('Tab');
    }
    function box(id) {return page.locator('[data-item="'+id+'"]');}

    async function jumpAndPick() {
        await findState('support','nexus-10-1');
        await page.keyboard.press('Space');
        await page.waitForFunction(selector=>Number(document.querySelector(selector).dataset.playerY)<245,sceneSelector);
        await page.keyboard.down('ArrowRight');
        await xAt(238);
        await page.keyboard.up('ArrowRight');
        await findState('collected',1);
    }

    try {
        await open();
        await q('tile-import-nexus').setInputFiles(fixture);
        await status('Imported a playable Nexus level');
        const base=await api('open');
        const badge=async (name,color)=>{
            const png=await page.evaluate(color=>{
                const canvas=document.createElement('canvas');canvas.width=20;canvas.height=20;
                const g=canvas.getContext('2d');g.fillStyle=color;g.fillRect(1,1,18,18);
                return canvas.toDataURL('image/png').split(',')[1];
            },color);
            return api('add',{source:{filename:name,png}});
        };
        const blue=await badge('Blue overlay.png','#3982f3');
        const red=await badge('Coral overlay.png','#f16d58');
        const props=[{id:'blue-overlap',assetId:blue.id,kind:'sprite',layer:'foreground',x:52,y:36,size:48},
            {id:'coral-overlap',assetId:red.id,kind:'sprite',layer:'foreground',x:52,y:36,size:48}];
        const withBadges=await api('save',{layout:{...base.layout,title:'Twilight Relay · layers and score',objects:[...base.layout.objects,...props]}});
        await app.close();
        await open();
        await status('Reopened checked level');
        await q('tile-layers').locator('summary').click();
        await q('tile-new-plane').click();
        const plane=await q('tile-plane').inputValue();
        assert(plane);
        await field('[data-plane="name"]','Motion checks');
        await field('[data-plane="z"]',220);
        for (const item of props) await box(item.id).check();
        assert.equal(await q('tile-selection-count').textContent(),'2 selected');
        await q('tile-assign-plane').click();
        await q('tile-plane-filter').selectOption(plane);
        assert.equal(await q('tile-selection-list').getByRole('checkbox').count(),2);
        await q('tile-select-clear').click();
        await box('blue-overlap').check();
        await field('#tile-item-z',7);
        await q('tile-assign-z').click();
        assert.deepEqual(await pixel(70,55),[57,130,243,255]);
        await page.locator('[data-plane="visible"]').uncheck();
        assert.notDeepEqual(await pixel(70,55),[57,130,243,255]);
        await page.locator('[data-plane="visible"]').check();
        await q('tile-plane-filter').selectOption('');
        await q('tile-select-clear').click();
        await box('nexus-9-5').check();
        await field('#tile-points',75);
        await field('[data-scoring="goal"]',250);
        await field('[data-scoring="resetPenalty"]',30);
        await q('tile-save').click();
        await status('Saved level. Sprite sources');
        const saved=await api('open');
        assert.notEqual(saved.id,withBadges.id);
        assert.equal(saved.layout.planes[0].name,'Motion checks');
        assert.equal(saved.layout.planes[0].z,220);
        assert.equal(saved.layout.objects.find(o=>o.id==='blue-overlap').z,7);
        assert.equal(saved.layout.objects.find(o=>o.id==='nexus-9-5').points,75);
        assert.deepEqual(saved.layout.scoring,{pickup:100,goal:250,resetPenalty:30});
        await q('tile-layers').evaluate(panel=>panel.scrollIntoView({block:'start'}));
        await capture('layers-editor.png');

        const scene=await tool('tile.level',{start:0,limit:2});
        assert.equal(scene.id,saved.id);
        assert.equal(scene.settings.planes[0].z,220);
        assert.equal(scene.objects.length,2);
        const rawAsset=await tool('tile.image',{assetId:blue.id});
        assert.equal(rawAsset.file.sha256,saved.sprites[blue.id].sha256);
        assert.deepEqual((await tool('tile.pixels',{assetId:blue.id,x:0,y:0})).rgba,[57,130,243,255]);
        await q('tile-play').click();
        await q('tile-canvas').focus();
        const live=await tool('tile.ticks',{start:0,limit:2});
        assert(live.live);
        assert.equal(live.levelId,saved.id);
        assert.equal(live.rows[0][0],0);
        await jumpAndPick();
        await findState('score',75);
        await page.keyboard.down('ArrowLeft');
        await findState('resets',1);
        await page.keyboard.up('ArrowLeft');
        await findState('score',45);
        await jumpAndPick();
        await findState('score',45);
        await page.keyboard.down('ArrowRight');
        await findState('bounces',1);
        await findState('won',true);
        await page.keyboard.up('ArrowRight');
        await findState('score',295);
        const livePng=Buffer.from((await q('tile-canvas').evaluate(c=>c.toDataURL('image/png'))).split(',')[1],'base64');
        await q('tile-canvas').evaluate(element=>element.scrollIntoView({block:'center'}));
        await capture('scored-playtest.png');

        await page.waitForFunction(()=>document.querySelector('#tile-run-summary')?.dataset.runId);
        const runId=await q('tile-run-summary').getAttribute('data-run-id');
        const all=await api('export-run',{runId});
        assert.equal(all.reason,'won');
        assert.equal(all.score,295);
        assert.equal(all.rows.length,all.tickCount);
        assert(all.rows.every((row,i)=>row[0]===i));
        const scoreEvents=all.rows.flatMap(row=>row[17]).filter(event=>['pickup','reset','goal'].includes(event[0]));
        assert.deepEqual(scoreEvents,[['pickup','nexus-9-5',75],['reset','nexus-9-3',-30],
            ['pickup','nexus-9-5',0],['goal','',250]]);
        const modified=structuredClone(all.rows);
        modified.at(-1)[15]++;
        await assert.rejects(api('save-run',{levelId:saved.id,reason:'won',rows:modified,rendererSha256:all.rendererSha256}),/score differs/);
        const full=await tool('tile.image',{runId,tick:all.lastTick});

        assert.equal(full.file.sha256,sha(livePng));
        const frame=await fs.readFile(full.file.path);
        assert(frame.equals(livePng));
        await fs.writeFile(path.join(out,'verified-final-frame.png'),frame);
        const sample=await tool('tile.pixels',{runId,tick:all.lastTick,x:70,y:55});
        assert.deepEqual(sample.rgba,[57,130,243,255]);
        await assert.rejects(tool('tile.pixels',{runId,tick:0,x:719,y:335,width:2}),/outside the image/);
        await q('tile-stop').click();
        await q('tile-export-run').click();
        await page.waitForFunction(()=>document.querySelector('#tile-status')?.textContent.endsWith('.json'));
        const filename=(await q('tile-status').textContent()).slice('Saved '.length);
        const exported=await fs.readFile(filename);
        assert.equal(JSON.parse(exported).id,runId);
        await fs.writeFile(path.join(out,'playtest.json'),exported);

        for (const format of ['json','html']) {
            const data=await api('export',{format,id:saved.id});
            await fs.writeFile(path.join(out,'layered-relay.'+format),Buffer.from(data.base64,'base64'));
        }
        await app.close();
        await open();
        await status('Reopened checked level');
        const reopened=await tool('tile.ticks',{runId,start:all.lastTick,limit:1});
        assert.deepEqual(reopened.rows[0],all.rows.at(-1));
        const reconstructed=await tool('tile.image',{runId,tick:all.lastTick});
        assert.equal(reconstructed.file.sha256,full.file.sha256);
        await q('goal').fill('Show Nexus pixel (70, 55) at tick '+all.lastTick);
        await q('run').click();
        await page.getByText('RGBA 57, 130, 243, 255.',{exact:false}).waitFor();
        await capture('chat-pixel-evidence.png');

        const url=pathToFileURL(path.join(out,'layered-relay.html')).href;
        await app.evaluate(async ({BrowserWindow},url)=>{
            const win=new BrowserWindow({width:950,height:610,webPreferences:{
                partition:'layer-score-standalone',sandbox:true,nodeIntegration:false,contextIsolation:true}});
            win.webContents.session.webRequest.onBeforeRequest((r,done)=>done({cancel:!/^(file|data):/.test(r.url)}));
            await win.loadURL(url);
        },url);
        page=app.windows().find(win=>win.url()===url);
        sceneSelector='canvas';
        await page.locator(sceneSelector).focus();
        await page.waitForFunction(()=>{
            const [r,g,b,a]=document.querySelector('canvas').getContext('2d').getImageData(70,55,1,1).data;
            return r===57&&g===130&&b===243&&a===255;
        });
        await jumpAndPick();
        await page.keyboard.down('ArrowRight');
        await findState('won',true);
        await page.keyboard.up('ArrowRight');
        await findState('score',325);

        await fs.writeFile(path.join(out,'evidence.json'),JSON.stringify({
            levelId:saved.id,runId,score:all.score,scoreEvents,fields:all.fields,hz:all.hz,ticks:all.tickCount,
            rendererSha256:all.rendererSha256,imageSha256:full.file.sha256,rgba:sample.rgba,exactPixelAndPngAfterRestart:true,
            contiguous:true,corruptedScoreRefused:true,chatRead:true,standaloneScore:325,offline:true
        },null,2)+'\n');
        console.log('Verified layer order, multiple selections, the exact score ledger and agent pixel replay after restart.');
    } catch (error) {
        if (page) {
            console.error('Tile status',await q('tile-status').textContent().catch(()=>null),
                'Playtest status',await q('tile-run-summary').textContent().catch(()=>null));
            await capture('failure.png').catch(()=>{});
        }
        throw error;
    } finally {
        if (app) await app.close().catch(()=>{});
        const log=await fs.readFile(path.join(project,'test-desktop-process.log')).catch(()=>null);
        if (log) await fs.writeFile(path.join(out,'desktop-process.log'),log);
        const root=await fs.realpath(project);
        if (path.dirname(root)===await fs.realpath(os.tmpdir())&&/^Fraynix tile evidence [a-z0-9]+$/i.test(path.basename(root)))
            await fs.rm(root,{recursive:true,force:true,maxRetries:3}).catch(()=>{});
    }
}
main().catch(error=>{console.error(error);process.exitCode=1;});
