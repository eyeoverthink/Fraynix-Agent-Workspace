# Native 3D mechanics and experiment evidence

Edition 010 · October 29, 2026

Java SceneStore owns authored scene revisions, stable object3d entities, copied experiment definitions and results. Native Bullet through pinned Minie integrates the world at 1/240-second ticks. Three.js renders its actual poses in the existing center view. Play/pause/step/reset, saved parameters, reference checks, history and old-definition replay work.

Eight new Java test methods and twelve actual native UI checks pass; existing regressions remain verified. Local gpt-oss:20b creates a requested scene, runs 180 native ticks and reports measured/reference displacement. Physical task episodes remain completed-unverified under the existing artifact receipt contract; the run has a separate reference evaluation. No physical policy or model weights are trained.

- [[README-010|Implementation and review steps]]
- [[docs/3D_BACKEND_DECISION|Adapter and dependency decision]]
- [[docs/SCENARIO_ENGINE_ROADMAP|Updated phases and remaining work]]
- [[docs/obsidian/native-mechanics.canvas|Native mechanics architecture]]
- [[docs/PROJECT_MEMORY|Project continuity]]
- [[docs/DEVELOPMENT_LOG|Development log]]

## Confirmed release and measured result

Frozen `readme-010` points to `5eeed1e`; main contains that release and may carry later continuity updates. GitHub references and a separately fetched checkout were verified. The actual local-model incline case records 1.387197904 m displacement versus ideal 1.379531250 m at 0.75 s, within the declared tolerance. See [[docs/readmes/010/live-scene.json|the physical task evidence]].

The release passes 105 Java tests, nine desktop tests, twelve native UI checks and the original/browser/learning regressions. Its inventory has 382 implementation source/assets. The three README images are actual app frames. Next: versioned measurement import and independent residual comparison; no physical-policy training is claimed by this release.
