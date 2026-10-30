# Fraynix Agent Workspace design and delivery plan

Edition 014: the native 3D lab now includes a Java-owned first-person observer, sampled box visibility/inspection, actual source/tick-bound PNG capture and durable read-only replay. Core observer logic needs no transformer or language-model inference. The local model is an optional request adapter. See [the observer contract](NATIVE_OBSERVER.md) and [README 014](../README-014.md). Physical body actuators and evaluated navigation remain subsequent native milestones.

This successor starts from Fraynix Studio commit `6341404`. The working Studio repository and its personal project state remain separate. The goal is a visible local environment in which agents can inspect real state, operate supported tools, create runnable artifacts, test their behavior, and reuse verified work.

## Product surfaces

The left column contains persistent project objects and a real browser. The center is a runnable creation canvas with a source view. The right column holds the local model selector, request, results and takeover control. The bottom timeline shows decisions, tool calls, failures and measured outcomes. Browser and canvas can each expand while keeping the agent visible.

The original Java game editor remains a separate Game Lab window, launched from this repository with a distinct project directory. Embedding its native viewport in the new shell is a later integration milestone. The existing Studio application is not replaced.

## Implementation boundaries

Java remains the model and data control plane. The new `workspace-engine` module reuses FactoryWorld, schemas, staged transactions, checkpoint storage, FactoryModelService, local Ollama routing, the offline kernel and its memory/journal interfaces. The desktop shell uses Electron to host two actual Chromium WebContentsView surfaces. This separates modern browser hosting from the legacy OpenGL game window.

The shell is trusted application UI. Remote browser pages and generated canvas applications have no Node integration or privileged preload. IPC accepts only the main shell frame. The Java control API binds loopback on an ephemeral port and requires a private session bearer held by the main process. Generated HTML previews cannot make network requests or call the control API. Their renderer sandbox is a boundary for web code; this version does not execute arbitrary generated Python, Java or native programs.

Source code and browser text are data. A model cannot register an executable handler, grant privileges or establish that a test passed by printing text. Structured model responses choose existing tools. Those tools own execution and return observations and receipts.

## Real browser observations and actions

The browser uses the same visible page for human interaction and agent actions. Navigation loads HTTP or HTTPS content. Observations include URL, title, bounded visible text, and supported interactive controls. Controls receive short-lived references tied to an observation. Actions reject stale observation IDs, scroll the target into view, and send actual input before observing again. Password fields require manual interaction.

This first adapter covers the main document's links, buttons and text inputs. General iframe traversal, complex shadow-DOM controls, downloads, multi-tab workflows and synchronized vision-model screenshots require additional adapters and tests. The surface is a real browser, but these automation limits remain explicit.

## Runnable artifacts and component state

An artifact is a FactoryWorld entity with a versioned `workspace.artifact` component. Its metadata contains title, revision, file inventory, source references and creation time. Files are stored under an immutable version directory. Updating an existing artifact requires its current version. Older sources remain available. An interrupted uncommitted version directory is preserved under recovery before another write proceeds.

Supported files are HTML, CSS, JavaScript, JSON, SVG, text, Markdown and CSV. A runnable artifact needs `index.html`. The preview serves those files from the Java host and executes their browser code in an isolated Chromium renderer. User source edits create a new version and invalidate the previous passing test badge.

The general object abstraction preserves domain meaning. A browser tab has a URL and controls; a document has a storage reference and structure; a game actor has a transform and physics. Visual placement on a canvas does not give every data object physical gravity. The inherited game remains a 2D physics environment; its zIndex describes draw order.

## Agent execution and progress

The agent runs a bounded local-model loop outside the render thread. The first milestone allows 16 steps, with model requests using the existing cancellable streaming transport and a JSON schema for permitted tool arguments. Stage guards advance repeated read-only observations toward creation, require tests for a new artifact, require a source change after a failed test and read, and require verified evidence before reporting completion.

The model still chooses page actions, generates artifact contents, specifies behavioral assertions, repairs code and describes its result. Stage guards constrain progress rather than establish that the generated work is correct. The separate integration harness evaluates real behavior independently of model prose.

Take over cancels the active model socket locally and prevents further agent actions. It does not kill a shared Ollama service. Manual browser/canvas actions and agent actions are serialized to prevent one from invalidating the other's observation mid-command.

