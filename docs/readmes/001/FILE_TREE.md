# Repository file tree / edition 001

Snapshot of tracked and non-ignored files at publication preparation. Runtime/build state and model weights are excluded. Generated snapshot assets are included.

[Edition 001](../../../README-001.md) · [Interactive path search](index.html#tree) · [JSON inventory](file-inventory.json)

```text
Fraynix-Studio/
├── .gitattributes
├── .github/
│   └── workflows/
│       └── build.yml
├── .gitignore
├── AGENTS.md
├── Launch-Fraynix-Studio.bat
├── README-001.md
├── README.md
├── THIRD_PARTY_NOTICES.md
├── assets/
│   ├── images/
│   │   ├── blendImage1.png
│   │   ├── blendImage2.png
│   │   ├── gizmos.png
│   │   ├── spritesheet.png
│   │   ├── spritesheets/
│   │   │   ├── decorationsAndBlocks.png
│   │   │   ├── icons.png
│   │   │   └── pipes.png
│   │   ├── testImage.png
│   │   └── testImage2.png
│   └── shaders/
│       ├── debugLine2D.glsl
│       ├── default.glsl
│       └── pickingShader.glsl
├── build.gradle
├── docs/
│   ├── ARCHITECTURE.md
│   ├── CONTINUITY_AND_SKILLS.md
│   ├── DREAMER_120B_REVIEW.md
│   ├── DREAMER_120B_SETUP.md
│   ├── IMPLEMENTATION_PLAN.md
│   ├── LIVE_MODEL_CONTROLS.md
│   ├── MODELS_AGENCY_REVIEW.md
│   ├── MODEL_TEST_RESULTS.md
│   ├── PROVENANCE.md
│   ├── VERIFICATION.md
│   ├── evidence/
│   │   ├── continuity-skills-verification.json
│   │   ├── dreamer120b-activation-2026-10-23.json
│   │   ├── dreamer120b-input-inventory.json
│   │   ├── dreamer120b-review-2026-10-23.json
│   │   ├── gpt-oss-120b-2026-10-22.json
│   │   ├── live-model-controls-2026-10-23.json
│   │   ├── models-agency-input-inventory.tsv
│   │   ├── models-agency-review-2026-10-22.json
│   │   └── standalone-verification.json
│   ├── images/
│   │   ├── creator-preview.png
│   │   ├── dreamer120b-live.png
│   │   ├── live-model-controls.png
│   │   └── tools-preview.png
│   ├── obsidian/
│   │   ├── 00-home.md
│   │   ├── 01-studio.md
│   │   ├── 01-system.canvas
│   │   ├── 02-lifecycle.canvas
│   │   ├── 02-world-and-physics.md
│   │   ├── 03-local-models.md
│   │   ├── 03-roadmap.canvas
│   │   ├── 04-memory-and-continuity.md
│   │   ├── 05-tools-and-jobs.md
│   │   ├── 06-agent-factory-plan.md
│   │   ├── 07-evidence.md
│   │   └── 08-edition-history.md
│   ├── provenance/
│   │   └── IMPORT_MANIFEST.json
│   └── readmes/
│       ├── 001/
│       │   ├── FILE_TREE.md
│       │   ├── file-inventory.json
│       │   ├── images/
│       │   │   ├── creator-preview.png
│       │   │   ├── dreamer120b-live.png
│       │   │   ├── live-model-controls.png
│       │   │   └── tools-preview.png
│       │   ├── index.html
│       │   ├── obsidian/
│       │   │   ├── 00-home.md
│       │   │   ├── 01-studio.md
│       │   │   ├── 01-system.canvas
│       │   │   ├── 02-lifecycle.canvas
│       │   │   ├── 02-world-and-physics.md
│       │   │   ├── 03-local-models.md
│       │   │   ├── 03-roadmap.canvas
│       │   │   ├── 04-memory-and-continuity.md
│       │   │   ├── 05-tools-and-jobs.md
│       │   │   ├── 06-agent-factory-plan.md
│       │   │   ├── 07-evidence.md
│       │   │   └── 08-edition-history.md
│       │   ├── system-map.json
│       │   └── verification.json
│       ├── INDEX.md
│       ├── README_SEQUENCE.md
│       └── editions.json
├── fraynix-modules/
│   ├── factory-kernel/
│   │   ├── build.gradle
│   │   └── src/
│   │       ├── main/
│   │       │   └── java/
│   │       │       ├── brain_architecture/
│   │       │       │   ├── integration/
│   │       │       │   │   └── unified/
│   │       │       │   │       ├── UnifiedDiagnosticEntry.java
│   │       │       │   │       ├── UnifiedDiagnostics.java
│   │       │       │   │       ├── UnifiedEvolutionEvaluation.java
│   │       │       │   │       ├── UnifiedEvolutionIteration.java
│   │       │       │   │       ├── UnifiedExecutionTrace.java
│   │       │       │   │       ├── UnifiedJournalEntry.java
│   │       │       │   │       ├── UnifiedKernelGateway.java
│   │       │       │   │       ├── UnifiedMemoryHit.java
│   │       │       │   │       ├── UnifiedMutationProposal.java
│   │       │       │   │       ├── UnifiedProposalPreview.java
│   │       │       │   │       ├── UnifiedResult.java
│   │       │       │   │       └── UnifiedTask.java
│   │       │       │   └── kernel/
│   │       │       │       ├── CanonicalTruthAuditor.java
│   │       │       │       ├── ClaimRecord.java
│   │       │       │       ├── ClaimStatus.java
│   │       │       │       ├── CorpusMutationSeed.java
│   │       │       │       ├── EvolutionReport.java
│   │       │       │       ├── ExecutionTrace.java
│   │       │       │       ├── F0RuntimeSubstrateAdapter.java
│   │       │       │       ├── FoundationalSubstrateBootstrapper.java
│   │       │       │       ├── FoundationalSubstrateReport.java
│   │       │       │       ├── GgufRuntimeModule.java
│   │       │       │       ├── GgufTokenPrimitiveEngine.java
│   │       │       │       ├── HybridMemoryStore.java
│   │       │       │       ├── KernelConfiguration.java
│   │       │       │       ├── KernelExecutionBridgeAdapter.java
│   │       │       │       ├── KernelHypervectorSpace.java
│   │       │       │       ├── KernelRunResult.java
│   │       │       │       ├── KernelStateStore.java
│   │       │       │       ├── KernelVerifier.java
│   │       │       │       ├── LocalModelDescriptor.java
│   │       │       │       ├── LocalModelRuntimeAdapter.java
│   │       │       │       ├── LocalModelRuntimeDescriptor.java
│   │       │       │       ├── LocalModelSubstrateCatalog.java
│   │       │       │       ├── MemoryBenchmarkReport.java
│   │       │       │       ├── MemoryDocument.java
│   │       │       │       ├── MemoryHit.java
│   │       │       │       ├── MerkleAppendOnlyJournal.java
│   │       │       │       ├── MutationCandidate.java
│   │       │       │       ├── MutationOperation.java
│   │       │       │       ├── MutationProposal.java
│   │       │       │       ├── MutationProposalEngine.java
│   │       │       │       ├── MutationProposalPreview.java
│   │       │       │       ├── OfflineKernel.java
│   │       │       │       ├── OfflineKernelBenchmark.java
│   │       │       │       ├── OfflineKernelCli.java
│   │       │       │       ├── OfflineKernelExample.java
│   │       │       │       ├── OfflineToolchainManifest.java
│   │       │       │       ├── OptionalPdfTextExtractor.java
│   │       │       │       ├── PdfDistillationChunk.java
│   │       │       │       ├── PdfDistillationEngine.java
│   │       │       │       ├── PdfDistillationReport.java
│   │       │       │       ├── PlanActionHint.java
│   │       │       │       ├── PlanStep.java
│   │       │       │       ├── ProjectMemoryArtifactWriter.java
│   │       │       │       ├── RepairMemoryStore.java
│   │       │       │       ├── RetrievalProfile.java
│   │       │       │       ├── ShadowMutationEngine.java
│   │       │       │       ├── StrategyBanditRouter.java
│   │       │       │       ├── SubstrateAction.java
│   │       │       │       ├── SubstrateAdapter.java
│   │       │       │       ├── SubstrateResult.java
│   │       │       │       ├── SubstrateSession.java
│   │       │       │       ├── SubstrateSnapshot.java
│   │       │       │       ├── TaskBudget.java
│   │       │       │       ├── TaskEnvelope.java
│   │       │       │       ├── TaskMode.java
│   │       │       │       ├── TaskPlanner.java
│   │       │       │       ├── TokenizerDigest.java
│   │       │       │       ├── TraceReplayStore.java
│   │       │       │       ├── UserSpaceSubstrateAdapter.java
│   │       │       │       ├── ValidationCommand.java
│   │       │       │       ├── ValidationSuite.java
│   │       │       │       ├── ValidationSuiteFactory.java
│   │       │       │       ├── VerifierDiagnostic.java
│   │       │       │       ├── VerifierResult.java
│   │       │       │       ├── WebServer.java
│   │       │       │       └── WorkspaceProfile.java
│   │       │       └── fraymus/
│   │       │           ├── causal/
│   │       │           │   ├── CausalModel.java
│   │       │           │   ├── DAGVerifier.java
│   │       │           │   ├── DoCalculusEngine.java
│   │       │           │   └── SCMEnforcementEngine.java
│   │       │           ├── execution/
│   │       │           │   └── ExecutionBridge.java
│   │       │           ├── factory/
│   │       │           │   └── kernel/
│   │       │           │       ├── ArtifactValidator.java
│   │       │           │       ├── FactoryDemo.java
│   │       │           │       ├── FactoryKernelService.java
│   │       │           │       ├── LocalSkillDispatcher.java
│   │       │           │       ├── SkillSchema.java
│   │       │           │       └── StudioSkillCli.java
│   │       │           ├── hdc/
│   │       │           │   └── SharedHypervectorCore.java
│   │       │           └── llm/
│   │       │               └── ModelLoader.java
│   │       └── test/
│   │           └── java/
│   │               └── fraymus/
│   │                   └── factory/
│   │                       └── kernel/
│   │                           ├── FactoryKernelIntegrationTest.java
│   │                           ├── JournalContinuityTest.java
│   │                           └── LocalSkillsTest.java
│   ├── factory-models/
│   │   ├── build.gradle
│   │   └── src/
│   │       ├── main/
│   │       │   └── java/
│   │       │       └── fraymus/
│   │       │           ├── OllamaIntegration.java
│   │       │           ├── factory/
│   │       │           │   └── models/
│   │       │           │       ├── ChatContext.java
│   │       │           │       ├── ConversationStore.java
│   │       │           │       ├── FactoryModelService.java
│   │       │           │       ├── ModelProfileStore.java
│   │       │           │       └── ModelRunSettings.java
│   │       │           ├── llm/
│   │       │           │   ├── CapabilityProfile.java
│   │       │           │   ├── LocalModelCatalog.java
│   │       │           │   ├── ModelAbstractionLayer.java
│   │       │           │   ├── ModelBackend.java
│   │       │           │   ├── ModelDescriptor.java
│   │       │           │   ├── ModelRole.java
│   │       │           │   ├── ModelSelectionPolicy.java
│   │       │           │   ├── ModelSelectionResult.java
│   │       │           │   └── OpenModelRegistry.java
│   │       │           └── model/
│   │       │               ├── CapabilityProfile.java
│   │       │               ├── LocalOllamaModelAdapter.java
│   │       │               ├── ModelAdapter.java
│   │       │               └── ModelDescriptor.java
│   │       └── test/
│   │           └── java/
│   │               └── fraymus/
│   │                   └── factory/
│   │                       └── models/
│   │                           └── FactoryModelsTest.java
│   └── factory-runtime/
│       ├── build.gradle
│       └── src/
│           ├── main/
│           │   └── java/
│           │       └── fraymus/
│           │           └── factory/
│           │               ├── ComponentRecord.java
│           │               ├── ComponentSchema.java
│           │               ├── EntityRecord.java
│           │               ├── FactoryWorld.java
│           │               ├── SchemaRegistry.java
│           │               ├── SpatialIndex2D.java
│           │               ├── WorldCheckpointStore.java
│           │               ├── WorldSnapshot.java
│           │               └── WorldTransaction.java
│           └── test/
│               └── java/
│                   └── fraymus/
│                       └── factory/
│                           ├── FactoryRuntimeTest.java
│                           └── WorldTransactionTest.java
├── gradle.lockfile
├── gradle/
│   └── wrapper/
│       ├── gradle-wrapper.jar
│       └── gradle-wrapper.properties
├── gradlew
├── gradlew.bat
├── level.txt
├── libs/
│   └── jbox2d-library.jar
├── models/
│   ├── FRAYNIX-DREAMER-120B.Modelfile
│   └── FRAYNIX-DREAMER-120B.candidate.Modelfile
├── offline-toolchain.toml
├── scripts/
│   ├── Configure-Models.ps1
│   ├── Run-Factory-Test.ps1
│   ├── Run-Kernel.ps1
│   ├── Run-Studio.ps1
│   ├── Start-FDrive-Ollama.ps1
│   ├── Verify-Documentation.ps1
│   ├── Verify-Native-Factory.ps1
│   ├── Verify-Native-SceneAgent.ps1
│   ├── Verify-Native-Studio.ps1
│   └── Verify-Studio.ps1
├── settings.gradle
└── src/
    ├── main/
    │   ├── java/
    │   │   ├── Main.java
    │   │   ├── components/
    │   │   │   ├── Component.java
    │   │   │   ├── ComponentDeserializer.java
    │   │   │   ├── EditorCamera.java
    │   │   │   ├── FontRenderer.java
    │   │   │   ├── Gizmo.java
    │   │   │   ├── GizmoSystem.java
    │   │   │   ├── GridLines.java
    │   │   │   ├── MouseControls.java
    │   │   │   ├── NonPickable.java
    │   │   │   ├── ScaleGizmo.java
    │   │   │   ├── Sprite.java
    │   │   │   ├── SpriteRenderer.java
    │   │   │   ├── Spritesheet.java
    │   │   │   └── TranslateGizmo.java
    │   │   ├── editor/
    │   │   │   ├── FraynixPanel.java
    │   │   │   ├── FraynixWorkspace.java
    │   │   │   ├── GameViewWindow.java
    │   │   │   ├── JImGui.java
    │   │   │   ├── MenuBar.java
    │   │   │   ├── PhysicsComponents.java
    │   │   │   ├── PropertiesWindow.java
    │   │   │   ├── SceneAgent.java
    │   │   │   ├── SceneAgentNativeSmoke.java
    │   │   │   ├── SceneAgentPanel.java
    │   │   │   ├── ScenePanel.java
    │   │   │   ├── StudioFrameCapture.java
    │   │   │   ├── StudioLayout.java
    │   │   │   └── ViewportCoordinates.java
    │   │   ├── factory/
    │   │   │   ├── FactorySceneAdapter.java
    │   │   │   ├── FactorySession.java
    │   │   │   ├── SceneEditHistory.java
    │   │   │   ├── SceneJson.java
    │   │   │   ├── SceneKnowledge.java
    │   │   │   ├── SceneProposal.java
    │   │   │   └── SceneSimulation.java
    │   │   ├── jade/
    │   │   │   ├── Camera.java
    │   │   │   ├── GameObject.java
    │   │   │   ├── GameObjectDeserializer.java
    │   │   │   ├── ImGuiLayer.java
    │   │   │   ├── KeyListener.java
    │   │   │   ├── MouseListener.java
    │   │   │   ├── Prefabs.java
    │   │   │   ├── Transform.java
    │   │   │   └── Window.java
    │   │   ├── observers/
    │   │   │   ├── EventSystem.java
    │   │   │   ├── Observer.java
    │   │   │   └── events/
    │   │   │       ├── Event.java
    │   │   │       └── EventType.java
    │   │   ├── physics2d/
    │   │   │   ├── Physics2D.java
    │   │   │   ├── components/
    │   │   │   │   ├── Box2DCollider.java
    │   │   │   │   ├── CircleCollider.java
    │   │   │   │   ├── Collider.java
    │   │   │   │   └── Rigidbody2D.java
    │   │   │   └── enums/
    │   │   │       └── BodyType.java
    │   │   ├── physics2dtmp/
    │   │   │   ├── PhysicsSystem2D.java
    │   │   │   ├── forces/
    │   │   │   │   ├── ForceGenerator.java
    │   │   │   │   ├── ForceRegistration.java
    │   │   │   │   ├── ForceRegistry.java
    │   │   │   │   └── Gravity2D.java
    │   │   │   ├── primitives/
    │   │   │   │   ├── AABB.java
    │   │   │   │   ├── Box2D.java
    │   │   │   │   ├── Circle.java
    │   │   │   │   ├── Collider2D.java
    │   │   │   │   ├── Ray2D.java
    │   │   │   │   └── RaycastResult.java
    │   │   │   └── rigidbody/
    │   │   │       ├── CollisionManifold.java
    │   │   │       ├── Collisions.java
    │   │   │       ├── IntersectionDetector2D.java
    │   │   │       └── Rigidbody2D.java
    │   │   ├── renderer/
    │   │   │   ├── DebugDraw.java
    │   │   │   ├── Framebuffer.java
    │   │   │   ├── Line2D.java
    │   │   │   ├── PickingTexture.java
    │   │   │   ├── RenderBatch.java
    │   │   │   ├── Renderer.java
    │   │   │   ├── Shader.java
    │   │   │   └── Texture.java
    │   │   ├── scenes/
    │   │   │   ├── LevelEditorSceneInitializer.java
    │   │   │   ├── Scene.java
    │   │   │   └── SceneInitializer.java
    │   │   └── util/
    │   │       ├── AssetPool.java
    │   │       ├── GridPlacement.java
    │   │       ├── JMath.java
    │   │       └── Settings.java
    │   └── resources/
    │       └── META-INF/
    │           └── MANIFEST.MF
    └── test/
        └── java/
            ├── editor/
            │   └── FraynixWorkspaceTest.java
            ├── factory/
            │   ├── EditorPhysicsRegressionTest.java
            │   ├── FactorySceneIntegrationTest.java
            │   ├── GridPlacementRegressionTest.java
            │   └── SceneAgentIntegrationTest.java
            └── physics2dtmp/
                └── CollisionDetectorTests.java
```

## File links

- [.gitattributes](../../../.gitattributes)
- [.github/workflows/build.yml](../../../.github/workflows/build.yml)
- [.gitignore](../../../.gitignore)
- [AGENTS.md](../../../AGENTS.md)
- [Launch-Fraynix-Studio.bat](../../../Launch-Fraynix-Studio.bat)
- [README-001.md](../../../README-001.md)
- [README.md](../../../README.md)
- [THIRD_PARTY_NOTICES.md](../../../THIRD_PARTY_NOTICES.md)
- [assets/images/blendImage1.png](../../../assets/images/blendImage1.png)
- [assets/images/blendImage2.png](../../../assets/images/blendImage2.png)
- [assets/images/gizmos.png](../../../assets/images/gizmos.png)
- [assets/images/spritesheet.png](../../../assets/images/spritesheet.png)
- [assets/images/spritesheets/decorationsAndBlocks.png](../../../assets/images/spritesheets/decorationsAndBlocks.png)
- [assets/images/spritesheets/icons.png](../../../assets/images/spritesheets/icons.png)
- [assets/images/spritesheets/pipes.png](../../../assets/images/spritesheets/pipes.png)
- [assets/images/testImage.png](../../../assets/images/testImage.png)
- [assets/images/testImage2.png](../../../assets/images/testImage2.png)
- [assets/shaders/debugLine2D.glsl](../../../assets/shaders/debugLine2D.glsl)
- [assets/shaders/default.glsl](../../../assets/shaders/default.glsl)
- [assets/shaders/pickingShader.glsl](../../../assets/shaders/pickingShader.glsl)
- [build.gradle](../../../build.gradle)
- [docs/ARCHITECTURE.md](../../../docs/ARCHITECTURE.md)
- [docs/CONTINUITY_AND_SKILLS.md](../../../docs/CONTINUITY_AND_SKILLS.md)
- [docs/DREAMER_120B_REVIEW.md](../../../docs/DREAMER_120B_REVIEW.md)
- [docs/DREAMER_120B_SETUP.md](../../../docs/DREAMER_120B_SETUP.md)
- [docs/IMPLEMENTATION_PLAN.md](../../../docs/IMPLEMENTATION_PLAN.md)
- [docs/LIVE_MODEL_CONTROLS.md](../../../docs/LIVE_MODEL_CONTROLS.md)
- [docs/MODELS_AGENCY_REVIEW.md](../../../docs/MODELS_AGENCY_REVIEW.md)
- [docs/MODEL_TEST_RESULTS.md](../../../docs/MODEL_TEST_RESULTS.md)
- [docs/PROVENANCE.md](../../../docs/PROVENANCE.md)
- [docs/VERIFICATION.md](../../../docs/VERIFICATION.md)
- [docs/evidence/continuity-skills-verification.json](../../../docs/evidence/continuity-skills-verification.json)
- [docs/evidence/dreamer120b-activation-2026-10-23.json](../../../docs/evidence/dreamer120b-activation-2026-10-23.json)
- [docs/evidence/dreamer120b-input-inventory.json](../../../docs/evidence/dreamer120b-input-inventory.json)
- [docs/evidence/dreamer120b-review-2026-10-23.json](../../../docs/evidence/dreamer120b-review-2026-10-23.json)
- [docs/evidence/gpt-oss-120b-2026-10-22.json](../../../docs/evidence/gpt-oss-120b-2026-10-22.json)
- [docs/evidence/live-model-controls-2026-10-23.json](../../../docs/evidence/live-model-controls-2026-10-23.json)
- [docs/evidence/models-agency-input-inventory.tsv](../../../docs/evidence/models-agency-input-inventory.tsv)
- [docs/evidence/models-agency-review-2026-10-22.json](../../../docs/evidence/models-agency-review-2026-10-22.json)
- [docs/evidence/standalone-verification.json](../../../docs/evidence/standalone-verification.json)
- [docs/images/creator-preview.png](../../../docs/images/creator-preview.png)
- [docs/images/dreamer120b-live.png](../../../docs/images/dreamer120b-live.png)
- [docs/images/live-model-controls.png](../../../docs/images/live-model-controls.png)
- [docs/images/tools-preview.png](../../../docs/images/tools-preview.png)
- [docs/obsidian/00-home.md](../../../docs/obsidian/00-home.md)
- [docs/obsidian/01-studio.md](../../../docs/obsidian/01-studio.md)
- [docs/obsidian/01-system.canvas](../../../docs/obsidian/01-system.canvas)
- [docs/obsidian/02-lifecycle.canvas](../../../docs/obsidian/02-lifecycle.canvas)
- [docs/obsidian/02-world-and-physics.md](../../../docs/obsidian/02-world-and-physics.md)
- [docs/obsidian/03-local-models.md](../../../docs/obsidian/03-local-models.md)
- [docs/obsidian/03-roadmap.canvas](../../../docs/obsidian/03-roadmap.canvas)
- [docs/obsidian/04-memory-and-continuity.md](../../../docs/obsidian/04-memory-and-continuity.md)
- [docs/obsidian/05-tools-and-jobs.md](../../../docs/obsidian/05-tools-and-jobs.md)
- [docs/obsidian/06-agent-factory-plan.md](../../../docs/obsidian/06-agent-factory-plan.md)
- [docs/obsidian/07-evidence.md](../../../docs/obsidian/07-evidence.md)
- [docs/obsidian/08-edition-history.md](../../../docs/obsidian/08-edition-history.md)
- [docs/provenance/IMPORT_MANIFEST.json](../../../docs/provenance/IMPORT_MANIFEST.json)
- [docs/readmes/001/FILE_TREE.md](../../../docs/readmes/001/FILE_TREE.md)
- [docs/readmes/001/file-inventory.json](../../../docs/readmes/001/file-inventory.json)
- [docs/readmes/001/images/creator-preview.png](../../../docs/readmes/001/images/creator-preview.png)
- [docs/readmes/001/images/dreamer120b-live.png](../../../docs/readmes/001/images/dreamer120b-live.png)
- [docs/readmes/001/images/live-model-controls.png](../../../docs/readmes/001/images/live-model-controls.png)
- [docs/readmes/001/images/tools-preview.png](../../../docs/readmes/001/images/tools-preview.png)
- [docs/readmes/001/index.html](../../../docs/readmes/001/index.html)
- [docs/readmes/001/obsidian/00-home.md](../../../docs/readmes/001/obsidian/00-home.md)
- [docs/readmes/001/obsidian/01-studio.md](../../../docs/readmes/001/obsidian/01-studio.md)
- [docs/readmes/001/obsidian/01-system.canvas](../../../docs/readmes/001/obsidian/01-system.canvas)
- [docs/readmes/001/obsidian/02-lifecycle.canvas](../../../docs/readmes/001/obsidian/02-lifecycle.canvas)
- [docs/readmes/001/obsidian/02-world-and-physics.md](../../../docs/readmes/001/obsidian/02-world-and-physics.md)
- [docs/readmes/001/obsidian/03-local-models.md](../../../docs/readmes/001/obsidian/03-local-models.md)
- [docs/readmes/001/obsidian/03-roadmap.canvas](../../../docs/readmes/001/obsidian/03-roadmap.canvas)
- [docs/readmes/001/obsidian/04-memory-and-continuity.md](../../../docs/readmes/001/obsidian/04-memory-and-continuity.md)
- [docs/readmes/001/obsidian/05-tools-and-jobs.md](../../../docs/readmes/001/obsidian/05-tools-and-jobs.md)
- [docs/readmes/001/obsidian/06-agent-factory-plan.md](../../../docs/readmes/001/obsidian/06-agent-factory-plan.md)
- [docs/readmes/001/obsidian/07-evidence.md](../../../docs/readmes/001/obsidian/07-evidence.md)
- [docs/readmes/001/obsidian/08-edition-history.md](../../../docs/readmes/001/obsidian/08-edition-history.md)
- [docs/readmes/001/system-map.json](../../../docs/readmes/001/system-map.json)
- [docs/readmes/001/verification.json](../../../docs/readmes/001/verification.json)
- [docs/readmes/INDEX.md](../../../docs/readmes/INDEX.md)
- [docs/readmes/README_SEQUENCE.md](../../../docs/readmes/README_SEQUENCE.md)
- [docs/readmes/editions.json](../../../docs/readmes/editions.json)
- [fraynix-modules/factory-kernel/build.gradle](../../../fraynix-modules/factory-kernel/build.gradle)
- [fraynix-modules/factory-kernel/src/main/java/brain_architecture/integration/unified/UnifiedDiagnosticEntry.java](../../../fraynix-modules/factory-kernel/src/main/java/brain_architecture/integration/unified/UnifiedDiagnosticEntry.java)
- [fraynix-modules/factory-kernel/src/main/java/brain_architecture/integration/unified/UnifiedDiagnostics.java](../../../fraynix-modules/factory-kernel/src/main/java/brain_architecture/integration/unified/UnifiedDiagnostics.java)
- [fraynix-modules/factory-kernel/src/main/java/brain_architecture/integration/unified/UnifiedEvolutionEvaluation.java](../../../fraynix-modules/factory-kernel/src/main/java/brain_architecture/integration/unified/UnifiedEvolutionEvaluation.java)
- [fraynix-modules/factory-kernel/src/main/java/brain_architecture/integration/unified/UnifiedEvolutionIteration.java](../../../fraynix-modules/factory-kernel/src/main/java/brain_architecture/integration/unified/UnifiedEvolutionIteration.java)
- [fraynix-modules/factory-kernel/src/main/java/brain_architecture/integration/unified/UnifiedExecutionTrace.java](../../../fraynix-modules/factory-kernel/src/main/java/brain_architecture/integration/unified/UnifiedExecutionTrace.java)
- [fraynix-modules/factory-kernel/src/main/java/brain_architecture/integration/unified/UnifiedJournalEntry.java](../../../fraynix-modules/factory-kernel/src/main/java/brain_architecture/integration/unified/UnifiedJournalEntry.java)
- [fraynix-modules/factory-kernel/src/main/java/brain_architecture/integration/unified/UnifiedKernelGateway.java](../../../fraynix-modules/factory-kernel/src/main/java/brain_architecture/integration/unified/UnifiedKernelGateway.java)
- [fraynix-modules/factory-kernel/src/main/java/brain_architecture/integration/unified/UnifiedMemoryHit.java](../../../fraynix-modules/factory-kernel/src/main/java/brain_architecture/integration/unified/UnifiedMemoryHit.java)
- [fraynix-modules/factory-kernel/src/main/java/brain_architecture/integration/unified/UnifiedMutationProposal.java](../../../fraynix-modules/factory-kernel/src/main/java/brain_architecture/integration/unified/UnifiedMutationProposal.java)
- [fraynix-modules/factory-kernel/src/main/java/brain_architecture/integration/unified/UnifiedProposalPreview.java](../../../fraynix-modules/factory-kernel/src/main/java/brain_architecture/integration/unified/UnifiedProposalPreview.java)
- [fraynix-modules/factory-kernel/src/main/java/brain_architecture/integration/unified/UnifiedResult.java](../../../fraynix-modules/factory-kernel/src/main/java/brain_architecture/integration/unified/UnifiedResult.java)
- [fraynix-modules/factory-kernel/src/main/java/brain_architecture/integration/unified/UnifiedTask.java](../../../fraynix-modules/factory-kernel/src/main/java/brain_architecture/integration/unified/UnifiedTask.java)
- [fraynix-modules/factory-kernel/src/main/java/brain_architecture/kernel/CanonicalTruthAuditor.java](../../../fraynix-modules/factory-kernel/src/main/java/brain_architecture/kernel/CanonicalTruthAuditor.java)
- [fraynix-modules/factory-kernel/src/main/java/brain_architecture/kernel/ClaimRecord.java](../../../fraynix-modules/factory-kernel/src/main/java/brain_architecture/kernel/ClaimRecord.java)
- [fraynix-modules/factory-kernel/src/main/java/brain_architecture/kernel/ClaimStatus.java](../../../fraynix-modules/factory-kernel/src/main/java/brain_architecture/kernel/ClaimStatus.java)
- [fraynix-modules/factory-kernel/src/main/java/brain_architecture/kernel/CorpusMutationSeed.java](../../../fraynix-modules/factory-kernel/src/main/java/brain_architecture/kernel/CorpusMutationSeed.java)
- [fraynix-modules/factory-kernel/src/main/java/brain_architecture/kernel/EvolutionReport.java](../../../fraynix-modules/factory-kernel/src/main/java/brain_architecture/kernel/EvolutionReport.java)
- [fraynix-modules/factory-kernel/src/main/java/brain_architecture/kernel/ExecutionTrace.java](../../../fraynix-modules/factory-kernel/src/main/java/brain_architecture/kernel/ExecutionTrace.java)
- [fraynix-modules/factory-kernel/src/main/java/brain_architecture/kernel/F0RuntimeSubstrateAdapter.java](../../../fraynix-modules/factory-kernel/src/main/java/brain_architecture/kernel/F0RuntimeSubstrateAdapter.java)
- [fraynix-modules/factory-kernel/src/main/java/brain_architecture/kernel/FoundationalSubstrateBootstrapper.java](../../../fraynix-modules/factory-kernel/src/main/java/brain_architecture/kernel/FoundationalSubstrateBootstrapper.java)
- [fraynix-modules/factory-kernel/src/main/java/brain_architecture/kernel/FoundationalSubstrateReport.java](../../../fraynix-modules/factory-kernel/src/main/java/brain_architecture/kernel/FoundationalSubstrateReport.java)
- [fraynix-modules/factory-kernel/src/main/java/brain_architecture/kernel/GgufRuntimeModule.java](../../../fraynix-modules/factory-kernel/src/main/java/brain_architecture/kernel/GgufRuntimeModule.java)
- [fraynix-modules/factory-kernel/src/main/java/brain_architecture/kernel/GgufTokenPrimitiveEngine.java](../../../fraynix-modules/factory-kernel/src/main/java/brain_architecture/kernel/GgufTokenPrimitiveEngine.java)
- [fraynix-modules/factory-kernel/src/main/java/brain_architecture/kernel/HybridMemoryStore.java](../../../fraynix-modules/factory-kernel/src/main/java/brain_architecture/kernel/HybridMemoryStore.java)
- [fraynix-modules/factory-kernel/src/main/java/brain_architecture/kernel/KernelConfiguration.java](../../../fraynix-modules/factory-kernel/src/main/java/brain_architecture/kernel/KernelConfiguration.java)
- [fraynix-modules/factory-kernel/src/main/java/brain_architecture/kernel/KernelExecutionBridgeAdapter.java](../../../fraynix-modules/factory-kernel/src/main/java/brain_architecture/kernel/KernelExecutionBridgeAdapter.java)
- [fraynix-modules/factory-kernel/src/main/java/brain_architecture/kernel/KernelHypervectorSpace.java](../../../fraynix-modules/factory-kernel/src/main/java/brain_architecture/kernel/KernelHypervectorSpace.java)
- [fraynix-modules/factory-kernel/src/main/java/brain_architecture/kernel/KernelRunResult.java](../../../fraynix-modules/factory-kernel/src/main/java/brain_architecture/kernel/KernelRunResult.java)
- [fraynix-modules/factory-kernel/src/main/java/brain_architecture/kernel/KernelStateStore.java](../../../fraynix-modules/factory-kernel/src/main/java/brain_architecture/kernel/KernelStateStore.java)
- [fraynix-modules/factory-kernel/src/main/java/brain_architecture/kernel/KernelVerifier.java](../../../fraynix-modules/factory-kernel/src/main/java/brain_architecture/kernel/KernelVerifier.java)
- [fraynix-modules/factory-kernel/src/main/java/brain_architecture/kernel/LocalModelDescriptor.java](../../../fraynix-modules/factory-kernel/src/main/java/brain_architecture/kernel/LocalModelDescriptor.java)
- [fraynix-modules/factory-kernel/src/main/java/brain_architecture/kernel/LocalModelRuntimeAdapter.java](../../../fraynix-modules/factory-kernel/src/main/java/brain_architecture/kernel/LocalModelRuntimeAdapter.java)
- [fraynix-modules/factory-kernel/src/main/java/brain_architecture/kernel/LocalModelRuntimeDescriptor.java](../../../fraynix-modules/factory-kernel/src/main/java/brain_architecture/kernel/LocalModelRuntimeDescriptor.java)
- [fraynix-modules/factory-kernel/src/main/java/brain_architecture/kernel/LocalModelSubstrateCatalog.java](../../../fraynix-modules/factory-kernel/src/main/java/brain_architecture/kernel/LocalModelSubstrateCatalog.java)
- [fraynix-modules/factory-kernel/src/main/java/brain_architecture/kernel/MemoryBenchmarkReport.java](../../../fraynix-modules/factory-kernel/src/main/java/brain_architecture/kernel/MemoryBenchmarkReport.java)
- [fraynix-modules/factory-kernel/src/main/java/brain_architecture/kernel/MemoryDocument.java](../../../fraynix-modules/factory-kernel/src/main/java/brain_architecture/kernel/MemoryDocument.java)
- [fraynix-modules/factory-kernel/src/main/java/brain_architecture/kernel/MemoryHit.java](../../../fraynix-modules/factory-kernel/src/main/java/brain_architecture/kernel/MemoryHit.java)
- [fraynix-modules/factory-kernel/src/main/java/brain_architecture/kernel/MerkleAppendOnlyJournal.java](../../../fraynix-modules/factory-kernel/src/main/java/brain_architecture/kernel/MerkleAppendOnlyJournal.java)
- [fraynix-modules/factory-kernel/src/main/java/brain_architecture/kernel/MutationCandidate.java](../../../fraynix-modules/factory-kernel/src/main/java/brain_architecture/kernel/MutationCandidate.java)
- [fraynix-modules/factory-kernel/src/main/java/brain_architecture/kernel/MutationOperation.java](../../../fraynix-modules/factory-kernel/src/main/java/brain_architecture/kernel/MutationOperation.java)
- [fraynix-modules/factory-kernel/src/main/java/brain_architecture/kernel/MutationProposal.java](../../../fraynix-modules/factory-kernel/src/main/java/brain_architecture/kernel/MutationProposal.java)
- [fraynix-modules/factory-kernel/src/main/java/brain_architecture/kernel/MutationProposalEngine.java](../../../fraynix-modules/factory-kernel/src/main/java/brain_architecture/kernel/MutationProposalEngine.java)
- [fraynix-modules/factory-kernel/src/main/java/brain_architecture/kernel/MutationProposalPreview.java](../../../fraynix-modules/factory-kernel/src/main/java/brain_architecture/kernel/MutationProposalPreview.java)
- [fraynix-modules/factory-kernel/src/main/java/brain_architecture/kernel/OfflineKernel.java](../../../fraynix-modules/factory-kernel/src/main/java/brain_architecture/kernel/OfflineKernel.java)
- [fraynix-modules/factory-kernel/src/main/java/brain_architecture/kernel/OfflineKernelBenchmark.java](../../../fraynix-modules/factory-kernel/src/main/java/brain_architecture/kernel/OfflineKernelBenchmark.java)
- [fraynix-modules/factory-kernel/src/main/java/brain_architecture/kernel/OfflineKernelCli.java](../../../fraynix-modules/factory-kernel/src/main/java/brain_architecture/kernel/OfflineKernelCli.java)
- [fraynix-modules/factory-kernel/src/main/java/brain_architecture/kernel/OfflineKernelExample.java](../../../fraynix-modules/factory-kernel/src/main/java/brain_architecture/kernel/OfflineKernelExample.java)
- [fraynix-modules/factory-kernel/src/main/java/brain_architecture/kernel/OfflineToolchainManifest.java](../../../fraynix-modules/factory-kernel/src/main/java/brain_architecture/kernel/OfflineToolchainManifest.java)
- [fraynix-modules/factory-kernel/src/main/java/brain_architecture/kernel/OptionalPdfTextExtractor.java](../../../fraynix-modules/factory-kernel/src/main/java/brain_architecture/kernel/OptionalPdfTextExtractor.java)
- [fraynix-modules/factory-kernel/src/main/java/brain_architecture/kernel/PdfDistillationChunk.java](../../../fraynix-modules/factory-kernel/src/main/java/brain_architecture/kernel/PdfDistillationChunk.java)
- [fraynix-modules/factory-kernel/src/main/java/brain_architecture/kernel/PdfDistillationEngine.java](../../../fraynix-modules/factory-kernel/src/main/java/brain_architecture/kernel/PdfDistillationEngine.java)
- [fraynix-modules/factory-kernel/src/main/java/brain_architecture/kernel/PdfDistillationReport.java](../../../fraynix-modules/factory-kernel/src/main/java/brain_architecture/kernel/PdfDistillationReport.java)
- [fraynix-modules/factory-kernel/src/main/java/brain_architecture/kernel/PlanActionHint.java](../../../fraynix-modules/factory-kernel/src/main/java/brain_architecture/kernel/PlanActionHint.java)
- [fraynix-modules/factory-kernel/src/main/java/brain_architecture/kernel/PlanStep.java](../../../fraynix-modules/factory-kernel/src/main/java/brain_architecture/kernel/PlanStep.java)
- [fraynix-modules/factory-kernel/src/main/java/brain_architecture/kernel/ProjectMemoryArtifactWriter.java](../../../fraynix-modules/factory-kernel/src/main/java/brain_architecture/kernel/ProjectMemoryArtifactWriter.java)
- [fraynix-modules/factory-kernel/src/main/java/brain_architecture/kernel/RepairMemoryStore.java](../../../fraynix-modules/factory-kernel/src/main/java/brain_architecture/kernel/RepairMemoryStore.java)
- [fraynix-modules/factory-kernel/src/main/java/brain_architecture/kernel/RetrievalProfile.java](../../../fraynix-modules/factory-kernel/src/main/java/brain_architecture/kernel/RetrievalProfile.java)
- [fraynix-modules/factory-kernel/src/main/java/brain_architecture/kernel/ShadowMutationEngine.java](../../../fraynix-modules/factory-kernel/src/main/java/brain_architecture/kernel/ShadowMutationEngine.java)
- [fraynix-modules/factory-kernel/src/main/java/brain_architecture/kernel/StrategyBanditRouter.java](../../../fraynix-modules/factory-kernel/src/main/java/brain_architecture/kernel/StrategyBanditRouter.java)
- [fraynix-modules/factory-kernel/src/main/java/brain_architecture/kernel/SubstrateAction.java](../../../fraynix-modules/factory-kernel/src/main/java/brain_architecture/kernel/SubstrateAction.java)
- [fraynix-modules/factory-kernel/src/main/java/brain_architecture/kernel/SubstrateAdapter.java](../../../fraynix-modules/factory-kernel/src/main/java/brain_architecture/kernel/SubstrateAdapter.java)
- [fraynix-modules/factory-kernel/src/main/java/brain_architecture/kernel/SubstrateResult.java](../../../fraynix-modules/factory-kernel/src/main/java/brain_architecture/kernel/SubstrateResult.java)
- [fraynix-modules/factory-kernel/src/main/java/brain_architecture/kernel/SubstrateSession.java](../../../fraynix-modules/factory-kernel/src/main/java/brain_architecture/kernel/SubstrateSession.java)
- [fraynix-modules/factory-kernel/src/main/java/brain_architecture/kernel/SubstrateSnapshot.java](../../../fraynix-modules/factory-kernel/src/main/java/brain_architecture/kernel/SubstrateSnapshot.java)
- [fraynix-modules/factory-kernel/src/main/java/brain_architecture/kernel/TaskBudget.java](../../../fraynix-modules/factory-kernel/src/main/java/brain_architecture/kernel/TaskBudget.java)
- [fraynix-modules/factory-kernel/src/main/java/brain_architecture/kernel/TaskEnvelope.java](../../../fraynix-modules/factory-kernel/src/main/java/brain_architecture/kernel/TaskEnvelope.java)
- [fraynix-modules/factory-kernel/src/main/java/brain_architecture/kernel/TaskMode.java](../../../fraynix-modules/factory-kernel/src/main/java/brain_architecture/kernel/TaskMode.java)
- [fraynix-modules/factory-kernel/src/main/java/brain_architecture/kernel/TaskPlanner.java](../../../fraynix-modules/factory-kernel/src/main/java/brain_architecture/kernel/TaskPlanner.java)
- [fraynix-modules/factory-kernel/src/main/java/brain_architecture/kernel/TokenizerDigest.java](../../../fraynix-modules/factory-kernel/src/main/java/brain_architecture/kernel/TokenizerDigest.java)
- [fraynix-modules/factory-kernel/src/main/java/brain_architecture/kernel/TraceReplayStore.java](../../../fraynix-modules/factory-kernel/src/main/java/brain_architecture/kernel/TraceReplayStore.java)
- [fraynix-modules/factory-kernel/src/main/java/brain_architecture/kernel/UserSpaceSubstrateAdapter.java](../../../fraynix-modules/factory-kernel/src/main/java/brain_architecture/kernel/UserSpaceSubstrateAdapter.java)
- [fraynix-modules/factory-kernel/src/main/java/brain_architecture/kernel/ValidationCommand.java](../../../fraynix-modules/factory-kernel/src/main/java/brain_architecture/kernel/ValidationCommand.java)
- [fraynix-modules/factory-kernel/src/main/java/brain_architecture/kernel/ValidationSuite.java](../../../fraynix-modules/factory-kernel/src/main/java/brain_architecture/kernel/ValidationSuite.java)
- [fraynix-modules/factory-kernel/src/main/java/brain_architecture/kernel/ValidationSuiteFactory.java](../../../fraynix-modules/factory-kernel/src/main/java/brain_architecture/kernel/ValidationSuiteFactory.java)
- [fraynix-modules/factory-kernel/src/main/java/brain_architecture/kernel/VerifierDiagnostic.java](../../../fraynix-modules/factory-kernel/src/main/java/brain_architecture/kernel/VerifierDiagnostic.java)
- [fraynix-modules/factory-kernel/src/main/java/brain_architecture/kernel/VerifierResult.java](../../../fraynix-modules/factory-kernel/src/main/java/brain_architecture/kernel/VerifierResult.java)
- [fraynix-modules/factory-kernel/src/main/java/brain_architecture/kernel/WebServer.java](../../../fraynix-modules/factory-kernel/src/main/java/brain_architecture/kernel/WebServer.java)
- [fraynix-modules/factory-kernel/src/main/java/brain_architecture/kernel/WorkspaceProfile.java](../../../fraynix-modules/factory-kernel/src/main/java/brain_architecture/kernel/WorkspaceProfile.java)
- [fraynix-modules/factory-kernel/src/main/java/fraymus/causal/CausalModel.java](../../../fraynix-modules/factory-kernel/src/main/java/fraymus/causal/CausalModel.java)
- [fraynix-modules/factory-kernel/src/main/java/fraymus/causal/DAGVerifier.java](../../../fraynix-modules/factory-kernel/src/main/java/fraymus/causal/DAGVerifier.java)
- [fraynix-modules/factory-kernel/src/main/java/fraymus/causal/DoCalculusEngine.java](../../../fraynix-modules/factory-kernel/src/main/java/fraymus/causal/DoCalculusEngine.java)
- [fraynix-modules/factory-kernel/src/main/java/fraymus/causal/SCMEnforcementEngine.java](../../../fraynix-modules/factory-kernel/src/main/java/fraymus/causal/SCMEnforcementEngine.java)
- [fraynix-modules/factory-kernel/src/main/java/fraymus/execution/ExecutionBridge.java](../../../fraynix-modules/factory-kernel/src/main/java/fraymus/execution/ExecutionBridge.java)
- [fraynix-modules/factory-kernel/src/main/java/fraymus/factory/kernel/ArtifactValidator.java](../../../fraynix-modules/factory-kernel/src/main/java/fraymus/factory/kernel/ArtifactValidator.java)
- [fraynix-modules/factory-kernel/src/main/java/fraymus/factory/kernel/FactoryDemo.java](../../../fraynix-modules/factory-kernel/src/main/java/fraymus/factory/kernel/FactoryDemo.java)
- [fraynix-modules/factory-kernel/src/main/java/fraymus/factory/kernel/FactoryKernelService.java](../../../fraynix-modules/factory-kernel/src/main/java/fraymus/factory/kernel/FactoryKernelService.java)
- [fraynix-modules/factory-kernel/src/main/java/fraymus/factory/kernel/LocalSkillDispatcher.java](../../../fraynix-modules/factory-kernel/src/main/java/fraymus/factory/kernel/LocalSkillDispatcher.java)
- [fraynix-modules/factory-kernel/src/main/java/fraymus/factory/kernel/SkillSchema.java](../../../fraynix-modules/factory-kernel/src/main/java/fraymus/factory/kernel/SkillSchema.java)
- [fraynix-modules/factory-kernel/src/main/java/fraymus/factory/kernel/StudioSkillCli.java](../../../fraynix-modules/factory-kernel/src/main/java/fraymus/factory/kernel/StudioSkillCli.java)
- [fraynix-modules/factory-kernel/src/main/java/fraymus/hdc/SharedHypervectorCore.java](../../../fraynix-modules/factory-kernel/src/main/java/fraymus/hdc/SharedHypervectorCore.java)
- [fraynix-modules/factory-kernel/src/main/java/fraymus/llm/ModelLoader.java](../../../fraynix-modules/factory-kernel/src/main/java/fraymus/llm/ModelLoader.java)
- [fraynix-modules/factory-kernel/src/test/java/fraymus/factory/kernel/FactoryKernelIntegrationTest.java](../../../fraynix-modules/factory-kernel/src/test/java/fraymus/factory/kernel/FactoryKernelIntegrationTest.java)
- [fraynix-modules/factory-kernel/src/test/java/fraymus/factory/kernel/JournalContinuityTest.java](../../../fraynix-modules/factory-kernel/src/test/java/fraymus/factory/kernel/JournalContinuityTest.java)
- [fraynix-modules/factory-kernel/src/test/java/fraymus/factory/kernel/LocalSkillsTest.java](../../../fraynix-modules/factory-kernel/src/test/java/fraymus/factory/kernel/LocalSkillsTest.java)
- [fraynix-modules/factory-models/build.gradle](../../../fraynix-modules/factory-models/build.gradle)
- [fraynix-modules/factory-models/src/main/java/fraymus/OllamaIntegration.java](../../../fraynix-modules/factory-models/src/main/java/fraymus/OllamaIntegration.java)
- [fraynix-modules/factory-models/src/main/java/fraymus/factory/models/ChatContext.java](../../../fraynix-modules/factory-models/src/main/java/fraymus/factory/models/ChatContext.java)
- [fraynix-modules/factory-models/src/main/java/fraymus/factory/models/ConversationStore.java](../../../fraynix-modules/factory-models/src/main/java/fraymus/factory/models/ConversationStore.java)
- [fraynix-modules/factory-models/src/main/java/fraymus/factory/models/FactoryModelService.java](../../../fraynix-modules/factory-models/src/main/java/fraymus/factory/models/FactoryModelService.java)
- [fraynix-modules/factory-models/src/main/java/fraymus/factory/models/ModelProfileStore.java](../../../fraynix-modules/factory-models/src/main/java/fraymus/factory/models/ModelProfileStore.java)
- [fraynix-modules/factory-models/src/main/java/fraymus/factory/models/ModelRunSettings.java](../../../fraynix-modules/factory-models/src/main/java/fraymus/factory/models/ModelRunSettings.java)
- [fraynix-modules/factory-models/src/main/java/fraymus/llm/CapabilityProfile.java](../../../fraynix-modules/factory-models/src/main/java/fraymus/llm/CapabilityProfile.java)
- [fraynix-modules/factory-models/src/main/java/fraymus/llm/LocalModelCatalog.java](../../../fraynix-modules/factory-models/src/main/java/fraymus/llm/LocalModelCatalog.java)
- [fraynix-modules/factory-models/src/main/java/fraymus/llm/ModelAbstractionLayer.java](../../../fraynix-modules/factory-models/src/main/java/fraymus/llm/ModelAbstractionLayer.java)
- [fraynix-modules/factory-models/src/main/java/fraymus/llm/ModelBackend.java](../../../fraynix-modules/factory-models/src/main/java/fraymus/llm/ModelBackend.java)
- [fraynix-modules/factory-models/src/main/java/fraymus/llm/ModelDescriptor.java](../../../fraynix-modules/factory-models/src/main/java/fraymus/llm/ModelDescriptor.java)
- [fraynix-modules/factory-models/src/main/java/fraymus/llm/ModelRole.java](../../../fraynix-modules/factory-models/src/main/java/fraymus/llm/ModelRole.java)
- [fraynix-modules/factory-models/src/main/java/fraymus/llm/ModelSelectionPolicy.java](../../../fraynix-modules/factory-models/src/main/java/fraymus/llm/ModelSelectionPolicy.java)
- [fraynix-modules/factory-models/src/main/java/fraymus/llm/ModelSelectionResult.java](../../../fraynix-modules/factory-models/src/main/java/fraymus/llm/ModelSelectionResult.java)
- [fraynix-modules/factory-models/src/main/java/fraymus/llm/OpenModelRegistry.java](../../../fraynix-modules/factory-models/src/main/java/fraymus/llm/OpenModelRegistry.java)
- [fraynix-modules/factory-models/src/main/java/fraymus/model/CapabilityProfile.java](../../../fraynix-modules/factory-models/src/main/java/fraymus/model/CapabilityProfile.java)
- [fraynix-modules/factory-models/src/main/java/fraymus/model/LocalOllamaModelAdapter.java](../../../fraynix-modules/factory-models/src/main/java/fraymus/model/LocalOllamaModelAdapter.java)
- [fraynix-modules/factory-models/src/main/java/fraymus/model/ModelAdapter.java](../../../fraynix-modules/factory-models/src/main/java/fraymus/model/ModelAdapter.java)
- [fraynix-modules/factory-models/src/main/java/fraymus/model/ModelDescriptor.java](../../../fraynix-modules/factory-models/src/main/java/fraymus/model/ModelDescriptor.java)
- [fraynix-modules/factory-models/src/test/java/fraymus/factory/models/FactoryModelsTest.java](../../../fraynix-modules/factory-models/src/test/java/fraymus/factory/models/FactoryModelsTest.java)
- [fraynix-modules/factory-runtime/build.gradle](../../../fraynix-modules/factory-runtime/build.gradle)
- [fraynix-modules/factory-runtime/src/main/java/fraymus/factory/ComponentRecord.java](../../../fraynix-modules/factory-runtime/src/main/java/fraymus/factory/ComponentRecord.java)
- [fraynix-modules/factory-runtime/src/main/java/fraymus/factory/ComponentSchema.java](../../../fraynix-modules/factory-runtime/src/main/java/fraymus/factory/ComponentSchema.java)
- [fraynix-modules/factory-runtime/src/main/java/fraymus/factory/EntityRecord.java](../../../fraynix-modules/factory-runtime/src/main/java/fraymus/factory/EntityRecord.java)
- [fraynix-modules/factory-runtime/src/main/java/fraymus/factory/FactoryWorld.java](../../../fraynix-modules/factory-runtime/src/main/java/fraymus/factory/FactoryWorld.java)
- [fraynix-modules/factory-runtime/src/main/java/fraymus/factory/SchemaRegistry.java](../../../fraynix-modules/factory-runtime/src/main/java/fraymus/factory/SchemaRegistry.java)
- [fraynix-modules/factory-runtime/src/main/java/fraymus/factory/SpatialIndex2D.java](../../../fraynix-modules/factory-runtime/src/main/java/fraymus/factory/SpatialIndex2D.java)
- [fraynix-modules/factory-runtime/src/main/java/fraymus/factory/WorldCheckpointStore.java](../../../fraynix-modules/factory-runtime/src/main/java/fraymus/factory/WorldCheckpointStore.java)
- [fraynix-modules/factory-runtime/src/main/java/fraymus/factory/WorldSnapshot.java](../../../fraynix-modules/factory-runtime/src/main/java/fraymus/factory/WorldSnapshot.java)
- [fraynix-modules/factory-runtime/src/main/java/fraymus/factory/WorldTransaction.java](../../../fraynix-modules/factory-runtime/src/main/java/fraymus/factory/WorldTransaction.java)
- [fraynix-modules/factory-runtime/src/test/java/fraymus/factory/FactoryRuntimeTest.java](../../../fraynix-modules/factory-runtime/src/test/java/fraymus/factory/FactoryRuntimeTest.java)
- [fraynix-modules/factory-runtime/src/test/java/fraymus/factory/WorldTransactionTest.java](../../../fraynix-modules/factory-runtime/src/test/java/fraymus/factory/WorldTransactionTest.java)
- [gradle.lockfile](../../../gradle.lockfile)
- [gradle/wrapper/gradle-wrapper.jar](../../../gradle/wrapper/gradle-wrapper.jar)
- [gradle/wrapper/gradle-wrapper.properties](../../../gradle/wrapper/gradle-wrapper.properties)
- [gradlew](../../../gradlew)
- [gradlew.bat](../../../gradlew.bat)
- [level.txt](../../../level.txt)
- [libs/jbox2d-library.jar](../../../libs/jbox2d-library.jar)
- [models/FRAYNIX-DREAMER-120B.Modelfile](../../../models/FRAYNIX-DREAMER-120B.Modelfile)
- [models/FRAYNIX-DREAMER-120B.candidate.Modelfile](../../../models/FRAYNIX-DREAMER-120B.candidate.Modelfile)
- [offline-toolchain.toml](../../../offline-toolchain.toml)
- [scripts/Configure-Models.ps1](../../../scripts/Configure-Models.ps1)
- [scripts/Run-Factory-Test.ps1](../../../scripts/Run-Factory-Test.ps1)
- [scripts/Run-Kernel.ps1](../../../scripts/Run-Kernel.ps1)
- [scripts/Run-Studio.ps1](../../../scripts/Run-Studio.ps1)
- [scripts/Start-FDrive-Ollama.ps1](../../../scripts/Start-FDrive-Ollama.ps1)
- [scripts/Verify-Documentation.ps1](../../../scripts/Verify-Documentation.ps1)
- [scripts/Verify-Native-Factory.ps1](../../../scripts/Verify-Native-Factory.ps1)
- [scripts/Verify-Native-SceneAgent.ps1](../../../scripts/Verify-Native-SceneAgent.ps1)
- [scripts/Verify-Native-Studio.ps1](../../../scripts/Verify-Native-Studio.ps1)
- [scripts/Verify-Studio.ps1](../../../scripts/Verify-Studio.ps1)
- [settings.gradle](../../../settings.gradle)
- [src/main/java/Main.java](../../../src/main/java/Main.java)
- [src/main/java/components/Component.java](../../../src/main/java/components/Component.java)
- [src/main/java/components/ComponentDeserializer.java](../../../src/main/java/components/ComponentDeserializer.java)
- [src/main/java/components/EditorCamera.java](../../../src/main/java/components/EditorCamera.java)
- [src/main/java/components/FontRenderer.java](../../../src/main/java/components/FontRenderer.java)
- [src/main/java/components/Gizmo.java](../../../src/main/java/components/Gizmo.java)
- [src/main/java/components/GizmoSystem.java](../../../src/main/java/components/GizmoSystem.java)
- [src/main/java/components/GridLines.java](../../../src/main/java/components/GridLines.java)
- [src/main/java/components/MouseControls.java](../../../src/main/java/components/MouseControls.java)
- [src/main/java/components/NonPickable.java](../../../src/main/java/components/NonPickable.java)
- [src/main/java/components/ScaleGizmo.java](../../../src/main/java/components/ScaleGizmo.java)
- [src/main/java/components/Sprite.java](../../../src/main/java/components/Sprite.java)
- [src/main/java/components/SpriteRenderer.java](../../../src/main/java/components/SpriteRenderer.java)
- [src/main/java/components/Spritesheet.java](../../../src/main/java/components/Spritesheet.java)
- [src/main/java/components/TranslateGizmo.java](../../../src/main/java/components/TranslateGizmo.java)
- [src/main/java/editor/FraynixPanel.java](../../../src/main/java/editor/FraynixPanel.java)
- [src/main/java/editor/FraynixWorkspace.java](../../../src/main/java/editor/FraynixWorkspace.java)
- [src/main/java/editor/GameViewWindow.java](../../../src/main/java/editor/GameViewWindow.java)
- [src/main/java/editor/JImGui.java](../../../src/main/java/editor/JImGui.java)
- [src/main/java/editor/MenuBar.java](../../../src/main/java/editor/MenuBar.java)
- [src/main/java/editor/PhysicsComponents.java](../../../src/main/java/editor/PhysicsComponents.java)
- [src/main/java/editor/PropertiesWindow.java](../../../src/main/java/editor/PropertiesWindow.java)
- [src/main/java/editor/SceneAgent.java](../../../src/main/java/editor/SceneAgent.java)
- [src/main/java/editor/SceneAgentNativeSmoke.java](../../../src/main/java/editor/SceneAgentNativeSmoke.java)
- [src/main/java/editor/SceneAgentPanel.java](../../../src/main/java/editor/SceneAgentPanel.java)
- [src/main/java/editor/ScenePanel.java](../../../src/main/java/editor/ScenePanel.java)
- [src/main/java/editor/StudioFrameCapture.java](../../../src/main/java/editor/StudioFrameCapture.java)
- [src/main/java/editor/StudioLayout.java](../../../src/main/java/editor/StudioLayout.java)
- [src/main/java/editor/ViewportCoordinates.java](../../../src/main/java/editor/ViewportCoordinates.java)
- [src/main/java/factory/FactorySceneAdapter.java](../../../src/main/java/factory/FactorySceneAdapter.java)
- [src/main/java/factory/FactorySession.java](../../../src/main/java/factory/FactorySession.java)
- [src/main/java/factory/SceneEditHistory.java](../../../src/main/java/factory/SceneEditHistory.java)
- [src/main/java/factory/SceneJson.java](../../../src/main/java/factory/SceneJson.java)
- [src/main/java/factory/SceneKnowledge.java](../../../src/main/java/factory/SceneKnowledge.java)
- [src/main/java/factory/SceneProposal.java](../../../src/main/java/factory/SceneProposal.java)
- [src/main/java/factory/SceneSimulation.java](../../../src/main/java/factory/SceneSimulation.java)
- [src/main/java/jade/Camera.java](../../../src/main/java/jade/Camera.java)
- [src/main/java/jade/GameObject.java](../../../src/main/java/jade/GameObject.java)
- [src/main/java/jade/GameObjectDeserializer.java](../../../src/main/java/jade/GameObjectDeserializer.java)
- [src/main/java/jade/ImGuiLayer.java](../../../src/main/java/jade/ImGuiLayer.java)
- [src/main/java/jade/KeyListener.java](../../../src/main/java/jade/KeyListener.java)
- [src/main/java/jade/MouseListener.java](../../../src/main/java/jade/MouseListener.java)
- [src/main/java/jade/Prefabs.java](../../../src/main/java/jade/Prefabs.java)
- [src/main/java/jade/Transform.java](../../../src/main/java/jade/Transform.java)
- [src/main/java/jade/Window.java](../../../src/main/java/jade/Window.java)
- [src/main/java/observers/EventSystem.java](../../../src/main/java/observers/EventSystem.java)
- [src/main/java/observers/Observer.java](../../../src/main/java/observers/Observer.java)
- [src/main/java/observers/events/Event.java](../../../src/main/java/observers/events/Event.java)
- [src/main/java/observers/events/EventType.java](../../../src/main/java/observers/events/EventType.java)
- [src/main/java/physics2d/Physics2D.java](../../../src/main/java/physics2d/Physics2D.java)
- [src/main/java/physics2d/components/Box2DCollider.java](../../../src/main/java/physics2d/components/Box2DCollider.java)
- [src/main/java/physics2d/components/CircleCollider.java](../../../src/main/java/physics2d/components/CircleCollider.java)
- [src/main/java/physics2d/components/Collider.java](../../../src/main/java/physics2d/components/Collider.java)
- [src/main/java/physics2d/components/Rigidbody2D.java](../../../src/main/java/physics2d/components/Rigidbody2D.java)
- [src/main/java/physics2d/enums/BodyType.java](../../../src/main/java/physics2d/enums/BodyType.java)
- [src/main/java/physics2dtmp/PhysicsSystem2D.java](../../../src/main/java/physics2dtmp/PhysicsSystem2D.java)
- [src/main/java/physics2dtmp/forces/ForceGenerator.java](../../../src/main/java/physics2dtmp/forces/ForceGenerator.java)
- [src/main/java/physics2dtmp/forces/ForceRegistration.java](../../../src/main/java/physics2dtmp/forces/ForceRegistration.java)
- [src/main/java/physics2dtmp/forces/ForceRegistry.java](../../../src/main/java/physics2dtmp/forces/ForceRegistry.java)
- [src/main/java/physics2dtmp/forces/Gravity2D.java](../../../src/main/java/physics2dtmp/forces/Gravity2D.java)
- [src/main/java/physics2dtmp/primitives/AABB.java](../../../src/main/java/physics2dtmp/primitives/AABB.java)
- [src/main/java/physics2dtmp/primitives/Box2D.java](../../../src/main/java/physics2dtmp/primitives/Box2D.java)
- [src/main/java/physics2dtmp/primitives/Circle.java](../../../src/main/java/physics2dtmp/primitives/Circle.java)
- [src/main/java/physics2dtmp/primitives/Collider2D.java](../../../src/main/java/physics2dtmp/primitives/Collider2D.java)
- [src/main/java/physics2dtmp/primitives/Ray2D.java](../../../src/main/java/physics2dtmp/primitives/Ray2D.java)
- [src/main/java/physics2dtmp/primitives/RaycastResult.java](../../../src/main/java/physics2dtmp/primitives/RaycastResult.java)
- [src/main/java/physics2dtmp/rigidbody/CollisionManifold.java](../../../src/main/java/physics2dtmp/rigidbody/CollisionManifold.java)
- [src/main/java/physics2dtmp/rigidbody/Collisions.java](../../../src/main/java/physics2dtmp/rigidbody/Collisions.java)
- [src/main/java/physics2dtmp/rigidbody/IntersectionDetector2D.java](../../../src/main/java/physics2dtmp/rigidbody/IntersectionDetector2D.java)
- [src/main/java/physics2dtmp/rigidbody/Rigidbody2D.java](../../../src/main/java/physics2dtmp/rigidbody/Rigidbody2D.java)
- [src/main/java/renderer/DebugDraw.java](../../../src/main/java/renderer/DebugDraw.java)
- [src/main/java/renderer/Framebuffer.java](../../../src/main/java/renderer/Framebuffer.java)
- [src/main/java/renderer/Line2D.java](../../../src/main/java/renderer/Line2D.java)
- [src/main/java/renderer/PickingTexture.java](../../../src/main/java/renderer/PickingTexture.java)
- [src/main/java/renderer/RenderBatch.java](../../../src/main/java/renderer/RenderBatch.java)
- [src/main/java/renderer/Renderer.java](../../../src/main/java/renderer/Renderer.java)
- [src/main/java/renderer/Shader.java](../../../src/main/java/renderer/Shader.java)
- [src/main/java/renderer/Texture.java](../../../src/main/java/renderer/Texture.java)
- [src/main/java/scenes/LevelEditorSceneInitializer.java](../../../src/main/java/scenes/LevelEditorSceneInitializer.java)
- [src/main/java/scenes/Scene.java](../../../src/main/java/scenes/Scene.java)
- [src/main/java/scenes/SceneInitializer.java](../../../src/main/java/scenes/SceneInitializer.java)
- [src/main/java/util/AssetPool.java](../../../src/main/java/util/AssetPool.java)
- [src/main/java/util/GridPlacement.java](../../../src/main/java/util/GridPlacement.java)
- [src/main/java/util/JMath.java](../../../src/main/java/util/JMath.java)
- [src/main/java/util/Settings.java](../../../src/main/java/util/Settings.java)
- [src/main/resources/META-INF/MANIFEST.MF](../../../src/main/resources/META-INF/MANIFEST.MF)
- [src/test/java/editor/FraynixWorkspaceTest.java](../../../src/test/java/editor/FraynixWorkspaceTest.java)
- [src/test/java/factory/EditorPhysicsRegressionTest.java](../../../src/test/java/factory/EditorPhysicsRegressionTest.java)
- [src/test/java/factory/FactorySceneIntegrationTest.java](../../../src/test/java/factory/FactorySceneIntegrationTest.java)
- [src/test/java/factory/GridPlacementRegressionTest.java](../../../src/test/java/factory/GridPlacementRegressionTest.java)
- [src/test/java/factory/SceneAgentIntegrationTest.java](../../../src/test/java/factory/SceneAgentIntegrationTest.java)
- [src/test/java/physics2dtmp/CollisionDetectorTests.java](../../../src/test/java/physics2dtmp/CollisionDetectorTests.java)
