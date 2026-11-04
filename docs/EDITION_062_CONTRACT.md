# Edition 062 · Physics first, learned residuals

Declared before implementation and new native observations, 2026-11-04.

Preserve the existing application, FraynixGPT port, original controllers,
verification gates, private projects and immutable README001–061. Extend the
existing Java brain job owner with a bounded residual-learning experiment.
No new runtime dependency, separate agent daemon or controller activation.

Predict endpoint = analytical Coulomb prediction + learned correction. Compare
unchanged analytical physics, a constant training residual, a linear residual
readout and the existing FraynixGPT cortex-feature residual readout. Both fitted
readouts use existing Java ridge fitting with coefficient .001; corrections are
clipped to ±0.05 m. Cortex plasticity sees training input patterns only; readouts
learn actual native endpoint residuals. No language-model weight training.

Fresh deterministic cases, all with 1 kg block, gravity9.81m/s², original default
12×3m ramp, 180 native ticks at240Hz. Define ratio = friction / tan(angle), so
values below/above1 straddle accelerating versus slowing/stopping regimes.

| Partition | Angles ° | Ratios | Initial tangent pushes m/s | Count |
| --- | --- | --- | --- | ---: |
| Train | 8,16,24,32 | .5,.9,1.1,1.3 | 0,.4 | 32 |
| Select | 12,20,28 | .8,1.2 | .1,.3 | 12 |
| Reserved | 10,18,26,30 | .7,1.05 | .05,.25 | 16 |

Domain: angle8–32°, ratio.5–1.3, push0–.4m/s, original geometry/mass/gravity.
No edition061 observations train this candidate. Freeze learned parameters
before selection. Pick minimum selection MAE with ties (within1e-12m) resolved
in order analytical, constant, linear, cortex. Selection qualification requires
a learned winner, ≥10% MAE improvement over analytical and max error≤.020m.
Pin that method, candidate hash and selection evidence before any reserved run.
Reserved qualification additionally requires ≥10% MAE improvement, max≤.020m,
and each ratio regime's MAE≤1.10× analytical regime MAE. Retain failures; never
retune against reserved results. No controller activation even on qualification.

Every native record retains sampled motion, input/source hash and actual clock.
Selection/reserved predictions are durably saved before their native runs.
Audit reconstructs training, readouts, model selection and every evaluation
from original records. Refuse duplicates, changed partitions, changed source,
tampered model/decision/metrics and out-of-domain requests. Candidate predictions
remain explicitly advisory. A hash is integrity, not authentication.

Expose progress, selected method, both evaluation stages, all method errors,
prediction comparisons, original evidence, completed-history selection and Stop
inside Java Brain. Current/last completed result must be available to chat via
existing checked agent facts/report. Cancellation and restart preserve original
records; no silent retraining. Verify edition061 compatibility and new native,
adversarial, UI, cold restart and actual local-model paths. Document measured
success or failure, real screenshots, offline guide, memory/logs, source and
original evidence; push main and verify freshly fetched publication.

Next after this experiment: typed support/load observations and calibrated
uncertainty on new cases, then assess whether prediction improves actual task
decisions. Simulation results do not establish physical accuracy or general RSI.
