from pathlib import Path
import json
r=Path.cwd()
def status(stage,published=''):
 v=json.loads((r/'docs/readmes/130/verification.json').read_text(encoding='utf8'))
 return f'''## Edition130 — {stage}

2026-11-12. Isolated S:/Codex-Work/fraynix-workspace-edition130; parent18259fc8. Candidate locked3e591ddd; runtime/audit e6a591d4; final tested build is retained in docs/readmes/130/verification.json. {published}

User explicitly prioritized physical accuracy ahead of the layout pass. Preserve129's failed promotion and28/30→16/30 coverage finding; it is not superseded or relabelled. Both legacy-flow and ordinary adjacent-point forecasting remain defaults. New object-flow-130 is an explicit per-run option. Its frozen seed/context colour evidence supports retained optical-flow landmarks, replenishing only with sufficient tracked support. Initial development source lost10/15 reference observations and is retained. Revised code was locked before fresh windows700/1900/2700/3700/4700; no held tuning. Bounded gate passed: incumbent14/15 accepted,12 within1m,one>2m,mean0.669m; candidate13/15,13 within1m,none>2m,mean0.301m. Common13 references:0.414→0.301m. Candidate missing2versus1; difficult occlusion/identity still unresolved. Same fixed camera, not cross-camera generalization or recognition.

PhysicalAudit130 accepts1..96 later independent declared planar references and scores actual accepted measurements separately from forecasts. Missing references stay in denominator. Centre coverage and explicit reference allowance are separate; radius never silently widened. Audit builds forecasts before scoring reference positions; no fitting/default/world change. Immutable source/clock/geometry/trace/code hashes and original references persist; reads recompute the original outcome and reject changed evidence. Capacity64 distinct audits, no pruning. One-step checks at sparse reference timestamps are not a guarantee for the full0.6second horizon. Existing calibration profiles and prior records untouched.

{len(v['javaChecks'])} distinct Java methods, {v['desktopChecks']} desktop checks, kernel harnesses, actual agency regression, offline preparation/package and actual app workflow verified. Same-task abrupt owned-Java kill/recovery, fresh-source reuse, original procedure lifecycle and saved audit recheck after another cold restart. No new model call, weight training, private webcam recording or native physical actuation. Numerical experiments and app demonstration scopes are separate. Raw public footage stays local; limited screenshots attributed. Hosted CI checked first and reported separately, not assumed passed. Original failures and complete final records are in docs/readmes/130/original-evidence.json.

Next: independent association/occlusion and contact-anchor measurements, then separate fitted uncertainty and different-camera/horizon evaluation. Keep explicit choices until stronger qualification. Connect supported physical tolerance to action refusal/observation requests, then resume coherent daily-use layout. Overall destination is one continuing local Java agent with connected world/browser/creation and durable verified procedures. No delegation or overlapping heavy studies. Node junction read-only. Preserve private projects and frozen001–130. Publish only Fraynix-Agent-Workspace; never relabel prior proofs. Do not repeat passed heavy studies without new cause.

'''
if __name__=='__main__':
 s=status('locally verified; publication preparation','No130 push claimed until actual push and fetched audit pass.')
 for name in ['AGENTS.md','README-CONTRIBUTORS.md','docs/PROJECT_MEMORY.md','docs/DEVELOPMENT_LOG.md','docs/ARCHITECTURE.md']:
  p=r/name;old=p.read_text(encoding='utf-8-sig');p.write_text(s+old if not old.startswith(s) else old,encoding='utf8')