Renderer operations and navigation also have deadlines. A hung artifact renderer is stopped while the host stays available; editing its source and reopening a corrected version restores a usable canvas. The integration harness includes an actual infinite-loop fixture and recovery.

## Tests and reusable skills

`canvas.test` reloads the current saved artifact, performs supported clicks/fills, and checks observable text, values or element counts. It returns individual assertion results and JavaScript errors. The Java engine records a receipt against the exact artifact version. Test coverage is limited to the assertions supplied; broader task correctness still needs appropriate independent evaluation.

A passing current artifact can become a reusable web-artifact recipe. The saved recipe contains versioned files, source references and actual test steps. Reusing it creates a new artifact and reruns the tests. The kernel indexes the recipe for memory retrieval. Editing a recipe's source requires fresh evidence before another verified version is saved.

These recipes are the first skill type. New compiled executors, operating-system adapters and general code workers require real implementations, declared capabilities and additional evaluation. A prompt naming a skill does not create those capabilities.

## Delivery milestones

| Milestone | Acceptance gate |
| --- | --- |
| Independent successor | Separate repository, launcher, runtime state and browser profile; Studio baseline preserved |
| Browser and canvas | Real external navigation, page interaction, runnable local artifact and visible source |
| Verify and repair | Catch incorrect behavior, write a corrected revision and pass actual assertions |
| Persistent skill reuse | Save only a current passing artifact, restart, recreate the recipe and pass again |
| Local model workflow | A real local model observes, creates, tests and saves through the same tool boundary |
| Broader artifact kinds | Dedicated renderers and validators for documents, diagrams, datasets and additional programs |
| Embodied perception | Tick-aligned engine observations and vision frames linked to object identity |
| Evaluated improvement | Replayable episodes, held-out task suites, candidate promotion and rollback |

The first delivery targets the first five gates. The remaining gates extend the same environment without claiming that every installed model or arbitrary website is already reliable. Initial operation needs Windows, JDK 21, Node.js 22.12 or newer, installed local Ollama models and the pinned Electron package. Docker and WSL are not prerequisites.

## Existing learning engines and the next implementation

The [October 29 Fraymus-OS-One assessment](FRAYMUS_OS_ONE_ASSESSMENT.md) identifies reusable Think rule revision/replay, synaptic adaptation, continuity, project generators and the separate fraymus-brain numerical training kernel. The review built OS One, ran four OPEG tests and sixteen standalone kernel tests, and independently checked persistent operator learning, bind collision repair and actual weight changes. No reviewed code is imported into the running app by this documentation update.

The next coding milestone is durable LearningEpisode, LearningCandidate, EvaluationReport and PromotionRecord components, a bounded worker for the extracted controller, independent regression/validation/final-test suites and reversible promotion. Java owns state and evidence; existing typed tools still execute actions. The visible library, browser, canvas, agent panel and timeline keep their current design. Do not copy the original generated-function or unrestricted Node runtime paths into the privileged shell.

Weight-training workers follow when the episode data and evaluation path are ready. The existing small ternary trainer can support a measured routing/controller experiment; the saved single-tensor reconstruction experiment does not by itself establish new full-model task knowledge or a loadable Ollama checkpoint. Whole-model training needs its own dataset, runtime adapter, quality gate and rollback.

## Delivered persistent controller learning

Edition [007](../README-007.md) implements the first three assessed stages. `LearningStore` registers separate FactoryWorld episode/candidate/evaluation/promotion/state schemas and checkpoints them under the successor project's `learning/` directory. Agent runs record bounded evidence and a frozen policy version; artifact verification requires an actual stored receipt. Restart recovers unfinished records as interrupted.

The pinned Fraymus nearest-class-mean classifier runs in a bounded child process. It updates prototypes from canonical failure summaries, reads back routing and proposes only `refreshStale` and `observationLimit`. Its memory is activated atomically with an accepted policy. Indexed episode IDs prevent duplicate prototype training. This is controller learning, not an Ollama weight update or a sandbox for arbitrary code.

Eight independent actual browser/canvas fixtures require no regression and measurable improvement before promotion. The first policy improves 5/8 to 8/8; a second retains 8/8 while reducing repeated reads. Harmful policies, stale parents and incomplete suites are rejected. Cancelled candidates are not promoted; previous policy and prototype memory can be restored together. The live local-model creation workflow passes using controller v2 and retains a verified episode.

