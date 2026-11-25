# Edition 245 implementation and reproduction

Java saves the request and exact review before the simulations run. The mission preserves the comparison and records which digest accompanied the explicit approval. It checks the existing durable approval marker and the committed scene. The export uses a deterministic project-local filename and stores its expected digest before publishing the file, so an interrupted or damaged file can be regenerated. The mission receipt remains local while the scene export uses the existing recipient exchange format.

Raised Switchyard was originally authored for edition 234. The existing harness now exports that exact raised layout through the verifier so the mission test starts with a checked bundle independent of Signal Bay. The test uses a fresh project, saves a geometry preview, takes a supported Chat request through comparison, then reopens the mission before and after approval and after export. A separate clean project replays the exported geometry and lineage.

```powershell
.\gradlew.bat :workspace-engine:test --tests '*PlacementExchangeTest' --tests '*SpatialExchangeTest'
.\gradlew.bat :workspace-engine:installDist
npm --prefix workspace-desktop test
node workspace-desktop/test/spatial-level-integration.cjs build/raised-switchyard245
node workspace-desktop/test/recipient-mission-integration.cjs build/mission245 build/raised-switchyard245/raised-switchyard.fraynix-level.json
node workspace-desktop/test/placement-exchange-integration.cjs build/replay245 build/mission245/raised-switchyard-reviewed.fraynix-recipient.json
```

On macOS and Linux use `./gradlew`. Run the desktop tests through `xvfb-run -a` on Linux when a graphical display is unavailable. [Source scene](../readmes/245/raised-switchyard.fraynix-level.json) · [Mission receipt](../readmes/245/collider-mission.json)
