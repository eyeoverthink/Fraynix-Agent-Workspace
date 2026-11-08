from pathlib import Path
import json,hashlib,gzip,base64,subprocess,shutil,xml.etree.ElementTree as ET,os,re
r=Path.cwd();d=r/'docs/readmes/092';d.mkdir(parents=True,exist_ok=True)
def load(p):return json.loads(p.read_text(encoding='utf-8-sig'))
def write(p,s):p.parent.mkdir(parents=True,exist_ok=True);p.write_text(s,encoding='utf8',newline='\n')
def js(p,v):write(p,json.dumps(v,indent=2,ensure_ascii=False)+'\n')
def sha(b):return hashlib.sha256(b).hexdigest()
def sec(v):return f'{v/1000:.2f} s'
a=load(r/'build/analysis092.json');rec=load(r/'build/ui092-study/result.json');assert a['passed'] and rec['passed']
identity=load(r/'build/source-comparison092.json');assert identity['unchangedClasses']==333 and not identity['changed']
xml=list((r/'build/regression092').glob('TEST-*.xml'));assert xml
tests={'cases':0,'failures':0,'errors':0,'skipped':0,'classes':len(xml),'skips':[]}
for f in xml:
 t=ET.parse(f).getroot();tests['cases']+=int(t.attrib['tests'])
 for k in ['failures','errors','skipped']:tests[k]+=int(t.attrib.get(k,0))
 for case in t.findall('testcase'):
  if case.find('skipped') is not None:tests['skips'].append(case.attrib)
assert tests['failures']==tests['errors']==0
assert 'pass 135' in (r/'build/desktop092.log').read_text(encoding='utf-8-sig')
assert 'pass 5' in (r/'build/work092.log').read_text(encoding='utf-8-sig')
test_summary=f"{tests['cases']-tests['skipped']} Java checks passed across {tests['classes']} focused classes; {tests['skipped']} optional host-dependent test skipped. 135 desktop checks and five new cross-boot accounting checks passed. Twenty fresh native goals and sixty repeated full goal audits passed."
tot=a['totals'];saving=tot['reference']['callerMillis']-tot['reuse']['callerMillis'];reduction=100*(1-tot['reuse']['canonicalChecks']/tot['reference']['canonicalChecks'])
finding=f"Across six paired comparisons, digest reuse took {sec(abs(saving))} {'less' if saving>=0 else 'more'} total caller time, with {reduction:.1f}% fewer canonical checks in the observed counter windows. All twenty goals in the final-stage follow-up verified, including six final-stage interrupted and resumed tasks. An earlier-stage restart was correctly refused after its next source state changed; that remains an unresolved recovery limit."
replays=[]
for row in rec['rows']:
 if row['mode']!='reuse' or row['retain']:continue
 for packed in load(r/'build/ui092-study'/row['originalFile'])['originals']:
  run=json.loads(gzip.decompress(base64.b64decode(packed['gzipBase64'])));attempt=next(x for x in row['job']['arm']['attempts'] if x['id']==run['id'])
  replays.append({'label':f"Bank {row['bank']} / {row['lifecycle']} / {attempt['family']} / {attempt['stage']}",'id':run['id'],'status':run['status'],'objects':run['definition']['objects'],'samples':[{'tick':s['tick'],'timeS':s['timeS'],'bodies':s['bodies']} for s in run['samples']]})
archive=[];local={}
def pack(p,logical):
 raw=(Path('\\\\?\\'+str(p)) if os.name=='nt' else p).read_bytes();h=sha(raw)
 if h not in local:
  dest=d/'original'/(h+'.gz');dest.parent.mkdir(parents=True,exist_ok=True);z=gzip.compress(raw,mtime=0);dest.write_bytes(z);local[h]={'path':dest.relative_to(r).as_posix(),'bytes':len(z),'sha256':sha(z),'rawBytes':len(raw),'rawSha256':h}
 archive.append(dict(local[h],logicalPath=logical))
for foldername in ['ui092-study','ui092-first','ui092-early-restart']:
 folder=r/'build'/foldername
 for p in folder.rglob('*'):
  if p.is_file() and 'desktop-profile' not in p.parts and p.name!='host.json' and p.suffix in ['.json','.jsonl','.log','.png','.html','.css','.js','.txt','.csv','.fworld','.previous']:pack(p,foldername+'/'+p.relative_to(folder).as_posix())
