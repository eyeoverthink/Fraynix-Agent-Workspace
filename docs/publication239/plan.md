# Edition 239 implementation and reproduction

## Export and admission

- Export only a committed recipient revision verified by the existing local scene reader. Retain the original authored archive bytes, the root’s pinned local replay, and the ordered approvals and measured receipts up to the selected revision.
- Reconstruct the root and every candidate with the existing Java pose review logic. Each edit must match its parent, the reviewed prop and protected aisle, its approval and both stored trials. Check the head identifier and GLB against the resulting scene.
- Replay the original placement and every subsequent edit twice with the native engine. Validate the hit, lateral miss, and aisle for each stage. Save a receipt after the whole lineage passes. On reopen, recheck the archive and all local comparison records.
- Keep the forwarded revision in the evidence viewer. Local edit approval remains scoped to the originating project. The package limit is 1.5 MB and up to 31 committed edits. The original archive keeps its 1.2 MB limit.

## Reproduce

From a checkout with the dependencies in the [install guide](../INSTALL.md), run the Java verifier and rebuild the installed engine.

```powershell
.\gradlew.bat :workspace-engine:test --tests '*PlacementExchangeTest' --tests '*SpatialExchangeTest'
.\gradlew.bat :workspace-engine:installDist
npm --prefix workspace-desktop test
node workspace-desktop/test/recipient-edit-integration.cjs build/recipient-source239
node workspace-desktop/test/placement-exchange-integration.cjs build/recipient-replay239 build/recipient-source239/signal-bay-revision.fraynix-recipient.json
node workspace-desktop/test/placement-exchange-integration.cjs build/recipient-published239 docs/readmes/239/signal-bay-revision.fraynix-recipient.json
```

On Mac and Linux use `./gradlew`. On a Linux runner without a display use `xvfb-run -a` for each desktop test, with `LIBGL_ALWAYS_SOFTWARE=1` when software OpenGL is needed.

The sender test verifies an explicit UI export, exact bytes, sidecar SHA-256 and the staged approval records. The receiver starts in a separate empty project, replays all three stages, and compares the saved receipt after a desktop restart. It refuses changed approvals and GLB bytes, withholds a damaged local receipt, and regenerates it. A Java regression removes the first approval. A separate test changes earlier sampled motion, regenerates the recorded hashes and following review, and asserts that fresh replay still refuses the package and saves no local receipt.

## Published evidence

The [sample](../readmes/239/signal-bay-revision.fraynix-recipient.json) preserves the previously published edition 238 scene ID. The [export receipt](../readmes/239/recipient-export-proof.json) binds the original archive and replay plus the final approval. The [receiver summary](../readmes/239/exchange-evidence.json) and [full local replay](../readmes/239/local-replay.json) show what the second project reproduced on Windows. Portable CI exercises a new sender and a second clean receiver on each runner.
