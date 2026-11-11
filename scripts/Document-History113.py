"""Document the actual measured application study, preserving original evidence."""
from pathlib import Path
import json,hashlib,gzip,subprocess,shutil,re
r=Path(__file__).resolve().parent.parent;d=r/'docs/readmes/113'
def load(p):return json.loads((r/p).read_text(encoding='utf-8-sig'))
def write(p,s):p.parent.mkdir(parents=True,exist_ok=True);p.write_text(s,encoding='utf8',newline='\n')
def js(p,v):write(p,json.dumps(v,ensure_ascii=False,indent=2)+'\n')
def sha(b):return hashlib.sha256(b).hexdigest()
assert subprocess.run(['git','show-ref','--verify','--quiet','refs/tags/readme-113'],cwd=r).returncode==1
a=load('build/analysis113.json');trial=load('build/history113/result.json');visual=load('build/visual-review113.json');identity=load('build/source-identity113.json')
assert a['passed'] and len(a['refusals'])==20 and trial['passed'] and visual['passed'] and identity['passed']
for name in ['desktop113-final.log','desktop113-additional.log']:assert 'fail 0' in (r/'build'/name).read_text(encoding='utf-8-sig')
d.mkdir(parents=True,exist_ok=True);js(d/'analysis.json',a)
js(d/'verification.json',{'passed':True,'desktopChecks':165,'historyFocusedChecks':17,'originalIntegrityControls':a['refusals'],'javaFreshBuild':'workspace-engine:installDist passed','javaFreshUnitTests':0,'javaPrior112Methods':127,'unchangedDependencies':identity,'cohortBuild':load('build/study095-build-cohort113.json'),'finalBuild':load('build/study095-build.json'),'finalUI':load('build/history113-final-ui/result.json'),'visual':visual,'parentCI':load('build/ci113-parent.json'),'actualStudy':{'completedChats':8,'qualificationEvidenceRechecks':40,'checkedDataCreationWorkflowReferences':48,'newSelectedDataWorkflows':8,'abruptRecoveries':1,'bankBefore':24,'bankAfter':32},'corrections':{'initialFocused':'10/11 passed; original wording assertion retained; outcome wording clarified','initialDesktop':'153/154 passed; startup-integrity fixture omitted new renderer files; fixture expanded, guard unchanged'},'scope':'Fresh165 desktop checks, actual Electron/Java growth/recovery and independent original reconstruction. All347 production classes and54 HTML/JS/JSON resources byte-identical to112. Prior127 Java tests are not relabelled as a new run. No changed physics, new model inference, weight training or speedup claim.'})
captions={}
for folder in ['build/history113','build/review113','build/history113-final-ui']:
 for p in sorted((r/folder).glob('*.png')):shutil.copyfile(p,d/p.name);captions[p.name]='Actual app capture: '+p.stem
paths=[]
for root in ['build/history113','build/review113','build/history113-final-ui']:
 for p in (r/root).rglob('*'):
  if p.is_file() and not any(x in {'desktop-profile','runtime-042','.northstar-kernel','binary'} for x in p.parts) and not p.name.endswith('.lock'):paths.append(p)
for name in ['compile113.log','desktop113-final.log','history113-late-error-before.log','history113-final-ui.log','doc-check113-initial.log','study095-build-cohort113.json','desktop113.log','desktop113-initial.log','desktop113-additional.log','history113-tests-initial.log','history113-tests.log','history113-ui.log','source-identity113.json','ci113-parent.json','study095-build.json','analysis113.json','history113.log','inspect113.log','visual-review113.json']:paths.append(r/'build'/name)
originals=[]
for p in sorted(set(paths)):
 raw=p.read_bytes();compressed=gzip.compress(raw,mtime=0);target=d/'original'/(sha(raw)+'.gz');target.parent.mkdir(exist_ok=True)
 if not target.exists():target.write_bytes(compressed)
 originals.append({'logicalPath':p.relative_to(r).as_posix(),'path':target.relative_to(r).as_posix(),'bytes':len(compressed),'sha256':sha(compressed),'rawBytes':len(raw),'rawSha256':sha(raw)})
