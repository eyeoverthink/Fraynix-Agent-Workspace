
# Reuse sprite assets in playable tile levels

Edition 230 · 2026-11-24

The tile editor uses a sprite library shared by all tile levels in the project. Each library entry retains the captured or imported source and the resulting transparent sprite hash. A level stores placement and collision data keyed to those source records. Opening or exporting a level rechecks the saved layout, source records and derived sprite pixels.

The scene has a 720 × 336 px canvas, a fixed horizontal floor, and a 24 px terrain grid. Free sprites can be dragged and sized. The backdrop and foreground are visual layers. Pickups are triggers, while terrain tiles and solid props can have an axis-aligned collision rectangle with independent offsets and dimensions. A project library can hold up to 64 sources, with up to six distinct sprites and 160 placements per level.

The same fixed-step browser runtime runs inside Media and in the exported HTML. The editable JSON bundles the sources used by that level for transfer. Edition 229's Single sprite editor and portable format remain available through Media.

## Acceptance evidence

- Two source sprites are shared across two saved levels. The live desktop test used pointer input to drag a free sprite, paint a platform and erase one cell.
- The test changed a 48 px prop to a 36 × 44 px collider with an offset of 8 px horizontally and 4 px vertically. The player stopped at x = 200 px, landed on the prop with the player top at y = 236 px, and landed on the edited first tile at y = 212 px in a separate trial.
- The complete course ran in the desktop, after desktop restart, and from the exported HTML with network requests blocked. The standalone game reset to zero pickups. The second course held its exit closed when the pickup was skipped.
- Reloading the original project reproduced the exact HTML hash. A separate project imported the bundle with both sources; changed storage and bundle bytes were refused. An invalid collider never became the current saved level.

## Reproduce

    ./gradlew :workspace-engine:installDist
    node workspace-desktop/test/media-tile-integration.cjs build/visual-tiles230

Use Xvfb for Electron on a headless Linux machine. The portable workflow runs the integration on Windows, Linux and both Mac architectures.

## Next design phase

Use these saved source records as the bridge into Spatial. The next test should take different registered views of the same library asset, reconstruct an editable 3D prop, and keep the visual mesh and collider separate through an offline Blender round trip. Then a reviewed assistant request can place that same asset into a scene and check a real collision in the exported build.

[Media production roadmap](../publication229/plan.md) · [Edition 230 guide](../../README-230.md)
