'use strict';

const assert = require('node:assert/strict');
const fs = require('node:fs/promises');
const path = require('node:path');
const {createHash} = require('node:crypto');
const {NativeExports} = require('../exports.cjs');
const {withOfflineBriefing} = require('./offline-briefing.cjs');

const options = {timeout:60_000};
const choices = [['rows'],['totals','chart']];
const hash = data => createHash('sha256').update(data).digest('hex');
const summaries = 'fraynix-last-comparison-summary';

async function testSelectedBriefing({project,output,original,second,route,call,api,send,reply,app,page,restart}) {
    const before = await route(['record ID,reading,probe_c,inches','probe,4,1,1','rail,12,0,4']);
    const after = await route(['reading,inches,record ID,probe_c','4,1,probe,4','12,3,rail,0']);
    await send('Compare the last two verified routed CSVs by key "record ID".');
    await page().waitForFunction(id=>JSON.parse(localStorage.getItem('fraynix-last-key-comparison')||'{}').later===id,after.id,options);
    const pair = {earlier:before.id,later:after.id};
    const exportLatest = () => call('routing-dashboard',{compare:true,verifyOnly:true,pair,keyColumn:'record ID',exportCsv:true});
    const newest = await exportLatest();
    await send('Summarize the last verified matched comparison CSV.');
    await page().waitForFunction(([key,id])=>localStorage.getItem(key)===id,[summaries,newest.id],options);
    const unsummarized = await exportLatest();
    assert.notEqual(newest.sha256,original.sha256);
    assert.notEqual(newest.sha256,second.sha256);
    // Fill the first page with exact comparison copies that have never been summarized.
    const fixtureExports = new NativeExports(project);
    const copy = await fs.readFile(unsummarized.path);
    for (let index=1;index<24;index++)
        await fixtureExports.save(unsummarized.kind,'csv',copy,unsummarized.source,unsummarized.sha256);

    await restart();
    await send('Review a two-source comparison briefing.');
    const picker = page().locator('#briefing-picker');
    await picker.waitFor({state:'visible',...options});
    await picker.getByRole('status').filter({hasText:'No verified summaries on this page'}).waitFor(options);
    assert.equal(await picker.locator('#briefing-pick-1 option').count(),1);
    await picker.getByRole('button',{name:'Load older comparisons'}).click();
    const first = picker.locator('#briefing-pick-1');
    const last = picker.locator('#briefing-pick-2');
    await first.locator('option[value="'+original.id+'"]').waitFor({state:'attached',...options});
    const catalog = await first.locator('option').evaluateAll(options=>options.map(option=>option.value).filter(Boolean));
    assert.equal(catalog[0],newest.id);
    assert(catalog.indexOf(second.id)<catalog.indexOf(original.id));
    assert(!catalog.includes(unsummarized.id));
    await first.selectOption(second.id);
    await last.selectOption(original.id);
    const source1 = picker.getByRole('group',{name:'Source 1'});
    const source2 = picker.getByRole('group',{name:'Source 2'});
    await source1.getByRole('checkbox',{name:'Totals by unit'}).uncheck();
    await source1.getByRole('checkbox',{name:'Interactive change chart'}).uncheck();
    await source2.getByRole('checkbox',{name:'Source rows'}).uncheck();
    await page().screenshot({path:path.join(output,'selected-picker.png')});
    await picker.getByRole('button',{name:'Review selections'}).click();

    const dialog = () => page().locator('#comparison-briefing');
    await dialog().waitFor({state:'visible',...options});
    const id = await page().evaluate(()=>localStorage.getItem('fraynix-comparison-briefing-review'));
    const review = await call('briefing-read',{id});
    assert.equal(review.approved,false);
    assert.deepEqual(review.sources.map(source=>source.exportId),[second.id,original.id]);
    assert.deepEqual(review.sources.map(source=>source.sections),choices);
    assert.deepEqual(await dialog().locator('li').allInnerTexts(),
        ['Source 1 · Source rows','Source 2 · Totals by unit, Interactive change chart']);
    await page().screenshot({path:path.join(output,'selected-review.png')});

    await restart();
    await send('Resume the comparison briefing.');
    await dialog().waitFor({state:'visible',...options});
    assert.deepEqual(await call('briefing-read',{id}),review);
    await dialog().getByRole('button',{name:'Approve and build'}).click();
    await reply('Comparison briefing verified against Java. 18 browser checks passed.');
    const firstProof = await call('briefing-recheck',{id});
    assert.deepEqual(firstProof.comparisonSources,review.sources);
    await restart();
    await send('Recheck the comparison briefing.');
    await reply('Comparison briefing verified against Java. 18 fresh browser checks passed.');
    const proof = await call('briefing-recheck',{id});
    assert.deepEqual(proof.artifact,firstProof.artifact);
    assert.equal(proof.htmlSha256,firstProof.htmlSha256);
    const artifact = await api('/api/artifact/read',{id:proof.artifact.id});
    const evidence = JSON.parse(artifact.contents['verification.json']);
    assert.equal(evidence.presentation,'selected-paired-briefing-v1');
    assert.deepEqual(evidence.sources,review.sources);

    const manifest = path.join(project,'exports','manifest.jsonl');
    await send('Export the comparison briefing.');
    await reply('Saved verified two-source comparison briefing to');
    let receipt;
    for (let attempt=0;attempt<80;attempt++) {
        const entries = (await fs.readFile(manifest,'utf8')).trim().split('\n').map(line=>JSON.parse(line));
        receipt = entries.findLast(entry=>entry.kind==='paired-comparison-briefing'&&entry.source.briefingId===id);
        if (receipt) break;
        await new Promise(resolve=>setTimeout(resolve,100));
    }
    assert(receipt);
    assert.deepEqual(receipt.source.comparisonSources,review.sources);
    const bytes = await fs.readFile(receipt.path);
    assert.equal(hash(bytes),proof.htmlSha256);
    const evidenceOutput = path.join(output,'selected');
    await fs.mkdir(evidenceOutput,{recursive:true});
    const standalone = path.join(project,'selected-offline');
    await fs.mkdir(standalone);
    const file = path.join(standalone,'selected-briefing.html');
    await fs.writeFile(file,bytes);

    const selection = ['#visible-totals','#change-map','.table-wrap'];
    await withOfflineBriefing(app(),file,evidenceOutput,artifact,async(browser,downloads)=>{
        async function checkSlot(index,source,visible) {
            const tab = browser.getByRole('button',{name:'Source '+index,exact:true});
            await tab.click();
            assert.equal(await browser.locator('#source-id').innerText(),'Source '+index+' · Export '+source.id);
            assert.deepEqual(await Promise.all(selection.map(selector=>browser.locator(selector).isVisible())),visible);
            const prefix = 'source-'+index+'-';
            assert.equal(hash(artifact.contents[prefix+'comparison.csv']),source.sha256);
            assert.equal(JSON.parse(artifact.contents[prefix+'receipt.json']).id,source.id);
            assert.equal(JSON.parse(artifact.contents[prefix+'summary.json']).import.id,source.id);
            await downloads(index);
        }
        await checkSlot(1,second,[false,false,true]);
        assert.equal(await browser.locator('#visible-rows tr').count(),5);
        await checkSlot(2,original,[true,true,false]);
        assert.match(await browser.locator('#visible-totals').innerText(),/Net \+9 °F[\s\S]*Net \+12.7 mm/);
        assert.equal(await browser.locator('.change-bar').count(),5);
        await browser.screenshot({path:path.join(output,'selected-offline.png'),fullPage:true});
        await browser.getByRole('button',{name:'Source 1',exact:true}).click();
        assert.deepEqual(await Promise.all(selection.map(selector=>browser.locator(selector).isVisible())),[false,false,true]);
    });
    for (const [name,content] of Object.entries(artifact.contents))
        await fs.writeFile(path.join(evidenceOutput,name==='index.html'?'selected-briefing.html':name),content);
    return {review:await call('briefing-read',{id}),proof,evidence,newestExportId:newest.id,unsummarizedExportId:unsummarized.id,
        checks:[
            'The picker pages past 24 newer unsummarized exports and finds the earlier saved summaries after restart',
            'The review, completed briefing and export preserve the chosen reverse order and sections on both restarts',
            'Source 1 shows rows and Source 2 shows totals and chart, with all six matching evidence files offline',
        ]};
}

module.exports = {testSelectedBriefing};
