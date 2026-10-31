# Edition 030 — recoverable native worlds and declared materials

Status: implementation in progress, 2026-10-31. Published edition 029 remains
unchanged. The separately supplied AI conversation is design input, not executed
code or evidence of implemented editions.

## Scope and acceptance

1. Save a checksum-bound native checkpoint containing the immutable map binding,
   Java implementation identity, avatar position/orientation, linear/angular
   velocity, controller waypoint, tick, settling count, work integrals, trace and
   already-applied disturbance. Explicit Resume continues the same run. Manual
   input expires on interruption. A completed run is never physically repeated.
2. Commit checkpoints atomically before publishing each corresponding live
   observation. Rebuild native contacts after restoration; do not promise bitwise
   Bullet solver/manifold restoration. Measure uninterrupted versus resumed runs.
   Refuse edited or incompatible checkpoints without silently respawning.
3. Add optional, validated palette-class material declarations. Compile actual
   native floor patches without changing source cells or published topology
   algorithms. Expose friction, restitution and Java-applied viscous drag with SI
   units and an explicit direct-force actuator model. Preserve legacy packages.
4. Measure force effort (N·s), signed/positive/braking actuator work (J), drag
   work, gravitational work and kinetic energy. Retain per-tick force/displacement
   rows so a separate verifier can reconstruct integrals. Unresolved contact and
   damping work remains a diagnostic; this is not a closed energy-balance proof.
5. Keep the current app design. Add real pause/resume controls, material settings,
   live measurements and independently checked report charts. Existing chat and
   retained map procedures use the same Java runner and material definition.

## Validation and publication gates

- Native tests: moving/rotating state restoration, disturbance exactly once,
  continuous tick/work/trace, no stale manual controls, checksum/source/code edits,
  actual force-limited material trials and reconstruction import.
- Actual Electron controls: moving checkpoint, pause, process kill/restart,
  explicit Resume of the same goal/run, all measured report charts, real file
  export/import, skill reuse, rejection/cancellation, existing map compatibility.
- Actual installed local-model request with declared materials, verified native
  outcome and saved procedure. This does not train Ollama weights.
- Appropriate Java/desktop regressions and an isolated checkout. Preserve user
  projects, model services and earlier review applications.
- New linked README-030 with real dated screenshots, original numeric evidence,
  offline HTML and Obsidian map; update project memory/log and verify documentation.
- Push authorized changes to private eyeoverthink/Fraynix-Agent-Workspace main;
  directly fetch and verify published source and frozen documentation.

## Subsequent work

Pushable-prop planning, persistent layer transitions and controller promotion on
separate evaluation tasks follow this phase. None is claimed as implemented here.
