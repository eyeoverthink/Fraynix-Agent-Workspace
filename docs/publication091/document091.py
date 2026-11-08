from pathlib import Path
import json,hashlib,gzip,base64,subprocess,shutil,datetime,xml.etree.ElementTree as ET,zipfile,re,os
r=Path.cwd();d=r/'docs/readmes/091';d.mkdir(parents=True,exist_ok=True)
def load(p):return json.loads(p.read_text(encoding='utf-8-sig'))
def write(p,s):p.parent.mkdir(parents=True,exist_ok=True);p.write_text(s,encoding='utf8',newline='\n')
def js(p,v):write(p,json.dumps(v,indent=2,ensure_ascii=False)+'\n')
def sha(b):return hashlib.sha256(b).hexdigest()
def sec(v):return f'{v/1000:.2f} s'
a=load(r/'build/analysis091.json');rec=load(r/'build/ui091-study/result.json');assert a['passed'] and rec['passed']
xml=list((r/'build/regression091').glob('TEST-*.xml'));assert xml
tests={'cases':0,'failures':0,'errors':0,'skipped':0,'classes':len(xml),'skips':[]}
for f in xml:
 tree=ET.parse(f).getroot();tests['cases']+=int(tree.attrib['tests'])
 for k in ['failures','errors','skipped']:tests[k]+=int(tree.attrib.get(k,0))
 for t in tree.findall('testcase'):
  if t.find('skipped') is not None:tests['skips'].append(t.attrib)
assert tests['failures']==tests['errors']==0
desktop=(r/'build/desktop091.log').read_text(encoding='utf-8-sig');assert 'tests 135' in desktop and 'pass 135' in desktop and 'fail 0' in desktop
junction=load(r/'build/junction091.json');assert junction['passed']
test_summary=f"{tests['cases']-tests['skipped']} Java cases passed across {tests['classes']} classes; {tests['skipped']} host-dependent case skipped; 135 desktop checks passed. A separate Windows directory-junction refusal probe passed. Nine fresh native goals and eight full post-task audits were independently inspected."
oldjar=r.parent/'fraynix-workspace-edition090/workspace-engine/build/install/workspace-engine/lib/workspace-engine.jar';newjar=r/'workspace-engine/build/install/workspace-engine/lib/workspace-engine.jar'
with zipfile.ZipFile(oldjar) as old,zipfile.ZipFile(newjar) as new:
 classes=[x for x in old.namelist() if x.endswith('.class')];changed=[x for x in classes if old.read(x)!=new.read(x)];added=[x for x in new.namelist() if x.endswith('.class') and x not in classes]
assert set(changed)=={'fraymus/workspace/WorldProcedure079.class','fraymus/workspace/GoalRunner.class','fraymus/workspace/WorkspaceServer.class'}
identity={'unchangedClasses':len(classes)-len(changed),'changed':changed,'added':added,'baseline':'edition090','scope':'Exact compiled class bytes; native motion, frame, procedure and verdict implementations unchanged.'};js(r/'build/source-comparison091.json',identity)
tot=a['totals'];saving=tot['reference']['callerMillis']-tot['reuse']['callerMillis'];reduction=100*(1-tot['reuse']['canonicalChecks']/tot['reference']['canonicalChecks'])
finding=f"Digest reuse reduced canonical verification passes by {reduction:.1f}% and took {sec(abs(saving))} {'less' if saving>=0 else 'more'} across the eight task calls (four paired comparisons). All nine new native goals completed and verified. Every original outcome check remains active."
replays=[]
for row in rec['rows']:
 if row['mode']!='reuse':continue
 original=load(r/'build/ui091-study'/row['originalFile'])
 for packed in original['originals']:
  run=json.loads(gzip.decompress(base64.b64decode(packed['gzipBase64'])))
  attempt=next(x for x in row['job']['arm']['attempts'] if x['id']==run['id'])
  replays.append({'label':row['id']+' / '+row['policy']+' / '+attempt['family']+' / '+attempt['stage'],'id':run['id'],'status':run['status'],'objects':run['definition']['objects'],'samples':[{'tick':s['tick'],'timeS':s['timeS'],'bodies':s['bodies']} for s in run['samples']]})
