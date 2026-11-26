# Edition 259 implementation and reproduction

Live play and deterministic input replay now share one fixed-step simulation. The record includes the requested direction and jump alongside observed direction, collision state, position and score. Comparative replay binds the saved level and runtime identity. It compares the complete unchanged rerun against the source before running the variant.

One jump can be omitted or moved up to 30 ticks to an unused jump tick. The variant keeps the original sequence length and direction inputs. A win can end it sooner. The receipt uses the measured first outcome difference and both final states. Java validates the exact allowed edit across the common rows and checks scoring against the level. The variant and receipt have their own IDs, and the original remains the latest recorded playtest. The saved record supplies the same window the human originally played.

The archived edition 258 fixture verified numeric and pixel access using the existing raster code. Its rows lack requested direction at reset, so comparison asks for a new recording. The raster compatibility check requires the exact archived drawing implementation.

## Acceptance

- The app recorded and repeated 273 rows at 120 Hz. The omission and shift changed exactly their permitted jump inputs.
- The original won with 295 points. The omission recorded 75 points and 3 resets within the same inputs. The delayed jump recorded 295 points.
- Java refused an incorrect description of the edited input. An exact repeat reused its receipt ID, and the result reopened after desktop restart.
- Offline chat reran the omission, and both original and variant PNGs decoded to distinct images at the first changed tick.
- The standalone playable HTML preserved score and layers. The classic editor and Nexus import regression passed.
- Hosted edition 258 jobs failed because the fixture lived under the excluded historical documentation tree. The test now takes its inputs from the portable test tree, including the historical record needed for the compatibility check.

## Reproduce

```powershell
.\gradlew.bat :workspace-engine:installDist
npm --prefix workspace-desktop test
node workspace-desktop/test/tile-evidence-integration.cjs build/tile259
node workspace-desktop/test/nexus-level-integration.cjs build/nexus-regression
node workspace-desktop/test/media-tile-integration.cjs build/classic-regression
```

Use `./gradlew` on macOS and Linux. Use `xvfb-run -a` for desktop tests on Linux without a display.

[Edition 258](../../README-258.md) · [Comparison](../readmes/259/jump-comparison.json) · [One-tick delay](../readmes/259/shifted-comparison.json)
