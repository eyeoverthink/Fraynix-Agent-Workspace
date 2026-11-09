from pathlib import Path
import json,hashlib,gzip,subprocess,shutil,xml.etree.ElementTree as ET,html
r=Path.cwd();d=r/'docs/readmes/099';d.mkdir(parents=True,exist_ok=True)
def load(p):return json.loads(p.read_text(encoding='utf-8-sig'))
def write(p,s):p.parent.mkdir(parents=True,exist_ok=True);p.write_text(s,encoding='utf8',newline='\n')
def js(p,v):write(p,json.dumps(v,indent=2,ensure_ascii=False)+'\n')
def sha(b):return hashlib.sha256(b).hexdigest()
a=load(r/'build/analysis099.json');run=load(r/'build/ui099-chat-attempt2/result.json');layout=load(r/'build/ui099-layout-complete/result.json');review=load(r/'build/ui099-review/result.json');assert review['passed'];assert a['passed'] and run['passed'] and layout['passed']
ci=load(r/'build/ci099-status.json');identity=load(r/'build/source-comparison099.json');classes=[]
for p in sorted((r/'workspace-engine/build/test-results/test').glob('TEST-*.xml')):
 x=ET.parse(p).getroot();classes.append({k:x.attrib[k] for k in ['name','tests','failures','errors','skipped','time']})
assert classes and not sum(int(x['failures'])+int(x['errors']) for x in classes)
desktop=load(r/'build/desktop099-summary.json');assert desktop['passed']
verification={'passed':True,'source':run['source'],'java':classes,'desktop':desktop,'layout':layout,'finalInterfaceReview':review,'productionClasses':identity,'parentCI':ci,'analysis':a,'scope':'Local model requests, actual native motion, recorded-data arithmetic and actual Chromium behavior. CI is separate; no physical accuracy, weight training or general intelligence claim.'}
js(d/'verification.json',verification);js(d/'analysis.json',a)
archive=[];unique={}
def pack(p,label):
 raw=p.read_bytes();h=sha(raw)
 if h not in unique:
  z=gzip.compress(raw,mtime=0);dest=d/'original'/(h+'.gz');dest.parent.mkdir(parents=True,exist_ok=True);dest.write_bytes(z);unique[h]={'path':dest.relative_to(r).as_posix(),'bytes':len(z),'sha256':sha(z),'rawBytes':len(raw),'rawSha256':h}
 archive.append(dict(unique[h],logicalPath=label))
for folder in ['ui099-chat','ui099-chat-attempt2','ui099-layout','ui099-layout-attempt2','ui099-layout-attempt3','ui099-layout-final','ui099-layout-refined','ui099-review','ui099-review-before-tab','ui099-review-before-mission','ui099-layout-complete']:
 for p in (r/'build'/folder).rglob('*'):
  if p.is_file() and not any(k in p.parts for k in ['desktop-profile','runtime-042','.northstar-kernel']) and p.name!='host.json' and p.suffix in ['.json','.jsonl','.fworld','.pending','.html','.css','.js','.csv','.log','.png']:pack(p,folder+'/'+p.relative_to(r/'build'/folder).as_posix())
for p in (r/'build').glob('*099*'):
 if p.is_file() and p.suffix in ['.json','.log','.py','.md','.cjs']:pack(p,'validation/'+p.name)
for p in (r/'workspace-engine/build/test-results/test').glob('TEST-*.xml'):pack(p,'java/'+p.name)
for c in review['captures']:
 shutil.copyfile(r/'build/ui099-review'/c['file'],d/c['file'])
