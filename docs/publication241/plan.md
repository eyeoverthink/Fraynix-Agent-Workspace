# Edition 241 implementation and reproduction

## Review and replay

The placed prop has a centered box in its local frame and a separate visual triangle mesh expressed in metres. The new review binds the parent and mesh digest, the previous box, the proposed box, and the minimum centered envelope computed from every visual vertex. Each vertex must stay inside the box to within 10 micrometres. The editing command accepts only horizontal box size and limits the change to 0.10 m per axis. The inherited pose, height and world objects remain available for the same collision and aisle checks.

The local verifier rebuilds the proposal from its parent. Existing support, obstruction and walkway rules apply. A fresh approval authorizes two native hit, miss and aisle runs. The evidence exchange repeats the reconstruction and physics for every archived pose or collider edit before saving a local replay. A rehashed substituted GLB must also match the box and mesh of the actual final scene.

## Reproduce

Use the dependencies in the [install guide](../INSTALL.md). These commands use the exact published Signal Bay input.

```powershell
.\gradlew.bat :workspace-engine:test --tests '*PlacementExchangeTest' --tests '*SpatialExchangeTest'
.\gradlew.bat :workspace-engine:installDist
npm --prefix workspace-desktop test
node workspace-desktop/test/recipient-collider-integration.cjs build/collider241 docs/readmes/240/signal-bay-handoff.fraynix-recipient.json
node workspace-desktop/test/placement-exchange-integration.cjs build/collider241-replay build/collider241/signal-bay-collider.fraynix-recipient.json
```

On macOS and Linux use `./gradlew`. On Linux without a display, run the desktop tests through `xvfb-run -a`. Set `FRAYNIX_TEST_BLENDER` to the installed Blender executable to import both GLBs headlessly and measure the mesh, rigid box and other bodies. The app and native replay run offline without Blender installed.

## Retained evidence

The [handoff archive](../readmes/241/signal-bay-collider.fraynix-recipient.json) embeds the exact edition 240 input and the fresh approval. The [verified GLB](../readmes/241/collider-final.glb), [Blender measurements](../readmes/241/blender-collider-proof.json), [edit result](../readmes/241/collider-evidence.json), [replay summary](../readmes/241/exchange-evidence.json), and [complete native receipt](../readmes/241/local-replay.json) record the checked Windows workflow.