archive=[];local={}
def pack(p,logical):
 raw=(Path('\\\\?\\'+str(p)) if os.name=='nt' else p).read_bytes();h=sha(raw)
 if h not in local:
  dest=d/'original'/(h+'.gz');dest.parent.mkdir(parents=True,exist_ok=True);z=gzip.compress(raw,mtime=0);dest.write_bytes(z);local[h]={'path':dest.relative_to(r).as_posix(),'bytes':len(z),'sha256':sha(z),'rawBytes':len(raw),'rawSha256':h}
 archive.append(dict(local[h],logicalPath=logical))
folder=r/'build/ui091-study'
for p in folder.rglob('*'):
 if p.is_file() and 'desktop-profile' not in p.parts and p.name!='host.json' and p.suffix in ['.json','.jsonl','.log','.png','.html','.css','.js','.txt','.csv','.fworld','.previous']:pack(p,'ui091-study/'+p.relative_to(folder).as_posix())
for foldername in ['regression091','unit091-first']:
 for p in (r/'build'/foldername).glob('TEST-*.xml'):pack(p,foldername+'/'+p.name)
for f in xml:
 for match in re.finditer(r'OUTCOME091_NATIVE ([^\r\n<]+)',f.read_text(encoding='utf8')):
  unit=Path(match.group(1).strip());assert unit.is_relative_to(r/'build/tmp-tests')
  for p in unit.rglob('*.json'):pack(p,'outcome091-unit/'+p.relative_to(unit).as_posix())
for p in (r/'build').glob('*091*'):
 if p.is_file() and not (p.name.startswith('document091') and p.suffix=='.log') and p.suffix in ['.json','.log','.ps1','.py','.cjs','.html','.md','.txt','.java']:pack(p,'validation/'+p.name)
for p in (r/'build/ui091-study').glob('*.png'):shutil.copyfile(p,d/p.name)
js(d/'original-evidence.json',{'files':archive,'distinctNewPayloads':len(local),'note':'Exact labelled synthetic originals and test logs. Content-addressed compression deduplicates bytes, not observations. Host credentials/profile excluded. Refusal probes restore exact original bytes.'})
old=load(r/'docs/readmes/090/file-inventory.json');paths={x['path'] for x in old};paths.update(x for x in subprocess.check_output(['git','diff','--name-only','--diff-filter=A','6d1ef698c44ca0e3bcdedf419b8f4b41dfc8238a','HEAD'],text=True).splitlines() if x.startswith(('workspace-engine/','workspace-desktop/','scripts/','fraynix-modules/')))
kinds={x['path']:x['hashKind'] for x in old};inventory=[]
for name in sorted(paths):
 raw=(r/name).read_bytes();kind=kinds.get(name,'utf8-lf');raw=raw.decode('utf8').replace('\r\n','\n').encode() if kind=='utf8-lf' else raw
 inventory.append({'path':name,'bytes':len(raw),'sha256':sha(raw),'hashKind':kind})
