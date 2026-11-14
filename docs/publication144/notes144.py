from pathlib import Path
import json,subprocess
r=Path.cwd()
def status(stage,published=''):
 v=json.loads((r/'docs/readmes/144/verification.json').read_text(encoding='utf8'));c=json.loads((r/'docs/readmes/144/comparison.json').read_text(encoding='utf8'))
 return f'''## Edition 144 — {stage}

2026-11-14 UTC. Isolated J:/Codex-Work/fraynix-workspace-edition144 from5f36761f. CI143 checked first, running at that check; hosted success remains separate. Freezebd2f83f0 before fresh generated pixels/labels; labels58d78ef6. Actual app startup refused two invalid interface middle-dot bytes; encoding-only amendmentc581f36f BEFORE evaluation, original lock/UI/source manifest preserved. No tracking/scorer/label/pixel change. Reused FaceOcc1 explicitly exposed regression. {published}

Explicit ambiguity-guard-144 retains strong competing-location evidence through underlying-flow loss, wrapping unchanged143 confirmations/search. No expiry or identity proof from a disappeared rival. Existing139 accepted continuity clears the latch; all accepted139 proposals preserved. Monitor144 matches143 tracker/filter exactly. Independent audit replays every original pixel and scores blocked correct/incorrect/unknown parent proposals as well as actual additional returns. No default, capacity, source-admission, authority, dependency or baseline reset changes.

{c['finding']}

{len(v['javaChecks'])} Java checks, {v['desktopChecks']} desktop checks, kernel, agency, offline/package verification and actual app integrity/restart/tamper checks passed. Seven focused tests passed first attempt. All attempts retained. Full costs, original source/code/outcome checks and screenshot records remain available. No fresh camera qualification, physical accuracy promotion or semantic identity oracle.

{(r/'build/next144.md').read_text(encoding='utf8').strip()}

Keep the durable unified Java world/browser/creation application central. No agents or overlapping heavy jobs. Shared node junction read-only; preserve private projects and frozen001–144. Publish only Fraynix-Agent-Workspace; fetch and audit exact bytes. Never relabel incompatible historical proofs.

'''
if __name__=='__main__':
 s=status('locally verified; publication preparation','No144 publication claimed until actual push/fetched audit.')
 for name in ['AGENTS.md','README-CONTRIBUTORS.md','docs/ARCHITECTURE.md','docs/PROJECT_MEMORY.md','docs/DEVELOPMENT_LOG.md']:
  p=r/name;old=p.read_text(encoding='utf-8-sig');p.write_bytes((s+old if not old.startswith(s) else old).encode())
 short='## Edition144 / 2026-11-14 — ambiguity continuity\n\n[Edition144](../README-144.md) retains competing-object evidence through loss. Pixel replay and independent outcomes measure prevented errors and useful recovery withheld. Explicit experiment; defaults unchanged. Unified Java world/browser/creation remains the destination.\n\n'+(r/'build/next144.md').read_text(encoding='utf8')+'\n\n'
 for name in ['docs/INTEGRATED_AGENT_ROADMAP.md','docs/PLAYABLE_LEVEL_CREATION_PLAN.md']:
  p=r/name;old=p.read_text(encoding='utf-8-sig');p.write_bytes((short+old if not old.startswith(short) else old).encode())
 subprocess.run(['python','-X','utf8','docs/publication144/finalize-inventory144.py'],check=True)
