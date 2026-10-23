# Verification

The independent Studio project was verified on October 22, 2026 using JDK 21 on Windows x64. Historical integration results are kept separate from the extraction results below.

## Verified extraction results

- A clean source checkout compiled and passed all 56 editor JUnit tests and the four executable runtime, transaction/spatial, kernel and model harnesses.
- The source build produced the new `Fraynix-Studio-0.1.0.zip`, including all three module JARs, dependency/native libraries, launcher, assets and documentation.
- The packaged native editor passed two process launches with eight restored sample entities, eight JBox2D bodies, rendered physics inspection and stable persistent identities.
- A real local `fraynix-3b:latest` proposal passed native preview, application, save and exact deserialization. A second process restored it and used durable undo to recover the original scene.
- A default launch created the checkpoint in `.fraynix-studio/projects/default`; the earlier app's checkpoint stayed byte-identical and its process remained responsive.
- Every Gradle project/source root resolved inside the new checkout. An explicit external `fraynixRoot` override was rejected.
- Local model discovery observed 46 exact names across two local services, including the 120B model's secondary endpoint, and excluded cloud proxies. The generated catalog is not tracked.
- The source repository's imported file hashes and Git status remained unchanged.

See `evidence/standalone-verification.json` for the structured extraction record. GitHub Actions provides a separate hosted Windows build result; it does not run the interactive native or real-model checks.

## Automated surface

`gradlew.bat clean check distZip installDist` compiles the editor and all three checked-in modules, runs the editor JUnit suite, runs the executable runtime/transaction/kernel/model harnesses, and packages the application.

The current integration has 58 editor JUnit tests. Those cover geometry, physics registration, picking/placement coordinates, serialization, checkpoints, actual scene transactions, protected identities, invalid JSON/fields, detached collisions, rollback, stale drafts, persistent undo and the live-world tool worker. The shared modules use six executable harnesses in addition to JUnit.

## Continuity and local tools

The next implementation slice adds `JournalContinuityTest` and `LocalSkillsTest` to the normal `check` dependency graph. These verify every journal field, independent Merkle roots, legacy format compatibility, explicit anchored tail recovery with byte-preserved evidence, staged-crash restart, failed writes, same-process writers and concurrent JVM writers. They execute all five local handlers, actual JDK parsing/diagnostics, persisted memory search, generated docs, positive/negative hashes, capability/schema denial, path boundaries and restored/unfinished receipts.

The local Windows symlink creation probe was skipped because the process lacks the required privilege. Absolute paths and traversal were tested. The tools enforce application path/capability checks; these tests do not establish an OS sandbox, signature authentication or hardware power-loss durability.

The native Studio script now requires all six tabs and a completed `world.inspect` receipt. It runs twice with real local models and checks stable scene identities/physics, persisted chat and scene restore. Current slice evidence is recorded in `evidence/continuity-skills-verification.json`.

## Native surface

`scripts/Verify-Native-Factory.ps1` launches the packaged application twice in a test project. It checks real native rendering, physics inspector rendering, checkpoint restore and stable identities across process restart.

`Verify-Native-Studio.ps1` and `Verify-Native-SceneAgent.ps1` are optional live-model checks. They require the named installed models, configured local catalog and running Ollama services. They do not fetch weights. Their test projects are under `build/`.

## Isolation surface

Validate a clean clone outside every original checkout. Confirm Gradle resolves all project/source paths inside that clone, that an external `fraynixRoot` override is refused, that no nested Git repositories/submodules exist, and that Git ignores user data and generated module outputs. Inspect the distribution for its launcher, three module JARs, native dependencies and assets.

## Local model evidence

The GPT-OSS 120B test is documented separately in `MODEL_TEST_RESULTS.md`. It ran against the same scene protocol and actual JBox2D validator before extraction. It is historical measured model evidence. Client timeouts are now configurable for Chat, but server-side loading is separate and cold 120B operation was not repeated for the live-chat release.

The final extraction results are recorded in `evidence/standalone-verification.json` after verification completes.

## October 23: live model controls

The full check passed with 58 JUnit tests and six executable module harnesses. Native restart checks used the actual Ultimate and bicameral models on ports 11434 and 11435, verified HTTP streams and fresh nonce responses, rendered all six tabs and retained fixture identities/physics. Real load/unload residency was also verified. See [live controls](LIVE_MODEL_CONTROLS.md) and [sanitized measurements](evidence/live-model-controls-2026-10-23.json).

## October 23: README edition 001

The local offline `:check` passed with 58 JUnit tests, zero failures and six
module harnesses. The previous hosted run exceeded the repeated-inspection
test's 30-second asynchronous deadline. That test now allows 120 seconds and
reports session status on timeout; its identity, component-count and
idempotence assertions remain unchanged. Local success and hosted success are
separate evidence; use Actions for the exact publication commit.

`scripts/Verify-Documentation.ps1` validates sequential edition numbers,
normalized text hashes, frozen assets, local links, wiki links, JSON Canvas
references and the viewer's embedded datasets. Browser checks covered module
search/state filtering, node selection, zoom/reset, section navigation, file
search, screenshot expansion, keyboard search focus and a 390-pixel viewport.
All four gallery images loaded and no warning/error console entries were
observed. The browser preview served only the frozen documentation directory
on loopback; other checkout links were checked by the documentation verifier.

See [README-001](../README-001.md), its
[verification record](readmes/001/verification.json), and the
[edition policy](readmes/README_SEQUENCE.md).
