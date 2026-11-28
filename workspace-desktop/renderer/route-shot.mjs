import {RouteOverlayEditor} from './route-overlay-editor.mjs';

export const isRouteShotRequest = text => /^(?:please\s+)?(?:make|create|render|film)\b(?=[\s\S]*\b(?:shot|video|clip)\b)(?=[\s\S]*\b(?:approved|verified)\s+(?:[a-z]+(?:[- ][a-z]+){0,3}\s+)?route\b)/i.test(text.trim());

const JPEG = 'data:image/jpeg;base64,';
const field = (name, value, min, max, step, title) =>
    Object.assign(document.createElement('input'), {type:'number', name, value, min, max, step, title, required:true});

export class RouteShotPanel {
    constructor(parent, stage, openTimeline) {
        Object.assign(this, {parent, openTimeline, record:null, take:'annotated', position:0, playToken:0, previewToken:0});
        this.root = document.createElement('details');
        this.root.id = 'route-shot-panel';
        this.root.className = 'spatial-detail';
        this.root.innerHTML = `<summary>Film an approved crate route</summary>
            <div class="spatial-task">
                <button id="shot-build" class="primary">Render approved route</button>
                <button id="shot-latest">Resume saved shot</button>
                <label class="spatial-file">Import portable shot<input id="shot-import" type="file" accept=".json,application/json" aria-label="Import portable shot"></label>
                <button id="shot-replay" disabled>Recheck route and both takes</button>
            </div>
            <p id="shot-status" class="spatial-note" role="status">Approve a multi-segment Loading Aisle move to use it as a shot.</p>
            <div id="shot-editor" hidden>
                <div class="route-shot-viewport">
                    <img id="shot-preview" alt="Verified loading-bay shot frame" width="640" height="360">
                </div>
                <div class="spatial-task">
                    <button id="shot-play" type="button">Play 6 seconds</button>
                    <label>Take<select id="shot-take"><option value="annotated">Annotated</option><option value="clean">Clean</option></select></label>
                    <label>Frame<input id="shot-scrub" type="range" min="0" max="143" value="0"></label>
                    <output id="shot-time" for="shot-scrub"></output>
                </div>
                <div class="spatial-task">
                    <button id="shot-before">Before shelf</button>
                    <button id="shot-hidden">Behind shelf</button>
                    <button id="shot-return">Visible again</button>
                </div>
                <p class="spatial-note" id="shot-evidence"></p>
                <details class="spatial-detail">
                    <summary>Edit camera keyframes</summary>
                    <form id="shot-keys">
                        <p class="spatial-note">Camera offsets are relative to the crate in the loading bay. The shot follows the approved waypoints at a steady rate.</p>
                        <div class="route-shot-table"><table>
                            <thead><tr><th scope="col">Frame</th><th scope="col">Across · m</th><th scope="col">Height · m</th>
                                <th scope="col">Forward · m</th><th scope="col">FOV · °</th><th scope="col">Edit</th></tr></thead>
                            <tbody id="shot-key-rows"></tbody>
                        </table></div>
                        <div class="spatial-task">
                            <button type="button" id="shot-add-key">Add key</button>
                            <button type="button" id="shot-reset">Reset draft</button>
                            <button class="primary" id="shot-save-keys">Render new camera revision</button>
                        </div>
                    </form>
                </details>
                <div class="spatial-task" id="shot-exports">
                    <button id="shot-edit-timeline">Add both takes to Media Timeline</button>
                    <button id="shot-export-clean">Export clean AVI</button>
                    <button id="shot-export-annotated">Export annotated AVI</button>
                    <button id="shot-export-json">Export camera and evidence JSON</button>
                    <button id="shot-export-bundle">Export portable shot</button>
                </div>
            </div>`;
        stage.before(this.root);
        this.overlayEditor = new RouteOverlayEditor(this.find('exports'),overlays=>this.handle(()=>
            this.build(this.record.definition.routeId,this.record.definition.cameraKeys,overlays)));
        this.find('import').onchange = () => {
            const input = this.find('import');
            const file = input.files[0];
            input.value = '';
            if (!file) return;
            this.handle(async () => {
                if (file.size>10_000_000) throw Error('Choose a portable shot no larger than 10 MB.');
                this.stop();
                this.status('Checking package bytes, native route and every rendered frame…');
                const record = await parent.call('shot-import',{text:await file.text()});
                await this.present(record);
                this.status('Imported and verified the portable shot and both 144-frame takes.');
            });
        };
        this.root.ontoggle = () => { if (!this.root.open) this.stop(); };
        this.find('build').onclick = () => this.fromChat(false);
        this.find('latest').onclick = () => this.handle(async () => {
            const record = await parent.call('shot-latest');
            if (!record.id) throw Error('There is no saved route shot yet.');
            await this.present(record);
            this.status('Saved camera track and both verified takes reopened.');
        });
        this.find('replay').onclick = () => this.handle(async () => {
            this.stop();
            this.status('Replaying native route checks and regenerating every video frame…');
            const report = await parent.call('shot-replay',{id:this.record.id});
            this.status('Replayed the native route and matched all '+report.audit.frameCount+' frames in each take.');
        });
        this.find('play').onclick = () => {
            if (this.playing) this.stop();
            else this.play().catch(error => this.status(error.message));
        };
        this.find('take').onchange = () => {
            this.stop();
            this.take = this.find('take').value;
            this.scrub(this.position);
        };
        this.find('scrub').oninput = () => {
            this.stop();
            this.scrub(Number(this.find('scrub').value));
        };
        for (const [id,key] of [['before','firstVisibleFrame'],['hidden','shelfHiddenFrame'],['return','returnedFrame']]) {
            this.find(id).onclick = () => {
                this.stop();
                this.scrub(this.record.audit[key]);
            };
        }
        this.find('keys').onsubmit = event => {
            event.preventDefault();
            const cameraKeys = this.readKeys();
            this.handle(() => this.build(this.record.definition.routeId,cameraKeys,this.record.definition.overlays));
        };
        this.find('add-key').onclick = () => {
            if (!this.find('keys').reportValidity()) return;
            const keys = this.readKeys();
            if (keys.length>=8) return;
            let insert = 1;
            for (let i=2;i<keys.length;i++) if (keys[i].frame-keys[i-1].frame>keys[insert].frame-keys[insert-1].frame) insert=i;
            const a = keys[insert-1], b = keys[insert];
            if (b.frame-a.frame<2) return;
            keys.splice(insert,0,{frame:Math.floor((a.frame+b.frame)/2),offsetM:a.offsetM.map((x,i)=>(x+b.offsetM[i])/2),fovDeg:(a.fovDeg+b.fovDeg)/2});
            this.drawKeys(keys);
        };
        this.find('reset').onclick = () => this.drawKeys(this.record.definition.cameraKeys);
        this.find('edit-timeline').onclick = () => this.handle(async () => {
            this.stop();
            await this.openTimeline(this.record.id);
        });
        for (const take of ['clean','annotated','json','bundle']) this.find('export-'+take).onclick = () => this.handle(async () => {
            const result = await parent.call('shot-export',{id:this.record.id,take});
            const label = {json:'camera JSON',bundle:'portable shot',clean:'clean AVI',annotated:'annotated AVI'}[take];
            this.status(result.path?'Saved '+label+' to '+result.path:'Export cancelled.');
        });
    }

