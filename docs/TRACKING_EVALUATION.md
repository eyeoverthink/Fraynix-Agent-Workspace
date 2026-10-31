# Tracking learning lab

**Current: edition024.** Pure Java motion tracking improves pooled mean overlap
on three newly reserved clips from0.080183 to0.328527. It remains rejected: only
33.73% of predictions reachIoU0.5 and four very-low-overlap visible boxes exceed
baseline's zero. Existing profiles and pins remain installed. See the current
[temporal tracking contract](TEMPORAL_TRACKING.md) for all results, code/source
freeze, real motion overlays and the next independent evaluation gate. The
edition023 implementation and first result below remain historical evidence.

Edition 023 adds original recorded-frame import, frozen Java tracker comparisons,
measured promotion gates, retained profile history, rollback, browser replay and
an actual tested report. The first real-data candidate is **rejected**: pooled
mean overlap falls from 0.094239 to 0.049849 over 38 prediction frames. The normal
active tracker remains `rgb-v1`. Development improvement does not establish
generalization, and no LLM weights were trained.

## Use the lab

Open **Perception → Tracking learning lab**. Expanding the lab uses the center
pane; closing it returns to detection and teaching. Import one development and
separate evaluation sequences, then **Compare selected sequences**. Progress
counts actual completed predictions. The report opens in the real creation canvas
and receives eight Chromium assertions, including a rejected candidate's report.

The activation checkbox applies only after fixed gates pass. A rejected candidate
cannot be activated through the button or typed tool. Disable automatic activation
to inspect a passing comparison before pressing **Activate passing candidate**.
**Restore baseline** records rollback without deleting the comparison or skills.
Profile state and history survive restart. Interrupted or stopped work cannot
promote partial results.

The frame slider shows original pixels, a blue reference and the actual saved
prediction for either profile. Lost predictions have no current predicted box.
The first-frame rectangle is labelled as given initialization. **Open actual frame
replay in browser** displays original selected frames without references inside
the source canvas. Ordinary Perception can capture and inspect that browser.
Playback interval is a presentation setting; original capture FPS is not assumed.

Select sources in the lab, then ask chat:

> Compare tracker profiles on the selected tracking sequences. Activate a candidate
> only if the fixed gates pass. Report failures honestly and open the verified report.

Registered tools are `tracking.read`, `tracking.start`, `tracking.wait`,
`tracking.report` and `tracking.rollback`. The host binds selected dataset IDs.
The model cannot supply thresholds or predictions. Java owns execution and the
decision. Completing a computation/report workflow does not make a failed accuracy
gate pass. Chat uses the actual numbers and retained decision.

## Data and boundaries

Import JSON schema `fraynix-tracking-sequence-v1` declares `title`, `sequence`,
`split`, `objectId`, `source`, `license`, `notes` and `frames`. Each frame carries
`sourceFrame`, `imageName`, `maskName`, base64 JPEG/PNG `image`, indexed PNG `mask`,
and exact `imageSha256`/`maskSha256`. The selected mask object ID supplies reference
geometry. Source names are labels, never filesystem destinations.

Limits are 5–128 selected frames, 24 MB original bytes and a 34 MB package, with
consistent bounded dimensions. Sources upload in order through bounded requests.
Retries are idempotent only for identical frames. Incomplete imports, changed
hashes and invalid geometry reject. Exact repeated image bytes within a sequence
are currently rejected, as are exact image overlaps across development/evaluation
cases. This restricted research format is not an arbitrary MP4 importer.

There are at most 12 datasets, 16 comparisons and 64 recent profile-history events
per project. Original data stays under private `tracking-lab/` storage. A canonical
package content hash differs from an external JSON file's byte hash. Reads and
promotion recheck original images, masks and the reconstructed package identity.
Roles, source authenticity and licenses remain declared metadata.

`rgb-v1` retains edition 022's RGB matcher. `adaptive-v2` adds denser RGB/edge
features, separate width/height search and a refined competing peak. Both are
bounded CPU appearance trackers. Ollama's generic category estimates remain a
separate qualified function.

New browser goals pin the active profile and an aggregate fingerprint covering
predictor helpers, feature extraction, geometry and factory code. An active goal
does not switch after promotion or rollback. A retained skill still uses its
original installed profile on a fresh source. Changed implementations fail
preflight rather than borrowing old verification. Legacy edition 022 procedures
without a fingerprint retain their baseline default.

