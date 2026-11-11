from pathlib import Path
import json
r=Path.cwd()
def status(title,publication='Publication preparation; do not claim pushed until fetched audits pass.'):
 v=json.loads((r/'docs/readmes/120/verification.json').read_text(encoding='utf8'));assert v['passed'];ms=v['controllerCases'];rr=next(x for x in ms if x['partition']=='reserved' and x['policy']=='opportunity120');sw=next(x for x in ms if x['partition']=='software-reserved' and x['policy']=='opportunity120')
 return f'''## Edition120 — {title}

2026-11-11, America/Los_Angeles. Isolated S:/Codex-Work/fraynix-workspace-edition120, parent483acb44. Source {v['sourceCommit']}. CI checked first:119 main43104121687/release43103791637 pending; hosted results remain separate. {publication}

Two supported119 reserved pickups are now collected through bounded waiting and actual clearance; the unsupported gap pickup stays refused. Seven final development finishes,3 pickups/0damage versus recover1197 finishes/0pickups/0damage. All54 older courses finish without damage, including24 original pickup successes. Fixed controller before six reserved courses: {rr['wins']}/6 finishes, {rr['pickups']} pickups/{rr['damage']} damage/{rr['deaths']} deaths; software repeat {sw['wins']}/6, {sw['pickups']} pickups/{sw['damage']} damage. No reserved tuning. General AI Play and declared challenges use opportunity120; old non-challenge tasks keep safe117; explicit historical policies retained.

OpportunityTiming120 estimates bounded enemy clearance, never success. PickupOpportunity120 holds normal input only with observed support and fresh safety checks,180 ticks/wait,360 total,3 starts and once/object. Nearby return admission checks continuous ground and forward braking room,1.75-unit reach,240 ticks/return and3 starts. Permanent refusal remains authoritative after prior waiting. Useful return collection is UNPROVEN: original early probe spent28 return ticks without collecting; braking support refuses it. Four extra development probes finish/0damage/0pickups. Failed fixture, first prototype, original unhelpful return and all comparisons retained. GameEvidence120 validates counters/decisions/deadlines alongside original source/asset/candidate/engine/owner/session/action/image/outcome checks. Task engine identity also binds evidence helpers. Physics unchanged.

{len(v['javaChecks'])} distinct Java methods,168 desktop checks,five actual visible requests,abrupt owned-process recovery,actual waiting/collection and final default verified. Six transformed reference cases/four refusals per renderer preserve original scope. Zero model calls in explicit Java task routes; no new model training or procedure admission. First five-request review passed functionally but its waiting screenshot showed an older display frame. Original attempt retained; capture waits for display frames. Second review exposed an ownership-denied message during the resume/open race; UI now displays waiting without retrying mutation, and handles a closed frame between trials. Unexpected errors still propagate; three focused UI tests added. Final five-request review repeats with this display correction and unchanged Java control/verification. Actual screenshots, motion chart and offline guide inspected. Preserve private worlds, Java owners33224/37676 and frozen001–120. No delegation; heavy checks serial.

Next121: measure observation value under enemy reversals, moving pickups and constrained platforms; construct genuinely useful return opportunities with a fresh reserved set, compare actual collection/survival/full costs, and keep permanent structural refusals. Broader image components and procedure lifecycle remain later work. Read README-120.md, contract and publication receipt before continuing; do not infer general learning from these fixed policies.

'''
if __name__=='__main__':
 s=status('implemented and locally validated; publication preparation')
 for name in ['AGENTS.md','README-CONTRIBUTORS.md','docs/PROJECT_MEMORY.md','docs/DEVELOPMENT_LOG.md','docs/ARCHITECTURE.md']:
  p=r/name;old=p.read_text(encoding='utf-8-sig')
  if not old.startswith(s):p.write_bytes((s+old).encode())
 p=r/'docs/PLAYABLE_LEVEL_CREATION_PLAN.md';old=p.read_text(encoding='utf-8-sig');plan='''## Progress through edition120 — 2026-11-11

Two retained supported pickups are collected after bounded waiting and actual clearance. The unsupported gap stays refused. Seven development finishes/three pickups/zero damage and all54 older finishes preserved. [Edition120](../README-120.md) records six reserved hardware/software outcomes, complete costs, original unsuccessful returns and actual chat/restart evidence. [Contract](EDITION_120_CONTRACT.md).

General AI Play and declared challenge tasks use opportunity120; old non-challenge tasks keep safe117. Nearby returns are implemented with continuous support and braking checks but useful collection remains unproven. No original physics or verification gate is removed. No neural training or new saved-procedure admission is claimed.

Next121: enemy reversals, moving pickups, constrained platforms and useful return opportunities. Compare observation timing, safe finishes, optional collection and complete work on a newly reserved set. Then expand declared image components and procedure retirement/reactivation under the unchanged32 active slots. Historical roadmap entries below remain dated plans.

'''
 if not old.startswith('## Progress through edition120'):p.write_bytes((plan+old).encode())
