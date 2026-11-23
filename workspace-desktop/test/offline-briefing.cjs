'use strict';

const assert = require('node:assert/strict');
const fs = require('node:fs/promises');
const path = require('node:path');

async function withOfflineBriefing(app, file, output, artifact, check) {
    const paths = Object.fromEntries(Object.keys(artifact.contents).filter(name=>name.startsWith('source-'))
        .map(name=>[name,path.join(output,name)]));
    const opened = app.waitForEvent('window');
    const id = await app.evaluate(({BrowserWindow},[file,paths])=>{
        const win = new BrowserWindow({width:730,height:940,webPreferences:{
            partition:'paired-briefing-offline',sandbox:true,contextIsolation:true,nodeIntegration:false}});
        const session = win.webContents.session;
        session.webRequest.onBeforeRequest({urls:['http://*/*','https://*/*']},(_,callback)=>callback({cancel:true}));
        win.downloads = {};
        session.on('will-download',(_,item)=>{
            const name = item.getFilename();
            if (!Object.hasOwn(paths,name)) { item.cancel(); return; }
            item.setSavePath(paths[name]);
            item.once('done',(_,state)=>{win.downloads[name]=state;});
        });
        win.loadFile(file);
        return win.id;
    },[file,paths]);
    const browser = await opened;
    const downloads = async number => {
        const prefix = 'source-'+number+'-';
        for (const name of ['comparison.csv','receipt.json','summary.json']) {
            const filename = prefix+name;
            await browser.locator('a[download="'+filename+'"]').evaluate(link=>link.click());
            let state;
            for (let attempt=0;attempt<80;attempt++) {
                state = await app.evaluate(({BrowserWindow},[id,name])=>BrowserWindow.fromId(id)?.downloads[name],[id,filename]);
                if (state) break;
                await new Promise(resolve=>setTimeout(resolve,100));
            }
            assert.equal(state,'completed');
            assert.equal(await fs.readFile(path.join(output,filename),'utf8'),artifact.contents[filename]);
        }
    };
    try {
        assert(browser.url().startsWith('file:'));
        await browser.locator('#source-id').filter({hasText:'Source 1'}).waitFor();
        await check(browser,downloads);
    } finally {
        await app.evaluate(({BrowserWindow},id)=>BrowserWindow.fromId(id)?.destroy(),id);
    }
}

module.exports = {withOfflineBriefing};
