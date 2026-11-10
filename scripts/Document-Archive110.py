"""Publish only independently inspected, actual edition110 results."""
from pathlib import Path
import json,hashlib,gzip,subprocess,shutil,xml.etree.ElementTree as ET
r=Path(__file__).resolve().parent.parent;d=r/'docs/readmes/110'
def load(p):return json.loads((r/p).read_text(encoding='utf-8-sig'))
def write(p,s):p.parent.mkdir(parents=True,exist_ok=True);p.write_text(s,encoding='utf8',newline='\n')
def js(p,v):write(p,json.dumps(v,ensure_ascii=False,indent=2)+'\n')
def sha(b):return hashlib.sha256(b).hexdigest()
assert subprocess.run(['git','show-ref','--verify','--quiet','refs/tags/readme-110'],cwd=r).returncode==1
a=load('build/analysis110.json');trial=load('build/archive110/result.json');visual=load('build/visual-review110.json');identity=load('build/source-identity110.json')
assert a['passed'] and len(a['refusals'])==20 and trial['passed'] and visual['passed'] and identity['passed']
tests=[]
for f in sorted((r/'workspace-engine/build/test-results/test').glob('TEST-*.xml')):
 e=ET.parse(f).getroot();assert all(e.attrib[k]=='0' for k in ['failures','errors','skipped']);tests.append(dict(e.attrib))
assert sum(int(t['tests']) for t in tests)==134
for log in ['desktop110.log','desktop110-additional.log']:assert 'fail 0' in (r/'build'/log).read_text(encoding='utf-8-sig')
d.mkdir(parents=True,exist_ok=True);js(d/'analysis.json',a)
js(d/'verification.json',{'passed':True,'java':tests,'javaMethods':134,'desktopChecks':148,'originalIntegrityControls':a['refusals'],'build':load('build/study095-build.json'),'unchangedDependencies':identity,'actualStudy':{'requests':11,'completed':6,'refused':5,'abruptRecoveries':1,'archivedRecords':42,'retainedProcedures':21},'visual':visual,'parentCI':load('build/ci110-parent.json'),'scope':'Actual source/outcome checks preserved. Exact record archival is separately verified from continued original evidence validity. Child goals stay in place; no raised runtime limits.'})
captions={}
for p in sorted((r/'build/archive110').glob('*.png')):shutil.copyfile(p,d/p.name);captions[p.name]='Actual edition110 app capture: '+p.stem
paths=[]
for root in ['build/archive110','workspace-engine/build/test-results/test']:
 for p in (r/root).rglob('*'):
  if p.is_file() and not any(x in {'desktop-profile','runtime-042','.northstar-kernel','binary'} for x in p.parts) and not p.name.endswith('.lock'):paths.append(p)
for name in ['java110.log','java110-archive.log','java110-command.log','compile110.log','desktop110.log','desktop110-additional.log','source-identity110.json','ci110-parent.json','study095-build.json','analysis110.json','archive110.log','inspect110.log','visual-review110.json']:
 paths.append(r/'build'/name)
originals=[]
for p in sorted(set(paths)):
 raw=p.read_bytes();compressed=gzip.compress(raw,mtime=0);target=d/'original'/(sha(raw)+'.gz');target.parent.mkdir(exist_ok=True)
 if not target.exists():target.write_bytes(compressed)
 originals.append({'logicalPath':p.relative_to(r).as_posix(),'path':target.relative_to(r).as_posix(),'bytes':len(compressed),'sha256':sha(compressed),'rawBytes':len(raw),'rawSha256':sha(raw)})
js(d/'original-evidence.json',{'files':originals,'scope':'Actual isolated closed109 history, exact archive payloads/checkpoints, native originals, all fresh browser outcomes, recovery and refusals with full costs. Private live worlds excluded.'})
kinds={v['path']:v['hashKind'] for v in load('docs/readmes/109/file-inventory.json')}
for p in ['docs/EDITION_110_CONTRACT.md','workspace-desktop/test/archive110-integration.cjs','workspace-engine/src/test/java/fraymus/workspace/Archive110Test.java','scripts/Inspect-Archive110.cjs','scripts/Document-Archive110.py','scripts/archive110-guide.html']:kinds[p]='utf8-lf'
inventory=[]
for p,kind in sorted(kinds.items()):
 b=(r/p).read_bytes();b=b.decode('utf8').replace('\r\n','\n').encode() if kind=='utf8-lf' else b;inventory.append({'path':p,'bytes':len(b),'sha256':sha(b),'hashKind':kind})
