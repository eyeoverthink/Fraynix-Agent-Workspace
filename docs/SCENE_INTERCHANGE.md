# Scene interchange, media and replication

Edition 015 supplies working exports and bounded native scene import. Files and source documents remain data; they do not install handlers, grant privileges or train model weights.

| Type | Import | Export | Current meaning |
|---|---|---|---|
| fraynix.scene v1 JSON | Yes, separate scene after validation | Yes | Supported authored ramp/block parameters, geometry, units/frame and provenance |
| fraynix.run v1 JSON | Not yet | Yes | Frozen native definition, samples, final state, actuator commands and outcomes |
| CSV measurements | Existing explicit column/unit/time mapping | Native trajectory CSV | Engine-generated CSV is synthetic; do not label it instrument data |
| PNG observer frame | Historical captures from own project | Yes | Actual rendered image with source/run/tick/pose/hash |
| VP8 WebM | Not converted into native physics | Yes | Actual recorded poses rendered at 960×540, nominal 30 fps, silent, ≤30 s |
| HTML/CSS/JS artifact | Existing creation/source workflow | Existing saved artifact files | Runnable isolated canvas; separate from physical scene import |
| glTF/GLB/OBJ meshes | Planned | Planned | Need asset/resource/transform/material/collision contracts and real native checks |
| PDFs/images/audio/video as scene knowledge | Planned extraction adapters | Source-linked artifacts as supported | Need source spans, units, uncertainty and typed mappings; no automatic scene creation claim |
| Java/C/Go physical source | Planned typed IR/build backends | Planned | Requires real compiler/test evidence, not automatic semantic equivalence |

Scene files are ≤64 KB and contain format/version, title, explicit units/frame, all seven bounded native parameters, generated role geometry/hash and source scene/version. Unknown fields, executable payloads, altered geometry/hash and different units/frame are rejected. The current geometry schema supports only ramp, ground and dynamic block. Import/clone mints new scene/entity IDs and a source-package/provenance receipt. Active native physics must be reset explicitly first.

The import source digest hashes the validated normalized JSON package, not original whitespace/byte encoding or an author signature. Export receipts separately hash the exact saved file bytes.

The UI file chooser previews metadata. **Import selected scene** performs validation and creation. Selecting a file never places it. Typed scene.import/scene.clone tools use the same native importer. Models cannot nominate an arbitrary output path; export filenames are generated under the selected project's exports folder.

Run JSON preserves native serialization, including signed-zero numeric representation. CSV contains recorded samples at 60 Hz; a non-sampled final tick is retained separately in run JSON. CSV columns specify seconds/metres and include velocity, quaternion, contacts and displacement. WebM selects actual recorded poses along the native timeline and retains the final pose. Codec timing can differ slightly from the nominal source duration; video is evidence/visualization, not an interactive physics file.

Every export has a SHA-256/bytes/source receipt sidecar and manifest entry. The app displays and reveals actual files; WebM also plays inside the existing panel. No paid renderer, model weights, external codec service or FFmpeg installation is required.

Replication must name its contract: equivalent supported authored geometry with fresh IDs; native command replay with measured final-state error; or rendered historical video. These are different outcomes. Portable foreign run-package import is next: validate versioned commands, remap roles, replay within limits and compare trajectories before accepting replication.

For broader file absorption use parse → validate → retain source/provenance → map typed components → execute tools → check results. A future mesh/document parser must not convert source text into instructions. Learned policy updates need separate training/evaluation, measured improvement and rollback.

See [README 015](../README-015.md), [actuator contract](NATIVE_BODY_CONTROL.md), [measurement edition](../README-011.md) and [roadmap](SCENARIO_ENGINE_ROADMAP.md).
