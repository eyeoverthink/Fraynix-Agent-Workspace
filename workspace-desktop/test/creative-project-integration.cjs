'use strict';

const assert=require('node:assert/strict');
const {createHash}=require('node:crypto');
const fs=require('node:fs/promises');
const os=require('node:os');
const path=require('node:path');
const {launch}=require('./calibration-integration.cjs');

const fixture=path.join(__dirname,'fixtures','timeline-edit');
const sha=bytes=>createHash('sha256').update(bytes).digest('hex');
const phrase='Make another one like the last one, a little quicker, for the family.';
const originalWords='Make a short keepsake for the family.';
const privatePrefix='PRIVATE-256-original-';
const writeJson=(file,value)=>fs.writeFile(file,JSON.stringify(value,null,2)+'\n');

async function main() {
    process.env.FRAYNIX_WORKSPACE_NO_MODELS='1';
    const out=path.resolve(process.argv[2]||'build/creative256');
    const root=await fs.mkdtemp(path.join(os.tmpdir(),'Fraynix creative '));
    const platformArgs=process.platform==='linux'?['--no-sandbox','--disable-dev-shm-usage']:[];
    await fs.mkdir(out,{recursive:true});
    let app,page,project;
    const api=(route,body={})=>app.evaluate((_,{route,body})=>global.workspaceTest.api(route,body),{route,body});
    const timeline=(command,body)=>api('/api/timeline/'+command,body);
    const creative=(command,args={})=>api('/api/creative',{command,args});
    const action=(command,args={})=>creative(command,{project,...args});
    const query=id=>page.locator('#'+id);
    const status=text=>page.waitForFunction(text=>document.querySelector('#creative-status')?.textContent.includes(text),text,{timeout:30000}).catch(async error=>{
        throw Error('Waiting for '+text+'; current message: '+await query('creative-status').textContent().catch(()=>''),{cause:error});
    });
    async function enter() {
        ({app,page}=await launch(root,platformArgs));
        await page.waitForFunction(()=>document.querySelector('#engine-state')?.textContent.includes('0 local models'));
        await query('media-tab').click();
        await query('media-timeline-mode').click();
    }
    async function checkpoint(name) {
        await page.waitForFunction(()=>!document.querySelector('.creative-project').hasAttribute('aria-busy'));
        await page.evaluate(()=>new Promise(resolve=>requestAnimationFrame(()=>requestAnimationFrame(resolve))));
        const image=await app.evaluate(()=>global.workspaceTest.capture());
        await fs.writeFile(path.join(out,name),Buffer.from(image,'base64'));
    }
    const importFixture=async (file,name)=>timeline('import',{filename:name,base64:(await fs.readFile(path.join(fixture,file))).toString('base64')});
    const row=(assetId,lane,start,length)=>({lane,assetId,start,in:0,length,...(lane.startsWith('AUDIO')?{gain:.8}:{})});
    const mediaClip=async (title,angle)=>{
        const scene=await api('/api/scene/save',{title,parameters:{angleDeg:angle,friction:0}});
        const run=await api('/api/experiment/run',{id:scene.id,expectedVersion:scene.version,seconds:.9});
        assert.equal(run.status,'completed');
        const packageResult=await api('/api/motion-media/build',{runId:run.id});
        const attached=(await timeline('attach',{id:packageResult.id})).assets.find(asset=>asset.source.format==='avi');
        await api('/api/experiment/control',{action:'reset'});
        return attached;
    };
    async function saveCut(id,name) {
        const file=await timeline('export',{id,format:'avi'});
        const bytes=Buffer.from(file.base64,'base64');
        assert.equal(sha(bytes),file.sha256);
        await fs.writeFile(path.join(out,name),bytes);
        return file.sha256;
    }
    async function proposeThroughUi({opening,followup,audio,reference,useMemory}) {
        await query('creative-compose').evaluate(details=>{details.open=true;});
        await query('creative-opening').selectOption(opening.id);
        await query('creative-followup').selectOption(followup.id);
        await query('creative-sound').selectOption(audio.id);
        await query('creative-reference').selectOption(reference||'');
        await query('creative-use-memory').setChecked(useMemory);
        await query('creative-request').fill(phrase);
        await query('creative-propose').getByRole('button',{name:'Propose cut'}).click();
    }
    const correction=async (words,button)=>{
        await query('creative-correction').fill(words);
        await query('creative-correct').getByRole('button',{name:button}).click();
        await status('Preview updated');
    };

    try {
        await enter();
        await status('Create a project');
        // Keep the seed master private. Its source names and its redrawn content must not leak.
        const privateImage=await importFixture('overlay.png',privatePrefix+'picture.png');
        const privateSound=await importFixture('cue.wav',privatePrefix+'voice.wav');
        const reference=await timeline('render',{title:'Saturday keepsake · first cut',strips:[
            row(privateImage.id,'PICTURE',0,12),row(privateImage.id,'PICTURE',12,12),row(privateSound.id,'AUDIO1',0,24)
        ]});
        const practiceOpening=await mediaClip('First practice shot',18);
        const practiceFollowup=await mediaClip('First practice ending',34);
        const freshOpening=await mediaClip('New slope footage',26);
        const freshFollowup=await importFixture('two-colors.avi','Two-color closing shot.avi');
        assert.notEqual(practiceOpening.source.sha256,freshOpening.source.sha256);
        await app.close();
        await enter();
        await status('Create a project');
        await query('creative-new').click();
        await query('creative-name').fill('Saturday keepsakes');
        await query('creative-intention').fill(originalWords);
        await query('creative-create').getByRole('button',{name:'Create project'}).click();
        await status('Project created');
        project=await query('creative-project').inputValue();
        assert(project);
        await query('creative-approve-current').click();
        await status('Current timeline cut approved');
        assert.equal((await action('open')).approved[0].id,reference.id);
        await proposeThroughUi({opening:practiceOpening,followup:practiceFollowup,audio:practiceOpening,reference:'',useMemory:true});
        await status('Proposed a verified cut');
        const unapproved=(await action('open')).draft;
        assert.equal(unapproved.openingFrames,10);
        await assert.rejects(action('review-share',{revision:unapproved.revision,audience:'Family'}),/Approve this cut/);
        await correction('Keep the opening slow. Speed up the rest.','Remember for this project');
        let state=await action('open');
        assert.equal(state.choices.length,0,'A proposal alone cannot teach a project');
        assert.deepEqual([state.draft.openingFrames,state.draft.followupFrames],[14,8]);
        await assert.rejects(action('approve-draft',{draft:unapproved.id}),/draft changed/);
        await query('creative-approve-draft').click();
        await status('Cut approved');
        state=await action('open');
        assert.equal(state.choices.at(-1).frames,4);
        const firstExample=state.choices.at(-1).revision;
        const savedIntent='Try the new slope footage in a family cut.';
        await query('creative-next').evaluate(form=>{form.parentElement.open=true;});
        await query('creative-next-text').fill(savedIntent);
        await query('creative-next').getByRole('button',{name:'Save intention'}).click();
        await status('Saved your next intention');
        await app.close();

        await enter();
        await status('Reopened Saturday keepsakes');
        assert.equal(await query('creative-project').inputValue(),project);
        assert((await query('creative-home').textContent()).includes(savedIntent));
        assert((await query('creative-home').textContent()).includes('Longer opening'));
        await checkpoint('project-resumed.png');
        const args={opening:freshOpening,followup:freshFollowup,audio:freshOpening,reference:'',useMemory:false};
        await proposeThroughUi(args);
        await status('Which approved cut');
        await query('creative-reference-cards').getByRole('button',{name:/Saturday keepsake · first cut/}).click();
        assert.equal(await query('creative-reference').inputValue(),reference.id);
        await query('creative-propose').getByRole('button',{name:'Propose cut'}).click();
        await status('Proposed a verified cut');
        const control=(await action('open')).draft;
        assert.equal(control.openingFrames,10);
        assert.equal(control.reference,reference.id);
        assert.equal(control.memory,undefined);
        const controlSha=await saveCut(control.revision,'without-memory.avi');
        await proposeThroughUi({...args,reference:reference.id,useMemory:true});
        await status('Proposed a verified cut');
        const adapted=(await action('open')).draft;
        assert.equal(adapted.openingFrames,14);
        assert.equal(adapted.memory.revision,firstExample);
        assert.equal(adapted.reference,control.reference);
        assert.equal(adapted.request,control.request);
        assert.equal(adapted.openingAsset,control.openingAsset);
        assert.equal(adapted.followupAsset,control.followupAsset);
        const adaptedSha=await saveCut(adapted.revision,'with-memory.avi');
        assert.notEqual(controlSha,adaptedSha);
        await query('creative-preview').click();
        await query('creative-draft-player').getByRole('button',{name:'Play picture and sound'}).click();
        await query('creative-draft-player').getByRole('button',{name:'Play picture and sound'}).waitFor();
        await query('creative-explanation').scrollIntoViewIfNeeded();
        await checkpoint('pacing-review.png');
        await correction('Make the opening shorter.','Just this cut');
        state=await action('open');
        assert.equal(state.draft.openingFrames,6);
        assert.equal(state.choices.at(-1).frames,4);
        await query('creative-approve-draft').click();
        await status('Cut approved');
        await app.close();

        await enter();
        await status('Reopened Saturday keepsakes');
        const rawArgs={opening:freshOpening.id,followup:freshFollowup.id,audio:freshOpening.id,reference:reference.id,
            request:phrase,useMemory:true};
        const conflict=await action('propose',{...rawArgs,request:'Make another for the family or for work.'});
        assert.match(conflict.clarification,/private/);
        const privateFootage=await importFixture('overlay.png','renamed-original.png');
        await assert.rejects(action('propose',{...rawArgs,opening:privateFootage.id}),/distinct bytes/);
        state=await action('propose',rawArgs);
        assert.equal(state.draft.openingFrames,14,'One-off change stays on its cut after restart');
        state=await action('correct',{draft:state.draft.id,request:'Make the opening shorter.',remember:true});
        const pending=state.draft;
        await app.close();
        await enter();
        await status('Reopened Saturday keepsakes');
        assert.deepEqual((await action('open')).draft,pending);
        assert((await query('creative-explanation').textContent()).includes('Remember opening on approval'));
        await query('creative-approve-draft').click();
        await status('Cut approved');
        state=await action('open');
        assert.deepEqual(state.choices.map(choice=>choice.frames),[4,-4]);
        await app.close();

        await enter();
        await status('Reopened Saturday keepsakes');
        state=await action('propose',rawArgs);
        assert.equal(state.draft.openingFrames,6,'A remembered correction changes the next render after restart');
        assert.equal(state.draft.memory.frames,-4);
        const sibling=await creative('create',{name:'Relay trailers',intent:'Keep game trailers brisk on their own terms.'});
        const other=async (command,args={})=>creative(command,{project:sibling.id,...args});
        const siblingReference=await timeline('render',{title:'Relay trailer reference',strips:[
            row(privateImage.id,'PICTURE',0,16),row(privateImage.id,'PICTURE',16,8)
        ]});
        await other('approve-cut',{revision:siblingReference.id});
        await assert.rejects(other('propose',rawArgs),/approved cut from this project/);
        const siblingCut=(await other('propose',{...rawArgs,reference:siblingReference.id})).draft;
        assert.equal(siblingCut.openingFrames,14);
        assert.equal(siblingCut.memory,undefined);
        let undone=await action('undo-choice');
        assert.equal(undone.choices.at(-1).frames,4);
        undone=await action('propose',rawArgs);
        assert.equal(undone.draft.openingFrames,14,'Undo restores immediately verified pacing');
        const familyRevision=adapted.revision;
        await action('approve-cut',{revision:familyRevision});
        assert.deepEqual((await timeline('open',{id:reference.id})).files,reference.files);
        await app.close();

        await enter();
        await status('Reopened Saturday keepsakes');
        const family=await action('review-share',{revision:familyRevision,audience:'Family'});
        await assert.rejects(action('export-share',{review:family.share.id}),/Approve the source list/);
        await assert.rejects(action('approve-share',{review:family.share.id,cleared:[freshOpening.id]}),/Clear each/);
        // Approve the actual picture/audio from the desktop after viewing the exact rendition.
        await query('creative-handoff').evaluate(element=>{element.open=true;});
        await query('creative-share-cut').selectOption(familyRevision);
        await query('creative-begin-share').click();
        await status('Review the actual cut');
        await query('creative-share-preview').click();
        await query('creative-share-details').getByRole('checkbox').first().waitFor();
        const boxes=query('creative-share-details').getByRole('checkbox');
        assert.equal(await boxes.count(),2);
        assert(!(await query('creative-share-details').textContent()).includes(privatePrefix));
        await query('creative-share').scrollIntoViewIfNeeded();
        await checkpoint('family-copy-review.png');
        for (const box of await boxes.all()) await box.check();
        await query('creative-share-details').getByRole('button',{name:'Approve this audience copy'}).click();
        await status('ready to export');
        const ready=(await action('open')).share;
        assert.equal(ready.phase,'prepared');
        await query('creative-audience').selectOption('Public');
        await status('Review the actual cut');
        const changed=(await action('open')).share;
        assert.equal(changed.audience,'Public');
        assert.notEqual(changed.id,ready.id);
        assert.equal(changed.phase,'review');
        await assert.rejects(action('export-share',{review:ready.id}),/audience review changed/);
        await assert.rejects(action('export-share',{review:changed.id}),/Approve the source list/);
        await query('creative-audience').selectOption('Family');
        await status('Review the actual cut');
        const again=(await action('open')).share;
        assert.equal(again.phase,'review');
        await action('approve-share',{review:again.id,cleared:[freshOpening.id,freshFollowup.id]});
        await app.close();

        await enter();
        await status('Reopened Saturday keepsakes');
        assert.equal((await action('open')).share.phase,'prepared');
        await query('creative-share-details').getByRole('button',{name:'Export approved AVI'}).click();
        await status('Exported the approved copy');
        const actual=await action('locate-share',{review:again.id});
        const bytes=await fs.readFile(actual.path);
        assert.equal(path.basename(actual.path),'family-cut.avi');
        assert.deepEqual(await fs.readdir(path.dirname(actual.path)),['family-cut.avi']);
        assert.equal(sha(bytes),adaptedSha);
        assert(!bytes.includes(Buffer.from(privatePrefix)));
        assert(!bytes.includes(Buffer.from(privateImage.source.sha256)));
        await fs.writeFile(path.join(out,'family-cut.avi'),bytes);
        await checkpoint('family-copy-exported.png');
        assert.deepEqual((await timeline('open',{id:reference.id})).files,reference.files);

        const sourceFile=path.join(root,'timeline','assets',freshFollowup.id+'.source');
        const sourceBytes=await fs.readFile(sourceFile);
        const modified=Buffer.from(sourceBytes);
        modified[modified.length-6]^=1;
        try {
            await fs.writeFile(sourceFile,modified);
            await assert.rejects(action('export-share',{review:again.id}),/differs from its verified bytes/);
        } finally { await fs.writeFile(sourceFile,sourceBytes); }
        await action('export-share',{review:again.id});
        assert.equal(sha(await fs.readFile(actual.path)),adaptedSha);

        const freshSound=await api('/api/motion-media/export',{id:freshOpening.source.origin.recipeId,format:'wav'});
        await fs.writeFile(path.join(out,'fresh-source.wav'),Buffer.from(freshSound.base64,'base64'));
        await saveCut(reference.id,'private-control.avi');
        const omitted=Buffer.from((await timeline('export',{id:reference.id,format:'wav'})).base64,'base64');
        await fs.writeFile(path.join(out,'private-control.wav'),omitted);
        await writeJson(path.join(out,'creative-evidence.json'),{
            offline:true,modelCount:0,reference:reference.id,learnedFrom:firstExample,
            compared:{controlOpening:control.openingFrames,controlSha,rememberedOpening:adapted.openingFrames,adaptedSha},
            oneOffOpening:6,afterOneOffRestart:14,afterRememberedCorrectionRestart:6,afterUndo:14,
            sibling:{opening:siblingCut.openingFrames,usedMemory:false},audience:'Family',
            exported:{name:'family-cut.avi',bytes:bytes.length,sha256:adaptedSha,files:1},
            refused:['unapproved cut','stale draft','ambiguous audience','same bytes under a new filename',
                'reference from another project','incomplete source clearance','changed audience approval','changed source bytes'],
            masterReopened:true,pendingDraftReopened:true
        });
        console.log('Verified project continuity, scoped pacing, independent audience approval and exact local media export.');
    } finally {
        if (app) await app.close().catch(()=>{});
        const log=await fs.readFile(path.join(root,'test-desktop-process.log')).catch(()=>null);
        if (log) await fs.writeFile(path.join(out,'desktop-process.log'),log);
        // The project is a disposable test workspace under the explicitly created temp directory.
        const temporary=await fs.realpath(os.tmpdir());
        const testRoot=await fs.realpath(root);
        if (path.dirname(testRoot)===temporary && /^Fraynix creative [a-z0-9]+$/i.test(path.basename(testRoot)))
            await fs.rm(testRoot,{recursive:true,force:true,maxRetries:3}).catch(()=>{});
    }
}

if (require.main===module) main().catch(error=>{console.error(error);process.exitCode=1;});
