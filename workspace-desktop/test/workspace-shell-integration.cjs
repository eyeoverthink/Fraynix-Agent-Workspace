'use strict';
const assert=require('node:assert/strict');
const fs=require('node:fs/promises');
const os=require('node:os');
const path=require('node:path');
const {launch}=require('./calibration-integration.cjs');
const {fixture,environmentGraphics}=require('./environment-integration.cjs');

async function main(){
    process.env.FRAYNIX_UNIFIED_TEST='1';
    process.env.FRAYNIX_WORKSPACE_NO_MODELS='1';
    const out=path.resolve(process.argv[2]||'build/workspace-shell-test');
    await fs.mkdir(out,{recursive:true});
    const project=await fs.mkdtemp(path.join(os.tmpdir(),'Fraynix workspace UI '));
    let app,page;
    const statuses=[];
    const open=async()=>{
        ({app,page}=await launch(project,environmentGraphics()));
        page.on('pageerror',error=>statuses.push(error.message));
        await page.locator('#workspace-home h1').waitFor();
    };
    const ask=async message=>{
        await page.locator('#goal').fill(message);
        await page.locator('#run').click();
    };
    const reply=text=>page.locator('#messages .message.assistant').filter({hasText:text}).last().waitFor();
    const capture=async name=>{
        await page.evaluate(()=>new Promise(resolve=>requestAnimationFrame(()=>requestAnimationFrame(resolve))));
        await fs.writeFile(path.join(out,name),Buffer.from(await app.evaluate(()=>global.workspaceTest.capture()),'base64'));
    };
    const native=()=>app.evaluate(()=>global.workspaceTest.surfaceLayout());
    const waitNative=async (browser,canvas)=>{
        for(let i=0;i<40;i++){
            const surfaces=await native();
            if(surfaces.browser.visible===browser&&surfaces.canvas.visible===canvas)return surfaces;
            await new Promise(resolve=>setTimeout(resolve,50));
        }
        assert.fail('Native surface visibility '+JSON.stringify(await native()));
    };

    try{
        await open();
        await page.waitForFunction(()=>document.querySelector('#engine-state').textContent.includes('0 local models'));
        assert(!await page.locator('body > header > .header-actions').isVisible());
        assert(!await page.locator('.left-column').isVisible());
        assert(!await page.locator('#workspace-manual').evaluate(details=>details.open));
        assert(await page.locator('#goal').isVisible());
        await waitNative(false,false);
        await capture('workspace-home.png');

        await ask('Inspect the current web page.');
        await reply('A real page in');
        await page.locator('#browser-surface').waitFor();
        const layout=await waitNative(true,false);
        const css=await page.locator('#browser-surface').boundingBox();
        for(const field of ['x','y','width','height'])
            assert(Math.abs(css[field]-layout.browser.bounds[field])<=2,field);
        assert(await page.locator('#goal').isVisible());
        await page.locator('#goal').focus();
        await page.keyboard.press('Control+k');
        await page.locator('#workspace-tools').waitFor();
        await waitNative(false,false);
        await page.locator('#workspace-tool-search').fill('floor plan');
        await page.locator('.workspace-tool[data-capability=floor-plan]').waitFor();
        await page.locator('#workspace-tool-search').press('ArrowDown');
        assert.equal(await page.evaluate(()=>document.activeElement.dataset.capability),'floor-plan');
        await capture('workspace-tools.png');
        await page.locator('#workspace-tool-search').fill('browser.observe');
        await page.locator('.workspace-tool[data-capability=registered]').filter({hasText:'browser.observe'}).first().waitFor();
        await page.keyboard.press('Escape');
        await page.locator('#workspace-tools').waitFor({state:'hidden'});
        await waitNative(true,false);
        assert.equal(await page.evaluate(()=>document.activeElement.id),'goal');

        const floor='Make a floor plan at 1.2 m.';
        await ask(floor);
        await page.locator('#workspace-next').waitFor();
        assert(await page.locator('#workspace-next-continue').isDisabled());
        await page.locator('#environment-plans').waitFor();
        await waitNative(false,false);
        const {document,encode}=await fixture();
        const model=path.join(out,'two-room-pavilion.glb');
        await fs.writeFile(model,encode(document));
        await page.locator('#environment-file').setInputFiles(model);
        await page.locator('#workspace-next-continue:not([disabled])').waitFor();
        await page.locator('#workspace-next-continue').click();
        await reply('I made a horizontal floor plan at 1.2 m.');
        await page.locator('#plan-diagram').evaluate(img=>img.decode());
        const diagram=await page.locator('#plan-diagram').boundingBox();
        const stage=await page.locator('#environment-view').boundingBox();
        assert(diagram.y>=stage.y && diagram.y+diagram.height<=stage.y+stage.height,
            'The complete floor plan is visible when chat presents it');
        assert.equal(await page.locator('#messages .user').filter({hasText:floor}).count(),1);
        assert(await page.locator('#goal').isVisible());
        await capture('workspace-floor-plan.png');

        await ask('Open source editor.');
        await page.locator('#source').waitFor();
        const original=await page.locator('#source').inputValue();
        await page.locator('#source').fill(original+'\n<!-- draft -->');
        assert(await page.locator('#run').isDisabled());
        await page.locator('#workspace-browser-open').click();
        assert.equal(await page.locator('#source').inputValue(),original+'\n<!-- draft -->');
        assert(await page.locator('#source').isVisible());
        await page.locator('#revert-source').click();
        await app.close();

        await open();
        await ask('Open my files.');
        await page.locator('.library').waitFor();
        assert(await page.locator('#goal').isVisible());
        await ask('Could you measure the height layers?');
        await reply('I measured 3 height bands.');
        assert(await page.locator('#layers-diagram').isVisible());
        assert.equal(await page.locator('#messages .user').filter({hasText:floor}).count(),1);
        await ask('Open browser.');
        await waitNative(true,false);

        await app.evaluate(({BrowserWindow})=>BrowserWindow.getAllWindows().find(window=>window.isVisible()).setSize(1100,850));
        await page.waitForFunction(()=>innerWidth<=1120);
        await waitNative(true,false);
        assert(await page.locator('#messages').isVisible());
        assert(await page.locator('#goal').isVisible());
        await app.evaluate(({BrowserWindow})=>BrowserWindow.getAllWindows().find(window=>window.isVisible()).setSize(790,760));
        await page.waitForFunction(()=>innerWidth<=820);
        assert(await page.locator('#goal').isVisible());
        assert(await page.locator('#browser-surface').isVisible());
        await waitNative(true,false);
        await page.locator('#workspace-focus-chat').click();
        await waitNative(false,false);
        assert(await page.locator('#messages').isVisible());
        assert(await page.locator('#goal').isVisible());
        assert(await page.evaluate(()=>document.documentElement.scrollWidth<=innerWidth+1));
        await capture('workspace-compact.png');
        await page.locator('#workspace-focus-stage').click();
        await waitNative(true,false);
        assert(await page.locator('#goal').isVisible());
        const ramp='Run the local ramp experiment and pause after reading.';
        await ask(ramp);
        await page.locator('#ramp-input-dialog').waitFor();
        assert(await page.locator('#workspace-next-continue').isDisabled());
        await page.locator('#ramp-input-save').click();
        await page.locator('#workspace-next-continue:not([disabled])').waitFor();
        await page.locator('#workspace-next-continue').click();
        await page.locator('#workspace-focus-chat').click();
        await reply('Input verified. Resume to start the world experiment.');
        const state=await app.evaluate(()=>global.workspaceTest.api('/api/agency043/state',{}));
        const task=state.tasks.find(item=>item.goal===ramp);
        assert.equal(task?.status,'interrupted',JSON.stringify(task));
        assert.equal(await page.locator('#messages .user').filter({hasText:ramp}).count(),1);
        assert.deepEqual(statuses,[]);
        const report={noModels:true,startingViews:['home','browser','project','source'],keyboardPalette:true,liveRegistrySearch:true,rampRequestResumed:true,
            nativeOverlayHiddenWhileModal:true,continuedPortablyAfterSource:true,retainedTranscript:true,
            preservedDirtySource:true,compactWithComposer:true,transcriptRequestsForFloor:1};
        await fs.writeFile(path.join(out,'workspace-shell-evidence.json'),JSON.stringify(report,null,2)+'\n');
        console.log('Unified workspace verified offline, including in-place continuation, restart and native bounds.');
    }catch(error){
        for(const selector of ['#notice','#engine-state','#environment-status','#workspace-context'])
            console.error(selector,await page?.locator(selector).textContent().catch(()=>null));
        console.error('Conversation',await page?.locator('#messages').innerText().catch(()=>null));
        console.error('Page errors',statuses);
        throw error;
    }finally{
        if(app)await app.close().catch(()=>{});
        const resolved=await fs.realpath(project);
        if(path.dirname(resolved)===await fs.realpath(os.tmpdir())&&/^Fraynix workspace UI [a-z0-9]+$/i.test(path.basename(resolved)))
            await fs.rm(resolved,{recursive:true,force:true,maxRetries:3}).catch(()=>{});
    }
}
main().catch(error=>{console.error(error);process.exitCode=1;});
