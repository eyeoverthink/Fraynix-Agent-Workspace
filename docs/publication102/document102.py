from pathlib import Path
import json,hashlib,gzip,subprocess,shutil,xml.etree.ElementTree as ET,datetime
r=Path.cwd();d=r/'docs/readmes/102'
def load(p):return json.loads(p.read_text(encoding='utf-8-sig'))
def write(p,s):p.parent.mkdir(parents=True,exist_ok=True);p.write_text(s,encoding='utf8',newline='\n')
def js(p,v):write(p,json.dumps(v,indent=2,ensure_ascii=False)+'\n')
def sha(b):return hashlib.sha256(b).hexdigest()
assert subprocess.run(['git','show-ref','--verify','--quiet','refs/tags/readme-102']).returncode==1
base=load(r/'build/analysis102-baseline.json');summary=load(r/'build/analysis102-summary.json');run=load(r/'build/summary102-attempt3/result-mixed.json');exhausted=load(r/'build/analysis102-exhausted.json');review=load(r/'build/review102/result.json');renderer=load(r/'build/renderer102-diagnostics/result.json');oldrenderer=load(r/'build/renderer101-diagnostics/result.json');comparison=load(r/'build/source-comparison102.json');ci=load(r/'build/ci102-status.json')
assert all(v['passed'] for v in [base,summary,review,renderer,oldrenderer,comparison,exhausted])
assert run['outcome']=='mixed-verified-and-exhausted' and run['firstTaskVerified'] and run['savedProcedureCount']==1
domFailure=load(r/'build/analysis102-dom-failure.json');diagnostics=load(r/'build/diagnostics102-final/result.json');assert domFailure['passed'] and diagnostics['passed'];assert len(diagnostics['results'])==7 and len(renderer['results'])==11 and len(oldrenderer['results'])==5;assert [len(v['refusals']) for v in [base,summary,exhausted,domFailure]]==[15,18,6,7]
classes=[]
for p in sorted((r/'workspace-engine/build/test-results/test').glob('TEST-*.xml')):
 x=ET.parse(p).getroot();classes.append({k:x.attrib[k] for k in ['name','tests','failures','errors','skipped','time']})
assert sum(int(c['tests']) for c in classes)==80 and not sum(int(c['failures'])+int(c['errors']) for c in classes)
for name,count in [('desktop102-diagnostics.log',135),('desktop102-new-final.log',10)]:
 text=(r/'build'/name).read_text(encoding='utf-8-sig');assert f'pass {count}' in text and 'fail 0' in text
results=base['results']+[exhausted['result'],domFailure['result']]+summary['results']
for t in results:
 t['drafts']=t['cost']['drafts'];t.setdefault('outcome','verified')
 if t['outcome']=='verified' and t['input'] in ['recorded52-restart','summary16-reuse']:t['outcome']='verified / assisted'
 elif t['outcome']=='verified' and t['cost']['resumes']:t['outcome']='verified / resumed'
analysis={'passed':True,'results':results,'baseline':base,'summary':summary,'exhausted':exhausted,'domFailure':domFailure,'scope':'Seven measured requests: four eventually verified, three summaries exhausted without new skills. The final changed-source summary reuse failed despite three resumes and assistance. Second baseline required explicit developer-assisted correction. Recorded and synthetic inputs are distinct. No matched performance advantage is inferred.'}
cohorts=[]
for label,file in [('Explorers','build/cost102-baseline-attempt2/result-recovered.json'),('Exhausted summary','build/summary102-attempt1/result-exhausted.json'),('Missing-element exhausted summary','build/summary102-attempt2/result-exhausted.json'),('Verified summary and refused reuse','build/summary102-attempt3/result-mixed.json')]:
 c=load(r/file);elapsed=round((datetime.datetime.fromisoformat(c['finishedAt'].replace('Z','+00:00'))-datetime.datetime.fromisoformat(c['startedAt'].replace('Z','+00:00'))).total_seconds()*1000);tasks=sum(t['end']-t['start'] for t in c['tasks']);assert elapsed>=tasks;cohorts.append({'cohort':label,'elapsedMs':elapsed,'requestIntervalsMs':tasks,'outsideRequestIntervalsMs':elapsed-tasks,'finalReopen':c.get('finalReopen'),'scope':'Initial harness start through final persisted checkpoint, before teardown. Outside request intervals include startup/import, intertask work and cold reopen; no inferred pure compute duration.'})
