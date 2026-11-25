# Edition 252 implementation and reproduction

The new mode fits the overhead and extra side PNGs with the existing full-pose search and unique assignment. The original side PNG is checked on the frozen pose. Java unions the projected prop silhouettes, compares that mask with the observed foreground, and requires **90% intersection over union**. Every prop must retain at least **35%** of its projection outside the overlaps, and the original image must support at least **85%** of that exclusive region. At least one prop must share **3%** of its projection. The conservative exclusive fraction is kept separate from the Blender reference measurement of the rear prop's true hidden fraction.

The held-out view is assessed per prop using the unchanged center, silhouette and rotation checks after the composite check. Neither comparison changes the pose from the two clear fitting images. The receipt binds four image hashes, the six declared camera angles and the original reconstruction sources. Every read revalidates these bindings. Restart triggers a fresh fit and comparison, and replay repeats the native physics measurements.

```powershell
.\gradlew.bat :workspace-engine:installDist
.\gradlew.bat :workspace-engine:test --tests '*SpatialReconstructionTest' --tests '*SpatialExchangeTest' --tests '*PlacementExchangeTest'
npm --prefix workspace-desktop test
node workspace-desktop/test/image-pose-survey-integration.cjs build/pose-legacy251
node workspace-desktop/test/image-occlusion-integration.cjs build/image-occlusion252
blender --background --factory-startup --disable-autoexec --python-exit-code 20 --python workspace-desktop/test/image-occlusion-fixtures.py -- build/image-occlusion-fixtures
blender --background --factory-startup --disable-autoexec --python-exit-code 20 --python workspace-desktop/test/image-assembly-blender.py -- build/image-occlusion252
```

Use `./gradlew` on macOS and Linux. Use `xvfb-run -a` for the Linux desktop test without a display. Windows and Blender produced the published measurements. The portable workflow adds the same controlled desktop test on Windows, Linux and both Mac architectures.

A follow-up study can test several overlapping pairs and vary their depth order. Keep the held-out view independent and measure whether adding another clear angle still helps when the foreground masks are noisier.

[Edition 251 CI checked first](https://github.com/eyeoverthink/Fraynix-Agent-Workspace/actions/runs/45693448313) · [Measured evidence](../readmes/252/occlusion-verification.json) · [Blender reference poses and visibility](../readmes/252/truth.json)
