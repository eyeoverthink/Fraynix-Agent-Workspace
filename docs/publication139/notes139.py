from pathlib import Path
import json
r=Path.cwd()
def status(stage,published=''):
 v=json.loads((r/'docs/readmes/139/verification.json').read_text(encoding='utf8'));c=json.loads((r/'docs/readmes/139/comparison.json').read_text(encoding='utf8'));g=c['summary']['foreground-guard-139'];b=c['summary']['legacy-flow']['tracking'];w=g['warnings'];g=g['tracking']
 return f'''## Edition139 — {stage}

2026-11-13. Isolated J:/Codex-Work/fraynix-workspace-edition139 from7e66b0c8. CI checked FIRST;138 hosted run pending, documentation subsequently passed while full compile/tests continued. Method/scorer frozen e7bd0ff7 before fresh pixels/annotations. Source-only cohort clarifications512fe0e1/0c7c40ff; sources/labels d84ccd12; strict-format/small-box and exact-rational normalization cc1d50a8/20db8d29 before predictions. Actual tested source/build in docs/readmes/139/verification.json. {published}

Optional foreground-guard-139 adds immutable textured seed parts, spatial consensus and relative foreground/background motion to138 appearance checks. Two structural disagreements may withhold before flow loss. Unique original-source recovery remains two-image confirmed and spatially supported. Monitor-only139 preserves every incumbent output exactly. Original pixels replay every decision before independent scoring. No automatic appearance admission, model weights, semantic identity, default/capacity/authority change or new runtime dependency.

Five fresh VOT-LT2018 source windows: ballet/bicycle/bird1 exits and returns, bike1 continuous-motion control, car1 central-structure cover/return.362 source images,357 later samples per method;201 scored visible/partial,85 confirmed absent (4 obstruction,81 outside view),71 uncertain/unscorable.14 visible external boxes below existing minimum remain unscored; exact rational normalization corrects only floating representation at the1/40 boundary. External rectangles/NaN labels plus separate AI source review, not independent human adjudication; no distractor geometry. Candidate {g['correct']}/{g['visible']} correct vsdefault{b['correct']}/{b['visible']}; wrong{g['overlapFailures']} vs{b['overlapFailures']}; missing{g['missing']} vs{b['missing']}; hidden outputs{g['unsupported']} vs{b['unsupported']}. Warnings useful{w['usefulWarnings']}, false{w['falseWarnings']}, missed wrong{w['missedWrongProposals']}. No promotion; report all losses and costs. Uncertain boundaries break the frozen recovery-episode scorer and cannot support invented delay claims.

Exposed138 development: correct122→72; wrong132→1; missing78→259; hidden1→0. The false withholding cost is material. Synthetic initial attempts failed due resampling aliasing and recovery search selecting a smaller wrong candidate; corrected before source freeze, with every attempt preserved.

{len(v['javaChecks'])} Java tests;{v['desktopChecks']} desktop checks; kernel/agency/offline/package validation. Actual app20runs, independent host arithmetic, exact monitor equivalence, pixel replay, full export, stopped-prefix resume, owned-process crash/cold restart with identical completed bytes/timestamps and zero observation writes. Source tampering refuses audit/export; restoration revalidates. Native world unchanged. Hosted CI separate from local tests and fetched publication.

{(r/'build/next139.md').read_text(encoding='utf8').strip()}

Unified Java world/browser/creation goals remain central. No agents or overlapping heavy jobs. Shared node junction read-only. Preserve private projects and frozen001–139. Publish only Fraynix-Agent-Workspace and fetch/audit. Never tune on fresh outcomes, relabel incompatible historical proofs or rerun passed expensive tests without new cause.

'''
if __name__=='__main__':
 s=status('locally verified; publication preparation','No139 publication claimed until push and fetched audit complete.')
 for name in ['AGENTS.md','README-CONTRIBUTORS.md','docs/ARCHITECTURE.md','docs/PROJECT_MEMORY.md','docs/DEVELOPMENT_LOG.md']:
  p=r/name;old=p.read_text(encoding='utf-8-sig');p.write_text(s+old if not old.startswith(s) else old,encoding='utf8')
 short='## Edition139 / 2026-11-13 — foreground motion and spatial structure\n\nThe unified Java world/browser/creation mission continues. [Edition139](../README-139.md) adds explicit immutable seed-part evidence and relative-motion warnings, retaining baseline behavior and every source/outcome check. Fresh camera disappearance/return results include withheld correct observations, uncertainty and full costs. No default promotion.\n\n'+(r/'build/next139.md').read_text(encoding='utf8')+'\n\n'
 for name in ['docs/INTEGRATED_AGENT_ROADMAP.md','docs/PLAYABLE_LEVEL_CREATION_PLAN.md']:
  p=r/name;old=p.read_text(encoding='utf-8-sig');p.write_text(short+old if not old.startswith(short) else old,encoding='utf8')
