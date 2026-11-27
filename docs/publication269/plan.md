# Edition 269 implementation and reproduction

## Course geometry and diagnosis

The Java course builder uses the existing stage renderer and physics. A gap is an actual opening in the solid bank tiles. A spike hazard uses the existing collision and reset path. Import and reload check that the declared gap agrees with the retained bank geometry. Original courses still use their existing renderer and portable export structure.

Fraynix tries a straight walk and up to 125 combinations of takeoff times in the 120 Hz simulation. It keeps the failed attempt with the most progress before a reset. Java reads the actual jump, fall and reset ticks. It narrows a diagnosed gap by adding bank terrain at one edge or reduces the single hazard that caused the reset. Both objects remain in the course. Each adjustment creates a new revision and starts a new playtest.

The winning run needs all requested pickups, no resets, an airborne gap crossing followed by contact with the far bank, clean progress over every hazard and a grounded finish. It must agree with a replay of the exact standalone HTML in an independent Chromium session with the network blocked. The saved export includes the unsuccessful runs and the revised level identities.

## Startup frame check

The previous Apple Silicon run reported a mismatch on a course with a pickup close to spawn. The game can collect that pickup while idle before the first offline image is sampled. The audit now compares a recorded zero-input desktop reference for the observed startup phase. If tick zero and the settled idle frame differ, the auditor waits for the settled phase. Decoded pixels must still match exactly. The exported HTML, embedded assets and full input replay still have their exact checks.

## Acceptance

- Bank Rescue preserved a failed gap jump and a subsequent hazard collision as separate repairs after restart. It landed on the far bank, cleared the reduced hazard and scored 900 in the offline keyboard playthrough with zero resets.
- Twin Banks used a fresh layout with two gaps and one hazard. It recorded two far-bank landings, a hazard clearance and 1,100 points. Its offline startup matched with zero changed pixels after the spawn pickup settled.
- The repaired editable level was imported in a clean project and rebuilt as byte-identical standalone HTML. The same mission returned the same output hashes after restart. The locked wide gap refused export before and after restart.
- The edition 267 and 268 Windows integrations passed again. The new traversal test runs early in the portable Windows, macOS and Linux matrix.

## Reproduce

Run `./gradlew :workspace-engine:installDist` on macOS/Linux or `gradlew.bat :workspace-engine:installDist` on Windows. Install desktop dependencies with `npm --prefix workspace-desktop ci`. Run `node workspace-desktop/test/avatar-traversal-integration.cjs docs/readmes/265/sample-portrait.png build/avatar-traversal` from the repository root. On Linux without a display, prepend `xvfb-run -a`.

[Edition 268](../../README-268.md) · [Playable courses and evidence](../readmes/269/index.html)
