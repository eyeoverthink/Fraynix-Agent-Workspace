# Edition 061 acceptance contract

Declared before implementation or new native outcomes, 2026-11-04.

Port selected owned FraynixGPT algorithms from eyeoverthink/fraynix-gpt at
f81d58a07790256c07b30c0d12958748b0ec329d into shared Java brain-core. Keep the
existing application, coordinator, physics, dispatch and verification ownership.
This is a numerical cortex/outcome learning integration, not an import of a
language-model checkpoint or a claim that the language model learned physics.

1. Repair Fly sparsity with deterministic positive top-k, including sizes 1–10,
   tied activity and zero input. Reject malformed/nonfinite input.
2. Save the full bounded hive continuation: columns, weights, activation,
   predictions, lineage, phases, lifecycle counters, fossils, clocks and retained
   snapshots. A split execution must match uninterrupted execution byte for byte.
3. Admit exactly the supported checkpoint version, bounded dimensions/counts and
   finite values. Reject unknown versions even with a recomputed checksum. Do not
   reinterpret incomplete legacy FRAYSEAL v19 as a full continuation. Failed
   restore must not mutate the running brain. SHA-256 detects byte changes; it is
   not authentication.

Connect those algorithms to an actual Java brain job collecting native ramp
outcomes. Each record retains inputs, prediction made before observation, all
sampled motion, final state, source implementation identity and a digest. Verify
the record before learning; preserve failures and evaluation observations.
Prediction is advisory and never bypasses existing action or outcome checks.

Frozen experiment: training angles 10/20/30/40 degrees crossed with friction
0/.15/.30 and tangent pushes 0/1 m/s (24 cases); reserved angles 15/25/35 crossed
with friction .05/.25 and pushes .25/.75 (12 cases). Gravity 9.81, mass 1 kg,
default 12-by-3 m ramp; 180 integration ticks at 240 Hz. No held-out tuning.
Fit once on training data. Compare absolute endpoint error against a training
mean, analytical Coulomb model and a simple linear readout without cortex.
Candidate qualifies only if mean error improves by at least 5% over ALL three
baselines and worst error is at most 0.10 m. Otherwise retain it as unqualified.
No controller activation in this edition. Same-solver validation is simulation
evidence, not physical calibration. No real-world or general intelligence claim.

Expose real progress, evidence, retained state and bounded prediction in the
existing Java Brain view; preserve cancellation and restart, and make current
capability/outcome status available to chat. Test native outcomes, refusals,
tampering, duplicate learning, interrupted jobs and cold reload. Update linked
README061, actual screenshots, offline guide, memory/logs and publication receipt.
Preserve frozen001–060 and the user's installed apps/private projects. Push to
private Fraynix-Agent-Workspace/main and verify the fetched content.
