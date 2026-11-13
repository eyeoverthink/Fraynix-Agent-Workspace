# Fraynix Agent Workspace — Edition 137

[Published and fetched verification receipt](docs/PUBLICATION_137.json) · [Release tag](https://github.com/eyeoverthink/Fraynix-Agent-Workspace/tree/readme-137)

**Verified camera references, protected evidence and cheaper reopening.** This local Java agent workspace connects chat, persistent worlds, physics, browser interaction and creation tools. Edition 137 improves its observation memory and persistence layer without changing the default tracker or increasing action authority.

[Previous edition](README-136.md) · [All editions](docs/readmes/INDEX.md) · [Interactive offline guide](docs/readmes/137/index.html) · [Integrated roadmap](docs/INTEGRATED_AGENT_ROADMAP.md) · [Contributor handoff](README-CONTRIBUTORS.md)

![Verified reference selection in the actual app](docs/readmes/137/references137.png)

## What changed

- **Explicit reference reuse.** Select a retained appearance set that was independently checked against original source pixels and fully visible reference boxes. Up to six examples are selected deterministically from the full immutable set. Earlier evidence remains available.
- **No learning from target predictions.** The reference bank stays frozen throughout the new run. A drifting prediction cannot become a new reference. This prevents that feedback path; it does not eliminate tracking drift.
- **Separated evaluation.** The reference source and target must be distinct single-object datasets with no shared image hashes. An explicit same-object declaration is retained as operator input, not treated as an independent identity certificate.
- **Conservative recovery.** Existing accepted flow measurements remain intact. After loss, reference matching requires a strong, spatially separated candidate and two consistent images. The seed-only control uses the same recovery logic without imported examples.
- **Cheaper durable history.** Opening completed history checks all current bytes and validates it without rewriting it. Running tasks are still marked interrupted. Transaction copying avoids a redundant JSON serialization round trip while retaining full validation, checksums, serialized reparse, forced atomic writes and detached cache publication.

Both `verified-memory-137` and `seed-only-137` are explicit experimental choices. `legacy-flow` remains the default. There is no model-weight training, biometric person identification, new network dependency or physical-device actuation in this edition.

## Measured results

The current-code curation retained 16 independently supported examples, including 11 camera examples. They remain frozen; no evaluation prediction was admitted. Verified-reference tracking produced 189/380 correct visible/partial measurements, versus 189/380 for seed-only and 189/380 for the default. Imported references produced 0 recovery events. Retention and provenance are operational; general camera recovery remains unqualified and no default promotion is made.

| Method / window | Correct / visible or partial | Missing | Wrong overlaps | Imported recoveries |
| --- | ---: | ---: | ---: | ---: |
| legacy-flow / tiger2-155 | 25 / 95 | 0 | 70 | 0 |
| seed-only-137 / tiger2-155 | 25 / 95 | 0 | 70 | 0 |
| verified-memory-137 / tiger2-155 | 25 / 95 | 0 | 70 | 0 |
| legacy-flow / box-535 | 75 / 95 | 0 | 20 | 0 |
| seed-only-137 / box-535 | 75 / 95 | 0 | 20 | 0 |
| verified-memory-137 / box-535 | 75 / 95 | 0 | 20 | 0 |
| legacy-flow / david3-153 | 34 / 95 | 13 | 48 | 0 |
| seed-only-137 / david3-153 | 34 / 95 | 13 | 48 | 0 |
| verified-memory-137 / david3-153 | 34 / 95 | 13 | 48 | 0 |
| legacy-flow / bird1-193 | 55 / 95 | 40 | 0 | 0 |
| seed-only-137 / bird1-193 | 55 / 95 | 40 | 0 | 0 |
| verified-memory-137 / bird1-193 | 55 / 95 | 40 | 0 | 0 |


| Reference source | Retained fully visible examples | Excluded proposals |
| --- | ---: | ---: |
| tiger2-59 | 3 | 2 |
| box-439 | 2 | 6 |
| david3-57 | 6 | 22 |
| bird1-97 | 5 | 0 |


These are earlier edition-135 source windows and later edition-136 evaluation windows from the same [OTB benchmark](https://rvlab.snu.ac.kr/research/tracker_benchmark) recordings. They have already been inspected in prior development. **They are not new unseen footage.** Tiger2, Box and David3 are camera recordings; Bird1 is rendered. Original benchmark boxes and source-only AI visibility annotations are retained. There is no independent human visibility adjudication or complete distractor annotation. The inherited generic annotation sentence is explained in [the preserved label note](docs/EDITION_136_LABEL_NOTE.md); per-frame labels determine the actual visibility counts. Later windows contain visible/partial targets, not a new complete-occlusion test.

The synthetic pose/return control proves that the new reference path can change a real recovery outcome; the seed-only control cannot recover that changed pose. An identical-twin control refuses the ambiguous match. These tests establish software behavior, not general camera accuracy.

![Independent outcome comparison on original camera pixels](docs/readmes/137/box-535-source-overlay.png)

## Startup and write costs

| Saved runs | Cold open, before → after / ms | Median write, before → after / ms |
| ---: | ---: | ---: |
| 1 | 360.7 → 152.8 | 161.9 → 124.6 |
| 3 | 359.9 → 140.5 | 288.3 → 240.4 |
| 6 | 751.0 → 235.9 | 798.6 → 592.6 |

| Saved runs / read | Before median / ms | After median / ms |
| --- | ---: | ---: |
| 1 / summary | 9.00 | 10.98 |
| 1 / run | 29.88 | 40.67 |
| 1 / dataset | 2.89 | 3.09 |
| 1 / image | 3.26 | 3.19 |
| 3 / summary | 9.46 | 10.26 |
| 3 / run | 14.46 | 14.09 |
| 3 / dataset | 4.61 | 4.48 |
| 3 / image | 4.67 | 4.66 |
| 6 / summary | 14.83 | 16.65 |
| 6 / run | 34.24 | 36.19 |
| 6 / dataset | 9.22 | 10.91 |
| 6 / image | 9.56 | 10.95 |


Published edition-136 and edition-137 binaries used identical isolated copies of the same 1/3/6-run public history on the same D drive. All **36 read-result hashes** and **9 write-result/persisted-payload pairs** match. Writes modify a deterministic reason field in the isolated study; they measure persistence transactions, not full agent-task throughput. Each operation has three repeats; cold open has one measurement per size. Some reads regress, and no general speed or live-camera throughput claim follows.

Every access still reads and hashes the complete state. Only exact current byte identity may reuse previously validated structure. Changed bytes with identical size/time, forged history, external writes, source modification and failed writes remain checked. State limits remain 24 MB, 16 datasets, 24 observation runs and 2,048 world revisions. No pruning or capacity increase hides the costs.

![Actual evidence-processing counters](docs/readmes/137/evidence-cost137.png)

## Try it

For a separate review project on Windows, install JDK 21 and Node.js LTS, then run from this checkout:

```powershell
$env:FRAYNIX_WORKSPACE_PROJECT = Join-Path $env:LOCALAPPDATA 'Fraynix-Review-137'
.\scripts\Start-Agent-Workspace.ps1
```

The launcher checks dependencies and existing Java owners. First preparation may download packages; after preparation, use `-Offline` to require cached dependencies. Keep your existing project path unchanged and stop its Java owner through its app before rebuilding that owner's installation.

1. Follow [the Windows launcher](scripts/Start-Agent-Workspace.ps1) with Java 21 and the existing desktop dependencies. Use a separate project directory for evaluation; preserve your current private project.
2. Open **Spatial → Real footage · observed world**. Load a saved world and import source footage with an initial object rectangle.
3. Use the independent association-label workflow to import original reference boxes and visibility annotations. Run an appearance method, audit it, and retain a supported appearance set. Incorrect, partial and uncertain proposals cannot enter this curated set.
4. Import separate target footage. Select **Experimental 137 · verified reference memory**. Open **Verified references**, list retained sets, select one and explain why it depicts the same object. Older code-incompatible evidence remains historical and is refused for current execution.
5. Confirm the world and footage, then start tracking. Compare with **Control 137 · original seed only** and the existing flow tracker. Target outcome labels stay downstream of tracking.
6. Inspect original frames and independent outcomes. Stop/resume preserves the exact run prefix. Export includes the reference set, original audit, labels, source run and donor pixels within existing bounds. The desktop file limit is 20 MB; Java also retains its 40 MB original-pixel and 64 MB raw-package bounds. If the complete export exceeds a limit, preserve/archive the project rather than discarding evidence.
7. Reopen the project. Completed observation history retains its original bytes and modification time. Original source/code/outcome checks run again when opening an audit or reusing references.

The desktop is presentation and interaction; Java owns observations, execution, persistence, validation and recovery. The Game Lab and 3D world remain connected parts of that system, not replacements for the browser, canvas or creation workflows.

## Evidence path

```mermaid
flowchart LR
  A[Original source images] --> B[Independent boxes and visibility]
  B --> C[Audited supported appearances]
  C --> D[Immutable reference set]
  D --> E[Separated target run]
  E --> F[Flow plus bounded recovery]
  F --> G[Persistent observed world]
  G --> H[Independent outcome audit]
  H --> I[Replay and complete export]
```

The target run has no edge back into its own reference set. Reuse checks source pixels, annotations, original proposal, audit, implementation identity and selected examples. Prediction confidence alone is never permission to retain an appearance.

## Validation and limits

Local checks passed: 289 distinct Java tests, 161 desktop checks, kernel integration, actual agency regression and isolated offline/package preparation. Four source runs plus twelve evaluation runs exercise 1,520 source samples. A separate two-run synthetic control verifies a complete desktop export within 20 MB. All 380 incumbent outputs match published edition-136 original traces; independent host overlap arithmetic, source curation, stopped-prefix preservation, export and cold restart pass.

- Four current-code source runs independently audited; only fully visible correct proposals curated from original reference-box pixels. All source labels and original bytes fixed before evaluation.
- Twelve separated evaluation runs (three methods × four windows), 1140 original frame outcomes independently scored. No automatic admissions; candidate/control share the same frozen source references. Default unchanged.
- Actual reference-selection controls, same-target declaration, stopped prefix/resume, complete Java reference bundle including donor pixels and explicit desktop export-size refusal, and four independent overlays exercised. Native world unchanged.
- Owned-process crash and cold reopen preserve exact completed-state bytes and timestamp with zero observation writes. All16 runs, labels, source examples and outcomes reverified; changed donor pixels refuse audit/export, restored originals reverify.
- Separate actual-app synthetic control: two runs/16 samples, complete small export with all13 donor images, reference labels/audit/source run and unchanged native world.

The first compile attempt caught a malformed class-resource expression introduced while extending audit identity hashing; it was corrected before camera evaluation. The next 11 persistence/provenance/cache checks passed, followed by all three frozen-reference tracker controls. Original logs and results remain archived. A UTF-8 label encoding issue and an incorrect source-inspection route in the app test were corrected during preparation, before evaluation. The first cost comparison is retained alongside the final-build comparison; no failed camera result is removed or tuned away.

The first actual-app attempt completed ten runs, then correctly refused a complete export above the existing desktop20MB limit. The test had expected an export within that bound. Original result/log/screenshot retained. The same project and ten immutable runs were reopened and reverified; only remaining runs continue. Java bundle contents are checked separately, desktop refusal retained, and a small complete export is tested independently. No tracker/scorer, default or capacity changed.


Hosted CI is reported separately from local checks and fetched Git publication. Original attempts, full run traces and numerical comparisons are preserved in [original evidence](docs/readmes/137/original-evidence.json), [validation](docs/readmes/137/verification.json), [comparison](docs/readmes/137/comparison.json) and [cost measurements](docs/readmes/137/cost-comparison.json). Screenshots are actual application captures using public evaluation pixels.

## Complete task costs

| Actual task | Track / ms | Audit and optional retain / ms |
| --- | ---: | ---: |
| tiger2-59 / appearance-continuity-135 | 20679.1 | 600.2 |
| box-439 / appearance-continuity-135 | 19534.4 | 539.1 |
| david3-57 / appearance-continuity-135 | 31411.0 | 881.7 |
| bird1-97 / appearance-continuity-135 | 54696.1 | 687.5 |
| tiger2-155 / legacy-flow | 50557.8 | 261.5 |
| tiger2-155 / seed-only-137 | 58097.8 | 844.4 |
| tiger2-155 / verified-memory-137 | 65095.5 | 879.1 |
| box-535 / legacy-flow | 71412.6 | 254.1 |
| box-535 / seed-only-137 | 78707.4 | 799.5 |
| box-535 / verified-memory-137 | 89689.8 | 797.5 |
| david3-153 / legacy-flow | 88960.3 | 373.7 |
| david3-153 / seed-only-137 | 96746.0 | 1303.0 |
| david3-153 / verified-memory-137 | 104084.2 | 1380.3 |
| bird1-193 / legacy-flow | 108657.1 | 325.5 |
| bird1-193 / seed-only-137 | 116369.4 | 1116.3 |
| bird1-193 / verified-memory-137 | 123293.8 | 1216.3 |


Tasks differ in source content, method and retained history size. These timings describe the observed workflow; they do not isolate a single cause or establish constant-time scaling.

## Next steps and the larger goal

1. Evaluate genuinely new camera occlusions and returns with independently adjudicated foreground, target and distractor labels. Use the outcome to decide whether any experimental method earns promotion.
2. Improve the evidence used to distinguish the object from its background and similar objects. A useful reference must improve recovery without worsening wrong-object measurements; retaining more descriptors alone is insufficient.
3. Profile complete observation tasks as the history grows. Consider separately stored immutable frame evidence to reduce repeated serialization, while retaining every source, chain, recovery and outcome check.
4. Connect only qualified observations to durable chat-driven world and creation goals, preserving source age, uncertainty, explicit action validation and independent success checks.

The destination remains one local agent system that can observe, predict, act, create, verify and remember useful procedures across sessions. Better measurements and lower evidence costs support that goal. They are not evidence of consciousness, unrestricted autonomy or general intelligence.

Raw benchmark footage is not committed. The source manifest records the original local execution paths and exact pack hashes; original public archive locations and hashes remain in [the source manifest from edition 135](docs/publication135/source135.json). Normal app use accepts your own footage.

## Source and handoff

[Reference validation](workspace-engine/src/main/java/fraymus/workspace/ReferenceMemory137.java) · [Frozen-reference tracking](workspace-engine/src/main/java/fraymus/workspace/VerifiedMemoryFlow137.java) · [Recovery audit](workspace-engine/src/main/java/fraymus/workspace/ReferenceAudit137.java) · [Durable observations](workspace-engine/src/main/java/fraymus/workspace/WorldObservationStore.java) · [Actual-app test](workspace-desktop/test/reference137-integration.cjs) · [Method lock](docs/OBSERVATION_137_LOCK.json) · [Source manifest](docs/publication137/source137.json) · [Full source inventory](docs/readmes/137/file-inventory.json) · [Project memory](docs/PROJECT_MEMORY.md)

Do not replace historical proof identities, modify frozen editions, silently change defaults, write against a private project during tests, or claim a successful publication before fetching and checking the remote commit. See [AGENTS.md](AGENTS.md) and [the edition contract](docs/EDITION_137_CONTRACT.md).
