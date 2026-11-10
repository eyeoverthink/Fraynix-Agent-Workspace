"""Publish local evidence only after actual qualification, independent inspection and visual review pass."""
from pathlib import Path
import json,hashlib,gzip,shutil,subprocess,xml.etree.ElementTree as ET
r=Path(__file__).resolve().parent.parent; d=r/'docs/readmes/107'
def load(p):return json.loads((r/p).read_text(encoding='utf-8-sig'))
def write(p,s):p.parent.mkdir(parents=True,exist_ok=True);p.write_text(s,encoding='utf8',newline='\n')
def js(p,v):write(p,json.dumps(v,indent=2,ensure_ascii=False)+'\n')
def sha(b):return hashlib.sha256(b).hexdigest()
assert subprocess.run(['git','show-ref','--verify','--quiet','refs/tags/readme-107'],cwd=r).returncode==1
analysis=load('build/analysis107.json');trial=load('build/restart107/result.json');visual=load('build/visual-review107.json')
assert analysis['passed'] and len(analysis['refusals'])==30 and trial['passed'] and visual['passed']
tests=[]
for f in sorted((r/'workspace-engine/build/test-results/test').glob('TEST-*.xml')):
 e=ET.parse(f).getroot();assert e.attrib['failures']=='0' and e.attrib['errors']=='0' and e.attrib['skipped']=='0';tests.append(dict(e.attrib))
assert sum(int(x['tests']) for x in tests)==118
for name in ['desktop107.log','desktop107-additional.log']:assert 'fail 0' in (r/'build'/name).read_text(encoding='utf-8-sig')
assert load('build/source-identity107.json')['passed']
for name,count in [('renderer101-107-validated',5),('renderer102-107',11),('diagnostics107',7)]:
 p=load('build/'+name+'/result.json');assert p['passed'] and len(p['results'])==count
d.mkdir(parents=True,exist_ok=True)
for row,t in zip(analysis['results'],trial['tasks']):row['screenshot']=t['captureKey']+'-1600.png'
js(d/'analysis.json',analysis)
js(d/'verification.json',{'passed':True,'java':tests,'desktopChecks':148,'actualBrowserControls':23,'independentRefusals':analysis['refusals'],'visual':visual,'sourceIdentity':load('build/source-identity107.json'),'build':load('build/study095-build.json'),'parentCI':load('build/ci107-parent.json')})
captions={}
for row in analysis['results']:
 name=row['screenshot'];shutil.copyfile(r/'build/restart107'/name,d/name);captions[name]=row['name']+': '+row['kind']+', starting with '+str(row['bankBefore'])+' published procedures; '+row['outcome']+'. Actual application capture.'
for name,caption in [('restart107-1100.png','Actual narrower workspace after mapped numerical recovery.'),('mapped-inversion-interrupted.png','Persisted report before behavior checks and abrupt interruption.'),('tiny-scale-interrupted.png','Persisted tiny-scale report before independent audit.')]:
 shutil.copyfile(r/'build/restart107'/name,d/name);captions[name]=caption
roots=['build/restart107','build/startup107-original','build/renderer101-107-validated','build/renderer102-107','build/diagnostics107','workspace-engine/build/test-results/test']
extras=['build/java107-new.log','build/java107-final.log','build/desktop107.log','build/desktop107-additional.log','build/renderer101-107.log','build/renderer102-107.log','build/diagnostics107.log','build/source-identity107.json','build/ci107-parent.json','build/study095-build.json','build/analysis107.json','build/inspect107.log','build/visual-review107.json','build/restart107.log']
paths=[]
for folder in roots:
 for p in (r/folder).rglob('*'):
  if p.is_file() and not any(k in {'desktop-profile','runtime-042','.northstar-kernel','binary'} for k in p.parts) and not p.name.endswith('.lock'):paths.append(p)
paths.extend(r/p for p in extras)
archive=[]
for p in sorted(set(paths)):
 raw=p.read_bytes();compressed=gzip.compress(raw,mtime=0);name=d/'original'/(sha(raw)+'.gz');name.parent.mkdir(exist_ok=True);name.write_bytes(compressed);archive.append({'logicalPath':p.relative_to(r).as_posix(),'path':name.relative_to(r).as_posix(),'bytes':len(compressed),'sha256':sha(compressed),'rawBytes':len(raw),'rawSha256':sha(raw)})
