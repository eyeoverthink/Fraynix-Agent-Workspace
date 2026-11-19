"""Freeze Edition 191 from native Chat and Game Lab verification."""
from hashlib import sha256
from html import escape
import json
from pathlib import Path
import re
import shutil
import subprocess
import sys

snap = Path("docs/readmes/191")
snap.mkdir(exist_ok=True)
live = Path(sys.argv[1] if len(sys.argv) > 1 else "build/edition191-release")
fixture = Path(sys.argv[2] if len(sys.argv) > 2 else "build/edition190-release/wide-gap-course.blend")
baseline = "b5715f2682d75f54c74c6b593ee9a14dd003f8d5"
source = subprocess.check_output(["git", "rev-parse", "HEAD"], text=True).strip()
app = json.loads((live / "result.json").read_bytes())
tests = json.loads(Path("build/edition191-regression/summary.json").read_bytes())
assert app["passed"] and tests["passed"] and all(app["checks"].values())
assert app["checks"]["browserChecks"] == 3 and app["recovery"]["resumes"] == 1
assert app["recovery"]["taskRetained"] and app["automatic"]["resumes"] == 0
assert app["recovery"]["candidateId"] != app["automatic"]["candidateId"]
audit = app["audit"]
diagnosis = audit["diagnosis"]
assert audit["passed"] and audit["playtest"]["trials"] == 2
assert diagnosis["lastTick"] == 1728 and len(diagnosis["edits"]) == diagnosis["budget"] == 8
assert diagnosis["originalSpan"] == 2.5 and diagnosis["remainingSpan"] == .5
assert sha256(fixture.read_bytes()).hexdigest() == app["blenderSha256"]
assert json.loads((live / "repair-evidence.json").read_bytes())["audit"] == audit
shutil.copyfile(fixture,snap / "wide-gap-course.blend")
shutil.copyfile(live / "repair-evidence.json",snap / "repair-evidence.json")
title = "Diagnose, repair and verify a Blender level in one Chat request"
summary = ("One durable Chat request runs the original Blender level, diagnoses the failed movement, "
           "repairs a separate candidate and saves two verified native playtests.")
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






labels={"mission-paused-after-repair.png":"The same Chat request retains its diagnosis and repaired candidate before restart",
        "mission-native-repair-verified.png":"Mission shows the checked native result",
        "blender-repair-evidence.png":"The actual browser-checked repair report and evidence link"}
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
write("README-191.md",f"""# Fraynix Agent Workspace · Edition 191

[Edition 190](README-190.md) · [Offline walkthrough](docs/readmes/191/index.html) · [Full edition history](docs/readmes/INDEX.md)

{summary}

![The verified Blender repair report](docs/readmes/191/blender-repair-evidence.png)

## Run it

1. Open **Game → Blender level candidates** and import the [wide-gap Blender sample](docs/readmes/191/wide-gap-course.blend).
2. Select **Prepare repair request**, then click **Start task** in Chat. The request names the selected candidate explicitly.
3. Follow the stages in **Mission**. The workflow runs a baseline when needed, diagnoses a supported failure pattern, builds one derived level, runs two fresh playtests and checks the saved report. Open **Open saved result** to view the diagnosis and download the native traces.

Add **Pause after repair.** to review the candidate before the playtests. **Resume** continues the saved request after a restart.

![The Mission is paused with a retained repair](docs/readmes/191/mission-paused-after-repair.png)

## What passed locally

The original Relay Garden variant reached tick **{diagnosis['lastTick']}** with the robot grounded and stalled at **X {diagnosis['x']:.3f}**. Java checked the final eight native samples, then found a **{diagnosis['originalSpan']:.2f}-unit** unsupported span immediately ahead. It reused eight raised ground tiles and reduced the largest remaining gap there to **{diagnosis['remainingSpan']:.2f} units**.

Two fresh native sessions reached the beacon at tick **{audit['playtest']['ticks']}** with matching sampled motion. The report passed three actual browser checks. A restart between repair and playtesting retained the same Chat and candidate IDs. A second request ran through automatically, reused the checked failed trace and produced an identical successful motion trace. Verification refused a modified original finish image and a modified browser receipt. The original Blender candidate file stayed byte-identical.

![Mission shows the completed repair evidence](docs/readmes/191/mission-native-repair-verified.png)

The current repair rule covers a grounded stall beside one unsupported span, using at most eight existing moved tiles. Java owns the diagnosis and bounded edit. {counts}

[Complete native evidence](docs/readmes/191/repair-evidence.json) · [Verification results](docs/readmes/191/verification.json) · [Implementation and local reproduction](docs/publication191/plan.md)
""")
entry=read("README.md")
previous=("[Edition 190 — playable Blender edits](README-190.md) · [Previous edition](README-189.md)"
          " · [Offline walkthrough](docs/readmes/190/index.html) · [Full history](docs/readmes/INDEX.md)")
