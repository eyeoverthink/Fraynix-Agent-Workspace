# Persistent native worlds — edition035

[README035](../README-035.md) · [Previous imported pose path](MULTI_OBJECT_POSES.md) · [Original evidence](readmes/035/verification.json)

`persistent-world-arrange-v1` continues a versioned declared box world across
separate goals. It preserves stable body IDs, positions, quaternions, linear and
angular velocities. Physics pauses between requests; native solver contact caches
are reconstructed. Exact kinematic cold initialization is measured, rather than
claimed equivalent to an uninterrupted warm-start solver. This is a simulated
virtual force/torque gripper, not a physical robot, calibrated room or LLM training.

## Ownership and transitions

`WorldFrames` separates immutable original source geometry from evolving body
state. `WorldLedger` owns hash-chained revisions, an atomic forced-write envelope,
cross-process file locks, observed-head compare-and-set and pending goal/run
ownership. Read/mutation reloads validate the current envelope and lineage.
Hashes detect altered bytes and lineage; they are not cryptographic signatures.

The Java goal freezes revision/targets/code/plan, saves the goal, reserves its
world and starts `WorldNativeStore`. Actual native success and `WorldVerify`
audit precede an original `WorldReport`. Genuine Chromium receipt is checked
before appending the next world revision. Native evidence and report identity
make commit idempotent under the same goal: replaying its acknowledgement does
not repeat forces or create another revision. Retained skills follow commit.

The native path uses separate035classes; protected034Pose and older Push/Contact/
Map sources remain unchanged. `WorldPhysics` applies only bounded central force
and torque to the currently selected body. No pose/velocity writes occur outside
initialization or compatible explicit cold restore. All dynamic bodies stay
physical, including unselected ones. Final pose/speed/protected-body gates remain
the034values. Body-state initialization must agree within2e-6m/velocity and
.0001degree; initial-state tampering is independently rejected.

Failed/interrupted/cancelled attempts do not advance verified head. A pending
owner blocks new goals in that world. Resume restores the same native run and
controller/prefix. Explicit restore appends a **nonphysical state operation**,
records any abandoned attempt, preserves prior revisions and returns to the
selected verified kinematics. Stop alone never silently resets a world.

## Application surface

Spatial's persistent-world panel creates/selects worlds, observes/confirms a
revision, edits targets, continues/verifies, selects history, explicitly restores
and exports. Reopening Spatial preserves its active world view. Authored-source
move controls are inactive while that world is selected. Older native tools
remain separate, executable paths. The Skills form names the world/revision and
pins exact035native version/defaults; each reuse must pass a fresh report.

`world.observe` returns current verified kinematics and separately identified
uncommitted native observations. It explicitly refreshes the current chat request
binding. `world.arrange` and procedure reuse accept semantic targets/limits;
world/revision/hash are host-bound. Models cannot send code, gates, commit or
success flags. Java owns physical timing, original verification and advancement.
World visibility uses actual oriented-box state/rays, not photographic recognition.

The local API includes `/api/world/create`, `list`, `read`, `observe`, `visibility`,
`preview`, `start`, `restore`, `run`, `export` and `export-run`. Pending/observed
heads remain separate; no endpoint lets the model directly claim a revision.

## Frozen measurements

Four development/four different reserved sequential definitions and gates were
committed9efd27f before development. Eight core sources locked dccd93c after
development and before reserved trials. The first reserved result:8verified goal
commits,1actual force-negative/no commit and1blocked-next refusal. Core source and
gates remain unchanged after inspection. Exact18original result/history files are
archived separately from actual UI/model exports. Cases are now seen regressions;
this is continuity validation, not a trained-policy comparison against034.

Native limits:240Hz,60N/4Nm,60simulated seconds/12per stage, .1m/5degree endpoint,
.2m/s/.2rad/s, .05m/5degree protected drift. Near-upright settled starts preserve
their actual height within.05m and quaternion x/z absolute sum≤.043; they are not
snapped onto an ideal floor. Supported geometry remains bounded flat-floor boxes.

## Full history and bounds

`WorldExport` includes complete ledger and every referenced native attempt,
including failed/cancelled attempts. It re-audits committed native evidence and
canonical hashes. Gzip transport retains original raw bytes/size/SHA-256. The
offline Inspector checks package hashes and references and explicitly does not
claim to rerun Java physics. Individual native attempts can also be exported.

```powershell
node scripts/Inspect-WorldExport.cjs docs/readmes/035/examples/persistent-world.json
```

Current bounds:16worlds,64revisions/world,32persistent-native attempts/project,
64shared goals,32procedures/eight versions/name;20MBtransport/256MBraw world
export. Oversize is refused, never silently pruned. Preserve native evidence when
archiving/rolling projects forward. No new dependencies/models/cloud/Docker/WSL.

Next proposed036: calibrated observations as proposed revisions, measured
prediction discrepancy, identity/scale/uncertainty validation and different frozen
sequences with delays, missing observations and disturbances. Keep simulation,
measurement and inference distinct; no such sensor fusion is claimed in035.
