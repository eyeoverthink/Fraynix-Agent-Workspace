'use strict';

const assert = require('node:assert/strict');
const fs = require('node:fs/promises');
const path = require('node:path');
const {createHash} = require('node:crypto');

const earlier = ['record ID,reading,probe_c,inches','chill,4,25,1','warm,4,-30,1','shift,12,0,3','retired,12,0,5'];
const later = ['reading,inches,record ID,probe_c','4,1,chill,15','4,1,warm,-29','12,2.75,shift,0','4,1,added,8'];
const options = {timeout:60_000};
const digest = value => createHash('sha256').update(value).digest('hex');

async function withChangedFile(file, change, test) {
    const retained = await fs.readFile(file);
    try {
        await fs.writeFile(file,change(retained));
        await test();
    } finally {
        await fs.writeFile(file,retained);
    }
}

async function offline(app, file, output, artifact, sources) {
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
    try {
        assert(browser.url().startsWith('file:'));
        await browser.locator('.change-bar').first().waitFor(options);
        for (const [index,source] of sources.entries()) {
            const number = index+1;
            await browser.getByRole('button',{name:'Source '+number,exact:true}).click();
            assert.equal(await browser.locator('#source-id').innerText(),'Source '+number+' · Export '+source.id);
            assert.equal(await browser.locator('#visible-count').innerText(),index===0?'8 groups':'5 groups');
            assert.equal(await browser.locator('#visible-rows tr').count(),index===0?8:5);
            assert.equal(await browser.locator('.change-bar').count(),index===0?5:3);
            const totals = await browser.locator('#visible-totals').innerText();
            assert.match(totals,index===0?/Net \+9 °F[\s\S]*Net \+12.7 mm/:/Net -16.2 °F[\s\S]*Net -6.35 mm/);
            const mark = browser.getByRole('button',{name:index===0?/"fall" -9 °F/:/"chill" -18 °F/});
            await mark.focus();
            await browser.keyboard.press('Enter');
            assert.equal(await browser.locator('#change-selection-readings').innerText(),
                index===0?'Earlier #3: 50 | Later #2: 41':'Earlier #2: 77 | Later #2: 59');
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
        }
        await browser.screenshot({path:path.join(output,'offline-paired.png'),fullPage:true});
        await browser.getByRole('button',{name:'Source 1',exact:true}).click();
        assert.equal(await browser.locator('#visible-count').innerText(),'8 groups');
        assert.equal(await browser.locator('#change-selection-readings').innerText(),'');
        assert(await browser.evaluate(()=>document.documentElement.scrollWidth<=innerWidth));
    } finally {
        await app.evaluate(({BrowserWindow},id)=>BrowserWindow.fromId(id)?.destroy(),id);
    }
}

