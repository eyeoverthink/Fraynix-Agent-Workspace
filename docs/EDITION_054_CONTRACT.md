# Edition 054 · Geometry, obstacles and a simpler-control comparison

2026-11-03. Frozen before implementation, from published053 main121f22e. Work only in the isolated054 checkout and new synthetic practice projects. Preserve the normal045 Java installation/private project and all published001–053 snapshots. No new solver, model dependency, scheduler, physical robot, material-strength or weight-training claim.

Extend the existing three-part construction with explicitly sized rectangular stock. Java validates declared dimensions, support spacing, mass, fixed floor and static obstacles. Geometry and obstacle definitions remain immutable in each world's original scene. Subsequent goals change poses through the existing virtual force/torque controller, never by teleporting bodies. Keep the native controller, integration ledger, wrench/pose/release thresholds and world ownership/commit logic unchanged. Support verification for the new geometry uses its declared support height plus half beam thickness, with the existing ±0.05m height band, two actual support contacts, at least six final samples, speed below0.08m/s and released gripper. Additionally refuse any measured part/obstacle contact.

Plan a conservative overhead route above all current/planned bodies and static obstacles, with sphere-bounded rotational clearance and separate vertical pickup/lowering corridor checks. Refuse occupied pickup/target corridors, invalid spacing, unsupported dimensions or lift above4m before actuation. A barrier may be crossed above it; no claim of arbitrary path finding, tunnels or non-box geometry. Legacy051 kits retain their existing planner and verifier behavior. Changed implementation identity requires a new reviewed world/skill; do not silently migrate existing private runtime data.

Frozen comparison cases, all centre(0,0), force ceiling40N, torque4Nm:

| Case | Support size X/Y/Z (m) | Beam size X/Y/Z (m) | Support spacing (m) | Mass support/beam (kg) | Obstacle |
| --- | --- | --- | --- | --- | --- |
| Legacy053 | .4/.6/.6 | 2/.3/.4 | 1.3 | 1.2/.8 | none |
| Compact054 | .35/.45/.5 | 1.6/.25/.35 | 1.0 | 1.0/.8 | none |
| Wide054 | .45/.75/.65 | 2.6/.3/.5 | 1.9 | 1.5/1.2 | none |
| Barrier054 | same as Wide | same as Wide | 1.9 | 1.5/1.2 | fixed5×.8×.25m barrier centred at(0,.4,-.8) |
| Blocked054 | same as Wide | same as Wide | 1.9 | 1.5/1.2 | fixed.35×.8×.35m post at the first support target |

New stock starts at X[-2.2,-.4,2], Z=-2, resting on the floor, upright yaw0. Legacy stock stays exact. Named cases and all failures remain in the record; do not change dimensions/thresholds to erase a failed acceptance. These are declared acceptance cases, not held-out evidence.

Simpler control is one direct assembly at the admitted maximum force, with the SAME source, target, geometry-aware route, solver and success checks. The maximum-force candidate provides that measured baseline in the product report. An independent native test repeats that single attempt from the exact same source and verifies its original metrics against the candidate. Compare feasibility, ticks, force effort N·s, work J and measured native trial wall time. Charge comparison for ALL attempted trials plus the fresh selected execution. Do not count a failed trial's lower effort as a benefit, call impulse energy, equate simulated seconds with wall seconds, or claim superiority when the simpler policy ties or wins. Report provenance and costs even when comparison offers no advantage.

Integrate stock controls and visible obstacles into the existing Spatial assembly panel, chat route, versioned procedures and checked report. Baseline rows derive from original native evidence and are checked in the actual canvas. Keep review-before-moving, cold restart, changed-centre fresh-trial reuse, stale/tamper refusal, Stop and complete original exports. No mocked model/native success. Run native/model validation serially. Original failures remain archived.

Deliver README054, actual dated screenshots, measured case table, offline wide/narrow guide, source inventory, Obsidian note/map and updated memory/logs. Verify old snapshots, commit and push main/edition054/readme-054 non-forced, fetch GitHub back and audit every source/frozen/evidence blob. Next phases depend on measured results: broader routing and independent load tests, plus a frozen diagnostic-retention comparison; no automatic learning-promotion claim.

## Explicit cold 053 handoff (acceptance addendum before implementation)

The geometry extension changes the bound engine identity. Permit the existing explicit upgrade control to accept the exact published 053 engine identity, only for a closed world without a pending owner. Test with the original published synthetic 053 ledger archive copied into a new temporary project. Preserve every previous revision and the exact final frame; append one labelled compatibility revision without motion. Reads must not migrate; stale bindings, unknown identities and pending owners must refuse. Saved skills/checkpoints remain source-bound and require their existing review/recovery paths. Never open, migrate or overwrite the user's original review045 project. This adds compatibility checks; it does not alter any frozen geometry, physics threshold or measured case.
