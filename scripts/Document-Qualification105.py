"""Publish local evidence only after actual qualification, independent inspection and visual review pass."""
from pathlib import Path
import json,hashlib,gzip,shutil,subprocess,xml.etree.ElementTree as ET
r=Path(__file__).resolve().parent.parent; d=r/'docs/readmes/105'
def load(p):return json.loads((r/p).read_text(encoding='utf-8-sig'))
def write(p,s):p.parent.mkdir(parents=True,exist_ok=True);p.write_text(s,encoding='utf8',newline='\n')
def js(p,v):write(p,json.dumps(v,indent=2,ensure_ascii=False)+'\n')
def sha(b):return hashlib.sha256(b).hexdigest()
assert subprocess.run(['git','show-ref','--verify','--quiet','refs/tags/readme-105'],cwd=r).returncode==1
analysis=load('build/analysis105.json');trial=load('build/qualification105-final/result.json');visual=load('build/visual-review105.json')
assert analysis['passed'] and len(analysis['refusals'])==28 and len(analysis['preliminaryRefusals'])==6 and trial['passed'] and visual['passed']
tests=[]
for f in sorted((r/'workspace-engine/build/test-results/test').glob('TEST-*.xml')):
 e=ET.parse(f).getroot();assert e.attrib['failures']=='0' and e.attrib['errors']=='0' and e.attrib['skipped']=='0';tests.append(dict(e.attrib))
assert sum(int(x['tests']) for x in tests)==102
for name in ['desktop105.log','desktop105-additional.log']:assert 'fail 0' in (r/'build'/name).read_text(encoding='utf-8-sig')
assert load('build/source-identity105.json')['passed']
for name,count in [('renderer101-105',5),('renderer102-105',11),('diagnostics105',7)]:
 p=load('build/'+name+'/result.json');assert p['passed'] and len(p['results'])==count
d.mkdir(parents=True,exist_ok=True)
for row,t in zip(analysis['results'],trial['tasks']):row['screenshot']=t['captureKey']+'-1600.png'
js(d/'analysis.json',analysis)
js(d/'verification.json',{'passed':True,'java':tests,'desktopChecks':148,'actualBrowserControls':23,'independentRefusals':analysis['refusals'],'preliminaryRefusals':analysis['preliminaryRefusals'],'visual':visual,'sourceIdentity':load('build/source-identity105.json'),'build':load('build/study095-build.json'),'parentCI':load('build/ci105-parent.json')})
captions={}
for row in analysis['results']:
 name=row['screenshot'];shutil.copyfile(r/'build/qualification105-final'/name,d/name);captions[name]=row['name']+': '+row['kind']+', starting with '+str(row['bankBefore'])+' published procedures; '+row['outcome']+'. Actual application capture.'
for name,caption in [('qualification105-1100.png','Actual 1100-pixel workspace after cold reopening the verified collection-eight result.'),('recovery-summary105-interrupted.png','Actual request after two verified examples, immediately before abrupt Java interruption.')]:
 shutil.copyfile(r/'build/qualification105-final'/name,d/name);captions[name]=caption
shutil.copyfile(r/'build/qualification105/unexpected105.png',d/'recovery105-initial-refusal.png')
captions['recovery105-initial-refusal.png']='Original cohort: interrupted child reservation incorrectly blocked parent Resume. Preserved failed request, later corrected and retested separately.'
roots=['build/qualification105','build/qualification105-final','build/preliminary105','build/renderer101-105','build/renderer102-105','build/diagnostics105','workspace-engine/build/test-results/test']
extras=['build/compile105.log','build/java105-qualification.log','build/java105-regression.log','build/java105-final.log','build/analysis105-preliminary.json','build/inspect105-preliminary.log','build/desktop105-first.log','build/desktop105-fixture-events.log','build/desktop105.log','build/desktop105-additional.log','build/renderer101-105.log','build/renderer102-105.log','build/diagnostics105.log','build/source-identity105.json','build/ci105-parent.json','build/study095-build.json','build/analysis105.json','build/visual-review105.json','build/qualification105.log','build/qualification105-final.log']
paths=[]
for folder in roots:
 for p in (r/folder).rglob('*'):
  if p.is_file() and not any(k in {'desktop-profile','runtime-042','.northstar-kernel','binary'} for k in p.parts) and not p.name.endswith('.lock'):paths.append(p)
