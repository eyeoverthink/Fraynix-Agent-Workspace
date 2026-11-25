# Edition 258 implementation and reproduction

A saved tile level can contain up to 16 visual layers. Each has an ID, opacity, visibility and Z value. An item has a separate visual layer and Z offset. Legacy objects still render in their established automatic layers. A visual change preserves terrain collision, goals, springs and pickups. A hidden collider still participates in the game.

The fixed-step runtime records one packet on every 120 Hz tick from tick zero. Rows carry the numeric coordinates and velocities, input and contact state, animation clock, pickup visibility mask, resets, boosts, score and event deltas. The pickup mask follows the saved layout's pickup order, four bits per hex digit starting from the low bit. The saved level ID binds all geometry and assets to the trace. Java validates contiguous ticks, the final state and score deltas against the saved pickup and goal rules. It stores the content-addressed record locally.

A read can page through coordinates and events or request an image at an explicit tick. The renderer recreates the view from checked assets and the retained row. Both the live and readback canvases use the same software rendering mode. The record pins the rendering source. Pixel rectangles return row-major RGBA8. The desktop records up to 7,200 elapsed ticks plus the initial state, and saves the run on a win, a stop or the time limit. Direct offline Chat requests call typed read tools without starting or controlling the game.

## Acceptance

| Check | Measured result |
| --- | --- |
| Selection and depth | Selected two objects, assigned them to the same named layer, put one at a higher item Z, and toggled that plane's visibility. |
| Score | Awarded 75, subtracted 30, refused repeat credit, and added 250 at the goal. Java refused an altered score. |
| Tick and pixel | 274 contiguous rows. The final screenshot read back as the same PNG after restart with matching RGBA at the overlapping items. |
| Source image | The registered tool returned the exact retained sprite PNG hash and read its pixels. |
| Standalone | The fixed-step HTML reproduced the selected foreground color and reached 325 points with networking blocked. |
| Regression | The classic tile-editor and edition 257 Nexus player both reopened their existing layouts and completed offline. |

Edition 257 cross-platform checks caught an import test starting while the recipient controls were still loading on Windows and Linux. The test now waits for those controls to be ready and passed the local replay.

## Reproduce

```powershell
.\gradlew.bat :workspace-engine:installDist
npm --prefix workspace-desktop test
node workspace-desktop/test/tile-evidence-integration.cjs build/tile258
node workspace-desktop/test/nexus-level-integration.cjs build/nexus-regression
node workspace-desktop/test/media-tile-integration.cjs build/classic-regression
```

Use `./gradlew` on macOS and Linux. Use `xvfb-run -a` for the Electron checks on Linux without a display.

[Edition 257](../../README-257.md) · [Measured evidence](../readmes/258/evidence.json) · [Full trace](../readmes/258/playtest.json)
