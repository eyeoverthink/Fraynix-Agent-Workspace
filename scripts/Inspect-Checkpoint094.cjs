'use strict';
const fs=require('node:fs'),path=require('node:path'),crypto=require('node:crypto'),assert=require('node:assert/strict');
const {inspectProcedure,inspectCommit}=require('./Inspect-Procedures093.cjs'),{canonical}=require('./Inspect-Spatial040.cjs'),{unpack}=require('./Inspect-Counterfactual045.cjs');
const repo=path.resolve(__dirname,'..'),out=path.join(repo,'build/ui094-checkpoint'),read=p=>JSON.parse(fs.readFileSync(p,'utf8')),sha=b=>crypto.createHash('sha256').update(b).digest('hex'),hash=x=>sha(canonical(x));
const r=read(path.join(out,'result.json'));assert(r.passed);assert.equal(r.checks.length,2);
const saved=fs.readFileSync(path.join(r.snapshot.path,r.snapshot.checkpointRelative));assert.equal(sha(saved),r.originalCheckpointSha256);
// Recompute the declared invalid input; this is not a replacement motion observation.
const mutation=JSON.parse(saved);mutation.payload.physics.tick+=12;assert.equal(sha(JSON.stringify(mutation)),r.mutatedCheckpointSha256);
for(const c of r.checks){assert(c.error.includes('checksum/version'));assert.equal(c.goal.status,'interrupted');assert.deepEqual(c.before,c.after);assert.equal(c.after.expectedRevision,0);assert.equal(c.after.pending.goalId,r.snapshot.goalId);assert.equal(c.job.data.resumes,0);assert(c.job.data.recoveryError.includes('checksum/version'));assert.equal(hash(c.job.data),c.job.sha256);}
assert.equal(r.checks[1].checkpointSha256,r.originalCheckpointSha256);
const x=read(path.join(out,'restored-originals.json')),root=path.join(r.restoredProject,'world-procedures-079'),result=inspectProcedure(x,root);assert(result.success);inspectCommit(x,root,r.goal);assert(r.fullVerify.passed);assert.equal(r.goal.worldCommit.revision,1);assert.equal(r.goal.resumes,1);
const c=x.cost080;assert.equal(c.eligible,false);assert.equal(c.totalMillis,0);assert(!c.clock089);assert.equal(c.jobSha256,hash(x.job));assert.deepEqual(c.goal.worldCommit,r.goal.worldCommit);
const checkpoint=JSON.parse(saved),n=checkpoint.payload.run,originals=new Map(x.originals.map(p=>{const a=unpack(p);return[a.id,a];})),current=originals.get(n.id);assert.deepEqual(current.samples.slice(0,n.samples.length),n.samples);assert(current.events.some(e=>e.kind==='resume'&&e.checkpointSha256===checkpoint.sha256));
const analysis={passed:true,source:r.source,goalId:r.goal.id,negativePhases:r.checks.map(c=>c.phase),restoredTask:result,committedPrefixSamples:n.samples.length,revision:r.goal.worldCommit.revision,scope:'Separate copies of one stopped synthetic task. Corruption and checkpoint-only restoration refuse motion; exact full stopped-state backup explicitly resumes and verifies. Invalid input digest recomputed from declared mutation. No automatic quarantine clearing or power-loss claim.'};
fs.writeFileSync(path.join(repo,'build/checkpoint094-analysis.json'),JSON.stringify(analysis,null,2));console.log(JSON.stringify(analysis));
