'use strict';
const assert = require('node:assert/strict');
const {createHash} = require('node:crypto');
const fs = require('node:fs/promises');
const path = require('node:path');
const {pathToFileURL} = require('node:url');
const {launch} = require('../../workspace-desktop/test/calibration-integration.cjs');

const hash = bytes => createHash('sha256').update(bytes).digest('hex');
const celsius = 'Build a data-to-tool converter from Celsius to Fahrenheit.';
const teach = 'Teach data-to-tool lesson temperature-batch';
const reuse = 'Use data-to-tool lesson temperature-batch';
const example = 'celsius\r\n0\r\n20\r\n-40\r\n36.625\r\n';
const fresh = 'celsius\n+0000000.0000\n-17.7777\n37.1234\n-999999.9999\n1000000\n';
const expectedFresh = '32 °F\n0.00014 °F\n98.82212 °F\n-1799967.99982 °F\n1800032 °F';

async function main() {
    const output = path.resolve(process.argv[2] || 'build/edition193-live');
    const project = path.join(output, 'project');
    await fs.mkdir(output);
    await fs.mkdir(project);
    Object.assign(process.env, {FRAYNIX_WORKSPACE_NO_MODELS:'1', FRAYNIX_WORKSPACE_PERSISTENT:'0',
        FRAYNIX_WORKSPACE_JAVA_HEAP_MIB:'768', FRAYNIX_WORKSPACE_JAVA_PROCESSORS:'2'});
    let {app, page} = await launch(project);
    const screenshots = [];
    const pageErrors = [];
    const flags = {timeout:45000};
    const api = (route,args) => app.evaluate((_,[route,args])=>global.workspaceTest.api(route,args),[route,args]);
    const verify = id => api('/api/requests/verify',{id});
    const review = id => api('/api/requests/read',{id});
    const call = (command,args) => page.evaluate(([command,args])=>
        window.workspace.request({operation:'requestWorkbench',command,args}),[command,args]);
    const active = () => page.evaluate(()=>localStorage.getItem('fraynix-request-review'));
    const status = () => page.locator('#requests-status').textContent();
    const canvasResult = () => app.evaluate(()=>global.workspaceTest.canvasRead('document.querySelector("#result").textContent'));

    async function ready() {
        page.on('pageerror',error=>pageErrors.push(error.message));
        await app.evaluate(({BrowserWindow})=>BrowserWindow.getAllWindows().find(w=>w.isVisible()).setSize(1480,1030));
        assert.equal((await api('/api/state')).models.length,0);
    }
    async function restart() {
        const pid = app.process().pid;
        await app.close();
        ({app,page}=await launch(project));
        assert.notEqual(app.process().pid,pid);
        await ready();
    }
    async function chat(request) {
        await page.locator('#goal').fill(request);
        await page.locator('#run').click();
    }
    async function start(request) {
        await chat(request);
        await page.locator('#requests-file').waitFor({state:'visible',...flags});
        return active();
    }
    async function attach(name, text, accepted=true) {
        const buffer = Buffer.from(text);
        await page.locator('#requests-file').setInputFiles({name,mimeType:'text/csv',buffer});
        if (accepted) {
            const digest = hash(buffer);
            await page.waitForFunction(value=>document.querySelector('#requests-source').textContent.includes(value),digest,flags);
            assert.equal((await review(await active())).source.sha256,digest);
        }
    }
    async function checkStatus(pattern) {
        await page.waitForFunction(source=>RegExp(source).test(document.querySelector('#requests-status').textContent),pattern.source,flags);
        assert.match(await status(),pattern);
    }
    async function openReview() {
        await page.locator('#requests-open').click();
        await page.locator('#requests-reopen').click();
    }
    async function build(id,title,expected) {
        await page.locator('#requests-confirm').click();
        await page.waitForFunction(title=>document.querySelector('#artifact-title').textContent===title
            && document.querySelector('#requests-status').textContent.startsWith('Built an offline tool.'),title,flags);
        const proof = await verify(id);
        assert(proof.passed && proof.output==='index.html');
        assert.equal(await canvasResult(),expected);
        const artifact = await api('/api/artifact/read',{id:proof.artifact.id});
        const evidence = JSON.parse(artifact.contents['verification.json']);
        assert.equal(hash(artifact.contents['index.html']),evidence.htmlSha256);
        assert.equal(artifact.test.checks.length,10);
        assert(artifact.test.passed && evidence.cases.every(test=>test.java===test.browser));
        return {proof,evidence,receipt:artifact.test,html:artifact.contents['index.html']};
    }
    async function capture(name) {
        await page.evaluate(()=>new Promise(resolve=>requestAnimationFrame(()=>requestAnimationFrame(resolve))));
        const bytes = Buffer.from(await app.evaluate(()=>global.workspaceTest.capture()),'base64');
        await fs.writeFile(path.join(output,name),bytes);
        screenshots.push({name,sha256:hash(bytes)});
    }
    async function refuseChanged(file,update,check,expected) {
        const original = await fs.readFile(file);
        try {
            const modified = JSON.parse(original);
            update(modified);
            await fs.writeFile(file,JSON.stringify(modified));
            await assert.rejects(check,expected);
        } finally {
            await fs.writeFile(file,original);
        }
    }
    try {
        await ready();
        const teacherId = await start(celsius);
        await attach('teaching-temperatures.csv',example);
        await assert.rejects(verify(teacherId));
        const premature = await api('/api/requests/chat',{request:teach,teacherId});
        assert.match(premature.refusal,/verified original CSV tool/);
        const original = await build(teacherId,'CSV · Celsius to Fahrenheit','32 °F\n68 °F\n-40 °F\n97.925 °F');
        assert.equal(original.evidence.inputCount,4);
        await openReview();
        await page.locator('#requests-teach-data').click();
        assert.equal(await page.locator('#goal').inputValue(),'Teach data-to-tool lesson ');
        await chat(teach);
        await page.getByText('Saved lesson temperature-batch.',{exact:false}).waitFor({state:'visible',...flags});
        await capture('chat-teaches-batch-tool.png');

        await restart();
        assert.deepEqual(await verify(teacherId),original.proof);
        const learnerId = await start(reuse);
        await attach('wrong-unit.csv','inches\n12',false);
        await checkStatus(/one column named celsius/);
        await attach('old-values.csv','celsius\n+20.0000\n-040.0\n000\n36.6250',false);
        await checkStatus(/measurement absent from the teaching example/);
        await attach('wrong-range.csv','celsius\n1000000.0001',false);
        await checkStatus(/Check row 2/);
        await attach('fresh-temperatures.csv',fresh);
        const pending = await review(learnerId);
        assert.equal(pending.status,'review');
        assert.equal(pending.choice.lesson.teacherId,teacherId);
        await capture('fresh-csv-reviewed.png');

        await restart();
        assert.deepEqual(await review(learnerId),pending);
        await openReview();
        await page.locator('#requests-confirm').waitFor({state:'visible',...flags});
        const confirm = {id:learnerId,reviewSha256:pending.reviewSha256};
        const prepared = await api('/api/requests/fray-prepare',confirm);
        await assert.rejects(api('/api/requests/execute',confirm),/data-tool browser checks/);
        await assert.rejects(api('/api/requests/fray-finish',{...confirm,
            htmlSha256:prepared.htmlSha256,checks:original.receipt.checks}),/Browser check|Browser input|Browser result/);
        await assert.rejects(verify(learnerId));
        const learned = await build(learnerId,'CSV · Celsius to Fahrenheit',expectedFresh);
        assert.equal(learned.evidence.inputCount,5);
        assert.deepEqual(learned.evidence.lesson,pending.choice.lesson);
        assert.notEqual(learned.proof.artifact.id,original.proof.artifact.id);
        assert.equal(learned.evidence.sourceSha256,hash(fresh));
        assert.notEqual(original.evidence.htmlSha256,learned.evidence.htmlSha256);
        const circular = await api('/api/requests/chat',{request:'Teach data-to-tool lesson circular',teacherId:learnerId});
        assert.match(circular.refusal,/verified original CSV tool/);
        await capture('fresh-batch-verified.png');

        const teacherReceipt = path.join(project,'receipts',original.proof.artifact.id+'-1.json');
        const freshReceipt = path.join(project,'receipts',learned.proof.artifact.id+'-1.json');
        const drift = receipt=>{receipt.checks[1].actual.text='Changed conversion';};
        await refuseChanged(teacherReceipt,drift,()=>verify(learnerId),/original artifact test receipt changed/);
        await refuseChanged(freshReceipt,drift,()=>verify(learnerId),/original artifact test receipt changed/);
        const bank = path.join(project,'request-workbench','data-tool-lessons.json');
        await refuseChanged(bank,data=>{data.data['temperature-batch'].column='inches';},
            ()=>verify(learnerId),/checkpoint integrity mismatch/);

        await restart();
        assert.deepEqual(await verify(learnerId),learned.proof);
        await openReview();
        await page.locator('#requests-result').click();
        await page.locator('#requests-dialog').waitFor({state:'hidden',...flags});
        assert.equal(await canvasResult(),expectedFresh);
        const exported = await call('export',{id:learnerId});
        assert.equal(hash(await fs.readFile(exported.path)),learned.evidence.htmlSha256);
        const offline = await app.evaluate(async ({BrowserWindow},url)=>{
            const tool = new BrowserWindow({show:false,webPreferences:{sandbox:true,contextIsolation:true,nodeIntegration:false}});
            try {
                await tool.loadURL(url);
                return await tool.webContents.executeJavaScript(`(()=>{
                    const input=document.querySelector('#source');
                    input.value='celsius\\n-7.5\\n45.125';
                    input.dispatchEvent(new Event('input'));
                    return {protocol:location.protocol,result:document.querySelector('#result').textContent};
                })()`);
            } finally { tool.destroy(); }
        },pathToFileURL(exported.path).href);
        assert.deepEqual(offline,{protocol:'file:',result:'18.5 °F\n113.225 °F'});

        const inchId = await start('Build a data-to-tool converter from inches to millimeters.');
        const inches = ['-19.8251','0.0001','1000000','-1000000','246.875',...Array.from({length:19},(_,i)=>String(i+1))];
        await attach('boundary-lengths.csv','inches\n'+inches.join('\n'));
        const inchOutput = ['-503.55754 mm','0.00254 mm','25400000 mm','-25400000 mm','6270.625 mm',
            ...Array.from({length:19},(_,i)=>((i+1)*254/10)+' mm')].join('\n');
        const length = await build(inchId,'CSV · Inches to millimetres',inchOutput);
        assert.equal(length.evidence.inputCount,24);
        const reserved = length.evidence.cases[1].input.split('\n').slice(1);
        assert(reserved.length>=3 && reserved.every(value=>!inches.includes(value)));
        assert.deepEqual(pageErrors,[]);

        const result = {passed:true,teacherId,learnerId,original:original.evidence,fresh:learned.evidence,
            length:length.evidence,offline,checks:{restartCount:3,wrongUnitRefused:true,nonFreshRefused:true,
                outOfRangeRefused:true,unverifiedTeacherRefused:true,circularTeacherRefused:true,
                swappedBrowserReceiptRefused:true,teacherReceiptTamperRefused:true,newReceiptTamperRefused:true,
                lessonTamperRefused:true,sourceReviewRetained:true,completedProofRetained:true},screenshots};
        await fs.writeFile(path.join(output,'result.json'),JSON.stringify(result,null,2)+'\n');
        await fs.writeFile(path.join(output,'fresh-browser-receipt.json'),JSON.stringify(learned.receipt,null,2)+'\n');
        await fs.writeFile(path.join(output,'offline-converter.html'),learned.html);
        console.log(JSON.stringify({passed:true,checks:result.checks,temperatureRows:learned.evidence.inputCount,lengthRows:24}));
    } catch (error) {
        await fs.writeFile(path.join(output,'failure.txt'),error.stack+'\n'+await page.locator('body').innerText()).catch(()=>{});
        throw error;
    } finally { await app.close().catch(()=>{}); }
}

main().catch(error=>{console.error(error);process.exitCode=1;});
