'use strict';
const {test}=require('node:test'),assert=require('node:assert/strict');
const {webURL,command,ObservationLease,taskMode}=require('../policy.cjs');
test('native observer tools preserve ordinary web routing and expose bounded actions',()=>{
 const {planTools}=require('../policy.cjs');
 assert.equal(taskMode('Move the first-person observer in the native ramp scene').needsObserver,true);
 assert.equal(taskMode('Build a camera inventory app').needsScene,false);
 assert.equal(taskMode('Use the native 3D camera and inspect a block').needsCalibration,undefined);
 const tools=planTools(['observer.move','observer.look','observer.inspect','observer.capture']);
 assert.equal(tools.length,4);
 const move=tools.find(t=>t.function.name==='observer_move').function.parameters;
 assert.equal(move.additionalProperties,false);
 assert(move.required.includes('observationId'));
 assert.equal(move.properties.right.maximum,5);
 assert.equal(tools.find(t=>t.function.name==='observer_capture').function.parameters.properties.pngBase64,undefined);
 const open=planTools(['observer.open'])[0].function.parameters;
 assert.deepEqual(Object.keys(open.properties),['sceneId']);
});
test('observer completion requires a capture of the current pose version',()=>{
 const {observerStage}=require('../policy.cjs'),context={};
 assert.deepEqual(observerStage(context,'move and inspect').allowed,['scene.create','scene.read','observer.open']);
 context.observerEvidence={id:'observation-1',observer:{id:'observer-1',version:2}};
 assert(!observerStage(context,'move and inspect').allowed.includes('finish'));
 context.observerCapture={observerVersion:1};
 assert(!observerStage(context,'move and inspect').allowed.includes('finish'));
 context.observerCapture.observerVersion=2;
 assert(!observerStage(context,'move and inspect').allowed.includes('finish'));
 assert.match(observerStage(context,'move and inspect').stage,/Movement requested/);
 assert(!observerStage(context,'move and inspect').allowed.includes('observer.open'));
 context.observerMoved=true;assert(!observerStage(context,'move once and inspect').allowed.includes('observer.move'));
 context.observerInspected=true;
 assert.deepEqual(observerStage(context,'move once and inspect').allowed,['finish']);
 context.observerEvidence.observer.version=3;
 assert(!observerStage(context,'move once and inspect').allowed.includes('finish'));
});
test('calibration uses typed native tools without changing CSV application routing',()=>{
  const {planTools}=require('../policy.cjs');const mode=taskMode('Fit friction in the 3D ramp using training and validation datasets');
  assert.equal(mode.needsCalibration,true);assert.equal(mode.needsComparison,false);
  const names=['calibration.start','calibration.read','calibration.apply','calibration.report'];const definitions=planTools(names);
  assert.equal(definitions.length,4);assert.equal(definitions[0].function.parameters.additionalProperties,false);
  assert(definitions[0].function.parameters.required.includes('validationDatasetVersion'));
  assert.equal(taskMode('Build a fitting room inventory app').needsScene,false);
  const {calibrationApplyRequested}=require('../policy.cjs');
  assert.equal(calibrationApplyRequested('Fit the ramp and apply the accepted fit'),true);
  for(const goal of ['Fit the ramp; do not apply','Fit without apply','Never apply the fitted value','Fit only, report it'])assert.equal(calibrationApplyRequested(goal),false);
  const {calibrationStage}=require('../policy.cjs'),context={};
  assert(calibrationStage(context,'fit ramp').allowed.includes('calibration.start'));
  context.calibrationEvidence={id:'job',status:'running'};assert.deepEqual(calibrationStage(context,'fit ramp').allowed,['calibration.read','calibration.cancel']);
  context.calibrationEvidence={id:'job',status:'completed',evaluation:{accepted:true}};
  assert.deepEqual(calibrationStage(context,'fit and apply').allowed,['calibration.apply']);
  assert.deepEqual(calibrationStage(context,'fit, do not apply').allowed,['calibration.report']);
  context.calibrationEvidence.application={applyReceipt:{id:'receipt'}};assert.deepEqual(calibrationStage(context,'fit and apply').allowed,['calibration.report']);
  context.calibrationReportId='job';assert.deepEqual(calibrationStage(context,'fit and apply').allowed,['finish']);
  context.calibrationEvidence.status='failed';assert.throws(()=>calibrationStage(context,'fit ramp'),/failed/);
});
test('CSV web creation stays in artifact mode while native measurement comparisons use physical tools',()=>{
  assert.deepEqual(taskMode('Build a CSV spreadsheet app'),{needsScene:false,needsComparison:false});
  assert.deepEqual(taskMode('Create a ramp experiment and compare the saved dataset'),{needsScene:true,needsComparison:true});
  assert.deepEqual(taskMode('Run a 3D ramp'),{needsScene:true,needsComparison:false});
});
const {NetworkPolicy,localURL}=require('../network-policy.cjs');
test('a corrupted theme or invalid UTF-8 stops startup before code is loaded',()=>{
  const fs=require('node:fs'),path=require('node:path'),os=require('node:os');const {verifyRendererSources}=require('../source-integrity.cjs');const folder=fs.mkdtempSync(path.join(os.tmpdir(),'fraynix-source-guard-'));
  for(const name of ['pose-panel.js','contact-study-panel.js','force-learning-panel.js','map-panel.js','maps.css','procedural-panel.js','workflow-panel.js','creation-panel.js','creation.css','tracking-panel.js','tracking.css','perception-panel.js','perception.css','spatial-panel.js','spatial.css','index.html','style.css','measurement.css','app.js','scene-lab.js','measurement-panel.js','calibration-panel.js','calibration.css','observer-panel.js','observer.css','body-panel.js','scene-files.js','scene-video.js','brain-game.js','brain-game.css','world-panel.js','world-panel.css','goals-panel.js','goals-panel.css','data-panel.js','data-panel.css'])fs.writeFileSync(path.join(folder,name),'/* valid UTF-8 fixture */');
  assert.equal(verifyRendererSources(folder).checked,36);fs.writeFileSync(path.join(folder,'style.css'),Buffer.alloc(128));assert.throws(()=>verifyRendererSources(folder),/style.css.*NUL/);
  fs.writeFileSync(path.join(folder,'style.css'),'body{}');fs.writeFileSync(path.join(folder,'app.js'),Buffer.from([255]));assert.throws(()=>verifyRendererSources(folder),/app.js.*UTF-8/);
});
test('offline application policy permits loopback and rejects external navigation without dispatch',()=>{
  const policy=new NetworkPolicy(true);for(const u of ['http://127.0.0.1:12/a','http://localhost/a','http://[::1]/'])assert(localURL(u));
  for(const u of ['https://example.com','http://127.0.0.1.evil.test','http://user:secret@localhost/','file:///x'])assert(!localURL(u));
  assert.throws(()=>policy.require('https://example.com'),/Offline mode/);assert.equal(policy.status().blockedRequests,1);policy.require('http://127.0.0.1:123/a');
});
test('browser cannot navigate to host files or executable protocols',()=>{
  for(const url of ['file:///C:/secret.txt','javascript:alert(1)','data:text/html,hello','https://user:secret@example.com'])assert.throws(()=>webURL(url));
  assert.equal(webURL('https://example.com'),'https://example.com/');
});
test('model cannot invent tool handlers or smuggle extra executable fields',()=>{
  assert.throws(()=>command({tool:'shell.exec',args:{cmd:'anything'}}));
  assert.throws(()=>command({tool:'browser.observe',args:{},script:'anything'}));
  assert.equal(command({tool:'canvas.test',args:{steps:[]}}).tool,'canvas.test');
});
test('an old observation expires after navigation or action',()=>{
  const lease=new ObservationLease(),first=lease.create();lease.require(first);lease.invalidate();assert.throws(()=>lease.require(first));
  const second=lease.create();assert.notEqual(first,second);lease.require(second);
});

