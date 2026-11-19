'use strict';
const assert = require('node:assert/strict');
const {createHash} = require('node:crypto');
const fs = require('node:fs/promises');
const path = require('node:path');
const {setTimeout:delay} = require('node:timers/promises');
const {launch} = require('../../workspace-desktop/test/calibration-integration.cjs');

const hash = bytes => createHash('sha256').update(bytes).digest('hex');

async function main() {
    const output = path.resolve(process.argv[2] || 'build/edition192-live');
    const project = path.join(output,'project');
    await fs.mkdir(output);
    await fs.mkdir(project);
    Object.assign(process.env,{FRAYNIX_WORKSPACE_NO_MODELS:'1',FRAYNIX_WORKSPACE_PERSISTENT:'0',
        FRAYNIX_WORKSPACE_JAVA_HEAP_MIB:'768',FRAYNIX_WORKSPACE_JAVA_PROCESSORS:'2'});
    let {app,page} = await launch(project);
    const errors = [];
    const flags = {timeout:70000};
    const screenshots = [];
    const api = (route,args={}) => app.evaluate((_,[route,args])=>global.workspaceTest.api(route,args),[route,args]);
    const read = id => api('/api/agency043/read',{id});
    const verify = id => api('/api/game-tasks115/verify',{id});
    const candidates = async () => (await api('/api/game/blender-list')).candidates;
    const teach = (id,name) => api('/api/game/blender-teach',{id,name});
    const useText = (name,id) => 'Use Blender repair lesson '+name+' on candidate '+id+', then playtest and save the evidence.';
    async function until(check) {
        const deadline = performance.now()+90000;
        do {
            const value = await check();
            if(value)return value;
            await delay(250);
        } while(performance.now()<deadline);
        throw Error('Timed out waiting for the Java task');
    }
    async function start(text) {
        if(text)await page.locator('#goal').fill(text);
        const goal = await page.locator('#goal').inputValue();
        await page.locator('#run').click();
        return until(async()=>(await api('/api/agency043/state')).tasks.find(row=>row.goal===goal));
    }
    async function finish(id,status='completed') {
        const finished = await until(async()=>{
            const row = await read(id);
            if(row.status===status)return row;
            if(['completed','interrupted','cancelled'].includes(row.status))throw Error(row.reason);
            return null;
        });
        assert.equal(finished.turn,0);
        if(status==='completed')assert.equal(finished.outcome,'verified');
        return finished;
    }
    async function mission(id) {
        if(!await page.locator('#mission044').isVisible())await page.locator('#mission044-open').click();
        await page.locator('#mission044-task').selectOption(id);
        await page.waitForFunction(id=>document.querySelector('#mission044').dataset.taskId===id,id,flags);
    }
    async function game() {
        await page.locator('#game-tab').click();
        await page.waitForFunction(()=>document.querySelector('#game-frame').naturalWidth===960,{},flags);
        const choice = page.locator('#game-blender-candidates');
        if(!await choice.evaluate(el=>el.open))await choice.locator('summary').click();
    }
    async function importLevel(file,moves) {
        const full = path.resolve(file);
        await app.evaluate(({dialog},file)=>{
            dialog.showOpenDialog = async()=>({canceled:false,filePaths:[file]});
        },full);
        await page.locator('#game-blender-import').click();
        await page.waitForFunction(count=>document.querySelector('#game-blender-status').textContent
            .includes('Saved '+count+' ground edit(s).'),moves,flags);
        const id = await page.locator('#game-blender-choice').inputValue();
        const candidate = (await candidates()).find(row=>row.id===id);
        assert.equal(candidate.blendSha256,hash(await fs.readFile(full)));
        assert.equal(candidate.moves.length,moves);
        assert.equal((await api('/api/game/blender-state',{id})).status,'ready');
        return candidate;
    }
    async function capture(name) {
        await page.evaluate(()=>new Promise(resolve=>requestAnimationFrame(()=>requestAnimationFrame(resolve))));
        const buffer = Buffer.from(await app.evaluate(()=>global.workspaceTest.capture()),'base64');
        await fs.writeFile(path.join(output,name),buffer);
        screenshots.push({name,sha256:hash(buffer)});
    }
    async function watch() {
        page.on('pageerror',error=>errors.push(error.message));
        await app.evaluate(({BrowserWindow})=>BrowserWindow.getAllWindows().find(w=>w.isVisible()).setSize(1500,1030));
    }
    async function rejectChanged(file, mutate, check, expected) {
        const bytes = await fs.readFile(file);
        try {
            await fs.writeFile(file,mutate(bytes));
            await assert.rejects(check,expected);
        } finally {
            await fs.writeFile(file,bytes);
        }
    }
    try {
        await watch();
        await game();
        const source = await importLevel('docs/readmes/191/wide-gap-course.blend',16);
        await page.locator('#game-blender-repair').click();
        const first = await start();
        await assert.rejects(teach(first.id,'unverified'),/verify the teaching mission|active Java task owns execution/);
        await finish(first.id);
        const teacher = await verify(first.id);
        assert.equal(teacher.audit.diagnosis.edits.length,8);
        assert.equal(teacher.display.checks.length,3);
        await mission(first.id);
        const save = page.locator('#mission-blender-lesson-save');
        await save.waitFor({state:'visible',...flags});
        await page.locator('#mission-blender-lesson-name').fill('restore-ground');
        await save.click();
        await page.waitForFunction(()=>document.querySelector('#mission-blender-lesson-status').textContent
            .includes('Saved restore-ground.'),{},flags);
        await capture('teach-checked-repair.png');
        const lesson = (await api('/api/game/blender-lessons')).lessons[0];
        assert.deepEqual(await teach(first.id,lesson.name),lesson);
        assert.equal(lesson.maxOriginalSpan,2.5);
        assert.equal(lesson.maxEdits,8);
        assert.equal(lesson.maxRemainingSpan,.5);

        const pid = app.process().pid;
        await app.close();
        ({app,page}=await launch(project));
        await watch();
        assert.notEqual(app.process().pid,pid);
        assert.deepEqual((await api('/api/game/blender-lessons')).lessons,[lesson]);
        assert.deepEqual(await verify(first.id),teacher);
        await game();
        const target = await importLevel('docs/readmes/192/narrower-gap-course.blend',12);
        assert.notEqual(target.levelSha256,source.levelSha256);
        const file = path.join(project,'blender-game-candidates',target.id,'level.json');
        const original = hash(await fs.readFile(file));
        await page.locator('#game-blender-lesson').selectOption(lesson.name);
        await page.locator('#game-blender-use-lesson').click();
        assert.equal(await page.locator('#goal').inputValue(),useText(lesson.name,target.id));
        await capture('apply-lesson-to-fresh-level.png');
        const fresh = await start();
        await finish(fresh.id);
        const verified = await verify(fresh.id);
        assert.deepEqual(verified.lesson,lesson);
        assert.deepEqual(verified.audit.lesson,lesson);
        assert.equal(verified.audit.diagnosis.originalSpan,2);
        assert.equal(verified.audit.diagnosis.edits.length,6);
        assert.equal(verified.audit.diagnosis.remainingSpan,.5);
        assert.equal(verified.audit.playtest.trials,2);
        assert.equal(verified.display.checks.length,4);
        assert.notEqual(verified.audit.failureSha256,teacher.audit.failureSha256);
        assert.notEqual(verified.audit.playtest.attemptId,teacher.audit.playtest.attemptId);
        assert.notEqual(verified.audit.candidate.levelSha256,teacher.audit.candidate.levelSha256);
        assert.equal(hash(await fs.readFile(file)),original);
        await mission(fresh.id);
        assert(!await page.locator('#mission-blender-teach').isVisible());
        await assert.rejects(teach(fresh.id,'circular'),/independently verified original repair/);
        await page.locator('#mission044-result').click();
        await page.waitForFunction(()=>document.querySelector('#test-badge').textContent==='TESTS PASSED',{},flags);
        await capture('fresh-level-verified-report.png');

        const receiptFile = path.join(project,'receipts',teacher.report.id+'-1.json');
        await rejectChanged(receiptFile,bytes=>{
            const saved = JSON.parse(bytes);
            saved.checks[0].actual.text='Altered heading';
            return JSON.stringify(saved);
        },()=>verify(fresh.id),/artifact test receipt changed|report checks changed/);
        const lessonFile = path.join(project,'blender-repair-lessons',lesson.name+'.json');
        await rejectChanged(lessonFile,bytes=>{
            const saved = JSON.parse(bytes);
            saved.data.maxEdits=20;
            return JSON.stringify(saved);
        },()=>verify(fresh.id),/checkpoint integrity mismatch/);
        assert.deepEqual(await verify(fresh.id),verified);

        const before = (await candidates()).length;
        const duplicate = await start(useText(lesson.name,source.id));
        const refused = await finish(duplicate.id,'interrupted');
        assert.match(refused.reason,/different level from the teaching example/);
        assert.equal((await candidates()).length,before);
        await api('/api/agency043/control',{id:duplicate.id,action:'cancel'});
        const comparison = await start('Diagnose and repair Blender candidate '+target.id+', then playtest and save the evidence.');
        await finish(comparison.id);
        const independent = await verify(comparison.id);
        assert.deepEqual(independent.audit.diagnosis,verified.audit.diagnosis);
        const narrowLesson = await teach(comparison.id,'smaller-gap');
        assert.equal(narrowLesson.maxEdits,6);
        const beforeLimit = (await candidates()).length;
        const wider = await start(useText(narrowLesson.name,source.id));
        const outside = await finish(wider.id,'interrupted');
        assert.match(outside.reason,/exceeds the conditions verified by this lesson/);
        assert.equal((await candidates()).length,beforeLimit);
        assert.deepEqual(errors,[]);
        const artifact = await api('/api/artifact/read',{id:verified.report.id});
        const result = {passed:true,sourceBlendSha256:source.blendSha256,freshBlendSha256:target.blendSha256,lesson,
            teacherAudit:teacher.audit,audit:verified.audit,teacherReport:teacher.report,report:verified.report,
            checks:{restartRetainedLesson:true,prematureTeachRefused:true,recursiveTeachRefused:true,
                teacherReceiptTamperRefused:true,lessonRecordTamperRefused:true,originalLevelRefused:true,
                beyondLessonLimitRefused:true,sourceBytesPreserved:true,teacherBrowserChecks:3,freshBrowserChecks:4},
            screenshots};
        await fs.writeFile(path.join(output,'repair-evidence.json'),artifact.contents['repair-evidence.json']);
        await fs.writeFile(path.join(output,'result.json'),JSON.stringify(result,null,2)+'\n');
        console.log(JSON.stringify({passed:true,checks:result.checks,ticks:verified.audit.playtest.ticks}));
    } catch(error) {
        const state=await api('/api/agency043/state').catch(()=>null);
        for(const row of state?.tasks||[])console.error(JSON.stringify({goal:row.goal,status:row.status,reason:row.reason}));
        console.error(await page.locator('#notice').textContent().catch(()=>null));
        throw error;
    } finally {
        await app.close().catch(()=>{});
    }
}

main().catch(error=>{console.error(error);process.exitCode=1;});
