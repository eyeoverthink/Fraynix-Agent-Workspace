"""Publish the actual independently inspected chat-history trial; never invent results."""
from pathlib import Path
import json,hashlib,gzip,subprocess,shutil,xml.etree.ElementTree as ET
r=Path(__file__).resolve().parent.parent;d=r/'docs/readmes/112'
def load(p):return json.loads((r/p).read_text(encoding='utf-8-sig'))
def write(p,s):p.parent.mkdir(parents=True,exist_ok=True);p.write_text(s,encoding='utf8',newline='\n')
def js(p,v):write(p,json.dumps(v,ensure_ascii=False,indent=2)+'\n')
def sha(b):return hashlib.sha256(b).hexdigest()
assert subprocess.run(['git','show-ref','--verify','--quiet','refs/tags/readme-112'],cwd=r).returncode==1
a=load('build/analysis112.json');trial=load('build/archive112/result.json');visual=load('build/visual-review112.json');identity=load('build/source-identity112.json')
assert a['passed'] and len(a['refusals'])==20 and trial['passed'] and visual['passed'] and identity['passed']
tests=[]
for f in sorted((r/'workspace-engine/build/test-results/test').glob('TEST-*.xml')):
 e=ET.parse(f).getroot();assert all(e.attrib[k]=='0' for k in ['failures','errors','skipped']);tests.append(dict(e.attrib))
methods=sum(int(t['tests']) for t in tests);assert len(tests)>=17 and methods>=100
for log in ['desktop112.log','desktop112-additional.log']:assert 'fail 0' in (r/'build'/log).read_text(encoding='utf-8-sig')
d.mkdir(parents=True,exist_ok=True);js(d/'analysis.json',a)
js(d/'verification.json',{'passed':True,'java':tests,'javaMethods':methods,'desktopChecks':148,'originalIntegrityControls':a['refusals'],'initialCorrection':load('build/correction112.json'),'build':load('build/study095-build.json'),'unchangedDependencies':identity,'actualStudy':{'completedChats':2,'archivedChats':23,'retainedCancelledChats':3,'activeLimit':24,'abruptRecoveries':1},'visual':visual,'parentCI':load('build/ci112-parent.json'),'scope':'Exact chat checkpoint snapshots, full original journals and current evidence checks. No raised limits, source deletion, proof relabelling, model-inference or training claim.'})
captions={}
for p in sorted((r/'build/archive112').glob('*.png')):shutil.copyfile(p,d/p.name);captions[p.name]='Actual edition112 app capture: '+p.stem
paths=[]
for root in ['build/archive112','workspace-engine/build/test-results/test','build/java112-initial-results','build/before-correction112']:
 for p in (r/root).rglob('*'):
  if p.is_file() and not any(x in {'desktop-profile','runtime-042','.northstar-kernel','binary'} for x in p.parts) and not p.name.endswith('.lock'):paths.append(p)
for name in ['java112-initial.log','java112-final.log','correction112.json','compile112.log','desktop112.log','desktop112-additional.log','source-identity112.json','ci112-parent.json','study095-build.json','analysis112.json','archive112.log','inspect112.log','visual-review112.json']:paths.append(r/'build'/name)
originals=[]
for p in sorted(set(paths)):
 raw=p.read_bytes();compressed=gzip.compress(raw,mtime=0);target=d/'original'/(sha(raw)+'.gz');target.parent.mkdir(exist_ok=True)
 if not target.exists():target.write_bytes(compressed)
 originals.append({'logicalPath':p.relative_to(r).as_posix(),'path':target.relative_to(r).as_posix(),'bytes':len(compressed),'sha256':sha(compressed),'rawBytes':len(raw),'rawSha256':sha(raw)})
js(d/'original-evidence.json',{'files':originals,'scope':'Closed genuine111 test-project copies, exact before/after chat checkpoints, original archive blobs, all browser outcomes, recovery and refusals. Private running review worlds excluded. Initial failed regression and correction retained.'})
kinds={v['path']:v['hashKind'] for v in load('docs/readmes/111/file-inventory.json')}
for p in ['docs/EDITION_112_CONTRACT.md','workspace-desktop/test/archive112-integration.cjs','workspace-engine/src/test/java/fraymus/workspace/ChatArchive112Test.java','workspace-engine/src/main/java/fraymus/workspace/ChatArchive112.java','scripts/Inspect-ChatArchive112.cjs','scripts/Document-ChatArchive112.py','scripts/chatarchive112-guide.html']:kinds[p]='utf8-lf'
inventory=[]
for p,kind in sorted(kinds.items()):
 b=(r/p).read_bytes();b=b.decode('utf8').replace('\r\n','\n').encode() if kind=='utf8-lf' else b;inventory.append({'path':p,'bytes':len(b),'sha256':sha(b),'hashKind':kind})
