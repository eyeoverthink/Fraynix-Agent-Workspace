# Procedural space runtime contract

Edition027. [Research and tutorial differences](PROCESSING_ALGORITHM_REFERENCE.md) · [Acceptance plan](EDITION_027_PLAN.md) · [README](../README-027.md).

The existing shared Java runner registers `procedural-space-v1`. Its `geometry-core-v1` kernel has no runtime dependency beyond Java21. The desktop model passes type, dimensions, seed and optional numeric conditions through `space.start`; the private host binds and checks the request before Java starts. No source code, vertices or success flags are accepted from the model.

| Input | Accepted contract |
|---|---|
| Seed | Integer0..1,000,000,000 |
| Colonization |2..16 dimensions;16..512 attraction points;1..128 iterations; at most1,024 vertices |
| Branch conditions | Step0.015..0.08; influence0.12..0.6; reached radius0.015..influence, at least half the step |
| Dungeon |2..4 dimensions;12..40 cells/axis; at most65,536 possible cells;4..24 rooms; at most4,096 floor cells |
| Projection | Four recorded views at0/30/60/90 degrees; original coordinates retained; no frame interpolation or perspective physics claim |
| Lifecycle | Frozen definition and compiled-kernel SHA; cooperative worker interruption; stable report identity; explicit restart recovery |

Generation rejects unknown/type-inappropriate fields, nonfinite values and excessive volume. Colonization verifies parent ordering, fixed segment length and attraction accounting. A finite iteration/node budget can leave attraction points unreached; remaining counts and stop reason are reported. Successful data verification does not mean every requested attraction point was reached.

Dungeon generation preserves room bounds, centre-to-centre corridors and every original floor cell. Verification checks disjoint bounded rooms, declared room coverage, corridor endpoints/adjacency, graph connectivity and full flood reachability. The reported route is checked against a separately calculated minimal grid distance. This does not automatically compile the map into a native physics level.

## Completion and retention

The state sequence is compute → report → actual host verification → complete. Java reconstructs the deterministic result, pins the report content/version, and requires the exact registered browser test receipt. The host rotates, resets and slices the actual view. It reads all reset SVG coordinates directly from Chromium; Java checks IDs/count/order and every point within0.002 SVG units. A rejected coordinate observation interrupts the task and cannot retain a procedure.

The report has `space.json`, `extra.json`, `frames.json`, `coordinates.csv`, `provenance.json` and `index.html`. Original coordinates and topology are distinct from their display projection. Each projection is fitted to the display; pixel distances are not original-coordinate measurements. Growth replay uses recorded branch generations; dungeon stepping uses the actual checked route. Slices filter the last axis. Hidden-layer route edges are omitted.

After completion, an optionally named procedure enters the existing versioned Skills registry. Reuse preserves the algorithm identity and retained defaults, binds an explicit fresh seed or declared changes, recomputes data and repeats verification. Registry limits remain32 procedures total and8 versions per name. There is no uploaded executable handler, new model weight or self-modifying Java source.

## Portable data

`space.export` writes a completed verified package under the current project's `exports/` directory and returns its actual path, bytes and SHA. Existing edited exports are preserved. Symbolic-link targets are rejected. The JSON contains format/schema, normalized definition, implementation identity, complete data and data hash.

The real file-import control accepts a JSON package below2MB. Java checks schema, definition and hashes, then independently reconstructs the seeded result in a new goal and compares its hash. It creates and checks a new report. Supplied metrics are not proof. Kernel identity mismatches reject imports rather than silently reinterpret an older algorithm. Arbitrary file, video, executable and scene-code import are outside this contract.

## Responsive evidence workflows

New UI/chat evidence workflows freeze `responsive:true` and `responsive-v1`. A hidden unprivileged Chromium window uses the already restricted canvas session, loads the exact artifact and measures780px and390px viewports. It never injects replacement CSS. Java checks document/table bounds, four semantic header/body columns, their alignment/overlap, minimum table font12px and usable filter/reset controls. This is a targeted usability gate, not a complete accessibility audit.

Layout errors join source-cell/SVG errors in the existing three-total-draft repair budget. Numerical tolerances and native/source distinctions stay unchanged. The old direct API default remains false for compatibility; historical workflows are not assigned a new frozen contract after completion. Reusing a procedure through the current UI explicitly opts into the new profile.

## Verify locally

```powershell
.\gradlew.bat --offline check installDist :workspace-engine:installDist
cd workspace-desktop
npm test
npm run test:procedural
npm run test:responsive
# Require the already-installed local gpt-oss:20b service:
npm run test:live-procedural
npm run test:live-responsive
```

Run native application integrations sequentially. Do not rebuild installed JARs under a running instance. Tests create separate temporary projects and keep software-supplied controls labelled separately from actual model calls. Preserve existing review windows and private project data.
