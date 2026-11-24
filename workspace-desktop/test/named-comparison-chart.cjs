'use strict';

const assert = require('node:assert/strict');
const fs = require('node:fs/promises');
const path = require('node:path');
const {createHash} = require('node:crypto');
const {NativeExports} = require('../exports.cjs');

const query = 'Could you chart gains and losses from "Workshop audit" in both units for keys "fall", "rise <probe>" and "length gain"?';
const specific = 'Make a chart of gains from "Workshop audit" in millimetres for key "length gain".';
const resume = 'Open my last comparison chart.';
const hash = value => createHash('sha256').update(value).digest('hex');
const args = {timeout:60_000};

function marks(root = document) {
    return [...root.querySelectorAll('.mark')].map(mark => {
        const track = mark.querySelector('.track').getBoundingClientRect();
        const painted = mark.querySelector('.track span').getBoundingClientRect();
        const direction = mark.dataset.direction, centre = track.left + track.width / 2;
        const fraction = Number.parseFloat(mark.querySelector('.track span').style.getPropertyValue('--bar')) / 100;
        const actual = direction === 'loss' ? painted.right : painted.left;
        if (Math.abs(painted.width - track.width*fraction) > .8 || Math.abs(actual-centre) > .8 || !track.width)
            throw Error('Rendered bar geometry differs from its scale and direction.');
        return [mark.closest('.lane').dataset.unit,JSON.parse(mark.querySelector('.key').textContent),
            mark.querySelector('.delta').textContent,direction,fraction];
    });
}

const expected = [
    ['°F','rise <probe>','+18 °F','gain',.5],
    ['°F','fall','-9 °F','loss',.25],
    ['mm','length gain','+25.4 mm','gain',.5],
];

