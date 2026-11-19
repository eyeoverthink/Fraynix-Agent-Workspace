'use strict';
const assert = require('node:assert/strict');
const {createHash} = require('node:crypto');
const fs = require('node:fs/promises');
const path = require('node:path');
const {pathToFileURL} = require('node:url');
const {launch} = require('../../workspace-desktop/test/calibration-integration.cjs');

const buildExample = 'Build a data-to-tool classifier using column reading.';
const teach = 'Teach data-to-tool lesson sample-ranges';
const reuse = 'Use data-to-tool lesson sample-ranges';
const title = 'CSV table · Range classification';
const rulesHeader = 'min_inclusive,max_exclusive,label';
const rules = '\ufeff'+rulesHeader+'\r\n60,1000000,hot\r\n-1000000,0,cold\r\n10,50,warm\r\n0,10,normal\r\n';
const example = 'id,reading\nt-1,-12\nt-2,2\nt-3,22\nt-4,65';
const fresh = 'reading,station\n-0.0001,"A, north"\n0,B\n9.9999,C\n10,D\n49.9999,E\n60,F\n999999.9999,G';
const expectedFresh = 'reading,station,classification\n-0.0001,"A, north",cold\n0,B,normal\n9.9999,C,normal\n10,D,warm\n49.9999,E,warm\n60,F,hot\n999999.9999,G,hot';
const hash = bytes => createHash('sha256').update(bytes).digest('hex');

