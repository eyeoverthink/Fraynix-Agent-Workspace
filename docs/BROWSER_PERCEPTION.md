# Browser perception, teaching and tracking

Edition023 adds the [tracking learning lab](TRACKING_EVALUATION.md), recorded-frame replay and pinned tracker implementations for new goals and retained skills. A failed real-sequence candidate leaves the baseline active. Existing detection, explicit teaching and source guards continue to operate.

Edition 022 adds a fourth registered Java procedure, `browser-perception-v1`.
It connects the real Chromium browser surface to local pixel inference, Java
appearance tracking, explicit user corrections, durable goals and saved skills.
The existing physics, spatial, data, browser and creation features remain in the
same workspace. No replacement renderer, hosted vision service or new model
weights are required.

## Try it

1. Open **Perception**, then **Load labelled demo**. This page uses real, credited
   photographs with synthetic motion, scale and hiding controls.
2. Choose a visible image or canvas region. **Capture source** freezes the actual
   unannotated Chromium pixels and records its source and hash.
3. Choose **Vision + taught memory**, a generic category such as `person` or
   `cup`, and an already installed vision model. Gemma3 is the tested detector.
   Start **Detect & follow**. The browser draws outlines and trails from actual
   observations. An estimate is labelled as an estimate.
4. To correct the result, capture again, drag a rectangle around the object in
   the preview, enter a generic category and example name, and save the drawn
   example. The example is tied to that frozen capture, not a moving preview.
5. Track using **User-taught appearance**, or return to **Vision + taught memory**.
   Matching retained examples are checked before a new model call. The memory
   survives restart and can transfer to changed scale within this tracker's
   tested range. A different appearance may require another example.
6. Give the procedure a name to retain it after the complete workflow passes.
   Reuse from the skill inspector or chat on a newly selected current source.
   Every use captures and checks fresh frames; it does not replay an old success.

Example chat request:

> On this webpage, detect and outline the person, follow for 3 seconds, trace
> the observations, and save the successful procedure as web-person-follow.

Saved procedure request:

> Reuse the web-person-follow procedure on this webpage for 2 seconds.

For a page heading or profile label, **Page-labelled element (DOM)** follows the actual
element rectangle. A name in page text remains a claim made by the page. It does
not establish the identity of someone in an image.

## Responsibilities and evidence

| Layer | Actual responsibility | Evidence retained |
| --- | --- | --- |
| Chromium host | Enumerate visible sources, capture pixels, render pointer-transparent outlines | Document UUID, weak element reference, URL, crop, scroll, time and PNG SHA-256 |
| Local vision model | Estimate generic category and bounding rectangle | Admitted local model digest, exact request hash, raw structured response and crop mapping |
| Java tracker | Match appearance, update cautiously, record loss and reacquisition | Temporary track ID, boxes, last-known state, match scores and broken trajectories |
| Taught memory | Retain explicit user-labelled appearance examples | Frozen source/hash, box, label, 192 RGB features, name and immutable version |
| Java goal runner | Own progress, browser sampling, interruption and success checks | Durable steps, source binding, attempt, leased effects and exact acknowledgements |
| Canvas verifier | Open and test the generated report in actual Chromium | Eight actual assertions on the pinned report version |
| Procedural skill | Retain a registered detector/tracker workflow after checks pass | Versioned defaults and provenance; a new source is bound on each use |

Java does not receive a DOM label as visual proof. Vision inference receives the
captured image and generic category instructions; page text is not promoted into
model instructions. A user correction has explicit user-labelled authority.
The overlay is hidden during capture, preventing its own boxes and trails from
becoming training pixels.

## Detection and appearance learning

`VisionDetector` verifies that the selected model is local and currently advertises
vision support, then uses Ollama image input and structured output. The model may
classify a different category; a requested category is not forced into its output.
Every returned rectangle is checked for valid range and geometry. A syntactically
valid rectangle can still be inaccurate. Workflow verification never makes that
rectangle semantic ground truth.

