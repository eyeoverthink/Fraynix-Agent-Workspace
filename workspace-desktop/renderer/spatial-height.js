const get = id => document.getElementById(id);

export function isSpatialHeightRequest(text) {
  return /^(?:(?:please )?(?:measure|estimate|check) (?:the )?height of (?:this|the) (?:prop|object)|which (?:camera )?view (?:would tell you|shows|would show) how tall (?:this|it) is|how tall is (?:this|the) (?:prop|object)|(?:measure|check) height in (?:the )?(?:open )?(?:height )?study)[.!?]?$/i.test(text.trim());
}

export class SpatialHeightPanel {
  constructor(spatial) {
    this.spatial = spatial;
    const panel = document.createElement('details');
    panel.id = 'spatial-height';
    panel.className = 'spatial-detail';
    panel.innerHTML = `
      <summary>Measure height from calibrated views</summary>
      <div class="spatial-task">
        <button id="height-example">Load Blender example</button>
        <label class="spatial-file">Import height study<input id="height-import" type="file" accept=".json,application/json" aria-label="Import height study"></label>
        <button id="height-reopen">Reopen last saved study</button>
      </div>
      <p id="height-source" class="spatial-note" hidden></p>
      <p id="height-status" class="spatial-note" role="status">Choose a registered study to compare camera angles.</p>
      <div class="height-views" id="height-views" hidden></div>
      <ul id="height-scores" class="height-scores" hidden></ul>
      <p id="height-measurement" class="spatial-note" hidden></p>
      <div class="spatial-task">
        <button id="height-approve" class="primary" disabled>Approve next view</button>
        <button id="height-collider" disabled>Approve collider and open in Spatial</button>
      </div>`;
    get('spatial-carry').before(panel);
    get('height-example').onclick = () => this.run(() => this.fetch('height-example'));
    get('height-reopen').onclick = () => this.run(() => this.fetch('height-latest'));
    get('height-import').onchange = () => this.run(() => this.importStudy());
    get('height-approve').onclick = () => this.run(() => this.approve());
    get('height-collider').onclick = () => this.run(() => this.collider());
  }

  controls() {
    for (const id of ['height-example','height-import','height-reopen']) get(id).disabled = this.busy;
    get('height-approve').disabled = this.busy || this.record?.status !== 'review';
    get('height-collider').disabled = this.busy || this.record?.status !== 'verified';
  }

  async run(work) {
    if (this.busy) return;
    this.busy = true;
    this.controls();
    try { await work(); }
    catch (error) {
      const status = get('height-status');
      status.dataset.status = 'error';
      status.textContent = error.message;
    } finally {
      this.busy = false;
      this.controls();
    }
  }

  async openFromChat(text) {
    if (!this.spatial.visible) await this.spatial.open();
    const panel = get('spatial-height');
    panel.open = true;
    panel.scrollIntoView({block:'start'});
    await this.run(async () => {
      if (!this.record) {
        this.spatial.announce('Choose the Blender example, import a registered study, or reopen your saved study in Spatial.');
        return;
      }
      if (this.spatial.scene && !/\bstudy\b/i.test(text)) {
        this.spatial.announce('A scene is open in Spatial. To measure the registered camera target, ask “Measure height in the open study.”');
        return;
      }
      const record = await this.fetch('height-read',{id:this.record.id});
      if (record.status === 'review') {
        this.spatial.announce('The '+this.label(record.plan.selectedView)+' view gives the strongest separation in '+record.plan.title+'. Approve that observation to measure height.');
      } else if (record.status === 'verified') {
        this.spatial.announce(this.summary(record));
      } else {
        this.spatial.announce('The saved study could not verify a height. Open its evidence to inspect the refusal.');
      }
    });
  }

  async fetch(command,args) {
    this.record = null;
    this.controls();
    const record = await this.spatial.call(command,args);
    this.show(record);
    return record;
  }

  async importStudy() {
    const input = get('height-import');
    const file = input.files[0];
    input.value = '';
    if (!file) return;
    if (file.size>1200000) throw Error('Choose a height study up to 1.2 MB.');
    const bytes = new Uint8Array(await file.arrayBuffer());
    let binary = '';
    for (let offset=0;offset<bytes.length;offset+=8192) binary+=String.fromCharCode(...bytes.subarray(offset,offset+8192));
    await this.fetch('height-review',{base64:btoa(binary)});
  }

  async approve() {
    const {id,planSha256} = this.record;
    const record = await this.fetch('height-approve',{id,planSha256});
    this.spatial.announce(record.status==='verified' ? this.summary(record)
      : (record.measurement?.reason || 'The study cannot verify height.'));
  }

  async collider() {
    const {id,colliderApprovalSha256} = this.record;
    const scene = await this.spatial.call('height-collider',{id,colliderApprovalSha256});
    await this.spatial.open(scene.id);
    this.spatial.notice('Approved upper-bound collider opened in Spatial.');
    this.spatial.announce('I opened the approved collider. Its height uses the upper bound from the verified interval.');
  }

  show(record) {
    const status = get('height-status');
    const source = get('height-source');
    const scores = get('height-scores');
    const pictures = get('height-views');
    const measure = get('height-measurement');
    this.record = record.id ? record : null;
    scores.replaceChildren();
    pictures.replaceChildren();
    measure.hidden = true;
    source.hidden = scores.hidden = pictures.hidden = !this.record;
    status.dataset.status = record.status || 'empty';
    if (!this.record) {
      status.textContent = 'No saved study. Choose the Blender example or import a registered study.';
      return;
    }
    const {plan,measurement,previews} = record;
    source.textContent = plan.title+' · '+record.sourceSha256.slice(0,10)+' · '+plan.sourceNote;
    for (const [key,caption] of [['initial','Initial image'],['selected','Approved view'],['witness','Withheld check']]) {
      if (!previews[key]) continue;
      const figure = document.createElement('figure');
      const image = document.createElement('img');
      image.src = 'data:image/png;base64,'+previews[key];
      image.alt = caption+' of the registered prop';
      const label = document.createElement('figcaption');
      label.textContent = caption;
      figure.append(image,label);
      pictures.append(figure);
    }
    for (const row of plan.candidateScores) {
      const item = document.createElement('li');
      item.textContent = this.label(row.view)+' · '+row.separationLowerPx.toFixed(1)+' px separation after the declared pixel allowance';
      if (row.view===plan.selectedView) item.className = 'height-selected';
      scores.append(item);
    }
    if (record.status==='review') {
      const heights = plan.hypothesesM.map(height=>this.mm(height));
      status.textContent = 'Both '+heights[0]+' and '+heights[1]+' fit the first view. Review '+this.label(plan.selectedView)+'.';
    } else if (record.status==='verified') {
      status.textContent = 'Height verified against the withheld view.';
      measure.hidden = false;
      measure.textContent = this.summary(record);
    } else {
      status.textContent = measurement?.reason || 'The registered next views cannot separate the two height hypotheses.';
    }
  }

  summary(record) {
    const m = record.measurement;
    return record.plan.title+' measured '+this.mm(m.heightM)+' · conditional range '+this.mm(m.lowM)+' to '+this.mm(m.highM)
      +' · withheld difference '+m.withheldErrorPx.toFixed(2)+' px. The proposed collider uses the upper bound and the declared footprint.';
  }

  mm(metres) { return (metres*1000).toFixed(1)+' mm'; }
  label(id) { return id.replaceAll('-',' '); }
}
