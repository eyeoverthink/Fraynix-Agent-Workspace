# Agent Workspace development log

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
