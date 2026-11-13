from pathlib import Path
import json
r=Path.cwd()
def status(stage,published=''):
 v=json.loads((r/'docs/readmes/133/verification.json').read_text(encoding='utf8'));d=json.loads((r/'docs/readmes/133/comparison.json').read_text(encoding='utf8'));a=d['summary']['default-support'];b=d['summary']['candidate']
 return f'''## Edition133 — {stage}

2026-11-12. Isolated S:/Codex-Work/fraynix-workspace-edition133; parent62c62fda. Candidate/audit/scorer locked11bbeea7 before comparison1600/3600/4600. Actual tested source and binary receipt in docs/readmes/133/verification.json. {published}

Explicit association-contact-133: frozen seed/context appearance, bounded scale/search, competing-match refusal, local optical-flow corroboration, strong two-image recovery after loss. New VisualSupport133 uses frozen lower-object contrast and connected support pixels; withholds clipping, insufficient separation, sparse/missing support, never defaults to box bottom. It also works on default tracker boxes through ContactAudit133. Source-only132 labels and original frames are scorer-only. New audit binds full original run/code, dataset, labels, current support implementation and recomputed outcomes; every original image/frame checked, including unlabelled samples.64 immutable records, no pruning; export and cold restart. Earlier stored boxes may be rescored, with trackerReplayed=false and original code identity retained; this is not a new tracking proof. Existing metric anchors, filter, forecast and legacy-flow default unchanged. Older incompatible tracking proofs never relabelled.

All25 labels/22 visible contacts/3 unobservable from previously exposed EPFL camera1. Development600/2600; code locked before the new implementation comparison1600/3600/4600; no tuning afterward. Retrospective AI labels, not blinded human truth, independent metrology or new trajectories. Default-box support available {a['available']}/22, within3px {a['within']}/22, mean accepted {a['meanPx']:.3f}px; strict association support {b['available']}/22, within3px {b['within']}/22, mean {b['meanPx']:.3f}px. Missing contacts remain in denominator. Precision improves on a subset but availability drops; candidate is unpromoted. Full real occlusion/re-entry rates remain unmeasured. Physical accuracy/general association unresolved; preserve129 coverage failure and130/131/132 limits.

{len(v['javaChecks'])} Java checks, {v['desktopChecks']} desktop checks, kernel integration, actual agency regression, isolated package checks. Actual app two95-frame source runs match frozen/published numeric evidence; independent contact arithmetic, visible audit/overlay, export and abrupt owned-Java termination/cold restart recheck pass. Original attempts and full evidence retained in docs/readmes/133/original-evidence.json. Hosted CI checked FIRST and reported separately. No new LLM calls, weights, private capture or native actuation. Original public pixels and private projects remain local.

Next: independent human association/contact review, ambiguous seed and full occlusion/re-entry sequences, measured availability improvement without unsupported reassignment, then calibrated physical tolerances and source-age in durable Java goals. Keep the connected world/browser/creation app, skills and cohesive daily-use interface central. No delegation or overlapping heavy studies. Node junction read-only; preserve private projects and frozen001–133. Publish only Fraynix-Agent-Workspace and verify fetched publication; do not repeat passed heavy checks without new cause.

'''
if __name__=='__main__':
 s=status('locally verified; publication preparation','No133 push claimed until actual push/fetched audit pass.')
 for name in ['AGENTS.md','README-CONTRIBUTORS.md','docs/PROJECT_MEMORY.md','docs/DEVELOPMENT_LOG.md','docs/ARCHITECTURE.md']:
  p=r/name;old=p.read_text(encoding='utf-8-sig');p.write_text(s+old if not old.startswith(s) else old,encoding='utf8')