js(d/'file-inventory.json',inventory);write(d/'FILE_TREE.txt','Edition091 canonical source inventory\n'+'\n'.join(x['path'] for x in inventory)+'\n')
note='docs/obsidian/99-native-envelope-verification.md'
modules=[{'id':'digest','name':'Bounded digest reuse','source':'workspace-engine/src/main/java/fraymus/workspace/NativeEnvelope091.java','description':'Every-byte SHA-256 and fresh JSON; canonical digest reuse only for previously authenticated identical envelopes.','note':note},{'id':'procedure','name':'Source and outcome authority','source':'workspace-engine/src/main/java/fraymus/workspace/WorldProcedure079.java','description':'Current identities, native chronology, motion, forecast comparison, protected bodies and whole-task success.','note':note},{'id':'goal','name':'Report and commit','source':'workspace-engine/src/main/java/fraymus/workspace/GoalRunner.java','description':'Durable goal, original saved report, measured costs and committed world must agree.','note':note},{'id':'inspect','name':'Independent accounting','source':'scripts/Inspect-Envelopes091.cjs','description':'Original native/commit arithmetic and measured fingerprint work for both modes.','note':note}]
system={'modules':modules,'screenshots':[{'path':'docs/readmes/091/'+x['file'],'caption':'Actual091 saved report capture'} for x in rec['captures']],'evidence':[{'path':'docs/readmes/091/verification.json','caption':'Checks and complete measurements'},{'path':'docs/readmes/091/original-evidence.json','caption':'Exact original archive'}]};js(d/'system-map.json',system)
verification={'passed':True,'analysis':a,'java':tests,'junction':junction,'desktopPassed':135,'classes':identity,'screenshots':rec['captures'],'finding':finding,'source':rec['source'],'limits':['One host, two labelled scene families from090, alternating orders without randomization; fresh executions, not newly held-out conditions.','Reference uses one canonical hash per read, prior090 used two.','No calibrated-selection or growing-bank timing in this study.','One optional symbolic-link creation test skipped by Windows; separate directory-junction refusal passed.','No outcome verdict cached; no new LLM inference/training or physical-accuracy claim.']};js(d/'verification.json',verification);js(d/'comparison-data.json',a)
data={'analysis':a,'finding':finding,'testSummary':test_summary,'replays':replays};js(d/'explorer-data.json',data)
html=(r/'build/guide091.html').read_text(encoding='utf8').replace('__DATA__',json.dumps(data,separators=(',',':')).replace('</','<\\/'))
extra=''.join('<script id="'+name+'" type="application/json">'+json.dumps(value,separators=(',',':')).replace('</','<\\/')+'</script>' for name,value in [('system-data',system),('inventory-data',inventory)])
write(d/'index.html',html.replace('</html>',extra+'</html>'))
table='\n'.join(f"| {x['id']} / {x['policy']} | {sec(x['referenceMillis'])} | {sec(x['reuseMillis'])} | {sec(x['savingMillis'])} | {x['ticks']:,} |" for x in a['pairs'])
readme=f'''# Edition 091 — Less repeated verification work

[Previous: edition090](README-090.md) · [README history](docs/readmes/INDEX.md) · [Interactive offline evidence](docs/readmes/091/index.html) · [Prospective contract](docs/EDITION_091_CONTRACT.md)

## What changed and why

{finding}

Edition090 showed that checking the cost of using memory could itself be expensive. Edition091 narrows one repeated part of that work: rebuilding the canonical digest of large, unchanged native motion records. Java still reads every byte, decodes new data and executes all original outcome checks.

This is the same integrated local agent workspace: chat requests become durable Java goals; the world runtime performs bounded actions; reports and verified commits retain what actually happened. This release makes evidence handling cheaper. It does not add a new model, claim consciousness, or substitute cached success for measured behavior.

## The actual application

Captured {rec['finishedAt'][:10]} from the isolated edition091 app after fresh labelled synthetic tasks, using saved reports opened through the real canvas tool.

![Actual verified memory report](docs/readmes/091/verified-memory-1600.png)

![Actual short-trip report](docs/readmes/091/verified-short-1100.png)

[Memory at 1100 pixels](docs/readmes/091/verified-memory-1100.png) · [Short trip at 1600 pixels](docs/readmes/091/verified-short-1600.png)

## The precise reuse boundary

1. Check that the native file belongs to this owner, exists as a regular unredirected file and fits the 32 MB format limit.
2. Read all bytes with a bounded read. Hash the complete envelope using SHA-256. File timestamps and sizes are not evidence of unchanged contents.
3. Decode a fresh JSON object on every call. A bounded table may supply the canonical digest only when the complete byte digest matches an earlier authenticated envelope.
4. Check envelope format/digest and the caller's current goal ID, attempt ID and expected canonical digest.
5. Run the original motion, chronology, work admission, forecast/actual, protected-body, task, report and committed-world checks at their existing boundaries.

The table holds at most **64 digest pairs**, and no JSON object, trajectory verdict, goal result or persistent proof. Invalid envelopes are not admitted. Different bytes need full canonical checking. Eviction and restart also require a new canonical pass. Concurrent callers receive independent data. Mode changes for testing clear the table.

All **{identity['unchangedClasses']} existing compiled engine classes** outside the three integrations are byte-identical to090. `WorldVerify`, `WorldFrames`, `Procedure079`, the native controller, proof memo, calibration reader and selection formulas remain unchanged. New `NativeEnvelope091` owns only digest reuse; `WorldProcedure079`, `GoalRunner` and `WorkspaceServer` integrate it. The engine identity includes the new class. Old motion proofs and old cost fits are not relabelled as091 evidence.

## Complete task measurements

The study used one fresh retained two-stage receipt and eight new task goals: long and short trips, each under simple and forced-memory policies, each in reference and reuse mode. Scene specifications derive from090; these are fresh executions, not newly held-out conditions. The same compiled engine ran both modes with 2 configured processors, OS affinity pinned to two CPUs and a 512 MiB Java heap. Every comparison starts a fresh matched world. There is one fixed memory receipt and no timed skill saving or calibration fitting.

| Task / policy | Reference call | Reuse call | Saving | Native ticks per mode |
| --- | ---: | ---: | ---: | ---: |
{table}

Positive saving means reuse was faster. Both modes selected the same final procedure and charged the same native ticks in each pair. A remembered route may be rejected before native execution; choosing memory is not proof that its requested route executed.

Across the four paired tasks, reference calls total **{sec(tot['reference']['callerMillis'])}** and reuse calls total **{sec(tot['reuse']['callerMillis'])}**. Reference performs {tot['reference']['canonicalChecks']:,} canonical checks; reuse performs {tot['reuse']['canonicalChecks']:,}, with {tot['reuse']['hits']:,} authenticated digest hits. Every read still hashes all source bytes. Raw read counts and bytes are retained in [complete measurements](docs/readmes/091/comparison-data.json); any difference in call counts must not be represented as skipped outcome checks.

The reference mode performs **one** full canonical pass per read;090 performed two. This comparison isolates reuse against an already simplified full-check baseline. It does not directly measure the entire release against an090 executable.

Four repeated full goal audits per mode took **{sec(tot['reference']['auditMillis'])}** in reference mode and **{sec(tot['reuse']['auditMillis'])}** with reuse. They returned exactly the same source-bound outcome and decision. Cold and warm table states are recorded separately; every native outcome is still reconstructed and checked.

Separate costs: seed receipt **{sec(a['preparationMillis'])}**, application startup **{sec(a['startupMillis'])}**, exports **{sec(a['exportMillis'])}**, cold-restart full verification **{sec(a['restart']['callerMillis'])}**. Task-call timing includes transport, polling and final cost bookkeeping. The existing Java cost timer ends before the final cost record and anchor write. Post-task audits and screenshots do not disappear into a claimed end-to-end saving.

## Verification

{test_summary}

Tests cover same-size/timestamp tampering, current goal/attempt/digest substitution, malformed formats, invalid UTF-8, size limits, missing or escaped paths, eviction, restart, reference mode and mutation of previously returned JSON. A warmed native regression rejects forged metrics, source and plan digests, charged ticks, success and prediction comparisons. Even a newly signed envelope with a false final motion result fails the unchanged native verifier. Failed probes restore exact original bytes.

Windows denied creation of a symbolic link in the optional host-specific test. It is recorded as skipped, not passed; no claim is made that this host executed that fixture. A separate actual Windows directory-junction probe successfully tested redirected-parent refusal against identical warmed bytes. All other cases passed in the final broad invocation. The original first focused invocation and the broad run remain available with their source identities.

The Electron study also rechecks the completed saved report, cost record and persistent world, rejects warmed same-size file corruption, and verifies the same goal after restarting Java with an empty digest table. A separate JavaScript reader independently reconstructs original native arithmetic, procedure decisions, costs and world commits. That reader is not a separate physics engine.

Documentation assembly was retried after waiting for the inspector's completed output and enabling Windows long-path reads for original unit-test evidence. These packaging failures and their recovery are retained in the logs; no measured task or runtime implementation was changed.

[Verification receipt](docs/readmes/091/verification.json) · [Exact original archive index](docs/readmes/091/original-evidence.json) · [Source inventory](docs/readmes/091/FILE_TREE.txt) · [Obsidian map](docs/obsidian/99-native-envelope-verification.md)

## Run and review

Use Java21 and the existing Node/Electron dependencies. Start `Launch-Agent-Workspace.bat` from this checkout, or use `scripts/Start-Agent-Workspace.ps1` with a distinct project directory for a review. Preserve existing private projects and live installations. Do not silently point a new checkout at the running user's database. The app is local; optional remote models and browser activity use their configured services. Offline test mode requires no cloud model.

In the app, import or select a labelled world, choose simple control or an available verified memory procedure, submit the arrangement goal, and inspect the resulting report. Verify final positions, motion comparisons and the committed revision. This change requires no new user-facing toggle. The local diagnostics route `/api/procedures091/diagnostics` reports reads, bytes, canonical checks, hits and failures. `/api/procedures091/test-mode` is restricted to idle isolated test runtimes; it offers full reference checking or bounded reuse, never a skip-verification mode.

To reproduce the declared comparison after a successful build, run `node scripts/Study-Build091.cjs --stamp`, then `node workspace-desktop/test/envelopes091-integration.cjs`, then `node scripts/Inspect-Envelopes091.cjs`. Use a fresh study output; keep failed attempts. The build receipt binds all current inputs and installed JARs before and after measurement. The protocol is in `workspace-desktop/test/fixtures/procedures091-protocol.json`.

## System direction

```mermaid
flowchart LR
  Request[Chat or world request] --> Goal[Durable Java goal]
  Goal --> Choice[Verified procedure selection]
  Choice --> Native[Forecast and actual native motion]
  Native --> Bytes[Read every original byte]
  Bytes --> Digest[Bounded canonical digest reuse]
  Digest --> Outcome[Original motion and outcome checks]
  Outcome --> Commit[Report and committed world]
  Commit --> Memory[Verified reusable experience]
  Memory --> Choice
```

The Java brain retains task ownership, policies, current-world binding and recovery; the desktop presents the browser, canvas and controls. Physics supplies testable consequences. Memory contributes verified procedures and measurements rather than authority by assertion. The intended evolution remains better task performance that survives restart and unfamiliar conditions, measured against a simple controller and charged for its actual work.

## Next and limits

Next, measure longer mixed and restarted workloads as the memory bank grows, including calibrated selection. Determine whether freshness checking now repays more of its cost. Profile the remaining decode and native arithmetic before changing any check; prefer bounded streaming or lower allocations to caching outcome authority.

This small study has four paired comparisons, alternating order, on one host. It is not randomized, a general latency guarantee or a demonstration of real-world physics accuracy. It does not measure larger banks, fit payback or calibrated-policy task latency. No LLM weights were trained, no physical sensor was used, and original090 evidence remains historical. Publication is recorded separately after remote fetch verification so the frozen edition does not claim an unobserved push.
'''
write(r/'README-091.md',readme)
note_text=f'''# Edition091 — native envelope verification

[[README-091.md|Full edition]] · [[docs/readmes/091/index.html|Offline evidence]] · [[docs/obsidian/98-cost-freshness-and-mixed-tasks.md|Prior edition]]

{finding}

Only canonical content fingerprints are reused. Every source read hashes all bytes and decodes new JSON. Source identity and all outcomes remain checked. Process-local64-entry bound; cold restart and changed bytes require a full canonical pass. No mutable JSON or success verdict is retained.

{test_summary}

Next: longer mixed/restarted/growing-bank and calibrated-policy measurements. Preserve original evidence and every outcome check. This is measured engineering improvement, not model training or physical accuracy.

[[workspace-engine/src/main/java/fraymus/workspace/NativeEnvelope091.java]] → [[workspace-engine/src/main/java/fraymus/workspace/WorldProcedure079.java]] → [[workspace-engine/src/main/java/fraymus/workspace/GoalRunner.java]]
'''
write(r/note,note_text)
nodes=[{'id':m['id'],'type':'file','file':m['source'],'x':i*390,'y':0,'width':350,'height':260} for i,m in enumerate(modules)]
nodes.append({'id':'evidence','type':'file','file':'README-091.md','x':390,'y':330,'width':740,'height':340})
edges=[{'id':'e'+str(i),'fromNode':modules[i]['id'],'fromSide':'right','toNode':modules[i+1]['id'],'toSide':'left'} for i in range(3)]
js(r/'docs/obsidian/99-native-envelope-verification.canvas',{'nodes':nodes,'edges':edges})
continuity=f'''## Edition091 — locally verified; publication pending

{finding}

{test_summary} {identity['unchangedClasses']} prior engine classes are byte-identical to090; only WorldProcedure079, GoalRunner and WorkspaceServer integrate the new bounded NativeEnvelope091. Complete bytes and current identities are checked on each read; every original outcome check remains. Old proofs/cost fits are not relabelled.

Isolated D:/Codex-Work/fraynix-workspace-edition091, private eyeoverthink/Fraynix-Agent-Workspace. Source {rec['source']}. Actual2CPU/512MiB study: one seed, eight task comparisons, four full audits per mode, restored tamper probe and cold restart. No calibration fit, growing-bank timing, weight training or hardware-accuracy claim. Preserve original live045/qa046/private data and frozen001–090. No subagents. Next: longer mixed/restarted and calibrated workloads, with all checks retained.

'''
for name in ['AGENTS.md','docs/PROJECT_MEMORY.md','docs/DEVELOPMENT_LOG.md']:write(r/name,continuity+(r/name).read_text(encoding='utf8'))
root=(r/'README.md').read_text(encoding='utf8').replace('**Latest: [edition090','**Earlier edition: [edition090',1)
root=root.replace('# Fraynix Agent Workspace\n','# Fraynix Agent Workspace\n\n**Latest: [edition091 — less repeated verification work](README-091.md).**\n\n'+finding+' [Offline evidence](docs/readmes/091/index.html).\n',1);write(r/'README.md',root)
for name,prefix in [('docs/ARCHITECTURE.md','Edition091 reuses only canonical fingerprints of identical authenticated native envelopes. Every read retains full-byte checking and fresh JSON; all existing motion/outcome/report/commit checks run. [Measured results and limits](../README-091.md).\n\n'),('docs/obsidian/00-home.md','[[README-091.md|Edition091 — less repeated verification]] · [[docs/obsidian/99-native-envelope-verification.md]]\n\n'),('docs/readmes/INDEX.md','[Edition091 — less repeated verification work](../../README-091.md) · [Offline evidence](091/index.html)\n\n')]:write(r/name,prefix+(r/name).read_text(encoding='utf8'))
print(json.dumps({'finding':finding,'tests':tests,'archiveFiles':len(archive),'uniquePayloads':len(local),'archiveMiB':sum(x['bytes'] for x in local.values())/2**20,'sources':len(inventory),'replays':len(replays),'classes':identity},indent=2))
