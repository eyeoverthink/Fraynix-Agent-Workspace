from pathlib import Path
import json
r=Path.cwd()

def status(stage,published=''):
 v=json.loads((r/'docs/readmes/131/verification.json').read_text(encoding='utf8'))
 d=json.loads((r/'docs/readmes/131/decision.json').read_text(encoding='utf8'))
 a=d['methods']['flow'];b=d['methods']['renewing-flow']
 return f'''## Edition131 — {stage}

2026-11-12. Isolated S:/Codex-Work/fraynix-workspace-edition131; parent a4e50e71. Production candidate locked630c3201, execution protocol/UI d50b718a and binary identity db5fa077 before camera1 decode. Final tested source in docs/readmes/131/verification.json. {published}

Strict normalized-box parser and observation filter remain unchanged. Producers now withhold invalid boxes; Adaptive local refinement respects the same minimum. The exposed130 collapsed-box execution2400 completes with all3 references, mean1.037m, which is software completion rather than certified physical accuracy. New explicit renewing-flow-131 replenishes accepted8..11 landmarks and allows at most24 appearance recovery attempts. Strong, bounded proposals require a second image with consistent flow; no current measurement while pending, hidden or ambiguous. Synthetic large-return recovery passes; initial weak-match refusal and diagnostic sweep are preserved. Two-image fallback adds no reference availability on exposed camera0 development. Renewal fixes starvation in4600; other losses persist.

New camera1 pixels, fixed windows600/1600/2600/3600/4600, locked before decode. Same recorded event/people as camera0, not independent trajectories/population. First eligible seed only; later ground references are scorer-only. Original130 binary separately replayed. Gate passed: {d['gatePassed']}. Candidate {b['available']}/{b['references']} available, {b['withinOneMetre']} within1m, mean accepted {b['meanAcceptedErrorM']}; patched default {a['available']}/{a['references']}, {a['withinOneMetre']} within1m, mean accepted {a['meanAcceptedErrorM']}. All missing/failures remain in denominator. No tuning after this cohort. Legacy-flow and ordinary adjacent forecasts remain defaults. No bound fitting/clipping, no semantic identification, no new model call or weight training, no private webcam capture or native physical actuation. Earlier129 coverage loss and130 provenance correction/failed qualification remain intact.

{len(v['javaChecks'])} distinct Java methods, {v['desktopChecks']} desktop checks, executable kernel integration, agency regression, isolated offline/package preparation and actual app verification. Initial agency launch before package creation failed and is retained; installed package then passes. Reference-only metadata refused by strict import; supported export continues from existing tasks with original clocks. Review selects the saved world and awaits actual frame painting; failed/stale attempts retained. Same-task abrupt Java interruption/cold recovery, actual artifact independent audit, fresh-input reuse, procedure lifecycle, saved physical audit/restart. Read original sources/timings and failures in docs/readmes/131/original-evidence.json; never treat local success or pushed Git as hosted CI success. Original video/frame packs and private projects stay local; limited screenshots attributed.

Next: independent association/occlusion labels and genuinely new trajectories; quantify false reacquisition and contact-anchor error, then separately fit/evaluate uncertainty at fixed horizons before linking physical tolerances to active world goals. Return to coherent daily-use layout without losing the connected Java-owned world/browser/creation application. No delegation or overlapping heavy studies. Node junction read-only. Preserve private projects and frozen001–131. Publish only Fraynix-Agent-Workspace; never relabel proofs or repeat passed heavy studies without new cause.

'''

if __name__=='__main__':
 s=status('locally verified; publication preparation','No131 push claimed until actual push and fetched audit pass.')
 for name in ['AGENTS.md','README-CONTRIBUTORS.md','docs/PROJECT_MEMORY.md','docs/DEVELOPMENT_LOG.md','docs/ARCHITECTURE.md']:
  p=r/name;old=p.read_text(encoding='utf-8-sig');p.write_text(s+old if not old.startswith(s) else old,encoding='utf8')
