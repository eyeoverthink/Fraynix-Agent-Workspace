# Shared-world and 3D experiment engineering plan

Edition 017 adds live bounded native feedback, gain selection from actual training outcomes, predeclared stronger-baseline evaluation, real disturbances, intent/receipt history, stop/replay/restart and separate feedback rollback. Job completion and activation now commit atomically. See [current feedback contract](CLOSED_LOOP_CONTROL.md) and [README 017](../README-017.md). Broader geometry/sensing, closed-loop manipulation/navigation and Java task graphs remain next.

Edition 016 delivers a shared Java nervous system, original-renderer Game tab, Java-owned durable neural jobs, real optional GPU readout, bounded automatic local memory and a retained physical controller with separate native validation. See [current contracts](JAVA_BRAIN_RUNTIME.md) and [README 016](../README-016.md). Closed-loop navigation/manipulation, stronger external evaluation, broader Java task ownership and full spiking GPU execution remain next gates.

Edition 015 update: native grab/move/release is implemented through a bounded dynamic-body constraint with collision/target checks, owned native worker threads, intent/receipt persistence, interruption/cancellation and command replay. Portable supported scene JSON import/clone and scene/run JSON, trajectory CSV and actual WebM exports work. See [README 015](../README-015.md), [body contract](NATIVE_BODY_CONTROL.md) and [formats/replication](SCENE_INTERCHANGE.md).

Next gates are general scene/component storage plus bounded mesh import; portable run-package remapping/replay; and closed-loop navigation/stuck recovery. Edition 016 delivers the first bounded physical policy with separate evaluation and rollback; broader task families remain next. A virtual constraint actuator is not an articulated hand or robot, and recorded history alone is not policy training.

Edition 014 update, October 30: the native first-person observer now implements Java camera pose/actions, sampled actual-box visibility/inspection, source/run/tick-linked actual PNG capture, durable history and read-only replay. No transformer is required for world logic or rendering. See [README 014](../README-014.md) and the [implemented contract](NATIVE_OBSERVER.md). Preserve the original application and its current layout.

The bounded actuator and select/grab/move/release milestone was delivered in edition 015; edition 016 adds separately evaluated bounded physical readout learning. Colliding navigation and broader tick-aligned policy tasks remain next. A free camera is not a physical avatar. A learned audiovisual renderer remains an optional later adapter rather than a prerequisite or authoritative physics source. The edition 013 real-data fit remains rejected by its declared validation gate.

Recorded October 29, 2026; updated October 30 for evidence/tooling edition 013. Status: **native lab, versioned observations, one-parameter fitting, separate validation and guarded application implemented**. Edition 009 remains the frozen design. [README 012](../README-012.md), [measurements](../README-011.md), [the native milestone](../README-010.md) and the [backend decision](3D_BACKEND_DECISION.md) identify the delivered slices, actual tests and remaining scope.

Delivered: initial P1 scene/experiment contracts and stable 3D entities, selected P2 Bullet/Minie plus Three.js adapters, P3 fixed ramp/block lab with save/replay and reference evaluation, early P4 registered tools and a bounded P5 CSV measurement binding/comparison/report/repeat workflow with explicit units, source and time/frame mappings. Verification uses synthetic fixtures and actual local-model/native execution. General dataset quantities/sensor calibration, cross-domain unification, arbitrary scene authoring, broader physical task coverage, P6 learning and P7 compiler targets remain open. The existing 2D runtime/units, app layout and repository remain preserved.

## Product contract

Preserve the current library/browser left, creation/source center, agent right and timeline below. The future scene view belongs to the existing workspace. Preserve the original Studio, saved levels, model services and personal projects. Java owns durable identities, transactions, experiment state and evidence; renderers and solver adapters consume validated state through defined boundaries.

The first complete deliverable is a 3D ramp/block experiment with live measurements, save/reset/replay and agent-driven parameter comparison. Real-data binding follows. Do not create empty tabs to represent these phases.

## Current versus proposed

- Current: JBox2D 2D bodies, fixed 1/60-second stepping, 2D transforms/camera, entities, schemas, checkpoints, local-model routing, browser/artifact tools, receipts, recipes and bounded controller learning.
- Added in 010: stable object3d entities with position/quaternion/size, authored scene revisions, copied experiment definitions, native Bullet fixed-step motion and contacts, real perspective rendering, recorded measurements/results, replay and typed scene/experiment tools.
- Added in 011: versioned CSV provenance/mapping, independent residual comparisons, reports and actual repeat after restart.
- Added in 012: one-parameter native friction/gravity search; training-only selection before separate validation; durable cancellation/resume, completed-operation reuse, version-checked application and rollback with atomic scene receipts.
- Remaining: independent real instrument validation, arbitrary scene/mesh authoring, general cross-domain atomic transactions, trained physical policies and additional compiler targets.
- Current zIndex remains draw order. Existing 2D saved units must not silently become meters.
- Current browser-controller-v2 and its eight-case promotion gate remain separate from future physical-task evaluation. No whole-model training is implemented.

