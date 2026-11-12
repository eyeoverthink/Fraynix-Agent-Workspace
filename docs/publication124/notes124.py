from pathlib import Path
import json
r=Path.cwd()
def status(title,publication='Publication preparation; do not claim pushed until fetched audits pass.'):
 v=json.loads((r/'docs/readmes/124/verification.json').read_text(encoding='utf8'));assert v['passed']
 outcomes=' '.join(f"{x['partition']}/{x['case']} allowance{x['allowance']:g}: {x['selected']}, pickups{x['pickups']}, first verified{x['seconds']:.2f}s." for x in v['measurements']['rows'] if x['allowance']==30)
 return f'''## Edition124 — {title}

2026-11-12 America/Los_Angeles. Isolated S:/Codex-Work/fraynix-workspace-edition124; parent1d328f37. Source {v['sourceCommit']}. CI checked first:123 main43231743693/release43231218250 and122 still running; no hosted success claimed. {publication}

Opt-in pickupStrategy=budget plus explicit pickupBudgetSeconds=0..120 means estimated extra elapsed request seconds per additional pickup, not a total task deadline. Default full/manual modes unchanged. GameChoices124 wraps unchanged GameTask115; contact122 controls and all source/asset/component/engine/owner/session/action/frame/outcome gates retained. Require3 current safe direct+3 alternative observations, exact geometry/seed and context/renderer; seven-day freshness. Retain all matching failures. Nonpositive minimum pickup benefit, timing ratio>1.5 or missing evidence abstains. Cost counts worst observed extra time, twice observed range, measured admission and completion-check reserve, plus0.25s recording reserve. Empirical engineering bound, not probability or guarantee.

Clock admission to first verified terminal outcome; final timing-write/later audits/shutdown excluded and external re-audit costs separate. Restarted timings and budget-chosen runs cannot calibrate. Legacy repeated start cannot invent a clock. Source-linked decisions reverified on resume/completion; changed renderer blocks before learned AI actions.24 game-task limit unchanged. Checked local records are not administrator-proof attestation.

40 final native workflows (27dev,4reserved,9software), plus earlier development/reserved passes retained separately; admission interruption cannot execute without its decision receipt; legacy failed-recipe message preserved and final development remeasured; second completion-check reserve added before reserved tests and fresh calibration. {outcomes} No reserved tuning. {len(v['javaChecks'])} distinct Java methods,168 desktop checks,18 prior courses finish without damage, hardware references6/four refusals plus original reference tests.11 actual chat requests, own-process crash/recovery and impossible-course refusal;0 model calls. Actual screenshots/chart/offline guide, originals and source inventory retained. Private worlds/frozen001–123 preserved; no delegation or heavy-test overlap.

No general cross-scene transfer, hard deadline, neural training or skill admission. Next125: evaluate predicted vs actual extra cost, late checks/recovery and useful opportunities missed by abstention. Then126: moving shells, reversals and changing supports. Read README-124, acceptance contract and publication receipt. Never replace negative outcomes with a success narrative.

'''
if __name__=='__main__':
 s=status('implemented and locally validated; publication preparation')
 for name in ['AGENTS.md','README-CONTRIBUTORS.md','docs/PROJECT_MEMORY.md','docs/DEVELOPMENT_LOG.md','docs/ARCHITECTURE.md']:
  p=r/name;old=p.read_text(encoding='utf-8-sig')
  if not old.startswith(s):p.write_text(s+old,encoding='utf8')
 p=r/'docs/PLAYABLE_LEVEL_CREATION_PLAN.md';old=p.read_text(encoding='utf-8-sig');entry='''## Progress through edition124 — 2026-11-12

[Edition124](../README-124.md) adds opt-in user allowance and conservative benefit/cost decisions around the unchanged native task verifier. Repeated fresh explicit calibration; no-benefit and unknown-scene abstention; original and recovered evidence stays authoritative. [Contract](EDITION_124_CONTRACT.md).

Next125: measure prediction error and missed useful opportunities, including late verification and recovery costs, before broadening reuse. Next126: moving shells, reversals and changing supports. Then expand image components and reusable verified procedure lifecycles. Earlier entries are dated history.

'''
 if not old.startswith('## Progress through edition124'):p.write_text(entry+old,encoding='utf8')