js(d/'original-evidence.json',{'files':originals,'scope':'Exact closed112 test-project copy, original history records, actual113 sources/browser outcomes/archive/recovery/control projects, full timings and initial failed fixture checks. Private running worlds excluded.'})
note='docs/obsidian/121-usable-experience-history.md'
modules=[('view','Unified history view','workspace-desktop/renderer/history-panel113.mjs'),('model','Read-only filtering and linked IDs','workspace-desktop/renderer/history-model113.mjs'),('result','Original evidence and exact saved result','workspace-desktop/history113.cjs'),('java','Unchanged Java chat authority','workspace-engine/src/main/java/fraymus/workspace/Agency043.java'),('workflow','Unchanged Java workflow verifier','workspace-engine/src/main/java/fraymus/workspace/WorkflowRunner.java'),('study','Actual growing-bank study','workspace-desktop/test/history113-integration.cjs'),('inspection','Independent source and outcome reconstruction','scripts/Inspect-History113.cjs')]
system={'modules':[{'id':i,'name':n,'source':p,'note':note} for i,n,p in modules],'screenshots':[{'path':'docs/readmes/113/'+p,'caption':caption} for p,caption in captions.items()],'evidence':[{'path':'docs/readmes/113/'+p,'caption':p} for p in ['analysis.json','verification.json','original-evidence.json']]};js(d/'system-map.json',system)
table='\n'.join('| '+x['name']+' | '+str(x['rows'])+' | '+str(x['bankBefore'])+' → '+str(x['bankAfter'])+' | '+f"{x['totalMs']/1000:.3f}"+' |' for x in a['results'])
cold='\n'.join('| '+x['label']+' | '+str(x['repeat']+1)+' | '+f"{x['startupMs']/1000:.3f}"+' | '+f"{x['previewMs']/1000:.3f}"+' | '+f"{sum(o['verifyMs'] for o in x['operations'])/1000:.3f}"+' |' for x in a['cold'])
readme=f'''# Fraynix Agent Workspace — edition 113

## Find the agent's experience, inspect the original, open the checked result

Edition113 brings retained chats, workflows and child steps into **one searchable History view**. It shows the original request and outcome, follows explicit connected-work IDs, exposes full original records and offers a fresh evidence recheck. **Open checked result** verifies the original evidence and exact saved report version and contents before displaying it in the existing canvas.

The actual app completed eight fresh chat requests, retained their verified procedures and grew the genuine saved bank **24 → 32**. One request survived an abrupt Java interruption and explicit same-ID recovery. Final retained history contains **31 archived chats,78 workflows and94 steps**, plus the same three cancelled active chats. All capacities and Java verification rules remain unchanged.

[Previous edition112](README-112.md) · [Offline interactive guide](docs/readmes/113/index.html) · [Full review and development plan](docs/APP_REVIEW_113.md) · [Measured results](docs/readmes/113/analysis.json) · [Validation](docs/readmes/113/verification.json) · [Contributor direction](README-CONTRIBUTORS.md)

![Searchable original history in the actual app](docs/readmes/113/bank32-chat31-1-history1600.png)

## What Fraynix is becoming

The useful foundation is a local runtime that connects requests, observations, actions, verification, memory and recovery. Java owns durable task execution and evidence. The desktop presents browser/canvas content, chat, world views, project objects and tools. Existing physics and observation paths can support bounded experiments and persistent object identities. Reusable procedures connect actions to success checks and recovery.

This is substantial infrastructure, with real task execution and retained outcomes. It is still a bounded engineering workspace. Neither Java itself nor an accumulated log establishes general intelligence, consciousness or weight training. The eight113 requests used the existing deterministic Java summary/qualification implementation with **zero model calls**. A local model is selectable in the app, but these results do not claim fresh LLM reasoning.

The review found a usability gap: the previous three history selectors hid useful work behind long lists and rawJSON. This edition makes that memory inspectable without adding a second memory store or turning old receipts into new authority. Broader tasks and real-model evaluation should follow this usability work.

## Use it step by step

1. Start the workspace using its existing launch scripts and a deliberately selected project. Keep private projects separate from test copies. Java21 and the existing desktop dependencies are required; this phase adds no new dependency.
2. Click **History** in the main toolbar. Counts show each active limit and archive separately.
3. Search by request, title, ID or outcome. Filter **Chats**, **Workflows** or **Steps**, and use **Needs attention** for cancelled/interrupted records. Lists display12 entries per page.
4. Select a record. Read its stored outcome, source/report references and recent events. Expand **Full original record** for all original fields and the complete journal.
5. Follow **Connected work** to the exact workflow/step IDs, then use **Back to related record**. Navigation does not start or resume execution.
6. Click **Recheck original evidence**. The new receipt is displayed separately from the old outcome. A failed check shows its reason; an unverified conversation remains unverified.
7. Use **Open checked result** for supported completed workflow reports. The host rechecks Java evidence and matches the retained artifact ID, version and content hash before opening the canvas.
8. Expand **Preserve completed work and free active slots** when needed. Each category has an explicit archive button and eligible count. Interrupted, cancelled and unsupported records stay in their original history. Resume remains the existing explicit task action.

![The same history view at1100 pixels](docs/readmes/113/bank32-chat31-history1100.png)

## What changed and what remains authoritative

The new desktop component reads the existing Java history/read/verify/archive endpoints. Search, filters, pagination and related links are presentation logic. It does not invent eligibility or outcome status. Late reads cannot replace a newer selection; closed/reopened dialogs reject stale responses. Original text is rendered as text rather than executable markup.

The saved-result operation accepts only a record type and ID. It rejects unfinished or unverified chat results, unsupported reports, foreign verification IDs, changed versions and changed contents. Browser fixture checks exercise these boundary cases; the actual app study separately changes originals and records real refusals. A successful recheck is not permission to replay actions.

All **347 installed production Java class files** and **54 HTML/JS/JSON resources** are byte-identical to112. The source/outcome checks, execution ownership, recovery protocol and archive formats are unchanged. Active limits remain chat24/queue4, workflows20, qualification coordinators16, child goals64 and saved procedures32. Existing archive limits remain in force. Pagination bounds visible rows; it does not turn the underlying bounded history endpoint into a streaming database.

## Actual requests and complete cost

The trial copied the closed112 final test project exactly. It first archived the remaining completed native records through the actual UI. Eight new visible-chat requests each rechecked the input-driven summary against five retained original synthetic examples, analyzed the selected freshCSV, checked the actual browser output and saved the procedure. Selected inputs contain3–109 rows, including flat and signed/reversing sequences. These are declared software tests, not new camera or physical measurements.

| Request | Selected rows | Saved bank | Request seconds |
| --- | ---: | --- | ---: |
{table}

Request5 was interrupted at the actual selected-data browser boundary after five qualification cases. The Java process was terminated, the same project reopened and the same task explicitly resumed. Workflow/child IDs and original report contents remained unchanged. Its recovery cost is included.

**Whole trial: {a['cohortMs']/1000:.3f}s.** Request intervals total{a['requestMs']/1000:.3f}s; explicit archive intervals total{a['archiveMs']/1000:.3f}s; six cold UI/history measurement intervals total{a['coldMs']/1000:.3f}s; copying, source/control checks and remaining work total{a['otherMs']/1000:.3f}s. These non-overlapping top-level categories sum to the complete trial. Per-request browser/recovery/coordination partitions are retained in the evidence.

## Retrieval as the real bank grows

At bank24/28/32 and archived-chat counts23/27/31, two repetitions reopen the actual app and reverse category order. Each searches/selects a chat, workflow and step, performs a fresh Java recheck, follows connected IDs and opens the original checked report. Actual filters, pagination and1600/1100-pixel layouts are exercised.

| Bank / chat archives | Repeat | App readiness / s | History preview / s | Three fresh checks / s |
| --- | ---: | ---: | ---: | ---: |
{cold}

These are caller-observed timings including transport/UI overhead. A cold application process does not clear the operating-system cache. Two repetitions and a changing bank do not establish a scaling law or causal speedup. The new UI makes existing experience easier to use; verification still performs real work. The five qualification examples were retained originals rechecked on each request, not40 newly executed examples. The eight saved procedures reuse the same qualified implementation with separate verified source/outcome lineage; this is not eight new skill families.

![Exact saved report opened after original evidence checks](docs/readmes/113/bank32-chat31-1-result1600.png)

## Tests and limits of the claims

**165 desktop checks pass**, including17 targeted history/host/browser-component checks. The first focused run passed10/11; its wording assertion led to a clearer outcome sentence. The first broad run passed153/154; the existing startup-integrity test fixture was missing the three new renderer files. The fixture now includes them and still checks invalidUTF-8/NUL refusal. Both initial logs are retained. Final review also caught a delayed failed recheck overwriting status after a dialog close/reopen. A sixth component test reproduced it before the fix; the final155-test desktop run plus10 additional checks passes. The view now rejects late failures by operation generation. A separate actual Electron/Java check reproduced the real changed-report refusal, closed/reopened during a second recheck, then verified and opened an unchanged report. Its original records and separate cost are retained; the eight-request study used the earlier113 desktop response handling with identical Java classes and evidence checks.

Fresh113 Java installation passed; all compiled production classes/resources matched112. The prior112 suite had127 relevant Java methods; those are explicitly prior evidence, not a relabelled fresh113 test run. The actual113 application study uses the newly installed identical Java engine.

Independent inspection reconstructs originalCSV arithmetic, normalized inputs, every displayed summary/point, behavior checks, qualification cases, retained procedures, journals, archive identity, recovery and complete costs. **20 altered-evidence controls** must be refused. Live corruption and missing-chat-snapshot controls refuse reading, verification and opening. A revised original report refuses opening. Adding dataset version2 preserves the immutable version1 actually used by the original proof; this is checked explicitly.

The new result button supports retained completed workflow reports. It does not promise to open every historic artifact type, replay a task, manage saved-procedure retirement or create a new skill family. The GameLab, webcam, physical controllers and local-model reasoning were not freshly evaluated in113. Remote CI status is reported separately from passed local checks.

## Next development gates

1. **114 — explicit saved-procedure lifecycle.** Keep32 active slots; retire eligible procedures with original provenance/descendants retained and recheck compatibility before deliberate reactivation. Test a full bank, crash boundaries and corrupt/missing ancestors.
2. **115 — another complete useful skill.** Proposed: compare two data sources, retain unit/time conventions, create an interactive difference report and independently verify every value. Test fresh inputs and restart before saving.
3. **116 — real-model selection and recovery.** Evaluate supported and unsupported requests using an installed local model against simple routing; preserve failed drafts and total costs.
4. **117 — calibrated observation to world to artifact.** Bind a measured sequence to a persistent object, predict bounded motion, compare outcomes and retain the checked procedure. Add physics/perception only where that task reveals a measurable gap.

[The full review](docs/APP_REVIEW_113.md) explains the strengths, current limits, design priorities and acceptance criteria. The aim is coherent useful agency: a tool acts; a skill completes a task through actions, checks and recovery.

## Reproduce and audit

The actual recorder is [history113-integration.cjs](workspace-desktop/test/history113-integration.cjs). It requires a closed genuine112 project, the installed113 engine, the existing desktop dependencies and an available local model catalog; it deliberately makes no model inference calls. `EDITION113_BANK112` selects that closed source, and `EDITION113_OUTPUT` selects a new output directory. Existing output is refused so prior trials cannot be overwritten. Use the documented build stamp before running; heavy validation is serial.

The [independent inspector](scripts/Inspect-History113.cjs), [source inventory](docs/readmes/113/file-inventory.json), [lossless original evidence index](docs/readmes/113/original-evidence.json), [contract](docs/EDITION_113_CONTRACT.md) and [offline guide](docs/readmes/113/index.html) retain the detailed evidence. Original test projects are included as content-addressed compressed originals; private running projects are excluded. Frozen001–112 remain intact. Publication is only confirmed after an actual push and fetched Git-tree audit.
'''
write(r/'README-113.md',readme);write(r/'README.md',readme)
captures=[{'file':'bank32-chat31-1-history1600.png','label':'Unified retained history ·1600px'},{'file':'bank32-chat31-history1100.png','label':'Same interface ·1100px'},{'file':'bank32-chat31-1-result1600.png','label':'Checked original result in canvas'},{'file':'changed-artifact-1600.png','label':'Revised original report is refused'},{'file':'before-history-1600.png','label':'Actual112 baseline: three independent selectors'}]
guide=(r/'scripts/history113-guide.html').read_text(encoding='utf8').replace('__ANALYSIS__',json.dumps(a,ensure_ascii=False)).replace('__MODULES__',json.dumps(system['modules'])).replace('__CAPTURES__',json.dumps(captures));write(d/'index.html',guide)
status=f'''## Edition113 — validated unified experience history

2026-11-10. Isolated edition113; parent0e02172b. Searchable chats/workflows/steps, original outcomes, explicit related IDs and checked exact report opening. Actual8 fresh requests,40 retained qualification-evidence rechecks and8 new selected-data workflows, one abrupt same-ID recovery; genuine bank24→32. Final31 chat/78 workflow/94 child archives plus3 cancelled chats. Active limits unchanged.165 desktop and20 altered-original checks pass; original fixture failures retained. Fresh Java install passed; all347 production classes and54 resources byte-identical to112. Prior127 Java tests remain prior evidence. Six cold UI/recheck repetitions retain complete costs; final late-error fix and separate actual Java close/reopen check retained; no general speedup or model-training claim. Actual app/screenshots and baseline reviewed; review/roadmap in docs/APP_REVIEW_113.md. Preserve private worlds/frozen001–112; no delegation; heavy work serial. Publication pending actual push/fetch; CI separate. Next: explicit saved-procedure lifecycle with original provenance and unchanged32 active slots.

'''
for name in ['AGENTS.md','README-CONTRIBUTORS.md','docs/PROJECT_MEMORY.md','docs/DEVELOPMENT_LOG.md','docs/ARCHITECTURE.md']:
 p=r/name;old=p.read_text(encoding='utf8');old=re.sub(r'\A## Edition113 — validated unified experience history\n\n.*?\n\n','',old,count=1,flags=re.S);write(p,status+old)
