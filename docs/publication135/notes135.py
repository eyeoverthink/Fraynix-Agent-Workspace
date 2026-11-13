from pathlib import Path
import json
r=Path.cwd()

def status(stage,published=''):
 v=json.loads((r/'docs/readmes/135/verification.json').read_text(encoding='utf8'))
 return f'''## Edition 135 — {stage}

2026-11-13. Isolated S:/Codex-Work/fraynix-workspace-edition135; parent da90a8d0. Frozen algorithm/scorer 4f3b719f, lock2f4bac45 before fresh pixels; source-only amendment b0223316 before extra sources; labels92839255 before predictions. Raw originally evaluated CRLF source-label/lock bytes restored to Git d492ff12, with declared whitespace4e3cff54; no label values or method changed. Actual tested source/build in docs/readmes/135/verification.json. {published}

Explicit appearance-continuity-135 preserves incumbent accepted outputs, adds bounded6-example provenance with motion-cycle/coverage/color checks and two-confirmation recovery only after loss. continuity-no-bank-135 is the ablation. Default legacy-flow, filter and forecast unchanged. Independent AppearanceAudit135 scores admitted examples using external labels never seen by tracking. VerifiedAppearances135 is separate POST-EVALUATION curation: immutable64-set/1MB bound, original source/audit/code rechecks, descriptors rebuilt from independent fully visible reference boxes, exact numeric JSON export. No automatic training/import feedback; no person recognition or native actuation.

Fresh fixed windows: Tiger2 59–154, Box439–534, David3 57–152 (camera partial cover) and Bird1 97–192 (RENDERED cloud disappearance/return). Four96-image windows,380 later labels:327 visible/partial,33 hidden,20 uncertain. External OTB boxes; source-only AI visibility, not human adjudication. No independent distractor labels, no real full out-of-frame return. Frozen scorer breaks recovery episodes at uncertainty; no claimed delay across uncertain reveal. All source/labels frozen before predictions; no outcome tuning.

All380 candidate/control tracker/filter outputs exactly match incumbent, including drift. Every method86/327 correct,96 missing,145 wrong overlaps; camera67/285, rendered19/42. No bank recoveries.46 motion-admitted examples:18 independently correct,28 incorrect. Automatic bank FAILED qualification; defaults kept. Separate post-evaluation export retains16 fully visible supported appearances (11 camera,5 rendered), not a remedy to the failed admission rule or proof of useful learned recovery. First synthetic twin-search failure and all subsequent results retained.

{len(v['javaChecks'])} distinct Java checks across broad249 and3 new export tests, {v['desktopChecks']} desktop checks, kernel integration, actual agency regression and isolated offline/package checks. Six actual UI runs reproduce570 frozen samples with independent overlap/admission arithmetic, source overlays, UI export, stop/resume prefix preservation and cold restart after owned-Java termination. All four curated sets reverify; native world unchanged. Hosted CI separate. No LLM calls, weights, private camera or project changes.

Observed actual 95-frame task times grow from 32.5 seconds to 422.7 seconds across the six-run history; workloads differ, so cause needs profiling. This does not demonstrate live camera throughput. Preserve all source/outcome checks while investigating repeated verification and history cost.

Next: foreground/identity evidence against known drift, then new held-out camera full occlusion/return with independently adjudicated target/distractor labels. Curated bank reuse must be explicit, provenance checked and evaluated on different footage. Keep unified Java world/browser/creation goals central; improve physical accuracy before increasing authority. No delegation or overlapping heavy work. Shared node junction read-only. Preserve private projects and frozen001–135. Publish only Fraynix-Agent-Workspace and audit fetched Git. Never rerun passed expensive checks without new cause.

'''

if __name__=='__main__':
 s=status('locally verified; publication preparation','No135 push claimed until actual push and fetched audit pass.')
 for name in ['AGENTS.md','README-CONTRIBUTORS.md','docs/ARCHITECTURE.md','docs/PROJECT_MEMORY.md','docs/DEVELOPMENT_LOG.md']:
  p=r/name;old=p.read_text(encoding='utf-8-sig');p.write_text(s+old if not old.startswith(s) else old,encoding='utf8')
