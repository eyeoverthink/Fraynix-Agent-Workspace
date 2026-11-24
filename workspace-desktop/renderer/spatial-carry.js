const get = id => document.getElementById(id);

export function isSpatialCarryRequest(text) {
  return /^(?:(?:can|could|would) you )?(?:please )?(?:carry|take|bring|move) (?:the )?selected (?:prop|object|key) through (?:the )?(?:doorway|door|gate)[.!?]?$/i.test(text.trim());
}

export class SpatialCarryPanel {
  constructor(spatial) {
    this.spatial = spatial;
    const panel = document.createElement('details');
    panel.className = 'spatial-detail';
    panel.id = 'spatial-carry';
    panel.innerHTML = `
      <summary>Carry through a doorway</summary>
      <div class="spatial-task">
        <label>X move · m <input id="carry-x" type="number" value="3.3" min="-6" max="6" step="0.01"></label>
        <label>Z move · m <input id="carry-z" type="number" value="0" min="-6" max="6" step="0.01"></label>
        <button id="carry-review">Review carry</button>
        <button id="carry-reopen">Reopen last review</button>
      </div>
      <p id="carry-result" class="spatial-note" role="status">Choose a moving prop and review a horizontal carry.</p>
      <div class="spatial-task">
        <label>Show route <select id="carry-route" disabled></select></label>
        <button id="carry-approve" class="primary" disabled>Approve and replay</button>
        <button id="carry-play" disabled>Play trace</button>
        <label>Trace frame <input id="carry-frame" type="range" min="0" max="0" value="0" disabled></label>
      </div>
      <p id="carry-evidence" class="spatial-note"></p>`;
    get('spatial-stage').before(panel);
    get('carry-review').onclick = () => spatial.action(() => this.review());
    get('carry-reopen').onclick = () => spatial.action(() => this.reopen());
    get('carry-approve').onclick = () => spatial.action(() => this.approve());
    get('carry-route').onchange = () => this.showRoute();
    get('carry-frame').oninput = () => this.showFrame();
    get('carry-play').onclick = () => this.togglePlayback();
    panel.addEventListener('toggle', () => { if (!panel.open) this.stopPlayback(); });
    for (const id of ['carry-x','carry-z']) get(id).oninput = () => this.clear();
    this.controls();
  }

  clear() {
    this.stopPlayback();
    this.record = null;
    this.spatial.showCarryFrame();
    get('carry-route').replaceChildren();
    get('carry-result').textContent = 'Choose a moving prop and review a horizontal carry.';
    get('carry-evidence').textContent = '';
    delete get('carry-result').dataset.status;
    this.controls();
  }

  hasImportedSource() {
    const spatial = this.spatial;
    return Boolean(spatial.scene && spatial.object?.massKg && !spatial.worldMode
      && !spatial.poseMode && !spatial.evaluationMode && !spatial.linkedMode);
  }

  matches(request) {
    return this.hasImportedSource() && this.spatial.scene.id === request.sceneId
      && this.spatial.object.id === request.objectId;
  }

  controls() {
    get('carry-review').disabled = this.busy;
    get('carry-reopen').disabled = this.busy;
    get('carry-approve').disabled = this.busy || this.record?.status !== 'review'
      || !this.matches(this.record.proposal.request);
    get('carry-route').disabled = !this.record;
    get('carry-frame').disabled = !this.record;
    get('carry-play').disabled = !this.record;
    for (const id of ['carry-x','carry-z']) get(id).disabled = this.busy;
  }

  async run(work) {
    if (this.busy) return;
    this.busy = true;
    this.controls();
    try { await work(); }
    catch (error) {
      get('carry-result').textContent = error.message;
      throw error;
    } finally {
      this.busy = false;
      this.controls();
    }
  }

  async review(fromChat = false) {
    const spatial = this.spatial;
    if (!spatial.visible) await spatial.open();
    if (!this.hasImportedSource()) throw Error("Select a dynamic prop in an imported scene first.");
    get('spatial-carry').open = true;
    if (fromChat) get('spatial-carry').scrollIntoView({block:'start'});
    const inputs = {...spatial.binding(), displacementM:[Number(get('carry-x').value),0,Number(get('carry-z').value)]};
    await this.run(async () => {
      this.clear();
      get('carry-result').textContent = 'Testing straight and quarter-turn routes in isolated physics worlds…';
      const record = await spatial.call('carry-review',inputs);
      if (this.matches(inputs)) {
        this.show(record);
        if (fromChat) this.spatial.announce(record.status === 'refused'
          ? 'I tested three routes and each failed. Open the route trace to inspect the first contact or clearance warning.'
          : 'The '+record.proposal.forecast.selectedRoute+' route reaches the target in simulation. Review the carry and approve a fresh replay in Spatial.');
      }
    });
  }

