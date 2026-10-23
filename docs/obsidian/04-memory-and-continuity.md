---
title: Memory and continuity
edition_context: 001
updated: 2026-10-23
tags: [fraynix, system-map]
---

# Memory and continuity

Project state is distinct from model weights. World checkpoints retain versioned entities/components; scene edit receipts support restart-safe undo. Chat stores separate prompts, attachments, replies and transport evidence. The offline kernel persists retrieval memory, journal, routing state and traces.

The journal verifies its defined hash-chain/Merkle consistency and exposes incomplete calls. Capability/path checks are application boundaries. Stronger authentication, adversarial rollback and power-loss claims require their own evidence.

Sources: [checkpoint store](../../fraynix-modules/factory-runtime/src/main/java/fraymus/factory/WorldCheckpointStore.java), [continuity contracts](../CONTINUITY_AND_SKILLS.md), [kernel](../../fraynix-modules/factory-kernel/src/main/java/brain_architecture/kernel/OfflineKernel.java).

Related: [[docs/obsidian/02-world-and-physics|World]], [[docs/obsidian/05-tools-and-jobs|Receipts]], [[docs/obsidian/06-agent-factory-plan|Reusable recipes]], [[docs/obsidian/00-home|Home]].
