
const $ = id => document.getElementById(id);

export class SpatialMeshEditPanel {
  constructor(spatial, geometryFrom) {
    this.spatial = spatial;
    this.geometryFrom = geometryFrom;
    this.generation = 0;
    const panel = document.createElement('details');
    panel.id = 'spatial-mesh-edit';
    panel.className = 'spatial-detail animation-timeline';
    panel.innerHTML =
      '<summary>Mesh editing</summary>' +
      '<p class="spatial-note">Pick an outer face on the selected prop, or enter its triangle index. The edit follows the connected flat region.</p>' +
      '<form id="mesh-edit-form" class="spatial-task">' +
      '<label>Triangle index<input id="mesh-edit-triangle" type="number" min="0" step="1" required></label>' +
      '<label>Outward distance · mm<input id="mesh-edit-distance" type="number" min="1" max="1000" step="any" value="100" required></label>' +
      '<button>Preview extrusion</button></form>' +
      '<div class="spatial-task"><button id="mesh-edit-save" class="primary">Save mesh copy</button>' +
      '<button id="mesh-edit-discard">Discard preview</button><button id="mesh-edit-stl">Export STL · mm</button></div>' +
      '<p id="mesh-edit-status" class="spatial-note" role="status"></p>';
    $('spatial-animation').before(panel);
    this.panel = panel;

    $('mesh-edit-form').oninput = () => this.discard();
    $('mesh-edit-form').onsubmit = event => {
      event.preventDefault();
      this.spatial.action(async () => {
        this.spatial.timeline.exit();
        this.clear();
        const version = this.generation;
        const args = { id: this.scene.id, objectId: this.object.id,
          triangle: $('mesh-edit-triangle').valueAsNumber, distanceM: $('mesh-edit-distance').valueAsNumber / 1000 };
        try {
          const result = await spatial.call('mesh-preview', args);
          if (!panel.open || this.blocked || this.generation !== version) return;
          const mesh = spatial.meshes.get(this.object.id);
          const edit = { args, result, mesh };
          if (mesh) {
            edit.original = mesh.geometry;
            edit.temporary = geometryFrom(result.visualMesh);
            mesh.geometry = edit.temporary;
          }
          this.preview = edit;
          const proof = result.evidence;
          $('mesh-edit-status').textContent = proof.regionTriangles + ' connected triangles · ' +
            (proof.predictedGainM3 * 1000).toFixed(3) + ' L added · collider unchanged.';
        } catch (error) { $('mesh-edit-status').textContent = error.message; }
      });
    };
    $('mesh-edit-save').onclick = () => {
      const edit = this.preview;
      if (!edit) return;
      this.spatial.action(async () => {
        if (edit !== this.preview) return;
        try {
          const id = this.object.sourceId;
          const copy = await spatial.call('mesh-save', { ...edit.args, previewSha256: edit.result.previewSha256 });
          await spatial.open(copy.id);
          await spatial.selectObject(copy.objects.find(body => body.sourceId === id).id);
          $('mesh-edit-status').textContent = 'Mesh copy saved · ' + copy.meshEdit.triangles + ' triangles · original retained.';
        } catch (error) { $('mesh-edit-status').textContent = error.message; }
      });
    };
    $('mesh-edit-stl').onclick = () => this.spatial.action(async () => {
      if (this.preview) return;
      const result = await spatial.call('mesh-stl', { id: this.scene.id, objectId: this.object.id });
      spatial.notice('Millimetre STL saved to ' + result.path);
    });
    $('mesh-edit-discard').onclick = () => this.discard();
    panel.ontoggle = () => { if (!panel.open) this.discard(); };
  }

  bind(scene, object) {
    if (scene?.id === this.scene?.id && object?.id === this.object?.id) return;
    this.clear();
    this.scene = scene;
    this.object = object;
    const encoded = scene?.visualMeshes?.[object?.visualMesh];
    const triangle = $('mesh-edit-triangle');
    triangle.value = '';
    triangle.max = encoded ? new DataView(Uint8Array.from(atob(encoded.slice(0, 12)), ch => ch.charCodeAt(0)).buffer)
      .getUint32(4, true) / 3 - 1 : 0;
    $('mesh-edit-status').textContent = encoded ? '' : 'Select a prop with a detailed triangle mesh.';
  }

  pick(hit) {
    if (!this.panel.open || this.blocked || this.preview || !this.object?.visualMesh ||
        hit.object !== this.spatial.meshes.get(this.object.id) || hit.faceIndex == null) return false;
    this.clear();
    $('mesh-edit-triangle').value = hit.faceIndex;
    $('mesh-edit-status').textContent = 'Triangle ' + hit.faceIndex + ' selected.';
    return true;
  }

  controls(blocked, busy) {
    this.blocked = blocked;
    if (blocked && this.preview) this.clear();
    const inactive = blocked || busy || !this.object?.visualMesh;
    for (const control of $('mesh-edit-form').elements) control.disabled = inactive;
    $('mesh-edit-save').disabled = inactive || !this.preview;
    $('mesh-edit-discard').disabled = !this.preview;
    $('mesh-edit-stl').disabled = inactive || Boolean(this.preview);
  }

  discard() {
    this.clear();
    $('mesh-edit-status').textContent = '';
    this.spatial.controls();
  }

  clear() {
    this.generation++;
    const edit = this.preview;
    this.preview = null;
    if (edit?.temporary) {
      edit.mesh.geometry = edit.original;
      edit.temporary.dispose();
    }
  }
}
