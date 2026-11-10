"""Publish local evidence only after actual qualification, independent inspection and visual review pass."""
from pathlib import Path
import json,hashlib,gzip,shutil,subprocess,xml.etree.ElementTree as ET
r=Path(__file__).resolve().parent.parent; d=r/'docs/readmes/108'
def load(p):return json.loads((r/p).read_text(encoding='utf-8-sig'))
def write(p,s):p.parent.mkdir(parents=True,exist_ok=True);p.write_text(s,encoding='utf8',newline='\n')
def js(p,v):write(p,json.dumps(v,indent=2,ensure_ascii=False)+'\n')
def sha(b):return hashlib.sha256(b).hexdigest()
assert subprocess.run(['git','show-ref','--verify','--quiet','refs/tags/readme-108'],cwd=r).returncode==1
analysis=load('build/analysis108.json');trial=load('build/transition108/result.json');visual=load('build/visual-review108.json')
assert analysis['passed'] and len(analysis['refusals'])==28 and trial['passed'] and visual['passed']
tests=[]
for f in sorted((r/'build/before-lineage108/test-results').glob('TEST-*.xml')):
 e=ET.parse(f).getroot();assert e.attrib['failures']=='0' and e.attrib['errors']=='0' and e.attrib['skipped']=='0';tests.append(dict(e.attrib))
assert sum(int(x['tests']) for x in tests)==126
final_tests=[]
for f in sorted((r/'workspace-engine/build/test-results/test').glob('TEST-*.xml')):
 e=ET.parse(f).getroot();assert e.attrib['failures']=='0' and e.attrib['errors']=='0' and e.attrib['skipped']=='0';final_tests.append(dict(e.attrib))
assert sum(int(x['tests']) for x in final_tests)==22
assert load('build/lineage-change108.json')['passed']
for name in ['desktop108.log','desktop108-additional.log']:assert 'fail 0' in (r/'build'/name).read_text(encoding='utf-8-sig')
assert load('build/source-identity108.json')['passed']
for name,count in [('renderer101-108',5),('renderer102-108',11),('diagnostics108',7)]:
 p=load('build/'+name+'/result.json');assert p['passed'] and len(p['results'])==count
d.mkdir(parents=True,exist_ok=True)
for row,t in zip(analysis['results'],trial['tasks']):row['screenshot']=t['captureKey']+'-1600.png'
js(d/'analysis.json',analysis)
js(d/'verification.json',{'passed':True,'java':tests,'finalAffectedJava':final_tests,'finalRuntimeDelta':load('build/lineage-change108.json'),'javaScope':'126-method broad regression passed before three final lineage guards. All22 affected transition, refresh and reuse methods rerun on final runtime; other341 production classes unchanged. Both original runs retained.','desktopChecks':148,'actualBrowserControls':23,'independentRefusals':analysis['refusals'],'visual':visual,'sourceIdentity':load('build/source-identity108.json'),'build':load('build/study095-build.json'),'parentCI':load('build/ci108-parent.json')})
captions={}
for row in analysis['results']:
 name=row['screenshot'];shutil.copyfile(r/'build/transition108'/name,d/name);captions[name]=row['name']+': '+row['kind']+', starting with '+str(row['bankBefore'])+' published procedures; '+row['outcome']+'. Actual application capture.'
for name,caption in [('transition108-1100.png','Actual narrower workspace after new procedure reuse.'),('partial-restart108-interrupted.png','Third fresh example held before its actual audit; no passing observation supplied.'),('selected-restart108-interrupted.png','Five fresh examples complete; selected-source audit held before abrupt interruption.')]:
 shutil.copyfile(r/'build/transition108'/name,d/name);captions[name]=caption
