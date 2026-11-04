# Edition 064 declared experiment

2026-11-04. Freeze before any new native observations. Preserve the published
061–063 implementations and their original audits. No controller promotion.

## Contact mechanism

New anchors: angles 24 and 29 degrees, initial tangent speeds .18 and .33 m/s,
friction balance 1.05; same 1 kg, .5 m cube, 12 × 3 m ramp and gravity 9.81.
Four variants per anchor: original baseline; zero initial geometric clearance
(shift the original .001 m gap by -.001 m along the ramp normal); half time step
(1/480 s instead of 1/240 s); locked rotation. Run three seconds, retain every
step including zero: 721 or 1441 observations. Separate unchanged edition063
baseline runs check .75 s and 3 s endpoint parity to 1e-6 m. Total 20 runs.

Read persistent block/ramp manifolds after each completed step, not fresh
contactTest callbacks, for cached normal and two lateral impulses, directions,
positions, separation, lifetime and friction. Record actual solver mode and
split-impulse settings. Native pointers are transient handles, not entity IDs.
Retain all original arrays. Cached impulses are solver evidence, not measured
physical forces; zero cache values do not prove zero friction.

Java derives tangent/normal speed, orientation, angular speed, persistent point
count and touch count. It sums cached impulse vectors on the block (normal
points from B to A) and compares them with m*(velocity change - gravity*dt).
Report the vector residual and tangent components per step; no assertion that
the cache perfectly reconstructs actual momentum. First impulse, first geometric
touch, .75 s/3 s endpoint errors, maximum rotation and largest momentum residual
are distinct. Momentum comparison excludes sample zero and requires every step.
No inferred causal label or automatic setting change from a smaller error.

## Prospective simpler uncertainty

Preselect pooled absolute-error calibration, nominal 90%; fixed ±.020 m is the
comparison. Analytical .75-second mean remains unchanged. Fresh Java Random
seeds: 64001 calibration, 64002 reserved, 64003 shifted stress. Regime outermost.
Calibration/reserved: 30 accelerating then 30 slowing cases each. Each draw
consumes angle, speed, ratio uniforms in that order: angle 12+20*u, speed
.05+.35*u, ratio .85+.14*u or 1.01+.19*u. Stress: 15 per regime, angle 32+3*u,
same speed; ratio .70+.15*u or 1.20+.15*u. Stress is outside calibration support,
explicitly labelled, and never calibrates or selects anything.

Use unchanged edition063 baseline native trial for these 150 uncertainty runs,
180 ticks / stride4 / .75s. Pooled radius is sorted calibration rank
ceil((60+1)*.9)=55 (one-based). Freeze calibration hash/radius before reserved
or stress data; save each interval before its native observation. Total study:
170 native executions. Retain interrupted originals; do not rerun for a good
result. Gate: reserved total coverage≥90%, each regime≥85%, mean full width≤.080m.
Stress results are separate and do not turn failure into success. Report both
methods, all misses, widths and descriptive 95% Wilson sampling intervals for
each total and regime proportion. Stratified empirical results and Wilson
summaries are not universal predictive guarantees. No posthoc method switch.

Queries remain advisory, same baseline geometry/mass/gravity/.75s; refuse
extrapolation, the unsampled friction gap, wrong study kind or incomplete jobs.
Hash/source-bind definitions, solver settings, clocks, original native records,
derived traces, frozen intervals and evaluations. Recompute outputs; refuse
tampering, duplicates, changed partitions, changed chronology and stale sources.

## Completion

Native study, cold exact queries, Stop and preserved complete history;
independent mathematical tests for momentum vectors, Wilson edge cases,
quantiles and gates; audit published063 unchanged. Actual integrated app run,
trace charts and time controls, stress comparison, real local-model checked
report, narrow screenshots. Update README064, offline explorer, Obsidian map,
memories/logs. Push main/tag, fetch and audit original bytes. Model runs CPU-only
with four threads; Java heap bounded. Heavy runs sequential. No new dependency.

References: existing pinned Minie 9.0.3 API and
https://stephengold.github.io/Minie/minie/minie-library-tutorials/collision.html
describe persistent manifolds; https://arxiv.org/abs/2107.07511 describes the
finite-sample conformal rank. Read those fields without changing solver settings.
