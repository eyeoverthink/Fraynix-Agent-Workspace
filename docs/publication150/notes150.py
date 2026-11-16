from pathlib import Path
import json,datetime
from zoneinfo import ZoneInfo
r=Path.cwd()
def status(stage,published=''):
 v=json.loads((r/'docs/readmes/150/verification.json').read_bytes());c=json.loads((r/'docs/readmes/150/comparison.json').read_bytes())
 return f'''## Edition 150 — {stage}

{datetime.datetime.now(ZoneInfo('America/Los_Angeles')).date()}, America/Los_Angeles. Isolated J:/Codex-Work/fraynix-workspace-edition150 from e5479b71. CI checked before implementation; pending/failure/success receipts remain separate. Method freeze b851a1d8; separate camera contract frozen before real preview. Source-only labels committed before predictions. {published}

{c['finding']}

{v['scope']}

StartingReference150 retains one explicitly requested independent starting observation after the first three consecutive full/correct observations, IoU >= 0.85, and other-object overlap < 0.2. Zero learned examples. No fallback to later source samples. Learned-only 135 admission is unchanged. Every use rechecks source/code/labels/outcomes/descriptors. Existing 149 tracker mathematics and default legacy-flow unchanged. Explicit UI retain-start150; one shared 64-set limit and six selected examples. No automatic admission or physical action authority.

Actual generated positive-segment interruption and replay are separate from fresh-camera performance. Camera recorder uses real countdowns and saved-sample holds, releases camera after completion or missed deadline, preserves failures. No retiming/trimming or silent qualification changes.

{(r/'build/attempts150.md').read_text(encoding='utf8').strip()}

## Next steps

{(r/'build/next150.md').read_text(encoding='utf8').strip()}

Keep the unified Java world/browser/creation app central. Original camera images, sampled RGB arrays and private projects stay local. Derived public evidence views preserve hashes but cannot replace original verifier inputs. Preserve frozen editions 001–149 and readonly dependencies. No agents; ONE heavy job at a time. Publish only Fraynix-Agent-Workspace, then fetch and verify actual main/tag bytes. Report hardware/runtime costs and every unavailable/failed outcome honestly.

'''
if __name__=='__main__':
 s=status('locally verified; publication preparation','No publication claim until actual push and fetched audit pass.')
 for name in ['AGENTS.md','README-CONTRIBUTORS.md','docs/ARCHITECTURE.md','docs/PROJECT_MEMORY.md','docs/DEVELOPMENT_LOG.md']:
  p=r/name;p.write_bytes((s+p.read_text(encoding='utf-8-sig')).encode())
