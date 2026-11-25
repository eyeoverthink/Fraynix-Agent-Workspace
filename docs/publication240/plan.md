# Edition 240 implementation and reproduction

## Branch and verify

The collision evidence viewer admits the existing authored-level and recipient formats. A forwarded recipient copy requires a complete local replay bound to the exact incoming archive. Java gives the copy a new root based on that archive’s digest and stores the replay with the root. The source’s final scene is the starting point. The earlier edit approvals remain in its attachment, and the new proposal is bound to the local scene and revision by the normal review path.

The editor validates the prop and protected aisle before saving a preview. An exact approval persists independently of execution. The new revision is committed only after matching native runs reproduce the expected hit, lateral miss and walking aisle. Restart can recover a preview, an approval, or a committed result.

Re-export stores the incoming archive byte for byte, its pinned local replay, the new reviews and approvals and the resulting GLB. The reader bounds the number of attachments, checks every archive and replay, reconstructs the resulting scene at each step, and runs the physics stages in order. It generates a new receipt only for a complete replay. The footage and browser reference images used before the original placement remain in their source project.

## Reproduce on Windows

Use the dependencies in the [install guide](../INSTALL.md).

```powershell
.\gradlew.bat :workspace-engine:test --tests '*PlacementExchangeTest' --tests '*SpatialExchangeTest'
.\gradlew.bat :workspace-engine:installDist
npm --prefix workspace-desktop test
node workspace-desktop/test/recipient-handoff-integration.cjs build/handoff240 docs/readmes/239/signal-bay-revision.fraynix-recipient.json
node workspace-desktop/test/placement-exchange-integration.cjs build/handoff240-replay build/handoff240/signal-bay-handoff.fraynix-recipient.json
```

On macOS and Linux use `./gradlew`. For a Linux runner with no display, run the Electron checks through `xvfb-run -a` with software OpenGL when required. The portable workflow exercises a fresh sender, a second project for the local edit, and a separate project for replay.

## Retained evidence

The [published handoff](../readmes/240/signal-bay-handoff.fraynix-recipient.json) starts from the exact [edition 239 archive](../readmes/239/signal-bay-revision.fraynix-recipient.json). The [new local approval results](../readmes/240/handoff-evidence.json) document the original and final pose, restart recovery, and refusal tests. The [third-project summary](../readmes/240/exchange-evidence.json) and [complete native receipt](../readmes/240/local-replay.json) show all four measured stages.