async function main() {
    const output = path.resolve(process.argv[2] || 'build/edition195-live');
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
    async function build(id,expectedTitle,expected,attachMs,checkCount=20) {
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
        assert.equal(artifact.test.checks.length,checkCount);
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
        const old = JSON.parse(await fs.readFile('docs/readmes/194/verification.json'));
        const results = [];
        for (const [id,expected] of [[old.teacherId,old.teacher],[old.learnerId,old.fresh]]) {
            const proof = await verify(id);
            const artifact = await api('/api/artifact/read',{id:proof.artifact.id});
            assert.deepEqual(JSON.parse(artifact.contents['verification.json']),expected);
            assert.equal(hash(artifact.contents['index.html']),expected.htmlSha256);
            assert.equal(artifact.test.checks.length,16);
            results.push({id,sha256:proof.artifact.sha256});
        }
        return results;
    }

    async function attachRules(name,text,accepted=true) {
        const buffer = Buffer.from(text);
        await page.locator('#requests-rules-file').setInputFiles({name,mimeType:'text/csv',buffer});
        if (accepted)
            await page.waitForFunction(digest=>document.querySelector('#requests-rules-source').textContent.includes(digest),hash(buffer),flags);
    }

    async function status(pattern) {
        await page.waitForFunction(source=>RegExp(source).test(document.querySelector('#requests-status').textContent),pattern.source,flags);
    }

    try {
        await ready();
        const legacy = await verifyLegacy();
        const teacherId = await start(buildExample);
        assert(await page.locator('#requests-file').isDisabled());
        await attachRules('overlap.csv',rulesHeader+'\n0,10,one\n9,20,two',false);
        await status(/ranges overlap/);
        assert(!(await read(teacherId)).rulesSource);
        await attachRules('duplicate-label.csv',rulesHeader+'\n0,10,one\n10,20,one',false);
        await status(/unique plain label/);
        await attachRules('sensor-rules.csv',rules);
        await attach('example.csv',example);
        const originalReview = await read(teacherId);
        assert.equal(originalReview.rulesSource.sha256,hash(rules));
        assert(!('base64' in originalReview.rulesSource));
        await attachRules('changed.csv',rules.replace('10,50,warm','10,55,warm'));
        const replaced = await read(teacherId);
        assert(!replaced.source);
        assert.notEqual(replaced.reviewSha256,originalReview.reviewSha256);
        await assert.rejects(api('/api/requests/fray-prepare',{id:teacherId,reviewSha256:originalReview.reviewSha256}),/reviewed action or attached source changed/);
        await attachRules('sensor-rules.csv',rules);
        const attachmentTime = await attach('example.csv',example);
        await capture('rules-and-example-reviewed.png');
        const teacher = await build(teacherId,title,'id,reading,classification\nt-1,-12,cold\nt-2,2,normal\nt-3,22,warm\nt-4,65,hot',attachmentTime);
        assert.equal(teacher.evidence.rulesSha256,hash(rules));
        assert(teacher.evidence.unseenValidCases >= 4);
        assert.match(teacher.evidence.ruleTable,/^-1000000,0,cold$/m);
        await chat(teach);
        await page.getByText('Saved lesson sample-ranges.',{exact:false}).waitFor({state:'visible',...flags});
        await capture('chat-teaches-ranges.png');

        await restart();
        assert.deepEqual(await verify(teacherId),teacher.proof);
        const learnerId = await start(reuse);
        assert(!await page.locator('#requests-rules-attachment').isVisible());
        assert.match(await page.locator('#requests-rules-source').textContent(),/rules verified by lesson sample-ranges/);
        await assert.rejects(api('/api/requests/attach-rules',{id:learnerId,name:'different.csv',base64:Buffer.from(rules).toString('base64')}),/Only an original classifier/);
        await rejectFile('gap.csv','reading,station\n55,Gap',/No declared range covers 55/,learnerId);
        await rejectFile('exclusive.csv','reading\n1000000',/No declared range covers 1000000/,learnerId);
        await rejectFile('old-values.csv','reading\n22\n65\n-12\n2',/measurement absent/,learnerId);
        const learnerMs = await attach('fresh-readings.csv',fresh);
        const pending = await read(learnerId);
        assert.equal(pending.choice.lesson.rulesSha256,hash(rules));
        await restart();
        assert.deepEqual(await read(learnerId),pending);
        await reviewLatest();
        const learned = await build(learnerId,title,expectedFresh,learnerMs);
        assert.equal(learned.evidence.inputCount,7);
        assert.equal(learned.evidence.ruleTable,teacher.evidence.ruleTable);
        assert.equal(learned.evidence.ruleProgramSha256,teacher.evidence.ruleProgramSha256);
        assert.equal(learned.evidence.sourceSha256,hash(fresh));
        assert.match(learned.evidence.cases[1].input,/^sample,reading\n/);
        assert(learned.evidence.cases.some(test=>test.input==='reading\n50' && test.java.includes('unique headers')));
        await capture('fresh-range-result.png');

        const reviewFile = path.join(project,'request-workbench',teacherId+'.task.json');
        const rawReview = await fs.readFile(reviewFile);
        try {
            const changed = JSON.parse(rawReview);
            changed.data.rulesSource.base64=Buffer.from(rules.replace('10,50,warm','10,55,warm')).toString('base64');
            await fs.writeFile(reviewFile,JSON.stringify(changed));
            await assert.rejects(verify(learnerId),/checkpoint integrity mismatch/);
        } finally { await fs.writeFile(reviewFile,rawReview); }

        await restart();
        assert.deepEqual(await verify(learnerId),learned.proof);
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
                    input.value = 'room,reading\\nHall,12.3456';
                    input.dispatchEvent(new Event('input'));
                    return {protocol:location.protocol,result:document.querySelector('#result').textContent};
                })()`);
            } finally { window.destroy(); }
        },pathToFileURL(exported.path).href);
        assert.deepEqual(offline,{protocol:'file:',result:'room,reading,classification\nHall,12.3456,warm'});

        const tinyId = await start(buildExample);
        await attachRules('tight.csv',rulesHeader+'\n0,0.0001,first\n0.0001,0.0002,second');
        const tinyMs = await attach('all-values.csv','reading\n0\n0.0001');
        const tiny = await build(tinyId,title,'reading,classification\n0,first\n0.0001,second',tinyMs,18);
        assert.equal(tiny.evidence.unseenValidCases,0);
        const tooNarrow = await api('/api/requests/chat',{request:'Teach data-to-tool lesson too-narrow',teacherId:tinyId});
        assert.match(tooNarrow.refusal,/no unseen valid value/);
        assert.deepEqual(errors,[]);
        const results = {passed:true,teacherId,learnerId,teacher:teacher.evidence,fresh:learned.evidence,
            exhausted:tiny.evidence,offline,legacy,timing:{teacher:teacher.timing,fresh:learned.timing},
            checks:{restartCount:3,overlapRefused:true,duplicateLabelRefused:true,invalidatedReviewAfterRuleChange:true,
                retainedRuleSource:true,lessonRulesImmutable:true,gapRefused:true,upperBoundaryRefused:true,
                unchangedTeachingValuesRefused:true,changedRulesEvidenceRefused:true,unseenRequiredForTeaching:true},screenshots};
        await fs.writeFile(path.join(output,'result.json'),JSON.stringify(results,null,2)+'\n');
        await fs.writeFile(path.join(output,'browser-receipt.json'),JSON.stringify(learned.receipt,null,2)+'\n');
        await fs.writeFile(path.join(output,'offline-classifier.html'),learned.html);
        await fs.writeFile(path.join(output,'sensor-rules.csv'),rules);
        console.log(JSON.stringify({passed:true,checks:results.checks,legacy:legacy?.length,rows:learned.evidence.inputCount,timing:results.timing}));
    } catch (error) {
        await fs.writeFile(path.join(output,'failure.txt'),error.stack+'\n'+await page.locator('body').innerText()).catch(()=>{});
        throw error;
    } finally { await app.close().catch(()=>{}); }
}

main().catch(error=>{console.error(error);process.exitCode=1;});
