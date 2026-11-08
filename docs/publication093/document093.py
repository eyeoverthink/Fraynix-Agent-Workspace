from pathlib import Path
import json,hashlib,gzip,base64,subprocess,shutil,xml.etree.ElementTree as ET,os
r=Path.cwd();d=r/'docs/readmes/093';d.mkdir(parents=True,exist_ok=True)
def load(p):return json.loads(p.read_text(encoding='utf-8-sig'))
def write(p,s):p.parent.mkdir(parents=True,exist_ok=True);p.write_text(s,encoding='utf8',newline='\n')
def js(p,v):write(p,json.dumps(v,indent=2,ensure_ascii=False)+'\n')
def sha(b):return hashlib.sha256(b).hexdigest()
def sec(x):return f'{x/1000:.2f} s'
a=load(r/'build/analysis093.json');rec=load(r/'build/ui093-study/result.json');assert a['passed'] and rec['passed']
xml=list((r/'build/regression093').glob('TEST-*.xml'));assert xml
tests={'cases':0,'failures':0,'errors':0,'skipped':0,'classes':len(xml),'skips':[]}
for f in xml:
 t=ET.parse(f).getroot();tests['cases']+=int(t.attrib['tests'])
 for k in ['failures','errors','skipped']:tests[k]+=int(t.attrib.get(k,0))
 for c in t.findall('testcase'):
  if c.find('skipped') is not None:tests['skips'].append(c.attrib)
assert tests['failures']==tests['errors']==0
assert 'pass 135' in (r/'build/desktop093.log').read_text(encoding='utf-8-sig')
identity=load(r/'build/source-comparison093.json')
finding='Java now creates and verifies a distinct forecast from recovered state before later actual motion. Both declared early-restart cases completed; a second interruption during forecasting also recovered. Stopping during forecasting prevented the next actual stage and any world commit.'
test_summary=f"{tests['cases']-tests['skipped']} Java checks passed across {tests['classes']} focused classes; {tests['skipped']} optional host-dependent case skipped. 135 desktop checks passed. Five new native goals completed and one was deliberately cancelled."
replays=[]
for row in rec['rows']:
 for packed in load(r/'build/ui093-study'/row['originalFile'])['originals']:
  run=json.loads(gzip.decompress(base64.b64decode(packed['gzipBase64'])));attempt=next(x for x in row['job']['arm']['attempts'] if x['id']==run['id'])
  if not run['samples']:continue
  replays.append({'label':f"{row['kind']} / {attempt['family']} / {attempt['stage']}",'id':run['id'],'status':run['status'],'objects':run['definition']['objects'],'samples':[{'tick':s['tick'],'timeS':s['timeS'],'bodies':s['bodies']} for s in run['samples']]})
archive=[];local={}
def pack(p,logical):
 raw=(Path('\\\\?\\'+str(p)) if os.name=='nt' else p).read_bytes();h=sha(raw)
 if h not in local:
  dest=d/'original'/(h+'.gz');dest.parent.mkdir(parents=True,exist_ok=True);z=gzip.compress(raw,mtime=0);dest.write_bytes(z);local[h]={'path':dest.relative_to(r).as_posix(),'bytes':len(z),'sha256':sha(z),'rawBytes':len(raw),'rawSha256':h}
 archive.append(dict(local[h],logicalPath=logical))
for folder in sorted((r/'build').glob('ui093-*')):
 for p in folder.rglob('*'):
  if p.is_file() and 'desktop-profile' not in p.parts and p.name!='host.json' and p.suffix in ['.json','.jsonl','.log','.png','.html','.css','.js','.txt','.csv','.previous']:pack(p,folder.name+'/'+p.relative_to(folder).as_posix())
for f in xml:pack(f,'regression093/'+f.name)
for p in (r/'build').glob('*093*'):
 if p.is_file() and not p.name.startswith('document093.log') and p.suffix in ['.json','.log','.ps1','.py','.cjs','.html','.md','.txt']:pack(p,'validation/'+p.name)
