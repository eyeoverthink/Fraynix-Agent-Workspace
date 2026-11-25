# Edition 242 implementation and reproduction

## Controlled comparison

The original box is the stage immediately before the most recent approved collider edit in the archive. Java compares the complete source definitions after substituting the reviewed box size. The mesh, pose, body properties, neighbors, and floor must match. It keeps the reviewed walking aisle and leaves the original replay protocol intact.

The shared launch uses an envelope that covers both horizontal boxes and the existing contact-test speed, radius and direction. The two collider sizes each repeat the same hit and lateral miss. Java checks launch coordinates and actual tick-zero velocity, the native collision trials, and the aisle. It requires exact agreement between repeats within the local runtime before saving either result. The receipt binds the archive and existing replay digests, the two scene identifiers, the mesh, the collider sizes and the launch protocol. On read, it checks that binding, verifies the digest, validates the trials, and recomputes the summary.

## Reproduce the Windows result

Use the dependencies in the [install guide](../INSTALL.md). These commands use the [exact edition 241 input](../readmes/241/signal-bay-collider.fraynix-recipient.json).

```powershell
.\gradlew.bat :workspace-engine:test --tests '*PlacementExchangeTest' --tests '*SpatialExchangeTest'
.\gradlew.bat :workspace-engine:installDist
npm --prefix workspace-desktop test
node workspace-desktop/test/recipient-comparison-integration.cjs build/comparison242 docs/readmes/241/signal-bay-collider.fraynix-recipient.json
```

On macOS and Linux use `./gradlew`. On Linux without a display run the desktop test under `xvfb-run -a`. The portable workflow compares a freshly generated archive from the same approved Signal Bay sequence. The desktop integration verifies the explicit run, rejects a missing or wrong local replay, restarts the app, damages the comparison receipt, and recreates the same result. It checks that the archived evidence and its local replay have identical bytes before and after comparison.

## Retained evidence

The [comparison summary](../readmes/242/fixed-approach-evidence.json) includes the two contact timings, actual launch, contact counts and recovery checks. The [full receipt](../readmes/242/fixed-approach-receipt.json) includes both native traces and the bound source identity. The timings apply to this shared launch in simulation.