js(d/'original-evidence.json',{'files':archive,'distinctNewPayloads':len(unique),'scope':'Exact isolated task originals and retained initial failures. Excludes desktop profiles, private live projects, runtime host credentials, personal footage and model weights.'})
kinds={x['path']:x['hashKind'] for x in load(r/'docs/readmes/098/file-inventory.json')};paths=set(kinds)
paths.update(['docs/EDITION_099_CONTRACT.md','workspace-desktop/renderer/workspace099.js','workspace-desktop/renderer/workspace099.css','workspace-desktop/test/chat099-integration.cjs','workspace-desktop/test/layout099-integration.cjs','workspace-engine/src/test/java/fraymus/workspace/Chat099Test.java','scripts/Inspect-Chat099.cjs'])
inventory=[]
for p in sorted(paths):
 b=(r/p).read_bytes();kind=kinds.get(p,'utf8-lf');b=b.decode('utf8').replace('\r\n','\n').encode() if kind=='utf8-lf' else b;inventory.append({'path':p,'bytes':len(b),'sha256':sha(b),'hashKind':kind})
js(d/'file-inventory.json',inventory)
note='docs/obsidian/107-local-model-workflows.md'
modules=[('chat','Conversation and adjustable layout','workspace-desktop/renderer/workspace099.js'),('java','Java task ownership','workspace-engine/src/main/java/fraymus/workspace/Agency043.java'),('world','Verified world procedures','workspace-engine/src/main/java/fraymus/workspace/WorldProcedure079.java'),('model','Java creation generation','workspace-engine/src/main/java/fraymus/workspace/CreationPlanner044.java'),('test','Actual local-model workflows','workspace-desktop/test/chat099-integration.cjs'),('audit','Independent original inspection','scripts/Inspect-Chat099.cjs')]
captures=review['captures']
system={'modules':[{'id':key,'name':name,'source':src,'note':note} for key,name,src in modules],'screenshots':[{'path':'docs/readmes/099/'+c['file'],'caption':'Actual app, '+c['at']+' — '+c['file']} for c in captures],'evidence':[{'path':'docs/readmes/099/'+n,'caption':n} for n in ['analysis.json','verification.json','original-evidence.json']]};js(d/'system-map.json',system)
table='\n'.join('| '+' | '.join([t['kind'],f"{t['callerMillis']/1000:.2f} s",str(t['routerCalls']),str(t['generationCalls']),t['savedSkill'] or ('Reused studio099-motion' if t['reusedSkill'] else '—')])+' |' for t in a['tasks'])
measured=next(t for t in run['tasks'] if t['kind']=='data');reused=next(t for t in run['tasks'] if t['kind']=='reuse');created=next(t for t in run['tasks'] if t['kind']=='creation')
java=sum(int(x['tests'])-int(x['skipped']) for x in classes)
lead='Edition099 makes the same integrated workspace easier to use and tests complete world, recorded-data and creation requests through the installed local gpt-oss:20b model. A fourth request reuses a saved data procedure after a cold restart.'
shots='\n\n'.join(f"![Actual099 {c['file']}](docs/readmes/099/{c['file']})\n\nCaptured {c['at']}. {c['caption']}. Actual isolated test project; world geometry is labelled synthetic, data comes from the frozen recorded CSV, and creation code came from the local model." for c in captures)
readme=f'''# Edition 099 — More room to work, real local-model workflows

[Previous:098](README-098.md) · [Offline interactive guide](docs/readmes/099/index.html) · [Edition history](docs/readmes/INDEX.md) · [Contributor direction](README-CONTRIBUTORS.md)

{lead}

## What changed in the application

Chat has more space by default, larger text and a larger composer. Drag the separator beside it, use Left/Right while the separator has keyboard focus, or use the width slider in **Skills, memory & settings**. Double-click resets its width. The stored preference is clamped to the available window. Browser and canvas are real embedded surfaces; their native bounds follow the layout. The Browser & files button expands the existing browser/library view. Opening a saved Mission result now selects Live preview after the artifact has refreshed, so an automatic world refresh cannot leave Spatial highlighted above an HTML report. The original tools, Game Lab, spatial views and creation controls remain in this application.

Activity is collapsed initially, with a visible Expand control. Mission still contains the detailed evidence and recovery controls. Opening it replaces the duplicate compact status card and preserves visible chat input and task buttons. Goal controls & recovery remain accessible in a compact expandable section. Outcome details are folded until requested, leaving more transcript space even after a task completes. The compact current-task card reflects Java's actual state: choosing a step, model planning, running/checking, needs attention, stopped or verified. **Open verified result** appears only for a completed, verified Java task. It uses the existing result-opening operation. Presentation does not invent progress or promote an outcome.

New replies preserve your reading position when you are looking at older messages. A New response button takes you to the latest text. The most recent conversation is retained in the isolated desktop profile, bounded to forty messages and 100,000 characters. An oversized saved message is explicitly marked as an excerpt. This is display continuity, not unlimited model context or permission to execute old chat. Java's task evidence remains separate. Malformed stored conversation data no longer prevents the interface from opening. Saved skill **versions**, task episodes and controller versions have separate labels.

The optional **Model planning for world procedures** setting is in Skills, memory & settings and is off by default. It lets the selected local model propose the next registered action before a verified world procedure starts. Java stores and seals that choice with the request, retains the review-only refusal, validates the tool and arguments, and verifies actual execution. Existing direct Java execution remains useful when no model decision is needed. No new dependency was added.

## What the real model completed

| Request | Submission to checked completion | Router calls | Creation generation jobs | Skill result |
|---|---:|---:|---:|---|
{table}

These are single-run descriptive times on this computer, including inference, native work, waiting and checks. They are not a speed benchmark or a claim of superiority. Setup and cold restart are separate; measured reopen time was {run['restartMillis']/1000:.2f} seconds. The model identity and its exact digest are retained in the task and generation receipts. CPU-only planning used four threads and no GPU offload, preserving graphics capacity. There were {sum(t['failedRouterCalls'] for t in a['tasks'])} failed parent-router requests in this completed cohort; generation attempts and actual code/checks are retained separately.

### 1. Chat into the persistent world

The harness imported a clearly labelled synthetic six-body scene, selected the two target poses and confirmed force/torque limits through the existing workspace interface. Those setup actions were not autonomous model discoveries. The request asked for both objects to move and rotate around obstacles, verification of the resulting revision, and a saved skill named studio099-arrangement.

The actual model participated in the Java-controlled tool sequence. Native controllers then ran distinct forecasts and actual motion. The independently inspected result selected **{a['world']['selected']}**, with {a['world']['stages']} native stages and {a['world']['ticks']} charged ticks. The committed world advanced once and matched its verified final frame. The procedure retained a skill only after completion. This exercises the current098 benefit gate: no suitable cost calibration means simple control is considered first, while bounded alternatives remain available. It does not establish that memory is faster or that the synthetic physics is calibrated to a real mechanism.

### 2. Chat into recorded data

The unchanged edition013 Delfys validation CSV was explicitly imported with seconds/metres and its recorded-data scope. The model requested the typed data workflow with a 0.05-second gap limit and 5 m/s speed limit. Java preserved the original CSV, calculated the report, rendered charts, checked eight actual assertions and saved studio099-motion.

Independent arithmetic from the original CSV agreed with the report: {measured['goal']['metrics']['rowCount']} rows, {measured['independent']['travel']:.9f} metres total travel and {measured['independent']['duration']:.9f} seconds duration. Gap flags: {measured['independent']['gaps']}; speed flags: {measured['independent']['speeds']}. These are descriptive measurements from a frozen source, not a fresh sensor recording or new permission to actuate physical hardware.

After closing and reopening the owned project, a fourth real-model request reused that exact saved procedure on the different frozen training CSV. New thresholds were 0.02 seconds and 0.4 m/s. Independent checks found {reused['independent']['gaps']} gap flags and {reused['independent']['speeds']} speed flags. A successfully verified report may correctly say that data quality failed. The new original bytes and source identity are preserved; reuse does not copy the old report or assert that a new task passed merely because the earlier one did.

### 3. Chat into working content

The model created a work-session tally with Add two, Undo and Reset. Java froze the behavioral contract before code generation, admitted actual code through its existing creation worker and verified the rendered artifact. There were {len(created['generation'])} retained generation jobs. The first generated tally failed because it used an input value where its own frozen checks required visible text. Java retained the actual failed checks and the model repaired its code under the unchanged contract. The parent router and the child creation generator have separate receipts: a zero router count is not evidence that no generation occurred.

Twelve additional actual canvas steps independently exercised 0 → 2 → 4 → 2 → 0, followed by Undo after Reset remaining at zero. The artifact uses local HTML/CSS/JavaScript and no external assets. The verified studio099-tally skill survived restart. This is actual generated and tested content; no sample implementation was substituted for the model's output. The exact source, frozen checks, model receipts and generated report are included in the original archive.

## Try it step by step

1. Launch the existing workspace with the normal project launcher. Keep a separate project for experiments; do not open a running installation's private database from another checkout.
2. Choose an installed local model. Open Skills, memory & settings to enable model planning for world procedures when you want that path. The setting does not add arbitrary tools or remove checks.
3. For a world task, use the existing Spatial world controls to select an actual world and confirm target poses and limits. Ask to arrange the selected targets and save a named skill. Watch the task card, then open the verified result or Mission for detailed native evidence.
4. For data, import a real CSV through Data tasks, select columns and units, declare its source and select it for chat. Ask for an analysis with explicit gap/speed thresholds and a skill name. Inspect the original-source audit and quality flags.
5. For creation, start with “Create and verify,” describe the behavior and give any required control IDs. Check the resulting live canvas yourself. The engine keeps the code and verification together.
6. Reopen the same project and explicitly reuse a saved procedure on a newly selected input. Inspect its new outcome. A retained skill is a checked way to work, not a guarantee on a different input.

Reproduction entry points are [layout checks](workspace-desktop/test/layout099-integration.cjs), [actual-model cohort](workspace-desktop/test/chat099-integration.cjs), [independent inspection](scripts/Inspect-Chat099.cjs) and the [prospective contract and amendment](docs/EDITION_099_CONTRACT.md). The cohort refuses to overwrite earlier attempts. Heavy/native/model work ran serially. No private live045/qa046 project was used.

## Verification, retained failures and limits

{java} focused Java test methods passed, covering task admission, sealed preferences, old-objective compatibility, domain scheduling, creation planning and retained completion gates. {desktop['tests']} existing desktop checks passed. The actual-app layout checks covered mouse and keyboard resizing, native browser/canvas bounds at 1600 and 1100 pixels, literal text rendering, scroll preservation, activity/Mission access, cold restoration and malformed saved conversation handling. A final review of the populated app also caught stacked status panels consuming chat space; the compact follow-up and pinned latest-message behavior were retested against the retained actual results. Source isolation passed. The production class comparison is retained in [verification.json](docs/readmes/099/verification.json); native physics and outcome verifiers were not replaced.

The independent world inspector preserved the inherited source, frame, forecast, actual-motion, sample, metric, budget and atomic-commit checks. It also refused four altered-evidence cases: changed target, changed charged work, missing native original and forged commit. Screenshot review supplements these checks; a screenshot alone does not prove success.

Initial failures are retained. The first layout harness mistakenly expected a receipt object where the surface API returns checks; its next attempt hit the closed-episode guard when it used the execution wrapper after a guided proof. The corrected harness checks actual surface results without reopening or rewriting that episode. The first world request really completed, but used the existing direct Java bootstrap and made zero model calls. It was rejected as a model-validation result. That finding led to the explicit planning option and a fresh second cohort. Creation inference is audited at its actual generation worker rather than inferred from router counters.

Parent GitHub CI at documentation time: **{ci['status']} / {ci.get('conclusion')}**, run {ci['id']}. This is separate from the local results; it is not represented as green unless GitHub reports success. Publication is confirmed by a separately fetched-content receipt, not by this prose. Historical README001–098 and their snapshots remain frozen.

The app is a useful, locally controlled agent environment with typed tools, durable tasks and observable outcomes. It is not evidence of consciousness, unrestricted general intelligence, or new LLM weight training. These tests cover selected supported workflows. Model mistakes, source ambiguity, expensive inference and unfamiliar geometry still require explicit limits and honest refusal. Local model availability and computer resources remain practical constraints.

## Architecture and direction

```mermaid
flowchart LR
  U[Request and selected input] --> J[Java task and sealed choices]
  J --> M[Optional local model proposal]
  M --> V[Typed admission and source checks]
  J --> V
  V --> W[Native world]
  V --> D[Recorded data]
  V --> C[Creation generator and canvas]
  W --> E[Original evidence and outcome checks]
  D --> E
  C --> E
  E --> S[Verified result and retained skill]
  S --> J
```

The next useful edition should make input selection and task handoffs easier across these same surfaces: identify exactly what is selected, show the requested success conditions before execution, and carry an artifact or dataset into the next supported procedure without forcing users to repeat context. Acceptance should include multiple fresh requests, changed inputs, interruption and recovery, and an independent baseline for any claimed efficiency improvement. Expand supported tasks only with complete handlers and observable checks. Keep the larger aim visible: the agent reasons and acts inside an environment whose consequences it can inspect, remember and reuse.

Suggested next gates: clearer selected-input cards; combined data-to-creation tasks with exact source bindings; task-level inference and generation timing in one readable view; more diverse user prompts and explicit failure/recovery trials; then broader geometry or perception when the workflow needs it. A larger catalogue alone would not establish better agency. Complete, understandable and verified work is the measure.

## Source and evidence map

```text
workspace-desktop/renderer/workspace099.js   presentation and retained chat
workspace-desktop/renderer/workspace099.css  responsive space and task card
workspace-engine/.../Agency043.java         durable request and admission
workspace-engine/.../Domain044.java         optional model world bootstrap
workspace-engine/.../ChatChain052.java      backward-compatible objective seal
workspace-desktop/test/*099*.cjs             actual layout/model workflows
scripts/Inspect-Chat099.cjs                  independent native originals
docs/readmes/099/                           screenshots, guide, source hashes, originals
```

[Complete canonical inventory](docs/readmes/099/file-inventory.json) · [Original evidence index](docs/readmes/099/original-evidence.json) · [Results](docs/readmes/099/analysis.json) · [Full verification](docs/readmes/099/verification.json) · [Obsidian map](docs/obsidian/107-local-model-workflows.canvas)

## Actual screenshots

{shots}
'''
write(r/'README-099.md',readme)
write(r/'README.md',f'# Fraynix Agent Workspace\n\n{lead}\n\n[Edition099: layout and real local-model workflows](README-099.md) · [Offline guide](docs/readmes/099/index.html) · [Previous edition](README-098.md) · [Contributor direction](README-CONTRIBUTORS.md)\n\nJava owns durable goals, physics, tools, verification and retained procedures. Chat, browser, creation canvas, original Game Lab and spatial views share those capabilities.\n\n![Actual099 creation](docs/readmes/099/creation099-1600.png)\n\nPublication receipt follows the actual push and fetched-content audit.\n')
for name,prefix in [('docs/readmes/INDEX.md','[Edition099 — real local-model workflows](../../README-099.md) · [Offline guide](099/index.html)\n\n'),('docs/obsidian/00-home.md','[[README-099.md|Edition099 — real local-model workflows]] · [[docs/obsidian/107-local-model-workflows.md]]\n\n')]:
 p=r/name;s=p.read_text(encoding='utf8');write(p,prefix+s if not s.startswith(prefix) else s)
