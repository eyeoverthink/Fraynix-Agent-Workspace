from pathlib import Path
import json
r=Path.cwd()
def status(stage,published=''):
 v=json.loads((r/'docs/readmes/127/verification.json').read_text(encoding='utf8'))
 return f"""## Edition127 - {stage}

2026-11-12. Isolated S:/Codex-Work/fraynix-workspace-edition127; parent7369b946. {published}

Java-owned saved-procedure inspection, retirement and reactivation are implemented in the existing UI. Separate lifecycle entity in the SAME atomic checkpoint; original procedure/source payloads unchanged. Historical lookup remains available; new task choices and direct execution refuse retired entries. Reactivation reconstructs original goal/workflow contracts and checks evidence, compatibility, executable graph dependencies and capacity. A full inspection receipt and chained event are retained.32 active,512 retired,eight versions/name,32 changes/managed procedure; bounded JSON. No deletion, automatic adaptation, new LLM calls or weight-training claim. Explicit controls remain outside model authority; running/interrupted work must finish or cancel first.

{len(v['javaChecks'])} distinct Java methods,161 desktop checks, executable kernel harnesses, actual agency regression, offline preparation/package receipt. Actual Electron data/4D geometry/qualified Java summary workflow, full32 capacity, fresh v2 descendant and owned-Java crash/cold restart passed. Native graph regression keeps original physics and labelled unit-host observations distinct. Source/report/default/ancestry, stale snapshot, version/history limits, failed atomic write and interruption checks remain strict. First unit test expected a legitimate v2 import to invalidate immutable v1; corrected test also corrupts original content and refuses it. Original failure retained. Initial UI study had mojibake introduced by file encoding; final UI reopens the SAME project after correction and preserves earlier captures. Original evidence remains dated. See README-127 and docs/readmes/127/original-evidence.json. Hosted CI is separate and was pending at the prepublication check.

Destination: one continuing local Java agent with connected world/browser/canvas/observation surfaces. Next128 observation-to-artifact mission,129 evaluated bounded adaptation,130 coherent daily use. Preserve private projects and frozen001-126; do not relabel old motion proofs under a new engine or repeat successful studies without a reason. No delegation or overlapping heavy/inference work. Read docs/INTEGRATED_AGENT_ROADMAP.md and docs/EDITION_127_CONTRACT.md. Publish only to Fraynix-Agent-Workspace, not historical Chandra/component destinations.

"""
if __name__=='__main__':
 s=status('locally verified; publication preparation','Do not claim pushed until the actual push and fetched audit succeed.')
 for name in ['AGENTS.md','README-CONTRIBUTORS.md','docs/PROJECT_MEMORY.md','docs/DEVELOPMENT_LOG.md','docs/ARCHITECTURE.md']:
  p=r/name;prior=p.read_text(encoding='utf-8-sig');p.write_text(s+prior if not prior.startswith(s) else prior,encoding='utf8')