js(d/'original-evidence.json',{'files':archive,'scope':'Lossless actual107 preparation, paired refresh/recheck copies, eight numerical recoveries after report input persistence, original and fresh proofs, two refused source revisions, actual browser observations, full costs and logs. No private live world.'})
kinds={x['path']:x['hashKind'] for x in load('docs/readmes/106/file-inventory.json')}
for p in ['docs/EDITION_107_CONTRACT.md','workspace-engine/src/main/java/fraymus/workspace/Qualification107.java','workspace-engine/src/test/java/fraymus/workspace/Qualification107Test.java','workspace-engine/src/test/java/fraymus/workspace/Restart107Test.java','workspace-engine/src/test/resources/restart107/cases.json','workspace-desktop/test/restart107-integration.cjs','scripts/Inspect-Restart107.cjs','scripts/Document-Restart107.py','scripts/restart107-guide.html']:kinds[p]='utf8-lf'
inventory=[]
for p,kind in sorted(kinds.items()):
 b=(r/p).read_bytes();b=b.decode('utf8').replace('\r\n','\n').encode() if kind=='utf8-lf' else b;inventory.append({'path':p,'bytes':len(b),'sha256':sha(b),'hashKind':kind})
js(d/'file-inventory.json',inventory)
note='docs/obsidian/115-measured-qualification-refresh.md'
modules=[('chat','Explicit qualification request','workspace-engine/src/main/java/fraymus/workspace/Domain044.java'),('cases','Explicit numerical boundaries','workspace-engine/src/test/resources/restart107/cases.json'),('supervisor','Durable refresh and original proof checks','workspace-engine/src/main/java/fraymus/workspace/WorkflowRunner.java'),('maintenance','Frozen mode, dependencies and baseline proofs','workspace-engine/src/main/java/fraymus/workspace/Qualification107.java'),('persistence','Stable reserved input before checkpoint','workspace-engine/src/main/java/fraymus/workspace/GoalRunner.java'),('producer','Unchanged input-driven producer','workspace-engine/src/main/java/fraymus/workspace/DataSummary104.java'),('audit','Unchanged independent outcome verifier','workspace-engine/src/main/java/fraymus/workspace/DataSummary102.java'),('host','Actual browser audit delegation','workspace-desktop/workflows.cjs'),('inspection','Independent originals and complete costs','scripts/Inspect-Restart107.cjs')]
system={'modules':[{'id':i,'name':n,'source':p,'note':note} for i,n,p in modules],'screenshots':[{'path':'docs/readmes/107/'+n,'caption':c} for n,c in captions.items()],'evidence':[{'path':'docs/readmes/107/'+n,'caption':n} for n in ['analysis.json','verification.json','original-evidence.json']]};js(d/'system-map.json',system)

