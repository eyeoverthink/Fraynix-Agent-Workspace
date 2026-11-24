
'use strict';

const assert = require('node:assert/strict');
const fs = require('node:fs/promises');
const os = require('node:os');
const path = require('node:path');
const { execFile } = require('node:child_process');
const { promisify } = require('node:util');
const { launch } = require('./calibration-integration.cjs');

function witness(axis, row, column, value) {
  const samples = Array(256).fill(-2);
  samples[row * 16 + column] = value;
  return { axis, origin: 'positive-grid-face', unit: 'um', samples };
}

async function main() {
  process.env.FRAYNIX_WORKSPACE_NO_MODELS = '1';
  const output = path.resolve(process.argv[2] || 'build/visual-side223');
  const fixture = path.resolve(__dirname, '../../workspace-engine/src/main/resources/spatial/reconstruction/side-depth.json');
  const input = JSON.parse(await fs.readFile(fixture, 'utf8'));
  const project = await fs.mkdtemp(path.join(os.tmpdir(), 'Fraynix perpendicular depth '));
  const extra = process.platform === 'linux' ? ['--no-sandbox', '--disable-dev-shm-usage'] : [];
  if (process.env.FRAYNIX_TEST_NO_WEBGL === '1') extra.push('--disable-gpu', '--disable-software-rasterizer');
  const blender = process.env.FRAYNIX_TEST_BLENDER;
  await fs.mkdir(output, { recursive: true });

  let { app, page } = await launch(project, extra);
  const api = (method, args = {}) => app.evaluate((_, request) =>
    global.workspaceTest.api('/api/spatial/' + request.method, request.args), { method, args });
  const build = depthViews => api('reconstruct', {
    base64: Buffer.from(JSON.stringify({ ...input, depthViews })).toString('base64')
  });
  const status = text => page.waitForFunction(text =>
    document.querySelector('#spatial-reconstruct-result')?.textContent.includes(text), text, { timeout: 12000 });
  const save = async (id, name) => {
    const file = await api('export-glb', { id });
    await fs.writeFile(path.join(output, name + '.glb'), Buffer.from(file.base64, 'base64'));
    return file.sha256;
  };

  try {
    const z = input.depthViews.filter(grid => grid.axis.endsWith('z'));
    const opposed = await build(z);
    const side = await build([...z, input.depthViews.find(grid => grid.axis === '+x')]);
    assert.equal(opposed.reconstruction.voxels, 614);
    assert.equal(side.reconstruction.voxels, 606);
    await save(opposed.id, 'opposed-only');

    await page.locator('#spatial-tab').click();
    await page.locator('#spatial-reconstruct summary').click();
    await page.locator('#spatial-reconstruct-side-example').click();
    await status('44 cells carved by depth from 6 views');
    const id = await page.locator('#spatial-scenes').inputValue();
    const full = await api('read', { id });
    assert.equal(full.reconstruction.voxels, 606);
    assert.equal(full.reconstruction.meshSha256, side.reconstruction.meshSha256);
    assert.notEqual(full.reconstruction.meshSha256, opposed.reconstruction.meshSha256);

    await page.locator('#spatial-reconstruct').evaluate(el => el.scrollIntoView({ block: 'start' }));
    await page.evaluate(() => new Promise(done => requestAnimationFrame(() => requestAnimationFrame(done))));
    const webglPreviewAvailable = await page.locator('#spatial-stage canvas').count() > 0;
    if (process.env.FRAYNIX_TEST_NO_WEBGL === '1') assert.equal(webglPreviewAvailable, false);
    if (!webglPreviewAvailable) {
      await page.locator('#spatial-stage').getByText('The 3D preview is unavailable on this display.', { exact: false }).waitFor();
    }
    const image = Buffer.from(await app.evaluate(() => global.workspaceTest.capture()), 'base64');
    await fs.writeFile(path.join(output, 'fraynix-three-recesses.png'), image);
    await page.locator('#spatial-reconstruct-export').click();
    await page.getByText('Checked GLB saved to', { exact: false }).last().waitFor();
    const sha = await save(id, 'three-recesses');

    const conflictPath = path.join(output, 'cross-axis-conflict.json');
    const xWitness = witness('+x', 11, 5, 450000);
    const zWitness = witness('+z', 11, 9, 250000);
    await build([xWitness]);
    await build([zWitness]);
    await fs.writeFile(conflictPath, JSON.stringify({ ...input, depthViews: [xWitness, zWitness] }));
    await page.locator('#spatial-reconstruct-file').setInputFiles(conflictPath);
    await status('first hit at grid cell 9, 4, 10 (+z)');
    assert.equal((await api('list')).scenes.length, 5);
    assert.equal(await page.locator('#spatial-scenes').inputValue(), id);

    const reversed = await build([...input.depthViews].reverse());
    assert.deepEqual(reversed.reconstruction.depth, full.reconstruction.depth);
    assert.equal(reversed.reconstruction.meshSha256, full.reconstruction.meshSha256);
    assert.notEqual(reversed.reconstruction.sourceSha256, full.reconstruction.sourceSha256);

    let independentBlender = null;
    if (blender) {
      const script = path.join(__dirname, 'spatial-side-depth-blender.py');
      await promisify(execFile)(blender, ['--background', '--factory-startup', '--disable-autoexec',
        '--python-exit-code', '17', '--python', script, '--', 'verify', output],
      { timeout: 90000, windowsHide: true });
      independentBlender = JSON.parse(await fs.readFile(path.join(output, 'side-depth-blender-evidence.json'), 'utf8'));
      assert(independentBlender.passed);
    }

    await app.close();
    ({ app, page } = await launch(project, extra));
    assert.deepEqual(await api('read', { id }), full);
    assert.equal((await api('export-glb', { id })).sha256, sha);
    const source = path.join(project, 'spatial', 'reconstruction-sources', id + '.json');
    const changed = JSON.parse(await fs.readFile(source, 'utf8'));
    changed.depthViews.find(grid => grid.axis === '+x').samples[0] = -2;
    await fs.writeFile(source, JSON.stringify(changed));
    await assert.rejects(api('export-glb', { id }), /original visual reconstruction/);
    assert.equal((await api('export-glb', { id: opposed.id })).base64.length > 0, true);

    await fs.writeFile(path.join(output, 'side-depth-evidence.json'), JSON.stringify({
      platform: process.platform, opposedOnly: opposed.reconstruction, withSide: side.reconstruction,
      allFaces: full.reconstruction, exportSha256: sha, independentBlender,
      webglPreviewAvailable, crossAxisConflictRefused: true, orderIndependentMesh: true, restoredAfterRestart: true,
      changedSourceRefused: true
    }, null, 2) + '\n');
    console.log(JSON.stringify({
      opposedCells: opposed.reconstruction.voxels, recoveredCells: full.reconstruction.voxels,
      views: full.reconstruction.depth.views.length, independentBlender
    }));
  } catch (error) {
    console.error((await page.locator('#spatial-reconstruct-result').textContent().catch(() => null)) || 'No reconstruction status');
    throw error;
  } finally {
    await app.close().catch(() => {});
  }
}

main().catch(error => { console.error(error); process.exitCode = 1; });
