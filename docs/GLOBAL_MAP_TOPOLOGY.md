# Explicit global topology in a physical world

Edition029 · [Acceptance plan](EDITION_029_PLAN.md) · [Frozen first-evaluation profile](TOPOLOGY_PROFILE_029.json) · [Previous map/runtime contract](REFERENCE_MAP_WORLDS.md) · [README029](../README-029.md).

The current app can require a connected floor graph, coverage across all grid cells and bounds on the original spawn-to-goal shortest route. These requirements use a separate `connected-grid-v1` profile. A map with no topology contract keeps the original028 generator and package semantics.

## Inputs and finite computation

`constraints` accepts `connected` (boolean), `minFloorFraction`, `maxFloorFraction`, `minRouteSteps` and `maxRouteSteps`. Coverage requires0.05≤minimum≤maximum≤0.95; route bounds require integers0≤minimum≤maximum≤128. Defaults are connected=true, coverage0.30–0.75 and route0–64. Output remains8–24cells/axis. Boundaries count toward the coverage denominator.

The JDK-only selector makes at most32 original adjacency candidates, each with two restarts of4,000 local repair iterations. Candidate1 uses the requested seed; later seeds use `floorMod(seed +104729 × index,1_000_000_001)`. Maximum repairs are256,000. Cooperative interruption remains active. Requested seed and actual candidate seed are distinct recorded values.

The selector never edits candidate cells. It preserves original image/rules, pins and rejected candidates' measurements. It does not carve corridors, remove isolated floor, recolour source classes or relax thresholds. Geometric impossibility and incompatible fixed pin pairs can terminate early while retaining the original failed result. Exhaustion is a real rejected outcome.

## Independent graph audit

The verifier enumerates directed source adjacency and fixed boundary/pin requirements. A separate flood traversal measures the complete four-neighbour floor graph, original-spawn reachability and shortest distance to the original goal. It checks every stored route cell, step and endpoint. It also records graph edges, cycle rank and degree-one dead ends.

Success requires all declared conditions together. When connectivity is requested, exactly one floor component and equality of reachable/total floor are required. A route in a different component or a fabricated cached component count cannot pass. Stored global claims are recomputed against original PNG bytes and requested constraints before native use. Import re-runs deterministic selection and physical compilation and compares the complete result; an outer JSON hash is insufficient.

A kernel identity pins the implementation. Saved global skills retain it. Future implementation changes require explicit versioning or migration rather than silent reinterpretation. Partial reuse options deep-merge original global defaults; chat cannot relax an unrequested retained bound. Existing compiled worlds accept physical execution/retention options while keeping their original geometry and global requirements.

## Physical outcomes and actual views

The028 native force-control, contact, fixed-step and trace gates remain unchanged. Global acceptance does not prove physical traversal. Every successful goal must pass both, then pass the actual six-chart browser audit. A real button exposes the complete topology record; its visible DOM data must match the original record exactly before retention. The report keeps original map/source JSON, every candidate's measurements/reasons, native run JSON and trace CSV.

Completed native evidence and stable report identities survive the existing explicit restart/recovery path. Legacy incomplete native motion without a checkpoint becomes a separate attempt. Edition030 adds compatible same-run native map recovery, declared materials and independent work measurements; see [the current contract](NATIVE_MAP_RECOVERY.md). The frozen029 comparison is unchanged. Cancellation stops owned motion and prevents late successful retention.

## Frozen reserved comparison

Four new labelled PNGs, original cell rows and three seeds per reference were committed before first evaluation. Development used028's two examples. Six kernel/admission source hashes, per-case criteria and compute bounds were frozen before running the reserved suite. First results remain1/12 baseline global acceptances and5/12 new acceptances, with actual native passes for all five accepted worlds. Seven requests remained rejected; every denominator and candidate record is retained.

The old and new generator budgets differ and are disclosed. These small synthetic acceptance cases establish source-bound constraint enforcement and measured outcomes; they do not establish equal-cost superiority, photographic reconstruction, universal completion, trained weights or scientific generalisation. There is no automatic policy promotion. Now that the reference set is published and in the UI library, future independent comparisons require new cases.

## Next gates

Native checkpoints are implemented in030. Next: actual pushable-object planning; a separately versioned topology-aware proposal method for unresolved cases; calibrated perception/material correspondence; independent navigation-policy evaluation with promotion gates and rollback. Mathematical source dimensions remain distinct from three physical spatial axes.