write(r/note,'# Edition113 — usable experience history\n\n'+status+'\n[[README-113.md|Complete edition]] · [[docs/APP_REVIEW_113.md|Review and next gates]] · [[docs/readmes/113/analysis.json|Measured results]]\n')
js(r/'docs/obsidian/121-usable-experience-history.canvas',{'nodes':[{'id':m['id'],'type':'file','file':m['source'],'x':i%3*440,'y':i//3*240,'width':400,'height':180} for i,m in enumerate(system['modules'])],'edges':[{'id':'view-result','fromNode':'view','fromSide':'right','toNode':'result','toSide':'left','label':'original evidence before open'},{'id':'result-java','fromNode':'result','fromSide':'bottom','toNode':'java','toSide':'top','label':'existing authority'}]})
for name,entry in [('docs/readmes/INDEX.md','\n- [113 — usable experience history](../../README-113.md) · [guide](113/index.html)\n'),('docs/obsidian/00-home.md','\n- [[docs/obsidian/121-usable-experience-history.md|113 — usable experience history]]\n')]:
 p=r/name;write(p,p.read_text(encoding='utf8').replace(entry,'')+entry)
kinds={v['path']:v['hashKind'] for v in load('docs/readmes/112/file-inventory.json')}
for p in ['docs/EDITION_113_CONTRACT.md','docs/APP_REVIEW_113.md','workspace-desktop/history113.cjs','workspace-desktop/renderer/history-panel113.mjs','workspace-desktop/renderer/history-model113.mjs','workspace-desktop/renderer/history113.css','workspace-desktop/test/history113-integration.cjs','workspace-desktop/test/history113-final-integration.cjs','workspace-desktop/test/history113.test.cjs','workspace-desktop/test/history113-ui.test.cjs','scripts/Inspect-History113.cjs','scripts/Document-History113.py','scripts/history113-guide.html']:kinds[p]='utf8-lf'
inventory=[]
for p,kind in sorted(kinds.items()):
 b=(r/p).read_bytes();b=b.decode('utf8').replace('\r\n','\n').encode() if kind=='utf8-lf' else b;inventory.append({'path':p,'bytes':len(b),'sha256':sha(b),'hashKind':kind})
js(d/'file-inventory.json',inventory);guide=guide.replace('</html>','<script id="system-data" type="application/json">'+json.dumps(system,ensure_ascii=False).replace('<',chr(92)+'u003c')+'</script><script id="inventory-data" type="application/json">'+json.dumps(inventory).replace('<',chr(92)+'u003c')+'</script></html>');write(d/'index.html',guide);print(json.dumps({'documented':113,'originalEntries':len(originals),'canonicalFiles':len(inventory),'screenshots':len(captions)}))
