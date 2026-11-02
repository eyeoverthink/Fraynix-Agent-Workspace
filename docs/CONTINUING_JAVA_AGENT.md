# Continuing Java agent · edition042

The workspace remains one application. A headless Java process now owns the project and continuing brain state. The existing desktop is its reconnectable interface, browser host and canvas host. The primary Game Lab button also opens the existing embedded original renderer, with its native window hidden and editor uninitialized. Legacy editor sources remain preserved. No new editor, inference framework, neural library or second user-facing window is added.

## Ownership and startup

Normal desktop launch starts or reconnects to the project's Java owner. Before opening stores, Java acquires `runtime-042/host.lock`. A private loopback descriptor contains the current port, capability, PID, stable agent identity, boot identity and installed-JAR fingerprint. The interface verifies the actual server against that descriptor and its own installed Java build; it never kills an arbitrary PID from a file. A different build requires explicitly stopping the current runtime before upgrading.

Only one interface lease may be live. It renews every two seconds and expires after seven seconds without renewal. Java checks that lease on host operations. A stale interface cannot detach or acknowledge work belonging to a replacement interface. Expired-host cleanup finishes before a new lease is accepted. Windows starts the Java owner through a hidden, independent process; it does not inherit the interface's lifetime or debugger handles. This release is validated on Windows and Java21.

## Everyday controls

1. Launch the existing app normally using the repository's start script.
2. Expand **Java brain** in **Your agent**. It shows retained active time, real heartbeat count, source experiences and the current scheduling reason.
3. **Pause background** pauses sensory updates and replay. It leaves the clock and explicitly started Java tasks running.
4. **Save checkpoint** verifies the witness and atomically records checked state and its neural-file pointer.
5. **Open evidence** saves a dated JSON/HTML report and opens it in the existing canvas. A task must finish before another report takes over its canvas.
6. Close the app normally. Java continues; camera, browser, canvas and original-game host activity stop.
7. Reopen the same project/build. The interface reconnects to the same Java boot. No camera or model request starts automatically.
8. To stop background Java itself, expand **Identity, memory and witness** and choose **Stop Java & close app**. Active tasks and captures must first finish or be stopped.

Java does not restart itself after Windows shuts down. Opening the app starts it again from its checked checkpoint, with the same agent identity and a new boot identity. A crash may lose work after the latest committed checkpoint. Uncertain effects require inspection and explicit resume; missing or corrupted evidence causes attention instead of a silent reset.

## Clock domains and recovery

Wall time is for dated receipts. `System.nanoTime()` measures elapsed time within one boot. Retained active time adds checkpointed earlier activity to this boot's measured elapsed time. Logical heartbeat ticks count actual callbacks; they are not fabricated catch-up ticks. Native simulation ticks and camera source seconds remain separate. Downtime is not simulated activity. The nominal heartbeat is250ms and collection uses a separate one-second scheduled worker; these are best-effort desktop schedules, not hard real-time guarantees.

State uses a canonical SHA-256 envelope, forced temporary-file writes and an atomic replacement. Neural files are immutable, binary, shape-checked checkpoints with both file and data hashes. The state pointer commits before an older owned neural file is removed. The background adapter uses the existing `BrainService` cortex, not a second neural network. Foreground cortex changes are included in checkpoints, and actual physics policies retain their existing independent stores, evaluations and promotion gates.

The witness reuses the owned Merkle journal:256 records per segment, parent-root links between segments and an exact record-count/root checkpoint anchor. Original corrupt or pending evidence is retained and refused. The budget is1024 segments; exhausting it requires deliberate archival, rather than overwriting history. Integrity establishes sequence/content consistency, not external truth or an external signature.

## What the continuing brain computes

The Java core has32,768 leaky integrate-and-fire neurons and1,048,576 synapses with the existing plasticity implementation. New operational/native-source changes and accepted observation receipts feed bounded sensory features. At most128 source experiences are retained; each may be replayed once. Replay is labelled retained evidence. Neural activity, changed weights and successful checkpoint restoration are measured computation, not a claim that an arbitrary task policy improved.

The appraisal uses actual heap occupancy, collection latency, source novelty and observation freshness. Resource/latency pressure suppresses background neural work; changed sources permit sensory integration. The displayed alertness and progress values summarize those conditions. They are computational state, not measured body temperature or felt emotions. Idle operation starts no autonomous web browsing, camera or language-model calls.

Only the existing Java observer's successfully accepted041frame route can feed a new camera receipt into this adapter. It carries original image hash, trial/ordinal, original source seconds, receipt time, reference validity and numeric planar estimate. Pixels stay in private storage. A receipt is fresh only when recent, reference-valid and measurement-accepted. Prediction-only/hidden samples do not become fresh measurements. Replaying an already accepted ordinal does not renew freshness. Restart begins without a fresh camera receipt. Native positions and mat-plane positions occupy separate sensory features; no undeclared conversion into measured depth occurs.

## Tasks, tools and skills

Pure Java goals and physical-policy evaluation can continue with no interface. A goal requiring browser/canvas action waits for its registered host. An already claimed action whose interface disappears becomes **interrupted**; its old acknowledgment is rejected. Explicit **Resume** reconciles saved evidence, reusing completed verified physical work. Unfinished chat episodes are also interrupted, independently of a Java goal that continued successfully.

Every local-model planning request receives a compact Java identity/clock/status/witness context alongside the existing capability inventory and bounded retrieved memory. Two typed tools, `agent.read` and `agent.report`, accept no model-supplied identity, time, measurements or success flags. The checked workflow is read → report → finish. Its report is verified against the actual Chromium DOM before its episode may be marked verified. Reports contain `snapshot.json` and a hash receipt, not private capability tokens.

The existing distinction still applies: a tool performs an action; a skill combines actions, checks and recovery to finish a task. This release preserves the existing versioned procedures and held-out physics-policy gates. It supplies continuity beneath them; it does not turn sensory replay into automatic approval of new code or policies.

## Remaining work

Move more surface-independent planning and skill scheduling out of the desktop coordinator into this Java runtime. Add measured episode prioritization and compare candidate controllers on unseen tasks before promotion. Profile the real camera-to-observation path before increasing capture rate; use new held-out rotation, lighting, movement and occlusion tests. A further goal is explicit restartable background research, still with source receipts and resource limits. Existing UI-dependent model tasks and browser/canvas actions are not promised to run while those surfaces are closed.

See [README042](../README-042.md), [prior measured evidence](../README-041.md), [contract](../workspace-engine/src/main/resources/agent-042-contract.json) and [project memory](PROJECT_MEMORY.md).
