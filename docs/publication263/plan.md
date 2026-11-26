# Edition 263 implementation and reproduction

## Mission design

The desktop decodes ten seconds from a local MP4, MOV or WebM through Chromium. The Java mission retains the selected video bytes, the hashes of 80 sampled JPEGs, the environment GLB identity, the selected anchor frame and the operator’s labelled correspondences. A 640 × 360 coordinate frame includes the letterbox when the recording is portrait. Longer or larger recordings can be trimmed locally to under 24 MiB before intake. An optional original-file digest and source offset are attributed to the preparation step. The mission independently rechecks the ingested file and decoded samples.

Java fits a normalized projective camera with noncoplanar points and rejects a reflected projection or one that misses fit or held-out landmarks by more than five pixels. It runs true triangle rays toward the supplied desk point and through the centre and edges of the proposed doorway path. This gives a model-space sightline and a height-specific clearance check. A different live camera pose needs fresh evidence.

The mission checkpoints preparation, anchoring, retained samples, verified preview and local calendar draft. The silent MJPEG AVI repeats each 8 Hz source sample three times in a 24 Hz stream. Java checks the RIFF stream and hashes the result. The desktop plays the same retained samples and exports the exact AVI and ICS bytes. When calibration refuses, the operator can explicitly continue with the footage and draft. The resulting mission records an unresolved anchor and has no qualifying clearance receipt.

A bounded busy-window snapshot supplies calendar evidence. The app chooses the first free thirty-minute quarter-hour slot and writes a tentative ICS file. The live apartment run supplied a read-only Google Calendar free/busy response. The workspace builds a local draft with no attendees. The included public fixture uses invented busy windows solely to exercise conflict handling. Public evidence contains no private frames or calendar intervals.

## Acceptance

- The public studio fixture checked a visible marker in the decoded recording, recovered the authored camera and agreed with two independent landmark positions. The anchor is bound to frame eight and its exact JPEG digest.
- The desk sightline and the three doorway rays passed in the public test geometry. A shifted held-out label refused the anchor. The explicit footage-only path left that anchor unresolved.
- Restart after anchoring resumed the same mission through chat. A second restart reopened the completed mission and identical draft. An altered saved frame refused export.
- FFprobe read 240 MJPEG frames at 24 fps and a duration of ten seconds. The actual apartment preview had 80 distinct decoded images.
- The apartment run recovered the preview and tentative draft after restart. Its doorway-and-desk candidate failed the 5 px limit, with fit maximum 49.2 px and held-out maximum 32.8 px. Physical placement and clearance still need better-matched model landmarks.

## Reproduce

```powershell
.\gradlew.bat :workspace-engine:installDist
npm --prefix workspace-desktop test
node workspace-desktop/test/desk-recording-integration.cjs build/desk-recording-test
ffprobe -v error -show_entries stream=codec_name,nb_frames,r_frame_rate:format=duration -of json build/desk-recording-test/desk-preview.avi
```

Use `./gradlew` on macOS or Linux. Run the desktop integration with `xvfb-run -a` on Linux without a display.

[Edition 262](../../README-262.md) · [Public studio evidence](../readmes/263/FILE_TREE.md)