for f in xml:
 pack(f,'regression092/'+f.name)
 for match in re.finditer(r'OUTCOME091_NATIVE ([^\r\n<]+)',f.read_text(encoding='utf8')):
  unit=Path(match.group(1).strip());assert unit.is_relative_to(r/'build/tmp-tests')
  for p in unit.rglob('*.json'):pack(p,'outcome-unit/'+p.relative_to(unit).as_posix())
for p in (r/'build').glob('*092*'):
 if p.is_file() and not (p.name.startswith('document092') and p.suffix=='.log') and p.suffix in ['.json','.log','.ps1','.py','.cjs','.html','.md','.txt']:pack(p,'validation/'+p.name)
for p in (r/'build/ui092-study').glob('*.png'):shutil.copyfile(p,d/p.name)
js(d/'original-evidence.json',{'files':archive,'distinctNewPayloads':len(local),'note':'Exact synthetic originals and logs; raw/compressed hashes. Host credentials and desktop profiles excluded. Reused compressed blobs do not constitute new motion observations.'})
old=load(r/'docs/readmes/091/file-inventory.json');paths={x['path'] for x in old};paths.update(x for x in subprocess.check_output(['git','diff','--name-only','--diff-filter=A','087266c2c764a25b313739a1a53c02e310f5f52c','HEAD'],text=True).splitlines() if x.startswith(('workspace-engine/','workspace-desktop/','scripts/','fraynix-modules/')))
kinds={x['path']:x['hashKind'] for x in old};inventory=[]
for name in sorted(paths):
 raw=(r/name).read_bytes();kind=kinds.get(name,'utf8-lf');raw=raw.decode('utf8').replace('\r\n','\n').encode() if kind=='utf8-lf' else raw
 inventory.append({'path':name,'bytes':len(raw),'sha256':sha(raw),'hashKind':kind})