paths.extend(r/p for p in extras)
archive=[]
for p in sorted(set(paths)):
 raw=p.read_bytes();compressed=gzip.compress(raw,mtime=0);name=d/'original'/(sha(raw)+'.gz');name.parent.mkdir(exist_ok=True);name.write_bytes(compressed);archive.append({'logicalPath':p.relative_to(r).as_posix(),'path':name.relative_to(r).as_posix(),'bytes':len(compressed),'sha256':sha(compressed),'rawBytes':len(raw),'rawSha256':sha(raw)})
js(d/'original-evidence.json',{'files':archive,'scope':'Lossless actual105 requests, all preparation, cold collection copies, inputs, authored outputs, successful and refused evidence, browser receipts, crash records, complete timings, screenshots and test/build logs. Earlier separate14-completion cohort and blocked recovery are preserved, with their own build identity and full costs. No private live project. Development compile and test-fixture errors retained separately from measured results.'})
kinds={x['path']:x['hashKind'] for x in load('docs/readmes/104/file-inventory.json')}
for p in ['docs/EDITION_105_CONTRACT.md','workspace-engine/src/main/java/fraymus/workspace/Qualification105.java','workspace-engine/src/test/java/fraymus/workspace/Qualification105Test.java','workspace-desktop/test/qualification105-integration.cjs','scripts/Inspect-Qualification105.cjs','scripts/Document-Qualification105.py','scripts/qualification105-guide.html']:kinds[p]='utf8-lf'
inventory=[]
for p,kind in sorted(kinds.items()):
 b=(r/p).read_bytes();b=b.decode('utf8').replace('\r\n','\n').encode() if kind=='utf8-lf' else b;inventory.append({'path':p,'bytes':len(b),'sha256':sha(b),'hashKind':kind})
