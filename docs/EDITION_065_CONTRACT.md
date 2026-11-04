# Edition 065 · Prospective decision value

Declared before edition 065 outcomes. Previous release: `3915d75`; immutable
064 experiment release: `50be9a6`. No change to the existing native physics.

## Question and scope

Does using the already calibrated uncertainty improve a concrete decision:
choose an initial push so the block's displacement at 0.75 seconds lies inside
a requested target window? This is bounded, open-loop decision execution in
reset native worlds, not closed-loop recovery or physical-world validation.

Use the published 064 pooled radius, its 60 calibration originals and exact
model/hash as a bundled, provenance-bound Java resource. Recompute its radius
and verify record/source hashes. No new fitting, tuning or posthoc switching.
The original full published study remains available and the resource extraction
must match it exactly. A previous 90% calibration does not guarantee 90%
coverage after policy-based action selection; measure selected-action coverage.

## Frozen tasks and policies

60 new supported tasks, Java Random seed 65001. Regime outer loop, 30 tasks
each. Draw angle `12 + 20*u`, friction balance `.85 + .14*u` for accelerating
or `1.01 + .19*u` for slowing, then target centre `.02 + .34*u`. Target half
width cycles `.012, .024, .045` by within-regime index. Each task fixes 1 kg,
gravity 9.81, original cube/ramp, original clearance/time step/free rotation.
No target is selected using observed outcomes. Windows may be unreachable.

Candidate tangent speeds, ascending: `.08, .16, .24, .32, .38` m/s.

- **Simple:** choose the analytical endpoint nearest the window centre.
- **Fixed:** require the analytical endpoint ±20 mm wholly inside the window;
  choose the nearest admitted candidate, or refuse.
- **Pooled (proposed):** identical rule using the frozen 064 radius.

Ties retain the first ascending candidate. Both interval policies may choose
the same action as Simple; their extra capability is selective refusal.
All policies share the same calibrated-domain admission fence. Each admitted
decision executes its own new native world, 180 ticks at 1/240 s, retaining
every fourth sample. Record an actual independent trajectory even when another
policy chose the same speed. Refusals have no fabricated trajectory or endpoint.
Persist all three plans before any outcome for that task. Neither policy reads
the other's result. Fixed method order Simple, Fixed, Pooled; expose this limit.

12 separate domain-refusal tasks, seed 65002, same draws with six per regime.
Within each regime: index modulo three sets angle to 10°, angle to 34°, or
balance to 1.00. All three policies must refuse; zero native executions.
These cases do not enter the decision-value qualification.

216 decision slots total, at most 180 native runs. Progress distinguishes
decisions and actual executions. Success requires observed displacement inside
the closed target window at the stated horizon, not complete rest.

## Costs, measurements and gate

Utility: successful action +1, missed action -2, refusal -0.15; subtract 0.02
per executed action. Report successes, misses, refusals, execution fraction,
all-task success fraction, conditional success, target error, selected-interval
coverage and all-task mean utility. A refusal is neither success nor a miss.
Report paired utility differences by task, wins/ties/losses, both regimes and
all three widths. Count refusals where Simple actually succeeded as missed
opportunities relative to that comparator, not an omniscient oracle.

Proposed Pooled must improve mean utility by at least 0.10 over Simple AND
0.05 over Fixed; its all-task miss fraction must not exceed Simple's, and its
all-task success fraction must be at least 0.50. These are declared empirical
gates, not statistical significance claims. Never promote another policy after
seeing results. No active-controller or LLM-weight changes, even if qualified.
Extra sensing and recovery are absent here: do not claim their costs measured.

## Verification and delivery

Audit the task partitions, complete deterministic plans, source/model binding,
unique native identities, clocks, definition matching, prediction-before-
observation chronology, derived metrics and refusal/no-trajectory invariant.
Reject tampering and stale sources. Cold restart preserves exact query results;
Stop preserves completed history. Reuse compact progress and bounded lossless
storage; app/test heap at 768 MB, CPU four threads, heavy validators serial.

Actual integrated UI: run/Stop/history, paired policy table, all-task score plot,
case selector, goal-window/endpoint plot and trajectory replay, explicit refusal
and shifted-domain views, query controls, raw evidence, local-model checked
report. No stubs or new dependencies. New README065, actual screenshots, offline
explorer, Obsidian map, source inventory, original logs and continuity notes.
Push main/edition065/readme-065, fetch and audit the published original bytes.
