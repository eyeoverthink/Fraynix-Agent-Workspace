'use strict';

const assert = require('node:assert/strict');
const os = require('node:os');
const path = require('node:path');
const fs = require('node:fs/promises');
const {launch} = require('./calibration-integration.cjs');

async function main() {
  process.env.FRAYNIX_WORKSPACE_NO_MODELS = '1';
  const output = path.resolve(process.argv[2] || 'workspace-desktop/test-output/doorway');
  await fs.mkdir(output,{recursive:true});
  const project = await fs.mkdtemp(path.join(os.tmpdir(),'Fraynix doorway '));
  const fixtures = path.resolve(__dirname,'../../workspace-engine/src/test/resources/spatial/doorways');
  const extra = process.platform === 'linux' ? ['--no-sandbox','--disable-dev-shm-usage'] : [];
  let {app,page} = await launch(project,extra);
  const api = (route,args) => app.evaluate((_,request)=>global.workspaceTest.api(request.route,request.args),{route,args});
  const record = () => api('/api/spatial/carry-latest',{});
  const waitForStatus = status => page.locator('#carry-result[data-status="'+status+'"]').waitFor({timeout:30000});
  const capture = async name => {
    await page.locator('#spatial-carry').evaluate(panel=>{
      panel.scrollIntoView({block:'start'});
      return new Promise(resolve=>requestAnimationFrame(()=>requestAnimationFrame(resolve)));
    });
    await fs.writeFile(path.join(output,name),Buffer.from(await app.evaluate(()=>global.workspaceTest.capture()),'base64'));
  };
  async function openImport(file) {
    const stage = page.locator('#spatial-stage');
    if (!await stage.isVisible()) await page.locator('#spatial-tab').click();
    if (!await page.locator('#spatial-exchange').evaluate(el=>el.open)) {
      await page.locator('#spatial-exchange summary').click();
    }
    await page.locator('#spatial-glb').setInputFiles(path.join(fixtures,file+'.glb'));
    await page.waitForFunction(title=>document.querySelector('#spatial-scenes').selectedOptions[0]?.textContent.includes(title),
      file.endsWith('-turn') ? 'Turn fits' : 'Narrow stop');
    await assertAppears('#spatial-objects','Gate key');
    await page.locator('#spatial-exchange summary').click();
  }
  const assertAppears = async (selector,text) => {
    await page.waitForFunction(([selector,text])=>document.querySelector(selector)?.textContent.includes(text),[selector,text]);
  };
  const requestCarry = async goal => {
    await page.locator('#goal').fill(goal);
    await page.locator('#run').click();
  };

  try {
    await openImport('gate-a-turn');
    await requestCarry('Could you carry the selected prop through the doorway?');
    await waitForStatus('review');
    assert(await page.locator('#carry-approve').isEnabled());
    const pending = await record();
    const source = await api('/api/spatial/read',{id:pending.proposal.request.sceneId});
    await capture('review.png');

    await app.close();
    ({app,page} = await launch(project,extra));
    await page.locator('#spatial-tab').click();
    await page.locator('#spatial-carry summary').click();
    await page.locator('#carry-reopen').click();
    await waitForStatus('review');
    assert.deepEqual(await record(),pending);
    const worldPanel = page.locator('#persistent-world-panel');
    await worldPanel.evaluate(panel=>{panel.open=true;panel.querySelector('details').open=true;});
    await page.locator('#persistent-case').selectOption('dev035-three-body-chain');
    await page.locator('#persistent-load').click();
    await page.locator('#persistent-run:enabled').waitFor();
    assert(await page.locator('#carry-approve').isDisabled());
    await page.locator('#carry-reopen').click();
    await waitForStatus('review');
    assert.equal(await page.locator('#spatial-scenes').inputValue(),source.id);
    await assertAppears('#spatial-live','authored');
    await worldPanel.evaluate(panel=>{panel.open=false;});
    assert.equal(await page.locator('#spatial-objects').inputValue(),pending.proposal.request.objectId);
    const beforeApprove=(await api('/api/spatial/list',{})).scenes;
    await page.locator('#carry-approve').click();
    await waitForStatus('completed');
    const completed = await record();
    const actual = completed.verified;
    assert.equal(actual.route,pending.proposal.forecast.selectedRoute);
    assert.equal(actual.settleTicks,240);
    assert(actual.consecutiveStableTicks>=120);
    assert.deepEqual(await api('/api/spatial/read',{id:source.id}),source);
    assert.deepEqual((await api('/api/spatial/list',{})).scenes,beforeApprove);
    const firstCarry = actual.samples.findIndex(sample=>sample.phase==='carry' && sample.positionM[0]>-.1);
    assert(firstCarry>0);
    await page.locator('#carry-frame').evaluate((frame,index)=>{
      frame.value=index;
      frame.dispatchEvent(new Event('input',{bubbles:true}));
    },firstCarry);
    await capture('verified.png');

    await openImport('gate-a-blocked');
    await requestCarry('Take the selected key through the gate.');
    await waitForStatus('refused');
    assert(await page.locator('#carry-approve').isDisabled());
    const refused = await record();
    assert(refused.proposal.forecast.trials.every(trial=>trial.firstUnexpectedContact?.obstacle.endsWith('-jamb')));
    await assertAppears('#carry-evidence','First contact');
    const route = page.locator('#carry-route');
    await route.selectOption(refused.proposal.forecast.trials[1].route);
    await capture('refused.png');
    await page.locator('#carry-frame').focus();
    await page.keyboard.press('Home');
    await assertAppears('#carry-evidence','frame 0 · start');
    const originalPairSceneCount = (await api('/api/spatial/list',{})).scenes.filter(scene=>scene.title.startsWith('Gate ')).length;
    assert.equal(originalPairSceneCount,2);

    const matrix = [];
    for (const name of ['gate-b-turn','gate-b-blocked','gate-c-turn','gate-c-blocked','gate-a-visual']) {
      const bytes = await fs.readFile(path.join(fixtures,name+'.glb'));
      const scene = await api('/api/spatial/import-glb',{base64:bytes.toString('base64')});
      const request = {sceneId:scene.id,objectId:scene.objects.find(object=>object.sourceId==='key').id,displacementM:[3.3,0,0]};
      const review = await api('/api/spatial/carry-review',request);
      const reachable = name.endsWith('-turn');
      assert.equal(review.status,reachable?'review':'refused');
      if (reachable) {
        const result = await api('/api/spatial/carry-approve',{sceneId:scene.id,id:review.id,approvalSha256:review.approvalSha256});
        assert(result.verified.passed);
        matrix.push({name,record:result});
      } else {
        matrix.push({name,record:review});
      }
    }

    await fs.writeFile(path.join(output,'desktop-evidence.json'),JSON.stringify({
      platform:process.platform, engine:'Minie / Bullet', approvedAfterRestart:true,
      sameOriginalScene:true, approvalClearedOnWorldSwitch:true, gateScenesAfterBothActions:originalPairSceneCount,
      pending, completed, refused, matrix
    })+'\n');
    console.log(JSON.stringify({passed:true,route:actual.route,tick:actual.ticks,
      settledTicks:actual.settleTicks,positionErrorM:actual.positionErrorM,angleErrorDeg:actual.angleErrorDeg,
      colliderM:actual.minColliderM,visualEnvelopeM:actual.minVisualEnvelopeM,
      refusedContacts:refused.proposal.forecast.trials.map(trial=>trial.firstUnexpectedContact)}));
  } finally {
    await app.close().catch(()=>{});
  }
}

main().catch(error=>{console.error(error);process.exitCode=1;});
