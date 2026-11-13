# Fraynix Agent Workspace — edition 136

**Keep drift out of retained evidence; spend less time rereading unchanged history.** Updated 2026-11-13.

[Previous: edition135](README-135.md) · [All editions](docs/readmes/INDEX.md) · [Interactive offline guide](docs/readmes/136/index.html) · [Development direction](docs/INTEGRATED_AGENT_ROADMAP.md) · [Contributor continuity](README-CONTRIBUTORS.md)

This remains the local Java agent workspace: durable chat goals, persistent physics worlds, browser and canvas tools, creation workflows, reusable procedures and independently checked outcomes. Reliable perception is needed before an agent can safely reuse what it thinks it saw. This edition improves that boundary and the cost of retrieving evidence; it does not claim that object identity, general learning or physical accuracy are solved.

## What changed

| Addition | Working behavior | Limit |
| --- | --- | --- |
| Original-image anchor | Optional `seed-anchored-136` requires fresh forward and reverse motion checks against the immutable first image before admitting an appearance | Rejects drift, but also many useful examples; remains experimental |
| Independent admission audit | Rechecks original seed, geometry, motion conditions, retained history and external target references | Motion consistency is not semantic identity; labels remain downstream only |
| Content-bound evidence reuse | Reads and SHA-256 hashes every current state byte under the existing lock; exact matches reuse a validated parsed structure | Every changed state, source image and outcome retains its checks; capacity remains 24 MB |
| Detached write cache | Copies before mutation; validates and reparses serialized content; installs the cache only after atomic durable write | More cold-open work; no timestamp shortcut, history pruning or altered proof identities |
| Cost control | **Show evidence-processing cost** displays reads, bytes hashed, structural checks, reuse and durable writes | Current-process counters, not a claim about physical accuracy |
| CI correction | Updates an obsolete exception expectation for unpublished procedures | The lifecycle refusal and unchanged publication state are still tested |

Local checks passed: 281 distinct Java tests, 161 desktop checks, kernel integration, actual agency regression and isolated offline/package preparation. Six actual app runs reproduce all 570 frozen tracker/filter samples. Independent host arithmetic, supported-example export or explicit refusal, stopped-prefix preservation and cold restart pass.

## Actual application

Screenshots are from the isolated application test, with public benchmark excerpts. Private camera/project data was not used.

![Actual application: anchored appearance evidence](docs/readmes/136/box-535-seed-anchored-136.png)

![Original source overlay and measured drift](docs/readmes/136/box-535-source-failure.png)

![Actual application: content-verified evidence costs](docs/readmes/136/evidence-cost136.png)

![Actual application: later rendered target and supported examples](docs/readmes/136/bird1-193-seed-anchored-136.png)

![Actual application at 1600 pixels](docs/readmes/136/appearance-1600.png)

[All captures and checks](docs/readmes/136/app-study.json) · [1100-pixel view](docs/readmes/136/appearance-1100.png)

## Did the memory boundary improve?

On the four known development windows, the previous bank admitted 46 examples: 18 correct and 28 wrong. The anchored bank admitted none, rejecting both groups. On the later four windows, the prior bank admitted 58: 37 correct and 21 wrong. The anchored bank admitted 3, all correct and all rendered; it rejected the remaining 34 correct and 21 wrong examples. Across both cohorts it prevents 49 observed wrong admissions but also loses 52 of 55 correct proposals. That is a useful contamination boundary, not successful general appearance learning.

| Cohort / method | Correct measurements / visible | Missing | Wrong overlaps | Admitted | Correct / wrong admissions |
| --- | ---: | ---: | ---: | ---: | ---: |
| development / legacy-flow | 86 / 327 | 96 | 145 | 0 | 0 / 0 |
| development / appearance-continuity-135 | 86 / 327 | 96 | 145 | 46 | 18 / 28 |
| development / seed-anchored-136 | 86 / 327 | 96 | 145 | 0 | 0 / 0 |
| later / legacy-flow | 189 / 380 | 53 | 138 | 0 | 0 / 0 |
| later / appearance-continuity-135 | 189 / 380 | 53 | 138 | 58 | 37 / 21 |
| later / seed-anchored-136 | 189 / 380 | 53 | 138 | 3 | 3 / 0 |