js(d/'file-inventory.json',inventory)
note='docs/obsidian/120-completed-chat-history.md'
modules=[('archive','Exact checked chat snapshots','workspace-engine/src/main/java/fraymus/workspace/ChatArchive112.java'),('agency','Chat admission, action authority and recovery','workspace-engine/src/main/java/fraymus/workspace/Agency043.java'),('api','Owned Java history endpoints','workspace-engine/src/main/java/fraymus/workspace/WorkspaceServer.java'),('view','History controls and retained originals','workspace-desktop/renderer/workflow-panel.js'),('workflow','Unchanged original workflow verifier','workspace-engine/src/main/java/fraymus/workspace/WorkflowRunner.java'),('study','Actual chat archival and recovery study','workspace-desktop/test/archive112-integration.cjs'),('inspection','Independent source/outcome and archive reconstruction','scripts/Inspect-ChatArchive112.cjs')]
system={'modules':[{'id':i,'name':n,'source':p,'note':note} for i,n,p in modules],'screenshots':[{'path':'docs/readmes/112/'+p,'caption':caption} for p,caption in captions.items()],'evidence':[{'path':'docs/readmes/112/'+p,'caption':p} for p in ['analysis.json','verification.json','original-evidence.json']]};js(d/'system-map.json',system)
table='\n'.join('| '+x['name']+' | '+str(x['bankBefore'])+' → '+str(x['bankAfter'])+' | '+f"{x['totalMs']/1000:.3f}"+' |' for x in a['results'])
archive_table='\n'.join('| '+x['name']+' | '+str(x['records'])+' | '+str(x['before'])+' → '+str(x['after'])+' | '+f"{x['totalMs']/1000:.3f}"+' |' for x in a['archives'])
readme=f'''# Fraynix Agent Workspace — edition 112

## Completed chat history remains available; new work can continue

Edition 112 adds explicit archival for completed chat tasks. Java preserves the **exact original checked checkpoint**, every task's complete action journal, its outcome and its links to the original evidence. An atomic checkpoint change releases eligible completed tasks from active scheduling. Their IDs continue to resolve after restart.

**The 24-active-chat-task limit is unchanged.** The actual trial began with 24 real tasks, archived 21 completed chats and retained all three cancelled tasks. Two fresh requests then completed through the visible chat interface, including one recovery after an abrupt Java interruption. Archiving those two brought the retained archive to **23 chats**, with the same three cancelled tasks still active. The separate 64-goal limit and all other execution budgets remain unchanged.

[Previous edition 111](README-111.md) · [Offline interactive guide](docs/readmes/112/index.html) · [Measured results](docs/readmes/112/analysis.json) · [Validation](docs/readmes/112/verification.json) · [Contributor direction](README-CONTRIBUTORS.md)

![Actual completed-chat archive controls](docs/readmes/112/original111-chats-1600.png)

## Why this change matters

Fraynix keeps the environment, goals, tools, verification, memory and recovery in its Java runtime. Chat is connected to those functions: a supported request can start a native workflow, retain its source bindings, create a report, check the actual browser result and save a procedure. The desktop presents that work alongside browser, canvas, spatial views and Game Lab.

Edition 111 freed completed child-goal slots, but exposed a separate boundary. Its original project already held 24 chat tasks. The next chat request correctly refused admission even though native workflow capacity was available. Deleting the journal or raising the limit would lose the intended bounded, evidence-preserving lifecycle.

This edition preserves that complete history and frees only eligible completed chat slots. It does not convert old receipts into current execution authority, change model weights or silently repeat a prior action.

## Use the History window

1. Open **History** in the main toolbar. The completed-chat section appears first, with active and archived counts.
2. Use **Refresh chat preview** to see which completed tasks can be checked.
3. Select **Verify & archive completed chat tasks**. Java verifies the original journal, closed episode and supported outcome before committing the archive.
4. Choose a retained task to **Inspect original chat** or **Recheck chat evidence**. The original record remains readable by its stable ID.
5. Close History and start a new chat request. Interrupted tasks keep their existing explicit Resume behavior.

Cancelled, interrupted, queued and running tasks are not archived. Supported outcomes are completed data/creation workflows and qualifications, workbench artifacts/recipes, native visual inspection reports and completed unverified conversations. Other native task families remain active until their complete archival checks are defined. A revised or missing original refuses verification; its historical task remains available for inspection.

An unverified conversation retains the explicit `completed-unverified` outcome. Successful archive integrity checks do not promote it to verified task success.

## Exact snapshots and atomic commitment

```mermaid
flowchart LR
  A[Preview eligible completed chats] --> B[Recheck journal and original evidence]
  B --> C[Force exact original checkpoint bytes]
  C --> D[Atomically save archive references and remaining tasks]
  D --> E[New chats use the unchanged 24-slot admission limit]
  D --> F[Stable IDs read and recheck archived originals]
```

Snapshots live under `agency-043/archive112/`, addressed by the SHA-256 of the exact original `tasks.json` bytes. Several tasks archived together share that single original snapshot. Each reference binds the task ID, creation time, original outcome, snapshot length/hash, task index, complete task hash and verified evidence hash. The original checkpoint envelope and every retained task journal are checked when read.

The archive file is written and forced before the active checkpoint changes. Only after the atomic checkpoint save succeeds does the live scheduler adopt the remaining task list. Prepared files without a committed reference leave tasks active and are available for checked retry. The unit recovery check exercises this preparation-without-commit boundary; the app study separately exercises an abrupt interruption during a real workflow.

Storage is bounded at **512 archived task references**, **128 MiB of snapshot files**, and **4 MB per original checkpoint**. Prepared files count toward that byte budget. The existing active checkpoint size budget, 24-task admission limit and four-task queue remain in place. The archive is not unlimited, and no automatic eviction is introduced.

A history operation rereads and hashes each distinct snapshot once during that operation, then checks each referenced task individually. It does not reuse a timestamp-based validity claim between operations. This avoids repeatedly parsing one shared file while preserving a fresh check on the next operation.

## Evidence remains separate from authority

Archived tasks cannot resume, cancel, acquire host claims or receive stale acknowledgements. A separate new request is required to execute work. Existing unfinished tasks keep their original state and recovery controls.

Java checks the original objective and hash-linked journal, closing outcome and learning episode. It rechecks stored screenshots against their original metadata and bytes. Workbench reports must still match their exact artifact version, contents and witnessed test receipt; requested recipe bindings must remain unchanged. Workflow chats invoke the existing original-source, owned-stage, outcome and publication verifier, then compare the retained completion binding. Rechecking an old report is not a fresh observation of the real world.

All **342 other existing production classes** remain byte-identical to edition 111. Only the existing Agency043 and WorkspaceServer classes changed, alongside the new archive helper. Original goal/workflow/qualification/numerical verifiers, historical runtime identities and summary resources remain unchanged. No qualification proof is relabelled.

## Actual application trial

The trial uses a closed exact copy of edition 111's genuine project, retaining its 22 published procedures, 74 archived child goals, 58 archived workflows and all 24 chat records. The original running review projects and private camera data are not used or changed.

The first new chat request correctly refused the 24-task limit. The actual History controls then archived the 21 completed chats, preserving the exact checked snapshot and the three cancelled records. A cold reopen recovered all archived IDs and full journals. The next two requests were submitted through the visible chat field and Start button:

> Qualify the input-driven summary, analyze the selected data, and save workflow as fresh112.

The second request was interrupted immediately before the selected-source browser audit, after its five examples had been checked. After restart, the same chat and native workflow were resumed explicitly. Original child IDs, report contents and source bindings were retained. Both requests completed with fresh browser and independent numeric/geometry checks, growing the procedure bank from 22 to 24.

![Actual fresh chat completion and report](docs/readmes/112/fresh112-1600.png)

![Actual current-chat archive after recovery](docs/readmes/112/current112-chats-1600.png)

The local model remains selectable, but these supported summary requests follow the existing deterministic Java route and invoke **zero model-planning calls**. The result demonstrates actual chat-to-workflow continuity, not a new measure of LLM intelligence or training.

Separate copies test same-length snapshot corruption, a missing snapshot, a revised original report and a revised source dataset. Reads or verification refuse as appropriate; restoring the exact snapshot restores verification. Historical task journals remain unchanged. Archived Resume and Cancel requests refuse. No passing browser observations are supplied by the recorder.

## Complete measured costs

| Archive operation | Chats archived | Active tasks | Caller seconds |
| --- | ---: | ---: | ---: |
{archive_table}

| Chat request | Saved procedures | Caller seconds |
| --- | ---: | ---: |
{table}

The complete cohort took **{a['cohortMs']/1000:.3f} seconds**: requests **{a['requestMs']/1000:.3f} s**, archival **{a['archiveMs']/1000:.3f} s**, and setup, copies, restart preparation, rechecks, refusals, screenshots and other work **{a['otherMs']/1000:.3f} s**. Request measurements include their actual browser checks and recovery pauses. Categories do not overlap. These measurements describe this host and workload; they do not establish a universal speedup.

The Java process uses a 512 MiB heap and two reported processors. Heavy work is serial. Cold reopen means a new app process on a closed project, not a cleared operating-system cache.

![Actual narrower History window](docs/readmes/112/chat-history112-1100.png)

## Tests and reproduction

**{methods} relevant Java tests**, **148 desktop checks** and **20 independent altered-evidence controls** pass. The first 36-test run passed 35 and exposed a missing-test-receipt exception when the original report was revised. The code now returns an explicit refusal; the test was not weakened. Its original XML/log/source and the final corrected results are retained.

Tests cover exact snapshots, cold lookup, full journals, preserved unverified outcomes, the unchanged 24-task boundary, unchanged cancelled/interrupted records, explicit recovery, stale previews, duplicate/foreign IDs, missing/corrupt/non-file archives, revised reports and screenshots, prepared-but-uncommitted snapshots and refusal of archived action authority. The independent inspector reconstructs actual summary values and chart points from original CSV, verifies archive envelopes, journals, evidence bindings, cold reads and recovery identity, and accounts for the entire cohort cost.

Build the Java runtime with Java 21 and `gradlew.bat :workspace-engine:installDist`; start `npm start` in `workspace-desktop`. Use a distinct project directory for experiments. Never open two Java owners against the same project or replace the installed JARs of a running private review app.

For this study, commit the built inputs, run `node scripts/Study-Build095.cjs --stamp`, then `node workspace-desktop/test/archive112-integration.cjs` and `node scripts/Inspect-ChatArchive112.cjs`. Set `EDITION112_BANK111` to a closed genuine edition111 project; choose a fresh output directory with `EDITION112_OUTPUT` for every rerun. The recorder refuses overwriting an earlier trial. The guide is checked at 1440 and 390 pixels; application captures use 1600 and 1100 pixel windows.

[Lossless originals](docs/readmes/112/original-evidence.json), [source inventory](docs/readmes/112/file-inventory.json) and [implementation map](docs/readmes/112/system-map.json) accompany this edition. Earlier editions 001–111 remain frozen. GitHub CI is reported separately from local tests and fetched publication verification.

## Next steps

Measure cold-open, archive lookup and repeated evidence verification costs as chat, workflow and child-goal histories grow together. Keep the original checks and account for complete task cost before optimizing repeated reads. Test mixed interrupted/completed histories and revised evidence at each size.

The saved-procedure bank still has its separate 32-procedure limit. Define a deliberate, evidence-preserving procedure lifecycle before expanding long-running workloads. Extend chat archival to other native task families only after their original motion, ownership, source and outcome checks have equivalent coverage.

The destination remains a local agent that can act, observe, verify, retain useful procedures and recover across sessions. This edition removes a demonstrated continuity limit without deleting the evidence that makes the system accountable.
'''
write(r/'README-112.md',readme)
html=(r/'scripts/chatarchive112-guide.html').read_text(encoding='utf8')
for marker,value in [('__SYSTEM__',system),('__INVENTORY__',inventory),('__RESULTS__',a)]:html=html.replace(marker,json.dumps(value,ensure_ascii=False).replace('</','<\\/'))
write(d/'index.html',html)
status=f'''## Edition112 — validated chat archival

2026-11-10. Isolated edition112; parent35d31790. Actual closed111 history24:21 completed chats archived, three cancelled retained unchanged. Two fresh visible-chat requests completed, one abrupt recovery, two more chats archived;23 archived/3 active, active24 limit unchanged, bank22→24. Exact original checked checkpoint bytes + full original journals + evidence bindings preserved. Prepared files precede one atomic active-checkpoint save; archived tasks read-only, source/outcome checks fresh.512 archive refs/128MiB/4MB blobs; other limits unchanged. Existing342 class files and original goal/workflow/qualification/verifier identities unchanged. Only Agency043/WorkspaceServer changed plus new helper. {methods} final Java,148 desktop and20 altered-evidence checks pass. Initial35/36 test result retained; explicit revised-report refusal corrected, no weaker check. Actual screenshots/guide, original bytes, complete costs, source inventory and logs retained. Preserve private Java33224/37676 and frozen001–111; no delegation; heavy work serial. Publication pending actual push/fetch; CI separate. Next: growing archive retrieval/evidence costs and separately defined saved-procedure lifecycle.

'''
for n in ['AGENTS.md','README-CONTRIBUTORS.md','docs/PROJECT_MEMORY.md','docs/DEVELOPMENT_LOG.md','docs/ARCHITECTURE.md']:
 p=r/n;s=p.read_text(encoding='utf8')
 if not s.startswith('## Edition112 — validated chat archival'):write(p,status+s)
