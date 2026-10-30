// Independent, local-only behavioral probes. Never starts the reviewed application.
import { pathToFileURL } from 'node:url';
import path from 'node:path';
import { createRequire } from 'node:module';
import { execFileSync } from 'node:child_process';
import { writeFileSync } from 'node:fs';
import assert from 'node:assert/strict';

const root = path.resolve(process.argv[2] || '');
if (!process.argv[2]) throw new Error('Usage: node scripts/Review-Chandra.mjs <isolated-chandra-checkout>');
const expected = 'f96eae62343308b4faf8c1d34c490c07d232f69b';
const revision = execFileSync('git', ['rev-parse', 'HEAD'], { cwd: root, encoding: 'utf8' }).trim();
if (revision !== expected) throw new Error(`Expected reviewed revision ${expected}; found ${revision}`);
const load = relative => import(pathToFileURL(path.join(root, relative)).href);
const require = createRequire(path.join(root, 'package.json'));
const Database = require('better-sqlite3');
const { RSIEngine } = await load('server/vertiso/rsi.js');
const { generateIdentity } = await load('server/fraymus/core/identity.js');
const { signMessage, verifyMessage } = await load('server/fraymus/core/signing.js');
const { HermesFleet } = await load('server/fraymus/intelligence/hermes.js');

const db = new Database(':memory:');
const random = Math.random;
let evolution;
try {
  // No mutation occurs at .99, yet the candidate receives a positive random fitness offset.
  Math.random = () => 0.99;
  const rsi = new RSIEngine({ db, vertiso: null, broadcast: () => {} });
  const before = rsi.getGenome('assessment-fixture');
  const result = await rsi.evolve('assessment-fixture', {});
  evolution = {
    constantRandom: 0.99, inputMetrics: {},
    reportedImproved: result.improved, previousFitness: result.previousFitness,
    reportedFitness: result.fitness, mutationCount: result.mutations.length,
    genomeUnchanged: JSON.stringify(before) === JSON.stringify(rsi.getGenome('assessment-fixture')),
    reportedPopulationTested: result.populationTested,
    candidateTaskExecutions: 0,
  };
} finally { Math.random = random; db.close(); }

const identity = await generateIdentity('ephemeral-assessment-only');
const signed = await signMessage(identity, 'fixture-recipient', { text: 'fixture-only' }, 'genesis');
const signature = await verifyMessage(signed, identity.pubkey, 'genesis');
const fleet = new HermesFleet();
const skill = fleet.listSkills()[0];
const missing = await fleet.execute(skill.name, {});
assert.equal(evolution.reportedImproved, true);
assert.equal(evolution.genomeUnchanged, true);
assert.equal(evolution.mutationCount, 0);
assert.equal(signature.valid, false);
assert.equal(signature.checks.sig, false);
assert.equal(missing.status, 'no_executor');
// Output contains no identity keys, signatures, runtime state or personal content.
const report = {
  revision, node: process.version,
  scope: 'Pure local module calls with an in-memory database; no application, provider or tool execution started. This is not an OS network-egress test.',
  evolution, freshMessageVerification: signature,
  skillRegistry: { declaredCount: fleet.listSkills().length, missingExecutorStatus: missing.status },
};
if (process.argv[3]) writeFileSync(process.argv[3], JSON.stringify(report, null, 2) + '\n', 'utf8');
console.log(JSON.stringify(report, null, 2));
