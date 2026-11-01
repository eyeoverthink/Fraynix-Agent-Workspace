# Versioned native contact controllers

Edition033 implementation contract. [Current edition](../README-033.md),
[previous force-cap experiment](../README-032.md),
[pre-measurement suite and gates](CONTACT_033_FROZEN_CONTRACT.json),
[candidate source lock](CONTACT_033_CANDIDATE_LOCK.json).

## Ownership and physical boundaries

The original031 PushPhysics/Store/Checkpoint/Verify/Report and032
PushLearning/Store/Report remain byte-identical. `PushRuntime` routes ordinary
tasks to the baseline and explicit evaluated tasks to the separate
`solver-contact-controller-033-v1`. No direct crate pose or velocity control:
the actuator applies forces only to the pusher and native physics resolves
contact movement. Both arms retain8N initial/32N maximum,240Hz,60s total,
15s per leg, two recoveries,0.035m internal endpoint,0.1m final endpoint and
0.2m/s final speed. No new library, weight, cloud, Docker or WSL dependency.

The candidate reads actual persistent native solver manifold points, distance
and positive normal impulse. Each committed observation includes these points
and the original transient probe count. `ContactVerify` independently
reconstructs qualifying contact counts and checks method/finite values, before
the original PushVerify outcome/force/ledger audit. Sampled normal impulses
are diagnostic; pusher effort is N·s and work is J. They are distinct measures.

The new bounded lateral feedback uses measured crate/target error while
retaining the original pusher actuator, planning and timing limits. Neural
weight training, photographic object recognition, arbitrary manipulation and
calibrated physical robot control are separate unimplemented claims.

## Development, measurement and activation

Commit c461a97 freezes eight now-seen032 development cases and eight
different033 evaluation cases before candidate measurement. Seven cases in
each split are feasible; one deliberately exceeds the force budget. Commit
57dec5e locks the candidate's exact source after first development and before
different reserved evaluation. Those gates and the locked ContactPhysics
source remain unchanged after inspection.

`ContactStudyStore` records32 actual native trials with stable IDs, original
run hashes, controller version and sensor/outcome audits. Sixteen seen paired
development trials precede the persisted candidate identity pin; sixteen
different reserved paired trials follow. Explicit compatible recovery keeps
completed trials and their original identity rather than repeating them or
using evaluation outcomes to choose new settings.

Acceptance requires all feasible development and evaluation tasks, additional
reserved success, at least2s mean penalized-time gain, matched-success effort
ratio≤1.1, no baseline-success regression and correct failure of both physical
negatives. The original failed-time score is60s. Effort is gated only on
matched successful evaluation pairs and also reported across all cases. This
definition was frozen before measurement; do not substitute a later objective.

The first native result is baseline2/7 versus candidate7/7 feasible reserved
tasks, both negatives failing, mean penalized-time gain20.640625s, matched
successful effort ratio0.9613100704593242 across two pairs, all-case effort
ratio0.8451998404027327. These cases are now seen. New claims need different
frozen evaluation conditions; do not repeatedly tune against these cases.

The report retains all16 paired rows/144 cells and both chart lines in three
modes. The actual Chromium document's complete rows, original decision data
and SVG coordinates are compared independently to native results (original
0.002 SVG-unit precision contract). Only a completed passing native decision
and genuine view receipt permit explicit profile activation. Activation binds
code, suite, decision and view hashes; rollback restores the exact prior ID.
The local model cannot supply cases, gates, source IDs or success flags to
`contact.compare`; its schema is empty. Chat completion does not activate.

## Task and retained skill versions

Fresh tasks opt in visibly with `useContactController:true`; ordinary tasks
preserve baseline routing. The Java runner binds the actual active profile
and checks evaluated native identity. A candidate task preserves original
8/32N conditions; incompatible force overrides are refused. The goal stores
its exact profile and native implementation. An in-flight task retains that
binding even after a later registry change.

A retained pushing skill stores its source controller policy and defaults.
Its exact controller must be active before reuse. Rollback cannot silently
replace the controller in a candidate skill. The actual Skills form displays
version/code provenance, locks the validated force settings and allows fresh
imported crate/pusher roles. Every execution plans actual geometry and checks
new native evidence and six chart modes before declaring completion.

`ContactCheckpoint` atomically records both body poses/quaternions/linear and
angular velocities, full controller/plan/force/work state and exact committed
trace. Source, runtime/code and payload hashes guard restore. Explicit Resume
continues the same run, and corrupt/incompatible evidence is preserved and
refused. Exact initialization is tested; bit-identical future contact-cache
evolution across machines is not promised.

## Lossless reports and exports

The actual long candidate native run exceeds the existing general512K file
cap. `WorkspaceStore.writeNativePushOnce` is package-private, requires an
independently passing actual native run, constructs the unchanged registered
PushReport internally, and bounds that transport to2MB per file/4MB total.
The public creation API retains512K per file/2MB total; input flags cannot
grant the internal budget. Native report files, original gzip/hash, restored
bytes and durable identity are independently tested. No physical/promotion
limit was increased to solve this storage failure.

The comparison export preserves the original job plus all32 native runs as
gzip/base64 with exact raw byte count and SHA-256. Each raw native trial has
its original committed hash. The first headless measurement is archived
separately as exact original-byte entries; that archival transport is labelled
as created afterward, distinct from the genuine app export. Failures remain
in both records. Private runtime data and model weights remain outside Git.

## Next acceptance gate

Keep033 as regression evidence. Freeze different contact transitions,
orientations and multi-body cases before development/evaluation separation.
Increase the number of matched successful pairs before a broad efficiency
claim. Qualify rendered state, image detections and calibrated observations
separately. A useful learned controller must beat the correct simple baseline,
retain improvement after restart and preserve recovery/rollback evidence.
