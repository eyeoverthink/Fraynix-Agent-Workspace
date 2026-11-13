from pathlib import Path
import json
r=Path.cwd()
def status(stage,published=''):
 v=json.loads((r/'docs/readmes/129/verification.json').read_text(encoding='utf8'))
 return f'''## Edition129 — {stage}

2026-11-12. Isolated S:/Codex-Work/fraynix-workspace-edition129; parent4b941ac6. Final production0a514d57; SVG serialization4e9cbb97; explicit opt-in422e6fb5; navigation correction03b6fd53. {published}

Tracking is evaluated against separate sparse ground references. The unchanged scale-aware candidate failed BOTH locked promotion gates; keep legacy-flow as default. First five-window cohort:15/15 accepted each, within1m9→11, mean0.789→0.724m, severe errors0→0, so strict severe-reduction gate failed. Separate confirmation:15/15 accepted each, within1m9→9, mean1.068→1.007m, severe errors1→1, so strict useful-count gate failed. Do not tune on either held cohort or call the experiment promoted. Foreground-occluded development remains unsolved. Frozen appearance baseline loses most subjects. Twenty independent forward-projected camera points check algebra/conventions, not physical accuracy.

Independent forecast coverage fell28/30→16/30 as envelopes narrowed; position error stayed about0.925m. This is NOT improved physical accuracy. Ordinary chat retains adjacent-point forecasting; an explicit maximum forecast radius opts into the experimental policy. Do not promote it or claim calibrated coverage.

Functional change: in that explicit mode, Java selects a causal earlier baseline with the smallest engineering envelope, including acceleration during baseline AND forecast. Chat and measured-world UI retain an explicit maximum forecast radius(default1m), withhold wider predictions without clipping, preserve rejected observations, source/clock/geometry/body evidence, recovery, independent DOM/numeric checks and versioned procedure lifecycle. Experimental tracking is an explicit per-run UI choice; source identity includes nested/supporting tracker classes. Old incompatible evidence requires fresh explicit runs, never relabelling.

{len(v['javaChecks'])} distinct Java methods, {v['desktopChecks']} desktop checks, kernel harnesses, actual agency regression and offline/package checks passed. Actual UI demonstrates incumbent and explicit candidate sources, SAME-task abrupt Java interruption/cold recovery, fresh-input saved-procedure reuse and retained precision budget. Wider final forecast withheld; available one-step forecasts remain within selected budget. That does NOT certify object associations, ground truth, probability coverage or native physical control. No new LLM call, camera recording or training claim. First test bug, routing correction, original SVG rounding audit refusal, driver-only hidden-control timeout and both failed research gates are preserved. Explicit float serialization keeps the strict verifier unchanged; the completed first task is rechecked and its original clock retained during driver continuation. Final visual wording explicitly distinguishes tracker residuals from physical accuracy; complete final-source tasks and review rerun after that change; final evidence is separately source-bound. See README-129 and docs/readmes/129/original-evidence.json. Hosted CI is reported separately; never infer success from local tests or push.

Destination remains one continuing local Java agent with connected browser/world/creation/observation surfaces. Next130: coherent daily use, active mission/source age/uncertainty/withholding/skills visible together, whole-app requests and restart. Future tracking promotion requires fresh independent association/occlusion evidence; smoothing alone is insufficient. Preserve private projects and frozen001–129. No delegation or overlapping heavy/inference studies. Node junction read-only. Publish only to Fraynix-Agent-Workspace. Read integrated roadmap and edition129 contract; do not repeat passed studies without a new concern.

'''
if __name__=='__main__':
 s=status('locally verified; publication preparation','Do not claim pushed until actual push and fetched audit succeed.')
 for name in ['AGENTS.md','README-CONTRIBUTORS.md','docs/PROJECT_MEMORY.md','docs/DEVELOPMENT_LOG.md','docs/ARCHITECTURE.md']:
  p=r/name;prior=p.read_text(encoding='utf-8-sig');p.write_text(s+prior if not prior.startswith(s) else prior,encoding='utf8')
