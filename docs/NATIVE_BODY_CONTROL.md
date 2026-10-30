# Native body control

Edition 015 adds a real point-to-point native Bullet constraint over the existing dynamic Block. The body remains dynamic. Only the constraint anchor is commanded; body position is produced by native integration, gravity and contacts.

## Contract

The operator selects a visible entity from a live observer. Grab requires a fresh native source, no active experiment, a dynamic Block, distance ≤6 m and mass ≤10 kg. The virtual actuator is not a colliding avatar, articulated hand or real robot. Static ramp/ground bodies cannot be grabbed.

The constraint impulse clamp is 1.25 N·s per axis at dt=1/240 s, equivalent to 300 N per axis. This is not a global contact-force/vector-force guarantee. Anchor speed is at most 1.5 m/s. One move is 0.01–2 m from both body and previous anchor, followed by 48 settling ticks. World target x/z stay within ±20 m and y within 0.26–20 m. Target reach remains ≤6 m. The actual endpoint passes only within 0.08 m; otherwise the receipt says blocked-or-force-limited.

Each BulletRamp owns a dedicated worker. Its native space is created, queried, stepped and destroyed on that thread. SceneStore serializes world operations and schedules four fixed ticks per clock interval while a body plan is active. The UI polls genuine intermediate snapshots. A run has at most 24 successful actuator events and 14,400 ticks. It pauses between commands.

Release removes the joint without clearing velocity, then advances 120 native ticks. Cancel removes the joint and settles the run as cancelled, with no continued simulation. Reset explicitly returns to authored state. Authored scene geometry/revision does not change during manipulation. The ideal frictionless incline reference is not applicable to an intervened manipulation run.

## Interfaces and authority

The private /api/body/action accepts action, observer id, operationId and fresh observationId. Move/release also require actual runId/gripId; move takes a three-number world target. Grab takes a native visible entityId. Cancel requires owned run identity but may interrupt an advancing plan without a stale observation. Unknown fields, wrong owner/grip, stale source and invalid values fail before motion.

Manual controls use these full fences. The language-model body tools are task scoped: grab takes entityId, move takes target, release/cancel/capture take no arguments. The trusted host binds the task's actual observer, run, grip and fresh observation; model text does not choose ownership tokens or acquire broader authority. The first local-model development trial repeatedly mixed IDs and was rejected; the simplified adapter removes this bookkeeping from the model while retaining native checks.

An action intent is checkpointed before native effect. The completed receipt records request hash, source scene/run, before/after physics, ticks, target error/outcome and release velocity. Identical successful operation retries return the saved receipt. Pending/interrupted operation IDs are not retried. On restart, unfinished run/operation records become interrupted; no constraint or claim of completion is restored.

Native state cannot be rolled back by a file transaction. A failure after an intent is treated as failed/uncertain, releases the joint where possible and does not certify success. Image/export files can remain orphaned after a failed receipt write; they are not successful receipts.

## Replay and evidence

Completed manipulation replay re-creates the frozen definition, then executes recorded grab/move/release commands and exact tick counts through native physics. Final position/tick comparison uses 0.0001 m on the tested single-precision backend. This is distinct from video export, which renders recorded poses.

[README 015](../README-015.md), [Java tests](../workspace-engine/src/test/java/fraymus/workspace/BodyActuatorTest.java), [visible workflow](../workspace-desktop/test/body-integration.cjs) and [local-model fixture](../workspace-desktop/test/live-body.cjs) document actual outcomes. Physical policy training, articulated grasping, collision-aware walking and arbitrary-body/mesh scenes remain subsequent milestones.
