# Temporal tracking · edition 024

The new installed `flow-v3` profile improves all three newly reserved DAVIS clips
against the unchanged `rgb-v1`. Across 166 predictions, mean IoU rises from
0.0801828732 to 0.3285274716. It **remains rejected for activation on these cases**:
IoU≥0.5 coverage is 0.3373493976 against the unchanged 0.65 gate, and four visible
predictions have IoU<0.1 against zero for baseline. Baseline abstains much more
often. Improvement and acceptable reliability are separate measured outcomes.

## What actually runs

`FlowTracker` is a pure Java CPU implementation, without OpenCV binaries, new
dependencies or downloaded weights. It preserves image aspect ratio at up to
480 pixels on the longest side, constructs four grayscale pyramid levels, and
selects spatially distributed corners within the given initial/current box.
It solves iterative local motion in 9×9 patches, follows points forward and
backward, rejects round-trip error above1.5 work pixels and patch correlation
below0.75, then filters inconsistent motion. Median displacement and pairwise
distance ratios estimate translation and bounded isotropic scale. The box keeps
its aspect ratio; it does not infer segmentation or arbitrary shape changes.

At least eight consistent points and a sufficient fraction of the proposed
features are required for the motion update. The previous accepted image and
geometry stay unchanged on loss. Reappearance can be checked against that last
accepted state. A stricter appearance fallback uses the existing `adaptive-v2`
implementation. A second near-exact initial appearance can force abstention;
the controls are specific examples, not a general identity guarantee.

The algorithmic basis is described in the primary
[OpenCV optical-flow guide](https://docs.opencv.org/4.x/d4/dee/tutorial_optical_flow.html).
This project implements the calculation in Java; it does not call the OpenCV
runtime. Local brightness consistency, texture and coherent motion remain
assumptions. Occlusion, deformation and background features can violate them.

Each prediction retains its actual method, feature/inlier counts, median
forward/backward error, displacement in work pixels, scale, ambiguity state and
up to12 normalized motion vectors. These are diagnostic measurements, not
calibrated object-category probabilities or physical world velocities.
When motion is not accepted, the inspector says so rather than treating a stored
default-zero diagnostic as a measured perfect round trip.

## Experiment and retained first result

Protocol [acceptance-024.json](../workspace-engine/src/main/resources/perception/acceptance-024.json)
was declared at `727fda4`, before new evaluation downloads. The new predictor,
adapter, admission implementation and controls were frozen at
`34c81f086b77f43e191e191e3f52f3851dd5d7f0`; source identities were committed at
`cab4d02`. The exact hashes are retained in
[profile-024.json](../workspace-engine/src/main/resources/perception/profile-024.json).
The four edition023 predictor/scorer files remain unchanged.

Edition023 clips are experienced development cases. The new development run
uses the first60 consecutive `hike` frames: 59 predictions, baseline0.075620,
adaptive-v2 0.030134, flow-v3 0.833151 mean IoU, candidate coverage1.0. The new
reserved cases use the first min(60,length) consecutive original frames. They
were downloaded only after freeze, with ZIP CRC, exact member SHA-256 and pinned
archive identity. They are a small public subset, not a sequestered benchmark.

| Case | Scored predictions | Baseline mean IoU | flow-v3 mean IoU | Candidate IoU≥0.5 | Candidate lost / very-low-overlap visible |
|---|---:|---:|---:|---:|---:|
| car-turn |59|0.101549|0.537575|0.542373|0 /2|
| dog |59|0.124050|0.220363|0.203390|32 /0|
| libby |48|0.000000|0.204526|0.250000|28 /2|
| Pooled reserved cases |166|0.080183|0.328527|0.337349|60 /4|

Only the first reference rectangle initializes a tracker. All remaining frames
score, including losses as zero. No resets, omitted failures or later reference
inputs occur. A test changes all later masks while retaining pixels and proves
that predictions stay identical; scores change accordingly. This evaluates boxes
derived from masks, not official DAVIS segmentation J/F.

Both profiles receive the same new frames. Edition023 used uniformly spaced
frames; its numbers cannot measure an edition-to-edition gain under this changed
consecutive-frame protocol. The first new decision is retained unchanged. Later
runs test exact accuracy reproducibility and software behavior, not candidate
selection. No candidate tuning followed the new evaluation.

## App, goals and skills

Open **Perception → Tracking learning lab** and select the new development plus
three evaluation packages. Comparison reports include every prediction, actual
source thumbnails, numeric gates, traces and latency. The four-case report has
18 files and eight real Chromium assertions. Report completion never upgrades a
failed accuracy verdict.

The frame inspector shows the blue reference, green candidate/amber baseline
box and optional cyan measured motion vectors. Unchecking the motion overlay
changes only rendering. Browser replay uses original images without reference
overlays; ordinary Perception captures that source separately. Replay timing is
presentation timing, not the unknown original capture rate.

New comparisons evaluate `flow-v3`; old `adaptive-v2` jobs remain readable and
their protocol/implementation identities remain attached. Existing profiles stay
installed. Java goals and saved skills retain the profile and helper-class
fingerprint they originally verified, even after global rollback. A changed
implementation fails preflight. Fresh projects keep baseline until their own
comparison passes fixed gates. The real024 cases do not pass. Separate explicitly
synthetic controls prove positive activation, actual browser teaching, saved
flow-v3 skill reuse after rollback and process persistence.

The local chat selects the same registered read/start/wait/report workflow. The
host binds datasets; Java performs predictions, scoring and admission. The model
cannot choose thresholds, supply boxes or override rejection. Neither this new
tracker nor stored procedures update LLM weights.

## Next evidence gate

The car case exposes late geometric drift despite continued visibility. The dog
cases expose losses under harder motion/deformation. These are now known failure
cases for future development. Useful next work is foreground-aware feature
selection, shape/aspect adaptation and stronger recovery with explicit uncertainty.
A future changed candidate must be frozen before another newly declared set of
clips is fetched/evaluated. Keep the current failure trace; do not relabel these
three clips as unseen after tuning on them.

The retained trace makes the distinction concrete: car-turn source frame59 still
has27 motion inliers and a0.0241-work-pixel round trip, but boxIoU is0.0862. Local
feature consistency therefore does not establish correct whole-object extent.
The dog first loses tracking at source frame28; libby first loses at21 and already
has a very-low-overlap visible prediction at19. These are post-evaluation failure
observations, not inputs used to tune this frozen candidate.

Research media retains the source terms and author credit in
[TRACKING_RESEARCH.md](../licenses/TRACKING_RESEARCH.md). Full original image/mask
packages remain outside Git; documentation publishes credited reduced previews
and derived measurements. See [README024](../README-024.md) for release tests,
screenshots, operating steps and the broader agency design.
