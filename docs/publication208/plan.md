# Build one briefing from two reviewed comparisons

1. Export and summarize two distinct verified matched comparisons in Chat. Each summary must pass its own source and browser checks.
2. Send **Review a two-source comparison briefing with totals, chart and rows.** Fraynix proposes the two most recently summarized distinct export IDs. Check the source order and sections, then choose **Approve and build**. A newer export cannot change this review.
3. Use **Source 1** and **Source 2** in the canvas to switch between the two comparisons. Each source has its own totals, chart scale, row table and downloads.
4. Use **Resume the comparison briefing** to return to a pending review after restarting. Use **Recheck the comparison briefing** to rerun the checks for a completed briefing.
5. Use **Export the comparison briefing** to save one HTML file with both sets of evidence. Moving only that HTML file to another machine preserves the tabs and downloads.

## Reproduce

Prepare the desktop with the [installation guide](../INSTALL.md), then run:

~~~sh
node workspace-desktop/test/change-story-integration.cjs build/paired-briefing
~~~

The fixture creates four verified routing plans and two independent matched comparisons. It checks the prior single-source workflow, rejects a copied CSV under a new export ID, resumes approval after restart and rechecks the original pair after a second restart. The test changes each comparison independently and alters one retained summary receipt. These failures identify the affected source and create no export. The offline test verifies both rendered sources and the exact bytes of all six downloads.

## Next candidate

Allow a reviewer to select from older verified sources and decide which sections belong to each source. Check the resulting order and provenance in the exported briefing.