js(d/'file-inventory.json',inventory)
note='docs/obsidian/118-verifiable-workflow-archives.md'
modules=[('archive','Atomic archival and original verification','workspace-engine/src/main/java/fraymus/workspace/WorkflowRunner.java'),('checkpoint','Checked atomic checkpoint generations','fraynix-modules/factory-runtime/src/main/java/fraymus/factory/WorldCheckpointStore.java'),('api','Owned Java archive endpoints','workspace-engine/src/main/java/fraymus/workspace/WorkspaceServer.java'),('view','History preview, archive and recheck','workspace-desktop/renderer/workflow-panel.js'),('goals','Unchanged original goal and procedure store','workspace-engine/src/main/java/fraymus/workspace/GoalRunner.java'),('transition','Explicit runtime qualification transition','workspace-engine/src/main/java/fraymus/workspace/Qualification108.java'),('audit','Original source and actual outcome verifier','workspace-engine/src/main/java/fraymus/workspace/DataSummary102.java'),('study','Actual archive, reuse and recovery study','workspace-desktop/test/archive110-integration.cjs'),('inspection','Independent archive and outcome reconstruction','scripts/Inspect-Archive110.cjs')]
system={'modules':[{'id':i,'name':n,'source':p,'note':note} for i,n,p in modules],'screenshots':[{'path':'docs/readmes/110/'+p,'caption':caption} for p,caption in captions.items()],'evidence':[{'path':'docs/readmes/110/'+p,'caption':p} for p in ['analysis.json','verification.json','original-evidence.json']]};js(d/'system-map.json',system)
table='\n'.join('| '+v['name']+' | '+v['outcome']+' | '+str(v['bankAfter'])+' | '+str(v['goalsAfter'])+' | '+f"{v['totalMs']/1000:.3f}"+' |' for v in a['results'])
archive_table='\n'.join('| '+v['name']+' | '+str(v['records'])+' | '+str(v['beforeActive'])+' → '+str(v['afterActive'])+' | '+f"{v['totalMs']/1000:.3f}"+' |' for v in a['archives'])
readme=f'''# Fraynix Agent Workspace — edition 110

## Archive completed work without losing its evidence

Edition 110 adds an explicit Java-owned archival lifecycle for completed data/creation workflows and qualification supervisors. Original record bytes stay intact under SHA-256 addresses. One atomic checkpoint update replaces active records with references. The agent can still retrieve those records by the same IDs and recheck their sources, owned stages, outcomes and saved-procedure publication.

The actual application archived **42 original records**, continued from edition 109's full bank, retained **21 genuine procedures**, completed **six fresh requests** and recovered one abruptly interrupted task. Five requests correctly refused changed runtime identity, insufficient capacity or changed original evidence. The remaining child-goal limit is visible and unchanged.

[Previous edition 109](README-109.md) | [Interactive offline guide](docs/readmes/110/index.html) | [Measurements](docs/readmes/110/analysis.json) | [Validation](docs/readmes/110/verification.json) | [Contributor direction](README-CONTRIBUTORS.md)

![Actual History panel with retained archived originals](docs/readmes/110/current110-archived-1600.png)

## Why this belongs in the Java brain

Fraynix remains a local agent workspace in which Java owns durable goals, actions, physics and world state, procedural memory, verification and recovery. The desktop presents chat, browser and creation surfaces, spatial views and Game Lab. A tool performs an action; a skill completes a task through actions, checks and recovery. Memory must retain what actually happened and the conditions under which a procedure worked.

Edition 109 found a concrete bottleneck: five retained qualification examples and fifteen selected-source workflows filled the twenty-workflow history before reaching the thirty-two saved-procedure slots. Removing completed records would have broken the references that made saved procedures verifiable. Raising the bound would have postponed the problem while retaining more active history. Edition 110 gives completed records an explicit storage lifecycle while preserving their identity and evidence.

This is implemented archival, not a memory summary or model-training operation. The exact original record is retained, including its frozen source binding, stage ownership, actual browser audit, report reference, qualification dependencies, recorded costs and saved-skill linkage. The archive does not invent new evidence or turn an older runtime proof into a newer one.

## What changes in the app

Use **History** in the main toolbar. It shows active workflow and supervisor counts, archived records and the retained child-goal count. Refresh the preview, then choose **Verify & archive completed data workflows**. Java rechecks the selected originals before committing the archive. The preview is bound to the current history; a changed preview refuses and must be refreshed.

Choose a retained record to inspect its original identity, source, report, saved skill and stage references. **Recheck original evidence** performs fresh checks against the current original stores. This is verification of retained observations, not a new camera capture or browser observation. The current workflow list contains active records; archived records remain available through History and stable-ID reads.

Active, interrupted, cancelled and unpublished work cannot be archived. This phase supports data/creation workflows and qualification supervisors, whose original visible outcomes can be reconstructed through the existing verifier. Physics workflow and typed-graph histories remain in active storage. The UI states this scope; no unsupported archive action is presented as working.

![Actual archival of the original full edition109 bank](docs/readmes/110/original109-archived-1600.png)

## The storage transaction

```mermaid
flowchart LR
  A[Reviewed completed records] --> B[Verify original sources and outcomes]
  B --> C[Force exact payload bytes to hashed files]
  C --> D[One atomic workflow checkpoint update]
  D --> E[Stable IDs resolve archived originals]
  E --> F[Fresh verification before reuse]
```

Each record's exact stored UTF-8 payload is written beneath `workflows/archive110/`. Its filename is the SHA-256 digest of those bytes. The existing checked workflow checkpoint retains the record ID, original creation time, archive schema, byte length, digest and archival timestamp. There is no separate mutable archive index to reconcile.

All archive files are written, forced and checked before the checkpoint changes. Until that commit, the original active records remain authoritative. A failed checkpoint write leaves those records active; prepared archive bytes remain available for a later checked retry. After a successful commit, reopening resolves the same original records. No original source, artifact, child goal or saved procedure is removed.

Reads validate the bounded reference, file type, exact length, hash, record ID, original creation time and completed state. Missing, changed or substituted bytes refuse. There is no fallback that silently replaces missing archived evidence with a different proof. Archived records cannot be edited or resumed as active work. New work produces a new verified workflow.

These hashes detect changes relative to the checked local references; they are not signatures from an independent authority. The existing checkpoint format retains its own checked prior generation. Private project ownership and backups remain important. This edition does not add archive import, remote trust, encryption or deletion.

## Every original check still applies

An unchanged archive file does not guarantee that its referenced evidence is still valid. A user can later revise a dataset or report in its original store. Accordingly, archive admission and subsequent reuse still check original source mapping and versions, goal ownership, numerical results, every required displayed value and chart point, qualification lineage and saved-procedure publication.

The test deliberately changed an archived example's report and separately revised its original dataset without changing the CSV. Both requests refused. Separate same-length corruption and missing-file controls refused archive reads and verification; restoring the exact original bytes restored verification. No planner retry converted those refusals into success.

Java's workflow implementation changes in this edition, so old qualification identity remains old. The study first demonstrates that ordinary reuse refuses an edition109 qualification. It then archives the original full bank and explicitly asks to **upgrade summary qualification**. The existing transition rechecks original evidence and runs all five fresh cases plus the selected source. The original runtime receipts remain intact and linked; they are never relabelled.

The data producer, numerical/browser verifier, goal runner, report resources and qualification-transition implementation remain unchanged. The source-identity receipt records exact compared class hashes. Runtime policy changes are confined to workflow storage/scheduling and the archive endpoints; the desktop adds History access.

## Actual end-to-end study

The study begins with a closed, byte-compared copy of edition109's genuinely grown fifteen-procedure bank. It retains all fifteen source-to-report publications, five original qualification cases and fifteen supervisors. It does not pad the bank or inject successful browser observations.

1. Ordinary qualification refuses the changed runtime identity. An explicit upgrade also refuses while the original active history is full.
2. Archive the thirty-five original records. Confirm the same saved procedures and forty child goals remain; close and reopen the project.
3. Explicitly upgrade qualification with five fresh examples and a new selected-source report. Real Chromium checks every required outcome before saving the sixteenth procedure.
4. Use the actual History button to archive the seven newly completed workflow records. Keep all forty-two archived originals and fifty-two child goals.
5. Begin another chat task using archived qualification. Hold the selected-source audit before it runs, abruptly terminate Java, reopen, explicitly Resume and complete the remaining real checks. Original child IDs and source/report bytes remain stable.
6. Reuse the archived verified implementation on new input, then complete three more qualification requests with fresh sources. Every successful request keeps all original checks.
7. Confirm the remaining child-goal capacity refuses the next request. Test changed archived sources in separate closed copies.

![Actual request interrupted before its independent audit](docs/readmes/110/recovered110-interrupted.png)

The successful collection reaches twenty-one procedures. They belong to the same input-driven summary family with distinct actual sources and publications; they are not twenty-one general reasoning algorithms. These recognized native chat workflows make zero model-planning or content-generation calls. Other local-model features remain available, but this study does not evaluate their intelligence or train model weights.

## Measured costs and the remaining limit

| Archive operation | Exact records | Active executable workflows | Caller seconds |
| --- | ---: | ---: | ---: |
{archive_table}

| Request | Outcome | Saved procedures afterward | Retained child goals | Caller seconds |
| --- | --- | ---: | ---: | ---: |
{table}

Complete cohort: **{a['cohortWallMs']/1000:.3f}s**. Requests: **{a['requestMs']/1000:.3f}s**. Archive operations: **{a['archiveMs']/1000:.3f}s**. Setup, checked copies, inspection, captures and between-request work: **{a['setupAndBetweenMs']/1000:.3f}s**. Preparation, refusal and recovery costs are retained. The categories are disjoint; repeated verifications outside the archive call remain in setup/between time.

The study uses a measured 512 MiB Java heap and two available Java processors, serial work and CPU-only planning settings. Cold sessions mean new application processes on closed project copies, not cleared operating-system caches. Timings describe this instrumented host and these tasks. They establish working archival and continuation, not a universal speedup or isolated causal effect of archive size.

Original bounds remain: twenty active executable workflows, sixteen active qualification supervisors, sixty-four child goals, thirty-two procedures, eight versions per procedure, a retained1200-second qualification budget and three resumes. Archive storage has separate bounds of256 records,128MiB total and8MiB per record. Existing unsupported or incomplete records still consume their original active slots.

At sixty-two child goals, admission correctly refuses another workflow because the existing check reserves room for three goals. This workload normally executes two. Archival has released workflow slots, but it deliberately has not erased or migrated the original child-goal evidence. The app displays that distinction.

![Actual narrower History view showing the remaining goal limit](docs/readmes/110/archive110-1100.png)

## Validation and reproducibility

The 134 Java regression methods cover archive lifecycle and corruption plus existing workflow, qualification, recovery, ownership, creation, numerical and typed-graph behavior. The 148 desktop checks pass. The actual study supplies real Electron/Chromium outcome checks, UI archival and inspection, eleven chat requests, six completions, five required refusals and one abrupt recovery. Independent inspection reconstructs all source values and charts, exact archive bytes and original checkpoint records; twenty deliberately altered evidence controls must fail.

The archive tests force a checkpoint-write failure after preparing files, verify that every original active payload remains, reopen and retry. They also test stale previews, duplicate/foreign IDs, unsupported arguments, incomplete work, immutable archived controls, fresh implementation reuse, changed sources and missing/corrupt archives. The initial compile typo and correction are retained in the build logs; passing checks refer to the final implementation.

Use Java21, the repository Gradle wrapper and existing desktop dependencies. Build `gradlew.bat :workspace-engine:installDist`, then `npm start` in `workspace-desktop`. Use a distinct project for experiments; do not open two Java owners on the same private data. The app provides History after its normal workspace has loaded.

For this isolated study, build and commit the source, stamp `node scripts/Study-Build095.cjs --stamp`, then run `node workspace-desktop/test/archive110-integration.cjs` and `node scripts/Inspect-Archive110.cjs`. Set `EDITION110_BANK109` to a closed genuine edition109 bank15 copy. The recorder refuses an existing output directory so failed evidence is not overwritten. The installed model catalogue must contain the selected model, but these native requests perform no inference.

[Lossless originals](docs/readmes/110/original-evidence.json), [source inventory](docs/readmes/110/file-inventory.json) and [implementation map](docs/readmes/110/system-map.json) accompany this edition. App captures are real and dated by the recorder; the offline guide is checked at desktop and narrow widths. Frozen editions001–109 and private live worlds remain preserved. GitHub CI is reported separately from local validation.

## Next: the child-goal lifecycle

Extend archival to eligible completed child goals without breaking stable workflow ownership, saved-procedure references or runtime identity. Make that a separately versioned transaction with original-source checks, crash/restart tests, corruption refusals and measured retrieval costs. The current64-goal bound should stay until that evidence-preserving lifecycle works.

Then measure how archive verification scales, decide whether selective loading earns its cost, and broaden archival to physical and graph task families only when their original outcome checks remain complete. The destination is a continuing Java agent that can act, check, remember and recover without forgetting what made its memories trustworthy.
'''
write(r/'README-110.md',readme)
html=(r/'scripts/archive110-guide.html').read_text(encoding='utf8')
for marker,value in [('__SYSTEM__',system),('__INVENTORY__',inventory),('__RESULTS__',a)]:html=html.replace(marker,json.dumps(value,ensure_ascii=False).replace('</','<\\/'))
write(d/'index.html',html)
status=f'''## Edition110 — validated workflow archival

2026-11-10. Isolated edition110; parent fc3b27b3. Actual35 old +7 new exact record archives, six fresh completions, five required refusals and one abrupt recovery;21 genuine saved procedures. Original child goals retained; admission refuses at62 because three slots are reserved under64. Active workflow20/supervisor16 unchanged; archive256/128MiB. Exact blobs prepared before one atomic checkpoint pointer commit. Original source/outcome/publication checks retained.134 Java,148 desktop and20 altered-evidence controls pass. Actual History UI and screenshots; no goal archival, general memory expansion or training claim. Preserve private worlds and frozen001–109; no delegation; heavy work serial. Publication pending actual push/fetch; CI separate. Next: evidence-preserving child-goal archival with explicit identity and recovery checks.

'''
for name in ['AGENTS.md','README-CONTRIBUTORS.md','docs/PROJECT_MEMORY.md','docs/DEVELOPMENT_LOG.md','docs/ARCHITECTURE.md']:
 p=r/name;text=p.read_text(encoding='utf8')
 if not text.startswith('## Edition110 — validated workflow archival'):write(p,status+text)
