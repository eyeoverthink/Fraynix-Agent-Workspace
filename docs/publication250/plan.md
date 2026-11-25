# Edition 250 implementation and reproduction

The overhead view measures X and Z. The fitting-side projection constrains Y and supplies different silhouette evidence for rotation. The search starts at 24° heading intervals and 15° pitch and roll intervals, then refines promising poses to 2° increments around the tested positions. One joint assignment pairs each checked mesh with unique silhouettes in both fitting images. A score gap screens for competing poses during fitting.

The held-out image is read after pose assignment. Java projects the fixed pose and requires at least 83% intersection over union and a center error no larger than 2.5 pixels. It also checks whether rotating at the fixed center would gain more than 2.2 points of overlap with a correction above 8°. This check can refuse the scene. It cannot change the saved pose.

Persistence binds all three PNGs and the checked model reconstructions to the receipt. A warm process can reuse the costly fit while checking the original source bindings. A cold process recomputes the fit. Replay recreates the rotated collider at its saved center and verifies fresh native floor contact and settlement. The saved JSON and GLB retain the starting geometry and full rotations. Independently importing the GLB in Blender checks complete rotations, collider dimensions and visual mesh topology.

```powershell
.\gradlew.bat :workspace-engine:installDist
.\gradlew.bat :workspace-engine:test --tests '*SpatialReconstructionTest' --tests '*SpatialExchangeTest' --tests '*PlacementExchangeTest'
npm --prefix workspace-desktop test
node workspace-desktop/test/image-tilt-integration.cjs build/image-tilt250
node workspace-desktop/test/image-assembly-integration.cjs build/image-heading250 yaw
node workspace-desktop/test/image-assembly-integration.cjs build/image-position250 registered
node workspace-desktop/test/image-assembly-integration.cjs build/image-marked250
blender --background --factory-startup --disable-autoexec --python-exit-code 20 --python workspace-desktop/test/image-tilt-fixtures.py -- build/image-tilt-fixtures
blender --background --factory-startup --disable-autoexec --python-exit-code 20 --python workspace-desktop/test/image-assembly-blender.py -- build/image-tilt250
```

Use `./gradlew` on macOS and Linux. Use `xvfb-run -a` for the Electron test on Linux without a display. The reported measurements use Windows and Blender 4.5.17 LTS. The portable workflow now exercises the same checked PNG fixtures on Windows, macOS and Linux.

A next validation gate is a held-out set with three or more shapes and different camera registration, including partially occluded objects and visually ambiguous poses. Compare complete rotation and translation with authoring references and require those ambiguous scenes to refuse before expanding the input contract.

[Edition 249 CI checked before implementation](https://github.com/eyeoverthink/Fraynix-Agent-Workspace/actions/runs/45680484781) · [Measured evidence](../readmes/250/pose-verification.json) · [Blender source poses](../readmes/250/truth.json)
