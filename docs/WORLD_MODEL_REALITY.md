# Learned world-model and reality contract

[README 018](../README-018.md) contains current screenshots, numerical tables, source links and reproduction steps. [Full evidence](readmes/018/world-model-evidence.json), [UI/source checks](readmes/018/world-model-integration.json) and [release verification](readmes/018/verification.json) separate native prediction, native planning and recorded-observation acceptance.

## Model and data

`structured-world-v1` learns projected block displacement change, tangent velocity and a continuous contact-support estimate. Five ridge-regression members each have three seven-feature readouts: 105 learned coefficients. The engineered inputs use velocity, commanded velocity impulse, projected gravity/friction, a smooth velocity-sign term and contact support. Known physical context and units are supplied; this does not discover a visual ontology or represent every scene component.

Native episodes execute actual Bullet impulses and twelve 240 Hz solver ticks per transition. Fitting uses 512 transitions from 32 episodes, calibration uses 128 from eight other episodes, and prediction evaluation uses 256 from sixteen further episodes. Whole-episode bootstrap and separate calibration/evaluation avoid random frame splits across those source groups. Thirty-two eight-step end-horizon rollouts start from actual states and then propagate predictions without intermediate observation replacement.

Sixteen additional native planning trials compare the learned model planner with fixed feedback gains49/10 on eight declared cases. Total native worlds:72. [Protocol](../workspace-engine/src/main/resources/world-model/contract-v1.json), [training](../workspace-engine/src/main/resources/world-model/training-v1.json), [calibration](../workspace-engine/src/main/resources/world-model/calibration-v1.json) and [evaluation](../workspace-engine/src/main/resources/world-model/evaluation-v1.json) were committed at a1729a97e81ad12f6e43b93e054d416b2af136a3 before experiments and remain unchanged. They are local development conditions, not an independent hidden benchmark.

The accepted scene/context domain is the default12×3m ramp atZ0, slope10–40°, friction0–0.3, mass0.5–2kg and gravity8–12m/s². Model state admits projected displacement−1..6m, tangent velocity±4m/s and support score0..1; actions are±0.6m/s. Goal target is0.4–1.8m. Unsupported observed states reject; future candidates leaving the domain cannot be selected.

## Prediction uncertainty and planning

One-step radii combine the separate-calibration90th percentile marginal absolute residual with twice ensemble standard deviation. The larger value is used for each motion output. Displayed multi-step radii grow with horizon and ensemble spread. Joint evaluation coverage is reported empirically; no formal probability, safety or real-world confidence guarantee is made. Contact support is an estimated/clipped score, not a calibrated contact probability or full collision model.

The planner compares49 two-block sequences: seven first actions×seven second actions, four steps per block, eight steps/0.4s total. Each ensemble member propagates its own state. Cost combines path position/velocity errors, action cost and uncertainty. Only the first action is executed, then actual state is observed and planning repeats. Excessive one-step radius or no supported future returns a labelled49/10 feedback fallback. Prediction accuracy and model-action fraction gate activation; fallback is not counted as learned planning.

Native prediction gates: position/velocity one-step RMSE≤0.008m/0.08m/s, eight-step RMSE≤0.08m/0.25m/s, RMSE ratios≤0.7 versus an action-aware constant-velocity reference, joint empirical band coverage≥0.8. Native planning gates: endpoint≤0.08m, tangent speed≤0.25m/s, last-quarter-second position error≤0.15m, mean score within+0.04m of fixed feedback and at least80% model actions. The existing feedback controller has a better current mean tracking score; the world model passes its stated limits and remains separately selectable.

## Recorded observation gate

The user selected the existing [Delfys75 case](datasets/delfys75-013/README.md). Training/validation CSVs and the frozen spatial calibration must match their recorded source digests. Missing/changed files fail; there is no synthetic fallback. The mapping, observations, times and units are unchanged. The case is already an observed regression dataset.

