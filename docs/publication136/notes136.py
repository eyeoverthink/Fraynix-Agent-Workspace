from pathlib import Path
import json
r=Path.cwd()
def status(stage,published=''):
 v=json.loads((r/'docs/readmes/136/verification.json').read_text(encoding='utf8'))
 return f'''## Edition136 — {stage}

2026-11-13. Isolated S:/Codex-Work/fraynix-workspace-edition136, parent2b68dd34. CI checked FIRST: linked43398780691 had1030 engine tests,1 failed,2 skipped. Correct unpublished-procedure refusal had an obsolete test exception expectation; corrected type/message without weakening refusal or publication assertions. Method/scorer17fdb4ab, locke7ff6cb4, later labels and exact bytes9a52ceb9 before suffix predictions. Actual tested source/build in docs/readmes/136/verification.json. {published}

Optional seed-anchored-136 preserves accepted incumbent outputs and135 behavior; adds fresh direct forward/reverse reconstruction against immutable first source, strong flow and0.75 box overlap before admission. Audit checks seed identity, stored geometry/motion conditions and external references. Defaults legacy-flow/filter/forecast unchanged. API retains appearance135 key with new schema. No model weights, semantic person identity, private capture or native actuation.

Known development four95-sample windows: prior46 admissions18 correct/28 wrong; new0. Later same-recording four95-sample windows: prior58 admissions37 correct/21 wrong; new3 correct rendered/0 wrong, no camera admission. All760 outputs per method exactly match incumbent, including errors; no bank recoveries. New guard prevents49 observed wrong proposals but also52 correct ones. Useful camera learning remains UNQUALIFIED; no promotion. Later camera partial leaf/caliper/tree/post cover; Bird1 rendered visible flight. No new complete occlusion/out-of-frame return; recordings already source-reviewed, NOT independent unseen videos. External OTB boxes plus AI visibility, no independent human adjudication or distractor labels.

Observation store reads/hashes ALL current state bytes every access under lock; only exact raw identity reuses parsed/validated structure. Writes clone, validate, serialize/reparse, atomically persist before installing detached cache. Every source image, history, code and outcome check remains. No timestamp shortcut or pruning/capacity change. Same-size/time corruption, forged signed history, external writer, failed write, alias and restart tests pass. Cost counters are current-process diagnostics only.

Controlled published135 vs136 identical1/3/6-run states:all36 result hashes equal. Six-run summary median408.2ms→20.1ms; full run264.4→36.8ms. Cold open534.5→748.4ms regresses. Three repeats per operation, not universal speed or realtime camera proof. Anchor tracking costs more:later49.7s vsprior17.4s, incumbent11.8s. Full writes/audits remain expensive.

{len(v['javaChecks'])} distinct Java tests, {v['desktopChecks']} desktop checks, kernel and actual agency integration, offline/package prep. Six actual UI runs570 frozen samples, independent host arithmetic, export/refusal, stop/resume exact prefix and cold restart with original proof rechecks. Native world untouched. Original attempts, full comparisons and costs archived. Hosted CI separate from local/fetched success.

Next: useful foreground/reference evidence with separated evaluation; genuinely new human-adjudicated camera complete occlusion/return and distractor labels; cold-open/write cost while retaining checks. Keep unified Java durable world/browser/creation goals central. No agents or overlapping heavy work. Shared node junction read-only. Preserve private projects and frozen001–136. Publish only Fraynix-Agent-Workspace, fetch/audit, never repeat passed expensive tests without cause.

'''
if __name__=='__main__':
 s=status('locally verified; publication preparation','No136 publication claimed until actual push and fetched audit pass.')
 for name in ['AGENTS.md','README-CONTRIBUTORS.md','docs/ARCHITECTURE.md','docs/PROJECT_MEMORY.md','docs/DEVELOPMENT_LOG.md']:
  p=r/name;old=p.read_text(encoding='utf-8-sig');p.write_bytes((s+old if not old.startswith(s) else old).encode())
 short='''## Edition136 / 2026-11-13 — anchored memory and cheaper verified reads

The unified Java world/browser/creation mission remains unchanged. [Edition136](../README-136.md) blocks49 observed wrong appearance admissions, but also52 correct ones; only3 rendered examples retained and no recovery gain. Camera learning remains unqualified; defaults stay intact. All36 controlled evidence-read hashes match; six-run summary408ms→20ms, while cold startup535ms→748ms. Every source/outcome check remains; no live-camera speed claim.

Next: useful foreground/reference evidence with independent new camera labels, then cold-open/write efficiency and durable goals. Preserve all prior evidence, private projects, authority checks and human-review boundaries.

'''
 for name in ['docs/INTEGRATED_AGENT_ROADMAP.md','docs/PLAYABLE_LEVEL_CREATION_PLAN.md']:
  p=r/name;old=p.read_text(encoding='utf-8-sig');p.write_bytes((short+old if not old.startswith(short) else old).encode())
