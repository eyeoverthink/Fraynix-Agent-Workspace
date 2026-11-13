# Fraynix Agent Workspace — Edition 139

**Check foreground motion and spatial structure before trusting a tracked box.** Fraynix connects durable chat goals, Java simulation, observed worlds, browser interaction, creation tools and verified reusable procedures. This edition adds an optional drift guard and tests it against new disappearance and return footage. The established tracker remains the default.

[Previous edition](README-138.md) · [All editions](docs/readmes/INDEX.md) · [Offline interactive guide](docs/readmes/139/index.html) · [Integrated roadmap](docs/INTEGRATED_AGENT_ROADMAP.md) · [Contributor handoff](README-CONTRIBUTORS.md)

![Actual app: foreground evidence](docs/readmes/139/ballet-100-drift.png)

## What changed

Color alone can remain plausible while a tracking rectangle gradually slips onto background pixels. **foreground-guard-139** retains nine textured parts from the original object. It searches for those parts, checks whether they still form a coherent arrangement, and compares their motion with nearby background motion and the flow tracker's proposed movement. Two consecutive structural disagreements can withhold a measurement while the underlying tracker still reports success.

This is handwritten Java computation. It adds no model weights or runtime vision library. The initial appearance is immutable: tracked predictions never become new templates. Missing texture or surrounding background is insufficient evidence, not a fabricated certainty. The trace exposes eligible parts, matched parts, geometric agreement, original proposal, foreground/background displacement, warning reasons and recovery confirmations.

Edition138's whole-appearance checks remain. Recovery requires a strong unique original-source match, spatial support when enough seed parts exist, and agreement across two images. A newly rejected image cannot alone certify recovery. Identical lookalikes must remain unresolved. This is appearance tracking, not person identification or general object understanding.

**monitor-only-139** computes the evidence without changing any incumbent tracker/filter output. Comparisons include **legacy-flow**, the unchanged default, and **drift-guard-138**. Earlier verified-reference features remain available with their original checks. No automatic appearance-memory admission is introduced.

## Fresh results

{{FINDING}}

{{RESULTS}}

{{WARNINGS}}

Useful warnings identify an already wrong or hidden proposal; they do not prove advance knowledge. False warnings flag correct visible proposals. Missing measurements stay in the denominator. Withholding everything is not successful tracking. An overlap failure does not by itself establish an identity switch, and unlabelled distractors cannot establish switch-free behavior.

## Sources and limits

{{SOURCES}}