analysis['cohorts']=cohorts
assert load(r/'build/retry102-source-comparison.json')['passed']
verification={'passed':True,'source':run['source'],'lastHarnessSource':run.get('resumeSource',run['source']),'finalRuntimeReceiptSource':load(r/'build/summary102-final-build.json')['source'],'java':classes,'existingDesktop':135,'additionalDesktop':10,'actualBrowserFixtures':{'legacy':5,'summary':11,'diagnostics':7},'independentRefusals':{'baseline':len(base['refusals']),'summary':len(summary['refusals']),'exhausted':len(exhausted['refusals']),'domFailure':len(domFailure['refusals'])},'retrySourceComparison':load(r/'build/retry102-source-comparison.json'),'diagnosticSourceComparison':load(r/'build/diagnostic102-source-comparison.json'),'sourceComparison':comparison,'presentationReview':review,'parentCI':ci,'scope':'Focused tests and actual local-model workflows. Unit and renderer fixtures are distinct from model output. Initial genuine failure and assisted recovery remain explicit.'}
js(d/'verification.json',verification);js(d/'analysis.json',analysis);js(r/'build/analysis102.json',analysis)
archive=[];unique={}
def pack(p,label):
 raw=p.read_bytes();h=sha(raw)
 if h not in unique:
  b=gzip.compress(raw,mtime=0);dest=d/'original'/(h+'.gz');dest.parent.mkdir(parents=True,exist_ok=True);dest.write_bytes(b);unique[h]={'path':dest.relative_to(r).as_posix(),'bytes':len(b),'sha256':sha(b),'rawBytes':len(raw),'rawSha256':h}
 archive.append(dict(unique[h],logicalPath=label))
for folder in ['cost102-baseline','cost102-baseline-attempt2','baseline102-failure','summary102-attempt1','summary102-attempt2','summary102-attempt2-failure','summary102-attempt2-budget2','summary102-attempt3','summary102-attempt3-failure','summary102-attempt3-budget2','summary102-resume-harness-failure','summary102-reuse-failure','summary102-reuse-budget2','summary102-reuse-budget3','java102-before-formula-results','summary102-failure','summary102-audit-failure','summary102-repair-budget','renderer101','renderer102','renderer102-final','diagnostics102','diagnostics102-final','renderer102-diagnostics','renderer101-diagnostics','review102','review-baseline102','java102-before-contract-results','java102-before-retry-results','java102-retry-targeted-results']:
 for p in (r/'build'/folder).rglob('*'):
  if p.is_file() and not any(k in p.parts for k in ['desktop-profile','runtime-042','.northstar-kernel']) and p.name!='host.json' and p.suffix in ['.json','.jsonl','.fworld','.pending','.html','.css','.js','.csv','.log','.txt','.png','.xml']:pack(p,folder+'/'+p.relative_to(r/'build'/folder).as_posix())
for p in (r/'build').glob('*102*'):
 if p.is_file() and p.suffix in ['.json','.log','.py','.cjs']:pack(p,'validation/'+p.name)
