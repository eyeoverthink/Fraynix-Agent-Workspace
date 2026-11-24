
'use strict';

const assert = require('node:assert/strict');
const os = require('node:os');
const path = require('node:path');
const fs = require('node:fs/promises');
const { execFile } = require('node:child_process');
const { createHash } = require('node:crypto');
const { promisify } = require('node:util');
const { launch } = require('./calibration-integration.cjs');

const exec = promisify(execFile);
const hashFile = async file => createHash('sha256').update(await fs.readFile(file)).digest('hex');
const near = (point, expected) => {
  assert.equal(point.length, expected.length);
  point.forEach((value, i) => assert(Math.abs(value - expected[i]) < 2e-6, [point, expected]));
};

async function main() {
  process.env.FRAYNIX_WORKSPACE_NO_MODELS = '1';
  const blender = process.env.FRAYNIX_TEST_BLENDER;
  if (blender) process.env.FRAYNIX_BLENDER = blender;
  const otherBlender = process.env.FRAYNIX_TEST_BLENDER_OLDER;
  const legacyBlend = process.env.FRAYNIX_TEST_LEGACY_BLEND;
  const project = await fs.mkdtemp(path.join(os.tmpdir(), 'Fraynix authored animation '));
  const output = path.resolve(process.argv[2] || 'build/visual-animation224');
  const extra = process.platform === 'linux' ? ['--no-sandbox', '--disable-dev-shm-usage'] : [];
  await fs.mkdir(output, { recursive: true });

  let { app, page } = await launch(project, extra);
  const api = (method, args = {}) => app.evaluate((_, r) =>
    global.workspaceTest.api('/api/spatial/' + r.method, r.args), { method, args });
  const wait = (selector, text) => page.waitForFunction(({ selector, text }) =>
    document.querySelector(selector)?.textContent.includes(text), { selector, text }, { timeout: 18000 });
  const setPlayhead = frame => page.locator('#animation-scrub').evaluate((slider, value) => {
    slider.value = value;
    slider.dispatchEvent(new Event('input', { bubbles: true }));
  }, frame);
  const choose = async (kind, file) => app.evaluate(({ dialog }, { kind, file }) => {
    // Select only the test path. The existing Electron handler performs the real exchange.
    if (kind === 'export') dialog.showSaveDialog = async () => ({ canceled: false, filePath: file });
    else dialog.showOpenDialog = async () => ({ canceled: false, filePaths: [file] });
  }, { kind, file });

  const capture = async name => {
    await page.locator('#spatial-animation').evaluate(el => el.scrollIntoView({ block: 'start' }));
    await page.evaluate(() => new Promise(done => requestAnimationFrame(() => requestAnimationFrame(done))));
    const image = await app.evaluate(() => global.workspaceTest.capture());
    await fs.writeFile(path.join(output, name), Buffer.from(image, 'base64'));
  };

  try {
    const base = await api('reconstruct-side-example');
    await page.locator('#spatial-tab').click();
    await page.locator('#spatial-animation summary').click();
    const prop = base.objects.find(body => body.sourceId === 'surface');
    const end = prop.position.map((value, i) => value + [.4, .2, -.1][i]);
    for (const [i, axis] of ['x', 'y', 'z'].entries()) {
      await page.locator('#animation-' + axis).fill(String(end[i]));
    }
    await page.getByRole('button', { name: 'Set key', exact: true }).click();
    const beforePreview = await api('snapshot');
    await page.locator('#animation-preview').click();
    await wait('#animation-playhead', 'Frame 1');
    await setPlayhead(13);
    await wait('#animation-playhead', 'Frame 13');
    assert(await page.locator('#spatial-observe').isDisabled());
    const expectedMid = prop.position.map((value, i) => (value + end[i]) / 2);
    const playhead = await page.locator('#animation-playhead').textContent();
    assert.deepEqual(await api('snapshot'), beforePreview);
    assert(playhead.endsWith(expectedMid.map(value => value.toFixed(3)).join(', ') + ' m'), playhead);
    await capture('animation-timeline.png');
    await page.locator('#animation-play').click();
    await wait('#animation-playhead', 'Frame 25');
    await page.locator('#animation-save').click();
    await wait('#animation-status', 'Scene copy saved');
    const savedId = await page.locator('#spatial-scenes').inputValue();
    assert.notEqual(savedId, base.id);
    const saved = await api('read', { id: savedId });
    assert.deepEqual(await api('read', { id: base.id }), base);
    assert.deepEqual(saved.visualMeshes, base.visualMeshes);
    const preview = await api('animation-preview', { id: savedId });
    near(preview.positionsM.surface[12], expectedMid);
    near(preview.positionsM.surface.at(-1), end);

    await assert.rejects(api('animation-save', { id: savedId, sourceId: 'surface', keys: [
      { frame: 1, positionM: prop.position }, { frame: 1, positionM: end }
    ] }), /strictly increasing/);
    await assert.rejects(api('export-glb', { id: savedId }), /native Blender export/);
    const staticGlb = await api('export-glb', { id: base.id });
    assert(staticGlb.base64.length > 100);

    let measured, returnedId, older, legacyOpened = false, fileHashes = null;
    if (blender) {
      const nativeFile = path.join(output, 'fraynix-authored.blend');
      await choose('export', nativeFile);
      await page.locator('#spatial-export-blender').click();
      await page.getByText('Editable Blender scene saved:', { exact: false }).last().waitFor();
      const verify = path.join(__dirname, 'spatial-animation-blender.py');
      const verifyWith = async executable => {
        await exec(executable, ['--background', '--factory-startup', '--disable-autoexec', nativeFile,
          '--python-exit-code', '17', '--python', verify, '--', output], { timeout: 60000, windowsHide: true });
        const found = JSON.parse(await fs.readFile(path.join(output, 'blender-animation-evidence.json'), 'utf8'));
        for (const [frame, point] of Object.entries(found.samplesM)) near(point, preview.positionsM.surface[Number(frame) - 1]);
        assert.equal(found.triangles, base.reconstruction.triangles);
        return found;
      };
      measured = await verifyWith(blender);
      if (otherBlender) older = await verifyWith(otherBlender);
      const changed = path.join(output, 'edited-in-blender.blend');
      fileHashes = { authored: await hashFile(nativeFile), returned: await hashFile(changed) };
      await choose('import', changed);
      await page.locator('#spatial-import-blender').click();
      await page.getByText('Blender scene imported.', { exact: false }).last().waitFor();
      returnedId = await page.locator('#spatial-scenes').inputValue();
      const returned = await api('read', { id: returnedId });
      assert(returned.source.note.includes(fileHashes.returned));
      assert.deepEqual(returned.visualMeshes, base.visualMeshes);
      for (const body of base.objects) {
        const actual = returned.objects.find(object => object.sourceId === body.sourceId);
        assert.equal(actual.massKg, body.massKg);
        near(actual.size, body.size);
      }
      near((await api('animation-preview', { id: returnedId })).positionsM.surface.at(-1),
        [end[0] + .25, end[1], end[2]]);
      await page.locator('#animation-preview').click();
      await wait('#animation-playhead', 'Frame 1');
      await setPlayhead(25);
      await wait('#animation-playhead', '0.650, 0.200, -0.100 m');
      await capture('animation-blender-return.png');
      await page.locator('#animation-stop').click();
      const sceneCount = (await api('list')).scenes.length;
      await choose('import', path.join(output, 'unsupported-collider.blend'));
      await page.locator('#spatial-import-blender').click();
      await page.getByText('collider shape keys and mesh animation', { exact: false }).last().waitFor();
      assert.equal((await api('list')).scenes.length, sceneCount);
      if (legacyBlend) {
        await choose('import', legacyBlend);
        await page.locator('#spatial-import-blender').click();
        await page.waitForFunction(() => document.querySelector('#spatial-scenes').selectedOptions[0]?.textContent.includes('crate and obstacle'));
        const oldId = await page.locator('#spatial-scenes').inputValue();
        const legacy = await api('read', { id: oldId });
        assert.equal(legacy.translationAnimation, undefined);
        near(legacy.objects.find(x => x.sourceId === 'crate').position, [-2, .25, 0]);
        legacyOpened = true;
      }
    }

    await app.close();
    ({ app, page } = await launch(project, extra));
    assert.deepEqual(await api('read', { id: savedId }), saved);
    assert.deepEqual(await api('animation-preview', { id: savedId }), preview);
    if (returnedId) near((await api('animation-preview', { id: returnedId })).positionsM.surface.at(-1),
      [end[0] + .25, end[1], end[2]]);
    await fs.writeFile(path.join(output, 'animation-evidence.json'), JSON.stringify({
      fps: preview.fps, endFrame: preview.endFrame, startM: prop.position, endM: end, midpointM: expectedMid,
      fileHashes, blender: measured || null, olderBlender: older || null, originalKept: true, restartPassed: true,
      invalidInputRefused: true, animatedGlbRefused: true, previewDidNotMovePhysics: true,
      unsupportedColliderRefused: Boolean(blender), legacyStaticSceneOpened: legacyOpened
    }, null, 2) + '\n');
    console.log(JSON.stringify({ frames: preview.endFrame, measured, older, restartPassed: true }));
  } catch (error) {
    console.error(await page.locator('#animation-status').textContent().catch(() => null));
    throw error;
  } finally {
    await app.close().catch(() => {});
  }
}

main().catch(error => { console.error(error); process.exitCode = 1; });
