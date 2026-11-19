'use strict';
const assert = require('node:assert/strict');
const {createHash} = require('node:crypto');
const fs = require('node:fs/promises');
const path = require('node:path');
const {pathToFileURL} = require('node:url');
const {launch} = require('../../workspace-desktop/test/calibration-integration.cjs');

const buildExample = 'Build a data-to-tool converter from Celsius to Fahrenheit using column probe_c.';
const teach = 'Teach data-to-tool lesson probe-batch';
const reuse = 'Use data-to-tool lesson probe-batch';
const title = 'CSV table · Celsius to Fahrenheit';
const example = '\ufeffid,Zone,probe_c\r\ne-2,North,0\r\ne-4,"South, aisle ""B""",20\r\ne-6,East,-40\r\n';
const fresh = [
    'probe_c,Zone,id',
    '37.1234,"R&D, α",f-1',
    '-17.7777,"Two\nlines",f-2',
    '0,West,f-3',
    '-999999.9999,<note> & draft,f-4',
    '1000000,,f-5',
    '+0002.0001,North,f-6',
].join('\n');
const expectedFresh = [
    'probe_c,Zone,id,fahrenheit',
    '37.1234,"R&D, α",f-1,98.82212',
    '-17.7777,"Two\nlines",f-2,0.00014',
    '0,West,f-3,32',
    '-999999.9999,<note> & draft,f-4,-1799967.99982',
    '1000000,,f-5,1800032',
    '+0002.0001,North,f-6,35.60018',
].join('\n');
const hash = bytes => createHash('sha256').update(bytes).digest('hex');

