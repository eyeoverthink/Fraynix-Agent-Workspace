from pathlib import Path
import json
r=Path.cwd();v=json.loads((r/'docs/readmes/118/verification.json').read_text(encoding='utf8'));u=json.loads((r/'docs/readmes/118/actual-ui.json').read_text(encoding='utf8'));assert v['passed'] and u['passed']
ms=v['controllerCases'];rr=next(x for x in ms if x['partition']=='reserved' and x['policy']=='risk118');old=next(x for x in ms if x['partition']=='reserved' and x['policy']=='safe117');sw=next(x for x in ms if x['partition']=='software-reserved' and x['policy']=='risk118')
status=f"""## Edition118 — implemented and locally validated; publication preparation

2026-11-11, America/Los_Angeles. Isolated S:/Codex-Work/fraynix-workspace-edition118, source {u['source']}, parent757f8d9d. CI checked first:117 main43073284552 still running. New declared construction clones original Goomba/Turtle/terrain/block components. PickupObserver118/PickupSafety118 read terrain support, clearance, obstruction and actual enemy motion. Small-player approach timing avoids the early-jump prototype failure. Ordinary inputs only; original physics untouched. Declined opportunities are transient, not motor-failure lessons. Original policies remain. Reserved regression prevents general promotion: general AI Play and old tasks retain safe117; challenge tasks explicitly bind risk118. Complete baseline and both prototypes retained.

Development risk11812/12 versus safe1179/12/simple2/12; five pickups, seven declined, zero damage. Previous24 layouts preserve finishes,24 pickups, zero damage. Reserved hardware risk118 {rr['wins']}/6, damage{rr['damage']}; safe117 {old['wins']}/6. Software repeats risk118 {sw['wins']}/6, damage{sw['damage']}. Retain any counterexample. Challenge chat keeps simple failure but requires actual AI finish; no terrain repair. Old recipe gate unchanged. Candidate/source/asset/engine/session/action/image/repair/outcome checks and task/skill limits remain. Self model describes actual challenge fields. Six transformed references/four refusals per renderer retained; no expanded image support.

{len(v['javaChecks'])} distinct Java methods,165 desktop checks, four actual chat requests, own-process abrupt interruption and explicit resume, actual enemy/pickup-decision screenshots and desktop/mobile guide. Zero model calls or new training. Failed AI challenge remains failed; forged completion refused. First UI interaction checks passed but visual review caught a blank resumed view. Read-only attachment waiting fixes it without weakening ownership; original image retained and complete UI replay checked the decoded live frame and actual reason. Preserve private owners33224/37676, original projects and frozen001–117; no delegation, heavy checks serial. Finish full publication/fetched audits before claiming pushed. Next: contact/displacement recovery for the reserved gap-to-step stall, followed by reconsideration of deferred pickups with bounded retries; broader obstacles and explicit image components, then procedure lifecycle under unchanged32 slots.

"""
for n in ['AGENTS.md','README-CONTRIBUTORS.md','docs/PROJECT_MEMORY.md','docs/DEVELOPMENT_LOG.md','docs/ARCHITECTURE.md']:
 p=r/n;s=p.read_text(encoding='utf-8-sig')
 if not s.startswith('## Edition118 — implemented and locally validated; publication preparation'):p.write_bytes((status+s).encode())
plan="""## Progress through edition118 — 2026-11-11

Declared enemies and obstacles now use original source components and normal inputs in the integrated Game Lab. Optional pickup decisions read actual support, clearance, obstruction and enemy motion; skipped opportunities do not become false failed-learning events. Development12/12 finishes versus9/12 prior policy, with five pickups/seven declined/zero damage; original24 regressions retain finishes and collection. See [edition118](../README-118.md) for separately reserved hardware/software results, actual costs and counterexamples.

One existing Java chat task constructs challenges, retains the fresh simple comparison, requires AI finish, survives interruption in a fresh trial, and reports verified reasons. Old measured-gap repairs remain for old recipes; challenge terrain repairs are refused. Original source/outcome gates remain. [Contract](EDITION_118_CONTRACT.md).

Next119: use contact/displacement observations for the retained seed8 gap-to-step stall. Compare bounded recovery inputs on fresh reserved cases; do not rewrite this failure. Then reconsider deferred opportunities after threats pass, with capped retries and measured benefit. Then broaden multiple-enemy/obstacle/height scenarios and already-wedged recovery. Broader explicit image-component support needs independent images, unknown-region review and refusal measurements. Reusable level procedures still require retirement/reactivation and fresh qualification before unchanged32-slot admission. The historical roadmap below remains planning, not a claim that every step is delivered.

"""
p=r/'docs/PLAYABLE_LEVEL_CREATION_PLAN.md';s=p.read_text(encoding='utf8')
if not s.startswith('## Progress through edition118'):p.write_bytes((plan+s).encode())
