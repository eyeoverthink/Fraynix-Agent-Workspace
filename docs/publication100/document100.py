from pathlib import Path
import json,hashlib,gzip,subprocess,shutil,xml.etree.ElementTree as ET,html
r=Path.cwd();d=r/'docs/readmes/100'
def load(p):return json.loads(p.read_text(encoding='utf-8-sig'))
def write(p,s):p.parent.mkdir(parents=True,exist_ok=True);p.write_text(s,encoding='utf8',newline='\n')
def js(p,v):write(p,json.dumps(v,indent=2,ensure_ascii=False)+'\n')
def sha(b):return hashlib.sha256(b).hexdigest()
assert subprocess.run(['git','show-ref','--verify','--quiet','refs/tags/readme-100']).returncode==1,'Published editions are immutable'
run=load(r/'build/ui100-handoff-attempt3/result-final.json');a=load(r/'build/analysis100.json');review=load(r/'build/review100/result.json');ci=load(r/'build/ci100-status.json')
assert run['passed'] and a['passed'] and review['passed']
classes=[]
for p in sorted((r/'workspace-engine/build/test-results/test').glob('TEST-*.xml')):
 x=ET.parse(p).getroot();classes.append({k:x.attrib[k] for k in ['name','tests','failures','errors','skipped','time']})
assert classes and not sum(int(c['failures'])+int(c['errors']) for c in classes)
for name,count in [('desktop100-tests-final.log',135),('inputs100-tests.log',4)]:
 text=(r/'build'/name).read_text(encoding='utf-8-sig');assert f'pass {count}' in text and 'fail 0' in text
verification={'passed':True,'modelCohortSource':run['source'],'finalSource':subprocess.check_output(['git','rev-parse','HEAD'],text=True).strip(),'java':classes,'existingDesktopChecks':135,'selectionChecks':4,'independent':a,'review':review,'layoutRegression':load(r/'build/ui100-layout-final/result.json'),'sourceComparison':load(r/'build/source-comparison100.json'),'parentCI':ci,'scope':'Real recorded-data analysis and local-model generated creation through the existing app. Explicit supplied reusable checks, native source and receipt validation, actual rendered controls, cold source restoration. No speedup or physical generalization claim.'}
js(d/'verification.json',verification);js(d/'analysis.json',a)
archive=[];unique={}
def pack(p,label):
 raw=p.read_bytes();h=sha(raw)
 if h not in unique:
  z=gzip.compress(raw,mtime=0);dest=d/'original'/(h+'.gz');dest.parent.mkdir(parents=True,exist_ok=True);dest.write_bytes(z);unique[h]={'path':dest.relative_to(r).as_posix(),'bytes':len(z),'sha256':sha(z),'rawBytes':len(raw),'rawSha256':h}
 archive.append(dict(unique[h],logicalPath=label))
for folder in ['ui100-handoff','ui100-handoff-attempt2','ui100-handoff-attempt3','review100','review100-initial','ui100-layout','ui100-layout-final']:
 for p in (r/'build'/folder).rglob('*'):
  if p.is_file() and not any(k in p.parts for k in ['desktop-profile','runtime-042','.northstar-kernel']) and p.name!='host.json' and p.suffix in ['.json','.jsonl','.fworld','.pending','.html','.css','.js','.csv','.log','.png']:pack(p,folder+'/'+p.relative_to(r/'build'/folder).as_posix())
for p in (r/'build').glob('*100*'):
 if p.is_file() and p.suffix in ['.json','.log','.py','.cjs']:pack(p,'validation/'+p.name)
