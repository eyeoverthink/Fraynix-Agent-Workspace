from pathlib import Path
import json
r=Path.cwd()
def status(title,publication='Publication preparation; do not claim pushed until fetched audits pass.'):
 v=json.loads((r/'docs/readmes/119/verification.json').read_text(encoding='utf8'));assert v['passed']
 ms=v['controllerCases'];rr=next(x for x in ms if x['partition']=='reserved' and x['policy']=='recover119');sw=next(x for x in ms if x['partition']=='software-reserved' and x['policy']=='recover119')
 return f'''## Edition119 — {title}

2026-11-11, America/Los_Angeles. Isolated S:/Codex-Work/fraynix-workspace-edition119, parent b154e9ea. Source {v['sourceCommit']}. CI checked first:118 main43095307410 queued, release43095071023 running; final hosted CI remains separate. {publication}

Fresh baseline reproduces seed8 contact stall and seed23 death after pickup decline. Actual displacement and opposing/support contacts reveal the stall despite reported downward velocity. ContactObserver119/ContactRecovery119 use bounded observed retreat and ordinary jump input; no physics, transform, collider or outcome changes. First prototype clears seed8 but lodges at the question-block corner; retained. Final policy adds supported-drop observation and bounded wait for rear threats. At most3 contact attempts; wait180/retreat90/jump90 ticks; jump held up to28. Opportunity deferral is at most3 per object,60 ticks apart; no false failed-motor learning. Backward stomp chase of a passed enemy removed only in recover119. Old explicit policies preserved.

Both retained failures finish with1 pickup and0 damage. Six development courses finish,5 pickups/0damage; one pickup remains uncollected. Old18 challenge courses finish/0damage; old24 courses retain24 finishes/24pickups/0damage. Final repeat of reserved cases: recover119 {rr['wins']}/6, {rr['pickups']} pickups/{rr['damage']} damage/{rr['deaths']} deaths; software repeat {sw['wins']}/6, {sw['damage']} damage. First fixed policy evaluated before default promotion. Later code inspection found a changed optional goal could redirect observations during a committed retreat; direction binding and real native support fixtures correct it. Original reserved results remain separately archived; final hardware/software repetitions and all42 regressions use the correction and are not newly unseen evidence. General AI Play and explicit challenge tasks now use recover119. Old non-challenge task recipes explicitly keep safe117. All evidence and pickup tradeoffs retained. GameEvidence119 strengthens bounded counter/deferral checks while original source/asset/candidate/engine/owner/session/action/image/outcome checks and limits remain.

{len(v['javaChecks'])} distinct Java methods,165 desktop checks, five actual requests, abrupt own-process recovery and visible final default checked. Both corrected tasks, low-clearance refusal and failed challenge retained. Six transformed references/four refusals per renderer preserve original image scope. Zero model calls in the five Java-routed requests, no new training or procedure admission. The first actual review caught history defaulting to restored UUID order; chronological display fixes the latest-task selection without changing controller/evidence. Original attempt retained and complete visible review repeated. Actual screenshots, native chart and offline desktop/mobile guide checked. Preserve private worlds, Java owners33224/37676 and frozen001–119; no delegation, heavy checks serial. Next120: measure waiting/revisiting value under richer enemy motion, restricted retreat and low ceilings; improve collection only when safety and complete cost justify it. Broader image components and procedure lifecycle follow with fresh verification.

'''
if __name__=='__main__':
 s=status('implemented and locally validated; publication preparation')
 for name in ['AGENTS.md','README-CONTRIBUTORS.md','docs/PROJECT_MEMORY.md','docs/DEVELOPMENT_LOG.md','docs/ARCHITECTURE.md']:
  p=r/name;old=p.read_text(encoding='utf-8-sig')
  if not old.startswith('## Edition119 — implemented and locally validated; publication preparation'):p.write_bytes((s+old).encode())
 p=r/'docs/PLAYABLE_LEVEL_CREATION_PLAN.md';old=p.read_text(encoding='utf-8-sig');plan='''## Progress through edition119 — 2026-11-11

Both retained118 failures are corrected through measured native contact/displacement and bounded input-only recovery. Temporary moving-threat deferrals can be reconsidered without false learning; passed enemies are not chased backward for a stomp. Six development finishes, all18 prior challenge finishes and all24 older finishes/pickups preserved. See [edition119](../README-119.md) for fresh reserved hardware/software comparisons, pickup tradeoffs, complete costs and counterexamples.

General AI Play and explicit challenge tasks use the qualified recover119 policy; old non-challenge task recipes retain their explicit safe117 comparison. Source/outcome checks, ownership, limits and old policy names remain. Actual chat reports verified recovery/deferral counters and resumes through a fresh trial. [Contract](EDITION_119_CONTRACT.md).

Next120: measure useful waiting and revisiting under changing enemy motion, multiple obstacles and constrained retreat. Compare survival, collection, failures and full task costs on newly reserved courses. Preserve refusal when no safe retreat is observed. Explicit image-component support and procedure retirement/reactivation follow; no new32-slot procedure admission is claimed. Historical roadmap entries below are planning, not statements that every phase has shipped.

'''
 if not old.startswith('## Progress through edition119'):p.write_bytes((plan+old).encode())
