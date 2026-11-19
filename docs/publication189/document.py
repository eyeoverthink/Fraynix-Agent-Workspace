"""Freeze Edition 189 from the verified Blender, desktop and regression runs."""
from hashlib import sha256
from html import escape
import json
from pathlib import Path
import re
import shutil
import subprocess
import sys

snap = Path("docs/readmes/189")
snap.mkdir(exist_ok=True)
live = Path(sys.argv[1] if len(sys.argv) > 1 else "build/edition189-live")
baseline = "8873dff842ab510640f49f77a028fc0fae68917b"
source = subprocess.check_output(["git", "rev-parse", "HEAD"], text=True).strip()
app = json.loads((live / "result.json").read_bytes())
tests = json.loads(Path("build/edition189-regression/summary.json").read_bytes())
assert app["passed"] and tests["passed"]
exchange = app["exchange"]
assert exchange["restart"]
assert exchange["sourceIds"] == ["crate", "floor", "obstacle"]
assert exchange["rejections"] == ["wrong scale", "unsupported sphere collider"]
assert exchange["scale"]["passed"] and exchange["scale"]["convertedLengthM"] == 1
assert not exchange["scale"]["independentlyCalibrated"]
fixture = "authored-bench.blend"
assert sha256((live / fixture).read_bytes()).hexdigest() == exchange["sourceSha256"]
shutil.copyfile(live / fixture, snap / fixture)
physics = app["physics"]
assert physics["metrics"]["passed"] and physics["metrics"]["grabs"] == physics["metrics"]["releases"] == 1
assert physics["browserChecks"] == 8
game = app["game"]
assert game["levelId"] == "relay-garden-v1" and game["tick"] == 30 and game["quads"] == 124
assert game["units"] == "legacy-game-unit"
title = "Bring Blender scenes and Game Lab layouts into one workflow"
summary = ("Import a Blender rigid-box scene into Spatial, test it with native physics, and export an editable .blend. "
           "Export a paused Game Lab frame with named objects and its original Java snapshot.")
counts = (f"{tests['rootJava']} root Java tests, {tests['focusedJava']} focused Java workspace tests, "
          f"{tests['desktop']} desktop tests and {tests['archive']} evidence archive tests passed.")
def read(path):
    return Path(path).read_text(encoding="utf-8-sig")

def write(path,text):
    Path(path).write_text(text,encoding="utf-8",newline="\n")

def js(path,data):
    write(path,json.dumps(data,ensure_ascii=False,indent=2)+"\n")

def prepend(path,text):
    old=read(path)
    if not old.startswith(text):
        write(path,text+old)

def item(path):
    data=Path(path).read_bytes()
    try:
        data=data.decode("utf-8-sig").replace("\r\n","\n").encode()
        kind="utf8-lf"
    except UnicodeDecodeError:
        kind="binary"
    return {"path":Path(path).as_posix(),"bytes":len(data),"sha256":sha256(data).hexdigest(),"hashKind":kind}




labels={"blender-import.png":"Blender-authored bench and its one-metre reference in Spatial",
        "blender-spatial-verified.png":"Verified native manipulation of the reimported Blender scene",
        "blender-game-layout.png":"Paused Relay Garden frame saved as a Blender layout"}
shots=[]
for name in app["screenshots"]:
    original=live/name
    shutil.copyfile(original,snap/name)
    shots.append({"path":(snap/name).as_posix(),"title":labels[name]})