async function main() {
    const output = path.resolve(process.argv[2] || 'build/edition194-live');
    const project = path.join(output,'project');
    const prior = process.argv[3] ? path.resolve(process.argv[3]) : null;
    await fs.mkdir(output);
    await fs.mkdir(project);
    if (prior) {
        for (const name of ['workspace.fworld','workspace.fworld.previous','artifacts','receipts','request-workbench'])
            await fs.cp(path.join(prior,name),path.join(project,name),{recursive:true});
    }
    Object.assign(process.env,{FRAYNIX_WORKSPACE_NO_MODELS:'1',FRAYNIX_WORKSPACE_PERSISTENT:'0',
        FRAYNIX_WORKSPACE_JAVA_HEAP_MIB:'768',FRAYNIX_WORKSPACE_JAVA_PROCESSORS:'2'});
    let {app,page} = await launch(project);
    const screenshots = [];
    const errors = [];
    const flags = {timeout:45000};
    const api = (route,args) => app.evaluate((_,[route,args])=>global.workspaceTest.api(route,args),[route,args]);
    const read = id => api('/api/requests/read',{id});
    const verify = id => api('/api/requests/verify',{id});
    const call = (command,args) => page.evaluate(([command,args])=>
        window.workspace.request({operation:'requestWorkbench',command,args}),[command,args]);
    const current = () => page.evaluate(()=>localStorage.getItem('fraynix-request-review'));
    const resultText = () => app.evaluate(()=>global.workspaceTest.canvasRead('document.querySelector("#result").textContent'));

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
    async function chat(message) {
        await page.locator('#goal').fill(message);
        await page.locator('#run').click();
    }
    async function start(message) {
        await chat(message);
        await page.locator('#requests-file').waitFor({state:'visible',...flags});
        return current();
    }
    async function attach(name,text) {
        const started = performance.now();
        const buffer = Buffer.from(text);
        await page.locator('#requests-file').setInputFiles({name,mimeType:'text/csv',buffer});
        const digest = hash(buffer);
        await page.waitForFunction(value=>document.querySelector('#requests-source').textContent.includes(value),digest,flags);
        assert.equal((await read(await current())).source.sha256,digest);
        return Math.round(performance.now()-started);
    }
    async function rejectFile(name,text,pattern,id) {
        await page.locator('#requests-file').setInputFiles({name,mimeType:'text/csv',buffer:Buffer.from(text)});
        await page.waitForFunction(source=>RegExp(source).test(document.querySelector('#requests-status').textContent),pattern.source,flags);
        assert(!(await read(id)).source,'Rejected CSV must leave the review unchanged');
        assert(await page.locator('#requests-confirm').isDisabled());
    }
    async function reviewLatest() {
        await page.locator('#requests-open').click();
        await page.locator('#requests-reopen').click();
    }
    async function build(id,expectedTitle,expected,attachMs) {
        const started = performance.now();
        await page.locator('#requests-confirm').click();
        await page.waitForFunction(title=>document.querySelector('#artifact-title').textContent===title
            && document.querySelector('#requests-status').textContent.startsWith('Built an offline tool.'),expectedTitle,flags);
        const proof = await verify(id);
        const buildVerifyMs = Math.round(performance.now()-started);
        assert(proof.passed && proof.output==='index.html');
        assert.equal(await resultText(),expected);
        const artifact = await api('/api/artifact/read',{id:proof.artifact.id});
        const evidence = JSON.parse(artifact.contents['verification.json']);
        assert.equal(hash(artifact.contents['index.html']),evidence.htmlSha256);
        assert.equal(artifact.test.checks.length,16);
        assert(artifact.test.passed && evidence.cases.every(test=>test.java===test.browser));
        return {proof,evidence,receipt:artifact.test,html:artifact.contents['index.html'],
            timing:{rows:evidence.inputCount,columns:evidence.inputHeaders.length,attachMs,buildVerifyMs}};
    }
    async function capture(name) {
        await page.evaluate(()=>new Promise(resolve=>requestAnimationFrame(()=>requestAnimationFrame(resolve))));
        const png = Buffer.from(await app.evaluate(()=>global.workspaceTest.capture()),'base64');
        await fs.writeFile(path.join(output,name),png);
        screenshots.push({name,sha256:hash(png)});
    }
    async function verifyLegacy() {
        if (!prior) return null;
        const old = JSON.parse(await fs.readFile('docs/readmes/193/verification.json'));
        const results = [];
        for (const [id,expected] of [[old.teacherId,old.original],[old.learnerId,old.fresh]]) {
            const proof = await verify(id);
            const artifact = await api('/api/artifact/read',{id:proof.artifact.id});
            const evidence = JSON.parse(artifact.contents['verification.json']);
            assert.deepEqual(evidence,expected);
            assert.equal(hash(artifact.contents['index.html']),expected.htmlSha256);
            assert.equal(artifact.test.checks.length,10);
            results.push({id,sha256:proof.artifact.sha256});
        }
        return results;
    }

    try {
        await ready();
        const legacy = await verifyLegacy();
        const teacherId = await start(buildExample);
        assert.equal((await read(teacherId)).choice.tableColumn,'probe_c');
        const teacherMs = await attach('teaching-table.csv',example);
        const teacher = await build(teacherId,title,
            'id,Zone,probe_c,fahrenheit\ne-2,North,0,32\ne-4,"South, aisle ""B""",20,68\ne-6,East,-40,-40',teacherMs);
        assert.equal(teacher.evidence.tableColumn,'probe_c');
        await chat(teach);
        await page.getByText('Saved lesson probe-batch.',{exact:false}).waitFor({state:'visible',...flags});
        await capture('chat-teaches-named-column.png');

        await restart();
        assert.deepEqual(await verify(teacherId),teacher.proof);
        const learnerId = await start(reuse);
        await rejectFile('missing.csv','Zone,sensor_c\nWest,10',/missing the selected column probe_c/,learnerId);
        await rejectFile('duplicate.csv','probe_c,Zone,Zone\n10,West,East',/headers must be nonempty and unique/,learnerId);
        await rejectFile('duplicate-measurement.csv','probe_c,probe_c\n10,20',/headers must be nonempty and unique/,learnerId);
        await rejectFile('ragged.csv','probe_c,Zone\n10',/must have 2 cells/,learnerId);
        await rejectFile('output-conflict.csv','probe_c,fahrenheit\n10,50',/already has the output column fahrenheit/,learnerId);
        await rejectFile('prior-values.csv','probe_c,Zone\n20,New\n-040.0000,New\n+0,New',/measurement absent/,learnerId);
        const learnerMs = await attach('reordered-table.csv',fresh);
        const pending = await read(learnerId);
        assert.equal(pending.choice.lesson.tableColumn,'probe_c');
        assert.equal(pending.choice.lesson.teacherId,teacherId);

        await restart();
        assert.deepEqual(await read(learnerId),pending);
        await reviewLatest();
        await page.getByText('Attach a CSV with the probe_c column.',{exact:false}).waitFor({state:'visible',...flags});
        await capture('review-reordered-table.png');
        const learned = await build(learnerId,title,expectedFresh,learnerMs);
        assert.equal(learned.evidence.inputCount,6);
        assert.deepEqual(learned.evidence.inputHeaders,['probe_c','Zone','id']);
        assert.equal(learned.evidence.sourceSha256,hash(fresh));
        assert.match(learned.evidence.cases[1].input,/^sample,probe_c\n/);
        assert.equal(learned.evidence.cases.filter(step=>step.java.startsWith('Use 1–8 unique headers')).length,5);
        await capture('reordered-table-verified.png');

        await restart();
        const recheckStarted = performance.now();
        assert.deepEqual(await verify(learnerId),learned.proof);
        const restartVerifyMs = Math.round(performance.now()-recheckStarted);
        assert.deepEqual(await verifyLegacy(),legacy);
        await reviewLatest();
        await page.locator('#requests-result').click();
        await page.locator('#requests-dialog').waitFor({state:'hidden',...flags});
        assert.equal(await resultText(),expectedFresh);
        const exported = await call('export',{id:learnerId});
        assert.equal(hash(await fs.readFile(exported.path)),learned.evidence.htmlSha256);
        const offline = await app.evaluate(async ({BrowserWindow},url)=>{
            const window = new BrowserWindow({show:false,webPreferences:{sandbox:true,contextIsolation:true,nodeIntegration:false}});
            try {
                await window.loadURL(url);
                return await window.webContents.executeJavaScript(`(()=>{
                    const input = document.querySelector('#source');
                    const output = document.querySelector('#result');
                    input.value = 'id,probe_c\\n"R-3, fresh",-7.5';
                    input.dispatchEvent(new Event('input'));
                    return {protocol:location.protocol,result:output.textContent};
                })()`);
            } finally { window.destroy(); }
        },pathToFileURL(exported.path).href);
        assert.deepEqual(offline,{protocol:'file:',result:'id,probe_c,fahrenheit\n"R-3, fresh",-7.5,18.5'});

        const lengthId = await start('Build a data-to-tool converter from inches to millimetres using column len_in.');
        const header = 'id,lot,x,y,len_in,source,tag,site';
        const rows = Array.from({length:24},(_,index)=>`${index},L,0,0,${index},S,T,A`);
        const lengthMs = await attach('eight-columns.csv',[header,...rows].join('\n'));
        const length = await build(lengthId,'CSV table · Inches to millimetres',
            [header+',millimetres',...rows.map((row,i)=>row+','+i*254/10)].join('\n'),lengthMs);
        assert.equal(length.evidence.inputCount,24);
        assert.deepEqual(errors,[]);
        const results = {passed:true,teacherId,learnerId,teacher:teacher.evidence,fresh:learned.evidence,
            length:length.evidence,offline,legacy,timing:{teacher:teacher.timing,reordered:learned.timing,
                boundary:length.timing,restartVerifyMs},checks:{restartCount:3,missingColumnRefused:true,
                duplicateHeadersRefused:true,raggedRowsRefused:true,outputCollisionRefused:true,
                repeatedTeachingValuesRefused:true,preservedUnrelatedCells:true},screenshots};
        await fs.writeFile(path.join(output,'result.json'),JSON.stringify(results,null,2)+'\n');
        await fs.writeFile(path.join(output,'browser-receipt.json'),JSON.stringify(learned.receipt,null,2)+'\n');
        await fs.writeFile(path.join(output,'offline-table-tool.html'),learned.html);
        console.log(JSON.stringify({passed:true,checks:results.checks,legacy:legacy?.length,timing:results.timing}));
    } catch (error) {
        await fs.writeFile(path.join(output,'failure.txt'),error.stack+'\n'+await page.locator('body').innerText()).catch(()=>{});
        throw error;
    } finally { await app.close().catch(()=>{}); }
}

main().catch(error=>{console.error(error);process.exitCode=1;});
