# Edition 066: one observation, one replan, measured cost

Declared before new outcomes. Preserve edition 065 and all previous evidence. No new dependency, controller promotion or LLM-weight update.

## Question and fixed cases

Can a bounded correction recover initially refused target tasks, after observation, replanning and action costs? Does using measured state outperform the same correction computed from predicted state?

Use the original cube/ramp/1 kg/9.81 m/s²/native contact/free-rotation setup. Target displacement is at 0.75 seconds, not rest. Fixed native step 1/240 second, 180 ticks, samples every four ticks. No teleportation, reset or second world inside a recovery attempt.

60 new supported tasks, Java Random seed 66001, regime outer loop with 30 tasks each. Draw angle=12+20u, ratio=.85+.14u for accelerating or 1.01+.19u for slowing, center=.02+.34u. Half-width cycles .012/.024/.045 m by within-regime index. Same family as 065, fresh draws independent of outcomes. Do not filter unreachable goals. 12 separate unsupported tasks use seed 66002, six/regime, same draws, overriding index modulo three with angle10°, angle34° or ratio1.00. Unsupported cases refuse without execution and do not qualify the candidate.

## Five fixed policies

1. Simple, Fixed and Pooled remain exactly the edition 065 rules, candidate initial pushes [.08,.16,.24,.32,.38] m/s and published 064 margin. Same shared domain fence.
2. Blind and Observed both use the Pooled initial decision when admitted. No extra observation or correction in those cases.
3. On a supported Pooled refusal, Blind and Observed start with Simple's chosen initial push. This is a real attempted action, not a cost-free diagnostic. Its eventual miss cannot be relabelled a refusal.
4. At tick60 (0.25s), Blind replans from analytical displacement and nonnegative analytical tangent speed only. It receives no actual sample. Observed replans from that same world's measured displacement, tangent velocity, normal velocity, rotation, angular speed and contact count. Recorder telemetry for all methods is distinct from observations exposed to their planners.
5. Observed may correct only with at least one near contact (separation≤.03m), rotation≤10°, absolute normal speed≤.10m/s, angular speed≤2rad/s and tangent speed≥0. An unmet condition causes continued motion without another action; the actual endpoint is still scored.
6. One correction computes the nonnegative tangent speed needed to reach the target center during the remaining .5s under the original analytical acceleration a=9.81 sin(angle)(1−ratio). For remaining distance d≥0 and a≥0, v=max(0,(d−.5ah²)/h). For a<0, if d≤.5|a|h² use v=sqrt(2|a|d); otherwise v=d/h+.5|a|h. A target already behind the state yields no correction.
7. Clamp requested velocity change to ±.16m/s. Apply only if the resulting tangent speed is in [0,.8]m/s and the predicted endpoint falls inside the goal window. Otherwise continue without another action. A magnitude≤1e-6 produces no extra impulse. Same planning formula and bounds for Blind and Observed. Blind assumes the nominal contact/orientation state; these are predictions, never labelled measurements.
8. The correction is an actual central impulse on the existing native body: mass×delta-velocity along the ramp tangent. Preserve normal/angular state. No velocity setter, pose rewrite, additional retry or hidden model search. Save observed/predicted state and plan before applying it. Record immediate before/after state and actual impulse.

The old interval is used for initial Pooled admission only. It is not a calibrated interval for the later controlled trajectory. No selected-action or recovery-coverage guarantee.

## Cost and comparison

All requested tasks count. Success+1, miss−2, refusal−.15. Each initial or nonzero corrective action costs .02. Each replan costs .01. Exposing a measured observation to the recovery planner costs .03; Blind pays no observation fee. Thus a two-action observed attempt costs .08 in addition to success/miss reward. Unused observation/replan/action counts are zero. These are frozen assumed utility weights, not physical energy or dollars.

Measure and retain monotonic wall milliseconds for complete native execution, the tick60 sample read, planner computation and corrective impulse call. Report total action count, actual absolute impulse, observation/replan counts, aggregate logical cost, native ticks and simulated horizon. All methods share the .75s deadline, so there is no invented extra simulated waiting-time charge. Native trials run faster/slower than real time; wall timings are machine-specific diagnostics and do not decide qualification.

Save all five initial plans before any method outcome. Execute methods in fixed order Simple, Fixed, Pooled, Blind, Observed; disclose fixed order. Separate fresh worlds for every admitted method, including identical initial actions. Refusal contains no native record. No endpoint or another method's outcome enters a recovery plan.

Report successes, misses, refusals, execution and all-task success, conditional success, target error, costs, action/observation/replan counts, actual impulse and wall time. For initially Pooled-refused tasks, report attempts, recovered successes and failed attempts. Paired Observed-minus-Simple/Fixed/Pooled/Blind wins/ties/losses and mean utility. Preserve accelerating/slowing and all three width groups. Report unsupported cases separately.

Proposed Observed qualifies only if mean utility gain≥.10 over both Fixed and Pooled, ≥.05 over Blind, all-task success≥.50, all-task miss fraction≤.20, and at least 30% of initially Pooled-refused tasks succeed. These are empirical usefulness gates, not significance or generalization claims. No posthoc policy switch and no active controller promotion even if qualified.

## Instrumentation and audit

Before the main tasks, four original-runtime parity checks at (angle,ratio,push)=(14,.90,.08),(30,.98,.38),(14,1.05,.08),(30,1.18,.38). Compare all46 recorded positions, velocities, angular velocities and rotations from the new adapter with original Stopping063 baseline to tolerance1e-6. Eight native executions. Failed parity blocks the study.

Total progress364 slots: four parity pairs plus72tasks×five decisions. Native execution count varies with admission; at most308 including parity. Every corrected record must verify source/settings/clock, task and initial plan, tick60 observation provenance, exact recomputed correction, chronology, same-world identity, unchanged immediate pose/angular state and measured impulse response. Full native records and failed attempts retained. Cold restore and Stop preserve completed history; an interrupted native world is marked interrupted, not silently continued from a new world.

Test formula/bounds, negative costs, refusal versus failed attempt, observation leakage, gate failures, tampering, parity, actual native recovery, cold state and Stop. Test the real integrated UI and local-model report. No success claim from a mock. Document screenshots, exact originals, cost limitations, source-qualified checks, offline explorer, Obsidian and next gates. Preserve earlier frozen editions. Push main/edition066/readme-066 and verify freshly fetched source and evidence.
