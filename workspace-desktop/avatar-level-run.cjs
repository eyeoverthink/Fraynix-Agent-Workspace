'use strict';

const assert = require('node:assert/strict');
const {BrowserWindow,session} = require('electron');
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

async function auditOffline(candidate, expectedLevel, inputs, initialPng) {
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
        let observation;
        for (let i=0;i<60;i++) {
            observation = await wc.executeJavaScript(`(() => {
                const canvas = document.querySelector('canvas');
                if (typeof globalThis.fraynixAvatarReplay !== 'function' || !canvas?.dataset.hero) return null;
                const data = document.querySelector('#level-data').content.textContent;
                const heading = document.querySelector('h1').textContent;
                return {data,heading,hero:canvas.dataset.hero,width:canvas.width,height:canvas.height,
                    png:canvas.toDataURL('image/png').split(',')[1]};
            })()`);
            if (observation?.png===initialPng) break;
            await delay(55);
        }
        if (!observation || observation.png!==initialPng || observation.width!==720 || observation.height!==336)
            throw Error('The offline player did not render the same starting frame as the desktop.');
        const embedded = JSON.parse(Buffer.from(observation.data,'base64').toString('utf8'));
        assert.deepEqual(embedded.layout,expectedLevel.layout,'Offline layout differs from the Java level');
        for (const [id,sprite] of Object.entries(expectedLevel.sprites)) {
            if (embedded.sprites[id]?.png!==sprite.png) throw Error('An offline sprite differs from its checked original.');
        }
        if (observation.hero!==expectedLevel.layout.player.name || observation.heading!==expectedLevel.layout.title)
            throw Error('The offline player did not show the expected avatar and title.');
        const witness = await wc.executeJavaScript('globalThis.fraynixAvatarReplay('+JSON.stringify(inputs)+')');
        if (blocked) throw Error('The standalone level attempted an external request.');
        return {witness,frameSha256:sha(Buffer.from(observation.png,'base64'))};
    } finally {
        clearTimeout(navigationTimer);
        if (window && !window.isDestroyed()) window.destroy();
        if (partition) partition.webRequest.onBeforeRequest(null);
        await fs.rm(folder,{recursive:true,force:true,maxRetries:4});
    }
}

function createAvatarLevelRunner({api,replay,render,rendererSha256,progress}) {
    const call=(action,body)=>api('/api/avatar-level/'+action,body);
    async function advance({id,avatarId,release=false}) {
        let mission = await call('read',{id});
        if (mission.stage==='awaiting-image') {
            if (!avatarId) throw Error('Choose an imported portrait before building this level.');
            mission = await call('build',{id,avatarId});
        }
        if (mission.stage==='built') {
            progress('Playtesting the level');
            const {level,candidate,maxTicks} = await call('prepare',{id});
            const {run,baseline} = await planControls(level,maxTicks,replay);
            const inputs = run.rows.slice(1).map(row=>[row[18],row[16]]);
            const {png} = await render(level,run.rows[0]);
            progress('Checking the offline player');
            const {witness,frameSha256} = await auditOffline(candidate,level,inputs,png);
            if (witness.reason!==run.reason || JSON.stringify(witness.rows)!==JSON.stringify(run.rows))
                throw Error('The standalone level played differently from the desktop.');
            const save=record=>api('/api/media-tiles/save-variant',{...record,rendererSha256});
            const saved = await save(run), control = baseline ? await save(baseline) : null;
            mission = await call('verify',{id,runId:saved.id,baselineId:control?.id||'',
                htmlSha256:candidate.sha256,frameSha256,witnessRows:witness.rows});
        }
        if (mission.stage==='verified' && mission.pause && !release) return mission;
        progress('Checking export files');
        return call('finish',{id});
    }
    return {advance};
}

module.exports = {createAvatarLevelRunner};