js(d/'file-inventory.json',inventory);write(d/'FILE_TREE.txt','Edition092 canonical source inventory\n'+'\n'.join(x['path'] for x in inventory)+'\n')
note='docs/obsidian/100-growing-bank-restarts.md'
modules=[{'id':'digest','name':'Bounded fingerprint table','source':'workspace-engine/src/main/java/fraymus/workspace/NativeEnvelope091.java','description':'Unchanged full-byte checks, fresh JSON and64-entry digest reuse.','note':note},{'id':'procedure','name':'Durable native procedure','source':'workspace-engine/src/main/java/fraymus/workspace/WorldProcedure079.java','description':'Preserves originals, source identity, motion checks and explicit recovery.','note':note},{'id':'study','name':'Growing-bank experiment','source':'workspace-desktop/test/envelopes092-integration.cjs','description':'Real fresh native goals, app restarts, prefix checks and measured caller time.','note':note},{'id':'inspect','name':'Independent evidence inspection','source':'scripts/Inspect-Envelopes092.cjs','description':'Original source/outcome/commit checks and full cross-boot cost accounting.','note':note}]
system={'modules':modules,'screenshots':[{'path':'docs/readmes/092/'+x['file'],'caption':'Actual092 saved report capture · 2026-11-08 America/Los_Angeles'} for x in rec['captures']],'evidence':[{'path':'docs/readmes/092/verification.json','caption':'Complete measured results'},{'path':'docs/readmes/092/original-evidence.json','caption':'Exact originals'}]};js(d/'system-map.json',system)
verification={'passed':True,'analysis':a,'java':tests,'desktopPassed':135,'accountingPassed':5,'classes':identity,'screenshots':rec['captures'],'finding':finding,'source':rec['source'],'limits':[a['scope'],'No LLM or real-camera test in092. No calibrated-selection fit. No changes to production Java.','Cold reopen/prefix verification uses production reuse in both arms. Restart totalMillis is unavailable (0), not an elapsed measurement. Counter snapshots cannot observe post-snapshot shutdown reads; caller wall time includes closure.','Optional Windows symbolic-link creation is recorded as skipped;091 separately passed its junction probe.']};js(d/'verification.json',verification);js(d/'comparison-data.json',a);js(d/'early-restart.json',load(r/'build/early-restart092.json'))
data={'analysis':a,'finding':finding,'testSummary':test_summary,'replays':replays};js(d/'explorer-data.json',data)
html=(r/'build/guide092.html').read_text(encoding='utf8').replace('__DATA__',json.dumps(data,separators=(',',':')).replace('</','<\\/'))
extra=''.join('<script id="'+name+'" type="application/json">'+json.dumps(value,separators=(',',':')).replace('</','<\\/')+'</script>' for name,value in [('system-data',system),('inventory-data',inventory)])
write(d/'index.html',html.replace('</html>',extra+'</html>'))
table='\n'.join(f"| {x['bank']} | {x['lifecycle']} | {sec(x['referenceMillis'])} | {sec(x['reuseMillis'])} | {sec(x['savingMillis'])} |" for x in a['pairs'])
audittable='\n'.join(f"| {x['pass']} | {sec(x['callerMillis'])} | {x['reads']:,} | {x['canonicalChecks']:,} | {x['hits']:,} |" for x in a['auditPasses'])
readme=f'''# Edition 092 — Growing banks and restarted tasks

[Previous: edition091](README-091.md) · [README history](docs/readmes/INDEX.md) · [Offline evidence explorer](docs/readmes/092/index.html) · [Declared protocol](docs/EDITION_092_CONTRACT.md)

Client date: **2026-11-08 · America/Los_Angeles**. Images below are actual captures of this edition's isolated app and synthetic test data.

## What this edition establishes

{finding}

Edition091 made one repeated part of verification cheaper: calculating the canonical digest of an unchanged native motion record. Edition092 asks whether those savings survive a growing memory bank and a real interruption. It grows a bank through eight newly executed, verified procedures, then compares full reference checking with digest reuse at banks of one, four and eight receipts. At each size it measures both an uninterrupted task and a task stopped during actual motion, reopened and explicitly resumed.

This is a measurement and recovery-validation edition. **All333 compiled production Java engine classes are byte-identical to091.** The new work consists of a prospective experiment, a real application harness, independent accounting, regression checks and inspectable evidence. No dummy handler, replacement physics result, guessed screenshot or cached success verdict was added. Source identity is unchanged because the executable is unchanged; no older evidence is relabelled as a fresh092 execution.

The application remains a local agent workspace. Java owns durable goals, procedural memory, world state and verification. The desktop presents chat, the browser, canvas and runtime views. A remembered procedure can guide a task, but successful execution still needs an original motion record, a validated outcome and a committed world revision. The purpose of this phase is to make that accountability practical as retained experience grows.

## Recovery limit discovered first

An earlier attempt resumed actual stage0 successfully, but the recovered motion slightly changed the starting state of stage1. Its precomputed forecast therefore had a different exact source binding. The unchanged verifier refused the stage comparison and no world revision was committed. The difference included approximately0.066mm of bystander position and small residual velocities; numerical size does not authorize changing a source hash or waiving a check.

The completed comparison matrix below deliberately interrupts final stage1, which has no later dependent forecast. This scope was selected **after** observing the earlier failure, so these are diagnostic results, not held-out evidence of general restart recovery. The full earlier attempt and an independent refusal inspection are archived. No production check was weakened to obtain a pass. **Early-stage recovery needs a fresh forecast from the recovered state before the next actual stage. That is the immediate next implementation.**

[Early restart refusal evidence](docs/readmes/092/early-restart.json)

## Complete task measurements

| Verified receipts | Lifecycle | Reference caller time | Reuse caller time | Observed saving |
|---:|---|---:|---:|---:|
{table}

Positive saving means less elapsed time with reuse; negative saving means a slower observed run. These are six paired observations on one machine, not population estimates or a latency promise. The same first receipt is explicitly selected in each comparison. All measured tasks use the same existing long-trip scene family. Preparation variants introduce small declared changes in mass, friction and target position; they are similar memories, not eight unrelated task families.

The Java process is limited to **two actual OS-affined processors and512MiB heap**. Affinity, runtime profile and resource snapshots are recorded at each boot. Electron and the operating system are outside that Java budget. Each measured task starts after a new application boot with an empty fingerprint table; we do not silently compare a warmed arm against a cold arm. Mode order alternates across sizes and lifecycle conditions.

The bank and project history grow together. Consequently, the differences between the one-, four- and eight-receipt rows cannot isolate the causal cost of bank size. Explicit receipt selection also means this is not a benchmark of searching every bank member or of the calibrated selector choosing the best memory. It measures the same policy's full work with different retained histories and cold starts.

## What restart actually means here

Six comparison goals were interrupted while the final native actual stage was moving, after a recorded tick of at least1200. The isolated app was closed, Java restarted and the goal reopened as interrupted. The unchanged world head and pending owner were checked before explicit resume. The tests then confirmed that every original attempt ID and saved sample prefix survived, the uncertain interruption tail was charged conservatively, and exactly one verified world revision was committed.

This is stronger than reopening a completed report: the motion was unfinished at closure. It is still a **graceful application interruption**, not a power-failure or abrupt process-kill test. The earlier abrupt-recovery tests remain historical evidence;092 does not claim to repeat them.

Caller time is measured by the surviving test process from request through completion. It includes app closure, restart, OS pinning, prefix export and explicit resume. The in-app taskCost080 complete timer is unavailable after process restart: totalMillis is0. It remains **ineligible for calibration**, has no promoted clock089 record, and is never substituted for the whole caller interval. Independent inspection verifies this distinction.

Reopening and exporting the interrupted prefix use ordinary production reuse in both arms. The requested reference or reuse mode is restored before actual resume. That common recovery work is included in time and added explicitly to the observed counter totals. Reference execution segments require zero hits; the common recovery operation may have hits. The last pre-close counter snapshot cannot prove how many reads shutdown might perform afterward; full caller time still includes the entire closure. This boundary is documented rather than presenting partial counters as a perfect process-wide profiler.

## A working set larger than the fingerprint table

After execution, the study audits all twenty completed goals in three ordered passes. The original working set contains **{a['distinctOriginalEnvelopes']} distinct native envelopes**, exceeding the unchanged64-entry table.

| Audit pass, twenty goals each | Elapsed | Original reads | Canonical checks | Digest hits |
|---|---:|---:|---:|---:|
{audittable}

The second reuse pass is not assumed to be fully warm. A finite table evicts older fingerprints as the reader walks through more than64 distinct records. Every reread still hashes all file bytes and decodes fresh JSON; a miss requires a new canonical calculation. The audit checks that all three passes return the same original result for each goal, with no failure or changed world outcome. This distinguishes bounded reuse from retaining an unlimited proof cache.

All source and outcome checks remain active: owned paths and size bounds; complete envelope bytes; current goal/attempt/digest identities; original source and action plan; native chronology and charged work; final positions and protected bodies; forecast-versus-actual comparison; completed report and cost identity; and a single persistent-world commit. A same-size mutation with the original modification time is made to a warmed, resumed goal's native envelope. Verification must refuse it. Exact byte restoration must restore verification.

## Costs outside the paired task totals

Eight native preparations took **{sec(a['preparationMillis'])}**. Initial boots outside task intervals took **{sec(a['initialBootMillisOutsideTasks'])}**. Final original exports took **{sec(a['exportMillis'])}** and the individual full post-task checks took **{sec(a['postTaskVerifyMillis'])}**. The three later twenty-goal audit passes are listed separately above.

Restart boots totalled **{sec(a['restartBootMillisAlreadyIncludedInTaskTimes'])}**, already included in restarted caller times. They must not be added again. None of these preparation, export or inspection costs is hidden inside a claim of free learning. They purchase test data and assurance; whether calibration or another memory policy repays its own cost remains a separate experiment.

## Verification and original evidence

{test_summary}

The nine focused Java classes exercise native envelope integrity, forged outcomes, cost freshness, reconstruction compatibility, procedure behavior, proof reuse, world ledger and resource limits. The optional symbolic-link creation case is recorded as skipped when Windows denies creating the fixture. Edition091's separate successful junction test remains linked historical evidence; it is not counted as a fresh092 check. The existing091 broad suite is likewise not relabelled as rerun.

The first independent092 inspection stopped because its new resumed-cost check incorrectly compared the full archived cost object with the deliberately smaller goal summary. That was corrected to check the exact summary projection. Inspection also checks the actual restart contract: totalMillis is0 because the original monotonic start clock is unavailable, executionMillis matches retained active time, and the record is excluded from calibration. The failed inspector log is retained; no production code or measured result changed.

The first study attempt completed three goals and interrupted a fourth, then stopped on an incorrect harness assertion: a newly booted recovery process had already checked an original during startup, so its post-startup fingerprint table contained one entry. That did not establish cache persistence or lost verification. The corrected harness permits entries backed by the new process's own canonical checks and charges constructor recovery from zero. The entire twenty-goal matrix was restarted with a fresh project; the first attempt remains archived separately and is excluded from its paired totals. Production Java was unchanged throughout.

Five new accounting tests reject counter resets inside a segment, missing reads, invalid timings, omitted prefix-export work, falsely eligible resumed costs and changed paired tasks. The independent inspector recalculates source/plan hashes, native motion metrics, charged work and ledger commits from exact originals. It is an independent evidence reader, not a second physics solver. Tests of the existing desktop pass; no fresh LLM, webcam or physical-accuracy claim is made in this phase.

[Verification and limitations](docs/readmes/092/verification.json) · [Complete comparison data](docs/readmes/092/comparison-data.json) · [Original compressed evidence with raw hashes](docs/readmes/092/original-evidence.json) · [Source inventory](docs/readmes/092/FILE_TREE.txt) · [Obsidian system map](docs/obsidian/100-growing-bank-restarts.md)

## Actual integrated app

![Actual eight-receipt resumed goal report](docs/readmes/092/bank-eight-resumed-1600.png)

*Actual app capture,2026-11-08,window1600×1100 (content1584×1061). A verified task after an actual interruption, with an eight-receipt bank. The capture shows the existing report surface; it does not imply a new bank-management UI.*

![Actual eight-receipt uninterrupted report at narrower width](docs/readmes/092/bank-eight-uninterrupted-1100.png)

*Actual app capture,2026-11-08,window1100×1100 (content1084×1061). An uninterrupted comparison task and its saved report. All four capture files are included in the original evidence index. The center canvas is the selected report; the right goal panel and timeline retain the most recent task. Controller v1’s remembered-episode counter is a separate memory system, not the world-procedure bank count.*

The offline companion displays every comparison, separates recovery costs, and lets you inspect exact retained native samples. Its top-down projection is a view of recorded data, not regenerated physics. The original records retain full state and rotation even though the compact projection displays body extents without full3D rotation. The guide has no CDN or model dependency.

## Run and reproduce

Use Java21 and the existing Node/Electron dependencies. Launch the existing app with `Launch-Agent-Workspace.bat`, or use `scripts/Start-Agent-Workspace.ps1` with a distinct project directory. Preserve any live installation and private database. This edition used `D:/Codex-Work/fraynix-workspace-edition092`; the original live045/qa046 projects were not migrated or overwritten.

For an isolated reproduction, build `:workspace-engine:installDist`, run `node scripts/Study-Build092.cjs --stamp`, run `node workspace-desktop/test/envelopes092-integration.cjs`, then run `node scripts/Inspect-Envelopes092.cjs`. Keep a fresh output directory and retain failed attempts. The prospective scene matrix is in `workspace-desktop/test/fixtures/procedures092-protocol.json`. The build receipt binds installed artifacts and inputs before and after the experiment.

In ordinary use, select a world and an available verified procedure, request the arrangement, and inspect the resulting report and world revision. Resume an interrupted goal explicitly. There is no new speed toggle in the product. The existing diagnostics route reports work counters; changing reference/reuse mode remains restricted to an idle test runtime. Reuse never means disabling outcome verification.

## Direction and next acceptance gates

```mermaid
flowchart LR
  Request[Chat or world request] --> Goal[Durable Java goal]
  Bank[Verified memory bank] --> Goal
  Goal --> Motion[Forecast and actual motion]
  Motion --> Evidence[Original source and outcome checks]
  Evidence --> Commit[Single verified world commit]
  Motion --> Interrupted[Save interrupted ownership]
  Interrupted --> Resume[Reopen and explicit resume]
  Resume --> Evidence
  Commit --> Bank
  Evidence --> Measure[Whole-task cost and bounded counters]
```

First, implement bounded reforecasting after an early-stage restart. Preserve the prior forecast as evidence, require a new source-bound forecast before later actual motion, charge its cost, and test changed-state refusals plus successful recovery. Do not normalize away measured differences or relax identity checks.

Then measure **calibrated procedure selection** with fixed project snapshots, varied eligible procedures and randomized paired order. Count bank preparation, stale-cost checking, calibration fitting and task completion together. Acceptance requires a verified improvement in complete-workload cost or task success; a faster isolated digest does not automatically prove the selection policy earns its overhead.

Then profile remaining fresh JSON decoding and native evidence arithmetic under a larger working set. Any optimization must preserve source identity, per-call fresh data and every outcome check, with mutation, restart and eviction tests. Prefer lower allocation and bounded streaming where profiling supports it; do not replace current verification with remembered success.

Finally broaden task families and restart conditions. Independent obstacles, payload distributions and resource budgets should be declared before measuring. Separate familiar-family diagnostics from held-out results, retain failure traces and keep partial recovery clocks outside calibration. The destination remains an agent whose abilities improve measurably and survive interruption, with evidence a user can inspect. This edition establishes a bounded piece of that path, not general intelligence, consciousness or new neural weight training.

Publication is recorded separately after an actual remote fetch and Git-tree audit. Frozen001–091 documentation is preserved; this092 snapshot records its own source and measured originals.
'''
write(r/'README-092.md',readme)
write(r/note,f'''# Edition092 — growing banks and restarted tasks

[[README-092.md|Full edition]] · [[docs/readmes/092/index.html|Offline evidence]] · [[docs/obsidian/99-native-envelope-verification.md|Prior edition]]

{finding}

{test_summary} All333 production engine classes are byte-identical to091. Eight fresh preparations, twelve measured tasks, six true in-flight interruptions. Original native IDs/sample prefixes and one commit preserved. Resumed partial clocks remain excluded from calibration. Bounded64-entry table is tested against{a['distinctOriginalEnvelopes']} original envelopes.

Scope: small similar synthetic bank, one host and existing scene family. Common reopen/prefix recovery uses production reuse in both arms. No new weights, physical observations or calibrated-selection claim. Known gap: early-stage restart changes the next source state and is refused. Next: bounded fresh forecast from recovered state before later actual motion, then calibrated-selection comparisons.

[[workspace-engine/src/main/java/fraymus/workspace/NativeEnvelope091.java]] → [[workspace-engine/src/main/java/fraymus/workspace/WorldProcedure079.java]] → [[scripts/Inspect-Envelopes092.cjs]]
''')
nodes=[{'id':m['id'],'type':'file','file':m['source'],'x':i*390,'y':0,'width':350,'height':260} for i,m in enumerate(modules)];nodes.append({'id':'evidence','type':'file','file':'README-092.md','x':390,'y':330,'width':740,'height':340})
edges=[{'id':'e'+str(i),'fromNode':modules[i]['id'],'fromSide':'right','toNode':modules[i+1]['id'],'toSide':'left'} for i in range(3)];js(r/'docs/obsidian/100-growing-bank-restarts.canvas',{'nodes':nodes,'edges':edges})
continuity=f'''## Edition092 — locally verified; publication pending

{finding}

{test_summary} All333 compiled production classes exactly match091. No runtime policy/controller/verifier change. Actual2CPU/512MiB; eight fresh preparations, twelve comparisons at banks1/4/8, six in-flight graceful interruptions,60 full repeated audits, restored mutation probe. Native IDs/prefixes and single commits verified. Resumed clocks remain ineligible for calibration. Working set{a['distinctOriginalEnvelopes']} envelopes exceeds64. Reopen/prefix recovery uses production reuse in both arms and is charged; counter windows exclude any post-snapshot shutdown reads.

Isolated D:/Codex-Work/fraynix-workspace-edition092; private eyeoverthink/Fraynix-Agent-Workspace. Study source{rec['source']}; inspector/README contain later accounting clarification. Preserve live045/qa046/private data and frozen001–091. No subagents. No calibrated selection, LLM weight training or physical accuracy claim. Next: bounded fresh forecasting from recovered state before later actual motion; then fixed-snapshot randomized calibrated-selection workloads including setup cost.

'''
for name in ['AGENTS.md','docs/PROJECT_MEMORY.md','docs/DEVELOPMENT_LOG.md']:write(r/name,continuity+(r/name).read_text(encoding='utf8'))
root=(r/'README.md').read_text(encoding='utf8').replace('**Latest: [edition091','**Earlier edition: [edition091',1);root=root.replace('# Fraynix Agent Workspace\n','# Fraynix Agent Workspace\n\n**Latest: [edition092 — growing banks and restarted tasks](README-092.md).**\n\n'+finding+' [Offline evidence](docs/readmes/092/index.html).\n',1);write(r/'README.md',root)
for name,prefix in [('docs/ARCHITECTURE.md','Edition092 validates the unchanged091 runtime against growing verified banks and actual interrupted tasks. Whole caller time includes restart; partial resumed costs remain excluded from calibration. [Results and limits](../README-092.md).\n\n'),('docs/obsidian/00-home.md','[[README-092.md|Edition092 — growing banks and restarts]] · [[docs/obsidian/100-growing-bank-restarts.md]]\n\n'),('docs/readmes/INDEX.md','[Edition092 — growing banks and restarted tasks](../../README-092.md) · [Offline evidence](092/index.html)\n\n')]:write(r/name,prefix+(r/name).read_text(encoding='utf8'))
print(json.dumps({'finding':finding,'tests':tests,'logicalOriginals':len(archive),'uniquePayloads':len(local),'sources':len(inventory),'replays':len(replays)},indent=2))
