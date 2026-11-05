# Edition 073: executable experimental memory

Prospective implementation and acceptance contract, November 5, 2026. This edition implements the user's three requested steps in the existing Java goal runner. It does not add a reverse-engineering engine or change the native controller.

## Scope and restore contract

Retain a completed, independently checked persistent-world arrangement as an immutable episode. Require the original native trace, source/version, exact report receipt and committed world outcome. An episode may suggest a plan; it never proves a new action happened.

The first adapter reconstructs a declared initial, at-rest box world with elapsedTicks=0, no held object, original controller initialization, fixed native time step and no random dynamics. Reject later moving/contact checkpoints rather than promise their hidden solver state is restored. Re-run the retained source/plan in a separate native world and compare all matched body samples before admitting variations. Required sample coverage >=0.95, maximum position difference <=0.00001 metres and angular difference <=0.001 degrees. Native success and protected-body gates must also pass.

## One-component forks and execution

Support one explicit change to friction (0..2) or dynamic-body massKg (>0..20), identified by sourceId. Preserve geometry, units, gravity, all other properties and original target poses. The changed source is a new persistent variant world; never silently reset or edit the user's original world. Record parent episode, exact source and changed field/value.

Use the existing counterfactual runner's three declared alternatives and ranking without changing its controller or gates. Keep rejected and failed alternatives. At most one reconstruction, three alternative trials and one fresh execution per uninterrupted goal; each existing alternative retains its 90-second bound and the shared goal has a ten-minute bound. Explicit interruption recovery retains every attempt and has at most two reconstruction retries. Stop cancels owned native work. Completed trials are not silently repeated.

If execution is requested, execute the selected plan freshly in that variant world. Require the existing native gates, prediction/actual comparison, original canvas checks and world revision commit. Only then save a verified child episode. Review-only requests retain their branches without committing movement. The variant's current revision must still match at execution; stale state refuses action. A failure remains a diagnostic record, never a successful memory or skill.

## Retrieval and integration

Use deterministic metadata retrieval: current compatible native identity, supported restore contract, requested sourceId/property, and normalized distance from the requested component value; stable ID breaks ties. An explicitly selected episode must pass the same checks. Record the ranked candidates and retrieval cost. HDC or language-model similarity is not required for this first baseline.

Expose the actual episode catalog and registered request schema to chat. A well-specified chat request runs through the same Java goal runner and existing host report checks. Use the current app and a recorded-state replay, showing source/variant properties, parent/child identity, reconstruction result, alternative costs and actual/predicted motion. No new detached app window or generated mockup.

## Acceptance and reporting

- Real source episode capture, exact immutable byte retention and reload after cold restart.
- Actual reconstruction, component-isolation checks and incompatible/edited source refusal.
- Friction and mass variants execute native alternatives; retain all failed/refused cases. A fresh execution must have a different run ID and its own receipt before the variant world advances.
- Chat/UI end-to-end completion, visual replay and actual report checks. Stop, interrupted recovery, stale revision and duplicate finalization must not become false success or duplicate commits.
- New checks use fresh labelled variants: friction 0.2 and 1.2; dynamic mass 0.7 and 1.5 kg; a deliberately excessive 20 kg payload, plus malformed/static-mass changes. These are simulator acceptance cases, not physical-world generalization or model-weight training.
- Record reconstruction/alternative/execution ticks and wall time. Reusing an episode is not automatically a speed improvement. Any matched-budget comparison retains retrieval/reconstruction costs and null results.

Preserve editions 001–072 and existing live installations/private worlds. Publish a new README, real screenshots, offline guide, continuity notes and fetched GitHub verification only after checks complete.
