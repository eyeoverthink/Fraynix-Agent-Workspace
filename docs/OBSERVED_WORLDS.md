# Observed worlds — edition 036

An observed world is a durable estimate attached to an existing persistent world
ID and its original body IDs. It shares identity and history with the workspace;
its measurements do not overwrite native verified physics.

## Domains and evidence

The supported measurement domain is normalized image coordinates: x increases
rightward and y downward. Time is the imported source-frame ordinal or the
local-video decoder's sample index. Source capture FPS, metric scale, camera
depth, physical orientation, mass and friction are unknown. Local video source
metadata records container duration, number of samples and resizing. That does
not establish original capture timing or a calibrated camera.

The importer supplies a first-frame rectangle and label for each of one to four
objects. Each rectangle is explicitly bound to one distinct existing dynamic
body ID. The label is a human declaration, not semantic recognition or named
person identification. Runtime import rejects masks, references and unknown
fields. Later annotations enter only the research scorer after tracking ends.

## Tracking and prediction

The unchanged flow-v3 Java tracker processes actual decoded pixels. It combines
pyramidal optical flow, forward/backward checks and appearance checks. It can
lose or drift from an object. Accepted match status does not prove semantic
identity, and correlation scores are not probabilities.

`ObservationFilter` maintains separate x/y position and velocity axes. Before
each measurement it predicts the next state and covariance. It then records the
actual measurement, innovation and fixed innovation gate. An accepted pixel
measurement updates the estimate; missing or rejected measurements retain a
labelled prediction and grow uncertainty. Non-increasing times and gaps above
30 indices are refused. No later observation enters an earlier prediction.

Frozen assumptions: initial position sigma 0.02, velocity sigma 0.03, measurement
sigma 0.015 and acceleration sigma 0.002 in the declared image/index domain.
Innovation gate is the larger of 0.12 and five times the combined predicted and
measurement standard deviation. These are model assumptions, not calibrated
real-world error bounds. The reserved measurements show important calibration
limitations; see README-036 and the original per-frame scoring records.

## Persistent ownership

`WorldObservationStore` owns a separate atomic checksum envelope and a
hash-chained observation history. Each accepted frame adds one observation
revision with updates for its measured bodies. A prediction-only frame remains
in the run trace without fabricating a measured world revision. Every update
records the source image hash, dataset, frame, run and exact native base binding.

Native revision/hash and observation revision are checked before starting.
Pending native tasks and stale bindings refuse observations. A native world
change marks existing estimates stale; resuming under that changed head is
refused. An explicit new run can bind to the new head. Native verified positions,
orientations and velocities are never replaced by image-plane estimates.

One run processes at a time. Stop retains its evidence. A process interruption
becomes an explicitly resumable run. Resume preserves its ID and exact committed
trace, reconstructing optical-flow and filter state by replaying original
pixels. An execution generation prevents a stopped worker from committing after
an immediate resume. Source integrity and implementation identity are checked.
Incompatible code, corrupted state or altered original images are refused.

## App workflow

1. Open Spatial and create/select a persistent world.
2. Open **Real footage · observed world** below its controls.
3. Choose a frame JSON package or a local video. For video, draw the initial
   rectangle in the actual first decoded sample, then import it.
4. Bind each image object to a stable world body and confirm current revisions.
5. Choose **Track & update estimates**. Inspect original frames, tracker boxes,
   prior predictions, updated estimates and all five motion chart modes.
6. Use Stop/Resume explicitly. Open the saved motion report or export the
   complete original evidence. Returning to the project retains the estimates.

Chat has host-bound `observations.read`, `observations.start`,
`observations.wait` and `observations.report`. Model calls cannot submit IDs,
measurements, success values or custom numeric gates. Ordinary `world.observe`
also includes the separately qualified observed estimate. Readouts expose the
latest prediction/loss separately from last accepted measurements, with run,
dataset coordinate frame and native binding. Different datasets are different
image coordinate frames; binding the same body ID does not calibrate cameras.
Physical goal tools
continue using the native verified state.

## Package format

```json
{
  "schema": "fraynix-observation-frames-036-v1",
  "title": "My recorded sequence",
  "source": "Original source and recording/sampling description",
  "license": "Source attribution or declared ownership",
  "clock": "source-frame",
  "objects": [{"key":"subject","label":"human-selected object","box":[0.2,0.2,0.5,0.6]}],
  "frames": [{"sourceFrame":0,"image":"BASE64_JPEG_OR_PNG","imageSha256":"EXACT_ORIGINAL_BYTE_HASH"}]
}
```

The example explains fields; it is not an executable fixture. Actual imports need
5–96 complete frames and exact SHA-256 hashes. Rectangles use normalized
[left, top, right, bottom]. Frames need constant dimensions, strictly increasing
integer indices, JPEG/PNG pixels, at most 1920×1080 and 1.5 MB each. Frame packages
contain at most 32 MB original image bytes. Local video import is bounded to
100 MB and ten container minutes, producing 5–96 uniform samples with a disclosed
sample-index clock. These are decoded video observations, not a camera stream. The export retains
the imported decoded JPEG samples; it does not embed the source video container.
Keep the original video separately.

## Export and limits

Complete exports contain the selected run, every other run for that world
(including failures/cancellations), every referenced dataset/original image,
the entire observation history and the native world lineage. Raw SHA-256 and
size accompany lossless gzip transport. Evidence is never silently pruned.

```powershell
node scripts/Inspect-ObservedWorld.cjs "C:\path\to\observed-world.json"
```

The inspector validates original bytes, source hashes, body references, frame
order and history links. It does not rerun Java tracking or independently
recompute Java canonical event hashes. Native physics exports remain available
through the unchanged edition035 controls.

Bounds: 16 imported datasets, 24 observation runs, 2,048 observation revisions,
24 MB persisted metadata, 40 MB original pixel bytes/64 MB raw full export and
the desktop's existing 20 MB export transport limit. Exceeding a bound refuses
the operation and preserves evidence. Project archival/rollover is future work.

## Frozen validation

Protocol 19c207d preceded implementation. Candidate 1dd0f07 preceded reserved
footage. Metadata-only amendment 85f546c replaced two external-source exceptions
before any of their images or measurements were fetched. Car-roundabout and bus
were then downloaded as original verified archive members. Candidate sources
and numeric gates remained unchanged. The first reserved results are preserved
with exact-byte hashes; later repetitions are regressions, not unseen tests.

The next gate is calibrated geometry/time and empirically tested uncertainty,
followed by deliberate observation requests when tracking may have drifted.
Edition036 does not claim metric physical understanding or learned model weights.
