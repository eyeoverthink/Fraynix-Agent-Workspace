from pathlib import Path
import json
r=Path.cwd()
def status(title,publication='Publication preparation; do not claim pushed until fetched audits pass.'):
 v=json.loads((r/'docs/readmes/121/verification.json').read_text(encoding='utf8'));assert v['passed'];ms=v['controllerCases'];rr=next(x for x in ms if x['partition']=='reserved' and x['policy']=='revisit121');old=next(x for x in ms if x['partition']=='reserved' and x['policy']=='opportunity120');sw=next(x for x in ms if x['partition']=='software-reserved' and x['policy']=='revisit121')
 return f"""## Edition121 — {title}

2026-11-11 America/Los_Angeles. Isolated S:/Codex-Work/fraynix-workspace-edition121, parent6999d1a3; production26cad633. Source {v['sourceCommit']}. CI checked first:119 main43104121657 FAILED Load055 interruption assertion;120 main43115922490/release43115639334 pending. Original log/XML retained. The120 note's43104121687 was a transcription error, corrected here without rewriting frozen history. Load055 interruption test now coordinates at a real durable native checkpoint; retained partial trace, restart and forgery checks remain. {publication}

Native enemy wall reversals observed by identity with bounded64 live records/16 events. Nearby30-tick reversal uncertainty and observed wall obstruction refuse linear wait forecasts. No physics edits. revisit121 and return-disabled observe121 share all other behavior. Extended2.75-unit supported return retains240-tick/three-start/once-object limits and braking/floor/risk gates. First prototype stalled during reversal braking; next bumped block but jumped over collectible. Both original failures retained.121 resets local recovery during opposite-direction braking and excludes collectible bodies from wall jumping. Actual development seed127 now returns, collects and finishes without damage; same-controller return-disabled and120 finish without the item. Return cost is extra, not a speedup.

Eight development:1216/8 finishes,4 pickups,2 deaths/0damage;120 and return-disabled6/8,3 pickups,2 deaths. Late-near-guard/late-turtle deaths remain, and simple control wins one of them. All71 older courses finish0damage; original24 pickups retained. Fixed policy before reserved:121 {rr['wins']}/6 finishes,{rr['pickups']} pickups,{rr['deaths']} deaths/{rr['damage']}damage;120 {old['wins']}/6,{old['pickups']} pickups,{old['deaths']} deaths. Software same cases: {sw['wins']}/6,{sw['pickups']} pickups,{sw['deaths']} deaths/{sw['damage']}damage. The reserved turtle-reversal course stalls under all three AI variants after collecting its item; that item is counted but the course is not certified. No reserved tuning. General AI Play/challenges use revisit121; old nonchallenge safe117; explicit120 and older policies preserved.

{len(v['javaChecks'])} distinct Java methods,168 desktop checks,five actual visible requests with abrupt owned-process recovery; reference6 cases/4 refusals per renderer. GameEvidence121 adds motion/return bounds alongside every source/asset/component/engine/owner/session/action/frame/outcome check. Zero model calls for explicit Java routes; no new model training or skill-bank admission. Actual screenshots/charts/offline guide and original archives retained. Preserve private worlds, prior Java owners33224/37676 and frozen001–121. No delegation; heavy checks serial.

Next122: investigate late-enemy airborne landing/contact timing and the reserved turtle stall using retained failures, new reserved cases and simple control. Then estimate optional return value from repeated complete costs and user task preference. Do not claim general return optimality, general recognition or neural learning from these fixed policies. Read README-121.md, contract and publication receipt before continuing.

"""
if __name__=='__main__':
 s=status('implemented and locally validated; publication preparation')
 for name in ['AGENTS.md','README-CONTRIBUTORS.md','docs/PROJECT_MEMORY.md','docs/DEVELOPMENT_LOG.md','docs/ARCHITECTURE.md']:
  p=r/name;old=p.read_text(encoding='utf-8-sig')
  if not old.startswith(s):p.write_bytes((s+old).encode())
 p=r/'docs/PLAYABLE_LEVEL_CREATION_PLAN.md';old=p.read_text(encoding='utf-8-sig');plan="""## Progress through edition121 — 2026-11-11

[Edition121](../README-121.md) establishes a useful return by actual collision/finish and a return-disabled comparison. One extra development pickup,71 older safe finishes, native wall reversals observed. Two development enemy-contact deaths remain. Reserved hardware/software outcomes and every unsuccessful prototype are retained. [Contract](EDITION_121_CONTRACT.md).

General AI Play and declared challenge tasks use revisit121; old nonchallenge recipes retain safe117. Original physics and verification remain. No model training or new saved-procedure admission.

Next122: late-enemy landing/contact timing, new reserved cases and comparison with simple control. Next123: repeated full-cost measurements and user objective to choose optional returns. Later: expanded image components and verified procedure lifecycle under existing collection limits. Earlier entries below are dated history.

"""
 if not old.startswith('## Progress through edition121'):p.write_bytes((plan+old).encode())