    find(id) { return this.root.querySelector('#shot-'+id); }
    status(message) { this.find('status').textContent=message; }

    handle(work) {
        return this.parent.action(async () => {
            try { await work(); }
            catch (error) { this.status(error.message); this.parent.notice(error.message); }
        });
    }

    fromChat(announce=true) {
        this.parent.activate('spatial');
        this.root.open = true;
        return this.handle(async () => {
            let route = this.parent.loadingAisle.record;
            if (!route) route = await this.parent.call('aisle-latest');
            const active = this.parent.scene?.id;
            if (route?.status!=='verified' || !route.review?.route ||
                (active && active!==route.review.parentId && active!==route.sceneId) ||
                this.parent.worldMode || this.parent.poseMode) {
                const message = 'Open an approved multi-segment loading-bay route in Spatial, then ask for a shot again.';
                if (announce) this.parent.announce(message);
                throw Error(message);
            }
            await this.build(route.id);
            if (announce) this.parent.announce('I rendered clean and annotated six-second takes from the approved route. '
                +this.summary()+' Edit the camera, add the takes to Media Timeline, or export them from Spatial.');
        });
    }

    async build(id,cameraKeys,overlays) {
        this.stop();
        this.status('Checking the route and rendering both 144-frame takes…');
        const input = {routeId:id,...(cameraKeys?{cameraKeys}:{}),...(overlays?{overlays}:{})};
        await this.present(await this.parent.call('shot-build',input));
        this.status('Verified both six-second takes. '+this.summary());
    }

    summary() {
        const audit = this.record.audit;
        return audit.occlusionDemonstrated
            ? 'The label disappeared behind a protected shelf and returned.'
            : 'This camera revision has no verified shelf disappearance and return.';
    }

