# Edition 071 · Account for uncertainty before skipping a look

Prospective contract, 2026-11-05. Base main 196fdce78ae5cb1937477266acc25b3385a4cb27. Commit this before new native outcomes. Preserve editions 001–070, all earlier controllers and evidence, personal worlds and live normal045/qa046 installations. No automatic promotion. No subagents or concurrent heavy validators; Java 21, four CPU threads, 768 MB test heap.

## Hypothesis and owned implementation

Edition 070 lost two goals after skipping from an unmeasured predicted state. This motivates a NEW hypothesis, not tuning the old cases: learn the error of the no-extra-observation trajectory, calibrate a conservative range separately, and override an old skip only when the range can leave the goal band. Keep the original initial push, first correction, native contact guard, deadline and remaining impulse budget unchanged.

Four paired policies: blind (no later check), fixed (exact 070 tick108 behavior), adaptive (exact 070 scheduler), uncertain (070 scheduler with a new uncertainty guard). All use the same unscreened initial policy; the published 068 screened policy remains intact but is not a comparison arm here. At tick60 the new guard gets only the paid first recovery state plus commanded delta, or the original analytical prediction for directly admitted tasks. No free current sample, future actual event, held-out outcome or peer-policy result enters planning.

## Fit, calibration and freeze

Separate deterministic Java Random streams: fit71001, calibration71002, clean71003, disturbed71004, unsupported71005. Draw angle12+20u, accelerating ratio.85+.14u or slowing1.01+.19u, center.02+.34u; half-widths cycle.012/.024/.045m. Fit/calibration draws are accepted by pre-action classification into four cells: accelerating/slowing crossed with predicted/measured-first anchor. Six fit and nine calibration examples per cell:24 fit and36 calibration native blind runs. Selection depends only on task/initial decision, never its physical outcome. Abort if any cell cannot be filled within10000 draws. All tuples must be disjoint across partitions and earlier068–070 partitions.

For each training run, use the no-later physical distances at92,116,140,180 minus the owned signed-Coulomb forecast from the tick60 anchor. Per cell and time, fit the median residual and median absolute deviation, floored at.002m. Only fit rows set those centers/scales. Calibration score is the maximum absolute standardized residual across the four times. Quantile rank ceil((n+1)*.90), capped at n, is nine for each nine-example calibration cell. Use that frozen multiplier for all four time intervals. Store every fitting/calibration case, score, source hash, counts, coefficients and model hash; save the model before any evaluation plan.

This is a nominal90% joint residual envelope with an explicit finite-sample rank rule. Exchangeability is not established for these stratified simulator fixtures or for disturbances. Report actual reserved coverage and widths; never label the interval a guaranteed probability, physical accuracy or confidence about arbitrary worlds. No held-out fitting, threshold adjustment or LLM-weight training.

## Scheduling and costs

For each of the original nine hypothetical070 scenarios, apply the learned terminal residual center plus/minus the calibrated radius to its no-check endpoint. Keep the original hypothetical scenario assumptions distinct from measured calibration. The union envelope is a risk bound, not a fitted scenario probability. If the old scheduler already chose92/116/140, keep that choice. If it skipped and the entire union is inside the target band (inclusive tolerance1e-12), retain the skip. Otherwise take the earliest permitted window ending92. Save the old decision, every scenario interval, union bound, source cell/model hash, reason and final choice before consuming that window.

All later corrections use unchanged070 inverse/contact logic: at most one extra window/impulse, .12individual and.24cumulative absolute m/s limits, tangent speed[0,.8], episode180ticks at240Hz. Native event timing and samples remain unchanged. Diagnostic46-sample traces are retained for verification; sensing savings refer to controller-selected samples, not fewer diagnostic captures or camera frames.

Existing operation fees remain. Adaptive pays.005 scheduling per execution; uncertain pays that plus.005 for interval inference even when it retains a skip. Training charges original blind actions/observations/replans and all target misses, plus four label observations(.12) and residual computation(.005) per training run, plus.02 once for fitting and.02 once for calibration. Report those costs separately from held-out policy utility and retain complete wall time. No invented native observations for refused tasks.

## Reserved comparison and audits

48 clean tasks (24/regime),60 disturbed (30/regime),12 unsupported(6/regime). Use the same draw order/width cycle and unsupported replacements as070, with new seeds. Disturbance families, balancing, amplitudes and times are EXACT070 definitions:12 each brake/lift/push/spin/after-last. All late events count. Freeze the manifest before training. Never give the event schedule to the scheduler.

Six parity slots: blind/fixed/adaptive on the first predicted-accelerating and measured-slowing fit tasks, each compared against actual070 execution. Compare all46 samples including contacts and both plans; no tolerance for differing JSON samples. Paired evaluation prefixes and first corrections through60 must be identical. Total546 slots (60 training,6 parity,480 evaluation), maximum504 native executions. Refusals have no fabricated motion. Verify exact source/model/chronology/identity/counts, rederive all plans, and reject tampered fit data, calibration scores, bounds, schedule times, events and cost claims.

Coverage is measured against the paired BLIND world, whose no-extra future is the quantity predicted. Never use a corrected endpoint as truth for that forecast. Report joint four-time coverage, terminal coverage, interval widths and nominal-only error by cell/phase. Also report coverage of the original070 hypothetical no-check range for comparison, and whether forced looks save/harm goals versus adaptive. All-policy successes, misses, refusals, selected observations, actions, fees, native/scheduler time and subgroups remain visible.

## Qualification before outcomes

Primary uncertain versus adaptive: clean successes no lower, mean utility gain>=.005, and joint clean coverage>=.85. Disturbed successes no more than one lower and utility difference>=-.03. Across supported cases require at least one newly forced look, one retained skip, and at least one goal saved versus adaptive. Clean uncertain must still use at least10% fewer extra observations than fixed. All source/parity/partition/domain audits must pass. These gates cannot be changed after outcomes; passing tests does not imply this policy passes qualification. No automatic controller promotion even on a pass.

## Delivery

Owned Java implementation, unit/adversarial/native/restart/Stop and old070 compatibility checks, actual integrated app and CPU4 local-model reporting. A readable uncertainty view with calibration examples, frozen bounds, blind forecast coverage, guard decisions, actual trajectories and costs. README071, dated actual screenshots, offline responsive evidence explorer, full source inventory, Obsidian79/canvas, notes and logs. Preserve failures. Push main/edition071/readme-071 and independently fetch/audit exact bytes before claiming publication.
