# Edition 261 implementation and reproduction

## Ownership and measurements

Java decodes a bounded grayscale wall mask or metric plan JSON into scanline spans. The PNG width sets the cell pitch and the wall height is a separate input. Matching spans merge into editable solids sharing a glTF cube mesh. A base fills the declared plan rectangle. The output records the exact source SHA-256 and the sampling grid. Java immediately cuts the new triangles at mid-height and compares every input cell with the measured section before the desktop activates the model.

Sectioning uses the original retained GLB bytes, the default scene and the node matrices. Java intersects static mesh triangles with a horizontal plane, joins segments whose endpoints agree at one micrometre, and samples closed, nondegenerate contours at the grid cell centres. The cut is evaluated just above coincident faces by one eighth of a micrometre. Open geometry stays in the vector diagram. The plan JSON represents occupied cells with ordered half-open spans on the X/Z plane. Generated GLBs supply their exact original grid on readback. Foreign GLBs use the chosen cell spacing. That sampling limits the resolution of features in a rebuilt model.

SVG and JSON exports reread the current source instead of trusting the preview. A change to the retained GLB blocks the cut and exports. The GLB export copies the exact retained model and verifies the bytes. The desktop supports an offline chat request for the current section.

## Acceptance

- Two synthetic rooms with a measured doorway. All 4,800 original cells agreed, including 528 filled cells. The width is 8 m and wall height is 2.6 m.
- A horizontal visual ray passed through the doorway for 3 m, and a parallel ray hit the wall at 1.3 m.
- Blender 5.3 independently imported the generated GLB and cast a vertical ray at each of the 4,800 cells. Each ray hit the expected wall or floor height.
- A separate GLB with a parent matrix, rotated furniture and an open vertical screen produced closed and open geometry at the expected positions.
- Windows reopened the model offline, reproduced the same SVG, and reconstructed the same plan from the reopened model. Source-byte tampering refused the cut. PNG import without a scale refused to create a model.
- A local render of the actual apartment at 1.2 m created a 361 × 501 grid with 2,893 filled cells and seven open edges. The rebuilt GLB reproduced the entire grid. Blender independently checked 144 sampled wall and floor cells in that rebuilt model. Apartment outputs stayed local.
- The prior Intel Mac runner could not create a WebGL context. Hosted Intel Mac tests now request software rendering, with the new checks scheduled in the portable matrix.

## Reproduce

```powershell
.\gradlew.bat :workspace-engine:installDist
npm --prefix workspace-desktop test
node workspace-desktop/test/environment-integration.cjs build/environment-regression
node workspace-desktop/test/floor-plan-integration.cjs build/floor-plan-test
blender -b --python-exit-code 5 --python workspace-desktop/test/floor-plan-blender.py -- build/floor-plan-test
```

Use `./gradlew` on macOS and Linux. Use `xvfb-run -a` for desktop tests on Linux without a display.

[Edition 260](../../README-260.md) · [Measured sample](../readmes/261/floor-plan-evidence.json)
