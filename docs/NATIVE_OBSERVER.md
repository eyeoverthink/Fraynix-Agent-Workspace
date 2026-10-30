# Native observer contract

Edition 015 now adds [real native body control](NATIVE_BODY_CONTROL.md) and [scene/media interchange](SCENE_INTERCHANGE.md) on top of this observer. The observer itself remains a free camera; body actuation uses a separate owned native constraint.

Implemented in edition 014. The native world, camera and visibility do not require a transformer. Java owns the authoritative state; the existing renderer supplies pixels. This contract is deliberately a free observer over the existing box-based ramp lab, not a physical avatar.

## Source and identity

SceneStore.observerWorld returns a copied snapshot with stable native object identities, definition version, current authored version, source kind, run ID, tick, time, scope and units. When an older experiment is replayed, its original definition version is preserved. ObserverStore stores identity, scene ID, pose, selected entity, action count and capture count in a separate FactoryWorld checkpoint under the selected project.

ObserverGeometry implements camera basis, frustum checks, inverse-quaternion box ray intersection and nearest-hit occlusion. Visibility samples a 9×5 ray grid plus box center/corners. It is approximate known-box visibility, not learned image recognition or arbitrary mesh segmentation.

## Operations

| API suffix under /api/observer/ | Input | Real effect |
|---|---|---|
| open | sceneId; optional id or newObserver | Restore a matching observer/latest for scene, or create bounded identity |
| list | none | List persistent observer identities |
| read | id | Read current pose/selection/version |
| observe | id; optional width/height | New expiring immutable observation and source fingerprint |
| action | id, observationId, operationId, action, declared action fields | Validate lease, execute camera/selection change and checkpoint receipt atomically |
| capture | observationId, operationId, worldSha256, pngBase64 | Trusted host submits actual renderer PNG, validated and content addressed |
| history | id | Read ordered durable actions and capture summaries |
| replay | capture id | Verify bytes and return read-only original image/source packet |

Agent tools expose open/observe/move/look/focus/inspect/fov/capture/history/replay. Agent open accepts only sceneId; the host returns the observer identity. The manual UI/API additionally supports explicit observer creation/restoration. Agent capture accepts only an observer ID: models cannot supply PNG bytes or a fabricated source packet.

Movement is yaw-relative forward/right plus world-up metres. Looking changes degrees; focus uses the actual entity center. Inspection requires sampled visibility and exact current source. All actions reject unrelated fields, unknown targets, invalid numbers, zero move/look, stale pose or authored-scene binding. Reused operation IDs return the previous receipt only for identical input. Action validation/commit holds the scene lock to prevent an authored edit interleaving with acceptance.

## Bounds and storage

The project permits 32 observers; each has 500 actions and 100 captures. Leases expire after 120 s and the ephemeral cache holds 128. Move length ≤5 m; yaw delta ±90°, pitch delta ±45°; x/z ±100 m, y 0.05–50 m; pitch ±85°, vertical FOV 30–110°. Viewport 320–1280 by 240–960, PNG ≤1.2 MB. The free camera has no avatar collisions.

Private paths are observers/state.fworld and observers/frames/SHA256.png beneath the selected project. Receipt/source metadata checkpoint together; a failed metadata write is not a successful capture. A leftover content-addressed file can be reused only after integrity verification. Replay never creates a live lease or alters the current scene/pose. New observations are required after restart.

The first-party renderer freezes the packet, uses pixel ratio one and exact dimensions, draws actual native entity poses and returns a PNG. PNG decoding and hashes validate bytes and source binding; independent image tests additionally verify the visible block shifts in the expected direction. This is a trusted-host rendering contract, not cryptographic proof of render semantics.

## Verification and remaining work

See [README 014](../README-014.md), [Java tests](../workspace-engine/src/test/java/fraymus/workspace/ObserverStoreTest.java), [actual UI tests](../workspace-desktop/test/observer-integration.cjs) and [local-model fixture](../workspace-desktop/test/live-observer.cjs). New source files implement the workflow; no inert page or placeholder handler is exposed.

The bounded native actuator is delivered in edition 015. Articulated grasping/navigation still needs body/collision/reachability checks and stuck recovery. Physical experience learning needs tick/entity-aligned outcomes, separate evaluation and rollback. Capturing history alone does not train weights or establish improvement.
