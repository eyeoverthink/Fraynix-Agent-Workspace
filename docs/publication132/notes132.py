from pathlib import Path
import json
r=Path.cwd()
def status(stage,published=''):
 v=json.loads((r/'docs/readmes/132/verification.json').read_text(encoding='utf8'))
 d=json.loads((r/'docs/readmes/132/comparison.json').read_text(encoding='utf8'))
 a=d['summary']['legacy-flow'];b=d['summary']['renewing-flow-131']
 return f'''## Edition132 — {stage}

2026-11-12. Isolated S:/Codex-Work/fraynix-workspace-edition132; parent434616ec. Java annotation/audit source65e790b5; actual tested source in docs/readmes/132/verification.json. {published}

Source-only image annotation, separate visibility/contact states, immutable source/frame/image bindings, declared annotator/provenance/exposure and pixel allowance. Unknown/hidden contact has no invented coordinates. Export/import verifies the original set identity. Audit accepts only the same completed dataset/object binding; source pixels, trace, implementation, optional clock/geometry and recomputed outcomes checked on read.64 labels/64 audits retained, no pruning. The initial UI audit recheck exceeded30 seconds because each image reread the growing history. The final source checks that history once per immutable dataset snapshot and still hashes every original image, including unlabelled frames. Two completed tracker runs and old audit identities are preserved; newly computed audit identities must match their original numbers. Labels never enter tracking, filtering, forecasts, fitting or native actuation. All defaults and earlier failed promotion/coverage evidence unchanged.

25 fixed sample slots across already exposed EPFL camera1 windows600/1600/2600/3600/4600, ordinals1/25/50/75/95.22 observable contacts,3 unobservable. Retrospective AI source-image annotations with prior aggregate-result exposure declared, not blinded human truth, fresh trajectories or independent metrology. No fully occluded/out-of-view target samples in this real subset; those refusal paths have synthetic software tests only. Default available {a['contactAvailable']}/22, within3px {a['withinLabelRadius']}/22, mean accepted filtered error {a['filteredAnchorMeanErrorPx']:.3f}px; candidate {b['contactAvailable']}/22, within3px {b['withinLabelRadius']}/22, mean {b['filteredAnchorMeanErrorPx']:.3f}px. Missing contacts stay in the denominator. No promotion or uncertainty fit. Shared-calibration metre errors are contact comparisons, not camera certification. Physical accuracy remains unresolved.

{len(v['javaChecks'])} distinct Java methods, {v['desktopChecks']} desktop checks, kernel integration, actual agency regression, isolated offline/package checks. Real app contact click/freeze, all labels frozen before new tracking,10 serial runs, independent host arithmetic, export/import and same-evidence cold restart after owned-Java termination. Private projects untouched, no new LLM call, weight training, camera capture or native motion. Original attempts and sources retained in docs/readmes/132/original-evidence.json. Hosted CI checked first and reported separately; local pass/push is not hosted success.

Next: human adjudication and independent identity/contact sequences including full occlusion; explicit contact estimator that can withhold, separately evaluated fixed-horizon uncertainty, then justified physical tolerances in durable world goals. Keep the connected Java world/browser/creation app and daily-use mission/source/skills layout central. No delegation or overlapping heavy studies. Node junction read-only. Preserve private projects and frozen001–132. Publish only Fraynix-Agent-Workspace; never relabel proofs or repeat passed heavy checks without new cause.

'''
if __name__=='__main__':
 s=status('locally verified; publication preparation','No132 push claimed until actual push and fetched audit pass.')
 for name in ['AGENTS.md','README-CONTRIBUTORS.md','docs/PROJECT_MEMORY.md','docs/DEVELOPMENT_LOG.md','docs/ARCHITECTURE.md']:
  p=r/name;old=p.read_text(encoding='utf-8-sig');p.write_text(s+old if not old.startswith(s) else old,encoding='utf8')
