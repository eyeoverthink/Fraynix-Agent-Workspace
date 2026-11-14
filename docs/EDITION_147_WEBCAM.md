# Edition 147 — separate private webcam study

Declared before webcam access. The user chose their webcam; the public-source protocol in EDITION_147_CONTRACT.md remains frozen and separate. This study qualifies the same unchanged 145/146 methods. It does not change production tracking, defaults, permissions, thresholds or capacity limits.

## Capture

Use a fixed top-down camera, two distinct similar objects, and a small opaque cover. Confirm which object is the target before capture. Keep the second object visible and separate; cover only the target. The source recorder is a review/test instrument embedded in an isolated instance of the existing app, not a new production feature. No microphone, model, tracker or predicted boxes run during capture. Camera images and raw logs stay private in ignored build storage.

The user starts recording and advances the cues. One continuous recording spans every step; pauses in user actions are real recorded time, never spliced away. Capture JPEG samples at no more than two samples per second, preserving Chromium camera mediaTime and wall-clock receipt times. Maximum 600 samples, 300 seconds or 120 MB, whichever comes first. These are private raw-source bounds; app imports remain at most 96 frames and all existing application limits remain unchanged. Save every captured sample immediately with its hash and append a journal receipt. A stopped, incomplete, interrupted or over-limit attempt remains evidence and cannot be silently resumed or overwritten.

First hold the unobscured target and the separate lookalike still for at least 24 samples. Then cover the target, wait for a few samples, and reveal the same target with a small movement on the mat while the lookalike remains separate. The reference need not be motionless at pixel level. Clicks annotate instructions and user declarations only; they never establish visibility, object identity or ground-truth rectangles. Review the actual source pixels before any predictions.

## Selection and independent labels

Preserve all attempts in capture order. Select the earliest event satisfying the existing source-only preflight: 2–32 fully covered consecutive samples, ten visible/partial samples before and after, and evaluation exactly hiddenStart-10 through returnFrame+20 with no skipped samples. Select the earliest disjoint 24-sample unobscured reference window before evaluation, first frame visible and at least 12 fully visible scored frames. Exact image bytes may not overlap. The lookalike must be separately labelled in at least five evaluation samples, including during cover and three after return. Every selected sample and cover boundary must be reviewed at original resolution. Actual timestamp gaps remain; gaps exceeding the existing importer limit fail eligibility. Partial cover is not complete cover; out-of-view is not cover. Rejected attempts remain visible. Do not adjust boundaries after tracking results.

Unlike the public benchmark, webcam rectangles have no externally supplied truth. Source-only manual rectangles and visibility are explicitly attributed to the reviewer, with user confirmation of the distinct physical objects and actions. They are independent of tracker predictions, but are not independently instrumented identity or metric calibration. Ambiguous target/competitor identity is labelled uncertain. The absence of instrumented truth and modest 2 Hz sampling remain limitations.

Before predictions, commit source hashes, labels, selected ranges, event rationale and receipts without private pixels. Use the existing source-only preflight and unchanged actual-app comparison harness. Original-source replay, donor admission, independent arithmetic, monitor equality, recovery, tamper rejection, export and world preservation checks all remain required. Donor refusal means reference-dependent qualification is unavailable; no fabricated appearance descriptors. No favorable-outcome replacement recordings.

## Reporting

Keep source eligibility, actual appearance admission, completed execution and useful recovery separate. Count correct returns withheld, accepted wrong objects, hidden-frame outputs and missing positions. A source-only recorder check is not a tracking pass. Record actual complete workflow cost. Documentation screenshots use camera-free status/results; private camera pixels are never published. No method promotion is authorized by this study.
