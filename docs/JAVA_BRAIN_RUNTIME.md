# Shared Java brain and integrated runtime contracts

Edition018 adds a learned numerical world model, separate uncertainty calibration/native evaluation, paused counterfactual previews, receding-horizon action/observation checks and recorded-motion forecasting. Native limits pass; the previous feedback controller still scores better on tracking, and the recorded-reality gate remains rejected. See [current world/reality contract](WORLD_MODEL_REALITY.md) and [README018](../README-018.md). Broader real calibration and multi-object/contact models remain next.

Edition 017 adds live bounded native feedback, gain selection from actual training outcomes, predeclared stronger-baseline evaluation, real disturbances, intent/receipt history, stop/replay/restart and separate feedback rollback. Job completion and activation now commit atomically. See [current feedback contract](CLOSED_LOOP_CONTROL.md) and [README 017](../README-017.md). Broader geometry/sensing, closed-loop manipulation/navigation and Java task graphs remain next.

Edition 016 adds a working Java-first slice while preserving the existing desktop layout and the separate original Studio. [README 016](../README-016.md) contains actual images, measured learning/GPU/latency results and the next implementation gates. [Offline explorer](readmes/016/index.html), [source inventory](readmes/016/file-inventory.json) and [Obsidian map](obsidian/java-brain-runtime.canvas) expose the concrete modules.

## Ownership

| Owner | Actual responsibility |
|---|---|
| `brain-core` | Dependency-free Java spiking computation, CPU matrix-vector reference, gradient-descent readout, binary neural checkpoint and window-independent runtime interface. Legacy Java callers retain a compatibility facade and the original state fields. |
| `brain-gpu` | Actual GLSL 430 compute shader, worker-owned hidden OpenGL context, CPU/GPU parity and transfer-inclusive measurements. Requires native bindings/driver; not a full GPU spiking cortex. |
| Java `BrainService` | One durable brain job at a time, native training/evaluation trials, cancellation, startup interruption handling, accepted physical policy versions, activation fences and rollback. |
| Java `SceneStore` | Live native world, actual physical impulse, fixed ticks, endpoint result and saved-command replay. Authoritative state is never supplied by the view. |
| Java `EmbeddedGameClient` | Child JVM lifecycle, private capability and original game runtime transport, independent project subdirectory and actual renderer frames. |
| Original game `EmbeddedGameRuntime` | Real scene/player observations, fixed-step input and periodic offscreen frames, without initializing the legacy editor. Hidden native context remains necessary for this renderer. |
| Existing Java kernel | Persistent hybrid HDC/lexical/graph retrieval, artifact ingestion and Merkle journal. New immutable notes and bounded automatic model context use this existing store. |
| Electron main/renderer | Existing model-agent loop, browser/canvas hosting and input; thin presentation for Java Game/Brain APIs. Imported source text receives no dispatcher privileges. |

The visible application is integrated. Process separation isolates the legacy graphics lifecycle; native window reparenting is not used. The current adapter favors original renderer compatibility, with measured costs recorded. A future shared-state renderer needs its own compatibility and complete-latency comparison before replacing it.

## Private operations

All operations use the existing loopback control plane and private session capability. Browser/preview renderers never receive that capability. The privileged desktop bridge accepts explicit command names only.

| API | Input / effect |
|---|---|
| `/api/brain/state` | Compact persisted jobs, active policy, cortex status and verified journal entry count. |
| `/api/brain/start` | `kind`: `pulse`, `gpu-check` or `train`; optional `autoPromote`. Rejects overlap, unsupported fields and exhausted history. |
| `/api/brain/read`, `/cancel` | Exact job `id`. Cancellation is checked between native ticks/workloads; partial candidates are never activated. |
| `/api/brain/rollback` | `expectedVersion`; restore prior weights as a new policy version, preserving history. |
| `/api/brain/run` | Saved scene `id`, `expectedVersion`, `targetM`, optional `gpu`. Actual native impulse and 180 physics ticks; previous experiment must be reset. |
| `/api/game/open`, `/state`, `/frame`, `/close` | Start/read/stop only the owned hidden original-game child. Frame contains real PNG, digest, source session, tick, player and capture/bridge timings. |
| `/api/game/command` | `step` (1–120 ticks, horizontal −1..1, jump), `play`, `pause`, bounded `ai` target, `reset`, `close`. No arbitrary script/file command. |
| `/api/memory/remember` | Exactly `text`, 1–4,000 characters; immutable hash-named UTF-8 note, ingestion and journal receipt. Maximum 128 notes. |
| `/api/memory/context` | Query up to 8,000 characters; text budget 256–8,000. Sources, scores, untrusted-data labels and journal head/root. |

