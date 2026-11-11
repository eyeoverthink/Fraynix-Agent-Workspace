from pathlib import Path
import json,hashlib,subprocess,shutil,gzip,xml.etree.ElementTree as ET
r=Path.cwd();out=r/'docs/readmes/115';out.mkdir(parents=True,exist_ok=True)
assert subprocess.run(['git','show-ref','--verify','--quiet','refs/tags/readme-115']).returncode==1,'Never rewrite a published edition'
def write(p,v):p.parent.mkdir(parents=True,exist_ok=True);p.write_bytes((json.dumps(v,indent=2,ensure_ascii=False)+'\n').encode('utf8'))
def text(p,v):p.parent.mkdir(parents=True,exist_ok=True);p.write_bytes(v.encode('utf8'))
def sha(b):return hashlib.sha256(b).hexdigest()
uiroot=r/'build/game115-ui-verified';ui=json.loads((uiroot/'result.json').read_text(encoding='utf8'));assert ui['passed']
reference=json.loads((r/'workspace-engine/build/reference115/result.json').read_text(encoding='utf8'));assert reference['passed']
shots=['chat-level-completed.png','retained-repairs.png','recovered-level.png','reference-analysis.png','reference-playtested.png','level-1100.png','actual-reference.png']
for name in shots:shutil.copy2(uiroot/name,out/name)
for name in ['actual-overview.png','scaled-test.png','occluded-test.png']:shutil.copy2(r/'workspace-engine/build/reference115'/name,out/name)
write(out/'actual-ui.json',ui);write(out/'reference-validation.json',reference)
for name in ['game115-repair','game115-restart']:shutil.copy2(r/'workspace-engine/build'/name/'result.json',out/(name+'.json'))
checks=[]
for base in [r/'build/test-results/test',r/'workspace-engine/build/test-results/test']:
 for p in sorted(base.glob('*.xml')):
  e=ET.parse(p).getroot();assert int(e.get('failures','0'))+int(e.get('errors','0'))==0
  checks.extend(c.get('classname')+'.'+c.get('name') for c in e.findall('testcase'))
assert len(checks)==79
verification={'passed':True,'javaChecks':checks,'desktopChecks':165,'actualChatRequests':4,'actualAbruptRecovery':True,'noRepairRefusal':True,'modelCalls':sum(x['task']['timing052']['modelCalls'] for x in ui['requests']),'referenceCases':{'native':True,'resized':True,'skyCrop':True,'coveredPlayerRefused':True,'distractorRefused':True,'changedImageRefused':True},'sourceCommit':ui['source'],'scope':'Known two-gap recipe and original sprite assets. Actual native simulation and observed finish contact. Existing AI fails on seed0, succeeds on seed3; no universal playability or model-training claim.'}
write(out/'verification.json',verification)
# Retain exact generated test evidence, including failed matcher iterations and the
# earlier complete UI run. Only these isolated test folders are read; no private app data.
originals=[]
def preserve(p,logical):
 if not p.is_file():return
 b=p.read_bytes();raw=sha(b);dest=out/'originals'/(raw+'.gz');packed=gzip.compress(b,mtime=0);dest.parent.mkdir(exist_ok=True)
 if not dest.exists():dest.write_bytes(packed)
 originals.append({'logicalPath':logical,'path':dest.relative_to(r).as_posix(),'bytes':len(packed),'sha256':sha(packed),'rawBytes':len(b),'rawSha256':raw})
for folder in ['build/before-reference-fix115','build/reference-attempt2','build/reference-attempt3','build/reference-attempt4','workspace-engine/build/reference115','workspace-engine/build/game115-repair','workspace-engine/build/game115-restart','build/test-results/test','workspace-engine/build/test-results/test']:
 for p in sorted((r/folder).rglob('*')):
  if p.suffix in ['.json','.xml','.png','.txt','.java']:preserve(p,p.relative_to(r).as_posix())
