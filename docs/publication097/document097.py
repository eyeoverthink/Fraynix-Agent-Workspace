from pathlib import Path
import json,hashlib,gzip,subprocess,shutil,xml.etree.ElementTree as ET,re,sys
draft='--draft' in sys.argv
r=Path.cwd();d=r/'docs/readmes/097';d.mkdir(parents=True,exist_ok=True)
def load(p):return json.loads(p.read_text(encoding='utf-8-sig'))
def write(p,s):p.parent.mkdir(parents=True,exist_ok=True);p.write_text(s,encoding='utf8',newline='\n')
def js(p,v):write(p,json.dumps(v,indent=2,ensure_ascii=False)+'\n')
def sha(b):return hashlib.sha256(b).hexdigest()
contract=r/'docs/EDITION_097_CONTRACT.md'
ct=contract.read_text(encoding='utf8').replace('2026-11-08,19:27PST','2026-11-08,19:24:29PST (commit28b93071)')
if 'Documentation correction:' not in ct:ct+='\nDocumentation correction: the original handwritten amendment minute was19:27. Git records the pre-trial amendment at19:24:29PST and its UTF-8 handoff at19:24:58PST, before the recorded native cohort. This corrects a minute label; conditions and acceptance checks are unchanged.\n'
write(contract,ct)
a=load(r/'build/analysis097.json');run=load(r/'build/ui097-study/result.json');mutations=load(r/'build/mutations097.json');identity=load(r/'build/source-comparison097.json');assert a['passed'] and run['passed'] and mutations['passed'] and not identity['changed']
ci=load(r/('build/ci097-gate.json' if draft else 'build/ci097-final.json'));ciPassed=ci['conclusion']=='success' and ci['status']=='completed'
assert ci['id']==42554061175 and ci['head_sha']=='453e8708ab9353d86ed666f3d059d28563372530'
assert draft or ciPassed
java=[]
for p in sorted((r/'workspace-engine/build/test-results/test').glob('TEST-*.xml')):
 x=ET.parse(p).getroot();java.append({'name':x.attrib['name'],'tests':int(x.attrib['tests']),'failed':int(x.attrib.get('failures',0))+int(x.attrib.get('errors',0)),'skipped':int(x.attrib.get('skipped',0)),'seconds':float(x.attrib['time'])})
assert java and not sum(x['failed'] for x in java)
assert re.search(r'pass\s+135\b',(r/'build/desktop097.log').read_text(encoding='utf-8-sig'))
assert re.search(r'pass\s+11\b',(r/'build/costs097-tests-final.log').read_text(encoding='utf-8-sig'))
detail=load(r/'build/detail097.json');assert detail['passed'] and detail['worldUnchanged']
captures=run['captures']+detail['captures']
caption='The canvas displays the recovered sixth-goal report (world revision 6). The right goal panel and timeline show the later saved-skill demonstration (revision 7); they are different tasks. Controller v1 is a separate legacy episode bank, not the seven-entry procedural bank. A selected model name does not imply model inference ran.'
verification={'passed':ciPassed,'localChecksPassed':True,'draft':draft,'studySource':run['source'],'ci':{k:ci[k] for k in ['id','head_sha','status','conclusion','html_url','created_at','updated_at']},'java':{'source':subprocess.check_output(['git','rev-parse','78e7d79b'],text=True).strip(),'qualification':'Focused regressions before native study; subsequent changes are study/validation/docs only. All 334 compiled production classes match096.','classes':java,'tests':sum(x['tests'] for x in java),'skipped':sum(x['skipped'] for x in java),'failures':0},'desktopPassed':135,'accountingPassed':11,'mutations':mutations,'productionClassComparison':identity,'study':a,'captures':captures,'captureDetail':detail,'captureCaption':caption,'scope':'Actual continuing synthetic worlds with unchanged production Java. Explicit external recovered timings remain outside calibration. No new model inference or physical observation.'}
js(d/'analysis.json',a);js(d/'verification.json',verification)
archive=[];unique={}
def pack(p,label):
 raw=p.read_bytes();h=sha(raw)
 if h not in unique:
  z=gzip.compress(raw,mtime=0);dest=d/'original'/(h+'.gz');dest.parent.mkdir(parents=True,exist_ok=True);dest.write_bytes(z);unique[h]={'path':dest.relative_to(r).as_posix(),'bytes':len(z),'sha256':sha(z),'rawBytes':len(raw),'rawSha256':h}
 archive.append(dict(unique[h],logicalPath=label))