for p in (r/'workspace-engine/build/test-results/test').glob('TEST-*.xml'):pack(p,'java/'+p.name)
js(d/'original-evidence.json',{'files':archive,'distinctNewPayloads':len(unique),'scope':'Lossless isolated test originals, generated code, durable jobs, failures, timings and screenshots. Private live data, credentials, profiles and weights excluded.'})
captions={'summary102-1600.png':'Actual 49-row recorded motion summary, generated by the local model and independently verified.','summary102-1100.png':'Same completed report at 1100-pixel width, alongside chat and task controls.','refused102-1600.png':'Refused reuse on16 synthetic samples: wrong travel0.160000m instead of0.310000m, three drafts exhausted. Original verified skill retained.','inputs102-1600.png':'The existing input chooser prepares either supported output without sending.'}
for c in captions:shutil.copyfile(r/'build/review102'/c,d/c)
shutil.copyfile(r/'build/review-baseline102/repaired102-1600.png',d/'repaired102-1600.png');captions['repaired102-1600.png']='Actual 96-row synthetic explorer after deliberately altered display evidence was rejected and repaired.'
kinds={x['path']:x['hashKind'] for x in load(r/'docs/readmes/101/file-inventory.json')}
for p in ['docs/EDITION_102_CONTRACT.md','workspace-engine/src/main/java/fraymus/workspace/DataSummary102.java','workspace-engine/src/test/java/fraymus/workspace/DataSummary102Test.java','workspace-engine/src/test/java/fraymus/workspace/CreationTiming102Test.java','workspace-desktop/data-summary102.cjs','workspace-desktop/test/data-summary102-integration.cjs','workspace-desktop/test/data-summary102-renderer.cjs','workspace-desktop/test/workflow-cost102-integration.cjs','workspace-desktop/test/workflow-cost102.test.cjs','scripts/WorkflowCost102.cjs','scripts/Inspect-WorkflowCost102.cjs','scripts/Inspect-DataSummary102.cjs','scripts/Inspect-Exhausted102.cjs','scripts/Inspect-DomFailure102.cjs','scripts/Inspect-ReuseFailure102.cjs','workspace-desktop/surface-diagnostics102.cjs','workspace-desktop/test/surface-diagnostics102-renderer.cjs','workspace-engine/src/test/java/fraymus/workspace/CreationRetry102Test.java']:kinds[p]='utf8-lf'
inventory=[]
for p,kind in sorted(kinds.items()):
 b=(r/p).read_bytes();b=b.decode('utf8').replace('\r\n','\n').encode() if kind=='utf8-lf' else b;inventory.append({'path':p,'bytes':len(b),'sha256':sha(b),'hashKind':kind})
