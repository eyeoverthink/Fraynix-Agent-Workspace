"""Generate edition109 documentation from independently verified real execution only."""
from pathlib import Path
import json,hashlib,gzip,shutil,subprocess,xml.etree.ElementTree as ET
r=Path(__file__).resolve().parent.parent;d=r/'docs/readmes/109'
def load(p):return json.loads((r/p).read_text(encoding='utf-8-sig'))
def write(p,s):p.parent.mkdir(parents=True,exist_ok=True);p.write_text(s,encoding='utf-8',newline='\n')
def js(p,v):write(p,json.dumps(v,indent=2,ensure_ascii=False)+'\n')
def sha(b):return hashlib.sha256(b).hexdigest()
assert subprocess.run(['git','show-ref','--verify','--quiet','refs/tags/readme-109'],cwd=r).returncode==1
analysis=load('build/analysis109.json');trial=load('build/maintenance109/result.json');visual=load('build/visual-review109.json')
assert analysis['passed'] and len(analysis['refusals'])==30 and trial['passed'] and visual['passed']
identity=load('build/source-identity109.json');assert identity['passed'] and identity['productionClasses']==342
tests=[]
for f in sorted((r/'workspace-engine/build/test-results/test').glob('TEST-*.xml')):
 e=ET.parse(f).getroot();assert all(e.attrib[k]=='0' for k in ['failures','errors','skipped']);tests.append(dict(e.attrib))
assert sum(int(x['tests']) for x in tests)==126
for name in ['desktop109.log','desktop109-additional.log']:assert 'fail 0' in (r/'build'/name).read_text(encoding='utf-8-sig')
d.mkdir(parents=True,exist_ok=True)
for row,t in zip(analysis['results'],trial['tasks']):row['screenshot']=t['captureKey']+'-1600.png'
js(d/'analysis.json',analysis)
js(d/'verification.json',{'passed':True,'java':tests,'javaScope':'126 methods on final unchanged production bytecode,20 classes. Full originals retained.','desktopChecks':148,'independentRefusals':analysis['refusals'],'sourceIdentity':identity,'build':load('build/study095-build.json'),'visual':visual,'parentCI':load('build/ci109-parent.json'),'actualRequests':36,'completions':30,'requiredRefusals':6,'abruptRecoveries':1})
captions={}
for row in analysis['results']:
 name=row['screenshot'];shutil.copyfile(r/'build/maintenance109'/name,d/name);captions[name]=row['name']+': '+row['kind']+', bank '+str(row['bankBefore'])+', '+row['outcome']+'. Actual application.'
for name,caption in [('maintenance109-1100.png','Actual narrower workspace after recovered bank9 refresh.'),('near-capacity-restart-interrupted.png','Third fresh example before its actual audit and abrupt process interruption.')]:
 shutil.copyfile(r/'build/maintenance109'/name,d/name);captions[name]=caption
roots=['build/maintenance109','workspace-engine/build/test-results/test'];extras=['build/java109.log','build/desktop109.log','build/desktop109-additional.log','build/source-identity109.json','build/ci109-parent.json','build/study095-build.json','build/analysis109.json','build/inspect109.log','build/visual-review109.json','build/maintenance109.log']
paths=[]
for folder in roots:
 for p in (r/folder).rglob('*'):
  if p.is_file() and not any(k in {'desktop-profile','runtime-042','.northstar-kernel','binary'} for k in p.parts) and not p.name.endswith('.lock'):paths.append(p)
paths.extend(r/p for p in extras);archive=[]
for p in sorted(set(paths)):
 raw=p.read_bytes();compressed=gzip.compress(raw,mtime=0);name=d/'original'/(sha(raw)+'.gz');name.parent.mkdir(exist_ok=True)
 if not name.exists():name.write_bytes(compressed)
 archive.append({'logicalPath':p.relative_to(r).as_posix(),'path':name.relative_to(r).as_posix(),'bytes':len(compressed),'sha256':sha(compressed),'rawBytes':len(raw),'rawSha256':sha(raw)})
