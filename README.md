# Fraynix Agent Workspace — edition 146

[Published and fetched verification receipt](docs/PUBLICATION_146.json) · [Release tag](https://github.com/eyeoverthink/Fraynix-Agent-Workspace/tree/readme-146)

**A returning appearance needs motion evidence even when no competitor warning was recorded.** This optional Java check closes the path that previously allowed a distant copy to return on appearance alone. It reuses the existing measured trajectory, keeps the original decisions visible, and independently scores both wrong returns prevented and correct returns refused.

**Exposed regression:** the app reduces accepted measurements on the hidden copy from 1 to 0. It newly withholds 12 incorrect raw proposals without a competitor warning. The separate legitimate regression retains 6 correct supplemental measurements while newly withholding 2 correct raw proposals. This is development evidence, not fresh qualification.

**Real-camera recovery remains unqualified.** The four predeclared sources yielded zero eligible event/reference pairs. Coke has a complete disappearance and return, but its all-partial donor supplies no admissible unobscured examples;145/146 comparisons are unavailable. Its existing139 baseline produced 8/30 correct visible measurements, 22 missing, 0 wrong overlaps and 0/9 hidden-frame outputs. This is a baseline-only diagnostic, not evidence for146.

**Generated cases are diagnostics only:** three evaluation images duplicate donor images in each case. The app refused every reference-dependent start. The frozen generator, refusals and baseline-only outcomes are retained; these cases do not count as fresh qualification.

Unavailable reference-dependent comparisons: 15. Defaults remain unchanged and the candidate is not promoted. Generated overlapping-source diagnostics and the identical-pixel limit are listed separately below; neither is camera qualification.

[Previous edition](README-145.md) · [Offline interactive guide](docs/readmes/146/index.html) · [All editions](docs/readmes/INDEX.md) · [Contributor handoff](README-CONTRIBUTORS.md) · [Architecture](docs/ARCHITECTURE.md) · [Integrated roadmap](docs/INTEGRATED_AGENT_ROADMAP.md)

![Actual app: exposed copy regression with return continuity](docs/readmes/146/regression-copy-remains-12-review-drift.png)

Already-exposed development illustration; not fresh qualification. See all outcomes below.

## The connected application

Fraynix is a local Java agent workspace with durable chat goals, a persistent physics world, Game Lab, browser observations, data analysis and verified creation procedures. Java owns execution, state, recovery and outcome checks; the desktop provides the browser, canvas and controls. Local models choose and explain actions through installed tools. The destination remains one observable system that creates, tests and retains useful procedures from measured outcomes.

This edition strengthens one source of evidence for that system. It does not establish semantic identity or physical accuracy. A goal must distinguish a measurement from a prediction and unknown identity before acting. Better tracking must earn its place through complete world, browser and creation tasks. Defaults, permissions, private project data, existing capacities and the `legacy-flow` policy remain unchanged.

## What changed

The edition 145 ambiguity guard could remain inactive when no strong competing appearance was detected. A later confirmed appearance could then be accepted without checking its distance from the target's last observed path. The exposed copy-remains case demonstrated this gap.

`return-guard-146` wraps the unchanged edition 145 method. It checks a supplemental return only when the parent accepted it and no competing-object warning is retained. The candidate must fit the existing measured target envelope and size bounds. Missing, expired or incompatible motion evidence withholds the return. Existing baseline tracking and edition 145 restorations with separated competing trajectories remain intact. `return-monitor-146` records the same checks while preserving every edition 145 tracker and filter output.

No extra image search is introduced. The target still uses at most five adjacent baseline boxes, requires four observations, and has a twelve-frame horizon. The inherited image-plane envelope and size limits are unchanged. Predictions and recovered boxes never extend the target anchor or become saved appearances. The original parent decision stays in the trace alongside the new screening result.

These fixed motion bounds are uncalibrated assumptions. A wrong baseline can contaminate the predicted path. An unseen identical copy following that same path may be indistinguishable. Camera movement, abrupt direction changes and long disappearances can also defeat the assumption. A refusal can prevent a wrong object or lose a legitimate return; both outcomes are measured.

```mermaid
flowchart LR
  P[Original source pixels] --> B[Unchanged baseline and appearance confirmations]
  B --> T[Existing target and rival trajectories]
  T --> G{Accepted return without warning?}
  G -->|Yes| C{Valid pre-loss path and size?}
  C -->|Yes| A[Keep parent proposal]
  C -->|No| U[Withhold; identity unresolved]
  G -->|No| A
  A --> V[Original-pixel replay and independent outcome check]
  U --> V
  V --> E[Persist evidence, refusal costs and replay]
```

## Results, including refusal costs

| Domain / source / method | Correct / visible | Missing | Wrong overlap | Hidden outputs | Other-object matches |
| --- | ---: | ---: | ---: | ---: | ---: |
| real-camera / coke-245 / foreground-guard-139 | 8/30 | 22 | 0 | 0/9 | 0 |
| generated-overlap-diagnostic / short-return / foreground-guard-139 | 6/29 | 23 | 0 | 0/2 | 0 |
| generated-overlap-diagnostic / distant-copy / foreground-guard-139 | 6/9 | 3 | 0 | 0/22 | 0 |
| generated-overlap-diagnostic / same-path-copy / foreground-guard-139 | 6/9 | 3 | 0 | 0/22 | 0 |
| generated-overlap-diagnostic / delayed-return / foreground-guard-139 | 6/17 | 11 | 0 | 0/14 | 0 |
| exposed-development / trajectory-return / trajectory-guard-145 | 14/21 | 7 | 0 | 0/2 | 0 |
| exposed-development / trajectory-return / return-monitor-146 | 14/21 | 7 | 0 | 0/2 | 0 |
| exposed-development / trajectory-return / return-guard-146 | 12/21 | 9 | 0 | 0/2 | 0 |
| exposed-development / copy-remains / trajectory-guard-145 | 7/9 | 2 | 0 | 1/14 | 1 |
| exposed-development / copy-remains / return-monitor-146 | 7/9 | 2 | 0 | 1/14 | 1 |
| exposed-development / copy-remains / return-guard-146 | 7/9 | 2 | 0 | 0/14 | 0 |


Correct means a filter-accepted measurement overlaps an independently labelled visible target by at least 0.5 IoU. Hidden outputs are accepted measurements while the target is labelled completely occluded or out of view. Wrong overlap alone does not establish an identity switch; overlaps with explicitly labelled competing objects are reported separately. Missing observations remain in every denominator. The [full comparison](docs/readmes/146/comparison.json) retains every method, source and unavailable comparison.

| Source / method | New146 refused correct / incorrect / unknown | All refused correct / incorrect | Filtered additional correct / incorrect |
| --- | ---: | ---: | ---: |
| coke / coke-245 / foreground-guard-139 | 0 / 0 / 0 | 0 / 0 | 0 / 0 |
| generated / short-return / foreground-guard-139 | 0 / 0 / 0 | 0 / 0 | 0 / 0 |
| generated / distant-copy / foreground-guard-139 | 0 / 0 / 0 | 0 / 0 | 0 / 0 |
| generated / same-path-copy / foreground-guard-139 | 0 / 0 / 0 | 0 / 0 | 0 / 0 |
| generated / delayed-return / foreground-guard-139 | 0 / 0 / 0 | 0 / 0 | 0 / 0 |
| regression / trajectory-return / trajectory-guard-145 | 0 / 0 / 0 | 0 / 0 | 8 / 0 |
| regression / trajectory-return / return-monitor-146 | 0 / 0 / 0 | 0 / 0 | 8 / 0 |
| regression / trajectory-return / return-guard-146 | 2 / 0 / 0 | 2 / 0 | 6 / 0 |
| regression / copy-remains / trajectory-guard-145 | 0 / 0 / 0 | 0 / 0 | 1 / 1 |
| regression / copy-remains / return-monitor-146 | 0 / 0 / 0 | 0 / 0 | 1 / 1 |
| regression / copy-remains / return-guard-146 | 0 / 12 / 0 | 0 / 12 | 1 / 0 |


New146 refusals are distinct from inherited143/144/145 refusals. Raw appearance proposals, downstream filter acceptance and final tracking totals are different measurements. A correct proposal withheld by the new check is a cost, even if the shared filter would have rejected it anyway.

Already-exposed regression results are listed separately in the table and original group report. The production study uses genuine135 donor retention; no descriptor is injected into the app. The same pixels and original labels are checked against their published145 hashes.

## Fresh footage review and remaining qualification gap

The method, scorer, UI, generator, app harness and source-selection criteria were frozen at `236d4b2f` before new pixels were accessed. The [contract](docs/EDITION_146_CONTRACT.md), [method lock](docs/OBSERVATION_146_LOCK.json) and [selection record](docs/publication146/source-selection146.json) define the protocol.

Sources come from the [original OTB benchmark](https://rvlab.snu.ac.kr/research/tracker_benchmark/). The predeclared order is Coke, Subway, Woman and Human4. Selection uses source images, never tracker outcomes: the earliest eligible complete disappearance and return, ten visible/partial frames before and after, a bounded contiguous evaluation, and a disjoint source-qualified donor. Every rejected source or event remains recorded. A later donor-admission refusal does not trigger an easier replacement.

**None of the four sources met both frozen eligibility requirements.** A supplemental Coke run preserves complete occlusion255–263 and return264, with an all-partial donor and explicitly unavailable145/146 comparisons. Generated-source preparation also found three exact donor/evaluation duplicates per case. Those cases remain fixed diagnostics, never fresh qualification. See the [pre-prediction amendment](docs/publication146/AMENDMENT-source-unavailable.md).

| Source | Reviewed frames | Primary eligibility | Source-only finding |
| --- | ---: | --- | --- |
| Coke | 1–291 | Ineligible | Complete cover255–263; return264. Holding hand prevents the required unobscured donor. Supplemental baseline-only diagnostic. |
| Subway | 1–175 | Ineligible | No qualifying complete disappearance and return in175 source frames. Low contrast is not proof of full occlusion. No tracking executed. |
| Woman | 1–597 | Ineligible | Source-only review before tracking; camera pans and zooms. No qualifying complete occlusion and return; no evaluation window selected. |
| Human4 | 1–667 | Ineligible | All667 original frames reviewed before predictions; only nonempty archive annotation2 used, annotation1 is empty. No qualifying event established. Camera pans; no outcome selection. |

Every rejected event, original annotation selection and review detail is retained in the [source-review record](docs/publication146/visibility146.json).

| Source / role | Domain | Original frames | Visible / partial | Fully hidden / out-of-view | Uncertain |
| --- | --- | --- | ---: | ---: | ---: |
| coke-1 / reference-source | real-camera | 1–24 | 0 / 23 | 0 / 0 | 0 |
| coke-245 / evaluation | real-camera | 245–284 | 0 / 30 | 9 / 0 | 0 |
| generated-reference / reference-source | generated-overlap-diagnostic | 1–24 | 23 / 0 | 0 / 0 | 0 |
| short-return / evaluation | generated-overlap-diagnostic | 1–32 | 29 / 0 | 2 / 0 | 0 |
| distant-copy / evaluation | generated-overlap-diagnostic | 1–32 | 9 / 0 | 22 / 0 | 0 |
| same-path-copy / evaluation | generated-overlap-diagnostic | 1–32 | 9 / 0 | 22 / 0 | 0 |
| delayed-return / evaluation | generated-overlap-diagnostic | 1–32 | 17 / 0 | 14 / 0 | 0 |
| generated-reference / reference-source | exposed-development | 1–24 | 23 / 0 | 0 / 0 | 0 |
| trajectory-return / evaluation | exposed-development | 1–24 | 21 / 0 | 2 / 0 | 0 |
| copy-remains / evaluation | exposed-development | 1–24 | 9 / 0 | 14 / 0 | 0 |


External benchmark rectangles are supplemented by attributed source-only AI visibility and competitor review. This is not independent human adjudication. Source annotations and uncertainty are committed before predictions. Original archives, checksums, CRC checks, annotation bytes and selected frame hashes are retained. No raw camera archive or private webcam footage is published. Camera motion and similar objects are reported only where the actual source supports them.

The donor rule permits a disjoint window before or after the evaluation event. Exact source-frame ranges disclose that ordering. This is an offline test with a previously supplied reference; a donor later in the original video does not establish causal, reference-free live tracking.

![Fresh camera baseline-only diagnostic; references unavailable / coke-245 / foreground-guard-139. Illustrative completed source and trace; consult all outcomes, including refusals and wrong objects.](docs/readmes/146/coke-coke-245-19-review-source.png)

![Fresh camera baseline-only diagnostic; references unavailable / coke-245 / foreground-guard-139. Illustrative completed source and trace; consult all outcomes, including refusals and wrong objects.](docs/readmes/146/coke-coke-245-10-review-source.png)

Generated overlapping-source diagnostics test a short legitimate return, a distant surviving copy, a same-path unseen copy and a delayed legitimate return. The same-path copy and short return have identical pixels after a declared identity substitution. They illustrate an observability limit and are not independent image trials. Generated geometry and already-exposed regressions do not establish camera reliability.

## Review the behavior

1. Launch `Launch-Agent-Workspace.bat` with Java 21 and the existing desktop prerequisites. Offline preparation is available through `scripts/Start-Agent-Workspace.ps1 -PrepareOnly -Offline`.
2. Use an isolated project. In the spatial workspace, select a saved world and import a frame pack.
3. Import independent source labels, audit a separate donor and retain its verified appearances. A successfully tracked donor does not automatically produce admissible examples.
4. Select the evaluation footage and verified reference set. Compare `trajectory-guard-145`, `return-monitor-146`, and `return-guard-146` explicitly. A same-object declaration is required; it is not identity proof.
5. Inspect original frames, the return-continuity explanation and independent outcome audit. The app distinguishes unsupported motion, size incompatibility, expired evidence and preserved baseline continuity. Charts distinguish measured, estimated and predicted positions.
6. Export the complete evidence or resume an interrupted run. Original source and code checks still apply; incompatible historical proofs are not relabelled.

The [offline guide](docs/readmes/146/index.html) provides source comparisons, actual screenshots, implementation links and a searchable file inventory without running new experiments.

## Verification and retained attempts

374 distinct Java tests and 161 desktop checks passed, plus kernel integration, agency regression, offline preparation and package/build verification. 14 actual app runs across 3 isolated groups, plus one separately reported process-interruption regression. All original source, code and outcome checks remain required. Hosted CI is separate.

- Separate donor runs precede all fixed evaluation windows. Retention with zero eligible examples is refused and recorded; reference-dependent cases remain unavailable without replacement. Eligible cases compare three methods on identical pixels. Fresh real footage and generated trajectory controls are reported separately; no-reference cases retain an explicit baseline run and unavailability records. Independent host overlap/distractor/supplemental-outcome arithmetic agrees with Java. Monitor146 preserves every145 tracker/filter output; candidate preserves139 baseline and145 competed restorations, independently scoring additional146 refusals without a warning. No automatic anchor admissions; defaults unchanged.
- Donor retention refusal preserved. Fixed baseline and its complete export checked; no claim of146 method selection, recovery or prefix-resume validation on this unavailable source.
- Owned-process crash and cold reopen preserve exact completed-state bytes/timestamp and zero observation writes. All recorded runs/audits reverified, including original-pixel decision replay. Donor and evaluation source modification independently refuse audit/export; original restoration revalidates.
- Supplemental generated refusal continuation: all12 reference-dependent starts refused shared images without changing durable state. Four baseline runs use no references. Original failed attempt preserved. Separate donor runs precede all fixed evaluation windows. Retention with zero eligible examples is refused and recorded; reference-dependent cases remain unavailable without replacement. Eligible cases compare three methods on identical pixels. Fresh real footage and generated trajectory controls are reported separately; no-reference cases retain an explicit baseline run and unavailability records. Independent host overlap/distractor/supplemental-outcome arithmetic agrees with Java. Monitor146 preserves every145 tracker/filter output; candidate preserves139 baseline and145 competed restorations, independently scoring additional146 refusals without a warning. No automatic anchor admissions; defaults unchanged.
- Shared-image start refusals preserved; retained donor is not supplied to evaluation. Fixed baseline and its complete export checked; no claim of146 method selection, recovery or prefix-resume validation on this unavailable source.
- Explicit method selection, recovery explanation, original-source overlay, independent audit UI, complete bounded export and exact stopped-prefix resume passed. Native physical world unchanged.
- Killed only the owned Java process during loss without a competing-object warning and the run was unfinished
- Cold reopen retained the exact committed prefix and interrupted status
- Resume reconstructed pre-loss motion and return decisions from original pixels before continuing
- Complete trace and independent outcome audit exactly match the uninterrupted run
- Native world unchanged; Additional observation run uses the existing capacity without expansion

The [interruption check](docs/readmes/146/restart-check.json) repeats the already-exposed copy-remains regression, kills only its owned Java process during loss without a competing warning, then reconstructs the original pre-loss evidence and compares the full resumed trace and audit. This is durability evidence, not another fresh sequence. [Diagnostics](docs/readmes/146/diagnostics.json) verify preserved parent proposals, new refusal preconditions and target anchors that never advance from restored predictions.

Each camera source uses its own isolated comparison project, with a separate generated project and exposed-regression project. Existing frame, dataset, run and export limits remain in place. No evidence is pruned to fit. If a donor has no verified examples, its refusal and fixed baseline result remain explicit; dependent146 checks are unavailable on that source.

- Initial focused compilation failed on a nonexistent test descriptor helper; corrected before the method freeze. Original log retained.
- Human4 source preparation rejected two annotation filenames; only annotation2 is nonempty. Source-only correction and CRC/hash receipt retained.
- First prepare146.py source-separation assertion failed: donor frames15,17,19 duplicate generated evaluation frames8,9,10. Generator unchanged; all four cases downgraded to overlapping-source diagnostics before any predictions. No fresh generated qualification claim.
- All four predeclared camera sources were ineligible under the fixed full-occlusion plus unobscured-donor rules. Coke is retained as a supplemental baseline-only diagnostic with partial labels and no fabricated verified examples.
- The actual generated app attempt correctly refused shared reference/evaluation images before tracking. Its frozen harness then attempted the wrong-source audit and failed. A separate explicit-unavailability continuation preserves the failure, checks every reference-dependent refusal and runs139 only; no source check is bypassed.


CI was checked before implementation: both edition 145 runs passed. An older edition 144 metadata run failed one calibration test while its release passed; that original failure remains recorded and the unchanged calibration test is included in local146 verification. Hosted CI after publication remains separate from local success. See [GitHub Actions](https://github.com/eyeoverthink/Fraynix-Agent-Workspace/actions) and the publication receipt for the actual release status.

## Complete workflow cost

| Domain / source / method | Tracking ms | Replay/audit ms |
| --- | ---: | ---: |
| real-camera / coke-245 / foreground-guard-139 | 7910.1 | 9066.9 |
| generated-overlap-diagnostic / short-return / foreground-guard-139 | 6734.6 | 7768.5 |
| generated-overlap-diagnostic / distant-copy / foreground-guard-139 | 6780.7 | 7239.9 |
| generated-overlap-diagnostic / same-path-copy / foreground-guard-139 | 8345.9 | 7706.6 |
| generated-overlap-diagnostic / delayed-return / foreground-guard-139 | 8245.1 | 6985.9 |
| exposed-development / trajectory-return / trajectory-guard-145 | 12482.8 | 19839.7 |
| exposed-development / trajectory-return / return-monitor-146 | 13756.8 | 19832.7 |
| exposed-development / trajectory-return / return-guard-146 | 16023.0 | 19808.7 |
| exposed-development / copy-remains / trajectory-guard-145 | 15085.3 | 17514.1 |
| exposed-development / copy-remains / return-monitor-146 | 16584.3 | 17730.2 |
| exposed-development / copy-remains / return-guard-146 | 17692.4 | 17555.1 |

Complete coke app interval: 39.9s, 2 runs, 7054215 export bytes. Includes imports, reference checks, UI, source replay, stop/resume, completed-state restart, export and tamper checks, plus any retained attempt gaps. Phase durations above are subsets.

Complete generated app interval: 167.4s, 5 runs, 1725011 export bytes. Includes imports, reference checks, UI, source replay, stop/resume, completed-state restart, export and tamper checks, plus any retained attempt gaps. Phase durations above are subsets.

Complete regression app interval: 318.2s, 7 runs, 1108655 export bytes. Includes imports, reference checks, UI, source replay, stop/resume, completed-state restart, export and tamper checks, plus any retained attempt gaps. Phase durations above are subsets.

Additional unfinished-run interruption: 46.1s. Separate read-only screenshot reviews: 144.9s.


The new screen reuses existing motion evidence and adds no image search. This does not make the full workflow free: original-source replay, reference verification, persistence, reopening and export remain required. Fixed-order timings are descriptive measurements, not an isolated speed comparison. No verification is skipped.

## Source and preserved evidence

- [Return controller](workspace-engine/src/main/java/fraymus/workspace/ReturnContinuity146.java), [independent audit](workspace-engine/src/main/java/fraymus/workspace/ReturnAudit146.java), [focused tests](workspace-engine/src/test/java/fraymus/workspace/ReturnContinuity146Test.java), and [actual app harness](workspace-desktop/test/return146-integration.cjs).
- [Unchanged145 controller](workspace-engine/src/main/java/fraymus/workspace/TrajectoryRecovery145.java), [motion bounds](workspace-engine/src/main/java/fraymus/workspace/TrajectoryGate145.java), [appearance confirmation](workspace-engine/src/main/java/fraymus/workspace/AppearanceConfirmation143.java), and [verified reference resolver](workspace-engine/src/main/java/fraymus/workspace/ReferenceMemory137.java).
- [Verification](docs/readmes/146/verification.json), [source selection and labels](docs/publication146/source146.json), [original evidence inventory](docs/readmes/146/original-evidence.json), [file inventory](docs/readmes/146/file-inventory.json), and [linked system map](docs/obsidian/154-return-continuity.canvas).

Historical editions001–145 remain frozen. Publication targets `eyeoverthink/Fraynix-Agent-Workspace` main, edition146 and annotated `readme-146`, followed by fetched Git-file verification. The unified Java world/browser/creation application remains the destination.

## Next steps

1. **Qualify full camera occlusion with an admissible reference.** Record a fixed-camera sequence with a clearly unobscured initial view, complete cover, return and a separate lookalike. Freeze source rules first; label before predictions. Existing partial-donor refusal must remain intact.
2. **Correct the generated evaluation design before a new freeze.** Use disjoint positions or backgrounds for donor and evaluation images, assert image separation before execution, and preserve the146 overlap failure. Keep an identical-pixel substitution case as an explicit observability limit.
3. **Measure the cost of conservatism.** Test turns, longer gaps and camera motion without using them to retune this frozen method. Count legitimate returns withheld alongside wrong objects prevented; do not promote a method that merely refuses everything.
4. **Use qualified observations in a durable application task.** Let chat request an observed-object report or working artifact. Java must preserve measured, predicted and unknown states, retain source evidence and stop actions when identity is unresolved. Keep the connected world/browser/creation application central.
