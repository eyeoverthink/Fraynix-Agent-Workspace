'use strict';
const assert=require('node:assert/strict');
const fs=require('node:fs/promises');
const os=require('node:os');
const path=require('node:path');
const {launch}=require('./calibration-integration.cjs');
const {fixture,environmentGraphics}=require('./environment-integration.cjs');

const world=[
    [-1.5,.3,1],[1.5,.3,1],[-1.1,2,1],[1.25,2.15,.6],[-.6,1,.05],[.6,1.9,.05],[-.55,2.1,-.1],
    [.65,.5,-.35],[-1.3,1.2,-.15]
];
const projected=([x,y,z])=>[320+275*x/(3.5-z),180+275*(1.35-y)/(3.5-z)];
const marks=world.map((worldM,i)=>({name:'Studio marker '+(i+1),worldM,pixel:projected(worldM)}));
const setup={
    fit:marks.slice(0,7),heldOut:marks.slice(7),deskM:[.85,.72,1.05],
    doorway:{fromM:[0,1.2,1.8],toM:[0,1.2,-1],halfWidthM:.35},
    availability:{windowStart:'2034-11-27T17:00:00Z',windowEnd:'2034-11-27T21:00:00Z',
        capturedAt:'2034-11-26T23:25:00Z',timezone:'America/Los_Angeles',source:'Public test calendar',
        busy:[{start:'2034-11-27T17:00:00Z',end:'2034-11-27T17:23:00Z'},
            {start:'2034-11-27T17:30:00Z',end:'2034-11-27T18:00:00Z'}]}
};