The existing layout gains contextual learning records and controls. Next gates expand task families and hidden evaluation, browser failure handling, embodied episode alignment and numerical training on meaningful task targets. The fixed suite is a regression check, not a permanent untouched benchmark.

## Broader release validation and controller contract v2

Edition [008](../README-008.md) runs fifteen families across three generated datasets: the unchanged starting release passed 28/45; corrections pass 45/45 on the same checks. Host reliability checks remain separate from actual local-model task quality and the eight-case automatic promotion gate. Counter, calculator and two-page catalog research pass 24 independent acceptance assertions with gpt-oss:20b. The catalog's original model run repairs a failed test; saved varied outputs are also independently checked after reopening.

The controller now counts only unchanged explicit page reads and preserves navigation/actions when suppressing repeated observation. It rejects finishing an uncreated artifact. Replaced, hidden or disabled targets cause a fresh observation rather than a blind action retry. Supported text/number fields replace their existing value before actual insertion. The browser-controller-v2 replay contract is recorded in new learning objects and checked during evaluation. The read-cost replay now measures two extra reads to one while retaining 8/8, under corrected research semantics.

Next gates: genuinely withheld task families and repeated trials, navigation races and frames, tick/entity-aligned game episodes, and small-model training with task-level outcomes. Current release checks are reproducible regression fixtures; once inspected for corrections they are no longer an untouched holdout. No Ollama weight update or arbitrary-program OS sandbox is delivered here.

## Approved direction: shared world and 3D experiments

Edition [009](../README-009.md) records the user's next vision and the [working engineering roadmap](SCENARIO_ENGINE_ROADMAP.md). It is a design publication; the executable runtime remains edition 008. The existing layout and original Studio remain preserved. A future scene view consumes the same Java-owned object state as the agent, inspector and experiment records.

The ordered work is P1 shared scene/experiment/data contracts; P2 selected 3D rendering and physics adapters; P3 a complete ramp/block mechanics lab; P4 typed agent experiment actions; P5 real measurement binding; P6 tick/entity/action-linked experience with evaluated improvement; P7 separate target-language generation through explicit compiler and isolation adapters. P5 can proceed after P3, and P7 must not delay the first working lab.

Acceptance distinguishes real observations, assumed parameters, predicted results and independently checked outcomes. Units, coordinate frames, solver/version/configuration, timestamps, scene/dataset revisions and numerical tolerances are part of experiment identity. Authored state, transient simulation and retained results are separate. Relevant object queries keep the model context bounded. Not every data object receives physical behavior.

The proposed transmutation pipeline is typed specification → intermediate representation → language-specific generation → recorded toolchain → isolated execution → independent contract tests. No native-code executor or 3D backend is installed by this documentation change. The next implementation begins with P1 and a narrow adapter spike for P2/P3, with actual 3D evidence published in a later numbered edition.

## Delivered first 3D mechanics slice · edition 010

Java now owns `workspace.scene`, object3d and experiment records in an independent FactoryWorld checkpoint. Minie 9.0.3 / native Libbulletjme 22.0.3 simulates a bounded ramp, block and ground at 1/240-second ticks. Three.js 0.186.1 consumes actual Java poses inside the existing center view. Saved authored revisions, live state and experiment definitions/results are distinct. Stable object IDs survive edits, and replay reconstructs an older recorded definition without overwriting the current scene. Interrupted runs recover as interrupted.

The manual lab supports parameter edits, save/revert, play/pause/step/reset, an applicable frictionless reference check, recorded runs, replay and camera interaction. Five registered scene/experiment tools operate the same Java state. The actual local gpt-oss:20b physical trial creates a requested scene, runs 180 ticks and reports measured/reference displacement. Physical episodes remain completed-unverified in the existing learning ledger; their run records contain separate reference evaluations. No physical policy is trained or promoted.

Eight new Java test methods, twelve native UI integration checks and existing regressions verify the bounded implementation. See [README 010](../README-010.md), [the backend decision](3D_BACKEND_DECISION.md) and [current roadmap](SCENARIO_ENGINE_ROADMAP.md). This is initial P1, a selected P2 adapter, bounded P3 and early P4. Real-data binding, arbitrary scene authoring, cross-domain atomic transactions, physical-policy evaluation/training and compiler targets remain subsequent gates.