async function testPairedBriefing({project,output,original,route,call,api,send,reply,app,page,restart}) {
    const a = await route(earlier);
    const b = await route(later);
    const pair = {earlier:a.id,later:b.id};
    const saveComparison = () => call('routing-dashboard',{compare:true,verifyOnly:true,pair,keyColumn:'record ID',exportCsv:true});
    await send('Compare the last two verified routed CSVs by key "record ID".');
    await page().waitForFunction(later=>JSON.parse(localStorage.getItem('fraynix-last-key-comparison')||'{}').later===later,b.id,options);
    const second = await saveComparison();

    async function summarize(source) {
        await send('Summarize the last verified matched comparison CSV.');
        await page().waitForFunction(id=>localStorage.getItem('fraynix-last-comparison-summary')===id,source.id,options);
    }

    await summarize(second);
    const before = (await api('/api/state')).artifacts.length;
    await send('Review a two-source comparison briefing with totals, chart and rows.');
    const dialog = () => page().locator('#comparison-briefing');
    await dialog().waitFor({state:'visible',...options});
    const id = await page().evaluate(()=>localStorage.getItem('fraynix-comparison-briefing-review'));
    const review = await call('briefing-read',{id});
    assert.equal(review.approved,false);
    assert.deepEqual(review.sections,['totals','chart','rows']);
    assert.deepEqual(review.sources.map(source=>source.exportId),[original.id,second.id]);
    assert.deepEqual(review.sources.map(source=>source.sourceSha256),[original.sha256,second.sha256]);
    const text = await dialog().locator('#briefing-source').innerText();
    assert.match(text,new RegExp('Source 1 · Comparison '+original.id));
    assert.match(text,new RegExp('Source 2 · Comparison '+second.id));
    await page().screenshot({path:path.join(output,'paired-review.png')});
    await dialog().getByRole('button',{name:'Close'}).click();

    const newer = await saveComparison();
    await summarize(newer);
    assert.equal(newer.sha256,second.sha256);
    await assert.rejects(call('briefing-review',{exportIds:[second.id,newer.id],sections:['rows']}),/different CSV contents/);
    await restart();
    await send('Resume the comparison briefing.');
    await dialog().waitFor({state:'visible',...options});
    assert.deepEqual(await call('briefing-read',{id}),review);
    await assert.rejects(call('briefing-build',{id,reviewSha256:'0'.repeat(64)}),/review changed/);
    await assert.rejects(api('/api/requests/comparison-briefing-approve',{id,reviewSha256:review.reviewSha256}),/Source 2/);
    const exportPath = source => path.join(project,'exports',source.id+'.csv');
    const changeBytes = bytes => Buffer.concat([bytes,Buffer.from('\nchanged')]);
    await withChangedFile(exportPath(second),changeBytes,async()=>{
        await assert.rejects(call('briefing-build',{id,reviewSha256:review.reviewSha256}),/Source 2: .*disagree/i);
        assert.equal((await call('briefing-read',{id})).approved,false);
    });
    const countBeforeBuild = (await api('/api/state')).artifacts.length;
    assert.equal(countBeforeBuild,before+1);
    await dialog().getByRole('button',{name:'Approve and build'}).click();
    await reply('Comparison briefing verified against Java. 24 browser checks passed.');
    assert.equal((await api('/api/state')).artifacts.length,countBeforeBuild+1);
    await restart();
    await send('Recheck the comparison briefing.');
    await reply('Comparison briefing verified against Java. 24 fresh browser checks passed.');
    const proof = await call('briefing-recheck',{id});
    assert.equal(proof.rechecked,true);
    assert.equal(proof.browserChecks,24);
    assert.deepEqual(proof.comparisonSources,review.sources);
    const artifact = await api('/api/artifact/read',{id:proof.artifact.id});
    const evidence = JSON.parse(artifact.contents['verification.json']);
    assert.equal(evidence.presentation,'paired-comparison-briefing-v1');
    assert.deepEqual(evidence.sources,review.sources);
    const manifest = path.join(project,'exports','manifest.jsonl');
    const originalManifest = await fs.readFile(manifest,'utf8');
    for (const [index,source] of [original,second].entries()) {
        await withChangedFile(exportPath(source),changeBytes,async()=>{
            const expected = new RegExp('Source '+(index+1)+': .*disagree','i');
            await assert.rejects(call('briefing-recheck',{id}),expected);
            await assert.rejects(call('briefing-export',{id}),expected);
        });
    }
    const secondReceipt = path.join(project,'receipts',review.sources[1].summary.id+'-1.json');
    await withChangedFile(secondReceipt,bytes=>{
        const changed = JSON.parse(bytes);
        changed.checks[0].actual.text='Altered verification';
        return JSON.stringify(changed);
    },async()=>{
        await assert.rejects(call('briefing-export',{id}),/Source 2: .*test receipt changed/i);
    });
    assert.equal(await fs.readFile(manifest,'utf8'),originalManifest);
    await send('Export the comparison briefing.');
    await reply('Saved verified comparison briefing to');
    const exported = JSON.parse((await fs.readFile(manifest,'utf8')).trim().split('\n').at(-1));
    assert.equal(exported.kind,'paired-comparison-briefing');
    assert.equal(exported.sha256,proof.htmlSha256);
    assert.deepEqual(exported.source.comparisonSources,review.sources);
    assert.equal(digest(await fs.readFile(exported.path)),proof.htmlSha256);
    const standalone = path.join(project,'paired-offline');
    await fs.mkdir(standalone);
    const file = path.join(standalone,'paired-briefing.html');
    await fs.copyFile(exported.path,file);
    await offline(app(),file,output,artifact,[original,second]);
    for (const [name,content] of Object.entries(artifact.contents))
        await fs.writeFile(path.join(output,name==='index.html'?'paired-briefing.html':
            name==='verification.json'?'paired-verification.json':name),content);
    return {review:await call('briefing-read',{id}),proof,evidence,checks:[
        'Two distinct source bindings and their order survive a restart before approval and after building',
        'Changed CSV bytes for either source block recheck and export with the correct source label',
        'Source 2 retained summary proof is independently checked even with unchanged CSV bytes',
        'Single offline file switches sources and returns all six original source and proof downloads',
    ]};
}

module.exports = {testPairedBriefing};
