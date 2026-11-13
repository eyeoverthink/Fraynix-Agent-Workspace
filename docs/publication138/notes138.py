from pathlib import Path
import json
r=Path.cwd()

def status(stage,published=''):
 v=json.loads((r/'docs/readmes/138/verification.json').read_text(encoding="utf8"));c=json.loads((r/'docs/readmes/138/comparison.json').read_text(encoding="utf8"))
 g=c['summary']['drift-guard-138'];b=c['summary']['legacy-flow'];w=g['warnings'];g=g['tracking'];b=b['tracking']
 return f"""## Edition138 — {stage}

2026-11-13. Isolated J:/Codex-Work/fraynix-workspace-edition138; parent5ee114d6. CI checked FIRST,137 hosted run pending at start. Method/scorer freeze22818761, source-only clarification71ae5444, sources/labels57dc95fc, exact original annotation/lock bytes a4e3bb23 all before fresh predictions. Actual tested source/artifacts in docs/readmes/138/verification.json. {published}

Explicit drift-guard-138 checks accepted flow proposals against immutable source color/spatial/foreground/background evidence and competing appearances. Suspect measurements withheld before flow reports loss; unique strong source matches require two consecutive images to recover. Monitor-only control preserves incumbent tracker/filter outputs exactly. Original pixels replay every decision before separate independent outcome scoring. No automatic target admission, weights, person recognition, default/capacity/authority changes or new runtime dependencies. Existing137 verified reference sets retained;138 uses only its original seed.

Four fixed windows from three fresh public OTB recordings,355 later frames per method:332 visible/partial,1 confidently hidden,22 uncertain. Two Skating2 targets share footage; Liquor stride3. External benchmark target/reciprocal skater boxes; source-only AI visibility and10 later bottle distractor samples, NOT human adjudication. New guard{g['correct']}/{g['visible']} correct versus default{b['correct']}/{b['visible']}; wrong overlaps{g['overlapFailures']} vs{b['overlapFailures']}; missing{g['missing']} vs{b['missing']}; labelled-other matches{g['confirmedDistractorMatches']} vs{b['confirmedDistractorMatches']}. Warnings useful{w['usefulWarnings']}, false{w['falseWarnings']}, missed wrong{w['missedWrongProposals']}. Report all costs; no promotion. Physical accuracy and broad complete-occlusion recovery remain unresolved.

{len(v['javaChecks'])} Java checks;{v['desktopChecks']} desktop checks; kernel/agency and isolated offline/package preparation. Actual app12runs, independent host arithmetic, exact monitor equivalence, original-pixel replay, export, stop/resume exact prefix, owned-process crash/cold restart with exact bytes/timestamps and zero observation writes. Changed source refuses audit/export; restoration revalidates. Native world unchanged. Original attempts and source/results retained; hosted CI separate.

{(r/'build/next138.md').read_text(encoding="utf8").strip()}

Unified Java world/browser/creation goals remain central. No agents/overlapping heavy jobs. Shared node junction read-only; preserve private projects and frozen001–138. Publish only Fraynix-Agent-Workspace, fetch/audit. Never relabel old proofs, tune on fresh outcomes, or rerun passed heavy tests without new cause.

"""
if __name__=='__main__':
 s=status('locally verified; publication preparation','No138 publication claimed until actual push and fetched audit pass.')
 for name in ['AGENTS.md','README-CONTRIBUTORS.md','docs/ARCHITECTURE.md','docs/PROJECT_MEMORY.md','docs/DEVELOPMENT_LOG.md']:
  p=r/name;old=p.read_text(encoding='utf-8-sig');p.write_bytes((s+old if not old.startswith(s) else old).encode())
 short="""## Edition138 / 2026-11-13 — drift evidence before tracking loss

The unified Java world/browser/creation mission continues. [Edition138](../README-138.md) adds an explicit original-appearance guard, an unchanged-output monitoring control and complete original-pixel replay before independent outcome scoring. Defaults and all verification remain. No target predictions become anchor memories. See fresh results, missing observations, false warnings and full task costs before judging utility.

"""+(r/'build/next138.md').read_text(encoding="utf8")+'\n\n'
 for name in ['docs/INTEGRATED_AGENT_ROADMAP.md','docs/PLAYABLE_LEVEL_CREATION_PLAN.md']:
  p=r/name;old=p.read_text(encoding='utf-8-sig');p.write_bytes((short+old if not old.startswith(short) else old).encode())