latest=("[Edition 191 — durable Blender repair from Chat](README-191.md) · [Previous edition](README-190.md)"
        " · [Offline walkthrough](docs/readmes/191/index.html) · [Full history](docs/readmes/INDEX.md)")
if latest not in entry:
    start=entry.index("\n\n",entry.index(previous))+2
    write("README.md",entry[:start].replace(previous,latest)+summary+entry[entry.index("\n",start):])
index=read("docs/readmes/INDEX.md")
if "README-191.md" not in index:
    start=index.index("[Edition 190")
    write("docs/readmes/INDEX.md",index[:start]+"[Edition 191 — durable Blender repair from Chat](../../README-191.md)"
          " · [Walkthrough](191/index.html)"+chr(10)*2+index[start:])
record=(f"## Edition 191 — {title}"+chr(10)*2+f"2026-11-19. {summary} {counts} "
        f"The wider-gap fixture stalled at tick {diagnosis['lastTick']}. An eight-tile repair narrowed 2.50 to 0.50 game units. "
        f"Two fresh trials reached the beacon at tick {audit['playtest']['ticks']}. "
        "The same Chat task resumed after restart and the automatic request passed. Java refused modified source and browser evidence. "
        "See README-191.md and docs/publication191/plan.md."+chr(10)*2)
for name in ("AGENTS.md","README-CONTRIBUTORS.md","docs/PROJECT_MEMORY.md","docs/DEVELOPMENT_LOG.md",
             "docs/ARCHITECTURE.md","docs/INTEGRATED_AGENT_ROADMAP.md"):
    prepend(name,record)
note="docs/obsidian/199-chat-blender-repair.md"
canvas="docs/obsidian/199-chat-blender-repair.canvas"
prepend("docs/obsidian/00-home.md","[Edition 191 — Durable Blender repair from Chat](199-chat-blender-repair.md)\n\n")
write(note,f"# {title}\n\n{summary}\n\n[Edition 191](../../README-191.md) · "
      "[Workflow map](199-chat-blender-repair.canvas) · [Implementation plan](../publication191/plan.md)\n")
java="workspace-engine/src/main/java/fraymus/workspace/"
modules=[{"title":label,"source":path,"note":note} for label,path in [
    ("Gap repair",java+"BlenderGapRepair.java"),("Java Chat scheduling",java+"Domain044.java"),
    ("Native evidence",java+"BlenderGamePlaytests.java"),("Game Lab controls","workspace-desktop/renderer/brain-game.js")]]
nodes=[m["source"] for m in modules]+["README-191.md"]
js(canvas,{"nodes":[{"id":str(i),"type":"file","file":p,"x":370*i,"y":0,"width":345,"height":240}
                    for i,p in enumerate(nodes)],
           "edges":[{"id":f"step-{i}","fromNode":str(i-1),"fromSide":"right","toNode":str(i),"toSide":"left"}
                    for i in range(1,len(nodes))]})
system={"summary":summary,"modules":modules,"screenshots":shots,"evidence":[
    {"path":"docs/readmes/191/verification.json","title":"Verification record"},
    {"path":"docs/readmes/191/repair-evidence.json","title":"Native failure and verified repaired trials"},
    {"path":"docs/readmes/191/wide-gap-course.blend","title":"Wide-gap Blender sample"}]}
js(snap/"system-map.json",system)
paths=set(subprocess.check_output(["git","diff","--name-only",baseline,source],text=True).splitlines())
paths.update([note,canvas])
inventory=[item(p) for p in sorted(paths)]
js(snap/"file-inventory.json",inventory)

guide=read("docs/readmes/190/index.html")
guide=re.sub(r"<title>.*?</title>",f"<title>Fraynix 191 · {escape(title)}</title>",guide,count=1)
guide=re.sub(r"<h1>.*?</h1><p>.*?</p>",f"<h1>{escape(title)}</h1><p>{escape(summary)}</p>",guide,count=1)
guide=guide.replace("../../../README-190.md","../../../README-191.md").replace(
    '../189/index.html">Edition 189','../190/index.html">Edition 190')
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
assert manifest["latest"] == 190
assets=[{"path":p.as_posix(),"kind":"binary" if p.suffix in {".png", ".blend"} else "text","sha256":sha256(p.read_bytes()).hexdigest() if p.suffix==".blend" else item(p)["sha256"]}
        for p in sorted(snap.iterdir())]
manifest["latest"]=191
manifest["editions"].append({"number":191,"date":"2026-11-19","title":title,"status":"validated-local",
    "markdown":"README-191.md","markdownSha256":item("README-191.md")["sha256"],
    "companion":"docs/readmes/191/index.html","tag":"readme-191","sourceBaseline":source,
    "snapshotDirectory":"docs/readmes/191","snapshotFiles":assets})
js("docs/readmes/editions.json",manifest)
print(json.dumps({"passed":True,"screenshots":len(shots),"inventory":len(inventory),"frozen":len(assets)}))
