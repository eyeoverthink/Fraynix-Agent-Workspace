# Shared spatial runtime · edition040

Open **Spatial → Linked views**. Choose an existing persistent world and click **Link world**, or create the explicitly labelled test world. The Java runtime returns one source-qualified snapshot, and Java computes its perspective, top, front and side projections. Picking an object in any view selects the same source UUID. **Focus view** enlarges one camera; **Back to native controls** returns to the existing manipulation interface.

**Open & link original game** explicitly starts the preserved game runtime. Its original raster and two geometric views share a source tick, session and entity identities. Step left/right/jump delegates to the original bounded controller. Pause before picking a moving source. The game uses XY coordinates and legacy game units. Its quads lie at z=0 for presentation; render layer is draw order, not physical depth. This interface does not convert JBox2D into a 3D solver or replace original sprite rendering.

Native worlds retain declared metres, right-handed Y-up coordinates, quaternions and physical properties. A snapshot distinguishes a committed ledger head from an uncommitted native attempt. Only a matching pending run can supply live poses. Head and ownership are checked on both sides of the read. Selection of a changed clock/revision is refused; refresh instead of guessing.

An optional **Separate recorded observation** shows an existing bound Tabletop recording in its own mat plane. It retains the original run/body/geometry/code/image hashes, source seconds, estimate and measured-versus-prediction status. Height and native alignment remain unknown. The human object association links identity evidence, not coordinate systems. No camera is opened by these views, and raw camera pixels are absent from spatial exports and reports.

Chat exposes `runtime.read`, `runtime.project` and `runtime.report`. Read pins the selected source for the task; project accepts only the four declared camera names; report uses that exact pinned snapshot. Full scene data is not dumped into the model context: the summary prioritizes the selected entity and includes at most16 rows. The complete export/report still retains every source entity. Host context names the actual binding and selection, with the full registered tool catalog. These tools are read-only; existing goal controllers retain action authority.

The report is a real saved canvas artifact. Four host assertions check snapshot hash, selected UUID, view count and row count. `snapshot-gzip.json` retains the full source-qualified JSON, compressed with its original byte count and SHA-256. `summary.json` and individual view JSON files accompany the rendered report. **Export full JSON** writes a real local file. Run `node scripts/Inspect-Spatial040.cjs <absolute-export-path>` for independent checksum, identity and coordinate checks. A valid checksum proves internal consistency, not permission to import or actuate a world.

Selection, binding and focus survive restart in `spatial-runtime-040/state.json`, with a checked envelope and atomic replacement. Cached projections are process-local:16 snapshots, at most two pinned task snapshots. A restarted game requires explicit launch/rebind. Native worlds remain durable; the read interface cannot advance a simulation or rewrite its ledger.

Bounds are4096 game entities,32 native entities and160–4096-pixel viewports. The visible linked view refreshes at most once per second and stops polling when hidden. Filled faces use depth ordering, not a per-pixel depth buffer. The original game raster remains the authoritative sprite image. Large native scenes, transparent surfaces, textured reconstruction and general N-dimensional physical simulation are outside this edition.

## Resource-conservative planning after the restart

Set `FRAYNIX_WORKSPACE_PLAN_CPU_ONLY=1` before starting the desktop app to request agent tool planning with `num_gpu=0`, four CPU threads and no retained model after each call. This affects `/api/plan`; it does not change global Ollama settings, disable the original OpenGL renderer or claim every other inference path is CPU-only. Planning responses retain the requested compute options. CPU planning can be slower. The interrupted first040 model run is retained as an interruption, not a passing suite. Windows recorded an unexpected shutdown with no bugcheck cause; that is not a diagnosis of a GPU or power fault.

## Verification and next gate

See [edition040](../README-040.md), its [verification record](readmes/040/verification.json), [declared contract](../workspace-engine/src/main/resources/runtime-040-contract.json) and [independent source inventory](readmes/040/file-inventory.json). Existing038 physical displacement/uncertainty remain UNVALIDATED. The next physical step needs explicit frame registration, stronger target/background discrimination and genuinely new measured footage. A later reconstruction step must evaluate depth and scale separately. Neural policy promotion must demonstrate better held-out task outcomes and retain rollback; persistent identity or a new camera view is not itself weight learning.


## Measured alignment successor

[Edition041](../README-041.md) now supplies an explicit declared mat-to-native plane placement, separate withheld-grid/tape checks and fresh independently referenced physical movement/occlusion results. [Measured operation](MEASURED_ALIGNMENT.md) explains its source fences and remaining height/contact/uncertainty limits. It adds diagnostics without moving native bodies. Earlier038 failures stay preserved and unpromoted.