js(d/'original-evidence.json',{'files':archive,'scope':'Lossless actual109 genuine growth, closed same-bank copies, original/fresh reports, native refusal and recovery, all preparation/request/copy costs and checks. Isolated synthetic software examples; private live worlds excluded.'})
kinds={x['path']:x['hashKind'] for x in load('docs/readmes/108/file-inventory.json')}
for p in ['docs/EDITION_109_CONTRACT.md','workspace-desktop/test/maintenance109-integration.cjs','scripts/Inspect-Maintenance109.cjs','scripts/Document-Maintenance109.py','scripts/maintenance109-guide.html']:kinds[p]='utf8-lf'
inventory=[]
for p,kind in sorted(kinds.items()):
 b=(r/p).read_bytes();b=b.decode('utf-8').replace('\r\n','\n').encode() if kind=='utf8-lf' else b;inventory.append({'path':p,'bytes':len(b),'sha256':sha(b),'hashKind':kind})
js(d/'file-inventory.json',inventory)
note='docs/obsidian/117-maintenance-at-capacity.md'
modules=[('chat','Native chat routing','workspace-engine/src/main/java/fraymus/workspace/Domain044.java'),('qualification','Five original example definitions','workspace-engine/src/main/java/fraymus/workspace/Qualification105.java'),('maintenance','Explicit refresh dependencies','workspace-engine/src/main/java/fraymus/workspace/Qualification107.java'),('supervisor','Unchanged capacity and source/outcome checks','workspace-engine/src/main/java/fraymus/workspace/WorkflowRunner.java'),('goals','Durable goals and procedure publication','workspace-engine/src/main/java/fraymus/workspace/GoalRunner.java'),('producer','Input-driven summary','workspace-engine/src/main/java/fraymus/workspace/DataSummary104.java'),('audit','Actual numerical and visual verification','workspace-engine/src/main/java/fraymus/workspace/DataSummary102.java'),('study','Real growth and paired maintenance','workspace-desktop/test/maintenance109-integration.cjs'),('inspection','Independent source, lineage and cost reconstruction','scripts/Inspect-Maintenance109.cjs')]
system={'modules':[{'id':i,'name':n,'source':p,'note':note} for i,n,p in modules],'screenshots':[{'path':'docs/readmes/109/'+n,'caption':c} for n,c in captions.items()],'evidence':[{'path':'docs/readmes/109/'+n,'caption':n} for n in ['analysis.json','verification.json','original-evidence.json']]};js(d/'system-map.json',system)
pairs='\n'.join('| '+str(x['bank'])+' | '+str(x['rep'])+' | '+f"{x['recheckMs']/1000:.3f} | {x['refreshMs']/1000:.3f} | "+('completed' if x['refreshCompleted'] else 'capacity refusal')+' |' for x in analysis['pairs'])
table='\n'.join('| '+x['name']+' | '+str(x['bankBefore'])+' | '+x['outcome']+' | '+' | '.join(f'{x[k]/1000:.3f}' for k in ['totalMs','browserMs','restartMs','otherMs'])+' |' for x in analysis['results'])
readme=f'''# Fraynix Agent Workspace - edition 109

## What it costs to maintain a real growing memory bank

Edition109 measures the existing Java agent as its saved-procedure bank grows through successful work, not inserted records. Thirty actual chat workflows complete, including one abruptly interrupted refresh. Six requests correctly refuse original capacity or changed evidence. Fifteen saved procedures are backed by completed source-to-report workflows and actual browser verification.

**The first limit is workflow history, not the advertised procedure slots.** Five retained qualification examples plus fifteen selected-source workflows fill the original twenty-workflow capacity. The 32-procedure limit remains unchanged and is not reached in this task family. A full five-example refresh needs six workflow slots; a recheck needs one. Knowing that difference is a practical result, not a reason to discard evidence.

[Previous edition108](README-108.md) | [Interactive offline guide](docs/readmes/109/index.html) | [Measurements](docs/readmes/109/analysis.json) | [Validation](docs/readmes/109/verification.json) | [Contributor direction](README-CONTRIBUTORS.md)

![Actual fifteen-procedure bank](docs/readmes/109/growth-15-1600.png)

## The application and this edition's role

Fraynix remains one local visual agent workspace. Java owns durable goals, tools, physics and world state, verification, recovery and procedural memory. The desktop hosts chat, browser and canvas surfaces, spatial views and Game Lab. A tool performs an action; a skill completes a task through actions, checks and recovery.

The measured task imports motion CSV with explicit units and coordinates, calculates motion and quality, creates an interactive report from fresh input, observes its actual browser output, checks required values and saves a verified procedure. Qualification covers monotonic motion, reversal, a stationary source, unit conversion and quality flags, and explicitly skipped missing observations. All original numerical, ownership, source, browser, dependency and publication checks remain.

This is a measurement and diagnostic edition. All342 compiled Java engine classes and both report resources are byte-identical to the measured final edition108 runtime. Application source and limits are unchanged. New code supplies a real integration study, independent inspection and an interactive cost/capacity guide. It does not manufacture a faster result by changing the implementation under comparison.

The fifteen saved entries have different selected datasets and genuine verified publications, but share the same summary procedure family. They are not fifteen different reasoning algorithms or demonstrations of model training. These native chat requests use zero planner calls and zero content-generation calls. Other local-model workflows remain available; this study does not evaluate them.

## How the bank was grown

1. Start a distinct empty project; keep private live worlds separate.
2. Use chat to qualify an input-driven summary and save a procedure. The first request runs all five examples and its selected dataset.
3. For each next entry, select distinct CSV data and save under a new name. Java rechecks every matching original example, verifies the new selected report and publishes only its completed child's procedure.
4. Close the project and freeze copies at banks1,8,9,14 and15. Every copied file is compared byte-for-byte and recorded with its hash. No passing proof or saved record is injected.
5. Run each comparison in a separate cold copy of exactly the same bank. Preserve preparation time and every request, including refusals.

The fifteen growth requests cost **{analysis['growthMs']/1000:.3f}seconds**, included in the complete cohort. The five reusable examples were executed once during initial growth; later qualification rechecks them rather than treating their presence as sufficient evidence.

## Recheck versus full refresh

At banks1,8,9 and14, two repetitions compare ordinary qualification with explicit full refresh. The second pair reverses order. Both members use identical selected CSV and criteria and the same closed original bank. Full refresh rechecks the baseline, then runs all five examples anew and verifies the selected source. Recheck verifies each original and then completes the selected-source workflow.

| Starting bank | Repetition | Recheck seconds | Refresh seconds | Refresh outcome |
| ---: | ---: | ---: | ---: | --- |
{pairs}

These two observations per point describe this machine and workload. They do not establish a universal latency curve, a confidence interval, a causal effect of procedure count alone, or an equal-work speedup. Bank size grows together with history, stored proofs and data. Full refresh performs more work. Refused refreshes are reported as refusals, never as fast successful maintenance.

The offline chart plots only completed requests and explicitly identifies capacity refusals. Detailed evidence includes request wall time, actual browser spans, recovery pauses, Java admission and final-verification times, retained store bytes and all workflow/goal/supervisor counts. A missing admission/final time for a refused request is null, not a measured zero.

## Capacity is a combined budget

| Original bound | Value | What this study establishes |
| --- | ---: | --- |
| Executable workflow history |20| First binding limit, reached with five examples plus fifteen selected-source workflows |
| Saved procedures |32| Unchanged; not reached in this qualification workload |
| Native goals |64| Counted on every request; preserved through recovery |
| Qualification supervisors |16| Counted independently from executable children |
| Versions per procedure |8| Unchanged; study uses distinct names and first versions |
| Awake time per qualification |1200seconds| Unchanged and retained across restart |
| Explicit resumes |3| Unchanged; recovery study uses one |

A bank9 project has fourteen executable workflows. Full refresh can consume the remaining six and complete. At bank14, nineteen workflows exist: recheck can use the last slot, but refresh refuses before starting. At bank15, both requests refuse. The saved bank, original workflow file, native-goal count and workflow count remain unchanged after each refused admission. The chat retains the original Java reason, makes no planner retry and refuses Resume.

![Actual full-bank refusal with original reason](docs/readmes/109/full-bank-recheck-1600.png)

An earlier report may remain visible on the canvas when a request refuses. It is retained history, not a new successful result. Existing limits deliberately prevent unbounded retained execution. This edition neither raises them nor silently deletes old tasks.

## Recovery and changed-source controls

The near-capacity test starts a full refresh from bank9. The recorder waits for Java to report two completed fresh examples and holds the third example's actual independent browser audit before execution. It abruptly terminates the Java process without supplying a passing observation. Reopening restores the same supervisor, baseline, children, original sources, artifact versions, reserved input bytes and completed stages. Explicit Resume performs the remaining real checks and saves the procedure, finishing at twenty executable workflows.

![Actual third example before interruption](docs/readmes/109/near-capacity-restart-interrupted.png)

Two additional bank14 tests change an original report or revise the original dataset without changing its CSV. Both refuse the original source/ownership check before creating a replacement workflow or procedure. The original reason remains visible and cannot be converted to success by planner retries or Resume.

Independent inspection recomputes each required metric, every SVG point and all four filter states from the original CSV and actual browser observations. It verifies all saved entries against their producing workflows, exact code and dependency identities, growth continuity, source revisions, publication checks, restart identity and measured cost partitions. Thirty deliberately altered records must fail inspection, including padded skill lineage, missing old proofs, changed limits, hidden cost, altered restart inputs and mutated refusal history.

## Complete measured costs

| Request | Starting bank | Outcome | Total seconds | Browser | Restart | Other |
| --- | ---: | --- | ---: | ---: | ---: | ---: |
{table}

Complete cohort: **{analysis['cohortWallMs']/1000:.3f}s**. All request intervals: **{analysis['requestMs']/1000:.3f}s**. Cold launch, copies, imports, inspection collection, captures and shutdown between requests: **{analysis['setupAndBetweenMs']/1000:.3f}s**. Growth preparation is included in the requests total. Component intervals are not double-counted. Build/regression and documentation checks occur outside the cohort and have separate original logs; these are application-workflow timings, not total developer effort.

## Verification and boundaries

-126 Java methods across20 classes pass on the final unchanged runtime.
-148 desktop checks pass.
-Thirty actual complete chat workflows, six required refusals and one abrupt recovery pass.
-Thirty altered-record controls refuse; every saved entry has checked original lineage.
-All342 engine classes and both report resources match edition108 exactly; no production verification or limit was relaxed.
-Actual application screenshots at1600 and1100 pixels; the offline guide is checked at1440 and390 pixels, including all36 selections, exact cost displays, chart points and local links.

[Lossless originals](docs/readmes/109/original-evidence.json), [source inventory](docs/readmes/109/file-inventory.json) and [system map](docs/readmes/109/system-map.json) retain the evidence. Inputs are declared synthetic software-test data, not new measurements from the physical world. No claim of automatic retirement, selective refresh, broader model intelligence or weight training is made. Private live data and frozen editions001-108 are preserved. Parent CI was checked first; remote CI is reported separately from local validation.

## Run and inspect

Use Java21 and the repository's Gradle wrapper. Build `gradlew.bat :workspace-engine:installDist`, install desktop dependencies with `npm ci` if needed, then run `npm start` in `workspace-desktop`. Use a separate project for a study and never two owners of the same private database.

Select fresh CSV data and ask:

> Qualify the input-driven summary, analyze selected data and save workflow as measured-summary.

To explicitly run all five examples again:

> Refresh qualification for the input-driven summary, analyze selected data and save workflow as refreshed-summary.

Inspect Mission for example progress, the actual report, original reasons and saved procedure. At capacity, preserve the project and inspect its history. There is no implemented archive command that safely frees these workflow slots yet.

To reproduce the full study, build the unchanged runtime, commit the source state, stamp `node scripts/Study-Build095.cjs --stamp`, run `node workspace-desktop/test/maintenance109-integration.cjs`, then `node scripts/Inspect-Maintenance109.cjs`. Keep the `gpt-oss:20b` catalogue entry available; these recognized native requests make no inference calls. The harness refuses an existing output directory. Preserve failed attempts and use a fresh directory rather than overwriting evidence.

```text
workspace-engine/src/main/java/fraymus/workspace/
  WorkflowRunner.java       unchanged source/outcome checks and capacity admission
  Qualification105.java    unchanged five software examples
  Qualification107.java    unchanged recheck/refresh intent and dependencies
  GoalRunner.java          unchanged durable children and procedure publication
workspace-desktop/test/
  maintenance109-integration.cjs  actual growth, cold copies, recovery and refusals
scripts/
  Inspect-Maintenance109.cjs      independent data, lineage, capacity and cost checks
  Document-Maintenance109.py      evidence-linked README and retained originals
  maintenance109-guide.html      interactive real-cost and capacity guide
```

## Next: an explicit archival lifecycle

The next useful improvement is a bounded active history whose immutable source and outcome evidence remains addressable. First define which completed records can leave active scheduling, how their proofs and dependencies stay reachable, and how old child ownership and saved-procedure references survive. Then test fresh verification, restart, corrupted archives and missing records before allowing archived workflows to release capacity. Keep the current limits and original trust boundaries until this is implemented.

After that, measure maintenance again on genuinely larger banks, test whether archival/retrieval earns its cost, and consider selective refresh only with a versioned contract and fresh outcome evidence. The larger direction remains a Java-owned agent that observes, acts, checks, remembers and recovers; scaling its memory requires preserving what made that memory trustworthy.
'''
for a,b in [('\n-126','\n- 126'),('\n-148','\n- 148'),('\n-Thirty','\n- Thirty'),('\n-All342','\n- All342'),('\n-Actual','\n- Actual')]:readme=readme.replace(a,b)
write(r/'README-109.md',readme)
write(r/'README.md','# Fraynix Agent Workspace\n\nEdition109 measures maintenance as genuine saved procedures fill the existing workflow capacity, preserving every source and outcome check.\n\n[Edition109: measurements, functions and next steps](README-109.md) | [Interactive offline guide](docs/readmes/109/index.html) | [Previous edition](README-108.md) | [Contributor direction](README-CONTRIBUTORS.md)\n\nThirty actual workflows complete, including abrupt recovery; six refuse correctly. All342 Java engine classes remain unchanged.\n\n![Actual fifteen-procedure bank](docs/readmes/109/growth-15-1600.png)\n\nJava owns durable goals, tools, worlds, verification, recovery and memory. Chat, browser/canvas, spatial views and Game Lab remain integrated.\n')
summary='2026-11-10. Isolated S:/Codex-Work/fraynix-workspace-edition109; parent028e2451. Actual36-request maintenance study:30 completions, six native refusals, one abrupt bank9 refresh recovery. Genuine growth to15 saved procedures reaches20 executable workflows before32 procedure slots. Cold paired recheck/full refresh at banks1/8/9/14, two reversed-order repetitions; full preparation/copy/refusal/restart costs retained. No padding, history deletion or raised limits. Every source/outcome check preserved. All342 engine classes and two resources byte-identical to108;126 Java,148 desktop and30 altered-record refusals pass. Private worlds and frozen001-108 preserved; no delegation; heavy work serial. Next: explicit evidence-preserving archival before larger active history. Publication pending actual push/fetch; CI separate.\n'
write(r/note,'# Edition109 - maintenance at capacity\n\n'+summary+'\n[[README-109.md|Full edition]] | [[docs/obsidian/116-explicit-qualification-upgrades.md|Previous qualification upgrades]]\n')
for name in ['AGENTS.md','README-CONTRIBUTORS.md','docs/PROJECT_MEMORY.md','docs/DEVELOPMENT_LOG.md','docs/ARCHITECTURE.md']:
 p=r/name;old=p.read_text(encoding='utf-8');header='## Edition109 - validated maintenance at capacity\n\n'
 if header not in old:write(p,header+summary+'\n'+old)
