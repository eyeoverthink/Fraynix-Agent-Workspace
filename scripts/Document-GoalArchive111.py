"""Publish only independently inspected, actual edition111 results."""
from pathlib import Path
import json,hashlib,gzip,subprocess,shutil,xml.etree.ElementTree as ET
r=Path(__file__).resolve().parent.parent;d=r/'docs/readmes/111'
def load(p):return json.loads((r/p).read_text(encoding='utf-8-sig'))
def write(p,s):p.parent.mkdir(parents=True,exist_ok=True);p.write_text(s,encoding='utf8',newline='\n')
def js(p,v):write(p,json.dumps(v,ensure_ascii=False,indent=2)+'\n')
def sha(b):return hashlib.sha256(b).hexdigest()
assert subprocess.run(['git','show-ref','--verify','--quiet','refs/tags/readme-111'],cwd=r).returncode==1
a=load('build/analysis111.json');trial=load('build/archive111-final/result.json');visual=load('build/visual-review111.json');identity=load('build/source-identity111.json')
assert a['passed'] and len(a['refusals'])==24 and trial['passed'] and visual['passed'] and identity['passed']
tests=[]
for f in sorted((r/'build/java111-broad-results').glob('TEST-*.xml')):
 e=ET.parse(f).getroot();assert all(e.attrib[k]=='0' for k in ['failures','errors','skipped']);tests.append(dict(e.attrib))
assert sum(int(t['tests']) for t in tests)==150
finalTests=[]
for f in sorted((r/'workspace-engine/build/test-results/test').glob('TEST-*.xml')):
 e=ET.parse(f).getroot();assert all(e.attrib[k]=='0' for k in ['failures','errors','skipped']);finalTests.append(dict(e.attrib))
assert sum(int(t['tests']) for t in finalTests)==24
for log in ['desktop111.log','desktop111-additional.log']:assert 'fail 0' in (r/'build'/log).read_text(encoding='utf-8-sig')
d.mkdir(parents=True,exist_ok=True);js(d/'analysis.json',a)
js(d/'verification.json',{'passed':True,'java':tests,'javaMethods':150,'finalAffectedJavaMethods':24,'finalAffectedJava':finalTests,'correction':load('build/correction111.json'),'coverage':load('build/coverage111.json'),'desktopChecks':148,'originalIntegrityControls':a['refusals'],'build':load('build/study095-build.json'),'unchangedDependencies':identity,'actualStudy':{'requests':17,'completed':11,'refused':6,'abruptRecoveries':1,'entry':'existing native workflow controls; chat24 remains full','initialChatBoundary':load('build/native-boundary111.json'),'initialRecorderBoundary':load('build/recorder-boundary111.json'),'archivedChildGoals':74,'archivedWorkflows':58,'retainedProcedures':32},'visual':visual,'parentCI':load('build/ci111-parent.json'),'scope':'Actual source/outcome checks preserved. Exact record archival is separately verified from continued original evidence validity. Completed owned children retain exact bytes outside active scheduling; active64 unchanged.'})
captions={}
for p in sorted((r/'build/archive111-final').glob('*.png')):shutil.copyfile(p,d/p.name);captions[p.name]='Actual edition111 app capture: '+p.stem
paths=[]
for root in ['build/archive111','build/archive111-native','build/archive111-final','workspace-engine/build/test-results/test','build/java111-target-results','build/java111-broad-results','build/before-correction111','build/java111-initial-results']:
 for p in (r/root).rglob('*'):
  if p.is_file() and not any(x in {'desktop-profile','runtime-042','.northstar-kernel','binary'} for x in p.parts) and not p.name.endswith('.lock'):paths.append(p)
for name in ['java111.log','java111-final.log','correction111.json','coverage111.json','java111-initial.log','java111-archive.log','compile111.log','desktop111.log','desktop111-additional.log','source-identity111.json','ci111-parent.json','study095-build.json','analysis111.json','archive111.log','archive111-native.log','archive111-final.log','native-boundary111.json','recorder-boundary111.json','inspect111.log','inspect111-initial.log','Inspect-GoalArchive111-initial.cjs','inspector-correction111.json','visual-review111.json']:
 paths.append(r/'build'/name)
originals=[]
for p in sorted(set(paths)):
 raw=p.read_bytes();compressed=gzip.compress(raw,mtime=0);target=d/'original'/(sha(raw)+'.gz');target.parent.mkdir(exist_ok=True)
 if not target.exists():target.write_bytes(compressed)
 originals.append({'logicalPath':p.relative_to(r).as_posix(),'path':target.relative_to(r).as_posix(),'bytes':len(compressed),'sha256':sha(compressed),'rawBytes':len(raw),'rawSha256':sha(raw)})