Before each actual model planning call, Java retrieves against the user's task with a 2,400-character text budget and inserts source-labelled data before the current host rules. Finite model context is still managed explicitly. Model admission/digest checks, typed tools and existing completion gates remain authoritative.

## Physical readout protocol

`native-push-readout-v1` trains a four-weight linear output using declared features `[1, target/2, gravity*sin(angle)/10, gravity*friction*cos(angle)/10]`. The output is multiplied by four and clipped to ±4 m/s; native impulse is `mass × output` downhill along the ramp. This is real gradient descent over real native-outcome labels, with engineered features. It is neither language-model training nor an autonomous general controller.

Twelve training conditions each evaluate thirteen native candidates (−2..4 m/s, step 0.5); training-only endpoint error selects the label. Twelve fitted training trials and twelve validation baseline/candidate trials bring the total to 180. All simulate 0.75 seconds at 240 Hz. Six separate validation conditions are frozen before selection. Acceptance requires at least 30% mean endpoint-error improvement against zero impulse and maximum error at most 0.25 m. Gate results, actual trajectories and weights are retained in the job. Rejected/cancelled jobs leave the active policy unchanged.

The accepted domain is the default 12×3 m ramp at Z=0, slope 10–40°, friction 0–0.3, mass 0.5–2 kg, gravity 8–12 m/s² and target 0.4–1.8 m. Out-of-domain geometry/parameters reject before applying motion. A completed run may still miss its target; completion and goal acceptance are distinct. Since the controller intervenes, the frictionless-from-rest formula is not treated as applicable. Native replay reuses the saved impulse, policy/source receipt and scene rather than the current policy.

The current validation set is a development suite. Stronger baselines, a locked external evaluation, disturbances and closed-loop sensing/action remain required before expanding capability claims.

## Persistence and recovery

Project-private `brain/tasks.fworld` stores jobs and policies using the existing atomic checkpoint/transaction machinery. Phase records include saved-payload hashes in the local Merkle journal. Queued/running records found at startup become interrupted; the application does not pretend native trials resumed or completed. History is currently bounded to 16 jobs and fails explicitly at capacity; archive/retention UI remains a future improvement.

`brain/cortex.bin` stores exact neural state, random state and routing with a SHA-256 prefix, size/dimension checks and atomic replacement. Exact continuation and corrupt-byte rejection are tested. Physical policy records are separate from the spiking cortex: a pulse can show plastic computation without establishing useful physical learning. The accepted readout's independent physical evaluation establishes the small task improvement.

The owned game child uses `<project>/embedded-game`, distinct from the optional legacy editor's `<project>/game-lab`. It does not use the original Studio data. Normal shell close requests runtime close; if the parent dies, the child detects it and exits. Original game source/rendering remains in this checkout. No personal runtime state or model weights enter Git.

## Current evidence and next gates

[Release verification](readmes/016/verification.json) separates Java tests, visible controls, real local-model memory retrieval, fresh-checkout validation and documentation checks. Unsettled-checkpoint recovery uses an explicit fixture; restart/rollback/memory and child-process cleanup use real desktop processes. GPU timing covers transfers and the worker boundary; it is not a shader-only benchmark or speedup claim.

Next: compare stronger control baselines on a fresh external set; implement closed-loop physical navigation/manipulation; align original-game goals and failures with actual ticks; migrate durable tool/task graph ownership to Java; benchmark batched GPU workloads; add broader import/compiler/MCP adapters through tested existing interfaces. Source documents remain data and cannot register executable tools or update trusted rules automatically.
