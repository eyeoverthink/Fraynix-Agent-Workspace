# Edition 265 implementation and reproduction

## Avatar Studio

An explicit chat request opens the studio and preserves the original request while a portrait is missing. The desktop rasterizes the selected local image or decoded video frame into bounded PNG. The crop and mouth point are editable by mouse and numeric input. Chromium decodes a separate audio file or audio from a supported clip into at most thirty seconds of mono 24 kHz PCM. A separate WAV works when the source video’s audio codec is unavailable.

Java checks the face crop and audio, builds a transparent game character, and runs a constrained image warp around the mouth. The five poses keep the rest of the original still image. Java measures short-window RMS amplitude at 24 Hz and maps it to the pose levels. The preview clock is the actual audio player. Java builds MJPEG/PCM AVI and verifies the indexed frames and audio bytes before returning the export. The exported walk sheet includes four frames from the character recipe.

The editable project binds the original normalized PNG, reusable crop and mouth measurements, canonical audio and regenerated output hashes. The recipe is addressed by the hash of its retained bytes. Opening or exporting it checks its identity and regenerates the expected outputs. Import checks those outputs before it writes anything. Tile Workshop saves and verifies the transparent idle sprite through its ordinary library intake.

## Acceptance

- The Windows Electron integration used an independently drawn robot and an audio fixture with silence, a quiet tone, a louder tone, and a second silence.
- The test verified silent and loud mouth frames, a transparent background, 96 × 112 sprite dimensions, restart replay, and the source hash in the Tile Workshop library.
- The local video export rebuilt byte-for-byte after restart and after import into a clean project. A changed saved recipe and a modified rig with stale hashes were both refused.
- FFprobe independently read 48 video frames at 24 fps, a 48,000-sample PCM track and exactly two seconds in the example AVI.
- The prior unified workspace integration passed again. The portable workflow includes the new test in the desktop matrix.

## Reproduce

Run `./gradlew :workspace-engine:installDist` on macOS/Linux or `gradlew.bat :workspace-engine:installDist` on Windows. Install desktop dependencies with `npm --prefix workspace-desktop ci`, then run `node workspace-desktop/test/avatar-integration.cjs build/avatar-test`. On Linux without a display, run the desktop test with `xvfb-run -a`.

[Edition 264](../../README-264.md) · [Screenshots and robot example](../readmes/265/index.html)
