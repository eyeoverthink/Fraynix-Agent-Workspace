# Edition 149 fresh camera protocol

Prepared after the user confirmed that the fixed webcam, black target lighter and red comparison lighter are available, and before any new camera preview or pixels. Production remains frozen at 9090c62d with 124 source/harness hashes in OBSERVATION_149_LOCK.json. This adds a source-only recorder and eligibility rules; it does not retune the tracker.

## Capture

Use the same fixed top-down camera and mat. Keep the red lighter still and separately visible. No microphone, tracking, model call or network upload during capture. Raw images and camera identifiers stay in ignored local build storage. Public documentation may contain numerical outcomes, hashes, manual labels and camera-free UI captures.

1. User starts private preview, verifies black target and red comparison, then starts the clear reference. Save at least 34 continuous samples at approximately 1 Hz.
2. Cue COVER. User covers only the black lighter with an opaque small cover and clicks Covered with hand away. Record at least 18 further samples before showing the next cue.
3. Cue MOVE UNDER COVER. Keep the black lighter hidden while sliding it one large mat square toward the tape. Leave the red lighter still. Uncover the black lighter at that new position, remove the hand and click Returned. This action stage waits for the user; recording remains continuous.
4. Save 24 return samples, stop the camera automatically and preserve the complete journal. All actual timestamps and intervening frames remain. No clicks after Returned are needed. Five-minute, 600-sample and 120 MB recorder limits remain; processing/export limits are unchanged.

Button declarations are not visibility ground truth. Review every captured source frame before predictions. Label full/partial/occluded/uncertain views and separate other-object boxes. Confirm physical actions with the user. The red lighter differs in color and is not an identical-copy challenge.

## Prospective source eligibility

Use the first continuous 24 frames as the donor. Pick the first complete cover event after it with 18–60 fully hidden samples. Evaluation contains exactly ten preceding samples, the entire hidden interval and 21 samples beginning at re-entry; at most 91 frames, below the existing 96-frame dataset limit. The donor must precede the evaluation and their original image bytes must be disjoint. At least 12 donor measurements after its seed must be fully visible. The actual runtime still decides whether any appearance can be retained; source eligibility is not admission.

The evaluation seed must be fully visible. All ten preceding and first ten re-entry frames must be visible or partial. A fully visible return is required within those ten samples and at least eight of the 21 post-return samples must be fully visible. Its first fully visible returned target must already be displaced by at least half the original target width in normalized image coordinates. Labelled partial transitions stay in the evaluation, rather than being removed. The comparison object must be separately labelled during cover and on at least three return frames. No trimming inside the event, time rescaling, moving the donor after the event or replacement of failed outcomes.

Manually review every original frame and every selected frame at original pixel scale before running trackers. Freeze the complete source labels, hashes, selected window and eligibility result in Git before predictions. Manual image labels are independent of predictions but not independently instrumented truth or metric calibration.

If the source fails eligibility, preserve that failed attempt and explain the missing condition before a new recording. Do not change selection rules to admit it. If the eligible source fails tracking or has no donor admission, retain the outcome without retuning on it and calling it fresh. Compare unchanged 148, monitor 149 and candidate 149 on the same original pixels. Report correct/missing/wrong/hidden outputs, other-object matches, new observed segments, future prediction errors where labels exist, restart checks and complete costs separately. No default promotion follows from one camera test.

The existing 148 recording remains an exposed regression. Generated controls remain generated. All three evidence domains stay distinct.
