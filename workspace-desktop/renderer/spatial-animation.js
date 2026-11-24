
const $ = id => document.getElementById(id);
const coordinates = ['x', 'y', 'z'];

export class SpatialAnimationPanel {
  constructor(spatial) {
    this.spatial = spatial;
    this.previewGeneration = 0;
    const panel = document.createElement('details');
    panel.id = 'spatial-animation';
    panel.className = 'spatial-detail animation-timeline';
    panel.innerHTML =
      '<summary>Animation timeline</summary>' +
      '<div class="animation-top"><strong id="animation-object">Choose a dynamic prop</strong><span>24 fps</span></div>' +
      '<form id="animation-key" class="spatial-task"><label>Frame<input id="animation-frame" type="number" min="2" max="721" required step="1" value="25"></label>' +
      coordinates.map(axis => '<label>' + axis.toUpperCase() + ' · m<input id="animation-' + axis +
        '" type="number" min="-20" max="20" step="any" required></label>').join('') +
      '<button>Set key</button></form>' +
      '<div class="spatial-task"><label>Keys<select id="animation-keys"></select></label><button id="animation-remove">Remove key</button>' +
      '<button id="animation-preview">Preview</button><button id="animation-save" class="primary">Save as scene copy</button></div>' +
      '<div id="animation-transport" hidden><div class="animation-scrub"><label for="animation-scrub">Playhead</label>' +
      '<input id="animation-scrub" type="range" min="1" value="1" step="1"><output id="animation-playhead"></output></div>' +
      '<div class="spatial-task"><button id="animation-play">Play</button><button id="animation-stop">Exit preview</button></div></div>' +
      '<p id="animation-status" role="status"></p>';
    $('spatial-stage').before(panel);
    $('animation-key').onsubmit = event => {
      event.preventDefault();
      this.stop();
      const frame = $('animation-frame').valueAsNumber;
      const positionM = coordinates.map(axis => $('animation-' + axis).valueAsNumber);
      this.keys = this.keys.filter(key => key.frame !== frame).concat({ frame, positionM }).sort((a, b) => a.frame - b.frame);
      this.dirty = true;
      this.renderKeys(frame);
    };
    $('animation-keys').onchange = () => {
      const key = this.keys.find(key => key.frame === Number($('animation-keys').value));
      $('animation-frame').value = Math.max(2, key.frame);
      this.fields(key.positionM);
      this.controls();
    };
    $('animation-remove').onclick = () => {
      this.stop();
      this.keys = this.keys.filter(key => key.frame !== Number($('animation-keys').value));
      this.dirty = true;
      this.renderKeys();
    };
    $('animation-preview').onclick = () => this.run(async () => {
      const generation = this.previewGeneration;
      const samples = await spatial.call('animation-preview', this.request());
      if (!panel.open || this.blocked || generation !== this.previewGeneration) return;
      this.samples = samples;
      $('animation-transport').hidden = false;
      $('animation-scrub').max = this.samples.endFrame;
      this.scrub(1);
      $('animation-status').textContent = 'Previewing authored positions. Physics starts from frame 1.';
    });
    $('animation-save').onclick = () => this.run(async () => {
      const sourceId = this.object.sourceId;
      const saved = await spatial.call('animation-save', this.request());
      await spatial.open(saved.id);
      await spatial.selectObject(saved.objects.find(body => body.sourceId === sourceId).id);
      $('animation-status').textContent = 'Scene copy saved. Use Export Blender to keep the timeline editable.';
    });
    $('animation-scrub').oninput = () => {
      this.pause();
      this.scrub(Number($('animation-scrub').value));
    };
    $('animation-play').onclick = () => this.play();
    $('animation-stop').onclick = () => this.exit();
    panel.ontoggle = () => { if (!panel.open) this.exit(); };
  }

