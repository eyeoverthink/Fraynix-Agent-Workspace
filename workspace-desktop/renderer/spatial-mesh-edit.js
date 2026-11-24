
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
      '<p class="spatial-note">Choose an operation, then click the selected prop or enter an index. Preview the edit before saving.</p>' +
      '<form id="mesh-edit-form" class="spatial-task">' +
       '<label>Operation<select id="mesh-edit-mode"><option value="extrude">Extrude region</option><option value="vertex">Move vertex</option><option value="edge">Move edge</option><option value="face">Move face</option><option value="fit">Refresh collider</option></select></label>' +
      '<label class="mesh-field" id="mesh-index-field"><span id="mesh-index-label">Triangle index</span><input id="mesh-edit-triangle" type="number" min="0" step="1" required></label>' +
      '<label class="mesh-field" id="mesh-distance-field">Outward distance · mm<input id="mesh-edit-distance" type="number" min="1" max="1000" step="any" value="100" required></label>' +
       '<label class="mesh-field" id="mesh-edge-field">Edge of triangle<select id="mesh-edge"><option value="0">A to B</option><option value="1">B to C</option><option value="2">C to A</option></select></label>' +
      ['x','y','z'].map(axis=>'<label class="mesh-field" id="mesh-delta-'+axis+'">'+axis.toUpperCase()+' offset · mm<input type="number" min="-1000" max="1000" step="any" value="0" required></label>').join('') +
      '<label class="mesh-field" id="mesh-fit-field"><input type="checkbox" id="mesh-fit"> Fit collider after edit</label>' +
      '<button id="mesh-preview-button">Preview extrusion</button></form>'  +
      '<div class="spatial-task"><button id="mesh-edit-save" class="primary">Save mesh copy</button>' +
      '<button id="mesh-undo">Undo saved edit</button><button id="mesh-edit-discard">Discard preview</button><button id="mesh-edit-stl">Export STL · mm</button></div>' +
      '<p id="mesh-edit-status" class="spatial-note" role="status"></p>';
    $('spatial-animation').before(panel);
    this.panel = panel;

    $('mesh-edit-form').oninput = () => this.discard();
    this.updateMode();
    $('mesh-edit-form').onsubmit = event => {
      event.preventDefault();
      this.spatial.action(async () => {
        this.spatial.timeline.exit();
        this.clear();
        const version = this.generation;
        const args = this.request();
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
          const collider = proof.colliderCentreShiftM ? 'collider fit ' + proof.colliderSizeM.map(v=>(v*1000).toFixed(1)).join(' × ') + ' mm' : 'collider unchanged';
          $('mesh-edit-status').textContent = proof.regionTriangles ? proof.regionTriangles + ' connected triangles · ' +
            (proof.predictedGainM3 * 1000).toFixed(3) + ' L added · ' + collider + '.' :
            (proof.movedVertices ? proof.movedVertices + ' shared vertices moved · ' : '') + collider + '.';
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
    $('mesh-undo').onclick = () => spatial.action(async () => {
      const sourceId=this.scene.meshEdit.sourceId;
      const prior=await spatial.call('mesh-undo',{id:this.scene.id});
      await spatial.open(prior.id);
      await spatial.selectObject(prior.objects.find(body=>body.sourceId===sourceId).id);
      $('mesh-edit-status').textContent='Previous scene restored.';
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
    this.header = encoded ? new DataView(Uint8Array.from(atob(encoded.slice(0, 12)), ch => ch.charCodeAt(0)).buffer)
       : null;
    this.updateMode();
    $('mesh-edit-status').textContent = encoded ? '' : 'Select a prop with a detailed triangle mesh.';
  }

  pick(hit) {
    if (!this.panel.open || this.blocked || this.preview || !this.object?.visualMesh ||
        hit.object !== this.spatial.meshes.get(this.object.id) || hit.faceIndex == null) return false;
    this.clear();
    const mode=$('mesh-edit-mode').value;
    if(mode==='fit')return false;
    if(mode==='vertex'||mode==='edge'){
      const mesh=hit.object,indexes=[hit.face.a,hit.face.b,hit.face.c];
      const attribute=mesh.geometry.getAttribute('position');
      const local=mesh.worldToLocal(hit.point.clone());
      const points=indexes.map(i=>local.clone().fromBufferAttribute(attribute,i));
      const distances=points.map((point,i)=>{
        if(mode==='vertex')return local.distanceToSquared(point);
        const direction=points[(i+1)%3].clone().sub(point);
        const t=Math.max(0,Math.min(1,local.clone().sub(point).dot(direction)/direction.lengthSq()));
        return local.distanceToSquared(point.clone().addScaledVector(direction,t));
      });
      const nearest=distances.indexOf(Math.min(...distances));
      if(mode==='vertex')$('mesh-edit-triangle').value=indexes[nearest];
      else {$('mesh-edit-triangle').value=hit.faceIndex;$('mesh-edge').value=nearest;}
    }else $('mesh-edit-triangle').value = hit.faceIndex;
    $('mesh-edit-status').textContent = mode + ' selected.';
    return true;
  }

  request() {
    const mode=$('mesh-edit-mode').value;
    const args={id:this.scene.id,objectId:this.object.id};
    const index=$('mesh-edit-triangle').valueAsNumber;
    if(mode==='fit')return {...args,mode};
    if($('mesh-fit').checked)args.refreshCollider=true;
    if(mode==='extrude')return {...args,triangle:index,distanceM:$('mesh-edit-distance').valueAsNumber/1000};
    if(mode==='edge')args.edge=Number($('mesh-edge').value);
    return {...args,mode,index,deltaM:['x','y','z'].map(axis=>$('mesh-delta-'+axis).querySelector('input').valueAsNumber/1000)};
  }

  updateMode() {
    const mode=$('mesh-edit-mode').value;
    const component=['vertex','edge','face'].includes(mode);
    $('mesh-index-field').hidden=mode==='fit';
    $('mesh-distance-field').hidden=mode!=='extrude';
    $('mesh-fit-field').hidden=mode==='fit';
    $('mesh-edge-field').hidden=mode!=='edge';
    $('mesh-index-label').textContent=mode==='vertex'?'Vertex index':'Triangle index';
    $('mesh-edit-triangle').max=this.header?(mode==='vertex'?this.header.getUint32(0,true):this.header.getUint32(4,true)/3)-1:0;
    for(const axis of ['x','y','z'])$('mesh-delta-'+axis).hidden=!component;
    $('mesh-preview-button').textContent=mode==='extrude'?'Preview extrusion':'Preview change';
  }

  controls(blocked, busy) {
    this.blocked = blocked;
    if (blocked && this.preview) this.clear();
    const inactive = blocked || busy || !this.object?.visualMesh;
    for (const control of $('mesh-edit-form').elements) control.disabled = inactive || Boolean(control.closest('.mesh-field[hidden]'));
    $('mesh-undo').disabled = blocked||busy||!this.scene?.meshEdit;
    $('mesh-edit-save').disabled = inactive || !this.preview;
    $('mesh-edit-discard').disabled = !this.preview;
    $('mesh-edit-stl').disabled = inactive || Boolean(this.preview);
  }

  discard() {
    this.clear();
    this.updateMode();
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
