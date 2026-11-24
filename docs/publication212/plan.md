# Edition 212 reproduction and next test

## Name and chart a comparison

1. Build and export a matched comparison using the [edition 201 workflow](../../README-201.md). In Chat, send **Summarize the last verified matched comparison CSV**.
2. Send `Name the last verified comparison "Workshop audit"`. To name an older summary by its exact export ID, send `Name comparison export <export ID> as "Workshop audit"`. Fraynix confirms the saved ID. A name already used for a different comparison is refused.
3. Ask `Chart gains in "Workshop audit" in millimetres`, or ask `Chart gains and losses from "Workshop audit" in both units for keys "fall", "rise <probe>" and "length gain"` for the example dataset. The selection uses exact, case-sensitive original keys. If you omit keys, Fraynix uses all matched rows in the requested units and direction.
4. Click a bar or focus it and press Enter. The chart shows the matched earlier and later source rows beside the selected bar. The download links provide the selected values and scales, original comparison CSV, and verified summary.
5. After restart, send **Open my last comparison chart**. Fraynix uses its saved selection, checks the original sources again, and reruns the saved chart checks if the chart already exists.

**Changes** includes steady matched rows. **Gains and losses** includes the nonzero matched deltas. A valid row with no matching requested delta produces an empty view. Fahrenheit and millimetres have separate scales.

## Verify the workload

Run `node workspace-desktop/test/change-story-integration.cjs build/named-comparison` with the Java and desktop runtimes prepared. The fixture creates two genuinely different matched comparisons, names the original one, verifies the selected marks in desktop Chromium, and checks the exact inputs and browser receipt again after restart. It opens the saved chart with networking blocked, checks a keyboard selection, and downloads the selected values byte for byte. The same integration run also covers the earlier change stories, explorers and briefings.

## Next acceptance test

Build a short briefing from a named chart in the same conversation. Verify every value and row citation against the pinned selection. Keep claims about why something changed tied to separate evidence about the cause.