    async present(record) {
        this.stop();
        this.record = record;
        this.root.open = true;
        this.find('editor').hidden=false;
        this.find('replay').disabled=false;
        const audit = record.audit;
        this.find('before').disabled=audit.firstVisibleFrame<0;
        this.find('hidden').disabled=audit.shelfHiddenFrame<0;
        this.find('return').disabled=audit.returnedFrame<0;
        this.drawKeys(record.definition.cameraKeys);
        this.overlayEditor.load(record.definition.overlays ?? await this.parent.call('shot-overlay-defaults',{id:record.id}));
        this.find('export-bundle').disabled=!record.definition.overlays;
        await this.preview(0);
        this.root.scrollIntoView({block:'start'});
    }

    readKeys() {
        return [...this.find('key-rows').rows].map(row => {
            const read = name => row.querySelector('[name="'+name+'"]').valueAsNumber;
            return {frame:read('frame'),offsetM:['across','height','forward'].map(read),fovDeg:read('fov')};
        });
    }

    drawKeys(keys) {
        const body = this.find('key-rows');
        body.replaceChildren();
        for (let i=0;i<keys.length;i++) {
            const key=keys[i], endpoint=i===0||i===keys.length-1;
            const row = body.insertRow();
            const fields = [
                field('frame',key.frame,0,143,1,'Camera key frame'),
                field('across',key.offsetM[0],-9,9,'any','Across the bay in metres'),
                field('height',key.offsetM[1],.35,9,'any','Height above the crate in metres'),
                field('forward',key.offsetM[2],-9,9,'any','Forward in metres'),
                field('fov',key.fovDeg,25,80,'any','Field of view in degrees'),
            ];
            fields[0].readOnly = endpoint;
            for (const input of fields) {
                input.setAttribute('aria-label',input.title+' · key '+(i+1));
                row.insertCell().append(input);
            }
            const cell = row.insertCell();
            if (!endpoint) {
                const remove = Object.assign(document.createElement('button'),{type:'button',textContent:'Remove'});
                remove.onclick=()=>this.drawKeys(this.readKeys().filter((_,index)=>index!==i));
                cell.append(remove);
            }
        }
        this.find('add-key').disabled=keys.length>=8;
    }

    scrub(frame) { this.preview(frame).catch(error => this.status(error.message)); }

    async preview(frame) {
        const token = ++this.previewToken;
        this.position = frame;
        const value = await this.parent.call('shot-frame',{id:this.record.id,take:this.take,frame});
        if (token===this.previewToken) this.paint(frame,value.base64);
    }

    paint(frame,base64) {
        this.position = frame;
        const {audit,definition} = this.record, row = audit.frames[frame];
        this.find('preview').src = JPEG+base64;
        this.find('preview').dataset.frame = frame;
        this.find('scrub').value=frame;
        this.find('time').textContent = (frame/definition.fps).toFixed(2)+' s · '+(frame+1)+' / '+audit.frameCount;
        const evidence = row.labelEnabled===false?'Label disabled':row.fullyHiddenByShelf?'Label fully behind '+row.centerOccluder:
            row.centerVisible?'Label visible':'Label center blocked by '+row.centerOccluder;
        const line = row.routePixels===undefined?'':' · '+row.routePixels+' route pixels';
        this.find('evidence').textContent = evidence+' · '+row.labelPixels+' label pixels'+line;
    }

    stop() {
        this.playToken++;
        this.playing=false;
        this.find('play').textContent='Play 6 seconds';
    }

    async play() {
        const token=++this.playToken;
        this.previewToken++;
        this.playing=true;
        this.find('play').textContent='Stop';
        try {
            const result = await this.parent.call('shot-playback',{id:this.record.id,take:this.take});
            if (token!==this.playToken) return;
            const start=performance.now(), last=result.frames.length-1, fps=this.record.definition.fps;
            let prior=-1;
            const tick = now => {
                if (token!==this.playToken) return;
                if (!this.root.open || !this.parent.visible) {
                    this.stop();
                    return;
                }
                const frame=Math.min(last,Math.floor((now-start)*fps/1000));
                if (frame!==prior) {
                    prior=frame;
                    this.paint(frame,result.frames[frame]);
                }
                if (now-start < result.frames.length*1000/fps) requestAnimationFrame(tick);
                else this.stop();
            };
            requestAnimationFrame(tick);
        } catch (error) {
            if (token===this.playToken) {
                this.stop();
                throw error;
            }
        }
    }

    controls(blocked) {
        this.find('build').disabled=Boolean(blocked||this.parent.busy);
    }
}