table='\n'.join('| '+x['name']+' | '+x['outcome']+' | '+' | '.join(f'{x[k]/1000:.3f}' for k in ['totalMs','browserMs','restartMs','otherMs'])+' |' for x in analysis['results'])
pairs='\n'.join('| '+x['rep']+' | '+' | '.join(f'{x[k]/1000:.3f}' for k in ['recheckMs','refreshMs','extraRefreshMs'])+' |' for x in analysis['pairs'])
readme=f'''# Fraynix Agent Workspace — edition 107

## Exact recovery and measured qualification refresh

Java now supports an explicit request to refresh summary qualification. It verifies existing proofs, freezes their references and code dependencies, executes five fresh examples, verifies selected data, and retains the new procedure through the existing publication gate. Ordinary qualification continues to recheck the retained examples. Every original source and outcome check remains.

Eight numerical tasks survive abrupt Java interruption after their report inputs are saved. Recovery preserves the source, both child identities, report revision and exact reserved input bytes. Fresh browser checks then verify all six summary values, every chart point and four filter states.

[Previous edition106](README-106.md) · [Interactive offline guide](docs/readmes/107/index.html) · [Measurements](docs/readmes/107/analysis.json) · [Validation](docs/readmes/107/verification.json) · [Contributor direction](README-CONTRIBUTORS.md)

![Actual completed qualification refresh](docs/readmes/107/pair-1-refresh-1600.png)

## The application and its direction

Fraynix is a local visual agent workspace. Java owns durable tasks, tools, policies, world state, verification, recovery and procedural memory. Chat, browser, canvas, spatial views and Game Lab present the same execution system. A tool performs an action; a skill completes a task using actions, checks and recovery.

The demonstrated creation procedure imports original motion CSV, applies explicit units and coordinates, calculates motion and quality, creates a working HTML report, checks the actual browser result, and saves or reuses the verified implementation. It calculates from each fresh input. The renderer receives no expected metric answers.

These recognized requests use Java-authored logic with **zero planner calls and zero content-generation calls**. A local model remains selectable for other tasks. This is retained procedural knowledge, not model-weight training or evidence of general intelligence.

## Refresh is an actual operation

1. Select fresh data and explicitly ask to refresh summary qualification.
2. Java verifies matching old examples before admitting new work. A changed report or revised original dataset refuses admission immediately.
3. The request retains its mode, code dependencies and original proof references, bound to a hash.
4. Refresh runs five new example workflows with fresh identities. The original five remain untouched. Ordinary recheck uses the original workflows after re-verification.
5. Java rechecks the original references before selected-source verification, completion and recovery. It checks every fresh example too.
6. The original retention gate verifies the old and new example reports: ten qualifying example proofs after refresh, five after ordinary recheck.
7. Only the independently verified selected-source child publishes the new procedure. Original workflow, native-goal, skill, version, time and resume limits remain.

Mission names the chosen mode. Detailed dependencies, source references and costs remain in workflow evidence. A changed original cannot be replaced with a convenient fresh success. The refusal retains its original Java reason without a planner retry; Resume cannot bypass it.

Changed implementations remain conservatively incompatible. This edition measures full refresh of current code, **not** automatic migration or selective refresh after arbitrary code changes. Old proofs are never relabelled as new evidence.

## Eight numerical process-interruption cases

| Case | Deliberate boundary |
| --- | --- |
| Mapped inversion | Milliseconds/millimetres, origin and direction conversion |
| Tiny scale | Exponent thresholds; values displayed as zero at six decimals |
| Large valid scale | Full accepted time/distance range and large speed criterion |
| Just-over threshold | Gap and speed flags from original full-precision values |
| Exponent threshold | Scientific CSV notation and a small threshold |
| Large exponent criterion | Ten-million-metre-per-second criterion; software stress case |
| Mapped skipped gap | Missing row, converted units, inverted direction and a gap flag |
| Negative large origin | Small motion relative to a large negative coordinate origin |

Each runs in a cold copy of the same genuinely published bank. The harness holds a real browser operation before execution, then terminates Java abruptly. Four stop before behavior checks; four stop before independent audit. No passing observation is supplied at interruption. The same project reopens, Resume is explicitly selected, and the remaining real checks run.

Source, mapping, normalized input, child IDs, artifact ID/version, authored files and exact reserved JSON bytes are compared before interruption, after reopen and after completion. Recovery does not repair or rewrite inputs. All eight finish with the original identities.

![Actual persisted report before its held behavior check; correctly marked untested](docs/readmes/107/mapped-inversion-interrupted.png)

![Actual recovered mapped input](docs/readmes/107/mapped-inversion-1600.png)

Very small values may display zero at six decimals; full-precision observations and thresholds remain authoritative. Large offsets can lose floating-point precision before normalization. These cases do not establish arbitrary precision or physical measurement accuracy. They are labelled synthetic software tests.

## Complete refresh costs

Each pair starts from a cold copy of the same bank containing one real published procedure and five verified examples. Fresh selected CSV and limits are identical. Pair1 runs recheck then refresh; pair2 reverses the order. Recheck creates one fresh selected report; refresh creates five fresh examples plus that report. Both retain every original check. No artificial bank padding or successful-result shortcut is used.

| Pair | Recheck seconds | Full refresh seconds | Extra refresh seconds |
| --- | ---: | ---: | ---: |
{pairs}

These two repetitions measure the price of explicitly acquiring fresh evidence on this machine. They do not show that refresh improves an unchanged implementation or establish a universal speed ratio. Admission and final-verification durations, phase intervals and full caller costs are retained. Phase durations overlap caller time and are not added twice.

| Request | Outcome | Total seconds | Browser | Restart | Other |
| --- | --- | ---: | ---: | ---: | ---: |
{table}

Full cohort: **{analysis['cohortWallMs']/1000:.3f}s**. All request intervals: **{analysis['requestMs']/1000:.3f}s**. Setup, imports, cold copies, captures and shutdown outside requests: **{analysis['setupAndBetweenMs']/1000:.3f}s**. Preparation and refused requests remain included.

## Verification and limits

Before the measured cohort, the unchanged UTF-8 startup guard rejected an incorrectly encoded character in the new mode label. The encoding was corrected and the actual browser checks ran in a fresh project. Original refusal logs are archived. The outer command took2.018s including a test-count check; individual startup time was not separately measured. This is separate from the workflow cohort, and no request result was discarded.

Two deliberate controls refuse an explicit refresh before creating a workflow or skill. One changes a retained report revision; the other revises a raw qualification dataset even with identical CSV. Chat preserves the original reason, makes no planner retries and refuses Resume. An older canvas may remain visible as history; it is not a new result for the refused request.

![Original dataset revision refused](docs/readmes/107/altered-dataset107-1600.png)

- 118 focused Java methods, including eight new refresh, dependency, source-revision and numerical restart tests.
- 148 desktop checks and23 actual Chromium controls.
- 13 actual completions, including eight abrupt recoveries, and two required source refusals.
- Independent original-CSV reconstruction, mappings, exact inputs, six displayed values, every chart point, four filter states, lineage and costs;30 altered-record controls rejected.
- Seven existing compiled producer/verifier/runner classes and summary104 resources byte-identical to106. WorkflowRunner and Domain044 change explicitly; Qualification107 is new. Suite/runtime identity changes are recorded, never hidden.
- Actual screenshots reviewed at1600 and1100 pixels; offline guide checked at1440 and390 pixels.

[Lossless originals](docs/readmes/107/original-evidence.json) retain sources, receipts, restart states, refused attempts and logs. [Canonical inventory](docs/readmes/107/file-inventory.json) identifies the reviewed code. Private live worlds and frozen editions001–106 remain separate. Parent106 CI was checked first; its observed status is retained in validation. Running remote CI is never claimed as passed.

## Run and review

Use Java21. From the root, build `gradlew.bat :workspace-engine:installDist`. In `workspace-desktop`, use existing dependencies and `npm start`; a fresh checkout requires `npm ci`. Preserve existing development dependency junctions. Use a separate validation project rather than opening the same private database twice.

Import CSV with explicit units, origin and direction. Select the data, then request:

> Qualify the input-driven summary, analyze the selected data, and save workflow as my-summary.

For full refresh:

> Refresh qualification for the input-driven summary, analyze the selected data, and save workflow as refreshed-summary.

For fresh data using retained code:

> Reuse my-summary version 1 on the selected dataset.

Inspect Mission, the resulting canvas and original evidence. Investigate invalid sources; refresh cannot bypass a refusal.

To reproduce the study, build and commit the runtime, stamp `node scripts/Study-Build095.cjs --stamp`, run `node workspace-desktop/test/restart107-integration.cjs`, then `node scripts/Inspect-Restart107.cjs`. The existing catalogue entry `gpt-oss:20b` is required but these requests make no inference calls. Every run needs a fresh output directory. Preserve earlier failures and costs. The study uses isolated application projects, not footage or private live worlds.

## Next steps

Define an explicit qualification transition for changed implementations: retain the old identity, identify which dependencies changed, and run fresh required cases before reuse. Measure the cost before considering selective refresh. Extend interrupted qualification across several stages and test capacity refusal with larger genuine banks.

Further numerical work can cover CSV dialects, offset/scale combinations and repeatable uncertain measurements. The wider direction remains one local system that observes, predicts, acts, verifies and remembers. This edition strengthens continuity and measured maintenance; it does not claim unrestricted autonomy, consciousness or learned model weights.
'''
write(r/'README-107.md',readme)
write(r/'README.md','# Fraynix Agent Workspace\n\nEdition107 adds explicit verified qualification refresh and tests exact numerical recovery after report inputs are saved.\n\n[Edition107: functions, evidence and next steps](README-107.md) · [Interactive offline guide](docs/readmes/107/index.html) · [Previous edition](README-106.md) · [Contributor direction](README-CONTRIBUTORS.md)\n\n13 actual requests complete, including eight abrupt recoveries; two changed-source requests refuse correctly. Every source and outcome check remains.\n\n![Actual refreshed qualification](docs/readmes/107/pair-1-refresh-1600.png)\n\nJava owns durable goals, tools, verification, recovery and memory. Chat, browser/canvas, spatial views and Game Lab remain integrated.\n\nPublication receipt follows the actual main/tag push and fetched verification.\n')
note_text='2026-11-10, America/Los_Angeles. Isolated S:/Codex-Work/fraynix-workspace-edition107. Explicit full qualification refresh retains dependencies and old proof references, rechecks originals before new work/recovery/completion, runs five fresh examples and publishes selected data through original gate. Default recheck retained. Actual15 requests:13 completions including eight abrupt numerical recoveries after report-input persistence; two altered-source admissions refuse with original106 reason and zero planner retry. Two paired cold recheck/refresh repetitions at genuine bank1; full preparation/caller/browser/restart/refusal costs retained.118 Java,148 desktop,23 browser controls and30 altered-record refusals. Seven existing compiled classes and summary104 resources unchanged; WorkflowRunner identity changes explicitly. No generated content or weight training. No selective refresh of changed implementations claimed. Private worlds and frozen001–106 preserved. No delegation; heavy work serial. Next: explicit changed-dependency qualification transitions with fresh evidence and costs. Main/tag publication pending actual push/fetch; CI separate.\n'
write(r/note,'# Edition107 — measured qualification refresh\n\n'+note_text+'\n[[README-107.md|Full edition]] · [[docs/obsidian/114-original-refusals.md|Prior numerical and refusal work]]\n')
for name in ['AGENTS.md','README-CONTRIBUTORS.md','docs/PROJECT_MEMORY.md','docs/DEVELOPMENT_LOG.md','docs/ARCHITECTURE.md']:
 p=r/name;old=p.read_text(encoding='utf8');header='## Edition107 — validated recovery and refresh\n\n'
 if header not in old:write(p,header+note_text+'\n'+old)