entry=f'''## Edition099 — locally verified; publication pending

2026-11-09. Isolated S:/Codex-Work/fraynix-workspace-edition099, branch edition099; actual cohort source {run['source']}. {lead} {java} focused Java methods and {desktop['tests']} desktop checks pass; layout/restart and four actual-model tasks pass. Independent native originals and four altered-evidence refusals pass. Three saved skills survive restart; one is reused on changed recorded input. Larger resizable chat, retained display transcript and actual task summaries preserve the integrated app. Optional sealed model planning is off by default. Initial harness failures and zero-model direct world result retained. Parent CI {ci['id']}: {ci['status']}/{ci.get('conclusion')}; no false CI success. Main/tag publication and fetched receipt still pending.

Keep Java authority, all original/outcome checks, live045/qa046 private data and frozen001–098. No subagents; heavy/model/native work serial. No physical generalization or weight-training claim. Next: clearer selected inputs and connected data-to-creation workflows, tested with real requests and changed inputs.

'''
for name in ['AGENTS.md','README-CONTRIBUTORS.md','docs/PROJECT_MEMORY.md','docs/DEVELOPMENT_LOG.md','docs/ARCHITECTURE.md']:
 p=r/name;s=p.read_text(encoding='utf8');write(p,entry+s if not s.startswith('## Edition099 — locally verified') else s)
