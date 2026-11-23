# Approve and export a comparison briefing

1. Export a verified matched comparison CSV and send **Summarize the last verified matched comparison CSV.**
2. Send **Review a comparison briefing with totals and chart.** The supported sections are **totals**, **chart** and **rows**. The review shows the chosen sections and original comparison. A pending review can be reopened with **Resume the comparison briefing.**
3. Choose **Approve and build**. Java checks the saved approval, original export and verified summary. The browser checks the approved sections and the evidence links.
4. Send **Recheck the comparison briefing** to repeat the saved checks. Send **Export the comparison briefing** to save the verified HTML. The original CSV and proof downloads work from the standalone file.

## Reproduce

Prepare the app with the [installation guide](../INSTALL.md), then run:

~~~sh
node workspace-desktop/test/portable-smoke.cjs build/portable-smoke
node workspace-desktop/test/change-story-integration.cjs build/comparison-briefing
~~~

The portable smoke injects EPIPE on each desktop output stream, then completes the guided proof. The briefing test builds on synthetic routed data in an isolated project. It tests an unapproved request, a stale review digest, restart during review, two section combinations, actual offline rendering and exact CSV download. Changing an original comparison blocks later exports.

## Next candidate

Build one reviewed briefing from two independently verified sources. Verify each section against its own source and preserve a distinct refusal when either source changes.
