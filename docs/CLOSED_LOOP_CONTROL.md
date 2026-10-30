# Native closed-loop control contract

[README 017](../README-017.md) contains current screenshots, measured comparisons, reproduction steps and limitations. [Verification](readmes/017/verification.json), [complete trials](readmes/017/feedback-evidence.json) and the [offline explorer](readmes/017/index.html) distinguish implemented behavior from the roadmap.

## Physical task and observation

`native-feedback-v1` uses the existing native Bullet ramp/block world at 240 Hz. A cubic smoothstep trajectory reaches a requested downhill distance in one second, followed by a half-second hold. Every 12 ticks, Java reads the actual native body and applies a bounded tangential impulse. No view-side positions or teleports supply authoritative motion. Position, tangent velocity, contact count, rotation, source definition and tick are retained; the current law uses position, velocity and declared physical parameters, while contacts are observed/logged.

The supported scene is the default 12×3 m ramp at Z=0, slope 10–40°, friction 0–0.3, mass 0.5–2 kg, gravity 8–12 m/s² and target 0.4–1.8 m. Unsupported geometry/parameters reject before creating the run. Each correction is at most ±0.6 m/s, applied as native impulse `mass × correction` along the ramp tangent. This is a virtual actuator, not a physical robot. Total contact/gravity forces are not bounded by that actuator cap.

Observations and controls use simulation time. Native work, checkpoint writes, desktop polling and Windows scheduling do not guarantee hard real-time wall-clock deadlines. The finite half-second hold does not establish indefinite stability.

## Fixed search and evaluation

Protocol commit `58ef53a51e0d706b4709e7a889958afe23fd1481` predates the first experiment. [Training](../workspace-engine/src/main/resources/feedback/training-v1.json), [evaluation](../workspace-engine/src/main/resources/feedback/evaluation-v1.json) and [contract](../workspace-engine/src/main/resources/feedback/contract-v1.json) remain byte-equivalent after canonical LF normalization. Their actual packaged content hashes accompany jobs/runs. These scenarios are native simulations, not instrumented physical-world recordings or a third-party hidden benchmark.

Six training scenarios × nine gain pairs produce 54 native trials. Only their mean score selects the gains. After selection freezes, eight different evaluation scenarios × zero/feedforward/hand-designed feedback/candidate produce 32 additional trials. The controller receives no future kick schedule. The runner applies the same actual disturbances to all compared modes.

Score is endpoint absolute error + 0.15 seconds × absolute terminal tangent speed + 0.5 × maximum final-quarter-second position error. Independent checks recompute terms from native poses/velocities, selected training winner and baseline means. Serialized single-precision velocity rounding permits 1e-7 m score reconstruction tolerance. Acceptance gates remain the original 0.06 m endpoint / 0.2 m/s speed / 0.12 m settling maximum, candidate mean score within +0.005 m of hand-designed feedback and at least 50% feedforward gain. The candidate is not required to dominate every individual case; report individual outcomes and the exact aggregate.

No neural or LLM weights are trained in this extension. It learns two feedback gains by finite physical trial search. The older four-weight one-shot policy and diagnostic spiking cortex remain separate. Fixed-suite retraining is deterministic; incorporating new live experience requires a versioned training dataset and fresh evaluation gate.

## Java API and durable ownership

The existing private loopback capability and typed desktop bridge remain authoritative; untrusted browser/document content cannot call these APIs directly.

| Operation | Contract |
|---|---|
| `/api/brain/start` | `kind=feedback-train`, optional `autoPromote`. Existing single-worker cancellation/history budget applies. |
| `/api/brain/state` | Includes `activeFeedback` separately from `activePolicy`; baseline v1 is hand-designed, accepted v2 is trained gains. |
| `/api/brain/feedback-start` | Exactly saved scene `id`, `expectedVersion`, `targetM`; previous experiment must be reset. Creates a live 360-tick run. |
| `/api/brain/feedback-kick` | Exactly `runId`, UUID `operationId`, `deltaVelocityMps` within ±0.5. Requires matching active running/paused feedback run; GUI buttons use ±0.4. |
| `/api/brain/feedback-cancel` | Matching active `runId`; persist cancellation and stop tick advancement. |
| `/api/brain/feedback-rollback` | Exact current `expectedVersion`; restore previous gains as a new monotonic version. |
| Existing scene control | Pause/resume/single-step/reset the actual live run. Completed/cancelled runs cannot resume. |
| Existing replay/export | Completed feedback run replays original control/disturbance impulses by saved tick/order. Native JSON export includes its real event history/outcome. |

Before a live control/disturbance effect, the run saves a pending intent. After applying it, Java records measured before/after native state and the actual impulse. An active-run disturbance retry with the same operation ID and same input returns its existing receipt; a changed input or stale run rejects. The checkpoint is the existing trusted local project store, rather than imported commands from arbitrary files.

Unfinished persisted runs become interrupted on restart, with no automatic effect retry or false completion. The last effect may have an intent without a receipt after abrupt termination; such a run is not eligible for successful replay. Cancelled/completed status and goal acceptance are separate. Native command replay is read-only with respect to policy and does not ask a new policy to produce matching actions.

Accepted policy activation and completed job now use one FactoryWorld transaction/checkpoint. The coordinator's final cancellation check and activation serialize under one lock. This also corrects the old one-shot promotion path. A late cancellation rejects an already committed job. Old policy records and job evidence remain available for rollback and audit. History capacity remains 16 jobs.

## UI, tests and next gates

Java Brain supplies gain learning, actual job progress/evaluation and separate feedback rollback. The 3D Lab supplies the live body view, real disturbance/stop controls and measured outcome. Rollback starts disabled until the current Java policy loads; unchanged job records are cached during polling. The current browser/canvas/chat/Game layout is retained.

Tests verify native impulse/velocity changes, training-only selection, independently reconstructed scores, action bounds, cancelled learning with no promotion, run stop, interrupted restart, out-of-domain rejection, source hashes, restart/rollback and exact saved-command replay. UI tests operate visible controls and export actual native records. [Current evidence](readmes/017/verification.json) records the clean checkout, regressions and limitations.

Next: external evaluation/geometry/sensing changes, closed-loop manipulation/navigation, original-game tick/entity-aligned learning, durable Java tool/task graphs and provenance-backed live experience training. No new language-model chat tools or automatic source mutation are installed by this slice.