test('native body stages retain physical progress and never repeat a completed movement',()=>{
 const {bodyStage,planTools}=require('../policy.cjs');const job={};assert(bodyStage(job).allowed.includes('observer.open'));
 job.observerEvidence={id:'obs',observer:{id:'camera'}};assert(bodyStage(job).allowed.includes('body.grab'));
 job.bodyGrab={id:'grip',runId:'run',after:{position:[1,2,3]}};assert(bodyStage(job).allowed.includes('body.move'));assert(!bodyStage(job).allowed.includes('body.grab'));
 job.bodyMove={outcome:'reached'};assert.deepEqual(bodyStage(job).allowed,['body.release','body.cancel']);
 job.bodyRelease={runId:'run'};assert.deepEqual(bodyStage(job).allowed,['body.capture']);
 job.observerCapture={packet:{world:{runId:'run'}}};assert.deepEqual(bodyStage(job).allowed,['finish']);
 const move=planTools(['body.move'])[0].function.parameters;assert.equal(move.additionalProperties,false);assert.equal(move.properties.target.minItems,3);assert.deepEqual(move.required,['target']);assert.equal(move.properties.gripId,undefined);
});
test('body and file task modes stay separate from ordinary web artifact requests',()=>{
 assert.deepEqual(taskMode('Grab the Block in the native scene, lift it and release'),{needsBody:true,needsScene:true,needsComparison:false});
 assert.deepEqual(taskMode('Export a physics scene video'),{needsTransfer:true,needsScene:true,needsComparison:false});
 assert.equal(taskMode('Build an import/export CSV web app').needsScene,false);
});

