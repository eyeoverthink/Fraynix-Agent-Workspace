from pathlib import Path
import json
r=Path.cwd()
def status(stage,published=''):
 v=json.loads((r/'docs/readmes/147/verification.json').read_bytes());c=json.loads((r/'docs/readmes/147/comparison.json').read_bytes())
 return f'''## Edition 147 — {stage}

2026-11-14, America/Los_Angeles. Isolated J:/Codex-Work/fraynix-workspace-edition147 from9f790c90. CI checked first; initial146 documentation passed with compilation/tests still pending. Method/public protocol frozen217173a5. User chose webcam; separate private capture/preparation protocol frozen before camera access, with pre-access lifecycle and disjoint-context corrections preserved in Git. Sources and labels committed before predictions. {published}

{c['finding']}

{v['scope']}

No new tracker or threshold, no automatic reference admission and no default promotion. Existing145/146 methods and legacy-flow default remain intact. Source eligibility, admitted references, verified execution and useful recovery are separate results. Camera pixel files remain private; only source hashes, attributed labels and camera-free actual app results are published. Keep the unified Java world/browser/creation application central.

{(r/'build/attempts147.md').read_text(encoding='utf8').strip()}

## Next steps

{(r/'build/next147.md').read_text(encoding='utf8').strip()}

No agents; ONE heavy job at a time. Shared dependency junction read-only. Preserve private projects and frozen001–146. Publish only Fraynix-Agent-Workspace, then fetch and audit exact bytes. Never relabel historical code-bound evidence.

'''
if __name__=='__main__':
 s=status('locally verified; publication preparation','No publication claim until actual push and fetched audit pass.')
 for name in ['AGENTS.md','README-CONTRIBUTORS.md','docs/ARCHITECTURE.md','docs/PROJECT_MEMORY.md','docs/DEVELOPMENT_LOG.md']:
  p=r/name;old=p.read_text(encoding='utf-8-sig');p.write_bytes((s+old if not old.startswith(s) else old).encode())
