# Edition 215 reproduction and next experiment

## Propose and run one observation

1. Select a tool-capable local Ollama model and ask **“What observation would distinguish these two explanations?”**
2. Enter two explanations that make different predictions in the original Relay Ridge scene. For example, compare disabled Right input with working Right input and provide disjoint horizontal-displacement intervals after 24 ticks.
3. Ask the model for one observation. Review its proposed input, two predicted ranges, and the pinned course. Java checks the range separation, the observation and the run budget before offering approval.
4. Select **Approve and run**. Game Lab starts in a separate runtime and settles for 24 ticks. It applies the exact named probe, records the positions, then repeats the same procedure with a new game session.
5. Read the outcome for each prediction and compare the original and replay positions. The report includes a checked Game Lab frame from the measured tick and the evidence JSON for both sessions.
6. Say **“Resume my last hypothesis experiment”** after a restart. It reopens the saved review or rechecks the completed report. An interrupted approved run can resume using the same review. A completed primary measurement is retained while the replay is retried.

The two supported observations are horizontal displacement after holding Right for 24 ticks, and vertical displacement after pressing Jump in place for 12 ticks. Each runs twice, for at most 96 simulated ticks across two sessions, including the settle. Java measures displacement from the settled position. The assessed interval spans both measured values with 0.0001 game units added on each side. A fully contained interval supports a prediction. A disjoint interval conflicts with that prediction. A partial overlap is inconclusive. These are simulated game units tied to this course and input.

## Reproduce

With the Java and Electron distributions prepared and local Ollama running, exercise both chat paths.

~~~sh
node workspace-desktop/test/hypothesis-experiment-integration.cjs build/edition215/right
node workspace-desktop/test/hypothesis-experiment-integration.cjs build/edition215/jump jump
~~~

The live test defaults to the installed gpt-oss:20b model. Set FRAYNIX_EXPERIMENT_MODEL to select a different installed tool-capable model. The cross-platform Java test supplies a known proposal at the model boundary and runs the real game and replay. Both tests keep the active Game Lab session separate from the experiment.

## Next acceptance test

Give the model two safe probes that could plausibly distinguish the same explanations. Have Java rank them by how much the predicted ranges separate for the time spent. Test whether the first choice stays useful when the course or starting position varies, and retain inconclusive choices alongside successful ones.