for p in (r/'workspace-engine/build/test-results/test').glob('TEST-*.xml'):pack(p,'java/'+p.name)
js(d/'original-evidence.json',{'files':archive,'distinctNewPayloads':len(unique),'scope':'Exact isolated originals, actual generated code/checks and retained failed attempts. Excludes private live projects, desktop profiles, runtime host credentials, personal footage and model weights.'})
for c in review['captures']:shutil.copyfile(r/'build/review100'/c['file'],d/c['file'])
kinds={x['path']:x['hashKind'] for x in load(r/'docs/readmes/099/file-inventory.json')}
for p in ['docs/EDITION_100_CONTRACT.md','workspace-desktop/inputs100.cjs','workspace-desktop/renderer/inputs100.js','workspace-desktop/renderer/inputs100.css','workspace-desktop/test/inputs100.test.cjs','workspace-desktop/test/handoff100-integration.cjs','workspace-engine/src/test/java/fraymus/workspace/DataHandoff100Test.java','scripts/Inspect-Handoff100.cjs']:kinds[p]='utf8-lf'
inventory=[]
for p,kind in sorted(kinds.items()):
 b=(r/p).read_bytes();b=b.decode('utf8').replace('\r\n','\n').encode() if kind=='utf8-lf' else b;inventory.append({'path':p,'bytes':len(b),'sha256':sha(b),'hashKind':kind})
