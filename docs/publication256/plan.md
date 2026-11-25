# Edition 256 implementation and reproduction

A creative project records its next intention and explicit approvals. The Java engine reads the approved reference's first two shots, applies the requested pacing adjustment, optionally applies a saved opening choice, and renders a draft from two fresh picture sources. The draft is bound to the reference and checked source bytes. A proposed cut can be resumed after restart. Approving the cut records the result and any requested reusable opening choice in the same project update.

The one reusable choice changes opening duration by four frames before source-duration limits. Quicker and slower requests change the two reference timings by two frames. A draft correction can shorten the second shot independently. The drawer shows requested and actual opening timing and links to the cut that taught the current choice. The project can restore the previous approved choice or run with style disabled.

A family or public copy requires a separate review of the exact rendered AVI. Playback reads its actual JPEG and PCM tracks, validates the embedded sound against the timeline mix, and uses the audio clock to show frames. Each source in the approved cut must be individually cleared for that named audience. Changing the selection creates a new review identity. The exporter rechecks the source and output hashes and copies only the AVI with a generic filename. The project stores the approval internally. Sharing with another person is a separate action outside this workflow.

## Acceptance

| Behavior | Evidence |
| --- | --- |
| Resume | Reopened the original intention, approved references, and an unapproved corrected draft across process restarts. |
| Ambiguity | Multiple approved references raised a choice with previews. Conflicting requested audiences asked for clarification. |
| Learning control | Held the request, reference, picture footage and sound constant. Independent decode found the second shot at frames 10 and 14 with pacing off and on. A one-off shorter opening did not carry forward. A shorter opening saved on approval carried through restart. Undo restored the prior preference. |
| Project isolation | A second project rejected the first project's reference and calculated timing from its own approved cut. |
| Private controls | The master reopened with matching file hashes. The family directory contained one standalone AVI. A private picture control was absent at its independently checked pixel in every frame. Every output PCM sample matched the permitted source. FFprobe found no stream or container tags. |
| Export authority | The runner refused an unapproved cut, incomplete clearance, stale audience approval and changed retained source bytes. An approved family copy survived a restart before export. |

## Reproduce

```powershell
.\gradlew.bat :workspace-engine:installDist
npm --prefix workspace-desktop test
node workspace-desktop/test/creative-project-integration.cjs build/creative256
node workspace-desktop/test/creative-project-ffmpeg.cjs build/creative256
node workspace-desktop/test/timeline-editor-integration.cjs build/timeline255-regression
```

Use `./gradlew` on macOS and Linux. On Linux without a display, run the Electron acceptance test under `xvfb-run -a`. FFmpeg and ffprobe are independent test readers. The application renders and verifies with Java and runs without a model.

[Edition 255 portability run](https://github.com/eyeoverthink/Fraynix-Agent-Workspace/actions/runs/45711279303) · [Measured result](../readmes/256/creative-evidence.json) · [Independent decode](../readmes/256/independent-creative.json)
