# Agent Workspace development log

## 2026-10-29 Native 3D mechanics milestone · edition 010

The user authorized the next implementation, testing, outline and push. Added Java-owned versioned ramp scenes and stable object3d entities in a separate FactoryWorld checkpoint, native Bullet through pinned Minie 9.0.3 / Libbulletjme 22.0.3, and a local Three.js 0.186.1 viewport inside the existing creation canvas. Current parameters use explicit SI units and a right-handed +Y-up frame. Authored scene revisions remain separate from live state and recorded experiment definitions/results.

Eight new Java tests pass: varied angles/masses versus analytical incline displacement, friction, resting contacts, 3D depth/free fall/ground collision, schema/value/version validation, persistence, pause/single-step/reset, interrupted recovery and replay of a retained older definition after editing. Twelve actual Electron/Java/WebGL integration checks pass, including the original browser/artifact workflow after leaving 3D. Existing 45 broader checks, eleven original integration checks and nine learning checks pass. A root distribution copy initially overlapped the integration test's own Game Lab process and hit a Windows asset lock; packaging passed after that test closed its own window. The original user Studio was not stopped.

Implementation commit `cd20256` adds typed scene create/read/update and experiment run/reset tools. An initial local-model trial created and ran the scene but failed reporting completion; a separate physical-task prompt fixes that workflow. Subsequent gpt-oss:20b physical trials and the existing counter pass independent checks. Physical task episodes are recorded as completed-unverified under the existing artifact-oriented learning contract; their physical runs have separate reference evaluations. Numerical discrepancy is distinguished from measurement uncertainty. No physical policy or Ollama weights are trained.

The full Java suite passes 105 tests. An independent clean clone builds all distributions, passes 105 Java tests, nine desktop tests, twelve native 3D checks and 45 broader checks, and matches all 382 source/asset hashes. The clean checkout's first parallel Electron launches collided during binary extraction; serial initialization and rerun pass. This was a test-bootstrap race, with no product/runtime change inferred from it. Existing eleven original integration and nine learning checks pass in the working checkout.

README 010 adds actual images, measurements, incomplete-trial and passing model evidence, backend selection, current scope, step-by-step review, inventory, offline explorer and Obsidian map. Documentation checks pass ten editions, fifteen canvases, 1,426 references and 131 frozen hashes. Viewer filters, selection/source, inventory search, navigation, three images and normal/narrow layouts pass. Real-data binding, arbitrary scene editing/mesh import, full cross-domain world unification and native-code generation remain later gates. Launcher follow-up `792e675` refreshes the incremental Java distribution and checks a lockfile hash before dependency installation. Prepare-only setup and repeat setup pass under Windows PowerShell, followed by the native integration suite. The normal updated app is open with the existing welcome artifact preserved and 3D Lab available; original Studio remains running. Final remote publication verification follows.

## 2026-10-29 Shared-world and 3D roadmap · edition 009

The user approved documenting the next vision and steps in a new README and updating GitHub in full. Added README 009 and a working engineering roadmap. This is a documentation/design publication against `3a1922d`; the executable runtime, user project state and original Studio are unchanged.

Recorded P1 shared scene/experiment/data contracts, P2 3D adapter selection, P3 complete ramp/block mechanics lab, P4 typed agent experiment actions, P5 real measurement binding, P6 evaluated physical experience and P7 separate target-language generation through typed specifications/IR/compiler adapters. Each phase has dependencies and acceptance criteria. 3D backends, real-data solvers, arbitrary native-code execution and whole-model learning remain planned. Preserve existing application layout and implement a functioning vertical slice before expanding scope.

Documentation includes a linked offline module/roadmap explorer, Mermaid and Obsidian maps, canonical implementation inventory and clearly labeled earlier actual images from editions 008 and 004. Prior runtime test evidence is linked as historical evidence, not a new test run or proof of 3D. Publication checks cover references, frozen snapshots, unchanged source hashes and normal/narrow browser behavior.

Verified 372 unchanged implementation hashes and nine documentation editions, fourteen canvases, 1,332 references and 114 frozen hashes. Browser checks pass seven planned/two verified module filters, phase search and selection, source search, section navigation, two loaded historical images and 1280 / 390×844 layouts without horizontal overflow. Runtime tests were not rerun for these documentation-only changes.

## 2026-10-29 Broader validation and recovery · edition 008

Starting from fully pushed `3d77635`, the user authorized broader validation, continued documentation and publication. Added 45 generated checks across three fixed seeds. The unmodified release passed 28/45. Recorded baseline evidence identifies forced creation before research, blocked navigation after repeated reads, changed-control recovery failures and intermittent numeric fill appending rather than replacing existing values.

Implemented and pushed `0e80086`: count only unchanged page observations, preserve meaningful navigation/actions when suppressing duplicate reads, refuse finishing an uncreated requested artifact, recover replaced/hidden/disabled controls with fresh observation rather than a blind click, and deterministically clear/focus supported inputs before actual text insertion. New runtime/replay contract is browser-controller-v2; older episode data stays readable and earlier edition evidence remains intact.

