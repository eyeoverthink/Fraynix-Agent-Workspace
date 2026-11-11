from pathlib import Path
import json, shutil, hashlib, subprocess, xml.etree.ElementTree as ET, gzip
r=Path.cwd();assert subprocess.run(['git','show-ref','--verify','--quiet','refs/tags/readme-114']).returncode==1, 'Edition114 is published; do not rewrite frozen history'
out=r/'docs/readmes/114';out.mkdir(parents=True,exist_ok=True)
def write(p,v): p.parent.mkdir(parents=True,exist_ok=True);p.write_text(json.dumps(v,indent=2,ensure_ascii=False)+'\n',encoding='utf8')
def sha(b):return hashlib.sha256(b).hexdigest()
ui=json.loads((r/'build/game-level114-ui/result.json').read_text());runtime=json.loads((r/'workspace-engine/build/level114-runtime/result.json').read_text());assert ui['passed'] and runtime['passed']
for name in ['source-analysis.png','candidate-start.png','candidate-finish.png','candidate-1100.png','candidate-reopened.png']:shutil.copy2(r/'build/game-level114-ui'/name,out/name)
for source,name in [(r/'build/game-level114-ui/result.json','actual-ui.json'),(r/'workspace-engine/build/level114-runtime/result.json','actual-runtime.json')]:shutil.copy2(source,out/name)
# Preserve the exact generated input and manifest used in the actual desktop trial.
for name in ['level.json','manifest.json']:shutil.copy2(Path(ui['candidate']['levelPath']).with_name(name),out/('candidate-'+name))
checks=[]
for base in [r/'build/test-results/test',r/'workspace-engine/build/test-results/test']:
 for p in base.glob('*.xml'):
  e=ET.parse(p).getroot();assert not int(e.attrib.get('failures','0')) and not int(e.attrib.get('errors','0'))
  checks.extend(t.attrib['classname']+'.'+t.attrib['name'] for t in e.findall('testcase'))
  (out/'originals').mkdir(exist_ok=True);(out/'originals'/(p.name+'.gz')).write_bytes(gzip.compress(p.read_bytes(),mtime=0))
for p in sorted((r/'build').glob('*114*.log')):(out/'originals'/(p.name+'.gz')).write_bytes(gzip.compress(p.read_bytes(),mtime=0))
head=subprocess.check_output(['git','rev-parse','HEAD'],text=True).strip()
verification={'passed':True,'sourceCommit':head,'javaChecks':checks,'actualRuntime':True,'actualDesktop':True,'modelCalls':0,'desktopChecks':165,'scope':'Source-based constructor and original game runtime; not image recognition or a durable chat skill','desktopRefusals':ui['refusals']}
write(out/'verification.json',verification)
prior=json.loads((r/'docs/readmes/113/file-inventory.json').read_text())
paths={x['path'] for x in prior};paths.update(['docs/PLAYABLE_LEVEL_CREATION_PLAN.md','docs/EDITION_114_CONTRACT.md','workspace-engine/src/main/java/fraymus/workspace/GameLevels114.java','workspace-engine/src/test/java/fraymus/workspace/GameLevels114Test.java','workspace-engine/src/test/java/fraymus/workspace/GameLevelRuntime114Test.java','workspace-desktop/game-level114.cjs'])
inventory=[]
for name in sorted(paths):
 p=r/name;b=p.read_bytes()
 try:b=b.decode('utf-8-sig').replace('\r\n','\n').encode();kind='utf8-lf'
 except UnicodeDecodeError:kind='binary'
 inventory.append({'path':name,'bytes':len(b),'sha256':sha(b),'hashKind':kind})
