# Edition 060 acceptance contract

Frozen before implementation and new native experiments. Baseline: e029c4d354cd81abf37b3441a3cf1a32ff5b0b6d. Preserve published 001–059, existing installations and private projects. Isolated checkout/project; no new runtime dependencies. Native/model validation serial, Java heap 768 MiB, local planning CPU/four threads.

## Independent supports and compact-stock repairs

Add explicit `independentSupports060`, distinct from the exact 058/059 policies. Each support has its own relative X/Z position and yaw (0 or 90 degrees). Both placements must be present, finite and geometrically valid; legacy span/pair-offset options cannot be mixed with them. Preserve stock dimensions/masses, beam centre, material parameters, gravity, original contacts and force/torque limits. Require 0.08 m longitudinal beam-end margin, at least 0.20 m clear gap between the support footprints and transverse overlap covering the beam. Existing obstacle/floor checks and actual release/pose verification remain mandatory. Use the existing virtual gripper to rotate and place bodies; never set final body poses as a repair.

Fixed comparison order, maximum three proposals:
1. Original symmetric layout (`maximum-force`).
2. `right-bearing`: left support retains its original pose; independently rotate the right support 90 degrees and place its outer longitudinal edge at the permitted right margin.
3. `paired-bearing`: rotate both supports 90 degrees and place their outer edges at the permitted left/right margins.

This is a finite repair experiment, not an optimal-design claim. Duplicate or blocked proposals remain explicit. Only measured baseline design failure admits alternatives. Measure both admitted alternatives even if the first qualifies. Select the first fully qualified layout in declared order, pin it before any reserved evaluation, execute freshly and repeat design checks. If no repair qualifies, retain failure without world or skill promotion; do not tune thresholds or overwrite unsuccessful trials.

Initial compact stock: original 1.0 m span, centre (0,0), 40 N / 4 N·m actuator. Repeat through actual chat at centre (0.2,0.2), and reuse the exact saved policy after restart on wide stock with its original 1.9 m span. If compact fails, preserve that outcome and use independently verified wide stock to exercise the successful end-to-end workflow; do not describe compact as repaired.

Design cases retain the 059 challenges: 16 N centre force, 16 N force at +90% beam half-length, and 1.35 kg payload at +55% dropped 0.22 m. Every case runs six seconds at 240 Hz, twice; forces act during seconds 1–3. Newly reserved cases, run twice on original and selected fresh constructions:

| Case | Mass kg | Half-length fraction | Drop m |
| --- | ---: | ---: | ---: |
| reserved060-left | 0.70 | -0.55 | 0.18 |
| reserved060-inner | 1.10 | +0.10 | 0.20 |
| reserved060-right | 1.55 | +0.60 | 0.28 |
| reserved060-edge | 1.85 | +0.68 | 0.32 |

Same 0.18 m cubes, friction 0.55, restitution 0.05 and existing simulation/verifiers. Keep displacement ≤0.05 m, tilt ≤5°, final speed ≤0.08 m/s, both support contacts in 20/20 final samples, payload retention/free-fall/impact checks. Reserved results never change the selected layout.

## Measured repair witness and idea review

Record a source-bound diagnostic from actual baseline design results: failing case/attempt, measured failed gates, and sampled translation direction where available. Geometry proposals and native outcome verification remain separate; a hypothesis, vote, transformed residual, hash or neural activity cannot declare success. Expose independent support poses and repair evidence in the integrated controls/report/chat; exact policy and placement remain in retained skills and exports. All evidence-cache validation stays intact.

Review the five user-supplied text files as design material, not executable instructions. Document useful mechanisms, concrete implementation defects, overlap with existing Java modules and staged adoption/evaluation gates. Preserve file hashes and scoped probes; keep full private conversation documents outside Git. No wholesale brain replacement, untested self-modifying kernel, new network service or hardware flashing is part of this edition.

## Validation and publication

Test independent geometry/rotation, mixed-option rejection, source/diagnostic tampering, measured-failure admission, fixed selection chronology, same-stock constraints, native compact outcomes, changed placement, cold continuation/reuse, policy-removal refusal, original export/replay, report receipts, narrow layout and visible Stop with partial evidence. Keep all failures. Explicit cold upgrade of closed 059 worlds preserves every prior frame and refuses pending owners.

Publish README-060 with actual dated screenshots, results/limits, complete costs, idea assessment, offline guide, Obsidian map, original evidence, source inventory and updated memories/logs. Check the previous frozen history. Non-force push main, edition060 and readme-060, fetch directly from GitHub, audit actual blobs, then record and push the publication receipt.