Original [VOT-LT2018 recordings](https://www.votchallenge.net/vot2018/dataset.html) and official annotation archives were acquired after the method/scorer freeze. Selection follows the [contract](docs/EDITION_139_CONTRACT.md) and [source-only clarification](docs/EDITION_139_SOURCE_NOTE.md). Every selected image was inspected without tracker predictions, then source labels were frozen. No thresholds were changed after seeing fresh outcomes.

Ballet and bicycle test exits from the frame and returns. Some NaN annotation boundaries contain visible fragments; these are uncertain rather than falsely counted as completely hidden. Bike1 has no annotated absence and remains a continuous-motion control. The additional bird1 source was selected by the first qualifying interior disappearance in metadata order. Its content and obstruction evidence are described in the [source manifest](docs/publication139/source139.json). VOT bird1 is distinct from the earlier rendered OTB Bird1. External rectangle/absence data and additional AI source judgments are separately recorded; AI review is not independent human adjudication. This bounded study is not a full VOT benchmark submission.

{{SOURCE_REVIEW}}

![Actual independent source/outcome view](docs/readmes/139/bird1-641-source.png)

### Previously exposed development footage

The four edition138 windows are development data, not fresh evaluation. Across332 visible/partial samples, the default and138 guard each retained122 correct observations;139 retained72. Wrong accepted overlaps fell from132 for the default and130 for138 to1 for139. Missing measurements rose from78 to259. On the single confidently hidden sample, the default and138 produced an unsupported box;139 withheld it. These substantial recall losses prevent a promotion claim. The monitor control preserved the first skater's95 later tracker/filter outputs exactly. Complete development traces and costs are retained.

## Verification and recovery

{{VALIDATION}}

{{APP_CHECKS}}

Before outcome scoring, the Java audit rebuilds the entire139 decision sequence from original pixels and the original seed. Each recorded decision must match. Independent labels never enter tracking. Source hashes, implementation identities, stored histories, world bindings and outcome checks remain mandatory. Changed pixels or forged decisions refuse verification/export. Completed-state restart does not silently rewrite evidence. Interrupted work requires explicit resume.

The production interface shows accepted boxes, withheld original proposals and seed-part candidates separately. Purple marks uniquely matched seed parts; brown marks unconfirmed candidates. Neither color is a ground-truth label. A withheld observation does not move the native physical world.

## Complete workflow costs

{{COSTS}}

These are measured durable tasks, including retained history and a separate audit with original-pixel replay. They are not a same-workload scaling benchmark or proof of live webcam speed. Extra spatial matching and replay have real costs. The study uses one owned Java process, bounded heap/processor settings and an isolated J: project; private projects and shared desktop dependencies remain intact.

## Review it locally

Use JDK21 and the prepared desktop dependencies. From the checkout:

```powershell
$env:FRAYNIX_WORKSPACE_PROJECT = Join-Path $env:LOCALAPPDATA 'Fraynix-Review-139'
.\scripts\Start-Agent-Workspace.ps1
```

After dependencies are cached, add `-Offline` to require local operation. Preserve your existing project path and stop its Java owner through its app before rebuilding that owner's installation.

1. Open **Spatial → Real footage · observed world**. Select a saved world, import footage and mark the initial object rectangle.
2. Run **Existing flow tracker** as the baseline. Choose the139 experimental method explicitly for a comparison; use the139 monitor to retain incumbent outputs.
3. Inspect original pixels, the proposed box, matched parts and warning explanations. Do not treat the prediction overlay as a measurement.
4. Import independently prepared identity/visibility labels under **Object identity · occlusion & recovery**. Audit the run and inspect false warnings, missing measurements and recovery outcomes.
5. Stop/resume, reopen and reverify. Export the complete evidence within existing bounds. Keep the original project if an export exceeds a limit; never discard evidence to make it fit.

## Where this fits

```mermaid
flowchart LR
  A[Original pixels and seed] --> B[Immutable textured parts]
  A --> C[Flow proposal]
  B --> D[Spatial and relative-motion checks]
  C --> D
  D --> E[Accept or withhold]
  E --> F[Persistent observed world]
  F --> G[Replay from original pixels]
  H[Independent labels] --> G
  G --> I[Evidence for durable Java goals]
```

The destination remains one local agent system: Java owns durable goals, environment state, action validation, recovery and evaluation; the desktop presents the world, browser and creation canvas. Better measurements should support useful tasks, not replace the application with an isolated tracking demonstration. No default, action-authority or capacity increase is part of this edition.

{{NEXT}}

## Attempts and publication evidence

{{ATTEMPTS}}

Hosted CI, local tests and fetched publication checks are distinct. Raw footage archives and private projects are not published. Exact source identities, external annotations, frozen labels, run traces, failures, tests and limited attributed app screenshots are retained.

[Validation](docs/readmes/139/verification.json) · [Original evidence](docs/readmes/139/original-evidence.json) · [Comparison](docs/readmes/139/comparison.json) · [Source manifest](docs/publication139/source139.json) · [Method lock](docs/OBSERVATION_139_LOCK.json) · [Source inventory](docs/readmes/139/file-inventory.json)

[Spatial evidence](workspace-engine/src/main/java/fraymus/workspace/SpatialEvidence139.java) · [Guard](workspace-engine/src/main/java/fraymus/workspace/ForegroundGuard139.java) · [Replay audit](workspace-engine/src/main/java/fraymus/workspace/ForegroundAudit139.java) · [Actual-app test](workspace-desktop/test/foreground139-integration.cjs) · [Project memory](docs/PROJECT_MEMORY.md)

Preserve every frozen edition, original failed comparison and private dataset. No candidate promotion or publication claim without supporting evidence. Read [AGENTS.md](AGENTS.md) before continuing.
