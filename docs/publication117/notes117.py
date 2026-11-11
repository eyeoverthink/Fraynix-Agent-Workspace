"""Refresh current project guidance without rewriting published editions."""
from pathlib import Path
import json
r=Path.cwd();v=json.loads((r/'docs/readmes/117/verification.json').read_text(encoding='utf8'));assert v['passed']
u=json.loads((r/'docs/readmes/117/actual-ui.json').read_text(encoding='utf8'));assert u['passed']
totals=v['controllerCases'];old=totals['landing116'];rows='; '.join(f"{x['task']['status']} {x['elapsedMs']/1000:.3f}s" for x in u['requests'])
status=f'''## Edition117 — implemented and locally validated; publication preparation

2026-11-11, America/Los_Angeles. Isolated S:/Codex-Work/fraynix-workspace-edition117, source {u['source']}, parent e015b692. CI checked first:116 main43056641370 was still running; setup/software renderer/docs passed and tests pending. Do not claim remote CI success from local validation. Reproduced seed15 five-tile-gap stall with original landing116: jumping over a falling mushroom produces a slow gap approach and corner jam. Read-only contacts and displacement distinguish actual movement from requested velocity. New owned Java Pursuit117 uses a bounded0.2s intercept and defers optional jump while the mushroom descends; unchanged edge and landing guards continue. This prevents the recorded jam; arbitrary already-wedged escape remains unproven.

Declared hardware comparison: safe117 finishes24/24, collects24, damage0; landing116 finishes{old['wins']}/24 with{old['pickups']} pickups. All12 old regression finishes preserved. Six wider development and six reserved cases included; reserved six repeat on software rendering. Six transformed references per renderer now require both simple and safe117 completion, one pickup and zero damage; four fresh image refusals per renderer and existing corruption/unknown-image checks remain. No added enemy challenge, so zero damage is scoped. Legacy115 and landing116 remain explicit baselines. Actual chat derives pickup/damage counts from verified native trial. Physics, transforms, source/outcome checks and limits unchanged.

{len(v['javaChecks'])} distinct Java methods,165 desktop checks, four visible requests: {rows}. Own Java abrupt interruption, explicit resume, distinct new session and retained partial trial verified. Actual powered-up frame and desktop screenshots retained. The skipped native test invocation exposed missing Gradle inputs; root game JAR/runtime dependencies/assets/levels/study options now invalidate native tests. Original skipped invocation and actual rerun retained. Zero model calls, no new model training. Preserve private owners33224/37676, projects and frozen001–116. No delegation; heavy checks serial. Finish publication and fetched audit before claiming pushed. Next: obstacle/enemy/risky-pickup decisions and separate jam recovery, then broader explicit image components and procedure lifecycle under unchanged32 slots.

'''
for name in ['AGENTS.md','README-CONTRIBUTORS.md','docs/PROJECT_MEMORY.md','docs/DEVELOPMENT_LOG.md','docs/ARCHITECTURE.md']:
 p=r/name;s=p.read_text(encoding='utf-8-sig')
 if not s.startswith('## Edition117 — implemented and locally validated; publication preparation'):p.write_bytes((status+s).encode())
plan='''## Progress through edition117 — 2026-11-11

The wider seed15 failure is corrected at its approach: bounded observed-item interception defers the optional chase jump while preserving the existing gap-safety jump. The original stalled contact/displacement trace remains. No physics, transform, collider or finish rule changed; generic escape from an already-wedged character is a separate future task.

All24 declared hardware layouts finish with24 pickups and no damage. Twelve prior regressions, six wider development and six reserved cases; the reserved six repeat on software rendering. Six existing transformed-reference cases regenerated per renderer require simple and revised completion, while four unsupported images still refuse. Actual chat exposes checked pickup/damage outcomes and resumes a wider-gap task after abrupt interruption. Full costs and original failed/skipped checks remain in [edition117](../README-117.md) and [its contract](EDITION_117_CONTRACT.md).

Next: declare enemy/obstacle layouts, unreachable and risky pickups, alternate heights and already-wedged states; compare completion, survival, collection and total work. Broader explicitly supported image components follow. Procedure retirement/reactivation with original provenance remains required before admission into the unchanged32-slot bank. The older roadmap below remains historical planning, not a claim that every item is delivered.

'''
p=r/'docs/PLAYABLE_LEVEL_CREATION_PLAN.md';s=p.read_text(encoding='utf8')
if not s.startswith('## Progress through edition117'):p.write_bytes((plan+s).encode())
