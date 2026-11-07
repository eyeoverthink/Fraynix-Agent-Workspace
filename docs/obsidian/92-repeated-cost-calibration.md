# Repeated complete-task cost calibration

**24/24 held-out evaluation goals completed; 8/8 calibrated decisions had matching repeated estimates.** Memory was selected in 0/8 decisions. Every requested skill and exact committed world state survived reopen. These are fresh current-code simulation results, not historical timings relabelled as new evidence.

| Condition / case | Actual saving | New predicted saving | New interval width | New hit | Old hit |
| --- | ---: | ---: | ---: | --- | --- |
| cold-run / 084-cold-run-held-a-r0 | -2.60 s | -2.42 s | 8.36 s | yes | yes |
| cold-run / 084-cold-run-held-b-r0 | -4.19 s | -2.42 s | 8.36 s | yes | no |
| cold-run / 084-cold-run-held-a-r1 | -3.24 s | -2.42 s | 8.36 s | yes | yes |
| cold-run / 084-cold-run-held-b-r1 | -2.76 s | -2.42 s | 8.36 s | yes | yes |
| cold-skill / 084-cold-skill-held-a-r0 | -4.09 s | -2.03 s | 11.57 s | yes | no |
| cold-skill / 084-cold-skill-held-b-r0 | -2.34 s | -2.03 s | 11.57 s | yes | yes |
| cold-skill / 084-cold-skill-held-a-r1 | -2.12 s | -2.03 s | 11.57 s | yes | yes |
| cold-skill / 084-cold-skill-held-b-r1 | -1.78 s | -2.03 s | 11.57 s | yes | yes |

New intervals covered **8/8** measured savings; the old arithmetic covered **6/8** on the same data. Mean interval width: new 9.96 s, old 2.48 s. Mean absolute saving error: new 0.73 s, old 0.84 s. These eight comparisons are descriptive, not a calibrated confidence guarantee. Independent repeats mean distinct goal executions; hardware noise is not assumed statistically independent.

The repeated estimator covered 8/8 held-out savings versus 6/8 for the previous arithmetic. Treat that together with the interval widths and complete-cost totals above; this small study does not establish a general speed gain. The next gate is calibrating a growing bank without changing the identity of the measured procedure, then measuring interrupted/resumed workloads separately. Reuse setup only when a bounded admission check predicts enough remaining task benefit to repay calibration and original-evidence reading. Test that decision on untouched longer sequences and report failures as well as successes. Preserve the current proof and world-commit boundaries.

[[README-084.md|Edition 084]] · [[docs/readmes/084/index.html|Offline evidence]]
