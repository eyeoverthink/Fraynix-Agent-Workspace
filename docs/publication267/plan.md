# Edition 267 implementation and reproduction

## Workflow

The chat router recognizes image-to-game requests with one of two course patterns. The default is a walking relay. A request for hurdles or jumping selects a two-hurdle course. The user can supply a quoted title and optionally pause before export. Avatar Studio supplies the selected image, crop and avatar. The workflow uses the existing idle sprite and four walking frames. No model or network service is needed.

Java retains the original request and the awaiting-image, built, verified and exported stages. Each mission is tied to the exact retained avatar and level revision. It stores a draft level without replacing the active Tile Workshop level. Manually opening the result and editing it starts a separate revision.

The desktop walks the relay or tests a bounded set of jump times using the existing 120 Hz simulation. It retains the straight-walk comparison on a hurdle course. A separate local Chromium session opens the exact prospective HTML. Only the tested local file and embedded PNG are allowed to load. The check compares the embedded assets, matches the starting raster and replays the same inputs in the standalone renderer. Java checks the input records, both tick streams, three pickups, hurdle clearing, score, grounded finish and exact HTML hash. Only then can it write the offline HTML, editable level, playtest and evidence. Export checks the hashes again and reuses the same owned paths when resumed.

## Acceptance

- The Windows test started from an empty project and restarted while waiting for the image.
- A second restart recovered the verified mission before export. Changing the retained avatar caused refusal. Restoring the exact bytes allowed the original mission to continue.
- Studio Circuit finished with 800 points, two jumps and three pickups. The straight-walk comparison stopped at the first hurdle.
- The exported level was imported in a clean workspace and rebuilt as byte-identical HTML. Network-blocked Chromium replayed the saved inputs and a separate keyboard play finished at 800 points.
- A second request used an altered portrait and produced a distinct walking relay with zero jumps, three pickups and 800 points.
- The prior Avatar Studio, playable avatar and unified workspace integration checks passed again on Windows. The new integration is included early in the Windows, macOS and Linux portable workflow.

## Reproduce

Run `./gradlew :workspace-engine:installDist` on macOS/Linux or `gradlew.bat :workspace-engine:installDist` on Windows. Install the desktop dependencies with `npm --prefix workspace-desktop ci`. Run `node workspace-desktop/test/avatar-level-mission-integration.cjs docs/readmes/265/sample-portrait.png build/avatar-level-mission` from the repository root. On Linux without a display, prepend `xvfb-run -a`.

[Edition 266](../../README-266.md) · [Offline example and evidence](../readmes/267/index.html)
