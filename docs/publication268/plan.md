# Edition 268 implementation and reproduction

## Course design and repair

Chat extracts bounded counts and spacing from plain language. Java checks the brief, generates the draft geometry and binds it to the avatar source. Existing requests and saved missions without a detailed brief keep their original course behavior.

The controller tests a straight walk, then at most 64 combinations of jump timing on a three-hurdle design. If none wins, it retains the attempt that reached the farthest position, using score to break a tie. The run uses the existing 120 Hz renderer. The Java diagnosis uses a stationary terminal state with repeated contact with the same solid object, or the exit position with a required pickup absent from the event record.

A blocked solid can be reduced in size. A missed pickup can move to the closest available grounded position measured in that same failed run. Each step edits one object and creates its own level revision. Java records the original level, the failed run, the object’s before and after geometry, and the next revision. A request can keep the geometry fixed. The mission stops after four repairs or when it cannot justify a supported change.

The final gates recheck the avatar, layout, failed attempts, replayable geometry changes, winning run and exact offline HTML. The export includes repair history with the failed layouts and playtest rows. A modified failure record is refused before geometry is changed. Every revision still has a different level identity.

## Acceptance

- The oversized hurdle stopped the character at x = 181 px. The last 90 ticks named the same obstruction. After restart, verified repair reduced its size from 144 px to 48 px. The final trace and separate offline keyboard run both passed with 900 points.
- The relay reached the locked exit at x = 648 px with a missing elevated token. The repaired position moved from (402, 54) to a grounded (401, 287). The relay collected five tokens and scored 1,000.
- The three-hurdle layout passed with all six pickups and 1,100 points. It needed zero geometry changes.
- A corrupted saved failure record blocked repair. Restoring it allowed the same mission to continue. Reopening the completed mission returned the same export hashes. A fixed layout refused export before and after restart.
- The Windows edition 267 integration also passed again. The new course repair test runs early in the portable CI matrix.

## Reproduce

Run `./gradlew :workspace-engine:installDist` on macOS/Linux or `gradlew.bat :workspace-engine:installDist` on Windows. Install desktop dependencies with `npm --prefix workspace-desktop ci`. Run `node workspace-desktop/test/avatar-course-repair-integration.cjs docs/readmes/265/sample-portrait.png build/avatar-repair` from the repo root. On Linux without a display, prepend `xvfb-run -a`.

[Edition 267](../../README-267.md) · [Offline examples and evidence](../readmes/268/index.html)
