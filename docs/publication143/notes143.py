from pathlib import Path
import json,subprocess
r=Path.cwd()
def status(stage,published=''):
 v=json.loads((r/'docs/readmes/143/verification.json').read_text(encoding='utf8'));c=json.loads((r/'docs/readmes/143/comparison.json').read_text(encoding='utf8'));g=c['byDomain']['real-camera']['reference-confirmation-143']['tracking'];b=c['byDomain']['real-camera']['foreground-guard-139']['tracking'];a=c['byDomain']['real-camera']['reference-confirmation-143']['additional'];s=c['byDomain']['generated-control']['reference-confirmation-143']['tracking']
 return f'''## Edition 143 — {stage}

2026-11-13. Isolated J:/Codex-Work/fraynix-workspace-edition143 from fe1e5c3f. CI checked first: edition142 release/metadata running; edition140 release passed. Method, scorer, source protocol and app harness frozen at 5cad54b5 before new annotations or images. Source-only labels precede predictions. {published}

Explicit reference-confirmation-143 reuses up to six verified full-view appearances through the existing137 resolver. Original donor images, labels, code and outcomes reverified; separate source/evaluation bytes and same-object declaration required. Fixed full/75%-visible regions, textured/color-supported match, competitor rejection and three adjacent motion confirmations. Visible support and inferred whole geometry remain distinct; no metric contact or semantic identity claim. Actual underlying flow loss required. No target predictions admitted, no baseline reset, new runtime dependency, authority/capacity/default change. Guard139 accepted tracker proposals preserved. Monitor preserves tracker/filter outputs; seed-only control measures reference benefit. Every decision replays original pixels before independent outcome scoring.

Real-camera evaluation: candidate {g['correct']}/{g['visible']} correct, {g['missing']} missing, {g['overlapFailures']} wrong and {g['unsupported']} hidden outputs. Guard139: {b['correct']} correct, {b['missing']} missing, {b['overlapFailures']} wrong and {b['unsupported']} hidden outputs. Additional raw recoveries: {a}. Generated controls separately: {s['correct']}/{s['visible']} correct, {s['overlapFailures']} wrong, {s['unsupported']} hidden and {s['confirmedDistractorMatches']} labelled-other matches. No default promotion or tuning on fresh outcomes. External OTB rectangles plus source-only AI visibility/competitors, not independent human adjudication. Generated geometry is not camera qualification.

{len(v['javaChecks'])} Java tests and {v['desktopChecks']} desktop checks; kernel, agency, offline preparation and packaging passed. Actual app checks source and outcome arithmetic, provenance, monitor equivalence, accepted baseline preservation, complete export, stopped-prefix resume, owned-process crash/reopen, exact completed bytes/timestamps, zero completed-state writes and donor/evaluation tamper refusal/restoration. Native physical world unchanged. Hosted CI status is separate. Original attempts retained, including one test fixture that inserted an invalid JSON value before its assertion; corrected before freeze. Production and frozen history remain intact.

{(r/'build/next143.md').read_text(encoding='utf8').strip()}

Keep durable Java world/browser/creation goals central. No agents or overlapping heavy jobs. Shared node dependency junction read-only. Preserve private projects and frozen editions001–143. Publish only Fraynix-Agent-Workspace; fetch and verify exact bytes. Never relabel incompatible historical proofs.

'''
if __name__=='__main__':
 s=status('locally verified; publication preparation','No edition143 publication claimed until push and fetched verification pass.')
 for name in ['AGENTS.md','README-CONTRIBUTORS.md','docs/ARCHITECTURE.md','docs/PROJECT_MEMORY.md','docs/DEVELOPMENT_LOG.md']:
  p=r/name;old=p.read_text(encoding='utf-8-sig');p.write_bytes((s+old if not old.startswith(s) else old).encode())
 short='## Edition 143 / 2026-11-13 — partial views from verified appearances\n\nThe unified Java world/browser/creation mission continues. [Edition143](../README-143.md) adds an explicit partial-view experiment with source provenance, original-pixel replay and independent outcomes. Real footage and generated controls remain separate; defaults and all checks retained.\n\n'+(r/'build/next143.md').read_text(encoding='utf8')+'\n\n'
 for name in ['docs/INTEGRATED_AGENT_ROADMAP.md','docs/PLAYABLE_LEVEL_CREATION_PLAN.md']:
  p=r/name;old=p.read_text(encoding='utf-8-sig');p.write_bytes((short+old if not old.startswith(short) else old).encode())
 subprocess.run(['python','docs/publication143/finalize-inventory143.py'],check=True)
