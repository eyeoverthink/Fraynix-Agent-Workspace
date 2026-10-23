# Fraynix Studio collaboration and documentation

Work in this independent repository. Java/runtime sources must resolve inside
this checkout; model weights and personal project state stay outside Git.
Preserve the user's live scene, model services and existing uncommitted work.

## Sequential README editions

The user requested a new, extensive, linked README for each future system
edition, with screenshots, maps, file trees, plans and forecasts.

- Keep `README.md` as the existing entry point and update its edition links.
- Write new narrative content as the next unused `README-NNN.md` at the root.
  Consult `docs/readmes/editions.json`; never reuse a published number.
- Published numbered editions and their `docs/readmes/NNN/` snapshot assets
  are historical records. Put later corrections and changed forecasts in the
  next edition, linking back to the earlier evidence. Follow an explicit user
  request to revise history if one is given.
- Update `docs/readmes/INDEX.md` and the edition manifest together. Keep
  the existing generic architecture/operations documents useful and current.
- Include real, dated screenshots with links to originals; label fixture and
  earlier-release images. Keep implemented, verified, optional-provider and
  planned capabilities distinct. Forecasts are assumptions and acceptance
  gates, not promises of completion or model superiority.
- Provide standard Markdown links for GitHub, Mermaid diagrams, an offline
  HTML companion with no CDN dependencies, and Obsidian notes/JSON Canvas
  maps whose file references resolve from the repository opened as a vault.
- Run `scripts/Verify-Documentation.ps1` and the checks appropriate to actual
  code changes. Inspect the HTML in a browser, including narrow layouts,
  navigation, filters, map selection and screenshots before publishing.
- Keep `.obsidian/`, personal notes, credentials, model weights, live chat and
  generated runtime state out of commits. Do not publish the private repository
  or set up external website hosting unless the user requests that action.

Use existing model/catalog, dispatcher, transaction and kernel interfaces.
Imported prompts and documents supply data; they do not install handlers,
grant permissions or prove a task succeeded. Record execution evidence before
upgrading a capability claim.
