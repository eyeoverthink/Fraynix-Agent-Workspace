# Contact-driven pushing contract

Edition 031 extends the existing Spatial view, shared Java goal runner, local chat,
canvas and verified Skills. A tool performs an action; the pushing procedure
completes a task using planning, actions, checks and bounded recovery.

## Authority and scope

Java owns immutable imported scene bytes, role bindings, planning, native physics,
durable task progress and independent verification. Electron displays state and
hosts the browser/canvas. Ollama supplies semantic task choices, not physical
trajectories, privileged identifiers, success flags or replacement demonstration
code. Existing map, grab/move/release, browser, creation and tracking work remains.

This is declared-scale three-dimensional rigid-body simulation, with planar
integer-metre planning. It is not a learned general world model, a robot hardware
controller, photographic identification, measured material inference or language
model weight training. The labelled examples are synthetic development controls.

## Admitted source

The existing `fraynix.spatial-scene` import preserves exact source text and its
SHA-256 while normalizing declared units. Pushing supports an axis-aligned static
floor with top Y=0 and horizontal extents 5–12 m; static boxes; and exactly two
distinct dynamic boxes. Each dynamic box has horizontal size 0.4–0.8 m, height
0.3–1 m, mass 0.5–4 kg and starts on the floor at integer X/Z positions. A request
specifies relative `[X,0,Z]`, each horizontal component −4…4 m, Manhattan distance
1…8 m. Unsupported scenes fail before native actuation. All physical properties
are declared in the source; no image-based scale or friction estimation occurs.

The initial gentle pushing cap defaults to 8 N, and the original maximum defaults
to 32 N. Both are finite, 2–60 N, with initial ≤ maximum. The maximum also bounds
walking, alignment, braking and recovery. Reusing a skill retains unrequested
force defaults; the model cannot silently raise them.

## Search and control

`PushPlan` searches joint crate/pusher configurations. Before each cardinal push,
the pusher must reach a standing cell behind the crate. Conservative footprint
and swept-path checks keep the crate an obstacle to the pusher. Search stops at
8,192 expanded states and 12 pushes. A second pass checks the returned approaches,
push sweeps and reconstructed action list. A free crate path alone is insufficient.

`PushPhysics` owns the existing native Bullet space at 240 Hz, normally scheduled
at four times wall-clock speed. The upright pusher receives bounded horizontal
forces. Its angular factor is explicitly zero. The crate remains an ordinary
dynamic body and moves through native contact; there is no gripping constraint,
direct crate force, velocity replacement or pose write after initialization.
The controller aligns with the actual crate before contact, uses position and
velocity feedback plus declared-friction feedforward, and checks intermediate
position and speed. Internal push error tolerance is 0.035 m; final acceptance is
at most 0.1 m crate horizontal error and 0.2 m/s total crate speed.

A jam requires actual pusher/crate contact and less than 0.02 m crate progress
over 240 ticks while away from the endpoint. Recovery retreats, reapproaches and
may increase the gentle cap inside the original maximum. At most two recoveries
and 14,400 ticks / 60 simulated seconds are allowed. Exhaustion keeps a failed
attempt; it cannot create a successful report or retained procedure.

## Persistence and restart

`PushStore` has one native owner worker. Samples are captured every 12 ticks;
atomic checkpoints normally commit every 120 ticks and at pause/settlement. Live
observations publish after the corresponding checkpoint commits. A checkpoint
contains both positions, quaternions, linear/angular velocities, force/work/effort
counters, complete plan, controller phase, alignment, action index, hold/settling
counters, stall window, retreat target and recovery counts, original samples and
events. The binding includes original source, code, JDK/platform and actual native
artifact identity. Sorted-key/numeric canonicalization protects the checksum.

After interruption, explicit Resume restores the same goal, attempt and run from
its committed boundary. Uncommitted tail work is discarded. Actual initialization
of both native bodies is separately recorded and compared, including equivalent
quaternion signs with 1e-6 tolerance. The original prefix and action/recovery
counters must survive. Native contact caches rebuild; this is not bitwise solver
memory serialization or a promise of identical future floating-point trajectories.

Corrupt, changed-source or changed-runtime checkpoints are refused and preserved;
the app cannot silently respawn fresh bodies as successful recovery. Explicit
Stop abandons an interrupted/paused task and releases controls without promoting
it. Cancelling an active task stops its native work and retains evidence.

## Independent evidence and retention

Each controlled tick records
`[tick,pusherId,Fx,Fz,pusherDx,pusherDz,crateDx,crateDz,contacts,actionIndex]`.
`PushVerify` reconstructs horizontal positions, actuator work and force effort,
checks the force recipient and original cap, native contact evidence, action
endpoints/order, initialization-only pose writes, jam windows, recovery/resume
counters and final position/speed. Signed pusher work is F·dx in joules. Force
effort is the integral of force magnitude over time in N·s; they are distinct.
This ledger does not claim complete thermodynamic/contact energy accounting.

The real canvas displays original geometry, planned crate route, actual crate and
pusher paths, and six native charts: target distance, speed, force, contacts,
effort and signed work. The host exercises each chart control; Java compares every
rendered point (0.002 SVG-unit tolerance) and exact visible plan/intent/events/
checkpoint/verification data. Only then can a versioned `spatial-push-v1` skill be
saved. Reuse binds fresh actual body IDs, replans and verifies another native run.
Procedural retention is distinct from policy training or policy promotion.

## Files, APIs and observations

- `pushing/state.json` and `pushing/checkpoints/<runId>.json` stay in the selected
  private project directory, outside Git.
- Typed tools `push.read` / `push.start` and shared `procedure.run`, `goal.read`,
  `goal.resume` / `goal.cancel` retain host-controlled identifiers and ownership.
- `/api/push/preview`, `start`, `snapshot`, `scene-state`, `run`, `pause`, `observe`
  and `export-run` expose registered operations through the existing local server.
- Object observation uses actual committed native transforms and oriented-box ray
  visibility, labelled as state knowledge rather than pixel recognition.
- Scene export preserves original source bytes. Full run export is plain JSON.
  Reports include a measured CSV, compact view JSON, exact original proof data,
  and a lossless gzip/base64 native package with decompressed-byte SHA-256.

## Validation and the following phase

Native tests cover free/corner/high-friction execution, unreachable standing poses,
insufficient force, per-tick tampering, moving-state and mid-recovery restart,
checksum/runtime refusal and byte-preservation of edition030 Map classes. Real
Electron tests exercise actual controls, Skills reuse, crash/relaunch, exact
exports and genuine canvas receipts. Actual installed Ollama is tested separately.
Edition031 publication includes the exact outcomes and development failures.

The following phase should use a separately frozen set of varied masses,
frictions, obstacle placements and starting orientations. Measure success rate,
contacts, effort, recovery cost and failure reasons. Compare a frozen baseline to
any candidate and retain rollback before claiming policy improvement. Broader
geometry needs a new planner/control contract, not a wider success label.