async function testNamedComparisonChart({project,output,original,call,api,app,page,send,reply,restart}) {
    const namedOutput = path.join(output,'named-chart');
    await fs.mkdir(namedOutput,{recursive:true});
    const exports = new NativeExports(project);
    const comparisonImport = await exports.comparisonCsv(original.id);
    const {earlier,later,keyColumn} = comparisonImport.receipt.source;
    const [baseline,file] = await Promise.all([exports.routedCsv(earlier.exportId),exports.routedCsv(later.exportId)]);
    const latest = await page().evaluate(()=>localStorage.getItem('fraynix-last-comparison-summary'));
    assert(latest && latest!==original.id);
    const distinct = await exports.comparisonCsv(latest);
    assert.notEqual(distinct.receipt.sha256,original.sha256);
    const summaryCount = async () => (await api('/api/state')).artifacts.length;

    async function capture(name) {
        const picture = Buffer.from(await app().evaluate(()=>global.workspaceTest.capture()),'base64');
        assert(picture.length>10000);
        await fs.writeFile(path.join(namedOutput,name),picture);
    }

    const fromBrowser = code => app().evaluate((_,value)=>global.workspaceTest.canvasRead(value),code);
    const readMarks = () => fromBrowser('(' + marks.toString() + ')(document)');
    const choices = await api('/api/requests/comparison-chart-intent',{request:query});
    assert.equal(choices.exportId,original.id);
    const beforePlan = await summaryCount();
    await assert.rejects(call('comparison-chart',{request:'Name the last verified comparison "workshop audit".',lastSummary:latest}),
        /already belongs/);
    await assert.rejects(call('comparison-chart',{request:'Chart gains from "Workshop audit" in millimetres for key "fall".'}),
        /No original row/);
    await assert.rejects(api('/api/requests/comparison-chart-intent',{request:'Chart gains from "Unknown comparison".'}),/No verified comparison/);
    const plan = await api('/api/requests/comparison-chart-pin',{request:query,file,baseline,keyColumn,comparisonImport});
    assert.deepEqual(plan.selection,{direction:'difference',unit:'all',keys:['fall','length gain','rise <probe>']});
    assert.equal(plan.verifyOnly,false);
    assert.equal(await summaryCount(),beforePlan);

    await restart();
    await send(resume);
    await reply('Opened the chart for “Workshop audit”. 3 matched rows');
    assert.equal(await fromBrowser('document.getElementById("chart-export").textContent'),original.id);
    assert.deepEqual(await readMarks(),expected);
    assert.equal(await summaryCount(),beforePlan+1);
    await capture('desktop-split-scales.png');

    const built = await call('comparison-chart',{request:resume});
    assert.equal(built.rechecked,true);
    assert.equal(built.comparisonExportId,original.id);
    assert.equal(built.browserChecks,20);
    const artifact = await api('/api/artifact/read',{id:built.artifact.id});
    const portfolio = JSON.parse(artifact.contents['chart-data.json']);
    assert.deepEqual(portfolio.selection,plan.selection);
    assert.equal(portfolio.source.sourceSha256,original.sha256);
    assert.equal(hash(artifact.contents['matched-comparison.csv']),original.sha256);
    assert.deepEqual(portfolio.groups.map(unit=>[unit.unit,unit.maximumAbsoluteDelta]),[['°F','18'],['mm','25.4']]);
    assert.equal(portfolio.browserChartSha256,await fromBrowser('document.getElementById("chart-audit").textContent'));
    await restart();
    const recovered = await call('comparison-chart',{request:resume});
    assert.deepEqual(recovered.artifact,built.artifact);
    assert.deepEqual(recovered.comparisonChart,built.comparisonChart);
    assert.deepEqual(await readMarks(),expected);
    assert.equal(await summaryCount(),beforePlan+1);
    assert.deepEqual((await api('/api/artifact/read',{id:built.artifact.id})).test,artifact.test);

    const falsified = structuredClone(artifact.test.checks);
    falsified.find(step=>step.selector==='#chart-audit').actual.text='0'.repeat(64);
    await assert.rejects(api('/api/requests/routing-dashboard-finish',{
        id:plan.comparisonChartId,comparisonChartId:plan.comparisonChartId,htmlSha256:built.htmlSha256,
        file,baseline,keyColumn,comparisonImport,checks:falsified,
    }),/Browser result differed/);

    const destination = path.join(project,'exports',original.id+'.csv');
    const originalBytes = await fs.readFile(destination);
    try {
        await fs.writeFile(destination,Buffer.concat([originalBytes,Buffer.from('\nchanged')]));
        await assert.rejects(call('comparison-chart',{request:resume}),/named comparison evidence.*disagree/i);
        assert.equal(await summaryCount(),beforePlan+1);
    } finally { await fs.writeFile(destination,originalBytes); }

    await send(specific);
    await reply('Opened the chart for “Workshop audit”. 1 matched row');
    assert.deepEqual(await readMarks(),[expected[2]]);
    await capture('desktop-length-only.png');
    const single = await call('comparison-chart',{request:resume});
    assert.equal(single.browserChecks,15);
    assert.notEqual(single.artifact.id,built.artifact.id);

    const empty = await call('comparison-chart',{
        request:'Chart losses from "Workshop audit" in millimetres for key "length gain".'});
    assert.equal(empty.comparisonChart.rows,0);
    assert.equal(empty.browserChecks,10);
    assert.equal(await fromBrowser('document.getElementById("detail-title").textContent'),'No matched rows satisfy this selection.');

    for (const [name,contents] of Object.entries(artifact.contents))
        await fs.writeFile(path.join(namedOutput,name),contents);
    const offlineOpened = app().waitForEvent('window');
    const download = path.join(namedOutput,'tested-chart-data.json');
    const windowId = await app().evaluate(({BrowserWindow},[fileName,download])=>{
        const window = new BrowserWindow({width:425,height:900,webPreferences:{
            partition:'named-comparison-chart-offline',sandbox:true,contextIsolation:true,nodeIntegration:false}});
        window.webContents.session.webRequest.onBeforeRequest({urls:['http://*/*','https://*/*']},(_,callback)=>callback({cancel:true}));
        window.webContents.session.on('will-download',(_,item)=>{
            item.setSavePath(download);
            item.once('done',(_,state)=>{window.chartDownload=state;});
        });
        window.loadFile(fileName);
        return window.id;
    },[path.join(namedOutput,'index.html'),download]);
    const offline = await offlineOpened;
    try {
        await offline.waitForFunction(hash=>document.getElementById('chart-audit')?.textContent===hash,
            portfolio.browserChartSha256,args);
        assert(offline.url().startsWith('file:'));
        assert(await offline.evaluate(()=>document.documentElement.scrollWidth<=innerWidth));
        assert.deepEqual(await offline.evaluate(marks),expected);
        const loss = offline.getByRole('button',{name:/"fall" -9 °F/});
        await loss.focus();
        await offline.keyboard.press('Enter');
        assert.equal(await loss.getAttribute('aria-pressed'),'true');
        assert.equal(await offline.locator('#detail-earlier').innerText(),'#3: 50');
        assert.equal(await offline.locator('#detail-later').innerText(),'#2: 41');
        assert.equal(await offline.locator('probe').count(),0);
        await offline.screenshot({path:path.join(namedOutput,'offline-readings.png'),fullPage:true});
        assert.equal(await offline.locator('a[download]').count(),3);
        await offline.getByRole('link',{name:'Selected rows and scales'}).click();
        let status;
        for(let attempt=0;attempt<80&&!status;attempt++){
            status=await app().evaluate(({BrowserWindow},id)=>BrowserWindow.fromId(id)?.chartDownload,windowId);
            if(!status)await new Promise(resolve=>setTimeout(resolve,100));
        }
        assert.equal(status,'completed');
        assert.equal(await fs.readFile(download,'utf8'),artifact.contents['chart-data.json']);
    } finally {
        await app().evaluate(({BrowserWindow},id)=>BrowserWindow.fromId(id)?.destroy(),windowId);
    }

    const evidence = {
        passed:true,sourceSha256:original.sha256,browserChartSha256:portfolio.browserChartSha256,
        mixed:built.browserChecks,single:single.browserChecks,empty:empty.browserChecks,
        units:['°F','mm'],rows:expected,checks:[
            'The named source and exact query survived a restart before rendering and after saving',
            'The newest comparison stayed distinct from the named source',
            'Name collisions, unknown keys, changed source bytes and changed browser proof were refused',
            'Each unit painted its own direction and scale and the offline keyboard inspection matched both original row values',
        ],
    };
    await fs.writeFile(path.join(namedOutput,'result.json'),JSON.stringify(evidence,null,2)+'\n');
    return evidence;
}

module.exports = {testNamedComparisonChart};
