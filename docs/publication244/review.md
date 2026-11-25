# Independent system review

2026-11-25 · [Edition 244](../../README-244.md)

The review assessed Fraynix against the goal of a local agent that understands scene and data context, takes actions under its own runtime, checks the consequences, and retains verified work. An independent review agent inspected the existing entry points, implementation, tests and released evidence. This review uses software and simulation evidence.

| Expectation | Working evidence | Gap to close |
| --- | --- | --- |
| Speak plainly and reach the right local workflow | Selected-artifact follow-ups, a bounded local-model hypothesis trial, and reviewed prop placement are working examples. [Edition 211](../../README-211.md) · [Edition 215](../../README-215.md) · [Edition 232](../../README-232.md) | Specialized entry points depend on a visible selection and supported request families. A clean-start user must be able to discover what the system can do and see the selected source before it acts. |
| Let the agent observe, experiment and verify inside the app | The Java runtime checks native scene collisions, keeps the evidence with forwarded revisions, and supports verified replay in clean projects. The selected artifact can also feed a verified output. [Edition 128](../../README-128.md) · [Edition 239](../../README-239.md) | A user-facing record should join the request, inputs, proposed action, checks, and replay in one place. Repeat the workflow on unseen scenes and with varied phrasing. |
| Create usable assets and exchange them with Blender | Separate visual meshes and colliders round-trip. Registered browser views can support bounded reconstruction, and the Media workspace supports sprite placement, colliders, and offline playable export. [Edition 217](../../README-217.md) · [Edition 227](../../README-227.md) · [Edition 230](../../README-230.md) | Reconstruction needs usable views and scale. Hidden geometry, arbitrary editing, animated production assets, and broader Blender editor coverage need separate qualification. |
| Keep learning across sessions | Verified procedures can be inspected, retired, reactivated, and reused. Native receipt chains survive restart. [Edition 127](../../README-127.md) · [Edition 243](../../README-243.md) | Durable procedure reuse and improved model weights are different claims. Measure transfer and recovery on reserved tasks before promoting changes. |
| Install, start, and finish one job offline | The sparse install and guided proof give a small entry point into the Java engine and visual tools. [Install guide](../INSTALL.md) | The evidence archive lives outside the sparse checkout. Give new users a direct download for the starting scene and a short route through the finished workflow. |

## Decision for this edition

The reviewer identified a sequencing gap. The five-approach evidence appeared only after approving a revision and replaying it. Edition 244 moves that check into the pending editor and exposes a supported Chat phrase that reaches it. The reviewer also called for per-approach uncertainty, exact parent and candidate binding, and a fresh native check when archived measurements need renewed trust. Receipt validation on ordinary reopen checks bindings, data and summary. A fresh native run measures the motion again.

The read-only comparison is local and advisory. Earlier contact is a measured direction of change. The one-tick timing goal makes it possible to see which cases retain timing and which change it. The acceptance case is a clean Windows session using Signal Bay without a model. Blender import, physical accuracy, generalized model routing, and novel-task transfer need their own tests.

## Next acceptance gates

1. Run the same request on a held-out scene and a separate mesh-edit case. Show which measured effects depend on scene or approach.
2. Present request interpretation, selected scene, draft, evidence, and committed outcome as one durable mission record.
3. Evaluate supported task selection with a real local model across world, browser and creation tasks, including ambiguous wording, refusal and restart.
4. Retain successful procedures when held-out verification shows a reliable improvement in completion, recovery, or cost.
