"""Publish local evidence only after actual qualification, independent inspection and visual review pass."""
from pathlib import Path
import json,hashlib,gzip,shutil,subprocess,xml.etree.ElementTree as ET
r=Path(__file__).resolve().parent.parent; d=r/'docs/readmes/106'
def load(p):return json.loads((r/p).read_text(encoding='utf-8-sig'))
def write(p,s):p.parent.mkdir(parents=True,exist_ok=True);p.write_text(s,encoding='utf8',newline='\n')
def js(p,v):write(p,json.dumps(v,indent=2,ensure_ascii=False)+'\n')
def sha(b):return hashlib.sha256(b).hexdigest()
assert subprocess.run(['git','show-ref','--verify','--quiet','refs/tags/readme-106'],cwd=r).returncode==1
analysis=load('build/analysis106.json');trial=load('build/numerical106-final/result.json');visual=load('build/visual-review106.json')
assert analysis['preliminary']['passed'] and len(analysis['preliminaryRefusals'])==6 and analysis['passed'] and len(analysis['refusals'])==26 and trial['passed'] and visual['passed']
tests=[]
for f in sorted((r/'workspace-engine/build/test-results/test').glob('TEST-*.xml')):
 e=ET.parse(f).getroot();assert e.attrib['failures']=='0' and e.attrib['errors']=='0' and e.attrib['skipped']=='0';tests.append(dict(e.attrib))
assert sum(int(x['tests']) for x in tests)==110
for name in ['desktop106.log','desktop106-additional.log']:assert 'fail 0' in (r/'build'/name).read_text(encoding='utf-8-sig')
assert load('build/source-identity106.json')['passed']
for name,count in [('renderer101-106',5),('renderer102-106',11),('diagnostics106',7)]:
 p=load('build/'+name+'/result.json');assert p['passed'] and len(p['results'])==count
d.mkdir(parents=True,exist_ok=True)
for row,t in zip(analysis['results'],trial['tasks']):row['screenshot']=t['captureKey']+'-1600.png'
js(d/'analysis.json',analysis)
js(d/'verification.json',{'passed':True,'java':tests,'desktopChecks':148,'actualBrowserControls':23,'independentRefusals':analysis['refusals'],'preliminary':analysis['preliminary'],'preliminaryRefusals':analysis['preliminaryRefusals'],'originalProofCheck':load('build/original-proof106.json'),'visual':visual,'sourceIdentity':load('build/source-identity106.json'),'build':load('build/study095-build.json'),'parentCI':load('build/ci106-parent.json')})
captions={}
for row in analysis['results']:
 name=row['screenshot'];shutil.copyfile(r/'build/numerical106-final'/name,d/name);captions[name]=row['name']+': '+row['kind']+', starting with '+str(row['bankBefore'])+' published procedures; '+row['outcome']+'. Actual application capture.'
for name,caption in [('numerical106-1100.png','Actual 1100-pixel workspace with mapped direction-inverted fresh input.'),('negative-reversal-interrupted.png','Actual review boundary before abrupt Java interruption; same data child retained on recovery.'),('refusal106-cold.png','Original changed-artifact refusal retained after cold restart; Resume disabled. The older completed canvas remains visible; the refused request has no new result.')]:
 shutil.copyfile(r/'build/numerical106-final'/name,d/name);captions[name]=caption
shutil.copyfile(r/'build/numerical106/unexpected106.png',d/'tiny106-initial-refusal.png')
captions['tiny106-initial-refusal.png']='Initial actual cohort: exact reserved-input mismatch refused. Original failure retained; corrected final cohort is separate.'
roots=['build/numerical106','build/numerical106-final','build/preliminary106','build/renderer101-106','build/renderer102-106','build/diagnostics106','workspace-engine/build/test-results/test']
extras=['build/java106-final.log','build/numerical106-final.log','build/inspect106-prior.log','build/tiny106-original-goal.json','build/OriginalProof106.java','build/original-proof106.json','build/original-proof106-console-first.bin','build/java106-first.log','build/java106-regression.log','build/desktop106.log','build/desktop106-additional.log','build/renderer101-106.log','build/renderer102-106.log','build/diagnostics106.log','build/source-identity106.json','build/ci106-parent.json','build/study095-build.json','build/analysis106.json','build/inspect106.log','build/visual-review106.json','build/numerical106.log']
paths=[]
for folder in roots:
 for p in (r/folder).rglob('*'):
  if p.is_file() and not any(k in {'desktop-profile','runtime-042','.northstar-kernel','binary'} for k in p.parts) and not p.name.endswith('.lock'):paths.append(p)
