# Edition 063 · Stopping traces and uncertainty

Declared before implementation or new native observations, 2026-11-04.
Preserve original BulletRamp, editions001–062, active controllers and private
review installations. Extend the current Java owner and integrated Brain panel.
No new library or automatic actuation. Native simulation is not physical calibration.

## Diagnostic intervention study

Use six anchors: angles26,30degrees × initial tangent speeds.10,.25,.40m/s,
friction/tan(angle)=1.05. These include prior062 failure conditions and are
explicit diagnostic revisits, not new generalization evidence. Same1kg cube,
gravity9.81,12×3m ramp, default initial transform and native solver as062.
For each anchor run baseline, rotation locked (angular factor zero), and
80 solver iterations; change only the named factor. Record721 samples at240Hz
through3seconds: position, linear/angular velocity, quaternion, proximity count,
touch/penetration count (signed separation≤0), minimum reported separation.
No manifold points means unknown separation, not zero. Contact queries are
geometric observations, not force/impulse measurements. Count only block/ramp
points; keep the legacy near-contact threshold.03m explicitly labelled.

Compare the baseline trace's .75s endpoint to a separate original BulletRamp
execution for every anchor; tolerance1e-6m. Refuse parity failure. Compare
endpoint errors at.75s and3s, rotation excursion, first reported touch tick,
first100ms window of speed≤.005m/s and angular speed≤.02rad/s, and whether
the final100ms is at rest. A first quiet window does not prove permanent rest.
Record analytical stop time and sampled predictions. Retain all interventions;
do not pick a new predictor or change original physics from these outcomes.

## Uncertainty experiment

The predictor remains the original analytical endpoint at.75s. No fitting or
retuning of its mean. Use40 calibration and40 reserved baseline cases,20 per
regime in each partition, with separate java.util.Random seeds63001 and63002.
Draw angle uniformly[12,32), speed[.05,.40), balance[.85,.99) for accelerating
or[1.01,1.20) for slowing. Generate in that order, regime outermost,20 draws
per regime; retain exact doubles/definitions. Never train on062 outcomes.
Each native record saves every fourth tick through180ticks (46samples).

At nominal90%, calibrate absolute endpoint error using order statistic
k=ceil((n+1)*.9),1-based:37/40 pooled;19/20 per regime. Proposed interval is
analytical±regime quantile; comparison intervals use pooled quantile and fixed
±.020m. Freeze all calibration evidence and radii before reserved observations.
Save each interval before its native execution. No reserved score changes any
radius, mean or acceptance rule. Check total coverage, coverage per regime,
mean/full maximum width and every miss. Proposed interval qualifies only if
reserved coverage≥90%, each regime≥85%, mean full width≤.080m. This is an
empirical utility gate on these cases, not a universal coverage guarantee.
Report all three methods without selecting posthoc. No controller promotion.

Hash/source-bind native records, parameters, original parity, clocks, samples,
derived traces and summaries. Reconstruct quantiles, frozen intervals and
evaluation. Refuse tampered/duplicate/incomplete records, wrong cases, wrong
source, altered summaries and unsupported query inputs. Calibrated queries
support only the declared input ranges/regimes,1kg,9.81m/s²,baseline geometry
and.75s. Refuse the unsampled balance gap[.99,1.01] and other extrapolation.
Historical061/062 records must remain readable with their original audit.

Test native parity, interventions, stopping/rotation extraction, quantile edges,
coverage and width gates, integrity/cold restart, cancellation/history, actual
local-model reporting and integrated visible controls. Expose trace replay,
contact/speed/rotation charts, intervals/misses and original evidence in one app.
Document failures and actual results with dated screenshots, offline guide,
source inventory, Obsidian map, memories and logs; push main/tag and fetch back.

Reference: finite-sample absolute-error calibration follows the rank convention
in https://arxiv.org/abs/2107.07511. Exchangeability assumptions and finite
evaluation size limit interpretation; fixed reproducible simulation cases do not
establish real-world coverage. Existing Minie APIs are inspected against the
installed pinned dependency, with official API documentation as guidance.