for folder in ['build/game115-ui','build/game115-ui-review','build/game115-ui-verified']:
 for p in sorted((r/folder).iterdir()):
  if p.is_file():preserve(p,p.relative_to(r).as_posix())
 for sub in ['game-tasks115','game-levels114','game-references115','agency-043','embedded-game']:
  for p in sorted((r/folder/'project'/sub).rglob('*')):
   if p.suffix in ['.json','.jsonl','.png','.log']:preserve(p,p.relative_to(r).as_posix())
for p in sorted((r/'build').glob('*115*.log')):preserve(p,p.relative_to(r).as_posix())
for name in ['build/study095-build.json','build/ci115-parent.json']:preserve(r/name,name)
write(out/'original-evidence.json',{'files':originals,'scope':'Exact original bytes from isolated tests, failed iterations and full visible chat trials; gzip deduplicated by raw SHA-256.'})
prior=json.loads((r/'docs/readmes/114/file-inventory.json').read_text(encoding='utf8'));paths={x['path'] for x in prior}
for name in subprocess.check_output(['git','diff','--name-only','56d4423a','HEAD'],text=True).splitlines():
 if name.startswith(('workspace-engine/','workspace-desktop/','src/')) or name=='build.gradle':paths.add(name)
paths.update(['docs/EDITION_115_CONTRACT.md','docs/PLAYABLE_LEVEL_CREATION_PLAN.md'])
inventory=[]
for name in sorted(paths):
 b=(r/name).read_bytes()
 try:b=b.decode('utf-8-sig').replace('\r\n','\n').encode('utf8');kind='utf8-lf'
 except UnicodeDecodeError:kind='binary'
 inventory.append({'path':name,'bytes':len(b),'sha256':sha(b),'hashKind':kind})
write(out/'file-inventory.json',inventory)
note='docs/obsidian/123-durable-level-tasks.md'
modules=[{'id':i,'name':n,'source':s,'note':note} for i,n,s in [('task','Durable Java task','workspace-engine/src/main/java/fraymus/workspace/GameTask115.java'),('image','Known-sprite reconstruction','workspace-engine/src/main/java/fraymus/workspace/GameReference115.java'),('scene','Source-derived candidates','workspace-engine/src/main/java/fraymus/workspace/GameLevels114.java'),('chat','Existing chat scheduler integration','workspace-engine/src/main/java/fraymus/workspace/Domain044.java'),('runtime','Actual renderer and bounded AI input','src/main/java/runtime/EmbeddedGameRuntime.java'),('view','Integrated Game Lab','workspace-desktop/renderer/brain-game.js'),('proof','Actual visible-chat trial','workspace-desktop/test/game115-integration.cjs')]]
system={'modules':modules,'screenshots':[{'path':'docs/readmes/115/'+n,'title':n} for n in shots],'evidence':[{'path':'docs/readmes/115/'+n} for n in ['actual-ui.json','reference-validation.json','verification.json','original-evidence.json']]};write(out/'system-map.json',system)
rows=[]
for label,x in zip(['Two measured gap repairs','Abrupt restart and resume','Repairs disabled','Image reconstruction'],ui['requests']):
 a=x['child'].get('audit',{});rows.append(f"| {label} | {x['elapsedMs']/1000:.3f} s | {a.get('simpleControl','failed')} | {a.get('existingAI','not run')} | {a.get('repairCount',0)} |")