js(d/'original-evidence.json',{'files':originals,'scope':'Actual isolated closed110 history, exact archive payloads/checkpoints, native originals, all fresh browser outcomes, recovery and refusals with full costs. Private live worlds excluded.'})
kinds={v['path']:v['hashKind'] for v in load('docs/readmes/110/file-inventory.json')}
for p in ['docs/EDITION_111_CONTRACT.md', 'workspace-desktop/test/archive111-integration.cjs', 'workspace-engine/src/test/java/fraymus/workspace/GoalArchive111Test.java', 'workspace-engine/src/test/java/fraymus/workspace/Qualification111Test.java', 'workspace-engine/src/main/java/fraymus/workspace/GoalArchive111.java', 'workspace-engine/src/main/java/fraymus/workspace/Qualification111.java', 'workspace-engine/src/main/resources/history111-published.json', 'scripts/Inspect-GoalArchive111.cjs', 'scripts/Document-GoalArchive111.py', 'scripts/goalarchive111-guide.html']:kinds[p]='utf8-lf'
inventory=[]
for p,kind in sorted(kinds.items()):
 b=(r/p).read_bytes();b=b.decode('utf8').replace('\r\n','\n').encode() if kind=='utf8-lf' else b;inventory.append({'path':p,'bytes':len(b),'sha256':sha(b),'hashKind':kind})
