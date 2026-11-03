# Edition 047 plan

Edition 046 can already arrange bodies in a persistent world, verify a report, save a procedure and connect multiple procedure runs. The fresh-project audit showed that people could reach the graph composer without a way to create the prerequisite skill from that screen.

| Phase | What to build | Evidence required |
| --- | --- | --- |
| 047 · First verified skill | Give the skill library and the empty graph composer a route to a labelled world exercise. Keep the result on screen with a link to its report and open the composer with the verified world and procedure selected. | Starting from a blank project, run real native motion, check the report, save exactly one procedure and see it after reopening. A stopped run must earn no skill. |
| 048 · Memory that survives | Extract the portable Carbon restore and packed HDC primitives into the Java brain. Separate observation, training and read-only recall. | Round-trip every learned accumulator across a fresh JVM. Compare HDC with exact and lexical search on held-out requests. Keep it advisory until it improves selection or useful abstention. |
| 049 · Evidence bench | Add a replayable observation stream. Pair the existing webcam and an ESP32-CAM with one independently timed Arduino sensor. | Measure alignment, dropouts, displacement and false reacquisition on held-out slides. During occlusion a predicted path must stay visibly distinct from a sensor-confirmed crossing. |
| 050 · Learn from disagreements | Freeze predictions before actions. Compare them with actual native outcomes or calibrated observations. Give a retained skill a counterexample and a controlled chance to improve. | Evaluate current policy, simpler baseline and candidate on the same untouched cases. Retain failures, regressions and rollback. |
| 051 · Offline creation | Build a reusable file-to-artifact skill that validates an input, transforms it with Java-owned tools and tests the exported result. | Run the same typed skill on a second file after restart. Preserve inputs and output hashes. Refuse incompatible schemas and stale bindings. |

Two ideas to carry through these phases. A **world fork** gives the agent a disposable place to try competing plans while the original world keeps its current revision. A **disagreement inbox** turns an observation that contradicts a prediction into a replayable case with its clocks, source, calibration and result. Both should use the existing mission, evidence and skill lifecycle.

047 uses the existing edition 035 labelled source and existing Java world, goal, report and procedure handlers. Reuse the native and Chromium checks before enabling the composition step. Place an explicit run action before creating the synthetic world. Keep interrupted runs in the ordinary Java goal lifecycle. Test in an isolated checkout and isolated project.

