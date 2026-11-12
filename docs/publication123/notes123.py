from pathlib import Path
import json
r=Path.cwd()
def status(title,publication='Publication preparation; do not claim pushed until fetched audits pass.'):
 v=json.loads((r/'docs/readmes/123/verification.json').read_text(encoding='utf8'));assert v['passed']
 cases=v['measurements']['comparisons'];costs=[x for x in cases if x['enabled']=='full' and ((x['case'].endswith('return') and x['comparison']=='no-return') or (x['case'].endswith('wait') and x['comparison']=='no-wait'))]
 summary=' '.join(f"{x['partition']}/{x['case']}: threshold {x.get('observedConservativeSecondsPerExtraPickup','unestablished')} seconds per extra pickup." for x in costs)
 return f"""## Edition123 — {title}

2026-11-12 America/Los_Angeles. Isolated S:/Codex-Work/fraynix-workspace-edition123; parent98d0724b. Source {v['sourceCommit']}. CI checked first:122 main43200530305/release43200200568 running, previous121 completed successfully. {publication}

Same contact122 controller, explicit full/no-wait/no-return/direct pickup choices in durable chat and integrated Game Lab. Defaults unchanged. No physics or body writes. All source/asset/component/engine/owner/session/action/frame/outcome checks retained; new checks bind request strategy and prove disabled actions stayed disabled. Failed traces may be verified for measurement with completionCertified=false; ordinary completion verification still refuses them. Existing history limit24 unchanged. Self-model capability description corrected from stale120 to current123 choices.

72 complete repeated workflows:36 development,24 two predeclared fresh scenes,12 software-rendering repeats. Three repetitions/four approaches; rotated order occupies three of four positions, not complete counterbalancing. Every task starts fresh, includes real simple comparison, construction, native trial, saves, source/outcome verification and shutdown. Simulation ticks and workNanos are not full cost. UI costs and report writing separate. Pilot excluded but preserved. No reserved tuning. {summary}

{len(v['javaChecks'])} distinct Java methods,168 desktop checks,18 prior course executions finish without damage. Six transformed reference images/four refusals per renderer plus original reference regression. Six actual app requests, owned-process crash/recovery, impossible-course refusal,0 model calls. Actual screenshots/chart/offline guide and exact original records retained. Private worlds/frozen001-122 preserved; no delegation or overlapping heavy checks.

No statistical confidence claim from three runs, automatic value policy, neural learning, user utility assignment or skill admission. Next124: explicit user time allowances, uncertainty-aware cost estimates and abstention on new scenes; include estimation cost before changing defaults. Next125: richer contact uncertainty. Read README-123, contract and publication receipt. Preserve all measured negatives.

"""
if __name__=='__main__':
 s=status('implemented and locally validated; publication preparation')
 for name in ['AGENTS.md','README-CONTRIBUTORS.md','docs/PROJECT_MEMORY.md','docs/DEVELOPMENT_LOG.md','docs/ARCHITECTURE.md']:
  p=r/name;old=p.read_text(encoding='utf-8-sig')
  if not old.startswith(s):p.write_text(s+old,encoding='utf8')
 p=r/'docs/PLAYABLE_LEVEL_CREATION_PLAN.md';old=p.read_text(encoding='utf-8-sig');entry='''## Progress through edition123 — 2026-11-12

[Edition123](../README-123.md) measures complete workflow costs of optional waiting and returns with the same contact122 controller. Four explicit approaches;72 repeated fresh-project workflows, retained simple comparisons and failed-task measurement refusal. No default promotion, model training or automatic utility assignment. [Contract](EDITION_123_CONTRACT.md).

Next124: explicit user time allowances and uncertainty-aware estimates, measured estimation cost and abstention on new cases. Next125: moving shells, reversals and changing supports. Then broader image components and verified procedure lifecycle. Earlier entries remain dated history.

'''
 if not old.startswith('## Progress through edition123'):p.write_text(entry+old,encoding='utf8')
