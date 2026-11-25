# Twilight Relay evidence

- [Download the standalone game](layered-relay.html). Open it locally in a browser.
- [Editable level](layered-relay.json)
- [Complete playtest rows](playtest.json)
- [Verified final PNG](verified-final-frame.png)
- [Measurements](evidence.json)

- `workspace-engine/src/main/java/fraymus/workspace/TileWorkshop.java` — Validated level and asset storage
- `workspace-engine/src/main/java/fraymus/workspace/TilePlayRecords.java` — Content-addressed tick records
- `workspace-desktop/renderer/tile-layer-panel.js` — Layer selection and score editor
- `workspace-desktop/renderer/media-tile-play.js` — Fixed-step play and image reconstruction
- `workspace-desktop/tile-evidence.cjs` — Agent level, tick and pixel reads
- `workspace-desktop/test/tile-evidence-integration.cjs` — Desktop and standalone acceptance

[Edition 258](../../../README-258.md)
