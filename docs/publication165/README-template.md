# Fraynix Agent Workspace — edition 165

2026-11-17 · Verified computation reuse · Complete workflow costs · Architecture renewal

[Previous edition 164](README-164.md) · [Offline guide](docs/readmes/165/index.html) · [All editions](docs/readmes/INDEX.md) · [Acceptance plan](docs/publication165/plan165.md) · [Renewal roadmap](docs/ARCHITECTURE_RENEWAL_PLAN.md)

{{FINDING}}

![Actual application showing checked reuse and workflow costs](docs/readmes/165/workflow-cost-breakdown.png)

The capture comes from the tested application. Its reuse counters describe the current process, so restarts reset them. The existing generic timeline notice about a missing linked game world is separate from the selected and verified recorded-action world; that presentation issue remains.

## What this application is becoming

Fraynix connects observations, simulated worlds, data, browser checks and creation through durable Java tasks. The useful unit is a completed, checked task: bind an input, choose a registered procedure, perform actions, inspect the actual result, recover after interruption and retain a verified procedure. The engine owns state and acceptance; the desktop presents the working surfaces together.

That remains the destination. A growing list of editions is a publication history, not a good permanent module boundary. Edition 165 starts separating those responsibilities by adding capability-named Java components and changing how new evidence is published. It also measures whether removing repeated work helps complete tasks, including interruptions, report creation and skill saving.

The current review found **272 numbered Java source files among 384 engine Java files**. That count does not establish that every class is simultaneously live. Some lines exceed 8,000 characters, including generated report content; GoalRunner contains a 3,807-character line. The tracked tree contains about **17.96 GB**, with **17.89 GB in docs/readmes** and 46,064 gzip files. These are tracked-content sizes, not the compressed Git database or installer size. The longest relative evidence path is 173 characters; an ordinary Windows checkout prefix can exceed legacy path limits. There is no root project license file.

The review also found existing foundations worth preserving: `Agency043.Planner`, a typed schema validator, a catalog of **110 tools**, durable child ownership and independent outcome checks. We should consolidate and expose those foundations rather than build duplicate orchestration. See the [measured inventory](docs/readmes/165/review-inventory.json) and [full phased plan](docs/ARCHITECTURE_RENEWAL_PLAN.md).

## What changed in this edition

**VerifiedComputationCache** retains up to four completed results per cache, each at most 2 MB serialized. Every access calculates a fresh evidence fingerprint before and after. A matching entry avoids repeating the completed computation. A changed fingerprint triggers the original computation; missing, linked, corrupt or changing evidence refuses the request and clears reuse. Returned results are isolated JSON values. There is no timestamp or age-based shortcut, and the cache disappears on restart.

**EvidenceFingerprint** reads original bytes and implementation containers. It checks ordinary, unlinked paths and bounded file reads. It includes the Java runtime identity. In this workflow, the witness covers the source state, calibration reference and actual code for metadata reuse; accepted-history reuse covers persisted and in-memory state, every referenced actual report and native implementation containers. The existing per-frame checks and full recording fingerprints remain separate checks.

**VerifiedHistory** wraps the original complete accepted-history verifier. On a miss it also confirms that persisted and in-memory state agree. On a hit it still reads every current witness. The original native execution, acknowledgement, outcome verification and world acceptance remain independent calls. New actions never inherit a previous action's outcome.

The five native/controller/store/report/verifier classes have unchanged source and byte-identical compiled classes against published edition 164. The recorded parent has a new implementation identity; older proofs are not relabelled. See [native identities](docs/readmes/165/native-identity.json).

The UI shows checked source/history reuses and original computations. The normal cost heading no longer carries an edition suffix. Windows CI enables long paths before checkout. Neither change substitutes for the larger consolidation or archive migration.

## Complete workflows, not a cache-only benchmark

The fixed itinerary preserves one world through eight accepted simulated actions. It alternates a historical camera sequence with two explicitly generated recordings, interleaves the two original Delfys data files, saves four motion-procedure versions and interrupts Java before two actions. Recovery must find the same parent and child. Every data report calculates from its fresh input and is inspected in four actual browser states.

