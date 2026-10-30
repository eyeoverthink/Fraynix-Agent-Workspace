# Shared-world and 3D experiment engineering plan

Recorded October 29, 2026; updated for implementation edition 010. Status: **first bounded 3D lab implemented**. Edition 009 remains the frozen design. [README 010](../README-010.md) and the [backend decision](3D_BACKEND_DECISION.md) identify the delivered slice, actual tests and remaining scope.

Delivered: initial P1 scene/experiment contracts and stable 3D entities, selected P2 Bullet/Minie plus Three.js adapters, P3 fixed ramp/block lab with save/replay and reference evaluation, and an early P4 registered-tool path verified with one local-model physical task. P1's full dataset contract and cross-domain unification, general P2/P3 scene authoring, broader P4 task coverage, P5 real measurement binding, P6 physical-policy learning and P7 compiler targets remain open. The existing 2D runtime/units and application layout remain preserved.

## Product contract

Preserve the current library/browser left, creation/source center, agent right and timeline below. The future scene view belongs to the existing workspace. Preserve the original Studio, saved levels, model services and personal projects. Java owns durable identities, transactions, experiment state and evidence; renderers and solver adapters consume validated state through defined boundaries.

The first complete deliverable is a 3D ramp/block experiment with live measurements, save/reset/replay and agent-driven parameter comparison. Real-data binding follows. Do not create empty tabs to represent these phases.

## Current versus proposed

- Current: JBox2D 2D bodies, fixed 1/60-second stepping, 2D transforms/camera, entities, schemas, checkpoints, local-model routing, browser/artifact tools, receipts, recipes and bounded controller learning.
- Added in 010: stable object3d entities with position/quaternion/size, authored scene revisions, copied experiment definitions, native Bullet fixed-step motion and contacts, real perspective rendering, recorded measurements/results, replay and typed scene/experiment tools.
- Remaining: arbitrary scene/mesh authoring, cross-domain atomic world transactions, measurement datasets/binding, trained physical policies and additional compiler targets.
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

These names describe the broader design. Edition 010 currently registers `workspace.scene`, `workspace.scene.object3d` and `workspace.experiment` in a dedicated FactoryWorld checkpoint. Transform and body fields are contained in validated authored object payloads generated from the bounded ramp definition. General standalone Transform3D/body/material authoring and MeasurementDataset remain proposed:

- `Scene`: ID, schema version, authored revision, entity membership and coordinate convention.
- `Transform3D`: position in explicit units, quaternion orientation, scale and parent relationship; validate finite values and normalized orientation.
- `RigidBody3D` / `Collider3D` / `Material`: solver body type, mass/inertia contract, shape, friction/restitution and permitted ranges.
- `ExperimentDefinition`: scene revision, initial conditions, timestep, solver/version/configuration, seed where relevant, limits, dataset binding and evaluator identity/tolerance.
- `ExperimentRun`: definition/version, job status, start/stop reason, timestamp/tick-aligned actions, observations, output references and evaluation receipt.
- `MeasurementDataset`: source/import version, columns, units, timestamps, frame, missing-value policy, uncertainty, provenance and immutable content reference.

Keep authored, running and recorded state distinct. Save the authored scene; checkpoint results without silently overwriting the starting conditions. A failed run cannot certify a changed scene. Large telemetry streams belong in bounded referenced storage, with summaries and selected samples in model context.

## Proposed tool boundary

Edition 010 registers `scene.create`, `scene.read`, `scene.update`, `experiment.run` and `experiment.reset`. Manual control additionally exposes play/pause/step/reset, snapshots, history and replay through the trusted shell. The broader proposed `scene.query`, `scene.apply`, `experiment.status`, `experiment.stop` and `experiment.compare` contracts remain future work; their exact names can change. Imported data and generated source cannot install handlers.

The model plans experiments and chooses bounded actions outside the physics tick. A solver/controller executes physical integration on its fixed schedule. Associate action completion with the relevant state revision and tick. Preserve human takeover and distinguish request acceptance, job completion and independently passing evaluation.

## Reference evaluation

Start with frictionless incline acceleration g × sin(theta), before end-of-ramp or other contacts, plus free fall and stable resting contact. Document coordinate/measurement transforms and solver error tolerances. Friction needs a declared static/kinetic model and separate reference cases. Use seeded/randomized conditions and new withheld scenarios with preregistered targets. State the supported platform/backend for repeatability; do not assume bitwise cross-platform determinism.

For data binding, independently compute at least one residual metric from a small known fixture. Reject ambiguous unit mappings; expose stale or missing data. Synthetic data must remain labeled synthetic. Calibrated agreement is evidence under tested conditions, not proof of arbitrary real-world causation.

## Transmutation pipeline

User request → reviewed typed specification → intermediate representation → language generator → recorded compiler/toolchain → isolated execution → independent contract tests → versioned artifact and receipt. Begin with a narrow numerical function and two targets; expand only after equivalent behavior is tested. Java native interoperability and target-language source generation are distinct mechanisms. Existing HTML artifact execution is the working reference for evidence, not an execution boundary for C/Go.

## Risks to resolve through implementation

Backend compatibility/redistribution; renderer-to-Java synchronization; numeric tolerances and timestep stability; unit/frame migration; unsupported physical domains; observation/context size; job cancellation; untrusted native execution; overfitting learned strategies to known cases. Each becomes a measured gate, not an invented capability badge.

## Documentation and next action

[README 009](../README-009.md) explains the vision and original sequence; its [frozen roadmap](readmes/009/roadmap.json) remains intact. [README 010](../README-010.md) documents the working slice. [Current design](AGENT_WORKSPACE_DESIGN.md), [memory](PROJECT_MEMORY.md) and [log](DEVELOPMENT_LOG.md) carry ongoing decisions. Next add a small versioned measurement dataset with explicit provenance/units/time mapping and independent residual evaluation, then broader physical task families and evaluated learning targets. Expand scene authoring and Game Lab bridges through tested contracts. Compiler targets remain a separate workstream.

The [Chandra review](CHANDRA_ASSESSMENT.md) supplies complementary document/OCR, retrieval and durable pipeline patterns. Begin measurement import with CSV/JSON before PDF extraction. Preserve page/span/digest provenance when document input follows. Verify one complete cached, network-disabled import → simulate → compare → local report → reopen/reuse loop. Pipeline lifecycle and compiler/isolation work extend this sequence; they do not replace P5 with a wholesale runtime/UI merge. Chandra's random candidate fitness and simulated code-output paths are excluded from success and promotion criteria.
