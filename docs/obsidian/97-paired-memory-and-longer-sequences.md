# Paired memory and longer sequences

**Reading this result:** the new selector reduced charged native ticks by **15.54%** and task-call time by **7.97%** relative to the previous selector in this study. However, its calls took **38.02 s longer than simple control** and **104.48 s longer than fixed memory**, even before charging fitting. The unchanged gate did not reject a profitable fit at any measured prefix, against either comparator.

The new empirical range contained only **3/12** observed simple-minus-memory savings: **5 fell below its lower bound** and **4 exceeded its upper bound**. Fixed memory was still faster than simple control on every matched case, but the later measured advantage was much smaller. These results support the changed procedure choice over the old selector; they do **not** establish reliable uncertainty bounds, general speed improvement, or calibration repayment. Time order, background load and changing task conditions were not independently randomized.

**Next evidence needed:** detect stale cost estimates, test mixed task families where memory helps and hurts, and use fully counterbalanced repeated measurements. Keep fixed verified memory as a comparator. Any attempt to reduce model-reading overhead must retain original-source verification. Do not narrow reserves or loosen the spending gate to make this completed study appear profitable.


**48/48 held-out goals completed; all76 newly executed native goals and three actual fitting calls were inspected.** Paired selection used memory **12/12** times; the previous selector used it **0/12** times. Its observed twelve-task call saving against the previous selector was **102.17 s**. At horizon12, net after one fit and review against simple: **-154.14 s**; useful fitting rejected: **False**. At horizon12, net after one fit and review against memory: **-220.60 s**; useful fitting rejected: **False**. This is one nearby synthetic task family on one machine, not a general speed or physical accuracy claim.

[Full edition](../../README-089.md) · [Offline explorer](../readmes/089/index.html)

**240 unique Java checks across 32 classes, 135 desktop checks and eight accounting checks passed.** The first full Java invocation found one invalid new test fixture: the old adaptive reference was missing its `lowerSavingMillis` input. The fixture was corrected without changing production code; all18 tests in that class passed in a second focused invocation. The other31 classes passed on the first invocation. Both original logs/XML and the combined result are retained.

**328 prior engine classes are byte-identical to088.** Only `WorldProcedure079` integrates new clocks/selection, and `CalibrationCoverage083` recognizes the new model version; `TaskCost089` is new. Original admission, allowance, prior estimators, goal runner, native controllers, proofs and ledger remain unchanged.

All76 actual goals, three fits,48 held-out outcomes,12 restart warmups and original commits/costs were independently inspected. Altered clock and pairing metadata were refused. An attractive forged timing proposal reached the original fitter and was refused by its original-source timing check. Exact restoration preserved model, world and successful allowance counts. Final world hashes survived restart. Actual app1600/1100 captures and offline1440/390 checks are retained with exact-coordinate replays.

Next: fresh task families, contention and bank growth. Cost estimates select a procedure; original outcome verification still decides whether actions succeeded.