A passive constant-acceleration model fits training second differences from52 rows. The first two of49 validation positions initialize displacement/velocity; the remaining47 are scored. Free forecasting uses no later observed positions. Teacher-forced one-step forecasting uses the preceding two actual observations and has its own metrics. The all-point0.015m gate rejects the free forecast; it is preserved. No validation-specific fit, rescaling or time offset is introduced.

Native-model transfer assumes20°, gravity9.81, friction0, mass1kg and contact score1. Predictions at20Hz are interpolated to source timestamps; observations are not changed. These are surrogate assumptions rather than measured apparatus calibration. This transfer and the passive model do not identify causal action effects or authorize physical actuation. Native acceptance and recorded acceptance remain separate fields. The earlier edition013 rejected parameter fit remains unchanged.

## Private API and durable effects

Existing loopback capability/sender checks remain authoritative. Imported notes, documents and numerical data cannot register handlers or grant execution rights.

| Operation | Effect |
|---|---|
| `/api/brain/start` | `kind=world-model-train` collects/fits/calibrates/evaluates native data, scores the fixed recording and optionally activates an accepted native model. |
| `/api/brain/state` | Separate `activeWorldModel`, availability/version, source hashes, learned coefficients and recorded acceptance. Other policies remain separate. |
| `/api/brain/world-prepare` | Exactly scene `id`, `expectedVersion`, `targetM`; creates a paused native run with pinned model/context. Reset previous experiment first. |
| `/api/brain/world-preview` | Exactly `runId`, `expectedTick`, `observationSha256`; requires paused matching actual native state. Returns selected forecast and seven constant-action alternatives without actuation. |
| Existing scene resume/step/pause | Executes new decisions from current actual state; old preview commands are never submitted as authority. |
| `/api/brain/world-kick` | Matching active run, UUID `operationId`, velocity impulse within±0.5m/s. GUI uses±0.4. Active-run identical retry returns the existing receipt; changed input rejects. |
| `/api/brain/world-cancel` | Settles cancellation, stores a nonpassing goal result and halts native evolution. |
| `/api/brain/world-rollback` | Current expected model version; restores prior coefficients/availability as a new version. |
| Existing replay/export | Saved native commands reproduce a completed run independently of a future active model. JSON contains forecasts/innovations; native video excludes projected overlay. |

Preview binds both tick and state digest, because a paused impulse can change velocity without changing tick. Expired previews disappear. At every executed control interval, Java saves a pending plan, applies one bounded native impulse and stores actual before/after receipts. Prediction innovations bind the previous forecast tick/model to the later actual observation. External disturbances may explain a surprise; the record retains both facts.

Accepted model and completed job share one FactoryWorld transaction/checkpoint. Jobs/runs persist in the existing private project. Unsettled jobs/runs mark interrupted after restart and are not silently resumed. Normal rollback leaves ongoing run source/model snapshots pinned and does not change the other policies. History remains bounded to16 brain jobs.

## Presentation and tests

World model is a working dialog in the current desktop. Its plot shows actual recorded observations, passive forecasts and assumption-labelled native transfer. The native view uses violet projected geometry beside the actual orange body. Observers and native-only video omit that overlay. Counterfactual rows repeat one constant action; they are distinguished from the49 optimized two-block plans.

Tests independently recalculate native prediction/baseline RMSE and empirical coverage, validate split membership/source timing/action bounds, recompute recorded residuals and gate rejection, reject unsupported states and altered recordings, verify preview immutability/state fences and exact coefficient retention, and exercise live disturbance/replay/stop/restart/rollback/export. Browser tests verify actual plot/forecast pixels and decoded native video. [Current verification](readmes/018/verification.json) records clean-checkout and documentation evidence.

Next work requires independently collected calibration/evaluation recordings, broader entity/contact geometry, sensing noise/delay, provenance-backed live training updates and additional agent task coordination in Java. This extension supplies a working numerical prediction–action–observation loop, with its limits retained as evidence.