js(d/'file-inventory.json',inventory)
note='docs/obsidian/110-measured-content-procedures.md'
modules=[('chat','Chat and selected source','workspace-engine/src/main/java/fraymus/workspace/Domain044.java'),('parent','Durable Java task','workspace-engine/src/main/java/fraymus/workspace/WorkflowRunner.java'),('generation','Measured local generation','workspace-engine/src/main/java/fraymus/workspace/CreationPlanner044.java'),('contract','Source summary and trace checks','workspace-engine/src/main/java/fraymus/workspace/DataSummary102.java'),('browser','Actual browser observations','workspace-desktop/data-summary102.cjs'),('cost','Complete cost accounting','scripts/WorkflowCost102.cjs')]
system={'modules':[{'id':k,'name':n,'source':p,'note':note}for k,n,p in modules],'screenshots':[{'path':'docs/readmes/102/'+p,'caption':c}for p,c in captions.items()],'evidence':[{'path':'docs/readmes/102/'+n,'caption':n}for n in ['verification.json','analysis.json','original-evidence.json']]};js(d/'system-map.json',system)
def seconds(n):return f'{n/1000:.3f}'
table='\n'.join('| '+t['input']+' | '+t['outcome']+' | '+str(t['rows'])+' | '+seconds(t['cost']['elapsedMs'])+' | '+' | '.join(seconds(t['cost']['exclusiveMs'][k])for k in ['generation','browser-verification','pause-restart','unfinished-generation-window','coordination-and-other'])+' | '+str(len(t['cost']['attempts']))+' | '+str(t['drafts'])+' |' for t in results)
cohortTable='\n'.join('| '+c['cohort']+' | '+seconds(c['elapsedMs'])+' | '+seconds(c['requestIntervalsMs'])+' | '+seconds(c['outsideRequestIntervalsMs'])+' |'for c in cohorts)
screens='\n\n'.join(f'![{c}](docs/readmes/102/{p})\n\n{c}'for p,c in captions.items())
readme=f'''# Edition 102 — measured workflows and verified motion reports

[Previous edition 101](README-101.md) · [Offline guide](docs/readmes/102/index.html) · [Edition history](docs/readmes/INDEX.md) · [Contributor direction](README-CONTRIBUTORS.md)

Edition 102 measures complete chat-driven work, including failures and recovery, then adds a second supported creation procedure. The app can now turn selected motion data into a summary report with an actual displacement trace, exact source table and working filters. Java checks the summary and every plotted point against the original data before retaining the whole procedure. The local model writes the implementation inside the existing integrated workspace.

**Measured limit:** four of seven requests eventually completed; three exhausted their allowed drafts. The new49-row report passed and its procedure survived restart, but16-row changed-source reuse repeatedly reported the wrong total travel and was refused. Even explicit metric instructions did not rescue that request. This edition adds a real checked creation contract; it does not establish reliable autonomous generation or successful summary reuse across new inputs.

## Use it

1. Import/select a motion CSV in Data tasks. Explicitly choose time and displacement columns and their units.
2. Open **Next request inputs → Change**. Choose **Source explorer** or **Motion summary and trace**, then **Prepare data-to-content request**. This fills the chat composer; it does not send.
3. Choose an installed local model, review the requested quality limits and send. Example: “Analyze the selected dataset, create a verified motion summary report and save the whole workflow as motion-summary. Use gap limit 0.05 seconds and speed limit 5 m/s.”
4. Review Mission for analysis, generated drafts, checks and completion. Add “review after data” to request a pause before generation. Resume continues the same owned work; Stop does not publish an unfinished skill.
5. After completion, select fresh data and request “Reuse motion-summary version 1 on the selected dataset.” Java rechecks the retained procedure and fresh source, generates a new report and verifies it again. Our16-row reuse test exhausted its drafts, so treat this as a checked attempt, not a guaranteed working report.

This is a second registered contract, not arbitrary pipeline invention. The original data explorer remains available with its previous verifier bytes unchanged. Existing Game Lab, browser, creation canvas, persistent worlds, physics, tools and procedural memories remain part of the same app. Java retains authority over task state, source identity, recovery, checks and publication.

## What the new report proves

The report contains sample count, duration in seconds, total travel in metres, peak absolute speed in metres per second, gap flags and speed flags. Values use the complete source and are displayed to six decimal places where appropriate. A native SVG polyline contains every displacement sample in time order, mapped to a declared 640×240 coordinate system. Axis labels are checked. The trace is not smoothed, subsampled or inferred from an image.

The existing four table states remain mandatory: all rows, one exact row match, no matches and reset. Each state must retain the exact original source strings, headers, counts, scope and quality labels. The summary and trace must remain unchanged while filtering only the table. Java compares every summary value and each actual SVG point in all four states; the 0.002 SVG-unit tolerance accommodates browser float32 point storage. It is not a physical measurement tolerance.

The browser host reads actual isolated content. The generated page cannot submit its own passing verdict. Hidden ancestors, transparent or absent strokes and changed SVG transforms are refused by the new trace observation path. Positive DOM geometry and style checks still do not prove visibility through every overlay, clipping rule or deceptive contrast. Screenshots receive a separate visual review. Flat, zero-displacement traces are supported without requiring a nonzero polyline bounding-box height.

The saved summary procedure has its own identity and verifier hash. Reuse requires its original completed parent, original evidence, exact published contract and a fresh explicit dataset binding. Imported procedure text does not install executable handlers. Pending skills remain unavailable until durable completion and publication. The previous three-draft and 600-second child limits are unchanged.

## Complete workflow measurements

The actual local-model cohorts use **gpt-oss:20b**, digest `{run['model']['digest']}`, CPU-only with four inference threads. Java is configured with a 512 MiB maximum heap and two reported active processors. Those settings are not a process-resident-memory limit or exclusive CPU allocation; this is a shared host, not a controlled isolated benchmark. The 49- and 52-row inputs are unchanged recorded Delfys data from edition013. The 96- and 16-row stop/reverse/gap sequences are explicitly synthetic controls, not new physical observations.

All numbers below are seconds. Categories partition the caller's complete elapsed interval without double-counting overlapping browser calls. Generation wall intervals include their start-checkpoint write; per-call monotonic nanosecond durations are retained separately. Child stage times overlap the categories and must not be added again.

| Task | Outcome | Rows | Elapsed | Known generation | Browser checks/reads | Pause/restart/assistance | Unfinished-call window | Other | Call attempts | Drafts |
|---|---|---:|---:|---:|---:|---:|---:|---:|---:|---:|
{table}

Request timing begins before chat submission and ends at verified completion or final refusal. Initial app startup and data import precede that request interval; an in-task restart is charged, and later cold reopening is separately recorded. The complete cohort intervals below also include startup/import and intertask work. They end at the final persisted checkpoint before app teardown.

| Cohort | Full interval (s) | Request intervals (s) | Outside requests (s) |
|---|---:|---:|---:|
{cohortTable}

“Other” includes Java analysis, persistence, coordination and harness polling. Instrumentation overhead remains in the complete measurement. Skill saving is inside task completion; final cold reopening is recorded separately. Call attempts include rejected native responses and unfinished calls; a committed content draft can require several call attempts. Failed-draft generation is a subset of generation, not an extra charge. The lossless archive includes every attempted generation job and its durable timing receipt, including interrupted calls that have no completed duration.

The result is clear enough to guide the next experiment: generation dominates these tasks. Correct refusal protects saved skills, but does not establish reliable generation; the exhausted request and repeated repairs show that distinction. It does not establish a matched speedup or prove that memory reduces cost. Current saved procedures retain a verified recipe and source contract; they still ask the model to generate a new implementation for fresh input.

## A genuine failure, retained in the record

The first 49-row explorer completed in 201.268 seconds with one draft. The 52-row reuse request preserved its completed data child across an abrupt Java interruption, but two actual model drafts used an input value where the frozen check required visible text. Both were correctly rejected. The third generation reached the unchanged 600-second creation budget before returning. The original baseline failed and saved no new procedure.

Inspection found that the general creation prompt's readonly-input example was confusing those text-output requirements. The correction enumerates exact text selectors from the already fixed checks and explains their semantics. No source check, verifier, draft limit or time budget was relaxed. The existing edition 101 verifier class remains byte-identical.

An explicit developer-assisted resume completed the remaining third committed draft on the same parent and children. Its 1367.292-second complete interval includes 483.695 seconds of pause, restart, correction and rebuild; 740.113 seconds of known generation intervals; 126.096 seconds bounded around the unfinished call; 10.766 seconds of browser checks/reads; and 6.622 seconds of other work. The unfinished window is uncertain wall time, not a measured completed generation duration. This is assisted recovery, not an autonomous-recovery success or an unchanged-code speed benchmark.

A separate 96-row synthetic trial deliberately altered one displayed source cell immediately before the independent audit. Normal behavior checks had passed, but the audit rejected the altered cell. A real model-generated repair then passed the original contract. This was an injected rendering fault, not a spontaneous model error. Its initial failed audit, replacement draft and complete cost remain attached.

The first summary request failed after its three allowed committed drafts and three explicit resumes. Draft 1 used incorrect summary element IDs, causing a JavaScript error; the next generation hit its unchanged budget. Explicit Resume preserved the data child, and draft 2 corrected the IDs but omitted axis labels. Missing labels initially threw in the browser adapter, interrupting inspection. A developer correction now observes absent or duplicate axes as empty text and visibility false, letting unchanged Java reject the report and request repair. That repair began with only about 20 seconds remaining in the previous child budget and was interrupted. The final allowed Resume generated draft 3, which again omitted the axes. Java refused completion and no skill was saved.

That exhausted request consumed 1708.308 seconds: 1139.843 known generation wall seconds,285.486 seconds in two unfinished-call windows,257.840 seconds in pauses/recovery/assistance,14.584 seconds in browser work and10.555 seconds elsewhere. The independent failure inspector verifies all five generation jobs, both unfinished boundaries, both failed audits, the original source and the absence of a saved skill. It refuses six altered failure records. This unsuccessful run is included in the table, not silently replaced by a later success.

The full requirements did reach the model. The weak point was feedback precision: early checks did not explicitly assert the axes, and the independent rejection described changed units without naming the missing selectors. The revised Java contract adds exact axis-label assertions to the existing fixed checks and supplies full summary DOM IDs in its instruction map. No original table, summary or trace requirement is removed. The new contract has a new code identity; the exhausted task is not relabelled or resumed beyond its limit.

The next fresh request also exhausted three drafts. Draft 1 had invalid JavaScript. Draft 2 fixed parsing but referenced `samples` where the actual ID was `summary-samples`; draft 3 repeated that error. Two intervening generations reached the unchanged attempt budget and were explicitly resumed with the same parent and data child. No runtime change occurred during that request, and no skill was saved. Its complete 1668.589 seconds include 1069.548 known generation seconds, 500.220 unfinished-call seconds, 65.303 recovery seconds, 26.013 browser seconds and 7.505 other seconds. Seven altered records are refused by an independent inspector.

The browser feedback was then improved without changing output checks or budgets. The browser change leaves Java unchanged; the later retry-persistence change is identified separately below. A bounded canvas diagnostic records actual null `getElementById` lookups and existing IDs with a matching suffix. For example, it can report missing `samples` and observed `summary-samples`. These page-originated details are untrusted repair hints. They do not modify generated code, pass assertions or authorize publication. Messages over4096 characters are discarded before diagnostic JSON parsing. Ordinary error messages remain errors; optional missing lookups alone do not fail a working page. The diagnostic uses the existing [Electron debugging transport](https://www.electronjs.org/docs/latest/api/debugger) and [browser document-start instrumentation](https://chromedevtools.github.io/devtools-protocol/tot/Page/#method-addScriptToEvaluateOnNewDocument), adding no package or model dependency.

A subsequent fresh cohort first received a response without a native tool call; it was rejected without executing text. The retry returned a complete timing receipt just before the task deadline, which then interrupted validation. An initial harness assumption that every timeout meant an unfinished model call was corrected; the original failed harness result is retained and both returned call durations remain known. Explicit Resume tested the saved draft, rejected net displacement being substituted for total travel and missing axis labels, and requested repair under the same checks. This cohort tests creation and saving on 49 recorded rows, cold restart and reuse on 16 labelled synthetic stop/reverse/gap samples, followed by read-only inspection after reopening. The 49-row request completed in 1139.873 seconds, including 964.388 seconds of known model-call time and 161.536 seconds of recovery/pause time. Its successful repair corrected numerical travel and missing axes; it did not exercise the new null-lookup hint. The hint is supported by the explicit browser controls, not a claim that it caused this model success. The table gives all actual costs and draft counts. These are actual model-generated implementations, not manually repaired output or test fixtures. Both exhausted predecessors and the later exhausted reuse stay in the table. The16-row reuse also received an invalid native-call response and reached its unchanged deadline during a retry. Inspection found that native-call rejection records persisted but their retry instruction was lost on restart. Java now retains a bounded hint and its source rejection ID only for the same goal, model, request and frozen checks. Two actual worker-boundary tests prove restoration and isolation from another task. This change affects generation guidance; it cannot change output checks, admit another tool or publish an unverified procedure. The reuse task was explicitly resumed after that developer-assisted change, with all build/test/recovery time charged. The first completed task was not rerun or relabelled. These functional demonstrations and adaptive corrections do not establish a controlled speedup or a general reliability rate.


## Validation and reproducibility

The final focused Java suite passes 80 methods, covering old creation and workflow behavior, pending publication, repair recovery, source handoff, typed graphs, new timing receipts and summary contracts. Existing desktop checks pass 135 methods; selection, legacy audit and interval accounting add 10. Five old and eleven summary actual Chromium adversarial fixtures pass. Seven further browser controls check actual missing-ID feedback, optional lookups, ordinary errors and forged diagnostic messages. They are explicitly handcrafted test pages, distinct from local-model demonstrations.

Independent inspectors recompute arithmetic from the original CSVs, check exact source lineage and four observed table states, compare every summary/trace sample and reconcile reported generation jobs with the actual durable journal. Fifteen altered baseline evidence/accounting variants, ten altered successful-summary variants and eight altered exhausted-reuse variants and six altered missing-axis failure variants and seven altered missing-element failure variants are refused. These include omitted jobs, fabricated recovery timing, erased unfinished-call windows, wrong source values, hidden traces and a summary that changes under filtering.

The [verification record](docs/readmes/102/verification.json), [complete measurements](docs/readmes/102/analysis.json), [original archive](docs/readmes/102/original-evidence.json) and [declared protocol](docs/EDITION_102_CONTRACT.md) preserve the results and limits. Each archive entry lists logical path, compressed/raw sizes and SHA-256. To rerun an inspector against retained evidence, restore its logical originals under the build directory and point it at the original result JSON; the durable project journal and baseline failure receipt are required. Restoration for analysis does not authorize executing imported code.

Initial harness failures are retained too: unavailable Electron-evaluation require before inference, and a UI operation guard still closing after the first completed task. They were corrected in the harness/test bridge. The runtime prompt defect and genuine budget failure are documented separately. A misplaced Gradle test-filter option was also corrected before any method ran; its log is retained. Presentation harness assumptions were also corrected: an interrupted workflow reserves its workspace; a separate review copy is stopped before opening the prior report, the host refresh must settle, an input dataset must be selected, and the agency state route requires POST. These logs are retained; original experiment records are unchanged. There is no erased unsuccessful attempt.

Parent CI run 42613246058 was `{ci['status']}` / `{ci['conclusion']}` at the recorded check. Local focused tests are not a full remote CI result. The publication receipt separately records the actual non-force push, fetched main/branch/tag and byte-level audit.

The changed-source reuse also exposed a wrong travel total: its first committed draft reported 0.160000m instead of the source-derived 0.310000m. The app refused it, started repair and reached the unchanged deadline before that call returned. The original interruption is retained separately; another explicit same-runtime Resume continues the remaining draft and charges the second pause and unfinished call window. The second draft repeated the range-versus-travel error and its next repair also reached the deadline. The final allowed Resume follows an explicit metric definition in the Java generation prompt. The source and outcome verifiers and three-draft limit are unchanged. The third draft STILL reported0.160000m. The task exhausted its three drafts and three resumes, and no new skill was saved. The explicit definition did not rescue this request. Its3927.334 seconds include1542.157 known-generation seconds,679.765 unfinished-call seconds,1692.506 pause/assistance seconds,5.054 browser seconds and7.852 other seconds. The original verified49-row skill remains intact. All three pauses and unfinished-call windows are charged; this is a failed request with developer assistance, not successful reuse.

## Actual application screenshots

Captured 2026-11-09 from isolated test projects, showing actual generated content and task state. No design mockups, private footage or model weights are included.

{screens}

## Architecture and next steps

```mermaid
flowchart LR
  A[Chat and explicit data version] --> B[Java durable workflow]
  B --> C[Verified analysis]
  C --> D[Measured local generation]
  D --> E[Actual browser actions and observations]
  E --> F[Java source and trace verification]
  F --> G[Durable completion and saved procedure]
  F -->|bounded repair| D
```

1. Reuse a verified implementation with fresh bound input, then run every source and outcome check again. Compare that against regeneration under matched datasets, restarts and failure cases. The current costs justify testing this; they do not yet prove the benefit.
2. Check remaining execution time before admitting expensive repair generation; the observed short interrupted call exposes that gap. Distinguish a transient rendered failure from an implementation defect with a bounded fresh observation. Measure whether re-observation avoids unnecessary regeneration without accepting corrupted output.
3. Expand creation contracts around complete useful tasks: comparison reports and source-bound exports are candidates. Each needs an independent output schema and altered-result refusals before promotion.
4. Strengthen clipping/overlay checks and bounded history archives. Current limits remain 20 workflows, 64 goals and 32 saved skills; unlimited memory growth is not implemented.

The destination remains a local integrated agent that observes, acts, simulates, creates and retains useful verified experience. This edition makes its work measurable and expands what a retained creation procedure can deliver. It does not update model weights or establish general intelligence, physical calibration accuracy or unrestricted self-improvement.

Java 21, the repository Gradle wrapper, installed desktop dependencies and an admitted local model remain the launch requirements. Follow the earlier setup guide and use a separate project directory for review. Docker, WSL and a cloud provider are not required for these demonstrated workflows. Preserve private live projects and frozen editions001–101.
'''
write(r/'README-102.md',readme)
write(r/'README.md','# Fraynix Agent Workspace\n\nEdition102 measures complete local-agent workflows, keeps failures and recovery visible, and adds a verified motion-summary report with source-checked charts and retained procedures. Four of seven measured requests completed; three exhausted their drafts, including changed-input summary reuse. Generation reliability remains a measured limitation.\n\n[Edition102: measurements, functions, results and next steps](README-102.md) · [Offline guide](docs/readmes/102/index.html) · [Previous edition](README-101.md) · [Contributor direction](README-CONTRIBUTORS.md)\n\nJava owns durable goals, physics, tools, verification and retained procedures. Chat, browser, creation canvas, Game Lab and spatial views share those capabilities.\n\n![Actual motion summary](docs/readmes/102/summary102-1600.png)\n\nPublication receipt follows the actual push and fetched-content audit.\n')
status=f'''## Edition102 — locally verified; publication pending

2026-11-09, America/Los_Angeles. Isolated S:/Codex-Work/fraynix-workspace-edition102; summary cohort began at {run['source']}; final runtime receipt source {load(r/'build/summary102-final-build.json')['source']}. Three measured explorer tasks and one new49-row motion summary completed; three summary requests, including changed-source16-row reuse, exhausted their draft limits and saved no new skills, with their full1708.308s,1668.589s and3927.334s retained. The52-row baseline first failed two drafts and the unchanged600s budget; explicit developer-assisted prompt correction/recovery is labelled, with originals and unfinished-call timing retained. The96-row injected display fault was rejected then repaired. The first summary exhausted all allowed drafts/resumes after JavaScript and missing-axis failures. Explicit resumes, adapter correction and all costs are retained as a failed request. Stronger explicit axis checks were followed by bounded actual missing-ID diagnostics, with Java and outcome checks unchanged for the browser change. Subsequent same-task Java retry-hint persistence is tested separately and the16-row continuation is labelled developer-assisted. The final fresh cohort is distinct from both exhausted requests. The new49-row summary/trace checks every original value and point in four filter states and its skill survives cold restart. Fresh16-row input binds correctly but all three generated drafts report range0.16m instead of total travel0.31m, even after explicit formula guidance; reuse is refused. Do not claim changed-input summary reuse succeeded. Legacy101 verifier bytes remain unchanged.

80 focused Java methods,135 existing desktop and10 additional checks pass;5 legacy,11 summary and7 diagnostic actual Chromium fixtures pass. Independent original/journal/accounting inspection passes with15 baseline,18 mixed-summary and6+7 prior-failure altered-evidence refusals. No matched speedup or weight-training claim. Complete cost is dominated by actual local generation; next test verified implementation reuse with full fresh-source/outcome checks. CPU-only/four-thread model policy preserved.

Keep private live045/qa046, earlier worktrees and frozen001–101. No new delegation; heavy work serial. Main/tag publication and fetched receipt still pending. Parent CI42613246058 is {ci['status']}/{ci['conclusion']}; do not call pending CI passed. All owned102 tests are closed. See README102, contract and lossless originals.

'''
for n in ['AGENTS.md','docs/PROJECT_MEMORY.md','docs/DEVELOPMENT_LOG.md','docs/ARCHITECTURE.md','README-CONTRIBUTORS.md']:
 p=r/n;old=p.read_text(encoding='utf8');assert '## Edition102 — locally verified; publication pending' not in old;write(p,status+old)