for p in (r/'build/ui097-study').rglob('*'):
 if p.is_file() and 'desktop-profile' not in p.parts and p.name!='host.json' and p.suffix in ['.json','.jsonl','.fworld','.pending','.html','.css','.js','.csv','.log','.png']:pack(p,'study/'+p.relative_to(r/'build/ui097-study').as_posix())
for p in (r/'build').glob('*097*'):
 if p.is_file() and p.suffix in ['.json','.log','.ps1','.py','.md','.cjs']:pack(p,'validation/'+p.name)
for p in (r/'workspace-engine/build/test-results/test').glob('TEST-*.xml'):pack(p,'java/'+p.name)
for c in captures:shutil.copyfile(r/'build/ui097-study'/c['file'],d/c['file'])
js(d/'original-evidence.json',{'files':archive,'distinctNewPayloads':len(unique),'scope':'Exact isolated synthetic study originals, app captures, verification outputs and CI gate. No private project, camera images, model weights or desktop credential profile.'})
prior=load(r/'docs/readmes/096/file-inventory.json');kinds={x['path']:x['hashKind'] for x in prior};paths=set(kinds);paths.add('docs/EDITION_097_CONTRACT.md')
paths.update(x for x in subprocess.check_output(['git','diff','--name-only','--diff-filter=A','453e8708ab9353d86ed666f3d059d28563372530','HEAD'],text=True).splitlines() if x.startswith(('workspace-engine/','workspace-desktop/','scripts/','fraynix-modules/')))
inventory=[]
for p in sorted(paths):
 b=(r/p).read_bytes();kind=kinds.get(p,'utf8-lf');b=b.decode('utf8').replace('\r\n','\n').encode() if kind=='utf8-lf' else b;inventory.append({'path':p,'bytes':len(b),'sha256':sha(b),'hashKind':kind})
js(d/'file-inventory.json',inventory)
note='docs/obsidian/105-continuing-costs.md'
system={'modules':[{'id':key,'name':name,'source':source,'note':note} for key,name,source in [('world','Continuing Java world','workspace-engine/src/main/java/fraymus/workspace/WorldLedger.java'),('memory','Verified procedures and memory','workspace-engine/src/main/java/fraymus/workspace/WorldProcedure079.java'),('cost','Complete external accounting','scripts/Costs097.cjs'),('inspector','Independent source and outcome inspection','scripts/Inspect-Continuing097.cjs'),('study','Actual app sequence and recovery','workspace-desktop/test/continuing097-integration.cjs')]],'screenshots':[{'path':'docs/readmes/097/'+c['file'],'caption':'Actual continuing-world report '+c['stream']} for c in captures],'evidence':[{'path':'docs/readmes/097/'+p,'caption':caption} for p,caption in [('verification.json','Verification and CI'),('analysis.json','Original task measurements'),('original-evidence.json','Original byte archives')]]}
js(d/'system-map.json',system)
html=(r/'build/guide097.html').read_text();images='\n'.join('<figure><a href="'+c['file']+'"><img src="'+c['file']+'" alt="Actual continuing-world task report, '+c['stream']+', '+str(c['width'])+' pixels"></a><figcaption>'+c['stream']+' · '+str(c['width'])+' pixels. '+caption+'</figcaption></figure>' for c in captures)
for key,value in [('IMAGES',images),('SYSTEM',json.dumps(system)),('INVENTORY',json.dumps(inventory)),('ANALYSIS',json.dumps(a))]:html=html.replace('@@'+key+'@@',value)
write(d/'index.html',html)
sec=lambda n:'unavailable' if n is None else f'{n/1000:,.2f} s'
complete=sum(x['success'] for x in a['results']);total=len(a['results']);recoveries=sum(x['resumes'] for x in a['results']);demo=sum(x['success'] for x in a['demonstrations']);summary=a['summary'];jp=verification['java']['tests']-verification['java']['skipped']
lead=f"**{complete}/{total} sequence goals completed; {recoveries} interruptions were explicitly recovered. {demo} saved procedures executed after reopening.**"
if summary['comparable']:
 lead+=f" Across both blocks, memory took {sec(abs(summary['streamSavingMillis']))} {'longer' if summary['streamSavingMillis']<0 else 'less time'} over complete streams and {sec(abs(summary['netAfterPreparationMillis']))} {'longer' if summary['netAfterPreparationMillis']<0 else 'less time'} after charging preparation once."
