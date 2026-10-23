---
title: Studio and ownership
edition_context: 001
updated: 2026-10-23
tags: [fraynix, system-map]
---

# Studio and ownership

The editor owns the mutable scene and frame loop. FactorySession captures immutable state for workers, accepts validated changes and retains checkpoints. FraynixWorkspace manages model discovery, chat and profiles off the UI thread.

Current tabs: Chat, Create, Models, Kernel, Tools and Jobs. The original physics playground remains the editable visual environment.

Sources: [Window](../../../../src/main/java/jade/Window.java), [FactorySession](../../../../src/main/java/factory/FactorySession.java), [FraynixWorkspace](../../../../src/main/java/editor/FraynixWorkspace.java).

Related: [[docs/readmes/001/obsidian/02-world-and-physics|World and physics]], [[docs/readmes/001/obsidian/03-local-models|Models]], [[docs/readmes/001/obsidian/05-tools-and-jobs|Tools]], [[docs/readmes/001/obsidian/00-home|Home]].
