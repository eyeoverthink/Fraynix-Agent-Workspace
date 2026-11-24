# Edition 211 reproduction and next test

## Try the chart request in a fresh project

1. Import the [Delfys validation recording](../datasets/delfys75-013/validation.csv) into Motion data. Map `time_s` as seconds and `displacement_m` as metres. Ask, “How far did it travel in the selected motion recording?”
2. Ask, “Make a chart of that.” With one verified source, the chart opens in the canvas. The downloaded `source.json` contains the mapped samples, series, source hash, original verification hash and chart coordinates.
3. Save two equal-duration setups in Ramp input. Ask, “Which of these two ramps will the block go farther down?” Wait for the verified comparison.
4. Ask, “Could you make me a chart of that?” Fraynix asks which result you mean. Close the desktop with the question open.
5. Reopen the same project and select **Chart request**. Choose the ramp comparison and select **Verify and open chart**. Open **Last chart** after another restart to recheck the original evidence and open the same artifact.
6. For refusal testing, use a test project. The integration test changes the original ramp report after it was verified and asserts that the affected chart refuses to reopen.

## Verification

Run `node workspace-desktop/test/chart-followup-integration.cjs build/chart-followup` after preparing the workspace. The test uses the real desktop and Java runtime with the model catalogue disabled. It independently derives expected coordinates from the source samples, checks every visible polyline, archives one source, reopens both a pending question and an already-selected request, and tests stale selections and missing browser evidence. The portable workflow runs the same test on Windows x64, Apple silicon macOS, Intel macOS and Linux x64.

The source choices are frozen when Fraynix asks the question. A later result can appear in a new request. For the current request, use **Discard request** if you need a different set of sources. Completed charts remain saved as versioned canvas artifacts. **Last chart** rechecks the current chart request against the original source and the retained browser receipt.

## Next acceptance test

Support a named source and an explicit chart question after a verified table comparison. Keep units separate when datasets differ. Verify that both the selected rows and display units survive restart, even if a newer source is added between the request and the chart.
