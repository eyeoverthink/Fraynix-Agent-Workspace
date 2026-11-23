# Edition 210 implementation and reproduction

## Run the same requests

1. Launch the workspace and use a separate project for examples.
2. In Ramp inputs, save two setups with the same duration and leave that input open in the workspace browser. Ask “Which of these two ramps will the block go farther down?” Java binds the source and measures each simulation on the same clock. The report includes both runs and their independent replays.
3. In Motion data, import the [recorded validation samples](../datasets/delfys75-013/validation.csv) with time in seconds and displacement in metres. Ask “How far did it travel in the selected motion recording?” The answer and interactive report use all accepted rows.
4. Ask “Turn this CSV into JSON, please.” Attach a CSV and choose **Confirm and verify**. The workspace opens the report and its downloadable JSON.
5. With an installed local model selected, ask “Read the selected motion recording and give me a checkable report on its travel and speed.” Choose **Confirm · Analyze motion** in Mission. To test recovery, close the desktop while awaiting confirmation and reopen the same project before confirming.
6. With that model available, ask “Could you whip up a little offline browser app that counts clicks and resets?” Java supplies the checks for the counter before the model writes code. Open the result in Mission.

## Tests

With the desktop and its Java distribution prepared, run:

- `node workspace-desktop/test/plain-language-integration.cjs build/plain-language`
- `node workspace-desktop/test/plain-language-integration.cjs build/plain-language-model --real-model`
- `npm --prefix workspace-desktop test`

The second command uses the local Ollama model `gpt-oss:20b` by default. Set `FRAYNIX_TEST_WORKFLOW_MODEL` to use another installed model with native tool calling. The portable workflow runs the model-free command and publishes its test report. The integration test starts a fresh local project, checks the numeric results and reopens saved evidence. Its sample inputs come from the recorded validation file and small authored ramp and CSV fixtures.

## Next acceptance test

Test unfamiliar phrasings from outside the development examples, including follow-ups such as “Would it go farther on the other ramp?” and “Make a chart of that.” Resolve the reference to one saved source and ask for confirmation when two sources fit. Measure correct procedure selection and safe clarifications across motion data, saved scenes, the local browser and creation. Include concurrent source edits and recoveries with the original source and output checks.
