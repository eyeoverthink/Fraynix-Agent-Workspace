---
title: World and physics
edition_context: 001
updated: 2026-10-23
tags: [fraynix, system-map]
---

# World and physics

Stable entity/component identities and versioned schemas support snapshots, transactions and exact restoration. SceneKnowledge supplies authored component state plus spatial/runtime metadata. JBox2D validates detached proposals before operator review.

Creation lifecycle: observe -> query -> propose -> validate/simulate -> review -> fingerprint check -> checkpoint -> install -> retain undo evidence.

Sources: [world runtime](../../fraynix-modules/factory-runtime/src/main/java/fraymus/factory/FactoryWorld.java), [scene knowledge](../../src/main/java/factory/SceneKnowledge.java), [simulation](../../src/main/java/factory/SceneSimulation.java), [edit history](../../src/main/java/factory/SceneEditHistory.java).

Related: [[docs/obsidian/01-studio|Studio]], [[docs/obsidian/04-memory-and-continuity|Persistence]], [[docs/obsidian/02-lifecycle.canvas|Lifecycle map]], [[docs/obsidian/00-home|Home]].
