# Edition 067: temporal contact evidence and value before the first push

Declared before new native outcomes. Preserve edition 066 and its original source identity. Native ramp simulation only, no physical-camera calibration, automatic controller promotion or LLM training.

## Fixed environment and comparisons

Reuse the exact RecoveryRamp066 adapter: 1 kg cube, unchanged ramp, gravity, friction, solver, free rotation and clearance. 180 ticks at 1/240 s; 46 samples at stride four. Goal is displacement at 0.75 seconds, not resting placement. At most one corrective central impulse at tick 60, bounded to ±0.16 m/s and resulting tangent speed [0, 0.8]. No teleport, reset, extra time or hidden retries.

Seven methods in fixed execution order: Simple, Fixed, Pooled, Blind, Observed (all exact 066 choices and rules), Temporal, Value. Every executed method has its own fresh world. Each individual attempt continues the same world across its correction. Freeze all initial decisions before any method executes.

Temporal takes Pooled's action directly when admitted. On a supported Pooled refusal it starts Simple's push and replaces Observed's instantaneous contact eligibility with the three-sample rule below. Value uses the same Temporal controller, but first runs the declared scenario screen; a declined screen prevents any first push. Direct Pooled admissions and unsupported cases do not run that screen.

## Three-sample contact evidence

Use only samples at ticks 52, 56 and 60 (0.216667, 0.233333 and 0.25 s). At least one must contain a ramp callback point with separation ≤0.03 m. Every sample must satisfy the old motion bounds: rotation from initial ≤10°, absolute normal speed ≤0.1 m/s, angular speed ≤2 rad/s, tangent speed ≥0.

Additionally calculate signed geometric clearance of the oriented cube's lowest point above the ramp top plane. With unit normal n=(sin(angle),cos(angle),0), center c=(0,rampCenterY,0), cube center p and its normalized quaternion basis R, clearance is n·(p−c)−0.1−0.25Σ|n·R_i|. Require clearance within [−0.03,+0.03] m at every sample. This is known native geometry, not camera-inferred shape. Keep original per-sample near-point counts, geometric clearances, motion values, ticks and sample hashes in the evidence.

If all conditions pass, apply the same 066 inverse-planning formula to the actual tick-60 distance and tangent speed. Do not falsify its measured near-point count: temporal eligibility is a separate explicit input. Otherwise withhold the correction, continue the original motion and score the actual outcome. No endpoint or future sample may enter planning.

A missing instantaneous callback does not prove absent physical support. Temporal versus Observed compares the whole history/geometry policy and its extra fees, not the causal effect of history alone.

## Pre-push value screen

Use only the task, initial Simple speed, 066 analytical prediction and the published 064 radius r from Calibration065. No new outcome enters the screen. This is a declared sensitivity model, not calibrated recovery confidence.

Compute the predicted state at 0.25 s. Enumerate nine equally weighted scenarios, with decision-time displacement offset in {−r,0,+r} and terminal displacement residual in {−r,0,+r}. Keep predicted tangent speed fixed and assume eligible contact. For each scenario, use the unchanged inverse planner and impulse bounds; its final hypothetical displacement equals the planner's predicted endpoint plus terminal residual. Count whether it falls inside the goal window. Save all scenario inputs, corrections, endpoints and outcomes before the first push.

Estimated success fraction p is the fraction of the nine scenarios that succeed, not a learned probability. Conservatively estimate two actions, three observations and one replan plus the screen: estimated utility = 3p−2−0.145. Declining scores −0.155 including the screen. Value attempts only when estimated utility ≥ −0.155+0.05. Screen does not choose a different initial action or use candidate-native simulations. Record screen wall time separately from instrumented native trial timings.

## Fresh tasks

Use java.util.Random seed 67001, regime outer loop then 30 tasks each. Draw angle=12+20u, ratio=0.85+0.14u for accelerating or 1.01+0.19u for slowing, center=0.02+0.34u. Half-width cycles 0.012, 0.024, 0.045 m. All 60 tasks remain in denominators, including potentially unreachable goals.

Twelve unsupported tasks use seed 67002, six per regime and the same draw order, then cycle angle=10°, angle=34°, ratio=1.00. Every method must refuse without a screen or native motion. Report separately, excluded from qualification.

## Costs and gates

Unchanged success +1, miss −2, refusal −0.15, each actual impulse −0.02, each planner-accessible measured sample −0.03, each replan −0.01. Blind receives no measured state. Observed gets one sample. Temporal and attempted Value get three. Value's supported Pooled-refusal screen costs 0.005 whether attempted or declined. Record action, observation, replan and screen counts, total impulse, full instrumented native time and screen time. Verification telemetry is separate from planner access. Timings are diagnostic, not qualification.

A failed first push remains a miss even if no correction follows. A Value refusal after screening has no native record or endpoint. Native trial time includes recording/checkpoint cost; fixed order and host load limit timing comparisons.

Proposed Value qualifies only with mean utility at least +0.05 above each of Fixed, Pooled, Blind and Temporal, all-task success ≥0.40, all-task miss rate ≤0.15 and recovery of initially Pooled-refused goals ≥0.20. No posthoc method switch or automatic promotion even if qualified. Also report Temporal versus Observed utility/outcomes and Value versus Temporal (avoided misses, sacrificed successes, accepted-case agreement and fees). Report all seven methods, paired Value comparisons, accelerating/slowing and all three widths. Gates are empirical, not significance claims.

## Provenance and acceptance

Before tasks, compare four uncorrected trials with original Stopping063 at (angle,ratio,push)=(14,.90,.08),(30,.98,.38),(14,1.05,.08),(30,1.18,.38). Every position, velocity, angular velocity and rotation sample must agree within 1e-6. Four parity pairs, eight executions. Total progress 508 slots = four pairs +72×seven methods; maximum 428 native executions.

Save every initial plan before native execution and each correction before its impulse. Verify source, task, scenario arithmetic, sample provenance, no future leakage, unchanged immediate pose/angular state, impulse response, same-world identity, chronology, costs and exact aggregates. Preserve failures and originals. Test temporal dropout versus persistent loss, rotated geometry, value refusal, changed evidence, old 066 compatibility, cold restore and Stop. Test integrated app and an actual local-model checked report. Publish README 067, actual screenshots, offline explorer, full inventory, Obsidian, original evidence, memories/logs, main/edition067/readme-067; fetch and audit the published bytes.