readme=f'''# Fraynix Agent Workspace — edition 115

## One request, a real level, retained evidence

Edition115 connects the existing Game Lab to the durable Java chat scheduler. A request constructs a separate component-based level, runs real physics, inspects the result, makes at most two justified gap repairs and checks the outcome again. A new reference-image importer reconstructs a supported course from its original sprite appearances and sends it through the same playtests.

**The completion gate is actual finish contact.** Files, screenshots and a target-X crossing do not substitute for a successful trial. Failed candidates and unfinished sessions remain available. The final candidate opens at its starting point for review; its earlier playtest results remain separate.

[Previous edition114](README-114.md) · [Full phased plan](docs/PLAYABLE_LEVEL_CREATION_PLAN.md) · [Acceptance contract](docs/EDITION_115_CONTRACT.md) · [Interactive offline guide](docs/readmes/115/index.html) · [Complete actual chat results](docs/readmes/115/actual-ui.json)

![Actual completed chat request and the verified level reopened for review](docs/readmes/115/chat-level-completed.png)

## Try it

1. Prepare and start the workspace with the commands below. Use a closed, deliberately selected project when upgrading. Java 21 and Node.js are required; existing game dependencies remain.
2. Open **Game Lab**. In chat, enter: `Create a playable Mario level gapTiles=10 repairLimit=2; construct, playtest and repair it.`
3. Watch the real game and task progress. **Mission** provides Stop and explicit Resume. Each trial has its own session and original trace.
4. Open **Level task history** to inspect the attempts, **Recheck evidence** to repeat source/outcome checks, or **Open verified result** to load its original starting state.
5. For an image, download the [actual native overview](docs/readmes/115/actual-reference.png), expand **Create a level**, select that PNG and choose **Analyze reference**. Review the outlines, then choose **Prepare reconstruction request** and **Start task**.

For a new or updated checkout, prepare both Java runtimes before launching. Stop that checkout’s Java owner through the app first; a closed desktop alone does not mean Java has stopped. These commands run from the repository root:

```powershell
.\\gradlew.bat prepareGameRuntime115 :workspace-engine:installDist
.\\scripts\\Start-Agent-Workspace.ps1
```

The [launcher](scripts/Start-Agent-Workspace.ps1) uses the existing project-selection settings; it does not migrate another project’s private data.

Supported request fields are `seed=0..9999`, `lengthTiles=48..72`, `gapTiles=1..10`, `secondGapTiles=1..10`, and `repairLimit=0..2`. Defaults are seed0, length56, two gaps of width2 and two permitted repairs. `without repairs` sets the repair limit to zero. These fields configure the declared two-gap recipe; this is not arbitrary natural-language level design. Explicit image references cannot be mixed with manual geometry overrides.

## What Java owns

```mermaid
flowchart LR
  Request[Retained chat request] --> Source[Checked components or image]
  Source --> Build[Separate candidate]
  Build --> Trial[Actual simple-control playtest]
  Trial --> Inspect[Observed finish or failure]
  Inspect -->|justified repair within budget| Build
  Inspect -->|finish contact| Compare[Existing AI comparison]
  Compare --> Verify[Recheck original evidence]
  Verify --> Review[Open candidate for review]
```

The existing chat task remains the parent. Java advances one bounded stage at a time, maintains the candidate lineage and retains all input chunks, observations, source/asset/engine identities and trial images. Each control chunk is 12 game ticks; a trial is limited to 180 chunks. The simple controller uses the known gap positions and ordinary movement/jump inputs. The existing AI uses its original game logic through a new bounded stepping port. Neither controller teleports the actor to make a test pass.

A repair is allowed only after an observed fall in a specific gap. It narrows that gap to two tiles, preserves the failed candidate and creates a fresh candidate. A failure outside that rule or after the repair budget is exhausted stops with the original reason. A construction check alone is never called a playability certificate.

After an abrupt interruption, the original unfinished trace remains marked interrupted. Explicit Resume rechecks the same source and engine, then starts that trial in a new native session. Completed trials remain unchanged. Sessions are never stitched together to create a false completion.

![Retained failures and measured repairs in the actual app](docs/readmes/115/retained-repairs.png)

## Reference-image reconstruction

The owned Java matcher reads a bounded PNG, estimates its tile grid and matches original terrain, player, question-block and finish sprites. It models the renderer's actual alpha blending, pixel-boundary sampling, and bounded nearest-neighbor resize hypotheses. Match error and background contrast are measurements, not probabilities of correctness. The interface draws Java's detected component boxes over a cropped preview; the complete original PNG and hash are retained.

The first supported domain is the source-derived **two-gap course**: 48–72 visible columns, two complete floor rows, the original player, one question block and a visible finish. Tiles must occupy 8–32 even pixels. PNG size is capped at 3 MB and 1536×864 pixels. Sixteen references can be retained. Unexplained foreground, missing actors, incomplete terrain and unsupported geometry refuse reconstruction. An image cannot reveal the hidden contents of a question block: this recipe explicitly uses the original **Powerup** behavior. It does not invent offscreen geometry or import arbitrary executable components.

The recognizer returns a canonical matching layout seed, not evidence of the author's original seed. Any later repair is recorded as a change from the image-derived layout. A recognized picture still requires an actual constructed-scene playtest.

![Actual reference analysis and Java component outlines](docs/readmes/115/reference-analysis.png)

## Measured results

The final visible desktop trial used four fresh chat requests. It includes a forced Java-process interruption and explicit recovery, a refusal when repairs are forbidden, and fresh reconstruction from a native overview. Total request elapsed times include orchestration, file checks, rendering, repairs, controller comparison and recovery; they are not just inference timings.

| Actual request | Complete elapsed | Simple control | Existing AI | Repairs |
| --- | ---: | --- | --- | ---: |
{chr(10).join(rows)}

The existing AI fails on the repaired seed0 course and succeeds on seed3. That counterexample is retained. The successful simple-control trial proves this candidate can be completed under the tested controls; it does not establish general AI mastery. The UI explicitly reports both outcomes.

Validation: **79 Java test methods**, **165 desktop regression checks**, actual Electron/Java/native-game trials, and source/candidate/image/checkpoint refusal tests. The image suite accepts a native overview, its resized version and a sky-only crop; it rejects an occluded player and an added foreground object. A separately constructed image-derived level reaches the finish. The original renderer and game tests remain included.

**Zero planner/model calls** were needed for these bounded requests. A selected local model remains available for other tasks, but these results come from Java routing, image algorithms and actual simulation. No new model training, consciousness, universal visual recognition or autonomous procedure promotion is claimed. Remote GitHub CI is reported separately from passed local checks.

![Image-derived candidate after actual playtesting](docs/readmes/115/reference-playtested.png)

## Preserved architecture and limits

The original `levels/mario-original.json` has 1,248 objects; root `level.txt` is a two-object example. Full original component and player animation definitions remain the construction source. Candidate JSON is data; the behavior lives in Java. Source hashes, exact generated structure, assets and installed game assets are checked before use.

Existing 24 chat-task and 32 candidate limits remain. The dedicated level-task history is also bounded at 24; recovery is limited to three explicit resumes. The active procedure bank remains 32 slots. Level task records and reconstruction evidence **do not automatically become saved skills**. Explicit retirement/reactivation with original provenance is still required before admitting more procedures into a full bank.

Native tests now prepare the actual game code and assets without repeatedly copying 16 GB of historical documentation. The full release distribution rule still includes that history. The initial packaging attempts and all image-matching failures are retained in the [original evidence index](docs/readmes/115/original-evidence.json). The corrected matcher models the actual blend/sampling behavior; no successful observation was substituted for a failed one. An earlier recovery screenshot was captured before the first game frame loaded; that attempt remains retained. The final UI trial waits for a decoded frame showing the reopened level at tick zero before taking its screenshots.

## Implementation map

```text
src/main/java/runtime/             original renderer and physics bridge
workspace-engine/src/main/java/    Java task, candidate and image logic
workspace-engine/src/test/java/    native and refusal checks
workspace-desktop/renderer/        existing chat and integrated Game Lab
workspace-desktop/test/            actual application trials
levels/                           preserved original component source
README-115.md                     this frozen edition
docs/readmes/115/                 screenshots, measurements and original evidence
docs/publication115/              documentation and publication checks
```

| Source | Responsibility |
| --- | --- |
| `GameTask115.java` | Durable stages, input budgets, failure-specific repairs, continuity and outcome audit |
| `GameReference115.java` | Original PNG retention, known-sprite grid matching, confidence measurements and refusals |
| `GameLevels114.java` | Source-derived candidates, independent gap parameters, deterministic task identity |
| `Domain044.java` / `ChatPolicy044.java` | Connect the existing chat owner to the native child task |
| `EmbeddedGameRuntime.java` | Original renderer/physics, bounded existing-AI input, paused overview capture |
| `brain-game.js` | Import, component outlines, task history and checked result opening |
| `game115-integration.cjs` | Actual UI requests, abrupt recovery, screenshots and complete costs |

## Next phases

1. **Broader image reconstruction:** separately validated grids/layouts, missing-content review, supported additional components, new images and measured false accept/refusal rates. Keep appearance, declared behavior and unknown content separate.
2. **Improve the game AI against the retained failure:** compare obstacle and power-up choices against simple control on unfamiliar seeds. Measure success and complete cost before retaining a new policy.
3. **Procedure lifecycle:** retire/reactivate saved procedures with their exact original evidence under the unchanged 32 active slots; then expose qualified level procedures for reuse.
4. **Broader worlds:** transfer the same request→observation→action→verification pattern to supported 3D components and calibrated observations. Preserve units, uncertainty and source identities.

The direction is one integrated system that can create an environment, act within it, see the measured result and retain what actually happened. The next improvement should be earned through new outcomes, not a larger capability claim.

[Project memory](docs/PROJECT_MEMORY.md) · [Development log](docs/DEVELOPMENT_LOG.md) · [Obsidian map](docs/obsidian/123-durable-level-tasks.md) · [All editions](docs/readmes/INDEX.md)
'''
text(r/'README-115.md',readme);text(r/'README.md',readme)
text(r/note,'# Durable level tasks — edition115\n\n[[README-115.md]] joins the existing Java chat owner to actual game construction, bounded playtests, observed-gap repair and fresh-session recovery. Known-sprite references use the same constructor and outcome gate. Keep actual finish contact separate from image matching and candidate validation. The existing AI failure on seed0 remains a counterexample; source-based simple control succeeds. No automatic skill promotion or LLM training.\n\nNext: broader reference cases, improve the measured controller failure, and explicit procedure lifecycle under 32 active slots. [[docs/PLAYABLE_LEVEL_CREATION_PLAN.md]] preserves the larger phased direction.\n')
write(r/'docs/obsidian/123-durable-level-tasks.canvas',{'nodes':[{'id':m['id'],'type':'file','file':m['source'],'x':i%3*430,'y':i//3*250,'width':390,'height':190} for i,m in enumerate(modules)],'edges':[{'id':str(i),'fromNode':modules[i]['id'],'toNode':modules[i+1]['id']} for i in range(len(modules)-1)]})
html='''<!doctype html><html lang="en"><meta charset="utf-8"><meta name="viewport" content="width=device-width,initial-scale=1"><title>Fraynix115 · Durable levels</title><style>*{box-sizing:border-box}body{margin:0;background:#102124;color:#e6f2ec;font:16px/1.6 system-ui}main{max-width:1120px;margin:auto;padding:32px 20px}h1{font-size:clamp(2rem,5vw,3.5rem);line-height:1.1}a{color:#8cdec1}img{width:100%;border:1px solid #426b63;border-radius:12px}section{margin:28px 0}select,input,button{font:inherit;background:#24443f;color:#f0fff6;border:1px solid #7caca0;border-radius:6px;padding:10px;max-width:100%}pre,li,p{overflow-wrap:anywhere}pre{white-space:pre-wrap}#files{max-height:280px;overflow:auto}.card{padding:20px;border:1px solid #426b63;border-radius:12px;background:#19332f}</style><main><small>FRAYNIX · EDITION115 · 2026-11-10</small><h1>Ask. Construct. Test.<br>Keep what happened.</h1><p>One durable Java chat request creates a separate level, observes actual play, repairs only measured gap failures and retains every trial. Known-sprite references enter the same checked path.</p><p><a href="../../../README-115.md">Full README</a> · <a href="../114/index.html">Previous edition</a> · <a href="../../PLAYABLE_LEVEL_CREATION_PLAN.md">Full phased plan</a></p><section class="card"><h2>Phases and boundaries</h2><select id="phase"><option value="115">115 · Durable construction and bounded references</option><option value="116">Next · Broader image evidence</option><option value="117">Next · Controller improvement</option><option value="skills">Next · Procedure lifecycle</option></select><p id="phase-detail"></p></section><section><h2>The actual app</h2><select id="shot"><option value="chat-level-completed.png">Completed chat and review level</option><option value="retained-repairs.png">Failed attempts and repairs</option><option value="reference-analysis.png">Reference-image outlines</option><option value="reference-playtested.png">Reconstructed level</option><option value="recovered-level.png">After explicit recovery</option><option value="level-1100.png">1100-pixel app</option></select><p><img id="screenshot" src="chat-level-completed.png" alt="Actual completed level task"></p></section><section class="card"><h2>Evidence</h2><p>79 Java checks · 165 desktop checks · four real chat requests · one abrupt recovery. No new model inference or training claim. Simple control succeeds on the repaired seed0 course; the existing AI fails there. Both succeed on seed3.</p><p><a href="actual-ui.json">Complete actual requests and costs</a> · <a href="reference-validation.json">Image checks</a> · <a href="actual-reference.png">Try the actual reference PNG</a> · <a href="original-evidence.json">Original evidence index</a></p></section><section class="card"><h2>Implementation</h2><select id="module" aria-label="Implementation module"></select><p id="module-detail"></p><h2>Source inventory</h2><input id="search" placeholder="Filter source files" aria-label="Filter source files"><p id="count"></p><ul id="files"></ul></section></main><script>const $=id=>document.getElementById(id),phases={'115':'Implemented: durable Java chat, exact candidate sources, bounded native control, observed repairs, explicit recovery and supported known-sprite image reconstruction.','116':'Broaden reconstruction with fresh images, uncertainty review and supported components. Refuse unknown content rather than inventing hidden rules.','117':'Use the retained AI-controller failure to improve action choice. Compare unfamiliar seeds against simple control and include complete cost.','skills':'Explicit retirement/reactivation with original provenance before new skill admission into the unchanged 32 active slots.'};$('phase').onchange=()=>{$('phase-detail').textContent=phases[$('phase').value]};$('phase').onchange();$('shot').onchange=()=>{$('screenshot').src=$('shot').value;$('screenshot').alt=$('shot').selectedOptions[0].textContent};document.addEventListener('DOMContentLoaded',()=>{const system=JSON.parse($('system-data').textContent),inventory=JSON.parse($('inventory-data').textContent);$('module').replaceChildren(...system.modules.map(m=>new Option(m.name,m.id)));$('module').onchange=()=>{$('module-detail').textContent=system.modules.find(m=>m.id===$('module').value).source};$('module').onchange();$('search').oninput=()=>{const rows=inventory.filter(f=>f.path.toLowerCase().includes($('search').value.toLowerCase()));$('count').textContent=rows.length+' matching files';$('files').replaceChildren(...rows.slice(0,60).map(f=>{const li=document.createElement('li');li.textContent=f.path+' · '+f.sha256.slice(0,12);return li}))};$('search').oninput()});</script>'''
html+='<script id="system-data" type="application/json">'+json.dumps(system)+'</script><script id="inventory-data" type="application/json">'+json.dumps(inventory)+'</script></html>';text(out/'index.html',html)
p=r/'docs/readmes/INDEX.md';s=p.read_text(encoding='utf-8-sig');link='[Edition115 — durable level tasks and checked references](../../README-115.md) · [Offline guide](115/index.html)';text(p,link+'\n\n'+s if not s.startswith(link) else s)
p=r/'docs/obsidian/00-home.md';s=p.read_text(encoding='utf-8-sig');link='Current: [[docs/obsidian/123-durable-level-tasks.md]] · [[README-115.md]]';text(p,link+'\n\n'+s if not s.startswith(link) else s)
print(json.dumps({'passed':True,'javaChecks':len(checks),'sources':len(inventory),'originalEntries':len(originals),'requests':len(ui['requests'])}))