async function main() {
    process.env.FRAYNIX_WORKSPACE_NO_MODELS='1';
    const out=path.resolve(process.argv[2]||'build/desk-recording-test');
    await fs.mkdir(out,{recursive:true});
    const project=await fs.mkdtemp(path.join(os.tmpdir(),'Fraynix desk '));
    let app,page;
    const launchDesk=async()=>{
        ({app,page}=await launch(project,environmentGraphics()));
        await page.locator('#environment-tab').click();
        await page.locator('#environment-desk > summary').click();
    };
    const status=text=>page.waitForFunction(
        text=>document.querySelector('#desk-status').textContent.includes(text),text,{timeout:20000});
    const api=(operation,args={})=>app.evaluate((_,r)=>global.workspaceTest.api('/api/environment/'+r.operation,r.args),{operation,args});
    const capture=async name=>{
        await page.evaluate(()=>new Promise(resolve=>requestAnimationFrame(()=>requestAnimationFrame(resolve))));
        await fs.writeFile(path.join(out,name),Buffer.from(await app.evaluate(()=>global.workspaceTest.capture()),'base64'));
    };
    const download=async(format)=>{
        const prefix='Saved '+format.toUpperCase()+' to ';
        await page.locator('#desk-'+format).click();
        await status(prefix);
        return fs.readFile((await page.locator('#desk-status').textContent()).slice(prefix.length));
    };
    try{
        const {document,encode}=await fixture();
        document.nodes[9].translation=[.60,.35,1.05];
        document.nodes[9].scale=[.6,.7,.5];
        document.nodes[9].name='Recording desk';
        for(let i=0;i<world.length;i++){
            document.nodes[0].children.push(document.nodes.length);
            document.nodes.push({name:marks[i].name,translation:world[i]});
        }
        const room=path.join(out,'recording-studio.glb');
        await fs.writeFile(room,encode(document));
        await launchDesk();
        await page.locator('#environment-file').setInputFiles(room);
        await page.waitForFunction(()=>document.querySelector('#environment-status').textContent.includes('20 objects'));
        await page.locator('#desk-footage').setInputFiles(path.join(__dirname,'fixtures','desk-recording','studio-observation.webm'));
        await page.locator('#desk-anchor-sample').fill('8');
        await page.locator('#desk-anchor-frame').click();
        await status('640 × 360');
        const image=await page.locator('#desk-frame').evaluate(async img=>{
            await img.decode();
            const canvas=document.createElement('canvas');
            canvas.width=img.naturalWidth;canvas.height=img.naturalHeight;
            const ctx=canvas.getContext('2d');
            ctx.drawImage(img,0,0);
            return [canvas.width,canvas.height,Array.from(ctx.getImageData(155,295,1,1).data)];
        });
        assert.deepEqual(image.slice(0,2),[640,360]);
        assert(image[2][0]>180 && image[2][1]>125 && image[2][2]<130,'visible mark on decoded footage');

        // Change a witness only. The other points and exact clip remain the same.
        const shifted=structuredClone(setup);
        shifted.heldOut[0].pixel[0]+=16;
        await page.locator('#desk-setup').fill(JSON.stringify(shifted));
        await page.locator('#desk-start').click();
        await status('stage · prepared');
        await page.locator('#desk-advance').click();
        await status('Landmark agreement exceeds five pixels');
        assert.equal((await api('desk-read')).stage,'prepared');
        await page.locator('#desk-footage-only').click();
        await status('Camera anchor unresolved.');
        const partial=await api('desk-read');
        assert.equal(partial.stage,'draft-anchor-unresolved');
        assert.equal(partial.anchor,undefined);
        assert.equal(partial.recording.anchorFrame,null);
        assert.equal(partial.preview.outputFrames,240);
        await page.locator('#desk-inputs summary').click();

        await page.locator('#desk-setup').fill(JSON.stringify(setup));
        await page.locator('#desk-start').click();
        await status('stage · prepared');
        await page.locator('#desk-advance').click();
        await status('stage · anchored');
        const anchored=await api('desk-read');
        assert(anchored.anchor.cameraM.every((value,i)=>Math.abs(value-[0,1.35,3.5][i])<1e-5));
        assert(anchored.anchor.heldOut.maxPx<.0001);
        assert.equal(anchored.anchor.doorwayProbes,3);
        assert.equal(anchored.anchor.anchorSample,8);
        assert.equal(anchored.anchor.anchorTimeS,1);
        assert.match(anchored.anchor.anchorFrameSha256,/^[a-f0-9]{64}$/);
        await capture('anchored.png');
        await app.close();

        await launchDesk();
        await status('stage · anchored');
        await page.locator('#goal').fill('Resume the desk recording mission');
        await page.locator('#run').click();
        await status('Calendar draft ready for review.');
        const finished=await api('desk-read');
        assert.equal(finished.id,anchored.id);
        assert.equal(finished.recording.frameCount,80);
        assert.equal(finished.preview.durationS,10);
        assert.equal(finished.preview.outputFrames,240);
        assert.equal(finished.draft.start,'2034-11-27T18:00:00Z');
        assert.equal(finished.draft.end,'2034-11-27T18:30:00Z');
        await page.locator('#desk-play').click();
        await status('Preview complete · 10 seconds.');
        await page.locator('#desk-frame').evaluate(img=>img.decode());
        assert.equal(await page.locator('#desk-frame-time').textContent(),'9.875 s');
        await capture('completed.png');

        const avi=await download('avi'), ics=await download('ics');
        assert.equal(avi.subarray(8,12).toString(),'AVI ');
        assert.match(ics.toString(),/DTSTART:20341127T180000Z\r\nDTEND:20341127T183000Z/);
        assert.match(ics.toString(),/STATUS:TENTATIVE/);
        assert.doesNotMatch(ics.toString(),/ATTENDEE|METHOD:REQUEST|ORGANIZER/);
        await fs.writeFile(path.join(out,'desk-preview.avi'),avi);
        await fs.writeFile(path.join(out,'calendar-draft.ics'),ics);
        await app.close();

        await launchDesk();
        await status('Calendar draft ready for review.');
        assert.deepEqual(await api('desk-read'),finished);
        assert((await download('ics')).equals(ics));
        const framePath=path.join(project,'desk-recordings',finished.id,'frame-52.jpg');
        const original=await fs.readFile(framePath);
        const changed=Buffer.from(original);
        changed[changed.length-1]^=1;
        try {
            await fs.writeFile(framePath,changed);
            await assert.rejects(api('desk-artifact',{id:finished.id,format:'ics'}),/bytes changed/);
        } finally {await fs.writeFile(framePath,original);}
        await fs.writeFile(path.join(out,'desk-mission-evidence.json'),JSON.stringify({mission:finished,shiftedWitnessRefused:true,
            resumedStage:'anchored',exactArtifactsAfterRestart:true,tamperRefused:true,partialKeptUnverified:true},null,2)+'\n');
        console.log('Desk mission verified across two restarts. Shifted witness and changed retained media refused.');
    } catch(error) {
        console.error('Desk:',await page?.locator('#desk-status').textContent().catch(()=>null));
        console.error('Environment:',await page?.locator('#environment-status').textContent().catch(()=>null));
        throw error;
    } finally {
        if(app) await app.close().catch(()=>{});
        const absolute=await fs.realpath(project);
        if(path.dirname(absolute)===await fs.realpath(os.tmpdir())&&/^Fraynix desk [a-z0-9]+$/i.test(path.basename(absolute)))
            await fs.rm(absolute,{recursive:true,force:true,maxRetries:3}).catch(()=>{});
    }
}
main().catch(error=>{console.error(error);process.exitCode=1;});
