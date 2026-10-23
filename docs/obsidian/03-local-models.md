---
title: Local models and Dreamer 120B
edition_context: 001
updated: 2026-10-23
tags: [fraynix, system-map]
---

# Local models and Dreamer 120B

The activated model is `eyeoverthink/fraynix-dreamer-ultimate:120b` on loopback port 11435. The reviewed inference recipe reuses the existing 116.8B MXFP4 weights. The account model was pushed and independently checked through public manifest references.

October 23 activation: 260.77 s model loading, 274.23 s complete cold request, 38 frames and exact fresh nonce. A separate low-reasoning warm identity/arithmetic check took 13.45 s. These are measured tasks, not general performance guarantees.

Current saved profile: 4K context, 1,024 output allowance, 12 CPU threads, zero GPU layers, 900 s response-idle allowance and 1,800 s keep-alive. The primary service on 11434 remains separate.

Sources: [setup](../DREAMER_120B_SETUP.md), [activation evidence](../evidence/dreamer120b-activation-2026-10-23.json), [model bridge](../../fraynix-modules/factory-models/src/main/java/fraymus/factory/models/FactoryModelService.java), [published model](https://ollama.com/eyeoverthink/fraynix-dreamer-ultimate:120b).

Related: [[docs/obsidian/01-studio|Studio]], [[docs/obsidian/05-tools-and-jobs|Tool-loop gap]], [[docs/obsidian/07-evidence|Evidence]], [[docs/obsidian/00-home|Home]].
