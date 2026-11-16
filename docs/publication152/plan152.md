# Edition 152 — orientation search without returned target locations

Baseline: 7372e9ed3279ceb7667b24850015425a3aeaf16a. Date: 2026-11-16.

CI checked first: run 43847306920 failed six replay-cache checks because Windows
path canonicalization rejected a differently spelled temporary path. Correct
real link detection, retain byte verification, and test real redirected paths.

Add a separate Java experiment that searches the whole frame at rotated poses
using only the initial crop and explicitly verified starting appearances. No
evaluation boxes, visibility labels, predicted positions or category names enter
the search. Bounded coarse-to-fine translation, rotation and scale proposals are
checked using the unchanged 149 full-object acceptance thresholds. Four adjacent
same-reference observations and optical flow are required; detected competing
matches remain a persistent veto. No appearance admission or physical authority.

Keep legacy-flow defaults and historical 129–150 tracker/audit code identities
unchanged. Expose the experiment through the existing comparison panel, with its
own source-bound deterministic receipt and full recomputation on verification.
Independent labels score outputs only after the complete trace is generated.

Development controls: generated translated/rotated structured patches, negative
patterns, full occlusion and simultaneous identical copies. Freeze the method
before new reserved controls and physical footage. Existing 150 recordings are
exposed diagnostics, never fresh tests. Physical capture awaits current setup
availability; no invented footage or ground truth. Keep all attempts and failures.

Measure complete search/replay costs and preserve original source bytes, world
state, prior numbered editions and private camera data. Publish the actual results
and limits, screenshots, offline guide, linked notes and source/evidence hashes.
Push main and edition152/readme-152, fetch and compare. No subagents; one heavy job.

## Declared runtime correction after the first app trials

The initial method at 86591529 completed the four new generated controls. The
search grid/thresholds were frozen before those trials. The exposed camera
calculation outlasted a single browser request. Final revision 7bccd697 adds one
background worker, progress polling, bounded status retention and interruption
refusal. It does not alter search/scoring mathematics. All four generated traces
were repeated and compared exactly; they are now regression data, not newly held
out again. Every failed harness/timeout attempt remains separate.

No fresh webcam setup was confirmed during this implementation; do not substitute
old footage for that gate. The original150 recording is exposed diagnosis only.
The camera result has better target proposal coverage but no accepted return.
