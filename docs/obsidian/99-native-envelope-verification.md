# Edition091 — native envelope verification

[[README-091.md|Full edition]] · [[docs/readmes/091/index.html|Offline evidence]] · [[docs/obsidian/98-cost-freshness-and-mixed-tasks.md|Prior edition]]

Digest reuse reduced canonical verification passes by 94.2% and took 40.07 s less across the eight task calls (four paired comparisons). All nine new native goals completed and verified. Every original outcome check remains active.

Only canonical content fingerprints are reused. Every source read hashes all bytes and decodes new JSON. Source identity and all outcomes remain checked. Process-local64-entry bound; cold restart and changed bytes require a full canonical pass. No mutable JSON or success verdict is retained.

268 Java cases passed across 35 classes; 1 host-dependent case skipped; 135 desktop checks passed. A separate Windows directory-junction refusal probe passed. Nine fresh native goals and eight full post-task audits were independently inspected.

Next: longer mixed/restarted/growing-bank and calibrated-policy measurements. Preserve original evidence and every outcome check. This is measured engineering improvement, not model training or physical accuracy.

[[workspace-engine/src/main/java/fraymus/workspace/NativeEnvelope091.java]] → [[workspace-engine/src/main/java/fraymus/workspace/WorldProcedure079.java]] → [[workspace-engine/src/main/java/fraymus/workspace/GoalRunner.java]]
