'use strict';

const assert = require('node:assert/strict');
const {BrowserWindow,nativeImage,session} = require('electron');
const {createHash} = require('node:crypto');
const fs = require('node:fs/promises');
const os = require('node:os');
const path = require('node:path');
const {pathToFileURL} = require('node:url');
const {setTimeout: delay} = require('node:timers/promises');

const sha = bytes => createHash('sha256').update(bytes).digest('hex');

async function planControls(level, maxTicks, replay) {
    const flat = Array.from({length:maxTicks},()=>[1,false]);
    const baseline = await replay(level,flat);
    if (level.layout.course==='relay') {
        if (baseline.reason!=='won') throw Error('The walk-through did not finish the relay.');
        return {run:baseline};
    }
    if (baseline.reason!=='stopped') throw Error('The jump course needs actual hurdles that stop a straight walk.');
    const hurdles = level.layout.objects.filter(item=>item.collider);
    if (hurdles.length!==2) throw Error('The jump course needs two checked hurdles.');
    const contact = hurdles.map(item=>Math.ceil((item.x-48-24)/(215/120)));
    for (const first of [26,32,22,16,35]) {
        for (const second of [26,32,22,16,35]) {
            const proposed = flat.map(row=>row.slice());
            proposed[contact[0]-first][1]=true;
            proposed[contact[1]-second][1]=true;
            const attempt = await replay(level,proposed);
            if (attempt.reason==='won') return {baseline,run:attempt};
        }
    }
    throw Error('No tested jump timing finished the course. The level remains unverified.');
}

function* timings(count, offsets, prefix=[]) {
    if (prefix.length===count) { yield prefix; return; }
    for (const offset of offsets) yield* timings(count,offsets,[...prefix,offset]);
}

async function planDesignedControls(level,maxTicks,replay) {
    const flat=Array.from({length:maxTicks},()=>[1,false]);
    const baseline=await replay(level,flat);
    if (baseline.reason==='won') return {run:baseline};
    const hurdles=level.layout.objects.filter(item=>item.collider);
    let best=baseline;
    const contact=hurdles.map(item=>Math.ceil((item.x-48-24)/(215/120)));
    const closer=(candidate,prior)=>{
        const a=candidate.rows.at(-1), b=prior.rows.at(-1);
        return a[1]>b[1] || a[1]===b[1] && a[15]>b[15];
    };
    if (hurdles.length) {
        for (const leads of timings(hurdles.length,[24,30,18,35])) {
            const inputs=flat.map(row=>row.slice());
            leads.forEach((lead,index)=>{inputs[contact[index]-lead][1]=true;});
            const attempt=await replay(level,inputs);
            if (attempt.reason==='won') return {baseline,run:attempt};
            if (closer(attempt,best)) best=attempt;
        }
    }
    return {failure:best};
}

async function planTraversalControls(level,maxTicks,replay) {
    const flat=Array.from({length:maxTicks},()=>[1,false]);
    const baseline=await replay(level,flat);
    const obstacles=[
        ...level.layout.traversal.gaps.map(gap=>({x:gap.x,takeoff:[9,15,22,29,36]})),
        ...level.layout.objects.filter(item=>item.effect==='hazard'||item.effect==='brick')
            .map(item=>({x:item.x-24,takeoff:[20,27,13,34,41]}))
    ].sort((a,b)=>a.x-b.x);
    const expected=500+level.layout.objects.filter(item=>item.layer==='pickup').length*100;
    const good=attempt=>{
        const row=attempt.rows.at(-1);
        return attempt.reason==='won' && row[12]===0 && row[15]===expected && row[5].startsWith('bank-');
    };
    const distance=attempt=>{
        const reset=attempt.rows.findIndex(row=>row[12]>0);
        const valid=reset<0?attempt.rows:attempt.rows.slice(0,reset);
        const row=valid.at(-1);
        return row[1]+row[15]/100000+(valid.some(tick=>tick[17].some(event=>event[0]==='jump')) ? .001 : 0);
    };
    let best=baseline, bestDistance=distance(baseline);
    for (const leads of timingOptions(obstacles)) {
        const inputs=flat.map(row=>row.slice());
        obstacles.forEach((feature,index)=>{
            const contact=Math.ceil((feature.x-48)/(215/120));
            inputs[Math.max(0,contact-leads[index])][1]=true;
        });
        const attempt=await replay(level,inputs);
        if (good(attempt)) return {baseline,run:attempt};
        const progress=distance(attempt);
        if (progress>bestDistance) {best=attempt;bestDistance=progress;}
    }
    return {failure:best};
}

