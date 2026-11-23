# Choose older verified comparisons and their sections

1. Export and summarize at least two distinct matched comparisons. The picker uses saved verified summaries in the project.
2. Send **Review a two-source comparison briefing.** Choose the comparisons for **Source 1** and **Source 2** in the order you want them exported. Use **Load older comparisons** when needed.
3. Choose **Totals by unit**, **Interactive change chart** or **Source rows** independently for each source. Each source needs at least one section. Click **Review selections** and confirm the two IDs, their order and the chosen sections.
4. Choose **Approve and build**. After restarting, **Resume the comparison briefing** reopens a pending review. **Recheck the comparison briefing** reruns the completed checks.
5. Send **Export the comparison briefing** to save one offline HTML file. Tabs, source labels, download names and provenance follow the approved order.

The older command with a shared section list still uses the last two summaries. Existing saved briefings continue to recheck against their original HTML and receipts.

## Reproduce

Prepare the app with the [installation guide](../INSTALL.md), then run:

~~~sh
node workspace-desktop/test/change-story-integration.cjs build/selected-briefing
~~~

The fixture creates three distinct verified comparisons. It pages past 24 newer exports with no verified summary, then deliberately selects two older comparisons in reverse chronological order. The test restarts before approval and after the build. It checks visible sections in the offline file and compares all six evidence downloads with their approved source bytes.

## Next candidate

Let a reviewer reuse a proven briefing layout on fresh verified comparisons while choosing the new sources explicitly. Verify every source handoff and preserve the original briefing.
