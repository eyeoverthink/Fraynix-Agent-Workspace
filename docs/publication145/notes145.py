from pathlib import Path
import json,subprocess
r=Path.cwd()
def status(stage,published=''):
 v=json.loads((r/'docs/readmes/145/verification.json').read_text(encoding='utf8'));c=json.loads((r/'docs/readmes/145/comparison.json').read_text(encoding='utf8'))
 return f'''## Edition 145 — {stage}

2026-11-14, America/Los_Angeles. Isolated J:/Codex-Work/fraynix-workspace-edition145 from c6445f6e. CI checked first:144 documentation passed, compile/tests pending at that check. Method, scorer, UI, source selection/generator and harness frozen at5922cb69 before fresh Basketball/Girl2 images or annotations. Source-only labels committed before predictions. {published}

Explicit trajectory-guard-145 wraps unchanged144. A fully confirmed143 appearance can override a144 refusal only when measured pre-loss target motion supports it and retained rival paths exclude it. At most five samples per path, eight rivals, twelve-frame horizon and capped uncertainty. Only accepted139 baseline observations anchor the target; predictions and restored proposals never extend that anchor. Path conflict, capacity overflow or stale/uncertain motion refuse. Missing rivals never erase evidence. Bounds are uncalibrated image-plane assumptions, not physical accuracy or semantic identity. Monitor145 preserves144 tracker/filter output. All144 accepted proposals remain unchanged. Original source, label, code and pixel replay checks remain mandatory. No defaults, permissions, capacity or automatic-admission changes.

First app attempt refused120-frame camera packs at the existing96-frame limit after three donor runs, before any evaluation. Capacity-only amendment5535c984 partitions each fixed window into96+25 frames with one boundary seed; all119 scored transitions and original labels/pixels retained. Separate tail project repeats donor verification and preserves24-run capacity. This is chunked tracking with a new labelled boundary seed, not uninterrupted120-frame tracking. Frozen production/scorer/main harness unchanged. Original failure, pre-partition manifest and labels preserved. Source review finds no complete camera occlusion in these fixed windows; generated occlusions remain separate, not camera qualification.

{c['finding']}

{c['developmentFinding']}

The large comparison project correctly refuses complete-world export at the existing20MB desktop limit. Amendmenta8156950 retains the failure and requires full export plus active-ambiguity restart in a separate exposed-development project. No pruning or limit change. Fresh return does not latch ambiguity, so it is not used to qualify that restart precondition. Development outcomes remain separate from fresh camera/generated qualification.

{len(v['javaChecks'])} distinct Java checks, {v['desktopChecks']} desktop checks, kernel, agency, offline/package and actual app checks passed. The first development attempt failed two of nine checks; the before-freeze envelope correction and original attempts are retained. Final broad verification includes the additional capacity-overflow case. Full costs, evidence and screenshots are published. Hosted CI remains separate from local success.

{(r/'build/next145.md').read_text(encoding='utf8').strip()}

Keep the durable unified Java world/browser/creation application central. No agents or overlapping heavy jobs. Shared dependency junction read-only. Preserve private projects and frozen001–145. Publish only Fraynix-Agent-Workspace; fetch and audit. Never relabel historical code-bound evidence.

'''
if __name__=='__main__':
 s=status('locally verified; publication preparation','No publication claim until actual push and fetched verification pass.')
 for name in ['AGENTS.md','README-CONTRIBUTORS.md','docs/ARCHITECTURE.md','docs/PROJECT_MEMORY.md','docs/DEVELOPMENT_LOG.md']:
  p=r/name;old=p.read_text(encoding='utf-8-sig');p.write_text(s+old if not old.startswith(s) else old,encoding='utf8')
 short='## Edition 145 / 2026-11-14 — measured trajectory evidence\n\n[Edition145](../README-145.md) tests bounded pre-loss target paths against retained rival paths. Original-pixel replay and independent outcomes measure recovered targets, wrong objects and refusal cost. Explicit experiment; defaults unchanged. Unified Java world/browser/creation remains the destination.\n\n'+(r/'build/next145.md').read_text(encoding='utf8')+'\n\n'
 for name in ['docs/INTEGRATED_AGENT_ROADMAP.md','docs/PLAYABLE_LEVEL_CREATION_PLAN.md']:
  p=r/name;old=p.read_text(encoding='utf-8-sig');p.write_text(short+old if not old.startswith(short) else old,encoding='utf8')
 subprocess.run(['python','-X','utf8','docs/publication145/finalize-inventory145.py'],check=True)
