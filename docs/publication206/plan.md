# Build, verify and share a change story

1. Finish two classification routing plans and export each verified CSV. Match the rows by their original key and output unit, then export the matched comparison.
2. In Chat send **Summarize the last verified matched comparison CSV.** Java rechecks the original inputs, matched rows and source receipt.
3. Send **Build a change story from the verified comparison totals.** In the canvas, select a bar to see the earlier and later source rows. Try the unit and status filters and reset the view.
4. After restarting, send **Recheck the verified change story.** The request uses the comparison associated with the saved story.
5. Send **Export the verified change story.** Fraynix runs the browser checks again and saves the exact verified HTML to the path shown in Chat. The file contains the input evidence and can be opened in a browser without Fraynix or internet access.

## Reproduction

Prepare the application with the [installation guide](../INSTALL.md), then run:

~~~sh
node workspace-desktop/test/change-story-integration.cjs build/change-story
~~~

The test starts with an empty project and teaches both conversions through Chat. Its synthetic datasets include negative and positive changes, a zero, one duplicate, one added group and one missing group. It measures rendered bar widths and checks the source readings, uses a keyboard to select a bar, rejects changed source evidence and tests the exported HTML in an isolated browser window with HTTP and HTTPS blocked. The export is the only file in the directory opened for that check.

## Next candidate

Let a reviewed Chat request choose which checked sections to assemble into a single briefing. Verify each section against its source and keep the original inputs available in the portable result.