All 760 tracker/filter outputs per method match the incumbent across development and later windows. Existing accepted outputs are deliberately preserved, including their errors. No bank recovery occurs. The guard is working as an admission restriction, but **has not qualified as useful camera appearance learning**: it retains no new camera examples in these cohorts. Three correct admissions are from rendered Bird1 only. Positive synthetic tests also demonstrate real admission and recovery; the implementation is not an always-reject placeholder.

All three methods still achieve only 189/380 correct visible/partial measurements on the later windows, with 53 missing and 138 wrong overlaps. In the camera subset that is 134/285; the rendered subset is 55/95. No independent distractor boxes are supplied, so zero confirmed switches does not establish switch-free tracking. The existing tracker, filter and forecast defaults stay unchanged.

Fully visible examples are still exportable only after the independent audit supports them. Their descriptors are rebuilt from reference boxes and original pixels. Empty, incorrect, partial or uncertain sets cannot become a pretend successful export. No automatic training, evaluation feedback, LLM weight update or identity recognition was added.

## Evaluation scope and source honesty

The tracker and scorer were frozen at `17fdb4ab`; the method lock was committed at `e7ff6cb4`. Later-window labels were committed before predictions, with exact byte preservation corrected at `9a52ceb9`. No threshold changes followed either comparison. Existing edition135 windows are development evidence. Later temporal sections of the same already source-reviewed recordings are new predictions, **not independent unseen videos**.

| Source | Earlier development window | Later window | Later source event |
| --- | --- | --- | --- |
| Tiger2 |59–154|155–250| Camera; partial leaf cover |
| Box |439–534|535–630| Camera; early partial cover, then visible rotation |
| David3 |57–152|153–248| Camera; partial tree/post cover and return |
| Bird1 |97–192|193–288| Rendered flight; target remains visible |

Each window has one seed image and 95 evaluated originals. All 384 later originals were reviewed source-only before predictions. The later cohort contains 113 partial and 267 fully visible samples, no fully hidden samples. Complete cloud disappearance remains confined to the earlier rendered development window; no new real-camera complete occlusion or out-of-frame return was demonstrated.

