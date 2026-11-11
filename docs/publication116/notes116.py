"""Refresh live project guidance after validation; frozen numbered editions remain untouched."""
from pathlib import Path
import json
r=Path.cwd();v=json.loads((r/'docs/readmes/116/verification.json').read_text());assert v['passed']
u=json.loads((r/'docs/readmes/116/actual-ui.json').read_text());assert u['passed']
rows='; '.join(f"{x['task']['status']} {x['elapsedMs']/1000:.3f}s" for x in u['requests'])
status=f'''## Edition116 — implemented and locally validated; publication preparation

2026-11-11, America/Los_Angeles. Isolated checkout S:/Codex-Work/fraynix-workspace-edition116, source {u['source']}, parent main2eae6260. CI checked first:115 failed one of863 engine tests (two skipped);114 passed. Exact failure reproduced using the CI-pinned Mesa package in a private Java installation. Nonconstant GLSL330 sampler indexing failed and disabled assertions masked it. Literal sampler branches in both shaders plus explicit shader failures fix rendering; unchanged115 matching and refusals pass. Shader bytes now bind runtime evidence. No recognition threshold weakened.

Final bounded input-only landing forecasts and edge commitments finish12/12 declared hardware layouts versus5/12 original and12/12 simple. Pickups2 versus7 original; retain that tradeoff. Reserved six layouts repeat on software rendering. Six new actual cropped/shifted/scaled references per renderer reconstruct independent candidates; four additional unsupported images per renderer refuse. Wider reference seed15 with a five-tile gap remains a controller counterexample: simple wins, revised stalls near x5.142/y0.539. Reference completion is6/6 simple and5/6 AI, distinct from the narrower12-case comparison. Preserve original CI logs, malformed frames and failed seed4 prototype; never relabel them as final evidence.

{len(v['javaChecks'])} distinct Java methods and165 desktop checks pass. Four actual UI requests: {rows}. Own-process abrupt interruption resumes through the visible Mission control in a fresh native session; old partial trial remains. Actual screenshots wait for a decoded tick0 review frame. Zero model calls; local gpt-oss:20b catalog availability only, no new inference/training claim. Existing limits and procedure-bank admission rules unchanged. All original private projects and Java owners33224/37676 remain untouched. No delegation; heavy checks serial. Next: wider-gap landing/contact recovery, safer optional collection, then explicit procedure lifecycle and broader supported images. Finish documentation hashes, main/tag push and fetched verification before claiming publication. New remote CI reported separately.

'''
for name in ['AGENTS.md','README-CONTRIBUTORS.md','docs/PROJECT_MEMORY.md','docs/DEVELOPMENT_LOG.md','docs/ARCHITECTURE.md']:
 p=r/name;s=p.read_text(encoding='utf-8-sig')
 if not s.startswith('## Edition116 — implemented and locally validated; publication preparation'):p.write_bytes((status+s).encode())
plan='''## Progress through edition116 — 2026-11-11

The retained seed0 controller failure is corrected using owned Java landing forecasts and ordinary input, without editing physics or actor state. The original policy remains an explicit baseline. Final narrower-layout comparison: revised12/12, original5/12, simple12/12; power-ups2 versus7 original. Six reserved layouts repeat under software rendering. The wider reference cohort still reveals a five-tile-gap failure on seed15: simple finishes6/6 reconstructed levels, revised5/6. This is the next contact/landing-recovery case, alongside restoring safe pickup pursuit without losing finishes.

CI-first investigation reproduced115's one failed reference test: invalid GLSL330 sampler indexing produced malformed frames on Mesa. Portable literal sampler branches and explicit shader exceptions fix the renderer; image thresholds and source/outcome checks remain. Six fresh native reference scenes, declared crops/shifts/scales, independent reconstruction parameters and actual playtests pass on both renderers; missing finish, incomplete floor, covered player and blank image refuse. No arbitrary web-image claim. Shader identity prevents reuse of old proofs as current evidence.

[Edition116](../README-116.md) and [its contract](EDITION_116_CONTRACT.md) preserve complete costs, original failures, actual chat/restart evidence and remaining limits. Procedure retirement/reactivation with original provenance is still required before new admission into the unchanged32-slot bank. The phase descriptions below are the original roadmap, not claims that every broader item is delivered.

'''
p=r/'docs/PLAYABLE_LEVEL_CREATION_PLAN.md';s=p.read_text(encoding='utf8')
if not s.startswith('## Progress through edition116'):p.write_bytes((plan+s).encode())
