# Edition 056 acceptance contract

Frozen before implementation, November 3, 2026. Published baseline: 781fb85f792a570e4b974feb4094b1202e8d724a. Separate edition056 checkout and synthetic projects; preserve the installed review runtime, private data and immutable editions001–055.

## Measured load envelope

Extend the existing independent native load engine. Use the released assembly's exact poses, velocities, dimensions and materials in separate copies. Apply downward force at a body-fixed point at90% of the beam's positive half-length. Use0,8,16,24,32,48,64N, each for seconds1–3 of a6-second240Hz run. Retain every integration tick, applied force/offset, original motion, support contacts and work. No pose writes after initialization. Existing055 stability thresholds remain unchanged: at most0.05m beam displacement,5degrees tilt,0.08m/s final-window speed, and both supports in all20final-window samples. Offset-work reconstruction may use2e-5J tolerance for native float rotation; central055 tolerance remains1e-7J.

Report a tested bracket, never an exact continuous strength limit: largest stable prefix and first unstable tested force, with explicit non-monotonic outcomes. Repeat each bracket endpoint twice from the identical released source, retaining all runs. If all tested forces pass, report only a lower bound at64N; if zero fails, report no stable tested baseline. This is rigid-body stability under a declared force/duration/application point, not material strength, calibration or statistical confidence.

## Bounded redesign after actual failure

Use the existing Java goal/counterfactual owner. Try the original support spacing first at the admitted gripper force. Only failure to verify the requested load (default24N, selectable from the declared nonzero levels) admits wider support spacing: halfway from original to maximum allowed span, then maximum span. Maximum span preserves the existing0.08m beam-end margin. Same stock, mass, friction, source and task centre; no fabricated parts or stronger material. Each candidate earns the original native motion, wrench, release, obstacle and structure checks, then its independent measured envelope. Skip later candidates only after a verified load success; retain failed/interrupted attempts. At most3candidate layouts. No successful candidate means no live-world ownership or commit.

The chosen layout must execute freshly in the continuing world, pass the original forecast-versus-actual checks, repeat its load envelope and satisfy the requested force before report/skill/world promotion. The failed baseline remains visible/exportable. Every tested force and all assembly+load costs are counted. Chat and versioned procedures preserve the required force and load policy; reuse reruns against the new source. Explicit review/restart, Stop, cold recovery, tamper rejection and closed055 compatibility are required.

## Frozen demonstration and publication

Wide054 stock with initial span1.0m, centre(0,0),40N/4Nm gripper, requested24N. Compare initial, midpoint and maximum spacing by actual outcomes, retaining contrary results. Repeat a changed centre and compact stock; test a no-redesign requested-load refusal and an obstruction refusal. Publish actual screenshots, measured limits, failures, original evidence, newREADME/offline guide, memory/log updates. Push main/edition056/readme-056 without force and audit a fresh fetched GitHub tree. No new libraries, cloud services, stubs or learned-model claims.

## Recorded contrary result and new declared case · 2026-11-03

The original24N experiment produced verified measurements but no feasible redesign: initial1.0m, midpoint1.495m and maximum1.99m spacing all had a stable tested prefix16N and failed24N. Wider layouts retained support but exceeded the unchanged0.05m motion threshold. The initial expectation that at least one would pass was wrong; retain its failing invocation and every original trace. This becomes the no-feasible-candidate acceptance case; never lower its requested force during execution.

Before the next run, declare a separate case: same wide stock, original span0.8m, requested16N, centre(0,0), same40N/4Nm controller and unchanged full force sweep/thresholds. Hypothesis: the original narrow support footprint fails16N and a wider layout passes. If the measurements disagree, record that outcome. The UI example explicitly selects this16N task; the general default requirement remains24N. This is a new development case, not held-out generalization or a softened version of the24N task.