## Chandra abstractions and offline creation

The [pinned Chandra assessment](CHANDRA_ASSESSMENT.md) adds an implementation plan without changing the UI or executable release. Prioritize document/page provenance, model-digest-scoped local retrieval, durable job/effect recovery and expected-hash source edits behind the existing Java-owned contracts. Do not import simulated workflow output or heuristic random fitness as verified execution/learning. The reviewed combined runtime has fresh fixture failures; each extracted adapter needs independent acceptance in this application.

The first offline pipeline joins versioned measurement import, explicit units/time mapping, native simulation, independent residual calculation and a locally rendered report. Verify it with cached prerequisites and no external network. Subsequent pipeline graphs arrange installed handlers; recipes cannot create permissions or install executable tools. Arbitrary native code remains behind a separate compiler and OS isolation gate. Shared entities keep typed semantics: document/file properties do not become physical mass or force unless a physical model explicitly defines that mapping.

## Measurements and bounded offline reports · edition 011

[README 011](../README-011.md) implements CSV provenance, explicit units/time/origin/direction, immutable dataset versions, strict run coverage, interpolation and residual receipts in Java. The current quantity is signed downhill displacement from release. The report tool produces tested local HTML/SVG with original CSV and full JSON, and exposes it in the existing creation canvas. A saved current binding can rerun native physics after restart and produce new run/receipt IDs. Historical receipts remain readable; stale bindings are rejected before repeat effects.

Application browser requests can be restricted to loopback, and local model admission rechecks proxy/digest metadata without redirects. Cached startup and real local-model comparison are verified, while OS-wide egress isolation and control of shared Ollama remain outside this profile. Code/source guards recover from and detect corrupted renderer files. This release stays in the existing Fraynix repository and layout. Document/OCR evidence, retrieval, general durable jobs, cross-domain atomicity, calibration/uncertainty and physical-policy learning remain later gates.


## Native calibration and separate validation · edition 012

The existing lab now fits friction or gravity, one parameter at a time, against frozen versioned observations. Every candidate executes a separate native Bullet world. Training RMSE alone selects the durable winner before validation scoring. Acceptance requires minimum training improvement, every validation residual within declared tolerance and no validation RMSE regression. Fitting does not change the viewed authored scene. Applying an accepted current fit creates an atomic scene/application receipt; rollback refuses to overwrite intervening edits.

A durable worker supports cancellation, explicit restart resume and reuse of matching completed native operations. A narrow cross-checkpoint intent/receipt protocol reconciles lost acknowledgements; it is not a general job graph. The panel stays in the existing 3D Lab, and reports remain real tested HTML/CSV/JSON artifacts in the creation canvas. Calibration overrides are labeled and cannot masquerade as authored-version comparisons. The chat path follows the owned job lifecycle and requires a real tested report before finishing.

The first local-model trial repeated Start/Read and failed its task budget. The failure is retained; corrected host stages prevent duplicate starts and advance through actual receipts. gpt-oss:20b subsequently completes one fit/apply/report in five steps, with all candidate scores independently checked. Sources are synthetic; collection independence and instrument accuracy are not established. Physical episodes remain completed-unverified and no model weights are trained. See [README 012](../README-012.md). Independent real collection, document/OCR provenance and tick-linked physical experience remain next gates.


## Real observations and observer design direction · edition 013

The first real-data case is a reproducible research workflow. It imports 101 centroid observations estimated from actual Delfys75 videos/masks, freezes a training-only camera-plane mapping, executes native candidates and retains a rejected validation. It does not certify the filmed apparatus or the fitted material friction. A narrow report-wrapping correction preserves long source hashes on small screens. The original UI and private runtime remain intact.

The JING review supports a useful division: Java preserves authoritative entities/actions, and observer/model adapters consume recorded conditions. The downloaded release is a bidirectional audiovisual demo; full causal/DAO/training capabilities are not established by that archive. No JING executor/weights are installed. A future first-person observer must deliver real manual/agent movement, rendered frames and scene/run/tick identity before a generated-media backend is exposed. Ordered references and bounded history should preserve source labels and actual event/effect receipts. See [assessment](XGEN_JING_ASSESSMENT.md), [methods](datasets/delfys75-013/README.md) and [README 013](../README-013.md).