else:lead+=' Some work did not complete; no complete-stream speed benefit is claimed.'
table='\n'.join('| '+ ' | '.join(map(str,[x['stream'],x['condition'],x['bankBefore'],x['bankAfter'],x['revision'],sec(x['taskMillis']),sec(x['restartMillis']),x['lookupMode'],'yes' if x['savedSkill'] else 'no']))+' |' for x in a['results'])
streams='\n'.join(f"| {p['block']+1} | {sec(p['simple']['totalMillis'])} | {sec(p['memory']['totalMillis'])} | {sec(p['streamSavingMillis'])} | {p['comparable']} |" for p in summary['pairs'])
components=[]
for mode in ['simple','memory']:
 rows=[x for x in a['results'] if x['mode']==mode];ss=[p[mode] for p in summary['pairs']]
 components.append('| '+' | '.join([mode,sec(sum(x['taskMillis'] for x in rows)),sec(sum(x['lookupMillis'] for x in rows)),str(sum(x['ticks'] for x in rows)),sec(sum(x['phases']['audit'] for x in rows)),sec(sum(x['phases']['persistence'] for x in ss)),sec(sum(x['phases']['skill'] for x in ss))])+' |')
componentTable='\n'.join(components)

readme=f'''# Edition 097 — One world, growing memory, complete costs

[Previous: edition 096](README-096.md) · [Offline results explorer](docs/readmes/097/index.html) · [All editions](docs/readmes/INDEX.md) · [Contributor direction](README-CONTRIBUTORS.md)

{lead}

Recorded November 8–9, 2026. This edition asks what repeated work costs when the application keeps the world it has actually changed and the memories it has actually earned. Java still owns the goals, native execution, evidence checks, recovery and retained procedures. No controller, physical tolerance, model policy or verification rule changed: all {identity['identical']} compiled production engine classes match edition 096.

## CI was checked first

The parent main build [{ci['id']}]({ci['html_url']}) completed successfully on `{ci['head_sha']}` before publication. It was checked before the native cohort and still running then. A dated pre-trial scheduling amendment allowed isolated measurements after local regression/source checks passed; {'the pending response is retained; the final response is still awaited' if draft else 'both the pending and final responses are retained'}. Local checks below are separate from that complete remote result, and the new edition's own post-push CI status must be checked separately.

## What changed

The new study runs complete requests through the existing integrated Electron/Java app. Four independent streams use two opposite control orders: simple then memory, followed by memory then simple. Each stream starts with a byte-verified copy of the same freshly earned preparation and follows six requests in one persistent world. The next task uses the last verified committed frame, including its actual positions and rotations. No between-task reset, teleport or new scene replaces that history.

Both controls retain every successful experience. Every second task also saves a named procedure. Memory control uses the existing nearest compatible verified receipt; simple control keeps its original candidate order. Two declared tasks in each stream receive an abrupt stop of the exact owned Java process during actual stage zero. The user’s existing installations and private projects remain untouched.

The [prospective contract](docs/EDITION_097_CONTRACT.md) and [exact fixture](workspace-desktop/test/fixtures/procedures097-protocol.json) were committed before native measurements. The geometry comes from the labelled095 synthetic long-route scene: two movable bodies, a protected bystander, a floor and a fixed obstacle. Requests alternate an outward move, a short adjustment, a return, another adjustment and repeated outward/return moves. This is a new continuing software workload using related geometry, not independent real-world validation. The five-body scene stays fixed: world growth here means retained revisions and execution history, not increasing object count. Object-count scaling needs a separate experiment.

## Measured complete sequences

Positive savings mean memory took less time. A failed or incomplete sequence cannot claim a complete-stream speed benefit.

| Block | Simple complete stream | Memory complete stream | Memory saving | Both completed |
| --- | ---: | ---: | ---: | --- |
{streams}

Fresh preparation cost **{sec(summary['preparationMillis'])}**. Preparation is shown separately and charged once in the memory-adoption total. It includes earning the seed, creating the common initial world, closure and its inventory. Both comparison arms then receive checked copies. This is a conservative experimental adoption charge, not a claim that simple control has no world-creation cost.

The comparison runs on this host's S drive, in an isolated checkout with enough space for the full evidence history. Older095 timings used another drive and a different cold-copy workload; do not interpret cross-edition wall-time differences as a performance improvement. Each actual Java owner is pinned to two logical processors with a 512 MiB heap. Other host activity and operating-system cache effects remain possible.

## What the result means

The short adjustments expose a concrete weakness: a warm memory lookup can still select a three-stage procedure when simple control completes directly. Simple adjustment goals took about 15.86–17.30 seconds; remembered procedures took 42.39–46.79 seconds. Some long/recovered requests were faster with memory, but that did not repay the full sequence cost. Faster recall is not enough; the chosen procedure also needs to fit the remaining work. This is a comparison of explicit controls, not a test showing an adaptive selector chose well.

## Every task and retained result

Recovery time is included in goal time and shown separately only for explanation. It is never added twice. Complete stream totals also include copying, startup, world observation, exports/full audits, final reopening, persisted-state checks, actual saved-skill reuse and shutdown.

| Stream | Request | Bank before | Bank after | Revision | Complete goal | Recovery, included | Lookup | Saved skill |
| --- | --- | ---: | ---: | ---: | ---: | ---: | --- | --- |
{table}

Use the offline explorer to compare blocks and filter tasks. Its charts derive from the retained measurements, not illustrative performance numbers. Full original native ticks, failed alternatives, lookup status and later starting-state differences are in the [independent analysis](docs/readmes/097/analysis.json).

## Lookup time and chosen work

| Control | Main goal calls | Lookup within goals | Charged native ticks | Export/full-audit phases | Reopen/persistence phases | Separate skill phases |
| --- | ---: | ---: | ---: | ---: | ---: | ---: |
{componentTable}

Lookup is nested within main goal time. The separate skill phase includes the demonstration's setup, goal and audit; reopen/persistence includes cold inspection of all prior goals and saved defaults. Do not add nested columns as though they were disjoint. The complete stream totals above remain the complete-cost comparison.


Memory lookups ranged from about 0.242 to 13.658 seconds. Warm receipts reduce some retrieval costs, but the four short-adjustment requests in the memory arm still selected the longer remembered procedure. Full lookup, native execution and report verification are separate observations in the analysis. Neither a cache hit nor a smaller lookup duration establishes better complete-task performance.

## What recovery and saving mean here

The interruption trigger waits for an actual stage-zero checkpoint of at least 1,200 ticks. The process stop checks PID, parent, start time, checkout and isolated project. Each restart retains original checkpoint bytes and sample prefixes, confirms the task remains paused, and requires explicit resume. Changed later stages need distinct verified forecasts. The two-resume, 115,200-tick, 20-minute active-work and 90-second attempt budgets remain unchanged; missing tails are charged conservatively.

After the six requests, the same project is closed and reopened. Exact world revision/hash, retained bank, saved skill identity, source goal and full defaults are checked. The last saved procedure is executed through its registered Java handler against the continuing world as a separate seventh task. It may begin close to its target; its timing is reported separately from the six full-distance/adjustment goals and is not called an equivalent full-distance test.

## What the costs do and do not establish

The measured workflow starts at the registered Java world-goal call and includes app verification. LLM chat planning, browser work and asset creation are not exercised or timed here. One surviving external monotonic clock spans the study. Disjoint phases preserve unattributed orchestration gaps. A goal interval includes actual execution, report verification, commit, requested retention and skill creation; for interrupted tasks it also includes interruption detection, process closure, evidence capture, reopening and explicit resume. Study exports and extra audits are separate phases. Screenshots and independent offline analysis occur outside the timed sequences.

Java's cross-boot complete-task duration remains unavailable and ineligible for calibration. The external clock does not become motion authority or training data. No cost fit or learned selector is promoted. These workloads do not isolate the cost of writing one skill from the accompanying execution and memory retention.

Initial sources, requests, resources and preparation match. Later actual frames, earned memory contents and cache state can differ. The independent inspector records those differences; it does not silently turn continuing trajectories into exactly paired calibration cases. Bank size, world revision, direction and warmup vary together. Extra full audits can prime evidence caches and their costs are included. Two opposite-order blocks support a descriptive comparison here, not a population confidence interval or an isolated causal effect of bank growth.

## Verification retained

- Parent complete GitHub CI passed before publication; its pending state was recorded before the cohort.
- {jp} focused Java methods passed across {len(java)} classes; {verification['java']['skipped']} conditional methods skipped. Original XML and logs are retained.
- 135 existing desktop checks and 11 new interval/continuity checks passed.
- {mutations['refusals']} altered metadata/accounting cases were refused using copied inputs; measured native originals were not rewritten.
- The unchanged edition 093 independent inspector recomputes source identity, full native traces, protected-object motion, forecast/actual comparisons, recovery provenance, budget charges and one matching world commit.
- The new edition 097 inspector adds exact continuing-state and memory/skill progression, complete costs, same-project cold persistence and retained-procedure execution.

Read the [verification record](docs/readmes/097/verification.json) for exact source-qualified results and skips, and the [original evidence index](docs/readmes/097/original-evidence.json) for compressed/raw byte counts and SHA-256. No fresh live-model conversation, physical measurement, LLM weight training, generalized autonomy or consciousness is claimed.

## Actual application views

Requested window widths are 1600/1100 pixels; the captured client areas are 1584×1061 and1084×1061. The expanded motion plot shows the final actual stage; complete exports retain every earlier stage and recovery forecast. These are screenshots of the existing integrated application after actual native execution. The report is the one verified before the associated world commit; it is not a generated mockup.

{caption}

The extra expanded chart capture is outside all timed measurements and leaves world state unchanged. Its first screenshot assertion incorrectly expected six SVG elements; this report has one chart with selectable metrics. The assertion was corrected against the actual markup, retaining the first failure. No study or report data was changed.

'''+ '\n\n'.join(f"![Actual097 report, {c['stream']} at {c['width']} pixels](docs/readmes/097/{c['file']})" for c in captures)+'''

## Open the app in a separate review project

From this checkout root, with JDK 21 and Node.js installed:

```powershell
$env:FRAYNIX_WORKSPACE_PROJECT = Join-Path (Get-Location).Path 'build/review097/project'
$env:FRAYNIX_WORKSPACE_PERSISTENT = '0'
./scripts/Start-Agent-Workspace.ps1
```

The existing launcher prepares pinned dependencies when needed and opens the integrated application. Use its `-Offline` option only after those dependencies have been cached. Keep the project path the same to return to this review state. The setting above stops this review's Java owner when its interface closes; it does not erase saved worlds or skills. Keep live installations in their own checkouts and projects.

The workspace provides chat, a real browser, creation canvas, Java Brain, original Game Lab, spatial/native world controls and a verified-skill library. These are existing capabilities with prior edition evidence; edition 097 specifically exercises native world goals, persistence and procedure reuse. Optional local chat requires an available Ollama server/model. The measured deterministic world workflow does not require a new model download.

To review without a model, open the actual reports and saved procedures in the project library, inspect the native evidence and use the registered controls. The fresh project starts without the study bank. For the exact 24-task sequence, use the fixture and reproduction commands below; screenshots from the measured project do not imply a new empty project already contains those results.

## Reproduce and inspect

For historical reproduction, use the annotated `readme-097` tag. Use JDK 21, the repository's Gradle wrapper and already installed desktop dependencies. Build the Java engine and source-isolation checks; run the focused Java and existing desktop tests. The study uses a fresh `build/ui097-study` and refuses to overwrite an existing cohort. Save the genuine parent CI response as `build/ci097-gate.json`; the runner checks the exact commit/run identity and refuses a failed or cancelled run. Publication additionally requires a successful final response in `build/ci097-final.json`. Do not fabricate that receipt or point the study at a live project.

```powershell
.\\gradlew.bat :verifySourceIsolation :workspace-engine:installDist --no-daemon --max-workers=1 --console=plain
node scripts/Study-Build095.cjs --stamp
node --test workspace-desktop/test/costs097.test.cjs
npm --prefix workspace-desktop test
node workspace-desktop/test/continuing097-integration.cjs
node scripts/Inspect-Continuing097.cjs
node scripts/Check-Continuing097.cjs
.\\scripts\\Verify-Documentation.ps1
```

The existing build-receipt tool retains095 in its filename but records current source and installed artifacts. The actual local build used cached dependencies with `--offline`. No Docker or WSL is needed. Original failed attempts, if any, are retained and separately identified; compiled code is not changed while an owned native study is running.

## Next acceptance gate

Next, compare a cost-aware choice of simple versus remembered procedure on separately declared short, long and recovered tasks. The acceptance gate is improved complete task cost without lower success or weaker evidence checks. Charge selection work and include negative cases where memory is the wrong choice. If memory does not repay its cost, report that and preserve simple control. Separate bank growth from task direction and cache state before fitting a cost model. A future cost-aware selector needs matched calibration coverage and separate evaluation; external recovered timings remain outside training until a durable clock contract is independently tested. Keep progressing toward useful chat-driven creation and world tasks in the same integrated Java application.
'''
if draft:
 readme=readme.replace('The parent main build', '**Publication pending: parent CI is still running.** The parent main build').replace('completed successfully on', 'was checked on').replace('before publication. It was checked', 'and must complete successfully before publication. It was checked').replace('Parent complete GitHub CI passed before publication;', 'Parent complete GitHub CI is still pending;').replace('its pending state was recorded before the cohort.', 'its state was recorded before the cohort.')
 lead='**Draft release — CI and publication pending.** '+lead