`PixelRegions` offers object-agnostic foreground proposals when an image has a
stable, mostly uniform border. Otherwise it uses the full captured region.
Proposals are upscaled before inference and mapped back into original coordinates.
This preprocessing does not guarantee precise object boundaries.

`PixelTracker` is pure Java CPU computation. It uses an 8 by 8 RGB descriptor and
normalized correlation over bounded scale and position candidates. It requires
both current and original-anchor similarity, rejects a close competing peak, and
updates the descriptor only above the stronger match thresholds. Correlation is
an appearance score, not a calibrated category probability. The original anchor
limits gradual drift; it does not solve arbitrary pose or viewpoint changes.

Explicit teaching stores a 192-value appearance descriptor. Ordinary vision mode
first searches the latest version of each retained example for that category.
A strong, sufficiently distinct match uses the correction without a model call.
This is persistent local appearance learning. It does not update LLM weights,
train a universal object detector, or establish a named person's identity.

## Source ownership and recovery

Selection is tied to a document UUID and a real weak element reference. Navigation
or page reload invalidates that source. A tracker may not silently continue on an
unrelated page. Offscreen or materially clipped targets fail capture explicitly.
During an active perception goal the host blocks unrelated navigation, clicking
and typing; the page's own dynamics continue and can cause genuine tracking loss.

Each sample has a strictly newer timestamp and exact sequence. A retried host
effect can reuse its already persisted exact receipt, but cannot advance with a
stale frame or caller-supplied success boolean. Source/hash/sequence are checked
again in Java. A failed host effect releases the owned tracker before interruption.

When an object disappears, the current box is absent and the last-known box is
retained separately. The trace breaks. Reacquisition may keep the temporary track
ID within that run; the app does not invent a path through hidden frames. Process
restart interrupts a live run. Explicit resume takes a new source capture and
records another attempt with fresh temporary IDs. It never silently extends the
identity of a track into another document.

## Files, limits and verification

Private project state lives under `perception/`: `state.json` and original source
PNGs in `frames/`. Teaching examples and procedures survive a normal restart.
Current limits are 512 frames / 128 MB, 32 runs, 32 examples, eight versions per
example name, at most six model objects and 120 samples per run. Durations are
2–30 seconds. This is bounded screenshot sampling, not high-frame-rate video or
an unlimited background surveillance service.

Reports retain `index.html`, `perception-run.json`, `source-frames.json`,
`detection.json`, `motion.csv` and `verification.json`, plus taught-example data
when used. The standalone HTML contains bounded actual source thumbnails;
original PNG hashes remain in the audit. **Export run** exports the actual run,
not generated model narration. Data formats do not install new executable tools.

The workflow requires at least four actual observations and a tracked fraction
of at least 0.65. Java reconstructs those metrics, source identities, hashes and
sequences; the report is then checked in actual Chromium. Its
`semanticGroundTruthVerified` field remains false. The independent reference-box
test in the release evidence reports model grounding separately: a coarse cup box
can pass a stable tracking workflow while failing the reference-overlap gate.
Both outcomes must remain visible.

## Next acceptance gates

First broaden separately collected, consented or appropriately licensed sequences:
moving real objects, changed background and lighting, viewpoint changes, long
occlusion and distractors. Measure detection recall, box overlap, track switches,
false reacquisition and end-to-end latency. Keep fitting/correction examples
separate from evaluation cases. Only promote a changed tracker when it improves
those measurements without regressing source and loss handling. A specialized
local detector, motion model or GPU implementation is a later measured choice;
neither GPU speed nor weight plasticity alone proves better perception.

Primary references: [Ollama vision](https://docs.ollama.com/capabilities/vision),
[structured outputs](https://docs.ollama.com/capabilities/structured-outputs),
[Electron webContents](https://www.electronjs.org/docs/latest/api/web-contents),
and [fixture attribution](../licenses/PERCEPTION_FIXTURES.md).
