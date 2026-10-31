# Reference images, dimensional data and playable physics

Edition028 · [Acceptance plan](EDITION_028_PLAN.md) · [Previous mathematical kernels](PROCEDURAL_SPACE.md) · [README028](../README-028.md).

## What a reference teaches

Java ImageIO decodes the original opaque PNG after checking its format and dimensions. Inputs are limited to256KiB,256pixels/axis and3–32tiles/axis. An explicit integer tile size1–16pixels must divide the image. The dominant RGB colour in each tile is counted deterministically. More than8 classes, transparency, unknown floor colours and an all-open/all-blocked palette reject.

The UI can inspect the real palette and suggest its most frequent colour as floor. The user confirms or changes that declaration before import. Suggested passability is not measured material recognition. A reference records original PNG bytes/SHA256, tile coordinates, palette, class counts and directed north/south/east/west adjacency. Opposite directions agree through actual observed neighbour pairs. No Processing code, new algorithm library or model weights are used.

Generation accepts8–24cells/axis, seed0..1,000,000,000 and at most64user pins. Boundaries are pinned to a blocked class; spawn/goal are pinned walkable. Random initial cells use observed class frequencies. Bounded minimum-conflicts repair chooses editable conflicted cells, scores candidate classes against actual neighbour rules and preserves hard pins. At most8restarts×20,000repairs per restart are attempted. An independent pass enumerates every edge, checks every pin, floods the grid and verifies the selected shortest route. There is no hidden corridor carving. An unsatisfied result retains its cells/conflicts/reason but cannot start physics or retain a successful procedure.

The contract requires a spawn-to-goal route. Other walkable components can exist; total and reachable floor-cell counts remain visible. This is not a guarantee that every generated floor cell belongs to one component or that local adjacency reconstructs the reference's global structure. Class frequencies guide generation but are not promised as exact output proportions.

## From data to an actual world

Compilation maps the grid to a right-handed native3D scene: grid X becomes physical X, grid Y becomes physical Z, and gravity acts along physical Y. Cell scale0.6–1.2m, friction0.05–1.2, avatar mass1kg and gravity9.81m/s² are declared parameters. Blocked tiles become solid1.1m-high barriers. They are not inferred water, terrain or material mechanics. A greedy rectangle compiler covers blocked cells without covering walkable cells; the floor and at most512colliders use the existing Bullet runtime. The avatar is an actual dynamic sphere of radius0.18m.

The native world owns fixed240Hz stepping. Manual input produces bounded desired velocity and a force capped at24N. The feedback controller uses measured position/velocity and verified grid waypoints; it never assigns a new body position after spawn. A run-bound input ID and400ms expiry prevent old held commands from controlling a later session. Release/blur removes directional input; native damping, friction and braking forces determine stopping. Manual sessions run1×; agent demonstrations run4× wall-clock simulation speed. Each attempt is bounded to90simulated seconds.

A scheduled impulse of at most±0.5N·s tests recovery. Its unit is momentum impulse, distinct from a velocity command. Native traces record time/tick, position, velocity, applied force, floor/wall contacts, route progress, integrated force effort and position-assignment count. Contact totals count sampled native contact observations within0.015m, not unique impacts. Force effort integrates force magnitude over time in N·s; it is not mechanical work in joules. Verification recomputes travel, speed, force limits, blocked-cell samples and endpoint/terminal-speed error. Agent success requires an actual completed automatic run, every route waypoint, endpoint≤0.2m, speed≤0.2m/s, no blocked-cell samples and no post-spawn position assignment. A manually stopped attempt is not a successful agent task.

## What happens to a4D map

The original027 dungeon data remain complete. For dimensions3or4, physical compilation takes an explicit coordinate for each hidden axis and uses axes0/1 as the selected grid. It never unions unrelated layers into one map. The UI accepts a layer JSON array or enumerates layers automatically. Each component supplies a deterministic two-flood route candidate; automatic selection chooses the longest candidate among those inspected. This is not an all-pairs diameter optimiser. Selected layers, the choice basis, original source goal/hash and every original N-D coordinate remain in the package and report.

Physical play still has3spatial axes. The agent's state can also contain time, source dimensions, material parameters, uncertainty, goals and relationships. Those additional variables have names, types, units and provenance; they are not extra physical directions merely because they occupy a vector column. A2D screen can expose several views of that persistent state without becoming the state itself. This is the concrete connection between the user's node/world idea and this edition, not a claim of particle-physics equivalence or conscious experience.

## Tasks, skills and durable evidence

The shared Java runner registers `map-world-v1`: freeze source/options → generate/compile → start native attempt → verify native outcome → write report → verify actual browser charts → retain procedure. Chat supplies semantic options; it cannot supply result coordinates, colliders, success flags or executable handlers. Source identity, goal progress, map definitions and completed attempts persist. Resume reuses completed native evidence and stable report identity. Incomplete native motion becomes a separately recorded attempt from the immutable definition. Cancellation stops owned native motion and prevents skill retention.

Native motion is sampled every12ticks. Completed/explicitly stopped traces are persisted; a process interruption does not resume an uncommitted native world in place. Sources and compiled definitions persist across restart. This bounded lifecycle is distinct from an unlimited background agent or continuous model training.

The existing Skills library retains versioned procedures with prerequisites, checks and recovery. Reuse binds a fresh reference or compiled map and re-verifies. Learned adjacency tables are data learned from the reference; saved procedures are procedural memory; feedback is a controller responding to measurements. None updates Ollama weights.

The report includes complete map/source JSON, native run JSON, trace CSV and six Java-derived chart datasets. Actual Chromium input switches through all six charts. The private host reads every SVG point and Java compares every frame with the original trace within0.002SVG units. A caption or claimed pass is insufficient. The3D avatar view reads actual native positions; it does not animate an invented route.

Export writes a real `fraynix.playable-map` JSON package through the existing project export service. Import is bounded to2MB, validates hashes, re-decodes original image/rules or reconstructs the original dungeon layer, and compares cells, route and colliders before assigning a new identity. It does not install code or accept imported metrics as proof of a native run.

## Validation and next gates

Software controls use labelled synthetic PNGs and actual native/desktop execution. Actual local-model tests are reported separately, including source and tool receipts. Changed images, seeds, declared scale/friction, impulse direction, impossible pins, real manual contact, export reconstruction, restart and cancellation are checked. Keep the first failures alongside results.

Next: evaluate larger/global map constraints, preserve longer world sessions through explicit state checkpoints, add measured material calibration and perception-to-state correspondence, then compare alternative navigation/learning policies on separate tasks with rollback. New dimensions or materials need explicit semantics and tests. More graph nodes alone do not establish useful learning.