js(d/'file-inventory.json',inventory)
note='docs/obsidian/108-verified-data-handoffs.md'
modules=[('inputs','Visible pinned inputs','workspace-desktop/renderer/inputs100.js'),('selection','Source restoration and recheck','workspace-desktop/inputs100.cjs'),('java','Java data and creation ownership','workspace-engine/src/main/java/fraymus/workspace/GoalRunner.java'),('creation','Source-derived behavior checks','workspace-engine/src/main/java/fraymus/workspace/Creation.java'),('generation','Actual local-model generation','workspace-engine/src/main/java/fraymus/workspace/CreationPlanner044.java'),('audit','Independent source and outcome inspection','scripts/Inspect-Handoff100.cjs')]
system={'modules':[{'id':key,'name':name,'source':src,'note':note} for key,name,src in modules],'screenshots':[{'path':'docs/readmes/100/'+c['file'],'caption':c['caption']} for c in review['captures']],'evidence':[{'path':'docs/readmes/100/'+n,'caption':n} for n in ['analysis.json','verification.json','original-evidence.json']]};js(d/'system-map.json',system)
table='\n'.join(f"| {x['input']} | {x['rows']} | {x['dataMillis']/1000:.2f} s | {x['creationMillis']/1000:.2f} s | {x['speedFlags']} | {x['generationJobs']} |" for x in a['results'])
screens='\n\n'.join(f"![{c['caption']}](docs/readmes/100/{c['file']})\n\n{c['caption']}" for c in review['captures'])
readme=f'''# Edition 100 — verified data becomes working content

[Previous:099](README-099.md) · [Offline interactive guide](docs/readmes/100/index.html) · [Edition history](docs/readmes/INDEX.md) · [Contributor direction](README-CONTRIBUTORS.md)

Edition100 connects two existing capabilities inside the same workspace: analyze a selected dataset, then create a working application from that verified result. The user can see the chosen source, review the behavior contract and keep using the resulting procedure with fresh data. Java still owns execution, provenance and completion.

## What changed

**Task inputs** now shows the selected motion dataset and the exact creation file. Chat creation no longer silently takes the first CSV from whichever artifact happens to be open. A pinned source keeps its file, version and hash while you browse other results. Clear or replace it explicitly. On restart the app asks Java to recheck it; a stale or changed source is shown as needing attention and cannot silently become a different input.

**Create from verified data** is available for completed data tasks. Java reconstructs the analysis from its frozen original CSV and mapping, verifies the report and actual canvas receipt, and checks that the report files still match the analysis. It supplies the normalized CSV, its precise binding, the parent task and the original quality flags. The new creation task retains that lineage and rechecks it during execution and completion.

The button opens the existing **Create & verify** dialog with that exact source selected. Its editable behavior contract uses **sourceRows** and **sourceCount**. Java derives counts from the current input before code generation; the checks remain fixed throughout repair. The local model writes the actual HTML, CSS and JavaScript. This is a visible two-step workflow with explicit user selection and supplied checks, not a claim that the model invented verification or autonomously discovered the dataset.

The input chooser is temporary so it does not consume the conversation area. Creation-source restoration is implemented; the separately selected motion dataset is not automatically restored. Wide generated tables can scroll within the canvas on narrow displays, and Expand gives the content more space. A compact source summary stays visible. Existing Browser, Creation canvas, Game Lab, Java brain, native worlds, procedural skills and Mission evidence remain integrated.

## Actual recorded-data and model trial

The completed cohort used installed **gpt-oss:20b**, digest `{run['model']['digest']}`. Inference retained the existing four-thread CPU-only policy. The two inputs are the unchanged frozen edition013 Delfys validation and training CSVs; they are recorded data, not newly captured footage, model training or calibrated physical actuation.

| Input | Rows | Analysis task | Creation task | Speed flags | Generation jobs |
|---|---:|---:|---:|---:|---:|
{table}

These are single-run submission-to-completion times, including waiting and verification. They are not a comparative speed benchmark. Setup and cold restart are separate. Mission’s model-request counter covers chat routing; the child creation generator has its own retained receipts and the generation-job counts above. Exact router/generator receipts, original prompts, code, checks and timings are retained in the [original evidence index](docs/readmes/100/original-evidence.json).

The first workflow saved motion100 and explorer100. After closing and reopening the owned project, Java restored the pinned source with fresh verification. The changed-input workflow reused both saved procedures against a different recorded CSV and tighter declared data thresholds. Source-bound counts were recalculated, rather than copying the earlier result. A verified analysis can correctly contain quality warnings; these warnings stayed attached to the handoff.

The fresh-source creation needed two actual model drafts. Its first draft put the row count in an input value, while the frozen contract required visible text. Java refused that draft, returned the observed failure to the model and verified the repaired second draft against the unchanged checks. Both versions and their receipts are retained.

Independent checks reconstructed normalized rows and total travel from the original CSV. They compared the generated artifact's exact input bytes and source lineage, then exercised the actual rendered table: all rows, exact row filtering, no-match filtering and reset. Displayed values were compared with the source. The [independent inspector](scripts/Inspect-Handoff100.cjs) also refuses five altered evidence variants.

## Retained failures and limits

Independent inspection initially assumed that the table ID was on the outer table and that the surface checker returned a receipt object. The actual generated table used its body ID and the checker returned individual checks. The corrected inspector checks both valid table-body placements without relaxing row counts or values. A restored-canvas navigation race was retained and corrected by waiting for the saved view to settle before input checks. The final value check also accepts an explicit “s” or “m” suffix only in its corresponding column; a wrong unit is refused. This corrected the harness's bare-number assumption without changing generated content or numeric tolerance. The original failed inspections remain in the archive.

The first UI harness used an incorrect host name; it was corrected before any model request in that attempt. An existing source-integrity test fixture also needed the new interface files; its refusal was retained, and all135 existing desktop checks now pass.

The first actual model creation attempt reached its saved time budget and was interrupted. It earned no successful skill. Its proposed checks also hard-coded49 rows. Those checks and the failed attempt were retained without rewriting them. The next fresh cohort used the existing visible, source-parameterized contract and a compact table/filter/reset explorer. Its optional chart was removed from the requested scope before that cohort; the time limits and verification gates were unchanged. See the [dated prospective contract and amendment](docs/EDITION_100_CONTRACT.md).

There is no claim of improved LLM weights, unlimited memory, consciousness, causal understanding or automatic support for arbitrary files. The current handoff supports completed registered motion-data reports and CSV creation inputs. Source labels remain declarations; normalized values inherit the original measurement assumptions. The model can still fail, and retained procedures still need new outcome checks.

## Try it

1. Launch the existing workspace in its own project. Choose an installed local model.
2. Import a CSV through Data tasks, declare its columns/units/source and select it for chat.
3. Ask for an analysis with explicit quality limits. Wait for a verified result.
4. Open **Task inputs → Change**, select the completed data task and choose **Create from verified data**.
5. Review the selected normalized file, request and behavior checks in Create & verify. Give the successful procedure a name, then build.
6. Inspect the live generated content and Mission evidence. Filtering and reset should work with the actual source values.
7. Reopen the same project, select a different input, analyze it and hand off its result. Reuse the saved creation procedure and verify its fresh counts. Clearing a source does not delete the original artifact.

## Ownership and persistence

```mermaid
flowchart LR
 D[Selected original CSV and mapping] --> A[Java analysis and rendered report checks]
 A --> H[Explicit normalized-file handoff]
 H --> C[Source-derived contract and local model code]
 C --> V[Java source checks and actual canvas behavior]
 V --> S[Saved procedure with fresh-input reuse]
 S --> H
```

```text
workspace-desktop/renderer/inputs100.*  visible selection and handoff
workspace-desktop/inputs100.cjs         project-local pin and fresh verification
workspace-engine/.../GoalRunner.java   data lineage, admission and completion
workspace-engine/.../Creation.java     original creation and count contracts
workspace-desktop/test/*100*           actual local-model and selection trials
scripts/Inspect-Handoff100.cjs         independent source/result checks
docs/readmes/100/                      immutable documentation and originals
```

Private project state, model weights and live camera data remain outside Git. A new computer needs its own installation and a separately verified private-state transfer. This edition does not change the CPU-only planning policy or install software on fraymus-home.

## Verification and next gates

40 focused Java methods,139 desktop/selection checks, actual UI/model trials and independent refusal checks are recorded in [verification.json](docs/readmes/100/verification.json). Native world, controller and atomic-commit verifiers were preserved. After the model cohort, one CSS adjustment kept Start task fully visible alongside Mission evidence. Final actual layout checks were repeated; compiled Java inputs and installed JARs are byte-identical to the tested cohort. Historical README001–099 and their assets remain frozen.

Parent CI42593795811 at the recorded check: **{ci['status']} / {ci['conclusion']}**. Local validation and GitHub CI are separate facts. Publication is confirmed by a separate fetched-object receipt.

Next: make the supported handoff available to durable multi-stage chat goals with explicit stage contracts, so a request can analyze, create, inspect and save without repeating selections. Then test interruption between stages, changed files, more varied source shapes and complete task costs. Add automatic contract generation only when it matches supplied-contract reliability on fresh inputs. On the new computer, separately validate GPU inference and compare complete task measurements under the same checks.

The direction remains one local agent environment: requests become observable work; results become reusable material; memories guide future attempts without certifying them. Progress is demonstrated by complete tasks and retained, rechecked evidence.

[Canonical source inventory](docs/readmes/100/file-inventory.json) · [Results](docs/readmes/100/analysis.json) · [Obsidian map](docs/obsidian/108-verified-data-handoffs.canvas)

## Actual screenshots

{screens}
'''
write(r/'README-100.md',readme)
write(r/note,'# Verified data handoffs\n\n[[README-100|Edition100]] · [[README-099|Previous edition]]\n\nOne explicit source feeds checked Java analysis, source-derived creation checks, actual generated code and fresh-input skill reuse. A past success is not a new outcome.\n\n'+ '\n'.join('[['+src+'|'+name+']]' for _,name,src in modules)+'\n')
nodes=[{'id':k,'type':'file','file':src,'x':(i%3)*360,'y':(i//3)*230,'width':320,'height':180} for i,(k,_,src) in enumerate(modules)];edges=[{'id':'e'+str(i),'fromNode':modules[i][0],'fromSide':'right','toNode':modules[i+1][0],'toSide':'left'} for i in range(len(modules)-1)];js(r/'docs/obsidian/108-verified-data-handoffs.canvas',{'nodes':nodes,'edges':edges})
write(d/'index.html','''<!doctype html><html lang="en"><meta charset="utf-8"><meta name="viewport" content="width=device-width,initial-scale=1"><title>Fraynix · Edition100</title><style>body{margin:0;background:#10221f;color:#e1eeea;font:16px/1.6 system-ui}main{max-width:1140px;margin:auto;padding:24px}h1{font-size:clamp(30px,5vw,58px);line-height:1.12}a{color:#a5ead5}nav{display:flex;gap:14px;flex-wrap:wrap}button,input,select{font:inherit;border:1px solid #56786b;border-radius:8px;padding:8px 13px;background:#183b30;color:inherit}button[aria-pressed=true]{background:#91d9bc;color:#10251e}.panel{margin:24px 0;padding:22px;background:#19332c;border:1px solid #405d52;border-radius:16px}.cards{display:grid;grid-template-columns:repeat(auto-fit,minmax(230px,1fr));gap:14px}img{width:100%;height:auto;border-radius:9px}li{overflow-wrap:anywhere}small{color:#b2cdc2}section[hidden]{display:none}input{max-width:90%}select{width:100%;max-width:100%}footer{margin:30px 0}</style><main><small>FRAYNIX AGENT WORKSPACE / 09 NOVEMBER 2026 / EDITION100</small><h1>Verified data becomes<br>working content.</h1><p>Select it. Check it. Create with it. Reuse the procedure on a fresh source.</p><nav><a href="../../../README-100.md">Full README</a><a href="../099/index.html">Previous099</a><a href="../INDEX.md">History</a><a href="../../../README-CONTRIBUTORS.md">Contributor direction</a></nav><div class="panel"><nav id="tabs"><button data-tab="overview" aria-pressed="true">Workflow</button><button data-tab="evidence" aria-pressed="false">Evidence</button><button data-tab="source" aria-pressed="false">Source map</button></nav></div><section id="overview"><div class="cards">'''+''.join('<article class="panel"><h2>'+html.escape(name)+'</h2><a href="../../../'+src+'">Open owned source</a></article>' for _,name,src in modules)+'''</div><div class="panel"><h2>Actual application</h2><select id="shot" aria-label="Screenshot"></select><p id="caption"></p><img id="image" alt="Actual Fraynix workspace"><p>These are real retained app views. The original model attempt that exceeded its time limit remains in the evidence archive.</p></div></section><section id="evidence" hidden><div class="panel"><h2>Checks and limits</h2><p>Real recorded input, real local-model code, exact source lineage, source-derived checks and fresh-input reuse. Explicit selection and supplied behavior checks are part of the workflow. No physical generalization or speedup claim.</p><a href="analysis.json">Independent results</a> · <a href="verification.json">Full verification</a> · <a href="original-evidence.json">Originals and failures</a><p>Parent CI is recorded separately in verification.json. A screenshot alone does not prove completion.</p></div></section><section id="source" hidden><div class="panel"><h2>Canonical source inventory</h2><input id="search" placeholder="Filter paths" aria-label="Filter source files"><p id="count"></p><ul id="files"></ul><a href="file-inventory.json">Complete hash inventory</a></div></section><footer>One continuing Java-owned workspace. <a href="../../../docs/obsidian/108-verified-data-handoffs.canvas">Obsidian map</a></footer></main><script id="system-data" type="application/json">'''+json.dumps(system).replace('</','<\\/')+'''</script><script id="inventory-data" type="application/json">'''+json.dumps(inventory).replace('</','<\\/')+'''</script><script>const system=JSON.parse(document.querySelector('#system-data').textContent),inventory=JSON.parse(document.querySelector('#inventory-data').textContent);for(const b of document.querySelectorAll('[data-tab]'))b.onclick=()=>{for(const x of document.querySelectorAll('[data-tab]'))x.setAttribute('aria-pressed',String(x===b));for(const id of ['overview','evidence','source'])document.getElementById(id).hidden=id!==b.dataset.tab;};const shot=document.getElementById('shot');system.screenshots.forEach((s,i)=>shot.add(new Option(s.caption,String(i))));function show(){const s=system.screenshots[Number(shot.value)];document.getElementById('image').src=s.path.split('/').pop();document.getElementById('caption').textContent=s.caption;}shot.onchange=show;show();function files(){const hits=inventory.filter(f=>f.path.toLowerCase().includes(document.getElementById('search').value.toLowerCase())),list=document.getElementById('files');list.replaceChildren();document.getElementById('count').textContent=hits.length+' matching files; showing first50';for(const f of hits.slice(0,50)){const li=document.createElement('li'),a=document.createElement('a');a.textContent=f.path;a.href='../../../'+f.path;li.append(a);list.append(li);}}document.getElementById('search').oninput=files;files();</script></html>''')
print(json.dumps({'canonicalSources':len(inventory),'archiveEntries':len(archive),'uniquePayloads':len(unique),'screenshots':len(review['captures'])}))