js(d/'file-inventory.json',inventory)
note='docs/obsidian/119-completed-child-goal-archives.md'
modules=[('archive','Exact child-goal archive transaction','workspace-engine/src/main/java/fraymus/workspace/GoalArchive111.java'),('checkpoint','Checked atomic checkpoint generations','fraynix-modules/factory-runtime/src/main/java/fraymus/factory/WorldCheckpointStore.java'),('api','Owned Java archive endpoints','workspace-engine/src/main/java/fraymus/workspace/WorkspaceServer.java'),('view','History preview, archive and recheck','workspace-desktop/renderer/workflow-panel.js'),('goals','Active goal lifecycle and retained procedures','workspace-engine/src/main/java/fraymus/workspace/GoalRunner.java'),('transition','Explicit runtime qualification transition','workspace-engine/src/main/java/fraymus/workspace/Qualification111.java'),('audit','Original source and actual outcome verifier','workspace-engine/src/main/java/fraymus/workspace/DataSummary102.java'),('study','Actual archive, reuse and recovery study','workspace-desktop/test/archive111-integration.cjs'),('inspection','Independent archive and outcome reconstruction','scripts/Inspect-GoalArchive111.cjs')]
system={'modules':[{'id':i,'name':n,'source':p,'note':note} for i,n,p in modules],'screenshots':[{'path':'docs/readmes/111/'+p,'caption':caption} for p,caption in captions.items()],'evidence':[{'path':'docs/readmes/111/'+p,'caption':p} for p in ['analysis.json','verification.json','original-evidence.json']]};js(d/'system-map.json',system)
table='\n'.join('| '+v['name']+' | '+v['outcome']+' | '+str(v['bankAfter'])+' | '+str(v['activeGoals'])+' / '+str(v['archivedGoals'])+' | '+f"{v['totalMs']/1000:.3f}"+' |' for v in a['results'])
archive_table='\n'.join('| '+v['name']+' | '+str(v['records'])+' | '+str(v['beforeActive'])+' → '+str(v['afterActive'])+' | '+f"{v['totalMs']/1000:.3f}"+' |' for v in a['archives'])
readme=f'''# Fraynix Agent Workspace — edition 111

## Archive completed child goals and keep their evidence

Edition 111 extends the Java brain's durable memory lifecycle to **completed, owned data/creation child goals**. It stores their exact original payloads under checked content addresses and replaces active records through one atomic goals checkpoint. Original IDs, parent ownership, sources, reports, audits and saved-procedure references remain intact.

The **64-active-goal limit is unchanged**. The actual application archived **74 child goals**, retained **58 archived workflow records**, completed **11 fresh native workflow requests**, recovered one abrupt interruption and reached **32 genuinely published procedures**. Six requests correctly refused capacity, changed runtime identity or revised original evidence. The final growing project held 20 active children and 74 archived children. Archive integrity and original evidence validity were checked separately.

[Previous edition 110](README-110.md) · [Offline interactive guide](docs/readmes/111/index.html) · [Measurements](docs/readmes/111/analysis.json) · [Validation](docs/readmes/111/verification.json) · [Contributor direction](README-CONTRIBUTORS.md)

![Actual History panel after archiving current child goals](docs/readmes/111/current111-children-1600.png)

## The problem and the continuing design

Fraynix is a local agent workspace. Java owns durable tasks, tools, physical state, observations, verification, procedural memory and recovery. The desktop hosts chat, browser and creation surfaces, spatial views and Game Lab. A tool performs an action; a skill completes a task with actions, checks and recovery. Continued operation needs both bounded active work and retrievable evidence of what worked.

Edition110 released completed workflow slots while retaining their original child goals. Its real trial reached 62 retained goals. The next request refused because the existing admission check reserves three slots under the 64-goal ceiling, even though these data workflows normally execute two. Raising the ceiling or deleting evidence would not provide the requested lifecycle. This edition moves eligible completed children outside active scheduling while keeping their original evidence available to the same verification paths.

The archive is exact retained history. It is not a generated summary, a new training example automatically accepted as truth, or an LLM weight update. Nothing about moving bytes proves that their referenced sources remain valid. Fresh verification still checks those sources and the actual outcomes.

## Using the app

Open **History** from the main toolbar. The existing workflow archive controls remain, followed by the child-goal section. It shows active and archived child counts, eligible records and the unchanged active limit. Refresh the preview, then use **Verify & archive completed child goals**. Java checks the parent and each owned stage before committing.

Select an archived child to inspect its original record or recheck its evidence. Stable-ID lookup resolves the same goal after restart. The active goal list no longer schedules it, but workflows and saved procedures retain their original references. Archive view metadata is separate from the retained original payload.

Only completed data and creation children whose completed parent passes all original checks qualify. Active, interrupted, cancelled, unpublished, standalone, physical and graph goals stay in their existing active lifecycle. Archive operations require an idle runner and a current preview; stale plans, foreign or duplicate IDs and unsupported arguments refuse. There is no hidden deletion or automatic evidence substitution.

![Actual original edition 110 children preserved outside active scheduling](docs/readmes/111/original110-children-1600.png)

## Atomic storage, stable identity

```mermaid
flowchart LR
  A[Current preview of completed children] --> B[Verify parent, sources and owned outcomes]
  B --> C[Write and force exact original goal bytes]
  C --> D[Atomic goals checkpoint stores references]
  D --> E[Remove only committed children from active scheduling]
  E --> F[Stable IDs resolve and reverify original evidence]
```

Child payloads live beneath `goals/archive111/`, named by their SHA-256 digest. A bounded checkpoint reference records the original ID and creation time, schema, byte count, digest and archival time. Reads validate the reference, regular-file type, length, hash, identity and completion state. Missing, changed or substituted files refuse. Archived records cannot be edited as active goals.

Files are prepared and forced before the checked checkpoint is saved. Before that commit, original active records remain authoritative. If the checkpoint write fails, every original active payload remains; prepared files stay available for a checked retry. The tests reopen after such a failure and retry successfully. After commit, the in-memory world and active index change together. The parent-workflow checkpoint and procedure records are not rewritten by child archival.

Active goals remain bounded at 64. Child archives have separate bounds of 512 records, 128 MiB total and 8 MiB per payload. Prepared files count against the byte budget. Existing workflow archives retain their 256-record/128 MiB bounds. Active executable workflows 20, qualification supervisors 16, saved procedures 32 and versions 8 are unchanged. Capacity is explicit rather than unlimited.

These hashes detect changes relative to checked local records; they are not independent signatures. The edition does not add archive import, deletion, encryption or external trust. Retaining the surrounding source and artifact stores is necessary because archives preserve references as well as values.

## Old evidence remains old

Goal lifecycle changes affect runtime identity. A narrowly scoped Java adapter recognizes the exact published 107, 109 and 110 dependency sets for completed historical inspection. It checks that original numerical, source and browser-outcome verifier classes remain unchanged, then rechecks the original workflow, qualification lineage and publication. Unknown dependency sets and unsupported changes refuse.

Ordinary qualification against an old runtime still refuses. An explicit **upgrade summary qualification** request rechecks the original proofs, records the old record hash and both dependency sets, then executes all five fresh examples plus the selected fresh source. The new transition binds those observations to111. It never edits an old proof to claim it came from this runtime.

The chat card in the report screenshot retains the previous edition's task history. The new workflow is shown in the creation canvas; its live status and restart evidence come from the native workflow records. Some task captures retain the open History dialog while the native canvas preview overlaps it. Those captures show the actual session, not a separate on-screen refusal confirmation; the request outcome is established by the linked native records. The dedicated History captures show the archive controls and counts clearly.

The same distinction applies to implementation reuse. Known old implementation receipts can be inspected as completed history, but cannot authorize current execution or resume. The trial demonstrates this refusal. Fresh reuse of a current verified implementation succeeds with new input and all original source and visible-outcome checks.

Of344 compiled engine classes,337 are unchanged from110. Five changed classes own goal/workflow orchestration, server routes and explicit runtime identities; two new helpers implement archival and historical review. The source-identity receipt enumerates every comparison. Numerical verification, original data checks and both input-driven report resources remain byte-identical.

## Actual application study

The trial starts from a closed, byte-compared copy of 110's genuine 21-procedure project, with 62 native child goals and 51 retained workflows,42 already archived. It uses real Electron/Chromium checks, existing desktop native workflow controls and actual History buttons, not inserted successful audit receipts. User-private worlds remain untouched.

The initial chat attempt correctly refused the separate 24-task chat-history limit. Its recorder expected a new admission and timed out; the entire 121.319-second attempt and screenshot are retained. The corrected cohort starts from another exact copy, preserves that full journal byte-for-byte and sends structured requests through the existing Java workflow controls. It does not clear chat history, raise its limit or claim chat can continue on this full project. A separate admission check confirms that boundary.

A second68.082-second trial archived62 children, then stopped because the recorder compared the public compact display with a full payload. The recorder now compares the full child-read record and exact archive bytes, with separate display-field checks. That stopped trial, both checkpoints and all62 exact payloads are independently checked and retained; the full final cohort starts fresh. Runtime code is unchanged across these attempts.

1. Confirm that another request refuses at 62 goals under the existing reservation policy.
2. Verify and archive all 62 original children. Confirm the workflow checkpoint is unchanged; reopen and resolve every child.
3. Demonstrate old-runtime refusal, then archive the nine remaining completed workflows and explicitly upgrade qualification with five fresh cases and a selected report.
4. Use the visible History controls to archive the 12 new children and seven new workflow records. The project now retains 74 child archives and 58 workflow archives.
5. Begin a fresh native workflow request using archived current proofs. Interrupt Java before its selected-source independent audit; reopen and explicitly Resume. Child IDs and source/report bytes remain stable and the remaining real checks complete.
6. Reuse the archived current implementation with fresh data, then complete eight more requests. The bank reaches 32 genuine procedures, where the unchanged saved-procedure capacity refuses another publication. Only 20 active child goals remain.
7. In separate checked copies, refuse execution from the old implementation protocol, corrupt and remove an archived child file, revise an original report, and revise an original dataset. Restoring exact archived bytes restores its verification; revised original evidence remains invalid.

![Actual native report at the held audit boundary before process interruption](docs/readmes/111/recovered111-interrupted.png)

The 32 procedures belong to the same input-driven summary family, with distinct sources and verified publications. They are not 32 general reasoning algorithms. The measured requests use existing native Java workflow controls and make no model-planning or content-generation calls. The unchanged 24-task chat history is full, so this cohort does not claim new chat admission. Local model features remain available; this experiment does not measure their intelligence or train model weights.

## Measurements, with complete costs

| Child archive operation | Exact records | Active children | Caller seconds |
| --- | ---: | ---: | ---: |
{archive_table}

| Request | Outcome | Saved procedures | Active / archived children | Caller seconds |
| --- | --- | ---: | ---: | ---: |
{table}

Complete cohort: **{a['cohortWallMs']/1000:.3f}s**. Requests: **{a['requestMs']/1000:.3f}s**. Child archival: **{a['childArchiveMs']/1000:.3f}s**. Workflow archival: **{a['workflowArchiveMs']/1000:.3f}s**. Setup, checked copies, captures, inspection and between-request work: **{a['setupAndBetweenMs']/1000:.3f}s**. These categories are disjoint; preparation, failed requests and recovery costs remain included. The two prior stopped trials add **{a['priorAttempts']['totalMs']/1000:.3f}s**; all attempts together take **{a['totalAllAttemptMs']/1000:.3f}s**. Their costs and evidence are retained separately from the corrected cohort.

The observed process uses a 512 MiB Java heap and two Java processors, serial work and CPU-only planning settings. Cold sessions mean newly opened application processes on closed copies, not cleared OS caches. Timings describe this instrumented host and workload. They establish working archival and continuation; they do not establish a universal speedup or an isolated effect of archive size.

![Actual narrower History view showing archived children and the active limit](docs/readmes/111/archive111-1100.png)

## Verification and reproduction

All 150 relevant Java regression methods are covered and 148 desktop checks pass. The initial broad run passed 145 and found five edition107 upgrade failures. After adding that exact published identity to the explicit review path and restoring display punctuation, all 24 affected Java methods pass. The original failures, source correction and final rechecks are retained separately. The 11 new Java methods cover exact bytes, cold lookup, full original verification, changed source/report refusals, corrupt/missing/non-file substitution, stale plans, ownership, active/cancelled refusal, failed checkpoint recovery, current reuse and unchanged 64-goal admission. The capacity test creates real standalone goals through the API and confirms they remain active; it does not pad the measured application bank.

The 17-request native-workflow application study provides 11 completions, six required refusals and one abrupt recovery. A separate inspector decodes the original checked checkpoints, matches exact archived payloads, reconstructs every summary value and chart point from source CSV, checks qualification and publication lineage, verifies current dependency identities and accounts for every timing category. Twenty-four deliberately altered evidence records were rejected. The inspector initially needed its string canonicalization corrected to match the established JSON-java encoding of HTML and typographic characters in the untouched chat journal. The initial inspector, failure log and correction receipt are retained; application runtime and original evidence were unchanged.

Build with Java 21 and the repository Gradle wrapper: `gradlew.bat :workspace-engine:installDist`, then `npm start` in `workspace-desktop`. Use a distinct project directory for experiments; never start two Java owners against the same private data. Existing desktop dependencies are unchanged.

For the recorded study, commit the built runtime, stamp `node scripts/Study-Build095.cjs --stamp`, run `node workspace-desktop/test/archive111-integration.cjs`, then `node scripts/Inspect-GoalArchive111.cjs`. Supply a closed genuine110 project through `EDITION111_BANK110`; the recorder refuses an existing output directory. Verify the installed input/artifact stamp before and after measurement. The selected model must exist in the local catalogue, although these native requests do not invoke it.

[Lossless original evidence](docs/readmes/111/original-evidence.json), [source inventory](docs/readmes/111/file-inventory.json) and [implementation map](docs/readmes/111/system-map.json) accompany the measurements. Real dated screenshots and an offline guide checked at 1440 and 390 pixels are included. Frozen editions 001–110 remain unchanged. GitHub CI status is reported separately from these local results.

## Next steps and acceptance gates

First, give the separate 24-task chat journal an explicit evidence-preserving archival lifecycle. It is already full in this project. Keep its bound and task chains, outcomes, source references and recovery evidence intact, then demonstrate a fresh chat request after reopening. Child-goal archival alone does not solve that admission limit.

Measure cold reopen, history inspection, archive retrieval and repeated verification as the archive grows. Separate unavoidable original-source checks from repeated file reads, and retain all source and outcome validation before accepting an optimization. Include interrupted tasks and revised or missing evidence at each bank size.

The next capacity boundary is the 32-procedure bank. Define a separate, explicit saved-procedure lifecycle before expanding long sequences; do not silently evict procedures or raise limits. Later, extend archival to physical and typed-graph task families only when their original motion, forecast, ownership and outcome evidence can be checked in full. Preserve the existing integrated Java app throughout.

The destination remains an agent that can act, observe, verify, retain useful procedures and recover across sessions. This edition advances continuity by keeping evidence available while freeing active scheduling slots. It makes no claim of unlimited memory, consciousness or general self-training.
'''
write(r/'README-111.md',readme)
html=(r/'scripts/goalarchive111-guide.html').read_text(encoding='utf8')
for marker,value in [('__SYSTEM__',system),('__INVENTORY__',inventory),('__RESULTS__',a)]:html=html.replace(marker,json.dumps(value,ensure_ascii=False).replace('</','<\\/'))
write(d/'index.html',html)
status='''## Edition111 — validated child-goal archival

2026-11-10. Isolated edition111; parent706a015b. Actual74 exact child-goal archives and 58 workflow archives,11 fresh completions, six required refusals and one abrupt recovery. Genuine bank reaches32 procedures with 20 active children. Active64-goal limit unchanged; completed owned data/creation children only,512 archive records/128MiB. Exact payload files precede one atomic goals checkpoint commit; parent checkpoint unchanged. Explicit pinned107/109/110 historical inspection and fresh111 qualification; old implementation cannot authorize new execution.150 distinct Java methods covered;24 affected pass after five initial107 compatibility failures, preserved in originals.148 desktop and 24 altered-evidence controls pass. Actual History UI/screenshots and native workflow continuation; original24 chat journal full and byte-preserved. Initial121.319s chat-boundary and68.082s recorder-boundary trials retained; runtime unchanged between attempts. No raised limits or training claim. Preserve private worlds/frozen001–110; no delegation; heavy work serial. Publication pending actual push/fetch; CI separate. Next: evidence-preserving chat-task archival under its unchanged24 bound, archive retrieval costs and a separately defined procedure lifecycle.

'''
for name in ['AGENTS.md','README-CONTRIBUTORS.md','docs/PROJECT_MEMORY.md','docs/DEVELOPMENT_LOG.md','docs/ARCHITECTURE.md']:
 p=r/name;text=p.read_text(encoding='utf8')
 if not text.startswith('## Edition111 — validated child-goal archival'):write(p,status+text)