roots=['build/transition108','build/renderer101-108','build/renderer102-108','build/diagnostics108','workspace-engine/build/test-results/test','build/java108-new-tests','build/before-lineage108']
extras=['build/lineage-change108.json','build/restore-check108.json','build/java108-full.log','build/java108-command-refusal.log','build/java108-new.log','build/java108-final.log','build/desktop108.log','build/desktop108-additional.log','build/renderer101-108.log','build/renderer102-108.log','build/diagnostics108.log','build/source-identity108.json','build/ci108-parent.json','build/study095-build.json','build/analysis108.json','build/inspect108.log','build/visual-review108.json','build/transition108.log']
paths=[]
for folder in roots:
 for p in (r/folder).rglob('*'):
  if p.is_file() and not any(k in {'desktop-profile','runtime-042','.northstar-kernel','binary'} for k in p.parts) and not p.name.endswith('.lock'):paths.append(p)
paths.extend(r/p for p in extras)
archive=[]
for p in sorted(set(paths)):
 raw=p.read_bytes();compressed=gzip.compress(raw,mtime=0);name=d/'original'/(sha(raw)+'.gz');name.parent.mkdir(exist_ok=True);name.write_bytes(compressed);archive.append({'logicalPath':p.relative_to(r).as_posix(),'path':name.relative_to(r).as_posix(),'bytes':len(compressed),'sha256':sha(compressed),'rawBytes':len(raw),'rawSha256':sha(raw)})
js(d/'original-evidence.json',{'files':archive,'scope':'Lossless actual108 preparation, genuine107 originals, paired clean/transition requests, partial and selected-source abrupt recovery, five refused admissions, actual browser observations, full costs and logs. No private live world.'})
kinds={x['path']:x['hashKind'] for x in load('docs/readmes/107/file-inventory.json')}
for p in ['docs/EDITION_108_CONTRACT.md','workspace-engine/src/main/java/fraymus/workspace/Qualification108.java','workspace-engine/src/test/java/fraymus/workspace/Qualification108Test.java','scripts/Restore-Qualification107.cjs','workspace-desktop/test/transition108-integration.cjs','scripts/Inspect-Transition108.cjs','scripts/Document-Transition108.py','scripts/transition108-guide.html']:kinds[p]='utf8-lf'
inventory=[]
for p,kind in sorted(kinds.items()):
 b=(r/p).read_bytes();b=b.decode('utf8').replace('\r\n','\n').encode() if kind=='utf8-lf' else b;inventory.append({'path':p,'bytes':len(b),'sha256':sha(b),'hashKind':kind})
