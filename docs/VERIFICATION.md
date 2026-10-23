# Verification

The independent Studio project requires validation after extraction; historical integration results alone do not establish its build status.

## Automated surface

`gradlew.bat clean check distZip installDist` compiles the editor and all three checked-in modules, runs the editor JUnit suite, runs the executable runtime/transaction/kernel/model harnesses, and packages the application.

The inherited integration has 56 editor JUnit tests. Those cover geometry, physics registration, picking/placement coordinates, serialization, checkpoints, actual scene transactions, protected identities, invalid JSON/fields, detached collisions, rollback, stale drafts and persistent undo. The shared modules use four executable harnesses in addition to JUnit.

## Native surface

`scripts/Verify-Native-Factory.ps1` launches the packaged application twice in a test project. It checks real native rendering, physics inspector rendering, checkpoint restore and stable identities across process restart.

`Verify-Native-Studio.ps1` and `Verify-Native-SceneAgent.ps1` are optional live-model checks. They require the named installed models, configured local catalog and running Ollama services. They do not fetch weights. Their test projects are under `build/`.

## Isolation surface

Validate a clean clone outside every original checkout. Confirm Gradle resolves all project/source paths inside that clone, that an external `fraynixRoot` override is refused, that no nested Git repositories/submodules exist, and that Git ignores user data and generated module outputs. Inspect the distribution for its launcher, three module JARs, native dependencies and assets.

## Local model evidence

The GPT-OSS 120B test is documented separately in `MODEL_TEST_RESULTS.md`. It ran against the same scene protocol and actual JBox2D validator before extraction. It is retained as measured model evidence, not a claim that the new application's two-minute bridge timeout already supports its cold start.

The final extraction results are recorded in `evidence/standalone-verification.json` after verification completes.
