# Edition 068: calibrate recovery value, then test unfamiliar disturbances

Declared before any new native outcome. Preserve published 001–067, old runtime identities, personal worlds and the normal045/qa046 installations. No new dependency, automatic controller promotion, physical-camera accuracy claim or LLM-weight training. All work remains in the Java-owned app.

## Fixed physics and interventions

Use a new isolated adapter preserving RecoveryRamp066's exact scene, initial impulse, settings, solver, 1 kg cube and free rotation. Keep 180 ticks at 1/240 s, 46 recorded samples, a 0.75-second displacement target and at most one controller correction at tick60, bounded to ±0.16 m/s and tangent speed [0,0.8]. Reuse TemporalContact067's unchanged three-sample geometry/motion rule and inverse planner. No teleport, extra deadline or hidden recovery.

Before calibration, compare four no-disturbance/no-correction adapter runs with original Stopping063: (angle,ratio,push)=(14,.90,.08),(30,.98,.38),(14,1.05,.08),(30,1.18,.38). Every recorded position, velocity, angular velocity and rotation must match within 1e-6. Four pairs/eight executions.

Calibration and clean evaluation have no external disturbance. Disturbed evaluation cycles four families: brake-before (−0.10 N·s along the downhill tangent at tick40), lift-before (+0.30 N·s along the ramp normal at tick40), push-after (+0.10 N·s downhill at tick84), spin-after (+0.025 N·m·s about world Z at tick84). Events occur after that step and before its sample; tick60 control remains unchanged. Record each actual external impulse's before/after pose, linear/angular velocities, identity and tick. Check the expected mass/inertia response. External events are experiment interventions, counted separately and not charged as agent actions. Every executed method gets the same event for its task in a fresh world.

The controller receives only the unchanged task, its own action and permitted observations. Neither the screen nor calibrator receives the disturbance identity, timing or future state. Later disturbances are deliberately beyond the single replanning opportunity; failure is a valid outcome.

## Prospective data split

Use java.util.Random with seeds 68001 (48 calibration), 68002 (48 clean evaluation), 68003 (48 disturbed evaluation), 68004 (12 unsupported). Regime outer loop, 24 tasks per regime (6 for unsupported); draw angle=12+20u, ratio=0.85+0.14u for accelerating or 1.01+0.19u for slowing, center=0.02+0.34u. Half-width cycles .012,.024,.045 m. Disturbance family cycles by within-regime index modulo four. Unsupported cases cycle angle10°, angle34°, ratio1.00 after drawing the same parameters. All requested goals stay in the denominator, including unreachable ones.

Run Temporal on every calibration task. Train only on supported, initially Pooled-refused tasks, including those the old Value screen would decline. Each label is the actual Temporal success, paired with that task's pre-push nine-scenario fraction. Direct Pooled admissions are retained but excluded from calibration because no screen is used there. No selected-attempt label bias, reserved label or disturbed outcome may enter fitting.

Freeze the model, source record hashes, all calibration labels, bin counts and fit time before any evaluation result. No refitting, threshold change or posthoc choice after evaluation begins. Persist this boundary through restart. Report the calibration's native execution count, misses, actions, observations, replans, operation fees and instrumented wall time separately from per-task evaluation costs. Upfront operation fees also include .005 for each scenario score generated as a training feature; those computations are counted separately from the unscreened Temporal policy's action outcome.

## Handwritten Java calibration

Group labels by the integer number of successful analytical scenarios, 0–9. For each occupied bin, use one success and one failure pseudocount: target=(successes+1)/(count+2), weight=count+2. Apply weighted pool-adjacent-violators to obtain nondecreasing fitted rates. Store each original bin and merged block; interpolate linearly between occupied-bin scores and clamp beyond their endpoints. Empty training data refuses fitting. This is a finite-data empirical probability estimate, not a confidence guarantee.

The monotonic objective and interpolation follow the mathematical description in [the primary scikit-learn documentation](https://scikit-learn.org/stable/modules/isotonic.html); implementation is original Java and does not import that library. Smoothing and admission thresholds are this experiment's declared choices.

## Four compared policies

Fixed order: Simple, Temporal, Value (exact earlier choices), Calibrated. All initial decisions are saved before any method executes. Value uses the original nine-scenario screen. Calibrated uses the identical Temporal controller and initial action, replacing only the screen's success fraction with the frozen fitted rate. Pooled-admitted tasks bypass both screens; unsupported tasks refuse without screening or native execution.

Keep prior reward/cost assumptions: success +1, miss −2, refusal −.15, actual agent impulse −.02, each planner-accessible measured sample −.03, replan −.01, scenario screen −.005. Calibrated additionally pays .002 for the learned lookup on screened tasks, including declined tasks. Its conservative attempt estimate is 3p−2−.147; decline is −.157; attempt only with at least .05 estimated gain. A failed first push stays a miss when correction is withheld. Keep native trial, scenario calculation and model-fit wall timings separate; they are machine-specific diagnostics.

No calibration cost is silently charged to another policy or hidden: report its upfront operation fees and misses separately. If mean evaluation utility gain over Value is positive, also report the assumed number of future tasks needed to repay those upfront operation fees; otherwise report no finite break-even. This calculation excludes fit compute, training miss penalties and physical risk and is not a deployment claim.

## Held-out calibration and task value

Use the independent unscreened Temporal outcome as the label for every initially Pooled-refused evaluation task, including refused Value/Calibrated tasks. Report raw versus calibrated Brier score, five equal-width reliability bins and ECE, plus all individual predictions/labels. This measures the whole score population, not only accepted attempts. Calibration on clean tasks does not establish calibration under interventions.

Report all four methods' successes, misses, refusals, recovery counts, operation fees, actual actions/observations/replans/screens/lookups, timings and paired Calibrated-minus-comparison utility. Show avoided misses, sacrificed successes and accepted-attempt agreement with Temporal. Include the two regimes, three goal widths and four disturbance families.

The proposed Calibrated gate requires, on clean evaluation: Brier at least .01 below raw, mean utility no worse than Value by more than .01, success rate ≥.40 and miss rate ≤.15. On disturbed evaluation: Brier no worse than raw, mean utility no worse than Value by more than .01, success rate ≥.25 and miss rate ≤.25. Both must pass; unsupported cases are separate. These are empirical gates on finite samples, not statistical significance. No automatic promotion even if passed. Never change the gate to obtain a passing result.

## Persistence, verification and publication

484 progress slots: four parity pairs +48 calibration executions +108 evaluation tasks×four policies. Maximum 440 native executions (eight parity +48 calibration +384 supported evaluation). Each method's attempt stays in one world; different methods and tasks use separate identities.

Audit exact case sets, calibration-only fit, frozen model before evaluation, score arithmetic, disturbance blindness, external and corrective impulse receipts, unchanged immediate pose, source identities, sample chronology, costs and aggregates. Test monotonic pooling, empty/tampered calibration, future-label exclusion, calibrated refusal, unsupported no-motion refusal, actual native disturbance response, old067 compatibility, cold restore and Stop. Keep original failed invocations. Test the integrated panel and real CPU-only/four-thread local-model checked report. Publish README068, actual screenshots, offline explorer, source inventory, Obsidian map, notes/logs and evidence; push main/edition068/readme-068 and freshly fetch/audit the published bytes.
