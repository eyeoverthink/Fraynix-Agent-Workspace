# Edition 243 implementation and reproduction

## Shared launches and retention

The sweep uses the same approved pair as edition 242. Java checks that the source scenes differ only in the reviewed collider dimensions. The center line keeps the previous launch. Angled and offset launches project the enclosing box along and across the chosen heading. This gives both collider sizes the same upstream start and a common lateral control for that heading. The entire matrix is bounded to five specified cases.

Each collider has two runs of the hit and miss at each approach. The simulation keeps the measured tick-zero starts, velocity and radius, rejects obstructions using the existing protocol, and checks hits, misses and the aisle. The sweep binds each ordered launch to the archive, local replay and both scene identifiers. Restore checks the receipt digest, the ordered bindings and each measured trace before computing the summary again. It keeps a separate file from the single-approach and archive replay evidence.

## Reproduce

Use the dependencies in the [install guide](../INSTALL.md).

```powershell
.\gradlew.bat :workspace-engine:test --tests '*PlacementExchangeTest' --tests '*SpatialExchangeTest'
.\gradlew.bat :workspace-engine:installDist
npm --prefix workspace-desktop test
node workspace-desktop/test/recipient-sweep-integration.cjs build/sweep243 docs/readmes/241/signal-bay-collider.fraynix-recipient.json
```

On macOS and Linux use `./gradlew`. On Linux without a display, run the desktop test under `xvfb-run -a`. Portable CI uses a freshly generated Signal Bay archive. The local publication test uses the exact edition 241 archive. It verifies angular and offset measurements from the actual launch, the center case against the standalone result, tamper recovery and intact archive bytes.

[Signal Bay source](../readmes/241/signal-bay-collider.fraynix-recipient.json) · [Summary](../readmes/243/approach-sweep-evidence.json) · [Full measured receipt](../readmes/243/approach-sweep-receipt.json)
