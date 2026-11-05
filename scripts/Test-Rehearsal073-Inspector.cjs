'use strict';
// Run against original exported episodes, not hand-built passing verdicts.
const fs = require('node:fs');
const assert = require('node:assert/strict');
const {inspect} = require('./Inspect-Rehearsal073.cjs');
const files = process.argv.slice(2);
assert(files.length, 'Supply one or more exported experiment replay JSON files');
const cases = files.map(file => ({file, ...inspect(JSON.parse(fs.readFileSync(file, 'utf8')))}));
const checks = [
  ['changed reconstructed position', r => {r.rebuilds.at(-1).samples[1].bodies[0].position[0] += 1;}],
  ['changed actual position', r => {r.actual.samples[1].bodies[0].position[0] += 1;}],
  ['undeclared component change', r => {
    const scene = JSON.parse(r.actual.definition.worldFrame.originalScene.sourceText);
    scene.objects.find(o => o.sourceId === 'B').friction += .1;
    r.actual.definition.worldFrame.originalScene.sourceText = JSON.stringify(scene);
  }],
  ['forged reconstruction coverage', r => {r.goal.rehearsal073.rebuild.sampleCoverage = .5;}],
  ['wrong child lineage', r => {r.goal.rehearsal073.childEpisode.lineage.parentEpisodeId = 'unrelated';}],
  ['wrong committed revision', r => {r.goal.worldCommit.revision += 1;}]
].map(([name, change]) => {
  const replay = JSON.parse(fs.readFileSync(files[0], 'utf8'));
  change(replay);
  assert.throws(() => inspect(replay), undefined, name);
  return {name, refused: true};
});
console.log(JSON.stringify({passed: true, cases, tamperChecks: checks}, null, 2));