write(r/note,'# Edition112 — completed chat history\n\n'+status+'[[README-112|Full edition]] · [[docs/readmes/112/index.html|Offline guide]] · [[docs/obsidian/119-completed-child-goal-archives|Previous child-goal archives]]\n')
nodes=[{'id':'edition','type':'file','file':'README-112.md','x':0,'y':0,'width':430,'height':330},{'id':'archive','type':'file','file':'workspace-engine/src/main/java/fraymus/workspace/ChatArchive112.java','x':490,'y':0,'width':430,'height':330},{'id':'proof','type':'file','file':'docs/readmes/112/analysis.json','x':490,'y':400,'width':430,'height':330}];js(r/'docs/obsidian/120-completed-chat-history.canvas',{'nodes':nodes,'edges':[{'id':'implements','fromNode':'edition','fromSide':'right','toNode':'archive','toSide':'left'},{'id':'measures','fromNode':'archive','fromSide':'bottom','toNode':'proof','toSide':'top'}]})
write(r/'README.md',f'''# Fraynix Agent Workspace

Edition112 preserves completed chat-task history and frees eligible completed-task slots. **The 24-active-chat-task limit remains unchanged.** Exact checked snapshots retain full journals, outcomes and links to original evidence.

[Edition112: functions, evidence and next steps](README-112.md) · [Interactive offline guide](docs/readmes/112/index.html) · [Previous edition111](README-111.md) · [Contributor direction](README-CONTRIBUTORS.md)

![Actual completed-chat History controls](docs/readmes/112/original111-chats-1600.png)

Use **History** to preview, verify and archive completed chats, inspect original records and recheck evidence. The actual app retained23 archived chats and three unchanged cancelled tasks. Two fresh chat requests completed, including one abrupt recovery. Revised sources and corrupt/missing snapshots refused. {methods} relevant Java tests,148 desktop checks and20 independent altered-evidence controls passed; initial failures are retained with their correction.

Java owns tasks, worlds, tools, verification, memory and recovery. Browser, canvas, spatial views and Game Lab remain integrated. Archived tasks are read-only; unfinished tasks retain explicit recovery. Unverified outcomes stay unverified. All original goal/workflow/qualification verifiers remain unchanged; no raised limits or model-training claim.

**Next:** measure archive retrieval and verification costs as histories grow together, then define the separate saved-procedure lifecycle.
''')
for n,line in [('docs/readmes/INDEX.md','[Edition112 — completed chat history](../../README-112.md) · [Offline guide](112/index.html)'),('docs/obsidian/00-home.md','[[docs/obsidian/120-completed-chat-history|Edition112: completed chat history]]')]:
 p=r/n;s=p.read_text(encoding='utf8')
 if line not in s:write(p,line+'\n\n'+s)
print(json.dumps({'passed':True,'originalFiles':len(originals),'canonicalSources':len(inventory),'javaMethods':methods,'actualChatCompletions':2}))
