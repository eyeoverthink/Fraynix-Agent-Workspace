"""Publish Edition 190 from the Blender and live Game Lab validation."""
from hashlib import sha256
from html import escape
import json
from pathlib import Path
import re
import shutil
import subprocess
import sys

snap = Path("docs/readmes/190")
snap.mkdir(exist_ok=True)
live = Path(sys.argv[1] if len(sys.argv) > 1 else "build/edition190-release")
baseline = "6b0117c97856c869718a69b323fb3d69a2425cba"
source = subprocess.check_output(["git", "rev-parse", "HEAD"], text=True).strip()
app = json.loads((live / "result.json").read_bytes())
tests = json.loads(Path("build/edition190-regression/summary.json").read_bytes())
assert app["passed"] and tests["passed"] and all(app["checks"].values())
candidate = app["candidate"]
proof = app["proof"]
failed = app["failedTrial"]
assert proof["passed"] and proof["trials"] == 2
assert candidate["moves"][0]["from"] == [4.375, .125] and candidate["moves"][0]["to"] == [4.625, .375]
assert failed["moves"] == 16 and not failed["verified"]
assert sha256((live / "edited-course.blend").read_bytes()).hexdigest() == candidate["blendSha256"]
traces = json.loads((live / "playtest-evidence.json").read_bytes())
assert traces["proof"] == proof
assert sha256((live / "native-beacon.png").read_bytes()).hexdigest() == traces["attempts"][-1]["trials"][1]["imageSha256"]
for name in ("edited-course.blend", "playtest-evidence.json", "native-beacon.png"):
    shutil.copyfile(live / name, snap / name)
title = "Turn a Blender edit into a verified playable Game Lab candidate"
summary = ("Move original Relay ground tiles in Blender, import a separate Game Lab candidate, "
           "and verify its playability with two fresh Java AI runs.")
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





labels={"blender-edit-imported.png":"The Blender translation received a separate candidate",
        "blender-candidate-playing.png":"The candidate advances in the native interactive runtime",
        "blender-candidate-verified.png":"Java rechecks the two completed AI playtests"}
shots=[]
for shot in app["screenshots"]:
    name=shot["name"]
    original=live/name
    assert sha256(original.read_bytes()).hexdigest()==shot["sha256"]
    shutil.copyfile(original,snap/name)
    shots.append({"path":(snap/name).as_posix(),"title":labels[name]})
assert set(labels)=={Path(shot["path"]).name for shot in shots}
app.update({"source":source,"tests":tests})
js(snap/"verification.json",app)
write("README-190.md",f"""# Fraynix Agent Workspace · Edition 190

[Edition 189](README-189.md) · [Offline walkthrough](docs/readmes/190/index.html) · [Full edition history](docs/readmes/INDEX.md)

{summary}

![Blender edit imported in Game Lab](docs/readmes/190/blender-edit-imported.png)

## Edit and play

1. In the **Game** tab, open an original Relay course and pause. Choose **Export Blender layout**.
2. In Blender Object Mode, move an existing ground tile. Blender X maps to Game Lab X and Blender Z maps to Game Lab Y. Move in increments of 0.25 and keep the tile in the game plane. Save a new .blend file.
3. In **Blender level candidates**, choose **Import Blender candidate**. For a quick trial, use the [edited Relay Garden sample](docs/readmes/190/edited-course.blend).
4. Select **Open candidate** to use ordinary controls, or **Run AI playtest** to let the Java controller try it. **Recheck evidence** audits the saved result and **Export evidence** saves the verified trace as JSON.

This first importer accepts translations of one to sixteen existing ground tiles. It checks positions against the original grid, source identities and start and finish clearance. Java rebuilds a separate level from the original course components.

![Native gameplay on the edited candidate](docs/readmes/190/blender-candidate-playing.png)

## What the test demonstrated

The test moved one tile from (4.375, 0.125) to (4.625, 0.375), narrowing the top of the first Relay Garden gap. The native runtime loaded the edited tile at that position, and manual input advanced the player. Two fresh AI sessions reached the beacon at tick **{proof['ticks']}** with matching sampled motion. The interactive session stayed at tick 30 throughout the background playtests. The original course reopened at its original tile position.

A separate sixteen-tile edit opened as a candidate and failed its AI trial at tick **{failed['tick']}**. Its trace stayed on disk. The test also exercised a moved player, off-grid tile, changed snapshot, modified level file and modified finish image. Java rechecked the successful proof after the desktop restarted. {counts} Validation ran locally.

![The completed Blender candidate proof](docs/readmes/190/blender-candidate-verified.png)

[Native beacon screenshot](docs/readmes/190/native-beacon.png) · [Full verified trace](docs/readmes/190/playtest-evidence.json) · [Verification summary](docs/readmes/190/verification.json) · [Implementation and qualification plan](docs/publication190/plan.md)
""")
entry=read("README.md")
previous=("[Edition 189 — Blender exchange and Game Lab layouts](README-189.md) · [Previous edition](README-188.md)"
          " · [Offline walkthrough](docs/readmes/189/index.html) · [Full history](docs/readmes/INDEX.md)")
