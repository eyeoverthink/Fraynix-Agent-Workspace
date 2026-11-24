
'use strict';

const assert = require('node:assert/strict');
const fs = require('node:fs/promises');
const os = require('node:os');
const path = require('node:path');
const { execFile } = require('node:child_process');
const { promisify } = require('node:util');
const { launch } = require('./calibration-integration.cjs');

const exec = promisify(execFile);
function findPlane(encoded, axis, value) {
  const data = Buffer.from(encoded, 'base64');
  const vertexCount = data.readUInt32LE(0);
  const triangleCount = data.readUInt32LE(4) / 3;
  for (let face = 0; face < triangleCount; face++) {
    const indices = [0, 1, 2].map(i => data.readUInt16LE(8 + vertexCount * 12 + face * 6 + i * 2));
    if (indices.every(i => Math.abs(data.readFloatLE(8 + i * 12 + axis * 4) - value) < 1e-6)) return face;
  }
  throw Error('Missing test plane');
}

async function main() {
  process.env.FRAYNIX_WORKSPACE_NO_MODELS = '1';
  const blender = process.env.FRAYNIX_TEST_BLENDER;
  if (blender) process.env.FRAYNIX_BLENDER = blender;
  const older = process.env.FRAYNIX_TEST_BLENDER_OLDER;
  const project = await fs.mkdtemp(path.join(os.tmpdir(), 'Fraynix mesh edit '));
  const output = path.resolve(process.argv[2] || 'build/visual-mesh-edit225');
  const extra = process.platform === 'linux' ? ['--no-sandbox', '--disable-dev-shm-usage'] : [];
  await fs.mkdir(output, { recursive: true });
  let { app, page } = await launch(project, extra);
  const api = (method, args = {}) => app.evaluate((_, r) =>
    global.workspaceTest.api('/api/spatial/' + r.method, r.args), { method, args });
  const status = text => page.waitForFunction(value =>
    document.querySelector('#mesh-edit-status')?.textContent.includes(value), text, { timeout: 16000 });
  const choose = (kind, file) => app.evaluate(({ dialog }, { kind, file }) => {
    if (kind === 'export') dialog.showSaveDialog = async () => ({ canceled: false, filePath: file });
    else dialog.showOpenDialog = async () => ({ canceled: false, filePaths: [file] });
  }, { kind, file });
  const capture = async name => {
    await page.locator('#spatial-mesh-edit').evaluate(el => el.scrollIntoView({ block: 'start' }));
    await page.evaluate(() => new Promise(done => requestAnimationFrame(() => requestAnimationFrame(done))));
    await fs.writeFile(path.join(output, name), Buffer.from(await app.evaluate(() => global.workspaceTest.capture()), 'base64'));
  };
  try {
    const original = await api('reconstruct-side-example');
    const prop = original.objects.find(o => o.sourceId === 'surface');
    const mesh = original.visualMeshes[prop.visualMesh];
    const triangle = findPlane(mesh, 1, .25);
    const args = { id: original.id, objectId: prop.id, triangle, distanceM: .10 };
    const snapshot = await api('snapshot');
    await page.locator('#spatial-tab').click();
    await page.locator('#spatial-mesh-edit summary').click();
    await page.locator('#mesh-edit-triangle').fill(String(triangle));
    await page.getByRole('button', { name: 'Preview extrusion', exact: true }).click();
    await status('10.500 L added');
    await capture('mesh-extrusion.png');
    assert(await page.locator('#spatial-observe').isDisabled());
    assert.deepEqual(await api('snapshot'), snapshot);

    const preview = await api('mesh-preview', args);
    assert.equal(preview.evidence.boundaryEdges, 26);
    assert.equal(preview.evidence.regionTriangles, 84);
    assert.equal(preview.evidence.triangles, 1428);
    await assert.rejects(api('mesh-save', { ...args, distanceM: .11, previewSha256: preview.previewSha256 }), /Preview the current face/);
    await assert.rejects(api('mesh-preview', { ...args, distanceM: -.1 }), /distanceM/);
    assert.equal((await api('list')).scenes.length, 1);
    await page.locator('#mesh-edit-save').click();
    await status('Mesh copy saved');
    const id = await page.locator('#spatial-scenes').inputValue();
    assert.notEqual(id, original.id);
    const saved = await api('read', { id });
    assert.equal(saved.meshEdit.meshSha256, preview.evidence.meshSha256);
    assert.deepEqual(saved.objects.map(o => [o.sourceId, o.size, o.massKg]), original.objects.map(o => [o.sourceId, o.size, o.massKg]));
    assert.deepEqual(await api('read', { id: original.id }), original);
    assert.equal((await api('mesh-save', { ...args, previewSha256: preview.previewSha256 })).id, id);
    await api('export-glb', { id });
    const savedProp = saved.objects.find(o => o.sourceId === 'surface');
    const stl = await api('mesh-stl', { id, objectId: savedProp.id });
    assert.equal(stl.meshSha256, savedProp.visualMesh);
    assert.equal(stl.units, 'mm');
    await page.locator('#mesh-edit-stl').click();
    await page.getByText('Millimetre STL saved to', { exact: false }).last().waitFor();
    const exports = path.join(project, 'exports');
    const names = (await fs.readdir(exports)).filter(name => name.endsWith('.stl'));
    assert.equal(names.length, 1);
    const bytes = await fs.readFile(path.join(exports, names[0]));
    assert.deepEqual(bytes, Buffer.from(stl.base64, 'base64'));
    assert.equal(bytes.readUInt32LE(80), 1428);
    assert.equal(bytes.length, 84 + 50 * 1428);
    await fs.writeFile(path.join(output, 'extruded-shell-mm.stl'), bytes);

    let blenderEvidence, returnedId;
    if (blender) {
      const native = path.join(output, 'fraynix-extrusion.blend');
      await choose('export', native);
      await page.locator('#spatial-export-blender').click();
      await page.getByText('Editable Blender scene saved:', { exact: false }).last().waitFor();
      const verifier = path.join(__dirname, 'spatial-mesh-edit-blender.py');
      await exec(older || blender, ['--background', '--factory-startup', '--disable-autoexec', native,
        '--python-exit-code', '17', '--python', verifier, '--', output], { timeout: 60000, windowsHide: true });
      blenderEvidence = JSON.parse(await fs.readFile(path.join(output, 'blender-mesh-evidence.json'), 'utf8'));
      assert.equal(blenderEvidence.original.triangles, 1428);
      await choose('import', path.join(output, 'mesh-blender-edit.blend'));
      await page.locator('#spatial-import-blender').click();
      await page.getByText('Blender scene imported.', { exact: false }).last().waitFor();
      returnedId = await page.locator('#spatial-scenes').inputValue();
      const returned = await api('read', { id: returnedId });
      const changed = returned.objects.find(o => o.sourceId === 'surface');
      assert.deepEqual(changed.size, prop.size);
      assert.equal(changed.massKg, prop.massKg);
      assert.notEqual(changed.visualMesh, preview.evidence.meshSha256);
      findPlane(returned.visualMeshes[changed.visualMesh], 1, .375);
      await capture('mesh-blender-return.png');
    }

    const animated = await api('animation-save', { id: original.id, sourceId: 'surface', keys: [
      { frame: 1, positionM: prop.position }, { frame: 25, positionM: [0, .1, 0] }
    ] });
    const animatedProp = animated.objects.find(o => o.sourceId === 'surface');
    const animationEdit = { ...args, id: animated.id, objectId: animatedProp.id };
    const animationPreview = await api('mesh-preview', animationEdit);
    const animationCopy = await api('mesh-save', { ...animationEdit, previewSha256: animationPreview.previewSha256 });
    assert.deepEqual(animationCopy.translationAnimation, animated.translationAnimation);
    const checkedAnimation = await api('animation-preview', { id: animationCopy.id });

    await app.close();
    ({ app, page } = await launch(project, extra));
    assert.deepEqual(await api('read', { id }), saved);
    assert.deepEqual(await api('animation-preview', { id: animationCopy.id }), checkedAnimation);
    if (returnedId) assert((await api('read', { id: returnedId })).visualMeshes);
    await fs.writeFile(path.join(output, 'mesh-edit-evidence.json'), JSON.stringify({
      geometry: saved.meshEdit, independentBlender: blenderEvidence || null, originalKept: true,
      stlSha256: stl.sha256, translationTimelineRetained: true, previewDidNotChangePhysics: true, stalePreviewRefused: true, restartPassed: true
    }, null, 2) + '\n');
    console.log(JSON.stringify({ triangles: saved.meshEdit.triangles, blenderEvidence, restartPassed: true }));
  } catch (error) {
    console.error(await page.locator('#mesh-edit-status').textContent().catch(() => null));
    throw error;
  } finally {
    await app.close().catch(() => {});
  }
}
main().catch(error => { console.error(error); process.exitCode = 1; });
