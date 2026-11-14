from pathlib import Path
import json,subprocess
r=Path.cwd()

def status(stage,published=''):
 v=json.loads((r/'docs/readmes/146/verification.json').read_text(encoding='utf8'));c=json.loads((r/'docs/readmes/146/comparison.json').read_text(encoding='utf8'))
 return f'''## Edition 146 — {stage}

2026-11-14, America/Los_Angeles. Isolated J:/Codex-Work/fraynix-workspace-edition146 from672894ea. CI checked first:145 release43584281387 and metadata43584459961 both success. Older144 metadata failed CalibrationStoreTest; unchanged test included in this edition's broad local verification. Method, scorer, UI, generator, source selection and harness frozen236d4b2f before fresh pixels. Source-only labels committed before predictions. {published}

Explicit return-guard-146 screens supplemental145 returns without retained144 ambiguity using the existing145 target forecast and scale checks. Monitor146 preserves145 tracker/filter output. Baseline139 continuity and competed145 restorations remain exact. No new thresholds, search, self-anchoring, automatic appearance admission, defaults, permissions or capacity changes. Original-pixel replay measures every additional refusal, including correct returns lost. Uncalibrated motion bounds and identical on-path copies remain limitations; absence of a warning is not identity evidence.

{c['finding']}

{v['scope']}

{(r/'build/attempts146.md').read_text(encoding='utf8').strip()}

{(r/'build/next146.md').read_text(encoding='utf8').strip()}

Keep the unified Java world/browser/creation application central. No agents or overlapping heavy jobs. Shared dependency junction read-only. Preserve private projects and frozen001–146. Publish only Fraynix-Agent-Workspace; fetch and audit. Never relabel historical code-bound evidence.

'''
if __name__=='__main__':
 s=status('locally verified; publication preparation','No publication claim until actual push and fetched audit pass.')
 for name in ['AGENTS.md','README-CONTRIBUTORS.md','docs/ARCHITECTURE.md','docs/PROJECT_MEMORY.md','docs/DEVELOPMENT_LOG.md']:
  p=r/name;old=p.read_text(encoding='utf-8-sig');p.write_text(s+old if not old.startswith(s) else old,encoding='utf8')
 short='## Edition146 / 2026-11-14 — continuity without a warning\n\n[Edition146](../README-146.md) checks returning appearances against pre-loss motion even when no competitor warning fired. Source-selected complete occlusions, exposed regressions and generated identity limits are reported separately. Defaults unchanged; unified Java world/browser/creation remains the destination.\n\n'+(r/'build/next146.md').read_text(encoding='utf8')+'\n\n'
 for name in ['docs/INTEGRATED_AGENT_ROADMAP.md','docs/PLAYABLE_LEVEL_CREATION_PLAN.md']:
  p=r/name;old=p.read_text(encoding='utf-8-sig');p.write_text(short+old if not old.startswith(short) else old,encoding='utf8')
 subprocess.run(['python','-X','utf8','docs/publication146/finalize-inventory146.py'],check=True)
