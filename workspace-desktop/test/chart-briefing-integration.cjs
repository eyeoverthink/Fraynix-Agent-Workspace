'use strict';

const assert = require('node:assert/strict');
const fs = require('node:fs/promises');
const path = require('node:path');
const {createHash} = require('node:crypto');

const brief = 'Could you brief me on that chart?';
const reopen = 'Open my last chart briefing.';
const scope = 'To explain why, attach separate evidence about the cause.';
const hash = data => createHash('sha256').update(data).digest('hex');
const expected = [
    {key:'rise <probe>',unit:'°F',delta:'18',earlier:{row:2,value:'32'},later:{row:3,value:'50'},
        sentence:'"rise <probe>" increased from 32 °F to 50 °F (+18 °F).'},
    {key:'fall',unit:'°F',delta:'-9',earlier:{row:3,value:'50'},later:{row:2,value:'41'},
        sentence:'"fall" decreased from 50 °F to 41 °F (-9 °F).'},
    {key:'length gain',unit:'mm',delta:'25.4',earlier:{row:5,value:'25.4'},later:{row:5,value:'50.8'},
        sentence:'"length gain" increased from 25.4 mm to 50.8 mm (+25.4 mm).'},
];

async function testChartBriefing({project,output,app,page,send,reply,restart,call,api,mixed,chartArtifact,empty,sources}) {
    const work = path.join(output,'chart-briefing');
    await fs.mkdir(work,{recursive:true});
    const count = async () => (await api('/api/state')).artifacts.length;
    const canvas = script => app().evaluate((_,code)=>global.workspaceTest.canvasRead(code),script);
    const chartData = JSON.parse(chartArtifact.contents['chart-data.json']);
    const show = id => page().evaluate(id => window.workspace.request({operation:'tool',tool:'canvas.open',args:{id}}),id);
    const lastPlan = request => api('/api/requests/chart-briefing-intent',{request});
    async function capture(name) {
        const bytes = Buffer.from(await app().evaluate(()=>global.workspaceTest.capture()),'base64');
        assert(bytes.length>10000);
        await fs.writeFile(path.join(work,name),bytes);
    }
    async function changed(file,mutate,test) {
        const previous = await fs.readFile(file);
        try { await fs.writeFile(file,mutate(previous)); await test(); }
        finally { await fs.writeFile(file,previous); }
    }

    const total = await count();
    await show(chartData.source.summary.id);
    await send(brief);
    await reply('Open the named chart you mean');
    await show(mixed.artifact.id);
    await send('What caused the changes in that chart?');
    await reply(scope);
    assert.equal(await count(),total);
    await assert.rejects(lastPlan(reopen),/Build a briefing from a named comparison chart first/);

    await send(brief);
    await reply('Opened the briefing for “Workshop audit”. 3 cited changes.');
    assert.equal(await canvas('document.getElementById("original-chart").textContent'),mixed.artifact.id);
    await capture('desktop-briefing.png');
    const completed = await call('chart-briefing',{request:reopen});
    assert.equal(completed.rechecked,true);
    assert.equal(completed.browserChecks,20);
    assert.equal(await count(),total+1);
    const savedPlan = (await lastPlan(reopen)).plan;
    assert.deepEqual(savedPlan.chartArtifact,mixed.artifact);
    const result = await api('/api/artifact/read',{id:completed.artifact.id});
    const evidence = JSON.parse(result.contents['claim-evidence.json']);
    assert.deepEqual(evidence.rows,expected);
    assert.deepEqual(evidence.binding.selection,chartData.selection);
    assert.equal(evidence.binding.chartDataSha256,hash(chartArtifact.contents['chart-data.json']));
    assert.deepEqual(evidence.units.map(unit=>[unit.unit,unit.rows,unit.netDelta]),[['°F',2,'9'],['mm',1,'25.4']]);
    assert.equal(evidence.evidenceScope,'descriptive-comparison');
    assert.equal(result.contents['named-chart.html'],chartArtifact.contents['index.html']);
    assert.equal(result.contents['matched-comparison.csv'],chartArtifact.contents['matched-comparison.csv']);
    assert.deepEqual(await canvas("[...document.querySelectorAll('.claim')].map(item=>item.textContent)"),expected.map(row=>row.sentence));

    const edited = structuredClone(result.test.checks);
    edited.find(step=>step.selector==='#citation-0 .earlier').actual.text='Row 999 · 32 °F';
    await assert.rejects(api('/api/requests/routing-dashboard-finish',{
        id:savedPlan.chartBriefingId,chartBriefingId:savedPlan.chartBriefingId,htmlSha256:completed.htmlSha256,
        ...sources,checks:edited,
    }),/Browser result differed/);
    await changed(path.join(project,'receipts',mixed.artifact.id+'-1.json'),bytes=>{
        const receipt=JSON.parse(bytes);
        receipt.checks[0].actual.text='Changed original chart heading';
        return JSON.stringify(receipt);
    },async()=>{
        await assert.rejects(call('chart-briefing',{request:reopen}),/test receipt changed/);
        assert.equal(await count(),total+1);
    });
    await changed(path.join(project,'exports',chartData.source.exportId+'.csv'),
        bytes=>Buffer.concat([bytes,Buffer.from('\nchanged')]),async()=>{
            await assert.rejects(call('chart-briefing',{request:reopen}),/original comparison.*disagree/i);
            assert.equal(await count(),total+1);
        });

    const newer = await call('comparison-chart',{request:'Chart losses from "Workshop audit" in Fahrenheit for key "fall".'});
    assert.notEqual(newer.artifact.id,mixed.artifact.id);
    await restart();
    const recovered = await call('chart-briefing',{request:reopen});
    assert.deepEqual(recovered.artifact,completed.artifact);
    assert.deepEqual(recovered.chartBriefing,completed.chartBriefing);
    assert.equal(await count(),total+2);
    assert.deepEqual((await api('/api/artifact/read',{id:completed.artifact.id})).test,result.test);

    await show(empty.artifact.id);
    const pending = await api('/api/requests/chart-briefing-pin',{request:brief,
        canvas:{id:empty.artifact.id,version:empty.artifact.version},...sources});
    assert.equal(pending.verifyOnly,false);
    assert.equal(await count(),total+2);
    await restart();
    const emptyBriefing = await call('chart-briefing',{request:reopen});
    assert.deepEqual(emptyBriefing.chartBriefing.binding.chart,empty.artifact);
    assert.equal(emptyBriefing.chartBriefing.claims,0);
    assert.equal(emptyBriefing.browserChecks,13);
    assert.equal(await canvas('document.getElementById("empty").textContent'),'This chart selection contains no matched rows.');

    for (const [name,contents] of Object.entries(result.contents)) await fs.writeFile(path.join(work,name),contents);
    const opened = app().waitForEvent('window');
    const downloaded = path.join(work,'tested-claim-evidence.json');
    const winId = await app().evaluate(({BrowserWindow},[file,download])=>{
        const win = new BrowserWindow({width:425,height:900,webPreferences:{
            sandbox:true,contextIsolation:true,nodeIntegration:false,partition:'checked-chart-briefing-offline'}});
        win.webContents.session.webRequest.onBeforeRequest({urls:['http://*/*','https://*/*']},(_,callback)=>callback({cancel:true}));
        win.webContents.session.on('will-download',(_,item)=>{
            item.setSavePath(download);
            item.once('done',(_,status)=>{win.claimDownload=status;});
        });
        win.loadFile(file);
        return win.id;
    },[path.join(work,'index.html'),downloaded]);
    const offline = await opened;
    try {
        await offline.waitForFunction(hash=>document.getElementById('briefing-audit')?.textContent===hash,evidence.browserBriefingSha256);
        assert(offline.url().startsWith('file:'));
        assert(await offline.evaluate(()=>document.documentElement.scrollWidth<=innerWidth));
        assert.deepEqual(await offline.locator('.claim').allInnerTexts(),expected.map(row=>row.sentence));
        await offline.locator('#citation-1 button').press('Enter');
        assert.equal(await offline.locator('#citation-1 .earlier').innerText(),'Row 3 · 50 °F');
        assert.equal(await offline.locator('#citation-1 .later').innerText(),'Row 2 · 41 °F');
        assert.equal(await offline.locator('probe').count(),0);
        await offline.screenshot({path:path.join(work,'offline-citation.png'),fullPage:true});
        await offline.getByRole('link',{name:'Claim evidence',exact:true}).click();
        let status;
        for(let attempt=0;attempt<80&&!status;attempt++){
            status=await app().evaluate(({BrowserWindow},id)=>BrowserWindow.fromId(id)?.claimDownload,winId);
            if(!status) await new Promise(resolve=>setTimeout(resolve,100));
        }
        assert.equal(status,'completed');
        assert.equal(await fs.readFile(downloaded,'utf8'),result.contents['claim-evidence.json']);
        await offline.getByRole('link',{name:'Open the original chart',exact:true}).click();
        await offline.waitForFunction(hash=>document.getElementById('chart-audit')?.textContent===hash,chartData.browserChartSha256);
    } finally {
        await app().evaluate(({BrowserWindow},id)=>BrowserWindow.fromId(id)?.destroy(),winId);
    }
    const proof = {passed:true,claims:evidence.rows,units:evidence.units,mixedChecks:completed.browserChecks,
        emptyChecks:emptyBriefing.browserChecks,sourceSha256:chartData.source.sourceSha256,
        browserBriefingSha256:evidence.browserBriefingSha256,checks:[
            'Natural-language follow-up uses the displayed chart even when a newer chart exists',
            'Every reported amount and citation matches the original rows, with totals kept by unit',
            'Requests for causes ask for independent evidence and create no briefing',
            'A newer chart cannot replace a saved briefing, and an unfinished empty briefing resumes after restart',
            'A falsified citation, a changed original chart receipt and a changed comparison export are refused',
            'Offline keyboard citations, exact downloads and the original chart link work',
        ]};
    await fs.writeFile(path.join(work,'result.json'),JSON.stringify(proof,null,2)+'\n');
    return proof;
}

module.exports = {testChartBriefing};