Four edition 165 sessions compare reuse enabled and disabled in alternating order: on-a, off-a, off-b, on-b. Both modes run the new fresh witnesses and the same original checks. Two additional untouched edition 164 sessions bracket them. Each session uses two Java processors and a 768 MiB heap. All six use the same inputs and ten-task itinerary.

Task totals include preparation, execution, requested skill saving and an additional archive verification. Interrupted tasks include restart. Read-only depth-2/4/6/8 history probes are extra work outside task totals. Whole-session totals include probes, diagnostics, startup and navigation. Cold verification is measured separately. Inclusive diagnostic spans overlap and must not be added across Java and browser clocks.

{{PAIR_TABLE}}

{{MEASURED}}

The old-build category comparison shows a tradeoff: across the two ten-task sessions, source-check spans fell from **97.005s to 86.447s**, while accepted-history spans rose from **16.244s to 19.606s**. Fresh history witnesses cost more than the computation they save at these history sizes. That cost is included in the modest 2.5% complete-task improvement; this release does not make every component faster. These inclusive spans overlap. The [baseline category receipt](docs/readmes/165/baseline-diagnostics.json) is derived directly from the downloaded original archive.

These are repeated, exposed local measurements, not a population latency guarantee. One historical camera episode remains one episode; generated sequences and repeated tasks do not become new physical observations. The untouched old build is the practical release comparison. The same-build full-computation mode isolates reuse under the new witnesses but is not the previously shipped app.

The exact comparison retains source coordinates and uncertainty, plans, full native traces, outcomes and actual report observations. Only fresh scene/object/job identifiers and their scene hash are normalized. Data comparisons retain exact CSV inputs, mappings, calculated metrics, observed cells, summaries, SVG points and generated HTML/JavaScript. See [complete costs and exact checks](docs/readmes/165/comparison-evidence.json).

## Validation and retained failures

{{VALIDATION}}

{{ATTEMPTS}}

Publication also retained a recoverable download error: the successfully uploaded draft asset initially supplied a temporary URL that returned 404 after publication. Re-reading the same published asset supplied its final URL; a fresh download then passed every archive and original-member hash. The [publication receipt](docs/readmes/165/evidence-publication.json) records that recovery. No archive bytes were replaced.

Cache tests include cold and disabled behavior, eviction, result isolation, changes between witnesses, same-size/timestamp corruption, changed implementation inventory, original exception propagation, forged native traces and altered actual reports. Archive tests cover exact restoration, deduplication, corrupted archives, forged catalogs, unexpected members, duplicate names and private-image rejection.

Every final app session also changes one actual report without changing its size and restores its timestamp. Both archive verification and the independent history probe must refuse it. Exact restoration must verify again. A cold launch must reverify all ten completed parents without adding a native action. The 198 original fixture files remain unchanged.

![Fresh data summary in the actual app](docs/readmes/165/fresh-data-summary.png)

![Eight accepted revisions and independent history costs](docs/readmes/165/history-eight-costs.png)

## Smaller new publications, retrievable originals

