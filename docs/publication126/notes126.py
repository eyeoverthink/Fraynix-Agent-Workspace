from pathlib import Path
import json
r=Path.cwd()
def status(stage,published=''):
 v=json.loads((r/'docs/readmes/126/verification.json').read_text(encoding='utf8'))
 return f'''## Edition126 — {stage}

2026-11-12. Isolated S:/Codex-Work/fraynix-workspace-edition126; parent246c70d2; production46e21739. {published}

Real local gpt-oss:20b, CPU-only four threads, serial work. Six actual chat requests: three browser tasks including owned-process interruption/recovery, two persistent-world arrangements and one generated calculator. Java owns task continuity and verification. Explicit browser criteria are immutable objective input; a separate trusted host reads actual DOM values, Java compares them, and checked episodes retain the original frames/journal. Public learning endpoints cannot fabricate browser completion. Historical results are dated, not fresh observations. World/native and creation verifiers remain unchanged. Direct browser baselines omit durable admission; artifact replay reuses generated files and is not fresh generation.

{len(v['javaChecks'])} distinct successful Java checks,161 desktop checks, actual agency regression, isolated offline preparation/package receipt, independent native targets/rotation/speed, page text/exactly-one submission and calculator decimal/reset checks. Failed fixtures, admission, numeric witness-event naming and collapsed-settings driver attempts preserved. Completed A was reverified in the SAME project before continuing; no failed record or original clock replaced. See README-126, docs/EDITION_126_CONTRACT.md and docs/readmes/126/original-evidence.json. Hosted CI is separate; do not imply success while pending.

The destination remains one continuing local Java agent with connected world/browser/canvas/observation surfaces. No general intelligence, unrestricted website competence, weight training, cross-scene learning or automatic procedure adaptation claimed. Next127: compatible procedure lifecycle (inspect, retire, reactivate, preserve descendants), then128 observation-to-artifact,129 evaluated adaptation,130 daily use. Preserve private projects and frozen001–126. No delegation or overlapping heavy/inference studies. Existing24 active chat tasks/512 archive limit unchanged; prior component repos remain historical.

'''
if __name__=='__main__':
 s=status('locally verified; publication preparation','Do not claim publication until actual push/fetched audit succeeds.')
 for name in ['AGENTS.md','README-CONTRIBUTORS.md','docs/PROJECT_MEMORY.md','docs/DEVELOPMENT_LOG.md','docs/ARCHITECTURE.md']:
  p=r/name;prior=p.read_text(encoding='utf-8-sig');p.write_text(s+prior if not prior.startswith(s) else prior,encoding='utf8')
