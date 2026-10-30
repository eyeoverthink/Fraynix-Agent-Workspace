# Source-qualified spatial worlds — edition 021

The Spatial tab adds a separate, functioning box-scene runtime to the current workspace. It imports source-qualified geometry, retains object identities, forecasts several physical action sequences, actually manipulates a selected body and retains a procedure only after native outcomes and the actual report pass. The original game, ramp, learned ramp predictor, data procedures, browser and creation canvas remain available.

The first scene is explicitly synthetic. Its one-metre reference checks the declared centimetre conversion; it is not an independent measurement of a captured room. Mass, friction, restitution and gravity are declared inputs. A photograph or splat cannot supply these properties by itself.

## Complete review workflow

1. Start the workspace and choose **Spatial** beside **3D Lab**.
2. Choose **Load labelled test scene**. The source band identifies synthetic data, centimetres converted to metres, a declared reference and uncalibrated physical properties.
3. Select the movable crate. Clicking the view selects an existing entity; it never stamps a new one. Selecting a static body clears the host's actionable selection.
4. Leave the relative displacement at X=4 m, Z=0 m. This procedure ends at the source height; unsupported vertical displacement rejects explicitly.
5. Choose **Forecast only**. Four finite native routes are considered: direct, over, left and right. The direct route is blocked in the fixture. The orange trace comes from a separate physics world. The displayed world is unchanged.
6. Choose **Grab, move & release**. Java owns a new trial from the immutable imported state. The viewport follows actual native positions at 240 Hz, sampled for the UI. A real point-to-point constraint holds the crate; there is no viewport teleport.
7. Java checks the actual endpoint, speed, release and forecast residuals. The desktop opens a real chart/audit artifact and runs eight assertions in Chromium. A successful named procedure appears in **Verified skills**.
8. Import a changed scene, open the saved procedure and explicitly choose its scene and dynamic object. Every reuse forecasts again; it does not replay a previously successful route blindly.

For chat, select the imported dynamic object first, then use:

> In the imported spatial scene, predict and move the selected crate with displacement [4,0,0] metres. Grab, move around the obstacle and release it. Chart predicted versus actual motion and save the successful procedure as spatial-crate-route.

Then, after selecting a changed source:

> Reuse the spatial-crate-route procedure in the selected imported spatial scene with displacement [4,0,0] metres. Replan, move, release and report the actual checks.

The model chooses semantic options. The host binds the actual selected scene/object and registry name/version; explicit vector and skill-name intent are checked. Titles and source notes remain untrusted data. No imported field installs a tool or changes execution policy.

## Import contract

The complete starter file is [declared-test-scene-v1.json](../workspace-engine/src/main/resources/spatial/declared-test-scene-v1.json). Accepted top-level fields are exactly:

| Field | Meaning |
|---|---|
| `format`, `version` | `fraynix.spatial-scene`, numeric version 1 |
| `title` | Nonempty, at most 120 characters |
| `units`, `frame` | Explicit m/cm/mm and `right-handed-y-up` |
| `source` | Qualified kind, note and property basis |
| `scaleReference` | Two source-space endpoints, known length in metres, tolerance and basis |
| `gravityMps2` | Numeric declared gravity, 0.1–20 m/s² |
| `objects` | 2–32 boxes, at least one static and 1–8 dynamic bodies |

Source kinds are `synthetic`, `measured-user-label` and `unverified`. The measured label records a user's assertion; it does not authenticate collection. Physical properties are `declared` or `measured-user-label`. Reference basis is `declared-test` or `user-measurement`; independent calibration is always false in this version.

Each box supplies a unique `sourceId`, label, shape=`box`, position, unit quaternion rotation, size, mass in kilograms, friction and restitution. Dimensions convert to SI. Positions are bounded to ±20 m; box dimensions are 0.05–40 m; mass is 0–20 kg; friction is 0–2; restitution is 0–0.8. The JSON is limited to 128 KiB. Invalid units, inconsistent reference lengths, nonnumeric values, duplicate IDs, nonunit quaternions and unknown fields reject before creating the scene. Meshes, splats and executable fields are unsupported and reject; there is no inactive mesh-import placeholder.