function* timingOptions(features, prefix=[]) {
    if(prefix.length===features.length){yield prefix;return;}
    for(const lead of features[prefix.length].takeoff)yield* timingOptions(features,[...prefix,lead]);
}

function compareStartFrame(expected, observed) {
    if (expected===observed) return {maxColorDelta:0,changedPixels:0};
    const a=nativeImage.createFromBuffer(Buffer.from(expected,'base64'));
    const b=nativeImage.createFromBuffer(Buffer.from(observed,'base64'));
    if(a.isEmpty()||b.isEmpty()||a.getSize().width!==b.getSize().width||a.getSize().height!==b.getSize().height)return null;
    return comparePixelBuffers(a.toBitmap(),b.toBitmap());
}

function comparePixelBuffers(x,y) {
    return x.length===y.length && x.length%4===0 && x.equals(y) ? {maxColorDelta:0,changedPixels:0} : null;
}

async function auditOffline(candidate, expectedLevel, inputs, initialPng, idlePng) {
    const folder = await fs.mkdtemp(path.join(os.tmpdir(),'fraynix-avatar-audit-'));
    const file = path.join(folder,'playable.html');
    const url = pathToFileURL(file).href;
    const bytes = Buffer.from(candidate.base64,'base64');
    let window, partition, navigationTimer;
    try {
        if (sha(bytes)!==candidate.sha256) throw Error('Candidate HTML bytes changed before testing.');
        await fs.writeFile(file,bytes,{flag:'wx'});
        partition = session.fromPartition('workspace-avatar-level-check');
        let blocked = 0;
        partition.setPermissionRequestHandler((_wc,_permission,callback)=>callback(false));
        partition.setPermissionCheckHandler(()=>false);
        partition.webRequest.onBeforeRequest((request,callback)=>{
            const deny = request.url!==url && !request.url.startsWith('data:image/png;base64,');
            if (deny) blocked++;
            callback({cancel:deny});
        });
        window = new BrowserWindow({show:false,width:850,height:540,webPreferences:{
            session:partition,sandbox:true,contextIsolation:true,nodeIntegration:false,backgroundThrottling:false
        }});
        const wc = window.webContents;
        wc.setWindowOpenHandler(()=>({action:'deny'}));
        wc.on('will-navigate',event=>event.preventDefault());
        await Promise.race([window.loadURL(url),new Promise((_,reject)=>{
            navigationTimer=setTimeout(()=>reject(Error('The standalone level took too long to open.')),10000);
        })]);
        clearTimeout(navigationTimer);
        const needsSettled=initialPng!==idlePng;
        let observation, frameComparison;
        for (let i=0;i<60;i++) {
            observation = await wc.executeJavaScript(`(() => {
                const canvas = document.querySelector('canvas');
                if (typeof globalThis.fraynixAvatarReplay !== 'function' || !canvas?.dataset.hero) return null;
                const data = document.querySelector('#level-data').content.textContent;
                const heading = document.querySelector('h1').textContent;
                return {data,heading,hero:canvas.dataset.hero,width:canvas.width,height:canvas.height,
                    tick:Number(canvas.dataset.tick),png:canvas.toDataURL('image/png').split(',')[1]};
            })()`);
            if (observation?.png && Number.isInteger(observation.tick) && observation.tick>=0) {
                const referenceTick=observation.tick===0?0:3;
                if ((!needsSettled && observation.tick===0) || observation.tick>=3) {
                    const match=compareStartFrame(referenceTick===0?initialPng:idlePng,observation.png);
                    if (match) {
                        frameComparison={...match,referenceTick,observedTick:observation.tick};
                        break;
                    }
                }
            }
            await delay(55);
        }
        if (!observation || !frameComparison || observation.width!==720 || observation.height!==336)
            throw Error('The offline player did not render the same starting or settled idle frame as the desktop.');
        const embedded = JSON.parse(Buffer.from(observation.data,'base64').toString('utf8'));
        assert.deepEqual(embedded.layout,expectedLevel.layout,'Offline layout differs from the Java level');
        for (const [id,sprite] of Object.entries(expectedLevel.sprites)) {
            if (embedded.sprites[id]?.png!==sprite.png) throw Error('An offline sprite differs from its checked original.');
        }
        if (observation.hero!==expectedLevel.layout.player.name || observation.heading!==expectedLevel.layout.title)
            throw Error('The offline player did not show the expected avatar and title.');
        const witness = await wc.executeJavaScript('globalThis.fraynixAvatarReplay('+JSON.stringify(inputs)+')');
        if (blocked) throw Error('The standalone level attempted an external request.');
        return {witness,frameComparison,frameSha256:sha(Buffer.from(observation.png,'base64'))};
    } finally {
        clearTimeout(navigationTimer);
        if (window && !window.isDestroyed()) window.destroy();
        if (partition) partition.webRequest.onBeforeRequest(null);
        await fs.rm(folder,{recursive:true,force:true,maxRetries:4});
    }
}

