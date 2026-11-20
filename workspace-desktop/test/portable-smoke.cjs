
'use strict';

const assert = require('node:assert/strict');
const fs = require('node:fs/promises');
const os = require('node:os');
const path = require('node:path');
const {launch} = require('./calibration-integration.cjs');

async function main() {
  const output = path.resolve(process.argv[2] || 'build/portable-smoke');
  await fs.mkdir(output, {recursive:true});
  const project = await fs.mkdtemp(path.join(os.tmpdir(), 'Fraynix portable workspace '));
  process.env.FRAYNIX_WORKSPACE_NO_MODELS = '1';
  process.env.FRAYNIX_WORKSPACE_PERSISTENT = '0';
  const args = process.env.FRAYNIX_WORKSPACE_CI_CHROMIUM === '1' && process.platform === 'linux' ? ['--no-sandbox'] : [];
  const checks = [];
  try {
    const {app,page} = await launch(project,args);
    try {
      const api = (route,input) => app.evaluate((_,[route,input])=>global.workspaceTest.api(route,input),[route,input]);
      const state = await api('/api/state');
      assert.equal(state.models.length,0);
      await page.getByRole('button',{name:'Run guided proof'}).click();
      await page.getByRole('status').filter({hasText:'Guided proof passed'}).waitFor({timeout:60_000});
      checks.push('Java connection, local browser interaction, creation canvas and guided verification');
      const screenshot = await app.evaluate(()=>global.workspaceTest.capture());
      await fs.writeFile(path.join(output,'workspace.png'),Buffer.from(screenshot,'base64'));

      const scene = await api('/api/scene/save',{title:'Portability ramp',parameters:{angleDeg:25,friction:0}});
      const ramp = await api('/api/experiment/run',{id:scene.id,expectedVersion:scene.version,seconds:0.75});
      assert.equal(ramp.evaluation.passed,true);
      assert.equal(ramp.samples.at(-1).tick,180);
      checks.push('Native Bullet physics passes the independent incline reference');

      const original = await api('/api/game/open-relay',{});
      assert.equal(original.state.visibleWindow,false);
      assert.equal(original.state.editorInitialized,false);
      const stepped = await api('/api/game/command',{action:'step',ticks:40,horizontal:1,jump:false});
      assert.equal(stepped.state.tick-original.state.tick,40);
      assert(stepped.state.player.x > original.state.player.x);
      assert.notEqual(original.pngSha256,stepped.pngSha256);
      const frame = Buffer.from(stepped.pngBase64,'base64');
      assert.equal(frame.subarray(0,8).toString('hex'),'89504e470d0a1a0a');
      assert.equal(frame.readUInt32BE(16),960);
      assert.equal(frame.readUInt32BE(20),540);
      await fs.writeFile(path.join(output,'game-frame.png'),frame);
      checks.push('Hidden native renderer loads Relay Garden and renders fixed-step movement');
    } finally { await app.close(); }

    process.env.FRAYNIX_WORKSPACE_PERSISTENT = '1';
    const {app:reopened,page:ownerPage} = await launch(project,args);
    try {
      const state = await reopened.evaluate(()=>global.workspaceTest.api('/api/agent042/state',{}));
      assert(state.pid>0 && state.bootId);
      const owner = await reopened.evaluate(()=>global.workspaceTest.api('/api/state'));
      assert(owner.artifacts.length>0);
      checks.push('Persistent Java owner starts and reopens the saved project');
      const stopped = await ownerPage.evaluate(()=>window.workspace.request({operation:'agent042',command:'stop',args:{}}));
      assert(stopped);
    } finally { await reopened.close().catch(()=>{}); }
  } finally {
    await fs.copyFile(path.join(project,'test-desktop-process.log'),path.join(output,'desktop.log')).catch(()=>{});
    await fs.copyFile(path.join(project,'engine.log'),path.join(output,'engine.log')).catch(()=>{});
    const nativeRoot = path.join(project,'embedded-game','original-content');
    for (const session of await fs.readdir(nativeRoot).catch(()=>[])) {
      for (const name of ['runtime.log','runtime-errors.log']) {
        const file = path.join(nativeRoot,session,name);
        await fs.copyFile(file,path.join(output,'game-'+session+'-'+name)).catch(()=>{});
      }
    }
  }
  const result = {passed:true,platform:process.platform,architecture:process.arch,checks};
  await fs.writeFile(path.join(output,'result.json'),JSON.stringify(result,null,2)+'\n');
  console.log(JSON.stringify(result));
}

main().catch(error=>{console.error(error);process.exitCode=1;});
