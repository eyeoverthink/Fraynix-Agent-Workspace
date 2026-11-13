from pathlib import Path
import json
r=Path.cwd()
def status(stage,published=''):
 v=json.loads((r/'docs/readmes/137/verification.json').read_text());c=json.loads((r/'docs/readmes/137/comparison.json').read_text());cost=json.loads((r/'docs/readmes/137/cost-comparison.json').read_text())['rows'][-1]
 bank=c['summary']['verified-memory-137'];control=c['summary']['seed-only-137'];base=c['summary']['legacy-flow'];retained=c['retained']
 return f'''## Edition137 — {stage}

2026-11-13. Isolated D:/Codex-Work/fraynix-workspace-edition137; parent72a3b969. CI checked FIRST, documentation passed and engine job pending at start. Method/scorer6764ffb8, incumbent archival comparisonc65d3640, frozen docs/OBSERVATION_137_LOCK.json before camera evaluation. Actual tested source/artifacts in docs/readmes/137/verification.json. {published}

Explicit verified-memory-137 resolves an immutable135 reference set through original source pixels, independent fully-visible reference boxes, proposal/audit/code and dataset checks. Chronologically spread at most6 examples; full source evidence preserved. Reject same dataset or any shared source-image hash, multiple objects, missing declaration, changed sources/code and recursive references. Operator same-target declaration is not semantic identity proof. Frozen target run cannot admit its own predictions. Existing incumbent accepted measurements intact; after loss use unique strong match and two consecutive images. Seed-only137 exact search ablation. No default promotion, weights, person recognition or native actuation.

Four current-code earlier135 runs retain {sum(x['examples'] for x in retained)} independently supported examples, {sum(x['examples'] for x in retained if not x['case'].startswith('bird1'))} camera. Later136 windows: candidate{bank['tracking']['correct']}/{bank['tracking']['visible']} correct, seed-only{control['tracking']['correct']}/{control['tracking']['visible']}, incumbent{base['tracking']['correct']}/{base['tracking']['visible']}; imported recoveries{bank['importedRecoveries']}. Automatic target admissions0 by construction. Previously exposed recordings, not unseen footage; three camera windows plus rendered Bird1. Source-only AI visibility/external boxes, no new human-adjudicated complete occlusion or distractor cohort. Retention works; general physical accuracy remains unresolved.

Completed observation startup validates/hashes but performs no write. Running tasks marked interrupted once. Recursive copy only of parsed JSON trees, full postmutation validation/canonical checksum/serialized reparse and forced atomic move retained before cache publication. Same24MB/16datasets/24runs/2048revisions limits. All source/outcome checks remain. Controlled identical1/3/6 histories:36read hashes and9write/persisted pairs equal. Six-run cold open{cost['coldOpenMs'][0]:.1f}→{cost['coldOpenMs'][1]:.1f}ms; median transaction write{cost['writeMedianMs'][0]:.1f}→{cost['writeMedianMs'][1]:.1f}ms. Some reads regress. One cold sample/three repeats; no general speed or realtime-camera claim.

{len(v['javaChecks'])} Java checks; {v['desktopChecks']} desktop checks; kernel/agency integration and offline package preparation. Actual app16runs/1520samples, all380 incumbent outputs match published136 originals, separate host overlap arithmetic, reference UI, bounded exports, stop/resume exact prefix, owned-process crash and cold reopen with exact bytes/timestamp and zero observation writes. Source modification refuses audit/export; original restoration revalidates. Original attempts retained; hosted CI separate.

Next: fresh independently adjudicated camera complete occlusion/return and similar distractors; stronger foreground evidence and measured recovery gain; profile full tasks and immutable per-frame evidence to reduce repeated writes without removing checks. Unified Java world/browser/creation goals remain central. No agents/overlapping heavy work; shared node junction read-only; private projects and frozen001–137 preserved. Publish only Fraynix-Agent-Workspace, fetch and audit. Never relabel historical code-incompatible proofs or repeat expensive passed tests without cause.

'''
if __name__=='__main__':
 s=status('locally verified; publication preparation','No137 publication claimed until actual push and fetched audit pass.')
 for name in ['AGENTS.md','README-CONTRIBUTORS.md','docs/ARCHITECTURE.md','docs/PROJECT_MEMORY.md','docs/DEVELOPMENT_LOG.md']:
  p=r/name;old=p.read_text(encoding='utf-8-sig');p.write_bytes((s+old if not old.startswith(s) else old).encode())
 short='''## Edition137 / 2026-11-13 — frozen verified references and cheaper durable history

The unified Java world/browser/creation mission remains unchanged. [Edition137](../README-137.md) connects independently supported reference examples to explicit bounded recovery while refusing target-prediction learning and source/evaluation overlap. Completed history reopens without rewriting; complete transaction and source/outcome checks remain. See the measured camera results and full costs before making any promotion claim.

Next: stronger independently checked foreground evidence on genuinely new occlusions/returns; separately stored immutable frame evidence to reduce repeated persistence work; then qualified observations in durable goals. Preserve prior proof identities, private projects, authority checks and defaults.

'''
 for name in ['docs/INTEGRATED_AGENT_ROADMAP.md','docs/PLAYABLE_LEVEL_CREATION_PLAN.md']:
  p=r/name;old=p.read_text(encoding='utf-8-sig');p.write_bytes((short+old if not old.startswith(short) else old).encode())