test('physical result reporting uses native target error rather than model travel-distance prose',()=>{
 const {bodyReport}=require('../body-result.cjs'),move={status:'completed',runId:'r',outcome:'reached',targetErrorM:0.0000006263956,toleranceM:.08},release={status:'completed',runId:'r',endTick:295,after:{position:[-3.6,.55,.25],grip:{held:false}}},capture={packet:{world:{runId:'r',tick:295}},pngSha256:'abc'};
 const report=bodyReport(move,release,capture);assert(report.includes('Measured target error 6.26396e-7 m'));assert(!report.includes('0.790569'));assert(report.includes('Native constraint removed'));
 move.outcome='blocked-or-force-limited';move.targetErrorM=.29;assert(bodyReport(move,release,capture).includes('blocked-or-force-limited'));
});
test('physical result reporting refuses stale final frames or a remaining native grip',()=>{
 const {bodyReport}=require('../body-result.cjs'),move={status:'completed',runId:'r',outcome:'reached',targetErrorM:0,toleranceM:.08},release={status:'completed',runId:'r',endTick:295,after:{position:[0,1,0],grip:{held:false}}},capture={packet:{world:{runId:'r',tick:294}},pngSha256:'abc'};
 assert.throws(()=>bodyReport(move,release,capture),/matching completed/);capture.packet.world.tick=295;release.after.grip.held=true;assert.throws(()=>bodyReport(move,release,capture),/matching completed/);
});


test('world-goal routing keeps ordinary creation and direct body actions independent',()=>{
 const {planTools,catalog}=require('../policy.cjs');
 assert.equal(taskMode('Predict how this block moves, reach the target despite a disturbance, chart predicted versus actual motion, and save the successful procedure.').needsGoal,true);
 assert.equal(taskMode('Use the world model to reach 1.2 m').needsGoal,true);
 assert.equal(taskMode('Reuse the reach-target procedure').needsGoal,true);
 assert.equal(taskMode('Build a canvas chart web app').needsGoal,undefined);
 assert.equal(taskMode('Grab the native Block then release it').needsBody,true);
 const tools=planTools(['goal.start','goal.read','procedure.run','world.model']);assert.equal(tools.length,4);
 const start=tools.find(t=>t.function.name==='goal_start').function.parameters;assert.equal(start.additionalProperties,false);assert.equal(start.properties.targetM.maximum,1.8);assert.equal(start.properties.angleDeg.maximum,40);assert.equal(start.properties.friction.maximum,.3);
 assert(!catalog.some(t=>/^(?:goal\.)?(?:ack|claim)$/.test(t.name)));assert.throws(()=>command({tool:'goal.ack',args:{passed:true}}),/Unknown tool/);
});


test('explicit physical and retention requirements cannot disappear into model defaults',()=>{
 const {requestedPhysics}=require('../goals.cjs');
 assert.deepEqual(requestedPhysics('On a 24 degree slope with friction 0.18 reach target 1.2 metres despite a +0.35 m/s disturbance. Save the procedure as live-world-reach. Include current browser observation as context.'),{angleDeg:24,friction:.18,targetM:1.2,disturbanceMps:.35,browserContext:true,saveSkillName:'live-world-reach'});
 assert.deepEqual(requestedPhysics('Create a counter web app'),{});
});