latest=("[Edition 190 — playable Blender edits](README-190.md) · [Previous edition](README-189.md)"
        " · [Offline walkthrough](docs/readmes/190/index.html) · [Full history](docs/readmes/INDEX.md)")
if latest not in entry:
    start=entry.index("\n\n",entry.index(previous))+2
    write("README.md",entry[:start].replace(previous,latest)+summary+entry[entry.index("\n",start):])
index=read("docs/readmes/INDEX.md")
if "README-190.md" not in index:
    start=index.index("[Edition 189")
    write("docs/readmes/INDEX.md",index[:start]+"[Edition 190 — playable Blender edits](../../README-190.md)"
          " · [Walkthrough](190/index.html)"+chr(10)*2+index[start:])
record=(f"## Edition 190 — {title}"+chr(10)*2+f"2026-11-19. {summary} {counts} "
        f"Relay Garden's edited candidate achieved two matching wins at tick {proof['ticks']}. "
        f"A sixteen-tile challenge failed at tick {failed['tick']} and its trace was retained. "
        "Source, candidate, engine and image checks gate evidence export. Successful evidence rechecked after restart. "
        "See README-190.md and docs/publication190/plan.md."+chr(10)*2)
for name in ("AGENTS.md","README-CONTRIBUTORS.md","docs/PROJECT_MEMORY.md","docs/DEVELOPMENT_LOG.md",
             "docs/ARCHITECTURE.md","docs/INTEGRATED_AGENT_ROADMAP.md"):
    prepend(name,record)
note="docs/obsidian/198-blender-playtest.md"
canvas="docs/obsidian/198-blender-playtest.canvas"
prepend("docs/obsidian/00-home.md","[Edition 190 — Playable Blender edits](198-blender-playtest.md)\n\n")
write(note,f"# {title}\n\n{summary}\n\n[Edition 190](../../README-190.md) · "
      "[Workflow map](198-blender-playtest.canvas) · [Implementation plan](../publication190/plan.md)\n")
java="workspace-engine/src/main/java/fraymus/workspace/"
modules=[{"title":label,"source":path,"note":note} for label,path in [
    ("Candidate compiler",java+"BlenderGameCandidates.java"),
    ("Native playtests",java+"BlenderGamePlaytests.java"),
    ("Blender import adapter","workspace-desktop/blender-exchange.py"),
    ("Game Lab controls","workspace-desktop/renderer/brain-game.js")]]
nodes=[m["source"] for m in modules]+["README-190.md"]
js(canvas,{"nodes":[{"id":str(i),"type":"file","file":p,"x":370*i,"y":0,"width":345,"height":240}
                    for i,p in enumerate(nodes)],
           "edges":[{"id":f"step-{i}","fromNode":str(i-1),"fromSide":"right","toNode":str(i),"toSide":"left"}
                    for i in range(1,len(nodes))]})
system={"summary":summary,"modules":modules,"screenshots":shots,"evidence":[
    {"path":"docs/readmes/190/verification.json","title":"Verification record"},
    {"path":"docs/readmes/190/playtest-evidence.json","title":"Verified native trials"},
    {"path":"docs/readmes/190/edited-course.blend","title":"Edited Relay Garden sample"}]}
js(snap/"system-map.json",system)
paths=set(subprocess.check_output(["git","diff","--name-only",baseline,source],text=True).splitlines())
paths.update([note,canvas])
inventory=[item(p) for p in sorted(paths)]
js(snap/"file-inventory.json",inventory)

guide=read("docs/readmes/189/index.html")
guide=re.sub(r"<title>.*?</title>",f"<title>Fraynix 190 · {escape(title)}</title>",guide,count=1)
guide=re.sub(r"<h1>.*?</h1><p>.*?</p>",f"<h1>{escape(title)}</h1><p>{escape(summary)}</p>",guide,count=1)
guide=guide.replace("../../../README-189.md","../../../README-190.md").replace(
    '../188/index.html">Edition 188','../189/index.html">Edition 189')
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
assert manifest["latest"] == 189
assets=[{"path":p.as_posix(),"kind":"binary" if p.suffix in {".png", ".blend"} else "text","sha256":sha256(p.read_bytes()).hexdigest() if p.suffix==".blend" else item(p)["sha256"]}
        for p in sorted(snap.iterdir())]
manifest["latest"]=190
manifest["editions"].append({"number":190,"date":"2026-11-19","title":title,"status":"validated-local",
    "markdown":"README-190.md","markdownSha256":item("README-190.md")["sha256"],
    "companion":"docs/readmes/190/index.html","tag":"readme-190","sourceBaseline":source,
    "snapshotDirectory":"docs/readmes/190","snapshotFiles":assets})
js("docs/readmes/editions.json",manifest)
print(json.dumps({"passed":True,"screenshots":len(shots),"inventory":len(inventory),"frozen":len(assets)}))
