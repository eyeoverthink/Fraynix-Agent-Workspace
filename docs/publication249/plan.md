# Edition 249 implementation and reproduction

The new placement mode projects each checked mesh at 12° coarse intervals and refines promising neighborhoods in 2° steps. Position and heading are fitted from the overhead PNG. Assignment needs a unique shape match. For each fitted heading, alternatives at least 24° away must score at least 3.5 points lower. The second image checks projected overlap and center, then independently checks heading at the retained floor position. The two yaw estimates must agree within 6°. Neither estimate from the second image can change the saved pose.

The new receipt version keeps both image hashes, the original model bindings, per-prop pose evidence and the repeat contact results. Rotation updates the collider quaternion. The placement guard uses the rotated footprints' axis-aligned bounds and a 1 cm clearance. Each native trial checks floor contact, settled position and angular drift. Java reconstructs the comparison after restart, and replay runs a new native trial.

```powershell
.\gradlew.bat :workspace-engine:installDist
npm --prefix workspace-desktop test
node workspace-desktop/test/image-assembly-integration.cjs build/image-yaw249 yaw
node workspace-desktop/test/image-assembly-integration.cjs build/image-position249 registered
node workspace-desktop/test/image-assembly-integration.cjs build/image-marked249
blender --background --factory-startup --disable-autoexec --python-exit-code 20 --python workspace-desktop/test/image-yaw-fixtures.py -- build/image-yaw249-fixtures
blender --background --factory-startup --disable-autoexec --python-exit-code 20 --python workspace-desktop/test/image-assembly-blender.py -- build/image-yaw249
```

Use `./gradlew` on macOS and Linux. On Linux without a display, run the desktop commands through `xvfb-run -a`. The published measurements came from the Windows desktop and Blender fixture. Portable CI runs the same desktop acceptance case.

[Edition 248 portable CI](https://github.com/eyeoverthink/Fraynix-Agent-Workspace/actions/runs/45677690877) · [Measured evidence](../readmes/249/image-assembly-evidence.json)