js(d/'file-inventory.json',inventory)
note='docs/obsidian/113-automatic-qualification.md'
modules=[('chat','Explicit qualification request','workspace-engine/src/main/java/fraymus/workspace/Domain044.java'),('cases','Owned labelled test cases and identity','workspace-engine/src/main/java/fraymus/workspace/Qualification105.java'),('supervisor','Durable sequencing, ownership and recovery','workspace-engine/src/main/java/fraymus/workspace/WorkflowRunner.java'),('producer','Unchanged input-driven producer','workspace-engine/src/main/java/fraymus/workspace/DataSummary104.java'),('audit','Unchanged independent outcome verifier','workspace-engine/src/main/java/fraymus/workspace/DataSummary102.java'),('host','Actual browser audit delegation','workspace-desktop/workflows.cjs'),('inspection','Independent originals and complete costs','scripts/Inspect-Qualification105.cjs')]
system={'modules':[{'id':i,'name':n,'source':p,'note':note} for i,n,p in modules],'screenshots':[{'path':'docs/readmes/105/'+n,'caption':c} for n,c in captions.items()],'evidence':[{'path':'docs/readmes/105/'+n,'caption':n} for n in ['analysis.json','verification.json','original-evidence.json']]};js(d/'system-map.json',system)
table='\n'.join('| '+x['name']+' | '+x['kind']+' | '+str(x['bankBefore'])+' | '+x['outcome']+' | '+' | '.join(f'{x[k]/1000:.3f}' for k in ['totalMs','browserMs','restartMs','otherMs'])+' |' for x in analysis['results'])
growth='\n'.join('| '+str(x['procedures'])+' | '+f"{x['meanMs']/1000:.3f}"+' | '+f"{x['minMs']/1000:.3f}–{x['maxMs']/1000:.3f}"+' | '+f"{x['meanAdmissionMs']/1000:.3f}"+' | '+f"{x['meanFinalVerificationMs']/1000:.3f}"+' |' for x in analysis['growth'])
readme=f'''# Fraynix Agent Workspace — edition 105

## Automatic qualification with measured collection costs

One explicit chat request now runs five labelled qualification examples, independently verifies the selected dataset and saves its successful input-driven summary procedure. Java owns the sequence, durable progress, recovery, source checks and publication. Later requests reuse completed examples only after rechecking their originals.

**Actual study: 15 completed requests and two required refusals.** Eight real published procedures build the collection. Six cold-copy probes compare the same fresh source at collections of 0, 4 and 8 procedures. A separate request recovers after abrupt Java interruption. A corrupted displayed example and an altered retained proof each refuse publication.

[Previous edition 104](README-104.md) · [Interactive offline guide](docs/readmes/105/index.html) · [Complete measurements](docs/readmes/105/analysis.json) · [Validation and build identity](docs/readmes/105/verification.json) · [Contributor direction](README-CONTRIBUTORS.md)

![Automatic qualification and verified publication](docs/readmes/105/qualified105-1-1600.png)

## What changed

Edition 104 required separate qualification requests before a summary could be retained. Edition 105 gives that work a durable Java supervisor. Every executable child still uses the original 104 workflow. The five examples have exact inputs, mappings, thresholds, missing-row policies and code identities. They are synthetic software tests, explicitly labelled in the application and evidence.

The selected dataset is frozen separately at admission. Qualification does not overwrite that selection. Each example must verify its original source, normalized data, fresh calculations, behavior receipt, displayed values, chart geometry and four filter states. Only then does the supervisor run the selected-data child. That child publishes through the unchanged 104 qualification gate.

| Example | Input | Purpose |
| --- | --- | --- |
| Monotonic | 17 rows, seconds/metres | Increasing displacement, travel and speed |
| Reversal | 23 rows, seconds/metres | Travel differs from endpoint displacement |
| Flat | 11 rows | Zero travel/speed and stable plotting |
| Changed units and flags | 9 rows, milliseconds/centimetres | SI conversion plus explicit gap and speed flags |
| Skipped row | 6 raw rows, 5 accepted | Missing-row accounting remains visible |

No expected summary answers are sent to the browser. The authored report calculates all six values and the trace from fresh normalized rows. Java independently checks the actual rendered result. This is owned executable logic and procedural evidence, not a content-generation or model-training claim.

## Use it

1. Start an isolated review project with Java 21 and the checked-in Gradle wrapper, then `Launch-Agent-Workspace.bat`. Keep private live projects separate from tests and upgrades.
2. Import and select the motion CSV in Data tasks. Choose the time and displacement columns, units and missing-row policy explicitly.
3. Send: **“Qualify the input-driven summary, analyze the selected data, and save workflow as my-qualified-summary.”** Use a new lowercase name with letters, digits or hyphens.
4. Mission shows verified example progress, whether examples are newly created or rechecked, and the published collection count. The selected-source report opens in the existing canvas.
5. After interruption, use the same request's **Resume** control. Completed examples keep their identities. Stop releases ownership. A failed original or independent audit remains a refusal; Resume cannot turn it into passing evidence.
6. A later explicit qualification request rechecks matching original examples. To reuse a published report implementation on fresh data, the established **“Reuse my-qualified-summary version 1 on the selected dataset”** path remains available with all original checks.

“Review after qualification” requests an explicit pause after the five examples and before the selected-source report. It retains ownership until Resume or Stop. Imported text cannot install handlers or fabricate passing evidence.

For a separate Windows review project, run from the repository root with Java 21 and Node.js installed:

```powershell
$env:FRAYNIX_WORKSPACE_PROJECT = Join-Path $env:USERPROFILE '.fraynix-agent-workspace/projects/review-105'
.\\Launch-Agent-Workspace.bat
```

Choose a different project folder if that name already contains work you intend to preserve separately. The launcher prepares the Java engine and verifies desktop dependencies. Initial dependency preparation may require a connection; after preparation, `Launch-Agent-Workspace.bat -Offline` uses the matching cached dependencies. Reconnecting to a continuing Java owner preserves that owner's loaded installation; use the app's Stop Java control before upgrading it.

## Durable recovery and limits

Java persists each child intent before execution, uses stable child identifiers and reconciles an imported example that was saved immediately before interruption. The measured recovery ends the Java process after two verified examples; reopening and explicit Resume continue the same request with exactly six executable workflows and twelve native goals. No verified example is duplicated.

The supervisor preflights space for all missing examples, the selected request and publication. Existing limits remain: **20 executable workflows, 64 native goals, 32 procedures and 8 versions per procedure name**. The new administrative supervisor history is separately bounded at 16. It has a retained 1,200-second awake budget and at most three resumes; wall-clock interruption time remains included in the external measured cost. Supervisors are not saved skills and do not count as invented procedure growth.

Original proof changes cause refusal rather than replacement. The generated case identity includes its own implementation, runner and 104 producer identity. Older runner-bound implementations are not relabelled after upgrades. Preserve and inspect their originals, then start a newly reviewed request if their identity is no longer supported.

![Same request recovered after abrupt Java interruption](docs/readmes/105/recovery-summary105-1600.png)

## Failure found and corrected before release

The first cohort completed 14 requests, then its abrupt-restart test could not resume. The interrupted child still held the workspace reservation, while host admission only recognized its parent. This was an application defect, not a successful recovery. Java retained the original two completed examples and published no procedure for the blocked request.

The fix maps the interrupted parent to its exact owned child reservation. All other busy-state, ownership, source and outcome checks remain. A new Java regression test checks the original failure and rejects a foreign child. The final17-request cohort below is fresh and separate; no old failure is relabelled.

The preliminary cohort cost **{analysis['preliminary']['cohortWallMs']/1000:.3f} seconds**, including **{analysis['preliminary']['requestMs']/1000:.3f} seconds** in requests and **{analysis['preliminary']['failedRecoveryCost']['elapsedMs']/1000:.3f} seconds** in the failed recovery. Both cohorts, build identities and original files remain archived. Six additional altered-record controls reject erased failure costs or invented success/resume. Development and rebuild time between cohorts is not included in either measured cohort.

![Original recovery refusal, preserved as a failure](docs/readmes/105/recovery105-initial-refusal.png)

## Measured cost as the collection grows

The collection grows through eight actual successful publication requests on varied inputs. No procedure records are inserted to pad the counts. The first uses the unchanged published Delfys013 recording; the other preparation inputs and held-out probe are declared synthetic arithmetic sequences. None is represented as new camera footage.

Each checkpoint is copied into a fresh project and reopened cold. Two probes per checkpoint use the same 37-row held-out dataset and identical chat prompt. The empty checkpoint creates its five examples; collections of four and eight recheck their originals. Collection size also changes task history and provenance volume, so this is a first-use/recheck and collection-growth observation, not a pure causal estimate of bank size.

| Published procedures | Mean complete request (s) | Observed range (s) | Mean admission (s) | Mean final verification (s) |
| --- | --- | --- | --- | --- |
{growth}

Eight preparation requests cost **{analysis['preparationMs']/1000:.3f} seconds** in total. All 17 request intervals cost **{analysis['requestMs']/1000:.3f} seconds**; the complete cohort spans **{analysis['cohortWallMs']/1000:.3f} seconds**, retaining another **{analysis['setupCaptureCopyAndOtherMs']/1000:.3f} seconds** for startup, imports, copying, captures, inspection and shutdown outside requests. Preparation is not omitted from the result.

Browser intervals are unioned, so nested checks are not double-counted. Restart includes process interruption and reopening. Other includes Java computation, persisted checkpoints, original proof checks, scheduling and harness polling. Native phase wall intervals overlap these categories and are not added again. Admission/final verification use monotonic timers; complete request timing starts before chat submission and ends at the collected outcome.

| Request | Role | Initial collection | Outcome | Total (s) | Browser (s) | Restart (s) | Other (s) |
| --- | --- | --- | --- | --- | --- | --- | --- |
{table}

![Cold collection-eight probe](docs/readmes/105/probe-8-1-1600.png)

## Verification and adversarial checks

- **102 Java tests** pass: all 93 focused prior checks plus nine new lifecycle, recovery, ownership, capacity, changed-source and failed-example checks.
- **148 desktop checks** pass: existing routing/transport/selection/accounting checks and new nested audit delegation, parent progress, paused-parent and late-result handling.
- **23 actual browser controls** pass: legacy data, summary/trace and diagnostics fixtures. These are explicitly handcrafted controls, separate from the actual chat study.
- **28 final altered-evidence inspections plus six prior-failure controls** refuse changed sources, mappings, criteria, skipped rows, authored code, normalized inputs, displayed values/points, visibility, filter state, proof links, ownership, padded/unpublished procedures, missing preparation, changed recovery identities and omitted costs.
- The compiled `Creation`, `MotionData`, `DataCreation101`, `DataSummary102` and `DataSummary104` classes match edition 104 byte for byte; the authored summary resources match the parent Git bytes.
- Actual application screenshots are reviewed at 1600 and 1100 pixels; the offline guide is checked at 1440 and 390 pixels. Initial Java compile syntax and desktop test-fixture mistakes are preserved in development logs. The first measured cohort also exposed a real host recovery-admission defect; its complete evidence and cost remain separate from the fresh final cohort.

The original source/outcome checks remain active on every request. Browser behavior passing alone is insufficient: the injected displayed travel of 999 is refused by the independent audit, and a revised saved example is refused before starting new executable work. Neither control adds a procedure.

![Deliberately corrupted qualification output refused](docs/readmes/105/fault-summary105-1600.png)

## Reproduce and inspect

The actual cohort is `workspace-desktop/test/qualification105-integration.cjs`. It refuses an existing output directory. Build the engine, record the installed inputs/JARs with `scripts/Study-Build095.cjs --stamp`, then run the cohort with an isolated project. Production runtime files must stay unchanged during measurement. The test requires the actual local model catalogue to contain `gpt-oss:20b`; the authored summary uses zero content-generation calls. App limits are 512 MiB Java heap and two processors; the focused test JVM uses the established 1,024 MiB/four-processor limit, serially.

`scripts/Inspect-Qualification105.cjs` independently rebuilds all values from the original CSV, validates actual browser observations, checks genuine publication ancestry and recomputes exclusive timing. [Lossless originals](docs/readmes/105/original-evidence.json) retain preparation, checkpoint copies, all requests, authored artifacts, failed controls, journals, timings and screenshots. The [source inventory](docs/readmes/105/file-inventory.json) records canonical bytes; the [system map](docs/readmes/105/system-map.json) links responsibilities to source. Remote CI status is recorded separately from local validation.

## Direction and next steps

This closes a practical autonomy gap: the user requests one complete task while Java carries out a known qualification procedure, checks it, resumes interruptions and remembers the verified result. The existing world, physics, browser, canvas and Game Lab remain the foundation. Procedural memory records evidence and executable behavior rather than asserting that activity alone is learning.

Next, expand numerical and mapping coverage: sign reversals, time offsets, direction inversion, precision boundaries, extreme but valid scales and threshold boundaries. Then measure which qualification evidence must be refreshed when an implementation changes. Any selective refresh needs explicit dependency identities and must retain every source/outcome check. Larger collections need a measured archival/retention design before the current history limits are raised.

Five authored examples do not establish arbitrary-data correctness, general intelligence or model-weight improvement. Two timings per checkpoint are a small sequential sample on this machine. Initial14 successes and one blocked recovery are separately retained; final results do not erase them. Preserve these limits in later comparisons and document failures with the same care as successes.
'''
write(r/'README-105.md',readme)
write(r/'README.md','# Fraynix Agent Workspace\n\nEdition 105 automates five qualification examples, verifies fresh selected data and retains the successful procedure. Java preserves progress across interruption and rechecks original evidence before reuse.\n\n[Edition 105: functions, measurements and next steps](README-105.md) · [Interactive offline guide](docs/readmes/105/index.html) · [Previous edition](README-104.md) · [Contributor direction](README-CONTRIBUTORS.md)\n\n15 actual requests completed; corrupted output and altered proof controls refused. Collection growth uses genuine published procedures at 0, 4 and 8 entries, with complete preparation and recovery costs retained.\n\n![Automatic qualification](docs/readmes/105/qualified105-1-1600.png)\n\nJava owns durable goals, tools, verification and memory. Chat, browser/canvas, spatial views and Game Lab remain integrated.\n\nPublication receipt follows the actual main/tag push and fetched verification.\n')
note_text='2026-11-09, America/Los_Angeles. Isolated S:/Codex-Work/fraynix-workspace-edition105. Java-owned durable supervisor automates five explicitly synthetic qualification cases through unchanged104 workflows, then verifies selected data and publishes through the original104 gate. Fifteen actual requests completed; displayed corruption and altered retained proof refused. Eight genuine preparation publications, six cold-copy probes at bank0/4/8, one abrupt recovery with stable original child IDs. Full preparation, caller, restart and refusal costs retained; two repetitions, no causal speedup claim. 102 Java,148 desktop,23 actual browser controls and28 final plus6 prior-failure altered-record refusals pass. Five verifier classes and104 resources unchanged. No content generation or weight training. Separate16-supervisor history; executable20/native64/procedures32/version8 limits remain;1200s awake/three resumes. Preserve private worlds, old worktrees and frozen001–104. No delegation; heavy work serial. Initial host Resume ownership defect fixed and fresh final cohort rerun; prior14 successes plus failed recovery26.608s retained separately. Read README105 and contract. Next: broader numerical/mapping cases and explicit dependency-based refresh measurements. Main/tag publication pending actual push/fetch; CI separate.\n'
write(r/note,'# Edition 105 — automatic qualification\n\n'+note_text+'\n[[README-105.md|Full edition]] · [[docs/obsidian/112-input-driven-summaries.md|Prior input-driven summaries]]\n')
for name in ['AGENTS.md','README-CONTRIBUTORS.md','docs/PROJECT_MEMORY.md','docs/DEVELOPMENT_LOG.md','docs/ARCHITECTURE.md']:
 p=r/name;old=p.read_text(encoding='utf8');header='## Edition105 — validated automatic qualification\n\n'
 if header not in old:write(p,header+note_text+'\n'+old)