write(r/note,'# Edition110 — verifiable workflow archives\n\n'+status+'[[README-110|Full edition]] · [[docs/readmes/110/index.html|Offline guide]] · [[docs/obsidian/117-maintenance-at-capacity|Previous capacity measurements]]\n\nExact archived payloads stay linked to original data, goals, reports, qualification and procedure publication. Only completed data/creation and qualification records leave active scheduling. Next: child-goal lifecycle, with all evidence checks.\n')
nodes=[{'id':'edition','type':'file','file':'README-110.md','x':0,'y':0,'width':430,'height':330},{'id':'archive','type':'file','file':'workspace-engine/src/main/java/fraymus/workspace/WorkflowRunner.java','x':490,'y':0,'width':430,'height':330},{'id':'proof','type':'file','file':'docs/readmes/110/analysis.json','x':490,'y':400,'width':430,'height':330}];js(r/'docs/obsidian/118-verifiable-workflow-archives.canvas',{'nodes':nodes,'edges':[{'id':'implements','fromNode':'edition','fromSide':'right','toNode':'archive','toSide':'left'},{'id':'measures','fromNode':'archive','fromSide':'bottom','toNode':'proof','toSide':'top'}]})
write(r/'README.md','''# Fraynix Agent Workspace

Edition 110 archives completed workflow history while keeping its original evidence verifiable. Java retains exact records, checks their sources and outcomes before reuse, and frees active workflow slots without deleting child goals or saved procedures.

[Edition 110: functions, measurements and next steps](README-110.md) | [Interactive offline guide](docs/readmes/110/index.html) | [Previous edition 109](README-109.md) | [Contributor direction](README-CONTRIBUTORS.md)

![Actual History view with 42 archived records](docs/readmes/110/current110-archived-1600.png)

Use **History** to preview completed work, verify and archive it, inspect original records, and recheck their evidence. The actual study archived 42 records, completed six fresh requests, recovered an abruptly interrupted task and retained 21 procedures. Five requests correctly refused invalid evidence or insufficient capacity. All 134 Java tests, 148 desktop checks and 20 altered-evidence controls passed.

Java owns durable goals, tools, worlds, verification, recovery and memory. Chat, browser/canvas, spatial views and Game Lab remain integrated. This release archives completed data/creation workflows and qualification supervisors; other workflow families and child goals remain in their original stores.

**Next:** preserve completed child-goal evidence outside active scheduling. The unchanged 64-goal limit remains visible; this edition does not claim unlimited memory or model training.
''')
for name,line in [('docs/readmes/INDEX.md','[Edition 110 — verifiable workflow archives](../../README-110.md) · [Offline guide](110/index.html)'),('docs/obsidian/00-home.md','[[docs/obsidian/118-verifiable-workflow-archives|Edition110: verifiable workflow archives]]')]:
 p=r/name;text=p.read_text(encoding='utf8')
 if line not in text:write(p,line+'\n\n'+text)
print(json.dumps({'passed':True,'originalFiles':len(originals),'canonicalSources':len(inventory),'javaMethods':134,'actualRequests':11}))
