
# Build an editable 3D prop from registered library images

2026-11-24 · Edition 231

The Media sprite library supplies the original PNG and its record ID for each view. Fraynix uses the original crop and margins for registration. Transparent images use their alpha silhouette. Opaque images use the same contrasting-background extraction as the browser workflow.

## Reconstruction and recheck

Front and left-side views fit the hull at the declared scale. The optional top view can recover openings along the vertical axis. Keep all originals square, 40–512 px on a side, with the same scale, center and crop. The top view uses the front edge at the bottom. The separate 45° view must have a different silhouette from the fitting views. A distinct library entry with copied pixels is refused.

The held-out image stays out of grid selection and must pass an 86% overlap check. The stored scene binds the original library records, the retained image package, the visual mesh and the fitted dimensions. Opening or exporting the scene checks the package and original library records. The reconstructed surface and its enclosing box are separate. Openings in the visual surface do not become collision openings.

## Acceptance evidence

Blender 5.3 rendered a fresh 500 × 100 × 400 mm plate with two offset through-openings. The top-assisted fit reached 99.2% against the separate elevated image. A narrower object failed at 24.6%. Both reuse of a fitting entry and a relabelled copy of its pixels were refused.

Blender 4.5.17 LTS measured the exported visual hull at 15.5 L, checked the manifold 496-triangle visual and the separate rigid-body box, and raised the visual cap by 5 mm. Fraynix imported the distinct visual hash and preserved the collider dimensions. The original scene kept its evidence binding after restart and produced the same GLB bytes. Corrupting either retained original refused the checked export.

Edition 230’s Linux job showed a test timing issue before the first PNG input. The tile regression now waits for the input to be enabled, as it already did on editable-level import. The new integration awaits readiness before every PNG input.

## Reproduce

    ./gradlew :workspace-engine:installDist
    node workspace-desktop/test/library-prop-integration.cjs build/visual-library-prop231

Set FRAYNIX_TEST_BLENDER to a local Blender executable to run the actual export, edit and return. Blender runs in the background with automatic script execution disabled. The fixture generator is [library-prop-blender.py](../../workspace-desktop/test/library-prop-blender.py).

The next phase can let a reviewed chat request place a checked prop, run a contact test in a scene, and export it with the placement and collision receipt.

[Edition 231](../../README-231.md) · [Previous phase](../publication230/plan.md)
