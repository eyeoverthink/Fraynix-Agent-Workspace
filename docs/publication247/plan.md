# Edition 247 implementation and reproduction

The local PNG picker sends the selected bytes and basename through the existing registered-view fitter. The separate oblique image checks the mesh after fitting. Java reads the opaque overhead layout and converts marker centers to X/Z metres. It copies the checked visual meshes and enclosing colliders into a flat scene with stable object identities. It refuses overlaps, insufficient clearance and colliders extending outside the floor, then runs two independent 240-tick drop tests before retaining the scene.

The assembly retains the layout digest, exact original image-set references, marker centers, positions and native receipt. Reading or exporting the scene checks the original source bytes and reconstructs the expected assembly definition. A fresh native replay must still agree with saved measurements.

```powershell
.\gradlew.bat :workspace-engine:installDist
npm --prefix workspace-desktop test
node workspace-desktop/test/image-assembly-integration.cjs build/image-assembly247
node workspace-desktop/test/browser-views-integration.cjs build/image-assembly247-regression placement
blender --background --factory-startup --disable-autoexec --python-exit-code 20 --python workspace-desktop/test/image-assembly-blender.py -- build/image-assembly247
```

Use `./gradlew` on macOS and Linux. On Linux without a display, prefix the desktop commands with `xvfb-run -a`. The desktop run here was on Windows. Portable CI now runs the same offline assembly test.

[Previous portable CI](https://github.com/eyeoverthink/Fraynix-Agent-Workspace/actions/runs/45669165068) · [Layout](../readmes/247/overhead.png) · [Desktop test receipt](../readmes/247/image-assembly-evidence.json)