An import receives a new scene UUID. Runtime object UUIDs derive from that import UUID plus each source ID. They persist across process restart; a separate import receives fresh identities. The original text and SHA-256 remain attached to the normalized definition. **Export scene** writes the original text exactly, with a separate local receipt; reimport preserves its source hash while assigning a new import identity.

## Prediction, execution and evidence

`SpatialPhysics` creates a real native Bullet world from all imported boxes. Each world belongs to one thread. All dynamic bodies and contacts evolve through the solver. The backend remains Minie 9.0.3 / Libbulletjme 22.0.3, single precision, fixed 1/240 s integration.

The bounded planner evaluates direct, over and two lateral waypoint sequences in separate native worlds. Conservative box extents generate alternatives; actual solver outcomes determine feasibility. It selects the shortest feasible candidate from at most four, with a 16 m path bound. This is a finite candidate planner, not a complete search algorithm. Some physically feasible scenes can still be rejected because no candidate passes.

Execution settles for 120 ticks, attaches a point-to-point joint, moves its anchor at at most 1.2 m/s, allows 96 settling ticks per waypoint, releases without resetting velocity and runs 240 more ticks under gravity and contact. Constraint impulse is limited to 300 N per axis times dt. A run stops at 6,000 native ticks (25 simulated seconds). These are simulated manipulation actions, not a robot arm, desktop mouse or external actuator.

The [acceptance declaration](../workspace-engine/src/main/resources/spatial/acceptance-021.json) precedes first execution. Gates are final horizontal error ≤0.1 m, vertical error ≤0.15 m, terminal speed ≤0.25 m/s, each waypoint endpoint error ≤0.08 m and maximum aligned forecast position error ≤0.02 m, with exactly one grab and one velocity-preserving release. Java reconstructs these checks from native samples/events and final body state. It rejects source, action, tick or outcome inconsistencies. The LLM's explanation is not evidence.

The forward model deliberately uses the same solver and parameters as execution. A zero or tiny forecast residual therefore demonstrates controlled execution consistency. It does **not** establish learned generalization or validate physical reality. The existing learned ramp predictor remains a separate, narrower model; its earlier recorded-data forecast rejection is unchanged.

Seven real files accompany each passing artifact: `index.html`, `original-scene.json`, `normalized-scene.json`, `native-run.json`, `prediction.json`, `motion.csv` and `verification.json`. The report shows predicted/actual X over time; raw JSON/CSV retain all three position components, velocities, object states, selected plan and rejected candidate outcomes. The workspace's Source view can inspect the files. Spatial scene/run export produces actual local JSON receipts. No video-export claim is added for this new view; the established native ramp video workflow is separate.

## Persistence, observations and recovery

`SpatialStore` atomically replaces `spatial/state.json` inside the selected private project. It retains up to 32 scene imports and 32 native runs. It never shares the legacy default database silently. A task binds the source/version/hash, object and relative displacement before effects. Native effect IDs derive from goal and attempt, preventing duplicate creation for the same recorded effect.

The shared Java goal runner registers `spatial-manipulation-v1` as a third family. It uses the existing goal checkpoint, host-effect lease, pinned report hash and actual canvas receipt. One goal is active at a time. Spatial ownership is separate from the ramp/data handlers. Cancellation stops the owned native constraint; failed or cancelled work never becomes a successful procedure. Process loss marks unfinished work interrupted. Resume is explicit, limited to three attempts after interruption and a 180-second budget per attempt. Incomplete physics starts a new recorded attempt from the immutable import; completed matching effects can be reused for report recovery.

`Check visible objects` computes oriented-box ray visibility from the actual view pose. It distinguishes all known identities from the sampled visible subset. Occlusion does not erase identity, and known hidden state is not described as visual recognition. This is structured scene perception, not segmentation of an arbitrary camera image.

## Next evidence gates

First, add separately collected, scale-checked scene geometry with a known measurement, uncertainty and declared or measured material properties. Keep capture provenance and import validation separate from native task success. Then add collider/mesh support only when file validation, coordinate conversion, rendering, collision and export all work together. More diverse interaction tasks need independent evaluation cases, measured failures and rollback before any learning-based policy is promoted. Generated video or Gaussian renderings could later supply a sensory layer; Java should continue to own geometry, actions and outcome evidence.
