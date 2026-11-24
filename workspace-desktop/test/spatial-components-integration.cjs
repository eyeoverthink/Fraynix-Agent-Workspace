
'use strict';

const assert = require('node:assert/strict');
const fs = require('node:fs/promises');
const http = require('node:http');
const os = require('node:os');
const path = require('node:path');
const { execFile } = require('node:child_process');
const { promisify } = require('node:util');
const { launch } = require('./calibration-integration.cjs');

const markup = '<!doctype html><title>Workshop bracket graphic</title><style>body{margin:0;background:white}svg{display:block;width:120px;height:auto}@media(max-width:119px){svg{width:100px}}@media(max-width:99px){svg{width:80px}}@media(max-width:79px){svg{width:60px}}</style>' +
  '<svg id="bracket-graphic" width="120" height="120" viewBox="0 0 160 160">' +
  '<path fill="#173d6a" fill-rule="evenodd" d="M32 24H120V48H136V128H56V112H24V56H32Z M64 64H96V96H64Z"/></svg>';
const closeTo = (actual, expected) => assert(Math.abs(actual - expected) < 2e-6, [actual, expected]);

function meshOf(scene) {
  const body = scene.objects.find(o => o.sourceId === 'surface');
  const bytes = Buffer.from(scene.visualMeshes[body.visualMesh], 'base64');
  const vertices = bytes.readUInt32LE();
  return { body, positions: Array.from({ length: vertices * 3 }, (_, i) => bytes.readFloatLE(8 + i * 4)),
    indices: Array.from({ length: bytes.readUInt32LE(4) }, (_, i) => bytes.readUInt16LE(8 + vertices * 12 + i * 2)) };
}