function createAvatarLevelRunner({api,replay,render,rendererSha256,progress}) {
    const call=(action,body)=>api('/api/avatar-level/'+action,body);
    async function verifyBuilt(mission) {
        const {id}=mission;
        progress('Playtesting the level');
        const {level,candidate,maxTicks}=await call('prepare',{id});
        const save=record=>api('/api/media-tiles/save-variant',{...record,rendererSha256});
        const plan=level.layout.traversal ? planTraversalControls : mission.brief ? planDesignedControls : planControls;
        const planned=await plan(level,maxTicks,replay);
        if (planned.failure) {
            const failed=await save(planned.failure);
            return call('diagnose',{id,runId:failed.id});
        }
        const {run,baseline}=planned;
        const inputs=run.rows.slice(1).map(row=>[row[18],row[16]]);
        const idle=await replay(level,[[0,false],[0,false],[0,false]]);
        const [initial,settled]=await Promise.all([render(level,run.rows[0]),render(level,idle.rows.at(-1))]);
        progress('Checking the offline player');
        const {witness,frameComparison,frameSha256}=await auditOffline(candidate,level,inputs,initial.png,settled.png);
        if (witness.reason!==run.reason || JSON.stringify(witness.rows)!==JSON.stringify(run.rows))
            throw Error('The standalone level played differently from the desktop.');
        const saved=await save(run), control=baseline ? await save(baseline) : null;
        return call('verify',{id,runId:saved.id,baselineId:control?.id||'',
            htmlSha256:candidate.sha256,frameSha256,frameComparison,witnessRows:witness.rows});
    }

    async function advance({id,avatarId,release=false,releaseRepair=false}) {
        let mission=await call('read',{id});
        if (mission.stage==='awaiting-image') {
            if (!avatarId) throw Error('Choose an imported portrait before building this level.');
            mission=await call('build',{id,avatarId});
        }
        while (mission.stage==='built' || mission.stage==='diagnosed') {
            if (mission.stage==='built') {
                mission=await verifyBuilt(mission);
                continue;
            }
            if (mission.brief.pauseRepair && mission.repairs.length===0 && !releaseRepair) return mission;
            progress('Repairing '+mission.diagnosis.objectId+' from the failed playtest');
            mission=await call('repair',{id});
        }
        if (mission.stage==='needs-review') return mission;
        if (mission.stage==='verified' && mission.pause && !release) return mission;
        progress('Checking export files');
        return call('finish',{id});
    }
    return {advance};
}

module.exports = {createAvatarLevelRunner,comparePixelBuffers};
