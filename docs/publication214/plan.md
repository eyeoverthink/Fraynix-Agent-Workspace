# Edition 214 reproduction and next test

## Audit one proposed explanation

1. Open a verified named comparison chart and say **“Brief me on that chart.”** The briefing retains the selected rows. [Edition 213](../../README-213.md)
2. With the briefing visible, say **“Audit a proposed explanation for that briefing.”** Select one nonzero change. Enter your hypothesis, assumptions, and the largest difference from the original change that would count as agreement.
3. Save a draft if the observations are still being collected. After restarting, say **“Resume my last explanation audit.”**
4. Attach a UTF-8 CSV with one row per treatment/control pair. Use the selected claim’s unit, unique run IDs and source references, a timestamp with a time zone, and a nonnegative error bound for the adjusted effect. The observation file can contain 1–64 trials and be at most 64 KiB. [Sample CSV](../readmes/214/observations.csv)
5. Open each trial in the report to see the input readings, adjusted effect and interval. Reopening the last completed audit after restart rechecks the original briefing and trial evidence.

The app subtracts each control change from the matching treatment change. The result is an adjusted trial effect, and its interval is that effect plus or minus the declared error bound. The predicted range is the original cited change plus or minus the selected tolerance. Two or more fully consistent trials, with no unresolved or conflicting trial, support the numerical prediction. Any conflicting trial is reported. An overlap or too few consistent trials can leave the result inconclusive. The interpretation depends on trial design, references, and error bounds supplied by the user.

Original row and receipt checks still apply. The app retains the exact observation CSV, the earlier claim evidence, and a machine-readable audit with dates, inputs, bounds and the checked rendering. If the original comparison or a stored receipt changes, the audit refuses to reopen as verified.

## Reproduce the result

Build the Java distribution and install the desktop dependencies using [the installation guide](../INSTALL.md). Then run:

~~~sh
node workspace-desktop/test/change-story-integration.cjs build/edition214/change-story
~~~

The suite creates a temporary workspace and disables external model use. It exercises the supported, mixed, conflicting and inconclusive outcomes; restarts both before and after report creation; changes source files and browser evidence to confirm refusal; and opens the published report with network requests blocked. The cases are synthetic and have hand-calculated expected effects.

## Next acceptance test

Ask **“What observation would distinguish these two explanations?”** from an open audit. Let a local model propose one measurable condition and predict different outcomes for both explanations. Show the test budget, source and expected intervals before approval. Start with a bounded Game Lab experiment whose actual observations can be collected and replayed. Reopen the resulting audit after a restart and show exactly which explanation gained or lost support.