assert set(labels)=={Path(shot["path"]).name for shot in shots}
app.update({"source":source,"tests":tests})
js(snap/"verification.json",app)
write("README-189.md",f"""# Fraynix Agent Workspace · Edition 189

[Edition 188](README-188.md) · [Offline walkthrough](docs/readmes/189/index.html) · [Full edition history](docs/readmes/INDEX.md)

{summary}

![A real Blender-authored scene in Fraynix Spatial](docs/readmes/189/blender-import.png)

## Try the Blender bench

1. Download the [editable bench fixture](docs/readmes/189/authored-bench.blend).
2. Open **Spatial → Import Blender** and select the fixture. Fraynix uses your installed Blender. Set FRAYNIX_BLENDER to the full executable path when it is installed in a custom location.
3. Check the one-metre reference and select **Movable crate**. Use **Forecast only** or **Grab, move & release**. A verified run retains its native trace and browser-checked report and can save a reusable procedure.
4. Use **Export Blender** to save an editable copy of the authored scene to a new filename.

For your own scene, set Blender to Metric or Imperial units. Add empties named **Fraynix Scale A** and **Fraynix Scale B** at the reference endpoints. Add numeric **fraynix_known_length_m** and **fraynix_tolerance_m** custom properties to the scene. Use a static surface and at least one active rigid box. The import supports the shapes and settings in the [Spatial v1 contract and qualification plan](docs/publication189/plan.md). The scale check compares the stated length with the model. Independent camera or instrument validation is still pending.

![Native Java grasp and release completed using the Blender source](docs/readmes/189/blender-spatial-verified.png)

## Take Game Lab geometry into Blender

Open **Game**, play a course, pause, then select **Export Blender layout**. The file contains planar meshes, stable source IDs and component metadata. The original Java snapshot is available in Blender's Text Editor as **Fraynix Game snapshot.json**. The layout retains game units and is intended for inspection and authoring. The export covers geometry and metadata. Use Game Lab to playtest levels.

![Relay Garden frame exported at tick 30](docs/readmes/189/blender-game-layout.png)

## What passed

| Check | Result |
| --- | --- |
| Blender authoring round trip | Blender {app['blenderVersion']}. Three source IDs preserved. Positions and dimensions matched within {exchange['roundTripToleranceM']:.6f} m. |
| Invalid imports | Mismatched scale and unsupported sphere collider refused. |
| Java physics | {physics['metrics']['samples']} aligned samples, one grab, one release and {physics['browserChecks']} browser report checks. The imported scene and verified procedure survived restart. |
| Game Lab export | {game['quads']} editable quads from {game['entities']} source entities at tick {game['tick']}. Java accepted the original snapshot and refused a changed one. |

{counts} Validation ran locally.

[Verification record](docs/readmes/189/verification.json) · [Implementation and follow-on plan](docs/publication189/plan.md) · [Earlier Blender source assessment](docs/publication161/blender161.md)
""")
entry=read("README.md")
previous=("[Edition 188 — recover the final review write](README-188.md) · [Previous edition](README-187.md)"
          " · [Offline walkthrough](docs/readmes/188/index.html) · [Full history](docs/readmes/INDEX.md)")
latest=("[Edition 189 — Blender exchange and Game Lab layouts](README-189.md) · [Previous edition](README-188.md)"
        " · [Offline walkthrough](docs/readmes/189/index.html) · [Full history](docs/readmes/INDEX.md)")
if latest not in entry:
    start=entry.index("\n\n",entry.index(previous))+2
    write("README.md",entry[:start].replace(previous,latest)+summary+entry[entry.index("\n",start):])
index=read("docs/readmes/INDEX.md")
if "README-189.md" not in index:
    start=index.index("[Edition 188")
    write("docs/readmes/INDEX.md",index[:start]+"[Edition 189 — Blender exchange and Game Lab layouts](../../README-189.md)"
          " · [Walkthrough](189/index.html)"+chr(10)*2+index[start:])
record=(f"## Edition 189 — {title}" + chr(10)*2 + f"2026-11-19. {summary} {counts} "
        "The Blender 5.3 round trip preserved three box source identities and the one-metre reference. "
        "A reimported scene passed native manipulation and report verification. Relay Garden exported "
        "124 editable quads with a verified Java snapshot at tick 30. Camera or instrument validation and "
        "importing edited Game Lab levels are open. See README-189.md and docs/publication189/plan.md."+chr(10)*2)
for name in ("AGENTS.md","README-CONTRIBUTORS.md","docs/PROJECT_MEMORY.md","docs/DEVELOPMENT_LOG.md",
             "docs/ARCHITECTURE.md","docs/INTEGRATED_AGENT_ROADMAP.md"):
    prepend(name,record)