js(d/'file-inventory.json',inventory)
note='docs/obsidian/116-explicit-qualification-upgrades.md'
modules=[('chat','Explicit upgrade request','workspace-engine/src/main/java/fraymus/workspace/Domain044.java'),('policy','Dependency differences and transition identity','workspace-engine/src/main/java/fraymus/workspace/Qualification108.java'),('supervisor','Original checks, fresh cases, recovery and publication','workspace-engine/src/main/java/fraymus/workspace/WorkflowRunner.java'),('cases','Unchanged five qualification cases','workspace-engine/src/main/java/fraymus/workspace/Qualification105.java'),('persistence','Unchanged durable goals and reserved inputs','workspace-engine/src/main/java/fraymus/workspace/GoalRunner.java'),('producer','Unchanged input-driven producer','workspace-engine/src/main/java/fraymus/workspace/DataSummary104.java'),('audit','Unchanged outcome verifier','workspace-engine/src/main/java/fraymus/workspace/DataSummary102.java'),('restore','Checked original107 archive restoration','scripts/Restore-Qualification107.cjs'),('inspection','Independent CSV, lineage and cost checks','scripts/Inspect-Transition108.cjs')]
system={'modules':[{'id':i,'name':n,'source':p,'note':note} for i,n,p in modules],'screenshots':[{'path':'docs/readmes/108/'+n,'caption':c} for n,c in captions.items()],'evidence':[{'path':'docs/readmes/108/'+n,'caption':n} for n in ['analysis.json','verification.json','original-evidence.json']]};js(d/'system-map.json',system)
table='\n'.join('| '+x['name']+' | '+x['outcome']+' | '+' | '.join(f'{x[k]/1000:.3f}' for k in ['totalMs','browserMs','restartMs','otherMs'])+' |' for x in analysis['results'])
pairs='\n'.join('| '+x['rep']+' | '+' | '.join(f'{x[k]/1000:.3f}' for k in ['cleanMs','transitionMs','extraTransitionMs'])+' |' for x in analysis['pairs'])
readme=f'''# Fraynix Agent Workspace - edition 108

## Upgrade the procedure without rewriting its evidence

An explicit chat request can now move a qualified summary procedure across a change in Java task coordination. It identifies the old qualification, checks exactly which dependencies changed, rechecks every original source and outcome, runs all five fresh examples, verifies the selected data and saves a new procedure through the existing publication gate.

This first transition supports **coordination changes only**. The numerical producer, report implementation, goal runner and verification dependencies must remain identical. Changes to them are refused. Nothing automatically declares old evidence valid for new code.

[Previous edition107](README-107.md) | [Interactive offline guide](docs/readmes/108/index.html) | [Measurements](docs/readmes/108/analysis.json) | [Validation](docs/readmes/108/verification.json) | [Contributor direction](README-CONTRIBUTORS.md)

![Actual completed upgrade from edition107 originals](docs/readmes/108/pair-1-transition-1600.png)

## What the application is becoming

Fraynix remains one local visual agent workspace. Java owns durable goals, task coordination, tools, world state, policies, verification, recovery and procedural memory. The desktop hosts chat, browser and canvas surfaces, spatial views and Game Lab. A tool performs an action; a skill completes a task using actions, checks and recovery.

The demonstrated workflow imports motion CSV with declared units and coordinates, calculates motion and quality, creates an interactive HTML report, observes the actual browser output, checks every required value and retains a procedure. The report calculates from fresh inputs, including after a restart. Expected answers are never injected into its renderer.

Phase108 strengthens continuity through software upgrades. Retaining a procedure is useful only if the system can say which implementation earned its evidence, whether its dependencies remain compatible, and what needs fresh testing. All demonstrated requests here use owned Java logic with **zero planner calls and zero content-generation calls**. Local models remain available for other tasks. This is verified procedural knowledge, not model-weight training, arbitrary self-modification or evidence of general intelligence.

## The explicit transition

1. Select a fresh dataset and ask to upgrade summary qualification, with a new save name.
2. Java chooses the latest completed qualification under an earlier coordination identity. Mission shows its identifier and changed dependencies.
3. The transition accepts only the `WorkflowRunner` and derived `suite105` identity changes. All other recorded dependency keys and hashes must match. Unknown or changed verifier dependencies refuse admission.
4. Java checks the original supervisor intent, five case identities, raw source revisions, criteria, report revisions, numerical results, actual browser observations, selected-source audit and published skill lineage.
5. It freezes the complete old supervisor hash, source proof references, before/after dependencies and transition code identity. That record is itself hashed.
6. All five examples run with new child identities. The older examples remain in place. The new selected report then passes every original source and outcome check.
7. Old evidence is checked again on recovery, before the selected-source handoff, before saving and before final completion. An edit during the request cannot be hidden by a fresh success.
8. The new skill is exposed only after its completed child and original publication gate pass. Ordinary qualification and direct reuse of an older qualified procedure refuse until an explicit upgrade has completed.

The older procedure remains historical. Use the newly saved procedure name for the new coordination version. Existing earlier implementation-reuse receipts retain their original identities and can still refuse as incompatible. This does not rewrite or promote them.

```mermaid
flowchart LR
  A[Explicit chat upgrade] --> B[Compare exact dependencies]
  B --> C[Verify original sources and outcomes]
  C --> D[Freeze linked transition]
  D --> E[Run five fresh examples]
  E --> F[Verify selected-source report]
  F --> G[Recheck originals before publication]
  G --> H[Save new procedure]
  D --> R[Persist and interrupt]
  R --> V[Reverify and explicitly resume]
  V --> E
```

The UI identifies an upgrade as **All fresh cases; originals retained**. Detailed dependency hashes and lineage remain inspectable in Mission evidence. Plain refresh cannot stand in for an upgrade.

## Genuine earlier evidence, fresh execution

The test restores108 files from the published edition107 bank containing one actually published procedure. Each compressed archive and restored original is hash-checked. The original checkpoint is independently parsed and its checksum verified; old runtime identities are never fabricated. Existing107 browser observations are rechecked, then each new108 result is observed in Chromium.

Two cold pairs compare the same selected CSV and criteria. One request starts with no saved bank and performs clean qualification. The other starts with the genuine107 bank and performs a full upgrade. Both run five examples plus the selected report; upgrade also checks and retains the older evidence. The order reverses in the second pair.

| Pair | Clean qualification seconds | Full upgrade seconds | Extra upgrade seconds |
| --- | ---: | ---: | ---: |
{pairs}

These are two measurements on this machine, with different historical evidence burdens. They are not a claim of equal-bank causal speedup, generalized performance, or a benefit from upgrading unchanged report code. The comparison makes the cost of preserving continuity visible. Native admission/final-check times, browser spans, interruptions, refused requests, restoration and setup are retained.

## Recovery and refusal results

Seven requests complete. Two deliberately terminate Java abruptly: one before the third example's actual browser audit, with two fresh examples complete; the other before the selected-source audit, with all five examples complete. No passing observation is supplied at interruption. Reopening preserves the transition, old supervisor, completed child IDs, active child, artifact versions, report bytes and exact reserved inputs. Explicit Resume completes the remaining real checks. A new dataset then successfully reuses the newly qualified procedure.

![Actual persisted third example before its held audit and abrupt interruption](docs/readmes/108/partial-restart108-interrupted.png)

Five requests correctly refuse: ordinary qualification against the older identity, ordinary refresh, direct reuse of the old saved procedure, a changed original report and a revised original dataset with unchanged CSV. Each retains its original Java reason, makes zero planner retries, saves no new skill and refuses Resume. An earlier canvas may remain visible as history; it is not evidence that the refused request produced a new result.

![Original revised data refused](docs/readmes/108/refused-dataset108-1600.png)

Additional Java tests check unsupported dependency changes, manifest edits, an old source changed while the new selected report awaits audit, an old source revised after upgrade and before later reuse, strict arguments and admission at the original capacity limit. Lifecycle unit replies are labelled fixtures; actual displayed outcomes come from the separate Electron cohort.

| Request | Outcome | Total seconds | Browser | Restart | Other |
| --- | --- | ---: | ---: | ---: | ---: |
{table}

Full cohort: **{analysis['cohortWallMs']/1000:.3f}s**. Request intervals: **{analysis['requestMs']/1000:.3f}s**. Restoration, cold copies, imports, captures, launch and shutdown outside requests: **{analysis['setupAndBetweenMs']/1000:.3f}s**. The earlier107 qualification preparation remains linked historical evidence, not a new108 cost measurement. Component intervals are not added twice.

## Verification and honest boundaries

The first regression invocation placed its test filter after the packaging task; Gradle refused the command before running tests. The argument order was corrected and the full126-method regression passed. Three final lineage guards then bound the old selected-source handoff and preserved original checks during later reuse. All22 affected transition, refresh and implementation-reuse methods were rerun on that final runtime; the other341 compiled production classes were unchanged. Both original test runs and the command refusal are retained. This is not a claim of one126-method invocation on the final bytecode.

-126 distinct focused Java methods across20 classes, including eight new transition cases;22 affected methods rerun after the final lineage guards.
-148 desktop checks and23 actual browser controls.
-Seven actual workflow completions, including two abrupt recoveries, and five required refusals.
-Independent reconstruction from original CSV, every required summary value and SVG point, four filter states, exact lineage and complete cost partitions;28 deliberately altered evidence records rejected.
-Eight existing compiled producer/verifier/helper classes and both summary104 resources remain byte-identical to107. WorkflowRunner and Domain044 change explicitly; Qualification108 is new. The derived suite identity changes legitimately.
-Real application captures at1600 and1100 pixels; the offline guide checked at1440 and390 pixels.

Every original capacity and ownership limit remains:20 executable workflows,64 native goals,32 saved procedures, eight versions,16 qualification supervisors,1200 awake seconds and at most three resumes. No artificial bank padding, skip-check mode or successful-result shortcut was added.

This supports an initial107-to108 coordination transition. A prior transition chain requires separate review. A changed numerical verifier, producer, resource or dependency set is refused. Selective refresh, arbitrary code migration, automatic retirement of history and automatic upgrade of an in-flight request remain unsupported. Hashes establish integrity and identity under the existing local trust model; they are not signatures or protection against an administrator rewriting every record and trust anchor.

[Lossless originals](docs/readmes/108/original-evidence.json), [source inventory](docs/readmes/108/file-inventory.json) and [system map](docs/readmes/108/system-map.json) retain the concrete evidence. Private live worlds and frozen001-107 editions remain separate. Edition107 CI was checked first and its observed status is retained; remote CI is reported separately from local validation.

## Run it and review

Use Java21, build `gradlew.bat :workspace-engine:installDist`, then run `npm start` in `workspace-desktop`. A fresh checkout requires `npm ci`; preserve existing dependency junctions. Use a distinct project for validation, never two owners of the same private database.

Select a fresh dataset and enter:

> Upgrade summary qualification, analyze selected data and save workflow as upgraded-summary.

For a project without earlier qualification:

> Qualify the input-driven summary, analyze selected data and save workflow as my-summary.

After the upgrade completes, import/select different data:

> Reuse upgraded-summary version 1 on the selected dataset.

Inspect Mission for changed dependencies, old qualification ID, fresh case progress, actual report and saved skill. Investigate refused sources; an upgrade does not repair or bypass them.

```text
workspace-engine/src/main/java/fraymus/workspace/
  Qualification108.java       transition identity and supported dependency changes
  WorkflowRunner.java         old evidence, fresh cases, recovery and publication
  Domain044.java              explicit chat intent
workspace-desktop/
  renderer/agency043.js        readable upgrade facts in Mission
  test/transition108-integration.cjs
scripts/
  Restore-Qualification107.cjs checked historical evidence restoration
  Inspect-Transition108.cjs    independent outcome and cost reconstruction
  Document-Transition108.py    evidence-linked documentation
```

To reproduce, build and commit the runtime, stamp `node scripts/Study-Build095.cjs --stamp`, run `node workspace-desktop/test/transition108-integration.cjs`, then `node scripts/Inspect-Transition108.cjs`. The existing local catalogue entry `gpt-oss:20b` is required; these recognized requests make no inference calls. Use a fresh output directory and retain failed attempts.

## Next steps

Measure maintenance as genuine banks approach capacity, including interrupted upgrades and decisions to retain or retire historical procedures. Before supporting changed report or numerical code, define a versioned transition contract that can verify old evidence with its original semantics and acquire wholly fresh evidence for the new implementation. Test incompatible changes and transitions spanning more than one earlier version before enabling them.

The larger direction remains a local agent that observes, predicts, acts, checks and remembers in one integrated environment. This edition supplies a concrete piece of that continuity: knowing which evidence belongs to which code, and paying for fresh verification when coordination changes.
'''.replace('\n-126','\n- 126').replace('\n-148','\n- 148').replace('\n-Seven','\n- Seven').replace('\n-Independent','\n- Independent').replace('\n-Eight','\n- Eight').replace('\n-Real','\n- Real')
write(r/'README-108.md',readme)
write(r/'README.md','# Fraynix Agent Workspace\n\nEdition108 adds explicit, verified qualification upgrades across Java coordination changes. It retains old evidence, runs every fresh example, survives interruption and verifies before saving.\n\n[Edition108: functions, evidence and next steps](README-108.md) | [Offline guide](docs/readmes/108/index.html) | [Previous edition](README-107.md) | [Contributor direction](README-CONTRIBUTORS.md)\n\nSeven actual requests complete; five refuse correctly. Every source and outcome check remains.\n\n![Actual completed upgrade](docs/readmes/108/pair-1-transition-1600.png)\n\nJava owns durable goals, tools, verification, recovery and memory. Chat, browser/canvas, spatial views and Game Lab remain integrated.\n')
summary='2026-11-10. Isolated S:/Codex-Work/fraynix-workspace-edition108; parent c294d77f. Explicit orchestration-only qualification transition from genuine107 browser-qualified originals. Exact old supervisor/dependency/proof/publication lineage rechecked at admission, recovery, handoff and before saving. Five fresh examples and selected report pass original gates. Silent qualification, refresh and old-procedure reuse refuse; unsupported dependencies and chained transitions refuse. Actual12 requests: seven completions, two abrupt recoveries, five required refusals; zero planner/generation calls.126 distinct Java checks plus22 final affected rechecks,148 desktop,23 browser controls and28 altered-record refusals pass. Two cold clean/upgrade pairs retain all costs; no equal-bank speedup or training claim. Eight compiled helpers and two104 resources unchanged. Private worlds/frozen001-107 preserved; no delegation; heavy work serial. Next: growing-bank capacity/maintenance and explicitly versioned changed-verifier transitions. Publication pending actual push/fetch; CI separate.\n'
write(r/note,'# Edition108 - explicit qualification upgrades\n\n'+summary+'\n[[README-108.md|Full edition]] | [[docs/obsidian/115-measured-qualification-refresh.md|Prior refresh measurements]]\n')
for name in ['AGENTS.md','README-CONTRIBUTORS.md','docs/PROJECT_MEMORY.md','docs/DEVELOPMENT_LOG.md','docs/ARCHITECTURE.md']:
 p=r/name;old=p.read_text(encoding='utf8');header='## Edition108 - validated explicit qualification upgrades\n\n'
 if header not in old:write(p,header+summary+'\n'+old)
