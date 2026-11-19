'use strict';
const assert = require('node:assert/strict');
const {createHash} = require('node:crypto');
const fs = require('node:fs/promises');
const path = require('node:path');
const {pathToFileURL} = require('node:url');
const {launch} = require('../../workspace-desktop/test/calibration-integration.cjs');

const build = 'Build a dashboard from the verified routed CSV.';
const recheck = 'Recheck the routed CSV dashboard.';
const temperature = '3 rows · Min -40 °F · Max 99.05 °F · Mean 52.7 °F';
const length = '2 rows · Min 63.5 mm · Max 946.15 mm · Mean 504.825 mm';
const hash = bytes => createHash('sha256').update(bytes).digest('hex');

function idFrom(text) {
    const digest = createHash('md5').update(text).digest();
    digest[6] = (digest[6]&15)|48;
    digest[8] = (digest[8]&63)|128;
    return digest.toString('hex').replace(/^(........)(....)(....)(....)(............)$/,'$1-$2-$3-$4-$5');
}

async function main() {
    const output = path.resolve(process.argv[2] || 'build/edition198-live');
    const prior = path.resolve(process.argv[3] || 'build/edition197-final/project');
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
    async function displayed() {
        return app.evaluate(()=>global.workspaceTest.canvasRead(`({
            count:document.querySelector('#dashboard-count').textContent,
            temperature:document.querySelector('#summary-fahrenheit').textContent,
            length:document.querySelector('#summary-millimetres').textContent,
            rows:[...document.querySelectorAll('#routed-rows li')].map(row=>row.textContent)
        })`));
    }
    async function changeReceipt(file,check) {
        const original = await fs.readFile(file);
        try {
            const changed = JSON.parse(original);
            changed.checks[1].actual.text='Changed evidence';
            await fs.writeFile(file,JSON.stringify(changed));
            await assert.rejects(check,/original artifact test receipt changed/);
        } finally { await fs.writeFile(file,original); }
    }

    try {
        await ready();
        const priorProof = JSON.parse(await fs.readFile('docs/readmes/197/verification.json'));
        const directory = path.join(project,'exports');
        const manifest = (await fs.readFile(path.join(directory,'manifest.jsonl'),'utf8')).trim().split('\n').map(line=>JSON.parse(line));
        const source = manifest.filter(entry=>entry.kind==='verified-routed-results').at(-1);
        const filePath = path.join(directory,source.id+'.csv');
        const sourceReceiptPath = filePath+'.receipt.json';
        const csv = await fs.readFile(filePath);
        const file = {exportId:source.id,sha256:hash(csv),source:source.source,base64:csv.toString('base64')};
        assert.equal(file.sha256,priorProof.export.sha256);
        assert.equal(csv.toString('utf8'),await fs.readFile('docs/readmes/197/verified-routed-results.csv','utf8'));
        const plan = await api('/api/requests/routing-read',{id:source.source.planId});
        assert.deepEqual(plan,priorProof.complete);
        const before = await artifactCount();
        await chat(recheck);
        await waitMessage('Error invoking remote method \'workspace:request\': Error: Build this routed CSV dashboard before rechecking it');
        assert.equal(await artifactCount(),before);

        const request = idFrom('routed-dashboard:'+source.id);
        const artifactId = idFrom('routed-dashboard:'+request);
        const block = path.join(project,'artifacts',artifactId);
        await fs.writeFile(block,'Test obstruction at the new artifact directory',{flag:'wx'});
        let staged;
        try {
            await chat(build);
            await page.waitForFunction(id=>document.querySelector('#notice').textContent.includes(id),artifactId,flags);
            staged = JSON.parse(await fs.readFile(path.join(project,'request-workbench','routed-dashboards',request+'.json'))).data;
            assert.equal(staged.checks.length,15);
            assert(!staged.artifact);
            assert.equal(await artifactCount(),before);
            assert((await api('/api/requests/routing-dashboard-prepare',{file})).savePending);
        } finally { await fs.unlink(block); }

        await restart();
        await chat(build);
        await waitMessage('Dashboard verified against Java. 15 browser checks passed.');
        const saved = await api('/api/requests/routing-dashboard-prepare',{file});
        assert.equal(saved.id,request);
        assert.equal(saved.artifact.id,artifactId);
        assert.equal(await artifactCount(),before+1);
        const artifact = await api('/api/artifact/read',{id:artifactId});
        const evidence = JSON.parse(artifact.contents['verification.json']);
        assert.deepEqual(artifact.test.checks,staged.checks);
        assert.equal(artifact.contents['routed-results.csv'],csv.toString('utf8'));
        assert.deepEqual(evidence.groups,[
            {unit:'°F',count:3,min:'-40',max:'99.05',mean:'52.7'},
            {unit:'mm',count:2,min:'63.5',max:'946.15',mean:'504.825'},
        ]);
        assert.deepEqual(evidence.rowOrder,['2','3','4','5','6']);
        const original = await displayed();
        assert.equal(original.count,'5 verified rows · 2 output units');
        assert.equal(original.temperature,temperature);
        assert.equal(original.length,length);
        assert(original.rows[0].includes('37.25 °C → 99.05 °F'));
        assert(original.rows[1].includes('37.25 in → 946.15 mm'));
        assert.equal(original.rows[0].slice(1),original.rows[2].slice(1));
        await capture('dashboard-separate-units.png');

        const receiptFile = path.join(project,'receipts',artifactId+'-1.json');
        const savedReceipt = await fs.readFile(receiptFile);
        await restart();
        await chat(recheck);
        await waitMessage('Dashboard verified against Java. 15 fresh browser checks passed.');
        assert.deepEqual(await displayed(),original);
        assert.deepEqual(await fs.readFile(receiptFile),savedReceipt);
        assert.equal(await artifactCount(),before+1);
        await capture('dashboard-rechecked-after-restart.png');
        const filter = await app.evaluate((_,steps)=>global.workspaceTest.canvasChecks(steps),[
            {selector:'#filter-millimetres',action:'click',expectCount:1},
            {selector:'#routed-rows li',expectCount:2},
        ]);
        assert(filter.every(row=>row.passed));
        assert.deepEqual((await displayed()).rows,[original.rows[1],original.rows[3]]);
        await capture('dashboard-length-filter.png');

        const prepared = await api('/api/requests/routing-dashboard-prepare',{file,verifyOnly:true});
        const failedCheck = JSON.parse(JSON.stringify(artifact.test.checks));
        const millimetres = failedCheck.find(row=>row.selector==='#summary-millimetres');
        millimetres.actual.text=temperature;
        await assert.rejects(api('/api/requests/routing-dashboard-finish',{id:request,file,htmlSha256:prepared.htmlSha256,
            checks:failedCheck}),/Browser result differed/);
        const externalReceipt = await fs.readFile(sourceReceiptPath);
        try {
            const changed = Buffer.from(csv.toString('utf8').replace('946.15','946.16'));
            const resealed = JSON.parse(externalReceipt);
            resealed.sha256=hash(changed);
            resealed.source.reportSha256=resealed.sha256;
            resealed.bytes=changed.length;
            await fs.writeFile(filePath,changed);
            await fs.writeFile(sourceReceiptPath,JSON.stringify(resealed));
            await assert.rejects(call('routing-dashboard',{verifyOnly:true}),/disagrees with the verified routing plan/);
        } finally {
            await fs.writeFile(filePath,csv);
            await fs.writeFile(sourceReceiptPath,externalReceipt);
        }
        await changeReceipt(receiptFile,()=>call('routing-dashboard',{verifyOnly:true}));
        await changeReceipt(path.join(project,'receipts',source.source.children[0].artifact.id+'-1.json'),
            ()=>call('routing-dashboard',{verifyOnly:true}));
        assert.deepEqual(await api('/api/requests/routing-dashboard-prepare',{file}),saved);
        assert.deepEqual(errors,[]);

        await fs.writeFile(path.join(output,'dashboard.html'),artifact.contents['index.html']);
        await fs.writeFile(path.join(output,'routed-results.csv'),csv);
        const standalone = await app.evaluate(async ({BrowserWindow},url)=>{
            const window = new BrowserWindow({show:false,webPreferences:{sandbox:true,contextIsolation:true,nodeIntegration:false}});
            try {
                await window.loadURL(url);
                return await window.webContents.executeJavaScript("document.querySelector('#summary-millimetres').textContent");
            } finally { window.destroy(); }
        },pathToFileURL(path.join(output,'dashboard.html')).href);
        assert.equal(standalone,length);
        const result = {passed:true,saved,evidence,screenshots,browserReceipt:artifact.test,
            checks:{restartCount:2,recoveredOriginalChecks:true,recheckedLiveBrowser:true,singleArtifact:true,standalone:true,
                duplicateRowsKept:true,mixedUnitSummaryRefused:true,externalCsvTamperRefused:true,
                dashboardReceiptTamperRefused:true,sourceChildReceiptTamperRefused:true,originalReceiptPreserved:true}};
        await fs.writeFile(path.join(output,'result.json'),JSON.stringify(result,null,2)+'\n');
        console.log(JSON.stringify({passed:true,browserChecks:saved.browserChecks,groups:evidence.groups,checks:result.checks}));
    } catch (error) {
        await fs.writeFile(path.join(output,'failure.txt'),error.stack+'\n'+await page.locator('body').innerText()).catch(()=>{});
        throw error;
    } finally { await app.close().catch(()=>{}); }
}

main().catch(error=>{console.error(error);process.exitCode=1;});
