# Edition 251 implementation and reproduction

The custom mode binds the fitting and witness headings and elevations to the receipt. An orthographic camera basis projects each known mesh horizontally and vertically. The fitting-side vertical coordinate resolves scene height together with the X and Z position from overhead. Camera separation uses the angle between their viewing directions. The independent held-out image enters after the pose is frozen and uses the same overlap and position checks as edition 250.

Joint assignment still requires a separate component for each selected model in every image. The custom policy explicitly tests a 180° rotation around each prop's local vertical axis. For a rectangular prism that half-turn can produce the same silhouettes even with a tilted body. A close fit at both orientations blocks saving. The numeric controls affect only the custom mode. Preset receipts replay through their original policy and projection.

The Windows acceptance test loads four independent registered models, checks three in the scene, refuses the four controls, verifies actual Bullet floor contacts, restarts Electron and Java, and checks that the same GLB is exported. Blender verifies every imported visual mesh and all three collider axes. The CI matrix adds the same three-prop acceptance test on Windows, macOS, and Linux.

```powershell
.\gradlew.bat :workspace-engine:installDist
.\gradlew.bat :workspace-engine:test --tests '*SpatialReconstructionTest' --tests '*SpatialExchangeTest' --tests '*PlacementExchangeTest'
npm --prefix workspace-desktop test
node workspace-desktop/test/image-tilt-integration.cjs build/pose-legacy250
node workspace-desktop/test/image-pose-survey-integration.cjs build/pose-survey251
blender --background --factory-startup --disable-autoexec --python-exit-code 20 --python workspace-desktop/test/image-pose-survey-fixtures.py -- build/pose-survey-fixtures
blender --background --factory-startup --disable-autoexec --python-exit-code 20 --python workspace-desktop/test/image-assembly-blender.py -- build/pose-survey251
```

Use `./gradlew` on macOS and Linux. On Linux without a display, run the desktop tests through `xvfb-run -a`. The prior full-pose step passed on all four runners. The separate [release-tag CI run](https://github.com/eyeoverthink/Fraynix-Agent-Workspace/actions/runs/45683081352) reported a chart-follow-up failure on its Apple Silicon job.

The next experiment can use an extra registered fitting angle when one view is partially occluded. Keep an untouched view for verification and require the added angle to improve recovery on new scenes while preserving refusal of ambiguous shapes.

[Edition 250 CI checked first](https://github.com/eyeoverthink/Fraynix-Agent-Workspace/actions/runs/45683081225) · [Measured evidence](../readmes/251/pose-survey.json) · [Blender reference poses](../readmes/251/truth.json)
