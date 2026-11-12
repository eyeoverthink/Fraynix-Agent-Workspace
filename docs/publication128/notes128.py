from pathlib import Path
import json
r=Path.cwd()

def status(stage,published=''):
 v=json.loads((r/'docs/readmes/128/verification.json').read_text(encoding='utf8'))
 return f"""## Edition128 — {stage}

2026-11-12. Isolated S:/Codex-Work/fraynix-workspace-edition128; parent86886289; final production b983874b. {published}

One Java-owned observation-to-artifact request binds a completed recorded tracker run, calibrated plane/clock and persistent body. It pins original pixels and metadata, computes causal bounded forecasts, creates a working local HTML explorer and independently checks actual source cells, metrics, future estimates and four rendered axis/frame views. Recovery retains the same owner/checkpoint and source identity. Saving uses the existing qualified procedure registry; reuse requires fresh source bindings and fresh outcomes. Retirement/reactivation and historical proofs remain intact. No native world action, new model calls, new webcam study or neural training claim.

{len(v['javaChecks'])} distinct Java methods, {v['desktopChecks']} desktop checks, executable kernel harnesses, actual agency regression and isolated offline/package receipt passed. The final actual Electron study processes two 96-frame windows from the existing public EPFL recording: each has 95 accepted observations and 93 causal comparisons. First request includes an abrupt owned-Java kill and cold restart of the SAME task; the second reuses v1 with different recorded data and saves v2. Independent reference means are about 5.15 m and 0.89 m over only three annotated positions each; future radii at 0.6 s are about 35.82 m and 20.63 m. Thus software completion passes but accurate physical prediction does NOT. Prioritize tracking/calibration drift and decision-useful uncertainty next. A third out-of-bounds seed remains refused. Final UI review reopens the SAME completed project and verifies both historical contracts and actual artifact controls unchanged. Original sparse-input, capacity, SVG representation, missing receipt and driver failures remain retained. Final source fixes repeated metadata reads while still hashing every original image and rechecking the pinned snapshot. See README-128 and docs/readmes/128/original-evidence.json. Hosted CI is separate; never claim it passed while pending.

Destination: one continuing local Java agent with connected world, browser, canvas and observation surfaces. Next129: bounded adaptation proposed from measured failures, compared with the incumbent and simpler control on unseen cases before promotion; then130: coherent daily use. Preserve private projects, frozen001–128, source ancestry and all verification. No delegation or overlapping heavy/inference work. Node dependency junction remains read-only. Publish only to Fraynix-Agent-Workspace. Read docs/INTEGRATED_AGENT_ROADMAP.md and docs/EDITION_128_CONTRACT.md. Do not repeat passed studies without a new concern.

"""
if __name__=='__main__':
 s=status('locally verified; publication preparation','Do not claim pushed until actual push and fetched audit succeed.')
 for name in ['AGENTS.md','README-CONTRIBUTORS.md','docs/PROJECT_MEMORY.md','docs/DEVELOPMENT_LOG.md','docs/ARCHITECTURE.md']:
  p=r/name;prior=p.read_text(encoding='utf-8-sig');p.write_text(s+prior if not prior.startswith(s) else prior,encoding='utf8')