Fixed promotion gates require 0.03 pooled mean-IoU gain, no sequence losing more
than 0.05, candidate IoU≥0.5 on at least 0.65 of prediction frames, no increase in
visible predictions below IoU0.1, passing duplicate/full-hide controls, intact
sources and unchanged code. These are local acceptance gates, not a universal
accuracy guarantee or permission to install arbitrary executable handlers.

## First recorded-sequence result

The declaration was committed at `ccda7cf`, with initialization scoring clarified
at `ac0cbf0`, before evaluation. Predictor and scorer source were frozen at
`69efdf6f5f1f11b5d5fbe58b109c35f8b9f2e9ad`; identities were recorded at `06b640a`
before downloading the reserved clips. Later identity-admission metadata includes
helper classes; it does not change frozen predictions or scores.

| Sequence / role | Baseline mean IoU | Candidate mean IoU | Candidate IoU≥0.5 fraction |
| --- | ---: | ---: | ---: |
| blackswan / development | 0.823485 | 0.913815 | 1.000000 |
| car-shadow / evaluation | 0.141412 | 0.099697 | 0.105263 |
| hike / evaluation | 0.047065 | 0.000000 | 0.000000 |
| Pooled evaluation | 0.094239 | 0.049849 | 0.052632 |

Only the first annotation initializes tracking. Later masks are read by the scorer
after prediction and never enter the predictor. Every remaining selected frame
counts, including losses as zero; no reset or frame removal occurs. This is
human-guided box tracking derived from masks, not official DAVIS J/F segmentation
scoring. Source selection is a small public subset, not a sequestered benchmark.

The first rejection is retained. Later executions are unchanged-code/source
reproducibility and software-validation replays, not newly selected candidates.
Accuracy repeats exactly; latency is measured per run. Timing includes source
verification, decode and CPU tracking, excluding browser capture, network, LLM
planning and reference scoring. No GPU speedup is claimed.

A stronger synthetic identical-copy case exposes a baseline ambiguity failure;
the candidate passes it. Separate synthetic aspect controls exercise positive
activation, rollback and persistent skill pins. Those controls do not override
the failed real-sequence result.

## Reports and next gate

Reports contain standalone HTML, comparison metadata, source summaries, accuracy
CSV, the protocol, plus original-source metadata and complete profile traces for
each case. The three-sequence proof has 15 actual files and eight actual canvas
checks. **Export comparison** saves the full original job record. Full original
image/mask files remain private; thumbnails are credited research derivatives.

Next, treat these failures as experienced development cases. A future candidate
needs time-aware correspondences and changing appearance, then a newly declared
independent evaluation set. Do not retune this frozen candidate against these
results and call the old clips unseen. Generic category training and LLM weight
updates remain separate tasks. See [research attribution](../licenses/TRACKING_RESEARCH.md).

## Original pre-execution declaration

This phase continues edition 022's browser perception with a real, bounded
comparison and promotion loop. The original tracker is the baseline. A candidate
must improve measured tracking on separate sequences before it becomes the
default for new runs. Rejected candidates remain recorded; rollback restores a
previous active version. Existing runs retain their pinned tracker version.

The machine-readable [acceptance declaration](../workspace-engine/src/main/resources/perception/acceptance-023.json)
is committed before the first tracking evaluation. Development uses only the
DAVIS `blackswan` sequence. `car-shadow` and `hike` are reserved for a single
evaluation after the candidate implementation is frozen. Only the first-frame
reference rectangle initializes tracking. Later reference masks are available to
the scorer, never to the tracker. Every selected frame counts, including failures.

The official [DAVIS download page](https://davischallenge.org/davis2017/code.html)
provides original frames and annotations. Its archive's README Terms of Use specify
CC BY-NC 4.0. Data remains separate from normal projects and model weights; source
URLs, archive metadata, original file hashes and attribution are retained. These
are human-guided bounding-box measurements derived from masks, rather than the
official segmentation benchmark metrics. Original capture FPS is not assumed.

At this declaration, implementation and measurements were pending. The current
result and operational contract are documented above; this text retains the
original pre-execution scope.

Development-only result: the RGB/edge candidate passed the stronger identical-copy ambiguity and full-hide controls after denser search and separately refining a competing peak. The frozen RGB baseline accepts this stronger duplicate counterexample; that failure is retained. Candidate changes have used development pixels and independent synthetic negative controls only. Reserved sequences have not been fetched or evaluated.