p=r/'docs/readmes/INDEX.md';write(p,'[Edition102 — measured content procedures](../../README-102.md) · [Offline guide](102/index.html)\n\n'+p.read_text(encoding='utf8'))
write(r/note,'# Measured content procedures\n\n[[README-102.md|Edition102]]\n\n'+status+''.join(f'\n- [[{p}|{n}]]'for k,n,p in modules)+'\n')
p=r/'docs/obsidian/00-home.md';write(p,'[[docs/obsidian/110-measured-content-procedures.md|Edition102 measured content procedures]]\n\n'+p.read_text(encoding='utf8'))
js(r/'docs/obsidian/110-measured-content-procedures.canvas',{'nodes':[{'id':k,'type':'file','file':p,'x':(i%3)*420,'y':(i//3)*260,'width':380,'height':210}for i,(k,n,p)in enumerate(modules)],'edges':[{'id':f'e{i}','fromNode':modules[i][0],'toNode':modules[i+1][0]}for i in range(len(modules)-1)]})
template=(r/'build/guide102-template.html').read_text(encoding='utf8');write(d/'index.html',template.replace('SYSTEM_JSON',json.dumps(system,ensure_ascii=False)).replace('INVENTORY_JSON',json.dumps(inventory,ensure_ascii=False)).replace('RESULT_JSON',json.dumps(analysis,ensure_ascii=False)))
print(json.dumps({'canonical':len(inventory),'originalEntries':len(archive),'readmeWords':len(readme.split())}))
