'use strict';

const assert = require('node:assert/strict');
const fs = require('node:fs/promises');
const path = require('node:path');
const {createHash} = require('node:crypto');

const ask = 'Could you audit a proposed explanation for that briefing?';
const resume = 'Resume my last explanation audit.';
const csvHeader = 'run_id,observed_at,source_ref,unit,treatment_before,treatment_after,control_before,control_after,error_bound';
const rowA = 'trial-A,2026-11-22T14:01:00Z,fixture log A,°F,55,44,64,62,0.25';
const rowB = 'trial-B,2026-11-22T14:03:00-08:00,fixture log B,°F,40,31,50,50,0.5';
const contrary = 'trial-C,2026-11-22T14:05:00Z,fixture log C,°F,40,44,50,50,0.25';
const uncertain = 'trial-D,2026-11-22T14:06:00Z,fixture log D,°F,40,31,50,50,2';
const hypothesis = 'The airflow change produced the reported temperature drop.';
const assumptions = 'Equal starting conditions; paired control drift is additive.';
const hash = text => createHash('sha256').update(text).digest('hex');
const csv = (...rows) => [csvHeader,...rows].join('\n') + '\n';
const file = data => ({name:'trial-observations.csv',base64:Buffer.from(data).toString('base64')});

async function testExplanationAudit({project,output,app,page,send,reply,restart,call,api,briefing,briefingContents,sources}) {
    const work = path.join(output,'explanation-audit');
    await fs.mkdir(work,{recursive:true});
    const artifacts = async () => (await api('/api/state')).artifacts.length;
    const show = id => page().evaluate(id => window.workspace.request({operation:'tool',tool:'canvas.open',args:{id}}),id);
    const canvas = code => app().evaluate((_,source)=>global.workspaceTest.canvasRead(source),code);
    const intent = async () => (await api('/api/requests/explanation-audit-intent',{request:resume})).plan;
    async function changed(name,mutate,test) {
        const previous = await fs.readFile(name);
        try { await fs.writeFile(name,mutate(previous)); await test(); }
        finally { await fs.writeFile(name,previous); }
    }
    async function capture(name) {
        const png = Buffer.from(await app().evaluate(()=>global.workspaceTest.capture()),'base64');
        assert(png.length > 10000);
        await fs.writeFile(path.join(work,name),png);
    }

    const start = await artifacts();
    await assert.rejects(call('explanation-audit-chat',{request:ask}),/Choose a briefing with at least one nonzero change/);
    await show(briefing.id);
    await send(ask);
    await page().locator('#audit-claim').waitFor({state:'visible'});
    await page().locator('#audit-claim').selectOption('1');
    await page().locator('#audit-hypothesis').fill(hypothesis);
    await page().locator('#audit-assumptions').fill(assumptions);
    await page().locator('#audit-margin').fill('1');
    await page().locator('#audit-submit').click();
    await page().getByText('Draft saved. Attach separate trial observations to run the audit.').waitFor();
    await capture('desktop-draft.png');
    const draft = await intent();
    assert.equal(draft.status,'needs-observations');
    assert.deepEqual(draft.briefingArtifact,briefing);
    assert.equal(await artifacts(),start);
    const input = {...draft.proposal,id:draft.id};
    await assert.rejects(call('explanation-audit-submit',{...input,file:file(briefingContents['matched-comparison.csv'])}),/Attach separate trial observations/);
    await assert.rejects(call('explanation-audit-submit',{...input,file:file(csv(rowA,rowB.replace('log B','log A')))}),/own run ID and source reference/);
    await assert.rejects(call('explanation-audit-submit',{...input,file:file(csv(rowA.replace('°F','mm')))}),/must use °F/);
    await assert.rejects(call('explanation-audit-submit',{...input,file:file(csv(rowA.replace('0.25','-0.25')))}),/zero or positive/);
    await assert.rejects(call('explanation-audit-submit',{...input,file:file(csv(rowA.replace('14:01:00Z','14:01:00')))}),/timestamp with a time zone/);
    assert.equal(await artifacts(),start);

    await restart();
    await send(resume);
    await page().locator('#audit-hypothesis').waitFor({state:'visible'});
    assert.equal(await page().locator('#audit-hypothesis').inputValue(),hypothesis);
    assert.equal(await page().locator('#audit-margin').inputValue(),'1');
    assert.equal(await page().locator('#audit-claim').inputValue(),'1');
    const readings = csv(rowA,rowB);
    await page().locator('#audit-file').setInputFiles({name:'trial-observations.csv',mimeType:'text/csv',buffer:Buffer.from(readings)});
    await page().getByRole('button',{name:'Run audit',exact:true}).click();
    const problem = page().locator('#explanation-audit-dialog[open] #audit-status:not(:empty)');
    const outcome = await Promise.race([
        reply('Trials support the prediction. 14 browser checks passed.').then(()=>null),
        problem.waitFor({state:'visible',timeout:60000}).then(()=>problem.innerText())
    ]);
    assert.equal(outcome,null,'The audit reopened with a verification error');
    const saved = await call('explanation-audit-chat',{request:resume});
    assert.equal(saved.rechecked,true);
    assert.equal(saved.explanationAudit.status,'supports-prediction');
    assert.equal(await artifacts(),start+1);
    const artifact = await api('/api/artifact/read',{id:saved.artifact.id});
    const evidence = JSON.parse(artifact.contents['audit-evidence.json']);
    assert.deepEqual(evidence.originalBriefing,briefing);
    assert.deepEqual(evidence.claim,JSON.parse(briefingContents['claim-evidence.json']).rows[1]);
    assert.equal(evidence.observationsSha256,hash(readings));
    assert.equal(artifact.contents['observations.csv'],readings);
    assert.equal(artifact.contents['claim-evidence.json'],briefingContents['claim-evidence.json']);
    assert.deepEqual(evidence.prediction,{low:'-10',high:'-8'});
    assert.deepEqual(evidence.trials.map(t=>[t.observedAt,t.treatmentChange,t.controlChange,t.effect,t.status,t.interval]),[
        ['2026-11-22T14:01:00Z','-11','-2','-9','consistent',{low:'-9.25',high:'-8.75'}],
        ['2026-11-22T22:03:00Z','-9','0','-9','consistent',{low:'-9.5',high:'-8.5'}]
    ]);
    assert.equal(await canvas('document.querySelector("#audit-outcome").textContent'),'Trials support the prediction');
    await capture('desktop-audit.png');

    const tampered = structuredClone(artifact.test.checks);
    tampered.find(step=>step.selector==='#audit-browser-digest').actual.text='forged browser plot';
    await assert.rejects(api('/api/requests/routing-dashboard-finish',{
        id:draft.id,explanationAuditId:draft.id,htmlSha256:saved.htmlSha256,...sources,checks:tampered
    }),/Browser result differed/);
    await changed(path.join(project,'receipts',briefing.id+'-1.json'),bytes=>{
        const receipt = JSON.parse(bytes);
        receipt.checks[0].actual.text = 'Edited original briefing heading';
        return JSON.stringify(receipt);
    },async()=>assert.rejects(call('explanation-audit-chat',{request:resume}),/test receipt changed/));
    await changed(path.join(project,'exports',draft.exportId+'.csv'),
        bytes=>Buffer.concat([bytes,Buffer.from('\nchanged')]),
        async()=>assert.rejects(call('explanation-audit-chat',{request:resume}),/original briefing evidence.*disagree/i));
    await changed(path.join(project,'request-workbench','explanation-audits',draft.id+'.json'),bytes=>{
        const record = JSON.parse(bytes);
        record.data.attachment.base64 = file(csv(contrary)).base64;
        return JSON.stringify(record);
    },async()=>assert.rejects(call('explanation-audit-chat',{request:resume}),/checkpoint|hash|changed|integrity/i));

    await restart();
    const recovered = await call('explanation-audit-chat',{request:resume});
    assert.deepEqual(recovered.artifact,saved.artifact);
    assert.deepEqual(recovered.explanationAudit,saved.explanationAudit);
    assert.equal(recovered.rechecked,true);
    assert.equal(await artifacts(),start+1);

    const otherOutcomes = [];
    for (const [trialFile,expected] of [
        [csv(contrary),'conflicting'],
        [csv(rowA,contrary),'mixed'],
        [csv(rowA),'inconclusive'],
        [csv(rowA,rowB,uncertain),'inconclusive']
    ]) {
        await show(briefing.id);
        const next = (await call('explanation-audit-chat',{request:ask})).plan;
        const result = await call('explanation-audit-submit',{
            ...input,id:next.id,file:file(trialFile)
        });
        assert.equal(result.explanationAudit.status,expected);
        assert.equal(result.browserChecks,14);
        otherOutcomes.push(result.explanationAudit);
        if (expected==='mixed') await capture('desktop-mixed.png');
    }
    assert.equal(await artifacts(),start+5);
    // A deliberately stale displayed artifact can never fall back to the newer/latest briefing.
    await assert.rejects(api('/api/requests/explanation-audit-intent',{
        request:ask,canvas:{id:briefing.id,version:briefing.version+1}
    }),/Open the verified chart briefing you mean/);

    for (const [name,body] of Object.entries(artifact.contents)) await fs.writeFile(path.join(work,name),body);
    const opened = app().waitForEvent('window');
    const downloaded = path.join(work,'tested-trial-download.csv');
    const winId = await app().evaluate(({BrowserWindow},[index,download])=>{
        const win = new BrowserWindow({width:425,height:900,webPreferences:{
            sandbox:true,contextIsolation:true,nodeIntegration:false,partition:'checked-explanation-audit-offline'}});
        win.webContents.session.webRequest.onBeforeRequest({urls:['http://*/*','https://*/*']},(_,callback)=>callback({cancel:true}));
        win.webContents.session.on('will-download',(_,item)=>{
            item.setSavePath(download);
            item.once('done',(_,status)=>{win.auditDownload=status;});
        });
        win.loadFile(index);
        return win.id;
    },[path.join(work,'index.html'),downloaded]);
    const offline = await opened;
    try {
        await offline.waitForFunction(digest=>document.getElementById('audit-browser-digest')?.textContent===digest,evidence.browserAuditSha256);
        assert(offline.url().startsWith('file:'));
        assert(await offline.evaluate(()=>document.documentElement.scrollWidth<=innerWidth));
        await offline.locator('#trial-0 button').press('Enter');
        assert.equal(await offline.locator('#trial-0 .treatment').innerText(),'55 → 44 °F');
        assert.equal(await offline.locator('#trial-0 .recorded').innerText(),'2026-11-22T14:01:00Z');
        await offline.screenshot({path:path.join(work,'offline-audit.png'),fullPage:true});
        await offline.getByRole('link',{name:'Trial observations',exact:true}).click();
        let status;
        for(let attempt=0;attempt<80&&!status;attempt++) {
            status = await app().evaluate(({BrowserWindow},id)=>BrowserWindow.fromId(id)?.auditDownload,winId);
            if (!status) await new Promise(resolve=>setTimeout(resolve,100));
        }
        assert.equal(status,'completed');
        assert.equal(await fs.readFile(downloaded,'utf8'),readings);
    } finally {
        await app().evaluate(({BrowserWindow},id)=>BrowserWindow.fromId(id)?.destroy(),winId);
    }
    const proof = {passed:true,fixture:'Synthetic paired trials with hand-derived exact effects',
        outcome:saved.explanationAudit,otherOutcomes,browserChecks:saved.browserChecks,evidence,checks:[
            'Ambiguous or empty displayed evidence is refused. A displayed older briefing is selected exactly.',
            'A proposal without observations saves a resumable draft. The draft and completed report survive separate desktop restarts.',
            'Original comparison input, repeated references, wrong units, negative error and undated trials are refused.',
            'Consistent, conflicting, mixed and insufficient or overlapping observations produce distinct outcomes.',
            'Exact source links, plots, dates, arithmetic and bounds are checked in Java and in the live Canvas.',
            'A changed original briefing receipt, comparison export, frozen trial file or browser digest is refused.',
            'The standalone file works with network blocked. Keyboard expansion and byte-exact CSV download are verified.'
        ]};
    await fs.writeFile(path.join(work,'result.json'),JSON.stringify(proof,null,2)+'\n');
}

module.exports = {testExplanationAudit};