  bind(scene, object) {
    if (scene?.id === this.scene?.id && object?.id === this.object?.id) return;
    this.stop();
    this.scene = scene;
    this.object = object;
    const saved = scene?.translationAnimation?.tracks[object?.sourceId];
    this.keys = saved ? structuredClone(saved) : object ? [{ frame: 1, positionM: object.position }] : [];
    this.dirty = false;
    $('animation-object').textContent = object?.label || 'Choose a dynamic prop';
    $('animation-status').textContent = saved ? 'Saved translation keys' : '';
    $('animation-frame').value = saved?.at(-1)?.frame || 25;
    if (object) this.fields(saved?.at(-1)?.positionM || object.position);
    this.renderKeys();
  }

  fields(point) {
    coordinates.forEach((axis, i) => { $('animation-' + axis).value = point[i]; });
  }

  renderKeys(selected = 1) {
    const list = $('animation-keys');
    list.replaceChildren(...this.keys.map(({ frame, positionM }) => new Option(
      'Frame ' + frame + ' · ' + positionM.map(value => value.toFixed(3)).join(', '), frame)));
    if (this.keys.some(key => key.frame === selected)) list.value = selected;
    this.controls();
  }

  controls(blocked = this.blocked) {
    this.blocked = blocked;
    const inactive = blocked || !this.object?.massKg;
    if (inactive && this.samples) this.stop();
    const locked = inactive || this.requesting;
    for (const field of $('animation-key').elements) field.disabled = locked;
    $('animation-keys').disabled = locked;
    $('animation-remove').disabled = locked || Number($('animation-keys').value) <= 1;
    $('animation-preview').disabled = locked || (this.keys.length < 2 && (this.dirty || !this.scene?.translationAnimation));
    $('animation-save').disabled = locked || !this.dirty || this.keys.length < 2;
  }

  request() {
    const id = this.scene.id;
    if (!this.dirty) return { id };
    return { id, sourceId: this.object.sourceId, keys: this.keys };
  }

  run(task) {
    if (this.requesting) return;
    const originalScene = this.scene;
    const originalObject = this.object;
    this.requesting = true;
    this.controls();
    return this.spatial.action(async () => {
      try {
        if (this.scene === originalScene && this.object === originalObject) await task();
      } catch (error) {
        $('animation-status').textContent = error.message;
      } finally {
        this.requesting = false;
        this.controls();
      }
    });
  }

  scrub(frame) {
    this.frame = frame;
    $('animation-scrub').value = frame;
    const point = this.point(this.object);
    $('animation-playhead').textContent = 'Frame ' + frame + ' · ' + ((frame - 1) / this.samples.fps).toFixed(2) + ' s · ' +
      point.map(value => value.toFixed(3)).join(', ') + ' m';
    this.spatial.paint(this.scene.objects);
  }

  point(object) {
    const track = this.samples?.positionsM[object.sourceId];
    return track?.[Math.min(this.frame, track.length) - 1] || object.position;
  }

  pose(objects) {
    if (!this.samples || this.scene?.id !== this.spatial.scene?.id) return objects;
    return objects.map(object => ({ ...object, position: this.point(object) }));
  }

  play() {
    if (!this.samples) return;
    if (this.playing) { this.pause(); return; }
    if (this.frame === this.samples.endFrame) this.scrub(1);
    const firstFrame = this.frame;
    const started = performance.now();
    $('animation-play').textContent = 'Pause';
    const advance = now => {
      const current = Math.min(this.samples.endFrame, firstFrame + Math.max(0, Math.floor((now - started) * this.samples.fps / 1000)));
      if (current !== this.frame) this.scrub(current);
      if (current === this.samples.endFrame) this.pause();
      else this.playing = requestAnimationFrame(advance);
    };
    this.playing = requestAnimationFrame(advance);
  }

  pause() {
    if (this.playing) cancelAnimationFrame(this.playing);
    this.playing = 0;
    $('animation-play').textContent = 'Play';
  }

  exit() {
    this.stop();
    this.spatial.controls();
  }

  stop() {
    this.previewGeneration++;
    this.pause();
    const hadPreview = Boolean(this.samples);
    this.samples = null;
    $('animation-transport').hidden = true;
    if (hadPreview && this.scene?.id === this.spatial.scene?.id) {
      this.spatial.paint(this.spatial.snapshot?.current?.objects || this.scene.objects);
    }
  }
}
