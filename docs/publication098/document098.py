from pathlib import Path
import json,hashlib,gzip,subprocess,shutil,xml.etree.ElementTree as ET,datetime
r=Path.cwd();d=r/'docs/readmes/098';d.mkdir(parents=True,exist_ok=True)
def load(p):return json.loads(p.read_text(encoding='utf-8-sig'))
def write(p,s):p.parent.mkdir(parents=True,exist_ok=True);p.write_text(s,encoding='utf8',newline='\n')
def js(p,v):write(p,json.dumps(v,indent=2,ensure_ascii=False)+'\n')
def sha(b):return hashlib.sha256(b).hexdigest()
a=load(r/'build/analysis098.json');run=load(r/'build/ui098-study/result.json');assert a['passed'] and run['passed']
ci=load(r/'build/ci098-status.json');detail=load(r/'build/detail098.json');assert detail['passed'] and detail['worldUnchanged'];captures=run['captures']+detail['captures'];classes=[]
for p in sorted((r/'build/unit098-final').glob('TEST-*.xml')):
 x=ET.parse(p).getroot();classes.append({k:x.attrib[k] for k in ['name','tests','failures','errors','skipped','time']})
assert classes and not sum(int(x['failures'])+int(x['errors']) for x in classes)
regressions=load(r/'build/regression098-summary.json');assert regressions['passed'];identity=load(r/'build/source-comparison098.json');assert identity['changed']==['fraymus/workspace/WorldProcedure079.class'] and identity['new']==['fraymus/workspace/TaskCost098.class'] and not identity['removed']
verification={'passed':True,'studySource':run['source'],'java':classes,'productionClasses':identity,'detailCapture':detail,'regressions':regressions,'parentCI':ci,'analysis':a,'scope':'Local original/outcome verification; CI status is separate. Synthetic native tasks, no LLM inference or physical accuracy claim.'}
js(d/'verification.json',verification);js(d/'analysis.json',a)
archive=[];unique={}
def pack(p,label):
 raw=p.read_bytes();h=sha(raw)
 if h not in unique:
  z=gzip.compress(raw,mtime=0);dest=d/'original'/(h+'.gz');dest.parent.mkdir(parents=True,exist_ok=True);dest.write_bytes(z);unique[h]={'path':dest.relative_to(r).as_posix(),'bytes':len(z),'sha256':sha(z),'rawBytes':len(raw),'rawSha256':h}
 archive.append(dict(unique[h],logicalPath=label))
for p in (r/'build/ui098-study').rglob('*'):
 if p.is_file() and 'desktop-profile' not in p.parts and p.name!='host.json' and p.suffix in ['.json','.jsonl','.fworld','.pending','.html','.css','.js','.csv','.log','.png']:pack(p,'study/'+p.relative_to(r/'build/ui098-study').as_posix())
for p in (r/'build').glob('*098*'):
 if p.is_file() and p.suffix in ['.json','.log','.py','.md','.cjs']:pack(p,'validation/'+p.name)
for folder in ['unit098-original','unit098-final','regression098']:
 for p in (r/'build'/folder).glob('TEST-*.xml'):pack(p,folder+'/'+p.name)
for c in captures:shutil.copyfile(r/'build/ui098-study'/c['file'],d/c['file'])
js(d/'original-evidence.json',{'files':archive,'distinctNewPayloads':len(unique),'scope':'Exact isolated synthetic originals and validation. No private projects, desktop profiles, credentials, webcam images or model weights.'})
prior=load(r/'docs/readmes/097/file-inventory.json');kinds={x['path']:x['hashKind'] for x in prior};paths=set(kinds)
paths.update(['scripts/Interrupt-Study098.ps1','docs/EDITION_098_CONTRACT.md','workspace-engine/src/main/java/fraymus/workspace/TaskCost098.java','workspace-engine/src/test/java/fraymus/workspace/TaskCost098Test.java','workspace-desktop/test/benefit098-integration.cjs','workspace-desktop/test/fixtures/procedures098-protocol.json','scripts/Inspect-Procedures098.cjs','scripts/Check-Benefit098.cjs','workspace-engine/src/test/java/fraymus/workspace/WorldProcedure098Test.java'])
inventory=[]
for p in sorted(paths):
 b=(r/p).read_bytes();kind=kinds.get(p,'utf8-lf');b=b.decode('utf8').replace('\r\n','\n').encode() if kind=='utf8-lf' else b;inventory.append({'path':p,'bytes':len(b),'sha256':sha(b),'hashKind':kind})