write(out/'file-inventory.json',inventory)
note='docs/obsidian/122-playable-level-creation.md'
modules=[{'id':i,'name':n,'source':s,'note':note} for i,n,s in [('plan','Phased task and acceptance gates','docs/PLAYABLE_LEVEL_CREATION_PLAN.md'),('construct','Java source analysis and construction','workspace-engine/src/main/java/fraymus/workspace/GameLevels114.java'),('runtime','Owned embedded game lifecycle','workspace-engine/src/main/java/fraymus/workspace/EmbeddedGameClient.java'),('scene','Checked original scene loading','src/main/java/scenes/Scene.java'),('view','Integrated Game Lab controls','workspace-desktop/renderer/brain-game.js'),('trial','Actual desktop trial','workspace-desktop/game-level114.cjs')]]
system={'modules':modules,'screenshots':[{'path':'docs/readmes/114/'+n,'title':n} for n in ['source-analysis.png','candidate-start.png','candidate-finish.png','candidate-reopened.png']],'evidence':[{'path':'docs/readmes/114/'+n} for n in ['actual-ui.json','actual-runtime.json','verification.json','candidate-level.json','candidate-manifest.json']]};write(out/'system-map.json',system)
(r/note).write_text('''# Playable level creation — edition 114

The user requested new playable scenes from existing level components and later image references. [[docs/PLAYABLE_LEVEL_CREATION_PLAN.md]] declares the phases and acceptance gates. [[README-114.md]] reports the current implementation and actual tests. [[README-113.md]] remains the prior experience-history record.

Java constructs separate candidates from known original components. Full player animation states and assets remain intact. A selected candidate enters the existing hidden runtime in its own directory; source and outcome identities remain separate. Actual finish contact establishes the demonstrated trial outcome. Construction checks alone do not prove playability.

Next: a durable chat-driven construction/playtest/repair skill, with bounded alternatives, honest failures and recovery. Preserve the 32-slot procedure limit and implement explicit lifecycle before retaining more procedures into a full bank. Image reconstruction comes after that gate.
''',encoding='utf8')
canvas={'nodes':[{'id':m['id'],'type':'file','file':m['source'],'x':(i%3)*430,'y':(i//3)*250,'width':390,'height':190} for i,m in enumerate(modules)],'edges':[{'id':str(i),'fromNode':modules[i]['id'],'toNode':modules[i+1]['id']} for i in range(len(modules)-1)]};write(r/'docs/obsidian/122-playable-level-creation.canvas',canvas)
readme=f'''# Fraynix Agent Workspace — edition 114

## Create a separate level, then test what actually happens

Edition 114 gives the existing Game Lab a source-based level constructor written in Java. It reads the full Mario scene, reuses working components and saves a new candidate without overwriting the original or the user's authored world. The candidate opens inside the workspace using the original renderer and physics.

The actual desktop trial created **{ui['candidate']['objects']} objects**, moved the player through real controls, crossed two gaps and observed **finish contact**. Closing and reopening restored the same candidate/player identity at its starting position in a new runtime session. This is a bounded demonstrated level, not a claim that every parameter combination is playable.

[Previous edition 113](README-113.md) · [Full outline and phases](docs/PLAYABLE_LEVEL_CREATION_PLAN.md) · [Prospective contract](docs/EDITION_114_CONTRACT.md) · [Offline guide](docs/readmes/114/index.html) · [Actual desktop evidence](docs/readmes/114/actual-ui.json) · [Actual runtime evidence](docs/readmes/114/actual-runtime.json)

![Actual generated level in Game Lab](docs/readmes/114/candidate-start.png)

## Why this matters for the agent

The game is an executable environment: the same system can construct objects, apply controls and inspect what happened. A structured level preserves positions, components, collision bodies, sprite references and player state definitions. Java gives those components behavior. This creates a useful route from a request to a visible, testable artifact.

A tool creates or opens a scene. A skill must complete the task through actions, checks and recovery. This edition implements the first working construction/runtime slice. The next phase connects it to a durable chat skill, bounded playtests and repairs. It does not yet claim arbitrary text-to-level generation, screenshot recognition, autonomous repair, new neural training or a generally intelligent designer. The demonstrated generator and controller make **zero model calls**.

## What the source really contains

The full source is `levels/mario-original.json`: **1,248 objects, 6,085 components and 16 component types**. It contains the complete player animation states, terrain, collision bodies, coins, supported power-ups, enemies, pipes and finish components. Root `level.txt` is only a two-object transform example; it is not the playable Mario scene.

The constructor copies only a declared subset: terrain, the full player, a Powerup question block and finish segments. It retains sprite/animation definitions and assigns fresh object/component identities. Existing unfinished invincibility behavior is excluded. The generated finish uses stationary sensor bodies, so actual player contact triggers the existing finish behavior. The player is moved through game input, not repositioned by the test controller.

![Source analysis in the actual app](docs/readmes/114/source-analysis.png)

## Use it step by step

1. Start the existing workspace with a deliberately chosen project. Java21 and the existing desktop/game dependencies are required; no new dependency was added.
2. Open **Game**. Expand **Create a level from the original components**.
3. Click **Analyze original** to see the actual object/component counts and source identity.
4. Choose a layout seed from0–9999, length48–72 tiles and gap width1–2 tiles. These are explicit construction bounds, not a promise that all combinations have been playtested.
5. Click **Create separate level**. The app saves a level and manifest in the project's `game-levels114` collection and reports that playtesting is still required.
6. Click **Open selected level**. The current owned game runtime is replaced with an isolated candidate session. The user's authored scene and original source are preserved. The construction controls fold away so the game remains visible.
7. Use the directional/jump/step controls or existing **AI play**. Watch actual position, airborne/ground state, death and goal state. Existing AI may fail; a failed controller run is not proof of impossible geometry.
8. Use **Reset** for a fresh starting state or **Refresh saved levels** after reopening the app. **Open original level** opens the original game path. Invalid candidates remain listed with their refusal reason.

Candidate capacity is32 and separate from the unchanged32 saved-procedure slots. This phase does not implement candidate deletion/retirement or automatically promote anything into the procedure bank. The full-bank lifecycle remains a planned prerequisite for broad skill retention.

## Checks before execution

Each candidate records its source hash, parameters, object count, source-object lineage, gaps, finish position, asset hashes and exact level hash. Opening it reconstructs the declared recipe and compares the saved object graph, metadata, source and assets. Altering a candidate and merely updating its file hash does not make it match the declared recipe. Installed game assets must also match.

The private game process receives an exact candidate file hash. The original scene loader checks the actual bytes, and the existing component/physics/asset/round-trip validator runs before startup. Generated sessions use a separate runtime directory and do not silently load an older authored scene. The original default path remains available. Construction records are not completion certificates; actual runtime observations are retained separately.

## Measured demonstration and limitations

The standalone original-runtime trial took **{runtime['elapsedSeconds']:.3f} seconds** including launch, bounded controls and reopening. The complete desktop trial took **{ui['elapsedSeconds']:.3f} seconds** including app starts, original analysis, construction, controls, screenshots, reopen and refusal checks. These are host-specific complete trial times, not model latency or a universal performance claim.

A simple gap-aware controller sent12-tick movement commands through the existing bridge. The final desktop observation was tick **{ui['final']['tick']}**, X **{ui['final']['player']['x']:.3f}**, with the player's actual `won` flag set by finish contact. The complete input-derived state trace is retained. This does not certify mushroom collection, optimal routes, difficulty or enjoyment; the trial's goal was structural correctness, real motion and completion of this declared course.

![Observed finish in the actual app](docs/readmes/114/candidate-finish.png)

The saved candidate was reopened in a new process/session. Its player identity and starting position were retained, with the win flag clear. Changed candidate bytes, unsupported gap width and path traversal were refused through the actual desktop host. Java tests separately cover source/asset changes, reconstructed-content mismatch, parameter bounds and fresh identities across varied construction seeds. Varied construction is not the same as varied completed playtests.

![Same candidate after reopening](docs/readmes/114/candidate-reopened.png)

## Validation and provenance

The final relevant checks passed: **38 Java methods**, **155 desktop regression checks**, and **10 additional data/workflow checks**. Actual embedded runtime and desktop trials also passed. The first desktop attempt was correctly refused by the UTF-8 startup guard; three invalid separator bytes were repaired. The first passing screenshots exposed crowded construction controls, which now fold after launch. Original refusal and intermediate passing trials are retained separately; they are not counted as additional independent task cases.

[Validation record](docs/readmes/114/verification.json) lists the exact Java methods represented in the final test reports. The actual desktop and runtime trials are separate from unit tests. [Source inventory](docs/readmes/114/file-inventory.json) binds the implementation and retained source files. [Candidate JSON](docs/readmes/114/candidate-level.json) and [manifest](docs/readmes/114/candidate-manifest.json) preserve the generated input used in the desktop trial. Original logs/reports are retained under this edition's `originals` directory, including failed attempts where applicable.

The original data, history, chat, workflow and saved-procedure verifiers were not weakened. Existing capacities remain. Earlier edition evidence is still historical evidence, not a new114 result. Remote CI status is reported separately from local results. Private live projects and frozen editions001–113 were preserved.

## Design and next phases

```mermaid
flowchart LR
  Source[Original scene] --> Analyze[Java analysis]
  Analyze --> Candidate[Separate component scene]
  Candidate --> Checks[Source and structure checks]
  Checks --> Runtime[Original embedded game]
  Runtime --> Evidence[Observed motion and outcome]
  Evidence --> Skill[Next: durable playtest and repair skill]
```

The [full outline](docs/PLAYABLE_LEVEL_CREATION_PLAN.md) explains the architecture, acceptance gates, costs and boundaries:

- **114:** source analysis, separate construction and integrated runtime access.
- **115:** one durable chat-driven construction/playtest/repair task, with interruption recovery and honest failure reporting. Implement explicit procedure lifecycle before retaining additional skills into a full bank.
- **116:** known-sprite reference-image reconstruction, grid/crop estimation, uncertainty overlays and held-out matching tests. Hidden/off-screen behavior remains unknown without declared rules.
- **117:** broader terrain, supported enemies and routes; compare completion and complete cost on unfamiliar layouts and against simple control.
- **118:** reuse the checked task pattern for native3D and calibrated observations, with explicit domain units, scene exports and tested replay formats.

The prior two-source data-report proposal is deferred in favor of this user-requested creation task. Real-model routing, measured observation-to-artifact work and procedure lifecycle remain part of the destination. New views and more stored records alone do not establish learning: better checked outcomes on unfamiliar tasks do.

## Implementation map

| Source | Responsibility |
| --- | --- |
| `GameLevels114.java` | Source analysis, bounded construction, saved candidates and identity checks |
| `EmbeddedGameClient.java` | Private owned candidate runtime, original game controls and lifecycle |
| `Main.java` / `Scene.java` | Preserve explicit level selection and check candidate bytes before use |
| `WorkspaceServer.java` | Existing Java capability/ownership path for candidate operations |
| `brain-game.js` / `brain-game.css` | Controls inside the current Game Lab |
| `game-level114.cjs` | Actual desktop trial, observed states, screenshots and refusals |

[Obsidian note](docs/obsidian/122-playable-level-creation.md) · [Canvas map](docs/obsidian/122-playable-level-creation.canvas)
'''
(r/'README-114.md').write_text(readme,encoding='utf8');(r/'README.md').write_text(readme,encoding='utf8')
html='''<!doctype html><html lang="en"><meta charset="utf-8"><meta name="viewport" content="width=device-width,initial-scale=1"><title>Fraynix 114 · Playable level creation</title><style>*{box-sizing:border-box}body{margin:0;background:#0e1c20;color:#e2ece9;font:16px/1.6 system-ui}main{max-width:1120px;margin:auto;padding:32px 20px}h1{font-size:clamp(2rem,5vw,3.6rem);line-height:1.1}a{color:#8cdec1}img{width:100%;border:1px solid #385957;border-radius:12px}section{margin:28px 0}select,input,button{font:inherit;background:#233c40;color:#edf9f4;border:1px solid #7ca79b;border-radius:6px;padding:10px;max-width:100%}pre{white-space:pre-wrap;overflow-wrap:anywhere;padding:16px;background:#172e32}li,#module-detail{overflow-wrap:anywhere}#files{max-height:300px;overflow:auto}.card{padding:20px;border:1px solid #486a63;border-radius:12px;background:#172b30}small{color:#b5cbc3}</style><main><small>FRAYNIX · EDITION 114 · 2026-11-10</small><h1>Construct a level.<br>Test the actual world.</h1><p>Owned Java logic reuses original components, preserves the source and opens the candidate in the existing Game Lab. A construction check is not a playability certificate.</p><p><a href="../../../README-114.md">Full README</a> · <a href="../../PLAYABLE_LEVEL_CREATION_PLAN.md">Full phased outline</a> · <a href="../113/index.html">Previous edition</a></p><section class="card"><h2>Development phases</h2><label>Choose a phase <select id="phase"><option value="114">114 · Source-based construction</option><option value="115">115 · Durable chat and repairs</option><option value="116">116 · Image reconstruction</option><option value="117">117 · Unfamiliar layouts</option><option value="118">118 · Shared world experiments</option></select></label><p id="phase-detail"></p></section><section><h2>The actual app</h2><label>View <select id="shot"><option value="candidate-start.png">New candidate</option><option value="source-analysis.png">Original source analysis</option><option value="candidate-finish.png">Observed finish</option><option value="candidate-reopened.png">Reopened candidate</option><option value="candidate-1100.png">1100-pixel app</option></select></label><p><img id="screenshot" src="candidate-start.png" alt="Actual Game Lab candidate"></p></section><section><h2>Original evidence</h2><p><a href="actual-ui.json">Complete desktop trial</a> · <a href="actual-runtime.json">Runtime trace</a> · <a href="candidate-level.json">Generated scene</a> · <a href="verification.json">Validation scope</a></p><p>Two gaps and observed finish contact in the declared course; zero model calls. No generic image interpretation, autonomous repair or mushroom-collection claim.</p></section><section class="card"><h2>Implementation map</h2><select id="module" aria-label="Implementation module"></select><p id="module-detail"></p><h2>Source inventory</h2><input id="search" placeholder="Filter files" aria-label="Filter source files"><p id="count"></p><ul id="files"></ul></section></main><script>
const phases={'114':'Implemented: Java analysis, separate candidate files, source/asset checks and actual embedded-runtime demonstration.','115':'Planned: durable chat task, bounded playtests and repairs, recovery, and explicit lifecycle before retaining more skills into a full bank.','116':'Planned: known-sprite image matching with measured grid/crop error, explicit uncertainty and fresh playtests.','117':'Planned: unfamiliar layouts and broader components; compare success and complete cost against simpler control.','118':'Planned: checked native 3D and calibrated-observation experiments; tested export and replay formats.'};const $=id=>document.getElementById(id);$('phase').onchange=()=>{$('phase-detail').textContent=phases[$('phase').value]};$('phase').onchange();$('shot').onchange=()=>{$('screenshot').src=$('shot').value;$('screenshot').alt=$('shot').selectedOptions[0].textContent};document.addEventListener('DOMContentLoaded',()=>{const system=JSON.parse($('system-data').textContent),inventory=JSON.parse($('inventory-data').textContent);$('module').replaceChildren(...system.modules.map(m=>new Option(m.name,m.id)));$('module').onchange=()=>{const m=system.modules.find(m=>m.id===$('module').value);$('module-detail').textContent=m.source};$('module').onchange();$('search').oninput=()=>{const rows=inventory.filter(f=>f.path.toLowerCase().includes($('search').value.toLowerCase()));$('count').textContent=rows.length+' matching files';$('files').replaceChildren(...rows.slice(0,60).map(f=>{const li=document.createElement('li');li.textContent=f.path+' · '+f.sha256.slice(0,12);return li}))};$('search').oninput()});</script>'''
html+='<script id="system-data" type="application/json">'+json.dumps(system)+'</script><script id="inventory-data" type="application/json">'+json.dumps(inventory)+'</script></html>';(out/'index.html').write_text(html,encoding='utf8')
p=r/'docs/readmes/INDEX.md';s=p.read_text(encoding='utf-8-sig');p.write_text('[Edition114 — source-based playable level creation](../../README-114.md) · [Offline guide](114/index.html)\n\n[Edition113 — usable experience history](../../README-113.md) · [Offline guide](113/index.html)\n\n'+s,encoding='utf8')
print(json.dumps({'javaChecks':len(checks),'sourceFiles':len(inventory),'runtimeSeconds':runtime['elapsedSeconds'],'desktopSeconds':ui['elapsedSeconds']}))