## Ordered milestones

| ID | Milestone | Dependencies | Acceptance |
| --- | --- | --- | --- |
| P1 | Shared experiment contracts | Existing transaction/checkpoint runtime | Stable IDs and versions, explicit units/frames/clock, persistence, invalid-value/stale-edit rejection, original regressions retained |
| P2 | 3D adapter selection and actual viewport | P1 | Backend decision record, compatible packaging/licenses, perspective rendering and selection, physical depth/collision support, shared Java state |
| P3 | Complete ramp/block mechanics lab | P2 | Play/pause/step/reset/save/replay; reference-case tolerance; bounded numerical error; stable contacts; actual time/position/velocity measurements |
| P4 | Agent-controlled experiments | P3 | Validated parameter edits, real runs and result queries, independent goal checking, bounded job/cancellation, incorrect-success detection |
| P5 | Real measurement binding | P3 | Versioned CSV/JSON provenance/units, explicit mapping, measured/assumed/predicted labeling, independently checked residual calculation |
| P6 | Evaluated physical learning | P4, P5 | Tick/entity/action-linked episodes, withheld cases, measurable improvement, rejected regression, reversible promotion |
| P7 | Target-language generation | P1 plus a separate execution boundary | Narrow typed specification/IR; two toolchains produce passing equivalent behavior; failing fixtures detected; build/run evidence and isolation |

P5 may proceed after P3 without waiting for P4. P7 is a separate workstream and must not delay a functioning first 3D lab. Native-code execution stays disabled until its own execution boundary and tests exist. Do not assume Docker or configured WSL.

## Candidate schemas and ownership

These names describe the broader design. Edition 010 registers `workspace.scene`, `workspace.scene.object3d` and `workspace.experiment` in a dedicated FactoryWorld checkpoint. Edition 011 adds `workspace.measurement.dataset`, `.version` and `.comparison` in a separate measurement checkpoint. Transform and body fields are contained in validated authored object payloads generated from the bounded ramp definition. General standalone Transform3D/body/material authoring and measurement quantities beyond downhill displacement remain proposed:

- `Scene`: ID, schema version, authored revision, entity membership and coordinate convention.
- `Transform3D`: position in explicit units, quaternion orientation, scale and parent relationship; validate finite values and normalized orientation.
- `RigidBody3D` / `Collider3D` / `Material`: solver body type, mass/inertia contract, shape, friction/restitution and permitted ranges.
- `ExperimentDefinition`: scene revision, initial conditions, timestep, solver/version/configuration, seed where relevant, limits, dataset binding and evaluator identity/tolerance.
- `ExperimentRun`: definition/version, job status, start/stop reason, timestamp/tick-aligned actions, observations, output references and evaluation receipt.
- `MeasurementDataset`: edition 011 retains source/import version, original CSV/hash, declared columns/units/time/origin/direction, missing-row policy, optional uncertainty, notes and immutable normalized rows. General sensor/frame adapters remain future work.

Keep authored, running and recorded state distinct. Save the authored scene; checkpoint results without silently overwriting the starting conditions. A failed run cannot certify a changed scene. Large telemetry streams belong in bounded referenced storage, with summaries and selected samples in model context.

## Proposed tool boundary

Edition 010 registers `scene.create`, `scene.read`, `scene.update`, `experiment.run` and `experiment.reset`. Edition 011 adds `dataset.import`, `dataset.read`, `experiment.compare` and `comparison.report`. Manual control additionally exposes play/pause/step/reset, snapshots, history, replay and saved comparison repeat through the trusted shell. Broader `scene.query`, `scene.apply`, `experiment.status` and `experiment.stop` contracts remain future work; exact names can change. Imported data and generated source cannot install handlers.

The model plans experiments and chooses bounded actions outside the physics tick. A solver/controller executes physical integration on its fixed schedule. Associate action completion with the relevant state revision and tick. Preserve human takeover and distinguish request acceptance, job completion and independently passing evaluation.

## Reference evaluation

Start with frictionless incline acceleration g × sin(theta), before end-of-ramp or other contacts, plus free fall and stable resting contact. Document coordinate/measurement transforms and solver error tolerances. Friction needs a declared static/kinetic model and separate reference cases. Use seeded/randomized conditions and new withheld scenarios with preregistered targets. State the supported platform/backend for repeatability; do not assume bitwise cross-platform determinism.

For data binding, independently compute at least one residual metric from a small known fixture. Reject ambiguous unit mappings; expose stale or missing data. Synthetic data must remain labeled synthetic. Calibrated agreement is evidence under tested conditions, not proof of arbitrary real-world causation.

## Transmutation pipeline

