# Durable creation inside the workspace

Edition025 extends the existing shared Java runner with `creation-web-v1`.
See the [edition plan](EDITION_025_PLAN.md), [README025](../README-025.md) and
[previous tracking edition](../README-024.md). The app layout and registered
physics, data, spatial and browser-perception families remain in place.

## User path

Choose an installed completion model, open **Create & verify**, select an
optional project CSV, describe the app and optionally name a retained workflow.
The advanced contract accepts observable click/fill and text/value/count checks.
Leave it empty to have the model declare checks before generating code.
The chat phrase **Create and verify** enters the same runner. A selected artifact
with a non-reserved CSV supplies its source; the dialog makes the binding explicit.
The chat phrase `save the skill as experiment-board` preserves that name.

Java persists the request first. It pins the source version and content hashes,
then freezes checks. Source macros `sourceRows:true` and
`sourceCount:{column,equals}` are computed by Java from original cells. The model
receives metadata and a bounded sample. Java supplies the entire CSV and parsed
JSON as reserved `inputs/source.csv` and `inputs/source.json` after generation.
Generated code reads those files in the existing canvas. The source artifact
is preserved; generated code cannot write the reserved inputs namespace.

## State and effect boundaries

`creation-plan` (optional) → `creation-generate` → `creation-write` →
`creation-test` → `creation-complete`. A failed actual receipt returns to
generation with the same checks and recorded failures. Java decides completion;
model prose is never a success receipt.

The current local generation adapter requests one complete `index.html` string,
with inline CSS/JavaScript, then maps it to Java's validated file contract. Its
native underscore function names follow the existing model adapter. This avoids
ambiguous nested file-map outputs; missing/malformed native calls remain failures.
The Java storage contract supports up to18 generated files, but this adapter
currently submits one document plus Java-supplied source inputs.

The desktop claims private leased effects for local generation and actual
Chromium interactions. Generated pages and model tools cannot claim or acknowledge
effects. The local model name and installed digest must match the saved task.
All writes use stable effect identity; repairs retain one artifact ID with new
versions. Java rechecks source and created-artifact identity before effects,
completion and recovery. An external source/artifact revision interrupts the task.

Receipts contain selectors, actions, expectations and observed values/counts.
Java recomputes assertion outcomes from those observations and verifies that the
steps match the frozen contract. This is a trusted local host boundary, not a
cryptographic proof against a compromised desktop or malicious renderer.

## Limits and recovery

One active shared goal; three drafts total across resumes; 24 behavior steps;
600 seconds per attempt; three explicit resumes. Generated files are at most18
and64K characters total, plus Java-supplied inputs. Selected CSVs use the existing
bounded parser (up to2048 source rows,32 columns and bounded cell sizes) and a100K
character source limit. Contracts are at most6K characters. Existing artifact
storage limits still apply and may reject an otherwise accepted source/task.

Stop cancels the owned model request and goal; stale acknowledgements cannot
write content or save a skill. A restart marks unfinished work interrupted.
**Resume** reconciles saved write intent, tests a saved untested version, or
reuses a passing receipt without repeating completed interactions. It does not
reset the total draft budget. Failure and cancellation remain recorded.

Artifact responses allow data requests only to their own version path; the
canvas also blocks requests outside that prefix. This enables local source reads
without granting generated pages the browser's web access or privileged tools.
Actual scripts are still generated code: behavioral checks cover declared cases,
not all possible behavior or interpretations of a request.

## Retained procedure

Only a completed verified goal can retain a versioned creation procedure.
It stores the request, model, check templates, prerequisites, recovery rules and
source evidence. Reusing a data workflow requires a newly selected CSV. Java
recomputes source-derived expectations and the local model generates a new app;
the real canvas tests it again. This differs from the existing artifact recipe
that copies and retests a known artifact. Neither installs new host handlers or
trains LLM weights. Passing a weak contract does not establish broad intelligence.

## Verification and next gate

`CreationTest` exercises source integrity, failure/revision, restart, cancellation,
model/contract identity, tampering and total budgets. `creation-integration.cjs`
uses deliberately supplied broken/fixed HTML in actual Chromium to isolate the
repair mechanism. `live-creation.cjs` uses the actual installed local model;
its files, source bindings and real interactions are separate evidence.

The first dataset is the published024 recorded prediction trace. Reusing it as
creation content does not constitute a new tracking evaluation or change the
rejected candidate's quality decision. The next task-composition phase needs an
explicit contract per registered family, fresh source binding, cancellation,
restart and changed-condition checks before it can claim reliable end-to-end
work. Physical learning and new visual candidates retain their separate gates.
