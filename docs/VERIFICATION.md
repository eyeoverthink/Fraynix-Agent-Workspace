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

The inherited integration has 56 editor JUnit tests. Those cover geometry, physics registration, picking/placement coordinates, serialization, checkpoints, actual scene transactions, protected identities, invalid JSON/fields, detached collisions, rollback, stale drafts and persistent undo. The shared modules use four executable harnesses in addition to JUnit.

## Native surface

`scripts/Verify-Native-Factory.ps1` launches the packaged application twice in a test project. It checks real native rendering, physics inspector rendering, checkpoint restore and stable identities across process restart.

`Verify-Native-Studio.ps1` and `Verify-Native-SceneAgent.ps1` are optional live-model checks. They require the named installed models, configured local catalog and running Ollama services. They do not fetch weights. Their test projects are under `build/`.

## Isolation surface

Validate a clean clone outside every original checkout. Confirm Gradle resolves all project/source paths inside that clone, that an external `fraynixRoot` override is refused, that no nested Git repositories/submodules exist, and that Git ignores user data and generated module outputs. Inspect the distribution for its launcher, three module JARs, native dependencies and assets.

## Local model evidence

The GPT-OSS 120B test is documented separately in `MODEL_TEST_RESULTS.md`. It ran against the same scene protocol and actual JBox2D validator before extraction. It is retained as measured model evidence, not a claim that the new application's two-minute bridge timeout already supports its cold start.

The final extraction results are recorded in `evidence/standalone-verification.json` after verification completes.
