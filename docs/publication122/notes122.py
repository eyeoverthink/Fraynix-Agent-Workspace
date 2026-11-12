from pathlib import Path
import json
r=Path.cwd()
def status(title,publication='Publication preparation; do not claim pushed until fetched audits pass.'):
 v=json.loads((r/'docs/readmes/122/verification.json').read_text(encoding='utf-8'));assert v['passed'];m=v['controllerCases'];rr=next(x for x in m if x['partition']=='reserved' and x['policy']=='contact122');pr=next(x for x in m if x['partition']=='reserved' and x['policy']=='revisit121');sw=next(x for x in m if x['partition']=='software-reserved' and x['policy']=='contact122')
 return f"""## Edition122 — {title}

2026-11-12 America/Los_Angeles. Isolated S:/Codex-Work/fraynix-workspace-edition122; parent cdc5bdcb. Source {v['sourceCommit']}. CI first:121 main43137995452 and release43137761304 both SUCCESS. {publication}

Retained seeds109/113 descending enemy-contact deaths and157 supported-edge stall reproduced under121, then finished without damage undercontact122. All eight development cases finish. EnemyLanding122 evaluates up to three ordinary horizontal choices for60 ticks, current terrain and up to64 observed enemies. It only overrides toward predicted supported contact-free landing; old descending stomp alignment is not used by122. Actual collision/finish remains authoritative. No physics or transform writes. Supported stationary contact with missing grounded probe admits the existing bounded retreat, with ground-behind and original three-attempt/deadline limits. Grounded flags untouched. Original policies remain explicit; general AI Play/challenge tasks use contact122 after gates; old nonchallenge safe117.

All85 older course executions finish0damage:71 previous regressions plus14 edition121 courses. Original24 pickups and all three earlier reserved return benefits remain. Prior reserved scenes are now regressions. Fresh eight declared before tuning:122 {rr['wins']}/8 finishes,{rr['pickups']} pickups,{rr['damage']} damage,{rr['deaths']} deaths;121 {pr['wins']}/8,{pr['pickups']} pickups,{pr['damage']} damage,{pr['deaths']} deaths. Software repeats: {sw['wins']}/8,{sw['pickups']} pickups,{sw['damage']} damage,{sw['deaths']} deaths. No reserved controller tuning. Initial reserved inputs exceeded the existing 72-tile bound or placed a turtle over a generated gap; both rejected attempts are retained. Fixture-only corrections admitted the original intents; repeated first cases are not additional unseen evidence. Retain every result and do not infer general safety from this set.

{len(v['javaChecks'])} distinct Java methods,168 desktop checks,seven actual requests with abrupt own-process restart and actual failed-challenge refusal. Six transformed image cases/four refusals per renderer plus original reference regression. GameEvidence122 adds bounded forecast/edge checks to all prior source/asset/component/engine/owner/session/action/frame/outcome gates. Old proofs stay engine-bound. Zero model calls in explicit Java routes; no new weight training or skill-bank admission. Actual screenshots/charts/offline guide and raw/hash archives retained. Private worlds and frozen001-122 preserved; no delegation/heavy overlap.

Next123: repeated complete cost and optional-goal value, preserve return-disabled/simple comparisons and negative cases. Next124: richer contact uncertainty with new reserved reversals, moving shells and changing support. Forecasts approximate short current-velocity enemy motion; do not claim full future interaction simulation. Read README-122.md, contract and publication receipt before continuing.

"""
if __name__=='__main__':
 s=status('implemented and locally validated; publication preparation')
 for name in ['AGENTS.md','README-CONTRIBUTORS.md','docs/PROJECT_MEMORY.md','docs/DEVELOPMENT_LOG.md','docs/ARCHITECTURE.md']:
  p=r/name;old=p.read_text(encoding='utf-8-sig')
  if not old.startswith(s):p.write_text(s+old,encoding='utf-8')
 p=r/'docs/PLAYABLE_LEVEL_CREATION_PLAN.md';s=p.read_text(encoding='utf-8-sig');entry="""## Progress through edition122 — 2026-11-12

[Edition122](../README-122.md) fixes the retained descending-contact deaths and supported-edge stall through ordinary input.85 older course executions finish without damage; original24 pickups and three previous return benefits remain. Fresh reserved outcomes and every comparison are retained in the edition. [Contract](EDITION_122_CONTRACT.md).

General AI Play/challenges use contact122; old nonchallenge safe117 and explicit historical policies remain. No new model training or skill admission. Next123: repeated full costs and optional-goal value. Next124: richer observed contact uncertainty on new declared cases. Then broader image components and verified procedure lifecycle. Earlier entries below remain dated history.

"""
 if not s.startswith('## Progress through edition122'):p.write_text(entry+s,encoding='utf-8')