test('motion data tasks use their registered family while CSV web creation and physics stay separate',()=>{
 const {planTools}=require('../policy.cjs'),{requestedData,goalSummary}=require('../goals.cjs');
 assert.equal(taskMode('Analyze my imported motion CSV and save the successful data procedure').needsData,true);
 assert.equal(taskMode('Summarize the selected motion dataset').needsData,true);
 assert.equal(taskMode('Reuse the live-motion-data procedure on the selected motion dataset').needsData,true);
 assert.equal(taskMode('Build an interactive CSV analysis web app').needsData,undefined);
 assert.equal(taskMode('Use the physics world model to predict block motion').needsData,undefined);
 const bound=planTools(['procedure.run'],{family:'data',procedureBound:true})[0].function.parameters;assert.equal(bound.properties.name,undefined);assert.equal(bound.properties.angleDeg,undefined);assert(bound.properties.maxGapS);
 const schema=planTools(['data.start'])[0].function.parameters;assert.equal(schema.additionalProperties,false);assert.equal(schema.properties.maxGapS.minimum,.000001);
 assert.deepEqual(requestedData('Analyze motion data with gap limit 0.25 and speed limit 0.4. Save the procedure as source-analysis.'),{maxGapS:.25,speedLimitMps:.4,saveSkillName:'source-analysis'});
 const summary=goalSummary({procedure:'motion-data-report-v1',status:'completed',reason:'Verified',metrics:{rowCount:5,totalTravelM:.18,meanSpeedMps:.18,gapFlags:1,speedFlags:1,skippedSourceRows:0,qualityPassed:false}});assert(summary.includes('Quality flags'));assert(!summary.includes('endpoint'));
});

test('spatial chat selects its own typed task family and retains exact vector intent',()=>{const {requestedSpatial}=require('../goals.cjs');assert.equal(taskMode('Predict and move the imported scene crate').needsSpatial,true);assert.equal(taskMode('Spatial displacement [4,0,0]').needsGoal,true);assert.deepEqual(requestedSpatial('Spatial displacement [4, 0, -1], save the procedure as route-proof'),{displacementM:[4,0,-1],saveSkillName:'route-proof'});const schemas=require('../policy.cjs').planTools(['spatial.start','procedure.run'],{family:'spatial',procedureBound:true});for(const item of schemas){assert(item.function.parameters.properties.displacementM);assert(!item.function.parameters.properties.targetM);assert(!item.function.parameters.properties.datasetId);}});

test('browser perception has a separate typed task family',()=>{assert.equal(taskMode('Outline people on the webpage').needsPerception,true);assert.equal(taskMode('Track the cup in the browser').needsPerception,true);assert(!taskMode('Build a browser game web app').needsPerception);assert(!taskMode('Explain browser perception').needsPerception);assert(!taskMode('Create a perception web app').needsPerception);assert(!taskMode('Follow the link in the browser').needsPerception);});
test('perception procedural schema keeps native and dataset fields out',()=>{const {planTools}=require('../policy.cjs');for(const t of planTools(['perception.start','procedure.run'],{family:'perception',procedureBound:true})){assert(t.function.parameters.properties.targetLabel);assert(!t.function.parameters.properties.targetM);assert(!t.function.parameters.properties.datasetId);assert(!t.function.parameters.properties.displacementM);if(t.function.name==='procedure_run'){assert(!t.function.parameters.properties.lessonName);assert(!t.function.parameters.properties.mode);}}});


test('tracking evaluation is a distinct task with fixed host-bound options',()=>{
 const {planTools}=require('../policy.cjs');
 assert.equal(taskMode('Compare tracker profiles on the selected tracking sequences and promote only if fixed gates pass').needsTracking,true);
 assert.equal(taskMode('Roll back the tracker to the baseline').needsTracking,true);
 assert(!taskMode('On this webpage detect and follow the person').needsTracking);
 assert(!taskMode('Build a web app for tracking packages').needsTracking);
 const tool=planTools(['tracking.start'])[0];assert.deepEqual(Object.keys(tool.function.parameters.properties),['autoPromote']);assert.equal(tool.function.parameters.additionalProperties,false);
});
