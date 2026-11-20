'use strict';
const assert = require('node:assert/strict');
const {createHash} = require('node:crypto');
const fs = require('node:fs/promises');
const path = require('node:path');
const {pathToFileURL} = require('node:url');
const {launch} = require('../../workspace-desktop/test/calibration-integration.cjs');

const build = 'Compare the last two verified routed CSVs.';
const recheck = 'Recheck the routed CSV comparison.';
const hash = bytes => createHash('sha256').update(bytes).digest('hex');

async function main() {
    const output = path.resolve(process.argv[2] || 'build/edition199-live');
    const prior = path.resolve(process.argv[3] || 'build/edition198-release/project');
    const project = path.join(output,'project');
    await fs.mkdir(output);
    await fs.mkdir(project);
    for (const name of ['workspace.fworld','workspace.fworld.previous','artifacts','receipts','request-workbench','exports'])
        await fs.cp(path.join(prior,name),path.join(project,name),{recursive:true});
    Object.assign(process.env,{FRAYNIX_WORKSPACE_NO_MODELS:'1',FRAYNIX_WORKSPACE_PERSISTENT:'0',
        FRAYNIX_WORKSPACE_JAVA_HEAP_MIB:'768',FRAYNIX_WORKSPACE_JAVA_PROCESSORS:'2'});
    let {app,page} = await launch(project);
    const screenshots = [];
    const errors = [];
    const flags = {timeout:45000};
    const api = (route,args) => app.evaluate((_,[route,args])=>global.workspaceTest.api(route,args),[route,args]);
    const call = (command,args) => page.evaluate(([command,args])=>
        window.workspace.request({operation:'requestWorkbench',command,args}),[command,args]);
    const artifactCount = async () => (await api('/api/state')).artifacts.length;

    async function ready() {
        page.on('pageerror',error=>errors.push(error.message));
        await app.evaluate(({BrowserWindow})=>BrowserWindow.getAllWindows().find(w=>w.isVisible()).setSize(1480,1030));
        assert.equal((await api('/api/state')).models.length,0);
    }
    async function restart() {
        const pid = app.process().pid;
        await app.close();
        ({app,page} = await launch(project));
        assert.notEqual(app.process().pid,pid);
        await ready();
    }
    async function chat(text) {
        await page.locator('#goal').fill(text);
        await page.locator('#run').click();
    }
    async function waitMessage(text) {
        await page.getByText(text,{exact:true}).last().waitFor({state:'visible',...flags});
    }
    async function capture(name) {
        await page.evaluate(()=>new Promise(resolve=>requestAnimationFrame(()=>requestAnimationFrame(resolve))));
        const png = Buffer.from(await app.evaluate(()=>global.workspaceTest.capture()),'base64');
        await fs.writeFile(path.join(output,name),png);
        screenshots.push({name,sha256:hash(png)});
    }
    async function display() {
        return app.evaluate(()=>global.workspaceTest.canvasRead(`Object.fromEntries(
            ['source-earlier','source-later','earlier-fahrenheit','later-fahrenheit','change-fahrenheit',
             'earlier-millimetres','later-millimetres','change-millimetres']
                .map(id=>[id,document.getElementById(id).textContent]))`));
    }
    async function readExport(receipt) {
        const csv = await fs.readFile(receipt.path);
        assert.equal(hash(csv),receipt.sha256);
        return {exportId:receipt.id,sha256:receipt.sha256,source:receipt.source,base64:csv.toString('base64')};
    }
    async function tamperExport(receipt,verify) {
        const file = receipt.path;
        const meta = file+'.receipt.json';
        const bytes = await fs.readFile(file);
        const text = await fs.readFile(meta);
        try {
            const changed = Buffer.from(bytes.toString('utf8').replace('63.5','63.6'));
            assert.notDeepEqual(changed,bytes);
            const rewritten = JSON.parse(text);
            rewritten.bytes=changed.length;
            rewritten.sha256=hash(changed);
            rewritten.source.reportSha256=rewritten.sha256;
            await fs.writeFile(file,changed);
            await fs.writeFile(meta,JSON.stringify(rewritten));
            await assert.rejects(verify,/disagrees with the verified routing plan/);
        } finally {
            await fs.writeFile(file,bytes);
            await fs.writeFile(meta,text);
        }
    }

    try {
        await ready();
        const before = await artifactCount();
        const oldDashboard = JSON.parse(await fs.readFile('docs/readmes/198/verification.json'));
        const legacy = await call('routing-dashboard',{verifyOnly:true});
        assert.equal(legacy.browserChecks,15);
        assert.deepEqual(legacy.artifact,oldDashboard.saved.artifact);
        assert(legacy.rechecked);
        await assert.rejects(call('routing-dashboard',{compare:true}),/two different verified routing plans first/);
        assert.equal(await artifactCount(),before);

        const oldPlan = JSON.parse(await fs.readFile('docs/readmes/196/verification.json')).done;
        const newPlan = JSON.parse(await fs.readFile('docs/readmes/197/verification.json')).complete;
        const earlier = await call('export-routing',{id:oldPlan.id});
        const laterFirst = await call('export-routing',{id:newPlan.id});
        const later = await call('export-routing',{id:newPlan.id});
        assert.notEqual(later.id,laterFirst.id);
        assert.equal(later.sha256,laterFirst.sha256);
        const file = await readExport(later);
        const baseline = await readExport(earlier);
        const repeated = await readExport(laterFirst);
        await assert.rejects(api('/api/requests/routing-dashboard-prepare',{file,baseline:repeated}),/two different verified routing plans/);

        await chat(build);
        await waitMessage('Comparison verified against Java. 12 browser checks passed.');
        const saved = await api('/api/requests/routing-dashboard-prepare',{file,baseline});
        assert.equal(saved.earlierExportId,earlier.id);
        assert.equal(saved.laterExportId,later.id);
        const artifact = await api('/api/artifact/read',{id:saved.artifact.id});
        const proof = JSON.parse(artifact.contents['verification.json']);
        assert.equal(artifact.contents['earlier-routed-results.csv'],Buffer.from(baseline.base64,'base64').toString('utf8'));
        assert.equal(artifact.contents['later-routed-results.csv'],Buffer.from(file.base64,'base64').toString('utf8'));
        assert.deepEqual(proof.comparisons.map(row=>[row.unit,row.earlier.count,row.later.count,row.meanChange]),
            [['°F',2,3,'23.175'],['mm',1,2,'441.325']]);
        assert.equal(proof.earlier.exportId,earlier.id);
        assert.equal(proof.later.exportId,later.id);
        assert.equal(await artifactCount(),before+1);
        const expected = {
            'source-earlier':'Earlier export · 3 rows · '+earlier.sha256.slice(0,12),
            'source-later':'Later export · 5 rows · '+later.sha256.slice(0,12),
            'earlier-fahrenheit':'2 rows · Min -40 °F · Max 99.05 °F · Mean 29.525 °F',
            'later-fahrenheit':'3 rows · Min -40 °F · Max 99.05 °F · Mean 52.7 °F',
            'change-fahrenheit':'Mean change · +23.175 °F',
            'earlier-millimetres':'1 row · Min 63.5 mm · Max 63.5 mm · Mean 63.5 mm',
            'later-millimetres':'2 rows · Min 63.5 mm · Max 946.15 mm · Mean 504.825 mm',
            'change-millimetres':'Mean change · +441.325 mm',
        };
        assert.deepEqual(await display(),expected);
        await capture('comparison-separate-units.png');

        const receiptFile = path.join(project,'receipts',saved.artifact.id+'-1.json');
        const originalReceipt = await fs.readFile(receiptFile);
        const intervening = await call('export-routing',{id:oldPlan.id});
        assert.notEqual(intervening.id,earlier.id);
        await restart();
        await chat(recheck);
        await waitMessage('Comparison verified against Java. 12 fresh browser checks passed.');
        assert.deepEqual(await display(),expected);
        assert.deepEqual(await fs.readFile(receiptFile),originalReceipt);
        assert.equal(await artifactCount(),before+1);
        await capture('comparison-rechecked-after-restart.png');

        const pair = {earlier:earlier.id,later:later.id};
        const verify = () => call('routing-dashboard',{compare:true,verifyOnly:true,pair});
        await tamperExport(earlier,verify);
        await tamperExport(later,verify);
        const prepared = await api('/api/requests/routing-dashboard-prepare',{file,baseline,verifyOnly:true});
        const changed = JSON.parse(originalReceipt);
        changed.checks.find(row=>row.selector==='#change-fahrenheit').actual.text=expected['change-millimetres'];
        await assert.rejects(api('/api/requests/routing-dashboard-finish',
            {id:saved.id,file,baseline,htmlSha256:prepared.htmlSha256,checks:changed.checks}),/Browser result differed/);
        assert.deepEqual(await api('/api/requests/routing-dashboard-prepare',{file,baseline}),saved);
        assert.deepEqual(errors,[]);

        for (const [name,bytes] of Object.entries(artifact.contents))
            await fs.writeFile(path.join(output,name==='index.html'?'comparison.html':name),bytes);
        const offline = await app.evaluate(async ({BrowserWindow},url)=>{
            const window = new BrowserWindow({show:false,webPreferences:{sandbox:true,contextIsolation:true,nodeIntegration:false}});
            try {
                await window.loadURL(url);
                return await window.webContents.executeJavaScript(`new Promise((resolve,reject)=>{
                    const stop=Date.now()+5000;
                    function inspect(){
                        const text=document.querySelector('#change-millimetres')?.textContent;
                        if(text)resolve(text);
                        else if(Date.now()>stop)reject(Error('Comparison did not render'));
                        else requestAnimationFrame(inspect);
                    }
                    inspect();
                })`);
            } finally { window.destroy(); }
        },pathToFileURL(path.join(output,'comparison.html')).href);
        assert.equal(offline,expected['change-millimetres']);
        const result = {passed:true,saved,proof,browserReceipt:artifact.test,screenshots,checks:{
            legacyDashboardRechecked:true,singlePlanRefused:true,repeatExportSkipped:true,distinctSourcePlans:true,
            pinnedAcrossRestart:true,recheckedLiveBrowser:true,originalReceiptPreserved:true,oneComparisonArtifact:true,
            earlierTamperRefused:true,laterTamperRefused:true,mixedUnitOutputRefused:true,standalone:true}};
        await fs.writeFile(path.join(output,'result.json'),JSON.stringify(result,null,2)+'\n');
        console.log(JSON.stringify({passed:true,changes:proof.comparisons.map(row=>[row.unit,row.meanChange]),checks:result.checks}));
    } catch (error) {
        await fs.writeFile(path.join(output,'failure.txt'),error.stack+'\n'+await page.locator('body').innerText()).catch(()=>{});
        throw error;
    } finally { await app.close().catch(()=>{}); }
}

main().catch(error=>{console.error(error);process.exitCode=1;});
