from pathlib import Path
import json,subprocess
r=Path.cwd()
def status(stage,published=''):
 v=json.loads((r/'docs/readmes/141/verification.json').read_text(encoding='utf8'));c=json.loads((r/'docs/readmes/141/comparison.json').read_text(encoding='utf8'));b=c['summary']['legacy-flow']['tracking'];old=c['summary']['foreground-guard-139']['tracking'];candidate=c['summary']['recovery-guard-141'];g=candidate['tracking'];a=candidate['additional'];f=candidate['filtered']
 return f'''## Edition141 — {stage}

2026-11-13. Isolated J:/Codex-Work/fraynix-workspace-edition141 fromd27846b2. CI checked FIRST:140 run43518090047 documentation passed, engine tests pending;139 hosted runs successful. No unobserved hosted success claimed. Method/scorer/harness/source-selection freeze05277fcb before fresh annotations or pixels. Actual tested source/build and locked bytes in docs/readmes/141/verification.json. {published}

Explicit recovery-guard-141 preserves unchanged139 accepted tracker proposals. Supplemental recovery only follows actual underlying flow loss, not warning-only quarantine. Immutable multi-scale source search retains3 peaks per scale, refines locations/sizes and rejects competitors. Whole appearance/color, original spatial parts and3 adjacent coherent-motion confirmations qualify additional boxes. Disappearance, ambiguity or discontinuity resets confirmation. No baseline resets, automatic appearance admissions, weights, semantic identity, runtime vision dependency, authority/capacity/default changes. Recovery-monitor-141 preserves139 tracker/filter outputs; added measurements can affect later filter estimates. Original pixels replay every decision; extra boxes and filter-accepted outputs scored separately.

Exposed140 development:139 and141 both83correct/10wrong/69missing,0hidden outputs; no additional recoveries. All93 baseline accepted tracker proposals preserved. Synthetic displaced/enlarged return adds correct source/motion-confirmed measurements; differently scaled copy and broken confirmation refuse extra outputs. Earlier139 can itself accept a copy in the control and that inherited limitation stays explicit. Search failures and fixture canonical-seed replay failure retained, corrected before freeze. Nine focused checks pass, including positive/contradicted independent outcomes and durable source-tamper refusal.

Fresh fixed names car3,following,person2; exclusions and source-only review retained without replacement search. {len(c['sources']['records'])} eligible windows, {len(c['rows'])} actual-app comparisons. Per-method references: {g['visible']} visible/partial, {g['hidden']} confirmed absent, {g['uncertain']} uncertain. Candidate correct{g['correct']}/wrong{g['overlapFailures']}/missing{g['missing']}/hidden{g['unsupported']};139 correct{old['correct']}/wrong{old['overlapFailures']}/missing{old['missing']}/hidden{old['unsupported']};default correct{b['correct']}/wrong{b['overlapFailures']}/missing{b['missing']}/hidden{b['unsupported']}. Additional returned boxes {a}; filter accepted {f}. No default promotion. External benchmark geometry plus separately attributed source-only AI visibility; no independent human adjudication or distractor geometry. No fresh-outcome tuning.

{len(v['javaChecks'])} Java tests, {v['desktopChecks']} desktop checks, kernel/agency and offline/package checks passed. Actual app includes independent host arithmetic, monitor equivalence, baseline accepted-proposal preservation, pixel replay, export, stopped-prefix resume, owned-process crash/cold reopen with exact completed bytes/timestamps and zero observation writes, tamper refusal/restoration and unchanged native world. Full elapsed workflow costs retained. Documentation inventory finalized after notes.

{(r/'build/next141.md').read_text(encoding='utf8').strip()}

Unified Java world/browser/creation goals remain central. No agents or overlapping heavy jobs. Shared dependency junction read-only. Preserve private projects and frozen001–141. Publish only Fraynix-Agent-Workspace and fetch/audit. Never relabel incompatible proofs or tune on fresh outcomes.

'''
if __name__=='__main__':
 s=status('locally verified; publication preparation','No141 publication claimed until push and fetched audit pass.')
 for name in ['AGENTS.md','README-CONTRIBUTORS.md','docs/ARCHITECTURE.md','docs/PROJECT_MEMORY.md','docs/DEVELOPMENT_LOG.md']:
  p=r/name;old=p.read_text(encoding='utf-8-sig');p.write_bytes((s+old if not old.startswith(s) else old).encode())
 short='## Edition141 / 2026-11-13 — source-qualified recovery after loss\n\nThe unified Java world/browser/creation mission continues. [Edition141](../README-141.md) adds explicit recovery after genuine flow loss while retaining baseline139 proposals, defaults and all checks. Fresh outcomes and costs remain separate from synthetic mechanism tests.\n\n'+(r/'build/next141.md').read_text(encoding='utf8')+'\n\n'
 for name in ['docs/INTEGRATED_AGENT_ROADMAP.md','docs/PLAYABLE_LEVEL_CREATION_PLAN.md']:
  p=r/name;old=p.read_text(encoding='utf-8-sig');p.write_bytes((short+old if not old.startswith(short) else old).encode())
 subprocess.run(['python','docs/publication141/finalize-inventory141.py'],check=True)