paths.extend(r/p for p in extras)
archive=[]
for p in sorted(set(paths)):
 raw=p.read_bytes();compressed=gzip.compress(raw,mtime=0);name=d/'original'/(sha(raw)+'.gz');name.parent.mkdir(exist_ok=True);name.write_bytes(compressed);archive.append({'logicalPath':p.relative_to(r).as_posix(),'path':name.relative_to(r).as_posix(),'bytes':len(compressed),'sha256':sha(compressed),'rawBytes':len(raw),'rawSha256':sha(raw)})
js(d/'original-evidence.json',{'files':archive,'scope':'Lossless actual106 preparation, fresh numeric inputs, unchanged authored implementations, accepted and refused evidence, native admission refusals, cold restart, abrupt recovery, timings, screenshots and test logs. Initial seven completions and tiny-scale serialized-input failure retained separately with full costs. Producer correction is tested in a fresh final cohort. No private live project.'})
kinds={x['path']:x['hashKind'] for x in load('docs/readmes/105/file-inventory.json')}
for p in ['docs/EDITION_106_CONTRACT.md','workspace-engine/src/test/java/fraymus/workspace/Refusal106Test.java','workspace-engine/src/test/java/fraymus/workspace/Numerical106Test.java','workspace-engine/src/test/resources/numerical106/cases.json','workspace-desktop/test/numerical106-integration.cjs','scripts/Inspect-Numerical106.cjs','scripts/Document-Numerical106.py','scripts/numerical106-guide.html']:kinds[p]='utf8-lf'
inventory=[]
for p,kind in sorted(kinds.items()):
 b=(r/p).read_bytes();b=b.decode('utf8').replace('\r\n','\n').encode() if kind=='utf8-lf' else b;inventory.append({'path':p,'bytes':len(b),'sha256':sha(b),'hashKind':kind})
js(d/'file-inventory.json',inventory)
note='docs/obsidian/114-original-refusals.md'
modules=[('chat','Explicit qualification request','workspace-engine/src/main/java/fraymus/workspace/Domain044.java'),('cases','Explicit numerical boundaries','workspace-engine/src/test/resources/numerical106/cases.json'),('supervisor','Authoritative original refusal and recovery','workspace-engine/src/main/java/fraymus/workspace/Agency043.java'),('admission','Authenticated native admission boundary','workspace-engine/src/main/java/fraymus/workspace/WorkspaceServer.java'),('persistence','Stable reserved input before checkpoint','workspace-engine/src/main/java/fraymus/workspace/GoalRunner.java'),('producer','Unchanged input-driven producer','workspace-engine/src/main/java/fraymus/workspace/DataSummary104.java'),('audit','Unchanged independent outcome verifier','workspace-engine/src/main/java/fraymus/workspace/DataSummary102.java'),('host','Actual browser audit delegation','workspace-desktop/workflows.cjs'),('inspection','Independent originals and complete costs','scripts/Inspect-Numerical106.cjs')]
system={'modules':[{'id':i,'name':n,'source':p,'note':note} for i,n,p in modules],'screenshots':[{'path':'docs/readmes/106/'+n,'caption':c} for n,c in captions.items()],'evidence':[{'path':'docs/readmes/106/'+n,'caption':n} for n in ['analysis.json','verification.json','original-evidence.json']]};js(d/'system-map.json',system)

