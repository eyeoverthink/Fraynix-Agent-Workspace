from pathlib import Path
import json
r=Path.cwd()
def status(stage,published=''):
 v=json.loads((r/'docs/readmes/149/verification.json').read_bytes());c=json.loads((r/'docs/readmes/149/comparison.json').read_bytes())
 return f'''## Edition 149 — {stage}

2026-11-15, America/Los_Angeles. Isolated J:/Codex-Work/fraynix-workspace-edition149 from d3012d4b. Predecessor CI checked first; preserve pending and completed receipts separately. Initial freeze 9e19970a failed all nine reserved generated recovery cases. The original result is preserved. Revised freeze 9090c62d adds bounded recent-match search proposals and geometry guards without changing identity thresholds, before the second reserved cohort. Existing 148 camera packs and source-only labels are exposed regression material. {published}

{c['finding']}

{v['scope']}

New explicit identity-return-149 / identity-monitor-149. Parent 148 is unchanged. Four adjacent full-object source matches plus per-reference optical flow start a new observed motion segment; missing/ambiguous views reset it. The initial selected crop and verified donor crops supply 16×16 RGB/spatial features. Donor features are rebuilt from original pixels before use; the initial selected identity remains an input assumption. Known competing matches stay a veto. No predicted anchor, automatic appearance admission, physical-action authority, enlarged old motion horizon or default change. Motion is normalized image/sample, not calibrated seconds or physical velocity. Invisible identical substitutions remain unobservable.

{(r/'build/attempts149.md').read_text(encoding='utf8').strip()}

## Next steps

{(r/'build/next149.md').read_text(encoding='utf8').strip()}

Keep the unified Java world/browser/creation application central. Original camera images, sampled RGB appearance arrays and projects stay local. Derived public camera views preserve measurements and decisions, plus hashes of omitted features and the unchanged private receipts. Public views cannot replace original evidence in the verifier. Preserve frozen 001–148 and the readonly dependency junction. No agents; ONE heavy job at a time. Publish only Fraynix-Agent-Workspace and fetch exact main/tag bytes after pushing. Tests and camera outcomes are separate claims; never describe exposed or generated cases as fresh-camera qualification.

'''
if __name__=='__main__':
 s=status('locally verified; publication preparation','Not published until actual push and fetched audit pass.')
 for name in ['AGENTS.md','README-CONTRIBUTORS.md','docs/ARCHITECTURE.md','docs/PROJECT_MEMORY.md','docs/DEVELOPMENT_LOG.md']:
  p=r/name;p.write_bytes((s+p.read_text(encoding='utf-8-sig')).encode())