write(r/'README-097.md',readme)
write(r/'README.md','# Fraynix Agent Workspace\n\n'+lead+'\n\n[Edition097: continuing-world costs](README-097.md) · [Interactive offline results](docs/readmes/097/index.html) · [Previous edition](README-096.md) · [Contributor direction](README-CONTRIBUTORS.md)\n\nOne local visual workspace with Java-owned goals, physics, browser and creation tools, retained skills and verified world outcomes. Read the edition for current functions, evidence, limits and next steps.\n')
rootread=r/'README.md'
write(rootread,rootread.read_text(encoding='utf8')+'\n## What the application does\n\n- Java maintains durable goals, typed tools, original evidence, explicit recovery and retained procedures.\n- The integrated workspace includes chat, browser and creation surfaces, the original game/runtime and native 3D world tools. Their earlier validation remains linked in the edition history.\n- Edition097 measures continuing world history and growing verified memory. It preserves current controllers and every source/outcome check; it does not add a new language model or claim physical accuracy.\n\n'+('![Actual edition097 integrated task report](docs/readmes/097/'+run['captures'][0]['file']+')\n\n'+caption+'\n\n' if run['captures'] else '')+'See the full [README097](README-097.md) for setup, step-by-step reproduction, results, current limits and the next acceptance gate.\n')
write(r/note,'# Continuing-world costs — edition097\n\n'+lead+'\n\nSee [[README-097.md]], [[docs/EDITION_097_CONTRACT.md]] and [[docs/readmes/097/verification.json]]. Keep Java ownership, original outcome checks, explicit recovery and measured complete cost. No cost-fit promotion.\n')
nodes=[{'id':m['id'],'type':'file','file':m['source'],'x':i*380,'y':0,'width':350,'height':240} for i,m in enumerate(system['modules'])];js(r/'docs/obsidian/105-continuing-costs.canvas',{'nodes':nodes,'edges':[{'id':'e'+str(i),'fromNode':nodes[i]['id'],'fromSide':'right','toNode':nodes[i+1]['id'],'toSide':'left'} for i in range(len(nodes)-1)]})
entry='## Edition097 — locally measured, publication pending\n\n'+lead+f"\n\nStudy source {run['source']}; parent CI{ci['id']} passed. {jp} Java methods passed, {verification['java']['skipped']} skipped;135 desktop,11 accounting and{mutations['refusals']} altered-evidence checks pass. All 334 production engine classes match096. Work in S:/Codex-Work/fraynix-workspace-edition097. Preserve live045/qa046/private data and frozen001–096. No subagents; heavy checks serial. No new fit or model inference. Next: measured bottleneck improvement with matched coverage; recovered external times remain ineligible for training. Actual main publication and fetched receipt still required.\n\n"
if draft:
 print(json.dumps({'draft':True,'goals':total,'archives':len(archive),'distinct':len(unique),'sourceFiles':len(inventory)}));sys.exit(0)
