
# Pick browser graphics and verify a two-view model

2026-11-24 · Edition 227

The browser picker intercepts the next trusted click while armed, uses the nearest image, SVG or canvas, checks that the full crop remains visible and captures Chromium pixels. It prevents link navigation for the selection click. Escape exits selection. The app normalizes selected crops to CSS pixels and strips credentials, query and fragment from saved URLs. The legacy selector workflow still opens and exports.

## Registered observations

- The front camera sees the horizontal X axis, and the orthographic view from the left sees the horizontal Z axis. The third camera is horizontally halfway between the first two and uses the same look-at center, scale and vertical framing. Provide a plain contrasting background with a margin.
- The input is three equally sized square crops. The app checks equal size and compatible vertical bounds. It uses the user's declared front width to convert the side silhouette's pixel extent into estimated depth. The app does not recover camera registration from ordinary perspective images.
- Java fits the intersection of the first two silhouettes on 16, 20 and 22 cell grids and checks the resulting shell. It chooses the grid with the best minimum fit across the first two views. The selected mesh is then projected into the 45° view and checked against the third captured silhouette.
- The third view enters after model choice. A duplicate capture or a mismatch refuses before creating a scene. The app stores the accepted crops, their hashes and the model's receipt. Restart reopens the same scene and rechecks the original capture on export.

## Independent measurements

A Blender scene assembled from two editable solids supplied all three reference renders. The alternate held-out view is a narrower prop with the same height. On the Windows run, the three inputs came through real browser clicks. The 300 mm estimated depth matched Blender's original and imported GLB extents. Fraynix's independent raster comparison reached 95.7% against the withheld capture. Blender's own render of the GLB and the original reached 100% thresholded silhouette overlap at 240 × 240 on this fixture. Its mesh volume differed by 0.43% because the source shoulder included a 2 mm overlap below the 50 mm grid resolution.

The alternate view scored 47.0% and created no scene. Tests also refuse a duplicate held-out PNG, mismatched crop sizes and changed retained bytes. This validates the registered orthographic workflow for the stepped test shape. Photo registration, concavities hidden in the projections and material recovery remain open.

Edition 226 CI failed on Windows and Apple silicon when its fixture exceeded the browser pane width. The fixture now scales at smaller viewport breakpoints while retaining the full graphic.

[Captured evidence](../readmes/227/browser-views-evidence.json) · [Original oblique render](../readmes/227/blender-held-out-original.png) · [Oblique render of the exported model](../readmes/227/blender-held-out-export.png) · [Browser modeling roadmap](../../README-226.md)

## Reproduce

Build the engine distribution and run the integration test. Set FRAYNIX_TEST_BLENDER to the local Blender executable to include independent GLB measurements. The Blender script can regenerate the registered fixture renders.

    ./gradlew :workspace-engine:installDist
    node workspace-desktop/test/browser-views-integration.cjs build/visual-browser-views227