for p in (r/'build/ui093-study').glob('*.png'):shutil.copyfile(p,d/p.name)
js(d/'original-evidence.json',{'files':archive,'distinctNewPayloads':len(local),'note':'Exact synthetic originals, checkpoints and logs. Raw and compressed hashes. Host credentials and desktop profiles excluded. Failed attempts remain distinct; no generated replacement observations.'})
old=load(r/'docs/readmes/092/file-inventory.json');paths={x['path'] for x in old};paths.update(x for x in subprocess.check_output(['git','diff','--name-only','--diff-filter=A','726ac0c30a2b9815c9ec91f681f2eab6460fadfb','HEAD'],text=True).splitlines() if x.startswith(('workspace-engine/','workspace-desktop/','scripts/','fraynix-modules/')))
kinds={x['path']:x['hashKind'] for x in old};inventory=[]
for name in sorted(paths):
 raw=(r/name).read_bytes();kind=kinds.get(name,'utf8-lf');raw=raw.decode('utf8').replace('\r\n','\n').encode() if kind=='utf8-lf' else raw
 inventory.append({'path':name,'bytes':len(raw),'sha256':sha(raw),'hashKind':kind})
js(d/'file-inventory.json',inventory);write(d/'FILE_TREE.txt','Edition093 canonical source inventory\n'+'\n'.join(x['path'] for x in inventory)+'\n')
note='docs/obsidian/101-recovered-state-forecast.md'
modules=[{'id':'runner','name':'Durable goal and actual stages','source':'workspace-engine/src/main/java/fraymus/workspace/WorldProcedure079.java'},{'id':'recovery','name':'Exact recovered forecast binding','source':'workspace-engine/src/main/java/fraymus/workspace/RecoveryForecast093.java'},{'id':'native','name':'Original native verification','source':'workspace-engine/src/main/java/fraymus/workspace/WorldVerify.java'},{'id':'inspection','name':'Independent source and outcome arithmetic','source':'scripts/Inspect-Recovery093.cjs'}]
for module in modules:module['note']=note
system={'modules':modules,'screenshots':[{'path':'docs/readmes/093/'+x['file'],'caption':'Actual093 report capture · 2026-11-08 America/Los_Angeles'} for x in rec['captures']],'evidence':[{'path':'docs/readmes/093/verification.json','caption':'Results and limits'},{'path':'docs/readmes/093/original-evidence.json','caption':'Original bytes'}]};js(d/'system-map.json',system)
verification={'passed':True,'analysis':a,'java':tests,'desktopPassed':135,'classes':identity,'screenshots':rec['captures'],'finding':finding,'source':rec['source'],'limits':[a['scope'],'Only two-stage recovered sequences exercised here. Three-stage budgeting is tested arithmetically; three-stage native recovery and abrupt power loss remain unvalidated.','Resumed totalMillis=0 means unavailable; it is excluded from calibration. Caller times include controlled restarts and test observations, so are not pure engine throughput.']};js(d/'verification.json',verification);js(d/'comparison-data.json',a)
data={'analysis':a,'finding':finding,'tests':test_summary,'replays':replays};html=(r/'build/guide093.html').read_text(encoding='utf8').replace('__DATA__',json.dumps(data,separators=(',',':')).replace('</','<\\/'))
extra=''.join('<script id="'+name+'" type="application/json">'+json.dumps(value,separators=(',',':')).replace('</','<\\/')+'</script>' for name,value in [('system-data',system),('inventory-data',inventory)])
write(d/'index.html',html.replace('</body>',extra+'</body>'))
table='\n'.join(f"| {x['kind']} | {'Completed' if x['success'] else 'Stopped'} | {x['resumes']} | {x['ticks']} | {x['forecastChargedTicks']} | {sec(x['forecastWallMillis'])} | {sec(x['callerMillis'])} |" for x in a['checks'])
readme=f'''# Edition 093 — Forecast from the world you recovered

[Previous: edition 092](README-092.md) · [All editions](docs/readmes/INDEX.md) · [Offline evidence explorer](docs/readmes/093/index.html) · [Current project](README.md)

**{finding}**

This edition closes the observed early-stage recovery gap without changing what counts as a successful prediction, a valid physical outcome, or permission to commit a world revision. A restart can restore a native physics checkpoint accurately enough to finish the current stage, while still changing a few values in its final state. The following stage must be predicted from those actual values. Treating the old prediction as current would break the link between a forecast and the world it describes.

The original092 refusal remains a correct historical result. Its archived evidence is [here](docs/readmes/092/early-restart.json). All093 successes are fresh executions under the new code identity, not upgrades of old outcomes.

## What changed in the application

Java still owns the durable task, bounded procedures, physics execution, proof checking and persistent world. The integrated desktop still presents chat, world tools, saved reports and the canvas. There is no extra window or external orchestration service for recovery.

Before each later actual stage, the runner compares its reconstructed starting scene and action plan with the original forecast. When they match exactly, the ordinary path continues. When they differ, continuation requires a completed, independently checked earlier actual stage with recorded native resume provenance. A mismatch without that provenance is refused.

The runner derives the next frame from the immediate previous actual result. It reserves enough of the existing goal budget, creates a new native forecast with a new identity, and checks its original outcome and protected-body trace before starting the later actual stage. It saves an explicit relationship between the original forecast, previous actual result, resumed ancestor, current frame and action plan. Those identities and content hashes are checked again during the final audit.

The original forecast sequence is retained unchanged. The replacement forecast is evidence of a new prediction from a changed starting point. It is never relabelled as actual motion. The actual stage remains a separate native run with its own samples and comparisons. Only the verified actual sequence can produce the final world revision.

```mermaid
flowchart LR
  A[Restore owned checkpoint] --> B[Finish and verify current actual stage]
  B --> C{{Does next source and plan match?}}
  C -->|Yes| D[Use original forecast]
  C -->|No, verified resumed ancestor| E[Reserve work and run fresh forecast]
  E --> F[Verify source, outcome and chronology]
  F --> G[Run distinct actual stage]
  D --> G
  G --> H[Compare, audit and commit once]
  C -->|Unbound change| X[Refuse continuation]
  F -->|Failed or stopped| X
```

## Measured outcomes

{test_summary}

The native study used one newly retained successful procedure and five further tasks on an isolated project, with two assigned logical processors and a 512 MiB Java heap. The conditions were declared in the [protocol](workspace-desktop/test/fixtures/procedures093-protocol.json) before these trials. The variant changes selected payload mass by 5%, friction by 0.02, and one target coordinate by −0.05 m. These are labelled synthetic rigid-body scenes with declared parameters, not camera observations or physical laboratory measurements.

| Case | Outcome | Explicit resumes | Total charged ticks | New forecast charged ticks | New forecast native wall time | Complete caller time |
|---|---|---:|---:|---:|---:|---:|
{table}

The seed is preparation, not a held-out comparison. The baseline required no replacement forecast. Both early actual-stage restart cases generated a fresh forecast before the second actual stage. The twice-restarted task was interrupted during that new forecast, then explicitly resumed with the same native identity and preserved sample prefix. It completed with one world commit. The cancellation case stopped the replacement forecast: no later actual stage started, no success was committed and no eligible cost record was produced.

These timings describe this local run. Complete caller time includes closure, reopening, resource pinning, reading interruption evidence and explicit resume. It excludes initial boot, later exports, screenshots and final audits. Native forecast wall time is the recorded attempt time across its runs. Charged ticks conservatively reserve a full 14,400 ticks for an attempt with an unknown interruption tail; observed ticks remain separately inspectable. These are different measures and should not be interchanged.

After a process restart, the original monotonic task clock cannot be recovered. The existing cost record deliberately has totalMillis=0 and eligible=false. Zero means unavailable, not instantaneous completion. Retained execution time and the surviving test caller clock provide separately labelled measurements. This edition does not train a cost fit on incomplete timing.

## Boundaries that remain enforced

One replacement is permitted per later stage. The original two explicit resumes, 115,200 charged ticks, 20-minute active goal and 90-second native-attempt limits remain. Admission reserves the new forecast and remaining actual work before starting it. The pure arithmetic test checks the exact tick boundary and refusal one tick beyond it. Three-stage reservation arithmetic is covered, but this native study exercises two-stage procedures only.

The full audit reconstructs the starting state, action plan, waypoint, original metrics, protected-body motion, forecast comparison, logical order, timestamps, ownership and charged work. The next actual attempt must be planned after the completed replacement forecast. A saved metadata claim alone cannot authorize execution. A stopped, failed, changed or unbound forecast cannot become a successful continuation.

Tests deliberately alter the original forecast reference, predecessor, resumed ancestor, frame and plan hashes, forecast identity, extra fields, stage number, chronology, admission reserve, charged work, interruption charge, status, metrics and waypoint. Those changes are refused. A warmed native-evidence read also refuses same-length original-byte corruption with its timestamp preserved; exact restoration restores verification. Cold reopening rechecks all five completed goals. The independent JavaScript reader separately reconstructs these bindings and checks native arithmetic and the single world commit from exported originals. It is not a second physics simulator.

The production change is limited to WorldProcedure079 and the new RecoveryForecast093 helper. The [compiled comparison](docs/readmes/093/verification.json) identifies the changed and unchanged classes. Original physics, tolerances, native checkpoint mechanics and world-commit rules remain. Code identity includes the new helper; old-code proofs stay historical.

## See it in the integrated app

![Actual early-restart report](docs/readmes/093/early-restart-1600.png)

Actual isolated093 application, captured 2026-11-08 in America/Los_Angeles after completion. The saved report states the additional recovery forecasts and charged work. Requested window 1600×1100; captured client area 1584×1061. The charts show the final actual stage, while the procedure export retains every original and replacement forecast.

![Actual twice-resumed task at narrower width](docs/readmes/093/reforecast-restart-1100.png)

Actual twice-resumed task, requested window 1100×1100; client area 1084×1061. Existing chat, Java brain, world controls and report remain in one application. The generic goal panel shows a three-recovery ceiling for the outer goal/report workflow; this native procedure separately enforces two execution resumes. The catalogue's displayed models are not evidence of new inference or model-weight training. These tests ran offline without asking an LLM to invent or certify a result.

To inspect the evidence, open the [offline companion](docs/readmes/093/index.html) locally. Filter completed and stopped tasks, select an original forecast, recovered forecast or actual stage, and step through retained native samples. The map is an X/Z projection of original positions, not a new physics run or a full rendering of3D rotation. Every replay is labelled by task, stage and native identity.

## Files and reproducibility

The [source inventory](docs/readmes/093/FILE_TREE.txt) and [hash manifest](docs/readmes/093/file-inventory.json) cover the canonical application sources. The [original archive index](docs/readmes/093/original-evidence.json) records compressed and raw-byte hashes for the generated evidence, including checkpoints and logs. Personal runtime state, credentials and model weights stay outside Git. Published 001–092 documents and screenshots remain frozen.

| Role | Source |
|---|---|
| Durable continuation and final audit | [WorldProcedure079.java](workspace-engine/src/main/java/fraymus/workspace/WorldProcedure079.java) |
| Recovered-state binding, reserve and chronology | [RecoveryForecast093.java](workspace-engine/src/main/java/fraymus/workspace/RecoveryForecast093.java) |
| Prospective acceptance gates | [Edition093 contract](docs/EDITION_093_CONTRACT.md) |
| Actual application interruption tests | [recovery093-integration.cjs](workspace-desktop/test/recovery093-integration.cjs) |
| Independent inspection | [Inspect-Recovery093.cjs](scripts/Inspect-Recovery093.cjs) |
| Evidence mutation tests | [RecoveryAudit093Test.java](workspace-engine/src/test/java/fraymus/workspace/RecoveryAudit093Test.java) |

Use an isolated checkout and a new private project for these tests. Build the Java engine, stamp its input/artifact receipt, then run the integration script. It refuses an existing study checkpoint. Run the independent inspector after completion. The optional native-evidence JUnit probe uses an explicit copied project supplied through FRAYNIX_RECOVERY093_PROJECT; the recorded validation includes that probe. Do not rebuild a user's live installation or point a second process at its private data.

## What this establishes, and what comes next

This is a concrete improvement in continuity: the agent can recognize that a recovered state invalidates a downstream prediction, spend bounded work on a replacement, check it, and continue from evidence. It preserves the user's core design of a local Java agent operating inside a stateful, observable world. It does not establish consciousness, general intelligence, physical accuracy, arbitrary learned procedures or new LLM weights.

The tests cover controlled isolated-app closure and reopening, not sudden power failure. They cover nearby two-stage box tasks and one host, not universal checkpoint recovery. A changed condition still has to satisfy the existing forecast and final-task tolerances. Recovery may refuse when no valid forecast fits the remaining budget. That refusal is part of correct behaviour.

Next, test native three-stage recovery at more than one boundary and abrupt-process termination with original evidence retained. Then measure whether recovery remains worth its complete cost on longer mixed tasks, including repeated interruptions near the budget boundary. Keep the simple controller and uninterrupted execution as comparators. Continue the pending calibrated-selection work only with complete clocks and all original source and outcome checks.

Publication is recorded separately after a real push and fetched Git-tree verification. The immutable readme-093 tag identifies this evidence snapshot; later publication receipts do not rewrite it.
'''
write(r/'README-093.md',readme)
write(r/note,f'# Edition093 — recovered-state forecasting\n\n[[README-093.md|Full edition]] · [[docs/readmes/093/index.html|Offline evidence]] · [[docs/obsidian/100-growing-bank-restarts.md|Prior edition]]\n\n{finding}\n\n{test_summary}\n\nOne replacement per later stage; original budgets and all proof gates retained. Two-stage synthetic recovery and controlled shutdown tested; abrupt crash, three-stage native recovery and general accuracy remain unvalidated. Resumed cost clocks remain ineligible.\n\n[[workspace-engine/src/main/java/fraymus/workspace/RecoveryForecast093.java]] → [[workspace-engine/src/main/java/fraymus/workspace/WorldProcedure079.java]] → [[scripts/Inspect-Recovery093.cjs]]\n')
nodes=[{'id':m['id'],'type':'file','file':m['source'],'x':i*390,'y':0,'width':350,'height':260} for i,m in enumerate(modules)];nodes.append({'id':'evidence','type':'file','file':'README-093.md','x':390,'y':330,'width':740,'height':340});edges=[{'id':'e'+str(i),'fromNode':modules[i]['id'],'fromSide':'right','toNode':modules[i+1]['id'],'toSide':'left'} for i in range(3)];js(r/'docs/obsidian/101-recovered-state-forecast.canvas',{'nodes':nodes,'edges':edges})
continuity=f'## Edition093 — locally verified; publication pending\n\n{finding}\n\n{test_summary} Original sources, sample prefixes, provenance, budgets, forecast comparisons and single commits independently inspected. Resumed clocks remain excluded from calibration. New forecast work is charged; stopping it prevents later actual motion. Native study source{rec["source"]}.\n\nIsolated D:/Codex-Work/fraynix-workspace-edition093; private eyeoverthink/Fraynix-Agent-Workspace. Preserve live045/qa046/private data and frozen001–092. No subagents. No physical accuracy, LLM training or general recovery claim. Next: native three-stage and abrupt-termination recovery, then complete mixed-task cost comparisons.\n\n'
for name in ['AGENTS.md','docs/PROJECT_MEMORY.md','docs/DEVELOPMENT_LOG.md']:write(r/name,continuity+(r/name).read_text(encoding='utf8'))
root=(r/'README.md').read_text(encoding='utf8').replace('**Latest: [edition092','**Earlier edition: [edition092',1);root=root.replace('# Fraynix Agent Workspace\n','# Fraynix Agent Workspace\n\n**Latest: [edition093 — forecast from recovered state](README-093.md).**\n\n'+finding+' [Offline evidence](docs/readmes/093/index.html).\n',1);write(r/'README.md',root)
for name,prefix in [('docs/ARCHITECTURE.md','Edition093 verifies a new forecast from recovered state before later actual motion. Original predictions and all source/outcome checks remain. [Results and limits](../README-093.md).\n\n'),('docs/obsidian/00-home.md','[[README-093.md|Edition093 — recovered-state forecasting]] · [[docs/obsidian/101-recovered-state-forecast.md]]\n\n'),('docs/readmes/INDEX.md','[Edition093 — forecast from recovered state](../../README-093.md) · [Offline evidence](093/index.html)\n\n')]:write(r/name,prefix+(r/name).read_text(encoding='utf8'))
print(json.dumps({'finding':finding,'tests':tests,'logicalOriginals':len(archive),'uniquePayloads':len(local),'sources':len(inventory),'replays':len(replays)},indent=2))
