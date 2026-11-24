# Independent audit of the visual reconstruction work

2026-11-24 · Edition 220

Two reviewers traced the earlier implementations before this work. A reviewer separately inspected the new geometry and checked the original and Blender-edited output.

| Prior work | Capability established | Reused for reconstruction |
| --- | --- | --- |
| [028](../../README-028.md) | Learns tile adjacency from labelled pixels, generates a playable layout, and checks traversal | Image provenance and the path from generated geometry to the app |
| [216](../../README-216.md) | Lifts a labelled image into boxes with declared heights, then exchanges the scene with Blender | Existing Spatial view and verified GLB transport |
| [217](../../README-217.md) and [218](../../README-218.md) | Preserves existing indexed triangles and checks a transformed copy or route using separate visual and collision geometry | Shared visual assets, distinct box collider, immutable originals and source checking |
| [219](../../README-219.md) | Chooses a camera and estimates a registered height with a declared footprint | Withheld verification and scoped evidence |
| Local physical-height preparation | Compares a marked planar span in two photographs against a ruler | Retained as a local experiment. The reconstruction release uses the image-to-mesh path. |

The audit also found a broken newline string in the edition 219 Blender generator. That generator was corrected and rerun. The graphics fallback lets the study retain its result when the display cannot create a 3D context. A desktop regression exercise with graphics disabled completed the original collider workflow.

The new reconstruction test puts original Blender geometry outside the production inputs, holds the fourth camera out of the fit, checks an actual Blender edit, and tests an unseen recess. The reviewer independently compared both GLBs and found the visual upper bound rose from about 0.25 m to 0.30 m while the lower bound and collider size stayed fixed. In general, an editable triangle surface can contain non-manifold joins. Downstream solid modeling needs separate topology checks.

For future phases, begin with the prior implementation, name what the current release adds, and choose a test that could disprove it. A scene round trip establishes interchange. A camera measurement establishes a dimension. A reconstruction claim needs a newly generated 3D surface and evidence against a view or geometry that was withheld.
