# Edition 069 · A bounded later check

Prospective contract, 2026-11-05. Base main 59478b388f6eb093c0ffa310231c645317ccbbf9. Commit this document before new native outcomes. Preserve the original runtime, older evidence, current active controller and private worlds. No automatic promotion or LLM weight training.

## Controller and sources

Compare four policies: single (067 unscreened Temporal), calibrated (068 calibrated screen), later (single plus one later observation), laterCalibrated (calibrated plus that same observation). Copy the published068 model exactly into a Java resource and verify its recorded hash. No069 label fits or changes it. Its probability describes the original single-stage recovery; do not label it calibrated for two-stage recovery. Retain historical training provenance and report reuse separately from new costs.

Initial plans and original tick60 contact/recovery remain unchanged. Later policies observe ticks100/104/108, in the same world, on every executed task including directly admitted tasks. Require at least one near callback, every sample's oriented clearance in [-.03,.03] m, rotation <=10 degrees, absolute normal speed <=.1 m/s, angular speed <=2 rad/s and nonnegative tangent speed. Use only the saved prefix. At tick108 (.45 s), inverse-plan the remaining .30 s to the target center using the original acceleration/travel equations. If the predicted endpoint already fits, do not act. Otherwise bound the second delta to +/-.12 m/s, cumulative absolute first and second corrections to .24 m/s, and resulting tangent speed to [0,.8] m/s. Act only when contact is eligible and the bounded predicted endpoint fits. Save the plan before acting. Keep the .75 s deadline.

Retain a diagnostic prediction error against the actual first corrected state (or initial state if uncorrected). Flag displacement error >.01 m or speed error >.04 m/s. This flag is diagnostic, never future-event knowledge or a separate action gate. Persist measured samples, source hashes, both plans/receipts, external receipts and monotonic operation timings.

Every executed later attempt pays three extra observations (.09) and one replan (.01), plus .02 if its second impulse is applied. Earlier costs, refused tasks and all missed attempts remain counted. Screened refusals have no native world or later sensing. Rewards remain success +1, miss -2, refusal -.15, less actual logical operation fees. Fees are explicit experimental assumptions, not money or physical-risk estimates.

## Reserved cases and disturbance schedule

Java Random seeds: clean69001, disturbed69002, unsupported69003. Clean has two regimes of24 cases (48); disturbed two of30 (60); unsupported two of6 (12). Draw angle=12+20u, friction ratio=.85+.14u for accelerating /1.01+.19u for slowing, center=.02+.34u, in that order. Half-width cycles .012/.024/.045 m. Unsupported cycles angle10, angle34, ratio1. All new tuples must be disjoint from068 calibration/evaluation.

Disturbed case i uses i%5: brake at88 tangent -.08/-.14 N s; lift at76 normal +.24/+.36 N s; push at88 tangent +.08/+.14 N s; spin at88 world-z +.02/+.04 N m s; after-second push at132 tangent +.08/+.14 N s. Choose amplitude by floor(i/5)%2. Each family has12 cases, both regimes and amplitudes. External events are actual native impulses with before/after receipts, unavailable to the planner. All60 disturbed cases, including after-second events, count toward the primary gates.

Four original-runtime parity cases unchanged from068, all46 position/velocity/rotation/angular samples within1e-6. Additionally compare paired single/later and calibrated/laterCalibrated prefixes through tick108 (before second impulse), first plans and execution decisions. Require identical prefixes and first behavior. 484 progress slots: four parity slots plus120 tasks times four policies; maximum440 native executions. Unsupported tasks must refuse without execution. Cancellation, cold restore and old068 compatibility must remain real.

## Gates fixed before outcomes

Primary later-versus-single clean gate: success count not lower and mean utility difference >=-.12 (the maximum added sensing/replanning/action fee). Disturbed gate: mean utility gain >=+.10, all-task success-rate gain >=+.10 and later all-task miss rate <=.45. Combined qualification requires both, verified source/parity and complete counts. Report the screened pair separately with the same thresholds as a secondary result; it cannot replace the primary result. Report every family and regime, operation totals, actual elapsed timings, saved/missed goals and harms. No task, physical rule, fee, gate or model tuning after outcomes.

## Delivery

Integrate the actual study, saved trace inspection and bounded Java chat facts into the existing app. Validate Java unit/adversarial/native tests, relevant historical regressions, desktop checks, actual app interaction/restart/Stop and actual CPU4 local model read/report. Preserve failed runs. Capture dated real screenshots; publish a new linked README069, offline guide inspected at wide/narrow sizes, source inventory, original evidence, Obsidian map, memories and logs. Run documentation verification, push main/edition069/readme069, fetch independently and audit actual remote bytes. Never claim a push before it happens.