for name,header in [('docs/readmes/INDEX.md','[Edition107 — recovery and qualification refresh](../../README-107.md) · [Offline guide](107/index.html)'),('docs/obsidian/00-home.md','[[docs/obsidian/115-measured-qualification-refresh.md|Edition107 — measured refresh]]')]:
 p=r/name;old=p.read_text(encoding='utf8')
 if header not in old:write(p,header+'\n\n'+old)
js(r/'docs/obsidian/115-measured-qualification-refresh.canvas',{'nodes':[{'id':'request','type':'file','file':'README-107.md','x':0,'y':0,'width':500,'height':320},{'id':'memory','type':'file','file':note,'x':580,'y':0,'width':500,'height':320}],'edges':[{'id':'refresh','fromNode':'request','fromSide':'right','toNode':'memory','toSide':'left','label':'exact recovery, original references, measured refresh, retained evidence'}]})
template=(r/'scripts/restart107-guide.html').read_text(encoding='utf8');write(d/'index.html',template.replace('SYSTEM_JSON',json.dumps(system)).replace('INVENTORY_JSON',json.dumps(inventory)).replace('RESULT_JSON',json.dumps(analysis)))
print(json.dumps({'passed':True,'originalFiles':len(archive),'canonicalSources':len(inventory),'javaMethods':118,'actualRequests':15}))
