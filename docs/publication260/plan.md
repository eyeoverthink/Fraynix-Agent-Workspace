# Edition 260 implementation and reproduction

## Environment import

The apartment combines a Blender-authored GLB, a Gaussian-splat scan and separate first-person puzzle behavior. Fraynix’s Environment view uses the Blender export. Java validates the GLB container, embedded assets, geometry accessors, hierarchy and scene references before retaining the original bytes by SHA-256. The renderer decodes textures and matches the scene hierarchy to the catalogue before it activates a retained environment. Opening the current environment or reading it from a tool checks the retained source again.

The view supports orbit, zoom, object selection and an adjustable section. The offline chat command opens the same typed environment reader as the desktop agent tool. Its paginated output supports any node by catalogue index. The visual ray tool reports the nearest hit with object identity, distance and point in glTF metres. The native inventory export records each default-scene node in source order and binds the exact GLB digest. Bounds describe geometry in the selected node’s subtree.

## Acceptance

- The original apartment GLB opened locally with 394 listed nodes, 386 mesh nodes, 14,596 triangles, five embedded images and eight named markers. The selected marker reopened after restart, and the exported inventory contained all 394 entries. The screenshots and copied source stayed in the local review workspace.
- The portable two-room pavilion includes transformed furniture, an embedded image, a marker, a roof and a doorway. Electron verified the texture-backed render, selected the bench and recovered its world position from the parent transform. The export contained all eleven catalogue entries and matched the bench measurement.
- Independent scene geometry left the doorway centre clear for 3 m and put the adjacent partition hit at 1.9 m.
- Desktop restart retained the environment and selection. Offline chat read the environment. Changing bytes on disk refused inventory and ray reads until the exact bytes were restored.
- External image references, cyclic nodes and unsupported required extensions were rejected while the previous environment remained available.

## Reproduce

```powershell
.\gradlew.bat :workspace-engine:installDist
npm --prefix workspace-desktop test
node workspace-desktop/test/environment-integration.cjs build/environment-test
```

Use `./gradlew` on macOS and Linux. Use `xvfb-run -a` for the desktop test on Linux without a display.

## Next design test

Add explicitly selected room and doorway collision geometry, then verify walkable paths and save the evidence against the exact original GLB. The same retained scene can carry reviewed agent placement and Blender export without deriving a collider from every rendering detail.

[Edition 259](../../README-259.md) · [Portable pavilion evidence](../readmes/260/environment-evidence.json)