write(r/note,'# Edition111 — completed child-goal archives\n\n'+status+'[[README-111|Full edition]] · [[docs/readmes/111/index.html|Offline guide]] · [[docs/obsidian/118-verifiable-workflow-archives|Previous workflow archives]]\n\nExact child payloads remain linked to original parents, sources, reports and publications. Active64 remains unchanged. Next: measured retrieval and evidence verification costs.\n')
nodes=[{'id':'edition','type':'file','file':'README-111.md','x':0,'y':0,'width':430,'height':330},{'id':'archive','type':'file','file':'workspace-engine/src/main/java/fraymus/workspace/GoalArchive111.java','x':490,'y':0,'width':430,'height':330},{'id':'proof','type':'file','file':'docs/readmes/111/analysis.json','x':490,'y':400,'width':430,'height':330}];js(r/'docs/obsidian/119-completed-child-goal-archives.canvas',{'nodes':nodes,'edges':[{'id':'implements','fromNode':'edition','fromSide':'right','toNode':'archive','toSide':'left'},{'id':'measures','fromNode':'archive','fromSide':'bottom','toNode':'proof','toSide':'top'}]})
write(r/'README.md','''# Fraynix Agent Workspace

Edition111 archives completed child goals while preserving exact original evidence. Java verifies parent ownership, sources and outcomes before archival and reuse. **The 64-active-goal limit remains unchanged.**

[Edition111: functions, evidence and next steps](README-111.md) · [Interactive offline guide](docs/readmes/111/index.html) · [Previous edition110](README-110.md) · [Contributor direction](README-CONTRIBUTORS.md)

![Actual child-goal History controls](docs/readmes/111/current111-children-1600.png)

Use **History** to preview, verify and archive completed data/creation children, inspect retained originals and recheck evidence. The actual app retained 74 archived children and 58 archived workflows, completed 11 fresh native workflow requests and recovered an abruptly interrupted task. It reached 32 genuine saved procedures with 20 active child goals. Six requests correctly refused capacity or invalid evidence/runtime identity. 150 distinct Java checks are covered, with 24 final affected rechecks after a retained compatibility correction. 148 desktop checks and 24 altered-evidence controls passed.

Java owns durable tasks, tools, worlds, memory, verification and recovery. Chat, browser/canvas, spatial views and Game Lab remain integrated. Active, incomplete, standalone, physical and graph children remain in their existing lifecycle. Old receipts retain their original runtime identity; fresh execution needs current verification.

The separate 24-task chat history remains full and unchanged: this study continues through existing native workflow controls, not fresh chat admission.

**Next:** archive completed chat-task history with its evidence, then measure retrieval costs and define a saved-procedure lifecycle. No raised limits or model-training claim.
''')
for name,line in [('docs/readmes/INDEX.md','[Edition111 — completed child-goal archives](../../README-111.md) · [Offline guide](111/index.html)'),('docs/obsidian/00-home.md','[[docs/obsidian/119-completed-child-goal-archives|Edition111: completed child-goal archives]]')]:
 p=r/name;text=p.read_text(encoding='utf8')
 if line not in text:write(p,line+'\n\n'+text)
print(json.dumps({'passed':True,'originalFiles':len(originals),'canonicalSources':len(inventory),'javaMethods':150,'actualRequests':17}))
