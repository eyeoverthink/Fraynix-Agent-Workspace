'use strict';

const assert=require('node:assert/strict');
const {createHash}=require('node:crypto');
const fs=require('node:fs/promises');
const os=require('node:os');
const path=require('node:path');
const {pathToFileURL}=require('node:url');
const {launch}=require('./calibration-integration.cjs');

const sha=bytes=>createHash('sha256').update(bytes).digest('hex');

async function main(){
    process.env.FRAYNIX_UNIFIED_TEST='1';
    process.env.FRAYNIX_WORKSPACE_NO_MODELS='1';
    const flags=process.platform==='linux'?['--no-sandbox','--disable-dev-shm-usage']:[];
    const portrait=path.resolve(process.argv[2]||'docs/readmes/265/sample-portrait.png');
    const out=path.resolve(process.argv[3]||'build/avatar-repair');
    const project=await fs.mkdtemp(path.join(os.tmpdir(),'Fraynix repaired avatar '));
    await fs.mkdir(out,{recursive:true});
    let app,page;
    const errors=[];
    const shots=[];
    async function open(){
        ({app,page}=await launch(project,flags));
        page.on('pageerror',error=>errors.push(error.message));
        await page.locator('#workspace-home h1').waitFor();
    }
    async function close(){
        if(app){await app.close();app=null;}
    }
    const ask=async text=>{await page.locator('#goal').fill(text);await page.locator('#run').click();};
    const api=(route,args={})=>app.evaluate((_,input)=>global.workspaceTest.api(input.route,input.args),{route,args});
    const read=()=>api('/api/avatar-level/read');
    const stage=text=>page.locator('#avatar-level-status').filter({hasText:text}).waitFor({timeout:45000});
    const assistant=text=>page.locator('#messages .assistant').filter({hasText:text}).last().waitFor({timeout:45000});
    const capture=async name=>{
        await page.evaluate(()=>new Promise(resolve=>requestAnimationFrame(()=>requestAnimationFrame(resolve))));
        await fs.writeFile(path.join(out,name),Buffer.from(await app.evaluate(()=>global.workspaceTest.capture()),'base64'));
        shots.push(name);
    };
    const noExport=async id=>assert.rejects(fs.stat(path.join(project,'exports','avatar-level',id,'playable.html')),{code:'ENOENT'});
    async function saveFiles(prefix,mission){
        const files={};
        for(const [kind,receipt] of Object.entries(mission.files)){
            const bytes=await fs.readFile(receipt.path);
            assert.equal(sha(bytes),receipt.sha256);
            const name=prefix+'-'+path.basename(receipt.path);
            await fs.writeFile(path.join(out,name),bytes);
            files[kind]={name,sha256:receipt.sha256};
        }
        return files;
    }

    async function standalone(file,report){
        const url=pathToFileURL(file).href;
        await app.evaluate(async({BrowserWindow},url)=>{
            const window=new BrowserWindow({width:940,height:620,webPreferences:{
                partition:'avatar-repair-proof',sandbox:true,contextIsolation:true,nodeIntegration:false
            }});
            window.webContents.session.webRequest.onBeforeRequest((req,done)=>
                done({cancel:!(/^(file|data):/.test(req.url))}));
            await window.loadURL(url);
        },url);
        const portable=app.windows().find(w=>w.url()===url);
        assert(portable);
        await portable.waitForFunction(()=>typeof globalThis.fraynixAvatarReplay==='function');
        const result=await portable.evaluate(inputs=>globalThis.fraynixAvatarReplay(inputs),
            report.rows.slice(1).map(row=>[row[18],row[16]]));
        assert.deepEqual(result.rows,report.rows);
        await portable.locator('canvas').focus();
        await portable.keyboard.down('ArrowRight');
        try{
            for(const row of report.rows.filter(row=>row[16])){
                await portable.waitForFunction(x=>Number(document.querySelector('canvas').dataset.playerX)>=x-4,row[1]);
                await portable.keyboard.press('Space');
            }
            await portable.waitForFunction(()=>document.querySelector('canvas').dataset.won==='true');
        }finally{await portable.keyboard.up('ArrowRight');}
        assert.equal(Number(await portable.locator('canvas').getAttribute('data-score')),report.score);
        return true;
    }

    try{
        await open();
        await ask('Open Avatar Studio.');
        await page.locator('#avatar-studio:visible #avatar-image:enabled').setInputFiles(portrait);
        await page.locator('#avatar-status').filter({hasText:'Image loaded'}).waitFor();
        await page.locator('#avatar-name').fill('Course Tester');
        const request='Build a playable level from this image with two hurdles, four pickups, staggered spacing and an oversized first hurdle, called “Repair Circuit”. Test it and pause before repair.';
        await ask(request);
        await stage('Failed playtest diagnosed');
        const diagnosed=await read();
        assert.equal(diagnosed.stage,'diagnosed');
        assert.equal(diagnosed.diagnosis.kind,'blocked-hurdle');
        assert.equal(diagnosed.diagnosis.objectId,'hurdle-1');
        assert.equal(diagnosed.diagnosis.before.size,144);
        assert(diagnosed.diagnosis.contacts>=10);
        assert.equal(diagnosed.repairs.length,0);
        await noExport(diagnosed.id);
        await page.locator('#avatar-level-evidence summary').click();
        await capture('diagnosed-course.png');

        const failed=path.join(project,'media','tile-playtests',diagnosed.diagnosis.runId+'.json');
        const original=await fs.readFile(failed);
        await close();
        const altered=JSON.parse(original);
        altered.data.rows[0][1]++;
        await fs.writeFile(failed,JSON.stringify(altered));
        await open();
        await ask('Resume my avatar level.');
        await stage('integrity mismatch');
        assert.equal((await read()).stage,'diagnosed');
        await noExport(diagnosed.id);
        await fs.writeFile(failed,original);
        await page.locator('#avatar-level-resume:enabled').click();
        await assistant('Repair Circuit is ready.');
        const repaired=await read();
        assert.equal(repaired.id,diagnosed.id);
        assert.equal(repaired.stage,'exported');
        assert.equal(repaired.repairs[0].nextLevelId,repaired.levelId);
        assert.equal(repaired.repairs[0].after.size,48);
        assert.equal(repaired.evidence.hurdles,2);
        assert.equal(repaired.evidence.pickups,4);
        assert.equal(repaired.evidence.score,900);
        assert.notEqual(repaired.levelId,diagnosed.levelId);
        const repairedFiles=await saveFiles('repair-circuit',repaired);
        const history=JSON.parse(await fs.readFile(repaired.files.repairs.path,'utf8'));
        assert.equal(history.originalLevelId,diagnosed.levelId);
        assert.equal(history.finalLevelId,repaired.levelId);
        assert.equal(history.history[0].failedPlaytest.reason,'stopped');
        assert.equal(history.history[0].failedPlaytest.id,diagnosed.diagnosis.runId);
        await page.locator('#avatar-level-evidence summary').click();
        await capture('repaired-course.png');
        const run=JSON.parse(await fs.readFile(repaired.files.playtest.path,'utf8'));
        await standalone(path.join(out,repairedFiles.html.name),run);
        await close();

        await open();
        await ask('Resume my avatar level.');
        await assistant('Repair Circuit is ready.');
        const reopened=await read();
        assert.deepEqual(reopened.files,repaired.files);

        await ask('Make a playable relay from my portrait with five pickups including an elevated coin called "Pickup Rescue". Test and export it offline.');
        await assistant('Pickup Rescue is ready.');
        const pickup=await read();
        assert.equal(pickup.stage,'exported');
        const moved=pickup.repairs.find(step=>step.diagnosis.kind==='missed-pickup');
        assert(moved);
        assert.equal(moved.diagnosis.before.y,54);
        assert.equal(moved.after.y,287);
        assert.equal(pickup.evidence.pickups,5);
        assert.equal(pickup.evidence.score,1000);
        const pickupFiles=await saveFiles('pickup-rescue',pickup);

        await ask('Build a playable platformer from my image with three hurdles and six coins, evenly spaced, called "Triple Trail". Playtest and export it offline.');
        await assistant('Triple Trail is ready.');
        const varied=await read();
        assert.equal(varied.stage,'exported');
        assert.equal(varied.evidence.hurdles,3);
        assert.equal(varied.evidence.pickups,6);
        assert.equal(varied.evidence.score,1100);
        const variedFiles=await saveFiles('triple-trail',varied);
        await page.locator('#avatar-level-review').click();
        await page.locator('#tile-status').filter({hasText:'Opened the verified level'}).waitFor();
        await page.locator('#tile-canvas').evaluate(el=>el.scrollIntoView({block:'center'}));
        await capture('triple-trail.png');

        await ask('Build a playable level from my photo with one hurdle and three pickups, an oversized first hurdle, and keep the layout fixed. Test and export it offline.');
        await stage('Needs review. Export is blocked');
        const locked=await read();
        assert.equal(locked.stage,'needs-review');
        assert.equal(locked.diagnosis.kind,'blocked-hurdle');
        assert.equal(locked.refusal,'The requested layout is locked.');
        assert.equal(locked.repairs.length,0);
        await noExport(locked.id);
        await assert.rejects(api('/api/avatar-level/finish',{id:locked.id}),/Complete the playtest/);
        await close();
        await open();
        await ask('Resume my avatar level.');
        await stage('Needs review. Export is blocked');
        assert.equal((await read()).id,locked.id);
        await noExport(locked.id);

        assert.deepEqual(errors,[]);
        const evidence={restartBeforeRepair:true,tamperedFailureRefused:true,exactExportsAfterRestart:true,
            offlineKeyboardWin:true,lockedRefusalAfterRestart:true,shots,
            hurdle:{...repaired.evidence,files:repairedFiles},pickup:{...pickup.evidence,files:pickupFiles},
            varied:{...varied.evidence,files:variedFiles},locked:locked.diagnosis};
        await fs.writeFile(path.join(out,'course-repair-evidence.json'),JSON.stringify(evidence,null,2)+'\n');
        console.log('Four different briefs passed: recorded hurdle and pickup repairs, three-hurdle variety, and locked refusal after restart.');
    }catch(error){
        for(const selector of ['#avatar-level-status','#avatar-status','#notice'])
            console.error(selector,await page?.locator(selector).textContent().catch(()=>null));
        console.error('Latest mission',await read().catch(()=>null),errors);
        throw error;
    }finally{
        await close();
        const resolved=await fs.realpath(project);
        if(path.dirname(resolved)===await fs.realpath(os.tmpdir())&&/^Fraynix repaired avatar [a-z0-9]+$/i.test(path.basename(resolved)))
            await fs.rm(resolved,{recursive:true,force:true,maxRetries:3}).catch(()=>{});
    }
}
main().catch(error=>{console.error(error);process.exitCode=1;});