table='\n'.join('| '+x['name']+' | '+x['outcome']+' | '+' | '.join(f'{x[k]/1000:.3f}' for k in ['totalMs','browserMs','restartMs','otherMs'])+' |' for x in analysis['results'])
readme=f'''# Fraynix Agent Workspace — edition 106

## Original refusals, fresh numerical evidence

Chat now preserves Java's original workflow-admission refusal without asking the planner to retry it. A changed retained artifact produces **“Request refused — Created artifact was changed outside its owned revision.”** The same reason survives restart; Resume stays disabled. Stop and a separate reviewed request remain available.

This edition also tests ten numerical cases using a genuinely qualified saved procedure and fresh input. Every accepted report still passes source validation, native analysis, behavior checks and independent checks of its displayed values and chart. No verifier was relaxed to admit an edge case.

[Previous edition 105](README-105.md) · [Interactive offline guide](docs/readmes/106/index.html) · [Complete measurements](docs/readmes/106/analysis.json) · [Validation](docs/readmes/106/verification.json) · [Contributor direction](README-CONTRIBUTORS.md)

![Original refusal retained after restart](docs/readmes/106/refusal106-cold.png)

## What the application does

Fraynix is a local visual agent workspace. Java owns durable tasks, tools, policies, world state, verification, recovery and procedural memory. The desktop presents chat, browser, canvas, spatial views and Game Lab. Physical experiments, recorded observations and reusable creation procedures share that owned execution structure. A tool performs an action; a skill completes a task with checks and recovery.

Here the demonstrated task is a data-to-content procedure: import original motion CSV, normalize explicit units and coordinates, calculate motion/quality, produce a working HTML summary and trace, inspect the actual browser result, and retain or reuse the verified procedure. Edition105 automates five labelled qualification examples before retention. Edition106 preserves that path and strengthens how failures are reported.

The report implementation is authored Java/browser logic. The selected local model remains available, but these recognized requests require **zero planner calls and zero content-generation calls**. This is evidence-backed procedural reuse, not model-weight training or proof of general intelligence.

## Original refusal ownership

1. The server first checks the authenticated interface, current Java task, claimed effect and matching workflow operation.
2. If that native admission fails, Java journals its original reason, route, effect identity, argument hash, claim hash, session and generation.
3. The request becomes terminally interrupted. It cannot silently retry, replace its source or be resumed around the refusal.
4. An exact late host receipt is harmless: it cannot replace the reason, context or result. Foreign and stale receipts are rejected.
5. Chat shows the native reason directly. Restart retains it. A user may stop the task, examine the evidence and start a separate request.

Only Java's own failure at this boundary can establish a native refusal. Matching browser error text, forged fields and unrelated transport failures cannot manufacture that authority. Successful admission clears the refusal boundary before sending its response, so a later response failure is not relabelled as an admission refusal. Ordinary recovery remains available for valid interrupted work.

Edition105 retained an altered-proof refusal but then attempted one failed planner fallback, obscuring the reason. Its original evidence and cost remain frozen in the previous edition. This edition tests the corrected path separately, with no fallback calls.

## A real edge failure found and corrected

The first cohort completed seven requests, then refused tiny-scale creation. Thresholds were written as `1.0E-6` in reserved input, while durable JSON parsing represented the same number as `0.000001`. The exact-input verifier correctly rejected the different bytes.

GoalRunner now freezes sourceData through the checkpoint-compatible JSON representation before generating reserved files. It changes neither values nor the verifier. A new native-checkpoint regression covers small and large exponent thresholds. The entire cohort is rerun in a fresh directory after rebuilding. The earlier failed request remains **{analysis['preliminary']['failedRequestCost']['elapsedMs']/1000:.3f}s**, with a **{analysis['preliminary']['cohortWallMs']/1000:.3f}s** preliminary cohort; none of that cost disappears into the final successes.

This legitimate producer change updates the implementation runtime identity. An actual binding from the earlier build is refused by the original protocol guard. The standalone source/outcome verifier identities remain unchanged; existing original reports still require their original checks. No stored binding is overwritten with a newer identity.

![Initial exact-input refusal, retained separately](docs/readmes/106/tiny106-initial-refusal.png)

## Numerical coverage

| Case | What is checked |
| --- | --- |
| Mapped inversion | Milliseconds/millimetres, negative raw times, time offset, nonzero origin and reversed direction |
| Negative reversal | Positive and negative positions, travel versus displacement, exact speed boundary; abrupt restart after data |
| Half / below / above rounding | Six-decimal half-up presentation around 0.0000005 |
| Flat signed zero | Zero travel/speed without a fabricated trend |
| Tiny scale | Valid sub-microsecond and sub-micrometre input; rounded zero remains distinct from the original nonzero data |
| Large valid scale | Full accepted time/coordinate range and finite derived speed |
| Large origin, small motion | Small displacement relative to a large raw coordinate origin |
| Just-over thresholds | Full-precision gap/speed flags remain even when six-decimal text rounds to the boundary |

The [declared cases](workspace-engine/src/test/resources/numerical106/cases.json) contain expected values for tests only. Those answers are never supplied to the report. An independent inspector reconstructs every normalized row, travel, speed, flag, summary value and SVG point from the original CSV and mapping, then checks all four actual filter states.

Five invalid imports are refused: duplicate times, NaN, infinity, time outside the accepted range and distance outside the accepted range. A finite-input sequence whose derived velocity overflows is also refused before a report or new skill can be retained. Inputs are not silently clamped or repaired.

![Fresh mapped input and independently checked chart](docs/readmes/106/mapped-inversion-1600.png)

## Measured results

**11 completed requests, three required request refusals and five invalid-import refusals.** Preparation runs five original qualification examples and the selected-source report, publishing one actual procedure. Ten fresh numerical requests use that same retained implementation through cold project copies. One recovers from an abrupt Java interruption while preserving its verified data child. The overflow request stops, and two altered-proof requests refuse admission through `/api/workflows/start` qualification and `/api/workflows/reuse` saved-implementation admission. A further cold restart retains the latter refusal.

| Request | Outcome | Complete s | Browser s | Restart s | Other s |
| --- | --- | ---: | ---: | ---: | ---: |
{table}

Complete request time includes coordination, Java persistence, verification and any recorded recovery pause. Categories partition each caller interval; none are subtracted to advertise speed. The full cohort lasts **{analysis['cohortWallMs']/1000:.3f}s**, including **{analysis['setupImportCaptureCopyAndOtherMs']/1000:.3f}s** outside request intervals for app launches, imports, copying, captures and shutdown. These are sequential measurements on this machine, not a matched performance comparison.

## Verification and reproducibility

- 110 focused Java test methods pass, including authentic/stale/foreign refusal authority, forged browser fields, unchanged transport repair, cold restart and numeric boundaries.
- 148 desktop checks and 23 actual browser fixture controls pass.
- The actual Electron/Java cohort passes all declared expectations; browser receipts and captures are retained.
- Independent source/outcome reconstruction passes, with 26 altered-record controls rejected, plus six checks that reject erasure or alteration of the preliminary failure evidence.
- Eight compiled producer/verifier/runner classes and summary104 resources remain byte-identical to edition105. GoalRunner changes only the pre-write serialization boundary; its changed implementation identity is recorded. A genuine old binding is rejected by the unchanged protocol check. Original proofs are not relabelled as new evidence.
- Actual app captures are reviewed at 1600 and 1100 pixels. The offline guide is checked at 1440 and 390 pixels.

[Lossless original records](docs/readmes/106/original-evidence.json) include source CSV, mappings, authored files, native child states, browser receipts, refused attempts, restart records, complete timings and logs. [Canonical source inventory](docs/readmes/106/file-inventory.json) identifies the reviewed source. Historical editions and private live worlds remain separate.

## Run and review

Use Java21 and the repository's existing desktop dependencies. From the repository root, build the installed Java runtime with `gradlew.bat :workspace-engine:installDist`. In `workspace-desktop`, run `npm ci` for a fresh checkout and `npm start`. Keep an isolated project for validation; do not point two running copies at the same private project. Existing dependency junctions should be preserved in the development workspace.

Import CSV with explicit columns, units, origin, direction and thresholds. Select the data, then request: “Qualify the input-driven summary, analyze the selected data, and save workflow as my-summary.” Once it completes, import fresh data and request: “Reuse my-summary version 1 on the selected dataset.” Review the resulting data report, canvas and task history. This route performs the original checks on every execution.

For the fixed study, run `node workspace-desktop/test/numerical106-integration.cjs` only after building, committing the tested runtime and stamping `node scripts/Study-Build095.cjs --stamp`. It requires the existing local model catalogue entry `gpt-oss:20b`, but this cohort makes no inference calls. Each output directory must be new; a rerun must preserve prior originals. Inspect with `node scripts/Inspect-Numerical106.cjs`. The declared cases are software tests, not footage or new camera measurements.

## Next steps and limits

Next, expand persisted-input representation cases across exponent thresholds and restart boundaries, then measure qualification maintenance when a procedure's implementation changes. Identify the exact dependencies of each proof, distinguish an unchanged original from an invalidated one, and measure the cost of fresh qualification before considering any selective reuse. Every source and outcome check must remain. Add independent raw-source revision and stale-claim integration cases beyond the current artifact revision tests.

Broader decimal/exponent combinations, missing-row interactions and imported CSV dialects remain useful numerical targets. Very small values may display as zero at six decimals; the originals and full-precision verification remain authoritative. Large coordinate offsets can still lose precision before normalization; no arbitrary numerical-accuracy claim is made.

The long-term direction remains one local system that observes, predicts, acts, verifies and remembers. This release removes a misleading retry and extends measured reliability. It does not claim unbounded autonomy, arbitrary-task correctness, consciousness or learned model weights.
'''
write(r/'README-106.md',readme)
write(r/'README.md','# Fraynix Agent Workspace\n\nEdition106 preserves the original Java refusal in chat without planner retries and tests fresh numerical boundaries through verified saved procedures.\n\n[Edition106: functions, evidence and next steps](README-106.md) · [Interactive offline guide](docs/readmes/106/index.html) · [Previous edition](README-105.md) · [Contributor direction](README-CONTRIBUTORS.md)\n\n11 actual requests complete; three request refusals and five invalid imports behave as required. Existing source/outcome checks remain unchanged.\n\n![Original refusal after restart](docs/readmes/106/refusal106-cold.png)\n\nJava owns durable goals, tools, verification, recovery and memory. Chat, browser/canvas, spatial views and Game Lab remain integrated.\n\nPublication receipt follows the actual main/tag push and fetched verification.\n')
note_text='2026-11-10, America/Los_Angeles. Isolated S:/Codex-Work/fraynix-workspace-edition106. Authentic Java workflow admission refusals persist the original reason and exact current effect authority; no planner retry, Resume refused, exact late receipts cannot mutate result/context. Foreign/stale/browser-forged refusal rejected; ordinary interrupted recovery remains. Actual cohort: one qualification preparation and ten numerical reuse completions; derived overflow plus two changed-proof admissions refuse; five invalid imports refuse. Cold refusal and abrupt data-boundary recovery pass. 110 Java,148 desktop,23 actual browser controls and26 independent altered-record refusals. Eight compiled classes and summary resources unchanged; GoalRunner freezes the durable numeric representation before producing exact reserved inputs. Its new runtime identity is explicit; a genuine old binding is refused. No generation/planner calls or training claim. Initial seven successes plus tiny-scale serialized-input refusal retained with full costs; final fresh cohort is separate. All costs and originals retained. Private worlds and frozen001–105 preserved. No delegation; heavy work serial. Next: explicit dependency-based qualification maintenance measurements, keeping all checks. Main/tag publication pending actual push/fetch; CI separate.\n'
write(r/note,'# Edition106 — original refusals and numerical edges\n\n'+note_text+'\n[[README-106.md|Full edition]] · [[docs/obsidian/113-automatic-qualification.md|Prior qualification]]\n')
for name in ['AGENTS.md','README-CONTRIBUTORS.md','docs/PROJECT_MEMORY.md','docs/DEVELOPMENT_LOG.md','docs/ARCHITECTURE.md']:
 p=r/name;old=p.read_text(encoding='utf8');header='## Edition106 — validated original refusals\n\n'
 if header not in old:write(p,header+note_text+'\n'+old)
