from pathlib import Path
import json
r=Path.cwd()
def status(stage,published=''):
 v=json.loads((r/'docs/readmes/134/verification.json').read_text(encoding='utf8'))
 return f'''## Edition 134 — {stage}

2026-11-12. Isolated S:/Codex-Work/fraynix-workspace-edition134; parent dcfa037f. Candidate/audit/scorer source c1c163bf, lock 48c3ae37 before selected OTB pixels; source labels 3e45395d before predictions. Original annotation line endings preserved. Actual tested source/build receipt in docs/readmes/134/verification.json. {published}

Explicit identity-recovery-134 uses frozen three-part RGB appearance, bounded scales, whole-image recovery search, spatial competitors and two-image confirmation after loss. No appearance updates, person recognition or native actuation. Existing legacy-flow/forecast/defaults and earlier candidates unchanged. New AssociationAudit134 imports immutable independent target boxes plus separately declared visibility/other objects.64 label sets/64 audits, no pruning. Every original image and complete trace binding is checked; labels never guide tracking. Original run/label/source/code identities and recomputed outcomes survive export and restart; changed code proofs are refused, not relabelled.

Three fixed sampled tasks from two OTB recordings: Jogging targets 1/2 (77 images each, stride 4) and Lemming first 476 source images (96 images, stride 5).247 later labels:246 visible/partial and one fully hidden. External benchmark boxes; Codex source-only AI visibility before predictions, not independent human adjudication. Lemming retains an eye during cover and is partial, not fully occluded. No real full out-of-frame re-entry in this cohort. All images/labels fixed before runs; no tuning afterward.

Candidate FAILED: 7/246 correctly matched visible samples versus incumbent 83/246 and prior candidate 23/246. All returned measurements in this small set meet IoU0.5; zero confirmed labelled-other matches is not proof of reliable identity because most targets are missing and Lemming distractors are not labelled. Incumbent alone recovers after the full occlusion, at sample23/source93, four sampled intervals after visibility returns. Candidate has three correct reacquisitions elsewhere, not success on the labelled full-occlusion episode. Keep defaults; general association and physical accuracy unresolved.

{len(v['javaChecks'])} Java checks, {v['desktopChecks']} desktop checks, kernel integration, actual agency regression and isolated package verification. Actual app four runs/323 tracker-filter samples equal frozen traces; independent host overlap/distractor checks, source overlays, exact label/audit export and reimport, owned-Java termination and cold restart pass. Private projects untouched. Original evidence retained; hosted CI checked first and reported separately. No new LLM calls or weight training.

Next: preserve incumbent continuity; investigate pose/color changes rejected by frozen parts, bounded appearance-bank provenance and contamination refusal, then fresh independently human-adjudicated full occlusion/out-of-frame returns with similar distractors. Keep the unified Java world/browser/creation mission and source age/uncertainty gates central. No delegation or overlapping heavy studies. Shared node junction read-only. Preserve private owners and frozen editions 001–134. Publish only Fraynix-Agent-Workspace and audit fetched Git; never repeat passed heavy tests without new cause.

'''
if __name__=='__main__':
 s=status('locally verified; publication preparation','No 134 push claimed until actual push/fetched audit pass.')
 for name in ['AGENTS.md','README-CONTRIBUTORS.md','docs/ARCHITECTURE.md','docs/PROJECT_MEMORY.md','docs/DEVELOPMENT_LOG.md']:
  p=r/name;old=p.read_text(encoding='utf-8-sig');p.write_text(s+old if not old.startswith(s) else old,encoding='utf8')