js(d/'file-inventory.json',inventory)
note='docs/obsidian/106-benefit-selection.md'
system={'modules':[{'id':key,'name':name,'source':src,'note':note} for key,name,src in [('policy','Current task benefit','workspace-engine/src/main/java/fraymus/workspace/TaskCost098.java'),('runtime','Java execution and original verification','workspace-engine/src/main/java/fraymus/workspace/WorldProcedure079.java'),('study','Matched app tasks','workspace-desktop/test/benefit098-integration.cjs'),('inspect','Independent original checks','scripts/Inspect-Procedures098.cjs')]],'screenshots':[{'path':'docs/readmes/098/'+c['file'],'caption':'Actual098 '+c['file']} for c in captures],'evidence':[{'path':'docs/readmes/098/'+n,'caption':n} for n in ['analysis.json','verification.json','original-evidence.json']]}
js(d/'system-map.json',system)
sec=lambda n:f'{n/1000:,.2f} s'
table='\n'.join('| '+' | '.join([x['case'],sec(x['simpleMillis']),sec(x['adaptiveMillis']),sec(x['memoryMillis']),'memory' if x['selectedMemory'] else 'simple first',sec(x['adaptiveSavingVsSimple']),sec(x['adaptiveSavingVsMemory'])])+' |' for x in a['comparisons'])
lead=f"Default adaptive requests now require evidence that remembered procedures repay their extra work. {len(run['evaluations'])}/{len(run['evaluations'])} fresh evaluation tasks completed, with {sum(bool(x['options'].get('recovery')) for x in run['evaluations'])} explicit recoveries and {sum(bool(x['goal'].get('savedSkill')) for x in run['evaluations'])} saved skills."
shots='\n\n'.join(f"![Actual app report: {c['file']}](docs/readmes/098/{c['file']})" for c in captures)
readme=f'''# Edition 098 — Memory must repay its work

[Previous:097](README-097.md) · [Offline results](docs/readmes/098/index.html) · [All editions](docs/readmes/INDEX.md) · [Contributor direction](README-CONTRIBUTORS.md)

{lead}

## The change users get

Edition 097 found that a warm lookup could still choose unnecessary stages for a small adjustment. The default adaptive path previously used work avoided during an older task. Java now requires the existing repeated, verified **complete-task** cost model, matching runtime, receipt, proof state, workload and memory-bank coverage. It retains execution-order, repeat-variation, geometry and recent-cost freshness checks. Without sufficient evidence, it tries simple control first and keeps the original bounded fallback procedures. No automatic calibration job spends resources merely because a memory exists.

The added coverage check compares remaining translation and rotation for **each target**. Neither is diluted among unrelated scene features. A measured/current magnitude must be within a factor of two, with floors of 0.1 metre and 5 degrees (the existing task tolerances). These are conservative coverage rules, not learned physical laws or changed success thresholds.

The pre-decision evidence pass has an owned monotonic timing receipt. It covers candidate collection, model validation and recent-cost collection before the final choice; the final calculation and receipt write are covered by the reserve and remain included in actual caller/Java task times. Before selecting memory, Java subtracts an allowance for the initial pass and the runner, goal and commit audits; retention and skill saving each add a pass. Each future pass is provisionally charged at the observed first-pass duration, plus max(500ms,25%) reserve. Future checks are **estimates**, not measured future durations or a probability guarantee. A positive remaining lower saving is required. Reports expose the reason, first-pass time, total allowance and net lower saving. The decision receipt is rechecked against its saved bytes and included in later proof evidence.

Explicit simple and memory modes remain available as comparison controls. Explicit memory means a requested procedure order, not a prediction of benefit. Every forecast, distinct actual execution, source binding, protected-body check, report verification and single world commit remains authoritative. Memory never certifies a new outcome.

## Fresh measured results

The [prospective contract](docs/EDITION_098_CONTRACT.md) and [fixture](workspace-desktop/test/fixtures/procedures098-protocol.json) preceded the cohort. Source: `{run['source']}`. All scenes are clearly labelled synthetic boxes, not real camera observations. The six-body geometry is related to earlier 090 fixtures, not an unfamiliar-domain generalization test.

One fresh preparation earned a procedure under the current Java implementation. Six calibration comparisons used two different target sets, three repeats each, both execution orders, the same Java clock owner and the original outcome checks. The existing calibration-admission gate declined fitting: its supported lower saving did not cover variability and the declared allowances. No model was installed. The preparation and that decline were then frozen before evaluation. Positive calibrated admission is covered by Java boundary tests; this native cohort tests the default fallback without a fitted model. Each of the 12 evaluations opened an exact byte-checked copy of that preparation, performed a charged warm-up, and used matching scene/targets for simple, adaptive and explicit-memory modes. Four cases cover a small move, long route, small move with skill saving, and interrupted long route. The skill workload has no matching calibration and cannot borrow the unsaved-task estimate. Execution order rotates; this is one descriptive block, not a confidence study. In the calibration batch, the minimum mean saving was {sec(a['fitReceipt']['admission']['minimumGroupSavingMillis'])}, while the variability allowance was {sec(a['fitReceipt']['admission']['variabilityAllowanceMillis'])}; the lower saving was {sec(a['fitReceipt']['admission']['lowerSavingMillis'])} before the additional checking allowance.

| Case | Simple caller time | Adaptive caller time | Explicit memory | Adaptive choice | Saved vs simple | Saved vs memory |
|---|---:|---:|---:|---|---:|---:|
{table}

Positive savings mean adaptive took less time. Negative numbers remain costs; no faster-than-simple claim follows merely from selecting simple first. Measured preparation components total **{sec(a['measuredPreparationComponentsMillis'])}**; the fit-admission API call took {sec(a['fitMillis'])}. The initial harness incorrectly expected a model from the declined response and stopped after all 13 native preparation tasks completed. Its checkpoint, script and error are retained. The corrected continuation reverified those actual goals and respected the decline. The original whole-preparation timer ended with that harness, so this is a component subtotal: measured rows, first launch, declined-fit call and continuation verification. Original intervening gaps and first shutdown remain unavailable. Evaluation has a new owned clock. These costs are not amortized away, and no full-setup payback claim is made. The [full measurements](docs/readmes/098/analysis.json) include caller, Java, lookup and charged native work plus complete evaluation durations covering copy/startup/warm-up, extra audits, shutdown and reopened verification. Screenshots are outside timing. The recovered tasks' external caller times remain excluded from Java calibration; zero/unavailable cross-boot durations never become free training examples.

A second harness failure occurred before the first deliberate process interruption: the edition 097 helper correctly rejected the edition 098 folder. Its closed project, completed extra warm-up and unfinished task are preserved. All three restart comparisons were then run afresh with an edition 098-specific path allowlist and unchanged process-identity checks. The nine prior evaluation intervals remain original. The abandoned warm-up caller time was {sec(a['abandonedAttempts'][0]['callerMillis'])}; the abandoned partial task's complete outer duration is unavailable. This extra work is excluded from the 37 planned tasks and 12 matched evaluations, disclosed separately, and cannot support a preparation-payback claim. The inspector additionally verifies the abandoned warm-up's originals. The retry reused its export filename; the independent inspector detected the mismatch. Its diagnostic bundle was repacked from the preserved checked source envelopes, matching the original saved job and cost exactly. Those source bytes, provenance and first inspection failure remain archived; no motion or result was regenerated.

Read each recorded reason in the offline explorer. Choosing memory only with supported benefit is a conservative policy; it can decline useful memory when coverage or timing is insufficient. This edition does not establish universal payoff, learned optimality or a guarantee about future machine load.

## Verification and CI

{sum(int(x['tests'])-int(x['skipped']) for x in classes)} focused Java methods passed. The additional six native/integrity classes passed {regressions['javaPassed']} methods with {regressions['javaSkipped']} host-dependent case skipped: {sum(int(x['tests'])-int(x['skipped']) for x in classes)+regressions['javaPassed']} Java passes in total. All {regressions['desktop']['pass']} desktop tests passed. Exact class counts and logs are recorded in [verification.json](docs/readmes/098/verification.json). The independent inspector checked {a['verified']} original tasks (37 planned plus one preserved diagnostic warm-up) and refused {len(a['refusals'])} altered decision/source/outcome cases. Every evaluated world and saved-skill identity was checked after reopening. All three declared abrupt Java interruptions kept saved prefixes, explicit recovery, conservative tail charging and fresh downstream forecasts when required. No threshold was relaxed.

The parent 097 GitHub run 42571456884 initially failed **before compilation**, when the pinned Mesa download returned HTTP504. The original failure is archived. Edition 098 adds three bounded transport retries and retains the exact checksum and extraction checks. At documentation capture, attempt {ci.get('run_attempt')} was `{ci.get('status')}` / `{ci.get('conclusion')}`. Local results do not imply a successful full CI run; the new main workflow is a separate status after publication.

## Actual application views

These captures show real integrated task reports from the 098 test projects. The displayed model name does not mean an LLM ran. The Controller v1 episode count is a separate legacy bank, not the one-receipt procedural bank used here. The left world/game/library and right Java goal panel belong to the existing shared application; no new disconnected demo app was substituted.

{shots}

## Run and inspect

Use JDK21 and the repository's Gradle wrapper, Node and the pinned desktop dependencies. Docker/WSL is not required. Keep live installations in their own projects. A new review project starts without this experiment's calibration or memories.

```powershell
$env:FRAYNIX_WORKSPACE_PROJECT = Join-Path (Get-Location).Path 'build/review098/project'
$env:FRAYNIX_WORKSPACE_PERSISTENT = '0'
./scripts/Start-Agent-Workspace.ps1
```

In the integrated spatial/world controls, use adaptive selection for normal tasks. Inspect its report's memory decision and evidence before interpreting the result. The chat tools use the same Java-owned world-goal path; this cohort exercised deterministic registered tools, not new model inference. Optional chat still requires a configured available model. Earlier editions document the browser, creation canvas, original Game Lab, shared brain and observation systems.

For reproduction, build isolated current artifacts, run source isolation, stamp the build, then run `workspace-desktop/test/benefit098-integration.cjs` and `scripts/Check-Benefit098.cjs`. The harness refuses to overwrite `build/ui098-study`; keep old evidence or choose a separately declared output before rerunning. Heavy work is serial with 2 reported/pinned CPUs and 512 MiB Java heap. Source-bound memories from an older implementation are not relabelled; earn fresh receipts under the current code. Of 334 prior production engine classes, 333 are byte-identical. WorldProcedure079 integrates the policy and its timing evidence; TaskCost098 is new. Native physics/controller classes are unchanged.

## Next

Use these complete costs to tighten the decision/audit allowance and avoid expensive estimation when evidence already cannot support reuse. Repeat the matched cases with different bank sizes and machine load, including short tasks where memory might truly help. Calibrate saving/recovery separately; durable cross-boot timing still needs an independently tested contract. Keep the destination one useful local Java agent that completes requests, observes outcomes and retains verified skills—not an expanding collection of benchmark windows.
'''
write(r/'README-098.md',readme)
write(r/'README.md','# Fraynix Agent Workspace\n\n'+lead+'\n\n[Edition 098: benefit-aware memory](README-098.md) · [Interactive offline results](docs/readmes/098/index.html) · [Previous edition](README-097.md) · [Contributor direction](README-CONTRIBUTORS.md)\n\nOne integrated local workspace: Java owns durable goals, physics, tools, verification and remembered procedures. Chat, browser, creation canvas, original Game Lab and native spatial views share those capabilities.\n\n'+f"![Actual098 report](docs/readmes/098/{captures[0]['file']})\n")
write(r/note,'# Benefit-aware memory — edition098\n\n'+lead+'\n\n[[README-098.md]] · [[docs/EDITION_098_CONTRACT.md]] · [[docs/readmes/098/verification.json]]\n')
nodes=[{'id':m['id'],'type':'file','file':m['source'],'x':i*380,'y':0,'width':350,'height':240} for i,m in enumerate(system['modules'])];js(r/'docs/obsidian/106-benefit-selection.canvas',{'nodes':nodes,'edges':[{'id':'e'+str(i),'fromNode':nodes[i]['id'],'fromSide':'right','toNode':nodes[i+1]['id'],'toSide':'left'} for i in range(len(nodes)-1)]})
for p,prefix in [('docs/obsidian/00-home.md','[[README-098.md|Edition 098 — benefit-aware memory]] · [[docs/obsidian/106-benefit-selection.md]]\n\n'),('docs/readmes/INDEX.md','[Edition 098 — benefit-aware memory](../../README-098.md) · [Offline evidence](098/index.html)\n\n')]:
 old=(r/p).read_text(encoding='utf8');write(r/p,prefix+old if prefix not in old else old)
