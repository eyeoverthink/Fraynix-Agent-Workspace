# Native experience and force-profile evaluation

Edition032 keeps the031 native controller and introduces a measured experiment
around one bounded parameter: the initial pushing-force cap. Java owns the frozen
design, actual native trials, training-only selection, reserved comparison,
durability, report verification and reversible profile registry. Existing local
chat, Spatial rendering, shared goal card, canvas and pushing Skills remain.

## Frozen protocol

Before any trial, commit8938672 recorded four training scenes, eight evaluation
scenes, candidate caps8/16/24/32N and every selection/promotion gate. The manifest
is `workspace-engine/src/main/resources/push-evaluation032.json`; its original
byte hash and the seven unchanged031 source hashes are recorded in
`PUSH_032_FROZEN_CONTRACT.json`. A native test verifies both.

Sources are explicitly synthetic, metre-scale axis-aligned box scenes. They vary
crate/pusher masses, declared friction, obstacle placement and X/Z direction.
One heavy/high-friction case is a deliberately insufficient-force negative
control. These cases are reserved from parameter selection, not secret data,
captured rooms or independently measured real materials. Repeating them later
is a regression check, not a new unseen evaluation.

Each candidate cap executes all four training scenes. Failed tasks score60s;
successful tasks score their actual native completion time. The lowest mean score
wins, with a lower-cap tie break. The selected cap and training evidence are
committed after16 trials, before any evaluation trial. Eight paired evaluation
scenes then compare baseline8N against the frozen selected cap. Both arms use the
same original32N maximum,240Hz native controller, two-recovery budget and final
0.1m horizontal error /0.2mps speed gates.

The evaluation gates are success fraction≥.75, no loss of a baseline success,
no new jams among paired successful cases, mean penalized-time improvement≥.5s,
total force-effort ratio≤1.25 and maximum per-case penalized-time regression≤.25s.
They cannot be changed through the UI, model tools or imported scene data.

## Actual first decision

Training selected24N. Reserved success remained5/8→5/8. Penalized native time
improved by approximately1.367708333s per case, but force effort increased to
approximately1.262196165 times baseline. Both success and effort gates fail.
Candidate acceptance is therefore false and the original8/32N profile stays
active. Faster completion alone is not sufficient evidence of useful improvement.

The failed cases are the changed obstacle, Z-direction corner and heavy friction
negative control. Actual native reasons and full trajectories remain inspectable.
The two geometric tasks exceed a15s push-leg budget; the negative control exhausts
two bounded recoveries. The new suite exposes limits beyond031's narrower demo
conditions. It does not negate the original successfully verified tasks.

## Trial integrity and durability

`PushLearningStore` runs one experiment worker and one owned `PushStore` per
trial. Stable trial IDs bind job, case, arm and cap. Each original native result
is stored separately with its exact byte hash; job state stores concise metrics,
source provenance and ordering. Independent verification reloads every original
run, checks byte hashes, source/body identities, original plan and cap, and reruns
the031 native ledger/outcome verifier. It recomputes training selection and the
complete paired decision using only the original frozen gates.

Native state remains recoverable through031's both-body checkpoints. A process
restart marks the experiment interrupted. Explicit Resume keeps the same goal,
job and attempt, reuses committed settled trials, resumes the incomplete native
run where compatible, and preserves the original training selection. It never
uses evaluation outcomes to reselect the candidate. A completed native result
whose summary had not yet committed is reconciled without replaying physics.
Native solver contact caches rebuild, so bitwise future trajectory identity is
not promised. Cancellation stops owned work and cannot activate a profile.

The compatibility identity includes the learner/store/report classes, complete
suite, unchanged native runtime, JDK/platform and backend binding. Edited native
bytes, altered source/plan/metrics or changed code/suite are refused. Archived
completed evidence remains data; incompatible experiments do not silently restart.

## Real view and profile authority

The existing Spatial viewport displays actual committed bodies from the current
private trial while preserving the authored scene selection. A folded experience
panel shows trial count, split, case, arm, selected cap, decision and active
profile. A saved-experiment selector restores older reports after later tasks or restart without changing current task ownership or replaying physics. A source inspector can load labelled case geometry into Spatial.

The actual canvas displays an eight-row paired table with72 cells, three metric
modes and both arms of every native chart. The host exercises time/effort/jam
controls and reads every cell, every coordinate and original selection/decision
JSON. Java independently verifies them before the goal completes. Evaluation
completion means the experiment/report was verified; `accepted:false` still
means the candidate failed. Those meanings stay distinct in the chat summary.

Activation requires the original passing reserved decision and genuine verified
report receipt. It records the complete profile/decision/view provenance and
prior active ID. Rollback requires the expected current profile and restores the
exact prior one while preserving history. The actual032 candidate is rejected,
so positive end-to-end activation is not claimed. The activation refusal is
tested on actual native evidence; persistence/rollback also has a separately
labelled software transaction control. Future successful candidate activation
remains an explicit acceptance gate.

New pushing tasks opt in through a visible active-profile checkbox or an explicit
chat request. The host binds the actual profile and Java records it on the goal.
Explicit force requests cannot silently conflict with the selected profile.
Retained pushing procedures keep their own checked force defaults; they do not
silently acquire a later global setting.

## Tools and files

Model tools `force.read` and `force.start` accept empty arguments. The model
cannot supply gates, source cases, selected force, privileged IDs or success
values. Java performs the full registered task; shared goal read/resume/cancel
and finish are used normally. Fresh `push.start` supports explicit
`useActiveProfile:true`; omitting a requested opt-in is refused.

Private state is under `<project>/push-learning/state.json` and
`jobs/<jobId>/native/<runId>/`. Complete export contains the original job and all32
native runs, losslessly compressed with a decompressed-byte SHA-256. Reports keep
concise original-data JSON/table/charts; full native ledgers stay available in the
export. Private projects, models, credentials and unrelated browser state stay
outside Git. Documentation publishes only this phase's labelled synthetic tests.

No new library, cloud provider, model weights, Docker or WSL is introduced.
This is native experience-based scalar parameter selection and guarded retained
configuration, not LLM-weight training or a universal self-improving controller.
Next: use these now-seen failure cases as development/regression data, improve
geometric contact control, and freeze a different evaluation set before claiming
generalization or activating a new controller.