for name,header in [('docs/readmes/INDEX.md','[Edition108 - explicit qualification upgrades](../../README-108.md) | [Offline guide](108/index.html)'),('docs/obsidian/00-home.md','[[docs/obsidian/116-explicit-qualification-upgrades.md|Edition108 - qualification upgrades]]')]:
 p=r/name;old=p.read_text(encoding='utf8')
 if header not in old:write(p,header+'\n\n'+old)
js(r/'docs/obsidian/116-explicit-qualification-upgrades.canvas',{'nodes':[{'id':'request','type':'file','file':'README-108.md','x':0,'y':0,'width':500,'height':320},{'id':'memory','type':'file','file':note,'x':580,'y':0,'width':500,'height':320}],'edges':[{'id':'upgrade','fromNode':'request','fromSide':'right','toNode':'memory','toSide':'left','label':'old identity, fresh qualification, recovery, measured cost'}]})
template=(r/'scripts/transition108-guide.html').read_text(encoding='utf8');write(d/'index.html',template.replace('SYSTEM_JSON',json.dumps(system)).replace('INVENTORY_JSON',json.dumps(inventory)).replace('RESULT_JSON',json.dumps(analysis)))
print(json.dumps({'passed':True,'originalFiles':len(archive),'canonicalSources':len(inventory),'javaMethods':126,'actualRequests':12}))
