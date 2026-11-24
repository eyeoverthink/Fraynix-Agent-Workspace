# Edition 213 reproduction and next test

## Build the briefing

1. Make a named comparison chart through [edition 212](../../README-212.md). Use the verified comparison and the exact row keys you want to explain.
2. With that chart open in the canvas, ask **“Brief me on that chart.”** Fraynix rechecks the chart, its original comparison and the saved selection before writing the briefing.
3. Expand a citation under a sentence. It shows both original source row numbers and their readings. Unit totals stay separate. If the chart selection is empty, the briefing has no claims.
4. Restart the desktop and send **Open my last chart briefing**. The saved request still points to its original chart, including when a newer chart exists. Use **Brief me on the last comparison chart** to deliberately switch to the most recent chart.
5. Download the claim evidence or follow **Open the original chart**. The files travel together and work offline.

Ask **“What caused the changes in that chart?”** to see the separate-evidence request. The comparison supports descriptive changes. A future causal workflow will need independently checked evidence for each explanation.

## Verify the workflow

After preparing the Java and desktop runtimes, run `node workspace-desktop/test/change-story-integration.cjs build/chart-briefing`. The same run covers comparison stories, paired briefings, named charts and this cited briefing. The new tests compare every claim with hand-derived amounts, verify the retained original chart receipt, test changed receipts and CSVs, reopen both a saved and a pending briefing, and inspect citations and the original chart in a window with networking blocked.

## Next acceptance test

Let a user attach an independent observation to a named briefing and propose a precise claim that links it to a change. Preserve source dates and uncertainty. Require the claim verifier to distinguish a supported explanation from an untested hypothesis, then test the boundary with conflicting and missing observations.