for name,header in [('docs/readmes/INDEX.md','[Edition106 — original refusals and numerical edges](../../README-106.md) · [Offline guide](106/index.html)'),('docs/obsidian/00-home.md','[[docs/obsidian/114-original-refusals.md|Edition106 — original refusals]]')]:
 p=r/name;old=p.read_text(encoding='utf8')
 if header not in old:write(p,header+'\n\n'+old)
js(r/'docs/obsidian/114-original-refusals.canvas',{'nodes':[{'id':'request','type':'file','file':'README-106.md','x':0,'y':0,'width':500,'height':320},{'id':'memory','type':'file','file':note,'x':580,'y':0,'width':500,'height':320}],'edges':[{'id':'refusal','fromNode':'request','fromSide':'right','toNode':'memory','toSide':'left','label':'original reason, exact authority, numerical evidence, retained memory'}]})
template=(r/'scripts/numerical106-guide.html').read_text(encoding='utf8');write(d/'index.html',template.replace('SYSTEM_JSON',json.dumps(system)).replace('INVENTORY_JSON',json.dumps(inventory)).replace('RESULT_JSON',json.dumps(analysis)))
print(json.dumps({'passed':True,'originalFiles':len(archive),'canonicalSources':len(inventory),'javaMethods':110,'actualRequests':14}))
