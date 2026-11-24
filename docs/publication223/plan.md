
# Recover the perpendicular recess

2026-11-24 · Edition 223

Edition 222 took depth from opposite Z faces. This edition adds both faces of X and Y, preserving the previous formats. A Blender Boolean operation adds a side pocket whose opening is visible from +X. The paired controls hold the PNG silhouettes and the two Z depth grids fixed. Adding +X recovers eight more cells of empty space. Supplying the other three faces produces the same mesh on this bracket.

## Registered package

Use fraynix.orthographic-silhouettes-axis-depth-v4 with the same scale, cube and three PNGs as the earlier formats. Supply one to six distinct entries in depthViews. Each entry uses the facing grid plane, integer micrometres and one row-major sample per silhouette cell. A ray goes inward from its named face. Distances fit to interior faces within the existing 500 µm tolerance. Values −1 and −2 represent a background miss and an unobserved ray, respectively.

| Faces | Row increases along | Column increases along |
| --- | --- | --- |
| +X, −X | −Y | −Z |
| +Y, −Y | +Z | +X |
| +Z, −Z | −Y | +X |

Register the actual raster to the table before packaging either sign. Both facing directions for a given axis use the same transverse coordinates. Java reads the corresponding silhouette, validates first hits against the same initial hull, collects free space, and rejects a union that would erase any first hit. The final mesh must still reproduce all three masks exactly at grid resolution.

## Verification

- Blender ray-cast all six input grids. The recess modifies four +X samples and zero samples from the other five faces. Opposed-Z reconstruction matches the edition 222 geometry. The added side view recovers a 100 mm recess, with a 1 L reduction in volume.
- Blender independently imports the exports and uses its triangle BVH to test 196 offset rays per face against the original geometry. It also renders one oblique view from a direction that did not supply a depth grid. These checks cover a registered orthographic bracket on the declared lattice.
- The original and a second model with a 0.125 L sealed cavity produce identical first-hit grids from all six faces. The evidence cannot distinguish their interiors.
- Java and Electron verify the cross-axis contradiction and provenance. The +X witness claims the +Z first hit at (9, 4, 10) as free. Each witness is accepted alone. Combined they are refused without creating a scene. Reordering valid inputs changes the source hash while preserving geometry. After restart, an edited source is refused at export.
- The independent reviewer checked the transforms, contradiction and old proof compatibility. They also identified that three faces in the Blender study never needed to carve. Directed Java witnesses verify removal on each of those faces.

## CI follow-up

Edition 222 reached the correct geometric result on its Intel Mac and Linux tag runners before timing out on an optional viewport rotation. A separate Linux job was rate limited by Maven before preparation completed. The updated integrations assert the app's visible fallback when WebGL is unavailable, then complete the same export, refusal and restart checks. Both ran locally with hardware and software rendering disabled. The preparation job retries a reported HTTP 429 up to twice.

## Reproduce

Run the focused Java tests and build the engine distribution, then launch the desktop integration. Use gradlew.bat on Windows. Set FRAYNIX_TEST_BLENDER to the Blender executable for the independent model comparison. The desktop integration also runs without Blender to exercise source checks, export and restart. Set FRAYNIX_TEST_NO_WEBGL=1 to require that the fallback is exercised.

    ./gradlew :workspace-engine:installDist :workspace-engine:test --tests '*SpatialReconstruction*Test'
    node workspace-desktop/test/spatial-side-depth-integration.cjs build/visual-side223

[Blender capture and verification](../../workspace-desktop/test/spatial-side-depth-blender.py) · [Desktop integration](../../workspace-desktop/test/spatial-side-depth-integration.cjs) · [Recorded measurements](../readmes/223/side-depth-evidence.json)