for name,header in [('docs/readmes/INDEX.md','[Edition109 - maintenance at capacity](../../README-109.md) | [Offline guide](109/index.html)'),('docs/obsidian/00-home.md','[[docs/obsidian/117-maintenance-at-capacity.md|Edition109 - maintenance at capacity]]')]:
 p=r/name;old=p.read_text(encoding='utf-8')
 if header not in old:write(p,header+'\n\n'+old)
js(r/'docs/obsidian/117-maintenance-at-capacity.canvas',{'nodes':[{'id':'study','type':'file','file':'README-109.md','x':0,'y':0,'width':500,'height':340},{'id':'continuity','type':'file','file':note,'x':580,'y':0,'width':500,'height':340}],'edges':[{'id':'maintenance','fromNode':'study','fromSide':'right','toNode':'continuity','toSide':'left','label':'genuine growth, unchanged checks, cost and capacity'}]})
template=(r/'scripts/maintenance109-guide.html').read_text(encoding='utf-8');write(d/'index.html',template.replace('SYSTEM_JSON',json.dumps(system)).replace('INVENTORY_JSON',json.dumps(inventory)).replace('RESULT_JSON',json.dumps(analysis)))
print(json.dumps({'passed':True,'originalFiles':len(archive),'canonicalSources':len(inventory),'javaMethods':126,'actualRequests':36}))