Original boxes come from the [OTB benchmark by Yi Wu, Ming-Hsuan Yang and Jongwoo Lim](https://rvlab.snu.ac.kr/research/tracker_benchmark). Visibility is separate AI source review, not independent human adjudication. Source hashes and labels are pinned in the [manifest](docs/publication136/source136.json); the [contract](docs/EDITION_136_CONTRACT.md) and [method lock](docs/OBSERVATION_136_LOCK.json) record the evaluation boundary. Raw video, ZIPs and frame packs remain local; limited attributed screenshots and numeric evidence are published. [Source-note clarification](docs/EDITION_136_LABEL_NOTE.md): frozen later labels retain a generic sentence about the earlier rendered occlusion; the later rows have no hidden/uncertain samples. Original evaluated bytes are preserved.

## Why the new checks matter

```mermaid
flowchart LR
    Source[Immutable first image] --> Anchor[Fresh forward and reverse reconstruction]
    Frames[Current source pixels] --> Flow[Existing Java tracker]
    Flow --> Estimate[Existing observation filter]
    Flow --> Candidate[Appearance proposal]
    Candidate --> Anchor
    Anchor -->|strong direct agreement| Bank[Bounded experimental bank]
    Anchor -->|insufficient evidence| Refusal[Retained refusal and diagnostics]
    Bank --> Audit[Independent source and outcome audit]
    Audit -->|fully visible and supported| Export[Reference-box numeric descriptor export]
```

A chain of small tracking errors can be internally consistent while walking onto the wrong texture. `AppearanceFlow136` requires a direct reconstruction from the original image, plus a reverse reconstruction back to that same seed. Each must use strong optical flow and at least 0.75 box overlap. The original six-example limit, initial seed, quarantine, competition checks and two-image recovery confirmation remain. `AppearanceAudit136` rechecks the anchor and scores proposals using independent labels. The metadata keeps the compatible `appearance135` API key with a new explicit schema; older evidence is never relabelled as a current proof.

This restriction is intentionally separate from the independent curation boundary. Neither motion direction proves identity. Better foreground evidence and controlled reference reuse are still needed to admit useful camera examples without contamination.

## Evidence-processing measurements

| Saved runs | Operation | Baseline median / ms | New median / ms |
| ---: | --- | ---: | ---: |
| 1 | summary | 97.24 | 9.85 |
| 1 | run | 82.04 | 29.96 |
| 1 | dataset | 52.43 | 3.08 |
| 1 | image | 55.09 | 3.25 |
| 3 | summary | 167.98 | 9.84 |
| 3 | run | 130.05 | 15.13 |
| 3 | dataset | 112.19 | 4.70 |
| 3 | image | 124.05 | 4.93 |
| 6 | summary | 408.23 | 20.11 |
| 6 | run | 264.43 | 36.78 |
| 6 | dataset | 257.37 | 10.42 |
| 6 | image | 250.49 | 10.65 |

| Saved runs | Baseline cold open / ms | New cold open / ms |
| ---: | ---: | ---: |
| 1 | 273.32 | 315.53 |
| 3 | 256.97 | 404.28 |
| 6 | 534.55 | 748.38 |


The comparison uses identical first-1/3/6 completed prefixes from the closed public edition135 study, with published135 binaries versus136. Original run/history hashes remain intact. All 36 returned result hashes agree; only the current engine identity is omitted from summary-result comparison, never from saved run proofs. Each operation has three repeats. These are local measurements, not a statistically calibrated universal speedup.

At six runs, repeated summary reads improve from 408.2 ms to 20.1 ms median; complete run reads from 264.4 ms to 36.8 ms. Cold open regresses from 534.5 ms to 748.4 ms because the new write cache is detached and checked. That cost is retained in the report. Reuse still hashes all bytes on every access. Source-image bytes are checked separately on each image access. The baseline sampling profile contains substantial canonical JSON and JSON parsing/serialization work; inclusive samples are not exclusive elapsed time.

Standalone anchored tracking itself costs more: 49.7 seconds for the later four windows versus 17.4 seconds for the prior bank and 11.8 seconds for incumbent. Reconstructing from the original source is additional work. The evidence-read optimization must not be presented as making this experimental tracker faster or establishing real-time camera throughput.

| Actual task | Tracking / ms | Audit plus optional export / ms |
| --- | ---: | ---: |
| box-535 / seed-anchored-136 | 29312.0 | 445.4 |
| box-535 / legacy-flow | 15329.2 | 411.1 |
| box-535 / appearance-continuity-135 | 28743.7 | 3015.7 |
| david3-153 / seed-anchored-136 | 54249.2 | 671.9 |
| tiger2-155 / seed-anchored-136 | 51294.5 | 681.5 |
| bird1-193 / seed-anchored-136 | 76539.5 | 2068.9 |


Actual app runs include UI polling, durable writes and a stop/resume in the first run. Their datasets and history differ, so these totals are not a controlled policy comparison. Full writes and audits still scale with retained history. No verification was removed to obtain lower read timings.

## Try it

1. Start the normal workspace with Java 21 and the existing dependencies. Use a separate project for experiments.
2. In Spatial, load a persistent world and import an observation frame pack. Bind its subject to a world body.
3. Keep `legacy-flow` for normal default behavior. Select `seed-anchored-136` explicitly to inspect the new restriction, or `appearance-continuity-135` to compare the previous bank.
4. Import independent source-bound target/visibility labels in the association panel; they are never passed into tracking.
5. Run, audit and inspect the source overlay, missingness, admissions and refusal reasons. Export supported visible examples only after verification.
6. Click **Show evidence-processing cost** to see work performed by this process. Restart resets counters but retains original evidence.
7. Export the audit or labels; reopen a saved audit after restart to recheck its source and outcomes. Incompatible code proofs remain refused.

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File .\scripts\Start-Agent-Workspace.ps1
```

No new vision framework or service dependency was introduced. Existing Electron/Chromium, Bullet and JSON dependencies remain. This is Java-owned processing logic, not a claim that the whole app has zero dependencies. No LLM call or private webcam session was needed for this study.

## Validation, failures and recovery

- Actual stopped Box task resumes the same source/run, preserves its complete original prefix and reproduces every checked appearance and later prediction.
- External OTB boxes plus source-only AI visibility frozen through actual import controls before each tracking task. Labels never enter tracking. Existing legacy-flow default remains selected initially.
- Six actual Java runs: four candidate windows plus incumbent and prior-bank Box controls. All 570 tracker/filter samples equal frozen evidence; independent host overlap, admission-correctness and distractor arithmetic match. Native world remains unchanged.
- Source overlays, visible/partial states, appearance provenance and independent admission verdicts displayed at 1100/1600 without page overflow. Label and audit exports match their immutable records; exact label reimport preserves identity.
- Any fully visible reference-supported admissions retained/exported from original source pixels. Empty/unsupported sets refused. All retained sets reverified after restart; no feedback into measured tracking.
- Abrupt owned-Java termination and cold restart retain all six original runs, labels, association proofs and untouched native world. Reopening actual saved comparison recomputes the same result.
- Actual cost control reports all-byte hashing and exact-byte structural reuse; full source/outcome checks, defaults and scene unchanged.

Tests include positive synthetic admissions/recovery, duplicate distractors, bounded memory, unchanged incumbent output, altered anchor evidence, same-size/same-time corruption, re-signed broken histories, valid external writers, failed-write isolation, input/output alias isolation, restart and source tampering. The broad regression includes world goals, data/creation workflows, procedure lifecycle, archives and recovery. All source image, code identity, history and independent outcome checks remain.

CI run43398780691 was checked first. It completed 1030 engine tests with one failure and two skipped tests. Its failing test expected an older exception type for an unpublished procedure; the actual lifecycle guard correctly refused it. The corrected test checks `IllegalStateException`, the unpublished reason and unchanged publication state. The whole failing class and broader local regression are rerun. Hosted CI for the new publication is reported separately; local success is not hosted CI success.

Retained attempts: the linked CI failure is preserved as its original XML, status and archive hash; its corrected lifecycle assertion and the complete class pass locally. The first focused136 guard/cache run passed. The malformed initial Git attribute newline was corrected before later predictions and raw label hashes re-staged; label values and method thresholds never changed. No default promotion is claimed. Further app/report issues, if found, are appended here rather than discarded.

Broad regression first attempt caught a test-fixture restart race: the alias test opened a second same-JVM store while its cancelled worker could still hold the original file lock. The corrected test waits for actual completed work, checks the unchanged caller-bound key, closes the first store, then reopens and verifies the durable record. Production locking/integrity code is unchanged. Preserve the first broad report; rerun the corrected cache class only after the broad run finishes.

The corrected cache class passes all6 checks; combined with280 unchanged successful broad checks,281 distinct tests are verified (overlapping cache checks counted once). A source-note clarification records inherited135 prose in frozen later labels about rendered occlusion; later numeric rows remain all visible/partial. No evaluated bytes, labels or thresholds were changed.


[Verification receipt](docs/readmes/136/verification.json) · [Comparison](docs/readmes/136/comparison.json) · [Timing data](docs/readmes/136/cost-comparison.json) · [Original evidence](docs/readmes/136/original-evidence.json) · [Source inventory](docs/readmes/136/file-inventory.json)

## Project continuity and next steps

[Architecture](docs/ARCHITECTURE.md) · [Project memory](docs/PROJECT_MEMORY.md) · [Development log](docs/DEVELOPMENT_LOG.md) · [Implementation map](docs/obsidian/144-anchored-evidence.canvas)

Earlier editions001–135 and their evidence are immutable. Publication remains `eyeoverthink/Fraynix-Agent-Workspace`; Chandra is separate. Private projects, credentials, raw captures and model weights are not committed. Main and the numbered tag are fetched and checked before publication is called complete.

1. **Recover useful camera memory without admitting drift.** Test foreground masks or separately verified reference examples with explicit provenance, held apart from evaluation data. Measure rejected useful examples alongside accepted wrong ones.
2. **Obtain independent complete-occlusion labels.** Human-adjudicated target and distractor boxes, genuinely new camera recordings, clear source visibility and actual out-of-frame returns are still missing.
3. **Reduce cold-open and write cost safely.** Profile immutable segments or bounded write transactions; preserve every original history/source/outcome check and test interruption, external changes and tampering.
4. **Bring reliable observations back to durable goals.** The destination remains one cohesive agent that observes, predicts, creates, acts and verifies. These perception experiments must earn accuracy before increasing authority over the persistent world.
