# Edition 266 implementation and reproduction

## Avatar to Tile Workshop

Avatar Studio rechecks the retained avatar recipe and sends its full framed idle sprite and walk sheet to the tile library. Java checks the dimensions and visible and transparent pixels in each frame. Both assets and the player entry keep the same avatar reference. Level validation checks the two asset roles and their identity, and includes their verified pixels in exported levels. The request changes the editor draft and the level is committed by the usual Save action.

The renderer selects one of four frames from the recorded clock while the player moves on a supported surface. It draws idle art while stopped or airborne. Player collision, speed, jump, scoring and input use the same simulation. The player can be assigned to both ordinary tile levels and imported Nexus levels.

The standalone file contains the same renderer and its assets. Editable export preserves the recipes and can be imported into an empty project. The app ships the byte-identical previous renderer to reproduce compatible historic pixel evidence and input replays. It continues to reject unknown renderer hashes.

## Acceptance

- The new Windows integration drove the Studio Robot to the exit in the desktop, after restart, and in network-blocked standalone HTML. Each run showed all four walk frames, collected one pickup and scored 600.
- The recorded inputs replayed identically on the original layout and the avatar layout. The test opened the avatar’s idle and moving tick images, accepted the retained renderer and refused a fabricated renderer hash.
- Java refused a wrong atlas asset and a wrong avatar reference. The clean-project import produced the exact standalone file.
- The local Avatar Studio, Nexus continuity, Tile Workshop replay and evidence, unified workspace, and spatial components tests passed. The spatial run did not include a Blender executable.
- The previous hosted run exposed a file-selection race in the avatar test and a browser SVG fixture that could exceed its viewport. The avatar fixture now waits for file inputs to become ready. The SVG fixture fits both viewport dimensions and waits for its measured bounds before issuing the same capture checks. The portable workflow runs the avatar checks early in each platform job.

## Reproduce

Run `./gradlew :workspace-engine:installDist` on macOS/Linux or `gradlew.bat :workspace-engine:installDist` on Windows. Install desktop dependencies with `npm --prefix workspace-desktop ci`. Then run `node workspace-desktop/test/avatar-player-integration.cjs docs/readmes/265/editable-avatar.avatar.json build/avatar-player`. On Linux without a display, prepend `xvfb-run -a`.

[Edition 265](../../README-265.md) · [Standalone sample and evidence](../readmes/266/index.html)
