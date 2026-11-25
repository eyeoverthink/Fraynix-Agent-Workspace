# Edition 257 implementation and reproduction

Nexus Engine joins three authoring steps that Fraynix can use directly. Scene to Game extracts backgrounds and character cutouts, the grid assigns gameplay roles, and the exporter moves the design between runtimes. The map studio adds palette, opacity and parallax. The character schema keeps controls and animation hints with the image. Fraynix uses these fields as a design input to the existing tile editor.

## Data to game

The importer reads version 2 scene JSON or the scene data embedded in the generated WebAssembly runner. Java checks the grid shape, spawn and goal count, image bounds and sprite bytes. It records the source hash, retains the supported style and playable character fields, and converts the grid into the same assets and colliders used by the editor. A 2D map row addresses a 24-pixel tile in Fraynix. Speed and jump values are bounded by the portable runtime.

The desktop and standalone export use the same fixed-step player and tile rules. The stage keeps authored openings in the terrain. A hazard resets the player, a spring adds a vertical impulse, and the goal finishes the course. Pickups are optional points of interaction on Nexus stages. Classic Fraynix tile levels retain their existing exit rules.

The source runner is read as data. Fraynix's standalone export embeds its checked runtime, retained PNGs and edited level. Style variants can use the same geometry. The editor can display the named style and change spawn and goal coordinates. Exported JSON retains the working layout and assets without requiring a service.

## Acceptance

- The original exporter built a runner from the synthetic Twilight Relay input. Both importer formats reproduced its expected tile layout.
- The desktop tester observed original hero pixels, a real ground contact, one hazard reset, one pickup, one spring boost and reaching the moved goal.
- A separate project opened the exported bundle and regenerated the exact standalone HTML. The isolated player reached the goal with networking blocked.
- The importer refused a missing grid, duplicate spawn and a remote hero image. A second palette retained every gameplay coordinate and collider with a remote backdrop falling back to the palette.
- The existing Tile Workshop play, restart, clean import, collision and locked exit regression passed.

## Reproduce

```powershell
.\gradlew.bat :workspace-engine:installDist
npm --prefix workspace-desktop test
node workspace-desktop/test/nexus-level-integration.cjs build/nexus257
node workspace-desktop/test/media-tile-integration.cjs build/nexus257-regression
```

Use `./gradlew` on macOS and Linux. On Linux without a display run the Electron acceptance checks with `xvfb-run -a`.

A run can also compare a runner produced by the Nexus exporter. Give the path to that runner in `FRAYNIX_NEXUS_RUNNER_FIXTURE`. It should contain the supplied synthetic Twilight Relay design to compare identical decoded scene data.

[Edition 256](../../README-256.md) · [Sample files](../readmes/257/FILE_TREE.md) · [Measured evidence](../readmes/257/nexus-evidence.json)