note="docs/obsidian/197-blender-game-layout.md"
canvas="docs/obsidian/197-blender-game-layout.canvas"
prepend("docs/obsidian/00-home.md","[Edition 189 — Blender exchange and Game Lab layouts](197-blender-game-layout.md)\n\n")
write(note,f"# {title}\n\n{summary}\n\n[Edition 189](../../README-189.md) · "
      "[Workflow map](197-blender-game-layout.canvas) · [Implementation plan](../publication189/plan.md)\n")
java="workspace-engine/src/main/java/fraymus/workspace/"
modules=[{"title":label,"source":path,"note":note} for label,path in [
    ("Blender exchange","workspace-desktop/blender-exchange.py"),
    ("Local desktop adapter","workspace-desktop/blender-exchange.cjs"),
    ("Native game layout",java+"EmbeddedGameClient.java"),
    ("Game Lab control","workspace-desktop/renderer/brain-game.js")]]
nodes=[m["source"] for m in modules]+["README-189.md"]
js(canvas,{"nodes":[{"id":str(i),"type":"file","file":p,"x":370*i,"y":0,"width":345,"height":240}
                    for i,p in enumerate(nodes)],
           "edges":[{"id":f"step-{i}","fromNode":str(i-1),"fromSide":"right","toNode":str(i),"toSide":"left"}
                    for i in range(1,len(nodes))]})
system={"summary":summary,"modules":modules,"screenshots":shots,"evidence":[
    {"path":"docs/readmes/189/verification.json","title":"Verification record"},
    {"path":"docs/readmes/189/authored-bench.blend","title":"Editable Blender bench fixture"}]}
js(snap/"system-map.json",system)
paths=set(subprocess.check_output(["git","diff","--name-only",baseline,source],text=True).splitlines())
paths.update([note,canvas])
inventory=[item(p) for p in sorted(paths)]
js(snap/"file-inventory.json",inventory)

guide=read("docs/readmes/188/index.html")
guide=re.sub(r"<title>.*?</title>",f"<title>Fraynix 189 · {escape(title)}</title>",guide,count=1)
guide=re.sub(r"<h1>.*?</h1><p>.*?</p>",f"<h1>{escape(title)}</h1><p>{escape(summary)}</p>",guide,count=1)
guide=guide.replace("../../../README-188.md","../../../README-189.md").replace(
    '../187/index.html">Edition 187','../188/index.html">Edition 188')
images="\n".join(f'<figure><img src="{Path(r["path"]).name}" alt="{escape(r["title"])}">'
                 f'<figcaption>{escape(r["title"])}</figcaption></figure>' for r in shots)
guide=re.sub(r"<figure>.*?(?=<h2)",lambda _:images+"\n",guide,count=1,flags=re.S)
for key,data in [("system",system),("inventory",inventory)]:
    payload=json.dumps(data,ensure_ascii=True).replace("</","<\\/")
    pattern=f'<script id="{key}-data" type="application/json">.*?</script>'
    guide=re.sub(pattern,lambda _:f'<script id="{key}-data" type="application/json">{payload}</script>',
                 guide,count=1,flags=re.S)
write(snap/"index.html",guide)

manifest=json.loads(subprocess.check_output(["git","show",f"{baseline}:docs/readmes/editions.json"]))
assert manifest["latest"] == 188
assets=[{"path":p.as_posix(),"kind":"binary" if p.suffix in {".png", ".blend"} else "text","sha256":sha256(p.read_bytes()).hexdigest() if p.suffix==".blend" else item(p)["sha256"]}
        for p in sorted(snap.iterdir())]
manifest["latest"]=189
manifest["editions"].append({"number":189,"date":"2026-11-19","title":title,"status":"validated-local",
    "markdown":"README-189.md","markdownSha256":item("README-189.md")["sha256"],
    "companion":"docs/readmes/189/index.html","tag":"readme-189","sourceBaseline":source,
    "snapshotDirectory":"docs/readmes/189","snapshotFiles":assets})
js("docs/readmes/editions.json",manifest)
print(json.dumps({"passed":True,"screenshots":len(shots),"inventory":len(inventory),"frozen":len(assets)}))