write(r/note,'# Edition099 — Real local-model workflows\n\n'+lead+'\n\n[[README-099.md|Full edition]] · [[docs/readmes/099/index.html|Offline guide]] · [[docs/obsidian/106-benefit-selection.md|Previous]]\n\nJava owns the task. The model proposes; actual tools and independent checks establish outcomes. Chat display memory is distinct from skills and neural/model learning.\n')
nodes=[{'id':key,'type':'file','file':src,'x':(i%3)*390,'y':(i//3)*300,'width':360,'height':240} for i,(key,_,src) in enumerate(modules)]
js(r/'docs/obsidian/107-local-model-workflows.canvas',{'nodes':nodes,'edges':[{'id':'e'+str(i),'fromNode':modules[i-1][0],'fromSide':'right','toNode':modules[i][0],'toSide':'left'} for i in range(1,len(modules))]})
cards=''.join(f'<article><strong>{html.escape(t["kind"])}</strong><span>{t["callerMillis"]/1000:.2f} seconds</span><small>{t["routerCalls"]} router calls · {t["generationCalls"]} generation jobs</small></article>' for t in a['tasks'])
figs=''.join(f'<figure><a href="{c["file"]}"><img src="{c["file"]}" alt="Actual app {c["file"]}"></a><figcaption>{c["at"]} — {c["file"]}</figcaption></figure>' for c in captures)
guide='''<!doctype html><html lang="en"><meta charset="utf-8"><meta name="viewport" content="width=device-width,initial-scale=1"><title>Fraynix edition099</title><style>body{margin:auto;max-width:1160px;padding:28px;background:#101d22;color:#dfebe7;font:17px/1.6 system-ui}a{color:#82d6ba}nav,.cards{display:flex;gap:14px;flex-wrap:wrap}h1{font-size:clamp(32px,5vw,62px);line-height:1.15}h2{margin-top:45px}article,section{border:1px solid #355253;border-radius:14px;padding:18px;background:#182c30}article{flex:1;min-width:180px}article span,article small{display:block}button,select,input{font:inherit;background:#203b3d;color:#e3f7ee;padding:9px;border:1px solid #638e82;border-radius:8px;max-width:100%;box-sizing:border-box}figure{margin:25px 0}img{width:100%;height:auto;border-radius:12px}figcaption,small{font-size:13px;color:#a8c0b9}table{border-collapse:collapse;width:100%;font-size:13px}td,th{padding:8px;border-bottom:1px solid #355253;text-align:left;overflow-wrap:anywhere}.scroll{overflow:auto}#map{display:flex;flex-wrap:wrap;gap:9px}#details{margin-top:10px;min-height:80px}#chart{display:grid;gap:12px}.bar{background:#356b5b;padding:8px;min-width:100px;box-sizing:border-box}code{overflow-wrap:anywhere}@media(max-width:500px){body{padding:18px}article{min-width:120px}td,th{padding:5px}}</style><nav><a href="../../../README-099.md">Full README</a><a href="../098/index.html">Previous edition</a><a href="original-evidence.json">Original evidence</a></nav><p>FRAYNIX · EDITION099 · 2026-11-09</p><h1>Room to think.<br>Actual work to verify.</h1><p>One integrated workspace. Local-model requests reach Java-owned world, data and creation tasks. Larger adjustable chat, readable progress and retained conversation make the work easier to follow.</p><div class="cards">__CARDS__</div><h2>Measured complete requests</h2><p>Single runs on this computer; not a speed comparison. Setup and reopening are separate. World geometry is labelled synthetic; the CSV is frozen recorded data.</p><label>Show task <select id="task"><option value="all">All tasks</option></select></label><div id="chart"></div><h2>Try the workflow</h2><ol><li>Choose an installed model and select your actual input.</li><li>Describe the result and the checks you need.</li><li>Follow the actual task card; open Mission for evidence and recovery.</li><li>Open the verified result and inspect its behavior.</li><li>Reuse a retained skill on a new input and check its new outcome.</li></ol><p>World model planning is an explicit option in Skills, memory &amp; settings. Direct Java execution remains available. Creation uses a separate Java model-generation worker. Neither route bypasses verification.</p><h2>Follow responsibility through the system</h2><div id="map"></div><section id="details" aria-live="polite"></section><h2>Actual application captures</h2>__FIGURES__<h2>What the evidence does and does not show</h2><p>Four real-model workflows, a cold restart, exact source checks, original native-motion inspection and independent rendered controls. Initial harness failures and the initial zero-model direct world request are retained. No consciousness, general intelligence, physical calibration or improved LLM weights is claimed.</p><p><a href="verification.json">Verification and CI status</a> · <a href="analysis.json">Full task measurements</a></p><details><summary>Canonical source inventory</summary><label>Find a source <input id="search" placeholder="File or module"></label><div class="scroll"><table><thead><tr><th>Path</th><th>Bytes</th></tr></thead><tbody id="files"></tbody></table></div></details><script id="system-data" type="application/json">__SYSTEM__</script><script id="inventory-data" type="application/json">__INVENTORY__</script><script id="results-data" type="application/json">__RESULTS__</script><script>const system=JSON.parse(document.querySelector('#system-data').textContent),inventory=JSON.parse(document.querySelector('#inventory-data').textContent),results=JSON.parse(document.querySelector('#results-data').textContent);const task=document.querySelector('#task');for(const t of results.tasks){const o=document.createElement('option');o.value=t.kind;o.textContent=t.kind;task.append(o)}function chart(){const root=document.querySelector('#chart');root.replaceChildren();const max=Math.max(...results.tasks.map(t=>t.callerMillis));for(const t of results.tasks.filter(t=>task.value==='all'||t.kind===task.value)){const bar=document.createElement('div');bar.className='bar';bar.style.width=Math.max(25,100*t.callerMillis/max)+'%';bar.textContent=t.kind+' · '+(t.callerMillis/1000).toFixed(2)+' s';root.append(bar)}}task.onchange=chart;chart();for(const m of system.modules){const b=document.createElement('button');b.textContent=m.name;b.onclick=()=>{const d=document.querySelector('#details');d.replaceChildren();const strong=document.createElement('strong');strong.textContent=m.name;const p=document.createElement('p');const a=document.createElement('a');a.href='../../../'+m.source;a.textContent=m.source;p.append(a);d.append(strong,p)};document.querySelector('#map').append(b)}document.querySelector('#map button').click();function files(){const q=document.querySelector('#search').value.toLowerCase(),root=document.querySelector('#files');root.replaceChildren();for(const f of inventory.filter(f=>f.path.toLowerCase().includes(q))){const tr=document.createElement('tr');const td=document.createElement('td');const a=document.createElement('a');a.href='../../../'+f.path;a.textContent=f.path;td.append(a);const bytes=document.createElement('td');bytes.textContent=f.bytes;tr.append(td,bytes);root.append(tr)}}document.querySelector('#search').oninput=files;files();</script></html>'''
for key,value in {'CARDS':cards,'FIGURES':figs,'SYSTEM':json.dumps(system).replace('</','<\\/'),'INVENTORY':json.dumps(inventory).replace('</','<\\/'),'RESULTS':json.dumps(a).replace('</','<\\/')}.items():guide=guide.replace('__'+key+'__',value)
write(d/'index.html',guide);print(json.dumps({'java':java,'desktop':desktop['tests'],'archiveEntries':len(archive),'canonicalSources':len(inventory),'screenshots':len(captures)}))