User request → reviewed typed specification → intermediate representation → language generator → recorded compiler/toolchain → isolated execution → independent contract tests → versioned artifact and receipt. Begin with a narrow numerical function and two targets; expand only after equivalent behavior is tested. Java native interoperability and target-language source generation are distinct mechanisms. Existing HTML artifact execution is the working reference for evidence, not an execution boundary for C/Go.

## Risks to resolve through implementation

Backend compatibility/redistribution; renderer-to-Java synchronization; numeric tolerances and timestep stability; unit/frame migration; unsupported physical domains; observation/context size; job cancellation; untrusted native execution; overfitting learned strategies to known cases. Each becomes a measured gate, not an invented capability badge.

## Edition 012 acceptance and next gate

The bounded fitting/evaluation extension executes every candidate in a separate actual native Bullet world. Training alone chooses a durable winner before validation scoring. Acceptance requires declared training improvement, all validation residuals within tolerance and no validation RMSE regression. Job recovery, source/scene version guards, atomic scene/application receipts and rollback are tested. A finite grid is not global optimization; source labels and collection independence remain user supplied. Recorded acceptance sources in that parameter-fitting milestone are synthetic. Edition 016 separately trains a bounded physical readout; language-model weights remain untrained.

Next collect independent instrument data with apparatus, timing, frame and uncertainty provenance; predeclare fitting bounds and validation criteria. Follow with document/OCR page/span evidence, local retrieval and durable creation contracts. Tick/entity/action-linked physical experience needs new withheld task families and evaluated policies. General scene authoring, Game Lab bridges and compiler targets remain separate. Preserve the present app/layout, original Studio and private projects.

## Documentation and next action

[README 009](../README-009.md) explains the vision and original sequence; its [frozen roadmap](readmes/009/roadmap.json) remains intact. [README 010](../README-010.md) documents the native slice and [README 011](../README-011.md) the working CSV comparison/report/repeat loop. [Current design](AGENT_WORKSPACE_DESIGN.md), [memory](PROJECT_MEMORY.md) and [log](DEVELOPMENT_LOG.md) carry ongoing decisions. The bounded separate fitting/evaluation loop is delivered in edition 012. Next broaden independent measured-data provenance, then document/OCR evidence, retrieval, durable workflows, physical task families and evaluated learning targets. Expand scene authoring and Game Lab bridges through tested contracts. Compiler targets remain a separate workstream.

The [Chandra review](CHANDRA_ASSESSMENT.md) supplies complementary document/OCR, retrieval and durable pipeline patterns. Begin measurement import with CSV/JSON before PDF extraction. Preserve page/span/digest provenance when document input follows. Verify one complete cached, network-disabled import → simulate → compare → local report → reopen/reuse loop. Pipeline lifecycle and compiler/isolation work extend this sequence; they do not replace P5 with a wholesale runtime/UI merge. Chandra's random candidate fitness and simulated code-output paths are excluded from success and promotion criteria.


## Edition 013: real observations and observer sequence

The first public-recording case now runs end to end: pinned Delfys75 videos/masks, explicit training-only image-plane mapping, 52 training/49 validation observations, native search, independently checked metrics and actual exported report. Friction 0.15625 lowers error but fails the declared 0.015 m all-point validation tolerance (max residual 0.159463 m). Preserve that rejection; both scenes remain v1. Surrogate geometry, image perspective and release conditions are unresolved. Do not treat these now-observed development recordings as a fresh scientific holdout. See [methods](datasets/delfys75-013/README.md).

The downloaded JING ZIP/source and small weight metadata are reviewed at upstream 41c41ae9a1dbe5213e8733bbebeaebe297afa7a7. It supplies bidirectional inference code, not an installed local generator or the full DAO/causal/training system. The current GPU is a 6 GB GTX 1660 Ti; weight, runtime and license requirements are material. No JING weights, inference, paid endpoint or CUDA environment was installed. See [assessment](XGEN_JING_ASSESSMENT.md).

Next complete local implementation:

1. Native first-person observer inside the existing lab: actual movement, pose/FOV, rendered frame capture, visible entity references and stable source scene/run/tick identity. Manual and agent controls share typed actions. Define free-camera versus physical-avatar semantics; verify movement, stale-observation rejection, restart and replay.
2. Immutable condition/reference packet and bounded source-linked history. Preserve measurement/simulation/generation/inference origin. Observation text cannot grant execution authority. Validate departure/revisit against committed events.
3. An executable eligible audiovisual backend only after a real complete case can run with actual output hashes, model/version/seed, cancellation and effect receipts. Keep generated appearance separate from committed native state. No inactive generator page.
4. Separately acquire camera/apparatus/release calibration and fresh physical evaluation recordings. Train perception/policy/weights only with an actual optimizer/checkpoint/inference path and independent gates.

This observer sequence complements the original physical-learning and compiler plans. It does not grant unlimited tools, automatically convert GGUF/DiT weights or replace independent scientific validation.
