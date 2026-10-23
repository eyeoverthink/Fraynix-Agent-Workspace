# Sequential README policy

The user requested successive, extensive README editions with interactive navigation, screenshots, Obsidian maps, file trees, plans and forecasts.

## Naming and preservation

Use `README-001.md`, `README-002.md`, and so on at the repository root. Consult `editions.json` and existing files before choosing the next number. Use a matching `docs/readmes/NNN/` directory for its viewer, map data, inventory and verification snapshot.

Once published, keep an edition and its snapshot assets intact. Explain subsequent corrections in the next edition with links to the earlier claim and replacement evidence. Shared operations/architecture documents and the Obsidian working notes may evolve; the numbered snapshots retain the historical view. An explicit user request can revise this convention.

## Required content

- New narrative describing the actual combined system and its creator workflow.
- Dated, real screenshots with contextual captions and full-size links.
- Source ownership, repository tree and data/persistence lifecycle.
- Capability states and test/evidence links with practical limits.
- A staged plan, acceptance gates, forecast assumptions and known unknowns.
- Standard GitHub navigation, Mermaid, an offline HTML companion and linked Obsidian maps.

Keep GitHub, offline HTML and Obsidian navigation distinct and usable. GitHub displays the HTML source; the offline companion supplies its own JavaScript interaction. Opening the repository root as an Obsidian vault resolves map file references without moving the source.

## Publication manifest

`editions.json` uses schema version 1. Each entry records its numeric ID, publication date, title, root Markdown path, snapshot directory, companion path, tag, source baseline and canonical text SHA-256. Canonical text is UTF-8 without BOM with CRLF normalized to LF. This tolerates checkout line-ending changes while detecting content changes.

The manifest hash checks consistency; it does not authenticate an adversarial rewrite of both manifest and content. Git history and edition tags provide the publication context.

## Verification and Git

Run `scripts/Verify-Documentation.ps1` from a complete source checkout. It checks published content hashes, sequential IDs, local Markdown/HTML links, fragment targets, wiki links, Canvas file references/edges and embedded map/inventory consistency. Inspect the interactive viewer in a browser and exercise its controls. Run appropriate source tests when implementation or packaging changes.

Update the index and root README navigation without replacing their previous narrative. Review staged files for private runtime state, model blobs, credentials and generated build artifacts. Commit and push when authorized; use an annotated `readme-NNN` tag for the edition snapshot.