entry='## Edition 098 — locally verified; publication pending\n\n'+lead+f"\n\nNative source{run['source']}; isolated S:/Codex-Work/fraynix-workspace-edition098. {a['verified']} original tasks independently inspected; {len(a['refusals'])} altered-evidence refusals. See README098 for actual comparative costs, conservative allowances and limits. Current parent CI attempt {ci.get('run_attempt')}: {ci.get('status')}/{ci.get('conclusion')}; original HTTP504 failure retained. Keep Java ownership, every source/outcome check, private live data and frozen001–097. No subagents; heavy work serial. Main/tag publication and fetched receipt still pending.\n\n"
for p in ['AGENTS.md','docs/PROJECT_MEMORY.md','docs/DEVELOPMENT_LOG.md','docs/ARCHITECTURE.md','README-CONTRIBUTORS.md']:
 old=(r/p).read_text(encoding='utf8');write(r/p,entry+old if entry not in old else old)
p=r/'README-CONTRIBUTORS.md';text=p.read_text(encoding='utf8').replace('For edition097, the isolated source checkout is `S:/Codex-Work/fraynix-workspace-edition097`, branch `edition097`, based on verified096 main `453e8708ab9353d86ed666f3d059d28563372530`.','For edition098, the isolated source checkout is `S:/Codex-Work/fraynix-workspace-edition098`, branch `edition098`, based on verified097 main `29129e6f085441ba860ec42f99c98fe4af0ce0eb`.').replace('[canonical source inventory](docs/readmes/097/file-inventory.json)','[canonical source inventory](docs/readmes/098/file-inventory.json)');write(p,text)
guide=(r/'build/guide098.html').read_text(encoding='utf8')
images=''.join(f'<figure><a href="{c["file"]}"><img src="{c["file"]}" alt="Actual098 {c["file"]}"></a><figcaption>{c["file"]}: actual synthetic-world task; no model inference.</figcaption></figure>' for c in captures)
for k,v in [('IMAGES',images),('ANALYSIS',json.dumps(a)),('SYSTEM',json.dumps(system)),('INVENTORY',json.dumps(inventory))]:guide=guide.replace('@@'+k+'@@',v)
write(d/'index.html',guide);print(json.dumps({'goals':a['verified'],'archiveEntries':len(archive),'distinctPayloads':len(unique),'sourceFiles':len(inventory)}))