for name,header in [('docs/readmes/INDEX.md','[Edition 105 — automatic qualification](../../README-105.md) · [Offline guide](105/index.html)'),('docs/obsidian/00-home.md','[[docs/obsidian/113-automatic-qualification.md|Edition 105 — automatic qualification]]')]:
 p=r/name;old=p.read_text(encoding='utf8')
 if header not in old:write(p,header+'\n\n'+old)
js(r/'docs/obsidian/113-automatic-qualification.canvas',{'nodes':[{'id':'request','type':'file','file':'README-105.md','x':0,'y':0,'width':500,'height':320},{'id':'memory','type':'file','file':note,'x':580,'y':0,'width':500,'height':320}],'edges':[{'id':'qualification','fromNode':'request','fromSide':'right','toNode':'memory','toSide':'left','label':'automatic examples, original checks, measured cost, durable retention'}]})
template=(r/'scripts/qualification105-guide.html').read_text(encoding='utf8');write(d/'index.html',template.replace('SYSTEM_JSON',json.dumps(system)).replace('INVENTORY_JSON',json.dumps(inventory)).replace('RESULT_JSON',json.dumps(analysis)))
print(json.dumps({'passed':True,'originalFiles':len(archive),'distinctArchivePayloads':len(set(x['path'] for x in archive)),'canonicalSources':len(inventory),'javaMethods':102,'actualRequests':17}))