Verified same generated checks 45/45 after corrections, nine desktop tests, 97 Java tests, eleven original integration checks and nine learning checks. Candidate replay remains eight cases; first candidate 5/8 to 8/8 and read-cost candidate 8/8 with extra reads two to one under the corrected semantics. Actual local gpt-oss:20b completes counter, calculator and two-page catalog research with 24 independent acceptance assertions. Catalog version 2 follows the model's own test/repair loop. Varied model outputs are rechecked after reopening. The acceptance harness was corrected to use standard CSS and explicit DOM visibility checks, keeping evaluator mistakes separate from application failures.

README 008 adds actual frame exports, before/after evidence, model acceptance records, searchable offline explorer, canonical 372-file inventory and Obsidian map. No personal runtime state or credentials are published. Broader generated release checks are separate from automatic candidate promotion; new withheld task families, repeated trials and embodied episode alignment remain next gates. Publication QA and final remote verification follow below.

Publication QA: an independent clean clone builds all distributions and passes 97 Java tests, nine desktop tests and 45 broader checks. All 372 implementation source/asset hashes match. Documentation verifies eight editions, thirteen canvases, 1,243 references and 104 frozen hashes. Browser inspection passes module/status filters, selected details/source, inventory search, navigation, five actual gallery images, normal width 1280 and narrow 390×844 with no horizontal overflow. Original native Studio remains running and its source checkout is clean. The idle review workspace is reopened on the corrected code with its existing project preserved.

## 2026-10-29 Learning milestone in progress

The user authorized continuing the assessed learning integration and documenting development, images, README editions, memory and logs. Preserve the original Studio, Fraymus-OS-One and current workspace design. Work starts from workspace commit `35720e9`.

Implement persistent versioned episodes, bounded adaptive strategy candidates, independent browser/artifact replay, evidence-based promotion and rollback. Reuse the pinned standalone Fraymus brain source in a bounded child worker. Model-weight training is a later mechanism; this milestone adapts an explicit controller policy.

Acceptance: actual failure capture; persistence/restart and interruption handling; candidate generation; held-out fixture replay; rejected regression; reversible promotion; current browser/canvas workflow remains functional; new screenshots and README 007 with matching evidence, inventory, offline viewer and Obsidian map.

### Implementation and verification progress

Added a separate learning checkpoint using FactoryWorld schemas and transactions; versioned episodes, candidates, evaluations and promotion/rollback records; crash recovery for unfinished runs; receipt-bound verification; bounded data capture without page/source/typed content in the episode records.

Pinned six unmodified fraymus-brain kernel source files. A 96 MiB / five-second child worker updates supervised nearest-class-mean prototypes and reads back their routing before proposing one of two bounded strategies. The worker receives canonical failure summaries, and learned state activates only with a passing promotion. Episode IDs prevent duplicate prototype training.

Connected the active policy to the real agent loop, automatic post-task candidate evaluation, the existing library, agent controls and evidence timeline. Added separate browser/canvas replay fixtures, leaving the user's visible browser unchanged.

Verification so far: 97 Java tests with no failures; seven desktop tests; eleven original desktop integration checks; eight learning integration checks; actual gpt-oss:20b creation/test/recipe workflow with controller v2 and a verified episode. First replay improved 5/8 to 8/8. A harmful candidate was rejected. Restart, rollback, cancellation and retry were checked.

The first replay exposed a missing content policy in the new fixtures and an unclear password error crossing the renderer boundary; both were corrected. A subsequent cancellation stress run exposed a renderer abort surfacing as a navigation error; cancellation now reports a consistent cancelled outcome and checkpoints the unfinished candidate. Screenshot capture now waits for the dialog close to paint before exporting the workspace frame. Final documentation and publication checks remain in progress.

### Release verification

The extended learning integration now contains nine passing checks, including a second policy that retains 8/8 while reducing the extra-read count from 1 to 0. The seven desktop tests also verify that the same episode is not trained twice. All 97 Java tests and the eleven original integration checks pass. Actual gpt-oss:20b completed the artifact workflow with controller v2 and retained its verified episode.

README 007 contains three new actual application frame exports, current evidence, a 369-file source/asset inventory, architecture and next gates. The offline viewer's search, status filter, selected module details, image gallery, navigation and 390px layout were checked; the temporary viewport/tab were restored/closed. The normal review app was reopened on the updated code with the same saved welcome artifact; the original Studio remains running. Preparing the clean-checkout verification and final publication.

### Publication gate complete

A clean Git clone of release candidate b9e8033 passed the complete Gradle check/install, 97 Java tests, seven desktop tests and nine learning integration checks. All 369 inventoried source/assets match canonical hashes. Documentation verification passes seven editions, twelve canvases, 1,156 references and 86 frozen hashes. The remaining publication action is the authorized main/tag push followed by remote-head verification.

### Published

The complete learning release was pushed to eyeoverthink/Fraynix-Agent-Workspace main at 7321f4a, with annotated tag readme-007. This follow-up records publication and the handoff; it does not change the frozen edition or executable behavior. The updated normal review app is open, its previous artifact is preserved, and the original Studio remains running. Next implementation gates are broader end-to-end task replay and hidden evaluation, additional failure strategies, Game Lab episode alignment and evaluated small-model training.
