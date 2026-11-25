# Edition 248 implementation and reproduction

Java segments contrasting foreground components, rasterizes the checked meshes at the declared scale, and searches a small pixel neighborhood around each component. It chooses the best one-to-one shape assignment and requires a lead over the runner-up. The second view enters after all identities and positions are fixed. The native checks and marked layout path use the existing edition 247 scene pipeline.

The overhead requires at least 40 pixels per metre and 82% silhouette overlap per prop. The second view requires 83% overlap and at most 2.5 pixels of projected centroid error. Both input PNGs, the source-mesh records, and the measured receipts are bound to the scene. The second camera geometry is declared during capture. Perspective calibration, rotation estimation, occlusion and background removal are outside this evaluation.

```powershell
.\gradlew.bat :workspace-engine:installDist
npm --prefix workspace-desktop test
node workspace-desktop/test/image-assembly-integration.cjs build/image-registration248 registered
node workspace-desktop/test/image-assembly-integration.cjs build/image-marked248
node workspace-desktop/test/media-workshop-integration.cjs build/media-workshop248
blender --background --factory-startup --disable-autoexec --python-exit-code 20 --python workspace-desktop/test/image-registration-fixtures.py -- build/image-registration248-fixtures
blender --background --factory-startup --disable-autoexec --python-exit-code 20 --python workspace-desktop/test/image-assembly-blender.py -- build/image-registration248
```

The Windows tests exercised original media-workshop save, offline play, and restart after scoping its title selector. Edition 247's portable run failed there because the media test's page-wide title selector also matched the new image-scene form. Edition 248 adds both layout modes to the portable checks. On Linux without a display, prefix the desktop commands with `xvfb-run -a`. On macOS and Linux use `./gradlew`.

[Edition 247 portable CI](https://github.com/eyeoverthink/Fraynix-Agent-Workspace/actions/runs/45673841503) · [Measured evidence](../readmes/248/image-assembly-evidence.json)
