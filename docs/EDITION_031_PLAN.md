# Edition 031 — native pushing with approach planning and recovery

Status: implementation starting, 2026-10-31. User approved “nice work, next step”.
Edition030 is actually published and independently verified; preserve its source
and frozen evidence. Its normal review remains isolated in qa030-release.

## Complete task

“Push the selected crate to this relative horizontal target, choose a valid
approach, detect a physical stall, recover within the declared force budget,
verify the final object, and retain the procedure.”

Use the existing Spatial import/view, local chat, Java goal runner, canvas and
Skills. Add actual controls for the new task; no placeholder page. Existing grab,
move and release remains available and its behavior must remain intact.

## Domain and implementation

- A qualified existing spatial-scene JSON supplies declared metre-scale static
  floor/axis-aligned obstacles and exactly two dynamic boxes: pusher and crate.
  Support integer-metre planar planning with explicit bounds; reject unsupported
  geometry/targets before actuation. Preserve original source bytes and IDs.
- Java bounded search reasons about crate position and pusher reachability.
  Every push requires a reachable standing cell behind the crate and a clear
  swept crate path. Walking through a crate is not proof of pushing solvability.
- A separate native pushing module applies bounded horizontal forces to the
  pusher only. The crate moves through actual Bullet contacts. No grip, direct
  crate force, velocity replacement or post-initialization pose assignment.
- Record actual pusher/crate positions, velocities, orientations, contacts,
  action/phase, forces and progress. Stalls require measured low crate progress
  and actual contact. Recovery backs away/reapproaches and can increase force
  only inside the original declared cap. Use finite recovery/time/search budgets.
- Persist both dynamic bodies and the entire controller/plan/stall/recovery state
  in an atomically committed checksum/source/code/backend-bound checkpoint.
  Explicit same-run resume must preserve the trace prefix and prevent duplicated
  recovery effects; stale inputs never replay. Native contact caches rebuild.
- Independent verification checks actual target error/speed, source identity,
  force bounds, plan/approach semantics, native contact evidence, action counters,
  initialization-only pose assignments and actual recovery state. Report original
  numeric data and check every visible chart point before procedure retention.
- Keep edition030 Map classes unchanged so its checkpoint contract stays intact.
  New classes use existing native/JDK facilities, not a new library or model.

## Acceptance and publication

1. Finite planner solves a scene requiring a changed approach; rejects blocked
   targets, unreachable standing poses and unsupported inputs honestly.
2. Native contact pushing succeeds on declared low/high-friction or changed-mass
   cases. A gentle-force high-friction trial must physically stall and recover;
   an insufficient maximum-force negative control must fail without promotion.
3. Actual process termination while both bodies/controller are active restores
   the same run, both velocities and all committed evidence. Corruption/source/
   implementation mismatches refuse recovery without silently respawning.
4. Actual existing UI, local-model chat and saved-skill reuse complete new tasks,
   display real native movement/evidence and preserve prior app functions.
5. Run appropriate Java/desktop/shared workflow checks and an independent
   checkout. New README031 with real dated screenshots, raw original evidence,
   offline companion, Obsidian map, memory/log updates and actual main/tag push,
   followed by direct remote verification. Preserve personal data and reviews.

This is bounded native manipulation and procedural retention. It does not claim
universal robotic manipulation, measured photographic material inference,
automatic policy promotion or Ollama-weight training. Geometry-aware replanning
under changing obstacles and independent controller promotion remain later gates.