async function main() {
  process.env.FRAYNIX_WORKSPACE_NO_MODELS = '1';
  const blender = process.env.FRAYNIX_TEST_BLENDER;
  if (blender) process.env.FRAYNIX_BLENDER = blender;
  const older = process.env.FRAYNIX_TEST_BLENDER_OLDER;
  const project = await fs.mkdtemp(path.join(os.tmpdir(), 'Fraynix component edits '));
  const out = path.resolve(process.argv[2] || 'build/visual-components226');
  await fs.mkdir(out, { recursive: true });
  const server = http.createServer((_, response) => {
    response.setHeader('Content-Type', 'text/html; charset=utf-8');
    response.end(markup);
  });
  await new Promise(resolve => server.listen(0, '127.0.0.1', resolve));
  const url = 'http://127.0.0.1:' + server.address().port + '/graphic';
  const extra = process.platform === 'linux' ? ['--no-sandbox', '--disable-dev-shm-usage'] : [];
  let { app, page } = await launch(project, extra);
  const api = (method, args = {}) => app.evaluate((_, value) =>
    global.workspaceTest.api('/api/spatial/' + value.method, value.args), { method, args });
  const read = async () => api('read', { id: await page.locator('#spatial-scenes').inputValue() });
  const status = (id, value) => page.waitForFunction(([id, value]) =>
    document.getElementById(id)?.textContent.includes(value), [id, value], { timeout: 20000 });
  const capture = async (panel, name) => {
    await page.locator(panel).evaluate(el => el.scrollIntoView({ block: 'start' }));
    await page.evaluate(() => new Promise(done => requestAnimationFrame(() => requestAnimationFrame(done))));
    await fs.writeFile(path.join(out, name), Buffer.from(await app.evaluate(() => global.workspaceTest.capture()), 'base64'));
  };
  const pick = (kind, file) => app.evaluate(({ dialog }, { kind, file }) => {
    if (kind === 'save') dialog.showSaveDialog = async () => ({ canceled: false, filePath: file });
    else dialog.showOpenDialog = async () => ({ canceled: false, filePaths: [file] });
  }, { kind, file });

  try {
    const original = await api('reconstruct-side-example');
    let before = meshOf(original);
    const top = 672, vertex = before.indices[top * 3];
    const neighbour = before.indices[top * 3 + 1];
    const collapsed = [0,1,2].map(axis=>before.positions[neighbour*3+axis]-before.positions[vertex*3+axis]);
    await assert.rejects(api('mesh-preview',{id:original.id,objectId:before.body.id,mode:'vertex',index:vertex,
      deltaM:collapsed}), /collapse or reverse/);
    await page.locator('#spatial-tab').click();
    await page.locator('#spatial-mesh-edit summary').click();
    const components = [];
    for (const [mode, index, affected] of [['vertex', vertex, 1], ['edge', top, 2], ['face', top, 3]]) {
      await page.locator('#mesh-edit-mode').selectOption(mode);
      await page.locator('#mesh-edit-triangle').fill(String(index));
      await page.locator('#mesh-delta-y input').fill('10');
      await page.locator('#mesh-preview-button').click();
      await status('mesh-edit-status', affected + ' shared vertices moved');
      await page.locator('#mesh-edit-save').click();
      await status('mesh-edit-status', 'Mesh copy saved');
      const edited = await read();
      const after = meshOf(edited);
      assert.deepEqual(after.indices, before.indices);
      let moved = 0;
      for (let i = 0; i < after.positions.length; i++) {
        const offset = after.positions[i] - before.positions[i];
        if (Math.abs(offset) > 1e-6) { assert.equal(i % 3, 1); closeTo(offset, .01); moved++; }
      }
      assert.equal(moved, affected);
      assert.deepEqual(after.body.size, before.body.size);
      before = after;
      components.push(edited.id);
    }

    await page.locator('#mesh-edit-mode').selectOption('fit');
    await page.locator('#mesh-preview-button').click();
    await status('mesh-edit-status', '530.0');
    await capture('#spatial-mesh-edit', 'collider-refresh.png');
    await page.locator('#mesh-edit-save').click();
    await status('mesh-edit-status', 'Mesh copy saved');
    const fitted = await read();
    const fittedMesh = meshOf(fitted);
    closeTo(fittedMesh.body.size[1], .53);
    closeTo(fittedMesh.body.position[1], .015);
    fittedMesh.positions.forEach((value, i) => closeTo(value + (i % 3 === 1 ? .015 : 0), before.positions[i]));
    await page.locator('#mesh-undo').click();
    await status('mesh-edit-status', 'Previous scene restored');
    assert.equal((await read()).id, components.at(-1));
    await page.locator('#spatial-scenes').selectOption(fitted.id);
    await page.waitForFunction(id => document.querySelector('#spatial-scenes').value === id, fitted.id);

    let blenderProof = null;
    if (blender) {
      const native = path.join(out, 'fraynix-components.blend');
      await pick('save', native);
      await page.locator('#spatial-export-blender').click();
      await page.getByText('Editable Blender scene saved:', { exact: false }).last().waitFor();
      await promisify(execFile)(older || blender, ['--background', '--factory-startup', '--disable-autoexec',
        native, '--python-exit-code', '17', '--python', path.join(__dirname, 'spatial-components-blender.py'),
        '--', out], { timeout: 60000, windowsHide: true });
      blenderProof = JSON.parse(await fs.readFile(path.join(out, 'blender-components.json'), 'utf8'));
      await pick('open', path.join(out, 'blender-component-edit.blend'));
      await page.locator('#spatial-import-blender').click();
      await page.getByText('Blender scene imported.', { exact: false }).last().waitFor();
      const returned = meshOf(await read());
      closeTo(returned.body.size[1], .53);
      closeTo(Math.max(...returned.positions.filter((_, i) => i % 3 === 1)), .270);
    }

    const rotated = JSON.parse(original.sourceText);
    rotated.objects.find(o=>o.sourceId==='surface').rotation = [0,0,Math.SQRT1_2,Math.SQRT1_2];
    rotated.translationAnimation={fps:24,tracks:{surface:[
      {frame:1,positionM:[0,0,0]},{frame:25,positionM:[0,0,.1]}
    ]}};
    const animated = await api('import',{text:JSON.stringify(rotated)});
    const animatedBody = meshOf(animated).body;
    const rotationEdit={id:animated.id,objectId:animatedBody.id,mode:'vertex',index:vertex,
      deltaM:[0,.01,0],refreshCollider:true};
    const rotationPreview = await api('mesh-preview',rotationEdit);
    const rotatedCopy = await api('mesh-save',{...rotationEdit,previewSha256:rotationPreview.previewSha256});
    closeTo(meshOf(rotatedCopy).body.position[0],-.005);
    closeTo(rotatedCopy.translationAnimation.tracks.surface[1].positionM[0],-.005);
    closeTo(rotatedCopy.translationAnimation.tracks.surface[1].positionM[2],.1);

    await app.evaluate((_, url) => global.workspaceTest.execute('browser.navigate', { url }), url);
    await page.locator('#spatial-mesh-edit summary').click();
    await page.locator('#spatial-browser-trace summary').click();
    await page.locator('#spatial-trace-selector').fill('#missing');
    await page.locator('#spatial-trace-build').click();
    await status('spatial-trace-status', 'Select exactly one');
    await page.locator('#spatial-trace-selector').fill('#bracket-graphic');
    await page.locator('#spatial-trace-build').click();
    await status('spatial-trace-status', 'silhouette overlap');
    await status('spatial-source', 'one captured view');
    const traced = await read();
    const proof = traced.reconstruction;
    assert.equal(proof.sourceUrl, url);
    assert.equal(proof.selector, '#bracket-graphic');
    assert(proof.foregroundIou > .98);
    assert.equal(proof.candidateFits.length, 3);
    assert(proof.foregroundIou > proof.candidateFits[0].foregroundIou);
    await page.locator('#spatial-trace-export').click();
    await page.getByText('Checked GLB saved to', { exact: false }).last().waitFor();
    const glb = await api('export-glb', { id: traced.id });
    await fs.writeFile(path.join(out, 'browser-trace.glb'), Buffer.from(glb.base64, 'base64'));
    await capture('#spatial-browser-trace', 'browser-trace.png');
    await app.close();
    ({ app, page } = await launch(project, extra));
    assert.deepEqual(await api('read', { id: traced.id }), traced);
    assert.equal((await api('mesh-undo', { id: fitted.id })).id, components.at(-1));
    assert.deepEqual(await api('read', { id: original.id }), original);
    const captured = path.join(project, 'spatial', 'reconstruction-sources', traced.id + '.json');
    const packageFile = JSON.parse(await fs.readFile(captured, 'utf8'));
    await fs.writeFile(path.join(out, 'browser-graphic.png'), Buffer.from(packageFile.png, 'base64'));
    await fs.appendFile(captured, ' ');
    await assert.rejects(api('export-glb', { id: traced.id }), /original visual reconstruction/);
    await fs.writeFile(path.join(out, 'component-trace-evidence.json'), JSON.stringify({
      componentCounts: [1, 2, 3], colliderSizeM: fittedMesh.body.size, colliderCenterM: fittedMesh.body.position,
      blender: blenderProof, browser: proof, glbSha256: glb.sha256, collapsedFaceRefused: true, rotatedAnimationRebased: true, undoAfterRestart: true, alteredCaptureRefused: true
    }, null, 2) + '\n');
    console.log(JSON.stringify({ components: [1, 2, 3], blenderProof, overlap: proof.foregroundIou, fits: proof.candidateFits }));
  } catch(error) {
    console.error(await page.locator("#spatial-trace-status").textContent().catch(()=>null));
    throw error;
  } finally {
    await app.close().catch(() => {});
    server.close();
  }
}
main().catch(error => { console.error(error); process.exitCode = 1; });