New raw evidence is in one [GitHub release archive](https://github.com/eyeoverthink/Fraynix-Agent-Workspace/releases/tag/evidence-165). Git contains its [manifest and hashes](docs/readmes/165/original-evidence.json), small results, source inventory, screenshots and guides. Identical raw files share one content-addressed member. The actual published asset was downloaded and every member verified before release completion.

The manifest binds the archive hash, size, URL and every original logical filename, byte count and SHA-256. The archive also carries its own matching catalog. Hashes do not replace the original bytes: retain downloaded archives or a second backup when long-term replay matters. A release asset can become unavailable; verification then fails rather than inventing evidence.

From the repository root, download into a new local filename:

```powershell
python scripts/evidence_archive.py download --manifest docs/readmes/165/original-evidence.json --archive build/evidence165-downloaded.zip
python scripts/evidence_archive.py verify --manifest docs/readmes/165/original-evidence.json --archive build/evidence165-downloaded.zip
```

The original archive includes failed attempts, raw final results, test reports, logs, method locks, build receipts, CI observations and the unmodified baseline identity. Private camera pixels, model weights, credentials and personal live projects are excluded. Public receipts can be checked as files; rerunning the private-camera reconstruction also requires the original private project. This is the first external-evidence publication; it does **not** remove the existing 17.89 GB archive or rewrite Git history. A clean small clone is a later migration milestone, not a claim of this release.

## Run and inspect

Use JDK 21 and the repository's Gradle wrapper. Existing desktop dependencies are declared in `workspace-desktop/package.json`.

The retained history is still large. For a current Git for Windows checkout, use `git -c core.longpaths=true clone https://github.com/eyeoverthink/Fraynix-Agent-Workspace.git`. CI now sets that option before checkout. A small, ordinary fresh clone remains an acceptance gate for the later migration, not a completed edition165 result.

```powershell
.\gradlew.bat :workspace-engine:installDist
cd workspace-desktop
npm ci
npm start
```

Use a separate project for experiments. In Spatial, create or select a compatible single-object world, open measured alignment and the recorded-object controls, choose an existing recording and independently labelled endpoint, then prepare and send the chat request. The Java workflow verifies the recorded source, performs the bounded simulated move and checks the report. Add “review before action” to exercise interruption and resumption.

Show workflow costs displays current-process counts and timings. Measure history & reports repeats the original checks independently and does not change accepted state. Reconstruct full recording remains available. For a controlled full-computation comparison, launch with `FRAYNIX_WORKSPACE_EVIDENCE_REUSE=off`; that disables only these new caches, not verification. The existing tracker/controller defaults, action limits and original reconstruction options remain intact.

These tasks use a plane-projected recorded point and an authored upright box. They do not measure mass, height, depth contact or hardware motion. Conditional geometric allowances are not calibrated confidence probabilities. Physical accuracy remains a separate unresolved validation task. This edition makes no model inference, weight training or autonomous self-improvement claim.

## Renewal phases and next acceptance gates

1. **Publication and distribution:** audit asset provenance, replace restricted game content with original or CC0 material, choose a project license with the owner, migrate old evidence with verified retrieval, and rehearse a clean-history cutover in a separate repository. Keep an immutable old snapshot and commit map. No force-push is part of edition 165.
2. **Readable capability modules:** establish a behavior-preserving formatting comparison, inventory active versus replay-only implementations, extract domain handlers and a durable-task kernel, and make policy versions explicit data. Formatting can change debug metadata and identity hashes; bytecode equivalence must be measured, not assumed.
3. **Constrained planning and distribution:** connect the existing planner interface and typed tool catalog to registered procedures, then implement and test an MCP adapter. A model chooses; original verifiers decide. Keep Ollama as the default and add optional providers only through explicit configuration and measured task tests.
4. **Useful learning:** export verified task/plan/result triples with source and policy identities. Compare candidate planners or skills on held-out tasks, including refusals and recovery, before promotion. A successful log entry alone is not a trained model.
5. **Product surface:** guided first run, a coherent Chat/Mission/Evidence flow with expert controls, and a signed Windows installer once signing and asset/license requirements are resolved.

**Next implementation:** audit the distributable assets and rehearse an evidence migration in a disposable clone, then extract one route domain with direct compatibility and recovery tests. Keep remaining witness costs visible while consolidating the engine; do not turn every small timing experiment into another permanent subsystem. The [renewal roadmap](docs/ARCHITECTURE_RENEWAL_PLAN.md) gives concrete acceptance gates and preserves the current app throughout.

## Source and continuity

[Source inventory](docs/readmes/165/file-inventory.json) · [Method lock](docs/EVIDENCE_REUSE_165_LOCK.json) · [Verification](docs/readmes/165/verification.json) · [Obsidian map](docs/obsidian/173-verified-evidence-reuse.canvas) · [Project memory](docs/PROJECT_MEMORY.md) · [Development log](docs/DEVELOPMENT_LOG.md)

The Java target remains `eyeoverthink/Fraynix-Agent-Workspace`. The separate Chandra publication instructions do not relocate this application. Published editions 001–164 and private runtime data remain intact. Edition numbers remain useful evidence labels; the new runtime components deliberately use capability names.