for p in ['AGENTS.md','docs/PROJECT_MEMORY.md','docs/DEVELOPMENT_LOG.md']:write(r/p,entry+(r/p).read_text(encoding='utf8'))
write(r/'docs/obsidian/00-home.md','[[README-097.md|Edition097 — continuing-world costs]] · [[docs/obsidian/105-continuing-costs.md]]\n\n'+(r/'docs/obsidian/00-home.md').read_text(encoding='utf8'))
write(r/'docs/readmes/INDEX.md','[Edition097 — continuing-world costs](../../README-097.md) · [Offline evidence](097/index.html)\n\n'+(r/'docs/readmes/INDEX.md').read_text(encoding='utf8'))
guide=r/'README-CONTRIBUTORS.md';text=guide.read_text();text=text.replace('For edition096, the isolated source checkout is `D:/Codex-Work/fraynix-workspace-edition096`, branch `edition096`, based on verified095 main `55d95f472133ff412a1940613e5bc0f9c179ad59`.','For edition097, the isolated source checkout is `S:/Codex-Work/fraynix-workspace-edition097`, branch `edition097`, based on verified096 main `453e8708ab9353d86ed666f3d059d28563372530`.');write(guide,entry+text)
write(r/'docs/ARCHITECTURE.md',entry+(r/'docs/ARCHITECTURE.md').read_text(encoding='utf8'))
print(json.dumps({'goals':total,'completed':complete,'archives':len(archive),'distinct':len(unique),'sourceFiles':len(inventory),'javaPassed':jp,'skipped':verification['java']['skipped']}))
