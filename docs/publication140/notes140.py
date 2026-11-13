from pathlib import Path
import json,subprocess
r=Path.cwd()
def status(stage,published=''):
 v=json.loads((r/'docs/readmes/140/verification.json').read_text());c=json.loads((r/'docs/readmes/140/comparison.json').read_text());b=c['summary']['legacy-flow']['tracking'];candidate=c['summary']['corroborated-guard-140'];g=candidate['tracking'];w=candidate['warnings']
 restores=[x['drift140']['continuity'] for x in c['rows'] if x['policy']=='corroborated-guard-140'];rest={k:sum(x[k] for x in restores) for k in ['restored','correct','incorrect','unknown']}
 return f'''## Edition 140 — {stage}

2026-11-13. Isolated J:/Codex-Work/fraynix-workspace-edition140 from86e0e5a3. CI checked FIRST:139 hosted run43503115840 in progress, documentation passed; no hosted success claimed. Production/scorer freeze5f400a7f, LF/renderer lock completion aa7a3178 before new source access. Actual tested source/build and locked bytes are recorded in docs/readmes/140/verification.json. {published}

Explicit corroborated-guard-140 separates advisory spatial/motion disagreement from three-image corroborated warnings. Existing hard appearance/scale/ambiguity checks remain. Structural-warning continuity restoration requires three consecutive original-source confirmations in at most12 images, robust uninterrupted flow, stable geometry, strong seed parts and a source-rival margin. Loss, recovery jumps, hard contradiction and ambiguity disable this lane for that episode; expiry never releases a box. Strict unique two-image original-source search remains. No automatic appearance admissions, weights, semantic identity, runtime vision dependency, authority/capacity/default change. Monitor-only140 preserves every incumbent output exactly; original pixels replay each decision before independent outcome scoring.

Exposed139 development bike1: default87correct/8wrong;139guard13/0;140guard72/4. This recovers useful observations but retains errors and missing measurements. Synthetic shadowed return restores on three source confirmations; a competing copy or interrupted flow refuses the continuity route. The first abrupt texture-return probe correctly lost continuity; a gradual-return probe exercised the new route. Initial helper compile typo retained. Independent audit checks both correct and deliberately contradicted restoration outcomes without changing replayed decisions.

Initially fixed source names: cat1, cat2, group1, group2. Group2 annotation geometry was ineligible at every prescribed seed; the bounded group3 replacement also failed. Source search stopped under the explicit clarification before pixels/predictions. Three eligible windows,12 actual app comparisons. Per-method scored counts: {g['visible']} visible/partial, {g['hidden']} confirmed absent, {g['uncertain']} uncertain/unscorable. Candidate correct{g['correct']} vsdefault{b['correct']}; wrong{g['overlapFailures']} vs{b['overlapFailures']}; missing{g['missing']} vs{b['missing']}; hidden outputs{g['unsupported']} vs{b['unsupported']}. Confirmed warnings useful{w['usefulWarnings']}, false{w['falseWarnings']}, missed wrong{w['missedWrongProposals']}. Continuity restorations {rest}. No default promotion. External benchmark rectangles plus separately attributed source-only AI visibility review, not independent human adjudication; no independent distractor geometry. Full source review and limitations in docs/publication140/source140.json. No fresh-outcome tuning or cherry-picking.

{len(v['javaChecks'])} Java tests, {v['desktopChecks']} desktop checks, kernel/agency and offline/package preparation. Actual app checks include independent host arithmetic, exact monitor equivalence, original-pixel replay, export, stopped-prefix resume, owned-process crash/cold reopen with identical completed bytes/timestamps and zero observation writes, source-tamper refusal/restoration and unchanged native world. Full times include study overhead; hosted CI is separate. Documentation hashes finalized after notes, preserving the139 publication lesson.

{(r/'build/next140.md').read_text(encoding='utf8').strip()}

Unified Java world/browser/creation goals remain central. No agents or overlapping heavy jobs; shared dependency junction read-only. Preserve private projects and frozen001–140. Publish only Fraynix-Agent-Workspace, fetch/audit. Never relabel incompatible proofs, tune on fresh outcomes or repeat passed heavy tests without a new reason.

'''
if __name__=='__main__':
 s=status('locally verified; publication preparation','No140 publication claimed until push and fetched audit pass.')
 for name in ['AGENTS.md','README-CONTRIBUTORS.md','docs/ARCHITECTURE.md','docs/PROJECT_MEMORY.md','docs/DEVELOPMENT_LOG.md']:
  p=r/name;old=p.read_text(encoding='utf-8-sig');p.write_bytes((s+old if not old.startswith(s) else old).encode())
 short='## Edition140 / 2026-11-13 — corroborated warnings and bounded restoration\n\nThe unified Java world/browser/creation mission continues. [Edition140](../README-140.md) separates advisory disagreement from confirmed warnings and adds bounded original-source restoration, with defaults and every source/outcome check preserved. Fresh results, costs and limitations are retained.\n\n'+(r/'build/next140.md').read_text(encoding='utf8')+'\n\n'
 for name in ['docs/INTEGRATED_AGENT_ROADMAP.md','docs/PLAYABLE_LEVEL_CREATION_PLAN.md']:
  p=r/name;old=p.read_text(encoding='utf-8-sig');p.write_bytes((short+old if not old.startswith(short) else old).encode())
 subprocess.run(['python','docs/publication140/finalize-inventory140.py'],check=True)
