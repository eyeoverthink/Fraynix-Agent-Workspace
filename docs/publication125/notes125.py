from pathlib import Path
import json
r=Path.cwd()

def status(title,publication='Publication preparation; do not claim pushed until fetched audits pass.'):
 v=json.loads((r/'docs/readmes/125/verification.json').read_text(encoding='utf8'));assert v['passed']
 outcomes=' '.join(f"{x['partition']}/{x['case']}: {x['metrics']['missedOpportunity']}."+((' '+x['metrics']['reason']+'.') if not x['metrics']['established'] else '') for x in v['measurements']['rows'])
 return f'''## Edition 125 — {title}

2026-11-12 America/Los_Angeles. Isolated S:/Codex-Work/fraynix-workspace-edition125; parent 63db5798. Packaged source {v['sourceCommit']}. CI checked first: edition 124 main/release and edition 123 main/release were pending; hosted success is separate from local verification. {publication}

GameCosts125 wraps unchanged GameChoices124/GameTask115 and contact122. Java records admission, execution, measurement, recovery and verification work. First verified terminal clock never resets; later checks stay separate. Same-boot monotonic time is comparable only without interruption. Across-restart wall spans and unknown pending work remain descriptive, never zero or calibrated timing. Legacy tasks have no invented clock. Existing source, asset, component, engine, owner, session, action and outcome checks remain authoritative. Limits remain 24 game tasks and 256 retained reviews. Checked local records are not administrator-proof attestation.

Independent review requires three later explicit direct and full trials with exact conditions, excluding original calibration IDs and recovered/automatic timings. Matched invalid evidence or failed/damaged outcomes prevents a favorable certification. Original budget failure is not completion. Empirical benefit/time ranges plus original decision-and-run excess are not statistical confidence or causal overhead. Reports bind original snapshots, complete cohort and cutoff; later trials do not rewrite history. Reopening rechecks sources. Altered metrics, omission with recomputed metrics and changed cost evidence are refused.

Final dataset: 53 actual workflows (26 development, 14 reserved, 13 software), separate from the retained 13-workflow earlier-source preflight. {outcomes} No reserved tuning. {len(v['javaChecks'])} distinct Java methods across retained invocations, 168 desktop checks, 18 prior courses finishing without damage, six transformed hardware references/four refusals plus original reference regression. Fifteen actual app requests, deliberate owned-process crash/recovery, saved-review reopening and failed-course refusal. Zero model calls for these explicit Java routes. Actual screenshots/chart/interactive guide, raw original records and full source inventory retained. Preserve private projects and frozen editions 001–124. No delegation or heavy-test overlap.

The destination remains one local Java agent with connected browser, creation, physics and observation surfaces. Next 126: real local-model selection across supported world/browser/creation tasks, wrong-tool/refusal cases, direct baseline and complete costs. Then 127 procedure lifecycle; 128 observation-to-artifact mission; 129 evaluated candidate adaptation; 130 coherent daily use. These are planned, not shipped. The integrated roadmap supersedes older contact-only next-phase proposals. No general cross-scene transfer, automatic policy promotion, neural training or new skill admission is claimed by 125. Read README-125.md, docs/EDITION_125_CONTRACT.md and docs/INTEGRATED_AGENT_ROADMAP.md. Preserve negative outcomes.

'''

if __name__=='__main__':
 s=status('implemented and locally validated; publication preparation')
 for name in ['AGENTS.md','README-CONTRIBUTORS.md','docs/PROJECT_MEMORY.md','docs/DEVELOPMENT_LOG.md','docs/ARCHITECTURE.md']:
  p=r/name;old=p.read_text(encoding='utf-8-sig')
  if not old.startswith(s):p.write_text(s+old,encoding='utf8')
 p=r/'docs/PLAYABLE_LEVEL_CREATION_PLAN.md';old=p.read_text(encoding='utf-8-sig');entry='''## Progress through edition 125 — 2026-11-12

[Edition 125](../README-125.md) records complete task costs and independently reviews optional-action decisions using later explicit trials. Recovery uncertainty, original calibration exclusion and complete cohort/source checks remain visible. The controller, selector and original native verification are unchanged. [Contract](EDITION_125_CONTRACT.md).

The next priority returns to the entire agent: 126 real local-model world/browser/creation selection; 127 procedure lifecycle; 128 calibrated observation-to-artifact mission; 129 evaluated adaptation; 130 coherent daily use. [Integrated roadmap](INTEGRATED_AGENT_ROADMAP.md) defines acceptance gates. Earlier contact-only proposals below remain dated history, not the current direction.

'''
 if not old.startswith('## Progress through edition 125'):p.write_text(entry+old,encoding='utf8')