  async reopen() {
    await this.run(async () => {
      const record = await this.spatial.call('carry-latest');
      if (!record.id) {
        get('carry-result').textContent = 'There is no saved carry review in this project.';
        return;
      }
      await this.spatial.open(record.proposal.request.sceneId);
      await this.spatial.selectObject(record.proposal.request.objectId);
      get('spatial-carry').open = true;
      this.show(record);
    });
  }

  async approve() {
    const {id,approvalSha256,proposal} = this.record;
    if (!this.matches(proposal.request)) throw Error("Reopen the saved review to return to its original prop.");
    await this.run(async () => {
      get('carry-result').textContent = 'Replaying the approved route in a fresh physics world…';
      const result = await this.spatial.call('carry-approve',{sceneId:proposal.request.sceneId,id,approvalSha256});
      if (this.record?.id === id) {
        this.show(result);
        this.spatial.announce('The fresh simulation matched the reviewed carry. The saved trace shows the full route and final pose.');
      }
    });
  }

  show(record) {
    this.record = record;
    const {forecast,request} = record.proposal;
    get('carry-x').value = request.displacementM[0];
    get('carry-z').value = request.displacementM[2];
    const status = get('carry-result');
    status.dataset.status = record.status;
    status.textContent = record.status === 'refused' ? 'Carry refused · all three routes failed the checks.'
      : record.status === 'completed' ? 'Carry verified · the fresh simulation matches the reviewed route.'
      : 'Ready to approve · '+forecast.selectedRoute+' reaches the reviewed pose.';
    const options = forecast.trials.map(trial => new Option(trial.route+(trial.passed?' · passed':' · refused'),trial.route));
    get('carry-route').replaceChildren(...options);
    get('carry-route').value = forecast.selectedRoute || forecast.trials[0].route;
    this.showRoute();
    this.controls();
  }

  showRoute() {
    if (!this.record) return;
    this.stopPlayback();
    const record = this.record;
    const route = get('carry-route').value;
    this.trial = record.verified?.route === route ? record.verified
      : record.proposal.forecast.trials.find(item => item.route === route);
    const frame = get('carry-frame');
    frame.max = this.trial.samples.length-1;
    frame.value = this.trial.samples.length-1;
    this.showFrame();
  }

  showFrame() {
    const sample = this.trial.samples[Number(get('carry-frame').value)];
    const {objectId} = this.record.proposal.request;
    const trial = this.trial;
    const first = trial.firstUnexpectedContact;
    const warning = trial.firstClearanceWarning || trial.firstCommandedWarning;
    const mm = value => (value*1000).toFixed(2)+' mm';
    const clearance = 'Measured gap · collider '+mm(trial.minColliderM)+' · visual '+mm(trial.minVisualEnvelopeM);
    const planned = trial.minCommandedColliderM === null ? '' : ' · Commanded lower bound · collider '
      +mm(trial.minCommandedColliderM)+' · visual '+mm(trial.minCommandedVisualEnvelopeM);
    const outcome = first ? 'First contact with '+first.obstacle+' at tick '+first.tick+' ('+first.phase+')'
      : trial.passed ? 'Settled '+mm(trial.positionErrorM)+' and '+trial.angleErrorDeg.toFixed(2)+'° from target'
      : warning ? 'First clearance warning at '+warning.obstacle+' · '+warning.envelope+' · '+warning.phase+' · tick '+warning.tick : trial.reason;
    get('carry-evidence').textContent = outcome+' · '+clearance+planned+' · frame '+sample.tick+' · '+sample.phase;
    this.spatial.showCarryFrame(objectId,sample,trial.samples);
  }

  togglePlayback() {
    if (this.playback) { this.stopPlayback(); return; }
    const frame=get('carry-frame');
    frame.value=0;
    this.showFrame();
    get('carry-play').textContent='Pause trace';
    this.playback=setInterval(() => {
      frame.value=Number(frame.value)+1;
      this.showFrame();
      if (frame.value===frame.max) this.stopPlayback();
    }, 100);
  }

  stopPlayback() {
    clearInterval(this.playback);
    this.playback=null;
    get('carry-play').textContent='Play trace';
  }
}
