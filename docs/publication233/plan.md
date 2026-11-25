
# Replay a collision bundle in a fresh project

2026-11-24 · Edition 233

## Review of edition 232

The release workflow passed on Windows, Linux, Intel Mac and Apple silicon. The original placement reviewed a held-out-verified prop, ran an independent repeat, and kept its approval and authored scene through restart. The bundle included enough scene and trace data to reproduce that collision. The recipient still needed the original project’s approval and source records to run the existing replay, so opening a GLB alone did not independently verify contact.

## Exchange verification

Opening a bundle verifies the review and evidence digests, matching archived repeats, required samples, the dedicated deck geometry and approved pose, and the embedded GLB against the editable scene. The original bytes stay intact. Fraynix retains the recipient’s replay in a separate local record. A changed local record is withheld until a fresh run verifies it.

When the recipient selects **Replay in local physics**, Fraynix reuses the placement contact protocol. Two local hit/miss pairs must agree exactly with each other. Across installations, the archived comparison allows up to one tick in first contact, three ticks in contact duration, 3 mm in first separation, 0.015 m/s in peak prop speed, 10 mm in sampled position and 0.05 m/s in sampled velocity. The lateral control requires zero contacts. The sample comparisons include both bodies and check the archived launch conditions.

The viewer reports archived visual provenance alongside the recipient’s independent physics result. Opening a bundle creates no authored scene or local placement approval. The original library images remain with the source project.

## Measured acceptance

The published Windows edition 232 archive was replayed in a clean project. The original and recipient both reached contact on tick 43, held contact across 59 steps and recorded zero miss contacts. The comparison found zero position and velocity error at every checked sample. Restart restored the verified comparison; a changed local replay was withheld and rebuilt.

The Java test modified an archived hit sample by 0.25 m and recomputed the checksums and repeat. The altered bundle passed structural inspection and failed the local outcome comparison. Missing samples, mismatched edited scene geometry, and a changed embedded GLB checksum were also exercised.

## Reproduce

    ./gradlew :workspace-engine:installDist
    node workspace-desktop/test/placement-exchange-integration.cjs build/placement-exchange233

The next review can evaluate placing a checked prop in a user-authored destination. That step needs destination pose checks, an explicit collision witness, and a replayable edit with its original destination bound.

[Edition 233](../../README-233.md) · [Edition 232 placement contract](../publication232/plan.md)
